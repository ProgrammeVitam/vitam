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

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.exception.InvalidGuidOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.WorkflowNotFoundException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.ProcessExecutionStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.server.application.AsyncInputStreamHelper;
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
import fr.gouv.vitam.processing.common.exception.ProcessingStorageWorkspaceException;
import fr.gouv.vitam.processing.common.exception.StepsNotFoundException;
import fr.gouv.vitam.processing.common.model.Action;
import fr.gouv.vitam.processing.common.model.ProcessBehavior;
import fr.gouv.vitam.processing.common.model.ProcessResponse;
import fr.gouv.vitam.processing.common.model.ProcessStep;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.data.core.ProcessDataAccess;
import fr.gouv.vitam.processing.data.core.ProcessDataAccessImpl;
import fr.gouv.vitam.processing.data.core.management.ProcessDataManagement;
import fr.gouv.vitam.processing.data.core.management.WorkspaceProcessDataManagement;
import fr.gouv.vitam.processing.distributor.api.ProcessDistributor;
import fr.gouv.vitam.processing.distributor.core.ProcessDistributorImplFactory;
import fr.gouv.vitam.processing.engine.api.ProcessEngine;

/**
 * ProcessEngineImpl class manages the context and call a process distributor
 *
 */
public class ProcessEngineImpl implements ProcessEngine, Runnable {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProcessEngineImpl.class);

    private static final String RUNTIME_EXCEPTION_MESSAGE =
        "runtime exceptions thrown by the Process engine during the execution :";
    private static final String ELAPSED_TIME_MESSAGE =
        "Total elapsed time in execution of method startProcessByWorkFlowId is :";
    private static final String START_MESSAGE = "start ProcessEngine ...";

    private static final String MESSAGE_IDENTIFIER = "messageIdentifier";


    private static final String OBJECTS_LIST_EMPTY = "OBJECTS_LIST_EMPTY";

    private final ProcessDistributor processDistributorMock;
    private final Map<String, String> messageIdentifierMap = new HashMap<>();
    private final ProcessDataAccess processData;
    private Object monitor = null;
    private WorkerParameters workParams = null;
    private boolean isFirstCall = true;
    private AsyncResponse asyncResponse = null;

    private ProcessDataManagement dataManagement;

    /**
     * ProcessEngineImpl constructor populate also the workflow to the pool of workflow
     */
    protected ProcessEngineImpl(WorkerParameters workParams, Object monitor, AsyncResponse asyncResponse) {
        processDistributorMock = null;
        processData = ProcessDataAccessImpl.getInstance();
        this.monitor = monitor;
        this.workParams = workParams;
        this.asyncResponse = asyncResponse;
        dataManagement = WorkspaceProcessDataManagement.getInstance();
    }

    /**
     * For test purpose
     *
     * @param processDistributor the wanted process distributor
     */
    ProcessEngineImpl(ProcessDistributor processDistributor) {
        processDistributorMock = processDistributor;
        processData = ProcessDataAccessImpl.getInstance();
    }

    @Override
    public void run() {
        try {
            if (isFirstCall) {
                isFirstCall = false;

                // TODO rename this method
                AsyncInputStreamHelper.asyncResponseResume(asyncResponse, Response.ok().build());
                synchronized (monitor) {
                    monitor.wait();
                }
            }
            startWorkflow(workParams);
        } catch (WorkflowNotFoundException e) {
            LOGGER.error(RUNTIME_EXCEPTION_MESSAGE, e);
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse,
                Response.status(Status.INTERNAL_SERVER_ERROR).build());

        } catch (InterruptedException e) {
            LOGGER.error(RUNTIME_EXCEPTION_MESSAGE, e);
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse,
                Response.status(Status.INTERNAL_SERVER_ERROR).build());
        }
    }


    @Override
    public ItemStatus startWorkflow(WorkerParameters workParams) throws WorkflowNotFoundException {

        ParametersChecker.checkParameter("WorkParams is a mandatory parameter", workParams);

        final long time = System.currentTimeMillis();
        LOGGER.info(START_MESSAGE);
        final int tenantId = ParameterHelper.getTenantParameter();
        String operationId = workParams.getContainerName();
        /**
         * Check if workflow exist in the pool of workflows
         */
        final ProcessResponse processResponse = new ProcessResponse();
        final GUID processId = GUIDFactory.newGUID();
        final ItemStatus workflowStatus = new ItemStatus(processId.toString());

        try (LogbookOperationsClient client = LogbookOperationsClientFactory.getInstance().getClient()) {

            processResponse.setProcessId(processId.getId());
            workParams.setProcessId(processId.getId());
            LOGGER.info("Start Workflow: " + processId.getId());

            /**
             * call process distribute to manage steps
             */
            ItemStatus stepResponse;
            ProcessStep step = null;

            while ((step = processData.nextStep(operationId, tenantId)) != null) {

                stepResponse =
                    processStep(processId.getId(), step, step.getId(), workParams, workflowStatus, client, operationId,
                        messageIdentifierMap.get(processId.getId()), tenantId);

                // update global status Process workFlow and process Step
                processData.updateStepStatus(operationId, step.getId(), stepResponse.getGlobalStatus(), tenantId);

                // if the step has been defined as Blocking and stepStatus is KO or FATAL
                // then stop the process
                if (stepResponse.shallStop(step.getBehavior().equals(ProcessBehavior.BLOCKING))) {
                    processData.updateProcessExecutionStatus(operationId, ProcessExecutionStatus.FAILED, tenantId);
                    // Delete
                    try {
                        dataManagement.removeProcessWorkflow(String.valueOf(ServerIdentity.getInstance().getServerId()),
                            operationId);
                    } catch (ProcessingStorageWorkspaceException e) {
                        LOGGER.error("cannot delete workflow file for serverID {} and asyncID {}", String.valueOf
                            (ServerIdentity.getInstance().getServerId()), operationId, e);
                        // Nothing for now
                    }
                    break;
                }

                // finalize step execution
                executeAfterEachStep(workflowStatus, asyncResponse, operationId, false, tenantId);
            }


            // the WorkFlow was failed or finished, so go to the finally step
            final ProcessStep finallyStep = processData.getFinallyStep(operationId, tenantId);

            // check if it's a final step
            if (finallyStep != null) {
                processStep(processId.getId(), finallyStep, finallyStep.getId(), workParams,
                    workflowStatus, client, operationId, messageIdentifierMap.get(processId.getId()),
                    tenantId);
                messageIdentifierMap.remove(processId);

                // process finished
                processData.updateProcessExecutionStatus(operationId, ProcessExecutionStatus.COMPLETED, tenantId);

                // Finalize step execution
                executeAfterEachStep(workflowStatus, asyncResponse, operationId, true, tenantId);

                LOGGER.info("End Workflow: " + processId.getId());
            }
        } catch (final StepsNotFoundException e) {
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse,
                Response.status(Status.NOT_FOUND).build());
        } catch (final Exception e) {
            processResponse.setStatus(StatusCode.FATAL);
            processData.updateProcessExecutionStatus(operationId, ProcessExecutionStatus.FAILED, tenantId);

            buildAndSendAsyncResponse(workflowStatus, asyncResponse,
                processData.getProcessWorkflow(operationId, tenantId));

            LOGGER.error(RUNTIME_EXCEPTION_MESSAGE, e);
        } finally {
            LOGGER.info(ELAPSED_TIME_MESSAGE + (System.currentTimeMillis() - time) / 1000 + "s, Status: " +
                processResponse.getStatus());
        }

        return workflowStatus;
    }

    private void executeAfterEachStep(ItemStatus workflowStatus, AsyncResponse asyncResponse, String operationId,
        boolean isFinalStep, Integer tenantId)
        throws InterruptedException {

        // Check now if it is a step by step workFlow; if it is so, build and send the asyncResponse before
        // suspending the thread
        ProcessWorkflow processWorkflow = processData.getProcessWorkflow(operationId, tenantId);

        boolean isStepByStepWorkflow = ProcessAction.NEXT.equals(processWorkflow.getExecutionMode());
        boolean isSuspendedWorkflow = ProcessExecutionStatus.PAUSE.equals(processWorkflow.getExecutionStatus());
        boolean isCancelledWorkflow = ProcessExecutionStatus.CANCELLED.equals(processWorkflow.getExecutionStatus());

        if (isFinalStep | isCancelledWorkflow) {
            try {
                dataManagement.removeProcessWorkflow(String.valueOf(ServerIdentity.getInstance().getServerId()),
                    operationId);
            } catch (ProcessingStorageWorkspaceException e) {
                LOGGER.error("cannot delete workflow file for serverID {} and asyncID {}", String.valueOf
                    (ServerIdentity.getInstance().getServerId()), operationId, e);
            }
            // Build asyncResponse and resume it
            buildAndSendAsyncResponse(workflowStatus, asyncResponse, processWorkflow);
        } else if (isStepByStepWorkflow | isSuspendedWorkflow) {
            try {
                dataManagement.persistProcessWorkflow(String.valueOf(ServerIdentity.getInstance().getServerId()),
                    operationId, processWorkflow);
            } catch (InvalidParseOperationException | ProcessingStorageWorkspaceException e) {
                LOGGER.error("Cannot persist process workflow file, set status to FAILED", e);
                processWorkflow.setExecutionStatus(ProcessExecutionStatus.FAILED);
                workflowStatus.setGlobalExecutionStatus(ProcessExecutionStatus.FAILED);
            }

            // Build asyncResponse : put ItemStatus in the body and resume it
            buildAndSendAsyncResponse(workflowStatus, asyncResponse, processWorkflow);

            // Suspend the current thread
            synchronized (monitor) {
                monitor.wait();
            }
        }
    }

    private void buildAndSendAsyncResponse(ItemStatus workflowStatus, AsyncResponse asyncResponse,
        ProcessWorkflow processWorkflow) {

        // Build the response based on the workFlow global status
        Status lastStatus = workflowStatus.getGlobalStatus().getEquivalentHttpStatus();

        // Add mandatory headers
        ProcessExecutionStatus processExecutionStatus = processWorkflow.getExecutionStatus();
        String logbookTypeProcess =
            processWorkflow.getLogbookTypeProcess().toString();

        ResponseBuilder currentResponse =
            Response.status(lastStatus).header(GlobalDataRest.X_GLOBAL_EXECUTION_STATUS, processExecutionStatus)
                .header(GlobalDataRest.X_CONTEXT_ID, logbookTypeProcess);
        if (asyncResponse != null) {
            asyncResponse.resume(currentResponse.status(lastStatus).build());
        }
    }

    /**
     * @param asyncResponse of type {@link AsyncResponse}
     */
    public void setAsyncResponse(AsyncResponse asyncResponse) {
        // Before setting the new asyncResponse, resume it with conflict status
        if (this.asyncResponse != null && (!this.asyncResponse.isDone() || !this.asyncResponse.isCancelled())) {
            Response currentResponse = Response.status(Status.CONFLICT).build();
            AsyncInputStreamHelper.asyncResponseResume(this.asyncResponse, currentResponse);
        }

        this.asyncResponse = asyncResponse;
    }

    private ItemStatus processStep(String processId, ProcessStep step, String uniqueId, WorkerParameters workParams,
        ItemStatus workflowStatus, LogbookOperationsClient client, String workflowId, String messageIdentifier,
        int tenantId)
        throws InvalidGuidOperationException, LogbookClientBadRequestException, LogbookClientNotFoundException,
        LogbookClientServerException, ProcessingException {

        String eventDetailData;
        workParams.setStepUniqId(uniqueId);
        LOGGER.info("Start Workflow: " + uniqueId + " Step:" + step.getStepName());


        LogbookTypeProcess logbookTypeProcess =
            processData.getProcessWorkflow(workParams.getContainerName(), tenantId).getLogbookTypeProcess();


        final LogbookOperationParameters parameters = LogbookParametersFactory.newLogbookOperationParameters(
            GUIDFactory.newEventGUID(tenantId),
            step.getStepName(),
            GUIDReader.getGUID(workParams.getContainerName()),
            logbookTypeProcess,
            StatusCode.STARTED,
            VitamLogbookMessages.getCodeOp(step.getStepName(), StatusCode.STARTED),
            GUIDReader.getGUID(workParams.getContainerName()));
        parameters.putParameterValue(
            LogbookParameterName.outcomeDetail, VitamLogbookMessages.getOutcomeDetail(step.getStepName(), StatusCode.STARTED));
        client.update(parameters);

        // update the process monitoring for this step
        processData.updateStepStatus(workParams.getContainerName(), step.getId(), StatusCode.STARTED, tenantId);

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
                final String handlerId = action.getActionDefinition().getActionKey();
                // Each handler could have a list itself => ItemStatus
                final ItemStatus itemStatus = stepResponse.getItemsStatus().get(handlerId);
                if (itemStatus != null) {
                    final LogbookOperationParameters actionParameters =
                        LogbookParametersFactory.newLogbookOperationParameters(
                            GUIDFactory.newEventGUID(tenantId),
                            handlerId,
                            GUIDReader.getGUID(workParams.getContainerName()),
                            logbookTypeProcess,
                            StatusCode.STARTED,
                            VitamLogbookMessages.getCodeOp(handlerId, StatusCode.STARTED),
                            GUIDReader.getGUID(workParams.getContainerName()));
                    actionParameters.putParameterValue(
                        LogbookParameterName.outcomeDetail, VitamLogbookMessages.getOutcomeDetail(handlerId, StatusCode.STARTED));
                    helper.updateDelegate(actionParameters);
                    if (itemStatus instanceof ItemStatus) {
                        final ItemStatus actionStatus = itemStatus;
                        for (final ItemStatus sub : actionStatus.getItemsStatus().values()) {
                            final LogbookOperationParameters sublogbook =
                                LogbookParametersFactory.newLogbookOperationParameters(
                                    GUIDFactory.newEventGUID(tenantId),
                                    actionStatus.getItemId(),
                                    GUIDReader.getGUID(workParams.getContainerName()),
                                    logbookTypeProcess,
                                    sub.getGlobalStatus(),
                                    sub.getItemId(), " Detail= " + sub.computeStatusMeterMessage(),
                                    GUIDReader.getGUID(workParams.getContainerName()));
                            helper.updateDelegate(sublogbook);
                        }
                    }
                    String itemId = null;
                    if (!itemStatus.getItemId().equals(handlerId)) {
                        itemId = itemStatus.getItemId();
                    }
                    final LogbookOperationParameters sublogbook =
                        LogbookParametersFactory.newLogbookOperationParameters(
                            GUIDFactory.newEventGUID(tenantId),
                            handlerId,
                            GUIDReader.getGUID(workParams.getContainerName()),
                            logbookTypeProcess,
                            itemStatus.getGlobalStatus(),
                            itemId, " Detail= " + itemStatus.computeStatusMeterMessage(),
                            GUIDReader.getGUID(workParams.getContainerName()));
                    if (itemStatus.getData().get(LogbookParameterName.eventDetailData.name()) != null) {
                        eventDetailData =
                            itemStatus.getData().get(LogbookParameterName.eventDetailData.name()).toString();
                        sublogbook.putParameterValue(LogbookParameterName.eventDetailData, eventDetailData);
                    }
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
                        logbookTypeProcess,
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
                processData.updateMessageIdentifier(workParams.getContainerName(), messageIdentifier, tenantId);
                parameters.putParameterValue(LogbookParameterName.objectIdentifierIncome, messageIdentifier);
            } else {
                parameters.putParameterValue(LogbookParameterName.objectIdentifierIncome,
                    processData.getMessageIdentifierByOperationId(workParams.getContainerName(), tenantId));
            }

            parameters.putParameterValue(LogbookParameterName.eventIdentifier,
                GUIDFactory.newEventGUID(tenantId).getId());
            parameters.putParameterValue(LogbookParameterName.outcome, stepResponse.getGlobalStatus().name());           
            parameters.putParameterValue(
                LogbookParameterName.outcomeDetail, VitamLogbookMessages.getOutcomeDetail(step.getStepName(), stepResponse.getGlobalStatus()));            
            parameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                VitamLogbookMessages.getCodeOp(stepResponse.getItemId(), stepResponse.getGlobalStatus()));
            helper.updateDelegate(parameters);
            client.bulkUpdate(workParams.getContainerName(),
                helper.removeUpdateDelegate(workParams.getContainerName()));

            // update the process with the final status
            processData.updateStepStatus(workParams.getContainerName(), step.getId(), stepResponse.getGlobalStatus(),
                tenantId);
            LOGGER.info("End Workflow: " + step.getId() + " Step:" + step.getStepName());
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

