/**
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
 */
package fr.gouv.vitam.workspace.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;

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

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

public class WorkspaceClientObjectTest extends WorkspaceClientTest {

    private static final String CONTAINER_NAME = "myContainer";
    private static final String OBJECT_NAME = "myObject";
    private static final String FOLDER_SIP = "SIP";
    
    public static final String X_DIGEST_ALGORITHM  = "X-digest-algorithm";
    public static final String X_DIGEST="X-digest";
    private static final DigestType ALGO = DigestType.MD5;
    private static final String MESSAGE_DIGEST = "DigestHex";
    
    private InputStream stream = null;

    @Override
    protected Application configure() {
        //set(TestProperties.LOG_TRAFFIC, true);
        set(TestProperties.DUMP_ENTITY, true);
        forceSet(TestProperties.CONTAINER_PORT, String.valueOf(port));

        final ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(JacksonFeature.class);
        resourceConfig.register(MultiPartFeature.class);
        mock = mock(ExpectedResults.class);
        resourceConfig.registerInstances(new MockObjectResource(mock));
        return resourceConfig;
    }

    @Path("workspace/v1/containers")
    public static class MockObjectResource {

        private final ExpectedResults expectedResponse;

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
        public Response isExistingObject(@PathParam("containerName") String containerName,
            @PathParam("objectName") String objectName, @HeaderParam(X_DIGEST_ALGORITHM) String algo) {
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
        
        @Path("{containerName}/objects/{objectName}")
        @GET
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response getObjectInformation(@PathParam("containerName") String containerName,
            @PathParam("objectName") String objectName) {
            return expectedResponse.get();
        }

        @Path("{containerName}/folders/{folderName}")
        @PUT
        @Consumes(MediaType.APPLICATION_OCTET_STREAM)
        @Produces(MediaType.APPLICATION_JSON)
        public Response unzipObject(InputStream stream,
            @PathParam("containerName") String containerName,
            @PathParam("folderName") String folderName) {
            return expectedResponse.put();
        }
        
        @HEAD
        @Path("{containerName}")
        public Response isExistingContainer(@PathParam("containerName") String containerName) {
            return expectedResponse.headContainer();
        }
        
        @HEAD
        @Path("{containerName}/folders/{folderName}")
        public Response isExistingFolder(@PathParam("containerName") String containerName,
            @PathParam("folderName") String folderName) {

            return expectedResponse.headFolder();
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
        assertTrue(true);
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
        assertTrue(true);
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
        assertTrue(true);
    }

    // check existence
    @Test(expected = IllegalArgumentException.class)
    public void givenNullParamWhenCheckObjectExistenceThenRaiseAnException() {
        client.isExistingObject(CONTAINER_NAME, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenEmptyParamWhenCheckObjectExistenceThenRaiseAnException() {
        client.isExistingObject(CONTAINER_NAME, "");
    }

    @Test
    public void givenObjectAlreadyExistsWhenCheckObjectExistenceThenReturnTrue() {
        when(mock.head()).thenReturn(Response.status(Status.OK).build());
        assertTrue(client.isExistingObject(CONTAINER_NAME, OBJECT_NAME));
    }

    @Test
    public void givenObjectNotFoundWhenCheckObjectExistenceThenReturnFalse() {
        when(mock.head()).thenReturn(Response.status(Status.NOT_FOUND).build());
        assertFalse(client.isExistingObject(CONTAINER_NAME, OBJECT_NAME));
    }
    
    //compute digest
    
    @Test(expected = IllegalArgumentException.class)
    public void givenNullParamWhenComputeDigestThenRaiseAnException() throws ContentAddressableStorageNotFoundException, ContentAddressableStorageException {
        client.computeObjectDigest(CONTAINER_NAME, OBJECT_NAME, null);
    }

    @Test
    public void givenObjectAlreadyExistsWhenComputeDigestThenReturnTrue() throws ContentAddressableStorageNotFoundException, ContentAddressableStorageException {
        when(mock.head()).thenReturn(Response.status(Status.OK).header(X_DIGEST, MESSAGE_DIGEST).build());
        assertTrue(client.computeObjectDigest(CONTAINER_NAME, OBJECT_NAME, ALGO).equals(MESSAGE_DIGEST));
    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenObjectNotFoundWhenComputeDigestThenRaiseAnException() throws ContentAddressableStorageNotFoundException, ContentAddressableStorageException {
        when(mock.head()).thenReturn(Response.status(Status.NOT_FOUND).build());
        client.computeObjectDigest(CONTAINER_NAME, OBJECT_NAME, ALGO);
    }

    // Unzip
    @Test(expected = IllegalArgumentException.class)
    public void givenNullParamWhenUnzipSipThenRaiseAnException() throws Exception {
        stream = getInputStream("sip.zip");
        client.unzipObject(null, FOLDER_SIP,stream);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenEmptyParamWhenUnzipSipThenRaiseAnException() throws Exception {
        stream = getInputStream("sip.zip");
        client.unzipObject("",FOLDER_SIP, stream);
    }

    @Test(expected = ContentAddressableStorageServerException.class)
    public void givenServerErrorWhenExtractObjectThenRaiseAnException() throws Exception {
        stream = getInputStream("sip.zip");
        when(mock.headContainer()).thenReturn(Response.status(Status.OK).build());
        when(mock.headFolder()).thenReturn(Response.status(Status.NOT_FOUND).build());
        when(mock.put()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        client.unzipObject(CONTAINER_NAME,FOLDER_SIP, stream);
    }
    
    
    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenContainerNotFoundWhenExtractObjectThenRaiseAnException() throws Exception {
        stream = getInputStream("sip.zip");
        when(mock.headContainer()).thenReturn(Response.status(Status.NOT_FOUND).build());
        client.unzipObject(CONTAINER_NAME,FOLDER_SIP, stream);
    }
    
    @Test(expected = ContentAddressableStorageAlreadyExistException.class)
    public void givenFolderAlreadyExistsWhenExtractObjectThenRaiseAnException() throws Exception {
        stream = getInputStream("sip.zip");
        when(mock.headContainer()).thenReturn(Response.status(Status.OK).build());
        when(mock.headFolder()).thenReturn(Response.status(Status.OK).build());
        client.unzipObject(CONTAINER_NAME,FOLDER_SIP, stream);
    }
    
    @Test
    public void givenContainerAlreadyExistsWhenUnzipThenReturnCreated() throws Exception {
        stream = getInputStream("sip.zip");
        when(mock.headContainer()).thenReturn(Response.status(Status.OK).build());
        when(mock.headFolder()).thenReturn(Response.status(Status.NOT_FOUND).build());
        when(mock.put()).thenReturn(Response.status(Status.CREATED).build());
        client.unzipObject(CONTAINER_NAME,FOLDER_SIP, stream);
        assertTrue(true);
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
        JsonNode jsonInfo = client.getObjectInformation(CONTAINER_NAME, OBJECT_NAME);
        assertNotNull(jsonInfo);
        assertNotNull(jsonInfo.get("size"));
        assertEquals(1024, jsonInfo.get("size").asInt());
    }
    
    
    private InputStream getInputStream(String file) {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream("file1.pdf");
    }
}
