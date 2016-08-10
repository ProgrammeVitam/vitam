package fr.gouv.vitam.core.database.collections;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

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
import fr.gouv.vitam.api.config.MetaDataConfiguration;
import fr.gouv.vitam.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.core.MongoDbAccessMetadataFactory;

public class MongoDbAccessMetadataImplTest {
   
    private static final String DEFAULT_MONGO =
        "ObjectGroup\n" + "Unit\n" + "Unit Document{{v=1, key=Document{{_id=1}}, name=_id_, ns=vitam-test.Unit}}\n" +
            "Unit Document{{v=1, key=Document{{_id=hashed}}, name=_id_hashed, ns=vitam-test.Unit}}\n" +
            "ObjectGroup Document{{v=1, key=Document{{_id=1}}, name=_id_, ns=vitam-test.ObjectGroup}}\n" +
            "ObjectGroup Document{{v=1, key=Document{{_id=hashed}}, name=_id_hashed, ns=vitam-test.ObjectGroup}}\n";

    private static final String s1 = "{\"_id\":\"id1\", \"title\":\"title1\", \"_max\": \"5\", \"_min\": \"2\"}";
    private static final String s2 = "{\"_id\":\"id2\", \"title\":\"title2\", \"_up\":\"id1\"}";
    private static final String s3 = "{\"_id\":\"id3\", \"title\":\"title3\", \"_up\":\"id1\"}";
    private static final String sub1 = "{\"_id\":\"id2\",\"description\":\"description1\"}";
    private static final String sub2 = "{\"_id\":\"id3\",\"champ\":\"champ1\"}";

    
    static MongodExecutable mongodExecutable;
    static MongodProcess mongod;
    static MongoClient mongoClient;
    static JunitHelper junitHelper;
    static final String DATABASE_HOST = "localhost";
    static final String DATABASE_NAME = "vitam-test";
    static int port;
    static MongoDbAccessMetadataImpl mongoDbAccess;
    static MongoDbAccessMetadataFactory mongoDbAccessFactory;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        final MongodStarter starter = MongodStarter.getDefaultInstance();
        junitHelper = new JunitHelper();
        port = junitHelper.findAvailablePort();
        mongodExecutable = starter.prepare(new MongodConfigBuilder()
            .version(Version.Main.PRODUCTION)
            .net(new Net(port, Network.localhostIsIPv6()))
            .build());
        mongod = mongodExecutable.start();
        mongoDbAccessFactory = new MongoDbAccessMetadataFactory();
        mongoDbAccess = mongoDbAccessFactory.create(new MetaDataConfiguration(DATABASE_HOST, port, DATABASE_NAME));

        final MongoClientOptions options = MongoDbAccessMetadataImpl.getMongoClientOptions();
        mongoClient = new MongoClient(new ServerAddress(DATABASE_HOST, port), options);

    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        mongoDbAccess.close();
        mongod.stop();
        mongodExecutable.stop();
        junitHelper.releasePort(port);
    }

    @After
    public void tearDown() throws Exception {
        for (final MetadataCollections col : MetadataCollections.values()) {
            if (col.getCollection() != null) {
                col.getCollection().drop();
            }
        }
        mongoDbAccess.getMongoDatabase().drop();
    }

    @Test
    public void givenMongoDbAccessConstructorWhenCreateWithRecreateThenAddDefaultCollections() {
        mongoDbAccess = new MongoDbAccessMetadataImpl(mongoClient, "vitam-test", true);
        assertEquals(DEFAULT_MONGO, mongoDbAccess.toString());
        assertEquals("Unit", MetadataCollections.C_UNIT.getName());
        assertEquals("ObjectGroup", MetadataCollections.C_OBJECTGROUP.getName());
        assertEquals(0, MongoDbAccessMetadataImpl.getUnitSize());
        assertEquals(0, MongoDbAccessMetadataImpl.getObjectGroupSize());
    }

    @Test
    public void givenMongoDbAccessConstructorWhenCreateWithoutRecreateThenAddNothing() {
        mongoDbAccess = new MongoDbAccessMetadataImpl(mongoClient, "vitam-test", false);
        assertEquals("", mongoDbAccess.toString());
    }

    @Test
    public void givenMongoDbAccessWhenFlushOnDisKThenDoNothing() {
        mongoDbAccess = new MongoDbAccessMetadataImpl(mongoClient, "vitam-test", false);
        mongoDbAccess.flushOnDisk();
    }

    @Test
    public void givenMongoDbAccessWhenNoDocumentAndRemoveIndexThenThrowError() {
        mongoDbAccess = new MongoDbAccessMetadataImpl(mongoClient, "vitam-test", false);
        MongoDbAccessMetadataImpl.resetIndexAfterImport();
        MongoDbAccessMetadataImpl.removeIndexBeforeImport();
    }

  @Test
  public void givenUnitWhenGetChildrenUnitIdsFromParent(){
      final Unit unit1 = new Unit(s1);
      final Unit unit2 = new Unit(s2);
      unit1.getChildrenUnitIdsFromParent();
  }    
}
