/*******************************************************************************
 * This file is part of Vitam Project.
 *
 * Copyright Vitam (2012, 2016)
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL license as circulated
 * by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL license and that you
 * accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.processing.engine.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookClient;
import fr.gouv.vitam.logbook.operations.client.LogbookClientFactory;
import fr.gouv.vitam.processing.common.exception.WorkflowNotFoundException;
import fr.gouv.vitam.processing.common.model.EngineResponse;
import fr.gouv.vitam.processing.common.model.ProcessResponse;
import fr.gouv.vitam.processing.common.model.StatusCode;
import fr.gouv.vitam.processing.common.model.Step;
import fr.gouv.vitam.processing.common.model.WorkFlow;
import fr.gouv.vitam.processing.common.model.WorkParams;
import fr.gouv.vitam.processing.common.utils.ProcessPopulator;
import fr.gouv.vitam.processing.distributor.api.ProcessDistributor;
import fr.gouv.vitam.processing.distributor.core.ProcessDistributorImpl;
import fr.gouv.vitam.processing.engine.api.ProcessEngine;

/**
 * ProcessEngineImpl class manages the context and call a process distributor
 *
 */
// FIXME REVIEW since build through Factory => class and constructor as package protected
public class ProcessEngineImpl implements ProcessEngine {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProcessEngineImpl.class);
    private static LogbookClient client = LogbookClientFactory.getInstance().getLogbookOperationClient();
    // FIXME REVIEW: you should not use this method but the one with full mandatory parameters
    LogbookOperationParameters parameters = LogbookParametersFactory.newLogbookOperationParameters();

    private static final String RUNTIME_EXCEPTION_MESSAGE =
        "runtime exceptions thrown by the Process engine during the execution :";
    private static final String ELAPSED_TIME_MESSAGE =
        "Total elapsed time in execution of method startProcessByWorkFlowId is :";
    private static final String START_MESSAGE = "start ProcessEngine ...";
    private static final String WORKFLOW_NOT_FOUND_MESSAGE = "Workflow not exist";

    private final Map<String, WorkFlow> poolWorkflows;
    private final ProcessDistributor processDistributor;

    /**
     * setWorkflow : populate a workflow to the pool of workflow
     *
     * @param workflowId as String
     * @throws WorkflowNotFoundException
     */
    // FIXME REVIEW check null
    public void setWorkflow(String workflowId) throws WorkflowNotFoundException {
        poolWorkflows.put(workflowId, ProcessPopulator.populate(workflowId));
    }

    /**
     * ProcessEngineImpl constructor populate also the workflow to the pool of workflow
     */
    public ProcessEngineImpl() {
        processDistributor = new ProcessDistributorImpl();
        poolWorkflows = new HashMap<>();
        try {
            setWorkflow("DefaultIngestWorkflow");
        } catch (final WorkflowNotFoundException e) {
            LOGGER.error(WORKFLOW_NOT_FOUND_MESSAGE, e);
        }
    }

    /**
     * Implement method startWorkflow of ProcessEngine API Ref : see return and params of method in ProcessEngine API
     * class
     */
    @Override
    public EngineResponse startWorkflow(WorkParams workParams, String workflowId)
        throws WorkflowNotFoundException {
        ParametersChecker.checkParameter("WorkParams is a mandatory parameter", workParams);
        ParametersChecker.checkParameter("workflowId is a mandatory parameter", workflowId);
        final long time = System.currentTimeMillis();
        LOGGER.info(START_MESSAGE);

        /**
         * Check if workflow exist in the pool of workflows
         */
        if (!poolWorkflows.containsKey(workflowId)) {
            throw new WorkflowNotFoundException(WORKFLOW_NOT_FOUND_MESSAGE);
        }
        final ProcessResponse processResponse = new ProcessResponse();
        final Map<String, List<EngineResponse>> stepsResponses = new HashMap<>();

        try {
            final WorkFlow workFlow = poolWorkflows.get(workflowId);
            if (workFlow != null && workFlow.getSteps() != null && !workFlow.getSteps().isEmpty()) {

                /**
                 * call process distribute to manage steps
                 */

                for (final Step step : workFlow.getSteps()) {

                    parameters.putParameterValue(LogbookParameterName.eventIdentifier,
                        GUIDFactory.newGUID().toString());
                    parameters.putParameterValue(LogbookParameterName.eventIdentifierProcess,
                        workParams.getContainerName());
                    parameters.putParameterValue(LogbookParameterName.eventIdentifierRequest, step.getStepName());
                    parameters.putParameterValue(LogbookParameterName.eventType, step.getStepName());
                    parameters.putParameterValue(LogbookParameterName.eventTypeProcess, StatusCode.SUBMITTED.value());
                    parameters.putParameterValue(LogbookParameterName.outcome, StatusCode.SUBMITTED.value());
                    parameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                        StatusCode.SUBMITTED.value());
                    client.update(parameters);

                    final List<EngineResponse> stepResponse =
                        processDistributor.distribute(workParams, step, workflowId);
                    final StatusCode stepStatus = processResponse.getGlobalProcessStatusCode(stepResponse);
                    stepsResponses.put(step.getStepName(), stepResponse);
                    if (stepStatus.equals(StatusCode.FATAL)) {
                        break;
                    }

                    parameters.putParameterValue(LogbookParameterName.eventTypeProcess, stepStatus.value());
                    parameters.putParameterValue(LogbookParameterName.outcome, stepStatus.value());
                    parameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                        "Result: " + stepStatus.value());
                    client.update(parameters);
                }

                /**
                 * the global status process managed in setStepResponses method
                 */
                processResponse.setStepResponses(stepsResponses);
            }
        } catch (final Exception e) {
            processResponse.setStatus(StatusCode.FATAL);
            LOGGER.error(RUNTIME_EXCEPTION_MESSAGE, e);
        } finally {

            LOGGER.info(ELAPSED_TIME_MESSAGE + (System.currentTimeMillis() - time) / 1000 + "s, Status: " +
                processResponse.getStatus());
        }

        return processResponse;
    }

}
