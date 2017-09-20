/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.ihmdemo.appserver;

import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.config.EncoderConfig;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Cookie;
import com.jayway.restassured.response.Header;
import com.jayway.restassured.response.ResponseBody;
import fr.gouv.vitam.access.external.client.AccessExternalClientFactory;
import fr.gouv.vitam.access.external.client.AdminExternalClient;
import fr.gouv.vitam.access.external.client.AdminExternalClientFactory;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientNotFoundException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientServerException;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.client.ClientMockResultHelper;
import fr.gouv.vitam.common.client.IngestCollection;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.GLOBAL;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.external.client.AbstractMockClient;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.ihmdemo.common.pagination.PaginationHelper;
import fr.gouv.vitam.ihmdemo.common.utils.PermissionReader;
import fr.gouv.vitam.ihmdemo.core.DslQueryHelper;
import fr.gouv.vitam.ihmdemo.core.JsonTransformer;
import fr.gouv.vitam.ihmdemo.core.UiConstants;
import fr.gouv.vitam.ihmdemo.core.UserInterfaceTransactionManager;
import fr.gouv.vitam.ingest.external.api.exception.IngestExternalException;
import fr.gouv.vitam.ingest.external.client.IngestExternalClient;
import fr.gouv.vitam.ingest.external.client.IngestExternalClientFactory;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.jayway.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.net.ssl.*", "javax.management.*"})
@PrepareForTest({PaginationHelper.class, UserInterfaceTransactionManager.class, DslQueryHelper.class,
    IngestExternalClientFactory.class, AdminExternalClientFactory.class, AccessExternalClientFactory.class,
    JsonTransformer.class, WebApplicationConfig.class})
public class WebApplicationResourceTest {

    private static final String DEFAULT_WEB_APP_CONTEXT = "/ihm-demo";
    private static final String DEFAULT_STATIC_CONTENT = "webapp";
    private static final String OPTIONS = "{name: \"myName\"}";
    private static final String AUDIT_OPTION = "{serviceProducteur: \"Service Producteur 1\"}";
    private static final Cookie COOKIE = new Cookie.Builder("JSESSIONID", "testId").build();
    private static final String CREDENTIALS = "{\"token\": {\"principal\": \"myName\", \"credentials\": \"myName\"}}";
    private static final String CREDENTIALS_NO_VALID =
        "{\"token\": {\"principal\": \"myName\", \"credentials\": \"myName\"}}";
    private static final String OPTIONS_DOWNLOAD = "{usage: \"Dissemination\", version: 1}";
    private static final String UPDATE = "{title: \"myarchive\"}";
    private static final String DEFAULT_HOST = "localhost";
    private static final String JETTY_CONFIG = "jetty-config-test.xml";
    private static final String ALL_PARENTS = "[\"P1\", \"P2\", \"P3\"]";
    private static final String FAKE_STRING_RETURN = "{Fake: \"String\"}";
    private static final JsonNode FAKE_JSONNODE_RETURN = JsonHandler.createObjectNode();
    private static final String FAKE_UNIT_LF_ID = "1";
    private static final String FAKE_OBG_LF_ID = "1";
    private static JsonNode sampleLogbookOperation;
    private static final String SAMPLE_LOGBOOKOPERATION_FILENAME = "logbookoperation_sample.json";
    private static final String SIP_DIRECTORY = "sip";
    private static final String INGEST_URI = "/ingests";
    private static JunitHelper junitHelper;
    private static int port;
    private static ServerApplication application;
    private static final List<Integer> tenants = new ArrayList<>();

    private static final String TRACEABILITY_CHECK_URL = "traceability/check";
    private static final String TRACEABILITY_CHECK_MAP = "{EventID: \"fake_id\"}";
    private static final String TRACEABILITY_CHECK_DSL_QUERY = "{EventID: \"fake_id\"}";

    private final int TENANT_ID = 0;
    private final String CONTRACT_NAME = "contract";
    private static final String STATUS_FIELD_QUERY = "Status";
    private static final String ACTIVATION_DATE_FIELD_QUERY = "ActivationDate";
    private static final String LAST_UPDATE_FIELD_QUERY = "LastUpdate";
    private static final String NAME_FIELD_QUERY = "Name";

    @BeforeClass
    public static void setup() throws Exception {
        junitHelper = JunitHelper.getInstance();
        port = junitHelper.findAvailablePort();
        tenants.add(0);
        tenants.add(1);

        // TODO P1 verifier la compatibilité avec les tests parallèles sur jenkins
        final WebApplicationConfig webApplicationConfig =
            (WebApplicationConfig) new WebApplicationConfig().setPort(port).setBaseUrl(DEFAULT_WEB_APP_CONTEXT)
                .setServerHost(DEFAULT_HOST).setStaticContent(DEFAULT_STATIC_CONTENT)
                .setSecure(false)
                .setSipDirectory(Thread.currentThread().getContextClassLoader().getResource(SIP_DIRECTORY).getPath())
                .setJettyConfig(JETTY_CONFIG).setTenants(tenants);
        application = new ServerApplication(webApplicationConfig) {
            @Override
            protected void registerInResourceConfig(ResourceConfig resourceConfig) {
                Set<String> permissions =
                    PermissionReader.getMethodsAnnotatedWith(WebApplicationResource.class, RequiresPermissions.class);
                resourceConfig.register(new WebApplicationResource(getConfiguration(), permissions));
            }
        };
        application.start();
        RestAssured.port = port;
        RestAssured.basePath = DEFAULT_WEB_APP_CONTEXT + "/v1/api";

        sampleLogbookOperation = JsonHandler.getFromFile(PropertiesUtils.findFile(SAMPLE_LOGBOOKOPERATION_FILENAME));
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        application.stop();
        junitHelper.releasePort(port);
    }

