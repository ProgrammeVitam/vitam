/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
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
 */
package fr.gouv.vitam.processing.engine.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Stopwatch;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.exception.InvalidGuidOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.Action;
import fr.gouv.vitam.common.model.processing.PauseOrCancelAction;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.performance.PerformanceLogger;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.logbook.common.MessageLogbookEngineHelper;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationsClientHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.processing.common.automation.IEventsProcessEngine;
import fr.gouv.vitam.processing.common.exception.ProcessingEngineException;
import fr.gouv.vitam.processing.common.metrics.CommonProcessingMetrics;
import fr.gouv.vitam.processing.common.model.ProcessStep;
import fr.gouv.vitam.processing.common.parameter.WorkerParameterName;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.distributor.api.ProcessDistributor;
import fr.gouv.vitam.processing.engine.api.ProcessEngine;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import io.prometheus.client.Histogram;
import org.elasticsearch.common.Strings;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;


/**
 * ProcessEngineImpl class manages the context and call a process distributor
 */
public class ProcessEngineImpl implements ProcessEngine {

    private static final PerformanceLogger PERFORMANCE_LOGGER = PerformanceLogger.getInstance();
    public static final String DETAILS = " Detail= ";

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProcessEngineImpl.class);
    private static final String AGENCY_DETAIL = "agIdExt";
    private static final String ORIGIN_AGENCY_NAME = "OriginatingAgency";
    private static final String OBJECTS_LIST_EMPTY = "OBJECTS_LIST_EMPTY";

    private String messageIdentifier;
    private String originatingAgency;
    private final WorkerParameters workerParameters;

    private IEventsProcessEngine stateMachineCallback;
    private final ProcessDistributor processDistributor;
    private final LogbookOperationsClientFactory logbookOperationsClientFactory;
    private final WorkspaceClientFactory workspaceClientFactory;

    public ProcessEngineImpl(WorkerParameters workerParameters, ProcessDistributor processDistributor,
                             LogbookOperationsClientFactory logbookOperationsClientFactory,
                             WorkspaceClientFactory workspaceClientFactory) {
        this.processDistributor = processDistributor;
        this.workerParameters = workerParameters;
        this.logbookOperationsClientFactory = logbookOperationsClientFactory;
        this.workspaceClientFactory = workspaceClientFactory;
    }

    @Override
    public void setStateMachineCallback(IEventsProcessEngine stateMachineCallback) {
        this.stateMachineCallback = stateMachineCallback;
    }

    @Override
    public CompletableFuture<ItemStatus> start(ProcessStep step, WorkerParameters workerParameters)
        throws ProcessingEngineException {

        if (null == stateMachineCallback) {
            throw new ProcessingEngineException("IEventsProcessEngine is required");
        }

        if (null == step) {
            throw new ProcessingEngineException("The parameter step cannot be null");
        }

        if (null != workerParameters) {
            for (final WorkerParameterName key : workerParameters.getMapParameters().keySet()) {
                if (this.workerParameters.getParameterValue(key) == null) {
                    this.workerParameters.putParameterValue(key, workerParameters.getMapParameters().get(key));
                }
            }
        }

        // if the current state is completed or pause, do not start the step
        final String operationId = this.workerParameters.getContainerName();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Start Workflow: " + step.getId() + " Step:" + step.getStepName());
        }
        final int tenantId = ParameterHelper.getTenantParameter();

        final LogbookTypeProcess logbookTypeProcess = this.workerParameters.getLogbookTypeProcess();

        // Prepare the logbook operation
        LogbookOperationParameters logbookParameter =
            logbookBeforeDistributorCall(step, this.workerParameters, tenantId, logbookTypeProcess);

        // update the process monitoring for this step
        if (!PauseOrCancelAction.ACTION_RECOVER.equals(step.getPauseOrCancelAction()) &&
            !PauseOrCancelAction.ACTION_REPLAY.equals(step.getPauseOrCancelAction())) {
            stateMachineCallback.onUpdate(StatusCode.STARTED);
        }

        this.workerParameters.setCurrentStep(step.getStepName());

        this.workerParameters.putParameterValue(WorkerParameterName.workflowStatusKo,
            stateMachineCallback.getCurrentProcessWorkflowStatus().name());

        Stopwatch stopwatch = Stopwatch.createStarted();

        return CompletableFuture
                // Check if the property waitFor is assigned before distributing (if yes it's an entry condition)
                .runAsync(() -> waitForStep(step, operationId, stopwatch), VitamThreadPoolExecutor.getDefaultExecutor())
                // call distributor in async mode
                .thenApplyAsync((e) -> callDistributor(step, this.workerParameters, operationId),
                        VitamThreadPoolExecutor.getDefaultExecutor())
                // When the distributor responds, finalize the logbook persistence
                .thenApply(distributorResponse -> {
                    try {
                        // Do not log if stop, replay or cancel occurs
                        // we have to logbook the event of the current step
                        if (step.getPauseOrCancelAction() ==
                                PauseOrCancelAction.ACTION_PAUSE) {// Do not logbook the event, as the step will be resumed
                            return distributorResponse;
                        }

                        logbookAfterDistributorCall(step, this.workerParameters, tenantId, logbookTypeProcess,
                                logbookParameter, distributorResponse);
                        return distributorResponse;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                // Finally handle event of evaluation (state and status and persistence/remove to/from workspace
                .thenApply(distributorResponse -> {
                    try {
                        if (step.getPauseOrCancelAction() == PauseOrCancelAction.ACTION_CANCEL) {
                            stateMachineCallback.onProcessEngineCancel(this.workerParameters);
                            return distributorResponse;
                        }

                        stateMachineCallback.onProcessEngineCompleteStep(distributorResponse, this.workerParameters);
                    } finally {
                        PERFORMANCE_LOGGER.log(step.getStepName(), stopwatch.elapsed(TimeUnit.MILLISECONDS));
                    }
                    return distributorResponse;
                })
                // When exception occurred
                .exceptionally((e) -> {
                    stateMachineCallback.onError(e);
                    PERFORMANCE_LOGGER.log(step.getStepName(), stopwatch.elapsed(TimeUnit.MILLISECONDS));
                    throw new CompletionException(e);
                });
    }

    private void waitForStep(ProcessStep step, String operationId, Stopwatch stopwatch) {
        if (step.getWaitFor() != null) {
            try (WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {
                // Increase sleepDelay (*2) in every check to avoid lot of calls to workspace
                int sleepDelay = 1;
                final int maxSleepDelay = 60;
                while (!workspaceClient.isExistingObject(operationId, step.getWaitFor()) &&
                        stopwatch.elapsed(TimeUnit.SECONDS) < VitamConfiguration.getProcessEngineWaitForStepTimeout()) {
                    TimeUnit.SECONDS.sleep(sleepDelay);
                    sleepDelay = Math.min(sleepDelay * 2, maxSleepDelay);
                }
                if (stopwatch.elapsed(TimeUnit.SECONDS) > VitamConfiguration.getProcessEngineWaitForStepTimeout()){
                    throw new RuntimeException("The file " + step.getWaitFor() + " was not found in workspace!");
                }
            } catch (ContentAddressableStorageServerException | InterruptedException e) {
                LOGGER.error(e.getMessage());
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Log operation before calling the distributor
     *
     * @param step
     * @param workParams
     * @param tenantId
     * @param logbookTypeProcess
     * @return
     * @throws ProcessingEngineException
     */
    private LogbookOperationParameters logbookBeforeDistributorCall(ProcessStep step, WorkerParameters workParams,
        int tenantId, LogbookTypeProcess logbookTypeProcess) throws ProcessingEngineException {
        try (final LogbookOperationsClient logbookClient = logbookOperationsClientFactory.getClient()) {
            MessageLogbookEngineHelper messageLogbookEngineHelper = new MessageLogbookEngineHelper(logbookTypeProcess);
            LogbookOperationParameters parameters;

            parameters = LogbookParameterHelper.newLogbookOperationParameters(
                GUIDFactory.newEventGUID(tenantId),
                step.getStepName(),
                GUIDReader.getGUID(workParams.getContainerName()),
                logbookTypeProcess,
                StatusCode.OK, // default to OK
                messageLogbookEngineHelper.getLabelOp(step.getStepName(), StatusCode.OK),
                GUIDReader.getGUID(workParams.getRequestId())); // default status code to OK
            parameters.putParameterValue(
                LogbookParameterName.outcomeDetail,
                messageLogbookEngineHelper
                    .getOutcomeDetail(step.getStepName(), StatusCode.OK)); // default outcome to OK

            // FIXME: bug 6542 events maybe lost or operation backup to the offer not done
            if (PauseOrCancelAction.ACTION_RECOVER.equals(step.getPauseOrCancelAction()) ||
                PauseOrCancelAction.ACTION_REPLAY.equals(step.getPauseOrCancelAction())) {
                return parameters;
            }

            // started event
            String eventType = VitamLogbookMessages.getEventTypeStarted(step.getStepName());
            LogbookOperationParameters startedParameters = LogbookParameterHelper.newLogbookOperationParameters(
                GUIDFactory.newEventGUID(tenantId),
                eventType,
                GUIDReader.getGUID(workParams.getContainerName()),
                logbookTypeProcess,
                StatusCode.OK,
                messageLogbookEngineHelper.getLabelOp(eventType, StatusCode.OK),
                GUIDReader.getGUID(workParams.getRequestId()));
            startedParameters.putParameterValue(
                LogbookParameterName.outcomeDetail,
                messageLogbookEngineHelper.getOutcomeDetail(eventType, StatusCode.OK));

            // update logbook op
            logbookClient.update(startedParameters);

            return parameters;
        } catch (LogbookClientBadRequestException | LogbookClientServerException | InvalidGuidOperationException | LogbookClientNotFoundException e) {
            throw new ProcessingEngineException(e);
        }
    }


    /**
     * Call distributor to start the given step
     *
     * @param step
     * @param workParams
     * @param operationId
     * @return
     */
    private ItemStatus callDistributor(ProcessStep step, WorkerParameters workParams, String operationId) {
        Histogram.Timer stepExecutionDurationTimer =
            CommonProcessingMetrics.PROCESS_WORKFLOW_STEP_EXECUTION_DURATION_HISTOGRAM
                .labels(workParams.getLogbookTypeProcess().name(), step.getStepName())
                .startTimer();
        try {
            return processDistributor.distribute(workParams, step, operationId);
        } finally {
            stepExecutionDurationTimer.observeDuration();
        }
    }

    /**
     * Log operation after distributor response
     *
     * @param step
     * @param workParams
     * @param tenantId
     * @param logbookTypeProcess
     * @param parameters
     * @param stepResponse
     * @throws InvalidGuidOperationException
     * @throws LogbookClientNotFoundException
     * @throws LogbookClientBadRequestException
     * @throws LogbookClientServerException
     * @throws InvalidParseOperationException
     */
    private void logbookAfterDistributorCall(ProcessStep step, WorkerParameters workParams, int tenantId,
        LogbookTypeProcess logbookTypeProcess, LogbookOperationParameters parameters, ItemStatus stepResponse)
        throws InvalidGuidOperationException, LogbookClientNotFoundException, LogbookClientBadRequestException,
        LogbookClientServerException, InvalidParseOperationException {
        MessageLogbookEngineHelper messageLogbookEngineHelper = new MessageLogbookEngineHelper(logbookTypeProcess);
        final LogbookOperationsClientHelper helper = new LogbookOperationsClientHelper();
        final String stepEventIdentifier = parameters.getParameterValue(LogbookParameterName.eventIdentifier);

        // handler step logbook
        if (Strings.isNullOrEmpty(messageIdentifier)) {
            if (stepResponse.getData(SedaConstants.TAG_MESSAGE_IDENTIFIER) != null) {
                messageIdentifier = stepResponse.getData(SedaConstants.TAG_MESSAGE_IDENTIFIER).toString();
            }
        }

        JsonNode node;
        if (stepResponse.getData(AGENCY_DETAIL) == null) {
            node = JsonHandler.createObjectNode();
        } else {
            node = JsonHandler.getFromString((String) stepResponse.getData(AGENCY_DETAIL));
        }
        ObjectNode agIdExt;
        try {
            JsonHandler.validate(node.asText());
            agIdExt = (ObjectNode) JsonHandler.getFromString(node.asText());
        } catch (InvalidParseOperationException e) {
            agIdExt = JsonHandler.createObjectNode();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Invalid Json");
            }
        }

        String rightsStatementIdentifier =
            (String) stepResponse.getData(LogbookMongoDbName.rightsStatementIdentifier.getDbname());

        if (rightsStatementIdentifier != null) {
            parameters.putParameterValue(LogbookParameterName.rightsStatementIdentifier, rightsStatementIdentifier);
        }

        if (originatingAgency == null && stepResponse.getData(AGENCY_DETAIL) != null) {
            if (node.get(ORIGIN_AGENCY_NAME) != null) {
                originatingAgency = node.get(ORIGIN_AGENCY_NAME).asText();
            }
        }

        if (!Strings.isNullOrEmpty(messageIdentifier)) {
            stateMachineCallback.onUpdate(messageIdentifier, null);
            parameters.putParameterValue(LogbookParameterName.objectIdentifierIncome, messageIdentifier);
        }

        if (!Strings.isNullOrEmpty(originatingAgency)) {
            stateMachineCallback.onUpdate(null, originatingAgency);
            agIdExt.put(ORIGIN_AGENCY_NAME, originatingAgency);
        }

        if (agIdExt != null && agIdExt.elements().hasNext()) {
            parameters.putParameterValue(LogbookParameterName.agIdExt, agIdExt.toString());
        }

        parameters.putParameterValue(LogbookParameterName.eventIdentifier, stepEventIdentifier);
        parameters.putParameterValue(LogbookParameterName.eventType, step.getStepName());
        parameters.putParameterValue(LogbookParameterName.outcome,
            stepResponse.getGlobalStatus().name());
        parameters.putParameterValue(LogbookParameterName.outcomeDetail,
            messageLogbookEngineHelper.getOutcomeDetail(step.getStepName(), stepResponse.getGlobalStatus()));
        parameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
            messageLogbookEngineHelper.getLabelOp(stepResponse.getItemId(), stepResponse.getGlobalStatus()));

        helper.updateDelegate(parameters);

        // handle actions logbook
        for (final Action action : step.getActions()) {
            final String handlerId = action.getActionDefinition().getActionKey();
            // Each handler could have a list itself => ItemStatus
            final ItemStatus itemStatus = stepResponse.getItemsStatus().get(handlerId);
            if (itemStatus != null) {

                final GUID actionEventIdentifier = GUIDFactory.newEventGUID(tenantId);
                final LogbookOperationParameters actionLogBookParameters =
                    LogbookParameterHelper.newLogbookOperationParameters(
                        actionEventIdentifier,
                        handlerId,
                        GUIDReader.getGUID(workParams.getContainerName()),
                        logbookTypeProcess,
                        itemStatus.getGlobalStatus(),
                        null, DETAILS + itemStatus.computeStatusMeterMessage(),
                        GUIDReader.getGUID(workParams.getRequestId()));
                actionLogBookParameters
                    .putParameterValue(LogbookParameterName.parentEventIdentifier, stepEventIdentifier);

                if (itemStatus.getGlobalOutcomeDetailSubcode() != null) {
                    actionLogBookParameters.putParameterValue(LogbookParameterName.outcomeDetail,
                        messageLogbookEngineHelper.getOutcomeDetail(
                            handlerId + "." + itemStatus.getGlobalOutcomeDetailSubcode(),
                            itemStatus.getGlobalStatus()));
                    actionLogBookParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                        messageLogbookEngineHelper.getLabelOp(
                            handlerId + "." + itemStatus.getGlobalOutcomeDetailSubcode(),
                            itemStatus.getGlobalStatus()) + DETAILS + itemStatus.computeStatusMeterMessage());
                }

                if (itemStatus.getMasterData() != null) {
                    JsonNode value = JsonHandler.toJsonNode(itemStatus.getMasterData());
                    actionLogBookParameters
                        .putParameterValue(LogbookParameterName.masterData, JsonHandler.writeAsString(value));
                }
                if (itemStatus.getEvDetailData() != null && !handlerId.equals("AUDIT_CHECK_OBJECT")) {
                    final String eventDetailData = itemStatus.getEvDetailData();
                    actionLogBookParameters.putParameterValue(LogbookParameterName.eventDetailData, eventDetailData);
                }
                // logbook for action
                helper.updateDelegate(actionLogBookParameters);

                // logbook for composite tasks
                for (final ItemStatus sub : itemStatus.getItemsStatus().values()) {
                    final LogbookOperationParameters subLogBookParameters =
                        LogbookParameterHelper.newLogbookOperationParameters(
                            GUIDFactory.newEventGUID(tenantId),
                            handlerId + "." + sub.getItemId(),
                            GUIDReader.getGUID(workParams.getContainerName()),
                            logbookTypeProcess,
                            sub.getGlobalStatus(),
                            null, DETAILS + sub.computeStatusMeterMessage(),
                            GUIDReader.getGUID(workParams.getRequestId()));
                    subLogBookParameters.putParameterValue(LogbookParameterName.parentEventIdentifier,
                        actionEventIdentifier.getId());

                    if (sub.getGlobalOutcomeDetailSubcode() != null) {
                        subLogBookParameters.putParameterValue(LogbookParameterName.outcomeDetail,
                            messageLogbookEngineHelper.getOutcomeDetail(
                                handlerId + "." + sub.getItemId() + "." + sub.getGlobalOutcomeDetailSubcode(),
                                sub.getGlobalStatus()));
                        subLogBookParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                            messageLogbookEngineHelper.getLabelOp(
                                handlerId + "." + sub.getItemId() + "." + sub.getGlobalOutcomeDetailSubcode(),
                                sub.getGlobalStatus()) + DETAILS + sub.computeStatusMeterMessage());
                    }
                    if (sub.getData(LogbookMongoDbName.rightsStatementIdentifier.getDbname()) != null) {
                        subLogBookParameters.putParameterValue(LogbookParameterName.rightsStatementIdentifier,
                            sub.getData(LogbookMongoDbName.rightsStatementIdentifier.getDbname()).toString());
                    }
                    if (sub.getData(AGENCY_DETAIL) != null) {
                        subLogBookParameters
                            .putParameterValue(LogbookParameterName.agIdExt, sub.getData(AGENCY_DETAIL).toString());
                    }
                    // logbook for subtasks
                    helper.updateDelegate(subLogBookParameters);
                }
            }
        }

        final ItemStatus itemStatusObjectListEmpty = stepResponse.getItemsStatus().get(OBJECTS_LIST_EMPTY);
        if (itemStatusObjectListEmpty != null) {
            final LogbookOperationParameters actionParameters =
                LogbookParameterHelper.newLogbookOperationParameters(
                    GUIDFactory.newEventGUID(tenantId),
                    OBJECTS_LIST_EMPTY,
                    GUIDReader.getGUID(workParams.getContainerName()),
                    logbookTypeProcess,
                    itemStatusObjectListEmpty.getGlobalStatus(),
                    messageLogbookEngineHelper
                        .getLabelOp(OBJECTS_LIST_EMPTY, itemStatusObjectListEmpty.getGlobalStatus()),
                    GUIDReader.getGUID(workParams.getRequestId()));
            actionParameters.putParameterValue(LogbookParameterName.parentEventIdentifier, stepEventIdentifier);
            helper.updateDelegate(actionParameters);
        }

        // If last step then finalize the logbook
        if (Boolean.TRUE.equals(step.getLastStep())) {
            String eventType = workParams.getWorkflowIdentifier();
            final GUID operationGuid = GUIDReader.getGUID(workParams.getContainerName());
            final GUID eventIdentifier = GUIDFactory.newEventGUID(operationGuid);

            // Pre-compute the status code using latest global status of the process workflow
            StatusCode statusCode =
                (stateMachineCallback.getCurrentProcessWorkflowStatus().compareTo(stepResponse.getGlobalStatus()) < 0 ||
                    StatusCode.FATAL.equals(stateMachineCallback.getCurrentProcessWorkflowStatus())) ?
                    stepResponse.getGlobalStatus() :
                    stateMachineCallback.getCurrentProcessWorkflowStatus();


            GUID requestId = GUIDReader.getGUID(workParams.getRequestId());

            final LogbookOperationParameters parametersFinal = LogbookParameterHelper
                .newLogbookOperationParameters(
                    eventIdentifier,
                    eventType,
                    operationGuid,
                    logbookTypeProcess,
                    statusCode,
                    messageLogbookEngineHelper.getLabelOp(eventType, statusCode),
                    requestId);
            parameters.putParameterValue(LogbookParameterName.outcomeDetail,
                messageLogbookEngineHelper.getOutcomeDetail(eventType, statusCode));

            helper.updateDelegate(parametersFinal);
        }

        try (final LogbookOperationsClient logbookClient = logbookOperationsClientFactory.getClient()) {
            logbookClient.bulkUpdate(workParams.getContainerName(),
                helper.removeUpdateDelegate(workParams.getContainerName()));
        }

        // update the process with the final status
        stateMachineCallback.onUpdate(stepResponse.getGlobalStatus());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("End Workflow: " + step.getId() + " Step:" + step.getStepName());
        }
    }
}

