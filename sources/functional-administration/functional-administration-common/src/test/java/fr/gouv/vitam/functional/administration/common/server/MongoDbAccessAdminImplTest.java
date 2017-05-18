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
package fr.gouv.vitam.functional.administration.common.server;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.and;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.match;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.or;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.functional.administration.common.AccessContract;
import fr.gouv.vitam.functional.administration.common.Profile;
import fr.gouv.vitam.functional.administration.common.embed.ProfileFormat;
import fr.gouv.vitam.functional.administration.common.embed.ProfileStatus;
import org.bson.Document;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

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
import fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.UPDATEACTION;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Delete;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.builder.request.single.Update;
import fr.gouv.vitam.common.database.server.DbRequestResult;
import fr.gouv.vitam.common.database.server.DbRequestSingle;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.junit.JunitHelper.ElasticsearchTestConfiguration;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.model.RegisterValueDetailModel;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterDetail;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterSummary;
import fr.gouv.vitam.functional.administration.common.ContractStatus;
import fr.gouv.vitam.functional.administration.common.FileFormat;
import fr.gouv.vitam.functional.administration.common.FileRules;
import fr.gouv.vitam.functional.administration.common.IngestContract;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;

public class MongoDbAccessAdminImplTest {

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    static MongodExecutable mongodExecutable;
    static MongodProcess mongod;
    static MongoClient mongoClient;
    static JunitHelper junitHelper;
    private final static String CLUSTER_NAME = "vitam-cluster";
    static final String DATABASE_HOST = "localhost";
    static final String DATABASE_NAME = "vitamtest";
    static final String COLLECTION_NAME = "FileFormat";
    static final String COLLECTION_RULES = "FileRules";
    private static final String ACCESSION_REGISTER_DETAIL_COLLECTION = "AccessionRegisterDetail";

    private static final String REUSE_RULE = "ReuseRule";
    private static final String RULE_ID_VALUE = "APK-485";
    private static final String RULE_ID_VALUE_2 = "APK-48";
    private static final String RULE_ID = "RuleId";
    private static final String FILEFORMAT_PUID = "x-fmt/33";
    private static final String FILEFORMAT_PUID_2 = "x-fmt/44";
    private static final String FILEFORMAT_PUID_3 = "x-fmt/55";
    private static final String AGENCY = "Agency";
    private static final Integer TENANT_ID = 0;

    static int port;
    static MongoDbAccessAdminImpl mongoAccess;
    static FileFormat fileFormat1;
    static FileFormat fileFormat2;
    static FileFormat fileFormat3;
    static FileRules fileRules1;
    static FileRules fileRules2;
    static AccessionRegisterDetail register;
    static IngestContract contract;
    static AccessContract accessContract;
    static Profile profile;

    private static ElasticsearchTestConfiguration esConfig = null;
    private final static String HOST_NAME = "127.0.0.1";
    private static ElasticsearchAccessFunctionalAdmin esClient;

