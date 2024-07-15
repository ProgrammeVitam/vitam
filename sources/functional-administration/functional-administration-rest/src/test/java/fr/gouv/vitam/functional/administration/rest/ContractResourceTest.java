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
package fr.gouv.vitam.functional.administration.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.google.common.collect.Lists;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.client.VitamClientFactory;
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
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.administration.AgenciesModel;
import fr.gouv.vitam.common.model.administration.ManagementContractModel;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadFactory;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.config.AdminManagementConfiguration;
import fr.gouv.vitam.functional.administration.common.config.ElasticsearchFunctionalAdminIndexManager;
import fr.gouv.vitam.functional.administration.common.server.ElasticsearchAccessFunctionalAdmin;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollectionsTestUtils;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminFactory;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessReferential;
import fr.gouv.vitam.functional.administration.core.agencies.AgenciesService;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.ws.rs.core.Response.Status;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.and;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.match;
import static fr.gouv.vitam.common.guid.GUIDFactory.newOperationLogbookGUID;
import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * As contract Resource call ContractService, the full tests are done in @see AccessContractTest
 */
public class ContractResourceTest {

    private static final String PREFIX = GUIDFactory.newGUID().getId();

    @ClassRule
    public static MongoRule mongoRule = new MongoRule(MongoDbAccess.getMongoClientSettingsBuilder());

