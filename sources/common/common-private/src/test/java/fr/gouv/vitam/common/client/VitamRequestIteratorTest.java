/*
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.server.application.junit.ResteasyTestApplication;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.serverv2.VitamServerTestRunner;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VitamRequestIteratorTest extends ResteasyTestApplication {
    private static final String RESOURCE_PATH = "/vitam-test/v1";

    private static boolean startup = true;
    private final static ExpectedResults mock = mock(ExpectedResults.class);

    private static DefaultClient client;

    static TestVitamClientFactory factory = new TestVitamClientFactory<DefaultClient>(1, RESOURCE_PATH);

    public static VitamServerTestRunner
        vitamServerTestRunner = new VitamServerTestRunner(VitamRequestIteratorTest.class, factory);


    @BeforeClass
    public static void setUpBeforeClass() throws Throwable {
        vitamServerTestRunner.start();
        client = (DefaultClient) vitamServerTestRunner.getClient();
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
    @ApplicationPath("webresources")
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
            startup = false;
            return response;
        }
    }

    @Test
    public void testIterator() {
        startup = true;
        try (VitamRequestIterator<ObjectNode> iterator =
            new VitamRequestIterator<>(client, VitamRequestBuilder.get().withPath("/iterator"), ObjectNode.class)) {
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
            new VitamRequestIterator<>(client, VitamRequestBuilder.get().withPath("/iterator"), ObjectNode.class)) {
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
            new VitamRequestIterator<>(client, VitamRequestBuilder.get().withPath("/iterator"), ObjectNode.class)) {
            final RequestResponseOK response = new RequestResponseOK();
            final ResponseBuilder builder = Response.status(Status.BAD_REQUEST);
            when(mock.get())
                .thenReturn(VitamRequestIterator.setHeaders(builder, true, null).entity(response).build());
            try {
                assertFalse(iterator.hasNext());
                fail("should raized an exception");
            } catch (final VitamRuntimeException e) {

            }
        }
    }

    @Test
    public void testIteratorShortList() {
        startup = true;
        try (VitamRequestIterator<ObjectNode> iterator =
            new VitamRequestIterator<>(client, VitamRequestBuilder.get().withPath("/iterator"), ObjectNode.class)) {
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
            new VitamRequestIterator<>(client, VitamRequestBuilder.get().withPath("/iterator"), ObjectNode.class)) {
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
            new VitamRequestIterator<>(client, VitamRequestBuilder.get().withPath("/iterator"), ObjectNode.class)) {
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
            new VitamRequestIterator<>(client, VitamRequestBuilder.get().withPath("/iterator"), ObjectNode.class)) {
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
}
