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
import fr.gouv.vitam.common.client.VitamRestEasyConfiguration;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.stream.StreamUtils;
import org.apache.commons.io.input.NullInputStream;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.ParseException;
import org.apache.http.ProtocolVersion;
import org.apache.http.TokenIterator;
import org.apache.http.annotation.Contract;
import org.apache.http.annotation.ThreadingBehavior;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.message.BasicTokenIterator;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.util.Args;
import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.engines.SelfExpandingBufferredInputStream;
import org.jboss.resteasy.client.jaxrs.internal.ClientInvocation;
import org.jboss.resteasy.client.jaxrs.internal.ClientRequestHeaders;
import org.jboss.resteasy.client.jaxrs.internal.ClientResponse;
import org.jboss.resteasy.tracing.RESTEasyTracingLogger;
import org.jboss.resteasy.util.CaseInsensitiveMap;
import org.jboss.resteasy.util.DelegatingOutputStream;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Invocation;
import java.io.BufferedInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Vtam Specific Apache Http Client Engine
 *
 * In particular, handle chunk mode
 */
public class VitamApacheHttpClientEngine implements ClientHttpEngine {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(VitamApacheHttpClientEngine.class);

    private static final ConnectionKeepAliveStrategy MY_KEEP_ALIVE_STRATEGY;
    private static final VitamConnectionReuseStrategy MY_CONNECTION_REUSE_STRATEGY = new VitamConnectionReuseStrategy();

    static {
        MY_KEEP_ALIVE_STRATEGY = (response, context) -> {
            // Honor 'keep-alive' header
            final HeaderElementIterator it = new BasicHeaderElementIterator(
                response.headerIterator(HTTP.CONN_KEEP_ALIVE)
            );
            while (it.hasNext()) {
                final HeaderElement he = it.nextElement();
                final String param = he.getName();
                final String value = he.getValue();

                if (value != null && "timeout".equalsIgnoreCase(param)) {
                    try {
                        return Long.parseLong(value) * 1000;
                    } catch (NumberFormatException ignore) {
                        LOGGER.warn(ignore);
                    }
                }
            }
            // otherwise keep alive for 60 seconds
            return VitamConfiguration.getMaxDelayUnusedConnection();
        };
    }

    private CookieStore cookieStore;
    private RequestConfig requestConfig;
    private HttpClientConnectionManager httpClientConnectionManager;
    private Map<VitamRestEasyConfiguration, Object> config;
    private final int connectTimeout;
    private final int socketTimeout;
    private final int bufferSize;
    private final boolean bufferingEnabled;
    private SSLContext sslContext;
    private int responseBufferSize = 0;
    private CloseableHttpClient httpClient;
    private boolean closed;

