/*******************************************************************************
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
 *******************************************************************************/
package fr.gouv.vitam.common.database.server.elasticsearch;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.application.configuration.DatabaseConnection;
import org.elasticsearch.ResourceAlreadyExistsException;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesResponse;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.Settings.Builder;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;

/**
 * Elasticsearch Access
 */
public class ElasticsearchAccess implements DatabaseConnection {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ElasticsearchAccess.class);

    /**
     * The ES Builder
     */
    public Builder default_builder;

    private static String ES_CONFIGURATION_FILE = "/elasticsearch-configuration.json";
    private AtomicReference<Client> esClient = new AtomicReference<>();
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
        String indexName = collectionName + "_" + tenant;
        purgeIndex(indexName);
    }

    public void purgeIndex(String indexName) {
        if (getClient().admin().indices().prepareExists(indexName.toLowerCase()).get().isExists()) {
            QueryBuilder qb = matchAllQuery();

            SearchResponse scrollResp = getClient().prepareSearch(indexName.toLowerCase())
                .addSort(FieldSortBuilder.DOC_FIELD_NAME, SortOrder.ASC)
                .setScroll(new TimeValue(60000))
                .setQuery(qb)
                .setFetchSource(false)
                .setSize(100).get();

            BulkRequestBuilder bulkRequest = getClient().prepareBulk();

            do {
                for (SearchHit hit : scrollResp.getHits().getHits()) {
                    bulkRequest.add(getClient().prepareDelete(indexName.toLowerCase(), "typeunique", hit.getId()));
                }

                scrollResp =
                    getClient().prepareSearchScroll(scrollResp.getScrollId()).setScroll(new TimeValue(60000)).execute()
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

    /**
     * Production settings, see Elasticsearch production settings
     * https://www.elastic.co/guide/en/elasticsearch/guide/current/deploy.html.</br>
     * </br>
     * Additionnal on server side:</br>
     * in sysctl "vm.swappiness = 1", "vm.max_map_count=262144"</br>
     * in elasticsearch.yml "bootstrap.mlockall: true"
     *
     * @return Settings for Elasticsearch client
     */
    public static Settings getSettings(String clusterName) {
        return Settings.builder().put("cluster.name", clusterName)
            .put("client.transport.sniff", true)
            .put("client.transport.ping_timeout", "1s")
            .put("transport.tcp.connect_timeout", "30s")
            // Note : thread_pool.refresh.size is now limited to max(half number of processors, 10)... that is the
            // default max value. So no configuration is needed.
            .put("thread_pool.refresh.max", VitamConfiguration.getNumberDbClientThread())
            .put("thread_pool.search.size", VitamConfiguration.getNumberDbClientThread())
            .put("thread_pool.search.queue_size", VitamConfiguration.getNumberEsQueue())
            // thread_pool.bulk.size is now boundedNumberOfProcessors() ; the default value is the maximum allowed (+1),
            // so no configuration is needed.
            // In addition, if the configured size is >= (1 + # of available processors), the threadpool creation fails.
            // .put("thread_pool.bulk.size", VitamConfiguration.getNumberDbClientThread())
            .put("thread_pool.bulk.queue_size", VitamConfiguration.getNumberEsQueue())
            // watcher settings are now part of X-pack (paid license) and can be configured once installed with the
            // corresponding xpack.http.default_read_timeout
            // .put("watcher.http.default_read_timeout", VitamConfiguration.getReadTimeout() / TOSECOND + "s")
            .build();
    }

    private TransportClient getClient(Settings settings) throws VitamException {
        try {
            final TransportClient clientNew = new PreBuiltTransportClient(settings);
            for (final ElasticsearchNode node : nodes) {
                clientNew.addTransportAddress(
                    new TransportAddress(InetAddress.getByName(node.getHostName()), node.getTcpPort()));
            }
            return clientNew;
        } catch (final UnknownHostException e) {
            LOGGER.error(e.getMessage(), e);
            throw new VitamException(e.getMessage());
        }
    }

    /**
     * Close the ElasticSearch connection
     */
    public void close() {
        getClient().close();
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
    public Client getClient() {
        Client client = esClient.get();
        if (null == client) {
            synchronized (this) {
                if (null == esClient.get()) {
                    try {
                        client = getClient(getSettings(clusterName));
                        esClient.set(client);
                    } catch (VitamException e) {
                        throw new RuntimeException("Error while get ES client", e);
                    }
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
        try (TransportClient clientCheck = getClient(getSettings(clusterName))) {
            return !clientCheck.connectedNodes().isEmpty();
        } catch (final Exception e) {
            LOGGER.warn(e);
            return false;
        }
    }

    @Override
    public String getInfo() {
        return clusterName;
    }

    /**
     * Create an index and alias for a collection (if the alias does not exist)
     *
     * @param collectionName the name of the collection
     * @param mapping the mapping as a string
     * @param type the type of the collection
     * @param tenantId the tenant on which to create the index
     * @return key aliasName value indexName or empty
     */
    public final Map<String, String> createIndexAndAliasIfAliasNotExists(String collectionName, String mapping,
        String type,
        Integer tenantId) {
        String indexName = getUniqueIndexName(collectionName, tenantId);
        String aliasName = getAliasName(collectionName, tenantId);
        LOGGER.debug("addIndex: {}", indexName);
        if (!getClient().admin().indices().prepareExists(aliasName).get().isExists()) {
            try {
                LOGGER.debug("createIndex");
                LOGGER.debug("setMapping: " + indexName + " type: " + type + "\n\t" + mapping);
                try {
                    final CreateIndexResponse response = getClient().admin().indices()
                        .prepareCreate(indexName)
                        .setSettings(default_builder)
                        .addMapping(type, mapping, XContentType.JSON).get();

                    if (!response.isAcknowledged()) {
                        LOGGER.error("Error creating index for " + type + " / collection : " + collectionName);
                        return new HashMap<>();
                    }
                } catch (ResourceAlreadyExistsException e) {
                    // Continue if index already exists
                    LOGGER.warn(e);
                }

                AcknowledgedResponse indAliasesResponse = getClient().admin().indices()
                    .prepareAliases().addAlias(indexName, aliasName).execute().get();

                if (!indAliasesResponse.isAcknowledged()) {
                    LOGGER.error("Error creating alias for " + type + " / collection : " + collectionName);
                    return new HashMap<>();
                }
            } catch (final Exception e) {
                LOGGER.error("Error while set Mapping", e);
                return new HashMap<>();
            }
        }
        Map<String, String> map = new HashMap<>();
        map.put(aliasName, indexName);
        return map;
    }

    /**
     * Create an index without a linked alias
     *
     * @param collectionName
     * @param mapping
     * @param type
     * @param tenantId
     * @return the newly created index Name
     * @throws DatabaseException
     */
    public final String createIndexWithoutAlias(String collectionName, String mapping, String type,
        Integer tenantId)
        throws DatabaseException {
        String indexName = getUniqueIndexName(collectionName, tenantId);
        // Retrieve alias
        LOGGER.debug("createIndex");
        LOGGER.debug("setMapping: " + indexName + " type: " + type + "\n\t" + mapping);
        final CreateIndexResponse response = getClient().admin().indices()
            .prepareCreate(indexName)
            .setSettings(default_builder)
            .addMapping(type, mapping, XContentType.JSON).get();
        if (!response.isAcknowledged()) {
            String message = "Database Exception for type " + type + " / collection : " + collectionName;
            LOGGER.error(message);
            throw new DatabaseException(message);
        }
        return indexName;
    }

    /**
     * Switch index
     *
     * @param aliasName
     * @param indexNameToSwitchWith
     * @throws DatabaseException
     */
    public final void switchIndex(String aliasName, String indexNameToSwitchWith)
        throws DatabaseException {
        GetAliasesResponse actualIndex =
            getClient().admin().indices().getAliases(new GetAliasesRequest().aliases(aliasName))
                .actionGet();
        String oldIndexName = null;
        for (Iterator<String> it = actualIndex.getAliases().keysIt(); it.hasNext(); ) {
            oldIndexName = it.next();
        }

        if (!getClient().admin().indices().prepareExists(aliasName).get().isExists()) {
            throw new DatabaseException(String.format("Alias not exist : %s", aliasName));
        }
        // RemoveAlias to the old index and Add alias to new index
        AcknowledgedResponse indAliasesResponse = getClient().admin().indices()
            .prepareAliases()
            .removeAlias(oldIndexName, aliasName)
            .addAlias(indexNameToSwitchWith, aliasName)
            .execute().actionGet();
        LOGGER.debug("aliasName %s", aliasName);

        if (!indAliasesResponse.isAcknowledged()) {
            final String message = "Switch Index error IndicesAliasesResponse " + indAliasesResponse.isAcknowledged();
            LOGGER.error(message);
            throw new DatabaseException(message);
        }
        // TODO Remove old index 3204 ?
    }

    /**
     * Settings method
     *
     * @return the builder
     * @throws IOException
     */
    public Builder settings() throws IOException {
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
