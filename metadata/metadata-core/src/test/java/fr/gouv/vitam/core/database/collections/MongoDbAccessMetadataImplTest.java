package fr.gouv.vitam.core.database.collections;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

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
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
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
    
    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();
    private static File elasticsearchHome;

    private final static String CLUSTER_NAME = "vitam-cluster";
    private final static String HOST_NAME = "127.0.0.1";
    private static int TCP_PORT = 9300;
    private static int HTTP_PORT = 9200;
    private static Node node;
    
    private static ElasticsearchAccessMetadata esClient;
    
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
        junitHelper = new JunitHelper();
      //ES
        TCP_PORT = junitHelper.findAvailablePort();
        HTTP_PORT = junitHelper.findAvailablePort();

        elasticsearchHome = tempFolder.newFolder();
        Settings settings = Settings.settingsBuilder()
            .put("http.enabled", true)
            .put("discovery.zen.ping.multicast.enabled", false)
            .put("transport.tcp.port", TCP_PORT)
            .put("http.port", HTTP_PORT)
            .put("path.home", elasticsearchHome.getCanonicalPath())
            .build();

        node = nodeBuilder()
            .settings(settings)
            .client(false)
            .clusterName(CLUSTER_NAME)
            .node();

       node.start();
        
        List<ElasticsearchNode> nodes = new ArrayList<ElasticsearchNode>();
        nodes.add(new ElasticsearchNode(HOST_NAME, TCP_PORT));
       
        esClient = new ElasticsearchAccessMetadata(CLUSTER_NAME, nodes);
        
        //MongoDB
        final MongodStarter starter = MongodStarter.getDefaultInstance();
        port = junitHelper.findAvailablePort();
        mongodExecutable = starter.prepare(new MongodConfigBuilder()
            .version(Version.Main.PRODUCTION)
            .net(new Net(port, Network.localhostIsIPv6()))
            .build());
        mongod = mongodExecutable.start();
        mongoDbAccessFactory = new MongoDbAccessMetadataFactory();
        mongoDbAccess = mongoDbAccessFactory.create(new MetaDataConfiguration(DATABASE_HOST, port, DATABASE_NAME, CLUSTER_NAME, nodes));

        final MongoClientOptions options = MongoDbAccessMetadataImpl.getMongoClientOptions();
        mongoClient = new MongoClient(new ServerAddress(DATABASE_HOST, port), options);

    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        mongoDbAccess.close();
        mongod.stop();
        mongodExecutable.stop();
        junitHelper.releasePort(port);
        
        if (node != null) {
            node.close();
        }

        junitHelper.releasePort(TCP_PORT);
        junitHelper.releasePort(HTTP_PORT);
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
        mongoDbAccess = new MongoDbAccessMetadataImpl(mongoClient, "vitam-test", true, esClient);
        assertEquals(DEFAULT_MONGO, mongoDbAccess.toString());
        assertEquals("Unit", MetadataCollections.C_UNIT.getName());
        assertEquals("ObjectGroup", MetadataCollections.C_OBJECTGROUP.getName());
        assertEquals(0, MongoDbAccessMetadataImpl.getUnitSize());
        assertEquals(0, MongoDbAccessMetadataImpl.getObjectGroupSize());
    }

    @Test
    public void givenMongoDbAccessConstructorWhenCreateWithoutRecreateThenAddNothing() {
        mongoDbAccess = new MongoDbAccessMetadataImpl(mongoClient, "vitam-test", false, esClient);
        assertEquals("", mongoDbAccess.toString());
    }

    @Test
    public void givenMongoDbAccessWhenFlushOnDisKThenDoNothing() {
        mongoDbAccess = new MongoDbAccessMetadataImpl(mongoClient, "vitam-test", false, esClient);
        mongoDbAccess.flushOnDisk();
    }

    @Test
    public void givenMongoDbAccessWhenNoDocumentAndRemoveIndexThenThrowError() {
        mongoDbAccess = new MongoDbAccessMetadataImpl(mongoClient, "vitam-test", false, esClient);
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
