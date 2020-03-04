/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
import fr.gouv.vitam.common.alert.AlertService;
import fr.gouv.vitam.common.alert.AlertServiceImpl;
import fr.gouv.vitam.common.collection.CloseableIterable;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.logging.VitamLogLevel;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.performance.PerformanceLogger;
import fr.gouv.vitam.common.security.SafeFileChecker;
import fr.gouv.vitam.common.storage.ContainerInformation;
import fr.gouv.vitam.common.storage.StorageConfiguration;
import fr.gouv.vitam.common.storage.cas.container.api.ContentAddressableStorage;
import fr.gouv.vitam.common.storage.cas.container.api.ObjectContent;
import fr.gouv.vitam.common.storage.cas.container.api.ObjectListingListener;
import fr.gouv.vitam.common.storage.constants.StorageProvider;
import fr.gouv.vitam.common.stream.ExactSizeInputStream;
import fr.gouv.vitam.common.stream.MultiplexedStreamReader;
import fr.gouv.vitam.storage.driver.model.StorageBulkPutResult;
import fr.gouv.vitam.storage.driver.model.StorageBulkPutResultEntry;
import fr.gouv.vitam.storage.driver.model.StorageMetadataResult;
import fr.gouv.vitam.storage.engine.common.model.CompactedOfferLog;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.OfferLogAction;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.storage.engine.common.model.TapeReadRequestReferentialEntity;
import fr.gouv.vitam.storage.offers.database.OfferLogAndCompactedOfferLogService;
import fr.gouv.vitam.storage.offers.database.OfferLogCompactionDatabaseService;
import fr.gouv.vitam.storage.offers.database.OfferLogDatabaseService;
import fr.gouv.vitam.storage.offers.database.OfferSequenceDatabaseService;
import fr.gouv.vitam.storage.offers.rest.OfferLogCompactionRequest;
import fr.gouv.vitam.storage.offers.tape.cas.ReadRequestReferentialRepository;
import fr.gouv.vitam.storage.offers.tape.exception.ReadRequestReferentialException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageDatabaseException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class DefaultOfferServiceImpl implements DefaultOfferService {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DefaultOfferServiceImpl.class);

    private final AlertService alertService = new AlertServiceImpl();

    private final ContentAddressableStorage defaultStorage;

    private final ReadRequestReferentialRepository readRequestReferentialRepository;
    private final OfferLogCompactionDatabaseService offerLogCompactionDatabaseService;
    private final OfferLogDatabaseService offerDatabaseService;
    private final OfferSequenceDatabaseService offerSequenceDatabaseService;
    private final StorageConfiguration configuration;
    private final OfferLogAndCompactedOfferLogService offerLogAndCompactedOfferLogService;

    public DefaultOfferServiceImpl(
        ContentAddressableStorage defaultStorage,
        ReadRequestReferentialRepository readRequestReferentialRepository,
        OfferLogCompactionDatabaseService offerLogCompactionDatabaseService,
        OfferLogDatabaseService offerDatabaseService,
        OfferSequenceDatabaseService offerSequenceDatabaseService,
        StorageConfiguration configuration,
        OfferLogAndCompactedOfferLogService offerLogAndCompactedOfferLogService) {

        this.defaultStorage = defaultStorage;
        this.readRequestReferentialRepository = readRequestReferentialRepository;
        this.offerLogCompactionDatabaseService = offerLogCompactionDatabaseService;
        this.offerDatabaseService = offerDatabaseService;
        this.offerSequenceDatabaseService = offerSequenceDatabaseService;
        this.configuration = configuration;
        this.offerLogAndCompactedOfferLogService = offerLogAndCompactedOfferLogService;
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
    public ObjectContent getObject(String containerName, String objectId)
        throws ContentAddressableStorageException {
        Stopwatch times = Stopwatch.createStarted();
        try {
            return defaultStorage.getObject(containerName, objectId);
        } finally {
            log(times, containerName, "GET_OBJECT");
        }
    }

    @Override
    public Optional<TapeReadRequestReferentialEntity> createReadOrderRequest(String containerName,
        List<String> objectsIds)
        throws ContentAddressableStorageException {

        if (!StorageProvider.TAPE_LIBRARY.getValue().equalsIgnoreCase(configuration.getProvider())) {
            throw new ContentAddressableStorageException("Read order is enabled only on tape library offer");
        }

        Stopwatch times = Stopwatch.createStarted();
        try {
            String readRequestID = defaultStorage.createReadOrderRequest(containerName, objectsIds);

            try {
                return readRequestReferentialRepository.find(readRequestID);
            } catch (ReadRequestReferentialException e) {
                LOGGER.error(e);
                return Optional.empty();
            }
        } finally {
            log(times, containerName, "ASYNC_GET_OBJECT");
        }
    }

    @Override
    public Optional<TapeReadRequestReferentialEntity> getReadOrderRequest(String readRequestID)
        throws ContentAddressableStorageException {
        if (!StorageProvider.TAPE_LIBRARY.getValue().equalsIgnoreCase(configuration.getProvider())) {
            throw new ContentAddressableStorageException("Read order is enabled only on tape library offer");
        }
        try {
            return readRequestReferentialRepository.find(readRequestID);
        } catch (ReadRequestReferentialException e) {
            LOGGER.error(e);
            return Optional.empty();
        }
    }

    @Override
    public void removeReadOrderRequest(String readRequestID)
        throws ContentAddressableStorageException {

        if (!StorageProvider.TAPE_LIBRARY.getValue().equalsIgnoreCase(configuration.getProvider())) {
            throw new ContentAddressableStorageException("Read order is enabled only on tape library offer");
        }

        Stopwatch times = Stopwatch.createStarted();
        try {
            defaultStorage.removeReadOrderRequest(readRequestID);
        } finally {
            log(times, readRequestID, "REMOVE_READ_ORDER_REQUEST");
        }
    }

    @Override
    public String createObject(String containerName, String objectId, InputStream objectPart,
        DataCategory type, Long size, DigestType digestType) throws ContentAddressableStorageException {

        ensureContainerExists(containerName);

        String digest = writeObject(containerName, objectId, objectPart, type, size, digestType);

        // Write offer log even if non updatable object already existed in CAS to ensure offer log is written if not yet
        // logged (idempotency)
        logObjectWriteInOfferLog(containerName, objectId);

        return digest;
    }

    @VisibleForTesting
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

    private String writeObject(String containerName, String objectId, InputStream objectPart, DataCategory type,
        Long size, DigestType digestType) throws ContentAddressableStorageException {
        if (!type.canUpdate() && isObjectExist(containerName, objectId)) {
            return checkNonRewritableObjects(containerName, objectId, objectPart, digestType);
        }
        return putObject(containerName, objectId, objectPart, size, digestType, type);
    }

    private String checkNonRewritableObjects(String containerName, String objectId, InputStream objectPart,
        DigestType digestType) throws ContentAddressableStorageException {

        Stopwatch stopwatch = Stopwatch.createStarted();
        try {

            // Compute file digest
            Digest digest = new Digest(digestType);
            digest.update(objectPart);
            String streamDigest = digest.digestHex();

            // Check actual object digest (without cache for full checkup)
            String actualObjectDigest = defaultStorage.getObjectDigest(containerName, objectId, digestType, true);

            if (streamDigest.equals(actualObjectDigest)) {
                LOGGER.warn(
                    "Non rewritable object updated with same content. Ignoring duplicate. Object Id '" + objectId +
                        "' in " + containerName);
                return actualObjectDigest;
            } else {
                alertService.createAlert(VitamLogLevel.ERROR, String.format(
                    "Object with id %s (%s) already exists and cannot be updated. Existing file digest=%s, input digest=%s",
                    objectId, containerName, actualObjectDigest, streamDigest));
                throw new NonUpdatableContentAddressableStorageException(
                    "Object with id " + objectId + " already exists " +
                        "and cannot be updated");
            }

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
        long sequence = offerSequenceDatabaseService.getNextSequence(OfferSequenceDatabaseService.BACKUP_LOG_SEQUENCE_ID);
        offerDatabaseService.save(containerName, objectId, OfferLogAction.WRITE, sequence);
        log(times, containerName, "LOG_CREATE_IN_DB");
    }

    private String putObject(String containerName, String objectId, InputStream objectPart, Long size,
        DigestType digestType, DataCategory type) throws ContentAddressableStorageException {
        // Write object
        Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            return defaultStorage.putObject(containerName, objectId, objectPart, digestType, size);
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
    public StorageBulkPutResult bulkPutObjects(String containerName, List<String> objectIds,
        MultiplexedStreamReader multiplexedStreamReader, DataCategory type, DigestType digestType)
        throws ContentAddressableStorageException, IOException {

        Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            ensureContainerExists(containerName);

            List<StorageBulkPutResultEntry> entries = new ArrayList<>();

            try {
                for (String objectId : objectIds) {

                    Optional<ExactSizeInputStream> entryInputStream = multiplexedStreamReader.readNextEntry();
                    if (entryInputStream.isEmpty()) {
                        throw new IllegalStateException("No entry not found for object id " + objectId);
                    }

                    LOGGER.info("Writing object '" + objectId + "' of container " + containerName);

                    ExactSizeInputStream inputStream = entryInputStream.get();

                    String digest;
                    if (!type.canUpdate() && isObjectExist(containerName, objectId)) {
                        digest = checkNonRewritableObjects(containerName, objectId, inputStream, digestType);
                    } else {
                        digest =
                            putObject(containerName, objectId, inputStream, inputStream.getSize(), digestType, type);
                    }
                    entries.add(new StorageBulkPutResultEntry(objectId, digest, inputStream.getSize()));
                }

            } finally {

                if (!entries.isEmpty()) {
                    // Write offer logs even if non updatable object already existed in CAS to ensure offer log is
                    // written if not yet logged (idempotency)
                    List<String> storedObjectIds =
                        entries.stream().map(StorageBulkPutResultEntry::getObjectId).collect(Collectors.toList());
                    bulkLogObjectWriteInOfferLog(containerName, storedObjectIds);
                }
            }

            if (multiplexedStreamReader.readNextEntry().isPresent()) {
                throw new IllegalStateException("No more entries expected");
            }

            return new StorageBulkPutResult(entries);

        } finally {
            log(stopwatch, containerName, "BULK_PUT_OBJECTS");
        }
    }

    private void bulkLogObjectWriteInOfferLog(String containerName, List<String> objectIds)
        throws ContentAddressableStorageServerException, ContentAddressableStorageDatabaseException {
        // Log in offer log
        Stopwatch times = Stopwatch.createStarted();
        long sequence = offerSequenceDatabaseService.getNextSequence(OfferSequenceDatabaseService.BACKUP_LOG_SEQUENCE_ID, objectIds.size());
        offerDatabaseService.bulkSave(containerName, objectIds, OfferLogAction.WRITE, sequence);
        log(times, containerName, "BULK_LOG_CREATE_IN_DB");
    }

    @Override
    public boolean isObjectExist(String containerName, String objectId)
        throws ContentAddressableStorageServerException {
        return defaultStorage.isExistingObject(containerName, objectId);
    }

    @Override
    public ContainerInformation getCapacity(String containerName)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {
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
            return;
        }
        try {
            defaultStorage.deleteObject(containerName, objectId);
        } catch (Exception e) {
            // Just warn, as if we have a write exception we can presumably got a delete exception (Ex. Network exception)
            LOGGER.warn("Cannot silently delete object of warm container after write exception occurs", e);
        }
    }

    @Override
    public void deleteObject(String containerName, String objectId, DataCategory type)
        throws ContentAddressableStorageException {
        Stopwatch times = Stopwatch.createStarted();
        if (!type.canDelete()) {
            throw new ContentAddressableStorageException("Object with id " + objectId + "can not be deleted");
        }

        long sequence = offerSequenceDatabaseService.getNextSequence(OfferSequenceDatabaseService.BACKUP_LOG_SEQUENCE_ID);
        // Log in offer
        offerDatabaseService.save(containerName, objectId, OfferLogAction.DELETE, sequence);
        log(times, containerName, "LOG_DELETE_IN_DB");

        times = Stopwatch.createStarted();
        defaultStorage.deleteObject(containerName, objectId);
        log(times, containerName, "DELETE_FILE");
    }

    @Override
    public StorageMetadataResult getMetadata(String containerName, String objectId, boolean noCache)
        throws ContentAddressableStorageException, IOException {
        Stopwatch times = Stopwatch.createStarted();
        try {
            return new StorageMetadataResult(defaultStorage.getObjectMetadata(containerName, objectId, noCache));
        } finally {
            log(times, containerName, "GET_METADATA");
        }
    }

    @Override
    public void listObjects(String containerName, ObjectListingListener objectListingListener)
        throws IOException, ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {
        defaultStorage.listContainer(containerName, objectListingListener);
    }

    @Override
    public List<OfferLog> getOfferLogs(String containerName, Long offset, int limit, Order order) throws ContentAddressableStorageDatabaseException {
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
            throw new ContentAddressableStorageDatabaseException(String.format("Database Error while getting OfferLog for container %s", containerName), e);
        } finally {
            log(times, containerName, "GET_OFFER_LOGS");
        }
    }

    private List<OfferLog> searchDescending(String containerName, Long offset, int limit) throws Exception {
        List<OfferLog> logs = new ArrayList<>();
        try (CloseableIterable<OfferLog> offerLogs = offerDatabaseService.getDescendingOfferLogsBy(containerName, offset, limit)) {
            for (OfferLog log : offerLogs) {
                logs.add(log);
            }
        }

        if (logs.size() == limit) {
            return logs;
        }

        try (CloseableIterable<CompactedOfferLog> offerLogsFromCompaction = offerLogCompactionDatabaseService.getDescendingOfferLogCompactionBy(containerName, getNextOffsetDescending(logs, offset))) {
            addCompactedLogsDescending(limit, logs, offerLogsFromCompaction);
        }
        return logs;
    }

    private void addCompactedLogsDescending(int limit, List<OfferLog> logs, Iterable<CompactedOfferLog> offerLogsFromCompaction) {
        for (CompactedOfferLog compaction : offerLogsFromCompaction) {
            List<OfferLog> compactedLogs = compaction.getLogs();
            for (int i = compactedLogs.size() - 1; i >= 0; i--) {
                logs.add(compactedLogs.get(i));
                if (logs.size() >= limit) {
                    return;
                }
            }
        }
    }

    private List<OfferLog> searchAscending(String containerName, Long offset, int limit) throws Exception {
        List<OfferLog> logs = new ArrayList<>();
        try (CloseableIterable<CompactedOfferLog> offerLogsFromCompaction = offerLogCompactionDatabaseService.getAscendingOfferLogCompactionBy(containerName, offset)) {
            addCompactedLogsAscending(limit, logs, offerLogsFromCompaction);
        }

        if (logs.size() == limit) {
            return logs;
        }

        try (CloseableIterable<OfferLog> offerLogs = offerDatabaseService.getAscendingOfferLogsBy(containerName, getNextOffsetAscending(logs, offset), getLimit(logs, limit))) {
            for (OfferLog log : offerLogs) {
                logs.add(log);
            }
        }


        if (logs.size() == limit) {
            return logs;
        }

        try (CloseableIterable<CompactedOfferLog> offerLogsFromCompaction = offerLogCompactionDatabaseService.getAscendingOfferLogCompactionBy(containerName, getNextOffsetAscending(logs, offset))) {
            addCompactedLogsAscending(limit, logs, offerLogsFromCompaction);
        }

        return logs;
    }

    private int getLimit(List<OfferLog> logs, int limit) {
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

    private void addCompactedLogsAscending(int limit, List<OfferLog> logs, Iterable<CompactedOfferLog> offerLogsFromCompaction) {
        for (CompactedOfferLog compaction : offerLogsFromCompaction) {
            for (OfferLog log : compaction.getLogs()) {
                logs.add(log);
                if (logs.size() >= limit) {
                    return;
                }
            }
        }
    }

    public void checkOfferPath(String... paths) throws IOException {
        SafeFileChecker.checkSafeFilePath(configuration.getStoragePath(), paths);
    }

    @Override
    public void compactOfferLogs(OfferLogCompactionRequest request) throws Exception {
        Stopwatch timer = Stopwatch.createStarted();
        try (CloseableIterable<OfferLog> expiredOfferLogsByContainer = offerDatabaseService.getExpiredOfferLogByContainer(request)) {
            List<OfferLog> bulkToSend = new ArrayList<>();

            for (OfferLog offerLog : expiredOfferLogsByContainer) {
                if (isBulkFull(request, bulkToSend) || !isInSameContainer(offerLog, bulkToSend)) {
                    saveOfferLogCompaction(bulkToSend);
                    bulkToSend = new ArrayList<>();
                }
                bulkToSend.add(offerLog);
            }

            if (!bulkToSend.isEmpty()) {
                saveOfferLogCompaction(bulkToSend);
            }
        } finally {
            log(timer, request.toString(), "COMPACT_OFFER_LOGS");
        }
    }

    public boolean isBulkFull(OfferLogCompactionRequest request, List<OfferLog> bulkToSend) {
        return bulkToSend.size() >= request.getCompactionSize();
    }

    public boolean isInSameContainer(OfferLog offerLog, List<OfferLog> bulkToSend) {
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
            LocalDateTime.now(),
            first.getContainer(),
            bulkToSend
        );
        offerLogAndCompactedOfferLogService.almostTransactionalSaveAndDelete(compactedOfferLog, bulkToSend);
    }

    public void log(Stopwatch timer, String action, String task) {
        PerformanceLogger.getInstance().log(
            String.format("STP_Offer_%s", configuration.getProvider()),
            action,
            task,
            timer.elapsed(MILLISECONDS)
        );
    }
}
