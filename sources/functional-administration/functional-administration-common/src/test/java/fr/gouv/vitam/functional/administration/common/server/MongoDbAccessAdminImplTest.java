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
package fr.gouv.vitam.functional.administration.common.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.client.MongoCollection;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Delete;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.builder.request.single.Update;
import fr.gouv.vitam.common.database.server.DbRequestResult;
import fr.gouv.vitam.common.database.server.DbRequestSingle;
import fr.gouv.vitam.common.database.server.DocumentValidator;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.SchemaValidationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.administration.ActivationStatus;
import fr.gouv.vitam.common.model.administration.ContextStatus;
import fr.gouv.vitam.common.model.administration.IngestContractCheckState;
import fr.gouv.vitam.common.model.administration.ProfileFormat;
import fr.gouv.vitam.common.model.administration.ProfileStatus;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.AccessContract;
import fr.gouv.vitam.functional.administration.common.Context;
import fr.gouv.vitam.functional.administration.common.FileFormat;
import fr.gouv.vitam.functional.administration.common.FileRules;
import fr.gouv.vitam.functional.administration.common.IngestContract;
import fr.gouv.vitam.functional.administration.common.Profile;
import fr.gouv.vitam.functional.administration.common.config.ElasticsearchFunctionalAdminIndexManager;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import org.bson.Document;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.and;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.match;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.or;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

