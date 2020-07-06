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
package fr.gouv.vitam.logbook.administration.core;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.logbook.common.model.LifecycleTraceabilityStatus;
import fr.gouv.vitam.logbook.common.model.TraceabilityEvent;
import fr.gouv.vitam.logbook.common.parameters.Contexts;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationsClientHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookDocument;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookOperation;
import fr.gouv.vitam.logbook.common.server.exception.LogbookAlreadyExistsException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookDatabaseException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookNotFoundException;
import fr.gouv.vitam.logbook.common.traceability.LogbookTraceabilityHelper;
import fr.gouv.vitam.logbook.lifecycles.api.LogbookLifeCycles;
import fr.gouv.vitam.logbook.operations.api.LogbookOperations;
import fr.gouv.vitam.processing.common.ProcessingEntry;
import fr.gouv.vitam.processing.common.parameter.WorkerParameterName;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

import javax.ws.rs.core.Response.Status;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName.eventDateTime;
import static fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName.eventDetailData;

/**
 * Business class for Logbook LFC Administration (traceability)
 */
public class LogbookLFCAdministration {


    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookLFCAdministration.class);
    private static final String LAST_LFC_TRACEABILITY_OPERATION_FILENAME = "lastOperation.json";

    private final LogbookOperations logbookOperations;
    private final LogbookLifeCycles logbookLifeCycles;
    private final ProcessingManagementClientFactory processingManagementClientFactory;
    private final WorkspaceClientFactory workspaceClientFactory;
    private final int lifecycleTraceabilityTemporizationDelayInSeconds;
    private final int traceabilityExpirationInSeconds;
    private final int lifecycleTraceabilityMaxEntries;

    /**
     * LogbookLFCAdministration constructor
     *
     * @param logbookOperations the logbook operations
     * @param processingManagementClientFactory the processManagementClient factory
     * @param workspaceClientFactory the Workspace Client Factory
     * @param lifecycleTraceabilityTemporizationDelay
     * @param lifecycleTraceabilityMaxEntries
     */
    public LogbookLFCAdministration(LogbookOperations logbookOperations,
        LogbookLifeCycles logbookLifeCycles,
        ProcessingManagementClientFactory processingManagementClientFactory,
        WorkspaceClientFactory workspaceClientFactory, Integer lifecycleTraceabilityTemporizationDelay,
        Integer lifecycleTraceabilityMaxRenewalDelay,
        ChronoUnit lifecycleTraceabilityMaxRenewalDelayUnit,
        Integer lifecycleTraceabilityMaxEntries) {
        this.logbookOperations = logbookOperations;
        this.logbookLifeCycles = logbookLifeCycles;
        this.processingManagementClientFactory = processingManagementClientFactory;
        this.workspaceClientFactory = workspaceClientFactory;
        this.lifecycleTraceabilityTemporizationDelayInSeconds = validateAndGetLifecycleTraceabilityTemporizationDelay(
            lifecycleTraceabilityTemporizationDelay);

        ParametersChecker.checkParameter("Missing max renewal delay or unit",
            lifecycleTraceabilityMaxRenewalDelay, lifecycleTraceabilityMaxRenewalDelayUnit);
        ParametersChecker.checkValue("Invalid max renewal delay", lifecycleTraceabilityMaxRenewalDelay, 1);

        this.traceabilityExpirationInSeconds = (int)
            Duration.of(lifecycleTraceabilityMaxRenewalDelay, lifecycleTraceabilityMaxRenewalDelayUnit).toSeconds();
        this.lifecycleTraceabilityMaxEntries = validateAndGetLifecycleTraceabilityMaxEntries(
            lifecycleTraceabilityMaxEntries);
    }

    private static int validateAndGetLifecycleTraceabilityTemporizationDelay(
        Integer lifecycleTraceabilityTemporizationDelay) {
        if (lifecycleTraceabilityTemporizationDelay == null) {
            return 0;
        }
        if (lifecycleTraceabilityTemporizationDelay < 0) {
            throw new IllegalArgumentException("Temporization delay cannot be negative");
        }
        return lifecycleTraceabilityTemporizationDelay;
    }

    private static int validateAndGetLifecycleTraceabilityMaxEntries(Integer lifecycleTraceabilityMaxEntries) {
        if (lifecycleTraceabilityMaxEntries == null) {
            return 0;
        }
        if (lifecycleTraceabilityMaxEntries <= 0) {
            throw new IllegalArgumentException("Max traceability events cannot be negative");
        }
        return lifecycleTraceabilityMaxEntries;
    }

    /**
     * Secure the logbook Lifecycles since last securisation by launching a workflow.
     */
    public synchronized boolean generateSecureLogbookLFC(
        GUID traceabilityOperationGUID,
        LfcTraceabilityType lfcTraceabilityType)
        throws VitamException {

        Contexts workflowContext = getWorkflowContext(lfcTraceabilityType);

        LogbookOperation lastLfcTraceabilityOperation =
            getLastTraceability(lfcTraceabilityType);
        LogbookOperation lastLfcTraceabilityOperationWithZip =
            getLastTraceabilityWithZip(lfcTraceabilityType, lastLfcTraceabilityOperation);

        if (!isNewTraceabilityRequired(lfcTraceabilityType, lastLfcTraceabilityOperation,
            lastLfcTraceabilityOperationWithZip)) {
            LOGGER.info("Traceability operation not required...");
            return false;
        }

        // Start traceability workflow
        return startTraceabilityWorkflow(traceabilityOperationGUID, workflowContext,
            lastLfcTraceabilityOperationWithZip);
    }

    private LogbookOperation getLastTraceability(LfcTraceabilityType lfcTraceabilityType) throws VitamException {
        return logbookOperations.findLastLifecycleTraceabilityOperation(
            getWorkflowContext(lfcTraceabilityType).getEventType(), false);
    }

    private LogbookOperation getLastTraceabilityWithZip(LfcTraceabilityType lfcTraceabilityType,
        LogbookOperation lastLfcTraceabilityOperation) throws VitamException {

        if (lastLfcTraceabilityOperation == null) {
            // Very first traceability operation
            return null;
        }

        // Check if last traceability has a zip file
        TraceabilityEvent traceabilityEvent = getTraceabilityEvent(lastLfcTraceabilityOperation);
        if (traceabilityEvent != null && traceabilityEvent.getFileName() != null) {
            return lastLfcTraceabilityOperation;
        }

        // Retrieve last traceability with zip file
        return logbookOperations.findLastLifecycleTraceabilityOperation(
            getWorkflowContext(lfcTraceabilityType).getEventType(), true);
    }

    private boolean isNewTraceabilityRequired(LfcTraceabilityType lfcTraceabilityType,
        LogbookOperation lastLfcTraceabilityOperation, LogbookOperation lastLfcTraceabilityOperationWithZip)
        throws InvalidParseOperationException {

        if (lastLfcTraceabilityOperation == null) {
            // Very first traceability operation
            return true;
        }

        if (isLastTraceabilityOperationTooOld(lastLfcTraceabilityOperation)) {
            LOGGER.info(lfcTraceabilityType + " LFC traceability required. " +
                "Last traceability operation is too old.");
            return true;
        }

        if (isMaxEntriesReached(lastLfcTraceabilityOperation)) {
            LOGGER.info("Previous traceability operation did not process all data set (MaxEntriesReached)");
            return true;
        }

        if (checkNewLifeCyclesSinceLastTraceabilityOperation(lfcTraceabilityType,
            lastLfcTraceabilityOperationWithZip)) {
            LOGGER.info(lfcTraceabilityType + " LFC traceability required. " +
                "New LFCs found since last traceability operation");
            return true;
        }

        LOGGER.info("Skipping " + lfcTraceabilityType + " LFC traceability. " +
            "No activity since last traceability operation");
        return false;

    }

    private boolean isLastTraceabilityOperationTooOld(LogbookOperation lastLfcTraceabilityOperation) {
        final String evDateTimeStr = (String) lastLfcTraceabilityOperation.get(eventDateTime.getDbname());
        LocalDateTime lastTraceabilityDate = LocalDateUtil.parseMongoFormattedDate(evDateTimeStr);
        LocalDateTime lastTraceabilityOperationValidityDateTime
            = lastTraceabilityDate.plusSeconds(this.traceabilityExpirationInSeconds);
        return lastTraceabilityOperationValidityDateTime.isBefore(LocalDateUtil.now());
    }

    private boolean isMaxEntriesReached(LogbookOperation lastLfcTraceabilityOperation)
        throws InvalidParseOperationException {
        TraceabilityEvent lastLfcTraceabilityDetails = getTraceabilityEvent(lastLfcTraceabilityOperation);
        return lastLfcTraceabilityDetails != null && lastLfcTraceabilityDetails.isMaxEntriesReached();
    }

    private boolean checkNewLifeCyclesSinceLastTraceabilityOperation(LfcTraceabilityType lfcTraceabilityType,
        LogbookOperation lastLfcTraceabilityOperationWithZip) throws InvalidParseOperationException {

        LocalDateTime traceabilityStartDate = getTraceabilityStartDate(lastLfcTraceabilityOperationWithZip);

        LocalDateTime traceabilityEndDate = LocalDateUtil.now()
            .minusSeconds(this.lifecycleTraceabilityTemporizationDelayInSeconds);

        switch (lfcTraceabilityType) {
            case Unit:
                return this.logbookLifeCycles.checkUnitLifecycleEntriesExistenceByLastPersistedDate(
                    LocalDateUtil.getFormattedDateForMongo(traceabilityStartDate),
                    LocalDateUtil.getFormattedDateForMongo(traceabilityEndDate));

            case ObjectGroup:
                return this.logbookLifeCycles.checkObjectGroupLifecycleEntriesExistenceByLastPersistedDate(
                    LocalDateUtil.getFormattedDateForMongo(traceabilityStartDate),
                    LocalDateUtil.getFormattedDateForMongo(traceabilityEndDate));
            default:
                throw new IllegalStateException("Unexpected value: " + lfcTraceabilityType);
        }
    }

    private LocalDateTime getTraceabilityStartDate(LogbookOperation lastLfcTraceabilityOperationWithZip)
        throws InvalidParseOperationException {
        LocalDateTime traceabilityStartDate;
        if (lastLfcTraceabilityOperationWithZip == null) {
            traceabilityStartDate = LogbookTraceabilityHelper.INITIAL_START_DATE;
        } else {

            TraceabilityEvent lastLfcTraceabilityWithZipEventDetails =
                getTraceabilityEvent(lastLfcTraceabilityOperationWithZip);

            if (lastLfcTraceabilityWithZipEventDetails == null) {
                throw new IllegalStateException("Last traceability with zip must have event details " +
                    lastLfcTraceabilityOperationWithZip.getId());
            }
            traceabilityStartDate = LocalDateUtil.parseMongoFormattedDate(
                lastLfcTraceabilityWithZipEventDetails.getEndDate());
        }
        return traceabilityStartDate;
    }

    private boolean startTraceabilityWorkflow(GUID traceabilityOperationGUID, Contexts workflowContext,
        LogbookOperation lastLfcTraceabilityOperationWithZip)
        throws VitamClientException, InternalServerException, BadRequestException {
        createContainer(traceabilityOperationGUID.getId());

        persistLastLfcTraceability(traceabilityOperationGUID.getId(), lastLfcTraceabilityOperationWithZip);

        try (ProcessingManagementClient processManagementClient =
            processingManagementClientFactory.getClient()) {
            final LogbookOperationParameters logbookUpdateParametersStart = LogbookParameterHelper
                .newLogbookOperationParameters(traceabilityOperationGUID, workflowContext.getEventType(),
                    traceabilityOperationGUID,
                    LogbookTypeProcess.TRACEABILITY,
                    StatusCode.STARTED,
                    VitamLogbookMessages.getCodeOp(workflowContext.getEventType(), StatusCode.STARTED),
                    traceabilityOperationGUID);
            LogbookOperationsClientHelper.checkLogbookParameters(logbookUpdateParametersStart);
            createLogBookEntry(logbookUpdateParametersStart);
            try {

                ProcessingEntry processingEntry =
                    new ProcessingEntry(traceabilityOperationGUID.getId(), workflowContext.name());
                processingEntry.getExtraParams().put(
                    WorkerParameterName.lifecycleTraceabilityTemporizationDelayInSeconds.name(),
                    Integer.toString(lifecycleTraceabilityTemporizationDelayInSeconds));
                processingEntry.getExtraParams().put(
                    WorkerParameterName.lifecycleTraceabilityMaxEntries.name(),
                    Integer.toString(lifecycleTraceabilityMaxEntries));

                // No need to backup operation context.
                processManagementClient.initVitamProcess(processingEntry);

                LOGGER.debug("Started Traceability in Resource");
                RequestResponse<ItemStatus> ret =
                    processManagementClient
                        .updateOperationActionProcess(ProcessAction.RESUME.getValue(),
                            traceabilityOperationGUID.getId());

                if (Status.ACCEPTED.getStatusCode() != ret.getStatus()) {
                    throw new VitamClientException("Process could not be executed");
                }

                return true;

            } catch (InternalServerException | VitamClientException | BadRequestException e) {
                LOGGER.error(e);
                final LogbookOperationParameters logbookUpdateParametersEnd =
                    LogbookParameterHelper
                        .newLogbookOperationParameters(traceabilityOperationGUID,
                            workflowContext.getEventType(),
                            traceabilityOperationGUID,
                            LogbookTypeProcess.TRACEABILITY,
                            StatusCode.KO,
                            VitamLogbookMessages.getCodeOp(workflowContext.getEventType(),
                                StatusCode.KO),
                            traceabilityOperationGUID);
                LogbookOperationsClientHelper.checkLogbookParameters(logbookUpdateParametersEnd);
                updateLogBookEntry(logbookUpdateParametersEnd);
                throw e;
            }
        }
    }

    private Contexts getWorkflowContext(LfcTraceabilityType lfcTraceabilityType) {
        switch (lfcTraceabilityType) {
            case Unit:
                return Contexts.UNIT_LFC_TRACEABILITY;
            case ObjectGroup:
                return Contexts.OBJECTGROUP_LFC_TRACEABILITY;
            default:
                throw new IllegalStateException("Unknown traceability type " + lfcTraceabilityType);
        }
    }

    private TraceabilityEvent getTraceabilityEvent(LogbookOperation traceabilityOperation)
        throws InvalidParseOperationException {
        final String evDetDataStr = (String) traceabilityOperation.get(eventDetailData.getDbname());
        return evDetDataStr == null ? null : JsonHandler.getFromString(evDetDataStr, TraceabilityEvent.class);
    }

    /**
     * Create a LogBook Entry related to object's creation
     *
     * @param logbookParametersStart
     */
    private void createLogBookEntry(LogbookOperationParameters logbookParametersStart) {
        try {
            logbookOperations.create(logbookParametersStart);
        } catch (LogbookAlreadyExistsException | LogbookDatabaseException e) {
            LOGGER.error(e.getMessage());
        }
    }

    /**
     * Create a LogBook Entry related to object's update
     *
     * @param logbookParametersEnd
     */
    private void updateLogBookEntry(LogbookOperationParameters logbookParametersEnd) {
        try {
            logbookOperations.update(logbookParametersEnd);
        } catch (LogbookNotFoundException | LogbookDatabaseException e) {
            LOGGER.error(e.getMessage());
        }
    }

    /**
     * Create a container in the workspace, this is necessary so the workflow could be executed
     *
     * @param containerName name of the container
     * @throws VitamClientException in case container couldnt be created
     */
    private void createContainer(String containerName) throws VitamClientException {
        try (WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {
            workspaceClient.createContainer(containerName);
        } catch (ContentAddressableStorageServerException e) {
            throw new VitamClientException(e);
        }
    }

    private void persistLastLfcTraceability(String containerName, LogbookOperation lastLfcTraceabilityOperation)
        throws VitamClientException {

        try (WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {

            JsonNode traceabilityOperationJson;
            if (lastLfcTraceabilityOperation == null) {
                // empty json file
                traceabilityOperationJson = JsonHandler.createObjectNode();
            } else {
                traceabilityOperationJson = JsonHandler.toJsonNode(lastLfcTraceabilityOperation);
            }

            workspaceClient.putObject(containerName, LAST_LFC_TRACEABILITY_OPERATION_FILENAME,
                JsonHandler.fromPojoToBytes(traceabilityOperationJson));
        } catch (ContentAddressableStorageServerException | InvalidParseOperationException e) {
            throw new VitamClientException(e);
        }
    }

    /**
     * Check lifecycle traceability status
     *
     * @param operationId the process id
     * @return the lifecycle traceability status
     */
    public LifecycleTraceabilityStatus checkLifecycleTraceabilityStatus(String operationId)
        throws VitamException, InvalidCreateOperationException {

        try (ProcessingManagementClient processManagementClient = processingManagementClientFactory.getClient()) {

            ItemStatus processStatus = processManagementClient.getOperationProcessStatus(operationId);

            boolean isCompleted = (processStatus.getGlobalState() == ProcessState.COMPLETED);
            boolean isOK = processStatus.getGlobalStatus() == StatusCode.OK;

            LifecycleTraceabilityStatus lifecycleTraceabilityStatus = new LifecycleTraceabilityStatus();
            lifecycleTraceabilityStatus.setCompleted(isCompleted);
            lifecycleTraceabilityStatus
                .setOutcome(processStatus.getGlobalState().name() + "." + processStatus.getGlobalStatus().name());

            if (isCompleted && isOK) {

                Select selectQuery = new Select();
                selectQuery.setQuery(QueryHelper.eq(LogbookMongoDbName.eventIdentifier.getDbname(), operationId));
                List<LogbookOperation> operations
                    = logbookOperations.select(selectQuery.getFinalSelect());

                if (operations.isEmpty()) {
                    throw new LogbookNotFoundException("Could not find logbook operation " + operationId);
                }

                String evDetData = operations.get(0).getString(LogbookDocument.EVENT_DETAILS);
                TraceabilityEvent traceabilityEvent = JsonHandler.getFromString(evDetData, TraceabilityEvent.class);

                lifecycleTraceabilityStatus.setMaxEntriesReached(traceabilityEvent.getMaxEntriesReached());
            }

            return lifecycleTraceabilityStatus;
        }
    }
}
