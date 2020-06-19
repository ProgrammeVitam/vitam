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
package fr.gouv.vitam.processing.engine.core;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.WorkflowNotFoundException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.PauseOrCancelAction;
import fr.gouv.vitam.common.model.processing.WorkFlow;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.processing.common.automation.IEventsProcessEngine;
import fr.gouv.vitam.processing.common.exception.ProcessingEngineException;
import fr.gouv.vitam.processing.common.model.PauseRecover;
import fr.gouv.vitam.processing.common.model.ProcessStep;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.processing.data.core.ProcessDataAccess;
import fr.gouv.vitam.processing.data.core.ProcessDataAccessImpl;
import fr.gouv.vitam.processing.distributor.api.ProcessDistributor;
import fr.gouv.vitam.processing.engine.api.ProcessEngine;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.FileNotFoundException;
import java.util.concurrent.CountDownLatch;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Do not forget init method on test method !
 */
public class ProcessEngineImplTest {
    private ProcessEngine processEngine;
    private WorkerParameters workParams;
    private static final Integer TENANT_ID = 0;
    private static final String WORKFLOW_FILE = "workflowJSONv1.json";

    private ProcessDataAccess processData;

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private LogbookOperationsClientFactory logbookOperationsClientFactory;

    @Mock
    private LogbookOperationsClient logbookOperationsClient;

    @Mock
    private ProcessDistributor processDistributor;

    @Mock
    private IEventsProcessEngine stateMachineCallback;

