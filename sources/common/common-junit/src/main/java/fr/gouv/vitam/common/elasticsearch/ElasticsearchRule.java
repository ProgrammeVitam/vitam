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
package fr.gouv.vitam.common.elasticsearch;

import com.google.common.collect.Sets;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import org.apache.commons.collections.CollectionUtils;
import org.apache.http.HttpHost;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.alias.Alias;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.support.ActiveShardCount;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.index.reindex.ScrollableHitSource;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.xcontent.XContentType;
import org.junit.rules.ExternalResource;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;

/**
 *
 */
public class ElasticsearchRule extends ExternalResource {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ElasticsearchRule.class);

    public static final int PORT = 9200;
    public static String HOST = "localhost";
    public static final String VITAM_CLUSTER = "elasticsearch-data";
    private boolean clientClosed = false;
    private final RestHighLevelClient client;
    private Set<String> indexesToBePurged = new HashSet<>();

    public ElasticsearchRule(String... indexesToBePurged) {
        client = new RestHighLevelClient(RestClient.builder(new HttpHost(HOST, PORT)));

        if (null != indexesToBePurged) {
            this.indexesToBePurged = Sets.newHashSet(indexesToBePurged);
        }
    }

    @Override
    protected void after() {
        if (!clientClosed) {
            purge(client, indexesToBePurged);
        }
    }

    private void purge(RestHighLevelClient client, Collection<String> indexesToBePurged) {
        for (String indexName : indexesToBePurged) {
            purge(client, indexName);
        }
    }

    public void purge(RestHighLevelClient client, String indexName) {
        handlePurge(client, indexName, matchAllQuery());
    }

    public void handlePurge(RestHighLevelClient client, String index, QueryBuilder qb) {
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
                throw new RuntimeException("ES purge errors : in search phase");
            }

            List<BulkItemResponse.Failure> bulkFailures = bulkResponse.getBulkFailures();
            if (CollectionUtils.isNotEmpty(bulkFailures)) {
                throw new RuntimeException("ES purge errors : in bulk phase");
            }

            LOGGER.info("Deleted : " + bulkResponse.getDeleted());
        } catch (ElasticsearchException e) {
            if (e.status() == RestStatus.NOT_FOUND) {
                return;
            }
            throw new RuntimeException("Purge Exception", e);
        } catch (IOException e) {
            throw new RuntimeException("Purge Exception", e);
        }
    }

    public boolean existsIndex(String indexName) throws IOException {
        GetIndexRequest request = new GetIndexRequest(indexName.toLowerCase());
        request.humanReadable(true);
        request.includeDefaults(false);
        request.indicesOptions(IndicesOptions.STRICT_EXPAND_OPEN);
        return getClient().indices().exists(request, RequestOptions.DEFAULT);
    }

    public boolean createIndex(String aliasName, String indexName, String mapping) throws IOException {
        boolean existsIndex = existsIndex(indexName);

        if (Boolean.TRUE.equals(existsIndex)) {
            LOGGER.debug("Index (" + existsIndex + ") already exists");
            return true;
        }

        CreateIndexRequest request = new CreateIndexRequest(indexName)
            .mapping(mapping, XContentType.JSON)
            .alias(new Alias(aliasName));

        request.setTimeout(
            TimeValue.timeValueMillis(VitamConfiguration.getElasticSearchTimeoutWaitRequestInMilliseconds())
        );
        request.setMasterTimeout(TimeValue.timeValueMinutes(1));
        request.waitForActiveShards(ActiveShardCount.DEFAULT);

        CreateIndexResponse response = getClient().indices().create(request, RequestOptions.DEFAULT);

        boolean acknowledged = response.isAcknowledged();
        boolean shardsAcknowledged = response.isShardsAcknowledged();

        LOGGER.debug(
            "Alias (" +
            aliasName +
            ") and index (" +
            indexName +
            ") create response acknowledged (" +
            acknowledged +
            ") and shardsAcknowledged (" +
            shardsAcknowledged +
            ") "
        );

        return acknowledged && shardsAcknowledged;
    }

    public final void purgeIndex(RestHighLevelClient client, String indexName) {
        purge(client, indexName);
    }

    public void deleteIndexesWithoutClose() {
        for (String indexName : indexesToBePurged) {
            purgeIndex(client, indexName);
        }
        indexesToBePurged = new HashSet<>();
    }

    public void purgeIndices() {
        for (String indexName : indexesToBePurged) {
            purgeIndex(client, indexName);
        }
        indexesToBePurged = new HashSet<>();
        close();
    }

    // Add index to be purged
    public ElasticsearchRule addIndexToBePurged(String indexName) {
        indexesToBePurged.add(indexName);
        return this;
    }

    /**
     * Used when annotated @ClassRule
     */
    public void handleAfter() {
        after();
    }

    public void handleAfter(Set<String> indexesToBePurged) {
        purge(client, indexesToBePurged);
    }

    /**
     * get the cluster name
     *
     * @return the vitam cluster name
     */
    public static String getClusterName() {
        return VITAM_CLUSTER;
    }

    /**
     * get the tcp port
     *
     * @return TCP_PORT
     */
    public static int getPort() {
        return PORT;
    }

    public static String getHost() {
        return HOST;
    }

    /**
     * get the Client
     *
     * @return the client
     */
    public RestHighLevelClient getClient() {
        return client;
    }

    public void close() {
        try {
            client.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        clientClosed = true;
    }
}
