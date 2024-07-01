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
import com.google.common.base.Strings;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.VitamConfigurationParameters;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.security.filter.AuthorizationFilter;
import fr.gouv.vitam.common.server.VitamServer;
import fr.gouv.vitam.common.server.VitamServerFactory;
import fr.gouv.vitam.common.server.application.configuration.VitamApplicationConfiguration;
import fr.gouv.vitam.common.tenant.filter.TenantFilter;
import fr.gouv.vitam.common.xsrf.filter.XSRFFilter;
import org.apache.shiro.web.env.EnvironmentLoaderListener;
import org.apache.shiro.web.servlet.ShiroFilter;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;

import javax.servlet.DispatcherType;
import javax.servlet.ServletContextListener;
import javax.ws.rs.core.Application;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.EnumSet;
import java.util.List;

import static fr.gouv.vitam.common.serverv2.application.ApplicationParameter.CONFIGURATION_FILE_APPLICATION;

/**
 * launch vitam server
 */
public class VitamStarter {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(VitamStarter.class);

    private static final String VITAM_CONF_FILE_NAME = "vitam.conf";
    private static final String SHIRO_FILE = "shiro.ini";

    private final String role = ServerIdentity.getInstance().getRole();

    private VitamServer vitamServer;

    private final Class<? extends Application> businessApplication;
    private final Class<? extends Application> adminApplication;
    private final Class<? extends VitamApplicationConfiguration> configurationType;
    private final List<ServletContextListener> customListeners;
    private final boolean deployStaticResources;

    /**
     * Constructor
     *
     * @param configurationType configuration type
     * @param configurationFile configuration file
     * @param businessApplication business application
     * @param adminApplication admin application
     */
    public VitamStarter(
        Class<? extends VitamApplicationConfiguration> configurationType,
        String configurationFile,
        Class<? extends Application> businessApplication,
        Class<? extends Application> adminApplication
    ) {
        this(configurationType, configurationFile, businessApplication, adminApplication, null, false);
    }

    /**
     * Constructor
     *
     * @param configurationType configuration type
     * @param configurationFile configuration file
     * @param businessApplication business application
     * @param adminApplication admin application
     * @param customListeners list of custom listeners
     */
    public VitamStarter(
        Class<? extends VitamApplicationConfiguration> configurationType,
        String configurationFile,
        Class<? extends Application> businessApplication,
        Class<? extends Application> adminApplication,
        List<ServletContextListener> customListeners,
        boolean deployStaticResources
    ) {
        this.businessApplication = businessApplication;
        this.adminApplication = adminApplication;
        this.configurationType = configurationType;
        this.customListeners = customListeners;
        this.deployStaticResources = deployStaticResources;
        configure(configurationFile);
    }

    protected final void configure(String configurationFile) {
        try (final InputStream yamlIS = PropertiesUtils.getConfigAsStream(configurationFile)) {
            final VitamApplicationConfiguration buildConfiguration = PropertiesUtils.readYaml(
                yamlIS,
                getConfigurationType()
            );
            configure(buildConfiguration, configurationFile);
        } catch (final IOException e) {
            LOGGER.error(e);
            throw new IllegalStateException("Cannot start the " + role + " Application Server", e);
        }
    }

    /**
     * Used in Junit test
     *
     * @param configuration
     * @param configurationFile
     * @throws IllegalStateException
     */
    private final void configure(VitamApplicationConfiguration configuration, String configurationFile) {
        try {
            configureVitamParameters();

            ContextHandlerCollection applicationHandlers = new ContextHandlerCollection();

            final HandlerList handlerList = new HandlerList();

            if (deployStaticResources) {
                URL staticResourcesUrl = this.getClass().getClassLoader().getResource("static");
                if (staticResourcesUrl != null) {
                    final ResourceHandler staticContentHandler = new ResourceHandler();
                    staticContentHandler.setDirectoriesListed(true);
                    staticContentHandler.setDirAllowed(true);
                    staticContentHandler.setWelcomeFiles(new String[] { "index.html" });
                    staticContentHandler.setResourceBase(staticResourcesUrl.toString());
                    final ContextHandler staticContext = new ContextHandler(configuration.getBaseUri());
                    staticContext.setHandler(staticContentHandler);
                    handlerList.addHandler(staticContext);
                } else {
                    // Static resources of ihm-demo & ihm-recette may be missing in development mode
                    LOGGER.warn("Missing static resources");
                }
            }

            handlerList.addHandler(buildApplicationHandler(configurationFile, configuration));

            if (deployStaticResources) {
                handlerList.addHandler(new DefaultHandler());
            }

            applicationHandlers.addHandler(handlerList);
            applicationHandlers.addHandler(buildAdminHandler(configurationFile));

            final String jettyConfig = configuration.getJettyConfig();

            LOGGER.info(role + " starts with jetty config");
            vitamServer = VitamServerFactory.newVitamServerByJettyConf(jettyConfig);
            vitamServer.configure(applicationHandlers);
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
            throw new IllegalStateException(String.format("Cannot start the %s application server", role), e);
        }
    }

