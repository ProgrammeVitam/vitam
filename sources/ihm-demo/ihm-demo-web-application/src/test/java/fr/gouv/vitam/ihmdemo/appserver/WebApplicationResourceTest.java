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

import static com.jayway.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.config.EncoderConfig;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.ResponseBody;

import fr.gouv.vitam.access.external.client.AccessExternalClientFactory;
import fr.gouv.vitam.access.external.client.AdminExternalClient;
import fr.gouv.vitam.access.external.client.AdminExternalClientFactory;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientNotFoundException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientServerException;
import fr.gouv.vitam.common.FileUtil;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.ihmdemo.core.DslQueryHelper;
import fr.gouv.vitam.ihmdemo.core.JsonTransformer;
import fr.gouv.vitam.ihmdemo.core.UiConstants;
import fr.gouv.vitam.ihmdemo.core.UserInterfaceTransactionManager;
import fr.gouv.vitam.ingest.external.api.IngestExternalException;
import fr.gouv.vitam.ingest.external.client.IngestExternalClient;
import fr.gouv.vitam.ingest.external.client.IngestExternalClientFactory;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.net.ssl.*", "javax.management.*"})
@PrepareForTest({UserInterfaceTransactionManager.class, DslQueryHelper.class,
    IngestExternalClientFactory.class, AdminExternalClientFactory.class,
    JsonTransformer.class, WebApplicationConfig.class, AccessExternalClientFactory.class})
public class WebApplicationResourceTest {

    private static final String DEFAULT_WEB_APP_CONTEXT = "/ihm-demo";
    private static final String DEFAULT_STATIC_CONTENT = "webapp";
    private static final String OPTIONS = "{name: \"myName\"}";
    private static final String CREDENTIALS = "{\"token\": {\"principal\": \"myName\", \"credentials\": \"myName\"}}";
    private static final String CREDENTIALS_NO_VALID =
        "{\"token\": {\"principal\": \"myName\", \"credentials\": \"myName\"}}";
    private static final String OPTIONS_DOWNLOAD = "{usage: \"Dissemination\", version: 1}";
    private static final String UPDATE = "{title: \"myarchive\"}";
    private static final String DEFAULT_HOST = "localhost";
    private static final String JETTY_CONFIG = "jetty-config-test.xml";
    private static final String ALL_PARENTS = "[\"P1\", \"P2\", \"P3\"]";
    private static final String FAKE_STRING_RETURN = "Fake String";
    private static final JsonNode FAKE_JSONNODE_RETURN = JsonHandler.createObjectNode();
    private static final String FAKE_UNIT_LF_ID = "1";
    private static final String FAKE_OBG_LF_ID = "1";
    private static final String FAKE_OPERATION_ID = "1";
    private static JsonNode sampleLogbookOperation;
    private static final String SAMPLE_LOGBOOKOPERATION_FILENAME = "logbookoperation_sample.json";
    private static final String SIP_DIRECTORY = "sip";
    private static JunitHelper junitHelper;
    private static int port;
    private static ServerApplication application;

    @BeforeClass
    public static void setup() throws Exception {
        junitHelper = JunitHelper.getInstance();
        port = junitHelper.findAvailablePort();
        // TODO P1 verifier la compatibilité avec les tests parallèles sur jenkins
        final WebApplicationConfig webApplicationConfig =
            (WebApplicationConfig) new WebApplicationConfig().setPort(port).setBaseUrl(DEFAULT_WEB_APP_CONTEXT)
                .setServerHost(DEFAULT_HOST).setStaticContent(DEFAULT_STATIC_CONTENT)
                .setSecure(false)
                .setSipDirectory(Thread.currentThread().getContextClassLoader().getResource(SIP_DIRECTORY).getPath())
                .setJettyConfig(JETTY_CONFIG);
        application = new ServerApplication(webApplicationConfig);
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
    }


    @SuppressWarnings("rawtypes")
    @Test
    public void testMessagesLogbook() throws InvalidParseOperationException {
        ResponseBody response =
            given().contentType(ContentType.JSON).expect().statusCode(Status.OK.getStatusCode()).when()
                .get("/messages/logbook").getBody();
        JsonNode jsonNode = JsonHandler.getFromInputStream(response.asInputStream());
        assertTrue(jsonNode.isObject());
    }


    @Test
    public void givenNoArchiveUnitWhenSearchOperationsThenReturnOK() {
        given().contentType(ContentType.JSON).body(OPTIONS).expect().statusCode(Status.OK.getStatusCode()).when()
            .post("/archivesearch/units");
    }

    @Test
    public void givenNoSecureServerLoginUnauthorized() {
        given().contentType(ContentType.JSON).body(CREDENTIALS).expect().statusCode(Status.UNAUTHORIZED.getStatusCode())
            .when()
            .post("login");
        given().contentType(ContentType.JSON).body(CREDENTIALS_NO_VALID).expect()
            .statusCode(Status.UNAUTHORIZED.getStatusCode()).when()
            .post("login");
    }

    @Test
    public void testSuccessStatus() {
        given().expect().statusCode(Status.OK.getStatusCode()).when().get("status");
    }


