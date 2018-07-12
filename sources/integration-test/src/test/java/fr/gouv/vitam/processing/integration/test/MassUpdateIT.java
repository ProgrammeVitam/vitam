/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 *******************************************************************************/
package fr.gouv.vitam.processing.integration.test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.database.api.VitamRepositoryFactory;
import fr.gouv.vitam.common.database.api.impl.VitamMongoRepository;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
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
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.database.collections.Unit;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.processing.data.core.ProcessDataAccessImpl;
import fr.gouv.vitam.processing.engine.core.monitoring.ProcessMonitoringImpl;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.common.rest.DefaultOfferMain;
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

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * MassUpdate Itests.
 */
public class MassUpdateIT extends VitamRuleRunner {

    private static final String INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_JSON =
        "integration-processing/mass-update/unit.json";
    private static final String INTEGRATION_PROCESSING_MASS_UPDATE_CONTRACT_PERMISSION_RESTRICTED_DESC_JSON =
        "integration-processing/mass-update/contract_permission_restricted_desc.json";
    public static final String STP_MASS_UPDATE_UNIT = "STP_MASS_UPDATE_UNIT";
    public static final String INTEGRATION_PROCESSING_MASS_UPDATE_UPDATE_QUERY_02_JSON =
        "integration-processing/mass-update/update_query_02.json";
    public static final String INTEGRATION_PROCESSING_MASS_UPDATE_UPDATE_QUERY_01_JSON =
        "integration-processing/mass-update/update_query_01.json";
    private static String SIP = "integration-processing/OK_ARBO-COMPLEXE.zip";
    private static final String FAKE_CONTEXT_ID = "fakeContextId";
    private static final String CONTRACT_ID = "AC-000001";
    private static final String SIP_FOLDER = "SIP";
    private static final int TENANT_0 = 0;

    @ClassRule
    public static VitamServerRunner runner =
        new VitamServerRunner(ProcessingIT.class, mongoRule.getMongoDatabase().getName(),
            elasticsearchRule.getClusterName(),
            Sets.newHashSet(
                MetadataMain.class,
                WorkerMain.class,
                AdminManagementMain.class,
                LogbookMain.class,
                WorkspaceMain.class,
                ProcessManagementMain.class,
                StorageMain.class,
                DefaultOfferMain.class
            ));

    private static final long SLEEP_TIME = 20l;
    private static final long NB_TRY = 18000;
    private static String CONFIG_BIG_WORKER_PATH = "";

