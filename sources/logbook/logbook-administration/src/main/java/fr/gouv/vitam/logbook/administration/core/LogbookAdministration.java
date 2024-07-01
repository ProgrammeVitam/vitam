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
package fr.gouv.vitam.logbook.administration.core;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.alert.AlertService;
import fr.gouv.vitam.common.alert.AlertServiceImpl;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogLevel;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.thread.ExecutorUtils;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.common.timestamp.TimestampGenerator;
import fr.gouv.vitam.logbook.common.exception.TraceabilityException;
import fr.gouv.vitam.logbook.common.model.TenantLogbookOperationTraceabilityResult;
import fr.gouv.vitam.logbook.common.traceability.TraceabilityService;
import fr.gouv.vitam.logbook.operations.api.LogbookOperations;

import java.io.File;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

/**
 * Business class for Logbook Administration (traceability)
 */
public class LogbookAdministration {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookAdministration.class);

    private final LogbookOperations logbookOperations;
    private final TimestampGenerator timestampGenerator;
    private final AlertService alertService = new AlertServiceImpl();

    private final File tmpFolder;
    private final int operationTraceabilityTemporizationDelayInSeconds;
    private final int operationTraceabilityMaxRenewalDelayInSeconds;
    private final int operationTraceabilityThreadPoolSize;

    @VisibleForTesting
    LogbookAdministration(
        LogbookOperations logbookOperations,
        TimestampGenerator timestampGenerator,
        File tmpFolder,
        Integer operationTraceabilityTemporizationDelayInSeconds,
        Integer operationTraceabilityMaxRenewalDelay,
        ChronoUnit operationTraceabilityMaxRenewalDelayUnit,
        int operationTraceabilityThreadPoolSize
    ) {
        this.operationTraceabilityThreadPoolSize = operationTraceabilityThreadPoolSize;

        ParametersChecker.checkParameter(
            "Missing max renewal delay or unit",
            operationTraceabilityMaxRenewalDelay,
            operationTraceabilityMaxRenewalDelayUnit
        );
        ParametersChecker.checkValue("Invalid max renewal delay", operationTraceabilityMaxRenewalDelay, 1);
        this.logbookOperations = logbookOperations;
        this.timestampGenerator = timestampGenerator;
        this.tmpFolder = tmpFolder;
        this.operationTraceabilityTemporizationDelayInSeconds = validateAndGetTraceabilityTemporizationDelay(
            operationTraceabilityTemporizationDelayInSeconds
        );
        this.operationTraceabilityMaxRenewalDelayInSeconds = (int) Duration.of(
            operationTraceabilityMaxRenewalDelay,
            operationTraceabilityMaxRenewalDelayUnit
        ).toSeconds();
    }

    private static int validateAndGetTraceabilityTemporizationDelay(Integer operationTraceabilityTemporizationDelay) {
        if (operationTraceabilityTemporizationDelay == null) {
            return 0;
        }
        if (operationTraceabilityTemporizationDelay < 0) {
            throw new IllegalArgumentException("Operation traceability temporization delay cannot be negative");
        }
        return operationTraceabilityTemporizationDelay;
    }

    public LogbookAdministration(
        LogbookOperations logbookOperations,
        TimestampGenerator timestampGenerator,
        Integer operationTraceabilityOverlapDelayInSeconds,
        Integer operationTraceabilityMaxRenewalDelay,
        ChronoUnit operationTraceabilityMaxRenewalDelayUnit,
        int operationTraceabilityThreadPoolSize
    ) {
        this(
            logbookOperations,
            timestampGenerator,
            PropertiesUtils.fileFromTmpFolder("secure"),
            operationTraceabilityOverlapDelayInSeconds,
            operationTraceabilityMaxRenewalDelay,
            operationTraceabilityMaxRenewalDelayUnit,
            operationTraceabilityThreadPoolSize
        );
    }

    /**
     * secure the logbook operation since last securisation.
     *
     * @return operation Id if traceability operation has not been skipped
     * @throws TraceabilityException if error on generating secure logbook
     */
    public String generateSecureLogbook(int tenantId) throws TraceabilityException {
        GUID guid = GUIDFactory.newOperationLogbookGUID(tenantId);
        VitamThreadUtils.getVitamSession().setRequestId(guid);

        LOGGER.info("Starting traceability operation for tenant " + tenantId);

        LogbookOperationTraceabilityHelper helper = new LogbookOperationTraceabilityHelper(
            logbookOperations,
            guid,
            operationTraceabilityTemporizationDelayInSeconds,
            operationTraceabilityMaxRenewalDelayInSeconds
        );

        helper.initialize();

        if (!helper.isTraceabilityOperationRequired()) {
            LOGGER.info("No need for traceability operation. No recent activity to secure...");
            return null;
        }

        TraceabilityService generator = new TraceabilityService(timestampGenerator, helper, tenantId, tmpFolder);

        generator.secureData(VitamConfiguration.getDefaultStrategy());

        LOGGER.info("Traceability operation succeeded for tenant " + tenantId);
        return guid.getId();
    }

    public synchronized List<TenantLogbookOperationTraceabilityResult> generateSecureLogbooks(List<Integer> tenants)
        throws TraceabilityException {
        int threadPoolSize = Math.min(this.operationTraceabilityThreadPoolSize, tenants.size());
        ExecutorService executorService = ExecutorUtils.createScalableBatchExecutorService(threadPoolSize);

        try {
            List<CompletableFuture<TenantLogbookOperationTraceabilityResult>> completableFutures = new ArrayList<>();

            for (Integer tenantId : tenants) {
                CompletableFuture<TenantLogbookOperationTraceabilityResult> traceabilityCompletableFuture =
                    CompletableFuture.supplyAsync(
                        () -> {
                            Thread.currentThread().setName("OperationTraceability-" + tenantId);
                            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
                            try {
                                String operationId = generateSecureLogbook(tenantId);
                                return new TenantLogbookOperationTraceabilityResult()
                                    .setTenantId(tenantId)
                                    .setOperationId(operationId);
                            } catch (Exception e) {
                                alertService.createAlert(
                                    VitamLogLevel.ERROR,
                                    "An error occurred during logbook operation traceability for tenant " + tenantId
                                );
                                throw new RuntimeException(
                                    "An error occurred during logbook operation traceability for tenant " + tenantId,
                                    e
                                );
                            }
                        },
                        executorService
                    );
                completableFutures.add(traceabilityCompletableFuture);
            }

            boolean allTenantsSucceeded = true;
            List<TenantLogbookOperationTraceabilityResult> results = new ArrayList<>();
            for (CompletableFuture<TenantLogbookOperationTraceabilityResult> completableFuture : completableFutures) {
                try {
                    results.add(completableFuture.get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new TraceabilityException("Traceability interrupted", e);
                } catch (ExecutionException e) {
                    LOGGER.error("Traceability operation failed", e);
                    allTenantsSucceeded = false;
                }
            }

            if (!allTenantsSucceeded) {
                throw new TraceabilityException("One or more traceability operations failed");
            }

            return results;
        } finally {
            executorService.shutdown();
        }
    }
}
