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
package fr.gouv.vitam.common.serverv2;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.MockOrRestClient;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.security.filter.AuthorizationFilter;
import fr.gouv.vitam.common.server.VitamServer;
import fr.gouv.vitam.common.server.VitamServerFactory;
import fr.gouv.vitam.common.server.application.resources.AdminStatusResource;
import fr.gouv.vitam.common.tenant.filter.TenantFilter;
import fr.gouv.vitam.common.xsrf.filter.XSRFFilter;
import org.apache.shiro.util.Assert;
import org.apache.shiro.web.env.EnvironmentLoaderListener;
import org.apache.shiro.web.servlet.ShiroFilter;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;

import javax.net.ServerSocketFactory;
import javax.servlet.DispatcherType;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VitamServerTestRunner { // NOSONAR

    public static final String LOCALHOST = "localhost";
    private final VitamServer server;
    private final VitamClientFactoryInterface<?> factory;
    private final Class<? extends Application> application;
    private final Class<? extends Application> adminAapplication;

    public static final int MIN_PORT = 11112;
    private static final int MAX_PORT = 65535;

    private final int businessPort;
    private final int adminPort;

    public VitamServerTestRunner(
        Class<? extends Application> application,
        Class<? extends Application> adminApplication
    ) { // NOSONAR
        this(application, adminApplication, false);
    }

    public VitamServerTestRunner(
        Class<? extends Application> application,
        Class<? extends Application> adminApplication,
        boolean hasAuthorizationFilter
    ) { // NOSONAR
        this(application, adminApplication, false, hasAuthorizationFilter);
    }

    public VitamServerTestRunner(
        Class<? extends Application> application,
        Class<? extends Application> adminApplication,
        boolean hasTenantFilter,
        boolean hasAuthorizationFilter
    ) { // NOSONAR
        this(application, adminApplication, hasTenantFilter, hasAuthorizationFilter, false);
    }

    public VitamServerTestRunner(
        Class<? extends Application> application,
        Class<? extends Application> adminApplication,
        boolean hasTenantFilter,
        boolean hasAuthorizationFilter,
        boolean hasSession
    ) { // NOSONAR
        this(application, adminApplication, hasTenantFilter, hasAuthorizationFilter, hasSession, false);
    }

    public VitamServerTestRunner(
        Class<? extends Application> application,
        Class<? extends Application> adminApplication,
        boolean hasTenantFilter,
        boolean hasAuthorizationFilter,
        boolean hasSession,
        boolean hasShiroFilter
    ) { // NOSONAR
        this(application, adminApplication, hasTenantFilter, hasAuthorizationFilter, hasSession, hasShiroFilter, false);
    }

    public VitamServerTestRunner(
        Class<? extends Application> application,
        Class<? extends Application> adminApplication,
        boolean hasTenantFilter,
        boolean hasAuthorizationFilter,
        boolean hasSession,
        boolean hasShiroFilter,
        boolean hasXsrFilter
    ) { // NOSONAR
        this(
            application,
            adminApplication,
            null,
            null,
            hasTenantFilter,
            hasAuthorizationFilter,
            hasSession,
            hasShiroFilter,
            hasXsrFilter
        );
    }

    public VitamServerTestRunner(
        Class<? extends Application> application,
        Class<? extends Application> adminApplication,
        VitamClientFactoryInterface<?> factory
    ) { // NOSONAR
        this(application, adminApplication, factory, false);
    }

    public VitamServerTestRunner(
        Class<? extends Application> application,
        Class<? extends Application> adminApplication,
        VitamClientFactoryInterface<?> factory,
        boolean hasAuthorizationFilter
    ) { // NOSONAR
        this(application, adminApplication, factory, false, hasAuthorizationFilter);
    }

    public VitamServerTestRunner(
        Class<? extends Application> application,
        Class<? extends Application> adminApplication,
        VitamClientFactoryInterface<?> factory,
        boolean hasTenantFilter,
        boolean hasAuthorizationFilter
    ) { // NOSONAR
        this(application, adminApplication, factory, hasTenantFilter, hasAuthorizationFilter, false);
    }

    public VitamServerTestRunner(
        Class<? extends Application> application,
        Class<? extends Application> adminApplication,
        VitamClientFactoryInterface<?> factory,
        boolean hasTenantFilter,
        boolean hasAuthorizationFilter,
        boolean hasSession
    ) { // NOSONAR
        this(
            application,
            adminApplication,
            null,
            factory,
            hasTenantFilter,
            hasAuthorizationFilter,
            hasSession,
            false,
            false
        );
    }

    public VitamServerTestRunner(Class<? extends Application> application) { // NOSONAR
        this(application, false);
    }

    public VitamServerTestRunner(Class<? extends Application> application, boolean hasAuthorizationFilter) { // NOSONAR
        this(application, false, hasAuthorizationFilter);
    }

    public VitamServerTestRunner(
        Class<? extends Application> application,
        boolean hasTenantFilter,
        boolean hasAuthorizationFilter
    ) { // NOSONAR
        this(application, hasTenantFilter, hasAuthorizationFilter, false);
    }

    public VitamServerTestRunner(
        Class<? extends Application> application,
        boolean hasTenantFilter,
        boolean hasAuthorizationFilter,
        boolean hasSession
    ) { // NOSONAR
        this(application, hasTenantFilter, hasAuthorizationFilter, hasSession, false);
    }

    public VitamServerTestRunner(
        Class<? extends Application> application,
        boolean hasTenantFilter,
        boolean hasAuthorizationFilter,
        boolean hasSession,
        boolean hasShiroFilter
    ) { // NOSONAR
        this(application, hasTenantFilter, hasAuthorizationFilter, hasSession, hasShiroFilter, false);
    }

    public VitamServerTestRunner(
        Class<? extends Application> application,
        boolean hasTenantFilter,
        boolean hasAuthorizationFilter,
        boolean hasSession,
        boolean hasShiroFilter,
        boolean hasXsrFilter
    ) { // NOSONAR
        this(
            application,
            (VitamClientFactoryInterface) null,
            hasTenantFilter,
            hasAuthorizationFilter,
            hasSession,
            hasShiroFilter,
            hasXsrFilter
        );
    }

    public VitamServerTestRunner(Class<? extends Application> application, VitamClientFactoryInterface<?> factory) { // NOSONAR
        this(application, factory, false);
    }

    public VitamServerTestRunner(
        Class<? extends Application> application,
        VitamClientFactoryInterface<?> factory,
        boolean hasAuthorizationFilter
    ) { // NOSONAR
        this(application, factory, false, hasAuthorizationFilter);
    }

    public VitamServerTestRunner(
        Class<? extends Application> application,
        VitamClientFactoryInterface<?> factory,
        boolean hasTenantFilter,
        boolean hasAuthorizationFilter
    ) { // NOSONAR
        this(application, factory, hasTenantFilter, hasAuthorizationFilter, false);
    }

    public VitamServerTestRunner(
        Class<? extends Application> application,
        VitamClientFactoryInterface<?> factory,
        boolean hasTenantFilter,
        boolean hasAuthorizationFilter,
        boolean hasSession
    ) { // NOSONAR
        this(application, factory, hasTenantFilter, hasAuthorizationFilter, hasSession, false, false);
    }

    public VitamServerTestRunner(
        Class<? extends Application> application,
        VitamClientFactoryInterface<?> factory,
        boolean hasTenantFilter,
        boolean hasAuthorizationFilter,
        boolean hasSession,
        boolean hasShiroFilter,
        boolean hasXsrFilter
    ) { // NOSONAR
        this(
            application,
            application,
            null,
            factory,
            hasTenantFilter,
            hasAuthorizationFilter,
            hasSession,
            hasShiroFilter,
            hasXsrFilter
        );
    }

    public VitamServerTestRunner(
        Class<? extends Application> application,
        Class<? extends Application> adminApplication,
        SslConfig configuration,
        VitamClientFactoryInterface<?> factory,
        boolean hasTenantFilter,
        boolean hasAuthorizationFilter,
        boolean hasSession,
        boolean hasShiroFilter,
        boolean hasXsrFilter
    ) { // NOSONAR
        this.application = application;
        this.adminAapplication = adminApplication;
        this.factory = factory;
        businessPort = getAvailablePort();
        adminPort = getAvailablePort();

        if (null != configuration) {
            server = VitamServerFactory.newVitamServerWithoutConnector(businessPort);
            createSslConnector(configuration);
        } else {
            server = VitamServerFactory.newVitamServer(businessPort);
            ServerConnector connector = (ServerConnector) server.getServer().getConnectors()[0];
            connector.setName("business");
        }

        prepare(hasTenantFilter, hasAuthorizationFilter, hasSession, hasShiroFilter, hasXsrFilter);

        if (null != factory) {
            //Force host to localhost: collision between tests
            if (null != factory.getClientConfiguration()) {
                factory.getClientConfiguration().setServerHost(LOCALHOST);
            }

            factory.changeServerPort(businessPort);
        }

        VitamConfiguration.setHttpClientWaitingTime(1);
        VitamConfiguration.setHttpClientRandomWaitingSleep(1);
        VitamConfiguration.setHttpClientFirstAttemptWaitingTime(1);
        VitamConfiguration.setHttpClientRetry(3);
    }

    /**
     * This is the same configuration as jetty xml file
     *
     * @param configuration
     */
    private void createSslConnector(SslConfig configuration) { // NOSONAR
        HttpConfiguration https = new HttpConfiguration();
        https.addCustomizer(new SecureRequestCustomizer());

        SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
        sslContextFactory.setKeyStorePath(configuration.getKeyStorePath());
        sslContextFactory.setKeyStorePassword(configuration.getKeyStorePassword());
        sslContextFactory.setKeyManagerPassword(configuration.getKeyStorePassword());
        sslContextFactory.setTrustStorePath(configuration.getTrustStorePath());
        sslContextFactory.setTrustStorePassword(configuration.getTrustStorePassword());
        sslContextFactory.setTrustStoreType("JKS");
        sslContextFactory.setNeedClientAuth(false);
        sslContextFactory.setWantClientAuth(true);
        sslContextFactory.setIncludeCipherSuites("TLS_ECDHE.*", "TLS_DHE_RSA.*");
        sslContextFactory.setIncludeProtocols("TLSv1.2");
        sslContextFactory.setExcludeCipherSuites(".*NULL.*", ".*RC4.*", ".*MD5.*", ".*DES.*", ".*DSS.");
        sslContextFactory.setUseCipherSuitesOrder(true);

        sslContextFactory.setRenegotiationAllowed(true);

        SslConnectionFactory sslConnectionFactory = new SslConnectionFactory(sslContextFactory, "http/1.1");
        HttpConnectionFactory httpConnectionFactory = new HttpConnectionFactory(https);
        ServerConnector sslConnector = new ServerConnector(
            server.getServer(),
            sslConnectionFactory,
            httpConnectionFactory
        );
        sslConnector.setPort(businessPort);
        sslConnector.setName("business");
        sslConnector.setHost("localhost");
        sslConnector.setIdleTimeout(30000);
        server.getServer().addConnector(sslConnector);
    }

    private void prepare(
        boolean hasTenantFilter,
        boolean hasAuthorizationFilter,
        boolean hasSession,
        boolean hasShiroFilter,
        boolean hasXsrFilter
    ) { // NOSONAR
        try {
            ContextHandlerCollection applicationHandlers = new ContextHandlerCollection();
            final ServletHolder servletHolder = new ServletHolder(new HttpServletDispatcher());
            servletHolder.setInitParameter("javax.ws.rs.Application", application.getName());
            final ServletContextHandler context = new ServletContextHandler(
                hasSession ? ServletContextHandler.SESSIONS : ServletContextHandler.NO_SESSIONS
            );

            context.addServlet(servletHolder, "/*");
            context.setContextPath("/");
            context.setVirtualHosts(new String[] { "@business" });

            // Authorization Filter
            // If you want to enable autorization filter
            if (hasAuthorizationFilter) {
                VitamConfiguration.setSecret("vitamsecret");
                context.addFilter(
                    AuthorizationFilter.class,
                    "/*",
                    EnumSet.of(
                        DispatcherType.INCLUDE,
                        DispatcherType.REQUEST,
                        DispatcherType.FORWARD,
                        DispatcherType.ERROR,
                        DispatcherType.ASYNC
                    )
                );
            }

            if (hasXsrFilter) {
                context.addFilter(
                    XSRFFilter.class,
                    "/*",
                    EnumSet.of(
                        DispatcherType.INCLUDE,
                        DispatcherType.REQUEST,
                        DispatcherType.FORWARD,
                        DispatcherType.ERROR,
                        DispatcherType.ASYNC
                    )
                );
            }

            if (hasShiroFilter) {
                addShiroFilter(context);
            }
            if (hasTenantFilter) {
                addTenantFilter(context, VitamConfiguration.getTenants());
            }
            StatisticsHandler stats = new StatisticsHandler();
            stats.setHandler(context);

            applicationHandlers.addHandler(stats);
            // Configure admin connector

            ServerConnector admin = new ServerConnector(server.getServer()); // NOSONAR
            admin.setName("admin");
            admin.setHost("localhost");
            admin.setPort(adminPort);
            server.getServer().addConnector(admin);

            final ServletHolder servletHolderAdmin = new ServletHolder(new HttpServletDispatcher());
            servletHolderAdmin.setInitParameter("javax.ws.rs.Application", adminAapplication.getName());

            final ServletContextHandler contextAdmin = new ServletContextHandler(
                hasSession ? ServletContextHandler.SESSIONS : ServletContextHandler.NO_SESSIONS
            );
            contextAdmin.addServlet(servletHolderAdmin, "/*");

            contextAdmin.setVirtualHosts(new String[] { "@admin" });

            StatisticsHandler statsAdmin = new StatisticsHandler();
            statsAdmin.setHandler(contextAdmin);

            applicationHandlers.addHandler(statsAdmin);

            server.configure(applicationHandlers);
        } catch (Exception e) {
            throw new RuntimeException(e); // NOSONAR
        }
    }

    @ApplicationPath("/")
    public static class AdminApp extends Application {

        public AdminApp() {}

        @Override
        public Set<Object> getSingletons() {
            return Sets.newHashSet(new AdminStatusResource());
        }
    }

    protected void after() throws Exception { // NOSONAR
        try {
            server.stop();
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.syserr("", e);
        }
    }

    public MockOrRestClient getClient() { // NOSONAR
        return factory == null ? null : factory.getClient();
    }

    public void runAfter() throws Exception { // NOSONAR
        after();
        releasePort();
        VitamClientFactory.resetConnections();
        try {
            if (null != factory) {
                factory.shutdown();
            }
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }
    }

    public void stop() throws Exception { // NOSONAR
        try {
            server.stop();
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.syserr("", e);
        }
    }

    public void start() throws Exception { // NOSONAR
        server.start();
    }

    private void addShiroFilter(ServletContextHandler context) throws VitamApplicationServerException { // NOSONAR
        File shiroFile;
        try {
            shiroFile = PropertiesUtils.findFile("shiro.ini");
        } catch (final FileNotFoundException e) {
            throw new VitamApplicationServerException(e.getMessage());
        }
        context.setInitParameter("shiroConfigLocations", "file:" + shiroFile.getAbsolutePath());
        context.addEventListener(new EnvironmentLoaderListener());
        context.addFilter(
            ShiroFilter.class,
            "/*",
            EnumSet.of(
                DispatcherType.INCLUDE,
                DispatcherType.REQUEST,
                DispatcherType.FORWARD,
                DispatcherType.ERROR,
                DispatcherType.ASYNC
            )
        );
    }

    private void addTenantFilter(ServletContextHandler context, List<Integer> tenantList)
        throws VitamApplicationServerException { // NOSONAR
        // Tenant Filter
        try {
            JsonNode node = JsonHandler.toJsonNode(tenantList);
            context.setInitParameter(GlobalDataRest.TENANT_LIST, JsonHandler.unprettyPrint(node));
            context.addFilter(
                TenantFilter.class,
                "/*",
                EnumSet.of(
                    DispatcherType.INCLUDE,
                    DispatcherType.REQUEST,
                    DispatcherType.FORWARD,
                    DispatcherType.ERROR,
                    DispatcherType.ASYNC
                )
            );
        } catch (InvalidParseOperationException e) {
            throw new VitamApplicationServerException(e.getMessage());
        }
    }

    private final Set<Integer> portAlreadyUsed = new HashSet<>();

    private final synchronized int getAvailablePort() {
        do {
            final Integer port = getPort();
            if (!portAlreadyUsed.contains(port)) {
                portAlreadyUsed.add(port);
                return port.intValue();
            }
            try {
                Thread.sleep(10); // NOSONAR
            } catch (final InterruptedException e) { // NOSONAR
                SysErrLogger.FAKE_LOGGER.syserr("", e);
            }
        } while (true);
    }

    private final int getPort() {
        return SocketType.TCP.findAvailablePort(MIN_PORT, MAX_PORT);
    }

    public final synchronized void releasePort() {
        portAlreadyUsed.remove(businessPort);
        portAlreadyUsed.remove(adminPort);
    }

    public int getBusinessPort() {
        return businessPort;
    }

    public int getAdminPort() {
        return adminPort;
    }

    private static final Set<Integer> usedPort = Sets.newConcurrentHashSet();

    /**
     * Copied from Spring SocketUtils
     */
    private enum SocketType {
        TCP {
            @Override
            protected boolean isPortAvailable(int port) {
                if (usedPort.contains(port)) {
                    return false;
                }

                try {
                    ServerSocket serverSocket = ServerSocketFactory.getDefault()
                        .createServerSocket(port, 1, InetAddress.getByName("localhost"));
                    serverSocket.close();
                    return true;
                } catch (Exception ex) {
                    return false;
                }
            }
        },

        UDP {
            @Override
            protected boolean isPortAvailable(int port) {
                if (usedPort.contains(port)) {
                    return false;
                }

                try {
                    DatagramSocket socket = new DatagramSocket(port, InetAddress.getByName("localhost"));
                    socket.close();
                    return true;
                } catch (Exception ex) {
                    return false;
                }
            }
        };

        /**
         * Determine if the specified port for this {@code SocketType} is
         * currently available on {@code localhost}.
         */
        protected abstract boolean isPortAvailable(int port);

        int findAvailablePort(int minPort, int maxPort) {
            Assert.isTrue(minPort > 0, "'minPort' must be greater than 0");
            Assert.isTrue(maxPort >= minPort, "'maxPort' must be greater than or equal to 'minPort'");
            Assert.isTrue(maxPort <= MAX_PORT, "'maxPort' must be less than or equal to " + MAX_PORT);

            Integer candidatePort = null;
            for (int port = minPort; port <= maxPort; port++) {
                if (!isPortAvailable(port)) {
                    continue;
                }

                usedPort.add(port);

                candidatePort = port;

                break;
            }

            if (candidatePort == null) {
                throw new IllegalStateException(
                    String.format(
                        "Could not find an available %s port in the range [%d, %d] after %d attempts",
                        name(),
                        minPort,
                        maxPort,
                        usedPort.size()
                    )
                );
            }

            return candidatePort;
        }
    }
}
