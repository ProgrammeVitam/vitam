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
package fr.gouv.vitam.common.client2;

import java.io.FileNotFoundException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.apache.connector.VitamClientProperties;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.JerseyInvocation;
import org.glassfish.jersey.client.RequestEntityProcessing;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.MockOrRestClient;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.client.configuration.ClientConfiguration;
import fr.gouv.vitam.common.client2.configuration.ClientConfigurationImpl;
import fr.gouv.vitam.common.client2.configuration.SSLConfiguration;
import fr.gouv.vitam.common.client2.configuration.SecureClientConfiguration;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

/**
 * General VitamClientFactory for non SSL client
 *
 * @param <T> MockOrRestClient class
 *
 */
public abstract class VitamClientFactory<T extends MockOrRestClient> implements VitamClientFactoryInterface<T> {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(VitamClientFactory.class);

    /**
     * Multipart response from Server side
     */
    public static final String MULTIPART_MIXED = "multipart/mixed";
    /**
     * Multipart response from Server side
     */
    public static final MediaType MULTIPART_MIXED_TYPE = new MediaType("multipart", "mixed");

    static final AtomicBoolean INIT_STATIC_CONFIG = new AtomicBoolean(false);
    /**
     * Global configuration for Apache: Pooling connection
     */
    static final PoolingHttpClientConnectionManager POOLING_CONNECTION_MANAGER =
        new PoolingHttpClientConnectionManager(VitamConfiguration.getMaxDelayUnusedConnection(), TimeUnit.MILLISECONDS);
    /**
     * Global configuration for Apache: Pooling connection
     */
    static final PoolingHttpClientConnectionManager POOLING_CONNECTION_MANAGER_NOT_CHUNKED =
        new PoolingHttpClientConnectionManager(VitamConfiguration.getMaxDelayUnusedConnection(), TimeUnit.MILLISECONDS);
    /**
     * Global configuration for Apache: Idle Monitor
     */
    final ExpiredConnectionMonitorThread idleMonitor;

    /**
     * Global configuration
     */
    final ClientConfig config = new ClientConfig();
    /**
     * Global configuration not chunked
     */
    final ClientConfig configNotChunked = new ClientConfig();

    private String serviceUrl;

    private String resourcePath;
    protected ClientConfiguration clientConfiguration;
    private boolean chunkedMode;
    private final Client givenClient;
    private VitamClientType vitamClientType = VitamClientType.MOCK;
    PoolingHttpClientConnectionManager chunkedPoolingManager;
    PoolingHttpClientConnectionManager notChunkedPoolingManager;

    /**
     * Constructor with standard configuration
     *
     * @param configuration The client configuration
     * @param resourcePath the resource path of the server for the client calls
     * @throws UnsupportedOperationException HTTPS not implemented yet
     */
    protected VitamClientFactory(ClientConfiguration configuration, String resourcePath) {
        this(configuration, resourcePath, true, false);
    }

    /**
     * Constructor to allow to enable Multipart support (until all are removed)
     *
     * @param configuration The client configuration
     * @param resourcePath the resource path of the server for the client calls
     * @param suppressHttpCompliance define if client (Jetty Client feature) check if request id HTTP compliant
     * @param multipart allow multipart and disabling chunked mode
     * @throws UnsupportedOperationException HTTPS not implemented yet
     */
    protected VitamClientFactory(ClientConfiguration configuration, String resourcePath,
        boolean suppressHttpCompliance, boolean allowMultipart) {
        internalConfigure();
        initialisation(configuration, resourcePath);
        config.property(ClientProperties.SUPPRESS_HTTP_COMPLIANCE_VALIDATION, suppressHttpCompliance);
        configNotChunked.property(ClientProperties.SUPPRESS_HTTP_COMPLIANCE_VALIDATION, suppressHttpCompliance);
        chunkedMode = !allowMultipart;
        if (allowMultipart) {
            LOGGER.warn("This client is using Multipart therefore not Chunked mode");
        }
        disableChunkMode(configNotChunked);
        givenClient = null;
        idleMonitor = new ExpiredConnectionMonitorThread(this);
        startupMonitor();
    }

    /**
     * ONLY use this constructor in unit test Remove this when JerseyTest will be fully compatible with Jetty
     *
     * @param configuration the client configuration
     * @param resourcePath the resource path of the server for the client calls
     * @param client the HTTP client to use
     * @throws UnsupportedOperationException HTTPS not implemented yet
     */
    protected VitamClientFactory(ClientConfiguration configuration, String resourcePath, Client client) {
        ParametersChecker.checkParameter("Client cannot be null", client);
        internalConfigure();
        initialisation(configuration, resourcePath);
        this.givenClient = client;
        idleMonitor = new ExpiredConnectionMonitorThread(this);
    }

