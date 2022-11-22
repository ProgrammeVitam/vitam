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
package fr.gouv.vitam.storage.engine.server.storagelog;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.alert.AlertService;
import fr.gouv.vitam.common.alert.AlertServiceImpl;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.logging.VitamLogLevel;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.ExecutorUtils;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationsClientHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.storage.driver.model.StorageLogBackupResult;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.storage.engine.server.distribution.StorageDistribution;
import fr.gouv.vitam.storage.engine.server.rest.StorageConfiguration;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

/**
 * Business class for Storage Log Administration (backup)
 */
public class StorageLogAdministration {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(StorageLogAdministration.class);

    public static final String STORAGE_WRITE_BACKUP = "STORAGE_BACKUP";
    public static final String STORAGE_ACCESS_BACKUP = "STORAGE_ACCESS_BACKUP";

    private final WorkspaceClientFactory workspaceClientFactory;
    private final LogbookOperationsClientFactory logbookOperationsClientFactory;

    private final AlertService alertService = new AlertServiceImpl();
    private final StorageLog storageLogService;
    private final int storageLogBackupThreadPoolSize;

    private final StorageDistribution distribution;


    public StorageLogAdministration(StorageLog storageLogService, StorageDistribution distribution,
        StorageConfiguration configuration) {
        this(storageLogService, distribution, configuration, WorkspaceClientFactory.getInstance(),
            LogbookOperationsClientFactory.getInstance());
    }

    @VisibleForTesting
    public StorageLogAdministration(StorageLog storageLogService, StorageDistribution distribution,
        StorageConfiguration configuration, WorkspaceClientFactory workspaceClientFactory,
        LogbookOperationsClientFactory logbookOperationsClientFactory) {
        this.storageLogService = storageLogService;
        this.distribution = distribution;
        this.workspaceClientFactory = workspaceClientFactory;
        this.logbookOperationsClientFactory = logbookOperationsClientFactory;
        this.storageLogBackupThreadPoolSize = configuration.getStorageLogBackupThreadPoolSize();
    }


    public List<StorageLogBackupResult> backupStorageWriteLog(String strategyId,
        List<Integer> tenants) throws StorageLogException {
        return backupStorageLog(strategyId, true, tenants);
    }


    public List<StorageLogBackupResult> backupStorageAccessLog(String strategyId,
        List<Integer> tenants) throws StorageLogException {
        return backupStorageLog(strategyId, false, tenants);
    }

