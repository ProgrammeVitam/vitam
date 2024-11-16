/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
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

package fr.gouv.vitam.metadata.core.migration;

import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.InvalidGuidOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamDBException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.Contexts;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.core.MetaDataImpl;
import fr.gouv.vitam.metadata.core.model.MetadataResult;
import fr.gouv.vitam.processing.engine.core.operation.OperationContextException;
import fr.gouv.vitam.processing.engine.core.operation.OperationContextModel;
import fr.gouv.vitam.processing.engine.core.operation.OperationContextMonitor;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.client.WorkspaceType;

import static fr.gouv.vitam.common.json.JsonHandler.writeToInpustream;
import static fr.gouv.vitam.common.model.ProcessAction.RESUME;
import static fr.gouv.vitam.common.model.StatusCode.STARTED;
import static fr.gouv.vitam.common.thread.VitamThreadUtils.getVitamSession;

public class UnitsWithTransferRequestsMigrationService {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(
        UnitsWithTransferRequestsMigrationService.class
    );
    private static final String QUERY_FILE = "query.json";
    private static final int MAX_RETRIES = 1000;
    private static final int WAIT_DELAY_IN_MS_BEFORE_RETRIES = 5_000;

    private final MetaDataImpl metadataImpl;
    private final ProcessingManagementClientFactory processingManagementClientFactory;
    private final LogbookOperationsClientFactory logbookOperationsClientFactory;
    private final WorkspaceClientFactory workspaceClientFactory;

    public UnitsWithTransferRequestsMigrationService(MetaDataImpl metadataImpl) {
        this.metadataImpl = metadataImpl;
        this.processingManagementClientFactory = ProcessingManagementClientFactory.getInstance();
        this.logbookOperationsClientFactory = LogbookOperationsClientFactory.getInstance();
        this.workspaceClientFactory = WorkspaceClientFactory.getInstance(WorkspaceType.VITAM);
    }

    public boolean migrateUnits(int threshold)
        throws InvalidCreateOperationException, VitamException, OperationContextException {
        int tenantId = getVitamSession().getTenantId();
        String operationId = getVitamSession().getRequestId();

        LOGGER.warn(
            "Start migration for units with transfer request for tenant " + tenantId + " with threshold " + threshold
        );

        if (threshold <= 0) {
            throw new IllegalArgumentException("Threshold must be greater than 0");
        }

        long hits = countUnitsToMigrate();
        if (hits == 0) {
            LOGGER.info("No migration required");
            return false;
        }

        if (hits > threshold) {
            throw new IllegalArgumentException(
                String.format("Cannot proceed to migration for tenant %d. Too many units found %d.", tenantId, hits)
            );
        }

        LOGGER.warn("Migration required for tenant {}. Units to migrate: {}", tenantId, hits);

        runUnitUpdateWorkflow(threshold, operationId);

        LOGGER.info("Update process " + operationId + " started for tenant " + tenantId);

        awaitForProcessTermination(operationId);

        LOGGER.info("Update process " + operationId + " completed for tenant " + tenantId);

        return true;
    }

    private long countUnitsToMigrate()
        throws InvalidCreateOperationException, MetaDataExecutionException, InvalidParseOperationException, MetaDataDocumentSizeException, MetaDataNotFoundException, BadRequestException, VitamDBException {
        SelectMultiQuery query = new SelectMultiQuery();
        query.addQueries(QueryHelper.exists("#opts"));
        query.setLimitFilter(0L, 1L);
        query.trackTotalHits(true);
        MetadataResult metadataResult = metadataImpl.selectUnitsByQuery(query.getFinalSelect());

        return metadataResult.getHits().getTotal();
    }

