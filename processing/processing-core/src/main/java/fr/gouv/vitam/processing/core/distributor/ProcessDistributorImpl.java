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
package fr.gouv.vitam.processing.core.distributor;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.gouv.vitam.processing.api.distributor.ProcessDistributor;
import fr.gouv.vitam.processing.api.exception.EngineIllegalArgumentException;
import fr.gouv.vitam.processing.api.exception.HandlerNotFoundException;
import fr.gouv.vitam.processing.api.model.ProcessResponse;
import fr.gouv.vitam.processing.api.model.Response;
import fr.gouv.vitam.processing.api.model.StatusCode;
import fr.gouv.vitam.processing.api.model.Step;
import fr.gouv.vitam.processing.api.model.WorkParams;
import fr.gouv.vitam.processing.api.worker.Worker;
import fr.gouv.vitam.processing.core.worker.WorkerImpl;

/**
 * The Process Distributor call the workers {@link Worker}and intercept the
 * response for manage a post actions step
 * 
 */
// FIXME REVIEW missing package-info
// FIXME REVIEW src/main/resources should not contains test files
public class ProcessDistributorImpl implements ProcessDistributor {

	private static final String ELAPSED_TIME_MESSAGE = "Total elapsed time in execution of method distribute is :";

	private static final String EXCEPTION_MESSAGE = "runtime exceptions thrown by the Process distributor during runnig...";

	private static final Logger LOGGER = LoggerFactory.getLogger(ProcessDistributorImpl.class);

	private Worker worker;

	/**
	 * Empty constructor
	 */
	public ProcessDistributorImpl() {
		worker = new WorkerImpl();
	}

	@Override
	public List<Response> distribute(WorkParams workParams, Step step, String workflowId) {
		long time = System.currentTimeMillis();
		Response errorResponse = new ProcessResponse();
		errorResponse.setStatus(StatusCode.FATAL);
		List<Response> responses = new ArrayList<>();
		try {
			/**
			 * 
			 */
			responses.addAll(worker.run(workParams, step));
			/**
			 * 
			 */
		} catch (EngineIllegalArgumentException e) {
			responses.add(errorResponse);
			LOGGER.error(e.getMessage());
		} catch (HandlerNotFoundException e) {
			responses.add(errorResponse);
			LOGGER.error(e.getMessage());

		} catch (Exception e) {
			responses.add(errorResponse);
			LOGGER.error(EXCEPTION_MESSAGE, e);
		} finally {
			LOGGER.info(ELAPSED_TIME_MESSAGE + ((System.currentTimeMillis() - time) / 1000) + "s /stepName :"
					+ getSaftyStepName(step) + "/workflowId :" + workflowId);
		}
		return responses;
	}

	private String getSaftyStepName(Step step) {

		if (step == null || step.getStepName() == null) {
			return "";
		}

		return step.getStepName();
	}
}