    protected final void initialisation(ClientConfiguration configuration, String resourcePath) {
        if (configuration == null) {
            setVitamClientType(VitamClientType.MOCK);
            clientConfiguration = new ClientConfigurationImpl();
        } else {
            setVitamClientType(VitamClientType.PRODUCTION);
            clientConfiguration = configuration;
            ParametersChecker.checkParameter("Host cannot be null", clientConfiguration.getServerHost());
            ParametersChecker.checkValue("Port has invalid value", clientConfiguration.getServerPort(), 1);
        }
        ParametersChecker.checkParameter("resourcePath cannot be null", resourcePath);
        this.resourcePath = Optional.ofNullable(resourcePath).orElse("/");
        if (this.resourcePath.codePointAt(0) != '/') {
            this.resourcePath = "/" + this.resourcePath;
        }
        serviceUrl = (clientConfiguration.isSecure() ? "https://"
            : "http://") + clientConfiguration.getServerHost() + ":" + clientConfiguration.getServerPort() +
            this.resourcePath;
        if (chunkedPoolingManager != null && chunkedPoolingManager != POOLING_CONNECTION_MANAGER) {
            chunkedPoolingManager.close();
        }
        if (notChunkedPoolingManager != null && notChunkedPoolingManager != POOLING_CONNECTION_MANAGER_NOT_CHUNKED) {
            notChunkedPoolingManager.close();
        }
        if (clientConfiguration.isSecure()) {
            SecureClientConfiguration sclientConfiguration = (SecureClientConfiguration) clientConfiguration;
            SSLConfiguration sslConfiguration = sclientConfiguration.getSslConfiguration();
            ParametersChecker.checkParameter("sslConfiguration is a mandatory parameter", sslConfiguration);
            Registry<ConnectionSocketFactory> registry;
            try {
                registry = sslConfiguration.getRegistry();
            } catch (FileNotFoundException e) {
                LOGGER.error(e);
                throw new IllegalArgumentException("SSLConfiguration issue while reading KeyStore or TrustStore", e);
            }
            PoolingHttpClientConnectionManager pool = new PoolingHttpClientConnectionManager(registry, null, null, null,
                VitamConfiguration.getMaxDelayUnusedConnection(), TimeUnit.MILLISECONDS);
            setupApachePool(pool);
            chunkedPoolingManager = pool;
            pool = new PoolingHttpClientConnectionManager(registry, null, null, null,
                VitamConfiguration.getMaxDelayUnusedConnection(), TimeUnit.MILLISECONDS);
            setupApachePool(pool);
            notChunkedPoolingManager = pool;
        } else {
            chunkedPoolingManager = POOLING_CONNECTION_MANAGER;
            notChunkedPoolingManager = POOLING_CONNECTION_MANAGER_NOT_CHUNKED;
        }
        config.property(ApacheClientProperties.CONNECTION_MANAGER, chunkedPoolingManager);
        configNotChunked.property(ApacheClientProperties.CONNECTION_MANAGER, notChunkedPoolingManager);
    }

    @Override
    public void changeServerPort(int port) {
        try {
            ParametersChecker.checkParameter("Host cannot be null", clientConfiguration.getServerHost());
        } catch (final IllegalArgumentException e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            clientConfiguration.setServerHost(TestVitamClientFactory.LOCALHOST);
        }
        this.initialisation(clientConfiguration.setServerPort(port), getResourcePath());
    }

    @Override
    public VitamClientType getVitamClientType() {
        return vitamClientType;
    }

    @Override
    public VitamClientFactory<T> setVitamClientType(VitamClientType vitamClientType) {
        this.vitamClientType = vitamClientType;
        return this;
    }

    /**
     * To be overridden if necessary (Benchmark Test)
     */
    void internalConfigure() {
        startup();
        internalConfigure(config, true);
        internalConfigure(configNotChunked, false);
    }

    private void internalConfigure(ClientConfig config, boolean chunkedMode) {
        commonConfigure(config);
        commonApacheConfigure(config, chunkedMode);
    }

    /**
     * Specific to handle Junit tests with given Client
     *
     * @param config
     * @return the associated client
     */
    private Client buildClient(ClientConfig config) {
        if (givenClient != null) {
            return givenClient;
        }
        return ClientBuilder.newClient(config);
    }

    /**
     *
     * @return the client chunked mode default configuration
     */
    boolean getChunkedMode() {
        return chunkedMode;
    }

    @Override
    public Client getHttpClient() {
        if (givenClient != null) {
            return givenClient;
        }
        return getHttpClient(chunkedMode);
    }

    @Override
    public Client getHttpClient(boolean useChunkedMode) {
        if (givenClient != null) {
            return givenClient;
        }
        if (useChunkedMode) {
            return buildClient(config);
        } else {
            return buildClient(configNotChunked);
        }
    }

    @Override
    public String getResourcePath() {
        return resourcePath;
    }

    @Override
    public String getServiceUrl() {
        return serviceUrl;
    }

    @Override
    public String toString() {
        return new StringBuilder("VitamFactory: { ")
            .append("ServiceUrl: ").append(serviceUrl)
            .append(", ResourcePath: ").append(resourcePath)
            .append(", ChunkedMode: ").append(chunkedMode)
            .append(", Configuration: { Classes: \"").append(config.getClasses())
            .append("\", Properties: \"").append(config.getProperties())
            .append("\" }")
            .append(" }").toString();
    }

