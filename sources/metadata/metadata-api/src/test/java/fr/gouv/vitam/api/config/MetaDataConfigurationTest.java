package fr.gouv.vitam.api.config;

import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class MetaDataConfigurationTest {

    private static final String HOST = "host";
    private static final int PORT = 1234;
    private static final String DB_NAME = "dbNameTest";
    private static final String JETTY_CONF = "jettyConfig";
    private static final String JETTY_CONF_FILE = "jetty-config-test.xml";

    private final static String CLUSTER_NAME = "cluster-vitam";
    private final static String HOST_NAME = "localhost";
    private static int TCP_PORT = 9300;


    @Test
    public void testSetterGetter() {
        MetaDataConfiguration config1 = new MetaDataConfiguration();
        assertEquals(config1.setHost(HOST).getHost(), HOST);
        assertEquals(config1.setPort(PORT).getPort(), PORT);
        assertEquals(config1.setDbName(DB_NAME).getDbName(), DB_NAME);
        assertEquals(JETTY_CONF, config1.setJettyConfig(JETTY_CONF).getJettyConfig());
        assertEquals(CLUSTER_NAME, config1.setClusterName(CLUSTER_NAME).getClusterName());

        List<ElasticsearchNode> nodes = new ArrayList<ElasticsearchNode>();
        nodes.add(new ElasticsearchNode(HOST_NAME, TCP_PORT));
        assertEquals(1, config1.setElasticsearchNodes(nodes).getElasticsearchNodes().size());

        MetaDataConfiguration config2 =
            new MetaDataConfiguration(HOST, PORT, DB_NAME, CLUSTER_NAME, nodes, JETTY_CONF_FILE);
        assertEquals(config2.getHost(), HOST);
        assertEquals(config2.getPort(), PORT);
        assertEquals(config2.getDbName(), DB_NAME);
        assertEquals(config2.getClusterName(), CLUSTER_NAME);
        assertEquals(config2.getElasticsearchNodes().size(), 1);
        assertEquals(config2.getJettyConfig(), JETTY_CONF_FILE);
    }
}
