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
package fr.gouv.vitam.processing.management.core;

import com.google.common.collect.Lists;
import fr.gouv.vitam.common.exception.StateNotAllowedException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.PauseOrCancelAction;
import fr.gouv.vitam.common.model.processing.ProcessBehavior;
import fr.gouv.vitam.common.model.processing.Step;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.parameters.Contexts;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.processing.common.exception.ProcessingEngineException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.ProcessStep;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.processing.data.core.ProcessDataAccessImpl;
import fr.gouv.vitam.processing.data.core.management.ProcessDataManagement;
import fr.gouv.vitam.processing.distributor.api.ProcessDistributor;
import fr.gouv.vitam.processing.distributor.core.ProcessDistributorImpl;
import fr.gouv.vitam.processing.engine.api.ProcessEngine;
import fr.gouv.vitam.processing.engine.core.ProcessEngineFactory;
import fr.gouv.vitam.processing.engine.core.ProcessEngineImpl;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class StateMachineTest {

    private static final Integer TENANT_ID = 0;
    private WorkerParameters workParams;

    private ProcessDataAccessImpl processDataAccess;

    private static final String WORKFLOW_FILE = "workflowJSONv1.json";
    private static final String WORKFLOW_FINALLY_STEP_FILE = "workflowJSONFinallyStep.json";

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    private static final WorkspaceClientFactory workspaceClientFactory = mock(WorkspaceClientFactory.class);
    private static final ProcessDataManagement dataManagement = mock(ProcessDataManagement.class);

    @Before
    public void setup() {
        when(workspaceClientFactory.getClient()).thenReturn(mock(WorkspaceClient.class));
        workParams = WorkerParametersFactory.newWorkerParameters();
        workParams
            .setWorkerGUID(GUIDFactory.newGUID().getId())
            .setUrlMetadata("http://localhost:8083")
            .setUrlWorkspace("http://localhost:8083")
            .setContainerName(GUIDFactory.newGUID().getId())
            .setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_ID).toString())
            .setWorkflowIdentifier(Contexts.DEFAULT_WORKFLOW.name())
            .setLogbookTypeProcess(Contexts.DEFAULT_WORKFLOW.getLogbookTypeProcess());

        processDataAccess = ProcessDataAccessImpl.getInstance();
    }

    @Test
    @RunWithCustomExecutor
    public void test_all_actions_on_pause_state() throws StateNotAllowedException, ProcessingException {
        StateMachine stateMachine = mock(StateMachine.class);
        ProcessState state = ProcessState.PAUSE;
        doAnswer(o -> {
            state.eval(ProcessState.RUNNING);
            return null;
        })
            .when(stateMachine)
            .next(any());
        stateMachine.next(null);

        doAnswer(o -> {
            state.eval(ProcessState.RUNNING);
            return null;
        })
            .when(stateMachine)
            .resume(any());
        stateMachine.resume(null);

        doAnswer(o -> {
            state.eval(ProcessState.PAUSE);
            return null;
        })
            .when(stateMachine)
            .pause();
        stateMachine.pause();

        doAnswer(o -> {
            state.eval(ProcessState.COMPLETED);
            return null;
        })
            .when(stateMachine)
            .cancel();
        stateMachine.cancel();
    }

    @Test
    @RunWithCustomExecutor
    public void test_pause_and_cancel_on_running_state_ok() throws StateNotAllowedException {
        StateMachine stateMachine = mock(StateMachine.class);
        ProcessState state = ProcessState.RUNNING;
        doAnswer(o -> {
            state.eval(ProcessState.PAUSE);
            return null;
        })
            .when(stateMachine)
            .pause();
        stateMachine.pause();

        doAnswer(o -> {
            state.eval(ProcessState.COMPLETED);
            return null;
        })
            .when(stateMachine)
            .cancel();
        stateMachine.cancel();
    }

    @Test(expected = StateNotAllowedException.class)
    @RunWithCustomExecutor
    public void test_next_on_running_state_ko() throws StateNotAllowedException, ProcessingException {
        StateMachine stateMachine = mock(StateMachine.class);
        ProcessState state = ProcessState.RUNNING;
        doAnswer(o -> {
            state.eval(ProcessState.RUNNING);
            return null;
        })
            .when(stateMachine)
            .next(any());

        stateMachine.next(null);
    }

    @Test(expected = StateNotAllowedException.class)
    @RunWithCustomExecutor
    public void test_resume_on_running_state_ko() throws StateNotAllowedException, ProcessingException {
        StateMachine stateMachine = mock(StateMachine.class);
        ProcessState state = ProcessState.RUNNING;
        doAnswer(o -> {
            state.eval(ProcessState.RUNNING);
            return null;
        })
            .when(stateMachine)
            .resume(any());

        stateMachine.resume(null);
    }

    @Test(expected = StateNotAllowedException.class)
    @RunWithCustomExecutor
    public void test_next_on_completed_state_ko() throws StateNotAllowedException, ProcessingException {
        StateMachine stateMachine = mock(StateMachine.class);
        ProcessState state = ProcessState.COMPLETED;
        doAnswer(o -> {
            state.eval(ProcessState.RUNNING);
            return null;
        })
            .when(stateMachine)
            .next(any());

        stateMachine.next(null);
    }

    @Test(expected = StateNotAllowedException.class)
    @RunWithCustomExecutor
    public void test_resume_on_completed_state_ko() throws StateNotAllowedException, ProcessingException {
        StateMachine stateMachine = mock(StateMachine.class);
        ProcessState state = ProcessState.COMPLETED;
        doAnswer(o -> {
            state.eval(ProcessState.RUNNING);
            return null;
        })
            .when(stateMachine)
            .resume(any());

        stateMachine.resume(null);
    }

    @Test(expected = StateNotAllowedException.class)
    @RunWithCustomExecutor
    public void test_pause_on_completed_state_ko() throws StateNotAllowedException {
        StateMachine stateMachine = mock(StateMachine.class);
        ProcessState state = ProcessState.COMPLETED;
        doAnswer(o -> {
            state.eval(ProcessState.PAUSE);
            return null;
        })
            .when(stateMachine)
            .pause();

        stateMachine.pause();
    }

    @Test(expected = StateNotAllowedException.class)
    @RunWithCustomExecutor
    public void test_cancel_on_completed_state_ko() throws StateNotAllowedException {
        StateMachine stateMachine = mock(StateMachine.class);
        ProcessState state = ProcessState.COMPLETED;
        doAnswer(o -> {
            state.eval(ProcessState.COMPLETED);
            return null;
        })
            .when(stateMachine)
            .cancel();

        stateMachine.cancel();
    }

    @Test
    @RunWithCustomExecutor
    public void test_all_actions_on_running_state_ok()
        throws ProcessingException, StateNotAllowedException, ProcessingEngineException {
        VitamThreadUtils.getVitamSession().setTenantId(1);

        final ProcessWorkflow processWorkflow = processDataAccess.initProcessWorkflow(
            ProcessPopulator.populate(WORKFLOW_FILE).get(),
            workParams.getContainerName()
        );

        final ProcessEngine processEngine = mock(ProcessEngineImpl.class);
        final StateMachine stateMachine = StateMachineFactory.get()
            .create(processWorkflow, processEngine, dataManagement, workspaceClientFactory);

        doAnswer(invocation -> null).when(processEngine).start(any(), any());
        stateMachine.next(workParams);
        try {
            stateMachine.next(workParams);
            fail("Should throw excetpion");
        } catch (StateNotAllowedException e) {}
        try {
            stateMachine.resume(workParams);
            fail("Should throw excetpion");
        } catch (StateNotAllowedException e) {}
        try {
            stateMachine.cancel();
        } catch (StateNotAllowedException e) {
            fail("Should not throw excetpion");
        }
        try {
            stateMachine.pause();
        } catch (StateNotAllowedException e) {
            fail("Should not throw excetpion");
        }
        processDataAccess.clearWorkflow();
    }

    /**
     * Test onComplete
     *
     * @throws ProcessingException
     * @throws StateNotAllowedException
     * @throws ProcessingEngineException
     */
    @Test
    @RunWithCustomExecutor
    public void testWhenProcessEngineOnCompleteOK() throws ProcessingException, StateNotAllowedException {
        VitamThreadUtils.getVitamSession().setTenantId(1);

        final ProcessWorkflow processWorkflow = processDataAccess.initProcessWorkflow(
            ProcessPopulator.populate(WORKFLOW_FILE).get(),
            workParams.getContainerName()
        );

        workParams.setLogbookTypeProcess(LogbookTypeProcess.INGEST);

        final ProcessDistributor processDistributorMock = mock(ProcessDistributorImpl.class);
        final ProcessEngineImpl processEngine = ProcessEngineFactoryTest.get()
            .create(workParams, processDistributorMock);
        StateMachine stateMachine = StateMachineFactory.get()
            .create(processWorkflow, processEngine, dataManagement, workspaceClientFactory);
        processEngine.setStateMachineCallback(stateMachine);

        ProcessStep firstStep = processWorkflow.getSteps().iterator().next();
        ItemStatus itemStatus = new ItemStatus(firstStep.getStepName()).increment(StatusCode.OK);
        when(processDistributorMock.distribute(any(), any(), any())).thenReturn(itemStatus);
        stateMachine.resume(workParams);
        waitProcessToFinish(processWorkflow, (ProcessEngineTest) processEngine);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(StatusCode.OK, processWorkflow.getStatus());

        try {
            stateMachine.next(workParams);
            fail("Should throw an exception");
        } catch (StateNotAllowedException e) {}

        processDataAccess.clearWorkflow();
    }

    /**
     * Test onComplete
     *
     * @throws ProcessingException
     * @throws StateNotAllowedException
     * @throws ProcessingEngineException
     */
    @Test
    @RunWithCustomExecutor
    public void testWhenProcessEngineOnCompleteKO() throws ProcessingException, StateNotAllowedException {
        VitamThreadUtils.getVitamSession().setTenantId(1);

        final ProcessWorkflow processWorkflow = processDataAccess.initProcessWorkflow(
            ProcessPopulator.populate(WORKFLOW_FILE).get(),
            workParams.getContainerName()
        );

        workParams.setLogbookTypeProcess(LogbookTypeProcess.INGEST);

        final ProcessDistributor processDistributorMock = mock(ProcessDistributorImpl.class);
        final ProcessEngineImpl processEngine = ProcessEngineFactoryTest.get()
            .create(workParams, processDistributorMock);
        StateMachine stateMachine = StateMachineFactory.get()
            .create(processWorkflow, processEngine, dataManagement, workspaceClientFactory);
        processEngine.setStateMachineCallback(stateMachine);

        ProcessStep firstStep = processWorkflow.getSteps().iterator().next();
        ItemStatus itemStatus = new ItemStatus(firstStep.getStepName()).increment(StatusCode.KO);
        when(processDistributorMock.distribute(any(), any(), any())).thenReturn(itemStatus);
        stateMachine.resume(workParams);
        waitProcessToFinish(processWorkflow, (ProcessEngineTest) processEngine);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(StatusCode.KO, processWorkflow.getStatus());

        try {
            stateMachine.next(workParams);
            fail("Should throw an exception");
        } catch (StateNotAllowedException e) {}

        processDataAccess.clearWorkflow();
    }

    /**
     * Test onError
     *
     * @throws ProcessingException
     * @throws StateNotAllowedException
     * @throws ProcessingEngineException
     */
    @Test
    @RunWithCustomExecutor
    public void testWhenProcessEngineOnErrorFATAL() {
        VitamThreadUtils.getVitamSession().setTenantId(1);

        final ProcessWorkflow processWorkflow = processDataAccess.initProcessWorkflow(
            ProcessPopulator.populate(WORKFLOW_FILE).get(),
            workParams.getContainerName()
        );

        workParams.setLogbookTypeProcess(LogbookTypeProcess.INGEST);

        final ProcessDistributor processDistributorMock = mock(ProcessDistributorImpl.class);
        final ProcessEngineImpl processEngine = ProcessEngineFactoryTest.get()
            .create(workParams, processDistributorMock);
        StateMachine stateMachine = StateMachineFactory.get()
            .create(processWorkflow, processEngine, dataManagement, workspaceClientFactory);
        processEngine.setStateMachineCallback(stateMachine);

        when(processDistributorMock.distribute(any(), any(), any())).thenThrow(
            new RuntimeException("Fake Exception From Distributor")
        );

        try {
            stateMachine.resume(workParams);
        } catch (StateNotAllowedException | ProcessingException e) {
            fail("Should throw FakeException");
        }
        try {
            waitProcessToFinish(processWorkflow, (ProcessEngineTest) processEngine);
        } catch (RuntimeException e) {}
        assertEquals(ProcessState.PAUSE, processWorkflow.getState());
        assertEquals(StatusCode.FATAL, processWorkflow.getStatus());

        processDataAccess.clearWorkflow();
    }

    @Test
    @RunWithCustomExecutor
    public void testWhenExceptionOccursThenDoNotExecuteFinalStep() {
        VitamThreadUtils.getVitamSession().setTenantId(1);

        final ProcessWorkflow processWorkflow = processDataAccess.initProcessWorkflow(
            ProcessPopulator.populate(WORKFLOW_FINALLY_STEP_FILE).get(),
            workParams.getContainerName()
        );

        workParams.setLogbookTypeProcess(LogbookTypeProcess.INGEST);

        final ProcessDistributor distributorMock = mock(ProcessDistributorImpl.class);
        final ProcessEngineImpl processEngine = ProcessEngineFactoryTest.get().create(workParams, distributorMock);
        StateMachine stateMachine = StateMachineFactory.get()
            .create(processWorkflow, processEngine, dataManagement, workspaceClientFactory);
        processEngine.setStateMachineCallback(stateMachine);

        final ProcessStep firstStep = processWorkflow.getSteps().get(0);
        final ProcessStep lastStep = processWorkflow.getSteps().get(1);

        // First Step FATAL call onError
        when(distributorMock.distribute(workParams, firstStep, workParams.getContainerName())).thenThrow(
            new RuntimeException("Fake Exception From Distributor")
        );

        // Final Step OK call onComplete
        when(distributorMock.distribute(workParams, lastStep, workParams.getContainerName())).thenReturn(
            new ItemStatus(lastStep.getStepName()).increment(StatusCode.OK)
        );
        try {
            try {
                stateMachine.resume(workParams);
            } catch (StateNotAllowedException | ProcessingException e) {
                fail("Should throw FakeException");
            }
            waitProcessToFinish(processWorkflow, (ProcessEngineTest) processEngine);
        } catch (CompletionException e) {}
        assertEquals(ProcessState.PAUSE, processWorkflow.getState());
        assertEquals(StatusCode.FATAL, processWorkflow.getStatus());

        assertEquals(StatusCode.STARTED, firstStep.getStepStatusCode());
        assertEquals(StatusCode.UNKNOWN, lastStep.getStepStatusCode());

        processDataAccess.clearWorkflow();
    }

    @Test
    @RunWithCustomExecutor
    public void testWhenStepKOBlockingThenExecuteFinalStep() throws ProcessingException, StateNotAllowedException {
        VitamThreadUtils.getVitamSession().setTenantId(1);

        final ProcessWorkflow processWorkflow = processDataAccess.initProcessWorkflow(
            ProcessPopulator.populate(WORKFLOW_FINALLY_STEP_FILE).get(),
            workParams.getContainerName()
        );

        workParams.setLogbookTypeProcess(LogbookTypeProcess.INGEST);

        final ProcessDistributor distributorMock = mock(ProcessDistributorImpl.class);
        final ProcessEngineImpl processEngine = ProcessEngineFactoryTest.get().create(workParams, distributorMock);
        StateMachine stateMachine = StateMachineFactory.get()
            .create(processWorkflow, processEngine, dataManagement, workspaceClientFactory);

        processEngine.setStateMachineCallback(stateMachine);

        final ProcessStep firstStep = processWorkflow.getSteps().get(0);
        final ProcessStep lastStep = processWorkflow.getSteps().get(1);

        // First Step KO blocking call onComplete
        when(distributorMock.distribute(workParams, firstStep, workParams.getContainerName())).thenReturn(
            new ItemStatus(firstStep.getStepName()).increment(StatusCode.KO)
        );

        // Final Step OK call onComplete
        when(distributorMock.distribute(workParams, lastStep, workParams.getContainerName())).thenReturn(
            new ItemStatus(lastStep.getStepName()).increment(StatusCode.OK)
        );

        stateMachine.resume(workParams);

        waitProcessToFinish(processWorkflow, (ProcessEngineTest) processEngine);

        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(StatusCode.KO, processWorkflow.getStatus());

        assertEquals(StatusCode.KO, firstStep.getStepStatusCode());
        assertEquals(StatusCode.OK, lastStep.getStepStatusCode());

        processDataAccess.clearWorkflow();
    }

    @Test
    @RunWithCustomExecutor
    public void testWhenStepFATALBlockingThenDoNotExecuteFinalStep()
        throws ProcessingException, StateNotAllowedException {
        VitamThreadUtils.getVitamSession().setTenantId(1);

        final ProcessWorkflow processWorkflow = processDataAccess.initProcessWorkflow(
            ProcessPopulator.populate(WORKFLOW_FINALLY_STEP_FILE).get(),
            workParams.getContainerName()
        );

        workParams.setLogbookTypeProcess(LogbookTypeProcess.INGEST);

        final ProcessDistributor distributorMock = mock(ProcessDistributorImpl.class);
        final ProcessEngineImpl processEngine = ProcessEngineFactoryTest.get().create(workParams, distributorMock);
        StateMachine stateMachine = StateMachineFactory.get()
            .create(processWorkflow, processEngine, dataManagement, workspaceClientFactory);
        processEngine.setStateMachineCallback(stateMachine);

        final ProcessStep firstStep = processWorkflow.getSteps().get(0);
        final ProcessStep lastStep = processWorkflow.getSteps().get(1);

        // First Step FATAL blocking call onComplete
        when(distributorMock.distribute(workParams, firstStep, workParams.getContainerName())).thenReturn(
            new ItemStatus(firstStep.getStepName()).increment(StatusCode.FATAL)
        );

        // Final Step OK call onComplete
        when(distributorMock.distribute(workParams, lastStep, workParams.getContainerName())).thenReturn(
            new ItemStatus(lastStep.getStepName()).increment(StatusCode.OK)
        );

        stateMachine.resume(workParams);
        waitProcessToFinish(processWorkflow, (ProcessEngineTest) processEngine);
        assertEquals(ProcessState.PAUSE, processWorkflow.getState());
        assertEquals(StatusCode.FATAL, processWorkflow.getStatus());

        assertEquals(StatusCode.FATAL, firstStep.getStepStatusCode());
        assertEquals(StatusCode.UNKNOWN, lastStep.getStepStatusCode());

        processDataAccess.clearWorkflow();
    }

    @Test
    @RunWithCustomExecutor
    public void testStepAndFinalStepThenOK() throws ProcessingException, StateNotAllowedException {
        VitamThreadUtils.getVitamSession().setTenantId(1);

        final ProcessWorkflow processWorkflow = processDataAccess.initProcessWorkflow(
            ProcessPopulator.populate(WORKFLOW_FINALLY_STEP_FILE).get(),
            workParams.getContainerName()
        );

        workParams.setLogbookTypeProcess(LogbookTypeProcess.INGEST);

        final ProcessDistributor distributorMock = mock(ProcessDistributorImpl.class);
        final ProcessEngineImpl processEngine = ProcessEngineFactoryTest.get().create(workParams, distributorMock);
        StateMachine stateMachine = StateMachineFactory.get()
            .create(processWorkflow, processEngine, dataManagement, workspaceClientFactory);
        processEngine.setStateMachineCallback(stateMachine);

        final ProcessStep firstStep = processWorkflow.getSteps().get(0);
        final ProcessStep lastStep = processWorkflow.getSteps().get(1);

        // First Step FATAL blocking call onComplete
        when(distributorMock.distribute(workParams, firstStep, workParams.getContainerName())).thenReturn(
            new ItemStatus(firstStep.getStepName()).increment(StatusCode.OK)
        );

        // Final Step OK call onComplete
        when(distributorMock.distribute(workParams, lastStep, workParams.getContainerName())).thenReturn(
            new ItemStatus(lastStep.getStepName()).increment(StatusCode.OK)
        );

        stateMachine.resume(workParams);
        waitProcessToFinish(processWorkflow, (ProcessEngineTest) processEngine);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(StatusCode.OK, processWorkflow.getStatus());

        assertEquals(StatusCode.OK, firstStep.getStepStatusCode());
        assertEquals(StatusCode.OK, lastStep.getStepStatusCode());

        processDataAccess.clearWorkflow();
    }

    @Test
    @RunWithCustomExecutor
    public void whenShutdownThenPauseOrCancelActionCurrentStepIsACTION_PAUSE()
        throws StateNotAllowedException, ProcessingException {
        VitamThreadUtils.getVitamSession().setTenantId(1);
        final ProcessDistributor processDistributorMock = mock(ProcessDistributorImpl.class);
        final ProcessEngine processEngine = ProcessEngineFactory.get().create(workParams, processDistributorMock);

        final ProcessWorkflow processWorkflow = processDataAccess.initProcessWorkflow(
            ProcessPopulator.populate(WORKFLOW_FINALLY_STEP_FILE).get(),
            workParams.getContainerName()
        );

        when(
            processDistributorMock.distribute(eq(workParams), any(Step.class), eq(workParams.getContainerName()))
        ).thenReturn(new ItemStatus().increment(StatusCode.OK));

        // Simulate running workflow to be able to test cancel a running workflow
        processWorkflow.setTargetState(ProcessState.PAUSE);
        StateMachine stateMachine = StateMachineFactory.get()
            .create(processWorkflow, processEngine, dataManagement, workspaceClientFactory);
        processEngine.setStateMachineCallback(stateMachine);
        stateMachine.resume(workParams);
        stateMachine.shutdown();

        assertThat(processWorkflow.getSteps().iterator().next().getPauseOrCancelAction()).isEqualTo(
            PauseOrCancelAction.ACTION_PAUSE
        );

        processDataAccess.clearWorkflow();
    }

    @Test
    @RunWithCustomExecutor
    public void whenShutdownThenPauseOrCancelActionCurrentStepIsACTION_CANCEL() throws StateNotAllowedException {
        VitamThreadUtils.getVitamSession().setTenantId(1);
        final ProcessDistributor processDistributorMock = mock(ProcessDistributorImpl.class);
        final ProcessEngine processEngine = ProcessEngineFactory.get()
            .create(mock(WorkerParameters.class), processDistributorMock);

        final ProcessWorkflow processWorkflow = processDataAccess.initProcessWorkflow(
            ProcessPopulator.populate(WORKFLOW_FINALLY_STEP_FILE).get(),
            workParams.getContainerName()
        );

        // Simulate running workflow to be able to test cancel a running workflow
        processWorkflow.setState(ProcessState.RUNNING);

        StateMachine stateMachine = StateMachineFactory.get()
            .create(processWorkflow, processEngine, dataManagement, workspaceClientFactory);
        processEngine.setStateMachineCallback(stateMachine);

        stateMachine.cancel();

        assertThat(processWorkflow.getSteps().iterator().next().getPauseOrCancelAction()).isEqualTo(
            PauseOrCancelAction.ACTION_CANCEL
        );
        processDataAccess.clearWorkflow();
    }

    @Test
    @RunWithCustomExecutor
    public void test_onUpdate_when_ProcessWorkflowStatusIsFATAL() {
        VitamThreadUtils.getVitamSession().setTenantId(1);
        final ProcessEngine processEngine = mock(ProcessEngine.class);

        final ProcessWorkflow processWorkflow = processDataAccess.initProcessWorkflow(
            ProcessPopulator.populate(WORKFLOW_FILE).get(),
            workParams.getContainerName()
        );
        processWorkflow.setStatus(StatusCode.FATAL);

        processWorkflow.getSteps().get(0).setStepStatusCode(StatusCode.OK);
        processWorkflow.getSteps().get(0).setPauseOrCancelAction(PauseOrCancelAction.ACTION_COMPLETE);
        processWorkflow.getSteps().get(1).setStepStatusCode(StatusCode.FATAL);

        StateMachine stateMachine = StateMachineFactory.get()
            .create(processWorkflow, processEngine, dataManagement, workspaceClientFactory);
        processEngine.setStateMachineCallback(stateMachine);

        // when distributor respond with OK then processWorkflowStatus should be OK
        stateMachine.onUpdate(StatusCode.OK);
        assertThat(processWorkflow.getStatus()).isEqualTo(StatusCode.OK);

        stateMachine.onUpdate(StatusCode.FATAL);
        assertThat(processWorkflow.getStatus()).isEqualTo(StatusCode.FATAL);

        stateMachine.onUpdate(StatusCode.WARNING);
        assertThat(processWorkflow.getStatus()).isEqualTo(StatusCode.WARNING);

        stateMachine.onUpdate(StatusCode.KO);
        assertThat(processWorkflow.getStatus()).isEqualTo(StatusCode.KO);

        stateMachine.onUpdate(StatusCode.OK);
        assertThat(processWorkflow.getStatus()).isEqualTo(StatusCode.KO);
    }

    @Test
    @RunWithCustomExecutor
    public void test_init_index_new_process_workflow() {
        VitamThreadUtils.getVitamSession().setTenantId(1);

        // When new process workflow
        final ProcessWorkflow processWorkflow = mock(ProcessWorkflow.class);
        when(processWorkflow.getState()).thenReturn(ProcessState.PAUSE);
        when(processWorkflow.getStatus()).thenReturn(StatusCode.UNKNOWN);

        final ProcessStep processStep1 = mock(ProcessStep.class);
        when(processStep1.getBehavior()).thenReturn(ProcessBehavior.BLOCKING);
        when(processStep1.getStepStatusCode()).thenReturn(StatusCode.UNKNOWN);
        when(processStep1.getPauseOrCancelAction()).thenReturn(PauseOrCancelAction.ACTION_RUN);

        final ProcessStep processStep2 = mock(ProcessStep.class);
        when(processStep2.getBehavior()).thenReturn(ProcessBehavior.BLOCKING);
        when(processStep2.getStepStatusCode()).thenReturn(StatusCode.UNKNOWN);
        when(processStep2.getPauseOrCancelAction()).thenReturn(PauseOrCancelAction.ACTION_RUN);

        when(processWorkflow.getSteps()).thenReturn(Lists.newArrayList(processStep1, processStep2));

        // When init state machine
        StateMachine stateMachine = StateMachineFactory.get()
            .create(
                processWorkflow,
                mock(ProcessEngineImpl.class),
                mock(ProcessDataManagement.class),
                workspaceClientFactory
            );

        assertThat(stateMachine.getCurrentStep()).isEqualTo(processStep1);
        assertThat(stateMachine.getStepIndex()).isEqualTo(0);
    }

    @Test
    @RunWithCustomExecutor
    public void test_init_index_when_first_step_status_started() {
        VitamThreadUtils.getVitamSession().setTenantId(1);
        // When new process workflow
        final ProcessWorkflow processWorkflow = mock(ProcessWorkflow.class);
        when(processWorkflow.getState()).thenReturn(ProcessState.PAUSE);
        when(processWorkflow.getStatus()).thenReturn(StatusCode.FATAL);

        final ProcessStep processStep1 = spy(ProcessStep.class);
        when(processStep1.getBehavior()).thenReturn(ProcessBehavior.BLOCKING);
        when(processStep1.getStepStatusCode()).thenReturn(StatusCode.STARTED);
        when(processStep1.getPauseOrCancelAction()).thenReturn(PauseOrCancelAction.ACTION_RUN);

        final ProcessStep processStep2 = mock(ProcessStep.class);
        when(processStep2.getBehavior()).thenReturn(ProcessBehavior.BLOCKING);
        when(processStep2.getStepStatusCode()).thenReturn(StatusCode.UNKNOWN);
        when(processStep2.getPauseOrCancelAction()).thenReturn(PauseOrCancelAction.ACTION_RUN);

        when(processWorkflow.getSteps()).thenReturn(Lists.newArrayList(processStep1, processStep2));

        // When init state machine
        StateMachine stateMachine = StateMachineFactory.get()
            .create(
                processWorkflow,
                mock(ProcessEngineImpl.class),
                mock(ProcessDataManagement.class),
                workspaceClientFactory
            );

        assertThat(stateMachine.getCurrentStep()).isEqualTo(processStep1);
        // Because of FATAL we replay the step 0
        assertThat(stateMachine.getStepIndex()).isEqualTo(0);
        verify(stateMachine.getCurrentStep()).setPauseOrCancelAction(eq(PauseOrCancelAction.ACTION_RECOVER));
    }

    @Test
    @RunWithCustomExecutor
    public void test_init_index_when_first_step_complete_status_fatal() {
        VitamThreadUtils.getVitamSession().setTenantId(1);
        // When new process workflow
        final ProcessWorkflow processWorkflow = mock(ProcessWorkflow.class);
        when(processWorkflow.getState()).thenReturn(ProcessState.PAUSE);
        when(processWorkflow.getStatus()).thenReturn(StatusCode.FATAL);

        final ProcessStep processStep1 = spy(ProcessStep.class);
        when(processStep1.getBehavior()).thenReturn(ProcessBehavior.BLOCKING);
        when(processStep1.getStepStatusCode()).thenReturn(StatusCode.FATAL);
        when(processStep1.getPauseOrCancelAction()).thenReturn(PauseOrCancelAction.ACTION_COMPLETE);

        final ProcessStep processStep2 = mock(ProcessStep.class);
        when(processStep2.getBehavior()).thenReturn(ProcessBehavior.BLOCKING);
        when(processStep2.getStepStatusCode()).thenReturn(StatusCode.UNKNOWN);
        when(processStep2.getPauseOrCancelAction()).thenReturn(PauseOrCancelAction.ACTION_RUN);

        when(processWorkflow.getSteps()).thenReturn(Lists.newArrayList(processStep1, processStep2));

        // When init state machine
        StateMachine stateMachine = StateMachineFactory.get()
            .create(
                processWorkflow,
                mock(ProcessEngineImpl.class),
                mock(ProcessDataManagement.class),
                workspaceClientFactory
            );

        assertThat(stateMachine.getCurrentStep()).isEqualTo(processStep1);
        // Because of FATAL we replay the step 0
        assertThat(stateMachine.getStepIndex()).isEqualTo(0);
        verify(stateMachine.getCurrentStep(), atLeastOnce()).setPauseOrCancelAction(
            eq(PauseOrCancelAction.ACTION_RECOVER)
        );
    }

    @Test
    @RunWithCustomExecutor
    public void test_init_index_when_first_step_complete_status_ok() {
        VitamThreadUtils.getVitamSession().setTenantId(1);
        // When new process workflow
        final ProcessWorkflow processWorkflow = mock(ProcessWorkflow.class);
        when(processWorkflow.getState()).thenReturn(ProcessState.PAUSE);
        when(processWorkflow.getStatus()).thenReturn(StatusCode.FATAL);

        final ProcessStep processStep1 = spy(ProcessStep.class);
        when(processStep1.getBehavior()).thenReturn(ProcessBehavior.BLOCKING);
        when(processStep1.getStepStatusCode()).thenReturn(StatusCode.OK);
        when(processStep1.getPauseOrCancelAction()).thenReturn(PauseOrCancelAction.ACTION_COMPLETE);

        final ProcessStep processStep2 = mock(ProcessStep.class);
        when(processStep2.getBehavior()).thenReturn(ProcessBehavior.BLOCKING);
        when(processStep2.getStepStatusCode()).thenReturn(StatusCode.UNKNOWN);
        when(processStep2.getPauseOrCancelAction()).thenReturn(PauseOrCancelAction.ACTION_RUN);

        when(processWorkflow.getSteps()).thenReturn(Lists.newArrayList(processStep1, processStep2));

        // When init state machine
        StateMachine stateMachine = StateMachineFactory.get()
            .create(
                processWorkflow,
                mock(ProcessEngineImpl.class),
                mock(ProcessDataManagement.class),
                workspaceClientFactory
            );

        assertThat(stateMachine.getCurrentStep()).isEqualTo(processStep2);
        // Because of FATAL we replay the step 1
        assertThat(stateMachine.getStepIndex()).isEqualTo(1);
        verify(stateMachine.getCurrentStep(), never()).setPauseOrCancelAction(eq(PauseOrCancelAction.ACTION_RECOVER));
    }

    @Test
    @RunWithCustomExecutor
    public void test_init_index_when_first_step_action_cancel() {
        VitamThreadUtils.getVitamSession().setTenantId(1);
        // When new process workflow
        final ProcessWorkflow processWorkflow = mock(ProcessWorkflow.class);
        when(processWorkflow.getState()).thenReturn(ProcessState.PAUSE);
        when(processWorkflow.getStatus()).thenReturn(StatusCode.FATAL);

        final ProcessStep processStep1 = spy(ProcessStep.class);
        when(processStep1.getBehavior()).thenReturn(ProcessBehavior.BLOCKING);
        when(processStep1.getStepStatusCode()).thenReturn(StatusCode.STARTED);
        when(processStep1.getPauseOrCancelAction()).thenReturn(PauseOrCancelAction.ACTION_CANCEL);

        final ProcessStep processStep2 = mock(ProcessStep.class);
        when(processStep2.getBehavior()).thenReturn(ProcessBehavior.BLOCKING);
        when(processStep2.getStepStatusCode()).thenReturn(StatusCode.UNKNOWN);
        when(processStep2.getPauseOrCancelAction()).thenReturn(PauseOrCancelAction.ACTION_RUN);

        when(processWorkflow.getSteps()).thenReturn(Lists.newArrayList(processStep1, processStep2));

        // When init state machine
        StateMachine stateMachine = StateMachineFactory.get()
            .create(
                processWorkflow,
                mock(ProcessEngineImpl.class),
                mock(ProcessDataManagement.class),
                workspaceClientFactory
            );

        assertThat(stateMachine.getCurrentStep()).isEqualTo(processStep2);
        // Because of FATAL we replay the step 1
        assertThat(stateMachine.getStepIndex()).isEqualTo(1);
        verify(stateMachine.getCurrentStep(), never()).setPauseOrCancelAction(eq(PauseOrCancelAction.ACTION_RECOVER));
    }

    @Test
    @RunWithCustomExecutor
    public void test_init_index_when_first_step_action_pause() {
        VitamThreadUtils.getVitamSession().setTenantId(1);
        // When new process workflow
        final ProcessWorkflow processWorkflow = mock(ProcessWorkflow.class);
        when(processWorkflow.getState()).thenReturn(ProcessState.PAUSE);
        when(processWorkflow.getStatus()).thenReturn(StatusCode.FATAL);

        final ProcessStep processStep1 = spy(ProcessStep.class);
        when(processStep1.getBehavior()).thenReturn(ProcessBehavior.BLOCKING);
        when(processStep1.getStepStatusCode()).thenReturn(StatusCode.OK);
        when(processStep1.getPauseOrCancelAction()).thenReturn(PauseOrCancelAction.ACTION_PAUSE);

        final ProcessStep processStep2 = mock(ProcessStep.class);
        when(processStep2.getBehavior()).thenReturn(ProcessBehavior.BLOCKING);
        when(processStep2.getStepStatusCode()).thenReturn(StatusCode.UNKNOWN);
        when(processStep2.getPauseOrCancelAction()).thenReturn(PauseOrCancelAction.ACTION_RUN);

        when(processWorkflow.getSteps()).thenReturn(Lists.newArrayList(processStep1, processStep2));

        // When init state machine
        StateMachine stateMachine = StateMachineFactory.get()
            .create(
                processWorkflow,
                mock(ProcessEngineImpl.class),
                mock(ProcessDataManagement.class),
                workspaceClientFactory
            );

        assertThat(stateMachine.getCurrentStep()).isEqualTo(processStep1);
        // Because of FATAL we replay the step 0
        assertThat(stateMachine.getStepIndex()).isEqualTo(0);
        verify(stateMachine.getCurrentStep(), atLeastOnce()).setPauseOrCancelAction(
            eq(PauseOrCancelAction.ACTION_RECOVER)
        );
    }

    @Test
    @RunWithCustomExecutor
    public void test_init_index_when_last_step_status_started() {
        VitamThreadUtils.getVitamSession().setTenantId(1);
        // When new process workflow
        final ProcessWorkflow processWorkflow = mock(ProcessWorkflow.class);
        when(processWorkflow.getState()).thenReturn(ProcessState.PAUSE);
        when(processWorkflow.getStatus()).thenReturn(StatusCode.FATAL);

        final ProcessStep processStep1 = spy(ProcessStep.class);
        when(processStep1.getBehavior()).thenReturn(ProcessBehavior.BLOCKING);
        when(processStep1.getStepStatusCode()).thenReturn(StatusCode.OK);
        when(processStep1.getPauseOrCancelAction()).thenReturn(PauseOrCancelAction.ACTION_COMPLETE);

        final ProcessStep processStep2 = mock(ProcessStep.class);
        when(processStep2.getBehavior()).thenReturn(ProcessBehavior.BLOCKING);
        when(processStep2.getStepStatusCode()).thenReturn(StatusCode.STARTED);
        when(processStep2.getPauseOrCancelAction()).thenReturn(PauseOrCancelAction.ACTION_RUN);

        when(processWorkflow.getSteps()).thenReturn(Lists.newArrayList(processStep1, processStep2));

        // When init state machine
        StateMachine stateMachine = StateMachineFactory.get()
            .create(
                processWorkflow,
                mock(ProcessEngineImpl.class),
                mock(ProcessDataManagement.class),
                workspaceClientFactory
            );

        assertThat(stateMachine.getCurrentStep()).isEqualTo(processStep2);
        // Because of FATAL we replay the step 1
        assertThat(stateMachine.getStepIndex()).isEqualTo(1);
        verify(stateMachine.getCurrentStep()).setPauseOrCancelAction(eq(PauseOrCancelAction.ACTION_RECOVER));
    }

    @Test
    @RunWithCustomExecutor
    public void test_init_index_when_last_step_complete_status_fatal() {
        VitamThreadUtils.getVitamSession().setTenantId(1);
        // When new process workflow
        final ProcessWorkflow processWorkflow = mock(ProcessWorkflow.class);
        when(processWorkflow.getState()).thenReturn(ProcessState.PAUSE);
        when(processWorkflow.getStatus()).thenReturn(StatusCode.FATAL);

        final ProcessStep processStep1 = spy(ProcessStep.class);
        when(processStep1.getBehavior()).thenReturn(ProcessBehavior.BLOCKING);
        when(processStep1.getStepStatusCode()).thenReturn(StatusCode.OK);
        when(processStep1.getPauseOrCancelAction()).thenReturn(PauseOrCancelAction.ACTION_COMPLETE);

        final ProcessStep processStep2 = mock(ProcessStep.class);
        when(processStep2.getBehavior()).thenReturn(ProcessBehavior.BLOCKING);
        when(processStep2.getStepStatusCode()).thenReturn(StatusCode.FATAL);
        when(processStep2.getPauseOrCancelAction()).thenReturn(PauseOrCancelAction.ACTION_COMPLETE);

        when(processWorkflow.getSteps()).thenReturn(Lists.newArrayList(processStep1, processStep2));

        // When init state machine
        StateMachine stateMachine = StateMachineFactory.get()
            .create(
                processWorkflow,
                mock(ProcessEngineImpl.class),
                mock(ProcessDataManagement.class),
                workspaceClientFactory
            );

        assertThat(stateMachine.getCurrentStep()).isEqualTo(processStep2);
        // Because of FATAL we replay the step 1
        assertThat(stateMachine.getStepIndex()).isEqualTo(1);
        verify(stateMachine.getCurrentStep(), atLeastOnce()).setPauseOrCancelAction(
            eq(PauseOrCancelAction.ACTION_RECOVER)
        );
    }

    @Test
    @RunWithCustomExecutor
    public void test_init_index_when_last_step_complete_status_ok() {
        VitamThreadUtils.getVitamSession().setTenantId(1);
        // When new process workflow
        final ProcessWorkflow processWorkflow = mock(ProcessWorkflow.class);
        when(processWorkflow.getState()).thenReturn(ProcessState.PAUSE);
        when(processWorkflow.getStatus()).thenReturn(StatusCode.FATAL);

        final ProcessStep processStep1 = spy(ProcessStep.class);
        when(processStep1.getBehavior()).thenReturn(ProcessBehavior.BLOCKING);
        when(processStep1.getStepStatusCode()).thenReturn(StatusCode.OK);
        when(processStep1.getPauseOrCancelAction()).thenReturn(PauseOrCancelAction.ACTION_COMPLETE);

        final ProcessStep processStep2 = mock(ProcessStep.class);
        when(processStep2.getBehavior()).thenReturn(ProcessBehavior.BLOCKING);
        when(processStep2.getStepStatusCode()).thenReturn(StatusCode.OK);
        when(processStep2.getPauseOrCancelAction()).thenReturn(PauseOrCancelAction.ACTION_COMPLETE);

        when(processWorkflow.getSteps()).thenReturn(Lists.newArrayList(processStep1, processStep2));

        // When init state machine
        StateMachine stateMachine = StateMachineFactory.get()
            .create(
                processWorkflow,
                mock(ProcessEngineImpl.class),
                mock(ProcessDataManagement.class),
                workspaceClientFactory
            );

        assertThat(stateMachine.getCurrentStep()).isEqualTo(processStep2);
        // Because of FATAL we replay the step 1
        assertThat(stateMachine.getStepIndex()).isEqualTo(1);
    }

    @Test
    @RunWithCustomExecutor
    public void test_init_index_when_last_step_action_cancel() {
        VitamThreadUtils.getVitamSession().setTenantId(1);
        // When new process workflow
        final ProcessWorkflow processWorkflow = mock(ProcessWorkflow.class);
        when(processWorkflow.getState()).thenReturn(ProcessState.PAUSE);
        when(processWorkflow.getStatus()).thenReturn(StatusCode.FATAL);

        final ProcessStep processStep1 = spy(ProcessStep.class);
        when(processStep1.getBehavior()).thenReturn(ProcessBehavior.BLOCKING);
        when(processStep1.getStepStatusCode()).thenReturn(StatusCode.OK);
        when(processStep1.getPauseOrCancelAction()).thenReturn(PauseOrCancelAction.ACTION_COMPLETE);

        final ProcessStep processStep2 = mock(ProcessStep.class);
        when(processStep2.getBehavior()).thenReturn(ProcessBehavior.BLOCKING);
        when(processStep2.getStepStatusCode()).thenReturn(StatusCode.STARTED);
        when(processStep2.getPauseOrCancelAction()).thenReturn(PauseOrCancelAction.ACTION_CANCEL);

        when(processWorkflow.getSteps()).thenReturn(Lists.newArrayList(processStep1, processStep2));

        // When init state machine
        StateMachine stateMachine = StateMachineFactory.get()
            .create(
                processWorkflow,
                mock(ProcessEngineImpl.class),
                mock(ProcessDataManagement.class),
                workspaceClientFactory
            );

        assertThat(stateMachine.getCurrentStep()).isEqualTo(processStep2);
        // Because of FATAL we replay the step 1
        assertThat(stateMachine.getStepIndex()).isEqualTo(1);
        verify(stateMachine.getCurrentStep(), never()).setPauseOrCancelAction(eq(PauseOrCancelAction.ACTION_RECOVER));
    }

    @Test
    @RunWithCustomExecutor
    public void test_init_index_when_last_step_action_pause() {
        VitamThreadUtils.getVitamSession().setTenantId(1);
        // When new process workflow
        final ProcessWorkflow processWorkflow = mock(ProcessWorkflow.class);
        when(processWorkflow.getState()).thenReturn(ProcessState.PAUSE);
        when(processWorkflow.getStatus()).thenReturn(StatusCode.FATAL);

        final ProcessStep processStep1 = spy(ProcessStep.class);
        when(processStep1.getBehavior()).thenReturn(ProcessBehavior.BLOCKING);
        when(processStep1.getStepStatusCode()).thenReturn(StatusCode.OK);
        when(processStep1.getPauseOrCancelAction()).thenReturn(PauseOrCancelAction.ACTION_COMPLETE);

        final ProcessStep processStep2 = mock(ProcessStep.class);
        when(processStep2.getBehavior()).thenReturn(ProcessBehavior.BLOCKING);
        when(processStep2.getStepStatusCode()).thenReturn(StatusCode.OK);
        when(processStep2.getPauseOrCancelAction()).thenReturn(PauseOrCancelAction.ACTION_PAUSE);

        when(processWorkflow.getSteps()).thenReturn(Lists.newArrayList(processStep1, processStep2));

        // When init state machine
        StateMachine stateMachine = StateMachineFactory.get()
            .create(
                processWorkflow,
                mock(ProcessEngineImpl.class),
                mock(ProcessDataManagement.class),
                workspaceClientFactory
            );

        assertThat(stateMachine.getCurrentStep()).isEqualTo(processStep2);
        // Because of FATAL we replay the step 1
        assertThat(stateMachine.getStepIndex()).isEqualTo(1);
        verify(stateMachine.getCurrentStep(), atLeastOnce()).setPauseOrCancelAction(
            eq(PauseOrCancelAction.ACTION_RECOVER)
        );
    }

    private void waitProcessToFinish(ProcessWorkflow processWorkflow, ProcessEngineTest processEngine) {
        for (int i = 0; i < processWorkflow.getSteps().size(); i++) {
            CompletableFuture<ItemStatus> taskToWait = processEngine.getCurrentTask();
            taskToWait.join();
            if (taskToWait.equals(processEngine.getCurrentTask())) break;
        }
    }
}