    @BeforeClass
    @RunWithCustomExecutor
    public static void setUpBeforeClass() throws Exception {
        final MongodStarter starter = MongodStarter.getDefaultInstance();
        junitHelper = JunitHelper.getInstance();
        try {
            esConfig = JunitHelper.startElasticsearchForTest(tempFolder, CLUSTER_NAME);
        } catch (final VitamApplicationServerException e1) {
            assumeTrue(false);
        }

        final List<ElasticsearchNode> esNodes = new ArrayList<>();
        esNodes.add(new ElasticsearchNode(HOST_NAME, esConfig.getTcpPort()));
        esClient = new ElasticsearchAccessFunctionalAdmin(CLUSTER_NAME, esNodes);
        FunctionalAdminCollections.FORMATS.initialize(esClient);
        FunctionalAdminCollections.RULES.initialize(esClient);
        // FunctionalAdminCollections.INGEST_CONTRACT.initialize(esClient);
        port = junitHelper.findAvailablePort();
        mongodExecutable = starter.prepare(new MongodConfigBuilder()
            .version(Version.Main.PRODUCTION)
            .net(new Net(port, Network.localhostIsIPv6()))
            .build());

        mongod = mongodExecutable.start();
        final List<MongoDbNode> nodes = new ArrayList<>();
        nodes.add(new MongoDbNode(DATABASE_HOST, port));
        mongoAccess = MongoDbAccessAdminFactory.create(new DbConfigurationImpl(nodes, DATABASE_NAME));

        final List<String> testList = new ArrayList<>();
        testList.add("test1");

        final List<String> testList2 = new ArrayList<>();
        testList.add("test2");

        String now = LocalDateUtil.now().toString();

        fileFormat1 = new FileFormat()
            .setCreatedDate(now)
            .setExtension(testList)
            .setMimeType(testList)
            .setName("this is a very long name")
            .setPriorityOverIdList(testList)
            .setPronomVersion("pronom version")
            .setPUID(FILEFORMAT_PUID)
            .setVersion("version");

        fileFormat2 = new FileFormat()
            .setCreatedDate(now)
            .setExtension(testList)
            .setMimeType(testList)
            .setName("Acrobat PDF 1.0 - Portable Document Format")
            .setPriorityOverIdList(testList)
            .setPronomVersion("pronom version")
            .setPUID(FILEFORMAT_PUID_2)
            .setVersion("version");

        fileFormat3 = new FileFormat()
            .setCreatedDate(now)
            .setExtension(testList)
            .setMimeType(testList2)
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
            .setRuleMeasurement("Annee");

        fileRules2 = new FileRules(TENANT_ID)
            .setCreationDate(now)
            .setRuleId(RULE_ID_VALUE_2)
            .setRuleValue(" 3D étudiants avec actes de naissance 10/10/2000 18 e siècle NFZ42020")
            .setRuleType(REUSE_RULE)
            .setRuleDescription("testList")
            .setRuleDuration("20")
            .setRuleMeasurement("Annee");

        final RegisterValueDetailModel initialValue = new RegisterValueDetailModel(1, 0, 1);
        register = new AccessionRegisterDetail(TENANT_ID)
            .setObjectSize(initialValue)
            .setOriginatingAgency(AGENCY)
            .setSubmissionAgency(AGENCY)
            .setStartDate("startDate")
            .setEndDate("endDate")
            .setTotalObjectGroups(initialValue)
            .setTotalObjects(initialValue)
            .setTotalUnits(initialValue);

        contract = createContract();

        accessContract = createAccessContract();

        profile = createProfile();

    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        if (esConfig != null) {
            JunitHelper.stopElasticsearchForTest(esConfig);
        }
        mongod.stop();
        mongodExecutable.stop();
        junitHelper.releasePort(port);
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
        FunctionalAdminCollections formatCollection = FunctionalAdminCollections.FORMATS;
        mongoAccess.insertDocuments(arrayNode, formatCollection);
        assertEquals("FileFormat", formatCollection.getName());
        final MongoClient client = new MongoClient(new ServerAddress(DATABASE_HOST, port));
        final MongoCollection<Document> collection = client.getDatabase(DATABASE_NAME).getCollection(COLLECTION_NAME);
        assertEquals(3, collection.count());

        // find all
        QueryBuilder query = QueryBuilders.matchAllQuery();
        final SearchResponse requestResponse = formatCollection.getEsClient().search(formatCollection, query, null);
        assertEquals(3, requestResponse.getHits().getTotalHits());

        // find one by id
        final Select select = new Select();
        select.setQuery(and()
            .add(match(FileFormat.NAME, "name"))
            .add(eq(FileFormat.PUID, FILEFORMAT_PUID)));
        final MongoCursor<VitamDocument<?>> fileList =
            mongoAccess.findDocuments(select.getFinalSelect(), formatCollection);
        final FileFormat f1 = (FileFormat) fileList.next();
        final String id = f1.getString("_id");
        final FileFormat f2 = (FileFormat) mongoAccess.getDocumentById(id, formatCollection);
        assertEquals(f2, f1);
        formatCollection.getEsClient().refreshIndex(formatCollection);
        assertEquals(false, fileList.hasNext());

        // Test select by query with order on name
        final Select selectWithSortName = new Select();
        selectWithSortName.setQuery(and().add(match(FileFormat.NAME, "acrobat")));
        selectWithSortName.addOrderByDescFilter(FileFormat.NAME);
        DbRequestSingle dbrequestSort = new DbRequestSingle(formatCollection.getVitamCollection());
        DbRequestResult selectSortResult = dbrequestSort.execute(selectWithSortName);
        final MongoCursor<VitamDocument<?>> selectSortList = selectSortResult.getCursor();
        assertEquals(true, selectSortList.hasNext());
        FileFormat fileFormatFirst = (FileFormat) selectSortList.next();
        assertEquals(FILEFORMAT_PUID_3, fileFormatFirst.getString(FileFormat.PUID));
        FileFormat fileFormatSecond = (FileFormat) selectSortList.next();
        assertEquals(FILEFORMAT_PUID_2, fileFormatSecond.getString(FileFormat.PUID));
        selectSortList.close();

        // Test select by query with order on id
        final Select selectWithSortId = new Select();
        selectWithSortId.setQuery(match(FileFormat.NAME, "acrobat"));
        selectWithSortName.addOrderByAscFilter(FileFormat.PUID);
        DbRequestSingle dbrequestSortId = new DbRequestSingle(formatCollection.getVitamCollection());
        DbRequestResult selectSortIdResult = dbrequestSortId.execute(selectWithSortId);
        final MongoCursor<VitamDocument<?>> selectSortIdList = selectSortIdResult.getCursor();
        assertEquals(true, selectSortIdList.hasNext());
        fileFormatFirst = (FileFormat) selectSortIdList.next();
        assertEquals(FILEFORMAT_PUID_2, fileFormatFirst.getString(FileFormat.PUID));
        fileFormatSecond = (FileFormat) selectSortIdList.next();
        assertEquals(FILEFORMAT_PUID_3, fileFormatSecond.getString(FileFormat.PUID));
        selectSortIdList.close();

        // Test update and delete by query
        final Update update = new Update();
        update.setQuery(match(FileFormat.NAME, "name"));
        update.addActions(UpdateActionHelper.set(FileFormat.NAME, "new name"));
        DbRequestSingle dbrequest = new DbRequestSingle(formatCollection.getVitamCollection());
        DbRequestResult updateResult = dbrequest.execute(update);
        assertEquals(1, updateResult.getCount());
        formatCollection.getEsClient().refreshIndex(formatCollection);

        final Delete delete = new Delete();
        delete.setQuery(match(FileFormat.NAME, "new name"));
        DbRequestResult deleteResult = dbrequest.execute(delete);
        assertEquals(1, deleteResult.getCount());
        assertEquals(2, collection.count());
        fileList.close();
        formatCollection.getEsClient().deleteIndex(formatCollection);
        mongoAccess.deleteCollection(formatCollection);
        client.close();
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
        FunctionalAdminCollections rulesCollection = FunctionalAdminCollections.RULES;
        mongoAccess.insertDocuments(arrayNode, rulesCollection);
        assertEquals("FileRules", rulesCollection.getName());
        final MongoClient client = new MongoClient(new ServerAddress(DATABASE_HOST, port));
        final MongoCollection<Document> collection = client.getDatabase(DATABASE_NAME).getCollection(COLLECTION_RULES);
        assertEquals(2, collection.count());

        final Select select = new Select();
        select.setQuery(and()
            .add(and()
                .add(match(FileRules.RULEVALUE, "10/10/2000"))
                .add(match(FileRules.RULEVALUE, "3D"))
                .add(match(FileRules.RULEVALUE, "17"))
                .add(match(FileRules.RULEVALUE, "siecle"))
                .add(match(FileRules.RULEVALUE, "acte"))
                .add(match(FileRules.RULEVALUE, "42020"))
                .add(match(FileRules.RULEVALUE, "NFZ")))
            .add(or()
                .add(eq(FileRules.RULETYPE, REUSE_RULE))
                .add(eq(FileRules.RULETYPE, "AccessRule"))));
        final MongoCursor<VitamDocument<?>> fileList =
            mongoAccess.findDocuments(select.getFinalSelect(), rulesCollection);
        final FileRules f1 = (FileRules) fileList.next();
        assertEquals(RULE_ID_VALUE, f1.getString(RULE_ID));
        rulesCollection.getEsClient().refreshIndex(rulesCollection);

        // Test select by query with order on rule value
        final Select selectWithSortName = new Select();
        selectWithSortName.setQuery(and().add(match(FileRules.RULEVALUE, "siecle")));
        selectWithSortName.addOrderByDescFilter(FileRules.RULEVALUE);
        DbRequestSingle dbrequestSort = new DbRequestSingle(rulesCollection.getVitamCollection());
        DbRequestResult selectSortResult = dbrequestSort.execute(selectWithSortName);
        final MongoCursor<VitamDocument<?>> selectSortList = selectSortResult.getCursor();
        assertEquals(true, selectSortList.hasNext());
        FileRules fileRuleFirst = (FileRules) selectSortList.next();
        assertEquals(RULE_ID_VALUE_2, fileRuleFirst.getString(FileRules.RULEID));
        FileRules fileRuleSecond = (FileRules) selectSortList.next();
        assertEquals(RULE_ID_VALUE, fileRuleSecond.getString(FileRules.RULEID));
        selectSortList.close();
        
        QueryBuilder query = QueryBuilders.matchAllQuery();
        SearchResponse requestResponse =
            rulesCollection.getEsClient()
                .search(rulesCollection, query, null);
        fileList.close();
        assertEquals(2, requestResponse.getHits().getTotalHits());
        mongoAccess.deleteCollection(rulesCollection);
        assertEquals(0, collection.count());
        client.close();
    }