    private WorkspaceClient workspaceClient;
    private ProcessingManagementClient processingClient;
    private static ProcessMonitoringImpl processMonitoring;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        CONFIG_BIG_WORKER_PATH = PropertiesUtils.getResourcePath("integration-processing/bigworker.conf").toString();
        FormatIdentifierFactory.getInstance().changeConfigurationFile(runner.FORMAT_IDENTIFIERS_CONF);
        processMonitoring = ProcessMonitoringImpl.getInstance();
        StorageClientFactory storageClientFactory = StorageClientFactory.getInstance();
        storageClientFactory.setVitamClientType(VitamClientFactoryInterface.VitamClientType.MOCK);
        new DataLoader("integration-processing").prepareData();

    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        StorageClientFactory storageClientFactory = StorageClientFactory.getInstance();
        storageClientFactory.setVitamClientType(VitamClientFactoryInterface.VitamClientType.PRODUCTION);
        runAfter();
    }

    @After
    public void afterTest() throws Exception {
        VitamThreadUtils.getVitamSession().setContractId(CONTRACT_ID);
        VitamThreadUtils.getVitamSession().setContextId(FAKE_CONTEXT_ID);

        ProcessDataAccessImpl.getInstance().clearWorkflow();
        runAfterMongo(Sets.newHashSet(
            MetadataCollections.UNIT.getName(),
            MetadataCollections.OBJECTGROUP.getName(),
            FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getName(),
            FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getName(),
            LogbookCollections.OPERATION.getName(),
            LogbookCollections.LIFECYCLE_UNIT.getName(),
            LogbookCollections.LIFECYCLE_OBJECTGROUP.getName(),
            LogbookCollections.LIFECYCLE_OBJECTGROUP.getName(),
            LogbookCollections.LIFECYCLE_UNIT_IN_PROCESS.getName()

        ));

        runAfterEs(Sets.newHashSet(
            MetadataCollections.UNIT.getName().toLowerCase() + "_0",
            MetadataCollections.UNIT.getName().toLowerCase() + "_1",
            MetadataCollections.OBJECTGROUP.getName().toLowerCase() + "_0",
            MetadataCollections.OBJECTGROUP.getName().toLowerCase() + "_1",
            LogbookCollections.OPERATION.getName().toLowerCase() + "_0",
            LogbookCollections.OPERATION.getName().toLowerCase() + "_1",
            FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getName().toLowerCase(),
            FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getName().toLowerCase()
        ));
    }

    private void wait(String operationId) {
        int nbTry = 0;
        while (!processingClient.isOperationCompleted(operationId)) {
            try {
                Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException e) {
                SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            }
            if (nbTry == NB_TRY)
                break;
            nbTry++;
        }
    }

    @RunWithCustomExecutor
    @Test
    public void updateWorkflowTestKO() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        VitamThreadUtils.getVitamSession().setContractId(CONTRACT_ID);
        VitamThreadUtils.getVitamSession().setContextId(FAKE_CONTEXT_ID);
        try (AdminManagementClient functionalClient = AdminManagementClientFactory.getInstance().getClient()) {
            final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_0);
            final String containerName = operationGuid.getId();
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            createLogbookOperation(operationGuid, operationGuid, null, LogbookTypeProcess.MASS_UPDATE);

            final InputStream zipInputStreamSipObject = PropertiesUtils.getResourceAsStream(SIP);
            workspaceClient = WorkspaceClientFactory.getInstance().getClient();
            workspaceClient.createContainer(containerName);
            workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP,
                zipInputStreamSipObject);

            // insert units
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_0));
            List<Document> units = JsonHandler.getFromFileAsTypeRefence(PropertiesUtils.getResourceFile(
                INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_JSON),
                new TypeReference<List<Unit>>() {
                });
            VitamRepositoryFactory.get().getVitamMongoRepository(MetadataCollections.UNIT.getVitamCollection())
                .save(units);
            VitamRepositoryFactory.get().getVitamESRepository(MetadataCollections.UNIT.getVitamCollection())
                .save(units);

            // import contract
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_0));
            File accessContracts = PropertiesUtils
                .getResourceFile(INTEGRATION_PROCESSING_MASS_UPDATE_CONTRACT_PERMISSION_RESTRICTED_DESC_JSON);
            List<AccessContractModel> accessContractList =
                JsonHandler.getFromFileAsTypeRefence(accessContracts, new TypeReference<List<AccessContractModel>>() {
                });
            functionalClient.importAccessContracts(accessContractList);

            JsonNode query =
                JsonHandler.getFromFile(
                    PropertiesUtils.findFile(INTEGRATION_PROCESSING_MASS_UPDATE_UPDATE_QUERY_01_JSON));
            workspaceClient
                .putObject(operationGuid.getId(), "query.json", JsonHandler.writeToInpustream(query));
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            processingClient = ProcessingManagementClientFactory.getInstance().getClient();
            processingClient
                .initVitamProcess(Contexts.MASS_UPDATE.name(), containerName, Contexts.MASS_UPDATE.getEventType());

            RequestResponse<ItemStatus> ret =
                processingClient.updateOperationActionProcess(ProcessAction.RESUME.getValue(), containerName);
            assertNotNull(ret);
            assertEquals(Response.Status.ACCEPTED.getStatusCode(), ret.getStatus());

            wait(containerName);
            ProcessWorkflow processWorkflow = processMonitoring.findOneProcessWorkflow(containerName, TENANT_0);
            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.KO, processWorkflow.getStatus());
        }
    }

    @RunWithCustomExecutor
    @Test
    public void updateWorkflowTestOK() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        VitamThreadUtils.getVitamSession().setContractId("AC-000001");
        VitamThreadUtils.getVitamSession().setContextId("fakeContextId");

        try (AdminManagementClient functionalClient = AdminManagementClientFactory.getInstance().getClient()) {
            final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_0);
            final String containerName = operationGuid.getId();
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            //            createLogbookOperation(operationGuid, operationGuid);

            createLogbookOperation(operationGuid, operationGuid, null, LogbookTypeProcess.MASS_UPDATE);

            // workspace client unzip SIP in workspace
            final InputStream zipInputStreamSipObject = PropertiesUtils.getResourceAsStream(SIP);
            workspaceClient = WorkspaceClientFactory.getInstance().getClient();
            workspaceClient.createContainer(containerName);
            workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP,
                zipInputStreamSipObject);

            // insert units
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_0));
            List<Document> units = JsonHandler.getFromFileAsTypeRefence(PropertiesUtils.getResourceFile(
                INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_JSON),
                new TypeReference<List<Unit>>() {
                });
            VitamRepositoryFactory.get().getVitamMongoRepository(MetadataCollections.UNIT.getVitamCollection())
                .save(units);
            VitamRepositoryFactory.get().getVitamESRepository(MetadataCollections.UNIT.getVitamCollection())
                .save(units);

            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_0));
            List<JsonNode> unitsLfc = JsonHandler.getFromFileAsTypeRefence(PropertiesUtils.getResourceFile(
                "integration-processing/mass-update/unit_lfc.json"),
                new TypeReference<List<JsonNode>>() {
                });
            List<Document> lfcs = unitsLfc.stream()
                .map(item -> Document.parse(JsonHandler.unprettyPrint(item))).collect(Collectors.toList());

            new VitamMongoRepository(LogbookCollections.LIFECYCLE_UNIT.getCollection()).save(lfcs);

            // import contract
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_0));
            File accessContracts = PropertiesUtils
                .getResourceFile(INTEGRATION_PROCESSING_MASS_UPDATE_CONTRACT_PERMISSION_RESTRICTED_DESC_JSON);
            List<AccessContractModel> accessContractList =
                JsonHandler.getFromFileAsTypeRefence(accessContracts, new TypeReference<List<AccessContractModel>>() {
                });
            functionalClient.importAccessContracts(accessContractList);

            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            processingClient = ProcessingManagementClientFactory.getInstance().getClient();
            JsonNode query =
                JsonHandler.getFromFile(
                    PropertiesUtils.findFile(INTEGRATION_PROCESSING_MASS_UPDATE_UPDATE_QUERY_02_JSON));
            workspaceClient
                .putObject(operationGuid.getId(), "query.json", JsonHandler.writeToInpustream(query));

            //            processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName, WORFKLOW_NAME);
            processingClient
                .initVitamProcess(Contexts.MASS_UPDATE.name(), containerName, Contexts.MASS_UPDATE.getEventType());

            RequestResponse<ItemStatus> ret =
                processingClient.updateOperationActionProcess(ProcessAction.RESUME.getValue(), containerName);
            assertNotNull(ret);
            assertEquals(Response.Status.ACCEPTED.getStatusCode(), ret.getStatus());

            wait(containerName);
            ProcessWorkflow processWorkflow = processMonitoring.findOneProcessWorkflow(containerName, TENANT_0);
            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.OK, processWorkflow.getStatus());

            LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
            fr.gouv.vitam.common.database.builder.request.single.Select selectQuery =
                new Select();
            selectQuery.setQuery(QueryHelper.eq("evIdProc", containerName));
            JsonNode logbookResult = logbookClient.selectOperation(selectQuery.getFinalSelect());

            // as logbookClient.selectOperation returns last two events and after removing STARTED from events
            // the order is main-event > sub-events, so events[0] will be "ROLL_BACK.OK" and not
            // "STP_INGEST_FINALISATION.OK"
            assertEquals(logbookResult.get("$results").get(0).get("events").get(0).get("outDetail").asText(),
                "MASS_UPDATE_FINALIZE.OK");
            assertEquals(logbookResult.get("$results").get(0).get("events").get(1).get("outDetail").asText(),
                "MASS_UPDATE.OK");
        }
    }

    public void createLogbookOperation(GUID operationId, GUID objectId, String type, LogbookTypeProcess typeProc)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException {

        final LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
        if (type == null) {
            type = STP_MASS_UPDATE_UNIT;
        }
        final LogbookOperationParameters initParameters = LogbookParametersFactory.newLogbookOperationParameters(
            operationId, type, objectId,
            typeProc, StatusCode.STARTED,
            operationId != null ? operationId.toString() : "outcomeDetailMessage",
            operationId);
        if (STP_MASS_UPDATE_UNIT.equals(type)) {
            initParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                VitamLogbookMessages.getLabelOp(STP_MASS_UPDATE_UNIT + ".STARTED") + " : " + operationId);
        }
        logbookClient.create(initParameters);
    }

    public void createLogbookOperation(GUID operationId, GUID objectId)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException {
        createLogbookOperation(operationId, objectId, null, LogbookTypeProcess.MASS_UPDATE);
    }
}
