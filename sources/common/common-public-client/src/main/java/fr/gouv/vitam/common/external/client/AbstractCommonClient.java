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
package fr.gouv.vitam.common.external.client;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.VitamAutoClosableResponse;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.client.VitamRequestBuilder;
import fr.gouv.vitam.common.client.configuration.ClientConfiguration;
import fr.gouv.vitam.common.exception.VitamApplicationServerDisconnectException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.retryable.DelegateRetry;
import fr.gouv.vitam.common.retryable.RetryableOnException;
import fr.gouv.vitam.common.retryable.RetryableParameters;
import fr.gouv.vitam.common.stream.StreamUtils;
import org.apache.http.NoHttpResponseException;
import org.apache.http.conn.ConnectTimeoutException;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.InputStream;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Predicate;

import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

abstract class AbstractCommonClient implements BasicClient {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AbstractCommonClient.class);

    private static final String BODY_AND_CONTENT_TYPE_CANNOT_BE_NULL = "Body and ContentType cannot be null";
    private static final String ARGUMENT_CANNOT_BE_NULL_EXCEPT_HEADERS = "Argument cannot be null except headers";

    private final RetryableParameters retryableParameters;
    final VitamClientFactory<?> clientFactory;
    private Client client;
    private Client clientNotChunked;

    private final Predicate<Exception> retryOnException = e -> {
        Throwable source = e.getCause();
        if (source == null) {
            return false;
        }
        return source instanceof ConnectTimeoutException
            || source instanceof UnknownHostException
            || source instanceof NoHttpResponseException
            || source instanceof SocketException;
    };

    protected AbstractCommonClient(VitamClientFactoryInterface<?> factory) {
        clientFactory = (VitamClientFactory<?>) factory;
        client = getClient(true);
        clientNotChunked = getClient(false);
        this.retryableParameters = new RetryableParameters(
            VitamConfiguration.getHttpClientRetry(),
            VitamConfiguration.getHttpClientFirstAttemptWaitingTime(),
            VitamConfiguration.getHttpClientWaitingTime(),
            VitamConfiguration.getHttpClientRandomWaitingSleep(),
            SECONDS
        );
        // External client or with no Session context are excluded
    }

    /**
     * This method consume everything (in particular InpuStream) and close the response.
     *
     * @param response
     */
    public static final void staticConsumeAnyEntityAndClose(Response response) {
        StreamUtils.consumeAnyEntityAndClose(response);
    }

    protected Client getClient(boolean chunked) throws IllegalStateException {
        Client clientToCreate;
        if (chunked) {
            clientToCreate = clientFactory.getHttpClient();
        } else {
            clientToCreate = clientFactory.getHttpClient(false);
        }
        return clientToCreate;
    }

    @Override
    public final void consumeAnyEntityAndClose(Response response) {
        staticConsumeAnyEntityAndClose(response);
    }

    @Override
    public void checkStatus() throws VitamApplicationServerException {
        this.checkStatus(null);
    }

    @Override
    public void checkStatus(MultivaluedHashMap<String, Object> headers)
        throws VitamApplicationServerException {
        Response response = null;
        try {
            response = make(VitamRequestBuilder.get().withPath(STATUS_URL).withHeaders(headers).withJsonAccept());
            final Response.Status status = Response.Status.fromStatusCode(response.getStatus());
            if (status == Status.OK || status == Status.NO_CONTENT) {
                return;
            }
            final String messageText = INTERNAL_SERVER_ERROR.getReasonPhrase() + " : " + status.getReasonPhrase();
            LOGGER.error(messageText);
            throw new VitamApplicationServerException(messageText);
        } catch (ProcessingException | VitamClientInternalException e) {
            final String messageText = INTERNAL_SERVER_ERROR.getReasonPhrase() + " : " + e.getMessage();
            LOGGER.error(messageText);
            throw new VitamApplicationServerDisconnectException(messageText, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    public Response make(VitamRequestBuilder vitamRequestBuilder) throws VitamClientInternalException {
        vitamRequestBuilder.runBeforeExecRequest();
        return new VitamAutoClosableResponse(
            performRequest(
                vitamRequestBuilder.getHttpMethod(),
                vitamRequestBuilder.getPath(),
                vitamRequestBuilder.getHeaders(),
                vitamRequestBuilder.getBody(),
                vitamRequestBuilder.getContentType(),
                vitamRequestBuilder.getAccept(),
                vitamRequestBuilder.isChunckedMode()
            )
        );
    }

    @Deprecated
    protected Response performRequest(String httpMethod, String path, MultivaluedMap<String, Object> headers,
        MediaType accept)
        throws VitamClientInternalException {
        return performRequest(httpMethod, path, headers, null, null, accept, false);
    }

    @Deprecated
    protected Response performRequest(String httpMethod, String path, MultivaluedMap<String, Object> headers,
        Object body, MediaType contentType, MediaType accept)
        throws VitamClientInternalException {
        return performRequest(httpMethod, path, headers, body, contentType, accept, getChunkedMode());
    }

    @Deprecated
    protected Response performRequest(String httpMethod, String path, MultivaluedMap<String, Object> headers,
        Object body, MediaType contentType, MediaType accept, boolean chunkedMode)
        throws VitamClientInternalException {
        try {
            final Builder builder = buildRequest(httpMethod, path, headers, accept, chunkedMode);
            return retryIfNecessary(httpMethod, body, contentType, builder);
        } catch (final ProcessingException e) {
            throw new VitamClientInternalException(e);
        }
    }

    private Response retryIfNecessary(String httpMethod, Object body, MediaType contentType, Builder builder) {
        if (body instanceof InputStream) {
            Entity<Object> entity = Entity.entity(body, contentType);
            return builder.method(httpMethod, entity);
        }

        DelegateRetry<Response, ProcessingException> delegate = () -> {
            if (body == null) {
                return builder.method(httpMethod);
            }
            Entity<Object> entity = Entity.entity(body, contentType);
            return builder.method(httpMethod, entity);
        };

        RetryableOnException<Response, ProcessingException> retryable = retryable();
        return retryable.exec(delegate);
    }

    protected VitamClientException createExceptionFromResponse(Response response) {
        VitamClientException exception = new VitamClientException(INTERNAL_SERVER_ERROR.getReasonPhrase());
        if (response != null && response.getStatusInfo() != null) {
            exception = new VitamClientException(response.getStatusInfo().getReasonPhrase());
        } else if (response != null && Status.fromStatusCode(response.getStatus()) != null) {
            exception = new VitamClientException(Status.fromStatusCode(response.getStatus()).getReasonPhrase());
        }
        return exception;
    }

    @Override
    public String getResourcePath() {
        return clientFactory.getResourcePath();
    }

    @Override
    public String getServiceUrl() {
        return clientFactory.getServiceUrl();
    }

    @Override
    public void close() {
        if (client != null) {
            clientFactory.resume(client, true);
        }
        if (clientNotChunked != null) {
            clientFactory.resume(clientNotChunked, false);
        }
    }

    private Builder buildRequest(String httpMethod, String path, MultivaluedMap<String, Object> headers,
        MediaType accept,
        boolean chunkedMode) {
        return buildRequest(httpMethod, getServiceUrl(), path, headers, accept, chunkedMode);
    }

    public Builder buildRequest(String httpMethod, String url, String path, MultivaluedMap<String, Object> headers,
        MediaType accept, boolean chunkedMode) {
        ParametersChecker.checkParameter(ARGUMENT_CANNOT_BE_NULL_EXCEPT_HEADERS, httpMethod, path, accept);
        final Builder builder = getHttpClient(chunkedMode).target(url).path(path).request().accept(accept);
        if (headers != null) {
            for (final Entry<String, List<Object>> entry : headers.entrySet()) {
                for (final Object value : entry.getValue()) {
                    builder.header(entry.getKey(), value);
                }
            }
        }
        if (this.clientFactory.isAllowGzipEncoded()) {
            builder.header(HttpHeaders.CONTENT_ENCODING, "gzip");
        }
        if (this.clientFactory.isAllowGzipDecoded()) {
            builder.header(HttpHeaders.ACCEPT_ENCODING, "gzip");
        }
        return builder;
    }

    Client getHttpClient() {
        return getHttpClient(getChunkedMode());
    }

    Client getHttpClient(boolean useChunkedMode) {
        if (useChunkedMode) {
            return client;
        } else {
            return clientNotChunked;
        }
    }

    final ClientConfiguration getClientConfiguration() {
        return clientFactory.getClientConfiguration();
    }

    boolean getChunkedMode() {
        return clientFactory.getChunkedMode();
    }

    private <T, E extends Exception> RetryableOnException<T, E> retryable() {
        return new RetryableOnException<>(retryableParameters, retryOnException);
    }

    @Override
    public String toString() {
        return "VitamClient: { " + clientFactory.toString() + " }";
    }
}
