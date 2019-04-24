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
package fr.gouv.vitam.storage.offers.workspace.driver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.stream.IntStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.TestVitamClientFactory;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.FakeInputStream;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.server.application.AbstractVitamApplication;
import fr.gouv.vitam.common.server.application.configuration.DefaultVitamApplicationConfiguration;
import fr.gouv.vitam.common.server.application.junit.VitamJerseyTest;
import fr.gouv.vitam.storage.driver.AbstractConnection;
import fr.gouv.vitam.storage.driver.Driver;
import fr.gouv.vitam.storage.driver.exception.StorageDriverConflictException;
import fr.gouv.vitam.storage.driver.exception.StorageDriverException;
import fr.gouv.vitam.storage.driver.exception.StorageDriverNotFoundException;
import fr.gouv.vitam.storage.driver.exception.StorageDriverPreconditionFailedException;
import fr.gouv.vitam.storage.driver.model.StorageCapacityResult;
import fr.gouv.vitam.storage.driver.model.StorageCheckRequest;
import fr.gouv.vitam.storage.driver.model.StorageCheckResult;
import fr.gouv.vitam.storage.driver.model.StorageOfferLogRequest;
import fr.gouv.vitam.storage.driver.model.StorageGetResult;
import fr.gouv.vitam.storage.driver.model.StorageListRequest;
import fr.gouv.vitam.storage.driver.model.StorageMetadatasResult;
import fr.gouv.vitam.storage.driver.model.StorageObjectRequest;
import fr.gouv.vitam.storage.driver.model.StoragePutRequest;
import fr.gouv.vitam.storage.driver.model.StoragePutResult;
import fr.gouv.vitam.storage.driver.model.StorageRemoveRequest;
import fr.gouv.vitam.storage.driver.model.StorageRemoveResult;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.storage.engine.common.model.request.OfferLogRequest;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageOffer;

public class ConnectionImplTest extends VitamJerseyTest {

    private static final Integer TENANT_ID = 1;
    protected static final String HOSTNAME = "localhost";
    protected static final String DEFAULT_GUID = "GUID";
    private static JunitHelper junitHelper;
    private static int tenant;
    private static AbstractConnection connection;
    private static StorageOffer offer = new StorageOffer();

    private static final String OBJECT_ID = "aeaaaaaaaaaam7mxaa2pkak2bnhxy5aaaaaq";
    private static final String TYPE = "object";
    private static Driver driver;

    public ConnectionImplTest() {
        super(new TestVitamClientFactory(8080, "/offer/v1", mock(Client.class)));
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        junitHelper = JunitHelper.getInstance();
        tenant = Instant.now().getNano();
        driver = DriverImpl.getInstance();
    }

    @Override
    public void beforeTest() throws VitamApplicationServerException {
        String offerId = "default" + new Random().nextDouble();
        offer.setId(offerId);
        offer.setBaseUrl("http://" + HOSTNAME + ":" + getServerPort());
        driver.addOffer(offer, null);
        try {
            connection = (AbstractConnection) driver.connect(offer.getId());
        } catch (final StorageDriverException e) {
            throw new VitamApplicationServerException(e);
        }
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

        @Override
        protected void configureVitamParameters() {
            // None
            VitamConfiguration.setSecret("vitamsecret");
            VitamConfiguration.setFilterActivation(false);
        }

    }


    // Define your Configuration class if necessary
    public static class TestVitamApplicationConfiguration extends DefaultVitamApplicationConfiguration {
    }


    @Path("/offer/v1")
    public static class MockResource {
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

        @HEAD
        @Path("/objects/{type}/{id:.+}")
        public Response headObject(@PathParam("type") DataCategory type, @PathParam("id") String idObject,
            @HeaderParam(GlobalDataRest.X_TENANT_ID) String xTenantId,
            @HeaderParam(GlobalDataRest.X_DIGEST) String xDigest,
            @HeaderParam(GlobalDataRest.X_DIGEST_ALGORITHM) String xDigestAlgorithm) {
            return expectedResponse.head();
        }

        @HEAD
        @Path("/objects/{type}")
        @Produces(MediaType.APPLICATION_JSON)
        public Response getContainerInformation() {
            return expectedResponse.head();
        }

