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
package fr.gouv.vitam.metadata.core.database.collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.junit.JunitHelper.ElasticsearchTestConfiguration;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.metadata.api.config.MetaDataConfiguration;
import fr.gouv.vitam.metadata.core.MongoDbAccessMetadataFactory;

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

    private final static String CLUSTER_NAME = "vitam-cluster";
    private final static String HOST_NAME = "127.0.0.1";

    private static ElasticsearchAccessMetadata esClient;

    static MongodExecutable mongodExecutable;
    static MongodProcess mongod;
    static MongoClient mongoClient;
    static JunitHelper junitHelper;
    static final String DATABASE_HOST = "localhost";
    static final String DATABASE_NAME = "vitam-test";
    static final String JETTY_CONFIG = "jetty-config-test.xml";
    static int port;
    static MongoDbAccessMetadataImpl mongoDbAccess;
    static MongoDbAccessMetadataFactory mongoDbAccessFactory;
    private static ElasticsearchTestConfiguration config = null;

    @BeforeClass
    public static void setup() throws IOException, VitamException {
        try {
            config = JunitHelper.startElasticsearchForTest(tempFolder, CLUSTER_NAME);
        } catch (final VitamApplicationServerException e1) {
            assumeTrue(false);
        }
        junitHelper = JunitHelper.getInstance();

        final List<ElasticsearchNode> nodes = new ArrayList<>();
        nodes.add(new ElasticsearchNode(HOST_NAME, config.getTcpPort()));

        esClient = new ElasticsearchAccessMetadata(CLUSTER_NAME, nodes);

        // MongoDB
        final MongodStarter starter = MongodStarter.getDefaultInstance();
        port = junitHelper.findAvailablePort();
        mongodExecutable = starter.prepare(new MongodConfigBuilder()
            .version(Version.Main.PRODUCTION)
            .net(new Net(port, Network.localhostIsIPv6()))
            .build());
        mongod = mongodExecutable.start();
        mongoDbAccessFactory = new MongoDbAccessMetadataFactory();

        final List<MongoDbNode> mongo_nodes = new ArrayList<>();
        mongo_nodes.add(new MongoDbNode(DATABASE_HOST, port));
        mongoDbAccess = MongoDbAccessMetadataFactory
            .create(new MetaDataConfiguration(mongo_nodes, DATABASE_NAME, CLUSTER_NAME, nodes));

        final MongoClientOptions options = MongoDbAccessMetadataImpl.getMongoClientOptions();
        mongoClient = new MongoClient(new ServerAddress(DATABASE_HOST, port), options);

    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        if (config == null) {
            return;
        }
        mongoDbAccess.close();
        mongod.stop();
        mongodExecutable.stop();
        junitHelper.releasePort(port);
        JunitHelper.stopElasticsearchForTest(config);
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
    public void givenUnitWhenGetChildrenUnitIdsFromParent() {
        final Unit unit1 = new Unit(s1);
        final Unit unit2 = new Unit(s2);
        unit1.getChildrenUnitIdsFromParent();
    }
}
