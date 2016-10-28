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

import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.access.external.common.exception.AccessExternalClientNotFoundException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientServerException;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.server.application.junit.VitamJerseyTest;
import fr.gouv.vitam.common.server2.application.AbstractVitamApplication;
import fr.gouv.vitam.common.server2.application.configuration.DefaultVitamApplicationConfiguration;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;

public class AccessExternalClientRestTest extends VitamJerseyTest {
    protected static final String HOSTNAME = "localhost";
    protected static final int PORT = 8082;
    protected static final String PATH = "/access-external/v1";
    protected AccessExternalClientRest client;

    final String queryDsql =
        "{ $query : [ { $eq : { 'title' : 'test' } } ], " +
            " $filter : { $orderby : { '#id' } }," +
            " $projection : {$fields : {#id : 1, title:2, transacdate:1}}" +
            " }";
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
        "    \"agIdAppSession\": null," +
        "    \"evIdReq\": \"aedqaaaaacaam7mxaaaamakvhiv4rsiaaaaq\"," +
        "    \"agIdSubm\": null," +
        "    \"agIdOrig\": null," +
        "    \"obId\": null," +
        "    \"obIdReq\": null," +
        "    \"obIdIn\": null," +
        "    \"events\": []}";    
    final String ID = "identfier1";
    final String USAGE = "BinaryMaster";
    final int VERSION = 1;


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

        @POST
        @Path("units")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response getUnits(String queryDsl,
            @HeaderParam(GlobalDataRest.X_HTTP_METHOD_OVERRIDE) String xhttpOverride) {
            return expectedResponse.post();
        }

