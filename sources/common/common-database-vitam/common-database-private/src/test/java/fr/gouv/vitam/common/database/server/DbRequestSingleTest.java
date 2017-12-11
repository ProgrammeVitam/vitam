package fr.gouv.vitam.common.database.server;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.match;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.mongodb.MongoClient;
import fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Delete;
import fr.gouv.vitam.common.database.builder.request.single.Insert;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.builder.request.single.Update;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.database.collections.VitamCollectionHelper;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchAccess;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.database.server.mongodb.CollectionSample;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.junit.JunitHelper.ElasticsearchTestConfiguration;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import org.bson.Document;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class DbRequestSingleTest {

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    static JunitHelper junitHelper;
    static final String DATABASE_HOST = "localhost";
    static final String DATABASE_NAME = "vitam-test";
    static int port;
    static VitamCollection vitamCollection;

    private final static String CLUSTER_NAME = "vitam-cluster";
    private final static String HOST_NAME = "127.0.0.1";

    private static ElasticsearchTestConfiguration config = null;

    private static final Integer TENANT_ID = 0;

    @Rule
    public MongoRule mongoRule =
        new MongoRule(VitamCollection.getMongoClientOptions(Lists.newArrayList(CollectionSample.class)), "vitam-test",
            "Unit", "ObjectGroup");

    @Rule
    public ElasticsearchRule elasticsearchRule = new ElasticsearchRule(tempFolder, "Unit", "ObjectGroup");

    private MongoClient mongoClient = mongoRule.getMongoClient();


    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {


        final List<ElasticsearchNode> nodes = new ArrayList<>();
        nodes.add(new ElasticsearchNode(HOST_NAME, elasticsearchRule.getTcpPort()));


        vitamCollection = VitamCollectionHelper.getCollection(CollectionSample.class, true, false);
        vitamCollection.initialize(new ElasticsearchAccess(CLUSTER_NAME, nodes));
        vitamCollection.initialize(mongoClient.getDatabase(DATABASE_NAME), true);


    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDown() throws Exception {
        if (config == null) {
            return;
        }

        JunitHelper.stopElasticsearchForTest(config);
    }

    @Test
    @RunWithCustomExecutor
    public void testVitamCollectionRequests()
        throws InvalidParseOperationException, BadRequestException, DatabaseException, InvalidCreateOperationException {

        final DbRequestSingle dbRequestSingle = new DbRequestSingle(vitamCollection);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        assertEquals(0, vitamCollection.getCollection().count());

        // init by dbRequest
        final ArrayNode datas = JsonHandler.createArrayNode();
        datas.add(getNewDocument(GUIDFactory.newGUID().toString(), "title one", 1));
        datas.add(getNewDocument(GUIDFactory.newGUID().toString(), "title two", 2));
        final Insert insert = new Insert();
        insert.setData(datas);
        final DbRequestResult insertResult = dbRequestSingle.execute(insert);
        assertEquals(2, insertResult.getCount());
        assertEquals(2, vitamCollection.getCollection().count());
        insertResult.close();

        // find all
        final Select select = new Select();
        final DbRequestResult selectResult = dbRequestSingle.execute(select);
        final List<VitamDocument> selectCursor = selectResult.getDocuments(VitamDocument.class);
        assertEquals(true, !selectCursor.isEmpty());
        assertEquals(2, selectCursor.size());
        selectCursor.clear();
        selectResult.close();

        // find all
        Select select2 = new Select();
        select2.setLimitFilter(11000, 11000);
        try {
            final DbRequestResult selectResult2 = dbRequestSingle.execute(select2);
            final List<VitamDocument> selectCursor2 = selectResult2.getDocuments(VitamDocument.class);
            fail("Should throw an exception, since max_result_window is 10000");
        } catch (BadRequestException e) {
            // do nothing, that means es has thrown an error
        }

        // find with sort in mongo
        final Select sortedSelect = new Select();
        sortedSelect.addOrderByDescFilter("Title.keyword");
        final DbRequestResult sortedSelectResult = dbRequestSingle.execute(sortedSelect);
        final List<VitamDocument> sortedSelectCursor = sortedSelectResult.getDocuments(VitamDocument.class);
        final Document documentSorted1 = sortedSelectCursor.get(0);
        final Document documentSorted2 = sortedSelectCursor.get(1);
        assertEquals("title two", documentSorted1.getString("Title"));
        assertEquals("title one", documentSorted2.getString("Title"));
        sortedSelectCursor.clear();
        sortedSelectResult.close();

        // find with sort in ES
        final Select sortedSelectES = new Select();
        sortedSelectES.setQuery(match("Title", "title"));
        sortedSelectES.addOrderByDescFilter("Title.keyword");
        DbRequestResult sortedSelectESResult = dbRequestSingle.execute(sortedSelectES);
        List<VitamDocument> sortedSelectESCursor = sortedSelectESResult.getDocuments(VitamDocument.class);
        final Document documentSortedES1 = sortedSelectESCursor.get(0);
        final Document documentSortedES2 = sortedSelectESCursor.get(1);
        assertEquals("title two", documentSortedES1.getString("Title"));
        assertEquals("title one", documentSortedES2.getString("Title"));
        sortedSelectESCursor.clear();
        sortedSelectESResult.close();

        // update
        final Update update = new Update();
        update.setQuery(eq("Title.keyword", "title one"));
        update.addActions(UpdateActionHelper.set("Title", "new name"));
        final DbRequestResult updateResult = dbRequestSingle.execute(update);
        assertEquals(1, updateResult.getCount());
        updateResult.close();

        sortedSelectESResult = dbRequestSingle.execute(sortedSelectES);
        sortedSelectESCursor = sortedSelectESResult.getDocuments(VitamDocument.class);
        assertEquals(1, sortedSelectESCursor.size());
        sortedSelectESCursor.clear();
        sortedSelectESResult.close();

        // delete
        final Delete delete = new Delete();
        delete.setQuery(match("Title", "title"));
        final DbRequestResult deleteResult = dbRequestSingle.execute(delete);
        assertEquals(1, deleteResult.getCount());
        assertEquals(1, vitamCollection.getCollection().count());
        deleteResult.close();
    }

    private ObjectNode getNewDocument(String id, String title, Integer num) {
        final ObjectNode node = JsonHandler.createObjectNode();
        node.put("_id", id);
        node.put("Title", title);
        node.put("Numero", num);
        node.put("_tenant", TENANT_ID);
        return node;
    }
}
