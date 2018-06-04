package fr.gouv.vitam.access.external.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import fr.gouv.vitam.access.external.api.AccessExtAPI;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientNotFoundException;
import fr.gouv.vitam.common.CharsetUtils;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.external.client.AbstractMockClient;
import fr.gouv.vitam.common.external.client.ClientMockResultHelper;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.FakeInputStream;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessQuery;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.administration.ContextModel;
import fr.gouv.vitam.common.model.administration.FileFormatModel;
import fr.gouv.vitam.common.model.administration.IngestContractModel;
import fr.gouv.vitam.common.model.administration.ProfileModel;
import fr.gouv.vitam.common.model.processing.ProcessDetail;
import fr.gouv.vitam.common.server.application.AbstractVitamApplication;
import fr.gouv.vitam.common.server.application.configuration.DefaultVitamApplicationConfiguration;
import fr.gouv.vitam.common.server.application.junit.VitamJerseyTest;

public class AdminExternalClientRestTest extends VitamJerseyTest {

    private static final String ID = "id";
    protected static final String HOSTNAME = "localhost";
    private static final String AUDIT_OPTION = "{serviceProducteur: \"Service Producteur 1\"}";
    protected AdminExternalClientRest client;
    final int TENANT_ID = 0;
    final String CONTRACT = "contract";

    final String queryDsql =
        "{ \"$query\" : [ { \"$eq\" : { \"title\" : \"test\" } } ]}";

    public AdminExternalClientRestTest() {
        super(AdminExternalClientFactory.getInstance());
    }

    @Override
    public void beforeTest() throws VitamApplicationServerException {
        client = (AdminExternalClientRest) getClient();
    }

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


    @Path("/admin-external/v1/")
    public static class MockResource {
        private final ExpectedResults expectedResponse;

        public MockResource(ExpectedResults expectedResponse) {
            this.expectedResponse = expectedResponse;
        }

        @POST
        @Path("{collections}")
        @Consumes(MediaType.APPLICATION_OCTET_STREAM)
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        public Response checkDocument(@PathParam("collections") String collection, InputStream document) {
            return expectedResponse.post();
        }

        @POST
        @Path("{collections}")
        @Consumes(MediaType.APPLICATION_OCTET_STREAM)
        @Produces(MediaType.APPLICATION_JSON)
        public Response importDocument(@PathParam("collections") String collection, InputStream document) {
            return expectedResponse.post();
        }

        @POST
        @Path("{collections}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response importDocumentAsJson(@PathParam("collections") String collection, JsonNode document) {
            return expectedResponse.post();
        }

        @DELETE
        @Path("{collections}")
        @Produces(MediaType.APPLICATION_JSON)
        public Response deleteDocuments(@PathParam("collections") String collection) {
            return expectedResponse.delete();
        }

        @GET
        @Path("{collections}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response findDocuments(@PathParam("collections") String collection, JsonNode select) {
            return expectedResponse.get();
        }

        @GET
        @Path("/{collections}/{id_document}")
        @Produces(MediaType.APPLICATION_JSON)
        public Response findDocumentByID(@PathParam("collections") String collection,
            @PathParam("id_document") String documentId) {
            return expectedResponse.get();
        }

        @POST
        @Path(AccessExtAPI.ACCESSION_REGISTERS_API + "/{id_document}/" + AccessExtAPI.ACCESSION_REGISTERS_DETAIL)
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response findAccessionRegisterDetail(@PathParam("id_document") String documentId,
            JsonNode select) {
            return expectedResponse.post();
        }

        @PUT
        @Path("/{collections}/{id}")
        @Consumes(MediaType.APPLICATION_OCTET_STREAM)
        @Produces(MediaType.APPLICATION_JSON)
        public Response importProfileFile(@PathParam("id") String id, InputStream document) {
            return expectedResponse.put();
        }

        @PUT
        @Path("/{collections}/{id}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response updateProfile(@PathParam("id") String id, JsonNode queryDsl) {
            return expectedResponse.put();
        }

        @GET
        @Path("/traceability/{id}/datafiles")
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        public Response downloadTraceabilityOperationFile(@PathParam("id") String id)
            throws InvalidParseOperationException {
            return expectedResponse.get();
        }

        @GET
        @Path("/{collection}/{id}")
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        public Response downloadFile(@PathParam("id") String id)
            throws InvalidParseOperationException {
            return expectedResponse.get();
        }

        @POST
        @Path(AccessExtAPI.AUDITS)
        @Produces(MediaType.APPLICATION_JSON)
        public Response checkExistenceAudit(JsonNode query) {
            return expectedResponse.post();
        }

        @Path("/operations/{id}")
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public Response getOperationProcessExecutionDetails(@PathParam("id") String id) {
            return expectedResponse.get();
        }

