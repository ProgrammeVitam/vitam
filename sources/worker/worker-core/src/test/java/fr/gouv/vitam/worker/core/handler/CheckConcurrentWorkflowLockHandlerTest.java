package fr.gouv.vitam.worker.core.handler;

import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.ProcessDetail;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.parameters.Contexts;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.plugin.reclassification.model.ReclassificationEventDetails;
import fr.gouv.vitam.worker.core.utils.LightweightWorkflowLock;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.Collections;

import static fr.gouv.vitam.worker.core.handler.CheckConcurrentWorkflowLockHandler.CONCURRENT_PROCESSES_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

@RunWithCustomExecutor
public class CheckConcurrentWorkflowLockHandlerTest {

    @ClassRule
    public static RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private LightweightWorkflowLock lightweightWorkflowLock;

    @InjectMocks
    private CheckConcurrentWorkflowLockHandler checkConcurrentWorkflowLockHandler;

    @Mock
    private HandlerIO handlerIO;

    @Mock
    private WorkerParameters parameters;

    @Before
    public void init() throws Exception {

        int tenant = 0;
        VitamThreadUtils.getVitamSession().setTenantId(tenant);
        String operationId = GUIDFactory.newRequestIdGUID(tenant).toString();
        VitamThreadUtils.getVitamSession().setRequestId(operationId);

        String objectId = GUIDFactory.newGUID().toString();
        parameters = WorkerParametersFactory.newWorkerParameters().setWorkerGUID(GUIDFactory
            .newGUID()).setContainerName(operationId)
            .setObjectNameList(Lists.newArrayList(objectId))
            .setObjectName(objectId).setCurrentStep("StepName");
    }

    @Test
    public void execute_GivenConcurrentProcessFoundThenExpectKO() throws Exception {

        ProcessDetail concurrentProcessDetail = new ProcessDetail();
        concurrentProcessDetail.setOperationId("SomeOtherProcessId");
        concurrentProcessDetail.setGlobalState("PAUSE");
        concurrentProcessDetail.setStepStatus("FATAL");

        doReturn(Collections.singletonList(concurrentProcessDetail)).when(lightweightWorkflowLock)
            .listConcurrentWorkflows(
                eq(Arrays.asList(Contexts.RECLASSIFICATION.getEventType(), Contexts.ELIMINATION_ACTION.getEventType())),
                eq(VitamThreadUtils.getVitamSession().getRequestId()));

        doReturn("RECLASSIFICATION,ELIMINATION_ACTION").when(handlerIO).getInput(0);

        // When
        ItemStatus itemStatus = checkConcurrentWorkflowLockHandler.execute(parameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(
            JsonHandler.getFromString(itemStatus.getEvDetailData(), ReclassificationEventDetails.class).getError())
            .isEqualTo(CONCURRENT_PROCESSES_FOUND);
    }

    @Test
    public void execute_GivenNoConcurrentProcessFoundThenExpectOK() throws Exception {

        doReturn(Collections.EMPTY_LIST).when(lightweightWorkflowLock)
            .listConcurrentWorkflows(
                eq(Arrays.asList(Contexts.RECLASSIFICATION.getEventType(), Contexts.ELIMINATION_ACTION.getEventType())),
                any());

        doReturn("RECLASSIFICATION,ELIMINATION_ACTION").when(handlerIO).getInput(0);

        // When
        ItemStatus itemStatus = checkConcurrentWorkflowLockHandler.execute(parameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
    }
}
