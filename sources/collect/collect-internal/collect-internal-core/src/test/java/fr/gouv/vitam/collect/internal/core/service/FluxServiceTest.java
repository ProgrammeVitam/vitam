/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */

package fr.gouv.vitam.collect.internal.core.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.collect.common.dto.MetadataUnitUp;
import fr.gouv.vitam.collect.common.exception.CollectInternalErrorsDetailsException;
import fr.gouv.vitam.collect.common.exception.CollectInternalException;
import fr.gouv.vitam.collect.internal.core.common.CollectJsonMetadataLine;
import fr.gouv.vitam.collect.internal.core.common.ManifestContext;
import fr.gouv.vitam.collect.internal.core.common.ProjectModel;
import fr.gouv.vitam.collect.internal.core.common.ProjectStatus;
import fr.gouv.vitam.collect.internal.core.common.TransactionModel;
import fr.gouv.vitam.collect.internal.core.configuration.CollectInternalConfiguration;
import fr.gouv.vitam.collect.internal.core.helpers.MetadataHelper;
import fr.gouv.vitam.collect.internal.core.repository.MetadataRepository;
import fr.gouv.vitam.collect.internal.core.repository.ProjectRepository;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.format.identification.model.FormatIdentifierResponse;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.jsonl.JsonLineIterator;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.schema.SchemaResponse;
import fr.gouv.vitam.common.model.objectgroup.FileInfoModel;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.common.tmp.TempFolderRule;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import net.javacrumbs.jsonunit.JsonAssert;
import net.javacrumbs.jsonunit.core.Option;
import org.apache.commons.collections4.IteratorUtils;
import org.assertj.core.api.ThrowableAssert;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static fr.gouv.vitam.collect.internal.core.service.FluxService.METADATA_CSV_FILE;
import static fr.gouv.vitam.common.PropertiesUtils.getResourceAsStream;
import static fr.gouv.vitam.common.SedaConstants.TAG_FILE_INFO;
import static fr.gouv.vitam.common.SedaConstants.TAG_URI;
import static fr.gouv.vitam.common.SedaConstants.TAG_VERSIONS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FluxServiceTest {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(FluxServiceTest.class);

    private static final int TENANT_ID = 0;
    private static final String TRANSACTION_ID = "TRANSACTION_ID";
    private static final String PROJECT_ID = "PROJECT_ID";

    private static final String UNITS_PATH = "streamZip/units.json";
    private static final String UNITS_WITHOUT_BINARIES_PATH = "streamZip/units_without_binaries.json";
    private static final String UNITS_WITHOUT_BINARIES_WITH_MISSED_PROJECT_CONTEXT_PATH =
        "streamZip/units_without_binaries_project_context_missed.json";

    private static final String OBJECTGROUPS_PATH = "streamZip/objectgroups.json";

    private static final String TRANSACTION_ZIP_PATH = "streamZip/transaction.zip";
    private static final String TRANSACTION_ZIP_WITHOUT_BINARY_PATH = "streamZip/transaction_without_binary.zip";
    private static final String TRANSACTION_ZIP_EMPTY_PATH = "streamZip/zip_empty.zip";

    private static final String TRANSACTION_ZIP_WITH_METADATA_CSV_PATH = "streamZip/transaction_with_metadata_csv.zip";

    private static final String TRANSACTION_ZIP_WITH_METADATA_JSONL_PATH =
        "streamZip/transaction_with_metadata_jsonl.zip";

    private static final String TRANSACTION_WITHOUT_FILE_COLUMN_ZIP_PATH =
        "streamZip/transaction_without_file_column.zip";

    private static final String ARBORESCENCE_WITH_ACCENTS_WINDOWS_ZIP =
        "streamZip/arborescence_with_accents_windows.zip";
    private static final String ARBORESCENCE_WITH_ACCENTS_OBJECTGROUPS =
        "streamZip/arborescence_with_accents_objectgroups.json";
    private static final String ARBORESCENCE_WITH_ACCENTS_UNITS = "streamZip/arborescence_with_accents_units.json";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public TempFolderRule tempFolder = new TempFolderRule();

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Mock
    private CollectService collectService;

    @Mock
    private MetadataService metadataService;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private MetadataRepository metadataRepository;

    @Mock
    private AdminManagementClient adminManagementClient;

    @Mock
    private AdminManagementClientFactory adminManagementClientFactory;

    private CollectInternalConfiguration config = new CollectInternalConfiguration();

    private FluxService fluxService;

    private TransactionModel transactionModel;
    private ProjectModel projectModel;

    @Before
    public void setUp() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(TENANT_ID).getId());

        config.setApplyJsltPostDynamicAttachement(false);
        fluxService = new FluxService(
            collectService,
            metadataService,
            projectRepository,
            metadataRepository,
            adminManagementClientFactory,
            config
        );

        doReturn(adminManagementClient).when(adminManagementClientFactory).getClient();
        doReturn(loadUnitSchema()).when(adminManagementClient).getUnitSchema();

        transactionModel = new TransactionModel();
        transactionModel.setId(TRANSACTION_ID);
        transactionModel.setProjectId(PROJECT_ID);
        projectModel = new ProjectModel();
        projectModel.setId(PROJECT_ID);
        ManifestContext manifestContext = new ManifestContext();
        manifestContext.setOriginatingAgencyIdentifier("Vitam");
        manifestContext.setLegalStatus("Some status");
        projectModel.setManifestContext(manifestContext);
        when(collectService.detectFileFormat(any(File.class))).thenReturn(
            Optional.of(new FormatIdentifierResponse("", "", "", ""))
        );
    }

    @Test
    @RunWithCustomExecutor
    public void processStream() throws Exception {
        when(projectRepository.findProjectById(anyString())).thenReturn(Optional.of(projectModel));
        Map<String, JsonNode> units = new HashMap<>();
        Map<String, JsonNode> objectGroups = new HashMap<>();
        when(metadataRepository.saveArchiveUnits(ArgumentMatchers.anyList())).thenAnswer(e -> {
            final List<ObjectNode> unitsToSave = e.getArgument(0);
            for (ObjectNode unit : unitsToSave) {
                units.put(unit.get(VitamFieldsHelper.id()).asText(), unit);
            }
            return JsonHandler.toJsonNode(
                new RequestResponseOK<>(JsonHandler.createObjectNode(), unitsToSave, unitsToSave.size())
            );
        });

        when(metadataRepository.saveObjectGroups(anyList())).thenAnswer(e -> {
            final List<ObjectNode> ogToSave = e.getArgument(0);
            for (ObjectNode og : ogToSave) {
                objectGroups.put(og.get(VitamFieldsHelper.id()).asText(), og);
            }
            return JsonHandler.toJsonNode(
                new RequestResponseOK<>(JsonHandler.createObjectNode(), ogToSave, ogToSave.size())
            );
        });

        when(metadataService.prepareAttachmentUnits(any(), anyString())).thenReturn(new HashMap<>());

        try (final InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(TRANSACTION_ZIP_PATH)) {
            fluxService.processStream(resourceAsStream, PROJECT_ID, TRANSACTION_ID, null, null);
        }

        final JsonNode expectedUnits = JsonHandler.getFromFile(PropertiesUtils.getResourceFile(UNITS_PATH));

        JsonAssert.assertJsonEquals(
            units.values(),
            expectedUnits,
            JsonAssert.when(Option.IGNORING_ARRAY_ORDER).whenIgnoringPaths(
                List.of(
                    "[*]." + VitamFieldsHelper.id(),
                    "[*]." + VitamFieldsHelper.unitups(),
                    "[*]." + VitamFieldsHelper.object(),
                    "[*]." + VitamFieldsHelper.batchId()
                )
            )
        );

        final JsonNode expectedGots = JsonHandler.getFromFile(PropertiesUtils.getResourceFile(OBJECTGROUPS_PATH));

        JsonAssert.assertJsonEquals(
            JsonHandler.toJsonNode(objectGroups.values()),
            expectedGots,
            JsonAssert.when(Option.IGNORING_ARRAY_ORDER).whenIgnoringPaths(
                List.of(
                    "[*]." + VitamFieldsHelper.id(),
                    "[*]." + VitamFieldsHelper.batchId(),
                    "[*]." + VitamFieldsHelper.unitups(),
                    "[*]." + VitamFieldsHelper.qualifiers() + "[*]." + TAG_VERSIONS + "[*]." + VitamFieldsHelper.id(),
                    "[*]." + TAG_FILE_INFO + "." + FileInfoModel.LAST_MODIFIED,
                    "[*]." +
                    VitamFieldsHelper.qualifiers() +
                    "[*]." +
                    TAG_VERSIONS +
                    "[*]." +
                    TAG_FILE_INFO +
                    "." +
                    FileInfoModel.LAST_MODIFIED,
                    "[*]." + VitamFieldsHelper.qualifiers() + "[*]." + TAG_VERSIONS + "[*]." + TAG_URI
                )
            )
        );
    }

    @Test
    @RunWithCustomExecutor
    public void processStreamWithoutBinaryThenOK() throws Exception {
        when(projectRepository.findProjectById(anyString())).thenReturn(Optional.of(projectModel));
        Map<String, JsonNode> units = new HashMap<>();
        when(metadataRepository.saveArchiveUnits(ArgumentMatchers.anyList())).thenAnswer(e -> {
            final List<ObjectNode> unitsToSave = e.getArgument(0);
            for (ObjectNode unit : unitsToSave) {
                units.put(unit.get(VitamFieldsHelper.id()).asText(), unit);
            }
            return JsonHandler.toJsonNode(
                new RequestResponseOK<>(JsonHandler.createObjectNode(), unitsToSave, unitsToSave.size())
            );
        });

        when(metadataService.prepareAttachmentUnits(any(), anyString())).thenReturn(new HashMap<>());

        try (
            final InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(
                TRANSACTION_ZIP_WITHOUT_BINARY_PATH
            )
        ) {
            fluxService.processStream(resourceAsStream, PROJECT_ID, TRANSACTION_ID, null, null);
        }

        final JsonNode expectedUnits = JsonHandler.getFromFile(
            PropertiesUtils.getResourceFile(UNITS_WITHOUT_BINARIES_PATH)
        );

        JsonAssert.assertJsonEquals(
            units.values(),
            expectedUnits,
            JsonAssert.when(Option.IGNORING_ARRAY_ORDER).whenIgnoringPaths(
                List.of(
                    "[*]." + VitamFieldsHelper.id(),
                    "[*]." + VitamFieldsHelper.unitups(),
                    "[*]." + VitamFieldsHelper.object(),
                    "[*]." + VitamFieldsHelper.batchId()
                )
            )
        );

        verify(metadataRepository, never()).saveObjectGroups(anyList());
    }

    @Test
    @RunWithCustomExecutor
    public void processStreamWithoutBinaryThenOKWithMissedContext() throws Exception {
        projectModel.setManifestContext(null);
        when(projectRepository.findProjectById(anyString())).thenReturn(Optional.of(projectModel));
        Map<String, JsonNode> units = new HashMap<>();
        when(metadataRepository.saveArchiveUnits(ArgumentMatchers.anyList())).thenAnswer(e -> {
            final List<ObjectNode> unitsToSave = e.getArgument(0);
            for (ObjectNode unit : unitsToSave) {
                units.put(unit.get(VitamFieldsHelper.id()).asText(), unit);
            }
            return JsonHandler.toJsonNode(
                new RequestResponseOK<>(JsonHandler.createObjectNode(), unitsToSave, unitsToSave.size())
            );
        });

        when(metadataService.prepareAttachmentUnits(any(), anyString())).thenReturn(new HashMap<>());

        try (
            final InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(
                TRANSACTION_ZIP_WITHOUT_BINARY_PATH
            )
        ) {
            fluxService.processStream(resourceAsStream, PROJECT_ID, TRANSACTION_ID, null, null);
        }

        final JsonNode expectedUnits = JsonHandler.getFromFile(
            PropertiesUtils.getResourceFile(UNITS_WITHOUT_BINARIES_WITH_MISSED_PROJECT_CONTEXT_PATH)
        );

        JsonAssert.assertJsonEquals(
            units.values(),
            expectedUnits,
            JsonAssert.when(Option.IGNORING_ARRAY_ORDER).whenIgnoringPaths(
                List.of(
                    "[*]." + VitamFieldsHelper.id(),
                    "[*]." + VitamFieldsHelper.unitups(),
                    "[*]." + VitamFieldsHelper.object(),
                    "[*]." + VitamFieldsHelper.batchId()
                )
            )
        );

        verify(metadataRepository, never()).saveObjectGroups(anyList());
    }

    @Test
    @RunWithCustomExecutor
    public void processStreamWithEmptyZipThenKO() throws Exception {
        when(projectRepository.findProjectById(anyString())).thenReturn(Optional.of(projectModel));
        when(metadataService.prepareAttachmentUnits(any(), anyString())).thenReturn(new HashMap<>());

        try (final InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(TRANSACTION_ZIP_EMPTY_PATH)) {
            assertThatThrownBy(
                () -> fluxService.processStream(resourceAsStream, PROJECT_ID, TRANSACTION_ID, null, null)
            )
                .isInstanceOf(CollectInternalErrorsDetailsException.class)
                .hasMessage("An unexpected error occurs when try to upload the ZIP: Empty zip file");
        }

        verify(metadataRepository, never()).saveArchiveUnits(anyList());
        verify(metadataRepository, never()).saveObjectGroups(anyList());
    }

    @Test
    @RunWithCustomExecutor
    public void processStream_with_metadata_csv_update() throws Exception {
        when(projectRepository.findProjectById(anyString())).thenReturn(Optional.of(projectModel));

        when(metadataService.prepareAttachmentUnits(any(), anyString())).thenReturn(new HashMap<>());

        AtomicReference<byte[]> transformedMetadataFile = new AtomicReference<>();
        doAnswer(args -> {
            InputStream is = args.getArgument(1);
            transformedMetadataFile.set(is.readAllBytes());
            return null;
        })
            .when(metadataService)
            .updateUnitsWithJsonlMetadataFile(eq("TRANSACTION_ID"), any());

        try (
            final InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(
                TRANSACTION_ZIP_WITH_METADATA_CSV_PATH
            )
        ) {
            fluxService.processStream(resourceAsStream, PROJECT_ID, TRANSACTION_ID, null, null);
        }

        JsonNode transformedMetadataFileLines = JsonHandler.toJsonNode(
            new JsonLineIterator<JsonNode>(
                new ByteArrayInputStream(transformedMetadataFile.get()),
                new TypeReference<>() {}
            )
                .stream()
                .toArray()
        );

        JsonNode expectedTransformedMetadataFileLines = JsonHandler.toJsonNode(
            new JsonLineIterator<JsonNode>(
                PropertiesUtils.getResourceAsStream("streamZip/expected_transformed_metadata_from_csv.jsonl"),
                new TypeReference<>() {}
            )
                .stream()
                .toArray()
        );

        JsonAssert.assertJsonEquals(
            expectedTransformedMetadataFileLines,
            transformedMetadataFileLines,
            JsonAssert.when(Option.IGNORING_ARRAY_ORDER)
        );
    }

    @Test
    @RunWithCustomExecutor
    public void processStream_with_metadata_jsonl_update() throws Exception {
        when(projectRepository.findProjectById(anyString())).thenReturn(Optional.of(projectModel));

        when(metadataService.prepareAttachmentUnits(any(), anyString())).thenReturn(new HashMap<>());

        AtomicReference<byte[]> transformedMetadataFile = new AtomicReference<>();
        doAnswer(args -> {
            InputStream is = args.getArgument(1);
            transformedMetadataFile.set(is.readAllBytes());
            return null;
        })
            .when(metadataService)
            .updateUnitsWithJsonlMetadataFile(eq("TRANSACTION_ID"), any());

        try (
            final InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(
                TRANSACTION_ZIP_WITH_METADATA_JSONL_PATH
            )
        ) {
            fluxService.processStream(resourceAsStream, PROJECT_ID, TRANSACTION_ID, null, null);
        }

        JsonNode transformedMetadataFileLines = JsonHandler.toJsonNode(
            new JsonLineIterator<JsonNode>(
                new ByteArrayInputStream(transformedMetadataFile.get()),
                new TypeReference<>() {}
            )
                .stream()
                .toArray()
        );

        JsonNode expectedTransformedMetadataFileLines = JsonHandler.toJsonNode(
            new JsonLineIterator<JsonNode>(
                PropertiesUtils.getResourceAsStream("streamZip/expected_transformed_metadata_from_jsonl.jsonl"),
                new TypeReference<>() {}
            )
                .stream()
                .toArray()
        );

        JsonAssert.assertJsonEquals(
            expectedTransformedMetadataFileLines,
            transformedMetadataFileLines,
            JsonAssert.when(Option.IGNORING_ARRAY_ORDER)
        );
    }

    @Test
    @RunWithCustomExecutor
    public void processStream_without_file_column_in_csv_file() throws Exception {
        // Given
        when(projectRepository.findProjectById(anyString())).thenReturn(Optional.of(projectModel));
        final AtomicReference<File> fileReference = new AtomicReference<>();
        when(metadataService.prepareAttachmentUnits(any(), anyString())).thenReturn(new HashMap<>());
        when(collectService.pushStreamToWorkspace(any(), any(InputStream.class), eq(METADATA_CSV_FILE))).thenAnswer(
            e -> {
                final InputStream is = e.getArgument(1);
                final File file = tempFolder.newFile(METADATA_CSV_FILE);
                Files.copy(is, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                fileReference.set(file);
                return "";
            }
        );
        when(collectService.getInputStreamFromWorkspace(any(), eq(METADATA_CSV_FILE))).thenAnswer(
            e -> new FileInputStream(fileReference.get())
        );

        // When
        try (
            final InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(
                TRANSACTION_WITHOUT_FILE_COLUMN_ZIP_PATH
            )
        ) {
            CollectInternalException exception = Assert.assertThrows(
                CollectInternalException.class,
                () -> fluxService.processStream(resourceAsStream, PROJECT_ID, TRANSACTION_ID, null, null)
            );
            Assert.assertEquals(
                "An unexpected error occurs when try to upload the ZIP: Invalid header names. Missing required 'File' or '_id' header name",
                exception.getMessage()
            );
        }

        // Then
        // bulkWriteUnits
        verify(metadataRepository, never()).saveArchiveUnits(any());
        // bulkWriteObjectGroups
        verify(metadataRepository, never()).saveObjectGroups(any());
    }

    @Test
    @RunWithCustomExecutor
    public void processStream_with_attachment_params_should_be_ok() throws Exception {
        final ProjectModel project = JsonHandler.getFromString(
            PropertiesUtils.getResourceAsString("json/01_project_flux_auto_attach_param.json"),
            ProjectModel.class
        );
        final TransactionModel transaction = JsonHandler.getFromString(
            PropertiesUtils.getResourceAsString("json/01_transaction.json"),
            TransactionModel.class
        );

        when(projectRepository.findProjectById(anyString())).thenReturn(Optional.of(project));
        Map<String, JsonNode> units = new HashMap<>();
        Map<String, JsonNode> objectGroups = new HashMap<>();
        when(metadataRepository.saveArchiveUnits(ArgumentMatchers.anyList())).thenAnswer(e -> {
            final List<ObjectNode> unitsToSave = e.getArgument(0);
            for (ObjectNode unit : unitsToSave) {
                units.put(unit.get(VitamFieldsHelper.id()).asText(), unit);
            }
            return JsonHandler.toJsonNode(
                new RequestResponseOK<>(JsonHandler.createObjectNode(), unitsToSave, unitsToSave.size())
            );
        });
        when(metadataRepository.saveObjectGroups(anyList())).thenAnswer(e -> {
            final List<ObjectNode> ogToSave = e.getArgument(0);
            for (ObjectNode og : ogToSave) {
                objectGroups.put(og.get(VitamFieldsHelper.id()).asText(), og);
            }
            return JsonHandler.toJsonNode(
                new RequestResponseOK<>(JsonHandler.createObjectNode(), ogToSave, ogToSave.size())
            );
        });
        when(metadataService.prepareAttachmentUnits(any(), anyString())).thenReturn(new HashMap<>());

        try (final InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(TRANSACTION_ZIP_PATH)) {
            fluxService.processStream(resourceAsStream, transaction.getProjectId(), transaction.getId(), null, null);
        }

        final JsonNode expectedUnits = JsonHandler.getFromFile(
            PropertiesUtils.getResourceFile("json/01_expected_units.json")
        );
        JsonAssert.assertJsonEquals(
            units.values(),
            expectedUnits,
            JsonAssert.when(Option.IGNORING_ARRAY_ORDER).whenIgnoringPaths(
                List.of(
                    "[*]." + VitamFieldsHelper.id(),
                    "[*]." + VitamFieldsHelper.batchId(),
                    "[*]." + VitamFieldsHelper.unitups(),
                    "[*]." + VitamFieldsHelper.object()
                )
            )
        );

        final JsonNode expectedGots = JsonHandler.getFromFile(
            PropertiesUtils.getResourceFile("json/01_expected_got.json")
        );
        JsonAssert.assertJsonEquals(
            JsonHandler.toJsonNode(objectGroups.values()),
            expectedGots,
            JsonAssert.when(Option.IGNORING_ARRAY_ORDER).whenIgnoringPaths(
                List.of(
                    "[*]." + VitamFieldsHelper.id(),
                    "[*]." + VitamFieldsHelper.batchId(),
                    "[*]." + VitamFieldsHelper.unitups(),
                    "[*]." + VitamFieldsHelper.qualifiers() + "[*]." + TAG_VERSIONS + "[*]." + VitamFieldsHelper.id(),
                    "[*]." + TAG_FILE_INFO + "." + FileInfoModel.LAST_MODIFIED,
                    "[*]." +
                    VitamFieldsHelper.qualifiers() +
                    "[*]." +
                    TAG_VERSIONS +
                    "[*]." +
                    TAG_FILE_INFO +
                    "." +
                    FileInfoModel.LAST_MODIFIED,
                    "[*]." + VitamFieldsHelper.qualifiers() + "[*]." + TAG_VERSIONS + "[*]." + TAG_URI
                )
            )
        );
    }

    @Test
    @RunWithCustomExecutor
    public void processStream_with_attachment_should_be_ok() throws Exception {
        final ProjectModel project = JsonHandler.getFromString(
            PropertiesUtils.getResourceAsString("json/01_project_flux_auto_attach_param.json"),
            ProjectModel.class
        );
        final TransactionModel transaction = JsonHandler.getFromString(
            PropertiesUtils.getResourceAsString("json/01_transaction.json"),
            TransactionModel.class
        );

        when(projectRepository.findProjectById(anyString())).thenReturn(Optional.of(project));
        Map<String, JsonNode> units = new HashMap<>();
        when(metadataRepository.saveArchiveUnits(ArgumentMatchers.anyList())).thenAnswer(e -> {
            final List<ObjectNode> unitsToSave = e.getArgument(0);
            for (ObjectNode unit : unitsToSave) {
                units.put(unit.get(VitamFieldsHelper.id()).asText(), unit);
            }
            return JsonHandler.toJsonNode(
                new RequestResponseOK<>(JsonHandler.createObjectNode(), unitsToSave, unitsToSave.size())
            );
        });
        when(metadataService.prepareAttachmentUnits(any(), anyString())).thenReturn(new HashMap<>());

        when(metadataService.selectUnitsByTransactionId(any(), anyString())).thenReturn(
            new RequestResponseOK<JsonNode>().addResult(JsonHandler.createObjectNode())
        );

        try (final InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(TRANSACTION_ZIP_PATH)) {
            fluxService.processStream(
                resourceAsStream,
                transaction.getProjectId(),
                transaction.getId(),
                null,
                "some-attachement-id"
            );
        }

        JsonNode rootUnit = units
            .values()
            .stream()
            .filter(unit -> unit.get("Title").asText().equals("ROOT"))
            .findFirst()
            .get();

        JsonNode unitUpsNode = rootUnit.get("#unitups");
        Assert.assertTrue(unitUpsNode.toString().contains("some-attachement-id"));
    }

    @Test
    @RunWithCustomExecutor
    public void processStream_with_arborescence_with_accents_windows_zip() throws Exception {
        Map<String, JsonNode> units = new HashMap<>();
        Map<String, JsonNode> objectGroups = new HashMap<>();

        when(projectRepository.findProjectById(anyString())).thenReturn(Optional.of(projectModel));
        when(metadataService.prepareAttachmentUnits(any(), anyString())).thenReturn(new HashMap<>());
        when(metadataRepository.saveArchiveUnits(ArgumentMatchers.anyList())).thenAnswer(e -> {
            final List<ObjectNode> unitsToSave = e.getArgument(0);
            for (ObjectNode unit : unitsToSave) {
                units.put(unit.get(VitamFieldsHelper.id()).asText(), unit);
            }
            return JsonHandler.toJsonNode(
                new RequestResponseOK<>(JsonHandler.createObjectNode(), unitsToSave, unitsToSave.size())
            );
        });
        when(metadataRepository.saveObjectGroups(anyList())).thenAnswer(e -> {
            final List<ObjectNode> ogToSave = e.getArgument(0);
            for (ObjectNode og : ogToSave) {
                objectGroups.put(og.get(VitamFieldsHelper.id()).asText(), og);
            }
            return JsonHandler.toJsonNode(
                new RequestResponseOK<>(JsonHandler.createObjectNode(), ogToSave, ogToSave.size())
            );
        });

        try (final InputStream resourceAsStream = getResourceAsStream(ARBORESCENCE_WITH_ACCENTS_WINDOWS_ZIP)) {
            fluxService.processStream(resourceAsStream, "PROJECT_ID", "TRANSACTION_ID", "IBM437", null);
        }

        JsonAssert.assertJsonEquals(
            JsonHandler.getFromFile(PropertiesUtils.getResourceFile(ARBORESCENCE_WITH_ACCENTS_UNITS)),
            units.values(),
            JsonAssert.when(Option.IGNORING_ARRAY_ORDER).whenIgnoringPaths(
                List.of(
                    "[*]." + VitamFieldsHelper.id(),
                    "[*]." + VitamFieldsHelper.unitups(),
                    "[*]." + VitamFieldsHelper.object(),
                    "[*]." + VitamFieldsHelper.batchId()
                )
            )
        );
        JsonAssert.assertJsonEquals(
            JsonHandler.getFromFile(PropertiesUtils.getResourceFile(ARBORESCENCE_WITH_ACCENTS_OBJECTGROUPS)),
            JsonHandler.toJsonNode(objectGroups.values()),
            JsonAssert.when(Option.IGNORING_ARRAY_ORDER).whenIgnoringPaths(
                List.of(
                    "[*]." + VitamFieldsHelper.id(),
                    "[*]." + VitamFieldsHelper.unitups(),
                    "[*]." + VitamFieldsHelper.batchId(),
                    "[*]." + VitamFieldsHelper.qualifiers() + "[*]." + TAG_VERSIONS + "[*]." + VitamFieldsHelper.id(),
                    "[*]." + TAG_FILE_INFO + "." + FileInfoModel.LAST_MODIFIED,
                    "[*]." +
                    VitamFieldsHelper.qualifiers() +
                    "[*]." +
                    TAG_VERSIONS +
                    "[*]." +
                    TAG_FILE_INFO +
                    "." +
                    FileInfoModel.LAST_MODIFIED,
                    "[*]." + VitamFieldsHelper.qualifiers() + "[*]." + TAG_VERSIONS + "[*]." + TAG_URI
                )
            )
        );
    }

    @Test
    @RunWithCustomExecutor
    public void processStream_with_metadata_csv_and_jslt_transformation_before_attachment_ok() throws Exception {
        // Given
        ProjectModel project = createTestProjectWithJsltTransformation();

        when(projectRepository.findProjectById(project.getId())).thenReturn(Optional.of(project));
        doReturn(JsonHandler.createObjectNode()).when(metadataRepository).saveArchiveUnits(ArgumentMatchers.anyList());

        when(metadataService.prepareAttachmentUnits(any(), anyString())).thenReturn(
            Map.of("rootUnitUp", "static_attachment_guid", "unit1", "guid_attachment1", "unit2", "guid_attachment2")
        );

        List<CollectJsonMetadataLine> unitUpdates = new ArrayList<>();
        doAnswer(e -> {
            try (
                JsonLineIterator<CollectJsonMetadataLine> metadata = new JsonLineIterator<>(
                    e.getArgument(1),
                    CollectJsonMetadataLine.TYPE_REFERENCE
                )
            ) {
                metadata.forEachRemaining(unitUpdates::add);
            }
            return null;
        })
            .when(metadataService)
            .updateUnitsWithJsonlMetadataFile(eq("transactionId"), any());

        // Apply JSLT "before" dynamic attachement
        config.setApplyJsltPostDynamicAttachement(false);

        // When
        try (
            final InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(
                "streamZip/simple_zip_with_metadata_csv.zip"
            )
        ) {
            fluxService.processStream(resourceAsStream, project.getId(), "transactionId", null, null);
        }

        // Then
        ArgumentCaptor<List<ObjectNode>> savedUnitsArgCaptor = ArgumentCaptor.forClass(List.class);
        verify(metadataRepository).saveArchiveUnits(savedUnitsArgCaptor.capture());
        checkInsertedUnits(
            savedUnitsArgCaptor.getValue(),
            "streamZip/expected_inserted_unit_with_jslt_before_attachement.json"
        );

        checkUpdatedUnits(unitUpdates, "streamZip/expected_updated_units_metadata_csv_and_jslt.json");
    }

    @Test
    @RunWithCustomExecutor
    public void processStream_with_metadata_csv_and_jslt_transformation_after_attachment_ok() throws Exception {
        // Given
        ProjectModel project = createTestProjectWithJsltTransformation();

        when(projectRepository.findProjectById(project.getId())).thenReturn(Optional.of(project));
        doReturn(JsonHandler.createObjectNode()).when(metadataRepository).saveArchiveUnits(ArgumentMatchers.anyList());

        when(metadataService.prepareAttachmentUnits(any(), anyString())).thenReturn(
            Map.of("rootUnitUp", "static_attachment_guid", "unit1", "guid_attachment1", "unit2", "guid_attachment2")
        );

        List<CollectJsonMetadataLine> unitUpdates = new ArrayList<>();
        doAnswer(e -> {
            try (
                JsonLineIterator<CollectJsonMetadataLine> metadata = new JsonLineIterator<>(
                    e.getArgument(1),
                    CollectJsonMetadataLine.TYPE_REFERENCE
                )
            ) {
                metadata.forEachRemaining(unitUpdates::add);
            }
            return null;
        })
            .when(metadataService)
            .updateUnitsWithJsonlMetadataFile(eq("transactionId"), any());

        // Apply JSLT "after" dynamic attachement
        config.setApplyJsltPostDynamicAttachement(true);

        // When
        try (
            final InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(
                "streamZip/simple_zip_with_metadata_csv.zip"
            )
        ) {
            fluxService.processStream(resourceAsStream, project.getId(), "transactionId", null, null);
        }

        // Then
        ArgumentCaptor<List<ObjectNode>> savedUnitsArgCaptor = ArgumentCaptor.forClass(List.class);
        verify(metadataRepository).saveArchiveUnits(savedUnitsArgCaptor.capture());
        checkInsertedUnits(
            savedUnitsArgCaptor.getValue(),
            "streamZip/expected_inserted_unit_with_jslt_after_attachement.json"
        );

        checkUpdatedUnits(unitUpdates, "streamZip/expected_updated_units_metadata_csv_and_jslt.json");
    }

    @Test
    @RunWithCustomExecutor
    public void processStream_with_metadata_jsonl_and_jslt_transformation_before_attachment_ok() throws Exception {
        // Given
        ProjectModel project = createTestProjectWithJsltTransformation();

        when(projectRepository.findProjectById(project.getId())).thenReturn(Optional.of(project));
        doReturn(JsonHandler.createObjectNode()).when(metadataRepository).saveArchiveUnits(ArgumentMatchers.anyList());

        when(metadataService.prepareAttachmentUnits(any(), anyString())).thenReturn(
            Map.of("rootUnitUp", "static_attachment_guid", "unit1", "guid_attachment1", "unit2", "guid_attachment2")
        );

        List<CollectJsonMetadataLine> unitUpdates = new ArrayList<>();
        doAnswer(e -> {
            try (
                JsonLineIterator<CollectJsonMetadataLine> metadata = new JsonLineIterator<>(
                    e.getArgument(1),
                    CollectJsonMetadataLine.TYPE_REFERENCE
                )
            ) {
                metadata.forEachRemaining(unitUpdates::add);
            }
            return null;
        })
            .when(metadataService)
            .updateUnitsWithJsonlMetadataFile(eq("transactionId"), any());

        // Apply JSLT "before" dynamic attachement
        config.setApplyJsltPostDynamicAttachement(false);

        // When
        try (
            final InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(
                "streamZip/simple_zip_with_metadata_jsonl.zip"
            )
        ) {
            fluxService.processStream(resourceAsStream, project.getId(), "transactionId", null, null);
        }

        // Then
        ArgumentCaptor<List<ObjectNode>> savedUnitsArgCaptor = ArgumentCaptor.forClass(List.class);
        verify(metadataRepository).saveArchiveUnits(savedUnitsArgCaptor.capture());
        checkInsertedUnits(
            savedUnitsArgCaptor.getValue(),
            "streamZip/expected_inserted_unit_with_jslt_before_attachement.json"
        );

        checkUpdatedUnits(unitUpdates, "streamZip/expected_updated_units_metadata_csv_and_jslt.json");
    }

    @Test
    @RunWithCustomExecutor
    public void processStream_with_metadata_jsonl_and_jslt_transformation_after_attachment_ok() throws Exception {
        // Given
        ProjectModel project = createTestProjectWithJsltTransformation();

        when(projectRepository.findProjectById(project.getId())).thenReturn(Optional.of(project));
        doReturn(JsonHandler.createObjectNode()).when(metadataRepository).saveArchiveUnits(ArgumentMatchers.anyList());

        when(metadataService.prepareAttachmentUnits(any(), anyString())).thenReturn(
            Map.of("rootUnitUp", "static_attachment_guid", "unit1", "guid_attachment1", "unit2", "guid_attachment2")
        );

        List<CollectJsonMetadataLine> unitUpdates = new ArrayList<>();
        doAnswer(e -> {
            try (
                JsonLineIterator<CollectJsonMetadataLine> metadata = new JsonLineIterator<>(
                    e.getArgument(1),
                    CollectJsonMetadataLine.TYPE_REFERENCE
                )
            ) {
                metadata.forEachRemaining(unitUpdates::add);
            }
            return null;
        })
            .when(metadataService)
            .updateUnitsWithJsonlMetadataFile(eq("transactionId"), any());

        // Apply JSLT "after" dynamic attachement
        config.setApplyJsltPostDynamicAttachement(true);

        // When
        try (
            final InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(
                "streamZip/simple_zip_with_metadata_jsonl.zip"
            )
        ) {
            fluxService.processStream(resourceAsStream, project.getId(), "transactionId", null, null);
        }

        // Then
        ArgumentCaptor<List<ObjectNode>> savedUnitsArgCaptor = ArgumentCaptor.forClass(List.class);
        verify(metadataRepository).saveArchiveUnits(savedUnitsArgCaptor.capture());
        checkInsertedUnits(
            savedUnitsArgCaptor.getValue(),
            "streamZip/expected_inserted_unit_with_jslt_after_attachement.json"
        );

        checkUpdatedUnits(unitUpdates, "streamZip/expected_updated_units_metadata_csv_and_jslt.json");
    }

    @Test
    @RunWithCustomExecutor
    public void processStream_without_metadata_with_jslt_transformation_before_attachment_ok() throws Exception {
        // Given
        ProjectModel project = createTestProjectWithJsltTransformation();

        when(projectRepository.findProjectById(project.getId())).thenReturn(Optional.of(project));
        doReturn(JsonHandler.createObjectNode()).when(metadataRepository).saveArchiveUnits(ArgumentMatchers.anyList());

        when(metadataService.prepareAttachmentUnits(any(), anyString())).thenReturn(
            Map.of("rootUnitUp", "static_attachment_guid", "unit1", "guid_attachment1", "unit2", "guid_attachment2")
        );

        List<CollectJsonMetadataLine> unitUpdates = new ArrayList<>();
        doAnswer(e -> {
            try (
                JsonLineIterator<CollectJsonMetadataLine> metadata = new JsonLineIterator<>(
                    e.getArgument(1),
                    CollectJsonMetadataLine.TYPE_REFERENCE
                )
            ) {
                metadata.forEachRemaining(unitUpdates::add);
            }
            return null;
        })
            .when(metadataService)
            .updateUnitsWithJsonlMetadataFile(eq("transactionId"), any());

        // Apply JSLT "before" dynamic attachement
        config.setApplyJsltPostDynamicAttachement(false);

        // When
        try (
            final InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(
                "streamZip/simple_zip_without_metadata.zip"
            )
        ) {
            fluxService.processStream(resourceAsStream, project.getId(), "transactionId", null, null);
        }

        // Then
        ArgumentCaptor<List<ObjectNode>> savedUnitsArgCaptor = ArgumentCaptor.forClass(List.class);
        verify(metadataRepository).saveArchiveUnits(savedUnitsArgCaptor.capture());
        checkInsertedUnits(
            savedUnitsArgCaptor.getValue(),
            "streamZip/expected_inserted_unit_with_jslt_before_attachement.json"
        );

        checkUpdatedUnits(unitUpdates, "streamZip/expected_updated_units_without_metadata_with_jslt.json");
    }

    @Test
    @RunWithCustomExecutor
    public void processStream_without_metadata_with_jslt_transformation_after_attachment_ok() throws Exception {
        // Given
        ProjectModel project = createTestProjectWithJsltTransformation();

        when(projectRepository.findProjectById(project.getId())).thenReturn(Optional.of(project));
        doReturn(JsonHandler.createObjectNode()).when(metadataRepository).saveArchiveUnits(ArgumentMatchers.anyList());

        when(metadataService.prepareAttachmentUnits(any(), anyString())).thenReturn(
            Map.of("rootUnitUp", "static_attachment_guid", "unit1", "guid_attachment1", "unit2", "guid_attachment2")
        );

        List<CollectJsonMetadataLine> unitUpdates = new ArrayList<>();
        doAnswer(e -> {
            try (
                JsonLineIterator<CollectJsonMetadataLine> metadata = new JsonLineIterator<>(
                    e.getArgument(1),
                    CollectJsonMetadataLine.TYPE_REFERENCE
                )
            ) {
                metadata.forEachRemaining(unitUpdates::add);
            }
            return null;
        })
            .when(metadataService)
            .updateUnitsWithJsonlMetadataFile(eq("transactionId"), any());

        // Apply JSLT "after" dynamic attachement
        config.setApplyJsltPostDynamicAttachement(true);

        // When
        try (
            final InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(
                "streamZip/simple_zip_without_metadata.zip"
            )
        ) {
            fluxService.processStream(resourceAsStream, project.getId(), "transactionId", null, null);
        }

        // Then
        ArgumentCaptor<List<ObjectNode>> savedUnitsArgCaptor = ArgumentCaptor.forClass(List.class);
        verify(metadataRepository).saveArchiveUnits(savedUnitsArgCaptor.capture());
        checkInsertedUnits(
            savedUnitsArgCaptor.getValue(),
            "streamZip/expected_inserted_unit_with_jslt_after_attachement_without_metadata.json"
        );

        checkUpdatedUnits(unitUpdates, "streamZip/expected_updated_units_without_metadata_with_jslt.json");
    }

    @Test
    @RunWithCustomExecutor
    public void processStream_with_csv_and_ObjectFiles_ok() throws Exception {
        when(projectRepository.findProjectById(anyString())).thenReturn(Optional.of(projectModel));
        Map<String, JsonNode> units = new HashMap<>();
        Map<String, JsonNode> objectGroups = new HashMap<>();
        when(metadataRepository.saveArchiveUnits(ArgumentMatchers.anyList())).thenAnswer(e -> {
            final List<ObjectNode> unitsToSave = e.getArgument(0);
            for (ObjectNode unit : unitsToSave) {
                units.put(unit.get(VitamFieldsHelper.id()).asText(), unit);
            }
            return JsonHandler.toJsonNode(
                new RequestResponseOK<>(JsonHandler.createObjectNode(), unitsToSave, unitsToSave.size())
            );
        });

        when(metadataRepository.saveObjectGroups(anyList())).thenAnswer(e -> {
            final List<ObjectNode> ogToSave = e.getArgument(0);
            for (ObjectNode og : ogToSave) {
                objectGroups.put(og.get(VitamFieldsHelper.id()).asText(), og);
            }
            return JsonHandler.toJsonNode(
                new RequestResponseOK<>(JsonHandler.createObjectNode(), ogToSave, ogToSave.size())
            );
        });

        when(metadataService.prepareAttachmentUnits(any(), anyString())).thenReturn(new HashMap<>());

        try (
            final InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(
                "streamZip/simple_zip_with_csv_and_ObjectFiles.zip"
            )
        ) {
            fluxService.processStream(resourceAsStream, PROJECT_ID, TRANSACTION_ID, null, null);
        }

        JsonNode expectedUnits = JsonHandler.getFromFile(
            PropertiesUtils.getResourceFile("streamZip/expected_simple_zip_with_ObjectFiles_units.json")
        );
        JsonNode expectedObjectGroups = JsonHandler.getFromFile(
            PropertiesUtils.getResourceFile("streamZip/expected_simple_zip_with_ObjectFiles_object_groups.json")
        );

        LOGGER.debug("Actual units: {}", units);
        LOGGER.debug("Actual object groups: {}", objectGroups);

        assertMetadataEquals(
            expectedUnits,
            expectedObjectGroups,
            JsonHandler.toJsonNode(units.values()),
            JsonHandler.toJsonNode(objectGroups.values())
        );
    }

    @Test
    @RunWithCustomExecutor
    public void processStream_with_jsonl_and_ObjectFiles_ok() throws Exception {
        when(projectRepository.findProjectById(anyString())).thenReturn(Optional.of(projectModel));
        Map<String, JsonNode> units = new HashMap<>();
        Map<String, JsonNode> objectGroups = new HashMap<>();
        when(metadataRepository.saveArchiveUnits(ArgumentMatchers.anyList())).thenAnswer(e -> {
            final List<ObjectNode> unitsToSave = e.getArgument(0);
            for (ObjectNode unit : unitsToSave) {
                units.put(unit.get(VitamFieldsHelper.id()).asText(), unit);
            }
            return JsonHandler.toJsonNode(
                new RequestResponseOK<>(JsonHandler.createObjectNode(), unitsToSave, unitsToSave.size())
            );
        });

        when(metadataRepository.saveObjectGroups(anyList())).thenAnswer(e -> {
            final List<ObjectNode> ogToSave = e.getArgument(0);
            for (ObjectNode og : ogToSave) {
                objectGroups.put(og.get(VitamFieldsHelper.id()).asText(), og);
            }
            return JsonHandler.toJsonNode(
                new RequestResponseOK<>(JsonHandler.createObjectNode(), ogToSave, ogToSave.size())
            );
        });

        when(metadataService.prepareAttachmentUnits(any(), anyString())).thenReturn(new HashMap<>());

        try (
            final InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(
                "streamZip/simple_zip_with_jsonl_and_ObjectFiles.zip"
            )
        ) {
            fluxService.processStream(resourceAsStream, PROJECT_ID, TRANSACTION_ID, null, null);
        }

        JsonNode expectedUnits = JsonHandler.getFromFile(
            PropertiesUtils.getResourceFile("streamZip/expected_simple_zip_with_ObjectFiles_units.json")
        );
        JsonNode expectedObjectGroups = JsonHandler.getFromFile(
            PropertiesUtils.getResourceFile("streamZip/expected_simple_zip_with_ObjectFiles_object_groups.json")
        );

        LOGGER.debug("Actual units: {}", units);
        LOGGER.debug("Actual object groups: {}", objectGroups);

        assertMetadataEquals(
            expectedUnits,
            expectedObjectGroups,
            JsonHandler.toJsonNode(units.values()),
            JsonHandler.toJsonNode(objectGroups.values())
        );
    }

    @Test
    @RunWithCustomExecutor
    public void processStream_with_ObjectFiles_with_duplicate_File_and_ObjectFiles_ko() throws Exception {
        when(projectRepository.findProjectById(anyString())).thenReturn(Optional.of(projectModel));

        when(metadataService.prepareAttachmentUnits(any(), anyString())).thenReturn(new HashMap<>());

        try (
            final InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(
                "streamZip/simple_zip_with_csv_and_ObjectFiles_with_duplicate_File_and_ObjectFiles.zip"
            )
        ) {
            // When
            ThrowableAssert.ThrowingCallable invocation = () ->
                fluxService.processStream(resourceAsStream, PROJECT_ID, TRANSACTION_ID, null, null);

            // Then
            assertThatThrownBy(invocation)
                .isInstanceOf(CollectInternalErrorsDetailsException.class)
                .hasMessage(
                    "An unexpected error occurs when try to upload the ZIP: Duplicate File or #uploadPath selector declaration for 'SomeFile.xml'"
                );
        }

        verify(metadataRepository, never()).saveArchiveUnits(anyList());
        verify(metadataRepository, never()).saveObjectGroups(anyList());
    }

    @Test
    @RunWithCustomExecutor
    public void processStream_with_ObjectFiles_with_duplicates_File_ko() throws Exception {
        when(projectRepository.findProjectById(anyString())).thenReturn(Optional.of(projectModel));

        when(metadataService.prepareAttachmentUnits(any(), anyString())).thenReturn(new HashMap<>());

        try (
            final InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(
                "streamZip/simple_zip_with_csv_and_ObjectFiles_with_duplicate_File.zip"
            )
        ) {
            // When
            ThrowableAssert.ThrowingCallable invocation = () ->
                fluxService.processStream(resourceAsStream, PROJECT_ID, TRANSACTION_ID, null, null);

            // Then
            assertThatThrownBy(invocation)
                .isInstanceOf(CollectInternalErrorsDetailsException.class)
                .hasMessage(
                    "An unexpected error occurs when try to upload the ZIP: Duplicate File or #uploadPath selector declaration for 'My Root Folder'"
                );
        }

        verify(metadataRepository, never()).saveArchiveUnits(anyList());
        verify(metadataRepository, never()).saveObjectGroups(anyList());
    }

    @Test
    @RunWithCustomExecutor
    public void processStream_with_ObjectFiles_with_duplicates_ObjectFiles_than_KO() throws Exception {
        when(projectRepository.findProjectById(anyString())).thenReturn(Optional.of(projectModel));

        when(metadataService.prepareAttachmentUnits(any(), anyString())).thenReturn(new HashMap<>());

        try (
            final InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(
                "streamZip/simple_zip_with_csv_and_ObjectFiles_with_duplicate_ObjectFiles.zip"
            )
        ) {
            // When
            ThrowableAssert.ThrowingCallable invocation = () ->
                fluxService.processStream(resourceAsStream, PROJECT_ID, TRANSACTION_ID, null, null);

            // Then
            assertThatThrownBy(invocation)
                .isInstanceOf(CollectInternalErrorsDetailsException.class)
                .hasMessage(
                    "An unexpected error occurs when try to upload the ZIP: Duplicate ObjectFiles declaration for 'My Root Folder/MyFile2.txt'"
                );
        }

        verify(metadataRepository, never()).saveArchiveUnits(anyList());
        verify(metadataRepository, never()).saveObjectGroups(anyList());
    }

    @Test
    @RunWithCustomExecutor
    public void processStream_with_ObjectFiles_with_ObjectFiles_set_to_object_KO() throws Exception {
        when(projectRepository.findProjectById(anyString())).thenReturn(Optional.of(projectModel));

        when(metadataService.prepareAttachmentUnits(any(), anyString())).thenReturn(new HashMap<>());

        try (
            final InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(
                "streamZip/simple_zip_with_csv_and_ObjectFiles_with_ObjectFiles_set_to_object_ko.zip"
            )
        ) {
            // When
            ThrowableAssert.ThrowingCallable invocation = () ->
                fluxService.processStream(resourceAsStream, PROJECT_ID, TRANSACTION_ID, null, null);

            // Then
            assertThatThrownBy(invocation)
                .isInstanceOf(CollectInternalErrorsDetailsException.class)
                .hasMessage(
                    "An unexpected error occurs when try to upload the ZIP: ObjectFiles value 'SomeFile.xml' can only be set when File or #uploadPath selector 'My Root Folder/MyFile1.txt' is a directory"
                );
        }

        verify(metadataRepository, never()).saveArchiveUnits(anyList());
        verify(metadataRepository, never()).saveObjectGroups(anyList());
    }

    @Test
    @RunWithCustomExecutor
    public void processStream_with_ObjectFiles_with_reference_to_folder_KO() throws Exception {
        when(projectRepository.findProjectById(anyString())).thenReturn(Optional.of(projectModel));

        when(metadataService.prepareAttachmentUnits(any(), anyString())).thenReturn(new HashMap<>());

        try (
            final InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(
                "streamZip/simple_zip_with_csv_and_ObjectFiles_with_reference_to_folder_ko.zip"
            )
        ) {
            // When
            ThrowableAssert.ThrowingCallable invocation = () ->
                fluxService.processStream(resourceAsStream, PROJECT_ID, TRANSACTION_ID, null, null);

            // Then
            assertThatThrownBy(invocation)
                .isInstanceOf(CollectInternalErrorsDetailsException.class)
                .hasMessage(
                    "An unexpected error occurs when try to upload the ZIP: Invalid ObjectFiles value 'My Root Folder'. Must be a file"
                );
        }

        verify(metadataRepository, never()).saveArchiveUnits(anyList());
        verify(metadataRepository, never()).saveObjectGroups(anyList());
    }

    @Test
    @RunWithCustomExecutor
    public void processStream_with_ObjectFiles_with_reference_to_non_existing_file_KO() throws Exception {
        when(projectRepository.findProjectById(anyString())).thenReturn(Optional.of(projectModel));

        when(metadataService.prepareAttachmentUnits(any(), anyString())).thenReturn(new HashMap<>());

        try (
            final InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(
                "streamZip/simple_zip_with_csv_and_ObjectFiles_with_reference_to_non_existing_file.zip"
            )
        ) {
            // When
            ThrowableAssert.ThrowingCallable invocation = () ->
                fluxService.processStream(resourceAsStream, PROJECT_ID, TRANSACTION_ID, null, null);

            // Then
            assertThatThrownBy(invocation)
                .isInstanceOf(CollectInternalErrorsDetailsException.class)
                .hasMessage(
                    "An unexpected error occurs when try to upload the ZIP: Invalid ObjectFiles value 'Unknown/file.Txt'. No such file"
                );
        }

        verify(metadataRepository, never()).saveArchiveUnits(anyList());
        verify(metadataRepository, never()).saveObjectGroups(anyList());
    }

    @Test
    @RunWithCustomExecutor
    public void processStream_with_csv_metadata_static_dynamic_attachement_and_UpdateOperation() throws Exception {
        // Given
        ProjectModel project = createProject();

        when(projectRepository.findProjectById(project.getId())).thenReturn(Optional.of(project));
        doReturn(JsonHandler.createObjectNode()).when(metadataRepository).saveArchiveUnits(ArgumentMatchers.anyList());

        when(metadataService.prepareAttachmentUnits(any(), anyString())).thenReturn(
            Map.of("rootUnitUp", "static_attachment_guid", "unit1", "guid_attachment1", "unit2", "guid_attachment2")
        );

        List<CollectJsonMetadataLine> unitUpdates = new ArrayList<>();
        doAnswer(e -> {
            try (
                JsonLineIterator<CollectJsonMetadataLine> metadata = new JsonLineIterator<>(
                    e.getArgument(1),
                    CollectJsonMetadataLine.TYPE_REFERENCE
                )
            ) {
                metadata.forEachRemaining(unitUpdates::add);
            }
            return null;
        })
            .when(metadataService)
            .updateUnitsWithJsonlMetadataFile(eq("transactionId"), any());

        // When
        try (
            final InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(
                "streamZip/simple_zip_with_metadata_csv_and_update_operation.zip"
            )
        ) {
            fluxService.processStream(resourceAsStream, project.getId(), "transactionId", null, null);
        }

        // Then

        ArgumentCaptor<List<ObjectNode>> savedUnitsArgCaptor = ArgumentCaptor.forClass(List.class);
        verify(metadataRepository).saveArchiveUnits(savedUnitsArgCaptor.capture());

        checkInsertedUnits(
            savedUnitsArgCaptor.getValue(),
            "streamZip/expected_inserted_unit_with_csv_and_update_operation.json"
        );

        checkUpdatedUnits(unitUpdates, "streamZip/expected_updated_unit_with_csv_and_update_operation.json");
    }

    @Test
    @RunWithCustomExecutor
    public void processStream_with_jsonl_metadata_static_dynamic_attachement_and_UpdateOperation() throws Exception {
        // Given
        ProjectModel project = createProject();

        when(projectRepository.findProjectById(project.getId())).thenReturn(Optional.of(project));
        doReturn(JsonHandler.createObjectNode()).when(metadataRepository).saveArchiveUnits(ArgumentMatchers.anyList());

        when(metadataService.prepareAttachmentUnits(any(), anyString())).thenReturn(
            Map.of("rootUnitUp", "static_attachment_guid", "unit1", "guid_attachment1", "unit2", "guid_attachment2")
        );

        List<CollectJsonMetadataLine> unitUpdates = new ArrayList<>();
        doAnswer(e -> {
            try (
                JsonLineIterator<CollectJsonMetadataLine> metadata = new JsonLineIterator<>(
                    e.getArgument(1),
                    CollectJsonMetadataLine.TYPE_REFERENCE
                )
            ) {
                metadata.forEachRemaining(unitUpdates::add);
            }
            return null;
        })
            .when(metadataService)
            .updateUnitsWithJsonlMetadataFile(eq("transactionId"), any());

        // When
        try (
            final InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(
                "streamZip/simple_zip_with_metadata_jsonl_and_update_operation.zip"
            )
        ) {
            fluxService.processStream(resourceAsStream, project.getId(), "transactionId", null, null);
        }

        // Then

        ArgumentCaptor<List<ObjectNode>> savedUnitsArgCaptor = ArgumentCaptor.forClass(List.class);
        verify(metadataRepository).saveArchiveUnits(savedUnitsArgCaptor.capture());

        checkInsertedUnits(
            savedUnitsArgCaptor.getValue(),
            "streamZip/expected_inserted_unit_with_jsonl_and_update_operation.json"
        );

        checkUpdatedUnits(unitUpdates, "streamZip/expected_updated_unit_with_jsonl_and_update_operation.json");
    }

    private void assertMetadataEquals(
        JsonNode expectedUnits,
        JsonNode expectedObjectGroups,
        JsonNode actualUnits,
        JsonNode actualObjectGroups
    ) throws InvalidParseOperationException {
        Map<String, String> expectedIdsToPlaceHolderMap = replaceIds(expectedUnits);
        JsonNode expectedNormalizedUnits = replaceIdsWithPlaceHolders(expectedUnits, expectedIdsToPlaceHolderMap);
        JsonNode expectedNormalizedObjectGroups = replaceIdsWithPlaceHolders(
            expectedObjectGroups,
            expectedIdsToPlaceHolderMap
        );

        Map<String, String> actualIdsToPlaceHolderMap = replaceIds(actualUnits);
        JsonNode actualNormalizedUnits = replaceIdsWithPlaceHolders(actualUnits, actualIdsToPlaceHolderMap);
        JsonNode actualNormalizedObjectGroups = replaceIdsWithPlaceHolders(
            actualObjectGroups,
            actualIdsToPlaceHolderMap
        );

        try {
            JsonAssert.assertJsonEquals(
                expectedNormalizedUnits,
                actualNormalizedUnits,
                JsonAssert.when(Option.IGNORING_ARRAY_ORDER).whenIgnoringPaths(
                    List.of("[*]." + VitamFieldsHelper.batchId())
                )
            );
        } catch (AssertionError e) {
            LOGGER.error("Expected Units: " + JsonHandler.prettyPrint(expectedNormalizedUnits));
            LOGGER.error("Actual Units: " + JsonHandler.prettyPrint(actualNormalizedUnits));
            throw e;
        }

        try {
            JsonAssert.assertJsonEquals(
                expectedNormalizedObjectGroups,
                actualNormalizedObjectGroups,
                JsonAssert.when(Option.IGNORING_ARRAY_ORDER).whenIgnoringPaths(
                    List.of(
                        "[*]." + VitamFieldsHelper.batchId(),
                        "[*]." + TAG_FILE_INFO + "." + FileInfoModel.LAST_MODIFIED,
                        "[*]." +
                        VitamFieldsHelper.qualifiers() +
                        "[*]." +
                        TAG_VERSIONS +
                        "[*]." +
                        VitamFieldsHelper.id(),
                        "[*]." +
                        VitamFieldsHelper.qualifiers() +
                        "[*]." +
                        TAG_VERSIONS +
                        "[*]." +
                        TAG_FILE_INFO +
                        "." +
                        FileInfoModel.LAST_MODIFIED,
                        "[*]." + VitamFieldsHelper.qualifiers() + "[*]." + TAG_VERSIONS + "[*]." + TAG_URI
                    )
                )
            );
        } catch (AssertionError e) {
            LOGGER.error("Expected ObjectGroups: " + JsonHandler.prettyPrint(expectedNormalizedObjectGroups));
            LOGGER.error("Actual ObjectGroups: " + JsonHandler.prettyPrint(actualNormalizedObjectGroups));
            throw e;
        }
    }

    private Map<String, String> replaceIds(JsonNode units) {
        Map<String, String> replacements = new HashMap<>();
        for (JsonNode unit : units) {
            String id = unit.get(VitamFieldsHelper.id()).asText();

            String title = unit.has("Title") ? unit.get("Title").asText() : null;

            if (title != null && title.startsWith(MetadataHelper.STATIC_ATTACHMENT)) {
                replacements.put(id, "#ID-UNIT-STATIC_ATTACHMENT");
                assertThat(unit.has(VitamFieldsHelper.object())).isFalse();
            } else if (title != null && title.startsWith(MetadataHelper.DYNAMIC_ATTACHEMENT)) {
                replacements.put(id, "#ID-UNIT-" + unit.get("Title").asText());
                assertThat(unit.has(VitamFieldsHelper.object())).isFalse();
            } else {
                assertThat(unit.has(VitamFieldsHelper.uploadPath())).isTrue();
                String uploadPath = unit.get(VitamFieldsHelper.uploadPath()).asText();
                replacements.put(id, "#ID-UNIT-" + uploadPath);

                if (unit.has(VitamFieldsHelper.object())) {
                    String objectGroupId = unit.get(VitamFieldsHelper.object()).asText();
                    replacements.put(objectGroupId, "#ID-OG-" + uploadPath);
                }
            }
        }
        return replacements;
    }

    private static JsonNode replaceIdsWithPlaceHolders(JsonNode units, Map<String, String> replacements)
        throws InvalidParseOperationException {
        String jsonString = JsonHandler.unprettyPrint(units);
        for (String id : replacements.keySet()) {
            jsonString = jsonString.replaceAll(id, replacements.get(id));
        }
        List<JsonNode> entries = IteratorUtils.toList(JsonHandler.getFromString(jsonString).elements());
        entries.sort(Comparator.comparing(e -> e.get(VitamFieldsHelper.id()).asText()));
        return JsonHandler.toJsonNode(entries);
    }

    private static void checkInsertedUnits(List<ObjectNode> actual, String expectedResourceFile)
        throws InvalidParseOperationException, FileNotFoundException {
        JsonAssert.assertJsonEquals(
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(expectedResourceFile)),
            actual,
            JsonAssert.when(Option.IGNORING_ARRAY_ORDER).whenIgnoringPaths(
                List.of(
                    "[*]." + VitamFieldsHelper.id(),
                    "[*]." + VitamFieldsHelper.batchId(),
                    "[*]." + VitamFieldsHelper.object()
                )
            )
        );
    }

    private static void checkUpdatedUnits(
        List<CollectJsonMetadataLine> unitUpdates,
        String expectedUnitUpdateResourcesFile
    ) throws InvalidParseOperationException, FileNotFoundException {
        JsonAssert.assertJsonEquals(
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(expectedUnitUpdateResourcesFile)),
            unitUpdates,
            JsonAssert.when(Option.IGNORING_ARRAY_ORDER)
        );
    }

    private static ProjectModel createTestProjectWithJsltTransformation() {
        ProjectModel project = createProject();
        project.setTransformationRules(
            """
            {
              "Title": .Title + " - TRANSFORMED",
              "Key": "2",
              *: .
            }
            """
        );
        return project;
    }

    private static ProjectModel createProject() {
        return new ProjectModel.Builder()
            .id(GUIDFactory.newGUID().getId())
            .name("projectName")
            .manifestContext(
                new ManifestContext(
                    "acquisitionInformation",
                    "legalStatus",
                    "archivalAgreement",
                    "messageIdentifier",
                    "archivalAgencyIdentifier",
                    "transferringAgencyIdentifier",
                    "originatingAgencyIdentifier",
                    "submissionAgencyIdentifier",
                    "archivalProfil",
                    "comment"
                )
            )
            .status(ProjectStatus.OPEN)
            .creationDate("creation date")
            .lastUpdate("last update")
            .unitUp("rootUnitUp")
            .unitUps(List.of(new MetadataUnitUp("unit1", "Key", "1"), new MetadataUnitUp("unit2", "Key", "2")))
            .tenant(TENANT_ID)
            .automaticIngest(false)
            .archivingSystemId(null)
            .archivingSystemTenant(null)
            .connectedToArchivingSystem(null)
            .transformationRules(null)
            .build();
    }

    public RequestResponse<SchemaResponse> loadUnitSchema() throws InvalidParseOperationException, IOException {
        List<SchemaResponse> unitSchemaModels = JsonHandler.getFromInputStreamAsTypeReference(
            PropertiesUtils.getResourceAsStream("unit-schema-with-custom-fields.json"),
            new TypeReference<>() {}
        );
        return new RequestResponseOK<SchemaResponse>().addAllResults(unitSchemaModels);
    }
}
