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

import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.and;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.match;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;
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
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.builder.query.BooleanQuery;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.action.SetAction;
import fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.builder.request.single.Update;
import fr.gouv.vitam.common.database.parser.request.adapter.SingleVarNameAdapter;
import fr.gouv.vitam.common.database.parser.request.single.SelectParserSingle;
import fr.gouv.vitam.common.database.parser.request.single.UpdateParserSingle;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.junit.JunitHelper.ElasticsearchTestConfiguration;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.administration.AgenciesModel;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadFactory;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.agencies.api.AgenciesService;
import fr.gouv.vitam.functional.administration.common.server.AdminManagementConfiguration;
import fr.gouv.vitam.functional.administration.common.server.ElasticsearchAccessFunctionalAdmin;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminFactory;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessReferential;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import org.jhades.JHades;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;


/**
 * As contract Resource call ContractService, the full tests are done in @see AccessContractTest
 */
public class ContractResourceTest {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ContractResourceTest.class);
    private static final String ADMIN_MANAGEMENT_CONF = "functional-administration-test-contract.conf";
    private static final String RESULTS = "$results";

    private static final String RESOURCE_URI = "/adminmanagement/v1";
    private static final String STATUS_URI = "/status";
    private static final String UPDATE_ACCESS_CONTRACT_URI = "/accesscontracts";


    private static final int TENANT_ID = 0;

    static MongodExecutable mongodExecutable;
    static MongodProcess mongod;
    static MongoDbAccessReferential mongoDbAccess;
    static String DATABASE_NAME = "vitam-test";
    private static String DATABASE_HOST = "localhost";

    private InputStream stream;
    private static JunitHelper junitHelper;
    private static int serverPort;
    private static int databasePort;
    private static File adminConfigFile;
    private static AdminManagementMain application;
    static AgenciesService agenciesService;


    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    private static ElasticsearchTestConfiguration configEs = null;

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    private final static String CLUSTER_NAME = "vitam-cluster";
    private static ElasticsearchAccessFunctionalAdmin esClient;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        new JHades().overlappingJarsReport();

        junitHelper = JunitHelper.getInstance();
        databasePort = junitHelper.findAvailablePort();

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

        final File fileAgencies = PropertiesUtils.getResourceFile("agencies.csv");
        final Thread thread = VitamThreadFactory.getInstance().newThread(() -> {
            RequestResponse<AgenciesModel> response = null;
            try {
                VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);


                response = agenciesService.importAgencies(new FileInputStream(fileAgencies));

                assertThat(response.isOk()).isTrue();
            } catch (VitamException | IOException e) {
                throw new RuntimeException(e);
            }
        });

        thread.start();
        thread.join();
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

    @After
    public void tearDown() throws Exception {
        mongoDbAccess.deleteCollection(FunctionalAdminCollections.ACCESS_CONTRACT).close();
    }

    @Test
    @RunWithCustomExecutor
    public final void testGetStatus() {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        get(STATUS_URI).then().statusCode(Status.NO_CONTENT.getStatusCode());
    }



    @Test
    @RunWithCustomExecutor
    public void givenAWellFormedIngestContractJsonThenReturnCeated() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_ok_unique.json");
        JsonNode json = JsonHandler.getFromFile(fileContracts);

        MetaDataClientFactory.changeMode(null);

        // transform to json
        given().contentType(ContentType.JSON).body(json)
            .header(GlobalDataRest.X_TENANT_ID, 0)
            .when().post(ContractResource.INGEST_CONTRACTS_URI)
            .then().statusCode(Status.CREATED.getStatusCode());

        final fr.gouv.vitam.common.database.builder.request.single.Select select =
            new fr.gouv.vitam.common.database.builder.request.single.Select();
        final BooleanQuery query = and();
        query.add(match("Name", "aUniqueName"));
        select.setQuery(query);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        List<String> result = given().contentType(ContentType.JSON).body(select.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, 0)
            .when().get(ContractResource.INGEST_CONTRACTS_URI)
            .then().statusCode(Status.OK.getStatusCode()).extract().body().jsonPath().get("$results.Name");

        assertThat(result).hasSize(1).contains("aUniqueName");
    }

    @Test
    @RunWithCustomExecutor
    public void givenIngestContractJsonWithAmissingNameReturnBadRequest() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_missingName.json");
        JsonNode json = JsonHandler.getFromFile(fileContracts);
        // transform to json
        given().contentType(ContentType.JSON).body(json)
            .header(GlobalDataRest.X_TENANT_ID, 0)
            .when().post(ContractResource.INGEST_CONTRACTS_URI)
            .then().statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenIngestContractsWithDuplicateNames() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_duplicate.json");
        JsonNode json = JsonHandler.getFromFile(fileContracts);
        // transform to json
        given().contentType(ContentType.JSON).body(json)
            .header(GlobalDataRest.X_TENANT_ID, 0)
            .when().post(ContractResource.INGEST_CONTRACTS_URI)
            .then().statusCode(Status.CREATED.getStatusCode());
    }


    @Test
    @RunWithCustomExecutor
    public void givenAccessContractsTestWellFormedContractThenImportSuccessfully() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        File fileContracts = PropertiesUtils.getResourceFile("contracts_access_ok.json");
        JsonNode json = JsonHandler.getFromFile(fileContracts);
        // transform to json
        given().contentType(ContentType.JSON).body(json)
            .header(GlobalDataRest.X_TENANT_ID, 0)
            .when().post(ContractResource.ACCESS_CONTRACTS_URI)
            .then().statusCode(Status.CREATED.getStatusCode());
    }

    private void createAccessContract() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        File fileContracts = PropertiesUtils.getResourceFile("contracts_access_ok.json");
        JsonNode json = JsonHandler.getFromFile(fileContracts);
        // transform to json
        given().contentType(ContentType.JSON).body(json)
            .header(GlobalDataRest.X_TENANT_ID, 0)
            .when().post(ContractResource.ACCESS_CONTRACTS_URI)
            .then().statusCode(Status.CREATED.getStatusCode());
    }

    private void createIngestContract() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_ok.json");
        JsonNode json = JsonHandler.getFromFile(fileContracts);
        // transform to json
        given().contentType(ContentType.JSON).body(json)
            .header(GlobalDataRest.X_TENANT_ID, 0)
            .when().post(ContractResource.INGEST_CONTRACTS_URI)
            .then().statusCode(Status.CREATED.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenAccessContractTestUpdate() throws Exception {
        createAccessContract();
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        // Test update for access contract Status => inactive
        String now = LocalDateUtil.now().toString();
        final UpdateParserSingle updateParser = new UpdateParserSingle(new SingleVarNameAdapter());
        SetAction setActionStatusInactive;
        try {
            setActionStatusInactive = UpdateActionHelper.set("Status", "INACTIVE");
            SetAction setActionDesactivationDateInactive = UpdateActionHelper.set("DeactivationDate", now);
            SetAction setActionLastUpdateInactive = UpdateActionHelper.set("LastUpdate", now);
            Update update = new Update();
            update.setQuery(QueryHelper.eq("Name", "aName"));
            update.addActions(setActionStatusInactive, setActionDesactivationDateInactive, setActionLastUpdateInactive);
            updateParser.parse(update.getFinalUpdate());
        } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
        }
        JsonNode queryDslForUpdate = updateParser.getRequest().getFinalUpdate();

        List<String> ids = selectContractByName("aName", ContractResource.ACCESS_CONTRACTS_URI);

        given().contentType(ContentType.JSON).body(queryDslForUpdate).header(GlobalDataRest.X_TENANT_ID, 0)
            .when().put(ContractResource.UPDATE_ACCESS_CONTRACT_URI + "/" + ids.get(0)).then()
            .statusCode(Status.OK.getStatusCode());
        
        given().contentType(ContentType.JSON).body(queryDslForUpdate).header(GlobalDataRest.X_TENANT_ID, 0)
        .when().put(ContractResource.UPDATE_ACCESS_CONTRACT_URI + "/wrongId").then()
        .statusCode(Status.NOT_FOUND.getStatusCode());
    }

    private List<String> selectContractByName(String name, String resource) throws Exception {
        final SelectParserSingle parser = new SelectParserSingle(new SingleVarNameAdapter());
        Select select = new Select();
        parser.parse(select.getFinalSelect());
        parser.addCondition(QueryHelper.eq("Name", name));
        JsonNode queryDsl = parser.getRequest().getFinalSelect();


        // find accessContract with the id1 should return Status.OK
        JsonPath body = given().contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, 0)
            .body(queryDsl)
            .when()
            .get(resource)
            .then().statusCode(Status.OK.getStatusCode()).extract().body().jsonPath();

        List<String> ids = body.get("$results.Identifier");
        return ids;
    }



    @Test
    @RunWithCustomExecutor
    public void givenIngestContractTestUpdate() throws Exception {
        createIngestContract();
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        // Test update for access contract Status => inactive
        String now = LocalDateUtil.now().toString();
        final UpdateParserSingle updateParser = new UpdateParserSingle(new SingleVarNameAdapter());
        SetAction setActionStatusInactive;
        try {
            setActionStatusInactive = UpdateActionHelper.set("Status", "INACTIVE");
            SetAction setActionDesactivationDateInactive = UpdateActionHelper.set("DeactivationDate", now);
            SetAction setActionLastUpdateInactive = UpdateActionHelper.set("LastUpdate", now);
            Update update = new Update();
            update.setQuery(QueryHelper.eq("Name", "aName"));
            update.addActions(setActionStatusInactive, setActionDesactivationDateInactive, setActionLastUpdateInactive);
            updateParser.parse(update.getFinalUpdate());



        } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
        }

        List<String> ids = selectContractByName("aName", ContractResource.INGEST_CONTRACTS_URI);

        JsonNode queryDslForUpdate = updateParser.getRequest().getFinalUpdate();
        given().contentType(ContentType.JSON).body(queryDslForUpdate).header(GlobalDataRest.X_TENANT_ID, 0)
            .when().put(ContractResource.UPDATE_INGEST_CONTRACTS_URI + "/" + ids.get(0)).then()
            .statusCode(Status.OK.getStatusCode());
        
        given().contentType(ContentType.JSON).body(queryDslForUpdate).header(GlobalDataRest.X_TENANT_ID, 0)
        .when().put(ContractResource.UPDATE_INGEST_CONTRACTS_URI + "/wrongId").then()
        .statusCode(Status.NOT_FOUND.getStatusCode());
        
    }



    @Test
    @RunWithCustomExecutor
    public void givenAccessContractsTestFindByName() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        File fileContracts = PropertiesUtils.getResourceFile("contracts_access_ok.json");

        JsonNode json = JsonHandler.getFromFile(fileContracts);
        // first succefull create
        JsonPath body = given().contentType(ContentType.JSON).body(json)
            .header(GlobalDataRest.X_TENANT_ID, 0)
            .when().post(ContractResource.ACCESS_CONTRACTS_URI)
            .then().statusCode(Status.CREATED.getStatusCode()).extract().body().jsonPath();

        List<String> names = body.get("$results.Identifier");

        assertThat(names).isNotEmpty();

        // We juste test the first contract
        String name = names.get(0);
        assertThat(name).isNotNull();

        final SelectParserSingle parser = new SelectParserSingle(new SingleVarNameAdapter());
        Select select = new Select();
        parser.parse(select.getFinalSelect());
        parser.addCondition(QueryHelper.eq("Identifier", name));
        JsonNode queryDsl = parser.getRequest().getFinalSelect();


        // find accessContract with the id1 should return Status.OK
        body = given().contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, 0)
            .body(queryDsl)
            .when()
            .get(ContractResource.ACCESS_CONTRACTS_URI)
            .then().statusCode(Status.OK.getStatusCode()).extract().body().jsonPath();

        names = body.get("$results.Identifier");
        assertThat(names).hasSize(1);

        // We juste test the first contract
        assertThat(names).contains(name);

    }
}
