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
package fr.gouv.vitam.metadata.core.reconstruction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;
import com.mongodb.MongoBulkWriteException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.WriteModel;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.api.VitamRepositoryProvider;
import fr.gouv.vitam.common.database.offset.OffsetRepository;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamFatalRuntimeException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.iterables.BulkIterator;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.storage.compress.VitamArchiveStreamFactory;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.database.collections.MetadataDocument;
import fr.gouv.vitam.metadata.core.database.collections.Unit;
import fr.gouv.vitam.metadata.core.model.ReconstructionRequestItem;
import fr.gouv.vitam.metadata.core.model.ReconstructionResponseItem;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.Order;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.exists;
import static com.mongodb.client.model.Filters.lte;
import static com.mongodb.client.model.Projections.include;
import static fr.gouv.vitam.common.database.server.mongodb.VitamDocument.ID;
import static fr.gouv.vitam.common.database.utils.MetadataDocumentHelper.getComputedGraphObjectGroupFields;
import static fr.gouv.vitam.common.database.utils.MetadataDocumentHelper.getComputedGraphUnitFields;

/**
 * Reconstruction of Vitam Metadata Collections.<br>
 */
public class ReconstructionService {

    /**
     * Vitam Logger.
     */
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ReconstructionService.class);
    private static final String RECONSTRUCTION_ITEM_MANDATORY_MSG = "the item defining reconstruction is mandatory.";
    private static final String RECONSTRUCTION_COLLECTION_MANDATORY_MSG = "the collection to reconstruct is mandatory.";
    private static final String RECONSTRUCTION_TENANT_MANDATORY_MSG = "the tenant to reconstruct is mandatory.";
    private static final String RECONSTRUCTION_LIMIT_POSITIVE_MSG = "the limit to reconstruct is should at least 0.";

    private static final String STRATEGY_ID = "default";
    private static final String $_SET = "$set";

    private RestoreBackupService restoreBackupService;

    private VitamRepositoryProvider vitamRepositoryProvider;

    private LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory;

    private OffsetRepository offsetRepository;

    /**
     * Constructor
     *
     * @param vitamRepositoryProvider vitamRepositoryProvider
     * @param offsetRepository offsetRepository
     */
    public ReconstructionService(VitamRepositoryProvider vitamRepositoryProvider,
        OffsetRepository offsetRepository) {
        this(vitamRepositoryProvider, new RestoreBackupService(), LogbookLifeCyclesClientFactory.getInstance(),
            offsetRepository);
    }

    /**
     * Constructor for tests
     *
     * @param vitamRepositoryProvider vitamRepositoryProvider
     * @param recoverBackupService recoverBackupService
     * @param logbookLifecycleClientFactory logbookLifecycleClientFactory
     * @param offsetRepository
     */
    @VisibleForTesting
    public ReconstructionService(VitamRepositoryProvider vitamRepositoryProvider,
        RestoreBackupService recoverBackupService, LogbookLifeCyclesClientFactory logbookLifecycleClientFactory,
        OffsetRepository offsetRepository) {
        this.vitamRepositoryProvider = vitamRepositoryProvider;
        this.restoreBackupService = recoverBackupService;
        this.logbookLifeCyclesClientFactory = logbookLifecycleClientFactory;
        this.offsetRepository = offsetRepository;
    }

    /**
     * Reconstruct a collection
     *
     * @param reconstructionItem request for reconstruction
     * @return response of reconstruction
     * @throws DatabaseException database exception
     * @throws IllegalArgumentException invalid input
     */
    public ReconstructionResponseItem reconstruct(ReconstructionRequestItem reconstructionItem) {
        ParametersChecker.checkParameter(RECONSTRUCTION_ITEM_MANDATORY_MSG, reconstructionItem);
        ParametersChecker.checkParameter(RECONSTRUCTION_COLLECTION_MANDATORY_MSG, reconstructionItem.getCollection());
        ParametersChecker.checkParameter(RECONSTRUCTION_TENANT_MANDATORY_MSG, reconstructionItem.getTenant());
        if (reconstructionItem.getLimit() < 0) {
            throw new IllegalArgumentException(RECONSTRUCTION_LIMIT_POSITIVE_MSG);
        }
        LOGGER
            .info(String.format(
                "[Reconstruction]: Reconstruction of {%s} Collection on {%s} Vitam tenant",
                reconstructionItem.getCollection(), reconstructionItem.getTenant()));

        switch (DataCategory.valueOf(reconstructionItem.getCollection().toUpperCase())) {
            case UNIT_GRAPH:
            case OBJECTGROUP_GRAPH:
                return reconstructGraphFromZipStream(reconstructionItem.getCollection(), reconstructionItem.getLimit());
            case UNIT:
            case OBJECTGROUP:
                return reconstructCollection(MetadataCollections.getFromValue(reconstructionItem.getCollection()),
                    reconstructionItem.getTenant(), reconstructionItem.getLimit());
            default:
                return new ReconstructionResponseItem(reconstructionItem, StatusCode.KO);
        }
    }

    private ReconstructionResponseItem reconstructGraphFromZipStream(String collectionName, int limit) {

        Integer tenant = VitamConfiguration.getAdminTenant();
        VitamThreadUtils.getVitamSession().setTenantId(tenant);


        final long offset = offsetRepository.findOffsetBy(tenant, collectionName);
        ParametersChecker.checkParameter("Parameter collection is required.", collectionName);
        LOGGER.info(String
            .format(
                "[Reconstruction]: Start reconstruction of the {%s} collection on the Vitam tenant {%s} for %s elements starting from {%s}.",
                collectionName, tenant, limit, offset));
        ReconstructionResponseItem response =
            new ReconstructionResponseItem().setCollection(collectionName).setTenant(tenant);
        MetadataCollections metaDaCollection;
        DataCategory dataCategory = DataCategory.valueOf(collectionName);
        switch (dataCategory) {
            case UNIT_GRAPH:
                metaDaCollection = MetadataCollections.UNIT;
                break;
            case OBJECTGROUP_GRAPH:
                metaDaCollection = MetadataCollections.OBJECTGROUP;
                break;
            default:
                throw new IllegalArgumentException(String.format("ERROR: Invalid collection {%s}", collectionName));
        }

        long newOffset = offset;


        try {
            // get the list of data to backup.
            Iterator<OfferLog> listing =
                restoreBackupService.getListing(STRATEGY_ID, dataCategory, offset, limit, Order.ASC, VitamConfiguration.getRestoreBulkSize());

            while (listing.hasNext()) {

                OfferLog offerLog = listing.next();
                String guid = GUIDFactory.newGUID().getId();

                Path filePath = Files.createTempFile(guid + "_", offerLog.getFileName());

                // Read zip file from offer
                try (InputStream zipFileAsStream =
                    restoreBackupService.loadData(STRATEGY_ID, dataCategory, offerLog.getFileName())) {

                    // Copy file to local tmp to prevent risk of broken stream
                    Files.copy(zipFileAsStream, filePath, StandardCopyOption.REPLACE_EXISTING);

                } catch (StorageNotFoundException ex) {
                    throw new ReconstructionException("Could not find graph zip file " + offerLog.getFileName(), ex);
                }

                // Handle a reconstruction from a copied zip file
                try (InputStream zipInputStream = Files.newInputStream(filePath)) {
                    reconstructGraphFromZipStream(metaDaCollection, zipInputStream);
                } finally {
                    // Remove file
                    Files.deleteIfExists(filePath);
                }
                newOffset = offerLog.getSequence();
                // log the reconstruction of Vitam collection.
                LOGGER.info(String.format(
                    "[Reconstruction]: the collection {%s} has been reconstructed on the tenant {%s} from {offset:%s} at %s",
                    collectionName, tenant, offset, LocalDateUtil.now()));

            }

            response.setStatus(StatusCode.OK);
        } catch (ReconstructionException | IOException de) {
            LOGGER.error(String.format(
                "[Reconstruction]: Exception has been thrown when reconstructing Vitam collection {%s} metadata on the tenant {%s} from {offset:%s}",
                collectionName, tenant, offset), de);
            newOffset = offset;
            response.setStatus(StatusCode.KO);

        } finally {
            offsetRepository.createOrUpdateOffset(tenant, collectionName, newOffset);
        }
        return response;
    }

    /**
     * Reconstruct collection.
     *
     * @param collection collection
     * @param tenant tenant
     * @param limit number of data to reconstruct
     * @return response of reconstruction
     * @throws IllegalArgumentException invalid input
     * @throws VitamRuntimeException storage error
     */
    private ReconstructionResponseItem reconstructCollection(MetadataCollections collection, int tenant, int limit) {

        final long offset = offsetRepository.findOffsetBy(tenant, collection.getName());
        ParametersChecker.checkParameter("Parameter collection is required.", collection);
        LOGGER.info(String
            .format(
                "[Reconstruction]: Start reconstruction of the {%s} collection on the Vitam tenant {%s} for %s elements starting from {%s}.",
                collection.name(), tenant, limit, offset));
        ReconstructionResponseItem response =
            new ReconstructionResponseItem().setCollection(collection.name()).setTenant(tenant);
        Integer originalTenant = VitamThreadUtils.getVitamSession().getTenantId();

        long newOffset = offset;

        try {
            // This is a hack, we must set manually the tenant is the VitamSession (used and transmitted in the
            // headers)
            VitamThreadUtils.getVitamSession().setTenantId(tenant);
            DataCategory type;
            switch (collection) {
                case UNIT:
                    type = DataCategory.UNIT;
                    break;
                case OBJECTGROUP:
                    type = DataCategory.OBJECTGROUP;
                    break;
                default:
                    throw new IllegalArgumentException(String.format("ERROR: Invalid collection {%s}", collection));
            }

            Iterator<OfferLog> listing = restoreBackupService.getListing(STRATEGY_ID, type, offset, limit, Order.ASC,
                    VitamConfiguration.getRestoreBulkSize());

            Iterator<List<OfferLog>> bulkListing = new BulkIterator<>(listing, VitamConfiguration.getRestoreBulkSize());

            while (bulkListing.hasNext()) {

                List<OfferLog> listingBulk = bulkListing.next();

                List<OfferLog> writtenMetadata = new ArrayList<>();
                List<String> deletedMetadataIds = new ArrayList<>();

                for (OfferLog offerLog : listingBulk) {

                    switch (offerLog.getAction()) {

                        case WRITE:
                            writtenMetadata.add(offerLog);
                            break;

                        case DELETE:
                            deletedMetadataIds.add(metadataFilenameToGuid(offerLog.getFileName()));
                            break;

                        default:
                            throw new UnsupportedOperationException(
                                "Unsupported offer log action " + offerLog.getAction());
                    }
                }

                processWrittenMetadata(collection, tenant, writtenMetadata);

                processDeletedMetadata(collection, deletedMetadataIds);

                newOffset = Iterables.getLast(listingBulk).getSequence();

                // log the reconstruction of Vitam collection.
                LOGGER.info(String.format(
                    "[Reconstruction]: the collection {%s} has been reconstructed on the tenant {%s} from {offset:%s} at %s",
                    collection.name(), tenant, offset, LocalDateUtil.now()));
            }

            offsetRepository.createOrUpdateOffset(tenant, collection.getName(), newOffset);

            response.setStatus(StatusCode.OK);

        } catch (LogbookClientException | InvalidParseOperationException | StorageException | DatabaseException e) {
            LOGGER.error(String.format(
                "[Reconstruction]: Exception has been thrown when reconstructing Vitam collection {%s} metadata & lifecycles on the tenant {%s} from {offset:%s}",
                collection, tenant, offset), e);
            response.setStatus(StatusCode.KO);
        } finally {
            VitamThreadUtils.getVitamSession().setTenantId(originalTenant);
        }
        return response;
    }

    /**
     * reconstruct Vitam collection from the backup data.
     */
    private void processWrittenMetadata(MetadataCollections collection, int tenant, List<OfferLog> writtenMetadata)
        throws StorageException, DatabaseException, LogbookClientException, InvalidParseOperationException {

        if (writtenMetadata.isEmpty()) {
            return;
        }

        for (int retry = VitamConfiguration.getOptimisticLockRetryNumber(); retry > 0; retry--) {

            List<MetadataBackupModel> dataFromOffer = loadMetadataSet(collection, tenant, writtenMetadata);

            if (dataFromOffer.isEmpty()) {
                // NOP
                return;
            }

            try {
                reconstructCollectionMetadata(collection, dataFromOffer);
                reconstructCollectionLifecycles(collection, dataFromOffer);

                // DONE
                return;

            } catch (DatabaseException e) {

                if (!(e.getCause() instanceof MongoBulkWriteException)) {
                    throw e;
                }

                LOGGER.warn("[Reconstruction]: [Optimistic_Lock]: optimistic lock occurs while reconstruct AU/GOT");

                try {
                    Thread.sleep(
                        ThreadLocalRandom.current().nextInt(VitamConfiguration.getOptimisticLockSleepTime()));
                } catch (InterruptedException e1) {
                    SysErrLogger.FAKE_LOGGER.ignoreLog(e1);
                    Thread.currentThread().interrupt();
                    throw new DatabaseException(e1);
                }
            }
        }

        throw new DatabaseException("Optimistic lock number of retry reached");
    }

    private List<MetadataBackupModel> loadMetadataSet(MetadataCollections collection, int tenant,
        List<OfferLog> writtenMetadata) throws StorageException {

        // FIXME : parallel processing
        List<MetadataBackupModel> dataFromOffer = new ArrayList<>();
        for (OfferLog offerLog : writtenMetadata) {

            try {
                MetadataBackupModel model = restoreBackupService
                    .loadData(STRATEGY_ID, collection, offerLog.getFileName(), offerLog.getSequence());

                if (model.getMetadatas() == null || model.getLifecycle() == null || model.getOffset() == null) {
                    throw new StorageException(String.format(
                        "[Reconstruction]: Invalid data to reconstruct in file {%s} for the collection {%s} on the tenant {%s}",
                        offerLog.getFileName(), collection, tenant));
                }

                dataFromOffer.add(model);

            } catch (StorageNotFoundException ex) {
                // 2 possibilities :
                // - File have never been written to offer (atomic commit bug in offer. Should be fixed in dedicated bug)
                // - File have been deleted meanwhile (it's ok to skip)
                LOGGER.warn(String.format(
                    "[Reconstruction]: Could not find file {%s} for the collection {%s} on the tenant {%s}. Corrupted file (atomicity bug) OR eliminated? ",
                    offerLog.getFileName(), collection, tenant));
            }
        }
        return dataFromOffer;
    }

    private void processDeletedMetadata(MetadataCollections collection, List<String> deletedMetadataIds)
        throws DatabaseException, LogbookClientBadRequestException, LogbookClientServerException {
        if (deletedMetadataIds.isEmpty()) {
            return;
        }
        reconstructDeletedMetadata(collection, deletedMetadataIds);
        reconstructDeletedLifecycles(collection, deletedMetadataIds);
    }

    private String metadataFilenameToGuid(String fileName) {
        // File name should in the format guid.json
        return fileName.substring(0, fileName.lastIndexOf('.'));
    }

    private void reconstructDeletedMetadata(MetadataCollections collection, List<String> ids)
        throws DatabaseException {

        LOGGER.info("[Reconstruction]: delete metadata bulk");

        int tenant = VitamThreadUtils.getVitamSession().getTenantId();

        this.vitamRepositoryProvider.getVitamMongoRepository(collection.getVitamCollection())
            .delete(ids, tenant);
        this.vitamRepositoryProvider.getVitamESRepository(collection.getVitamCollection())
            .delete(ids, tenant);
    }

    private void reconstructDeletedLifecycles(MetadataCollections collection, List<String> ids)
        throws LogbookClientBadRequestException, LogbookClientServerException {

        LOGGER.info("[Reconstruction]: delete lifecycle bulk");

        try (LogbookLifeCyclesClient logbookLifecycleClient = logbookLifeCyclesClientFactory.getClient()) {
            switch (collection) {
                case UNIT:
                    logbookLifecycleClient.deleteLifecycleUnitsBulk(ids);
                    break;
                case OBJECTGROUP:
                    logbookLifecycleClient.deleteLifecycleObjectGroupBulk(ids);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid collection");
            }
        }
    }

    /**
     * If an already existing document, have a graph data. Do not erase graph data
     *
     * @param collection
     * @param dataFromOffer
     */
    private void preventAlreadyExistingGraphData(MetadataCollections collection,
        List<MetadataBackupModel> dataFromOffer) {

        List<MetadataBackupModel> toRemove = new ArrayList<>();
        Map<String, MetadataBackupModel> dataMap = new HashMap<>();

        for (MetadataBackupModel mbm : dataFromOffer) {
            Document document = mbm.getMetadatas();
            String id = document.getString(ID);
            MetadataBackupModel alreadyExists = dataMap.remove(id);
            if (null != alreadyExists) {
                toRemove.add(alreadyExists);
            }

            dataMap.put(id, mbm);
        }

        // Remove duplicate document and keep only the latest one (as they have the latest version in the storage)
        dataFromOffer.removeAll(toRemove);


        final Bson projection;

        switch (collection) {
            case UNIT:
                projection = include(getComputedGraphUnitFields());
                break;
            case OBJECTGROUP:
                projection = include(getComputedGraphObjectGroupFields());
                break;
            default:
                throw new IllegalStateException("Unsupported metadata type " + collection);
        }


        try (MongoCursor<Document> iterator = this.vitamRepositoryProvider
            .getVitamMongoRepository(collection.getVitamCollection())
            .findDocuments(dataMap.keySet(), projection)
            .iterator()) {

            while (iterator.hasNext()) {

                // SourceDocument document from mongo contains only graph data and _id
                // TargetDocument document from offer (do no contains graph data

                Document sourceDocument = iterator.next();
                String id = sourceDocument.getString(ID);
                Document targetDocument = dataMap.get(id).getMetadatas();

                targetDocument.putAll(sourceDocument);
            }
        }

    }

    /**
     * Reconstruct lifecycles in logbook
     *
     * @param collection collection
     * @param bulk list of items to back up
     * @throws LogbookClientException error from logbook
     * @throws InvalidParseOperationException error parsing logbook response
     */
    private void reconstructCollectionLifecycles(MetadataCollections collection, List<MetadataBackupModel> bulk)
        throws LogbookClientException, InvalidParseOperationException {

        LOGGER.info("[Reconstruction]: Back up of lifecycles bulk");

        try (LogbookLifeCyclesClient logbookLifecycleClient = logbookLifeCyclesClientFactory.getClient()) {
            List<JsonNode> lifecycles =
                bulk.stream()
                    .map(model -> {
                        try {
                            if (model.getLifecycle() != null) {
                                return JsonHandler
                                    .getFromString(
                                        model.getLifecycle().toJson(new JsonWriterSettings(JsonMode.STRICT)));
                            } else {
                                throw new VitamRuntimeException("lifecycle should not be null");
                            }
                        } catch (InvalidParseOperationException e) {
                            throw new VitamRuntimeException(e);
                        }
                    })
                    .collect(Collectors.toList());
            switch (collection) {
                case UNIT:
                    logbookLifecycleClient.createRawbulkUnitlifecycles(lifecycles);
                    break;
                case OBJECTGROUP:
                    logbookLifecycleClient.createRawbulkObjectgrouplifecycles(lifecycles);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid collection");
            }

        } catch (VitamRuntimeException lifecycleParsingException) {
            throw new InvalidParseOperationException(lifecycleParsingException);
        }
    }

    /**
     * Reconstruct metadata in databases
     *
     * @param collection the concerning collection
     * @param dataFromOffer list of items to back up
     * @throws DatabaseException databaseException
     */
    private void reconstructCollectionMetadata(MetadataCollections collection, List<MetadataBackupModel> dataFromOffer)
        throws DatabaseException {
        LOGGER.info("[Reconstruction]: Back up of metadata bulk");

        // Do not erase graph data
        preventAlreadyExistingGraphData(collection, dataFromOffer);

        // Create bulk of ReplaceOneModel
        List<WriteModel<Document>> metadata =
            dataFromOffer.stream().map(MetadataBackupModel::getMetadatas).map(this::createReplaceOneModel)
                .collect(Collectors.toList());

        this.bulkMongo(collection, metadata);

        List<Document> documents =
            dataFromOffer.stream().map(MetadataBackupModel::getMetadatas).collect(Collectors.toList());
        bulkElasticsearch(collection, documents);
    }

    /**
     * @param metaDaCollection
     * @param zipStream The zip inputStream
     * @throws DatabaseException
     */
    private void reconstructGraphFromZipStream(MetadataCollections metaDaCollection, InputStream zipStream)
        throws ReconstructionException {
        LOGGER.info("[Reconstruction]: Back up of metadata bulk");

        try (final ArchiveInputStream archiveInputStream = new VitamArchiveStreamFactory()
            .createArchiveInputStream(CommonMediaType.valueOf(CommonMediaType.ZIP), zipStream)) {
            ArchiveEntry entry;
            while ((entry = archiveInputStream.getNextEntry()) != null) {
                if (archiveInputStream.canReadEntryData(entry)) {
                    if (!entry.isDirectory()) {
                        ArrayNode arrayNode = (ArrayNode) JsonHandler.getFromInputStream(archiveInputStream);
                        treatBulkGraph(metaDaCollection, arrayNode);
                    }
                }
            }
        } catch (InvalidParseOperationException | IOException | ArchiveException | DatabaseException e) {
            throw new ReconstructionException(e);
        } finally {
            removeGraphOnlyReconstructedOlderDocuments(metaDaCollection);
        }

    }


    /**
     * Find all older (AU/GOT) where only graph data are reconstructed
     * As Documents with only graph data are not indexed in elasticsearch=> we have not to implement deletion from Elastcisearch
     */
    private void removeGraphOnlyReconstructedOlderDocuments(MetadataCollections metaDaCollection) {

        try {

            String dateDeleteLimit = LocalDateUtil.getFormattedDateForMongo(
                LocalDateTime
                    .now()
                    .minus(VitamConfiguration.getDeleteIncompleteReconstructedUnitDelay(), ChronoUnit.SECONDS));
            Bson query = and(
                exists(Unit.TENANT_ID, false),
                lte(Unit.GRAPH_LAST_PERSISTED_DATE, dateDeleteLimit));

            this.vitamRepositoryProvider.getVitamMongoRepository(metaDaCollection.getVitamCollection()).remove(query);
        } catch (DatabaseException e) {
            LOGGER.error("[Reconstruction]: Error while remove older documents having only graph data", e);
        }
    }

    /**
     * @param metadataCollections
     * @param arrayNode
     * @throws DatabaseException
     */
    private void treatBulkGraph(MetadataCollections metadataCollections, ArrayNode arrayNode) throws DatabaseException {
        List<WriteModel<Document>> collection = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        arrayNode.forEach(o -> {
            //Create UpdateOneModel
            try {
                collection.add(createUpdateOneModel(o));
                /**
                 * Take only documents having graph data and business data to be indexed in elasticsearch
                 * Skip all documents with only graph data
                 */
                if (null != o.get(Unit.TENANT_ID)) {
                    ids.add(o.get(Unit.ID).asText());
                }
            } catch (InvalidParseOperationException e) {
                throw new VitamFatalRuntimeException(e);
            }
        });

        // Save in MongoDB
        bulkMongo(metadataCollections, collection);

        // Save in Elasticsearch
        bulkElasticsearch(metadataCollections, ids);
    }

    /**
     * Bulk write in mongodb
     *
     * @param metaDaCollection
     * @param collection
     * @throws DatabaseException
     */
    private void bulkMongo(MetadataCollections metaDaCollection, List<WriteModel<Document>> collection)
        throws DatabaseException {
        this.vitamRepositoryProvider.getVitamMongoRepository(metaDaCollection.getVitamCollection()).update(collection);
    }


    /**
     * Bulk save in elasticsearch
     *
     * @param metaDaCollection
     * @param collection of id of documents
     * @throws DatabaseException
     */
    private void bulkElasticsearch(MetadataCollections metaDaCollection, Set<String> collection)
        throws DatabaseException {

        if (collection.isEmpty()) {
            return;
        }

        FindIterable<Document> fit =
            this.vitamRepositoryProvider.getVitamMongoRepository(metaDaCollection.getVitamCollection())
                .findDocuments(collection, null);
        MongoCursor<Document> it = fit.iterator();
        List<Document> documents = new ArrayList<>();
        while (it.hasNext()) {
            documents.add(it.next());
        }

        bulkElasticsearch(metaDaCollection, documents);
    }

    /**
     * Bulk save in elasticsearch
     *
     * @param metaDaCollection
     * @param collection of documents
     * @throws DatabaseException
     */
    private void bulkElasticsearch(MetadataCollections metaDaCollection, List<Document> collection)
        throws DatabaseException {
        this.vitamRepositoryProvider.getVitamESRepository(metaDaCollection.getVitamCollection()).save(collection);
    }

    /**
     * @param graphData
     * @return
     * @throws InvalidParseOperationException
     */
    private UpdateOneModel<Document> createUpdateOneModel(JsonNode graphData) throws InvalidParseOperationException {
        JsonNode id = ((ObjectNode) graphData).remove(Unit.ID);
        final Document data = new Document($_SET, Document.parse(JsonHandler.writeAsString(graphData)));
        return new UpdateOneModel<>(eq(Unit.ID, id.asText()), data, new UpdateOptions().upsert(true));
    }


    /**
     * @param document
     * @return
     * @throws InvalidParseOperationException
     */
    private WriteModel<Document> createReplaceOneModel(Document document) {
        final Object glpd = document.get(MetadataDocument.GRAPH_LAST_PERSISTED_DATE);

        Bson filter;
        if (null == glpd) {
            // Document not yet in mongodb or in mongodb with but without graph data
            filter = and(
                eq(ID, document.get(ID)),
                exists(MetadataDocument.GRAPH_LAST_PERSISTED_DATE, false)
            );
        } else {
            // Document already exists in mongodb and already have graph data
            filter = and(
                eq(ID, document.get(ID)),
                eq(MetadataDocument.GRAPH_LAST_PERSISTED_DATE, glpd.toString())
            );
        }

        // No need for "_av" (ATOMIC_VERSION) update in secondary site
        return new ReplaceOneModel<>(filter, document, new UpdateOptions().upsert(true));
    }
}
