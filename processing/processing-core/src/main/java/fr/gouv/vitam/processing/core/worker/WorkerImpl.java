/*******************************************************************************
 * This file is part of Vitam Project.
 * 
 * Copyright Vitam (2012, 2015)
 *
 * This software is governed by the CeCILL 2.1 license under French law and
 * abiding by the rules of distribution of free software. You can use, modify
 * and/ or redistribute the software under the terms of the CeCILL license as
 * circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify
 * and redistribute granted by the license, users are provided only with a
 * limited warranty and the software's author, the holder of the economic
 * rights, and the successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with
 * loading, using, modifying and/or developing or reproducing the software by
 * the user in light of its specific status of free software, that may mean that
 * it is complicated to manipulate, and that also therefore means that it is
 * reserved for developers and experienced professionals having in-depth
 * computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling
 * the security of their systems and/or data to be ensured and, more generally,
 * to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.processing.core.worker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.gouv.vitam.processing.api.exception.EngineIllegalArgumentException;
import fr.gouv.vitam.processing.api.exception.HandlerNotFoundException;
import fr.gouv.vitam.processing.api.model.Action;
import fr.gouv.vitam.processing.api.model.Response;
import fr.gouv.vitam.processing.api.model.Step;
import fr.gouv.vitam.processing.api.model.WorkParams;
import fr.gouv.vitam.processing.api.worker.Worker;
import fr.gouv.vitam.processing.core.handler.ActionHandler;
import fr.gouv.vitam.processing.core.handler.AnalyseActionHandler;
import fr.gouv.vitam.processing.core.handler.ExtractContentActionHandler;
import fr.gouv.vitam.processing.core.handler.InsertInMongodbActionHandler;
import fr.gouv.vitam.processing.core.handler.StoreInWorkspaceActionHandler;

/**
 * WorkerImpl class implements Worker interface
 * 
 * manages and executes actions by step
 */

public class WorkerImpl implements Worker {

	private static final Logger LOGGER = LoggerFactory.getLogger(WorkerImpl.class);

	private static final String EMPTY_LIST = "null or Empty Action list";
	private static final String STEP_NULL = "step paramaters is null";
	private static final String WORK_PARAM_NULL = "workparams is null";
	private static final String HANDLER_NOT_FOUND = ": handler not found exception";
	private static final String ELAPSED_TIME_MESSAGE = "Total elapsed time in execution of WorkerImpl method run is :";

	/**
	 * Empty Object constructor
	 **/
	public WorkerImpl() {
		/**
		 * temporary init: will be managed by spring annotation
		 */
		init();
	}

	Map<String, ActionHandler> actions = new HashMap<>();

	private void init() {
		/**
		 * Pool of action 's object
		 */
		actions.put(AnalyseActionHandler.HANDLER_ID, new AnalyseActionHandler());
		actions.put(ExtractContentActionHandler.HANDLER_ID, new ExtractContentActionHandler());
		actions.put(StoreInWorkspaceActionHandler.HANDLER_ID, new StoreInWorkspaceActionHandler());
		try {
			actions.put(InsertInMongodbActionHandler.HANDLER_ID, new InsertInMongodbActionHandler());
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
		}
	}

	@Override
	public List<Response> run(WorkParams workParams, Step step)
			throws EngineIllegalArgumentException, HandlerNotFoundException {

		long time = System.currentTimeMillis();

		if (workParams == null) {
			throw new EngineIllegalArgumentException(WORK_PARAM_NULL);
		}

		if (step == null) {
			throw new EngineIllegalArgumentException(STEP_NULL);
		}

		if (step.getActions() == null || step.getActions().isEmpty()) {
			throw new EngineIllegalArgumentException(EMPTY_LIST);
		}

		List<Response> responses = new ArrayList<>();

		for (Action action : step.getActions()) {

			ActionHandler actionHandler = getActionHandler(action.getActionKey());
			if (actionHandler == null) {
				throw new HandlerNotFoundException(action.getActionKey() + HANDLER_NOT_FOUND);
			}

			responses.add(actionHandler.execute(workParams));
		}

		LOGGER.info(ELAPSED_TIME_MESSAGE + ((System.currentTimeMillis() - time) / 1000) + "s / for step name :"
				+ step.getStepName());
		return responses;
	}

	private ActionHandler getActionHandler(String actionId) {
		return actions.get(actionId);
	}

}
