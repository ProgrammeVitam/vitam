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

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.junit.JunitHelper;

public class ElasticsearchAccessTest {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    private File elasticsearchHome;

    private final static String CLUSTER_NAME = "cluster-vitam";
    private final static String HOST_NAME = "localhost";
    private static int TCP_PORT = 9300;
    private static int HTTP_PORT = 9200;
    private static Node node;

    private static JunitHelper junitHelper;


    @Before
    public void setup() throws IOException {

        junitHelper = new JunitHelper();
        TCP_PORT = junitHelper.findAvailablePort();
        HTTP_PORT = junitHelper.findAvailablePort();

        elasticsearchHome = tempFolder.newFolder();
        Settings settings = Settings.settingsBuilder()
            .put("http.enabled", true)
            .put("discovery.zen.ping.multicast.enabled", false)
            .put("transport.tcp.port", TCP_PORT)
            .put("http.port", HTTP_PORT)
            .put("path.home", elasticsearchHome.getCanonicalPath())
            .build();

        node = nodeBuilder()
            .settings(settings)
            .client(false)
            .clusterName(CLUSTER_NAME)
            .node();

        node.start();
    }


    @After
    public void tearDownAfterClass() {
        if (node != null) {
            node.close();
        }

        junitHelper.releasePort(TCP_PORT);
        junitHelper.releasePort(HTTP_PORT);

    }

    @Test
    public void testElasticsearchAccess() throws VitamException {
        List<ElasticsearchNode> nodes = new ArrayList<ElasticsearchNode>();
        nodes.add(new ElasticsearchNode(HOST_NAME, TCP_PORT));

        ElasticsearchAccess elastic = new ElasticsearchAccess(CLUSTER_NAME, nodes);
        assertEquals(CLUSTER_NAME, elastic.getClusterName());
        assertEquals(nodes, elastic.getNodes());
        assertNotNull(elastic.getClient());
        elastic.close();

    }
}