    /**
     * Default constructor
     *
     * @param config
     */
    public VitamApacheHttpClientEngine(Map<VitamRestEasyConfiguration, Object> config) {
        this.config = config;
        final Object cm0 = VitamRestEasyConfiguration.CONNECTION_MANAGER.getObject(config);
        if (cm0 != null && !(cm0 instanceof HttpClientConnectionManager)) {
            LOGGER.error(VitamRestEasyConfiguration.CONNECTION_MANAGER.name() + " missing");
            throw new IllegalArgumentException(
                VitamRestEasyConfiguration.CONNECTION_MANAGER.name() + " will be ignored"
            );
        }
        httpClientConnectionManager = (HttpClientConnectionManager) cm0;
        final Object reqConfig = VitamRestEasyConfiguration.REQUEST_CONFIG.getObject(config);
        if (reqConfig != null && !(reqConfig instanceof RequestConfig)) {
            LOGGER.warn(VitamRestEasyConfiguration.REQUEST_CONFIG.name() + " will be ignored");
        }

        sslContext = (SSLContext) VitamRestEasyConfiguration.SSL_CONTEXT.getObject(config);

        final HttpClientBuilder clientBuilder = HttpClientBuilder.create();
        clientBuilder.useSystemProperties();

        final boolean disableAutomaticRetries = VitamRestEasyConfiguration.DISABLE_AUTOMATIC_RETRIES.isTrue(config);
        if (disableAutomaticRetries) {
            clientBuilder.disableAutomaticRetries();
        }
        clientBuilder.setConnectionManager(httpClientConnectionManager);
        clientBuilder.setConnectionManagerShared(VitamRestEasyConfiguration.CONNECTION_MANAGER_SHARED.isTrue(config));

        final RequestConfig.Builder requestConfigBuilder = RequestConfig.custom();

        final Object credentialsProvider = VitamRestEasyConfiguration.CREDENTIALS_PROVIDER.getObject(config);
        if (credentialsProvider != null && credentialsProvider instanceof CredentialsProvider) {
            clientBuilder.setDefaultCredentialsProvider((CredentialsProvider) credentialsProvider);
        }

        final Object proxyUri;
        proxyUri = VitamRestEasyConfiguration.PROXY_URI.getObject(config);
        if (proxyUri != null) {
            final URI u = getProxyUri(proxyUri);
            final HttpHost proxy = new HttpHost(u.getHost(), u.getPort(), u.getScheme());
            final String userName;
            userName = VitamRestEasyConfiguration.PROXY_USERNAME.getString(config, null);
            if (userName != null) {
                final String password;
                password = VitamRestEasyConfiguration.PROXY_PASSWORD.getString(config, null);

                if (password != null) {
                    final CredentialsProvider credsProvider = new BasicCredentialsProvider();
                    credsProvider.setCredentials(
                        new AuthScope(u.getHost(), u.getPort()),
                        new UsernamePasswordCredentials(userName, password)
                    );
                    clientBuilder.setDefaultCredentialsProvider(credsProvider);
                }
            }
            clientBuilder.setProxy(proxy);
        }

        if (reqConfig != null) {
            final RequestConfig.Builder reqConfigBuilder = RequestConfig.copy((RequestConfig) reqConfig);
            requestConfig = reqConfigBuilder.build();
        } else {
            requestConfig = requestConfigBuilder.build();
        }

        if (
            requestConfig.getCookieSpec() == null || !requestConfig.getCookieSpec().equals(CookieSpecs.IGNORE_COOKIES)
        ) {
            cookieStore = new BasicCookieStore();
            clientBuilder.setDefaultCookieStore(cookieStore);
        } else {
            cookieStore = null;
        }
        clientBuilder.setConnectionReuseStrategy(MY_CONNECTION_REUSE_STRATEGY);
        clientBuilder.setKeepAliveStrategy(MY_KEEP_ALIVE_STRATEGY);
        clientBuilder.setDefaultRequestConfig(requestConfig);
        if (!VitamRestEasyConfiguration.CONTENTCOMPRESSIONENABLED.isTrue(config)) {
            clientBuilder.disableContentCompression();
        }

        connectTimeout = VitamRestEasyConfiguration.CONNECT_TIMEOUT.getInt(config, 1000);
        socketTimeout = VitamRestEasyConfiguration.READ_TIMEOUT.getInt(config, 1000);
        bufferSize = VitamRestEasyConfiguration.CHUNKED_ENCODING_SIZE.getInt(config, 8192);
        responseBufferSize = VitamRestEasyConfiguration.RECV_BUFFER_SIZE.getInt(config, 0);
        bufferingEnabled = VitamRestEasyConfiguration.BUFFERED.equalsIgnoreCase(
            VitamRestEasyConfiguration.REQUEST_ENTITY_PROCESSING.getString(config, VitamRestEasyConfiguration.CHUNKED)
        );

        httpClient = clientBuilder.build();
        closed = false;
        LOGGER.debug("{}", this);
    }

    @Override
    public String toString() {
        return (
            "connectTimeout: " +
            connectTimeout +
            " socketTimeout: " +
            socketTimeout +
            " bufferSize: " +
            bufferSize +
            " responseBufferSize: " +
            responseBufferSize +
            " bufferingEnabled: " +
            bufferingEnabled +
            " config: " +
            config
        );
    }

    private static URI getProxyUri(final Object proxy) {
        if (proxy instanceof URI) {
            return (URI) proxy;
        } else if (proxy instanceof String) {
            return URI.create((String) proxy);
        } else {
            throw new ProcessingException("WRONG_PROXY_URI_TYPE");
        }
    }

    @Override
    public SSLContext getSslContext() {
        return sslContext;
    }

    @Override
    public HostnameVerifier getHostnameVerifier() {
        return null;
    }

