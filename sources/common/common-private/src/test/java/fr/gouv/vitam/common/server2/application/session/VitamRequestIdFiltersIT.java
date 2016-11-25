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

package fr.gouv.vitam.common.server2.application.session;

import javax.ws.rs.core.MultivaluedHashMap;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.junit.VitamApplicationTestFactory.StartApplicationResponse;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server2.application.TestApplication;

/**
 * Tests for the requestId propagation between servers.
 */
public class VitamRequestIdFiltersIT {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(VitamRequestIdFiltersIT.class);

    private static int serverPort2;
    private static TestApplication application2;
    private static int serverPort1;
    private static TestApplication application1;
    private static LocalhostClientFactory server1ClientFactory;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // Start server 2
        final StartApplicationResponse<AbstractTestApplication> response2 =
            AbstractTestApplication.startTestApplication(new Server2TestApplication());
        serverPort2 = response2.getServerPort();
        application2 = response2.getApplication();
        // Configure and start server 1
        Server1TestApplication.setServer2Port(serverPort2);
        final StartApplicationResponse<AbstractTestApplication> response1 =
            AbstractTestApplication.startTestApplication(new Server1TestApplication());
        serverPort1 = response1.getServerPort();
        application1 = response1.getApplication();
        // Configure local client factory
        server1ClientFactory = new LocalhostClientFactory(serverPort1, "server1");
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Stop server 1
        if (application1 != null) {
            application1.stop();
        }
        JunitHelper.getInstance().releasePort(serverPort1);
        // Stop server 2
        if (application2 != null) {
            application2.stop();
        }
        JunitHelper.getInstance().releasePort(serverPort2);
        // Stop server 1
        server1ClientFactory.shutdown();
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
        final String propagatedId = server1ClientFactory.getClient().doRequest("callWithRequestId");
        Assert.assertEquals("id-from-server-1", propagatedId);
    }

    /**
     * Test purpose : same as previous one, but with no requestId set by the server1.
     */
    @Test
    public void testServer1ToServer2NoRequestIdSet() {
        // KWA TODO: explain a little what we do...
        final String propagatedId = server1ClientFactory.getClient().doRequest("callWithoutRequestId");
        Assert.assertEquals(Server2TestApplication.NO_REQUEST_ID_FOUND, propagatedId);
    }

    /**
     * Test purpose : same as previous ones insequence ; check especially if the thread context is correctly cleaned up
     * between requests.
     */
    @Test
    public void testRequestIdCleanupBetweenRequests() {
        // KWA TODO: explain a little what we do...
        LOGGER.info("First request");
        final String propagatedId = server1ClientFactory.getClient().doRequest("callWithRequestId");
        Assert.assertEquals("id-from-server-1", propagatedId);
        LOGGER.info("Second request");
        final String propagatedId2 = server1ClientFactory.getClient().doRequest("callWithoutRequestId");
        LOGGER.info("Assert");
        Assert.assertEquals(Server2TestApplication.NO_REQUEST_ID_FOUND, propagatedId2);
    }

    /**
     * Test purpose : same as first one, but the client in server1 is called in another thread (to check the passing of
     * the requestId between threads).
     */
    @Test
    public void testServer1ToServer2RequestIdPropagationWithThreadPool() {
        // KWA TODO: explain a little what we do...
        final String propagatedId = server1ClientFactory.getClient().doRequest("callWithThreadPoolRequestId");
        Assert.assertEquals("id-from-server-1-with-threadpool", propagatedId);
    }

    /**
     * Test purpose : check requestId propagation in responses
     */
    @Test
    public void testServer2SetRequestIdInResponsePropagation() {
        final String propagatedId = server1ClientFactory.getClient().doRequest("callToGetRequestIdInResponse");
        Assert.assertEquals("id-from-server-2", propagatedId);
    }

    @Test
    public void testTwoHeaders() {
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(GlobalDataRest.X_REQUEST_ID, "header-1");
        headers.add(GlobalDataRest.X_REQUEST_ID, "header-2-should-not-be-taken");
        final String propagatedId = server1ClientFactory.getClient().doRequest("directResponse", headers);
        Assert.assertEquals("header-1", propagatedId);
    }

}
