/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.processing.engine.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.exception.InvalidGuidOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationsClientHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.exception.WorkflowNotFoundException;
import fr.gouv.vitam.processing.common.model.Action;
import fr.gouv.vitam.processing.common.model.ProcessBehavior;
import fr.gouv.vitam.processing.common.model.ProcessResponse;
import fr.gouv.vitam.processing.common.model.ProcessStep;
import fr.gouv.vitam.processing.common.model.WorkFlow;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.utils.ProcessPopulator;
import fr.gouv.vitam.processing.distributor.api.ProcessDistributor;
import fr.gouv.vitam.processing.distributor.core.ProcessDistributorImplFactory;
import fr.gouv.vitam.processing.engine.api.ProcessEngine;
import fr.gouv.vitam.processing.engine.core.monitoring.ProcessMonitoringImpl;

/**
 * ProcessEngineImpl class manages the context and call a process distributor
 *
 */
public class ProcessEngineImpl implements ProcessEngine {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProcessEngineImpl.class);

    private static final String RUNTIME_EXCEPTION_MESSAGE =
        "runtime exceptions thrown by the Process engine during the execution :";
    private static final String ELAPSED_TIME_MESSAGE =
        "Total elapsed time in execution of method startProcessByWorkFlowId is :";
    private static final String START_MESSAGE = "start ProcessEngine ...";
    private static final String WORKFLOW_NOT_FOUND_MESSAGE = "Workflow not exist";
    private static final String MESSAGE_IDENTIFIER = "messageIdentifier";

    private static final String OBJECTS_LIST_EMPTY = "OBJECTS_LIST_EMPTY";

    private final Map<String, WorkFlow> poolWorkflows;

    private final ProcessDistributor processDistributorMock;
    private final Map<String, String> messageIdentifierMap = new HashMap<>();


    /**
     * ProcessEngineImpl constructor populate also the workflow to the pool of workflow
     */
    protected ProcessEngineImpl() {
        processDistributorMock = null;
        poolWorkflows = new HashMap<>();
        try {
            setWorkflow("DefaultIngestWorkflow");
        } catch (final WorkflowNotFoundException e) {
            LOGGER.error(WORKFLOW_NOT_FOUND_MESSAGE, e);
        }
    }

    /**
     * For test purpose
     *
     * @param processDistributor the wanted process distributor
     */
    ProcessEngineImpl(ProcessDistributor processDistributor) {
        processDistributorMock = processDistributor;
        poolWorkflows = new HashMap<>();
    }

    /**
     * setWorkflow : populate a workflow to the pool of workflow
     *
     * @param workflowId as String
     * @throws WorkflowNotFoundException throw when workflow not found
     */
    public void setWorkflow(String workflowId) throws WorkflowNotFoundException {
        ParametersChecker.checkParameter("workflowId is a mandatory parameter", workflowId);
        poolWorkflows.put(workflowId, ProcessPopulator.populate(workflowId));
    }

    @Override
    public ItemStatus startWorkflow(WorkerParameters workParams, String workflowId)
        throws WorkflowNotFoundException, ProcessingException {
        ParametersChecker.checkParameter("WorkParams is a mandatory parameter", workParams);
        ParametersChecker.checkParameter("workflowId is a mandatory parameter", workflowId);
        final long time = System.currentTimeMillis();
        LOGGER.info(START_MESSAGE);
        // TODO P1 replace with real tenant
        final int tenantId = 0;
        /**
         * Check if workflow exist in the pool of workflows
         */
        if (!poolWorkflows.containsKey(workflowId)) {
            LOGGER.error(WORKFLOW_NOT_FOUND_MESSAGE);
            throw new WorkflowNotFoundException(WORKFLOW_NOT_FOUND_MESSAGE);
        }
        final ProcessResponse processResponse = new ProcessResponse();
        final GUID processId = GUIDFactory.newGUID();
        final ItemStatus workflowStatus = new ItemStatus(processId.toString());

        try (LogbookOperationsClient client = LogbookOperationsClientFactory.getInstance().getClient()) {
            final WorkFlow workFlow = poolWorkflows.get(workflowId);

            if (workFlow != null && workFlow.getSteps() != null && !workFlow.getSteps().isEmpty()) {

                processResponse.setProcessId(processId.getId());
                workParams.setProcessId(processId.getId());
                LOGGER.info("Start Workflow: " + processId.getId());


                final Map<String, ProcessStep> processSteps = ProcessMonitoringImpl.getInstance().initOrderedWorkflow(
                    workParams.getProcessId(), workFlow,
                    workParams.getContainerName());

                /**
                 * call process distribute to manage steps
                 */
                boolean finished = true;
                ItemStatus stepResponse;
                for (final Map.Entry<String, ProcessStep> entry : processSteps.entrySet()) {
                    final ProcessStep step = entry.getValue();
                    stepResponse = processStep(processId.getId(), step, entry.getKey(), workParams,
                        workflowStatus, client, workflowId, messageIdentifierMap.get(processId.getId()),
                        tenantId, finished);
                    // if the step has been defined as Blocking and then stepStatus is KO or FATAL
                    // then break the process
                    if (stepResponse.shallStop(step.getBehavior().equals(ProcessBehavior.BLOCKING))) {
                        finished = false;
                        break;
                    }
                }

                // Workflow was break, go last step
                if (!finished) {
                    final String theLastKey = new ArrayList<>(processSteps.keySet()).get(processSteps.size() - 1);
                    final ProcessStep lastStep = processSteps.get(theLastKey);
                    // check if it's a final step
                    if (ProcessBehavior.FINALLY.equals(lastStep.getBehavior())) {
                        processStep(processId.getId(), lastStep, theLastKey, workParams,
                            workflowStatus, client, workflowId, messageIdentifierMap.get(processId.getId()),
                            tenantId, finished);
                    } else {
                        LOGGER.info("No final step found");
                    }
                }

                messageIdentifierMap.remove(processId);
                LOGGER.info("End Workflow: " + processId.getId());
            }
        } catch (final Exception e) {
            processResponse.setStatus(StatusCode.FATAL);
            LOGGER.error(RUNTIME_EXCEPTION_MESSAGE, e);
        } finally {
            LOGGER.info(ELAPSED_TIME_MESSAGE + (System.currentTimeMillis() - time) / 1000 + "s, Status: " +
                processResponse.getStatus());
        }

        return workflowStatus;
    }



    private ItemStatus processStep(String processId, ProcessStep step, String uniqueId, WorkerParameters workParams,
        ItemStatus workflowStatus, LogbookOperationsClient client, String workflowId, String messageIdentifier,
        int tenantId, boolean finished)
        throws InvalidGuidOperationException, LogbookClientBadRequestException, LogbookClientNotFoundException,
        LogbookClientServerException, ProcessingException {

        workParams.setStepUniqId(uniqueId);
        LOGGER.info("Start Workflow: " + uniqueId + " Step:" + step.getStepName());
        final LogbookOperationParameters parameters = LogbookParametersFactory.newLogbookOperationParameters(
            GUIDFactory.newGUID(),
            step.getStepName(),
            GUIDReader.getGUID(workParams.getContainerName()),
            LogbookTypeProcess.INGEST,
            StatusCode.STARTED,
            VitamLogbookMessages.getCodeOp(step.getStepName(), StatusCode.STARTED),
            GUIDReader.getGUID(workParams.getContainerName()));
        client.update(parameters);

        // update the process monitoring for this step
        ProcessMonitoringImpl.getInstance().updateStepStatus(
            workParams.getProcessId(), uniqueId,
            StatusCode.STARTED);

        workParams.setCurrentStep(step.getStepName());
        ProcessDistributor processDistributor = processDistributorMock;
        try {
            if (processDistributor == null) {
                processDistributor = ProcessDistributorImplFactory.getDefaultDistributor();
            }
            final ItemStatus stepResponse =
                processDistributor.distribute(workParams, step, workflowId);

            // update workflow Status
            workflowStatus.increment(stepResponse.getGlobalStatus());
            final LogbookOperationsClientHelper helper = new LogbookOperationsClientHelper();
            for (final Action action : step.getActions()) {
                final String hanlderId = action.getActionDefinition().getActionKey();
                // Each handler could have a list itself => ItemStatus
                final ItemStatus itemStatus = stepResponse.getItemsStatus().get(hanlderId);
                if (itemStatus != null) {
                    final LogbookOperationParameters actionParameters =
                        LogbookParametersFactory.newLogbookOperationParameters(
                            GUIDFactory.newEventGUID(tenantId),
                            hanlderId,
                            GUIDReader.getGUID(workParams.getContainerName()),
                            LogbookTypeProcess.INGEST,
                            StatusCode.STARTED,
                            VitamLogbookMessages.getCodeOp(hanlderId, StatusCode.STARTED),
                            GUIDReader.getGUID(workParams.getContainerName()));
                    helper.updateDelegate(actionParameters);
                    if (itemStatus instanceof ItemStatus) {
                        final ItemStatus actionStatus = itemStatus;
                        for (final ItemStatus sub : actionStatus.getItemsStatus().values()) {
                            final LogbookOperationParameters sublogbook =
                                LogbookParametersFactory.newLogbookOperationParameters(
                                    GUIDFactory.newEventGUID(tenantId),
                                    actionStatus.getItemId(),
                                    GUIDReader.getGUID(workParams.getContainerName()),
                                    LogbookTypeProcess.INGEST,
                                    sub.getGlobalStatus(),
                                    sub.getItemId(), " Detail= " + sub.computeStatusMeterMessage(),
                                    GUIDReader.getGUID(workParams.getContainerName()));
                            helper.updateDelegate(sublogbook);
                        }
                    }

                    final LogbookOperationParameters sublogbook =
                        LogbookParametersFactory.newLogbookOperationParameters(
                            GUIDFactory.newEventGUID(tenantId),
                            itemStatus.getItemId(),
                            GUIDReader.getGUID(workParams.getContainerName()),
                            LogbookTypeProcess.INGEST,
                            itemStatus.getGlobalStatus(),
                            null, " Detail= " + itemStatus.computeStatusMeterMessage(),
                            GUIDReader.getGUID(workParams.getContainerName()));
                    helper.updateDelegate(sublogbook);
                }
            }

            final ItemStatus itemStatusObjectListEmpty = stepResponse.getItemsStatus().get(OBJECTS_LIST_EMPTY);
            if (itemStatusObjectListEmpty != null) {
                final LogbookOperationParameters actionParameters =
                    LogbookParametersFactory.newLogbookOperationParameters(
                        GUIDFactory.newEventGUID(tenantId),
                        OBJECTS_LIST_EMPTY,
                        GUIDReader.getGUID(workParams.getContainerName()),
                        LogbookTypeProcess.INGEST,
                        itemStatusObjectListEmpty.getGlobalStatus(),
                        VitamLogbookMessages.getCodeOp(OBJECTS_LIST_EMPTY, itemStatusObjectListEmpty.getGlobalStatus()),
                        GUIDReader.getGUID(workParams.getContainerName()));
                helper.updateDelegate(actionParameters);
            }

            if (messageIdentifier == null) {
                if (stepResponse.getData().get(MESSAGE_IDENTIFIER) != null) {
                    messageIdentifier = stepResponse.getData().get(MESSAGE_IDENTIFIER).toString();
                    messageIdentifierMap.put(processId, messageIdentifier);
                }

            }

            if (messageIdentifier != null && !messageIdentifier.isEmpty()) {
                parameters.putParameterValue(LogbookParameterName.objectIdentifierIncome, messageIdentifier);
            }

            parameters.putParameterValue(LogbookParameterName.outcome, stepResponse.getGlobalStatus().name());
            parameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                VitamLogbookMessages.getCodeOp(stepResponse.getItemId(), stepResponse.getGlobalStatus()));
            helper.updateDelegate(parameters);
            client.bulkUpdate(workParams.getContainerName(),
                helper.removeUpdateDelegate(workParams.getContainerName()));

            // update the process monitoring with the final status
            ProcessMonitoringImpl.getInstance().updateStepStatus(
                workParams.getProcessId(), uniqueId,
                stepResponse.getGlobalStatus());
            LOGGER.info("End Workflow: " + uniqueId + " Step:" + step.getStepName());
            // TODO P1 : deal with the pause
            // else if (step.getStepType().equals(StepType.PAUSE)) {
            // THEN PAUSE
            // }
            return stepResponse;
        } finally {
            if (processDistributorMock == null && processDistributor != null) {
                try {
                    processDistributor.close();
                } catch (final Exception exc) {
                    SysErrLogger.FAKE_LOGGER.ignoreLog(exc);
                }
            }
        }
    }

}