    @Before
    public void init() throws WorkflowNotFoundException {
        reset(logbookOperationsClient);
        reset(logbookOperationsClientFactory);
        reset(processDistributor);
        reset(stateMachineCallback);

        LogbookOperationsClientFactory.changeMode(null);
        workParams = WorkerParametersFactory.newWorkerParameters();
        workParams.setWorkerGUID(GUIDFactory.newGUID().getId())
            .setUrlMetadata("http://localhost:8083")
            .setUrlWorkspace("http://localhost:8083")
            .setContainerName(GUIDFactory.newGUID().getId())
            .setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_ID).toString())
            .setLogbookTypeProcess(LogbookTypeProcess.INGEST_TEST);

        processData = ProcessDataAccessImpl.getInstance();
        when(logbookOperationsClientFactory.getClient()).thenReturn(logbookOperationsClient);
        processEngine =
            ProcessEngineFactory.get().create(workParams, processDistributor, logbookOperationsClientFactory);
        processEngine.setStateMachineCallback(stateMachineCallback);
    }

    @Test
    @RunWithCustomExecutor
    public void startTestWhenStatusCodeKOThenOK() throws Exception {

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final ProcessWorkflow processWorkflow =
            processData.initProcessWorkflow(populate(WORKFLOW_FILE), workParams.getContainerName()
            );

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        when(processDistributor.distribute(any(), any(), any(), any()))
            .thenReturn(new ItemStatus().increment(StatusCode.KO));

        IEventsProcessEngine iEventsProcessEngine = mock(IEventsProcessEngine.class);
        when(iEventsProcessEngine.getCurrentProcessWorkflowStatus()).thenReturn(StatusCode.KO);
        processEngine.setStateMachineCallback(iEventsProcessEngine);

        ProcessStep step = processWorkflow.getSteps().iterator().next();
        doAnswer(o -> step.setStepStatusCode(StatusCode.KO)).when(iEventsProcessEngine)
            .onProcessEngineCompleteStep(any(), any());
        doAnswer(o -> step.setStepStatusCode(StatusCode.STARTED)).when(iEventsProcessEngine).onUpdate(any());
        processEngine.start(step, workParams, PauseRecover.NO_RECOVER);

        // Because of start is async
        // Sleep to be sur that completableFeature is called in the Engine
        for (int i = 0; i <= 100; i++) {
            if (step.getStepStatusCode() != StatusCode.KO) {
                Thread.sleep(5);
            }
        }

        InOrder inOrders = inOrder(processDistributor, iEventsProcessEngine);
        inOrders.verify(iEventsProcessEngine).onUpdate(any());
        inOrders.verify(processDistributor).distribute(any(), any(), any(), any());
        inOrders.verify(iEventsProcessEngine).onProcessEngineCompleteStep(any(), any());
        Assertions.assertThat(step.getStepStatusCode()).isEqualTo(StatusCode.KO);
    }

    @Test
    @RunWithCustomExecutor
    public void startTestWhenStatusCodeOKThenOK() throws Exception {

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final ProcessWorkflow processWorkflow =
            processData.initProcessWorkflow(populate(WORKFLOW_FILE), workParams.getContainerName()
            );

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        when(processDistributor.distribute(any(), any(), any(), any()))
            .thenReturn(new ItemStatus().increment(StatusCode.OK));

        IEventsProcessEngine iEventsProcessEngine = mock(IEventsProcessEngine.class);
        when(iEventsProcessEngine.getCurrentProcessWorkflowStatus()).thenReturn(StatusCode.OK);
        processEngine.setStateMachineCallback(iEventsProcessEngine);

        ProcessStep step = processWorkflow.getSteps().iterator().next();
        doAnswer(o -> step.setStepStatusCode(StatusCode.OK)).when(iEventsProcessEngine)
            .onProcessEngineCompleteStep(any(), any());
        doAnswer(o -> step.setStepStatusCode(StatusCode.STARTED)).when(iEventsProcessEngine).onUpdate(any());

        processEngine.start(step, workParams, PauseRecover.NO_RECOVER);

        // Because of start is async
        // Sleep to be sur that completableFeature is called in the Engine
        for (int i = 0; i <= 100; i++) {
            if (step.getStepStatusCode() != StatusCode.OK) {
                Thread.sleep(5);
            }
        }

        InOrder inOrders = inOrder(processDistributor, iEventsProcessEngine);
        inOrders.verify(iEventsProcessEngine).onUpdate(any());
        inOrders.verify(processDistributor).distribute(any(), any(), any(), any());
        inOrders.verify(iEventsProcessEngine).onProcessEngineCompleteStep(any(), any());
        Assertions.assertThat(step.getStepStatusCode()).isEqualTo(StatusCode.OK);

    }

    @Test(expected = ProcessingEngineException.class)
    @RunWithCustomExecutor
    public void startTestIEventsProcessEngineRequiredKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final ProcessWorkflow processWorkflow =
            processData.initProcessWorkflow(populate(WORKFLOW_FILE), workParams.getContainerName()
            );
        processEngine.setStateMachineCallback(null);

        processEngine.start(processWorkflow.getSteps().iterator().next(), workParams, PauseRecover.NO_RECOVER);
    }

    @Test
    @RunWithCustomExecutor
    public void test_start_ok() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        final ProcessWorkflow processWorkflow =
            processData.initProcessWorkflow(populate(WORKFLOW_FILE), workParams.getContainerName()
            );

        ItemStatus itemStatus = new ItemStatus("fakeId").increment(StatusCode.OK);

        CountDownLatch countDownLatch = new CountDownLatch(5);

        when(stateMachineCallback.getCurrentProcessWorkflowStatus()).thenReturn(StatusCode.OK);
        when(processDistributor
            .distribute(any(), any(), anyString(), any())).thenReturn(itemStatus);
        doNothing().when(logbookOperationsClient).update(any());

        doAnswer(o ->
        {
            countDownLatch.countDown();
            return o;
        }).when(stateMachineCallback).onUpdate(any());
        doAnswer(o ->
        {
            countDownLatch.countDown();
            return o;
        }).when(stateMachineCallback).onProcessEngineCompleteStep(any(), any());


        doAnswer(o -> {
            countDownLatch.countDown();
            return o;
        }).when(logbookOperationsClient).update(any());
        doAnswer(o -> {
            countDownLatch.countDown();
            return o;
        }).when(logbookOperationsClient).bulkUpdate(anyString(), any());

        processEngine.start(processWorkflow.getSteps().get(0), workParams, PauseRecover.NO_RECOVER);

        countDownLatch.await();

        verify(logbookOperationsClient).update(any());
        verify(logbookOperationsClient).bulkUpdate(anyString(), any());

        verify(stateMachineCallback, times(2)).onUpdate(any());
        verify(stateMachineCallback).onProcessEngineCompleteStep(any(), any());
    }

    @Test(expected = ProcessingEngineException.class)
    @RunWithCustomExecutor
    public void logbookBeforeDistributorCallKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        final ProcessWorkflow processWorkflow =
            processData.initProcessWorkflow(populate(WORKFLOW_FILE), workParams.getContainerName()
            );

        doThrow(new LogbookClientServerException("")).when(logbookOperationsClient).update(any());

        processEngine.start(processWorkflow.getSteps().get(0), workParams, PauseRecover.NO_RECOVER);
    }

    @Test
    @RunWithCustomExecutor
    public void test_when_callDistributor_return_ActionCancel() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        final ProcessWorkflow processWorkflow =
            processData.initProcessWorkflow(populate(WORKFLOW_FILE), workParams.getContainerName()
            );

        ProcessStep processStep = processWorkflow.getSteps().get(0);
        processStep.setPauseOrCancelAction(PauseOrCancelAction.ACTION_CANCEL);

        ItemStatus itemStatus = new ItemStatus("fakeId").increment(StatusCode.OK);

        CountDownLatch countDownLatch = new CountDownLatch(4);

        when(stateMachineCallback.getCurrentProcessWorkflowStatus()).thenReturn(StatusCode.OK);
        when(processDistributor
            .distribute(any(), any(), anyString(), any())).thenReturn(itemStatus);

        doAnswer(o ->
        {
            countDownLatch.countDown();
            return o;
        }).when(stateMachineCallback).onUpdate(any());
        doAnswer(o ->
        {
            countDownLatch.countDown();
            return o;
        }).when(stateMachineCallback).onProcessEngineCancel(any());

        doAnswer(o -> {
            countDownLatch.countDown();
            return o;
        }).when(logbookOperationsClient).update(any());

        processEngine.start(processStep, workParams, PauseRecover.NO_RECOVER);

        countDownLatch.await();

        verify(logbookOperationsClient).update(any());

        verify(stateMachineCallback, times(2)).onUpdate(any());
        verify(stateMachineCallback).onProcessEngineCancel(any());
        verify(stateMachineCallback, times(0)).onProcessEngineCompleteStep(any(), any());
    }

    @Test
    @RunWithCustomExecutor
    public void test_when_callDistributor_return_ActionPause() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        final ProcessWorkflow processWorkflow =
            processData.initProcessWorkflow(populate(WORKFLOW_FILE), workParams.getContainerName()
            );

        ProcessStep processStep = processWorkflow.getSteps().get(0);
        processStep.setPauseOrCancelAction(PauseOrCancelAction.ACTION_PAUSE);

        ItemStatus itemStatus = new ItemStatus("fakeId").increment(StatusCode.OK);
        CountDownLatch countDownLatch = new CountDownLatch(3);

        when(stateMachineCallback.getCurrentProcessWorkflowStatus()).thenReturn(StatusCode.OK);
        when(processDistributor
            .distribute(any(), any(), anyString(), any())).thenReturn(itemStatus);

        doAnswer(o ->
        {
            countDownLatch.countDown();
            return o;
        }).when(stateMachineCallback).onUpdate(any());
        doAnswer(o ->
        {
            countDownLatch.countDown();
            return o;
        }).when(stateMachineCallback).onProcessEngineCompleteStep(any(), any());

        doAnswer(o -> {
            countDownLatch.countDown();
            return o;
        }).when(logbookOperationsClient).update(any());

        processEngine.start(processStep, workParams, PauseRecover.NO_RECOVER);

        countDownLatch.await();

        verify(logbookOperationsClient).update(any());
        verify(stateMachineCallback, times(1)).onUpdate(any());
        verify(stateMachineCallback).onProcessEngineCompleteStep(any(), any());
    }

    @Test
    @RunWithCustomExecutor
    public void test_when_callDistributor_return_FATAL() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        final ProcessWorkflow processWorkflow =
            processData.initProcessWorkflow(populate(WORKFLOW_FILE), workParams.getContainerName()
            );

        ProcessStep processStep = processWorkflow.getSteps().get(0);

        ItemStatus itemStatus = new ItemStatus("fakeId").increment(StatusCode.FATAL);

        CountDownLatch countDownLatch = new CountDownLatch(5);

        when(stateMachineCallback.getCurrentProcessWorkflowStatus()).thenReturn(StatusCode.OK);
        when(processDistributor
            .distribute(any(), any(), anyString(), any())).thenReturn(itemStatus);
        doAnswer(o ->
        {
            countDownLatch.countDown();
            return o;
        }).when(stateMachineCallback).onUpdate(any());
        doAnswer(o ->
        {
            countDownLatch.countDown();
            return o;
        }).when(stateMachineCallback).onProcessEngineCompleteStep(any(), any());


        doAnswer(o -> {
            countDownLatch.countDown();
            return o;
        }).when(logbookOperationsClient).update(any());
        doAnswer(o -> {
            countDownLatch.countDown();
            return o;
        }).when(logbookOperationsClient).bulkUpdate(anyString(), any());

        processEngine.start(processStep, workParams, PauseRecover.NO_RECOVER);

        countDownLatch.await();

        verify(logbookOperationsClient).update(any());
        verify(logbookOperationsClient).bulkUpdate(anyString(), any());

        verify(stateMachineCallback, times(2)).onUpdate(any());
        verify(stateMachineCallback).onProcessEngineCompleteStep(any(), any());
    }

    @Test
    @RunWithCustomExecutor
    public void test_when_logbookAfterDistributorCallKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        final ProcessWorkflow processWorkflow =
            processData.initProcessWorkflow(populate(WORKFLOW_FILE), workParams.getContainerName()
            );

        ItemStatus itemStatus = new ItemStatus("fakeId").increment(StatusCode.OK);

        CountDownLatch countDownLatch = new CountDownLatch(2);
        when(stateMachineCallback.getCurrentProcessWorkflowStatus()).thenReturn(StatusCode.OK);
        when(processDistributor
            .distribute(any(), any(), anyString(), any())).thenReturn(itemStatus);
        doNothing().when(logbookOperationsClient).update(any());
        doAnswer(o ->
        {
            countDownLatch.countDown();
            return o;
        }).when(stateMachineCallback).onUpdate(any());
        doAnswer(o ->
        {
            countDownLatch.countDown();
            return o;
        }).when(stateMachineCallback).onError(any());
        doThrow(new LogbookClientServerException("")).when(logbookOperationsClient).bulkUpdate(any(), any());
        processEngine.start(processWorkflow.getSteps().get(0), workParams, PauseRecover.NO_RECOVER);

        countDownLatch.await();

        verify(logbookOperationsClient).update(any());
        verify(stateMachineCallback, times(1)).onUpdate(any());
        verify(stateMachineCallback).onError(any());
        verify(stateMachineCallback, times(0)).onProcessEngineCompleteStep(any(), any());
    }

    public static WorkFlow populate(String workflowFile) throws FileNotFoundException, InvalidParseOperationException {
        return JsonHandler.getFromInputStream(PropertiesUtils.getConfigAsStream(workflowFile), WorkFlow.class);
    }
}
