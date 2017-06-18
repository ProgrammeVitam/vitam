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

import java.io.FileNotFoundException;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.SocketConfig;
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

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.configuration.ClientConfiguration;
import fr.gouv.vitam.common.client.configuration.ClientConfigurationImpl;
import fr.gouv.vitam.common.client.configuration.SSLConfiguration;
import fr.gouv.vitam.common.client.configuration.SecureClientConfiguration;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutorProvider;

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
    static final AtomicBoolean STATIC_IDLE_MONITOR = new AtomicBoolean(false);

    /**
     * Specific Socket Configuration
     */
    static final SocketConfig SOCKETCONFIG = SocketConfig.custom()
        .setRcvBufSize(0).setSndBufSize(VitamConfiguration.getChunkSize())
        .setSoKeepAlive(true).setSoReuseAddress(true).setTcpNoDelay(true)
        .setSoTimeout(VitamConfiguration.getReadTimeout()).build();

    /**
     * Specific Socket Configuration not chunked
     */
    static final SocketConfig SOCKETCONFIG_NONCHUNKED = SocketConfig.custom()
        .setRcvBufSize(0).setSndBufSize(0)
        .setSoKeepAlive(true).setSoReuseAddress(true).setTcpNoDelay(true)
        .setSoTimeout(VitamConfiguration.getReadTimeout()).build();

    /**
     * Global PoolingHttpClientConnectionManager active list
     */
    static final Queue<PoolingHttpClientConnectionManager> allManagers = new ConcurrentLinkedQueue<>();
    
    static {
        if (INIT_STATIC_CONFIG.compareAndSet(false, true)) {
            setupApachePool(POOLING_CONNECTION_MANAGER);
            POOLING_CONNECTION_MANAGER.setDefaultSocketConfig(SOCKETCONFIG);
            setupApachePool(POOLING_CONNECTION_MANAGER_NOT_CHUNKED);
            POOLING_CONNECTION_MANAGER.setDefaultSocketConfig(SOCKETCONFIG_NONCHUNKED);
            allManagers.add(POOLING_CONNECTION_MANAGER);
            allManagers.add(POOLING_CONNECTION_MANAGER_NOT_CHUNKED);
        }
    }
    
    /**
     * Pool of JerseyClient for chunk mode
     */
    final Queue<Client> clientJerseyPool = new ConcurrentLinkedQueue<>();
    /**
     * Pool JerseyClient for non chunk mode
     */
    final Queue<Client> nonChunkedClientJerseyPool = new ConcurrentLinkedQueue<>();

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
    VitamThreadPoolExecutor vitamThreadPoolExecutor = VitamThreadPoolExecutor.getDefaultExecutor();
    private final RequestConfig requestConfig = RequestConfig.custom()
        .setConnectionRequestTimeout(VitamConfiguration.getDelayGetClient()).build();
    SSLConfiguration sslConfiguration = null;
    private boolean useAuthorizationFilter = true;
    
    /**
     * Constructor with standard configuration
     *
     * @param configuration The client configuration
     * @param resourcePath the resource path of the server for the client calls
     * @throws UnsupportedOperationException HTTPS not implemented yet
     */
    protected VitamClientFactory(ClientConfiguration configuration, String resourcePath) {
        this(configuration, resourcePath, true);
    }

    /**
     * Constructor to allow to enable Multipart support or Chunked mode.<br/>
     * <br/>
     * HACK: to support Storage DriverImpl!
     *
     * @param configuration The client configuration
     * @param resourcePath the resource path of the server for the client calls
     * @param chunkedMode one can managed here if the client is in default chunkedMode or not
     * @throws UnsupportedOperationException HTTPS not implemented yet
     */
    protected VitamClientFactory(ClientConfiguration configuration, String resourcePath,
        boolean chunkedMode) {
        internalConfigure();
        initialisation(configuration, resourcePath);
        config.property(ClientProperties.SUPPRESS_HTTP_COMPLIANCE_VALIDATION, true);
        configNotChunked.property(ClientProperties.SUPPRESS_HTTP_COMPLIANCE_VALIDATION, true);
        this.chunkedMode = chunkedMode;
        disableChunkMode(configNotChunked);
        givenClient = null;
        if (STATIC_IDLE_MONITOR.compareAndSet(false, true)) {
            idleMonitor = new ExpiredConnectionMonitorThread();
            startupMonitor();
        } else {
            idleMonitor = null;
        }
        if (configuration != null) {
            useAuthorizationFilter = !configuration.isSecure();
        }
    }

    /**
     * ONLY use this constructor in unit test. Remove this when JerseyTest will be fully compatible with Jetty
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
        idleMonitor = new ExpiredConnectionMonitorThread();
    }
    
    protected void disableUseAuthorizationFilter() {
        useAuthorizationFilter = false;
    }

    protected void enableUseAuthorizationFilter() {
        useAuthorizationFilter = true;
    }

    boolean useAuthorizationFilter() {
        return useAuthorizationFilter;
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
            allManagers.remove(chunkedPoolingManager);
            chunkedPoolingManager.close();
        }
        if (notChunkedPoolingManager != null && notChunkedPoolingManager != POOLING_CONNECTION_MANAGER_NOT_CHUNKED) {
            allManagers.remove(notChunkedPoolingManager);
            notChunkedPoolingManager.close();
        }
        if (clientConfiguration.isSecure()) {
            final SecureClientConfiguration sclientConfiguration = (SecureClientConfiguration) clientConfiguration;
            sslConfiguration = sclientConfiguration.getSslConfiguration();
            ParametersChecker.checkParameter("sslConfiguration is a mandatory parameter", sslConfiguration);
            Registry<ConnectionSocketFactory> registry;
            try {
                registry = sslConfiguration.getRegistry(sslConfiguration.createSSLContext());
            } catch (FileNotFoundException | VitamException e) {
                LOGGER.error(e);
                throw new IllegalArgumentException("SSLConfiguration issue while reading KeyStore or TrustStore", e);
            }
            PoolingHttpClientConnectionManager pool = new PoolingHttpClientConnectionManager(registry, null, null, null,
                VitamConfiguration.getMaxDelayUnusedConnection(), TimeUnit.MILLISECONDS);
            setupApachePool(pool);
            pool.setDefaultSocketConfig(SOCKETCONFIG);
            chunkedPoolingManager = pool;
            allManagers.add(chunkedPoolingManager);
            pool = new PoolingHttpClientConnectionManager(registry, null, null, null,
                VitamConfiguration.getMaxDelayUnusedConnection(), TimeUnit.MILLISECONDS);
            setupApachePool(pool);
            pool.setDefaultSocketConfig(SOCKETCONFIG_NONCHUNKED);
            notChunkedPoolingManager = pool;
            allManagers.add(notChunkedPoolingManager);
        } else {
            chunkedPoolingManager = POOLING_CONNECTION_MANAGER;
            notChunkedPoolingManager = POOLING_CONNECTION_MANAGER_NOT_CHUNKED;
        }
        config.property(ApacheClientProperties.CONNECTION_MANAGER, chunkedPoolingManager);
        configNotChunked.property(ApacheClientProperties.CONNECTION_MANAGER, notChunkedPoolingManager);
    }

    @Override
    public void changeResourcePath(String resourcePath) {
        initialisation(clientConfiguration, resourcePath);
    }

    @Override
    public void changeServerPort(int port) {
        try {
            ParametersChecker.checkParameter("Host cannot be null", clientConfiguration.getServerHost());
        } catch (final IllegalArgumentException e) {
            LOGGER.debug(e);
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

    private final Client getFromCache(boolean useChunkedMode) {
        Client client = null;
        if (useChunkedMode) {
            client = clientJerseyPool.poll();
        } else {
            client = nonChunkedClientJerseyPool.poll();
        }
        return client;
    }

    private final synchronized void addToCache(Client client, boolean chunk) {
        if (chunk) {
            if (clientJerseyPool.size() >= VitamConfiguration.getMaxClientPerHost()) {
                // Remove oldest client
                Client client2 = clientJerseyPool.poll();
                client2.close();
            }
            clientJerseyPool.add(client);
        } else {
            if (nonChunkedClientJerseyPool.size() >= VitamConfiguration.getMaxClientPerHost()) {
                // Remove oldest client
                Client client2 = nonChunkedClientJerseyPool.poll();
                client2.close();
            }
            nonChunkedClientJerseyPool.add(client);
        }
    }

    /**
     * Specific to handle Junit tests with given Client
     *
     * @param config
     * @param useChunkedMode
     * @return the associated client
     */
    private Client buildClient(ClientConfig config, boolean useChunkedMode) {
        if (givenClient != null) {
            return givenClient;
        }
        synchronized (this) {
            if (useChunkedMode) {
                if (clientJerseyPool.isEmpty()) {
                    return buildClientWithFilter(config);
                }
                return getFromCache(useChunkedMode);
            } else {
                if (nonChunkedClientJerseyPool.isEmpty()) {
                    return buildClientWithFilter(config);
                }
                return getFromCache(useChunkedMode);
            }
        }
    }

    private Client buildClientWithFilter(ClientConfig config) {
        Client client = ClientBuilder.newClient(config);
        // TODO: Find a better check (a specific one, instead of inferring the context from another constraint ?);
        if (useAuthorizationFilter()) {
            client.register(HeaderIdClientFilter.class);
        }
        return client;
    }

    @Override
    public void resume(Client client, boolean chunk) {
        if (client == givenClient) {
            return;
        }
        addToCache(client, chunk);
    }

    /**
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
            return buildClient(config, useChunkedMode);
        } else {
            return buildClient(configNotChunked, useChunkedMode);
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
        config.property(ClientProperties.CHUNKED_ENCODING_SIZE, 0)
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
    @Override
    public synchronized void shutdown() {
        if (idleMonitor != null && !STATIC_IDLE_MONITOR.get()) {
            idleMonitor.shutdown();
            idleMonitor.interrupt();
        }
        for (Client client : clientJerseyPool) {
            client.close();
        }
        clientJerseyPool.clear();
        for (Client client : nonChunkedClientJerseyPool) {
            client.close();
        }
        nonChunkedClientJerseyPool.clear();
        if (chunkedPoolingManager != null && chunkedPoolingManager != POOLING_CONNECTION_MANAGER) {
            allManagers.remove(chunkedPoolingManager);
            chunkedPoolingManager.close();
        }
        if (notChunkedPoolingManager != null && notChunkedPoolingManager != POOLING_CONNECTION_MANAGER_NOT_CHUNKED) {
            allManagers.remove(notChunkedPoolingManager);
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
        manager.setValidateAfterInactivity(VitamConfiguration.getDelayValidationAfterInactivity());
    }

    private void startupMonitor() {
        // Apache configuration
        if (idleMonitor != null) {
            idleMonitor.setDaemon(true);
            idleMonitor.start();
        }
    }

    void commonConfigure(ClientConfig config) {
        // Prevent Warning on misusage of non standard Calls
        Logger.getLogger(JerseyInvocation.class.getName()).setLevel(Level.OFF);
        config.register(JacksonJsonProvider.class)
            .register(JacksonFeature.class)
            .register(new VitamThreadPoolExecutorProvider("Vitam"))
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
     * @return the VitamThreadPoolExecutor used by the server
     */
    public VitamThreadPoolExecutor getVitamThreadPoolExecutor() {
        return vitamThreadPoolExecutor;
    }

    /**
     * Monitor to check Expired Connection (staled). Idle connections are managed directly in the Pool
     */
    private static class ExpiredConnectionMonitorThread extends Thread {
        volatile boolean shutdown;

        /**
         * Constructor
         */
        ExpiredConnectionMonitorThread() {
        }

        @Override
        public void run() {
            try {
                while (!shutdown) {
                    synchronized (this) {
                        wait(VitamConfiguration.getIntervalDelayCheckIdle());
                        // Close expired connections
                        for (PoolingHttpClientConnectionManager poolingHttpClientConnectionManager : allManagers) {
                            poolingHttpClientConnectionManager.closeExpiredConnections();
                            poolingHttpClientConnectionManager.closeIdleConnections(
                                VitamConfiguration.getDelayValidationAfterInactivity(), TimeUnit.MILLISECONDS);
                        }
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
