package fr.gouv.vitam.logbook.rest;

import static org.junit.Assume.assumeTrue;

import java.io.File;

import org.jhades.JHades;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.junit.JunitHelper.ElasticsearchTestConfiguration;
import fr.gouv.vitam.logbook.common.server.LogbookConfiguration;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbAccessImpl;
import ru.yandex.qatools.embed.service.MongoEmbeddedService;

public class LogbookApplicationAuthenticationTest {
    private static final String DATABASE_HOST = "localhost";
    static LogbookMongoDbAccessImpl mongoDbAccess;
    private static int port;
    private static JunitHelper junitHelper;
    private static MongoEmbeddedService mongo;
    private static final String databaseName = "db-logbook";
    private static final String user = "user-logbook";
    private static final String pwd = "user-logbook";
    private static final String LOGBOOK_CONF = "logbook-auth-test.conf";

    // ES
    @ClassRule
    public static TemporaryFolder esTempFolder = new TemporaryFolder();
    private final static String ES_CLUSTER_NAME = "vitam-cluster";
    private static ElasticsearchTestConfiguration config = null;
    
    private static File logbook;
    private static LogbookConfiguration realLogbook;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        new JHades().overlappingJarsReport();

        junitHelper = JunitHelper.getInstance();
        port = junitHelper.findAvailablePort();

        // Starting the embedded services within temporary dir
        mongo = new MongoEmbeddedService(
            DATABASE_HOST + ":" + port, databaseName, user, pwd, "localreplica");
        mongo.start();
        // ES
        try {
            config = JunitHelper.startElasticsearchForTest(esTempFolder, ES_CLUSTER_NAME);
        } catch (final VitamApplicationServerException e1) {
            assumeTrue(false);
        }

        logbook = PropertiesUtils.findFile(LOGBOOK_CONF);
        realLogbook = PropertiesUtils.readYaml(logbook, LogbookConfiguration.class);
        realLogbook.getMongoDbNodes().get(0).setDbPort(port);
        realLogbook.getElasticsearchNodes().get(0).setTcpPort(config.getTcpPort());       
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        if (config != null) {
            JunitHelper.stopElasticsearchForTest(config);
        }
        mongo.stop();
        junitHelper.releasePort(port);
    }

    @Test
    public void testApplicationLaunch() {
        new LogbookApplication(realLogbook);
    }

}
