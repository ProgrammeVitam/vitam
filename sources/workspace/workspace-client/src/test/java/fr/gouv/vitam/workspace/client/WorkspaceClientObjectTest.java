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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.server.application.junit.ResteasyTestApplication;
import fr.gouv.vitam.common.serverv2.VitamServerTestRunner;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class WorkspaceClientObjectTest extends ResteasyTestApplication {

    protected static WorkspaceClient client;
    private static final ExpectedResults mock = mock(ExpectedResults.class);

    private static final String CONTAINER_NAME = "myContainer";
    private static final String OBJECT_NAME = "myObject";
    private static final String FOLDER_SIP = "SIP";

    public static final String X_DIGEST_ALGORITHM = "X-digest-algorithm";
    public static final String X_DIGEST = "X-digest";
    private static final DigestType ALGO = DigestType.MD5;
    private static final String MESSAGE_DIGEST = "DigestHex";

    static WorkspaceClientFactory factory = WorkspaceClientFactory.getInstance();

    public static VitamServerTestRunner vitamServerTestRunner = new VitamServerTestRunner(
        WorkspaceClientObjectTest.class,
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

    private InputStream stream = null;

    @Override
    public Set<Object> getResources() {
        return Sets.newHashSet(new MockObjectResource(mock));
    }

    @Path("workspace/v1/containers")
    public static class MockObjectResource {

        private final ExpectedResults mock;

        public MockObjectResource(ExpectedResults mock) {
            this.mock = mock;
        }

        @Path("{containerName}/objects/{objectName}")
        @POST
        @Consumes(MediaType.APPLICATION_OCTET_STREAM)
        @Produces(MediaType.APPLICATION_JSON)
        public Response create(
            InputStream stream,
            @PathParam("containerName") String containerName,
            @PathParam("objectName") String objectName
        ) {
            return mock.post();
        }

        @DELETE
        @Path("{containerName}/objects/{objectName}")
        public Response delete(
            @PathParam("containerName") String containerName,
            @PathParam("objectName") String objectName
        ) {
            return mock.delete();
        }

        @HEAD
        @Path("{containerName}/objects/{objectName}")
        public Response isExistingObject(
            @PathParam("containerName") String containerName,
            @PathParam("objectName") String objectName,
            @HeaderParam(X_DIGEST_ALGORITHM) String algo
        ) {
            return mock.head();
        }

        @Path("{containerName}/objects/{objectName}")
        @GET
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        public Response get(
            @PathParam("containerName") String containerName,
            @PathParam("objectName") String objectName
        ) {
            return mock.get();
        }

        @Path("{containerName}/objects/{objectName}")
        @GET
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response getObjectInformation(
            @PathParam("containerName") String containerName,
            @PathParam("objectName") String objectName
        ) {
            return mock.get();
        }

        @Path("{containerName}/folders/{folderName}")
        @PUT
        @Consumes({ MediaType.APPLICATION_OCTET_STREAM, "application/zip", "application/gzip" })
        @Produces(MediaType.APPLICATION_JSON)
        public Response uncompressObject(
            InputStream stream,
            @PathParam("containerName") String containerName,
            @PathParam("folderName") String folderName,
            @PathParam("archiveType") String archiveType
        ) {
            return mock.put();
        }

        @HEAD
        @Path("{containerName}")
        public Response isExistingContainer(@PathParam("containerName") String containerName) {
            return mock.headContainer();
        }

        @HEAD
        @Path("{containerName}/folders/{folderName}")
        public Response isExistingFolder(
            @PathParam("containerName") String containerName,
            @PathParam("folderName") String folderName
        ) {
            return mock.headFolder();
        }

        @Path("/{containerName}/objects")
        @GET
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        @Consumes(MediaType.APPLICATION_JSON)
        public Response bulkGetObjects(@PathParam(CONTAINER_NAME) String containerName, List<String> objectURIs) {
            return mock.get();
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
    public void givenServerErrorWhenCreateObjectThenRaiseAnException() throws Exception {
        stream = getInputStream("file1");
        when(mock.post()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        client.putObject(CONTAINER_NAME, OBJECT_NAME, stream);
    }

    @Test
    public void givenObjectNotFoundWhenCreateObjectThenReturnCreated() throws Exception {
        stream = getInputStream("file1");
        when(mock.post()).thenReturn(Response.status(Status.CREATED).build());
        client.putObject(CONTAINER_NAME, OBJECT_NAME, stream);
    }

    // delete
    @Test(expected = IllegalArgumentException.class)
    public void givenNullParamWhenDeleteObjectThenRaiseAnException() throws Exception {
        client.deleteObject(CONTAINER_NAME, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenEmptyParamWhenDeleteObjectThenRaiseAnException() throws Exception {
        client.deleteObject(CONTAINER_NAME, "");
    }

    @Test(expected = ContentAddressableStorageServerException.class)
    public void givenServerErrorWhenDeleteObjectThenRaiseAnException() throws Exception {
        when(mock.delete()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        client.deleteObject(CONTAINER_NAME, OBJECT_NAME);
    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenObjectNotFoundWhenDeleteObjectThenRaiseAnException() throws Exception {
        when(mock.delete()).thenReturn(Response.status(Status.NOT_FOUND).build());
        client.deleteObject(CONTAINER_NAME, OBJECT_NAME);
    }

    @Test
    public void givenObjectAlreadyExistsWhenDeleteObjectThenReturnNotContent() throws Exception {
        when(mock.delete()).thenReturn(Response.status(Status.NO_CONTENT).build());
        client.deleteObject(CONTAINER_NAME, OBJECT_NAME);
    }

    // get
    @Test(expected = IllegalArgumentException.class)
    public void givenNullParamWhenGeObjetctThenRaiseAnException() throws Exception {
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
    }

    // check existence
    @Test(expected = IllegalArgumentException.class)
    public void givenNullParamWhenCheckObjectExistenceThenRaiseAnException()
        throws ContentAddressableStorageServerException {
        client.isExistingObject(CONTAINER_NAME, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenEmptyParamWhenCheckObjectExistenceThenRaiseAnException()
        throws ContentAddressableStorageServerException {
        client.isExistingObject(CONTAINER_NAME, "");
    }

    @Test
    public void givenObjectAlreadyExistsWhenCheckObjectExistenceThenReturnTrue()
        throws ContentAddressableStorageServerException {
        when(mock.head()).thenReturn(Response.status(Status.OK).build());
        assertTrue(client.isExistingObject(CONTAINER_NAME, OBJECT_NAME));
    }

    @Test
    public void givenObjectNotFoundWhenCheckObjectExistenceThenReturnFalse()
        throws ContentAddressableStorageServerException {
        when(mock.head()).thenReturn(Response.status(Status.NOT_FOUND).build());
        assertFalse(client.isExistingObject(CONTAINER_NAME, OBJECT_NAME));
    }

    // compute digest

    @Test(expected = IllegalArgumentException.class)
    public void givenNullParamWhenComputeDigestThenRaiseAnException() throws ContentAddressableStorageException {
        client.computeObjectDigest(CONTAINER_NAME, OBJECT_NAME, null);
    }

    @Test
    public void givenObjectAlreadyExistsWhenComputeDigestThenReturnTrue() throws ContentAddressableStorageException {
        when(mock.head()).thenReturn(Response.status(Status.OK).header(X_DIGEST, MESSAGE_DIGEST).build());
        assertTrue(client.computeObjectDigest(CONTAINER_NAME, OBJECT_NAME, ALGO).equals(MESSAGE_DIGEST));
    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenObjectNotFoundWhenComputeDigestThenRaiseAnException() throws ContentAddressableStorageException {
        when(mock.head()).thenReturn(Response.status(Status.NOT_FOUND).build());
        client.computeObjectDigest(CONTAINER_NAME, OBJECT_NAME, ALGO);
    }

    // Unzip
    @Test(expected = IllegalArgumentException.class)
    public void givenNullParamWhenUnzipSipThenRaiseAnException() throws Exception {
        stream = getInputStream("sip.zip");
        client.uncompressObject(null, FOLDER_SIP, CommonMediaType.ZIP, stream);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenEmptyParamWhenUnzipSipThenRaiseAnException() throws Exception {
        stream = getInputStream("sip.zip");
        client.uncompressObject("", FOLDER_SIP, CommonMediaType.ZIP, stream);
    }

    @Test(expected = ContentAddressableStorageServerException.class)
    public void givenServerErrorWhenExtractObjectThenRaiseAnException() throws Exception {
        stream = getInputStream("sip.zip");
        when(mock.headContainer()).thenReturn(Response.status(Status.OK).build());
        when(mock.headFolder()).thenReturn(Response.status(Status.NOT_FOUND).build());
        when(mock.put()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        client.uncompressObject(CONTAINER_NAME, FOLDER_SIP, CommonMediaType.ZIP, stream);
    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenContainerNotFoundWhenExtractObjectThenRaiseAnException() throws Exception {
        stream = getInputStream("sip.zip");
        when(mock.headContainer()).thenReturn(Response.status(Status.NOT_FOUND).build());
        client.uncompressObject(CONTAINER_NAME, FOLDER_SIP, CommonMediaType.ZIP, stream);
    }

    @Test(expected = ContentAddressableStorageAlreadyExistException.class)
    public void givenFolderAlreadyExistsWhenExtractObjectThenRaiseAnException() throws Exception {
        stream = getInputStream("sip.zip");
        when(mock.headContainer()).thenReturn(Response.status(Status.OK).build());
        when(mock.headFolder()).thenReturn(Response.status(Status.OK).build());
        client.uncompressObject(CONTAINER_NAME, FOLDER_SIP, CommonMediaType.ZIP, stream);
    }

    @Test
    public void givenContainerAlreadyExistsWhenUnzipThenReturnCreated() throws Exception {
        stream = getInputStream("sip.zip");
        when(mock.headContainer()).thenReturn(Response.status(Status.OK).build());
        when(mock.headFolder()).thenReturn(Response.status(Status.NOT_FOUND).build());
        when(mock.put()).thenReturn(Response.status(Status.CREATED).build());
        client.uncompressObject(CONTAINER_NAME, FOLDER_SIP, CommonMediaType.ZIP, stream);
    }

    // get information
    @Test(expected = IllegalArgumentException.class)
    public void givenNullParamWhenGetObjetctInformationThenRaiseAnException() throws Exception {
        client.getObjectInformation(CONTAINER_NAME, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenEmptyParamWhenGetObjectInformationThenRaiseAnException() throws Exception {
        client.getObjectInformation(CONTAINER_NAME, "");
    }

    @Test(expected = ContentAddressableStorageServerException.class)
    public void givenServerErrorWhenGetObjectInformationThenRaiseAnException() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        client.getObjectInformation(CONTAINER_NAME, OBJECT_NAME);
    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenObjectNotFoundWhenGetObjectInformationThenRaiseAnException() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        client.getObjectInformation(CONTAINER_NAME, OBJECT_NAME);
    }

    @Test
    public void givenObjectAlreadyExistsWhenGetObjectInformationThenReturnInformation() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.OK).entity("{\"size\" : \"1024\"}").build());
        final JsonNode jsonInfo = client
            .getObjectInformation(CONTAINER_NAME, OBJECT_NAME)
            .toJsonNode()
            .get("$results")
            .get(0);
        assertNotNull(jsonInfo);
        assertNotNull(jsonInfo.get("size"));
        assertEquals(1024, jsonInfo.get("size").asInt());
    }

    private InputStream getInputStream(String file) throws FileNotFoundException {
        return PropertiesUtils.getResourceAsStream("file1.pdf");
    }

    @Test
    public void givenObjectAlreadyExistsWhenCheckObjectThenOK() throws ContentAddressableStorageException {
        when(mock.head()).thenReturn(Response.status(Status.OK).header(X_DIGEST, MESSAGE_DIGEST).build());
        assertTrue(client.checkObject(CONTAINER_NAME, OBJECT_NAME, MESSAGE_DIGEST, DigestType.MD5));
    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenContainerNotExistingWhenCheckObjectThenNotFound() throws ContentAddressableStorageException {
        when(mock.head()).thenReturn(Response.status(Status.NOT_FOUND).build());
        client.checkObject(CONTAINER_NAME, OBJECT_NAME, MESSAGE_DIGEST, DigestType.MD5);
    }

    @Test(expected = ContentAddressableStorageServerException.class)
    public void givenObjectNotExistingWhenCheckObjectThenOK() throws ContentAddressableStorageException {
        when(mock.head()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        client.checkObject(CONTAINER_NAME, OBJECT_NAME, MESSAGE_DIGEST, DigestType.MD5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenNullParamWhenCheckObjectThenRaiseAnException() throws ContentAddressableStorageException {
        client.checkObject(CONTAINER_NAME, OBJECT_NAME, "fakeDigest", null);
    }

    // get
    @Test(expected = IllegalArgumentException.class)
    public void givenNullParamWhenBulkGetObjectsThenRaiseAnException() throws Exception {
        client.bulkGetObjects(CONTAINER_NAME, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenNullIdParamWhenBulkGetObjectsThenRaiseAnException() throws Exception {
        client.bulkGetObjects(CONTAINER_NAME, Arrays.asList("id1", null));
    }

    @Test(expected = ContentAddressableStorageServerException.class)
    public void givenServerErrorWhenBulkGetObjectsThenRaiseAnException() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        client.bulkGetObjects(CONTAINER_NAME, Arrays.asList("id1", "id2"));
    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenObjectNotFoundWhenBulkGetObjectsThenRaiseAnException() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        client.bulkGetObjects(CONTAINER_NAME, Arrays.asList("id1", "id2"));
    }

    @Test
    public void givenObjectAlreadyExistsWhenBulkGetObjectsThenReturnObject() throws Exception {
        reset(mock);
        when(mock.get()).thenReturn(Response.status(Status.OK).build());
        System.out.println(client.toString());
        client.bulkGetObjects(CONTAINER_NAME, Arrays.asList("id1", "id2"));
    }
}
