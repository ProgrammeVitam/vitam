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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.cas.container.builder.StoreContextBuilder;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.alert.AlertService;
import fr.gouv.vitam.common.alert.AlertServiceImpl;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogLevel;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.application.VitamHttpHeader;
import fr.gouv.vitam.common.storage.ContainerInformation;
import fr.gouv.vitam.common.storage.StorageConfiguration;
import fr.gouv.vitam.common.storage.cas.container.api.ContentAddressableStorage;
import fr.gouv.vitam.common.storage.cas.container.api.VitamPageSet;
import fr.gouv.vitam.common.storage.cas.container.api.VitamStorageMetadata;
import fr.gouv.vitam.common.storage.constants.ErrorMessage;
import fr.gouv.vitam.common.stream.VitamAsyncInputStreamResponse;
import fr.gouv.vitam.storage.driver.model.StorageMetadatasResult;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.storage.offers.common.database.OfferLogDatabaseService;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageDatabaseException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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

/**
 * Default offer service implementation
 */
public class DefaultOfferServiceImpl implements DefaultOfferService {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DefaultOfferServiceImpl.class);

    private final ContentAddressableStorage defaultStorage;
    private static final String STORAGE_CONF_FILE_NAME = "default-storage.conf";

    private final Map<String, String> mapXCusor;

    private OfferLogDatabaseService offerDatabaseService;

    private AlertService alertService = new AlertServiceImpl();

    // FIXME When the server shutdown, it should be able to close the
    // defaultStorage (Http clients)
    public DefaultOfferServiceImpl(OfferLogDatabaseService offerDatabaseService)
        throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, KeyManagementException {
        this.offerDatabaseService = offerDatabaseService;
        StorageConfiguration configuration;
        try {
            configuration = PropertiesUtils.readYaml(PropertiesUtils.findFile(STORAGE_CONF_FILE_NAME),
                StorageConfiguration.class);
        } catch (final IOException exc) {
            LOGGER.error(exc);
            throw new ExceptionInInitializerError(exc);
        }
        defaultStorage = StoreContextBuilder.newStoreContext(configuration);
        mapXCusor = new HashMap<>();
    }

    @Override
    public String getObjectDigest(String containerName, String objectId, DigestType digestAlgorithm)
        throws ContentAddressableStorageException {
        return defaultStorage.computeObjectDigest(containerName, objectId, digestAlgorithm);
    }

    @Override
    public Response getObject(String containerName, String objectId)
        throws ContentAddressableStorageException {

        final Response response = defaultStorage.getObjectAsync(containerName, objectId);

        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM);
        headers.put(VitamHttpHeader.X_CONTENT_LENGTH.getName(),
            response.getHeaderString(VitamHttpHeader.X_CONTENT_LENGTH.getName()));

        return new VitamAsyncInputStreamResponse(response, Response.Status.fromStatusCode(response.getStatus()),
            headers);
    }

    @Override
    public String createObject(String containerName, String objectId, InputStream objectPart,
        DataCategory type, Long size, DigestType digestType) throws ContentAddressableStorageException {

        ensureContainerExists(containerName);

        String existingDigest = checkNonRewritableObjects(containerName, objectId, objectPart, type, digestType);
        if (existingDigest != null) {
            return existingDigest;
        }

        logObjectWriteInOfferLog(containerName, objectId);

        return putObject(containerName, objectId, objectPart, size, digestType);
    }

    private void ensureContainerExists(String containerName) throws ContentAddressableStorageServerException {
        if (!defaultStorage.isExistingContainer(containerName)) {
            defaultStorage.createContainer(containerName);
        }
    }

    private String checkNonRewritableObjects(String containerName, String objectId, InputStream objectPart,
        DataCategory type, DigestType digestType) throws ContentAddressableStorageException {

        try {

            if (type.canUpdate() || !isObjectExist(containerName, objectId)) {
                return null;
            }

            // Compute file digest
            Digest digest = new Digest(digestType);
            digest.update(objectPart);
            String streamDigest = digest.digestHex();

            // Check actual object digest (without cache for full checkup)
            String actualObjectDigest = defaultStorage.computeObjectDigest(containerName, objectId, digestType);

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
        }
    }

    private void logObjectWriteInOfferLog(String containerName, String objectId)
        throws ContentAddressableStorageServerException, ContentAddressableStorageDatabaseException {
        offerDatabaseService.save(containerName, objectId, "write");
    }

    private String putObject(String containerName, String objectId, InputStream objectPart, Long size,
        DigestType digestType) throws ContentAddressableStorageException {
        defaultStorage.putObject(containerName, objectId, objectPart, digestType, size);
        // Check digest AFTER writing in order to ensure correctness
        final String digest = defaultStorage.computeObjectDigest(containerName, objectId, digestType);
        return digest;
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
        ContainerInformation containerInformation;
        try {
            containerInformation = defaultStorage.getContainerInformation(containerName);
        } catch (ContentAddressableStorageNotFoundException exc) {
            defaultStorage.createContainer(containerName);
            containerInformation = defaultStorage.getContainerInformation(containerName);
        }
        result.put("usableSpace", containerInformation.getUsableSpace());
        result.put("usedSpace", containerInformation.getUsedSpace());
        return result;
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
        DataCategory type)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageException {
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
        return offerDatabaseService.searchOfferLog(containerName, offset, limit, order);
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
