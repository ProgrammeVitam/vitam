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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.jclouds.blobstore.domain.PageSet;
import org.jclouds.blobstore.domain.StorageMetadata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.cas.container.builder.StoreContextBuilder;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.application.AsyncInputStreamHelper;
import fr.gouv.vitam.common.storage.StorageConfiguration;
import fr.gouv.vitam.common.storage.cas.container.api.ContentAddressableStorage;
import fr.gouv.vitam.common.storage.constants.ErrorMessage;
import fr.gouv.vitam.storage.driver.model.StorageMetadatasResult;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.ObjectInit;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.common.storage.ContainerInformation;

/**
 * Default offer service implementation
 */
public class DefaultOfferServiceImpl implements DefaultOfferService {

    private static final String CONTAINER_ALREADY_EXISTS = "Container already exists";

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DefaultOfferServiceImpl.class);

    private static final DefaultOfferService INSTANCE = new DefaultOfferServiceImpl();
    private final ContentAddressableStorage defaultStorage;
    private static final String STORAGE_CONF_FILE_NAME = "default-storage.conf";

    private final Map<String, DigestType> digestTypeFor;
    private final Map<String, String> objectTypeFor;

    private final Map<String, String> mapXCusor;

    // FIXME When the server shutdown, it should be able to close the
    // defaultStorage (Http clients)
    private DefaultOfferServiceImpl() {
        StorageConfiguration configuration;
        try {
            configuration = PropertiesUtils.readYaml(PropertiesUtils.findFile(STORAGE_CONF_FILE_NAME),
                    StorageConfiguration.class);
        } catch (final IOException exc) {
            LOGGER.error(exc);
            throw new ExceptionInInitializerError(exc);
        }
        defaultStorage = StoreContextBuilder.newStoreContext(configuration);
        digestTypeFor = new HashMap<>();
        objectTypeFor = new HashMap<>();
        mapXCusor = new HashMap<>();
    }

    /**
     * @return the default instance
     */
    public static DefaultOfferService getInstance() {
        return INSTANCE;
    }

    @Override
    public String getObjectDigest(String containerName, String objectId, DigestType digestAlgorithm)
            throws ContentAddressableStorageException {
        return defaultStorage.computeObjectDigest(containerName, objectId, digestAlgorithm);
    }

    @Override
    public Response getObject(String containerName, String objectId, AsyncResponse asyncResponse)
            throws ContentAddressableStorageException {
        final Response response = defaultStorage.getObjectAsync(containerName, objectId, asyncResponse);
        final AsyncInputStreamHelper helper = new AsyncInputStreamHelper(asyncResponse, response);
        final ResponseBuilder responseBuilder = Response.status(response.getStatus()).type(MediaType.APPLICATION_OCTET_STREAM);
        helper.writeResponse(responseBuilder);
        return response;
    }

    @Override
    public ObjectInit initCreateObject(String containerName, ObjectInit objectInit, String objectGUID)
            throws ContentAddressableStorageServerException {
        try {
            defaultStorage.createContainer(containerName);
        } catch (ContentAddressableStorageAlreadyExistException ex) {
            LOGGER.debug(CONTAINER_ALREADY_EXISTS, ex);
        }
        objectInit.setId(objectGUID);
        objectTypeFor.put(objectGUID, objectInit.getType().getFolder());
        if (objectInit.getDigestAlgorithm() != null) {
            digestTypeFor.put(objectGUID, objectInit.getDigestAlgorithm());
        } else {
            digestTypeFor.put(objectGUID, VitamConfiguration.getDefaultDigestType());
        }

        return objectInit;
    }

    @Override
    public String createObject(String containerName, String objectId, InputStream objectPart, boolean ending,
        DataCategory type) throws IOException, ContentAddressableStorageException {
        // TODO No chunk mode (should be added in the future)
        // TODO the objectPart should contain the full object.
        try {
            return putObject(containerName, objectId, objectPart, type);
        } catch (ContentAddressableStorageNotFoundException ex) {
            try {
                defaultStorage.createContainer(containerName);
            } catch (ContentAddressableStorageAlreadyExistException e) {
                LOGGER.debug(CONTAINER_ALREADY_EXISTS, e);
            }
            return putObject(containerName, objectId, objectPart, type);
        } catch (final ContentAddressableStorageException exc) {
            LOGGER.error("Error with storage service", exc);
            throw exc;
        }
    }

    private String putObject(String containerName, String objectId, InputStream objectPart, DataCategory type)
        throws ContentAddressableStorageException {
        // TODO: review this check and the defaultstorage implementation
        if (isObjectExist(containerName, objectId) && !type.canUpdate()) {
            throw new ContentAddressableStorageAlreadyExistException("Object with id " + objectId + "already exists " +
                "and cannot be updated");
        }
        defaultStorage.putObject(containerName, objectId, objectPart);
        // Check digest AFTER writing in order to ensure correctness
        final String digest =
            defaultStorage.computeObjectDigest(containerName, objectId, getDigestAlgoFor(objectId));
        // remove digest algo
        digestTypeFor.remove(objectId);
        return digest;
    }

    @Override
    public boolean isObjectExist(String containerName, String objectId) throws ContentAddressableStorageServerException {
        return defaultStorage.isExistingObject(containerName, objectId);
    }

    @Override
    public JsonNode getCapacity(String containerName)
            throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {
        final ObjectNode result = JsonHandler.createObjectNode();
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
        result.put("usedSpace", containerInformation.getUsedSpace());
        return result;
    }

    @Override
    public JsonNode countObjects(String containerName)
            throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {
        final ObjectNode result = JsonHandler.createObjectNode();
        final long objectNumber = defaultStorage.countObjects(containerName);
        result.put("objectNumber", objectNumber);
        return result;
    }

    private DigestType getDigestAlgoFor(String id) {
        return digestTypeFor.get(id) != null ? digestTypeFor.get(id) : VitamConfiguration.getDefaultDigestType();
    }

    @Override
    public boolean checkObject(String containerName, String objectId, String digest, DigestType digestAlgorithm)
            throws ContentAddressableStorageException {
        String offerDigest = getObjectDigest(containerName, objectId, digestAlgorithm);
        return offerDigest.equals(digest);
    }

    @Override
    public boolean checkDigest(String containerName, String idObject, String digest) {
        LOGGER.error("Not yet implemented");
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public boolean checkDigestAlgorithm(String containerName, String idObject, DigestType digestAlgorithm) {
        LOGGER.error("Not yet implemented");
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void deleteObject(String containerName, String objectId, String digest, DigestType digestAlgorithm,
        DataCategory type) throws ContentAddressableStorageNotFoundException, ContentAddressableStorageException {
        if (!type.canDelete()) {
            throw new ContentAddressableStorageException("Object with id " + objectId + "can not be deleted");
        }
        // TODO G1 : replace with checkObject when merged
        String offerDigest = getObjectDigest(containerName, objectId, digestAlgorithm);
        if (offerDigest.equals(digest)) {
            defaultStorage.deleteObject(containerName, objectId);
        } else {
            LOGGER.error(ErrorMessage.OBJECT_NOT_FOUND.getMessage() + objectId);
            throw new ContentAddressableStorageNotFoundException(ErrorMessage.OBJECT_NOT_FOUND.getMessage() + objectId);
        }
    }

    @Override
    public StorageMetadatasResult getMetadatas(String containerName, String objectId)
            throws ContentAddressableStorageException, IOException {
        return new StorageMetadatasResult(defaultStorage.getObjectMetadatas(containerName, objectId));
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
        throws ContentAddressableStorageNotFoundException,ContentAddressableStorageServerException {
        String keyMap = getKeyMap(containerName, cursorId);
        if (mapXCusor.containsKey(keyMap)) {
            PageSet<? extends StorageMetadata> pageSet;
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

    private List<JsonNode> getListFromPageSet(PageSet<? extends StorageMetadata> pageSet) {
        List<JsonNode> list = new ArrayList<>();
        for (StorageMetadata storageMetadata : pageSet) {
            list.add(JsonHandler.createObjectNode().put("objectId", storageMetadata.getName()));
        }
        return list;
    }

    private String getKeyMap(String containerName, String cursorId) {
        return cursorId + containerName;
    }

}
