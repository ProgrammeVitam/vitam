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

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.google.common.collect.Sets;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InternalServerException;
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
import fr.gouv.vitam.common.model.processing.PauseOrCancelAction;
import fr.gouv.vitam.common.storage.compress.ArchiveEntryInputStream;
import fr.gouv.vitam.common.storage.compress.VitamArchiveStreamFactory;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.parameters.Contexts;
import fr.gouv.vitam.processing.common.model.DistributorIndex;
import fr.gouv.vitam.processing.common.model.PauseRecover;
import fr.gouv.vitam.processing.common.model.ProcessStep;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.processing.common.model.WorkerBean;
import fr.gouv.vitam.processing.common.model.WorkerRemoteConfiguration;
import fr.gouv.vitam.processing.data.core.management.WorkspaceProcessDataManagement;
import fr.gouv.vitam.processing.engine.core.monitoring.ProcessMonitoringImpl;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageCompressedFileException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static fr.gouv.vitam.common.VitamTestHelper.insertWaitForStepEssentialFiles;
import static fr.gouv.vitam.common.VitamTestHelper.waitOperation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

public class ProperlyStopStartProcessingIT extends VitamRuleRunner {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProperlyStopStartProcessingIT.class);

    private static final int STEP_INDEX = 4;

    @ClassRule
    public static VitamServerRunner runner = new VitamServerRunner(
        ProperlyStopStartProcessingIT.class,
        mongoRule.getMongoDatabase().getName(),
        ElasticsearchRule.getClusterName(),
        Sets.newHashSet(WorkspaceMain.class, ProcessManagementMain.class)
    );

    private static final Integer TENANT_ID = 0;
    private final int[] elementCountPerStep = { 1, 1, 1, 0, 170, 1, 0, 170, 0, 170, 0, 1, 1 };

    public static final String INGEST_LEVEL_STACK_JSON = "integration-processing/ingestLevelStack.json";
    public static final String UNITS_LEVEL_STACK_PATH = "UnitsLevel/ingestLevelStack.json";

    public static final String EXISING_GOT_FILE = "integration-processing/existing_object_group.json";

    public static final String EXISTING_GOT = "UpdateObjectGroup/existing_object_group.json";

    @ClassRule
    public static WireMockClassRule workerMockRule = new WireMockClassRule(options().dynamicPort());

    @Rule
    public WireMockClassRule workerInstance = workerMockRule;

    private WorkspaceClient workspaceClient;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        handleBeforeClass(Arrays.asList(0, 1), Collections.emptyMap());
        // set bulk size to 1 for tests
        VitamConfiguration.setWorkerBulkSize(3);
        VitamConfiguration.setDistributeurBatchSize(10);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        handleAfterClass();
        runAfter();

        VitamConfiguration.setWorkerBulkSize(10);
        VitamConfiguration.setDistributeurBatchSize(100);

        ProcessingManagementClientFactory.getInstance()
            .getClient()
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
        workerInstance.resetAll();

        // Delete eventually existing workerdb file
        final WorkerBean workerBean = new WorkerBean(
            "DefaultWorker",
            "DefaultWorker",
            1,
            "status",
            new WorkerRemoteConfiguration("localhost", workerMockRule.port())
        );
        workerBean.setWorkerId(String.valueOf(ServerIdentity.getInstance().getGlobalPlatformId()));
        // Ensure processing is started
        runner.startProcessManagementServer();
        ProcessingManagementClientFactory.getInstance()
            .getClient()
            .registerWorker(
                "DefaultWorker",
                String.valueOf(ServerIdentity.getInstance().getGlobalPlatformId()),
                workerBean
            );

        workerInstance.stubFor(
            WireMock.get(urlMatching("/worker/v1/status")).willReturn(
                aResponse().withStatus(200).withHeader(GlobalDataRest.X_TENANT_ID, Integer.toString(TENANT_ID))
            )
        );

        workerInstance.stubFor(
            WireMock.post(urlMatching("/worker/v1/tasks")).willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody(JsonHandler.unprettyPrint(getMockedItemStatus(StatusCode.OK)))
                    .withHeader(GlobalDataRest.X_TENANT_ID, Integer.toString(TENANT_ID))
                    .withHeader("Content-Type", "application/json")
            )
        );
    }

    ItemStatus getMockedItemStatus(StatusCode statusCode) {
        return new ItemStatus("StepId").setItemsStatus(
            "ItemId",
            new ItemStatus("ItemId").setMessage("message").increment(statusCode, VitamConfiguration.getWorkerBulkSize())
        );
    }

    private void waitStep(ProcessWorkflow processWorkflow, int stepId) {
        while (processWorkflow.getSteps().get(stepId).getStepStatusCode() == StatusCode.UNKNOWN) {
            try {
                LOGGER.info("== Wait step :" + stepId);
                TimeUnit.MILLISECONDS.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            }
        }
    }

    @Test
    @RunWithCustomExecutor
    public void given_running_operations_when_stop_processing_then_pause_operations_when_start_processing_then_start_paused_operations()
        throws Exception {
        ProcessingIT.prepareVitamSession();
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_ID);
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        final String operationId = operationGuid.toString();
        simulateIngest(operationId);

        ProcessWorkflow processWorkflow = ProcessMonitoringImpl.getInstance()
            .findOneProcessWorkflow(operationId, TENANT_ID);

        RequestResponse<ItemStatus> resp = ProcessingManagementClientFactory.getInstance()
            .getClient()
            .executeOperationProcess(operationId, Contexts.DEFAULT_WORKFLOW.name(), ProcessAction.RESUME.getValue());

        assertThat(resp).isNotNull();
        assertThat(resp.isOk()).isTrue();
        assertThat(resp.getStatus()).isEqualTo(Response.Status.ACCEPTED.getStatusCode());

        // Wait step STP_UNIT_CHECK_AND_PROCESS
        ProcessStep step = processWorkflow.getSteps().get(STEP_INDEX);
        waitStep(processWorkflow, STEP_INDEX);

        // Wait until step STEP_INDEX have Response from workers
        while (
            step.getStepResponses() == null ||
            step.getStepResponses().getStatusMeter().get(StatusCode.OK.ordinal()) == 0
        ) {
            TimeUnit.MILLISECONDS.sleep(1);
        }

        // Shutdown processing
        runner.stopProcessManagementServer(false);
        LOGGER.warn("=== After STOP");

        assertThat(processWorkflow.getState()).isEqualTo(ProcessState.PAUSE);
        assertThat(processWorkflow.getStatus()).isEqualTo(StatusCode.WARNING);
        assertThat(processWorkflow.getPauseRecover()).isEqualTo(PauseRecover.RECOVER_FROM_SERVER_PAUSE);
        Optional<DistributorIndex> distributorIndex = WorkspaceProcessDataManagement.getInstance()
            .getDistributorIndex(operationId);

        assertThat(distributorIndex).isPresent();
        assertThat(distributorIndex.get().getItemStatus()).isNotNull();
        assertThat(
            distributorIndex.get().getItemStatus().getStatusMeter().get(StatusCode.OK.ordinal())
        ).isGreaterThanOrEqualTo(
            VitamConfiguration.getWorkerBulkSize() * VitamConfiguration.getDistributeurBatchSize()
        );

        // restart processing
        runner.startProcessManagementServer();
        LOGGER.warn("After RE-START");

        waitOperation(operationId);

        processWorkflow = ProcessMonitoringImpl.getInstance().findOneProcessWorkflow(operationId, TENANT_ID);
        assertThat(processWorkflow).isNotNull();
        assertThat(processWorkflow.getStatus()).isEqualTo(StatusCode.WARNING);
        assertThat(processWorkflow.getState()).isEqualTo(ProcessState.COMPLETED);

        checkAllSteps(processWorkflow);
    }

    @Test
    @RunWithCustomExecutor
    public void given_pause_operations_when_stop_processing_then_pause_operations_when_start_processing_then_start_paused_operations()
        throws Exception {
        ProcessingIT.prepareVitamSession();
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_ID);
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        final String operationId = operationGuid.toString();
        simulateIngest(operationId);

        ProcessWorkflow processWorkflow = ProcessMonitoringImpl.getInstance()
            .findOneProcessWorkflow(operationId, TENANT_ID);

        RequestResponse<ItemStatus> resp = ProcessingManagementClientFactory.getInstance()
            .getClient()
            .executeOperationProcess(operationId, Contexts.DEFAULT_WORKFLOW.name(), ProcessAction.NEXT.getValue());

        assertThat(resp).isNotNull();
        assertThat(resp.isOk()).isTrue();
        assertThat(resp.getStatus()).isEqualTo(Response.Status.ACCEPTED.getStatusCode());

        // Wait until operation become PAUSE
        waitOperation(operationId, ProcessState.PAUSE);
        // shutdown processing
        runner.stopProcessManagementServer(false);
        LOGGER.warn("=== After STOP");

        assertThat(processWorkflow.getState()).isEqualTo(ProcessState.PAUSE);
        assertThat(processWorkflow.getStatus()).isEqualTo(StatusCode.OK);
        assertThat(processWorkflow.getSteps().get(STEP_INDEX).getStepStatusCode()).isEqualTo(StatusCode.UNKNOWN);

        // restart processing
        runner.startProcessManagementServer();
        LOGGER.warn("After RE-START");

        assertThat(processWorkflow.getState()).isEqualTo(ProcessState.PAUSE);
        assertThat(processWorkflow.getStatus()).isEqualTo(StatusCode.OK);
        assertThat(processWorkflow.getPauseRecover()).isEqualTo(PauseRecover.NO_RECOVER);
        assertThat(processWorkflow.getSteps().get(STEP_INDEX).getStepStatusCode()).isEqualTo(StatusCode.UNKNOWN);

        // Then resume operation
        resp = ProcessingManagementClientFactory.getInstance()
            .getClient()
            .executeOperationProcess(operationId, Contexts.DEFAULT_WORKFLOW.name(), ProcessAction.RESUME.getValue());

        // wait a little bit
        assertThat(resp).isNotNull();
        assertThat(resp.isOk()).isTrue();
        assertThat(resp.getStatus()).isEqualTo(Response.Status.ACCEPTED.getStatusCode());

        // Wait until operation become COMPLETED
        waitOperation(operationId);

        processWorkflow = ProcessMonitoringImpl.getInstance().findOneProcessWorkflow(operationId, TENANT_ID);
        assertThat(processWorkflow).isNotNull();
        assertThat(processWorkflow.getStatus()).isEqualTo(StatusCode.WARNING);
        assertThat(processWorkflow.getState()).isEqualTo(ProcessState.COMPLETED);

        checkAllSteps(processWorkflow);
    }

    @Test
    @RunWithCustomExecutor
    public void given_running_operations_when_cancel_operation_then_restart_processing_then_operation_is_completed_ko()
        throws Exception {
        ProcessingIT.prepareVitamSession();
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_ID);
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        final String operationId = operationGuid.toString();
        simulateIngest(operationId);

        ProcessWorkflow processWorkflow = ProcessMonitoringImpl.getInstance()
            .findOneProcessWorkflow(operationId, TENANT_ID);

        RequestResponse<ItemStatus> resp = ProcessingManagementClientFactory.getInstance()
            .getClient()
            .executeOperationProcess(operationId, Contexts.DEFAULT_WORKFLOW.name(), ProcessAction.RESUME.getValue());

        assertThat(resp).isNotNull();
        assertThat(resp.isOk()).isTrue();
        assertThat(resp.getStatus()).isEqualTo(Response.Status.ACCEPTED.getStatusCode());

        // Wait step STP_UNIT_CHECK_AND_PROCESS
        ProcessStep step = processWorkflow.getSteps().get(STEP_INDEX);
        waitStep(processWorkflow, STEP_INDEX);

        // Wait until step STEP_INDEX have Response from workers
        while (
            step.getStepResponses() == null ||
            step.getStepResponses().getStatusMeter().get(StatusCode.OK.ordinal()) == 0
        ) {
            TimeUnit.MILLISECONDS.sleep(1);
        }

        // Cancel operation
        resp = ProcessingManagementClientFactory.getInstance().getClient().cancelOperationProcessExecution(operationId);
        assertThat(resp).isNotNull();
        assertThat(resp.isOk()).isTrue();
        assertThat(resp.getStatus()).isEqualTo(Response.Status.ACCEPTED.getStatusCode());

        // When stop processing
        LOGGER.warn("=== Before STOP");
        runner.stopProcessManagementServer(false);
        LOGGER.warn("=== After STOP");
        //Then
        // As processing stops, it will force PAUSE on all running operation
        // If current step of running operations is KO, last step, the execution will ends (COMPLETED)
        // So at this point, the state of the ProcessWorkflow is randomly: COMPLETED or PAUSED

        assertThat(processWorkflow.getTargetState()).isEqualTo(ProcessState.COMPLETED);
        assertThat(processWorkflow.getTargetStatus()).isEqualTo(StatusCode.KO);
        assertThat(processWorkflow.getPauseRecover()).isEqualTo(PauseRecover.RECOVER_FROM_SERVER_PAUSE);
        assertThat(step.getPauseOrCancelAction()).isEqualTo(PauseOrCancelAction.ACTION_CANCEL);

        // Restart processing
        runner.startProcessManagementServer();
        LOGGER.warn("After RE-START");

        // Wait in case where the processWorkflow PAUSED By the processing stop server. So in startup of the server the operation will be automatically resumed
        waitOperation(operationId);

        processWorkflow = ProcessMonitoringImpl.getInstance().findOneProcessWorkflow(operationId, TENANT_ID);
        assertThat(processWorkflow).isNotNull();
        assertThat(processWorkflow.getStatus()).isEqualTo(StatusCode.KO);
        assertThat(processWorkflow.getState()).isEqualTo(ProcessState.COMPLETED);

        // Assert that the execution is breaking down and jumped to the final step
        assertThat(processWorkflow.getSteps().get(5).getStepStatusCode()).isEqualTo(StatusCode.UNKNOWN);
        // Assert that the final step is executed
        assertThat(processWorkflow.getSteps().get(processWorkflow.getSteps().size() - 1).getStepStatusCode()).isEqualTo(
            StatusCode.OK
        );
    }

    @Test
    @RunWithCustomExecutor
    public void simulate_crash_test_on_step_index_before_distributor_complete_distribution() throws Exception {
        // We simulate case where Processing crash in the middle of distribution
        runner.stopProcessManagementServer(false);

        ProcessingIT.prepareVitamSession();
        final String operationId = "aeeaaaaaacbfmfhuaa64malrmquyediaaaaq";
        VitamThreadUtils.getVitamSession().setRequestId(operationId);

        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(operationId);

        String vitamDataFolder = VitamConfiguration.getVitamDataFolder();
        Files.delete(Paths.get(vitamDataFolder + "/storage/" + operationId));

        extractArchiveInputStreamOnContainer(
            vitamDataFolder + "/storage",
            PropertiesUtils.getResourceAsStream("integration-processing/simulate_processing_crash/case_2.zip")
        );

        LOGGER.error("=== After START");
        runner.startProcessManagementServer();

        // Wait until operation become PAUSE
        waitOperation(operationId, ProcessState.PAUSE);

        ProcessWorkflow processWorkflow = ProcessMonitoringImpl.getInstance()
            .findOneProcessWorkflow(operationId, TENANT_ID);

        assertThat(processWorkflow).isNotNull();
        assertThat(processWorkflow.getStatus()).isEqualTo(StatusCode.OK);
        assertThat(processWorkflow.getState()).isEqualTo(ProcessState.PAUSE);

        ProcessStep processStep = processWorkflow.getSteps().get(STEP_INDEX);

        assertThat(processStep.getElementToProcess().get()).isEqualTo(processStep.getElementProcessed().get());
        assertThat(processStep.getStepStatusCode()).isEqualTo(StatusCode.OK);
        assertThat(processStep.getPauseOrCancelAction()).isEqualTo(PauseOrCancelAction.ACTION_COMPLETE);

        processStep = processWorkflow.getSteps().get(STEP_INDEX + 1);
        assertThat(processStep.getStepStatusCode()).isEqualTo(StatusCode.UNKNOWN);

        // resume
        RequestResponse<ItemStatus> resp = ProcessingManagementClientFactory.getInstance()
            .getClient()
            .executeOperationProcess(operationId, Contexts.DEFAULT_WORKFLOW.name(), ProcessAction.RESUME.getValue());

        assertThat(resp).isNotNull();
        assertThat(resp.isOk()).isTrue();
        assertThat(resp.getStatus()).isEqualTo(Response.Status.ACCEPTED.getStatusCode());

        waitOperation(operationId);

        assertThat(processWorkflow).isNotNull();
        assertThat(processWorkflow.getStatus()).isEqualTo(StatusCode.OK);
        assertThat(processWorkflow.getState()).isEqualTo(ProcessState.COMPLETED);

        // Assert that all steps are executed
        processWorkflow.getSteps().forEach(step -> assertThat(step.getStepStatusCode()).isEqualTo(StatusCode.OK));
    }

    @Test
    @RunWithCustomExecutor
    public void simulate_crash_test_on_step_index_when_step_action_pause_before_distributor_complete_distribution()
        throws Exception {
        // We simulate case where Processing crash in the middle of distribution
        runner.stopProcessManagementServer(false);

        ProcessingIT.prepareVitamSession();
        final String operationId = "aeeaaaaaacbfmfhuaa64malrmquyediaaaaq";
        VitamThreadUtils.getVitamSession().setRequestId(operationId);

        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(operationId);

        String vitamDataFolder = VitamConfiguration.getVitamDataFolder();
        Files.delete(Paths.get(vitamDataFolder + "/storage/" + operationId));

        extractArchiveInputStreamOnContainer(
            vitamDataFolder + "/storage",
            PropertiesUtils.getResourceAsStream(
                "integration-processing/simulate_processing_crash/case_action_pause.zip"
            )
        );

        LOGGER.error("=== After START");
        runner.startProcessManagementServer();

        // Wait until operation become PAUSE
        waitOperation(operationId, ProcessState.PAUSE);

        ProcessWorkflow processWorkflow = ProcessMonitoringImpl.getInstance()
            .findOneProcessWorkflow(operationId, TENANT_ID);

        assertThat(processWorkflow).isNotNull();
        assertThat(processWorkflow.getStatus()).isEqualTo(StatusCode.OK);
        assertThat(processWorkflow.getState()).isEqualTo(ProcessState.PAUSE);

        // as step STEP_INDEX is action pause StateMachine initialize the index step to this step STEP_INDEX
        ProcessStep processStep = processWorkflow.getSteps().get(STEP_INDEX);

        assertThat(processStep.getElementToProcess().get()).isEqualTo(processStep.getElementProcessed().get());
        assertThat(processStep.getStepStatusCode()).isEqualTo(StatusCode.OK);
        assertThat(processStep.getPauseOrCancelAction()).isEqualTo(PauseOrCancelAction.ACTION_COMPLETE);

        processStep = processWorkflow.getSteps().get(STEP_INDEX + 1);
        assertThat(processStep.getStepStatusCode()).isEqualTo(StatusCode.UNKNOWN);

        // resume
        RequestResponse<ItemStatus> resp = ProcessingManagementClientFactory.getInstance()
            .getClient()
            .executeOperationProcess(operationId, Contexts.DEFAULT_WORKFLOW.name(), ProcessAction.RESUME.getValue());

        assertThat(resp).isNotNull();
        assertThat(resp.isOk()).isTrue();
        assertThat(resp.getStatus()).isEqualTo(Response.Status.ACCEPTED.getStatusCode());

        waitOperation(operationId);

        assertThat(processWorkflow).isNotNull();
        assertThat(processWorkflow.getStatus()).isEqualTo(StatusCode.OK);
        assertThat(processWorkflow.getState()).isEqualTo(ProcessState.COMPLETED);

        // Assert that all steps are executed
        processWorkflow.getSteps().forEach(step -> assertThat(step.getStepStatusCode()).isEqualTo(StatusCode.OK));
    }

    @Test
    @RunWithCustomExecutor
    public void simulate_crash_test_on_step_index_when_step_action_cancel_before_distributor_complete_distribution()
        throws Exception {
        // We simulate case where Processing crash in the middle of distribution
        runner.stopProcessManagementServer(false);

        ProcessingIT.prepareVitamSession();
        final String operationId = "aeeaaaaaacbfmfhuaa64malrmquyediaaaaq";
        VitamThreadUtils.getVitamSession().setRequestId(operationId);

        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(operationId);

        String vitamDataFolder = VitamConfiguration.getVitamDataFolder();
        Files.delete(Paths.get(vitamDataFolder + "/storage/" + operationId));

        extractArchiveInputStreamOnContainer(
            vitamDataFolder + "/storage",
            PropertiesUtils.getResourceAsStream(
                "integration-processing/simulate_processing_crash/case_action_cancel.zip"
            )
        );

        LOGGER.error("=== After START");
        runner.startProcessManagementServer();

        waitOperation(operationId);

        ProcessWorkflow processWorkflow = ProcessMonitoringImpl.getInstance()
            .findOneProcessWorkflow(operationId, TENANT_ID);

        assertThat(processWorkflow).isNotNull();
        assertThat(processWorkflow.getStatus()).isEqualTo(StatusCode.KO);
        assertThat(processWorkflow.getState()).isEqualTo(ProcessState.COMPLETED);

        // as step STEP_INDEX is action complete and status ok when processing start, it execute the next step
        ProcessStep processStep = processWorkflow.getSteps().get(STEP_INDEX);

        assertThat(processStep.getElementProcessed().get()).isEqualTo(3);
        assertThat(processStep.getStepStatusCode()).isEqualTo(StatusCode.STARTED);
        assertThat(processStep.getPauseOrCancelAction()).isEqualTo(PauseOrCancelAction.ACTION_CANCEL);
    }

    @Test
    @RunWithCustomExecutor
    public void simulate_crash_test_on_step_index_when_step_action_complete() throws Exception {
        // We simulate case where Processing crash in the middle of distribution
        runner.stopProcessManagementServer(false);

        ProcessingIT.prepareVitamSession();
        final String operationId = "aeeaaaaaacbfmfhuaa64malrmquyediaaaaq";
        VitamThreadUtils.getVitamSession().setRequestId(operationId);

        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(operationId);

        String vitamDataFolder = VitamConfiguration.getVitamDataFolder();
        Files.delete(Paths.get(vitamDataFolder + "/storage/" + operationId));

        extractArchiveInputStreamOnContainer(
            vitamDataFolder + "/storage",
            PropertiesUtils.getResourceAsStream("integration-processing/simulate_processing_crash/case_1.zip")
        );

        LOGGER.error("=== After START");
        runner.startProcessManagementServer();

        // Wait until operation become PAUSE
        waitOperation(operationId, ProcessState.PAUSE);

        ProcessWorkflow processWorkflow = ProcessMonitoringImpl.getInstance()
            .findOneProcessWorkflow(operationId, TENANT_ID);

        assertThat(processWorkflow).isNotNull();
        assertThat(processWorkflow.getStatus()).isEqualTo(StatusCode.OK);
        assertThat(processWorkflow.getState()).isEqualTo(ProcessState.PAUSE);

        // as step STEP_INDEX is action complete and status ok when processing start, it execute the next step
        ProcessStep processStep = processWorkflow.getSteps().get(STEP_INDEX + 1);

        assertThat(processStep.getElementToProcess().get()).isEqualTo(processStep.getElementProcessed().get());
        assertThat(processStep.getStepStatusCode()).isEqualTo(StatusCode.OK);
        assertThat(processStep.getPauseOrCancelAction()).isEqualTo(PauseOrCancelAction.ACTION_COMPLETE);

        // resume
        RequestResponse<ItemStatus> resp = ProcessingManagementClientFactory.getInstance()
            .getClient()
            .executeOperationProcess(operationId, Contexts.DEFAULT_WORKFLOW.name(), ProcessAction.RESUME.getValue());

        assertThat(resp).isNotNull();
        assertThat(resp.isOk()).isTrue();
        assertThat(resp.getStatus()).isEqualTo(Response.Status.ACCEPTED.getStatusCode());

        waitOperation(operationId);

        assertThat(processWorkflow).isNotNull();
        assertThat(processWorkflow.getStatus()).isEqualTo(StatusCode.OK);
        assertThat(processWorkflow.getState()).isEqualTo(ProcessState.COMPLETED);

        // Assert that all steps are executed
        processWorkflow.getSteps().forEach(step -> assertThat(step.getStepStatusCode()).isEqualTo(StatusCode.OK));
    }

    @Test
    @RunWithCustomExecutor
    public void simulate_crash_test_on_all_complete_steps_but_process_in_pause_fatal() throws Exception {
        // We simulate case where Processing crash in the middle of distribution
        runner.stopProcessManagementServer(false);

        ProcessingIT.prepareVitamSession();
        final String operationId = "aecaaaaaa6haol6vaacfyalrvlc4qbiaaaaq";
        VitamThreadUtils.getVitamSession().setRequestId(operationId);

        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(operationId);

        String vitamDataFolder = VitamConfiguration.getVitamDataFolder();
        Files.delete(Paths.get(vitamDataFolder + "/storage/" + operationId));

        extractArchiveInputStreamOnContainer(
            vitamDataFolder + "/storage",
            PropertiesUtils.getResourceAsStream(
                "integration-processing/simulate_processing_crash/case-traceability-all-step-complete-but-pause-fatal.zip"
            )
        );

        LOGGER.error("=== After START");
        runner.startProcessManagementServer();

        // Wait until operation become PAUSE
        waitOperation(operationId, ProcessState.PAUSE);

        ProcessWorkflow processWorkflow = ProcessMonitoringImpl.getInstance()
            .findOneProcessWorkflow(operationId, TENANT_ID);

        assertThat(processWorkflow).isNotNull();
        assertThat(processWorkflow.getStatus()).isEqualTo(StatusCode.FATAL);
        assertThat(processWorkflow.getState()).isEqualTo(ProcessState.PAUSE);

        // resume
        RequestResponse<ItemStatus> resp = ProcessingManagementClientFactory.getInstance()
            .getClient()
            .executeOperationProcess(
                operationId,
                Contexts.UNIT_LFC_TRACEABILITY.name(),
                ProcessAction.RESUME.getValue()
            );

        assertThat(resp).isNotNull();
        assertThat(resp.isOk()).isTrue();
        assertThat(resp.getStatus()).isEqualTo(Response.Status.ACCEPTED.getStatusCode());

        waitOperation(operationId);

        assertThat(processWorkflow).isNotNull();
        assertThat(processWorkflow.getStatus()).isEqualTo(StatusCode.WARNING);
        assertThat(processWorkflow.getState()).isEqualTo(ProcessState.COMPLETED);

        // Assert that all steps are executed
        ProcessStep firstStep = processWorkflow.getSteps().get(0);
        ProcessStep lastStep = processWorkflow.getSteps().get(1);
        assertThat(firstStep.getElementProcessed().get()).isEqualTo(1);

        assertThat(firstStep.getPauseOrCancelAction()).isEqualTo(PauseOrCancelAction.ACTION_COMPLETE);
        assertThat(lastStep.getPauseOrCancelAction()).isEqualTo(PauseOrCancelAction.ACTION_COMPLETE);

        assertThat(firstStep.getStepStatusCode()).isEqualTo(StatusCode.WARNING);
        assertThat(lastStep.getStepStatusCode()).isEqualTo(StatusCode.OK);
    }

    @Test
    @RunWithCustomExecutor
    public void simulate_crash_test_on_complete_step_with_status_fatal() throws Exception {
        // We simulate case where Processing crash in the middle of distribution
        runner.stopProcessManagementServer(false);

        ProcessingIT.prepareVitamSession();
        VitamThreadUtils.getVitamSession().setTenantId(1);
        final String operationId = "aecaaaaaaghaol6vaacfyalrvlc4elaaaaaq";
        VitamThreadUtils.getVitamSession().setRequestId(operationId);

        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(operationId);

        String vitamDataFolder = VitamConfiguration.getVitamDataFolder();
        Files.delete(Paths.get(vitamDataFolder + "/storage/" + operationId));

        extractArchiveInputStreamOnContainer(
            vitamDataFolder + "/storage",
            PropertiesUtils.getResourceAsStream(
                "integration-processing/simulate_processing_crash/case-traceability-step-complete-status-fatal.zip"
            )
        );

        LOGGER.error("=== After START");
        runner.startProcessManagementServer();

        // Wait until operation become PAUSE
        waitOperation(operationId, ProcessState.PAUSE);

        ProcessWorkflow processWorkflow = ProcessMonitoringImpl.getInstance().findOneProcessWorkflow(operationId, 1);

        assertThat(processWorkflow).isNotNull();
        assertThat(processWorkflow.getStatus()).isEqualTo(StatusCode.FATAL);
        assertThat(processWorkflow.getState()).isEqualTo(ProcessState.PAUSE);

        // resume
        RequestResponse<ItemStatus> resp = ProcessingManagementClientFactory.getInstance()
            .getClient()
            .executeOperationProcess(
                operationId,
                Contexts.UNIT_LFC_TRACEABILITY.name(),
                ProcessAction.RESUME.getValue()
            );

        assertThat(resp).isNotNull();
        assertThat(resp.isOk()).isTrue();
        assertThat(resp.getStatus()).isEqualTo(Response.Status.ACCEPTED.getStatusCode());

        waitOperation(operationId);

        assertThat(processWorkflow).isNotNull();
        assertThat(processWorkflow.getStatus()).isEqualTo(StatusCode.OK);
        assertThat(processWorkflow.getState()).isEqualTo(ProcessState.COMPLETED);

        // Assert that all steps are executed
        ProcessStep firstStep = processWorkflow.getSteps().get(0);
        ProcessStep lastStep = processWorkflow.getSteps().get(1);
        assertThat(firstStep.getElementProcessed().get()).isEqualTo(1);

        assertThat(firstStep.getPauseOrCancelAction()).isEqualTo(PauseOrCancelAction.ACTION_COMPLETE);
        assertThat(lastStep.getPauseOrCancelAction()).isEqualTo(PauseOrCancelAction.ACTION_COMPLETE);

        assertThat(firstStep.getStepStatusCode()).isEqualTo(StatusCode.OK);
        assertThat(lastStep.getStepStatusCode()).isEqualTo(StatusCode.OK);
    }

    private void extractArchiveInputStreamOnContainer(final String destination, final InputStream inputStreamObject)
        throws ContentAddressableStorageException {
        try (
            final InputStream inputStreamClosable = StreamUtils.getRemainingReadOnCloseInputStream(inputStreamObject);
            final ArchiveInputStream archiveInputStream = new VitamArchiveStreamFactory()
                .createArchiveInputStream(CommonMediaType.ZIP_TYPE, inputStreamClosable)
        ) {
            ArchiveEntry entry;
            boolean isEmpty = true;
            // create entryInputStream to resolve the stream closed problem
            final ArchiveEntryInputStream entryInputStream = new ArchiveEntryInputStream(archiveInputStream);

            Path folderPath = Paths.get(destination);

            while ((entry = archiveInputStream.getNextEntry()) != null) {
                if (archiveInputStream.canReadEntryData(entry)) {
                    isEmpty = false;

                    final String entryName = entry.getName();

                    final Path target = Paths.get(folderPath.toString(), entryName);
                    final Path parent = target.getParent();

                    if (parent != null && !parent.toFile().exists()) {
                        Files.createDirectories(parent);
                    }
                    if (!entry.isDirectory()) {
                        Files.copy(entryInputStream, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
                entryInputStream.setClosed(false);
            }
            if (isEmpty) {
                throw new ContentAddressableStorageCompressedFileException("File is empty");
            }
        } catch (final IOException | ArchiveException e) {
            LOGGER.error(e);
            throw new ContentAddressableStorageException(e);
        }
    }

    private void simulateIngest(String containerName)
        throws ContentAddressableStorageServerException, BadRequestException, InternalServerException, IOException {
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        ProcessingManagementClientFactory.getInstance()
            .getClient()
            .initVitamProcess(containerName, Contexts.DEFAULT_WORKFLOW.name());

        final File resourceFile = PropertiesUtils.getResourceFile(INGEST_LEVEL_STACK_JSON);
        workspaceClient.putObject(containerName, UNITS_LEVEL_STACK_PATH, Files.newInputStream(resourceFile.toPath()));

        final File existing_got = PropertiesUtils.getResourceFile(EXISING_GOT_FILE);
        workspaceClient.putObject(containerName, EXISTING_GOT, Files.newInputStream(existing_got.toPath()));

        // Insert sanityCheck file & StpUpload
        insertWaitForStepEssentialFiles(containerName);
    }

    private void checkAllSteps(ProcessWorkflow processWorkflow) {
        int stepIndex = 0;
        for (ProcessStep step : processWorkflow.getSteps()) {
            // check status
            assertTrue(
                step.getStepStatusCode().equals(StatusCode.OK) || step.getStepStatusCode().equals(StatusCode.WARNING)
            );

            // check processed elements
            Assertions.assertThat(step.getElementProcessed().get())
                .as("Compare elementProcessed with elementToProcess > step :" + step.getStepName())
                .isEqualTo(step.getElementToProcess().get());
            Assertions.assertThat(step.getElementProcessed().get())
                .as("Compare elementProcessed with expected number > step :" + step.getStepName())
                .isEqualTo(elementCountPerStep[stepIndex++]);
        }
    }
}
