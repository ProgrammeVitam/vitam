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
package fr.gouv.vitam.processing.management.core;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import fr.gouv.vitam.common.exception.WorkflowNotFoundException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.ProcessExecutionStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.processing.common.config.ServerConfiguration;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.Action;
import fr.gouv.vitam.processing.common.model.ActionDefinition;
import fr.gouv.vitam.processing.common.model.Distribution;
import fr.gouv.vitam.processing.common.model.DistributionKind;
import fr.gouv.vitam.processing.common.model.IOParameter;
import fr.gouv.vitam.processing.common.model.ProcessBehavior;
import fr.gouv.vitam.processing.common.model.ProcessStep;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.processing.common.model.ProcessingUri;
import fr.gouv.vitam.processing.common.model.Step;
import fr.gouv.vitam.processing.common.model.UriPrefix;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.processing.data.core.management.ProcessDataManagement;
import fr.gouv.vitam.processing.data.core.management.WorkspaceProcessDataManagement;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.net.ssl.*"})
@PrepareForTest({WorkspaceProcessDataManagement.class})
public class ProcessManagementImplTest {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProcessManagementImplTest.class);

    private ProcessManagementImpl processManagementImpl;
    private static final Integer TENANT_ID = 0;
    private static final String CONTAINER_NAME = "container1";
    private static final String ID = "id1";
    private static ProcessDataManagement processDataManagement;

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @Before
    public void setup() {
        PowerMockito.mockStatic(WorkspaceProcessDataManagement.class);
        processDataManagement = Mockito.mock(WorkspaceProcessDataManagement.class);
        PowerMockito.when(WorkspaceProcessDataManagement.getInstance()).thenReturn(processDataManagement);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenProcessingManagementWhenWorkflowIsNullThenThrowIllegalArgumentException()
        throws ProcessingException {
        Mockito.reset(processDataManagement);
        Mockito.doReturn(new HashMap<String, List<Object>>()).when(processDataManagement).getProcessWorkflowFor
            (Matchers.anyInt(), Matchers.anyString());
        processManagementImpl = new ProcessManagementImpl(new ServerConfiguration());
        processManagementImpl.submitWorkflow(WorkerParametersFactory.newWorkerParameters(), CONTAINER_NAME,
            ProcessAction.RESUME, null, TENANT_ID);
    }

    @Test(expected = WorkflowNotFoundException.class)
    @RunWithCustomExecutor
    public void testSubmitWorkFlow() throws ProcessingException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        Mockito.reset(processDataManagement);
        Mockito.when(processDataManagement.getProcessWorkflowFor(Matchers.anyInt(), Matchers.anyString())).thenReturn
            (new HashMap<>());
        processManagementImpl =
            new ProcessManagementImpl(new ServerConfiguration());
        processManagementImpl.submitWorkflow(
            WorkerParametersFactory.newWorkerParameters(ID, ID, CONTAINER_NAME, ID, ID,
                "http://localhost:8083",
                "http://localhost:8083"),
            "XXX",
            ProcessAction.RESUME, null, TENANT_ID);
    }

    @RunWithCustomExecutor
    @Test
    public void loadNoPersistedWorkflowTest() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        // No persisted Workflow
        Mockito.reset(processDataManagement);
        Mockito.when(processDataManagement.getProcessWorkflowFor(Matchers.anyInt(), Matchers.anyString()))
            .thenReturn(new HashMap<>());
        processManagementImpl = new ProcessManagementImpl(new ServerConfiguration());
        Assert.assertNotNull(processManagementImpl);
        List<ProcessWorkflow> processWorkflowList = processManagementImpl.getAllWorkflowProcess(TENANT_ID);
        Assert.assertNotNull(processWorkflowList);
        Assert.assertTrue(processWorkflowList.isEmpty());
    }

    @RunWithCustomExecutor
    @Test
    public void loadPersitedPausedWorkflowTest() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        Mockito.reset(processDataManagement);
        Mockito.when(processDataManagement.getProcessWorkflowFor(Matchers.anyInt(), Matchers.anyString()))
            .thenReturn(getPausedWorkflowList());

        ServerConfiguration serverConfiguration = new ServerConfiguration();
        serverConfiguration.setUrlMetadata("fakeurl:1111");
        serverConfiguration.setUrlWorkspace("fakeurl:1112");
        processManagementImpl = new ProcessManagementImpl(serverConfiguration);
        Assert.assertNotNull(processManagementImpl);
        List<ProcessWorkflow> processWorkflowList = processManagementImpl.getAllWorkflowProcess(TENANT_ID);
        Assert.assertNotNull(processWorkflowList);
        Assert.assertFalse(processWorkflowList.isEmpty());
    }

    private Map<String, ProcessWorkflow> getPausedWorkflowList() {
        Map<String, ProcessWorkflow> result = new HashMap<>();
        ProcessWorkflow processWorkflow = new ProcessWorkflow();
        processWorkflow.setTenantId(0);
        processWorkflow.setExecutionMode(ProcessAction.PAUSE);
        processWorkflow.setExecutionStatus(ProcessExecutionStatus.PAUSE);
        processWorkflow.setGlobalStatusCode(StatusCode.OK);
        processWorkflow.setLogbookTypeProcess(LogbookTypeProcess.INGEST);
        processWorkflow.setMessageIdentifier("MessageIdentifier");
        processWorkflow.setOperationId("operationId");
        Map<String, ProcessStep> mapProcessStep = new HashMap<>();
        for(int i = 0; i < 20; i++) {
            mapProcessStep.put("key-map-" + i, getProcessStep("name-" + i, "element-" + i, "groupID-" + i));
        }
        processWorkflow.setOrderedProcessStep(mapProcessStep);
        processWorkflow.setProcessDate(new Date());

        result.put(processWorkflow.getOperationId(), processWorkflow);
        return result;
    }

    private List<Action> getActions() {
        List<Action> actionsList = new ArrayList<>();
        for (int j = 0; j < 10; j++) {
            Action action = new Action();
            ActionDefinition actionDefinition = new ActionDefinition();
            actionDefinition.setBehavior(j % 2 == 0 ? ProcessBehavior.BLOCKING: ProcessBehavior.NOBLOCKING);
            actionDefinition.setActionKey("actionKey-" + j);
            List<IOParameter> in = new ArrayList<>();
            List<IOParameter> out = new ArrayList<>();
            for (int i = 0; i < 15; i++) {
                IOParameter ioParameter = new IOParameter();
                ioParameter.setName("io-name-" + i);
                ioParameter.setOptional(false);
                ProcessingUri processingUri = new ProcessingUri(UriPrefix.WORKSPACE, "path-" + i);
                ioParameter.setUri(processingUri);
                in.add(ioParameter);
                out.add(ioParameter);
            }
            actionDefinition.setIn(in);
            actionDefinition.setOut(out);
            action.setActionDefinition(actionDefinition);
            actionsList.add(action);
        }
        return actionsList;
    }

    private ProcessStep getProcessStep(String name, String element, String groupId) {
        Step step = new Step();
        step.setActions(getActions());
        step.setBehavior(ProcessBehavior.NOBLOCKING);
        Distribution distrib = new Distribution();
        distrib.setElement(element);
        distrib.setKind(DistributionKind.LIST);
        step.setDistribution(distrib);
        step.setStepName(name);
        ItemStatus itemStatus = new ItemStatus();
        itemStatus.setMessage("message");
        itemStatus.setItemId("itemId");
        itemStatus.setEvDetailData("evDetailData");
        itemStatus.setGlobalExecutionStatus(ProcessExecutionStatus.PAUSE);
        step.setStepResponses(itemStatus);
        step.setWorkerGroupId(groupId);
        ProcessStep ps = new ProcessStep(step, 0, 0, "id");
        ps.setStepStatusCode(StatusCode.UNKNOWN);
        return ps;
    }

}
