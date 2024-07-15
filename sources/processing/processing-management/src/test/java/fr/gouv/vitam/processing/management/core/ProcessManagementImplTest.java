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
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.exception.StateNotAllowedException;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessQuery;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.Action;
import fr.gouv.vitam.common.model.processing.ActionDefinition;
import fr.gouv.vitam.common.model.processing.Distribution;
import fr.gouv.vitam.common.model.processing.DistributionKind;
import fr.gouv.vitam.common.model.processing.IOParameter;
import fr.gouv.vitam.common.model.processing.ProcessBehavior;
import fr.gouv.vitam.common.model.processing.ProcessDetail;
import fr.gouv.vitam.common.model.processing.ProcessingUri;
import fr.gouv.vitam.common.model.processing.Step;
import fr.gouv.vitam.common.model.processing.UriPrefix;
import fr.gouv.vitam.common.model.processing.WorkFlow;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.config.ServerConfiguration;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.ProcessStep;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.processing.data.core.ProcessDataAccessImpl;
import fr.gouv.vitam.processing.data.core.management.WorkspaceProcessDataManagement;
import fr.gouv.vitam.processing.distributor.api.IWorkerManager;
import fr.gouv.vitam.processing.distributor.api.ProcessDistributor;
import fr.gouv.vitam.processing.distributor.core.AsyncResourceCleaner;
import fr.gouv.vitam.processing.distributor.core.AsyncResourcesMonitor;
import fr.gouv.vitam.processing.distributor.core.ProcessDistributorImpl;
import fr.gouv.vitam.processing.distributor.core.WorkerManager;
import fr.gouv.vitam.processing.engine.core.operation.OperationContextException;
import fr.gouv.vitam.processing.engine.core.operation.OperationContextMonitor;
import fr.gouv.vitam.worker.client.WorkerClient;
import fr.gouv.vitam.worker.client.WorkerClientFactory;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static fr.gouv.vitam.logbook.common.parameters.Contexts.DEFAULT_WORKFLOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ProcessManagementImplTest {

    private ProcessManagementImpl processManagementImpl;
    private static final String CONTAINER_NAME = "container1";
    private static final String ID = "id1";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private ProcessDataAccessImpl processDataAccess;

    @Mock
    private WorkspaceClientFactory workspaceClientFactory;

    @Mock
    private WorkspaceClient workspaceClient;

    @Mock
    private MetaDataClient metaDataClient;

    @Mock
    private WorkspaceProcessDataManagement processDataManagement;

    @Mock
    private OperationContextMonitor operationContextMonitor;

    @Mock
    private AsyncResourceCleaner asyncResourceCleaner;

    @Mock
    private AsyncResourcesMonitor asyncResourcesMonitor;

    private IWorkerManager workerManager = null;
    private ProcessDistributor processDistributor = null;

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Before
    public void setup() {
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);
        WorkerClientFactory workerClientFactory = mock(WorkerClientFactory.class);
        MetaDataClientFactory metaDataClientFactory = mock(MetaDataClientFactory.class);
        WorkerClient workerClient = mock(WorkerClient.class);
        when(workerClientFactory.getClient()).thenReturn(workerClient);
        when(metaDataClientFactory.getClient()).thenReturn(metaDataClient);
        workerManager = new WorkerManager(workerClientFactory);
        ServerConfiguration configuration = new ServerConfiguration();
        when(processDataAccess.getWorkFlowList()).thenReturn(new ConcurrentHashMap<>());
        processDistributor = new ProcessDistributorImpl(
            workerManager,
            asyncResourcesMonitor,
            asyncResourceCleaner,
            configuration,
            processDataManagement,
            workspaceClientFactory,
            metaDataClientFactory,
            workerClientFactory
        );
    }

    @Test(expected = StateNotAllowedException.class)
    @RunWithCustomExecutor
    public void testResumeNotInitiatedWorkflow() throws ProcessingException, StateNotAllowedException {
        VitamThreadUtils.getVitamSession().setTenantId(1);

        when(processDataManagement.getProcessWorkflowFor(eq(1), anyString())).thenReturn(new HashMap<>());
        processManagementImpl = new ProcessManagementImpl(
            new ServerConfiguration(),
            processDistributor,
            processDataAccess,
            processDataManagement,
            operationContextMonitor
        );
        processManagementImpl.resume(
            WorkerParametersFactory.newWorkerParameters(
                ID,
                ID,
                "NotExistsContainer",
                ID,
                Lists.newArrayList(ID),
                "http://localhost:8083",
                "http://localhost:8083"
            ),
            1,
            false
        );
    }

    @RunWithCustomExecutor
    @Test
    public void loadNoPersistedWorkflowTest() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(2);
        // No persisted Workflow
        verifyNoMoreInteractions(processDataManagement);
        when(processDataManagement.getProcessWorkflowFor(eq(2), anyString())).thenReturn(new HashMap<>());
        processManagementImpl = new ProcessManagementImpl(
            new ServerConfiguration(),
            processDistributor,
            processDataAccess,
            processDataManagement,
            operationContextMonitor
        );
        Assert.assertNotNull(processManagementImpl);
        List<ProcessWorkflow> processWorkflowList = processManagementImpl.findAllProcessWorkflow(2);
        Assert.assertNotNull(processWorkflowList);
        Assert.assertTrue(processWorkflowList.isEmpty());
    }

    @RunWithCustomExecutor
    @Test
    public void whenOperationContextExceptionThenSilentlyContinue() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(2);
        // No persisted Workflow
        verifyNoMoreInteractions(processDataManagement);

        doThrow(OperationContextException.class).when(operationContextMonitor).backup(anyString(), anyString(), any());
        when(processDataAccess.initProcessWorkflow(any(), anyString())).thenReturn(
            getPausedWorkflowList(1).iterator().next()
        );
        processManagementImpl = new ProcessManagementImpl(
            new ServerConfiguration(),
            processDistributor,
            processDataAccess,
            processDataManagement,
            operationContextMonitor
        );
        Assert.assertNotNull(processManagementImpl);
        ProcessWorkflow wf = processManagementImpl.init(
            WorkerParametersFactory.newWorkerParameters(
                ID,
                ID,
                CONTAINER_NAME,
                ID,
                Lists.newArrayList(ID),
                "http://localhost:8083",
                "http://localhost:8083"
            ),
            DEFAULT_WORKFLOW.name()
        );
        assertThat(wf).isNotNull();
    }

    @RunWithCustomExecutor
    @Test
    public void loadPersistedPausedWorkflowTest() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(3);

        when(processDataAccess.findAllProcessWorkflow(eq(3))).thenReturn(getPausedWorkflowList(3));

        ServerConfiguration serverConfiguration = new ServerConfiguration();
        serverConfiguration.setUrlMetadata("fakeurl:1111");
        serverConfiguration.setUrlWorkspace("fakeurl:1112");
        processManagementImpl = new ProcessManagementImpl(
            serverConfiguration,
            processDistributor,
            processDataAccess,
            processDataManagement,
            operationContextMonitor
        );
        Assert.assertNotNull(processManagementImpl);
        List<ProcessWorkflow> processWorkflowList = processManagementImpl.findAllProcessWorkflow(3);
        Assert.assertNotNull(processWorkflowList);
        Assert.assertFalse(processWorkflowList.isEmpty());
        Map<String, WorkFlow> workflowDefinitions = processManagementImpl.getWorkflowDefinitions();
        Assert.assertNotNull(workflowDefinitions);
        Assert.assertNotNull(workflowDefinitions.get("FILING_SCHEME"));
        Assert.assertNotNull(workflowDefinitions.get("DEFAULT_WORKFLOW"));
        Assert.assertEquals("FILINGSCHEME", workflowDefinitions.get("FILING_SCHEME").getIdentifier());
        Assert.assertEquals(13, workflowDefinitions.get("DEFAULT_WORKFLOW").getSteps().size());
        Assert.assertEquals(4, workflowDefinitions.get("DEFAULT_WORKFLOW").getSteps().get(4).getActions().size());
        Assert.assertEquals(
            "CHECK_UNIT_SCHEMA",
            workflowDefinitions
                .get("DEFAULT_WORKFLOW")
                .getSteps()
                .get(4)
                .getActions()
                .get(0)
                .getActionDefinition()
                .getActionKey()
        );
    }

    @Test
    public void getFilteredProcessTest() throws Exception {
        ServerConfiguration serverConfiguration = new ServerConfiguration();
        serverConfiguration.setUrlMetadata("fakeurl:1111");
        serverConfiguration.setUrlWorkspace("fakeurl:1112");

        when(processDataAccess.findAllProcessWorkflow(eq(0))).thenReturn(getPausedWorkflowList(5));

        processManagementImpl = new ProcessManagementImpl(
            serverConfiguration,
            processDistributor,
            processDataAccess,
            processDataManagement,
            operationContextMonitor
        );
        Assert.assertNotNull(processManagementImpl);

        ProcessQuery pq = new ProcessQuery();

        List<ProcessDetail> results = processManagementImpl.getFilteredProcess(pq, 0);
        assertThat(results).hasSize(5);

        LocalDate date = LocalDate.now(ZoneOffset.UTC);
        LocalDate dateMax = LocalDate.now(ZoneOffset.UTC);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        pq.setStartDateMin(date.format(formatter));
        pq.setStartDateMax(dateMax.format(formatter));
        results = processManagementImpl.getFilteredProcess(pq, 0);
        Assert.assertEquals(1, results.size());

        dateMax = dateMax.plusDays(1);
        pq.setStartDateMax(dateMax.format(formatter));
        results = processManagementImpl.getFilteredProcess(pq, 0);
        Assert.assertEquals(2, results.size());

        dateMax = dateMax.plusDays(2);
        pq.setStartDateMax(dateMax.format(formatter));
        results = processManagementImpl.getFilteredProcess(pq, 0);
        Assert.assertEquals(4, results.size());

        dateMax = dateMax.plusDays(1);
        pq.setStartDateMax(dateMax.format(formatter));
        results = processManagementImpl.getFilteredProcess(pq, 0);
        Assert.assertEquals(5, results.size());

        date = date.plusDays(1);
        pq.setStartDateMin(date.format(formatter));
        results = processManagementImpl.getFilteredProcess(pq, 0);
        Assert.assertEquals(4, results.size());

        pq.setStartDateMin(null);
        pq.setStartDateMax(null);
        List<String> list = new ArrayList<>();
        list.add(StatusCode.OK.name());
        pq.setStatuses(list);
        results = processManagementImpl.getFilteredProcess(pq, 0);
        Assert.assertEquals(5, results.size());

        list.clear();
        list.add(StatusCode.KO.name());
        pq.setStatuses(list);
        results = processManagementImpl.getFilteredProcess(pq, 0);
        Assert.assertEquals(0, results.size());
        list.add(StatusCode.OK.name());
        pq.setStatuses(list);
        results = processManagementImpl.getFilteredProcess(pq, 0);
        Assert.assertEquals(5, results.size());

        pq.setStatuses(null);
        pq.setId("operationId0");
        results = processManagementImpl.getFilteredProcess(pq, 0);
        Assert.assertEquals(1, results.size());

        pq.setId(null);
        list.clear();
        list.add(ProcessState.PAUSE.name());
        pq.setStates(list);
        results = processManagementImpl.getFilteredProcess(pq, 0);
        Assert.assertEquals(5, results.size());

        list.clear();
        list.add(ProcessState.RUNNING.name());
        pq.setStates(list);
        results = processManagementImpl.getFilteredProcess(pq, 0);
        Assert.assertEquals(0, results.size());
        list.add(ProcessState.PAUSE.name());
        pq.setStates(list);
        results = processManagementImpl.getFilteredProcess(pq, 0);
        Assert.assertEquals(5, results.size());
    }

    private List<ProcessWorkflow> getPausedWorkflowList(int nbProcess) {
        List<ProcessWorkflow> list = new ArrayList<>();
        LocalDate date = LocalDate.now(ZoneOffset.UTC);
        for (int j = 0; j < nbProcess; j++) {
            ProcessWorkflow processWorkflow = new ProcessWorkflow();
            processWorkflow.setTenantId(3);
            processWorkflow.setState(ProcessState.PAUSE);
            processWorkflow.setStatus(StatusCode.OK);
            processWorkflow.setLogbookTypeProcess(LogbookTypeProcess.INGEST);
            processWorkflow.setMessageIdentifier("MessageIdentifier");
            processWorkflow.setOperationId("operationId" + j);
            for (int i = 0; i < 20; i++) {
                processWorkflow
                    .getSteps()
                    .add(getProcessStep("key-map-" + i, "name-" + i, "element-" + i, "groupID-" + i));
            }
            date = date.plusDays(j == 0 ? 0 : 1);
            processWorkflow.setProcessDate(LocalDateUtil.getFormattedDateTimeForMongo(date.atStartOfDay()));
            list.add(processWorkflow);
        }
        return list;
    }

    private List<Action> getActions() {
        List<Action> actionsList = new ArrayList<>();
        for (int j = 0; j < 10; j++) {
            Action action = new Action();
            ActionDefinition actionDefinition = new ActionDefinition();
            actionDefinition.setBehavior(j % 2 == 0 ? ProcessBehavior.BLOCKING : ProcessBehavior.NOBLOCKING);
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

    private ProcessStep getProcessStep(String id, String name, String element, String groupId) {
        Step step = new Step();
        step.setId(id);
        step.setActions(getActions());
        step.setBehavior(ProcessBehavior.NOBLOCKING);
        Distribution distrib = new Distribution();
        distrib.setElement(element);
        distrib.setKind(DistributionKind.LIST_ORDERING_IN_FILE);
        step.setDistribution(distrib);
        step.setStepName(name);
        ItemStatus itemStatus = new ItemStatus();
        itemStatus.setMessage("message");
        itemStatus.setItemId("itemId");
        itemStatus.setGlobalState(ProcessState.COMPLETED);
        step.setStepResponses(itemStatus);
        step.setWorkerGroupId(groupId);
        ProcessStep ps = new ProcessStep(step, new AtomicLong(0), new AtomicLong(0), "id");
        ps.setStepStatusCode(StatusCode.OK);
        return ps;
    }
}
