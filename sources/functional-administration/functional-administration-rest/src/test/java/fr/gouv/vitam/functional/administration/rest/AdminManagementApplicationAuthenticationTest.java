package fr.gouv.vitam.functional.administration.rest;

import java.io.File;
import java.io.IOException;

import org.jhades.JHades;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import ru.yandex.qatools.embed.service.MongoEmbeddedService;

public class AdminManagementApplicationAuthenticationTest {

    private static final String DATABASE_HOST = "localhost";
    static MongoDbAccessAdminImpl mongoDbAccess;
    private static int port;
    private static JunitHelper junitHelper;
    private static MongoEmbeddedService mongo;
    private static final String databaseName = "db-functional-administration";
    private static final String user = "user-functional-administration";
    private static final String pwd = "user-functional-administration";
    private static final String ADMIN_MANAGEMENT_CONF = "functional-administration-auth-test.conf";
    private static File admin;
    private static AdminManagementConfiguration config;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        new JHades().overlappingJarsReport();

        junitHelper = JunitHelper.getInstance();
        port = junitHelper.findAvailablePort();
        final int adminPort = junitHelper.findAvailablePort();
        SystemPropertyUtil.set("jetty.port", adminPort);

        // Starting the embedded services within temporary dir
        mongo = new MongoEmbeddedService(
            DATABASE_HOST + ":" + port, databaseName, user, pwd, "localreplica");
        mongo.start();

        admin = PropertiesUtils.findFile(ADMIN_MANAGEMENT_CONF);
        config = PropertiesUtils.readYaml(admin, AdminManagementConfiguration.class);
        config.setDbPort(port);     
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        mongo.stop();
        junitHelper.releasePort(port);
    }    
    
    @Test
    public void testApplicationLaunch() throws IOException, VitamException {
        final File newConf = File.createTempFile("test", ADMIN_MANAGEMENT_CONF, admin.getParentFile());
        PropertiesUtils.writeYaml(newConf, config);
        AdminManagementApplication.startApplication(newConf.getAbsolutePath());
        newConf.delete();
        AdminManagementApplication.stop();
    }

}
