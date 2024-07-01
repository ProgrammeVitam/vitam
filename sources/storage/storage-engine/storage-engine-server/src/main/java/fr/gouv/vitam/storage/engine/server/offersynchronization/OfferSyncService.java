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
package fr.gouv.vitam.storage.engine.server.offersynchronization;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.thread.ExecutorUtils;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.request.OfferPartialSyncItem;
import fr.gouv.vitam.storage.engine.server.distribution.StorageDistribution;
import fr.gouv.vitam.storage.engine.server.rest.StorageConfiguration;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manages offer synchronization service.
 */
public class OfferSyncService implements AutoCloseable {

    /**
     * Vitam Logger.
     */
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(OfferSyncService.class);

    private final RestoreOfferBackupService restoreOfferBackupService;
    private final StorageDistribution distribution;
    private final int bulkSize;
    private final int offerSyncNumberOfRetries;
    private final int offerSyncFirstAttemptWaitingTime;
    private final int offerSyncWaitingTime;
    private final int offerSyncAccessRequestCheckWaitingTime;

    private final ExecutorService executor;
    private final AtomicReference<OfferSyncProcess> lastOfferSyncService = new AtomicReference<>(null);

    /**
     * Constructor.
     */
    public OfferSyncService(StorageDistribution distribution, StorageConfiguration storageConfiguration) {
        this(
            new RestoreOfferBackupService(distribution),
            distribution,
            storageConfiguration.getOfferSynchronizationBulkSize(),
            storageConfiguration.getOfferSyncThreadPoolSize(),
            storageConfiguration.getOfferSyncNumberOfRetries(),
            storageConfiguration.getOfferSyncFirstAttemptWaitingTime(),
            storageConfiguration.getOfferSyncWaitingTime(),
            storageConfiguration.getOfferSyncAccessRequestCheckWaitingTime()
        );
    }

    /**
     * Test constructor.
     */
    @VisibleForTesting
    OfferSyncService(
        RestoreOfferBackupService restoreOfferBackupService,
        StorageDistribution distribution,
        int bulkSize,
        int offerSyncThreadPoolSize,
        int offerSyncNumberOfRetries,
        int offerSyncFirstAttemptWaitingTime,
        int offerSyncWaitingTime,
        int offerSyncAccessRequestCheckWaitingTime
    ) {
        this.restoreOfferBackupService = restoreOfferBackupService;
        this.distribution = distribution;
        this.bulkSize = bulkSize;
        this.offerSyncNumberOfRetries = offerSyncNumberOfRetries;
        this.offerSyncFirstAttemptWaitingTime = offerSyncFirstAttemptWaitingTime;
        this.offerSyncWaitingTime = offerSyncWaitingTime;
        this.offerSyncAccessRequestCheckWaitingTime = offerSyncAccessRequestCheckWaitingTime;
        this.executor = ExecutorUtils.createScalableBatchExecutorService(offerSyncThreadPoolSize);
    }

    public boolean startSynchronization(
        String sourceOffer,
        String targetOffer,
        String strategyId,
        List<OfferPartialSyncItem> items
    ) {
        OfferSyncProcess offerSyncProcess = createOfferSyncProcess();

        OfferSyncProcess currentOfferSyncProcess = lastOfferSyncService.updateAndGet(previousOfferSyncService -> {
            if (previousOfferSyncService != null && previousOfferSyncService.isRunning()) {
                return previousOfferSyncService;
            }
            return offerSyncProcess;
        });

        // Ensure no concurrent synchronization service running
        if (offerSyncProcess != currentOfferSyncProcess) {
            LOGGER.error("Another synchronization workflow is already running " + currentOfferSyncProcess.toString());
            return false;
        }

        runSynchronizationAsync(sourceOffer, targetOffer, strategyId, items, offerSyncProcess);

        return true;
    }