    @Override
    public ClientResponse invoke(Invocation clientInvocation) {
        final HttpUriRequest request = getUriHttpRequest((ClientInvocation) clientInvocation);
        writeOutBoundHeaders(((ClientInvocation) clientInvocation).getHeaders(), request);

        try {
            final CloseableHttpResponse response;
            final HttpClientContext context = HttpClientContext.create();

            response = httpClient.execute(getHost(request), request, context);

            final int statusCode = response.getStatusLine().getStatusCode();

            final ClientResponse responseContext = new ClientResponse(
                ((ClientInvocation) clientInvocation).getClientConfiguration(),
                RESTEasyTracingLogger.empty()
            ) {
                InputStream stream = getNativeInputStream(response);

                // Bad Way but no other way to do it !
                {
                    setEntity(stream);
                }

                @Override
                protected InputStream getInputStream() {
                    return stream;
                }

                @Override
                protected void setInputStream(InputStream is) {
                    stream = is;
                }

                @Override
                public void releaseConnection(boolean consumeInputStream) throws IOException {
                    // Apache Client 4 is stupid, You have to get the InputStream and close it if there is an entity
                    // otherwise the connection is never released. There is, of course, no close() method on response
                    // to make this easier.
                    try {
                        // Another stupid thing...TCK is testing a specific exception from stream.close()
                        // so, we let it propagate up.
                        stream.close();
                    } finally {
                        // just in case the input stream was entirely replaced and not wrapped, we need
                        // to close the apache client input stream.
                        StreamUtils.closeSilently(response.getEntity().getContent());
                    }
                    response.close();
                }

                @Override
                public void releaseConnection() throws IOException {
                    releaseConnection(true);
                }
            };
            responseContext.setProperties(((ClientInvocation) clientInvocation).getMutableProperties());
            responseContext.setStatus(statusCode);
            responseContext.setHeaders(extractHeaders(response));
            responseContext.setClientConfiguration(((ClientInvocation) clientInvocation).getClientConfiguration());
            return responseContext;
        } catch (final Exception e) {
            throw new ProcessingException(e);
        }
    }

    private static CaseInsensitiveMap<String> extractHeaders(HttpResponse response) {
        final CaseInsensitiveMap<String> headers = new CaseInsensitiveMap<String>();

        for (Header header : response.getAllHeaders()) {
            headers.add(header.getName(), header.getValue());
        }
        return headers;
    }

    private HttpHost getHost(final HttpUriRequest request) {
        return new HttpHost(request.getURI().getHost(), request.getURI().getPort(), request.getURI().getScheme());
    }

    private HttpUriRequest getUriHttpRequest(final ClientInvocation clientInvocation) {
        final RequestConfig.Builder requestConfigBuilder = RequestConfig.copy(requestConfig);

        if (connectTimeout >= 0) {
            requestConfigBuilder.setConnectTimeout(connectTimeout);
        }
        if (socketTimeout >= 0) {
            requestConfigBuilder.setSocketTimeout(socketTimeout);
        }

        requestConfigBuilder.setRedirectsEnabled(true);

        final HttpEntity entity = getHttpEntity(clientInvocation, bufferingEnabled, bufferSize);

        return RequestBuilder.create(clientInvocation.getMethod())
            .setUri(clientInvocation.getUri())
            .setConfig(requestConfigBuilder.build())
            .setEntity(entity)
            .build();
    }

    private HttpEntity getHttpEntity(
        final ClientInvocation clientInvocation,
        final boolean bufferingEnabled,
        final int bufferSize
    ) {
        final Object entity = clientInvocation.getEntity();

        if (entity == null) {
            return null;
        }

        final AbstractHttpEntity httpEntity = new AbstractHttpEntity() {
            @Override
            public boolean isRepeatable() {
                return false;
            }

            @Override
            public long getContentLength() {
                return -1;
            }

            @Override
            public InputStream getContent() throws IOException, IllegalStateException {
                if (bufferingEnabled) {
                    final ByteArrayOutputStream buffer = new ByteArrayOutputStream(bufferSize);
                    writeTo(buffer);
                    return buffer.toInputStream();
                } else if (entity instanceof InputStream) {
                    return (InputStream) entity;
                } else {
                    return null;
                }
            }

            @Override
            public void writeTo(final OutputStream outputStream) throws IOException {
                clientInvocation.setDelegatingOutputStream(new DelegatingOutputStream(outputStream));
                clientInvocation.writeRequestBody(outputStream);
            }

            @Override
            public boolean isStreaming() {
                return !bufferingEnabled;
            }
        };

        if (bufferingEnabled) {
            try {
                return new BufferedHttpEntity(httpEntity);
            } catch (final IOException e) {
                throw new ProcessingException("ERROR_BUFFERING_ENTITY", e);
            }
        } else {
            return httpEntity;
        }
    }

