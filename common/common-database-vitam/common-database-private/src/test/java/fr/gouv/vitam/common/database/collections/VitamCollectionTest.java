package fr.gouv.vitam.common.database.collections;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import fr.gouv.vitam.common.database.server.mongodb.CollectionSample;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.junit.JunitHelper;

public class VitamCollectionTest {

    static MongodExecutable mongodExecutable;
    static MongodProcess mongod;
    static MongoClient mongoClient;
    static JunitHelper junitHelper;
    static final String DATABASE_HOST = "localhost";
    static final String DATABASE_NAME = "vitam-test";
    static int port;
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
        
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        mongod.stop();
        mongodExecutable.stop();
        junitHelper.releasePort(port);
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void shouldCreateVitamCollection() {
        List<Class<?>> classList = new ArrayList<>();
        classList.add(CollectionSample.class);
        mongoClient = new MongoClient(new ServerAddress(DATABASE_HOST, port), VitamCollection.getMongoClientOptions(classList));
        VitamCollection vitamCollection = VitamCollectionHelper.getCollection(CollectionSample.class);
        assertEquals(vitamCollection.getClasz(), CollectionSample.class);
        assertEquals(vitamCollection.getName(), "CollectionSample");
        vitamCollection.initialize(mongoClient.getDatabase(DATABASE_NAME), true);
        MongoCollection<CollectionSample> collection = (MongoCollection<CollectionSample>) vitamCollection.getCollection();
        CollectionSample test = new CollectionSample(new Document("_id", GUIDFactory.newGUID().toString()));
        collection.insertOne(test);
        assertEquals(1, collection.count());
    }

}
