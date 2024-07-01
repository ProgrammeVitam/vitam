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

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client.DefaultClient;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.stream.StreamUtils;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.CompletionCallback;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import java.io.InputStream;

/**
 * Async Response for InputStream Helper</br>
 * </br>
 * Usage:</br>
 * </br>
 * <b>Direct download, where no Http Client is called but a direct InputStream generated</b></br>
 *
 * <pre>
 * <code>
 * &#64;Path(DOWNLOAD + HttpMethod.GET)
 * &#64;GET
 * &#64;Produces(MediaType.APPLICATION_OCTET_STREAM)
 * &#64;Consumes(MediaType.WILDCARD)
 * public void downloadDirectGet(@Suspended final AsyncResponse asyncResponse) {
 * VitamThreadPoolExecutor.getInstance().execute(new Runnable() {
 *
 * &#64;Override
 * public void run() {
 * File file = new File(...)
 * FileInputStream inputStream = new FileInputStream(file);
 * new AsyncInputStreamHelper(asyncResponse, inputStream)
 * .writeResponse(Response.ok());
 * }
 * });
 * }
 * </code>
 * </pre>
 *
 * </br>
 * <b>Indirect download, where one Http Client is called to get an InputStream to forward</b></br>
 *
 * <pre>
 * <code>
 * &#64;Path(DOWNLOAD_INDIRECT + HttpMethod.GET)
 * &#64;GET
 * &#64;Produces(MediaType.APPLICATION_OCTET_STREAM)
 * &#64;Consumes(MediaType.WILDCARD)
 * public void downloadIndirectGet(@Suspended final AsyncResponse asyncResponse) throws VitamClientInternalException {
 * VitamThreadPoolExecutor.getInstance().execute(new Runnable() {
 *
 * &#64;Override
 * public void run() {
 * String method = HttpMethod.GET;
 * Response response = null;
 * try (final BenchmarkClientRest client =
 * BenchmarkClientFactory.getInstance().getClient()) {
 * response = client.performRequest(method, BenchmarkResourceProduceInputStream.DOWNLOAD + method,
 * null, MediaType.APPLICATION_OCTET_STREAM_TYPE);
 * buildReponse(asyncResponse, response); // Using AsyncInputStreamHelper
 * } catch (VitamClientInternalException e) {
 * AsyncInputStreamHelper.asyncResponseResume(asyncResponse,
 * Response.status(Status.INTERNAL_SERVER_ERROR).build());
 * }
 * }
 * });
 * }
 * </code>
 * </pre>
 */
public class AsyncInputStreamHelper {

    private final AsyncResponse asyncResponse;
    private final Response receivedResponse;
    private InputStream inputStream;

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AsyncInputStreamHelper.class);

    /**
     * Constructor using received response containing an InputStream to forward
     *
     * @param asyncResponse the AsyncReponse from the Resource API
     * @param inputStream the native InputStream to send (not from a Client response)
     */
    public AsyncInputStreamHelper(AsyncResponse asyncResponse, InputStream inputStream) {
        ParametersChecker.checkParameter("Parameters should not be null", asyncResponse, inputStream);
        this.asyncResponse = asyncResponse;
        receivedResponse = null;
        this.inputStream = inputStream;
    }

    /**
     * Constructor using native InputStream and size to forward
     *
     * @param asyncResponse the AsyncReponse from the Resource API
     * @param receivedResponse Received Response containing the InputStream to forward as is
     */
    public AsyncInputStreamHelper(AsyncResponse asyncResponse, Response receivedResponse) {
        ParametersChecker.checkParameter("Parameters should not be null", asyncResponse, receivedResponse);
        this.asyncResponse = asyncResponse;
        this.receivedResponse = receivedResponse;
    }

    /**
     * Once constructed, call this to finalize your operation in case of Error message.</br>
     * </br>
     * Note that receivedResponse if any is fully read and closed for you there.
     *
     * @param errorResponse the fully prepared ErrorResponse
     */
    public void writeErrorResponse(Response errorResponse) {
        if (inputStream != null) {
            StreamUtils.closeSilently(inputStream);
            inputStream = null;
        }
        try {
            asyncResponseResume(asyncResponse, errorResponse);
        } finally {
            DefaultClient.staticConsumeAnyEntityAndClose(receivedResponse);
        }
    }

    /**
     * Once constructed, call this to finalize your operation.</br>
     * </br>
     * Note that receivedResponse if any is closed for you there.
     *
     * @param responseBuilder the ResponseBuilder initialize with your own parameters and status
     */
    public void writeResponse(ResponseBuilder responseBuilder) {
        try {
            ParametersChecker.checkParameter("ResponseBuilder should not be null", responseBuilder);
            if (receivedResponse != null) {
                try {
                    inputStream = receivedResponse.readEntity(InputStream.class);
                } catch (final IllegalStateException e) {
                    LOGGER.error(e);
                    asyncResponseResume(asyncResponse, getResponseError(VitamCode.GLOBAL_INTERNAL_SERVER_ERROR));
                    return;
                }
            }
            final VitamStreamingOutput vitamStreamingOutput = new VitamStreamingOutput(inputStream);
            this.asyncResponse.register((CompletionCallback) throwable -> vitamStreamingOutput.close());

            asyncResponse.resume(responseBuilder.entity(vitamStreamingOutput).build());
        } finally {
            DefaultClient.staticConsumeAnyEntityAndClose(receivedResponse);
        }
    }

    /**
     * Call this to finalize your operation in case of Error message while no remote client operation is done.</br>
     * </br>
     * Note you must not call this method if you have already a received Response but the
     * {@link #writeErrorResponse(Response)}.
     *
     * @param asyncResponse
     * @param response the fully prepared ErrorResponse
     */
    public static void asyncResponseResume(AsyncResponse asyncResponse, final Response response) {
        ParametersChecker.checkParameter("ErrorResponse should not be null", response);
        asyncResponse.register(
            (CompletionCallback) throwable -> {
                Object entity = response.getEntity();
                if (entity != null && entity instanceof InputStream) {
                    StreamUtils.closeSilently((InputStream) entity);
                }
            }
        );
        asyncResponse.resume(response);
    }

    /**
     * Call this to finalize your operation in case of Error message while no remote client operation is done.</br>
     * </br>
     * Note you must not call this method if you have already a received Response but the
     * {@link #writeErrorResponse(Response)}.
     *
     * @param asyncResponse
     * @param response the fully prepared ErrorResponse
     * @param stream an inputStream to close anyway
     */
    public static void asyncResponseResume(
        AsyncResponse asyncResponse,
        final Response response,
        final InputStream stream
    ) {
        StreamUtils.closeSilently(stream);
        ParametersChecker.checkParameter("ErrorResponse should not be null", response);
        asyncResponse.resume(response);
    }

    private Response getResponseError(VitamCode vitamCode) {
        return Response.status(vitamCode.getStatus())
            .entity(
                new VitamError(VitamCodeHelper.getCode(vitamCode))
                    .setContext(vitamCode.getService().getName())
                    .setState(vitamCode.getDomain().getName())
                    .setMessage(vitamCode.getMessage())
                    .setDescription(vitamCode.getMessage())
                    .toString()
            )
            .build();
    }
}
