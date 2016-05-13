package fr.gouv.vitam.processing.core.distributor;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import fr.gouv.vitam.processing.api.config.ServerConfiguration;
import fr.gouv.vitam.processing.api.model.Action;
import fr.gouv.vitam.processing.api.model.Step;
import fr.gouv.vitam.processing.api.model.WorkFlow;
import fr.gouv.vitam.processing.api.model.WorkParams;
import fr.gouv.vitam.processing.core.utils.ProcessPopulator;

public class ProcessDistributorImplTest {

	private ProcessDistributorImpl processDistributorImpl;
	private WorkParams params;

	@Before
	public void setUp() throws Exception {
		processDistributorImpl = new ProcessDistributorImpl();

		params = new WorkParams().setServerConfiguration(new ServerConfiguration()).setGuuid("aa125487");
	}

	// @Test
	public void should_be_started() {
		WorkFlow flow = ProcessPopulator.populate("workflowJSONv1");
		processDistributorImpl.distribute(params, flow.getSteps().get(0), flow.getId());
	}

	@Test
	public void EngineIllegalArgumentException_should_be_catched() {
		Step step = new Step();
		Action a = new Action();
		a.setActionKey("notExist");

		List<Action> actions = new ArrayList<>();
		actions.add(a);
		step.setActions(actions);
		processDistributorImpl.distribute(params, null, "workflowJSONv1");
	}

	@Test
	public void EngineIllegalArgumentException_actionkey_null_should_be_catched() {
		Step step = new Step();
		Action a = new Action();
		a.setActionKey("notExist");
		List<Action> actions = new ArrayList<>();
		actions.add(a);
		step.setActions(actions);
		processDistributorImpl.distribute(params, step, "workflowJSONv1");
	}
}
