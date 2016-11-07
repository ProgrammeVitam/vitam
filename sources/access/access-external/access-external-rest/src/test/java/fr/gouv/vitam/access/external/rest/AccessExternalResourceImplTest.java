package fr.gouv.vitam.access.external.rest;

import static com.jayway.restassured.RestAssured.given;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;

import fr.gouv.vitam.access.internal.client.AccessInternalClient;
import fr.gouv.vitam.access.internal.client.AccessInternalClientFactory;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalClientNotFoundException;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalClientServerException;
import fr.gouv.vitam.access.internal.common.model.AccessInternalConfiguration;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.BasicVitamServer;
import fr.gouv.vitam.common.server.VitamServer;
import fr.gouv.vitam.common.server.application.junit.ResponseHelper;


@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.net.ssl.*")
@PrepareForTest({AccessInternalClientFactory.class})
public class AccessExternalResourceImplTest {
    @Mock
    private AccessInternalConfiguration configuration;
    @InjectMocks
    private AccessExternalResourceImpl accessExternalResourceImpl;
    private static final String ACCESS_CONF = "access-external-test.conf";
    // URI
    private static final String ACCESS_RESOURCE_URI = "access-external/v1";
    private static final String ACCESS_UNITS_ID_URI = "/units/xyz";
    private static AccessExternalApplication application; 
    private static VitamServer vitamServer;

