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

import com.google.common.collect.Iterables;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.storage.AccessRequestStatus;
import fr.gouv.vitam.common.retryable.RetryableOnException;
import fr.gouv.vitam.common.retryable.RetryableParameters;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.OfferLogAction;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.storage.engine.common.model.request.OfferPartialSyncItem;
import fr.gouv.vitam.storage.engine.server.distribution.StorageDistribution;
import fr.gouv.vitam.storage.engine.server.distribution.impl.DataContext;
import fr.gouv.vitam.storage.engine.server.exception.RuntimeStorageException;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Synchronization of a storage offer from another one.
 */
public class OfferSyncProcess {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(OfferSyncProcess.class);
    public static final String OFFER_SYNC_ORIGIN = "offer_sync";

    private final RestoreOfferBackupService restoreOfferBackupService;
    private final StorageDistribution distribution;
    private final int bulkSize;
    private final RetryableParameters retryableParameters;
    private final int accessRequestCheckWaitingTime;

    private OfferSyncStatus offerSyncStatus;

    public OfferSyncProcess(
        RestoreOfferBackupService restoreOfferBackupService,
        StorageDistribution distribution,
        int bulkSize,
        int offerSyncNumberOfRetries,
        int offerSyncFirstAttemptWaitingTime,
        int offerSyncWaitingTime,
        int accessRequestCheckWaitingTime
    ) {
        this.restoreOfferBackupService = restoreOfferBackupService;
        this.distribution = distribution;
        this.bulkSize = bulkSize;
        this.offerSyncStatus = new OfferSyncStatus(
            VitamThreadUtils.getVitamSession().getRequestId(),
            StatusCode.UNKNOWN,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
        this.retryableParameters = new RetryableParameters(
            offerSyncNumberOfRetries,
            offerSyncFirstAttemptWaitingTime,
            offerSyncWaitingTime,
            10,
            SECONDS
        );
        this.accessRequestCheckWaitingTime = accessRequestCheckWaitingTime;
    }

    private static OfferLog getLastOfferLog(OfferLog offerLog1, OfferLog offerLog2) {
        return offerLog1.getSequence() > offerLog2.getSequence() ? offerLog1 : offerLog2;
    }

    private String getCurrentDate() {
        return LocalDateUtil.nowFormatted();
    }

    public void synchronize(
        ExecutorService executor,
        String sourceOffer,
        String targetOffer,
        String strategyId,
        DataCategory dataCategory,
        Long startOffset
    ) {
        this.offerSyncStatus = new OfferSyncStatus(
            VitamThreadUtils.getVitamSession().getRequestId(),
            StatusCode.UNKNOWN,
            getCurrentDate(),
            null,
            sourceOffer,
            targetOffer,
            dataCategory.getCollectionName(),
            startOffset,
            null
        );

        try {
            LOGGER.info(
                String.format(
                    "Start the synchronization process of the target offer {%s} from the source offer {%s} for category {%s}.",
                    targetOffer,
                    sourceOffer,
                    dataCategory
                )
            );

            Long offset = startOffset;
            while (true) {
                // get the data to startSynchronization
                List<OfferLog> rawOfferLogs = restoreOfferBackupService.getListing(
                    strategyId,
                    sourceOffer,
                    dataCategory,
                    offset,
                    bulkSize,
                    Order.ASC
                );

                if (rawOfferLogs.isEmpty()) {
                    break;
                }

                long lastSequence = synchronizeOfferLogs(
                    executor,
                    sourceOffer,
                    targetOffer,
                    strategyId,
                    dataCategory,
                    rawOfferLogs,
                    offset
                );

                this.offerSyncStatus.setCurrentOffset(lastSequence);

                offset = lastSequence + 1;

                LOGGER.info(
                    String.format(
                        "Offer synchronization safe point offset : %s (from %s to %s for category %s)",
                        offset,
                        sourceOffer,
                        targetOffer,
                        dataCategory
                    )
                );

                if (rawOfferLogs.size() < bulkSize) {
                    break;
                }
            }

            LOGGER.info(
                String.format("The offers synchronization completed successfully. from %d to %d", startOffset, offset)
            );
            this.offerSyncStatus.setStatusCode(StatusCode.OK);
        } catch (Exception e) {
            this.offerSyncStatus.setStatusCode(StatusCode.KO);
            LOGGER.error(
                String.format(
                    "[OfferSync]: An exception has been thrown when synchronizing {%s} offer from {%s} source offer with {%s} offset.",
                    targetOffer,
                    sourceOffer,
                    startOffset
                ),
                e
            );
        } finally {
            this.offerSyncStatus.setEndDate(getCurrentDate());
        }
    }

    private long synchronizeOfferLogs(
        Executor executor,
        String sourceOffer,
        String destinationOffer,
        String strategyId,
        DataCategory dataCategory,
        List<OfferLog> rawOfferLogs,
        Long offset
    ) throws StorageException {
        int tenantId = VitamThreadUtils.getVitamSession().getTenantId();
        String requestId = VitamThreadUtils.getVitamSession().getRequestId();

        // Deduplicate entries (on duplication, keep last only)
        Collection<OfferLog> offerLogs = removeDuplicates(rawOfferLogs);

        // Create / await access request readiness if offer is asyncRead mode.
        Optional<String> optionalAccessRequest = awaitAccessRequestReadiness(
            strategyId,
            sourceOffer,
            dataCategory,
            offerLogs
        );

        try {
            List<CompletableFuture<Void>> completableFutures = new ArrayList<>();
            for (OfferLog offerLog : offerLogs) {
                switch (offerLog.getAction()) {
                    case WRITE:
                        completableFutures.add(
                            CompletableFuture.runAsync(
                                () ->
                                    retryable()
                                        .execute(
                                            () ->
                                                copyObject(
                                                    sourceOffer,
                                                    destinationOffer,
                                                    dataCategory,
                                                    offerLog.getContainer(),
                                                    offerLog.getFileName(),
                                                    tenantId,
                                                    strategyId,
                                                    requestId
                                                )
                                        ),
                                executor
                            )
                        );
                        break;
                    case DELETE:
                        completableFutures.add(
                            CompletableFuture.runAsync(
                                () ->
                                    retryable()
                                        .execute(
                                            () ->
                                                deleteObject(
                                                    destinationOffer,
                                                    dataCategory,
                                                    offerLog.getContainer(),
                                                    offerLog.getFileName(),
                                                    tenantId,
                                                    strategyId,
                                                    requestId
                                                )
                                        ),
                                executor
                            )
                        );
                        break;
                    default:
                        throw new UnsupportedOperationException("Unknown offer log action " + offerLog.getAction());
                }
            }

            boolean allSucceeded = awaitCompletion(completableFutures);

            if (!allSucceeded) {
                throw new StorageException(
                    "Error(s) occurred during offer synchronization " +
                    sourceOffer +
                    " > " +
                    destinationOffer +
                    " for container " +
                    dataCategory +
                    " at start offset " +
                    offset
                );
            }

            long lastSequence = Iterables.getLast(rawOfferLogs).getSequence();
            LOGGER.info(
                "[OfferSync]: successful synchronization of dataCategory : {}, tenant : {}, offset : {}",
                dataCategory,
                tenantId,
                lastSequence
            );
            return lastSequence;
        } finally {
            optionalAccessRequest.ifPresent(accessRequestId -> {
                // Remove access request quietly to prevent potential root exception suppression
                removeAccessRequestQuietly(strategyId, sourceOffer, accessRequestId);
            });
        }
    }

    private Optional<String> awaitAccessRequestReadiness(
        String strategyId,
        String sourceOffer,
        DataCategory dataCategory,
        Collection<OfferLog> offerLogs
    ) throws StorageException {
        List<String> objectNames = offerLogs
            .stream()
            .filter(offerLog -> OfferLogAction.WRITE.equals(offerLog.getAction()))
            .map(OfferLog::getFileName)
            .collect(Collectors.toList());

        Optional<String> optionalAccessRequest = createAccessRequestIfRequired(
            strategyId,
            sourceOffer,
            dataCategory,
            objectNames
        );

        if (optionalAccessRequest.isPresent()) {
            awaitAccessRequestReadiness(strategyId, sourceOffer, optionalAccessRequest.get());
        }

        return optionalAccessRequest;
    }

    private RetryableOnException<Void, RuntimeStorageException> retryable() {
        return new RetryableOnException<>(retryableParameters);
    }

    private <T> boolean awaitCompletion(List<CompletableFuture<T>> completableFutures) throws StorageException {
        boolean allSucceeded = true;
        for (CompletableFuture<T> completableFuture : completableFutures) {
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

    private void syncObject(
        String sourceOffer,
        String destinationOffer,
        DataCategory dataCategory,
        String container,
        String fileName,
        int tenant,
        String strategyId,
        String requestId
    ) {
        VitamThreadUtils.getVitamSession().setTenantId(tenant);
        VitamThreadUtils.getVitamSession().setRequestId(requestId);

        Response resp = null;
        try {
            LOGGER.debug(
                "Sync object " +
                container +
                "/" +
                fileName +
                " from offer " +
                sourceOffer +
                " to offer " +
                destinationOffer
            );

            resp = distribution.getContainerByCategory(
                strategyId,
                OFFER_SYNC_ORIGIN,
                fileName,
                dataCategory,
                sourceOffer
            );
            LOGGER.debug(
                "Copy object " +
                container +
                "/" +
                fileName +
                " from offer " +
                sourceOffer +
                " to offer " +
                destinationOffer
            );
            // Assume file found so copy object to destination offer
            distribution.storeDataInOffers(
                strategyId,
                OFFER_SYNC_ORIGIN,
                fileName,
                dataCategory,
                null,
                Collections.singletonList(destinationOffer),
                resp
            );
        } catch (StorageNotFoundException e) {
            deleteObject(destinationOffer, dataCategory, container, fileName, tenant, strategyId, requestId);
        } catch (StorageException e) {
            throw new RuntimeStorageException(
                "An error occurred during copying '" +
                container +
                "/" +
                fileName +
                "' from " +
                sourceOffer +
                " to " +
                destinationOffer,
                e
            );
        } finally {
            StreamUtils.consumeAnyEntityAndClose(resp);
        }
    }

    private void copyObject(
        String sourceOffer,
        String destinationOffer,
        DataCategory dataCategory,
        String container,
        String fileName,
        int tenant,
        String strategyId,
        String requestId
    ) {
        VitamThreadUtils.getVitamSession().setTenantId(tenant);
        VitamThreadUtils.getVitamSession().setRequestId(requestId);

        // Assuming atomic write / delete operations in offers (not yet the case)
        // Try reading a file from source offer
        // If found, copy it to target offer
        // Otherwise, assume file has been deleted meanwhile.

        Response resp = null;
        try {
            LOGGER.debug(
                "Copying object " +
                container +
                "/" +
                fileName +
                " from offer " +
                sourceOffer +
                " to offer " +
                destinationOffer
            );

            resp = distribution.getContainerByCategory(
                strategyId,
                OFFER_SYNC_ORIGIN,
                fileName,
                dataCategory,
                sourceOffer
            );

            distribution.storeDataInOffers(
                strategyId,
                OFFER_SYNC_ORIGIN,
                fileName,
                dataCategory,
                null,
                Collections.singletonList(destinationOffer),
                resp
            );
        } catch (StorageNotFoundException e) {
            LOGGER.debug("File not found", e);
            LOGGER.warn("File " + sourceOffer + " not found on " + sourceOffer + ". File deleted meanwhile?");
        } catch (StorageException e) {
            throw new RuntimeStorageException(
                "An error occurred during copying '" +
                container +
                "/" +
                fileName +
                "' from " +
                sourceOffer +
                " to " +
                destinationOffer,
                e
            );
        } finally {
            StreamUtils.consumeAnyEntityAndClose(resp);
        }
    }

    private void deleteObject(
        String destinationOffer,
        DataCategory dataCategory,
        String container,
        String fileName,
        int tenant,
        String strategyId,
        String requestId
    ) {
        VitamThreadUtils.getVitamSession().setTenantId(tenant);
        VitamThreadUtils.getVitamSession().setRequestId(requestId);

        try {
            // A deleted file should (presumably) never be rewritten
            LOGGER.debug("Deleting object " + container + "/" + fileName + " from offer " + destinationOffer);

            DataContext context = new DataContext(fileName, dataCategory, null, tenant, strategyId);

            distribution.deleteObjectInOffers(strategyId, context, Collections.singletonList(destinationOffer));
        } catch (Exception e) {
            throw new RuntimeStorageException(
                "An error occurred during deleting '" + container + "/" + fileName + "' from " + destinationOffer,
                e
            );
        }
    }

    private Collection<OfferLog> removeDuplicates(List<OfferLog> rawOfferLogs) {
        // Remove duplicate entries (same container & same filename)
        return rawOfferLogs
            .stream()
            .collect(
                Collectors.toMap(
                    offerLog -> new ImmutablePair<>(offerLog.getContainer(), offerLog.getFileName()),
                    offerLog -> offerLog,
                    OfferSyncProcess::getLastOfferLog
                )
            )
            .values();
    }

    public boolean isRunning() {
        return this.offerSyncStatus.getEndDate() == null;
    }

    public OfferSyncStatus getOfferSyncStatus() {
        return offerSyncStatus;
    }

    public void synchronize(
        Executor executor,
        String sourceOffer,
        String targetOffer,
        String strategyId,
        List<OfferPartialSyncItem> items
    ) {
        String requestId = VitamThreadUtils.getVitamSession().getRequestId();

        this.offerSyncStatus = new OfferSyncStatus(
            VitamThreadUtils.getVitamSession().getRequestId(),
            StatusCode.UNKNOWN,
            getCurrentDate(),
            null,
            sourceOffer,
            targetOffer,
            null,
            null,
            null
        );
        try {
            for (OfferPartialSyncItem item : items) {
                DataCategory dataCategory = DataCategory.getByCollectionName(item.getContainer());
                int tenant = item.getTenantId();
                LOGGER.info(
                    String.format(
                        "Start the partial synchronization process of the target offer {%s} from the source offer {%s} for category {%s} and tenant {%s}.",
                        targetOffer,
                        sourceOffer,
                        dataCategory,
                        tenant
                    )
                );

                List<List<String>> objectNameBulks = ListUtils.partition(item.getFilenames(), bulkSize);
                for (List<String> objectNames : objectNameBulks) {
                    synchronize(
                        executor,
                        sourceOffer,
                        targetOffer,
                        strategyId,
                        requestId,
                        dataCategory,
                        tenant,
                        objectNames,
                        item.getContainer()
                    );
                }
            }

            this.offerSyncStatus.setStatusCode(StatusCode.OK);
        } catch (Exception e) {
            this.offerSyncStatus.setStatusCode(StatusCode.KO);
            LOGGER.error(
                String.format(
                    "[OfferSync]: An exception has been thrown when synchronizing {%s} offer from {%s} source offer :",
                    targetOffer,
                    sourceOffer
                ),
                e
            );
        } finally {
            this.offerSyncStatus.setEndDate(getCurrentDate());
        }
    }

    private void synchronize(
        Executor executor,
        String sourceOffer,
        String targetOffer,
        String strategyId,
        String requestId,
        DataCategory dataCategory,
        int tenant,
        List<String> objectNames,
        String container
    ) throws StorageException {
        VitamThreadUtils.getVitamSession().setTenantId(tenant);

        // Create / await access request readiness if offer is asyncRead mode.
        Optional<String> optionalAccessRequest = awaitAccessRequestReadiness(
            strategyId,
            sourceOffer,
            dataCategory,
            objectNames
        );

        try {
            List<CompletableFuture<Void>> completableFutureList = new ArrayList<>();
            for (String objectName : objectNames) {
                completableFutureList.add(
                    CompletableFuture.runAsync(
                        () ->
                            retryable()
                                .execute(
                                    () ->
                                        syncObject(
                                            sourceOffer,
                                            targetOffer,
                                            dataCategory,
                                            container,
                                            objectName,
                                            tenant,
                                            strategyId,
                                            requestId
                                        )
                                ),
                        executor
                    )
                );
            }

            boolean allSucceeded = awaitCompletion(completableFutureList);

            if (!allSucceeded) {
                throw new StorageException(
                    "Error(s) occurred during offer synchronization " + sourceOffer + " > " + targetOffer
                );
            }
        } finally {
            optionalAccessRequest.ifPresent(accessRequestId -> {
                // Remove access request quietly to prevent potential root exception suppression
                removeAccessRequestQuietly(strategyId, sourceOffer, accessRequestId);
            });
        }
    }

    private Optional<String> awaitAccessRequestReadiness(
        String strategyId,
        String sourceOffer,
        DataCategory dataCategory,
        List<String> objectNames
    ) throws StorageException {
        Optional<String> optionalAccessRequest = createAccessRequestIfRequired(
            strategyId,
            sourceOffer,
            dataCategory,
            objectNames
        );

        if (optionalAccessRequest.isPresent()) {
            awaitAccessRequestReadiness(strategyId, sourceOffer, optionalAccessRequest.get());
        }

        return optionalAccessRequest;
    }

    private Optional<String> createAccessRequestIfRequired(
        String strategyId,
        String sourceOffer,
        DataCategory dataCategory,
        List<String> objectNames
    ) throws StorageException {
        return new RetryableOnException<Optional<String>, StorageException>(retryableParameters).exec(
            () -> distribution.createAccessRequestIfRequired(strategyId, sourceOffer, dataCategory, objectNames)
        );
    }

    private void awaitAccessRequestReadiness(String strategyId, String sourceOffer, String accessRequestId)
        throws StorageException {
        while (!isAccessRequestReady(strategyId, sourceOffer, accessRequestId)) {
            sleepBeforeAccessRequestReadinessCheckRetry();
        }
    }

    private boolean isAccessRequestReady(String strategyId, String sourceOffer, String accessRequestId)
        throws StorageException {
        Map<String, AccessRequestStatus> accessRequestStatuses = new RetryableOnException<
            Map<String, AccessRequestStatus>,
            StorageException
        >(retryableParameters).exec(
            () -> distribution.checkAccessRequestStatuses(strategyId, sourceOffer, List.of(accessRequestId), false)
        );

        if (!accessRequestStatuses.containsKey(accessRequestId)) {
            throw new IllegalStateException("Access request status missing from response for " + accessRequestId);
        }

        AccessRequestStatus accessRequestStatus = accessRequestStatuses.get(accessRequestId);
        switch (accessRequestStatus) {
            case READY:
                LOGGER.info("Access request " + accessRequestId + " is READY !");
                return true;
            case NOT_READY:
                LOGGER.info("Access request " + accessRequestId + " is NOT READY... Retrying later...");
                return false;
            case EXPIRED:
                throw new IllegalStateException("Access request Id " + accessRequestId + " already expired ?!");
            case NOT_FOUND:
                throw new IllegalStateException("Access request Id " + accessRequestId + " not found ?!");
            default:
                throw new IllegalStateException("Unexpected value: " + accessRequestStatus);
        }
    }

    private void sleepBeforeAccessRequestReadinessCheckRetry() throws StorageException {
        try {
            SECONDS.sleep(this.accessRequestCheckWaitingTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new StorageException("Offer sync process interrupted", e);
        }
    }

    private void removeAccessRequestQuietly(String strategyId, String sourceOffer, String accessRequestId) {
        try {
            new RetryableOnException<Void, StorageException>(retryableParameters).execute(
                () -> distribution.removeAccessRequest(strategyId, sourceOffer, accessRequestId, false)
            );
        } catch (StorageException e) {
            LOGGER.error(
                "Could not remove access request " +
                accessRequestId +
                " from offer " +
                sourceOffer +
                " of strategy " +
                strategyId,
                e
            );
        }
    }
}
