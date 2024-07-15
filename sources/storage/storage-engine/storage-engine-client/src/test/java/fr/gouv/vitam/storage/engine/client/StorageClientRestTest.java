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
package fr.gouv.vitam.storage.engine.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.accesslog.AccessLogUtils;
import fr.gouv.vitam.common.client.CustomVitamHttpStatusCode;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.storage.AccessRequestStatus;
import fr.gouv.vitam.common.server.application.junit.ResteasyTestApplication;
import fr.gouv.vitam.common.serverv2.VitamServerTestRunner;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.storage.driver.model.StorageLogBackupResult;
import fr.gouv.vitam.storage.engine.client.exception.StorageAlreadyExistsClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageIllegalOperationClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageUnavailableDataFromAsyncOfferClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.storage.engine.common.model.request.BulkObjectAvailabilityRequest;
import fr.gouv.vitam.storage.engine.common.model.request.BulkObjectStoreRequest;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.storage.engine.common.model.request.OfferLogRequest;
import fr.gouv.vitam.storage.engine.common.model.response.BulkObjectAvailabilityResponse;
import fr.gouv.vitam.storage.engine.common.model.response.BulkObjectStoreResponse;
import fr.gouv.vitam.storage.engine.common.model.response.StoredInfoResult;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageStrategy;
import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
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
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * StorageClientRest Test
 */
public class StorageClientRestTest extends ResteasyTestApplication {

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    protected static StorageClientRest client;
    private static final Integer TENANT_ID = 0;

    protected static final ExpectedResults mock = mock(ExpectedResults.class);

    private static StorageClientFactory factory = StorageClientFactory.getInstance();
    private static VitamServerTestRunner vitamServerTestRunner = new VitamServerTestRunner(
        StorageClientRestTest.class,
        factory
    );

    @BeforeClass
    public static void setUpBeforeClass() throws Throwable {
        vitamServerTestRunner.start();
        client = (StorageClientRest) vitamServerTestRunner.getClient();
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
        return Sets.newHashSet(new MockResource(mock));
    }

    @Path("/storage/v1")
    public static class MockResource {

        private static final String STORAGE_BACKUP_PATH = "/storage/backup";
        private final ExpectedResults expectedResponse;

