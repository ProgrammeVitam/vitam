/**
 * 
 */
package fr.gouv.vitam.processing.core.handler;

import org.junit.Before;

import fr.gouv.vitam.processing.api.model.WorkParams;
import fr.gouv.vitam.processing.core.utils.ContainerUtils;

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

	public void test1_shoutPutObjectInWorkspace() {

		StoreInWorkspaceActionHandler actionHandler = new StoreInWorkspaceActionHandler();

	}

}
