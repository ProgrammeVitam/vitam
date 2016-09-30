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
        connection.putObject(new PutObjectRequest(null, null, null, null, null));
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
        StorageCapacityResult result = connection.getStorageCapacity("0");
        assertNotNull(result);
        assertEquals("0", result.getTenantId());
        assertNotNull(result.getUsableSpace());
        assertNotNull(result.getUsedSpace());
    }

    @Test(expected = StorageDriverException.class)
    public void getStorageCapacityException() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        connection.getStorageCapacity("0");
    }

    @Test(expected = IllegalArgumentException.class)
    public void getGetObjectGUIDIllegalArgumentException() throws Exception {
        GetObjectRequest request = new GetObjectRequest("0", null, DataCategory.OBJECT.getFolder());
        connection.getObject(request);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getGetObjectTypeIllegalArgumentException() throws Exception {
        GetObjectRequest request = new GetObjectRequest(null, "guid", DataCategory.OBJECT.getFolder());
        connection.getObject(request);
    }

    @Test
    public void getObjectNotFound() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        GetObjectRequest request = new GetObjectRequest("0", "guid", DataCategory.OBJECT.getFolder());
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
        GetObjectRequest request = new GetObjectRequest("0", "guid", DataCategory.OBJECT.getFolder());
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
        GetObjectRequest request = new GetObjectRequest("0", "guid", DataCategory.OBJECT.getFolder());
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
        GetObjectRequest request = new GetObjectRequest("0", "guid", DataCategory.OBJECT.getFolder());
        GetObjectResult result = connection.getObject(request);
        assertNotNull(result);
    }

    private PutObjectRequest getPutObjectRequest(boolean putDataS, boolean putDigestA, boolean putGuid,
        boolean putTenantId, boolean putType)
        throws Exception {
        FileInputStream stream = null;
        String digest = null;
        String guid = null;
        String tenantId = null;
        String type = null;

        if (putDataS) {
            stream = new FileInputStream(PropertiesUtils.findFile("digitalObject.pdf"));
        }
        if (putDigestA) {
            digest = DigestType.MD5.getName();
        }
        if (putGuid) {
            guid = "GUID";
        }
        if (putTenantId) {
            tenantId = "0";
        }
        if (putType) {
            type = DataCategory.OBJECT.name();
        }
        return new PutObjectRequest(tenantId, digest, guid, stream, type);
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
        JsonNode result = mapper.readTree("{\"tenantId\":\"0\",\"usableSpace\":\"100000\"," +
            "\"usedSpace\":\"100000\"}");
        return result;
    }

}