        @Path("/operations/{id}")
        @HEAD
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response getWorkFlowExecutionStatus(@PathParam("id") String id) {
            return expectedResponse.head();
        }

        @Path("operations/{id}")
        @DELETE
        @Produces(MediaType.APPLICATION_JSON)
        public Response interruptWorkFlowExecution(@PathParam("id") String id) {
            return expectedResponse.delete();
        }

        @GET
        @Path("/operations")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response listOperationsDetails(@Context HttpHeaders headers, ProcessQuery query) {
            return expectedResponse.get();
        }

        @Path("operations/{id}")
        @PUT
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response updateWorkFlowStatus(@PathParam("id") String id) {
            return expectedResponse.put();
        }

        @GET
        @Path("/rulesreport/{objectId}")
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        public Response downloadObject(@PathParam("objectId") String objectId) {
            return expectedResponse.get();
        }

        @Path("evidenceaudit")
        @POST
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response evidenceAudit(String query) {
            return expectedResponse.post();
        }

    }

    @Test
    public void testCheckDocument()
        throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.OK).build());
        Response checkDocumentsResponse =
            client.checkFormats(new VitamContext(TENANT_ID), new ByteArrayInputStream("test".getBytes()));
        assertEquals(Status.OK.getStatusCode(), checkDocumentsResponse.getStatus());
    }

    @Test
    public void testCheckDocumentVitamClientException()
        throws Exception {
        VitamError error =
            VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_CHECK_DOCUMENT_NOT_FOUND, "Collection nout found");
        AbstractMockClient.FakeInboundResponse fakeResponse =
            new AbstractMockClient.FakeInboundResponse(Status.NOT_FOUND, JsonHandler.writeToInpustream(error),
                MediaType.APPLICATION_OCTET_STREAM_TYPE, new MultivaluedHashMap<String, Object>());
        when(mock.post()).thenReturn(fakeResponse);
        Response response =
            client.checkFormats(new VitamContext(TENANT_ID), new ByteArrayInputStream("test".getBytes()));
        assertNotNull(response);
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }



    @Test
    public void testCheckDocumentAccessExternalClientException()
        throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        Response checkDocumentsResponse =
            client.checkFormats(new VitamContext(TENANT_ID), new ByteArrayInputStream("test".getBytes()));
        assertEquals(Status.BAD_REQUEST.getStatusCode(), checkDocumentsResponse.getStatus());
    }

    @Test
    public void testImportFormats()
        throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.OK).build());
        assertEquals(
            client.createFormats(new VitamContext(TENANT_ID),
                new ByteArrayInputStream("test".getBytes()), "test.xml").getHttpCode(),
            Status.CREATED.getStatusCode());
    }

    @Test
    public void testImportAgencies()
        throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.OK).build());
        assertEquals(
            client.createAgencies(new VitamContext(TENANT_ID),
                new ByteArrayInputStream("test".getBytes()), "test.csv").getHttpCode(),
            Status.CREATED.getStatusCode());
    }

    @Test
    public void testFindAgencies()
        throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.OK).entity(ClientMockResultHelper.getAgencies()).build());
        assertEquals(
            client.findAgencies(new VitamContext(TENANT_ID).setAccessContract(null), JsonHandler.createObjectNode())
                .toString(),
            ClientMockResultHelper.getAgencies().toString());
    }


    @Test
    public void testAgencyById()
        throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.OK).entity(ClientMockResultHelper.getAgencies()).build());
        assertEquals(
            client.findAgencyByID(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), ID).toString(),
            ClientMockResultHelper.getAgencies().toString());
    }

    @Test(expected = AccessExternalClientNotFoundException.class)
    public void testImportFormatsAccessExternalClientNotFoundException()
        throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.NOT_FOUND).build());
        client.createFormats(new VitamContext(TENANT_ID),
            new ByteArrayInputStream("test".getBytes()), "test.xml");
    }

    @Test
    public void testImportFormatsAccessExternalClientException()
        throws Exception {
        VitamError error = new VitamError("vitam_code").setHttpCode(400).setContext("ADMIN").setState("INVALID")
            .setMessage("invalid input").setDescription("Input file of formats is malformed");
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).entity(error).build());
        assertEquals(Status.BAD_REQUEST.getStatusCode(),
            client.createFormats(new VitamContext(TENANT_ID),
                new ByteArrayInputStream("test".getBytes()), "test.xml").getHttpCode());
    }

    @Test
    public void testFindFormats()
        throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.OK).entity(ClientMockResultHelper.getFormatList()).build());
        assertEquals(
            client.findFormats(new VitamContext(TENANT_ID).setAccessContract(null), JsonHandler.createObjectNode())
                .toString(),
            ClientMockResultHelper.getFormatList().toString());
    }

    @Test
    public void testFindFormatsNotFound()
        throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        assertThat(
            client.findFormats(new VitamContext(TENANT_ID).setAccessContract(null), JsonHandler.createObjectNode())
                .getHttpCode()).isEqualTo(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void testFindFormatsPreconditionFailed()
        throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        assertThat(
            client.findFormats(new VitamContext(TENANT_ID).setAccessContract(null), JsonHandler.createObjectNode())
                .getHttpCode()).isEqualTo(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void testFindFormatsById()
        throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.OK).entity(ClientMockResultHelper.getFormat()).build());
        assertEquals(
            client.findFormatById(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), ID).toString(),
            ClientMockResultHelper.getFormat().toString());
    }

    @Test
    public void testFindDocumentByIdAccessExternalClientNotFoundException()
        throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        assertThat(client.findFormatById(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), ID).getHttpCode())
            .isEqualTo(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void testFindDocumentByIdAccessExternalClientException()
        throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        assertThat(client.findFormatById(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), ID).getHttpCode())
            .isEqualTo(Status.PRECONDITION_FAILED.getStatusCode());
    }


    @Test()
    public void importContractsWithCorrectJsonReturnCreated()
        throws FileNotFoundException, InvalidParseOperationException, AccessExternalClientException {
        when(mock.post()).thenReturn(
            Response.status(Status.CREATED).entity(new RequestResponseOK<>().addAllResults(getContracts())).build());
        InputStream fileContracts = PropertiesUtils.getResourceAsStream("referential_contracts_ok.json");
        RequestResponse resp =
            client.createIngestContracts(new VitamContext(TENANT_ID), fileContracts);
        Assert.assertTrue(RequestResponseOK.class.isAssignableFrom(resp.getClass()));
        Assert.assertTrue((((RequestResponseOK) resp).isOk()));
    }

    private List<Object> getContracts() throws FileNotFoundException, InvalidParseOperationException {
        InputStream fileContracts = PropertiesUtils.getResourceAsStream("referential_contracts_ok.json");
        ArrayNode array = (ArrayNode) JsonHandler.getFromInputStream(fileContracts);
        List<Object> res = new ArrayList<>();
        array.forEach(e -> res.add(e));
        return res;
    }

    @Test()
    public void importContractsWithIncorrectJsonReturnBadRequest()
        throws FileNotFoundException, InvalidParseOperationException, AccessExternalClientException {
        VitamError error = new VitamError("vitam_code").setHttpCode(400).setContext("ADMIN").setState("INVALID")
            .setMessage("invalid input").setDescription("Input file of contracts is malformed");
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).entity(error).build());
        RequestResponse resp =
            client.createIngestContracts(new VitamContext(TENANT_ID), new FakeInputStream(0));
        Assert.assertTrue(VitamError.class.isAssignableFrom(resp.getClass()));
        Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), (((VitamError) resp).getHttpCode()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void importContractsWithNullStreamThrowIllegalArgException()
        throws FileNotFoundException, InvalidParseOperationException, AccessExternalClientException {
        client.createIngestContracts(new VitamContext(TENANT_ID), null);
    }



    @Test()
    public void importAccessContractsWithCorrectJsonReturnCreated()
        throws FileNotFoundException, InvalidParseOperationException, AccessExternalClientException {
        when(mock.post()).thenReturn(
            Response.status(Status.CREATED).entity(new RequestResponseOK<>().addAllResults(getAccessContracts()))
                .build());
        InputStream fileContracts = PropertiesUtils.getResourceAsStream("contracts_access_ok.json");
        RequestResponse resp =
            client.createAccessContracts(new VitamContext(TENANT_ID), fileContracts);
        Assert.assertTrue(RequestResponseOK.class.isAssignableFrom(resp.getClass()));
        Assert.assertTrue((((RequestResponseOK) resp).isOk()));
    }

    private List<Object> getAccessContracts() throws FileNotFoundException, InvalidParseOperationException {
        InputStream fileContracts = PropertiesUtils.getResourceAsStream("contracts_access_ok.json");
        ArrayNode array = (ArrayNode) JsonHandler.getFromInputStream(fileContracts);
        List<Object> res = new ArrayList<>();
        array.forEach(e -> res.add(e));
        return res;
    }

    @Test
    public void importAccessContractsWithIncorrectJsonReturnBadRequest()
        throws FileNotFoundException, InvalidParseOperationException, AccessExternalClientException {
        VitamError error = new VitamError("vitam_code").setHttpCode(400).setContext("ADMIN").setState("INVALID")
            .setMessage("invalid input").setDescription("Input file of contracts is malformed");
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).entity(error).build());
        RequestResponse resp =
            client.createAccessContracts(new VitamContext(TENANT_ID), new FakeInputStream(0));
        Assert.assertTrue(VitamError.class.isAssignableFrom(resp.getClass()));
        Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), (((VitamError) resp).getHttpCode()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void importAccessContractsWithNullStreamThrowIllegalArgException()
        throws FileNotFoundException, InvalidParseOperationException, AccessExternalClientException {
        client.createAccessContracts(new VitamContext(TENANT_ID), null);
    }


    /**
     * Test that findAccessContracts is reachable and does not return elements
     *
     * @throws VitamClientInternalException
     */
    @Test
    public void findAllAccessContractsThenReturnEmpty()
        throws VitamClientException {

        when(mock.get()).thenReturn(Response.status(Status.OK)
            .entity(new RequestResponseOK<AccessContractModel>().setHttpCode(Status.OK.getStatusCode())).build());
        RequestResponse<AccessContractModel> resp =
            client.findAccessContracts(new VitamContext(TENANT_ID).setAccessContract(null),
                JsonHandler.createObjectNode());
        assertThat(resp.isOk()).isTrue();
        assertThat(resp.getStatus()).isEqualTo(Status.OK.getStatusCode());
        assertThat(((RequestResponseOK<AccessContractModel>) resp).getResults()).hasSize(0);
    }

    @Test
    public void findAllFormatsThenReturnOk()
        throws FileNotFoundException, InvalidParseOperationException, AccessExternalClientException,
        VitamClientException {

        when(mock.get()).thenReturn(
            Response.status(Status.OK).entity(ClientMockResultHelper.getFormatList()).build());
        RequestResponse<FileFormatModel> resp =
            client.findFormats(new VitamContext(TENANT_ID).setAccessContract(null), JsonHandler.createObjectNode());
        assertThat(resp).isInstanceOf(RequestResponseOK.class);
        assertThat(((RequestResponseOK<FileFormatModel>) resp).getResults()).hasSize(1);
        assertThat(((RequestResponseOK<FileFormatModel>) resp).getFirstResult().getPuid()).isEqualTo("x-fmt/20");
    }

    @Test
    public void findAllFormatsThenCollectionNotFound() throws VitamClientException {

        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        RequestResponse<FileFormatModel> response =
            client.findFormats(new VitamContext(TENANT_ID).setAccessContract(null), JsonHandler.createObjectNode());
        assertThat(response.getHttpCode()).isEqualTo(Status.NOT_FOUND.getStatusCode());
    }

    /**
     * Test that findAccessContracts is reachable and return two elements as expected
     *
     * @throws VitamClientInternalException
     */
    @Test
    public void findAllAccessContractsThenReturnOne()
        throws VitamClientException {

        when(mock.get())
            .thenReturn(Response.status(Status.OK).entity(ClientMockResultHelper.getAccessContracts()).build());
        RequestResponse<AccessContractModel> resp =
            client.findAccessContracts(new VitamContext(TENANT_ID).setAccessContract(null),
                JsonHandler.createObjectNode());
        assertThat(resp.isOk()).isTrue();
        assertThat(((RequestResponseOK<AccessContractModel>) resp).getResults()).hasSize(1);
    }

    /**
     * Test that findAccessContractsByID is reachable
     *
     * @throws FileNotFoundException
     * @throws InvalidParseOperationException
     * @throws VitamClientException
     */
    @Test
    public void findAccessContractsByIdThenReturnEmpty()
        throws FileNotFoundException, InvalidParseOperationException, AccessExternalClientException,
        VitamClientException {

        when(mock.get()).thenReturn(Response.status(Status.OK).entity(new RequestResponseOK<>()).build());
        RequestResponse<AccessContractModel> resp =
            client.findAccessContractById(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                "fakeId");
        assertThat(resp).isInstanceOf(RequestResponseOK.class);
        assertThat(((RequestResponseOK) resp).getResults()).hasSize(0);
    }

    /**
     * Test that findIngestContracts is reachable and return two elements as expected
     *
     * @throws VitamClientInternalException
     */
    @Test
    public void findAllIngestContractsThenReturnOne()
        throws VitamClientException {

        when(mock.get())
            .thenReturn(Response.status(Status.OK).entity(ClientMockResultHelper.getIngestContracts()).build());
        RequestResponse<IngestContractModel> resp =
            client.findIngestContracts(new VitamContext(TENANT_ID).setAccessContract(null),
                JsonHandler.createObjectNode());
        assertThat(resp.isOk()).isTrue();
        assertThat(((RequestResponseOK<IngestContractModel>) resp).getResults()).hasSize(1);
    }

    /**
     * Test that findIngestContractsByID is reachable
     *
     * @throws FileNotFoundException
     * @throws InvalidParseOperationException
     * @throws VitamClientException
     */
    @Test
    public void findIngestContractsByIdThenReturnEmpty()
        throws FileNotFoundException, InvalidParseOperationException, AccessExternalClientException,
        VitamClientException {

        when(mock.get()).thenReturn(Response.status(Status.OK).entity(new RequestResponseOK<>()).build());
        RequestResponse<IngestContractModel> resp =
            client.findIngestContractById(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                "fakeId");
        assertThat(resp).isInstanceOf(RequestResponseOK.class);
        assertThat(((RequestResponseOK) resp).getResults()).hasSize(0);
    }

    /**
     * Test that findContexts is reachable and return two elements as expected
     *
     * @throws VitamClientInternalException
     */
    @Test
    public void findAllContextsThenReturnOne()
        throws VitamClientException {

        when(mock.get())
            .thenReturn(Response.status(Status.OK).entity(ClientMockResultHelper.getContexts(Status.OK.getStatusCode()))
                .build());
        RequestResponse<ContextModel> resp =
            client.findContexts(new VitamContext(TENANT_ID).setAccessContract(null), JsonHandler.createObjectNode());
        assertThat(resp.isOk()).isTrue();
        assertThat(((RequestResponseOK<ContextModel>) resp).getResults()).hasSize(1);
    }

    /**
     * Test that findContextsByID is reachable
     *
     * @throws FileNotFoundException
     * @throws InvalidParseOperationException
     * @throws VitamClientException
     */
    @Test
    public void findContextsByIdThenReturnEmpty()
        throws FileNotFoundException, InvalidParseOperationException, AccessExternalClientException,
        VitamClientException {

        when(mock.get()).thenReturn(Response.status(Status.OK).entity(new RequestResponseOK<>()).build());
        RequestResponse<ContextModel> resp =
            client.findContextById(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), "fakeId");
        assertThat(resp).isInstanceOf(RequestResponseOK.class);
        assertThat(((RequestResponseOK) resp).getResults()).hasSize(0);
    }

    @Test
    public void createProfilesWithCorrectJsonReturnCreated()
        throws FileNotFoundException, InvalidParseOperationException, AccessExternalClientException {
        when(mock.post()).thenReturn(
            Response.status(Status.CREATED)
                .entity(ClientMockResultHelper.getProfiles(Status.CREATED.getStatusCode()))
                .build());
        InputStream fileProfiles = PropertiesUtils.getResourceAsStream("contracts_access_ok.json");
        RequestResponse resp = client.createProfiles(new VitamContext(TENANT_ID), fileProfiles);
        Assert.assertTrue(RequestResponseOK.class.isAssignableFrom(resp.getClass()));
        Assert.assertTrue((((RequestResponseOK) resp).isOk()));
    }


    @Test()
    public void createProfilesWithIncorrectJsonReturnBadRequest()
        throws FileNotFoundException, InvalidParseOperationException, AccessExternalClientException {
        VitamError error = new VitamError("vitam_code").setHttpCode(400).setContext("ADMIN").setState("INVALID")
            .setMessage("invalid input").setDescription("Input file of profiles is malformed");
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).entity(error).build());
        RequestResponse resp =
            client.createProfiles(new VitamContext(TENANT_ID), new FakeInputStream(0));
        Assert.assertTrue(VitamError.class.isAssignableFrom(resp.getClass()));
        Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), (((VitamError) resp).getHttpCode()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void createProfilesWithNullStreamThrowIllegalArgException()
        throws FileNotFoundException, InvalidParseOperationException, AccessExternalClientException {
        client.createProfiles(new VitamContext(TENANT_ID), null);
    }



    @Test
    public void importProfileFileXSDReturnCreated()
        throws FileNotFoundException, InvalidParseOperationException, AccessExternalClientException {
        when(mock.put()).thenReturn(
            Response.status(Status.CREATED).entity(new RequestResponseOK<>())
                .build());
        RequestResponse resp =
            client.createProfileFile(new VitamContext(TENANT_ID), "FakeIdXSD", new FakeInputStream(0));
        Assert.assertTrue(RequestResponseOK.class.isAssignableFrom(resp.getClass()));
        Assert.assertTrue((((RequestResponseOK) resp).isOk()));
    }

    @Test
    public void importProfileFileRNGReturnCreated()
        throws FileNotFoundException, InvalidParseOperationException, AccessExternalClientException {
        when(mock.put()).thenReturn(
            Response.status(Status.CREATED).entity(new RequestResponseOK<>())
                .build());
        RequestResponse resp =
            client.createProfileFile(new VitamContext(TENANT_ID), "FakeIdRNG", new FakeInputStream(0));
        Assert.assertTrue(RequestResponseOK.class.isAssignableFrom(resp.getClass()));
        Assert.assertTrue((((RequestResponseOK) resp).isOk()));
    }



    @Test
    public void givenProfileIdWhenDownloadProfileFileThenOK() throws Exception {

        when(mock.get()).thenReturn(ClientMockResultHelper.getObjectStream());

        Response response = client.downloadProfileFile(new VitamContext(TENANT_ID), "OP_ID");
        assertNotNull(response);
    }

    /**
     * Test that findProfiles is reachable and does not return elements
     *
     * @throws VitamClientException
     */
    @Test
    public void findAllProfilesThenReturnEmpty()
        throws VitamClientException {

        when(mock.get()).thenReturn(Response.status(Status.OK).entity(new RequestResponseOK<>()).build());
        RequestResponse<ProfileModel> resp =
            client.findProfiles(new VitamContext(TENANT_ID).setAccessContract(null), JsonHandler.createObjectNode());
        assertThat(resp.isOk()).isTrue();
        assertThat(((RequestResponseOK<ProfileModel>) resp).getResults()).hasSize(0);
    }

    /**
     * Test that findProfiles is reachable and return one elements as expected
     *
     * @throws VitamClientException
     */
    @Test
    public void findAllProfilesThenReturnOne()
        throws VitamClientException {

        when(mock.get()).thenReturn(
            Response.status(Status.OK).entity(ClientMockResultHelper.getProfiles(Status.OK.getStatusCode())).build());
        RequestResponse<ProfileModel> resp =
            client.findProfiles(new VitamContext(TENANT_ID).setAccessContract(null), JsonHandler.createObjectNode());
        assertThat(resp.isOk()).isTrue();
        assertThat(((RequestResponseOK<ProfileModel>) resp).getResults()).hasSize(1);
    }

    /**
     * @throws FileNotFoundException
     * @throws InvalidParseOperationException
     * @throws VitamClientException
     */
    @Test
    public void findProfilesByIdThenReturnEmpty()
        throws FileNotFoundException, InvalidParseOperationException, AccessExternalClientException,
        VitamClientException {

        when(mock.get()).thenReturn(Response.status(Status.OK).entity(new RequestResponseOK<>()).build());
        RequestResponse<ProfileModel> resp =
            client.findProfileById(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), "fakeId");
        assertThat(resp).isInstanceOf(RequestResponseOK.class);
        assertThat(((RequestResponseOK) resp).getResults()).hasSize(0);
    }

    @Test
    public void importContextsWithCorrectJsonReturnCreated()
        throws FileNotFoundException, InvalidParseOperationException, AccessExternalClientException {
        when(mock.post()).thenReturn(
            Response.status(Status.CREATED).entity(new RequestResponseOK<>().addAllResults(getContracts())).build());
        InputStream fileContexts = PropertiesUtils.getResourceAsStream("contexts_ok.json");
        RequestResponse resp = client.createContexts(new VitamContext(TENANT_ID), fileContexts);
        Assert.assertTrue(RequestResponseOK.class.isAssignableFrom(resp.getClass()));
        Assert.assertTrue((((RequestResponseOK) resp).isOk()));
    }

    /***
     *
     * Accession register test
     *
     ***/
    @Test
    public void selectAccessionExternalSumary() throws Exception {
        when(mock.get()).thenReturn(
            Response.status(Status.OK).entity(ClientMockResultHelper.getAccessionRegisterSummary()).build());
        assertThat(
            client.findAccessionRegister(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                JsonHandler.getFromString(queryDsql)).getHttpCode())
                    .isEqualTo(Status.OK.getStatusCode());
    }

    @Test
    public void selectAccessionExternalSumaryError() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        assertThat(client.findAccessionRegister(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
            JsonHandler.getFromString(queryDsql)).getHttpCode()).isEqualTo(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void selectAccessionExternalDetail() throws Exception {
        when(mock.post()).thenReturn(
            Response.status(Status.OK).entity(ClientMockResultHelper.getAccessionRegisterDetail()).build());
        assertThat(client.getAccessionRegisterDetail(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), ID,
            JsonHandler.getFromString(queryDsql))
            .getHttpCode()).isEqualTo(Status.OK.getStatusCode());
    }

    @Test
    public void selectAccessionExternalDetailError() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.NOT_FOUND).build());
        assertThat(client.getAccessionRegisterDetail(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), ID,
            JsonHandler.getFromString(queryDsql))
            .getHttpCode()).isEqualTo(Status.NOT_FOUND.getStatusCode());
    }

    /***
     *
     * TRACEABILITY operation test
     *
     ***/

    @Test
    public void testCheckTraceabilityOperation()
        throws Exception {
        when(mock.post()).thenReturn(
            Response.status(Status.OK).entity(ClientMockResultHelper.getLogbooksRequestResponseJsonNode()).build());
        client.checkTraceabilityOperation(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
            JsonHandler.getFromString(queryDsql));
    }

    @Test
    public void testDownloadTraceabilityOperationFile()
        throws Exception {
        when(mock.get()).thenReturn(ClientMockResultHelper.getObjectStream());
        client.downloadTraceabilityOperationFile(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), ID);
    }

    @Test
    public void testCheckExistenceAudit()
        throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.OK).build());
        JsonNode auditOption = JsonHandler.getFromString(AUDIT_OPTION);
        assertThat(
            client.launchAudit(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), auditOption).getHttpCode())
                .isEqualTo(Status.OK.getStatusCode());
    }

    @Test
    public void testImportSecurityProfiles()
        throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.OK).build());
        assertEquals(
            client.createSecurityProfiles(new VitamContext(TENANT_ID),
                new ByteArrayInputStream("{}".getBytes()), "test.json").getHttpCode(),
            Status.CREATED.getStatusCode());
    }

    @Test
    public void testFindSecurityProfiles()
        throws Exception {
        when(mock.get())
            .thenReturn(Response.status(Status.OK).entity(ClientMockResultHelper.getSecurityProfiles()).build());
        assertThat(client.findSecurityProfileById(
            new VitamContext(TENANT_ID).setAccessContract(CONTRACT), ID).isOk()).isTrue();
    }

    @Test
    public void testFindSecurityProfileById()
        throws Exception {
        when(mock.get())
            .thenReturn(Response.status(Status.OK).entity(ClientMockResultHelper.getSecurityProfiles()).build());
        assertThat(client.findSecurityProfileById(
            new VitamContext(TENANT_ID).setAccessContract(CONTRACT), ID).isOk()).isTrue();
    }

    @Test
    public void testUpdateProfile() throws Exception {
        when(mock.put()).thenReturn(Response.status(Status.OK).entity(new RequestResponseOK<>()).build());
        assertThat(client.updateProfile(
            new VitamContext(TENANT_ID).setAccessContract(CONTRACT), ID, JsonHandler.createObjectNode()).isOk())
                .isTrue();
    }

    @Test
    public void listOperationsDetailsTest() throws Exception {

        when(mock.get()).thenReturn(Response.status(Status.OK)
            .entity(new RequestResponseOK<>().addResult(new ProcessDetail()).setHttpCode(Status.OK.getStatusCode()))
            .build());
        RequestResponse<ProcessDetail> resp = client.listOperationsDetails(new VitamContext(0), new ProcessQuery());
        assertEquals(resp.getStatus(), Status.OK.getStatusCode());

        when(mock.get()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        RequestResponse<ProcessDetail> resp2 = client.listOperationsDetails(new VitamContext(0), new ProcessQuery());
        assertEquals(resp2.getStatus(), Status.INTERNAL_SERVER_ERROR.getStatusCode());

        when(mock.get()).thenReturn(Response.status(Status.UNSUPPORTED_MEDIA_TYPE).build());
        RequestResponse<ProcessDetail> resp3 = client.listOperationsDetails(new VitamContext(0), null);
        assertEquals(resp3.getStatus(), Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode());
    }


    @Test
    public void givenOKWhenGetOperationDetailThenReturnOK() throws VitamClientException, IllegalArgumentException {
        when(mock.get()).thenReturn(Response.status(Status.OK.getStatusCode())
            .entity(new RequestResponseOK<ItemStatus>().addResult(new ItemStatus())).build());
        RequestResponse<ItemStatus> result =
            client.getOperationProcessExecutionDetails(new VitamContext(TENANT_ID), ID);
        assertEquals(result.getHttpCode(), Status.OK.getStatusCode());
    }

    @Test
    public void givenNotFoundWhenGetOperationDetailThenReturnNotFound() throws VitamClientException, IllegalArgumentException {
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND.getStatusCode())
            .entity(new RequestResponseOK<ItemStatus>().addResult(new ItemStatus())).build());
        RequestResponse<ItemStatus> result =
            client.getOperationProcessExecutionDetails(new VitamContext(TENANT_ID), ID);
        assertEquals(result.getHttpCode(), Status.NOT_FOUND.getStatusCode());
    }
    
    @Test
    public void givenOKWhenCancelOperationThenReturnOK() throws VitamClientException, IllegalArgumentException {
        when(mock.delete()).thenReturn(Response.status(Status.OK.getStatusCode())
            .entity(new RequestResponseOK<ItemStatus>().addResult(new ItemStatus())).build());
        RequestResponse<ItemStatus> result = client.cancelOperationProcessExecution(new VitamContext(TENANT_ID), ID);
        assertEquals(result.getHttpCode(), Status.OK.getStatusCode());

    }

    @Test
    public void givenHeadOperationStatusThenOK()
        throws Exception {

        when(mock.head()).thenReturn(
            Response.status(Status.OK)
                .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATE, ProcessState.COMPLETED)
                .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATUS, StatusCode.OK)
                .header(GlobalDataRest.X_CONTEXT_ID, "Fake").build());
        RequestResponse<ItemStatus> resp = client.getOperationProcessStatus(new VitamContext(0), ID);
        assertEquals(true, resp.isOk());
        ItemStatus itemStatus = ((RequestResponseOK<ItemStatus>) resp).getResults().get(0);
        assertEquals(StatusCode.OK, itemStatus.getGlobalStatus());
        assertEquals(Status.OK, itemStatus.getGlobalStatus().getEquivalentHttpStatus());
        assertEquals(ProcessState.COMPLETED, itemStatus.getGlobalState());

    }

    @Test
    public void cancelOperationTest()
        throws Exception {
        when(mock.delete()).thenReturn(Response.status(Status.OK)
            .entity(new RequestResponseOK<>().addResult(new ItemStatus()).setHttpCode(Status.OK.getStatusCode()))
            .build());
        RequestResponse<ItemStatus> resp = client.cancelOperationProcessExecution(new VitamContext(0), ID);
        assertTrue(resp.isOk());
        assertEquals(resp.getStatus(), Status.OK.getStatusCode());

        when(mock.delete()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        resp = client.cancelOperationProcessExecution(new VitamContext(0), ID);
        assertFalse(resp.isOk());
        assertEquals(resp.getStatus(), Status.PRECONDITION_FAILED.getStatusCode());

        when(mock.delete()).thenReturn(Response.status(Status.UNAUTHORIZED).build());
        resp = client.cancelOperationProcessExecution(new VitamContext(0), ID);
        assertFalse(resp.isOk());
        assertEquals(resp.getStatus(), Status.UNAUTHORIZED.getStatusCode());

        when(mock.delete()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        resp = client.cancelOperationProcessExecution(new VitamContext(0), ID);
        assertFalse(resp.isOk());
        assertEquals(resp.getStatus(), Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void updateOperationActionProcessTest() throws Exception {
        when(mock.put()).thenReturn(Response.status(Status.OK)
            .entity(new RequestResponseOK<>().addResult(new ItemStatus()).setHttpCode(Status.OK.getStatusCode()))
            .build());
        RequestResponse<ItemStatus> resp = client.updateOperationActionProcess(new VitamContext(0), "NEXT", ID);
        assertTrue(resp.isOk());
        assertEquals(Status.OK.getStatusCode(), resp.getStatus());

        when(mock.put()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        RequestResponse<ItemStatus> resp2 = client.updateOperationActionProcess(new VitamContext(0), "NEXT", ID);
        assertFalse(resp2.isOk());
        assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), resp2.getStatus());
    }

    @Test
    public void givenNotFoundWhenDownloadObjectThenReturn404()
        throws VitamClientException, InvalidParseOperationException, IOException {
        VitamError error = VitamCodeHelper.toVitamError(VitamCode.INGEST_EXTERNAL_NOT_FOUND, "NOT FOUND");
        AbstractMockClient.FakeInboundResponse fakeResponse =
            new AbstractMockClient.FakeInboundResponse(Status.NOT_FOUND, JsonHandler.writeToInpustream(error),
                MediaType.APPLICATION_OCTET_STREAM_TYPE, new MultivaluedHashMap<String, Object>());
        when(mock.get()).thenReturn(fakeResponse);
        InputStream input =
            client.downloadRulesReport(new VitamContext(TENANT_ID), "1")
                .readEntity(InputStream.class);
        VitamError response = JsonHandler.getFromInputStream(input, VitamError.class);
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getHttpCode());
    }

    @Test
    public void givenInputstreamWhenDownloadObjectThenReturnOK()
        throws VitamClientException {

        when(mock.get()).thenReturn(ClientMockResultHelper.getObjectStream());

        final InputStream fakeUploadResponseInputStream =
            client.downloadRulesReport(new VitamContext(TENANT_ID), "1")
                .readEntity(InputStream.class);
        assertNotNull(fakeUploadResponseInputStream);

        try {
            assertTrue(IOUtils.contentEquals(fakeUploadResponseInputStream,
                IOUtils.toInputStream("test", CharsetUtils.UTF_8)));
        } catch (final IOException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void evidenceAuditTest()
        throws VitamClientException, InvalidParseOperationException {

        JsonNode dsl = JsonHandler.getFromString(queryDsql);
        when(mock.post()).thenReturn(Response.status(Status.OK.getStatusCode())
            .entity(new RequestResponseOK<ItemStatus>().addResult(new ItemStatus())).build());
        RequestResponse<ItemStatus> result = client.evidenceAudit(new VitamContext(TENANT_ID), dsl);
        assertEquals(result.getHttpCode(), Status.OK.getStatusCode());
    }

}
