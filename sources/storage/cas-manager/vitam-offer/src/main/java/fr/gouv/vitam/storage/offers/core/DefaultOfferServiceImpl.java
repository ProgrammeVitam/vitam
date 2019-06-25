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

package fr.gouv.vitam.storage.offers.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import fr.gouv.vitam.cas.container.builder.StoreContextBuilder;
import fr.gouv.vitam.common.FileUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.alert.AlertService;
import fr.gouv.vitam.common.alert.AlertServiceImpl;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogLevel;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.performance.PerformanceLogger;
import fr.gouv.vitam.common.storage.ContainerInformation;
import fr.gouv.vitam.common.storage.StorageConfiguration;
import fr.gouv.vitam.common.storage.cas.container.api.ContentAddressableStorage;
import fr.gouv.vitam.common.storage.cas.container.api.ObjectContent;
import fr.gouv.vitam.common.storage.cas.container.api.VitamPageSet;
import fr.gouv.vitam.common.storage.cas.container.api.VitamStorageMetadata;
import fr.gouv.vitam.common.stream.ExactSizeInputStream;
import fr.gouv.vitam.common.stream.MultiplexedStreamReader;
import fr.gouv.vitam.storage.driver.model.StorageBulkPutResult;
import fr.gouv.vitam.storage.driver.model.StorageBulkPutResultEntry;
import fr.gouv.vitam.storage.driver.model.StorageMetadataResult;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.OfferLogAction;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.storage.offers.database.OfferLogDatabaseService;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageDatabaseException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Default offer service implementation
 */