    private void runUnitUpdateWorkflow(int threshold, String operationId)
        throws InvalidGuidOperationException, LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException, ContentAddressableStorageServerException, InvalidParseOperationException, OperationContextException, BadRequestException, InternalServerException, VitamClientException, InvalidCreateOperationException {
        ObjectNode queryDsl = createUpdateQuery(threshold);

        try (
            ProcessingManagementClient processingClient = processingManagementClientFactory.getClient();
            LogbookOperationsClient logbookOperationsClient = logbookOperationsClientFactory.getClient();
            WorkspaceClient workspaceClient = workspaceClientFactory.getClient()
        ) {
            // Init logbook operation
            final LogbookOperationParameters initParameters = LogbookParameterHelper.newLogbookOperationParameters(
                GUIDReader.getGUID(operationId),
                Contexts.MASS_UPDATE_UNIT_DESC.getEventType(),
                GUIDReader.getGUID(operationId),
                LogbookTypeProcess.MASS_UPDATE,
                STARTED,
                VitamLogbookMessages.getCodeOp(Contexts.MASS_UPDATE_UNIT_DESC.getEventType(), STARTED),
                GUIDReader.getGUID(operationId)
            );

            // Add access contract rights
            logbookOperationsClient.create(initParameters);

            workspaceClient.createContainer(operationId);

            // store original query in workspace
            workspaceClient.putObject(
                operationId,
                OperationContextMonitor.OperationContextFileName,
                writeToInpustream(OperationContextModel.get(queryDsl))
            );

            // Internal query is NOT enforced using AccessContractRestrictionHelper
            workspaceClient.putObject(operationId, QUERY_FILE, writeToInpustream(queryDsl));

            // compress file to back up
            OperationContextMonitor.compressInWorkspace(
                workspaceClientFactory,
                operationId,
                Contexts.MASS_UPDATE_UNIT_DESC.getLogbookTypeProcess(),
                OperationContextMonitor.OperationContextFileName
            );

            processingClient.initVitamProcess(operationId, Contexts.MASS_UPDATE_UNIT_DESC.name());

            processingClient.executeOperationProcess(
                operationId,
                Contexts.MASS_UPDATE_UNIT_DESC.name(),
                RESUME.getValue()
            );
        }
    }

    private static ObjectNode createUpdateQuery(int threshold) throws InvalidCreateOperationException {
        UpdateMultiQuery updateMultiQuery = new UpdateMultiQuery();
        updateMultiQuery.addQueries(QueryHelper.exists("#opts"));
        updateMultiQuery.addActions(UpdateActionHelper.unset("NonExistingField-" + GUIDFactory.newGUID()));
        updateMultiQuery.setThreshold((long) threshold);
        return updateMultiQuery.getFinalUpdate();
    }

    private void awaitForProcessTermination(String operationId)
        throws VitamClientException, InternalServerException, BadRequestException, MetaDataExecutionException {
        for (int i = 0; i < MAX_RETRIES; i++) {
            try {
                Thread.sleep(WAIT_DELAY_IN_MS_BEFORE_RETRIES);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }

            try (ProcessingManagementClient processingClient = processingManagementClientFactory.getClient()) {
                ItemStatus operationProcessStatus = processingClient.getOperationProcessStatus(operationId);
                if (!checkProcessCompletion(operationProcessStatus, operationId)) {
                    continue;
                }

                checkCompletedProcessStatus(operationProcessStatus, operationId);
            }
            return;
        }
        throw new MetaDataExecutionException("Process " + operationId + " timed out");
    }

    private static boolean checkProcessCompletion(ItemStatus operationProcessStatus, String operationId)
        throws MetaDataExecutionException {
        switch (operationProcessStatus.getGlobalState()) {
            case RUNNING:
                LOGGER.info("Process " + operationId + " is still running.");
                return false;
            case PAUSE:
                throw new MetaDataExecutionException("Process " + operationId + " is paused.");
            case COMPLETED:
                LOGGER.info("Process " + operationId + " completed.");
                return true;
            default:
                throw new IllegalStateException("Unexpected value: " + operationProcessStatus.getGlobalState());
        }
    }

    private static void checkCompletedProcessStatus(ItemStatus operationProcessStatus, String operationId)
        throws MetaDataExecutionException {
        switch (operationProcessStatus.getGlobalStatus()) {
            case OK:
            case STARTED:
            case WARNING:
                LOGGER.info(
                    "Process {} is completed successfully (status: {})",
                    operationId,
                    operationProcessStatus.getGlobalState()
                );
                break;
            case KO:
                throw new MetaDataExecutionException("Process " + operationId + " is KO.");
            default:
                throw new IllegalStateException("Unexpected value: " + operationProcessStatus.getGlobalStatus());
        }
    }
}
