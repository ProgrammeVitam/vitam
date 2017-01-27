package fr.gouv.vitam.functional.administration.rest;

import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jhades.JHades;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.junit.JunitHelper.ElasticsearchTestConfiguration;
import fr.gouv.vitam.functional.administration.common.server.AdminManagementConfiguration;
import fr.gouv.vitam.functional.administration.common.server.ElasticsearchAccessFunctionalAdmin;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import ru.yandex.qatools.embed.service.MongoEmbeddedService;

public class AdminManagementApplicationAuthenticationTest {

    private static final String SHOULD_NOT_RAIZED_AN_EXCEPTION = "Should not raise an exception";
    private static final String DATABASE_HOST = "localhost";
    static MongoDbAccessAdminImpl mongoDbAccess;
    private static int databasePort;

    private static JunitHelper junitHelper;
    private static MongoEmbeddedService mongo;
    private static final String databaseName = "db-functional-administration";
    private static final String user = "user-functional-administration";
    private static final String pwd = "user-functional-administration";
    private static final String ADMIN_MANAGEMENT_CONF = "functional-administration-auth-test.conf";

    static AdminManagementConfiguration configuration;
    private static ElasticsearchTestConfiguration configEs = null;

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();
    
    private static File adminConfigFile;
    private final static String CLUSTER_NAME = "vitam-cluster";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        new JHades().overlappingJarsReport();

        junitHelper = JunitHelper.getInstance();
        databasePort = junitHelper.findAvailablePort();
        
        // ES
        try {
            configEs = JunitHelper.startElasticsearchForTest(tempFolder, CLUSTER_NAME);
        } catch (final VitamApplicationServerException e1) {
            assumeTrue(false);
        }

        final List<ElasticsearchNode> nodesEs = new ArrayList<>();
        nodesEs.add(new ElasticsearchNode("localhost", configEs.getTcpPort()));

        final File adminConfig = PropertiesUtils.findFile(ADMIN_MANAGEMENT_CONF);
        final AdminManagementConfiguration realAdminConfig =
            PropertiesUtils.readYaml(adminConfig, AdminManagementConfiguration.class);
        realAdminConfig.getMongoDbNodes().get(0).setDbPort(databasePort);
        realAdminConfig.setElasticsearchNodes(nodesEs);
        realAdminConfig.setClusterName(CLUSTER_NAME);
        adminConfigFile = File.createTempFile("test", ADMIN_MANAGEMENT_CONF, adminConfig.getParentFile());
        PropertiesUtils.writeYaml(adminConfigFile, realAdminConfig);
        
        // Starting the embedded services within temporary dir
        mongo = new MongoEmbeddedService(
            DATABASE_HOST + ":" + databasePort, databaseName, user, pwd, "localreplica");
        mongo.start();

    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        mongo.stop();
        junitHelper.releasePort(databasePort);
    }

    @Test
    public void testApplicationLaunch() throws IOException, VitamException {
        try {
            new AdminManagementApplication(adminConfigFile.getAbsolutePath());
        } catch (final IllegalStateException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        }
    }

}
