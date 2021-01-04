/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchIndexAlias;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialNotFoundException;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.Contexts;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.database.collections.Unit;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.processing.data.core.ProcessDataAccessImpl;
import fr.gouv.vitam.processing.engine.core.monitoring.ProcessMonitoringImpl;
import fr.gouv.vitam.processing.engine.core.operation.OperationContextModel;
import fr.gouv.vitam.processing.engine.core.operation.OperationContextMonitor;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.rest.DefaultOfferMain;
import fr.gouv.vitam.worker.core.plugin.bulkatomicupdate.BulkUpdateUnitReportKey;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
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
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.VitamTestHelper.waitOperation;
import static fr.gouv.vitam.common.stream.StreamUtils.consumeAnyEntityAndClose;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BulkAtomicUpdateIT extends VitamRuleRunner {

    private static final String TITLE = "Title";
    private static final String EV_ID_PROC = "evIdProc";
    private static final String BULK_ATOMIC_UPDATE_UNIT_DESC = "BULK_ATOMIC_UPDATE_UNIT_DESC";

    private static final String UNIT_00_JSON = "integration-processing/bulk-update/unit_00.json";
    private static final String UNIT_01_JSON = "integration-processing/bulk-update/unit_01.json";
    private static final String UNIT_02_JSON = "integration-processing/bulk-update/unit_02.json";
    private static final String UNIT_03_JSON = "integration-processing/bulk-update/unit_03.json";
    private static final String UNIT_04_JSON = "integration-processing/bulk-update/unit_04.json";
    private static final String UNIT_05_JSON = "integration-processing/bulk-update/unit_05.json";
    private static final String UNIT_06_JSON = "integration-processing/bulk-update/unit_06.json";
    private static final String UNIT_07_JSON = "integration-processing/bulk-update/unit_07.json";
    private static final String UNIT_08_JSON = "integration-processing/bulk-update/unit_08.json";
    private static final String UNIT_09_JSON = "integration-processing/bulk-update/unit_09.json";
    private static final String UNIT_10_JSON = "integration-processing/bulk-update/unit_10.json";

    private static final String UNIT_LFC_00_JSON = "integration-processing/bulk-update/unit_lfc_00.json";
    private static final String UNIT_LFC_01_JSON = "integration-processing/bulk-update/unit_lfc_01.json";
    private static final String UNIT_LFC_02_JSON = "integration-processing/bulk-update/unit_lfc_02.json";
    private static final String UNIT_LFC_03_JSON = "integration-processing/bulk-update/unit_lfc_03.json";
    private static final String UNIT_LFC_04_JSON = "integration-processing/bulk-update/unit_lfc_04.json";
    private static final String UNIT_LFC_05_JSON = "integration-processing/bulk-update/unit_lfc_05.json";
    private static final String UNIT_LFC_06_JSON = "integration-processing/bulk-update/unit_lfc_06.json";
    private static final String UNIT_LFC_07_JSON = "integration-processing/bulk-update/unit_lfc_07.json";
    private static final String UNIT_LFC_08_JSON = "integration-processing/bulk-update/unit_lfc_08.json";
    private static final String UNIT_LFC_09_JSON = "integration-processing/bulk-update/unit_lfc_09.json";
    private static final String UNIT_LFC_10_JSON = "integration-processing/bulk-update/unit_lfc_10.json";

    private static final String UPDATE_QUERY_01_JSON = "integration-processing/bulk-update/bulk_query_01.json";
    private static final String UPDATE_QUERY_02_JSON = "integration-processing/bulk-update/bulk_query_02.json";
    private static final String UPDATE_QUERY_03_JSON = "integration-processing/bulk-update/bulk_query_03.json";
    private static final String UPDATE_QUERY_04_JSON = "integration-processing/bulk-update/bulk_query_04.json";
    private static final String UPDATE_QUERY_05_JSON = "integration-processing/bulk-update/bulk_query_05.json";
    private static final String UPDATE_QUERY_06_JSON = "integration-processing/bulk-update/bulk_query_06.json";

    private static final String ACCESS_CONTRACT_FILE_INPUT = "accessContract.json";
    private static final String QUERIES_FILE_INPUT = "query.json";

    private static final String CONTEXT_ID = "fakeContextId";
    private static final String CONTRACT_ID = "aName";
    private static final int TENANT_0 = 0;

    private static final String RESULTS = "$results";
    private static final String EVENTS = "events";
    private static final String OUT_DETAIL = "outDetail";


    @ClassRule
    public static VitamServerRunner runner = new VitamServerRunner(ProcessingIT.class,
        mongoRule.getMongoDatabase().getName(), elasticsearchRule.getClusterName(),
        Sets.newHashSet(MetadataMain.class,
            WorkerMain.class,
            AdminManagementMain.class,
            LogbookMain.class,
            WorkspaceMain.class,
            ProcessManagementMain.class,
            StorageMain.class,
            DefaultOfferMain.class,
            BatchReportMain.class
        ));

    private static final TypeReference<List<Document>> TYPE_LIST_UNIT = new TypeReference<>() {
    };

    private WorkspaceClient workspaceClient;
    private ProcessingManagementClient processingClient;
    private static ProcessMonitoringImpl processMonitoring;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        handleBeforeClass(Arrays.asList(0, 1), Collections.emptyMap());
        // Usage ?
        PropertiesUtils.getResourcePath("integration-processing/bigworker.conf");
        FormatIdentifierFactory.getInstance().changeConfigurationFile(runner.FORMAT_IDENTIFIERS_CONF);
        processMonitoring = ProcessMonitoringImpl.getInstance();
        new DataLoader("integration-processing").prepareData();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        handleAfterClass();
        runAfter();
        VitamClientFactory.resetConnections();
    }

    @After
    public void afterTest() throws Exception {
        VitamThreadUtils.getVitamSession().setContractId(CONTRACT_ID);
        VitamThreadUtils.getVitamSession().setContextId(CONTEXT_ID);

        ProcessDataAccessImpl.getInstance().clearWorkflow();
        runAfterMongo(Sets.newHashSet(MetadataCollections.UNIT.getName(),
            MetadataCollections.OBJECTGROUP.getName(),
            FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getName(),
            FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getName(),
            LogbookCollections.OPERATION.getName(),
            LogbookCollections.LIFECYCLE_UNIT.getName(),
            LogbookCollections.LIFECYCLE_OBJECTGROUP.getName(),
            LogbookCollections.LIFECYCLE_OBJECTGROUP.getName(),
            LogbookCollections.LIFECYCLE_UNIT_IN_PROCESS.getName()
        ));

        runAfterEs(ElasticsearchIndexAlias.ofMultiTenantCollection(MetadataCollections.UNIT.getName(), 0),
            ElasticsearchIndexAlias.ofMultiTenantCollection(MetadataCollections.UNIT.getName(), 1),
            ElasticsearchIndexAlias.ofMultiTenantCollection(MetadataCollections.OBJECTGROUP.getName(), 0),
            ElasticsearchIndexAlias.ofMultiTenantCollection(MetadataCollections.OBJECTGROUP.getName(), 1),
            ElasticsearchIndexAlias.ofMultiTenantCollection(LogbookCollections.OPERATION.getName(), 0),
            ElasticsearchIndexAlias.ofMultiTenantCollection(LogbookCollections.OPERATION.getName(), 1),
            ElasticsearchIndexAlias
                .ofCrossTenantCollection(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getName()),
            ElasticsearchIndexAlias
                .ofCrossTenantCollection(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getName()));
    }

    @RunWithCustomExecutor
    @Test
    public void given_valid_queries_then_OK() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        VitamThreadUtils.getVitamSession().setContractId(CONTRACT_ID);
        VitamThreadUtils.getVitamSession().setContextId(CONTEXT_ID);

        try (AdminManagementClient functionalClient = AdminManagementClientFactory.getInstance().getClient()) {
            final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_0);
            final String containerName = operationGuid.getId();
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            createLogbookOperation(operationGuid, operationGuid, BULK_ATOMIC_UPDATE_UNIT_DESC,
                LogbookTypeProcess.BULK_UPDATE);
            workspaceClient = WorkspaceClientFactory.getInstance().getClient();
            workspaceClient.createContainer(containerName);

            // insert units and LFC
            insertUnitAndLFC(UNIT_00_JSON, UNIT_LFC_00_JSON);
            insertUnitAndLFC(UNIT_01_JSON, UNIT_LFC_01_JSON);
            insertUnitAndLFC(UNIT_02_JSON, UNIT_LFC_02_JSON);
            insertUnitAndLFC(UNIT_03_JSON, UNIT_LFC_03_JSON);
            insertUnitAndLFC(UNIT_04_JSON, UNIT_LFC_04_JSON);
            insertUnitAndLFC(UNIT_05_JSON, UNIT_LFC_05_JSON);
            insertUnitAndLFC(UNIT_06_JSON, UNIT_LFC_06_JSON);
            insertUnitAndLFC(UNIT_07_JSON, UNIT_LFC_07_JSON);
            insertUnitAndLFC(UNIT_08_JSON, UNIT_LFC_08_JSON);
            insertUnitAndLFC(UNIT_09_JSON, UNIT_LFC_09_JSON);
            insertUnitAndLFC(UNIT_10_JSON, UNIT_LFC_10_JSON);

            // Init workspace
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            initAccessContract(functionalClient, operationGuid);
            // init request
            JsonNode queries = JsonHandler
                .getFromFile(PropertiesUtils.findFile(UPDATE_QUERY_01_JSON));
            assertNotNull(queries);
            workspaceClient
                .putObject(operationGuid.getId(), QUERIES_FILE_INPUT, JsonHandler.writeToInpustream(queries));

            workspaceClient.putObject(containerName, OperationContextMonitor.OperationContextFileName, 
                    JsonHandler.writeToInpustream(OperationContextModel.get(queries)));
            OperationContextMonitor
                .compressInWorkspace(WorkspaceClientFactory.getInstance(), containerName,
                    Contexts.BULK_ATOMIC_UPDATE_UNIT_DESC.getLogbookTypeProcess(),
                    OperationContextMonitor.OperationContextFileName);
            
            // Init workflow
            processingClient = ProcessingManagementClientFactory.getInstance().getClient();
            processingClient.initVitamProcess(containerName, Contexts.BULK_ATOMIC_UPDATE_UNIT_DESC.name());

            RequestResponse<ItemStatus> ret = processingClient
                .updateOperationActionProcess(ProcessAction.RESUME.getValue(), containerName);
            assertNotNull(ret);
            assertEquals(Response.Status.ACCEPTED.getStatusCode(), ret.getStatus());

            waitOperation(containerName);
            ProcessWorkflow processWorkflow = processMonitoring.findOneProcessWorkflow(containerName, TENANT_0);
            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.OK, processWorkflow.getStatus());

            Optional<Document> updatedUnit = VitamRepositoryFactory.get()
                .getVitamMongoRepository(MetadataCollections.UNIT.getVitamCollection())
                .getByID("aeaqaaaaaahpqzjiaaoxmalwdlxeemqaaacq", TENANT_0);
            assertTrue(updatedUnit.isPresent());
            String expected = "update old title sous fonds \"émouvant สวัสดี";
            assertThat(updatedUnit.get().get(TITLE)).isEqualTo(expected);
            assertThat((List<String>) updatedUnit.get().get(Unit.OPS)).contains(operationGuid.getId());

            JsonNode logbookResult = getLogbookOperation(containerName);
            JsonNode events = logbookResult.get(RESULTS).get(0).get(EVENTS);
            verifyEvent(events, "CHECK_QUERIES_THRESHOLD.OK");
            verifyEvent(events, "PREPARE_BULK_ATOMIC_UPDATE_UNIT_LIST.OK");
            verifyEvent(events, "BULK_ATOMIC_UPDATE_UNITS.OK");
            verifyEvent(events, "BULK_ATOMIC_UPDATE_FINALIZE.OK");
            verifyEvent(events, "BULK_ATOMIC_UPDATE_UNIT_DESC.OK");

            LogbookLifeCyclesClient logbookLifeCyclesClient = LogbookLifeCyclesClientFactory.getInstance().getClient();
            JsonNode unitLfc = logbookLifeCyclesClient.selectUnitLifeCycleById(
                updatedUnit.get().get("_id", String.class), new Select().getFinalSelectById());

            JsonNode lfcEvents = unitLfc.get("$results").get(0).get("events");
            final JsonNode lastEvent = lfcEvents.get(lfcEvents.size() - 1);
            JsonNode evDetData = lastEvent.get("evDetData");
            JsonNode jsoned = JsonHandler.getFromString(evDetData.textValue());
            expected = "update old title sous fonds \\\"\\u00E9mouvant \\u0E2A\\u0E27\\u0E31\\u0E2A\\u0E14\\u0E35";
            assertThat(jsoned.get("diff").textValue()).contains(expected);

            // CHECK REPORT
            List<JsonNode> reportLines;
            try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
                Response reportResponse = null;
                try {
                    reportResponse = storageClient.getContainerAsync(VitamConfiguration.getDefaultStrategy(),
                        operationGuid.getId() + ".jsonl", DataCategory.REPORT, AccessLogUtils.getNoLogAccessLog());
                    assertThat(reportResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

                    reportLines = VitamTestHelper.getReport(reportResponse);
                } finally {
                    consumeAnyEntityAndClose(reportResponse);
                }
            }
            assertThat(reportLines.size()).isEqualTo(14);
            assertThat(reportLines.get(0).get("outDetail").asText()).isEqualTo("BULK_ATOMIC_UPDATE_UNITS.OK");
            assertThat(reportLines.get(1).get("vitamResults").get("OK").asInt()).isEqualTo(11);
            assertThat(reportLines.get(1).get("vitamResults").get("KO").asInt()).isEqualTo(0);
            assertThat(reportLines.get(1).get("vitamResults").get("WARNING").asInt()).isEqualTo(0);

            Map<Integer, JsonNode> reportDetailsByQueryIndex = getReportDetailsByQueryIndex(reportLines);
            assertThat(reportDetailsByQueryIndex.get(0).get("status").asText()).isEqualTo("OK");
            assertThat(reportDetailsByQueryIndex.get(0).get("unitId").asText())
                .isEqualTo("aeaqaaaaaahpqzjiaaoxmalwdlxeemqaaacq");
            assertThat(reportDetailsByQueryIndex.get(0).get("query").asText())
                .isEqualTo(JsonHandler.unprettyPrint(queries.get("queries").get(0)));

            assertThat(reportDetailsByQueryIndex.get(1).get("status").asText()).isEqualTo("OK");
            assertThat(reportDetailsByQueryIndex.get(1).get("unitId").asText())
                .isEqualTo("aeaqaaaaaagbcaacaang6ak4ts6palibbbbq");
            assertThat(reportDetailsByQueryIndex.get(1).get("query").asText())
                .isEqualTo(JsonHandler.unprettyPrint(queries.get("queries").get(1)));

            assertThat(reportDetailsByQueryIndex.get(10).get("status").asText()).isEqualTo("OK");
            assertThat(reportDetailsByQueryIndex.get(10).get("unitId").asText())
                .isEqualTo("aeaqaaaaaahpqzjiaaoxmalwdlwy2iiaaaaq");
            assertThat(reportDetailsByQueryIndex.get(10).get("query").asText())
                .isEqualTo(JsonHandler.unprettyPrint(queries.get("queries").get(10)));
        }
    }

    @RunWithCustomExecutor
    @Test
    public void given_invalid_queries_and_invalid_results_then_WARNING() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        VitamThreadUtils.getVitamSession().setContractId(CONTRACT_ID);
        VitamThreadUtils.getVitamSession().setContextId(CONTEXT_ID);

        try (AdminManagementClient functionalClient = AdminManagementClientFactory.getInstance().getClient()) {
            final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_0);
            final String containerName = operationGuid.getId();
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            createLogbookOperation(operationGuid, operationGuid, BULK_ATOMIC_UPDATE_UNIT_DESC,
                LogbookTypeProcess.BULK_UPDATE);
            workspaceClient = WorkspaceClientFactory.getInstance().getClient();
            workspaceClient.createContainer(containerName);

            // insert units and LFC
            insertUnitAndLFC(UNIT_00_JSON, UNIT_LFC_00_JSON);
            insertUnitAndLFC(UNIT_01_JSON, UNIT_LFC_01_JSON);
            insertUnitAndLFC(UNIT_02_JSON, UNIT_LFC_02_JSON);
            insertUnitAndLFC(UNIT_03_JSON, UNIT_LFC_03_JSON);
            insertUnitAndLFC(UNIT_04_JSON, UNIT_LFC_04_JSON);
            insertUnitAndLFC(UNIT_05_JSON, UNIT_LFC_05_JSON);
            insertUnitAndLFC(UNIT_06_JSON, UNIT_LFC_06_JSON);
            insertUnitAndLFC(UNIT_07_JSON, UNIT_LFC_07_JSON);
            insertUnitAndLFC(UNIT_08_JSON, UNIT_LFC_08_JSON);
            insertUnitAndLFC(UNIT_09_JSON, UNIT_LFC_09_JSON);
            insertUnitAndLFC(UNIT_10_JSON, UNIT_LFC_10_JSON);

            // Init workspace
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            initAccessContract(functionalClient, operationGuid);
            // init request
            JsonNode queries = JsonHandler
                .getFromFile(PropertiesUtils.findFile(UPDATE_QUERY_02_JSON));
            assertNotNull(queries);
            workspaceClient
                .putObject(operationGuid.getId(), QUERIES_FILE_INPUT, JsonHandler.writeToInpustream(queries));

            workspaceClient.putObject(containerName, OperationContextMonitor.OperationContextFileName, 
                    JsonHandler.writeToInpustream(OperationContextModel.get(queries)));
            OperationContextMonitor
                .compressInWorkspace(WorkspaceClientFactory.getInstance(), containerName,
                    Contexts.BULK_ATOMIC_UPDATE_UNIT_DESC.getLogbookTypeProcess(),
                    OperationContextMonitor.OperationContextFileName);

            // Init workflow
            processingClient = ProcessingManagementClientFactory.getInstance().getClient();
            processingClient.initVitamProcess(containerName, Contexts.BULK_ATOMIC_UPDATE_UNIT_DESC.name());

            RequestResponse<ItemStatus> ret = processingClient
                .updateOperationActionProcess(ProcessAction.RESUME.getValue(), containerName);
            assertNotNull(ret);
            assertEquals(Response.Status.ACCEPTED.getStatusCode(), ret.getStatus());

            waitOperation(containerName);
            ProcessWorkflow processWorkflow = processMonitoring.findOneProcessWorkflow(containerName, TENANT_0);
            assertNotNull(processWorkflow);

            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.WARNING, processWorkflow.getStatus());

            Optional<Document> updatedUnit = VitamRepositoryFactory.get()
                .getVitamMongoRepository(MetadataCollections.UNIT.getVitamCollection())
                .getByID("aeaqaaaaaahpqzjiaaoxmalwdlxeemqaaacq", TENANT_0);
            assertTrue(updatedUnit.isPresent());
            String expected = "update old title sous fonds Identifier00";
            assertThat(updatedUnit.get().get(TITLE)).isEqualTo(expected);
            assertThat((List<String>) updatedUnit.get().get(Unit.OPS)).contains(operationGuid.getId());

            JsonNode logbookResult = getLogbookOperation(containerName);
            JsonNode events = logbookResult.get(RESULTS).get(0).get(EVENTS);
            verifyEvent(events, "CHECK_QUERIES_THRESHOLD.OK");
            verifyEvent(events, "PREPARE_BULK_ATOMIC_UPDATE_UNIT_LIST.WARNING");
            verifyEvent(events, "BULK_ATOMIC_UPDATE_UNITS.OK");
            verifyEvent(events, "BULK_ATOMIC_UPDATE_FINALIZE.WARNING");
            verifyEvent(events, "BULK_ATOMIC_UPDATE_UNIT_DESC.WARNING");

            LogbookLifeCyclesClient logbookLifeCyclesClient = LogbookLifeCyclesClientFactory.getInstance().getClient();
            JsonNode unitLfc = logbookLifeCyclesClient.selectUnitLifeCycleById(
                updatedUnit.get().get("_id", String.class), new Select().getFinalSelectById());

            JsonNode lfcEvents = unitLfc.get("$results").get(0).get("events");
            final JsonNode lastEvent = lfcEvents.get(lfcEvents.size() - 1);
            JsonNode evDetData = lastEvent.get("evDetData");
            JsonNode jsoned = JsonHandler.getFromString(evDetData.textValue());
            expected = "update old title sous fonds Identifier00";
            assertThat(jsoned.get("diff").textValue()).contains(expected);

            // CHECK REPORT
            List<JsonNode> reportLines;
            try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
                Response reportResponse = null;
                try {
                    reportResponse = storageClient.getContainerAsync(VitamConfiguration.getDefaultStrategy(),
                        operationGuid.getId() + ".jsonl", DataCategory.REPORT, AccessLogUtils.getNoLogAccessLog());
                    assertThat(reportResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

                    reportLines = VitamTestHelper.getReport(reportResponse);
                } finally {
                    consumeAnyEntityAndClose(reportResponse);
                }
            }
            assertThat(reportLines.size()).isEqualTo(14);
            assertThat(reportLines.get(0).get("outDetail").asText()).isEqualTo("BULK_ATOMIC_UPDATE_UNITS.OK");
            assertThat(reportLines.get(1).get("vitamResults").get("OK").asInt()).isEqualTo(8);
            assertThat(reportLines.get(1).get("vitamResults").get("KO").asInt()).isEqualTo(0);
            assertThat(reportLines.get(1).get("vitamResults").get("WARNING").asInt()).isEqualTo(3);

            Map<Integer, JsonNode> reportDetailsByQueryIndex = getReportDetailsByQueryIndex(reportLines);
            // first prepare error : warning
            assertThat(reportDetailsByQueryIndex.get(1).get("status").asText()).isEqualTo("WARNING");
            assertThat(reportDetailsByQueryIndex.get(1).has("unitId")).isFalse();
            assertThat(reportDetailsByQueryIndex.get(1).get("resultKey").asText())
                .isEqualTo(BulkUpdateUnitReportKey.UNIT_NOT_FOUND.name());
            assertThat(reportDetailsByQueryIndex.get(1).get("message").asText())
                .isEqualTo(BulkUpdateUnitReportKey.UNIT_NOT_FOUND.getMessage());
            assertThat(reportDetailsByQueryIndex.get(1).get("query").asText())
                .isEqualTo(JsonHandler.unprettyPrint(queries.get("queries").get(1)));
            // second prepare error : KO
            assertThat(reportDetailsByQueryIndex.get(8).get("status").asText()).isEqualTo("WARNING");
            assertThat(reportDetailsByQueryIndex.get(8).get("resultKey").asText())
                .isEqualTo(BulkUpdateUnitReportKey.INVALID_DSL_QUERY.name());
            assertThat(reportDetailsByQueryIndex.get(8).get("message").asText())
                .startsWith(BulkUpdateUnitReportKey.INVALID_DSL_QUERY.getMessage());
            assertThat(reportDetailsByQueryIndex.get(8).get("query").asText())
                .isEqualTo(JsonHandler.unprettyPrint(queries.get("queries").get(8)));
            // third prepare error : KO
            assertThat(reportDetailsByQueryIndex.get(9).get("status").asText()).isEqualTo("WARNING");
            assertThat(reportDetailsByQueryIndex.get(9).get("resultKey").asText())
                .isEqualTo(BulkUpdateUnitReportKey.TOO_MANY_UNITS_FOUND.name());
            assertThat(reportDetailsByQueryIndex.get(9).get("message").asText())
                .isEqualTo(BulkUpdateUnitReportKey.TOO_MANY_UNITS_FOUND.getMessage());
            assertThat(reportDetailsByQueryIndex.get(9).get("query").asText())
                .isEqualTo(JsonHandler.unprettyPrint(queries.get("queries").get(9)));
            // first update ok
            assertThat(reportDetailsByQueryIndex.get(0).get("status").asText()).isEqualTo("OK");
            assertThat(reportDetailsByQueryIndex.get(0).get("unitId").asText())
                .isEqualTo("aeaqaaaaaahpqzjiaaoxmalwdlxeemqaaacq");
            assertThat(reportDetailsByQueryIndex.get(0).get("query").asText())
                .isEqualTo(JsonHandler.unprettyPrint(queries.get("queries").get(0)));
            // last update ok
            assertThat(reportDetailsByQueryIndex.get(10).get("status").asText()).isEqualTo("OK");
            assertThat(reportDetailsByQueryIndex.get(10).get("unitId").asText())
                .isEqualTo("aeaqaaaaaahpqzjiaaoxmalwdlwy2iiaaaaq");
            assertThat(reportDetailsByQueryIndex.get(10).get("query").asText())
                .isEqualTo(JsonHandler.unprettyPrint(queries.get("queries").get(10)));
        }
    }

    @RunWithCustomExecutor
    @Test
    public void given_invalid_updates_then_WARNING() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        VitamThreadUtils.getVitamSession().setContractId(CONTRACT_ID);
        VitamThreadUtils.getVitamSession().setContextId(CONTEXT_ID);

        try (AdminManagementClient functionalClient = AdminManagementClientFactory.getInstance().getClient()) {
            final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_0);
            final String containerName = operationGuid.getId();
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            createLogbookOperation(operationGuid, operationGuid, BULK_ATOMIC_UPDATE_UNIT_DESC,
                LogbookTypeProcess.BULK_UPDATE);
            workspaceClient = WorkspaceClientFactory.getInstance().getClient();
            workspaceClient.createContainer(containerName);

            // insert units and LFC
            insertUnitAndLFC(UNIT_00_JSON, UNIT_LFC_00_JSON);
            insertUnitAndLFC(UNIT_01_JSON, UNIT_LFC_01_JSON);
            insertUnitAndLFC(UNIT_02_JSON, UNIT_LFC_02_JSON);
            insertUnitAndLFC(UNIT_03_JSON, UNIT_LFC_03_JSON);

            // Init workspace
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            initAccessContract(functionalClient, operationGuid);
            // init request
            JsonNode queries = JsonHandler
                .getFromFile(PropertiesUtils.findFile(UPDATE_QUERY_03_JSON));
            assertNotNull(queries);
            workspaceClient
                .putObject(operationGuid.getId(), QUERIES_FILE_INPUT, JsonHandler.writeToInpustream(queries));

            workspaceClient.putObject(containerName, OperationContextMonitor.OperationContextFileName, 
                    JsonHandler.writeToInpustream(OperationContextModel.get(queries)));
            OperationContextMonitor
                .compressInWorkspace(WorkspaceClientFactory.getInstance(), containerName,
                    Contexts.BULK_ATOMIC_UPDATE_UNIT_DESC.getLogbookTypeProcess(),
                    OperationContextMonitor.OperationContextFileName);

            // Init workflow
            processingClient = ProcessingManagementClientFactory.getInstance().getClient();
            processingClient.initVitamProcess(containerName, Contexts.BULK_ATOMIC_UPDATE_UNIT_DESC.name());

            RequestResponse<ItemStatus> ret = processingClient
                .updateOperationActionProcess(ProcessAction.RESUME.getValue(), containerName);
            assertNotNull(ret);
            assertEquals(Response.Status.ACCEPTED.getStatusCode(), ret.getStatus());

            waitOperation(containerName);
            ProcessWorkflow processWorkflow = processMonitoring.findOneProcessWorkflow(containerName, TENANT_0);
            assertNotNull(processWorkflow);

            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.WARNING, processWorkflow.getStatus());

            JsonNode logbookResult = getLogbookOperation(containerName);
            JsonNode events = logbookResult.get(RESULTS).get(0).get(EVENTS);
            verifyEvent(events, "CHECK_QUERIES_THRESHOLD.OK");
            verifyEvent(events, "PREPARE_BULK_ATOMIC_UPDATE_UNIT_LIST.OK");
            verifyEvent(events, "BULK_ATOMIC_UPDATE_UNITS.KO");
            verifyEvent(events, "BULK_ATOMIC_UPDATE_FINALIZE.WARNING");
            verifyEvent(events, "BULK_ATOMIC_UPDATE_UNIT_DESC.WARNING");

            Optional<Document> updatedUnit = VitamRepositoryFactory.get()
                .getVitamMongoRepository(MetadataCollections.UNIT.getVitamCollection())
                .getByID("aeaqaaaaaagbcaacaang6ak4ts6palibbbbq", TENANT_0);
            assertTrue(updatedUnit.isPresent());
            assertThat(updatedUnit.get().getList("ArchivalAgencyArchiveUnitIdentifier", String.class).get(0))
                .isEqualTo("2");
            assertThat((List<String>) updatedUnit.get().get(Unit.OPS)).contains(operationGuid.getId());
            LogbookLifeCyclesClient logbookLifeCyclesClient = LogbookLifeCyclesClientFactory.getInstance().getClient();

            JsonNode unitLfc = logbookLifeCyclesClient.selectUnitLifeCycleById(
                updatedUnit.get().get("_id", String.class), new Select().getFinalSelectById());
            JsonNode lfcEvents = unitLfc.get("$results").get(0).get("events");
            final JsonNode lastEvent = lfcEvents.get(lfcEvents.size() - 1);
            JsonNode evDetData = lastEvent.get("evDetData");
            JsonNode jsoned = JsonHandler.getFromString(evDetData.textValue());
            String expected = "\"ArchivalAgencyArchiveUnitIdentifier\" : [ \"2\" ]";
            assertThat(jsoned.get("diff").textValue()).contains(expected);

            // CHECK REPORT
            List<JsonNode> reportLines;
            try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
                Response reportResponse = null;
                try {
                    reportResponse = storageClient.getContainerAsync(VitamConfiguration.getDefaultStrategy(),
                        operationGuid.getId() + ".jsonl", DataCategory.REPORT, AccessLogUtils.getNoLogAccessLog());
                    assertThat(reportResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

                    reportLines = VitamTestHelper.getReport(reportResponse);
                } finally {
                    consumeAnyEntityAndClose(reportResponse);
                }
            }

            assertThat(reportLines.size()).isEqualTo(7);
            assertThat(reportLines.get(0).get("outDetail").asText()).isEqualTo("BULK_ATOMIC_UPDATE_UNITS.WARNING");
            assertThat(reportLines.get(1).get("vitamResults").get("OK").asInt()).isEqualTo(3);
            assertThat(reportLines.get(1).get("vitamResults").get("KO").asInt()).isEqualTo(1);
            assertThat(reportLines.get(1).get("vitamResults").get("WARNING").asInt()).isEqualTo(0);

            Map<Integer, JsonNode> reportDetailsByQueryIndex = getReportDetailsByQueryIndex(reportLines);
            assertThat(reportDetailsByQueryIndex.get(0).get("status").asText()).isEqualTo("OK");
            assertThat(reportDetailsByQueryIndex.get(0).get("query").asText())
                .isEqualTo(JsonHandler.unprettyPrint(queries.get("queries").get(0)));
            assertThat(reportDetailsByQueryIndex.get(1).get("status").asText()).isEqualTo("OK");
            assertThat(reportDetailsByQueryIndex.get(1).get("query").asText())
                .isEqualTo(JsonHandler.unprettyPrint(queries.get("queries").get(1)));

            assertThat(reportDetailsByQueryIndex.get(2).get("status").asText()).isEqualTo("KO");
            assertThat(reportDetailsByQueryIndex.get(2).get("resultKey").asText()).isEqualTo("CHECK_UNIT_SCHEMA");
            assertThat(reportDetailsByQueryIndex.get(2).get("message").asText())
                .contains("Archive unit contains fields declared in ontology with a wrong format");
            assertThat(reportDetailsByQueryIndex.get(2).get("query").asText())
                .isEqualTo(JsonHandler.unprettyPrint(queries.get("queries").get(2)));

            assertThat(reportDetailsByQueryIndex.get(3).get("status").asText()).isEqualTo("OK");
            assertThat(reportDetailsByQueryIndex.get(3).get("query").asText())
                .isEqualTo(JsonHandler.unprettyPrint(queries.get("queries").get(3)));
        }
    }

    @RunWithCustomExecutor
    @Test
    public void given_all_invalid_queries_then_empty_WARNING() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        VitamThreadUtils.getVitamSession().setContractId(CONTRACT_ID);
        VitamThreadUtils.getVitamSession().setContextId(CONTEXT_ID);

        try (AdminManagementClient functionalClient = AdminManagementClientFactory.getInstance().getClient()) {
            final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_0);
            final String containerName = operationGuid.getId();
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            createLogbookOperation(operationGuid, operationGuid, BULK_ATOMIC_UPDATE_UNIT_DESC,
                LogbookTypeProcess.BULK_UPDATE);
            workspaceClient = WorkspaceClientFactory.getInstance().getClient();
            workspaceClient.createContainer(containerName);

            // insert units and LFC
            insertUnitAndLFC(UNIT_00_JSON, UNIT_LFC_00_JSON);
            insertUnitAndLFC(UNIT_01_JSON, UNIT_LFC_01_JSON);
            insertUnitAndLFC(UNIT_02_JSON, UNIT_LFC_02_JSON);
            insertUnitAndLFC(UNIT_03_JSON, UNIT_LFC_03_JSON);

            // Init workspace
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            initAccessContract(functionalClient, operationGuid);
            // init request
            JsonNode queries = JsonHandler
                .getFromFile(PropertiesUtils.findFile(UPDATE_QUERY_04_JSON));
            assertNotNull(queries);
            workspaceClient
                .putObject(operationGuid.getId(), QUERIES_FILE_INPUT, JsonHandler.writeToInpustream(queries));

            workspaceClient.putObject(containerName, OperationContextMonitor.OperationContextFileName, 
                    JsonHandler.writeToInpustream(OperationContextModel.get(queries)));
            OperationContextMonitor
                .compressInWorkspace(WorkspaceClientFactory.getInstance(), containerName,
                    Contexts.BULK_ATOMIC_UPDATE_UNIT_DESC.getLogbookTypeProcess(),
                    OperationContextMonitor.OperationContextFileName);

            // Init workflow
            processingClient = ProcessingManagementClientFactory.getInstance().getClient();
            processingClient.initVitamProcess(containerName, Contexts.BULK_ATOMIC_UPDATE_UNIT_DESC.name());

            RequestResponse<ItemStatus> ret = processingClient
                .updateOperationActionProcess(ProcessAction.RESUME.getValue(), containerName);
            assertNotNull(ret);
            assertEquals(Response.Status.ACCEPTED.getStatusCode(), ret.getStatus());

            waitOperation(containerName);
            ProcessWorkflow processWorkflow = processMonitoring.findOneProcessWorkflow(containerName, TENANT_0);
            assertNotNull(processWorkflow);

            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.WARNING, processWorkflow.getStatus());

            JsonNode logbookResult = getLogbookOperation(containerName);
            JsonNode events = logbookResult.get(RESULTS).get(0).get(EVENTS);
            verifyEvent(events, "CHECK_QUERIES_THRESHOLD.OK");
            verifyEvent(events, "PREPARE_BULK_ATOMIC_UPDATE_UNIT_LIST.WARNING");
            verifyEvent(events, "OBJECTS_LIST_EMPTY.WARNING");
            verifyEvent(events, "BULK_ATOMIC_UPDATE_FINALIZE.WARNING");
            verifyEvent(events, "BULK_ATOMIC_UPDATE_UNIT_DESC.WARNING");

            // CHECK REPORT
            List<JsonNode> reportLines;
            try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
                Response reportResponse = null;
                try {
                    reportResponse = storageClient.getContainerAsync(VitamConfiguration.getDefaultStrategy(),
                        operationGuid.getId() + ".jsonl", DataCategory.REPORT, AccessLogUtils.getNoLogAccessLog());
                    assertThat(reportResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

                    reportLines = VitamTestHelper.getReport(reportResponse);
                } finally {
                    consumeAnyEntityAndClose(reportResponse);
                }
            }

            assertThat(reportLines.size()).isEqualTo(5);
            assertThat(reportLines.get(0).get("outDetail").asText()).isEqualTo("OBJECTS_LIST_EMPTY.WARNING");
            assertThat(reportLines.get(1).get("vitamResults").get("OK").asInt()).isEqualTo(0);
            assertThat(reportLines.get(1).get("vitamResults").get("KO").asInt()).isEqualTo(0);
            assertThat(reportLines.get(1).get("vitamResults").get("WARNING").asInt()).isEqualTo(2);

            Map<Integer, JsonNode> reportDetailsByQueryIndex = getReportDetailsByQueryIndex(reportLines);
            assertThat(reportDetailsByQueryIndex.get(0).get("status").asText()).isEqualTo("WARNING");
            assertThat(reportDetailsByQueryIndex.get(0).has("unitId")).isFalse();
            assertThat(reportDetailsByQueryIndex.get(0).get("resultKey").asText())
                .isEqualTo(BulkUpdateUnitReportKey.UNIT_NOT_FOUND.name());
            assertThat(reportDetailsByQueryIndex.get(0).get("message").asText())
                .isEqualTo(BulkUpdateUnitReportKey.UNIT_NOT_FOUND.getMessage());
            assertThat(reportDetailsByQueryIndex.get(0).get("query").asText())
                .isEqualTo(JsonHandler.unprettyPrint(queries.get("queries").get(0)));

            assertThat(reportDetailsByQueryIndex.get(1).get("status").asText()).isEqualTo("WARNING");
            assertThat(reportDetailsByQueryIndex.get(1).has("unitId")).isFalse();
            assertThat(reportDetailsByQueryIndex.get(1).get("resultKey").asText())
                .isEqualTo(BulkUpdateUnitReportKey.INVALID_DSL_QUERY.name());
            assertThat(reportDetailsByQueryIndex.get(1).get("message").asText())
                .startsWith(BulkUpdateUnitReportKey.INVALID_DSL_QUERY.getMessage());
            assertThat(reportDetailsByQueryIndex.get(1).get("query").asText())
                .isEqualTo(JsonHandler.unprettyPrint(queries.get("queries").get(1)));
        }

    }

    @RunWithCustomExecutor
    @Test
    public void given_dsl_invalid_then_KO() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        VitamThreadUtils.getVitamSession().setContractId(CONTRACT_ID);
        VitamThreadUtils.getVitamSession().setContextId(CONTEXT_ID);

        try (AdminManagementClient functionalClient = AdminManagementClientFactory.getInstance().getClient()) {
            final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_0);
            final String containerName = operationGuid.getId();
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            createLogbookOperation(operationGuid, operationGuid, BULK_ATOMIC_UPDATE_UNIT_DESC,
                LogbookTypeProcess.BULK_UPDATE);
            workspaceClient = WorkspaceClientFactory.getInstance().getClient();
            workspaceClient.createContainer(containerName);

            // insert units and LFC
            insertUnitAndLFC(UNIT_00_JSON, UNIT_LFC_00_JSON);
            insertUnitAndLFC(UNIT_01_JSON, UNIT_LFC_01_JSON);
            insertUnitAndLFC(UNIT_02_JSON, UNIT_LFC_02_JSON);
            insertUnitAndLFC(UNIT_03_JSON, UNIT_LFC_03_JSON);

            // Init workspace
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            // init access contrat
            initAccessContract(functionalClient, operationGuid);
            // init request
            JsonNode queries = JsonHandler
                .getFromFile(PropertiesUtils.findFile(UPDATE_QUERY_05_JSON));
            assertNotNull(queries);
            workspaceClient
                .putObject(operationGuid.getId(), QUERIES_FILE_INPUT, JsonHandler.writeToInpustream(queries));

            workspaceClient.putObject(containerName, OperationContextMonitor.OperationContextFileName, 
                    JsonHandler.writeToInpustream(OperationContextModel.get(queries)));
            OperationContextMonitor
                .compressInWorkspace(WorkspaceClientFactory.getInstance(), containerName,
                    Contexts.BULK_ATOMIC_UPDATE_UNIT_DESC.getLogbookTypeProcess(),
                    OperationContextMonitor.OperationContextFileName);

            // Init workflow
            processingClient = ProcessingManagementClientFactory.getInstance().getClient();
            processingClient.initVitamProcess(containerName, Contexts.BULK_ATOMIC_UPDATE_UNIT_DESC.name());

            RequestResponse<ItemStatus> ret = processingClient
                .updateOperationActionProcess(ProcessAction.RESUME.getValue(), containerName);
            assertNotNull(ret);
            assertEquals(Response.Status.ACCEPTED.getStatusCode(), ret.getStatus());

            waitOperation(containerName);
            ProcessWorkflow processWorkflow = processMonitoring.findOneProcessWorkflow(containerName, TENANT_0);
            assertNotNull(processWorkflow);

            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.KO, processWorkflow.getStatus());

            JsonNode logbookResult = getLogbookOperation(containerName);
            JsonNode events = logbookResult.get(RESULTS).get(0).get(EVENTS);
            verifyEvent(events, "CHECK_QUERIES_THRESHOLD.OK");
            verifyEvent(events, "PREPARE_BULK_ATOMIC_UPDATE_UNIT_LIST.KO");
            verifyEvent(events, "BULK_ATOMIC_UPDATE_FINALIZE.WARNING");
            verifyEvent(events, "BULK_ATOMIC_UPDATE_UNIT_DESC.KO");

            // CHECK REPORT
            List<JsonNode> reportLines;
            try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
                Response reportResponse = null;
                try {
                    reportResponse = storageClient.getContainerAsync(VitamConfiguration.getDefaultStrategy(),
                        operationGuid.getId() + ".jsonl", DataCategory.REPORT, AccessLogUtils.getNoLogAccessLog());
                    assertThat(reportResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

                    reportLines = VitamTestHelper.getReport(reportResponse);
                } finally {
                    consumeAnyEntityAndClose(reportResponse);
                }
            }

            assertThat(reportLines.size()).isEqualTo(3);
            assertThat(reportLines.get(0).get("outDetail").asText())
                .isEqualTo("PREPARE_BULK_ATOMIC_UPDATE_UNIT_LIST.KO");
            assertThat(reportLines.get(1).get("vitamResults").get("OK").asInt()).isEqualTo(0);
            assertThat(reportLines.get(1).get("vitamResults").get("KO").asInt()).isEqualTo(1);
            assertThat(reportLines.get(1).get("vitamResults").get("WARNING").asInt()).isEqualTo(0);
        }
    }

    @RunWithCustomExecutor
    @Test
    public void given_redo_of_fatal_bulk_update_then_OK() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        VitamThreadUtils.getVitamSession().setContractId(CONTRACT_ID);
        VitamThreadUtils.getVitamSession().setContextId(CONTEXT_ID);

        try (AdminManagementClient functionalClient = AdminManagementClientFactory.getInstance().getClient()) {

            final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_0);
            final String containerName = operationGuid.getId();
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            createLogbookOperation(operationGuid, operationGuid, BULK_ATOMIC_UPDATE_UNIT_DESC,
                LogbookTypeProcess.BULK_UPDATE);

            workspaceClient = WorkspaceClientFactory.getInstance().getClient();
            workspaceClient.createContainer(containerName);

            // insert units and LFC
            insertUnitAndLFC(UNIT_00_JSON, UNIT_LFC_00_JSON);

            // Init workspace
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            // init access contrat
            initAccessContract(functionalClient, operationGuid);
            // no request in workspace


            processingClient = ProcessingManagementClientFactory.getInstance().getClient();
            processingClient.initVitamProcess(containerName, Contexts.BULK_ATOMIC_UPDATE_UNIT_DESC.name());

            // first launch fatal
            RequestResponse<ItemStatus> ret =
                processingClient.updateOperationActionProcess(ProcessAction.RESUME.getValue(), containerName);
            assertNotNull(ret);
            assertEquals(Response.Status.ACCEPTED.getStatusCode(), ret.getStatus());

            waitOperation(containerName);
            ProcessWorkflow processWorkflow = processMonitoring.findOneProcessWorkflow(containerName, TENANT_0);
            assertNotNull(processWorkflow);
            assertEquals(ProcessState.PAUSE, processWorkflow.getState());
            assertEquals(StatusCode.FATAL, processWorkflow.getStatus());

            // init request
            JsonNode queries = JsonHandler
                .getFromFile(PropertiesUtils.findFile(UPDATE_QUERY_06_JSON));
            assertNotNull(queries);
            workspaceClient
                .putObject(operationGuid.getId(), QUERIES_FILE_INPUT, JsonHandler.writeToInpustream(queries));

            workspaceClient.putObject(containerName, OperationContextMonitor.OperationContextFileName, 
                    JsonHandler.writeToInpustream(OperationContextModel.get(queries)));
            OperationContextMonitor
                .compressInWorkspace(WorkspaceClientFactory.getInstance(), containerName,
                    Contexts.BULK_ATOMIC_UPDATE_UNIT_DESC.getLogbookTypeProcess(),
                    OperationContextMonitor.OperationContextFileName);
            
            // relaunch
            ret = processingClient.updateOperationActionProcess(ProcessAction.RESUME.getValue(), containerName);
            assertNotNull(ret);
            assertEquals(Response.Status.ACCEPTED.getStatusCode(), ret.getStatus());

            waitOperation(containerName);
            processWorkflow = processMonitoring.findOneProcessWorkflow(containerName, TENANT_0);
            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.OK, processWorkflow.getStatus());
        }
    }


    @RunWithCustomExecutor
    @Test
    public void given_invalid_threshold_then_KO() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        VitamThreadUtils.getVitamSession().setContractId(CONTRACT_ID);
        VitamThreadUtils.getVitamSession().setContextId(CONTEXT_ID);

        try (AdminManagementClient functionalClient = AdminManagementClientFactory.getInstance().getClient()) {

            final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_0);
            final String containerName = operationGuid.getId();
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            createLogbookOperation(operationGuid, operationGuid, BULK_ATOMIC_UPDATE_UNIT_DESC,
                LogbookTypeProcess.BULK_UPDATE);

            workspaceClient = WorkspaceClientFactory.getInstance().getClient();
            workspaceClient.createContainer(containerName);

            // insert units and LFC
            insertUnitAndLFC(UNIT_00_JSON, UNIT_LFC_00_JSON);

            // Init workspace
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            // init access contrat
            initAccessContract(functionalClient, operationGuid);

            // init request
            ObjectNode queries = (ObjectNode) JsonHandler
                .getFromFile(PropertiesUtils.findFile(UPDATE_QUERY_01_JSON));
            queries.put("threshold", 1);
            assertNotNull(queries);
            workspaceClient
                .putObject(operationGuid.getId(), QUERIES_FILE_INPUT, JsonHandler.writeToInpustream(queries));
            workspaceClient.putObject(containerName, OperationContextMonitor.OperationContextFileName, 
                    JsonHandler.writeToInpustream(OperationContextModel.get(queries)));
            OperationContextMonitor
                .compressInWorkspace(WorkspaceClientFactory.getInstance(), containerName,
                    Contexts.BULK_ATOMIC_UPDATE_UNIT_DESC.getLogbookTypeProcess(),
                    OperationContextMonitor.OperationContextFileName);

            processingClient = ProcessingManagementClientFactory.getInstance().getClient();
            processingClient.initVitamProcess(containerName, Contexts.BULK_ATOMIC_UPDATE_UNIT_DESC.name());

            // first launch fatal
            RequestResponse<ItemStatus> ret =
                processingClient.updateOperationActionProcess(ProcessAction.RESUME.getValue(), containerName);
            assertNotNull(ret);
            assertEquals(Response.Status.ACCEPTED.getStatusCode(), ret.getStatus());

            waitOperation(containerName);
            ProcessWorkflow processWorkflow = processMonitoring.findOneProcessWorkflow(containerName, TENANT_0);
            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.KO, processWorkflow.getStatus());

            JsonNode logbookResult = getLogbookOperation(containerName);
            JsonNode events = logbookResult.get(RESULTS).get(0).get(EVENTS);
            verifyEvent(events, "CHECK_QUERIES_THRESHOLD.KO");
            verifyEvent(events, "BULK_ATOMIC_UPDATE_FINALIZE.WARNING");
            verifyEvent(events, "BULK_ATOMIC_UPDATE_UNIT_DESC.KO");
        }
    }

    private void initAccessContract(AdminManagementClient functionalClient, final GUID operationGuid)
        throws InvalidParseOperationException, AdminManagementClientServerException, ReferentialNotFoundException,
        ContentAddressableStorageServerException {
        RequestResponse<AccessContractModel> accessContractResponse = functionalClient
            .findAccessContractsByID(CONTRACT_ID);
        if (!accessContractResponse.isOk()
            || ((RequestResponseOK<AccessContractModel>) accessContractResponse).isEmpty()) {
            fail("count not find access contract");
        }
        AccessContractModel accessContractModel = ((RequestResponseOK<AccessContractModel>) accessContractResponse)
            .getFirstResult();
        workspaceClient.putObject(operationGuid.getId(), ACCESS_CONTRACT_FILE_INPUT,
            JsonHandler.writeToInpustream(accessContractModel));
    }

    private JsonNode getLogbookOperation(final String containerName)
        throws InvalidCreateOperationException, LogbookClientException, InvalidParseOperationException {
        LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
        Select selectQuery = new Select();
        selectQuery.setQuery(QueryHelper.eq(EV_ID_PROC, containerName));
        return logbookClient.selectOperation(selectQuery.getFinalSelect());
    }


    private void verifyEvent(JsonNode events, String s) {
        List<JsonNode> massUpdateFinalized = events.findValues(OUT_DETAIL).stream().filter(e -> e.asText().equals(s))
            .collect(Collectors.toList());
        assertThat(massUpdateFinalized.size()).isGreaterThan(0);
    }

    /**
     * insertUnitAndLFC
     *
     * @param unitFile
     * @param lfcFile
     * @throws fr.gouv.vitam.common.exception.InvalidParseOperationException
     * @throws FileNotFoundException
     * @throws DatabaseException
     */
    private void insertUnitAndLFC(final String unitFile, final String lfcFile)
        throws fr.gouv.vitam.common.exception.InvalidParseOperationException, FileNotFoundException,
        DatabaseException {
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_0));
        List<Document> units = JsonHandler.getFromFileAsTypeReference(PropertiesUtils.getResourceFile(unitFile),
            TYPE_LIST_UNIT);
        VitamRepositoryFactory.get().getVitamMongoRepository(MetadataCollections.UNIT.getVitamCollection()).save(units);
        VitamRepositoryFactory.get().getVitamESRepository(MetadataCollections.UNIT.getVitamCollection(),
            metadataIndexManager.getElasticsearchIndexAliasResolver(MetadataCollections.UNIT)).save(units);

        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_0));
        List<JsonNode> unitsLfc = JsonHandler.getFromFileAsTypeReference(PropertiesUtils.getResourceFile(lfcFile),
            new TypeReference<>() {
            });
        List<Document> lfcs = unitsLfc.stream().map(item -> Document.parse(JsonHandler.unprettyPrint(item)))
            .collect(Collectors.toList());

        new VitamMongoRepository(LogbookCollections.LIFECYCLE_UNIT.getCollection()).save(lfcs);
    }

    /**
     * create a logbook operation
     *
     * @param operationId
     * @param objectId
     * @param type
     * @param typeProc
     * @throws LogbookClientBadRequestException
     * @throws LogbookClientAlreadyExistsException
     * @throws LogbookClientServerException
     */
    private void createLogbookOperation(GUID operationId, GUID objectId, String type, LogbookTypeProcess typeProc)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException {

        final LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
        final LogbookOperationParameters initParameters = LogbookParameterHelper.newLogbookOperationParameters(
            operationId, type, objectId, typeProc, StatusCode.STARTED,
            operationId != null ? operationId.toString() : "outcomeDetailMessage", operationId);
        initParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
            VitamLogbookMessages.getLabelOp(type + ".STARTED") + " : " + operationId);
        logbookClient.create(initParameters);
    }

    private Map<Integer, JsonNode> getReportDetailsByQueryIndex(List<JsonNode> reportLines) {
        return reportLines.subList(3, reportLines.size()).stream()
            .collect(Collectors.toMap(
                i -> Integer.parseInt(i.get("id").asText()),
                i -> i
            ));
    }
}