    @SuppressWarnings("unchecked")
    @Test
    public void testLogbookResultRemainingExceptions()
        throws InvalidParseOperationException, InvalidCreateOperationException, LogbookClientException {

        final Map<String, String> searchCriteriaMap = JsonHandler.getMapStringFromString(OPTIONS);
        final String preparedDslQuery = "";
        PowerMockito.when(DslQueryHelper.createSingleQueryDSL(searchCriteriaMap)).thenReturn(preparedDslQuery);

        PowerMockito.when(UserInterfaceTransactionManager.selectOperation(preparedDslQuery)).thenThrow(Exception.class);
        given().contentType(ContentType.JSON).body(OPTIONS).expect()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).when().post("/logbook/operations");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetLogbookResultByIdLogbookClientException()
        throws InvalidParseOperationException, LogbookClientException {
        PowerMockito.when(UserInterfaceTransactionManager.selectOperationbyId("1"))
            .thenThrow(LogbookClientException.class);

        given().param("idOperation", "1").expect().statusCode(Status.NOT_FOUND.getStatusCode()).when()
            .post("/logbook/operations/1");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetLogbookResultByIdLogbookRemainingException()
        throws InvalidParseOperationException, LogbookClientException {
        PowerMockito.when(UserInterfaceTransactionManager.selectOperationbyId("1")).thenThrow(Exception.class);

        given().param("idOperation", "1").expect().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).when()
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

        final Map<String, String> searchCriteriaMap = JsonHandler.getMapStringFromString(OPTIONS);

        // DslqQueryHelper Exceptions : InvalidParseOperationException,
        // InvalidCreateOperationException
        PowerMockito.when(DslQueryHelper.createSelectElasticsearchDSLQuery(searchCriteriaMap))
            .thenThrow(InvalidParseOperationException.class, InvalidCreateOperationException.class);

        given().contentType(ContentType.JSON).body(OPTIONS).expect().statusCode(Status.BAD_REQUEST.getStatusCode())
            .when().post("/archivesearch/units");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testArchiveSearchResultAccessExternalClientServerException() throws AccessExternalClientServerException,
        AccessExternalClientNotFoundException, InvalidParseOperationException, InvalidCreateOperationException {
        final Map<String, String> searchCriteriaMap = JsonHandler.getMapStringFromString(OPTIONS);
        final String preparedDslQuery = "";

        PowerMockito.when(DslQueryHelper.createSelectElasticsearchDSLQuery(searchCriteriaMap))
            .thenReturn(preparedDslQuery);

        // UserInterfaceTransactionManager Exception 1 :
        // AccessExternalClientServerException
        PowerMockito.when(UserInterfaceTransactionManager.searchUnits(preparedDslQuery))
            .thenThrow(AccessExternalClientServerException.class);

        given().contentType(ContentType.JSON).body(OPTIONS).expect()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).when().post("/archivesearch/units");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testArchiveSearchResultAccessExternalClientNotFoundException()
        throws AccessExternalClientServerException,
        AccessExternalClientNotFoundException, InvalidParseOperationException, InvalidCreateOperationException {
        final Map<String, String> searchCriteriaMap = JsonHandler.getMapStringFromString(OPTIONS);
        final String preparedDslQuery = "";

        PowerMockito.when(DslQueryHelper.createSelectElasticsearchDSLQuery(searchCriteriaMap))
            .thenReturn(preparedDslQuery);

        // UserInterfaceTransactionManager Exception 1 :
        // AccessExternalClientServerException
        PowerMockito.when(UserInterfaceTransactionManager.searchUnits(preparedDslQuery))
            .thenThrow(AccessExternalClientNotFoundException.class);

        given().contentType(ContentType.JSON).body(OPTIONS).expect().statusCode(Status.NOT_FOUND.getStatusCode()).when()
            .post("/archivesearch/units");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testArchiveSearchResultRemainingExceptions() throws AccessExternalClientServerException,
        AccessExternalClientNotFoundException, InvalidParseOperationException, InvalidCreateOperationException {
        final Map<String, String> searchCriteriaMap = JsonHandler.getMapStringFromString(OPTIONS);
        final String preparedDslQuery = "";

        PowerMockito.when(DslQueryHelper.createSelectElasticsearchDSLQuery(searchCriteriaMap))
            .thenReturn(preparedDslQuery);

        // UserInterfaceTransactionManager Exception 1 :
        // AccessExternalClientServerException
        PowerMockito.when(UserInterfaceTransactionManager.searchUnits(preparedDslQuery)).thenThrow(Exception.class);

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

        final Map<String, String> searchCriteriaMap = new HashMap<String, String>();
        searchCriteriaMap.put(UiConstants.SELECT_BY_ID.toString(), "1");

        // DslqQueryHelper Exceptions : InvalidParseOperationException,
        // InvalidCreateOperationException
        PowerMockito.when(DslQueryHelper.createSelectDSLQuery(searchCriteriaMap))
            .thenThrow(InvalidParseOperationException.class, InvalidCreateOperationException.class);

        given().param("id", "1").expect().statusCode(Status.BAD_REQUEST.getStatusCode()).when()
            .get("/archivesearch/unit/1");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testArchiveUnitDetailsAccessExternalClientServerException() throws AccessExternalClientServerException,
        AccessExternalClientNotFoundException, InvalidParseOperationException, InvalidCreateOperationException {
        final Map<String, String> searchCriteriaMap = new HashMap<String, String>();
        searchCriteriaMap.put(UiConstants.SELECT_BY_ID.toString(), "1");

        final String preparedDslQuery = "";

        PowerMockito.when(DslQueryHelper.createSelectDSLQuery(searchCriteriaMap)).thenReturn(preparedDslQuery);

        // UserInterfaceTransactionManager Exception 1 :
        // AccessExternalClientServerException
        PowerMockito.when(UserInterfaceTransactionManager.getArchiveUnitDetails(preparedDslQuery, "1"))
            .thenThrow(AccessExternalClientServerException.class);

        given().param("id", "1").expect().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).when()
            .get("/archivesearch/unit/1");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testArchiveUnitDetailsAccessExternalClientNotFoundException()
        throws AccessExternalClientServerException,
        AccessExternalClientNotFoundException, InvalidParseOperationException, InvalidCreateOperationException {
        final Map<String, String> searchCriteriaMap = new HashMap<String, String>();
        searchCriteriaMap.put(UiConstants.SELECT_BY_ID.toString(), "1");

        final String preparedDslQuery = "";

        PowerMockito.when(DslQueryHelper.createSelectDSLQuery(searchCriteriaMap)).thenReturn(preparedDslQuery);

        // UserInterfaceTransactionManager Exception 2 :
        // AccessExternalClientNotFoundException
        PowerMockito.when(UserInterfaceTransactionManager.getArchiveUnitDetails(preparedDslQuery, "1"))
            .thenThrow(AccessExternalClientNotFoundException.class);

        given().param("id", "1").expect().statusCode(Status.NOT_FOUND.getStatusCode()).when()
            .get("/archivesearch/unit/1");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testArchiveUnitDetailsRemainingExceptions() throws AccessExternalClientServerException,
        AccessExternalClientNotFoundException, InvalidParseOperationException, InvalidCreateOperationException {
        final Map<String, String> searchCriteriaMap = new HashMap<String, String>();
        searchCriteriaMap.put(UiConstants.SELECT_BY_ID.toString(), "1");

        final String preparedDslQuery = "";

        PowerMockito.when(DslQueryHelper.createSelectDSLQuery(searchCriteriaMap)).thenReturn(preparedDslQuery);

        // All exceptions
        PowerMockito.when(UserInterfaceTransactionManager.getArchiveUnitDetails(preparedDslQuery, "1"))
            .thenThrow(Exception.class);

        given().param("id", "1").expect().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).when()
            .get("/archivesearch/unit/1");
    }

    /**
     * Update Unit Treatment
     */

    @Test
    public void testUpdateArchiveUnitWithoutBody() {
        given().contentType(ContentType.JSON).expect().statusCode(Status.BAD_REQUEST.getStatusCode()).when()
            .put("/archiveupdate/units/1");
    }

    @SuppressWarnings({"unchecked"})
    @Test
    public void testUpdateArchiveUnitDetailsDsl()
        throws InvalidParseOperationException, InvalidCreateOperationException {

        final Map<String, String> updateCriteriaMap = new HashMap<String, String>();
        updateCriteriaMap.put(UiConstants.SELECT_BY_ID.toString(), "1");

        updateCriteriaMap.put("title", "archive1");

        // DslqQueryHelper Exceptions : InvalidParseOperationException,
        // InvalidCreateOperationException
        PowerMockito.when(DslQueryHelper.createUpdateDSLQuery(updateCriteriaMap))
            .thenThrow(InvalidParseOperationException.class, InvalidCreateOperationException.class);

        given().contentType(ContentType.JSON).body(UPDATE).expect()
            .statusCode(Status.OK.getStatusCode()).when()
            .put("/archiveupdate/units/1");
    }

    @Test
    public void testUploadSipOK() throws Exception {
        Response mockResponse = Mockito.mock(Response.class);
        final IngestExternalClient ingestClient = PowerMockito.mock(IngestExternalClient.class);
        final IngestExternalClientFactory ingestFactory = PowerMockito.mock(IngestExternalClientFactory.class);
        PowerMockito.when(ingestFactory.getClient()).thenReturn(ingestClient);
        PowerMockito.when(IngestExternalClientFactory.getInstance()).thenReturn(ingestFactory);

        final InputStream inputStreamATR = PropertiesUtils.getResourceAsStream("ATR_example.xml");
        final String xmlString = FileUtil.readInputStream(inputStreamATR);
        Mockito.doReturn("Atr").when(mockResponse).getHeaderString(anyObject());
        Mockito.doReturn(200).when(mockResponse).getStatus();
        Mockito.doReturn(xmlString).when(mockResponse).readEntity(String.class);
        Mockito.doReturn(mockResponse).when(ingestClient).upload(anyObject());

        final InputStream stream = PropertiesUtils.getResourceAsStream("SIP.zip");
        // Need for test
        IOUtils.toByteArray(stream);
        final String s = given()
            .contentType(ContentType.BINARY)
            .config(RestAssured.config().encoderConfig(
                EncoderConfig.encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false)))
            .content(stream).expect()
            .statusCode(Status.OK.getStatusCode()).when()
            .post("/ingest/upload").getHeader("Content-Disposition");
        assertEquals("attachment; filename=Atr.xml", s);
    }

    @Test
    public void givenReferentialWrongFormatWhenUploadThenThrowReferentialException() throws Exception {

        final AdminExternalClient adminManagementClient = PowerMockito.mock(AdminExternalClient.class);
        final AdminExternalClientFactory adminManagementClientFactory =
            PowerMockito.mock(AdminExternalClientFactory.class);
        PowerMockito.doThrow(new AccessExternalClientException("")).when(adminManagementClient).createDocuments(anyObject(), anyObject());
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
        PowerMockito.doReturn(Status.OK).when(adminManagementClient).createDocuments(anyObject(), anyObject());
        PowerMockito.when(adminManagementClientFactory.getClient()).thenReturn(adminManagementClient);
        PowerMockito.when(AdminExternalClientFactory.getInstance()).thenReturn(adminManagementClientFactory);

        final InputStream stream = PropertiesUtils.getResourceAsStream("FF-vitam.xml");
        // Need for test
        IOUtils.toByteArray(stream);

        given()
            .contentType(ContentType.BINARY)
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
        doThrow(new IngestExternalException("")).when(ingestClient).upload(anyObject());
        PowerMockito.when(ingestFactory.getClient()).thenReturn(ingestClient);
        PowerMockito.when(IngestExternalClientFactory.getInstance()).thenReturn(ingestFactory);

        final InputStream stream = PropertiesUtils.getResourceAsStream("SIP.zip");
        // Need for test
        IOUtils.toByteArray(stream);

        given()
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
        doReturn(JsonHandler.getFromString(OPTIONS)).when(adminClient).findDocuments(anyObject(), anyObject());
        PowerMockito.when(DslQueryHelper.createSingleQueryDSL(anyObject())).thenReturn(OPTIONS);

        PowerMockito.when(adminFactory.getClient()).thenReturn(adminClient);
        PowerMockito.when(AdminExternalClientFactory.getInstance()).thenReturn(adminFactory);

        given().contentType(ContentType.JSON).body(OPTIONS).expect()
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

        given().contentType(ContentType.JSON).body(OPTIONS).expect()
            .statusCode(Status.BAD_REQUEST.getStatusCode()).when()
            .post("/admin/formats");
    }

    @Test
    public void testSearchFormatNotFound() throws Exception {
        final AdminExternalClient adminClient = PowerMockito.mock(AdminExternalClient.class);
        final AdminExternalClientFactory adminFactory = PowerMockito.mock(AdminExternalClientFactory.class);
        PowerMockito.doThrow(new AccessExternalClientNotFoundException("")).when(adminClient).findDocuments(anyObject(), anyObject());
        PowerMockito.when(DslQueryHelper.createSingleQueryDSL(anyObject())).thenReturn(OPTIONS);

        PowerMockito.when(adminFactory.getClient()).thenReturn(adminClient);
        PowerMockito.when(AdminExternalClientFactory.getInstance()).thenReturn(adminFactory);

        given().contentType(ContentType.JSON).body(OPTIONS).expect()
            .statusCode(Status.NOT_FOUND.getStatusCode()).when()
            .post("/admin/formats");
    }

    @Test
    public void testSearchFormatByIdOK() throws Exception {
        final AdminExternalClient adminClient = PowerMockito.mock(AdminExternalClient.class);
        final AdminExternalClientFactory adminFactory = PowerMockito.mock(AdminExternalClientFactory.class);
        doReturn(JsonHandler.getFromString(OPTIONS)).when(adminClient).findDocumentById(anyObject(), anyObject());

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
        PowerMockito.doThrow(new AccessExternalClientNotFoundException("")).when(adminClient).findDocumentById(anyObject(), anyObject());

        PowerMockito.when(adminFactory.getClient()).thenReturn(adminClient);
        PowerMockito.when(AdminExternalClientFactory.getInstance()).thenReturn(adminFactory);

        given().contentType(ContentType.JSON).body(OPTIONS).expect()
            .statusCode(Status.NOT_FOUND.getStatusCode()).when()
            .post("/admin/formats/1");
    }

    @Test
    public void testDeleteFormatOK() throws Exception {
        final AdminExternalClient adminClient = PowerMockito.mock(AdminExternalClient.class);
        final AdminExternalClientFactory adminFactory = PowerMockito.mock(AdminExternalClientFactory.class);
        PowerMockito.doReturn(Status.OK).when(adminClient).deleteDocuments(anyObject());
        PowerMockito.when(DslQueryHelper.createSingleQueryDSL(anyObject())).thenReturn(OPTIONS);

        PowerMockito.when(adminFactory.getClient()).thenReturn(adminClient);
        PowerMockito.when(AdminExternalClientFactory.getInstance()).thenReturn(adminFactory);

        given().config(RestAssured.config()
            .encoderConfig(EncoderConfig.encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false)))
            .expect()
            .statusCode(Status.OK.getStatusCode()).when()
            .delete("/format/delete");
    }


    @Test
    public void testCheckFormatOK() throws Exception {
        final AdminExternalClient adminClient = PowerMockito.mock(AdminExternalClient.class);
        final AdminExternalClientFactory adminFactory = PowerMockito.mock(AdminExternalClientFactory.class);
        PowerMockito.when(adminClient.checkDocuments(anyObject(), anyObject())).thenReturn(Status.OK);
        PowerMockito.when(DslQueryHelper.createSingleQueryDSL(anyObject())).thenReturn(OPTIONS);

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
    public void testNotFoundGetArchiveObjectGroup() throws AccessExternalClientServerException,
        AccessExternalClientNotFoundException, InvalidParseOperationException, InvalidCreateOperationException {

        PowerMockito.when(UserInterfaceTransactionManager.selectObjectbyId(anyObject(), anyObject()))
            .thenThrow(new AccessExternalClientNotFoundException(""));

        given().accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
            .expect().statusCode(Status.NOT_FOUND.getStatusCode()).when()
            .get("/archiveunit/objects/idOG");
    }

    @Test
    public void testOKGetArchiveObjectGroup() throws Exception {
        final JsonNode sampleObjectGroup =
            JsonHandler.getFromFile(PropertiesUtils.findFile("sample_objectGroup_document.json"));
        PowerMockito.when(UserInterfaceTransactionManager.selectObjectbyId(anyObject(), anyObject()))
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
        PowerMockito.when(UserInterfaceTransactionManager.selectObjectbyId(anyObject(), anyObject()))
            .thenThrow(new AccessExternalClientServerException(""));

        given().accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
            .expect().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).when()
            .get("/archiveunit/objects/idOG");
    }

    @Test
    public void testUnknownErrorGetArchiveObjectGroup() throws Exception {
        PowerMockito.when(UserInterfaceTransactionManager.selectObjectbyId(anyObject(), anyObject()))
            .thenThrow(new NullPointerException(""));

        given().accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
            .expect().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).when()
            .get("/archiveunit/objects/idOG");
    }

