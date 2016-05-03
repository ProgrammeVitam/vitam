/**
 * 
 */
package fr.gouv.vitam.processing.core.handler;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import fr.gouv.vitam.processing.api.Response;
import fr.gouv.vitam.processing.beans.Process;
import fr.gouv.vitam.processing.beans.WorkParams;
import fr.gouv.vitam.processing.constants.StatusCode;
import fr.gouv.vitam.utils.ContainerUtils;
import fr.gouv.vitam.processing.core.handler.StoreActionHandler;

/**
 * @author RLazreg
 *
 */
public class StoreActionHandlerTest {

	private Process process;
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

	@Test
	public void test1_shoutPutObjectInWorkspace() {

		StoreActionHandler actionHandler = new StoreActionHandler();
		Response response = actionHandler.execute(process, params);
		assertTrue(StatusCode.OK == response.getStatus());
	}

}
