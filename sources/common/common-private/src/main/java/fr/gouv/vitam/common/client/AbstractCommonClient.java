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

import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.Future;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.http.conn.ConnectTimeoutException;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.configuration.ClientConfiguration;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.security.filter.AuthorizationFilterHelper;
import fr.gouv.vitam.common.stream.StreamUtils;

/**
 * Abstract Partial client class for all vitam clients
 */
abstract class AbstractCommonClient implements BasicClient {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AbstractCommonClient.class);
    private static final String BODY_AND_CONTENT_TYPE_CANNOT_BE_NULL = "Body and ContentType cannot be null";

    /**
     * Multipart response from Server side
     */
    public static final String MULTIPART_MIXED = "multipart/mixed";
    public static final MediaType MULTIPART_MIXED_TYPE = new MediaType("multipart", "mixed");

    private static final String ARGUMENT_CANNOT_BE_NULL_EXCEPT_HEADERS = "Argument cannot be null except headers";
    protected static final String INTERNAL_SERVER_ERROR = "Internal Server Error";

    /**
     * Client Factory
     */
    final VitamClientFactory<?> clientFactory;

    private final Client client;
    private final Client clientNotChunked;
    private final Random random = new Random(System.currentTimeMillis());
    
    /**
     * Constructor with standard configuration
     *
     * @param factory
     *            The client factory
     */
    protected AbstractCommonClient(VitamClientFactoryInterface<?> factory) {
        clientFactory = (VitamClientFactory<?>) factory;
        client = clientFactory.getHttpClient();
        clientNotChunked = clientFactory.getHttpClient(false);
        // External client or with no Session context are excluded
        // TODO: Find a better check (a specific one, instead of inferring the context from another constraint ?);
        if (clientFactory.useAuthorizationFilter()) {
            client.register(RequestIdClientFilter.class);
            client.register(TenantIdClientFilter.class);
            clientNotChunked.register(RequestIdClientFilter.class);
            clientNotChunked.register(TenantIdClientFilter.class);
        }
    }

    @Override
    public final void consumeAnyEntityAndClose(Response response) {
        staticConsumeAnyEntityAndClose(response);
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
        } catch (final IllegalStateException | ProcessingException e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    @Override
    public void checkStatus() throws VitamApplicationServerException {
        Response response = null;
        try {
            response = performRequest(HttpMethod.GET, STATUS_URL, null, MediaType.APPLICATION_JSON_TYPE, false);
            final Response.Status status = Response.Status.fromStatusCode(response.getStatus());
            consumeAnyEntityAndClose(response);
            if (status == Status.OK || status == Status.NO_CONTENT) {
                return;
            }
            final String messageText = INTERNAL_SERVER_ERROR + " : " + status.getReasonPhrase();
            LOGGER.error(messageText);
            throw new VitamApplicationServerException(messageText);
        } catch (ProcessingException | VitamClientInternalException e) {
            final String messageText = INTERNAL_SERVER_ERROR + " : " + e.getMessage();
            LOGGER.error(messageText);
            throw new VitamApplicationServerException(messageText, e);
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    /**
     * Perform a HTTP request to the server for synchronous call using default chunked mode configured in this client
     *
     * @param httpMethod
     *            HTTP method to use for request
     * @param path
     *            URL to request
     * @param headers
     *            headers HTTP to add to request, may be null
     * @param accept
     *            asked type of response
     * @return the response from the server
     * @throws VitamClientInternalException
     */
    protected Response performRequest(String httpMethod, String path, MultivaluedHashMap<String, Object> headers,
            MediaType accept) throws VitamClientInternalException {
        final boolean chunkFinalMode = getChunkedMode() && !HttpMethod.HEAD.equals(path) && !HttpMethod.OPTIONS.equals(path);
        return performRequest(httpMethod, path, headers, accept, chunkFinalMode);
    }

    /**
     * Helper for retry request when unreachable or Connect timeout
     * 
     * @param retry retry count
     * @param e
     *            the original ProcessingException
     * @return the original exception allowing to continue and store the last one
     * @throws ProcessingException
     */
    private ProcessingException checkSpecificExceptionForRetry(int retry, ProcessingException e) throws ProcessingException {
        Throwable source = e.getCause();
        if (source instanceof ConnectTimeoutException || source instanceof UnknownHostException
                || source.getMessage().startsWith("Unable to establish route:")) {
            LOGGER.warn("TimeoutOccurs or DNS probe error, retry: " + retry, source);
            try {
                long sleep = random.nextInt(50) + 10;
                Thread.sleep(sleep);
            } catch (InterruptedException e1) {
                throw new ProcessingException("Interruption received", e1);
            }
            return e;
        } else {
            throw e;
        }
    }

    /**
     * Perform a HTTP request to the server for synchronous call
     *
     * @param httpMethod
     *            HTTP method to use for request
     * @param path
     *            URL to request
     * @param headers
     *            headers HTTP to add to request, may be null
     * @param accept
     *            asked type of response
     * @param chunkedMode
     *            True use default client, else False use non Chunked mode client
     * @return the response from the server
     * @throws VitamClientInternalException
     */
    protected Response performRequest(String httpMethod, String path, MultivaluedHashMap<String, Object> headers,
            MediaType accept, boolean chunkedMode) throws VitamClientInternalException {
        try {
            final Builder builder = buildRequest(httpMethod, path, headers, accept, chunkedMode);
            ProcessingException lastException = null;
            for (int i = 0; i < VitamConfiguration.getRetryNumber(); i++) {
                try {
                    return builder.method(httpMethod);
                } catch (ProcessingException e) {
                    lastException = checkSpecificExceptionForRetry(i, e);
                    continue;
                }
            }
            if (lastException != null) {
                throw lastException;
            } else {
                throw new VitamClientInternalException("Unknown error in client");
            }
        } catch (final ProcessingException e) {
            throw new VitamClientInternalException(e);
        }
    }

    /**
     * Perform a HTTP request to the server for synchronous call
     *
     * @param httpMethod
     *            HTTP method to use for request
     * @param path
     *            URL to request
     * @param headers
     *            headers HTTP to add to request, may be null
     * @param body
     *            body content of type contentType, may be null
     * @param contentType
     *            the media type of the body to send, null if body is null
     * @param accept
     *            asked type of response
     * @return the response from the server
     * @throws VitamClientInternalException
     */
    protected Response performRequest(String httpMethod, String path, MultivaluedHashMap<String, Object> headers, Object body,
            MediaType contentType, MediaType accept) throws VitamClientInternalException {
        if (body == null) {
            return performRequest(httpMethod, path, headers, accept, getChunkedMode());
        }
        try {
            ParametersChecker.checkParameter(BODY_AND_CONTENT_TYPE_CANNOT_BE_NULL, body, contentType);
            final Builder builder = buildRequest(httpMethod, path, headers, accept, getChunkedMode());
            Entity<Object> entity = Entity.entity(body, contentType);
            return builder.method(httpMethod, entity);
        } catch (final ProcessingException e) {
            throw new VitamClientInternalException(e);
        }
    }

    /**
     * Perform a HTTP request to the server for synchronous call
     *
     * @param httpMethod
     *            HTTP method to use for request
     * @param path
     *            URL to request
     * @param headers
     *            headers HTTP to add to request, may be null
     * @param body
     *            body content of type contentType, may be null
     * @param contentType
     *            the media type of the body to send, null if body is null
     * @param accept
     *            asked type of response
     * @param chunkedMode
     *            True use default client, else False use non Chunked mode client
     * @return the response from the server
     * @throws VitamClientInternalException
     */
    protected Response performRequest(String httpMethod, String path, MultivaluedHashMap<String, Object> headers, Object body,
            MediaType contentType, MediaType accept, boolean chunkedMode) throws VitamClientInternalException {
        if (body == null) {
            return performRequest(httpMethod, path, headers, accept, getChunkedMode());
        }
        try {
            ParametersChecker.checkParameter(BODY_AND_CONTENT_TYPE_CANNOT_BE_NULL, body, contentType);
            final Builder builder = buildRequest(httpMethod, path, headers, accept, chunkedMode);
            Entity<Object> entity = Entity.entity(body, contentType);
            return builder.method(httpMethod, entity);
        } catch (final ProcessingException e) {
            throw new VitamClientInternalException(e);
        }
    }

    /**
     * Perform an Async HTTP request to the server with callback
     *
     * @param httpMethod
     *            HTTP method to use for request
     * @param path
     *            URL to request
     * @param headers
     *            headers HTTP to add to request, may be null
     * @param body
     *            body content of type contentType, may be null
     * @param contentType
     *            the media type of the body to send, null if body is null
     * @param accept
     *            asked type of response
     * @param callback
     * @param <T>
     *            the type of the Future result (generally Response)
     * @return the response from the server
     * @throws VitamClientInternalException
     */
    protected <T> Future<T> performAsyncRequest(String httpMethod, String path, MultivaluedHashMap<String, Object> headers,
            Object body, MediaType contentType, MediaType accept, InvocationCallback<T> callback)
            throws VitamClientInternalException {
        try {
            ParametersChecker.checkParameter(ARGUMENT_CANNOT_BE_NULL_EXCEPT_HEADERS, callback);
            if (body != null) {
                ParametersChecker.checkParameter(BODY_AND_CONTENT_TYPE_CANNOT_BE_NULL, body, contentType);
            }
            final Builder builder = buildRequest(httpMethod, path, headers, accept, getChunkedMode());
            ProcessingException lastException = null;
            Entity<Object> entity = body != null ? Entity.entity(body, contentType) : null;
            if (entity != null) {
                return builder.async().method(httpMethod, entity, callback);
            } else {
                for (int i = 0; i < VitamConfiguration.getRetryNumber(); i++) {
                    try {
                        return builder.async().method(httpMethod, callback);
                    } catch (ProcessingException e) {
                        lastException = checkSpecificExceptionForRetry(i, e);
                        continue;
                    }
                }
                if (lastException != null) {
                    throw lastException;
                } else {
                    throw new VitamClientInternalException("Unknown error in client");
                }
            }
        } catch (final ProcessingException e) {
            throw new VitamClientInternalException(e);
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
        Object body, MediaType contentType, MediaType accept) throws VitamClientInternalException {
        try {
            if (body != null) {
                ParametersChecker.checkParameter(BODY_AND_CONTENT_TYPE_CANNOT_BE_NULL,
                    body, contentType);
            }
            final Builder builder = buildRequest(httpMethod, path, headers, accept, getChunkedMode());
            ProcessingException lastException = null;
            Entity<Object> entity = body != null ? Entity.entity(body, contentType) : null;
            if (entity != null) {
                return builder.async().method(httpMethod, entity);
            } else {
                for (int i = 0; i < VitamConfiguration.getRetryNumber(); i++) {
                    try {
                        return builder.async().method(httpMethod);
                    } catch (ProcessingException e) {
                        lastException = checkSpecificExceptionForRetry(i, e);
                        continue;
                    }
                }
                if (lastException != null) {
                    throw lastException;
                } else {
                    throw new VitamClientInternalException("Unknown error in client");
                }
            }
        } catch (final ProcessingException e) {
            throw new VitamClientInternalException(e);
        }
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
            client.close();
        }
        if (clientNotChunked != null) {
            clientNotChunked.close();
        }
    }

    @Override
    public String toString() {
        return new StringBuilder("VitamClient: { ").append(clientFactory.toString()).append(" }").toString();
    }

    /**
     * Build a HTTP request to the server for synchronous call without Body
     *
     * @param httpMethod
     *            HTTP method to use for request
     * @param path
     *            URL to request
     * @param headers
     *            headers HTTP to add to request, may be null
     * @param accept
     *            asked type of response
     * @param chunkedMode
     *            True use default client, else False use non Chunked mode client
     * @return the builder ready to be performed
     */
    final Builder buildRequest(String httpMethod, String path, MultivaluedHashMap<String, Object> headers, MediaType accept,
            boolean chunkedMode) {
        return buildRequest(httpMethod, getServiceUrl(), path, headers, accept, chunkedMode);
    }

    /**
     * Build a HTTP request to the server for synchronous call without Body
     *
     * @param httpMethod
     *            HTTP method to use for request
     * @param url
     *            base url
     * @param path
     *            URL to request
     * @param headers
     *            headers HTTP to add to request, may be null
     * @param accept
     *            asked type of response
     * @param chunkedMode
     *            True use default client, else False use non Chunked mode client
     * @return the builder ready to be performed
     */
    final Builder buildRequest(String httpMethod, String url, String path, MultivaluedHashMap<String, Object> headers,
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
            final Map<String, String> authorizationHeaders = AuthorizationFilterHelper.getAuthorizationHeaders(httpMethod,
                    baseUri);
            if (authorizationHeaders.size() == 2) {
                builder.header(GlobalDataRest.X_TIMESTAMP, authorizationHeaders.get(GlobalDataRest.X_TIMESTAMP));
                builder.header(GlobalDataRest.X_PLATFORM_ID, authorizationHeaders.get(GlobalDataRest.X_PLATFORM_ID));
            }
        }
        return builder;
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
     *
     * @return the client chunked mode default configuration
     */
    boolean getChunkedMode() {
        return clientFactory.getChunkedMode();
    }

    /**
     *
     * @return the VitamClientFactory
     */
    public VitamClientFactory<?> getVitamClientFactory() {
        return clientFactory;
    }
}
