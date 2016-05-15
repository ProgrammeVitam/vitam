package fr.gouv.vitam.processing.core.utils;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;

import fr.gouv.vitam.processing.api.model.WorkFlow;

// TODO REVIEW missing licence header
// TODO REVIEW missing javadoc comment

public class ProcessPopulatorTest {

	@Before
	public void setUp() throws Exception {
	}

	// @Test
	public void should_generate_workflow_object() {
                // TODO REVIEW factor the String elements via private static final variable
		WorkFlow workFlow = ProcessPopulator.populate("workflowJSONv1");
		assertNotNull(workFlow);
		assertNotNull(workFlow.getSteps());
		assertTrue(workFlow.getSteps().size() > 1);
		assertNotNull(workFlow.getSteps().get(0).getActions());
	}
	// FIXME REVIEW and a test with error

}
