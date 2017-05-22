package fr.gouv.vitam.access.external.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.stream.StreamUtils;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import fr.gouv.vitam.access.external.api.AdminCollections;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientNotFoundException;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.client.ClientMockResultHelper;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.FakeInputStream;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.server.application.AbstractVitamApplication;
import fr.gouv.vitam.common.server.application.configuration.DefaultVitamApplicationConfiguration;
import fr.gouv.vitam.common.server.application.junit.VitamJerseyTest;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;

public class AdminExternalClientRestTest extends VitamJerseyTest {

    private static final String ID = "id";
    protected static final String HOSTNAME = "localhost";
    protected AdminExternalClientRest client;
    final int TENANT_ID = 0;

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

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

        @PUT
        @Path("{collections}")
        @Consumes(MediaType.APPLICATION_OCTET_STREAM)
        @Produces(MediaType.APPLICATION_JSON)
        public Response checkDocument(@PathParam("collections") String collection, InputStream document) {
            return expectedResponse.put();
        }

        @POST
        @Path("{collections}")
        @Consumes(MediaType.APPLICATION_OCTET_STREAM)
        @Produces(MediaType.APPLICATION_JSON)
        public Response importDocument(@PathParam("collections") String collection, InputStream document) {
            return expectedResponse.post();
        }

        @DELETE
        @Path("{collections}")
        @Produces(MediaType.APPLICATION_JSON)
        public Response deleteDocuments(@PathParam("collections") String collection) {
            return expectedResponse.delete();
        }


        @POST
        @Path("{collections}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response findDocuments(@PathParam("collections") String collection, JsonNode select) {
            return expectedResponse.post();
        }


        @POST
        @Path("/{collections}/{id_document}")
        @Produces(MediaType.APPLICATION_JSON)
        public Response findDocumentByID(@PathParam("collections") String collection,
            @PathParam("id_document") String documentId) {
            return expectedResponse.get();
        }




        @PUT
        @Path("/{collections}/{id}")
        @Consumes(MediaType.APPLICATION_OCTET_STREAM)
        @Produces(MediaType.APPLICATION_JSON)
        public Response importProfileFile(@PathParam("id") String id, InputStream document) {
            return expectedResponse.put();
        }

