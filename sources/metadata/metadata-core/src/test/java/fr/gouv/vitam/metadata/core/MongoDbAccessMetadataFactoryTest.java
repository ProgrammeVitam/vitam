/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 *******************************************************************************/

package fr.gouv.vitam.metadata.core;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bson.Document;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.metadata.api.config.MetaDataConfiguration;
import fr.gouv.vitam.metadata.api.exception.MetaDataException;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.database.collections.MongoDbAccessMetadataImpl;
import ru.yandex.qatools.embed.service.MongoEmbeddedService;

public class MongoDbAccessMetadataFactoryTest {

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();
    private static File elasticsearchHome;

    private final static String CLUSTER_NAME = "vitam-cluster";
    private final static String HOST_NAME = "127.0.0.1";
    private static int TCP_PORT = 9300;
    private static int HTTP_PORT = 9200;
    private static Node node;

    private static List<ElasticsearchNode> nodes;

    private static final String DATABASE_HOST = "localhost";
    private static final String JETTY_CONFIG = "jetty-config-test.xml";
    static MongoDbAccessMetadataImpl mongoDbAccess;
    private static JunitHelper junitHelper;
    private static int port;
    private static MongoEmbeddedService mongo;
    private static final String databaseName = "db-metadata";
    private static final String user = "user-metadata";
    private static final String pwd = "user-metadata";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        junitHelper = JunitHelper.getInstance();
        // ES
        TCP_PORT = junitHelper.findAvailablePort();
        HTTP_PORT = junitHelper.findAvailablePort();

        elasticsearchHome = tempFolder.newFolder();
        final Settings settings = Settings.settingsBuilder()
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

        nodes = new ArrayList<ElasticsearchNode>();
        nodes.add(new ElasticsearchNode(HOST_NAME, TCP_PORT));

        // MongoDB
        port = junitHelper.findAvailablePort();

        // Starting the embedded services within temporary dir
        mongo = new MongoEmbeddedService(
            DATABASE_HOST + ":" + port, databaseName, user, pwd, "localreplica");
        mongo.start();
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        mongo.stop();
        junitHelper.releasePort(port);


        if (node != null) {
            node.close();
        }

        junitHelper.releasePort(TCP_PORT);
        junitHelper.releasePort(HTTP_PORT);
    }

    @Test
    public void testCreateMetadataMongoAccessWithAuthentication() {
        MetaDataConfiguration config =
            new MetaDataConfiguration(DATABASE_HOST, port, databaseName, CLUSTER_NAME, nodes, JETTY_CONFIG, true, user, pwd);
        mongoDbAccess = new MongoDbAccessMetadataFactory()
            .create(config);
        assertNotNull(mongoDbAccess);
        assertEquals("db-metadata", mongoDbAccess.getMongoDatabase().getName());
        mongoDbAccess.close();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateFnWithError() throws Exception {
        final List<ElasticsearchNode> nodesEmpty = new ArrayList<ElasticsearchNode>();
        final MongoDbAccessMetadataImpl mongoDbAccessError = new MongoDbAccessMetadataFactory()
            .create(
                new MetaDataConfiguration(DATABASE_HOST, port, "vitam-test", CLUSTER_NAME, nodesEmpty, JETTY_CONFIG));
        mongoDbAccessError.close();
    }

    @Test(expected = com.mongodb.MongoCommandException.class)
    public void shouldThrowExceptionWhenGetDatabaseNames() {
        MetaDataConfiguration config =
            new MetaDataConfiguration(DATABASE_HOST, port, databaseName, CLUSTER_NAME, nodes, JETTY_CONFIG);
        config.setDbUserName(user);
        config.setDbPassword(pwd);
        config.setDbAuthentication(true);

        final List<Class<?>> classList = new ArrayList<>();
        for (final MetadataCollections e : MetadataCollections.class.getEnumConstants()) {
            classList.add(e.getClasz());
        }
        MetadataCollections.class.getEnumConstants();

        MongoCredential credential = MongoCredential.createCredential(
            config.getDbUserName(), config.getDbName(), config.getDbPassword().toCharArray());

        MongoClient mongoClient = new MongoClient(new ServerAddress(
            config.getDbHost(),
            config.getDbPort()),
            Arrays.asList(credential),
            VitamCollection.getMongoClientOptions(classList));

        MongoDatabase metadatabase = mongoClient.getDatabase(databaseName);

        // access only to metadata base, so DatabaseNames() should raise an exception
        List<String> dbs = mongoClient.getDatabaseNames();
    }


    @Test
    public void shouldHavePermissions() throws MetaDataException{        
        MetaDataConfiguration config =
            new MetaDataConfiguration(DATABASE_HOST, port, databaseName, CLUSTER_NAME, nodes, JETTY_CONFIG);
        config.setDbUserName(user);
        config.setDbPassword(pwd);
        config.setDbAuthentication(true);

        final List<Class<?>> classList = new ArrayList<>();
        for (final MetadataCollections e : MetadataCollections.class.getEnumConstants()) {
            classList.add(e.getClasz());
        }
        MetadataCollections.class.getEnumConstants();

        MongoCredential credential = MongoCredential.createCredential(
            config.getDbUserName(), config.getDbName(), config.getDbPassword().toCharArray());

        MongoClient mongoClient = new MongoClient(new ServerAddress(
            config.getDbHost(),
            config.getDbPort()),
            Arrays.asList(credential),
            VitamCollection.getMongoClientOptions(classList));

        MongoDatabase metadatabase = mongoClient.getDatabase(databaseName);
        
        // User "integration" have authentication in Database "Metadata"
        metadatabase.createCollection("Unit2");
        MongoCollection<Document> col = metadatabase.getCollection("Unit2");
        col.insertOne(new Document("_id", "1234"));
        col.find(Filters.eq("_id", "1234"));
        col.deleteOne(Filters.eq("_id", "1234"));
    }
}
