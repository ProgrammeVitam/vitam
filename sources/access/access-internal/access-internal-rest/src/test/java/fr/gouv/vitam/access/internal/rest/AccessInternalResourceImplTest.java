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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import fr.gouv.culture.archivesdefrance.seda.v2.IdentifierType;
import fr.gouv.culture.archivesdefrance.seda.v2.LevelType;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalExecutionException;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.client.VitamRequestIterator;
import fr.gouv.vitam.common.database.parser.request.GlobalDatasParser;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.mapping.serializer.IdentifierTypeDeserializer;
import fr.gouv.vitam.common.mapping.serializer.LevelTypeDeserializer;
import fr.gouv.vitam.common.mapping.serializer.TextByLangDeserializer;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.unit.ArchiveUnitModel;
import fr.gouv.vitam.common.model.unit.TextByLang;
import fr.gouv.vitam.common.server.application.junit.ResteasyTestApplication;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.client.MetaDataClientMock;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.StorageClientMock;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.shiro.util.Assert;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.with;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class AccessInternalResourceImplTest extends ResteasyTestApplication {

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    // LOGGER
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AccessInternalResourceImplTest.class);

    // URI
    private static final String ACCESS_CONF = "access-test.conf";
    private static final String ACCESS_RESOURCE_URI = "access-internal/v1";
    private static final String ACCESS_UNITS_URI = "/units";
    private static final String ACCESS_UNITS_WITH_INHERITED_RULES_URI = "/unitsWithInheritedRules";
    private static final String ACCESS_UNITS_ID_URI = "/units/xyz";
    private static final String ACCESS_UPDATE_UNITS_ID_URI = "/units/xyz";
    private static final String ACCESS_ACCESS_LOG_FILE = "/storageaccesslog";

    private static AccessInternalMain application;

    // QUERIES AND DSL
    // Create a "GET" query inspired by DSL, exemple from tech design story 76
    private static final String QUERY_TEST =
        "{ \"$query\" : [ { \"$eq\": { \"title\" : \"test\" } } ], " +
            " \"$filter\": { \"$orderby\": \"#id\" }, " +
            " \"$projection\" : { \"$fields\" : { \"#id\": 1, \"title\" : 2, \"transacdate\": 1 } } " +
            " }";

    private static final String QUERY_SIMPLE_TEST = "{ \"$eq\" : { \"title\" : \"test\" } }";

    private static final String EMPTY_QUERY = "{ \"$query\" : \"\", \"$roots\" : []  }";

    private static final String DATA =
        "{ \"#id\": \"aeaqaaaaaeaaaaakaarp4akuuf2ldmyaaaaq\", " + "\"data\": \"data1\" }";

    private static final String DATA2 =
        "{ \"#id\": \"aeaqaaaaaeaaaaakaarp4akuuf2ldmyaaaab\"," + "\"data\": \"data2\" }";

    private static final String DATA_HTML =
        "{ \"#id\": \"<a href='www.culture.gouv.fr'>Culture</a>\"," + "\"data\": \"data2\" }";

    private static final String ID = "identifier4";

    private static final String BODY_TEST =
        "{\"$query\": {\"$eq\": {\"data\" : \"data2\" }}, \"$projection\": {}, \"$filter\": {}}";

    private static final String ID_UNIT = "identifier5";
    private static JunitHelper junitHelper;
    private static int port;
    private static final String OBJECT_ID = "objectId";
    private static final String OBJECTS_URI = "/objects/";

    private final static ProcessingManagementClientFactory processingManagementClientFactory =
        mock(ProcessingManagementClientFactory.class);
    private final static ProcessingManagementClient processingManagementClient = mock(ProcessingManagementClient.class);

    private final static LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory =
        mock(LogbookLifeCyclesClientFactory.class);
    private final static LogbookLifeCyclesClient logbookLifeCyclesClient = mock(LogbookLifeCyclesClient.class);

    private final static LogbookOperationsClientFactory logbookOperationsClientFactory =
        mock(LogbookOperationsClientFactory.class);
    private final static LogbookOperationsClient logbookOperationsClient = mock(LogbookOperationsClient.class);

    private final static WorkspaceClientFactory workspaceClientFactory = mock(WorkspaceClientFactory.class);
    private final static WorkspaceClient workspaceClient = mock(WorkspaceClient.class);

    private final static AdminManagementClientFactory adminManagementClientFactory =
        mock(AdminManagementClientFactory.class);
    private final static AdminManagementClient adminManagementClient = mock(AdminManagementClient.class);

    private final static StorageClientFactory storageClientFactory = mock(StorageClientFactory.class);
    private final static StorageClient storageClient = mock(StorageClient.class);

    private final static MetaDataClientFactory metaDataClientFactory = mock(MetaDataClientFactory.class);
    private final static MetaDataClient metaDataClient = mock(MetaDataClient.class);

    private final static BusinessApplication businessApplication =
        new BusinessApplication(logbookLifeCyclesClientFactory, logbookOperationsClientFactory, storageClientFactory,
            workspaceClientFactory, adminManagementClientFactory, metaDataClientFactory,
            processingManagementClientFactory);

    @Override
    public Set<Object> getResources() {
        return businessApplication.getSingletons();
    }

    @Override
    public Set<Class<?>> getClasses() {
        return businessApplication.getClasses();
    }


    @BeforeClass
    public static void setUpBeforeClass() {
        junitHelper = JunitHelper.getInstance();
        port = junitHelper.findAvailablePort();
        try {
            application = new AccessInternalMain(ACCESS_CONF, AccessInternalResourceImplTest.class, null);
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
    public static void tearDownAfterClass() {
        LOGGER.debug("Ending tests");
        try {
            application.stop();
            junitHelper.releasePort(port);
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
        }
        VitamClientFactory.resetConnections();
    }

    @Before
    public void setUp() {
        reset(logbookOperationsClient);
        reset(workspaceClient);
        reset(processingManagementClient);
        reset(metaDataClient);
        reset(storageClient);
        reset(adminManagementClient);
        reset(logbookLifeCyclesClient);


        when(logbookOperationsClientFactory.getClient()).thenReturn(logbookOperationsClient);
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);
        when(processingManagementClientFactory.getClient()).thenReturn(processingManagementClient);
        when(metaDataClientFactory.getClient()).thenReturn(metaDataClient);
        when(storageClientFactory.getClient()).thenReturn(storageClient);
        when(adminManagementClientFactory.getClient()).thenReturn(adminManagementClient);
        when(logbookLifeCyclesClientFactory.getClient()).thenReturn(logbookLifeCyclesClient);
    }

    // Error cases

    /**
     * Test if the update is in error the 500
     *
     * @throws Exception
     */
    @Test
    public void givenStartedServer_WhenUpdateUnitError_ThenReturnError() throws Exception {
        when(metaDataClient.updateUnitbyId(any(), any()))
            .thenThrow(new MetaDataExecutionException("Wanted exception"));

        given().contentType(ContentType.JSON).body(buildDSLWithOptions(QUERY_SIMPLE_TEST, DATA))
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, "all")
            .when().put("/units/" + ID).then().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());

    }

    /**
     * Checks if the send parameter doesn't have Json format
     *
     * @throws Exception
     */
    @Test
    public void givenStartedServer_WhenRequestNotJson_ThenReturnError_UnsupportedMediaType() throws Exception {
        given()
            .contentType(ContentType.XML)
            .body(buildDSLWithOptions(QUERY_TEST, DATA2).asText())
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, "AccessContract")
            .when().get(ACCESS_UNITS_URI).then().statusCode(Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode());
    }

    /**
     * Checks if the send parameter is a bad request
     *
     * @throws Exception
     */
    @Test(expected = InvalidParseOperationException.class)
    public void givenStartedServer_WhenBadRequest_ThenReturnError_BadRequest() throws Exception {
        given()
            .contentType(ContentType.JSON)
            .body(buildDSLWithOptions(QUERY_TEST, "test")).when()
            .get(ACCESS_UNITS_URI).then()
            .statusCode(Status.OK.getStatusCode());
    }

    @Test
    public void givenStartedServer_WhenJsonContainsHtml_ThenReturnError_BadRequest() throws Exception {
        given()
            .contentType(ContentType.JSON)
            .body(buildDSLWithRoots(DATA_HTML)).when()
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, "all")
            .get(ACCESS_UNITS_URI).then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void givenStartedServer_WhenEmptyQuery_ThenReturnError_Forbidden() throws Exception {
        given()
            .contentType(ContentType.JSON)
            .body(JsonHandler.getFromString(EMPTY_QUERY)).when()
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, "all")
            .get(ACCESS_UNITS_URI).then()
            .statusCode(Status.FORBIDDEN.getStatusCode());
    }

    @Test
    public void testMissingAccessContract() throws Exception {
        given()
            .contentType(ContentType.JSON)
            .body(JsonHandler.getFromString(EMPTY_QUERY)).when()
            .get(ACCESS_UNITS_URI).then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());
    }

    /**
     * @param data
     * @return query DSL with Options
     * @throws InvalidParseOperationException
     */
    private static final JsonNode buildDSLWithOptions(String query, String data) throws InvalidParseOperationException {
        return JsonHandler
            .getFromString("{ \"$roots\" : [], \"$query\" : [ " + query + " ], \"$data\" : " + data + " }");
    }

    /**
     * @param data
     * @return query DSL with id as Roots
     * @throws InvalidParseOperationException
     */
    private static final JsonNode buildDSLWithRoots(String data) throws InvalidParseOperationException {
        return JsonHandler
            .getFromString("{ \"$roots\" : [ " + data + " ], \"$query\" : [ \"\" ], \"$data\" : " + data + " }");
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
            .contentType(ContentType.XML)
            .body(buildDSLWithRoots("\"" + ID + "\"").asText())
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, "AccessContract")
            .when().get(ACCESS_UNITS_ID_URI).then().statusCode(Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode());
    }

    /**
     * Checks if the send parameter is a bad request
     *
     * @throws Exception
     */
    @Test(expected = InvalidParseOperationException.class)
    public void givenStartedServer_WhenBadRequest_ThenReturnError_SelectById_BadRequest() throws Exception {
        given()
            .contentType(ContentType.JSON)
            .body(buildDSLWithRoots(ID))
            .when().get(ACCESS_UNITS_ID_URI).then().statusCode(Status.BAD_REQUEST.getStatusCode());
    }


    @Test(expected = InvalidParseOperationException.class)
    public void given_SelectUnitById_WhenStringTooLong_Then_RaiseException() throws Exception {
        final int oldValue = GlobalDatasParser.limitRequest;
        try {
            GlobalDatasParser.limitRequest = 1000;
            given()
                .contentType(ContentType.JSON)
                .body(buildDSLWithOptions(createLongString(1001), DATA2))
                .when().get(ACCESS_UNITS_ID_URI).then().statusCode(Status.METHOD_NOT_ALLOWED.getStatusCode());
        } finally {
            GlobalDatasParser.limitRequest = oldValue;
        }

    }

    @Test(expected = InvalidParseOperationException.class)
    public void given_updateUnitById_WhenStringTooLong_Then_RaiseException() throws Exception {
        final int oldValue = GlobalDatasParser.limitRequest;
        try {
            GlobalDatasParser.limitRequest = 1000;
            given()
                .contentType(ContentType.JSON).body(buildDSLWithOptions(createLongString(1001), DATA2))
                .when().put(ACCESS_UPDATE_UNITS_ID_URI).then().statusCode(Status.BAD_REQUEST.getStatusCode());
        } finally {
            GlobalDatasParser.limitRequest = oldValue;
        }
    }



    private static String createLongString(int size) throws Exception {
        final StringBuilder sb = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            sb.append('a');
        }
        return sb.toString();
    }

    @Test
    @RunWithCustomExecutor
    public void given_getUnits_and_getUnitByID_thenReturn_OK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        AccessContractModel contract = new AccessContractModel();
        Set<String> prodServices = new HashSet<>(Arrays.asList("a", "b"));
        contract.setOriginatingAgencies(prodServices);
        VitamThreadUtils.getVitamSession().setContract(contract);
        with()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, "all")
            .body(BODY_TEST).when()
            .get("/units").then()
            .statusCode(Status.OK.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, "all")
            .body(BODY_TEST).when()
            .get("/units/" + ID_UNIT).then()
            .statusCode(Status.OK.getStatusCode());

    }


    @Test
    public void given_emptyQuery_when_SelectByID_thenReturn_Bad_Request() {
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, "all")
            .body("")
            .when()
            .get("/units/" + ID_UNIT)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void given_emptyQuery_when_UpdateByID_thenReturn_Bad_Request() {
        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, "all")
            .header(GlobalDataRest.X_REQUEST_ID, "aeaqaaaaaaag3r7kjkkkkkmfjfikiaaaaq").body("")
            .when()
            .put("/units/" + ID_UNIT)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void given_queryThatThrowException_when_updateByID()
        throws InvalidParseOperationException, LogbookClientBadRequestException, LogbookClientAlreadyExistsException,
        LogbookClientServerException {
        doThrow(new LogbookClientServerException("Error")).when(logbookOperationsClient).create(any());
        given()
            .contentType(ContentType.JSON)
            .body(buildDSLWithOptions(QUERY_SIMPLE_TEST, DATA))
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, "all")
            .header(GlobalDataRest.X_REQUEST_ID, GUIDFactory.newGUID().getId())
            .when()
            .put("/units/" + GUIDFactory.newGUID().getId())
            .then()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void given_pathWithId_when_get_SelectByID() {
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, "all")
            .body(BODY_TEST)
            .when()
            .get("/units/" + ID_UNIT)
            .then()
            .statusCode(Status.OK.getStatusCode());
    }

    @Test
    public void shouldReturnBadRequestError() throws Exception {
        final int limitRequest = GlobalDatasParser.limitRequest;
        GlobalDatasParser.limitRequest = 99;
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, "all")
            .body(buildDSLWithOptions("", createJsonStringWithDepth(101))).when()
            .get("/units/" + ID_UNIT).then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
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
    @RunWithCustomExecutor
    public void getAccessLogFilesBadRequest() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM)
            .header(GlobalDataRest.X_TENANT_ID, "0")
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, "all")
            .body(JsonHandler.createObjectNode().put("StartDate", "M108-20-12"))
            .when().get(ACCESS_ACCESS_LOG_FILE).then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void getAccessLogFilesInternalError() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);

        doThrow(new StorageServerClientException("Internal Error")).when(storageClient)
            .listContainer(any(), any());

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM)
            .header(GlobalDataRest.X_TENANT_ID, "0").body("{}")
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, "all")
            .when().get(ACCESS_ACCESS_LOG_FILE).then()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void getAccessLogFilesNotFound() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);

        doThrow(new StorageNotFoundException("Storage Not Found")).when(storageClient)
            .getContainerAsync(any(), anyString(), any(), any());
        VitamRequestIterator<JsonNode> vitamRequestIterator = mock(VitamRequestIterator.class);
        when(vitamRequestIterator.hasNext()).thenReturn(true);
        when(vitamRequestIterator.next()).thenReturn(JsonHandler.createObjectNode().put("objectId", "guid"));

        when(storageClient.listContainer(anyString(), any())).thenReturn(vitamRequestIterator);

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM)
            .header(GlobalDataRest.X_TENANT_ID, "0")
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, "all")
            .body(JsonHandler.createObjectNode())
            .when().get(ACCESS_ACCESS_LOG_FILE).then()
            .statusCode(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void getAccessLogFilesPreconditionFailed() {
        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM)
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, "all")
            .get(ACCESS_ACCESS_LOG_FILE).then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void getObjectGroupOk() throws Exception {
        when(metaDataClient.selectObjectGrouptbyId(any(), eq(OBJECT_ID))).thenReturn(JsonHandler
            .getFromString(DATA));

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, "all")
            .header(GlobalDataRest.X_TENANT_ID, 0)
            .body(BODY_TEST).when()
            .get(OBJECTS_URI +
                OBJECT_ID)
            .then()
            .statusCode(Status.OK.getStatusCode()).contentType(MediaType.APPLICATION_JSON);
    }

    @Test
    public void getObjectGroupPreconditionFailed() {
        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, "all")
            .when().get(OBJECTS_URI +
            OBJECT_ID)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void getObjectGroupBadRequest() {
        // since resteasy rejects the query because it's not a proper json format, 412 is thrown now
        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, "all")
            .body("").when()
            .get(OBJECTS_URI + OBJECT_ID).then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void getObjectGroupNotFound() throws Exception {
        when(metaDataClient.selectObjectGrouptbyId(any(), eq(OBJECT_ID)))
            .thenThrow(new NotFoundException());

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, "all")
            .body(BODY_TEST).when()
            .get(OBJECTS_URI + OBJECT_ID).then()
            .statusCode(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void getObjectGroupInternalServerError() throws Exception {
        when(metaDataClient.selectObjectGrouptbyId(any(), eq(OBJECT_ID)))
            .thenThrow(new MetaDataClientServerException("Wanted exception"));

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, "all")
            .body(BODY_TEST).when()
            .get(OBJECTS_URI + OBJECT_ID).then()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void getObjectStreamPreconditionFailed() {
        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM)
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, "all")
            .header(GlobalDataRest.X_QUALIFIER, "qualif").header(GlobalDataRest.X_VERSION, 1).when()
            .get(OBJECTS_URI + OBJECT_ID + "/unitID").then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM)
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, "all")
            .header(GlobalDataRest.X_QUALIFIER, "qualif").header(GlobalDataRest.X_TENANT_ID, "0").when()
            .get(OBJECTS_URI + OBJECT_ID + "/unitID").then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM)
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, "all")
            .header(GlobalDataRest.X_TENANT_ID, "0").header(GlobalDataRest.X_VERSION, 1).when()
            .get(OBJECTS_URI + OBJECT_ID + "/unitID").then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM)
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, "all")
            .when().get(OBJECTS_URI + OBJECT_ID + "/unitID").then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void getObjectStreamPostMethodNotAllowed() throws InvalidParseOperationException {
        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM)
            .header(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, "TEST").when()
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, "AccessContract")
            .post(OBJECTS_URI + OBJECT_ID)
            .then().statusCode(Status.METHOD_NOT_ALLOWED.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void getObjectStreamNotFound() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        when(metaDataClient.selectObjectGrouptbyId(any(), anyString()))
            .thenReturn(new MetaDataClientMock().selectObjectGrouptbyId(JsonHandler.createObjectNode(), "id"));

        when(storageClient.getContainerAsync(anyString(), eq(null), any(), any()))
            .thenThrow(new StorageNotFoundException("test"));

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM)
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, "all")
            .header(GlobalDataRest.X_QUALIFIER, "BinaryMaster_1")
            .headers(getStreamHeaders()).when().get(OBJECTS_URI + OBJECT_ID + "/unitID").then()
            .statusCode(Status.NOT_FOUND.getStatusCode());

        doThrow(new MetaDataNotFoundException("test")).when(metaDataClient)
            .selectObjectGrouptbyId(any(), anyString());

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM)
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, "all")
            .header(GlobalDataRest.X_QUALIFIER, "BinaryMaster_1")
            .headers(getStreamHeaders()).when().get(OBJECTS_URI + OBJECT_ID + "/unitID").then()
            .statusCode(Status.NOT_FOUND.getStatusCode());

    }

    @Test
    @RunWithCustomExecutor
    public void getObjectStreamInternalServerError() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        doThrow(new MetaDataExecutionException("Wanted exception")).when(metaDataClient)
            .selectObjectGrouptbyId(any(), anyString());

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM)
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, "all")
            .header(GlobalDataRest.X_QUALIFIER, "BinaryMaster_1")
            .headers(getStreamHeaders())
            .when().get(OBJECTS_URI + OBJECT_ID + "/unitID").then()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    private Map<String, Object> getStreamHeaders() {
        final Map<String, Object> headers = new HashMap<>();
        headers.put(GlobalDataRest.X_TENANT_ID, "0");
        headers.put(GlobalDataRest.X_QUALIFIER, "qualif");
        headers.put(GlobalDataRest.X_VERSION, 1);
        return headers;
    }

    @Test
    @RunWithCustomExecutor
    public void testGetObjectStreamUnauthorized() throws Exception {

        doAnswer(invocation -> {
            return null;
        }).when(metaDataClient)
            .selectObjectGrouptbyId(any(), anyString());

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM)
            .headers(getStreamHeaders())
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, "all")
            .when().get(OBJECTS_URI + OBJECT_ID + "/unitID").then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void should_retrieve_unit_with_xml_format() throws Exception {
        RequestResponseOK<JsonNode> requestResponse = new RequestResponseOK<>();
        InputStream resourceAsStream = getClass().getResourceAsStream("/simpleUnit.json");
        JsonNode jsonNode = JsonHandler.getFromInputStream(resourceAsStream);
        requestResponse.addResult(jsonNode);

        when(metaDataClient.selectUnitbyId(any(), anyString()))
            .thenReturn(JsonHandler.toJsonNode(requestResponse));

        ArchiveUnitModel archiveUnitModel = buildObjectMapper().treeToValue(jsonNode, ArchiveUnitModel.class);

        Assert.notNull(archiveUnitModel);
        Assert.notNull(archiveUnitModel.getManagement());

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_XML)
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, "all")
            .headers(getStreamHeaders())
            .body(BODY_TEST).when().get(ACCESS_UNITS_URI + "/1234").then()
            .statusCode(Status.OK.getStatusCode());
    }

    private ObjectMapper buildObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.UPPER_CAMEL_CASE);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        SimpleModule module = new SimpleModule();

        module.addDeserializer(TextByLang.class, new TextByLangDeserializer());
        module.addDeserializer(LevelType.class, new LevelTypeDeserializer());
        module.addDeserializer(IdentifierType.class, new IdentifierTypeDeserializer());

        objectMapper.registerModule(module);

        return objectMapper;
    }

    /**
     * Checks if the send parameter doesn't have Json format
     *
     * @throws Exception
     */
    @Test
    public void givenStartedServer_WhenSelectUnitsWithInheritedRulesWithNotJsonRequest_ThenReturnError_UnsupportedMediaType()
        throws Exception {
        given()
            .contentType(ContentType.XML)
            .body(buildDSLWithOptions(QUERY_TEST, DATA2).asText())
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, "AccessContract")
            .when().get(ACCESS_UNITS_WITH_INHERITED_RULES_URI).then()
            .statusCode(Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode());
    }

    @Test
    public void givenStartedServer_WhenSelectUnitsWithInheritedRulesWithJsonContainsHtml_ThenReturnError_BadRequest()
        throws Exception {
        given()
            .contentType(ContentType.JSON)
            .body(buildDSLWithRoots(DATA_HTML)).when()
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, "all")
            .get(ACCESS_UNITS_WITH_INHERITED_RULES_URI).then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void givenStartedServer_WhenSelectUnitsWithInheritedRulesWithEmptyQuery_ThenReturnError_Forbidden()
        throws Exception {
        given()
            .contentType(ContentType.JSON)
            .body(JsonHandler.getFromString(EMPTY_QUERY)).when()
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, "all")
            .get(ACCESS_UNITS_WITH_INHERITED_RULES_URI).then()
            .statusCode(Status.FORBIDDEN.getStatusCode());
    }
}

