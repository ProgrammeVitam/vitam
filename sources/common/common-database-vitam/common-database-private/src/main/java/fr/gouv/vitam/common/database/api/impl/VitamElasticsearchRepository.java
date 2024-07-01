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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Iterators;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.api.VitamRepositoryStatus;
import fr.gouv.vitam.common.database.builder.request.configuration.GlobalDatas;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchIndexAliasResolver;
import fr.gouv.vitam.common.database.server.elasticsearch.model.ElasticsearchCollections;
import fr.gouv.vitam.common.database.server.mongodb.BsonHelper;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import org.apache.commons.collections.CollectionUtils;
import org.bson.Document;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.support.replication.ReplicationResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.index.reindex.ScrollableHitSource;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.xcontent.XContentType;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;

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

    private final RestHighLevelClient client;
    private final ElasticsearchIndexAliasResolver elasticsearchIndexAliasResolver;

    /**
     * VitamElasticsearchRepository Constructor
     *
     * @param client the es client
     * @param elasticsearchIndexAliasResolver the name of the index
     */
    public VitamElasticsearchRepository(
        RestHighLevelClient client,
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
            final String source = BsonHelper.stringify(internalDocument);

            String aliasName = this.elasticsearchIndexAliasResolver.resolveIndexName(tenant).getName();
            IndexRequest request = new IndexRequest(aliasName)
                .id(id)
                .source(source, XContentType.JSON)
                .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE)
                .timeout(
                    TimeValue.timeValueMillis(VitamConfiguration.getElasticSearchTimeoutWaitRequestInMilliseconds())
                )
                .opType(DocWriteRequest.OpType.INDEX);

            IndexResponse indexResponse;
            try {
                indexResponse = client.index(request, RequestOptions.DEFAULT);
            } catch (IOException e) {
                throw new DatabaseException(e);
            }

            if (indexResponse.getResult() == DocWriteResponse.Result.CREATED) {
                return VitamRepositoryStatus.CREATED;
            } else if (indexResponse.getResult() == DocWriteResponse.Result.UPDATED) {
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

    private VitamRepositoryStatus handleFailures(DocWriteResponse indexResponse) throws DatabaseException {
        String error = null;
        ReplicationResponse.ShardInfo shardInfo = indexResponse.getShardInfo();
        if (shardInfo.getTotal() != shardInfo.getSuccessful()) {
            error = String.format(
                "Exception occurred : total shards %s, successful shards %s",
                shardInfo.getTotal(),
                shardInfo.getSuccessful()
            );
        }
        if (shardInfo.getFailed() > 0) {
            StringBuilder failures = new StringBuilder();
            for (ReplicationResponse.ShardInfo.Failure failure : shardInfo.getFailures()) {
                String reason = failure.reason();
                failures.append(reason).append(" ; ");
            }
            error = String.format("Exception occurred caused by : %s", failures.toString());
        }

        if (null == error) {
            error = String.format("Insert Documents Exception caused by : %s", indexResponse.getResult().toString());
        }

        LOGGER.error(error);
        throw new DatabaseException(error);
    }

    public void save(List<Document> documents) throws DatabaseException {
        ParametersChecker.checkParameter(ALL_PARAMS_REQUIRED, documents);

        if (documents.isEmpty()) {
            return;
        }

        BulkRequest bulkRequest = new BulkRequest();

        documents.forEach(document -> {
            Document internalDocument = new Document(document);
            Integer tenant = internalDocument.getInteger(VitamDocument.TENANT_ID);
            String id = (String) internalDocument.remove(VitamDocument.ID);
            internalDocument.remove(VitamDocument.SCORE);
            internalDocument.remove("_unused");

            final String source = BsonHelper.stringify(internalDocument);

            String aliasName = this.elasticsearchIndexAliasResolver.resolveIndexName(tenant).getName();

            bulkRequest.add(new IndexRequest(aliasName).id(id).source(source, XContentType.JSON));
        });

        if (bulkRequest.numberOfActions() != 0) {
            final BulkResponse bulkResponse;
            try {
                bulkRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
                bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
            } catch (IOException e) {
                throw new DatabaseException(e);
            }

            if (bulkResponse.hasFailures()) {
                LOGGER.error(BULK_REQ_FAIL_WITH_ERROR + bulkResponse.buildFailureMessage());
                throw new DatabaseException(bulkResponse.buildFailureMessage());
            }
        }
    }

    public void save(ElasticsearchCollections elasticsearchCollections, List<Document> documents)
        throws DatabaseException {
        ParametersChecker.checkParameter(ALL_PARAMS_REQUIRED, documents);
        BulkRequest bulkRequest = new BulkRequest();

        documents.forEach(document -> {
            Document internalDocument = new Document(document);
            Integer tenant = internalDocument.getInteger(VitamDocument.TENANT_ID);
            String id = (String) internalDocument.remove(VitamDocument.ID);
            internalDocument.remove(VitamDocument.SCORE);
            internalDocument.remove("_unused");

            String aliasName = this.elasticsearchIndexAliasResolver.resolveIndexName(tenant).getName();

            switch (elasticsearchCollections) {
                case OPERATION:
                    transformDataForElastic(internalDocument);
                    break;
            }

            final String source = BsonHelper.stringify(internalDocument);

            bulkRequest.add(new IndexRequest(aliasName).id(id).source(source, XContentType.JSON));
        });

        if (bulkRequest.numberOfActions() != 0) {
            final BulkResponse bulkResponse;
            try {
                bulkRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
                bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
            } catch (IOException e) {
                throw new DatabaseException(e);
            }

            if (bulkResponse.hasFailures()) {
                LOGGER.error(BULK_REQ_FAIL_WITH_ERROR + bulkResponse.buildFailureMessage());
                throw new DatabaseException(bulkResponse.buildFailureMessage());
            }
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

        DeleteRequest request = new DeleteRequest(index)
            .id(id)
            .timeout(TimeValue.timeValueMillis(VitamConfiguration.getElasticSearchTimeoutWaitRequestInMilliseconds()))
            .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
        DeleteResponse deleteResponse;
        try {
            deleteResponse = client.delete(request, RequestOptions.DEFAULT);
        } catch (IOException | ElasticsearchException e) {
            if (e instanceof ElasticsearchException && ((ElasticsearchException) e).status() == RestStatus.NOT_FOUND) {
                //Nothing to do
                return;
            }

            throw new DatabaseException(e);
        }

        DocWriteResponse.Result result = deleteResponse.getResult();
        switch (result) {
            case DELETED:
            case NOT_FOUND:
                break;
            default:
                handleFailures(deleteResponse);
        }
    }

    public long purge(Integer tenant) throws DatabaseException {
        ParametersChecker.checkParameter(ALL_PARAMS_REQUIRED, tenant);

        String index = this.elasticsearchIndexAliasResolver.resolveIndexName(tenant).getName();

        QueryBuilder qb = termQuery(VitamDocument.TENANT_ID, tenant);

        return handlePurge(client, index, qb);
    }

    private static long handlePurge(RestHighLevelClient client, String index, QueryBuilder qb)
        throws DatabaseException {
        try {
            DeleteByQueryRequest request = new DeleteByQueryRequest(index);
            request.setConflicts("proceed");
            request.setQuery(qb);
            request.setBatchSize(VitamConfiguration.getMaxElasticsearchBulk());
            request.setScroll(
                TimeValue.timeValueMillis(VitamConfiguration.getElasticSearchScrollTimeoutInMilliseconds())
            );
            request.setTimeout(
                TimeValue.timeValueMillis(VitamConfiguration.getElasticSearchTimeoutWaitRequestInMilliseconds())
            );
            request.setRefresh(true);

            BulkByScrollResponse bulkResponse = client.deleteByQuery(request, RequestOptions.DEFAULT);

            TimeValue timeTaken = bulkResponse.getTook();
            boolean timedOut = bulkResponse.isTimedOut();
            long totalDocs = bulkResponse.getTotal();
            long deletedDocs = bulkResponse.getDeleted();
            long batches = bulkResponse.getBatches();
            long noops = bulkResponse.getNoops();
            long versionConflicts = bulkResponse.getVersionConflicts();
            long bulkRetries = bulkResponse.getBulkRetries();
            long searchRetries = bulkResponse.getSearchRetries();
            TimeValue throttledMillis = bulkResponse.getStatus().getThrottled();
            TimeValue throttledUntilMillis = bulkResponse.getStatus().getThrottledUntil();

            LOGGER.debug(
                "Purge : timeTaken (" +
                timeTaken +
                "), timedOut (" +
                timedOut +
                "), totalDocs (" +
                totalDocs +
                ")," +
                " deletedDocs (" +
                deletedDocs +
                "), batches (" +
                batches +
                "), noops (" +
                noops +
                "), versionConflicts (" +
                versionConflicts +
                ")" +
                "bulkRetries (" +
                bulkRetries +
                "), searchRetries (" +
                searchRetries +
                "),  throttledMillis(" +
                throttledMillis +
                "), throttledUntilMillis(" +
                throttledUntilMillis +
                ")"
            );

            List<ScrollableHitSource.SearchFailure> searchFailures = bulkResponse.getSearchFailures();
            if (CollectionUtils.isNotEmpty(searchFailures)) {
                throw new DatabaseException("ES purge errors : in search phase");
            }

            List<BulkItemResponse.Failure> bulkFailures = bulkResponse.getBulkFailures();
            if (CollectionUtils.isNotEmpty(bulkFailures)) {
                throw new DatabaseException("ES purge errors : in bulk phase");
            }

            return bulkResponse.getDeleted();
        } catch (IOException e) {
            throw new DatabaseException("Purge Exception", e);
        }
    }

    public long purge() throws DatabaseException {
        String index = this.elasticsearchIndexAliasResolver.resolveIndexName(null).getName();
        QueryBuilder qb = matchAllQuery();
        return handlePurge(client, index, qb);
    }

    public Optional<Document> getByID(String id, Integer tenant) throws DatabaseException {
        ParametersChecker.checkParameter(ALL_PARAMS_REQUIRED, id);

        String index = this.elasticsearchIndexAliasResolver.resolveIndexName(tenant).getName();
        GetRequest request = new GetRequest(index).id(id).fetchSourceContext(FetchSourceContext.FETCH_SOURCE);
        GetResponse response;
        try {
            response = client.get(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new DatabaseException(e);
        }
        if (response.isExists()) {
            try {
                return Optional.of(JsonHandler.getFromString(response.getSourceAsString(), Document.class));
            } catch (InvalidParseOperationException e) {
                throw new DatabaseException(e);
            }
        } else {
            return Optional.empty();
        }
    }

    /**
     * @param id
     * @return
     * @throws DatabaseException
     */
    public Optional<Document> getDocumentById(String id) throws DatabaseException {
        ParametersChecker.checkParameter(ALL_PARAMS_REQUIRED, id);

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.termQuery(ID, id));
        sourceBuilder.from(0);
        sourceBuilder.size(1);

        SearchRequest searchRequest = new SearchRequest();
        searchRequest.source(sourceBuilder);

        SearchResponse searchResponse;
        try {
            searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new DatabaseException(e);
        }
        for (SearchHit hit : searchResponse.getHits().getHits()) {
            try {
                return Optional.of(JsonHandler.getFromString(hit.getSourceAsString(), Document.class));
            } catch (InvalidParseOperationException e) {
                throw new DatabaseException(e);
            }
        }

        return Optional.empty();
    }

    public Optional<Document> findByIdentifierAndTenant(String identifier, Integer tenant) throws DatabaseException {
        ParametersChecker.checkParameter(ALL_PARAMS_REQUIRED, tenant);

        try {
            String aliasName = this.elasticsearchIndexAliasResolver.resolveIndexName(tenant).getName();
            QueryBuilder qb = boolQuery()
                .must(termQuery(IDENTIFIER, identifier))
                .must(termQuery(VitamDocument.TENANT_ID, tenant));

            return handleSearch(aliasName, qb);
        } catch (IOException e) {
            throw new DatabaseException("Search by identifier and tenant exception", e);
        }
    }

    private Optional<Document> handleSearch(String index, QueryBuilder qb) throws IOException, DatabaseException {
        SearchResponse search = search(index, qb);

        for (SearchHit hit : search.getHits().getHits()) {
            try {
                return Optional.of(JsonHandler.getFromString(hit.getSourceAsString(), Document.class));
            } catch (InvalidParseOperationException e) {
                throw new DatabaseException(e);
            }
        }

        return Optional.empty();
    }

    public SearchResponse search(String index, QueryBuilder qb) throws IOException {
        SearchSourceBuilder searchSourceBuilder = SearchSourceBuilder.searchSource()
            .query(qb)
            .size(GlobalDatas.LIMIT_LOAD)
            .sort(FieldSortBuilder.DOC_FIELD_NAME, SortOrder.ASC);

        SearchRequest searchRequest = new SearchRequest()
            .indices(index)
            .scroll(new TimeValue(60000))
            .searchType(SearchType.DFS_QUERY_THEN_FETCH)
            .source(searchSourceBuilder);

        return client.search(searchRequest, RequestOptions.DEFAULT);
    }

    public Optional<Document> findByIdentifier(String identifier) throws DatabaseException {
        ParametersChecker.checkParameter(ALL_PARAMS_REQUIRED);
        String index = this.elasticsearchIndexAliasResolver.resolveIndexName(null).getName();
        try {
            return handleSearch(index, termQuery(IDENTIFIER, identifier));
        } catch (IOException e) {
            throw new DatabaseException("Search by identifier exception", e);
        }
    }

    public void delete(List<String> ids, int tenant) throws DatabaseException {
        String index = this.elasticsearchIndexAliasResolver.resolveIndexName(tenant).getName();
        Iterator<List<String>> idIterator = Iterators.partition(
            ids.iterator(),
            VitamConfiguration.getMaxElasticsearchBulk()
        );

        while (idIterator.hasNext()) {
            BulkRequest bulkRequest = new BulkRequest();
            for (String id : idIterator.next()) {
                bulkRequest.add(new DeleteRequest(index, id));
            }

            WriteRequest.RefreshPolicy refreshPolicy = idIterator.hasNext()
                ? WriteRequest.RefreshPolicy.NONE
                : WriteRequest.RefreshPolicy.IMMEDIATE;

            bulkRequest.setRefreshPolicy(refreshPolicy);

            if (bulkRequest.numberOfActions() != 0) {
                BulkResponse bulkResponse;
                try {
                    bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
                } catch (IOException e) {
                    throw new DatabaseException("Bulk delete exception", e);
                }

                if (bulkResponse.hasFailures()) {
                    throw new DatabaseException("ES delete in error: " + bulkResponse.buildFailureMessage());
                }
            }
        }
    }
}
