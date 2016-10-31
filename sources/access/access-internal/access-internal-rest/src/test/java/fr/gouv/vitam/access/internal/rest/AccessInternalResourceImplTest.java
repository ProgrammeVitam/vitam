/*******************************************************************************
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
 *******************************************************************************/
package fr.gouv.vitam.access.internal.rest;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.with;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.MarshalException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;

import fr.gouv.vitam.access.internal.api.AccessBinaryData;
import fr.gouv.vitam.access.internal.api.AccessInternalModule;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalExecutionException;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.database.parser.request.GlobalDatasParser;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.application.junit.ResponseHelper;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;

// FIXME P1 : there is big changes to do in this junit class! Almost all SelectByUnitId tests are wrong (should be a
// fix me)
public class AccessInternalResourceImplTest {
    // LOGGER
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AccessInternalResourceImplTest.class);

    // URI
    private static final String ACCESS_CONF = "access-test.conf";
    private static final String ACCESS_RESOURCE_URI = "access-internal/v1";
    private static final String ACCESS_UNITS_URI = "/units";
    private static final String ACCESS_UNITS_ID_URI = "/units/xyz";
    private static final String ACCESS_UPDATE_UNITS_ID_URI = "/units/xyz";

    private static AccessInternalApplication application;

    // QUERIES AND DSL
    // TODO P1
    // Create a "GET" query inspired by DSL, exemple from tech design story 76
    private static final String QUERY_TEST =
        "{ \"$query\" : [ { \"$eq\": { \"title\" : \"test\" } } ], " +
            " \"$filter\": { \"$orderby\": \"#id\" }, " +
            " \"$projection\" : { \"$fields\" : { \"#id\": 1, \"title\" : 2, \"transacdate\": 1 } } " +
            " }";

    private static final String QUERY_SIMPLE_TEST = "{ \"$query\" : [ { \"$eq\" : { \"title\" : \"test\" } } ] }";

    private static final String DATA =
        "{ \"_id\": \"aeaqaaaaaeaaaaakaarp4akuuf2ldmyaaaaq\", " + "\"data\": \"data1\" }";

    private static final String DATA2 =
        "{ \"_id\": \"aeaqaaaaaeaaaaakaarp4akuuf2ldmyaaaab\"," + "\"data\": \"data2\" }";

    private static final String DATA_HTML =
        "{ \"_id\": \"<a href='www.culture.gouv.fr'>Culture</a>\"," + "\"data\": \"data2\" }";

    private static final String ID = "identifier4";

    private static final String BODY_TEST =
        "{\"$query\": {\"$eq\": {\"data\" : \"data2\" }}, \"$projection\": {}, \"$filter\": {}}";

    private static final String ID_UNIT = "identifier5";
    private static JunitHelper junitHelper;
    private static int port;
    private static final String OBJECT_ID = "objectId";
    private static final String OBJECTS_URI = "/objects/";

    private static AccessInternalModule mock;

    private static int step = 0;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        junitHelper = JunitHelper.getInstance();
        port = junitHelper.findAvailablePort();
        mock = mock(AccessInternalModule.class);
        AccessInternalApplication.mock = mock;
        try {
            application = new AccessInternalApplication(ACCESS_CONF);
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

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        LOGGER.debug("Ending tests");
        try {
            application.stop();
            junitHelper.releasePort(port);
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
        }
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
            .body(buildDSLWithOptions(QUERY_TEST, DATA2).asText())
            .when().post(ACCESS_UNITS_URI).then().statusCode(Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode());
    }

    /**
     * Checks if the send parameter is a bad request
     *
     * @throws Exception
     */
    @Test(expected = InvalidParseOperationException.class)
    public void givenStartedServer_WhenBadRequest_ThenReturnError_BadRequest() throws Exception {
        given()
            .contentType(ContentType.JSON).header(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, "GET")
            .body(buildDSLWithOptions(QUERY_TEST, "test")).when()
            .post(ACCESS_UNITS_URI).then()
            .statusCode(Status.OK.getStatusCode());
    }

    @Test
    public void givenStartedServer_WhenJsonContainsHtml_ThenReturnError_BadRequest() throws Exception {
        given()
            .contentType(ContentType.JSON).header(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, "GET")
            .body(buildDSLWithRoots(DATA_HTML)).when()
            .post(ACCESS_UNITS_URI).then()
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
     * @throws InvalidParseOperationException
     */
    private static final JsonNode buildDSLWithOptions(String query, String data) throws InvalidParseOperationException {
        return JsonHandler
            .getFromString("{ \"$roots\" : [], \"$query\" : [ " + query + " ], \"$data\" : " + data + " }");
    }

    /**
     *
     * @param data
     * @return query DSL with id as Roots
     * @throws InvalidParseOperationException 
     */
    private static final JsonNode buildDSLWithRoots(String data) throws InvalidParseOperationException {
        return JsonHandler.getFromString("{ \"$roots\" : [ " + data + " ], \"$query\" : [ \"\" ], \"$data\" : " + data + " }");
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
            .body(buildDSLWithRoots("\"" + ID + "\"").asText())
            .when().post(ACCESS_UNITS_ID_URI).then().statusCode(Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode());
    }

    /**
     * Checks if the send parameter is a bad request
     *
     * @throws Exception
     */
    @Test (expected = InvalidParseOperationException.class)
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
            .body(buildDSLWithRoots(null))
            .when().post(ACCESS_UNITS_ID_URI).then().statusCode(Status.METHOD_NOT_ALLOWED.getStatusCode());
    }

    @Test(expected = InvalidParseOperationException.class)
    public void given_SelectUnitById_WhenStringTooLong_Then_RaiseException() throws Exception {
        GlobalDatasParser.limitRequest = 1000;
        given()
            .contentType(ContentType.JSON).header(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, "ABC")
            .body(buildDSLWithOptions(createLongString(1001), DATA2))
            .when().post(ACCESS_UNITS_ID_URI).then().statusCode(Status.METHOD_NOT_ALLOWED.getStatusCode());

    }

    @Test(expected = InvalidParseOperationException.class)
    public void given_updateUnitById_WhenStringTooLong_Then_RaiseException() throws Exception {
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
    public void given_queryThatThrowException_when_updateByID() throws InvalidParseOperationException {
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
    public void given_bad_header_when_updateByID_thenReturn_Not_allowed() throws InvalidParseOperationException {

        given()
            .contentType(ContentType.JSON)
            .body(JsonHandler.getFromString(BODY_TEST))
            .when()
            .put("/units/" + ID_UNIT)
            .then()
            .statusCode(Status.METHOD_NOT_ALLOWED.getStatusCode());
    }

    @Test
    public void shouldReturnInternalServerError() throws Exception {
        final int limitRequest = GlobalDatasParser.limitRequest;
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

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).body(BODY_TEST).when()
            .get(OBJECTS_URI +
                OBJECT_ID)
            .then()
            .statusCode(Status.OK.getStatusCode()).contentType(MediaType.APPLICATION_JSON);
    }

    @Test
    public void getObjectGroupPostOK() throws Exception {
        reset(mock);
        when(mock.selectObjectGroupById(anyObject(), anyObject())).thenReturn(JsonHandler
            .getFromString(DATA));

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).body(BODY_TEST)
            .header(GlobalDataRest.X_HTTP_METHOD_OVERRIDE,
                "GET")
            .when().post(OBJECTS_URI + OBJECT_ID).then()
            .statusCode(Status.OK.getStatusCode()).contentType(MediaType.APPLICATION_JSON);
    }

    @Test
    public void getObjectGroupPreconditionFailed() {
        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).when().get(OBJECTS_URI +
            OBJECT_ID).then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void getObjectGroupPostMethodNotAllowed() throws InvalidParseOperationException {
        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).body(buildDSLWithRoots(null))
            .when().post(OBJECTS_URI + OBJECT_ID)
            .then().statusCode(Status.METHOD_NOT_ALLOWED.getStatusCode());
    }

    @Test
    public void getObjectGroupBadRequest() {
        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).body("test").when()
            .get(OBJECTS_URI + OBJECT_ID).then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void getObjectGroupNotFound() throws Exception {
        reset(mock);
        when(mock.selectObjectGroupById(JsonHandler.getFromString(BODY_TEST), OBJECT_ID))
            .thenThrow(new NotFoundException());

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).body(BODY_TEST).when()
            .get(OBJECTS_URI + OBJECT_ID).then()
            .statusCode(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void getObjectGroupInternalServerError() throws Exception {
        reset(mock);
        when(mock.selectObjectGroupById(JsonHandler.getFromString(BODY_TEST), OBJECT_ID))
            .thenThrow(new AccessInternalExecutionException("Wanted exception"));

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).body(BODY_TEST).when()
            .get(OBJECTS_URI + OBJECT_ID).then()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    // Stream
    @Test
    public void getObjectStreamOk() throws Exception {
        reset(mock);
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Length", "4");
        Response response = ResponseHelper.getOutboundResponse(Status.OK, new ByteArrayInputStream("test".getBytes()),
            MediaType.APPLICATION_OCTET_STREAM, headers);
        AccessBinaryData abd = new AccessBinaryData("test.pdf", "application/pdf", response);
        when(
            mock.getOneObjectFromObjectGroup(anyString(), anyObject(), anyString(), anyInt(), anyString()))
                .thenReturn(abd);

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM)
            .headers(getStreamHeaders()).body(BODY_TEST).when().get(OBJECTS_URI + OBJECT_ID).then()
            .statusCode(Status.OK.getStatusCode()).contentType("application/pdf");

        reset(mock);
        headers.put("Content-Length", "");
        response = ResponseHelper.getOutboundResponse(Status.OK, new ByteArrayInputStream("test".getBytes()),
            MediaType.APPLICATION_OCTET_STREAM, headers);
        abd = new AccessBinaryData("test.pdf", "application/pdf", response);
        when(
            mock.getOneObjectFromObjectGroup(anyString(), anyObject(), anyString(), anyInt(), anyString()))
                .thenReturn(abd);

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM)
            .headers(getStreamHeaders()).body(BODY_TEST).when().get(OBJECTS_URI + OBJECT_ID).then()
            .statusCode(Status.OK.getStatusCode()).contentType("application/pdf");
    }

    @Test
    public void getObjectStreamPostOK() throws Exception {
        reset(mock);
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Length", "4");
        Response response = ResponseHelper.getOutboundResponse(Status.OK, new ByteArrayInputStream("test".getBytes()),
            MediaType.APPLICATION_OCTET_STREAM, headers);
        AccessBinaryData abd = new AccessBinaryData("test.pdf", "application/pdf", response);
        when(
            mock.getOneObjectFromObjectGroup(anyString(), anyObject(), anyString(), anyInt(), anyString()))
                .thenReturn(abd);

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM).body(BODY_TEST)
            .headers(getStreamHeaders()).header(GlobalDataRest.X_HTTP_METHOD_OVERRIDE,
                "GET")
            .when().post(OBJECTS_URI + OBJECT_ID).then()
            .statusCode(Status.OK.getStatusCode()).contentType("application/pdf");
    }

    @Test
    public void getObjectStreamPreconditionFailed() {
        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM)
            .header(GlobalDataRest.X_QUALIFIER, "qualif").header(GlobalDataRest.X_VERSION, 1).when()
            .get(OBJECTS_URI + OBJECT_ID).then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM)
            .header(GlobalDataRest.X_QUALIFIER, "qualif").header(GlobalDataRest.X_TENANT_ID, "0").when()
            .get(OBJECTS_URI + OBJECT_ID).then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM)
            .header(GlobalDataRest.X_TENANT_ID, "0").header(GlobalDataRest.X_VERSION, 1).when()
            .get(OBJECTS_URI + OBJECT_ID).then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM)
            .when().post(OBJECTS_URI + OBJECT_ID).then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM)
            .headers(getStreamHeaders())
            .when().get(OBJECTS_URI + OBJECT_ID).then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void getObjectStreamPostMethodNotAllowed() throws InvalidParseOperationException {
        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM)
            .header(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, "TEST").body(JsonHandler.getFromString(QUERY_TEST)).when()
            .post(OBJECTS_URI + OBJECT_ID)
            .then().statusCode(Status.METHOD_NOT_ALLOWED.getStatusCode());
    }

    @Ignore
    @Test
    // Data needed in body
    public void getObjectStreamBadRequest() throws InvalidParseOperationException {
        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM)
            .headers(getStreamHeaders()).body(JsonHandler.getFromString(DATA)).when().get(OBJECTS_URI + OBJECT_ID)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void getObjectStreamNotFound() throws Exception {
        reset(mock);
        when(
            mock.getOneObjectFromObjectGroup(anyString(), anyObject(), anyString(), anyInt(), anyString()))
                .thenThrow(new StorageNotFoundException("test"));

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM)
            .headers(getStreamHeaders()).body(BODY_TEST).when().get(OBJECTS_URI + OBJECT_ID).then()
            .statusCode(Status.NOT_FOUND.getStatusCode());

        reset(mock);
        when(
            mock.getOneObjectFromObjectGroup(anyString(), anyObject(), anyString(), anyInt(), anyString()))
                .thenThrow(new MetaDataNotFoundException("test"));

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM)
            .headers(getStreamHeaders()).body(BODY_TEST).when().get(OBJECTS_URI + OBJECT_ID).then()
            .statusCode(Status.NOT_FOUND.getStatusCode());

    }

    @Test
    public void getObjectStreamInternalServerError() throws Exception {
        reset(mock);
        when(
            mock.getOneObjectFromObjectGroup(anyString(), anyObject(), anyString(), anyInt(), anyString()))
                .thenThrow(new AccessInternalExecutionException("Wanted exception"));

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM)
            .headers(getStreamHeaders())
            .body(BODY_TEST).when().get(OBJECTS_URI + OBJECT_ID).then()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    private Map<String, Object> getStreamHeaders() {
        final Map<String, Object> headers = new HashMap<>();
        headers.put(GlobalDataRest.X_TENANT_ID, "0");
        headers.put(GlobalDataRest.X_QUALIFIER, "qualif");
        headers.put(GlobalDataRest.X_VERSION, 1);
        return headers;
    }

}

