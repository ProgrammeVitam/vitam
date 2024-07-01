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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.server.mongodb.BsonHelper;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.exception.VitamFatalRuntimeException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.application.configuration.DatabaseConnection;
import org.apache.commons.collections.CollectionUtils;
import org.apache.http.HttpHost;
import org.apache.http.util.Asserts;
import org.bson.Document;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.LatchedActionListener;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.indices.alias.Alias;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest.AliasActions;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshResponse;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.ClearScrollResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.support.ActiveShardCount;
import org.elasticsearch.action.support.DefaultShardOperationFailedException;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.GetAliasesResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.cluster.metadata.AliasMetadata;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.index.reindex.ScrollableHitSource;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.xcontent.XContentType;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;

/**
 * Elasticsearch Access
 */
public class ElasticsearchAccess implements DatabaseConnection {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ElasticsearchAccess.class);
    /**
     * default limit scroll timeout
     */
    public static final int DEFAULT_SCROLL_TIMEOUT = 60000;
    /**
     * default limit scroll size
     */
    public static final int DEFAULT_LIMIT_SCROLL = 100;

    /**
     * KEYWORD to activate scroll
     */
    public static final String SCROLL_ACTIVATE_KEYWORD = "START";
    private static final String NUMBER_OF_SHARDS = "number_of_shards";
    private static final String NUMBER_OF_REPLICAS = "number_of_replicas";

    private static final String ES_CONFIGURATION_FILE = "/elasticsearch-configuration.json";
    private final AtomicReference<RestHighLevelClient> esClient = new AtomicReference<>();
    protected final String clusterName;
    protected final List<ElasticsearchNode> nodes;

    /**
     * Create an ElasticSearch access
     *
     * @param clusterName the name of the Cluster
     * @param nodes the elasticsearch nodes
     * @throws VitamException when elasticseach node list is empty
     */
    public ElasticsearchAccess(final String clusterName, List<ElasticsearchNode> nodes) throws VitamException {
        ParametersChecker.checkParameter(
            "clusterName, elasticsearch nodes list are a mandatory parameters",
            clusterName,
            nodes
        );

        if (nodes.isEmpty()) {
            throw new VitamException("elasticsearch nodes list is empty");
        }

        this.clusterName = clusterName;
        this.nodes = nodes;
    }

    public final GetAliasesResponse getAlias(ElasticsearchIndexAlias indexAlias) throws IOException {
        GetAliasesRequest request = new GetAliasesRequest(indexAlias.getName());
        request.indicesOptions(IndicesOptions.STRICT_EXPAND_OPEN);
        return getClient().indices().getAlias(request, RequestOptions.DEFAULT);
    }

    public final ElasticsearchIndexAlias createIndexWithoutAlias(
        ElasticsearchIndexAlias indexAlias,
        ElasticsearchIndexSettings indexSettings
    ) throws DatabaseException {
        ElasticsearchIndexAlias newIndexName = indexAlias.createUniqueIndexName();
        createIndexWithOptionalAlias(
            null,
            newIndexName.getName(),
            indexSettings.loadMapping(),
            indexSettings.getShards(),
            indexSettings.getReplicas()
        );
        return newIndexName;
    }

    private void createIndexWithOptionalAlias(
        String aliasName,
        String indexName,
        String mapping,
        Integer shards,
        Integer replicas
    ) throws DatabaseException {
        try {
            CreateIndexRequest request = new CreateIndexRequest(indexName)
                .settings(createIndexSettings(shards, replicas))
                .mapping(mapping, XContentType.JSON);

            if (aliasName != null) {
                request.alias(new Alias(aliasName));
            }

            request.setTimeout(
                TimeValue.timeValueMillis(VitamConfiguration.getElasticSearchTimeoutWaitRequestInMilliseconds())
            );
            request.setMasterTimeout(
                TimeValue.timeValueMillis(VitamConfiguration.getElasticSearchTimeoutWaitRequestInMilliseconds())
            );
            request.waitForActiveShards(ActiveShardCount.DEFAULT);

            VitamElasticListener listener = new VitamElasticListener();
            final CountDownLatch latch = new CountDownLatch(1);
            LOGGER.debug("async request to create index [" + indexName + "]");

            getClient()
                .indices()
                .createAsync(request, RequestOptions.DEFAULT, new LatchedActionListener<>(listener, latch));

            try {
                LOGGER.debug("request sent -> waiting for a response now");
                latch.await();
            } catch (InterruptedException e) {
                throw new IOException(e);
            }

            Optional<Exception> hasException = Optional.ofNullable(listener.getException());
            if (hasException.isPresent()) {
                throw new IOException(hasException.get());
            }

            if (!listener.isAcknowledged() || !listener.isShardsAcknowledged()) {
                throw new DatabaseException(
                    "Could not create index " +
                    indexName +
                    ". acknowledged: " +
                    listener.isAcknowledged() +
                    ", shardsAcknowledged: " +
                    listener.isShardsAcknowledged()
                );
            }
        } catch (IOException e) {
            throw new DatabaseException("Could not create index " + indexName, e);
        }
    }

    public final boolean existsAlias(ElasticsearchIndexAlias indexAlias) throws DatabaseException {
        try {
            GetAliasesRequest request = new GetAliasesRequest(indexAlias.getName());
            request.local(true);
            request.indicesOptions(IndicesOptions.STRICT_EXPAND_OPEN);
            return getClient().indices().existsAlias(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new DatabaseException("Could not check alias existence " + indexAlias.getName(), e);
        }
    }

    public final boolean existsIndex(ElasticsearchIndexAlias index) throws DatabaseException {
        try {
            GetIndexRequest request = new GetIndexRequest(index.getName());
            request.local(true);
            request.indicesOptions(IndicesOptions.STRICT_EXPAND_OPEN);
            return getClient().indices().exists(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new DatabaseException("Could not check index existence " + index.getName(), e);
        }
    }

    public final void refreshIndex(ElasticsearchIndexAlias indexAlias) throws DatabaseException {
        LOGGER.debug("refreshIndex: " + indexAlias.getName());

        RefreshRequest request = new RefreshRequest(indexAlias.getName());
        request.indicesOptions(IndicesOptions.STRICT_EXPAND_OPEN);
        RefreshResponse refreshResponse;
        try {
            refreshResponse = getClient().indices().refresh(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new DatabaseException("Could not refresh index ", e);
        }
        LOGGER.debug("Refresh request executed with {} successful shards", refreshResponse.getSuccessfulShards());

        int failedShards = refreshResponse.getFailedShards();
        if (failedShards > 0) {
            DefaultShardOperationFailedException[] failures = refreshResponse.getShardFailures();
            StringBuilder sb = new StringBuilder();
            for (DefaultShardOperationFailedException failure : failures) {
                sb.append(failure.toString()).append("; ");
            }
            throw new DatabaseException(sb.toString());
        }
    }

    @VisibleForTesting
    protected void purgeIndexForTesting(ElasticsearchIndexAlias indexAlias, Integer tenantId) throws DatabaseException {
        TermQueryBuilder query = QueryBuilders.termQuery(VitamDocument.TENANT_ID, tenantId);
        purgeIndexForTesting(indexAlias, query);
    }

    @VisibleForTesting
    public final void purgeIndexForTesting(ElasticsearchIndexAlias indexAlias) throws DatabaseException {
        MatchAllQueryBuilder query = matchAllQuery();
        purgeIndexForTesting(indexAlias, query);
    }

    private void purgeIndexForTesting(ElasticsearchIndexAlias indexAlias, QueryBuilder query) throws DatabaseException {
        try {
            if (existsAlias(indexAlias)) {
                DeleteByQueryRequest request = new DeleteByQueryRequest(indexAlias.getName());
                request.setConflicts("proceed");
                request.setQuery(query);
                request.setBatchSize(VitamConfiguration.getMaxElasticsearchBulk());
                request.setScroll(
                    TimeValue.timeValueMillis(VitamConfiguration.getElasticSearchScrollTimeoutInMilliseconds())
                );
                request.setTimeout(
                    TimeValue.timeValueMillis(VitamConfiguration.getElasticSearchTimeoutWaitRequestInMilliseconds())
                );
                request.setRefresh(true);

                BulkByScrollResponse bulkResponse = getClient().deleteByQuery(request, RequestOptions.DEFAULT);

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
                    "Purge alias (" +
                    indexAlias.getName() +
                    ") : timeTaken (" +
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
                    throw new DatabaseException("ES purge errors : in search phase > " + searchFailures);
                }

                List<BulkItemResponse.Failure> bulkFailures = bulkResponse.getBulkFailures();
                if (CollectionUtils.isNotEmpty(bulkFailures)) {
                    throw new DatabaseException("ES purge errors : in bulk phase > " + bulkFailures);
                }
            }
        } catch (IOException e) {
            throw new DatabaseException(e);
        }
    }

    public final <T> void indexEntry(
        ElasticsearchIndexAlias indexAlias,
        final String id,
        final VitamDocument<T> vitamDocument
    ) throws DatabaseException {
        vitamDocument.remove(VitamDocument.ID);
        vitamDocument.remove(VitamDocument.SCORE);

        String source = BsonHelper.stringify(vitamDocument);

        IndexRequest request = new IndexRequest(indexAlias.getName())
            .id(id)
            .source(source, XContentType.JSON)
            .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE)
            .timeout(TimeValue.timeValueSeconds(VitamConfiguration.getElasticSearchTimeoutWaitRequestInMilliseconds()))
            .opType(DocWriteRequest.OpType.INDEX);

        IndexResponse indexResponse;
        try {
            indexResponse = getClient().index(request, RequestOptions.DEFAULT);
        } catch (IOException | ElasticsearchException e) {
            throw new DatabaseException(e);
        } finally {
            vitamDocument.put(VitamDocument.ID, id);
        }

        if (indexResponse.getResult() != DocWriteResponse.Result.CREATED) {
            throw new DatabaseException(
                String.format(
                    "Could not index document on ES. Id=%s, aliasName=%s, status=%s",
                    id,
                    indexAlias.getName(),
                    indexResponse.status()
                )
            );
        }
    }

    public void indexEntries(ElasticsearchIndexAlias indexAlias, final Collection<? extends Document> documents)
        throws DatabaseException {
        UnmodifiableIterator<? extends List<? extends Document>> idIterator = Iterators.partition(
            documents.iterator(),
            VitamConfiguration.getMaxElasticsearchBulk()
        );

        while (idIterator.hasNext()) {
            BulkRequest bulkRequest = new BulkRequest();
            List<? extends Document> docs = idIterator.next();
            docs.forEach(document -> {
                String id = (String) document.remove(VitamDocument.ID);
                try {
                    String source = BsonHelper.stringify(document);
                    bulkRequest.add(
                        new IndexRequest(indexAlias.getName())
                            .id(id)
                            .opType(DocWriteRequest.OpType.INDEX)
                            .source(source, XContentType.JSON)
                    );
                } finally {
                    document.put(VitamDocument.ID, id);
                }
            });

            if (bulkRequest.numberOfActions() != 0) {
                final BulkResponse bulkResponse;
                try {
                    bulkRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
                    bulkResponse = getClient().bulk(bulkRequest, RequestOptions.DEFAULT);
                } catch (IOException | ElasticsearchException e) {
                    throw new DatabaseException(e);
                }

                LOGGER.debug("Written document {}", bulkResponse.getItems().length);
                if (bulkResponse.hasFailures()) {
                    throw new DatabaseException(
                        "Bulk Request failure with error: " + bulkResponse.buildFailureMessage()
                    );
                }
            }
        }
    }

    /**
     * Update one element fully
     */
    public <T> void updateEntry(ElasticsearchIndexAlias indexAlias, String id, VitamDocument<T> vitamDocument)
        throws DatabaseException {
        try {
            vitamDocument.remove(VitamDocument.ID);
            final String document = BsonHelper.stringify(vitamDocument);

            IndexRequest request = new IndexRequest(indexAlias.getName())
                .id(id)
                .source(document, XContentType.JSON)
                .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE)
                .timeout(
                    TimeValue.timeValueMillis(VitamConfiguration.getElasticSearchTimeoutWaitRequestInMilliseconds())
                )
                .opType(DocWriteRequest.OpType.INDEX);

            IndexResponse indexResponse;
            try {
                indexResponse = getClient().index(request, RequestOptions.DEFAULT);
            } catch (IOException | ElasticsearchException e) {
                throw new DatabaseException(e);
            }

            if (indexResponse.getResult() != DocWriteResponse.Result.UPDATED) {
                throw new DatabaseException(
                    String.format(
                        "Could not update document on ES. Id=%s, aliasName=%s, status=%s",
                        id,
                        indexAlias.getName(),
                        indexResponse.status()
                    )
                );
            }
        } finally {
            vitamDocument.put(VitamDocument.ID, id);
        }
    }

    public final SearchResponse search(
        ElasticsearchIndexAlias indexAlias,
        final QueryBuilder query,
        final QueryBuilder filter,
        String[] esProjection,
        final List<SortBuilder<?>> sorts,
        int offset,
        Integer limit
    ) throws DatabaseException, BadRequestException {
        return search(indexAlias, query, filter, esProjection, sorts, offset, limit, null, null, null, false);
    }

    public final SearchResponse searchCrossIndices(
        Set<ElasticsearchIndexAlias> indexAliases,
        final QueryBuilder query,
        final QueryBuilder filter,
        String[] esProjection,
        final List<SortBuilder<?>> sorts,
        int offset,
        Integer limit,
        final List<AggregationBuilder> facets,
        final String scrollId,
        final Integer scrollTimeout,
        boolean trackTotalHits
    ) throws DatabaseException, BadRequestException {
        SearchResponse response;
        SearchSourceBuilder searchSourceBuilder = SearchSourceBuilder.searchSource()
            .explain(false)
            .query(query)
            .size(VitamConfiguration.getElasticSearchScrollLimit());

        if (trackTotalHits) {
            // Enable trackTotalHits flag to compute total result set count (not 10000 since ES 7.0)
            // Warning, only call trackTotalHits setter only for "true". Setting "false" causes $hits to not be computed
            searchSourceBuilder.trackTotalHits(true);
        }

        if (null != filter) {
            searchSourceBuilder.postFilter(filter);
        }

        if (sorts != null) {
            sorts.forEach(searchSourceBuilder::sort);
        }

        if (null != esProjection) {
            searchSourceBuilder.fetchSource(esProjection, null);
        }

        SearchRequest searchRequest = new SearchRequest()
            .indices(indexAliases.stream().map(ElasticsearchIndexAlias::getName).toArray(String[]::new))
            .source(searchSourceBuilder)
            .searchType(SearchType.DFS_QUERY_THEN_FETCH);

        if (scrollId != null && !scrollId.isEmpty()) {
            int limitES = (limit != null && limit > 0) ? limit : DEFAULT_LIMIT_SCROLL;
            int scrollTimeoutES = (scrollTimeout != null && scrollTimeout > 0) ? scrollTimeout : DEFAULT_SCROLL_TIMEOUT;
            searchRequest.scroll(TimeValue.timeValueMillis(scrollTimeoutES));
            searchSourceBuilder.size(limitES);

            try {
                if (scrollId.equals(SCROLL_ACTIVATE_KEYWORD)) {
                    response = getClient().search(searchRequest, RequestOptions.DEFAULT);
                } else {
                    response = getClient()
                        .scroll(
                            new SearchScrollRequest(scrollId).scroll(new TimeValue(scrollTimeoutES)),
                            RequestOptions.DEFAULT
                        );
                }
            } catch (IOException e) {
                throw new DatabaseException(e);
            }
        } else {
            if (offset != -1) {
                searchSourceBuilder.from(offset);
            }
            if (limit != -1) {
                searchSourceBuilder.size(limit);
            }

            if (facets != null && !facets.isEmpty()) {
                facets.forEach(searchSourceBuilder::aggregation);
            }

            searchSourceBuilder.query(query);

            LOGGER.debug("ESReq: {}", searchRequest);

            try {
                response = getClient().search(searchRequest, RequestOptions.DEFAULT);
            } catch (final ElasticsearchException e) {
                if (e.status() == RestStatus.BAD_REQUEST) {
                    throw new BadRequestException(e);
                }
                throw new DatabaseException(e);
            } catch (IOException e) {
                throw new DatabaseException(e);
            }
        }

        if (response.status() != RestStatus.OK) {
            throw new DatabaseException("Error " + response.status() + " from : " + searchRequest + ":" + query);
        }

        return response;
    }

    public final SearchResponse search(
        ElasticsearchIndexAlias indexAlias,
        final QueryBuilder query,
        final QueryBuilder filter,
        String[] esProjection,
        final List<SortBuilder<?>> sorts,
        int offset,
        Integer limit,
        final List<AggregationBuilder> facets,
        final String scrollId,
        final Integer scrollTimeout,
        boolean trackTotalHits
    ) throws DatabaseException, BadRequestException {
        return searchCrossIndices(
            Set.of(indexAlias),
            query,
            filter,
            esProjection,
            sorts,
            offset,
            limit,
            facets,
            scrollId,
            scrollTimeout,
            trackTotalHits
        );
    }

    public void clearScroll(String scrollId) throws DatabaseException {
        ClearScrollRequest request = new ClearScrollRequest();
        request.addScrollId(scrollId);
        try {
            ClearScrollResponse response = getClient().clearScroll(request, RequestOptions.DEFAULT);

            boolean success = response.isSucceeded();
            int released = response.getNumFreed();

            Asserts.check(success, "clear scroll" + scrollId + " ko");
            LOGGER.debug("clear scroll " + scrollId + " > success :" + success + ", released: " + released);
        } catch (IOException e) {
            throw new DatabaseException(e);
        }
    }

    private RestHighLevelClient createClient() {
        HttpHost[] hosts = new HttpHost[nodes.size()];
        for (int i = 0; i < nodes.size(); i++) {
            ElasticsearchNode elasticsearchNode = nodes.get(i);
            hosts[i] = new HttpHost(elasticsearchNode.getHostName(), elasticsearchNode.getHttpPort(), "http");
        }
        RestClientBuilder restClientBuilder = RestClient.builder(hosts).setRequestConfigCallback(
            requestConfigBuilder ->
                requestConfigBuilder
                    .setConnectionRequestTimeout(VitamConfiguration.getElasticSearchTimeoutWaitRequestInMilliseconds())
                    .setConnectTimeout(VitamConfiguration.getConnectTimeout())
                    .setSocketTimeout(VitamConfiguration.getReadTimeout())
        );
        return new RestHighLevelClient(restClientBuilder);
    }

    /**
     * Close the ElasticSearch connection
     */
    public void close() {
        try {
            getClient().close();
        } catch (IOException e) {
            throw new VitamFatalRuntimeException(e);
        }
    }

    /**
     * @return the Cluster Name
     */
    public String getClusterName() {
        return clusterName;
    }

    /**
     * @return the client
     */
    public RestHighLevelClient getClient() {
        RestHighLevelClient client = esClient.get();
        if (null == client) {
            synchronized (this) {
                if (null == esClient.get()) {
                    client = createClient();
                    esClient.set(client);
                }
            }
        }
        return client;
    }

    /**
     * @return the nodes
     */
    public List<ElasticsearchNode> getNodes() {
        return nodes;
    }

    @Override
    public boolean checkConnection() {
        try (RestHighLevelClient clientCheck = createClient()) {
            return !clientCheck.cluster().health(new ClusterHealthRequest(), RequestOptions.DEFAULT).isTimedOut();
        } catch (final Exception e) {
            LOGGER.warn(e);
            return false;
        }
    }

    @Override
    public String getInfo() {
        return clusterName;
    }

    public final void createIndexAndAliasIfAliasNotExists(
        ElasticsearchIndexAlias indexAlias,
        ElasticsearchIndexSettings indexSettings
    ) throws DatabaseException {
        LOGGER.debug("createIndexAndAliasIfAliasNotExists: {}", indexAlias.getName());

        if (existsAlias(indexAlias)) {
            return;
        }

        ElasticsearchIndexAlias indexName = indexAlias.createUniqueIndexName();

        createIndexWithOptionalAlias(
            indexAlias.getName(),
            indexName.getName(),
            indexSettings.loadMapping(),
            indexSettings.getShards(),
            indexSettings.getReplicas()
        );
    }

    public final void switchIndex(ElasticsearchIndexAlias indexAlias, ElasticsearchIndexAlias indexNameToSwitchTo)
        throws DatabaseException, IOException {
        if (!existsAlias(indexAlias)) {
            throw new DatabaseException(String.format("Alias does not exist : %s", indexAlias.getName()));
        }

        if (!existsIndex(indexNameToSwitchTo)) {
            throw new DatabaseException(String.format("New index does not exist : %s", indexNameToSwitchTo.getName()));
        }

        GetAliasesResponse actualIndex = getClient()
            .indices()
            .getAlias(new GetAliasesRequest(indexAlias.getName()), RequestOptions.DEFAULT);

        Map<String, Set<AliasMetadata>> aliases = actualIndex.getAliases();

        String oldIndexName = null;

        if (!aliases.isEmpty()) {
            oldIndexName = aliases.keySet().iterator().next();
        }

        LOGGER.debug("Alias (" + indexAlias.getName() + ") map to index (" + oldIndexName + ")");

        IndicesAliasesRequest request = new IndicesAliasesRequest();
        AliasActions addNewIndexAliasAction = new AliasActions(AliasActions.Type.ADD)
            .index(indexNameToSwitchTo.getName())
            .alias(indexAlias.getName());
        request.addAliasAction(addNewIndexAliasAction);

        AliasActions deleteOldIndexAliasAction = new AliasActions(AliasActions.Type.REMOVE)
            .index(oldIndexName)
            .alias(indexAlias.getName());
        request.addAliasAction(deleteOldIndexAliasAction);

        request.timeout(
            TimeValue.timeValueMillis(VitamConfiguration.getElasticSearchTimeoutWaitRequestInMilliseconds())
        );
        request.masterNodeTimeout(
            TimeValue.timeValueMillis(VitamConfiguration.getElasticSearchTimeoutWaitRequestInMilliseconds())
        );

        AcknowledgedResponse response = getClient().indices().updateAliases(request, RequestOptions.DEFAULT);

        if (!response.isAcknowledged()) {
            throw new DatabaseException(
                "Could not switch alias index " +
                indexAlias.getName() +
                " from " +
                oldIndexName +
                " to " +
                indexNameToSwitchTo
            );
        }
    }

    @VisibleForTesting
    public final void deleteIndexByAliasForTesting(ElasticsearchIndexAlias indexAlias) throws DatabaseException {
        GetAliasesResponse aliasResponse;
        try {
            aliasResponse = getAlias(indexAlias);
        } catch (IOException e) {
            throw new DatabaseException(e);
        }
        for (Map.Entry<String, Set<AliasMetadata>> entry : aliasResponse.getAliases().entrySet()) {
            deleteIndexForTesting(entry.getKey());
        }
    }

    @VisibleForTesting
    public final void deleteIndexForTesting(ElasticsearchIndexAlias indexAlias) throws DatabaseException {
        deleteIndexForTesting(indexAlias.getName());
    }

    private void deleteIndexForTesting(final String indexFullName) throws DatabaseException {
        DeleteIndexRequest request = new DeleteIndexRequest(indexFullName);
        request.timeout(
            TimeValue.timeValueMillis(VitamConfiguration.getElasticSearchTimeoutWaitRequestInMilliseconds())
        );
        request.masterNodeTimeout(
            TimeValue.timeValueMillis(VitamConfiguration.getElasticSearchTimeoutWaitRequestInMilliseconds())
        );
        request.indicesOptions(IndicesOptions.STRICT_EXPAND_OPEN);

        try {
            AcknowledgedResponse deleteIndexResponse = getClient().indices().delete(request, RequestOptions.DEFAULT);

            if (!deleteIndexResponse.isAcknowledged()) {
                throw new DatabaseException("Error while deleting index " + indexFullName + ". Not acknowledged");
            }
        } catch (Exception exception) {
            if (
                exception instanceof ElasticsearchException &&
                ((ElasticsearchException) exception).status() == RestStatus.NOT_FOUND
            ) {
                //Nothing to do
                return;
            }
            throw new DatabaseException("Error while deleting index " + indexFullName, exception);
        }
    }

    public void delete(ElasticsearchIndexAlias indexAlias, List<String> ids) throws DatabaseException {
        Iterator<List<String>> idIterator = Iterators.partition(
            ids.iterator(),
            VitamConfiguration.getMaxElasticsearchBulk()
        );

        while (idIterator.hasNext()) {
            BulkRequest bulkRequest = new BulkRequest();
            for (String id : idIterator.next()) {
                bulkRequest.add(new DeleteRequest(indexAlias.getName(), id));
            }

            WriteRequest.RefreshPolicy refreshPolicy = idIterator.hasNext()
                ? WriteRequest.RefreshPolicy.NONE
                : WriteRequest.RefreshPolicy.IMMEDIATE;

            bulkRequest.setRefreshPolicy(refreshPolicy);

            if (bulkRequest.numberOfActions() != 0) {
                BulkResponse bulkResponse;
                try {
                    bulkResponse = getClient().bulk(bulkRequest, RequestOptions.DEFAULT);
                } catch (IOException e) {
                    throw new DatabaseException("Bulk delete exception", e);
                }

                if (bulkResponse.hasFailures()) {
                    throw new DatabaseException("ES delete in error: " + bulkResponse.buildFailureMessage());
                }
            }
        }
    }

    private Settings createIndexSettings(int shards, int replicas) throws IOException {
        return Settings.builder()
            .loadFromStream(
                ES_CONFIGURATION_FILE,
                ElasticsearchAccess.class.getResourceAsStream(ES_CONFIGURATION_FILE),
                false
            )
            .put(NUMBER_OF_SHARDS, shards)
            .put(NUMBER_OF_REPLICAS, replicas)
            .build();
    }
}
