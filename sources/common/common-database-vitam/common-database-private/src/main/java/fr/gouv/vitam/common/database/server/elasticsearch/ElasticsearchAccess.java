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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.application.configuration.DatabaseConnection;

/**
 * Elasticsearch Access
 */
public class ElasticsearchAccess implements DatabaseConnection {

    private static final int TOSECOND = 1000;

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ElasticsearchAccess.class);

    private static final String DEFAULT_FRENCH_STOP_WORDS = "\"_french_\"";
    
    private static final String FRENCH_STOP_WORDS = "[\"vers\", \"a\", \"à\", \"afin\", \"ai\", \"ainsi\", \"après\", \"au\", \"auquel\", \"aussi\", " +
        "\"autre\", \"autres\", \"aux\", \"auxquelles\", \"auxquels\", \"avait\", \"c\", \"ça\", \"ce\", \"ceci\", \"cela\", \"celle\", \"celles\", " +
        "\"celui\", \"cependant\", \"certain\", \"certaine\", \"certaines\", \"certains\", \"ces\", \"cet\", \"cette\", \"ceux\", \"chez\", \"ci\", " +
        "\"combien\", \"comme\", \"comment\", \"concernant\", \"contre\", \"d\", \"dans\", \"de\", \"dedans\", \"dehors\", \"delà\", \"depuis\", " +
        "\"des\", \"dès\", \"désormais\", \"desquelles\", \"desquels\", \"dessous\", \"dessus\", \"devant\", \"devers\", \"devra\", \"divers\", " +
        "\"diverse\", \"diverses\", \"doit\", \"donc\", \"dont\", \"du\", \"duquel\", \"durant\", \"elle\", \"elles\", \"en\", \"entre\", " +
        "\"environ\", \"et\", \"etc\", \"etre\", \"être\", \"eu\", \"eux\", \"hélas\", \"hormis\", \"hors\", \"hui\", \"il\", \"ils\", \"j\", \"je\", " +
        "\"jusqu\", \"jusque\", \"l\", \"la\", \"là\", \"laquelle\", \"le\", \"lequel\", \"les\", \"lesquelles\", \"lesquels\", \"leur\", \"leurs\", " +
        "\"lorsque\", \"lui\", \"ma\", \"mais\", \"malgré\", \"me\", \"même\", \"mêmes\", \"mes\", \"mien\", \"mienne\", \"miennes\", \"miens\", " +
        "\"moi\", \"moins\", \"mon\", \"moyennant\", \"n\", \"ne\", \"néanmoins\", \"ni\", \"non\", \"nos\", \"notre\", \"nôtre\", \"nôtres\", " +
        "\"nous\", \"ô\", \"on\", \"ont\", \"ou\", \"où\", \"outre\", \"par\", \"parmi\", \"pas\", \"pendant\", \"plein\", \"plus\", \"plusieurs\", " +
        "\"pour\", \"pourquoi\", \"près\", \"proche\", \"puisque\", \"qu\", \"quand\", \"que\", \"quel\", \"quelle\", \"quelles\", \"quels\", \"qui\", " +
        "\"quoi\", \"quoique\", \"revoici\", \"revoilà\", \"s\", \"sa\", \"sauf\", \"se\", \"selon\", \"seront\", \"ses\", \"si\", \"sien\", \"sienne\", " +
        "\"siennes\", \"siens\", \"sinon\", \"soi\", \"soit\", \"son\", \"sont\", \"sous\", \"suivant\", \"sur\", \"ta\", \"te\", \"tes\", \"tien\", " +
        "\"tienne\", \"tiennes\", \"tiens\", \"toi\", \"ton\", \"tous\", \"tout\", \"toute\", \"toutes\", \"tu\", \"un\", \"une\", \"va\", \"voici\", " +
        "\"voilà\", \"vos\", \"votre\", \"vôtre\", \"vôtres\", \"vous\", \"vu\", \"y\"]";
    /**
     * Default Index Configuration
     */
    public static final String DEFAULT_INDEX_CONFIGURATION = "{\"analysis\":{" +
        "\"analyzer\": {" +
        "\"default\": {\"type\":\"custom\",\"tokenizer\":\"icu_tokenizer\"," +
        "\"filter\":[\"stopwords\",\"asciifolding\",\"lowercase\",\"snowball\",\"elision\",\"worddelimiter\"]," +
        "\"char_filter\": [\"dotreplace\", \"html_strip\"]}," +
        "\"default_search\":{\"type\":\"custom\",\"tokenizer\":\"icu_tokenizer\"," +
        "\"filter\":[\"stopwords\",\"asciifolding\",\"lowercase\",\"snowball\",\"elision\",\"worddelimiter\"]," +
        "\"char_filter\": [\"dotreplace\", \"html_strip\"]}}," +
        "\"char_filter\": {\"dotreplace\":{\"type\":\"pattern_replace\",\"pattern\":\"\\\\.\", \"replacement\": \" \"}}," +
        "\"tokenizer\":{\"letter\":{\"type\":\"letter\"}}," +
        "\"filter\":{" +
        "\"snowball\":{\"type\":\"snowball\",\"language\":\"French\"}," +
        "\"elision\":{\"type\":\"elision\",\"articles\":[\"l\",\"m\",\"t\",\"qu\",\"n\",\"s\",\"j\",\"d\",\"jusqu\",\"quoiqu\",\"lorsqu\",\"puisqu\"]}," +
        "\"stopwords\":{\"type\":\"stop\",\"stopwords\":" + DEFAULT_FRENCH_STOP_WORDS + ",\"ignore_case\":true}," +
        "\"worddelimiter\":{\"type\":\"word_delimiter\"}}}}";

    protected final TransportClient client;
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

        ParametersChecker.checkParameter("clusterName, elasticsearch nodes list are a mandatory parameters",
            clusterName, nodes);

        if (nodes.isEmpty()) {
            throw new VitamException("elasticsearch nodes list is empty");
        }

        this.clusterName = clusterName;
        this.nodes = nodes;

        final Settings settings = getSettings();

        client = getClient(settings);
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
    private Settings getSettings() {
        return Settings.settingsBuilder().put("cluster.name", clusterName)
            .put("client.transport.sniff", true)
            .put("client.transport.ping_timeout", "2s")
            .put("transport.tcp.connect_timeout", "1s")
            .put("transport.profiles.client.connect_timeout", "1s")
            .put("transport.profiles.tcp.connect_timeout", "1s")
            .put("watcher.http.default_read_timeout", VitamConfiguration.getReadTimeout() / TOSECOND + "s")
            .build();
    }

    private TransportClient getClient(Settings settings) throws VitamException {
        try {
            final TransportClient clientNew = TransportClient.builder().settings(settings).build();
            for (final ElasticsearchNode node : nodes) {
                clientNew.addTransportAddress(
                    new InetSocketTransportAddress(InetAddress.getByName(node.getHostName()), node.getTcpPort()));
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
        client.close();
    }

    /**
     *
     * @return the Cluster Name
     */
    public String getClusterName() {
        return clusterName;
    }

    /**
     * @return the client
     */
    public Client getClient() {
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
        try (TransportClient clientCheck = getClient(getSettings())) {
            return !clientCheck.connectedNodes().isEmpty();
        } catch (final VitamException e) {
            LOGGER.warn(e);
            return false;
        }
    }

    @Override
    public String toString() {
        return clusterName;
    }
    
    public final boolean addIndex(String collectionName, String mapping, String type) {
        if (!client.admin().indices().prepareExists(collectionName).get().isExists()) {
            try {
                LOGGER.debug("createIndex");
                LOGGER.debug("setMapping: " + collectionName + " type: " + type + "\n\t" + mapping);
                final CreateIndexResponse response =
                    client.admin().indices().prepareCreate(collectionName)
                    .setSettings(Settings.builder().loadFromSource(DEFAULT_INDEX_CONFIGURATION))
                    .addMapping(type, mapping)
                    .get();
                if (!response.isAcknowledged()) {
                    LOGGER.error(type + ":" + response.isAcknowledged());
                    return false;
                }
            } catch (final Exception e) {
                LOGGER.error("Error while set Mapping", e);
                return false;
            }
        }
        return true;
    }
}
