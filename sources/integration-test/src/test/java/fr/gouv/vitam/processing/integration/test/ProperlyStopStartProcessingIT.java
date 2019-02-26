/*
 *  Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *  <p>
 *  contact.vitam@culture.gouv.fr
 *  <p>
 *  This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 *  high volumetry securely and efficiently.
 *  <p>
 *  This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 *  software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 *  circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 *  <p>
 *  As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 *  users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 *  successive licensors have only limited liability.
 *  <p>
 *  In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 *  developing or reproducing the software by the user in light of its specific status of free software, that may mean
 *  that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 *  experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 *  software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 *  to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *  <p>
 *  The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 *  accept its terms.
 */

package fr.gouv.vitam.processing.integration.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.google.common.collect.Sets;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.parameters.Contexts;
import fr.gouv.vitam.processing.common.model.DistributorIndex;
import fr.gouv.vitam.processing.common.model.PauseRecover;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.processing.common.model.WorkerBean;
import fr.gouv.vitam.processing.common.model.WorkerRemoteConfiguration;
import fr.gouv.vitam.processing.data.core.management.WorkspaceProcessDataManagement;
import fr.gouv.vitam.processing.distributor.api.ProcessDistributor;
import fr.gouv.vitam.processing.engine.core.monitoring.ProcessMonitoringImpl;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.io.File;
import java.nio.file.Files;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

