package fr.gouv.vitam.functional.administration.format.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.functional.administration.common.FileFormat;
import fr.gouv.vitam.functional.administration.common.exception.FileFormatException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;

public class ReferentialFormatFileImplTest {
    String FILE_TO_TEST_KO = "FF-vitam-format-KO.xml";
    String FILE_TO_TEST_OK = "FF-vitam.xml";
    File pronomFile = null;

    static MongodExecutable mongodExecutable;
    static MongodProcess mongod;
    static MongoClient mongoClient;
    static JunitHelper junitHelper;
    static final String DATABASE_HOST = "localhost";
    static final String DATABASE_NAME = "vitam-test";
    static final String COLLECTION_NAME = "FileFormat";
    static int port;
    static ReferentialFormatFileImpl formatFile;
    
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
        formatFile = new ReferentialFormatFileImpl(
            new DbConfigurationImpl(DATABASE_HOST, port, DATABASE_NAME));
        
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        mongod.stop();
        mongodExecutable.stop();
        junitHelper.releasePort(port);
    }

    @Test
    public void testFormatXML() throws FileNotFoundException, ReferentialException {
        formatFile.checkFile(new FileInputStream(PropertiesUtils.findFile(FILE_TO_TEST_OK)));        
    }
    
    @Test(expected = ReferentialException.class)
    public void testFormatXMLKO() throws FileNotFoundException, ReferentialException {
        formatFile.checkFile(new FileInputStream(PropertiesUtils.findFile(FILE_TO_TEST_KO)));
    }
    @Test
    public void testimportAndDeleteFormat() throws Exception {
        formatFile.importFile(new FileInputStream(PropertiesUtils.findFile(FILE_TO_TEST_OK)));
        MongoClient client = new MongoClient(new ServerAddress(DATABASE_HOST, port));
        MongoCollection<Document> collection = client.getDatabase(DATABASE_NAME).getCollection(COLLECTION_NAME);
        assertEquals(1328, collection.count());
        Select select = new Select();
        select.setQuery(eq("PUID", "x-fmt/2"));
        List<FileFormat> fileList = formatFile.findDocuments(select.getFinalSelect());
        String id = fileList.get(0).getString("_id");
        FileFormat file = formatFile.findDocumentById(id);
        assertEquals(file, fileList.get(0));
        formatFile.deleteCollection();
        assertEquals(0, collection.count());
        client.close();
    }
}
