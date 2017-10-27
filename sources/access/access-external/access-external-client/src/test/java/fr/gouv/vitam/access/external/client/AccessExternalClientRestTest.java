package fr.gouv.vitam.access.external.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

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

import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Rule;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.access.external.api.AccessExtAPI;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientNotFoundException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientServerException;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.external.client.ClientMockResultHelper;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.server.application.AbstractVitamApplication;
import fr.gouv.vitam.common.server.application.configuration.DefaultVitamApplicationConfiguration;
import fr.gouv.vitam.common.server.application.junit.VitamJerseyTest;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;

public class AccessExternalClientRestTest extends VitamJerseyTest {
    protected static final String HOSTNAME = "localhost";
    protected static final String PATH = "/access-external/v1";
    protected AccessExternalClientRest client;

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    final String queryDsql =
        "{ \"$query\" : [ { \"$eq\" : { \"title\" : \"test\" } } ] }";
    final String MOCK_LOGBOOK_RESULT =
        "{\"_id\": \"aedqaaaaacaam7mxaaaamakvhiv4rsiaaaaq\"," +
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
    final String BODY_WITH_ID =
        "{\"$query\": {\"$eq\": {\"obId\": \"aedqaaaaacaam7mxaaaamakvhiv4rsiaaaaq\" }}, \"$projection\": {}, \"$filter\": {}}";
    final String ID = "identfier1";
    final String USAGE = "BinaryMaster";
    final int VERSION = 1;
    final int TENANT_ID = 0;
    final String CONTRACT = "contract";

    public AccessExternalClientRestTest() {
        super(AccessExternalClientFactory.getInstance());
    }

    @Override
    public void beforeTest() throws VitamApplicationServerException {
        client = (AccessExternalClientRest) getClient();
    }

    // Define the getApplication to return your Application using the correct Configuration
    @Override
    public StartApplicationResponse<AbstractApplication> startVitamApplication(int reservedPort) {
        final TestVitamApplicationConfiguration configuration = new TestVitamApplicationConfiguration();
        configuration.setJettyConfig(DEFAULT_XML_CONFIGURATION_FILE);
        final AbstractApplication application = new AbstractApplication(configuration);
        try {
            application.start();
        } catch (final VitamApplicationServerException e) {
            throw new IllegalStateException("Cannot start the application", e);
        }
        return new StartApplicationResponse<AbstractApplication>()
            .setServerPort(application.getVitamServer().getPort())
            .setApplication(application);
    }

    // Define your Application class if necessary
    public final class AbstractApplication
        extends AbstractVitamApplication<AbstractApplication, TestVitamApplicationConfiguration> {
        protected AbstractApplication(TestVitamApplicationConfiguration configuration) {
            super(TestVitamApplicationConfiguration.class, configuration);
        }

        @Override
        protected void registerInResourceConfig(ResourceConfig resourceConfig) {
            resourceConfig.registerInstances(new MockResource(mock));
        }

        @Override
        protected boolean registerInAdminConfig(ResourceConfig resourceConfig) {
            // do nothing as @admin is not tested here
            return false;
        }
    }


    // Define your Configuration class if necessary
    public static class TestVitamApplicationConfiguration extends DefaultVitamApplicationConfiguration {
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
        @Path("/objects/{id_object_group}")
        @Consumes(MediaType.APPLICATION_JSON)
        // @Produces(MediaType.APPLICATION_OCTET_STREAM)
        public Response getUnitObject(@PathParam("id_object_group") String idObjectGroup, String query) {
            return expectedResponse.get();
        }


        @POST
        @Path("/objects/{id_object_group}")
        @Consumes(MediaType.APPLICATION_JSON)
        // @Produces(MediaType.APPLICATION_OCTET_STREAM)
        public Response getUnitObject(@HeaderParam(GlobalDataRest.X_HTTP_METHOD_OVERRIDE) String xHttpOverride,
            @PathParam("id_object_group") String idObjectGroup, String query) {
            return expectedResponse.post();
        }


        @GET
        @Path("/objects/{id_object_group}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        public Response getObjectGroup(@PathParam("id_object_group") String idObjectGroup, String query) {
            return expectedResponse.get();
        }


