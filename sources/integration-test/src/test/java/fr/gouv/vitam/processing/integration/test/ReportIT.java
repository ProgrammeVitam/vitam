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

package fr.gouv.vitam.processing.integration.test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import fr.gouv.vitam.batch.report.rest.BatchReportMain;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.VitamTestHelper;
import fr.gouv.vitam.common.accesslog.AccessLogUtils;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.database.api.VitamRepositoryFactory;
import fr.gouv.vitam.common.database.api.impl.VitamMongoRepository;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchIndexAlias;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.Contexts;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.processing.data.core.ProcessDataAccessImpl;
import fr.gouv.vitam.processing.engine.core.monitoring.ProcessMonitoringImpl;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.rest.DefaultOfferMain;
import fr.gouv.vitam.worker.server.rest.WorkerConfiguration;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import org.bson.Document;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.VitamTestHelper.waitOperation;
import static fr.gouv.vitam.common.stream.StreamUtils.consumeAnyEntityAndClose;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ReportIT extends VitamRuleRunner {

    private static final int TENANT_0 = 0;
    private static final String CONTEXT_ID = "fakeContextId";
    private static final String CONTRACT_ID = "AC-000001";
    private static final String STP_MASS_UPDATE_UNIT = "STP_MASS_UPDATE_UNIT";
    private static final String TITLE = "Title";
    private static final String QUERY = "query.json";
    private static final String ACTION = "actions.json";

    private static final String INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_00_JSON =
        "integration-processing/mass-update/unit_00.json";
    private static final String INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_LFC_00_JSON =
        "integration-processing/mass-update/unit_lfc_00.json";

    private static final String INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_05_JSON =
        "integration-processing/mass-update/unit_05.json";
    private static final String INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_LFC_05_JSON =
        "integration-processing/mass-update/unit_lfc_05.json";

    private static final String INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_06_JSON =
        "integration-processing/mass-update/unit_06.json";
    private static final String INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_LFC_06_JSON =
        "integration-processing/mass-update/unit_lfc_06.json";

    private static final String INTEGRATION_PROCESSING_MASS_UPDATE_UPDATE_QUERY_02_JSON =
        "integration-processing/mass-update/update_query_02.json";
    private static final String INTEGRATION_PROCESSING_MASS_UPDATE_RULE =
        "/integration-processing/mass-update/rules_referential.csv";
    private static final String INTEGRATION_PROCESSING_MASS_UPDATE_UPDATE_RULE =
        "integration-processing/mass-update/Action_update_rules.json";

    private static final TypeReference<List<Document>> TYPE_LIST_UNIT = new TypeReference<>() {};
    private ProcessingManagementClient processingClient;
    private static ProcessMonitoringImpl processMonitoring;
    private WorkspaceClient workspaceClient;

    @ClassRule
    public static VitamServerRunner runner = new VitamServerRunner(
        ProcessingIT.class,
        mongoRule.getMongoDatabase().getName(),
        ElasticsearchRule.getClusterName(),
        Sets.newHashSet(
            MetadataMain.class,
            WorkerMain.class,
            AdminManagementMain.class,
            LogbookMain.class,
            WorkspaceMain.class,
            ProcessManagementMain.class,
            StorageMain.class,
            DefaultOfferMain.class,
            BatchReportMain.class
        )
    );

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        handleBeforeClass(Arrays.asList(0, 1), Collections.emptyMap());
        PropertiesUtils.getResourcePath("integration-processing/bigworker.conf");
        FormatIdentifierFactory.getInstance().changeConfigurationFile(runner.FORMAT_IDENTIFIERS_CONF);
        processMonitoring = ProcessMonitoringImpl.getInstance();
        new DataLoader("integration-processing").prepareData();
    }

    @AfterClass
    public static void tearDownAfterClass() {
        handleAfterClass();
        runAfter();
        VitamClientFactory.resetConnections();
    }

    @After
    public void afterTest() {
        VitamThreadUtils.getVitamSession().setContractId(CONTRACT_ID);
        VitamThreadUtils.getVitamSession().setContextId(CONTEXT_ID);

        ProcessDataAccessImpl.getInstance().clearWorkflow();
        runAfterMongo(
            Sets.newHashSet(
                MetadataCollections.UNIT.getName(),
                MetadataCollections.OBJECTGROUP.getName(),
                FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getName(),
                FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getName(),
                LogbookCollections.OPERATION.getName(),
                LogbookCollections.LIFECYCLE_UNIT.getName(),
                LogbookCollections.LIFECYCLE_OBJECTGROUP.getName(),
                LogbookCollections.LIFECYCLE_OBJECTGROUP.getName(),
                LogbookCollections.LIFECYCLE_UNIT_IN_PROCESS.getName()
            )
        );

        runAfterEs(
            ElasticsearchIndexAlias.ofMultiTenantCollection(MetadataCollections.UNIT.getName(), 0),
            ElasticsearchIndexAlias.ofMultiTenantCollection(MetadataCollections.UNIT.getName(), 1),
            ElasticsearchIndexAlias.ofMultiTenantCollection(MetadataCollections.OBJECTGROUP.getName(), 0),
            ElasticsearchIndexAlias.ofMultiTenantCollection(MetadataCollections.OBJECTGROUP.getName(), 1),
            ElasticsearchIndexAlias.ofMultiTenantCollection(LogbookCollections.OPERATION.getName(), 0),
            ElasticsearchIndexAlias.ofMultiTenantCollection(LogbookCollections.OPERATION.getName(), 1),
            ElasticsearchIndexAlias.ofCrossTenantCollection(
                FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getName()
            ),
            ElasticsearchIndexAlias.ofCrossTenantCollection(
                FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getName()
            )
        );
    }

    @RunWithCustomExecutor
    @Test
    public void should_update_workflow_MASS_UPDATE_DESC_WARNING_when_at_least_one_KO_occurs() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        VitamThreadUtils.getVitamSession().setContractId(CONTRACT_ID);
        VitamThreadUtils.getVitamSession().setContextId(CONTEXT_ID);

        WorkerConfiguration workerConfiguration = new WorkerConfiguration();
        workerConfiguration.setCapacity(2);

        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_0);
        final String containerName = operationGuid.getId();
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        createLogbookOperation(operationGuid, operationGuid, null, LogbookTypeProcess.MASS_UPDATE);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);

        // insert 2 units and LFC
        insertUnitAndLFC(
            INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_00_JSON,
            INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_LFC_00_JSON
        );
        insertUnitAndLFC(
            INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_05_JSON,
            INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_LFC_05_JSON
        );

        // import contract
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_0));

        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        JsonNode query = JsonHandler.getFromFile(
            PropertiesUtils.findFile(INTEGRATION_PROCESSING_MASS_UPDATE_UPDATE_QUERY_02_JSON)
        );
        workspaceClient.putObject(operationGuid.getId(), QUERY, JsonHandler.writeToInpustream(query));
        workspaceClient.putObject(
            operationGuid.getId(),
            ACTION,
            JsonHandler.writeToInpustream(JsonHandler.createObjectNode())
        );
        processingClient.initVitamProcess(containerName, Contexts.MASS_UPDATE_UNIT_DESC.name());

        VitamTestHelper.runStepByStepUntilStepReached(containerName, "STP_CHECK_AND_COMPUTE");
        // delete unit to create a KO
        VitamRepositoryFactory.get()
            .getVitamMongoRepository(MetadataCollections.UNIT.getVitamCollection())
            .delete(Collections.singletonList("aeaqaaaaaagbcaacaang6ak4ts6paliaaaaq"), TENANT_0);

        processingClient.updateOperationActionProcess(ProcessAction.RESUME.getValue(), containerName);

        waitOperation(containerName);
        ProcessWorkflow processWorkflow = processMonitoring.findOneProcessWorkflow(containerName, TENANT_0);
        assertNotNull(processWorkflow);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(StatusCode.WARNING, processWorkflow.getStatus());
        VitamTestHelper.verifyLogbook(containerName, "MASS_UPDATE_FINALIZE", StatusCode.WARNING.name());

        // CHECK REPORT
        List<JsonNode> reportLines;
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            Response reportResponse = null;
            try {
                reportResponse = storageClient.getContainerAsync(
                    VitamConfiguration.getDefaultStrategy(),
                    operationGuid.getId() + ".jsonl",
                    DataCategory.REPORT,
                    AccessLogUtils.getNoLogAccessLog()
                );
                assertThat(reportResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
                reportLines = VitamTestHelper.getReport(reportResponse);
            } finally {
                consumeAnyEntityAndClose(reportResponse);
            }
        }
        assertThat(reportLines.size()).isEqualTo(4);
        assertThat(reportLines.get(0).get("outDetail").asText()).isEqualTo("MASS_UPDATE_UNITS.WARNING");
        assertThat(reportLines.get(1).get("vitamResults").get("OK").asInt()).isEqualTo(1);
        assertThat(reportLines.get(1).get("vitamResults").get("KO").asInt()).isEqualTo(1);
        assertThat(reportLines.get(1).get("vitamResults").get("WARNING").asInt()).isEqualTo(0);
        assertThat(reportLines.get(3).get("status").asText()).isEqualTo("KO");
    }

    @RunWithCustomExecutor
    @Test
    public void should_update_workflow_MASS_UPDATE_DESC_WARNING_when_at_least_one_KO_occurs_by_lots() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        VitamThreadUtils.getVitamSession().setContractId(CONTRACT_ID);
        VitamThreadUtils.getVitamSession().setContextId(CONTEXT_ID);

        WorkerConfiguration workerConfiguration = new WorkerConfiguration();
        workerConfiguration.setCapacity(1);
        VitamConfiguration.setWorkerBulkSize(1);
        VitamConfiguration.setDistributeurBatchSize(1);

        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_0);
        final String containerName = operationGuid.getId();
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        createLogbookOperation(operationGuid, operationGuid, null, LogbookTypeProcess.MASS_UPDATE);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);

        // insert units and LFC
        insertUnitAndLFC(
            INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_00_JSON,
            INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_LFC_00_JSON
        );
        insertUnitAndLFC(
            INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_05_JSON,
            INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_LFC_05_JSON
        );
        insertUnitAndLFC(
            INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_06_JSON,
            INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_LFC_06_JSON
        );

        // import contract
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_0));

        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        JsonNode query = JsonHandler.getFromFile(
            PropertiesUtils.findFile(INTEGRATION_PROCESSING_MASS_UPDATE_UPDATE_QUERY_02_JSON)
        );
        workspaceClient.putObject(operationGuid.getId(), QUERY, JsonHandler.writeToInpustream(query));
        workspaceClient.putObject(
            operationGuid.getId(),
            ACTION,
            JsonHandler.writeToInpustream(JsonHandler.createObjectNode())
        );
        processingClient.initVitamProcess(containerName, Contexts.MASS_UPDATE_UNIT_DESC.name());

        VitamTestHelper.runStepByStepUntilStepReached(containerName, "STP_CHECK_AND_COMPUTE");
        // delete unit to create a KO
        VitamRepositoryFactory.get()
            .getVitamMongoRepository(MetadataCollections.UNIT.getVitamCollection())
            .delete(Collections.singletonList("aeaqaaaaaagbcaacaang6ak4ts6paliaaaaq"), TENANT_0);
        VitamRepositoryFactory.get()
            .getVitamMongoRepository(MetadataCollections.UNIT.getVitamCollection())
            .delete(Collections.singletonList("aeaqaaaaaagbcaacaang6ak4ts6paliaaaah"), TENANT_0);

        processingClient.updateOperationActionProcess(ProcessAction.RESUME.getValue(), containerName);

        waitOperation(containerName);
        ProcessWorkflow processWorkflow = processMonitoring.findOneProcessWorkflow(containerName, TENANT_0);
        assertNotNull(processWorkflow);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(StatusCode.WARNING, processWorkflow.getStatus());
        VitamTestHelper.verifyLogbook(containerName, "MASS_UPDATE_UNITS", StatusCode.KO.name());
        VitamTestHelper.verifyLogbook(containerName, "MASS_UPDATE_FINALIZE", StatusCode.WARNING.name());

        // CHECK REPORT
        List<JsonNode> reportLines;
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            Response reportResponse = null;
            try {
                reportResponse = storageClient.getContainerAsync(
                    VitamConfiguration.getDefaultStrategy(),
                    operationGuid.getId() + ".jsonl",
                    DataCategory.REPORT,
                    AccessLogUtils.getNoLogAccessLog()
                );
                assertThat(reportResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
                reportLines = VitamTestHelper.getReport(reportResponse);
            } finally {
                consumeAnyEntityAndClose(reportResponse);
            }
        }
        assertThat(reportLines.size()).isEqualTo(5);
        assertThat(reportLines.get(0).get("outDetail").asText()).isEqualTo("MASS_UPDATE_UNITS.WARNING");
        assertThat(reportLines.get(1).get("vitamResults").get("OK").asInt()).isEqualTo(1);
        assertThat(reportLines.get(1).get("vitamResults").get("KO").asInt()).isEqualTo(2);
        assertThat(reportLines.get(1).get("vitamResults").get("WARNING").asInt()).isEqualTo(0);
        assertThat(reportLines.get(3).get("status").asText()).isEqualTo("KO");
        assertThat(reportLines.get(3).get("outcome").asText()).isEqualTo("MASS_UPDATE_UNITS.KO");
        assertThat(reportLines.get(4).get("status").asText()).isEqualTo("KO");
        assertThat(reportLines.get(4).get("outcome").asText()).isEqualTo("MASS_UPDATE_UNITS.KO");
    }

    @RunWithCustomExecutor
    @Test
    public void should_update_workflow_MASS_UPDATE_UNIT_WARNING_when_at_least_one_KO_occurs() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        VitamThreadUtils.getVitamSession().setContractId(CONTRACT_ID);
        VitamThreadUtils.getVitamSession().setContextId(CONTEXT_ID);

        try (AdminManagementClient functionalClient = AdminManagementClientFactory.getInstance().getClient()) {
            final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_0);
            final String containerName = operationGuid.getId();
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            createLogbookOperation(operationGuid, operationGuid, null, LogbookTypeProcess.MASS_UPDATE);
            workspaceClient = WorkspaceClientFactory.getInstance().getClient();
            workspaceClient.createContainer(containerName);

            // insert 2 units and LFC
            insertUnitAndLFC(
                INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_00_JSON,
                INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_LFC_00_JSON
            );
            insertUnitAndLFC(
                INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_05_JSON,
                INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_LFC_05_JSON
            );

            // import contract
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_0));
            functionalClient.importRulesFile(
                getClass().getResourceAsStream(INTEGRATION_PROCESSING_MASS_UPDATE_RULE),
                "Rules.json"
            );

            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            processingClient = ProcessingManagementClientFactory.getInstance().getClient();
            JsonNode query = JsonHandler.getFromFile(
                PropertiesUtils.findFile(INTEGRATION_PROCESSING_MASS_UPDATE_UPDATE_QUERY_02_JSON)
            );
            workspaceClient.putObject(operationGuid.getId(), QUERY, JsonHandler.writeToInpustream(query));
            JsonNode action = JsonHandler.getFromFile(
                PropertiesUtils.findFile(INTEGRATION_PROCESSING_MASS_UPDATE_UPDATE_RULE)
            );
            workspaceClient.putObject(operationGuid.getId(), ACTION, JsonHandler.writeToInpustream(action));
            processingClient.initVitamProcess(containerName, Contexts.MASS_UPDATE_UNIT_RULE.name());

            VitamTestHelper.runStepByStepUntilStepReached(containerName, "STP_INVALIDATE");
            // delete unit to create a KO
            VitamRepositoryFactory.get()
                .getVitamMongoRepository(MetadataCollections.UNIT.getVitamCollection())
                .delete(Collections.singletonList("aeaqaaaaaagbcaacaang6ak4ts6paliaaaaq"), TENANT_0);

            processingClient.updateOperationActionProcess(ProcessAction.RESUME.getValue(), containerName);

            waitOperation(containerName);
            ProcessWorkflow processWorkflow = processMonitoring.findOneProcessWorkflow(containerName, TENANT_0);
            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.WARNING, processWorkflow.getStatus());
            VitamTestHelper.verifyLogbook(containerName, "MASS_UPDATE_FINALIZE", StatusCode.WARNING.name());

            Optional<Document> updatedUnit = VitamRepositoryFactory.get()
                .getVitamMongoRepository(MetadataCollections.UNIT.getVitamCollection())
                .getByID("aeaqaaaaaagbcaacaang6ak4ts6paliaaaas", TENANT_0);
            assertTrue(updatedUnit.isPresent());
            String expected = "sous fonds";
            assertThat(updatedUnit.get().get(TITLE)).isEqualTo(expected);

            Optional<Document> deletedUnit = VitamRepositoryFactory.get()
                .getVitamMongoRepository(MetadataCollections.UNIT.getVitamCollection())
                .getByID("aeaqaaaaaagbcaacaang6ak4ts6paliaaaaq", TENANT_0);
            assertTrue(deletedUnit.isEmpty());

            // CHECK REPORT
            List<JsonNode> reportLines;
            try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
                Response reportResponse = null;
                try {
                    reportResponse = storageClient.getContainerAsync(
                        VitamConfiguration.getDefaultStrategy(),
                        operationGuid.getId() + ".jsonl",
                        DataCategory.REPORT,
                        AccessLogUtils.getNoLogAccessLog()
                    );
                    assertThat(reportResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
                    reportLines = VitamTestHelper.getReport(reportResponse);
                } finally {
                    consumeAnyEntityAndClose(reportResponse);
                }
            }
            assertThat(reportLines.size()).isEqualTo(4);
            assertThat(reportLines.get(0).get("outDetail").asText()).isEqualTo("MASS_UPDATE_UNITS_RULES.WARNING");
            assertThat(reportLines.get(1).get("vitamResults").get("OK").asInt()).isEqualTo(1);
            assertThat(reportLines.get(1).get("vitamResults").get("KO").asInt()).isEqualTo(1);
            assertThat(reportLines.get(1).get("vitamResults").get("WARNING").asInt()).isEqualTo(0);
            assertThat(reportLines.get(3).get("status").asText()).isEqualTo("KO");
        }
    }

    @RunWithCustomExecutor
    @Test
    public void should_update_workflow_MASS_UPDATE_UNIT_WARNING_when_at_least_one_KO_occurs_by_lots() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        VitamThreadUtils.getVitamSession().setContractId(CONTRACT_ID);
        VitamThreadUtils.getVitamSession().setContextId(CONTEXT_ID);

        try (AdminManagementClient functionalClient = AdminManagementClientFactory.getInstance().getClient()) {
            WorkerConfiguration workerConfiguration = new WorkerConfiguration();
            workerConfiguration.setCapacity(1);
            VitamConfiguration.setWorkerBulkSize(1);
            VitamConfiguration.setDistributeurBatchSize(1);

            final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_0);
            final String containerName = operationGuid.getId();
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            createLogbookOperation(operationGuid, operationGuid, null, LogbookTypeProcess.MASS_UPDATE);

            workspaceClient = WorkspaceClientFactory.getInstance().getClient();
            workspaceClient.createContainer(containerName);

            // insert units and LFC
            insertUnitAndLFC(
                INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_00_JSON,
                INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_LFC_00_JSON
            );
            insertUnitAndLFC(
                INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_05_JSON,
                INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_LFC_05_JSON
            );
            insertUnitAndLFC(
                INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_06_JSON,
                INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_LFC_06_JSON
            );

            // import contract
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_0));
            functionalClient.importRulesFile(
                getClass().getResourceAsStream(INTEGRATION_PROCESSING_MASS_UPDATE_RULE),
                "Rules.json"
            );

            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            processingClient = ProcessingManagementClientFactory.getInstance().getClient();
            JsonNode query = JsonHandler.getFromFile(
                PropertiesUtils.findFile(INTEGRATION_PROCESSING_MASS_UPDATE_UPDATE_QUERY_02_JSON)
            );
            workspaceClient.putObject(operationGuid.getId(), QUERY, JsonHandler.writeToInpustream(query));
            JsonNode action = JsonHandler.getFromFile(
                PropertiesUtils.findFile(INTEGRATION_PROCESSING_MASS_UPDATE_UPDATE_RULE)
            );
            workspaceClient.putObject(operationGuid.getId(), ACTION, JsonHandler.writeToInpustream(action));
            processingClient.initVitamProcess(containerName, Contexts.MASS_UPDATE_UNIT_RULE.name());

            VitamTestHelper.runStepByStepUntilStepReached(containerName, "STP_INVALIDATE");
            // delete unit to create a KO
            VitamRepositoryFactory.get()
                .getVitamMongoRepository(MetadataCollections.UNIT.getVitamCollection())
                .delete(Collections.singletonList("aeaqaaaaaagbcaacaang6ak4ts6paliaaaaq"), TENANT_0);
            VitamRepositoryFactory.get()
                .getVitamMongoRepository(MetadataCollections.UNIT.getVitamCollection())
                .delete(Collections.singletonList("aeaqaaaaaagbcaacaang6ak4ts6paliaaaah"), TENANT_0);

            processingClient.updateOperationActionProcess(ProcessAction.RESUME.getValue(), containerName);

            waitOperation(containerName);
            ProcessWorkflow processWorkflow = processMonitoring.findOneProcessWorkflow(containerName, TENANT_0);
            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.WARNING, processWorkflow.getStatus());
            VitamTestHelper.verifyLogbook(containerName, "MASS_UPDATE_UNITS_RULES", StatusCode.KO.name());
            VitamTestHelper.verifyLogbook(containerName, "MASS_UPDATE_FINALIZE", StatusCode.WARNING.name());

            // CHECK REPORT
            List<JsonNode> reportLines;
            try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
                Response reportResponse = null;
                try {
                    reportResponse = storageClient.getContainerAsync(
                        VitamConfiguration.getDefaultStrategy(),
                        operationGuid.getId() + ".jsonl",
                        DataCategory.REPORT,
                        AccessLogUtils.getNoLogAccessLog()
                    );
                    assertThat(reportResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
                    reportLines = VitamTestHelper.getReport(reportResponse);
                } finally {
                    consumeAnyEntityAndClose(reportResponse);
                }
            }
            assertThat(reportLines.size()).isEqualTo(5);
            assertThat(reportLines.get(0).get("outDetail").asText()).isEqualTo("MASS_UPDATE_UNITS_RULES.WARNING");
            assertThat(reportLines.get(1).get("vitamResults").get("OK").asInt()).isEqualTo(1);
            assertThat(reportLines.get(1).get("vitamResults").get("KO").asInt()).isEqualTo(2);
            assertThat(reportLines.get(1).get("vitamResults").get("WARNING").asInt()).isEqualTo(0);
            assertThat(reportLines.get(3).get("status").asText()).isEqualTo("KO");
            assertThat(reportLines.get(3).get("outcome").asText()).isEqualTo("MASS_UPDATE_UNITS_RULES.KO");
            assertThat(reportLines.get(4).get("status").asText()).isEqualTo("KO");
            assertThat(reportLines.get(4).get("outcome").asText()).isEqualTo("MASS_UPDATE_UNITS_RULES.KO");
        }
    }

    private void createLogbookOperation(GUID operationId, GUID objectId, String type, LogbookTypeProcess typeProc)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException {
        final LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
        if (type == null) {
            type = STP_MASS_UPDATE_UNIT;
        }
        final LogbookOperationParameters initParameters = LogbookParameterHelper.newLogbookOperationParameters(
            operationId,
            type,
            objectId,
            typeProc,
            StatusCode.STARTED,
            operationId != null ? operationId.toString() : "outcomeDetailMessage",
            operationId
        );
        if (STP_MASS_UPDATE_UNIT.equals(type)) {
            initParameters.putParameterValue(
                LogbookParameterName.outcomeDetailMessage,
                VitamLogbookMessages.getLabelOp(STP_MASS_UPDATE_UNIT + ".STARTED") + " : " + operationId
            );
        }
        logbookClient.create(initParameters);
    }

    private void insertUnitAndLFC(final String unitFile, final String lfcFile)
        throws fr.gouv.vitam.common.exception.InvalidParseOperationException, FileNotFoundException, DatabaseException {
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_0));
        List<Document> units = JsonHandler.getFromFileAsTypeReference(
            PropertiesUtils.getResourceFile(unitFile),
            TYPE_LIST_UNIT
        );
        VitamRepositoryFactory.get().getVitamMongoRepository(MetadataCollections.UNIT.getVitamCollection()).save(units);
        VitamRepositoryFactory.get()
            .getVitamESRepository(
                MetadataCollections.UNIT.getVitamCollection(),
                metadataIndexManager.getElasticsearchIndexAliasResolver(MetadataCollections.UNIT)
            )
            .save(units);

        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_0));
        List<JsonNode> unitsLfc = JsonHandler.getFromFileAsTypeReference(
            PropertiesUtils.getResourceFile(lfcFile),
            new TypeReference<>() {}
        );
        List<Document> lfcs = unitsLfc
            .stream()
            .map(item -> Document.parse(JsonHandler.unprettyPrint(item)))
            .collect(Collectors.toList());

        new VitamMongoRepository(LogbookCollections.LIFECYCLE_UNIT.getCollection()).save(lfcs);
    }
}
