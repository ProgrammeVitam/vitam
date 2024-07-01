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
package fr.gouv.vitam.storage.offers.workspace.driver;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.client.CustomVitamHttpStatusCode;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.FakeInputStream;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.storage.AccessRequestStatus;
import fr.gouv.vitam.common.model.storage.ObjectEntry;
import fr.gouv.vitam.common.server.application.junit.ResteasyTestApplication;
import fr.gouv.vitam.common.serverv2.VitamServerTestRunner;
import fr.gouv.vitam.storage.driver.AbstractConnection;
import fr.gouv.vitam.storage.driver.Connection;
import fr.gouv.vitam.storage.driver.Driver;
import fr.gouv.vitam.storage.driver.exception.StorageDriverConflictException;
import fr.gouv.vitam.storage.driver.exception.StorageDriverException;
import fr.gouv.vitam.storage.driver.exception.StorageDriverNotFoundException;
import fr.gouv.vitam.storage.driver.exception.StorageDriverPreconditionFailedException;
import fr.gouv.vitam.storage.driver.exception.StorageDriverServerErrorException;
import fr.gouv.vitam.storage.driver.exception.StorageDriverUnavailableDataFromAsyncOfferException;
import fr.gouv.vitam.storage.driver.model.StorageAccessRequestCreationRequest;
import fr.gouv.vitam.storage.driver.model.StorageBulkMetadataResult;
import fr.gouv.vitam.storage.driver.model.StorageBulkMetadataResultEntry;
import fr.gouv.vitam.storage.driver.model.StorageBulkPutRequest;
import fr.gouv.vitam.storage.driver.model.StorageBulkPutResult;
import fr.gouv.vitam.storage.driver.model.StorageBulkPutResultEntry;
import fr.gouv.vitam.storage.driver.model.StorageCapacityResult;
import fr.gouv.vitam.storage.driver.model.StorageCheckObjectAvailabilityRequest;
import fr.gouv.vitam.storage.driver.model.StorageCheckObjectAvailabilityResult;
import fr.gouv.vitam.storage.driver.model.StorageGetBulkMetadataRequest;
import fr.gouv.vitam.storage.driver.model.StorageGetMetadataRequest;
import fr.gouv.vitam.storage.driver.model.StorageGetResult;
import fr.gouv.vitam.storage.driver.model.StorageListRequest;
import fr.gouv.vitam.storage.driver.model.StorageMetadataResult;
import fr.gouv.vitam.storage.driver.model.StorageObjectRequest;
import fr.gouv.vitam.storage.driver.model.StorageOfferLogRequest;
import fr.gouv.vitam.storage.driver.model.StoragePutRequest;
import fr.gouv.vitam.storage.driver.model.StoragePutResult;
import fr.gouv.vitam.storage.driver.model.StorageRemoveRequest;
import fr.gouv.vitam.storage.driver.model.StorageRemoveResult;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.storage.engine.common.model.request.OfferLogRequest;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageOffer;
import org.apache.commons.collections4.IteratorUtils;
import org.junit.AfterClass;
import org.junit.Before;
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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class ConnectionImplTest extends ResteasyTestApplication {

    private static final Integer TENANT_ID = 1;
    private static final String HOSTNAME = "localhost";
    private static final String DEFAULT_GUID = "GUID";
    private static int tenant;
    private static StorageOffer offer = new StorageOffer();

    private static final String OBJECT_ID = "aeaaaaaaaaaam7mxaa2pkak2bnhxy5aaaaaq";
    private static final String TYPE = "object";
    private static Driver driver;

    protected static final ExpectedResults mock = mock(ExpectedResults.class);

    private static VitamServerTestRunner vitamServerTestRunner = new VitamServerTestRunner(ConnectionImplTest.class);

    @BeforeClass
    public static void setUpBeforeClass() throws Throwable {
        vitamServerTestRunner.start();
        tenant = Instant.now().getNano();
        driver = DriverImpl.getInstance();
        beforeTest();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Throwable {
        vitamServerTestRunner.runAfter();
        try {
            driver.close();
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }
    }

    @Override
    public Set<Object> getResources() {
        return Sets.newHashSet(new MockResource(mock));
    }

    private static void beforeTest() {
        String offerId = "default" + new SecureRandom().nextDouble();
        offer.setId(offerId);
        offer.setBaseUrl("http://" + HOSTNAME + ":" + vitamServerTestRunner.getBusinessPort());
        driver.addOffer(offer, null);
    }

    @Path("/offer/v1")
    public static class MockResource {

        private final ExpectedResults mock;

        MockResource(ExpectedResults mock) {
            this.mock = mock;
        }

        @GET
        @Path("/status")
        @Produces(MediaType.APPLICATION_JSON)
        public Response getStatus() {
            return mock.get();
        }

        @HEAD
        @Path("/objects/{type}/{id:.+}")
        public Response headObject(
            @PathParam("type") DataCategory type,
            @PathParam("id") String idObject,
            @HeaderParam(GlobalDataRest.X_TENANT_ID) String xTenantId,
            @HeaderParam(GlobalDataRest.X_DIGEST) String xDigest,
            @HeaderParam(GlobalDataRest.X_DIGEST_ALGORITHM) String xDigestAlgorithm
        ) {
            return mock.head();
        }

        @HEAD
        @Path("/objects/{type}")
        @Produces(MediaType.APPLICATION_JSON)
        public Response getContainerInformation() {
            return mock.head();
        }

        @GET
        @Path("/objects/{type}")
        @Produces(MediaType.APPLICATION_JSON)
        public Response listContainers() {
            return mock.get();
        }

        @PUT
        @Path("/objects/{type}/{guid:.+}")
        @Consumes(MediaType.APPLICATION_OCTET_STREAM)
        @Produces(MediaType.APPLICATION_JSON)
        public Response putObject(@PathParam("guid") String objectId, InputStream stream) {
            consumeAndCloseStream(stream);
            return mock.put();
        }

        @GET
        @Path("/objects/{type}/{id:.+}/metadatas")
        @Produces(MediaType.APPLICATION_JSON)
        public Response getObjectMetadata(
            @PathParam("type") DataCategory type,
            @PathParam("id") String idObject,
            @HeaderParam(GlobalDataRest.X_TENANT_ID) String xTenantId
        ) {
            return mock.get();
        }

        @GET
        @Path("/bulk/objects/{type}/metadata")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response getBulkObjectMetadata(
            @PathParam("type") DataCategory type,
            @HeaderParam(GlobalDataRest.X_TENANT_ID) String xTenantId,
            @HeaderParam(GlobalDataRest.X_OFFER_NO_CACHE) boolean noCache,
            List<String> guids
        ) {
            return mock.get();
        }

        @GET
        @Path("/objects/{type}/{id:.+}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(value = { MediaType.APPLICATION_JSON, MediaType.APPLICATION_OCTET_STREAM })
        public Response getObject(@PathParam("id") String objectId) {
            return mock.get();
        }

        @DELETE
        @Path("/objects/{type}/{id:.+}")
        @Produces(MediaType.APPLICATION_JSON)
        public Response removeObject(@PathParam("id") String objectId, @PathParam("type") String type) {
            return mock.delete();
        }

        @GET
        @Path("/objects/{type}/log")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response getOfferLogs(
            @HeaderParam(GlobalDataRest.X_TENANT_ID) String xTenantId,
            @PathParam("type") String type,
            OfferLogRequest offerLogRequest
        ) {
            return mock.get();
        }

        @PUT
        @Path("/bulk/objects/{type}")
        @Consumes(MediaType.APPLICATION_OCTET_STREAM)
        @Produces(MediaType.APPLICATION_JSON)
        public Response bulkPutObjects(@PathParam("type") DataCategory type, InputStream input) {
            return mock.put();
        }

        @POST
        @Path("/access-request/{type}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response createAccessRequest(
            @PathParam("type") DataCategory type,
            List<String> objectsIds,
            @Context HttpHeaders headers
        ) {
            final String xTenantId = headers.getHeaderString(GlobalDataRest.X_TENANT_ID);
            assertThat(xTenantId).isEqualTo("3");
            assertThat(objectsIds).containsExactly("obj1", "obj2");
            assertThat(type).isEqualTo(DataCategory.OBJECT);
            return mock.post();
        }

        @GET
        @Path("/access-request/statuses")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response checkAccessRequestStatuses(List<String> accessRequestIds, @Context HttpHeaders headers) {
            final String xTenantId = headers.getHeaderString(GlobalDataRest.X_TENANT_ID);
            assertThat(xTenantId).isEqualTo("3");
            assertThat(headers.getHeaderString(GlobalDataRest.X_ADMIN_CROSS_TENANT_ACCESS_REQUEST_ALLOWED)).isEqualTo(
                "true"
            );
            assertThat(accessRequestIds).containsExactly("accessRequestId1", "accessRequestId2");
            return mock.get();
        }

        @DELETE
        @Path("/access-request/{accessRequestId}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response removeAccessRequest(
            @PathParam("accessRequestId") String accessRequestId,
            @Context HttpHeaders headers
        ) {
            final String xTenantId = headers.getHeaderString(GlobalDataRest.X_TENANT_ID);
            assertThat(xTenantId).isEqualTo("3");
            assertThat(headers.getHeaderString(GlobalDataRest.X_ADMIN_CROSS_TENANT_ACCESS_REQUEST_ALLOWED)).isEqualTo(
                "true"
            );
            assertThat(accessRequestId).isEqualTo("accessRequestId1");

            return mock.delete();
        }

        @GET
        @Path("/object-availability-check/{type}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response checkObjectAvailability(
            @PathParam("type") DataCategory type,
            List<String> objectsIds,
            @Context HttpHeaders headers
        ) {
            final String xTenantId = headers.getHeaderString(GlobalDataRest.X_TENANT_ID);
            assertThat(xTenantId).isEqualTo("3");
            assertThat(objectsIds).containsExactly("obj1", "obj2");
            assertThat(type).isEqualTo(DataCategory.OBJECT);
            return mock.get();
        }

        void consumeAndCloseStream(InputStream stream) {
            try {
                if (null != stream) {
                    while (stream.read() > 0) {}
                    stream.close();
                }
            } catch (IOException e) {
                SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            }
        }
    }

    @Before
    public void before() {
        reset(mock);
    }

    @Test(expected = VitamApplicationServerException.class)
    public void getStatusKO() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        try (AbstractConnection connection = (AbstractConnection) driver.connect(offer.getId())) {
            connection.checkStatus();
        }
    }

    @Test
    public void getStatusNoContent() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.NO_CONTENT).build());
        try (AbstractConnection connection = (AbstractConnection) driver.connect(offer.getId())) {
            assertNotNull(connection.getServiceUrl());
            connection.checkStatus();
        }
    }

    @Test(expected = StorageDriverException.class)
    public void putObjectWithoutRequestKO() throws Exception {
        try (Connection connection = driver.connect(offer.getId())) {
            connection.putObject(null);
        }
    }

    @Test(expected = StorageDriverException.class)
    public void putObjectWithEmptyRequestKO() throws Exception {
        try (Connection connection = driver.connect(offer.getId())) {
            connection.putObject(new StoragePutRequest(null, null, null, null, null));
        }
    }

    @Test(expected = StorageDriverException.class)
    public void putObjectRequestWithOnlyMissingTenantIdKO() throws Exception {
        final StoragePutRequest request = getPutObjectRequest(true, true, true, false, true);
        try (Connection connection = driver.connect(offer.getId())) {
            connection.putObject(request);
        }
    }

    @Test(expected = StorageDriverException.class)
    public void putObjectRequestWithOnlyMissingDataStreamKO() throws Exception {
        final StoragePutRequest request = getPutObjectRequest(false, true, true, true, true);
        try (Connection connection = driver.connect(offer.getId())) {
            connection.putObject(request);
        }
    }

    @Test(expected = StorageDriverException.class)
    public void putObjectRequestWithOnlyMissingAlgortihmKO() throws Exception {
        final StoragePutRequest request = getPutObjectRequest(true, false, true, true, true);
        try (Connection connection = driver.connect(offer.getId())) {
            connection.putObject(request);
        }
    }

    @Test(expected = StorageDriverException.class)
    public void putObjectRequestWithOnlyMissingGuidKO() throws Exception {
        final StoragePutRequest request = getPutObjectRequest(true, true, false, true, true);
        try (Connection connection = driver.connect(offer.getId())) {
            connection.putObject(request);
        }
    }

    @Test(expected = StorageDriverException.class)
    public void putObjectRequestWithOnlyMissingTypeKO() throws Exception {
        final StoragePutRequest request = getPutObjectRequest(true, true, true, true, false);
        try (Connection connection = driver.connect(offer.getId())) {
            connection.putObject(request);
        }
    }

    @Test
    public void putObjectWithRequestOK() throws Exception {
        final StoragePutRequest request = getPutObjectRequest(true, true, true, true, true);
        when(mock.put())
            .thenReturn(Response.status(Status.CREATED).entity(getPutObjectResult(0)).build())
            .thenReturn(Response.status(Status.CREATED).entity(getPutObjectResult(1)).build())
            .thenReturn(Response.status(Status.CREATED).entity(getPutObjectResult(2)).build())
            .thenReturn(Response.status(Status.CREATED).entity(getPutObjectResult(3)).build())
            .thenReturn(Response.status(Status.CREATED).entity(getPutObjectResult(4)).build())
            .thenReturn(Response.status(Status.CREATED).entity(getPutObjectResult(5)).build())
            .thenReturn(Response.status(Status.CREATED).entity(getPutObjectResult(6)).build())
            .thenReturn(Response.status(Status.CREATED).entity(getPutObjectResult(7)).build());
        try (Connection connection = driver.connect(offer.getId())) {
            final StoragePutResult result = connection.putObject(request);
            assertNotNull(result);
            assertNotNull(result.getDistantObjectId());
            assertNotNull(result.getDigestHashBase16());
        }
    }

    // chunk size (1024) factor size case
    @Test
    public void putBigObjectWithRequestOk() throws Exception {
        final StoragePutRequest request = new StoragePutRequest(
            1,
            DataCategory.OBJECT.getFolder(),
            "GUID",
            DigestType.MD5.getName(),
            new FakeInputStream(2097152)
        );
        when(mock.put())
            .thenReturn(Response.status(Status.CREATED).entity(getPutObjectResult(0)).build())
            .thenReturn(Response.status(Status.CREATED).entity(getPutObjectResult(1)).build())
            .thenReturn(Response.status(Status.CREATED).entity(getPutObjectResult(2)).build())
            .thenReturn(Response.status(Status.CREATED).entity(getPutObjectResult(3)).build())
            .thenReturn(Response.status(Status.CREATED).entity(getPutObjectResult(4)).build())
            .thenReturn(Response.status(Status.CREATED).entity(getPutObjectResult(5)).build())
            .thenReturn(Response.status(Status.CREATED).entity(getPutObjectResult(6)).build());
        try (Connection connection = driver.connect(offer.getId())) {
            final StoragePutResult result = connection.putObject(request);
            assertNotNull(result);
            assertNotNull(result.getDistantObjectId());
            assertNotNull(result.getDigestHashBase16());
        }
    }

    // No chunk size (1024) factor case
    @Test
    public void putBigObject2WithRequestOk() throws Exception {
        final StoragePutRequest request = new StoragePutRequest(
            tenant,
            DataCategory.OBJECT.getFolder(),
            "GUID",
            DigestType.MD5.getName(),
            new FakeInputStream(2201507)
        );
        when(mock.put())
            .thenReturn(Response.status(Status.CREATED).entity(getPutObjectResult(0)).build())
            .thenReturn(Response.status(Status.CREATED).entity(getPutObjectResult(1)).build());
        try (Connection connection = driver.connect(offer.getId())) {
            final StoragePutResult result = connection.putObject(request);
            assertNotNull(result);
            assertNotNull(result.getDistantObjectId());
            assertNotNull(result.getDigestHashBase16());
        }
    }

    @Test(expected = StorageDriverException.class)
    public void putObjectWithRequestThrowsNotFoundErrorOnPutKO() throws Exception {
        final StoragePutRequest request = getPutObjectRequest(true, true, true, true, true);
        when(mock.put()).thenReturn(Response.status(Status.NOT_FOUND).build());
        try (Connection connection = driver.connect(offer.getId())) {
            connection.putObject(request);
        }
    }

    @Test(expected = StorageDriverException.class)
    public void putObjectWithRequestThrowsOtherErrorOnPutKO() throws Exception {
        final StoragePutRequest request = getPutObjectRequest(true, true, true, true, true);
        when(mock.put()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        try (Connection connection = driver.connect(offer.getId())) {
            connection.putObject(request);
        }
    }

    @Test(expected = StorageDriverException.class)
    public void putObjectWithRequestThrowsInternalServerErrorOnPutOK() throws Exception {
        final StoragePutRequest request = getPutObjectRequest(true, true, true, true, true);
        when(mock.put()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        try (Connection connection = driver.connect(offer.getId())) {
            connection.putObject(request);
        }
    }

    @Test(expected = StorageDriverException.class)
    public void putObjectThrowsOtherException() throws Exception {
        when(mock.put()).thenReturn(Response.status(Status.SERVICE_UNAVAILABLE).build());
        final StoragePutRequest request = getPutObjectRequest(true, true, true, true, true);
        try (Connection connection = driver.connect(offer.getId())) {
            connection.putObject(request);
        }
    }

    @Test(expected = StorageDriverConflictException.class)
    public void putObjectThrowsConflictException() throws Exception {
        when(mock.put()).thenReturn(Response.status(Status.CONFLICT).build());
        final StoragePutRequest request = getPutObjectRequest(true, true, true, true, true);
        try (Connection connection = driver.connect(offer.getId())) {
            connection.putObject(request);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void getGetObjectRequestIllegalArgumentException() throws Exception {
        try (Connection connection = driver.connect(offer.getId())) {
            connection.getObject(null);
        }
    }

    @Test
    public void getStorageCapacityOK() throws Exception {
        when(mock.head()).thenReturn(
            Response.status(Status.OK)
                .header("X-Usable-Space", "1000")
                .header("X-Used-Space", "1000")
                .header(GlobalDataRest.X_TENANT_ID, tenant)
                .build()
        );
        try (Connection connection = driver.connect(offer.getId())) {
            final StorageCapacityResult result = connection.getStorageCapacity(tenant);
            assertNotNull(result);
            assertEquals(Integer.valueOf(tenant), result.getTenantId());
            assertEquals(1000, result.getUsableSpace());
        }
    }

    @Test(expected = StorageDriverException.class)
    public void getStorageCapacityException() throws Exception {
        when(mock.head()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        try (Connection connection = driver.connect(offer.getId())) {
            connection.getStorageCapacity(0);
        }
    }

    @Test(expected = StorageDriverNotFoundException.class)
    public void getStorageCapacityNotFoundException() throws Exception {
        when(mock.head()).thenReturn(Response.status(Status.NOT_FOUND).build());
        try (Connection connection = driver.connect(offer.getId())) {
            connection.getStorageCapacity(0);
        }
    }

    @Test(expected = StorageDriverPreconditionFailedException.class)
    public void getStorageCapacityPreconditionFailedException() throws Exception {
        when(mock.head()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        try (Connection connection = driver.connect(offer.getId())) {
            connection.getStorageCapacity(0);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void getGetObjectGUIDIllegalArgumentException() throws Exception {
        final StorageObjectRequest request = new StorageObjectRequest(tenant, DataCategory.OBJECT.getFolder(), null);
        try (Connection connection = driver.connect(offer.getId())) {
            connection.getObject(request);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void getGetObjectTypeIllegalArgumentException() throws Exception {
        final StorageObjectRequest request = new StorageObjectRequest(null, DataCategory.OBJECT.getFolder(), "guid");
        try (Connection connection = driver.connect(offer.getId())) {
            connection.getObject(request);
        }
    }

    @Test
    public void getObjectUnavailableDataFromAsyncOffer() throws Exception {
        when(mock.get()).thenReturn(
            Response.status(CustomVitamHttpStatusCode.UNAVAILABLE_DATA_FROM_ASYNC_OFFER.getStatusCode()).build()
        );
        final StorageObjectRequest request = new StorageObjectRequest(tenant, DataCategory.OBJECT.getFolder(), "guid");
        try (Connection connection = driver.connect(offer.getId())) {
            assertThatThrownBy(() -> connection.getObject(request)).isInstanceOf(
                StorageDriverUnavailableDataFromAsyncOfferException.class
            );
        }
    }

    @Test
    public void getObjectNotFound() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        final StorageObjectRequest request = new StorageObjectRequest(tenant, DataCategory.OBJECT.getFolder(), "guid");
        try (Connection connection = driver.connect(offer.getId())) {
            connection.getObject(request);
            fail("Expected exception");
        } catch (final StorageDriverException exc) {
            assertEquals(StorageDriverNotFoundException.class, exc.getClass());
        }
    }

    @Test
    public void getObjectInternalError() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        final StorageObjectRequest request = new StorageObjectRequest(tenant, DataCategory.OBJECT.getFolder(), "guid");
        try (Connection connection = driver.connect(offer.getId())) {
            assertThatCode(() -> connection.getObject(request)).isInstanceOf(StorageDriverServerErrorException.class);
        }
    }

    @Test
    public void getObjectPreconditionFailed() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        final StorageObjectRequest request = new StorageObjectRequest(tenant, DataCategory.OBJECT.getFolder(), "guid");
        try (Connection connection = driver.connect(offer.getId())) {
            connection.getObject(request);
            fail("Expected exception");
        } catch (final StorageDriverException exc) {
            assertEquals(StorageDriverPreconditionFailedException.class, exc.getClass());
        }
    }

    @Test
    public void getObjectOK() throws Exception {
        final InputStream stream = new ByteArrayInputStream("Test".getBytes());
        when(mock.get()).thenReturn(Response.status(Status.OK).entity(stream).build());
        final StorageObjectRequest request = new StorageObjectRequest(tenant, DataCategory.OBJECT.getFolder(), "guid");
        try (Connection connection = driver.connect(offer.getId())) {
            final StorageGetResult result = connection.getObject(request);
            assertNotNull(result);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void objectExistInOfferWithEmptyParameterThrowsException() throws Exception {
        try (Connection connection = driver.connect(offer.getId())) {
            connection.objectExistsInOffer(null);
        }
    }

    @Test
    public void objectExistInOfferInternalServerError() throws Exception {
        when(mock.head()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        final StorageObjectRequest request = new StorageObjectRequest(tenant, DataCategory.OBJECT.getFolder(), "guid");
        try (Connection connection = driver.connect(offer.getId())) {
            connection.objectExistsInOffer(request);
            fail("Expected exception");
        } catch (final StorageDriverException exc) {
            assertEquals(StorageDriverException.class, exc.getClass());
        }
    }

    @Test
    public void objectExistInOfferNotFound() throws Exception {
        when(mock.head()).thenReturn(Response.status(Status.NOT_FOUND).build());
        final StorageObjectRequest request = new StorageObjectRequest(tenant, DataCategory.OBJECT.getFolder(), "guid");
        try (Connection connection = driver.connect(offer.getId())) {
            final boolean found = connection.objectExistsInOffer(request);
            assertFalse(found);
        } catch (final StorageDriverException exc) {
            fail("Ne exception expected");
        }
    }

    @Test
    public void objectExistInOfferFound() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.OK).build());
        final StorageObjectRequest request = new StorageObjectRequest(tenant, DataCategory.OBJECT.getFolder(), "guid");
        try (Connection connection = driver.connect(offer.getId())) {
            final boolean found = connection.objectExistsInOffer(request);
            assertTrue(found);
        } catch (final StorageDriverException exc) {
            fail("Ne exception expected");
        }
    }

    @Test
    public void objectExistInOfferPreconditionFailed() throws Exception {
        when(mock.head()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        final StorageObjectRequest request = new StorageObjectRequest(tenant, DataCategory.OBJECT.getFolder(), "guid");
        try (Connection connection = driver.connect(offer.getId())) {
            connection.objectExistsInOffer(request);
            fail("Expected exception");
        } catch (final StorageDriverException exc) {
            assertEquals(StorageDriverPreconditionFailedException.class, exc.getClass());
        }
    }

    private StoragePutRequest getPutObjectRequest(
        boolean putDataS,
        boolean putDigestA,
        boolean putGuid,
        boolean putTenantId,
        boolean putType
    ) throws Exception {
        FakeInputStream stream = null;
        String digest = null;
        String guid = null;
        Integer tenantId = null;
        String type = null;

        if (putDataS) {
            stream = new FakeInputStream(1);
        }
        if (putDigestA) {
            digest = DigestType.MD5.getName();
        }
        if (putGuid) {
            guid = "GUID";
        }
        if (putTenantId) {
            tenantId = 0;
        }
        if (putType) {
            type = DataCategory.OBJECT.getFolder();
        }
        return new StoragePutRequest(tenantId, type, guid, digest, stream);
    }

    private JsonNode getPutObjectResult(int uniqueId) throws JsonProcessingException, IOException {
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree("{\"digest\":\"aaakkkk" + uniqueId + "\",\"size\":\"666\"}");
    }

    @Test
    public void deleteObjectTestIllegalArgument() throws Exception {
        try (Connection connection = driver.connect(offer.getId())) {
            assertThatThrownBy(() -> connection.removeObject(null)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> connection.removeObject(getStorageRemoveRequest(false, true, true))).isInstanceOf(
                IllegalArgumentException.class
            );
            assertThatThrownBy(() -> connection.removeObject(getStorageRemoveRequest(true, false, true))).isInstanceOf(
                IllegalArgumentException.class
            );
            assertThatThrownBy(() -> connection.removeObject(getStorageRemoveRequest(true, true, false))).isInstanceOf(
                IllegalArgumentException.class
            );
        }
    }

    @Test
    public void deleteObjectTestOK() throws Exception {
        when(mock.delete()).thenReturn(Response.status(Status.OK).entity(getRemoveObjectResult()).build());
        try (Connection connection = driver.connect(offer.getId())) {
            StorageRemoveResult storageRemoveResult = connection.removeObject(
                getStorageRemoveRequest(true, true, true)
            );
            assertNotNull(storageRemoveResult);
            assertTrue(storageRemoveResult.isObjectDeleted());
        }

        when(mock.delete()).thenReturn(Response.status(Status.OK).entity(getRemoveObjectResultNotFound()).build());
        try (Connection connection = driver.connect(offer.getId())) {
            StorageRemoveResult storageRemoveResult2 = connection.removeObject(
                getStorageRemoveRequest(true, true, true)
            );
            assertNotNull(storageRemoveResult2);
            assertFalse(storageRemoveResult2.isObjectDeleted());
        }
    }

    @Test(expected = StorageDriverException.class)
    public void deleteObjectTestNotFound() throws Exception {
        when(mock.delete()).thenReturn(Response.status(Status.NOT_FOUND).build());
        try (Connection connection = driver.connect(offer.getId())) {
            connection.removeObject(getStorageRemoveRequest(true, true, true));
        }
    }

    @Test(expected = StorageDriverException.class)
    public void deleteObjectTestPreconditionFailed() throws Exception {
        when(mock.delete()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        try (Connection connection = driver.connect(offer.getId())) {
            connection.removeObject(getStorageRemoveRequest(true, true, true));
        }
    }

    @Test(expected = StorageDriverException.class)
    public void deleteObjectTestBadRequest() throws Exception {
        when(mock.delete()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        try (Connection connection = driver.connect(offer.getId())) {
            connection.removeObject(getStorageRemoveRequest(true, true, true));
        }
    }

    @Test(expected = StorageDriverException.class)
    public void deleteObjectTestInternalServerError() throws Exception {
        when(mock.delete()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        try (Connection connection = driver.connect(offer.getId())) {
            connection.removeObject(getStorageRemoveRequest(true, true, true));
        }
    }

    @Test
    public void listObjectsTest() throws Exception {
        StorageListRequest storageRequest = new StorageListRequest(TENANT_ID, DataCategory.OBJECT.getFolder());

        String responseContent = "{objectId:\"id\", size: 10}\n" + "{}";
        InputStream is = new ByteArrayInputStream(responseContent.getBytes(StandardCharsets.UTF_8));
        when(mock.get()).thenReturn(Response.ok(is).build());
        try (Connection connection = driver.connect(offer.getId())) {
            Iterator<ObjectEntry> response = connection.listObjects(storageRequest);
            List<ObjectEntry> objectEntries = IteratorUtils.toList(response);
            assertThat(objectEntries).hasSize(1);
            assertThat(objectEntries.get(0).getObjectId()).isEqualTo("id");
            assertThat(objectEntries.get(0).getSize()).isEqualTo(10L);
        }
    }

    private StorageRemoveRequest getStorageRemoveRequest(boolean putGuid, boolean putTenantId, boolean putType) {
        String guid = null;
        Integer tenantId = null;
        String type = null;
        if (putGuid) {
            guid = DEFAULT_GUID;
        }
        if (putTenantId) {
            tenantId = tenant;
        }
        if (putType) {
            type = DataCategory.OBJECT.getFolder();
        }
        return new StorageRemoveRequest(tenantId, type, guid);
    }

    private JsonNode getRemoveObjectResult() {
        final ObjectNode result = JsonHandler.createObjectNode();
        result.put("id", DEFAULT_GUID);
        result.put("status", Response.Status.OK.toString());
        return result;
    }

    private JsonNode getRemoveObjectResultNotFound() {
        final ObjectNode result = JsonHandler.createObjectNode();
        result.put("id", DEFAULT_GUID);
        result.put("status", Response.Status.NOT_FOUND.toString());
        return result;
    }

    @Test
    public void getObjectMetadataTestOK() throws StorageDriverException {
        when(mock.get()).thenReturn(Response.status(Status.OK).entity(mockMetadatasObjectResult()).build());
        final StorageGetMetadataRequest request = new StorageGetMetadataRequest(
            tenant,
            DataCategory.OBJECT.getFolder(),
            "guid",
            false
        );
        try (Connection connection = driver.connect(offer.getId())) {
            final StorageMetadataResult result = connection.getMetadatas(request);
            assertNotNull(result);
        }
    }

    @Test(expected = StorageDriverNotFoundException.class)
    public void getObjectMetadataTestTestNotFound() throws StorageDriverException {
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        final StorageGetMetadataRequest request = new StorageGetMetadataRequest(
            tenant,
            DataCategory.OBJECT.getFolder(),
            "guid",
            false
        );
        try (Connection connection = driver.connect(offer.getId())) {
            connection.getMetadatas(request);
        }
    }

    @Test(expected = StorageDriverException.class)
    public void getObjectMetadataTestInternalServerError() throws StorageDriverException {
        when(mock.get()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        final StorageGetMetadataRequest request = new StorageGetMetadataRequest(
            tenant,
            DataCategory.OBJECT.getFolder(),
            "guid",
            false
        );
        try (Connection connection = driver.connect(offer.getId())) {
            connection.getMetadatas(request);
        }
    }

    @Test
    public void getOfferLogsOK() throws Exception {
        RequestResponseOK<OfferLog> requestResponse = new RequestResponseOK<>();
        IntStream.range(1, 11).forEach(sequence -> {
            OfferLog offerLog = new OfferLog();
            offerLog.setContainer(DataCategory.OBJECT.getFolder() + "_" + tenant);
            offerLog.setFileName(GUIDFactory.newGUID().getId());
            offerLog.setSequence(sequence);
            offerLog.setTime(LocalDateUtil.now());
            requestResponse.addResult(offerLog);
        });
        requestResponse.setHttpCode(Status.OK.getStatusCode());

        when(mock.get()).thenReturn(
            Response.status(Status.OK)
                .header(GlobalDataRest.X_TENANT_ID, tenant)
                .entity(JsonHandler.writeAsString(requestResponse))
                .build()
        );

        StorageOfferLogRequest offerLogRequest = new StorageOfferLogRequest(
            tenant,
            DataCategory.OBJECT.getFolder(),
            2L,
            10,
            Order.ASC
        );
        try (Connection connection = driver.connect(offer.getId())) {
            final RequestResponse<OfferLog> result = connection.getOfferLogs(offerLogRequest);
            assertNotNull(result);
            assertEquals(String.valueOf(tenant), result.getHeaderString(GlobalDataRest.X_TENANT_ID));
            assertEquals(true, result.isOk());
            assertEquals(Status.OK.getStatusCode(), result.getHttpCode());
            RequestResponseOK<OfferLog> resultOK = (RequestResponseOK<OfferLog>) result;
            assertEquals(10, resultOK.getResults().size());
        }
    }

    @Test
    public void getOfferLogsInternalServerError() throws Exception {
        when(mock.get()).thenReturn(
            Response.status(Status.INTERNAL_SERVER_ERROR).header(GlobalDataRest.X_TENANT_ID, tenant).build()
        );
        StorageOfferLogRequest offerLogRequest = new StorageOfferLogRequest(
            tenant,
            DataCategory.OBJECT.getFolder(),
            2L,
            10,
            Order.ASC
        );
        try (Connection connection = driver.connect(offer.getId())) {
            assertThatThrownBy(() -> connection.getOfferLogs(offerLogRequest)).isInstanceOf(
                StorageDriverException.class
            );
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void getOfferLogsInvalidRequest() throws Exception {
        StorageOfferLogRequest offerLogRequest = new StorageOfferLogRequest(tenant, null, 2L, 10, Order.ASC);
        try (Connection connection = driver.connect(offer.getId())) {
            connection.getOfferLogs(offerLogRequest);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void getOfferLogsInvalidRequestOrder() throws Exception {
        StorageOfferLogRequest offerLogRequest = new StorageOfferLogRequest(
            tenant,
            DataCategory.OBJECT.getFolder(),
            2L,
            10,
            null
        );
        try (Connection connection = driver.connect(offer.getId())) {
            connection.getOfferLogs(offerLogRequest);
        }
    }

    private StorageMetadataResult mockMetadatasObjectResult() {
        return new StorageMetadataResult(
            OBJECT_ID,
            TYPE,
            "abcdef",
            6096,
            "Tue Aug 31 10:20:56 SGT 2016",
            "Tue Aug 31 10:20:56 SGT 2016"
        );
    }

    @Test(expected = StorageDriverException.class)
    public void bulkPutObjectsWithoutRequestKO() throws Exception {
        try (Connection connection = driver.connect(offer.getId())) {
            connection.bulkPutObjects(null);
        }
    }

    @Test(expected = StorageDriverException.class)
    public void bulkPutObjectsWithEmptyRequestKO() throws Exception {
        try (Connection connection = driver.connect(offer.getId())) {
            connection.bulkPutObjects(new StorageBulkPutRequest(null, null, null, null, null, 0));
        }
    }

    @Test(expected = StorageDriverException.class)
    public void bulkPutObjectsRequestWithOnlyMissingTenantIdKO() throws Exception {
        final StorageBulkPutRequest request = getBulkPutObjectsRequest(true, true, true, false, true);
        try (Connection connection = driver.connect(offer.getId())) {
            connection.bulkPutObjects(request);
        }
    }

    @Test(expected = StorageDriverException.class)
    public void bulkPutObjectsRequestWithOnlyMissingDataStreamKO() throws Exception {
        final StorageBulkPutRequest request = getBulkPutObjectsRequest(false, true, true, true, true);
        try (Connection connection = driver.connect(offer.getId())) {
            connection.bulkPutObjects(request);
        }
    }

    @Test(expected = StorageDriverException.class)
    public void bulkPutObjectsRequestWithOnlyMissingAlgortihmKO() throws Exception {
        final StorageBulkPutRequest request = getBulkPutObjectsRequest(true, false, true, true, true);
        try (Connection connection = driver.connect(offer.getId())) {
            connection.bulkPutObjects(request);
        }
    }

    @Test(expected = StorageDriverException.class)
    public void bulkPutObjectsRequestWithOnlyMissingGuidKO() throws Exception {
        final StorageBulkPutRequest request = getBulkPutObjectsRequest(true, true, false, true, true);
        try (Connection connection = driver.connect(offer.getId())) {
            connection.bulkPutObjects(request);
        }
    }

    @Test(expected = StorageDriverException.class)
    public void bulkPutObjectsRequestWithOnlyMissingTypeKO() throws Exception {
        final StorageBulkPutRequest request = getBulkPutObjectsRequest(true, true, true, true, false);
        try (Connection connection = driver.connect(offer.getId())) {
            connection.bulkPutObjects(request);
        }
    }

    @Test
    public void bulkPutObjectsWithRequestOK() throws Exception {
        final StorageBulkPutRequest request = getBulkPutObjectsRequest(true, true, true, true, true);

        StorageBulkPutResult storageBulkPutResult = new StorageBulkPutResult(
            Arrays.asList(
                new StorageBulkPutResultEntry("GUID1", "d1", 1),
                new StorageBulkPutResultEntry("GUID2", "d2", 2)
            )
        );
        when(mock.put()).thenReturn(Response.status(Status.CREATED).entity(storageBulkPutResult).build());

        try (Connection connection = driver.connect(offer.getId())) {
            final StorageBulkPutResult result = connection.bulkPutObjects(request);
            assertNotNull(result);
            assertThat(result.getEntries()).hasSize(2);
            assertThat(result.getEntries().get(0).getObjectId()).isEqualTo("GUID1");
            assertThat(result.getEntries().get(0).getDigest()).isEqualTo("d1");
            assertThat(result.getEntries().get(0).getSize()).isEqualTo(1);
        }
    }

    @Test(expected = StorageDriverException.class)
    public void bulkPutObjectsWithRequestThrowsNotFoundErrorOnPutKO() throws Exception {
        final StorageBulkPutRequest request = getBulkPutObjectsRequest(true, true, true, true, true);
        when(mock.put()).thenReturn(Response.status(Status.NOT_FOUND).build());
        try (Connection connection = driver.connect(offer.getId())) {
            connection.bulkPutObjects(request);
        }
    }

    @Test(expected = StorageDriverException.class)
    public void bulkPutObjectsWithRequestThrowsOtherErrorOnPutKO() throws Exception {
        final StorageBulkPutRequest request = getBulkPutObjectsRequest(true, true, true, true, true);
        when(mock.put()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        try (Connection connection = driver.connect(offer.getId())) {
            connection.bulkPutObjects(request);
        }
    }

    @Test(expected = StorageDriverException.class)
    public void bulkPutObjectsWithRequestThrowsInternalServerErrorOnPutOK() throws Exception {
        final StorageBulkPutRequest request = getBulkPutObjectsRequest(true, true, true, true, true);
        when(mock.put()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        try (Connection connection = driver.connect(offer.getId())) {
            connection.bulkPutObjects(request);
        }
    }

    @Test(expected = StorageDriverException.class)
    public void bulkPutObjectsThrowsOtherException() throws Exception {
        when(mock.put()).thenReturn(Response.status(Status.SERVICE_UNAVAILABLE).build());
        final StorageBulkPutRequest request = getBulkPutObjectsRequest(true, true, true, true, true);
        try (Connection connection = driver.connect(offer.getId())) {
            connection.bulkPutObjects(request);
        }
    }

    private StorageBulkPutRequest getBulkPutObjectsRequest(
        boolean putInputStream,
        boolean putDigestA,
        boolean putGuid,
        boolean putTenantId,
        boolean putType
    ) {
        FakeInputStream stream = null;
        DigestType digest = null;
        List<String> guid = null;
        Integer tenantId = null;
        String type = null;

        if (putInputStream) {
            stream = new FakeInputStream(1);
        }
        if (putDigestA) {
            digest = DigestType.MD5;
        }
        if (putGuid) {
            guid = Arrays.asList("GUID1", "GUID2");
        }
        if (putTenantId) {
            tenantId = 0;
        }
        if (putType) {
            type = DataCategory.OBJECT.getFolder();
        }
        return new StorageBulkPutRequest(tenantId, type, guid, digest, stream, 1L);
    }

    @Test
    public void bulkMetadataWithRequestOK() throws Exception {
        final StorageGetBulkMetadataRequest request = new StorageGetBulkMetadataRequest(
            0,
            DataCategory.UNIT.getFolder(),
            Arrays.asList("GUID1", "GUID2"),
            false
        );

        StorageBulkMetadataResult storageBulkMetadataResult = new StorageBulkMetadataResult(
            Arrays.asList(
                new StorageBulkMetadataResultEntry("GUID1", "d1", 1L),
                new StorageBulkMetadataResultEntry("GUID2", "d2", 2L)
            )
        );
        when(mock.get()).thenReturn(Response.status(Status.OK).entity(storageBulkMetadataResult).build());

        try (Connection connection = driver.connect(offer.getId())) {
            final StorageBulkMetadataResult result = connection.getBulkMetadata(request);
            assertNotNull(result);
            assertThat(result.getObjectMetadata()).hasSize(2);
            assertThat(result.getObjectMetadata().get(0).getObjectName()).isEqualTo("GUID1");
            assertThat(result.getObjectMetadata().get(0).getDigest()).isEqualTo("d1");
            assertThat(result.getObjectMetadata().get(0).getSize()).isEqualTo(1L);
        }
    }

    @Test
    public void bulkMetadataWithInternalServerErrorThanStorageDriverException() throws Exception {
        final StorageGetBulkMetadataRequest request = new StorageGetBulkMetadataRequest(
            0,
            DataCategory.UNIT.getFolder(),
            Arrays.asList("GUID1", "GUID2"),
            false
        );

        when(mock.get()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        try (Connection connection = driver.connect(offer.getId())) {
            assertThatThrownBy(() -> connection.getBulkMetadata(request)).isInstanceOf(StorageDriverException.class);
        }
    }

    @Test
    public void createAccessRequestOK() throws Exception {
        // Given
        StorageAccessRequestCreationRequest request = new StorageAccessRequestCreationRequest(
            3,
            DataCategory.OBJECT.getFolder(),
            Arrays.asList("obj1", "obj2")
        );
        when(mock.post()).thenReturn(
            new RequestResponseOK<>()
                .addResult("myAccessRequestId")
                .setHttpCode(Status.CREATED.getStatusCode())
                .toResponse()
        );

        // When
        try (Connection connection = driver.connect(offer.getId())) {
            String accessRequest = connection.createAccessRequest(request);

            // Then
            assertThat(accessRequest).isEqualTo("myAccessRequestId");
        }
    }

    @Test
    public void createAccessRequestWithServerErrorThenException() throws Exception {
        // Given
        StorageAccessRequestCreationRequest request = new StorageAccessRequestCreationRequest(
            3,
            DataCategory.OBJECT.getFolder(),
            Arrays.asList("obj1", "obj2")
        );

        when(mock.post()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());

        // When / Then
        try (Connection connection = driver.connect(offer.getId())) {
            assertThatThrownBy(() -> connection.createAccessRequest(request)).isInstanceOf(
                StorageDriverException.class
            );
        }
    }

    @Test
    public void checkAccessRequestStatusesOK() throws Exception {
        // Given
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
        try (Connection connection = driver.connect(offer.getId())) {
            Map<String, AccessRequestStatus> accessRequestStatuses = connection.checkAccessRequestStatuses(
                List.of("accessRequestId1", "accessRequestId2"),
                3,
                true
            );

            // Then
            assertThat(accessRequestStatuses).isEqualTo(
                Map.of("accessRequestId1", AccessRequestStatus.NOT_FOUND, "accessRequestId2", AccessRequestStatus.READY)
            );
        }
    }

    @Test
    public void checkAccessRequestStatusesWithServerErrorThenException() throws Exception {
        // Given
        when(mock.get()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());

        // When / Then
        try (Connection connection = driver.connect(offer.getId())) {
            assertThatThrownBy(
                () -> connection.checkAccessRequestStatuses(List.of("accessRequestId1", "accessRequestId2"), 3, true)
            ).isInstanceOf(StorageDriverException.class);
        }
    }

    @Test
    public void removeAccessRequestOK() throws Exception {
        // Given
        when(mock.delete()).thenReturn(Response.status(Status.OK).build());

        try (Connection connection = driver.connect(offer.getId())) {
            // When / Then
            assertThatCode(
                () -> connection.removeAccessRequest("accessRequestId1", 3, true)
            ).doesNotThrowAnyException();
        }
    }

    @Test
    public void removeAccessRequestOKWithServerErrorThenException() throws Exception {
        // Given
        when(mock.delete()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());

        // When / Then
        try (Connection connection = driver.connect(offer.getId())) {
            assertThatThrownBy(() -> connection.removeAccessRequest("accessRequestId1", 3, true)).isInstanceOf(
                StorageDriverException.class
            );
        }
    }

    @Test
    public void checkObjectAvailabilityOK() throws Exception {
        // Given
        StorageCheckObjectAvailabilityRequest request = new StorageCheckObjectAvailabilityRequest(
            3,
            DataCategory.OBJECT.getFolder(),
            Arrays.asList("obj1", "obj2")
        );
        when(mock.get()).thenReturn(
            new RequestResponseOK<>()
                .addResult(new StorageCheckObjectAvailabilityResult(true))
                .setHttpCode(Status.OK.getStatusCode())
                .toResponse()
        );

        // When
        try (Connection connection = driver.connect(offer.getId())) {
            boolean areObjectsAvailable = connection.checkObjectAvailability(request);

            // Then
            assertThat(areObjectsAvailable).isTrue();
        }
    }

    @Test
    public void checkObjectAvailabilityWithServerErrorThenException() throws Exception {
        // Given
        StorageCheckObjectAvailabilityRequest request = new StorageCheckObjectAvailabilityRequest(
            3,
            DataCategory.OBJECT.getFolder(),
            Arrays.asList("obj1", "obj2")
        );

        when(mock.get()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());

        // When / Then
        try (Connection connection = driver.connect(offer.getId())) {
            assertThatThrownBy(() -> connection.checkObjectAvailability(request)).isInstanceOf(
                StorageDriverException.class
            );
        }
    }
}