    /**
     * Allow override Vitam parameters
     */
    protected void configureVitamParameters() {
        try (final InputStream yamlIS = PropertiesUtils.getConfigAsStream(VITAM_CONF_FILE_NAME)) {
            final VitamConfigurationParameters vitamConfigurationParameters = PropertiesUtils.readYaml(
                yamlIS,
                VitamConfigurationParameters.class
            );

            VitamConfiguration.importConfigurationParameters(vitamConfigurationParameters);
            VitamConfiguration.setSecret(vitamConfigurationParameters.getSecret());
            VitamConfiguration.setFilterActivation(vitamConfigurationParameters.isFilterActivation());
            VitamConfiguration.setEnvironmentName(vitamConfigurationParameters.getEnvironmentName());
        } catch (final IOException e) {
            LOGGER.error(e);
            throw new IllegalStateException("Cannot start the " + role + " Application Server", e);
        }
    }

    /**
     * Get configuration Type
     *
     * @return configuration type
     */
    public final Class<? extends VitamApplicationConfiguration> getConfigurationType() {
        return configurationType;
    }

    protected Handler buildApplicationHandler(String configurationFile, VitamApplicationConfiguration configuration)
        throws VitamApplicationServerException {
        final ServletHolder servletHolder = new ServletHolder(new HttpServletDispatcher());

        servletHolder.setInitParameter("javax.ws.rs.Application", businessApplication.getName());
        servletHolder.setInitParameter(CONFIGURATION_FILE_APPLICATION, configurationFile);

        final ServletContextHandler context = new ServletContextHandler(
            configuration.isEnableSession() ? ServletContextHandler.SESSIONS : ServletContextHandler.NO_SESSIONS
        );

        context.addServlet(servletHolder, "/*");

        if (!Strings.isNullOrEmpty(configuration.getBaseUrl())) {
            context.setContextPath(configuration.getBaseUrl());
        }
        // Authorization Filter
        if (VitamConfiguration.isFilterActivation() && !Strings.isNullOrEmpty(VitamConfiguration.getSecret())) {
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

        context.setVirtualHosts(new String[] { "@business" });

        if (configuration.isEnableXsrFilter()) {
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

        if (configuration.isAuthentication()) {
            addShiroFilter(context);
        }
        if (configuration.isTenantFilter()) {
            addTenantFilter(context, VitamConfiguration.getTenants());
        }
        StatisticsHandler stats = new StatisticsHandler();
        stats.setHandler(context);
        return stats;
    }

    protected Handler buildAdminHandler(String configurationFile) {
        final ServletHolder servletHolder = new ServletHolder(new HttpServletDispatcher());
        servletHolder.setInitParameter("javax.ws.rs.Application", adminApplication.getName());
        servletHolder.setInitParameter(CONFIGURATION_FILE_APPLICATION, configurationFile);

        final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        context.addServlet(servletHolder, "/*");

        context.setVirtualHosts(new String[] { "@admin" });

        // no shiro, only internal network
        if (customListeners != null && !customListeners.isEmpty()) {
            customListeners.forEach(listener -> {
                context.addEventListener(listener);
            });
        }
        StatisticsHandler stats = new StatisticsHandler();
        stats.setHandler(context);
        return stats;
    }

    private void addShiroFilter(ServletContextHandler context) throws VitamApplicationServerException {
        File shiroFile = null;
        try {
            shiroFile = PropertiesUtils.findFile(SHIRO_FILE);
        } catch (final FileNotFoundException e) {
            LOGGER.error(e.getMessage(), e);
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
        throws VitamApplicationServerException {
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
            LOGGER.error(e.getMessage(), e);
            throw new VitamApplicationServerException(e.getMessage());
        }
    }

    /**
     * Run method, start and join the server
     *
     * @throws VitamApplicationServerException
     */
    public final void run() throws VitamApplicationServerException {
        if (vitamServer != null && !vitamServer.isStarted()) {
            vitamServer.startAndJoin();
        } else if (vitamServer == null) {
            throw new VitamApplicationServerException("VitamServer is not ready to be started");
        }
    }

    /**
     * For Junit tests, starts only, not join
     *
     * @throws VitamApplicationServerException in case the server could not be started
     */
    public void start() throws VitamApplicationServerException {
        try {
            vitamServer.start();
        } catch (final Exception exc) {
            throw new VitamApplicationServerException("A problem occurred while attempting to start the server", exc);
        }
    }

    /**
     * For Junit tests, stops the server
     *
     * @throws VitamApplicationServerException
     */
    public void stop() throws VitamApplicationServerException {
        try {
            vitamServer.stop();
        } catch (final Exception exc) {
            throw new VitamApplicationServerException("A problem occurred while attempting to stop the server", exc);
        }
    }

    /**
     * Check if server is started
     *
     * @return true if started
     */
    public boolean isStarted() {
        return vitamServer.isStarted();
    }

    /**
     * Get the vitam server
     *
     * @return the vitam server
     */
    public VitamServer getVitamServer() {
        return vitamServer;
    }

    /**
     * Check if server is stopped
     *
     * @return true if stopped
     */
    public boolean isStopped() {
        return vitamServer.isStopped();
    }
}
