/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.elasticsearch;

import com.google.common.collect.Sets;
import fr.gouv.vitam.common.VitamConfiguration;
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


    public ElasticsearchRule(String... collectionNames) {
        try {
            client = new PreBuiltTransportClient(getClientSettings()).addTransportAddress(
                new TransportAddress(InetAddress.getByName("localhost"), TCP_PORT));
        } catch (final UnknownHostException e) {
            throw new RuntimeException(e);
        }

        if (null != collectionNames) {
            this.collectionNames = Sets.newHashSet(collectionNames);

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


    private Client client;
    private Set<String> collectionNames = new HashSet<>();

    @Override
    protected void after() {
        if (!clientClosed) {
            purge(client, collectionNames);
        }
    }

    private void purge(Client client, Collection<String> collectionNames) {
        for (String collectionName : collectionNames) {

            purge(client, collectionName);
        }
    }


    public static void purge(Client client, String collectionName) {
        if (client.admin().indices().prepareExists(collectionName.toLowerCase()).get().isExists()) {
            QueryBuilder qb = matchAllQuery();

            SearchResponse scrollResp = client.prepareSearch(collectionName.toLowerCase())
                .addSort(FieldSortBuilder.DOC_FIELD_NAME, SortOrder.ASC)
                .setScroll(new TimeValue(60000))
                .setQuery(qb)
                .setFetchSource(false)
                .setSize(100).get();

            BulkRequestBuilder bulkRequest = client.prepareBulk();

            do {
                for (SearchHit hit : scrollResp.getHits().getHits()) {
                    bulkRequest.add(client.prepareDelete(collectionName.toLowerCase(), "typeunique", hit.getId()));
                }

                scrollResp =
                    client.prepareSearchScroll(scrollResp.getScrollId()).setScroll(new TimeValue(60000)).execute()
                        .actionGet();
            } while (scrollResp.getHits().getHits().length != 0);

            bulkRequest.request().setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);

            if (bulkRequest.request().numberOfActions() != 0) {
                BulkResponse bulkResponse = bulkRequest.get();

                if (bulkResponse.hasFailures()) {
                    throw new RuntimeException(
                        String.format("DatabaseException when calling purge by bulk Request %s",
                            bulkResponse.buildFailureMessage()));
                }
            }
        }
    }


    public final void deleteIndex(Client client, String collectionName) {
        purge(client, collectionName);
       /* try {
            if (client.admin().indices().prepareExists(collectionName).get().isExists()) {

                String indexName = collectionName;
                ImmutableOpenMap<String, List<AliasMetaData>>
                    alias = client.admin().indices().prepareGetAliases(collectionName).get().getAliases();
                if (alias.size() > 0) {
                    indexName = alias.iterator().next().key;
                }
                DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest(indexName);

                if (!client.admin().indices().delete(deleteIndexRequest).get().isAcknowledged()) {
                    SysErrLogger.FAKE_LOGGER.syserr("Index :" + collectionName + " not deleted");
                }
            }
        } catch (final Exception e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);

        }*/
    }

    public void deleteIndexesWithoutClose() {
        for (String collectionName : collectionNames) {
            deleteIndex(client, collectionName);
        }
        collectionNames = new HashSet<>();
    }

    public void deleteIndexes() {
        for (String collectionName : collectionNames) {
            deleteIndex(client, collectionName);
        }
        collectionNames = new HashSet<>();
        close();
    }

    // Add index to be purged
    public ElasticsearchRule addIndexToBePurged(String indexName) {
        collectionNames.add(indexName);
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

    private void after(Set<String> collections) {
        purge(client, collections);
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
