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

import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
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
import fr.gouv.vitam.logbook.operations.api.LogbookOperations;
import fr.gouv.vitam.processing.common.ProcessingEntry;
import fr.gouv.vitam.processing.common.parameter.WorkerParameterName;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

import javax.ws.rs.core.Response.Status;
import java.util.List;

/**
 * Business class for Logbook LFC Administration (traceability)
 */
public class LogbookLFCAdministration {


    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookLFCAdministration.class);

    private final LogbookOperations logbookOperations;
    private final ProcessingManagementClientFactory processingManagementClientFactory;
    private final WorkspaceClientFactory workspaceClientFactory;
    private final int lifecycleTraceabilityTemporizationDelayInSeconds;
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
        ProcessingManagementClientFactory processingManagementClientFactory,
        WorkspaceClientFactory workspaceClientFactory, Integer lifecycleTraceabilityTemporizationDelay,
        Integer lifecycleTraceabilityMaxEntries) {
        this.logbookOperations = logbookOperations;
        this.processingManagementClientFactory = processingManagementClientFactory;
        this.workspaceClientFactory = workspaceClientFactory;
        this.lifecycleTraceabilityTemporizationDelayInSeconds = validateAndGetLifecycleTraceabilityTemporizationDelay(
            lifecycleTraceabilityTemporizationDelay);
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
     *
     * @param lfcTraceabilityType
     * @return the GUID of the operation
     * @throws VitamException if case of errors launching the workflow
     */
    public synchronized GUID generateSecureLogbookLFC(
        LfcTraceabilityType lfcTraceabilityType)
        throws VitamException {

        Contexts workflowContext = getWorkflowContext(lfcTraceabilityType);

        final GUID traceabilityOperationGUID = GUIDFactory.newOperationLogbookGUID(
            VitamThreadUtils.getVitamSession().getTenantId());
        try (ProcessingManagementClient processManagementClient =
            processingManagementClientFactory.getClient()) {
            // FIXME: 01/01/2020 request id should be set in the externals
            VitamThreadUtils.getVitamSession().setRequestId(traceabilityOperationGUID);
            final LogbookOperationParameters logbookUpdateParametersStart = LogbookParameterHelper
                .newLogbookOperationParameters(traceabilityOperationGUID, workflowContext.getEventType(),
                    traceabilityOperationGUID,
                    LogbookTypeProcess.TRACEABILITY,
                    StatusCode.STARTED,
                    VitamLogbookMessages.getCodeOp(workflowContext.getEventType(), StatusCode.STARTED),
                    traceabilityOperationGUID);
            LogbookOperationsClientHelper.checkLogbookParameters(logbookUpdateParametersStart);
            logbookOperations.create(logbookUpdateParametersStart);
            try {
                createContainer(traceabilityOperationGUID.getId());

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
                logbookOperations.update(logbookUpdateParametersEnd);
                throw e;
            }
        }
        return traceabilityOperationGUID;
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

    /**
     * Create a container in the workspace, this is necessary so the workflow could be executed
     *
     * @param containerName name of the container
     * @throws VitamClientException in case container couldnt be created
     */
    private void createContainer(String containerName) throws VitamClientException {
        try (WorkspaceClient workspaceClient = workspaceClientFactory.getClient();) {
            workspaceClient.createContainer(containerName);
        } catch (ContentAddressableStorageServerException e) {
            LOGGER.error(e.getMessage());
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
