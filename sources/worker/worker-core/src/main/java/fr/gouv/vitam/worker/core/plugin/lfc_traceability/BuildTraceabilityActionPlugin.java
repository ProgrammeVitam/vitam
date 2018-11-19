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
package fr.gouv.vitam.worker.core.plugin.lfc_traceability;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Iterables;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.alert.AlertService;
import fr.gouv.vitam.common.alert.AlertServiceImpl;
import fr.gouv.vitam.common.collection.CloseableIterator;
import fr.gouv.vitam.common.collection.CloseableIteratorUtils;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.database.utils.MetadataDocumentHelper;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.iterables.BulkIterator;
import fr.gouv.vitam.common.json.CanonicalJsonFormatter;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.LifeCycleTraceabilitySecureFileObject;
import fr.gouv.vitam.common.model.MetadataType;
import fr.gouv.vitam.common.model.ObjectGroupDocumentHash;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.performance.PerformanceLogger;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookDocument;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleObjectGroup;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleUnit;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName;
import fr.gouv.vitam.metadata.core.database.collections.MetadataDocument;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.response.BatchObjectInformationResponse;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.common.utils.StorageClientUtil;
import fr.gouv.vitam.worker.core.distribution.JsonLineIterator;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import fr.gouv.vitam.worker.core.distribution.JsonLineWriter;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static fr.gouv.vitam.metadata.core.database.collections.MetadataDocument.OG;

public abstract class BuildTraceabilityActionPlugin extends ActionHandler {

    private static final VitamLogger LOGGER =
        VitamLoggerFactory.getInstance(BuildTraceabilityActionPlugin.class);

    private static final int LFC_AND_METADATA_IN_RANK = 0;
    private static final int TRACEABILITY_DATA_OUT_RANK = 0;
    private static final String DEFAULT_STRATEGY = "default";
    private static final String JSON_EXTENSION = ".json";

    private final DigestType digestType = VitamConfiguration.getDefaultDigestType();
    private final StorageClientFactory storageClientFactory;
    private final int batchSize;
    private final AlertService alertService;

    public BuildTraceabilityActionPlugin() {
        this(
            StorageClientFactory.getInstance(),
            VitamConfiguration.getBatchSize(), new AlertServiceImpl());
    }

    @VisibleForTesting
    BuildTraceabilityActionPlugin(
        StorageClientFactory storageClientFactory,
        int batchSize, AlertService alertService) {
        this.storageClientFactory = storageClientFactory;
        this.batchSize = batchSize;
        this.alertService = alertService;
    }

    protected StatusCode buildTraceabilityData(HandlerIO handler, String lifecycleType) throws ProcessingException {
        List<String> offerIds = getOfferIds();

        File lfcAndMetadataFile = (File) handler.getInput(LFC_AND_METADATA_IN_RANK);
        File traceabilityDataFile =
            handler.getNewLocalFile(handler.getOutput(TRACEABILITY_DATA_OUT_RANK).getPath());

        int nbEntries = 0;

        try (InputStream is = new FileInputStream(lfcAndMetadataFile);
            JsonLineIterator jsonLineIterator = new JsonLineIterator(is);
            OutputStream os = new FileOutputStream(traceabilityDataFile);
            JsonLineWriter jsonLineWriter = new JsonLineWriter(os)) {

            CloseableIterator<LfcMetadataPair> lfcMetadataIterator = CloseableIteratorUtils
                .map(jsonLineIterator, BuildTraceabilityActionPlugin::parse);

            BulkIterator<LfcMetadataPair> bulkIterator = new BulkIterator<>(lfcMetadataIterator, batchSize);

            while (bulkIterator.hasNext()) {
                List<LfcMetadataPair> lfcMetadataPairList = bulkIterator.next();
                nbEntries += lfcMetadataPairList.size();

                processBulk(lfcMetadataPairList, offerIds, jsonLineWriter, lifecycleType);
            }

        } catch (IOException e) {
            throw new ProcessingException("Could not load storage information", e);
        }

        handler.addOutputResult(TRACEABILITY_DATA_OUT_RANK, traceabilityDataFile, true, false);

        LOGGER.info("Metadata traceability entries: " + nbEntries);

        return (nbEntries > 0) ? StatusCode.OK : StatusCode.WARNING;
    }

    private List<String> getOfferIds() throws ProcessingException {

        try (StorageClient storageClient = storageClientFactory.getClient()) {
            return storageClient.getOffers(DEFAULT_STRATEGY);
        } catch (StorageServerClientException | StorageNotFoundClientException e) {
            throw new ProcessingException("Could not load offer id list", e);
        }
    }

