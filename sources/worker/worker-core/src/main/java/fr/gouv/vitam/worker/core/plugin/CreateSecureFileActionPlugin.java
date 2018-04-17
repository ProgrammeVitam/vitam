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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.utils.MetadataDocumentHelper;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.json.CanonicalJsonFormatter;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.LifeCycleTraceabilitySecureFileObject;
import fr.gouv.vitam.common.model.MetadataType;
import fr.gouv.vitam.common.model.ObjectGroupDocumentHash;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookDocument;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleObjectGroup;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleUnit;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.common.utils.StorageClientUtil;
import fr.gouv.vitam.worker.core.handler.ActionHandler;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;


/**
 * CreateSecureFileAction Plugin.
 */
public abstract class CreateSecureFileActionPlugin extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CreateSecureFileActionPlugin.class);
    private static final String JSON_EXTENSION = ".json";
    private final DigestType digestType = VitamConfiguration.getDefaultDigestType();
    private final MetaDataClientFactory metaDataClientFactory;
    private final StorageClientFactory storageClientFactory;

    @VisibleForTesting CreateSecureFileActionPlugin(MetaDataClientFactory metaDataClientFactory,
        StorageClientFactory storageClientFactory) {
        this.metaDataClientFactory = metaDataClientFactory;
        this.storageClientFactory = storageClientFactory;
    }

    CreateSecureFileActionPlugin() {
        metaDataClientFactory = MetaDataClientFactory.getInstance();
        storageClientFactory = StorageClientFactory.getInstance();
    }

    void storeLifecycle(JsonNode lifecycle, String lfGuid, HandlerIO handlerIO, String lifecycleType)
        throws ProcessingException {
        try (
            StorageClient storageClient = storageClientFactory.getClient()) {

            LifeCycleTraceabilitySecureFileObject lfcTraceSecFileDataLine;

            ArrayNode events = (ArrayNode) lifecycle.get(LogbookDocument.EVENTS);
            String objectGroupId = lifecycle.get(LogbookMongoDbName.objectIdentifier.getDbname()).asText();
            JsonNode lastEvent = Iterables.getLast(events);
            String finalOutcome = lastEvent.get(LogbookMongoDbName.outcome.getDbname()).asText();
            String finalEventIdProc = lifecycle.get(LogbookMongoDbName.eventIdentifierProcess.getDbname()).asText();
            String finalEventTypeProc = lastEvent.get(LogbookMongoDbName.eventTypeProcess.getDbname()).asText();
            String finalEvDateTime = lastEvent.get(LogbookMongoDbName.eventDateTime.getDbname()).asText();

            final String hashLFC = generateDigest(lifecycle);

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

            String lfcAndMetadataGlobalHashFromStorage;
            String hashMetaData;
            MetadataType metadataType;
            String folder;

            if (LogbookLifeCycleUnit.class.getName().equals(lifecycleType)) {
                folder = SedaConstants.LFC_UNITS_FOLDER;
                JsonNode unit = selectArchiveUnitById(objectGroupId);
                metadataType = MetadataType.UNIT;
                MetadataDocumentHelper.removeComputedGraphFieldsFromUnit(unit);

                hashMetaData = generateDigest(unit);
                lfcAndMetadataGlobalHashFromStorage = StorageClientUtil.getLFCAndMetadataGlobalHashFromStorage(unit,
                    DataCategory.UNIT, lfGuid + JSON_EXTENSION, storageClient);
            } else if (LogbookLifeCycleObjectGroup.class.getName().equals(lifecycleType)) {
                folder = SedaConstants.LFC_OBJECTS_FOLDER;
                JsonNode og = selectObjectGroupById(objectGroupId);
                metadataType = MetadataType.OBJECTGROUP;
                MetadataDocumentHelper.removeComputedGraphFieldsFromObjectGroup(og);

                hashMetaData = generateDigest(og);
                lfcAndMetadataGlobalHashFromStorage = StorageClientUtil.getLFCAndMetadataGlobalHashFromStorage(og,
                    DataCategory.OBJECTGROUP, lfGuid + JSON_EXTENSION, storageClient);
                List<ObjectGroupDocumentHash> list = StorageClientUtil.extractListObjectsFromJson(og, storageClient);

                lfcTraceSecFileDataLine.setObjectGroupDocumentHashList(list);
            } else {
                throw new IllegalStateException("Unknown lifecycleType " + lifecycleType);
            }

            lfcTraceSecFileDataLine.setMetadataType(metadataType);
            lfcTraceSecFileDataLine.setHashMetadata(hashMetaData);
            lfcTraceSecFileDataLine.setHashGlobalFromStorage(lfcAndMetadataGlobalHashFromStorage);

            JsonNode jsonDataToWrite = JsonHandler.toJsonNode(lfcTraceSecFileDataLine);

            InputStream inputStream = CanonicalJsonFormatter.serialize(jsonDataToWrite);

            handlerIO.transferInputStreamToWorkspace(
                folder + "/" + lfGuid + JSON_EXTENSION, inputStream, null, false);

        } catch (IOException | InvalidParseOperationException e) {
            throw new ProcessingException("Could not serialize json object or could not write to file", e);
        }
    }

    private JsonNode selectArchiveUnitById(String archiveUnitId) throws ProcessingException {
        try (MetaDataClient metadataClient = metaDataClientFactory.getClient()) {
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


    private JsonNode selectObjectGroupById(String objectGroupId) throws ProcessingException {
        try (MetaDataClient metadataClient = metaDataClientFactory.getClient()) {
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
     * Generate a hash for a JsonNode using VITAM Digest Algorithm
     *
     * @param jsonNode the jsonNode to compute digest for
     * @return hash of the jsonNode
     */
    private String generateDigest(JsonNode jsonNode) throws IOException {
        final Digest digest = new Digest(digestType);
        digest.update(CanonicalJsonFormatter.serialize(jsonNode));
        return digest.digest64();
    }
}
