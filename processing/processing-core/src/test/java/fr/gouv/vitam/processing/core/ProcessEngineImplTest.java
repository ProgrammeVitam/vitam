package fr.gouv.vitam.processing.core;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import fr.gouv.vitam.processing.api.Response;
import fr.gouv.vitam.processing.beans.Process;
import fr.gouv.vitam.processing.beans.WorkParams;
import fr.gouv.vitam.processing.constants.StatusCode;
import fr.gouv.vitam.utils.ContainerUtils;
import fr.gouv.vitam.processing.core.ExecutionContext;
import fr.gouv.vitam.processing.core.ProcessEngineImpl;
import fr.gouv.vitam.processing.core.Populator.ProcessPopulator;
import fr.gouv.vitam.processing.core.handler.ActionHandler;
import fr.gouv.vitam.processing.core.handler.AnalyseActionHandler;
import fr.gouv.vitam.processing.core.handler.ExtractContentActionHandler;
import fr.gouv.vitam.processing.core.handler.SaveInDataBaseActionHandler;
import fr.gouv.vitam.processing.core.handler.StoreActionHandler;

public class ProcessEngineImplTest {

	ProcessEngineImpl processEngine;

	/**
	 * improve the different elements and objects for test
	 */
	@Before
	public void init() {

		this.processEngine = new ProcessEngineImpl();
		Process process = ProcessPopulator.populate();
		/**
		 * Pool of action 's object
		 */
		Map<String, ActionHandler> actions = new HashMap<>();
		actions.put(AnalyseActionHandler.HANDLER_ID, new AnalyseActionHandler());
		actions.put(ExtractContentActionHandler.HANDLER_ID, new ExtractContentActionHandler());
		actions.put(StoreActionHandler.HANDLER_ID, new StoreActionHandler());
		actions.put(SaveInDataBaseActionHandler.HANDLER_ID, new SaveInDataBaseActionHandler());
		// add pool in executionContext
		ExecutionContext context = new ExecutionContext(actions);
		// add workParams
		WorkParams params = new WorkParams();
		params.setContainerName(ContainerUtils.generateContainerName());
		params.setObjectName("workflowJSONv1.json");

		this.processEngine.process(process).context(context).Workparams(params);

	}

	@Test
	public void processEngineTest() {
		Response response = processEngine.start();
		assertTrue(StatusCode.OK == response.getStatus());
	}

}
