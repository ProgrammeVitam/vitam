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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.culture.archivesdefrance.seda.v2.UpdateOperationType;
import fr.gouv.vitam.collect.common.dto.BulkAtomicUpdateResult;
import fr.gouv.vitam.collect.common.dto.BulkAtomicUpdateStatus;
import fr.gouv.vitam.collect.common.dto.MetadataUnitUp;
import fr.gouv.vitam.collect.internal.core.common.ManifestContext;
import fr.gouv.vitam.collect.internal.core.common.ProjectModel;
import fr.gouv.vitam.collect.internal.core.common.TransactionModel;
import fr.gouv.vitam.collect.internal.core.repository.MetadataRepository;
import fr.gouv.vitam.collect.internal.core.repository.ProjectRepository;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.utils.ScrollSpliterator;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.schema.SchemaResponse;
import fr.gouv.vitam.common.model.unit.ArchiveUnitModel;
import fr.gouv.vitam.common.model.unit.ManagementModel;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.common.tmp.TempFolderRule;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import net.javacrumbs.jsonunit.JsonAssert;
import net.javacrumbs.jsonunit.core.Option;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MetadataServiceTest {

    private static final String TRANSACTION_ID = "TRANSACTION_ID";
    private static final String PROJECT_ID = "PROJECT_ID";
    private static final String UNIT_ID = "UNIT_ID";
    private static final int TENANT_ID = 0;
    private static final String UNITS_WITH_GRAPH_PATH = "streamZip/units_with_graph.json";
    private static final String QUERIES_PATH = "streamZip/queries.json";
    private static final String METADATA_CSV_FILE = "update/metadata.csv";
    private static final String METADATA_JSONL_FILE = "update/metadata_update.jsonl";
    private static final String UNIT_FILE = "collect_unit.json";
    private static final String UNIT_UP = "UNIT_UP";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public TempFolderRule tempFolder = new TempFolderRule();

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Mock
    private MetadataRepository metadataRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private AdminManagementClient adminManagementClient;

    @Mock
    private AdminManagementClientFactory adminManagementClientFactory;

    @Mock
    private BulkAtomicUpdateMetadataService bulkAtomicUpdateMetadataService;

    @InjectMocks
    private MetadataService metadataService;

    private TransactionModel transactionModel;

    private ProjectModel projectModel;

    @Before
    public void setUp() throws Exception {
        doReturn(adminManagementClient).when(adminManagementClientFactory).getClient();
        doReturn(loadUnitSchema()).when(adminManagementClient).getUnitSchema();

        transactionModel = new TransactionModel();
        transactionModel.setId(TRANSACTION_ID);
        transactionModel.setProjectId(PROJECT_ID);

        projectModel = new ProjectModel();
        projectModel.setId(PROJECT_ID);
        projectModel.setManifestContext(new ManifestContext());
    }

    @Test
    public void selectUnitById() throws Exception {
        metadataService.selectUnitById(UNIT_ID);
        verify(metadataRepository).selectUnitById(UNIT_ID);
    }

    @Test
    public void selectObjectGroupById() throws Exception {
        metadataService.selectObjectGroupById("OBJECTGROUP_ID");
        verify(metadataRepository).selectObjectGroupById("OBJECTGROUP_ID", true);
    }

    @Test
    @RunWithCustomExecutor
    public void saveArchiveUnit_without_attachment() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        try (InputStream is = PropertiesUtils.getResourceAsStream(UNIT_FILE)) {
            ObjectNode unit = (ObjectNode) JsonHandler.getFromInputStream(is);

            when(projectRepository.findProjectById(eq("PROJECT_ID"))).thenReturn(Optional.of(projectModel));

            metadataService.saveArchiveUnit(unit, transactionModel);

            verify(metadataRepository).saveArchiveUnit(any());
        }
    }

    @Test
    @RunWithCustomExecutor
    public void save_no_root_ArchiveUnit() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        try (InputStream is = PropertiesUtils.getResourceAsStream(UNIT_FILE)) {
            ObjectNode unit = (ObjectNode) JsonHandler.getFromInputStream(is);
            projectModel.setUnitUp(UNIT_UP);
            unit.set(VitamFieldsHelper.unitups(), JsonHandler.createArrayNode().add("ID"));

            when(projectRepository.findProjectById(eq(PROJECT_ID))).thenReturn(Optional.of(projectModel));

            when(metadataRepository.selectUnits(any(JsonNode.class), anyString())).thenReturn(
                new RequestResponseOK<>()
            );

            AtomicReference<String> unitUp = new AtomicReference<>();
            when(metadataRepository.saveArchiveUnits(anyList())).thenAnswer(a -> {
                List<ObjectNode> units = a.getArgument(0);
                unitUp.set(units.get(0).get(VitamFieldsHelper.id()).asText());
                return null;
            });

            metadataService.saveArchiveUnit(unit, transactionModel);

            verify(metadataRepository).saveArchiveUnit(
                ArgumentMatchers.argThat(
                    e ->
                        !e.get(VitamFieldsHelper.unitups()).get(0).asText().equals(unitUp.get()) &&
                        e.get(VitamFieldsHelper.unitups()).get(0).asText().equals("ID")
                )
            );
        }
    }

    @Test
    @RunWithCustomExecutor
    public void saveArchiveUnit_with_simple_attachment() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        try (InputStream is = PropertiesUtils.getResourceAsStream(UNIT_FILE)) {
            ObjectNode unit = (ObjectNode) JsonHandler.getFromInputStream(is);
            projectModel.setUnitUp(UNIT_UP);

            when(projectRepository.findProjectById(eq(PROJECT_ID))).thenReturn(Optional.of(projectModel));

            when(metadataRepository.selectUnits(any(JsonNode.class), anyString())).thenReturn(
                new RequestResponseOK<>()
            );

            AtomicReference<String> unitUp = new AtomicReference<>();
            when(metadataRepository.saveArchiveUnits(anyList())).thenAnswer(a -> {
                List<ObjectNode> units = a.getArgument(0);
                unitUp.set(units.get(0).get(VitamFieldsHelper.id()).asText());
                return null;
            });

            metadataService.saveArchiveUnit(unit, transactionModel);

            verify(metadataRepository).saveArchiveUnit(
                ArgumentMatchers.argThat(e -> e.get(VitamFieldsHelper.unitups()).get(0).asText().equals(unitUp.get()))
            );
        }
    }

    @Test
    @RunWithCustomExecutor
    public void saveArchiveUnit_with_dynamic_attachment() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        try (InputStream is = PropertiesUtils.getResourceAsStream(UNIT_FILE)) {
            ObjectNode unit = (ObjectNode) JsonHandler.getFromInputStream(is);
            // Given
            projectModel.setUnitUp(UNIT_UP);

            MetadataUnitUp metadataUnitUp = new MetadataUnitUp();
            metadataUnitUp.setMetadataKey("Status");
            metadataUnitUp.setMetadataValue("Pret");
            metadataUnitUp.setUnitUp("DYNAMIC_UNIT_UP");

            projectModel.setUnitUps(List.of(metadataUnitUp));

            when(projectRepository.findProjectById(eq("PROJECT_ID"))).thenReturn(Optional.of(projectModel));

            when(metadataRepository.selectUnits(any(JsonNode.class), anyString())).thenReturn(
                new RequestResponseOK<>()
            );

            AtomicReference<String> unitUp = new AtomicReference<>();
            when(metadataRepository.saveArchiveUnits(anyList())).thenAnswer(a -> {
                List<ObjectNode> units = a.getArgument(0);
                unitUp.set(units.get(0).get(VitamFieldsHelper.id()).asText());
                return null;
            });

            // When
            metadataService.saveArchiveUnit(unit, transactionModel);

            // Then
            verify(metadataRepository).saveArchiveUnits(anyList());
            verify(metadataRepository).saveArchiveUnit(
                ArgumentMatchers.argThat(e -> !e.get(VitamFieldsHelper.unitups()).get(0).asText().equals(unitUp.get()))
            );
        }
    }

    @Test
    public void selectUnits() throws Exception {
        // Given
        when(metadataRepository.selectUnits(any(JsonNode.class), anyString())).thenReturn(new RequestResponseOK<>());
        // When
        metadataService.selectUnitsByTransactionId(JsonHandler.createObjectNode(), TRANSACTION_ID);
        // Then
        verify(metadataRepository).selectUnits(any(JsonNode.class), eq(TRANSACTION_ID));
    }

    @Test
    @RunWithCustomExecutor
    public void testUpdateUnitsWithCsvMetadata() throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(0).getId());
        AtomicReference<ArrayNode> requestReference = new AtomicReference<>();

        final List<JsonNode> unitsJson = JsonHandler.getFromFileAsTypeReference(
            PropertiesUtils.getResourceFile(UNITS_WITH_GRAPH_PATH),
            new TypeReference<>() {}
        );

        when(
            bulkAtomicUpdateMetadataService.bulkAtomicUpdateUnits(eq(transactionModel.getId()), any(), eq(true))
        ).thenAnswer(e -> {
            final ArrayNode argument = e.getArgument(1);
            requestReference.set(argument);
            return IntStream.range(0, argument.size())
                .mapToObj(i -> new BulkAtomicUpdateResult(BulkAtomicUpdateStatus.OK, "guid" + i, null))
                .collect(Collectors.toList());
        });

        when(metadataRepository.selectUnits(any(SelectMultiQuery.class), any())).thenReturn(
            new ScrollSpliterator<>(
                mock(SelectMultiQuery.class),
                query -> new RequestResponseOK<JsonNode>().addAllResults(new ArrayList<>(unitsJson)),
                0,
                0
            )
        );

        try (InputStream is = PropertiesUtils.getResourceAsStream(METADATA_CSV_FILE)) {
            metadataService.updateUnitsWithMetadataCsv(transactionModel, is);
        }

        final JsonNode expectedQueries = JsonHandler.getFromFile(PropertiesUtils.getResourceFile(QUERIES_PATH));

        JsonAssert.assertJsonEquals(
            JsonHandler.toJsonNode(requestReference.get()),
            expectedQueries,
            JsonAssert.when(Option.IGNORING_ARRAY_ORDER)
        );
    }

    @Test
    public void given_only_unitup_then_prepareAttachmentUnits_return_only_static() throws Exception {
        projectModel.setUnitUp("SYSTEM_ID");
        JsonNode unitJson = createAttachmentUnit("UNIT_ID", "SYSTEM_ID");

        when(metadataRepository.selectUnits(any(JsonNode.class), eq(TRANSACTION_ID))).thenReturn(
            new RequestResponseOK<JsonNode>().addResult(unitJson)
        );
        Map<String, String> result = metadataService.prepareAttachmentUnits(projectModel, TRANSACTION_ID);

        assertThat(result).containsOnlyKeys("SYSTEM_ID");
        assertThat(result.values()).containsOnly("UNIT_ID");
        verify(metadataRepository, never()).saveArchiveUnits(anyList());
    }

    @Test
    @RunWithCustomExecutor
    public void should_create_unit_when_prepareAttachmentUnits_with_unitup_only() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        projectModel.setUnitUp("SYSTEM_ID");
        when(metadataRepository.selectUnits(any(JsonNode.class), eq(TRANSACTION_ID))).thenReturn(
            new RequestResponseOK<>()
        );
        Map<String, String> result = metadataService.prepareAttachmentUnits(projectModel, TRANSACTION_ID);

        assertThat(result).containsOnlyKeys("SYSTEM_ID");
        assertThat(result.values()).hasSize(1);
        verify(metadataRepository, times(1)).saveArchiveUnits(anyList());
    }

    @Test
    @RunWithCustomExecutor
    public void should_create_unit_when_prepareAttachmentUnits_with_unitups_only() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        MetadataUnitUp up1 = new MetadataUnitUp();
        up1.setUnitUp("SYSTEM_ID1");
        up1.setMetadataKey("KEY1");
        up1.setMetadataKey("VALUE1");
        MetadataUnitUp up2 = new MetadataUnitUp();
        up2.setUnitUp("SYSTEM_ID2");
        up2.setMetadataKey("KEY2");
        up2.setMetadataKey("VALUE2");
        projectModel.setUnitUps(List.of(up1, up2));
        when(metadataRepository.selectUnits(any(JsonNode.class), eq(TRANSACTION_ID))).thenReturn(
            new RequestResponseOK<>()
        );
        Map<String, String> result = metadataService.prepareAttachmentUnits(projectModel, TRANSACTION_ID);

        assertThat(result).containsOnlyKeys("SYSTEM_ID1", "SYSTEM_ID2");
        assertThat(result.values()).hasSize(2);

        ArgumentCaptor<List<ObjectNode>> saveUnitsArgumentCaptor = ArgumentCaptor.forClass(List.class);
        verify(metadataRepository, times(1)).saveArchiveUnits(saveUnitsArgumentCaptor.capture());
        assertThat(saveUnitsArgumentCaptor.getAllValues()).hasSize(1);
        assertThat(saveUnitsArgumentCaptor.getAllValues().get(0)).hasSize(2);
        assertThat(saveUnitsArgumentCaptor.getAllValues().get(0))
            .extracting(unit -> unit.get("Title").asText())
            .containsExactlyInAnyOrder("DYNAMIC_ATTACHEMENT_SYSTEM_ID1", "DYNAMIC_ATTACHEMENT_SYSTEM_ID2");
    }

    @Test
    public void given_only_unitups_then_prepareAttachmentUnits_return_only_dynamic() throws Exception {
        MetadataUnitUp up1 = new MetadataUnitUp();
        up1.setUnitUp("SYSTEM_ID1");
        up1.setMetadataKey("KEY1");
        up1.setMetadataKey("VALUE1");
        MetadataUnitUp up2 = new MetadataUnitUp();
        up2.setUnitUp("SYSTEM_ID2");
        up2.setMetadataKey("KEY2");
        up2.setMetadataKey("VALUE2");
        projectModel.setUnitUps(List.of(up1, up2));
        JsonNode unitJson1 = createAttachmentUnit("UNIT_ID1", "SYSTEM_ID1");
        JsonNode unitJson2 = createAttachmentUnit("UNIT_ID2", "SYSTEM_ID2");

        when(metadataRepository.selectUnits(any(JsonNode.class), eq(TRANSACTION_ID))).thenReturn(
            new RequestResponseOK<JsonNode>().addResult(unitJson1).addResult(unitJson2)
        );
        Map<String, String> result = metadataService.prepareAttachmentUnits(projectModel, TRANSACTION_ID);

        assertThat(result).containsOnlyKeys("SYSTEM_ID1", "SYSTEM_ID2");
        assertThat(result.values()).containsOnly("UNIT_ID1", "UNIT_ID2");
        verify(metadataRepository, never()).saveArchiveUnits(anyList());
    }

    private static JsonNode createAttachmentUnit(String id, String systemId) throws InvalidParseOperationException {
        ArchiveUnitModel unit = new ArchiveUnitModel();
        unit.setId(id);
        ManagementModel managementModel = new ManagementModel();
        UpdateOperationType updateOperationType = new UpdateOperationType();
        updateOperationType.setSystemId(systemId);
        managementModel.setUpdateOperationType(updateOperationType);
        unit.setManagement(managementModel);

        return JsonHandler.toJsonNode(unit);
    }

    @Test
    public void testSelectUnitsByTransactionId() throws Exception {
        // Given
        when(metadataRepository.selectUnits(any(JsonNode.class), anyString())).thenReturn(new RequestResponseOK<>());
        // When
        metadataService.selectUnitsByTransactionId(JsonHandler.createObjectNode(), TRANSACTION_ID);
        // Then
        verify(metadataRepository).selectUnits(any(JsonNode.class), eq(TRANSACTION_ID));
    }

    @Test
    @RunWithCustomExecutor
    public void testUpdateUnitsWithJsonlMetadata() throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(0).getId());
        AtomicReference<ArrayNode> requestReference = new AtomicReference<>();

        final List<JsonNode> unitsJson = JsonHandler.getFromFileAsTypeReference(
            PropertiesUtils.getResourceFile(UNITS_WITH_GRAPH_PATH),
            new TypeReference<>() {}
        );

        when(
            bulkAtomicUpdateMetadataService.bulkAtomicUpdateUnits(eq(transactionModel.getId()), any(), eq(true))
        ).thenAnswer(e -> {
            final ArrayNode argument = e.getArgument(1);
            requestReference.set(argument);
            return IntStream.range(0, argument.size())
                .mapToObj(i -> new BulkAtomicUpdateResult(BulkAtomicUpdateStatus.OK, "guid" + i, null))
                .collect(Collectors.toList());
        });

        when(metadataRepository.selectUnits(any(SelectMultiQuery.class), any())).thenReturn(
            new ScrollSpliterator<>(
                mock(SelectMultiQuery.class),
                query -> new RequestResponseOK<JsonNode>().addAllResults(new ArrayList<>(unitsJson)),
                0,
                0
            )
        );

        // When
        try (InputStream is = PropertiesUtils.getResourceAsStream(METADATA_JSONL_FILE)) {
            metadataService.updateUnitsWithJsonlMetadata(transactionModel, is);
        }

        final JsonNode expectedQueries = JsonHandler.getFromFile(
            PropertiesUtils.getResourceFile("streamZip/queries_with_set_and_unset.json")
        );

        JsonAssert.assertJsonEquals(
            JsonHandler.toJsonNode(requestReference.get()),
            expectedQueries,
            JsonAssert.when(Option.IGNORING_ARRAY_ORDER)
        );
    }

    public RequestResponse<SchemaResponse> loadUnitSchema() throws InvalidParseOperationException, IOException {
        List<SchemaResponse> unitSchemaModels = JsonHandler.getFromInputStreamAsTypeReference(
            PropertiesUtils.getResourceAsStream("unit-schema-with-custom-fields.json"),
            new TypeReference<>() {}
        );
        return new RequestResponseOK<SchemaResponse>().addAllResults(unitSchemaModels);
    }
}
