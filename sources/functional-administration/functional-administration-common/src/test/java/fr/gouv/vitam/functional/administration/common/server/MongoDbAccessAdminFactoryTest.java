package fr.gouv.vitam.functional.administration.common.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.junit.JunitHelper.ElasticsearchTestConfiguration;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import ru.yandex.qatools.embed.service.MongoEmbeddedService;

public class MongoDbAccessAdminFactoryTest {
    
    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();
    
    private final static String CLUSTER_NAME = "vitam-cluster";
    private static final String DATABASE_HOST = "localhost";
    static MongoDbAccessAdminImpl mongoDbAccess;
    private static int port;
    private static JunitHelper junitHelper;
    private static MongoEmbeddedService mongo;
    private static final String databaseName = "db-functional-administration";
    private static final String user = "user-functional-administration";
    private static final String pwd = "user-functional-administration";
    private static ElasticsearchTestConfiguration esConfig = null;    
    private final static String HOST_NAME = "127.0.0.1";
    private static ElasticsearchAccessFunctionalAdmin esClient;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        junitHelper = JunitHelper.getInstance();
        port = junitHelper.findAvailablePort();

        try {
            esConfig = JunitHelper.startElasticsearchForTest(tempFolder, CLUSTER_NAME);
        } catch (final VitamApplicationServerException e1) {
            assumeTrue(false);
        }


        final List<ElasticsearchNode> esNodes = new ArrayList<>();
        esNodes.add(new ElasticsearchNode(HOST_NAME, esConfig.getTcpPort()));

        esClient = new ElasticsearchAccessFunctionalAdmin(CLUSTER_NAME, esNodes);
        // Starting the embedded services within temporary dir
        mongo = new MongoEmbeddedService(
            DATABASE_HOST + ":" + port, databaseName, user, pwd, "localreplica");
        mongo.start();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        if (esConfig != null) {
            JunitHelper.stopElasticsearchForTest(esConfig);
        }
        esClient.close();
        mongo.stop();
        junitHelper.releasePort(port);
    }

    @Test
    public void testCreateAdmin() {
        final List<MongoDbNode> nodes = new ArrayList<>();
        nodes.add(new MongoDbNode(DATABASE_HOST, port));
        mongoDbAccess = MongoDbAccessAdminFactory.create(
            new DbConfigurationImpl(nodes, databaseName, true, user, pwd));
        assertNotNull(mongoDbAccess);
        assertEquals("db-functional-administration", mongoDbAccess.getMongoDatabase().getName());
        mongoDbAccess.close();
    }
}