public class ProperlyStopStartProcessingIT extends VitamRuleRunner {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProperlyStopStartProcessingIT.class);


    @ClassRule
    public static VitamServerRunner runner =
        new VitamServerRunner(ProperlyStopStartProcessingIT.class, mongoRule.getMongoDatabase().getName(),
            elasticsearchRule.getClusterName(),
            Sets.newHashSet(
                WorkspaceMain.class,
                ProcessManagementMain.class
            ));
    private static final Integer TENANT_ID = 0;
    private static final long SLEEP_TIME = 20l;
    private static final long NB_TRY = 18000;

    public static final String INGEST_LEVEL_STACK_JSON =
        "integration-processing/ingestLevelStack.json";
    public static final String UNITS_LEVEL_STACK_PATH = "UnitsLevel/ingestLevelStack.json";

    public static final String EXISING_GOT_FILE =
        "integration-processing/existing_object_group.json";

    public static final String EXISTING_GOT = "UpdateObjectGroup/existing_object_group.json";


    @ClassRule
    public static WireMockClassRule workerMockRule = new WireMockClassRule(options().dynamicPort());
    @Rule
    public WireMockClassRule workerInstance = workerMockRule;

    private WorkspaceClient workspaceClient;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        handleBeforeClass(0, 1);
        // set bulk size to 1 for tests
        VitamConfiguration.setWorkerBulkSize(1);
        // TODO perhaps we have to delete worker.db FileUtils.forceDeleteOnExit(PropertiesUtils.fileFromDataFolder("worker.db"));
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        handleAfterClass(0, 1);
        runAfter();

        VitamConfiguration.setWorkerBulkSize(10);

        ProcessingManagementClientFactory.getInstance().getClient()
            .unregisterWorker("DefaultWorker", String.valueOf(ServerIdentity.getInstance().getGlobalPlatformId()));

        try (WorkspaceClient workspaceClient = WorkspaceClientFactory.getInstance().getClient()) {
            workspaceClient.deleteContainer("process", true);
        } catch (Exception e) {
            LOGGER.error(e);
        }

        workerMockRule.shutdownServer();
        VitamClientFactory.resetConnections();
    }

    @Before
    public void setUp() throws Exception {

        // Delete eventually existing workerdb file
        final WorkerBean workerBean =
            new WorkerBean("DefaultWorker", "DefaultWorker", 1, 0, "status",
                new WorkerRemoteConfiguration("localhost", workerMockRule.port()));
        workerBean.setWorkerId(String.valueOf(ServerIdentity.getInstance().getGlobalPlatformId()));
        ProcessingManagementClientFactory.getInstance().getClient()
            .registerWorker("DefaultWorker", String.valueOf(ServerIdentity.getInstance().getGlobalPlatformId()),
                workerBean);

        workerInstance.stubFor(WireMock.get(urlMatching("/worker/v1/status"))
            .willReturn(
                aResponse().withStatus(200).withHeader(GlobalDataRest.X_TENANT_ID, Integer.toString(TENANT_ID))));


        workerInstance.stubFor(WireMock.post(urlMatching("/worker/v1/tasks"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody(JsonHandler.unprettyPrint(getMockedItemStatus(StatusCode.OK)))
                    .withHeader(GlobalDataRest.X_TENANT_ID, Integer.toString(TENANT_ID))
                    .withHeader("Content-Type", "application/json"))
        );
    }

    ItemStatus getMockedItemStatus(StatusCode statusCode) {
        return new ItemStatus("StepId")
            .setItemsStatus("ItemId",
                new ItemStatus("ItemId")
                    .setMessage("message")
                    .increment(statusCode)
            );
    }


    private void waitStep(ProcessWorkflow processWorkflow, int stepId) {

        while (processWorkflow.getSteps().get(stepId).getStepStatusCode() != StatusCode.STARTED) {
            try {
                LOGGER.error("== Wait step :" + stepId);
                Thread.sleep(1);
            } catch (InterruptedException e) {
                SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            }
        }
    }

    private void wait(String operationId) {
        int nbTry = 0;
        while (!ProcessingManagementClientFactory.getInstance().getClient().isOperationCompleted(operationId)) {
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


    @Test
    @RunWithCustomExecutor
    public void whenProcessingServerStopStartThenPauseStartProperlyProcessWorkflow() throws Exception {
        ProcessingIT.prepareVitamSession();
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_ID);
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        final String containerName = operationGuid.toString();
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        ProcessingManagementClientFactory.getInstance().getClient()
            .initVitamProcess(containerName, Contexts.DEFAULT_WORKFLOW.name());

        final File resourceFile = PropertiesUtils.getResourceFile(INGEST_LEVEL_STACK_JSON);
        workspaceClient
            .putObject(containerName, UNITS_LEVEL_STACK_PATH, Files.newInputStream(resourceFile.toPath()));

        final File existing_got = PropertiesUtils.getResourceFile(EXISING_GOT_FILE);
        workspaceClient
            .putObject(containerName, EXISTING_GOT, Files.newInputStream(existing_got.toPath()));

        ProcessWorkflow processWorkflow =
            ProcessMonitoringImpl.getInstance().findOneProcessWorkflow(containerName, TENANT_ID);

        RequestResponse<JsonNode> resp = ProcessingManagementClientFactory.getInstance().getClient()
            .executeOperationProcess(containerName, Contexts.DEFAULT_WORKFLOW.name(), ProcessAction.RESUME.getValue());
        // wait a little bit
        assertThat(resp).isNotNull();
        assertThat(resp.getStatus()).isEqualTo(Response.Status.ACCEPTED.getStatusCode());

        waitStep(processWorkflow, 2);

        // shutdown processing
        runner.stopProcessManagementServer(false);

        // wait a little bit
        LOGGER.error("=== After STOP");

        assertThat(processWorkflow.getState()).isEqualTo(ProcessState.PAUSE);
        assertThat(processWorkflow.getStatus()).isEqualTo(StatusCode.WARNING);
        assertThat(processWorkflow.getPauseRecover()).isEqualTo(PauseRecover.RECOVER_FROM_SERVER_PAUSE);
        DistributorIndex distributorIndex =
            WorkspaceProcessDataManagement.getInstance()
                .getDistributorIndex(ProcessDistributor.DISTRIBUTOR_INDEX, containerName);

        assertThat(distributorIndex).isNotNull();
        assertThat(distributorIndex.getItemStatus()).isNotNull();
        assertThat(distributorIndex.getItemStatus().getItemsStatus()).isNotEmpty();

        // restart processing
        runner.startProcessManagementServer();

        LOGGER.info("After RE-START");

        wait(containerName);

        processWorkflow =
            ProcessMonitoringImpl.getInstance().findOneProcessWorkflow(containerName, TENANT_ID);
        assertThat(processWorkflow).isNotNull();
        assertThat(processWorkflow.getStatus()).isEqualTo(StatusCode.WARNING);
        assertThat(processWorkflow.getState()).isEqualTo(ProcessState.COMPLETED);
        assertThat(processWorkflow.getPauseRecover()).isEqualTo(PauseRecover.NO_RECOVER);
    }
}
