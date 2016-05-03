package fr.gouv.vitam.processing.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.gouv.vitam.processing.api.ProcessEngine;
import fr.gouv.vitam.processing.api.Response;
import fr.gouv.vitam.processing.beans.Process;
import fr.gouv.vitam.processing.beans.ProcessResponse;
import fr.gouv.vitam.processing.beans.Step;
import fr.gouv.vitam.processing.beans.WorkParams;
import fr.gouv.vitam.processing.beans.Workflow;
import fr.gouv.vitam.processing.constants.StatusCode;
import fr.gouv.vitam.processing.engine.ExecutionContextInterface;
import fr.gouv.vitam.processing.handler.api.Action;
import fr.gouv.vitam.processing.core.Populator.ProcessPopulator;
import fr.gouv.vitam.processing.core.handler.ActionHandler;
import fr.gouv.vitam.processing.core.handler.AnalyseActionHandler;
import fr.gouv.vitam.processing.core.handler.ExtractContentActionHandler;
import fr.gouv.vitam.processing.core.handler.SaveInDataBaseActionHandler;
import fr.gouv.vitam.processing.core.handler.StoreActionHandler;

/**
 * 
 *
 */
public class ProcessEngineImpl implements ProcessEngine {

	private static final Logger LOGGER = LoggerFactory.getLogger(ProcessEngineImpl.class);

	private Process process;

	private ExecutionContextInterface context;

	// muste be contains the same of params
	private WorkParams workParams;

	@Override
	public ProcessEngine init() {

		LOGGER.info("Init ProcessEngine ...");
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

		return new ProcessEngineImpl().process(process).context(context);

	}

	@Override
	public Response start() {

		LOGGER.info("start ProcessEngine ...");

		ProcessResponse processResponse = new ProcessResponse();
		try {

			for (Workflow workflow : this.process.getWorkflow()) {
				List<Step> steps = workflow.getSteps();
				for (Step step : steps)

				{
					getActionHandler(step.getAction()).execute(this.process, this.workParams);
				}
			}

			processResponse.setStatus(StatusCode.OK);

		} catch (Exception e) {
			processResponse.setStatus(StatusCode.ERROR);
			LOGGER.error("runtime exceptions thrown by the Process engine during the execution :", e);
		}

		return processResponse;
	}

	@Override
	public ProcessEngine process(Process process) {
		this.process = process;
		return this;
	}

	public ProcessEngine Workparams(WorkParams workParams) {
		this.workParams = workParams;
		return this;
	}

	/**
	 * return Action Handler by ID
	 * 
	 * @param actionID
	 * @return ActionHandler
	 */
	private Action getActionHandler(String actionID) {

		return context.getActions().get(actionID);
	}

	@Override
	public ProcessEngine context(ExecutionContextInterface context) {
		this.context = context;
		return this;
	}

}