        MockResource(ExpectedResults expectedResponse) {
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
        @Path("/delete/{id_object}")
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
        @Produces({ MediaType.APPLICATION_OCTET_STREAM, CommonMediaType.ZIP })
        @Consumes(MediaType.APPLICATION_JSON)
        public Response getObject(@Context HttpHeaders headers, @PathParam("id_object") String objectId) {
            return expectedResponse.get();
        }

        @Path("/copy/{id_object}")
        @POST
        @Produces(MediaType.APPLICATION_JSON)
        public Response copy(
            @Context HttpServletRequest httpServletRequest,
            @Context HttpHeaders headers,
            @PathParam("id_object") String objectId
        ) {
            assertThat(headers.getHeaderString(GlobalDataRest.X_TENANT_ID)).isNotNull();
            assertThat(headers.getHeaderString(GlobalDataRest.X_CONTENT_DESTINATION)).isNotNull();
            assertThat(headers.getHeaderString(GlobalDataRest.X_CONTENT_SOURCE)).isNotNull();
            assertThat(headers.getHeaderString(GlobalDataRest.X_STRATEGY_ID)).isNotNull();
            assertThat(headers.getHeaderString(GlobalDataRest.X_DATA_CATEGORY)).isNotNull();
            assertThat(objectId).isNotNull();

            return expectedResponse.post();
        }

        @POST
        @Path(STORAGE_BACKUP_PATH)
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response backup(List<Integer> tenants) {
            return expectedResponse.post();
        }

        // operations/traceability"

        @GET
        @Path("/{type}/logs")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response getOfferLogs(
            @HeaderParam(GlobalDataRest.X_TENANT_ID) String xTenantId,
            @PathParam("type") String type,
            OfferLogRequest offerLogRequest
        ) {
            return expectedResponse.get();
        }

        @Path("/bulk/{folder}")
        @POST
        @Produces(MediaType.APPLICATION_JSON)
        @Consumes(MediaType.APPLICATION_JSON)
        public Response bulkCreateFromWorkspace(
            @Context HttpServletRequest httpServletRequest,
            @PathParam("folder") String folder,
            BulkObjectStoreRequest bulkObjectStoreRequest
        ) {
            return expectedResponse.post();
        }

        @GET
        @Path("/strategies")
        @Produces(MediaType.APPLICATION_JSON)
        public Response getStorageStrategies() {
            return expectedResponse.get();
        }

        @POST
        @Path("/access-request/{dataCategory}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response createAccessRequestIfRequired(
            @PathParam("dataCategory") DataCategory dataCategory,
            List<String> objectsNames,
            @Context HttpHeaders headers
        ) {
            assertThat(headers.getHeaderString(GlobalDataRest.X_TENANT_ID)).isEqualTo("3");
            assertThat(headers.getHeaderString(GlobalDataRest.X_STRATEGY_ID)).isEqualTo("myStrategyId");
            assertThat(headers.getHeaderString(GlobalDataRest.X_OFFER)).isEqualTo("myOfferId");
            assertThat(dataCategory).isEqualTo(DataCategory.OBJECT);
            assertThat(objectsNames).containsExactly("obj1", "obj2");

            return expectedResponse.post();
        }

        @GET
        @Path("/access-request/statuses")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response checkAccessRequestStatuses(List<String> accessRequestIds, @Context HttpHeaders headers) {
            assertThat(headers.getHeaderString(GlobalDataRest.X_TENANT_ID)).isEqualTo("3");
            assertThat(headers.getHeaderString(GlobalDataRest.X_STRATEGY_ID)).isEqualTo("myStrategyId");
            assertThat(headers.getHeaderString(GlobalDataRest.X_OFFER)).isEqualTo("myOfferId");
            assertThat(headers.getHeaderString(GlobalDataRest.X_ADMIN_CROSS_TENANT_ACCESS_REQUEST_ALLOWED)).isEqualTo(
                "true"
            );
            assertThat(accessRequestIds).containsExactly("accessRequestId1", "accessRequestId2");

            return expectedResponse.get();
        }

        @DELETE
        @Path("/access-request/{accessRequestId}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response removeAccessRequest(
            @PathParam("accessRequestId") String accessRequestId,
            @Context HttpHeaders headers
        ) {
            assertThat(headers.getHeaderString(GlobalDataRest.X_TENANT_ID)).isEqualTo("3");
            assertThat(headers.getHeaderString(GlobalDataRest.X_STRATEGY_ID)).isEqualTo("myStrategyId");
            assertThat(headers.getHeaderString(GlobalDataRest.X_OFFER)).isEqualTo("myOfferId");
            assertThat(headers.getHeaderString(GlobalDataRest.X_ADMIN_CROSS_TENANT_ACCESS_REQUEST_ALLOWED)).isEqualTo(
                "true"
            );
            assertThat(accessRequestId).isEqualTo("accessRequestId1");

            return expectedResponse.delete();
        }

        @GET
        @Path("/object-availability-check/{dataCategory}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response checkObjectAvailability(
            @PathParam("dataCategory") DataCategory dataCategory,
            List<String> objectsNames,
            @Context HttpHeaders headers
        ) {
            assertThat(headers.getHeaderString(GlobalDataRest.X_TENANT_ID)).isEqualTo("3");
            assertThat(headers.getHeaderString(GlobalDataRest.X_STRATEGY_ID)).isEqualTo("myStrategyId");
            assertThat(headers.getHeaderString(GlobalDataRest.X_OFFER)).isEqualTo("myOfferId");
            assertThat(dataCategory).isEqualTo(DataCategory.OBJECT);
            assertThat(objectsNames).containsExactly("obj1", "obj2");

            return expectedResponse.get();
        }

        @GET
        @Path("/referentOffer")
        @Produces(MediaType.APPLICATION_JSON)
        public Response getReferentOffer() {
            return expectedResponse.get();
        }
    }

