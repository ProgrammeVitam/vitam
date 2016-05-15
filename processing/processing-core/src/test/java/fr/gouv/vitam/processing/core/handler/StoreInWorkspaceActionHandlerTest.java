/**
 * 
 */
package fr.gouv.vitam.processing.core.handler;

import org.junit.Before;

import fr.gouv.vitam.processing.api.model.WorkParams;
import fr.gouv.vitam.processing.core.utils.ContainerUtils;

// TODO REVIEW missing licence header
// TODO REVIEW missing javadoc comment
// TODO REVIEW factor the String elements via private static final variable


/**
 * 
 *
 */
public class StoreInWorkspaceActionHandlerTest {

	private WorkParams params;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		params = new WorkParams();
		params.setContainerName(ContainerUtils.generateContainerName());
		params.setObjectName("workflowJSONv1.json");

	}
	// FIXME REVIEW Not a test

	public void test1_shoutPutObjectInWorkspace() {

		StoreInWorkspaceActionHandler actionHandler = new StoreInWorkspaceActionHandler();

	}

}
