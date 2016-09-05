/**
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
 */
package fr.gouv.vitam.ihmdemo.appserver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.config.EncoderConfig;
import com.jayway.restassured.http.ContentType;
import com.mongodb.util.JSON;

import fr.gouv.vitam.access.common.exception.AccessClientNotFoundException;
import fr.gouv.vitam.access.common.exception.AccessClientServerException;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.ihmdemo.core.DslQueryHelper;
import fr.gouv.vitam.ihmdemo.core.UiConstants;
import fr.gouv.vitam.ihmdemo.core.UserInterfaceTransactionManager;
import fr.gouv.vitam.ingest.external.api.IngestExternalException;
import fr.gouv.vitam.ingest.external.client.IngestExternalClient;
import fr.gouv.vitam.ingest.external.client.IngestExternalClientFactory;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.operations.client.LogbookClient;
import fr.gouv.vitam.logbook.operations.client.LogbookClientFactory;
import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jayway.restassured.RestAssured.given;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.net.ssl.*")
@PrepareForTest({UserInterfaceTransactionManager.class, DslQueryHelper.class, LogbookClientFactory.class,
    IngestExternalClientFactory.class, AdminManagementClientFactory.class})
public class WebApplicationResourceTest {

    private static final String DEFAULT_WEB_APP_CONTEXT = "/ihm-demo";
    private static final String DEFAULT_STATIC_CONTENT = "webapp";
    private static final String OPTIONS = "{name: \"myName\"}";
    private static final String OPTIONS_DOWNLOAD = "{usage: \"Dissemination\", version: 1}";
    private static final String UPDATE = "{title: \"myarchive\"}";
    private static final String DEFAULT_HOST = "localhost";
    private static JunitHelper junitHelper;
    private static int port;

    @BeforeClass
    public static void setup() throws Exception {
        junitHelper = new JunitHelper();
        port = junitHelper.findAvailablePort();
        ServerApplication.run(new WebApplicationConfig().setPort(port).setBaseUrl(DEFAULT_WEB_APP_CONTEXT)
            .setServerHost(DEFAULT_HOST).setStaticContent(DEFAULT_STATIC_CONTENT));
        RestAssured.port = port;
        RestAssured.basePath = DEFAULT_WEB_APP_CONTEXT + "/v1/api";
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        ServerApplication.stop();
        junitHelper.releasePort(port);
    }

    @Before
    public void initStaticMock() {
        PowerMockito.mockStatic(UserInterfaceTransactionManager.class);
        PowerMockito.mockStatic(DslQueryHelper.class);
        PowerMockito.mockStatic(IngestExternalClientFactory.class);
        PowerMockito.mockStatic(AdminManagementClientFactory.class);
    }

    @Test
    public void givenEmptyPayloadWhenSearchOperationsThenReturnBadRequest() {
        given().contentType(ContentType.JSON).body("{}").expect().statusCode(Status.BAD_REQUEST.getStatusCode()).when()
            .post("/logbook/operations");
    }

    @Test
    public void givenNoArchiveUnitWhenSearchOperationsThenReturnOK() {
        given().contentType(ContentType.JSON).body(OPTIONS).expect().statusCode(Status.OK.getStatusCode()).when()
            .post("/archivesearch/units");
    }

