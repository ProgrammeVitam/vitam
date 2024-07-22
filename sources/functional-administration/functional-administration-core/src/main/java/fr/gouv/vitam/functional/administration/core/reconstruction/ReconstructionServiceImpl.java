/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
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
package fr.gouv.vitam.functional.administration.core.reconstruction;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoBulkWriteException;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.WriteModel;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.api.VitamRepositoryProvider;
import fr.gouv.vitam.common.database.api.impl.VitamElasticsearchRepository;
import fr.gouv.vitam.common.database.api.impl.VitamMongoRepository;
import fr.gouv.vitam.common.database.offset.OffsetRepository;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.metrics.VitamCommonMetrics;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterBackupModel;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterDetail;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterSummary;
import fr.gouv.vitam.functional.administration.common.CollectionBackupModel;
import fr.gouv.vitam.functional.administration.common.ReconstructionRequestItem;
import fr.gouv.vitam.functional.administration.common.ReconstructionResponseItem;
import fr.gouv.vitam.functional.administration.common.VitamSequence;
import fr.gouv.vitam.functional.administration.common.config.ElasticsearchFunctionalAdminIndexManager;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.core.backup.RestoreBackupService;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.Order;
import io.prometheus.client.Histogram;
import org.apache.shiro.util.CollectionUtils;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;
import static fr.gouv.vitam.common.database.server.mongodb.VitamDocument.ID;
import static fr.gouv.vitam.common.database.server.mongodb.VitamDocument.TENANT_ID;
import static fr.gouv.vitam.common.database.server.mongodb.VitamDocument.VERSION;

/**
 * Reconstrution of Vitam Collections.<br>
 */
public class ReconstructionServiceImpl implements ReconstructionService {

