package fr.gouv.vitam.logbook.rest;

import static org.junit.Assert.fail;

import java.io.FileNotFoundException;

import org.jhades.JHades;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.server.BasicVitamServer;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.logbook.common.server.MongoDbAccess;
import fr.gouv.vitam.logbook.common.server.database.collections.MongoDbAccessFactory;


public class LogbookApplicationTest {
    private static final String SHOULD_NOT_RAIZED_AN_EXCEPTION = "Should not raized an exception";
    private static final String SHOULD_RAIZED_AN_EXCEPTION = "Should raized an exception";

    private static final String LOGBOOK_CONF = "logbook.conf";
    private static final String DATABASE_HOST = "localhost";
    private static final int DATABASE_PORT = 12346;
    private static MongoDbAccess mongoDbAccess;
    private static MongodExecutable mongodExecutable;
    private static MongodProcess mongod;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // Identify overlapping in particular jsr311
        new JHades().overlappingJarsReport();

        final MongodStarter starter = MongodStarter.getDefaultInstance();
        mongodExecutable = starter.prepare(new MongodConfigBuilder()
            .version(Version.Main.PRODUCTION)
            .net(new Net(DATABASE_PORT, Network.localhostIsIPv6()))
            .build());
        mongod = mongodExecutable.start();
        mongoDbAccess =
            MongoDbAccessFactory.create(
                new DbConfigurationImpl(DATABASE_HOST, DATABASE_PORT,
                    "vitam-test"));
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        mongoDbAccess.close();
        mongod.stop();
        mongodExecutable.stop();
    }

    @Test
    public final void testFictiveLaunch() {
        try {
            ((BasicVitamServer) LogbookApplication.startApplication(new String[] {
                PropertiesUtils.getResourcesFile(LOGBOOK_CONF).getAbsolutePath(), "-1"
            })).stop();
        } catch (final IllegalStateException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        } catch (final FileNotFoundException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        } catch (VitamApplicationServerException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        }
        try {
            ((BasicVitamServer) LogbookApplication.startApplication(new String[] {
                PropertiesUtils.getResourcesFile(LOGBOOK_CONF).getAbsolutePath(), "-1xx"
            })).stop();
        } catch (final IllegalStateException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        } catch (final FileNotFoundException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        } catch (VitamApplicationServerException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        }
        try {
            ((BasicVitamServer) LogbookApplication.startApplication(new String[0])).stop();
        } catch (final IllegalStateException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        } catch (VitamApplicationServerException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        }

    }

}
