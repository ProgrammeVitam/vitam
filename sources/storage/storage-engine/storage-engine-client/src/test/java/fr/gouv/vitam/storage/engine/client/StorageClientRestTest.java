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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.stream.IntStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HeaderParam;
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
import org.junit.Rule;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.SingletonUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.server.application.AbstractVitamApplication;
import fr.gouv.vitam.common.server.application.configuration.DefaultVitamApplicationConfiguration;
import fr.gouv.vitam.common.server.application.junit.VitamJerseyTest;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.storage.engine.client.exception.StorageAlreadyExistsClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.storage.engine.common.model.request.OfferLogRequest;
import fr.gouv.vitam.storage.engine.common.model.response.StoredInfoResult;

/**
 * StorageClientRest Test
 */
public class StorageClientRestTest extends VitamJerseyTest {

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    protected StorageClientRest client;
    private static final Integer TENANT_ID = 0;

    private static final String OFFER_METADATA =
        "{\"offer1\":\"c117854cbca3e51ea94c4bd2bcf4a6756209e6c65ddbf696313e1801b2235ff33d44b2bb272e714c335a44a3b4f92d399056b94dff4dfe6b7038fa56f23b438e\"," +
            "\"offer2\":\"c117854cbca3e51ea94c4bd2bcf4a6756209e6c65ddbf696313e1801b2235ff33d44b2bb272e714c335a44a3b4f92d399056b94dff4dfe6b7038fa56f23b438e\"}";

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

    // Define the getApplication to return your Application using the correct
    // Configuration
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

        return new StartApplicationResponse<AbstractApplication>().setServerPort(application.getVitamServer().getPort())
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

    @Path("/storage/v1")
    public static class MockResource {
        private static final String STORAGE_BACKUP_PATH = "/storage/backup";
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
        @Path("/")
        @Produces(MediaType.APPLICATION_JSON)
        public Response getContainer() {
            return expectedResponse.get();
        }

        @GET
        @Path("/info/{type}/{id_object}")
        public Response getInformation(@PathParam("id_object") String idObject) {
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
        @Path("/{type}/{id_object}")
        public Response checkObject(@PathParam("id_object") String idObject) {
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

        @POST
        @Path(STORAGE_BACKUP_PATH)
        @Produces(MediaType.APPLICATION_JSON)
        public Response t() {
            return expectedResponse.post();
        }
        // operations/traceability"


        @GET
        @Path("/{type}/logs")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response getOfferLogs(@HeaderParam(GlobalDataRest.X_TENANT_ID) String xTenantId,
            @PathParam("type") String type, OfferLogRequest offerLogRequest) {
            return expectedResponse.get();
        }
    }

    @RunWithCustomExecutor
    @Test
    public void getContainerInfos() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.get()).thenReturn(Response.status(Response.Status.OK)
            .entity(JsonHandler.createObjectNode().put("test", "test")).build());
        client.getStorageInformation("idStrategy");
    }

