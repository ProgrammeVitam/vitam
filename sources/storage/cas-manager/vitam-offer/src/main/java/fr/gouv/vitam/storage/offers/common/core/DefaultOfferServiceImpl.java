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

package fr.gouv.vitam.storage.offers.common.core;

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
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import fr.gouv.vitam.cas.container.builder.StoreContextBuilder;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.performance.PerformanceLogger;
import fr.gouv.vitam.common.storage.ContainerInformation;
import fr.gouv.vitam.common.storage.StorageConfiguration;
import fr.gouv.vitam.common.storage.cas.container.api.ContentAddressableStorage;
import fr.gouv.vitam.common.storage.cas.container.api.ObjectContent;
import fr.gouv.vitam.common.storage.cas.container.api.VitamPageSet;
import fr.gouv.vitam.common.storage.cas.container.api.VitamStorageMetadata;
import fr.gouv.vitam.storage.driver.model.StorageMetadataResult;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.ObjectInit;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.OfferLogAction;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.storage.offers.common.database.OfferLogDatabaseService;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageDatabaseException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

/**
 * Default offer service implementation
 */
public class DefaultOfferServiceImpl implements DefaultOfferService {

    private static final String CONTAINER_ALREADY_EXISTS = "Container already exists";

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DefaultOfferServiceImpl.class);

    private final ContentAddressableStorage defaultStorage;
    private static final String STORAGE_CONF_FILE_NAME = "default-storage.conf";

    private final Map<String, String> mapXCusor;

    private OfferLogDatabaseService offerDatabaseService;
    private final boolean recomputeDigest;

    private StorageConfiguration configuration;

    // FIXME When the server shutdown, it should be able to close the
    // defaultStorage (Http clients)
    public DefaultOfferServiceImpl(OfferLogDatabaseService offerDatabaseService)
            throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, KeyManagementException {
        this.offerDatabaseService = offerDatabaseService;
        try {
            configuration = PropertiesUtils.readYaml(PropertiesUtils.findFile(STORAGE_CONF_FILE_NAME),
                    StorageConfiguration.class);
        } catch (final IOException exc) {
            LOGGER.error(exc);
            throw new ExceptionInInitializerError(exc);
        }
        defaultStorage = StoreContextBuilder.newStoreContext(configuration);
        this.recomputeDigest = configuration.isRecomputeDigest();
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
            PerformanceLogger.getInstance().log("STP_Offer_" + configuration.getProvider(), "COMPUTE_DIGEST", times.elapsed(TimeUnit.MILLISECONDS));
        }
    }

    @Override
    public ObjectContent getObject(String containerName, String objectId)
            throws ContentAddressableStorageException {
        Stopwatch times = Stopwatch.createStarted();
        try {
            return defaultStorage.getObject(containerName, objectId);
        } finally {
            PerformanceLogger.getInstance().log("OfferType_" + configuration.getProvider(), "GET_OBJECT", times.elapsed(TimeUnit.MILLISECONDS));

        }
    }

    @Override
    public ObjectInit initCreateObject(String containerName, ObjectInit objectInit, String objectGUID)
            throws ContentAddressableStorageServerException, ContentAddressableStorageDatabaseException {
        Stopwatch times = Stopwatch.createStarted();
        boolean existsContainer = defaultStorage.isExistingContainer(containerName);
        PerformanceLogger.getInstance().log("STP_Offer_" + configuration.getProvider(), "INIT_CRETAE_OBJECT", "CHECK_EXISTS_CONTAINER" , times.elapsed(TimeUnit.MILLISECONDS));
        if (!existsContainer) {
            try {
                times = Stopwatch.createStarted();
                defaultStorage.createContainer(containerName);
                PerformanceLogger.getInstance().log("STP_Offer_" + configuration.getProvider(), "INIT_CRETAE_OBJECT", "CREATE_CONTAINER" , times.elapsed(TimeUnit.MILLISECONDS));
            } catch (ContentAddressableStorageAlreadyExistException ex) {
                LOGGER.debug(CONTAINER_ALREADY_EXISTS, ex);
            }
        }

        objectInit.setId(objectGUID);
        times = Stopwatch.createStarted();
        offerDatabaseService.save(containerName, objectInit.getId(), OfferLogAction.WRITE);
        PerformanceLogger.getInstance().log("STP_Offer_" + configuration.getProvider(), "INIT_CRETAE_OBJECT", "SAVE_IN_DB" , times.elapsed(TimeUnit.MILLISECONDS));
        return objectInit;
    }


    @Override
    public String createObject(String containerName, String objectId, InputStream objectPart, boolean ending,
                               DataCategory type, Long size, DigestType digestType) throws IOException, ContentAddressableStorageException {
        // TODO No chunk mode (should be added in the future)
        // TODO the objectPart should contain the full object.
        try {
            return putObject(containerName, objectId, objectPart, type, size, digestType);
        } catch (ContentAddressableStorageNotFoundException ex) {
            try {
                defaultStorage.createContainer(containerName);
            } catch (ContentAddressableStorageAlreadyExistException e) {
                LOGGER.debug(CONTAINER_ALREADY_EXISTS, e);
            }
            return putObject(containerName, objectId, objectPart, type, size, digestType);
        } catch (final ContentAddressableStorageException exc) {
            LOGGER.error("Error with storage service", exc);
            throw exc;
        }
    }

    private String putObject(String containerName, String objectId, InputStream objectPart, DataCategory type,
                             Long size, DigestType digestType)
            throws ContentAddressableStorageException {
        // TODO: review this check and the defaultstorage implementation
        Stopwatch times = Stopwatch.createStarted();
        try {
            if (!type.canUpdate() && isObjectExist(containerName, objectId)) {
                throw new ContentAddressableStorageAlreadyExistException("Object with id " + objectId + "already exists " +
                        "and cannot be updated");
            }
        } finally {
            PerformanceLogger.getInstance().log("STP_Offer_" + configuration.getProvider(), "CREATE_OBJECT", "CHECK_EXISTS_OBJECT", times.elapsed(TimeUnit.MILLISECONDS));
        }
        times = Stopwatch.createStarted();
        try {
            return defaultStorage.putObject(containerName, objectId, objectPart, digestType, size, recomputeDigest);
        } finally {
            PerformanceLogger.getInstance().log("STP_Offer_" + configuration.getProvider(), "CREATE_OBJECT", "GLOBAL_PUT_OBJECT", times.elapsed(TimeUnit.MILLISECONDS));
        }
    }

    @Override
    public boolean isObjectExist(String containerName, String objectId)
            throws ContentAddressableStorageServerException {
        return defaultStorage.isExistingObject(containerName, objectId);
    }

    @Override
    public JsonNode getCapacity(String containerName)
            throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {
        final ObjectNode result = JsonHandler.createObjectNode();
        Stopwatch times = Stopwatch.createStarted();
        ContainerInformation containerInformation;
        try {
            containerInformation = defaultStorage.getContainerInformation(containerName);
        } catch (ContentAddressableStorageNotFoundException exc) {
            try {
                defaultStorage.createContainer(containerName);
            } catch (ContentAddressableStorageAlreadyExistException e) {
                LOGGER.debug(CONTAINER_ALREADY_EXISTS, e);
            }
            containerInformation = defaultStorage.getContainerInformation(containerName);
        }
        result.put("usableSpace", containerInformation.getUsableSpace());
        PerformanceLogger.getInstance().log("STP_Offer_" + configuration.getProvider(),"CHECK_CAPACITY", "CHECK_CAPACITY", times.elapsed(TimeUnit.MILLISECONDS));
        return result;
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
        PerformanceLogger.getInstance().log("STP_Offer_" + configuration.getProvider(), "DELETE_OBJECT", "SAVE_IN_DB", times.elapsed(TimeUnit.MILLISECONDS));

        defaultStorage.deleteObject(containerName, objectId);
        PerformanceLogger.getInstance().log("STP_Offer_" + configuration.getProvider(), "DELETE_OBJECT", "DELETE_FILE", times.elapsed(TimeUnit.MILLISECONDS));
    }

    @Override
    public StorageMetadataResult getMetadatas(String containerName, String objectId, boolean noCache)
        throws ContentAddressableStorageException, IOException {
        Stopwatch times = Stopwatch.createStarted();
        try {
            return new StorageMetadataResult(defaultStorage.getObjectMetadatas(containerName, objectId, noCache));
        } finally {
            PerformanceLogger.getInstance().log("STP_Offer_" + configuration.getProvider(), "GET_METADATA", times.elapsed(TimeUnit.MILLISECONDS));
        }
    }

    public String createCursor(String containerName) throws ContentAddressableStorageServerException {
        try {
            defaultStorage.createContainer(containerName);
        } catch (ContentAddressableStorageAlreadyExistException ex) {
            LOGGER.debug(CONTAINER_ALREADY_EXISTS, ex);
        }
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
            PerformanceLogger.getInstance().log("STP_Offer_" + configuration.getProvider(), "getOfferLogs_", times.elapsed(TimeUnit.MILLISECONDS));

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
