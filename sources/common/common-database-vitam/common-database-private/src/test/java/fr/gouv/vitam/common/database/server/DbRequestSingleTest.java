/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
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
package fr.gouv.vitam.common.database.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Delete;
import fr.gouv.vitam.common.database.builder.request.single.Insert;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.builder.request.single.Update;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.database.collections.VitamCollectionHelper;
import fr.gouv.vitam.common.database.collections.VitamDescriptionResolver;
import fr.gouv.vitam.common.database.collections.VitamDescriptionType;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchAccess;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchIndexAlias;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.database.server.mongodb.BsonHelper;
import fr.gouv.vitam.common.database.server.mongodb.CollectionSample;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
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
import net.javacrumbs.jsonunit.JsonAssert;
import org.bson.Document;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.match;
import static fr.gouv.vitam.common.database.collections.VitamDescriptionType.VitamCardinality.one;
import static fr.gouv.vitam.common.database.collections.VitamDescriptionType.VitamType.text;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

public class DbRequestSingleTest {

    public static final String PREFIX = GUIDFactory.newGUID().getId();
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DbRequestSingle.class);
    private static final Integer TENANT_ID = 0;
    private static final Integer ADMIN_TENANT_ID = 1;

    @ClassRule
    public static MongoRule mongoRule = new MongoRule(
        MongoDbAccess.getMongoClientSettingsBuilder(CollectionSample.class),
        PREFIX + CollectionSample.class.getSimpleName()
    );

    @ClassRule
    public static ElasticsearchRule elasticsearchRule = new ElasticsearchRule(
        ElasticsearchIndexAlias.ofCrossTenantCollection(PREFIX + CollectionSample.class.getSimpleName()).getName()
    );

    private static VitamCollection vitamCollection;
    private static VitamCollection vitamCollectionCrossTenant;
    private static ElasticsearchAccess esClient;

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @BeforeClass
    public static void setUp() throws Exception {
        final List<ElasticsearchNode> nodes = new ArrayList<>();
        nodes.add(new ElasticsearchNode(ElasticsearchRule.getHost(), ElasticsearchRule.getPort()));
        List<VitamDescriptionType> descriptions = Collections.singletonList(
            new VitamDescriptionType("Title", null, text, one, true)
        );
        VitamDescriptionResolver vitamDescriptionResolver = new VitamDescriptionResolver(descriptions);
        vitamCollection = VitamCollectionHelper.getCollection(
            CollectionSample.class,
            true,
            false,
            PREFIX,
            vitamDescriptionResolver
        );
        esClient = new ElasticsearchAccess(ElasticsearchRule.VITAM_CLUSTER, nodes);
        vitamCollection.initialize(esClient);
        vitamCollection.initialize(mongoRule.getMongoDatabase(), true);

        vitamCollectionCrossTenant = VitamCollectionHelper.getCollection(
            CollectionSample.class,
            false,
            false,
            PREFIX,
            vitamDescriptionResolver
        );
        esClient = new ElasticsearchAccess(ElasticsearchRule.VITAM_CLUSTER, nodes);
        vitamCollectionCrossTenant.initialize(esClient);
        vitamCollectionCrossTenant.initialize(mongoRule.getMongoDatabase(), true);
    }

    @AfterClass
    public static void afterClass() {
        mongoRule.handleAfterClass();
        elasticsearchRule.purgeIndices();
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
        throws InvalidParseOperationException, BadRequestException, DatabaseException, InvalidCreateOperationException, VitamDBException, SchemaValidationException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        DbRequestSingle dbRequestSingle = new DbRequestSingle(
            vitamCollection,
            Collections::emptyList,
            ElasticsearchIndexAlias.ofCrossTenantCollection(vitamCollection.getName())
        );
        assertEquals(0, vitamCollection.getCollection().countDocuments());

        // init by dbRequest
        final ArrayNode data = JsonHandler.createArrayNode();
        data.add(getNewDocument(GUIDFactory.newGUID().toString(), "title one", 1));
        data.add(getNewDocument(GUIDFactory.newGUID().toString(), "title two", 2));
        final Insert insert = new Insert();
        insert.setData(data);
        final DbRequestResult insertResult = dbRequestSingle.execute(insert, 0, mock(DocumentValidator.class));
        assertEquals(2, insertResult.getCount());
        assertEquals(2, vitamCollection.getCollection().countDocuments());
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
        final DbRequestResult updateResult = dbRequestSingle.execute(update, mock(DocumentValidator.class));
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
        assertEquals(1, vitamCollection.getCollection().countDocuments());
        deleteResult.close();
    }

    @Test
    @RunWithCustomExecutor
    public void testReplaceMultiTenantCollectionDocuments() throws Exception {
        // Given: (multi-tenant data set)
        DbRequestSingle dbRequestSingle = new DbRequestSingle(
            vitamCollection,
            Collections::emptyList,
            ElasticsearchIndexAlias.ofCrossTenantCollection(vitamCollection.getName())
        );
        assertEquals(0, vitamCollection.getCollection().countDocuments());
        DocumentValidator documentValidator = mock(DocumentValidator.class);

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        dbRequestSingle.execute(
            new Insert()
                .setData(
                    JsonHandler.getFromInputStream(
                        PropertiesUtils.getConfigAsStream(
                            "replaceDocumentMultiTenantCollection/inputDataSetTenant0.json"
                        )
                    )
                ),
            0,
            documentValidator
        );

        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT_ID);
        dbRequestSingle.execute(
            new Insert()
                .setData(
                    JsonHandler.getFromInputStream(
                        PropertiesUtils.getConfigAsStream(
                            "replaceDocumentMultiTenantCollection/inputDataSetTenant1.json"
                        )
                    )
                ),
            0,
            documentValidator
        );

        // When: Replace docs from tenant 0
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        dbRequestSingle.replaceDocuments(
            Map.of(
                "Identifier2",
                JsonHandler.createObjectNode()
                    .put("_tenant", 0)
                    .put("Identifier", "Identifier2")
                    .put("Description", "NewDescription2"),
                "Identifier4",
                JsonHandler.createObjectNode()
                    .put("_tenant", 0)
                    .put("Identifier", "Identifier4")
                    .put("NewField", "NewFieldValue")
            ),
            "Identifier",
            vitamCollection
        );

        // Then
        // Check tenant 0 updates
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        List<JsonNode> tenant0Documents = getAllDocuments(dbRequestSingle);

        final JsonNode expectedTenant0Results = JsonHandler.getFromInputStream(
            PropertiesUtils.getConfigAsStream("replaceDocumentMultiTenantCollection/expectedTenant0Results.json")
        );

        JsonAssert.assertJsonEquals(expectedTenant0Results, JsonHandler.toJsonNode(tenant0Documents));

        // Ensure tenant 1 unchanged
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT_ID);
        List<JsonNode> tenant1Documents = getAllDocuments(dbRequestSingle);

        final JsonNode expectedTenant1Results = JsonHandler.getFromInputStream(
            PropertiesUtils.getConfigAsStream("replaceDocumentMultiTenantCollection/expectedTenant1Results.json")
        );

        JsonAssert.assertJsonEquals(expectedTenant1Results, JsonHandler.toJsonNode(tenant1Documents));
    }

    @Test
    @RunWithCustomExecutor
    public void testReplaceCrossTenantCollectionDocuments() throws Exception {
        // Given: (multi-tenant data set)
        DbRequestSingle dbRequestSingle = new DbRequestSingle(
            vitamCollectionCrossTenant,
            Collections::emptyList,
            ElasticsearchIndexAlias.ofCrossTenantCollection(vitamCollectionCrossTenant.getName())
        );
        assertEquals(0, vitamCollectionCrossTenant.getCollection().countDocuments());
        DocumentValidator documentValidator = mock(DocumentValidator.class);

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        dbRequestSingle.execute(
            new Insert()
                .setData(
                    JsonHandler.getFromInputStream(
                        PropertiesUtils.getConfigAsStream("replaceDocumentCrossTenantCollection/inputDataSet.json")
                    )
                ),
            0,
            documentValidator
        );

        // When
        dbRequestSingle.replaceDocuments(
            Map.of(
                "Identifier2",
                JsonHandler.createObjectNode().put("Identifier", "Identifier2").put("Description", "NewDescription2"),
                "Identifier4",
                JsonHandler.createObjectNode().put("Identifier", "Identifier4").put("NewField", "NewFieldValue")
            ),
            "Identifier",
            vitamCollectionCrossTenant
        );

        // Then
        List<JsonNode> updatedDocuments = getAllDocuments(dbRequestSingle);

        final JsonNode expectedTenant0Results = JsonHandler.getFromInputStream(
            PropertiesUtils.getConfigAsStream("replaceDocumentCrossTenantCollection/expectedResults.json")
        );

        JsonAssert.assertJsonEquals(expectedTenant0Results, JsonHandler.toJsonNode(updatedDocuments));
    }

    @Test
    @RunWithCustomExecutor
    public void testReplaceNotFoundDocumentThenThrowDatabaseException() {
        // Given
        DbRequestSingle dbRequestSingle = new DbRequestSingle(
            vitamCollectionCrossTenant,
            Collections::emptyList,
            ElasticsearchIndexAlias.ofCrossTenantCollection(vitamCollectionCrossTenant.getName())
        );
        assertEquals(0, vitamCollectionCrossTenant.getCollection().countDocuments());

        // When / Then
        assertThatThrownBy(
            () ->
                dbRequestSingle.replaceDocuments(
                    Map.of("No such doc", JsonHandler.createObjectNode()),
                    "Title",
                    vitamCollectionCrossTenant
                )
        ).isInstanceOf(DatabaseException.class);
    }

    private List<JsonNode> getAllDocuments(DbRequestSingle dbRequestSingle)
        throws InvalidCreateOperationException, InvalidParseOperationException, DatabaseException, BadRequestException, VitamDBException, SchemaValidationException {
        final Select sortedSelectES = new Select();
        DbRequestResult sortedSelectESResult = dbRequestSingle.execute(sortedSelectES);
        return sortedSelectESResult
            .getDocuments(VitamDocument.class)
            .stream()
            .map(doc -> {
                try {
                    return BsonHelper.fromDocumentToJsonNode(doc);
                } catch (InvalidParseOperationException e) {
                    throw new RuntimeException(e);
                }
            })
            .sorted(Comparator.comparing(doc -> doc.get("Identifier").asText()))
            .collect(Collectors.toList());
    }

    @Test
    @RunWithCustomExecutor
    public void testInsertRequestWithValidationOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        DbRequestSingle dbRequestSingle = new DbRequestSingle(
            vitamCollection,
            Collections::emptyList,
            ElasticsearchIndexAlias.ofCrossTenantCollection(vitamCollection.getName())
        );
        assertEquals(0, vitamCollection.getCollection().countDocuments());

        // init by dbRequest
        final ArrayNode data = JsonHandler.createArrayNode();
        data.add(getNewDocument(GUIDFactory.newGUID().toString(), "title one", 1));
        data.add(getNewDocument(GUIDFactory.newGUID().toString(), "title two", 2));
        final Insert insert = new Insert();
        insert.setData(data);

        DocumentValidator documentValidator = mock(DocumentValidator.class);
        doNothing().when(documentValidator).validateDocument(any());

        final DbRequestResult insertResult = dbRequestSingle.execute(insert, 0, documentValidator);
        assertEquals(2, insertResult.getCount());
        assertEquals(2, vitamCollection.getCollection().countDocuments());
        insertResult.close();

        final Select select = new Select();
        select.setQuery(eq("Title.keyword", "title one"));
        final DbRequestResult selectResult = dbRequestSingle.execute(select);
        final List<VitamDocument> selectCursor = selectResult.getDocuments(VitamDocument.class);
        assertFalse(selectCursor.isEmpty());
        assertEquals(1, selectCursor.size());
        assertThat(selectCursor.get(0).getString("Title")).isEqualTo("title one");
        assertThat(selectCursor.get(0).getInteger("#version")).isEqualTo(0);
        selectCursor.clear();
        selectResult.close();
    }

    @Test
    @RunWithCustomExecutor
    public void testInsertRequestWithValidationFailure() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        DbRequestSingle dbRequestSingle = new DbRequestSingle(
            vitamCollection,
            Collections::emptyList,
            ElasticsearchIndexAlias.ofCrossTenantCollection(vitamCollection.getName())
        );
        assertEquals(0, vitamCollection.getCollection().countDocuments());

        // init by dbRequest
        final ArrayNode data = JsonHandler.createArrayNode();
        data.add(getNewDocument(GUIDFactory.newGUID().toString(), "title one", 1));
        final Insert insert = new Insert();
        insert.setData(data);

        DocumentValidator documentValidator = mock(DocumentValidator.class);
        doThrow(new SchemaValidationException("Prb...")).when(documentValidator).validateDocument(any());

        assertThatThrownBy(() -> dbRequestSingle.execute(insert, 0, documentValidator)).isInstanceOf(
            SchemaValidationException.class
        );

        assertEquals(0, vitamCollection.getCollection().countDocuments());
    }

    @Test
    @RunWithCustomExecutor
    public void testUpdateRequestWithValidationFailure() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        DbRequestSingle dbRequestSingle = new DbRequestSingle(
            vitamCollection,
            Collections::emptyList,
            ElasticsearchIndexAlias.ofCrossTenantCollection(vitamCollection.getName())
        );
        assertEquals(0, vitamCollection.getCollection().countDocuments());

        // init by dbRequest
        final ArrayNode data = JsonHandler.createArrayNode();
        data.add(getNewDocument(GUIDFactory.newGUID().toString(), "title one", 1));
        final Insert insert = new Insert();
        insert.setData(data);

        final DbRequestResult insertResult = dbRequestSingle.execute(insert, 0, mock(DocumentValidator.class));
        assertEquals(1, insertResult.getCount());
        assertEquals(1, vitamCollection.getCollection().countDocuments());
        insertResult.close();

        // update
        final Update update = new Update();
        update.setQuery(eq("Title.keyword", "title one"));
        update.addActions(UpdateActionHelper.set("Title", "new name"));

        DocumentValidator documentValidator = mock(DocumentValidator.class);
        doThrow(new SchemaValidationException("Prb...")).when(documentValidator).validateDocument(any());

        assertThatThrownBy(() -> dbRequestSingle.execute(update, documentValidator)).isInstanceOf(
            SchemaValidationException.class
        );

        // Ensure not updated
        final Select select = new Select();
        select.setQuery(eq("Title.keyword", "title one"));
        final DbRequestResult selectResult = dbRequestSingle.execute(select);
        final List<VitamDocument> selectCursor = selectResult.getDocuments(VitamDocument.class);
        assertFalse(selectCursor.isEmpty());
        assertEquals(1, selectCursor.size());
        assertThat(selectCursor.get(0).getInteger("#version")).isEqualTo(0);
        selectCursor.clear();
        selectResult.close();
    }

    @Test
    @RunWithCustomExecutor
    public void testOptimisticLockOK()
        throws InvalidParseOperationException, BadRequestException, DatabaseException, InvalidCreateOperationException, VitamDBException, SchemaValidationException {
        VitamConfiguration.setOptimisticLockSleepTime(10);
        VitamConfiguration.setOptimisticLockRetryNumber(5);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        assertEquals(0, vitamCollection.getCollection().countDocuments());

        // init by dbRequest
        final ArrayNode datas = JsonHandler.createArrayNode();
        datas.add(getNewDocument(GUIDFactory.newGUID().toString(), "Optimistic lock test", 3));
        final Insert insert = new Insert();
        insert.setData(datas);
        final DbRequestResult insertResult = new DbRequestSingle(
            vitamCollection,
            Collections::emptyList,
            ElasticsearchIndexAlias.ofCrossTenantCollection(vitamCollection.getName())
        ).execute(insert, 0, mock(DocumentValidator.class));
        assertEquals(1, insertResult.getCount());
        assertEquals(1, vitamCollection.getCollection().countDocuments());
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
        assertThat(document.getString("Title")).contains("thread_");
        assertThat(document.getInteger("_v")).isEqualTo(5);
    }

    private void update(CountDownLatch countDownLatch, int nbr) {
        try {
            VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
            final Update update = new Update();
            update.setQuery(eq("Numero", 3));
            update.addActions(UpdateActionHelper.set("Title", "thread_" + nbr));

            final DbRequestResult updateResult = new DbRequestSingle(
                vitamCollection,
                Collections::emptyList,
                ElasticsearchIndexAlias.ofCrossTenantCollection(vitamCollection.getName())
            ).execute(update, mock(DocumentValidator.class));
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
