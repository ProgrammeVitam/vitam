package fr.gouv.vitam.functional.administration.rest;

import com.fasterxml.jackson.databind.JsonNode;
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
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.server.AdminManagementConfiguration;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminFactory;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessReferential;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import org.jhades.JHades;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.ws.rs.core.Response;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;
import static org.junit.Assume.assumeTrue;

/**
 * AdminReconstruction tests.
 */
public class AdminReconstructionResourceTest {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AgenciesResourceTest.class);
    private static final String ADMIN_MANAGEMENT_CONF = "functional-administration-test.conf";

    private static final String RESOURCE_URI = "/adminmanagement/v1";
    private static final String STATUS_URI = "/status";

    private static final String RECONSTRUCTION_URI = "/reconstruction";
    private static final String  RECONSTRUCTION_COLLECTION_URI = "/rules";

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

    private static JunitHelper.ElasticsearchTestConfiguration configEs = null;

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    @ClassRule
    public static WireMockClassRule wireMockRule = new WireMockClassRule(workspacePort);

    @Rule
    public WireMockClassRule instanceRule = wireMockRule;


    private final static String CLUSTER_NAME = "vitam-cluster";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        new JHades().overlappingJarsReport();

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

    @Test
    @RunWithCustomExecutor
    public final void testGetStatus() {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        get(STATUS_URI).then().statusCode(Response.Status.NO_CONTENT.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void testCallReconstructionEndpointOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        // create json collectionItems
        JsonNode json = JsonHandler.createArrayNode();

        // call the endpoint
        given().contentType(ContentType.JSON).body(json)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(RECONSTRUCTION_URI)
            .then().statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void testCallReconstrutVitamCollectionEndpointOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        // call the endpoint with rules collection.
        given().contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(RECONSTRUCTION_URI + RECONSTRUCTION_COLLECTION_URI)
            .then().statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    @Ignore("integration tests on the collections and tenants are done on the service level.")
    @RunWithCustomExecutor
    public void testReconstructionOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        // get reconstruction parameters from json file.
        File fileProfiles = PropertiesUtils.getResourceFile("reconstruction_rest_query.json");
        JsonNode json = JsonHandler.getFromFile(fileProfiles);

        // call the endpoint with valid json
        given().contentType(ContentType.JSON).body(json)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(RECONSTRUCTION_URI)
            .then().statusCode(Response.Status.OK.getStatusCode());

    }

}