    private void processBulk(List<LfcMetadataPair> lfcMetadataPairList, List<String> offerIds,
        JsonLineWriter jsonLineWriter, String lifecycleType)
        throws ProcessingException {

        LOGGER.debug("Processing " + lfcMetadataPairList.size() + " traceability data entries");

        try (StorageClient storageClient = storageClientFactory.getClient()) {

            List<String> metadataFileNames = lfcMetadataPairList.stream()
                .map(lfcMetadataPair -> lfcMetadataPair.getLfc().get(LogbookDocument.ID).textValue() + JSON_EXTENSION)
                .collect(Collectors.toList());

            DataCategory dataCategory = lifecycleType.equals(LogbookLifeCycleUnit.class.getName()) ?
                DataCategory.UNIT : DataCategory.OBJECTGROUP;

            Map<String, Map<String, String>> offerDigestsByMetadataId =
                getOfferDigests(storageClient, dataCategory, offerIds, metadataFileNames);


            MultiValuedMap<String, String> objectIdsByMetadata = null;
            Map<String, Map<String, String>> offerDigestsByBinaryObjectIds = null;

            if (lifecycleType.equals(LogbookLifeCycleObjectGroup.class.getName())) {

                objectIdsByMetadata = new ArrayListValuedHashMap<>();
                for (LfcMetadataPair lfcMetadataPair : lfcMetadataPairList) {
                    String id = lfcMetadataPair.getLfc().get(LogbookDocument.ID).textValue();
                    JsonNode metadata = lfcMetadataPair.getMetadata();

                    List<String> binaryObjectIds = extractObjectIdsFromObjectGroup(metadata);
                    objectIdsByMetadata.putAll(id, binaryObjectIds);
                }

                offerDigestsByBinaryObjectIds =
                    getOfferDigests(storageClient, DataCategory.OBJECT, offerIds, objectIdsByMetadata.values());
            }

            for (LfcMetadataPair lfcMetadataPair : lfcMetadataPairList) {
                String id = lfcMetadataPair.getLfc().get(LogbookDocument.ID).textValue();

                JsonNode lifecycle = lfcMetadataPair.getLfc();
                JsonNode metadata = lfcMetadataPair.getMetadata();
                Map<String, String> metadataDigestsByOfferId = offerDigestsByMetadataId.get(id + JSON_EXTENSION);

                Map<String, Map<String, String>> binaryObjectDigestsByOffers = null;
                if (objectIdsByMetadata != null) {
                    binaryObjectDigestsByOffers = objectIdsByMetadata.get(id)
                        .stream()
                        .collect(Collectors.toMap(
                            objectId -> objectId, offerDigestsByBinaryObjectIds::get));
                }

                storeTraceabilityData(id, lifecycle, metadata, metadataDigestsByOfferId, binaryObjectDigestsByOffers,
                    lifecycleType, jsonLineWriter);
            }

        } catch (StorageServerClientException e) {
            throw new ProcessingException("Could not load storage information", e);
        }
    }

    private Map<String, Map<String, String>> getOfferDigests(StorageClient storageClient, DataCategory dataCategory,
        List<String> offerIds,
        Collection<String> objectNames)
        throws StorageServerClientException, ProcessingException {

        Stopwatch bulkObjectInformationSW = Stopwatch.createStarted();
        RequestResponse<BatchObjectInformationResponse> requestResponse =
            storageClient.getBatchObjectInformation(DEFAULT_STRATEGY, dataCategory, offerIds, objectNames);
        PerformanceLogger.getInstance().log(stepName(), actionName(),
            "BULK_DIGEST_" + dataCategory.getCollectionName().toUpperCase(),
            bulkObjectInformationSW.elapsed(TimeUnit.MILLISECONDS));

        if (!requestResponse.isOk()) {
            throw new ProcessingException(
                "Could not load storage information " + dataCategory + ": " +
                    ((VitamError) requestResponse).getDescription());
        }

        return ((RequestResponseOK<BatchObjectInformationResponse>) requestResponse).getResults()
            .stream()
            .collect(Collectors.toMap(BatchObjectInformationResponse::getObjectId,
                BatchObjectInformationResponse::getOfferDigests));
    }

    private static LfcMetadataPair parse(JsonLineModel entry) {
        try {
            return JsonHandler.getFromJsonNode(entry.getParams(), LfcMetadataPair.class);
        } catch (InvalidParseOperationException e) {
            throw new RuntimeException("Could not parse json line entry", e);
        }
    }

