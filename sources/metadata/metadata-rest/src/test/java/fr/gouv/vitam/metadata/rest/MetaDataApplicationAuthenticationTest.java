package fr.gouv.vitam.metadata.rest;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import java.io.File;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.http.BindHttpException;
import org.elasticsearch.node.Node;
import org.jhades.JHades;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.metadata.api.config.MetaDataConfiguration;
import fr.gouv.vitam.metadata.core.database.collections.MongoDbAccessMetadataImpl;
import ru.yandex.qatools.embed.service.MongoEmbeddedService;

public class MetaDataApplicationAuthenticationTest {
    private static final String DATABASE_HOST = "localhost";
    static MongoDbAccessMetadataImpl mongoDbAccess;
    private static int port;
    private static JunitHelper junitHelper;
    private static MongoEmbeddedService mongo;
    private static final String databaseName = "db-metadata";
    private static final String user = "user-metadata";
    private static final String pwd = "user-metadata";
    private static final String METADATA_CONF = "metadata-auth.conf";
    private static File metadata;
    private static MetaDataConfiguration metadataConfig;
    
    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();
    private static File elasticsearchHome;

    private final static String CLUSTER_NAME = "vitam-cluster";
    private static int TCP_PORT = 9300;
    private static int HTTP_PORT = 9200;
    private static Node node;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        new JHades().overlappingJarsReport();

        junitHelper = JunitHelper.getInstance();
        port = junitHelper.findAvailablePort();

        // Starting the embedded services within temporary dir
        mongo = new MongoEmbeddedService(
            DATABASE_HOST + ":" + port, databaseName, user, pwd, "localreplica");
        mongo.start();

        metadata = PropertiesUtils.findFile(METADATA_CONF);
        metadataConfig = PropertiesUtils.readYaml(metadata, MetaDataConfiguration.class);
        metadataConfig.setDbPort(port);

        // ES
        elasticsearchHome = tempFolder.newFolder();
        for (int i = 0; i < 3; i++) {
            TCP_PORT = junitHelper.findAvailablePort();
            HTTP_PORT = junitHelper.findAvailablePort();

            try {
                final Settings settings = Settings.settingsBuilder()
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
            } catch (BindHttpException e) {
                junitHelper.releasePort(TCP_PORT);
                junitHelper.releasePort(HTTP_PORT);
                node = null;
                continue;
            }
        }        
        if (node == null) {
            return;
        }
        metadataConfig.getElasticsearchNodes().get(0).setTcpPort(TCP_PORT);
        
        final int metadataPort = junitHelper.findAvailablePort();
        SystemPropertyUtil.set("jetty.port", metadataPort);

    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        mongo.stop();
        junitHelper.releasePort(port);
        
        if (node != null) {
            node.close();
        }

        junitHelper.releasePort(TCP_PORT);
        junitHelper.releasePort(HTTP_PORT);
    }


    @Test
    public void testLauchApplication() throws Exception {
        final File newConf = File.createTempFile("test", METADATA_CONF, metadata.getParentFile());
        PropertiesUtils.writeYaml(newConf, metadataConfig);
        MetaDataApplication application = new MetaDataApplication(newConf.getAbsolutePath());
        newConf.delete();
        application.stop();
    }

}
