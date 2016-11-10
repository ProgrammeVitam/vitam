package fr.gouv.vitam.access.external.rest;

import static com.jayway.restassured.RestAssured.given;

import javax.ws.rs.core.Response.Status;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;

import fr.gouv.vitam.access.internal.client.AccessInternalClient;
import fr.gouv.vitam.access.internal.client.AccessInternalClientFactory;
import fr.gouv.vitam.access.internal.common.model.AccessInternalConfiguration;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.BasicVitamServer;
import fr.gouv.vitam.common.server.VitamServer;
import fr.gouv.vitam.common.server.VitamServerFactory;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;


@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.net.ssl.*", "javax.management.*"})
@PrepareForTest({AccessInternalClientFactory.class})
public class LogbookExternalResourceImplTest {
    @Mock
    private AccessInternalConfiguration configuration;
    @InjectMocks
    private LogbookExternalResourceImpl accessExternalResourceImpl;

    // URI
    private static final String ACCESS_RESOURCE_LOGBOOK_URI = "access-external/v1";

    private static VitamServer vitamServer;

    // LOGGER
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookExternalResourceImplTest.class);

    private static JunitHelper junitHelper = JunitHelper.getInstance();
    private static int port = junitHelper.findAvailablePort();
    private static AccessInternalClient accessInternalClient;

    private static final String OPERATIONS_URI = "/operations";
    private static final String OPERATION_ID_URI = "/{id_op}";

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
        "    \"agIdAppSession\": null," +
        "    \"evIdReq\": \"aedqaaaaacaam7mxaaaamakvhiv4rsiaaaaq\"," +
        "    \"agIdSubm\": null," +
        "    \"agIdOrig\": null," +
        "    \"obId\": null," +
        "    \"obIdReq\": null," +
        "    \"obIdIn\": null," +
        "    \"events\": []}";

    private static final String BODY_TEST = "{$query: {$eq: {\"aa\" : \"vv\" }}, $projection: {}, $filter: {}}";
    static String request = "{ $query: {} , $projection: {}, $filter: {} }";
    static String bad_request = "{ $query: \"bad_request\" , $projection: {}, $filter: {} }";
    static String good_id = "goodId";
    static String bad_id = "badId";

    public static String X_HTTP_METHOD_OVERRIDE = "X-HTTP-Method-Override";


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        try {
            vitamServer = buildTestServer();
            ((BasicVitamServer) vitamServer).start();

            RestAssured.port = port;
            RestAssured.basePath = ACCESS_RESOURCE_LOGBOOK_URI;

            LOGGER.debug("Beginning tests");
        } catch (VitamApplicationServerException e) {
            LOGGER.error(e);
            throw new IllegalStateException(
                "Cannot start the Access External Application Server", e);
        }

    }

    private static VitamServer buildTestServer()
        throws Exception {
        VitamServer vitamServer = VitamServerFactory.newVitamServer(port);


        final ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(JacksonFeature.class);
        PowerMockito.mockStatic(AccessInternalClientFactory.class);
        accessInternalClient = PowerMockito.mock(AccessInternalClient.class);
        AccessInternalClientFactory accessInternalFactory = PowerMockito.mock(AccessInternalClientFactory.class);
        PowerMockito.when(AccessInternalClientFactory.getInstance()).thenReturn(accessInternalFactory);
        PowerMockito.when(AccessInternalClientFactory.getInstance().getClient()).thenReturn(accessInternalClient);

        PowerMockito.when(accessInternalClient.selectOperation(JsonHandler.getFromString(request)))
        .thenReturn(JsonHandler.getFromString(MOCK_SELECT_RESULT));
        PowerMockito.when(accessInternalClient.selectOperationbyId(good_id))
        .thenReturn(JsonHandler.getFromString(MOCK_SELECT_RESULT));

        PowerMockito.doThrow(new LogbookClientException(""))
        .when(accessInternalClient).selectOperation(JsonHandler.getFromString(bad_request));
        PowerMockito.doThrow(new LogbookClientException(""))
        .when(accessInternalClient).selectOperationbyId(bad_id);

        PowerMockito.when(accessInternalClient.selectUnitLifeCycleById(good_id))
        .thenReturn(JsonHandler.getFromString(MOCK_SELECT_RESULT));
        PowerMockito.when(accessInternalClient.selectObjectGroupLifeCycleById(good_id))
        .thenReturn(JsonHandler.getFromString(MOCK_SELECT_RESULT));
        PowerMockito.doThrow(new LogbookClientException(""))
        .when(accessInternalClient).selectUnitLifeCycleById(bad_id);
        PowerMockito.doThrow(new LogbookClientException(""))
        .when(accessInternalClient).selectObjectGroupLifeCycleById(bad_id);

        resourceConfig.register(new AccessExternalResourceImpl());
        resourceConfig.register(new LogbookExternalResourceImpl());

        final ServletContainer servletContainer = new ServletContainer(resourceConfig);
        final ServletHolder sh = new ServletHolder(servletContainer);
        final ServletContextHandler contextHandler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        contextHandler.setContextPath("/");
        contextHandler.addServlet(sh, "/*");

        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] {contextHandler});
        vitamServer.configure(contextHandler);
        return vitamServer;
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        LOGGER.debug("Ending tests");
        try {
            ((BasicVitamServer) vitamServer).stop();
            junitHelper.releasePort(port);
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
        }
    }
    // FIXME P0 this test is erratic
    @Ignore
    @Test
    public void testErrorSelect() throws Exception {
        given()
        .contentType(ContentType.JSON)
        .body(JsonHandler.getFromString(request))
        .header(X_HTTP_METHOD_OVERRIDE, "ABC")
        .pathParam("id_op", 1)
        .when()
        .post(OPERATIONS_URI + OPERATION_ID_URI)
        .then()
        .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
        .contentType(ContentType.JSON)
        .header(X_HTTP_METHOD_OVERRIDE, "ABC")
        .body(JsonHandler.getFromString(BODY_TEST))
        .when()
        .post(OPERATIONS_URI)
        .then()
        .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
        .contentType(ContentType.JSON)
        .body(JsonHandler.getFromString(bad_request))
        .when()
        .get(OPERATIONS_URI)
        .then()
        .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());

        given()
        .contentType(ContentType.JSON)
        .pathParam("id_op", bad_id)
        .when()
        .get(OPERATIONS_URI + OPERATION_ID_URI)
        .then()
        .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());

        given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .param("id_lc", bad_id)
        .when()
        .get("/unitlifecycles/" + bad_id)
        .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());       

        given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .param("id_lc", bad_id)
        .when()
        .get("/objectgrouplifecycles/" + bad_id)
        .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());             
    }

    @Test
    public void testSelectOperations() throws Exception {
        given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(JsonHandler.getFromString(request))
        .when()
        .get(OPERATIONS_URI)
        .then().statusCode(Status.OK.getStatusCode());

        given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .header(X_HTTP_METHOD_OVERRIDE, "GET")
        .body(JsonHandler.getFromString(request))
        .when()
        .post(OPERATIONS_URI)
        .then().statusCode(Status.OK.getStatusCode());
    }

    @Test
    public void testSelectOperationsById() throws Exception {
        given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .pathParam("id_op", good_id)
        .when()
        .get(OPERATIONS_URI + OPERATION_ID_URI)
        .then().statusCode(Status.OK.getStatusCode());

        given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .header(X_HTTP_METHOD_OVERRIDE, "GET")
        .body(bad_request)
        .pathParam("id_op", good_id)
        .when()
        .post(OPERATIONS_URI + OPERATION_ID_URI)
        .then().statusCode(Status.OK.getStatusCode());
    }

    @Test
    public void testLifeCycleSelect() throws Exception {
        given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .param("id_lc", good_id)
        .when()
        .get("/unitlifecycles/" + good_id)
        .then().statusCode(Status.OK.getStatusCode());

        given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .param("id_lc", good_id)
        .when()
        .get("/objectgrouplifecycles/" + good_id)
        .then().statusCode(Status.OK.getStatusCode());       
    }


}
