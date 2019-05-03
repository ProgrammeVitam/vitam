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
package fr.gouv.vitam.access.external.rest;

import static com.jayway.restassured.RestAssured.given;
import static fr.gouv.vitam.common.GlobalDataRest.X_HTTP_METHOD_OVERRIDE;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
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

import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.exception.VitamDBException;
import org.hamcrest.CoreMatchers;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Header;
import com.jayway.restassured.response.Headers;

import fr.gouv.vitam.access.internal.client.AccessInternalClient;
import fr.gouv.vitam.access.internal.client.AccessInternalClientFactory;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalClientNotFoundException;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalClientServerException;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.exception.AccessUnauthorizedException;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.NoWritingPermissionException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.server.VitamServer;
import fr.gouv.vitam.common.server.application.junit.ResponseHelper;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;


@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.net.ssl.*", "javax.management.*"})
@PrepareForTest({AccessInternalClientFactory.class, AdminManagementClientFactory.class})
public class AccessExternalResourceTest {
    @Rule
    public ExpectedException thrownException = ExpectedException.none();

    private static final String ACCESS_CONF = "access-external-test.conf";
    // URI
    private static final String ACCESS_RESOURCE_URI = "access-external/v1";
    private static final String ACCESS_UNITS_ID_URI = "/units/xyz";
    private static final String ACCESS_OBJECTS_ID_URI = "/units/xyz/objects";
    private static AccessExternalMain application;
    private static VitamServer vitamServer;

