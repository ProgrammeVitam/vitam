/*
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
 */

package fr.gouv.vitam.metadata.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.Filters;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.metadata.api.config.MetaDataConfiguration;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.database.collections.MongoDbAccessMetadataImpl;
import org.bson.Document;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ru.yandex.qatools.embed.service.MongoEmbeddedService;

public class MongoDbAccessMetadataFactoryTest {

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    private final static String CLUSTER_NAME = "vitam-cluster";
    private final static String HOST_NAME = "127.0.0.1";

    private static List<ElasticsearchNode> nodes;
    private static List<MongoDbNode> mongoDbNodes;

    private static final String DATABASE_HOST = "localhost";
    static MongoDbAccessMetadataImpl mongoDbAccess;
    private static JunitHelper junitHelper;
    private static int port;
    private static MongoEmbeddedService mongo;
    private static MongoEmbeddedService mongo_bis;
    private static final String databaseName = "db-metadata";
    private static final String user = "user-metadata";
    private static final String pwd = "user-metadata";

    static final int tenantId = 0;
    static final List tenantList = new ArrayList() {
        /**
         *
         */
        private static final long serialVersionUID = -315017116521336143L;

        {
            add(tenantId);
        }
    };

    @BeforeClass
    public static void setup() throws IOException {
        junitHelper = JunitHelper.getInstance();

        nodes = new ArrayList<>();
        nodes.add(new ElasticsearchNode(HOST_NAME, ElasticsearchRule.TCP_PORT));

        // MongoDB Node1
        mongoDbNodes = new ArrayList<>();
        port = junitHelper.findAvailablePort();
        mongoDbNodes.add(new MongoDbNode(DATABASE_HOST, port));

        // Starting the embedded services within temporary dir
        mongo = new MongoEmbeddedService(
            DATABASE_HOST + ":" + port, databaseName, user, pwd, "localreplica");
        mongo.start();

        // MongoDB Node2
        mongoDbNodes = new ArrayList<>();
        port = junitHelper.findAvailablePort();
        mongoDbNodes.add(new MongoDbNode(DATABASE_HOST, port));

        // Starting the embedded services within temporary dir
        mongo_bis = new MongoEmbeddedService(
            DATABASE_HOST + ":" + port, databaseName, user, pwd, "localreplica");
        mongo_bis.start();
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        try {
            mongo.stop();
        } finally {
            junitHelper.releasePort(port);
            VitamClientFactory.resetConnections();
        }

    }

    @Test
    // FIXME : MongoEmbeddedService lib not compatible with readConcern MAJORITY mongo configuration
    @Ignore
    public void testCreateMetadataMongoAccessWithAuthentication() {
        final MetaDataConfiguration config =
            new MetaDataConfiguration(mongoDbNodes, databaseName, CLUSTER_NAME, nodes, true, user, pwd);
        VitamConfiguration.setTenants(tenantList);
        new MongoDbAccessMetadataFactory();
        mongoDbAccess = MongoDbAccessMetadataFactory.create(config);
        assertNotNull(mongoDbAccess);
        assertEquals("db-metadata", mongoDbAccess.getMongoDatabase().getName());
        mongoDbAccess.close();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateFnWithError() throws Exception {
        final List<ElasticsearchNode> nodesEmpty = new ArrayList<>();
        new MongoDbAccessMetadataFactory();
        final MongoDbAccessMetadataImpl mongoDbAccessError = MongoDbAccessMetadataFactory
            .create(
                new MetaDataConfiguration(mongoDbNodes, "vitam-test", CLUSTER_NAME, nodesEmpty));
        mongoDbAccessError.close();
    }

    @Test(expected = com.mongodb.MongoCommandException.class)
    public void shouldThrowExceptionWhenGetDatabaseNames() {
        final MetaDataConfiguration config =
            new MetaDataConfiguration(mongoDbNodes, databaseName, CLUSTER_NAME, nodes);
        config.setDbUserName(user);
        config.setDbPassword(pwd);
        config.setDbAuthentication(true);

        final List<Class<?>> classList = new ArrayList<>();
        for (final MetadataCollections e : MetadataCollections.class.getEnumConstants()) {
            classList.add(e.getClasz());
        }
        MetadataCollections.class.getEnumConstants();

        final MongoCredential credential = MongoCredential.createCredential(
            config.getDbUserName(), config.getDbName(), config.getDbPassword().toCharArray());

        final List<ServerAddress> serverAddress = new ArrayList<>();
        for (final MongoDbNode node : mongoDbNodes) {
            serverAddress.add(new ServerAddress(node.getDbHost(), node.getDbPort()));
        }

        final MongoClient mongoClient = new MongoClient(serverAddress,
            Arrays.asList(credential),
            VitamCollection.getMongoClientOptions(classList));

        final MongoDatabase metadatabase = mongoClient.getDatabase(databaseName);

        // access only to metadata base, so DatabaseNames() should raise an exception
        final MongoIterable<String> iterable = mongoClient.listDatabaseNames();
        Iterator<String> iterator = iterable.iterator();
        while (iterator.hasNext()) {
            iterator.next();
        }
    }


    @Test
    public void shouldHavePermissions() {
        final MetaDataConfiguration config =
            new MetaDataConfiguration(mongoDbNodes, databaseName, CLUSTER_NAME, nodes);
        config.setDbUserName(user);
        config.setDbPassword(pwd);
        config.setDbAuthentication(true);

        final List<Class<?>> classList = new ArrayList<>();
        for (final MetadataCollections e : MetadataCollections.class.getEnumConstants()) {
            classList.add(e.getClasz());
        }
        MetadataCollections.class.getEnumConstants();

        final MongoCredential credential = MongoCredential.createCredential(
            config.getDbUserName(), config.getDbName(), config.getDbPassword().toCharArray());

        final List<ServerAddress> serverAddress = new ArrayList<>();
        for (final MongoDbNode node : mongoDbNodes) {
            serverAddress.add(new ServerAddress(node.getDbHost(), node.getDbPort()));
        }

        final MongoClient mongoClient = new MongoClient(serverAddress,
            Arrays.asList(credential),
            VitamCollection.getMongoClientOptions(classList));

        final MongoDatabase metadatabase = mongoClient.getDatabase(databaseName);

        // User "integration" have authentication in Database "Metadata"
        metadatabase.createCollection("Unit2");
        final MongoCollection<Document> col = metadatabase.getCollection("Unit2");
        col.insertOne(new Document("_id", "1234"));
        col.find(Filters.eq("_id", "1234"));
        col.deleteOne(Filters.eq("_id", "1234"));
    }
}