        @GET
        @Path("/{collections}/{id}")
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        public Response downloadTraceabilityOperationFile(@PathParam("id") String id)
            throws InvalidParseOperationException {
            return expectedResponse.get();
        }

    }

    @Test
    public void testCheckDocument()
        throws Exception {
        when(mock.put()).thenReturn(Response.status(Status.OK).build());
        assertEquals(
            client.checkDocuments(AdminCollections.FORMATS, new ByteArrayInputStream("test".getBytes()), TENANT_ID),
            Status.OK);
    }

    @Test(expected = AccessExternalClientNotFoundException.class)
    public void testCheckDocumentAccessExternalClientNotFoundException()
        throws Exception {
        when(mock.put()).thenReturn(Response.status(Status.NOT_FOUND).build());
        client.checkDocuments(AdminCollections.FORMATS, new ByteArrayInputStream("test".getBytes()), TENANT_ID);
    }

    @Test
    public void testCheckDocumentAccessExternalClientException()
        throws Exception {
        when(mock.put()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        assertEquals(Status.BAD_REQUEST, 
            client.checkDocuments(AdminCollections.FORMATS, new ByteArrayInputStream("test".getBytes()), TENANT_ID));
    }

    @Test
    public void testImportDocument()
        throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.OK).build());
        assertEquals(
            client.createDocuments(AdminCollections.FORMATS, new ByteArrayInputStream("test".getBytes()), TENANT_ID),
            Status.OK);
    }

    @Test(expected = AccessExternalClientNotFoundException.class)
    public void testImportDocumentAccessExternalClientNotFoundException()
        throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.NOT_FOUND).build());
        client.createDocuments(AdminCollections.FORMATS, new ByteArrayInputStream("test".getBytes()), TENANT_ID);
    }

    @Test
    public void testImportDocumentAccessExternalClientException()
        throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).entity("not well formated").build());
        assertEquals(Status.BAD_REQUEST, 
            client.createDocuments(AdminCollections.FORMATS, new ByteArrayInputStream("test".getBytes()), TENANT_ID));
    }

    @Test
    public void testFindDocuments()
        throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.OK).entity(ClientMockResultHelper.getFormatList()).build());
        assertEquals(
            client.findDocuments(AdminCollections.FORMATS, JsonHandler.createObjectNode(), TENANT_ID).toString(),
            ClientMockResultHelper.getFormatList().toString());
    }

    @Test(expected = AccessExternalClientNotFoundException.class)
    public void testFindDocumentAccessExternalClientNotFoundException()
        throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.NOT_FOUND).build());
        client.findDocuments(AdminCollections.FORMATS, JsonHandler.createObjectNode(), TENANT_ID);
    }

    @Test(expected = AccessExternalClientException.class)
    public void testFindDocumentAccessExternalClientException()
        throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        client.findDocuments(AdminCollections.FORMATS, JsonHandler.createObjectNode(), TENANT_ID);
    }

    @Test
    public void testFindDocumentById()
        throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.OK).entity(ClientMockResultHelper.getFormat()).build());
        assertEquals(
            client.findDocumentById(AdminCollections.FORMATS, ID, TENANT_ID).toString(),
            ClientMockResultHelper.getFormat().toString());
    }

    @Test(expected = AccessExternalClientNotFoundException.class)
    public void testFindDocumentByIdAccessExternalClientNotFoundException()
        throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        client.findDocumentById(AdminCollections.FORMATS, ID, TENANT_ID);
    }

    @Test(expected = AccessExternalClientException.class)
    public void testFindDocumentByIdAccessExternalClientException()
        throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        client.findDocumentById(AdminCollections.FORMATS, ID, TENANT_ID);
    }


    @Test()
    @RunWithCustomExecutor
    public void importContractsWithCorrectJsonReturnCreated()
        throws FileNotFoundException, InvalidParseOperationException, AccessExternalClientException {
        when(mock.post()).thenReturn(
            Response.status(Status.CREATED).entity(new RequestResponseOK<>().addAllResults(getContracts())).build());
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        InputStream fileContracts = PropertiesUtils.getResourceAsStream("referential_contracts_ok.json");
        RequestResponse resp = client.importContracts(fileContracts, TENANT_ID, AdminCollections.CONTRACTS);
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
    @RunWithCustomExecutor
    public void importContractsWithIncorrectJsonReturnBadRequest()
        throws FileNotFoundException, InvalidParseOperationException, AccessExternalClientException {
        VitamError error = new VitamError("vitam_code").setHttpCode(400).setContext("ADMIN").setState("INVALID")
            .setMessage("invalid input").setDescription("Input file of contracts is malformed");
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).entity(error).build());
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        RequestResponse resp =
            client.importContracts(new FakeInputStream(0), TENANT_ID, AdminCollections.CONTRACTS);
        Assert.assertTrue(VitamError.class.isAssignableFrom(resp.getClass()));
        Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), (((VitamError) resp).getHttpCode()));
    }

    @Test(expected = IllegalArgumentException.class)
    @RunWithCustomExecutor
    public void importContractsWithNullStreamThrowIllegalArgException()
        throws FileNotFoundException, InvalidParseOperationException, AccessExternalClientException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        client.importContracts(null, TENANT_ID, AdminCollections.CONTRACTS);
    }



    @Test()
    @RunWithCustomExecutor
    public void importAccessContractsWithCorrectJsonReturnCreated()
        throws FileNotFoundException, InvalidParseOperationException, AccessExternalClientException {
        when(mock.post()).thenReturn(
            Response.status(Status.CREATED).entity(new RequestResponseOK<>().addAllResults(getAccessContracts()))
                .build());
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        InputStream fileContracts = PropertiesUtils.getResourceAsStream("contracts_access_ok.json");
        RequestResponse resp = client.importContracts(fileContracts, TENANT_ID, AdminCollections.ACCESS_CONTRACTS);
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

    @Test()
    @RunWithCustomExecutor
    public void importAccessContractsWithIncorrectJsonReturnBadRequest()
        throws FileNotFoundException, InvalidParseOperationException, AccessExternalClientException {
        VitamError error = new VitamError("vitam_code").setHttpCode(400).setContext("ADMIN").setState("INVALID")
            .setMessage("invalid input").setDescription("Input file of contracts is malformed");
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).entity(error).build());
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        RequestResponse resp =
            client.importContracts(new FakeInputStream(0), TENANT_ID, AdminCollections.ACCESS_CONTRACTS);
        Assert.assertTrue(VitamError.class.isAssignableFrom(resp.getClass()));
        Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), (((VitamError) resp).getHttpCode()));
    }

    @Test(expected = IllegalArgumentException.class)
    @RunWithCustomExecutor
    public void importAccessContractsWithNullStreamThrowIllegalArgException()
        throws FileNotFoundException, InvalidParseOperationException, AccessExternalClientException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        client.importContracts(null, TENANT_ID, AdminCollections.ACCESS_CONTRACTS);
    }


    /**
     * Test that findAccessContracts is reachable and does not return elements
     * 
     * @throws FileNotFoundException
     * @throws InvalidParseOperationException
     * @throws VitamClientInternalException
     */
    @Test
    @RunWithCustomExecutor
    public void findAllAccessContractsThenReturnEmpty()
        throws FileNotFoundException, InvalidParseOperationException, VitamClientInternalException,
        AccessExternalClientException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        when(mock.post()).thenReturn(Response.status(Status.OK).entity(new RequestResponseOK<>()).build());
        RequestResponse resp =
            client.findDocuments(AdminCollections.ACCESS_CONTRACTS, JsonHandler.createObjectNode(), TENANT_ID);
        assertThat(resp).isInstanceOf(RequestResponseOK.class);
        assertThat(((RequestResponseOK) resp).getResults()).hasSize(0);
    }


    /**
     * Test that findAccessContracts is reachable and return two elements as expected
     * 
     * @throws FileNotFoundException
     * @throws InvalidParseOperationException
     * @throws VitamClientInternalException
     */
    @Test
    @RunWithCustomExecutor
    public void findAllAccessContractsThenReturnTwo()
        throws FileNotFoundException, InvalidParseOperationException, VitamClientInternalException,
        AccessExternalClientException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        when(mock.post()).thenReturn(
            Response.status(Status.OK).entity(new RequestResponseOK<>().addAllResults(getAccessContracts())).build());
        RequestResponse resp =
            client.findDocuments(AdminCollections.ACCESS_CONTRACTS, JsonHandler.createObjectNode(), TENANT_ID);
        assertThat(resp).isInstanceOf(RequestResponseOK.class);
        assertThat(((RequestResponseOK) resp).getResults()).hasSize(2);
    }

    @Test
    @RunWithCustomExecutor
    public void updateAccessContract()
        throws FileNotFoundException, InvalidParseOperationException, AccessExternalClientException {
        importContractsWithCorrectJsonReturnCreated();
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.post()).thenReturn(
            Response.status(Status.OK).entity(new RequestResponseOK<>().addAllResults(getAccessContracts())).build());
        
        RequestResponse resp =
            client.findDocuments(AdminCollections.ACCESS_CONTRACTS, JsonHandler.createObjectNode(), TENANT_ID);
        assertThat(resp).isInstanceOf(RequestResponseOK.class);
        assertThat(((RequestResponseOK) resp).getResults()).hasSize(2);
    }



    /**
     * Test that findAccessContractsByID is reachable
     * 
     * @throws FileNotFoundException
     * @throws InvalidParseOperationException
     * @throws VitamClientInternalException
     */
    @Test
    @RunWithCustomExecutor
    public void findAccessContractsByIdThenReturnEmpty()
        throws FileNotFoundException, InvalidParseOperationException, VitamClientInternalException,
        AccessExternalClientException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        when(mock.get()).thenReturn(Response.status(Status.OK).entity(new RequestResponseOK<>()).build());
        RequestResponse resp = client.findDocumentById(AdminCollections.ACCESS_CONTRACTS, "fakeId", TENANT_ID);
        assertThat(resp).isInstanceOf(RequestResponseOK.class);
        assertThat(((RequestResponseOK) resp).getResults()).hasSize(0);
    }




    @Test
    @RunWithCustomExecutor
    public void createProfilesWithCorrectJsonReturnCreated()
        throws FileNotFoundException, InvalidParseOperationException, AccessExternalClientException {
        when(mock.post()).thenReturn(
            Response.status(Status.CREATED).entity(new RequestResponseOK<>().addAllResults(getAccessContracts()))
                .build());
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        InputStream fileProfiles = PropertiesUtils.getResourceAsStream("contracts_access_ok.json");
        RequestResponse resp = client.createProfiles(fileProfiles, TENANT_ID);
        Assert.assertTrue(RequestResponseOK.class.isAssignableFrom(resp.getClass()));
        Assert.assertTrue((((RequestResponseOK) resp).isOk()));
    }

    private List<Object> getProfiles() throws FileNotFoundException, InvalidParseOperationException {
        InputStream fileProfiles = PropertiesUtils.getResourceAsStream("profiles_ok.json");
        ArrayNode array = (ArrayNode) JsonHandler.getFromInputStream(fileProfiles);
        List<Object> res = new ArrayList<>();
        array.forEach(e -> res.add(e));
        return res;
    }

    @Test()
    @RunWithCustomExecutor
    public void createProfilesWithIncorrectJsonReturnBadRequest()
        throws FileNotFoundException, InvalidParseOperationException, AccessExternalClientException {
        VitamError error = new VitamError("vitam_code").setHttpCode(400).setContext("ADMIN").setState("INVALID")
            .setMessage("invalid input").setDescription("Input file of profiles is malformed");
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).entity(error).build());
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        RequestResponse resp =
            client.createProfiles(new FakeInputStream(0), TENANT_ID);
        Assert.assertTrue(VitamError.class.isAssignableFrom(resp.getClass()));
        Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), (((VitamError) resp).getHttpCode()));
    }

    @Test(expected = IllegalArgumentException.class)
    @RunWithCustomExecutor
    public void createProfilesWithNullStreamThrowIllegalArgException()
        throws FileNotFoundException, InvalidParseOperationException, AccessExternalClientException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        client.createProfiles(null, TENANT_ID);
    }




    @Test
    @RunWithCustomExecutor
    public void importProfileFileXSDReturnCreated()
        throws FileNotFoundException, InvalidParseOperationException, AccessExternalClientException {
        when(mock.put()).thenReturn(
            Response.status(Status.CREATED).entity(new RequestResponseOK<>())
                .build());
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        RequestResponse resp = client.importProfileFile("FakeIdXSD", new FakeInputStream(0), TENANT_ID);
        Assert.assertTrue(RequestResponseOK.class.isAssignableFrom(resp.getClass()));
        Assert.assertTrue((((RequestResponseOK) resp).isOk()));
    }

    @Test
    @RunWithCustomExecutor
    public void importProfileFileRNGReturnCreated()
        throws FileNotFoundException, InvalidParseOperationException, AccessExternalClientException {
        when(mock.put()).thenReturn(
            Response.status(Status.CREATED).entity(new RequestResponseOK<>())
                .build());
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        RequestResponse resp = client.importProfileFile("FakeIdRNG", new FakeInputStream(0), TENANT_ID);
        Assert.assertTrue(RequestResponseOK.class.isAssignableFrom(resp.getClass()));
        Assert.assertTrue((((RequestResponseOK) resp).isOk()));
    }



    @Test
    @RunWithCustomExecutor
    public void givenProfileIdWhenDownloadProfileFileThenOK() throws Exception {

        when(mock.get()).thenReturn(ClientMockResultHelper.getObjectStream());

        Response response = client.downloadProfileFile("OP_ID", TENANT_ID);
        assertNotNull(response);
    }

    /**
     * Test that findAccessContracts is reachable and does not return elements
     *
     * @throws FileNotFoundException
     * @throws InvalidParseOperationException
     * @throws VitamClientInternalException
     */
    @Test
    @RunWithCustomExecutor
    public void findAllProfilesThenReturnEmpty()
        throws FileNotFoundException, InvalidParseOperationException, VitamClientInternalException,
        AccessExternalClientException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        when(mock.post()).thenReturn(Response.status(Status.OK).entity(new RequestResponseOK<>()).build());
        RequestResponse resp =
            client.findDocuments(AdminCollections.PROFILE, JsonHandler.createObjectNode(), TENANT_ID);
        assertThat(resp).isInstanceOf(RequestResponseOK.class);
        assertThat(((RequestResponseOK) resp).getResults()).hasSize(0);
    }


    /**
     * Test that findAccessContracts is reachable and return two elements as expected
     *
     * @throws FileNotFoundException
     * @throws InvalidParseOperationException
     * @throws VitamClientInternalException
     */
    @Test
    @RunWithCustomExecutor
    public void findAllProfilesThenReturnOne()
        throws FileNotFoundException, InvalidParseOperationException, VitamClientInternalException,
        AccessExternalClientException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        when(mock.post()).thenReturn(
            Response.status(Status.OK).entity(new RequestResponseOK<>().addAllResults(getProfiles())).build());
        RequestResponse resp =
            client.findDocuments(AdminCollections.PROFILE, JsonHandler.createObjectNode(), TENANT_ID);
        assertThat(resp).isInstanceOf(RequestResponseOK.class);
        assertThat(((RequestResponseOK) resp).getResults()).hasSize(1);
    }

    /**
     *
     * @throws FileNotFoundException
     * @throws InvalidParseOperationException
     * @throws VitamClientInternalException
     */
    @Test
    @RunWithCustomExecutor
    public void findProfilesByIdThenReturnEmpty()
        throws FileNotFoundException, InvalidParseOperationException, VitamClientInternalException,
        AccessExternalClientException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        when(mock.get()).thenReturn(Response.status(Status.OK).entity(new RequestResponseOK<>()).build());
        RequestResponse resp = client.findDocumentById(AdminCollections.PROFILE, "fakeId", TENANT_ID);
        assertThat(resp).isInstanceOf(RequestResponseOK.class);
        assertThat(((RequestResponseOK) resp).getResults()).hasSize(0);
    }

}
