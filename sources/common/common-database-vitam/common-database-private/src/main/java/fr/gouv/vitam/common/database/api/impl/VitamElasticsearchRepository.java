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

package fr.gouv.vitam.common.database.api.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.BulkIndexByScrollFailure;
import co.elastic.clients.elasticsearch._types.Conflicts;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.OpType;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch._types.SearchType;
import co.elastic.clients.elasticsearch._types.ShardFailure;
import co.elastic.clients.elasticsearch._types.ShardStatistics;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.WriteResponseBase;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.DeleteByQueryRequest;
import co.elastic.clients.elasticsearch.core.DeleteByQueryResponse;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.SourceConfigParam;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Iterators;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.api.VitamRepositoryStatus;
import fr.gouv.vitam.common.database.builder.request.configuration.GlobalDatas;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchIndexAliasResolver;
import fr.gouv.vitam.common.database.server.elasticsearch.model.ElasticsearchCollections;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import jakarta.ws.rs.core.Response;
import org.apache.commons.collections4.CollectionUtils;
import org.bson.Document;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchUtil.boolMust;
import static fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchUtil.buildFailureMessage;
import static fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchUtil.getDocSorts;
import static fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchUtil.matchAll;
import static fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchUtil.termQuery;
import static fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchUtil.timeOfMilliseconds;
import static fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchUtil.toDatabaseException;

/**
 * Implementation for Elasticsearch
 */
public class VitamElasticsearchRepository {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(VitamElasticsearchRepository.class);
    /**
     * Identifier
     */
    public static final String IDENTIFIER = "Identifier";
    public static final String ID = "_id";
    private static final String ALL_PARAMS_REQUIRED = "All params are required";
    private static final String BULK_REQ_FAIL_WITH_ERROR = "Bulk Request failure with error: ";
    private static final String EV_DET_DATA = "evDetData";
    private static final String RIGHT_STATE_ID = "rightsStatementIdentifier";
    private static final String AG_ID_EXT = "agIdExt";
    private static final String EVENTS = "events";

    private final ElasticsearchClient client;
    private final ElasticsearchIndexAliasResolver elasticsearchIndexAliasResolver;

    /**
     * VitamElasticsearchRepository Constructor
     *
     * @param client the es client
     * @param elasticsearchIndexAliasResolver the name of the index
     */
    public VitamElasticsearchRepository(
        ElasticsearchClient client,
        ElasticsearchIndexAliasResolver elasticsearchIndexAliasResolver
    ) {
        this.client = client;
        this.elasticsearchIndexAliasResolver = elasticsearchIndexAliasResolver;
    }

    public VitamRepositoryStatus save(Document document) throws DatabaseException {
        ParametersChecker.checkParameter(ALL_PARAMS_REQUIRED, document);
        Document internalDocument = new Document(document);
        Integer tenant = internalDocument.getInteger(VitamDocument.TENANT_ID);
        String id = (String) internalDocument.remove(VitamDocument.ID);
        Object score = internalDocument.remove(VitamDocument.SCORE);
        try {
            String indexName = this.elasticsearchIndexAliasResolver.resolveIndexName(tenant).getName();
            IndexRequest<Document> request = IndexRequest.of(
                builder ->
                    builder
                        .index(indexName)
                        .id(id)
                        .document(internalDocument)
                        .refresh(Refresh.True)
                        .timeout(
                            timeOfMilliseconds(VitamConfiguration.getElasticSearchTimeoutWaitRequestInMilliseconds())
                        )
                        .opType(OpType.Index)
            );

            IndexResponse indexResponse;
            try {
                indexResponse = client.index(request);
            } catch (IOException | ElasticsearchException e) {
                throw toDatabaseException(e);
            }

            if (indexResponse.result() == Result.Created) {
                return VitamRepositoryStatus.CREATED;
            } else if (indexResponse.result() == Result.Updated) {
                return VitamRepositoryStatus.UPDATED;
            }

            return handleFailures(indexResponse);
        } finally {
            internalDocument.put(VitamDocument.ID, id);
            if (Objects.nonNull(score)) {
                internalDocument.put(VitamDocument.SCORE, score);
            }
        }
    }

    private VitamRepositoryStatus handleFailures(WriteResponseBase response) throws DatabaseException {
        String error = null;
        ShardStatistics shardInfo = response.shards();
        if (!Objects.equals(shardInfo.total(), shardInfo.successful())) {
            error = String.format(
                "Exception occurred : total shards %s, successful shards %s",
                shardInfo.total(),
                shardInfo.successful()
            );
        }
        if (shardInfo.failed().intValue() > 0) {
            StringBuilder failures = new StringBuilder();
            for (ShardFailure failure : shardInfo.failures()) {
                String reason = failure.reason().reason();
                failures.append(reason).append(" ; ");
            }
            error = String.format("Exception occurred caused by : %s", failures);
        }

        if (null == error) {
            error = String.format("Insert Documents Exception caused by : %s", response.result().toString());
        }

        LOGGER.error(error);
        throw new DatabaseException(error);
    }

