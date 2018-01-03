package fr.gouv.vitam.functional.administration.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.path.json.JsonPath;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.parser.request.adapter.SingleVarNameAdapter;
import fr.gouv.vitam.common.database.parser.request.single.SelectParserSingle;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
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
import fr.gouv.vitam.functional.administration.common.Agencies;
import fr.gouv.vitam.functional.administration.common.server.AdminManagementConfiguration;
import fr.gouv.vitam.functional.administration.common.server.ElasticsearchAccessFunctionalAdmin;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminFactory;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessReferential;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookDocument;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import org.jhades.JHades;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;

/**
 * As agencies Resource call AgencyService
 */
public class AgenciesResourceTest {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AgenciesResourceTest.class);
    private static final String ADMIN_MANAGEMENT_CONF = "functional-administration-test.conf";
    private static final String RESULTS = "$results";

    private static final String RESOURCE_URI = "/adminmanagement/v1";
    private static final String STATUS_URI = "/status";

    private static final int TENANT_ID = 0;

    static MongodExecutable mongodExecutable;
    static MongodProcess mongod;
    static MongoDbAccessReferential mongoDbAccess;
    static String DATABASE_NAME = "vitam-test";
    private static String DATABASE_HOST = "localhost";

    private static JunitHelper junitHelper = JunitHelper.getInstance();
    private static int serverPort;
    private static int databasePort;
    private static File adminConfigFile;
    private static AdminManagementMain application;


    private static int workspacePort = junitHelper.findAvailablePort();

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    private static ElasticsearchTestConfiguration configEs = null;

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    @ClassRule
    public static WireMockClassRule wireMockRule = new WireMockClassRule(workspacePort);

    @Rule
    public WireMockClassRule instanceRule = wireMockRule;


    private final static String CLUSTER_NAME = "vitam-cluster";
    private static ElasticsearchAccessFunctionalAdmin esClient;
    private String AGENCIES_URI = "/agencies/import";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        new JHades().overlappingJarsReport();

        File tmpFolder = tempFolder.newFolder();
        System.setProperty("vitam.tmp.folder", tmpFolder.getAbsolutePath());

        junitHelper = JunitHelper.getInstance();
        databasePort = junitHelper.findAvailablePort();
        databasePort = 41909;

        // ES
        try {
            configEs = JunitHelper.startElasticsearchForTest(tempFolder, CLUSTER_NAME);
        } catch (final VitamApplicationServerException e1) {
            assumeTrue(false);

        }

        final List<ElasticsearchNode> nodesEs = new ArrayList<>();
        nodesEs.add(new ElasticsearchNode("localhost", configEs.getTcpPort()));
        esClient = new ElasticsearchAccessFunctionalAdmin(CLUSTER_NAME, nodesEs);
        LogbookOperationsClientFactory.changeMode(null);


        final File adminConfig = PropertiesUtils.findFile(ADMIN_MANAGEMENT_CONF);
        final AdminManagementConfiguration realAdminConfig =
            PropertiesUtils.readYaml(adminConfig, AdminManagementConfiguration.class);
        realAdminConfig.getMongoDbNodes().get(0).setDbPort(databasePort);
        realAdminConfig.setElasticsearchNodes(nodesEs);
        realAdminConfig.setClusterName(CLUSTER_NAME);

        realAdminConfig.setWorkspaceUrl("http://localhost:" + workspacePort);

        adminConfigFile = File.createTempFile("test", ADMIN_MANAGEMENT_CONF, adminConfig.getParentFile());
        PropertiesUtils.writeYaml(adminConfigFile, realAdminConfig);

        final MongodStarter starter = MongodStarter.getDefaultInstance();
        mongodExecutable = starter.prepare(new MongodConfigBuilder()
            .withLaunchArgument("--enableMajorityReadConcern")
            .version(Version.Main.PRODUCTION)
            .net(new Net(databasePort, Network.localhostIsIPv6()))
            .build());
        mongod = mongodExecutable.start();

        final List<MongoDbNode> nodes = new ArrayList<>();
        nodes.add(new MongoDbNode(DATABASE_HOST, databasePort));
        mongoDbAccess = MongoDbAccessAdminFactory.create(new DbConfigurationImpl(nodes, DATABASE_NAME));

        serverPort = junitHelper.findAvailablePort();

        RestAssured.port = serverPort;
        RestAssured.basePath = RESOURCE_URI;

        try {
            application = new AdminManagementMain(adminConfigFile.getAbsolutePath());
            application.start();
            JunitHelper.unsetJettyPortSystemProperty();
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
            throw new IllegalStateException(
                "Cannot start the AdminManagement Application Server", e);
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
        if (null != configEs) {
            junitHelper.releasePort(configEs.getHttpPort());
            junitHelper.releasePort(configEs.getTcpPort());
        }
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
        mongoDbAccess.deleteCollection(FunctionalAdminCollections.AGENCIES).close();
    }

    @Test
    @RunWithCustomExecutor
    public final void testGetStatus() {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        get(STATUS_URI).then().statusCode(Status.NO_CONTENT.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenAWellFormedAgenciesThenReturnCeated() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        File fileAgencies = PropertiesUtils.getResourceFile("agencies.csv");
        MetaDataClientFactory.changeMode(null);

        given().contentType(ContentType.BINARY).body(new FileInputStream(fileAgencies))
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(AGENCIES_URI)
            .then().statusCode(Status.CREATED.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void testDuplicatedAgencies() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        File fileAgencies = PropertiesUtils.getResourceFile("agencies_duplicate.csv");

        MetaDataClientFactory.changeMode(null);

        given().contentType(ContentType.BINARY).body(new FileInputStream(fileAgencies))
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(AGENCIES_URI)
            .then().statusCode(Status.BAD_REQUEST.getStatusCode());
    }



    @Test
    @RunWithCustomExecutor
    public void givenAgenciesTest() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        File fileAgencies = PropertiesUtils.getResourceFile("agencies.csv");
        // first succefull create
        given().contentType(ContentType.BINARY).body(new FileInputStream(fileAgencies))
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(AGENCIES_URI)
            .then().statusCode(Status.CREATED.getStatusCode());


        String name = "AG-000003";
        Select select = new Select();
        select.setQuery(QueryHelper.eq(Agencies.IDENTIFIER, name));

        JsonPath body = given().contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, 0)
            .body(select.getFinalSelect())
            .when()
            .get(AgenciesResource.AGENCIES)
            .then().statusCode(Status.OK.getStatusCode()).extract().body().jsonPath();
        List<String> names = body.get("$results.Identifier");

        assertThat(names).hasSize(1);

        name = "agency";
        select = new Select();
        select.setQuery(QueryHelper.eq("Name", name));
        body = given().contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, 0)
            .body(select.getFinalSelect())
            .when()
            .post(AgenciesResource.AGENCIES)
            .then().statusCode(Status.OK.getStatusCode()).extract().body().jsonPath();
        names = body.get("$results.Identifier");
        assertThat(names.size()).isGreaterThan(1);

        fileAgencies = PropertiesUtils.getResourceFile("agencies_remove.csv");
        given().contentType(ContentType.BINARY).body(new FileInputStream(fileAgencies))
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(AGENCIES_URI)
            .then().statusCode(Status.BAD_REQUEST.getStatusCode());
    }

}
