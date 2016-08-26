/*******************************************************************************
 * This file is part of Vitam Project.
 * <p>
 * Copyright Vitam (2012, 2015)
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL license as circulated
 * by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL license and that you
 * accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.access.rest;

import static com.jayway.restassured.RestAssured.*;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import fr.gouv.vitam.common.PropertiesUtils;
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

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;

import fr.gouv.vitam.access.api.AccessModule;
import fr.gouv.vitam.access.common.exception.AccessExecutionException;
import fr.gouv.vitam.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.database.parser.request.GlobalDatasParser;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.BasicVitamServer;
import fr.gouv.vitam.common.server.VitamServer;
import fr.gouv.vitam.common.server.VitamServerFactory;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;

// TODO: there is big changes to do in this junit class! Almost all SelectByUnitId tests are wrong (should be a
// fix me)
public class AccessResourceImplTest {

    // URI
    private static final String ACCESS_CONF = "access-test.conf";
    private static final String ACCESS_RESOURCE_URI = "access/v1";
    private static final String ACCESS_STATUS_URI = "/status";
    private static final String ACCESS_UNITS_URI = "/units";
    private static final String ACCESS_UNITS_ID_URI = "/units/xyz";
    private static final String ACCESS_UPDATE_UNITS_ID_URI = "/units/xyz";

    private static VitamServer vitamServer;

    // QUERIES AND DSL
    // TODO
    // Create a "GET" query inspired by DSL, exemple from tech design story 76
    private static final String QUERY_TEST = "{ $query : [ { $eq : { 'title' : 'test' } } ], " +
        " $filter : { $orderby : { '#id' } }," +
        " $projection : {$fields : {#id : 1, title:2, transacdate:1}}" +
        " }";

    private static final String QUERY_SIMPLE_TEST = "{ $query : [ { $eq : { 'title' : 'test' } } ] }";

    private static final String DATA =
        "{ \"_id\": \"aeaqaaaaaeaaaaakaarp4akuuf2ldmyaaaaq\", " + "\"data\": \"data1\" }";

    private static final String DATA2 =
        "{ \"_id\": \"aeaqaaaaaeaaaaakaarp4akuuf2ldmyaaaab\"," + "\"data\": \"data2\" }";

    private static final String ID = "identifier4";
    // LOGGER
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AccessResourceImplTest.class);

    private static final String BODY_TEST = "{$query: {$eq: {\"data\" : \"data2\" }}, $projection: {}, $filter: {}}";

    private static final String ID_UNIT = "identifier5";
    private static JunitHelper junitHelper;
    private static int port;
    private static final String OBJECT_ID = "objectId";
    private static final String OBJECTS_URI = "/objects/";

    private static AccessModule mock;


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        junitHelper = new JunitHelper();
        port = junitHelper.findAvailablePort();
        try {
            vitamServer = buildTestServer();
            ((BasicVitamServer) vitamServer).start();

            RestAssured.port = port;
            RestAssured.basePath = ACCESS_RESOURCE_URI;

            LOGGER.debug("Beginning tests");
        } catch (VitamApplicationServerException e) {
            LOGGER.error(e);
            throw new IllegalStateException(
                "Cannot start the Access Application Server", e);
        }
    }

    private static VitamServer buildTestServer() throws VitamApplicationServerException {
        VitamServer vitamServer = VitamServerFactory.newVitamServer(port);


        final ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(JacksonFeature.class);
        mock = mock(AccessModule.class);
        resourceConfig.register(new AccessResourceImpl(mock));

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

    // Status
    /**
     * Tests the state of the access service API by get
     *
     * @throws Exception
     */
    @Test
    public void givenStartedServer_WhenGetStatus_ThenReturnStatusOk() throws Exception {
        get(ACCESS_STATUS_URI).then().statusCode(Status.OK.getStatusCode());
    }

    // Error cases
    /**
     * Test if the request is inconsistent
     *
     * @throws Exception
     */
    @Ignore("To implement")
    public void givenStartedServer_WhenRequestNotCorrect_ThenReturnError() throws Exception {

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
     * Checks if the send parameter is a bad request
     *
     * @throws Exception
     */
    @Test
    public void givenStartedServer_WhenBadRequest_ThenReturnError_BadRequest() throws Exception {
        given()
            .contentType(ContentType.JSON).header(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, "GET")
            .body(buildDSLWithOptions(QUERY_TEST, DATA2)).
        when()
            .post(ACCESS_UNITS_URI).
        then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void givenStartedServer_When_Empty_Http_Get_ThenReturnError_METHOD_NOT_ALLOWED() throws Exception {
        given()
            .contentType(ContentType.JSON).header(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, "ABC")
            .body(buildDSLWithOptions(QUERY_TEST, DATA2))
            .when().post(ACCESS_UNITS_URI).then().statusCode(Status.METHOD_NOT_ALLOWED.getStatusCode());
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

    /**
     * Checks if the send parameter is a bad request
     * 
     * @throws Exception
     */
    @Test
    public void givenStartedServer_WhenBadRequest_ThenReturnError_SelectById_BadRequest() throws Exception {
        given()
            .contentType(ContentType.JSON).header(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, "GET")
            .body(buildDSLWithRoots(ID))
            .when().post(ACCESS_UNITS_ID_URI).then().statusCode(Status.BAD_REQUEST.getStatusCode());
    }


    @Test
    public void givenStartedServer_When_Empty_Http_Get_ThenReturnError_SelectById_METHOD_NOT_ALLOWED()
        throws Exception {
        given()
            .contentType(ContentType.JSON).header(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, "ABC")
            .body(buildDSLWithRoots(ID))
            .when().post(ACCESS_UNITS_ID_URI).then().statusCode(Status.METHOD_NOT_ALLOWED.getStatusCode());
    }

    @Test
    public void given_SelectUnitById_WhenStringTooLong_Then_Throw_MethodNotAllowed() throws Exception {
        GlobalDatasParser.limitRequest = 1000;
        given()
            .contentType(ContentType.JSON).header(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, "ABC")
            .body(buildDSLWithOptions(createLongString(1001), DATA2))
            .when().post(ACCESS_UNITS_ID_URI).then().statusCode(Status.METHOD_NOT_ALLOWED.getStatusCode());

    }

    @Test
    public void given_updateUnitById_WhenStringTooLong_Then_Throw_BadRequest() throws Exception {
        GlobalDatasParser.limitRequest = 1000;
        given()
                .contentType(ContentType.JSON).body(buildDSLWithOptions(createLongString(1001), DATA2))
                .when().put(ACCESS_UPDATE_UNITS_ID_URI).then().statusCode(Status.BAD_REQUEST.getStatusCode());
    }



    private static String createLongString(int size) throws Exception {
        final StringBuilder sb = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            sb.append('a');
        }
        return sb.toString();
    }

    @Ignore
    @Test
    public void given_units_insert_when_searchUnitsByID_thenReturn_Found() throws Exception {
        with()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, "GET")
            .body(buildDSLWithOptions("", DATA2)).when()
            .post("/units").then()
            .statusCode(Status.CREATED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, "GET")
            .body(BODY_TEST).when()
            .post("/units/" + ID_UNIT).then()
            .statusCode(Status.FOUND.getStatusCode());

    }


    @Test
    public void given_emptyQuery_when_SelectByID_thenReturn_Bad_Request() {

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, "GET")
            .body("")
            .when()
            .post("/units/" + ID_UNIT)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void given_emptyQuery_when_UpdateByID_thenReturn_Bad_Request() {

        given()
                .contentType(ContentType.JSON)
                .body("")
                .when()
                .put("/units/" + ID_UNIT)
                .then()
                .statusCode(Status.BAD_REQUEST.getStatusCode());
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
                .statusCode(Status.METHOD_NOT_ALLOWED.getStatusCode());
    }

    @Test
    public void given_pathWithId_when_get_SelectByID() {
        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, "GET")
            .body(BODY_TEST)
            .when()
            .post("/units/" + ID_UNIT)
            .then()
            .statusCode(Status.OK.getStatusCode());
    }

    @Ignore
    @Test
    public void given_bad_header_when_updateByID_thenReturn_Not_allowed() {

        given()
                .contentType(ContentType.JSON)
                .body(BODY_TEST)
                .when()
                .put("/units/" + ID_UNIT)
                .then()
                .statusCode(Status.METHOD_NOT_ALLOWED.getStatusCode());
    }

    @Ignore
    @Test
    public void shouldReturnInternalServerError() throws Exception {
        int limitRequest = GlobalDatasParser.limitRequest;
        GlobalDatasParser.limitRequest = 99;
        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, "GET")
            .body(buildDSLWithOptions("", createJsonStringWithDepth(101))).when()
            .post("/units/" + ID_UNIT).then()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());
        GlobalDatasParser.limitRequest = limitRequest;
    }

    private static String createJsonStringWithDepth(int depth) {
        final StringBuilder obj = new StringBuilder();
        if (depth == 0) {
            return " \"b\" ";
        }
        obj.append("{ \"a\": ").append(createJsonStringWithDepth(depth - 1)).append("}");
        return obj.toString();
    }

    @Test
    public void getObjectGroupOk() throws Exception {
        reset(mock);
        when(mock.selectObjectGroupById(JsonHandler.getFromString(BODY_TEST), OBJECT_ID)).thenReturn(JsonHandler
            .getFromString(DATA));

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).body(BODY_TEST).when().get
            (OBJECTS_URI +
            OBJECT_ID)
            .then()
            .statusCode(Status.OK.getStatusCode()).contentType(MediaType.APPLICATION_JSON);
    }

    @Test
    public void getObjectGroupPostOK() throws Exception {
        reset(mock);
        when(mock.selectObjectGroupById(anyObject(), anyObject())).thenReturn(JsonHandler
            .getFromString(DATA));

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).body(BODY_TEST).header
            (GlobalDataRest.X_HTTP_METHOD_OVERRIDE,
            "GET").when().post(OBJECTS_URI + OBJECT_ID).then()
            .statusCode(Status.OK.getStatusCode()).contentType(MediaType.APPLICATION_JSON);
    }

    @Test
    public void getObjectGroupPreconditionFailed() {
        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).when().get(OBJECTS_URI +
            OBJECT_ID).then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void getObjectGroupPostMethodNotAllowed() {
        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).body(buildDSLWithRoots("")
        ).when().post(OBJECTS_URI + OBJECT_ID)
            .then().statusCode(Status.METHOD_NOT_ALLOWED.getStatusCode());
    }

    @Test
    public void getObjectGroupBadRequest() {
        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).body("test").when().get
            (OBJECTS_URI + OBJECT_ID).then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void getObjectGroupNotFound() throws Exception {
        reset(mock);
        when(mock.selectObjectGroupById(JsonHandler.getFromString(BODY_TEST), OBJECT_ID)).thenThrow(new
            NotFoundException());

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).body(BODY_TEST).when().get
            (OBJECTS_URI + OBJECT_ID).then()
            .statusCode(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void getObjectGroupInternalServerError() throws Exception {
        reset(mock);
        when(mock.selectObjectGroupById(JsonHandler.getFromString(BODY_TEST), OBJECT_ID)).thenThrow(new
            AccessExecutionException("Wanted exception"));

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).body(BODY_TEST).when().get
            (OBJECTS_URI + OBJECT_ID).then()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    // Stream
    @Test
    public void getObjectStreamOk() throws Exception {
        reset(mock);
        when(mock.getOneObjectFromObjectGroup(anyString(), anyObject(), anyString(), anyInt(), anyString()))
            .thenReturn(new ByteArrayInputStream("test".getBytes()));

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM).headers
            (getStreamHeaders()).body(BODY_TEST).when().get(OBJECTS_URI + OBJECT_ID).then()
            .statusCode(Status.OK.getStatusCode()).contentType(MediaType.APPLICATION_OCTET_STREAM);
    }

    @Test
    public void getObjectStreamPostOK() throws Exception {
        reset(mock);
        when(mock.getOneObjectFromObjectGroup(anyString(), anyObject(), anyString(), anyInt(), anyString()))
            .thenReturn(new ByteArrayInputStream("test".getBytes()));

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM).body(BODY_TEST)
            .headers(getStreamHeaders()).header(GlobalDataRest.X_HTTP_METHOD_OVERRIDE,
            "GET").when().post(OBJECTS_URI + OBJECT_ID).then()
            .statusCode(Status.OK.getStatusCode()).contentType(MediaType.APPLICATION_OCTET_STREAM);
    }

    @Test
    public void getObjectStreamPreconditionFailed() {
        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM).header
            (GlobalDataRest.X_QUALIFIER, "qualif").header(GlobalDataRest.X_VERSION, 1).when()
            .get(OBJECTS_URI + OBJECT_ID).then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM).header
            (GlobalDataRest.X_QUALIFIER, "qualif").header(GlobalDataRest.X_TENANT_ID, "0").when()
            .get(OBJECTS_URI + OBJECT_ID).then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM).header
            (GlobalDataRest.X_TENANT_ID, "0").header(GlobalDataRest.X_VERSION, 1).when()
            .get(OBJECTS_URI + OBJECT_ID).then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM)
            .when().post(OBJECTS_URI + OBJECT_ID).then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM).headers(getStreamHeaders())
            .when().get(OBJECTS_URI + OBJECT_ID).then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void getObjectStreamPostMethodNotAllowed() {
        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM).header
            (GlobalDataRest.X_HTTP_METHOD_OVERRIDE, "TEST").body("test").when().post(OBJECTS_URI + OBJECT_ID)
            .then().statusCode(Status.METHOD_NOT_ALLOWED.getStatusCode());
    }

    @Test
    public void getObjectStreamBadRequest() {
        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM)
            .headers(getStreamHeaders()).body("test").when().get(OBJECTS_URI + OBJECT_ID).then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void getObjectStreamNotFound() throws Exception {
        reset(mock);
        when(mock.getOneObjectFromObjectGroup(anyString(), anyObject(), anyString(), anyInt(), anyString()))
            .thenThrow(new StorageNotFoundException("test"));

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM)
            .headers(getStreamHeaders()).body(BODY_TEST).when().get(OBJECTS_URI + OBJECT_ID).then()
            .statusCode(Status.NOT_FOUND.getStatusCode());

        reset(mock);
        when(mock.getOneObjectFromObjectGroup(anyString(), anyObject(), anyString(), anyInt(), anyString()))
            .thenThrow(new MetaDataNotFoundException("test"));

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM)
            .headers(getStreamHeaders()).body(BODY_TEST).when().get(OBJECTS_URI + OBJECT_ID).then()
            .statusCode(Status.NOT_FOUND.getStatusCode());

    }

    @Test
    public void getObjectStreamInternalServerError() throws Exception {
        reset(mock);
        when(mock.getOneObjectFromObjectGroup(anyString(), anyObject(), anyString(), anyInt(), anyString())).thenThrow(new
            AccessExecutionException("Wanted exception"));

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM).headers(getStreamHeaders())
            .body(BODY_TEST).when().get(OBJECTS_URI + OBJECT_ID).then().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    private Map<String, Object> getStreamHeaders() {
        Map<String, Object> headers = new HashMap<>();
        headers.put(GlobalDataRest.X_TENANT_ID, "0");
        headers.put(GlobalDataRest.X_QUALIFIER, "qualif");
        headers.put(GlobalDataRest.X_VERSION, 1);
        return headers;
    }

}

