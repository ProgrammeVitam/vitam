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

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.configuration.ClientConfiguration;
import fr.gouv.vitam.common.exception.VitamApplicationServerDisconnectException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.security.filter.AuthorizationFilterHelper;
import fr.gouv.vitam.common.stream.StreamUtils;
import org.apache.http.NoHttpResponseException;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.AsyncInvoker;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.InputStream;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.Future;

/**
 * Abstract Partial client class for all vitam clients
 */
abstract class AbstractCommonClient implements BasicClient {
    protected static final String INTERNAL_SERVER_ERROR = "Internal Server Error";
    private static final String TIMEOUT_OCCURS_OR_DNS_PROBE_ERROR_RETRY = "TimeoutOccurs or DNS probe error, retry: ";
    private static final String UNKNOWN_ERROR_IN_CLIENT = "Unknown error in client";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AbstractCommonClient.class);
    private static final String BODY_AND_CONTENT_TYPE_CANNOT_BE_NULL = "Body and ContentType cannot be null";
    private static final String ARGUMENT_CANNOT_BE_NULL_EXCEPT_HEADERS = "Argument cannot be null except headers";
    /**
     * Client Factory
     */
    final VitamClientFactory<?> clientFactory;
    /**
     * Used to get random sleep only
     */
    private final Random random = new Random(System.currentTimeMillis());
    private Client client;
    private Client clientNotChunked;

    /**
     * Constructor with standard configuration
     *
     * @param factory The client factory
     */
    protected AbstractCommonClient(VitamClientFactoryInterface<?> factory) {
        clientFactory = (VitamClientFactory<?>) factory;
        client = getClient(true);
        clientNotChunked = getClient(false);
        // External client or with no Session context are excluded
    }

    /**
     * This method consume everything (in particular InpuStream) and close the response.
     *
     * @param response
     */
    public static final void staticConsumeAnyEntityAndClose(Response response) {
        try {
            if (response != null && response.hasEntity()) {
                final Object object = response.getEntity();
                if (object instanceof InputStream) {
                    StreamUtils.closeSilently((InputStream) object);
                }
            }
        } catch (final Exception e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (final Exception e) {
                    SysErrLogger.FAKE_LOGGER.ignoreLog(e);
                }
            }
        }
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
            response = performRequest(HttpMethod.GET, STATUS_URL, headers, MediaType.APPLICATION_JSON_TYPE);
            final Response.Status status = Response.Status.fromStatusCode(response.getStatus());
            if (status == Status.OK || status == Status.NO_CONTENT) {
                return;
            }
            final String messageText = INTERNAL_SERVER_ERROR + " : " + status.getReasonPhrase();
            LOGGER.error(messageText);
            throw new VitamApplicationServerException(messageText);
        } catch (ProcessingException | VitamClientInternalException e) {
            final String messageText = INTERNAL_SERVER_ERROR + " : " + e.getMessage();
            LOGGER.error(messageText);
            throw new VitamApplicationServerDisconnectException(messageText, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    /**
     * Helper for retry request when unreachable or Connect timeout
     *
     * @param retry retry count
     * @param e the original ProcessingException
     * @return the original exception allowing to continue and store the last one
     * @throws ProcessingException
     */
    private final ProcessingException checkSpecificExceptionForRetry(int retry, ProcessingException e)
        throws ProcessingException {
        Throwable source = e.getCause();
        if (source == null) {
            LOGGER.error(TIMEOUT_OCCURS_OR_DNS_PROBE_ERROR_RETRY + retry, e);
            throw e;
        }
        if (source instanceof ConnectTimeoutException || source instanceof UnknownHostException ||
            source instanceof HttpHostConnectException || source instanceof NoHttpResponseException ||
            source instanceof NoRouteToHostException || source instanceof SocketException) {
            LOGGER.info(TIMEOUT_OCCURS_OR_DNS_PROBE_ERROR_RETRY + retry, source);
            try {
                long sleep = random.nextInt(50) + 20;
                Thread.sleep(sleep);
            } catch (InterruptedException e1) {
                LOGGER.error(TIMEOUT_OCCURS_OR_DNS_PROBE_ERROR_RETRY + retry, source);
                throw new ProcessingException("Interruption received", e1);
            }
            return e;
        } else {
            LOGGER.error(TIMEOUT_OCCURS_OR_DNS_PROBE_ERROR_RETRY + retry, source);
            throw e;
        }
    }

    /**
     * Helper for retry request when unreachable or Connect timeout for Stream
     *
     * @param retry retry count
     * @param e the original ProcessingException
     * @return the original exception allowing to continue and store the last one
     * @throws ProcessingException
     */
    private final ProcessingException checkSpecificExceptionForRetryUsingStream(int retry, ProcessingException e)
        throws ProcessingException {
        // Not a good idea to retry here on InputStream
        throw e;
    }

    /**
     * @param httpMethod
     * @param body may be null
     * @param contentType may be null
     * @param builder
     * @return the final response
     * @throws VitamClientInternalException if retry is not possible and http call is failed
     */
    private final Response retryIfNecessary(String httpMethod, Object body, MediaType contentType, Builder builder)
        throws VitamClientInternalException {
        ProcessingException lastException = null;
        if (body instanceof InputStream) {
            for (int i = 0; i < VitamConfiguration.getRetryNumber(); i++) {
                try {
                    Entity<Object> entity = Entity.entity(body, contentType);
                    return builder.method(httpMethod, entity);
                } catch (ProcessingException e) {
                    lastException = checkSpecificExceptionForRetryUsingStream(i, e);
                    continue;
                }
            }
        } else {
            for (int i = 0; i < VitamConfiguration.getRetryNumber(); i++) {
                try {
                    if (body == null) {
                        return builder.method(httpMethod);
                    } else {
                        Entity<Object> entity = Entity.entity(body, contentType);
                        return builder.method(httpMethod, entity);
                    }
                } catch (ProcessingException e) {
                    lastException = checkSpecificExceptionForRetry(i, e);
                    continue;
                }
            }
        }
        if (lastException != null) {
            LOGGER.error(lastException);
            throw lastException;
        } else {
            throw new VitamClientInternalException(UNKNOWN_ERROR_IN_CLIENT);
        }
    }

    /**
     * Perform a HTTP request to the server for synchronous call
     *
     * @param httpMethod HTTP method to use for request
     * @param path URL to request
     * @param headers headers HTTP to add to request, may be null
     * @param accept asked type of response
     * @return the response from the server
     * @throws VitamClientInternalException
     */
    protected Response performRequest(String httpMethod, String path, MultivaluedMap<String, Object> headers,
        MediaType accept)
        throws VitamClientInternalException {
        return performRequest(httpMethod, path, headers, null, accept);
    }

    /**
     * Perform a HTTP request to the server for synchronous call
     *
     * @param httpMethod HTTP method to use for request
     * @param path URL to request
     * @param headers headers HTTP to add to request, may be null
     * @param queryParams query parameters to add to get request, my be null
     * @param accept asked type of response
     * @return the response from the server
     * @throws VitamClientInternalException
     */
    protected Response performRequest(String httpMethod, String path, MultivaluedMap<String, Object> headers,
        MultivaluedMap<String, Object> queryParams,
        MediaType accept)
        throws VitamClientInternalException {
        try {
            final Builder builder = buildRequest(httpMethod, path, headers, queryParams, accept, false);
            return retryIfNecessary(httpMethod, null, null, builder);
        } catch (final ProcessingException e) {
            throw new VitamClientInternalException(e);
        }
    }

    /**
     * Perform a HTTP request to the server for synchronous call
     *
     * @param httpMethod HTTP method to use for request
     * @param path URL to request
     * @param headers headers HTTP to add to request, may be null
     * @param body body content of type contentType, may be null
     * @param contentType the media type of the body to send, null if body is null
     * @param accept asked type of response
     * @return the response from the server
     * @throws VitamClientInternalException
     */
    protected Response performRequest(String httpMethod, String path, MultivaluedMap<String, Object> headers,
        Object body,
        MediaType contentType, MediaType accept)
        throws VitamClientInternalException {
        if (body == null) {
            return performRequest(httpMethod, path, headers, accept);
        }
        try {
            ParametersChecker.checkParameter(BODY_AND_CONTENT_TYPE_CANNOT_BE_NULL, body, contentType);
            final Builder builder = buildRequest(httpMethod, path, headers, accept, getChunkedMode());
            return retryIfNecessary(httpMethod, body, contentType, builder);
        } catch (final ProcessingException e) {
            throw new VitamClientInternalException(e);
        }
    }

    /**
     * Perform a HTTP request to the server for synchronous call
     *
     * @param httpMethod HTTP method to use for request
     * @param path URL to request
     * @param headers headers HTTP to add to request, may be null
     * @param body body content of type contentType, may be null
     * @param contentType the media type of the body to send, null if body is null
     * @param accept asked type of response
     * @param chunkedMode True use default client, else False use non Chunked mode client
     * @return the response from the server
     * @throws VitamClientInternalException
     */
    protected Response performRequest(String httpMethod, String path, MultivaluedHashMap<String, Object> headers,
        Object body,
        MediaType contentType, MediaType accept, boolean chunkedMode)
        throws VitamClientInternalException {
        if (body == null) {
            return performRequest(httpMethod, path, headers, accept);
        }
        try {
            ParametersChecker.checkParameter(BODY_AND_CONTENT_TYPE_CANNOT_BE_NULL, body, contentType);
            final Builder builder = buildRequest(httpMethod, path, headers, accept, chunkedMode);
            return retryIfNecessary(httpMethod, body, contentType, builder);
        } catch (final ProcessingException e) {
            throw new VitamClientInternalException(e);
        }
    }

    /**
     * @param httpMethod
     * @param body may be null
     * @param contentType may be null
     * @param builder
     * @param callback
     * @param <T> the type of the Future result (generally Response)
     * @return the response from the server as Future
     * @throws VitamClientInternalException if retry is not possible and http call is failed
     */
    private final <T> Future<T> retryIfNecessary(String httpMethod, Object body, MediaType contentType,
        AsyncInvoker builder, InvocationCallback<T> callback)
        throws VitamClientInternalException {
        ProcessingException lastException = null;
        if (body instanceof InputStream) {
            for (int i = 0; i < VitamConfiguration.getRetryNumber(); i++) {
                try {
                    Entity<Object> entity = Entity.entity(body, contentType);
                    return builder.method(httpMethod, entity, callback);
                } catch (ProcessingException e) {
                    lastException = checkSpecificExceptionForRetryUsingStream(i, e);
                    continue;
                }
            }
        } else {
            for (int i = 0; i < VitamConfiguration.getRetryNumber(); i++) {
                try {
                    if (body == null) {
                        return builder.method(httpMethod, callback);
                    } else {
                        Entity<Object> entity = Entity.entity(body, contentType);
                        return builder.method(httpMethod, entity, callback);
                    }
                } catch (ProcessingException e) {
                    lastException = checkSpecificExceptionForRetry(i, e);
                    continue;
                }
            }
        }
        if (lastException != null) {
            LOGGER.error(lastException);
            throw lastException;
        } else {
            throw new VitamClientInternalException(UNKNOWN_ERROR_IN_CLIENT);
        }
    }

    /**
     * Perform an Async HTTP request to the server with callback
     *
     * @param httpMethod HTTP method to use for request
     * @param path URL to request
     * @param headers headers HTTP to add to request, may be null
     * @param body body content of type contentType, may be null
     * @param contentType the media type of the body to send, null if body is null
     * @param accept asked type of response
     * @param callback
     * @param <T> the type of the Future result (generally Response)
     * @return the response from the server as Future
     * @throws VitamClientInternalException
     */
    protected <T> Future<T> performAsyncRequest(String httpMethod, String path,
        MultivaluedHashMap<String, Object> headers,
        Object body, MediaType contentType, MediaType accept, InvocationCallback<T> callback)
        throws VitamClientInternalException {
        try {
            ParametersChecker.checkParameter(ARGUMENT_CANNOT_BE_NULL_EXCEPT_HEADERS, callback);
            if (body != null) {
                ParametersChecker.checkParameter(BODY_AND_CONTENT_TYPE_CANNOT_BE_NULL, body, contentType);
                final Builder builder = buildRequest(httpMethod, path, headers, accept, getChunkedMode());
                return retryIfNecessary(httpMethod, body, contentType, builder.async(), callback);
            } else {
                final Builder builder = buildRequest(httpMethod, path, headers, accept, false);
                return retryIfNecessary(httpMethod, null, null, builder.async(), callback);
            }
        } catch (final ProcessingException e) {
            throw new VitamClientInternalException(e);
        }
    }

    /**
     * @param httpMethod
     * @param body may be null
     * @param contentType may be null
     * @param builder
     * @return the response from the server as Future
     * @throws VitamClientInternalException if retry is not possible and http call is failed
     */
    private final Future<Response> retryIfNecessary(String httpMethod, Object body, MediaType contentType,
        AsyncInvoker builder)
        throws VitamClientInternalException {
        ProcessingException lastException = null;
        if (body instanceof InputStream) {
            for (int i = 0; i < VitamConfiguration.getRetryNumber(); i++) {
                try {
                    Entity<Object> entity = Entity.entity(body, contentType);
                    return builder.method(httpMethod, entity);
                } catch (ProcessingException e) {
                    lastException = checkSpecificExceptionForRetryUsingStream(i, e);
                    continue;
                }
            }
        } else {
            for (int i = 0; i < VitamConfiguration.getRetryNumber(); i++) {
                try {
                    if (body == null) {
                        return builder.method(httpMethod);
                    } else {
                        Entity<Object> entity = Entity.entity(body, contentType);
                        return builder.method(httpMethod, entity);
                    }
                } catch (ProcessingException e) {
                    lastException = checkSpecificExceptionForRetry(i, e);
                    continue;
                }
            }
        }
        if (lastException != null) {
            LOGGER.error(lastException);
            throw lastException;
        } else {
            throw new VitamClientInternalException(UNKNOWN_ERROR_IN_CLIENT);
        }
    }

    /**
     * Perform an Async HTTP request to the server with full control of action on caller
     *
     * @param httpMethod HTTP method to use for request
     * @param path URL to request
     * @param headers headers HTTP to add to request, may be null
     * @param body body content of type contentType, may be null
     * @param contentType the media type of the body to send, null if body is null
     * @param accept asked type of response
     * @return the response from the server as a Future
     * @throws VitamClientInternalException
     */
    protected Future<Response> performAsyncRequest(String httpMethod, String path,
        MultivaluedHashMap<String, Object> headers,
        Object body, MediaType contentType, MediaType accept)
        throws VitamClientInternalException {
        try {
            if (body != null) {
                ParametersChecker.checkParameter(BODY_AND_CONTENT_TYPE_CANNOT_BE_NULL, body, contentType);
                final Builder builder = buildRequest(httpMethod, path, headers, accept, getChunkedMode());
                return retryIfNecessary(httpMethod, body, contentType, builder.async());
            } else {
                final Builder builder = buildRequest(httpMethod, path, headers, accept, false);
                return retryIfNecessary(httpMethod, null, null, builder.async());
            }
        } catch (final ProcessingException e) {
            throw new VitamClientInternalException(e);
        }
    }

    /**
     * Handle all errors and throw a VitamClientException in case the response does not contains a vitamError type
     * result.
     *
     * @param response response
     * @return VitamClientException exception thrown for the response
     */
    protected VitamClientException createExceptionFromResponse(Response response) {
        VitamClientException exception = new VitamClientException(INTERNAL_SERVER_ERROR);
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

    @Override
    public String toString() {
        return "VitamClient: { " + clientFactory.toString() + " }";
    }

    /**
     * Build a HTTP request to the server for synchronous call without Body
     *
     * @param httpMethod HTTP method to use for request
     * @param path URL to request
     * @param headers headers HTTP to add to request, may be null
     * @param accept asked type of response
     * @param chunkedMode True use default client, else False use non Chunked mode client
     * @return the builder ready to be performed
     */
    final Builder buildRequest(String httpMethod, String path, MultivaluedMap<String, Object> headers,
        MediaType accept,
        boolean chunkedMode) {
        return buildRequest(httpMethod, getServiceUrl(), path, headers, accept, chunkedMode);
    }

    /**
     * Build a HTTP request to the server for synchronous call without Body
     *
     * @param httpMethod HTTP method to use for request
     * @param path URL to request
     * @param headers headers HTTP to add to request, may be null
     * @param queryParams query parameters to add to get request, my be null
     * @param accept asked type of response
     * @param chunkedMode True use default client, else False use non Chunked mode client
     * @return the builder ready to be performed
     */
    final Builder buildRequest(String httpMethod, String path, MultivaluedMap<String, Object> headers,
        MultivaluedMap<String, Object> queryParams,
        MediaType accept,
        boolean chunkedMode) {
        return buildRequest(httpMethod, getServiceUrl(), path, headers, queryParams, accept, chunkedMode);
    }

    /**
     * Build a HTTP request to the server for synchronous call without Body
     *
     * @param httpMethod HTTP method to use for request
     * @param url base url
     * @param path URL to request
     * @param headers headers HTTP to add to request, may be null
     * @param queryParams query parameters to add to get request, my be null
     * @param accept asked type of response
     * @param chunkedMode True use default client, else False use non Chunked mode client
     * @return the builder ready to be performed
     */
    final Builder buildRequest(String httpMethod, String url, String path, MultivaluedMap<String, Object> headers,
        MultivaluedMap<String, Object> queryParams,
        MediaType accept, boolean chunkedMode) {

        ParametersChecker.checkParameter(ARGUMENT_CANNOT_BE_NULL_EXCEPT_HEADERS, httpMethod, path, accept);

        WebTarget webTarget = getHttpClient(chunkedMode).target(url).path(path);

        //add query parameters
        if (HttpMethod.GET.equals(httpMethod) && queryParams != null) {
            for (final Entry<String, List<Object>> entry : queryParams.entrySet()) {
                for (final Object value : entry.getValue()) {
                    webTarget = webTarget.queryParam(entry.getKey(), value);
                }
            }
        }

        final Builder builder = webTarget.request().accept(accept);

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

        String newPath = path;
        if (newPath.codePointAt(0) != '/') {
            newPath = "/" + newPath;
        }

        String baseUri = getResourcePath() + newPath;
        if (url.endsWith(VitamConfiguration.ADMIN_PATH)) {
            baseUri = VitamConfiguration.ADMIN_PATH + newPath;
        }

        // add Authorization Headers (X_TIMESTAMP, X_PLATFORM_ID)
        if (clientFactory.useAuthorizationFilter()) {
            final Map<String, String> authorizationHeaders =
                AuthorizationFilterHelper.getAuthorizationHeaders(httpMethod,
                    baseUri);
            if (authorizationHeaders.size() == 2) {
                builder.header(GlobalDataRest.X_TIMESTAMP, authorizationHeaders.get(GlobalDataRest.X_TIMESTAMP));
                builder.header(GlobalDataRest.X_PLATFORM_ID, authorizationHeaders.get(GlobalDataRest.X_PLATFORM_ID));
            }
        }
        return builder;
    }

    /**
     * Build a HTTP request to the server for synchronous call without Body
     *
     * @param httpMethod HTTP method to use for request
     * @param url base url
     * @param path URL to request
     * @param headers headers HTTP to add to request, may be null
     * @param accept asked type of response
     * @param chunkedMode True use default client, else False use non Chunked mode client
     * @return the builder ready to be performed
     */
    final Builder buildRequest(String httpMethod, String url, String path, MultivaluedMap<String, Object> headers,
        MediaType accept, boolean chunkedMode) {
        return buildRequest(httpMethod, url, path, headers, null, accept, chunkedMode);
    }

    /**
     * Get the internal Http client
     *
     * @return the client
     */
    Client getHttpClient() {
        return getHttpClient(getChunkedMode());
    }

    /**
     * Get the internal Http client according to the chunk mode
     *
     * @param useChunkedMode
     * @return the client
     */
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

    /**
     * @return the client chunked mode default configuration
     */
    boolean getChunkedMode() {
        return clientFactory.getChunkedMode();
    }

    /**
     * @return the VitamClientFactory
     */
    public VitamClientFactory<?> getVitamClientFactory() {
        return clientFactory;
    }
}
