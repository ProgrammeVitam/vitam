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
package fr.gouv.vitam.core;

import static fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper.id;
import static org.elasticsearch.node.NodeBuilder.nodeBuilder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import fr.gouv.vitam.api.MetaData;
import fr.gouv.vitam.api.config.MetaDataConfiguration;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.database.builder.query.action.Action;
import fr.gouv.vitam.common.database.builder.query.action.SetAction;
import fr.gouv.vitam.common.database.builder.request.multiple.Insert;
import fr.gouv.vitam.common.database.builder.request.multiple.Select;
import fr.gouv.vitam.common.database.builder.request.multiple.Update;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.core.database.collections.DbRequest;
import fr.gouv.vitam.core.database.collections.MongoDbAccessMetadataImpl;


public class MongoDbAccessFactoryTest {

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
    static MongodExecutable mongodExecutable;
    static MongodProcess mongod;
    private static JunitHelper junitHelper;
    private static int port;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        junitHelper = new JunitHelper();

        // ES
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

        nodes = new ArrayList<ElasticsearchNode>();
        nodes.add(new ElasticsearchNode(HOST_NAME, TCP_PORT));

        //MongoDB
        port = junitHelper.findAvailablePort();
        final MongodStarter starter = MongodStarter.getDefaultInstance();
        mongodExecutable = starter.prepare(new MongodConfigBuilder()
            .version(Version.Main.PRODUCTION)
            .net(new Net(port, Network.localhostIsIPv6()))
            .build());
        mongod = mongodExecutable.start();
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        mongod.stop();
        mongodExecutable.stop();
        junitHelper.releasePort(port);


        if (node != null) {
            node.close();
        }

        junitHelper.releasePort(TCP_PORT);
        junitHelper.releasePort(HTTP_PORT);
    }

    @Test
    public void testCreateFn() {
        mongoDbAccess = new MongoDbAccessMetadataFactory()
            .create(new MetaDataConfiguration(DATABASE_HOST, port, "vitam-test", CLUSTER_NAME, nodes, JETTY_CONFIG));
        assertNotNull(mongoDbAccess);
        assertEquals("vitam-test", mongoDbAccess.getMongoDatabase().getName());
        mongoDbAccess.close();
    }
    private JsonNode createInsertRequestWithUUID(GUID uuid) {
        final String MY_INT = "MyInt";
        final String CREATED_DATE = "CreatedDate";
        final String DESCRIPTION = "Description";
        final String TITLE = "Title";
        final String MY_BOOLEAN = "MyBoolean";
        final String MY_FLOAT = "MyFloat";
        final String VALUE_MY_TITLE = "MyTitle";
        final String ARRAY_VAR = "ArrayVar";
        final String ARRAY2_VAR = "Array2Var";
        final String EMPTY_VAR = "EmptyVar";
        // INSERT
        final List<String> list = Arrays.asList("val1", "val2");
        final ObjectNode data = JsonHandler.createObjectNode().put(id(), uuid.toString())
            .put(TITLE, VALUE_MY_TITLE).put(DESCRIPTION, "Ma description est bien détaillée")
            .put(CREATED_DATE, "" + LocalDateUtil.now()).put(MY_INT, 20)
            .put(MY_BOOLEAN, false).putNull(EMPTY_VAR).put(MY_FLOAT, 2.0);
        try {
            data.putArray(ARRAY_VAR).addAll((ArrayNode) JsonHandler.toJsonNode(list));
        } catch (final InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        try {
            data.putArray(ARRAY2_VAR).addAll((ArrayNode) JsonHandler.toJsonNode(list));
        } catch (final InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        final Insert insert = new Insert();
        insert.addData(data);
        // LOGGER.debug("InsertString: " + insert.getFinalInsert().toString());
        return insert.getFinalInsert();
    }

    private JsonNode createUpdateRequest() throws Exception {
        final Update update = new Update();
        Action setTitleAction = new SetAction("Title", "Modified title");
        // Action setDescAction = new SetAction("Description", "Modified description");
        Action setDescAction = new SetAction("MyBoolean", true);
        update.addActions(setTitleAction, setDescAction);
        return update.getFinalUpdate();
    }


    @Test
    public void test() throws Exception {
        MetaData metaDataImpl = MetaDataImpl.newMetadata(new MetaDataConfiguration(DATABASE_HOST, port, "vitam-test",
            CLUSTER_NAME, nodes, JETTY_CONFIG), new MongoDbAccessMetadataFactory(), DbRequest::new);
        assertNotNull(metaDataImpl);

        GUID guid = GUIDFactory.newUnitGUID(0);

        metaDataImpl.insertUnit(createInsertRequestWithUUID(guid));

        JsonNode node = metaDataImpl.selectUnitsById(new Select().getFinalSelect().toString(), guid.getId());
        assertNotNull(node);

        metaDataImpl.updateUnitbyId(createUpdateRequest().toString(), guid.getId());

    }

}
