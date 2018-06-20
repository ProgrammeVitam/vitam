
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
package fr.gouv.vitam.worker.common.utils;

import static fr.gouv.vitam.storage.engine.common.model.response.StoredInfoResult.fromMetadataJson;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ObjectGroupDocumentHash;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.storage.driver.model.StorageMetadatasResult;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.response.StoredInfoResult;

/**
 * StorageClientUtil class
 */
public class StorageClientUtil {


    /**
     * retrieve global Hash (lfc+metadata{unit|og} or Object Doucment under og) from storage offers picked from the optimistic {@code SedaConstants.STORAGE}  node
     *
     * @param metadataOrDocumentJsonNode json document for the metadadataObject (Unit  or ObjectGroup) or plain document (raw type)
     * @param dataCategory               the data category
     * @param objectId                   the object id
     * @return the Digest of the document containing (LFC + METADATA) as saved in the storage offers
     * @throws ProcessingException Exception thrown when :
     *                             <ul>
     *                             li>
     *                             {@code StorageNotFoundClientException} or {@code StorageServerClientException} if no connection can be done for the given storage strategy
     *                             </li>
     *                             <li>
     *                             {@code InvalidParseOperationException} if no digest information was found when parsing json node
     *                             </li>
     *                             </ul>
     */
    public static String getLFCAndMetadataGlobalHashFromStorage(JsonNode metadataOrDocumentJsonNode,
        DataCategory dataCategory, String objectId, StorageClient storageClient)
        throws ProcessingException {

        String digest = null, firstDigest = null;

        try {
            // store binary data object

            StoredInfoResult mdOptimisticStorageInfos = fromMetadataJson(metadataOrDocumentJsonNode);

            JsonNode storageMetadatasResultListJsonNode = storageClient.
                getInformation(mdOptimisticStorageInfos.getStrategy(),
                    dataCategory,
                    objectId,
                    mdOptimisticStorageInfos.getOfferIds());

            boolean isFirstDigest = false;

            for (String offerId : mdOptimisticStorageInfos.getOfferIds()) {
                JsonNode metadataResultJsonNode = storageMetadatasResultListJsonNode.get(offerId);
                if (metadataResultJsonNode != null && metadataResultJsonNode.isObject()) {
                    StorageMetadatasResult storageMetadatasResult =
                        JsonHandler.getFromJsonNode(metadataResultJsonNode, StorageMetadatasResult.class);

                    if (storageMetadatasResult == null) {
                        throw new ProcessingException(String.format("The %s is null for the offer %s",
                            StorageMetadatasResult.class.getSimpleName(), offerId));
                    }

                    digest = storageMetadatasResult.getDigest();

                    if (!isFirstDigest) {
                        isFirstDigest = true;
                        firstDigest = String.copyValueOf(digest.toCharArray());
                    } else if (digest == null || !digest.equals(firstDigest)) {
                        throw new ProcessingException(
                            String.format(
                                "The digest '%s' for the offer '%s' is null or not equal to the first Offer globalDigest expected (%s)",

                                digest, offerId, firstDigest));
                    }

                }
            }

        } catch (StorageNotFoundClientException | StorageServerClientException | IllegalArgumentException e) {
            throw new ProcessingException("Exception when retrieving storage client ", e);
        } catch (InvalidParseOperationException e) {
            throw new ProcessingException("Exception when parsing JsonNode to StorageMetadatasResult object ", e);
        }

        return digest;
    }


    private static JsonNode getStorageResultsJsonNode(JsonNode objectMd, DataCategory dataCategory, String guid,
        StorageClient storageClient) throws StorageException {
        StoredInfoResult mdOptimisticStorageInfo = fromMetadataJson(objectMd);
        JsonNode storageMetadataResultListJsonNode;
        try {
            // store binary data object
            storageMetadataResultListJsonNode = storageClient.
                getInformation(mdOptimisticStorageInfo.getStrategy(),
                    dataCategory,
                    guid,
                    mdOptimisticStorageInfo.getOfferIds());

            return storageMetadataResultListJsonNode;

        } catch (StorageServerClientException | StorageNotFoundClientException e) {
            throw new StorageException("An error occurred during last traceability operation retrieval", e);
        }
    }

    /**
     * Extract Document Objects from ObjectGroup jsonNode and populate  {@link fr.gouv.vitam.common.model.LifeCycleTraceabilitySecureFileObject#objectGroupDocumentHashList}
     * with hash retrieved from storage offer
     *
     * @param og object
     * @return list of ObjectGroupDocumentHash
     * @throws ProcessingException
     */
    public static List<ObjectGroupDocumentHash> extractListObjectsFromJson(JsonNode og, StorageClient storageClient)
        throws ProcessingException {
        JsonNode qualifiers = og.get(SedaConstants.PREFIX_QUALIFIERS);

        List<ObjectGroupDocumentHash> objectGroupDocumentHashToList = new ArrayList<>();
        if (qualifiers != null && qualifiers.isArray() && qualifiers.size() > 0) {
            List<JsonNode> listQualifiers = JsonHandler.toArrayList((ArrayNode) qualifiers);
            for (final JsonNode qualifier : listQualifiers) {
                JsonNode versions = qualifier.get(SedaConstants.TAG_VERSIONS);
                if (versions.isArray() && versions.size() > 0) {
                    for (final JsonNode version : versions) {
                        if (version.get(SedaConstants.TAG_PHYSICAL_ID) != null) {
                            // Skip physical objects
                            continue;
                        }
                        String objectId = version.get(VitamDocument.ID).asText();
                        String lfcAndMetadataGlobalHashFromStorage =
                            getLFCAndMetadataGlobalHashFromStorage(version,
                                DataCategory.OBJECT, objectId, storageClient);
                        objectGroupDocumentHashToList
                            .add(new ObjectGroupDocumentHash(objectId, lfcAndMetadataGlobalHashFromStorage));
                    }


                }
            }
        }
        return objectGroupDocumentHashToList;
    }

}
