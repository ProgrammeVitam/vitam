/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.worker.core.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Iterables;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.LifeCycleTraceabilitySecureFileObject;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookDocument;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleObjectGroup;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleUnit;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.storage.driver.model.StorageMetadatasResult;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.response.StoredInfoResult;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;

import static fr.gouv.vitam.storage.engine.common.model.response.StoredInfoResult.fromMetadataJson;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;


/**
 * CreateSecureFileAction Plugin.<br>
 */
public abstract class CreateSecureFileActionPlugin extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CreateSecureFileActionPlugin.class);
    private static final String JSON_EXTENSION = ".json";
    private final DigestType digestType = VitamConfiguration.getDefaultDigestType();


    protected void storeLifecycle(JsonNode lifecycle, String lfGuid, HandlerIO handlerIO, String lifecycleType)
        throws ProcessingException {
        String folder = "";
        final File lifecycleGlobalTmpFile = handlerIO.getNewLocalFile(lfGuid);
        try (FileWriter fw = new FileWriter(lifecycleGlobalTmpFile);) {

            LifeCycleTraceabilitySecureFileObject lfcTraceSecFileDataLine;

            ArrayNode events = (ArrayNode) lifecycle.get(LogbookDocument.EVENTS);
            String objectGroupId = lifecycle.get(LogbookMongoDbName.objectIdentifier.getDbname()).asText();
            JsonNode lastEvent = Iterables.getLast(events);
            String finalOutcome = lastEvent.get(LogbookMongoDbName.outcome.getDbname()).asText();
            String finalEventIdProc = lifecycle.get(LogbookMongoDbName.eventIdentifierProcess.getDbname()).asText();
            String finalEventTypeProc = lastEvent.get(LogbookMongoDbName.eventTypeProcess.getDbname()).asText();
            String finalEvDateTime = lastEvent.get(LogbookMongoDbName.eventDateTime.getDbname()).asText();

            final String hashLFC = generateDigest(lifecycle);

            String lfcAndMetadataGlobalHashFromStorage = null;
            String hashMetaData = null;
            LifeCycleTraceabilitySecureFileObject.MetadataType metadataType = null;

            lfcTraceSecFileDataLine = new LifeCycleTraceabilitySecureFileObject(
                finalEventIdProc,
                finalEventTypeProc,
                finalEvDateTime,
                lifecycle.get(LogbookDocument.ID).asText(),//_id not from DSL but from lifecycle
                null,
                lifecycle.get(LogbookDocument.VERSION).asInt(),
                finalOutcome,
                hashLFC,
                null,
                null,
                null
            );

            if (LogbookLifeCycleUnit.class.getName().equals(lifecycleType)) {
                folder = SedaConstants.LFC_UNITS_FOLDER;
                JsonNode unit = selectArchiveUnitById(objectGroupId);
                metadataType = LifeCycleTraceabilitySecureFileObject.MetadataType.UNIT;
                hashMetaData = generateDigest(unit);
                lfcAndMetadataGlobalHashFromStorage = getLFCAndMetadataGlobalHashFromStorage(unit,
                    DataCategory.UNIT, lfGuid + JSON_EXTENSION);
            } else if (LogbookLifeCycleObjectGroup.class.getName().equals(lifecycleType)) {
                folder = SedaConstants.LFC_OBJECTS_FOLDER;
                JsonNode og = selectObjectGroupById(objectGroupId);
                metadataType = LifeCycleTraceabilitySecureFileObject.MetadataType.OBJECTGROUP;
                hashMetaData = generateDigest(og);
                lfcAndMetadataGlobalHashFromStorage = getLFCAndMetadataGlobalHashFromStorage(og,
                    DataCategory.OBJECTGROUP, lfGuid + JSON_EXTENSION);
                extractListObjectsFromJson(og, lfcTraceSecFileDataLine);
            }

            lfcTraceSecFileDataLine.setMetadataType(metadataType);
            lfcTraceSecFileDataLine.setHashMetadata(hashMetaData);
            lfcTraceSecFileDataLine.setHashGlobalFromStorage(lfcAndMetadataGlobalHashFromStorage);

            JsonNode jsonDataToWrite = JsonHandler.toJsonNode(lfcTraceSecFileDataLine);

            fw.write(jsonDataToWrite.toString());
            fw.flush();
            fw.close();
            // TODO : this is not a json file
            handlerIO
                .transferFileToWorkspace(folder + "/" + lfGuid + JSON_EXTENSION,
                    lifecycleGlobalTmpFile, true, false);
        } catch (IOException | InvalidParseOperationException e) {
            throw new ProcessingException("Could not serialize json object or could not write to file", e);
        }
    }

    private JsonNode selectArchiveUnitById(String archiveUnitId) throws ProcessingException {
        try (MetaDataClient metadataClient = MetaDataClientFactory.getInstance().getClient()) {
            RequestResponse<JsonNode> requestResponse =
                metadataClient.getUnitByIdRaw(archiveUnitId);

            if (requestResponse.isOk()) {
                return ((RequestResponseOK<JsonNode>) requestResponse).getFirstResult();
            } else {
                throw new ProcessingException("Document not found");
            }
        } catch (VitamClientException e) {
            throw new ProcessingException(e);
        }
    }

    /**
     * Extract Document Objects from ObjectGroup jsonNode and populate  {@link fr.gouv.vitam.common.model.LifeCycleTraceabilitySecureFileObject#objectGroupDocumentHashList}
     * with hash retrieved from storage offer
     *
     * @param og
     * @param lfcTracabilityScureFileDataLine
     * @return
     * @throws ProcessingException
     */
    private void extractListObjectsFromJson(JsonNode og,
        LifeCycleTraceabilitySecureFileObject lfcTracabilityScureFileDataLine) throws ProcessingException {
        JsonNode qualifiers = og.get(SedaConstants.PREFIX_QUALIFIERS);
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
                        lfcTracabilityScureFileDataLine.addObjectGroupDocumentHashToList(
                            objectId, getLFCAndMetadataGlobalHashFromStorage(version,
                                DataCategory.OBJECT, objectId)
                        );
                    }
                }
            }
        }

    }

    private JsonNode selectObjectGroupById(String objectGroupId) throws ProcessingException {
        try (MetaDataClient metadataClient = MetaDataClientFactory.getInstance().getClient()) {
            RequestResponse<JsonNode> requestResponse =
                metadataClient.getObjectGroupByIdRaw(objectGroupId);

            if (requestResponse.isOk()) {
                return ((RequestResponseOK<JsonNode>) requestResponse).getFirstResult();
            } else {
                throw new ProcessingException("Document not found");
            }
        } catch (VitamClientException e) {
            throw new ProcessingException(e);
        }
    }

    /**
     * retrieve global Hash (lfc+metadata{unit|og} or Object Doucment under og) from storage offers picked from the optimistic {@code SedaConstants.STORAGE}  node
     *
     * @param metadataOrDocumentJsonNode json document for the metadadataObject (Unit  or ObjectGroup) or plain document (raw type)
     * @param dataCategory the data category
     * @param objectId the object id
     * @return the Digest of the document containing (LFC + METADATA) as saved in the storage offers
     * @throws ProcessingException Exception thrown when :
     * <ul>
     *    li>
     *       {@code StorageNotFoundClientException} or {@code StorageServerClientException} if no connection can be done for the given storage strategy
     *     </li>
     *     <li>
     *       {@code InvalidParseOperationException} if no digest information was found when parsing json node
     *     </li>
     * </ul>
     */
    private String getLFCAndMetadataGlobalHashFromStorage(JsonNode metadataOrDocumentJsonNode,
        DataCategory dataCategory, String objectId)
        throws ProcessingException {

        final StorageClientFactory storageClientFactory = StorageClientFactory.getInstance();
        String digest = null, firstDigest = null;

        try (final StorageClient storageClient = storageClientFactory.getClient()) {
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
                        logAndThrowProcessingException("The {} is null for the offer {}", null,
                            StorageMetadatasResult.class.getSimpleName(), offerId);
                    }

                    digest = storageMetadatasResult.getDigest();

                    if (!isFirstDigest) {
                        isFirstDigest = true;
                        firstDigest = String.copyValueOf(digest.toCharArray());
                    } else if (digest == null || !digest.equals(firstDigest)) {
                        logAndThrowProcessingException(
                            "The digest '{}' for the offer '{}' is null or not equal to the first Offer globalDigest expected ({})",
                            null,
                            digest, offerId, firstDigest);
                    }

                }
            }

        } catch (StorageNotFoundClientException | StorageServerClientException | IllegalArgumentException e) {
            logAndThrowProcessingException("Exception when retrieving storage client ", e);
        } catch (InvalidParseOperationException e) {
            logAndThrowProcessingException("Exception when parsing JsonNode to StorageMetadatasResult object ", e);
        }

        return digest;
    }

    /**
     * log and throw ProcessingException to abort Handler Work
     *
     * @param msg
     * @param exception
     * @throws ProcessingException
     */
    private void logAndThrowProcessingException(String msg, Exception exception, Object... args)
        throws ProcessingException {
        if (exception != null) {
            LOGGER.error(msg, args, exception);
            throw new ProcessingException(msg, exception);
        } else {
            LOGGER.error(msg, args);
            throw new ProcessingException(msg);
        }

    }

    /**
     * Generate a hash for a JsonNode using VITAM Digest Algorithm
     *
     * @param jsonNode the jsonNode to compute digest for
     * @return hash of the jsonNode
     */
    private String generateDigest(JsonNode jsonNode) {
        final Digest digest = new Digest(digestType);
        digest.update(JsonHandler.unprettyPrint(jsonNode).getBytes(StandardCharsets.UTF_8));
        return digest.digest64();
    }

}
