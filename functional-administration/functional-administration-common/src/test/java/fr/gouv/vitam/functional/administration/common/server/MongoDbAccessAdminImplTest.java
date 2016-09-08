package fr.gouv.vitam.functional.administration.common.server;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.functional.administration.common.FileFormat;

public class MongoDbAccessAdminImplTest {

    static MongodExecutable mongodExecutable;
    static MongodProcess mongod;
    static MongoClient mongoClient;
    static JunitHelper junitHelper;
    static final String DATABASE_HOST = "localhost";
    static final String DATABASE_NAME = "vitam-test";
    static final String COLLECTION_NAME = "FileFormat";
    static int port;
    static MongoDbAccessAdminImpl mongoAccess;
    static FileFormat file;

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
        mongoAccess = MongoDbAccessAdminFactory.create(
            new DbConfigurationImpl(DATABASE_HOST, port, DATABASE_NAME));

        List<String> testList = new ArrayList<>();
        testList.add("test1");

        file = new FileFormat()
            .setCreatedDate("now")
            .setExtension(testList)
            .setMimeType(testList)
            .setName("name")
            .setPriorityOverIdList(testList)
            .setPronomVersion("pronom version")
            .setPUID("puid")
            .setVersion("version");

    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        mongod.stop();
        mongodExecutable.stop();
        junitHelper.releasePort(port);
    }

    @Test
    public void testImplementFunction() throws Exception {
        JsonNode jsonNode = JsonHandler.getFromString(file.toJson());
        ArrayNode arrayNode = JsonHandler.createArrayNode();
        arrayNode.add(jsonNode);
        mongoAccess.insertDocuments(arrayNode, FunctionalAdminCollections.FORMATS);
        assertEquals("FileFormat", FunctionalAdminCollections.FORMATS.getName());
        MongoClient client = new MongoClient(new ServerAddress(DATABASE_HOST, port));
        MongoCollection<Document> collection = client.getDatabase(DATABASE_NAME).getCollection(COLLECTION_NAME);
        assertEquals(1, collection.count());
        Select select = new Select();
        select.setQuery(eq("Name", "name"));
        MongoCursor<FileFormat> fileList =
            (MongoCursor<FileFormat>) mongoAccess.select(select.getFinalSelect(), FunctionalAdminCollections.FORMATS);
        FileFormat f1 = fileList.next();
        String id = f1.getString("_id");
        FileFormat f2 = (FileFormat) mongoAccess.getDocumentById(id, FunctionalAdminCollections.FORMATS);
        assertEquals(f2, f1);
        mongoAccess.deleteCollection(FunctionalAdminCollections.FORMATS);
        assertEquals(0, collection.count());
        client.close();
    }
}
