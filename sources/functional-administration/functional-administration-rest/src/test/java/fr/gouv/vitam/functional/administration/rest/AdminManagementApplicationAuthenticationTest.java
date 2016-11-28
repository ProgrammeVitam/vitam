package fr.gouv.vitam.functional.administration.rest;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.jhades.JHades;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.junit.JunitHelper;
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

    private static File admin;
    private static AdminManagementConfiguration config;

    private AdminManagementApplication application;
    static AdminManagementConfiguration configuration;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        new JHades().overlappingJarsReport();

        junitHelper = JunitHelper.getInstance();
        databasePort = junitHelper.findAvailablePort();

        // final File adminConfig = PropertiesUtils.findFile(ADMIN_MANAGEMENT_CONF);
        // final AdminManagementConfiguration realAdminConfig =
        // PropertiesUtils.readYaml(adminConfig, AdminManagementConfiguration.class);
        // realAdminConfig.setDbPort(databasePort);
        // adminConfigFile = File.createTempFile("test", ADMIN_MANAGEMENT_CONF, adminConfig.getParentFile());
        // PropertiesUtils.writeYaml(adminConfigFile, realAdminConfig);

        // Starting the embedded services within temporary dir
        mongo = new MongoEmbeddedService(
            DATABASE_HOST + ":" + databasePort, databaseName, user, pwd, "localreplica");
        mongo.start();

        admin = PropertiesUtils.findFile(ADMIN_MANAGEMENT_CONF);
        config = PropertiesUtils.readYaml(admin, AdminManagementConfiguration.class);
        config.getMongoDbNodes().get(0).setDbPort(databasePort);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        mongo.stop();
        junitHelper.releasePort(databasePort);
    }

    @Test
    public void testApplicationLaunch() throws IOException, VitamException {
        try {
            new AdminManagementApplication(admin.getAbsolutePath());
        } catch (final IllegalStateException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        }
    }

}