        @POST
        @Path("/objects/{id_object_group}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        public Response getObjectGroup(@HeaderParam(GlobalDataRest.X_HTTP_METHOD_OVERRIDE) String xHttpOverride,
            @PathParam("id_object_group") String idObjectGroup, String query) {
            return expectedResponse.post();
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


        // Logbook operations
        @GET
        @Path("/operations")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response selectOperation(@PathParam("id_op") String operationId) throws InvalidParseOperationException {
            return expectedResponse.get();
        }

        @POST
        @Path("/operations")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response selectOperationWithPostOverride(@PathParam("id_op") String operationId,
            @HeaderParam("X-HTTP-Method-Override") String xhttpOverride)
            throws InvalidParseOperationException {
            return expectedResponse.post();
        }

        @GET
        @Path("/operations/{id_op}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response getOperation(@PathParam("id_op") String operationId) throws InvalidParseOperationException {
            return expectedResponse.get();
        }

        @POST
        @Path("/operations/{id_op}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response selectOperationByPost(@PathParam("id_op") String operationId,
            @HeaderParam("X-HTTP-Method-Override") String xhttpOverride)
            throws InvalidParseOperationException {
            return expectedResponse.post();
        }

        @GET
        @Path("/unitlifecycles/{id_lc}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response getUnitLifeCycle(@PathParam("id_lc") String unitLifeCycleId, JsonNode queryDsl) {
            return expectedResponse.get();
        }

        @GET
        @Path("/objectgrouplifecycles/{id_lc}")
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
            @HeaderParam("X-HTTP-Method-Override") String xhttpOverride)
            throws InvalidParseOperationException {
            return expectedResponse.post();
        }

        @POST
        @Path(AccessExtAPI.ACCESSION_REGISTERS_API + "/{id_document}/accession-register-detail")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response findAccessionRegisterDetail(@PathParam("id_op") String operationId,
            @HeaderParam("X-HTTP-Method-Override") String xhttpOverride)
            throws InvalidParseOperationException {
            return expectedResponse.post();
        }


        // Functionalities related to TRACEABILITY operation

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
        final String queryDsql =
            "{ $query : [ { $eq : { 'title' : 'test' } } ], " +
                " $filter : { $orderby : '#id' }," +
                " $projection : {$fields : {#id : 1, title:2, transacdate:1}}" +
                " }";
        assertThat(client
            .selectUnits(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), JsonHandler.getFromString(queryDsql))
            .getHttpCode())
            .isEqualTo(Status.UNAUTHORIZED.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenRessourceNotFound_whenSelectUnit_ThenRaiseAnException()
        throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        final String queryDsql =
            "{ $query : [ { $eq : { 'title' : 'test' } } ], " +
                " $filter : { $orderby : '#id' }," +
                " $projection : {$fields : {#id : 1, title:2, transacdate:1}}" +
                " }";
        assertThat(client
            .selectUnits(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), JsonHandler.getFromString(queryDsql))
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

    @Test(expected = IllegalArgumentException.class)
    @RunWithCustomExecutor
    public void givenQueryNullWhenSelectObjectByIdThenRaiseAnInvalidParseOperationException() throws Exception {
        client.selectObjectMetadatasByUnitId(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), null, ID);
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

    @Test(expected = IllegalArgumentException.class)
    @RunWithCustomExecutor
    public void givenQueryNullWhenGetObjectAsInputStreamThenRaiseAnInvalidParseOperationException() throws Exception {
        client.getObject(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), null, ID, USAGE, VERSION);
    }

    @Test(expected = AccessExternalClientServerException.class)
    @RunWithCustomExecutor
    public void givenQueryCorrectWhenGetObjectAsInputStreamThenRaiseInternalServerError() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        client.getObject(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), JsonHandler.getFromString(queryDsql),
            ID, USAGE, VERSION);
    }

    @Test(expected = InvalidParseOperationException.class)
    @RunWithCustomExecutor
    public void givenQueryCorrectWhenGetObjectAsInputStreamThenRaiseBadRequest() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        client.getObject(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), JsonHandler.getFromString(queryDsql),
            ID, USAGE, VERSION);
    }

    @Test(expected = AccessExternalClientServerException.class)
    @RunWithCustomExecutor
    public void givenQueryCorrectWhenGetObjectAsInputStreamThenRaisePreconditionFailed() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        client.getObject(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), JsonHandler.getFromString(queryDsql),
            ID, USAGE, VERSION);
    }

    @Test(expected = AccessExternalClientNotFoundException.class)
    @RunWithCustomExecutor
    public void givenQueryCorrectWhenGetObjectAsInputStreamThenNotFound() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.NOT_FOUND).build());
        client.getObject(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), JsonHandler.getFromString(queryDsql),
            ID, USAGE, VERSION);
    }

    @Test
    @RunWithCustomExecutor
    public void givenQueryCorrectWhenGetObjectAsInputStreamThenOK() throws Exception {
        when(mock.post())
            .thenReturn(Response.status(Status.OK).entity(StreamUtils.toInputStream("Vitam test")).build());
        final Response response =
            client.getObject(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                JsonHandler.getFromString(queryDsql), ID, USAGE, VERSION);
        assertNotNull(response);
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
        assertThat(client.selectOperation(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
            JsonHandler.getFromString(queryDsql))).isNotNull();
    }

    @Test
    @RunWithCustomExecutor
    public void givenSelectLogbookNotFoundThenNotFound() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        assertThat(client.selectOperation(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
            JsonHandler.getFromString(queryDsql)).getHttpCode())
            .isEqualTo(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenSelectLogbookBadQueryThenPreconditionFailed() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        assertThat(client.selectOperation(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
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

}
