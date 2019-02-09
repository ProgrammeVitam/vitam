package fr.gouv.vitam.common.database.server;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import fr.gouv.vitam.common.VitamConfiguration;
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
import fr.gouv.vitam.common.exception.SchemaValidationException;
import fr.gouv.vitam.common.exception.VitamDBException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadFactory;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import org.assertj.core.api.Assertions;
import org.bson.Document;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.match;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class DbRequestSingleTest {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DbRequestSingle.class);

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());


    static VitamCollection vitamCollection;

    private final static String HOST_NAME = "127.0.0.1";

    private static final Integer TENANT_ID = 0;


    public static final String PREFIX = GUIDFactory.newGUID().getId();
    @ClassRule
    public static MongoRule mongoRule =
        new MongoRule(VitamCollection.getMongoClientOptions(Lists.newArrayList(CollectionSample.class)),
            PREFIX + CollectionSample.class.getSimpleName());

    @ClassRule
    public static ElasticsearchRule elasticsearchRule =
        new ElasticsearchRule(PREFIX + CollectionSample.class.getSimpleName());

    private static ElasticsearchAccess esClient;


    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUp() throws Exception {


        final List<ElasticsearchNode> nodes = new ArrayList<>();
        nodes.add(new ElasticsearchNode(HOST_NAME, elasticsearchRule.getTcpPort()));

        vitamCollection = VitamCollectionHelper.getCollection(CollectionSample.class, true, false, PREFIX);
        esClient = new ElasticsearchAccess(ElasticsearchRule.VITAM_CLUSTER, nodes);
        vitamCollection.initialize(esClient);
        vitamCollection.initialize(mongoRule.getMongoDatabase(), true);

    }

    @AfterClass
    public static void afterClass() {
        mongoRule.handleAfterClass();
        elasticsearchRule.deleteIndexes();
        esClient.close();
    }

    @After
    public void after() {
        mongoRule.handleAfter();
        elasticsearchRule.handleAfter();
    }

    @Test
    @RunWithCustomExecutor
    public void testVitamCollectionRequests()
        throws InvalidParseOperationException, BadRequestException, DatabaseException, InvalidCreateOperationException,
        VitamDBException, SchemaValidationException {

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        DbRequestSingle dbRequestSingle = new DbRequestSingle(vitamCollection);
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


    @Test
    @RunWithCustomExecutor
    public void testOptimisticLockOK()
        throws InvalidParseOperationException, BadRequestException, DatabaseException, InvalidCreateOperationException,
        VitamDBException, SchemaValidationException {

        VitamConfiguration.setOptimisticLockSleepTime(10);
        VitamConfiguration.setOptimisticLockRetryNumber(5);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        assertEquals(0, vitamCollection.getCollection().count());

        // init by dbRequest
        final ArrayNode datas = JsonHandler.createArrayNode();
        datas.add(getNewDocument(GUIDFactory.newGUID().toString(), "Optimistic lock test", 3));
        final Insert insert = new Insert();
        insert.setData(datas);
        final DbRequestResult insertResult = new DbRequestSingle(vitamCollection).execute(insert);
        assertEquals(1, insertResult.getCount());
        assertEquals(1, vitamCollection.getCollection().count());
        insertResult.close();

        CountDownLatch countDownLatch = new CountDownLatch(5);
        // update
        VitamThreadFactory.getInstance().newThread(() -> update(countDownLatch, 1)).start();
        VitamThreadFactory.getInstance().newThread(() -> update(countDownLatch, 2)).start();
        VitamThreadFactory.getInstance().newThread(() -> update(countDownLatch, 3)).start();
        VitamThreadFactory.getInstance().newThread(() -> update(countDownLatch, 4)).start();
        VitamThreadFactory.getInstance().newThread(() -> update(countDownLatch, 5)).start();

        try {
            countDownLatch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("OptimisticLock KO : " + e.getMessage());
        }

        Document document = (Document) vitamCollection.getCollection().find(new Document("Numero", 3)).first();
        Assertions.assertThat(document.getString("Title")).contains("thread_");
        Assertions.assertThat(document.getInteger("_v")).isEqualTo(5);
    }


    private void update(CountDownLatch countDownLatch, int nbr) {
        try {
            VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
            final Update update = new Update();
            update.setQuery(eq("Numero", 3));
            update.addActions(UpdateActionHelper.set("Title", "thread_" + nbr));

            final DbRequestResult updateResult = new DbRequestSingle(vitamCollection).execute(update);
            System.err.println("Thread_" + nbr + " >> " + updateResult.getDiffs());
            assertEquals(1, updateResult.getCount());
            updateResult.close();
        } catch (Exception e) {
            LOGGER.error("should not throw exception : ", e);
            fail("should not throw exception : " + e.getMessage());
        } finally {
            countDownLatch.countDown();
        }
    }

    private ObjectNode getNewDocument(String id, String title, Integer num) {
        final ObjectNode node = JsonHandler.createObjectNode();
        node.put("_id", id);
        node.put("Title", title);
        node.put("Numero", num);
        node.put("_tenant", TENANT_ID);
        node.put("_v", 0);
        return node;
    }
}
