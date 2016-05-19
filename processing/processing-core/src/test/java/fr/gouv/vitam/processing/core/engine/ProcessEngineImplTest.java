package fr.gouv.vitam.processing.core.engine;

import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

import fr.gouv.vitam.processing.api.config.ServerConfiguration;
import fr.gouv.vitam.processing.api.exception.EngineIllegalArgumentException;
import fr.gouv.vitam.processing.api.exception.WorkflowNotFoundException;
import fr.gouv.vitam.processing.api.model.Response;
import fr.gouv.vitam.processing.api.model.WorkParams;
import fr.gouv.vitam.processing.core.utils.ContainerUtils;

// TODO REVIEW missing licence header
// TODO REVIEW missing javadoc comment
// TODO REVIEW factor the String elements via private static final variable

public class ProcessEngineImplTest {

	ProcessEngineImpl processEngine;
	private WorkParams workParams;

	/**
	 * improves the different elements and objects for test
	 */
	@Before
	public void init() {

		this.processEngine = new ProcessEngineImpl();
		workParams = new WorkParams().setGuuid("hchcdh1255").setServerConfiguration(new ServerConfiguration());
	}

	// @Test
	// TODO REVIEW mock
	public void processEngineTest() {

		workParams.setContainerName(ContainerUtils.generateContainerName());
		workParams.setObjectName("workflowJSONv1.json");
		Response response = processEngine.startProcessByWorkFlowId(workParams, "workflowJSONv1");
		assertNotNull(response);
	}

	@Test(expected = EngineIllegalArgumentException.class)
	public void should_Throws_EngineIllegalArgumentException() {
		processEngine.startProcessByWorkFlowId(workParams, null);

	}

	@Test(expected = WorkflowNotFoundException.class)
	public void should_Throw_WorkflowNotFoundException() {
		processEngine.startProcessByWorkFlowId(workParams, "notExist");

	}

	@Test(expected = EngineIllegalArgumentException.class)
	public void should_Throw_EngineIllegalArguementException() {
		processEngine.startProcessByWorkFlowId(null, "workflowJSONv1");

	}

        // FIXME REVIEW Complete the tests
	@Test
	public void status_response_should_be_fatal() {

	}
}
