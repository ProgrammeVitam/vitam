/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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

import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.request.configuration.GlobalDatas;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.json.BsonHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.application.configuration.DatabaseConnection;
import org.apache.commons.collections.CollectionUtils;
import org.apache.http.HttpHost;
import org.apache.http.util.Asserts;
import org.bson.Document;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.DocWriteRequest;
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
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.Settings.Builder;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.index.reindex.ScrollableHitSource;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    /**
     * The ES Builder
     */
    private Builder default_builder;

    private static String ES_CONFIGURATION_FILE = "/elasticsearch-configuration.json";
    private AtomicReference<RestHighLevelClient> esClient = new AtomicReference<>();
    protected final String clusterName;
    protected final List<ElasticsearchNode> nodes;

    /**
     * Create an ElasticSearch access
     *
     * @param clusterName the name of the Cluster
     * @param nodes the elasticsearch nodes
     * @throws VitamException when elasticseach node list is empty
     */
    public ElasticsearchAccess(final String clusterName, List<ElasticsearchNode> nodes)
        throws VitamException, IOException {

        ParametersChecker.checkParameter("clusterName, elasticsearch nodes list are a mandatory parameters",
            clusterName, nodes);

        if (nodes.isEmpty()) {
            throw new VitamException("elasticsearch nodes list is empty");
        }

        this.clusterName = clusterName;
        this.nodes = nodes;

        default_builder = settings();
    }

    public void purgeIndex(String collectionName, Integer tenant) {
        String indexName = collectionName.toLowerCase() + "_" + tenant;
        purgeIndex(indexName);
    }

    public boolean existsIndex(String indexName) throws IOException {
        GetIndexRequest request = new GetIndexRequest(indexName.toLowerCase());
        request.humanReadable(true);
        request.includeDefaults(false);
        request.indicesOptions(IndicesOptions.STRICT_EXPAND_OPEN);
        return getClient().indices().exists(request, RequestOptions.DEFAULT);
    }

    public GetAliasesResponse getAlias(String alias) throws IOException {
        GetAliasesRequest request = new GetAliasesRequest(alias.toLowerCase());
        request.indicesOptions(IndicesOptions.STRICT_EXPAND_OPEN);
        return getClient().indices().getAlias(request, RequestOptions.DEFAULT);
    }


    public boolean createIndex(String aliasName, String indexName, String mapping) throws IOException {

        boolean existsIndex = existsIndex(indexName);

        if (Boolean.TRUE.equals(existsIndex)) {
            LOGGER.debug("Index (" + existsIndex + ") already exists");
            return true;
        }

        CreateIndexRequest request = new CreateIndexRequest(indexName)
            .settings(default_builder)
            .mapping(mapping, XContentType.JSON)
            .alias(new Alias(aliasName));

        request.setTimeout(TimeValue.timeValueMillis(
            VitamConfiguration.getElasticSearchTimeoutWaitAvailableShardsForBulkRequestInMilliseconds()));
        request.setMasterTimeout(TimeValue.timeValueMinutes(1));
        request.waitForActiveShards(ActiveShardCount.DEFAULT);

        CreateIndexResponse response =
            getClient().indices().create(request, RequestOptions.DEFAULT);

        boolean acknowledged = response.isAcknowledged();
        boolean shardsAcknowledged = response.isShardsAcknowledged();

        LOGGER.debug(
            "Alias (" + aliasName + ") and index (" + indexName + ") create response acknowledged (" + acknowledged +
                ") and shardsAcknowledged (" + shardsAcknowledged + ") ");

        return acknowledged && shardsAcknowledged;
    }

    public boolean createIndex(String indexName, String mapping) throws IOException {

        boolean existsIndex = existsIndex(indexName);

        if (Boolean.TRUE.equals(existsIndex)) {
            LOGGER.debug("Index (" + existsIndex + ") already exists");
            return true;
        }

        CreateIndexRequest request = new CreateIndexRequest(indexName)
            .settings(default_builder)
            .mapping(mapping, XContentType.JSON);

        request.setTimeout(TimeValue.timeValueMillis(
            VitamConfiguration.getElasticSearchTimeoutWaitAvailableShardsForBulkRequestInMilliseconds()));
        request.setMasterTimeout(TimeValue.timeValueMinutes(1));
        request.waitForActiveShards(ActiveShardCount.DEFAULT);

        CreateIndexResponse response =
            getClient().indices().create(request, RequestOptions.DEFAULT);

        boolean acknowledged = response.isAcknowledged();
        boolean shardsAcknowledged = response.isShardsAcknowledged();

        LOGGER.debug(
            "Index (" + indexName + ") create response : acknowledged (" + acknowledged +
                ") and shardsAcknowledged (" + shardsAcknowledged + ") ");

        return acknowledged && shardsAcknowledged;
    }



    public boolean existsAlias(String aliasName) throws IOException {
        GetAliasesRequest request = new GetAliasesRequest(aliasName.toLowerCase());
        request.local(true);
        request.indicesOptions(IndicesOptions.STRICT_EXPAND_OPEN);
        return getClient().indices().existsAlias(request, RequestOptions.DEFAULT);
    }

    public boolean aliasingIndex(String aliasName, String indexName) throws IOException {

        boolean existsAlias = existsAlias(aliasName);
        if (Boolean.FALSE.equals(existsAlias)) {
            String msg = "The alias (" + aliasName + ") must exists";
            LOGGER.error(msg);
            throw new RuntimeException(msg);
        }

        boolean existsIndex = existsIndex(indexName);
        if (Boolean.FALSE.equals(existsIndex)) {
            String msg = "The index (" + indexName + ") must exists";
            LOGGER.error(msg);
            throw new RuntimeException(msg);
        }

        IndicesAliasesRequest request = new IndicesAliasesRequest();
        AliasActions aliasAction =
            new AliasActions(AliasActions.Type.ADD)
                .index(indexName.toLowerCase())
                .alias(aliasName.toLowerCase());
        request.addAliasAction(aliasAction);

        //TODO conf timeout
        request.timeout(TimeValue.timeValueMinutes(2));
        request.masterNodeTimeout(TimeValue.timeValueMinutes(1));

        AcknowledgedResponse response = getClient().indices().updateAliases(request, RequestOptions.DEFAULT);

        return response.isAcknowledged();
    }


    public boolean switchAliasIndex(String aliasName, String oldIndex, String newIndexName) throws IOException {
        IndicesAliasesRequest request = new IndicesAliasesRequest();
        AliasActions aliasAction =
            new AliasActions(AliasActions.Type.ADD)
                .index(newIndexName.toLowerCase())
                .alias(aliasName.toLowerCase());
        request.addAliasAction(aliasAction);

        if (null != oldIndex) {
            aliasAction =
                new AliasActions(AliasActions.Type.REMOVE)
                    .index(oldIndex.toLowerCase())
                    .alias(aliasName.toLowerCase());
            request.addAliasAction(aliasAction);
        }

        //TODO conf timeout
        request.timeout(TimeValue.timeValueMinutes(2));
        request.masterNodeTimeout(TimeValue.timeValueMinutes(1));

        AcknowledgedResponse response = getClient().indices().updateAliases(request, RequestOptions.DEFAULT);

        return response.isAcknowledged();
    }

    public void purgeIndex(String indexName) {
        try {
            String vitamIndexName = indexName.toLowerCase();
            if (existsIndex(vitamIndexName)) {
                DeleteByQueryRequest request = new DeleteByQueryRequest(vitamIndexName);
                request.setConflicts("proceed");
                request.setQuery(matchAllQuery());
                request.setBatchSize(VitamConfiguration.getMaxElasticsearchBulk());
                request.setScroll(TimeValue.timeValueMillis(
                    VitamConfiguration.getElasticSearchScrollTimeoutInMilliseconds()));
                request.setTimeout(TimeValue.timeValueMillis(
                    VitamConfiguration.getElasticSearchTimeoutWaitAvailableShardsForBulkRequestInMilliseconds()));
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
                TimeValue throttledUntilMillis =
                    bulkResponse.getStatus().getThrottledUntil();

                LOGGER.debug(
                    "Purge index (" + indexName + ") : timeTaken (" + timeTaken + "), timedOut (" + timedOut +
                        "), totalDocs (" + totalDocs +
                        ")," +
                        " deletedDocs (" + deletedDocs + "), batches (" + batches + "), noops (" + noops +
                        "), versionConflicts (" + versionConflicts + ")" +
                        "bulkRetries (" + bulkRetries + "), searchRetries (" + searchRetries + "),  throttledMillis(" +
                        throttledMillis + "), throttledUntilMillis(" + throttledUntilMillis + ")");

                List<ScrollableHitSource.SearchFailure> searchFailures = bulkResponse.getSearchFailures();
                if (CollectionUtils.isNotEmpty(searchFailures)) {
                    throw new VitamRuntimeException("ES purge errors : in search phase > " + searchFailures);
                }

                List<BulkItemResponse.Failure> bulkFailures = bulkResponse.getBulkFailures();
                if (CollectionUtils.isNotEmpty(bulkFailures)) {
                    throw new VitamRuntimeException("ES purge errors : in bulk phase > " + bulkFailures);
                }

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * refresh an index
     *
     * @param collectionName the working metadata collection name
     * @param tenantId the tenant for operation
     */
    public final void refreshIndex(final String collectionName, Integer tenantId)
        throws IOException, DatabaseException {
        String index = getAliasName(collectionName, tenantId);
        LOGGER.debug("refreshIndex: " + index);

        RefreshRequest request = new RefreshRequest(index);
        request.indicesOptions(IndicesOptions.STRICT_EXPAND_OPEN);
        RefreshResponse refreshResponse = getClient().indices().refresh(request, RequestOptions.DEFAULT);
        LOGGER.debug("Refresh request executed with {} successfull shards", refreshResponse.getSuccessfulShards());

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


    /**
     * Add an entry in the ElasticSearch index
     *
     * @param collectionName
     * @param tenantId
     * @param id
     * @param vitamDocument
     * @return True if ok
     */
    public final <T extends VitamDocument> void indexEntry(final String collectionName, final Integer tenantId,
        final String id,
        final T vitamDocument) throws DatabaseException {

        vitamDocument.remove(VitamDocument.ID);
        vitamDocument.remove(VitamDocument.SCORE);

        String source = BsonHelper.stringify(vitamDocument);

        IndexRequest request = new IndexRequest(getAliasName(collectionName, tenantId))
            .id(id)
            .source(source, XContentType.JSON)
            .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE)
            .timeout(TimeValue.timeValueSeconds(1)) // TODO: 03/02/2020 config
            .opType(DocWriteRequest.OpType.INDEX);

        IndexResponse indexResponse;
        try {
            indexResponse = getClient().index(request, RequestOptions.DEFAULT);
        } catch (IOException | ElasticsearchException e) {
            throw new DatabaseException(e);
        } finally {
            vitamDocument.put(VitamDocument.ID, id);
        }

        switch (indexResponse.getResult()) {
            case CREATED:
                break;
            default:
                throw new DatabaseException(String
                    .format("Could not index document on ES. Id=%s, collection=%s, status=%s", id, collectionName,
                        indexResponse.status()));
        }
    }

    /**
     * Add a set of entries in the ElasticSearch index. <br>
     * Used in reload from scratch.
     *
     * @param collectionName collection of index
     * @param tenantId tenant Id
     * @param documents documents to index
     * @return the listener on bulk insert
     */
    final public void indexEntries(final String collectionName, final Integer tenantId,
        final Collection<? extends Document> documents) throws DatabaseException {

        UnmodifiableIterator<? extends List<? extends Document>> idIterator =
            Iterators.partition(documents.iterator(), VitamConfiguration.getMaxElasticsearchBulk());

        while (idIterator.hasNext()) {
            BulkRequest bulkRequest = new BulkRequest();
            List<? extends Document> docs = idIterator.next();
            docs.forEach(document -> {
                String id = (String) document.remove(VitamDocument.ID);
                try {
                    String source = BsonHelper.stringify(document);
                    bulkRequest.add(new IndexRequest(getAliasName(collectionName, tenantId))
                        .id(id)
                        .opType(DocWriteRequest.OpType.INDEX)
                        .source(source, XContentType.JSON));
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
                    LOGGER.error("##### Bulk Request failure with error: " + bulkResponse.buildFailureMessage());
                    throw new DatabaseException(bulkResponse.buildFailureMessage());
                }
            }
        }
    }

    /**
     * Update one element fully
     *
     * @param collectionName
     * @param tenantId
     * @param id
     * @param vitamDocument full document to update
     */
    public <T extends VitamDocument> void updateEntry(String collectionName, Integer tenantId, String id,
        T vitamDocument)
        throws DatabaseException {
        try {
            vitamDocument.remove(VitamDocument.ID);
            final String document = BsonHelper.stringify(vitamDocument);

            IndexRequest request = new IndexRequest(getAliasName(collectionName, tenantId))
                .id(id)
                .source(document, XContentType.JSON)
                .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE)
                .timeout(TimeValue.timeValueSeconds(1)) // TODO: 03/02/2020 config
                .opType(DocWriteRequest.OpType.INDEX);

            IndexResponse indexResponse;
            try {
                indexResponse = getClient().index(request, RequestOptions.DEFAULT);
            } catch (IOException | ElasticsearchException e) {
                throw new DatabaseException(e);
            }

            switch (indexResponse.getResult()) {
                case UPDATED:
                    break;
                default:
                    throw new DatabaseException(String
                        .format("Could not update document on ES. Id=%s, collection=%s, status=%s", id, collectionName,
                            indexResponse.status()));
            }

        } finally {
            vitamDocument.put(VitamDocument.ID, id);
        }
    }

    public final SearchResponse search(final String collectionName, final Integer tenantId,
        final QueryBuilder query)
        throws DatabaseException, BadRequestException {
        return search(collectionName, tenantId, query, null);
    }

    public final SearchResponse search(final String collectionName, final Integer tenantId,
        final QueryBuilder query, final QueryBuilder filter)
        throws DatabaseException, BadRequestException {
        return search(collectionName, tenantId, query, filter, null, null, 0, GlobalDatas.LIMIT_LOAD);
    }

    public final SearchResponse search(final String collectionName, final Integer tenantId,
        final QueryBuilder query, final QueryBuilder filter, String[] esProjection, final List<SortBuilder> sorts,
        int offset, Integer limit)
        throws DatabaseException, BadRequestException {
        return search(collectionName, tenantId, query, filter, esProjection, sorts, offset, limit, null, null, null);
    }


    public final SearchResponse search(final String collectionName, final Integer tenantId,
        final QueryBuilder query, final QueryBuilder filter, String[] esProjection, final List<SortBuilder> sorts,
        int offset, Integer limit,
        final List<AggregationBuilder> facets, final String scrollId, final Integer scrollTimeout)
        throws DatabaseException, BadRequestException {

        SearchResponse response;
        SearchSourceBuilder searchSourceBuilder = SearchSourceBuilder.searchSource()
            .explain(false)
            .query(query)
            .size(VitamConfiguration.getElasticSearchScrollLimit());

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
            .indices(getAliasName(collectionName, tenantId))
            .source(searchSourceBuilder)
            .searchType(SearchType.DFS_QUERY_THEN_FETCH);



        if (scrollId != null && !scrollId.isEmpty()) {
            int limitES = (limit != null && limit > 0) ? limit : DEFAULT_LIMIT_SCROLL;
            int scrollTimeoutES =
                (scrollTimeout != null && scrollTimeout > 0) ? scrollTimeout : DEFAULT_SCROLL_TIMEOUT;
            searchRequest.scroll(TimeValue.timeValueMillis(scrollTimeoutES));
            searchSourceBuilder.size(limitES);

            try {
                if (scrollId.equals(SCROLL_ACTIVATE_KEYWORD)) {
                    response = getClient().search(searchRequest, RequestOptions.DEFAULT);
                } else {
                    response = getClient()
                        .scroll(new SearchScrollRequest(scrollId).scroll(new TimeValue(scrollTimeoutES)),
                            RequestOptions.DEFAULT);
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
                switch (e.status()) {
                    case BAD_REQUEST:
                        throw new BadRequestException(e);
                     default:
                        throw new DatabaseException(e);
                }
            } catch (IOException e) {
                throw new DatabaseException(e);
            }
        }

        switch (response.status()) {
            case OK:
                break;
            default:
                throw new DatabaseException("Error " + response.status() + " from : " + searchRequest + ":" + query);
        }

        return response;
    }

    public void clearScroll(String scrollId) throws DatabaseException {
        ClearScrollRequest request = new ClearScrollRequest();
        request.addScrollId(scrollId);
        try {
            ClearScrollResponse response =
                getClient().clearScroll(request, RequestOptions.DEFAULT);

            boolean success = response.isSucceeded();
            int released = response.getNumFreed();

            Asserts.check(success, "clear scroll" + scrollId + " ko");
            LOGGER.error("clear scroll " + scrollId + " > success :" + success + ", released: " + released);
        } catch (IOException e) {
            throw new DatabaseException(e);
        }
    }

    private RestHighLevelClient createClient() {
        HttpHost[] hosts = new HttpHost[nodes.size()];
        for (int i = 0; i < nodes.size(); i++) {
            ElasticsearchNode elasticsearchNode = nodes.get(i);
            hosts[i] = new HttpHost(elasticsearchNode.getHostName(), elasticsearchNode.getTcpPort(), "http");
        }
        RestClientBuilder restClientBuilder = RestClient.builder(hosts);
        return new RestHighLevelClient(restClientBuilder);
    }

    /**
     * Close the ElasticSearch connection
     */
    public void close() {
        try {
            getClient().close();
        } catch (IOException e) {
            throw new RuntimeException(e);
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


    public final Map<String, String> createIndexAndAliasIfAliasNotExists(String collectionName, String mapping) {
        return createIndexAndAliasIfAliasNotExists(collectionName, mapping, null);
    }

    public final Map<String, String> createIndexAndAliasIfAliasNotExists(String collectionName, String mapping,
        Integer tenantId) {
        String indexName = getUniqueIndexName(collectionName, tenantId);
        String aliasName = getAliasName(collectionName, tenantId);
        LOGGER.debug("addIndex: {}", indexName);
        try {
            if (!existsAlias(aliasName)) {
                LOGGER.debug("createIndex");
                LOGGER.debug("setMapping: " + indexName + "\n\t" + mapping);

                boolean indexCreated = createIndex(aliasName, indexName, mapping);

                if (!indexCreated) {
                    return new HashMap<>();
                }
            }
        } catch (final IOException e) {
            LOGGER.error("Error while check alias existence", e);
            return new HashMap<>();
        }
        Map<String, String> map = new HashMap<>();
        map.put(aliasName, indexName);
        return map;
    }

    public final String createIndexWithoutAlias(String collectionName, String mapping,
        Integer tenantId)
        throws DatabaseException, IOException {
        String indexName = getUniqueIndexName(collectionName, tenantId);
        // Retrieve alias
        LOGGER.debug("createIndex");
        LOGGER.debug("setMapping: " + indexName + "\n\t" + mapping);
        boolean indexCreated = createIndex(indexName, mapping);

        if (!indexCreated) {
            String message = "Index creation exception for collection : " + collectionName;
            LOGGER.error(message);
            throw new DatabaseException(message);
        }
        return indexName;
    }

    public final void switchIndex(String aliasName, String indexNameToSwitchWith)
        throws DatabaseException, IOException {

        if (!existsAlias(aliasName)) {
            throw new DatabaseException(String.format("Alias not exist : %s", aliasName));
        }

        GetAliasesResponse actualIndex =
            getClient().indices().getAlias(new GetAliasesRequest(aliasName.toLowerCase()), RequestOptions.DEFAULT);

        Map<String, Set<AliasMetaData>> aliases = actualIndex.getAliases();

        String oldIndexName = null;

        if (!aliases.isEmpty()) {
            oldIndexName = aliases.keySet().iterator().next();
        }

        LOGGER.debug("Alias (" + aliasName.toLowerCase() + ") map to index (" + oldIndexName + ")");

        boolean aliasSwitched = switchAliasIndex(aliasName, oldIndexName, indexNameToSwitchWith);

        LOGGER.debug("aliasName %s", aliasName);

        if (!aliasSwitched) {
            final String message = "Switch alias (" + aliasName + ") from index (" + oldIndexName + ") to index (" +
                indexNameToSwitchWith + ")  error ";
            LOGGER.error(message);
            throw new DatabaseException(message);
        }
    }

    public final boolean deleteIndex(final String collectionName, Integer tenantId) {

        DeleteIndexRequest request = new DeleteIndexRequest(getAliasName(collectionName, tenantId));
        request.timeout(TimeValue.timeValueMinutes(2));
        request.masterNodeTimeout(TimeValue.timeValueMinutes(1));
        request.indicesOptions(IndicesOptions.STRICT_EXPAND_OPEN);

        try {
            AcknowledgedResponse deleteIndexResponse =
                getClient().indices().delete(request, RequestOptions.DEFAULT);

            return deleteIndexResponse.isAcknowledged();
        } catch (Exception exception) {
            if (exception instanceof ElasticsearchException &&
                ((ElasticsearchException) exception).status() == RestStatus.NOT_FOUND) {
                //Nothing to do
                return true;
            }
            LOGGER.error("Error while deleting index", exception);
            return false;
        }
    }

    public void delete(String collectionName, List<String> ids, Integer tenant) throws DatabaseException {

        String index = getAliasName(collectionName, tenant);


        Iterator<List<String>> idIterator =
            Iterators.partition(ids.iterator(), VitamConfiguration.getMaxElasticsearchBulk());

        while (idIterator.hasNext()) {

            BulkRequest bulkRequest = new BulkRequest();
            for (String id : idIterator.next()) {
                bulkRequest.add(new DeleteRequest(index.toLowerCase(), id));
            }

            WriteRequest.RefreshPolicy refreshPolicy = idIterator.hasNext() ?
                WriteRequest.RefreshPolicy.NONE :
                WriteRequest.RefreshPolicy.IMMEDIATE;

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

    private Builder settings() throws IOException {
        return Settings.builder().loadFromStream(ES_CONFIGURATION_FILE,
            ElasticsearchAccess.class.getResourceAsStream(ES_CONFIGURATION_FILE), true);
    }

    private String getUniqueIndexName(final String collectionName, Integer tenantId) {
        final String currentDate = LocalDateUtil.getFormattedDateForEsIndexes(LocalDateUtil.now());
        if (tenantId != null) {
            return collectionName.toLowerCase() + "_" + tenantId.toString() + "_" +
                currentDate;
        } else {
            return collectionName.toLowerCase() + "_" + currentDate;
        }
    }

    private String getAliasName(final String collectionName, Integer tenantId) {
        if (tenantId != null) {
            return collectionName.toLowerCase() + "_" + tenantId.toString();
        } else {
            return collectionName.toLowerCase();
        }
    }
}