    // LOGGER
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AccessExternalResourceImplTest.class);
    private static JunitHelper junitHelper = JunitHelper.getInstance();;
    private static int port = junitHelper.findAvailablePort();;
    private static AccessInternalClient clientAccessInternal;

    private static final String BODY_TEST = "{$query: {$eq: {\"aa\" : \"vv\" }}, $projection: {}, $filter: {}}";
    static String request = "{ $query: {} }, $projection: {}, $filter: {} }";
    static String bad_request = "{ $query:\\ {} }, $projection: {}, $filter: {} }";
    static String good_id = "goodId";
    static String bad_id = "badId";

    public static String X_HTTP_METHOD_OVERRIDE = "X-HTTP-Method-Override";
    private static final String QUERY_TEST = "{ $query : [ { $eq : { \"title\" : \"test\" } } ], " +
        " $filter : { $orderby : [ \"#id\" ] }," +
        " $projection : {$fields : {\"#id\" : 1, \"title\":2, \"transacdate\":1}}" +
        " }";

    private static final String QUERY_SIMPLE_TEST = "{ $query : [ { $eq : { 'title' : 'test' } } ] }";

    private static final String BAD_QUERY_TEST = "{ $query ; [ { $eq : { 'title' : 'test' } } ] }";

    private static final String DATA =
        "{ \"_id\": \"aeaqaaaaaeaaaaakaarp4akuuf2ldmyaaaaq\", " + "\"data\": \"data1\" }";

    private static final String DATA2 =
        "{ \"_id\": \"aeaqaaaaaeaaaaakaarp4akuuf2ldmyaaaab\"," + "\"data\": \"data2\" }";

    private static final String DATA_TEST =
        "{ \"_id\": \"aeaqaaaaaeaaaaakaarp4akuuf2ldmyaaaaq\", " + "\"title\": \"test\"," + "\"data\": \"data1\" }";


    private static final String ID = "identifier4";
    // LOGGER
    private static final String ACCESS_UNITS_URI = "/units";

    private static final String ID_UNIT = "identifier5";

    private static final String OBJECT_ID = "objectId";
    private static final String OBJECTS_URI = "/objects/";
    private static Map<String, Object> headers;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        junitHelper = JunitHelper.getInstance();
        port = junitHelper.findAvailablePort();
        try {
            application = new AccessExternalApplication(ACCESS_CONF);
            application.start();
            RestAssured.port = port;
            RestAssured.basePath = ACCESS_RESOURCE_URI;

            LOGGER.debug("Beginning tests");
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
            throw new IllegalStateException(
                "Cannot start the Access Application Server", e);
        }

    }

    @Before
    public void setUpBefore() throws Exception {

        PowerMockito.mockStatic(AccessInternalClientFactory.class);
        clientAccessInternal = PowerMockito.mock(AccessInternalClient.class);
        AccessInternalClientFactory clientAccessInternalFactory = PowerMockito.mock(AccessInternalClientFactory.class);
        PowerMockito.when(AccessInternalClientFactory.getInstance()).thenReturn(clientAccessInternalFactory);
        PowerMockito.when(AccessInternalClientFactory.getInstance().getClient())
        .thenReturn(clientAccessInternal);

    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        LOGGER.debug("Ending tests");
        try {
            if (vitamServer != null) {
                ((BasicVitamServer) vitamServer).stop();
            }
            junitHelper.releasePort(port);
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
        }
    }

    /**
     * 
     * @param data
     * @return query DSL with Options
     */
    private static final String buildDSLWithOptions(String query, String data) {
        return "{ $roots : [ '' ], $query : [ " + query + " ], $data : " + data + " }";
    }

    /**
     * 
     * @param data
     * @return query DSL with id as Roots
     */

    private static final String buildDSLWithRoots(String data) {
        return "{ $roots : [ " + data + " ], $query : [ '' ], $data : " + data + " }";
    }

    private Map<String, Object> getStreamHeaders() {
        Map<String, Object> headers = new HashMap<>();
        headers.put(GlobalDataRest.X_TENANT_ID, "0");
        headers.put(GlobalDataRest.X_QUALIFIER, "qualif");
        headers.put(GlobalDataRest.X_VERSION, 1);
        return headers;
    }

    private void setHeaders(String key, Object value) {
        headers.put(key, value);
    }

    /**
     * Checks if the send parameter doesn't have Json format
     *
     * @throws Exception
     */
    @Test
    public void givenStartedServer_WhenRequestNotJson_ThenReturnError_UnsupportedMediaType() throws Exception {
        given()
        .contentType(ContentType.XML).header(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, "GET")
        .body(buildDSLWithOptions(QUERY_TEST, DATA2))
        .when().post(ACCESS_UNITS_URI).then().statusCode(Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode());
    }

    /**
     * Checks if the send parameter doesn't have Json format
     * 
     * @throws Exception
     */
    @Test
    public void givenStartedServer_WhenRequestNotJson_ThenReturnError_SelectById_UnsupportedMediaType()
        throws Exception {
        given()
        .contentType(ContentType.XML).header(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, "GET")
        .body(buildDSLWithRoots(ID))
        .when().post(ACCESS_UNITS_ID_URI).then().statusCode(Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode());
    }

    @Test
    public void given_queryThatThrowException_when_updateByID() {

        given()
        .contentType(ContentType.JSON)
        .body(buildDSLWithOptions(QUERY_SIMPLE_TEST, DATA))
        .when()
        .put("/units/" + ID)
        .then()
        .statusCode(Status.OK.getStatusCode());
    }

    @Test
    public void given_bad_header_when_SelectByID_thenReturn_Not_allowed() {

        given()
        .contentType(ContentType.JSON)
        .header(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, "ABC")
        .body(BODY_TEST)
        .when()
        .post("/units/" + ID_UNIT)
        .then()
        .statusCode(Status.UNAUTHORIZED.getStatusCode());
    }

    @Test
    public void given_pathWithId_when_get_SelectByID()
        throws AccessInternalClientServerException, AccessInternalClientNotFoundException, InvalidParseOperationException {
        PowerMockito.when(clientAccessInternal.selectObjectbyId(QUERY_TEST, ID_UNIT))
        .thenReturn(JsonHandler.getFromString(DATA_TEST));

        given()
        .contentType(ContentType.JSON)
        .header(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, "GET")
        .body(QUERY_TEST)
        .when()
        .post("/units/" + ID_UNIT)
        .then()
        .statusCode(Status.OK.getStatusCode());
    }

    @Test
    public void testAccessUnits() throws Exception {

        reset(clientAccessInternal);
        PowerMockito.when(clientAccessInternal.selectUnits(anyObject()))
        .thenReturn(JsonHandler.getFromString(DATA_TEST));
        given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(QUERY_TEST)
        .when()
        .get(ACCESS_UNITS_URI)
        .then().statusCode(Status.OK.getStatusCode());
        given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(QUERY_TEST)
        .header(X_HTTP_METHOD_OVERRIDE, "GET")
        .when()
        .post(ACCESS_UNITS_URI)
        .then().statusCode(Status.OK.getStatusCode());
    }

    @Test
    public void testErrorSelectUnitsById()
        throws AccessInternalClientServerException, AccessInternalClientNotFoundException, InvalidParseOperationException {

        PowerMockito.when(clientAccessInternal.selectUnitbyId(BAD_QUERY_TEST, good_id))
        .thenThrow(InvalidParseOperationException.class);
        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
        .body(BAD_QUERY_TEST).when().get("units/goodId").then()
        .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        PowerMockito.when(clientAccessInternal.selectUnitbyId(BAD_QUERY_TEST, bad_id))
        .thenThrow(AccessInternalClientNotFoundException.class);
        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
        .body(BAD_QUERY_TEST).when().get("units/badId").then()
        .statusCode(Status.NOT_FOUND.getStatusCode());

        PowerMockito.when(clientAccessInternal.selectUnitbyId("INTERAL_SEVER_ERROR", bad_id))
        .thenThrow(AccessInternalClientServerException.class);
        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
        .body("INTERAL_SEVER_ERROR").when().get("units/badId").then()
        .statusCode(Status.UNAUTHORIZED.getStatusCode());
    }


    @Test
    public void testErrorsUpdateUnitsById()
        throws AccessInternalClientServerException, AccessInternalClientNotFoundException, InvalidParseOperationException {

        PowerMockito.when(clientAccessInternal.updateUnitbyId(BAD_QUERY_TEST, good_id))
        .thenThrow(InvalidParseOperationException.class);
        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
        .body(BAD_QUERY_TEST).when().put("units/goodId").then()
        .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        PowerMockito.when(clientAccessInternal.updateUnitbyId(BAD_QUERY_TEST, bad_id))
        .thenThrow(AccessInternalClientNotFoundException.class);
        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
        .body(BAD_QUERY_TEST).when().put("units/badId").then()
        .statusCode(Status.NOT_FOUND.getStatusCode());

        PowerMockito.when(clientAccessInternal.updateUnitbyId("INTERAL_SEVER_ERROR", bad_id))
        .thenThrow(AccessInternalClientServerException.class);
        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
        .body("INTERAL_SEVER_ERROR").when().put("units/badId").then()
        .statusCode(Status.UNAUTHORIZED.getStatusCode());
    }


    @Test
    public void testErrorsSelectUnits()
        throws AccessInternalClientServerException, AccessInternalClientNotFoundException, InvalidParseOperationException {

        given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body("{BAD_QUERY_TEST_UNITS}")
        .when()
        .get(ACCESS_UNITS_URI)
        .then()
        .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        reset(clientAccessInternal);
        PowerMockito.when(clientAccessInternal.selectUnits(anyObject()))
        .thenThrow(AccessInternalClientServerException.class);

        given()
        .contentType(ContentType.JSON)
        .body(BODY_TEST)
        .header(X_HTTP_METHOD_OVERRIDE, "GET")
        .when()
        .post(ACCESS_UNITS_URI)
        .then()
        .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());

        reset(clientAccessInternal);
        PowerMockito.when(clientAccessInternal.selectUnits(anyObject()))
        .thenThrow(AccessInternalClientNotFoundException.class);

        given()
        .contentType(ContentType.JSON)
        .body(BODY_TEST)
        .header(X_HTTP_METHOD_OVERRIDE, "GET")
        .when()
        .post(ACCESS_UNITS_URI)
        .then()
        .statusCode(Status.NOT_FOUND.getStatusCode());

    }


    @Test
    public void getObjectGroupPost() throws Exception {
        reset(clientAccessInternal);
        JsonNode result = JsonHandler.getFromString(BODY_TEST);
        when(clientAccessInternal.selectObjectbyId(anyString(), anyString())).thenReturn(result);

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).body(BODY_TEST)
        .headers(getStreamHeaders()).header(GlobalDataRest.X_HTTP_METHOD_OVERRIDE,
            "GET")
        .when().post(OBJECTS_URI + OBJECT_ID).then()
        .statusCode(Status.OK.getStatusCode()).contentType(MediaType.APPLICATION_JSON);

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).body(BODY_TEST)
        .headers(getStreamHeaders()).header(GlobalDataRest.X_HTTP_METHOD_OVERRIDE,
            "PUT")
        .when().post(OBJECTS_URI + OBJECT_ID).then()
        .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        reset(clientAccessInternal);
        when(clientAccessInternal.selectObjectbyId(anyString(), anyString()))
        .thenThrow(new AccessInternalClientServerException(""));

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).body(BODY_TEST)
        .headers(getStreamHeaders()).header(GlobalDataRest.X_HTTP_METHOD_OVERRIDE,
            "GET")
        .when().post(OBJECTS_URI + OBJECT_ID).then()
        .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());

    }

    @Test
    public void testGetObjectStream() throws Exception {

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Length", "4");
        reset(clientAccessInternal);
        Response response = ResponseHelper.getOutboundResponse(Status.OK, new ByteArrayInputStream("test".getBytes()), 
            MediaType.APPLICATION_OCTET_STREAM, headers);

        PowerMockito.when(clientAccessInternal.getObject(anyString(), anyString(), anyString(), anyInt()))
        .thenReturn(response);

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM)
        .headers(getStreamHeaders()).body(BODY_TEST).when().get("units/goodId/object").then()
        .statusCode(Status.OK.getStatusCode());

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM)
        .body(BODY_TEST).when().get("units/goodId/object").then()
        .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM)
        .headers(getStreamHeaders()).body(BODY_TEST).when().post("units/goodId/object").then()
        .statusCode(Status.PRECONDITION_FAILED.getStatusCode());


        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM)
        .headers(getStreamHeaders()).header(GlobalDataRest.X_HTTP_METHOD_OVERRIDE,
            "PUT").body(BODY_TEST).when().post("units/goodId/object").then()
        .statusCode(Status.METHOD_NOT_ALLOWED.getStatusCode());

    }

    @Test
    public void testErrorsGetObjects()
        throws AccessInternalClientServerException, AccessInternalClientNotFoundException, InvalidParseOperationException {

        PowerMockito.when(clientAccessInternal.getObject(anyString(), anyString(), anyString(), anyInt()))
        .thenThrow(new InvalidParseOperationException(""));

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM)
        .headers(getStreamHeaders()).header(GlobalDataRest.X_HTTP_METHOD_OVERRIDE,
            "GET").body(BODY_TEST).when().get("units/goodId/object").then()
        .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

}
