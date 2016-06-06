package fr.gouv.vitam.workspace.client;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
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
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.TestProperties;
import org.junit.Test;

import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

public class WorkspaceClientObjectTest extends WorkspaceClientTest {

    private static final String CONTAINER_NAME = "myContainer";
    private static final String OBJECT_NAME = "myObject";
    private InputStream stream = null;

    @Override
    protected Application configure() {
        set(TestProperties.LOG_TRAFFIC, true);
        set(TestProperties.DUMP_ENTITY, true);
        forceSet(TestProperties.CONTAINER_PORT, String.valueOf(PORT));

        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(JacksonFeature.class);
        resourceConfig.register(MultiPartFeature.class);
        mock = mock(ExpectedResults.class);
        resourceConfig.registerInstances(new MockObjectResource(mock));
        return resourceConfig;
    }

    @Path("workspace/v1/containers")
    public static class MockObjectResource {

        private ExpectedResults expectedResponse;

        public MockObjectResource(ExpectedResults expectedResponse) {
            this.expectedResponse = expectedResponse;
        }

        @Path("{containerName}/objects")
        @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        @Produces(MediaType.APPLICATION_JSON)
        public Response create(@FormDataParam("object") InputStream stream,
            @FormDataParam("object") FormDataContentDisposition header,
            @FormDataParam("objectName") String objectName, @PathParam("containerName") String containerName) {
            return expectedResponse.post();
        }

        @DELETE
        @Path("{containerName}/objects/{objectName}")
        public Response delete(@PathParam("containerName") String containerName,
            @PathParam("objectName") String objectName) {
            return expectedResponse.delete();
        }

        @HEAD
        @Path("{containerName}/objects/{objectName}")
        public Response containerExists(@PathParam("containerName") String containerName,
            @PathParam("objectName") String objectName) {
            return expectedResponse.head();
        }

        @Path("{containerName}/objects/{objectName}")
        @GET
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        public Response get(@PathParam("containerName") String containerName,
            @PathParam("objectName") String objectName) {
            return expectedResponse.get();
        }

        @Path("{containerName}/objects")
        @PUT
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        @Produces(MediaType.APPLICATION_JSON)
        public Response unzipSipObject(@FormDataParam("object") InputStream stream,
            @FormDataParam("object") FormDataContentDisposition header,
            @PathParam("containerName") String containerName) {
            return expectedResponse.put();
        }

    }

