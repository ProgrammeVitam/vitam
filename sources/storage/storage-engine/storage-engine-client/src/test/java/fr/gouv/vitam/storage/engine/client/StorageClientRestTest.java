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
package fr.gouv.vitam.storage.engine.client;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.time.LocalDateTime;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Test;

import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.server.application.junit.VitamJerseyTest;
import fr.gouv.vitam.common.server2.application.AbstractVitamApplication;
import fr.gouv.vitam.common.server2.application.configuration.DefaultVitamApplicationConfiguration;
import fr.gouv.vitam.storage.engine.client.exception.StorageAlreadyExistsClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.request.CreateObjectDescription;
import fr.gouv.vitam.storage.engine.common.model.response.StoredInfoResult;

/**
 * StorageClientRest Test
 */
public class StorageClientRestTest extends VitamJerseyTest {

    protected static final String HOSTNAME = "localhost";
    protected StorageClientRest client;

    // ************************************** //
    // Start of VitamJerseyTest configuration //
    // ************************************** //
    public StorageClientRestTest() {
        super(StorageClientFactory.getInstance());
    }

    // Override the beforeTest if necessary
    @Override
    public void beforeTest() throws VitamApplicationServerException {
        client = (StorageClientRest) getClient();
    }

    // Define the getApplication to return your Application using the correct Configuration
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
    }
    // Define your Configuration class if necessary
    public static class TestVitamApplicationConfiguration extends DefaultVitamApplicationConfiguration {

    }


    @Path("/storage/v1")
    public static class MockResource {
        private final ExpectedResults expectedResponse;
        public static final String APPLICATION_ZIP = "application/zip";

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
        @Path("/")
        public Response getContainer() {
            return expectedResponse.get();
        }

        @POST
        @Path("/objects/{id_object}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response createObject(@PathParam("id_object") String idObject) {
            return expectedResponse.post();
        }

        @POST
        @Path("/units/{id_unit}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response createUnit(@PathParam("id_unit") String idUnit) {
            return expectedResponse.post();
        }

        @POST
        @Path("/logbooks/{id_logbook}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response createLogbook(@PathParam("id_logbook") String idLogbook) {
            return expectedResponse.post();
        }

        @POST
        @Path("/objectgroups/{id_objectgroup}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response createObjectGroup(@PathParam("id_objectgroup") String idObjectGroup) {
            return expectedResponse.post();
        }

        @HEAD
        @Path("/")
        public Response checkContainer() {
            return expectedResponse.head();
        }

        @HEAD
        @Path("/objects/{id_object}")
        public Response checkObject(@PathParam("id_object") String idObject) {
            return expectedResponse.head();
        }

        @HEAD
        @Path("/units/{id_unit}")
        public Response checkUnit(@PathParam("id_unit") String idUnit) {
            return expectedResponse.head();
        }

        @HEAD
        @Path("/logbooks/{id_logbook}")
        public Response checkLogbook(@PathParam("id_logbook") String idLogbook) {
            return expectedResponse.head();
        }

        @HEAD
        @Path("/objectgroups/{id_objectgroup}")
        public Response checkObjectGroup(@PathParam("id_objectgroup") String idObjectGroup) {
            return expectedResponse.head();
        }

        @DELETE
        @Path("/")
        public Response deleteContainer() {
            return expectedResponse.delete();
        }

        @DELETE
        @Path("/objects/{id_object}")
        public Response deleteObject(@PathParam("id_object") String idObject) {
            return expectedResponse.delete();
        }

        @DELETE
        @Path("/units/{id_unit}")
        public Response deleteUnit(@PathParam("id_unit") String idUnit) {
            return expectedResponse.delete();
        }

        @DELETE
        @Path("/logbooks/{id_logbook}")
        public Response deleteLogbook(@PathParam("id_logbook") String idLogbook) {
            return expectedResponse.delete();
        }

        @DELETE
        @Path("/objectgroups/{id_objectgroup}")
        public Response deleteObjectGroup(@PathParam("id_objectgroup") String idObjectGroup) {
            return expectedResponse.delete();
        }

        @Path("/objects/{id_object}")
        @GET
        @Produces({MediaType.APPLICATION_OCTET_STREAM, CommonMediaType.ZIP})
        @Consumes(MediaType.APPLICATION_JSON)
        public Response getObject(@Context HttpHeaders headers, @PathParam("id_object") String objectId) {
            return expectedResponse.get();
        }
    }

    @Test
    public void getContainerInfos() throws Exception {
        when(mock.get()).thenReturn(Response.status(Response.Status.OK).build());
        client.getStorageInformation("idTenant", "idStrategy");
    }

    @Test(expected = IllegalArgumentException.class)
    public void getContainerInfosWithTenantIllegalArgumentException() throws Exception {
        when(mock.get()).thenReturn(Response.status(Response.Status.OK).build());
        client.getStorageInformation("", "idStrategy");
    }

    @Test(expected = IllegalArgumentException.class)
    public void getContainerInfosWithStrategyIllegalArgumentException() throws Exception {
        when(mock.get()).thenReturn(Response.status(Response.Status.OK).build());
        client.getStorageInformation("idTenant", null);
    }

    @Test(expected = StorageNotFoundClientException.class)
    public void getContainerInfosNotFound() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        client.getStorageInformation("idTenant", "idStrategy");
    }

    @Test(expected = StorageServerClientException.class)
    public void getContainerInfosInternalServerError() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        client.getStorageInformation("idTenant", "idStrategy");
    }

    @Test
    public void createFromWorkspaceOK() throws Exception {
        when(mock.post())
            .thenReturn(Response.status(Response.Status.CREATED).entity(generateStoredInfoResult("idObject")).build());
        client.storeFileFromWorkspace("idTenant", "idStrategy", StorageCollectionType.OBJECTS, "idObject",
            getDescription());
    }

    @Test(expected = StorageNotFoundClientException.class)
    public void createFromWorkspaceNotFound() throws Exception {
        when(mock.post()).thenReturn(Response.status(Response.Status.NOT_FOUND).build());
        client.storeFileFromWorkspace("idTenant", "idStrategy", StorageCollectionType.OBJECTS, "idObject",
            getDescription());
    }

    @Test(expected = StorageAlreadyExistsClientException.class)
    public void createFromWorkspaceAlreadyExist() throws Exception {
        when(mock.post()).thenReturn(Response.status(Response.Status.CONFLICT).build());
        client.storeFileFromWorkspace("idTenant", "idStrategy", StorageCollectionType.OBJECTS, "idObject",
            getDescription());
    }

    @Test(expected = StorageServerClientException.class)
    public void createFromWorkspaceInternalServerError() throws Exception {
        when(mock.post()).thenReturn(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
        client.storeFileFromWorkspace("idTenant", "idStrategy", StorageCollectionType.OBJECTS, "idObject",
            getDescription());
    }

    @Test(expected = IllegalArgumentException.class)
    public void createFromWorkspaceWithTenantIllegalArgumentException() throws Exception {
        when(mock.post()).thenReturn(Response.status(Response.Status.CREATED).build());
        client.storeFileFromWorkspace("", "idStrategy", StorageCollectionType.OBJECTS, "idObject", getDescription());
    }

    @Test(expected = IllegalArgumentException.class)
    public void createFromWorkspaceWithStrategyIllegalArgumentException() throws Exception {
        when(mock.post()).thenReturn(Response.status(Response.Status.CREATED).build());
        client.storeFileFromWorkspace("idTenant", null, StorageCollectionType.OBJECTS, "idObject", getDescription());
    }

    @Test(expected = IllegalArgumentException.class)
    public void createFromWorkspaceWithObjectIdIllegalArgumentException() throws Exception {
        when(mock.post()).thenReturn(Response.status(Response.Status.CREATED).build());
        client.storeFileFromWorkspace(null, "idStrategy", StorageCollectionType.OBJECTS, "", getDescription());
    }

    @Test(expected = IllegalArgumentException.class)
    public void createFromWorkspaceWithDecritionIllegalArgumentException() throws Exception {
        when(mock.post()).thenReturn(Response.status(Response.Status.CREATED).build());
        client.storeFileFromWorkspace(null, "idStrategy", StorageCollectionType.OBJECTS, "idObject", null);
    }

    private CreateObjectDescription getDescription() {
        final CreateObjectDescription description = new CreateObjectDescription();
        description.setWorkspaceContainerGUID("aeaaaaaaaaaam7mxaaaamakwfnzbudaaaaaq");
        description.setWorkspaceObjectURI(
            "SIP/content/e726e114f302c871b64569a00acb3a19badb7ee8ce4aef72cc2a043ace4905b8e8fca6f4771f8d6f67e221a53a4bbe170501af318c8f2c026cc8ea60f66fa804.odt");
        return description;
    }

    @Test
    public void existsOK() throws Exception {
        when(mock.head()).thenReturn(Response.status(Response.Status.NO_CONTENT).build());
        assertTrue(client.existsContainer("idTenant", "idStrategy"));
        assertTrue(client.exists("idTenant", "idStrategy", StorageCollectionType.OBJECTS, "idObject"));
        assertTrue(client.exists("idTenant", "idStrategy", StorageCollectionType.UNITS, "idUnits"));
        assertTrue(client.exists("idTenant", "idStrategy", StorageCollectionType.LOGBOOKS, "idLogbooks"));
        assertTrue(client.exists("idTenant", "idStrategy", StorageCollectionType.OBJECTGROUPS, "idObjectGroups"));
    }

    @Test
    public void existsKO() throws Exception {
        when(mock.head()).thenReturn(Response.status(Response.Status.NOT_FOUND).build());
        assertFalse(client.existsContainer("idTenant", "idStrategy"));
        assertFalse(client.exists("idTenant", "idStrategy", StorageCollectionType.OBJECTS, "idObject"));
        assertFalse(client.exists("idTenant", "idStrategy", StorageCollectionType.UNITS, "idUnits"));
        assertFalse(client.exists("idTenant", "idStrategy", StorageCollectionType.LOGBOOKS, "idLogbooks"));
        assertFalse(client.exists("idTenant", "idStrategy", StorageCollectionType.OBJECTGROUPS, "idObjectGroups"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void existsWithTenantIllegalArgumentException() throws Exception {
        when(mock.head()).thenReturn(Response.status(Response.Status.NO_CONTENT).build());
        client.exists("", "idStrategy", StorageCollectionType.OBJECTS, "idObject");
    }

    @Test(expected = IllegalArgumentException.class)
    public void existsWithStrategyIllegalArgumentException() throws Exception {
        when(mock.head()).thenReturn(Response.status(Response.Status.NO_CONTENT).build());
        client.exists("idTenant", "", StorageCollectionType.OBJECTS, "idObject");
    }

    @Test(expected = IllegalArgumentException.class)
    public void existsWorkspaceWithObjectTypeIllegalArgumentException() throws Exception {
        when(mock.head()).thenReturn(Response.status(Response.Status.NO_CONTENT).build());
        client.exists("idTenant", "idStrategy", StorageCollectionType.CONTAINERS, "idTenant");
    }


    @Test(expected = IllegalArgumentException.class)
    public void existsWithObjectIdIllegalArgumentException() throws Exception {
        when(mock.head()).thenReturn(Response.status(Response.Status.NO_CONTENT).build());
        client.exists("idTenant", "idStrategy", StorageCollectionType.OBJECTS, "");
    }

    @Test
    public void existsServerError() {
        when(mock.head()).thenReturn(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
        try {
            client.existsContainer("idTenant", "idStrategy");
            fail("Should rise an exception");
        } catch (final VitamClientException e) {
            // nothing to do
        }

        when(mock.head()).thenReturn(Response.status(Response.Status.PRECONDITION_FAILED).build());
        try {
            client.existsContainer("idTenant", "idStrategy");
            fail("Should rise an exception");
        } catch (final VitamClientException e) {
            // nothing to do
        }
    }

    @Test
    public void deleteOK() throws Exception {
        when(mock.delete()).thenReturn(Response.status(Response.Status.NO_CONTENT).build());
        assertTrue(client.deleteContainer("idTenant", "idStrategy"));
        assertTrue(client.delete("idTenant", "idStrategy", StorageCollectionType.OBJECTS, "idObject"));
        assertTrue(client.delete("idTenant", "idStrategy", StorageCollectionType.UNITS, "idUnits"));
        assertTrue(client.delete("idTenant", "idStrategy", StorageCollectionType.LOGBOOKS, "idLogbooks"));
        assertTrue(client.delete("idTenant", "idStrategy", StorageCollectionType.OBJECTGROUPS, "idObjectGroups"));
    }

    @Test
    public void deleteKO() throws Exception {
        when(mock.delete()).thenReturn(Response.status(Response.Status.NOT_FOUND).build());
        assertFalse(client.deleteContainer("idTenant", "idStrategy"));
        assertFalse(client.delete("idTenant", "idStrategy", StorageCollectionType.OBJECTS, "idObject"));
        assertFalse(client.delete("idTenant", "idStrategy", StorageCollectionType.UNITS, "idUnits"));
        assertFalse(client.delete("idTenant", "idStrategy", StorageCollectionType.LOGBOOKS, "idLogbooks"));
        assertFalse(client.delete("idTenant", "idStrategy", StorageCollectionType.OBJECTGROUPS, "idObjectGroups"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void deleteContainerWithIllegalArgumentException() throws Exception {
        client.delete("idTenant", "idStrategy", StorageCollectionType.CONTAINERS, "guid");
    }

    @Test(expected = IllegalArgumentException.class)
    public void deleteWithTenantIllegalArgumentException() throws Exception {
        when(mock.delete()).thenReturn(Response.status(Response.Status.NO_CONTENT).build());
        client.exists("", "idStrategy", StorageCollectionType.OBJECTS, "idObject");
    }

    @Test(expected = IllegalArgumentException.class)
    public void deleteWithStrategyIllegalArgumentException() throws Exception {
        when(mock.delete()).thenReturn(Response.status(Response.Status.NO_CONTENT).build());
        client.exists("idTenant", "", StorageCollectionType.OBJECTS, "idObject");
    }

    @Test(expected = IllegalArgumentException.class)
    public void deleteWorkspaceWithObjectTypeIllegalArgumentException() throws Exception {
        when(mock.delete()).thenReturn(Response.status(Response.Status.NO_CONTENT).build());
        client.exists("idTenant", "idStrategy", StorageCollectionType.CONTAINERS, "idTenant");
    }


    @Test(expected = IllegalArgumentException.class)
    public void deleteWithObjectIdIllegalArgumentException() throws Exception {
        when(mock.delete()).thenReturn(Response.status(Response.Status.NO_CONTENT).build());
        client.exists("idTenant", "idStrategy", StorageCollectionType.OBJECTS, "");
    }

    @Test
    public void deleteServerError() {
        when(mock.delete()).thenReturn(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
        try {
            client.deleteContainer("idTenant", "idStrategy");
            fail("Should rise an exception");
        } catch (final VitamClientException e) {
            // nothing to do
        }

        when(mock.delete()).thenReturn(Response.status(Response.Status.PRECONDITION_FAILED).build());
        try {
            client.deleteContainer("idTenant", "idStrategy");
            fail("Should rise an exception");
        } catch (final VitamClientException e) {
            // nothing to do
        }
    }

    @Test
    public void statusExecutionWithouthBody() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.NO_CONTENT).build());
        client.checkStatus();
    }

    @Test
    public void statusExecutionWithBody() throws Exception {
        when(mock.get()).thenReturn(
            Response.status(Response.Status.NO_CONTENT).entity("{\"pid\":\"1\",\"name\":\"name1\", \"role\":\"role1\"}")
                .build());
        client.checkStatus();
    }

    @Test(expected = VitamApplicationServerException.class)
    public void failsStatusExecution() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.NOT_IMPLEMENTED).build());
        client.checkStatus();
    }

    @Test(expected = StorageServerClientException.class)
    public void failsGetContainerObjectExecutionWhenPreconditionFailed() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        client.getContainerAsync("idTenant", "idStrategy", "guid", StorageCollectionType.OBJECTS);
    }

    @Test(expected = StorageServerClientException.class)
    public void failsGetContainerObjectExecutionWhenInternalServerError() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        client.getContainerAsync("idTenant", "idStrategy", "guid", StorageCollectionType.OBJECTS);
    }

    @Test(expected = StorageNotFoundException.class)
    public void failsGetContainerObjectExecutionWhenNotFound() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        client.getContainerAsync("idTenant", "idStrategy", "guid", StorageCollectionType.OBJECTS);
    }

    @Test
    public void successGetContainerObjectExecutionWhenFound() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.OK).entity(IOUtils.toInputStream("Vitam test")).build());
        final InputStream stream =
            client.getContainerAsync("idTenant", "idStrategy", "guid", StorageCollectionType.OBJECTS)
                .readEntity(InputStream.class);
        final InputStream stream2 = IOUtils.toInputStream("Vitam test");
        assertNotNull(stream);
        assertTrue(IOUtils.contentEquals(stream, stream2));
    }

    private StoredInfoResult generateStoredInfoResult(String guid) {
        final StoredInfoResult result = new StoredInfoResult();
        result.setId(guid);
        result.setInfo("Creation ok");
        result.setCreationTime(LocalDateUtil.getString(LocalDateTime.now()));
        result.setLastModifiedTime(LocalDateUtil.getString(LocalDateTime.now()));
        return result;
    }

}
