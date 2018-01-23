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

package fr.gouv.vitam.logbook.administration.core;

import javax.ws.rs.core.Response.Status;

import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.parameters.Contexts;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationsClientHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.exception.LogbookAlreadyExistsException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookDatabaseException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookNotFoundException;
import fr.gouv.vitam.logbook.operations.api.LogbookOperations;
import fr.gouv.vitam.processing.common.ProcessingEntry;
import fr.gouv.vitam.processing.common.parameter.WorkerParameterName;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

/**
 * Business class for Logbook LFC Administration (traceability)
 */
public class LogbookLFCAdministration {


    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookLFCAdministration.class);

    private static String SECURISATION_LC = "LOGBOOK_LC_SECURISATION";

    private final LogbookOperations logbookOperations;
    private final ProcessingManagementClientFactory processingManagementClientFactory;
    private final WorkspaceClientFactory workspaceClientFactory;
    private final int lifecycleTraceabilityOverlapDelayInSeconds;

    /**
     * LogbookLFCAdministration constructor
     *  @param logbookOperations the logbook operations
     * @param processingManagementClientFactory the processManagementClient factory
     * @param workspaceClientFactory the Workspace Client Factory
     * @param lifecycleTraceabilityOverlapDelay
     */
    public LogbookLFCAdministration(LogbookOperations logbookOperations,
        ProcessingManagementClientFactory processingManagementClientFactory,
        WorkspaceClientFactory workspaceClientFactory, Integer lifecycleTraceabilityOverlapDelay) {
        this.logbookOperations = logbookOperations;
        this.processingManagementClientFactory = processingManagementClientFactory;
        this.workspaceClientFactory = workspaceClientFactory;
        this.lifecycleTraceabilityOverlapDelayInSeconds = validateAndGetTraceabilityOverlapDelay(
            lifecycleTraceabilityOverlapDelay);
    }

    private static int validateAndGetTraceabilityOverlapDelay(Integer operationTraceabilityOverlapDelay) {
        if (operationTraceabilityOverlapDelay == null) {
            return 0;
        }
        if (operationTraceabilityOverlapDelay < 0) {
            throw new IllegalArgumentException("Operation traceability overlap delay cannot be negative");
        }
        return operationTraceabilityOverlapDelay;
    }

    /**
     * Secure the logbook Lifecycles since last securisation by launching a workflow.
     * 
     * @return the GUID of the operation
     * @throws VitamException if case of errors launching the workflow
     */
    public synchronized GUID generateSecureLogbookLFC()
        throws VitamException {
        final GUID traceabilityOperationGUID = GUIDFactory.newOperationLogbookGUID(
            VitamThreadUtils.getVitamSession().getTenantId());
        try (ProcessingManagementClient processManagementClient =
            processingManagementClientFactory.getClient()) {
            VitamThreadUtils.getVitamSession().setRequestId(traceabilityOperationGUID);
            final LogbookOperationParameters logbookUpdateParametersStart = LogbookParametersFactory
                .newLogbookOperationParameters(traceabilityOperationGUID, SECURISATION_LC,
                    traceabilityOperationGUID,
                    LogbookTypeProcess.TRACEABILITY,
                    StatusCode.STARTED,
                    VitamLogbookMessages.getCodeOp(SECURISATION_LC, StatusCode.STARTED),
                    traceabilityOperationGUID);
            LogbookOperationsClientHelper.checkLogbookParameters(logbookUpdateParametersStart);
            createLogBookEntry(logbookUpdateParametersStart);
            try {
                createContainer(traceabilityOperationGUID.getId());

                ProcessingEntry processingEntry = new ProcessingEntry(traceabilityOperationGUID.getId(),
                    SECURISATION_LC);
                processingEntry.getExtraParams().put(
                    WorkerParameterName.lifecycleTraceabilityOverlapDelayInSeconds.name(),
                    Integer.toString(lifecycleTraceabilityOverlapDelayInSeconds));
                processManagementClient.initVitamProcess(Contexts.SECURISATION_LC.name(),
                    processingEntry);

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
                    LogbookParametersFactory
                        .newLogbookOperationParameters(traceabilityOperationGUID,
                            SECURISATION_LC,
                            traceabilityOperationGUID,
                            LogbookTypeProcess.TRACEABILITY,
                            StatusCode.KO,
                            VitamLogbookMessages.getCodeOp(SECURISATION_LC,
                                StatusCode.KO),
                            traceabilityOperationGUID);
                LogbookOperationsClientHelper.checkLogbookParameters(logbookUpdateParametersEnd);
                updateLogBookEntry(logbookUpdateParametersEnd);
                throw e;
            }
        }
        return traceabilityOperationGUID;
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
        try (WorkspaceClient workspaceClient = workspaceClientFactory.getClient();) {
            workspaceClient.createContainer(containerName);
        } catch (ContentAddressableStorageAlreadyExistException | ContentAddressableStorageServerException e) {
            LOGGER.error(e.getMessage());
            throw new VitamClientException(e);
        }
    }


}