    void runSynchronizationAsync(
        String sourceOffer,
        String targetOffer,
        String strategyId,
        List<OfferPartialSyncItem> items,
        OfferSyncProcess offerSyncProcess
    ) {
        int tenantId = VitamThreadUtils.getVitamSession().getTenantId();
        String requestId = VitamThreadUtils.getVitamSession().getRequestId();

        VitamThreadPoolExecutor.getDefaultExecutor()
            .execute(() -> {
                try {
                    VitamThreadUtils.getVitamSession().setTenantId(tenantId);
                    VitamThreadUtils.getVitamSession().setRequestId(requestId);

                    offerSyncProcess.synchronize(executor, sourceOffer, targetOffer, strategyId, items);
                } catch (Exception e) {
                    LOGGER.error("An error occurred during partial synchronization process execution", e);
                }
            });
    }

    /**
     * Synchronize an offer from another using the offset.
     *
     * @param sourceOffer the identifier of the source offer
     * @param targetOffer the identifier of the target offer
     * @param strategyId the identifier of the strategy containing the two offers
     * @param offset the offset of the process of the synchronisation
     */
    public boolean startSynchronization(
        String sourceOffer,
        String targetOffer,
        String strategyId,
        DataCategory dataCategory,
        Long offset
    ) {
        OfferSyncProcess offerSyncProcess = createOfferSyncProcess();

        OfferSyncProcess currentOfferSyncProcess = lastOfferSyncService.updateAndGet(previousOfferSyncService -> {
            if (previousOfferSyncService != null && previousOfferSyncService.isRunning()) {
                return previousOfferSyncService;
            }
            return offerSyncProcess;
        });

        // Ensure no concurrent synchronization service running
        if (offerSyncProcess != currentOfferSyncProcess) {
            LOGGER.error("Another synchronization workflow is already running " + currentOfferSyncProcess.toString());
            return false;
        }

        LOGGER.info(
            String.format(
                "Start the synchronization process of the new offer {%s} from the source offer {%s} fro category {%s}.",
                targetOffer,
                sourceOffer,
                dataCategory
            )
        );

        runSynchronizationAsync(sourceOffer, targetOffer, strategyId, dataCategory, offset, offerSyncProcess);

        return true;
    }

    OfferSyncProcess createOfferSyncProcess() {
        return new OfferSyncProcess(
            restoreOfferBackupService,
            distribution,
            bulkSize,
            offerSyncNumberOfRetries,
            offerSyncFirstAttemptWaitingTime,
            offerSyncWaitingTime,
            offerSyncAccessRequestCheckWaitingTime
        );
    }

    void runSynchronizationAsync(
        String sourceOffer,
        String targetOffer,
        String strategyId,
        DataCategory dataCategory,
        Long offset,
        OfferSyncProcess offerSyncProcess
    ) {
        int tenantId = VitamThreadUtils.getVitamSession().getTenantId();
        String requestId = VitamThreadUtils.getVitamSession().getRequestId();

        VitamThreadPoolExecutor.getDefaultExecutor()
            .execute(() -> {
                try {
                    VitamThreadUtils.getVitamSession().setTenantId(tenantId);
                    VitamThreadUtils.getVitamSession().setRequestId(requestId);

                    offerSyncProcess.synchronize(executor, sourceOffer, targetOffer, strategyId, dataCategory, offset);
                } catch (Exception e) {
                    LOGGER.error("An error occurred during synchronization process execution", e);
                }
            });
    }

    public boolean isRunning() {
        OfferSyncProcess offerSyncProcess = lastOfferSyncService.get();
        return offerSyncProcess != null && offerSyncProcess.isRunning();
    }

    public OfferSyncStatus getLastSynchronizationStatus() {
        OfferSyncProcess offerSyncProcess = lastOfferSyncService.get();
        if (offerSyncProcess == null) {
            return null;
        } else {
            return offerSyncProcess.getOfferSyncStatus();
        }
    }

    @VisibleForTesting
    public ExecutorService getExecutor() {
        return executor;
    }

    @Override
    public void close() throws Exception {
        if (null != executor) {
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
    }
}