        @POST
        @Path("/units/{id_unit}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response getUnitById(String queryDsl,
            @HeaderParam(GlobalDataRest.X_HTTP_METHOD_OVERRIDE) String xhttpOverride,
            @PathParam("id_unit") String id_unit) {
            return expectedResponse.post();
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
        //@Produces(MediaType.APPLICATION_OCTET_STREAM)
        public Response getObjectGroup(@PathParam("id_object_group") String idObjectGroup, String query) {
            return expectedResponse.get();
        }


        @POST
        @Path("/objects/{id_object_group}")
        @Consumes(MediaType.APPLICATION_JSON)
        //@Produces(MediaType.APPLICATION_OCTET_STREAM)
        public Response getObjectGroup(@HeaderParam(GlobalDataRest.X_HTTP_METHOD_OVERRIDE) String xHttpOverride,
            @PathParam("id_object_group") String idObjectGroup, String query) {
            return expectedResponse.post();
        }

        @GET
        @Path("/units/{id_object_group}/object")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        public Response getObjectStream(@Context HttpHeaders headers,
            @PathParam("id_object_group") String idObjectGroup, String query) {
            return expectedResponse.get();
        }

        @POST
        @Path("/units/{id_object_group}/object")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        public Response getObjectStreamPost(@Context HttpHeaders headers,
            @PathParam("id_object_group") String idObjectGroup, String query) {
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
        
        // Logbook lifecycle
        @GET
        @Path("/unitlifecycles/{id_lc}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response getUnitLifeCycle(@PathParam("id_lc") String unitLifeCycleId) {
            return expectedResponse.get();
        }

        @GET
        @Path("/objectgrouplifecycles/{id_lc}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response getObjectGroupLifeCycle(@PathParam("id_lc") String objectGroupLifeCycleId) {
            return expectedResponse.get();
        }

    }

    @Test
    public void givenRessourceOKWhenSelectTehnReturnOK()
        throws AccessExternalClientServerException, AccessExternalClientNotFoundException, InvalidParseOperationException {
        when(mock.post()).thenReturn(Response.status(Status.OK).entity("{ \"hint\": {\"total\":\"1\"} }").build());
        assertThat(client.selectUnits(queryDsql)).isNotNull();
    }

    @Test(expected = AccessExternalClientServerException.class)
    public void givenInternalServerError_whenSelect_ThenRaiseAnExeption() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.UNAUTHORIZED).build());
        final String queryDsql =
            "{ $query : [ { $eq : { 'title' : 'test' } } ], " +
                " $filter : { $orderby : { '#id' } }," +
                " $projection : {$fields : {#id : 1, title:2, transacdate:1}}" +
                " }";

        assertThat(client.selectUnits(queryDsql)).isNotNull();
    }

    @Test(expected = AccessExternalClientNotFoundException.class)
    public void givenRessourceNotFound_whenSelectUnit_ThenRaiseAnException()
        throws AccessExternalClientNotFoundException, AccessExternalClientServerException, InvalidParseOperationException {
        when(mock.post()).thenReturn(Response.status(Status.NOT_FOUND).build());
        final String queryDsql =
            "{ $query : [ { $eq : { 'title' : 'test' } } ], " +
                " $filter : { $orderby : { '#id' } }," +
                " $projection : {$fields : {#id : 1, title:2, transacdate:1}}" +
                " }";

        assertThat(client.selectUnits(queryDsql)).isNotNull();
    }

    @Test(expected = InvalidParseOperationException.class)
    public void givenBadRequest_whenSelectUnit_ThenRaiseAnException()
        throws InvalidParseOperationException, AccessExternalClientServerException, AccessExternalClientNotFoundException {
        when(mock.post()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        assertThat(client.selectUnits(queryDsql)).isNotNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenRequestBlank_whenSelectUnit_ThenRaiseAnException()
        throws IllegalArgumentException, AccessExternalClientServerException, AccessExternalClientNotFoundException,
        InvalidParseOperationException {
        assertThat(client.selectUnits("")).isNotNull();
    }
    
/****
 * 
 Select Unit By Id
 
***/
    @Test(expected = AccessExternalClientServerException.class)
    public void givenInternalServerError_whenSelectById_ThenRaiseAnExeption() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.UNAUTHORIZED).build());
        final String queryDsql =
            "{ $query : [ { $eq : { 'title' : 'test' } } ], " +
                " $filter : { $orderby : { '#id' } }," +
                " $projection : {$fields : {#id : 1, title:2, transacdate:1}}" +
                " }";

        assertThat(client.selectUnitbyId(queryDsql, ID)).isNotNull();
    }

    @Test(expected = AccessExternalClientNotFoundException.class)
    public void givenRessourceNotFound_whenSelectUnitById_ThenRaiseAnException()
        throws AccessExternalClientNotFoundException, AccessExternalClientServerException, InvalidParseOperationException {
        when(mock.post()).thenReturn(Response.status(Status.NOT_FOUND).build());
        final String queryDsql =
            "{ $query : [ { $eq : { 'title' : 'test' } } ], " +
                " $filter : { $orderby : { '#id' } }," +
                " $projection : {$fields : {#id : 1, title:2, transacdate:1}}" +
                " }";

        assertThat(client.selectUnitbyId(queryDsql, ID)).isNotNull();
    }

    @Test(expected = InvalidParseOperationException.class)
    public void givenBadRequest_whenSelectUnitById_ThenRaiseAnException()
        throws InvalidParseOperationException, AccessExternalClientServerException, AccessExternalClientNotFoundException {
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        client.selectUnitbyId(queryDsql, ID);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenRequestBlank_whenSelectUnitById_ThenRaiseAnException()
        throws IllegalArgumentException, AccessExternalClientServerException, AccessExternalClientNotFoundException,
        InvalidParseOperationException {
        assertThat(client.selectUnitbyId("", "")).isNotNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenIDBlank_whenSelectUnitById_ThenRaiseAnException()
        throws IllegalArgumentException, AccessExternalClientServerException, AccessExternalClientNotFoundException,
        InvalidParseOperationException {
        assertThat(client.selectUnitbyId(queryDsql, "")).isNotNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenrEQUESTBlank_IDFilledwhenSelectUnitById_ThenRaiseAnException()
        throws IllegalArgumentException, AccessExternalClientServerException, AccessExternalClientNotFoundException,
        InvalidParseOperationException {
        assertThat(client.selectUnitbyId("", ID)).isNotNull();
    }

    @Test(expected = AccessExternalClientNotFoundException.class)
    public void givenBadRequest_whenUpdateUnitById_ThenRaiseAnException()
        throws InvalidParseOperationException, AccessExternalClientServerException, AccessExternalClientNotFoundException {
        when(mock.put()).thenReturn(Response.status(Status.NOT_FOUND).build());
        assertThat(client.updateUnitbyId(queryDsql, ID)).isNotNull();
    }


    @Test(expected = IllegalArgumentException.class)
    public void givenRequestBlank_whenUpdateUnitById_ThenRaiseAnException()
        throws IllegalArgumentException, AccessExternalClientServerException, AccessExternalClientNotFoundException,
        InvalidParseOperationException {
        assertThat(client.updateUnitbyId("", "")).isNotNull();
    }


    @Test(expected = IllegalArgumentException.class)
    public void givenIdBlank_whenUpdateUnitById_ThenRaiseAnException()
        throws IllegalArgumentException, AccessExternalClientServerException, AccessExternalClientNotFoundException,
        InvalidParseOperationException {
        assertThat(client.updateUnitbyId(queryDsql, "")).isNotNull();
    }


    @Test(expected = IllegalArgumentException.class)
    public void givenrEquestBlank_IDFilledwhenUpdateUnitById_ThenRaiseAnException()
        throws IllegalArgumentException, AccessExternalClientServerException, AccessExternalClientNotFoundException,
        InvalidParseOperationException {
        assertThat(client.updateUnitbyId("", ID)).isNotNull();
    }

    @Test(expected = InvalidParseOperationException.class)
    public void givenBadRequest_whenUpdateUnit_ThenRaiseAnException()
        throws InvalidParseOperationException, AccessExternalClientServerException, AccessExternalClientNotFoundException {
        when(mock.put()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        assertThat(client.updateUnitbyId(queryDsql, ID)).isNotNull();
    }

    @Test(expected = AccessExternalClientServerException.class)
    public void given500_whenUpdateUnit_ThenRaiseAnException()
        throws InvalidParseOperationException, AccessExternalClientServerException, AccessExternalClientNotFoundException {
        when(mock.put()).thenReturn(Response.status(Status.UNAUTHORIZED).build());
        assertThat(client.updateUnitbyId(queryDsql, ID)).isNotNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenQueryNullWhenSelectObjectByIdThenRaiseAnIllegalArgumentException() throws Exception {
        client.selectObjectById(null, ID);
    }

    @Test(expected = AccessExternalClientServerException.class)
    public void givenQueryCorrectWhenSelectObjectByIdThenRaiseInternalServerError() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.UNAUTHORIZED).build());
        client.selectObjectById(queryDsql, ID);
    }

    @Test(expected = InvalidParseOperationException.class)
    public void givenQueryCorrectWhenSelectObjectByIdThenRaiseBadRequest() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        client.selectObjectById(queryDsql, ID);
    }

    @Test(expected = AccessExternalClientServerException.class)
    public void givenQueryCorrectWhenSelectObjectByIdThenRaisePreconditionFailed() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        client.selectObjectById(queryDsql, ID);
    }

    @Test(expected = AccessExternalClientNotFoundException.class)
    public void givenQueryCorrectWhenSelectObjectByIdThenNotFound() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.NOT_FOUND).build());
        client.selectObjectById(queryDsql, ID);
    }

    @Test
    public void givenQueryCorrectWhenSelectObjectByIdThenOK() throws Exception {
        JsonNode result = JsonHandler.getFromString(MOCK_LOGBOOK_RESULT);
        when(mock.post()).thenReturn(Response.status(Status.OK).entity(result).build());
        assertThat(client.selectObjectById(queryDsql, ID)).isNotNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenQueryNullWhenGetObjectAsInputStreamThenRaiseAnIllegalArgumentException() throws Exception {
        client.getObject(null, ID, USAGE, VERSION);
    }

    @Test(expected = AccessExternalClientServerException.class)
    public void givenQueryCorrectWhenGetObjectAsInputStreamThenRaiseInternalServerError() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        client.getObject(queryDsql, ID, USAGE, VERSION);
    }

    @Test(expected = InvalidParseOperationException.class)
    public void givenQueryCorrectWhenGetObjectAsInputStreamThenRaiseBadRequest() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        client.getObject(queryDsql, ID, USAGE, VERSION);
    }

    @Test(expected = AccessExternalClientServerException.class)
    public void givenQueryCorrectWhenGetObjectAsInputStreamThenRaisePreconditionFailed() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        client.getObject(queryDsql, ID, USAGE, VERSION);
    }

    @Test(expected = AccessExternalClientNotFoundException.class)
    public void givenQueryCorrectWhenGetObjectAsInputStreamThenNotFound() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.NOT_FOUND).build());
        client.getObject(queryDsql, ID, USAGE, VERSION);
    }

    @Test
    public void givenQueryCorrectWhenGetObjectAsInputStreamThenOK() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.OK).entity(IOUtils.toInputStream("Vitam test")).build());
        final Response response = client.getObject(queryDsql, ID, USAGE, VERSION);
        assertNotNull(response);
    }
    
    /***
     * 
     * logbook operations
     * 
     ***/
    
    @Test
    public void selectLogbookOperations() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.OK).entity(JsonHandler.getFromString(MOCK_LOGBOOK_RESULT)).build());
        assertThat(client.selectOperation(queryDsql)).isNotNull();
    }

    @Test(expected = LogbookClientNotFoundException.class)
    public void givenSelectLogbookNotFoundThenNotFound() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.NOT_FOUND).build());
        client.selectOperation(queryDsql);
    }

    @Test(expected = LogbookClientException.class)
    public void givenSelectLogbookBadQueryThenPreconditionFailed() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        client.selectOperation(queryDsql);
    }

    /***
     * 
     * logbook operationById
     * 
     ***/
    @Test
    public void selectLogbookOperationByID() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.OK).entity(JsonHandler.getFromString(MOCK_LOGBOOK_RESULT)).build());
        assertThat(client.selectOperationbyId(ID)).isNotNull();
    }

    @Test(expected = LogbookClientNotFoundException.class)
    public void givenSelectLogbookOperationByIDNotFoundThenNotFound() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.NOT_FOUND).build());
        client.selectOperationbyId(ID);
    }

    @Test(expected = LogbookClientException.class)
    public void givenSelectLogbookOperationByIDBadQueryThenPreconditionFailed() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        client.selectOperationbyId(ID);
    }    
    

    /***
     * 
     * logbook lifecycle units
     * 
     ***/
    @Test
    public void selectLogbookLifeCyclesUnit() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.OK).entity(JsonHandler.getFromString(MOCK_LOGBOOK_RESULT)).build());
        assertThat(client.selectUnitLifeCycleById(ID)).isNotNull();
    }

    @Test(expected = LogbookClientNotFoundException.class)
    public void givenSelectLogbookLifeCyclesUnitNotFoundThenNotFound() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        client.selectUnitLifeCycleById(ID);
    }

    @Test(expected = LogbookClientException.class)
    public void givenSelectLogbookLifeCyclesUnitBadQueryThenPreconditionFailed() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        client.selectUnitLifeCycleById(ID);
    }        
    
    /***
     * 
     * logbook lifecycle object
     * 
     ***/
    @Test
    public void selectLogbookLifeCyclesObject() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.OK).entity(JsonHandler.getFromString(MOCK_LOGBOOK_RESULT)).build());
        assertThat(client.selectObjectGroupLifeCycleById(ID)).isNotNull();
    }

    @Test(expected = LogbookClientNotFoundException.class)
    public void givenSelectLogbookLifeCyclesObjectsNotFoundThenNotFound() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        client.selectObjectGroupLifeCycleById(ID);
    }

    @Test(expected = LogbookClientException.class)
    public void givenSelectLogbookLifeCyclesObjectBadQueryThenPreconditionFailed() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        client.selectObjectGroupLifeCycleById(ID);
    }        
}
