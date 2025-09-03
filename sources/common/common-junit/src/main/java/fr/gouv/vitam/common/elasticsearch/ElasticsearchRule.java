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

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.BulkIndexByScrollFailure;
import co.elastic.clients.elasticsearch._types.Conflicts;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.ExpandWildcard;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch.core.DeleteByQueryRequest;
import co.elastic.clients.elasticsearch.core.DeleteByQueryResponse;
import co.elastic.clients.elasticsearch.indices.Alias;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.instrumentation.NoopInstrumentation;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.google.common.collect.Sets;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import jakarta.ws.rs.core.Response;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.junit.rules.ExternalResource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class ElasticsearchRule extends ExternalResource {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ElasticsearchRule.class);

    public static final int PORT = 9200;
    public static String HOST = "localhost";
    public static final String VITAM_CLUSTER = "elasticsearch-data";
    private boolean clientClosed = false;
    private final ElasticsearchClient client;
    private Set<String> indexesToBePurged = new HashSet<>();

    public ElasticsearchRule(String... indexesToBePurged) {
        RestClient restClient = RestClient.builder(new HttpHost(HOST, PORT)).build();

        ElasticsearchTransport transport = new RestClientTransport(
            restClient,
            new JacksonJsonpMapper(),
            null,
            NoopInstrumentation.INSTANCE
        );

        // And create the API client
        client = new ElasticsearchClient(transport);

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

    private void purge(ElasticsearchClient client, Collection<String> indexesToBePurged) {
        for (String indexName : indexesToBePurged) {
            purge(client, indexName);
        }
    }

    public void purge(ElasticsearchClient client, String indexName) {
        handlePurge(client, indexName, QueryBuilders.matchAll().build()._toQuery());
    }

    public void handlePurge(ElasticsearchClient client, String index, Query query) {
        try {
            DeleteByQueryRequest request = new DeleteByQueryRequest.Builder()
                .index(index)
                .conflicts(Conflicts.Proceed)
                .query(query)
                .scrollSize((long) VitamConfiguration.getMaxElasticsearchBulk())
                .scroll(timeOfMilliseconds(VitamConfiguration.getElasticSearchScrollTimeoutInMilliseconds()))
                .timeout(timeOfMilliseconds(VitamConfiguration.getElasticSearchTimeoutWaitRequestInMilliseconds()))
                .refresh(true)
                .waitForCompletion(true)
                .build();

            DeleteByQueryResponse bulkResponse = client.deleteByQuery(request);

            LOGGER.debug("Purge : {}", bulkResponse);

            List<BulkIndexByScrollFailure> searchFailures = bulkResponse.failures();
            if (CollectionUtils.isNotEmpty(searchFailures)) {
                throw new RuntimeException("ES purge errors : in search phase " + bulkResponse);
            }

            LOGGER.info("Deleted : " + bulkResponse.deleted());
        } catch (ElasticsearchException e) {
            if (e.status() == Response.Status.NOT_FOUND.getStatusCode()) {
                return;
            }
            throw new RuntimeException("Purge Exception", e);
        } catch (IOException e) {
            throw new RuntimeException("Purge Exception", e);
        }
    }

    public boolean existsIndex(String indexName) throws IOException {
        ExistsRequest existsRequest = new ExistsRequest.Builder()
            .index(indexName.toLowerCase())
            .includeDefaults(false)
            .expandWildcards(ExpandWildcard.Open)
            .allowNoIndices(false)
            .build();
        return client.indices().exists(existsRequest).value();
    }

    public boolean createIndex(String aliasName, String indexName, String mapping) throws IOException {
        boolean existsIndex = existsIndex(indexName);

        if (Boolean.TRUE.equals(existsIndex)) {
            LOGGER.debug("Index (" + indexName + ") already exists");
            return true;
        }

        CreateIndexRequest request = new CreateIndexRequest.Builder()
            .index(indexName)
            .mappings(
                new TypeMapping.Builder()
                    .withJson(new ByteArrayInputStream(mapping.getBytes(StandardCharsets.UTF_8)))
                    .build()
            )
            .aliases(Map.of(aliasName, new Alias.Builder().build()))
            .timeout(timeOfMilliseconds(VitamConfiguration.getElasticSearchTimeoutWaitRequestInMilliseconds()))
            .masterTimeout(timeOfMinutes(1))
            .settings(new IndexSettings.Builder().numberOfShards("1").numberOfReplicas("0").build())
            .build();

        CreateIndexResponse response = getClient().indices().create(request);

        boolean acknowledged = response.acknowledged();
        boolean shardsAcknowledged = response.shardsAcknowledged();

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

    public final void purgeIndex(ElasticsearchClient client, String indexName) {
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
    public ElasticsearchClient getClient() {
        return client;
    }

    public void close() {
        try {
            client._transport().close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        clientClosed = true;
    }

    private static Time timeOfMilliseconds(int duration) {
        return new Time.Builder().time(duration + "ms").build();
    }

    private static Time timeOfMinutes(int duration) {
        return new Time.Builder().time(duration + "m").build();
    }
}
