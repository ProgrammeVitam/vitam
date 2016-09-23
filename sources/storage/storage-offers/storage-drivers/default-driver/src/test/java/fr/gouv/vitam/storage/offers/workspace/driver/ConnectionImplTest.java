package fr.gouv.vitam.storage.offers.workspace.driver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.storage.driver.exception.StorageDriverException;
import fr.gouv.vitam.storage.driver.model.GetObjectRequest;
import fr.gouv.vitam.storage.driver.model.GetObjectResult;
import fr.gouv.vitam.storage.driver.model.PutObjectRequest;
import fr.gouv.vitam.storage.driver.model.PutObjectResult;
import fr.gouv.vitam.storage.driver.model.StorageCapacityRequest;
import fr.gouv.vitam.storage.driver.model.StorageCapacityResult;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.ObjectInit;

public class ConnectionImplTest extends JerseyTest {

    protected static final String HOSTNAME = "localhost";
    private static JunitHelper junitHelper;
    private static int port;
    private static ConnectionImpl connection;

    protected ExpectedResults mock;

    interface ExpectedResults {
        Response get();

        Response put();

        Response post();
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        junitHelper = new JunitHelper();
        port = junitHelper.findAvailablePort();
        connection = new ConnectionImpl("http://" + HOSTNAME + ":" + port, "driverName");
    }

    @Path("/offer/v1")
    public static class MockResource {
        private final ExpectedResults expectedResponse;

        public MockResource(ExpectedResults expectedResponse) {
            this.expectedResponse = expectedResponse;
        }

        @GET
        @Path("/status")
        @Produces(MediaType.APPLICATION_JSON)
        public Response getStatus() {
            return expectedResponse.get();
        }

        @GET
        @Path("/objects")
        @Produces(MediaType.APPLICATION_JSON)
        public Response getContainerInformation() {
            return expectedResponse.get();
        }

        @POST
        @Path("/objects/{guid:.+}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response postObject(@PathParam("guid") String objectGUID, ObjectInit objectInit) {
            return expectedResponse.post();
        }

        @PUT
        @Path("/objects/{guid:.+}")
        @Consumes(MediaType.APPLICATION_OCTET_STREAM)
        @Produces(MediaType.APPLICATION_JSON)
        public Response putObject(@PathParam("id") String objectId, InputStream input) {
            return expectedResponse.put();
        }

        @GET
        @Path("/objects/{id:.+}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(value = {MediaType.APPLICATION_JSON, MediaType.APPLICATION_OCTET_STREAM})
        public Response getObject(@PathParam("id") String objectId) {
            return expectedResponse.get();
        }
    }

    @AfterClass
    public static void shutdownAfterClass() {
        junitHelper.releasePort(port);
        try {
            connection.close();
        } catch (Exception e) {

        }
    }

    @Override
    protected Application configure() {
        enable(TestProperties.DUMP_ENTITY);
        forceSet(TestProperties.CONTAINER_PORT, Integer.toString(port));
        mock = mock(ExpectedResults.class);
        final ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(JacksonFeature.class);
        return resourceConfig.registerInstances(new MockResource(mock));
    }

    @Test(expected = StorageDriverException.class)
    public void getStatusKO() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        connection.getStatus();
    }

