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

import fr.gouv.vitam.access.external.api.AccessExternalConfiguration;
import fr.gouv.vitam.access.internal.common.model.AccessInternalConfiguration;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.BasicVitamServer;
import fr.gouv.vitam.common.server.VitamServer;
import fr.gouv.vitam.common.server.VitamServerFactory;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;


@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.net.ssl.*")
@PrepareForTest({LogbookOperationsClientFactory.class, LogbookLifeCyclesClientFactory.class})
public class LogbookExternalResourceImplTest {
    @Mock
    private AccessInternalConfiguration configuration;
    @InjectMocks
    private AccessExternalResourceImpl accessExternalResourceImpl;

    // URI
    private static final String ACCESS_CONF = "access-external-test.conf";
    private static final String ACCESS_RESOURCE_LOGBOOK_URI = "access-external/v1";

    private static VitamServer vitamServer;

    // LOGGER
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AccessExternalResourceImplTest.class);

    private static JunitHelper junitHelper = JunitHelper.getInstance();
    private static int port = junitHelper.findAvailablePort();
    private static LogbookOperationsClient logbookClient;
    private static LogbookLifeCyclesClient logbookLifeCycleClient;

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
    private static final String BODY_QUERY =
        "{$query: {$eq: {\"evType\" : \"Process_SIP_unitary\"}}, $projection: {}, $filter: {}}";
    static String request = "{ $query: {} }, $projection: {}, $filter: {} }";
    static String bad_request = "{ $query:\\ {} }, $projection: {}, $filter: {} }";
    static String good_id = "goodId";
    static String bad_id = "badId";

    public static String X_HTTP_METHOD_OVERRIDE = "X-HTTP-Method-Override";


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        PowerMockito.mockStatic(LogbookOperationsClientFactory.class, LogbookLifeCyclesClientFactory.class);
        logbookClient = PowerMockito.mock(LogbookOperationsClient.class);
        LogbookOperationsClientFactory logbookFactory = PowerMockito.mock(LogbookOperationsClientFactory.class);
        PowerMockito.when(LogbookOperationsClientFactory.getInstance()).thenReturn(logbookFactory);
        PowerMockito.when(LogbookOperationsClientFactory.getInstance().getClient()).thenReturn(logbookClient);

        logbookLifeCycleClient = PowerMockito.mock(LogbookLifeCyclesClient.class);
        LogbookLifeCyclesClientFactory logbookLifeCycleFactory =
            PowerMockito.mock(LogbookLifeCyclesClientFactory.class);
        PowerMockito.when(LogbookLifeCyclesClientFactory.getInstance()).thenReturn(logbookLifeCycleFactory);
        PowerMockito.when(LogbookLifeCyclesClientFactory.getInstance().getClient())
            .thenReturn(logbookLifeCycleClient);
        
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
        throws VitamApplicationServerException, LogbookClientException, InvalidParseOperationException {
        VitamServer vitamServer = VitamServerFactory.newVitamServer(port);


        final ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(JacksonFeature.class);

        PowerMockito.when(logbookClient.selectOperation(request))
            .thenReturn(JsonHandler.getFromString(MOCK_SELECT_RESULT));
        PowerMockito.when(logbookClient.selectOperationbyId(good_id))
            .thenReturn(JsonHandler.getFromString(MOCK_SELECT_RESULT));

        PowerMockito.when(logbookClient.selectOperation(bad_request))
            .thenThrow(LogbookClientException.class);
        PowerMockito.when(logbookClient.selectOperationbyId(bad_id))
            .thenThrow(LogbookClientException.class);

        PowerMockito.when(logbookLifeCycleClient.selectUnitLifeCycleById(good_id))
            .thenReturn(JsonHandler.getFromString(MOCK_SELECT_RESULT));
        PowerMockito.when(logbookLifeCycleClient.selectObjectGroupLifeCycleById(good_id))
            .thenReturn(JsonHandler.getFromString(MOCK_SELECT_RESULT));
        PowerMockito.when(logbookLifeCycleClient.selectUnitLifeCycleById(bad_id))
        .thenThrow(LogbookClientException.class);
        PowerMockito.when(logbookLifeCycleClient.selectObjectGroupLifeCycleById(bad_id))
        .thenThrow(LogbookClientException.class);

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

    @Test
    public void testErrorSelect() {

        given()
            .contentType(ContentType.JSON)
            .body(request)
            .header(X_HTTP_METHOD_OVERRIDE, "ABC")
            .pathParam("id_op", 1)
            .when()
            .post(OPERATIONS_URI + OPERATION_ID_URI)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "ABC")
            .body(BODY_TEST)
            .when()
            .post(OPERATIONS_URI)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(bad_request)
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
            .body(request)
            .when()
            .get(OPERATIONS_URI)
            .then().statusCode(Status.OK.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .body(request)
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