    @Override
    public final ClientConfiguration getClientConfiguration() {
        return clientConfiguration;
    }

    /**
     * To allow specific client to disable ChunkMode until all API remove Multipart
     *
     * @param config
     */
    private final void disableChunkMode(ClientConfig config) {
        config.register(MultiPartFeature.class)
            .property(ClientProperties.CHUNKED_ENCODING_SIZE, 0)
            .property(ClientProperties.REQUEST_ENTITY_PROCESSING, RequestEntityProcessing.BUFFERED);
    }

    @Override
    public final ClientConfig getDefaultConfigCient() {
        return config;
    }

    @Override
    public final ClientConfig getDefaultConfigCient(boolean chunkedMode) {
        if (chunkedMode) {
            return config;
        } else {
            return configNotChunked;
        }
    }

    /**
     * Shutdown the global Connection Manager (cannot be restarted yet)
     */
    public void shutdown() {
        idleMonitor.shutdown();
        idleMonitor.interrupt();
        if (chunkedPoolingManager != null) {
            chunkedPoolingManager.close();
            notChunkedPoolingManager.close();
        }
    }

    static void startup() {
        if (INIT_STATIC_CONFIG.compareAndSet(false, true)) {
            setupApachePool(POOLING_CONNECTION_MANAGER);
            setupApachePool(POOLING_CONNECTION_MANAGER_NOT_CHUNKED);
        }
    }

    private static void setupApachePool(PoolingHttpClientConnectionManager manager) {
        manager.setMaxTotal(VitamConfiguration.getMaxTotalClient());
        manager.setDefaultMaxPerRoute(VitamConfiguration.getMaxClientPerHost());
        manager
            .setValidateAfterInactivity(VitamConfiguration.getDelayValidationAfterInactivity());
    }

    private void startupMonitor() {
        // Apache configuration
        idleMonitor.setDaemon(true);
        idleMonitor.start();
    }

    void commonConfigure(ClientConfig config) {
        // Prevent Warning on misusage of non standard Calls
        Logger.getLogger(JerseyInvocation.class.getName()).setLevel(Level.OFF);
        config.register(JacksonJsonProvider.class)
            .register(JacksonFeature.class)
            // Not supported MultiPartFeature.class
            .property(ClientProperties.CHUNKED_ENCODING_SIZE, VitamConfiguration.getChunkSize())
            .property(ClientProperties.REQUEST_ENTITY_PROCESSING, RequestEntityProcessing.CHUNKED)
            .property(ClientProperties.CONNECT_TIMEOUT, VitamConfiguration.getConnectTimeout())
            .property(ClientProperties.READ_TIMEOUT, VitamConfiguration.getReadTimeout())
            .property(ClientProperties.SUPPRESS_HTTP_COMPLIANCE_VALIDATION, true);
    }

    final void commonApacheConfigure(ClientConfig config, boolean chunkedMode) {
        if (chunkedMode) {
            config.property(ApacheClientProperties.CONNECTION_MANAGER, POOLING_CONNECTION_MANAGER);
        } else {
            config.property(ApacheClientProperties.CONNECTION_MANAGER, POOLING_CONNECTION_MANAGER_NOT_CHUNKED);
        }
        final RequestConfig requestConfig = RequestConfig.custom()
            .setConnectionRequestTimeout(VitamConfiguration.getDelayGetClient()).build();
        config.property(ApacheClientProperties.CONNECTION_MANAGER_SHARED, true)
            .property(VitamClientProperties.DISABLE_AUTOMATIC_RETRIES, true)
            .property(ApacheClientProperties.REQUEST_CONFIG, requestConfig);
        config.connectorProvider(new ApacheConnectorProvider());
    }

    /**
     * Monitor to check Expired Connection (staled). Idle connections are managed directly in the Pool
     */
    private class ExpiredConnectionMonitorThread extends Thread {
        volatile boolean shutdown;
        final VitamClientFactory<?> factory;

        /**
         * Constructor
         */
        ExpiredConnectionMonitorThread(VitamClientFactory<?> factory) {
            this.factory = factory;
        }

        @Override
        public void run() {
            try {
                while (!shutdown) {
                    synchronized (this) {
                        if (factory.chunkedPoolingManager == null) {
                            return;
                        }
                        wait(VitamConfiguration.getIntervalDelayCheckIdle());
                        // Close expired connections
                        factory.chunkedPoolingManager.closeExpiredConnections();
                        factory.notChunkedPoolingManager.closeExpiredConnections();
                    }
                }
            } catch (final InterruptedException ex) {
                SysErrLogger.FAKE_LOGGER.ignoreLog(ex);
                // terminate
            }
        }

        /**
         * Shutdown this Daemon
         */
        public void shutdown() {
            shutdown = true;
            synchronized (this) {
                notifyAll();
            }
        }
    }
}