    @Test
    @RunWithCustomExecutor
    public void testAccessionRegister() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final JsonNode jsonNode = JsonHandler.toJsonNode(register);
        mongoAccess.insertDocument(jsonNode, FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL);
        assertEquals(ACCESSION_REGISTER_DETAIL_COLLECTION,
            FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getName());
        final MongoClient client = new MongoClient(new ServerAddress(DATABASE_HOST, port));
        final MongoCollection<Document> collection =
            client.getDatabase(DATABASE_NAME).getCollection(ACCESSION_REGISTER_DETAIL_COLLECTION);
        assertEquals(1, collection.count());
        final Map<String, Object> updateMap = new HashMap<>();
        updateMap.put(AccessionRegisterSummary.TOTAL_OBJECTGROUPS, 1);
        mongoAccess.updateDocumentByMap(updateMap, jsonNode, FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL,
            UPDATEACTION.SET);
        mongoAccess.deleteCollection(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL);
        assertEquals(0, collection.count());
        client.close();
    }

    @Test
    @RunWithCustomExecutor
    public void testIngestContract() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        FunctionalAdminCollections contractCollection = FunctionalAdminCollections.INGEST_CONTRACT;
        final String id = GUIDFactory.newIngestContractGUID(TENANT_ID).getId();
        contract.setId(id);
        JsonNode jsonContract = JsonHandler.toJsonNode(contract);
        final ArrayNode arrayNode = JsonHandler.createArrayNode();
        arrayNode.add(jsonContract);
        final MongoClient client = new MongoClient(new ServerAddress(DATABASE_HOST, port));
        final MongoCollection<Document> collection =
            client.getDatabase(DATABASE_NAME).getCollection(FunctionalAdminCollections.INGEST_CONTRACT.getName());
        for (String c : client.getDatabase(DATABASE_NAME).listCollectionNames()) {
            System.out.println(c);
        }
        mongoAccess.insertDocuments(arrayNode, contractCollection);
        System.out.println(arrayNode.toString());
        assertEquals(1, collection.count());
        mongoAccess.deleteCollection(contractCollection);
        assertEquals(0, collection.count());
        client.close();
    }


    @Test
    @RunWithCustomExecutor
    public void testAccessContract() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        FunctionalAdminCollections contractCollection = FunctionalAdminCollections.ACCESS_CONTRACT;
        final String id = GUIDFactory.newIngestContractGUID(TENANT_ID).getId();
        contract.setId(id);
        JsonNode jsonContract = JsonHandler.toJsonNode(contract);
        final ArrayNode arrayNode = JsonHandler.createArrayNode();
        arrayNode.add(jsonContract);
        final MongoClient client = new MongoClient(new ServerAddress(DATABASE_HOST, port));
        final MongoCollection<Document> collection =
            client.getDatabase(DATABASE_NAME).getCollection(FunctionalAdminCollections.ACCESS_CONTRACT.getName());
        for (String c : client.getDatabase(DATABASE_NAME).listCollectionNames()) {
            System.out.println(c);
        }
        mongoAccess.insertDocuments(arrayNode, contractCollection);
        System.out.println(arrayNode.toString());
        assertEquals(1, collection.count());
        mongoAccess.deleteCollection(contractCollection);
        assertEquals(0, collection.count());
        client.close();
    }

    @Test
    @RunWithCustomExecutor
    public void testProfile() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        FunctionalAdminCollections profileCollection = FunctionalAdminCollections.PROFILE;
        final String id = GUIDFactory.newProfileGUID(TENANT_ID).getId();
        profile.setId(id);
        JsonNode jsonprofile = JsonHandler.toJsonNode(profile);
        final ArrayNode arrayNode = JsonHandler.createArrayNode();
        arrayNode.add(jsonprofile);
        final MongoClient client = new MongoClient(new ServerAddress(DATABASE_HOST, port));
        final MongoCollection<Document> collection =
            client.getDatabase(DATABASE_NAME).getCollection(FunctionalAdminCollections.PROFILE.getName());
        for (String c : client.getDatabase(DATABASE_NAME).listCollectionNames()) {
            System.out.println(c);
        }
        mongoAccess.insertDocuments(arrayNode, profileCollection);
        System.out.println(arrayNode.toString());
        assertEquals(1, collection.count());
        mongoAccess.deleteCollection(profileCollection);
        assertEquals(0, collection.count());
        client.close();
    }


    @Test
    @RunWithCustomExecutor
    public void testFindContract()
        throws ReferentialException, InvalidCreateOperationException, InvalidParseOperationException,
        DatabaseException {


        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        FunctionalAdminCollections contractCollection = FunctionalAdminCollections.INGEST_CONTRACT;
        final String id = GUIDFactory.newIngestContractGUID(TENANT_ID).getId();
        contract.setId(id);
        JsonNode jsonContract = JsonHandler.toJsonNode(contract);
        final ArrayNode arrayNode = JsonHandler.createArrayNode();
        arrayNode.add(jsonContract);
        mongoAccess.insertDocuments(arrayNode, contractCollection);

        final Select select = new Select();
        select.setQuery(and()
            .add(eq(IngestContract.NAME, "aName"))
            .add(or()
                .add(eq(IngestContract.CREATIONDATE, "10/12/2016"))));
        final MongoCursor<VitamDocument<?>> contracts =
            mongoAccess.findDocuments(select.getFinalSelect(), contractCollection);
        final IngestContract foundContract = (IngestContract) contracts.next();
        contracts.close();
        assertEquals("aName", foundContract.getString(IngestContract.NAME));
        mongoAccess.deleteCollection(contractCollection);

    }

    @Test
    @RunWithCustomExecutor
    public void testFindAccessContract()
        throws ReferentialException, InvalidCreateOperationException, InvalidParseOperationException,
        DatabaseException {


        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        FunctionalAdminCollections contractCollection = FunctionalAdminCollections.ACCESS_CONTRACT;
        final String id = GUIDFactory.newIngestContractGUID(TENANT_ID).getId();
        contract.setId(id);
        JsonNode jsonContract = JsonHandler.toJsonNode(contract);
        final ArrayNode arrayNode = JsonHandler.createArrayNode();
        arrayNode.add(jsonContract);
        mongoAccess.insertDocuments(arrayNode, contractCollection);

        final Select select = new Select();
        select.setQuery(and()
            .add(eq(AccessContract.NAME, "aName"))
            .add(or()
                .add(eq(AccessContract.CREATIONDATE, "10/12/2016"))));
        final MongoCursor<VitamDocument<?>> contracts =
            mongoAccess.findDocuments(select.getFinalSelect(), contractCollection);
        final AccessContract foundContract = (AccessContract) contracts.next();
        contracts.close();
        assertEquals("aName", foundContract.getString(AccessContract.NAME));
        mongoAccess.deleteCollection(contractCollection);

    }


    @Test
    @RunWithCustomExecutor
    public void testFindProfile()
        throws ReferentialException, InvalidCreateOperationException, InvalidParseOperationException,
        DatabaseException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        FunctionalAdminCollections profileCollection = FunctionalAdminCollections.PROFILE;
        final String id = GUIDFactory.newProfileGUID(TENANT_ID).getId();
        profile.setId(id);
        JsonNode jsonProfile = JsonHandler.toJsonNode(profile);
        final ArrayNode arrayNode = JsonHandler.createArrayNode();
        arrayNode.add(jsonProfile);
        mongoAccess.insertDocuments(arrayNode, profileCollection);

        final Select select = new Select();
        select.setQuery(and()
            .add(eq(Profile.IDENTIFIER, "FakeId"))
            .add(or()
                .add(eq(Profile.CREATIONDATE, "10/12/2016"))));
        final MongoCursor<VitamDocument<?>> profiles =
            mongoAccess.findDocuments(select.getFinalSelect(), profileCollection);
        final Profile foundProfile = (Profile) profiles.next();
        profiles.close();
        assertEquals("FakeId", foundProfile.getString(Profile.IDENTIFIER));
        mongoAccess.deleteCollection(profileCollection);

    }


    private static IngestContract createContract() {
        IngestContract contract = new IngestContract(TENANT_ID);
        String name = "aName";
        String description = "aDescription of the contract";
        String lastupdate = "10/12/2016";
        contract
            .setName(name)
            .setDescription(description).setStatus(ContractStatus.ACTIVE)
            .setLastupdate(lastupdate)
            .setCreationdate(lastupdate)
            .setActivationdate(lastupdate).setDeactivationdate(lastupdate);
        return contract;
    }

    private static AccessContract createAccessContract() {
        AccessContract contract = new AccessContract(TENANT_ID);
        String name = "aName";
        String description = "aDescription of the access contract";
        String lastupdate = "10/12/2016";
        Set<String> originatingAgencies = new HashSet<>();
        originatingAgencies.add("Fake");

        contract
            .setName(name)
            .setDescription(description).setStatus(ContractStatus.ACTIVE)
            .setOriginatingAgencies(originatingAgencies)
            .setLastupdate(lastupdate)
            .setCreationdate(lastupdate)
            .setActivationdate(lastupdate).setDeactivationdate(lastupdate);
        return contract;
    }

    private static Profile createProfile() {
        Profile profile = new Profile(TENANT_ID);
        String name = "aName";
        String description = "aDescription of the profile";
        String lastupdate = "10/12/2016";
        profile
            .setIdentifier("FakeId")
            .setName(name)
            .setDescription(description)
            .setStatus(ProfileStatus.INACTIVE)
            .setFormat(ProfileFormat.XSD)
            .setLastupdate(lastupdate)
            .setCreationdate(lastupdate)
            .setActivationdate(lastupdate).setDeactivationdate(lastupdate);
        return profile;
    }
}
