/*******************************************************************************
 * This file is part of Vitam Project.
 * 
 * Copyright Vitam (2012, 2015)
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
public class ProcessEngineImpl implements ProcessEngine {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProcessEngineImpl.class);

    final LogbookClient client = LogbookClientFactory.getInstance().getLogbookOperationClient();
    LogbookOperationParameters parameters = LogbookParametersFactory.newLogbookOperationParameters();

    private static final String RUNTIME_EXCEPTION_MESSAGE =
        "runtime exceptions thrown by the Process engine during the execution :";
    private static final String ELAPSED_TIME_MESSAGE =
        "Total elapsed time in execution of method startProcessByWorkFlowId is :";
    private static final String START_MESSAGE = "start ProcessEngine ...";
    private static final String WORKFLOW_NOT_FOUND_MESSAGE = "Workflow not exist";

    private Map<String, WorkFlow> poolWorkflows;
    private ProcessDistributor processDistributor;

    public void setWorkflow(String worrkflowId) throws WorkflowNotFoundException {
        poolWorkflows.put(worrkflowId, ProcessPopulator.populate(worrkflowId));
    }

    public ProcessEngineImpl() {
        this.processDistributor = new ProcessDistributorImpl();
        this.poolWorkflows = new HashMap<>();
        try {
            setWorkflow("DefaultIngestWorkflow");
        } catch (WorkflowNotFoundException e) {
            LOGGER.error(WORKFLOW_NOT_FOUND_MESSAGE, e);
        }
    }

    @Override
    public EngineResponse startWorkflow(WorkParams workParams, String workflowId)
        throws IllegalArgumentException, WorkflowNotFoundException {
        ParametersChecker.checkParameter("WorkParams is a mandatory parameter", workParams);
        ParametersChecker.checkParameter("workflowId is a mandatory parameter", workflowId);
        long time = System.currentTimeMillis();
        LOGGER.info(START_MESSAGE);

        if (!poolWorkflows.containsKey(workflowId)) {
            throw new WorkflowNotFoundException(WORKFLOW_NOT_FOUND_MESSAGE);
        }
        ProcessResponse processResponse = new ProcessResponse();
        Map<String, List<EngineResponse>> stepsResponses = new HashMap<>();

        try {
            WorkFlow workFlow = poolWorkflows.get(workflowId);
            if (workFlow != null && workFlow.getSteps() != null && !workFlow.getSteps().isEmpty()) {
                /**
                 * call process distribute to manage steps
                 */

                for (Step step : workFlow.getSteps()) {
                    List<EngineResponse> stepResponse = processDistributor.distribute(workParams, step, workflowId);
                    StatusCode stepStatus = processResponse.getGlobalProcessStatusCode(stepResponse);
                    stepsResponses.put(step.getStepName(), stepResponse);

                    parameters.putParameterValue(LogbookParameterName.eventIdentifier,
                        GUIDFactory.newGUID().toString());
                    parameters.putParameterValue(LogbookParameterName.eventIdentifierProcess, step.getStepName());
                    parameters.putParameterValue(LogbookParameterName.eventIdentifierRequest, step.getStepName());
                    parameters.putParameterValue(LogbookParameterName.eventType, step.getStepName());
                    parameters.putParameterValue(LogbookParameterName.eventTypeProcess, step.getStepName());
                    parameters.putParameterValue(LogbookParameterName.outcome, stepStatus.value());
                    parameters.putParameterValue(LogbookParameterName.outcomeDetailMessage, "Result: " + stepStatus.value());
                    client.create(parameters);

                    if (stepStatus.equals(StatusCode.FATAL)) {
                        break;
                    }
                }

                /**
                 * the global status process managed in setStepResponses method
                 */
                processResponse.setStepResponses(stepsResponses);
            }
        } catch (Exception e) {
            processResponse.setStatus(StatusCode.FATAL);
            LOGGER.error(RUNTIME_EXCEPTION_MESSAGE, e);
        } finally {

            LOGGER.info(ELAPSED_TIME_MESSAGE + ((System.currentTimeMillis() - time) / 1000) + "s, Status: " +
                processResponse.getStatus());
        }

        return processResponse;
    }

}
