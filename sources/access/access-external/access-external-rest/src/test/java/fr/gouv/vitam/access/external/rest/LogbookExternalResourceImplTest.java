package fr.gouv.vitam.access.external.rest;

import static com.jayway.restassured.RestAssured.given;
import static org.mockito.Matchers.anyObject;

import javax.ws.rs.core.Response.Status;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;

import fr.gouv.vitam.access.external.api.AccessExtAPI;
import fr.gouv.vitam.access.internal.client.AccessInternalClient;
import fr.gouv.vitam.access.internal.client.AccessInternalClientFactory;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.client.ClientMockResultHelper;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.server.BasicVitamServer;
import fr.gouv.vitam.common.server.VitamServer;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;


@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.net.ssl.*", "javax.management.*"})
@PrepareForTest({AccessInternalClientFactory.class})
public class LogbookExternalResourceImplTest {

    private static final String TRACEABILITY_OPERATION_ID = "op_id";

    private static final String ACCESS_CONF = "access-external-test.conf";

    // URI
    private static final String ACCESS_RESOURCE_URI = "access-external/v1";

    private static AccessExternalMain application;
    private static VitamServer vitamServer;

    // LOGGER
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookExternalResourceImplTest.class);

    private static JunitHelper junitHelper = JunitHelper.getInstance();
    private static int port = junitHelper.findAvailablePort();
    private static AccessInternalClient accessInternalClient;

    private static final String TENANT_ID = "0";
    private static final String UNEXISTING_TENANT_ID = "25";

    private static final String OPERATIONS_URI = "/operations";
    private static final String OPERATION_ID_URI = "/{id_op}";

    private static final String CHECK_TRACEABILITY_OPERATION_URI = AccessExtAPI.TRACEABILITY_API + "/check";
    private static final String TRACEABILITY_OPERATION_BASE_URI = AccessExtAPI.TRACEABILITY_API + "/";

    private static final String MOCK_SELECT_RESULT = "{\"_id\": \"aedqaaaaacaam7mxaaaamakvhiv4rsiaaaaq\"," +
        "    \"evId\": \"aedqaaaaacaam7mxaaaamakvhiv4rsqaaaaq\"," +
        "    \"evType\": \"Process_SIP_unitary\"," +
        "    \"evDateTime\": \"2016-06-10T11:56:35.914\"," +
        "    \"evIdProc\": \"aedqaaaaacaam7mxaaaamakvhiv4rsiaaaaq\"," +
        "    \"evTypeProc\": \"INGEST\"," +
        "    \"outcome\": \"STARTED\"," +
        "    \"outDetail\": null," +
        "    \"outMessg\": \"SIP entry : SIP.zip\"," +
        "    \"agId\": {\"name\":\"ingest_1\",\"role\":\"ingest\",\"pid\":425367}," +
        "    \"agIdApp\": null," +
        "    \"evIdAppSession\": null," +
        "    \"evIdReq\": \"aedqaaaaacaam7mxaaaamakvhiv4rsiaaaaq\"," +
        "    \"agIdSubm\": null," +
        "    \"agIdOrig\": null," +
        "    \"obId\": null," +
        "    \"obIdReq\": null," +
        "    \"obIdIn\": null," +
        "    \"events\": []}";

    private static final String BODY_TEST = "{$query: {$eq: {\"aa\" : \"vv\" }}, $projection: {}, $filter: {}}";
    private static final String BODY_TEST_WITH_ID =
        "{$query: {$eq: {\"evIdProc\": \"aedqaaaaacaam7mxaaaamakvhiv4rsiaaaaq\" }}, $projection: {}, $filter: {}}";
    static String request = "{ $query: {} , $projection: {}, $filter: {} }";
    static String bad_request = "{ $query: \"bad_request\" , $projection: {}, $filter: {} }";
    static String good_id = "goodId";
    static String bad_id = "badId";

    public static String X_HTTP_METHOD_OVERRIDE = "X-HTTP-Method-Override";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        junitHelper = JunitHelper.getInstance();
        port = junitHelper.findAvailablePort();
        try {
            application = new AccessExternalMain(ACCESS_CONF);
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
        accessInternalClient = PowerMockito.mock(AccessInternalClient.class);
        final AccessInternalClientFactory clientAccessInternalFactory =
            PowerMockito.mock(AccessInternalClientFactory.class);
        PowerMockito.when(AccessInternalClientFactory.getInstance()).thenReturn(clientAccessInternalFactory);
        PowerMockito.when(AccessInternalClientFactory.getInstance().getClient())
            .thenReturn(accessInternalClient);

        PowerMockito.when(accessInternalClient.selectOperation(anyObject()))
            .thenReturn(new RequestResponseOK().addResult(ClientMockResultHelper.getLogbookResults()));

        PowerMockito.when(accessInternalClient.selectOperationById(anyObject(), anyObject()))
            .thenReturn(new RequestResponseOK().addResult(ClientMockResultHelper.getLogbookOperation()));

        PowerMockito.when(accessInternalClient.selectUnitLifeCycleById(anyObject(), anyObject()))
            .thenReturn(new RequestResponseOK().addResult(ClientMockResultHelper.getLogbookOperation()));

        PowerMockito.when(accessInternalClient.selectObjectGroupLifeCycleById(anyObject(), anyObject()))
            .thenReturn(new RequestResponseOK().addResult(ClientMockResultHelper.getLogbookOperation()));

        // Mock AccessInternal response for check TRACEABILITY operation request
        PowerMockito.when(accessInternalClient.checkTraceabilityOperation(JsonHandler.getFromString(request)))
            .thenReturn(ClientMockResultHelper.checkOperationTraceability());

        // Mock AccessInternal response for download TRACEABILITY operation request
        PowerMockito.when(accessInternalClient.downloadTraceabilityFile(TRACEABILITY_OPERATION_ID))
            .thenReturn(ClientMockResultHelper.getObjectStream());

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


    @Test
    public void testSelectOperations_PreconditionFailed() throws Exception {
        given()
            .contentType(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "ABC")
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(JsonHandler.getFromString(BODY_TEST))
            .when()
            .post(OPERATIONS_URI)
            .then()
            .statusCode(Status.METHOD_NOT_ALLOWED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "ABC")
            .and().header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .body(JsonHandler.getFromString(BODY_TEST))
            .when()
            .post(OPERATIONS_URI)
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "ABC")
            .body(JsonHandler.getFromString(BODY_TEST))
            .when()
            .post(OPERATIONS_URI)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void testSelectOperationById_PreconditionFailed() throws Exception {
        given()
            .contentType(ContentType.JSON)
            .body(JsonHandler.getFromString(request))
            .header(X_HTTP_METHOD_OVERRIDE, "ABC")
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .pathParam("id_op", 1)
            .when()
            .post(OPERATIONS_URI + OPERATION_ID_URI)
            .then()
            .statusCode(Status.METHOD_NOT_ALLOWED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(JsonHandler.getFromString(request))
            .header(X_HTTP_METHOD_OVERRIDE, "ABC")
            .and().header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .pathParam("id_op", 1)
            .when()
            .post(OPERATIONS_URI + OPERATION_ID_URI)
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(JsonHandler.getFromString(request))
            .header(X_HTTP_METHOD_OVERRIDE, "ABC")
            .pathParam("id_op", 1)
            .when()
            .post(OPERATIONS_URI + OPERATION_ID_URI)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void testSelectLifecycleUnits_PreconditionFailed() throws Exception {
        PowerMockito.when(accessInternalClient.selectUnitLifeCycleById(bad_id, JsonHandler.getFromString(BODY_TEST)))
            .thenThrow(new LogbookClientException(""));
        PowerMockito.when(accessInternalClient.selectUnitLifeCycle(JsonHandler.getFromString(BODY_TEST)))
            .thenThrow(new LogbookClientException(""));
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .param("id_lc", bad_id)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get("/unitlifecycles/" + bad_id)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .param("id_lc", bad_id)
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when()
            .get("/unitlifecycles/" + bad_id)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .param("id_lc", bad_id)
            .when()
            .get("/unitlifecycles/" + bad_id)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void testSelectLifecycleOGById_PreconditionFailed() throws Exception {
        PowerMockito
            .when(accessInternalClient.selectObjectGroupLifeCycleById(bad_id, JsonHandler.getFromString(request)))
            .thenThrow(new LogbookClientException(""));
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .param("id_lc", bad_id)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get("/objectgrouplifecycles/" + bad_id)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .param("id_lc", bad_id)
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when()
            .get("/objectgrouplifecycles/" + bad_id)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .param("id_lc", bad_id)
            .when()
            .get("/objectgrouplifecycles/" + bad_id)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void testSelectOperations_InternalServerError() throws Exception {
        PowerMockito.when(accessInternalClient.selectOperation(JsonHandler.getFromString(bad_request)))
            .thenThrow(new LogbookClientException(""));
        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(JsonHandler.getFromString(bad_request))
            .when()
            .get(OPERATIONS_URI)
            .then()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .body(JsonHandler.getFromString(bad_request))
            .when()
            .get(OPERATIONS_URI)
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(JsonHandler.getFromString(bad_request))
            .when()
            .get(OPERATIONS_URI)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void testSelectOperationById_InternalServerError() throws Exception {
        PowerMockito.when(accessInternalClient.selectOperationById(bad_id, JsonHandler.getFromString(request)))
            .thenThrow(new LogbookClientException(""));

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .pathParam("id_op", bad_id)
            .when()
            .get(OPERATIONS_URI + OPERATION_ID_URI)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .pathParam("id_op", bad_id)
            .when()
            .get(OPERATIONS_URI + OPERATION_ID_URI)
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .pathParam("id_op", bad_id)
            .when()
            .get(OPERATIONS_URI + OPERATION_ID_URI)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void testGetSelectOperations() throws Exception {
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(JsonHandler.getFromString(request))
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get(OPERATIONS_URI)
            .then().statusCode(Status.OK.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(JsonHandler.getFromString(request))
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when()
            .get(OPERATIONS_URI)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(JsonHandler.getFromString(request))
            .when()
            .get(OPERATIONS_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(JsonHandler.getFromString(request))
            .when()
            .get(OPERATIONS_URI)
            .then().statusCode(Status.OK.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and().header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .body(JsonHandler.getFromString(request))
            .when()
            .get(OPERATIONS_URI)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .body(JsonHandler.getFromString(request))
            .when()
            .get(OPERATIONS_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void testSelectOperations() throws Exception {

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(JsonHandler.getFromString(request))
            .when()
            .get(OPERATIONS_URI)
            .then().statusCode(Status.OK.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .body(JsonHandler.getFromString(request))
            .when()
            .get(OPERATIONS_URI)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(JsonHandler.getFromString(request))
            .when()
            .get(OPERATIONS_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(JsonHandler.getFromString(request))
            .when()
            .post(OPERATIONS_URI)
            .then().statusCode(Status.OK.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and().header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .body(JsonHandler.getFromString(request))
            .when()
            .post(OPERATIONS_URI)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .body(JsonHandler.getFromString(request))
            .when()
            .post(OPERATIONS_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void testSelectOperationsById() throws Exception {
        given()
            .contentType(ContentType.JSON)
            .body(JsonHandler.getFromString(BODY_TEST_WITH_ID))
            .pathParam("id_op", good_id)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get(OPERATIONS_URI + OPERATION_ID_URI)
            .then().statusCode(Status.OK.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(JsonHandler.getFromString(BODY_TEST_WITH_ID))
            .pathParam("id_op", good_id)
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when()
            .get(OPERATIONS_URI + OPERATION_ID_URI)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(JsonHandler.getFromString(BODY_TEST_WITH_ID))
            .pathParam("id_op", good_id)
            .when()
            .get(OPERATIONS_URI + OPERATION_ID_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(JsonHandler.getFromString(BODY_TEST_WITH_ID))
            .pathParam("id_op", good_id)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get(OPERATIONS_URI + OPERATION_ID_URI)
            .then().statusCode(Status.OK.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(JsonHandler.getFromString(BODY_TEST_WITH_ID))
            .pathParam("id_op", good_id)
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when()
            .get(OPERATIONS_URI + OPERATION_ID_URI)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(JsonHandler.getFromString(BODY_TEST_WITH_ID))
            .pathParam("id_op", good_id)
            .when()
            .get(OPERATIONS_URI + OPERATION_ID_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void testPostSelectOperationsById() throws Exception {
        given()
            .contentType(ContentType.JSON)
            .body(JsonHandler.getFromString(BODY_TEST_WITH_ID))
            .pathParam("id_op", good_id)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get(OPERATIONS_URI + OPERATION_ID_URI)
            .then().statusCode(Status.OK.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(JsonHandler.getFromString(BODY_TEST_WITH_ID))
            .pathParam("id_op", good_id)
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when()
            .get(OPERATIONS_URI + OPERATION_ID_URI)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(JsonHandler.getFromString(BODY_TEST_WITH_ID))
            .pathParam("id_op", good_id)
            .when()
            .get(OPERATIONS_URI + OPERATION_ID_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, "GET")
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(JsonHandler.getFromString(BODY_TEST_WITH_ID))
            .pathParam("id_op", good_id)
            .when()
            .post(OPERATIONS_URI + OPERATION_ID_URI)
            .then().statusCode(Status.OK.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, "GET")
            .and().header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .body(JsonHandler.getFromString(BODY_TEST_WITH_ID))
            .pathParam("id_op", good_id)
            .when()
            .post(OPERATIONS_URI + OPERATION_ID_URI)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, "GET")
            .body(JsonHandler.getFromString(BODY_TEST_WITH_ID))
            .pathParam("id_op", good_id)
            .when()
            .post(OPERATIONS_URI + OPERATION_ID_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void testLifeCycleSelect() throws Exception {
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(JsonHandler.getFromString(BODY_TEST_WITH_ID))
            .param("id_lc", good_id)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get("/unitlifecycles/" + good_id)
            .then().statusCode(Status.OK.getStatusCode());


        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(JsonHandler.getFromString(BODY_TEST_WITH_ID))
            .param("id_lc", good_id)
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when()
            .get("/unitlifecycles/" + good_id)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(JsonHandler.getFromString(BODY_TEST_WITH_ID))
            .param("id_lc", good_id)
            .when()
            .get("/unitlifecycles/" + good_id)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(JsonHandler.getFromString(BODY_TEST_WITH_ID))
            .param("id_lc", good_id)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get("/objectgrouplifecycles/" + good_id)
            .then().statusCode(Status.OK.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(JsonHandler.getFromString(BODY_TEST_WITH_ID))
            .param("id_lc", good_id)
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when()
            .get("/objectgrouplifecycles/" + good_id)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(JsonHandler.getFromString(BODY_TEST_WITH_ID))
            .param("id_lc", good_id)
            .when()
            .get("/objectgrouplifecycles/" + good_id)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

}