public class MongoDbAccessAdminImplTest {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MongoDbAccessAdminImplTest.class);
    private static final ElasticsearchFunctionalAdminIndexManager indexManager =
        FunctionalAdminCollectionsTestUtils.createTestIndexManager();

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    public static final String PREFIX = GUIDFactory.newGUID().getId();

    @ClassRule
    public static MongoRule mongoRule = new MongoRule(
        MongoDbAccess.getMongoClientSettingsBuilder(FunctionalAdminCollections.getClasses())
    );

    @ClassRule
    public static ElasticsearchRule elasticsearchRule = new ElasticsearchRule();

    private static final String REUSE_RULE = "ReuseRule";
    private static final String RULE_ID_VALUE = "APK-485";
    private static final String RULE_ID_VALUE_2 = "APK-48";
    private static final String RULE_ID = "RuleId";
    private static final String FILEFORMAT_PUID = "x-fmt/33";
    private static final String FILEFORMAT_PUID_2 = "x-fmt/44";
    private static final String FILEFORMAT_PUID_3 = "x-fmt/55";
    private static final Integer TENANT_ID = 0;

    private static final String DEFAULT_DATE = "2018-04-05T13:34:40.234";

    static MongoDbAccessAdminImpl mongoAccess;
    static FileFormat fileFormat1;
    static FileFormat fileFormat2;
    static FileFormat fileFormat3;
    static FileRules fileRules1;
    static FileRules fileRules2;
    static IngestContract contract;
    static AccessContract accessContract;
    static Profile profile;
    static Context context;

    private static ElasticsearchAccessFunctionalAdmin esClient;

    @BeforeClass
    @RunWithCustomExecutor
    public static void setUpBeforeClass() throws Exception {
        final List<ElasticsearchNode> esNodes = new ArrayList<>();
        esNodes.add(new ElasticsearchNode(ElasticsearchRule.getHost(), ElasticsearchRule.getPort()));
        esClient = new ElasticsearchAccessFunctionalAdmin(elasticsearchRule.getClusterName(), esNodes, indexManager);
        final List<MongoDbNode> nodes = new ArrayList<>();
        nodes.add(new MongoDbNode("localhost", mongoRule.getDataBasePort()));
        mongoAccess = MongoDbAccessAdminFactory.create(
            new DbConfigurationImpl(nodes, mongoRule.getMongoDatabase().getName()),
            Collections::emptyList,
            indexManager
        );

        final List<String> testList = new ArrayList<>();
        testList.add("test1");

        final String now = LocalDateUtil.now().toString();

        fileFormat1 = new FileFormat()
            .setCreatedDate(now)
            .setExtension(testList)
            .setMimeType("test1")
            .setName("this is a very long name")
            .setPriorityOverIdList(testList)
            .setPronomVersion("pronom version")
            .setPUID(FILEFORMAT_PUID)
            .setVersion("version");

        fileFormat2 = new FileFormat()
            .setCreatedDate(now)
            .setExtension(testList)
            .setMimeType("test2")
            .setName("Acrobat PDF 1.0 - Portable Document Format")
            .setPriorityOverIdList(testList)
            .setPronomVersion("pronom version")
            .setPUID(FILEFORMAT_PUID_2)
            .setVersion("version");

        fileFormat3 = new FileFormat()
            .setCreatedDate(now)
            .setExtension(testList)
            .setMimeType("test3")
            .setName("Acrobat PDF/X - Portable Document Format - Exchange 1a:2001")
            .setPriorityOverIdList(testList)
            .setPronomVersion("pronom version")
            .setPUID(FILEFORMAT_PUID_3)
            .setVersion("version");

        fileRules1 = new FileRules(TENANT_ID)
            .setCreationDate(now)
            .setRuleId(RULE_ID_VALUE)
            .setRuleValue(" 3D étudiants avec actes de naissance 10/10/2000 17 e siècle NFZ42020")
            .setRuleType(REUSE_RULE)
            .setRuleDescription("testList")
            .setRuleDuration("10")
            .setRuleMeasurement("Annee")
            .setUpdateDate("2019-10-11");

        fileRules2 = new FileRules(TENANT_ID)
            .setCreationDate(now)
            .setRuleId(RULE_ID_VALUE_2)
            .setRuleValue(" 3D étudiants avec actes de naissance 10/10/2000 18 e siècle NFZ42020")
            .setRuleType(REUSE_RULE)
            .setRuleDescription("testList")
            .setRuleDuration("20")
            .setRuleMeasurement("Annee")
            .setUpdateDate("2019-10-10");

        contract = createContract();

        accessContract = createAccessContract();

        profile = createProfile();

        context = createContext();

        FunctionalAdminCollectionsTestUtils.beforeTestClass(mongoRule.getMongoDatabase(), PREFIX, esClient);
    }

    @AfterClass
    public static void tearDownAfterClass() {
        FunctionalAdminCollectionsTestUtils.afterTestClass(true);
    }

    @After
    public void after() {
        FunctionalAdminCollectionsTestUtils.afterTest();
    }

    @Test
    @RunWithCustomExecutor
    public void testImplementFunction() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final JsonNode jsonNode1 = JsonHandler.toJsonNode(fileFormat1);
        final JsonNode jsonNode2 = JsonHandler.toJsonNode(fileFormat2);
        final JsonNode jsonNode3 = JsonHandler.toJsonNode(fileFormat3);
        final ArrayNode arrayNode = JsonHandler.createArrayNode();
        arrayNode.add(jsonNode1);
        arrayNode.add(jsonNode2);
        arrayNode.add(jsonNode3);
        final FunctionalAdminCollections formatCollection = FunctionalAdminCollections.FORMATS;
        mongoAccess.insertDocuments(arrayNode, formatCollection).close();
        assertEquals(PREFIX + "FileFormat", formatCollection.getName());
        final MongoCollection<Document> collection = mongoRule.getMongoCollection(
            FunctionalAdminCollections.FORMATS.getName()
        );
        assertEquals(3, collection.countDocuments());

        // find all
        final QueryBuilder query = QueryBuilders.matchAllQuery();
        final SearchResponse requestResponse = formatCollection.getEsClient().search(formatCollection, query, null);
        assertEquals(3, requestResponse.getHits().getTotalHits().value);

        // find one by id
        final Select select = new Select();
        select.setQuery(and().add(match(FileFormat.NAME, "name")).add(eq(FileFormat.PUID, FILEFORMAT_PUID)));
        final DbRequestResult fileList = mongoAccess.findDocuments(select.getFinalSelect(), formatCollection);
        final FileFormat f1 = fileList.getDocuments(FileFormat.class).get(0);
        final String id = f1.getString("#id");
        final FileFormat f2 = (FileFormat) mongoAccess.getDocumentById(id, formatCollection);

        assertEquals(f2.get("#id"), f1.getId());
        final String puid = f1.getString(FileFormat.PUID);
        final FileFormat f3 = (FileFormat) mongoAccess.getDocumentByUniqueId(puid, formatCollection, FileFormat.PUID);
        assertEquals(f3.get("#id"), f1.getId());
        formatCollection
            .getEsClient()
            .refreshIndex(indexManager.getElasticsearchIndexAliasResolver(formatCollection).resolveIndexName(null));
        assertEquals(1, fileList.getCount());
        fileList.close();

        // Test select by query with order on name
        final Select selectWithSortName = new Select();
        selectWithSortName.setQuery(and().add(match(FileFormat.NAME, "acrobat")));
        selectWithSortName.addOrderByDescFilter(FileFormat.NAME);
        final DbRequestSingle dbrequestSort = new DbRequestSingle(
            formatCollection.getVitamCollection(),
            Collections::emptyList,
            indexManager.getElasticsearchIndexAliasResolver(formatCollection).resolveIndexName(null)
        );
        final DbRequestResult selectSortResult = dbrequestSort.execute(selectWithSortName);
        final List<FileFormat> selectSortList = selectSortResult.getDocuments(FileFormat.class);
        assertFalse(selectSortList.isEmpty());
        FileFormat fileFormatFirst = selectSortList.get(0);
        assertEquals(FILEFORMAT_PUID_3, fileFormatFirst.getString(FileFormat.PUID));
        FileFormat fileFormatSecond = selectSortList.get(1);
        assertEquals(FILEFORMAT_PUID_2, fileFormatSecond.getString(FileFormat.PUID));
        selectSortList.clear();
        selectSortResult.close();

        // Test select by query with order on id
        final Select selectWithSortId = new Select();
        selectWithSortId.setQuery(match(FileFormat.NAME, "acrobat"));
        selectWithSortName.addOrderByAscFilter(FileFormat.PUID);
        final DbRequestSingle dbrequestSortId = new DbRequestSingle(
            formatCollection.getVitamCollection(),
            Collections::emptyList,
            indexManager.getElasticsearchIndexAliasResolver(formatCollection).resolveIndexName(null)
        );
        final DbRequestResult selectSortIdResult = dbrequestSortId.execute(selectWithSortId);
        final List<FileFormat> selectSortIdList = selectSortIdResult.getDocuments(FileFormat.class);
        assertFalse(selectSortIdList.isEmpty());
        fileFormatFirst = selectSortIdList.get(0);
        assertEquals(FILEFORMAT_PUID_2, fileFormatFirst.getString(FileFormat.PUID));
        fileFormatSecond = selectSortIdList.get(1);
        assertEquals(FILEFORMAT_PUID_3, fileFormatSecond.getString(FileFormat.PUID));
        selectSortIdList.clear();
        selectSortIdResult.close();

        // Test update and delete by query
        final Update update = new Update();
        update.setQuery(match(FileFormat.NAME, "name"));
        update.addActions(UpdateActionHelper.set(FileFormat.COMMENT, "new comment"));
        final DbRequestSingle dbrequest = new DbRequestSingle(
            formatCollection.getVitamCollection(),
            Collections::emptyList,
            indexManager.getElasticsearchIndexAliasResolver(formatCollection).resolveIndexName(null)
        );
        final DbRequestResult updateResult = dbrequest.execute(update, mock(DocumentValidator.class));
        assertEquals(1, updateResult.getCount());
        formatCollection
            .getEsClient()
            .refreshIndex(indexManager.getElasticsearchIndexAliasResolver(formatCollection).resolveIndexName(null));
        updateResult.close();

        final Delete delete = new Delete();
        delete.setQuery(match(FileFormat.COMMENT, "new comment"));
        final DbRequestResult deleteResult = dbrequest.execute(delete);
        assertEquals(1, deleteResult.getCount());
        assertEquals(2, collection.countDocuments());
        fileList.close();
        mongoAccess.deleteCollectionForTesting(formatCollection).close();
        deleteResult.close();
    }

    @Test
    @RunWithCustomExecutor
    public void testRulesFunction() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final JsonNode jsonNode1 = JsonHandler.toJsonNode(fileRules1);
        final JsonNode jsonNode2 = JsonHandler.toJsonNode(fileRules2);
        final ArrayNode arrayNode = JsonHandler.createArrayNode();
        arrayNode.add(jsonNode1);
        arrayNode.add(jsonNode2);
        final FunctionalAdminCollections rulesCollection = FunctionalAdminCollections.RULES;
        mongoAccess.insertDocuments(arrayNode, rulesCollection).close();
        assertEquals(PREFIX + "FileRules", rulesCollection.getName());
        final MongoCollection<Document> collection = mongoRule.getMongoCollection(
            FunctionalAdminCollections.RULES.getName()
        );
        assertEquals(2, collection.countDocuments());

        final Select select = new Select();
        select.setQuery(
            and()
                .add(
                    and()
                        .add(match(FileRules.RULEVALUE, "10/10/2000"))
                        .add(match(FileRules.RULEVALUE, "3D"))
                        .add(match(FileRules.RULEVALUE, "17"))
                        .add(match(FileRules.RULEVALUE, "siecle"))
                        .add(match(FileRules.RULEVALUE, "acte"))
                        .add(match(FileRules.RULEVALUE, "42020"))
                        .add(match(FileRules.RULEVALUE, "NFZ"))
                )
                .add(or().add(eq(FileRules.RULETYPE, REUSE_RULE)).add(eq(FileRules.RULETYPE, "AccessRule")))
        );
        final DbRequestResult fileList = mongoAccess.findDocuments(select.getFinalSelect(), rulesCollection);
        final FileRules f1 = fileList.getDocuments(FileRules.class).get(0);
        LOGGER.debug(JsonHandler.prettyPrint(f1));
        assertEquals(RULE_ID_VALUE, f1.getString(RULE_ID));
        rulesCollection
            .getEsClient()
            .refreshIndex(indexManager.getElasticsearchIndexAliasResolver(rulesCollection).resolveIndexName(null));

        final QueryBuilder query = QueryBuilders.matchAllQuery();
        final SearchResponse requestResponse = rulesCollection.getEsClient().search(rulesCollection, query, null);
        fileList.close();
        assertEquals(2, requestResponse.getHits().getTotalHits().value);
        mongoAccess.deleteCollectionForTesting(rulesCollection).close();
        assertEquals(0, collection.countDocuments());
    }

    @Test
    @RunWithCustomExecutor
    public void testAccessionRegister() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final JsonNode jsonNode = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream("accession_register_detail_OK.json")
        );
        mongoAccess.insertDocument(jsonNode, FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL).close();
        final MongoCollection<Document> collection = mongoRule.getMongoCollection(
            FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getName()
        );
        assertEquals(1, collection.countDocuments());
        mongoAccess.deleteCollectionForTesting(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL).close();
        assertEquals(0, collection.countDocuments());
    }

    @Test
    @RunWithCustomExecutor
    public void testIngestContract() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final FunctionalAdminCollections contractCollection = FunctionalAdminCollections.INGEST_CONTRACT;
        final String id = GUIDFactory.newContractGUID(TENANT_ID).getId();
        contract.setId(id);
        JsonNode jsonContract = JsonHandler.toJsonNode(contract);
        ((ObjectNode) jsonContract).put("Identifier", contract.getName());
        final ArrayNode arrayNode = JsonHandler.createArrayNode();
        arrayNode.add(jsonContract);
        final MongoCollection<Document> collection = mongoRule.getMongoCollection(
            FunctionalAdminCollections.INGEST_CONTRACT.getName()
        );
        mongoAccess.insertDocuments(arrayNode, contractCollection).close();
        assertEquals(1, collection.countDocuments());

        try {
            JsonNode update = JsonHandler.getFromString(
                "{\"$query\":{\"$eq\":{\"_id\":\"" +
                id +
                "\"}},\"$filter\":{},\"$action\":[{\"$set\":{\"Status\":\"ACTIVE\"}}]}"
            );
            mongoAccess.updateData(update, FunctionalAdminCollections.INGEST_CONTRACT);
            fail("This should fail, as no changes is made");
        } catch (BadRequestException e) {
            // do nothing
        }

        try {
            JsonNode update = JsonHandler.getFromString(
                "{\"$query\":{\"$eq\":{\"_id\":\"" +
                id +
                "\"}},\"$filter\":{},\"$action\":[{\"$set\":{\"Status\":\"BLOP\"}}]}"
            );
            mongoAccess.updateData(update, FunctionalAdminCollections.INGEST_CONTRACT);
            fail("This should fail, as schema validation is not ok");
        } catch (SchemaValidationException e) {
            // do nothing
        }
        mongoAccess.deleteCollectionForTesting(contractCollection).close();
        assertEquals(0, collection.countDocuments());
    }

    @Test
    @RunWithCustomExecutor
    public void testAccessContract() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final FunctionalAdminCollections contractCollection = FunctionalAdminCollections.ACCESS_CONTRACT;
        final String id = GUIDFactory.newContractGUID(TENANT_ID).getId();
        accessContract.setId(id);
        final JsonNode jsonContract = JsonHandler.toJsonNode(accessContract);
        ((ObjectNode) jsonContract).put("Identifier", accessContract.getName());
        ((ObjectNode) jsonContract).put("EveryDataObjectVersion", false);
        ((ObjectNode) jsonContract).put("EveryOriginatingAgency", false);
        ((ObjectNode) jsonContract).put("WritingPermission", true);
        ((ObjectNode) jsonContract).put("AccessLog", "INACTIVE");
        final ArrayNode arrayNode = JsonHandler.createArrayNode();
        arrayNode.add(jsonContract);
        final MongoCollection<Document> collection = mongoRule.getMongoCollection(
            FunctionalAdminCollections.ACCESS_CONTRACT.getName()
        );
        mongoAccess.insertDocuments(arrayNode, contractCollection).close();
        assertEquals(1, collection.countDocuments());

        try {
            JsonNode update = JsonHandler.getFromString(
                "{\"$query\":{\"$eq\":{\"_id\":\"" +
                id +
                "\"}},\"$filter\":{},\"$action\":[{\"$set\":{\"Status\":\"ACTIVE\"}}]}"
            );
            mongoAccess.updateData(update, FunctionalAdminCollections.ACCESS_CONTRACT);
            fail("This should fail, as no changes is made");
        } catch (BadRequestException e) {
            // do nothing
        }

        try {
            JsonNode update = JsonHandler.getFromString(
                "{\"$query\":{\"$eq\":{\"_id\":\"" +
                id +
                "\"}},\"$filter\":{},\"$action\":[{\"$set\":{\"Status\":\"BLOP\"}}]}"
            );
            mongoAccess.updateData(update, FunctionalAdminCollections.ACCESS_CONTRACT);
            fail("This should fail, as schema validation is not ok");
        } catch (SchemaValidationException e) {
            // do nothing
        }

        mongoAccess.deleteCollectionForTesting(contractCollection).close();
        assertEquals(0, collection.countDocuments());
    }

    @Test
    @RunWithCustomExecutor
    public void testProfile() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final FunctionalAdminCollections profileCollection = FunctionalAdminCollections.PROFILE;
        final String id = GUIDFactory.newProfileGUID(TENANT_ID).getId();
        profile.setId(id);
        final JsonNode jsonprofile = JsonHandler.toJsonNode(profile);
        final ArrayNode arrayNode = JsonHandler.createArrayNode();
        arrayNode.add(jsonprofile);
        final MongoCollection<Document> collection = mongoRule.getMongoCollection(
            FunctionalAdminCollections.PROFILE.getName()
        );
        mongoAccess.insertDocuments(arrayNode, profileCollection).close();
        assertEquals(1, collection.countDocuments());
        mongoAccess.deleteCollectionForTesting(profileCollection).close();
        assertEquals(0, collection.countDocuments());
    }

    @Test
    @RunWithCustomExecutor
    public void testFindContract()
        throws ReferentialException, InvalidCreateOperationException, InvalidParseOperationException, DatabaseException, SchemaValidationException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final FunctionalAdminCollections contractCollection = FunctionalAdminCollections.INGEST_CONTRACT;
        final String id = GUIDFactory.newContractGUID(TENANT_ID).getId();
        contract.setId(id);
        final JsonNode jsonContract = JsonHandler.toJsonNode(contract);
        ((ObjectNode) jsonContract).put("Identifier", GUIDFactory.newGUID().toString());
        final ArrayNode arrayNode = JsonHandler.createArrayNode();
        arrayNode.add(jsonContract);
        mongoAccess.insertDocuments(arrayNode, contractCollection).close();

        final Select select = new Select();
        select.setQuery(
            and().add(eq(IngestContract.NAME, "aName")).add(or().add(eq(IngestContract.CREATIONDATE, DEFAULT_DATE)))
        );
        final DbRequestResult contracts = mongoAccess.findDocuments(select.getFinalSelect(), contractCollection);
        final IngestContract foundContract = contracts.getDocuments(IngestContract.class).get(0);
        contracts.close();
        assertEquals("aName", foundContract.getString(IngestContract.NAME));
        mongoAccess.deleteCollectionForTesting(contractCollection).close();
    }

    @Test
    @RunWithCustomExecutor
    public void testFindAccessContract()
        throws ReferentialException, InvalidCreateOperationException, InvalidParseOperationException, DatabaseException, SchemaValidationException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final FunctionalAdminCollections contractCollection = FunctionalAdminCollections.ACCESS_CONTRACT;
        final String id = GUIDFactory.newContractGUID(TENANT_ID).getId();
        accessContract.setId(id);
        final JsonNode jsonContract = JsonHandler.toJsonNode(accessContract);
        final ArrayNode arrayNode = JsonHandler.createArrayNode();

        ObjectNode contractToPersist = JsonHandler.getFromJsonNode(jsonContract, ObjectNode.class);
        contractToPersist.put("Identifier", "Identifier" + GUIDFactory.newGUID().toString());
        contractToPersist.put("AccessLog", ActivationStatus.INACTIVE.toString());
        contractToPersist.put("EveryOriginatingAgency", false);
        contractToPersist.put("WritingPermission", false);
        contractToPersist.put("EveryDataObjectVersion", false);

        arrayNode.add(JsonHandler.toJsonNode(contractToPersist));

        mongoAccess.insertDocuments(arrayNode, contractCollection).close();

        final Select select = new Select();
        select.setQuery(
            and().add(eq(AccessContract.NAME, "aName")).add(or().add(eq(AccessContract.CREATIONDATE, DEFAULT_DATE)))
        );
        final DbRequestResult contracts = mongoAccess.findDocuments(select.getFinalSelect(), contractCollection);
        final AccessContract foundContract = contracts.getDocuments(AccessContract.class).get(0);
        contracts.close();
        assertEquals("aName", foundContract.getString(AccessContract.NAME));
        mongoAccess.deleteCollectionForTesting(contractCollection).close();
    }

    @Test
    @RunWithCustomExecutor
    public void testFindProfile()
        throws ReferentialException, InvalidCreateOperationException, InvalidParseOperationException, DatabaseException, SchemaValidationException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final FunctionalAdminCollections profileCollection = FunctionalAdminCollections.PROFILE;
        final String id = GUIDFactory.newProfileGUID(TENANT_ID).getId();
        profile.setId(id);
        final JsonNode jsonProfile = JsonHandler.toJsonNode(profile);
        final ArrayNode arrayNode = JsonHandler.createArrayNode();
        arrayNode.add(jsonProfile);
        mongoAccess.insertDocuments(arrayNode, profileCollection).close();

        final Select select = new Select();
        select.setQuery(
            and().add(eq(Profile.IDENTIFIER, "FakeId")).add(or().add(eq(Profile.CREATIONDATE, DEFAULT_DATE)))
        );
        final DbRequestResult profiles = mongoAccess.findDocuments(select.getFinalSelect(), profileCollection);
        final Profile foundProfile = profiles.getDocuments(Profile.class).get(0);
        profiles.close();
        assertEquals("FakeId", foundProfile.getString(Profile.IDENTIFIER));
        mongoAccess.deleteCollectionForTesting(profileCollection).close();
    }

    @Test
    @RunWithCustomExecutor
    public void testDeleteContext()
        throws ReferentialException, InvalidCreateOperationException, InvalidParseOperationException, DatabaseException, SchemaValidationException, BadRequestException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final FunctionalAdminCollections contextCollection = FunctionalAdminCollections.CONTEXT;
        final JsonNode jsonContext = JsonHandler.toJsonNode(context);
        final ArrayNode arrayNode = JsonHandler.createArrayNode();
        arrayNode.add(jsonContext);
        mongoAccess.insertDocuments(arrayNode, contextCollection).close();

        final Select select = new Select();
        select.setQuery(eq(Context.IDENTIFIER, "contextId"));
        final DbRequestResult result = mongoAccess.deleteDocument(select.getFinalSelect(), contextCollection);
        result.close();

        final DbRequestResult contextFound = mongoAccess.findDocuments(select.getFinalSelect(), contextCollection);
        assertEquals(0, contextFound.getTotal());
        contextFound.close();

        mongoAccess.deleteCollectionForTesting(contextCollection).close();
    }

    private static IngestContract createContract() {
        final IngestContract contract = new IngestContract(TENANT_ID);
        final String name = "aName";
        final String description = "aDescription of the contract";
        final String lastupdate = DEFAULT_DATE;
        contract
            .setName(name)
            .setDescription(description)
            .setStatus(ActivationStatus.ACTIVE)
            .setCheckParentLink(IngestContractCheckState.AUTHORIZED)
            .setLastupdate(lastupdate)
            .setCreationdate(lastupdate)
            .setActivationdate(lastupdate)
            .setDeactivationdate(lastupdate)
            .setMasterMandatory(true)
            .setEveryDataObjectVersion(false)
            .setFormatUnidentifiedAuthorized(true)
            .setEveryFormatType(false);
        return contract;
    }

    private static AccessContract createAccessContract() {
        final AccessContract contract = new AccessContract(TENANT_ID);
        final String name = "aName";
        final String description = "aDescription of the access contract";
        final String lastupdate = DEFAULT_DATE;
        final Set<String> originatingAgencies = new HashSet<>();
        originatingAgencies.add("Fake");

        contract
            .setName(name)
            .setDescription(description)
            .setStatus(ActivationStatus.ACTIVE)
            .setOriginatingAgencies(originatingAgencies)
            .setLastupdate(lastupdate)
            .setCreationdate(lastupdate)
            .setActivationdate(lastupdate)
            .setDeactivationdate(lastupdate);
        return contract;
    }

    private static Profile createProfile() {
        final Profile profile = new Profile(TENANT_ID);
        final String name = "aName";
        final String description = "aDescription of the profile";
        final String lastupdate = DEFAULT_DATE;
        profile
            .setIdentifier("FakeId")
            .setName(name)
            .setDescription(description)
            .setStatus(ProfileStatus.INACTIVE)
            .setFormat(ProfileFormat.XSD)
            .setLastupdate(lastupdate)
            .setCreationdate(lastupdate)
            .setActivationdate(lastupdate)
            .setDeactivationdate(lastupdate);
        return profile;
    }

    private static Context createContext() {
        ObjectNode node = JsonHandler.createObjectNode();
        node.put(VitamDocument.ID, GUIDFactory.newContractGUID(TENANT_ID).getId());
        node.put(Context.IDENTIFIER, "contextId");
        node.put(Context.NAME, "contextName");
        node.set(Context.PERMISSION, new ArrayNode(null));
        node.put(Context.SECURITY_PROFILE, "contextName");
        node.put(Context.STATUS, ContextStatus.INACTIVE.toString());
        node.put("EnableControl", false);
        node.put("CreationDate", "2019-02-10");
        node.put("LastUpdate", "2019-02-11");

        return new Context(node);
    }
}
