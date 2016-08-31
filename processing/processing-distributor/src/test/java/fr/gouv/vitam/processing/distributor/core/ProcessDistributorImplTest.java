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
package fr.gouv.vitam.processing.distributor.core;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import fr.gouv.vitam.processing.common.config.ServerConfiguration;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.Action;
import fr.gouv.vitam.processing.common.model.ActionDefinition;
import fr.gouv.vitam.processing.common.model.ActionType;
import fr.gouv.vitam.processing.common.model.Distribution;
import fr.gouv.vitam.processing.common.model.DistributionKind;
import fr.gouv.vitam.processing.common.model.EngineResponse;
import fr.gouv.vitam.processing.common.model.ProcessResponse;
import fr.gouv.vitam.processing.common.model.ProcessStep;
import fr.gouv.vitam.processing.common.model.StatusCode;
import fr.gouv.vitam.processing.common.model.Step;
import fr.gouv.vitam.processing.common.model.StepType;
import fr.gouv.vitam.processing.common.model.WorkFlow;
import fr.gouv.vitam.processing.common.model.WorkParams;
import fr.gouv.vitam.processing.engine.core.monitoring.ProcessMonitoringImpl;
import fr.gouv.vitam.processing.worker.core.WorkerImpl;
import fr.gouv.vitam.processing.worker.handler.ExtractSedaActionHandler;

public class ProcessDistributorImplTest {
    private ProcessDistributorImpl processDistributorImpl;
    private WorkParams params;
    private static final String WORKFLOW_ID = "workflowJSONv1";
    private ProcessMonitoringImpl processMonitoring;
    private WorkFlow worfklow;

    @Before
    public void setUp() throws Exception {
        params = new WorkParams().setServerConfiguration(new ServerConfiguration()).setGuuid("aa125487");
        processMonitoring = ProcessMonitoringImpl.getInstance();
        final List<Step> steps = new ArrayList<>();
        steps.add(new Step().setStepName("TEST"));
        worfklow = new WorkFlow().setSteps(steps).setId(WORKFLOW_ID);
        //set process_id and step_id (set in the engine)
        params.setAdditionalProperty(WorkParams.PROCESS_ID, WorkParams.PROCESS_ID);        
        Map<String, ProcessStep> processSteps = processMonitoring.initOrderedWorkflow(WorkParams.PROCESS_ID, worfklow, "containerName");
        for (Map.Entry<String, ProcessStep> entry : processSteps.entrySet()) {
            params.setAdditionalProperty(WorkParams.STEP_ID, entry.getKey());
        }
    }

    @Test
    public void givenProcessDistributorWhendistributeThenCatchTheOtherException() {
        processDistributorImpl = new ProcessDistributorImplFactory().create();
        final Step step = new Step();
        step.setStepName("Traiter_archives");
        step.setStepType(StepType.BLOCK);
        final List<Action> actions = new ArrayList<Action>();
        final Action action = new Action();
        final ActionDefinition actionDefinition = new ActionDefinition();
        actionDefinition.setActionKey(ExtractSedaActionHandler.getId());
        actionDefinition.setActionType(ActionType.NOBLOCK);
        action.setActionDefinition(actionDefinition);
        actions.add(action);
        step.setActions(actions);

        processDistributorImpl.distribute(params, step, WORKFLOW_ID);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenProcessDistributorWhendistributeThenCatchIllegalArgumentException() {
        processDistributorImpl = new ProcessDistributorImplFactory().create();
        final Step step = new Step();
        final Action a = new Action();
        final ActionDefinition actionDefinition = new ActionDefinition();
        actionDefinition.setActionKey("notExist");
        actionDefinition.setActionType(ActionType.NOBLOCK);
        a.setActionDefinition(actionDefinition);
        final List<Action> actions = new ArrayList<>();
        actions.add(a);
        step.setActions(actions);
        processDistributorImpl.distribute(params, null, WORKFLOW_ID);
    }

    @Test
    public void givenProcessDistributorWhenDistributeWithListKindThenCatchHandlerNotFoundException() {
        processDistributorImpl = new ProcessDistributorImplFactory().create();
        final Step step = new Step().setDistribution(new Distribution().setKind(DistributionKind.LIST));
        final Action a = new Action();
        final ActionDefinition actionDefinition = new ActionDefinition();
        actionDefinition.setActionKey("notExist");
        actionDefinition.setActionType(ActionType.NOBLOCK);
        a.setActionDefinition(actionDefinition);
        final List<Action> actions = new ArrayList<>();
        actions.add(a);
        step.setActions(actions);
        processDistributorImpl.distribute(params, step, WORKFLOW_ID);
    }

    @Test
    public void givenProcessDistributorWhenDistributeWithRefThenCatchHandlerNotFoundException() {
        processDistributorImpl = new ProcessDistributorImplFactory().create();
        final Step step = new Step().setDistribution(new Distribution().setKind(DistributionKind.REF));
        final Action a = new Action();
        final ActionDefinition actionDefinition = new ActionDefinition();
        actionDefinition.setActionKey("notExist");
        actionDefinition.setActionType(ActionType.NOBLOCK);
        a.setActionDefinition(actionDefinition);
        final List<Action> actions = new ArrayList<>();
        actions.add(a);
        step.setActions(actions);
        processDistributorImpl.distribute(params, step, WORKFLOW_ID);
    }

    @Test
    public void test() throws IllegalArgumentException, ProcessingException {
        final WorkerImpl worker = mock(WorkerImpl.class);
        final List<EngineResponse> response = new ArrayList<EngineResponse>();
        response.add(new ProcessResponse().setStatus(StatusCode.OK));
        when(worker.run(anyObject(), anyObject())).thenReturn(response);

        processDistributorImpl = new ProcessDistributorImplFactory().create(worker);
        processDistributorImpl.distribute(params, worfklow.getSteps().get(0), WORKFLOW_ID);
        
        // checkMonitoring
        String processId = (String) params.getAdditionalProperties().get(WorkParams.PROCESS_ID);
        Map<String, ProcessStep> map = processMonitoring.getWorkflowStatus(processId);
        assertNotNull(map);
        // At least one element has been processed
        for (Map.Entry<String, ProcessStep> entry : map.entrySet()) {
            assertTrue(entry.getValue().getElementProcessed() > 0);
        }
        
    }
}
