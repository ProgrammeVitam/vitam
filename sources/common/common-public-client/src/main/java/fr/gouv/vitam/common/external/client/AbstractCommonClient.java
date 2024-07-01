/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.external.client;

import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.VitamAutoClosableResponse;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.client.VitamRequestBuilder;
import fr.gouv.vitam.common.exception.VitamApplicationServerDisconnectException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.retryable.DelegateRetry;
import fr.gouv.vitam.common.retryable.RetryableOnException;
import fr.gouv.vitam.common.retryable.RetryableParameters;
import fr.gouv.vitam.common.stream.StreamUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.NoHttpResponseException;
import org.apache.http.conn.ConnectTimeoutException;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Predicate;

import static fr.gouv.vitam.common.CommonMediaType.GZIP_TYPE;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.ws.rs.core.HttpHeaders.ACCEPT_ENCODING;
import static javax.ws.rs.core.HttpHeaders.CONTENT_ENCODING;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;

abstract class AbstractCommonClient implements BasicClient {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AbstractCommonClient.class);

    private final RetryableParameters retryableParameters;
    private final Client chunkedClient;
    private final Client client;
    private final VitamClientFactory<?> clientFactory;

    private final Predicate<Exception> retryOnException = e -> {
        if (isNetworkException(e)) {
            return true;
        }

        final List<Throwable> throwableList = ExceptionUtils.getThrowableList(e);

        for (Throwable th : throwableList) {
            if (isNetworkException(th)) {
                return true;
            }
        }

        return false;
    };

    private boolean isNetworkException(Throwable th) {
        return (
            th instanceof ConnectTimeoutException ||
            th instanceof UnknownHostException ||
            th instanceof NoHttpResponseException ||
            th instanceof SocketException
        );
    }

    AbstractCommonClient(VitamClientFactoryInterface<?> factory) {
        this.clientFactory = (VitamClientFactory<?>) factory;
        this.chunkedClient = clientFactory.getHttpClient(true);
        this.client = clientFactory.getHttpClient(false);
        this.retryableParameters = new RetryableParameters(
            VitamConfiguration.getHttpClientRetry(),
            VitamConfiguration.getHttpClientFirstAttemptWaitingTime(),
            VitamConfiguration.getHttpClientWaitingTime(),
            VitamConfiguration.getHttpClientRandomWaitingSleep(),
            SECONDS
        );
    }

    public static void staticConsumeAnyEntityAndClose(Response response) {
        StreamUtils.consumeAnyEntityAndClose(response);
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
    public void checkStatus(MultivaluedHashMap<String, Object> headers) throws VitamApplicationServerException {
        VitamRequestBuilder request = VitamRequestBuilder.get().withPath(STATUS_URL).withJsonAccept();
        if (headers != null) {
            request.withHeaders(headers);
        }
        try (Response response = make(request)) {
            Response.Status status = response.getStatusInfo().toEnum();
            if (SUCCESSFUL.equals(status.getFamily())) {
                return;
            }
            LOGGER.error(status.getReasonPhrase());
            throw new VitamApplicationServerException(status.getReasonPhrase());
        } catch (VitamClientInternalException e) {
            throw new VitamApplicationServerDisconnectException(e);
        }
    }

    @Override
    public String getResourcePath() {
        return this.clientFactory.getResourcePath();
    }

    @Override
    public String getServiceUrl() {
        return this.clientFactory.getServiceUrl();
    }

    @Override
    public void close() {
        if (client != null) {
            this.clientFactory.resume(this.client, false);
        }
        if (this.chunkedClient != null) {
            this.clientFactory.resume(this.chunkedClient, true);
        }
    }

    @Override
    public String toString() {
        return "VitamClient: { " + clientFactory.toString() + " }";
    }

    public Response makeSpecifyingUrl(VitamRequestBuilder request) throws VitamClientInternalException {
        if (StringUtils.isBlank(request.getBaseUrl())) {
            throw new VitamRuntimeException("Base URL must not be 'null nor empty' with method 'makeSpecifyingUrl'.");
        }
        return doRequest(request);
    }

    public Response make(VitamRequestBuilder request) throws VitamClientInternalException {
        if (StringUtils.isNotBlank(request.getBaseUrl())) {
            throw new VitamRuntimeException(
                String.format(
                    "Base URL must not be 'set' with method 'make' it will be override, here it equals '%s'.",
                    request.getBaseUrl()
                )
            );
        }
        request.withBaseUrl(getServiceUrl());
        return doRequest(request);
    }

    private Response doRequest(VitamRequestBuilder request) throws VitamClientInternalException {
        request.runBeforeExecRequest();

        if (this.clientFactory.isAllowGzipEncoded()) {
            request.withHeader(CONTENT_ENCODING, GZIP_TYPE.getSubtype());
        }
        if (this.clientFactory.isAllowGzipDecoded()) {
            request.withHeader(ACCEPT_ENCODING, GZIP_TYPE.getSubtype());
        }

        try {
            Object body = request.getBody();
            if (body instanceof InputStream) {
                Response response = builder(request).method(
                    request.getHttpMethod(),
                    Entity.entity(body, request.getContentType())
                );
                return new VitamAutoClosableResponse(response);
            }

            DelegateRetry<Response, ProcessingException> delegate = () -> {
                if (body == null) {
                    return builder(request).method(request.getHttpMethod());
                }
                return builder(request).method(request.getHttpMethod(), Entity.entity(body, request.getContentType()));
            };

            return new VitamAutoClosableResponse(retryable().exec(delegate));
        } catch (final ProcessingException e) {
            throw new VitamClientInternalException(e);
        }
    }

    private RetryableOnException<Response, ProcessingException> retryable() {
        return new RetryableOnException<>(this.retryableParameters, this.retryOnException);
    }

    private Builder builder(VitamRequestBuilder request) {
        Client client = request.isChunckedMode() ? this.chunkedClient : this.client;

        WebTarget webTarget = client.target(request.getBaseUrl()).path(request.getPath());

        for (Entry<String, String> entry : request.getQueryParams().entrySet()) {
            webTarget = webTarget.queryParam(entry.getKey(), entry.getValue());
        }

        return webTarget.request().headers(request.getHeaders()).accept(request.getAccept());
    }

    public Client getChunkedClient() {
        return chunkedClient;
    }

    public Client getClient() {
        return client;
    }

    public VitamClientFactory<?> getClientFactory() {
        return clientFactory;
    }
}
