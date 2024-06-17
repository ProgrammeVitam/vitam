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
package fr.gouv.vitam.storage.offers.core;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.alert.AlertService;
import fr.gouv.vitam.common.alert.AlertServiceImpl;
import fr.gouv.vitam.common.collection.CloseableIterable;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.logging.VitamLogLevel;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.MetadatasObject;
import fr.gouv.vitam.common.model.storage.AccessRequestStatus;
import fr.gouv.vitam.common.performance.PerformanceLogger;
import fr.gouv.vitam.common.storage.ContainerInformation;
import fr.gouv.vitam.common.storage.StorageConfiguration;
import fr.gouv.vitam.common.storage.cas.container.api.ContentAddressableStorage;
import fr.gouv.vitam.common.storage.cas.container.api.ObjectContent;
import fr.gouv.vitam.common.storage.cas.container.api.ObjectListingListener;
import fr.gouv.vitam.common.storage.constants.StorageProvider;
import fr.gouv.vitam.common.stream.ExactSizeInputStream;
import fr.gouv.vitam.common.stream.MultiplexedStreamReader;
import fr.gouv.vitam.common.thread.ExecutorUtils;
import fr.gouv.vitam.storage.driver.model.StorageBulkMetadataResult;
import fr.gouv.vitam.storage.driver.model.StorageBulkMetadataResultEntry;
import fr.gouv.vitam.storage.driver.model.StorageBulkPutResult;
import fr.gouv.vitam.storage.driver.model.StorageBulkPutResultEntry;
import fr.gouv.vitam.storage.driver.model.StorageMetadataResult;
import fr.gouv.vitam.storage.engine.common.model.CompactedOfferLog;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.OfferLogAction;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.storage.offers.database.OfferLogAndCompactedOfferLogService;
import fr.gouv.vitam.storage.offers.database.OfferLogCompactionDatabaseService;
import fr.gouv.vitam.storage.offers.database.OfferLogDatabaseService;
import fr.gouv.vitam.storage.offers.database.OfferSequenceDatabaseService;
import fr.gouv.vitam.storage.offers.rest.OfferLogCompactionConfiguration;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageDatabaseException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.iterators.PeekingIterator;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.collections4.iterators.PeekingIterator.peekingIterator;

