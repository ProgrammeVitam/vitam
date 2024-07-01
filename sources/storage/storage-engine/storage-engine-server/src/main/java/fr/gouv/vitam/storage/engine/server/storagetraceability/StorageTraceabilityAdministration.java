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
package fr.gouv.vitam.storage.engine.server.storagetraceability;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.alert.AlertService;
import fr.gouv.vitam.common.alert.AlertServiceImpl;
import fr.gouv.vitam.common.exception.VitamFatalRuntimeException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogLevel;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.thread.ExecutorUtils;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.common.timestamp.TimestampGenerator;
import fr.gouv.vitam.logbook.common.exception.TraceabilityException;
import fr.gouv.vitam.logbook.common.traceability.LogbookTraceabilityHelper;
import fr.gouv.vitam.logbook.common.traceability.TraceabilityService;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.storage.driver.model.StorageLogTraceabilityResult;
import fr.gouv.vitam.storage.engine.server.distribution.StorageDistribution;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

/**
 * Business class for Storage Traceability Administration
 */
public class StorageTraceabilityAdministration {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(StorageTraceabilityAdministration.class);
    private static final int MIN_THREAD_POOL_SIZE = 1;

    private final AlertService alertService = new AlertServiceImpl();
    private final TraceabilityStorageService traceabilityLogbookService;
    private final StorageDistribution distribution;
    private final LogbookOperationsClient logbookOperations;
    private final WorkspaceClient workspaceClient;
    private final TimestampGenerator timestampGenerator;
    private final int operationTraceabilityOverlapDelayInSeconds;
    private final int storageLogTraceabilityThreadPoolSize;
    private final File tmpFolder;

    public StorageTraceabilityAdministration(
        TraceabilityStorageService traceabilityLogbookService,
        StorageDistribution distribution,
        String tmpFolder,
        TimestampGenerator timestampGenerator,
        Integer operationTraceabilityOverlapDelay,
        int storageLogTraceabilityThreadPoolSize
    ) {
        this.traceabilityLogbookService = traceabilityLogbookService;
        this.distribution = distribution;
        this.timestampGenerator = timestampGenerator;
        this.workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        this.logbookOperations = LogbookOperationsClientFactory.getInstance().getClient();
        this.operationTraceabilityOverlapDelayInSeconds = validateAndGetTraceabilityOverlapDelay(
            operationTraceabilityOverlapDelay
        );
        this.storageLogTraceabilityThreadPoolSize = storageLogTraceabilityThreadPoolSize;
        this.tmpFolder = new File(tmpFolder);
        if (!this.tmpFolder.exists() && !this.tmpFolder.mkdirs()) {
            throw new VitamFatalRuntimeException("Could not initialize temp folder " + tmpFolder);
        }
    }

    @VisibleForTesting
    public StorageTraceabilityAdministration(
        TraceabilityStorageService traceabilityLogbookService,
        StorageDistribution distribution,
        LogbookOperationsClient mockedLogbookOperations,
        File tmpFolder,
        WorkspaceClient mockedWorkspaceClient,
        TimestampGenerator timestampGenerator,
        Integer operationTraceabilityOverlapDelay,
        int storageLogTraceabilityThreadPoolSize
    ) {
        this.traceabilityLogbookService = traceabilityLogbookService;
        this.distribution = distribution;
        this.logbookOperations = mockedLogbookOperations;
        this.timestampGenerator = timestampGenerator;
        this.workspaceClient = mockedWorkspaceClient;
        this.operationTraceabilityOverlapDelayInSeconds = validateAndGetTraceabilityOverlapDelay(
            operationTraceabilityOverlapDelay
        );
        ParametersChecker.checkValue(
            "Traceability thread pool size",
            storageLogTraceabilityThreadPoolSize,
            MIN_THREAD_POOL_SIZE
        );
        this.storageLogTraceabilityThreadPoolSize = storageLogTraceabilityThreadPoolSize;
        this.tmpFolder = tmpFolder;
    }

    private int validateAndGetTraceabilityOverlapDelay(Integer operationTraceabilityOverlapDelay) {
        if (operationTraceabilityOverlapDelay == null) {
            return 0;
        }
        if (operationTraceabilityOverlapDelay < 0) {
            throw new IllegalArgumentException("Operation traceability overlap delay cannot be negative");
        }
        return operationTraceabilityOverlapDelay;
    }

    public List<StorageLogTraceabilityResult> generateStorageLogTraceabilityOperations(
        String strategyId,
        List<Integer> tenants
    ) throws TraceabilityException {
        int threadPoolSize = Math.min(this.storageLogTraceabilityThreadPoolSize, tenants.size());
        ExecutorService executorService = ExecutorUtils.createScalableBatchExecutorService(threadPoolSize);

        try {
            List<CompletableFuture<StorageLogTraceabilityResult>> completableFutures = new ArrayList<>();

            for (Integer tenantId : tenants) {
                CompletableFuture<StorageLogTraceabilityResult> traceabilityCompletableFuture =
                    CompletableFuture.supplyAsync(
                        () -> generateTraceabilityStorageLogbook(strategyId, tenantId),
                        executorService
                    );
                completableFutures.add(traceabilityCompletableFuture);
            }

            boolean allTenantsSucceeded = true;
            List<StorageLogTraceabilityResult> results = new ArrayList<>();
            for (CompletableFuture<StorageLogTraceabilityResult> completableFuture : completableFutures) {
                try {
                    results.add(completableFuture.get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new TraceabilityException("Storage log traceability interrupted", e);
                } catch (ExecutionException e) {
                    LOGGER.error("Storage log traceability failed", e);
                    allTenantsSucceeded = false;
                }
            }

            if (!allTenantsSucceeded) {
                throw new TraceabilityException("One or more storage log traceability operations failed");
            }

            return results;
        } finally {
            executorService.shutdown();
        }
    }

    /**
     * secure the logbook operation since last traceability
     *
     * @param tenantId tenant Id
     * @param strategyId strategy ID
     */
    private StorageLogTraceabilityResult generateTraceabilityStorageLogbook(String strategyId, Integer tenantId) {
        Thread.currentThread().setName("StorageLogTraceability-" + tenantId);
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        GUID requestId = GUIDFactory.newOperationLogbookGUID(tenantId);
        VitamThreadUtils.getVitamSession().setRequestId(requestId);
        try {
            LogbookTraceabilityHelper traceabilityHelper = new LogbookStorageTraceabilityHelper(
                logbookOperations,
                workspaceClient,
                traceabilityLogbookService,
                distribution,
                requestId,
                operationTraceabilityOverlapDelayInSeconds
            );

            TraceabilityService service = new TraceabilityService(
                timestampGenerator,
                traceabilityHelper,
                tenantId,
                tmpFolder
            );

            service.secureData(strategyId);

            return new StorageLogTraceabilityResult().setTenantId(tenantId).setOperationId(requestId.getId());
        } catch (Exception e) {
            alertService.createAlert(
                VitamLogLevel.ERROR,
                "An error occurred during storage log traceability for tenant " + tenantId
            );
            throw new RuntimeException("An error occurred during storage log traceability for tenant " + tenantId, e);
        }
    }
}
