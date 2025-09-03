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

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.BulkIndexByScrollFailure;
import co.elastic.clients.elasticsearch._types.Conflicts;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.ExpandWildcard;
import co.elastic.clients.elasticsearch._types.OpType;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch._types.SearchType;
import co.elastic.clients.elasticsearch._types.ShardFailure;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.cluster.HealthRequest;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.ClearScrollRequest;
import co.elastic.clients.elasticsearch.core.ClearScrollResponse;
import co.elastic.clients.elasticsearch.core.DeleteByQueryRequest;
import co.elastic.clients.elasticsearch.core.DeleteByQueryResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.ScrollRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.search.ResponseBody;
import co.elastic.clients.elasticsearch.core.search.TrackHits;
import co.elastic.clients.elasticsearch.indices.Alias;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexResponse;
import co.elastic.clients.elasticsearch.indices.ExistsAliasRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.elasticsearch.indices.GetAliasRequest;
import co.elastic.clients.elasticsearch.indices.GetAliasResponse;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import co.elastic.clients.elasticsearch.indices.RefreshRequest;
import co.elastic.clients.elasticsearch.indices.RefreshResponse;
import co.elastic.clients.elasticsearch.indices.UpdateAliasesRequest;
import co.elastic.clients.elasticsearch.indices.UpdateAliasesResponse;
import co.elastic.clients.elasticsearch.indices.get_alias.IndexAliases;
import co.elastic.clients.elasticsearch.indices.update_aliases.Action;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.instrumentation.NoopInstrumentation;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.exception.VitamFatalRuntimeException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.server.application.configuration.DatabaseConnection;
import jakarta.ws.rs.core.Response;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpHost;
import org.bson.Document;
import org.elasticsearch.client.RestClient;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchUtil.buildFailureMessage;
import static fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchUtil.matchAll;
import static fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchUtil.termQuery;
import static fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchUtil.timeOfMilliseconds;
import static fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchUtil.toDatabaseException;

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
    private final AtomicReference<ElasticsearchClient> esClient = new AtomicReference<>();
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

    public final GetAliasResponse getAlias(ElasticsearchIndexAlias indexAlias)
        throws IOException, ElasticsearchException {
        GetAliasRequest request = new GetAliasRequest.Builder()
            .name(indexAlias.getName())
            .expandWildcards(List.of(ExpandWildcard.Open))
            .allowNoIndices(false)
            .build();
        return getClient().indices().getAlias(request);
    }

    public final ElasticsearchIndexAlias createIndexWithoutAlias(
        ElasticsearchIndexAlias indexAlias,
        ElasticsearchIndexSettings indexSettings,
        String elasticsearchConfigurationFile
    ) throws DatabaseException {
        ElasticsearchIndexAlias newIndexName = indexAlias.createUniqueIndexName();
        createIndexWithOptionalAlias(
            null,
            newIndexName.getName(),
            indexSettings.loadMapping(),
            indexSettings.getShards(),
            indexSettings.getReplicas(),
            elasticsearchConfigurationFile
        );
        return newIndexName;
    }

    private void createIndexWithOptionalAlias(
        String aliasName,
        String indexName,
        String mapping,
        Integer shards,
        Integer replicas,
        String elasticsearchConfigurationFile
    ) throws DatabaseException {
        try {
            CreateIndexRequest.Builder requestBuilder = new CreateIndexRequest.Builder()
                .index(indexName)
                .settings(createIndexSettings(shards, replicas, elasticsearchConfigurationFile))
                .mappings(new TypeMapping.Builder().withJson(new StringReader(mapping)).build());

            if (aliasName != null) {
                requestBuilder.aliases(Map.of(aliasName, new Alias.Builder().build()));
            }

            requestBuilder.timeout(
                timeOfMilliseconds(VitamConfiguration.getElasticSearchTimeoutWaitRequestInMilliseconds())
            );
            requestBuilder.masterTimeout(
                timeOfMilliseconds(VitamConfiguration.getElasticSearchTimeoutWaitRequestInMilliseconds())
            );

            CreateIndexResponse createIndexResponse = getClient().indices().create(requestBuilder.build());

            if (!createIndexResponse.acknowledged() || !createIndexResponse.shardsAcknowledged()) {
                throw new DatabaseException(
                    "Could not create index " +
                    indexName +
                    ". acknowledged: " +
                    createIndexResponse.acknowledged() +
                    ", shardsAcknowledged: " +
                    createIndexResponse.shardsAcknowledged()
                );
            }
        } catch (IOException | ElasticsearchException e) {
            throw toDatabaseException("Could not create index " + indexName, e);
        }
    }

    public final boolean existsAlias(ElasticsearchIndexAlias indexAlias) throws DatabaseException {
        try {
            ExistsAliasRequest existsAliasRequest = new ExistsAliasRequest.Builder()
                .name(indexAlias.getName())
                .expandWildcards(List.of(ExpandWildcard.Open))
                .allowNoIndices(false)
                .build();
            return getClient().indices().existsAlias(existsAliasRequest).value();
        } catch (IOException | ElasticsearchException e) {
            throw toDatabaseException("Could not check alias existence " + indexAlias.getName(), e);
        }
    }

    public final boolean existsIndex(ElasticsearchIndexAlias index) throws DatabaseException {
        try {
            ExistsRequest existsRequest = new ExistsRequest.Builder()
                .index(index.getName())
                .expandWildcards(List.of(ExpandWildcard.Open))
                .allowNoIndices(false)
                .build();
            return getClient().indices().exists(existsRequest).value();
        } catch (IOException | ElasticsearchException e) {
            throw toDatabaseException("Could not check index existence " + index.getName(), e);
        }
    }

    public final void refreshIndex(ElasticsearchIndexAlias indexAlias) throws DatabaseException {
        LOGGER.debug("refreshIndex: " + indexAlias.getName());

        RefreshRequest request = new RefreshRequest.Builder()
            .index(indexAlias.getName())
            .expandWildcards(List.of(ExpandWildcard.Open))
            .allowNoIndices(false)
            .build();
        RefreshResponse refreshResponse;
        try {
            refreshResponse = getClient().indices().refresh(request);
        } catch (IOException | ElasticsearchException e) {
            throw toDatabaseException("Could not refresh index ", e);
        }
        LOGGER.debug(
            "Refresh request executed with {} successful shards",
            refreshResponse.shards().successful().intValue()
        );

        int failedShards = refreshResponse.shards().failed().intValue();
        if (failedShards > 0) {
            throw new DatabaseException(
                refreshResponse
                    .shards()
                    .failures()
                    .stream()
                    .map(ShardFailure::toString)
                    .collect(Collectors.joining("; "))
            );
        }
    }

    @VisibleForTesting
    protected void purgeIndexForTesting(ElasticsearchIndexAlias indexAlias, Integer tenantId) throws DatabaseException {
        Query query = termQuery(VitamDocument.TENANT_ID, tenantId);
        purgeIndexForTesting(indexAlias, query);
    }

    @VisibleForTesting
    public final void purgeIndexForTesting(ElasticsearchIndexAlias indexAlias) throws DatabaseException {
        purgeIndexForTesting(indexAlias, matchAll());
    }

    private void purgeIndexForTesting(ElasticsearchIndexAlias indexAlias, Query query) throws DatabaseException {
        try {
            if (existsAlias(indexAlias)) {
                DeleteByQueryRequest request = new DeleteByQueryRequest.Builder()
                    .index(indexAlias.getName())
                    .conflicts(Conflicts.Proceed)
                    .query(query)
                    .scrollSize((long) VitamConfiguration.getMaxElasticsearchBulk())
                    .scroll(timeOfMilliseconds(VitamConfiguration.getElasticSearchScrollTimeoutInMilliseconds()))
                    .timeout(timeOfMilliseconds(VitamConfiguration.getElasticSearchTimeoutWaitRequestInMilliseconds()))
                    .refresh(true)
                    .build();
                DeleteByQueryResponse bulkResponse = getClient().deleteByQuery(request);

                LOGGER.debug("Purge alias (" + indexAlias.getName() + ") : " + bulkResponse.toString());

                List<BulkIndexByScrollFailure> searchFailures = bulkResponse.failures();
                if (CollectionUtils.isNotEmpty(searchFailures)) {
                    throw new DatabaseException("ES purge errors : in search phase > " + searchFailures);
                }
            }
        } catch (IOException | ElasticsearchException e) {
            throw toDatabaseException(e);
        }
    }

    public final <T> void indexEntry(
        ElasticsearchIndexAlias indexAlias,
        final String id,
        final VitamDocument<T> vitamDocument
    ) throws DatabaseException {
        IndexResponse indexResponse = indexDocument(indexAlias, id, vitamDocument);

        if (indexResponse.result() != Result.Created && indexResponse.result() != Result.Updated) {
            throw new DatabaseException(
                String.format(
                    "Could not index document on ES. Id=%s, aliasName=%s, response=%s",
                    id,
                    indexAlias.getName(),
                    indexResponse
                )
            );
        }
    }

    public void indexEntries(
        ElasticsearchIndexAlias indexAlias,
        final Collection<? extends Document> documents,
        boolean withRefreshIndex
    ) throws DatabaseException {
        UnmodifiableIterator<? extends List<? extends Document>> idIterator = Iterators.partition(
            documents.iterator(),
            VitamConfiguration.getMaxElasticsearchBulk()
        );

        while (idIterator.hasNext()) {
            List<? extends Document> docs = idIterator.next();
            List<Pair<Document, String>> docsAndIds = new ArrayList<>(docs.size());
            BulkRequest.Builder bulkRequestBuilder = new BulkRequest.Builder();

            try {
                docs.forEach(document -> {
                    // Remove temporarily _id field from document (forbidden by ES API)
                    String id = (String) document.remove(VitamDocument.ID);
                    docsAndIds.add(ImmutablePair.of(document, id));

                    bulkRequestBuilder.operations(
                        o -> o.index(idx -> idx.index(indexAlias.getName()).id(id).document(document))
                    );
                });

                try {
                    bulkRequestBuilder.refresh(withRefreshIndex ? Refresh.True : Refresh.False);

                    BulkResponse bulkResponse = getClient().bulk(bulkRequestBuilder.build());

                    LOGGER.debug("Written document {}", bulkResponse.items().size());
                    if (bulkResponse.errors()) {
                        LOGGER.error("Bulk Request failure with error: " + buildFailureMessage(bulkResponse));
                        throw new DatabaseException("Bulk Request failure");
                    }
                } catch (IOException | ElasticsearchException e) {
                    throw toDatabaseException(e);
                }
            } finally {
                // Restore _id fields
                for (Pair<Document, String> docsAndId : docsAndIds) {
                    docsAndId.getLeft().put(VitamDocument.ID, docsAndId.getRight());
                }
            }
        }
    }

    /**
     * Update one element fully
     */
    public <T> void updateEntry(ElasticsearchIndexAlias indexAlias, String id, VitamDocument<T> vitamDocument)
        throws DatabaseException {
        IndexResponse indexResponse = indexDocument(indexAlias, id, vitamDocument);

        if (indexResponse.result() != Result.Updated) {
            throw new DatabaseException(
                String.format(
                    "Could not update document on ES. Id=%s, aliasName=%s, response=%s",
                    id,
                    indexAlias.getName(),
                    indexResponse
                )
            );
        }
    }

    private <T> IndexResponse indexDocument(
        ElasticsearchIndexAlias indexAlias,
        String id,
        VitamDocument<T> vitamDocument
    ) throws DatabaseException {
        IndexRequest<VitamDocument<T>> request = new IndexRequest.Builder<VitamDocument<T>>()
            .index(indexAlias.getName())
            .id(id)
            .document(vitamDocument)
            .refresh(Refresh.True)
            .timeout(timeOfMilliseconds(VitamConfiguration.getElasticSearchTimeoutWaitRequestInMilliseconds()))
            .opType(OpType.Index)
            .build();

        vitamDocument.remove(VitamDocument.ID);
        vitamDocument.remove(VitamDocument.SCORE);

        try {
            return getClient().index(request);
        } catch (IOException | ElasticsearchException e) {
            throw toDatabaseException(e);
        } finally {
            vitamDocument.put(VitamDocument.ID, id);
        }
    }

    public final ResponseBody<ObjectNode> search(
        ElasticsearchIndexAlias indexAlias,
        final Query query,
        String[] esProjection,
        final List<SortOptions> sorts,
        int offset,
        Integer limit,
        final Map<String, Aggregation> facets
    ) throws DatabaseException, BadRequestException {
        return search(indexAlias, query, esProjection, sorts, offset, limit, facets, null, null, false);
    }

    public final ResponseBody<ObjectNode> searchCrossIndices(
        Set<ElasticsearchIndexAlias> indexAliases,
        final Query query,
        String[] esProjection,
        final List<SortOptions> sorts,
        int offset,
        Integer limit,
        final Map<String, Aggregation> facets,
        final String scrollId,
        final Integer scrollTimeout,
        boolean trackTotalHits
    ) throws DatabaseException, BadRequestException {
        ResponseBody<ObjectNode> response;

        SearchRequest.Builder searchRequest = new SearchRequest.Builder()
            .index(indexAliases.stream().map(ElasticsearchIndexAlias::getName).collect(Collectors.toList()))
            .query(query)
            .size(VitamConfiguration.getElasticSearchScrollLimit())
            .sort(ListUtils.emptyIfNull(sorts))
            .source(
                s ->
                    s.filter(
                        f -> f.includes(esProjection == null ? Collections.emptyList() : Arrays.asList(esProjection))
                    )
            )
            .searchType(SearchType.DfsQueryThenFetch);

        if (trackTotalHits) {
            // Enable trackTotalHits flag to compute total result set count (not 10000 since ES 7.0)
            // Warning, only call trackTotalHits setter only for "true". Setting "false" causes $hits to not be computed
            searchRequest.trackTotalHits(TrackHits.of(t -> t.enabled(true)));
        }

        if (scrollId != null && !scrollId.isEmpty()) {
            int limitES = (limit != null && limit > 0) ? limit : DEFAULT_LIMIT_SCROLL;
            int scrollTimeoutES = (scrollTimeout != null && scrollTimeout > 0) ? scrollTimeout : DEFAULT_SCROLL_TIMEOUT;
            searchRequest.scroll(timeOfMilliseconds(scrollTimeoutES));
            searchRequest.size(limitES);

            try {
                if (scrollId.equals(SCROLL_ACTIVATE_KEYWORD)) {
                    response = getClient().search(searchRequest.build(), ObjectNode.class);
                } else {
                    ScrollRequest scrollRequest = ScrollRequest.of(
                        s -> s.scrollId(scrollId).scroll(timeOfMilliseconds(scrollTimeoutES))
                    );
                    response = getClient().scroll(scrollRequest, ObjectNode.class);
                }
            } catch (IOException | ElasticsearchException e) {
                throw toDatabaseException(e);
            }
        } else {
            if (offset != -1) {
                searchRequest.from(offset);
            }
            if (limit != -1) {
                searchRequest.size(limit);
            }

            if (facets != null && !facets.isEmpty()) {
                for (Map.Entry<String, Aggregation> facet : facets.entrySet()) {
                    searchRequest.aggregations(facet.getKey(), facet.getValue());
                }
            }

            searchRequest.query(query);

            LOGGER.debug("ESReq: {}", searchRequest);

            try {
                response = getClient().search(searchRequest.build(), ObjectNode.class);
            } catch (final ElasticsearchException e) {
                if (e.status() == Response.Status.BAD_REQUEST.getStatusCode()) {
                    throw new BadRequestException(toDatabaseException(e));
                }
                throw toDatabaseException(e);
            } catch (IOException e) {
                throw toDatabaseException(e);
            }
        }

        if (response.timedOut() || Boolean.TRUE.equals(response.terminatedEarly())) {
            LOGGER.error("Request didn't terminate properly " + response);
            throw new DatabaseException("Error " + response + " from : " + searchRequest + ":" + query);
        }

        return response;
    }

    public final ResponseBody<ObjectNode> search(
        ElasticsearchIndexAlias indexAlias,
        final Query query,
        String[] esProjection,
        final List<SortOptions> sorts,
        int offset,
        Integer limit,
        final Map<String, Aggregation> facets,
        final String scrollId,
        final Integer scrollTimeout,
        boolean trackTotalHits
    ) throws DatabaseException, BadRequestException {
        return searchCrossIndices(
            Set.of(indexAlias),
            query,
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
        ClearScrollRequest request = ClearScrollRequest.of(s -> s.scrollId(scrollId));
        try {
            ClearScrollResponse response = getClient().clearScroll(request);

            boolean success = response.succeeded();
            int released = response.numFreed();
            LOGGER.debug("clear scroll " + scrollId + " > success :" + success + ", released: " + released);

            if (!success) {
                throw new DatabaseException("Clear scroll" + scrollId + " ko");
            }
        } catch (IOException | ElasticsearchException e) {
            throw toDatabaseException(e);
        }
    }

    private ElasticsearchClient createClient() {
        HttpHost[] hosts = new HttpHost[nodes.size()];
        for (int i = 0; i < nodes.size(); i++) {
            ElasticsearchNode elasticsearchNode = nodes.get(i);
            hosts[i] = new HttpHost(elasticsearchNode.getHostName(), elasticsearchNode.getHttpPort(), "http");
        }
        RestClient httpClient = RestClient.builder(hosts)
            .setRequestConfigCallback(
                requestConfigBuilder ->
                    requestConfigBuilder
                        .setConnectionRequestTimeout(
                            VitamConfiguration.getElasticSearchTimeoutWaitRequestInMilliseconds()
                        )
                        .setConnectTimeout(VitamConfiguration.getConnectTimeout())
                        .setSocketTimeout(VitamConfiguration.getReadTimeout())
            )
            .build();

        ElasticsearchTransport transport = new RestClientTransport(
            httpClient,
            new JacksonJsonpMapper(),
            null,
            NoopInstrumentation.INSTANCE
        );
        return new ElasticsearchClient(transport);
    }

    /**
     * Close the ElasticSearch connection
     */
    public void close() {
        try {
            getClient()._transport().close();
        } catch (IOException | ElasticsearchException e) {
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
    public ElasticsearchClient getClient() {
        ElasticsearchClient client = esClient.get();
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
        try {
            ElasticsearchClient clientCheck = createClient();
            return !clientCheck.cluster().health(new HealthRequest.Builder().build()).timedOut();
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
        ElasticsearchIndexSettings indexSettings,
        String elasticsearchConfigurationFile
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
            indexSettings.getReplicas(),
            elasticsearchConfigurationFile
        );
    }

    public final void switchIndex(ElasticsearchIndexAlias indexAlias, ElasticsearchIndexAlias indexNameToSwitchTo)
        throws DatabaseException, IOException, ElasticsearchException {
        if (!existsAlias(indexAlias)) {
            throw new DatabaseException(String.format("Alias does not exist : %s", indexAlias.getName()));
        }

        if (!existsIndex(indexNameToSwitchTo)) {
            throw new DatabaseException(String.format("New index does not exist : %s", indexNameToSwitchTo.getName()));
        }

        GetAliasResponse actualIndex = getAlias(indexAlias);

        Map<String, IndexAliases> aliases = actualIndex.result();

        if (aliases.isEmpty()) {
            throw new DatabaseException("No previous index found");
        }
        String oldIndexName = aliases.keySet().iterator().next();

        LOGGER.debug("Alias (" + indexAlias.getName() + ") map to index (" + oldIndexName + ")");

        Action addNewIndexAliasAction = Action.of(
            b -> b.add(idx -> idx.index(indexNameToSwitchTo.getName()).alias(indexAlias.getName()))
        );
        Action deleteOldIndexAliasAction = Action.of(
            b -> b.remove(idx -> idx.index(oldIndexName).alias(indexAlias.getName()))
        );
        UpdateAliasesRequest request = new UpdateAliasesRequest.Builder()
            .actions(addNewIndexAliasAction, deleteOldIndexAliasAction)
            .timeout(timeOfMilliseconds(VitamConfiguration.getElasticSearchTimeoutWaitRequestInMilliseconds()))
            .masterTimeout(timeOfMilliseconds(VitamConfiguration.getElasticSearchTimeoutWaitRequestInMilliseconds()))
            .build();
        UpdateAliasesResponse response = getClient().indices().updateAliases(request);

        if (!response.acknowledged()) {
            LOGGER.error("Cannot switch index" + response);
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
        GetAliasResponse aliasResponse;
        try {
            aliasResponse = getAlias(indexAlias);
        } catch (ElasticsearchException e) {
            if (e.status() != 404) {
                throw toDatabaseException(e);
            }
            return;
        } catch (IOException e) {
            throw new DatabaseException(e);
        }
        for (Map.Entry<String, IndexAliases> entry : aliasResponse.result().entrySet()) {
            deleteIndexForTesting(entry.getKey());
        }
    }

    @VisibleForTesting
    public final void deleteIndexForTesting(ElasticsearchIndexAlias indexAlias) throws DatabaseException {
        deleteIndexForTesting(indexAlias.getName());
    }

    private void deleteIndexForTesting(final String indexFullName) throws DatabaseException {
        DeleteIndexRequest request = new DeleteIndexRequest.Builder()
            .index(indexFullName)
            .timeout(timeOfMilliseconds(VitamConfiguration.getElasticSearchTimeoutWaitRequestInMilliseconds()))
            .masterTimeout(timeOfMilliseconds(VitamConfiguration.getElasticSearchTimeoutWaitRequestInMilliseconds()))
            .expandWildcards(List.of(ExpandWildcard.Open))
            .allowNoIndices(false)
            .build();

        try {
            DeleteIndexResponse deleteIndexResponse = getClient().indices().delete(request);

            if (!deleteIndexResponse.acknowledged()) {
                throw new DatabaseException("Error while deleting index " + indexFullName + ". Not acknowledged");
            }
        } catch (Exception exception) {
            if (
                exception instanceof ElasticsearchException &&
                ((ElasticsearchException) exception).status() == Response.Status.NOT_FOUND.getStatusCode()
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
            BulkRequest.Builder bulkRequestBuilder = new BulkRequest.Builder();
            for (String id : idIterator.next()) {
                bulkRequestBuilder.operations(op -> op.delete(idx -> idx.index(indexAlias.getName()).id(id)));
            }
            bulkRequestBuilder.refresh(idIterator.hasNext() ? Refresh.False : Refresh.True);

            BulkResponse bulkResponse;
            try {
                bulkResponse = getClient().bulk(bulkRequestBuilder.build());
            } catch (IOException | ElasticsearchException e) {
                throw toDatabaseException("Bulk delete exception", e);
            }

            if (bulkResponse.errors()) {
                throw new DatabaseException("ES delete in error: " + buildFailureMessage(bulkResponse));
            }
        }
    }

    private IndexSettings createIndexSettings(int shards, int replicas, String elasticsearchConfigurationFile)
        throws IOException {
        return new IndexSettings.Builder()
            .withJson(getElasticSearchSettingAsInputStream(elasticsearchConfigurationFile))
            .numberOfShards(String.valueOf(shards))
            .numberOfReplicas(String.valueOf(replicas))
            .build();
    }

    private InputStream getElasticSearchSettingAsInputStream(String elasticsearchConfigurationFilePath)
        throws FileNotFoundException {
        try {
            Path elasticSearchConfigPath = Paths.get(elasticsearchConfigurationFilePath);
            SanityChecker.checkJsonFile(elasticSearchConfigPath.toFile());
            return Files.newInputStream(elasticSearchConfigPath);
        } catch (IOException e) {
            LOGGER.error("Elastic search File not found with path " + elasticsearchConfigurationFilePath);
            throw new FileNotFoundException(e.getMessage());
        } catch (InvalidParseOperationException e) {
            LOGGER.error("Invalid Elastic search settings file ");
            throw new IllegalStateException(e.getMessage());
        }
    }
}