    @Test
    public void getStatusOK() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.OK).build());
        assertNotNull(connection.getServiceUrl());
        assertEquals(Response.Status.OK.getStatusCode(), connection.getStatus().getStatusInfo().getStatusCode());
    }

    @Test(expected = StorageDriverException.class)
    public void putObjectWithoutRequestKO() throws Exception {
        connection.putObject(null);
    }

    @Test(expected = StorageDriverException.class)
    public void putObjectWithEmptyRequestKO() throws Exception {
        connection.putObject(new PutObjectRequest());
    }

    @Test(expected = StorageDriverException.class)
    public void putObjectRequestWithOnlyMissingTenantIdKO() throws Exception {
        PutObjectRequest request = getPutObjectRequest(true, true, true, false, true);
        connection.putObject(request);
    }

    @Test(expected = StorageDriverException.class)
    public void putObjectRequestWithOnlyMissingDataStreamKO() throws Exception {
        PutObjectRequest request = getPutObjectRequest(false, true, true, true, true);
        connection.putObject(request);
    }

    @Test(expected = StorageDriverException.class)
    public void putObjectRequestWithOnlyMissingAlgortihmKO() throws Exception {
        PutObjectRequest request = getPutObjectRequest(true, false, true, true, true);
        connection.putObject(request);
    }

    @Test(expected = StorageDriverException.class)
    public void putObjectRequestWithOnlyMissingGuidKO() throws Exception {
        PutObjectRequest request = getPutObjectRequest(true, true, false, true, true);
        connection.putObject(request);
    }

    @Test(expected = StorageDriverException.class)
    public void putObjectRequestWithOnlyMissingTypeKO() throws Exception {
        PutObjectRequest request = getPutObjectRequest(true, true, true, true, false);
        connection.putObject(request);
    }

    @Test
    public void putObjectWithRequestOK() throws Exception {
        PutObjectRequest request = getPutObjectRequest(true, true, true, true, true);
        when(mock.post()).thenReturn(Response.status(Status.CREATED).entity(getPostObjectResult(-1)).build());
        when(mock.put()).thenReturn(Response.status(Status.CREATED).entity(getPutObjectResult(0)).build())
            .thenReturn(Response.status(Status.CREATED).entity(getPutObjectResult(1)).build())
            .thenReturn(Response.status(Status.CREATED).entity(getPutObjectResult(2)).build())
            .thenReturn(Response.status(Status.CREATED).entity(getPutObjectResult(3)).build())
            .thenReturn(Response.status(Status.CREATED).entity(getPutObjectResult(4)).build())
            .thenReturn(Response.status(Status.CREATED).entity(getPutObjectResult(5)).build())
            .thenReturn(Response.status(Status.CREATED).entity(getPutObjectResult(6)).build())
            .thenReturn(Response.status(Status.CREATED).entity(getPutObjectResult(7)).build());
        PutObjectResult result = connection.putObject(request);
        assertNotNull(result);
        assertNotNull(result.getDistantObjectId());
        assertNotNull(result.getDigestHashBase16());
    }

    @Test(expected = StorageDriverException.class)
    public void putObjectWithRequestThrowsInternalServerErrorOnPostKO() throws Exception {
        PutObjectRequest request = getPutObjectRequest(true, true, true, true, true);
        when(mock.post()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        connection.putObject(request);
    }

    @Test(expected = StorageDriverException.class)
    public void putObjectWithRequestThrowsNotFoundErrorOnPutKO() throws Exception {
        PutObjectRequest request = getPutObjectRequest(true, true, true, true, true);
        when(mock.post()).thenReturn(Response.status(Status.CREATED).entity(getPostObjectResult(0)).build());
        when(mock.put()).thenReturn(Response.status(Status.NOT_FOUND).build());
        connection.putObject(request);
    }

    @Test(expected = StorageDriverException.class)
    public void putObjectWithRequestThrowsOtherErrorOnPutKO() throws Exception {
        PutObjectRequest request = getPutObjectRequest(true, true, true, true, true);
        when(mock.post()).thenReturn(Response.status(Status.CREATED).entity(getPostObjectResult(0)).build());
        when(mock.put()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        connection.putObject(request);
    }

    @Test(expected = StorageDriverException.class)
    public void putObjectWithRequestThrowsInternalServerErrorOnPutOK() throws Exception {
        PutObjectRequest request = getPutObjectRequest(true, true, true, true, true);
        when(mock.post()).thenReturn(Response.status(Status.CREATED).entity(getPostObjectResult(0)).build());
        when(mock.put()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        connection.putObject(request);
    }

    @Test(expected = StorageDriverException.class)
    public void putObjectThrowsInternalServerException() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        PutObjectRequest request = getPutObjectRequest(true, true, true, true, true);
        connection.putObject(request);
    }

    @Test(expected = StorageDriverException.class)
    public void putObjectThrowsOtherException() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.SERVICE_UNAVAILABLE).build());
        PutObjectRequest request = getPutObjectRequest(true, true, true, true, true);
        connection.putObject(request);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void putObjectNotImplemented() throws Exception {
        connection.putObject(null, null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void removeObjectNotImplemented() throws Exception {
        connection.removeObject(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getGetObjectRequestIllegalArgumentException() throws Exception {
        connection.getObject(null);
    }


    @Test
    public void getStorageCapacityOK() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.OK).entity(getStorageCapacityResult()).build());
        StorageCapacityRequest request = new StorageCapacityRequest();
        request.setTenantId("0");
        StorageCapacityResult result = connection.getStorageCapacity(request);
        assertNotNull(result);
        assertNotNull(result.getUsableSpace());
        assertNotNull(result.getUsedSpace());
    }

    @Test(expected = StorageDriverException.class)
    public void getStorageCapacityException() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        StorageCapacityRequest request = new StorageCapacityRequest();
        request.setTenantId("0");
        connection.getStorageCapacity(request);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getGetObjectGUIDIllegalArgumentException() throws Exception {
        GetObjectRequest request = new GetObjectRequest();
        request.setTenantId("0");
        request.setFolder(DataCategory.OBJECT.getFolder());
        connection.getObject(request);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getGetObjectTenantIdIllegalArgumentException() throws Exception {
        GetObjectRequest request = new GetObjectRequest();
        request.setGuid("guid");
        request.setFolder(DataCategory.OBJECT.getFolder());
        connection.getObject(request);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getGetObjectTypeIllegalArgumentException() throws Exception {
        GetObjectRequest request = new GetObjectRequest();
        request.setGuid("guid");
        request.setTenantId("0");
        connection.getObject(request);
    }

    @Test
    public void getObjectNotFound() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        GetObjectRequest request = new GetObjectRequest();
        request.setTenantId("0");
        request.setGuid("guid");
        request.setFolder(DataCategory.OBJECT.getFolder());
        try {
            connection.getObject(request);
            fail("Expected exception");
        } catch (StorageDriverException exc) {
            assertEquals(exc.getErrorCode(), StorageDriverException.ErrorCode.NOT_FOUND);
        }
    }

    @Test
    public void getObjectInternalError() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        GetObjectRequest request = new GetObjectRequest();
        request.setTenantId("0");
        request.setGuid("guid");
        request.setFolder(DataCategory.OBJECT.getFolder());
        try {
            connection.getObject(request);
            fail("Expected exception");
        } catch (StorageDriverException exc) {
            assertEquals(StorageDriverException.ErrorCode.INTERNAL_SERVER_ERROR, exc.getErrorCode());
        }
    }

    @Test
    public void getObjectPreconditionFailed() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        GetObjectRequest request = new GetObjectRequest();
        request.setTenantId("0");
        request.setGuid("guid");
        request.setFolder(DataCategory.OBJECT.getFolder());
        try {
            connection.getObject(request);
            fail("Expected exception");
        } catch (StorageDriverException exc) {
            assertEquals(StorageDriverException.ErrorCode.PRECONDITION_FAILED, exc.getErrorCode());
        }
    }

    @Test
    public void getObjectOK() throws Exception {
        InputStream stream = new ByteArrayInputStream("Test".getBytes());
        when(mock.get()).thenReturn(Response.status(Status.OK).entity(stream).build());
        GetObjectRequest request = new GetObjectRequest();
        request.setTenantId("0");
        request.setGuid("guid");
        request.setFolder(DataCategory.OBJECT.getFolder());
        GetObjectResult result = connection.getObject(request);
        assertNotNull(result);
    }

    private PutObjectRequest getPutObjectRequest(boolean dataS, boolean digestA, boolean guid, boolean tenantId,
        boolean type)
        throws Exception {
        PutObjectRequest request = new PutObjectRequest();
        if (dataS) {
            request
                .setDataStream(new FileInputStream(PropertiesUtils.findFile("digitalObject.pdf")));
        }
        if (digestA) {
            request.setDigestAlgorithm(DigestType.MD5.getName());
        }
        if (guid) {
            request.setGuid("GUID");
        }
        if (tenantId) {
            request.setTenantId("0");
        }
        if (type) {
            request.setType(DataCategory.OBJECT.name());
        }

        return request;
    }

    private ObjectInit getPostObjectResult(int uniqueId) {
        ObjectInit object = new ObjectInit();
        object.setId("" + uniqueId);
        object.setDigestAlgorithm(DigestType.SHA256);
        object.setSize(1024);
        object.setType(DataCategory.OBJECT);
        return object;
    }

    private JsonNode getPutObjectResult(int uniqueId) throws JsonProcessingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode actualObj = mapper.readTree("{\"digest\":\"aaakkkk" + uniqueId + "\"}");
        return actualObj;
    }

    private JsonNode getStorageCapacityResult() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode result = mapper.readTree("{\"usableSpace\":\"100000\"," +
            "\"usedSpace\":\"100000\"}");
        return result;
    }

}
