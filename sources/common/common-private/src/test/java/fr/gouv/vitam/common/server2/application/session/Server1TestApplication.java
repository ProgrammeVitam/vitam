/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 *******************************************************************************/

package fr.gouv.vitam.common.server2.application.session;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.junit.Assert;
import org.slf4j.MDC;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.exception.VitamThreadAccessException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;

/**
 * Server1 implementation of {@link VitamRequestIdFiltersIT} tests.
 */
public class Server1TestApplication extends AbstractTestApplication {

    @Override
    protected final Class getResourceClass() {
        return Server1Resource.class;
    }

    public Server1TestApplication() {
        super("session/server1.conf");
    }

    private static LocalhostClientFactory clientFactory;

    public static void setServer2Port(int server2Port) {
        clientFactory = new LocalhostClientFactory(server2Port, "/server2");
    }

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(Server1TestApplication.class);


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
            return clientFactory.getClient().doRequest("failIfNoRequestId");
        }

        @GET
        @Path("/callWithoutRequestId")
        public String justCallServer2() {
            LOGGER.debug("RequestId not set.");
            return clientFactory.getClient().doRequest("failIfNoRequestId");
        }

        @GET
        @Path("/callWithThreadPoolRequestId")
        public String setRequestIdAndCallServer2WithThreadPool()
            throws ExecutionException, InterruptedException, VitamThreadAccessException {
            VitamThreadUtils.getVitamSession().setRequestId("id-from-server-1-with-threadpool");
            LOGGER.debug("RequestId set. Forwarding execution to ThreadPool.");
            final Future<String> future = VitamThreadPoolExecutor.getDefaultExecutor()
                .submit(() -> clientFactory.getClient().doRequest("failIfNoRequestId"));
            return future.get();
        }


        @GET
        @Path("/callToGetRequestIdInResponse")
        public String justCallServer2ForRequestIdInResponse() throws VitamThreadAccessException {
            LOGGER.debug("RequestId not set.");
            clientFactory.getClient().doRequest("setRequestIdInResponse");
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
    }
}
