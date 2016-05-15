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
package fr.gouv.vitam.processing.core.engine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.gouv.vitam.processing.api.distributor.ProcessDistributor;
import fr.gouv.vitam.processing.api.engine.ProcessEngine;
import fr.gouv.vitam.processing.api.exception.EngineIllegalArgumentException;
import fr.gouv.vitam.processing.api.exception.WorkflowNotFoundException;
import fr.gouv.vitam.processing.api.model.ProcessResponse;
import fr.gouv.vitam.processing.api.model.Response;
import fr.gouv.vitam.processing.api.model.StatusCode;
import fr.gouv.vitam.processing.api.model.WorkFlow;
import fr.gouv.vitam.processing.api.model.WorkParams;
import fr.gouv.vitam.processing.core.distributor.ProcessDistributorImpl;
import fr.gouv.vitam.processing.core.utils.ProcessPopulator;

/**
 * ProcessEngineImpl class manages the context and call a process distributor
 * 
 * 
 * 
 * 
 */
// FIXME REVIEW missing package-info
public class ProcessEngineImpl implements ProcessEngine {

	private static final Logger LOGGER = LoggerFactory.getLogger(ProcessEngineImpl.class);

	// messages

	private static final String RUNTIME_EXCEPTION_MESSAGE = "runtime exceptions thrown by the Process engine during the execution :";
	private static final String ELAPSED_TIME_MESSAGE = "Total elapsed time in execution of method startProcessByWorkFlowId is :";
	private static final String START_MESSAGE = "start ProcessEngine ...";

	private Map<String, WorkFlow> poolWorkflows;

	private ProcessDistributor processDistributor;

	// FIXME REVIEW make it private and build a public static getInstance() / createInstance()
	public ProcessEngineImpl() {
		this.processDistributor = new ProcessDistributorImpl();
		this.poolWorkflows = new HashMap<String, WorkFlow>();

		// init map

		poolWorkflows.put("workflowJSONv1", ProcessPopulator.populate("workflowJSONv1"));
		poolWorkflows.put("workflowJSONv2", ProcessPopulator.populate("workflowJSONv2"));
	}

	@Override
	public Response startProcessByWorkFlowId(WorkParams workParams, String workflowId)
			throws EngineIllegalArgumentException, WorkflowNotFoundException {
		long time = System.currentTimeMillis();
		LOGGER.info(START_MESSAGE);
                // FIXME REVIEW Put a message in the Exception
		if (workParams == null) {
			throw new EngineIllegalArgumentException("");
		}

                // FIXME REVIEW Put a message in the Exception
		if (StringUtils.isEmpty(workflowId)) {
			throw new EngineIllegalArgumentException("");
		}

                // FIXME REVIEW Put a message in the Exception
		if (!poolWorkflows.containsKey(workflowId)) {
			throw new WorkflowNotFoundException("");
		}
		ProcessResponse processResponse = new ProcessResponse();
		Map<String, List<Response>> stepResponses = new HashMap<>();

		try {

			WorkFlow workFlow = poolWorkflows.get(workflowId);
			// FIXME REVIEW no exception returned for not found ? ProcessResponse is empty then... ?
			// TODO REVIEW Try to not call getSteps 2 times
			if (workFlow != null && workFlow.getSteps() != null && !workFlow.getSteps().isEmpty()) {

				/**
				 * call process distribute to manage steps
				 */
				workFlow.getSteps().forEach(step -> stepResponses.put(step.getStepName(),
						processDistributor.distribute(workParams, step, workflowId)));
				/**
				 * the global status process managed in setStepResponses method
				 */
				processResponse.setStepResponses(stepResponses);
			}

		} catch (Exception e) {
			processResponse.setStatus(StatusCode.FATAL);
			LOGGER.error(RUNTIME_EXCEPTION_MESSAGE, e);
		} finally {
			LOGGER.info(ELAPSED_TIME_MESSAGE + ((System.currentTimeMillis() - time) / 1000) + "s");
		}

		return processResponse;
	}

}
