package fr.gouv.vitam.core.database.collections;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.ServerAddress;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import fr.gouv.vitam.core.database.collections.MongoDbAccess.VitamCollections;

public class MongoDbAccessTest {
    private static final String DATABASE_HOST = "localhost";
    private static final int DATABASE_PORT = 12345;
    private static final String DEFAULT_MONGO =
        "ObjectGroup\n" + "Unit\n" + "Unit Document{{v=1, key=Document{{_id=1}}, name=_id_, ns=vitam-test.Unit}}\n" +
            "Unit Document{{v=1, key=Document{{_id=hashed}}, name=_id_hashed, ns=vitam-test.Unit}}\n" +
            "ObjectGroup Document{{v=1, key=Document{{_id=1}}, name=_id_, ns=vitam-test.ObjectGroup}}\n" +
            "ObjectGroup Document{{v=1, key=Document{{_id=hashed}}, name=_id_hashed, ns=vitam-test.ObjectGroup}}\n";
    static MongoDbAccess mongoDbAccess;
    static MongodExecutable mongodExecutable;
    static MongoClient mongoClient;
    static MongodProcess mongod;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        final MongodStarter starter = MongodStarter.getDefaultInstance();
        mongodExecutable = starter.prepare(new MongodConfigBuilder()
            .version(Version.Main.PRODUCTION)
            .net(new Net(DATABASE_PORT, Network.localhostIsIPv6()))
            .build());
        mongod = mongodExecutable.start();

        final MongoClientOptions options = MongoDbAccess.getMongoClientOptions();

        mongoClient = new MongoClient(new ServerAddress(DATABASE_HOST, DATABASE_PORT), options);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        mongoDbAccess.closeFinal();
        mongod.stop();
        mongodExecutable.stop();
    }

    @After
    public void tearDown() throws Exception {
        MongoDbAccess.reset();
        mongoDbAccess.getMongoDatabase().drop();
    }

    @Test
    public void givenMongoDbAccessConstructorWhenCreateWithRecreateThenAddDefaultCollections() {
        mongoDbAccess = new MongoDbAccess(mongoClient, "vitam-test", true);
        assertEquals(DEFAULT_MONGO, mongoDbAccess.toString());
        assertEquals("Unit", VitamCollections.Cunit.getName());
        assertEquals("ObjectGroup", VitamCollections.Cobjectgroup.getName());
        assertEquals(0, MongoDbAccess.getUnitSize());
        assertEquals(0, MongoDbAccess.getObjectGroupSize());
    }

    @Test
    public void givenMongoDbAccessConstructorWhenCreateWithoutRecreateThenAddNothing() {
        mongoDbAccess = new MongoDbAccess(mongoClient, "vitam-test", false);
        assertEquals("", mongoDbAccess.toString());
    }

    @Test
    public void givenMongoDbAccessWhenFlushOnDisKThenDoNothing() {
        mongoDbAccess = new MongoDbAccess(mongoClient, "vitam-test", false);
        mongoDbAccess.flushOnDisk();
    }

    @Test
    public void givenMongoDbAccessWhenNoDocumentAndRemoveIndexThenThrowError() {
        mongoDbAccess = new MongoDbAccess(mongoClient, "vitam-test", false);
        MongoDbAccess.resetIndexAfterImport();
        MongoDbAccess.removeIndexBeforeImport();
    }

}
