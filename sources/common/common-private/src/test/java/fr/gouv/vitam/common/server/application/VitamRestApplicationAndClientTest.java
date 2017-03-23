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
package fr.gouv.vitam.common.server.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Test;

import fr.gouv.vitam.common.client.AbstractMockClient;
import fr.gouv.vitam.common.client.BasicClient;
import fr.gouv.vitam.common.client.DefaultClient;
import fr.gouv.vitam.common.client.TestVitamClientFactory;
import fr.gouv.vitam.common.client.VitamRestTestClient;
import fr.gouv.vitam.common.client.VitamRestTestClient.VitamRestTest;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.VitamServerFactory;
import fr.gouv.vitam.common.server.application.configuration.DefaultVitamApplicationConfiguration;
import fr.gouv.vitam.common.server.application.junit.ResponseHelper;
import fr.gouv.vitam.common.server.application.junit.VitamJerseyTest;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;

/**
 * Model for equivalent to JerseyTest and/or usage of restassured like VitamRestTestClient
 */
public class VitamRestApplicationAndClientTest extends VitamJerseyTest {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(VitamRestApplicationAndClientTest.class);

    private static final String RESOURCE_PATH = "/vitam-test/v1";

    private BasicClient client;
    private VitamRestTestClient testClient;

    // ************************************** //
    // Start of VitamJerseyTest configuration //
    // ************************************** //
    public VitamRestApplicationAndClientTest() {
        super(new TestVitamClientFactory<DefaultClient>(VitamServerFactory.getDefaultPort(), RESOURCE_PATH));
    }

    // Override the beforeTest if necessary
    @Override
    public void beforeTest() throws VitamApplicationServerException {
        client = (BasicClient) getClient();
        testClient = new VitamRestTestClient(getFactory());
    }

    // Override the afterTest if necessary
    @Override
    public void afterTest() throws VitamApplicationServerException {
        // Nothing
        if (null != testClient) testClient.close();
    }

    // Create the setup application to setup anything necessary, among the factory
    @Override
    public void setup() {
        // Empty
    }

