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
package fr.gouv.vitam.common.server.application.session;

import com.google.common.collect.Sets;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.exception.VitamThreadAccessException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.HeaderIdContainerFilter;
import fr.gouv.vitam.common.server.application.junit.ResteasyTestApplication;
import fr.gouv.vitam.common.serverv2.VitamServerTestRunner;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.MDC;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Tests for the requestId propagation between servers.
 */
public class VitamRequestIdFiltersIT extends ResteasyTestApplication {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(VitamRequestIdFiltersIT.class);

    public static VitamServerTestRunner server1 = new VitamServerTestRunner(
        VitamRequestIdFiltersIT.class,
        new LocalhostClientFactory(1, "/server1")
    );

    public static VitamServerTestRunner server2 = new VitamServerTestRunner(
        VitamRequestIdFiltersIT.class,
        new LocalhostClientFactory(2, "/server2")
    );

    @BeforeClass
    public static void setUpBeforeClass() throws Throwable {
        server1.start();
        server2.start();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Throwable {
        server1.runAfter();
        server2.runAfter();
    }

    @Override
    public Set<Object> getResources() {
        return Sets.newHashSet(new HeaderIdContainerFilter(), new Server1Resource(), new Server2Resource());
    }

    /**
     * Test purpose : validate simple RequestId propagation between server 1 and 2 ; sequence :
     * <p>
     * <ol>
     * <li>junit test sends a request with a client (no request id in header) ;</li>
     * <li>server1 gets the request, add a requestId and calls server2 ;</li>
     * <li>server2 gets the request, extract the requestId from the thread context, validate the consistency with the
     * MDC and return the extracted requestId in the response body ;</li>
     * <li>server1 transfers back the response (unchanged) to the client ;</li>
     * <li>the client validate the body of the response to be the awaited requestId seen by the server2.
     * </ol>
     * <p>
     */
    @Test
    public void testServer1ToServer2RequestIdPropagation() {
        try (
            LocalhostClientFactory.LocalhostClient client = (LocalhostClientFactory.LocalhostClient) server1.getClient()
        ) {
            final String propagatedId = client.doRequest("callWithRequestId");
            Assert.assertEquals("id-from-server-1", propagatedId);
        }
    }

    /**
     * Test purpose : same as previous one, but with no requestId set by the server1.
     */
    @Test
    public void testServer1ToServer2NoRequestIdSet() {
        // KWA TODO: explain a little what we do...
        try (
            LocalhostClientFactory.LocalhostClient client = (LocalhostClientFactory.LocalhostClient) server1.getClient()
        ) {
            final String propagatedId = client.doRequest("callWithoutRequestId");
            Assert.assertEquals("<REQUEST_ID_EMPTY>", propagatedId);
        }
    }

    /**
     * Test purpose : same as previous ones insequence ; check especially if the thread context is correctly cleaned up
     * between requests.
     */
    @Test
    public void testRequestIdCleanupBetweenRequests() {
        // KWA TODO: explain a little what we do...
        LOGGER.info("First request");
        try (
            LocalhostClientFactory.LocalhostClient client = (LocalhostClientFactory.LocalhostClient) server1.getClient()
        ) {
            final String propagatedId = client.doRequest("callWithRequestId");
            Assert.assertEquals("id-from-server-1", propagatedId);
        }
        LOGGER.info("Second request");
        try (
            LocalhostClientFactory.LocalhostClient client = (LocalhostClientFactory.LocalhostClient) server1.getClient()
        ) {
            final String propagatedId2 = client.doRequest("callWithoutRequestId");
            LOGGER.info("Assert");
            Assert.assertEquals("<REQUEST_ID_EMPTY>", propagatedId2);
        }
    }

    /**
     * Test purpose : same as first one, but the client in server1 is called in another thread (to check the passing of
     * the requestId between threads).
     */
    @Test
    public void testServer1ToServer2RequestIdPropagationWithThreadPool() {
        // KWA TODO: explain a little what we do...
        try (
            LocalhostClientFactory.LocalhostClient client = (LocalhostClientFactory.LocalhostClient) server1.getClient()
        ) {
            final String propagatedId = client.doRequest("callWithThreadPoolRequestId");
            Assert.assertEquals("id-from-server-1-with-threadpool", propagatedId);
        }
    }

    /**
     * Test purpose : check no requestId propagation in responses
     */
    @Test
    public void testServer2DoNotPropagateHiSessionToCallerServer() {
        try (
            LocalhostClientFactory.LocalhostClient client = (LocalhostClientFactory.LocalhostClient) server1.getClient()
        ) {
            final String propagatedId = client.doRequest("callToGetRequestIdInResponse");
            Assert.assertEquals("id-from-server-1", propagatedId);
        }
    }

    @Test
    public void testTwoHeaders() {
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(GlobalDataRest.X_REQUEST_ID, "header-1");
        headers.add(GlobalDataRest.X_REQUEST_ID, "header-2-should-not-be-taken");
        try (
            LocalhostClientFactory.LocalhostClient client = (LocalhostClientFactory.LocalhostClient) server1.getClient()
        ) {
            final String propagatedId = client.doRequest("directResponse", headers);
            Assert.assertEquals("header-1", propagatedId);
        }
    }

    /**
     * Implementation of test Server1 ; note : returned values from REST interfaces should serve to the validation of
     * the results in {@link VitamRequestIdFiltersIT}
     */
    @Path("/server1")
    public static class Server1Resource {

        @GET
        @Path("/callWithRequestId")
        public String setRequestIdAndCallServer2() throws VitamThreadAccessException {
            VitamThreadUtils.getVitamSession().setRequestId("id-from-server-1");
            LOGGER.debug("RequestId set.");
            try (
                LocalhostClientFactory.LocalhostClient client =
                    (LocalhostClientFactory.LocalhostClient) server2.getClient()
            ) {
                return client.doRequest("failIfNoRequestId");
            }
        }

        @GET
        @Path("/callWithoutRequestId")
        public String justCallServer2() {
            LOGGER.debug("RequestId not set.");
            try (
                LocalhostClientFactory.LocalhostClient client =
                    (LocalhostClientFactory.LocalhostClient) server2.getClient()
            ) {
                return client.doRequest("failIfNoRequestId");
            }
        }

        @GET
        @Path("/callWithThreadPoolRequestId")
        public String setRequestIdAndCallServer2WithThreadPool()
            throws ExecutionException, InterruptedException, VitamThreadAccessException {
            VitamThreadUtils.getVitamSession().setRequestId("id-from-server-1-with-threadpool");
            LOGGER.debug("RequestId set. Forwarding execution to ThreadPool.");
            final Future<String> future = VitamThreadPoolExecutor.getDefaultExecutor()
                .submit(() -> {
                    try (
                        LocalhostClientFactory.LocalhostClient client =
                            (LocalhostClientFactory.LocalhostClient) server2.getClient()
                    ) {
                        return client.doRequest("failIfNoRequestId");
                    }
                });
            return future.get();
        }

        @GET
        @Path("/callToGetRequestIdInResponse")
        public String justCallServer2ForRequestIdInResponse() throws VitamThreadAccessException {
            LOGGER.debug("RequestId not set.");
            VitamThreadUtils.getVitamSession().setRequestId("id-from-server-1");
            try (
                LocalhostClientFactory.LocalhostClient client =
                    (LocalhostClientFactory.LocalhostClient) server2.getClient()
            ) {
                client.doRequest("setRequestIdInResponse");
            }

            final String reqId = VitamThreadUtils.getVitamSession().getRequestId();
            Assert.assertEquals(MDC.get(GlobalDataRest.X_REQUEST_ID), reqId); // Check that the logging framework is
            // working
            return reqId;
        }

        @GET
        @Path("/directResponse")
        public String directResponse() throws VitamThreadAccessException {
            return VitamThreadUtils.getVitamSession().getRequestId();
        }

        @GET
        @Path("/testWaitFiveSecond")
        @Consumes(MediaType.APPLICATION_JSON)
        public Response wait5second() {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {}
            return Response.status(Response.Status.OK).build();
        }

        @GET
        @Path("/testReturnImmediately")
        @Consumes(MediaType.APPLICATION_JSON)
        public Response wait0second() {
            return Response.status(Response.Status.OK).build();
        }
    }

    @Path("/server2")
    public static class Server2Resource {

        public static final String NO_REQUEST_ID_FOUND = "<REQUEST_ID_EMPTY>";

        @GET
        @Path("/failIfNoRequestId")
        public String failIfNoRequestId() throws VitamThreadAccessException {
            final String reqId = VitamThreadUtils.getVitamSession().getRequestId();
            Assert.assertEquals(MDC.get(GlobalDataRest.X_REQUEST_ID), reqId); // Check that the logging framework is
            // working
            return reqId == null ? NO_REQUEST_ID_FOUND : reqId;
        }

        @GET
        @Path("/setRequestIdInResponse")
        public String setRequestIdInResponse() throws VitamThreadAccessException {
            VitamThreadUtils.getVitamSession().setRequestId("id-from-server-2");
            return "";
        }
    }
}
