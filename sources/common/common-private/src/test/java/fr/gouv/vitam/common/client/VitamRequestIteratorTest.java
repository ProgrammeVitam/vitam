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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.server.application.AbstractVitamApplication;
import fr.gouv.vitam.common.server.application.configuration.DefaultVitamApplicationConfiguration;
import fr.gouv.vitam.common.server.application.junit.VitamJerseyTest;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;

public class VitamRequestIteratorTest extends VitamJerseyTest {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(VitamRequestIteratorTest.class);

    private static final String RESOURCE_PATH = "/vitam-test/v1";

    private DefaultClient client;
    private static boolean startup = true;

    // ************************************** //
    // Start of VitamJerseyTest configuration //
    // ************************************** //
    public VitamRequestIteratorTest() {
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
        @Path("/iterator")
        @Produces(MediaType.APPLICATION_JSON)
        public Response iterator(@Context HttpHeaders headers) {
            final Response response = expectedResponse.get();
            final boolean checkStart = VitamRequestIterator.isNewCursor(headers);
            VitamRequestIterator.isEndOfCursor(headers);
            assertEquals(startup, checkStart);
            startup = false;
            return response;
        }


    }
    // ************************************ //
    // End of VitamJerseyTest configuration //
    // ************************************ //

    @Test
    public void testIterator() {
        startup = true;
        try (VitamRequestIterator<ObjectNode> iterator =
            new VitamRequestIterator<>(client, HttpMethod.GET, "/iterator", ObjectNode.class, null, null)) {
            final RequestResponseOK response = new RequestResponseOK(JsonHandler.createObjectNode());
            final ObjectNode node1 = JsonHandler.createObjectNode().put("val", 1);
            final ObjectNode node2 = JsonHandler.createObjectNode().put("val", 2);
            final ObjectNode node3 = JsonHandler.createObjectNode().put("val", 3);
            response.addResult(node1);
            final List<ObjectNode> list = new ArrayList<>();
            list.add(node2);
            list.add(node3);
            response.addAllResults(list);
            ResponseBuilder builder = Response.status(Status.PARTIAL_CONTENT);
            when(mock.get())
                .thenReturn(VitamRequestIterator.setHeaders(builder, true, "newcursor").entity(response).build());
            for (int i = 0; i < 3; i++) {
                assertTrue(iterator.hasNext());
                final ObjectNode node = iterator.next();
                assertNotNull(node);
                assertEquals(i + 1, node.get("val").asInt());
            }
            builder = Response.status(Status.OK);
            when(mock.get())
                .thenReturn(VitamRequestIterator.setHeaders(builder, true, "newcursor").entity(response).build());
            for (int i = 0; i < 3; i++) {
                assertTrue(iterator.hasNext());
                final JsonNode node = iterator.next();
                assertNotNull(node);
                assertEquals(i + 1, node.get("val").asInt());
            }
            // Ensure next hasNext should be False without exception
            when(mock.get()).thenReturn(Response.status(Status.BAD_REQUEST.getStatusCode()).build());
            assertFalse(iterator.hasNext());
        }
    }

    @Test
    public void testIteratorEmpty() {
        startup = true;
        try (VitamRequestIterator iterator =
            new VitamRequestIterator(client, HttpMethod.GET, "/iterator", ObjectNode.class, null, null)) {
            final RequestResponseOK response = new RequestResponseOK();
            final ResponseBuilder builder = Response.status(Status.NOT_FOUND);
            when(mock.get())
                .thenReturn(VitamRequestIterator.setHeaders(builder, true, null).entity(response).build());
            assertFalse(iterator.hasNext());
        }
    }

    @Test
    public void testIteratorFailed() {
        startup = true;
        try (VitamRequestIterator iterator =
            new VitamRequestIterator(client, HttpMethod.GET, "/iterator", ObjectNode.class, null, null)) {
            final RequestResponseOK response = new RequestResponseOK();
            final ResponseBuilder builder = Response.status(Status.BAD_REQUEST);
            when(mock.get())
                .thenReturn(VitamRequestIterator.setHeaders(builder, true, null).entity(response).build());
            try {
                assertFalse(iterator.hasNext());
                fail("should raized an exception");
            } catch (final BadRequestException e) {

            }
        }
    }

