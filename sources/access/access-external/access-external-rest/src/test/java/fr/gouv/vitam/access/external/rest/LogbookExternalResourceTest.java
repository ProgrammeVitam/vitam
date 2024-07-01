/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
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
package fr.gouv.vitam.access.external.rest;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.access.internal.client.AccessInternalClient;
import fr.gouv.vitam.access.internal.client.AccessInternalClientFactory;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.client.ClientMockResultHelper;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.parser.request.single.SelectParserSingle;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.server.application.junit.ResteasyTestApplication;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClient;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClientFactory;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.core.Response.Status;
import java.util.Set;

import static fr.gouv.vitam.common.GlobalDataRest.X_HTTP_METHOD_OVERRIDE;
import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class LogbookExternalResourceTest extends ResteasyTestApplication {

    private static final String TRACEABILITY_OPERATION_ID = "op_id";

    private static final String ACCESS_CONF = "access-external-test.conf";

    // URI
    private static final String ACCESS_RESOURCE_URI = "access-external/v1";

    private static AccessExternalMain application;

    // LOGGER
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookExternalResourceTest.class);

    private static JunitHelper junitHelper = JunitHelper.getInstance();
    private static int port = junitHelper.findAvailablePort();

    private static final int TENANT_ID = 0;
    private static final String UNEXISTING_TENANT_ID = "25";

    private static final String OPERATIONS_URI = "/logbookoperations";
    private static final String OPERATION_ID_URI = "/{id_op}";

    private static final String BODY_TEST = "{$query: {$eq: {\"aa\" : \"vv\" }}, $projection: {}, $filter: {}}";
    private static final String BODY_TEST_WITH_ID = "{$projection: {}}";
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

    private static final BusinessApplicationTest businessApplicationTest = new BusinessApplicationTest();

    private static final AccessInternalClientFactory accessInternalClientFactory =
        businessApplicationTest.getAccessInternalClientFactory();
    private static final AccessInternalClient accessInternalClient = mock(AccessInternalClient.class);

    private static final AdminManagementClientFactory adminManagementClientFactory =
        businessApplicationTest.getAdminManagementClientFactory();
    private static final AdminManagementClient adminManagementClient = mock(AdminManagementClient.class);

    private static final IngestInternalClientFactory ingestInternalClientFactory =
        businessApplicationTest.getIngestInternalClientFactory();
    private static final IngestInternalClient ingestInternalClient = mock(IngestInternalClient.class);

    @Override
    public Set<Object> getResources() {
        return businessApplicationTest.getSingletons();
    }

    @Override
    public Set<Class<?>> getClasses() {
        return businessApplicationTest.getClasses();
    }

    @BeforeClass
    public static void setUpBeforeClass() {
        junitHelper = JunitHelper.getInstance();
        port = junitHelper.findAvailablePort();
        try {
            application = new AccessExternalMain(ACCESS_CONF, LogbookExternalResourceTest.class, null);
            application.start();
            RestAssured.port = port;
            RestAssured.basePath = ACCESS_RESOURCE_URI;

            LOGGER.debug("Beginning tests");
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
            throw new IllegalStateException("Cannot start the Access Application Server", e);
        }
    }

    @Before
    public void setUpBefore() throws Exception {
        reset(accessInternalClient);
        reset(accessInternalClientFactory);
        reset(adminManagementClient);
        reset(adminManagementClientFactory);
        reset(ingestInternalClient);
        reset(ingestInternalClientFactory);

        when(accessInternalClientFactory.getClient()).thenReturn(accessInternalClient);
        when(adminManagementClientFactory.getClient()).thenReturn(adminManagementClient);
        when(ingestInternalClientFactory.getClient()).thenReturn(ingestInternalClient);
        when(accessInternalClient.selectOperation(any(), anyBoolean(), anyBoolean())).thenReturn(
            new RequestResponseOK().addResult(ClientMockResultHelper.getLogbookResults())
        );

        when(accessInternalClient.selectOperationById(any())).thenReturn(
            new RequestResponseOK().addResult(ClientMockResultHelper.getLogbookOperation())
        );
        when(accessInternalClient.selectOperationById(any(), any(), anyBoolean(), anyBoolean())).thenReturn(
            new RequestResponseOK().addResult(ClientMockResultHelper.getLogbookOperation())
        );

        when(accessInternalClient.selectUnitLifeCycleById(any(), any())).thenReturn(
            new RequestResponseOK().addResult(ClientMockResultHelper.getLogbookOperation())
        );

        when(accessInternalClient.selectObjectGroupLifeCycleById(any(), any())).thenReturn(
            new RequestResponseOK().addResult(ClientMockResultHelper.getLogbookOperation())
        );

        // Mock AccessInternal response for download TRACEABILITY operation request
        when(accessInternalClient.downloadTraceabilityFile(TRACEABILITY_OPERATION_ID)).thenReturn(
            ClientMockResultHelper.getObjectStream()
        );
    }

    @AfterClass
    public static void tearDownAfterClass() {
        LOGGER.debug("Ending tests");
        try {
            application.stop();
        } catch (final Exception e) {
            LOGGER.error(e);
        } finally {
            junitHelper.releasePort(port);
        }
        VitamClientFactory.resetConnections();
        fr.gouv.vitam.common.external.client.VitamClientFactory.resetConnections();
    }

    @Test
    public void testSelectOperations_PreconditionFailed() throws Exception {
        given()
            .contentType(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "ABC")
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(JsonHandler.getFromString(BODY_TEST))
            .when()
            .post(OPERATIONS_URI)
            .then()
            .statusCode(Status.METHOD_NOT_ALLOWED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "ABC")
            .and()
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
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
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .pathParam("id_op", 1)
            .when()
            .post(OPERATIONS_URI + OPERATION_ID_URI)
            .then()
            .statusCode(Status.METHOD_NOT_ALLOWED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(JsonHandler.getFromString(request))
            .header(X_HTTP_METHOD_OVERRIDE, "ABC")
            .and()
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
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
    public void testSelect_NotFound() throws Exception {
        when(accessInternalClient.selectUnitLifeCycleById(any(), any())).thenThrow(
            new LogbookClientNotFoundException("")
        );
        when(accessInternalClient.selectObjectGroupLifeCycleById(any(), any())).thenThrow(
            new LogbookClientNotFoundException("")
        );
        when(accessInternalClient.selectOperationById(any())).thenThrow(new LogbookClientNotFoundException(""));
        when(accessInternalClient.selectOperationById(any(), any(), anyBoolean(), anyBoolean())).thenThrow(
            new LogbookClientNotFoundException("")
        );
        when(accessInternalClient.selectOperation(any(), anyBoolean(), anyBoolean())).thenReturn(
            new RequestResponseOK<>()
        );

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(JsonHandler.getFromString(BODY_TEST_WITH_ID))
            .when()
            .get("/logbookoperations/" + bad_id)
            .then()
            .statusCode(Status.NOT_FOUND.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .param("id_lc", bad_id)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(JsonHandler.getFromString(BODY_TEST_WITH_ID))
            .when()
            .get("/logbookunitlifecycles/" + bad_id)
            .then()
            .statusCode(Status.NOT_FOUND.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .param("id_lc", bad_id)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(JsonHandler.getFromString(BODY_TEST_WITH_ID))
            .when()
            .get("/logbookobjectgroups/" + bad_id)
            .then()
            .statusCode(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void testSelectLifecycleUnitById_PreconditionFailed() throws Exception {
        when(accessInternalClient.selectUnitLifeCycleById(bad_id, JsonHandler.getFromString(BODY_TEST))).thenThrow(
            new LogbookClientException("")
        );
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .param("id_lc", bad_id)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get("/logbookunitlifecycles/" + bad_id)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .param("id_lc", bad_id)
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when()
            .get("/logbookunitlifecycles/" + bad_id)
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .param("id_lc", bad_id)
            .when()
            .get("/logbookunitlifecycles/" + bad_id)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void testSelectLifecycleOGById_PreconditionFailed() throws Exception {
        when(accessInternalClient.selectObjectGroupLifeCycleById(bad_id, JsonHandler.getFromString(request))).thenThrow(
            new LogbookClientException("")
        );
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .param("id_lc", bad_id)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get("/logbookobjectslifecycles/" + bad_id)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .param("id_lc", bad_id)
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when()
            .get("/logbookobjectslifecycles/" + bad_id)
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .param("id_lc", bad_id)
            .when()
            .get("/logbookobjectslifecycles/" + bad_id)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void testSelectOperations_InternalServerError() throws Exception {
        when(
            accessInternalClient.selectOperation(eq(JsonHandler.getFromString(bad_request)), anyBoolean(), anyBoolean())
        ).thenThrow(new LogbookClientException(""));
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
        when(accessInternalClient.selectOperationById(bad_id)).thenThrow(new LogbookClientException(""));

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
            .then()
            .statusCode(Status.OK.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(JsonHandler.getFromString(request))
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get(OPERATIONS_URI)
            .then()
            .statusCode(Status.OK.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(JsonHandler.getFromString(request))
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when()
            .get(OPERATIONS_URI)
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(JsonHandler.getFromString(request))
            .when()
            .get(OPERATIONS_URI)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(JsonHandler.getFromString(request))
            .when()
            .get(OPERATIONS_URI)
            .then()
            .statusCode(Status.OK.getStatusCode());

        // Test DSL query Validation code
        final Response response = given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(JsonHandler.getFromString(wrong_request_format))
            .when()
            .get(OPERATIONS_URI);

        final JsonNode responseNode = JsonHandler.getFromString(response.getBody().prettyPrint());
        assertEquals(601, responseNode.get("code").asInt());
        assertEquals(400, responseNode.get("httpCode").asInt());
        assertEquals("Dsl query is not valid.", responseNode.get("message").asText());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(JsonHandler.getFromString(request))
            .when()
            .get(OPERATIONS_URI)
            .then()
            .statusCode(Status.OK.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and()
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .body(JsonHandler.getFromString(request))
            .when()
            .get(OPERATIONS_URI)
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .body(JsonHandler.getFromString(request))
            .when()
            .get(OPERATIONS_URI)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
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
            .then()
            .statusCode(Status.OK.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(JsonHandler.getFromString(request))
            .when()
            .get(OPERATIONS_URI)
            .then()
            .statusCode(Status.OK.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .body(JsonHandler.getFromString(request))
            .when()
            .get(OPERATIONS_URI)
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(JsonHandler.getFromString(request))
            .when()
            .get(OPERATIONS_URI)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and()
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .body(JsonHandler.getFromString(request))
            .when()
            .post(OPERATIONS_URI)
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .body(JsonHandler.getFromString(request))
            .when()
            .post(OPERATIONS_URI)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
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
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
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
            .then()
            .statusCode(Status.OK.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelectById())
            .pathParam("id_op", good_id)
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when()
            .get(OPERATIONS_URI + OPERATION_ID_URI)
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelectById())
            .pathParam("id_op", good_id)
            .when()
            .get(OPERATIONS_URI + OPERATION_ID_URI)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(select.getFinalSelectById())
            .pathParam("id_op", good_id)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get(OPERATIONS_URI + OPERATION_ID_URI)
            .then()
            .statusCode(Status.OK.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(select.getFinalSelectById())
            .pathParam("id_op", good_id)
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when()
            .get(OPERATIONS_URI + OPERATION_ID_URI)
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(select.getFinalSelectById())
            .pathParam("id_op", good_id)
            .when()
            .get(OPERATIONS_URI + OPERATION_ID_URI)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
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
            .then()
            .statusCode(Status.OK.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelectById())
            .pathParam("id_op", good_id)
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when()
            .get(OPERATIONS_URI + OPERATION_ID_URI)
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelectById())
            .pathParam("id_op", good_id)
            .when()
            .get(OPERATIONS_URI + OPERATION_ID_URI)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(select.getFinalSelectById())
            .pathParam("id_op", good_id)
            .when()
            .post(OPERATIONS_URI + OPERATION_ID_URI)
            .then()
            .statusCode(Status.OK.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and()
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .body(select.getFinalSelectById())
            .pathParam("id_op", good_id)
            .when()
            .post(OPERATIONS_URI + OPERATION_ID_URI)
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .body(select.getFinalSelectById())
            .pathParam("id_op", good_id)
            .when()
            .post(OPERATIONS_URI + OPERATION_ID_URI)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
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
            .get("/logbookunitlifecycles/" + good_id)
            .then()
            .statusCode(Status.OK.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(select.getFinalSelectById())
            .param("id_lc", good_id)
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when()
            .get("/logbookunitlifecycles/" + good_id)
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(select.getFinalSelectById())
            .param("id_lc", good_id)
            .when()
            .get("/logbookunitlifecycles/" + good_id)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(select.getFinalSelectById())
            .param("id_lc", good_id)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get("/logbookobjectslifecycles/" + good_id)
            .then()
            .statusCode(Status.OK.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(select.getFinalSelectById())
            .param("id_lc", good_id)
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when()
            .get("/logbookobjectslifecycles/" + good_id)
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(select.getFinalSelectById())
            .param("id_lc", good_id)
            .when()
            .get("/logbookobjectslifecycles/" + good_id)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }
}