    @Test
    public void testSuccessStatus() {
        given().expect().statusCode(Status.OK.getStatusCode()).when().get("status");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testLogbookResultLogbookClientException()
        throws InvalidParseOperationException, InvalidCreateOperationException, LogbookClientException {

        PowerMockito.mockStatic(LogbookClientFactory.class);
        LogbookClient logbookClient = PowerMockito.mock(LogbookClient.class);
        LogbookClientFactory logbookFactory = PowerMockito.mock(LogbookClientFactory.class);
        PowerMockito.when(LogbookClientFactory.getInstance()).thenReturn(logbookFactory);
        PowerMockito.when(LogbookClientFactory.getInstance().getLogbookOperationClient()).thenReturn(logbookClient);

        Map<String, String> searchCriteriaMap = JsonHandler.getMapStringFromString(OPTIONS);
        String preparedDslQuery = "";
        PowerMockito.when(DslQueryHelper.createSingleQueryDSL(searchCriteriaMap)).thenReturn(preparedDslQuery);

        PowerMockito.when(logbookClient.selectOperation(preparedDslQuery)).thenThrow(LogbookClientException.class);
        given().contentType(ContentType.JSON).body(OPTIONS).expect().statusCode(Status.NOT_FOUND.getStatusCode()).when()
            .post("/logbook/operations");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testLogbookResultRemainingExceptions()
        throws InvalidParseOperationException, InvalidCreateOperationException, LogbookClientException {

        PowerMockito.mockStatic(LogbookClientFactory.class);
        LogbookClient logbookClient = PowerMockito.mock(LogbookClient.class);
        LogbookClientFactory logbookFactory = PowerMockito.mock(LogbookClientFactory.class);
        PowerMockito.when(LogbookClientFactory.getInstance()).thenReturn(logbookFactory);
        PowerMockito.when(LogbookClientFactory.getInstance().getLogbookOperationClient()).thenReturn(logbookClient);

        Map<String, String> searchCriteriaMap = JsonHandler.getMapStringFromString(OPTIONS);
        String preparedDslQuery = "";
        PowerMockito.when(DslQueryHelper.createSingleQueryDSL(searchCriteriaMap)).thenReturn(preparedDslQuery);

        PowerMockito.when(logbookClient.selectOperation(preparedDslQuery)).thenThrow(Exception.class);
        given().contentType(ContentType.JSON).body(OPTIONS).expect()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).when().post("/logbook/operations");
    }

    @Test
    public void testSuccessGetLogbookResultById() throws InvalidParseOperationException, LogbookClientException {
        PowerMockito.mockStatic(LogbookClientFactory.class);
        LogbookClient logbookClient = PowerMockito.mock(LogbookClient.class);
        LogbookClientFactory logbookFactory = PowerMockito.mock(LogbookClientFactory.class);
        PowerMockito.when(LogbookClientFactory.getInstance()).thenReturn(logbookFactory);
        PowerMockito.when(LogbookClientFactory.getInstance().getLogbookOperationClient()).thenReturn(logbookClient);

        JsonNode result = JsonHandler.getFromString("{}");

        PowerMockito.when(logbookClient.selectOperationbyId("1")).thenReturn(result);
        given().param("idOperation", "1").expect().statusCode(Status.OK.getStatusCode()).when()
            .post("/logbook/operations/1");
    }

    @Test
    public void testSuccessGetLogbookResult()
        throws InvalidParseOperationException, LogbookClientException, InvalidCreateOperationException {

        PowerMockito.mockStatic(LogbookClientFactory.class);
        LogbookClient logbookClient = PowerMockito.mock(LogbookClient.class);
        LogbookClientFactory logbookFactory = PowerMockito.mock(LogbookClientFactory.class);
        PowerMockito.when(LogbookClientFactory.getInstance()).thenReturn(logbookFactory);
        PowerMockito.when(LogbookClientFactory.getInstance().getLogbookOperationClient()).thenReturn(logbookClient);

        Map<String, String> searchCriteriaMap = JsonHandler.getMapStringFromString(OPTIONS);
        String preparedDslQuery = "";
        PowerMockito.when(DslQueryHelper.createSingleQueryDSL(searchCriteriaMap)).thenReturn(preparedDslQuery);

        JsonNode result = JsonHandler.getFromString("{}");
        PowerMockito.when(logbookClient.selectOperation(preparedDslQuery)).thenReturn(result);
        given().contentType(ContentType.JSON).body(OPTIONS).expect().statusCode(Status.OK.getStatusCode()).when()
            .post("/logbook/operations");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetLogbookResultByIdLogbookClientException()
        throws InvalidParseOperationException, LogbookClientException {
        PowerMockito.mockStatic(LogbookClientFactory.class);
        LogbookClient logbookClient = PowerMockito.mock(LogbookClient.class);
        LogbookClientFactory logbookFactory = PowerMockito.mock(LogbookClientFactory.class);
        PowerMockito.when(LogbookClientFactory.getInstance()).thenReturn(logbookFactory);
        PowerMockito.when(LogbookClientFactory.getInstance().getLogbookOperationClient()).thenReturn(logbookClient);
        PowerMockito.when(logbookClient.selectOperationbyId("1")).thenThrow(LogbookClientException.class);

        given().param("idOperation", "1").expect().statusCode(Status.NOT_FOUND.getStatusCode()).when()
            .post("/logbook/operations/1");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetLogbookResultByIdLogbookRemainingException()
        throws InvalidParseOperationException, LogbookClientException {
        PowerMockito.mockStatic(LogbookClientFactory.class);
        LogbookClient logbookClient = PowerMockito.mock(LogbookClient.class);
        LogbookClientFactory logbookFactory = PowerMockito.mock(LogbookClientFactory.class);
        PowerMockito.when(LogbookClientFactory.getInstance()).thenReturn(logbookFactory);
        PowerMockito.when(LogbookClientFactory.getInstance().getLogbookOperationClient()).thenReturn(logbookClient);
        PowerMockito.when(logbookClient.selectOperationbyId("1")).thenThrow(Exception.class);

        given().param("idOperation", "1").expect().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).when()
            .post("/logbook/operations/1");
    }

    @Test
    public void testGetLogbookResultByIdLogbookRemainingIllrgalArgumentException()
        throws InvalidParseOperationException, LogbookClientException, InvalidCreateOperationException {
        PowerMockito.mockStatic(LogbookClientFactory.class);
        LogbookClient logbookClient = PowerMockito.mock(LogbookClient.class);
        LogbookClientFactory logbookFactory = PowerMockito.mock(LogbookClientFactory.class);
        PowerMockito.when(LogbookClientFactory.getInstance()).thenReturn(logbookFactory);
        PowerMockito.when(LogbookClientFactory.getInstance().getLogbookOperationClient()).thenReturn(logbookClient);

        given().contentType(ContentType.JSON).expect().statusCode(Status.BAD_REQUEST.getStatusCode()).when()
            .post("/logbook/operations/1");
    }

    @SuppressWarnings({"unchecked"})
    @Test
    public void testArchiveSearchResultDslQueryHelperExceptions()
        throws InvalidParseOperationException, InvalidCreateOperationException {

        Map<String, String> searchCriteriaMap = JsonHandler.getMapStringFromString(OPTIONS);

        // DslqQueryHelper Exceptions : InvalidParseOperationException,
        // InvalidCreateOperationException
        PowerMockito.when(DslQueryHelper.createSelectDSLQuery(searchCriteriaMap))
            .thenThrow(InvalidParseOperationException.class, InvalidCreateOperationException.class);

        given().contentType(ContentType.JSON).body(OPTIONS).expect().statusCode(Status.BAD_REQUEST.getStatusCode())
            .when().post("/archivesearch/units");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testArchiveSearchResultAccessClientServerException() throws AccessClientServerException,
        AccessClientNotFoundException, InvalidParseOperationException, InvalidCreateOperationException {
        Map<String, String> searchCriteriaMap = JsonHandler.getMapStringFromString(OPTIONS);
        String preparedDslQuery = "";

        PowerMockito.when(DslQueryHelper.createSelectDSLQuery(searchCriteriaMap)).thenReturn(preparedDslQuery);

        // UserInterfaceTransactionManager Exception 1 :
        // AccessClientServerException
        PowerMockito.when(UserInterfaceTransactionManager.searchUnits(preparedDslQuery))
            .thenThrow(AccessClientServerException.class);

        given().contentType(ContentType.JSON).body(OPTIONS).expect()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).when().post("/archivesearch/units");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testArchiveSearchResultAccessClientNotFoundException() throws AccessClientServerException,
        AccessClientNotFoundException, InvalidParseOperationException, InvalidCreateOperationException {
        Map<String, String> searchCriteriaMap = JsonHandler.getMapStringFromString(OPTIONS);
        String preparedDslQuery = "";

        PowerMockito.when(DslQueryHelper.createSelectDSLQuery(searchCriteriaMap)).thenReturn(preparedDslQuery);

        // UserInterfaceTransactionManager Exception 1 :
        // AccessClientServerException
        PowerMockito.when(UserInterfaceTransactionManager.searchUnits(preparedDslQuery))
            .thenThrow(AccessClientNotFoundException.class);

        given().contentType(ContentType.JSON).body(OPTIONS).expect().statusCode(Status.NOT_FOUND.getStatusCode()).when()
            .post("/archivesearch/units");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testArchiveSearchResultRemainingExceptions() throws AccessClientServerException,
        AccessClientNotFoundException, InvalidParseOperationException, InvalidCreateOperationException {
        Map<String, String> searchCriteriaMap = JsonHandler.getMapStringFromString(OPTIONS);
        String preparedDslQuery = "";

        PowerMockito.when(DslQueryHelper.createSelectDSLQuery(searchCriteriaMap)).thenReturn(preparedDslQuery);

        // UserInterfaceTransactionManager Exception 1 :
        // AccessClientServerException
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

        Map<String, String> searchCriteriaMap = new HashMap<String, String>();
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
    public void testArchiveUnitDetailsAccessClientServerException() throws AccessClientServerException,
        AccessClientNotFoundException, InvalidParseOperationException, InvalidCreateOperationException {
        Map<String, String> searchCriteriaMap = new HashMap<String, String>();
        searchCriteriaMap.put(UiConstants.SELECT_BY_ID.toString(), "1");

        String preparedDslQuery = "";

        PowerMockito.when(DslQueryHelper.createSelectDSLQuery(searchCriteriaMap)).thenReturn(preparedDslQuery);

        // UserInterfaceTransactionManager Exception 1 :
        // AccessClientServerException
        PowerMockito.when(UserInterfaceTransactionManager.getArchiveUnitDetails(preparedDslQuery, "1"))
            .thenThrow(AccessClientServerException.class);

        given().param("id", "1").expect().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).when()
            .get("/archivesearch/unit/1");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testArchiveUnitDetailsAccessClientNotFoundException() throws AccessClientServerException,
        AccessClientNotFoundException, InvalidParseOperationException, InvalidCreateOperationException {
        Map<String, String> searchCriteriaMap = new HashMap<String, String>();
        searchCriteriaMap.put(UiConstants.SELECT_BY_ID.toString(), "1");

        String preparedDslQuery = "";

        PowerMockito.when(DslQueryHelper.createSelectDSLQuery(searchCriteriaMap)).thenReturn(preparedDslQuery);

        // UserInterfaceTransactionManager Exception 2 :
        // AccessClientNotFoundException
        PowerMockito.when(UserInterfaceTransactionManager.getArchiveUnitDetails(preparedDslQuery, "1"))
            .thenThrow(AccessClientNotFoundException.class);

        given().param("id", "1").expect().statusCode(Status.NOT_FOUND.getStatusCode()).when()
            .get("/archivesearch/unit/1");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testArchiveUnitDetailsRemainingExceptions() throws AccessClientServerException,
        AccessClientNotFoundException, InvalidParseOperationException, InvalidCreateOperationException {
        Map<String, String> searchCriteriaMap = new HashMap<String, String>();
        searchCriteriaMap.put(UiConstants.SELECT_BY_ID.toString(), "1");

        String preparedDslQuery = "";

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

        Map<String, String> updateCriteriaMap = new HashMap<String, String>();
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

        IngestExternalClient ingestClient = PowerMockito.mock(IngestExternalClient.class);
        IngestExternalClientFactory ingestFactory = PowerMockito.mock(IngestExternalClientFactory.class);
        doNothing().when(ingestClient).upload(anyObject());
        PowerMockito.when(ingestFactory.getIngestExternalClient()).thenReturn(ingestClient);
        PowerMockito.when(IngestExternalClientFactory.getInstance()).thenReturn(ingestFactory);

        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("SIP.zip");
        byte[] bytes = IOUtils.toByteArray(stream); // need for the test !

        given()
            .contentType(ContentType.BINARY)
            .config(RestAssured.config().encoderConfig(
                EncoderConfig.encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false)))
            .content(stream).expect()
            .statusCode(Status.OK.getStatusCode()).when()
            .post("/ingest/upload");
    }

    @Test
    public void givenReferentialWrongFormatWhenUploadThenThrowReferentialException() throws Exception {

        AdminManagementClient adminManagementClient = PowerMockito.mock(AdminManagementClient.class);
        AdminManagementClientFactory adminManagementClientFactory =
            PowerMockito.mock(AdminManagementClientFactory.class);
        doThrow(ReferentialException.class).when(adminManagementClient).importFormat(anyObject());
        // doNothing().when(adminManagementClient).importFormat(anyObject());
        PowerMockito.when(adminManagementClientFactory.getAdminManagementClient()).thenReturn(adminManagementClient);
        PowerMockito.when(AdminManagementClientFactory.getInstance()).thenReturn(adminManagementClientFactory);

        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("FF-vitam-ko.fake");
        byte[] bytes = IOUtils.toByteArray(stream); // need for the test !

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

        AdminManagementClient adminManagementClient = PowerMockito.mock(AdminManagementClient.class);
        AdminManagementClientFactory adminManagementClientFactory =
            PowerMockito.mock(AdminManagementClientFactory.class);
        doNothing().when(adminManagementClient).importFormat(anyObject());
        PowerMockito.when(adminManagementClientFactory.getAdminManagementClient()).thenReturn(adminManagementClient);
        PowerMockito.when(AdminManagementClientFactory.getInstance()).thenReturn(adminManagementClientFactory);

        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("FF-vitam.xml");
        byte[] bytes = IOUtils.toByteArray(stream); // need for the test !

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

        IngestExternalClient ingestClient = PowerMockito.mock(IngestExternalClient.class);
        IngestExternalClientFactory ingestFactory = PowerMockito.mock(IngestExternalClientFactory.class);
        doThrow(new IngestExternalException("")).when(ingestClient).upload(anyObject());
        PowerMockito.when(ingestFactory.getIngestExternalClient()).thenReturn(ingestClient);
        PowerMockito.when(IngestExternalClientFactory.getInstance()).thenReturn(ingestFactory);

        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("SIP.zip");
        byte[] bytes = IOUtils.toByteArray(stream); // need for the test !

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
        AdminManagementClient adminClient = PowerMockito.mock(AdminManagementClient.class);
        AdminManagementClientFactory adminFactory = PowerMockito.mock(AdminManagementClientFactory.class);
        doReturn(JsonHandler.getFromString(OPTIONS)).when(adminClient).getFormats(anyObject());
        PowerMockito.when(DslQueryHelper.createSingleQueryDSL(anyObject())).thenReturn(OPTIONS);

        PowerMockito.when(adminFactory.getAdminManagementClient()).thenReturn(adminClient);
        PowerMockito.when(AdminManagementClientFactory.getInstance()).thenReturn(adminFactory);

        given().contentType(ContentType.JSON).body(OPTIONS).expect()
            .statusCode(Status.OK.getStatusCode()).when()
            .post("/admin/formats");
    }

    @Test
    public void testSearchFormatBadRequest() throws Exception {
        AdminManagementClient adminClient = PowerMockito.mock(AdminManagementClient.class);
        AdminManagementClientFactory adminFactory = PowerMockito.mock(AdminManagementClientFactory.class);
        doReturn(JsonHandler.getFromString(OPTIONS)).when(adminClient).getFormats(anyObject());
        PowerMockito.when(DslQueryHelper.createSingleQueryDSL(anyObject()))
            .thenThrow(new InvalidParseOperationException(""));

        PowerMockito.when(adminFactory.getAdminManagementClient()).thenReturn(adminClient);
        PowerMockito.when(AdminManagementClientFactory.getInstance()).thenReturn(adminFactory);

        given().contentType(ContentType.JSON).body(OPTIONS).expect()
            .statusCode(Status.BAD_REQUEST.getStatusCode()).when()
            .post("/admin/formats");
    }

    @Test
    public void testSearchFormatNotFound() throws Exception {
        AdminManagementClient adminClient = PowerMockito.mock(AdminManagementClient.class);
        AdminManagementClientFactory adminFactory = PowerMockito.mock(AdminManagementClientFactory.class);
        doThrow(new ReferentialException("")).when(adminClient).getFormats(anyObject());
        PowerMockito.when(DslQueryHelper.createSingleQueryDSL(anyObject())).thenReturn(OPTIONS);

        PowerMockito.when(adminFactory.getAdminManagementClient()).thenReturn(adminClient);
        PowerMockito.when(AdminManagementClientFactory.getInstance()).thenReturn(adminFactory);

        given().contentType(ContentType.JSON).body(OPTIONS).expect()
            .statusCode(Status.NOT_FOUND.getStatusCode()).when()
            .post("/admin/formats");
    }

    @Test
    public void testSearchFormatByIdOK() throws Exception {
        AdminManagementClient adminClient = PowerMockito.mock(AdminManagementClient.class);
        AdminManagementClientFactory adminFactory = PowerMockito.mock(AdminManagementClientFactory.class);
        doReturn(JsonHandler.getFromString(OPTIONS)).when(adminClient).getFormatByID(anyObject());

        PowerMockito.when(adminFactory.getAdminManagementClient()).thenReturn(adminClient);
        PowerMockito.when(AdminManagementClientFactory.getInstance()).thenReturn(adminFactory);

        given().contentType(ContentType.JSON).body(OPTIONS).expect()
            .statusCode(Status.OK.getStatusCode()).when()
            .post("/admin/formats/1");
    }

    @Test
    public void testSearchFormatByIdNotFound() throws Exception {
        AdminManagementClient adminClient = PowerMockito.mock(AdminManagementClient.class);
        AdminManagementClientFactory adminFactory = PowerMockito.mock(AdminManagementClientFactory.class);
        doThrow(new ReferentialException("")).when(adminClient).getFormatByID(anyObject());

        PowerMockito.when(adminFactory.getAdminManagementClient()).thenReturn(adminClient);
        PowerMockito.when(AdminManagementClientFactory.getInstance()).thenReturn(adminFactory);

        given().contentType(ContentType.JSON).body(OPTIONS).expect()
            .statusCode(Status.NOT_FOUND.getStatusCode()).when()
            .post("/admin/formats/1");
    }

    @Test
    public void testDeleteFormatOK() throws Exception {
        AdminManagementClient adminClient = PowerMockito.mock(AdminManagementClient.class);
        AdminManagementClientFactory adminFactory = PowerMockito.mock(AdminManagementClientFactory.class);
        doNothing().when(adminClient).deleteFormat();
        PowerMockito.when(DslQueryHelper.createSingleQueryDSL(anyObject())).thenReturn(OPTIONS);

        PowerMockito.when(adminFactory.getAdminManagementClient()).thenReturn(adminClient);
        PowerMockito.when(AdminManagementClientFactory.getInstance()).thenReturn(adminFactory);

        given().config(RestAssured.config()
            .encoderConfig(EncoderConfig.encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false)))
            .expect()
            .statusCode(Status.OK.getStatusCode()).when()
            .delete("/format/delete");
    }


    @Test
    public void testCheckFormatOK() throws Exception {
        AdminManagementClient adminClient = PowerMockito.mock(AdminManagementClient.class);
        AdminManagementClientFactory adminFactory = PowerMockito.mock(AdminManagementClientFactory.class);
        doNothing().when(adminClient).checkFormat(anyObject());
        PowerMockito.when(DslQueryHelper.createSingleQueryDSL(anyObject())).thenReturn(OPTIONS);

        PowerMockito.when(adminFactory.getAdminManagementClient()).thenReturn(adminClient);
        PowerMockito.when(AdminManagementClientFactory.getInstance()).thenReturn(adminFactory);

        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("FF-vitam.xml");
        byte[] bytes = IOUtils.toByteArray(stream); // need for the test !

        given()
            .contentType(ContentType.BINARY)
            .config(RestAssured.config().encoderConfig(
                EncoderConfig.encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false)))
            .content(stream).expect()
            .statusCode(Status.OK.getStatusCode()).when()
            .post("/format/check");
    }

    @Test
    public void testNotFoundGetArchiveObjectGroup() throws AccessClientServerException,
        AccessClientNotFoundException, InvalidParseOperationException, InvalidCreateOperationException {

        PowerMockito.when(UserInterfaceTransactionManager.selectObjectbyId(anyObject(), anyObject()))
            .thenThrow(new AccessClientNotFoundException(""));

        given().accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
            .expect().statusCode(Status.NOT_FOUND.getStatusCode()).when()
            .get("/archiveunit/objects/idOG");
    }

    @Test
    public void testOKGetArchiveObjectGroup() throws Exception {
        JsonNode sampleObjectGroup =
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
            .thenThrow(new AccessClientServerException(""));
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
            .thenThrow(new AccessClientNotFoundException(""));

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
            .thenThrow(new AccessClientServerException(""));
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
    
    
}