        @GET
        @Path("/objects/{type}")
        @Produces(MediaType.APPLICATION_JSON)
        public Response listContainers() {
            return expectedResponse.get();
        }

        @PUT
        @Path("/objects/{type}/{guid:.+}")
        @Consumes(MediaType.APPLICATION_OCTET_STREAM)
        @Produces(MediaType.APPLICATION_JSON)
        public Response putObject(@PathParam("id") String objectId, InputStream input) {
            return expectedResponse.put();
        }

        @GET
        @Path("/objects/{type}/{id:.+}/metadatas")
        @Produces(MediaType.APPLICATION_JSON)
        public Response getObjectMetadata(@PathParam("type") DataCategory type, @PathParam("id") String idObject,
            @HeaderParam(GlobalDataRest.X_TENANT_ID) String xTenantId) {
            return expectedResponse.get();
        }

        @GET
        @Path("/objects/{type}/{id:.+}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(value = {MediaType.APPLICATION_JSON, MediaType.APPLICATION_OCTET_STREAM})
        public Response getObject(@PathParam("id") String objectId) {
            return expectedResponse.get();
        }

        @DELETE
        @Path("/objects/{type}/{id:.+}")
        @Produces(MediaType.APPLICATION_JSON)
        public Response removeObject(@PathParam("id") String objectId, @PathParam("type") String type) {
            return expectedResponse.delete();
        }

        @GET
        @Path("/objects/{type}/log")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response getOfferLogs(@HeaderParam(GlobalDataRest.X_TENANT_ID) String xTenantId,
            @PathParam("type") String type, OfferLogRequest offerLogRequest) {
            return expectedResponse.get();
        }
    }

    @AfterClass
    public static void shutdownAfterClass() {
        try {
            connection.close();
        } catch (final Exception e) {

        }
    }

    @Test(expected = VitamApplicationServerException.class)
    public void getStatusKO() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        connection.checkStatus();
    }

