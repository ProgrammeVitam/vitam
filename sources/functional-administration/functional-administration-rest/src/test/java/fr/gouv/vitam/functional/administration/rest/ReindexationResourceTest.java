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
package fr.gouv.vitam.functional.administration.rest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.jayway.restassured.RestAssured.given;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response.Status;

import fr.gouv.vitam.common.client.VitamClientFactory;
import org.jhades.JHades;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.database.parameter.SwitchIndexParameters;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchUtil;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.junit.JunitHelper.ElasticsearchTestConfiguration;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.server.AdminManagementConfiguration;
import fr.gouv.vitam.functional.administration.common.server.ElasticsearchAccessFunctionalAdmin;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminFactory;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessReferential;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;

public class ReindexationResourceTest {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ReindexationResourceTest.class);


    static MongodExecutable mongodExecutable;
    static MongodProcess mongod;
    static MongoDbAccessReferential mongoDbAccess;
    static String DATABASE_NAME = "vitam-test";
    private static String DATABASE_HOST = "localhost";

    private static final String RESOURCE_URI = "/adminmanagement/v1";
    private static final int TENANT_ID = 0;

    private static final String REINDEX_URI = "/reindex";
    private static final String ALIASES_URI = "/alias";

    private static final String ADMIN_MANAGEMENT_CONF = "functional-administration-test.conf";
    private static final String TEST_ES_MAPPING_JSON = "test-es-mapping.json";
    private static final String TYPEUNIQUE = VitamCollection.getTypeunique();

    private static MongoDbAccessAdminImpl dbImpl;

    private InputStream stream;
    private static JunitHelper junitHelper = JunitHelper.getInstance();
    private static int serverPort;
    private static int databasePort;
    private static File adminConfigFile;

    private static AdminManagementMain application;

    private static int workspacePort = junitHelper.findAvailablePort();
    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @ClassRule
    public static WireMockClassRule wireMockRule = new WireMockClassRule(workspacePort);

    @Rule
    public WireMockClassRule instanceRule = wireMockRule;

    private static ElasticsearchTestConfiguration configEs = null;

    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final static String CLUSTER_NAME = "vitam-cluster";
    private static ElasticsearchAccessFunctionalAdmin esClient;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        new JHades().overlappingJarsReport();

        databasePort = junitHelper.findAvailablePort();

        // ES
        try {
            configEs = JunitHelper.startElasticsearchForTest(temporaryFolder, CLUSTER_NAME);
        } catch (final VitamApplicationServerException e1) {
            assumeTrue(false);
        }

        File tempFolder = temporaryFolder.newFolder();
        System.setProperty("vitam.tmp.folder", tempFolder.getAbsolutePath());

        SystemPropertyUtil.refresh();

        final List<ElasticsearchNode> nodesEs = new ArrayList<>();
        nodesEs.add(new ElasticsearchNode("localhost", configEs.getTcpPort()));
        esClient = new ElasticsearchAccessFunctionalAdmin(CLUSTER_NAME, nodesEs);
        LogbookOperationsClientFactory.changeMode(null);

        final MongodStarter starter = MongodStarter.getDefaultInstance();
        mongodExecutable = starter.prepare(new MongodConfigBuilder()
            .withLaunchArgument("--enableMajorityReadConcern")
            .version(Version.Main.PRODUCTION)
            .net(new Net(databasePort, Network.localhostIsIPv6()))
            .build());
        mongod = mongodExecutable.start();

        final File adminConfig = PropertiesUtils.findFile(ADMIN_MANAGEMENT_CONF);
        final AdminManagementConfiguration realAdminConfig =
            PropertiesUtils.readYaml(adminConfig, AdminManagementConfiguration.class);
        realAdminConfig.getMongoDbNodes().get(0).setDbPort(databasePort);
        realAdminConfig.setElasticsearchNodes(nodesEs);
        realAdminConfig.setClusterName(CLUSTER_NAME);
        realAdminConfig.setWorkspaceUrl("http://localhost:" + workspacePort);

        final List<MongoDbNode> nodes = new ArrayList<>();
        nodes.add(new MongoDbNode(DATABASE_HOST, databasePort));
        mongoDbAccess = MongoDbAccessAdminFactory.create(new DbConfigurationImpl(nodes, "vitam-test"));

        serverPort = junitHelper.findAvailablePort();

        RestAssured.port = serverPort;
        RestAssured.basePath = RESOURCE_URI;

        adminConfigFile = File.createTempFile("test", ADMIN_MANAGEMENT_CONF, adminConfig.getParentFile());
        PropertiesUtils.writeYaml(adminConfigFile, realAdminConfig);

        dbImpl = MongoDbAccessAdminFactory.create(new DbConfigurationImpl(nodes, DATABASE_NAME));

        try {

            application = new AdminManagementMain(adminConfigFile.getAbsolutePath());
            application.start();
            JunitHelper.unsetJettyPortSystemProperty();

        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
            throw new IllegalStateException(
                "Cannot start the Logbook Application Server", e);
        }
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        LOGGER.debug("Ending tests");
        try {
            application.stop();
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
        }

        mongod.stop();
        mongodExecutable.stop();
        junitHelper.releasePort(databasePort);
        junitHelper.releasePort(serverPort);
        VitamClientFactory.resetConnections();
    }

    @Before
    public void setUp() throws Exception {
        instanceRule.stubFor(WireMock.post(urlMatching("/workspace/v1/containers/(.*)"))
            .willReturn(
                aResponse().withStatus(201).withHeader(GlobalDataRest.X_TENANT_ID, Integer.toString(TENANT_ID))));
        instanceRule.stubFor(WireMock.delete(urlMatching("/workspace/v1/containers/(.*)"))
            .willReturn(
                aResponse().withStatus(204).withHeader(GlobalDataRest.X_TENANT_ID, Integer.toString(TENANT_ID))));
    }

    @After
    public void tearDown() throws Exception {
        mongoDbAccess.deleteCollection(FunctionalAdminCollections.FORMATS).close();
        mongoDbAccess.deleteCollection(FunctionalAdminCollections.RULES).close();
    }

    @Test
    @RunWithCustomExecutor
    public void launchReindexationMetadataLogbookTest() throws Exception {

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        stream = PropertiesUtils.getResourceAsStream("reindex_order.json");
        final JsonNode reindexOrder = JsonHandler.getFromInputStream(stream);
        given().contentType(ContentType.JSON).body(reindexOrder)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID).accept(ContentType.JSON)
            .when().post(REINDEX_URI)
            .then()
            .statusCode(Status.CREATED.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void launchReindexationFuncAdminTest() throws Exception {

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        stream = PropertiesUtils.getResourceAsStream("reindex_order_func_adm.json");
        final JsonNode reindexOrder = JsonHandler.getFromInputStream(stream);
        given().contentType(ContentType.JSON).body(reindexOrder)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID).accept(ContentType.JSON)
            .when().post(REINDEX_URI)
            .then()
            .statusCode(Status.CREATED.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void launchReindexationWithUnknownCollectionTest() throws Exception {

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        stream = PropertiesUtils.getResourceAsStream("reindex_order_unknown_collection.json");
        final JsonNode reindexOrder = JsonHandler.getFromInputStream(stream);
        given().contentType(ContentType.JSON).body(reindexOrder)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID).accept(ContentType.JSON)
            .when().post(REINDEX_URI)
            .then()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void launchReindexationOfOneCorrectAndOneIncorrectCollectionTest() throws Exception {

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        stream = PropertiesUtils.getResourceAsStream("reindex_order_one_correct_one_incorrect.json");
        final JsonNode reindexOrder = JsonHandler.getFromInputStream(stream);
        given().contentType(ContentType.JSON).body(reindexOrder)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID).accept(ContentType.JSON)
            .when().post(REINDEX_URI)
            .then()
            .statusCode(Status.ACCEPTED.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void switchIndexesTest() throws Exception {

        String mapping = ElasticsearchUtil
            .transferJsonToMapping(new FileInputStream(PropertiesUtils.findFile(TEST_ES_MAPPING_JSON)));
        esClient.createIndexAndAliasIfAliasNotExists(
            FunctionalAdminCollections.CONTEXT.toString().toLowerCase(), mapping, TYPEUNIQUE, null);
        String newIndex = esClient.createIndexWithoutAlias(
            FunctionalAdminCollections.CONTEXT.toString().toLowerCase(), mapping, TYPEUNIQUE, null);

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        /*
         * stream = PropertiesUtils.getResourceAsStream("switch_indexes.json"); JsonNode switchOrder =
         * JsonHandler.getFromInputStream(stream);
         */

        List<SwitchIndexParameters> listSwitches = new ArrayList<>();
        SwitchIndexParameters switchParams = new SwitchIndexParameters();
        switchParams.setAlias(FunctionalAdminCollections.CONTEXT.toString().toLowerCase());
        switchParams.setIndexName(newIndex);
        listSwitches.add(switchParams);

        given().contentType(ContentType.JSON).body(listSwitches)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID).accept(ContentType.JSON)
            .when().post(ALIASES_URI)
            .then().statusCode(Status.OK.getStatusCode());

        listSwitches = new ArrayList<>();
        switchParams = new SwitchIndexParameters();
        switchParams.setAlias(FunctionalAdminCollections.CONTEXT.toString().toLowerCase());
        switchParams.setIndexName("unknown_index");
        listSwitches.add(switchParams);

        given().contentType(ContentType.JSON).body(listSwitches)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID).accept(ContentType.JSON)
            .when().post(ALIASES_URI)
            .then().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void switchIndexesWithUnknownCollectionTest() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        stream = PropertiesUtils.getResourceAsStream("switch_indexes_unknown_collection.json");
        final JsonNode reindexOrder = JsonHandler.getFromInputStream(stream);
        given().contentType(ContentType.JSON).body(reindexOrder)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID).accept(ContentType.JSON)
            .when().post(ALIASES_URI)
            .then().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

}