    // Define the getApplication to return your Application using the correct Configuration
    @Override
    public StartApplicationResponse<AbstractApplication> startVitamApplication(int reservedPort) {
        final TestVitamApplicationConfiguration configuration = new TestVitamApplicationConfiguration();
        configuration.setJettyConfig("jetty-config-benchmark-test.xml");

        final AbstractApplication application = new AbstractApplication(configuration);
        try {
            application.start();
        } catch (final VitamApplicationServerException e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
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

        @Override
        protected boolean registerInAdminConfig(ResourceConfig resourceConfig) {
            // do nothing as @admin is not tested here
            return false;
        }
    }

    // Define your Configuration class if necessary
    public static class TestVitamApplicationConfiguration extends DefaultVitamApplicationConfiguration {

    }

    // Define your Resource class if necessary
    @Path(RESOURCE_PATH)
    @javax.ws.rs.ApplicationPath("webresources")
    public static class MockResource extends ApplicationStatusResource {
        private final ExpectedResults expectedResponse;

        public MockResource(ExpectedResults expectedResponse) {
            this.expectedResponse = expectedResponse;
        }

        @GET
        @Path(STATUS_URL)
        @Produces(MediaType.APPLICATION_JSON)
        @Override
        public Response status() {
            return expectedResponse.get();
        }

        @HEAD
        @Path(STATUS_URL)
        @Produces(MediaType.APPLICATION_JSON)
        public Response head() {
            return expectedResponse.head();
        }

        @POST
        @Path("/resource/path1/{id}/path2/{id2}")
        @Consumes(MediaType.TEXT_PLAIN)
        @Produces(MediaType.APPLICATION_JSON)
        public Response post(String arg) {
            return expectedResponse.post();
        }

        @PUT
        @Path("/resource/path1/{id}/path2/{id2}")
        @Consumes(MediaType.TEXT_PLAIN)
        @Produces(MediaType.APPLICATION_JSON)
        public Response put(String arg) {
            return expectedResponse.put();
        }

        @DELETE
        @Path("/resource/path1/{id}/path2/{id2}")
        @Consumes(MediaType.TEXT_PLAIN)
        @Produces(MediaType.APPLICATION_JSON)
        public Response delete(String arg) {
            return expectedResponse.delete();
        }

        @GET
        @Path("/resource/path1/{id}/path2/{id2}")
        @Consumes(MediaType.TEXT_PLAIN)
        @Produces(MediaType.APPLICATION_JSON)
        public Response get(String arg) {
            return expectedResponse.get();
        }

        @OPTIONS
        @Path("/resource/path1/{id}/path2/{id2}")
        @Produces(MediaType.APPLICATION_JSON)
        public Response options() {
            return expectedResponse.options();
        }

        @POST
        @Path("/resourceasync")
        @Consumes(MediaType.TEXT_PLAIN)
        @Produces(MediaType.APPLICATION_JSON)
        public void postAsync(String arg, @Suspended final AsyncResponse asyncResponse) {
            final Response response = expectedResponse.post();
            final AsyncInputStreamHelper helper = new AsyncInputStreamHelper(asyncResponse, response);
            if (response.getStatus() >= 500) {
                throw new IllegalArgumentException("Error");
            } else if (response.getStatus() >= 400) {
                helper.writeErrorResponse(Response.status(response.getStatus())
                        .entity("Error")
                        .build());
            } else {
                helper.writeResponse(Response.status(response.getStatus()));
            }
        }
    }
    // ************************************ //
    // End of VitamJerseyTest configuration //
    // ************************************ //


    // Now write your tests
    @Test
    public void statusUsingStandardClient() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.NO_CONTENT).build());
        client.checkStatus();
    }

    @Test
    public void statusUsingVitamRestTestClient() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.NO_CONTENT).build());
        final VitamRestTest restTest =
                testClient.given().accept(MediaType.APPLICATION_JSON_TYPE).addHeader("X-Request-Id", "abcd")
                        .status(Status.NO_CONTENT).when().then();
        LOGGER.warn(restTest.toString());
        assertEquals(Status.NO_CONTENT.getStatusCode(),
                restTest.get(ApplicationStatusResource.STATUS_URL));
        when(mock.head()).thenReturn(Response.status(Status.NO_CONTENT).build());
        assertEquals(Status.NO_CONTENT.getStatusCode(),
                restTest.accept(MediaType.APPLICATION_JSON_TYPE).addHeader("X-Request-Id", "abcd")
                        .status(Status.NO_CONTENT).when().head(ApplicationStatusResource.STATUS_URL));
    }

    @Test
    public void badStatusUsingVitamRestTestClient() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.SERVICE_UNAVAILABLE).build());
        final VitamRestTest restTest =
                testClient.given().accept(MediaType.APPLICATION_JSON_TYPE).addHeader("X-Request-Id", "abcd")
                        .status(Status.NO_CONTENT).when().then();
        try {
            restTest.get(ApplicationStatusResource.STATUS_URL);
            fail("Should raized an exception");
        } catch (final VitamClientException e) {
            // Ignore
        }
    }

    @Test
    public void variousCommandRestTestClient() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.OK).build());
        testClient.given().accept(MediaType.APPLICATION_JSON_TYPE)
                .addHeader("X-Request-Id", "abcd")
                .addPathParameter("path1", "monid1").addPathParameter("path2", "monid2")
                .body(DEFAULT_XML_CONFIGURATION_FILE, MediaType.TEXT_PLAIN_TYPE)
                .status(Status.OK).when().get("resource");
        when(mock.post()).thenReturn(Response.status(Status.OK).build());
        testClient.given().accept(MediaType.APPLICATION_JSON_TYPE)
                .addHeader("X-Request-Id", "abcd")
                .addPathParameter("path1", "monid1").addPathParameter("path2", "monid2")
                .body(DEFAULT_XML_CONFIGURATION_FILE, MediaType.TEXT_PLAIN_TYPE)
                .status(Status.OK).when().post("resource");
        when(mock.put()).thenReturn(Response.status(Status.OK).build());
        testClient.given().accept(MediaType.APPLICATION_JSON_TYPE)
                .addHeader("X-Request-Id", "abcd")
                .addPathParameter("path1", "monid1").addPathParameter("path2", "monid2")
                .body(DEFAULT_XML_CONFIGURATION_FILE, MediaType.TEXT_PLAIN_TYPE)
                .status(Status.OK).when().put("resource");
        when(mock.delete()).thenReturn(Response.status(Status.OK).build());
        testClient.given().accept(MediaType.APPLICATION_JSON_TYPE)
                .addHeader("X-Request-Id", "abcd")
                .addPathParameter("path1", "monid1").addPathParameter("path2", "monid2")
                .body(DEFAULT_XML_CONFIGURATION_FILE, MediaType.TEXT_PLAIN_TYPE)
                .statusCode(Status.OK.getStatusCode()).when().delete("resource");
        when(mock.options()).thenReturn(Response.status(Status.OK).build());
        testClient.given().accept(MediaType.APPLICATION_JSON_TYPE)
                .addHeader("X-Request-Id", "abcd")
                .addPathParameter("path1", "monid1").addPathParameter("path2", "monid2")
                .status(Status.OK).when().options("resource");
    }

    @Test
    public void variousCommandWithBodyRestTestClient() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.OK).entity(DEFAULT_XML_CONFIGURATION_FILE).build());
        assertEquals(DEFAULT_XML_CONFIGURATION_FILE,
                testClient.given().accept(MediaType.APPLICATION_JSON_TYPE)
                        .addHeader("X-Request-Id", "abcd")
                        .addPathParameter("path1", "monid1").addPathParameter("path2", "monid2")
                        .body(DEFAULT_XML_CONFIGURATION_FILE, MediaType.TEXT_PLAIN_TYPE)
                        .status(Status.OK).when().get("resource", String.class));
        when(mock.post()).thenReturn(Response.status(Status.OK).entity(DEFAULT_XML_CONFIGURATION_FILE).build());
        assertEquals(DEFAULT_XML_CONFIGURATION_FILE,
                testClient.given().accept(MediaType.APPLICATION_JSON_TYPE)
                        .addHeader("X-Request-Id", "abcd")
                        .addPathParameter("path1", "monid1").addPathParameter("path2", "monid2")
                        .body(DEFAULT_XML_CONFIGURATION_FILE, MediaType.TEXT_PLAIN_TYPE)
                        .status(Status.OK).when().post("resource", String.class));
        when(mock.put()).thenReturn(Response.status(Status.OK).entity(DEFAULT_XML_CONFIGURATION_FILE).build());
        assertEquals(DEFAULT_XML_CONFIGURATION_FILE,
                testClient.given().accept(MediaType.APPLICATION_JSON_TYPE)
                        .addHeader("X-Request-Id", "abcd")
                        .addPathParameter("path1", "monid1").addPathParameter("path2", "monid2")
                        .body(DEFAULT_XML_CONFIGURATION_FILE, MediaType.TEXT_PLAIN_TYPE)
                        .status(Status.OK).when().put("resource", String.class));
        when(mock.delete()).thenReturn(Response.status(Status.OK).entity(DEFAULT_XML_CONFIGURATION_FILE).build());
        assertEquals(DEFAULT_XML_CONFIGURATION_FILE,
                testClient.given().accept(MediaType.APPLICATION_JSON_TYPE)
                        .addHeader("X-Request-Id", "abcd")
                        .addPathParameter("path1", "monid1").addPathParameter("path2", "monid2")
                        .body(DEFAULT_XML_CONFIGURATION_FILE, MediaType.TEXT_PLAIN_TYPE)
                        .status(Status.OK).when().delete("resource", String.class));
        when(mock.options()).thenReturn(Response.status(Status.OK).entity(DEFAULT_XML_CONFIGURATION_FILE).build());
        assertEquals(DEFAULT_XML_CONFIGURATION_FILE,
                testClient.given().accept(MediaType.APPLICATION_JSON_TYPE)
                        .addHeader("X-Request-Id", "abcd")
                        .addPathParameter("path1", "monid1").addPathParameter("path2", "monid2")
                        .status(Status.OK).when().options("resource", String.class));
    }

    @Test
    public void asyncCommandWithBodyRestTestClient() throws Exception {
        Response response = ResponseHelper.getOutboundResponse(Status.OK,
                new ByteArrayInputStream(DEFAULT_XML_CONFIGURATION_FILE.getBytes()), MediaType.TEXT_PLAIN, null);
        when(mock.post()).thenReturn(response);
        assertEquals(DEFAULT_XML_CONFIGURATION_FILE,
                testClient.given().accept(MediaType.APPLICATION_JSON_TYPE)
                        .addHeader("X-Request-Id", "abcd")
                        .body(DEFAULT_XML_CONFIGURATION_FILE, MediaType.TEXT_PLAIN_TYPE)
                        .status(Status.OK).when().post("resourceasync", String.class));
        response = new AbstractMockClient.FakeInboundResponse(Status.OK,
                new ByteArrayInputStream(DEFAULT_XML_CONFIGURATION_FILE.getBytes()), MediaType.TEXT_PLAIN_TYPE, null);
        when(mock.post()).thenReturn(response);
        assertEquals(DEFAULT_XML_CONFIGURATION_FILE,
                testClient.given().accept(MediaType.APPLICATION_JSON_TYPE)
                        .addHeader("X-Request-Id", "abcd")
                        .body(DEFAULT_XML_CONFIGURATION_FILE, MediaType.TEXT_PLAIN_TYPE)
                        .status(Status.OK).when().post("resourceasync", String.class));
        response = ResponseHelper.getOutboundResponse(Status.BAD_REQUEST, DEFAULT_XML_CONFIGURATION_FILE,
                MediaType.TEXT_PLAIN, null);
        when(mock.post()).thenReturn(response);
        assertEquals("Error",
                testClient.given().accept(MediaType.APPLICATION_JSON_TYPE)
                        .addHeader("X-Request-Id", "abcd")
                        .body(DEFAULT_XML_CONFIGURATION_FILE, MediaType.TEXT_PLAIN_TYPE)
                        .status(Status.BAD_REQUEST).when().post("resourceasync", String.class));
        response = ResponseHelper.getOutboundResponse(Status.INTERNAL_SERVER_ERROR, DEFAULT_XML_CONFIGURATION_FILE,
                MediaType.TEXT_PLAIN, null);
        when(mock.post()).thenReturn(response);
        // Here no equality since an exception raises a 500 error
        testClient.given().accept(MediaType.APPLICATION_JSON_TYPE)
                .addHeader("X-Request-Id", "abcd")
                .body(DEFAULT_XML_CONFIGURATION_FILE, MediaType.TEXT_PLAIN_TYPE)
                .status(Status.INTERNAL_SERVER_ERROR).when().post("resourceasync", String.class);
    }

}