    @RunWithCustomExecutor
    @Test
    public void getContainerInfos() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.get()).thenReturn(
            Response.status(Response.Status.OK).entity(JsonHandler.createObjectNode().put("test", "test")).build()
        );
        client.getStorageInformation("idStrategy");
    }

    @RunWithCustomExecutor
    @Test(expected = IllegalArgumentException.class)
    public void getContainerInfosWithTenantIllegalArgumentException() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(null);
        when(mock.get()).thenReturn(
            Response.status(Response.Status.OK).entity(JsonHandler.createObjectNode().put("test", "test")).build()
        );
        client.getStorageInformation("idStrategy");
    }

    @RunWithCustomExecutor
    @Test(expected = IllegalArgumentException.class)
    public void getContainerInfosWithStrategyIllegalArgumentException() throws Exception {
        when(mock.get()).thenReturn(
            Response.status(Response.Status.OK).entity(JsonHandler.createObjectNode().put("test", "test")).build()
        );
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
        when(mock.post()).thenReturn(
            Response.status(Response.Status.CREATED).entity(generateStoredInfoResult("idObject")).build()
        );
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
            "SIP/content/e726e114f302c871b64569a00acb3a19badb7ee8ce4aef72cc2a043ace4905b8e8fca6f4771f8d6f67e221a53a4bbe170501af318c8f2c026cc8ea60f66fa804.odt"
        );
        return description;
    }

    @RunWithCustomExecutor
    @Test
    public void existsWithTenantIllegalArgumentException() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(null);
        when(mock.head()).thenReturn(Response.status(Response.Status.NO_CONTENT).build());
        assertThatThrownBy(() -> {
            client.exists("idStrategy", DataCategory.OBJECT, "idObject", Collections.emptyList());
        }).isInstanceOf(IllegalArgumentException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void existsWithStrategyIllegalArgumentException() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.head()).thenReturn(Response.status(Response.Status.NO_CONTENT).build());
        assertThatThrownBy(() -> {
            client.exists("", DataCategory.OBJECT, "idObject", Collections.emptyList());
        }).isInstanceOf(IllegalArgumentException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void existsWithObjectIdIllegalArgumentException() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.head()).thenReturn(Response.status(Response.Status.NO_CONTENT).build());
        assertThatThrownBy(() -> {
            client.exists("idStrategy", DataCategory.OBJECT, "", Collections.emptyList());
        }).isInstanceOf(IllegalArgumentException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void deleteOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.delete()).thenReturn(Response.status(Response.Status.NO_CONTENT).build());
        assertTrue(client.delete("idStrategy", DataCategory.OBJECT, "idObject"));
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertTrue(client.delete("idStrategy", DataCategory.UNIT, "idUnits"));
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertTrue(client.delete("idStrategy", DataCategory.LOGBOOK, "idLogbooks"));
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertTrue(client.delete("idStrategy", DataCategory.OBJECTGROUP, "idObjectGroups"));
    }

    @RunWithCustomExecutor
    @Test
    public void deleteKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.delete()).thenReturn(Response.status(Response.Status.NOT_FOUND).build());

        assertFalse(client.delete("idStrategy", DataCategory.OBJECT, "idObject"));
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertFalse(client.delete("idStrategy", DataCategory.UNIT, "idUnits"));
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertFalse(client.delete("idStrategy", DataCategory.LOGBOOK, "idLogbooks"));
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertFalse(client.delete("idStrategy", DataCategory.OBJECTGROUP, "idObjectGroups"));
    }

    @RunWithCustomExecutor
    @Test(expected = IllegalArgumentException.class)
    public void deleteWithTenantIllegalArgumentException() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(null);
        when(mock.delete()).thenReturn(Response.status(Response.Status.NO_CONTENT).build());
        client.exists("idStrategy", DataCategory.OBJECT, "idObject", Collections.emptyList());
    }

    @RunWithCustomExecutor
    @Test(expected = IllegalArgumentException.class)
    public void deleteWithStrategyIllegalArgumentException() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.delete()).thenReturn(Response.status(Response.Status.NO_CONTENT).build());
        client.exists("", DataCategory.OBJECT, "idObject", Collections.emptyList());
    }

    @RunWithCustomExecutor
    @Test(expected = IllegalArgumentException.class)
    public void deleteWithObjectIdIllegalArgumentException() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.delete()).thenReturn(Response.status(Response.Status.NO_CONTENT).build());
        client.exists("idStrategy", DataCategory.OBJECT, "", Collections.emptyList());
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
        when(mock.get()).thenReturn(
            Response.status(Response.Status.NO_CONTENT)
                .entity("{\"pid\":\"1\",\"name\":\"name1\", \"role\":\"role1\"}")
                .build()
        );
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
        client.getContainerAsync("idStrategy", "guid", DataCategory.OBJECT, AccessLogUtils.getNoLogAccessLog());
    }

    @RunWithCustomExecutor
    @Test
    public void failsGetContainerObjectExecutionWhenUnavailableDataFromAsyncOffer() {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.get()).thenReturn(
            Response.status(CustomVitamHttpStatusCode.UNAVAILABLE_DATA_FROM_ASYNC_OFFER.getStatusCode()).build()
        );
        assertThatThrownBy(
            () ->
                client.getContainerAsync("idStrategy", "guid", DataCategory.OBJECT, AccessLogUtils.getNoLogAccessLog())
        ).isInstanceOf(StorageUnavailableDataFromAsyncOfferClientException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void failsGetContainerObjectWithExplicitOfferIdWhenUnavailableDataFromAsyncOffer() {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.get()).thenReturn(
            Response.status(CustomVitamHttpStatusCode.UNAVAILABLE_DATA_FROM_ASYNC_OFFER.getStatusCode()).build()
        );
        assertThatThrownBy(
            () ->
                client.getContainerAsync(
                    "idStrategy",
                    "tape_offer",
                    "guid",
                    DataCategory.OBJECT,
                    AccessLogUtils.getNoLogAccessLog()
                )
        ).isInstanceOf(StorageUnavailableDataFromAsyncOfferClientException.class);
    }

    @RunWithCustomExecutor
    @Test(expected = StorageServerClientException.class)
    public void failsGetContainerObjectExecutionWhenInternalServerError() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.get()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        client.getContainerAsync("idStrategy", "guid", DataCategory.OBJECT, AccessLogUtils.getNoLogAccessLog());
    }

    @RunWithCustomExecutor
    @Test(expected = StorageNotFoundException.class)
    public void failsGetContainerObjectExecutionWhenNotFound() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        client.getContainerAsync("idStrategy", "guid", DataCategory.OBJECT, AccessLogUtils.getNoLogAccessLog());
    }

    @RunWithCustomExecutor
    @Test
    public void successGetContainerObjectExecutionWhenFound() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.get()).thenReturn(Response.status(Status.OK).entity(StreamUtils.toInputStream("Vitam test")).build());
        final InputStream stream = client
            .getContainerAsync("idStrategy", "guid", DataCategory.OBJECT, AccessLogUtils.getNoLogAccessLog())
            .readEntity(InputStream.class);
        final InputStream stream2 = StreamUtils.toInputStream("Vitam test");
        assertNotNull(stream);
        assertTrue(IOUtils.contentEquals(stream, stream2));
    }

    @RunWithCustomExecutor
    @Test
    public void failsCopyObjectExecutionWhenPreconditionFailed() {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.post()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        assertThatThrownBy(
            () ->
                client.copyObjectFromOfferToOffer(
                    "guid",
                    DataCategory.OBJECT,
                    "sourceOffer",
                    "destinationOffer",
                    "strategyId"
                )
        ).isInstanceOf(StorageServerClientException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void failsCopyObjectExecutionWhenUnavailableDataFromAsyncOffer() {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.post()).thenReturn(
            Response.status(CustomVitamHttpStatusCode.UNAVAILABLE_DATA_FROM_ASYNC_OFFER.getStatusCode()).build()
        );
        assertThatThrownBy(
            () ->
                client.copyObjectFromOfferToOffer(
                    "guid",
                    DataCategory.OBJECT,
                    "sourceOffer",
                    "destinationOffer",
                    "strategyId"
                )
        ).isInstanceOf(StorageUnavailableDataFromAsyncOfferClientException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void failsCopyObjectExecutionWhenInternalServerError() {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.post()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        assertThatThrownBy(
            () ->
                client.copyObjectFromOfferToOffer(
                    "guid",
                    DataCategory.OBJECT,
                    "sourceOffer",
                    "destinationOffer",
                    "strategyId"
                )
        ).isInstanceOf(StorageServerClientException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void successCopyObjectExecutionWhenFound() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.post()).thenReturn(new RequestResponseOK<>().setHttpCode(200).toResponse());
        RequestResponseOK<JsonNode> jsonNodeRequestResponseOK = client.copyObjectFromOfferToOffer(
            "guid",
            DataCategory.OBJECT,
            "sourceOffer",
            "destinationOffer",
            "strategyId"
        );
        assertNotNull(jsonNodeRequestResponseOK);
    }

    @RunWithCustomExecutor
    @Test
    public void successBackupStorageLog() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.post()).thenReturn(
            Response.status(Status.OK)
                .entity(
                    new RequestResponseOK<StorageLogBackupResult>()
                        .addResult(
                            new StorageLogBackupResult().setTenantId(0).setOperationId(GUIDFactory.newGUID().getId())
                        )
                        .addResult(
                            new StorageLogBackupResult().setTenantId(1).setOperationId(GUIDFactory.newGUID().getId())
                        )
                )
                .build()
        );
        client.storageLogBackup(Arrays.asList(0, 1));
    }

    private StoredInfoResult generateStoredInfoResult(String guid) {
        final StoredInfoResult result = new StoredInfoResult();
        result.setId(guid);
        result.setInfo("Creation ok");
        result.setCreationTime(LocalDateUtil.nowFormatted());
        result.setLastModifiedTime(LocalDateUtil.nowFormatted());
        return result;
    }

    @RunWithCustomExecutor
    @Test
    public void getOffsetLogOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        RequestResponseOK<OfferLog> requestResponse = new RequestResponseOK<>();
        IntStream.range(1, 11).forEach(sequence -> {
            OfferLog offerLog = setUpOfferLog(sequence);
            requestResponse.addResult(offerLog);
        });
        requestResponse.setHttpCode(Status.OK.getStatusCode());

        when(mock.get()).thenReturn(
            Response.status(Status.OK)
                .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
                .entity(JsonHandler.writeAsString(requestResponse))
                .build()
        );

        final RequestResponse<OfferLog> result = client.getOfferLogs(
            "idStrategy",
            null,
            DataCategory.OBJECT,
            2L,
            10,
            Order.ASC
        );
        assertNotNull(result);
        assertEquals(String.valueOf(TENANT_ID), result.getHeaderString(GlobalDataRest.X_TENANT_ID));
        assertEquals(true, result.isOk());
        assertEquals(Status.OK.getStatusCode(), result.getHttpCode());
        RequestResponseOK<OfferLog> resultOK = (RequestResponseOK<OfferLog>) result;
        assertEquals(10, resultOK.getResults().size());
    }

    private OfferLog setUpOfferLog(int sequence) {
        OfferLog offerLog = new OfferLog();
        offerLog.setContainer(DataCategory.OBJECT.getFolder() + "_" + TENANT_ID);
        offerLog.setFileName(GUIDFactory.newGUID().getId());
        offerLog.setSequence(sequence);
        offerLog.setTime(LocalDateUtil.now());
        return offerLog;
    }

    @RunWithCustomExecutor
    @Test
    public void getOfferLogInternalServerError() {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.get()).thenReturn(
            Response.status(Status.INTERNAL_SERVER_ERROR).header(GlobalDataRest.X_TENANT_ID, TENANT_ID).build()
        );
        assertThatThrownBy(
            () -> client.getOfferLogs("idStrategy", null, DataCategory.OBJECT, 2L, 10, Order.ASC)
        ).isInstanceOf(StorageServerClientException.class);
    }

    @RunWithCustomExecutor
    @Test(expected = IllegalArgumentException.class)
    public void getOfferLogInvalidRequest() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        client.getOfferLogs("idStrategy", null, null, 2L, 10, Order.ASC);
    }

    @RunWithCustomExecutor
    @Test(expected = IllegalArgumentException.class)
    public void getOfferLogInvalidRequestOrder() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        client.getOfferLogs("idStrategy", null, DataCategory.OBJECT, 2L, 10, null);
    }

    @RunWithCustomExecutor
    @Test
    public void bulkCreateFromWorkspaceOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.post()).thenReturn(
            Response.status(Response.Status.CREATED)
                .entity(
                    new BulkObjectStoreResponse(
                        Arrays.asList("offer1", "offer2"),
                        DigestType.SHA512.getName(),
                        ImmutableMap.of("ob1", "digest1", "ob2", "digest2")
                    )
                )
                .build()
        );
        client.bulkStoreFilesFromWorkspace("idStrategy", getBulkObjectStoreRequest());
    }

    @RunWithCustomExecutor
    @Test(expected = StorageNotFoundClientException.class)
    public void bulkCreateFromWorkspaceNotFound() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.post()).thenReturn(Response.status(Response.Status.NOT_FOUND).build());
        client.bulkStoreFilesFromWorkspace("idStrategy", getBulkObjectStoreRequest());
    }

    @RunWithCustomExecutor
    @Test(expected = StorageAlreadyExistsClientException.class)
    public void bulkCreateFromWorkspaceAlreadyExist() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.post()).thenReturn(Response.status(Response.Status.CONFLICT).build());
        client.bulkStoreFilesFromWorkspace("idStrategy", getBulkObjectStoreRequest());
    }

    @RunWithCustomExecutor
    @Test(expected = StorageServerClientException.class)
    public void bulkCreateFromWorkspaceInternalServerError() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.post()).thenReturn(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
        client.bulkStoreFilesFromWorkspace("idStrategy", getBulkObjectStoreRequest());
    }

    @RunWithCustomExecutor
    @Test(expected = IllegalArgumentException.class)
    public void bulkCreateFromWorkspaceWithTenantIllegalArgumentException() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(null);
        client.bulkStoreFilesFromWorkspace("idStrategy", getBulkObjectStoreRequest());
    }

    @RunWithCustomExecutor
    @Test(expected = IllegalArgumentException.class)
    public void bulkCreateFromWorkspaceWithStrategyIllegalArgumentException() throws Exception {
        when(mock.post()).thenReturn(Response.status(Response.Status.CREATED).build());
        client.bulkStoreFilesFromWorkspace(null, getBulkObjectStoreRequest());
    }

    @RunWithCustomExecutor
    @Test(expected = IllegalArgumentException.class)
    public void bulkCreateFromWorkspaceWithoutRequestIllegalArgumentException() throws Exception {
        when(mock.post()).thenReturn(Response.status(Response.Status.CREATED).build());
        client.bulkStoreFilesFromWorkspace("idStrategy", null);
    }

    @RunWithCustomExecutor
    @Test(expected = IllegalArgumentException.class)
    public void bulkCreateFromWorkspaceWithBadWorkspaceContainerIllegalArgumentException() throws Exception {
        when(mock.post()).thenReturn(Response.status(Response.Status.CREATED).build());
        client.bulkStoreFilesFromWorkspace(
            "idStrategy",
            new BulkObjectStoreRequest(
                "",
                Arrays.asList("uri1", "uri2"),
                DataCategory.UNIT,
                Arrays.asList("ob1", "ob2")
            )
        );
    }

    @RunWithCustomExecutor
    @Test(expected = IllegalArgumentException.class)
    public void bulkCreateFromWorkspaceWithBadWorkspaceUrisIllegalArgumentException() throws Exception {
        when(mock.post()).thenReturn(Response.status(Response.Status.CREATED).build());
        client.bulkStoreFilesFromWorkspace(
            "idStrategy",
            new BulkObjectStoreRequest(
                "workspaceContainer",
                Arrays.asList("uri1", ""),
                DataCategory.UNIT,
                Arrays.asList("ob1", "ob2")
            )
        );
    }

    @RunWithCustomExecutor
    @Test(expected = IllegalArgumentException.class)
    public void bulkCreateFromWorkspaceWithBadObjectIdsIllegalArgumentException() throws Exception {
        when(mock.post()).thenReturn(Response.status(Response.Status.CREATED).build());
        client.bulkStoreFilesFromWorkspace(
            "idStrategy",
            new BulkObjectStoreRequest(
                "workspaceContainer",
                Arrays.asList("uri1", "uri2"),
                DataCategory.UNIT,
                Arrays.asList("ob1", "")
            )
        );
    }

    @RunWithCustomExecutor
    @Test(expected = IllegalArgumentException.class)
    public void bulkCreateFromWorkspaceWithBadDataCategoryIllegalArgumentException() throws Exception {
        when(mock.post()).thenReturn(Response.status(Response.Status.CREATED).build());
        client.bulkStoreFilesFromWorkspace(
            "idStrategy",
            new BulkObjectStoreRequest(
                "workspaceContainer",
                Arrays.asList("uri1", "uri2"),
                null,
                Arrays.asList("ob1", "ob2")
            )
        );
    }

    @RunWithCustomExecutor
    @Test
    public void getStrategiesOk() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.get()).thenReturn(
            Response.status(Response.Status.OK).entity(new RequestResponseOK<StorageStrategy>()).build()
        );
        client.getStorageStrategies();
    }

    @RunWithCustomExecutor
    @Test(expected = StorageServerClientException.class)
    public void getStrategiesStorageServerClientException() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.get()).thenReturn(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
        client.getStorageStrategies();
    }

    @RunWithCustomExecutor
    @Test
    public void createAccessRequestIfRequiredWithAsyncOffer() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(3);
        when(mock.post()).thenReturn(
            new RequestResponseOK<>()
                .addResult("myAccessRequestId")
                .setHttpCode(Status.CREATED.getStatusCode())
                .toResponse()
        );

        // When
        Optional<String> accessRequestId = client.createAccessRequestIfRequired(
            "myStrategyId",
            "myOfferId",
            DataCategory.OBJECT,
            List.of("obj1", "obj2")
        );

        // Then
        assertThat(accessRequestId).isPresent();
        assertThat(accessRequestId.get()).isEqualTo("myAccessRequestId");
    }

    @RunWithCustomExecutor
    @Test
    public void createAccessRequestIfRequiredWithSyncOffer() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(3);
        when(mock.post()).thenReturn(Response.noContent().build());

        // When
        Optional<String> accessRequestId = client.createAccessRequestIfRequired(
            "myStrategyId",
            "myOfferId",
            DataCategory.OBJECT,
            List.of("obj1", "obj2")
        );

        // Then
        assertThat(accessRequestId).isEmpty();
    }

    @RunWithCustomExecutor
    @Test
    public void createAccessRequestIfRequiredWithServerErrorThenException() {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(3);
        when(mock.post()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());

        // When / Then
        assertThatThrownBy(
            () ->
                client.createAccessRequestIfRequired(
                    "myStrategyId",
                    "myOfferId",
                    DataCategory.OBJECT,
                    List.of("obj1", "obj2")
                )
        ).isInstanceOf(StorageServerClientException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void checkAccessRequestStatusesOK() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(3);
        when(mock.get()).thenReturn(
            new RequestResponseOK<>()
                .addResult(
                    Map.of(
                        "accessRequestId1",
                        AccessRequestStatus.NOT_FOUND,
                        "accessRequestId2",
                        AccessRequestStatus.READY
                    )
                )
                .setHttpCode(Status.OK.getStatusCode())
                .toResponse()
        );

        // When
        Map<String, AccessRequestStatus> accessRequestStatuses = client.checkAccessRequestStatuses(
            "myStrategyId",
            "myOfferId",
            List.of("accessRequestId1", "accessRequestId2"),
            true
        );

        // Then
        assertThat(accessRequestStatuses).isEqualTo(
            Map.of("accessRequestId1", AccessRequestStatus.NOT_FOUND, "accessRequestId2", AccessRequestStatus.READY)
        );
    }

    @RunWithCustomExecutor
    @Test
    public void checkAccessRequestStatusesWithNotAcceptableResponseThenException() {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(3);
        when(mock.get()).thenReturn(Response.status(Status.NOT_ACCEPTABLE).build());

        // When / Then
        assertThatThrownBy(
            () ->
                client.checkAccessRequestStatuses(
                    "myStrategyId",
                    "myOfferId",
                    List.of("accessRequestId1", "accessRequestId2"),
                    true
                )
        ).isInstanceOf(StorageIllegalOperationClientException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void checkAccessRequestStatusesWithServerErrorThenException() {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(3);
        when(mock.get()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());

        // When / Then
        assertThatThrownBy(
            () ->
                client.checkAccessRequestStatuses(
                    "myStrategyId",
                    "myOfferId",
                    List.of("accessRequestId1", "accessRequestId2"),
                    true
                )
        ).isInstanceOf(StorageServerClientException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void removeAccessRequestOK() {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(3);
        when(mock.delete()).thenReturn(Response.status(Status.OK).build());

        // When / Then
        assertThatCode(
            () -> client.removeAccessRequest("myStrategyId", "myOfferId", "accessRequestId1", true)
        ).doesNotThrowAnyException();
    }

    @RunWithCustomExecutor
    @Test
    public void removeAccessRequestWithNotAcceptableResponseThenException() {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(3);
        when(mock.delete()).thenReturn(Response.status(Status.NOT_ACCEPTABLE).build());

        // When / Then
        assertThatThrownBy(
            () -> client.removeAccessRequest("myStrategyId", "myOfferId", "accessRequestId1", true)
        ).isInstanceOf(StorageIllegalOperationClientException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void removeAccessRequestWithServerErrorThenException() {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(3);
        when(mock.delete()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());

        // When / Then
        assertThatThrownBy(
            () -> client.removeAccessRequest("myStrategyId", "myOfferId", "accessRequestId1", true)
        ).isInstanceOf(StorageServerClientException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void checkObjectAvailabilityOK() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(3);
        when(mock.get()).thenReturn(
            new RequestResponseOK<>()
                .addResult(new BulkObjectAvailabilityResponse(true))
                .setHttpCode(Status.OK.getStatusCode())
                .toResponse()
        );

        // When
        BulkObjectAvailabilityResponse objectAvailabilityResponse = client.checkBulkObjectAvailability(
            "myStrategyId",
            "myOfferId",
            new BulkObjectAvailabilityRequest(DataCategory.OBJECT, List.of("obj1", "obj2"))
        );

        // Then
        assertThat(objectAvailabilityResponse).isNotNull();
        assertThat(objectAvailabilityResponse.getAreObjectsAvailable()).isTrue();
    }

    @RunWithCustomExecutor
    @Test
    public void getReferentOffer() throws Exception {
        // Given
        final String OFFER_ID = "offerId";
        VitamThreadUtils.getVitamSession().setTenantId(3);
        when(mock.get()).thenReturn(
            new RequestResponseOK<>().addResult("offerId").setHttpCode(Status.OK.getStatusCode()).toResponse()
        );

        // When
        String referentOffer = client.getReferentOffer("myStrategyId");

        // Then
        assertThat(referentOffer).isNotNull();
    }

    @RunWithCustomExecutor
    @Test
    public void checkObjectAvailabilityWithServerErrorThenException() {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(3);
        when(mock.get()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());

        // When / Then
        assertThatThrownBy(
            () ->
                client.checkBulkObjectAvailability(
                    "myStrategyId",
                    "myOfferId",
                    new BulkObjectAvailabilityRequest(DataCategory.OBJECT, List.of("obj1", "obj2"))
                )
        ).isInstanceOf(StorageServerClientException.class);
    }

    private BulkObjectStoreRequest getBulkObjectStoreRequest() {
        return new BulkObjectStoreRequest(
            "workspaceContainer",
            Arrays.asList("uri1", "uri2"),
            DataCategory.UNIT,
            Arrays.asList("ob1", "ob2")
        );
    }
}
