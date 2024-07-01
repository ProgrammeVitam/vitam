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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.google.common.collect.Sets;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.server.application.junit.ResteasyTestApplication;
import fr.gouv.vitam.common.serverv2.VitamServerTestRunner;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.api.model.FileParams;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WorkspaceClientFolderTest extends ResteasyTestApplication {

    protected static WorkspaceClient client;

    private static final String CONTAINER_NAME = "myContainer";
    private static final String FOLDER_NAME = "myFolder";
    static WorkspaceClientFactory factory = WorkspaceClientFactory.getInstance();
    private static final ExpectedResults mock = mock(ExpectedResults.class);

    public static VitamServerTestRunner vitamServerTestRunner = new VitamServerTestRunner(
        WorkspaceClientFolderTest.class,
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

    @Override
    public Set<Object> getResources() {
        return Sets.newHashSet(new MockFolderResource(mock));
    }

    @Path("workspace/v1/containers")
    public static class MockFolderResource {

        private final ExpectedResults expectedResponse;

        public MockFolderResource(ExpectedResults expectedResponse) {
            this.expectedResponse = expectedResponse;
        }

        @POST
        @Path("{containerName}/folders/{folderName}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response create(
            @PathParam("containerName") String containerName,
            @PathParam("folderName") String folderName
        ) {
            return expectedResponse.post();
        }

        @DELETE
        @Path("{containerName}/folders/{folderName}")
        public Response delete(
            @PathParam("containerName") String containerName,
            @PathParam("folderName") String folderName
        ) {
            return expectedResponse.delete();
        }

        @HEAD
        @Path("{containerName}/folders/{folderName}")
        public Response isExistingFolder(
            @PathParam("containerName") String containerName,
            @PathParam("folderName") String folderName
        ) {
            return expectedResponse.head();
        }

        @GET
        @Path("{containerName}/folders/{folderName}")
        public Response getListUriDigitalObjectFromFolder(
            @PathParam("containerName") String containerName,
            @PathParam("folderName") String folderName
        ) {
            return expectedResponse.get();
        }

        @GET
        @Path("{containerName}/folders/{folderName}/filesWithParams")
        public Response getFilesWithParamsFromFolder(
            @PathParam("containerName") String containerName,
            @PathParam("folderName") String folderName
        ) {
            return expectedResponse.get();
        }
    }

    // create
    @Test(expected = IllegalArgumentException.class)
    public void givenNullParamWhenCreateFolderThenRaiseAnException() throws Exception {
        client.createFolder(CONTAINER_NAME, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenEmptyParamWhenCreateFolderThenRaiseAnException() throws Exception {
        client.createFolder(CONTAINER_NAME, "");
    }

    @Test(expected = ContentAddressableStorageServerException.class)
    public void givenServerErrorWhenCreateFolderThenRaiseAnException() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        client.createFolder(CONTAINER_NAME, FOLDER_NAME);
    }

    @Test
    public void givenFolderNotFoundWhenCreateFolderThenReturnCreated() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.CREATED).build());
        client.createFolder(CONTAINER_NAME, FOLDER_NAME);
    }

    // delete
    @Test(expected = IllegalArgumentException.class)
    public void givenNullParamWhenDeleteFolderThenRaiseAnException() throws Exception {
        client.deleteFolder(CONTAINER_NAME, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenEmptyParamWhenDeleteFolderThenRaiseAnException() throws Exception {
        client.deleteFolder(CONTAINER_NAME, "");
    }

    @Test(expected = ContentAddressableStorageServerException.class)
    public void givenServerErrorWhenDeleteFolderThenRaiseAnException() throws Exception {
        when(mock.delete()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        client.deleteFolder(CONTAINER_NAME, FOLDER_NAME);
    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenFolderNotFoundWhenDeleteFolderThenRaiseAnException() throws Exception {
        when(mock.delete()).thenReturn(Response.status(Status.NOT_FOUND).build());
        client.deleteFolder(CONTAINER_NAME, FOLDER_NAME);
    }

    @Test
    public void givenFolderAlreadyExistsWhenDeleteFolderThenReturnNotContent() throws Exception {
        when(mock.delete()).thenReturn(Response.status(Status.NO_CONTENT).build());
        client.deleteFolder(CONTAINER_NAME, FOLDER_NAME);
    }

    // check existence
    @Test(expected = IllegalArgumentException.class)
    public void givenNullParamWhenCheckFolderExistenceThenRaiseAnException()
        throws ContentAddressableStorageServerException {
        client.isExistingFolder(CONTAINER_NAME, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenEmptyParamWhenCheckFolderExistenceThenRaiseAnException()
        throws ContentAddressableStorageServerException {
        client.isExistingFolder(CONTAINER_NAME, "");
    }

    @Test
    public void givenFolderAlreadyExistsWhenCheckFolderExistenceThenReturnTrue()
        throws ContentAddressableStorageServerException {
        when(mock.head()).thenReturn(Response.status(Status.OK).build());
        assertTrue(client.isExistingFolder(CONTAINER_NAME, FOLDER_NAME));
    }

    @Test
    public void givenFolderAlreadyExistsWhenCheckFolderExistenceThenReturnFalse()
        throws ContentAddressableStorageServerException {
        when(mock.head()).thenReturn(Response.status(Status.NOT_FOUND).build());
        assertFalse(client.isExistingFolder(CONTAINER_NAME, FOLDER_NAME));
    }

    // get URI list
    @Test(expected = IllegalArgumentException.class)
    public void given_NullParam_When_FindingUriObjects_Then_RaiseAnException()
        throws ContentAddressableStorageServerException {
        client.getListUriDigitalObjectFromFolder(CONTAINER_NAME, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void given_EmptyParam_When_FindingUriObjects_Then_RaiseAnException()
        throws ContentAddressableStorageServerException {
        client.getListUriDigitalObjectFromFolder(CONTAINER_NAME, "");
    }

    @Test
    public void given_FolderAlreadyExists_When_FindingUriObjects_Then_ReturnList()
        throws ContentAddressableStorageServerException, InvalidParseOperationException, InvalidFormatException {
        when(mock.get()).thenReturn(Response.status(Status.OK).entity(Collections.<URI>emptyList()).build());
        final List<URI> uris = JsonHandler.getFromStringAsTypeReference(
            client
                .getListUriDigitalObjectFromFolder(CONTAINER_NAME, FOLDER_NAME)
                .toJsonNode()
                .get("$results")
                .get(0)
                .toString(),
            new TypeReference<List<URI>>() {}
        );
        assertTrue(uris.isEmpty());
    }

    @Test
    public void given_FolderExists_When_FindingUriObjects_Then_ReturnURIList()
        throws ContentAddressableStorageServerException, InvalidParseOperationException, URISyntaxException, InvalidFormatException {
        List<URI> uriListWorkspaceOK = new ArrayList<>();
        uriListWorkspaceOK.add(new URI("content/file1.pdf"));
        uriListWorkspaceOK.add(new URI("content/file2.pdf"));
        when(mock.get()).thenReturn(Response.status(Status.OK).entity(uriListWorkspaceOK).build());
        final List<URI> uris = JsonHandler.getFromStringAsTypeReference(
            client
                .getListUriDigitalObjectFromFolder(CONTAINER_NAME, FOLDER_NAME)
                .toJsonNode()
                .get("$results")
                .get(0)
                .toString(),
            new TypeReference<List<URI>>() {}
        );
        assertTrue(!uris.isEmpty());
        for (final URI uriWorkspace : uris) {
            assertTrue(uriWorkspace.toString().contains("content/"));
        }
    }

    @Test
    public void given_FolderExists_When_FindingFilesWithParams_Then_ReturnFilesMap()
        throws ContentAddressableStorageServerException, InvalidParseOperationException, InvalidFormatException {
        Map<String, FileParams> fileParamsMapResults = new HashMap<>();
        fileParamsMapResults.put("content/file1.pdf", new FileParams(256L));
        fileParamsMapResults.put("content/file2.pdf", new FileParams(512L));
        when(mock.get()).thenReturn(Response.status(Status.OK).entity(fileParamsMapResults).build());
        final Map<String, FileParams> fileParamsMap = JsonHandler.getFromStringAsTypeReference(
            client
                .getFilesWithParamsFromFolder(CONTAINER_NAME, FOLDER_NAME)
                .toJsonNode()
                .get("$results")
                .get(0)
                .toString(),
            new TypeReference<>() {}
        );
        assertTrue(!fileParamsMap.isEmpty());
    }
}