public class DefaultOfferServiceImpl implements DefaultOfferService {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DefaultOfferServiceImpl.class);

    private final AlertService alertService = new AlertServiceImpl();

    private final ContentAddressableStorage defaultStorage;

    private final OfferLogCompactionDatabaseService offerLogCompactionDatabaseService;
    private final OfferLogDatabaseService offerDatabaseService;
    private final OfferSequenceDatabaseService offerSequenceDatabaseService;
    private final StorageConfiguration configuration;
    private final OfferLogCompactionConfiguration offerLogCompactionConfig;
    private final OfferLogAndCompactedOfferLogService offerLogAndCompactedOfferLogService;
    private final ExecutorService batchExecutorService;
    private final int batchMetadataComputationTimeoutIsSeconds;

    public DefaultOfferServiceImpl(
        ContentAddressableStorage defaultStorage,
        OfferLogCompactionDatabaseService offerLogCompactionDatabaseService,
        OfferLogDatabaseService offerDatabaseService,
        OfferSequenceDatabaseService offerSequenceDatabaseService,
        StorageConfiguration configuration,
        OfferLogCompactionConfiguration offerLogCompactionConfig,
        OfferLogAndCompactedOfferLogService offerLogAndCompactedOfferLogService,
        int maxBatchThreadPoolSize,
        int batchMetadataComputationTimeout
    ) {
        this.defaultStorage = defaultStorage;
        this.offerLogCompactionDatabaseService = offerLogCompactionDatabaseService;
        this.offerDatabaseService = offerDatabaseService;
        this.offerSequenceDatabaseService = offerSequenceDatabaseService;
        this.configuration = configuration;
        this.offerLogCompactionConfig = offerLogCompactionConfig;
        this.offerLogAndCompactedOfferLogService = offerLogAndCompactedOfferLogService;
        this.batchMetadataComputationTimeoutIsSeconds = batchMetadataComputationTimeout;
        this.batchExecutorService = ExecutorUtils.createScalableBatchExecutorService(maxBatchThreadPoolSize);
    }

    @Override
    @VisibleForTesting
    public String getObjectDigest(String containerName, String objectId, DigestType digestAlgorithm)
        throws ContentAddressableStorageException {
        Stopwatch times = Stopwatch.createStarted();
        try {
            return defaultStorage.getObjectDigest(containerName, objectId, digestAlgorithm, true);
        } finally {
            log(times, containerName, "COMPUTE_DIGEST");
        }
    }

    @Override
    public ObjectContent getObject(String containerName, String objectId) throws ContentAddressableStorageException {
        Stopwatch times = Stopwatch.createStarted();
        try {
            return defaultStorage.getObject(containerName, objectId);
        } finally {
            log(times, containerName, "GET_OBJECT");
        }
    }

    @Override
    public String createAccessRequest(String containerName, List<String> objectNames)
        throws ContentAddressableStorageException {
        if (!StorageProvider.TAPE_LIBRARY.getValue().equalsIgnoreCase(configuration.getProvider())) {
            throw new ContentAddressableStorageException("Access request is enabled only on tape library offer");
        }

        Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            return defaultStorage.createAccessRequest(containerName, objectNames);
        } finally {
            log(stopwatch, containerName, "CREATE_ACCESS_REQUEST");
        }
    }

    @Override
    public Map<String, AccessRequestStatus> checkAccessRequestStatuses(
        List<String> accessRequestIds,
        boolean adminCrossTenantAccessRequestAllowed
    ) throws ContentAddressableStorageException {
        if (!StorageProvider.TAPE_LIBRARY.getValue().equalsIgnoreCase(configuration.getProvider())) {
            throw new ContentAddressableStorageException("Access request is enabled only on tape library offer");
        }

        Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            return defaultStorage.checkAccessRequestStatuses(accessRequestIds, adminCrossTenantAccessRequestAllowed);
        } finally {
            log(stopwatch, "CHECK_ACCESS_REQUEST", "CHECK_ACCESS_REQUEST");
        }
    }

    @Override
    public void removeAccessRequest(String accessRequestId, boolean adminCrossTenantAccessRequestAllowed)
        throws ContentAddressableStorageException {
        if (!StorageProvider.TAPE_LIBRARY.getValue().equalsIgnoreCase(configuration.getProvider())) {
            throw new ContentAddressableStorageException("Access request is enabled only on tape library offer");
        }

        Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            defaultStorage.removeAccessRequest(accessRequestId, adminCrossTenantAccessRequestAllowed);
        } finally {
            log(stopwatch, accessRequestId, "REMOVE_ACCESS_REQUEST");
        }
    }

    @Override
    public boolean checkObjectAvailability(String containerName, List<String> objectNames)
        throws ContentAddressableStorageException {
        if (!StorageProvider.TAPE_LIBRARY.getValue().equalsIgnoreCase(configuration.getProvider())) {
            throw new ContentAddressableStorageException(
                "Object availability check is enabled only on tape library offer"
            );
        }

        Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            return defaultStorage.checkObjectAvailability(containerName, objectNames);
        } finally {
            log(stopwatch, containerName, "CHECK_OBJECT_AVAILABILITY");
        }
    }

    @Override
    public String createObject(
        String containerName,
        String objectId,
        InputStream objectPart,
        DataCategory type,
        long size,
        DigestType digestType
    ) throws ContentAddressableStorageException {
        ensureContainerExists(containerName);

        String digest;
        if (!type.canUpdate() && defaultStorage.isExistingObject(containerName, objectId)) {
            digest = checkNonRewritableObjects(containerName, objectId, objectPart, digestType);
        } else {
            digest = writeObject(containerName, objectId, objectPart, size, digestType, type);
            this.defaultStorage.checkObjectDigestAndStoreDigest(containerName, objectId, digest, digestType, size);
        }

        // Write offer log even if non-updatable object already existed in CAS to ensure offer log is written if not yet
        // logged (idempotency)
        logObjectWriteInOfferLog(containerName, objectId);

        return digest;
    }

    void ensureContainerExists(String containerName) throws ContentAddressableStorageServerException {
        // Create container if not exists
        Stopwatch stopwatch = Stopwatch.createStarted();
        boolean existsContainer = defaultStorage.isExistingContainer(containerName);
        log(stopwatch, containerName, "INIT_CHECK_EXISTS_CONTAINER");
        if (!existsContainer) {
            stopwatch = Stopwatch.createStarted();
            defaultStorage.createContainer(containerName);
            log(stopwatch, containerName, "INIT_CREATE_CONTAINER");
        }
    }

    private String checkNonRewritableObjects(
        String containerName,
        String objectId,
        InputStream objectPart,
        DigestType digestType
    ) throws ContentAddressableStorageException {
        Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            // Compute file digest
            Digest digest = new Digest(digestType);
            digest.update(objectPart);
            String streamDigest = digest.digestHex();

            // Check actual object digest (without cache for full checkup)
            return checkObjectDigest(containerName, digestType, objectId, streamDigest);
        } catch (IOException e) {
            throw new ContentAddressableStorageException("Could not read input stream", e);
        } finally {
            log(stopwatch, containerName, "CHECK_EXISTS_PUT_OBJECT");
        }
    }

    private void logObjectWriteInOfferLog(String containerName, String objectId)
        throws ContentAddressableStorageServerException, ContentAddressableStorageDatabaseException {
        // Log in offer log
        Stopwatch times = Stopwatch.createStarted();
        long sequence = offerSequenceDatabaseService.getNextSequence(
            OfferSequenceDatabaseService.BACKUP_LOG_SEQUENCE_ID
        );
        offerDatabaseService.save(containerName, objectId, OfferLogAction.WRITE, sequence);
        log(times, containerName, "LOG_CREATE_IN_DB");
    }

    private String writeObject(
        String containerName,
        String objectId,
        InputStream objectPart,
        long size,
        DigestType digestType,
        DataCategory type
    ) throws ContentAddressableStorageException {
        // Write object
        Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            Digest digest = new Digest(digestType);
            InputStream inputStream = digest.getDigestInputStream(objectPart);
            defaultStorage.writeObject(containerName, objectId, inputStream, digestType, size);

            return digest.digestHex();
        } catch (ContentAddressableStorageNotFoundException e) {
            throw e;
        } catch (Exception ex) {
            trySilentlyDeleteWormObject(containerName, objectId, type);
            // Propagate the initial exception
            throw ex;
        } finally {
            log(stopwatch, containerName, "GLOBAL_PUT_OBJECT");
        }
    }

    @Override
    public StorageBulkPutResult bulkPutObjects(
        String containerName,
        List<String> objectIds,
        MultiplexedStreamReader multiplexedStreamReader,
        DataCategory type,
        DigestType digestType
    ) throws ContentAddressableStorageException, IOException {
        Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            ensureContainerExists(containerName);

            BackgroundObjectDigestValidator backgroundObjectDigestValidator = new BackgroundObjectDigestValidator(
                defaultStorage,
                containerName,
                digestType
            );

            try {
                for (String objectId : objectIds) {
                    // Ensure no exception thrown by background object validator meanwhile
                    if (
                        backgroundObjectDigestValidator.hasTechnicalExceptionsReported() ||
                        backgroundObjectDigestValidator.hasConflictsReported()
                    ) {
                        break;
                    }

                    Optional<ExactSizeInputStream> entryInputStream = multiplexedStreamReader.readNextEntry();
                    if (entryInputStream.isEmpty()) {
                        throw new IllegalStateException("No entry not found for object id " + objectId);
                    }

                    LOGGER.info("Writing object '" + objectId + "' of container " + containerName);

                    ExactSizeInputStream inputStream = entryInputStream.get();
                    long size = inputStream.getSize();

                    String objectDigest;
                    if (!type.canUpdate() && defaultStorage.isExistingObject(containerName, objectId)) {
                        // Do not override non-rewritable objets, just check digest for idempotency
                        objectDigest = computeObjectDigest(digestType, inputStream);

                        backgroundObjectDigestValidator.addExistingWormObjectToCheck(objectId, objectDigest, size);
                    } else {
                        objectDigest = writeObject(containerName, objectId, inputStream, size, digestType, type);

                        backgroundObjectDigestValidator.addWrittenObjectToCheck(objectId, objectDigest, size);
                    }
                }
            } finally {
                backgroundObjectDigestValidator.awaitTermination();

                List<StorageBulkPutResultEntry> entries = backgroundObjectDigestValidator.getWrittenObjects();
                if (!entries.isEmpty()) {
                    // Write offer logs even if non-updatable object already existed in CAS to ensure offer log is
                    // written if not yet logged (idempotency)
                    List<String> storedObjectIds = entries
                        .stream()
                        .map(StorageBulkPutResultEntry::getObjectId)
                        .collect(Collectors.toList());
                    bulkLogObjectWriteInOfferLog(containerName, storedObjectIds);
                }
            }

            if (backgroundObjectDigestValidator.hasConflictsReported()) {
                throw new NonUpdatableContentAddressableStorageException(
                    "Bulk object write failed. At least one non-rewritable object override rejected"
                );
            }

            if (backgroundObjectDigestValidator.hasTechnicalExceptionsReported()) {
                throw new ContentAddressableStorageException("Bulk object write failed. At least one object failed");
            }

            if (multiplexedStreamReader.readNextEntry().isPresent()) {
                throw new IllegalStateException("No more entries expected");
            }

            return new StorageBulkPutResult(backgroundObjectDigestValidator.getWrittenObjects());
        } finally {
            log(stopwatch, containerName, "BULK_PUT_OBJECTS");
        }
    }

    private String computeObjectDigest(DigestType digestType, ExactSizeInputStream inputStream) throws IOException {
        // Compute file digest
        Digest entryDigest = new Digest(digestType);
        entryDigest.update(inputStream);
        return entryDigest.digestHex();
    }

    private String checkObjectDigest(
        String containerName,
        DigestType digestType,
        String objectId,
        String entryDigestValue
    ) throws ContentAddressableStorageException {
        // Check actual object digest (without cache for full checkup)
        String actualObjectDigest = defaultStorage.getObjectDigest(containerName, objectId, digestType, true);

        if (entryDigestValue.equals(actualObjectDigest)) {
            LOGGER.warn(
                "Non rewritable object updated with same content. Ignoring duplicate. Object Id '" +
                objectId +
                "' in " +
                containerName
            );
            return actualObjectDigest;
        } else {
            alertService.createAlert(
                VitamLogLevel.ERROR,
                String.format(
                    "Object with id %s (%s) already exists and cannot be updated. Existing file digest=%s, input digest=%s",
                    objectId,
                    containerName,
                    actualObjectDigest,
                    entryDigestValue
                )
            );
            throw new NonUpdatableContentAddressableStorageException(
                "Object with id " + objectId + " already exists " + "and cannot be updated"
            );
        }
    }

    private void bulkLogObjectWriteInOfferLog(String containerName, List<String> objectIds)
        throws ContentAddressableStorageServerException, ContentAddressableStorageDatabaseException {
        // Log in offer log
        Stopwatch times = Stopwatch.createStarted();
        long sequence = offerSequenceDatabaseService.getNextSequence(
            OfferSequenceDatabaseService.BACKUP_LOG_SEQUENCE_ID,
            objectIds.size()
        );
        offerDatabaseService.bulkSave(containerName, objectIds, OfferLogAction.WRITE, sequence);
        log(times, containerName, "BULK_LOG_CREATE_IN_DB");
    }

    @Override
    public boolean isObjectExist(String containerName, String objectId) throws ContentAddressableStorageException {
        return defaultStorage.isExistingObject(containerName, objectId);
    }

    @Override
    public ContainerInformation getCapacity(String containerName) throws ContentAddressableStorageException {
        Stopwatch times = Stopwatch.createStarted();
        ContainerInformation containerInformation;
        try {
            containerInformation = defaultStorage.getContainerInformation(containerName);
        } catch (ContentAddressableStorageNotFoundException exc) {
            defaultStorage.createContainer(containerName);
            containerInformation = defaultStorage.getContainerInformation(containerName);
        }
        log(times, containerName, "CHECK_CAPACITY");
        return containerInformation;
    }

    private void trySilentlyDeleteWormObject(String containerName, String objectId, DataCategory type) {
        if (type.canUpdate()) {
            LOGGER.error(
                "Write file failed for " +
                containerName +
                "/" +
                objectId +
                ". No need to cleanup (object is rewritable)"
            );
            return;
        }

        try {
            LOGGER.warn("Cleanup partially written object: " + containerName + "/" + objectId + ". Try deleting it...");
            defaultStorage.deleteObject(containerName, objectId);
        } catch (ContentAddressableStorageNotFoundException e) {
            // JUST LOG. DO NOT RETHROW EXCEPTION AS IT MAY HIDE ROOT CAUSE EXCEPTION FROM PARENT METHOD
            LOGGER.warn(
                "Delete object after upload failure " + containerName + "/" + objectId + ". Object not found",
                e
            );
        } catch (Exception e) {
            // JUST LOG. DO NOT RETHROW EXCEPTION AS IT MAY HIDE ROOT CAUSE EXCEPTION FROM PARENT METHOD
            LOGGER.error(
                "Cannot delete object " +
                containerName +
                "/" +
                objectId +
                ". Potentially partially written object on WORM container",
                e
            );
        }
    }

    @Override
    public void deleteObject(String containerName, String objectId, DataCategory type)
        throws ContentAddressableStorageException {
        Stopwatch times = Stopwatch.createStarted();
        if (!type.canDelete()) {
            throw new ContentAddressableStorageException("Object with id " + objectId + "can not be deleted");
        }

        long sequence = offerSequenceDatabaseService.getNextSequence(
            OfferSequenceDatabaseService.BACKUP_LOG_SEQUENCE_ID
        );
        // Log in offer
        offerDatabaseService.save(containerName, objectId, OfferLogAction.DELETE, sequence);
        log(times, containerName, "LOG_DELETE_IN_DB");

        times = Stopwatch.createStarted();
        defaultStorage.deleteObject(containerName, objectId);
        log(times, containerName, "DELETE_FILE");
    }

    @Override
    public StorageMetadataResult getMetadata(String containerName, String objectId, boolean noCache)
        throws ContentAddressableStorageException {
        Stopwatch times = Stopwatch.createStarted();
        try {
            return new StorageMetadataResult(defaultStorage.getObjectMetadata(containerName, objectId, noCache));
        } finally {
            log(times, containerName, "GET_METADATA");
        }
    }

    @Override
    public StorageBulkMetadataResult getBulkMetadata(String containerName, List<String> objectIds, Boolean noCache)
        throws ContentAddressableStorageException {
        Stopwatch times = Stopwatch.createStarted();
        try {
            List<CompletableFuture<StorageBulkMetadataResultEntry>> completableFutures = new ArrayList<>();
            for (String objectId : objectIds) {
                CompletableFuture<StorageBulkMetadataResultEntry> objectInformationCompletableFuture =
                    CompletableFuture.supplyAsync(
                        () -> {
                            try {
                                MetadatasObject objectMetadata = defaultStorage.getObjectMetadata(
                                    containerName,
                                    objectId,
                                    noCache
                                );
                                return new StorageBulkMetadataResultEntry(
                                    objectMetadata.getObjectName(),
                                    objectMetadata.getDigest(),
                                    objectMetadata.getFileSize()
                                );
                            } catch (ContentAddressableStorageNotFoundException e) {
                                LOGGER.info("Object " + objectId + " not found in container " + containerName, e);
                                return new StorageBulkMetadataResultEntry(objectId, null, null);
                            } catch (ContentAddressableStorageException e) {
                                throw new RuntimeException(
                                    "Could not get object metadata for " +
                                    containerName +
                                    "/" +
                                    objectId +
                                    " (noCache=" +
                                    noCache +
                                    ")",
                                    e
                                );
                            }
                        },
                        batchExecutorService
                    );
                completableFutures.add(objectInformationCompletableFuture);
            }

            CompletableFuture<List<StorageBulkMetadataResultEntry>> batchObjectInformationFuture = sequence(
                completableFutures
            );

            try {
                return new StorageBulkMetadataResult(
                    batchObjectInformationFuture.get(batchMetadataComputationTimeoutIsSeconds, SECONDS)
                );
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                // Abort pending tasks
                for (CompletableFuture<StorageBulkMetadataResultEntry> completableFuture : completableFutures) {
                    completableFuture.cancel(false);
                }
                throw new ContentAddressableStorageException("Batch object information timed out", e);
            }
        } finally {
            log(times, containerName, "GET_BULK_METADATA");
        }
    }

    private <T> CompletableFuture<List<T>> sequence(List<CompletableFuture<T>> completableFutures) {
        CompletableFuture<Void> allDoneFuture = CompletableFuture.allOf(
            completableFutures.toArray(new CompletableFuture[0])
        );
        return allDoneFuture.thenApply(
            v -> completableFutures.stream().map(CompletableFuture::join).collect(Collectors.toList())
        );
    }

    @Override
    public void listObjects(String containerName, ObjectListingListener objectListingListener)
        throws IOException, ContentAddressableStorageException {
        ensureContainerExists(containerName);
        defaultStorage.listContainer(containerName, objectListingListener);
    }

    @Override
    public List<OfferLog> getOfferLogs(String containerName, Long offset, int limit, Order order)
        throws ContentAddressableStorageDatabaseException {
        Stopwatch times = Stopwatch.createStarted();
        try {
            switch (order) {
                case ASC:
                    return searchAscending(containerName, offset, limit);
                case DESC:
                    return searchDescending(containerName, offset, limit);
                default:
                    throw new VitamRuntimeException("Order must be ASC or DESC, here " + order);
            }
        } catch (Exception e) {
            throw new ContentAddressableStorageDatabaseException(
                String.format("Database Error while getting OfferLog for container %s", containerName),
                e
            );
        } finally {
            log(times, containerName, "GET_OFFER_LOGS");
        }
    }

    private List<OfferLog> searchDescending(String containerName, Long offset, int limit) {
        // First seek results from newest records (OfferLog)
        List<OfferLog> offerLogs = offerDatabaseService.getDescendingOfferLogsBy(containerName, offset, limit);

        // If not enough entries, then fetch next entries from older records (CompactedOfferLog)
        int remainingLimit = getRemainingLimit(offerLogs, limit);
        Long nextOffset = getNextOffsetDescending(offerLogs, offset);

        if (remainingLimit == 0) {
            return offerLogs;
        }

        List<OfferLog> compactedOfferLogs = offerLogCompactionDatabaseService.getDescendingOfferLogCompactionBy(
            containerName,
            nextOffset,
            remainingLimit
        );

        return ListUtils.union(offerLogs, compactedOfferLogs);
    }

    private List<OfferLog> searchAscending(String containerName, Long offset, int limit) {
        // First seek results from oldest records (CompactedOfferLog)
        List<OfferLog> compactedOfferLogs = offerLogCompactionDatabaseService.getAscendingOfferLogCompactionBy(
            containerName,
            offset,
            limit
        );

        int remainingLimit = getRemainingLimit(compactedOfferLogs, limit);
        Long nextOffset = getNextOffsetAscending(compactedOfferLogs, offset);

        if (remainingLimit == 0) {
            return compactedOfferLogs;
        }

        // If not enough entries, then fetch next entries from new records
        // CAUTION : We need to double-check OfferLog + CompactedOfferLog to avoid concurrent offer log compaction :
        //  - If we only fetch from OfferLog, we might miss entries that have just been compacted concurrently
        //  - If we fetch from both OfferLog & CompactedOfferLog, we might get duplicates (non-transactional update)
        // So we fetch both collections & merge results
        List<OfferLog> nextOfferLogs = offerDatabaseService.getAscendingOfferLogsBy(
            containerName,
            nextOffset,
            remainingLimit
        );

        List<OfferLog> nextCompactedOfferLogs = offerLogCompactionDatabaseService.getAscendingOfferLogCompactionBy(
            containerName,
            nextOffset,
            remainingLimit
        );

        List<OfferLog> nextMergedOfferLogs = mergeAndResolveDuplicates(
            nextOfferLogs,
            nextCompactedOfferLogs,
            remainingLimit
        );

        return ListUtils.union(compactedOfferLogs, nextMergedOfferLogs);
    }

    private List<OfferLog> mergeAndResolveDuplicates(List<OfferLog> list1, List<OfferLog> list2, int limit) {
        if (list1.isEmpty()) {
            return list2;
        }
        if (list2.isEmpty()) {
            return list1;
        }

        // MergeSort algorithm with result limit
        PeekingIterator<OfferLog> iterator1 = peekingIterator(list1.iterator());
        PeekingIterator<OfferLog> iterator2 = peekingIterator(list2.iterator());

        List<OfferLog> results = new ArrayList<>();
        while (results.size() < limit) {
            boolean shouldTakeFromList1 =
                iterator1.hasNext() &&
                (!iterator2.hasNext() || iterator1.peek().getSequence() <= iterator2.peek().getSequence());

            boolean shouldTakeFromList2 =
                iterator2.hasNext() &&
                (!iterator1.hasNext() || iterator1.peek().getSequence() >= iterator2.peek().getSequence());

            if (shouldTakeFromList1 && shouldTakeFromList2) {
                results.add(iterator1.next());
                iterator2.next();
            } else if (shouldTakeFromList1) {
                results.add(iterator1.next());
            } else if (shouldTakeFromList2) {
                results.add(iterator2.next());
            } else {
                break;
            }
        }
        return results;
    }

    private int getRemainingLimit(List<OfferLog> logs, int limit) {
        return limit - logs.size();
    }

    private Long getNextOffsetDescending(List<OfferLog> logs, Long offset) {
        if (!logs.isEmpty()) {
            return logs.get(logs.size() - 1).getSequence() - 1;
        }
        return offset;
    }

    private Long getNextOffsetAscending(List<OfferLog> logs, Long offset) {
        if (!logs.isEmpty()) {
            return logs.get(logs.size() - 1).getSequence() + 1;
        }
        return offset;
    }

    @Override
    public void compactOfferLogs() throws Exception {
        Stopwatch timer = Stopwatch.createStarted();
        try (
            CloseableIterable<OfferLog> expiredOfferLogsByContainer =
                offerDatabaseService.getExpiredOfferLogByContainer(
                    offerLogCompactionConfig.getExpirationValue(),
                    offerLogCompactionConfig.getExpirationUnit()
                )
        ) {
            List<OfferLog> bulkToSend = new ArrayList<>();

            for (OfferLog offerLog : expiredOfferLogsByContainer) {
                if (isBulkFull(bulkToSend) || !isInSameContainer(offerLog, bulkToSend)) {
                    saveOfferLogCompaction(bulkToSend);
                    bulkToSend = new ArrayList<>();
                }
                bulkToSend.add(offerLog);
            }

            if (!bulkToSend.isEmpty()) {
                saveOfferLogCompaction(bulkToSend);
            }
        } finally {
            log(timer, offerLogCompactionConfig.toString(), "COMPACT_OFFER_LOGS");
        }
    }

    private boolean isBulkFull(List<OfferLog> bulkToSend) {
        return bulkToSend.size() >= offerLogCompactionConfig.getCompactionSize();
    }

    private boolean isInSameContainer(OfferLog offerLog, List<OfferLog> bulkToSend) {
        if (bulkToSend.isEmpty()) {
            return true;
        }
        return offerLog.getContainer().equals(bulkToSend.get(0).getContainer());
    }

    private void saveOfferLogCompaction(List<OfferLog> bulkToSend) {
        OfferLog first = bulkToSend.get(0);
        OfferLog last = bulkToSend.get(bulkToSend.size() - 1);

        CompactedOfferLog compactedOfferLog = new CompactedOfferLog(
            first.getSequence(),
            last.getSequence(),
            LocalDateUtil.now(),
            first.getContainer(),
            bulkToSend
        );
        offerLogAndCompactedOfferLogService.almostTransactionalSaveAndDelete(compactedOfferLog, bulkToSend);
    }

    public void log(Stopwatch timer, String action, String task) {
        PerformanceLogger.getInstance()
            .log(String.format("STP_Offer_%s", configuration.getProvider()), action, task, timer.elapsed(MILLISECONDS));
    }
}