    public void save(List<Document> documents) throws DatabaseException {
        ParametersChecker.checkParameter(ALL_PARAMS_REQUIRED, documents);

        if (documents.isEmpty()) {
            return;
        }

        BulkRequest.Builder bulkRequestBuilder = new BulkRequest.Builder();

        documents.forEach(document -> {
            Document internalDocument = new Document(document);
            Integer tenant = internalDocument.getInteger(VitamDocument.TENANT_ID);
            String id = (String) internalDocument.remove(VitamDocument.ID);
            internalDocument.remove(VitamDocument.SCORE);

            String aliasName = this.elasticsearchIndexAliasResolver.resolveIndexName(tenant).getName();

            bulkRequestBuilder.operations(o -> o.index(idx -> idx.index(aliasName).id(id).document(internalDocument)));
        });
        bulkRequestBuilder.refresh(Refresh.True);

        final BulkResponse bulkResponse;
        try {
            bulkResponse = client.bulk(bulkRequestBuilder.build());
        } catch (IOException | ElasticsearchException e) {
            throw toDatabaseException(e);
        }

        if (bulkResponse.errors()) {
            LOGGER.error(BULK_REQ_FAIL_WITH_ERROR + buildFailureMessage(bulkResponse));
            throw new DatabaseException("Bulk Request failure");
        }
    }

    public void save(ElasticsearchCollections elasticsearchCollections, List<Document> documents)
        throws DatabaseException {
        ParametersChecker.checkParameter(ALL_PARAMS_REQUIRED, documents);

        if (documents.isEmpty()) {
            return;
        }

        BulkRequest.Builder bulkRequestBuilder = new BulkRequest.Builder();

        documents.forEach(document -> {
            Document internalDocument = new Document(document);
            Integer tenant = internalDocument.getInteger(VitamDocument.TENANT_ID);
            String id = (String) internalDocument.remove(VitamDocument.ID);
            internalDocument.remove(VitamDocument.SCORE);

            String aliasName = this.elasticsearchIndexAliasResolver.resolveIndexName(tenant).getName();

            if (ElasticsearchCollections.OPERATION == elasticsearchCollections) {
                transformDataForElastic(internalDocument);
            }

            bulkRequestBuilder.operations(o -> o.index(idx -> idx.index(aliasName).id(id).document(internalDocument)));
        });
        bulkRequestBuilder.refresh(Refresh.True);

        final BulkResponse bulkResponse;
        try {
            bulkResponse = client.bulk(bulkRequestBuilder.build());
        } catch (IOException | ElasticsearchException e) {
            throw toDatabaseException(e);
        }

        if (bulkResponse.errors()) {
            LOGGER.error(BULK_REQ_FAIL_WITH_ERROR + buildFailureMessage(bulkResponse));
            throw new DatabaseException("Bulk Request failure");
        }
    }

