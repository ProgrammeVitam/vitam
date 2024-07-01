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
package fr.gouv.vitam.common.server.application;

import com.google.common.collect.Sets;
import fr.gouv.vitam.common.client.AbstractMockClient;
import fr.gouv.vitam.common.client.BasicClient;
import fr.gouv.vitam.common.client.DefaultClient;
import fr.gouv.vitam.common.client.TestVitamClientFactory;
import fr.gouv.vitam.common.client.VitamRestTestClient;
import fr.gouv.vitam.common.client.VitamRestTestClient.VitamRestTest;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.application.junit.ResponseHelper;
import fr.gouv.vitam.common.server.application.junit.ResteasyTestApplication;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.serverv2.VitamServerTestRunner;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

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
import java.io.ByteArrayInputStream;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VitamRestApplicationAndClientTest extends ResteasyTestApplication {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(VitamRestApplicationAndClientTest.class);

    private static final String RESOURCE_PATH = "/vitam-test/v1";
    private static final ExpectedResults mock = mock(ExpectedResults.class);

    private static BasicClient client;
    private static VitamRestTestClient testClient;

    static TestVitamClientFactory factory = new TestVitamClientFactory<>(1, RESOURCE_PATH);

    public static VitamServerTestRunner vitamServerTestRunner = new VitamServerTestRunner(
        VitamRestApplicationAndClientTest.class,
        factory
    );

    @BeforeClass
    public static void setUpBeforeClass() throws Throwable {
        vitamServerTestRunner.start();
        client = (DefaultClient) vitamServerTestRunner.getClient();
        testClient = new VitamRestTestClient(factory);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Throwable {
        vitamServerTestRunner.runAfter();
    }

    @Override
    public Set<Object> getResources() {
        return Sets.newHashSet(new MockResource(mock));
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
                helper.writeErrorResponse(Response.status(response.getStatus()).entity("Error").build());
            } else {
                helper.writeResponse(Response.status(response.getStatus()));
            }
        }
    }

    // Now write your tests
    @Test
    public void statusUsingStandardClient() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.NO_CONTENT).build());
        client.checkStatus();
    }

    @Test
    public void statusUsingVitamRestTestClient() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.NO_CONTENT).build());
        final VitamRestTest restTest = testClient
            .given()
            .accept(MediaType.APPLICATION_JSON_TYPE)
            .addHeader("X-Request-Id", "abcd")
            .status(Status.NO_CONTENT)
            .when()
            .then();
        LOGGER.warn(restTest.toString());
        assertEquals(Status.NO_CONTENT.getStatusCode(), restTest.get(ApplicationStatusResource.STATUS_URL));
        when(mock.head()).thenReturn(Response.status(Status.NO_CONTENT).build());
        assertEquals(
            Status.NO_CONTENT.getStatusCode(),
            restTest
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .addHeader("X-Request-Id", "abcd")
                .status(Status.NO_CONTENT)
                .when()
                .head(ApplicationStatusResource.STATUS_URL)
        );
    }

    @Test
    public void badStatusUsingVitamRestTestClient() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.SERVICE_UNAVAILABLE).build());
        final VitamRestTest restTest = testClient
            .given()
            .accept(MediaType.APPLICATION_JSON_TYPE)
            .addHeader("X-Request-Id", "abcd")
            .status(Status.NO_CONTENT)
            .when()
            .then();
        try {
            restTest.get(ApplicationStatusResource.STATUS_URL);
            fail("Should raized an exception");
        } catch (final VitamClientException e) {
            // Ignore
        }
    }

    @Test
    public void variousCommandRestTestClient() throws Exception {
        assertThatCode(() -> {
            when(mock.get()).thenReturn(Response.status(Status.OK).build());
            testClient
                .given()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .addHeader("X-Request-Id", "abcd")
                .addPathParameter("path1", "monid1")
                .addPathParameter("path2", "monid2")
                .body(DEFAULT_XML_CONFIGURATION_FILE, MediaType.TEXT_PLAIN_TYPE)
                .status(Status.OK)
                .when()
                .get("resource");
            when(mock.post()).thenReturn(Response.status(Status.OK).build());
            testClient
                .given()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .addHeader("X-Request-Id", "abcd")
                .addPathParameter("path1", "monid1")
                .addPathParameter("path2", "monid2")
                .body(DEFAULT_XML_CONFIGURATION_FILE, MediaType.TEXT_PLAIN_TYPE)
                .status(Status.OK)
                .when()
                .post("resource");
            when(mock.put()).thenReturn(Response.status(Status.OK).build());
            testClient
                .given()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .addHeader("X-Request-Id", "abcd")
                .addPathParameter("path1", "monid1")
                .addPathParameter("path2", "monid2")
                .body(DEFAULT_XML_CONFIGURATION_FILE, MediaType.TEXT_PLAIN_TYPE)
                .status(Status.OK)
                .when()
                .put("resource");
            when(mock.delete()).thenReturn(Response.status(Status.OK).build());
            testClient
                .given()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .addHeader("X-Request-Id", "abcd")
                .addPathParameter("path1", "monid1")
                .addPathParameter("path2", "monid2")
                .body(DEFAULT_XML_CONFIGURATION_FILE, MediaType.TEXT_PLAIN_TYPE)
                .statusCode(Status.OK.getStatusCode())
                .when()
                .delete("resource");
            when(mock.options()).thenReturn(Response.status(Status.OK).build());
            testClient
                .given()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .addHeader("X-Request-Id", "abcd")
                .addPathParameter("path1", "monid1")
                .addPathParameter("path2", "monid2")
                .status(Status.OK)
                .when()
                .options("resource");
        }).doesNotThrowAnyException();
    }

    @Test
    public void variousCommandWithBodyRestTestClient() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.OK).entity(DEFAULT_XML_CONFIGURATION_FILE).build());
        assertEquals(
            DEFAULT_XML_CONFIGURATION_FILE,
            testClient
                .given()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .addHeader("X-Request-Id", "abcd")
                .addPathParameter("path1", "monid1")
                .addPathParameter("path2", "monid2")
                .body(DEFAULT_XML_CONFIGURATION_FILE, MediaType.TEXT_PLAIN_TYPE)
                .status(Status.OK)
                .when()
                .get("resource", String.class)
        );
        when(mock.post()).thenReturn(Response.status(Status.OK).entity(DEFAULT_XML_CONFIGURATION_FILE).build());
        assertEquals(
            DEFAULT_XML_CONFIGURATION_FILE,
            testClient
                .given()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .addHeader("X-Request-Id", "abcd")
                .addPathParameter("path1", "monid1")
                .addPathParameter("path2", "monid2")
                .body(DEFAULT_XML_CONFIGURATION_FILE, MediaType.TEXT_PLAIN_TYPE)
                .status(Status.OK)
                .when()
                .post("resource", String.class)
        );
        when(mock.put()).thenReturn(Response.status(Status.OK).entity(DEFAULT_XML_CONFIGURATION_FILE).build());
        assertEquals(
            DEFAULT_XML_CONFIGURATION_FILE,
            testClient
                .given()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .addHeader("X-Request-Id", "abcd")
                .addPathParameter("path1", "monid1")
                .addPathParameter("path2", "monid2")
                .body(DEFAULT_XML_CONFIGURATION_FILE, MediaType.TEXT_PLAIN_TYPE)
                .status(Status.OK)
                .when()
                .put("resource", String.class)
        );
        when(mock.delete()).thenReturn(Response.status(Status.OK).entity(DEFAULT_XML_CONFIGURATION_FILE).build());
        assertEquals(
            DEFAULT_XML_CONFIGURATION_FILE,
            testClient
                .given()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .addHeader("X-Request-Id", "abcd")
                .addPathParameter("path1", "monid1")
                .addPathParameter("path2", "monid2")
                .body(DEFAULT_XML_CONFIGURATION_FILE, MediaType.TEXT_PLAIN_TYPE)
                .status(Status.OK)
                .when()
                .delete("resource", String.class)
        );
        when(mock.options()).thenReturn(Response.status(Status.OK).entity(DEFAULT_XML_CONFIGURATION_FILE).build());
        assertEquals(
            DEFAULT_XML_CONFIGURATION_FILE,
            testClient
                .given()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .addHeader("X-Request-Id", "abcd")
                .addPathParameter("path1", "monid1")
                .addPathParameter("path2", "monid2")
                .status(Status.OK)
                .when()
                .options("resource", String.class)
        );
    }

    @Test
    public void asyncCommandWithBodyRestTestClient() throws Exception {
        Response response = ResponseHelper.getOutboundResponse(
            Status.OK,
            new ByteArrayInputStream(DEFAULT_XML_CONFIGURATION_FILE.getBytes()),
            MediaType.TEXT_PLAIN,
            null
        );
        when(mock.post()).thenReturn(response);
        assertEquals(
            DEFAULT_XML_CONFIGURATION_FILE,
            testClient
                .given()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .addHeader("X-Request-Id", "abcd")
                .body(DEFAULT_XML_CONFIGURATION_FILE, MediaType.TEXT_PLAIN_TYPE)
                .status(Status.OK)
                .when()
                .post("resourceasync", String.class)
        );
        response = new AbstractMockClient.FakeInboundResponse(
            Status.OK,
            new ByteArrayInputStream(DEFAULT_XML_CONFIGURATION_FILE.getBytes()),
            MediaType.TEXT_PLAIN_TYPE,
            null
        );
        when(mock.post()).thenReturn(response);
        assertEquals(
            DEFAULT_XML_CONFIGURATION_FILE,
            testClient
                .given()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .addHeader("X-Request-Id", "abcd")
                .body(DEFAULT_XML_CONFIGURATION_FILE, MediaType.TEXT_PLAIN_TYPE)
                .status(Status.OK)
                .when()
                .post("resourceasync", String.class)
        );
        response = ResponseHelper.getOutboundResponse(
            Status.BAD_REQUEST,
            DEFAULT_XML_CONFIGURATION_FILE,
            MediaType.TEXT_PLAIN,
            null
        );
        when(mock.post()).thenReturn(response);
        assertEquals(
            "Error",
            testClient
                .given()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .addHeader("X-Request-Id", "abcd")
                .body(DEFAULT_XML_CONFIGURATION_FILE, MediaType.TEXT_PLAIN_TYPE)
                .status(Status.BAD_REQUEST)
                .when()
                .post("resourceasync", String.class)
        );
        response = ResponseHelper.getOutboundResponse(
            Status.INTERNAL_SERVER_ERROR,
            DEFAULT_XML_CONFIGURATION_FILE,
            MediaType.TEXT_PLAIN,
            null
        );
        when(mock.post()).thenReturn(response);
        // Here no equality since an exception raises a 500 error
        testClient
            .given()
            .accept(MediaType.APPLICATION_JSON_TYPE)
            .addHeader("X-Request-Id", "abcd")
            .body(DEFAULT_XML_CONFIGURATION_FILE, MediaType.TEXT_PLAIN_TYPE)
            .status(Status.INTERNAL_SERVER_ERROR)
            .when()
            .post("resourceasync", String.class);
    }
}