    // create
    @Test(expected = IllegalArgumentException.class)
    public void givenNullParamWhenCreateObjectThenRaiseAnException() throws Exception {
        stream = getInputStream("file1");
        client.putObject(CONTAINER_NAME, null, stream);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenEmptyParamWhenCreateObjectThenRaiseAnException() throws Exception {
        stream = getInputStream("file1");
        client.putObject(CONTAINER_NAME, "", stream);
    }

    @Test(expected = ContentAddressableStorageServerException.class)
    public void givenServerErrorWhenCreateObjectThenRaiseAnException()throws Exception  {
        stream = getInputStream("file1");
        when(mock.post()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        client.putObject(CONTAINER_NAME, OBJECT_NAME, stream);
    }

    @Test
    public void givenObjectNotFoundWhenCreateObjectThenReturnCreated() throws Exception {
        stream = getInputStream("file1");
        when(mock.post()).thenReturn(Response.status(Status.CREATED).build());
        client.putObject(CONTAINER_NAME, OBJECT_NAME, stream);
        assertTrue(true);
    }

    // delete
    @Test(expected = IllegalArgumentException.class)
    public void givenNullParamWhenDeleteObjectThenRaiseAnException() throws Exception {
        client.deleteObject(CONTAINER_NAME, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenEmptyParamWhenDeleteObjectThenRaiseAnException()throws Exception  {
        client.deleteObject(CONTAINER_NAME, "");
    }

    @Test(expected = ContentAddressableStorageServerException.class)
    public void givenServerErrorWhenDeleteObjectThenRaiseAnException()throws Exception  {
        when(mock.delete()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        client.deleteObject(CONTAINER_NAME, OBJECT_NAME);
    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenObjectNotFoundWhenDeleteObjectThenRaiseAnException() throws Exception {
        when(mock.delete()).thenReturn(Response.status(Status.NOT_FOUND).build());
        client.deleteObject(CONTAINER_NAME, OBJECT_NAME);
    }

    @Test
    public void givenObjectAlreadyExistsWhenDeleteObjectThenReturnNotContent()throws Exception  {
        when(mock.delete()).thenReturn(Response.status(Status.NO_CONTENT).build());
        client.deleteObject(CONTAINER_NAME, OBJECT_NAME);
        assertTrue(true);
    }

    // get
    @Test(expected = IllegalArgumentException.class)
    public void givenNullParamWhenGeObjetctThenRaiseAnException()throws Exception  {
        client.getObject(CONTAINER_NAME, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenEmptyParamWhenGetObjectThenRaiseAnException() throws Exception {
        client.getObject(CONTAINER_NAME, "");
    }

    @Test(expected = ContentAddressableStorageServerException.class)
    public void givenServerErrorWhenGetObjectThenRaiseAnException() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        client.getObject(CONTAINER_NAME, OBJECT_NAME);
    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenObjectNotFoundWhenGetObjectThenRaiseAnException() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        client.getObject(CONTAINER_NAME, OBJECT_NAME);
    }

    @Test
    public void givenObjectAlreadyExistsWhenGetObjectThenReturnObject() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.OK).build());
        client.getObject(CONTAINER_NAME, OBJECT_NAME);
        assertTrue(true);
    }

    // check existence
    @Test(expected = IllegalArgumentException.class)
    public void givenNullParamWhenCheckObjectExistenceThenRaiseAnException() {
        client.objectExists(CONTAINER_NAME, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenEmptyParamWhenCheckObjectExistenceThenRaiseAnException() {
        client.objectExists(CONTAINER_NAME, "");
    }

    @Test
    public void givenObjectAlreadyExistsWhenCheckObjectExistenceThenReturnTrue() {
        when(mock.head()).thenReturn(Response.status(Status.OK).build());
        assertTrue(client.objectExists(CONTAINER_NAME, OBJECT_NAME));
    }

    @Test
    public void givenObjectAlreadyExistsWhenCheckObjectExistenceThenReturnFalse() {
        when(mock.head()).thenReturn(Response.status(Status.NOT_FOUND).build());
        assertFalse(client.objectExists(CONTAINER_NAME, OBJECT_NAME));
    }

    @Test(expected = IllegalArgumentException.class)
    public void given_NullParam_When_UnzipSip_Then_RaiseAnException() throws Exception {
        stream = getInputStream("sip.zip");
        client.unzipSipObject(null, stream);
    }

    @Test(expected = IllegalArgumentException.class)
    public void given_EmptyParam_When_UnzipSip_Then_RaiseAnException() throws Exception {
        stream = getInputStream("sip.zip");
        client.unzipSipObject("", stream);
    }

    @Test(expected = ContentAddressableStorageServerException.class)
    public void given_ServerError_When_ExtractSipObject_Then_RaiseAnException()throws Exception  {
        stream = getInputStream("sip.zip");
        when(mock.put()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        client.unzipSipObject(CONTAINER_NAME, stream);
    }

    @Test
    public void given_ObjectNotFound_When_UnzipSip_Then_ReturnCreated() throws Exception {
        stream = getInputStream("sip.zip");
        when(mock.put()).thenReturn(Response.status(Status.CREATED).build());
        client.unzipSipObject(CONTAINER_NAME, stream);
        assertTrue(true);
    }

    private InputStream getInputStream(String file) {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream("file1.pdf");
    }
}