package fr.gouv.vitam.metadata.rest;

import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.junit.JunitHelper.ElasticsearchTestConfiguration;
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

    private final static String CLUSTER_NAME = "vitam-cluster";
    private static ElasticsearchTestConfiguration config = null;

    @BeforeClass
    public static void setup() throws IOException {
        // ES
        try {
            config = JunitHelper.startElasticsearchForTest(tempFolder, CLUSTER_NAME);
        } catch (final VitamApplicationServerException e1) {
            assumeTrue(false);
        }
        junitHelper = JunitHelper.getInstance();
        port = junitHelper.findAvailablePort();

        // Starting the embedded services within temporary dir
        mongo = new MongoEmbeddedService(
            DATABASE_HOST + ":" + port, databaseName, user, pwd, "localreplica");
        mongo.start();

        metadata = PropertiesUtils.findFile(METADATA_CONF);
        metadataConfig = PropertiesUtils.readYaml(metadata, MetaDataConfiguration.class);
        metadataConfig.getMongoDbNodes().get(0).setDbPort(port);
        metadataConfig.getElasticsearchNodes().get(0).setTcpPort(config.getTcpPort());

        final int metadataPort = junitHelper.findAvailablePort();
        SystemPropertyUtil.set("jetty.port", metadataPort);

    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        if (config == null) {
            return;
        }
        mongo.stop();
        junitHelper.releasePort(port);
        JunitHelper.stopElasticsearchForTest(config);
    }


    @Test
    // FIXME : MongoEmbeddedService lib not compatible with readConcern MAJORITY mongo configuration
    @Ignore
    public void testLauchApplication() throws Exception {
        final File newConf = File.createTempFile("test", METADATA_CONF, metadata.getParentFile());
        PropertiesUtils.writeYaml(newConf, metadataConfig);
        final MetadataMain application = new MetadataMain(newConf.getAbsolutePath());
        newConf.delete();
        application.stop();
    }

}
