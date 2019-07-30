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
package fr.gouv.vitam.storage.engine.server.offersynchronization;

import com.google.common.collect.Iterables;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.thread.VitamThreadFactory;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.storage.engine.server.distribution.StorageDistribution;
import fr.gouv.vitam.storage.engine.server.distribution.impl.DataContext;
import fr.gouv.vitam.storage.engine.server.exception.RuntimeStorageException;
import org.apache.commons.lang3.tuple.ImmutablePair;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Synchronization of a storage offer from another one.
 */
public class OfferSyncProcess {

    /**
     * Vitam Logger.
     */
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(OfferSyncProcess.class);

    private static final String STRATEGY_ID = "default";

    private final RestoreOfferBackupService restoreOfferBackupService;
    private final StorageDistribution distribution;
    private final int bulkSize;
    private final int offerSyncThreadPoolSize;

    private OfferSyncStatus offerSyncStatus;

    public OfferSyncProcess(
        RestoreOfferBackupService restoreOfferBackupService,
        StorageDistribution distribution, int bulkSize, int offerSyncThreadPoolSize) {
        this.restoreOfferBackupService = restoreOfferBackupService;
        this.distribution = distribution;
        this.bulkSize = bulkSize;
        this.offerSyncThreadPoolSize = offerSyncThreadPoolSize;
        this.offerSyncStatus = new OfferSyncStatus(
            VitamThreadUtils.getVitamSession().getRequestId(), StatusCode.UNKNOWN, null, null,
            null, null, null, null, null);
    }

    /**
     * Synchronize an offer from another using the offset.
     *
     * @param sourceOffer the identifier of the source offer
     * @param targetOffer the identifier of the target offer
     * @param offset the offset of the process of the synchronisation
     */
    public void synchronize(String sourceOffer, String targetOffer,
        DataCategory dataCategory, Long offset) {

        this.offerSyncStatus = new OfferSyncStatus(
            VitamThreadUtils.getVitamSession().getRequestId(), StatusCode.UNKNOWN, getCurrentDate(), null,
            sourceOffer, targetOffer, dataCategory.getCollectionName(), offset, null);

        ExecutorService executor = null;
        try {
            LOGGER.info(String.format(
                "Start the synchronization process of the target offer {%s} from the source offer {%s} for category {%s}.",
                targetOffer, sourceOffer, dataCategory));

            executor = Executors.newFixedThreadPool(
                this.offerSyncThreadPoolSize, VitamThreadFactory.getInstance());

            synchronize(executor, sourceOffer, targetOffer, dataCategory, offset);
            this.offerSyncStatus.setStatusCode(StatusCode.OK);

        } catch (Throwable e) {
            this.offerSyncStatus.setStatusCode(StatusCode.KO);
            LOGGER.error(String.format(
                "[OfferSync]: An exception has been thrown when synchronizing {%s} offer from {%s} source offer with {%s} offset.",
                targetOffer, sourceOffer, offset), e);
        } finally {
            this.offerSyncStatus.setEndDate(getCurrentDate());
            if (executor != null) {
                executor.shutdown();
            }
        }
    }

