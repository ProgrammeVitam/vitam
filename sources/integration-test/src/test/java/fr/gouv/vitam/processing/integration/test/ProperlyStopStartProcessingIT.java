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
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.parameters.Contexts;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.processing.common.model.DistributorIndex;
import fr.gouv.vitam.processing.common.model.PauseRecover;
import fr.gouv.vitam.processing.common.model.ProcessStep;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.processing.common.model.WorkerBean;
import fr.gouv.vitam.processing.common.model.WorkerRemoteConfiguration;
import fr.gouv.vitam.processing.data.core.management.WorkspaceProcessDataManagement;
import fr.gouv.vitam.processing.distributor.api.ProcessDistributor;
import fr.gouv.vitam.processing.engine.core.monitoring.ProcessMonitoringImpl;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.processing.management.rest.ProcessManagementApplication;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.ws.rs.core.Response;
import java.io.File;
import java.nio.file.Files;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.assertj.core.api.Assertions.assertThat;

public class ProperlyStopStartProcessingIT {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProperlyStopStartProcessingIT.class);

    private static final Integer TENANT_ID = 0;
    private static final long SLEEP_TIME = 100l;
    private static final long NB_TRY = 4800; // equivalent to 4 minute

    private static final String WORFKLOW_NAME = "PROCESS_SIP_UNITARY";

    private static final int PORT_SERVICE_WORKSPACE = 8094;
    private static final int PORT_SERVICE_PROCESSING = 8097;
    public static final String INGEST_LEVEL_STACK_JSON =
        "integration-processing/ingestLevelStack.json";
    public static final String UNITS_LEVEL_STACK_PATH = "UnitsLevel/ingestLevelStack.json";

    private static JunitHelper junitHelper = JunitHelper.getInstance();

    private static int workerPort = junitHelper.findAvailablePort();
    private static int logbookPort = junitHelper.findAvailablePort();

    @ClassRule
    public static WireMockClassRule workerMockRule = new WireMockClassRule(workerPort);
    @Rule
    public WireMockClassRule worckerInstance = workerMockRule;

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    private static String CONFIG_WORKSPACE_PATH;
    private static String CONFIG_PROCESSING_PATH;
    private static final String WORKSPACE_URL = "http://localhost:" + PORT_SERVICE_WORKSPACE;
    private static final String PROCESSING_URL = "http://localhost:" + PORT_SERVICE_PROCESSING;

    private static WorkspaceMain workspaceMain;
    private static ProcessManagementApplication processManagementApplication;

    private WorkspaceClient workspaceClient;
    private ProcessingManagementClient processingClient;


    @Before
    public void setUp() throws Exception {

        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        final WorkerBean workerBean =
            new WorkerBean("DefaultWorker", "DefaultWorker", 10, 0, "status",
                new WorkerRemoteConfiguration("localhost", workerPort));
        workerBean.setWorkerId("FakeWorkerId");
        processingClient.registerWorker("DefaultWorker", "1", workerBean);

        worckerInstance.stubFor(WireMock.get(urlMatching("/worker/v1/status"))
            .willReturn(
                aResponse().withStatus(200).withHeader(GlobalDataRest.X_TENANT_ID, Integer.toString(TENANT_ID))));


        worckerInstance.stubFor(WireMock.post(urlMatching("/worker/v1/tasks"))
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

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        VitamConfiguration.getConfiguration()
            .setData(PropertiesUtils.getResourcePath("integration-processing/").toString());
        CONFIG_WORKSPACE_PATH = PropertiesUtils.getResourcePath("integration-processing/workspace.conf").toString();
        CONFIG_PROCESSING_PATH = PropertiesUtils.getResourcePath("integration-processing/processing.conf").toString();

        // launch workspace
        SystemPropertyUtil.set(WorkspaceMain.PARAMETER_JETTY_SERVER_PORT,
            Integer.toString(PORT_SERVICE_WORKSPACE));
        workspaceMain = new WorkspaceMain(CONFIG_WORKSPACE_PATH);
        workspaceMain.start();
        SystemPropertyUtil.clear(WorkspaceMain.PARAMETER_JETTY_SERVER_PORT);

        WorkspaceClientFactory.changeMode(WORKSPACE_URL);

        // launch processing
        SystemPropertyUtil.set(ProcessManagementApplication.PARAMETER_JETTY_SERVER_PORT,
            Integer.toString(PORT_SERVICE_PROCESSING));
        processManagementApplication = new ProcessManagementApplication(CONFIG_PROCESSING_PATH);
        processManagementApplication.start();
        SystemPropertyUtil.clear(ProcessManagementApplication.PARAMETER_JETTY_SERVER_PORT);

        ProcessingManagementClientFactory.changeConfigurationUrl(PROCESSING_URL);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        WorkspaceClient workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.deleteContainer("process", true);

        try {
            workspaceMain.stop();
            processManagementApplication.stop();
        } catch (final Exception e) {
            LOGGER.error(e);
        }
    }

    private void waitStep(ProcessWorkflow processWorkflow, int stepId) {

        ProcessStep step = processWorkflow.getSteps().get(stepId);
        while (step.getStepStatusCode() != StatusCode.STARTED) {
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            }
        }
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

    private void waitServerStart() {
        int nbTry = 30;
        while (!checkStatus()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            }
            nbTry--;

            if (nbTry < 0) {
                LOGGER.error("CANNOT CONNECT TO SERVER {}",
                    ProcessingManagementClientFactory.getInstance().getServiceUrl());
                break;
            }
        }
        if (nbTry >= 0) {
            LOGGER.debug("CONNECTED TO SERVER");
        }
    }

    private boolean checkStatus() {
        try {
            processingClient.checkStatus();
            return true;
        } catch (Exception e) {
            LOGGER.error("ProcessManagement server is not active.", e);
            return false;
        }
    }


    @Test
    @RunWithCustomExecutor
    public void whenProcessingServerStopStartThenPauseStartProperlyProcessWorkflow() throws Exception {

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_ID);
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        final GUID objectGuid = GUIDFactory.newManifestGUID(TENANT_ID);
        final String containerName = objectGuid.getId();
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName, WORFKLOW_NAME);

        final File resourceFile = PropertiesUtils.getResourceFile(INGEST_LEVEL_STACK_JSON);

        workspaceClient
            .putObject(containerName, UNITS_LEVEL_STACK_PATH, Files.newInputStream(resourceFile.toPath()));

        ProcessWorkflow processWorkflow =
            ProcessMonitoringImpl.getInstance().findOneProcessWorkflow(containerName, TENANT_ID);

        RequestResponse<JsonNode> resp = processingClient.executeOperationProcess(containerName, WORFKLOW_NAME,
            LogbookTypeProcess.INGEST.toString(), ProcessAction.RESUME.getValue());
        // wait a little bit
        assertThat(resp).isNotNull();
        assertThat(resp.getStatus()).isEqualTo(Response.Status.ACCEPTED.getStatusCode());

        waitStep(processWorkflow, 2);

        // shutdown processing
        processManagementApplication.stop();

        // wait a little bit
        LOGGER.info("After STOP");

        assertThat(processWorkflow.getState()).isEqualTo(ProcessState.PAUSE);
        assertThat(processWorkflow.getPauseRecover()).isEqualTo(PauseRecover.RECOVER_FROM_SERVER_PAUSE);

        DistributorIndex distributorIndex =
            WorkspaceProcessDataManagement.getInstance()
                .getDistributorIndex(ProcessDistributor.DISTRIBUTOR_INDEX, containerName);

        assertThat(distributorIndex).isNotNull();
        assertThat(distributorIndex.getItemStatus()).isNotNull();
        assertThat(distributorIndex.getItemStatus().getItemsStatus()).isNotEmpty();

        // restart processing
        SystemPropertyUtil.set(ProcessManagementApplication.PARAMETER_JETTY_SERVER_PORT,
            Integer.toString(PORT_SERVICE_PROCESSING));
        processManagementApplication = new ProcessManagementApplication(CONFIG_PROCESSING_PATH);
        processManagementApplication.start();
        SystemPropertyUtil.clear(ProcessManagementApplication.PARAMETER_JETTY_SERVER_PORT);

        // wait a little bit until jetty start
        waitServerStart();

        LOGGER.info("After RE-START");

        wait(containerName);

        processWorkflow =
            ProcessMonitoringImpl.getInstance().findOneProcessWorkflow(containerName, TENANT_ID);
        assertThat(processWorkflow).isNotNull();
        assertThat(processWorkflow.getPauseRecover()).isEqualTo(PauseRecover.NO_RECOVER);
        assertThat(processWorkflow.getState()).isEqualTo(ProcessState.COMPLETED);
        assertThat(processWorkflow.getStatus()).isEqualTo(StatusCode.WARNING);
    }
}
