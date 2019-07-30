package fr.gouv.vitam.access.external.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import fr.gouv.vitam.access.external.api.AccessExtAPI;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.external.client.ClientMockResultHelper;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.elimination.EliminationRequestBody;
import fr.gouv.vitam.common.server.application.junit.ResteasyTestApplication;
import fr.gouv.vitam.common.serverv2.VitamServerTestRunner;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Set;

import static fr.gouv.vitam.common.GlobalDataRest.X_HTTP_METHOD_OVERRIDE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AccessExternalClientRestTest extends ResteasyTestApplication {
    private static final String QUERY_DSL = "{ $query : [ { $eq : { 'title' : 'test' } } ], " +
        " $filter : { $orderby : '#id' }," +
        " $projection : {$fields : {#id : 1, title:2, transacdate:1}}" +
        " }";
    protected static AccessExternalClientRest client;

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    final String queryDsql =
        "{ \"$query\" : [ { \"$eq\" : { \"title\" : \"test\" } } ], \"$projection\" : {} }";
    final String ID = "identfier1";
    final int TENANT_ID = 0;
    final String CONTRACT = "contract";

    private final static ExpectedResults mock = mock(ExpectedResults.class);

    static AccessExternalClientFactory factory = AccessExternalClientFactory.getInstance();
    public static VitamServerTestRunner
        vitamServerTestRunner =
        new VitamServerTestRunner(AccessExternalClientRestTest.class, factory);


    @BeforeClass
    public static void init() throws Throwable {
        vitamServerTestRunner.start();
        client = (AccessExternalClientRest) vitamServerTestRunner.getClient();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Throwable {
        vitamServerTestRunner.runAfter();
    }

    @Override
    public Set<Object> getResources() {
        return Sets.newHashSet(new MockResource(mock));
    }

    @Path("/access-external/v1")
    public static class MockResource {
        private final ExpectedResults expectedResponse;

        public MockResource(ExpectedResults expectedResponse) {
            this.expectedResponse = expectedResponse;
        }

        @GET
        @Path("units")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response getUnits(String queryDsl) {
            return expectedResponse.get();
        }

        @GET
        @Path("/units/{id_unit}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response getUnitById(String queryDsl,
            @PathParam("id_unit") String id_unit) {
            return expectedResponse.get();
        }

        @PUT
        @Path("/units/{id_unit}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response updateUnitById(String queryDsl, @PathParam("id_unit") String id_unit) {
            return expectedResponse.put();
        }

        @GET
        @Path("/units/{id_object_group}/objects")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        public Response getObjectStream(@Context HttpHeaders headers,
            @PathParam("id_object_group") String idObjectGroup, String query) {
            return expectedResponse.get();
        }

        @POST
        @Path("/units/{id_object_group}/objects")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        public Response getObjectStreamPost(@Context HttpHeaders headers,
            @PathParam("id_object_group") String idObjectGroup, String query) {
            return expectedResponse.post();
        }

        @GET
        @Path("/units/{id_unit}/objects")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response getObjectGroupMetadatas(@Context HttpHeaders headers,
            @PathParam("id_unit") String idUnit, String query) {
            return expectedResponse.get();
        }

        @POST
        @Path("/units/{id_unit}/objects")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response getObjectGroupMetadatasPost(@Context HttpHeaders headers,
            @PathParam("id_unit") String idUnit, String query) {
            return expectedResponse.post();
        }

        @GET
        @Path("unitsWithInheritedRules")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response selectUnitsWithInheritedRules(String queryDsl) {
            return expectedResponse.get();
        }


        // Logbook operations
        @GET
        @Path("/logbookoperations")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response selectOperation(@PathParam("id_op") String operationId) throws InvalidParseOperationException {
            return expectedResponse.get();
        }

        @POST
        @Path("/logbookoperations")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response selectOperationWithPostOverride(@PathParam("id_op") String operationId,
            @HeaderParam(X_HTTP_METHOD_OVERRIDE) String xhttpOverride)
            throws InvalidParseOperationException {
            return expectedResponse.post();
        }

        @GET
        @Path("/logbookoperations/{id_op}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response getOperation(@PathParam("id_op") String operationId) throws InvalidParseOperationException {
            return expectedResponse.get();
        }

        @POST
        @Path("/logbookoperations/{id_op}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response selectOperationByPost(@PathParam("id_op") String operationId,
            @HeaderParam(X_HTTP_METHOD_OVERRIDE) String xhttpOverride)
            throws InvalidParseOperationException {
            return expectedResponse.post();
        }

        @GET
        @Path("/logbookunitlifecycles/{id_lc}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response getUnitLifeCycle(@PathParam("id_lc") String unitLifeCycleId, JsonNode queryDsl) {
            return expectedResponse.get();
        }

        @GET
        @Path("/logbookobjectslifecycles/{id_lc}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response getObjectGroupLifeCycle(@PathParam("id_lc") String objectGroupLifeCycleId, JsonNode queryDsl) {
            return expectedResponse.get();
        }

        @POST
        @Path(AccessExtAPI.ACCESSION_REGISTERS_API)
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response findAccessionRegister(@PathParam("id_op") String operationId,
            @HeaderParam(X_HTTP_METHOD_OVERRIDE) String xhttpOverride)
            throws InvalidParseOperationException {
            return expectedResponse.post();
        }

        @POST
        @Path(AccessExtAPI.ACCESSION_REGISTERS_API + "/{id_document}/accession-register-detail")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response findAccessionRegisterDetail(@PathParam("id_op") String operationId,
            @HeaderParam(X_HTTP_METHOD_OVERRIDE) String xhttpOverride)
            throws InvalidParseOperationException {
            return expectedResponse.post();
        }

        @POST
        @Path(AccessExtAPI.TRACEABILITY_API + "/check")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response checkTraceabilityOperation(JsonNode query)
            throws InvalidParseOperationException {
            return expectedResponse.post();
        }

        @GET
        @Path(AccessExtAPI.TRACEABILITY_API + "/{idOperation}")
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        public Response downloadTraceabilityOperationFile(@PathParam("idOperation") String operationId)
            throws InvalidParseOperationException {
            return expectedResponse.get();
        }

        @POST
        @Path("/dipexport")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response exportDIP(JsonNode queryJson) {
            return expectedResponse.post();
        }

        @POST
        @Path("/elimination/analysis")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response startEliminationAnalysis(String queryDsl) {
            return expectedResponse.post();
        }

        @POST
        @Path("/elimination/action")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response startEliminationAction(String queryDsl) {
            return expectedResponse.post();
        }

        @POST
        @Path("/units/computedInheritedRules")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response computeInheritedrules(String queryDsl) {
            return expectedResponse.post();
        }
    }

    @Test
    @RunWithCustomExecutor
    public void givenRessourceOKWhenSelectTehnReturnOK()
        throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.OK).entity(ClientMockResultHelper.getFormat()).build());
        assertThat(client.selectUnits(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
            JsonHandler.getFromString(queryDsql))).isNotNull();
    }

    @Test
    @RunWithCustomExecutor
    public void givenInternalServerError_whenSelect_ThenRaiseAnExeption() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.UNAUTHORIZED).build());
        assertThat(client
            .selectUnits(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), JsonHandler.getFromString(QUERY_DSL))
            .getHttpCode())
            .isEqualTo(Status.UNAUTHORIZED.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenRessourceNotFound_whenSelectUnit_ThenRaiseAnException()
        throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        assertThat(client
            .selectUnits(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), JsonHandler.getFromString(QUERY_DSL))
            .getHttpCode())
            .isEqualTo(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenBadRequest_whenSelectUnit_ThenRaiseAnException()
        throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        assertThat(client
            .selectUnits(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), JsonHandler.getFromString(queryDsql))
            .getHttpCode())
            .isEqualTo(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @RunWithCustomExecutor
    @Test
    public void givenRequestNull_whenSelectUnit_ThenErrorResponse()
        throws Exception {
        assertThat(client.selectUnits(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), null).getHttpCode())
            .isEqualTo(Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode());
    }

    /****
     *
     * Select Unit By Id
     *
     ***/
    @Test
    @RunWithCustomExecutor
    public void givenInternalServerError_whenSelectById_ThenRaiseAnExeption() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.UNAUTHORIZED).build());
        final String queryDsql =
            "{ $query : [ { $eq : { 'title' : 'test' } } ], " +
                " $filter : { $orderby : {'#id': 1} }," +
                " $projection : {$fields : {#id : 1, title:2, transacdate:1}}" +
                " }";
        assertThat(client.selectUnitbyId(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
            createDslQueryById(queryDsql), ID).getHttpCode())
            .isEqualTo(Status.UNAUTHORIZED.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenRessourceNotFound_whenSelectUnitById_ThenRaiseAnException()
        throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        final String queryDsql =
            "{ \"$query\" : [ { \"$eq\" : { \"title\" : \"test\" } } ], " +
                " \"$filter\" : { \"$orderby\" : {\"#id\": 1}}," +
                " \"$projection\": {\"$fields\" : {\"#id\" : 1, \"title\":2, \"transacdate\":1}}" +
                " }";

        assertThat(client.selectUnitbyId(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
            createDslQueryById(queryDsql), ID).getHttpCode())
            .isEqualTo(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenBadRequest_whenSelectUnitById_ThenRaiseAnException()
        throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        assertThat(client.selectUnitbyId(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
            createDslQueryById(queryDsql), ID).getHttpCode())
            .isEqualTo(Status.BAD_REQUEST.getStatusCode());
    }

    private JsonNode createDslQueryById(String queryDsl) throws Exception {
        final SelectParserMultiple selectParserMultiple = new SelectParserMultiple();
        selectParserMultiple.parse(JsonHandler.getFromString(queryDsl));
        SelectMultiQuery selectMultiQuery = selectParserMultiple.getRequest();
        return selectMultiQuery.getFinalSelectById();
    }


    @Test(expected = IllegalArgumentException.class)
    @RunWithCustomExecutor
    public void givenRequestBlank_whenSelectUnitById_ThenRaiseAnException()
        throws Exception {
        assertThat(client.selectUnitbyId(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
            createDslQueryById(queryDsql), "")).isNotNull();
    }

    @Test(expected = IllegalArgumentException.class)
    @RunWithCustomExecutor
    public void givenIDBlank_whenSelectUnitById_ThenRaiseAnException()
        throws Exception {
        assertThat(client.selectUnitbyId(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
            createDslQueryById(queryDsql), "")).isNotNull();
    }

    @Test
    @RunWithCustomExecutor
    public void givenrEQUESTBlank_IDFilledwhenSelectUnitById_ThenVitamError()
        throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.NO_CONTENT).build());
        assertThat(client
            .selectUnitbyId(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), createDslQueryById(queryDsql), ID)
            .getHttpCode()).isEqualByComparingTo(Status.NO_CONTENT.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenBadRequest_whenUpdateUnitById_ThenRaiseAnException()
        throws Exception {
        when(mock.put()).thenReturn(Response.status(Status.NOT_FOUND).build());
        assertThat(client.updateUnitbyId(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
            JsonHandler.getFromString(queryDsql), ID).getHttpCode())
            .isEqualTo(Status.NOT_FOUND.getStatusCode());
    }


    @Test(expected = IllegalArgumentException.class)
    @RunWithCustomExecutor
    public void givenRequestBlank_whenUpdateUnitById_ThenRaiseAnException()
        throws Exception {
        assertThat(client.updateUnitbyId(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
            JsonHandler.createObjectNode(), "")).isNotNull();
    }


    @Test(expected = IllegalArgumentException.class)
    @RunWithCustomExecutor
    public void givenIdBlank_whenUpdateUnitById_ThenRaiseAnException()
        throws Exception {
        assertThat(client.updateUnitbyId(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
            JsonHandler.getFromString(queryDsql), "")).isNotNull();
    }

    @Test
    @RunWithCustomExecutor
    public void givenrEquestBlank_IDFilledwhenUpdateUnitById_ThenRaiseAnException()
        throws Exception {
        when(mock.put()).thenReturn(Response.status(Status.UNSUPPORTED_MEDIA_TYPE).build());
        assertThat(client.updateUnitbyId(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
            JsonHandler.createObjectNode(), ID).getHttpCode())
            .isEqualTo(Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenBadRequest_whenUpdateUnit_ThenRaiseAnException()
        throws Exception {
        when(mock.put()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        assertThat(client.updateUnitbyId(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
            JsonHandler.getFromString(queryDsql), ID).getHttpCode())
            .isEqualTo(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void given500_whenUpdateUnit_ThenRaiseAnException()
        throws Exception {
        when(mock.put()).thenReturn(Response.status(Status.UNAUTHORIZED).build());
        assertThat(client.updateUnitbyId(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
            JsonHandler.getFromString(queryDsql), ID).getHttpCode())
            .isEqualTo(Status.UNAUTHORIZED.getStatusCode());
    }


    @RunWithCustomExecutor
    @Test
    public void givenQueryNullWhenSelectObjectByIdThenRaiseAnInvalidParseOperationException() throws Exception {

        when(mock.get()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        assertThat(client.selectObjectMetadatasByUnitId(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
            JsonHandler.createObjectNode(), ID).getHttpCode())
            .isEqualTo(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenQueryCorrectWhenSelectObjectByIdThenRaiseInternalServerError() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.UNAUTHORIZED).build());
        assertThat(client.selectObjectMetadatasByUnitId(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
            JsonHandler.getFromString(queryDsql), ID).getHttpCode())
            .isEqualTo(Status.UNAUTHORIZED.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenQueryCorrectWhenSelectObjectByIdThenRaiseBadRequest() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        assertThat(client.selectObjectMetadatasByUnitId(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
            JsonHandler.getFromString(queryDsql), ID).getHttpCode())
            .isEqualTo(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenQueryCorrectWhenSelectObjectByIdThenRaisePreconditionFailed() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        assertThat(client.selectObjectMetadatasByUnitId(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
            JsonHandler.getFromString(queryDsql), ID).getHttpCode())
            .isEqualTo(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenQueryCorrectWhenSelectObjectByIdThenNotFound() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        assertThat(client.selectObjectMetadatasByUnitId(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
            JsonHandler.getFromString(queryDsql), ID).getHttpCode())
            .isEqualTo(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenQueryCorrectWhenSelectObjectByIdThenOK() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.OK).entity(ClientMockResultHelper.getEmptyResult()).build());
        assertThat(client.selectObjectMetadatasByUnitId(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
            JsonHandler.getFromString(queryDsql), ID)).isNotNull();
    }

    /***
     *
     * logbook operations
     *
     ***/
    // TODO migration
    @Test
    @RunWithCustomExecutor
    public void selectLogbookOperations() throws Exception {
        when(mock.post())
            .thenReturn(Response.status(Status.OK).entity(ClientMockResultHelper.getLogbookOperationsRequestResponse())
                .build());
        assertThat(client.selectOperations(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
            JsonHandler.getFromString(queryDsql))).isNotNull();
    }

    @Test
    @RunWithCustomExecutor
    public void givenSelectLogbookNotFoundThenNotFound() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        assertThat(client.selectOperations(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
            JsonHandler.getFromString(queryDsql)).getHttpCode())
            .isEqualTo(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenSelectLogbookBadQueryThenPreconditionFailed() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        assertThat(client.selectOperations(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
            JsonHandler.getFromString(queryDsql)).getHttpCode())
            .isEqualTo(Status.PRECONDITION_FAILED.getStatusCode());
    }

    /***
     *
     * logbook operationById
     *
     ***/
    @Test
    @RunWithCustomExecutor
    public void selectLogbookOperationByID() throws Exception {
        when(mock.post())
            .thenReturn(
                Response.status(Status.OK).entity(ClientMockResultHelper.getLogbookOperationRequestResponse()).build());
        assertThat(client.selectOperationbyId(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), ID,
            JsonHandler.getFromString(queryDsql))).isNotNull();
    }

    @Test
    @RunWithCustomExecutor
    public void givenSelectLogbookOperationByIDNotFoundThenNotFound() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        assertThat(client.selectOperationbyId(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), ID,
            JsonHandler.getFromString(queryDsql)).getHttpCode())
            .isEqualTo(Status.NOT_FOUND.getStatusCode());
    }


    @Test
    @RunWithCustomExecutor
    public void givenSelectLogbookOperationByIdNoQueryThen415() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.UNSUPPORTED_MEDIA_TYPE).build());
        assertThat(
            client.selectOperationbyId(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), ID, null).getHttpCode())
            .isEqualTo(Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenSelectLogbookOperationByIDBadQueryThenPreconditionFailed() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        assertThat(client.selectOperationbyId(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), ID,
            JsonHandler.getFromString(queryDsql)).getHttpCode())
            .isEqualTo(Status.PRECONDITION_FAILED.getStatusCode());
    }

    /***
     *
     * logbook lifecycle units
     *
     ***/
    @Test
    @RunWithCustomExecutor
    public void selectLogbookLifeCyclesUnitById() throws Exception {
        when(mock.get())
            .thenReturn(
                Response.status(Status.OK).entity(ClientMockResultHelper.getLogbookOperationRequestResponse()).build());
        assertThat(client.selectUnitLifeCycleById(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), ID,
            JsonHandler.getFromString(queryDsql))).isNotNull();
    }

    @Test
    @RunWithCustomExecutor
    public void givenSelectLogbookLifeCyclesUnitByIdNotFoundThenNotFound() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        assertThat(client.selectUnitLifeCycleById(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), ID,
            JsonHandler.getFromString(queryDsql)).getHttpCode())
            .isEqualTo(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenSelectLogbookLifeCyclesUnitByIdNoQueryThen415() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.UNSUPPORTED_MEDIA_TYPE).build());
        assertThat(client.selectUnitLifeCycleById(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), ID, null)
            .getHttpCode())
            .isEqualTo(Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenSelectLogbookLifeCyclesUnitByIdBadQueryThenPreconditionFailed() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        assertThat(client.selectUnitLifeCycleById(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), ID,
            JsonHandler.getFromString(queryDsql)).getHttpCode())
            .isEqualTo(Status.PRECONDITION_FAILED.getStatusCode());
    }

    /***
     *
     * logbook lifecycle object
     *
     ***/
    @Test
    @RunWithCustomExecutor
    public void selectLogbookLifeCyclesObjectById() throws Exception {
        when(mock.get())
            .thenReturn(
                Response.status(Status.OK).entity(ClientMockResultHelper.getLogbookOperationRequestResponse()).build());
        assertThat(client.selectObjectGroupLifeCycleById(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), ID,
            JsonHandler.getFromString(queryDsql))).isNotNull();
    }

    @Test
    @RunWithCustomExecutor
    public void givenSelectLogbookLifeCyclesObjectsNotFoundThenNotFound() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        assertThat(client.selectObjectGroupLifeCycleById(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), ID,
            JsonHandler.getFromString(queryDsql)).getHttpCode())
            .isEqualTo(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenSelectLogbookLifeCyclesObjectsByIdNoQueryThen415() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.UNSUPPORTED_MEDIA_TYPE).build());
        assertThat(
            client.selectObjectGroupLifeCycleById(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), ID, null)
                .getHttpCode())
            .isEqualTo(Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenSelectLogbookLifeCyclesObjectBadQueryThenPreconditionFailed() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        assertThat(client.selectObjectGroupLifeCycleById(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), ID,
            JsonHandler.getFromString(queryDsql)).getHttpCode())
            .isEqualTo(Status.PRECONDITION_FAILED.getStatusCode());
    }

    /***
     *
     * DIP export
     *
     ***/
    @Test
    @RunWithCustomExecutor
    public void exportDIP() throws Exception {
        when(mock.post())
            .thenReturn(
                Response.status(Status.OK).entity(ClientMockResultHelper.getLogbookOperationRequestResponse()).build());
        assertThat(client.exportDIP(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
            JsonHandler.getFromString(queryDsql))).isNotNull();
    }

    @Test
    @RunWithCustomExecutor
    public void givenExportDIPNotFoundThenNotFound() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.NOT_FOUND).build());
        assertThat(client
            .exportDIP(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), JsonHandler.getFromString(queryDsql))
            .getHttpCode())
            .isEqualTo(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenExportDIPNoQueryThen415() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.UNSUPPORTED_MEDIA_TYPE).build());
        assertThat(client.exportDIP(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), null).getHttpCode())
            .isEqualTo(Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenExportDIPBadQueryThenPreconditionFailed() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        assertThat(client
            .exportDIP(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), JsonHandler.getFromString(queryDsql))
            .getHttpCode())
            .isEqualTo(Status.PRECONDITION_FAILED.getStatusCode());
    }

    //@Test
    @RunWithCustomExecutor
    public void givenBadRequestWhenSelectObjectsThenRaiseAnException()
        throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        assertThat(client
            .selectObjects(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                JsonHandler.getFromString(queryDsql))
            .getHttpCode())
            .isEqualTo(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @RunWithCustomExecutor
    public void givenRequestNullWhenSelectObjectsThenErrorResponse()
        throws Exception {
        assertThat(client.selectObjects(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), null).getHttpCode())
            .isEqualTo(Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode());
    }

    /*
     * select units with inherited rules
     */

    @Test
    @RunWithCustomExecutor
    public void givenRessourceOKWhenSelectUnitsWithInheritedRulesThenReturnOK()
        throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.OK).entity(ClientMockResultHelper.getFormat()).build());
        assertThat(client.selectUnitsWithInheritedRules(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
            JsonHandler.getFromString(queryDsql))).isNotNull();
    }

    @Test
    @RunWithCustomExecutor
    public void givenInternalServerError_whenSelectUnitsWithInheritedRules_ThenRaiseAnExeption() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.UNAUTHORIZED).build());
        assertThat(client
            .selectUnitsWithInheritedRules(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                JsonHandler.getFromString(QUERY_DSL))
            .getHttpCode())
            .isEqualTo(Status.UNAUTHORIZED.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenRessourceNotFound_whenSelectUnitsWithInheritedRules_ThenRaiseAnException()
        throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        assertThat(client
            .selectUnitsWithInheritedRules(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                JsonHandler.getFromString(QUERY_DSL))
            .getHttpCode())
            .isEqualTo(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenBadRequest_whenSelectUnitsWithInheritedRules_ThenRaiseAnException()
        throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        assertThat(client
            .selectUnitsWithInheritedRules(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                JsonHandler.getFromString(QUERY_DSL))
            .getHttpCode())
            .isEqualTo(Status.PRECONDITION_FAILED.getStatusCode());
    }

    /*
     * Elimination analysis
     */

    @Test
    @RunWithCustomExecutor
    public void startEliminationAnalysisWhenSuccessThenReturnVitamResponseOK()
        throws Exception {

        // Given
        RequestResponseOK responseOK = new RequestResponseOK();
        when(mock.post()).thenReturn(Response.status(Status.OK).entity(responseOK).build());

        EliminationRequestBody eliminationRequestBody = new EliminationRequestBody(
            "2000-01-02", JsonHandler.getFromString(queryDsql));

        // When
        RequestResponse<JsonNode> requestResponse = client.startEliminationAnalysis(
            new VitamContext(TENANT_ID).setAccessContract(CONTRACT), eliminationRequestBody);

        // Then
        assertThat(requestResponse.isOk()).isTrue();
    }

    @Test
    @RunWithCustomExecutor
    public void startEliminationAnalysisWhenServerErrorThenReturnInternalServerErrorVitamResponse() throws Exception {

        // Given
        when(mock.post()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());

        EliminationRequestBody eliminationRequestBody = new EliminationRequestBody(
            "2000-01-02", JsonHandler.getFromString(queryDsql));

        // When
        RequestResponse<JsonNode> requestResponse = client.startEliminationAnalysis(
            new VitamContext(TENANT_ID).setAccessContract(CONTRACT), eliminationRequestBody);

        // Then
        assertThat(requestResponse.getHttpCode()).isEqualTo(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void startEliminationAnalysisWhenResourceNotFoundThenReturnNotFoundVitamResponse()
        throws Exception {

        // Given
        when(mock.post()).thenReturn(Response.status(Status.NOT_FOUND).build());

        EliminationRequestBody eliminationRequestBody = new EliminationRequestBody(
            "2000-01-02", JsonHandler.getFromString(queryDsql));

        // When
        RequestResponse<JsonNode> requestResponse = client.startEliminationAnalysis(
            new VitamContext(TENANT_ID).setAccessContract(CONTRACT), eliminationRequestBody);

        // Then
        assertThat(requestResponse.getHttpCode()).isEqualTo(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void startEliminationAnalysisWhenBadRequestThenReturnBadRequestVitamResponse()
        throws Exception {

        // Given
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).build());

        EliminationRequestBody eliminationRequestBody = new EliminationRequestBody(
            "2000-01-02", JsonHandler.getFromString(queryDsql));

        // When
        RequestResponse<JsonNode> requestResponse = client.startEliminationAnalysis(
            new VitamContext(TENANT_ID).setAccessContract(CONTRACT), eliminationRequestBody);

        // Then
        assertThat(requestResponse.getHttpCode()).isEqualTo(Status.BAD_REQUEST.getStatusCode());
    }

    /*
     * Elimination action
     */

    @Test
    @RunWithCustomExecutor
    public void startEliminationActionWhenSuccessThenReturnVitamResponseOK()
        throws Exception {

        // Given
        RequestResponseOK responseOK = new RequestResponseOK();
        when(mock.post()).thenReturn(Response.status(Status.OK).entity(responseOK).build());

        EliminationRequestBody eliminationRequestBody = new EliminationRequestBody(
            "2000-01-02", JsonHandler.getFromString(queryDsql));

        // When
        RequestResponse<JsonNode> requestResponse = client.startEliminationAction(
            new VitamContext(TENANT_ID).setAccessContract(CONTRACT), eliminationRequestBody);

        // Then
        assertThat(requestResponse.isOk()).isTrue();
    }

    @Test
    @RunWithCustomExecutor
    public void startEliminationActionWhenServerErrorThenReturnInternalServerErrorVitamResponse() throws Exception {

        // Given
        when(mock.post()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());

        EliminationRequestBody eliminationRequestBody = new EliminationRequestBody(
            "2000-01-02", JsonHandler.getFromString(queryDsql));

        // When
        RequestResponse<JsonNode> requestResponse = client.startEliminationAction(
            new VitamContext(TENANT_ID).setAccessContract(CONTRACT), eliminationRequestBody);

        // Then
        assertThat(requestResponse.getHttpCode()).isEqualTo(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void startEliminationActionWhenResourceNotFoundThenReturnNotFoundVitamResponse()
        throws Exception {

        // Given
        when(mock.post()).thenReturn(Response.status(Status.NOT_FOUND).build());

        EliminationRequestBody eliminationRequestBody = new EliminationRequestBody(
            "2000-01-02", JsonHandler.getFromString(queryDsql));

        // When
        RequestResponse<JsonNode> requestResponse = client.startEliminationAction(
            new VitamContext(TENANT_ID).setAccessContract(CONTRACT), eliminationRequestBody);

        // Then
        assertThat(requestResponse.getHttpCode()).isEqualTo(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void startEliminationActionWhenBadRequestThenReturnBadRequestVitamResponse()
        throws Exception {

        // Given
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).build());

        EliminationRequestBody eliminationRequestBody = new EliminationRequestBody(
            "2000-01-02", JsonHandler.getFromString(queryDsql));

        // When
        RequestResponse<JsonNode> requestResponse = client.startEliminationAction(
            new VitamContext(TENANT_ID).setAccessContract(CONTRACT), eliminationRequestBody);

        // Then
        assertThat(requestResponse.getHttpCode()).isEqualTo(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenRequestNullWhencomputedInheritedRules() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        assertThat(client
            .computedInheritedRules(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                JsonHandler.getFromString(QUERY_DSL))
            .getHttpCode())
            .isEqualTo(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenBadRequestWhencomputedInheritedRules() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        assertThat(client
            .computedInheritedRules(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                JsonHandler.getFromString(QUERY_DSL))
            .getHttpCode())
            .isEqualTo(Status.PRECONDITION_FAILED.getStatusCode());
    }
}