    // LOGGER
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AccessExternalResourceTest.class);
    private static JunitHelper junitHelper = JunitHelper.getInstance();
    private static int port = junitHelper.findAvailablePort();
    private static AccessInternalClient clientAccessInternal;

    private static final String BODY_TEST_SINGLE =
        "{\"$query\": {\"$eq\": {\"aa\" : \"vv\" }}, \"$projection\": {}, \"$filter\": {}}";
    private static final String BODY_TEST_MULTIPLE =
        "{\"$query\": [{\"$eq\": {\"aa\" : \"vv\" }}], \"$projection\": {}, \"$filter\": {}}";
    private static String good_id = "goodId";
    private static String bad_id = "badId";

    private static final String QUERY_TEST = "{ \"$query\" : [ { \"$eq\" : { \"title\" : \"test\" } } ], " +
        " \"$filter\" : { \"$orderby\" : { \"#id\":1 } }," +
        "\"$projection\" : {\"$fields\" : {\"#id\" : 1, \"title\":1, \"transacdate\":1}} }";

    private static final String QUERY_TEST_BY_ID =
        "{\"$projection\" : {\"$fields\" : {\"#id\" : 1, \"title\":1, \"transacdate\":1}} }";

    private static final String QUERY_TEST_BAD_VALIDATION_REQUEST =
        "{ \"$query\" : [ { \"$eq\" : { \"title\" : \"test\" } } ], " +
            " \"$filter\" : { \"$orderby\" : { \"#id\":1 } }," +
            "\"$projection\" : {\"$fields\" : {\"#id\" : 1, \"title\":1, \"transacdate\":1}} }";

    private static final String UPDATE_QUERY_VALID = "{ \"$action\": [ { \"$set\": { \"Title\": \"Titre test\" } } ] }";

    private static final String QUERY_SIMPLE_TEST = "{ \"$query\" : [ { \"$eq\" : { \"title\" : \"test\" } } ] }";

    private static final String BAD_QUERY_TEST = "{ \"$query\" ; [ { \"$eq\" : { \"title\" : \"test\" } } ] }";

    private static final String DATA =
        "{ \"#id\": \"aeaqaaaaaeaaaaakaarp4akuuf2ldmyaaaaq\", " + "\"data\": \"data1\" }";

    private static final String DATA2 =
        "{ \"#id\": \"aeaqaaaaaeaaaaakaarp4akuuf2ldmyaaaab\"," + "\"data\": \"data2\" }";

    private static final String DATA_TEST =
        "{ \"#id\": \"aeaqaaaaaeaaaaakaarp4akuuf2ldmyaaaaq\", " + "\"title\": \"test\"," + "\"data\": \"data1\" }";

    private static final String SELECT_RETURN =
        "{$hint: {'total':'1'},$context:{$query: {$eq: {\"id\" : \"1\" }}, $projection: {}, $filter: {}},$result:" +
            "[{'#id': '1', 'name': 'abcdef', 'creation_date': '2015-07-14T17:07:14Z', 'fmt': 'ftm/123', 'numerical_information': '55.3'}]}";

    private static final String DATA_HTML =
        "{ \"#id\": \"<a href='www.culture.gouv.fr'>Culture</a>\"," + "\"data\": \"data2\" }";

    private static final String ID = "identifier4";
    // LOGGER
    private static final String ACCESS_UNITS_URI = "/units";

    private static final String ID_UNIT = "identifier5";
    private static final String TENANT_ID = "0";
    private static final String UNEXTISTING_TENANT_ID = "25";
    private static final String GOOD_ID = "goodId";


    private static final String OBJECT_ID = "objectId";
    private static final String OBJECT_URI = "/objects";
    private static final String OBJECT_RETURN =
        "{$hint: {'total':'1'},$context:{$query: {$eq: {\"id\" : \"ArchiveUnit1\" }}, " +
            "$projection: {}, $filter: {}},$result:[{\"#id\":\"1\",\"#tenant\":0,\"#object\":\"" + OBJECT_ID +
            "\",\"#version\":0}]}";

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

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
        clientAccessInternal = PowerMockito.mock(AccessInternalClient.class);
        final AccessInternalClientFactory clientAccessInternalFactory =
            PowerMockito.mock(AccessInternalClientFactory.class);
        PowerMockito.when(AccessInternalClientFactory.getInstance()).thenReturn(clientAccessInternalFactory);
        PowerMockito.when(AccessInternalClientFactory.getInstance().getClient())
            .thenReturn(clientAccessInternal);

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
        VitamClientFactory.resetConnections();
        fr.gouv.vitam.common.external.client.VitamClientFactory.resetConnections();
    }

    private static JsonNode buildDSLWithOptions(String query, String data) throws InvalidParseOperationException {
        return JsonHandler
            .getFromString("{ \"$roots\" : [ \"\" ], \"$query\" : [ " + query + " ], \"$data\" : " + data + " }");
    }

    private static JsonNode buildDSLWithRoots(String data) throws InvalidParseOperationException {
        return JsonHandler
            .getFromString("{ \"$roots\" : [ " + data + " ], \"$query\" : [ '' ], \"$data\" : " + data + " }");
    }

    private Map<String, Object> getStreamHeadersUnknwonTenant() {
        final Map<String, Object> headers = new HashMap<>();
        headers.put(GlobalDataRest.X_TENANT_ID, UNEXTISTING_TENANT_ID);
        headers.put(GlobalDataRest.X_QUALIFIER, "qualif");
        headers.put(GlobalDataRest.X_VERSION, 1);
        return headers;
    }

    private Map<String, Object> getStreamHeaders() {
        final Map<String, Object> headers = new HashMap<>();
        headers.put(GlobalDataRest.X_TENANT_ID, TENANT_ID);
        headers.put(GlobalDataRest.X_QUALIFIER, "qualif");
        headers.put(GlobalDataRest.X_VERSION, 1);
        return headers;
    }

    private Map<String, Object> getStreamHeadersWithoutTenant() {
        final Map<String, Object> headers = new HashMap<>();
        headers.put(GlobalDataRest.X_QUALIFIER, "qualif");
        headers.put(GlobalDataRest.X_VERSION, 1);
        return headers;
    }

    @Test
    public void givenStartedServerHtpOverride_WhenRequestNotJson_ThenReturnError_UnsupportedMediaType()
        throws Exception {
        given()
            .contentType(ContentType.XML).header(X_HTTP_METHOD_OVERRIDE, "GET")
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(buildDSLWithOptions(QUERY_TEST, DATA2).asText())
            .when().post(ACCESS_UNITS_URI).then().statusCode(Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode());

        given()
            .contentType(ContentType.XML).header(X_HTTP_METHOD_OVERRIDE, "GET")
            .header(GlobalDataRest.X_TENANT_ID, UNEXTISTING_TENANT_ID)
            .body(buildDSLWithOptions(QUERY_TEST, DATA2).asText())
            .when().post(ACCESS_UNITS_URI).then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.XML).header(X_HTTP_METHOD_OVERRIDE, "GET")
            .body(buildDSLWithOptions(QUERY_TEST, DATA2).asText())
            .when().post(ACCESS_UNITS_URI).then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }


    @Test
    public void givenStartedServer_WhenRequestNotJson_ThenReturnOK() throws Exception {
        given()
            .contentType(ContentType.XML).header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(buildDSLWithOptions(QUERY_TEST, DATA2).asText())
            .when().get(ACCESS_UNITS_URI).then().statusCode(Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode());

        given()
            .contentType(ContentType.XML).header(GlobalDataRest.X_TENANT_ID, UNEXTISTING_TENANT_ID)
            .body(buildDSLWithOptions(QUERY_TEST, DATA2).asText())
            .when().get(ACCESS_UNITS_URI).then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .body(buildDSLWithOptions(QUERY_TEST, DATA2).asText())
            .when().get(ACCESS_UNITS_URI).then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }


    @Test
    public void givenStartedServer_WhenRequestNotJson_ThenReturnError_UnsupportedMediaType() throws Exception {
        given()
            .contentType(ContentType.XML).header(X_HTTP_METHOD_OVERRIDE, "GET")
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(buildDSLWithOptions(QUERY_TEST, DATA2).asText())
            .when().post(ACCESS_UNITS_URI).then().statusCode(Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode());

        given()
            .contentType(ContentType.XML).header(X_HTTP_METHOD_OVERRIDE, "GET")
            .header(GlobalDataRest.X_TENANT_ID, UNEXTISTING_TENANT_ID)
            .body(buildDSLWithOptions(QUERY_TEST, DATA2).asText())
            .when().post(ACCESS_UNITS_URI).then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.XML).header(X_HTTP_METHOD_OVERRIDE, "GET")
            .body(buildDSLWithOptions(QUERY_TEST, DATA2).asText())
            .when().post(ACCESS_UNITS_URI).then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }



    @Test
    public void givenStartedServer_WhenRequestNotJson_ThenReturnError_SelectObjectById_UnsupportedMediaType()
        throws Exception {
        given()
            .contentType(ContentType.XML).header(X_HTTP_METHOD_OVERRIDE, "GET")
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(buildDSLWithRoots("\"" + ID + "\"").asText())
            .when().post(ACCESS_UNITS_ID_URI).then().statusCode(Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode());

        given()
            .contentType(ContentType.XML).header(X_HTTP_METHOD_OVERRIDE, "GET")
            .header(GlobalDataRest.X_TENANT_ID, UNEXTISTING_TENANT_ID)
            .body(buildDSLWithRoots("\"" + ID + "\"").asText())
            .when().post(ACCESS_UNITS_ID_URI).then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.XML).header(X_HTTP_METHOD_OVERRIDE, "GET")
            .body(buildDSLWithRoots("\"" + ID + "\"").asText())
            .when().post(ACCESS_UNITS_ID_URI).then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }


    @Test
    public void givenStartedServer_WhenRequestNotJson_ThenReturnError_SelectById_UnsupportedMediaType()
        throws Exception {
        given()
            .contentType(ContentType.XML).header(X_HTTP_METHOD_OVERRIDE, "GET")
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(buildDSLWithRoots("\"" + ID + "\"").asText())
            .when().post(ACCESS_OBJECTS_ID_URI).then().statusCode(Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode());

        given()
            .contentType(ContentType.XML).header(X_HTTP_METHOD_OVERRIDE, "GET")
            .header(GlobalDataRest.X_TENANT_ID, UNEXTISTING_TENANT_ID)
            .body(buildDSLWithRoots("\"" + ID + "\"").asText())
            .when().post(ACCESS_OBJECTS_ID_URI).then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.XML).header(X_HTTP_METHOD_OVERRIDE, "GET")
            .body(buildDSLWithRoots("\"" + ID + "\"").asText())
            .when().post(ACCESS_OBJECTS_ID_URI).then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void given_bad_header_when_SelectByID_thenReturn_Not_allowed() {
        given()
            .contentType(ContentType.JSON)
            .headers(new Headers(new Header(X_HTTP_METHOD_OVERRIDE, "ABC"),
                new Header(GlobalDataRest.X_TENANT_ID, TENANT_ID)))
            .body(BODY_TEST_SINGLE)
            .when()
            .post("/units/" + ID_UNIT)
            .then()
            .statusCode(Status.METHOD_NOT_ALLOWED.getStatusCode());



        given()
            .contentType(ContentType.JSON)
            .headers(new Headers(new Header(X_HTTP_METHOD_OVERRIDE, "ABC"),
                new Header(GlobalDataRest.X_TENANT_ID, UNEXTISTING_TENANT_ID)))
            .body(BODY_TEST_SINGLE)
            .when()
            .post("/units/" + ID_UNIT)
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "ABC")
            .body(BODY_TEST_SINGLE)
            .when()
            .post("/units/" + ID_UNIT)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }


    @Test
    public void given_pathWithId_when_get_SelectByID()
        throws AccessInternalClientServerException, AccessInternalClientNotFoundException,
        InvalidParseOperationException, AccessUnauthorizedException {


        SelectParserMultiple selectParserMultiple = new SelectParserMultiple();
        selectParserMultiple.parse(JsonHandler.getFromString(QUERY_TEST_BY_ID));

        SelectMultiQuery selectMultiQuery = selectParserMultiple.getRequest();
        selectMultiQuery.addRoots(ID_UNIT);


        PowerMockito.when(clientAccessInternal.selectUnitbyId(selectMultiQuery.getFinalSelect(), ID_UNIT))
            .thenReturn(new RequestResponseOK().addResult(JsonHandler.getFromString(SELECT_RETURN)));

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(selectMultiQuery.getFinalSelectById())
            .when()
            .post("/units/" + ID_UNIT)
            .then()
            .statusCode(Status.OK.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and().header(GlobalDataRest.X_TENANT_ID, UNEXTISTING_TENANT_ID)
            .body(QUERY_TEST)
            .when()
            .post("/units/" + ID_UNIT)
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .body(QUERY_TEST)
            .when()
            .post("/units/" + ID_UNIT)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }


    @Test
    public void givenStartedServer_WhenJsonContainsHtml_ThenReturnError_getUnitById_PreconditionFailed()
        throws Exception {
        // HERE
        given()
            .contentType(ContentType.JSON).header(X_HTTP_METHOD_OVERRIDE, "GET")
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(buildDSLWithRoots(DATA_HTML))
            .accept(ContentType.JSON)
            .when()
            .get("/units/" + ID_UNIT)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON).header(X_HTTP_METHOD_OVERRIDE, "GET")
            .header(GlobalDataRest.X_TENANT_ID, UNEXTISTING_TENANT_ID)
            .body(buildDSLWithRoots(DATA_HTML))
            .when()
            .get("/units/" + ID_UNIT)
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON).header(X_HTTP_METHOD_OVERRIDE, "GET")
            .body(buildDSLWithRoots(DATA_HTML))
            .when()
            .get("/units/" + ID_UNIT)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void givenStartedServer_WhenJsonContainsHtml_ThenReturnError_createOrSelectUnitById_PreconditionFailed()
        throws Exception {
        // HERE
        given()
            .contentType(ContentType.JSON).header(X_HTTP_METHOD_OVERRIDE, "GET")
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .accept(ContentType.JSON)
            .body(buildDSLWithRoots(DATA_HTML))
            .when()
            .post("/units/" + ID_UNIT)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON).header(X_HTTP_METHOD_OVERRIDE, "GET")
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, UNEXTISTING_TENANT_ID)
            .body(buildDSLWithRoots(DATA_HTML))
            .when()
            .post("/units/" + ID_UNIT)
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON).header(X_HTTP_METHOD_OVERRIDE, "GET")
            .accept(ContentType.JSON)
            .body(buildDSLWithRoots(DATA_HTML))
            .when()
            .post("/units/" + ID_UNIT)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }


    @Test
    public void givenStartedServer_WhenJsonContainsHtml_ThenReturnError_updateUnitById_PreconditionFailed()
        throws Exception {
        // HERE
        given()
            .contentType(ContentType.JSON).header(X_HTTP_METHOD_OVERRIDE, "GET")
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(buildDSLWithRoots(DATA_HTML))
            .when()
            .put("/units/" + ID_UNIT)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON).header(X_HTTP_METHOD_OVERRIDE, "GET")
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, UNEXTISTING_TENANT_ID)
            .body(buildDSLWithRoots(DATA_HTML))
            .when()
            .put("/units/" + ID_UNIT)
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON).header(X_HTTP_METHOD_OVERRIDE, "GET")
            .accept(ContentType.JSON)
            .body(buildDSLWithRoots(DATA_HTML))
            .when()
            .put("/units/" + ID_UNIT)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }



    @Test
    @Deprecated
    public void givenStartedServer_WhenJsonContainsHtml_ThenReturnError_getObjectGroup_PreconditionFailed()
        throws Exception {
        // HERE
        given()
            .contentType(ContentType.JSON).header(X_HTTP_METHOD_OVERRIDE, "GET")
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(buildDSLWithRoots(DATA_HTML))
            .when()
            .get("/units/" + ID_UNIT + "/objects")
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON).header(X_HTTP_METHOD_OVERRIDE, "GET")
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, UNEXTISTING_TENANT_ID)
            .body(buildDSLWithRoots(DATA_HTML))
            .when()
            .get("/units/" + ID_UNIT + "/objects")
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON).header(X_HTTP_METHOD_OVERRIDE, "GET")
            .accept(ContentType.JSON)
            .body(buildDSLWithRoots(DATA_HTML))
            .when()
            .get("/units/" + ID_UNIT + "/objects")
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }



    @Test
    public void givenStartedServer_WhenJsonContainsHtml_ThenReturnError_getObjectGroupMetadatas_PreconditionFailed()
        throws Exception {
        // HERE
        given()
            .contentType(ContentType.JSON).header(X_HTTP_METHOD_OVERRIDE, "GET")
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(buildDSLWithRoots(DATA_HTML))
            .when()
            .get("/units/" + ID_UNIT + "/objects/")
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON).header(X_HTTP_METHOD_OVERRIDE, "GET")
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, UNEXTISTING_TENANT_ID)
            .body(buildDSLWithRoots(DATA_HTML))
            .when()
            .get("/units/" + ID_UNIT + "/objects/")
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON).header(X_HTTP_METHOD_OVERRIDE, "GET")
            .accept(ContentType.JSON)
            .body(buildDSLWithRoots(DATA_HTML))
            .when()
            .get("/units/" + ID_UNIT + "/objects/")
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }


    @Test
    @Deprecated
    public void givenStartedServer_WhenJsonContainsHtml_ThenReturnError_getObjectGroupPost_PreconditionFailed()
        throws Exception {
        // HERE
        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .accept(ContentType.JSON)
            .body(buildDSLWithRoots(DATA_HTML))
            .when()
            .get("/units/" + ID_UNIT + "/objects")
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, UNEXTISTING_TENANT_ID)
            .accept(ContentType.JSON)
            .body(buildDSLWithRoots(DATA_HTML))
            .when()
            .get("/units/" + ID_UNIT + "/objects")
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(buildDSLWithRoots(DATA_HTML))
            .when()
            .get("/units/" + ID_UNIT + "/objects")
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }


    @Test
    public void givenStartedServer_WhenJsonContainsHtml_ThenReturnError_getObjectGroupMetadatasPost_PreconditionFailed()
        throws Exception {
        // HERE
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(buildDSLWithRoots(DATA_HTML))
            .when()
            .get("/units/" + ID_UNIT + "/objects/")
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, UNEXTISTING_TENANT_ID)
            .body(buildDSLWithRoots(DATA_HTML))
            .when()
            .get("/units/" + ID_UNIT + "/objects/")
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(buildDSLWithRoots(DATA_HTML))
            .when()
            .get("/units/" + ID_UNIT + "/objects/")
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void testUpdateUnitById() throws Exception {
        reset(clientAccessInternal);
        PowerMockito.when(clientAccessInternal.updateUnitbyId(anyObject(), anyObject()))
            .thenReturn(new RequestResponseOK<JsonNode>().addResult(
                JsonHandler.getFromString("{'#id': '1', 'Title': 'Archive 1', 'DescriptionLevel': 'Archive Mock'}")));

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(UPDATE_QUERY_VALID)
            .when()
            .put("/units/" + ID)
            .then()
            .statusCode(Status.OK.getStatusCode());

        UpdateMultiQuery updateMultiQuery = new UpdateMultiQuery();
        updateMultiQuery.addActions(UpdateActionHelper.set("Title", "new title"));
        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(updateMultiQuery.getFinalUpdateById())
            .when()
            .put("/units/" + ID)
            .then()
            .statusCode(Status.OK.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(updateMultiQuery.getFinalUpdate())
            .when()
            .put("/units/" + ID)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());

        UpdateMultiQuery emptyUpdateMultiQuery = new UpdateMultiQuery();
        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(emptyUpdateMultiQuery.getFinalUpdate())
            .when()
            .put("/units/" + ID)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());

        UpdateMultiQuery queryUpdateMultiQuery = new UpdateMultiQuery();
        queryUpdateMultiQuery.setQuery(eq("#id", "1"));
        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(updateMultiQuery.getFinalUpdate())
            .when()
            .put("/units/" + ID)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());

        SelectMultiQuery selectMultiQuery = new SelectMultiQuery();
        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(selectMultiQuery.getFinalSelect())
            .when()
            .put("/units/" + ID)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, UNEXTISTING_TENANT_ID)
            .body(buildDSLWithOptions(QUERY_SIMPLE_TEST, DATA))
            .when()
            .put("/units/" + ID)
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(buildDSLWithOptions(QUERY_SIMPLE_TEST, DATA))
            .when()
            .put("/units/" + ID)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());


    }


    @Test
    public void testAccessUnits() throws Exception {
        reset(clientAccessInternal);
        PowerMockito.when(clientAccessInternal.selectUnits(anyObject()))
            .thenReturn(new RequestResponseOK().addResult(JsonHandler.getFromString(DATA_TEST)).setHttpCode(200));
        // Multiple Query DSL Validator Ok
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(QUERY_TEST)
            .when()
            .get(ACCESS_UNITS_URI).then().statusCode(Status.OK.getStatusCode());


        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, UNEXTISTING_TENANT_ID)
            .body(QUERY_TEST)
            .when()
            .get(ACCESS_UNITS_URI)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(QUERY_TEST)
            .when()
            .get(ACCESS_UNITS_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void testHttpOverrideAccessUnits() throws Exception {
        reset(clientAccessInternal);
        PowerMockito.when(clientAccessInternal.selectUnits(anyObject()))
            .thenReturn(new RequestResponseOK().addResult(JsonHandler.getFromString(DATA_TEST)).setHttpCode(200));
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(QUERY_TEST)
            .when()
            .get(ACCESS_UNITS_URI)
            .then().statusCode(Status.OK.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, UNEXTISTING_TENANT_ID)
            .body(QUERY_TEST)
            .when()
            .get(ACCESS_UNITS_URI)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(QUERY_TEST)
            .when()
            .get(ACCESS_UNITS_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(QUERY_TEST)
            .headers(new Headers(new Header(X_HTTP_METHOD_OVERRIDE, "GET"),
                new Header(GlobalDataRest.X_TENANT_ID, TENANT_ID)))
            .when()
            .post(ACCESS_UNITS_URI)
            .then().statusCode(Status.OK.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(QUERY_TEST)
            .headers(new Headers(new Header(X_HTTP_METHOD_OVERRIDE, "GET"),
                new Header(GlobalDataRest.X_TENANT_ID, UNEXTISTING_TENANT_ID)))
            .when()
            .post(ACCESS_UNITS_URI)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(QUERY_TEST)
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .when()
            .post(ACCESS_UNITS_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void testErrorSelectUnitsById()
        throws AccessInternalClientServerException, AccessInternalClientNotFoundException,
        InvalidParseOperationException, AccessUnauthorizedException {

        try {
            PowerMockito.when(clientAccessInternal.selectUnitbyId(JsonHandler.getFromString(BAD_QUERY_TEST), good_id))
                .thenThrow(InvalidParseOperationException.class);
        } catch (final InvalidParseOperationException e) {
            given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
                .body(BAD_QUERY_TEST).when().get("units/goodId").then()
                .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

            given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                .header(GlobalDataRest.X_TENANT_ID, UNEXTISTING_TENANT_ID)
                .body(BAD_QUERY_TEST).when().get("units/goodId").then()
                .statusCode(Status.UNAUTHORIZED.getStatusCode());

            given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                .body(BAD_QUERY_TEST).when().get("units/goodId").then()
                .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
        }


        try {
            PowerMockito.when(clientAccessInternal.selectUnitbyId(JsonHandler.getFromString(BAD_QUERY_TEST), bad_id))
                .thenThrow(InvalidParseOperationException.class);
        } catch (final InvalidParseOperationException e) {
            given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
                .body(BAD_QUERY_TEST).when().get("units/badId").then()
                .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

            given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                .header(GlobalDataRest.X_TENANT_ID, UNEXTISTING_TENANT_ID)
                .body(BAD_QUERY_TEST).when().get("units/badId").then()
                .statusCode(Status.UNAUTHORIZED.getStatusCode());

            given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                .body(BAD_QUERY_TEST).when().get("units/badId").then()
                .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
        }

        try {
            PowerMockito
                .when(clientAccessInternal.selectUnitbyId(JsonHandler.getFromString("INTERAL_SEVER_ERROR"), bad_id))
                .thenThrow(InvalidParseOperationException.class);
        } catch (final InvalidParseOperationException e) {
            given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
                .body("INTERAL_SEVER_ERROR").when().get("units/badId").then()
                .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

            given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                .header(GlobalDataRest.X_TENANT_ID, UNEXTISTING_TENANT_ID)
                .body("INTERAL_SEVER_ERROR").when().get("units/badId").then()
                .statusCode(Status.UNAUTHORIZED.getStatusCode());

            given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                .body("INTERAL_SEVER_ERROR").when().get("units/badId").then()
                .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
        }
    }


    @Test
    public void testErrorsUpdateUnitsById()
        throws AccessInternalClientServerException, AccessInternalClientNotFoundException,
        InvalidParseOperationException, NoWritingPermissionException, AccessUnauthorizedException {

        try {
            PowerMockito.when(clientAccessInternal.updateUnitbyId(JsonHandler.getFromString(BAD_QUERY_TEST), good_id))
                .thenThrow(InvalidParseOperationException.class);
        } catch (final InvalidParseOperationException e) {
            given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
                .body(BAD_QUERY_TEST).when().put("units/goodId").then()
                .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

            given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                .header(GlobalDataRest.X_TENANT_ID, UNEXTISTING_TENANT_ID)
                .body(BAD_QUERY_TEST).when().put("units/goodId").then()
                .statusCode(Status.UNAUTHORIZED.getStatusCode());

            given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                .body(BAD_QUERY_TEST).when().put("units/goodId").then()
                .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
        }

        try {
            PowerMockito.when(clientAccessInternal.updateUnitbyId(JsonHandler.getFromString(BAD_QUERY_TEST), bad_id))
                .thenThrow(InvalidParseOperationException.class);
        } catch (final InvalidParseOperationException e) {
            given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
                .body(BAD_QUERY_TEST).when().put("units/badId").then()
                .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

            given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                .header(GlobalDataRest.X_TENANT_ID, UNEXTISTING_TENANT_ID)
                .body(BAD_QUERY_TEST).when().put("units/badId").then()
                .statusCode(Status.UNAUTHORIZED.getStatusCode());

            given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                .body(BAD_QUERY_TEST).when().put("units/badId").then()
                .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
        }

        try {
            PowerMockito
                .when(clientAccessInternal.updateUnitbyId(JsonHandler.getFromString("INTERAL_SEVER_ERROR"), bad_id))
                .thenThrow(InvalidParseOperationException.class);
        } catch (final InvalidParseOperationException e) {
            given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
                .body("INTERAL_SEVER_ERROR").when().put("units/badId").then()
                .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

            given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                .header(GlobalDataRest.X_TENANT_ID, UNEXTISTING_TENANT_ID)
                .body("INTERAL_SEVER_ERROR").when().put("units/badId").then()
                .statusCode(Status.UNAUTHORIZED.getStatusCode());

            given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                .body("INTERAL_SEVER_ERROR").when().put("units/badId").then()
                .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
        }
    }


    @Test
    public void testErrorsSelectUnits()
        throws AccessInternalClientServerException, AccessInternalClientNotFoundException,
        InvalidParseOperationException, AccessUnauthorizedException, BadRequestException, VitamDBException {

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .accept(ContentType.JSON)
            .body("{BAD_QUERY_TEST_UNITS}")
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get(ACCESS_UNITS_URI)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body("{BAD_QUERY_TEST_UNITS}")
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get(ACCESS_UNITS_URI)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        PowerMockito.doThrow(new AccessInternalClientServerException(""))
            .when(clientAccessInternal).selectUnits(anyObject());

        // Wrong Query for ACCESS_UNITS_URI accept only select-multiple Schema and not select-single
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(BODY_TEST_SINGLE)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get(ACCESS_UNITS_URI)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());

        PowerMockito.doThrow(new AccessInternalClientNotFoundException(""))
            .when(clientAccessInternal).selectUnits(anyObject());

        given()
            .contentType(ContentType.JSON)
            .body(BODY_TEST_SINGLE)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get(ACCESS_UNITS_URI)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get(ACCESS_UNITS_URI)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

    }

    @Test
    public void testOkSelectUnits() throws Exception {
        PowerMockito.when(clientAccessInternal.selectUnits(anyObject()))
            .thenReturn(new RequestResponseOK().addResult(JsonHandler.getFromString(DATA_TEST)).setHttpCode(200));
        // Query Validation Ok
        JsonNode queryNode = JsonHandler.getFromString(BODY_TEST_MULTIPLE);
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(queryNode)
            .headers(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get(ACCESS_UNITS_URI)
            .then()
            .statusCode(Status.OK.getStatusCode());

        // Validation Ok
        given()
            .contentType(ContentType.JSON)
            .body(JsonHandler.getFromString(BODY_TEST_MULTIPLE))
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(ACCESS_UNITS_URI)
            .then()
            .statusCode(Status.OK.getStatusCode());

    }

    @Test
    public void testhttpOverrideErrorsSelectUnits()
        throws AccessInternalClientServerException, AccessInternalClientNotFoundException,
        InvalidParseOperationException, AccessUnauthorizedException, BadRequestException, VitamDBException {

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body("{BAD_QUERY_TEST_UNITS}")
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get(ACCESS_UNITS_URI)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body("{BAD_QUERY_TEST_UNITS}")
            .header(GlobalDataRest.X_TENANT_ID, UNEXTISTING_TENANT_ID)
            .when()
            .get(ACCESS_UNITS_URI)
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body("{BAD_QUERY_TEST_UNITS}")
            .when()
            .get(ACCESS_UNITS_URI)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        PowerMockito.doThrow(new AccessInternalClientServerException(""))
            .when(clientAccessInternal).selectUnits(anyObject());

        // Wrong Query for ACCESS_UNITS_URI accept only select-multiple Schema and not select-single
        given()
            .contentType(ContentType.JSON)
            .body(BODY_TEST_SINGLE)
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(ACCESS_UNITS_URI)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());

        PowerMockito.doThrow(new AccessInternalClientNotFoundException(""))
            .when(clientAccessInternal).selectUnits(anyObject());

        given()
            .contentType(ContentType.JSON)
            .body(BODY_TEST_SINGLE)
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(ACCESS_UNITS_URI)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());

        PowerMockito.doThrow(new BadRequestException("Bad Request"))
            .when(clientAccessInternal).selectUnits(anyObject());

        given()
            .contentType(ContentType.JSON)
            .body(BODY_TEST_SINGLE)
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(ACCESS_UNITS_URI)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());

    }



    @Test
    @Deprecated
    public void getObjectGroupPost() throws Exception {
        reset(clientAccessInternal);
        final JsonNode result = JsonHandler.getFromString(BODY_TEST_SINGLE);
        when(clientAccessInternal.selectObjectbyId(JsonHandler.getFromString("\"" + anyString() + "\""),
            "\"" + anyString() + "\""))
                .thenReturn(new RequestResponseOK().addResult(result));
        final JsonNode resultObjectReturn = JsonHandler.getFromString(OBJECT_RETURN);
        when(clientAccessInternal.selectUnitbyId(JsonHandler.getFromString("\"" + anyString() + "\""),
            "\"" + anyString() + "\""))
                .thenReturn(new RequestResponseOK().addResult(resultObjectReturn));

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
            .body(JsonHandler.getFromString(QUERY_TEST_BY_ID))
            .headers(getStreamHeaders()).header(X_HTTP_METHOD_OVERRIDE,
                "GET")
            .when().post("/units/" + ID_UNIT + "/objects").then()
            .statusCode(Status.OK.getStatusCode()).contentType(MediaType.APPLICATION_JSON);

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
            .body(JsonHandler.getFromString(QUERY_TEST_BY_ID))
            .headers(getStreamHeadersUnknwonTenant()).header(X_HTTP_METHOD_OVERRIDE,
                "GET")
            .when().post("/units/" + ID_UNIT + "/objects").then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
            .body(JsonHandler.getFromString(QUERY_TEST_BY_ID))
            .headers(getStreamHeadersWithoutTenant()).header(X_HTTP_METHOD_OVERRIDE,
                "GET")
            .when().post("/units/" + ID_UNIT + "/objects").then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
            .body(QUERY_TEST_BY_ID)
            .headers(getStreamHeaders()).header(X_HTTP_METHOD_OVERRIDE,
                "PUT")
            .when().get("/units/" + ID_UNIT + "/objects").then()
            .statusCode(Status.OK.getStatusCode());

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
            .body(QUERY_TEST_BY_ID)
            .headers(getStreamHeadersUnknwonTenant()).header(X_HTTP_METHOD_OVERRIDE,
                "PUT")
            .when().get("/units/" + ID_UNIT + "/objects").then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
            .body(QUERY_TEST_BY_ID)
            .headers(getStreamHeadersWithoutTenant()).header(X_HTTP_METHOD_OVERRIDE,
                "PUT")
            .when().get("/units/" + ID_UNIT + "/objects").then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        reset(clientAccessInternal);
        when(clientAccessInternal.selectObjectbyId(JsonHandler.getFromString("\"" + anyString() + "\""),
            "\"" + anyString() + "\""))
                .thenThrow(new AccessInternalClientServerException(""));

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
            .body(QUERY_TEST_BY_ID)
            .headers(getStreamHeaders()).header(X_HTTP_METHOD_OVERRIDE, "GET")
            .when().get("/units/" + ID_UNIT + "/objects").then()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());

        reset(clientAccessInternal);
        when(clientAccessInternal.selectObjectbyId(JsonHandler.getFromString("\"" + anyString() + "\""),
            "\"" + anyString() + "\""))
                .thenThrow(new AccessInternalClientServerException(""));

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
            .body(QUERY_TEST_BY_ID)
            .headers(getStreamHeaders()).header(X_HTTP_METHOD_OVERRIDE, "GET")
            .when().post("/units/" + ID_UNIT + "/objects").then()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }


    @Test
    public void getObjectGroupMetadata() throws Exception {
        reset(clientAccessInternal);
        String unitId = "good_id";
        JsonNode unit = JsonHandler.getFromString(
            "{\"#id\":\"1\",\"#object\":\"goodResult\",\"Title\":\"Archive 1\",\"DescriptionLevel\":\"Archive Mock\"}");
        JsonNode unitNoObject =
            JsonHandler.getFromString("{\"#id\":\"1\",\"Title\":\"Archive 1\",\"DescriptionLevel\":\"Archive Mock\"}");
        JsonNode got = JsonHandler.getFromString("{\"#id\":\"goodResult\"}");
        final RequestResponse<JsonNode> responseUnit =
            new RequestResponseOK<JsonNode>(JsonHandler.getFromString(BODY_TEST_MULTIPLE)).addResult(unit)
                .setHttpCode(200);
        final RequestResponse<JsonNode> responseUnitNoObject =
            new RequestResponseOK<JsonNode>(JsonHandler.getFromString(BODY_TEST_MULTIPLE)).addResult(unitNoObject)
                .setHttpCode(200);
        final RequestResponse<JsonNode> responseGOT =
            new RequestResponseOK<JsonNode>(JsonHandler.getFromString(BODY_TEST_MULTIPLE)).addResult(got)
                .setHttpCode(200);
        when(clientAccessInternal.selectUnitbyId(JsonHandler.getFromString("\"" + anyString() + "\""),
            "\"" + anyString() + "\""))
                .thenReturn(responseUnit);
        when(clientAccessInternal.selectObjectbyId(JsonHandler.getFromString("\"" + anyString() + "\""),
            "\"" + anyString() + "\""))
                .thenReturn(responseGOT);

        // POST override GET ok
        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
            .body(JsonHandler.getFromString(QUERY_TEST_BY_ID))
            .headers(getStreamHeaders()).header(X_HTTP_METHOD_OVERRIDE,
                "GET")
            .when().post("/units/" + unitId + "/objects").then()
            .statusCode(Status.OK.getStatusCode()).contentType(MediaType.APPLICATION_JSON);

        // GET ok
        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
            .body(JsonHandler.getFromString(QUERY_TEST_BY_ID))
            .headers(getStreamHeaders())
            .when().get("/units/" + unitId + "/objects").then()
            .statusCode(Status.OK.getStatusCode()).contentType(MediaType.APPLICATION_JSON);

        // POST override GET no tenant
        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
            .body(JsonHandler.getFromString(QUERY_TEST_BY_ID))
            .headers(getStreamHeadersUnknwonTenant()).header(X_HTTP_METHOD_OVERRIDE,
                "GET")
            .when().post("/units/" + unitId + "/objects").then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        // GET no tenant
        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
            .body(JsonHandler.getFromString(QUERY_TEST_BY_ID))
            .headers(getStreamHeadersUnknwonTenant())
            .when().get("/units/" + unitId + "/objects").then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        // GET (PUT override isn't filtered) ok
        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
            .body(QUERY_TEST_BY_ID)
            .headers(getStreamHeaders()).header(X_HTTP_METHOD_OVERRIDE, "PUT")
            .when().get("/units/" + unitId + "/objects").then()
            .statusCode(Status.OK.getStatusCode());

        // POST (PUT override isn't filtered) unmapped
        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
            .body(QUERY_TEST_BY_ID)
            .headers(getStreamHeaders()).header(X_HTTP_METHOD_OVERRIDE, "PUT")
            .when().post("/units/" + unitId + "/objects").then()
            .statusCode(Status.METHOD_NOT_ALLOWED.getStatusCode());

        // applicative error 500
        reset(clientAccessInternal);
        when(clientAccessInternal.selectUnitbyId(JsonHandler.getFromString("\"" + anyString() + "\""),
            "\"" + anyString() + "\"")).thenReturn(responseUnit);
        when(clientAccessInternal.selectObjectbyId(JsonHandler.getFromString("\"" + anyString() + "\""),
            "\"" + anyString() + "\""))
                .thenThrow(new AccessInternalClientServerException(""));

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
            .body(QUERY_TEST_BY_ID)
            .headers(getStreamHeaders())
            .when().get("/units/" + unitId + "/objects").then()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());

        // applicative error 412
        reset(clientAccessInternal);
        when(clientAccessInternal.selectUnitbyId(JsonHandler.getFromString("\"" + anyString() + "\""),
            "\"" + anyString() + "\"")).thenReturn(responseUnit);
        when(clientAccessInternal.selectObjectbyId(JsonHandler.getFromString("\"" + anyString() + "\""),
            "\"" + anyString() + "\""))
                .thenThrow(new InvalidParseOperationException(""));

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
            .body(QUERY_TEST_BY_ID)
            .headers(getStreamHeaders())
            .when().get("/units/" + unitId + "/objects").then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        // applicative error 404
        reset(clientAccessInternal);
        when(clientAccessInternal.selectUnitbyId(JsonHandler.getFromString("\"" + anyString() + "\""),
            "\"" + anyString() + "\"")).thenReturn(responseUnit);
        when(clientAccessInternal.selectObjectbyId(JsonHandler.getFromString("\"" + anyString() + "\""),
            "\"" + anyString() + "\""))
                .thenThrow(new AccessInternalClientNotFoundException(""));

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
            .body(QUERY_TEST_BY_ID)
            .headers(getStreamHeaders())
            .when().get("/units/" + unitId + "/objects").then()
            .statusCode(Status.NOT_FOUND.getStatusCode());


        // applicative error 404 => unit without object
        reset(clientAccessInternal);
        when(clientAccessInternal.selectUnitbyId(JsonHandler.getFromString("\"" + anyString() + "\""),
            "\"" + anyString() + "\"")).thenReturn(responseUnitNoObject);
        when(clientAccessInternal.selectObjectbyId(JsonHandler.getFromString("\"" + anyString() + "\""),
            "\"" + anyString() + "\"")).thenReturn(new RequestResponseOK<JsonNode>().setHttpCode(200));

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
            .body(QUERY_TEST_BY_ID)
            .headers(getStreamHeaders())
            .when().get("/units/" + unitId + "/objects").then()
            .statusCode(Status.NOT_FOUND.getStatusCode());

        // applicative error 404 => object empty
        reset(clientAccessInternal);
        when(clientAccessInternal.selectUnitbyId(JsonHandler.getFromString("\"" + anyString() + "\""),
            "\"" + anyString() + "\"")).thenReturn(responseUnit);
        when(clientAccessInternal.selectObjectbyId(JsonHandler.getFromString("\"" + anyString() + "\""),
            "\"" + anyString() + "\"")).thenReturn(new RequestResponseOK<JsonNode>().setHttpCode(200));

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
            .body(QUERY_TEST_BY_ID)
            .headers(getStreamHeaders())
            .when().get("/units/" + unitId + "/objects").then()
            .statusCode(Status.NOT_FOUND.getStatusCode());

        // applicative error 401
        reset(clientAccessInternal);
        when(clientAccessInternal.selectUnitbyId(JsonHandler.getFromString("\"" + anyString() + "\""),
            "\"" + anyString() + "\"")).thenReturn(responseUnit);
        when(clientAccessInternal.selectObjectbyId(JsonHandler.getFromString("\"" + anyString() + "\""),
            "\"" + anyString() + "\""))
                .thenThrow(new AccessUnauthorizedException(""));

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
            .body(QUERY_TEST_BY_ID)
            .headers(getStreamHeaders())
            .when().get("/units/" + unitId + "/objects").then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());
    }

    @Test
    public void testGetObjectStream() throws Exception {
        final Map<String, String> headers = new HashMap<>();
        final String GET_OBJECT_STREAM_URI = ACCESS_UNITS_URI + "/" + GOOD_ID + OBJECT_URI;
        headers.put("Content-Length", "4");
        reset(clientAccessInternal);
        final Response response =
            ResponseHelper.getOutboundResponse(Status.OK, new ByteArrayInputStream("test".getBytes()),
                MediaType.APPLICATION_OCTET_STREAM, headers);

        final JsonNode resultObjectReturn = JsonHandler.getFromString(OBJECT_RETURN);
        PowerMockito.when(clientAccessInternal.selectUnitbyId(anyObject(), anyString()))
            .thenReturn(new RequestResponseOK().addResult(resultObjectReturn));

        PowerMockito.when(clientAccessInternal.getObject(anyString(), anyString(), anyInt()))
            .thenReturn(response);

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM)
            .headers(getStreamHeaders())
            .when().get(GET_OBJECT_STREAM_URI)
            .then()
            .statusCode(Status.OK.getStatusCode());

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM)
            .headers(getStreamHeadersUnknwonTenant())
            .when().get(GET_OBJECT_STREAM_URI)
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM)
            .headers(getStreamHeadersWithoutTenant())
            .when().get(GET_OBJECT_STREAM_URI)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_OCTET_STREAM)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().get(GET_OBJECT_STREAM_URI).then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_OCTET_STREAM)
            .header(GlobalDataRest.X_TENANT_ID, UNEXTISTING_TENANT_ID)
            .when().get(GET_OBJECT_STREAM_URI).then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_OCTET_STREAM)
            .when().get(GET_OBJECT_STREAM_URI).then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM)
            .headers(getStreamHeaders())
            .when().post(GET_OBJECT_STREAM_URI)
            .then()
            .statusCode(Status.METHOD_NOT_ALLOWED.getStatusCode());

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM)
            .headers(getStreamHeadersUnknwonTenant())
            .when().post(GET_OBJECT_STREAM_URI)
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());


        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM)
            .headers(getStreamHeadersWithoutTenant())
            .when().post(GET_OBJECT_STREAM_URI)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM)
            .headers(getStreamHeaders()).header(X_HTTP_METHOD_OVERRIDE,
                "PUT")
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(JsonHandler.getFromString(BODY_TEST_SINGLE)).when().post(GET_OBJECT_STREAM_URI).then()
            .statusCode(Status.METHOD_NOT_ALLOWED.getStatusCode());

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM)
            .headers(getStreamHeadersUnknwonTenant()).header(X_HTTP_METHOD_OVERRIDE,
                "PUT")
            .when().post(GET_OBJECT_STREAM_URI).then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM)
            .headers(getStreamHeadersWithoutTenant()).header(X_HTTP_METHOD_OVERRIDE,
                "PUT")
            .when().post(GET_OBJECT_STREAM_URI).then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

    }

    @Test
    public void getObjectUnit() throws Exception {
        reset(clientAccessInternal);

        final Map<String, String> headers = new HashMap<>();
        headers.put("Content-Length", "4");

        final Response response =
            ResponseHelper.getOutboundResponse(Status.OK, new ByteArrayInputStream("test".getBytes()),
                MediaType.APPLICATION_OCTET_STREAM, headers);

        PowerMockito.when(clientAccessInternal.getObject(anyString(), anyString(), anyInt()))
            .thenReturn(response);
        String objectnode = "{\"$query\": {\"$eq\": {\"aa\" : \"vv\" }}, \"$projection\": {}, \"$filter\": {}}";
        JsonNode objectGroup = JsonHandler.getFromString(
            "{\"$hint\":{\"total\":1},\"$context\":{\"$query\":{\"$eq\":{\"id\":\"1\"}},\"$projection\":{},\"$filter\":{}},\"$result\":[{\"#id\":\"1\",\"#object\":\"goodResult\",\"Title\":\"Archive 1\",\"DescriptionLevel\":\"Archive Mock\"}]}");
        final JsonNode result = JsonHandler.getFromString(objectnode);

        when(clientAccessInternal.selectObjectbyId(JsonHandler.getFromString("\"" + anyString() + "\""),
            "\"" + anyString() + "\"")).thenReturn(new RequestResponseOK<JsonNode>().addResult(result));

        when(clientAccessInternal.selectUnitbyId(JsonHandler.getFromString("\"" + anyString() + "\""),
            "\"" + anyString() + "\""))
                .thenReturn(new RequestResponseOK<JsonNode>().addResult(objectGroup));
        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM)
            .headers(getStreamHeaders()).when()
            .get(ACCESS_UNITS_URI + "/goodId/objects")
            .then()
            .statusCode(Status.OK.getStatusCode());

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM)
            .headers(getStreamHeadersUnknwonTenant()).when()
            .get(ACCESS_UNITS_URI + "/goodId/objects")
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM)
            .headers(getStreamHeadersWithoutTenant()).when()
            .get(ACCESS_UNITS_URI + "/goodId/objects")
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM)
            .headers(getStreamHeaders()).header(X_HTTP_METHOD_OVERRIDE,
                "GET")
            .when().post(ACCESS_UNITS_URI + "/goodId/objects").then()
            .statusCode(Status.OK.getStatusCode());

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM)
            .body(JsonHandler.getFromString(objectnode))
            .headers(getStreamHeadersUnknwonTenant()).header(X_HTTP_METHOD_OVERRIDE,
                "GET")
            .when().post(ACCESS_UNITS_URI + "/goodId/objects").then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM)
            .headers(getStreamHeadersWithoutTenant()).header(X_HTTP_METHOD_OVERRIDE,
                "GET")
            .when().post(ACCESS_UNITS_URI + "/goodId/objects").then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

    }


    @Test
    public void testErrorsGetObjects()
        throws AccessInternalClientServerException, AccessInternalClientNotFoundException,
        InvalidParseOperationException, AccessUnauthorizedException {
        JsonNode objectGroup = JsonHandler.getFromString(
            "{\"$hint\":{\"total\":1},\"$context\":{\"$query\":{\"$eq\":{\"id\":\"1\"}},\"$projection\":{},\"$filter\":{}},\"$result\":[{\"#id\":\"1\",\"#object\":\"goodResult\",\"Title\":\"Archive 1\",\"DescriptionLevel\":\"Archive Mock\"}]}");

        PowerMockito.when(clientAccessInternal.getObject(anyString(), anyString(), anyInt()))
            .thenThrow(new InvalidParseOperationException(""));
        PowerMockito.when(clientAccessInternal.selectUnitbyId(anyObject(), anyString()))
            .thenReturn(new RequestResponseOK().addResult(objectGroup));

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM)
            .headers(getStreamHeaders()).header(X_HTTP_METHOD_OVERRIDE, "GET")
            .when().get("/units/goodId/objects").then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM)
            .headers(getStreamHeadersUnknwonTenant()).header(X_HTTP_METHOD_OVERRIDE,
                "GET")
            .when().get("/units/goodId/objects").then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM)
            .headers(getStreamHeadersWithoutTenant()).header(X_HTTP_METHOD_OVERRIDE,
                "GET")
            .when().get("/units/goodId/objects").then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }


    @Test
    public void should_retrieve_dsl_validation_error_when_send_bad_query_dsl() {
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(QUERY_TEST_BAD_VALIDATION_REQUEST)
            .when()
            .post("/units/" + ID_UNIT)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void should_respond_no_content_when_status() {
        given()
            .accept(ContentType.JSON)
            .when()
            .get("/status")
            .then()
            .statusCode(Status.NO_CONTENT.getStatusCode());
    }

    @Test
    public void listResourceEndpoints()
        throws Exception {
        RestAssured.given()
            .accept(MediaType.APPLICATION_JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().options("/")
            .then().statusCode(Status.OK.getStatusCode())
            .body(CoreMatchers.containsString("units:read"));
    }
}
