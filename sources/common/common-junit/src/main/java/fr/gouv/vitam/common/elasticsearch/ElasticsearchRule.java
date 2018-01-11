/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.junit.NodeWithPlugins;
import org.apache.commons.io.FileUtils;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.node.InternalSettingsPreparer;
import org.elasticsearch.node.NodeValidationException;
import org.elasticsearch.plugin.analysis.icu.AnalysisICUPlugin;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.transport.Netty4Plugin;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.junit.rules.ExternalResource;

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;

/**
 */
public class ElasticsearchRule extends ExternalResource {

    private static int tcpPort;
    private static int httpPort;
    private File temporaryFolder;

    public static final String VITAM_CLUSTER = "vitam-cluster";

    synchronized void init() {
        if (tcpPort > 0) {
            return;
        }

        tcpPort = JunitHelper.getInstance().findAvailablePort();
        httpPort = JunitHelper.getInstance().findAvailablePort();

        try {
            final Settings settings = Settings.builder()
                .put("http.enabled", true)
                .put("http.type", "netty4")
                .put("transport.tcp.port", tcpPort)
                .put("transport.type", "netty4")
                .put("http.port", httpPort)
                .put("cluster.name", VITAM_CLUSTER)
                .put("path.home", temporaryFolder.getCanonicalPath())
                .put("plugin.mandatory", "org.elasticsearch.plugin.analysis.icu.AnalysisICUPlugin")
                .put("transport.tcp.connect_timeout", "1s")
                .put("transport.profiles.tcp.connect_timeout", "1s")
                .put("thread_pool.search.size", 4)
                .put("thread_pool.search.queue_size", VitamConfiguration.getNumberEsQueue())
                .put("thread_pool.bulk.queue_size", VitamConfiguration.getNumberEsQueue())
                .build();

            final List<Class<? extends Plugin>> plugins = Arrays.asList(Netty4Plugin.class, AnalysisICUPlugin.class);
            NodeWithPlugins node =
                new NodeWithPlugins(InternalSettingsPreparer.prepareEnvironment(settings, null), plugins);

            node.start();

        } catch (IOException | NodeValidationException e) {
            throw new RuntimeException(e);
        }
    }

    public ElasticsearchRule(File tempFolder, String... collectionNames) {

        try {
            this.temporaryFolder = tempFolder;
            init();

            client = new PreBuiltTransportClient(getClientSettings()).addTransportAddress(
                new InetSocketTransportAddress(InetAddress.getByName("localhost"), tcpPort));
        } catch (final UnknownHostException e) {
            throw new RuntimeException(e);
        }
        this.collectionNames = Arrays.asList(collectionNames);

        // TODO: 12/13/17 create index for each collection
    }

    private Settings getClientSettings() {
        return Settings.builder().put("cluster.name", VITAM_CLUSTER)
            .put("client.transport.sniff", true)
            .put("client.transport.ping_timeout", "2s")
            .put("transport.tcp.connect_timeout", "1s")
            .put("transport.profiles.client.connect_timeout", "1s")
            .put("transport.profiles.tcp.connect_timeout", "1s")
            .put("thread_pool.refresh.max", VitamConfiguration.getNumberDbClientThread())
            .put("thread_pool.search.size", VitamConfiguration.getNumberDbClientThread())
            .put("thread_pool.search.queue_size", VitamConfiguration.getNumberEsQueue())
            .put("thread_pool.bulk.queue_size", VitamConfiguration.getNumberEsQueue())
            .build();
    }


    private Client client;
    private List<String> collectionNames;

    @Override
    protected void after() {
        for (String collectionName : collectionNames) {

            if (client.admin().indices().prepareExists(collectionName.toLowerCase()).get().isExists()) {
                QueryBuilder qb = matchAllQuery();

                SearchResponse scrollResp = client.prepareSearch(collectionName.toLowerCase())
                    .addSort(FieldSortBuilder.DOC_FIELD_NAME, SortOrder.ASC)
                    .setScroll(new TimeValue(60000))
                    .setQuery(qb)
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
    }

    /**
     * Used when annotated @ClassRule
     */
    public void handleAfter() {
        after();
    }

    /**
     * Used when annotated @ClassRule
     */
    public void afterClass() {

        if (null != temporaryFolder && temporaryFolder.exists()) {
            try {
                // if clean offer delete did not work
                FileUtils.cleanDirectory(temporaryFolder);
                FileUtils.deleteDirectory(temporaryFolder);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public String getClusterName() {
        return VITAM_CLUSTER;
    }

    public static int getTcpPort() {
        return tcpPort;
    }

    public Client getClient() {
        return client;
    }
}
