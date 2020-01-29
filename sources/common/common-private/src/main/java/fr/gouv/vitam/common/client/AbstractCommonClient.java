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

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.configuration.ClientConfiguration;
import fr.gouv.vitam.common.exception.VitamApplicationServerDisconnectException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.retryable.DelegateRetry;
import fr.gouv.vitam.common.retryable.RetryableOnException;
import fr.gouv.vitam.common.retryable.RetryableParameters;
import fr.gouv.vitam.common.security.filter.AuthorizationFilterHelper;
import fr.gouv.vitam.common.stream.StreamUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NoHttpResponseException;
import org.apache.http.conn.ConnectTimeoutException;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;

import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.ws.rs.core.Response.Status.Family.REDIRECTION;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;

abstract class AbstractCommonClient implements BasicClient {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AbstractCommonClient.class);

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

    AbstractCommonClient(VitamClientFactoryInterface<?> factory) {
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
    public static void staticConsumeAnyEntityAndClose(Response response) {
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
        VitamRequestBuilder request = VitamRequestBuilder.get().withPath(STATUS_URL).withHeaders(headers).withJsonAccept();
        try (Response response = make(request)) {
            Response.Status status = response.getStatusInfo().toEnum();
            if (SUCCESSFUL.equals(status.getFamily()) || REDIRECTION.equals(status.getFamily())) {
                return;
            }
            LOGGER.error(status.getReasonPhrase());
            throw new VitamApplicationServerException(status.getReasonPhrase());
        } catch (VitamClientInternalException e) {
            throw new VitamApplicationServerDisconnectException(e);
        }
    }

    public Response makeSpecifyingUrl(VitamRequestBuilder request) throws VitamClientInternalException {
        if (StringUtils.isBlank(request.getBaseUrl())) {
            throw new VitamRuntimeException("Base URL must not be 'null nor empty' with method 'makeSpecifyingUrl'.");
        }
        return doRequest(request);
    }

    public Response make(VitamRequestBuilder request) throws VitamClientInternalException {
        if (StringUtils.isNotBlank(request.getBaseUrl())) {
            throw new VitamRuntimeException(String.format("Base URL must not be 'set' with method 'make' it will be override, here it equals '%s'.", request.getBaseUrl()));
        }
        request.withBaseUrl(getServiceUrl());
        return doRequest(request);
    }

    private Response doRequest(VitamRequestBuilder request) throws VitamClientInternalException {
        request.runBeforeExecRequest();

        try {
            Builder builder = buildRequest(
                request.getHttpMethod(),
                request.getBaseUrl(),
                request.getPath(),
                request.getHeaders(),
                request.getAccept(),
                request.isChunckedMode(),
                request.getQueryParams()
            );

            Response response = retryIfNecessary(
                request.getHttpMethod(),
                request.getBody(),
                request.getContentType(),
                builder
            );

            return new VitamAutoClosableResponse(response);
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

    private Builder buildRequest(String httpMethod, String url, String path, MultivaluedMap<String, Object> headers, MediaType accept, boolean chunkedMode, MultivaluedMap<String, Object> queryParams) {
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
