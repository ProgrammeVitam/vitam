/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019) <p> contact.vitam@culture.gouv.fr <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently. <p> This software is governed by the CeCILL 2.1 license under French law and
 * abiding by the rules of distribution of free software. You can use, modify and/ or redistribute the software under
 * the terms of the CeCILL 2.1 license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info". <p> As a counterpart to the access to the source code and rights to copy, modify and
 * redistribute granted by the license, users are provided only with a limited warranty and the software's author, the
 * holder of the economic rights, and the successive licensors have only limited liability. <p> In this respect, the
 * user's attention is drawn to the risks associated with loading, using, modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software, that may mean that it is complicated to
 * manipulate, and that also therefore means that it is reserved for developers and experienced professionals having
 * in-depth computer knowledge. Users are therefore encouraged to load and test the software's suitability as regards
 * their requirements in conditions enabling the security of their systems and/or data to be ensured and, more
 * generally, to use and operate it in the same conditions as regards security. <p> The fact that you are presently
 * reading this means that you have had knowledge of the CeCILL 2.1 license and that you accept its terms.
 */

package fr.gouv.vitam.processing.management.core;

import fr.gouv.vitam.common.exception.StateNotAllowedException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingEngineException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.PauseRecover;
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
import org.mockito.internal.verification.VerificationModeFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class StateMachineTest {

    private static final Integer TENANT_ID = 0;
    private WorkerParameters workParams;

    private ProcessDataAccessImpl processDataAccess;

    private static final String WORKFLOW_FILE = "workflowJSONv1.json";
    private static final String WORKFLOW_FINALLY_STEP_FILE = "workflowJSONFinallyStep.json";


    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());


    private static final WorkspaceClientFactory workspaceClientFactory = mock(WorkspaceClientFactory.class);
    private static final LogbookOperationsClientFactory logbookOperationsClientFactory =
        mock(LogbookOperationsClientFactory.class);
    private static final ProcessDataManagement dataManagement = mock(ProcessDataManagement.class);

    @Before
    public void setup() {
        when(logbookOperationsClientFactory.getClient()).thenReturn(mock(LogbookOperationsClient.class));
        when(workspaceClientFactory.getClient()).thenReturn(mock(WorkspaceClient.class));
        workParams = WorkerParametersFactory.newWorkerParameters();
        workParams
            .setWorkerGUID(GUIDFactory.newGUID())
            .setUrlMetadata("http://localhost:8083")
            .setUrlWorkspace("http://localhost:8083")
            .setContainerName(GUIDFactory.newGUID().getId())
            .setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_ID).toString())
            .setWorkflowIdentifier("DEFAULT_WORKFLOW");

        processDataAccess = ProcessDataAccessImpl.getInstance();
    }

    @Test
    @RunWithCustomExecutor
    public void getPauseAnyActionOK() throws StateNotAllowedException, ProcessingException {
        StateMachine stateMachine = mock(StateMachine.class);
        ProcessState state = ProcessState.PAUSE;
        doAnswer(o -> {
            state.eval(ProcessState.RUNNING);
            return null;
        }).when(stateMachine).next(any());
        stateMachine.next(null);

        doAnswer(o -> {
            state.eval(ProcessState.RUNNING);
            return null;
        }).when(stateMachine).resume(any());
        stateMachine.resume(null);

        doAnswer(o -> {
            state.eval(ProcessState.PAUSE);
            return null;
        }).when(stateMachine).pause();
        stateMachine.pause();

        doAnswer(o -> {
            state.eval(ProcessState.COMPLETED);
            return null;
        }).when(stateMachine).cancel();
        stateMachine.cancel();
    }

    @Test
    @RunWithCustomExecutor
    public void getRunningTestPauseAndCancelOK() throws StateNotAllowedException, ProcessingException {
        StateMachine stateMachine = mock(StateMachine.class);
        ProcessState state = ProcessState.RUNNING;
        doAnswer(o -> {
            state.eval(ProcessState.PAUSE);
            return null;
        }).when(stateMachine).pause();
        stateMachine.pause();

        doAnswer(o -> {
            state.eval(ProcessState.COMPLETED);
            return null;
        }).when(stateMachine).cancel();
        stateMachine.cancel();
    }

    @Test(expected = StateNotAllowedException.class)
    @RunWithCustomExecutor
    public void getRunningTestNextKO() throws StateNotAllowedException, ProcessingException {
        StateMachine stateMachine = mock(StateMachine.class);
        ProcessState state = ProcessState.RUNNING;
        doAnswer(o -> {
            state.eval(ProcessState.RUNNING);
            return null;
        }).when(stateMachine).next(any());

        stateMachine.next(null);
    }


    @Test(expected = StateNotAllowedException.class)
    @RunWithCustomExecutor
    public void getRunningTestResumeKO() throws StateNotAllowedException, ProcessingException {
        StateMachine stateMachine = mock(StateMachine.class);
        ProcessState state = ProcessState.RUNNING;
        doAnswer(o -> {
            state.eval(ProcessState.RUNNING);
            return null;
        }).when(stateMachine).resume(any());

        stateMachine.resume(null);
    }

    @Test(expected = StateNotAllowedException.class)
    @RunWithCustomExecutor
    public void getCompletedTestNextKO() throws StateNotAllowedException, ProcessingException {
        StateMachine stateMachine = mock(StateMachine.class);
        ProcessState state = ProcessState.COMPLETED;
        doAnswer(o -> {
            state.eval(ProcessState.RUNNING);
            return null;
        }).when(stateMachine).next(any());

        stateMachine.next(null);
    }


    @Test(expected = StateNotAllowedException.class)
    @RunWithCustomExecutor
    public void getCompletedTestResumeKO() throws StateNotAllowedException, ProcessingException {
        StateMachine stateMachine = mock(StateMachine.class);
        ProcessState state = ProcessState.COMPLETED;
        doAnswer(o -> {
            state.eval(ProcessState.RUNNING);
            return null;
        }).when(stateMachine).resume(any());

        stateMachine.resume(null);
    }

    @Test(expected = StateNotAllowedException.class)
    @RunWithCustomExecutor
    public void getCompletedTestPauseKO() throws StateNotAllowedException {
        StateMachine stateMachine = mock(StateMachine.class);
        ProcessState state = ProcessState.COMPLETED;
        doAnswer(o -> {
            state.eval(ProcessState.PAUSE);
            return null;
        }).when(stateMachine).pause();

        stateMachine.pause();
    }


    @Test(expected = StateNotAllowedException.class)
    @RunWithCustomExecutor
    public void getCompletedTestCancelKO() throws StateNotAllowedException, ProcessingException {
        StateMachine stateMachine = mock(StateMachine.class);
        ProcessState state = ProcessState.COMPLETED;
        doAnswer(o -> {
            state.eval(ProcessState.COMPLETED);
            return null;
        }).when(stateMachine).cancel();

        stateMachine.cancel();
    }



    @Test
    @RunWithCustomExecutor
    public void tryAllActionsOnRunningState()
        throws ProcessingException, StateNotAllowedException, ProcessingEngineException {
        VitamThreadUtils.getVitamSession().setTenantId(1);

        final ProcessWorkflow processWorkflow =
            processDataAccess.initProcessWorkflow(
                ProcessPopulator.populate(WORKFLOW_FILE).get(),
                workParams.getContainerName()
            );

        final ProcessEngine processEngine = mock(ProcessEngineImpl.class);
        final StateMachine stateMachine = StateMachineFactory.get()
            .create(processWorkflow, processEngine, dataManagement, workspaceClientFactory,
                logbookOperationsClientFactory);

        doAnswer(invocation -> null).when(processEngine).start(any(), any(), any());
        stateMachine.next(workParams);
        try {
            stateMachine.next(workParams);
            fail("Should throw excetpion");
        } catch (StateNotAllowedException e) {
        }
        try {
            stateMachine.resume(workParams);
            fail("Should throw excetpion");
        } catch (StateNotAllowedException e) {
        }
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
     * @throws InterruptedException
     */
    @Test
    @RunWithCustomExecutor
    public void testWhenProcessEngineOnCompleteOK()
        throws ProcessingException, StateNotAllowedException, InterruptedException {
        VitamThreadUtils.getVitamSession().setTenantId(1);

        final ProcessWorkflow processWorkflow =
            processDataAccess.initProcessWorkflow(
                ProcessPopulator.populate(WORKFLOW_FILE).get(),
                workParams.getContainerName()
            );

        workParams.setLogbookTypeProcess(LogbookTypeProcess.INGEST);

        final ProcessDistributor processDistributorMock = mock(ProcessDistributorImpl.class);
        final ProcessEngineImpl processEngine = ProcessEngineFactory.get().create(workParams, processDistributorMock);
        StateMachine stateMachine = StateMachineFactory.get()
            .create(processWorkflow, processEngine, dataManagement, workspaceClientFactory,
                logbookOperationsClientFactory);
        processEngine.setCallback(stateMachine);

        ProcessStep firstStep = processWorkflow.getSteps().iterator().next();
        ItemStatus itemStatus = new ItemStatus(firstStep.getStepName()).increment(StatusCode.OK);
        when(processDistributorMock.distribute(any(), any(), any(), any()))
            .thenReturn(itemStatus);
        stateMachine.resume(workParams);
        int nbtry = 50;
        while (!ProcessState.COMPLETED.equals(processWorkflow.getState())) {
            Thread.sleep(20);
            nbtry--;
            if (nbtry < 0)
                break;
        }
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(StatusCode.OK, processWorkflow.getStatus());

        try {
            stateMachine.next(workParams);
            fail("Should throw an exception");
        } catch (StateNotAllowedException e) {
        }

        processDataAccess.clearWorkflow();
    }

    /**
     * Test onComplete
     *
     * @throws ProcessingException
     * @throws StateNotAllowedException
     * @throws ProcessingEngineException
     * @throws InterruptedException
     */
    @Test
    @RunWithCustomExecutor
    public void testWhenProcessEngineOnCompleteKO()
        throws ProcessingException, StateNotAllowedException, InterruptedException {
        VitamThreadUtils.getVitamSession().setTenantId(1);

        final ProcessWorkflow processWorkflow =
            processDataAccess.initProcessWorkflow(
                ProcessPopulator.populate(WORKFLOW_FILE).get(),
                workParams.getContainerName()
            );

        workParams.setLogbookTypeProcess(LogbookTypeProcess.INGEST);

        final ProcessDistributor processDistributorMock = mock(ProcessDistributorImpl.class);
        final ProcessEngineImpl processEngine = ProcessEngineFactory.get().create(workParams, processDistributorMock);
        StateMachine stateMachine = StateMachineFactory.get()
            .create(processWorkflow, processEngine, dataManagement, workspaceClientFactory,
                logbookOperationsClientFactory);
        processEngine.setCallback(stateMachine);

        ProcessStep firstStep = processWorkflow.getSteps().iterator().next();
        ItemStatus itemStatus = new ItemStatus(firstStep.getStepName()).increment(StatusCode.KO);
        when(processDistributorMock.distribute(any(), any(), any(), any()))
            .thenReturn(itemStatus);
        stateMachine.resume(workParams);
        int nbtry = 50;
        while (!ProcessState.COMPLETED.equals(processWorkflow.getState())) {
            Thread.sleep(20);
            nbtry--;
            if (nbtry < 0)
                break;
        }
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(StatusCode.KO, processWorkflow.getStatus());

        try {
            stateMachine.next(workParams);
            fail("Should throw an exception");
        } catch (StateNotAllowedException e) {
        }

        processDataAccess.clearWorkflow();
    }

    /**
     * Test onError
     *
     * @throws ProcessingException
     * @throws StateNotAllowedException
     * @throws ProcessingEngineException
     * @throws InterruptedException
     */
    @Test
    @RunWithCustomExecutor
    public void testWhenProcessEngineOnErrorFATAL()
        throws ProcessingException, StateNotAllowedException, InterruptedException {
        VitamThreadUtils.getVitamSession().setTenantId(1);

        final ProcessWorkflow processWorkflow =
            processDataAccess.initProcessWorkflow(
                ProcessPopulator.populate(WORKFLOW_FILE).get(),
                workParams.getContainerName()
            );

        workParams.setLogbookTypeProcess(LogbookTypeProcess.INGEST);

        final ProcessDistributor processDistributorMock = mock(ProcessDistributorImpl.class);
        final ProcessEngineImpl processEngine = ProcessEngineFactory.get().create(workParams, processDistributorMock);
        StateMachine stateMachine = StateMachineFactory.get()
            .create(processWorkflow, processEngine, dataManagement, workspaceClientFactory,
                logbookOperationsClientFactory);
        processEngine.setCallback(stateMachine);

        when(processDistributorMock.distribute(any(), any(), any(), any()))
            .thenThrow(new RuntimeException("Fake Exception From Distributor"));

        stateMachine.resume(workParams);
        int nbtry = 50;
        while (!ProcessState.PAUSE.equals(processWorkflow.getState())) {
            Thread.sleep(20);
            nbtry--;
            if (nbtry < 0)
                break;
        }
        assertEquals(ProcessState.PAUSE, processWorkflow.getState());
        assertEquals(StatusCode.FATAL, processWorkflow.getStatus());

        processDataAccess.clearWorkflow();
    }

    @Test
    @RunWithCustomExecutor
    public void testWhenExceptionOccurThenDoNotExecuteFinallyStep()
        throws ProcessingException, StateNotAllowedException, InterruptedException {
        VitamThreadUtils.getVitamSession().setTenantId(1);

        final ProcessWorkflow processWorkflow =
            processDataAccess.initProcessWorkflow(
                ProcessPopulator.populate(WORKFLOW_FINALLY_STEP_FILE).get(),
                workParams.getContainerName()
            );

        workParams.setLogbookTypeProcess(LogbookTypeProcess.INGEST);

        final ProcessDistributor distributorMock = mock(ProcessDistributorImpl.class);
        final ProcessEngineImpl processEngine = ProcessEngineFactory.get().create(workParams, distributorMock);
        StateMachine stateMachine = StateMachineFactory.get()
            .create(processWorkflow, processEngine, dataManagement, workspaceClientFactory,
                logbookOperationsClientFactory);
        processEngine.setCallback(stateMachine);

        final ProcessStep firstStep = processWorkflow.getSteps().get(0);
        final ProcessStep lastStep = processWorkflow.getSteps().get(1);

        // First Step FATAL call onError
        when(distributorMock.distribute(workParams, firstStep, workParams.getContainerName(), PauseRecover.NO_RECOVER))
            .thenThrow(new RuntimeException("Fake Exception From Distributor"));

        // Finally Step OK call onComplete
        when(distributorMock.distribute(workParams, lastStep, workParams.getContainerName(), PauseRecover.NO_RECOVER))
            .thenReturn(new ItemStatus(lastStep.getStepName()).increment(StatusCode.OK));

        stateMachine.resume(workParams);

        int nbtry = 50;
        while (!ProcessState.PAUSE.equals(processWorkflow.getState())) {
            Thread.sleep(20);
            nbtry--;
            if (nbtry < 0)
                break;
        }
        assertEquals(ProcessState.PAUSE, processWorkflow.getState());
        assertEquals(StatusCode.FATAL, processWorkflow.getStatus());

        assertEquals(StatusCode.STARTED, firstStep.getStepStatusCode());
        assertEquals(StatusCode.UNKNOWN, lastStep.getStepStatusCode());

        processDataAccess.clearWorkflow();
    }

    @Test
    @RunWithCustomExecutor
    public void testWhenStepKOBlockingThenExecuteFinallyStep()
        throws ProcessingException, StateNotAllowedException, InterruptedException {
        VitamThreadUtils.getVitamSession().setTenantId(1);

        final ProcessWorkflow processWorkflow =
            processDataAccess.initProcessWorkflow(
                ProcessPopulator.populate(WORKFLOW_FINALLY_STEP_FILE).get(),
                workParams.getContainerName()
            );

        workParams.setLogbookTypeProcess(LogbookTypeProcess.INGEST);

        final ProcessDistributor distributorMock = mock(ProcessDistributorImpl.class);
        final ProcessEngineImpl processEngine = ProcessEngineFactory.get().create(workParams, distributorMock);
        StateMachine stateMachine = StateMachineFactory.get()
            .create(processWorkflow, processEngine, dataManagement, workspaceClientFactory,
                logbookOperationsClientFactory);
        processEngine.setCallback(stateMachine);

        final ProcessStep firstStep = processWorkflow.getSteps().get(0);
        final ProcessStep lastStep = processWorkflow.getSteps().get(1);

        // First Step KO blocking call onComplete
        when(distributorMock.distribute(workParams, firstStep, workParams.getContainerName(), PauseRecover.NO_RECOVER))
            .thenReturn(new ItemStatus(firstStep.getStepName()).increment(StatusCode.KO));

        // Finally Step OK call onComplete
        when(distributorMock.distribute(workParams, lastStep, workParams.getContainerName(), PauseRecover.NO_RECOVER))
            .thenReturn(new ItemStatus(lastStep.getStepName()).increment(StatusCode.OK));

        stateMachine.resume(workParams);
        int nbtry = 50;
        while (!ProcessState.COMPLETED.equals(processWorkflow.getState())) {
            Thread.sleep(20);
            nbtry--;
            if (nbtry < 0)
                break;
        }
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(StatusCode.KO, processWorkflow.getStatus());

        assertEquals(StatusCode.KO, firstStep.getStepStatusCode());
        assertEquals(StatusCode.OK, lastStep.getStepStatusCode());

        processDataAccess.clearWorkflow();
    }

    @Test
    @RunWithCustomExecutor
    public void testWhenStepFATALBlockingThenDoNotExecuteFinallyStep()
        throws ProcessingException, StateNotAllowedException, InterruptedException {
        VitamThreadUtils.getVitamSession().setTenantId(1);

        final ProcessWorkflow processWorkflow =
            processDataAccess.initProcessWorkflow(
                ProcessPopulator.populate(WORKFLOW_FINALLY_STEP_FILE).get(),
                workParams.getContainerName()
            );

        workParams.setLogbookTypeProcess(LogbookTypeProcess.INGEST);

        final ProcessDistributor distributorMock = mock(ProcessDistributorImpl.class);
        final ProcessEngineImpl processEngine = ProcessEngineFactory.get().create(workParams, distributorMock);
        StateMachine stateMachine = StateMachineFactory.get()
            .create(processWorkflow, processEngine, dataManagement, workspaceClientFactory,
                logbookOperationsClientFactory);
        processEngine.setCallback(stateMachine);

        final ProcessStep firstStep = processWorkflow.getSteps().get(0);
        final ProcessStep lastStep = processWorkflow.getSteps().get(1);

        // First Step FATAL blocking call onComplete
        when(distributorMock.distribute(workParams, firstStep, workParams.getContainerName(), PauseRecover.NO_RECOVER))
            .thenReturn(new ItemStatus(firstStep.getStepName()).increment(StatusCode.FATAL));

        // Finally Step OK call onComplete
        when(distributorMock.distribute(workParams, lastStep, workParams.getContainerName(), PauseRecover.NO_RECOVER))
            .thenReturn(new ItemStatus(lastStep.getStepName()).increment(StatusCode.OK));

        stateMachine.resume(workParams);
        int nbtry = 50;
        while (!ProcessState.PAUSE.equals(processWorkflow.getState())) {
            Thread.sleep(20);
            nbtry--;
            if (nbtry < 0)
                break;
        }
        assertEquals(ProcessState.PAUSE, processWorkflow.getState());
        assertEquals(StatusCode.FATAL, processWorkflow.getStatus());

        assertEquals(StatusCode.FATAL, firstStep.getStepStatusCode());
        assertEquals(StatusCode.UNKNOWN, lastStep.getStepStatusCode());

        processDataAccess.clearWorkflow();
    }


    @Test
    @RunWithCustomExecutor
    public void testStepAndFinallyStepThenOK()
        throws ProcessingException, StateNotAllowedException, InterruptedException {
        VitamThreadUtils.getVitamSession().setTenantId(1);

        final ProcessWorkflow processWorkflow =
            processDataAccess.initProcessWorkflow(
                ProcessPopulator.populate(WORKFLOW_FINALLY_STEP_FILE).get(),
                workParams.getContainerName()
            );

        workParams.setLogbookTypeProcess(LogbookTypeProcess.INGEST);

        final ProcessDistributor distributorMock = mock(ProcessDistributorImpl.class);
        final ProcessEngineImpl processEngine = ProcessEngineFactory.get().create(workParams, distributorMock);
        StateMachine stateMachine = StateMachineFactory.get()
            .create(processWorkflow, processEngine, dataManagement, workspaceClientFactory,
                logbookOperationsClientFactory);
        processEngine.setCallback(stateMachine);

        final ProcessStep firstStep = processWorkflow.getSteps().get(0);
        final ProcessStep lastStep = processWorkflow.getSteps().get(1);

        // First Step FATAL blocking call onComplete
        when(distributorMock.distribute(workParams, firstStep, workParams.getContainerName(), PauseRecover.NO_RECOVER))
            .thenReturn(new ItemStatus(firstStep.getStepName()).increment(StatusCode.OK));

        // Finally Step OK call onComplete
        when(distributorMock.distribute(workParams, lastStep, workParams.getContainerName(), PauseRecover.NO_RECOVER))
            .thenReturn(new ItemStatus(lastStep.getStepName()).increment(StatusCode.OK));

        stateMachine.resume(workParams);
        int nbtry = 50;
        while (!ProcessState.COMPLETED.equals(processWorkflow.getState())) {
            Thread.sleep(20);
            nbtry--;
            if (nbtry < 0)
                break;
        }
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(StatusCode.OK, processWorkflow.getStatus());

        assertEquals(StatusCode.OK, firstStep.getStepStatusCode());
        assertEquals(StatusCode.OK, lastStep.getStepStatusCode());

        processDataAccess.clearWorkflow();
    }



    @Test
    @RunWithCustomExecutor
    public void whenShutdownThenPauseOnDistributorOccurred()
        throws ProcessingException, StateNotAllowedException {
        VitamThreadUtils.getVitamSession().setTenantId(1);
        final ProcessDistributor processDistributorMock = mock(ProcessDistributorImpl.class);
        final ProcessEngine processEngine =
            ProcessEngineFactory.get().create(mock(WorkerParameters.class), processDistributorMock);

        final ProcessWorkflow processWorkflow =
            processDataAccess.initProcessWorkflow(
                ProcessPopulator.populate(WORKFLOW_FINALLY_STEP_FILE).get(),
                workParams.getContainerName()
            );

        StateMachine stateMachine = StateMachineFactory.get()
            .create(processWorkflow, processEngine, dataManagement, workspaceClientFactory,
                logbookOperationsClientFactory);
        processEngine.setCallback(stateMachine);
        stateMachine.shutdown();
        verify(processDistributorMock, VerificationModeFactory.atLeastOnce()).pause(anyString());

        processDataAccess.clearWorkflow();
    }


    @Test
    @RunWithCustomExecutor
    public void whenCancelThenCancelOnDistributorOccurred()
        throws ProcessingException, StateNotAllowedException {
        VitamThreadUtils.getVitamSession().setTenantId(1);
        final ProcessDistributor processDistributorMock = mock(ProcessDistributorImpl.class);
        final ProcessEngine processEngine =
            ProcessEngineFactory.get().create(mock(WorkerParameters.class), processDistributorMock);

        final ProcessWorkflow processWorkflow =
            processDataAccess.initProcessWorkflow(
                ProcessPopulator.populate(WORKFLOW_FINALLY_STEP_FILE).get(),
                workParams.getContainerName()
            );

        // Simulate running workflow to be able to test cancel a running workflow
        processWorkflow.setState(ProcessState.RUNNING);

        StateMachine stateMachine = StateMachineFactory.get()
            .create(processWorkflow, processEngine, dataManagement, workspaceClientFactory,
                logbookOperationsClientFactory);
        processEngine.setCallback(stateMachine);

        stateMachine.cancel();

        verify(processDistributorMock).cancel(anyString());

        processDataAccess.clearWorkflow();
    }
}
