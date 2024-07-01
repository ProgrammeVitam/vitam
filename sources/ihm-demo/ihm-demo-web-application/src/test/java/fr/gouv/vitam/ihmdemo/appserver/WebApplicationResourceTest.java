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
package fr.gouv.vitam.ihmdemo.appserver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import fr.gouv.vitam.access.external.client.AccessExternalClientFactory;
import fr.gouv.vitam.access.external.client.AdminExternalClient;
import fr.gouv.vitam.access.external.client.AdminExternalClientFactory;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientServerException;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.ClientMockResultHelper;
import fr.gouv.vitam.common.client.IngestCollection;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.GLOBAL;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.error.ServiceName;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.external.client.AbstractMockClient;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AccessionRegisterSummaryModel;
import fr.gouv.vitam.common.model.administration.AgenciesModel;
import fr.gouv.vitam.common.model.logbook.LogbookLifecycle;
import fr.gouv.vitam.common.xsrf.filter.XSRFFilter;
import fr.gouv.vitam.common.xsrf.filter.XSRFHelper;
import fr.gouv.vitam.ihmdemo.common.pagination.PaginationHelper;
import fr.gouv.vitam.ihmdemo.core.DslQueryHelper;
import fr.gouv.vitam.ihmdemo.core.UiConstants;
import fr.gouv.vitam.ihmdemo.core.UserInterfaceTransactionManager;
import fr.gouv.vitam.ingest.external.api.exception.IngestExternalException;
import fr.gouv.vitam.ingest.external.client.IngestExternalClient;
import fr.gouv.vitam.ingest.external.client.IngestExternalClientFactory;
import io.restassured.RestAssured;
import io.restassured.config.EncoderConfig;
import io.restassured.http.ContentType;
import io.restassured.http.Cookie;
import io.restassured.http.Header;
import io.restassured.response.ResponseBody;
import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response.Status;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WebApplicationResourceTest {

    private static final String DEFAULT_WEB_APP_CONTEXT = "/ihm-demo";
    private static final String DEFAULT_WEB_APP_CONTEXT_V2 = "/ihm-demo-v2";
    private static final String OPTIONS =
        "{\"dslRequest\": {\"$query\": [{\"$eq\": {\"#id\": \"toto\"}}],\"$filter\": {},\"$projection\": {}},\"dataObjectVersionToExport\": { }}";
    private static final String AUDIT_OPTION = "{serviceProducteur: \"Service Producteur 1\"}";
    private static final Cookie COOKIE = new Cookie.Builder("JSESSIONID", "testId").build();
    private static final String CREDENTIALS = "{\"token\": {\"principal\": \"myName\", \"credentials\": \"myName\"}}";
    private static final String CREDENTIALS_NO_VALID =
        "{\"token\": {\"principal\": \"myName\", \"credentials\": \"myName\"}}";
    private static final String OPTIONS_DOWNLOAD = "{usage: \"Dissemination\", version: 1}";
    private static final String UPDATE = "{title: \"myarchive\"}";
    private static final String DEFAULT_HOST = "localhost";
    private static final String JETTY_CONFIG = "jetty-config-test.xml";
    private static final String TREE_QUERY =
        "{\"$query\": [{" +
        "\"$and\": [{" +
        "\"$in\": {\"#id\": [\"P1\",\"P2\",\"P3\"]}},{" +
        "\"$eq\": {\"#max\": 1}" +
        "}]}], \"$projection\": {}}";
    private static final JsonNode FAKE_JSONNODE_RETURN = JsonHandler.createObjectNode();
    private static final String FAKE_UNIT_LF_ID = "1";
    private static final String FAKE_OBG_LF_ID = "1";
    private static final String INGEST_URI = "/ingests";
    private static JunitHelper junitHelper;
    private static int port;
    private static IhmDemoMain application;
    private static final List<Integer> tenants = new ArrayList<>();

    private static final String TRACEABILITY_CHECK_URL = "traceability/check";
    private static final String TRACEABILITY_CHECK_MAP = "{EventID: \"fake_id\"}";

    private final int TENANT_ID = 0;
    private final String CONTRACT_NAME = "contract";
    private static final String STATUS_FIELD_QUERY = "Status";
    private static final String ACTIVATION_DATE_FIELD_QUERY = "ActivationDate";
    private static final String LAST_UPDATE_FIELD_QUERY = "LastUpdate";
    private static final String NAME_FIELD_QUERY = "Name";
    private static final String ADMIN_EXTERNAL_MODULE = "AdminExternalModule";
    private static final String IHM_DEMO_CONF = "ihm-demo.conf";
    static final String tokenCSRF = XSRFHelper.generateCSRFToken();

    private UserInterfaceTransactionManager userInterfaceTransactionManager;
    private AdminExternalClientFactory adminExternalClientFactory;
    private DslQueryHelper dslQueryHelper;
    private PaginationHelper paginationHelper;
    private IngestExternalClientFactory ingestExternalClientFactory;

    @BeforeClass
    public static void setup() throws Exception {
        junitHelper = JunitHelper.getInstance();
        port = junitHelper.findAvailablePort();
        tenants.add(0);
        tenants.add(1);

        final WebApplicationConfig webApplicationConfig = (WebApplicationConfig) new WebApplicationConfig()
            .setPort(port)
            .setServerHost(DEFAULT_HOST)
            .setBaseUrl(DEFAULT_WEB_APP_CONTEXT)
            .setBaseUri(DEFAULT_WEB_APP_CONTEXT_V2)
            .setJettyConfig(JETTY_CONFIG);
        webApplicationConfig.setEnableSession(true);
        webApplicationConfig.setEnableXsrFilter(true);
        VitamConfiguration.setTenants(tenants);
        VitamConfiguration.setAdminTenant(1);
        final File conf = PropertiesUtils.findFile(IHM_DEMO_CONF);
        final File newConf = File.createTempFile("test", IHM_DEMO_CONF, conf.getParentFile());
        PropertiesUtils.writeYaml(newConf, webApplicationConfig);
        application = new IhmDemoMain(newConf.getAbsolutePath(), BusinessApplicationTest.class, null);
        application.start();
        RestAssured.port = port;
        RestAssured.basePath = DEFAULT_WEB_APP_CONTEXT + "/v1/api";

        XSRFFilter.addToken("testId", tokenCSRF);
    }

    @AfterClass
    public static void tearDownAfterClass() {
        try {
            application.stop();
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }
        junitHelper.releasePort(port);
    }

    @Before
    public void initStaticMock() {
        userInterfaceTransactionManager = BusinessApplicationTest.getUserInterfaceTransactionManager();
        adminExternalClientFactory = BusinessApplicationTest.getAdminExternalClientFactory();
        AccessExternalClientFactory accessExternalClientFactory =
            BusinessApplicationTest.getAccessExternalClientFactory();
        ingestExternalClientFactory = BusinessApplicationTest.getIngestExternalClientFactory();
        paginationHelper = BusinessApplicationTest.getPaginationHelper();
        dslQueryHelper = BusinessApplicationTest.getDslQueryHelper();

        Mockito.reset(userInterfaceTransactionManager);
        Mockito.reset(adminExternalClientFactory);
        Mockito.reset(accessExternalClientFactory);
        Mockito.reset(ingestExternalClientFactory);
        Mockito.reset(paginationHelper);
        Mockito.reset(dslQueryHelper);
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testMessagesLogbook() throws InvalidParseOperationException {
        final ResponseBody response = given()
            .contentType(ContentType.JSON)
            .expect()
            .statusCode(Status.OK.getStatusCode())
            .when()
            .get("/messages/logbook")
            .getBody();
        final JsonNode jsonNode = JsonHandler.getFromInputStream(response.asInputStream());
        assertTrue(jsonNode.isObject());
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testTenants() throws InvalidParseOperationException {
        final ResponseBody response = given()
            .contentType(ContentType.JSON)
            .expect()
            .statusCode(Status.OK.getStatusCode())
            .when()
            .get("/tenants")
            .getBody();
        final JsonNode jsonNode = JsonHandler.getFromInputStream(response.asInputStream());
        assertTrue(jsonNode.isArray());
        assertEquals(0, jsonNode.get(0).asInt());
    }

    @Test
    public void givenNoArchiveUnitWhenSearchOperationsThenReturnOK() throws Exception {
        when(userInterfaceTransactionManager.searchUnits(any(), any())).thenReturn(
            RequestResponseOK.getFromJsonNode(FAKE_JSONNODE_RETURN)
        );

        doNothing().when(paginationHelper).setResult(anyString(), any());
        when(paginationHelper.getResult(any(JsonNode.class), any())).thenReturn(JsonHandler.createObjectNode());

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .body(OPTIONS)
            .cookie(COOKIE)
            .expect()
            .statusCode(Status.OK.getStatusCode())
            .when()
            .post("/archivesearch/units");
    }

    @Test
    public void givenNoSecureServerLoginUnauthorized() {
        given()
            .contentType(ContentType.JSON)
            .body(CREDENTIALS)
            .expect()
            .statusCode(Status.UNAUTHORIZED.getStatusCode())
            .when()
            .post("login");
        given()
            .contentType(ContentType.JSON)
            .body(CREDENTIALS_NO_VALID)
            .expect()
            .statusCode(Status.UNAUTHORIZED.getStatusCode())
            .when()
            .post("login");
    }

    private Map<String, String> createActiveMapForUpdateCommonContract() {
        String now = LocalDateUtil.now().toString();
        Map<String, String> parameters = new HashMap<>();
        parameters.put(STATUS_FIELD_QUERY, "ACTIVE");
        parameters.put(LAST_UPDATE_FIELD_QUERY, now);
        parameters.put(ACTIVATION_DATE_FIELD_QUERY, now);
        parameters.put(NAME_FIELD_QUERY, "aName");
        return parameters;
    }

    private void initializeAdminExternalClientMock()
        throws InvalidParseOperationException, AccessExternalClientException {
        final RequestResponse<?> mockResponse = mock(RequestResponse.class);
        final AdminExternalClient adminExternalClient = mock(AdminExternalClient.class);
        Mockito.when(adminExternalClientFactory.getClient()).thenReturn(adminExternalClient);

        JsonNode jsonNode = JsonHandler.createObjectNode();
        doReturn("Atr").when(mockResponse).getHeaderString(any());
        doReturn(mockResponse)
            .when(adminExternalClient)
            .updateAccessContract(eq(new VitamContext(TENANT_ID)), eq("azercdsqsdf"), eq(jsonNode));
    }

    @Test
    public void givenAccessContractTestUpdate() throws InvalidParseOperationException, AccessExternalClientException {
        initializeAdminExternalClientMock();
        final Map<String, String> parameters = createActiveMapForUpdateCommonContract();
        String jsonObject = JsonHandler.unprettyPrint(parameters);

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .body(jsonObject)
            .cookie(COOKIE)
            .expect()
            .statusCode(Status.OK.getStatusCode())
            .when()
            .post("/accesscontracts/azercdsqsdf");
    }

    @Test
    public void givenIngestContractTestUpdate() throws InvalidParseOperationException, AccessExternalClientException {
        initializeAdminExternalClientMock();
        final Map<String, String> parameters = createActiveMapForUpdateCommonContract();
        String jsonObject = JsonHandler.unprettyPrint(parameters);
        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .body(jsonObject)
            .cookie(COOKIE)
            .expect()
            .statusCode(Status.OK.getStatusCode())
            .when()
            .post("/contracts/azercdsqsdf")
            .getBody();
    }

    @Test
    public void givenManagementContractTestUpdate()
        throws InvalidParseOperationException, AccessExternalClientException {
        initializeAdminExternalClientMock();
        final Map<String, String> parameters = createActiveMapForUpdateCommonContract();
        String jsonObject = JsonHandler.unprettyPrint(parameters);
        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .body(jsonObject)
            .cookie(COOKIE)
            .expect()
            .statusCode(Status.OK.getStatusCode())
            .when()
            .post("/managementcontracts/azercdsqsdf")
            .getBody();
    }

    @Test
    public void testSuccessStatus() {
        given()
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .cookie(COOKIE)
            .expect()
            .statusCode(Status.NO_CONTENT.getStatusCode())
            .when()
            .get("status");
    }

    @Test
    public void testLogbookResultRemainingExceptions() throws Exception {
        final Map<String, Object> searchCriteriaMap = JsonHandler.getMapFromString(OPTIONS);
        final JsonNode preparedDslQuery = JsonHandler.createObjectNode();
        when(dslQueryHelper.createSingleQueryDSL(searchCriteriaMap)).thenReturn(preparedDslQuery);

        when(userInterfaceTransactionManager.selectOperation(any(), any())).thenThrow(new RuntimeException());
        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .body(OPTIONS)
            .cookie(COOKIE)
            .expect()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
            .when()
            .post("/logbook/operations");
    }

    @Test
    public void testGetLogbookResultByIdVitamClientException() throws Exception {
        String contractName = "test_contract";
        when(userInterfaceTransactionManager.selectOperationbyId(any(), any())).thenThrow(VitamClientException.class);

        given()
            .param("idOperation", "1")
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .header(new Header(GlobalDataRest.X_ACCESS_CONTRAT_ID, contractName))
            .cookie(COOKIE)
            .expect()
            .statusCode(Status.NOT_FOUND.getStatusCode())
            .when()
            .post("/logbook/operations/1");
    }

    @Test
    public void testGetLogbookResultByIdLogbookRemainingException() throws Exception {
        String contractName = "test_contract";
        when(userInterfaceTransactionManager.selectOperationbyId(any(), any())).thenThrow(RuntimeException.class);

        given()
            .param("idOperation", "1")
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .header(new Header(GlobalDataRest.X_ACCESS_CONTRAT_ID, contractName))
            .cookie(COOKIE)
            .expect()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
            .when()
            .post("/logbook/operations/1");
    }

    @Test
    public void testGetLogbookResultByIdLogbookRemainingIllrgalArgumentException() {
        given()
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .cookie(COOKIE)
            .contentType(ContentType.JSON)
            .expect()
            .statusCode(Status.BAD_REQUEST.getStatusCode())
            .when()
            .post("/logbook/operations/1");
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void testArchiveSearchResultDslQueryHelperExceptions()
        throws InvalidParseOperationException, InvalidCreateOperationException {
        final Map<String, Object> searchCriteriaMap = JsonHandler.getMapFromString(OPTIONS);

        // DslqQueryHelper Exceptions : InvalidParseOperationException,
        // InvalidCreateOperationException
        when(dslQueryHelper.createSelectElasticsearchDSLQuery(searchCriteriaMap)).thenThrow(
            InvalidParseOperationException.class,
            InvalidCreateOperationException.class
        );

        given()
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .cookie(COOKIE)
            .contentType(ContentType.JSON)
            .body(OPTIONS)
            .expect()
            .statusCode(Status.BAD_REQUEST.getStatusCode())
            .when()
            .post("/archivesearch/units");
    }

    @Test
    public void testArchiveSearchResultAccessExternalClientServerException() throws Exception {
        final Map<String, Object> searchCriteriaMap = JsonHandler.getMapFromString(OPTIONS);
        final JsonNode preparedDslQuery = JsonHandler.createObjectNode();

        when(dslQueryHelper.createSelectElasticsearchDSLQuery(searchCriteriaMap)).thenReturn(preparedDslQuery);

        // UserInterfaceTransactionManager Exception 1 :
        // AccessExternalClientServerException
        when(userInterfaceTransactionManager.searchUnits(any(), any())).thenThrow(new VitamClientException(""));

        given()
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .contentType(ContentType.JSON)
            .body(OPTIONS)
            .cookie(COOKIE)
            .expect()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
            .when()
            .post("/archivesearch/units");
    }

    @Test
    public void testArchiveSearchResultRemainingExceptions() throws Exception {
        final Map<String, Object> searchCriteriaMap = JsonHandler.getMapFromString(OPTIONS);
        final JsonNode preparedDslQuery = JsonHandler.createObjectNode();

        when(dslQueryHelper.createSelectElasticsearchDSLQuery(searchCriteriaMap)).thenReturn(preparedDslQuery);

        // UserInterfaceTransactionManager Exception 1 :
        // AccessExternalClientServerException
        when(userInterfaceTransactionManager.searchUnits(any(), any())).thenThrow(VitamClientException.class);

        given()
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .contentType(ContentType.JSON)
            .body(OPTIONS)
            .cookie(COOKIE)
            .expect()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
            .when()
            .post("/archivesearch/units");
    }

    @Test
    public void testGetArchiveUnitDetails() throws Exception {
        final Map<String, String> searchCriteriaMap = new HashMap<>();
        searchCriteriaMap.put(DslQueryHelper.PROJECTION_DSL, GLOBAL.RULES.exactToken());

        final JsonNode preparedDslQuery = JsonHandler.createObjectNode();

        when(dslQueryHelper.createGetByIdDSLSelectMultipleQuery(searchCriteriaMap)).thenReturn(preparedDslQuery);

        when(userInterfaceTransactionManager.getArchiveUnitDetails(any(), any(), any())).thenReturn(
            new RequestResponseOK<JsonNode>().setHttpCode(Status.OK.getStatusCode())
        );

        given()
            .param("id", "1")
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .cookie(COOKIE)
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_NAME)
            .expect()
            .statusCode(Status.OK.getStatusCode())
            .when()
            .get("/archivesearch/unit/1");
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void testArchiveUnitDetailsDslQueryHelperExceptions()
        throws InvalidParseOperationException, InvalidCreateOperationException {
        final Map<String, String> searchCriteriaMap = new HashMap<>();
        // searchCriteriaMap.put(UiConstants.SELECT_BY_ID.toString(), "1");
        searchCriteriaMap.put(DslQueryHelper.PROJECTION_DSL, GLOBAL.RULES.exactToken());

        // DslqQueryHelper Exceptions : InvalidParseOperationException,
        // InvalidCreateOperationException
        when(dslQueryHelper.createGetByIdDSLSelectMultipleQuery(searchCriteriaMap)).thenThrow(
            InvalidParseOperationException.class,
            InvalidCreateOperationException.class
        );

        given()
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .cookie(COOKIE)
            .param("id", "1")
            .expect()
            .statusCode(Status.BAD_REQUEST.getStatusCode())
            .when()
            .get("/archivesearch/unit/1");
    }

    @Test
    public void testArchiveUnitDetailsAccessExternalClientServerException() throws Exception {
        final Map<String, String> searchCriteriaMap = new HashMap<>();
        searchCriteriaMap.put(UiConstants.SELECT_BY_ID.toString(), "1");
        searchCriteriaMap.put(DslQueryHelper.PROJECTION_DSL, GLOBAL.RULES.exactToken());

        final JsonNode preparedDslQuery = JsonHandler.createObjectNode();

        when(dslQueryHelper.createSelectDSLQuery(searchCriteriaMap)).thenReturn(preparedDslQuery);

        when(userInterfaceTransactionManager.getArchiveUnitDetails(any(), any(), any())).thenThrow(
            VitamClientException.class
        );

        given()
            .param("id", "1")
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_NAME)
            .cookie(COOKIE)
            .expect()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
            .when()
            .get("/archivesearch/unit/1");
    }

    @Test
    public void testArchiveUnitDetailsNotFoundError() throws Exception {
        final Map<String, String> searchCriteriaMap = new HashMap<>();
        searchCriteriaMap.put(DslQueryHelper.PROJECTION_DSL, GLOBAL.RULES.exactToken());

        final JsonNode preparedDslQuery = JsonHandler.createObjectNode();

        when(dslQueryHelper.createGetByIdDSLSelectMultipleQuery(searchCriteriaMap)).thenReturn(preparedDslQuery);

        when(userInterfaceTransactionManager.getArchiveUnitDetails(any(), any(), any())).thenReturn(
            new VitamError<JsonNode>("vitam_code").setHttpCode(Status.NOT_FOUND.getStatusCode())
        );

        given()
            .param("id", "1")
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .cookie(COOKIE)
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_NAME)
            .expect()
            .statusCode(Status.NOT_FOUND.getStatusCode())
            .when()
            .get("/archivesearch/unit/1");
    }

    @Test
    public void testArchiveUnitDetailsRemainingExceptions() throws Exception {
        final Map<String, String> searchCriteriaMap = new HashMap<>();
        searchCriteriaMap.put(UiConstants.SELECT_BY_ID.toString(), "1");
        searchCriteriaMap.put(DslQueryHelper.PROJECTION_DSL, GLOBAL.RULES.exactToken());
        final JsonNode preparedDslQuery = JsonHandler.createObjectNode();

        when(dslQueryHelper.createSelectDSLQuery(searchCriteriaMap)).thenReturn(preparedDslQuery);

        // All exceptions
        when(userInterfaceTransactionManager.getArchiveUnitDetails(any(), any(), any())).thenThrow(
            VitamClientException.class
        );

        given()
            .param("id", "1")
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_NAME)
            .cookie(COOKIE)
            .expect()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
            .when()
            .get("/archivesearch/unit/1");
    }

    /**
     * Update Unit Treatment
     */
    @Test
    public void testUpdateArchiveUnitWithoutBody() {
        given()
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .cookie(COOKIE)
            .contentType(ContentType.JSON)
            .expect()
            .statusCode(Status.BAD_REQUEST.getStatusCode())
            .when()
            .post("/archiveupdate/units/1");
    }

    @Test
    public void testUpdateArchiveUnitDetailsDsl()
        throws InvalidParseOperationException, InvalidCreateOperationException {
        final Map<String, JsonNode> updateCriteriaMap = new HashMap<>();
        updateCriteriaMap.put(UiConstants.SELECT_BY_ID.toString(), new TextNode("1"));
        updateCriteriaMap.put("title", new TextNode("archive1"));

        // DslqQueryHelper Exceptions : InvalidParseOperationException,
        // InvalidCreateOperationException
        final Map<String, JsonNode> updateRules = new HashMap<>();

        when(dslQueryHelper.createUpdateByIdDSLQuery(updateCriteriaMap, updateRules)).thenThrow(
            InvalidParseOperationException.class
        );

        given()
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .contentType(ContentType.JSON)
            .body(UPDATE)
            .cookie(COOKIE)
            .expect()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
            .when()
            .post("/archiveupdate/units/1");
    }

    @Test
    public void testUploadSipOK() throws Exception {
        final RequestResponse<?> mockResponse = mock(RequestResponse.class);
        final IngestExternalClient ingestClient = mock(IngestExternalClient.class);
        when(ingestExternalClientFactory.getClient()).thenReturn(ingestClient);

        doReturn("Atr").when(mockResponse).getHeaderString(any());
        doReturn(mockResponse).when(ingestClient).ingest(any(), any(), any(), any());

        final InputStream stream = PropertiesUtils.getResourceAsStream("SIP.zip");
        // Need for test
        IOUtils.toByteArray(stream);

        final ResponseBody<?> s = given()
            .headers(WebApplicationResource.X_CHUNK_OFFSET, "1", WebApplicationResource.X_SIZE_TOTAL, "1")
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .contentType(ContentType.BINARY)
            .config(
                RestAssured.config()
                    .encoderConfig(
                        EncoderConfig.encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false)
                    )
            )
            .body(stream)
            .cookie(COOKIE)
            .expect()
            .statusCode(Status.OK.getStatusCode())
            .when()
            .post("/ingest/upload")
            .getBody();

        final JsonNode firstRequestId = JsonHandler.getFromString(s.asString());
        assertNotNull(firstRequestId.get(GlobalDataRest.X_REQUEST_ID.toLowerCase()).asText());
    }

    @Test
    public void testUploadSipMultipleChunkOK() throws Exception {
        final RequestResponse<?> mockResponse = mock(RequestResponse.class);
        final IngestExternalClient ingestClient = mock(IngestExternalClient.class);
        when(ingestExternalClientFactory.getClient()).thenReturn(ingestClient);

        doReturn("Atr").when(mockResponse).getHeaderString(any());
        doReturn(mockResponse).when(ingestClient).ingest(any(), any(), any(), any());

        final InputStream stream = PropertiesUtils.getResourceAsStream("SIP.zip");
        // Need for test
        byte[] content = IOUtils.toByteArray(stream);

        InputStream stream1 = new ByteArrayInputStream(content, 0, 1048576);
        InputStream stream2 = new ByteArrayInputStream(content, 1048576, 2097152);
        InputStream stream3 = new ByteArrayInputStream(content, 2097152, 3145728);
        InputStream stream4 = new ByteArrayInputStream(content, 3145728, 4194304);
        InputStream stream5 = new ByteArrayInputStream(content, 4194304, 5000000);
        final ResponseBody<?> s1 = given()
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .headers(WebApplicationResource.X_SIZE_TOTAL, "5000000", WebApplicationResource.X_CHUNK_OFFSET, "0")
            .contentType(ContentType.BINARY)
            .config(
                RestAssured.config()
                    .encoderConfig(
                        EncoderConfig.encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false)
                    )
            )
            .body(stream1)
            .cookie(COOKIE)
            .expect()
            .statusCode(Status.OK.getStatusCode())
            .when()
            .post("/ingest/upload")
            .getBody();
        final JsonNode firstRequestId = JsonHandler.getFromString(s1.asString());
        assertNotNull(firstRequestId.get(GlobalDataRest.X_REQUEST_ID.toLowerCase()).asText());
        String reqId = firstRequestId.get(GlobalDataRest.X_REQUEST_ID.toLowerCase()).asText();
        File temporarySipFile = PropertiesUtils.fileFromTmpFolder(reqId);
        given()
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .cookie(COOKIE)
            .headers(
                WebApplicationResource.X_SIZE_TOTAL,
                "5000000",
                WebApplicationResource.X_CHUNK_OFFSET,
                "1048576",
                GlobalDataRest.X_REQUEST_ID,
                reqId
            )
            .contentType(ContentType.BINARY)
            .config(
                RestAssured.config()
                    .encoderConfig(
                        EncoderConfig.encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false)
                    )
            )
            .body(stream2)
            .expect()
            .statusCode(Status.OK.getStatusCode())
            .when()
            .post("/ingest/upload")
            .getBody();
        given()
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .cookie(COOKIE)
            .headers(
                WebApplicationResource.X_SIZE_TOTAL,
                "5000000",
                WebApplicationResource.X_CHUNK_OFFSET,
                "2097152",
                GlobalDataRest.X_REQUEST_ID,
                reqId
            )
            .contentType(ContentType.BINARY)
            .config(
                RestAssured.config()
                    .encoderConfig(
                        EncoderConfig.encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false)
                    )
            )
            .body(stream3)
            .expect()
            .statusCode(Status.OK.getStatusCode())
            .when()
            .post("/ingest/upload")
            .getBody();

        given()
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .cookie(COOKIE)
            .headers(
                WebApplicationResource.X_SIZE_TOTAL,
                "5000000",
                WebApplicationResource.X_CHUNK_OFFSET,
                "3145728",
                GlobalDataRest.X_REQUEST_ID,
                reqId
            )
            .contentType(ContentType.BINARY)
            .config(
                RestAssured.config()
                    .encoderConfig(
                        EncoderConfig.encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false)
                    )
            )
            .body(stream4)
            .expect()
            .statusCode(Status.OK.getStatusCode())
            .when()
            .post("/ingest/upload")
            .getBody();
        given()
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .cookie(COOKIE)
            .headers(
                WebApplicationResource.X_SIZE_TOTAL,
                "5000000",
                WebApplicationResource.X_CHUNK_OFFSET,
                "4194304",
                GlobalDataRest.X_REQUEST_ID,
                reqId
            )
            .contentType(ContentType.BINARY)
            .config(
                RestAssured.config()
                    .encoderConfig(
                        EncoderConfig.encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false)
                    )
            )
            .body(stream5)
            .expect()
            .statusCode(Status.OK.getStatusCode())
            .when()
            .post("/ingest/upload")
            .getBody();
        // Cannot check uploaded file for certain since it might be already deleted
        try {
            byte[] finalContent = IOUtils.toByteArray(new FileInputStream(temporarySipFile));
            assertArrayEquals(content, finalContent);
        } catch (IOException e) {
            // Ignore since file wad deleted before test
        }
    }

    @Test
    public void givenReferentialWrongFormatWhenUploadThenThrowReferentialException() throws Exception {
        final AdminExternalClient adminManagementClient = mock(AdminExternalClient.class);
        doThrow(new AccessExternalClientException("")).when(adminManagementClient).createFormats(any(), any(), any());
        when(adminExternalClientFactory.getClient()).thenReturn(adminManagementClient);

        final InputStream stream = PropertiesUtils.getResourceAsStream("FF-vitam-ko.fake");
        // Need for test
        IOUtils.toByteArray(stream);

        given()
            .contentType(ContentType.BINARY)
            .config(
                RestAssured.config()
                    .encoderConfig(
                        EncoderConfig.encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false)
                    )
            )
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .body(stream)
            .cookie(COOKIE)
            .expect()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
            .when()
            .post("/format/upload");
    }

    @Test
    public void testFormatUploadOK() throws Exception {
        final AdminExternalClient adminManagementClient = mock(AdminExternalClient.class);
        doReturn(new RequestResponseOK<JsonNode>().setHttpCode(Status.OK.getStatusCode()))
            .when(adminManagementClient)
            .createFormats(any(), any(), any());
        when(adminExternalClientFactory.getClient()).thenReturn(adminManagementClient);

        final InputStream stream = PropertiesUtils.getResourceAsStream("DROID_SignatureFile_V94.xml");
        // Need for test
        IOUtils.toByteArray(stream);

        given()
            .contentType(ContentType.BINARY)
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .header(GlobalDataRest.X_FILENAME, "DROID_SignatureFile_V94.xml")
            .config(
                RestAssured.config()
                    .encoderConfig(
                        EncoderConfig.encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false)
                    )
            )
            .body(stream)
            .cookie(COOKIE)
            .expect()
            .statusCode(Status.OK.getStatusCode())
            .when()
            .post("/format/upload");
    }

    @Test
    public void testUploadSipError() throws Exception {
        final IngestExternalClient ingestClient = mock(IngestExternalClient.class);
        doThrow(new IngestExternalException("IngestExternalException"))
            .when(ingestClient)
            .ingest(any(), any(), any(), any());
        when(ingestExternalClientFactory.getClient()).thenReturn(ingestClient);

        final InputStream stream = PropertiesUtils.getResourceAsStream("SIP.zip");
        // Need for test
        IOUtils.toByteArray(stream);

        given()
            .headers(GlobalDataRest.X_REQUEST_ID, "no_req_id")
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .contentType(ContentType.BINARY)
            .config(
                RestAssured.config()
                    .encoderConfig(
                        EncoderConfig.encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false)
                    )
            )
            .body(stream)
            .cookie(COOKIE)
            .expect()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
            .when()
            .post("/ingest/upload");
    }

    @Test
    public void testSearchFormatOK() throws Exception {
        final AdminExternalClient adminClient = mock(AdminExternalClient.class);
        doReturn(ClientMockResultHelper.getFormatList()).when(adminClient).findFormats(any(), any());
        when(dslQueryHelper.createSingleQueryDSL(any())).thenReturn(JsonHandler.getFromString(OPTIONS));

        when(adminExternalClientFactory.getClient()).thenReturn(adminClient);
        doNothing().when(paginationHelper).setResult(anyString(), any());
        when(paginationHelper.getResult(any(JsonNode.class), any())).thenReturn(JsonHandler.createObjectNode());

        given()
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .contentType(ContentType.JSON)
            .body(OPTIONS)
            .cookie(COOKIE)
            .expect()
            .statusCode(Status.OK.getStatusCode())
            .when()
            .post("/admin/formats");
    }

    @Test
    public void testSearchFormatBadRequest() throws Exception {
        final AdminExternalClient adminClient = mock(AdminExternalClient.class);
        when(dslQueryHelper.createSingleQueryDSL(any())).thenThrow(new InvalidParseOperationException(""));

        when(adminExternalClientFactory.getClient()).thenReturn(adminClient);

        doNothing().when(paginationHelper).setResult(anyString(), any());
        when(paginationHelper.getResult(any(JsonNode.class), any())).thenReturn(JsonHandler.createObjectNode());

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .body(OPTIONS)
            .cookie(COOKIE)
            .expect()
            .statusCode(Status.BAD_REQUEST.getStatusCode())
            .when()
            .post("/admin/formats");
    }

    @Test
    public void testSearchFormatNotFound() throws Exception {
        final AdminExternalClient adminClient = mock(AdminExternalClient.class);
        doThrow(new VitamClientException("")).when(adminClient).findFormats(any(), any());
        when(dslQueryHelper.createSingleQueryDSL(any())).thenReturn(JsonHandler.getFromString(OPTIONS));

        when(adminExternalClientFactory.getClient()).thenReturn(adminClient);

        doNothing().when(paginationHelper).setResult(anyString(), any());
        when(paginationHelper.getResult(any(JsonNode.class), any())).thenReturn(JsonHandler.createObjectNode());

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .body(OPTIONS)
            .cookie(COOKIE)
            .expect()
            .statusCode(Status.NOT_FOUND.getStatusCode())
            .when()
            .post("/admin/formats");
    }

    @Test
    public void testSearchFormatByIdOK() throws Exception {
        final AdminExternalClient adminClient = mock(AdminExternalClient.class);
        when(adminExternalClientFactory.getClient()).thenReturn(adminClient);
        doReturn(ClientMockResultHelper.getFormat()).when(adminClient).findFormatById(any(), any());

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .body(OPTIONS)
            .cookie(COOKIE)
            .expect()
            .statusCode(Status.OK.getStatusCode())
            .when()
            .post("/admin/formats/1");
    }

    @Test
    public void testSearchFormatByIdNotFound() throws Exception {
        final AdminExternalClient adminClient = mock(AdminExternalClient.class);
        when(adminExternalClientFactory.getClient()).thenReturn(adminClient);
        doThrow(new VitamClientException("VitamClientException")).when(adminClient).findFormatById(any(), any());

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .body(OPTIONS)
            .cookie(COOKIE)
            .expect()
            .statusCode(Status.NOT_FOUND.getStatusCode())
            .when()
            .post("/admin/formats/1");
    }

    @Test
    public void testCheckFormatOK() throws Exception {
        final AdminExternalClient adminClient = mock(AdminExternalClient.class);
        when(adminExternalClientFactory.getClient()).thenReturn(adminClient);
        when(adminClient.checkFormats(any(), any())).thenReturn(ok().build());
        when(dslQueryHelper.createSingleQueryDSL(any())).thenReturn(JsonHandler.getFromString(OPTIONS));

        final InputStream stream = PropertiesUtils.getResourceAsStream("FF-vitam-ko.fake");
        // Need for test
        IOUtils.toByteArray(stream);

        given()
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .contentType(ContentType.BINARY)
            .config(
                RestAssured.config()
                    .encoderConfig(
                        EncoderConfig.encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false)
                    )
            )
            .body(stream)
            .cookie(COOKIE)
            .expect()
            .statusCode(Status.OK.getStatusCode())
            .when()
            .post("/format/check");
    }

    @Test
    public void testNotFoundGetArchiveObjectGroup() throws Exception {
        VitamError<JsonNode> vitamError = new VitamError<JsonNode>(
            VitamCode.ACCESS_EXTERNAL_SELECT_OBJECT_BY_ID_ERROR.getItem()
        )
            .setMessage(VitamCode.ACCESS_EXTERNAL_SELECT_OBJECT_BY_ID_ERROR.getMessage())
            .setState(StatusCode.KO.name())
            .setContext(ServiceName.EXTERNAL_ACCESS.getName())
            .setDescription(VitamCode.ACCESS_EXTERNAL_SELECT_OBJECT_BY_ID_ERROR.getMessage())
            .setHttpCode(Status.NOT_FOUND.getStatusCode())
            .setDescription(
                VitamCode.ACCESS_EXTERNAL_SELECT_OBJECT_BY_ID_ERROR.getMessage() +
                " Cause : " +
                Status.NOT_FOUND.getReasonPhrase()
            );
        when(userInterfaceTransactionManager.selectObjectbyId(any(), any(), any())).thenReturn(vitamError);
        given()
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .cookie(COOKIE)
            .expect()
            .statusCode(Status.NOT_FOUND.getStatusCode())
            .when()
            .get("/archiveunit/objects/idOG");
    }

    @Test
    public void testOKGetArchiveObjectGroup() throws Exception {
        JsonNode node = JsonHandler.getFromFile(PropertiesUtils.findFile("sample_objectGroup_document.json"));
        final RequestResponseOK<JsonNode> sampleObjectGroup = RequestResponseOK.getFromJsonNode(node);
        sampleObjectGroup.setHttpCode(Status.OK.getStatusCode());
        when(userInterfaceTransactionManager.selectObjectbyId(any(), any(), any())).thenReturn(sampleObjectGroup);

        given()
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .cookie(COOKIE)
            .expect()
            .statusCode(Status.OK.getStatusCode())
            .when()
            .get("/archiveunit/objects/idOG");
    }

    @Test
    public void testBadRequestGetArchiveObjectGroup() throws Exception {
        when(dslQueryHelper.createSelectDSLQuery(any())).thenThrow(new InvalidParseOperationException(""));
        when(userInterfaceTransactionManager.selectObjectbyId(any(), any(), any())).thenReturn(
            RequestResponseOK.getFromJsonNode(FAKE_JSONNODE_RETURN).setHttpCode(400)
        );

        given()
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .cookie(COOKIE)
            .expect()
            .statusCode(Status.BAD_REQUEST.getStatusCode())
            .when()
            .get("/archiveunit/objects/idOG");
    }

    @Test
    public void testInternalServerErrorGetArchiveObjectGroup() throws Exception {
        when(userInterfaceTransactionManager.selectObjectbyId(any(), any(), any())).thenThrow(
            new VitamClientException("")
        );

        given()
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .cookie(COOKIE)
            .expect()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
            .when()
            .get("/archiveunit/objects/idOG");
    }

    @Test
    public void testUnknownErrorGetArchiveObjectGroup() throws Exception {
        when(userInterfaceTransactionManager.selectObjectbyId(any(), any(), any())).thenThrow(
            new NullPointerException("")
        );

        given()
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .cookie(COOKIE)
            .expect()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
            .when()
            .get("/archiveunit/objects/idOG");
    }

    @Test
    public void testVitamExceptionGetObjectAsInputStream() throws Exception {
        when(
            userInterfaceTransactionManager.getObjectAsInputStream(
                any(),
                anyString(),
                anyString(),
                anyInt(),
                anyString(),
                any(),
                any()
            )
        ).thenThrow(new VitamClientException(""));

        given()
            .accept(MediaType.APPLICATION_OCTET_STREAM)
            .body(OPTIONS_DOWNLOAD)
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .cookie(COOKIE)
            .expect()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
            .when()
            .get(
                "/archiveunit/objects/download/idOG?usage=BinaryMaster_1&version=0&filename=Vitam-Sensibilisation-API-V1.0.odp&tenantId=0"
            );
    }

    @Test
    public void testOKGetObjectAsInputStream() throws Exception {
        ArgumentCaptor<AsyncResponse> argumentCaptor = ArgumentCaptor.forClass(AsyncResponse.class);
        when(
            userInterfaceTransactionManager.getObjectAsInputStream(
                argumentCaptor.capture(),
                anyString(),
                anyString(),
                anyInt(),
                anyString(),
                any(),
                any()
            )
        ).then(o -> {
            argumentCaptor.getValue().resume(ok().build());
            return true;
        });

        given()
            .accept(MediaType.APPLICATION_OCTET_STREAM)
            .expect()
            .statusCode(Status.OK.getStatusCode())
            .when()
            .get(
                "/archiveunit/objects/download/idOG?usage=Dissamination_1&version=1&filename=Vitam-Sensibilisation-API-V1.0.odp"
            );
    }

    @Test
    public void testNotFoundGetObjectAsInputStream() throws Exception {
        ArgumentCaptor<AsyncResponse> argumentCaptor = ArgumentCaptor.forClass(AsyncResponse.class);
        when(
            userInterfaceTransactionManager.getObjectAsInputStream(
                argumentCaptor.capture(),
                anyString(),
                anyString(),
                anyInt(),
                anyString(),
                any(),
                any()
            )
        ).then(o -> {
            argumentCaptor.getValue().resume(status(Status.NOT_FOUND).build());
            return true;
        });

        given()
            .accept(MediaType.APPLICATION_OCTET_STREAM)
            .expect()
            .statusCode(Status.NOT_FOUND.getStatusCode())
            .when()
            .get(
                "/archiveunit/objects/download/idOG?usage=Dissamination_1&version=1&filename=Vitam-Sensibilisation-API-V1.0.odp"
            );
    }

    @Test
    public void testBadRequestGetObjectAsInputStream() throws Exception {
        when(
            userInterfaceTransactionManager.getObjectAsInputStream(
                any(),
                anyString(),
                anyString(),
                anyInt(),
                anyString(),
                any(),
                any()
            )
        ).thenReturn(true);
        given()
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .cookie(COOKIE)
            .accept(MediaType.APPLICATION_OCTET_STREAM)
            .expect()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
            .when()
            .get(
                "/archiveunit/objects/download/idOG?usage=Dissemination&filename=Vitam-Sensibilisation-API" +
                "-V1.0.odp&tenantId=0"
            );
    }

    @Test
    public void testAccessUnknownExceptionGetObjectAsInputStream() throws Exception {
        when(
            userInterfaceTransactionManager.getObjectAsInputStream(
                any(),
                anyString(),
                anyString(),
                anyInt(),
                anyString(),
                any(),
                any()
            )
        ).thenThrow(new NullPointerException());
        given()
            .accept(MediaType.APPLICATION_OCTET_STREAM)
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .cookie(COOKIE)
            .expect()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
            .when()
            .get(
                "/archiveunit/objects/download/idOG?usage=BinaryMaster_1&version=0&filename=Vitam-Sensibilisation-API-V1.0.odp"
            );
    }

    @Test
    public void testUnitTreeOk() throws VitamException {
        when(userInterfaceTransactionManager.searchUnits(any(), any())).thenReturn(
            RequestResponseOK.getFromJsonNode(FAKE_JSONNODE_RETURN)
        );

        given()
            .contentType(ContentType.JSON)
            .body(TREE_QUERY)
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .cookie(COOKIE)
            .expect()
            .statusCode(Status.OK.getStatusCode())
            .when()
            .post("/archiveunit/tree");
    }

    @Test
    public void testUnitTreeWithAccessExternalClientServerException() throws Exception {
        when(userInterfaceTransactionManager.searchUnits(any(), any())).thenThrow(VitamClientException.class);

        given()
            .contentType(ContentType.JSON)
            .body(TREE_QUERY)
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .cookie(COOKIE)
            .expect()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
            .when()
            .post("/archiveunit/tree");
    }

    @Test
    public void testUnitTreeWithAccessExternalClientNotFoundException() throws Exception {
        when(userInterfaceTransactionManager.searchUnits(any(), any())).thenThrow(VitamClientException.class);

        given()
            .contentType(ContentType.JSON)
            .body(TREE_QUERY)
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .cookie(COOKIE)
            .expect()
            .statusCode(Status.NOT_FOUND.getStatusCode())
            .when()
            .post("/archiveunit/tree/1");
    }

    /**
     * rules Management
     ********/

    @Test
    public void givenReferentialWrongFormatRulesWhenUploadThenThrowReferentialException() throws Exception {
        final AdminExternalClient adminManagementClient = mock(AdminExternalClient.class);
        when(adminExternalClientFactory.getClient()).thenReturn(adminManagementClient);

        doThrow(new AccessExternalClientException("")).when(adminManagementClient).createRules(any(), any(), any());
        final InputStream stream = PropertiesUtils.getResourceAsStream("jeu_donnees_KO_regles_CSV_Parameters.csv");
        // Need for test
        IOUtils.toByteArray(stream);

        given()
            .contentType(ContentType.BINARY)
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .config(
                RestAssured.config()
                    .encoderConfig(
                        EncoderConfig.encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false)
                    )
            )
            .body(stream)
            .cookie(COOKIE)
            .expect()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
            .when()
            .post("/rules/upload");
    }

    @Test
    public void testRuleUploadOK() throws Exception {
        final AdminExternalClient adminManagementClient = mock(AdminExternalClient.class);
        when(adminExternalClientFactory.getClient()).thenReturn(adminManagementClient);

        doReturn(new RequestResponseOK<JsonNode>().setHttpCode(Status.OK.getStatusCode()))
            .when(adminManagementClient)
            .createRules(any(), any(), any());

        final InputStream stream = PropertiesUtils.getResourceAsStream("jeu_donnees_OK_regles_CSV.csv");
        // Need for test
        IOUtils.toByteArray(stream);

        given()
            .contentType(ContentType.BINARY)
            .header(GlobalDataRest.X_FILENAME, "jeu_donnees_OK_regles_CSV.csv")
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .config(
                RestAssured.config()
                    .encoderConfig(
                        EncoderConfig.encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false)
                    )
            )
            .body(stream)
            .cookie(COOKIE)
            .expect()
            .statusCode(Status.OK.getStatusCode())
            .when()
            .post("/rules/upload");
    }

    @Test
    public void testSearchRulesOK() throws Exception {
        final AdminExternalClient adminManagementClient = mock(AdminExternalClient.class);
        when(adminExternalClientFactory.getClient()).thenReturn(adminManagementClient);
        doReturn(ClientMockResultHelper.getRuleList()).when(adminManagementClient).findRules(any(), any());
        when(dslQueryHelper.createSingleQueryDSL(any())).thenReturn(JsonHandler.getFromString(OPTIONS));

        doNothing().when(paginationHelper).setResult(anyString(), any());
        when(paginationHelper.getResult(any(JsonNode.class), any())).thenReturn(JsonHandler.createObjectNode());

        given()
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .contentType(ContentType.JSON)
            .body(OPTIONS)
            .cookie(COOKIE)
            .expect()
            .statusCode(Status.OK.getStatusCode())
            .when()
            .post("/admin/rules");
    }

    @Test
    public void testSearchRuleBadRequest() throws Exception {
        final AdminExternalClient adminManagementClient = mock(AdminExternalClient.class);
        when(adminExternalClientFactory.getClient()).thenReturn(adminManagementClient);
        doReturn(ClientMockResultHelper.getRuleList()).when(adminManagementClient).findRules(any(), any());
        when(dslQueryHelper.createSingleQueryDSL(any())).thenThrow(new InvalidParseOperationException(""));

        doNothing().when(paginationHelper).setResult(anyString(), any());
        when(paginationHelper.getResult(any(JsonNode.class), any())).thenReturn(JsonHandler.createObjectNode());

        given()
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .contentType(ContentType.JSON)
            .body(OPTIONS)
            .cookie(COOKIE)
            .expect()
            .statusCode(Status.BAD_REQUEST.getStatusCode())
            .when()
            .post("/admin/rules");
    }

    @Test
    public void testSearchRuleNotFound() throws Exception {
        final AdminExternalClient adminManagementClient = mock(AdminExternalClient.class);
        when(adminExternalClientFactory.getClient()).thenReturn(adminManagementClient);
        doThrow(new VitamClientException("")).when(adminManagementClient).findRules(any(), any());
        when(dslQueryHelper.createSingleQueryDSL(any())).thenReturn(JsonHandler.getFromString(OPTIONS));

        doNothing().when(paginationHelper).setResult(anyString(), any());
        when(paginationHelper.getResult(any(JsonNode.class), any())).thenReturn(JsonHandler.createObjectNode());

        given()
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .contentType(ContentType.JSON)
            .body(OPTIONS)
            .cookie(COOKIE)
            .expect()
            .statusCode(Status.OK.getStatusCode())
            .when()
            .post("/admin/rules");
    }

    @Test
    public void testSearchRuleByIdNotFound() throws Exception {
        final AdminExternalClient adminManagementClient = mock(AdminExternalClient.class);
        when(adminExternalClientFactory.getClient()).thenReturn(adminManagementClient);
        doThrow(new VitamClientException("VitamClientException"))
            .when(adminManagementClient)
            .findRuleById(any(), any());

        given()
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .contentType(ContentType.JSON)
            .body(OPTIONS)
            .cookie(COOKIE)
            .expect()
            .statusCode(Status.NOT_FOUND.getStatusCode())
            .when()
            .post("/admin/rules/1");
    }

    @Test
    public void testCheckRulesFileOK() throws Exception {
        final AdminExternalClient adminManagementClient = mock(AdminExternalClient.class);
        when(adminExternalClientFactory.getClient()).thenReturn(adminManagementClient);
        when(adminManagementClient.checkRules(any(), any())).thenReturn(ClientMockResultHelper.getObjectStream());
        when(dslQueryHelper.createSingleQueryDSL(any())).thenReturn(JsonHandler.getFromString(OPTIONS));

        final InputStream stream = PropertiesUtils.getResourceAsStream("jeu_donnees_KO_regles_CSV_Parameters.csv");

        // Need for test
        IOUtils.toByteArray(stream);
        final io.restassured.response.Response response = given()
            .contentType(ContentType.BINARY)
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .cookie(COOKIE)
            .config(
                RestAssured.config()
                    .encoderConfig(
                        EncoderConfig.encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false)
                    )
            )
            .body(stream)
            .post("/rules/check");
        assertEquals(Status.OK.getStatusCode(), response.getStatusCode());
    }

    @Test
    public void testGetUnitLifeCycleByIdOk() throws Exception {
        final RequestResponseOK<LogbookLifecycle> result = RequestResponseOK.getFromJsonNode(
            FAKE_JSONNODE_RETURN,
            LogbookLifecycle.class
        );
        when(userInterfaceTransactionManager.selectUnitLifeCycleById(any(), any())).thenReturn(result);

        given()
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .cookie(COOKIE)
            .param("id_lc", FAKE_UNIT_LF_ID)
            .expect()
            .statusCode(Status.OK.getStatusCode())
            .when()
            .get("/logbookunitlifecycles/" + FAKE_UNIT_LF_ID);
    }

    @Test
    public void testGetObjectGroupLifeCycleByIdOk() throws Exception {
        final RequestResponseOK<LogbookLifecycle> result = RequestResponseOK.getFromJsonNode(
            FAKE_JSONNODE_RETURN,
            LogbookLifecycle.class
        );
        when(userInterfaceTransactionManager.selectObjectGroupLifeCycleById(any(), any())).thenReturn(result);

        given()
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .cookie(COOKIE)
            .param("id_lc", FAKE_OBG_LF_ID)
            .expect()
            .statusCode(Status.OK.getStatusCode())
            .when()
            .get("/logbookobjectslifecycles/" + FAKE_OBG_LF_ID);
    }

    @Test
    public void testGetUnitLifeCycleByIdWithBadRequestWhenVitamClientException() throws Exception {
        when(userInterfaceTransactionManager.selectUnitLifeCycleById(any(), any())).thenThrow(
            VitamClientException.class
        );

        given()
            .param("id_lc", FAKE_UNIT_LF_ID)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .cookie(COOKIE)
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_NAME)
            .expect()
            .statusCode(Status.NOT_FOUND.getStatusCode())
            .when()
            .get("/logbookunitlifecycles/" + FAKE_UNIT_LF_ID);
    }

    @Test
    public void testGetUnitLifeCycleByIdWithInternalServerErrorWhenUnknownException() throws Exception {
        when(userInterfaceTransactionManager.selectUnitLifeCycleById(any(), any())).thenThrow(
            NullPointerException.class
        );

        given()
            .param("id_lc", FAKE_UNIT_LF_ID)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .cookie(COOKIE)
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_NAME)
            .expect()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
            .when()
            .get("/logbookunitlifecycles/" + FAKE_UNIT_LF_ID);
    }

    @Test
    public void testGetObjectGroupLifeCycleByIdWithBadRequestWhenVitamClientException() throws Exception {
        when(userInterfaceTransactionManager.selectObjectGroupLifeCycleById(any(), any())).thenThrow(
            VitamClientException.class
        );

        given()
            .param("id_lc", FAKE_OBG_LF_ID)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .cookie(COOKIE)
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_NAME)
            .expect()
            .statusCode(Status.NOT_FOUND.getStatusCode())
            .when()
            .get("/logbookobjectslifecycles/" + FAKE_OBG_LF_ID);
    }

    @Test
    public void testGetOjectGroupLifeCycleByIdWithInternalServerErrorWhenUnknownException() throws Exception {
        when(userInterfaceTransactionManager.selectObjectGroupLifeCycleById(any(), any())).thenThrow(
            NullPointerException.class
        );

        given()
            .param("id_lc", FAKE_OBG_LF_ID)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .cookie(COOKIE)
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_NAME)
            .expect()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
            .when()
            .get("/logbookobjectslifecycles/" + FAKE_OBG_LF_ID);
    }

    @Test
    public void testSearchFundsRegisterOK() throws Exception {
        when(userInterfaceTransactionManager.findAccessionRegisterSummary(any(), any())).thenReturn(
            ClientMockResultHelper.getAccessionRegisterSummary()
        );

        doNothing().when(paginationHelper).setResult(anyString(), any());
        when(paginationHelper.getResult(any(JsonNode.class), any())).thenReturn(JsonHandler.createObjectNode());

        given()
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .contentType(ContentType.JSON)
            .body(OPTIONS)
            .cookie(COOKIE)
            .expect()
            .statusCode(Status.OK.getStatusCode())
            .when()
            .post("/admin/accession-register");
    }

    @Test
    public void testSerachFundsRegisterNotFound() throws Exception {
        VitamError<AccessionRegisterSummaryModel> vitamError = new VitamError<AccessionRegisterSummaryModel>(
            VitamCode.ADMIN_EXTERNAL_FIND_DOCUMENT_BY_ID_ERROR.getItem()
        )
            .setMessage(VitamCode.ADMIN_EXTERNAL_FIND_DOCUMENT_BY_ID_ERROR.getMessage())
            .setState(StatusCode.KO.name())
            .setContext(ADMIN_EXTERNAL_MODULE)
            .setDescription(VitamCode.ADMIN_EXTERNAL_FIND_DOCUMENT_BY_ID_ERROR.getMessage())
            .setHttpCode(Status.NOT_FOUND.getStatusCode())
            .setDescription(
                VitamCode.ADMIN_EXTERNAL_FIND_DOCUMENT_ERROR.getMessage() +
                " Cause : " +
                Status.NOT_FOUND.getReasonPhrase()
            );

        when(userInterfaceTransactionManager.findAccessionRegisterSummary(any(), any())).thenReturn(vitamError);

        doNothing().when(paginationHelper).setResult(anyString(), any());
        when(paginationHelper.getResult(any(JsonNode.class), any())).thenReturn(JsonHandler.createObjectNode());

        given()
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .contentType(ContentType.JSON)
            .body(OPTIONS)
            .cookie(COOKIE)
            .expect()
            .statusCode(Status.NOT_FOUND.getStatusCode())
            .when()
            .post("/admin/accession-register");
    }

    @Test
    public void testSearchFundsRegisterBadRequest() throws Exception {
        when(userInterfaceTransactionManager.findAccessionRegisterSummary(any(), any())).thenThrow(
            new InvalidParseOperationException("")
        );

        doNothing().when(paginationHelper).setResult(anyString(), any());
        when(paginationHelper.getResult(any(JsonNode.class), any())).thenReturn(JsonHandler.createObjectNode());

        given()
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .contentType(ContentType.JSON)
            .body(OPTIONS)
            .cookie(COOKIE)
            .expect()
            .statusCode(Status.BAD_REQUEST.getStatusCode())
            .when()
            .post("/admin/accession-register");
    }

    @Test
    public void testGetAccessionRegisterDetailOK() throws Exception {
        when(userInterfaceTransactionManager.findAccessionRegisterDetail(any(), any(), any())).thenReturn(
            ClientMockResultHelper.getAccessionRegisterDetail()
        );
        when(paginationHelper.getResult(any(JsonNode.class), any())).thenReturn(JsonHandler.createObjectNode());

        given()
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .cookie(COOKIE)
            .contentType(ContentType.JSON)
            .body(OPTIONS)
            .expect()
            .statusCode(Status.OK.getStatusCode())
            .when()
            .post("/admin/accession-register/1/accession-register-detail");
    }

    @Test
    public void testGetAccessionRegisterDetailBadRequest() throws Exception {
        when(userInterfaceTransactionManager.findAccessionRegisterDetail(any(), any(), any())).thenThrow(
            new InvalidParseOperationException("")
        );

        given()
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .cookie(COOKIE)
            .contentType(ContentType.JSON)
            .body(OPTIONS)
            .expect()
            .statusCode(Status.BAD_REQUEST.getStatusCode())
            .when()
            .post("/admin/accession-register/1/accession-register-detail");
    }

    @Test
    public void downloadObjects() throws Exception {
        final IngestExternalClient ingestClient = mock(IngestExternalClient.class);
        when(ingestExternalClientFactory.getClient()).thenReturn(ingestClient);
        doReturn(ClientMockResultHelper.getObjectStream()).when(ingestClient).downloadObjectAsync(any(), any(), any());

        RestAssured.given()
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .cookie(COOKIE)
            .when()
            .get(INGEST_URI + "/1/" + IngestCollection.MANIFESTS.getCollectionName())
            .then()
            .statusCode(Status.OK.getStatusCode());

        RestAssured.given()
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .cookie(COOKIE)
            .when()
            .get(INGEST_URI + "/1/unknown")
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());

        VitamError<JsonNode> error = VitamCodeHelper.toVitamError(VitamCode.INGEST_EXTERNAL_NOT_FOUND, "NOT FOUND");
        AbstractMockClient.FakeInboundResponse fakeResponse = new AbstractMockClient.FakeInboundResponse(
            Status.NOT_FOUND,
            JsonHandler.writeToInpustream(error),
            MediaType.APPLICATION_OCTET_STREAM_TYPE,
            new MultivaluedHashMap<>()
        );

        doReturn(fakeResponse).when(ingestClient).downloadObjectAsync(any(), any(), any());
        RestAssured.given()
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .cookie(COOKIE)
            .when()
            .get(INGEST_URI + "/1/" + IngestCollection.MANIFESTS.getCollectionName())
            .then()
            .statusCode(Status.NOT_FOUND.getStatusCode());

        Mockito.doThrow(new VitamClientException("")).when(ingestClient).downloadObjectAsync(any(), any(), any());
        RestAssured.given()
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .cookie(COOKIE)
            .when()
            .get(INGEST_URI + "/1/" + IngestCollection.MANIFESTS.getCollectionName())
            .then()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void testCheckTraceabilityOperation() throws Exception {
        // Mock AccessExternal response
        when(userInterfaceTransactionManager.checkTraceabilityOperation(any(), any())).thenReturn(
            ClientMockResultHelper.getLogbooksRequestResponse()
        );

        given()
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .cookie(COOKIE)
            .contentType(ContentType.JSON)
            .body(TRACEABILITY_CHECK_MAP)
            .expect()
            .statusCode(Status.OK.getStatusCode())
            .when()
            .post(TRACEABILITY_CHECK_URL);
    }

    @Test
    public void testDownloadTraceabilityOperation() throws Exception {
        // Mock AccessExternal response
        AdminExternalClient adminExternalClient = mock(AdminExternalClient.class);
        when(adminExternalClientFactory.getClient()).thenReturn(adminExternalClient);

        String contractName = "test_contract";

        when(adminExternalClient.downloadTraceabilityOperationFile(any(), eq("1"))).thenReturn(
            ClientMockResultHelper.getObjectStream()
        );

        RestAssured.given()
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .cookie(COOKIE)
            .when()
            .get("traceability" + "/1/" + "content?contractId=" + contractName)
            .then()
            .statusCode(Status.OK.getStatusCode());
    }

    @Test
    public void testExtractTimestampInformation() throws Exception {
        when(userInterfaceTransactionManager.extractInformationFromTimestamp(any())).thenCallRealMethod();

        final InputStream tokenFile = PropertiesUtils.getResourceAsStream("token.tsp");
        String encodedTimeStampToken = IOUtils.toString(tokenFile, StandardCharsets.UTF_8);
        String timestampExtractMap = "{timestamp: \"" + encodedTimeStampToken + "\"}";
        given()
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .cookie(COOKIE)
            .contentType(ContentType.JSON)
            .body(timestampExtractMap)
            .expect()
            .statusCode(Status.OK.getStatusCode())
            .when()
            .post("/traceability/extractTimestamp");

        timestampExtractMap = "{timestamp: \"FakeTimeStamp\"}";
        given()
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .cookie(COOKIE)
            .contentType(ContentType.JSON)
            .body(timestampExtractMap)
            .expect()
            .statusCode(Status.BAD_REQUEST.getStatusCode())
            .when()
            .post("/traceability/extractTimestamp");

        timestampExtractMap = "{fakeTimestamp: \"FakeTimeStamp\"}";
        given()
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .cookie(COOKIE)
            .contentType(ContentType.JSON)
            .body(timestampExtractMap)
            .expect()
            .statusCode(Status.BAD_REQUEST.getStatusCode())
            .when()
            .post("/traceability/extractTimestamp");
    }

    @Test
    public void testLaunchAudit() throws AccessExternalClientServerException, InvalidParseOperationException {
        AdminExternalClient adminExternalClient = mock(AdminExternalClient.class);
        when(adminExternalClientFactory.getClient()).thenReturn(adminExternalClient);
        JsonNode auditOption = JsonHandler.getFromString(AUDIT_OPTION);
        when(adminExternalClient.launchAudit(any(), any())).thenReturn(
            ClientMockResultHelper.checkOperationTraceability()
        );

        given()
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .cookie(COOKIE)
            .contentType(ContentType.JSON)
            .body(auditOption)
            .expect()
            .when()
            .post("audits")
            .then()
            .statusCode(Status.OK.getStatusCode());
    }

    @Test
    public void testSerViceAgencies() throws Exception {
        AdminExternalClient adminExternalClient = mock(AdminExternalClient.class);
        when(adminExternalClientFactory.getClient()).thenReturn(adminExternalClient);
        when(adminExternalClient.createAgencies(any(), any(), any())).thenReturn(
            new RequestResponseOK<JsonNode>().setHttpCode(Status.OK.getStatusCode())
        );
        when(adminExternalClient.findAgencies(any(), any())).thenReturn(ClientMockResultHelper.getAgenciesList());
        when(adminExternalClient.findAgencyByID(any(), any())).thenReturn(ClientMockResultHelper.getAgency());

        final InputStream stream = PropertiesUtils.getResourceAsStream("FF-vitam-ko.fake");

        // import agencies
        given()
            .contentType(ContentType.BINARY)
            .body(stream)
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .cookie(COOKIE)
            .expect()
            .statusCode(Status.OK.getStatusCode())
            .when()
            .post("/agencies")
            .getBody();

        // find agencies by DSL
        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .body(new Select().getFinalSelect())
            .cookie(COOKIE)
            .expect()
            .statusCode(Status.OK.getStatusCode())
            .when()
            .post("/agencies");

        // find agencies by Id
        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .cookie(COOKIE)
            .expect()
            .statusCode(Status.OK.getStatusCode())
            .when()
            .get("/agencies/id");

        when(adminExternalClient.findAgencyByID(any(), any())).thenReturn(
            VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_NOT_FOUND, "NOT FOUND", AgenciesModel.class)
        );

        // find agencies by Id
        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .cookie(COOKIE)
            .expect()
            .statusCode(Status.NOT_FOUND.getStatusCode())
            .when()
            .get("/agencies/id");

        when(adminExternalClient.findAgencyByID(any(), any())).thenReturn(
            VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST, "BAD REQUEST", AgenciesModel.class)
        );

        // find agencies by Id
        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .cookie(COOKIE)
            .expect()
            .statusCode(Status.BAD_REQUEST.getStatusCode())
            .when()
            .get("/agencies/id");

        when(adminExternalClient.createAgencies(any(), any(), any())).thenThrow(new AccessExternalClientException(""));
        when(adminExternalClient.findAgencies(any(), any())).thenThrow(new VitamClientException(""));
        when(adminExternalClient.findAgencyByID(any(), any())).thenThrow(new VitamClientException(""));
        final InputStream stream2 = PropertiesUtils.getResourceAsStream("FF-vitam-ko.fake");
        // import agencies
        given()
            .contentType(ContentType.BINARY)
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .body(stream2)
            .cookie(COOKIE)
            .expect()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
            .when()
            .post("/agencies")
            .getBody();

        // find agencies by DSL
        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .body(new Select().getFinalSelect())
            .cookie(COOKIE)
            .expect()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
            .when()
            .post("/agencies");

        // find agencies by Id
        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .cookie(COOKIE)
            .expect()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
            .when()
            .get("/agencies/id");
    }

    @Test
    public void testCreateDipOK() throws Exception {
        when(userInterfaceTransactionManager.exportDIP(any(), any())).thenReturn(ClientMockResultHelper.getDipInfo());

        given()
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .cookie(COOKIE)
            .contentType(ContentType.JSON)
            .body(OPTIONS)
            .expect()
            .statusCode(Status.OK.getStatusCode())
            .when()
            .post("/archiveunit/dipexport");
    }

    @Test
    public void testCreateDipBadRequest() throws Exception {
        when(userInterfaceTransactionManager.exportDIP(any(), any())).thenThrow(new VitamClientException(""));

        given()
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .cookie(COOKIE)
            .contentType(ContentType.JSON)
            .body(OPTIONS)
            .expect()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
            .when()
            .post("/archiveunit/dipexport");
    }

    @Test
    public void testGetAdminTenant() {
        final ResponseBody<?> response = given()
            .contentType(ContentType.JSON)
            .expect()
            .statusCode(Status.OK.getStatusCode())
            .when()
            .get("/admintenant")
            .getBody();

        assertEquals("1", response.print());
    }
}
