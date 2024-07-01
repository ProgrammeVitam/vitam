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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Sets;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.database.builder.query.BooleanQuery;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.ProcessQuery;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AccessionRegisterDetailModel;
import fr.gouv.vitam.common.model.processing.ProcessDetail;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.Contexts;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.processing.engine.core.monitoring.ProcessMonitoringImpl;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import org.assertj.core.api.SoftAssertions;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.gouv.vitam.common.VitamTestHelper.insertWaitForStepEssentialFiles;
import static fr.gouv.vitam.common.VitamTestHelper.waitOperation;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.and;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.exists;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ReplayProcessingIT extends VitamRuleRunner {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ReplayProcessingIT.class);

    @ClassRule
    public static VitamServerRunner runner = new VitamServerRunner(
        ReplayProcessingIT.class,
        mongoRule.getMongoDatabase().getName(),
        ElasticsearchRule.getClusterName(),
        Sets.newHashSet(
            MetadataMain.class,
            WorkerMain.class,
            AdminManagementMain.class,
            LogbookMain.class,
            WorkspaceMain.class,
            ProcessManagementMain.class
        )
    );

    private static final Integer TENANT_ID = 0;

    private static final long SLEEP_TIME = 20L;
    private static final long NB_TRY = 18000;

    private static final String SIP_FOLDER = "SIP";

    private static final String SIP_OK_REPLAY_1 = "integration-processing/OK_TEST_REPLAY_1.zip";
    private static final String SIP_OK_REPLAY_2 = "integration-processing/OK_TEST_REPLAY_2.zip";
    private static DataLoader dataLoader = new DataLoader("integration-processing");

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        handleBeforeClass(Arrays.asList(0, 1), Collections.emptyMap());
        String configSiegfriedPath = PropertiesUtils.getResourcePath(
            "integration-processing/format-identifiers.conf"
        ).toString();

        FormatIdentifierFactory.getInstance().changeConfigurationFile(configSiegfriedPath);

        StorageClientFactory storageClientFactory = StorageClientFactory.getInstance();
        storageClientFactory.setVitamClientType(VitamClientFactoryInterface.VitamClientType.MOCK);

        dataLoader.prepareData();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        handleAfterClass();
        StorageClientFactory storageClientFactory = StorageClientFactory.getInstance();
        storageClientFactory.setVitamClientType(VitamClientFactoryInterface.VitamClientType.PRODUCTION);

        runAfter();

        try (WorkspaceClient workspaceClient = WorkspaceClientFactory.getInstance().getClient()) {
            workspaceClient.deleteContainer("process", true);
        } catch (ContentAddressableStorageNotFoundException e) {
            LOGGER.error(e);
        }
        VitamClientFactory.resetConnections();
    }

    private void createLogbookOperation(GUID operationId, GUID objectId)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException {
        final LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient();

        final LogbookOperationParameters initParameters = LogbookParameterHelper.newLogbookOperationParameters(
            operationId,
            "Process_SIP_unitary",
            objectId,
            LogbookTypeProcess.INGEST,
            StatusCode.STARTED,
            operationId != null ? operationId.toString() : "outcomeDetailMessage",
            operationId
        );
        logbookClient.create(initParameters);
    }

    @RunWithCustomExecutor
    @Test
    public void testPauseAndReplayStepInAWorkflow() throws Exception {
        ProcessingIT.prepareVitamSession();
        String containerNameNoReplay = launchIngest(false);
        String containerNameReplay = launchIngest(true);

        LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
        JsonNode logbookResultReplay = logbookClient.selectOperationById(containerNameReplay);
        JsonNode logbookResultNoReplay = logbookClient.selectOperationById(containerNameNoReplay);
        validateLogbookOperations(
            logbookResultReplay.get("$results").get(0),
            logbookResultNoReplay.get("$results").get(0)
        );

        LogbookLifeCyclesClient lifecycleClient = LogbookLifeCyclesClientFactory.getInstance().getClient();
        final Select selectReplay = new Select();
        selectReplay.setQuery(QueryHelper.eq("evIdProc", containerNameReplay));
        RequestResponseOK requestResponseOKReplay = RequestResponseOK.getFromJsonNode(
            lifecycleClient.selectUnitLifeCycle(selectReplay.getFinalSelect())
        );

        final Select selectNoReplay = new Select();
        selectNoReplay.setQuery(QueryHelper.eq("evIdProc", containerNameNoReplay));
        RequestResponseOK requestResponseOKNoReplay = RequestResponseOK.getFromJsonNode(
            lifecycleClient.selectUnitLifeCycle(selectNoReplay.getFinalSelect())
        );
        validateLogbookLifecycles(
            requestResponseOKReplay.getResults(),
            requestResponseOKNoReplay.getResults(),
            "Units"
        );

        final Select selectObjReplay = new Select();
        selectObjReplay.setQuery(QueryHelper.eq("evIdProc", containerNameReplay));
        RequestResponseOK requestObjResponseOKReplay = RequestResponseOK.getFromJsonNode(
            lifecycleClient.selectObjectGroupLifeCycle(selectObjReplay.getFinalSelect())
        );
        final Select selectObjNoReplay = new Select();
        selectObjNoReplay.setQuery(QueryHelper.eq("evIdProc", containerNameNoReplay));
        RequestResponseOK requestObjResponseOKNoReplay = RequestResponseOK.getFromJsonNode(
            lifecycleClient.selectObjectGroupLifeCycle(selectObjNoReplay.getFinalSelect())
        );

        validateLogbookLifecycles(
            requestObjResponseOKReplay.getResults(),
            requestObjResponseOKNoReplay.getResults(),
            "Objects"
        );

        validateUnitsGoTs(containerNameReplay, containerNameNoReplay);

        validateAccessRegInDetail();
    }

    private void validateAccessRegInDetail() throws Exception {
        try (AdminManagementClient adminClient = AdminManagementClientFactory.getInstance().getClient()) {
            Select select = new Select();
            final BooleanQuery query = and();
            query.add(exists("OriginatingAgency"));
            select.setQuery(query);
            RequestResponse<AccessionRegisterDetailModel> responseNoReplay = adminClient.getAccessionRegisterDetail(
                "TestReplayOriginAgency1",
                select.getFinalSelect()
            );
            RequestResponse<AccessionRegisterDetailModel> responseReplay = adminClient.getAccessionRegisterDetail(
                "TestReplayOriginAgency2",
                select.getFinalSelect()
            );

            if (responseNoReplay.isOk() && responseReplay.isOk()) {
                RequestResponseOK<AccessionRegisterDetailModel> responseReplayOK = (RequestResponseOK<
                        AccessionRegisterDetailModel
                    >) responseReplay;
                RequestResponseOK<AccessionRegisterDetailModel> responseNoReplayOK = (RequestResponseOK<
                        AccessionRegisterDetailModel
                    >) responseNoReplay;

                assertEquals(responseReplayOK.getResults().size(), responseNoReplayOK.getResults().size());
            }
        }
    }

    private void validateUnitsGoTs(String containerNameReplay, String containerNameNoReplay) throws Exception {
        try (final MetaDataClient metadataClient = MetaDataClientFactory.getInstance().getClient()) {
            // First thing first - units
            SelectMultiQuery selectReplay = new SelectMultiQuery();
            selectReplay.addQueries(QueryHelper.in(VitamFieldsHelper.operations(), containerNameReplay));
            selectReplay.addOrderByDescFilter("Title");
            ArrayNode resultUnitsReplay = (ArrayNode) metadataClient
                .selectUnits(selectReplay.getFinalSelect())
                .get("$results");

            SelectMultiQuery selectNoReplay = new SelectMultiQuery();
            selectNoReplay.addQueries(QueryHelper.in(VitamFieldsHelper.operations(), containerNameNoReplay));
            selectNoReplay.addOrderByDescFilter("Title");
            ArrayNode resultUnitsNoReplay = (ArrayNode) metadataClient
                .selectUnits(selectNoReplay.getFinalSelect())
                .get("$results");

            assertEquals(resultUnitsReplay.size(), resultUnitsNoReplay.size());
            assertEquals(
                resultUnitsReplay.get(0).get("Title").asText(),
                resultUnitsNoReplay.get(0).get("Title").asText()
            );

            // And now - GoT
            SelectMultiQuery selectObjReplay = new SelectMultiQuery();
            selectObjReplay.addQueries(QueryHelper.in(VitamFieldsHelper.operations(), containerNameReplay));
            ArrayNode resultObjReplay = (ArrayNode) metadataClient
                .selectObjectGroups(selectObjReplay.getFinalSelect())
                .get("$results");

            SelectMultiQuery selectObjNoReplay = new SelectMultiQuery();
            selectObjNoReplay.addQueries(QueryHelper.in(VitamFieldsHelper.operations(), containerNameNoReplay));
            ArrayNode resultObjectsNoReplay = (ArrayNode) metadataClient
                .selectObjectGroups(selectObjNoReplay.getFinalSelect())
                .get("$results");

            assertEquals(resultObjReplay.size(), resultObjectsNoReplay.size());
        }
    }

    private void validateLogbookLifecycles(
        List<JsonNode> logbookResultReplay,
        List<JsonNode> logbookResultNoReplay,
        String type
    ) {
        assertEquals(logbookResultReplay.size(), logbookResultNoReplay.size());
        // maybe later, it could be improved
    }

    private void validateLogbookOperations(JsonNode logbookResultReplay, JsonNode logbookResultNoReplay)
        throws Exception {
        JsonNode evDetDataReplay = JsonHandler.getFromString(logbookResultReplay.get("evDetData").asText());
        JsonNode evDetDataNotReplay = JsonHandler.getFromString(logbookResultNoReplay.get("evDetData").asText());
        assertEquals(evDetDataReplay.get("EvDetailReq").asText(), evDetDataNotReplay.get("EvDetailReq").asText());
        Map<String, Integer> listStepsToCheck = new HashMap<>();

        ArrayNode eventsNoReplay = (ArrayNode) logbookResultNoReplay.get("events");
        for (JsonNode event : eventsNoReplay) {
            if ("OK".equals(event.get("outcome").asText())) {
                String evType = event.get("evType").asText();
                if (listStepsToCheck.containsKey(evType)) {
                    listStepsToCheck.put(evType, listStepsToCheck.get(evType) + 1);
                } else {
                    listStepsToCheck.put(evType, 1);
                }
            }
        }
        Map<String, Integer> listStepsExecutedReplay = new HashMap<>();
        ArrayNode eventsReplay = (ArrayNode) logbookResultReplay.get("events");
        for (JsonNode event : eventsReplay) {
            if (
                "OK".equals(event.get("outcome").asText()) || "ALREADY_EXECUTED".equals(event.get("outcome").asText())
            ) {
                String evType = event.get("evType").asText();
                if (listStepsExecutedReplay.containsKey(evType)) {
                    listStepsExecutedReplay.put(evType, listStepsExecutedReplay.get(evType) + 1);
                } else {
                    listStepsExecutedReplay.put(evType, 1);
                }
            }
        }
        SoftAssertions.assertSoftly(softAssertions -> {
            listStepsExecutedReplay.forEach((k, v) -> {
                if (
                    "ATR_NOTIFICATION".equals(k) ||
                    "ROLL_BACK".equals(k) ||
                    "PROCESS_SIP_UNITARY".equals(k) ||
                    "STP_INGEST_FINALISATION".equals(k) ||
                    k.endsWith(".STARTED")
                ) {
                    softAssertions.assertThat(listStepsToCheck.get(k)).isEqualTo(v);
                } else {
                    softAssertions.assertThat(2 * listStepsToCheck.get(k)).as("step " + k + " failed").isEqualTo(v);
                }
            });
        });
    }

    private String launchIngest(boolean replayModeActivated) throws Exception {
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_ID);
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        final GUID objectGuid = GUIDFactory.newManifestGUID(TENANT_ID);
        final String containerName = objectGuid.getId();
        createLogbookOperation(operationGuid, objectGuid);

        // workspace client dezip SIP in workspace
        InputStream zipInputStreamSipObject;
        if (replayModeActivated) {
            zipInputStreamSipObject = PropertiesUtils.getResourceAsStream(SIP_OK_REPLAY_2);
        } else {
            zipInputStreamSipObject = PropertiesUtils.getResourceAsStream(SIP_OK_REPLAY_1);
        }

        WorkspaceClient workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP, zipInputStreamSipObject);
        // Insert sanityCheck file & StpUpload
        insertWaitForStepEssentialFiles(containerName);

        ProcessingManagementClient processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        processingClient.initVitamProcess(containerName, Contexts.DEFAULT_WORKFLOW.name());
        if (replayModeActivated) {
            // wait a little bit
            ProcessWorkflow processWorkflow = null;
            String currentStep = null;
            boolean executedOnce = false;
            while (true) {
                if (!executedOnce) {
                    // First launch of the step
                    RequestResponse<ItemStatus> resp = processingClient.executeOperationProcess(
                        containerName,
                        Contexts.DEFAULT_WORKFLOW.name(),
                        ProcessAction.NEXT.getValue()
                    );
                    // wait a little bit
                    assertNotNull(resp);
                    assertThat(resp.isOk()).isTrue();
                    assertEquals(Response.Status.ACCEPTED.getStatusCode(), resp.getStatus());
                    waitOperation(NB_TRY, SLEEP_TIME, containerName);

                    ProcessQuery query = new ProcessQuery();
                    query.setId(containerName);
                    RequestResponseOK<ProcessDetail> response = (RequestResponseOK<
                            ProcessDetail
                        >) processingClient.listOperationsDetails(query);
                    ProcessDetail currentProcess = response.getResults().get(0);
                    currentStep = currentProcess.getPreviousStep();

                    processWorkflow = ProcessMonitoringImpl.getInstance()
                        .findOneProcessWorkflow(containerName, TENANT_ID);
                    assertNotNull(processWorkflow);
                    assertEquals(ProcessState.PAUSE, processWorkflow.getState());
                    // assertEquals(StatusCode.OK, processWorkflow.getStatus());

                    System.out.println("Launch step : " + currentStep);
                    executedOnce = true;
                } else {
                    // LETS REPLAY THE SAME STEP AGAIN
                    RequestResponse<ItemStatus> ret = processingClient.updateOperationActionProcess(
                        ProcessAction.REPLAY.getValue(),
                        containerName
                    );
                    assertNotNull(ret);
                    assertEquals(Response.Status.ACCEPTED.getStatusCode(), ret.getStatus());
                    waitOperation(NB_TRY, SLEEP_TIME, containerName);

                    ProcessQuery query = new ProcessQuery();
                    query.setId(containerName);
                    RequestResponseOK<ProcessDetail> response = (RequestResponseOK<
                            ProcessDetail
                        >) processingClient.listOperationsDetails(query);
                    ProcessDetail currentProcess = response.getResults().get(0);
                    currentStep = currentProcess.getPreviousStep();

                    processWorkflow = ProcessMonitoringImpl.getInstance()
                        .findOneProcessWorkflow(containerName, TENANT_ID);
                    assertNotNull(processWorkflow);
                    assertEquals(ProcessState.PAUSE, processWorkflow.getState());
                    // assertEquals(StatusCode.OK, processWorkflow.getStatus());
                    executedOnce = false;
                }
                if ("STP_ACCESSION_REGISTRATION".equals(currentStep) && !executedOnce) {
                    break;
                }
            }
            RequestResponse<ItemStatus> ret = processingClient.updateOperationActionProcess(
                ProcessAction.RESUME.getValue(),
                containerName
            );
            assertNotNull(ret);
            assertEquals(Response.Status.ACCEPTED.getStatusCode(), ret.getStatus());
            waitOperation(NB_TRY, SLEEP_TIME, containerName);
            processWorkflow = ProcessMonitoringImpl.getInstance().findOneProcessWorkflow(containerName, TENANT_ID);
            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.OK, processWorkflow.getStatus());
        } else {
            final RequestResponse<ItemStatus> ret = processingClient.executeOperationProcess(
                containerName,
                Contexts.DEFAULT_WORKFLOW.name(),
                ProcessAction.RESUME.getValue()
            );

            assertNotNull(ret);
            assertThat(ret.isOk()).isTrue();
            assertEquals(Status.ACCEPTED.getStatusCode(), ret.getStatus());

            waitOperation(NB_TRY, SLEEP_TIME, containerName);
            ProcessWorkflow processWorkflow = ProcessMonitoringImpl.getInstance()
                .findOneProcessWorkflow(containerName, TENANT_ID);
            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.OK, processWorkflow.getStatus());
        }
        return containerName;
    }
}
