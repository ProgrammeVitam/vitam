package fr.gouv.vitam.functional.administration.rest;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

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
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.server.BasicVitamServer;
import fr.gouv.vitam.common.server.VitamServerFactory;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminFactory;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessReferential;

public class AdminManagementApplicationTest {

    private static final String SHOULD_NOT_RAIZED_AN_EXCEPTION = "Should not raise an exception";
    private static final String ADMIN_MANAGEMENT_CONF = "functional-administration.conf";
    private static final String DATABASE_HOST = "localhost";
    
    static MongodExecutable mongodExecutable;
    static MongodProcess mongod;
    static MongoDbAccessReferential mongoDbAccess;

    
    private static int databasePort;
    private static int serverPort;
    private static int oldPort;
    private static JunitHelper junitHelper;
    private static File functionalAdmin;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // Identify overlapping in particular jsr311

        junitHelper = new JunitHelper();
        databasePort = junitHelper.findAvailablePort();
        
        functionalAdmin = PropertiesUtils.findFile(ADMIN_MANAGEMENT_CONF);
        final AdminManagementConfiguration realfunctionalAdmin = PropertiesUtils.readYaml(functionalAdmin, AdminManagementConfiguration.class);
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
        
        mongoDbAccess = MongoDbAccessAdminFactory.create(new DbConfigurationImpl(DATABASE_HOST, databasePort, "vitam-test"));
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
            ((BasicVitamServer) AdminManagementApplication.startApplication(new String[] {
                functionalAdmin.getAbsolutePath(), "-1"
            })).stop();
        } catch (final IllegalStateException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        } catch (final VitamApplicationServerException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        }
        try {
            ((BasicVitamServer) AdminManagementApplication.startApplication(new String[] {
                functionalAdmin.getAbsolutePath(), "-1xx"
            })).stop();
        } catch (final IllegalStateException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        } catch (final VitamApplicationServerException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        }
        try {
            ((BasicVitamServer) AdminManagementApplication.startApplication(new String[] {
                functionalAdmin.getAbsolutePath(), Integer.toString(serverPort)
            })).stop();
        } catch (final IllegalStateException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        } catch (final VitamApplicationServerException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        }
        try {
            ((BasicVitamServer) AdminManagementApplication.startApplication(new String[0])).stop();
        } catch (final IllegalStateException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        } catch (final VitamApplicationServerException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        }

    }
}