    @Test
    public void testIteratorShortList() {
        startup = true;
        try (VitamRequestIterator<ObjectNode> iterator =
            new VitamRequestIterator<>(client, HttpMethod.GET, "/iterator", ObjectNode.class, null, null)) {
            final RequestResponseOK response = new RequestResponseOK(JsonHandler.createObjectNode());
            final ObjectNode node1 = JsonHandler.createObjectNode().put("val", 1);
            final ObjectNode node2 = JsonHandler.createObjectNode().put("val", 2);
            final ObjectNode node3 = JsonHandler.createObjectNode().put("val", 3);
            response.addResult(node1);
            final List<ObjectNode> list = new ArrayList<>();
            list.add(node2);
            list.add(node3);
            response.addAllResults(list);
            final ResponseBuilder builder = Response.status(Status.OK);
            when(mock.get())
                .thenReturn(VitamRequestIterator.setHeaders(builder, true, null).entity(response).build());
            for (int i = 0; i < 3; i++) {
                assertTrue(iterator.hasNext());
                final ObjectNode node = iterator.next();
                assertNotNull(node);
                assertEquals(i + 1, node.get("val").asInt());
            }
            assertFalse(iterator.hasNext());
        }
    }

    @Test
    public void testIteratorThirdShort() {
        startup = true;
        try (VitamRequestIterator<ObjectNode> iterator =
            new VitamRequestIterator<>(client, HttpMethod.GET, "/iterator", ObjectNode.class, null, null)) {
            final RequestResponseOK response = new RequestResponseOK(JsonHandler.createObjectNode());
            final ObjectNode node1 = JsonHandler.createObjectNode().put("val", 1);
            final ObjectNode node2 = JsonHandler.createObjectNode().put("val", 2);
            final ObjectNode node3 = JsonHandler.createObjectNode().put("val", 3);
            response.addResult(node1);
            final List<ObjectNode> list = new ArrayList<>();
            list.add(node2);
            list.add(node3);
            response.addAllResults(list);
            ResponseBuilder builder = Response.status(Status.PARTIAL_CONTENT);
            when(mock.get())
                .thenReturn(VitamRequestIterator.setHeaders(builder, true, "newcursor").entity(response).build());
            for (int i = 0; i < 3; i++) {
                assertTrue(iterator.hasNext());
                final ObjectNode node = iterator.next();
                assertNotNull(node);
                assertEquals(i + 1, node.get("val").asInt());
            }
            when(mock.get())
                .thenReturn(VitamRequestIterator.setHeaders(builder, true, "newcursor").entity(response).build());
            for (int i = 0; i < 3; i++) {
                assertTrue(iterator.hasNext());
                final ObjectNode node = iterator.next();
                assertNotNull(node);
                assertEquals(i + 1, node.get("val").asInt());
            }
            builder = Response.status(Status.NOT_FOUND);
            when(mock.get())
                .thenReturn(VitamRequestIterator.setHeaders(builder, true, "newcursor").build());
            assertFalse(iterator.hasNext());
        }
    }

    @Test
    public void testIteratorStopBefore() {
        startup = true;
        try (VitamRequestIterator iterator =
            new VitamRequestIterator(client, HttpMethod.GET, "/iterator", ObjectNode.class, null, null)) {
            final RequestResponseOK response = new RequestResponseOK(JsonHandler.createObjectNode());
            final ObjectNode node1 = JsonHandler.createObjectNode().put("val", 1);
            final ObjectNode node2 = JsonHandler.createObjectNode().put("val", 2);
            final ObjectNode node3 = JsonHandler.createObjectNode().put("val", 3);
            response.addResult(node1);
            final List<ObjectNode> list = new ArrayList<>();
            list.add(node2);
            list.add(node3);
            response.addAllResults(list);
            final ResponseBuilder builder = Response.status(Status.PARTIAL_CONTENT);
            when(mock.get())
                .thenReturn(VitamRequestIterator.setHeaders(builder, true, "newcursor").entity(response).build());
            iterator.close();
            assertFalse(iterator.hasNext());
        }
        startup = true;
        try (VitamRequestIterator<ObjectNode> iterator =
            new VitamRequestIterator<>(client, HttpMethod.GET, "/iterator", ObjectNode.class, null, null)) {
            final RequestResponseOK response = new RequestResponseOK(JsonHandler.createObjectNode());
            final ObjectNode node1 = JsonHandler.createObjectNode().put("val", 1);
            final ObjectNode node2 = JsonHandler.createObjectNode().put("val", 2);
            final ObjectNode node3 = JsonHandler.createObjectNode().put("val", 3);
            response.addResult(node1);
            final List<ObjectNode> list = new ArrayList<>();
            list.add(node2);
            list.add(node3);
            response.addAllResults(list);
            final ResponseBuilder builder = Response.status(Status.PARTIAL_CONTENT);
            when(mock.get())
                .thenReturn(VitamRequestIterator.setHeaders(builder, true, "newcursor").entity(response).build());
            for (int i = 0; i < 3; i++) {
                assertTrue(iterator.hasNext());
                final ObjectNode node = iterator.next();
                assertNotNull(node);
                assertEquals(i + 1, node.get("val").asInt());
            }
            when(mock.get())
                .thenReturn(VitamRequestIterator.setHeaders(builder, true, "newcursor").entity(response).build());
            iterator.close();
            assertFalse(iterator.hasNext());
        }
    }