    @Test
    public void testNotFoundGetObjectAsInputStream() throws Exception {

        PowerMockito.when(
            UserInterfaceTransactionManager.getObjectAsInputStream(anyString(), anyString(), anyString(), anyInt()))
            .thenThrow(new AccessExternalClientNotFoundException(""));

        given().accept(MediaType.APPLICATION_OCTET_STREAM)
            .body(OPTIONS_DOWNLOAD).expect().statusCode(Status.NOT_FOUND.getStatusCode()).when()
            .post("/archiveunit/objects/download/idOG");
    }

    @Test
    public void testOKGetObjectAsInputStream() throws Exception {

        PowerMockito.when(
            UserInterfaceTransactionManager.getObjectAsInputStream(anyString(), anyString(), anyString(), anyInt()))
            .thenReturn(IOUtils.toInputStream("Vitam Test"));

        given().accept(MediaType.APPLICATION_OCTET_STREAM)
            .body(OPTIONS_DOWNLOAD).expect().statusCode(Status.OK.getStatusCode()).when()
            .post("/archiveunit/objects/download/idOG");
    }

    @Test
    public void testBadRequestGetObjectAsInputStream() throws Exception {
        PowerMockito.when(
            UserInterfaceTransactionManager.getObjectAsInputStream(anyString(), anyString(), anyString(), anyInt()))
            .thenReturn(IOUtils.toInputStream("Vitam Test"));
        given().accept(MediaType.APPLICATION_OCTET_STREAM)
            .body("{usage: \"Dissemination\", version: \"KO\"}").expect().statusCode(Status.BAD_REQUEST.getStatusCode())
            .when()
            .post("/archiveunit/objects/download/idOG");
    }

