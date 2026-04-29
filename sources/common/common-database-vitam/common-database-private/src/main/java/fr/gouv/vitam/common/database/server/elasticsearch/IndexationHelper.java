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
package fr.gouv.vitam.common.database.server.elasticsearch;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import com.google.common.collect.Iterators;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.api.impl.VitamElasticsearchRepository;
import fr.gouv.vitam.common.database.api.impl.VitamMongoRepository;
import fr.gouv.vitam.common.database.index.model.ReindexationKO;
import fr.gouv.vitam.common.database.index.model.ReindexationOK;
import fr.gouv.vitam.common.database.index.model.ReindexationResult;
import fr.gouv.vitam.common.database.index.model.SwitchIndexResult;
import fr.gouv.vitam.common.database.parameter.IndexParameters;
import fr.gouv.vitam.common.database.parameter.SwitchIndexParameters;
import fr.gouv.vitam.common.database.server.elasticsearch.model.ElasticsearchCollections;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import org.apache.commons.collections4.CollectionUtils;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchUtil.toDatabaseException;

/**
 * IndexationHelper useful method for indexation
 */
public class IndexationHelper {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IndexationHelper.class);

    private static final IndexationHelper instance = new IndexationHelper();

    public static IndexationHelper getInstance() {
        return instance;
    }

    /**
     * Converts a LocalDate to LocalDateTime at start of day
     *
     * @param localDate the LocalDate to convert
     * @return LocalDateTime or null if localDate is null
     */
    private LocalDateTime convertLocalDateToLocalDateTime(LocalDate localDate) {
        if (localDate == null) {
            return null;
        }
        return localDate.atStartOfDay();
    }

    /**
     * reindex a collection on a tenant list with a esmapping file
     *
     * @param collection the collection to be reindexed
     * @param esClient the elastic client to be used to reindex
     * @param indexAlias the elastic index alias information
     * @param indexationStartDate optional start date for indexation. If provided, reindexation will start from this date
     * @return the result of the reindexation as a IndexationResult object
     */
    public ReindexationOK reindex(
        MongoCollection<Document> collection,
        ElasticsearchAccess esClient,
        ElasticsearchIndexAlias indexAlias,
        ElasticsearchIndexSettings indexSettings,
        ElasticsearchCollections elasticsearchCollection,
        List<Integer> tenantIds,
        String tenantGroupName,
        String elasticsearchConfigurationFilePath,
        LocalDate indexationStartDate
    ) throws DatabaseException {
        MongoCursor<Document> cursor = null;
        long numberOfDocumentsToIndex;
        try {
            // Select data from mongo
            VitamMongoRepository vitamMongoRepository = new VitamMongoRepository(collection);

            Bson filter = buildReindexationFilter(tenantIds, indexationStartDate);

            logReindexationStart(indexAlias, tenantIds);

            LOGGER.warn("Starting document count for reindexation on collection: {}", elasticsearchCollection.name());
            numberOfDocumentsToIndex = countDocuments(vitamMongoRepository, filter);
            LOGGER.warn(
                "Document count completed for collection {}: {} documents to index",
                elasticsearchCollection.name(),
                numberOfDocumentsToIndex
            );

            cursor = (filter != null)
                ? vitamMongoRepository.findDocuments(filter, VitamConfiguration.getMaxElasticsearchBulk()).iterator()
                : vitamMongoRepository.findDocuments(VitamConfiguration.getMaxElasticsearchBulk()).iterator();

            ElasticsearchIndexAlias targetIndex;

            if (indexationStartDate != null) {
                // Use existing index for upserts
                targetIndex = indexAlias;
                LOGGER.warn("Using existing index {} for incremental reindexation", indexAlias.getName());
            } else {
                // Create ElasticSearch new index for a given collection
                targetIndex = esClient.createIndexWithoutAlias(
                    indexAlias,
                    indexSettings,
                    elasticsearchConfigurationFilePath
                );
                LOGGER.warn("Created new index {} for full reindexation", targetIndex.getName());
            }

            // Create repository for the given indexName
            VitamElasticsearchRepository vitamElasticsearchRepository = new VitamElasticsearchRepository(
                esClient.getClient(),
                tenant -> targetIndex
            );

            LOGGER.warn("number of documents to index = {}", numberOfDocumentsToIndex);
            long numberOfDocumentsIndexed = 0;
            // Reindex document with bulk
            Iterator<List<Document>> bulkDocumentIterator = Iterators.partition(
                cursor,
                VitamConfiguration.getMaxElasticsearchBulk()
            );
            while (bulkDocumentIterator.hasNext()) {
                List<Document> documents = bulkDocumentIterator.next();
                if (elasticsearchCollection == ElasticsearchCollections.OPERATION) {
                    vitamElasticsearchRepository.save(ElasticsearchCollections.OPERATION, documents);
                } else {
                    vitamElasticsearchRepository.save(documents);
                }
                numberOfDocumentsIndexed += documents.size();
                LOGGER.warn(
                    "Reindexation in progress {}%",
                    0.01 * ((int) (((float) numberOfDocumentsIndexed / numberOfDocumentsToIndex) * 10000))
                );
                LOGGER.warn(
                    "number of indexed documents = {}\t number of remaining documents = {}",
                    numberOfDocumentsIndexed,
                    numberOfDocumentsToIndex - numberOfDocumentsIndexed
                );
            }
            LOGGER.warn("Reindexation ended successfully");
            return new ReindexationOK(indexAlias.getName(), targetIndex.getName(), tenantIds, tenantGroupName);
        } catch (DatabaseException | RuntimeException e) {
            LOGGER.error("Reindexation failed", e);
            throw e;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * reindex a collection on a tenant list with a esmapping file
     *
     * @param collection the collection to be reindexed
     * @param esClient the elastic client to be used to reindex
     * @param indexAlias the elastic index alias information
     * @return the result of the reindexation as a IndexationResult object
     */
    public ReindexationOK reindex(
        MongoCollection<Document> collection,
        ElasticsearchAccess esClient,
        ElasticsearchIndexAlias indexAlias,
        ElasticsearchIndexSettings indexSettings,
        ElasticsearchCollections elasticsearchCollection,
        List<Integer> tenantIds,
        String tenantGroupName,
        String elasticsearchConfigurationFilePath
    ) throws DatabaseException {
        return reindex(
            collection,
            esClient,
            indexAlias,
            indexSettings,
            elasticsearchCollection,
            tenantIds,
            tenantGroupName,
            elasticsearchConfigurationFilePath,
            null
        );
    }

    private Bson buildReindexationFilter(List<Integer> tenantIds, LocalDate indexationStartDate) {
        List<Bson> filters = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(tenantIds)) {
            filters.add(Filters.in(VitamDocument.TENANT_ID, tenantIds));
        }

        if (indexationStartDate != null) {
            filters.add(
                Filters.gte("_aud", LocalDateUtil.getFormattedDateTimeForMongo(indexationStartDate.atStartOfDay()))
            );
        }

        if (filters.isEmpty()) {
            return null;
        }

        return filters.size() == 1 ? filters.getFirst() : Filters.and(filters);
    }

    private void logReindexationStart(ElasticsearchIndexAlias indexAlias, List<Integer> tenantIds) {
        if (CollectionUtils.isNotEmpty(tenantIds)) {
            LOGGER.warn("Reindexation started on index {} in tenants {}", indexAlias.getName(), tenantIds);
        } else {
            LOGGER.warn("Reindexation started on index {} in all tenants", indexAlias.getName());
        }
    }

    private long countDocuments(VitamMongoRepository vitamMongoRepository, Bson filter) {
        return filter != null ? vitamMongoRepository.count(filter) : vitamMongoRepository.count();
    }

    /**
     * switch index, attach a new index to an existing alias
     *
     * @param indexAlias the alias information
     * @param newIndex the new index name to switch on
     * @param esClient the elastic client
     * @throws DatabaseException if an error occurs
     */
    public SwitchIndexResult switchIndex(
        ElasticsearchIndexAlias indexAlias,
        ElasticsearchIndexAlias newIndex,
        ElasticsearchAccess esClient
    ) throws DatabaseException {
        try {
            if (!indexAlias.isValidAliasOfIndex(newIndex)) {
                throw new DatabaseException(
                    "Illegal index name '" + newIndex + "' for alias '" + indexAlias.getName() + "'"
                );
            }
            esClient.switchIndex(indexAlias, newIndex);
            return new SwitchIndexResult()
                .setAlias(indexAlias.getName())
                .setIndexName(newIndex.getName())
                .setStatusCode(StatusCode.OK);
        } catch (IOException | ElasticsearchException e) {
            throw toDatabaseException(e);
        }
    }

    /**
     * Validates indexation parameters according to business rules
     *
     * @param indexParameters the parameters to validate
     * @throws IllegalStateException if indexationStartDate is not null and collection is not UNIT or OBJECTGROUP
     */
    public void validateIndexationParameters(List<IndexParameters> indexParameters) {
        for (IndexParameters index : indexParameters) {
            if (index.getIndexationStartDate() != null) {
                String collectionName = index.getCollectionName().toUpperCase();
                if (
                    !ElasticsearchCollections.UNIT.getIndexName().toUpperCase().equals(collectionName) &&
                    !ElasticsearchCollections.OBJECTGROUP.getIndexName().toUpperCase().equals(collectionName)
                ) {
                    throw new IllegalStateException(
                        String.format(
                            "Indexation with start date is only allowed for UNIT and OBJECTGROUP collections, but was requested for collection: %s",
                            index.getCollectionName()
                        )
                    );
                }
            }
        }
    }

    /**
     * Get KO Result for reindexation orders
     *
     * @param indexParameters the index parameter
     * @param message the message to be added
     * @return the final result as an IndexationResult object
     */
    public ReindexationResult getFullKOResult(IndexParameters indexParameters, String message) {
        ReindexationResult result = new ReindexationResult();
        result.setIndexKO(Collections.singletonList(new ReindexationKO(indexParameters.getTenants(), null, message)));
        result.setCollectionName(indexParameters.getCollectionName());
        return result;
    }

    /**
     * Get KO Result for switching order
     *
     * @param switchIndexParameters the switch index parameter
     * @param message the message to be added
     * @return the final result as an IndexationResult object
     */
    public SwitchIndexResult getKOResult(SwitchIndexParameters switchIndexParameters, String message) {
        return new SwitchIndexResult()
            .setAlias(switchIndexParameters.getAlias())
            .setIndexName(switchIndexParameters.getIndexName())
            .setStatusCode(StatusCode.KO)
            .setMessage(message);
    }
}
