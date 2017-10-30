package fr.gouv.vitam.access.external.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import fr.gouv.vitam.access.external.api.AccessExtAPI;
import fr.gouv.vitam.access.internal.client.AccessInternalClient;
import fr.gouv.vitam.access.internal.client.AccessInternalClientFactory;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.client.ClientMockResultHelper;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.parser.request.single.SelectParserSingle;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.server.VitamServer;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ws.rs.core.Response.Status;

import static com.jayway.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyObject;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;


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
        "    \"agIdExt\": null," +
        "    \"obId\": null," +
        "    \"obIdReq\": null," +
        "    \"obIdIn\": null," +
        "    \"events\": []}";

    private static final String BODY_TEST = "{$query: {$eq: {\"aa\" : \"vv\" }}, $projection: {}, $filter: {}}";
    private static final String BODY_TEST_WITH_ID =
        "{$projection: {}}";
    private static final String BODY_TEST_WITH_BAD_REQUEST_FOR_BYID_DSL_REQUEST =
        "{$query: {$eq: {\"evIdProc\": \"aedqaaaaacaam7mxaaaamakvhiv4rsiaaaaq\" }}, $projection: {}, $filter: {}}";
    private static String request = "{ $query: {} , $projection: {}, $filter: {} }";
    private static String bad_request = "{ $query: \"bad_request\" , $projection: {}, $filter: {} }";
    private static String good_request =
        "{ $query: {\"$match\": {\"Description\": \"Zimbabwe\" }} , $projection: {}, $filter: {} }";
    private static String wrong_request_format =
        "{ $query: {\"$match\": {\"Description\": \"ragnar\",\"Title\": \"lockbrok\"}} , $projection: {}, $filter: {} }";
    private static String good_id = "goodId";
    private static String bad_id = "badId";

    public static String X_HTTP_METHOD_OVERRIDE = "X-HTTP-Method-Override";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        junitHelper = JunitHelper.getInstance();
        port = junitHelper.findAvailablePort();
        try {
            application = new AccessExternalMain(ACCESS_CONF, BusinessApplicationTest.class, null);
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
        accessInternalClient = mock(AccessInternalClient.class);
        final AccessInternalClientFactory clientAccessInternalFactory =
            mock(AccessInternalClientFactory.class);
        when(AccessInternalClientFactory.getInstance()).thenReturn(clientAccessInternalFactory);
        when(AccessInternalClientFactory.getInstance().getClient())
            .thenReturn(accessInternalClient);

        when(accessInternalClient.selectOperation(anyObject()))
            .thenReturn(new RequestResponseOK().addResult(ClientMockResultHelper.getLogbookResults()));

        when(accessInternalClient.selectOperationById(anyObject(), anyObject()))
            .thenReturn(new RequestResponseOK().addResult(ClientMockResultHelper.getLogbookOperation()));

        when(accessInternalClient.selectUnitLifeCycleById(anyObject(), anyObject()))
            .thenReturn(new RequestResponseOK().addResult(ClientMockResultHelper.getLogbookOperation()));

        when(accessInternalClient.selectObjectGroupLifeCycleById(anyObject(), anyObject()))
            .thenReturn(new RequestResponseOK().addResult(ClientMockResultHelper.getLogbookOperation()));

        // Mock AccessInternal response for check TRACEABILITY operation request
        when(accessInternalClient.checkTraceabilityOperation(JsonHandler.getFromString(request)))
            .thenReturn(ClientMockResultHelper.checkOperationTraceability());

        // Mock AccessInternal response for download TRACEABILITY operation request
        when(accessInternalClient.downloadTraceabilityFile(TRACEABILITY_OPERATION_ID))
            .thenReturn(ClientMockResultHelper.getObjectStream());

    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        LOGGER.debug("Ending tests");
        try {
            if (vitamServer != null) {
                vitamServer.stop();
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
        when(accessInternalClient.selectUnitLifeCycleById(bad_id, JsonHandler.getFromString(BODY_TEST)))
            .thenThrow(new LogbookClientException(""));
        when(accessInternalClient.selectUnitLifeCycle(JsonHandler.getFromString(BODY_TEST)))
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
        when(accessInternalClient.selectObjectGroupLifeCycleById(bad_id, JsonHandler.getFromString(
            request)))
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
        when(accessInternalClient.selectOperation(JsonHandler.getFromString(bad_request)))
            .thenThrow(new LogbookClientException(""));
        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(JsonHandler.getFromString(bad_request))
            .when()
            .get(OPERATIONS_URI)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());

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
        when(
            accessInternalClient.selectOperationById(bad_id, JsonHandler.getFromString(request)))
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
            .body(JsonHandler.getFromString(good_request))
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get(OPERATIONS_URI)
            .then().statusCode(Status.OK.getStatusCode());

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
            .body(JsonHandler.getFromString(request)).when()
            .get(OPERATIONS_URI).then().statusCode(Status.OK.getStatusCode());

        //Test DSL query Validation code
        final Response response = given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(JsonHandler.getFromString(wrong_request_format)).when()
            .get(OPERATIONS_URI);


        final JsonNode responseNode = JsonHandler.getFromString(response.getBody().prettyPrint());
        assertEquals(601, responseNode.get("code").asInt());
        assertEquals(400, responseNode.get("httpCode").asInt());
        assertEquals("Dsl query is not valid.", responseNode.get("message").asText());

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
            .body(JsonHandler.getFromString(good_request))
            .when()
            .get(OPERATIONS_URI)
            .then().statusCode(Status.OK.getStatusCode());

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
    public void should_return_validation_error_when_selectOperationById_with_bad_request() throws Exception {
        given()
            .contentType(ContentType.JSON)
            .body(JsonHandler.getFromString(BODY_TEST_WITH_BAD_REQUEST_FOR_BYID_DSL_REQUEST))
            .pathParam("id_op", good_id)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get(OPERATIONS_URI + OPERATION_ID_URI)
            .then().statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void testSelectOperationsById() throws Exception {
        final JsonNode bodyQuery = JsonHandler.getFromString(BODY_TEST_WITH_ID);
        SelectParserSingle selectParserSingle = new SelectParserSingle();
        selectParserSingle.parse(bodyQuery);
        Select select = selectParserSingle.getRequest();
        select.getFinalSelectById();

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelectById())
            .pathParam("id_op", good_id)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get(OPERATIONS_URI + OPERATION_ID_URI)
            .then().statusCode(Status.OK.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelectById())
            .pathParam("id_op", good_id)
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when()
            .get(OPERATIONS_URI + OPERATION_ID_URI)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelectById())
            .pathParam("id_op", good_id)
            .when()
            .get(OPERATIONS_URI + OPERATION_ID_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(select.getFinalSelectById())
            .pathParam("id_op", good_id)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get(OPERATIONS_URI + OPERATION_ID_URI)
            .then().statusCode(Status.OK.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(select.getFinalSelectById())
            .pathParam("id_op", good_id)
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when()
            .get(OPERATIONS_URI + OPERATION_ID_URI)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(select.getFinalSelectById())
            .pathParam("id_op", good_id)
            .when()
            .get(OPERATIONS_URI + OPERATION_ID_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void testPostSelectOperationsById() throws Exception {
        final JsonNode bodyQuery = JsonHandler.getFromString(BODY_TEST_WITH_ID);
        SelectParserSingle selectParserSingle = new SelectParserSingle();
        selectParserSingle.parse(bodyQuery);
        Select select = selectParserSingle.getRequest();
        select.getFinalSelectById();



        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelectById())
            .pathParam("id_op", good_id)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get(OPERATIONS_URI + OPERATION_ID_URI)
            .then().statusCode(Status.OK.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelectById())
            .pathParam("id_op", good_id)
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when()
            .get(OPERATIONS_URI + OPERATION_ID_URI)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelectById())
            .pathParam("id_op", good_id)
            .when()
            .get(OPERATIONS_URI + OPERATION_ID_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, "GET")
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(select.getFinalSelectById())
            .pathParam("id_op", good_id)
            .when()
            .post(OPERATIONS_URI + OPERATION_ID_URI)
            .then().statusCode(Status.OK.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, "GET")
            .and().header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .body(select.getFinalSelectById())
            .pathParam("id_op", good_id)
            .when()
            .post(OPERATIONS_URI + OPERATION_ID_URI)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, "GET")
            .body(select.getFinalSelectById())
            .pathParam("id_op", good_id)
            .when()
            .post(OPERATIONS_URI + OPERATION_ID_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void testLifeCycleSelect() throws Exception {
        final JsonNode bodyQuery = JsonHandler.getFromString(BODY_TEST_WITH_ID);
        SelectParserSingle selectParserSingle = new SelectParserSingle();
        selectParserSingle.parse(bodyQuery);
        Select select = selectParserSingle.getRequest();
        select.getFinalSelectById();


        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(select.getFinalSelectById())
            .param("id_lc", good_id)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get("/unitlifecycles/" + good_id)
            .then().statusCode(Status.OK.getStatusCode());


        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(select.getFinalSelectById())
            .param("id_lc", good_id)
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when()
            .get("/unitlifecycles/" + good_id)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(select.getFinalSelectById())
            .param("id_lc", good_id)
            .when()
            .get("/unitlifecycles/" + good_id)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(select.getFinalSelectById())
            .param("id_lc", good_id)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get("/objectgrouplifecycles/" + good_id)
            .then().statusCode(Status.OK.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(select.getFinalSelectById())
            .param("id_lc", good_id)
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when()
            .get("/objectgrouplifecycles/" + good_id)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(select.getFinalSelectById())
            .param("id_lc", good_id)
            .when()
            .get("/objectgrouplifecycles/" + good_id)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

}