    @Test
    public void testAccessServerExceptionGetObjectAsInputStream() throws Exception {
        PowerMockito.when(
            UserInterfaceTransactionManager.getObjectAsInputStream(anyString(), anyString(), anyString(), anyInt()))
            .thenThrow(new AccessExternalClientServerException(""));
        given().accept(MediaType.APPLICATION_OCTET_STREAM)
            .body(OPTIONS_DOWNLOAD).expect()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).when()
            .post("/archiveunit/objects/download/idOG");
    }

    @Test
    public void testAccessUnknownExceptionGetObjectAsInputStream() throws Exception {
        PowerMockito.when(
            UserInterfaceTransactionManager.getObjectAsInputStream(anyString(), anyString(), anyString(), anyInt()))
            .thenThrow(new NullPointerException());
        given().accept(MediaType.APPLICATION_OCTET_STREAM)
            .body(OPTIONS_DOWNLOAD).expect()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).when()
            .post("/archiveunit/objects/download/idOG");
    }

    @Test
    public void testUnitTreeOk() throws InvalidCreateOperationException, VitamException {

        PowerMockito.when(
            DslQueryHelper.createSelectUnitTreeDSLQuery(anyString(), anyObject())).thenReturn(FAKE_STRING_RETURN);
        PowerMockito.when(
            UserInterfaceTransactionManager.searchUnits(anyString())).thenReturn(FAKE_JSONNODE_RETURN);
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
        throws InvalidParseOperationException, InvalidCreateOperationException, AccessExternalClientServerException,
        AccessExternalClientNotFoundException {
        PowerMockito.when(
            DslQueryHelper.createSelectUnitTreeDSLQuery(anyString(), anyObject())).thenReturn(FAKE_STRING_RETURN);

        PowerMockito.when(
            UserInterfaceTransactionManager.searchUnits(anyString()))
            .thenThrow(AccessExternalClientServerException.class);

        given().contentType(ContentType.JSON).body(ALL_PARENTS)
            .expect().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).when()
            .post("/archiveunit/tree/1");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUnitTreeWithAccessExternalClientNotFoundException()
        throws InvalidParseOperationException, InvalidCreateOperationException, AccessExternalClientServerException,
        AccessExternalClientNotFoundException {
        PowerMockito.when(
            DslQueryHelper.createSelectUnitTreeDSLQuery(anyString(), anyObject())).thenReturn(FAKE_STRING_RETURN);

        PowerMockito.when(
            UserInterfaceTransactionManager.searchUnits(anyString()))
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
            DslQueryHelper.createSelectUnitTreeDSLQuery(anyString(), anyObject())).thenReturn(FAKE_STRING_RETURN);
        PowerMockito.when(
            UserInterfaceTransactionManager.searchUnits(anyString())).thenReturn(FAKE_JSONNODE_RETURN);
        PowerMockito.when(
            UserInterfaceTransactionManager.buildUnitTree(anyString(), anyObject())).thenThrow(VitamException.class);

        given().contentType(ContentType.JSON).body(ALL_PARENTS)
            .expect().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).when()
            .post("/archiveunit/tree/1");
    }

    /** rules Management ********/

    @Test
    public void givenReferentialWrongFormatRulesWhenUploadThenThrowReferentialException() throws Exception {

        final AdminExternalClient adminManagementClient = PowerMockito.mock(AdminExternalClient.class);
        final AdminExternalClientFactory adminExternalClientFactory =
            PowerMockito.mock(AdminExternalClientFactory.class);
        PowerMockito.doThrow(new AccessExternalClientException("")).when(adminManagementClient).createDocuments(anyObject(), anyObject());
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
        PowerMockito.doReturn(Status.OK).when(adminManagementClient).createDocuments(anyObject(), anyObject());
        PowerMockito.when(adminManagementClientFactory.getClient()).thenReturn(adminManagementClient);
        PowerMockito.when(AdminExternalClientFactory.getInstance()).thenReturn(adminManagementClientFactory);

        final InputStream stream =
            PropertiesUtils.getResourceAsStream("jeu_donnees_OK_regles_CSV.csv");
        // Need for test
        IOUtils.toByteArray(stream);

        given()
            .contentType(ContentType.BINARY)
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
        doReturn(JsonHandler.getFromString(OPTIONS)).when(adminClient).findDocuments(anyObject(), anyObject());
        PowerMockito.when(DslQueryHelper.createSingleQueryDSL(anyObject())).thenReturn(OPTIONS);

        PowerMockito.when(adminFactory.getClient()).thenReturn(adminClient);
        PowerMockito.when(AdminExternalClientFactory.getInstance()).thenReturn(adminFactory);

        given().contentType(ContentType.JSON).body(OPTIONS).expect()
            .statusCode(Status.OK.getStatusCode()).when()
            .post("/admin/rules");
    }

    @Test
    public void testSearchRuleBadRequest() throws Exception {
        final AdminExternalClient adminClient = PowerMockito.mock(AdminExternalClient.class);
        final AdminExternalClientFactory adminFactory = PowerMockito.mock(AdminExternalClientFactory.class);
        doReturn(JsonHandler.getFromString(OPTIONS)).when(adminClient).findDocuments(anyObject(), anyObject());
        PowerMockito.when(DslQueryHelper.createSingleQueryDSL(anyObject()))
            .thenThrow(new InvalidParseOperationException(""));

        PowerMockito.when(adminFactory.getClient()).thenReturn(adminClient);
        PowerMockito.when(AdminExternalClientFactory.getInstance()).thenReturn(adminFactory);

        given().contentType(ContentType.JSON).body(OPTIONS).expect()
            .statusCode(Status.BAD_REQUEST.getStatusCode()).when()
            .post("/admin/rules");
    }

    @Test
    public void testSearchRuleNotFound() throws Exception {
        final AdminExternalClient adminClient = PowerMockito.mock(AdminExternalClient.class);
        final AdminExternalClientFactory adminFactory = PowerMockito.mock(AdminExternalClientFactory.class);
        PowerMockito.doThrow(new AccessExternalClientNotFoundException("")).when(adminClient).findDocuments(anyObject(), anyObject());
        PowerMockito.when(DslQueryHelper.createSingleQueryDSL(anyObject())).thenReturn(OPTIONS);

        PowerMockito.when(adminFactory.getClient()).thenReturn(adminClient);
        PowerMockito.when(AdminExternalClientFactory.getInstance()).thenReturn(adminFactory);

        given().contentType(ContentType.JSON).body(OPTIONS).expect()
            .statusCode(Status.NOT_FOUND.getStatusCode()).when()
            .post("/admin/rules");
    }

    @Test
    public void testSearchRuleByIdNotFound() throws Exception {
        final AdminExternalClient adminClient = PowerMockito.mock(AdminExternalClient.class);
        final AdminExternalClientFactory adminFactory = PowerMockito.mock(AdminExternalClientFactory.class);
        PowerMockito.doThrow(new AccessExternalClientNotFoundException("")).when(adminClient).findDocumentById(anyObject(), anyObject());

        PowerMockito.when(adminFactory.getClient()).thenReturn(adminClient);
        PowerMockito.when(AdminExternalClientFactory.getInstance()).thenReturn(adminFactory);

        given().contentType(ContentType.JSON).body(OPTIONS).expect()
            .statusCode(Status.NOT_FOUND.getStatusCode()).when()
            .post("/admin/rules/1");
    }

    @Test
    public void testDeleteRulesFileOK() throws Exception {
        final AdminExternalClient adminClient = PowerMockito.mock(AdminExternalClient.class);
        final AdminExternalClientFactory adminFactory = PowerMockito.mock(AdminExternalClientFactory.class);
        PowerMockito.doReturn(Status.OK).when(adminClient).deleteDocuments(anyObject());
        PowerMockito.when(DslQueryHelper.createSingleQueryDSL(anyObject())).thenReturn(OPTIONS);

        PowerMockito.when(adminFactory.getClient()).thenReturn(adminClient);
        PowerMockito.when(AdminExternalClientFactory.getInstance()).thenReturn(adminFactory);

        given().config(RestAssured.config()
            .encoderConfig(EncoderConfig.encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false)))
            .expect()
            .statusCode(Status.OK.getStatusCode()).when()
            .delete("/rules/delete");
    }


    @Test
    public void testCheckRulesFileOK() throws Exception {
        final AdminExternalClient adminClient = PowerMockito.mock(AdminExternalClient.class);
        final AdminExternalClientFactory adminFactory = PowerMockito.mock(AdminExternalClientFactory.class);
        PowerMockito.when(adminClient.checkDocuments(anyObject(), anyObject())).thenReturn(Status.OK);
        PowerMockito.when(DslQueryHelper.createSingleQueryDSL(anyObject())).thenReturn(OPTIONS);

        PowerMockito.when(adminFactory.getClient()).thenReturn(adminClient);
        PowerMockito.when(AdminExternalClientFactory.getInstance()).thenReturn(adminFactory);

        final InputStream stream = PropertiesUtils.getResourceAsStream("jeu_donnees_KO_regles_CSV_Parameters.csv");
        // Need for test
        IOUtils.toByteArray(stream);

        given()
            .contentType(ContentType.BINARY)
            .config(RestAssured.config().encoderConfig(
                EncoderConfig.encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false)))
            .content(stream).expect()
            .statusCode(Status.OK.getStatusCode()).when()
            .post("/rules/check");
    }

    @Test
    public void testGetUnitLifeCycleByIdOk() throws InvalidParseOperationException, LogbookClientException {
        final JsonNode result = FAKE_JSONNODE_RETURN;

        PowerMockito.when(UserInterfaceTransactionManager.selectUnitLifeCycleById(FAKE_UNIT_LF_ID)).thenReturn(result);

        given().param("id_lc", FAKE_UNIT_LF_ID).expect().statusCode(Status.OK.getStatusCode()).when()
            .get("/unitlifecycles/" + FAKE_UNIT_LF_ID);
    }

    @Test
    public void testGetObjectGroupLifeCycleByIdOk() throws InvalidParseOperationException, LogbookClientException {
        final JsonNode result = FAKE_JSONNODE_RETURN;

        PowerMockito.when(UserInterfaceTransactionManager.selectObjectGroupLifeCycleById(FAKE_OBG_LF_ID))
            .thenReturn(result);

        given().param("id_lc", FAKE_OBG_LF_ID).expect().statusCode(Status.OK.getStatusCode()).when()
            .get("/objectgrouplifecycles/" + FAKE_OBG_LF_ID);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetUnitLifeCycleByIdWithBadRequestWhenInvalidParseOperationException()
        throws InvalidParseOperationException, InvalidCreateOperationException, LogbookClientException {
        PowerMockito.when(UserInterfaceTransactionManager.selectUnitLifeCycleById(FAKE_UNIT_LF_ID))
            .thenThrow(InvalidParseOperationException.class);

        given().param("id_lc", FAKE_UNIT_LF_ID).expect().statusCode(Status.BAD_REQUEST.getStatusCode()).when()
            .get("/unitlifecycles/" + FAKE_UNIT_LF_ID);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetUnitLifeCycleByIdWithNotFoundResponseWhenLogbookClientException()
        throws InvalidParseOperationException, InvalidCreateOperationException, LogbookClientException {
        PowerMockito.when(UserInterfaceTransactionManager.selectUnitLifeCycleById(FAKE_UNIT_LF_ID))
            .thenThrow(LogbookClientException.class);

        given().param("id_lc", FAKE_UNIT_LF_ID).expect().statusCode(Status.NOT_FOUND.getStatusCode()).when()
            .get("/unitlifecycles/" + FAKE_UNIT_LF_ID);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetUnitLifeCycleByIdWithInternalServerErrorWhenUnknownException()
        throws InvalidParseOperationException, InvalidCreateOperationException, LogbookClientException {
        PowerMockito.when(UserInterfaceTransactionManager.selectUnitLifeCycleById(FAKE_UNIT_LF_ID))
            .thenThrow(NullPointerException.class);

        given().param("id_lc", FAKE_UNIT_LF_ID).expect().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).when()
            .get("/unitlifecycles/" + FAKE_UNIT_LF_ID);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetObjectGroupLifeCycleByIdWithBadRequestWhenInvalidParseOperationException()
        throws InvalidParseOperationException, InvalidCreateOperationException, LogbookClientException {
        PowerMockito.when(UserInterfaceTransactionManager.selectObjectGroupLifeCycleById(FAKE_OBG_LF_ID))
            .thenThrow(InvalidParseOperationException.class);

        given().param("id_lc", FAKE_OBG_LF_ID).expect().statusCode(Status.BAD_REQUEST.getStatusCode()).when()
            .get("/objectgrouplifecycles/" + FAKE_OBG_LF_ID);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetObjectGroupLifeCycleByIdWithNotFoundResponseWhenLogbookClientException()
        throws InvalidParseOperationException, InvalidCreateOperationException, LogbookClientException {
        PowerMockito.when(UserInterfaceTransactionManager.selectObjectGroupLifeCycleById(FAKE_OBG_LF_ID))
            .thenThrow(LogbookClientException.class);

        given().param("id_lc", FAKE_OBG_LF_ID).expect().statusCode(Status.NOT_FOUND.getStatusCode()).when()
            .get("/objectgrouplifecycles/" + FAKE_OBG_LF_ID);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetOjectGroupLifeCycleByIdWithInternalServerErrorWhenUnknownException()
        throws InvalidParseOperationException, InvalidCreateOperationException, LogbookClientException {
        PowerMockito.when(UserInterfaceTransactionManager.selectObjectGroupLifeCycleById(FAKE_OBG_LF_ID))
            .thenThrow(NullPointerException.class);

        given().param("id_lc", FAKE_OBG_LF_ID).expect().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).when()
            .get("/objectgrouplifecycles/" + FAKE_OBG_LF_ID);
    }

    @Test
    public void testGetLogbookStatisticsWithSuccess() throws LogbookClientException, InvalidParseOperationException {
        PowerMockito.when(UserInterfaceTransactionManager.selectOperationbyId(FAKE_OPERATION_ID))
            .thenReturn(sampleLogbookOperation);
        given().param("id_op", FAKE_OPERATION_ID).expect().statusCode(Status.OK.getStatusCode()).when()
            .get("/stat/" + FAKE_OPERATION_ID);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetLogbookStatisticsWithNotFoundWhenLogbookClientException()
        throws LogbookClientException, InvalidParseOperationException {
        PowerMockito.when(UserInterfaceTransactionManager.selectOperationbyId(FAKE_OPERATION_ID))
            .thenThrow(LogbookClientException.class);
        given().param("id_op", FAKE_OPERATION_ID).expect().statusCode(Status.NOT_FOUND.getStatusCode()).when()
            .get("/stat/" + FAKE_OPERATION_ID);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetLogbookStatisticsWithInternalServerErrorWhenInvalidParseOperationException()
        throws LogbookClientException, InvalidParseOperationException {
        PowerMockito.when(UserInterfaceTransactionManager.selectOperationbyId(FAKE_OPERATION_ID))
            .thenThrow(InvalidParseOperationException.class);
        given().param("id_op", FAKE_OPERATION_ID).expect().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
            .when()
            .get("/stat/" + FAKE_OPERATION_ID);
    }

    @Test
    public void testGetAvailableFilesListWithSuccess() {
        given().expect().statusCode(Status.OK.getStatusCode())
            .when()
            .get("/upload/fileslist");
    }

    @Test
    public void testUploadFileFromServerSuccess() throws Exception {
        final IngestExternalClient ingestClient = PowerMockito.mock(IngestExternalClient.class);
        final IngestExternalClientFactory ingestFactory = PowerMockito.mock(IngestExternalClientFactory.class);

        PowerMockito.when(ingestFactory.getClient()).thenReturn(ingestClient);
        PowerMockito.when(IngestExternalClientFactory.getInstance()).thenReturn(ingestFactory);
        Mockito.doReturn(Response.status(Status.OK).header(GlobalDataRest.X_REQUEST_ID, FAKE_OPERATION_ID)
            .build()).when(ingestClient).upload(anyObject());

        given().param("file_name", "SIP.zip").expect().statusCode(Status.OK.getStatusCode())
            .when()
            .get("/upload/SIP.zip");
    }

    @Test
    public void testUploadFileFromServerWithInternalServerWhenFileNotFound() throws Exception {
        final IngestExternalClient ingestClient = PowerMockito.mock(IngestExternalClient.class);
        final IngestExternalClientFactory ingestFactory = PowerMockito.mock(IngestExternalClientFactory.class);

        PowerMockito.when(ingestFactory.getClient()).thenReturn(ingestClient);
        PowerMockito.when(IngestExternalClientFactory.getInstance()).thenReturn(ingestFactory);
        Mockito.doReturn(Response.status(Status.OK).header(GlobalDataRest.X_REQUEST_ID, FAKE_OPERATION_ID)
            .build()).when(ingestClient).upload(anyObject());

        given().param("file_name", "SIP_NOT_FOUND.zip").expect()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
            .when()
            .get("/upload/SIP_NOT_FOUND.zip");
    }

    @Test
    public void testUploadFileFromServerWithInternalServerWhenVitamException() throws Exception {
        final IngestExternalClient ingestClient = PowerMockito.mock(IngestExternalClient.class);
        final IngestExternalClientFactory ingestFactory = PowerMockito.mock(IngestExternalClientFactory.class);

        PowerMockito.when(ingestFactory.getClient()).thenReturn(ingestClient);
        PowerMockito.when(IngestExternalClientFactory.getInstance()).thenReturn(ingestFactory);
        Mockito.doThrow(VitamException.class).when(ingestClient).upload(anyObject());

        given().param("file_name", "SIP.zip").expect()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
            .when()
            .get("/upload/SIP.zip");
    }

    @Test
    public void testGetAvailableFilesListWithInternalSererWhenBadSipDirectory() {
        final String currentSipDirectory = application.getConfiguration().getSipDirectory();
        application.getConfiguration().setSipDirectory("SIP_DIRECTORY_NOT_FOUND");

        given().expect().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
            .when()
            .get("/upload/fileslist");

        // Reset WebApplicationConfiguration
        application.getConfiguration().setSipDirectory(currentSipDirectory);
    }

    @Test
    public void testGetAvailableFilesListWithInternalSererWhenNotConfiguredSipDirectory() {
        final String currentSipDirectory = application.getConfiguration().getSipDirectory();
        application.getConfiguration().setSipDirectory(null);

        given().expect().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
            .when()
            .get("/upload/fileslist");

        // Reset WebApplicationConfiguration
        application.getConfiguration().setSipDirectory(currentSipDirectory);
    }

    @Test
    public void testUploadFileFromServerWithInternalServerWhenNotConfiguredSipDirectory() throws VitamException {
        final String currentSipDirectory = application.getConfiguration().getSipDirectory();
        application.getConfiguration().setSipDirectory(null);

        given().param("file_name", "SIP.zip").expect()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
            .when()
            .get("/upload/SIP.zip");

        // Reset WebApplicationConfiguration
        application.getConfiguration().setSipDirectory(currentSipDirectory);
    }

    @Test
    public void testSearchFundsRegisterOK() throws Exception {
        PowerMockito.when(UserInterfaceTransactionManager.findAccessionRegisterSummary(anyObject()))
        .thenReturn(sampleLogbookOperation);

        given().contentType(ContentType.JSON).body(OPTIONS).expect()
            .statusCode(Status.OK.getStatusCode()).when()
            .post("/admin/accession-register");
    }

    @Test
    public void testSearchFundsRegisterBadRequest() throws Exception {
        PowerMockito.when(UserInterfaceTransactionManager.findAccessionRegisterSummary(anyObject()))
            .thenThrow(new InvalidParseOperationException(""));

        given().contentType(ContentType.JSON).body(OPTIONS).expect()
            .statusCode(Status.BAD_REQUEST.getStatusCode()).when()
            .post("/admin/accession-register");
    }

    @Test
    public void testGetAccessionRegisterDetailOK() throws Exception {
        PowerMockito.when(UserInterfaceTransactionManager.findAccessionRegisterSummary(anyObject()))
        .thenReturn(sampleLogbookOperation);

        given().contentType(ContentType.JSON).body(OPTIONS).expect()
            .statusCode(Status.OK.getStatusCode()).when()
            .post("/admin/accession-register/1/accession-register-detail");
    }

    @Test
    public void testGetAccessionRegisterDetailBadRequest() throws Exception {
        PowerMockito.when(UserInterfaceTransactionManager.findAccessionRegisterDetail(anyObject(), anyObject()))
            .thenThrow(new InvalidParseOperationException(""));

        given().contentType(ContentType.JSON).body(OPTIONS).expect()
            .statusCode(Status.BAD_REQUEST.getStatusCode()).when()
            .post("/admin/accession-register/1/accession-register-detail");
    }
}