public class DefaultOfferServiceImpl implements DefaultOfferService {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DefaultOfferServiceImpl.class);

    private final ContentAddressableStorage defaultStorage;

    private final Map<String, String> mapXCusor;

    private OfferLogDatabaseService offerDatabaseService;

    private StorageConfiguration configuration;

    private AlertService alertService = new AlertServiceImpl();

    // FIXME When the server shutdown, it should be able to close the
    // defaultStorage (Http clients)
    public DefaultOfferServiceImpl(OfferLogDatabaseService offerDatabaseService, MongoDbAccess mongoDBAccess)
        throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, KeyManagementException {
        this.offerDatabaseService = offerDatabaseService;
        try {
            configuration = PropertiesUtils.readYaml(PropertiesUtils.findFile(STORAGE_CONF_FILE_NAME),
                StorageConfiguration.class);
            if (!Strings.isNullOrEmpty(configuration.getStoragePath())) {
                configuration.setStoragePath(FileUtil.getFileCanonicalPath(configuration.getStoragePath()));
            }
        } catch (final IOException exc) {
            LOGGER.error(exc);
            throw new ExceptionInInitializerError(exc);
        }
        defaultStorage = StoreContextBuilder.newStoreContext(configuration, mongoDBAccess);
        mapXCusor = new HashMap<>();
    }

    @Override
    @VisibleForTesting
    public String getObjectDigest(String containerName, String objectId, DigestType digestAlgorithm)
        throws ContentAddressableStorageException {
        Stopwatch times = Stopwatch.createStarted();
        try {
            return defaultStorage.getObjectDigest(containerName, objectId, digestAlgorithm, true);
        } finally {
            PerformanceLogger.getInstance()
                .log("STP_Offer_" + configuration.getProvider(), containerName, "COMPUTE_DIGEST",
                    times.elapsed(TimeUnit.MILLISECONDS));
        }
    }

    @Override
    public ObjectContent getObject(String containerName, String objectId)
        throws ContentAddressableStorageException {
        Stopwatch times = Stopwatch.createStarted();
        try {
            return defaultStorage.getObject(containerName, objectId);
        } finally {
            PerformanceLogger.getInstance().log("STP_Offer_" + configuration.getProvider(), containerName, "GET_OBJECT",
                times.elapsed(TimeUnit.MILLISECONDS));

        }
    }

    @Override
    public void asyncGetObject(String containerName, String objectId)
        throws ContentAddressableStorageException {
        Stopwatch times = Stopwatch.createStarted();
        try {
            defaultStorage.asyncGetObject(containerName, objectId);
        } finally {
            PerformanceLogger.getInstance()
                .log("STP_Offer_" + configuration.getProvider(), containerName, "ASYNC_GET_OBJECT",
                    times.elapsed(TimeUnit.MILLISECONDS));

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

    void ensureContainerExists(String containerName) throws ContentAddressableStorageServerException {
        // Create container if not exists
        Stopwatch stopwatch = Stopwatch.createStarted();
        boolean existsContainer = defaultStorage.isExistingContainer(containerName);
        PerformanceLogger
            .getInstance().log("STP_Offer_" + configuration.getProvider(), containerName, "INIT_CHECK_EXISTS_CONTAINER",
            stopwatch.elapsed(
                TimeUnit.MILLISECONDS));
        if (!existsContainer) {
            stopwatch = Stopwatch.createStarted();
            defaultStorage.createContainer(containerName);
            PerformanceLogger.getInstance()
                .log("STP_Offer_" + configuration.getProvider(), containerName, "INIT_CREATE_CONTAINER",
                    stopwatch.elapsed(TimeUnit.MILLISECONDS));
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
            PerformanceLogger
                .getInstance().log("STP_Offer_" + configuration.getProvider(), containerName, "CHECK_EXISTS_PUT_OBJECT",
                stopwatch.elapsed(
                    TimeUnit.MILLISECONDS));
        }
    }

    private void logObjectWriteInOfferLog(String containerName, String objectId)
        throws ContentAddressableStorageServerException, ContentAddressableStorageDatabaseException {
        // Log in offer log
        Stopwatch times = Stopwatch.createStarted();
        offerDatabaseService.save(containerName, objectId, OfferLogAction.WRITE);
        PerformanceLogger
            .getInstance()
            .log("STP_Offer_" + configuration.getProvider(), containerName, "LOG_CREATE_IN_DB", times.elapsed(
                TimeUnit.MILLISECONDS));
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
            PerformanceLogger
                .getInstance()
                .log("STP_Offer_" + configuration.getProvider(), containerName, "GLOBAL_PUT_OBJECT", stopwatch.elapsed(
                    TimeUnit.MILLISECONDS));
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
                    if (!entryInputStream.isPresent()) {
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
            PerformanceLogger.getInstance().log("STP_Offer_" + configuration.getProvider(), containerName,
                "BULK_PUT_OBJECTS", stopwatch.elapsed(TimeUnit.MILLISECONDS));
        }
    }

    private void bulkLogObjectWriteInOfferLog(String containerName, List<String> objectIds)
        throws ContentAddressableStorageServerException, ContentAddressableStorageDatabaseException {
        // Log in offer log
        Stopwatch times = Stopwatch.createStarted();
        offerDatabaseService.bulkSave(containerName, objectIds, OfferLogAction.WRITE);
        PerformanceLogger.getInstance().log("STP_Offer_" + configuration.getProvider(), containerName,
            "BULK_LOG_CREATE_IN_DB", times.elapsed(TimeUnit.MILLISECONDS));
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
        PerformanceLogger.getInstance().log("STP_Offer_" + configuration.getProvider(), containerName, "CHECK_CAPACITY",
            times.elapsed(TimeUnit.MILLISECONDS));
        return containerInformation;
    }


    /**
     * In case not updatable containers, try
     *
     * @param containerName
     * @param objectId
     * @param type
     */
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

        // Log in offer
        offerDatabaseService.save(containerName, objectId, OfferLogAction.DELETE);
        PerformanceLogger.getInstance()
            .log("STP_Offer_" + configuration.getProvider(), containerName, "LOG_DELETE_IN_DB",
                times.elapsed(TimeUnit.MILLISECONDS));

        times = Stopwatch.createStarted();
        defaultStorage.deleteObject(containerName, objectId);
        PerformanceLogger.getInstance().log("STP_Offer_" + configuration.getProvider(), containerName, "DELETE_FILE",
            times.elapsed(TimeUnit.MILLISECONDS));
    }

    @Override
    public StorageMetadataResult getMetadata(String containerName, String objectId, boolean noCache)
        throws ContentAddressableStorageException, IOException {
        Stopwatch times = Stopwatch.createStarted();
        try {
            return new StorageMetadataResult(defaultStorage.getObjectMetadata(containerName, objectId, noCache));
        } finally {
            PerformanceLogger.getInstance()
                .log("STP_Offer_" + configuration.getProvider(), containerName, "GET_METADATA",
                    times.elapsed(TimeUnit.MILLISECONDS));
        }
    }

    public String createCursor(String containerName) throws ContentAddressableStorageServerException {
        defaultStorage.createContainer(containerName);
        String cursorId = GUIDFactory.newGUID().toString();
        mapXCusor.put(getKeyMap(containerName, cursorId), null);
        return cursorId;
    }

    @Override
    public boolean hasNext(String containerName, String cursorId) {
        return mapXCusor.containsKey(getKeyMap(containerName, cursorId));
    }

    @Override
    public List<JsonNode> next(String containerName, String cursorId)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {
        String keyMap = getKeyMap(containerName, cursorId);
        if (mapXCusor.containsKey(keyMap)) {
            VitamPageSet<? extends VitamStorageMetadata> pageSet;
            if (mapXCusor.get(keyMap) == null) {
                pageSet = defaultStorage.listContainer(containerName);
            } else {
                pageSet = defaultStorage.listContainerNext(containerName, mapXCusor.get(keyMap));
            }
            if (pageSet.getNextMarker() != null) {
                mapXCusor.put(keyMap, pageSet.getNextMarker());
            } else {
                mapXCusor.remove(keyMap);
            }
            return getListFromPageSet(pageSet);
        } else {
            // TODO: manage with exception cursor already close
            return null;
        }
    }

    @Override
    public void finalizeCursor(String containerName, String cursorId) {
        if (mapXCusor.containsKey(getKeyMap(containerName, cursorId))) {
            mapXCusor.remove(getKeyMap(containerName, cursorId));
        }
    }

    @Override
    public List<OfferLog> getOfferLogs(String containerName, Long offset, int limit, Order order)
        throws ContentAddressableStorageDatabaseException, ContentAddressableStorageServerException {
        Stopwatch times = Stopwatch.createStarted();
        try {
            return offerDatabaseService.searchOfferLog(containerName, offset, limit, order);
        } finally {
            PerformanceLogger.getInstance()
                .log("STP_Offer_" + configuration.getProvider(), containerName, "GET_OFFER_LOGS",
                    times.elapsed(TimeUnit.MILLISECONDS));

        }
    }

    private List<JsonNode> getListFromPageSet(VitamPageSet<? extends VitamStorageMetadata> pageSet) {
        List<JsonNode> list = new ArrayList<>();
        for (VitamStorageMetadata storageMetadata : pageSet) {
            list.add(JsonHandler.createObjectNode().put("objectId", storageMetadata.getName()));
        }
        return list;
    }

    private String getKeyMap(String containerName, String cursorId) {
        return cursorId + containerName;
    }

}