    private String getCurrentDate() {
        return LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now());
    }

    private void synchronize(ExecutorService executor, String sourceOffer, String targetOffer,
        DataCategory dataCategory, Long startOffset) throws StorageException {

        Long offset = startOffset;
        while (true) {
            // get the data to startSynchronization
            List<OfferLog> rawOfferLogs = restoreOfferBackupService.getListing(
                STRATEGY_ID, sourceOffer, dataCategory, offset, bulkSize, Order.ASC);

            if (rawOfferLogs.isEmpty()) {
                break;
            }

            long lastSequence =
                synchronizeOfferLogs(executor, sourceOffer, targetOffer, dataCategory, rawOfferLogs);

            this.offerSyncStatus.setCurrentOffset(lastSequence);

            LOGGER.info(String.format("Offer synchronization safe point offset : %s (from %s to %s for category %s)",
                offset, sourceOffer, targetOffer, dataCategory));

            offset = lastSequence + 1;

            if (rawOfferLogs.size() < bulkSize) {
                break;
            }
        }

        LOGGER.info(String.format("The offers synchronization completed successfully. from %d to %d",
            startOffset, offset));
    }

    private long synchronizeOfferLogs(ExecutorService executor, String sourceOffer,
        String destinationOffer, DataCategory dataCategory, List<OfferLog> rawOfferLogs) throws StorageException {

        int tenantId = VitamThreadUtils.getVitamSession().getTenantId();
        String requestId = VitamThreadUtils.getVitamSession().getRequestId();

        // Deduplicate entries (on duplication, keep last only)
        Collection<OfferLog> offerLogs = removeDuplicates(rawOfferLogs);

        List<CompletableFuture<Void>> completableFutures = new ArrayList<>();
        for (OfferLog offerLog : offerLogs) {

            CompletableFuture<Void> completableFuture;
            switch (offerLog.getAction()) {

                case WRITE:
                    completableFuture = CompletableFuture.runAsync(
                        RetryableRunnable.from(() -> copyObject(sourceOffer, destinationOffer, dataCategory, offerLog, tenantId, requestId)),
                        executor);
                    break;

                case DELETE:
                    completableFuture = CompletableFuture.runAsync(
                        RetryableRunnable.from(() -> deleteObject(destinationOffer, dataCategory, offerLog, tenantId, requestId)),
                        executor);
                    break;

                default:
                    throw new UnsupportedOperationException("Unknown offer log action " + offerLog.getAction());
            }
            completableFutures.add(completableFuture);
        }

        boolean allSucceeded = awaitCompletion(completableFutures);

        if (!allSucceeded) {
            throw new StorageException(
                "Error(s) occurred during offer synchronization " + sourceOffer + " > " + destinationOffer +
                    " for container " + dataCategory);
        }

        long lastSequence = Iterables.getLast(rawOfferLogs).getSequence();
        LOGGER.info("[OfferSync]: successful synchronization of dataCategory : {}, tenant : {}, offset : {}",
            dataCategory, tenantId, lastSequence);
        return lastSequence;
    }

    private boolean awaitCompletion(List<CompletableFuture<Void>> completableFutures) throws StorageException {
        boolean allSucceeded = true;
        for (CompletableFuture<Void> completableFuture : completableFutures) {
            try {
                completableFuture.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new StorageException(e);
            } catch (ExecutionException e) {
                LOGGER.error(e);
                allSucceeded = false;
            }
        }
        return allSucceeded;
    }

    private void copyObject(String sourceOffer, String destinationOffer, DataCategory dataCategory,
        OfferLog offerLog, int tenant, String requestId) {

        VitamThreadUtils.getVitamSession().setTenantId(tenant);
        VitamThreadUtils.getVitamSession().setRequestId(requestId);

        // Assuming atomic write / delete operations in offers (not yet the case)
        // Try reading a file from source offer
        // If found, copy it to target offer
        // Otherwise, assume file has been deleted meanwhile.

        Response resp = null;
        try {
            LOGGER.debug("Copying object " + offerLog.getContainer() + "/" + offerLog.getFileName() + " from offer " +
                sourceOffer + " to offer " + destinationOffer);

            resp = distribution
                .getContainerByCategory(STRATEGY_ID, offerLog.getFileName(), dataCategory,
                    sourceOffer);

            distribution.storeDataInOffers(STRATEGY_ID, offerLog.getFileName(),
                dataCategory, null, Collections.singletonList(destinationOffer), resp);

        } catch (StorageNotFoundException e) {
            LOGGER.debug("File not found", e);
            LOGGER.warn("File " + sourceOffer + " not found on " + sourceOffer + ". File deleted meanwhile?");
        } catch (StorageException e) {
            throw new RuntimeStorageException(
                "An error occurred during copying '" + offerLog.getContainer() + "/" + offerLog.getFileName() +
                    "' from "
                    + sourceOffer + " to " + destinationOffer, e);
        } finally {
            StreamUtils.consumeAnyEntityAndClose(resp);
        }
    }

    private void deleteObject(String destinationOffer, DataCategory dataCategory, OfferLog offerLog, int tenant,
        String requestId) {

        VitamThreadUtils.getVitamSession().setTenantId(tenant);
        VitamThreadUtils.getVitamSession().setRequestId(requestId);

        try {
            // A deleted file should (presumably) never be rewritten
            LOGGER.debug("Deleting object " + offerLog.getContainer() + "/" + offerLog.getFileName() + " from offer " +
                destinationOffer);

            DataContext context = new DataContext(
                offerLog.getFileName(), dataCategory, null, tenant);

            distribution.deleteObjectInOffers(STRATEGY_ID, context, Collections.singletonList(destinationOffer));

        } catch (StorageException e) {
            throw new RuntimeStorageException("An error occurred during deleting '" + offerLog.getContainer() + "/" +
                offerLog.getFileName() + "' from " + destinationOffer, e);
        }
    }

    private Collection<OfferLog> removeDuplicates(List<OfferLog> rawOfferLogs) {

        // Remove duplicate entries (same container & same filename)
        return rawOfferLogs.stream()
            .collect(Collectors.toMap(
                offerLog -> new ImmutablePair<>(offerLog.getContainer(), offerLog.getFileName()),
                offerLog -> offerLog,
                OfferSyncProcess::getLastOfferLog)
            ).values();
    }

    private static OfferLog getLastOfferLog(OfferLog offerLog1, OfferLog offerLog2) {
        return offerLog1.getSequence() > offerLog2.getSequence() ? offerLog1 : offerLog2;
    }

    public boolean isRunning() {
        return this.offerSyncStatus.getEndDate() == null;
    }

    public OfferSyncStatus getOfferSyncStatus() {
        return offerSyncStatus;
    }
}
