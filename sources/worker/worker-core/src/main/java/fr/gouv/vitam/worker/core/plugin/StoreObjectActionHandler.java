/**
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
 */
package fr.gouv.vitam.worker.core.plugin;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageAlreadyExistsClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.model.request.BulkObjectStoreRequest;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.storage.engine.common.model.response.BulkObjectStoreResponse;
import fr.gouv.vitam.storage.engine.common.model.response.StoredInfoResult;
import fr.gouv.vitam.worker.core.handler.ActionHandler;

/**
 *
 */
public abstract class StoreObjectActionHandler extends ActionHandler {

    private static final VitamLogger LOGGER =
        VitamLoggerFactory.getInstance(StoreObjectActionHandler.class);

    protected static final String DEFAULT_STRATEGY = "default";

    private static final String FILE_NAME = "FileName";
    private static final String OFFERS = "Offers";
    private static final String ALGORITHM = "Algorithm";
    private static final String DIGEST = "MessageDigest";

    private final StorageClientFactory storageClientFactory;

    public StoreObjectActionHandler(StorageClientFactory storageClientFactory) {
        this.storageClientFactory = storageClientFactory;
    }

    /**
     * The function is used for retrieving ObjectGroup in workspace and storing metaData in storage offer
     *
     * @param description the object description
     * @param itemStatus item status
     * @return StoredInfoResult
     */
    protected StoredInfoResult storeObject(ObjectDescription description,
        ItemStatus itemStatus) {

        try (final StorageClient storageClient = storageClientFactory.getClient()) {
            // store binary data object
            return storageClient.storeFileFromWorkspace(DEFAULT_STRATEGY, description.getType(),
                description.getObjectName(), description);

        } catch (StorageAlreadyExistsClientException e) {
            LOGGER.error(e);
            itemStatus.increment(StatusCode.KO);
        } catch (StorageNotFoundClientException e) {
            LOGGER.error(e);
            itemStatus.increment(StatusCode.FATAL);
        } catch (StorageServerClientException e) {
            LOGGER.error(e);
            itemStatus.increment(StatusCode.FATAL);
        }
        return null;
    }

    protected BulkObjectStoreResponse storeObjects(BulkObjectStoreRequest bulkObjectStoreRequest)
            throws StorageNotFoundClientException, StorageServerClientException, StorageAlreadyExistsClientException {

        try (final StorageClient storageClient = storageClientFactory.getClient()) {
            // store binary data objects
            return storageClient.bulkStoreFilesFromWorkspace(DEFAULT_STRATEGY, bulkObjectStoreRequest);

        }
    }

    /**
     * Helper to set _storage on node after storing it
     *
     * @param node
     * @param result
     */
    protected void storeStorageInfo(ObjectNode node, StoredInfoResult result) {
        LOGGER.debug("DEBUG result: {}", result);
        int nbc = result.getNbCopy();
        String strategy = result.getStrategy();
        List<String> offers = result.getOfferIds();
        ObjectNode storage = JsonHandler.createObjectNode();
        storage.put(SedaConstants.TAG_NB, nbc);
        ArrayNode offersId = JsonHandler.createArrayNode();
        if (offers != null) {
            for (String id : offers) {
                offersId.add(id);
            }
        }
        storage.set(SedaConstants.OFFER_IDS, offersId);
        storage.put(SedaConstants.STRATEGY_ID, strategy);
        node.set(SedaConstants.STORAGE, storage);
        LOGGER.debug("DEBUG node: {}", node);
    }

    protected void storeStorageInfos(String strategy, Map<String, JsonNode> nodes, BulkObjectStoreResponse result) {
        LOGGER.debug("DEBUG result: {}", result);
        Iterator<Map.Entry<String, String>> objectsDigests = result.getObjectDigests().entrySet().iterator();
        while(objectsDigests.hasNext()) {
            Map.Entry<String, String> objectDigest = objectsDigests.next();
            List<String> offers = result.getOfferIds();
            ObjectNode storage = JsonHandler.createObjectNode();
            storage.put(SedaConstants.TAG_NB, result.getOfferIds().size());
            ArrayNode offersId = JsonHandler.createArrayNode();
            if (offers != null) {
                for (String id : offers) {
                    offersId.add(id);
                }
            }
            storage.set(SedaConstants.OFFER_IDS, offersId);
            storage.put(SedaConstants.STRATEGY_ID, strategy);
            ((ObjectNode)nodes.get(objectDigest.getKey())).set(SedaConstants.STORAGE, storage);
            LOGGER.debug("DEBUG node: {}", nodes.get(objectDigest.getKey()));
        }
    }

    /**
     * detailsFromStorageInfo, get storage details as JSON String from storageInfo result
     *
     * @param result
     * @return JSON String
     */
    protected String detailsFromStorageInfo(StoredInfoResult result) {
        final ObjectNode object = JsonHandler.createObjectNode();

        if (result != null) {
            object.put(FILE_NAME, result.getId());
            object.put(ALGORITHM, result.getDigestType());
            object.put(DIGEST, result.getDigest());
            List<String> offers = result.getOfferIds();
            object.put(OFFERS, offers != null ? String.join(",", offers) : "");
        }

        return JsonHandler.unprettyPrint(object);
    }

    /**
     * detailsFromStorageInfo, get storage details as JSON String from storageInfo result
     *
     * @param result
     * @param itemStatusByObject
     */
    protected void updateSubTasksAndTasksFromStorageInfos(BulkObjectStoreResponse result, Map<String, ItemStatus> itemStatusByObject, ItemStatus itemStatus) {
        Iterator<Map.Entry<String, String>> objectsDigests = result.getObjectDigests().entrySet().iterator();
        while(objectsDigests.hasNext()) {
            ObjectNode object = JsonHandler.createObjectNode();
            Map.Entry<String, String> objectDigest = objectsDigests.next();
            object.put(FILE_NAME, objectDigest.getKey());
            object.put(ALGORITHM, result.getDigestType());
            object.put(DIGEST, objectDigest.getValue());
            List<String> offers = result.getOfferIds();
            object.put(OFFERS, offers != null ? String.join(",", offers) : "");
            itemStatusByObject.get(objectDigest.getKey()).increment(StatusCode.OK);
            itemStatusByObject.get(objectDigest.getKey()).setEvDetailData(JsonHandler.unprettyPrint(object));

            // increment itemStatus with subtask
            itemStatus.setSubTaskStatus(objectDigest.getKey(), itemStatusByObject.get(objectDigest.getKey()))
                    .increment(itemStatusByObject.get(objectDigest.getKey()).getGlobalStatus());
        }
    }
}