    // TODO : very ugly here, should be generic
    private void transformDataForElastic(Document vitamDocument) {
        if (vitamDocument.get(EV_DET_DATA) != null) {
            String evDetDataString = (String) vitamDocument.get(EV_DET_DATA);
            LOGGER.debug(evDetDataString);
            try {
                JsonNode evDetData = JsonHandler.getFromString(evDetDataString);
                vitamDocument.remove(EV_DET_DATA);
                vitamDocument.put(EV_DET_DATA, evDetData);
            } catch (InvalidParseOperationException e) {
                LOGGER.warn("EvDetData is not a json compatible field", e);
                throw new RuntimeException(e);
            }
        }
        if (vitamDocument.get(AG_ID_EXT) != null) {
            String agidExt = (String) vitamDocument.get(AG_ID_EXT);
            LOGGER.debug(agidExt);
            try {
                JsonNode agidExtNode = JsonHandler.getFromString(agidExt);
                vitamDocument.remove(AG_ID_EXT);
                vitamDocument.put(AG_ID_EXT, agidExtNode);
            } catch (InvalidParseOperationException e) {
                LOGGER.warn("agidExtNode is not a json compatible field", e);
            }
        }
        if (vitamDocument.get(RIGHT_STATE_ID) != null) {
            String rightsStatementIdentifier = (String) vitamDocument.get(RIGHT_STATE_ID);
            LOGGER.debug(rightsStatementIdentifier);
            try {
                JsonNode rightsStatementIdentifierNode = JsonHandler.getFromString(rightsStatementIdentifier);
                vitamDocument.remove(RIGHT_STATE_ID);
                vitamDocument.put(RIGHT_STATE_ID, rightsStatementIdentifierNode);
            } catch (InvalidParseOperationException e) {
                LOGGER.warn("rightsStatementIdentifier is not a json compatible field", e);
            }
        }
        List<Document> eventDocuments = (List<Document>) vitamDocument.get(EVENTS);
        if (eventDocuments != null) {
            for (Document eventDocument : eventDocuments) {
                if (eventDocument.getString(EV_DET_DATA) != null) {
                    String eventEvDetDataString = eventDocument.getString(EV_DET_DATA);
                    Document eventEvDetDataDocument = Document.parse(eventEvDetDataString);
                    eventDocument.remove(EV_DET_DATA);
                    eventDocument.put(EV_DET_DATA, eventEvDetDataDocument);
                }
                if (eventDocument.getString(RIGHT_STATE_ID) != null) {
                    String eventrightsStatementIdentifier = eventDocument.getString(RIGHT_STATE_ID);
                    Document eventEvDetDataDocument = Document.parse(eventrightsStatementIdentifier);
                    eventDocument.remove(RIGHT_STATE_ID);
                    eventDocument.put(RIGHT_STATE_ID, eventEvDetDataDocument);
                }
                if (eventDocument.getString(AG_ID_EXT) != null) {
                    String eventagIdExt = eventDocument.getString(AG_ID_EXT);
                    Document eventEvDetDataDocument = Document.parse(eventagIdExt);
                    eventDocument.remove(AG_ID_EXT);
                    eventDocument.put(AG_ID_EXT, eventEvDetDataDocument);
                }
            }
        }
        vitamDocument.remove(EVENTS);
        vitamDocument.put(EVENTS, eventDocuments);
    }

    public void remove(String id, Integer tenant) throws DatabaseException {
        ParametersChecker.checkParameter(ALL_PARAMS_REQUIRED, id);

        String index = this.elasticsearchIndexAliasResolver.resolveIndexName(tenant).getName();

        DeleteRequest request = new DeleteRequest.Builder()
            .index(index)
            .id(id)
            .timeout(timeOfMilliseconds(VitamConfiguration.getElasticSearchTimeoutWaitRequestInMilliseconds()))
            .refresh(Refresh.True)
            .build();

        DeleteResponse deleteResponse;
        try {
            deleteResponse = client.delete(request);
        } catch (IOException e) {
            throw new DatabaseException(e);
        } catch (ElasticsearchException e) {
            if (e.status() == Response.Status.NOT_FOUND.getStatusCode()) {
                //Nothing to do
                return;
            }
            throw toDatabaseException(e);
        }

        Result result = deleteResponse.result();
        switch (result) {
            case Deleted:
            case NotFound:
                break;
            default:
                handleFailures(deleteResponse);
        }
    }

    public long purge(Integer tenant) throws DatabaseException {
        ParametersChecker.checkParameter(ALL_PARAMS_REQUIRED, tenant);

        String index = this.elasticsearchIndexAliasResolver.resolveIndexName(tenant).getName();

        Query qb = termQuery(VitamDocument.TENANT_ID, tenant);

        return handlePurge(client, index, qb);
    }

    private static long handlePurge(ElasticsearchClient client, String index, Query qb) throws DatabaseException {
        try {
            DeleteByQueryRequest request = new DeleteByQueryRequest.Builder()
                .index(index)
                .conflicts(Conflicts.Proceed)
                .query(qb)
                .scrollSize((long) VitamConfiguration.getMaxElasticsearchBulk())
                .scroll(timeOfMilliseconds(VitamConfiguration.getElasticSearchScrollTimeoutInMilliseconds()))
                .timeout(timeOfMilliseconds(VitamConfiguration.getElasticSearchTimeoutWaitRequestInMilliseconds()))
                .refresh(true)
                .build();

            DeleteByQueryResponse response = client.deleteByQuery(request);

            LOGGER.debug("Purge details: {}", response);

            List<BulkIndexByScrollFailure> searchFailures = response.failures();
            if (CollectionUtils.isNotEmpty(searchFailures)) {
                throw new DatabaseException("ES purge errors : in search phase");
            }

            return Objects.requireNonNull(response.deleted());
        } catch (IOException | ElasticsearchException e) {
            throw toDatabaseException("Purge Exception", e);
        }
    }

