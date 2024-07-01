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

package fr.gouv.vitam.scheduler.server.job;

import com.google.common.base.Stopwatch;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.MetadataType;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.model.LifecycleTraceabilityStatus;
import fr.gouv.vitam.logbook.common.model.TraceabilityType;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import org.apache.commons.lang3.StringUtils;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@DisallowConcurrentExecution
public class TraceabilityLFCJob implements Job {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TraceabilityLFCJob.class);
    private static final String ITEM = "item";

    private final LogbookOperationsClientFactory logbookOperationsClientFactory;

    public TraceabilityLFCJob() {
        this(LogbookOperationsClientFactory.getInstance());
    }

    public TraceabilityLFCJob(LogbookOperationsClientFactory logbookOperationsClientFactory) {
        this.logbookOperationsClientFactory = logbookOperationsClientFactory;
    }

    private boolean secureAllTenants(MetadataType metadataType) {
        List<Integer> tenants = VitamConfiguration.getTenants();

        VitamThreadPoolExecutor defaultExecutor = VitamThreadPoolExecutor.getDefaultExecutor();

        List<CompletableFuture<?>> completableFutures = new ArrayList<>();
        AtomicBoolean atLeastOneTenantReachedMaxCapacity = new AtomicBoolean();
        for (Integer tenant : tenants) {
            completableFutures.add(
                CompletableFuture.runAsync(
                    () -> {
                        if (secureByTenantId(tenant, metadataType)) {
                            atLeastOneTenantReachedMaxCapacity.set(true);
                        }
                    },
                    defaultExecutor
                )
            );
        }

        // Await for all tenants
        CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[0])).join();

        // Retry if a least one reached maximum capacity (to avoid contention)
        return atLeastOneTenantReachedMaxCapacity.get();
    }

    /**
     * Launch securization for a specific tenant
     *
     * @param tenantId to be secured
     * @param metadataType - Unit or ObjectGroup
     * @return true if the tenant need another run to be fully secured
     */
    private boolean secureByTenantId(int tenantId, MetadataType metadataType) {
        String operationId = null;
        try {
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);

            try (LogbookOperationsClient client = logbookOperationsClientFactory.getClient()) {
                operationId = runLfcTraceability(tenantId, metadataType, client);

                if (operationId == null) {
                    LOGGER.info("No " + metadataType + " LFC traceability required for tenant " + tenantId);
                    return false;
                }

                // Await for termination (polling the logbook server)
                LifecycleTraceabilityStatus lifecycleTraceabilityStatus;
                int timeSleep = 1000;
                Stopwatch stopwatch = Stopwatch.createStarted();
                do {
                    TimeUnit.MILLISECONDS.sleep(timeSleep);
                    timeSleep = Math.min(timeSleep * 2, 60000);

                    lifecycleTraceabilityStatus = client.checkLifecycleTraceabilityWorkflowStatus(operationId);

                    if (lifecycleTraceabilityStatus.isPaused()) {
                        LOGGER.error(
                            "LFC traceability operation on tenant {} for operationId {} is in PAUSE state",
                            tenantId,
                            operationId
                        );
                        break;
                    }

                    LOGGER.info(
                        "Traceability operation status for tenant {}, operationId {}, status {})",
                        tenantId,
                        operationId,
                        lifecycleTraceabilityStatus.toString()
                    );
                } while (!lifecycleTraceabilityStatus.isCompleted() && stopwatch.elapsed(TimeUnit.MINUTES) < 30);

                return lifecycleTraceabilityStatus.isMaxEntriesReached();
            }
        } catch (InvalidParseOperationException | LogbookClientServerException e) {
            LOGGER.error(e);
            throw new IllegalStateException("Error when securing Tenant  :  " + tenantId, e);
        } catch (InterruptedException e) {
            LOGGER.error(e);
            throw new IllegalStateException(
                "Error on Thread on Tenant: " + tenantId + " for operationId: " + operationId,
                e
            );
        } finally {
            VitamThreadUtils.getVitamSession().setTenantId(null);
        }
    }

    private String runLfcTraceability(int tenantId, MetadataType metadataType, LogbookOperationsClient client)
        throws LogbookClientServerException, InvalidParseOperationException {
        RequestResponseOK<String> response;
        switch (metadataType) {
            case OBJECTGROUP:
                response = client.traceabilityLfcObjectGroup();
                break;
            case UNIT:
                response = client.traceabilityLfcUnit();
                break;
            default:
                throw new IllegalStateException("Unknown metadata type " + metadataType);
        }

        String operationId = getOperationId(response);
        if (operationId == null) {
            LOGGER.info("Traceability operation not required for tenant {}", tenantId);
        } else {
            LOGGER.info("Traceability operation for tenant started successfully ({})", tenantId, operationId);
        }
        return operationId;
    }

    private static String getOperationId(RequestResponseOK<String> response) {
        return response.getFirstResult();
    }

    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
            MetadataType metadataType = MetadataType.fromName(jobDataMap.get(ITEM).toString());
            while (true) {
                boolean atLeastOneTenantReachedMaxCapacity = secureAllTenants(metadataType);

                if (!atLeastOneTenantReachedMaxCapacity) {
                    LOGGER.info("Done !");
                    break;
                }

                LOGGER.warn("At least one traceability operation reached max capacity. Re-run traceability...");
            }
            LOGGER.info("Traceability LFC for " + metadataType.getName() + " operation is finished");
        } catch (IllegalArgumentException e) {
            LOGGER.error(
                "Expecting traceability type argument. Valid values: {}",
                StringUtils.join(TraceabilityType.values(), ", ")
            );
            throw new IllegalStateException("Invalid command arguments", e);
        } catch (Exception e) {
            throw new JobExecutionException(e);
        }
    }
}
