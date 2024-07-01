/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
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
 */
package fr.gouv.vitam.workspace.client;

import com.google.common.collect.Sets;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.server.application.junit.ResteasyTestApplication;
import fr.gouv.vitam.common.serverv2.VitamServerTestRunner;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.api.model.TimeToLive;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.DELETE;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class WorkspaceClientContainerTest extends ResteasyTestApplication {

    protected static WorkspaceClient client;

    private static final String CONTAINER_NAME = "myContainer" + GUIDFactory.newGUID().toString();
    static WorkspaceClientFactory factory = WorkspaceClientFactory.getInstance();

    private static final ExpectedResults mock = mock(ExpectedResults.class);
    public static VitamServerTestRunner vitamServerTestRunner = new VitamServerTestRunner(
        WorkspaceClientContainerTest.class,
        factory
    );

    @BeforeClass
    public static void setUpBeforeClass() throws Throwable {
        vitamServerTestRunner.start();
        client = (WorkspaceClient) vitamServerTestRunner.getClient();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Throwable {
        vitamServerTestRunner.runAfter();
    }

    @Before
    public void before() {
        reset(mock);
    }

    @Override
    public Set<Object> getResources() {
        return Sets.newHashSet(new MockContainerResource(mock));
    }

    @Path("workspace/v1/containers")
    public static class MockContainerResource {

        private final ExpectedResults expectedResponse;

        public MockContainerResource(ExpectedResults expectedResponse) {
            this.expectedResponse = expectedResponse;
        }

        @POST
        @Produces(MediaType.APPLICATION_JSON)
        @Path("{containerName}")
        public Response create(@PathParam("containerName") String containerName) {
            return expectedResponse.post();
        }

        @DELETE
        @Path("{containerName}")
        @Produces(MediaType.APPLICATION_JSON)
        public Response delete(@PathParam("containerName") String containerName) {
            return expectedResponse.delete();
        }

        @HEAD
        @Path("{containerName}")
        @Produces(MediaType.APPLICATION_JSON)
        public Response isExistingContainer(@PathParam("containerName") String containerName) {
            return expectedResponse.head();
        }

        @DELETE
        @Path("{containerName}/old_files")
        @Produces(MediaType.APPLICATION_JSON)
        public Response purgeOldFilesInContainer(
            @PathParam("containerName") String containerName,
            TimeToLive timeToLive
        ) {
            return expectedResponse.delete();
        }
    }

    // create
    @Test(expected = IllegalArgumentException.class)
    public void givenNullParamWhenCreateContainerThenRaiseAnException() throws Exception {
        client.createContainer(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenEmptyParamWhenCreateContainerThenRaiseAnException() throws Exception {
        client.createContainer("");
    }

    @Test(expected = ContentAddressableStorageServerException.class)
    public void givenServerErrorWhenCreateContainerThenRaiseAnException() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        client.createContainer(CONTAINER_NAME);
    }

    @Test
    public void givenContainerNotFoundWhenCreateContainerThenReturnCreated() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.CREATED).build());
        client.createContainer(CONTAINER_NAME);
    }

    // delete
    @Test(expected = IllegalArgumentException.class)
    public void givenNullParamWhenDeleteContainerThenRaiseAnException() throws Exception {
        client.deleteContainer(null, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenEmptyParamWhenDeleteContainerThenRaiseAnException() throws Exception {
        client.deleteContainer("", false);
    }

    @Test(expected = ContentAddressableStorageServerException.class)
    public void givenServerErrorWhenDeleteContainerThenRaiseAnException() throws Exception {
        when(mock.delete()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        client.deleteContainer(CONTAINER_NAME, false);
    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenContainerNotFoundWhenDeleteContainerThenRaiseAnException() throws Exception {
        when(mock.delete()).thenReturn(Response.status(Status.NOT_FOUND).build());
        client.deleteContainer(CONTAINER_NAME, false);
    }

    @Test
    public void givenContainerAlreadyExistsWhenDeleteContainerThenReturnNotContent() throws Exception {
        when(mock.delete()).thenReturn(Response.status(Status.NO_CONTENT).build());
        client.deleteContainer(CONTAINER_NAME, true);
    }

    // check existence
    @Test(expected = IllegalArgumentException.class)
    public void givenNullParamWhenCheckContainerExistenceThenRaiseAnException()
        throws ContentAddressableStorageServerException {
        client.isExistingContainer(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenEmptyParamWhenCheckContainerExistenceThenRaiseAnException()
        throws ContentAddressableStorageServerException {
        client.isExistingContainer("");
    }

    @Test
    public void givenContainerAlreadyExistsWhenCheckContainerExistenceThenReturnTrue()
        throws ContentAddressableStorageServerException {
        when(mock.head()).thenReturn(Response.status(Status.OK).build());
        assertTrue(client.isExistingContainer(CONTAINER_NAME));
    }

    @Test
    public void givenContainerAlreadyExistsWhenCheckContainerExistenceThenReturnFalse()
        throws ContentAddressableStorageServerException {
        when(mock.head()).thenReturn(Response.status(Status.NOT_FOUND).build());
        assertFalse(client.isExistingContainer(CONTAINER_NAME));
    }

    @Test
    public void givenSuccessWhenPurgeOldFilesInContainerThenReturnOk() throws ContentAddressableStorageServerException {
        when(mock.delete()).thenReturn(Response.status(Status.NO_CONTENT).build());
        client.purgeOldFilesInContainer(CONTAINER_NAME, new TimeToLive(10, ChronoUnit.MINUTES));
    }

    @Test(expected = ContentAddressableStorageServerException.class)
    public void givenInternalErrorWhenPurgeOldFilesInContainerThenThrowException()
        throws ContentAddressableStorageServerException {
        when(mock.delete()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        client.purgeOldFilesInContainer(CONTAINER_NAME, new TimeToLive(10, ChronoUnit.MINUTES));
    }
}
