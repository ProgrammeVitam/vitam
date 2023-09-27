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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.collect.common.exception.CollectInternalException;
import fr.gouv.vitam.collect.internal.core.common.ManifestContext;
import fr.gouv.vitam.collect.internal.core.common.ProjectModel;
import fr.gouv.vitam.collect.internal.core.common.TransactionModel;
import fr.gouv.vitam.collect.internal.core.repository.MetadataRepository;
import fr.gouv.vitam.collect.internal.core.repository.ProjectRepository;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.format.identification.model.FormatIdentifierResponse;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.objectgroup.FileInfoModel;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.common.tmp.TempFolderRule;
import net.javacrumbs.jsonunit.JsonAssert;
import net.javacrumbs.jsonunit.core.Option;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static fr.gouv.vitam.collect.internal.core.service.FluxService.METADATA_CSV_FILE;
import static fr.gouv.vitam.common.SedaConstants.TAG_FILE_INFO;
import static fr.gouv.vitam.common.SedaConstants.TAG_URI;
import static fr.gouv.vitam.common.SedaConstants.TAG_VERSIONS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FluxServiceTest {

    private static final int TENANT_ID = 0;
    private static final String TRANSACTION_ID = "TRANSACTION_ID";
    private static final String PROJECT_ID = "PROJECT_ID";

    private static final String UNITS_PATH = "streamZip/units.json";

    private static final String OBJECTGROUPS_PATH = "streamZip/objectgroups.json";

    private static final String TRANSACTION_ZIP_PATH = "streamZip/transaction.zip";

    private static final String TRANSACTION2_ZIP_PATH = "streamZip/transaction2.zip";

    @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule public TempFolderRule tempFolder = new TempFolderRule();

    @Rule public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @Mock private CollectService collectService;
    @Mock private MetadataService metadataService;

    @Mock private ProjectRepository projectRepository;
    @Mock private MetadataRepository metadataRepository;

    @InjectMocks private FluxService fluxService;

    private TransactionModel transactionModel;
    private ProjectModel projectModel;

    @Before
    public void setUp() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(TENANT_ID).getId());

        transactionModel = new TransactionModel();
        transactionModel.setId(TRANSACTION_ID);
        transactionModel.setProjectId(PROJECT_ID);
        projectModel = new ProjectModel();
        projectModel.setId(PROJECT_ID);

        projectModel.setManifestContext(new ManifestContext());
        when(collectService.detectFileFormat(any(File.class))).thenReturn(
            Optional.of(new FormatIdentifierResponse("", "", "", "")));
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
                new RequestResponseOK<>(JsonHandler.createObjectNode(), unitsToSave, unitsToSave.size()));
        });

        when(metadataRepository.saveObjectGroups(anyList())).thenAnswer(e -> {
            final List<ObjectNode> ogToSave = e.getArgument(0);
            for (ObjectNode og : ogToSave) {
                objectGroups.put(og.get(VitamFieldsHelper.id()).asText(), og);
            }
            return JsonHandler.toJsonNode(
                new RequestResponseOK<>(JsonHandler.createObjectNode(), ogToSave, ogToSave.size()));
        });

        when(metadataService.prepareAttachmentUnits(any(), anyString())).thenReturn(new HashMap<>());


        try (final InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(TRANSACTION_ZIP_PATH)) {
            fluxService.processStream(resourceAsStream, transactionModel);
        }


        final JsonNode expectedUnits = JsonHandler.getFromFile(PropertiesUtils.getResourceFile(UNITS_PATH));

        JsonAssert.assertJsonEquals(units.values(), expectedUnits, JsonAssert.when(Option.IGNORING_ARRAY_ORDER)
            .whenIgnoringPaths(List.of("[*]." + VitamFieldsHelper.id(), "[*]." + VitamFieldsHelper.unitups(),
                "[*]." + VitamFieldsHelper.object())));

        final JsonNode expectedGots = JsonHandler.getFromFile(PropertiesUtils.getResourceFile(OBJECTGROUPS_PATH));

        JsonAssert.assertJsonEquals(JsonHandler.toJsonNode(objectGroups.values()), expectedGots,
            JsonAssert.when(Option.IGNORING_ARRAY_ORDER).whenIgnoringPaths(List.of("[*]." + VitamFieldsHelper.id(),
                "[*]." + VitamFieldsHelper.qualifiers() + "[*]." + TAG_VERSIONS + "[*]." + VitamFieldsHelper.id(),
                "[*]." + TAG_FILE_INFO + "." + FileInfoModel.LAST_MODIFIED,
                "[*]." + VitamFieldsHelper.qualifiers() + "[*]." + TAG_VERSIONS + "[*]." + TAG_FILE_INFO + "." +
                    FileInfoModel.LAST_MODIFIED,
                "[*]." + VitamFieldsHelper.qualifiers() + "[*]." + TAG_VERSIONS + "[*]." + TAG_URI)));
    }

    @Test
    @RunWithCustomExecutor
    public void processStream_with_metadata_update() throws Exception {
        when(projectRepository.findProjectById(anyString())).thenReturn(Optional.of(projectModel));

        final AtomicReference<File> fileReference = new AtomicReference<>();
        when(collectService.pushStreamToWorkspace(any(), any(InputStream.class), eq(METADATA_CSV_FILE))).thenAnswer(
            (e) -> {
                final InputStream is = e.getArgument(1);
                final File file = tempFolder.newFile(METADATA_CSV_FILE);
                Files.copy(is, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                fileReference.set(file);
                return "";
            });

        when(collectService.getInputStreamFromWorkspace(any(), eq(METADATA_CSV_FILE))).thenAnswer(
            (e) -> new FileInputStream(fileReference.get()));

        when(metadataService.prepareAttachmentUnits(any(), anyString())).thenReturn(new HashMap<>());

        try (final InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(TRANSACTION2_ZIP_PATH)) {
            fluxService.processStream(resourceAsStream, transactionModel);
        }

        verify(metadataService).updateUnitsWithMetadataFile(eq("TRANSACTION_ID"), any());
    }

    private static final String TRANSACTION_WITHOUT_FILE_COLUMN_ZIP_PATH =
        "streamZip/transaction_without_file_column.zip";

    @Test
    @RunWithCustomExecutor
    public void processStream_without_file_column_in_csv_file() throws Exception {
        // Given
        when(projectRepository.findProjectById(anyString())).thenReturn(Optional.of(projectModel));
        final AtomicReference<File> fileReference = new AtomicReference<>();
        when(metadataService.prepareAttachmentUnits(any(), anyString())).thenReturn(new HashMap<>());
        when(collectService.pushStreamToWorkspace(any(), any(InputStream.class), eq(METADATA_CSV_FILE))).thenAnswer(
            (e) -> {
                final InputStream is = e.getArgument(1);
                final File file = tempFolder.newFile(METADATA_CSV_FILE);
                Files.copy(is, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                fileReference.set(file);
                return "";
            });
        when(collectService.getInputStreamFromWorkspace(any(), eq(METADATA_CSV_FILE))).thenAnswer(
            (e) -> new FileInputStream(fileReference.get()));

        // When
        try (final InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(
            TRANSACTION_WITHOUT_FILE_COLUMN_ZIP_PATH)) {
            CollectInternalException exception = Assert.assertThrows(CollectInternalException.class,
                () -> fluxService.processStream(resourceAsStream, transactionModel));
            Assert.assertEquals("Mapping for File not found, expected one of [Content.DescriptionLevel, Content.Title]",
                exception.getMessage());
        }

        // Then
        // bulkWriteUnits
        verify(metadataRepository, never()).saveArchiveUnits(any());
        // bulkWriteObjectGroups
        verify(metadataRepository, never()).saveObjectGroups(any());
    }
}