    private void storeTraceabilityData(String id, JsonNode lifecycle, JsonNode metadata,
        Map<String, String> metadataDigestsByOfferId,
        Map<String, Map<String, String>> offerDigestsByBinaryObjectIds,
        String lifecycleType,
        JsonLineWriter jsonLineWriter)
        throws ProcessingException {
        try {

            LifeCycleTraceabilitySecureFileObject lfcTraceSecFileDataLine;

            ArrayNode events = (ArrayNode) lifecycle.get(LogbookDocument.EVENTS);
            JsonNode lastEvent = Iterables.getLast(events);
            String finalOutcome = getJsonText(lastEvent, LogbookMongoDbName.outcome);
            String finalEventIdProc = getJsonText(lifecycle, LogbookMongoDbName.eventIdentifierProcess);
            String finalEventTypeProc = getJsonText(lastEvent, LogbookMongoDbName.eventTypeProcess);
            String finalEvDateTime = getJsonText(lastEvent, LogbookMongoDbName.eventDateTime);

            final String hashLFC = generateDigest(lifecycle, digestType);
            final String hashLFCEvents = generateDigest(events, digestType);

            lfcTraceSecFileDataLine = new LifeCycleTraceabilitySecureFileObject(
                finalEventIdProc,
                finalEventTypeProc,
                finalEvDateTime,
                lifecycle.get(LogbookDocument.ID).asText(), // _id not from DSL but from lifecycle
                null,
                lifecycle.get(LogbookDocument.VERSION).asInt(),
                finalOutcome,
                hashLFC,
                hashLFCEvents,
                null,
                null,
                null);

            String lfcAndMetadataGlobalHashFromStorage;
            String hashMetaData;
            MetadataType metadataType;

            if (LogbookLifeCycleUnit.class.getName().equals(lifecycleType)) {

                metadataType = MetadataType.UNIT;
                MetadataDocumentHelper.removeComputedFieldsFromUnit(metadata);

                if (metadata.get(OG) != null) {
                    lfcTraceSecFileDataLine.setIdGot(metadata.get(OG).textValue());
                }

                ArrayList<String> parents = new ArrayList<>();
                if (metadata.has(MetadataDocument.UP)) {
                    metadata.get(MetadataDocument.UP).forEach(up -> parents.add(up.asText()));
                }
                lfcTraceSecFileDataLine.setUp(parents);

                hashMetaData = generateDigest(metadata, digestType);
                lfcAndMetadataGlobalHashFromStorage = StorageClientUtil.aggregateOfferDigests(metadataDigestsByOfferId,
                    DataCategory.UNIT, id + JSON_EXTENSION, alertService);
            } else if (LogbookLifeCycleObjectGroup.class.getName().equals(lifecycleType)) {
                metadataType = MetadataType.OBJECTGROUP;
                MetadataDocumentHelper.removeComputedFieldsFromObjectGroup(metadata);

                hashMetaData = generateDigest(metadata, digestType);
                lfcAndMetadataGlobalHashFromStorage = StorageClientUtil.aggregateOfferDigests(metadataDigestsByOfferId,
                    DataCategory.OBJECTGROUP, id + JSON_EXTENSION, alertService);

                List<ObjectGroupDocumentHash> list =
                    offerDigestsByBinaryObjectIds.entrySet()
                        .stream()
                        .map(entry -> new ObjectGroupDocumentHash(
                            entry.getKey(),
                            StorageClientUtil.aggregateOfferDigests(
                                entry.getValue(), DataCategory.OBJECT, entry.getKey(), alertService))
                        ).collect(Collectors.toList());

                ArrayList<String> auParents = new ArrayList<>();
                if (metadata.has(MetadataDocument.UP)) {
                    metadata.get(MetadataDocument.UP).forEach(up -> auParents.add(up.asText()));
                }
                lfcTraceSecFileDataLine.setUp(auParents);

                lfcTraceSecFileDataLine.setObjectGroupDocumentHashList(list);
            } else {
                throw new IllegalStateException("Unknown lifecycleType " + lifecycleType);
            }

            lfcTraceSecFileDataLine.setMetadataType(metadataType);
            lfcTraceSecFileDataLine.setHashMetadata(hashMetaData);
            lfcTraceSecFileDataLine.setHashGlobalFromStorage(lfcAndMetadataGlobalHashFromStorage);

            JsonNode jsonDataToWrite = JsonHandler.toJsonNode(lfcTraceSecFileDataLine);

            jsonLineWriter.addEntry(new JsonLineModel(id, null, jsonDataToWrite));

        } catch (IOException | InvalidParseOperationException e) {
            throw new ProcessingException("Could not serialize json object or could not write to file", e);
        }
    }

    private String getJsonText(JsonNode lastEvent, LogbookMongoDbName outcome) {
        if (lastEvent == null || !lastEvent.has(outcome.getDbname())) {
            return null;
        }
        return lastEvent.get(outcome.getDbname()).asText();
    }

    /**
     * Extract list of binary object ids
     */
    public static List<String> extractObjectIdsFromObjectGroup(JsonNode og) {
        JsonNode qualifiers = og.get(SedaConstants.PREFIX_QUALIFIERS);

        List<String> result = new ArrayList<>();
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
                        result.add(objectId);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Generate a hash for a JsonNode using VITAM Digest Algorithm
     *
     * @param jsonNode the jsonNode to compute digest for
     * @param digestType
     * @return hash of the jsonNode
     */
    public static String generateDigest(JsonNode jsonNode, DigestType digestType) throws IOException {
        final Digest digest = new Digest(digestType);
        digest.update(CanonicalJsonFormatter.serialize(jsonNode));
        return digest.digest64();
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // NOP.
    }

    protected abstract String stepName();

    protected abstract String actionName();
}
