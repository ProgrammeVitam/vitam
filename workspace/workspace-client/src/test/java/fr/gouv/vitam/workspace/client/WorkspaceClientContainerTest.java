package fr.gouv.vitam.workspace.client;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.TestProperties;
import org.junit.Test;

import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.common.Entry;


public class WorkspaceClientContainerTest extends WorkspaceClientTest {
    
    private static final String CONTAINER_NAME = "myContainer";

    @Override
    protected Application configure() {
        set(TestProperties.LOG_TRAFFIC, true);
        set(TestProperties.DUMP_ENTITY, true);
        forceSet(TestProperties.CONTAINER_PORT, String.valueOf(PORT));

        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(JacksonFeature.class);
        mock = mock(ExpectedResults.class);
        resourceConfig.registerInstances(new MockContainerResource(mock));
        return resourceConfig;
    }

    @Path("workspace/v1/containers")
    public static class MockContainerResource {

        private ExpectedResults expectedResponse;

        public MockContainerResource(ExpectedResults expectedResponse) {
            this.expectedResponse = expectedResponse;
        }

        @POST
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response create(Entry container) {
            return expectedResponse.post();
        }

        @DELETE
        @Path("{containerName}")
        public Response delete(@PathParam("containerName") String containerName) {
            return expectedResponse.delete();
        }

        @HEAD
        @Path("{containerName}")
        public Response containerExists(@PathParam("containerName") String containerName) {
            return expectedResponse.head();
        }
    }

    // create
    @Test(expected = IllegalArgumentException.class)
    public void givenNullParamWhenCreateContainerThenRaiseAnException() {
        client.createContainer(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenEmptyParamWhenCreateContainerThenRaiseAnException() {
        client.createContainer("");
    }

    @Test(expected = ContentAddressableStorageServerException.class)
    public void givenServerErrorWhenCreateContainerThenRaiseAnException() {
        when(mock.post()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        client.createContainer(CONTAINER_NAME);
    }

    @Test(expected = ContentAddressableStorageAlreadyExistException.class)
    public void givenContainerAlreadyExistsWhenCreateContainerThenRaiseAnException() {
        when(mock.post()).thenReturn(Response.status(Status.CONFLICT).build());
        client.createContainer(CONTAINER_NAME);
    }

    @Test
    public void givenContainerNotFoundWhenCreateContainerThenReturnCreated() {
        when(mock.post()).thenReturn(Response.status(Status.CREATED).build());
        client.createContainer(CONTAINER_NAME);
        assertTrue(true);
    }

    // delete
    @Test(expected = IllegalArgumentException.class)
    public void givenNullParamWhenDeleteContainerThenRaiseAnException() {
        client.deleteContainer(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenEmptyParamWhenDeleteContainerThenRaiseAnException() {
        client.deleteContainer("");
    }

    @Test(expected = ContentAddressableStorageServerException.class)
    public void givenServerErrorWhenDeleteContainerThenRaiseAnException() {
        when(mock.delete()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        client.deleteContainer(CONTAINER_NAME);
    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenContainerNotFoundWhenDeleteContainerThenRaiseAnException() {
        when(mock.delete()).thenReturn(Response.status(Status.NOT_FOUND).build());
        client.deleteContainer(CONTAINER_NAME);
    }

    @Test
    public void givenContainerAlreadyExistsWhenDeleteContainerThenReturnNotContent() {
        when(mock.delete()).thenReturn(Response.status(Status.NO_CONTENT).build());
        client.deleteContainer(CONTAINER_NAME);
        assertTrue(true);
    }

    // check existence
    @Test(expected = IllegalArgumentException.class)
    public void givenNullParamWhenCheckContainerExistenceThenRaiseAnException() {
        client.containerExists(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenEmptyParamWhenCheckContainerExistenceThenRaiseAnException() {
        client.containerExists("");
    }

    @Test
    public void givenContainerAlreadyExistsWhenCheckContainerExistenceThenReturnTrue() {
        when(mock.head()).thenReturn(Response.status(Status.OK).build());
        assertTrue(client.containerExists(CONTAINER_NAME));
    }

    @Test
    public void givenContainerAlreadyExistsWhenCheckContainerExistenceThenReturnFalse() {
        when(mock.head()).thenReturn(Response.status(Status.NOT_FOUND).build());
        assertFalse(client.containerExists(CONTAINER_NAME));
    }

}