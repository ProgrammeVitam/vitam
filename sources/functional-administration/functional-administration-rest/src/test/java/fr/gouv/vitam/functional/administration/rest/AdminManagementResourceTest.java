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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.administration.AccessionRegisterDetailModel;
import fr.gouv.vitam.common.model.administration.ActivationStatus;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.config.AdminManagementConfiguration;
import fr.gouv.vitam.functional.administration.common.config.ElasticsearchFunctionalAdminIndexManager;
import fr.gouv.vitam.functional.administration.common.server.ElasticsearchAccessFunctionalAdmin;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollectionsTestUtils;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminFactory;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessReferential;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static fr.gouv.vitam.common.guid.GUIDFactory.newOperationLogbookGUID;
import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.with;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;

public class AdminManagementResourceTest {

    private static final String PREFIX = GUIDFactory.newGUID().getId();

    @ClassRule
    public static MongoRule mongoRule = new MongoRule(MongoDbAccess.getMongoClientSettingsBuilder());

    @ClassRule
    public static ElasticsearchRule elasticsearchRule = new ElasticsearchRule();

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AdminManagementResourceTest.class);
    private static final String ADMIN_MANAGEMENT_CONF = "functional-administration-test.conf";
    private static final String RESULTS = "$results";

    private static final String RESOURCE_URI = "/adminmanagement/v1";
    private static final String STATUS_URI = "/status";
    private static final String CHECK_FORMAT_URI = "/format/check";
    private static final String IMPORT_FORMAT_URI = "/format/import";

    private static final String GET_BYID_FORMAT_URI = "/format";
    private static final String FORMAT_ID_URI = "/{id_format}";

    private static final String GET_DOCUMENT_FORMAT_URI = "/format/document";

    private static final String CHECK_RULES_URI = "/rules/check";
    private static final String IMPORT_RULES_URI = "/rules/import";

    private static final String GET_BYID_RULES_URI = "/rules";
    private static final String RULES_ID_URI = "/{id_rule}";

    private static final String GET_DOCUMENT_RULES_URI = "/rules/document";

    private static final String CREATE_FUND_REGISTER_URI = "/accession-register";

    private static final String CREATE_EXTERNAL_LOGBOOK_URI = "/logbookoperations";

    private static final String FILE_TEST_OK = "jeu_donnees_OK_regles_CSV.csv";

    private static final int TENANT_ID = 0;
    private static final String ERROR_REPORT_CONTENT = "error_report_content.json";
    public static final int TENANT_ID1 = 1;
    private static final String PRONOM_FILE = "DROID_SignatureFile_V94.xml";
    static MongoDbAccessReferential mongoDbAccess;
    private static String DATABASE_HOST = "localhost";

    private InputStream stream;
    private static JunitHelper junitHelper = JunitHelper.getInstance();
    private static int serverPort;
    private static File adminConfigFile;

    private static AdminManagementMain application;

    private static int workspacePort = junitHelper.findAvailablePort();

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @ClassRule
    public static WireMockClassRule wireMockRule = new WireMockClassRule(workspacePort);

    @Rule
    public WireMockClassRule instanceRule = wireMockRule;

    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static final String originatingAgency = "OriginatingAgency";

    private static final ElasticsearchFunctionalAdminIndexManager indexManager =
        FunctionalAdminCollectionsTestUtils.createTestIndexManager();

    private InputStream streamErrorReport;

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

        File tempFolder = temporaryFolder.newFolder();
        System.setProperty(VitamConfiguration.getVitamTmpProperty(), tempFolder.getAbsolutePath());
        SystemPropertyUtil.refresh();

        LogbookOperationsClientFactory.changeMode(null);

        final File adminConfig = PropertiesUtils.findFile(ADMIN_MANAGEMENT_CONF);
        final AdminManagementConfiguration realAdminConfig = PropertiesUtils.readYaml(
            adminConfig,
            AdminManagementConfiguration.class
        );
        realAdminConfig.getMongoDbNodes().get(0).setDbPort(mongoRule.getDataBasePort());
        realAdminConfig.setElasticsearchNodes(esNodes);
        realAdminConfig.setClusterName(ElasticsearchRule.VITAM_CLUSTER);
        realAdminConfig.setWorkspaceUrl("http://localhost:" + workspacePort);
        realAdminConfig.setDbName(MongoRule.VITAM_DB);
        adminConfigFile = File.createTempFile("test", ADMIN_MANAGEMENT_CONF, adminConfig.getParentFile());
        PropertiesUtils.writeYaml(adminConfigFile, realAdminConfig);

        final List<MongoDbNode> nodes = new ArrayList<>();
        nodes.add(new MongoDbNode(DATABASE_HOST, mongoRule.getDataBasePort()));
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
            throw new IllegalStateException("Cannot start the Logbook Application Server", e);
        }
    }

    @AfterClass
    public static void tearDownAfterClass() {
        System.setProperty(VitamConfiguration.getVitamTmpProperty(), VitamConfiguration.getVitamTmpFolder());
        SystemPropertyUtil.refresh();

        FunctionalAdminCollectionsTestUtils.afterTestClass(true);

        LOGGER.debug("Ending tests");
        try {
            application.stop();
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
        }
        junitHelper.releasePort(serverPort);
        VitamClientFactory.resetConnections();
    }

    @Before
    public void setUp() {
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(TENANT_ID));
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        instanceRule.stubFor(
            WireMock.post(urlMatching("/workspace/v1/containers/(.*)")).willReturn(
                aResponse().withStatus(201).withHeader(GlobalDataRest.X_TENANT_ID, Integer.toString(TENANT_ID))
            )
        );
        instanceRule.stubFor(
            WireMock.delete(urlMatching("/workspace/v1/containers/(.*)")).willReturn(
                aResponse().withStatus(204).withHeader(GlobalDataRest.X_TENANT_ID, Integer.toString(TENANT_ID))
            )
        );
    }

    @After
    public void tearDown() {
        FunctionalAdminCollectionsTestUtils.afterTest();
    }

    @Test
    @RunWithCustomExecutor
    public final void testGetStatus() {
        get(STATUS_URI).then().statusCode(Status.NO_CONTENT.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenAWellFormedXMLInputstreamCheckThenReturnOK() throws FileNotFoundException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(TENANT_ID));

        stream = PropertiesUtils.getResourceAsStream(PRONOM_FILE);
        given()
            .contentType(ContentType.BINARY)
            .body(stream)
            .when()
            .post(CHECK_FORMAT_URI)
            .then()
            .statusCode(Status.OK.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenANotWellFormedXMLInputstreamCheckThenReturnKO() throws FileNotFoundException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        stream = PropertiesUtils.getResourceAsStream("FF-vitam-format-KO.xml");
        given()
            .contentType(ContentType.BINARY)
            .body(stream)
            .when()
            .post(CHECK_FORMAT_URI)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void insertAPronomFile() throws FileNotFoundException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        stream = PropertiesUtils.getResourceAsStream(PRONOM_FILE);
        given()
            .contentType(ContentType.BINARY)
            .body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_FILENAME, PRONOM_FILE)
            .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
            .when()
            .post(IMPORT_FORMAT_URI)
            .then()
            .statusCode(Status.CREATED.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("FF-vitam-format-KO.xml");
        given()
            .contentType(ContentType.BINARY)
            .body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_FILENAME, "FF-vitam-format-KO.xml")
            .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
            .when()
            .post(IMPORT_FORMAT_URI)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void createAccessionRegister() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(TENANT_ID));

        stream = PropertiesUtils.getResourceAsStream("accession-register.json");
        final AccessionRegisterDetailModel register = JsonHandler.getFromInputStream(
            stream,
            AccessionRegisterDetailModel.class
        );
        GUID guid = GUIDFactory.newAccessionRegisterDetailGUID(TENANT_ID);
        register.setId(guid.toString());
        given()
            .contentType(ContentType.JSON)
            .body(register)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(CREATE_FUND_REGISTER_URI)
            .then()
            .statusCode(Status.CREATED.getStatusCode());

        // Already exists --> created
        given()
            .contentType(ContentType.JSON)
            .body(register)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(CREATE_FUND_REGISTER_URI)
            .then()
            .statusCode(Status.CREATED.getStatusCode());

        // Invalid request (bad format) --> bad request
        register.setTotalObjects(null);
        given()
            .contentType(ContentType.JSON)
            .body(register)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(CREATE_FUND_REGISTER_URI)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void findAccessionRegisterDetail() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID1);
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(TENANT_ID1));

        String contractId = "contractId";

        AccessContractModel contractModel = new AccessContractModel();
        contractModel.setOriginatingAgencies(Sets.newHashSet(originatingAgency));
        contractModel.setIdentifier(contractId);
        contractModel.setName(contractId);
        contractModel.setStatus(ActivationStatus.ACTIVE);
        contractModel.setCreationdate("2019-02-12T14:51:22.567");
        contractModel.setLastupdate("2019-02-12T14:51:23.567");
        contractModel.initializeDefaultValue();

        mongoDbAccess
            .insertDocument(JsonHandler.toJsonNode(contractModel), FunctionalAdminCollections.ACCESS_CONTRACT)
            .close();

        stream = PropertiesUtils.getResourceAsStream("accession-register.json");
        final AccessionRegisterDetailModel register = JsonHandler.getFromInputStream(
            stream,
            AccessionRegisterDetailModel.class
        );
        GUID guid = GUIDFactory.newAccessionRegisterDetailGUID(TENANT_ID1);
        register.setId(guid.toString());
        given()
            .contentType(ContentType.JSON)
            .body(register)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID1)
            .when()
            .post(CREATE_FUND_REGISTER_URI)
            .then()
            .statusCode(Status.CREATED.getStatusCode());
        register.setTotalObjects(null);

        Select select = new Select();

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID1)
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, contractId)
            .when()
            .post("accession-register/detail/" + originatingAgency)
            .then()
            .body("$results.size()", equalTo(1))
            .statusCode(Status.OK.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void getFileFormatByID() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(TENANT_ID));

        stream = PropertiesUtils.getResourceAsStream(PRONOM_FILE);
        final Select select = new Select();
        select.setQuery(eq("PUID", "x-fmt/2"));
        with()
            .contentType(ContentType.BINARY)
            .body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
            .header(GlobalDataRest.X_FILENAME, PRONOM_FILE)
            .when()
            .post(IMPORT_FORMAT_URI)
            .then()
            .statusCode(Status.CREATED.getStatusCode());

        final String document = given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(select.getFinalSelect())
            .when()
            .post(GET_DOCUMENT_FORMAT_URI)
            .getBody()
            .asString();
        final JsonNode jsonDocument = JsonHandler.getFromString(document).get(RESULTS);

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(jsonDocument)
            .pathParam("id_format", jsonDocument.get(0).get("PUID").asText())
            .when()
            .get(GET_BYID_FORMAT_URI + FORMAT_ID_URI)
            .then()
            .statusCode(Status.OK.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenFileFormatByIDWhenNotFoundThenThrowReferentialException() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        stream = PropertiesUtils.getResourceAsStream(PRONOM_FILE);
        final Select select = new Select();
        select.setQuery(eq("PUID", "x-fmt/2"));
        with()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .contentType(ContentType.BINARY)
            .body(stream)
            .header(GlobalDataRest.X_FILENAME, PRONOM_FILE)
            .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
            .when()
            .post(IMPORT_FORMAT_URI)
            .then()
            .statusCode(Status.CREATED.getStatusCode());

        final String document = given()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .when()
            .post(GET_DOCUMENT_FORMAT_URI)
            .getBody()
            .asString();
        final JsonNode jsonDocument = JsonHandler.getFromString(document);

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(jsonDocument)
            .pathParam("id_format", "fake_identifier")
            .when()
            .get(GET_BYID_FORMAT_URI + FORMAT_ID_URI)
            .then()
            .statusCode(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void findFormat() throws Exception {
        stream = PropertiesUtils.getResourceAsStream(PRONOM_FILE);
        final Select select = new Select();
        select.setQuery(eq("PUID", "x-fmt/2"));
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        with()
            .contentType(ContentType.BINARY)
            .body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_FILENAME, PRONOM_FILE)
            .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
            .when()
            .post(IMPORT_FORMAT_URI)
            .then()
            .statusCode(Status.CREATED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(select.getFinalSelect())
            .when()
            .post(GET_DOCUMENT_FORMAT_URI)
            .then()
            .statusCode(Status.OK.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenFindDocumentWhenNotFoundThenReturnZeroResult() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        stream = PropertiesUtils.getResourceAsStream(PRONOM_FILE);
        final Select select = new Select();
        select.setQuery(eq("fakeName", "fakeValue"));
        with()
            .contentType(ContentType.BINARY)
            .body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
            .header(GlobalDataRest.X_FILENAME, PRONOM_FILE)
            .when()
            .post(IMPORT_FORMAT_URI)
            .then()
            .statusCode(Status.CREATED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
            .body(select.getFinalSelect())
            .when()
            .post(GET_DOCUMENT_FORMAT_URI)
            .then()
            .statusCode(Status.OK.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenAWellFormedCSVInputstreamCheckThenReturnOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(TENANT_ID));

        stream = PropertiesUtils.getResourceAsStream(FILE_TEST_OK);
        given()
            .contentType(ContentType.BINARY)
            .body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
            .when()
            .post(CHECK_RULES_URI)
            .then()
            .statusCode(Status.OK.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenANotWellFormedCSVInputstreamCheckThenReturnKO()
        throws IOException, InvalidParseOperationException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(TENANT_ID));

        streamErrorReport = PropertiesUtils.getResourceAsStream(ERROR_REPORT_CONTENT);
        stream = PropertiesUtils.getResourceAsStream("jeu_donnees_KO_regles_CSV_DuplicatedReference.csv");
        Response rr = given()
            .contentType(ContentType.BINARY)
            .body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(CHECK_RULES_URI);
        rr.then().statusCode(Status.BAD_REQUEST.getStatusCode());
        JsonNode responseInputStream = JsonHandler.getFromInputStream(rr.asInputStream());
        ArrayNode responseArrayNode = (ArrayNode) responseInputStream.get("error").get("line 3");
        JsonNode expectedInputStream = JsonHandler.getFromInputStream(streamErrorReport);
        ArrayNode expectedArrayNode = (ArrayNode) expectedInputStream.get("error").get("line 3");
        assertEquals(responseArrayNode.get(0), expectedArrayNode.get(0));
    }

    @Test
    @RunWithCustomExecutor
    public void givenADecadeMeasureCSVInputstreamCheckThenReturnKO() throws FileNotFoundException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        stream = PropertiesUtils.getResourceAsStream("jeu_donnees_KO_regles_CSV_Decade_Measure.csv");
        given()
            .contentType(ContentType.BINARY)
            .body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(CHECK_RULES_URI)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenAnANarchyRuleTypeCSVInputstreamCheckThenReturnKO() throws FileNotFoundException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        stream = PropertiesUtils.getResourceAsStream("jeu_donnees_KO_regles_CSV_AnarchyRule.csv");
        given()
            .contentType(ContentType.BINARY)
            .body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(CHECK_RULES_URI)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenWrongDurationTypeCSVInputstreamCheckThenReturnKO() throws FileNotFoundException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        stream = PropertiesUtils.getResourceAsStream("jeu_donnees_KO_regles_CSV_90000_YEAR.csv");
        given()
            .contentType(ContentType.BINARY)
            .body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(CHECK_RULES_URI)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenDuplicatedReferenceCSVInputstreamCheckThenReturnKO() throws FileNotFoundException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(TENANT_ID));
        stream = PropertiesUtils.getResourceAsStream("jeu_donnees_KO_regles_CSV_DuplicatedReference.csv");
        given()
            .contentType(ContentType.BINARY)
            .body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(CHECK_RULES_URI)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenNegativeDurationCSVInputstreamCheckThenReturnKO() throws FileNotFoundException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        stream = PropertiesUtils.getResourceAsStream("jeu_donnees_KO_regles_CSV_Negative_Duration.csv");
        given()
            .contentType(ContentType.BINARY)
            .body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(CHECK_RULES_URI)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenReferenceWithWrongCommaCSVInputstreamCheckThenReturnKO() throws FileNotFoundException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        stream = PropertiesUtils.getResourceAsStream("jeu_donnees_KO_regles_CSV_ReferenceWithWrongComma.csv");
        given()
            .contentType(ContentType.BINARY)
            .body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(CHECK_RULES_URI)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenUnknownDurationCSVInputstreamCheckThenReturnKO() throws FileNotFoundException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        stream = PropertiesUtils.getResourceAsStream("jeu_donnees_KO_regles_CSV_UNKNOWN_Duration.csv");
        given()
            .contentType(ContentType.BINARY)
            .body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(CHECK_RULES_URI)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void given15000JoursCSVInputstreamCheckThenReturnOK() throws FileNotFoundException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        stream = PropertiesUtils.getResourceAsStream("jeu_donnees_OK_regles_CSV_15000Jours.csv");
        given()
            .contentType(ContentType.BINARY)
            .body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(CHECK_RULES_URI)
            .then()
            .statusCode(Status.OK.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenUnlimitedDurationCSVInputstreamCheckThenReturnOK() throws FileNotFoundException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        stream = PropertiesUtils.getResourceAsStream("jeu_donnees_OK_regles_CSV_unLimiTEd.csv");
        given()
            .contentType(ContentType.BINARY)
            .body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(CHECK_RULES_URI)
            .then()
            .statusCode(Status.OK.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void given600000DAYCSVInputstreamCheckThenReturnKO() throws FileNotFoundException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        stream = PropertiesUtils.getResourceAsStream("jeu_donnees_KO_regles_600000_DAY.csv");
        given()
            .contentType(ContentType.BINARY)
            .body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(CHECK_RULES_URI)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void given90000YEARCSVInputstreamCheckThenReturnKO() throws FileNotFoundException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        stream = PropertiesUtils.getResourceAsStream("jeu_donnees_KO_regles_CSV_90000_YEAR.csv");
        given()
            .contentType(ContentType.BINARY)
            .body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(CHECK_RULES_URI)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void insertRulesFile() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        String resquestId = GUIDFactory.newOperationLogbookGUID(TENANT_ID).toString();
        stream = PropertiesUtils.getResourceAsStream(FILE_TEST_OK);
        given()
            .contentType(ContentType.BINARY)
            .body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_REQUEST_ID, resquestId)
            .header(GlobalDataRest.X_FILENAME, FILE_TEST_OK)
            .when()
            .post(IMPORT_RULES_URI)
            .then()
            .statusCode(Status.CREATED.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream(FILE_TEST_OK);
        resquestId = GUIDFactory.newOperationLogbookGUID(TENANT_ID).toString();
        given()
            .contentType(ContentType.BINARY)
            .body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_REQUEST_ID, resquestId)
            .when()
            .post(IMPORT_RULES_URI)
            .then()
            .statusCode(Status.CREATED.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void insertRulesForDifferentTenantsSuccess() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        String resquestId = GUIDFactory.newOperationLogbookGUID(TENANT_ID).toString();
        stream = PropertiesUtils.getResourceAsStream(FILE_TEST_OK);
        given()
            .contentType(ContentType.BINARY)
            .body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_REQUEST_ID, resquestId)
            .header(GlobalDataRest.X_FILENAME, FILE_TEST_OK)
            .when()
            .post(IMPORT_RULES_URI)
            .then()
            .statusCode(Status.CREATED.getStatusCode());

        mongoDbAccess.deleteCollectionForTesting(FunctionalAdminCollections.RULES).close();

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID1);
        resquestId = GUIDFactory.newOperationLogbookGUID(TENANT_ID).toString();
        stream = PropertiesUtils.getResourceAsStream(FILE_TEST_OK);
        given()
            .contentType(ContentType.BINARY)
            .body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID1)
            .header(GlobalDataRest.X_REQUEST_ID, resquestId)
            .header(GlobalDataRest.X_FILENAME, FILE_TEST_OK)
            .when()
            .post(IMPORT_RULES_URI)
            .then()
            .statusCode(Status.CREATED.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void getRuleByID() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        stream = PropertiesUtils.getResourceAsStream(FILE_TEST_OK);
        String resquestId = GUIDFactory.newOperationLogbookGUID(TENANT_ID).toString();
        final Select select = new Select();
        select.setQuery(eq("RuleId", "APP-00001"));
        with()
            .contentType(ContentType.BINARY)
            .body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_REQUEST_ID, resquestId)
            .header(GlobalDataRest.X_FILENAME, FILE_TEST_OK)
            .when()
            .post(IMPORT_RULES_URI)
            .then()
            .statusCode(Status.CREATED.getStatusCode());

        resquestId = GUIDFactory.newOperationLogbookGUID(TENANT_ID).toString();
        final String document = given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_REQUEST_ID, resquestId)
            .body(select.getFinalSelect())
            .when()
            .post(GET_DOCUMENT_RULES_URI)
            .getBody()
            .asString();
        final JsonNode jsonDocument = JsonHandler.getFromString(document).get(RESULTS);

        resquestId = GUIDFactory.newOperationLogbookGUID(TENANT_ID).toString();
        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_REQUEST_ID, resquestId)
            .body(jsonDocument)
            .pathParam("id_rule", jsonDocument.get(0).get("RuleId").asText())
            .when()
            .get(GET_BYID_RULES_URI + RULES_ID_URI)
            .then()
            .statusCode(Status.OK.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenFakeRuleByIDTheReturnNotFound() throws Exception {
        stream = PropertiesUtils.getResourceAsStream(FILE_TEST_OK);
        final Select select = new Select();
        select.setQuery(eq("RuleId", "APP-00001"));
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        String resquestId = GUIDFactory.newOperationLogbookGUID(TENANT_ID).toString();
        with()
            .contentType(ContentType.BINARY)
            .body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_REQUEST_ID, resquestId)
            .header(GlobalDataRest.X_FILENAME, FILE_TEST_OK)
            .when()
            .post(IMPORT_RULES_URI)
            .then()
            .statusCode(Status.CREATED.getStatusCode());

        resquestId = GUIDFactory.newOperationLogbookGUID(TENANT_ID).toString();
        final String document = given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_REQUEST_ID, resquestId)
            .body(select.getFinalSelect())
            .when()
            .post(GET_DOCUMENT_RULES_URI)
            .getBody()
            .asString();
        final JsonNode jsonDocument = JsonHandler.getFromString(document);

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_REQUEST_ID, resquestId)
            .body(jsonDocument)
            .when()
            .get(GET_BYID_RULES_URI + "/fake_identifier")
            .then()
            .statusCode(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void getDocumentRulesFile() throws InvalidCreateOperationException, FileNotFoundException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        String resquestId = GUIDFactory.newOperationLogbookGUID(TENANT_ID).toString();
        stream = PropertiesUtils.getResourceAsStream(FILE_TEST_OK);
        final Select select = new Select();
        select.setQuery(eq("RuleId", "APP-00001"));
        with()
            .contentType(ContentType.BINARY)
            .body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_REQUEST_ID, resquestId)
            .header(GlobalDataRest.X_FILENAME, FILE_TEST_OK)
            .when()
            .post(IMPORT_RULES_URI)
            .then()
            .statusCode(Status.CREATED.getStatusCode());

        resquestId = GUIDFactory.newOperationLogbookGUID(TENANT_ID).toString();
        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_REQUEST_ID, resquestId)
            .body(select.getFinalSelect())
            .when()
            .post(GET_DOCUMENT_RULES_URI)
            .then()
            .statusCode(Status.OK.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void getDocumentRulesFileBug4806WhenOrderByAnalyzedFieldThenReturnInternalServerError() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        String resquestId = GUIDFactory.newOperationLogbookGUID(TENANT_ID).toString();
        stream = PropertiesUtils.getResourceAsStream(FILE_TEST_OK);

        with()
            .contentType(ContentType.BINARY)
            .body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_REQUEST_ID, resquestId)
            .header(GlobalDataRest.X_FILENAME, FILE_TEST_OK)
            .when()
            .post(IMPORT_RULES_URI)
            .then()
            .statusCode(Status.CREATED.getStatusCode());

        resquestId = GUIDFactory.newOperationLogbookGUID(TENANT_ID).toString();

        final Select selectOrderByNonAnalyzed = new Select();
        selectOrderByNonAnalyzed.setQuery(eq("RuleId", "APP-00001"));
        selectOrderByNonAnalyzed.addOrderByAscFilter("RuleId");

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_REQUEST_ID, resquestId)
            .body(selectOrderByNonAnalyzed.getFinalSelect())
            .when()
            .post(GET_DOCUMENT_RULES_URI)
            .then()
            .statusCode(Status.OK.getStatusCode());

        final Select selectOrderByAnalyzed = new Select();
        selectOrderByAnalyzed.setQuery(eq("RuleId", "APP-00001"));
        selectOrderByAnalyzed.addOrderByAscFilter("RuleValue");

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_REQUEST_ID, resquestId)
            .body(selectOrderByAnalyzed.getFinalSelect())
            .when()
            .post(GET_DOCUMENT_RULES_URI)
            .then()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void testImportRulesForTenant0_ThenSearchForTenant1ReturnNotFound()
        throws InvalidCreateOperationException, FileNotFoundException {
        stream = PropertiesUtils.getResourceAsStream(FILE_TEST_OK);
        String resquestId = GUIDFactory.newOperationLogbookGUID(TENANT_ID).toString();
        final Select select = new Select();
        select.setQuery(eq("RuleId", "APP-00001"));
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        with()
            .contentType(ContentType.BINARY)
            .body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_REQUEST_ID, resquestId)
            .header(GlobalDataRest.X_FILENAME, FILE_TEST_OK)
            .when()
            .post(IMPORT_RULES_URI)
            .then()
            .statusCode(Status.CREATED.getStatusCode());

        resquestId = GUIDFactory.newOperationLogbookGUID(TENANT_ID1).toString();
        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID1)
            .header(GlobalDataRest.X_REQUEST_ID, resquestId)
            .body(select.getFinalSelect())
            .when()
            .post(GET_DOCUMENT_RULES_URI)
            .then()
            .statusCode(Status.OK.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenFindDocumentRulesFileWhenNotFoundThenReturnNotFound() throws Exception {
        String resquestId = GUIDFactory.newOperationLogbookGUID(TENANT_ID).toString();
        stream = PropertiesUtils.getResourceAsStream(FILE_TEST_OK);
        final Select select = new Select();
        select.setQuery(eq("fakeName", "fakeValue"));

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        with()
            .contentType(ContentType.BINARY)
            .body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_REQUEST_ID, resquestId)
            .header(GlobalDataRest.X_FILENAME, FILE_TEST_OK)
            .when()
            .post(IMPORT_RULES_URI)
            .then()
            .statusCode(Status.CREATED.getStatusCode());

        resquestId = GUIDFactory.newOperationLogbookGUID(TENANT_ID).toString();
        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_REQUEST_ID, resquestId)
            .body(select.getFinalSelect())
            .when()
            .post(GET_DOCUMENT_RULES_URI)
            .then()
            .statusCode(Status.OK.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void createExternalLogbook() throws Exception {
        GUID request = GUIDFactory.newOperationLogbookGUID(TENANT_ID);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        VitamThreadUtils.getVitamSession().setRequestId(request);

        LogbookOperationParameters logbook = fillLogbookParameters(
            newOperationLogbookGUID(TENANT_ID).getId(),
            newOperationLogbookGUID(TENANT_ID).getId()
        );

        given()
            .contentType(ContentType.JSON)
            .body(JsonHandler.toJsonNode(logbook))
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_REQUEST_ID, request.getId())
            .when()
            .post(CREATE_EXTERNAL_LOGBOOK_URI)
            .then()
            .statusCode(Status.CREATED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(JsonHandler.toJsonNode(LogbookParameterHelper.newLogbookOperationParameters()))
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_REQUEST_ID, request.getId())
            .when()
            .post(CREATE_EXTERNAL_LOGBOOK_URI)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());

        logbook.setTypeProcess(LogbookTypeProcess.AUDIT);
        given()
            .contentType(ContentType.JSON)
            .body(JsonHandler.toJsonNode(JsonHandler.toJsonNode(logbook)))
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_REQUEST_ID, request.getId())
            .when()
            .post(CREATE_EXTERNAL_LOGBOOK_URI)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    private LogbookOperationParameters fillLogbookParameters(String guid, String evIdProc) {
        LogbookOperationParameters logbookParamaters = LogbookParameterHelper.newLogbookOperationParameters();
        logbookParamaters.putParameterValue(LogbookParameterName.eventIdentifier, guid);
        logbookParamaters.putParameterValue(
            LogbookParameterName.eventType,
            "EXT_" + LogbookParameterName.eventType.name()
        );
        logbookParamaters.putParameterValue(LogbookParameterName.eventDateTime, LocalDateUtil.nowFormatted());
        logbookParamaters.putParameterValue(
            LogbookParameterName.eventIdentifierProcess,
            evIdProc != null ? evIdProc : guid
        );
        logbookParamaters.setTypeProcess(LogbookTypeProcess.EXTERNAL_LOGBOOK);
        logbookParamaters.putParameterValue(LogbookParameterName.outcome, LogbookParameterName.outcome.name());
        logbookParamaters.putParameterValue(
            LogbookParameterName.outcomeDetail,
            LogbookParameterName.outcomeDetail.name()
        );
        logbookParamaters.putParameterValue(
            LogbookParameterName.outcomeDetailMessage,
            LogbookParameterName.outcomeDetailMessage.name()
        );
        logbookParamaters.putParameterValue(
            LogbookParameterName.agentIdentifier,
            LogbookParameterName.agentIdentifier.name()
        );
        logbookParamaters.putParameterValue(
            LogbookParameterName.agentIdentifierApplicationSession,
            LogbookParameterName.agentIdentifierApplicationSession.name()
        );
        logbookParamaters.putParameterValue(
            LogbookParameterName.eventIdentifierRequest,
            LogbookParameterName.eventIdentifierRequest.name()
        );
        logbookParamaters.putParameterValue(
            LogbookParameterName.agIdExt,
            JsonHandler.unprettyPrint(JsonHandler.createObjectNode())
        );

        logbookParamaters.putParameterValue(
            LogbookParameterName.objectIdentifier,
            LogbookParameterName.objectIdentifier.name()
        );
        logbookParamaters.putParameterValue(
            LogbookParameterName.objectIdentifierRequest,
            LogbookParameterName.objectIdentifierRequest.name()
        );
        logbookParamaters.putParameterValue(
            LogbookParameterName.objectIdentifierIncome,
            LogbookParameterName.objectIdentifierIncome.name()
        );

        return logbookParamaters;
    }
}
