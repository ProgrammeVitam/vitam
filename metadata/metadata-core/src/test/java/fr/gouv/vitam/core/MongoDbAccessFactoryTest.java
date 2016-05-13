package fr.gouv.vitam.core;

import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import fr.gouv.vitam.api.config.MetaDataConfiguration;
import fr.gouv.vitam.core.database.collections.MongoDbAccess;

public class MongoDbAccessFactoryTest {

    private static final String DATABASE_HOST = "localhost";
    private static final int DATABASE_PORT = 12345; 
    static MongoDbAccess mongoDbAccess;
    static MongodExecutable mongodExecutable;
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {

        MongodStarter starter = MongodStarter.getDefaultInstance();
        IMongodConfig mongodConfig = new MongodConfigBuilder()
                .version(Version.Main.PRODUCTION)
                .net(new Net(DATABASE_PORT, Network.localhostIsIPv6()))
                .build();

        mongodExecutable = starter.prepare(mongodConfig);
        mongodExecutable.start();
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        mongoDbAccess.closeFinal();
        mongodExecutable.stop();
    }
    
    @Test
    public void testCreateFn() {
        mongoDbAccess = new MongoDbAccessFactory().create(new MetaDataConfiguration(DATABASE_HOST, DATABASE_PORT, "vitam-test", "Unit"));
        assertNotNull(mongoDbAccess);
        assertEquals("vitam-test", mongoDbAccess.getMongoDatabase().getName());
    }

}
