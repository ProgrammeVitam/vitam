package fr.gouv.vitam.processing.distributor.core;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import fr.gouv.vitam.processing.common.config.ServerConfiguration;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.Action;
import fr.gouv.vitam.processing.common.model.Distribution;
import fr.gouv.vitam.processing.common.model.DistributionKind;
import fr.gouv.vitam.processing.common.model.EngineResponse;
import fr.gouv.vitam.processing.common.model.ProcessResponse;
import fr.gouv.vitam.processing.common.model.StatusCode;
import fr.gouv.vitam.processing.common.model.Step;
import fr.gouv.vitam.processing.common.model.WorkParams;
import fr.gouv.vitam.processing.worker.core.WorkerImpl;
import fr.gouv.vitam.processing.worker.handler.ExtractSedaActionHandler;

public class ProcessDistributorImplTest {
    private ProcessDistributorImpl processDistributorImpl;
    private WorkParams params;
    private static final String WORKFLOW_ID = "workflowJSONv1";

    @Before
    public void setUp() throws Exception {
        params = new WorkParams().setServerConfiguration(new ServerConfiguration()).setGuuid("aa125487");
    }

    @Test
    public void givenProcessDistributorWhendistributeThenCatchTheOtherException(){
        processDistributorImpl = new ProcessDistributorImplFactory().create();
        Step step = new Step();
        step.setStepName("Traiter_archives");
        List<Action> actions = new ArrayList<Action>();
        Action action = new Action();
        action.setActionKey(ExtractSedaActionHandler.getId());
        System.out.println(action.getActionKey());
        actions.add(action);
        step.setActions(actions);
        
        processDistributorImpl.distribute(params, step, WORKFLOW_ID);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void givenProcessDistributorWhendistributeThenCatchIllegalArgumentException() {
        processDistributorImpl = new ProcessDistributorImplFactory().create();
        Step step = new Step();
        Action a = new Action();
        a.setActionKey("notExist");
        List<Action> actions = new ArrayList<>();
        actions.add(a);
        step.setActions(actions);
        processDistributorImpl.distribute(params, null, WORKFLOW_ID);
    }

    @Test
    public void givenProcessDistributorWhenDistributeWithListKindThenCatchHandlerNotFoundException() {
        processDistributorImpl = new ProcessDistributorImplFactory().create();
        Step step = new Step().setDistribution(new Distribution().setKind(DistributionKind.LIST));
        Action a = new Action();
        a.setActionKey("notExist");
        List<Action> actions = new ArrayList<>();
        actions.add(a);
        step.setActions(actions);
        processDistributorImpl.distribute(params, step, WORKFLOW_ID);
    }
    
    @Test
    public void givenProcessDistributorWhenDistributeWithRefThenCatchHandlerNotFoundException() {
        processDistributorImpl = new ProcessDistributorImplFactory().create();
        Step step = new Step().setDistribution(new Distribution().setKind(DistributionKind.REF));
        Action a = new Action();
        a.setActionKey("notExist");
        List<Action> actions = new ArrayList<>();
        actions.add(a);
        step.setActions(actions);
        processDistributorImpl.distribute(params, step, WORKFLOW_ID);
    }
    
    @Test
    public void test() throws IllegalArgumentException, ProcessingException{
        WorkerImpl worker = mock(WorkerImpl.class);
        List<EngineResponse> response = new ArrayList<EngineResponse>();
        response.add(new ProcessResponse().setStatus(StatusCode.OK));
        when(worker.run(anyObject(), anyObject())).thenReturn(response);
        
        processDistributorImpl = new ProcessDistributorImplFactory().create(worker);
        processDistributorImpl.distribute(params, new Step(), WORKFLOW_ID);   
    }
}