    /**
     * backup the log files since last backup: <br/>
     * * Link the appender to a new file in order to continue to log access/write during the operation <br/>
     * * Copy previous log files from Storage to Offers </br>
     * * Delete old files from Storage
     *
     * @param strategyId strategyId
     * @param backupWriteLog backupWriteLog
     * @param tenants tenant list to backup
     * @return backup result list
     * @throws StorageLogException if storage log backup failed
     */
    private synchronized List<StorageLogBackupResult> backupStorageLog(String strategyId, Boolean backupWriteLog,
        List<Integer> tenants)
        throws StorageLogException {

        String operationType = backupWriteLog ? "StorageWriteLog" : "StorageAccessLog";

        int threadPoolSize = Math.min(this.storageLogBackupThreadPoolSize, tenants.size());
        ExecutorService executorService = ExecutorUtils.createScalableBatchExecutorService(threadPoolSize);

        try {
            List<CompletableFuture<StorageLogBackupResult>> completableFutures = new ArrayList<>();

            for (Integer tenantId : tenants) {
                CompletableFuture<StorageLogBackupResult> traceabilityCompletableFuture =
                    CompletableFuture.supplyAsync(() -> {
                        Thread.currentThread().setName(operationType + "-" + tenantId);
                        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
                        try {
                            String operationId = backupStorageLog(strategyId, backupWriteLog, tenantId);
                            return new StorageLogBackupResult()
                                .setTenantId(tenantId)
                                .setOperationId(operationId);
                        } catch (Exception e) {
                            alertService.createAlert(VitamLogLevel.ERROR,
                                "An error occurred during " + operationType + " for tenant " + tenantId);
                            throw new RuntimeException(
                                "An error occurred during " + operationType + " for tenant " + tenantId);
                        }
                    }, executorService);
                completableFutures.add(traceabilityCompletableFuture);
            }

            boolean allTenantsSucceeded = true;
            List<StorageLogBackupResult> results = new ArrayList<>();
            for (CompletableFuture<StorageLogBackupResult> completableFuture : completableFutures) {
                try {
                    results.add(completableFuture.get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new StorageLogException(operationType + " interrupted", e);
                } catch (ExecutionException e) {
                    LOGGER.error(operationType + " failed", e);
                    allTenantsSucceeded = false;
                }
            }

            if (!allTenantsSucceeded) {
                throw new StorageLogException("One or more " + operationType + " operations failed");
            }

            return results;

        } finally {
            executorService.shutdown();
        }
    }

    private String backupStorageLog(String strategyId, Boolean backupWriteLog, int tenantId)
        throws IOException, StorageLogException,
        LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException {

        final LogbookOperationsClientHelper helper = new LogbookOperationsClientHelper();
        GUID eip = GUIDFactory.newOperationLogbookGUID(tenantId);
        VitamThreadUtils.getVitamSession().setRequestId(eip);
        try {

            String evType;

            if (backupWriteLog) {
                evType = STORAGE_WRITE_BACKUP;
            } else {
                evType = STORAGE_ACCESS_BACKUP;
            }

            createLogbookOperationStarted(helper, eip, evType);

            List<LogInformation> info = storageLogService.rotateLogFile(tenantId, backupWriteLog);

            for (LogInformation logInformation : info) {
                storeLogFile(helper, strategyId, tenantId, eip, logInformation, storageLogService, evType,
                    backupWriteLog);
            }

            createLogbookOperationEvent(helper, eip, evType, StatusCode.OK);
            return eip.getId();

        } catch (LogbookClientNotFoundException | LogbookClientAlreadyExistsException e) {
            throw new StorageLogException(e);
        } finally {
            logbookOperationsClientFactory.getClient()
                .bulkCreate(eip.getId(), helper.removeCreateDelegate(eip.getId()));
        }
    }

    private void storeLogFile(LogbookOperationsClientHelper helper, String strategyId, Integer tenantId, GUID eip,
        LogInformation logInformation, StorageLog storageLogService, String evType, boolean isWriteOperation)
        throws LogbookClientNotFoundException, StorageLogException {
        LOGGER.info("Storing log file " + logInformation.getPath() + " -- " + isWriteOperation);

        String fileName = tenantId + "_" + storageLogService.getFileName(isWriteOperation) + "_"
            + logInformation.getBeginTime().format(getDateTimeFormatter()) + "_"
            + logInformation.getEndTime().format(getDateTimeFormatter()) + "_"
            + eip.toString() + ".log";

        try (InputStream inputStream =
            new BufferedInputStream(new FileInputStream(logInformation.getPath().toFile()));
            WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {

            String containerName = GUIDFactory.newGUID().toString();

            workspaceClient.createContainer(containerName);

            try {

                workspaceClient.putObject(containerName, fileName, inputStream);

                try {

                    final ObjectDescription description = new ObjectDescription();
                    description.setWorkspaceContainerGUID(containerName);
                    description.setWorkspaceObjectURI(fileName);

                    distribution.storeDataInAllOffers(strategyId, fileName, description,
                        isWriteOperation ? DataCategory.STORAGELOG : DataCategory.STORAGEACCESSLOG, null);

                } catch (StorageException e) {
                    LOGGER.error("unable to store log file", e);
                    createLogbookOperationEvent(helper, eip, evType, StatusCode.FATAL);
                    throw new StorageLogException(e);
                }

                if (!Files.deleteIfExists(logInformation.getPath())) {
                    LOGGER.warn("Could not delete local storage file " + logInformation.getPath().toAbsolutePath());
                }

            } finally {
                try {
                    workspaceClient.deleteContainer(containerName, true);
                } catch (ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException e) {
                    LOGGER.error(
                        String.format("Unable to cleanup file from workspace %s/%s", containerName, fileName), e);
                }
            }


        } catch (ContentAddressableStorageServerException | IOException e) {
            LOGGER.error("Unable to create container", e);
            createLogbookOperationEvent(helper, eip, evType, StatusCode.FATAL);
            throw new StorageLogException(e);
        }
    }

    private void createLogbookOperationStarted(LogbookOperationsClientHelper helper, GUID eip, String evType)
        throws LogbookClientAlreadyExistsException {
        final LogbookOperationParameters logbookOperationParameters = LogbookParameterHelper
            .newLogbookOperationParameters(eip, evType, eip, LogbookTypeProcess.STORAGE_BACKUP,
                StatusCode.STARTED,
                VitamLogbookMessages.getCodeOp(evType, StatusCode.STARTED), eip);
        logbookOperationParameters.putParameterValue(LogbookParameterName.outcomeDetail, evType +
            "." + StatusCode.STARTED);

        LogbookOperationsClientHelper.checkLogbookParameters(logbookOperationParameters);
        helper.createDelegate(logbookOperationParameters);
    }

    private void createLogbookOperationEvent(LogbookOperationsClientHelper helper, GUID parentEventId, String eventType,
        StatusCode statusCode) throws LogbookClientNotFoundException {

        final LogbookOperationParameters logbookOperationParameters = LogbookParameterHelper
            .newLogbookOperationParameters(GUIDFactory.newEventGUID(parentEventId), eventType, parentEventId,
                LogbookTypeProcess.STORAGE_BACKUP,
                statusCode,
                VitamLogbookMessages.getCodeOp(eventType, statusCode), parentEventId);
        logbookOperationParameters.putParameterValue(LogbookParameterName.outcomeDetail, eventType +
            "." + statusCode);

        LogbookOperationsClientHelper.checkLogbookParameters(logbookOperationParameters);
        helper.updateDelegate(logbookOperationParameters);
    }

    private DateTimeFormatter getDateTimeFormatter() {
        return DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").withZone(ZoneOffset.UTC);
    }
}