    @RunWithCustomExecutor
    @Test(expected = IllegalArgumentException.class)
    public void getContainerInfosWithTenantIllegalArgumentException() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(null);
        when(mock.get()).thenReturn(Response.status(Response.Status.OK)
            .entity(JsonHandler.createObjectNode().put("test", "test")).build());
        client.getStorageInformation("idStrategy");
    }

    @RunWithCustomExecutor
    @Test(expected = IllegalArgumentException.class)
    public void getContainerInfosWithStrategyIllegalArgumentException() throws Exception {
        when(mock.get()).thenReturn(Response.status(Response.Status.OK)
            .entity(JsonHandler.createObjectNode().put("test", "test")).build());
        client.getStorageInformation(null);
    }

    @RunWithCustomExecutor
    @Test(expected = StorageNotFoundClientException.class)
    public void getContainerInfosNotFound() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        client.getStorageInformation("idStrategy");
    }

    @RunWithCustomExecutor
    @Test(expected = StorageServerClientException.class)
    public void getContainerInfosInternalServerError() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.head()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        client.getStorageInformation("idStrategy");
    }

    @RunWithCustomExecutor
    @Test(expected = StorageServerClientException.class)
    public void getContainerInfosBadRequest() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.head()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        client.getStorageInformation("idStrategy");
    }

    @RunWithCustomExecutor
    @Test
    public void createFromWorkspaceOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.post())
            .thenReturn(Response.status(Response.Status.CREATED).entity(generateStoredInfoResult("idObject")).build());
        client.storeFileFromWorkspace("idStrategy", DataCategory.OBJECT, "idObject", getDescription());
    }

    @RunWithCustomExecutor
    @Test(expected = StorageNotFoundClientException.class)
    public void createFromWorkspaceNotFound() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.post()).thenReturn(Response.status(Response.Status.NOT_FOUND).build());
        client.storeFileFromWorkspace("idStrategy", DataCategory.OBJECT, "idObject", getDescription());
    }

    @RunWithCustomExecutor
    @Test(expected = StorageAlreadyExistsClientException.class)
    public void createFromWorkspaceAlreadyExist() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.post()).thenReturn(Response.status(Response.Status.CONFLICT).build());
        client.storeFileFromWorkspace("idStrategy", DataCategory.OBJECT, "idObject", getDescription());
    }

    @RunWithCustomExecutor
    @Test(expected = StorageServerClientException.class)
    public void createFromWorkspaceInternalServerError() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.post()).thenReturn(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
        client.storeFileFromWorkspace("idStrategy", DataCategory.OBJECT, "idObject", getDescription());
    }

    @RunWithCustomExecutor
    @Test(expected = IllegalArgumentException.class)
    public void createFromWorkspaceWithTenantIllegalArgumentException() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(null);
        when(mock.post()).thenReturn(Response.status(Response.Status.CREATED).build());
        client.storeFileFromWorkspace("idStrategy", DataCategory.OBJECT, "idObject", getDescription());
    }

    @RunWithCustomExecutor
    @Test(expected = IllegalArgumentException.class)
    public void createFromWorkspaceWithStrategyIllegalArgumentException() throws Exception {
        when(mock.post()).thenReturn(Response.status(Response.Status.CREATED).build());
        client.storeFileFromWorkspace(null, DataCategory.OBJECT, "idObject", getDescription());
    }

    @RunWithCustomExecutor
    @Test(expected = IllegalArgumentException.class)
    public void createFromWorkspaceWithObjectIdIllegalArgumentException() throws Exception {
        when(mock.post()).thenReturn(Response.status(Response.Status.CREATED).build());
        client.storeFileFromWorkspace("idStrategy", DataCategory.OBJECT, "", getDescription());
    }

    @RunWithCustomExecutor
    @Test(expected = IllegalArgumentException.class)
    public void createFromWorkspaceWithDecritionIllegalArgumentException() throws Exception {
        when(mock.post()).thenReturn(Response.status(Response.Status.CREATED).build());
        client.storeFileFromWorkspace("idStrategy", DataCategory.OBJECT, "idObject", null);
    }

    private ObjectDescription getDescription() {
        final ObjectDescription description = new ObjectDescription();
        description.setWorkspaceContainerGUID("aeaaaaaaaaaam7mxaaaamakwfnzbudaaaaaq");
        description.setWorkspaceObjectURI(
            "SIP/content/e726e114f302c871b64569a00acb3a19badb7ee8ce4aef72cc2a043ace4905b8e8fca6f4771f8d6f67e221a53a4bbe170501af318c8f2c026cc8ea60f66fa804.odt");
        return description;
    }

    @RunWithCustomExecutor
    @Test
    public void existsOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.head()).thenReturn(Response.status(Response.Status.NO_CONTENT).build());
        assertTrue(client.existsContainer("idStrategy"));
        assertTrue(client.exists("idStrategy", DataCategory.OBJECT, "idObject", SingletonUtils.singletonList()));
        assertTrue(client.exists("idStrategy", DataCategory.UNIT, "idUnits", SingletonUtils.singletonList()));
        assertTrue(client.exists("idStrategy", DataCategory.LOGBOOK, "idLogbooks", SingletonUtils.singletonList()));
        assertTrue(
            client.exists("idStrategy", DataCategory.OBJECTGROUP, "idObjectGroups", SingletonUtils.singletonList()));
    }

    @RunWithCustomExecutor
    @Test
    public void existsKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.head()).thenReturn(Response.status(Response.Status.NOT_FOUND).build());
        assertFalse(client.existsContainer("idStrategy"));
        assertFalse(client.exists("idStrategy", DataCategory.OBJECT, "idObject", SingletonUtils.singletonList()));
        assertFalse(client.exists("idStrategy", DataCategory.UNIT, "idUnits", SingletonUtils.singletonList()));
        assertFalse(client.exists("idStrategy", DataCategory.LOGBOOK, "idLogbooks", SingletonUtils.singletonList()));
        assertFalse(
            client.exists("idStrategy", DataCategory.OBJECTGROUP, "idObjectGroups", SingletonUtils.singletonList()));
    }

    @RunWithCustomExecutor
    @Test(expected = IllegalArgumentException.class)
    public void existsWithTenantIllegalArgumentException() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(null);
        when(mock.head()).thenReturn(Response.status(Response.Status.NO_CONTENT).build());
        client.exists("idStrategy", DataCategory.OBJECT, "idObject", SingletonUtils.singletonList());
    }

    @RunWithCustomExecutor
    @Test(expected = IllegalArgumentException.class)
    public void existsWithStrategyIllegalArgumentException() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.head()).thenReturn(Response.status(Response.Status.NO_CONTENT).build());
        client.exists("", DataCategory.OBJECT, "idObject", SingletonUtils.singletonList());
    }

    @RunWithCustomExecutor
    @Test(expected = IllegalArgumentException.class)
    public void existsWorkspaceWithObjectTypeIllegalArgumentException() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.head()).thenReturn(Response.status(Response.Status.NO_CONTENT).build());
        client.exists("idStrategy", DataCategory.CONTAINER, "0", SingletonUtils.singletonList());
    }

    @RunWithCustomExecutor
    @Test(expected = IllegalArgumentException.class)
    public void existsWithObjectIdIllegalArgumentException() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.head()).thenReturn(Response.status(Response.Status.NO_CONTENT).build());
        client.exists("idStrategy", DataCategory.OBJECT, "", SingletonUtils.singletonList());
    }

    @RunWithCustomExecutor
    @Test
    public void existsServerError() {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.head()).thenReturn(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
        try {
            client.existsContainer("idStrategy");
            fail("Should rise an exception");
        } catch (final VitamClientException e) {
            // nothing to do
        }

        when(mock.head()).thenReturn(Response.status(Response.Status.PRECONDITION_FAILED).build());
        try {
            client.existsContainer("idStrategy");
            fail("Should rise an exception");
        } catch (final VitamClientException e) {
            // nothing to do
        }
    }

    @RunWithCustomExecutor
    @Test
    public void deleteOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.delete()).thenReturn(Response.status(Response.Status.NO_CONTENT).build());
        assertTrue(client.deleteContainer("idStrategy"));
        assertTrue(client.delete("idStrategy", DataCategory.OBJECT, "idObject", "digest",
            VitamConfiguration.getDefaultDigestType().getName()));
        assertTrue(client.delete("idStrategy", DataCategory.UNIT, "idUnits", "digest",
            VitamConfiguration.getDefaultDigestType().getName()));
        assertTrue(client.delete("idStrategy", DataCategory.LOGBOOK, "idLogbooks", "digest",
            VitamConfiguration.getDefaultDigestType().getName()));
        assertTrue(client.delete("idStrategy", DataCategory.OBJECTGROUP, "idObjectGroups", "digest",
            VitamConfiguration.getDefaultDigestType().getName()));
    }

    @RunWithCustomExecutor
    @Test
    public void deleteKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.delete()).thenReturn(Response.status(Response.Status.NOT_FOUND).build());
        assertFalse(client.deleteContainer("idStrategy"));
        assertFalse(client.delete("idStrategy", DataCategory.OBJECT, "idObject", "digest",
            VitamConfiguration.getDefaultDigestType().getName()));
        assertFalse(client.delete("idStrategy", DataCategory.UNIT, "idUnits", "digest",
            VitamConfiguration.getDefaultDigestType().getName()));
        assertFalse(client.delete("idStrategy", DataCategory.LOGBOOK, "idLogbooks", "digest",
            VitamConfiguration.getDefaultDigestType().getName()));
        assertFalse(client.delete("idStrategy", DataCategory.OBJECTGROUP, "idObjectGroups", "digest",
            VitamConfiguration.getDefaultDigestType().getName()));
    }

    @RunWithCustomExecutor
    @Test(expected = IllegalArgumentException.class)
    public void deleteContainerWithIllegalArgumentException() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        client.delete("idStrategy", DataCategory.CONTAINER, "guid", null, null);
    }

    @RunWithCustomExecutor
    @Test(expected = IllegalArgumentException.class)
    public void deleteWithTenantIllegalArgumentException() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(null);
        when(mock.delete()).thenReturn(Response.status(Response.Status.NO_CONTENT).build());
        client.exists("idStrategy", DataCategory.OBJECT, "idObject", SingletonUtils.singletonList());
    }

    @RunWithCustomExecutor
    @Test(expected = IllegalArgumentException.class)
    public void deleteWithStrategyIllegalArgumentException() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.delete()).thenReturn(Response.status(Response.Status.NO_CONTENT).build());
        client.exists("", DataCategory.OBJECT, "idObject", SingletonUtils.singletonList());
    }

    @RunWithCustomExecutor
    @Test(expected = IllegalArgumentException.class)
    public void deleteWorkspaceWithObjectTypeIllegalArgumentException() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.delete()).thenReturn(Response.status(Response.Status.NO_CONTENT).build());
        client.exists("idStrategy", DataCategory.CONTAINER, "0", SingletonUtils.singletonList());
    }

    @RunWithCustomExecutor
    @Test(expected = IllegalArgumentException.class)
    public void deleteWithObjectIdIllegalArgumentException() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.delete()).thenReturn(Response.status(Response.Status.NO_CONTENT).build());
        client.exists("idStrategy", DataCategory.OBJECT, "", SingletonUtils.singletonList());
    }

    @RunWithCustomExecutor
    @Test
    public void deleteServerError() {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.delete()).thenReturn(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
        try {
            client.deleteContainer("idStrategy");
            fail("Should rise an exception");
        } catch (final VitamClientException e) {
            // nothing to do
        }

        when(mock.delete()).thenReturn(Response.status(Response.Status.PRECONDITION_FAILED).build());
        try {
            client.deleteContainer("idStrategy");
            fail("Should rise an exception");
        } catch (final VitamClientException e) {
            // nothing to do
        }
    }

    @RunWithCustomExecutor
    @Test
    public void statusExecutionWithouthBody() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.NO_CONTENT).build());
        client.checkStatus();
    }

    @RunWithCustomExecutor
    @Test
    public void statusExecutionWithBody() throws Exception {
        when(mock.get()).thenReturn(Response.status(Response.Status.NO_CONTENT)
            .entity("{\"pid\":\"1\",\"name\":\"name1\", \"role\":\"role1\"}").build());
        client.checkStatus();
    }

    @RunWithCustomExecutor
    @Test(expected = VitamApplicationServerException.class)
    public void failsStatusExecution() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.NOT_IMPLEMENTED).build());
        client.checkStatus();
    }

    @RunWithCustomExecutor
    @Test(expected = StorageServerClientException.class)
    public void failsGetContainerObjectExecutionWhenPreconditionFailed() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.get()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        client.getContainerAsync("idStrategy", "guid", DataCategory.OBJECT);
    }

    @RunWithCustomExecutor
    @Test(expected = StorageServerClientException.class)
    public void failsGetContainerObjectExecutionWhenInternalServerError() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.get()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        client.getContainerAsync("idStrategy", "guid", DataCategory.OBJECT);
    }

    @RunWithCustomExecutor
    @Test(expected = StorageNotFoundException.class)
    public void failsGetContainerObjectExecutionWhenNotFound() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        client.getContainerAsync("idStrategy", "guid", DataCategory.OBJECT);
    }

    @RunWithCustomExecutor
    @Test
    public void successGetContainerObjectExecutionWhenFound() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.get()).thenReturn(Response.status(Status.OK).entity(StreamUtils.toInputStream("Vitam test")).build());
        final InputStream stream = client.getContainerAsync("idStrategy", "guid", DataCategory.OBJECT)
            .readEntity(InputStream.class);
        final InputStream stream2 = StreamUtils.toInputStream("Vitam test");
        assertNotNull(stream);
        assertTrue(IOUtils.contentEquals(stream, stream2));
    }

    @RunWithCustomExecutor
    @Test
    public void successBackupStorageLog() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.post()).thenReturn(
            Response.status(Status.OK).entity("{\"pid\":\"1\",\"name\":\"name1\", \"role\":\"role1\"}").build());
        client.storageLogBackup();
    }

    private StoredInfoResult generateStoredInfoResult(String guid) {
        final StoredInfoResult result = new StoredInfoResult();
        result.setId(guid);
        result.setInfo("Creation ok");
        result.setCreationTime(LocalDateUtil.getString(LocalDateTime.now()));
        result.setLastModifiedTime(LocalDateUtil.getString(LocalDateTime.now()));
        return result;
    }

    @RunWithCustomExecutor
    @Test
    public void successGetObjectInformation() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.get()).thenReturn(Response.status(Status.OK).entity(OFFER_METADATA).build());
        JsonNode metadata = client.getInformation("idStrategy", DataCategory.OBJECT, "guid", SingletonUtils.singletonList());
        assertEquals(metadata.toString(), OFFER_METADATA);
    }

    @RunWithCustomExecutor
    @Test
    public void getOffsetLogOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        RequestResponseOK<OfferLog> requestResponse = new RequestResponseOK<>();
        IntStream.range(1, 11).forEach(sequence -> {
            OfferLog offerLog = new OfferLog();
            offerLog.setContainer(DataCategory.OBJECT.getFolder() + "_" + TENANT_ID);
            offerLog.setFileName(GUIDFactory.newGUID().getId());
            offerLog.setSequence(sequence);
            offerLog.setTime(LocalDateTime.now());
            requestResponse.addResult(offerLog);
        });
        requestResponse.setHttpCode(Status.OK.getStatusCode());

        when(mock.get()).thenReturn(
            Response.status(Status.OK).header(GlobalDataRest.X_TENANT_ID, TENANT_ID).entity(JsonHandler.writeAsString(requestResponse)).build());

        final RequestResponse<OfferLog> result = client.getOfferLogs("idStrategy", DataCategory.OBJECT, 2L, 10, Order.ASC);
        assertNotNull(result);
        assertEquals(String.valueOf(TENANT_ID), result.getHeaderString(GlobalDataRest.X_TENANT_ID));
        assertEquals(true, result.isOk());
        assertEquals(Status.OK.getStatusCode(), result.getHttpCode());
        RequestResponseOK<OfferLog> resultOK = (RequestResponseOK<OfferLog>) result;
        assertEquals(10, resultOK.getResults().size());
    }

    @RunWithCustomExecutor
    @Test
    public void getOfferLogInternalServerError() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.get()).thenReturn(
            Response.status(Status.INTERNAL_SERVER_ERROR).header(GlobalDataRest.X_TENANT_ID, TENANT_ID).build());
        final RequestResponse<OfferLog> result = client.getOfferLogs("idStrategy", DataCategory.OBJECT, 2L, 10, Order.ASC);
        assertNotNull(result);
        assertEquals(false, result.isOk());
        assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), result.getHttpCode());
    }

    @RunWithCustomExecutor
    @Test(expected = IllegalArgumentException.class)
    public void getOfferLogInvalidRequest() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        client.getOfferLogs("idStrategy", null, 2L, 10, Order.ASC);
    }

    @RunWithCustomExecutor
    @Test(expected = IllegalArgumentException.class)
    public void getOfferLogInvalidRequestOrder() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        client.getOfferLogs("idStrategy", DataCategory.OBJECT, 2L, 10, null);
    }

}