    private static void writeOutBoundHeaders(
        final ClientRequestHeaders clientRequestHeaders,
        final HttpUriRequest request
    ) {
        for (final Entry<String, List<String>> e : clientRequestHeaders.asMap().entrySet()) {
            StringBuilder builder = new StringBuilder();
            for (String value : e.getValue()) {
                if (builder.length() == 0) {
                    builder.append(value);
                } else {
                    builder.append(',').append(value);
                }
            }
            String finalHeader = builder.toString();
            request.addHeader(e.getKey(), finalHeader);
        }
    }

    private InputStream createBufferedStream(InputStream is) {
        if (responseBufferSize == 0) {
            return is;
        }
        if (responseBufferSize < 0) {
            return new SelfExpandingBufferredInputStream(is, 4 * 8192);
        }
        return new BufferedInputStream(is, responseBufferSize);
    }

    private InputStream getNativeInputStream(final CloseableHttpResponse response) throws IOException {
        final InputStream inputStream;
        LOGGER.debug("{}", this);
        if (response.getEntity() == null) {
            inputStream = new NullInputStream(0);
        } else {
            final InputStream i = response.getEntity().getContent();
            if (i.markSupported()) {
                inputStream = i;
            } else {
                inputStream = createBufferedStream(i);
            }
        }

        return new FilterInputStream(inputStream) {
            @Override
            public void close() throws IOException {
                StreamUtils.closeSilently(response.getEntity().getContent());
                super.close();
                response.close();
            }
        };
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        try {
            httpClient.close();
        } catch (IOException e) {
            LOGGER.error(e);
        }
        clean();
        closed = true;
    }

    private void clean() {
        cookieStore = null;
        requestConfig = null;
        httpClientConnectionManager = null;
        config = null;
        sslContext = null;
        httpClient = null;
    }

    /**
     * Default implementation of a strategy deciding about connection re-use. The default implementation first checks
     * some basics, for example whether the connection is still open or whether the end of the request entity can be
     * determined without closing the connection. If these checks pass, the tokens in the {@code Connection} header will
     * be examined. In the absence of a {@code Connection} header, the non-standard but commonly used
     * {@code Proxy-Connection} header takes it's role. A token {@code close} indicates that the connection cannot be
     * reused. If there is no such token, a token {@code keep-alive} indicates that the connection should be re-used. If
     * neither token is found, or if there are no {@code Connection} headers, the default policy for the HTTP version is
     * applied. Since {@code HTTP/1.1}, connections are re-used by default. Up until {@code HTTP/1.0}, connections are
     * not re-used by default.
     *
     * @since 4.0
     */
    @Contract(threading = ThreadingBehavior.IMMUTABLE)
    public static class VitamConnectionReuseStrategy implements ConnectionReuseStrategy {

        public static final VitamConnectionReuseStrategy INSTANCE = new VitamConnectionReuseStrategy();

        public VitamConnectionReuseStrategy() {}

