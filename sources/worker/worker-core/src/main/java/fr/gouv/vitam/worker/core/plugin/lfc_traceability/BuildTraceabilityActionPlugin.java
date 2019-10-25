/*
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
package fr.gouv.vitam.worker.core.plugin.lfc_traceability;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
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
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.json.CanonicalJsonFormatter;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.LifeCycleTraceabilitySecureFileObject;
import fr.gouv.vitam.common.model.MetadataStorageHelper;
import fr.gouv.vitam.common.model.MetadataType;
import fr.gouv.vitam.common.model.ObjectGroupDocumentHash;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.TraceabilityHashDetails;
import fr.gouv.vitam.common.performance.PerformanceLogger;
import fr.gouv.vitam.logbook.common.model.TraceabilityStatistics;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookDocument;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleObjectGroup;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleUnit;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName;
import fr.gouv.vitam.metadata.core.database.collections.MetadataDocument;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.response.BatchObjectInformationResponse;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.distribution.JsonLineIterator;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import fr.gouv.vitam.worker.core.distribution.JsonLineWriter;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import org.apache.commons.collections4.ListUtils;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static fr.gouv.vitam.metadata.core.database.collections.MetadataDocument.OG;
import static java.util.stream.Collectors.toMap;

public abstract class BuildTraceabilityActionPlugin extends ActionHandler {

    private static final VitamLogger LOGGER =
        VitamLoggerFactory.getInstance(BuildTraceabilityActionPlugin.class);

    private static final int LFC_AND_METADATA_IN_RANK = 0;
    private static final int TRACEABILITY_DATA_OUT_RANK = 0;
    private static final int TRACEABILITY_STATISTICS_OUT_RANK = 1;
    private static final String JSON_EXTENSION = ".json";
    private static final String MESSAGE_DIGEST = "MessageDigest";
    private static final String STRATEGY_ID_FIELD = "strategyId";
    private static final String STORAGE_FIELD = "_storage";

    private final DigestType digestType = VitamConfiguration.getDefaultDigestType();
    private final StorageClientFactory storageClientFactory;
    private final int batchSize;
    private final AlertService alertService;

    public BuildTraceabilityActionPlugin() {
        this(
            StorageClientFactory.getInstance(),
            VitamConfiguration.getBatchSize(),
            new AlertServiceImpl());
    }

    @VisibleForTesting
    BuildTraceabilityActionPlugin(
        StorageClientFactory storageClientFactory,
        int batchSize,
        AlertService alertService) {
        this.storageClientFactory = storageClientFactory;
        this.batchSize = batchSize;
        this.alertService = alertService;
    }

    protected void buildTraceabilityData(HandlerIO handler, String lifecycleType,
        ItemStatus itemStatus) throws ProcessingException {

        File lfcAndMetadataFile = (File) handler.getInput(LFC_AND_METADATA_IN_RANK);
        File traceabilityDataFile =
            handler.getNewLocalFile(handler.getOutput(TRACEABILITY_DATA_OUT_RANK).getPath());

        DigestValidator digestValidator = new DigestValidator(alertService);
        StrategyIdOfferIdLoader strategyIdOfferIdLoader = new StrategyIdOfferIdLoader(
            this.storageClientFactory);

        int nbEntries = 0;

        try (InputStream is = new FileInputStream(lfcAndMetadataFile);
            JsonLineIterator jsonLineIterator = new JsonLineIterator(is);
            OutputStream os = new FileOutputStream(traceabilityDataFile);
            JsonLineWriter jsonLineWriter = new JsonLineWriter(os)) {

            CloseableIterator<LfcMetadataPair> lfcMetadataIterator = CloseableIteratorUtils
                .map(jsonLineIterator, BuildTraceabilityActionPlugin::parse);

            Iterator<List<LfcMetadataPair>> bulkIterator = Iterators.partition(lfcMetadataIterator, batchSize);

            while (bulkIterator.hasNext()) {
                List<LfcMetadataPair> lfcMetadataPairList = bulkIterator.next();
                nbEntries += lfcMetadataPairList.size();

                processBulk(lfcMetadataPairList, jsonLineWriter, lifecycleType, digestValidator, strategyIdOfferIdLoader);
            }

        } catch (IOException e) {
            throw new ProcessingException("Could not load storage information", e);
        }

        handler.addOutputResult(TRACEABILITY_DATA_OUT_RANK, traceabilityDataFile, true, false);

        TraceabilityStatistics traceabilityStatistics = getTraceabilityStatistics(digestValidator);
        try {
            File traceabilityStatsFile = handler.getNewLocalFile(
                handler.getOutput(TRACEABILITY_STATISTICS_OUT_RANK).getPath());
            JsonHandler.writeAsFile(traceabilityStatistics, traceabilityStatsFile);

            handler.addOutputResult(TRACEABILITY_STATISTICS_OUT_RANK, traceabilityStatsFile, true, false);
        } catch (InvalidParseOperationException e) {
            throw new ProcessingException("Could not serialize validation statistics", e);
        }

        LOGGER.info("Traceability statistics: " + traceabilityStatistics);
        if (nbEntries == 0) {
            itemStatus.increment(StatusCode.WARNING);
            itemStatus.setEvDetailData(buildEvDetData("No metadata entries to secure"));
        } else if (digestValidator.hasInconsistencies()) {
            itemStatus.increment(StatusCode.WARNING);
            itemStatus.setEvDetailData(buildEvDetData("Inconsistencies found"));
        } else {
            itemStatus.increment(StatusCode.OK);
        }
    }

    protected abstract TraceabilityStatistics getTraceabilityStatistics(DigestValidator digestValidator);

    private String buildEvDetData(String message) {
        Map<String, String> evDetData = new HashMap<>();
        evDetData.put("message", message);
        return JsonHandler.unprettyPrint(evDetData);
    }

    private void processBulk(List<LfcMetadataPair> lfcMetadataPairList, JsonLineWriter jsonLineWriter,
        String lifecycleType, DigestValidator digestValidator,
        StrategyIdOfferIdLoader strategyIdOfferIdLoader)
        throws ProcessingException {

        LOGGER.debug("Processing " + lfcMetadataPairList.size() + " traceability data entries");

        try (StorageClient storageClient = storageClientFactory.getClient()) {

            DataCategory dataCategory = lifecycleType.equals(LogbookLifeCycleUnit.class.getName()) ?
                DataCategory.UNIT : DataCategory.OBJECTGROUP;

            Map<String, DigestValidationDetails> metadataDigestsById = computeMetadataDigests(
                lfcMetadataPairList, lifecycleType, storageClient, dataCategory, digestValidator,
                strategyIdOfferIdLoader);

            Map<String, Map<String, DigestValidationDetails>> objectDigestsByObjectGroupId =
                computeObjectDigests(lfcMetadataPairList, lifecycleType, storageClient, digestValidator,
                    strategyIdOfferIdLoader);

            for (LfcMetadataPair lfcMetadataPair : lfcMetadataPairList) {
                String id = lfcMetadataPair.getLfc().get(LogbookDocument.ID).textValue();

                JsonNode lifecycle = lfcMetadataPair.getLfc();
                JsonNode metadata = lfcMetadataPair.getMetadata();

                storeTraceabilityData(id, lifecycle, metadata,
                    metadataDigestsById.get(id),
                    objectDigestsByObjectGroupId.get(id), lifecycleType, jsonLineWriter);
            }

        } catch (StorageServerClientException | IOException e) {
            throw new ProcessingException("Could not load storage information", e);
        }
    }

    private Map<String, DigestValidationDetails> computeMetadataDigests(
        List<LfcMetadataPair> lfcMetadataPairList, String lifecycleType,
        StorageClient storageClient, DataCategory dataCategory,
        DigestValidator digestValidator,
        StrategyIdOfferIdLoader strategyIdOfferIdLoader)
        throws IOException, StorageServerClientException, ProcessingException {

        Map<String, String> metadataDigestsInDb = computeMetadataDigestsInDb(lfcMetadataPairList, lifecycleType);

        // Group metadata ids by strategy Id
        MultiValuedMap<String, String> metadataIdsByStrategyId = new ArrayListValuedHashMap<>();
        for (LfcMetadataPair lfcMetadataPair : lfcMetadataPairList) {
            JsonNode metadata = lfcMetadataPair.getMetadata();
            String id = metadata.get(LogbookDocument.ID).textValue();

            String strategyID = getStrategyId(metadata);
            metadataIdsByStrategyId.put(strategyID, id);
        }

        Map<String, DigestValidationDetails> result = new HashMap<>();
        for (String strategyId : metadataIdsByStrategyId.keySet()) {

            Collection<String> metadataIds = metadataIdsByStrategyId.get(strategyId);
            Collection<String> offerIds = strategyIdOfferIdLoader.getOfferIds(strategyId);

            Collection<String> metadataFilenames = metadataIds.stream()
                .map(id -> id + JSON_EXTENSION)
                .collect(Collectors.toList());

            Map<String, Map<String, String>> offerDigestsByMetadataFilenames =
                getOfferDigests(storageClient, dataCategory, strategyId, offerIds, metadataFilenames);

            for (String id : metadataIds) {
                DigestValidationDetails digestValidationDetails = digestValidator
                    .validateMetadataDigest(id, strategyId, metadataDigestsInDb.get(id),
                        offerDigestsByMetadataFilenames.get(id + JSON_EXTENSION));
                result.put(id, digestValidationDetails);
            }
        }

        return result;
    }

    private Map<String, String> computeMetadataDigestsInDb(List<LfcMetadataPair> lfcMetadataPairList,
        String lifecycleType)
        throws IOException {
        Map<String, String> dbMetadataWithLfcDigests = new HashMap<>();
        for (LfcMetadataPair lfcMetadataPair : lfcMetadataPairList) {

            JsonNode metadataWithLfc = getMetadataWithLifecycle(lifecycleType, lfcMetadataPair);

            Digest digest = new Digest(digestType);
            digest.update(CanonicalJsonFormatter.serialize(metadataWithLfc));
            String dbDigest = digest.digestHex();

            String id = lfcMetadataPair.getLfc().get(LogbookDocument.ID).textValue();
            dbMetadataWithLfcDigests.put(id, dbDigest);
        }
        return dbMetadataWithLfcDigests;
    }

    private JsonNode getMetadataWithLifecycle(String lifecycleType, LfcMetadataPair lfcMetadataPair) {
        JsonNode metadataWithLfc;
        if (lifecycleType.equals(LogbookLifeCycleObjectGroup.class.getName())) {
            JsonNode unit = lfcMetadataPair.getMetadata();
            MetadataDocumentHelper.removeComputedFieldsFromObjectGroup(unit);
            metadataWithLfc = MetadataStorageHelper.getGotWithLFC(unit, lfcMetadataPair.getLfc());
        } else {
            JsonNode objectGroup = lfcMetadataPair.getMetadata();
            MetadataDocumentHelper.removeComputedFieldsFromUnit(objectGroup);
            metadataWithLfc = MetadataStorageHelper.getUnitWithLFC(objectGroup, lfcMetadataPair.getLfc());
        }
        return metadataWithLfc;
    }

    private Map<String, Map<String, DigestValidationDetails>> computeObjectDigests(
        List<LfcMetadataPair> lfcMetadataPairList, String lifecycleType,
        StorageClient storageClient, DigestValidator digestValidator, StrategyIdOfferIdLoader strategyIdOfferIdLoader)
        throws StorageServerClientException, ProcessingException {

        if (lifecycleType.equals(LogbookLifeCycleUnit.class.getName())) {
            return Collections.emptyMap();
        }

        Map<String, String> objectGroupIdByObjectId = new HashMap<>();
        ArrayListValuedHashMap<String, String> objectIdsByStrategyId = new ArrayListValuedHashMap<>();
        Map<String, String> dbDigestByObjectId = new HashMap<>();

        for (LfcMetadataPair lfcMetadataPair : lfcMetadataPairList) {

            String objectGroupId = lfcMetadataPair.getLfc().get(LogbookDocument.ID).textValue();
            JsonNode objectGroup = lfcMetadataPair.getMetadata();

            JsonNode qualifiers = objectGroup.get(SedaConstants.PREFIX_QUALIFIERS);

            for (final JsonNode qualifier : qualifiers) {
                JsonNode versions = qualifier.get(SedaConstants.TAG_VERSIONS);
                for (final JsonNode version : versions) {
                    if (version.get(SedaConstants.TAG_PHYSICAL_ID) != null) {
                        // Skip physical objects
                        continue;
                    }

                    String objectId = version.get(VitamDocument.ID).asText();
                    String strategyId = getStrategyId(version);
                    String digestInDb = version.get(MESSAGE_DIGEST).asText();

                    objectGroupIdByObjectId.put(objectId, objectGroupId);
                    objectIdsByStrategyId.put(strategyId, objectId);
                    dbDigestByObjectId.put(objectId, digestInDb);
                }
            }
        }

        Map<String, Map<String, DigestValidationDetails>> result = new HashMap<>();
        for (String strategyId : objectIdsByStrategyId.keySet()) {

            List<String> objectIds = objectIdsByStrategyId.get(strategyId);
            List<String> offerIds = strategyIdOfferIdLoader.getOfferIds(strategyId);

            Map<String, Map<String, String>> offerDigestsByObjectIds = getOfferDigests(
                storageClient, DataCategory.OBJECT, strategyId, offerIds, objectIds, batchSize);

            for (String objectId : offerDigestsByObjectIds.keySet()) {

                Map<String, String> offerDigest = offerDigestsByObjectIds.get(objectId);
                String dbDigest = dbDigestByObjectId.get(objectId);
                String objectGroupId = objectGroupIdByObjectId.get(objectId);

                Map<String, DigestValidationDetails> digestValidationDetailsByObjectId =
                    result.computeIfAbsent(objectGroupId, (unused) -> new HashMap<>());

                digestValidationDetailsByObjectId.put(
                    objectId, digestValidator.validateObjectDigest(objectId, strategyId, dbDigest, offerDigest));
            }

        }

        return result;
    }

    private String getStrategyId(JsonNode metadata) {

        String id = metadata.get(LogbookDocument.ID).textValue();

        JsonNode storageNode = metadata.get(STORAGE_FIELD);
        if (storageNode == null || !storageNode.isObject()) {
            throw new IllegalStateException("No storage information for metadata " + id);
        }

        JsonNode strategyNode = storageNode.get(STRATEGY_ID_FIELD);
        if (strategyNode == null || !strategyNode.isTextual()) {
            throw new IllegalStateException("No strategy id information for metadata " + id);
        }

        return strategyNode.asText();
    }

    private Map<String, Map<String, String>> getOfferDigests(StorageClient storageClient, DataCategory dataCategory,
        String strategyId, List<String> offerIds, List<String> dbDigests, int batchSize)
        throws ProcessingException, StorageServerClientException {

        List<List<String>> partitions = ListUtils.partition(dbDigests, batchSize);

        Map<String, Map<String, String>> result = new HashMap<>();
        for (List<String> partition : partitions) {
            result.putAll(getOfferDigests(storageClient, dataCategory, strategyId, offerIds, partition));
        }
        return result;
    }

    private Map<String, Map<String, String>> getOfferDigests(StorageClient storageClient, DataCategory dataCategory,
        String strategyId, Collection<String> offerIds, Collection<String> ids)
        throws StorageServerClientException, ProcessingException {

        Stopwatch bulkObjectInformationSW = Stopwatch.createStarted();
        RequestResponse<BatchObjectInformationResponse> requestResponse =
            storageClient.getBatchObjectInformation(strategyId, dataCategory, offerIds, ids);
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
            .collect(toMap(BatchObjectInformationResponse::getObjectId,
                BatchObjectInformationResponse::getOfferDigests));
    }

    private static LfcMetadataPair parse(JsonLineModel entry) {
        try {
            return JsonHandler.getFromJsonNode(entry.getParams(), LfcMetadataPair.class);
        } catch (InvalidParseOperationException e) {
            throw new VitamRuntimeException("Could not parse json line entry", e);
        }
    }

    private void storeTraceabilityData(String id, JsonNode lifecycle, JsonNode metadata,
        DigestValidationDetails metadataDigestValidationDetails,
        Map<String, DigestValidationDetails> objectDigests,
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

            ArrayList<String> parents = new ArrayList<>();
            if (metadata.has(MetadataDocument.UP)) {
                metadata.get(MetadataDocument.UP).forEach(up -> parents.add(up.asText()));
            }
            lfcTraceSecFileDataLine.setUp(parents);

            TraceabilityHashDetails metadataTraceabilityHashDetails = new TraceabilityHashDetails()
                .setStrategyId(metadataDigestValidationDetails.getStrategyId())
                .setOfferIds(metadataDigestValidationDetails.getOfferIds());

            if (metadataDigestValidationDetails.hasInconsistencies()) {
                // Store digest details only when inconsistencies occurred
                metadataTraceabilityHashDetails.setDbHash(metadataDigestValidationDetails.getDigestInDb());
                metadataTraceabilityHashDetails.setOfferHashes(metadataDigestValidationDetails.getDigestByOfferId());
            }

            lfcTraceSecFileDataLine.setTraceabilityHashDetails(metadataTraceabilityHashDetails);
            lfcTraceSecFileDataLine.setHashGlobalFromStorage(metadataDigestValidationDetails.getGlobalDigest());

            if (LogbookLifeCycleUnit.class.getName().equals(lifecycleType)) {

                lfcTraceSecFileDataLine.setMetadataType(MetadataType.UNIT);

                MetadataDocumentHelper.removeComputedFieldsFromUnit(metadata);
                lfcTraceSecFileDataLine.setHashMetadata(generateDigest(metadata, digestType));

                if (metadata.get(OG) != null) {
                    lfcTraceSecFileDataLine.setIdGot(metadata.get(OG).textValue());
                }

            } else if (LogbookLifeCycleObjectGroup.class.getName().equals(lifecycleType)) {

                lfcTraceSecFileDataLine.setMetadataType(MetadataType.OBJECTGROUP);

                MetadataDocumentHelper.removeComputedFieldsFromObjectGroup(metadata);
                lfcTraceSecFileDataLine.setHashMetadata(generateDigest(metadata, digestType));

                List<ObjectGroupDocumentHash> objectHashes = new ArrayList<>();

                if (objectDigests != null) {
                    for (String objectId : objectDigests.keySet()) {

                        DigestValidationDetails objectDigestValidationDetails = objectDigests.get(objectId);

                        TraceabilityHashDetails objectTraceabilityHashDetails = new TraceabilityHashDetails()
                            .setStrategyId(objectDigestValidationDetails.getStrategyId())
                            .setOfferIds(objectDigestValidationDetails.getOfferIds());

                        if (objectDigestValidationDetails.hasInconsistencies()) {
                            // Store digest details only when inconsistencies occurred
                            objectTraceabilityHashDetails.setDbHash(objectDigestValidationDetails.getDigestInDb());
                            objectTraceabilityHashDetails
                                .setOfferHashes(objectDigestValidationDetails.getDigestByOfferId());
                        }

                        objectHashes.add(
                            new ObjectGroupDocumentHash(objectId, objectDigestValidationDetails.getGlobalDigest(),
                                objectTraceabilityHashDetails));
                    }
                }

                lfcTraceSecFileDataLine.setObjectGroupDocumentHashList(objectHashes);


            } else {
                throw new IllegalStateException("Unknown lifecycleType " + lifecycleType);
            }


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
     * Generate a hash for a JsonNode using VITAM Digest Algorithm
     *
     * @param jsonNode the jsonNode to compute digest for
     * @param digestType the digest type
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
