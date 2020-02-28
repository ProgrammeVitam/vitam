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
package fr.gouv.vitam.common.elasticsearch;

import com.google.common.collect.Sets;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.junit.rules.ExternalResource;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;

/**
 *
 */
public class ElasticsearchRule extends ExternalResource {

    public static final int TCP_PORT = 9300;
    public static final String VITAM_CLUSTER = "elasticsearch-data";
    private boolean clientClosed = false;
    private Client client;
    private Set<String> indexesToBePurged = new HashSet<>();

    public ElasticsearchRule(String... indexesToBePurged) {
        try {
            client = new PreBuiltTransportClient(getClientSettings()).addTransportAddress(
                new TransportAddress(InetAddress.getByName("localhost"), TCP_PORT));
        } catch (final UnknownHostException e) {
            throw new VitamRuntimeException(e);
        }

        if (null != indexesToBePurged) {
            this.indexesToBePurged = Sets.newHashSet(indexesToBePurged);

        }
    }

    private Settings getClientSettings() {
        return Settings.builder().put("cluster.name", VITAM_CLUSTER)
            .put("client.transport.sniff", true)
            .put("client.transport.ping_timeout", "2s")
            .put("transport.tcp.connect_timeout", "1s")
            .put("thread_pool.refresh.max", VitamConfiguration.getNumberDbClientThread())
            .put("thread_pool.search.size", VitamConfiguration.getNumberDbClientThread())
            .put("thread_pool.search.queue_size", VitamConfiguration.getNumberEsQueue())
            .put("thread_pool.bulk.queue_size", VitamConfiguration.getNumberEsQueue())
            .build();
    }


    @Override
    protected void after() {
        if (!clientClosed) {
            purge(client, indexesToBePurged);
        }
    }

    private void purge(Client client, Collection<String> indexesToBePurged) {
        for (String indexName : indexesToBePurged) {

            purge(client, indexName);
        }
    }


    public static void purge(Client client, String indexName) {
        if (client.admin().indices().prepareExists(indexName.toLowerCase()).get().isExists()) {
            QueryBuilder qb = matchAllQuery();

            SearchResponse scrollResp = client.prepareSearch(indexName.toLowerCase())
                .addSort(FieldSortBuilder.DOC_FIELD_NAME, SortOrder.ASC)
                .setScroll(new TimeValue(60000))
                .setQuery(qb)
                .setFetchSource(false)
                .setSize(100).get();

            BulkRequestBuilder bulkRequest = client.prepareBulk();

            do {
                for (SearchHit hit : scrollResp.getHits().getHits()) {
                    bulkRequest.add(client.prepareDelete(indexName.toLowerCase(), "typeunique", hit.getId()));
                }

                scrollResp =
                    client.prepareSearchScroll(scrollResp.getScrollId()).setScroll(new TimeValue(60000)).execute()
                        .actionGet();
            } while (scrollResp.getHits().getHits().length != 0);

            bulkRequest.request().setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);

            if (bulkRequest.request().numberOfActions() != 0) {
                BulkResponse bulkResponse = bulkRequest.get();

                if (bulkResponse.hasFailures()) {
                    throw new VitamRuntimeException(
                        String.format("DatabaseException when calling purge by bulk Request %s",
                            bulkResponse.buildFailureMessage()));
                }
            }
        }
    }


    public final void deleteIndex(Client client, String indexName) {
        purge(client, indexName);
    }

    public void deleteIndexesWithoutClose() {
        for (String indexName : indexesToBePurged) {
            deleteIndex(client, indexName);
        }
        indexesToBePurged = new HashSet<>();
    }

    public void deleteIndexes() {
        for (String indexName : indexesToBePurged) {
            deleteIndex(client, indexName);
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

    public void handleAfter(Set<String> collections) {
        after(collections);
    }

    private void after(Set<String> indexesToBePurged) {
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
    public static int getTcpPort() {
        return TCP_PORT;
    }

    /**
     * get the Client
     *
     * @return the client
     */
    public Client getClient() {
        return client;
    }

    public void close() {
        client.close();
        clientClosed = true;
    }
}
