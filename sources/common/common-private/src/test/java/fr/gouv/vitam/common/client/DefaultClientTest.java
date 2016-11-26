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
package fr.gouv.vitam.common.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.Future;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.NotSupportedException;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Test;

import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.application.AbstractVitamApplication;
import fr.gouv.vitam.common.server.application.configuration.DefaultVitamApplicationConfiguration;
import fr.gouv.vitam.common.server.application.junit.VitamJerseyTest;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;

public class DefaultClientTest extends VitamJerseyTest {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DefaultClientTest.class);
    private static final String RESOURCE_PATH = "/vitam-test/v1";

    private DefaultClient client;

    // ************************************** //
    // Start of VitamJerseyTest configuration //
    // ************************************** //
    public DefaultClientTest() {
        // The port will be overridden by the VitamJerseyTest
        super(new TestVitamClientFactory<DefaultClient>(1234, RESOURCE_PATH));
    }

    // Override the beforeTest if necessary
    @Override
    public void beforeTest() throws VitamApplicationServerException {
        client = (DefaultClient) getClient();
    }

    // Override the afterTest if necessary
    @Override
    public void afterTest() throws VitamApplicationServerException {
        // Nothing
    }

    // Create the setup application to setup anything necessary
    @Override
    public void setup() {
        // nothing
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
    }
    // ************************************ //
    // End of VitamJerseyTest configuration //
    // ************************************ //


    // Now write your tests
    @Test
    public void statusExecutionWithouthBody() throws Exception {
        when(mock.get()).thenReturn(Response.status(Response.Status.OK).build());
        client.checkStatus();
    }

    @Test
    public void constructorWithGivenClient() throws VitamClientException {
        final Client mock = mock(Client.class);
        final TestVitamClientFactory<DefaultClient> testMockFactory =
            new TestVitamClientFactory<>(getServerPort(), RESOURCE_PATH, mock);
        try (DefaultClient testClient = testMockFactory.getClient()) {
            assertEquals(mock, testClient.getHttpClient());
            assertEquals("http://" + HOSTNAME + ":" + getServerPort() + client.getResourcePath(),
                testClient.getServiceUrl());
        }
    }

    @Test
    public void statusExecutionWithBody() throws Exception {
        when(mock.get()).thenReturn(
            Response.status(Response.Status.NO_CONTENT).build());
        client.checkStatus();
        assertTrue("no exception".length() > 0);
        assertTrue(client.getChunkedMode());
        assertTrue(client.getHttpClient() == client.getHttpClient(true));
        assertTrue(getFactory().getDefaultConfigCient() == getFactory().getDefaultConfigCient(true));
    }

    @Test
    public void statusExecutionThroughPerformRequest() throws Exception {
        when(mock.get()).thenReturn(
            Response.status(Response.Status.OK).entity("{\"pid\":\"1\",\"name\":\"name1\", \"role\":\"role1\"}")
                .build());
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add("X-Test", "testvalue");
        Response message =
            client.performRequest(HttpMethod.GET, BasicClient.STATUS_URL, headers, MediaType.APPLICATION_JSON_TYPE,
                false);
        assertEquals(Response.Status.OK.getStatusCode(), message.getStatus());
        when(mock.get()).thenReturn(
            Response.status(Response.Status.OK).entity("{\"pid\":\"1\",\"name\":\"name1\", \"role\":\"role1\"}")
                .build());
        message = client.performRequest(HttpMethod.GET, BasicClient.STATUS_URL, headers, null, null,
            MediaType.APPLICATION_JSON_TYPE);
        assertEquals(Response.Status.OK.getStatusCode(), message.getStatus());
        when(mock.get()).thenReturn(
            Response.status(Response.Status.OK).entity("{\"pid\":\"1\",\"name\":\"name1\", \"role\":\"role1\"}")
                .build());
        message = client.performRequest(HttpMethod.GET, BasicClient.STATUS_URL, headers, null, null,
            MediaType.APPLICATION_JSON_TYPE, true);
        assertEquals(Response.Status.OK.getStatusCode(), message.getStatus());
        when(mock.get()).thenReturn(
            Response.status(Response.Status.OK).entity("{\"pid\":\"1\",\"name\":\"name1\", \"role\":\"role1\"}")
                .build());
        message = client.performRequest(HttpMethod.GET, BasicClient.STATUS_URL, headers, null, null,
            MediaType.APPLICATION_JSON_TYPE, false);
        assertEquals(Response.Status.OK.getStatusCode(), message.getStatus());
        when(mock.get()).thenReturn(
            Response.status(Response.Status.OK).entity("{\"pid\":\"1\",\"name\":\"name1\", \"role\":\"role1\"}")
                .build());
        message = client.performRequest(HttpMethod.GET, BasicClient.STATUS_URL, headers, "{}",
            MediaType.APPLICATION_JSON_TYPE,
            MediaType.APPLICATION_JSON_TYPE, false);
        assertEquals(Response.Status.OK.getStatusCode(), message.getStatus());
    }

    @Test
    public void statusExecutionThroughPerformAsyncRequest() throws Exception {
        when(mock.get()).thenReturn(
            Response.status(Response.Status.OK).entity("{\"pid\":\"1\",\"name\":\"name1\", \"role\":\"role1\"}")
                .build());
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add("X-Test", "testvalue");
        Future<Response> future = client.performAsyncRequest(HttpMethod.GET, BasicClient.STATUS_URL, headers, null,
            null, MediaType.APPLICATION_JSON_TYPE);
        Response message = future.get();
        assertEquals(Response.Status.OK.getStatusCode(), message.getStatus());
        when(mock.get()).thenReturn(
            Response.status(Response.Status.OK).entity("{\"pid\":\"1\",\"name\":\"name1\", \"role\":\"role1\"}")
                .build());
        future = client.performAsyncRequest(HttpMethod.GET, BasicClient.STATUS_URL, headers, null, null,
            MediaType.APPLICATION_JSON_TYPE,
            new InvocationCallback<Response>() {

                @Override
                public void completed(Response response) {
                    // Completed
                }

                @Override
                public void failed(Throwable throwable) {
                    // Failed
                    SysErrLogger.FAKE_LOGGER.syserr("Failed Status in Async Callback", throwable);
                }
            });
        message = future.get();
        assertEquals(Response.Status.OK.getStatusCode(), message.getStatus());
        when(mock.get()).thenReturn(
            Response.status(Response.Status.OK).entity("{\"pid\":\"1\",\"name\":\"name1\", \"role\":\"role1\"}")
                .build());
        headers.clear();
        headers.add("X-Test", "testvalue");
        future = client.performAsyncRequest(HttpMethod.GET, BasicClient.STATUS_URL, headers, "{}",
            MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
        message = future.get();
        assertEquals(Response.Status.OK.getStatusCode(), message.getStatus());
        when(mock.get()).thenReturn(
            Response.status(Response.Status.OK).entity("{\"pid\":\"1\",\"name\":\"name1\", \"role\":\"role1\"}")
                .build());
        future = client.performAsyncRequest(HttpMethod.GET, BasicClient.STATUS_URL, headers, "{}",
            MediaType.APPLICATION_JSON_TYPE,
            MediaType.APPLICATION_JSON_TYPE,
            new InvocationCallback<Response>() {

                @Override
                public void completed(Response response) {
                    // Completed
                }

                @Override
                public void failed(Throwable throwable) {
                    // Failed
                    SysErrLogger.FAKE_LOGGER.syserr("Failed Status in Async Callback", throwable);
                }
            });
        message = future.get();
        assertEquals(Response.Status.OK.getStatusCode(), message.getStatus());
    }

    @Test(expected = VitamApplicationServerException.class)
    public void failsStatusExecution() throws Exception {
        when(mock.get()).thenReturn(Response.status(Response.Status.SERVICE_UNAVAILABLE).build());
        client.checkStatus();
    }

    @Test
    public void testVariousFails() throws Exception {
        Response response;
        try {
            when(mock.get()).thenThrow(new ForbiddenException());
            response = client.performRequest(HttpMethod.GET, "/status", null, MediaType.APPLICATION_JSON_TYPE);
            assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
        } catch (final Exception e) {
            // Ignore
        }
        try {
            when(mock.get()).thenThrow(new NotAcceptableException());
            response = client.performRequest(HttpMethod.GET, "/status", null, MediaType.APPLICATION_JSON_TYPE);
            assertEquals(Response.Status.NOT_ACCEPTABLE.getStatusCode(), response.getStatus());
        } catch (final Exception e) {
            // Ignore
        }
        try {
            when(mock.get()).thenThrow(new NotAllowedException("POST"));
            response = client.performRequest(HttpMethod.GET, "/status", null, MediaType.APPLICATION_JSON_TYPE);
            assertEquals(Response.Status.METHOD_NOT_ALLOWED.getStatusCode(), response.getStatus());
        } catch (final Exception e) {
            // Ignore
        }
        try {
            when(mock.get()).thenThrow(new NotAuthorizedException(Response.status(Status.UNAUTHORIZED).build()));
            response = client.performRequest(HttpMethod.GET, "/status", null, MediaType.APPLICATION_JSON_TYPE);
            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        } catch (final Exception e) {
            // Ignore
        }
        try {
            when(mock.get()).thenThrow(new NotSupportedException());
            response = client.performRequest(HttpMethod.GET, "/status", null, MediaType.APPLICATION_JSON_TYPE);
            assertEquals(Response.Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode(), response.getStatus());
        } catch (final Exception e) {
            // Ignore
        }
        try {
            when(mock.get()).thenThrow(new ServiceUnavailableException());
            response = client.performRequest(HttpMethod.GET, "/status", null, MediaType.APPLICATION_JSON_TYPE);
            assertEquals(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), response.getStatus());
        } catch (final Exception e) {
            // Ignore
        }
        try {
            when(mock.get()).thenThrow(new NotFoundException());
            response =
                client.performRequest(HttpMethod.GET, "/statusNotFound", null, MediaType.APPLICATION_JSON_TYPE, true);
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        } catch (final Exception e) {
            // Ignore
        }
    }
}