    @Test
    public void getStatusNoContent() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.NO_CONTENT).build());
        assertNotNull(connection.getServiceUrl());
        connection.checkStatus();
    }

    @Test(expected = StorageDriverException.class)
    public void putObjectWithoutRequestKO() throws Exception {
        connection.putObject(null);
    }

    @Test(expected = StorageDriverException.class)
    public void putObjectWithEmptyRequestKO() throws Exception {
        connection.putObject(new StoragePutRequest(null, null, null, null, null));
    }

    @Test(expected = StorageDriverException.class)
    public void putObjectRequestWithOnlyMissingTenantIdKO() throws Exception {
        final StoragePutRequest request = getPutObjectRequest(true, true, true, false, true);
        connection.putObject(request);
    }

    @Test(expected = StorageDriverException.class)
    public void putObjectRequestWithOnlyMissingDataStreamKO() throws Exception {
        final StoragePutRequest request = getPutObjectRequest(false, true, true, true, true);
        connection.putObject(request);
    }

    @Test(expected = StorageDriverException.class)
    public void putObjectRequestWithOnlyMissingAlgortihmKO() throws Exception {
        final StoragePutRequest request = getPutObjectRequest(true, false, true, true, true);
        connection.putObject(request);
    }

    @Test(expected = StorageDriverException.class)
    public void putObjectRequestWithOnlyMissingGuidKO() throws Exception {
        final StoragePutRequest request = getPutObjectRequest(true, true, false, true, true);
        connection.putObject(request);
    }

    @Test(expected = StorageDriverException.class)
    public void putObjectRequestWithOnlyMissingTypeKO() throws Exception {
        final StoragePutRequest request = getPutObjectRequest(true, true, true, true, false);
        connection.putObject(request);
    }

    @Test
    public void putObjectWithRequestOK() throws Exception {
        final StoragePutRequest request = getPutObjectRequest(true, true, true, true, true);
        when(mock.put()).thenReturn(Response.status(Status.CREATED).entity(getPutObjectResult(0)).build())
            .thenReturn(Response.status(Status.CREATED).entity(getPutObjectResult(1)).build())
            .thenReturn(Response.status(Status.CREATED).entity(getPutObjectResult(2)).build())
            .thenReturn(Response.status(Status.CREATED).entity(getPutObjectResult(3)).build())
            .thenReturn(Response.status(Status.CREATED).entity(getPutObjectResult(4)).build())
            .thenReturn(Response.status(Status.CREATED).entity(getPutObjectResult(5)).build())
            .thenReturn(Response.status(Status.CREATED).entity(getPutObjectResult(6)).build())
            .thenReturn(Response.status(Status.CREATED).entity(getPutObjectResult(7)).build());
        final StoragePutResult result = connection.putObject(request);
        assertNotNull(result);
        assertNotNull(result.getDistantObjectId());
        assertNotNull(result.getDigestHashBase16());
    }

    // chunk size (1024) factor size case
    @Test
    public void putBigObjectWithRequestOk() throws Exception {
        final StoragePutRequest request = new StoragePutRequest(1, DataCategory.OBJECT.getFolder(), "GUID",
            DigestType.MD5.getName(), new FakeInputStream(2097152));
        when(mock.put()).thenReturn(Response.status(Status.CREATED).entity(getPutObjectResult(0)).build())
            .thenReturn(Response.status(Status.CREATED).entity(getPutObjectResult(1)).build())
            .thenReturn(Response.status(Status.CREATED).entity(getPutObjectResult(2)).build())
            .thenReturn(Response.status(Status.CREATED).entity(getPutObjectResult(3)).build())
            .thenReturn(Response.status(Status.CREATED).entity(getPutObjectResult(4)).build())
            .thenReturn(Response.status(Status.CREATED).entity(getPutObjectResult(5)).build())
            .thenReturn(Response.status(Status.CREATED).entity(getPutObjectResult(6)).build());
        final StoragePutResult result = connection.putObject(request);
        assertNotNull(result);
        assertNotNull(result.getDistantObjectId());
        assertNotNull(result.getDigestHashBase16());
    }

    // No chunk size (1024) factor case
    @Test
    public void putBigObject2WithRequestOk() throws Exception {
        final StoragePutRequest request = new StoragePutRequest(tenant, DataCategory.OBJECT.getFolder(), "GUID",
            DigestType.MD5.getName(), new FakeInputStream(2201507));
        when(mock.put()).thenReturn(Response.status(Status.CREATED).entity(getPutObjectResult(0)).build())
            .thenReturn(Response.status(Status.CREATED).entity(getPutObjectResult(1)).build())
            .thenReturn(Response.status(Status.CREATED).entity(getPutObjectResult(2)).build())
            .thenReturn(Response.status(Status.CREATED).entity(getPutObjectResult(3)).build())
            .thenReturn(Response.status(Status.CREATED).entity(getPutObjectResult(4)).build())
            .thenReturn(Response.status(Status.CREATED).entity(getPutObjectResult(5)).build())
            .thenReturn(Response.status(Status.CREATED).entity(getPutObjectResult(6)).build());
        final StoragePutResult result = connection.putObject(request);
        assertNotNull(result);
        assertNotNull(result.getDistantObjectId());
        assertNotNull(result.getDigestHashBase16());
    }

    @Test(expected = StorageDriverException.class)
    public void putObjectWithRequestThrowsNotFoundErrorOnPutKO() throws Exception {
        final StoragePutRequest request = getPutObjectRequest(true, true, true, true, true);
        when(mock.put()).thenReturn(Response.status(Status.NOT_FOUND).build());
        connection.putObject(request);
    }

    @Test(expected = StorageDriverException.class)
    public void putObjectWithRequestThrowsOtherErrorOnPutKO() throws Exception {
        final StoragePutRequest request = getPutObjectRequest(true, true, true, true, true);
        when(mock.put()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        connection.putObject(request);
    }

    @Test(expected = StorageDriverException.class)
    public void putObjectWithRequestThrowsInternalServerErrorOnPutOK() throws Exception {
        final StoragePutRequest request = getPutObjectRequest(true, true, true, true, true);
        when(mock.put()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        connection.putObject(request);
    }

    @Test(expected = StorageDriverException.class)
    public void putObjectThrowsOtherException() throws Exception {
        when(mock.put()).thenReturn(Response.status(Status.SERVICE_UNAVAILABLE).build());
        final StoragePutRequest request = getPutObjectRequest(true, true, true, true, true);
        connection.putObject(request);
    }

    @Test(expected = StorageDriverConflictException.class)
    public void putObjectThrowsConflictException() throws Exception {
        when(mock.put()).thenReturn(Response.status(Status.CONFLICT).build());
        final StoragePutRequest request = getPutObjectRequest(true, true, true, true, true);
        connection.putObject(request);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getGetObjectRequestIllegalArgumentException() throws Exception {
        connection.getObject(null);
    }

    @Test
    public void getStorageCapacityOK() throws Exception {
        when(mock.head())
            .thenReturn(Response.status(Status.OK).header("X-Usable-Space", "1000").header("X-Used-Space", "1000")
                .header(GlobalDataRest.X_TENANT_ID, tenant).build());
        // TODO check result
        final StorageCapacityResult result = connection.getStorageCapacity(tenant);
        assertNotNull(result);
        assertEquals(Integer.valueOf(tenant), result.getTenantId());
        assertNotNull(result.getUsableSpace());
        assertNotNull(result.getUsedSpace());
    }

    @Test(expected = StorageDriverException.class)
    public void getStorageCapacityException() throws Exception {
        when(mock.head()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        connection.getStorageCapacity(0);
    }

    @Test(expected = StorageDriverNotFoundException.class)
    public void getStorageCapacityNotFoundException() throws Exception {
        when(mock.head()).thenReturn(Response.status(Status.NOT_FOUND).build());
        connection.getStorageCapacity(0);
    }

    @Test(expected = StorageDriverPreconditionFailedException.class)
    public void getStorageCapacityPreconditionFailedException() throws Exception {
        when(mock.head()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        connection.getStorageCapacity(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getGetObjectGUIDIllegalArgumentException() throws Exception {
        final StorageObjectRequest request = new StorageObjectRequest(tenant, DataCategory.OBJECT.getFolder(), null);
        connection.getObject(request);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getGetObjectTypeIllegalArgumentException() throws Exception {
        final StorageObjectRequest request = new StorageObjectRequest(null, DataCategory.OBJECT.getFolder(), "guid");
        connection.getObject(request);
    }

    @Test
    public void getObjectNotFound() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        final StorageObjectRequest request = new StorageObjectRequest(tenant, DataCategory.OBJECT.getFolder(), "guid");
        try {
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
        try {
            connection.getObject(request);
            fail("Expected exception");
        } catch (final StorageDriverException exc) {
            assertEquals(StorageDriverException.class, exc.getClass());
        }
    }

    @Test
    public void getObjectPreconditionFailed() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        final StorageObjectRequest request = new StorageObjectRequest(tenant, DataCategory.OBJECT.getFolder(), "guid");
        try {
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
        final StorageGetResult result = connection.getObject(request);
        assertNotNull(result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void objectExistInOfferWithEmptyParameterThrowsException() throws Exception {
        connection.objectExistsInOffer(null);
    }


    @Test
    public void objectExistInOfferInternalServerError() throws Exception {
        when(mock.head()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        final StorageObjectRequest request = new StorageObjectRequest(tenant, DataCategory.OBJECT.getFolder(), "guid");
        try {
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
        try {
            final boolean found = connection.objectExistsInOffer(request);
            assertEquals(false, found);
        } catch (final StorageDriverException exc) {
            fail("Ne exception expected");
        }
    }

    @Test
    public void objectExistInOfferFound() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.OK).build());
        final StorageObjectRequest request = new StorageObjectRequest(tenant, DataCategory.OBJECT.getFolder(), "guid");
        try {
            final boolean found = connection.objectExistsInOffer(request);
            assertEquals(true, found);
        } catch (final StorageDriverException exc) {
            fail("Ne exception expected");
        }
    }

    @Test
    public void objectExistInOfferPreconditionFailed() throws Exception {
        when(mock.head()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        final StorageObjectRequest request = new StorageObjectRequest(tenant, DataCategory.OBJECT.getFolder(), "guid");
        try {
            connection.objectExistsInOffer(request);
            fail("Expected exception");
        } catch (final StorageDriverException exc) {
            assertEquals(StorageDriverPreconditionFailedException.class, exc.getClass());
        }
    }

    @Test
    public void checkObjectTestOK() throws Exception {
        when(mock.head()).thenReturn(Response.status(Status.OK).build());
        StorageCheckResult storageCheckResult =
            connection.checkObject(getStorageCheckRequest(true, true, true, true, true));
        assertNotNull(storageCheckResult);
        assertEquals(true, storageCheckResult.isDigestMatch());
    }


    @Test
    public void checkObjectTestIllegalArgument() throws Exception {
        try {
            connection.checkObject(null);
            fail("Should raized an exception");
        } catch (IllegalArgumentException e) {

        }
        try {
            connection.checkObject(getStorageCheckRequest(false, true, true, true, true));
            fail("Should raized an exception");
        } catch (IllegalArgumentException e) {

        }
        try {
            connection.checkObject(getStorageCheckRequest(true, false, true, true, true));
            fail("Should raized an exception");
        } catch (IllegalArgumentException e) {

        }
        try {
            connection.checkObject(getStorageCheckRequest(true, true, false, true, true));
            fail("Should raized an exception");
        } catch (IllegalArgumentException e) {

        }
        try {
            connection.checkObject(getStorageCheckRequest(true, true, true, false, true));
            fail("Should raized an exception");
        } catch (IllegalArgumentException e) {

        }
        try {
            connection.checkObject(getStorageCheckRequest(true, true, true, true, false));
            fail("Should raized an exception");
        } catch (IllegalArgumentException e) {

        }
    }

    @Test(expected = StorageDriverException.class)
    public void checkObjectTestNotFound() throws Exception {
        when(mock.head()).thenReturn(Response.status(Status.NOT_FOUND).build());
        connection.checkObject(getStorageCheckRequest(true, true, true, true, true));
    }

    @Test(expected = StorageDriverException.class)
    public void checkObjectTestPreconditionFailed() throws Exception {
        when(mock.head()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        connection.checkObject(getStorageCheckRequest(true, true, true, true, true));
    }

    @Test(expected = StorageDriverException.class)
    public void checkObjectTestInternalServerError() throws Exception {
        when(mock.head()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        connection.checkObject(getStorageCheckRequest(true, true, true, true, true));
    }

    private StoragePutRequest getPutObjectRequest(boolean putDataS, boolean putDigestA, boolean putGuid,
        boolean putTenantId,
        boolean putType)
        throws Exception {
        FileInputStream stream = null;
        String digest = null;
        String guid = null;
        Integer tenantId = null;
        String type = null;

        if (putDataS) {
            stream = new FileInputStream(PropertiesUtils.findFile("digitalObject.pdf"));
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

    private StorageCheckRequest getStorageCheckRequest(boolean putDigestType, boolean putDigestA, boolean putGuid,
        boolean putTenantId, boolean putType)
        throws Exception {
        DigestType digestType = null;
        String digest = null;
        String guid = null;
        Integer tenantId = null;
        String type = null;

        if (putDigestType) {
            digestType = VitamConfiguration.getDefaultDigestType();
        }
        if (putDigestA) {
            digest = "digest";
        }
        if (putGuid) {
            guid = "GUID";
        }
        if (putTenantId) {
            tenantId = tenant;
        }
        if (putType) {
            type = DataCategory.OBJECT.getFolder();
        }
        return new StorageCheckRequest(tenantId, type, guid, digestType, digest);
    }

    private JsonNode getPutObjectResult(int uniqueId) throws JsonProcessingException, IOException {
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree("{\"digest\":\"aaakkkk" + uniqueId + "\",\"size\":\"666\"}");
    }

    @Test
    public void deleteObjectTestIllegalArgument() throws Exception {
        try {
            connection.removeObject(null);
            fail("Should raized an exception");
        } catch (IllegalArgumentException e) {

        }
        try {
            connection.removeObject(getStorageRemoveRequest(false, true, true, true, true));
            fail("Should raized an exception");
        } catch (IllegalArgumentException e) {

        }
        try {
            connection.removeObject(getStorageRemoveRequest(true, false, true, true, true));
            fail("Should raized an exception");
        } catch (IllegalArgumentException e) {

        }
        try {
            connection.removeObject(getStorageRemoveRequest(true, true, false, true, true));
            fail("Should raized an exception");
        } catch (IllegalArgumentException e) {

        }
        try {
            connection.removeObject(getStorageRemoveRequest(true, true, true, false, true));
            fail("Should raized an exception");
        } catch (IllegalArgumentException e) {

        }
        try {
            connection.removeObject(getStorageRemoveRequest(true, true, true, true, false));
            fail("Should raized an exception");
        } catch (IllegalArgumentException e) {

        }
    }

    @Test
    public void deleteObjectTestOK() throws Exception {
        when(mock.delete()).thenReturn(Response.status(Status.OK).entity(getRemoveObjectResult()).build());
        StorageRemoveResult storageRemoveResult =
            connection.removeObject(getStorageRemoveRequest(true, true, true, true, true));
        assertNotNull(storageRemoveResult);
        assertTrue(storageRemoveResult.isObjectDeleted());

        when(mock.delete()).thenReturn(Response.status(Status.OK).entity(getRemoveObjectResultNotFound()).build());
        StorageRemoveResult storageRemoveResult2 =
            connection.removeObject(getStorageRemoveRequest(true, true, true, true, true));
        assertNotNull(storageRemoveResult2);
        assertTrue(!storageRemoveResult2.isObjectDeleted());
    }

    @Test(expected = StorageDriverException.class)
    public void deleteObjectTestNotFound() throws Exception {
        when(mock.delete()).thenReturn(Response.status(Status.NOT_FOUND).build());
        connection.removeObject(getStorageRemoveRequest(true, true, true, true, true));
    }

    @Test(expected = StorageDriverException.class)
    public void deleteObjectTestPreconditionFailed() throws Exception {
        when(mock.delete()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        connection.removeObject(getStorageRemoveRequest(true, true, true, true, true));
    }

    @Test(expected = StorageDriverException.class)
    public void deleteObjectTestBadRequest() throws Exception {
        when(mock.delete()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        connection.removeObject(getStorageRemoveRequest(true, true, true, true, true));
    }

    @Test(expected = StorageDriverException.class)
    public void deleteObjectTestInternalServerError() throws Exception {
        when(mock.delete()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        connection.removeObject(getStorageRemoveRequest(true, true, true, true, true));
    }

    @Test
    public void listObjectsTest() throws Exception {
        StorageListRequest storageRequest =
            new StorageListRequest(TENANT_ID, DataCategory.OBJECT.getFolder(), null, true);
        when(mock.get()).thenReturn(Response.status(Status.OK).build());
        RequestResponse<JsonNode> jsonNodeRequestResponse = connection.listObjects(storageRequest);
        assertNotNull(jsonNodeRequestResponse);
    }

    private StorageRemoveRequest getStorageRemoveRequest(boolean putDigestType, boolean putDigestA, boolean putGuid,
        boolean putTenantId, boolean putType)
        throws Exception {
        DigestType digestType = null;
        String digest = null;
        String guid = null;
        Integer tenantId = null;
        String type = null;

        if (putDigestType) {
            digestType = VitamConfiguration.getDefaultDigestType();
        }
        if (putDigestA) {
            digest = "digest";
        }
        if (putGuid) {
            guid = DEFAULT_GUID;
        }
        if (putTenantId) {
            tenantId = tenant;
        }
        if (putType) {
            type = DataCategory.OBJECT.getFolder();
        }
        return new StorageRemoveRequest(tenantId, type, guid, digestType, digest);
    }

    private JsonNode getRemoveObjectResult() throws IOException {
        final ObjectNode result = JsonHandler.createObjectNode();
        result.put("id", DEFAULT_GUID);
        result.put("status", Response.Status.OK.toString());
        return result;
    }

    private JsonNode getRemoveObjectResultNotFound() throws IOException {
        final ObjectNode result = JsonHandler.createObjectNode();
        result.put("id", DEFAULT_GUID);
        result.put("status", Response.Status.NOT_FOUND.toString());
        return result;
    }

    @Test
    public void getObjectMetadataTestOK() throws StorageDriverException {
        when(mock.get()).thenReturn(Response.status(Status.OK).entity(mockMetadatasObjectResult()).build());
        final StorageObjectRequest request = new StorageObjectRequest(tenant, DataCategory.OBJECT.getFolder(), "guid");
        final StorageMetadatasResult result = connection.getMetadatas(request);
        assertNotNull(result);

    }

    @Test(expected = StorageDriverNotFoundException.class)
    public void getObjectMetadataTestTestNotFound() throws StorageDriverException {
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        final StorageObjectRequest request =
            new StorageObjectRequest(tenant, DataCategory.OBJECT.getFolder(), "guid");
        connection.getMetadatas(request);
    }

    @Test(expected = StorageDriverException.class)
    public void getObjectMetadataTestInternalServerError() throws StorageDriverException {
        when(mock.get()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        final StorageObjectRequest request =
            new StorageObjectRequest(tenant, DataCategory.OBJECT.getFolder(), "guid");
        connection.getMetadatas(request);
    }



    @Test
    public void getOfferLogsOK() throws Exception {

        RequestResponseOK<OfferLog> requestResponse = new RequestResponseOK<>();
        IntStream.range(1, 11).forEach(sequence -> {
            OfferLog offerLog = new OfferLog();
            offerLog.setContainer(DataCategory.OBJECT.getFolder() + "_" + tenant);
            offerLog.setFileName(GUIDFactory.newGUID().getId());
            offerLog.setSequence(sequence);
            offerLog.setTime(LocalDateTime.now());
            requestResponse.addResult(offerLog);
        });
        requestResponse.setHttpCode(Status.OK.getStatusCode());

        when(mock.get()).thenReturn(
            Response.status(Status.OK).header(GlobalDataRest.X_TENANT_ID, tenant).entity(JsonHandler.writeAsString(requestResponse)).build());

        StorageOfferLogRequest offerLogRequest =
            new StorageOfferLogRequest(tenant, DataCategory.OBJECT.getFolder(), 2L, 10, Order.ASC);
        final RequestResponse<OfferLog> result = connection.getOfferLogs(offerLogRequest);
        assertNotNull(result);
        assertEquals(String.valueOf(tenant), result.getHeaderString(GlobalDataRest.X_TENANT_ID));
        assertEquals(true, result.isOk());
        assertEquals(Status.OK.getStatusCode(), result.getHttpCode());
        RequestResponseOK<OfferLog> resultOK = (RequestResponseOK<OfferLog>) result;
        assertEquals(10, resultOK.getResults().size());
    }

    @Test
    public void getOfferLogsInternalServerError() throws Exception {
        when(mock.get()).thenReturn(
            Response.status(Status.INTERNAL_SERVER_ERROR).header(GlobalDataRest.X_TENANT_ID, tenant).build());
        StorageOfferLogRequest offerLogRequest =
            new StorageOfferLogRequest(tenant, DataCategory.OBJECT.getFolder(), 2L, 10, Order.ASC);
        final RequestResponse<OfferLog> result = connection.getOfferLogs(offerLogRequest);
        assertNotNull(result);
        assertEquals(false, result.isOk());
        assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), result.getHttpCode());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getOfferLogsInvalidRequest() throws Exception {
        StorageOfferLogRequest offerLogRequest = new StorageOfferLogRequest(tenant, null, 2L, 10, Order.ASC);
        connection.getOfferLogs(offerLogRequest);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getOfferLogsInvalidRequestOrder() throws Exception {
        StorageOfferLogRequest offerLogRequest = new StorageOfferLogRequest(tenant, DataCategory.OBJECT.getFolder(), 2L, 10, null);
        connection.getOfferLogs(offerLogRequest);
    }

    private StorageMetadatasResult mockMetadatasObjectResult() {
        return new StorageMetadatasResult(OBJECT_ID, TYPE, "abcdef", 6096,
            "Vitam_0", "Tue Aug 31 10:20:56 SGT 2016", "Tue Aug 31 10:20:56 SGT 2016");
    }

}