    @Before
    public void initStaticMock() {
        PowerMockito.mockStatic(UserInterfaceTransactionManager.class);
        PowerMockito.mockStatic(DslQueryHelper.class);
        PowerMockito.mockStatic(IngestExternalClientFactory.class);
        PowerMockito.mockStatic(AdminExternalClientFactory.class);
        PowerMockito.mockStatic(AccessExternalClientFactory.class);
        PowerMockito.mockStatic(PaginationHelper.class);
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testMessagesLogbook() throws InvalidParseOperationException {
        final ResponseBody response =
            given().contentType(ContentType.JSON).expect().statusCode(Status.OK.getStatusCode()).when()
                .get("/messages/logbook").getBody();
        final JsonNode jsonNode = JsonHandler.getFromInputStream(response.asInputStream());
        assertTrue(jsonNode.isObject());
    }


    @SuppressWarnings("rawtypes")
    @Test
    public void testTenants() throws InvalidParseOperationException {
        final ResponseBody response =
            given().contentType(ContentType.JSON).expect().statusCode(Status.OK.getStatusCode()).when()
                .get("/tenants").getBody();
        final JsonNode jsonNode = JsonHandler.getFromInputStream(response.asInputStream());
        assertTrue(jsonNode.isArray());
        assertEquals(0, jsonNode.get(0).asInt());
    }


    @Test
    public void givenNoArchiveUnitWhenSearchOperationsThenReturnOK() throws Exception {
        PowerMockito.when(UserInterfaceTransactionManager.searchUnits(anyObject(), anyObject(), anyObject()))
            .thenReturn(RequestResponseOK.getFromJsonNode(FAKE_JSONNODE_RETURN));

        PowerMockito.doNothing().when(PaginationHelper.class, "setResult", anyString(), anyObject());
        PowerMockito.when(PaginationHelper.getResult(Matchers.any(JsonNode.class), anyObject()))
            .thenReturn(JsonHandler.createObjectNode());

        given().contentType(ContentType.JSON).body(OPTIONS).cookie(COOKIE).expect()
            .statusCode(Status.OK.getStatusCode()).when()
            .post("/archivesearch/units");
    }

    @Test
    public void givenNoSecureServerLoginUnauthorized() {
        given().contentType(ContentType.JSON).body(CREDENTIALS).expect()
            .statusCode(Status.UNAUTHORIZED.getStatusCode())
            .when()
            .post("login");
        given().contentType(ContentType.JSON).body(CREDENTIALS_NO_VALID).expect()
            .statusCode(Status.UNAUTHORIZED.getStatusCode()).when()
            .post("login");
    }


    private Map<String, String> createActiveMapForUpdateAccessContract() {
        String now = LocalDateUtil.now().toString();
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(STATUS_FIELD_QUERY, "ACTIVE");
        parameters.put(LAST_UPDATE_FIELD_QUERY, now);
        parameters.put(ACTIVATION_DATE_FIELD_QUERY, now);
        parameters.put(NAME_FIELD_QUERY, "aName");
        return parameters;
    }

    private void initializeAdminExternalClientMock()
        throws InvalidParseOperationException, AccessExternalClientException {
        final RequestResponse mockResponse = Mockito.mock(RequestResponse.class);
        final AdminExternalClient adminExternalClient = PowerMockito.mock(AdminExternalClient.class);
        final AdminExternalClientFactory adminExternalFactory = PowerMockito.mock(AdminExternalClientFactory.class);
        PowerMockito.when(adminExternalFactory.getClient()).thenReturn(adminExternalClient);
        PowerMockito.when(AdminExternalClientFactory.getInstance()).thenReturn(adminExternalFactory);

        JsonNode jsonNode = JsonHandler.createObjectNode();
        Mockito.doReturn("Atr").when(mockResponse).getHeaderString(anyObject());
        Mockito.doReturn(200).when(mockResponse).getStatus();
        Mockito.doReturn(mockResponse).when(adminExternalClient).updateAccessContract(eq(new VitamContext(TENANT_ID)),
            eq("azercdsqsdf"), eq(jsonNode)
        );
    }


    @Test
    public void givenAccessContractTestUpdate() throws InvalidParseOperationException, AccessExternalClientException {

        initializeAdminExternalClientMock();
        final Map<String, String> parameters = createActiveMapForUpdateAccessContract();
        String jsonObject = JsonHandler.unprettyPrint(parameters);
        final ResponseBody response =
            given().contentType(ContentType.JSON)
                .body(jsonObject).expect()
                .statusCode(Status.OK.getStatusCode()).when()
                .post("/accesscontracts/azercdsqsdf").getBody();
    }

    @Test
    public void givenIngestContractTestUpdate() throws InvalidParseOperationException, AccessExternalClientException {
        initializeAdminExternalClientMock();
        final Map<String, String> parameters = createActiveMapForUpdateAccessContract();
        String jsonObject = JsonHandler.unprettyPrint(parameters);
        final ResponseBody response =
            given().contentType(ContentType.JSON)
                .body(jsonObject).expect()
                .statusCode(Status.OK.getStatusCode()).when()
                .post("/contracts/azercdsqsdf").getBody();
    }

    @Test
    public void testSuccessStatus() {
        given().expect().statusCode(Status.NO_CONTENT.getStatusCode()).when().get("status");
    }


    @SuppressWarnings("unchecked")
    @Test
    public void testLogbookResultRemainingExceptions()
        throws Exception {

        final Map<String, Object> searchCriteriaMap = JsonHandler.getMapFromString(OPTIONS);
        final JsonNode preparedDslQuery = JsonHandler.createObjectNode();
        PowerMockito.when(DslQueryHelper.createSingleQueryDSL(searchCriteriaMap)).thenReturn(preparedDslQuery);

        PowerMockito.when(UserInterfaceTransactionManager.selectOperation(preparedDslQuery, TENANT_ID, CONTRACT_NAME))
            .thenThrow(Exception.class);
        given().contentType(ContentType.JSON).body(OPTIONS).expect()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).when().post("/logbook/operations");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetLogbookResultByIdVitamClientException()
        throws Exception {
        String contractName = "test_contract";
        PowerMockito.when(UserInterfaceTransactionManager.selectOperationbyId("1", TENANT_ID, contractName))
            .thenThrow(VitamClientException.class);

        given().param("idOperation", "1").header(new Header(GlobalDataRest.X_ACCESS_CONTRAT_ID, contractName)).expect()
            .statusCode(Status.NOT_FOUND.getStatusCode()).when()
            .post("/logbook/operations/1");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetLogbookResultByIdLogbookRemainingException()
        throws Exception {
        String contractName = "test_contract";
        PowerMockito.when(UserInterfaceTransactionManager.selectOperationbyId("1", TENANT_ID, contractName))
            .thenThrow(Exception.class);

        given().param("idOperation", "1").header(new Header(GlobalDataRest.X_ACCESS_CONTRAT_ID, contractName)).expect()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).when()
            .post("/logbook/operations/1");
    }

    @Test
    public void testGetLogbookResultByIdLogbookRemainingIllrgalArgumentException()
        throws InvalidParseOperationException, LogbookClientException, InvalidCreateOperationException {

        given().contentType(ContentType.JSON).expect().statusCode(Status.BAD_REQUEST.getStatusCode()).when()
            .post("/logbook/operations/1");
    }

    @SuppressWarnings({"unchecked"})
    @Test
    public void testArchiveSearchResultDslQueryHelperExceptions()
        throws InvalidParseOperationException, InvalidCreateOperationException {

        final Map<String, Object> searchCriteriaMap = JsonHandler.getMapFromString(OPTIONS);

        // DslqQueryHelper Exceptions : InvalidParseOperationException,
        // InvalidCreateOperationException
        PowerMockito.when(DslQueryHelper.createSelectElasticsearchDSLQuery(searchCriteriaMap))
            .thenThrow(InvalidParseOperationException.class, InvalidCreateOperationException.class);

        given().contentType(ContentType.JSON).body(OPTIONS).expect().statusCode(Status.BAD_REQUEST.getStatusCode())
            .when().post("/archivesearch/units");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testArchiveSearchResultAccessExternalClientServerException() throws Exception {
        final Map<String, Object> searchCriteriaMap = JsonHandler.getMapFromString(OPTIONS);
        final JsonNode preparedDslQuery = JsonHandler.createObjectNode();

        PowerMockito.when(DslQueryHelper.createSelectElasticsearchDSLQuery(searchCriteriaMap))
            .thenReturn(preparedDslQuery);

        // UserInterfaceTransactionManager Exception 1 :
        // AccessExternalClientServerException
        PowerMockito.when(UserInterfaceTransactionManager.searchUnits(preparedDslQuery, TENANT_ID, CONTRACT_NAME))
            .thenThrow(AccessExternalClientServerException.class);

        given().contentType(ContentType.JSON).body(OPTIONS).expect()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).when().post("/archivesearch/units");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testArchiveSearchResultAccessExternalClientNotFoundException()
        throws Exception {
        final Map<String, Object> searchCriteriaMap = JsonHandler.getMapFromString(OPTIONS);
        final JsonNode preparedDslQuery = JsonHandler.createObjectNode();

        PowerMockito.when(DslQueryHelper.createSelectElasticsearchDSLQuery(searchCriteriaMap))
            .thenReturn(preparedDslQuery);
        PowerMockito.when(UserInterfaceTransactionManager.searchUnits(preparedDslQuery, TENANT_ID, CONTRACT_NAME))
            .thenThrow(AccessExternalClientNotFoundException.class);

        given().contentType(ContentType.JSON).body(OPTIONS).header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_NAME).expect()
            .statusCode(Status.NOT_FOUND.getStatusCode())
            .when()
            .post("/archivesearch/units");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testArchiveSearchResultRemainingExceptions() throws Exception {
        final Map<String, Object> searchCriteriaMap = JsonHandler.getMapFromString(OPTIONS);
        final JsonNode preparedDslQuery = JsonHandler.createObjectNode();

        PowerMockito.when(DslQueryHelper.createSelectElasticsearchDSLQuery(searchCriteriaMap))
            .thenReturn(preparedDslQuery);

        // UserInterfaceTransactionManager Exception 1 :
        // AccessExternalClientServerException
        PowerMockito.when(UserInterfaceTransactionManager.searchUnits(preparedDslQuery, TENANT_ID, CONTRACT_NAME))
            .thenThrow(Exception.class);

        given().contentType(ContentType.JSON).body(OPTIONS).expect()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).when().post("/archivesearch/units");
    }

    @Test
    public void testGetArchiveUnitDetails() {
        given().param("id", "1").expect().statusCode(Status.OK.getStatusCode()).when().get("/archivesearch/unit/1");
    }

    @SuppressWarnings({"unchecked"})
    @Test
    public void testArchiveUnitDetailsDslQueryHelperExceptions()
        throws InvalidParseOperationException, InvalidCreateOperationException {

        final Map<String, String> searchCriteriaMap = new HashMap<>();
        searchCriteriaMap.put(UiConstants.SELECT_BY_ID.toString(), "1");
        searchCriteriaMap.put(DslQueryHelper.PROJECTION_DSL, GLOBAL.RULES.exactToken());

        // DslqQueryHelper Exceptions : InvalidParseOperationException,
        // InvalidCreateOperationException
        PowerMockito.when(DslQueryHelper.createSelectDSLQuery(searchCriteriaMap))
            .thenThrow(InvalidParseOperationException.class, InvalidCreateOperationException.class);

        given().param("id", "1").expect().statusCode(Status.BAD_REQUEST.getStatusCode()).when()
            .get("/archivesearch/unit/1");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testArchiveUnitDetailsAccessExternalClientServerException() throws Exception {
        final Map<String, String> searchCriteriaMap = new HashMap();
        searchCriteriaMap.put(UiConstants.SELECT_BY_ID.toString(), "1");
        searchCriteriaMap.put(DslQueryHelper.PROJECTION_DSL, GLOBAL.RULES.exactToken());

        final JsonNode preparedDslQuery = JsonHandler.createObjectNode();

        PowerMockito.when(DslQueryHelper.createSelectDSLQuery(searchCriteriaMap)).thenReturn(preparedDslQuery);

        // UserInterfaceTransactionManager Exception 1 :
        // AccessExternalClientServerException
        PowerMockito
            .when(
                UserInterfaceTransactionManager.getArchiveUnitDetails(preparedDslQuery, "1", TENANT_ID, CONTRACT_NAME))
            .thenThrow(AccessExternalClientServerException.class);

        given().param("id", "1").header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_NAME).expect()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).when()
            .get("/archivesearch/unit/1");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testArchiveUnitDetailsAccessExternalClientNotFoundException()
        throws Exception {
        final Map<String, String> searchCriteriaMap = new HashMap<>();
        searchCriteriaMap.put(UiConstants.SELECT_BY_ID.toString(), "1");
        searchCriteriaMap.put(DslQueryHelper.PROJECTION_DSL, GLOBAL.RULES.exactToken());

        final JsonNode preparedDslQuery = JsonHandler.createObjectNode();

        PowerMockito.when(DslQueryHelper.createSelectDSLQuery(searchCriteriaMap)).thenReturn(preparedDslQuery);

        // UserInterfaceTransactionManager Exception 2 :
        // AccessExternalClientNotFoundException
        PowerMockito
            .when(
                UserInterfaceTransactionManager.getArchiveUnitDetails(preparedDslQuery, "1", TENANT_ID, CONTRACT_NAME))
            .thenThrow(AccessExternalClientNotFoundException.class);

        given().param("id", "1").header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_NAME).expect()
            .statusCode(Status.NOT_FOUND.getStatusCode()).when()
            .get("/archivesearch/unit/1");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testArchiveUnitDetailsRemainingExceptions() throws Exception {
        final Map<String, String> searchCriteriaMap = new HashMap<>();
        searchCriteriaMap.put(UiConstants.SELECT_BY_ID.toString(), "1");
        searchCriteriaMap.put(DslQueryHelper.PROJECTION_DSL, GLOBAL.RULES.exactToken());
        final JsonNode preparedDslQuery = JsonHandler.createObjectNode();

        PowerMockito.when(DslQueryHelper.createSelectDSLQuery(searchCriteriaMap)).thenReturn(preparedDslQuery);

        // All exceptions
        PowerMockito
            .when(
                UserInterfaceTransactionManager.getArchiveUnitDetails(preparedDslQuery, "1", TENANT_ID, CONTRACT_NAME))
            .thenThrow(Exception.class);

        given().param("id", "1").header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_NAME).expect()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).when()
            .get("/archivesearch/unit/1");
    }

    /**
     * Update Unit Treatment
     */
    @Test
    public void testUpdateArchiveUnitWithoutBody() {
        given().contentType(ContentType.JSON).expect().statusCode(Status.BAD_REQUEST.getStatusCode()).when()
            .post("/archiveupdate/units/1");
    }

    @SuppressWarnings({"unchecked"})
    @Test
    public void testUpdateArchiveUnitDetailsDsl()
        throws InvalidParseOperationException, InvalidCreateOperationException {
        final Map<String, String> updateCriteriaMap = new HashMap<>();
        updateCriteriaMap.put(UiConstants.SELECT_BY_ID.toString(), "1");
        updateCriteriaMap.put("title", "archive1");

        // DslqQueryHelper Exceptions : InvalidParseOperationException,
        // InvalidCreateOperationException
        final Map<String, JsonNode> updateRules = new HashMap<>();
        PowerMockito.when(DslQueryHelper.createUpdateDSLQuery(updateCriteriaMap, updateRules))
            .thenThrow(InvalidParseOperationException.class, InvalidCreateOperationException.class);

        given().contentType(ContentType.JSON).body(UPDATE).expect()
            .statusCode(Status.OK.getStatusCode()).when()
            .post("/archiveupdate/units/1");
    }

    @Test
    public void testUploadSipOK() throws Exception {
        final RequestResponse<Void> mockResponse = Mockito.mock(RequestResponse.class);
        final IngestExternalClient ingestClient = PowerMockito.mock(IngestExternalClient.class);
        final IngestExternalClientFactory ingestFactory = PowerMockito.mock(IngestExternalClientFactory.class);
        PowerMockito.when(ingestFactory.getClient()).thenReturn(ingestClient);
        PowerMockito.when(IngestExternalClientFactory.getInstance()).thenReturn(ingestFactory);

        Mockito.doReturn("Atr").when(mockResponse).getHeaderString(anyObject());
        Mockito.doReturn(200).when(mockResponse).getStatus();
        Mockito.doReturn(mockResponse).when(ingestClient).upload(anyObject(), anyObject(), anyObject(), anyObject());

        final InputStream stream = PropertiesUtils.getResourceAsStream("SIP.zip");
        // Need for test
        IOUtils.toByteArray(stream);

        final ResponseBody s = given()
            .headers(WebApplicationResource.X_CHUNK_OFFSET, "1", WebApplicationResource.X_SIZE_TOTAL, "1")
            .contentType(ContentType.BINARY)
            .config(RestAssured.config().encoderConfig(
                EncoderConfig.encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false)))
            .content(stream).expect()
            .statusCode(Status.OK.getStatusCode()).when()
            .post("/ingest/upload").getBody();

        final JsonNode firstRequestId = JsonHandler.getFromString(s.asString());
        assertTrue(firstRequestId.get(GlobalDataRest.X_REQUEST_ID.toLowerCase()).asText() != null);
    }

    @Test
    public void testUploadSipMultipleChunkOK() throws Exception {
        final RequestResponse mockResponse = Mockito.mock(RequestResponse.class);
        final IngestExternalClient ingestClient = PowerMockito.mock(IngestExternalClient.class);
        final IngestExternalClientFactory ingestFactory = PowerMockito.mock(IngestExternalClientFactory.class);
        PowerMockito.when(ingestFactory.getClient()).thenReturn(ingestClient);
        PowerMockito.when(IngestExternalClientFactory.getInstance()).thenReturn(ingestFactory);

        Mockito.doReturn("Atr").when(mockResponse).getHeaderString(anyObject());
        Mockito.doReturn(200).when(mockResponse).getStatus();
        Mockito.doReturn(mockResponse).when(ingestClient).upload(anyObject(), anyObject(), anyObject(), anyObject());

        final InputStream stream = PropertiesUtils.getResourceAsStream("SIP.zip");
        // Need for test
        byte[] content = IOUtils.toByteArray(stream);

        InputStream stream1 = new ByteArrayInputStream(content, 0, 1048576);
        InputStream stream2 = new ByteArrayInputStream(content, 1048576, 2097152);
        InputStream stream3 = new ByteArrayInputStream(content, 2097152, 3145728);
        InputStream stream4 = new ByteArrayInputStream(content, 3145728, 4194304);
        InputStream stream5 = new ByteArrayInputStream(content, 4194304, 5000000);
        final ResponseBody s1 = given()
            .headers(WebApplicationResource.X_SIZE_TOTAL, "5000000", WebApplicationResource.X_CHUNK_OFFSET, "0")
            .contentType(ContentType.BINARY)
            .config(RestAssured.config().encoderConfig(
                EncoderConfig.encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false)))
            .content(stream1).expect()
            .statusCode(Status.OK.getStatusCode()).when()
            .post("/ingest/upload").getBody();
        final JsonNode firstRequestId = JsonHandler.getFromString(s1.asString());
        assertTrue(firstRequestId.get(GlobalDataRest.X_REQUEST_ID.toLowerCase()).asText() != null);
        String reqId = firstRequestId.get(GlobalDataRest.X_REQUEST_ID.toLowerCase()).asText();
        File temporarySipFile = PropertiesUtils.fileFromTmpFolder(reqId);
        given()
            .headers(WebApplicationResource.X_SIZE_TOTAL, "5000000", WebApplicationResource.X_CHUNK_OFFSET, "1048576",
                GlobalDataRest.X_REQUEST_ID, reqId)
            .contentType(ContentType.BINARY)
            .config(RestAssured.config().encoderConfig(
                EncoderConfig.encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false)))
            .content(stream2).expect()
            .statusCode(Status.OK.getStatusCode()).when()
            .post("/ingest/upload").getBody();
        given()
            .headers(WebApplicationResource.X_SIZE_TOTAL, "5000000", WebApplicationResource.X_CHUNK_OFFSET, "2097152",
                GlobalDataRest.X_REQUEST_ID, reqId)
            .contentType(ContentType.BINARY)
            .config(RestAssured.config().encoderConfig(
                EncoderConfig.encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false)))
            .content(stream3).expect()
            .statusCode(Status.OK.getStatusCode()).when()
            .post("/ingest/upload").getBody();

        given()
            .headers(WebApplicationResource.X_SIZE_TOTAL, "5000000", WebApplicationResource.X_CHUNK_OFFSET, "3145728",
                GlobalDataRest.X_REQUEST_ID, reqId)
            .contentType(ContentType.BINARY)
            .config(RestAssured.config().encoderConfig(
                EncoderConfig.encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false)))
            .content(stream4).expect()
            .statusCode(Status.OK.getStatusCode()).when()
            .post("/ingest/upload").getBody();
        given()
            .headers(WebApplicationResource.X_SIZE_TOTAL, "5000000", WebApplicationResource.X_CHUNK_OFFSET, "4194304",
                GlobalDataRest.X_REQUEST_ID, reqId)
            .contentType(ContentType.BINARY)
            .config(RestAssured.config().encoderConfig(
                EncoderConfig.encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false)))
            .content(stream5).expect()
            .statusCode(Status.OK.getStatusCode()).when()
            .post("/ingest/upload").getBody();
        // Cannot check uploaded file for certain since it might be already deleted
        try {
            byte[] finalContent = IOUtils.toByteArray(new FileInputStream(temporarySipFile));
            assertTrue(Arrays.equals(content, finalContent));
        } catch (IOException e) {
            // Ignore since file wad deleted before test
        }
    }

    @Test
    public void givenReferentialWrongFormatWhenUploadThenThrowReferentialException() throws Exception {

        final AdminExternalClient adminManagementClient = PowerMockito.mock(AdminExternalClient.class);
        final AdminExternalClientFactory adminManagementClientFactory =
            PowerMockito.mock(AdminExternalClientFactory.class);
        PowerMockito.doThrow(new AccessExternalClientException("")).when(adminManagementClient)
            .createDocuments(anyObject(), anyObject(), anyObject(), anyObject());
        PowerMockito.when(adminManagementClientFactory.getClient()).thenReturn(adminManagementClient);
        PowerMockito.when(AdminExternalClientFactory.getInstance()).thenReturn(adminManagementClientFactory);

        final InputStream stream = PropertiesUtils.getResourceAsStream("FF-vitam-ko.fake");
        // Need for test
        IOUtils.toByteArray(stream);

        given()
            .contentType(ContentType.BINARY)
            .config(RestAssured.config().encoderConfig(
                EncoderConfig.encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false)))
            .content(stream).expect()
            .statusCode(Status.FORBIDDEN.getStatusCode()).when()
            .post("/format/upload");
    }

    @Test
    public void testFormatUploadOK() throws Exception {

        final AdminExternalClient adminManagementClient = PowerMockito.mock(AdminExternalClient.class);
        final AdminExternalClientFactory adminManagementClientFactory =
            PowerMockito.mock(AdminExternalClientFactory.class);
        PowerMockito.doReturn(Status.OK).when(adminManagementClient).createDocuments(anyObject(),
            anyObject(), anyObject(),
            anyObject());
        PowerMockito.when(adminManagementClientFactory.getClient()).thenReturn(adminManagementClient);
        PowerMockito.when(AdminExternalClientFactory.getInstance()).thenReturn(adminManagementClientFactory);

        final InputStream stream = PropertiesUtils.getResourceAsStream("FF-vitam.xml");
        // Need for test
        IOUtils.toByteArray(stream);

        given()
            .contentType(ContentType.BINARY)
            .header(GlobalDataRest.X_FILENAME, "FF-vitam.xml")
            .config(RestAssured.config().encoderConfig(
                EncoderConfig.encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false)))
            .content(stream).expect()
            .statusCode(Status.OK.getStatusCode()).when()
            .post("/format/upload");
    }

    @Test
    public void testUploadSipError() throws Exception {

        final IngestExternalClient ingestClient = PowerMockito.mock(IngestExternalClient.class);
        final IngestExternalClientFactory ingestFactory = PowerMockito.mock(IngestExternalClientFactory.class);
        doThrow(new IngestExternalException("IngestExternalException")).when(ingestClient).upload(
            anyObject(), anyObject(),
            anyObject(), anyObject());
        PowerMockito.when(ingestFactory.getClient()).thenReturn(ingestClient);
        PowerMockito.when(IngestExternalClientFactory.getInstance()).thenReturn(ingestFactory);

        final InputStream stream = PropertiesUtils.getResourceAsStream("SIP.zip");
        // Need for test
        IOUtils.toByteArray(stream);

        given()
            .headers(GlobalDataRest.X_REQUEST_ID, "no_req_id")
            .contentType(ContentType.BINARY)
            .config(RestAssured.config().encoderConfig(
                EncoderConfig.encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false)))
            .content(stream).expect()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).when()
            .post("/ingest/upload");
    }

    @Test
    public void testSearchFormatOK() throws Exception {
        final AdminExternalClient adminClient = PowerMockito.mock(AdminExternalClient.class);
        final AdminExternalClientFactory adminFactory = PowerMockito.mock(AdminExternalClientFactory.class);
        doReturn(ClientMockResultHelper.getFormatList()).when(adminClient).findFormats(
            anyObject(), anyObject()
        );
        PowerMockito.when(DslQueryHelper.createSingleQueryDSL(anyObject()))
            .thenReturn(JsonHandler.getFromString(OPTIONS));

        PowerMockito.when(adminFactory.getClient()).thenReturn(adminClient);
        PowerMockito.when(AdminExternalClientFactory.getInstance()).thenReturn(adminFactory);

        PowerMockito.doNothing().when(PaginationHelper.class, "setResult", anyString(), anyObject());
        PowerMockito.when(PaginationHelper.getResult(Matchers.any(JsonNode.class), anyObject()))
            .thenReturn(JsonHandler.createObjectNode());

        given().contentType(ContentType.JSON).body(OPTIONS).cookie(COOKIE).expect()
            .statusCode(Status.OK.getStatusCode()).when()
            .post("/admin/formats");
    }

    @Test
    public void testSearchFormatBadRequest() throws Exception {
        final AdminExternalClient adminClient = PowerMockito.mock(AdminExternalClient.class);
        final AdminExternalClientFactory adminFactory = PowerMockito.mock(AdminExternalClientFactory.class);
        PowerMockito.when(DslQueryHelper.createSingleQueryDSL(anyObject()))
            .thenThrow(new InvalidParseOperationException(""));

        PowerMockito.when(adminFactory.getClient()).thenReturn(adminClient);
        PowerMockito.when(AdminExternalClientFactory.getInstance()).thenReturn(adminFactory);

        PowerMockito.doNothing().when(PaginationHelper.class, "setResult", anyString(), anyObject());
        PowerMockito.when(PaginationHelper.getResult(Matchers.any(JsonNode.class), anyObject()))
            .thenReturn(JsonHandler.createObjectNode());

        given().contentType(ContentType.JSON).body(OPTIONS).cookie(COOKIE).expect()
            .statusCode(Status.BAD_REQUEST.getStatusCode()).when()
            .post("/admin/formats");
    }

    @Test
    public void testSearchFormatNotFound() throws Exception {
        final AdminExternalClient adminClient = PowerMockito.mock(AdminExternalClient.class);
        final AdminExternalClientFactory adminFactory = PowerMockito.mock(AdminExternalClientFactory.class);
        PowerMockito.doThrow(new VitamClientException("")).when(adminClient).findFormats(
            anyObject(), anyObject()
        );
        PowerMockito.when(DslQueryHelper.createSingleQueryDSL(anyObject()))
            .thenReturn(JsonHandler.getFromString(OPTIONS));

        PowerMockito.when(adminFactory.getClient()).thenReturn(adminClient);
        PowerMockito.when(AdminExternalClientFactory.getInstance()).thenReturn(adminFactory);

        PowerMockito.doNothing().when(PaginationHelper.class, "setResult", anyString(), anyObject());
        PowerMockito.when(PaginationHelper.getResult(Matchers.any(JsonNode.class), anyObject()))
            .thenReturn(JsonHandler.createObjectNode());

        given().contentType(ContentType.JSON).body(OPTIONS).cookie(COOKIE).expect()
            .statusCode(Status.NOT_FOUND.getStatusCode()).when()
            .post("/admin/formats");
    }

    @Test
    public void testSearchFormatByIdOK() throws Exception {
        final AdminExternalClient adminClient = PowerMockito.mock(AdminExternalClient.class);
        final AdminExternalClientFactory adminFactory = PowerMockito.mock(AdminExternalClientFactory.class);
        doReturn(ClientMockResultHelper.getFormat()).when(adminClient).findFormatById(
            anyObject(), anyObject()
        );

        PowerMockito.when(adminFactory.getClient()).thenReturn(adminClient);
        PowerMockito.when(AdminExternalClientFactory.getInstance()).thenReturn(adminFactory);

        given().contentType(ContentType.JSON).body(OPTIONS).expect()
            .statusCode(Status.OK.getStatusCode()).when()
            .post("/admin/formats/1");
    }

    @Test
    public void testSearchFormatByIdNotFound() throws Exception {
        final AdminExternalClient adminClient = PowerMockito.mock(AdminExternalClient.class);
        final AdminExternalClientFactory adminFactory = PowerMockito.mock(AdminExternalClientFactory.class);
        PowerMockito.doThrow(new VitamClientException("VitamClientException"))
            .when(adminClient).findFormatById(anyObject(), anyObject());

        PowerMockito.when(adminFactory.getClient()).thenReturn(adminClient);
        PowerMockito.when(AdminExternalClientFactory.getInstance()).thenReturn(adminFactory);

        given().contentType(ContentType.JSON).body(OPTIONS).expect()
            .statusCode(Status.NOT_FOUND.getStatusCode()).when()
            .post("/admin/formats/1");
    }

    @Test
    public void testCheckFormatOK() throws Exception {
        final AdminExternalClient adminClient = PowerMockito.mock(AdminExternalClient.class);
        final AdminExternalClientFactory adminFactory = PowerMockito.mock(AdminExternalClientFactory.class);
        PowerMockito.when(adminClient.checkDocuments(anyObject(), anyObject(), anyObject()))
            .thenReturn(Response.ok().build());
        PowerMockito.when(DslQueryHelper.createSingleQueryDSL(anyObject()))
            .thenReturn(JsonHandler.getFromString(OPTIONS));

        PowerMockito.when(adminFactory.getClient()).thenReturn(adminClient);
        PowerMockito.when(AdminExternalClientFactory.getInstance()).thenReturn(adminFactory);

        final InputStream stream = PropertiesUtils.getResourceAsStream("FF-vitam-ko.fake");
        // Need for test
        IOUtils.toByteArray(stream);

        given()
            .contentType(ContentType.BINARY)
            .config(RestAssured.config().encoderConfig(
                EncoderConfig.encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false)))
            .content(stream).expect()
            .statusCode(Status.OK.getStatusCode()).when()
            .post("/format/check");
    }

    @Test
    public void testNotFoundGetArchiveObjectGroup() throws Exception {

        PowerMockito
            .when(UserInterfaceTransactionManager.selectObjectbyId(anyObject(), anyObject(), anyObject(), anyObject()))
            .thenThrow(new AccessExternalClientNotFoundException(""));

        given().accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
            .expect().statusCode(Status.NOT_FOUND.getStatusCode()).when()
            .get("/archiveunit/objects/idOG");
    }

    @Test
    public void testOKGetArchiveObjectGroup() throws Exception {
        final RequestResponseOK sampleObjectGroup =
            RequestResponseOK
                .getFromJsonNode(JsonHandler.getFromFile(PropertiesUtils.findFile("sample_objectGroup_document.json")));
        PowerMockito
            .when(UserInterfaceTransactionManager.selectObjectbyId(anyObject(), anyObject(), anyObject(), anyObject()))
            .thenReturn(sampleObjectGroup);

        given().accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
            .expect().statusCode(Status.OK.getStatusCode()).when()
            .get("/archiveunit/objects/idOG");
    }

    @Test
    public void testBadRequestGetArchiveObjectGroup() throws Exception {
        PowerMockito.when(DslQueryHelper.createSelectDSLQuery(anyObject()))
            .thenThrow(new InvalidParseOperationException(""));

        given().accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
            .expect().statusCode(Status.BAD_REQUEST.getStatusCode()).when()
            .get("/archiveunit/objects/idOG");
    }

    @Test
    public void testInternalServerErrorGetArchiveObjectGroup() throws Exception {
        PowerMockito
            .when(UserInterfaceTransactionManager.selectObjectbyId(anyObject(), anyObject(), anyObject(), anyObject()))
            .thenThrow(new AccessExternalClientServerException(""));

        given().accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
            .expect().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).when()
            .get("/archiveunit/objects/idOG");
    }

    @Test
    public void testUnknownErrorGetArchiveObjectGroup() throws Exception {
        PowerMockito
            .when(UserInterfaceTransactionManager.selectObjectbyId(anyObject(), anyObject(), anyObject(), anyObject()))
            .thenThrow(new NullPointerException(""));

        given().accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
            .expect().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).when()
            .get("/archiveunit/objects/idOG");
    }

    @Test
    public void testNotFoundGetObjectAsInputStream() throws Exception {

        PowerMockito.when(
            UserInterfaceTransactionManager.getObjectAsInputStream(anyObject(), anyObject(), anyString(), anyString(),
                anyInt(), anyString(), anyObject(), anyString()))
            .thenThrow(new AccessExternalClientNotFoundException(""));

        given()
            .accept(MediaType.APPLICATION_OCTET_STREAM)
            .body(OPTIONS_DOWNLOAD)
            .expect()
            .statusCode(Status.NOT_FOUND.getStatusCode())
            .when()
            .get(
                "/archiveunit/objects/download/idOG?usage=BinaryMaster_1&version=0&filename=Vitam-Sensibilisation-API-V1.0.odp&tenantId=0");
    }

    // FIXME: review return of method getObjectAsInputStream and fix this test
    // The issue seems to be the asyncResponse waiting for resume
    @Ignore
    @Test
    public void testOKGetObjectAsInputStream() throws Exception {

        PowerMockito.when(
            UserInterfaceTransactionManager.getObjectAsInputStream(anyObject(), anyObject(), anyString(), anyString(),
                anyInt(), anyString(), anyObject(), anyString()))
            .thenReturn(true);

        given()
            .accept(MediaType.APPLICATION_OCTET_STREAM)
            .expect()
            .statusCode(Status.OK.getStatusCode())
            .when()
            .get(
                "/archiveunit/objects/download/idOG?usage=Dissamination&version=1&filename=Vitam-Sensibilisation-API-V1.0.odp");
    }

    @Test
    public void testBadRequestGetObjectAsInputStream() throws Exception {
        PowerMockito.when(
            UserInterfaceTransactionManager.getObjectAsInputStream(anyObject(), anyObject(), anyString(), anyString(),
                anyInt(), anyString(), anyObject(), anyString()))
            .thenThrow(new InvalidParseOperationException(""));
        given().accept(MediaType.APPLICATION_OCTET_STREAM).expect().statusCode(Status.BAD_REQUEST.getStatusCode())
            .when()
            .get("/archiveunit/objects/download/idOG?usage=Dissemination_1&filename=Vitam-Sensibilisation-API" +
                "-V1.0.odp&tenantId=0");
    }

    @Test
    public void testAccessServerExceptionGetObjectAsInputStream() throws Exception {
        PowerMockito.when(
            UserInterfaceTransactionManager.getObjectAsInputStream(anyObject(), anyObject(), anyString(), anyString(),
                anyInt(), anyString(), anyObject(), anyString()))
            .thenThrow(new AccessExternalClientServerException(""));
        given()
            .accept(MediaType.APPLICATION_OCTET_STREAM)
            .body(OPTIONS_DOWNLOAD)
            .expect()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
            .when()
            .get(
                "/archiveunit/objects/download/idOG?usage=BinaryMaster_1&version=0&filename=Vitam-Sensibilisation-API-V1.0.odp");
    }

    @Test
    public void testAccessUnknownExceptionGetObjectAsInputStream() throws Exception {
        PowerMockito.when(
            UserInterfaceTransactionManager.getObjectAsInputStream(anyObject(), anyObject(), anyString(), anyString(),
                anyInt(), anyString(), anyObject(), anyString()))
            .thenThrow(new NullPointerException());
        given()
            .accept(MediaType.APPLICATION_OCTET_STREAM)
            .expect()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
            .when()
            .get(
                "/archiveunit/objects/download/idOG?usage=BinaryMaster_1&version=0&filename=Vitam-Sensibilisation-API-V1.0.odp");
    }

    @Test
    public void testUnitTreeOk() throws InvalidCreateOperationException, VitamException {

        PowerMockito.when(
            DslQueryHelper.createSelectUnitTreeDSLQuery(anyString(), anyObject())).thenReturn(
                JsonHandler
                    .getFromString(FAKE_STRING_RETURN));
        PowerMockito.when(
            UserInterfaceTransactionManager.searchUnits(anyObject(), anyObject(), anyObject()))
            .thenReturn(RequestResponseOK.getFromJsonNode(FAKE_JSONNODE_RETURN));
        PowerMockito.when(
            UserInterfaceTransactionManager.buildUnitTree(anyString(), anyObject())).thenReturn(FAKE_JSONNODE_RETURN);

        given().contentType(ContentType.JSON).body(ALL_PARENTS)
            .expect().statusCode(Status.OK.getStatusCode()).when()
            .post("/archiveunit/tree/1");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUnitTreeWithBadRequestExceptionWhenInvalidParseOperationException()
        throws InvalidParseOperationException, InvalidCreateOperationException {
        PowerMockito.when(
            DslQueryHelper.createSelectUnitTreeDSLQuery(anyString(), anyObject()))
            .thenThrow(InvalidParseOperationException.class);

        given().contentType(ContentType.JSON).body(ALL_PARENTS)
            .expect().statusCode(Status.BAD_REQUEST.getStatusCode()).when()
            .post("/archiveunit/tree/1");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUnitTreeWithBadRequestExceptionWhenInvalidCreateOperationException()
        throws InvalidParseOperationException, InvalidCreateOperationException {
        PowerMockito.when(
            DslQueryHelper.createSelectUnitTreeDSLQuery(anyString(), anyObject()))
            .thenThrow(InvalidCreateOperationException.class);

        given().contentType(ContentType.JSON).body(ALL_PARENTS)
            .expect().statusCode(Status.BAD_REQUEST.getStatusCode()).when()
            .post("/archiveunit/tree/1");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUnitTreeWithAccessExternalClientServerException()
        throws Exception {
        PowerMockito.when(
            DslQueryHelper.createSelectUnitTreeDSLQuery(anyString(), anyObject())).thenReturn(
                JsonHandler
                    .getFromString(FAKE_STRING_RETURN));
        PowerMockito.when(
            UserInterfaceTransactionManager.searchUnits(anyObject(), anyObject(), anyObject()))
            .thenThrow(AccessExternalClientServerException.class);

        given().contentType(ContentType.JSON).body(ALL_PARENTS)
            .expect().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).when()
            .post("/archiveunit/tree/1");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUnitTreeWithAccessExternalClientNotFoundException()
        throws Exception {
        PowerMockito.when(
            DslQueryHelper.createSelectUnitTreeDSLQuery(anyString(), anyObject())).thenReturn(
                JsonHandler
                    .getFromString(FAKE_STRING_RETURN));

        PowerMockito.when(
            UserInterfaceTransactionManager.searchUnits(anyObject(), anyObject(), anyObject()))
            .thenThrow(AccessExternalClientNotFoundException.class);

        given().contentType(ContentType.JSON).body(ALL_PARENTS)
            .expect().statusCode(Status.NOT_FOUND.getStatusCode()).when()
            .post("/archiveunit/tree/1");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUnitTreeWithUnknownException()
        throws InvalidCreateOperationException, VitamException {
        PowerMockito.when(
            DslQueryHelper.createSelectUnitTreeDSLQuery(anyString(), anyObject())).thenReturn(
                JsonHandler.getFromString(FAKE_STRING_RETURN));
        PowerMockito.when(
            UserInterfaceTransactionManager.searchUnits(anyObject(), anyObject(), anyObject()))
            .thenReturn(RequestResponseOK.getFromJsonNode(FAKE_JSONNODE_RETURN));
        PowerMockito.when(
            UserInterfaceTransactionManager.buildUnitTree(anyString(), anyObject())).thenThrow(VitamException.class);

        given().contentType(ContentType.JSON).body(ALL_PARENTS)
            .expect().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).when()
            .post("/archiveunit/tree/1");
    }

    /**
     * rules Management
     ********/

    @Test
    public void givenReferentialWrongFormatRulesWhenUploadThenThrowReferentialException() throws Exception {

        final AdminExternalClient adminManagementClient = PowerMockito.mock(AdminExternalClient.class);
        final AdminExternalClientFactory adminExternalClientFactory =
            PowerMockito.mock(AdminExternalClientFactory.class);
        PowerMockito.doThrow(new AccessExternalClientException("")).when(adminManagementClient)
            .createDocuments(anyObject(), anyObject(), anyObject(), anyObject());
        PowerMockito.when(adminExternalClientFactory.getClient()).thenReturn(adminManagementClient);
        PowerMockito.when(AdminExternalClientFactory.getInstance()).thenReturn(adminExternalClientFactory);

        final InputStream stream = PropertiesUtils.getResourceAsStream("jeu_donnees_KO_regles_CSV_Parameters.csv");
        // Need for test
        IOUtils.toByteArray(stream);

        given()
            .contentType(ContentType.BINARY)
            .config(RestAssured.config().encoderConfig(
                EncoderConfig.encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false)))
            .content(stream).expect()
            .statusCode(Status.FORBIDDEN.getStatusCode()).when()
            .post("/rules/upload");
    }

    @Test
    public void testRuleUploadOK() throws Exception {

        final AdminExternalClient adminManagementClient = PowerMockito.mock(AdminExternalClient.class);
        final AdminExternalClientFactory adminManagementClientFactory =
            PowerMockito.mock(AdminExternalClientFactory.class);
        PowerMockito.doReturn(Status.OK).when(adminManagementClient).createDocuments(anyObject(),
            anyObject(), anyObject(),
            anyObject());
        PowerMockito.when(adminManagementClientFactory.getClient()).thenReturn(adminManagementClient);
        PowerMockito.when(AdminExternalClientFactory.getInstance()).thenReturn(adminManagementClientFactory);

        final InputStream stream =
            PropertiesUtils.getResourceAsStream("jeu_donnees_OK_regles_CSV.csv");
        // Need for test
        IOUtils.toByteArray(stream);

        given()
            .contentType(ContentType.BINARY)
            .header(GlobalDataRest.X_FILENAME, "jeu_donnees_OK_regles_CSV.csv")
            .config(RestAssured.config().encoderConfig(
                EncoderConfig.encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false)))
            .content(stream).expect()
            .statusCode(Status.OK.getStatusCode()).when()
            .post("/rules/upload");
    }

    @Test
    public void testSearchRulesOK() throws Exception {
        final AdminExternalClient adminClient = PowerMockito.mock(AdminExternalClient.class);
        final AdminExternalClientFactory adminFactory = PowerMockito.mock(AdminExternalClientFactory.class);
        doReturn(ClientMockResultHelper.getRuleList()).when(adminClient).findRules(
            anyObject(), anyObject()
        );
        PowerMockito.when(DslQueryHelper.createSingleQueryDSL(anyObject()))
            .thenReturn(JsonHandler.getFromString(OPTIONS));

        PowerMockito.when(adminFactory.getClient()).thenReturn(adminClient);
        PowerMockito.when(AdminExternalClientFactory.getInstance()).thenReturn(adminFactory);

        PowerMockito.doNothing().when(PaginationHelper.class, "setResult", anyString(), anyObject());
        PowerMockito.when(PaginationHelper.getResult(Matchers.any(JsonNode.class), anyObject()))
            .thenReturn(JsonHandler.createObjectNode());

        given().contentType(ContentType.JSON).body(OPTIONS).cookie(COOKIE).expect()
            .statusCode(Status.OK.getStatusCode()).when()
            .post("/admin/rules");
    }

    @Test
    public void testSearchRuleBadRequest() throws Exception {
        final AdminExternalClient adminClient = PowerMockito.mock(AdminExternalClient.class);
        final AdminExternalClientFactory adminFactory = PowerMockito.mock(AdminExternalClientFactory.class);
        doReturn(ClientMockResultHelper.getRuleList()).when(adminClient).findRules(
            anyObject(), anyObject()
        );
        PowerMockito.when(DslQueryHelper.createSingleQueryDSL(anyObject()))
            .thenThrow(new InvalidParseOperationException(""));

        PowerMockito.when(adminFactory.getClient()).thenReturn(adminClient);
        PowerMockito.when(AdminExternalClientFactory.getInstance()).thenReturn(adminFactory);

        PowerMockito.doNothing().when(PaginationHelper.class, "setResult", anyString(), anyObject());
        PowerMockito.when(PaginationHelper.getResult(Matchers.any(JsonNode.class), anyObject()))
            .thenReturn(JsonHandler.createObjectNode());

        given().contentType(ContentType.JSON).body(OPTIONS).cookie(COOKIE).expect()
            .statusCode(Status.BAD_REQUEST.getStatusCode()).when()
            .post("/admin/rules");
    }

    @Test
    public void testSearchRuleNotFound() throws Exception {
        final AdminExternalClient adminClient = PowerMockito.mock(AdminExternalClient.class);
        final AdminExternalClientFactory adminFactory = PowerMockito.mock(AdminExternalClientFactory.class);
        PowerMockito.doThrow(new VitamClientException("")).when(adminClient)
            .findRules(anyObject(), anyObject());
        PowerMockito.when(DslQueryHelper.createSingleQueryDSL(anyObject()))
            .thenReturn(JsonHandler.getFromString(OPTIONS));

        PowerMockito.when(adminFactory.getClient()).thenReturn(adminClient);
        PowerMockito.when(AdminExternalClientFactory.getInstance()).thenReturn(adminFactory);

        PowerMockito.doNothing().when(PaginationHelper.class, "setResult", anyString(), anyObject());
        PowerMockito.when(PaginationHelper.getResult(Matchers.any(JsonNode.class), anyObject()))
            .thenReturn(JsonHandler.createObjectNode());

        given().contentType(ContentType.JSON).body(OPTIONS).cookie(COOKIE).expect()
            .statusCode(Status.OK.getStatusCode()).when()
            .post("/admin/rules");
    }

    @Test
    public void testSearchRuleByIdNotFound() throws Exception {
        final AdminExternalClient adminClient = PowerMockito.mock(AdminExternalClient.class);
        final AdminExternalClientFactory adminFactory = PowerMockito.mock(AdminExternalClientFactory.class);
        PowerMockito.doThrow(new VitamClientException("VitamClientException"))
            .when(adminClient).findRuleById(anyObject(), anyObject());

        PowerMockito.when(adminFactory.getClient()).thenReturn(adminClient);
        PowerMockito.when(AdminExternalClientFactory.getInstance()).thenReturn(adminFactory);

        given().contentType(ContentType.JSON).body(OPTIONS).expect()
            .statusCode(Status.NOT_FOUND.getStatusCode()).when()
            .post("/admin/rules/1");
    }

    @Test
    public void testCheckRulesFileOK() throws Exception {
        String jsonReturn = "{test: \"ok\"}";
        InputStream inputStream =
            new ByteArrayInputStream(jsonReturn.getBytes(StandardCharsets.UTF_8));
        final AdminExternalClient adminClient = PowerMockito.mock(AdminExternalClient.class);
        final AdminExternalClientFactory adminFactory = PowerMockito.mock(AdminExternalClientFactory.class);
        when(adminClient.checkDocuments(anyObject(), anyObject(), anyObject()))
            .thenReturn(ClientMockResultHelper.getObjectStream());
        PowerMockito.when(DslQueryHelper.createSingleQueryDSL(anyObject()))
            .thenReturn(JsonHandler.getFromString(OPTIONS));
        PowerMockito.when(adminFactory.getClient()).thenReturn(adminClient);
        PowerMockito.when(AdminExternalClientFactory.getInstance()).thenReturn(adminFactory);
        final InputStream stream = PropertiesUtils.getResourceAsStream("jeu_donnees_KO_regles_CSV_Parameters.csv");

        // Need for test
        IOUtils.toByteArray(stream);
        final com.jayway.restassured.response.Response response = given().contentType(ContentType.BINARY)
            .config(RestAssured.config().encoderConfig(
                EncoderConfig.encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false)))
            .content(stream).post("/rules/check");
        assertEquals(Status.OK.getStatusCode(), response.getStatusCode());
    }

    @Test
    public void testGetUnitLifeCycleByIdOk() throws Exception {
        final RequestResponseOK result = RequestResponseOK.getFromJsonNode(FAKE_JSONNODE_RETURN);

        PowerMockito
            .when(UserInterfaceTransactionManager.selectUnitLifeCycleById(FAKE_UNIT_LF_ID, TENANT_ID, CONTRACT_NAME))
            .thenReturn(result);

        given().param("id_lc", FAKE_UNIT_LF_ID).expect().statusCode(Status.OK.getStatusCode()).when()
            .get("/unitlifecycles/" + FAKE_UNIT_LF_ID);
    }

    @Test
    public void testGetObjectGroupLifeCycleByIdOk() throws Exception {
        final RequestResponseOK result = RequestResponseOK.getFromJsonNode(FAKE_JSONNODE_RETURN);

        PowerMockito
            .when(UserInterfaceTransactionManager.selectObjectGroupLifeCycleById(FAKE_OBG_LF_ID, TENANT_ID,
                CONTRACT_NAME))
            .thenReturn(result);

        given().param("id_lc", FAKE_OBG_LF_ID).expect().statusCode(Status.OK.getStatusCode()).when()
            .get("/objectgrouplifecycles/" + FAKE_OBG_LF_ID);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetUnitLifeCycleByIdWithBadRequestWhenVitamClientException()
        throws Exception {
        PowerMockito
            .when(UserInterfaceTransactionManager.selectUnitLifeCycleById(FAKE_UNIT_LF_ID, TENANT_ID, CONTRACT_NAME))
            .thenThrow(VitamClientException.class);

        given().param("id_lc", FAKE_UNIT_LF_ID).header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_NAME).expect()
            .statusCode(Status.NOT_FOUND.getStatusCode()).when()
            .get("/unitlifecycles/" + FAKE_UNIT_LF_ID);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetUnitLifeCycleByIdWithInternalServerErrorWhenUnknownException()
        throws Exception {
        PowerMockito
            .when(UserInterfaceTransactionManager.selectUnitLifeCycleById(FAKE_UNIT_LF_ID, TENANT_ID, CONTRACT_NAME))
            .thenThrow(NullPointerException.class);

        given().param("id_lc", FAKE_UNIT_LF_ID).header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_NAME).expect()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
            .when()
            .get("/unitlifecycles/" + FAKE_UNIT_LF_ID);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetObjectGroupLifeCycleByIdWithBadRequestWhenVitamClientException()
        throws Exception {
        PowerMockito
            .when(UserInterfaceTransactionManager.selectObjectGroupLifeCycleById(FAKE_OBG_LF_ID, TENANT_ID,
                CONTRACT_NAME))
            .thenThrow(VitamClientException.class);

        given().param("id_lc", FAKE_OBG_LF_ID).header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_NAME).expect()
            .statusCode(Status.NOT_FOUND.getStatusCode()).when()
            .get("/objectgrouplifecycles/" + FAKE_OBG_LF_ID);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetOjectGroupLifeCycleByIdWithInternalServerErrorWhenUnknownException()
        throws Exception {
        PowerMockito
            .when(UserInterfaceTransactionManager.selectObjectGroupLifeCycleById(FAKE_OBG_LF_ID, TENANT_ID,
                CONTRACT_NAME))
            .thenThrow(NullPointerException.class);

        given().param("id_lc", FAKE_OBG_LF_ID).header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_NAME).expect()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).when()
            .get("/objectgrouplifecycles/" + FAKE_OBG_LF_ID);
    }

    @Test
    public void testSearchFundsRegisterOK() throws Exception {
        PowerMockito.when(UserInterfaceTransactionManager.findAccessionRegisterSummary(anyObject(), anyObject(),
            anyObject()))
            .thenReturn(ClientMockResultHelper.getAccessionRegisterSummary());

        PowerMockito.doNothing().when(PaginationHelper.class, "setResult", anyString(), anyObject());
        PowerMockito.when(PaginationHelper.getResult(Matchers.any(JsonNode.class), anyObject()))
            .thenReturn(JsonHandler.createObjectNode());

        given().contentType(ContentType.JSON).body(OPTIONS).cookie(COOKIE).expect()
            .statusCode(Status.OK.getStatusCode()).when()
            .post("/admin/accession-register");
    }

    @Test
    public void testSearchFundsRegisterBadRequest() throws Exception {
        PowerMockito.when(UserInterfaceTransactionManager.findAccessionRegisterSummary(anyObject(), anyObject(),
            anyObject()))
            .thenThrow(new InvalidParseOperationException(""));

        PowerMockito.doNothing().when(PaginationHelper.class, "setResult", anyString(), anyObject());
        PowerMockito.when(PaginationHelper.getResult(Matchers.any(JsonNode.class), anyObject()))
            .thenReturn(JsonHandler.createObjectNode());

        given().contentType(ContentType.JSON).body(OPTIONS).cookie(COOKIE).expect()
            .statusCode(Status.BAD_REQUEST.getStatusCode()).when()
            .post("/admin/accession-register");
    }

    @Test
    public void testGetAccessionRegisterDetailOK() throws Exception {
        PowerMockito.when(UserInterfaceTransactionManager.findAccessionRegisterSummary(anyObject(), anyObject(),
            anyObject()))
            .thenReturn(ClientMockResultHelper.getAccessionRegisterDetail());

        given().contentType(ContentType.JSON).body(OPTIONS).expect()
            .statusCode(Status.OK.getStatusCode()).when()
            .post("/admin/accession-register/1/accession-register-detail");
    }

    @Test
    public void testGetAccessionRegisterDetailBadRequest() throws Exception {
        PowerMockito
            .when(UserInterfaceTransactionManager.findAccessionRegisterDetail(anyObject(), anyObject(), anyObject(),
                anyObject()))
            .thenThrow(new InvalidParseOperationException(""));

        given().contentType(ContentType.JSON).body(OPTIONS).expect()
            .statusCode(Status.BAD_REQUEST.getStatusCode()).when()
            .post("/admin/accession-register/1/accession-register-detail");
    }

    @Test
    public void downloadObjects()
        throws Exception {
        final IngestExternalClient ingestClient = PowerMockito.mock(IngestExternalClient.class);
        final IngestExternalClientFactory ingestFactory = PowerMockito.mock(IngestExternalClientFactory.class);
        PowerMockito.when(ingestFactory.getClient()).thenReturn(ingestClient);
        PowerMockito.when(IngestExternalClientFactory.getInstance()).thenReturn(ingestFactory);
        Mockito.doReturn(ClientMockResultHelper.getObjectStream()).when(ingestClient).downloadObjectAsync(
            anyObject(), anyObject(),
            anyObject());

        RestAssured.given()
            .when().get(INGEST_URI + "/1/" + IngestCollection.REPORTS.getCollectionName())
            .then().statusCode(Status.OK.getStatusCode());

        Mockito.doReturn(ClientMockResultHelper.getObjectStream()).when(ingestClient).downloadObjectAsync(
            anyObject(), anyObject(),
            anyObject());

        RestAssured.given()
            .when().get(INGEST_URI + "/1/" + IngestCollection.MANIFESTS.getCollectionName())
            .then().statusCode(Status.OK.getStatusCode());

        RestAssured.given()
            .when().get(INGEST_URI + "/1/unknown")
            .then().statusCode(Status.BAD_REQUEST.getStatusCode());

        VitamError error = VitamCodeHelper.toVitamError(VitamCode.INGEST_EXTERNAL_NOT_FOUND, "NOT FOUND");
        AbstractMockClient.FakeInboundResponse fakeResponse =
            new AbstractMockClient.FakeInboundResponse(Status.NOT_FOUND, JsonHandler.writeToInpustream(error),
                MediaType.APPLICATION_OCTET_STREAM_TYPE, new MultivaluedHashMap<String, Object>());

        Mockito.doReturn(fakeResponse).when(ingestClient).downloadObjectAsync(
            anyObject(), anyObject(),
            anyObject());
        RestAssured.given()
            .when().get(INGEST_URI + "/1/" + IngestCollection.MANIFESTS.getCollectionName())
            .then().statusCode(Status.NOT_FOUND.getStatusCode());

        Mockito.doThrow(new VitamClientException("")).when(ingestClient).downloadObjectAsync(
            anyObject(), anyObject(),
            anyObject());
        RestAssured.given()
            .when().get(INGEST_URI + "/1/" + IngestCollection.MANIFESTS.getCollectionName())
            .then().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void testCheckTraceabilityOperation()
        throws Exception {
        // Mock AccessExternal response
        PowerMockito
            .when(
                UserInterfaceTransactionManager.checkTraceabilityOperation(Mockito.anyObject(),
                    (Integer) anyInt(), anyString()))
            .thenReturn(ClientMockResultHelper.getLogbooksRequestResponse());

        given().contentType(ContentType.JSON).body(TRACEABILITY_CHECK_MAP).expect()
            .statusCode(Status.OK.getStatusCode()).when()
            .post(TRACEABILITY_CHECK_URL);
    }

    @Test
    public void testDownloadTraceabilityOperation() throws Exception {

        // Mock AccessExternal response
        AdminExternalClient adminExternalClient = Mockito.mock(AdminExternalClient.class);

        final AdminExternalClientFactory adminExternalClientFactory =
            PowerMockito.mock(AdminExternalClientFactory.class);
        PowerMockito.when(adminExternalClientFactory.getClient()).thenReturn(adminExternalClient);
        PowerMockito.when(AdminExternalClientFactory.getInstance()).thenReturn(adminExternalClientFactory);

        String contractName = "test_contract";

        when(adminExternalClient.downloadTraceabilityOperationFile(
            eq(new VitamContext(0).setAccessContract(contractName)), eq("1")))
            .thenReturn(ClientMockResultHelper.getObjectStream());

        RestAssured.given()
            .when().get("traceability" + "/1/" + "content?contractId=" + contractName)
            .then().statusCode(Status.OK.getStatusCode());
    }

    @Test
    public void testExtractTimestampInformation() throws Exception {

        PowerMockito.when(UserInterfaceTransactionManager.extractInformationFromTimestamp(anyObject()))
            .thenCallRealMethod();

        final InputStream tokenFile =
            PropertiesUtils.getResourceAsStream("token.tsp");
        String encodedTimeStampToken = IOUtils.toString(tokenFile, "UTF-8");
        String timestampExtractMap = "{timestamp: \"" + encodedTimeStampToken + "\"}";
        given().contentType(ContentType.JSON).body(timestampExtractMap).expect()
            .statusCode(Status.OK.getStatusCode()).when()
            .post("/traceability/extractTimestamp");

        timestampExtractMap = "{timestamp: \"FakeTimeStamp\"}";
        given().contentType(ContentType.JSON).body(timestampExtractMap).expect()
            .statusCode(Status.BAD_REQUEST.getStatusCode()).when()
            .post("/traceability/extractTimestamp");

        timestampExtractMap = "{fakeTimestamp: \"FakeTimeStamp\"}";
        given().contentType(ContentType.JSON).body(timestampExtractMap).expect()
            .statusCode(Status.BAD_REQUEST.getStatusCode()).when()
            .post("/traceability/extractTimestamp");
    }

    @Test
    public void testLaunchAudit() throws AccessExternalClientServerException, InvalidParseOperationException {
        AdminExternalClient adminExternalClient = Mockito.mock(AdminExternalClient.class);

        final AdminExternalClientFactory adminExternalClientFactory =
            PowerMockito.mock(AdminExternalClientFactory.class);
        PowerMockito.when(adminExternalClientFactory.getClient()).thenReturn(adminExternalClient);
        PowerMockito.when(AdminExternalClientFactory.getInstance()).thenReturn(adminExternalClientFactory);

        JsonNode auditOption = JsonHandler.getFromString(AUDIT_OPTION);
        PowerMockito
            .when(adminExternalClient.launchAudit(anyObject(), Mockito.anyObject()
            ))
            .thenReturn(ClientMockResultHelper.checkOperationTraceability());

        given().contentType(ContentType.JSON).body(auditOption).expect()
            .when().post("audits")
            .then().statusCode(Status.OK.getStatusCode());
    }

}
