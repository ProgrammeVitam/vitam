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
package fr.gouv.vitam.common.server2.application;

import java.util.concurrent.ExecutorService;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.CompletionCallback;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import fr.gouv.vitam.common.client2.DefaultClient;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;

/**
 * Class to handle FutureResponseHelper from a client in order to close it when done. </br>
 * <b>Deprecated</b>: One should use {@link AsyncInputStreamHelper} and potentially {@link ResponseHelper} instead.</br>
 * </br>
 * Example:</br>
 *
 * <pre>
 * <code>
 * GET
 * public void getInputStream(@Suspended AsyncResponse asyncResponse) {
     final FutureResponseHelper futureResponseHelper = new FutureResponseHelper(asyncResponse);
     futureResponseHelper.startAsyncRunnable(new AsyncRunnable() {

       Override
       public Response run() {
          ... anything useful
          Response response =
             performRequest(..., MediaType.APPLICATION_OCTET_STREAM_TYPE);
          setInnerClientResponseToCloseOnSent(response);

          switch (Response.Status.fromStatusCode(response.getStatus())) {
             case OK:
               InputStream stream = response.readEntity(InputStream.class);
               // Return the Response
               return Response.status(Status.OK).entity(stream).build();
             case NOT_FOUND:
               // Return an error
               LOGGER.error(log);
               return Response.status(Status.NOT_FOUND).entity(other).build();
             default:
               // Return another error
               LOGGER.error(log);
               return Response.status(Status.SERVICE_UNAVAILABLE).entity(other).build();
          }
       }
   });
 }
 * </code>
 * </pre>
 */
@Deprecated
public class FutureResponseHelper {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(FutureResponseHelper.class);
    private static final ExecutorService EXECUTOR_SERVICE = new VitamThreadPoolExecutor();

    /**
     * Runnable Abstract to extend to create the response model
     */
    public abstract static class AsyncRunnable {
        FutureResponseHelper responseHelper;

        private final AsyncRunnable setResponseHelper(FutureResponseHelper responseHelper) {
            this.responseHelper = responseHelper;
            return this;
        }

        /**
         * Setter for the inner Response to close once the build one is returned
         *
         * @param response the response received from another service, to close once done
         */
        public final void setInnerClientResponseToCloseOnSent(Response response) {
            responseHelper.setReceivedResponse(response);
        }

        /**
         * Method to implement
         *
         * @return the final Response to send to the caller
         */
        public abstract Response run();
    }

    final AsyncResponse asyncResponse;
    Response reicevedResponse;

    /**
     * Constructor from asyncResponse
     *
     * @param asyncResponse
     */
    public FutureResponseHelper(AsyncResponse asyncResponse) {
        this.asyncResponse = asyncResponse;
        prepareAsync();
    }

    /**
     * Register the completion callback to clean and close the original response
     */
    private void prepareAsync() {
        asyncResponse.register(new VitamCompletionCallback(reicevedResponse));
    }

    private static class VitamCompletionCallback implements CompletionCallback {
        private final Response reicevedResponse;

        private VitamCompletionCallback(Response reicevedResponse) {
            this.reicevedResponse = reicevedResponse;
        }

        @Override
        public void onComplete(Throwable throwable) {
            if (throwable != null) {
                LOGGER.error(throwable);
            }
            // no throwable - the processing ended successfully
            // (response already written to the client)
            DefaultClient.staticConsumeAnyEntityAndClose(reicevedResponse);
        }

    }

    /**
     *
     * @param asyncRunnable is the Runnable method that handle the asyncResponse
     */
    public void startAsyncRunnable(AsyncRunnable asyncRunnable) {
        EXECUTOR_SERVICE.submit(new InternalAsyncRunnable(this, asyncRunnable.setResponseHelper(this)));
    }

    /**
     * Set the original received Response
     *
     * @param reicevedResponse
     * @return this
     */
    public FutureResponseHelper setReceivedResponse(Response reicevedResponse) {
        this.reicevedResponse = reicevedResponse;
        return this;
    }

    /**
     * Real Runnable class
     */
    private static class InternalAsyncRunnable implements Runnable {
        final AsyncRunnable asyncRunnable;
        final FutureResponseHelper responseHelper;

        private InternalAsyncRunnable(FutureResponseHelper responseHelper, AsyncRunnable asyncRunnable) {
            this.responseHelper = responseHelper;
            this.asyncRunnable = asyncRunnable;
        }

        @Override
        public void run() {
            try {
                responseHelper.asyncResponse.resume(asyncRunnable.run());
            } catch (final Exception e) {
                LOGGER.error("Catched exception from AsyncRunnable", e);
                responseHelper.asyncResponse.resume(
                    Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity(e.getMessage()).build());
            }
        }

    }

}