    @ClassRule
    public static ElasticsearchRule elasticsearchRule = new ElasticsearchRule();

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ContractResourceTest.class);
    private static final String ADMIN_MANAGEMENT_CONF = "functional-administration-test-contract.conf";

    private static final String RESOURCE_URI = "/adminmanagement/v1";
    private static final String STATUS_URI = "/status";

    private static final int TENANT_ID = 0;

    private static MongoDbAccessReferential mongoDbAccess;
    private static final String DATABASE_HOST = "localhost";
    private static final JunitHelper junitHelper = JunitHelper.getInstance();
    private static int serverPort;
    private static File adminConfigFile;
    private static AdminManagementMain application;
    private static AgenciesService agenciesService;

    private static final int workspacePort = junitHelper.findAvailablePort();

    private static final ElasticsearchFunctionalAdminIndexManager indexManager =
        FunctionalAdminCollectionsTestUtils.createTestIndexManager();

    @ClassRule
    public static WireMockClassRule workspaceWireMock = new WireMockClassRule(workspacePort);

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setUp() {
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(TENANT_ID));
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        List<ElasticsearchNode> esNodes = Lists.newArrayList(
            new ElasticsearchNode(ElasticsearchRule.getHost(), ElasticsearchRule.getPort())
        );

        FunctionalAdminCollectionsTestUtils.beforeTestClass(
            mongoRule.getMongoDatabase(),
            PREFIX,
            new ElasticsearchAccessFunctionalAdmin(ElasticsearchRule.VITAM_CLUSTER, esNodes, indexManager)
        );

        File tmpFolder = tempFolder.newFolder();
        System.setProperty("vitam.tmp.folder", tmpFolder.getAbsolutePath());
        SystemPropertyUtil.refresh();

        // Mock workspace API
        workspaceWireMock.stubFor(
            WireMock.post(urlMatching("/workspace/v1/containers/(.*)")).willReturn(
                aResponse().withStatus(201).withHeader(GlobalDataRest.X_TENANT_ID, Integer.toString(TENANT_ID))
            )
        );
        workspaceWireMock.stubFor(
            WireMock.delete(urlMatching("/workspace/v1/containers/(.*)")).willReturn(
                aResponse().withStatus(204).withHeader(GlobalDataRest.X_TENANT_ID, Integer.toString(TENANT_ID))
            )
        );

        LogbookOperationsClientFactory.changeMode(null);

        final File adminConfig = PropertiesUtils.findFile(ADMIN_MANAGEMENT_CONF);
        final AdminManagementConfiguration realAdminConfig = PropertiesUtils.readYaml(
            adminConfig,
            AdminManagementConfiguration.class
        );
        realAdminConfig.getMongoDbNodes().get(0).setDbPort(MongoRule.getDataBasePort());
        realAdminConfig.setElasticsearchNodes(esNodes);
        realAdminConfig.setClusterName(ElasticsearchRule.VITAM_CLUSTER);
        realAdminConfig.setWorkspaceUrl("http://localhost:" + workspacePort);
        adminConfigFile = File.createTempFile("test", ADMIN_MANAGEMENT_CONF, adminConfig.getParentFile());
        PropertiesUtils.writeYaml(adminConfigFile, realAdminConfig);

        final List<MongoDbNode> nodes = new ArrayList<>();
        nodes.add(new MongoDbNode(DATABASE_HOST, MongoRule.getDataBasePort()));
        mongoDbAccess = MongoDbAccessAdminFactory.create(
            new DbConfigurationImpl(nodes, mongoRule.getMongoDatabase().getName()),
            Collections::emptyList,
            indexManager
        );

        serverPort = junitHelper.findAvailablePort();

        RestAssured.port = serverPort;
        RestAssured.basePath = RESOURCE_URI;

        try {
            application = new AdminManagementMain(adminConfigFile.getAbsolutePath());
            application.start();
            JunitHelper.unsetJettyPortSystemProperty();
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
            throw new IllegalStateException("Cannot start the AdminManagement Application Server", e);
        }

        final File fileAgencies = PropertiesUtils.getResourceFile("agencies.csv");
        final Thread thread = VitamThreadFactory.getInstance()
            .newThread(() -> {
                RequestResponse<AgenciesModel> response = null;
                try {
                    VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

                    response = agenciesService.importAgencies(new FileInputStream(fileAgencies), "test.json");

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

        FunctionalAdminCollectionsTestUtils.afterTestClass(true);

        junitHelper.releasePort(serverPort);
        VitamClientFactory.resetConnections();
    }

    @After
    public void tearDown() throws Exception {
        FunctionalAdminCollectionsTestUtils.afterTest(Arrays.asList(FunctionalAdminCollections.ACCESS_CONTRACT));
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
        given()
            .contentType(ContentType.JSON)
            .body(json)
            .header(GlobalDataRest.X_TENANT_ID, 0)
            .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
            .when()
            .post(ContractResource.INGEST_CONTRACTS_URI)
            .then()
            .statusCode(Status.CREATED.getStatusCode());

        final fr.gouv.vitam.common.database.builder.request.single.Select select =
            new fr.gouv.vitam.common.database.builder.request.single.Select();
        final BooleanQuery query = and();
        query.add(match("Name", "aUniqueName"));
        select.setQuery(query);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        List<String> result = given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, 0)
            .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
            .when()
            .get(ContractResource.INGEST_CONTRACTS_URI)
            .then()
            .statusCode(Status.OK.getStatusCode())
            .extract()
            .body()
            .jsonPath()
            .get("$results.Name");

        assertThat(result).hasSize(1).contains("aUniqueName");
    }

    @Test
    @RunWithCustomExecutor
    public void givenIngestContractJsonWithAmissingNameReturnBadRequest() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_missingName.json");
        JsonNode json = JsonHandler.getFromFile(fileContracts);
        // transform to json
        given()
            .contentType(ContentType.JSON)
            .body(json)
            .header(GlobalDataRest.X_TENANT_ID, 0)
            .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
            .when()
            .post(ContractResource.INGEST_CONTRACTS_URI)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenIngestContractsWithDuplicateNames() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_duplicate.json");
        JsonNode json = JsonHandler.getFromFile(fileContracts);
        // transform to json
        given()
            .contentType(ContentType.JSON)
            .body(json)
            .header(GlobalDataRest.X_TENANT_ID, 0)
            .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
            .when()
            .post(ContractResource.INGEST_CONTRACTS_URI)
            .then()
            .statusCode(Status.CREATED.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenAccessContractsTestWellFormedContractThenImportSuccessfully() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        File fileContracts = PropertiesUtils.getResourceFile("contracts_access_ok.json");
        JsonNode json = JsonHandler.getFromFile(fileContracts);
        // transform to json
        given()
            .contentType(ContentType.JSON)
            .body(json)
            .header(GlobalDataRest.X_TENANT_ID, 0)
            .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
            .when()
            .post(ContractResource.ACCESS_CONTRACTS_URI)
            .then()
            .statusCode(Status.CREATED.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenAccessContractsTestWithInvalidRuleCategoryThenImportKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        File fileContracts = PropertiesUtils.getResourceFile("contracts_access_ko_bad_rule_category.json");
        JsonNode json = JsonHandler.getFromFile(fileContracts);
        // transform to json
        given()
            .contentType(ContentType.JSON)
            .body(json)
            .header(GlobalDataRest.X_TENANT_ID, 0)
            .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
            .when()
            .post(ContractResource.ACCESS_CONTRACTS_URI)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenAccessContractTestUpdate() throws Exception {
        createAccessContract();
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        // Test update for access contract Status => inactive
        String now = LocalDateUtil.nowFormatted();
        final UpdateParserSingle updateParser = new UpdateParserSingle(new SingleVarNameAdapter());
        SetAction setActionStatusInactive;
        setActionStatusInactive = UpdateActionHelper.set("Status", "INACTIVE");
        SetAction setActionDesactivationDateInactive = UpdateActionHelper.set("DeactivationDate", now);
        SetAction setActionLastUpdateInactive = UpdateActionHelper.set("LastUpdate", now);
        SetAction setActionRuleCategory = UpdateActionHelper.set(
            AccessContractModel.RULE_CATEGORY_TO_FILTER,
            "DisseminationRule"
        );
        Update update = new Update();
        update.setQuery(QueryHelper.eq("Name", "aName"));
        update.addActions(
            setActionStatusInactive,
            setActionDesactivationDateInactive,
            setActionLastUpdateInactive,
            setActionRuleCategory
        );
        updateParser.parse(update.getFinalUpdate());
        JsonNode queryDslForUpdate = updateParser.getRequest().getFinalUpdate();

        List<String> ids = selectContractByName("aName", ContractResource.ACCESS_CONTRACTS_URI);

        given()
            .contentType(ContentType.JSON)
            .body(queryDslForUpdate)
            .header(GlobalDataRest.X_TENANT_ID, 0)
            .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
            .when()
            .put(ContractResource.UPDATE_ACCESS_CONTRACTS_URI + "/" + ids.get(0))
            .then()
            .statusCode(Status.OK.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(queryDslForUpdate)
            .header(GlobalDataRest.X_TENANT_ID, 0)
            .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
            .when()
            .put(ContractResource.UPDATE_ACCESS_CONTRACTS_URI + "/wrongId")
            .then()
            .statusCode(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenAccessContractTestUpdateWithInvalidRuleCategoryThenKO() throws Exception {
        createAccessContract();
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        // Test update for access contract Status => inactive
        String now = LocalDateUtil.nowFormatted();
        final UpdateParserSingle updateParser = new UpdateParserSingle(new SingleVarNameAdapter());
        SetAction setActionStatusInactive = UpdateActionHelper.set(AccessContractModel.RULE_CATEGORY_TO_FILTER, "Bad");
        Update update = new Update();
        update.setQuery(QueryHelper.eq("Name", "aName"));
        update.addActions(setActionStatusInactive);
        updateParser.parse(update.getFinalUpdate());
        JsonNode queryDslForUpdate = updateParser.getRequest().getFinalUpdate();

        List<String> ids = selectContractByName("aName", ContractResource.ACCESS_CONTRACTS_URI);

        given()
            .contentType(ContentType.JSON)
            .body(queryDslForUpdate)
            .header(GlobalDataRest.X_TENANT_ID, 0)
            .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
            .when()
            .put(ContractResource.UPDATE_ACCESS_CONTRACTS_URI + "/" + ids.get(0))
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenIngestContractTestUpdate() throws Exception {
        createIngestContract();
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        // Test update for access contract Status => inactive
        String now = LocalDateUtil.nowFormatted();
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
        } catch (InvalidCreateOperationException | InvalidParseOperationException e) {}

        List<String> ids = selectContractByName("aName", ContractResource.INGEST_CONTRACTS_URI);

        JsonNode queryDslForUpdate = updateParser.getRequest().getFinalUpdate();
        given()
            .contentType(ContentType.JSON)
            .body(queryDslForUpdate)
            .header(GlobalDataRest.X_TENANT_ID, 0)
            .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
            .when()
            .put(ContractResource.UPDATE_INGEST_CONTRACTS_URI + "/" + ids.get(0))
            .then()
            .statusCode(Status.OK.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(queryDslForUpdate)
            .header(GlobalDataRest.X_TENANT_ID, 0)
            .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
            .when()
            .put(ContractResource.UPDATE_INGEST_CONTRACTS_URI + "/wrongId")
            .then()
            .statusCode(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenAccessContractsTestFindByName() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        File fileContracts = PropertiesUtils.getResourceFile("contracts_access_ok.json");

        JsonNode json = JsonHandler.getFromFile(fileContracts);
        // first succefull create
        JsonPath body = given()
            .contentType(ContentType.JSON)
            .body(json)
            .header(GlobalDataRest.X_TENANT_ID, 0)
            .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
            .when()
            .post(ContractResource.ACCESS_CONTRACTS_URI)
            .then()
            .statusCode(Status.CREATED.getStatusCode())
            .extract()
            .body()
            .jsonPath();

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
        body = given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, 0)
            .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
            .body(queryDsl)
            .when()
            .get(ContractResource.ACCESS_CONTRACTS_URI)
            .then()
            .statusCode(Status.OK.getStatusCode())
            .extract()
            .body()
            .jsonPath();

        names = body.get("$results.Identifier");
        assertThat(names).hasSize(1);

        // We juste test the first contract
        assertThat(names).contains(name);
    }

    @Test
    @RunWithCustomExecutor
    public void givenManagementContractsTestWellFormedContractThenImportSuccessfully() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        File fileContracts = PropertiesUtils.getResourceFile("contracts_management_ok.json");
        JsonNode json = JsonHandler.getFromFile(fileContracts);

        RequestResponseOK<ManagementContractModel> body = given()
            .contentType(ContentType.JSON)
            .body(json)
            .header(GlobalDataRest.X_TENANT_ID, 0)
            .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
            .when()
            .post(ContractResource.MANAGEMENT_CONTRACTS_URI)
            .then()
            .statusCode(Status.CREATED.getStatusCode())
            .extract()
            .body()
            .as(new TypeRef<RequestResponseOK<ManagementContractModel>>() {});
        assertThat(body.getResults().size()).isEqualTo(5);
        assertThat(body.getResults().get(0).getIdentifier()).isNotNull().isNotEmpty().startsWith("MC-");
        assertThat(body.getResults().get(0).getId()).isNotNull().isNotEmpty();
    }

    @Test
    @RunWithCustomExecutor
    public void givenManagementContractJsonWithMissingNamesReturnBadRequest() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        File fileContracts = PropertiesUtils.getResourceFile("contracts_management_missingNames.json");
        JsonNode json = JsonHandler.getFromFile(fileContracts);
        // transform to json
        given()
            .contentType(ContentType.JSON)
            .body(json)
            .header(GlobalDataRest.X_TENANT_ID, 0)
            .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
            .when()
            .post(ContractResource.MANAGEMENT_CONTRACTS_URI)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenManagementContractTestUpdate() throws Exception {
        List<String> createdIdentifiers = createManagementContracts();
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        // Test update for access contract Status => inactive
        String now = LocalDateUtil.nowFormatted();
        final UpdateParserSingle updateParser = new UpdateParserSingle(new SingleVarNameAdapter());

        assertThatCode(() -> {
            SetAction setActionStatusInactive = UpdateActionHelper.set("Status", "INACTIVE");
            SetAction setActionDesactivationDateInactive = UpdateActionHelper.set("DeactivationDate", now);
            SetAction setActionLastUpdateInactive = UpdateActionHelper.set("LastUpdate", now);
            Update update = new Update();
            update.setQuery(QueryHelper.eq("Identifier", createdIdentifiers.get(0)));
            update.addActions(setActionStatusInactive, setActionDesactivationDateInactive, setActionLastUpdateInactive);
            updateParser.parse(update.getFinalUpdate());
        }).doesNotThrowAnyException();

        JsonNode queryDslForUpdate = updateParser.getRequest().getFinalUpdate();

        given()
            .contentType(ContentType.JSON)
            .body(queryDslForUpdate)
            .header(GlobalDataRest.X_TENANT_ID, 0)
            .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
            .when()
            .put(ContractResource.UPDATE_MANAGEMENT_CONTRACTS_URI + "/" + createdIdentifiers.get(0))
            .then()
            .statusCode(Status.OK.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(queryDslForUpdate)
            .header(GlobalDataRest.X_TENANT_ID, 0)
            .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
            .when()
            .put(ContractResource.UPDATE_MANAGEMENT_CONTRACTS_URI + "/wrongId")
            .then()
            .statusCode(Status.NOT_FOUND.getStatusCode());

        assertThatCode(() -> {
            SetAction setActionChangeIdentifier = UpdateActionHelper.set("Identifier", "FAKE");
            Update update = new Update();
            update.setQuery(QueryHelper.eq("Identifier", createdIdentifiers.get(1)));
            update.addActions(setActionChangeIdentifier);
            updateParser.parse(update.getFinalUpdate());
        }).doesNotThrowAnyException();

        given()
            .contentType(ContentType.JSON)
            .body(queryDslForUpdate)
            .header(GlobalDataRest.X_TENANT_ID, 0)
            .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
            .when()
            .put(ContractResource.UPDATE_MANAGEMENT_CONTRACTS_URI + "/" + createdIdentifiers.get(1))
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenManagementContractsTestFind() throws Exception {
        List<String> createdIdentifiers = createManagementContracts();
        final SelectParserSingle parser = new SelectParserSingle(new SingleVarNameAdapter());
        Select select = new Select();
        parser.parse(select.getFinalSelect());
        parser.addCondition(QueryHelper.eq("Identifier", createdIdentifiers.get(0)));
        JsonNode queryDsl = parser.getRequest().getFinalSelect();

        JsonPath body = given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, 0)
            .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
            .body(queryDsl)
            .when()
            .get(ContractResource.MANAGEMENT_CONTRACTS_URI)
            .then()
            .statusCode(Status.OK.getStatusCode())
            .extract()
            .body()
            .jsonPath();

        List<String> identifiers = body.get("$results.Identifier");
        assertThat(identifiers).hasSize(1);
        assertThat(identifiers.get(0)).isEqualTo(createdIdentifiers.get(0));
    }

    private void createAccessContract() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        File fileContracts = PropertiesUtils.getResourceFile("contracts_access_ok.json");
        JsonNode json = JsonHandler.getFromFile(fileContracts);
        // transform to json
        given()
            .contentType(ContentType.JSON)
            .body(json)
            .header(GlobalDataRest.X_TENANT_ID, 0)
            .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
            .when()
            .post(ContractResource.ACCESS_CONTRACTS_URI)
            .then()
            .statusCode(Status.CREATED.getStatusCode());
    }

    private void createIngestContract() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_ok.json");
        JsonNode json = JsonHandler.getFromFile(fileContracts);
        // transform to json
        given()
            .contentType(ContentType.JSON)
            .body(json)
            .header(GlobalDataRest.X_TENANT_ID, 0)
            .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
            .when()
            .post(ContractResource.INGEST_CONTRACTS_URI)
            .then()
            .statusCode(Status.CREATED.getStatusCode());
    }

    private List<String> createManagementContracts() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        File fileContracts = PropertiesUtils.getResourceFile("contracts_management_ok.json");
        JsonNode json = JsonHandler.getFromFile(fileContracts);
        // transform to json
        JsonPath body = given()
            .contentType(ContentType.JSON)
            .body(json)
            .header(GlobalDataRest.X_TENANT_ID, 0)
            .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
            .when()
            .post(ContractResource.MANAGEMENT_CONTRACTS_URI)
            .then()
            .statusCode(Status.CREATED.getStatusCode())
            .extract()
            .body()
            .jsonPath();

        List<String> ids = body.get("$results.Identifier");
        return ids;
    }

    private List<String> selectContractByName(String name, String resource) throws Exception {
        final SelectParserSingle parser = new SelectParserSingle(new SingleVarNameAdapter());
        Select select = new Select();
        parser.parse(select.getFinalSelect());
        parser.addCondition(QueryHelper.eq("Name", name));
        JsonNode queryDsl = parser.getRequest().getFinalSelect();

        // find xxxxContract with the id1 should return Status.OK
        JsonPath body = given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, 0)
            .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
            .body(queryDsl)
            .when()
            .get(resource)
            .then()
            .statusCode(Status.OK.getStatusCode())
            .extract()
            .body()
            .jsonPath();

        List<String> ids = body.get("$results.Identifier");
        return ids;
    }
}
