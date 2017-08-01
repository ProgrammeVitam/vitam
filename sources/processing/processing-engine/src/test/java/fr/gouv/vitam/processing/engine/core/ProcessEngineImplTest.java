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
package fr.gouv.vitam.processing.engine.core;

import fr.gouv.vitam.common.exception.WorkflowNotFoundException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.processing.common.automation.IEventsProcessEngine;
import fr.gouv.vitam.processing.common.automation.IEventsState;
import fr.gouv.vitam.processing.common.exception.ProcessingEngineException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.PauseRecover;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.processing.common.utils.ProcessPopulator;
import fr.gouv.vitam.processing.data.core.ProcessDataAccess;
import fr.gouv.vitam.processing.data.core.ProcessDataAccessImpl;
import fr.gouv.vitam.processing.distributor.api.ProcessDistributor;
import fr.gouv.vitam.processing.engine.api.ProcessEngine;
import fr.gouv.vitam.processing.engine.core.monitoring.ProcessMonitoringImpl;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InOrder;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Do not forget init method on test method !
 */
@Ignore
public class ProcessEngineImplTest {
    private ProcessEngine processEngine;
    private IEventsState stateMachine;
    private WorkerParameters workParams;
    private ItemStatus response;
    private ProcessMonitoringImpl processMonitoring;
    private ProcessDistributor processDistributor;
    private static final Integer TENANT_ID = 0;
    private static final String WORKFLOW_FILE = "workflowJSONv1.json";
    private static final String WORKFLOW_WITH_FINALLY_STEP = "workflowJSONFinallyStep.json";

    private ProcessDataAccess processData;

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @Before
    public void init() throws WorkflowNotFoundException, ProcessingException {
        workParams = WorkerParametersFactory.newWorkerParameters();
        workParams.setWorkerGUID(GUIDFactory.newGUID())
            .setUrlMetadata("http://localhost:8083")
            .setUrlWorkspace("http://localhost:8083")
            .setContainerName(GUIDFactory.newGUID().getId())
            .setLogbookTypeProcess(LogbookTypeProcess.INGEST_TEST);

        processDistributor = mock(ProcessDistributor.class);
        processMonitoring = ProcessMonitoringImpl.getInstance();

        processData = ProcessDataAccessImpl.getInstance();
        processEngine =
            ProcessEngineFactory.get().create(workParams, processDistributor);
    }

    @Test(expected = IllegalArgumentException.class)
    public void pauseTestParamRequiredKO() throws Exception {
        processEngine.pause(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void cancelTestParamRequiredKO() throws Exception {
        processEngine.cancel(null);
    }


    @Test
    public void pauseTestOK() throws Exception {
        processEngine.pause("fakeOperationId");
        verify(processDistributor).pause(anyString());
    }

    @Test
    public void cancelTestOK() throws Exception {
        processEngine.cancel("fakeOperationId");
        verify(processDistributor).cancel(anyString());
    }

    @Test
    @RunWithCustomExecutor
    public void startTestKO() throws Exception {

        final ProcessWorkflow processWorkflow =
            processData.initProcessWorkflow(ProcessPopulator.populate(WORKFLOW_FILE), workParams.getContainerName(),
                LogbookTypeProcess.INGEST, TENANT_ID);

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        when(processDistributor.distribute(anyObject(), anyObject(), anyObject(), anyObject()))
            .thenReturn(new ItemStatus().increment(StatusCode.OK));

        IEventsProcessEngine iEventsProcessEngine = mock(IEventsProcessEngine.class);
        processEngine.setCallback(iEventsProcessEngine);
        processEngine.start(processWorkflow.getSteps().iterator().next(), workParams, null, PauseRecover.NO_RECOVER);
        InOrder inOrders = inOrder(processDistributor, iEventsProcessEngine);
        inOrders.verify(iEventsProcessEngine).onUpdate(anyObject());
        inOrders.verify(processDistributor).distribute(anyObject(), anyObject(), anyObject(), anyObject());
        inOrders.verify(iEventsProcessEngine).onComplete(anyObject(), anyObject());
    }

    @Test
    @RunWithCustomExecutor
    public void startTestOK() throws Exception {

        final ProcessWorkflow processWorkflow =
            processData.initProcessWorkflow(ProcessPopulator.populate(WORKFLOW_FILE), workParams.getContainerName(),
                LogbookTypeProcess.INGEST, TENANT_ID);

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        when(processDistributor.distribute(anyObject(), anyObject(), anyObject(), anyObject()))
            .thenReturn(new ItemStatus().increment(StatusCode.OK));

        IEventsProcessEngine iEventsProcessEngine = mock(IEventsProcessEngine.class);
        processEngine.setCallback(iEventsProcessEngine);
        processEngine.start(processWorkflow.getSteps().iterator().next(), workParams, null, PauseRecover.NO_RECOVER);

        // Because of start is async
        // Sleep to be sur that completableFeature is called in the Engine
        Thread.sleep(5);

        InOrder inOrders = inOrder(processDistributor, iEventsProcessEngine);
        inOrders.verify(iEventsProcessEngine).onUpdate(anyObject());
        inOrders.verify(processDistributor).distribute(anyObject(), anyObject(), anyObject(), anyObject());
        inOrders.verify(iEventsProcessEngine).onComplete(anyObject(), anyObject());
    }

    @Test(expected = ProcessingEngineException.class)
    @RunWithCustomExecutor
    public void startTestIEventsProcessEngineRequiredKO() throws Exception {
        final ProcessWorkflow processWorkflow =
            processData.initProcessWorkflow(ProcessPopulator.populate(WORKFLOW_FILE), workParams.getContainerName(),
                LogbookTypeProcess.INGEST, TENANT_ID);
        processEngine.start(processWorkflow.getSteps().iterator().next(), workParams, null, PauseRecover.NO_RECOVER);
    }
}
