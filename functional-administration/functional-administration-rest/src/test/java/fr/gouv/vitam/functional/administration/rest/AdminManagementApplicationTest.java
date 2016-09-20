package fr.gouv.vitam.functional.administration.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.server.VitamServerFactory;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminFactory;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessReferential;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;

import static org.junit.Assert.fail;

public class AdminManagementApplicationTest {

    private static final String SHOULD_NOT_RAIZED_AN_EXCEPTION = "Should not raise an exception";
    private static final String ADMIN_MANAGEMENT_CONF = "functional-administration-test.conf";
    private static final String DATABASE_HOST = "localhost";

    static MongodExecutable mongodExecutable;
    static MongodProcess mongod;
    static MongoDbAccessReferential mongoDbAccess;


    private static int databasePort;
    private static int serverPort;
    private static int oldPort;
    private static JunitHelper junitHelper;
    private static File functionalAdmin;
    private final AdminManagementApplication application = new AdminManagementApplication();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // Identify overlapping in particular jsr311

        junitHelper = new JunitHelper();
        databasePort = junitHelper.findAvailablePort();

        functionalAdmin = PropertiesUtils.findFile(ADMIN_MANAGEMENT_CONF);
        final AdminManagementConfiguration realfunctionalAdmin =
            PropertiesUtils.readYaml(functionalAdmin, AdminManagementConfiguration.class);
        realfunctionalAdmin.setDbPort(databasePort);
        try (FileOutputStream outputStream = new FileOutputStream(functionalAdmin)) {
            final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            mapper.writeValue(outputStream, realfunctionalAdmin);
        }

        final MongodStarter starter = MongodStarter.getDefaultInstance();
        mongodExecutable = starter.prepare(new MongodConfigBuilder()
            .version(Version.Main.PRODUCTION)
            .net(new Net(databasePort, Network.localhostIsIPv6()))
            .build());
        mongod = mongodExecutable.start();

        mongoDbAccess =
            MongoDbAccessAdminFactory.create(new DbConfigurationImpl(DATABASE_HOST, databasePort, "vitam-test"));
        serverPort = junitHelper.findAvailablePort();
        oldPort = VitamServerFactory.getDefaultPort();
        VitamServerFactory.setDefaultPort(serverPort);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        mongod.stop();
        mongodExecutable.stop();
        junitHelper.releasePort(serverPort);
        junitHelper.releasePort(databasePort);
        VitamServerFactory.setDefaultPort(oldPort);
    }

    @Test
    public final void testFictiveLaunch() {
        try {
            AdminManagementApplication.startApplication(new String[] {functionalAdmin.getAbsolutePath()});
            AdminManagementApplication.stop();
        } catch (final IllegalStateException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        } catch (final VitamApplicationServerException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        } catch (VitamException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        }
    }

    @Test(expected = VitamException.class)
    public void shouldRaiseAnExceptionWhenConfigureApplicationWithEmptyArgs() throws Exception {
        application.startApplication(new String[] {""});
    }

    @Test(expected = VitamException.class)
    public void shouldRaiseAnExceptionWhenConfigureApplicationWithNullArgs() throws Exception {
        application.startApplication(null);
    }


    @Test
    public void shouldGetConfigFileName() {
        Assert.assertEquals(AdminManagementApplication.CONF_FILE_NAME, application.getConfigFilename());
    }
}