        // see interface ConnectionReuseStrategy
        @Override
        public boolean keepAlive(final HttpResponse response, final HttpContext context) {
            Args.notNull(response, "HTTP response");
            Args.notNull(context, "HTTP context");

            final HttpRequest request = (HttpRequest) context.getAttribute(HttpCoreContext.HTTP_REQUEST);
            if (request != null) {
                try {
                    final TokenIterator ti = new BasicTokenIterator(request.headerIterator(HttpHeaders.CONNECTION));
                    while (ti.hasNext()) {
                        final String token = ti.nextToken();
                        if (HTTP.CONN_CLOSE.equalsIgnoreCase(token)) {
                            return false;
                        }
                    }
                } catch (final ParseException px) {
                    // invalid connection header. do not re-use
                    return false;
                }
            }

            // Check for a self-terminating entity. If the end of the entity will
            // be indicated by closing the connection, there is no keep-alive.
            final ProtocolVersion ver = response.getStatusLine().getProtocolVersion();
            final Header teh = response.getFirstHeader(HTTP.TRANSFER_ENCODING);
            if (teh != null) {
                if (!HTTP.CHUNK_CODING.equalsIgnoreCase(teh.getValue())) {
                    return false;
                }
            } else {
                if (canResponseHaveBody(request, response)) {
                    final Header[] clhs = response.getHeaders(HTTP.CONTENT_LEN);
                    // Do not reuse if not properly content-length delimited
                    if (clhs.length == 1) {
                        final Header clh = clhs[0];
                        try {
                            final int contentLen = Integer.parseInt(clh.getValue());
                            if (contentLen < 0) {
                                return false;
                            }
                        } catch (final NumberFormatException ex) {
                            return false;
                        }
                    } else {
                        return true;
                    }
                }
            }

            // Check for the "Connection" header. If that is absent, check for
            // the "Proxy-Connection" header. The latter is an unspecified and
            // broken but unfortunately common extension of HTTP.
            HeaderIterator headerIterator = response.headerIterator(HTTP.CONN_DIRECTIVE);
            if (!headerIterator.hasNext()) {
                headerIterator = response.headerIterator("Proxy-Connection");
            }

            // Experimental usage of the "Connection" header in HTTP/1.0 is
            // documented in RFC 2068, section 19.7.1. A token "keep-alive" is
            // used to indicate that the connection should be persistent.
            // Note that the final specification of HTTP/1.1 in RFC 2616 does not
            // include this information. Neither is the "Connection" header
            // mentioned in RFC 1945, which informally describes HTTP/1.0.
            //
            // RFC 2616 specifies "close" as the only connection token with a
            // specific meaning: it disables persistent connections.
            //
            // The "Proxy-Connection" header is not formally specified anywhere,
            // but is commonly used to carry one token, "close" or "keep-alive".
            // The "Connection" header, on the other hand, is defined as a
            // sequence of tokens, where each token is a header name, and the
            // token "close" has the above-mentioned additional meaning.
            //
            // To get through this mess, we treat the "Proxy-Connection" header
            // in exactly the same way as the "Connection" header, but only if
            // the latter is missing. We scan the sequence of tokens for both
            // "close" and "keep-alive". As "close" is specified by RFC 2068,
            // it takes precedence and indicates a non-persistent connection.
            // If there is no "close" but a "keep-alive", we take the hint.

            if (headerIterator.hasNext()) {
                try {
                    final TokenIterator ti = new BasicTokenIterator(headerIterator);
                    boolean keepalive = true;
                    while (ti.hasNext()) {
                        final String token = ti.nextToken();
                        if (HTTP.CONN_CLOSE.equalsIgnoreCase(token)) {
                            return false;
                        } else if (HTTP.CONN_KEEP_ALIVE.equalsIgnoreCase(token)) {
                            // continue the loop, there may be a "close" afterwards
                            keepalive = true;
                        }
                    }
                    return keepalive;
                    // neither "close" nor "keep-alive", use default policy

                } catch (final ParseException px) {
                    // invalid connection header. do not re-use
                    return false;
                }
            }

            // default since HTTP/1.1 is persistent, before it was non-persistent
            return !ver.lessEquals(HttpVersion.HTTP_1_0);
        }

        /**
         * Creates a token iterator from a header iterator. This method can be overridden to replace the implementation
         * of the token iterator.
         *
         * @param hit the header iterator
         * @return the token iterator
         */
        protected TokenIterator createTokenIterator(final HeaderIterator hit) {
            return new BasicTokenIterator(hit);
        }

        private boolean canResponseHaveBody(final HttpRequest request, final HttpResponse response) {
            if (request != null && "HEAD".equalsIgnoreCase(request.getRequestLine().getMethod())) {
                return false;
            }
            final int status = response.getStatusLine().getStatusCode();
            return (
                status >= HttpStatus.SC_ACCEPTED &&
                status != HttpStatus.SC_NO_CONTENT &&
                status != HttpStatus.SC_NOT_MODIFIED &&
                status != HttpStatus.SC_RESET_CONTENT
            );
        }
    }
}