    public long purge() throws DatabaseException {
        String index = this.elasticsearchIndexAliasResolver.resolveIndexName(null).getName();
        Query qb = matchAll();
        return handlePurge(client, index, qb);
    }

    public Optional<Document> getByID(String id, Integer tenant) throws DatabaseException {
        ParametersChecker.checkParameter(ALL_PARAMS_REQUIRED, id);

        String index = this.elasticsearchIndexAliasResolver.resolveIndexName(tenant).getName();
        GetRequest request = new GetRequest.Builder()
            .index(index)
            .id(id)
            .source(SourceConfigParam.of(i -> i.fetch(true)))
            .build();
        GetResponse<Document> response;
        try {
            response = client.get(request, Document.class);
        } catch (IOException | ElasticsearchException e) {
            throw toDatabaseException(e);
        }

        if (response.found()) {
            return Optional.of(Objects.requireNonNull(response.source()));
        } else {
            return Optional.empty();
        }
    }

    /**
     * WARNING: Query all indices. Use with caution!
     */
    public Optional<Document> getDocumentByIdCrossIndices(String id) throws DatabaseException {
        ParametersChecker.checkParameter(ALL_PARAMS_REQUIRED, id);

        SearchRequest searchRequest = new SearchRequest.Builder().query(termQuery(ID, id)).from(0).size(1).build();

        SearchResponse<Document> searchResponse;
        try {
            searchResponse = client.search(searchRequest, Document.class);
        } catch (IOException | ElasticsearchException e) {
            throw toDatabaseException(e);
        }
        for (Hit<Document> hit : searchResponse.hits().hits()) {
            return Optional.of(Objects.requireNonNull(hit.source()));
        }

        return Optional.empty();
    }

    public Optional<Document> findByIdentifierAndTenant(String identifier, Integer tenant) throws DatabaseException {
        ParametersChecker.checkParameter(ALL_PARAMS_REQUIRED, tenant);

        String aliasName = this.elasticsearchIndexAliasResolver.resolveIndexName(tenant).getName();
        Query qb = boolMust(termQuery(IDENTIFIER, identifier), termQuery(VitamDocument.TENANT_ID, tenant));

        return handleSearch(aliasName, qb);
    }

    private Optional<Document> handleSearch(String index, Query qb) throws DatabaseException {
        SearchResponse<Document> search = search(index, qb);

        for (Hit<Document> hit : search.hits().hits()) {
            return Optional.of(Objects.requireNonNull(hit.source()));
        }

        return Optional.empty();
    }

    public SearchResponse<Document> search(String index, Query qb) throws DatabaseException {
        SearchRequest searchRequest = new SearchRequest.Builder()
            .index(index)
            .scroll(timeOfMilliseconds(60000))
            .searchType(SearchType.DfsQueryThenFetch)
            .query(qb)
            .size(GlobalDatas.LIMIT_LOAD)
            .sort(getDocSorts(SortOrder.Asc))
            .build();

        try {
            return client.search(searchRequest, Document.class);
        } catch (IOException | ElasticsearchException e) {
            throw toDatabaseException("Search failed", e);
        }
    }

    public Optional<Document> findByIdentifier(String identifier) throws DatabaseException {
        ParametersChecker.checkParameter(ALL_PARAMS_REQUIRED);
        String index = this.elasticsearchIndexAliasResolver.resolveIndexName(null).getName();
        return handleSearch(index, termQuery(IDENTIFIER, identifier));
    }

    public void delete(List<String> ids, int tenant) throws DatabaseException {
        String index = this.elasticsearchIndexAliasResolver.resolveIndexName(tenant).getName();
        Iterator<List<String>> idIterator = Iterators.partition(
            ids.iterator(),
            VitamConfiguration.getMaxElasticsearchBulk()
        );

        while (idIterator.hasNext()) {
            List<String> documentIds = idIterator.next();
            BulkRequest bulkRequest = new BulkRequest.Builder()
                .operations(
                    documentIds
                        .stream()
                        .map(id -> BulkOperation.of(b -> b.delete(d -> d.id(id).index(index))))
                        .collect(Collectors.toList())
                )
                .refresh(idIterator.hasNext() ? Refresh.False : Refresh.True)
                .build();

            BulkResponse bulkResponse;
            try {
                bulkResponse = client.bulk(bulkRequest);
            } catch (IOException | ElasticsearchException e) {
                throw toDatabaseException("Bulk delete exception", e);
            }

            if (bulkResponse.errors()) {
                LOGGER.error("ES delete in error: " + buildFailureMessage(bulkResponse));
                throw new DatabaseException("An error occurred while bulk deleting documents");
            }
        }
    }
}