    @Test
    public void testStaticMethod() {
        assertTrue(VitamRequestIterator.isEndOfCursor(false, "test"));
        assertFalse(VitamRequestIterator.isEndOfCursor(true, "test"));
        assertFalse(VitamRequestIterator.isEndOfCursor(false, ""));
        assertFalse(VitamRequestIterator.isEndOfCursor(true, ""));

        assertTrue(VitamRequestIterator.isNewCursor(true, ""));
        assertFalse(VitamRequestIterator.isNewCursor(true, "test"));
        assertFalse(VitamRequestIterator.isNewCursor(false, ""));
        assertFalse(VitamRequestIterator.isNewCursor(false, "test"));

        final MultivaluedMap<String, String> map = new MultivaluedHashMap<>();

        final HttpHeaders headers = new HttpHeaders() {

            @Override
            public MultivaluedMap<String, String> getRequestHeaders() {
                return map;
            }

            @Override
            public List<String> getRequestHeader(String name) {
                return map.get(name);
            }

            @Override
            public MediaType getMediaType() {
                return null;
            }

            @Override
            public int getLength() {
                return 0;
            }

            @Override
            public Locale getLanguage() {
                return null;
            }

            @Override
            public String getHeaderString(String name) {
                return map.get(name).get(0);
            }

            @Override
            public Date getDate() {
                return null;
            }

            @Override
            public Map<String, Cookie> getCookies() {
                return null;
            }

            @Override
            public List<MediaType> getAcceptableMediaTypes() {
                return null;
            }

            @Override
            public List<Locale> getAcceptableLanguages() {
                return null;
            }
        };

        // empty
        try {
            VitamRequestIterator.isEndOfCursor(headers);
            fail("Should raized an exception");
        } catch (final IllegalStateException e) {}
        try {
            VitamRequestIterator.isNewCursor(headers);
            fail("Should raized an exception");
        } catch (final IllegalStateException e) {}
        map.add(GlobalDataRest.X_CURSOR, "");
        try {
            VitamRequestIterator.isEndOfCursor(headers);
            fail("Should raized an exception");
        } catch (final IllegalStateException e) {}
        try {
            VitamRequestIterator.isNewCursor(headers);
            fail("Should raized an exception");
        } catch (final IllegalStateException e) {}
        map.clear();
        map.add(GlobalDataRest.X_CURSOR, "true");
        assertTrue(VitamRequestIterator.isNewCursor(headers));
        assertFalse(VitamRequestIterator.isEndOfCursor(headers));
        map.clear();
        map.add(GlobalDataRest.X_CURSOR, "true");
        map.add(GlobalDataRest.X_CURSOR_ID, "value");
        assertFalse(VitamRequestIterator.isNewCursor(headers));
        assertFalse(VitamRequestIterator.isEndOfCursor(headers));
        map.clear();
        map.add(GlobalDataRest.X_CURSOR, "false");
        try {
            VitamRequestIterator.isNewCursor(headers);
            fail("Should raized an exception");
        } catch (final IllegalStateException e) {}
        try {
            assertTrue(VitamRequestIterator.isEndOfCursor(headers));
        } catch (final IllegalStateException e) {}
        map.clear();
        map.add(GlobalDataRest.X_CURSOR, "false");
        map.add(GlobalDataRest.X_CURSOR_ID, "value");
        assertFalse(VitamRequestIterator.isNewCursor(headers));
        assertTrue(VitamRequestIterator.isEndOfCursor(headers));
    }

}
