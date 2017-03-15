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
import java.util.List;
import java.util.Map;

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
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.UPDATEACTION;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
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
import fr.gouv.vitam.functional.administration.common.FileFormat;
import fr.gouv.vitam.functional.administration.common.FileRules;

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
    private static final String RULE_ID = "RuleId";
    private static final String FILEFORMAT_PUID = "x-fmt/33";
    private static final String AGENCY = "Agency";
    private static final Integer TENANT_ID = 0;

    static int port;
    static MongoDbAccessAdminImpl mongoAccess;
    static FileFormat file;
    static FileRules fileRules;
    static AccessionRegisterDetail register;
    private static ElasticsearchTestConfiguration esConfig = null;    
    private final static String HOST_NAME = "127.0.0.1";
    private static ElasticsearchAccessFunctionalAdmin esClient;

    @BeforeClass
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
        port = junitHelper.findAvailablePort();
        mongodExecutable = starter.prepare(new MongodConfigBuilder()
            .version(Version.Main.PRODUCTION)
            .net(new Net(port, Network.localhostIsIPv6()))
            .build());
        mongod = mongodExecutable.start();
        final List<MongoDbNode> nodes = new ArrayList<>();
        nodes.add(new MongoDbNode(DATABASE_HOST, port));
        mongoAccess = MongoDbAccessAdminFactory.create(
            new DbConfigurationImpl(nodes, DATABASE_NAME));

        final List<String> testList = new ArrayList<>();
        testList.add("test1");

        file = new FileFormat()
            .setCreatedDate("now")
            .setExtension(testList)
            .setMimeType(testList)
            .setName("this is a very long name")
            .setPriorityOverIdList(testList)
            .setPronomVersion("pronom version")
            .setPUID(FILEFORMAT_PUID)
            .setVersion("version");

        fileRules = new FileRules(TENANT_ID)
            .setRuleId(RULE_ID_VALUE)
            .setRuleValue("Actes de naissance")
            .setRuleType(REUSE_RULE)
            .setRuleDescription("testList")
            .setRuleDuration("10")
            .setRuleMeasurement("Annee");

        final RegisterValueDetailModel initialValue = new RegisterValueDetailModel(1, 0, 1);
        register = new AccessionRegisterDetail(TENANT_ID)
            .setObjectSize(initialValue)
            .setOriginatingAgency(AGENCY)
            .setId(AGENCY)
            .setSubmissionAgency(AGENCY)
            .setStartDate("startDate")
            .setEndDate("endDate")
            .setTotalObjectGroups(initialValue)
            .setTotalObjects(initialValue)
            .setTotalUnits(initialValue);

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
        final JsonNode jsonNode = JsonHandler.getFromString(file.toJson());
        final ArrayNode arrayNode = JsonHandler.createArrayNode();
        arrayNode.add(jsonNode);
        FunctionalAdminCollections formatCollection = FunctionalAdminCollections.FORMATS;
        mongoAccess.insertDocuments(arrayNode, formatCollection);
        assertEquals("FileFormat", formatCollection.getName());
        final MongoClient client = new MongoClient(new ServerAddress(DATABASE_HOST, port));
        final MongoCollection<Document> collection = client.getDatabase(DATABASE_NAME).getCollection(COLLECTION_NAME);
        assertEquals(1, collection.count());
        final Select select = new Select();
        select.setQuery(and()
            .add(match(FileFormat.NAME, "name"))
            .add(eq(FileFormat.PUID, FILEFORMAT_PUID)));
        final MongoCursor<FileFormat> fileList =
            (MongoCursor<FileFormat>) mongoAccess.findDocuments(select.getFinalSelect(), formatCollection);
        final FileFormat f1 = fileList.next();
        final String id = f1.getString("_id");
        final FileFormat f2 = (FileFormat) mongoAccess.getDocumentById(id, formatCollection);
        assertEquals(f2, f1);
        formatCollection.getEsClient().refreshIndex(formatCollection);
        QueryBuilder query = QueryBuilders.matchAllQuery();
        final SearchResponse requestResponse =
            formatCollection.getEsClient()
            .search(formatCollection, query, null);
        assertEquals(1, requestResponse.getHits().getTotalHits());
        formatCollection.getEsClient().deleteIndex(formatCollection);
        mongoAccess.deleteCollection(formatCollection);
        assertEquals(0, collection.count());
        fileList.close();
        client.close();
    }

    @Test
    @RunWithCustomExecutor
    public void testRulesFunction() throws Exception {
    	VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final JsonNode jsonNode = JsonHandler.getFromString(fileRules.toJson());
        final ArrayNode arrayNode = JsonHandler.createArrayNode();
        arrayNode.add(jsonNode);
        FunctionalAdminCollections rulesCollection = FunctionalAdminCollections.RULES;
        mongoAccess.insertDocuments(arrayNode, rulesCollection);
        assertEquals("FileRules", rulesCollection.getName());
        final MongoClient client = new MongoClient(new ServerAddress(DATABASE_HOST, port));
        final MongoCollection<Document> collection = client.getDatabase(DATABASE_NAME).getCollection(COLLECTION_RULES);
        assertEquals(1, collection.count());
        
        final Select select = new Select();
        select.setQuery(and()
            .add(match(FileRules.RULEVALUE, "acte"))
            .add(or()
                .add(eq(FileRules.RULETYPE, REUSE_RULE))
                .add(eq(FileRules.RULETYPE, "AccessRule")))
            );
        final MongoCursor<FileRules> fileList =
            (MongoCursor<FileRules>) mongoAccess.findDocuments(select.getFinalSelect(), rulesCollection);
        final FileRules f1 = fileList.next();
        assertEquals(RULE_ID_VALUE, f1.getString(RULE_ID));
        final String id = f1.getString(RULE_ID);
        final FileRules f2 = (FileRules) mongoAccess.getDocumentById(id, rulesCollection);
        rulesCollection.getEsClient().refreshIndex(rulesCollection);
        
        QueryBuilder query = QueryBuilders.matchAllQuery();
        SearchResponse requestResponse =
            rulesCollection.getEsClient()
            .search(rulesCollection, query, null);

        assertEquals(1, requestResponse.getHits().getTotalHits());
        mongoAccess.deleteCollection(rulesCollection);
        assertEquals(0, collection.count());
        
        fileList.close();
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
}