    /**
     * Vitam Logger.
     */
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ReconstructionServiceImpl.class);

    private static final int ADMIN_TENANT = VitamConfiguration.getAdminTenant();

    private final RestoreBackupService recoverBackupService;

    private final VitamRepositoryProvider vitamRepositoryProvider;

    private static final String RECONSTRUCTION_ITEM_MANDATORY_MSG = "the item defining reconstruction is mandatory.";
    private static final String RECONSTRUCTION_COLLECTION_MANDATORY_MSG = "the collection to reconstruct is mandatory.";
    private static final String RECONSTRUCTION_TENANT_MANDATORY_MSG = "the tenant to reconstruct is mandatory.";
    private static final String RECONSTRUCTION_LIMIT_POSITIVE_MSG = "the limit to reconstruct is should at least 0.";

    private static final String TOTAL_OBJECT_GROUPS_INGESTED = "TotalObjectGroups_ingested";
    private static final String TOTAL_OBJECT_GROUPS_DELETED = "TotalObjectGroups_deleted";
    private static final String TOTAL_OBJECT_GROUPS_REMAINED = "TotalObjectGroups_remained";
    private static final String TOTAL_UNITS_INGESTED = "TotalUnits_ingested";
    private static final String TOTAL_UNITS_DELETED = "TotalUnits_deleted";
    private static final String TOTAL_UNITS_REMAINED = "TotalUnits_remained";
    private static final String TOTAL_OBJECT_INGEST = "TotalObjects_ingested";
    private static final String TOTAL_OBJECT_DELETED = "TotalObjects_deleted";
    private static final String TOTAL_OBJECT_REMAINED = "TotalObjects_remained";
    private static final String OBJECT_SIZE_INGESTED = "ObjectSize_ingested";
    private static final String OBJECT_SIZE_DELETED = "ObjectSize_deleted";
    private static final String OBJECT_SIZE_REMAINED = "ObjectSize_remained";

    private final OffsetRepository offsetRepository;
    private final ElasticsearchFunctionalAdminIndexManager indexManager;
    private final FunctionalAdministrationReconstructionMetricsCache reconstructionMetricsCache;

    public ReconstructionServiceImpl(
        VitamRepositoryProvider vitamRepositoryProvider,
        OffsetRepository offsetRepository,
        ElasticsearchFunctionalAdminIndexManager indexManager,
        FunctionalAdministrationReconstructionMetricsCache reconstructionMetricsCache
    ) {
        this(
            vitamRepositoryProvider,
            new RestoreBackupServiceImpl(),
            offsetRepository,
            indexManager,
            reconstructionMetricsCache
        );
    }

    @VisibleForTesting
    public ReconstructionServiceImpl(
        VitamRepositoryProvider vitamRepositoryProvider,
        RestoreBackupService recoverBackupService,
        OffsetRepository offsetRepository,
        ElasticsearchFunctionalAdminIndexManager indexManager,
        FunctionalAdministrationReconstructionMetricsCache reconstructionMetricsCache
    ) {
        this.vitamRepositoryProvider = vitamRepositoryProvider;
        this.recoverBackupService = recoverBackupService;
        this.offsetRepository = offsetRepository;
        this.indexManager = indexManager;
        this.reconstructionMetricsCache = reconstructionMetricsCache;
    }

    /**
     * purge collection content and reconstruct the content.
     *
     * @param collection the collection to reconstruct.
     * @param tenants the given tenant.
     */
    @Override
    public void reconstruct(FunctionalAdminCollections collection, Integer... tenants) throws DatabaseException {
        ParametersChecker.checkParameter("All parameters [%s, %s] are required.", collection, tenants);
        LOGGER.debug(
            String.format(
                "Start reconstruction of the %s collection on the Vitam tenant %s.",
                collection.getName(),
                Arrays.toString(tenants)
            )
        );

        Integer originalTenant = VitamThreadUtils.getVitamSession().getTenantId();

        final VitamMongoRepository mongoRepository = vitamRepositoryProvider.getVitamMongoRepository(
            collection.getVitamCollection()
        );
        final VitamElasticsearchRepository elasticsearchRepository = vitamRepositoryProvider.getVitamESRepository(
            collection.getVitamCollection(),
            indexManager.getElasticsearchIndexAliasResolver(collection)
        );

        final VitamMongoRepository sequenceRepository = vitamRepositoryProvider.getVitamMongoRepository(
            FunctionalAdminCollections.VITAM_SEQUENCE.getVitamCollection()
        );

        switch (collection) {
            case RULES:
            case INGEST_CONTRACT:
            case ACCESS_CONTRACT:
            case MANAGEMENT_CONTRACT:
            case PROFILE:
            case ARCHIVE_UNIT_PROFILE:
            case AGENCIES:
            case GRIFFIN:
            case PRESERVATION_SCENARIO:
                // Keep reconstruction process by specified Tenant
                break;
            case CONTEXT:
            case FORMATS:
            case SECURITY_PROFILE:
            case ONTOLOGY:
                // Admin tenant ONLY
                tenants = new Integer[] { ADMIN_TENANT };
                break;
            case ACCESSION_REGISTER_SUMMARY:
            case ACCESSION_REGISTER_DETAIL:
            case ACCESSION_REGISTER_SYMBOLIC:
            case VITAM_SEQUENCE:
            default:
                throw new IllegalArgumentException("Collection not supported in this service!");
        }
        try {
            for (Integer tenant : tenants) {
                reconstructByTenant(
                    collection,
                    mongoRepository,
                    elasticsearchRepository,
                    sequenceRepository,
                    tenant,
                    tenants
                );
            }
        } finally {
            VitamThreadUtils.getVitamSession().setTenantId(originalTenant);
        }
    }

    private void reconstructByTenant(
        FunctionalAdminCollections collection,
        VitamMongoRepository mongoRepository,
        VitamElasticsearchRepository elasticsearchRepository,
        VitamMongoRepository sequenceRepository,
        Integer tenant,
        Integer[] tenants
    ) throws DatabaseException {
        final Histogram.Timer timer = VitamCommonMetrics.RECONSTRUCTION_DURATION.labels(
            String.valueOf(tenant),
            collection.name()
        ).startTimer();
        try (final StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            // This is a hak, we must set manually the tenant in the VitamSession (used and transmitted in the headers)
            VitamThreadUtils.getVitamSession().setTenantId(tenant);

            // get the last version of the backup copies.
            String referentOfferForStrategy = storageClient.getReferentOffer(VitamConfiguration.getDefaultStrategy());
            Optional<CollectionBackupModel> collectionBackup = recoverBackupService.readLatestSavedFile(
                VitamConfiguration.getDefaultStrategy(),
                referentOfferForStrategy,
                collection
            );

            // reconstruct Vitam collection from the backup copy.
            if (collectionBackup.isPresent()) {
                LOGGER.debug(String.format("Last backup copy version : %s", collectionBackup));

                // purge collection content
                if (collection.isMultitenant()) {
                    mongoRepository.purge(tenant);
                    elasticsearchRepository.purge(tenant);
                } else {
                    mongoRepository.purge();
                    elasticsearchRepository.purge();
                }

                // saving the sequence & backup sequence in mongoDB
                restoreSequence(sequenceRepository, collectionBackup.get().getSequence());
                restoreSequence(sequenceRepository, collectionBackup.get().getBackupSequence());

                // saving the backup collection in mongoDB and elasticSearch.
                if (!CollectionUtils.isEmpty(collectionBackup.get().getDocuments())) {
                    mongoRepository.save(collectionBackup.get().getDocuments());
                    elasticsearchRepository.save(collectionBackup.get().getDocuments());
                }
                // log the reconstruction of Vitam collection.
                LOGGER.debug(
                    String.format(
                        "[Reconstruction]: the collection {%s} has been reconstructed on the tenants {%s} at %s",
                        collectionBackup,
                        Arrays.toString(tenants),
                        LocalDateUtil.now()
                    )
                );
            }
        } catch (StorageNotFoundClientException | StorageServerClientException e) {
            LOGGER.error("ERROR: Exception has been thrown when trying to get Referent offer :", e);
        } finally {
            timer.observeDuration();
        }
    }

    /**
     * Reconstruct a collection
     *
     * @param reconstructionItem request for reconstruction
     * @return response of reconstruction
     * @throws IllegalArgumentException invalid input
     */
    public ReconstructionResponseItem reconstructAccessionRegister(ReconstructionRequestItem reconstructionItem) {
        ParametersChecker.checkParameter(RECONSTRUCTION_ITEM_MANDATORY_MSG, reconstructionItem);
        ParametersChecker.checkParameter(RECONSTRUCTION_COLLECTION_MANDATORY_MSG, reconstructionItem.getCollection());
        ParametersChecker.checkParameter(RECONSTRUCTION_TENANT_MANDATORY_MSG, reconstructionItem.getTenant());
        if (reconstructionItem.getLimit() < 0) {
            throw new IllegalArgumentException(RECONSTRUCTION_LIMIT_POSITIVE_MSG);
        }
        LOGGER.info(
            String.format(
                "[Reconstruction]: Reconstruction of {%s} Collection on {%s} Vitam tenant",
                reconstructionItem.getCollection(),
                reconstructionItem.getTenant()
            )
        );

        FunctionalAdminCollections collection = getCollection(reconstructionItem);

        final Histogram.Timer timer = VitamCommonMetrics.RECONSTRUCTION_DURATION.labels(
            String.valueOf(reconstructionItem.getTenant()),
            collection.name()
        ).startTimer();
        try {
            return reconstructAccessionRegister(
                collection,
                reconstructionItem.getTenant(),
                reconstructionItem.getLimit()
            );
        } finally {
            timer.observeDuration();
        }
    }

    private FunctionalAdminCollections getCollection(ReconstructionRequestItem reconstructionItem) {
        if (
            FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getName()
                .equalsIgnoreCase(reconstructionItem.getCollection())
        ) {
            return FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL;
        } else if (
            FunctionalAdminCollections.ACCESSION_REGISTER_SYMBOLIC.getName()
                .equalsIgnoreCase(reconstructionItem.getCollection())
        ) {
            return FunctionalAdminCollections.ACCESSION_REGISTER_SYMBOLIC;
        } else {
            throw new IllegalArgumentException(
                String.format("ERROR: Invalid collection {%s}", reconstructionItem.getCollection())
            );
        }
    }

    /**
     * Reconstruct collection.
     *
     * @param collection collection
     * @param tenant tenant
     * @param limit number of data to reconstruct
     * @return response of reconstruction
     */
    private ReconstructionResponseItem reconstructAccessionRegister(
        FunctionalAdminCollections collection,
        int tenant,
        int limit
    ) {
        final long offset = offsetRepository.findOffsetBy(
            tenant,
            VitamConfiguration.getDefaultStrategy(),
            collection.getName()
        );
        ParametersChecker.checkParameter("Parameter collection is required.", collection);
        LOGGER.info(
            String.format(
                "[Reconstruction]: Start reconstruction of the {%s} collection on the Vitam tenant {%s} for %s elements starting from {%s}.",
                collection.name(),
                tenant,
                limit,
                offset + 1
            )
        );
        ReconstructionResponseItem response = new ReconstructionResponseItem()
            .setCollection(collection.name())
            .setTenant(tenant);
        Integer originalTenant = VitamThreadUtils.getVitamSession().getTenantId();

        long newOffset = offset;

        try {
            // This is a hack, we must set manually the tenant is the VitamSession (used and transmitted in the
            // headers)
            VitamThreadUtils.getVitamSession().setTenantId(tenant);
            DataCategory type;
            switch (collection) {
                case ACCESSION_REGISTER_DETAIL:
                    type = DataCategory.ACCESSION_REGISTER_DETAIL;
                    break;
                case ACCESSION_REGISTER_SYMBOLIC:
                    type = DataCategory.ACCESSION_REGISTER_SYMBOLIC;
                    break;
                default:
                    throw new IllegalArgumentException(String.format("ERROR: Invalid collection {%s}", collection));
            }

            LocalDateTime reconstructionStartDateTime = LocalDateUtil.now();
            LocalDateTime lastReconstructedDocumentDate = null;
            int nbEntriesReconstructed = 0;

            // get the list of data to backup.
            Iterator<List<OfferLog>> offerLogIterator = recoverBackupService.getListing(
                VitamConfiguration.getDefaultStrategy(),
                type,
                offset + 1L,
                limit,
                Order.ASC
            );

            Set<String> originatingAgencies = new HashSet<>();

            while (offerLogIterator.hasNext()) {
                List<OfferLog> listingBulk = offerLogIterator.next();

                List<AccessionRegisterBackupModel> dataFromOffer = new ArrayList<>();
                for (OfferLog offerLog : listingBulk) {
                    AccessionRegisterBackupModel model = recoverBackupService.loadData(
                        VitamConfiguration.getDefaultStrategy(),
                        collection,
                        offerLog.getFileName(),
                        offerLog.getSequence()
                    );
                    if (model.getAccessionRegister() != null && model.getOffset() != null) {
                        originatingAgencies.add((model.getAccessionRegister().getString("OriginatingAgency")));
                        dataFromOffer.add(model);
                    } else {
                        throw new StorageException(
                            String.format(
                                "[Reconstruction]: Data is not present in file {%s} for the collection {%s} on the tenant {%s}",
                                offerLog.getFileName(),
                                collection,
                                tenant
                            )
                        );
                    }
                }

                // reconstruct Vitam collection from the backup datas.
                if (dataFromOffer.isEmpty()) {
                    continue;
                }

                reconstructCollectionAccessionRegister(
                    collection,
                    dataFromOffer,
                    VitamConfiguration.getOptimisticLockRetryNumber()
                );
                AccessionRegisterBackupModel last = Iterables.getLast(dataFromOffer);
                newOffset = last.getOffset();

                nbEntriesReconstructed += listingBulk.size();
                lastReconstructedDocumentDate = listingBulk.get(listingBulk.size() - 1).getTime();

                // log the reconstruction of Vitam collection.
                LOGGER.info(
                    String.format(
                        "[Reconstruction]: the collection {%s} has been reconstructed on the tenant {%s} from {offset:%s} at %s",
                        collection.name(),
                        tenant,
                        offset,
                        LocalDateUtil.now()
                    )
                );
            }

            if (collection.equals(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL)) {
                computeAccessionRegisterSummary(originatingAgencies, tenant);
            }

            // Report reconstruction stats
            if (nbEntriesReconstructed != limit) {
                // Limit has not been reached ==> there was no more data to reconstruct at the time we started reconstruction
                lastReconstructedDocumentDate = LocalDateUtil.max(
                    reconstructionStartDateTime,
                    lastReconstructedDocumentDate
                );
            }
            this.reconstructionMetricsCache.registerLastDocumentReconstructionDate(
                    collection,
                    tenant,
                    lastReconstructedDocumentDate
                );

            response.setStatus(StatusCode.OK);
        } catch (DatabaseException de) {
            LOGGER.error(
                String.format(
                    "[Reconstruction]: Exception has been thrown when reconstructing Vitam collection {%s} on the tenant {%s} from {offset:%s}",
                    collection,
                    tenant,
                    offset
                ),
                de
            );
            newOffset = offset;
            response.setStatus(StatusCode.KO);
        } catch (StorageException | StorageServerClientException | StorageNotFoundClientException se) {
            LOGGER.error(se.getMessage());
            newOffset = offset;
            response.setStatus(StatusCode.KO);
        } finally {
            if (newOffset != offset) {
                offsetRepository.createOrUpdateOffset(
                    tenant,
                    VitamConfiguration.getDefaultStrategy(),
                    collection.getName(),
                    newOffset
                );
            } else {
                LOGGER.info(
                    String.format(
                        "[Reconstruction]: Nothing to reconstruct for Vitam collection {%s} on the tenant {%s} from {offset:%s}",
                        collection,
                        tenant,
                        offset
                    )
                );
            }
            VitamThreadUtils.getVitamSession().setTenantId(originalTenant);
        }
        return response;
    }

    /**
     * Reconstruct metadatas in databases
     *
     * @param collection the concerning collection
     * @param dataFromOffer list of items to back up
     * @throws DatabaseException databaseException
     */
    private void reconstructCollectionAccessionRegister(
        FunctionalAdminCollections collection,
        List<AccessionRegisterBackupModel> dataFromOffer,
        Integer nbRetry
    ) throws DatabaseException {
        if (nbRetry < 0) {
            throw new DatabaseException("Optimistic lock number of retry reached");
        }
        LOGGER.info("[Reconstruction]: Back up of Accession Register bulk");

        // Create bulk of ReplaceOneModel
        List<WriteModel<Document>> metadataList = dataFromOffer
            .stream()
            .map(AccessionRegisterBackupModel::getAccessionRegister)
            .map(this::createReplaceOneModel)
            .collect(Collectors.toList());
        try {
            this.bulkMongo(collection, metadataList);
        } catch (DatabaseException e) {
            if (e.getCause() instanceof MongoBulkWriteException) {
                LOGGER.warn(
                    "[Reconstruction]: [Optimistic_Lock]: optimistic lock occurs while reconstruct Accession Register"
                );

                try {
                    Thread.sleep(ThreadLocalRandom.current().nextInt(VitamConfiguration.getOptimisticLockSleepTime()));
                } catch (InterruptedException e1) {
                    SysErrLogger.FAKE_LOGGER.ignoreLog(e1);
                    throw new DatabaseException(e1);
                }

                nbRetry--; // Retry after optimistic lock problem
                reconstructCollectionAccessionRegister(collection, dataFromOffer, nbRetry);
                return;
            } else {
                throw e;
            }
        }

        List<Document> documents = dataFromOffer
            .stream()
            .map(AccessionRegisterBackupModel::getAccessionRegister)
            .collect(Collectors.toList());
        bulkElasticsearch(collection, documents);
    }

    private void restoreSequence(VitamMongoRepository sequenceRepository, VitamSequence sequenceCollection)
        throws DatabaseException {
        sequenceRepository.removeByNameAndTenant(sequenceCollection.getName(), sequenceCollection.getTenantId());
        sequenceRepository.save(sequenceCollection);
    }

    @Override
    public void reconstruct(FunctionalAdminCollections collection) throws DatabaseException {
        ParametersChecker.checkParameter("The collection parameter is required.", collection);
        LOGGER.debug(
            String.format(
                "Start reconstruction of the %s collection on all of the Vitam tenants.",
                collection.getName()
            )
        );

        // get the list of vitam tenants from the configuration.
        List<Integer> tenants = VitamConfiguration.getTenants();

        // reconstruct all the Vitam tenants from the backup copy.
        if (null != tenants && !tenants.isEmpty()) {
            LOGGER.debug(String.format("Reconstruction of %s Vitam tenants", tenants.size()));
            // reconstruction of the list of tenants
            reconstruct(collection, tenants.toArray(new Integer[0]));
        }
    }

    /**
     * @param document
     * @return
     */
    private WriteModel<Document> createReplaceOneModel(Document document) {
        Bson filter = eq(ID, document.get(ID));
        return new ReplaceOneModel<>(filter, document, new ReplaceOptions().upsert(true));
    }

    /**
     * Bulk write in mongodb
     *
     * @param faCollection
     * @param collection
     * @throws DatabaseException
     */
    private void bulkMongo(FunctionalAdminCollections faCollection, List<WriteModel<Document>> collection)
        throws DatabaseException {
        this.vitamRepositoryProvider.getVitamMongoRepository(faCollection.getVitamCollection()).update(collection);
    }

    /**
     * Bulk save in elasticsearch
     *
     * @param faCollection
     * @param collection of documents
     * @throws DatabaseException
     */
    private void bulkElasticsearch(FunctionalAdminCollections faCollection, List<Document> collection)
        throws DatabaseException {
        this.vitamRepositoryProvider.getVitamESRepository(
                faCollection.getVitamCollection(),
                indexManager.getElasticsearchIndexAliasResolver(faCollection)
            ).save(collection);
    }

    public void computeAccessionRegisterSummary(Set<String> originatingAgencies, Integer tenant) {
        ParametersChecker.checkParameter("All params are required", originatingAgencies, tenant);

        if (originatingAgencies.isEmpty()) {
            return;
        }

        int originalTenant = VitamThreadUtils.getVitamSession().getTenantId();

        try {
            VitamThreadUtils.getVitamSession().setTenantId(tenant);
            List<Document> documents = agregateAccessionRegisterSummary(originatingAgencies, tenant);
            Set<Document> accessionRegisterSummary = new HashSet<>();
            for (Document registerSummaryDoc : documents) {
                registerSummaryDoc
                    .append(TENANT_ID, tenant)
                    .append(VERSION, 0)
                    .append(AccessionRegisterSummary.CREATION_DATE, LocalDateUtil.nowFormatted());

                BasicDBObject searchQuery = new BasicDBObject();
                searchQuery.put(
                    AccessionRegisterSummary.ORIGINATING_AGENCY,
                    registerSummaryDoc.getString(AccessionRegisterSummary.ORIGINATING_AGENCY)
                );
                searchQuery.put(TENANT_ID, tenant);
                try (
                    MongoCursor<AccessionRegisterSummary> registerSummaryIt =
                        (FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.<AccessionRegisterSummary>getCollection()
                                .find(searchQuery)).iterator()
                ) {
                    if (registerSummaryIt.hasNext()) {
                        registerSummaryDoc.append(ID, registerSummaryIt.next().get(ID));
                    } else {
                        registerSummaryDoc.append(
                            ID,
                            GUIDFactory.newAccessionRegisterSummaryGUID(ParameterHelper.getTenantParameter()).getId()
                        );
                    }
                }
                accessionRegisterSummary.add(registerSummaryDoc);
            }

            // Create bulk of ReplaceOneModel
            List<WriteModel<Document>> collectionWM = accessionRegisterSummary
                .stream()
                .map(this::createReplaceOneModel)
                .collect(Collectors.toList());
            try {
                this.bulkMongo(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY, collectionWM);
                this.bulkElasticsearch(
                        FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY,
                        Lists.newArrayList(accessionRegisterSummary)
                    );
            } catch (DatabaseException e) {
                if (e.getCause() instanceof MongoBulkWriteException) {
                    LOGGER.warn(
                        "[Reconstruction]: [Optimistic_Lock]: optimistic lock occurs while reconstruct Accession Register"
                    );
                } else {
                    LOGGER.error(e);
                }
            }
        } finally {
            VitamThreadUtils.getVitamSession().setTenantId(originalTenant);
        }
    }

    @Override
    public List<Document> agregateAccessionRegisterSummary(Set<String> originatingAgencies, Integer tenant) {
        MongoCollection<AccessionRegisterDetail> accessionRegisterDetailCollection =
            FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getCollection();
        AggregateIterable<Document> aggregate = accessionRegisterDetailCollection.aggregate(
            Arrays.asList(
                Aggregates.match(
                    and(
                        in(AccessionRegisterDetail.ORIGINATING_AGENCY, originatingAgencies),
                        eq(AccessionRegisterDetail.TENANT_ID, tenant)
                    )
                ),
                Aggregates.group(
                    "$" + AccessionRegisterDetail.ORIGINATING_AGENCY,
                    Accumulators.sum(TOTAL_OBJECT_GROUPS_INGESTED, "$TotalObjectGroups.ingested"),
                    Accumulators.sum(TOTAL_OBJECT_GROUPS_DELETED, "$TotalObjectGroups.deleted"),
                    Accumulators.sum(TOTAL_OBJECT_GROUPS_REMAINED, "$TotalObjectGroups.remained"),
                    Accumulators.sum(TOTAL_UNITS_INGESTED, "$TotalUnits.ingested"),
                    Accumulators.sum(TOTAL_UNITS_DELETED, "$TotalUnits.deleted"),
                    Accumulators.sum(TOTAL_UNITS_REMAINED, "$TotalUnits.remained"),
                    Accumulators.sum(TOTAL_OBJECT_INGEST, "$TotalObjects.ingested"),
                    Accumulators.sum(TOTAL_OBJECT_DELETED, "$TotalObjects.deleted"),
                    Accumulators.sum(TOTAL_OBJECT_REMAINED, "$TotalObjects.remained"),
                    Accumulators.sum(OBJECT_SIZE_INGESTED, "$ObjectSize.ingested"),
                    Accumulators.sum(OBJECT_SIZE_DELETED, "$ObjectSize.deleted"),
                    Accumulators.sum(OBJECT_SIZE_REMAINED, "$ObjectSize.remained")
                ),
                Aggregates.project(
                    Projections.fields(
                        new Document("_id", 0),
                        new Document(AccessionRegisterDetail.ORIGINATING_AGENCY, "$_id"),
                        new Document(
                            AccessionRegisterSummary.TOTAL_OBJECTGROUPS,
                            new Document()
                                .append(AccessionRegisterSummary.INGESTED, "$" + TOTAL_OBJECT_GROUPS_INGESTED)
                                .append(AccessionRegisterSummary.DELETED, "$" + TOTAL_OBJECT_GROUPS_DELETED)
                                .append(AccessionRegisterSummary.REMAINED, "$" + TOTAL_OBJECT_GROUPS_REMAINED)
                        ),
                        new Document(
                            AccessionRegisterSummary.TOTAL_UNITS,
                            new Document()
                                .append(AccessionRegisterSummary.INGESTED, "$" + TOTAL_UNITS_INGESTED)
                                .append(AccessionRegisterSummary.DELETED, "$" + TOTAL_UNITS_DELETED)
                                .append(AccessionRegisterSummary.REMAINED, "$" + TOTAL_UNITS_REMAINED)
                        ),
                        new Document(
                            AccessionRegisterSummary.TOTAL_OBJECTS,
                            new Document()
                                .append(AccessionRegisterSummary.INGESTED, "$" + TOTAL_OBJECT_INGEST)
                                .append(AccessionRegisterSummary.DELETED, "$" + TOTAL_OBJECT_DELETED)
                                .append(AccessionRegisterSummary.REMAINED, "$" + TOTAL_OBJECT_REMAINED)
                        ),
                        new Document(
                            AccessionRegisterSummary.OBJECT_SIZE,
                            new Document()
                                .append(AccessionRegisterSummary.INGESTED, "$" + OBJECT_SIZE_INGESTED)
                                .append(AccessionRegisterSummary.DELETED, "$" + OBJECT_SIZE_DELETED)
                                .append(AccessionRegisterSummary.REMAINED, "$" + OBJECT_SIZE_REMAINED)
                        )
                    )
                )
            ),
            Document.class
        );

        return Lists.newArrayList(aggregate.iterator());
    }
}
