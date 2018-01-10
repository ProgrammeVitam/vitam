/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.serverv2;

import static fr.gouv.vitam.common.serverv2.application.ApplicationParameter.CONFIGURATION_FILE_APPLICATION;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import javax.servlet.DispatcherType;
import javax.servlet.ServletContextListener;
import javax.ws.rs.core.Application;

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

/**
 * launch vitam server
 */
public class VitamStarter {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(VitamStarter.class);

    private static final String VITAM_CONF_FILE_NAME = "vitam.conf";
    private static final String SHIRO_FILE = "shiro.ini";
    private static final String IHM_RECETTE = "/ihm-recette";

    private final String role = ServerIdentity.getInstance().getRole();

    private VitamServer vitamServer;

    private Class<? extends Application> businessApplication;
    private Class<? extends Application> adminApplication;
    private Class<? extends VitamApplicationConfiguration> configurationType;
    private List<ServletContextListener> customListeners;

    public VitamStarter(Class<? extends VitamApplicationConfiguration> configurationType,
        String configurationFile,
        Class<? extends Application> businessApplication,
        Class<? extends Application> adminApplication) {
        this(configurationType, configurationFile, businessApplication, adminApplication, null);

    }

    public VitamStarter(Class<? extends VitamApplicationConfiguration> configurationType,
        String configurationFile,
        Class<? extends Application> businessApplication,
        Class<? extends Application> adminApplication,
        List<ServletContextListener> customListeners) {

        this.businessApplication = businessApplication;
        this.adminApplication = adminApplication;
        this.configurationType = configurationType;
        this.customListeners = customListeners;
        configure(configurationFile);
    }

    protected final void configure(String configurationFile) {
        try (final InputStream yamlIS = PropertiesUtils.getConfigAsStream(configurationFile)) {
            final VitamApplicationConfiguration buildConfiguration =
                PropertiesUtils.readYaml(yamlIS, getConfigurationType());
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
            platformSecretConfiguration();

            ContextHandlerCollection applicationHandlers = new ContextHandlerCollection();

            applicationHandlers
                .addHandler(buildApplicationHandler(configurationFile, configuration));
            applicationHandlers.addHandler(
                buildAdminHandler(configurationFile, configuration));

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
     * To allow override on non Vitam platform such as IHM
     */
    protected void platformSecretConfiguration() {
        // Load Platform secret from vitam.conf file
        try (final InputStream yamlIS = PropertiesUtils.getConfigAsStream(VITAM_CONF_FILE_NAME)) {
            final VitamConfigurationParameters vitamConfigurationParameters =
                PropertiesUtils.readYaml(yamlIS, VitamConfigurationParameters.class);

            VitamConfiguration.setSecret(vitamConfigurationParameters.getSecret());
            VitamConfiguration.setFilterActivation(vitamConfigurationParameters.isFilterActivation());

        } catch (final IOException e) {
            LOGGER.error(e);
            throw new IllegalStateException("Cannot start the " + role + " Application Server", e);
        }
    }

    /**
     * Allow override Vitam parameters
     */
    protected void configureVitamParameters() {
        try (final InputStream yamlIS = PropertiesUtils.getConfigAsStream(VITAM_CONF_FILE_NAME)) {
            final VitamConfigurationParameters vitamConfigurationParameters =
                PropertiesUtils.readYaml(yamlIS, VitamConfigurationParameters.class);

            VitamConfiguration.importConfigurationParameters(vitamConfigurationParameters);

        } catch (final IOException e) {
            LOGGER.error(e);
            throw new IllegalStateException("Cannot start the " + role + " Application Server", e);
        }
    }

    public final Class<? extends VitamApplicationConfiguration> getConfigurationType() {
        return configurationType;
    }

    /**
     * Re Implement this method to construct your application specific handler if necessary. </br>
     * </br>
     * If extra register are needed, override the method getResources.</br>
     * If extra filter or action on context are needed, override setFilter.
     *
     * @return the generated Handler
     * @throws VitamApplicationServerException
     * @param configurationFile
     * @param configuration
     */
    protected Handler buildApplicationHandler(String configurationFile, VitamApplicationConfiguration configuration)
        throws VitamApplicationServerException {
        final ServletHolder servletHolder = new ServletHolder(new HttpServletDispatcher());

        servletHolder.setInitParameter("javax.ws.rs.Application", businessApplication.getName());
        servletHolder.setInitParameter(CONFIGURATION_FILE_APPLICATION, configurationFile);

        final ServletContextHandler context = new ServletContextHandler(getSession());

        context.addServlet(servletHolder, "/*");

        // Authorization Filter
        if (VitamConfiguration.isFilterActivation() && !Strings.isNullOrEmpty(VitamConfiguration.getSecret())) {
            context.addFilter(AuthorizationFilter.class, "/*", EnumSet.of(
                DispatcherType.INCLUDE, DispatcherType.REQUEST,
                DispatcherType.FORWARD, DispatcherType.ERROR, DispatcherType.ASYNC));
        }

        context.setVirtualHosts(new String[] {"@business"});

        if (configuration.isAuthentication()) {
            addShiroFilter(context);
        }
        if (configuration.isTenantFilter()) {
            addTenantFilter(context, configuration.getTenants());
        }
        StatisticsHandler stats = new StatisticsHandler();
        stats.setHandler(context);
        return stats;
    }

    protected Handler buildAdminHandler(String configurationFile, VitamApplicationConfiguration configuration)
        throws VitamApplicationServerException {
        final ServletHolder servletHolder = new ServletHolder(new HttpServletDispatcher());

        servletHolder.setInitParameter("javax.ws.rs.Application", adminApplication.getName());
        servletHolder.setInitParameter(CONFIGURATION_FILE_APPLICATION, configurationFile);

        final ServletContextHandler context = new ServletContextHandler(getSession());
        context.addServlet(servletHolder, "/*");

        context.setVirtualHosts(new String[] {"@admin"});

        //no shiro, only internal network
        if (customListeners != null && !customListeners.isEmpty()) {
            customListeners.forEach((listener) -> {
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
        context.addFilter(ShiroFilter.class, "/*", EnumSet.of(
            DispatcherType.INCLUDE, DispatcherType.REQUEST,
            DispatcherType.FORWARD, DispatcherType.ERROR, DispatcherType.ASYNC));

    }

    private void addTenantFilter(ServletContextHandler context, List<Integer> tenantList)
        throws VitamApplicationServerException {
        // Tenant Filter
        try {
            JsonNode node = JsonHandler.toJsonNode(tenantList);
            context.setInitParameter(GlobalDataRest.TENANT_LIST, JsonHandler.unprettyPrint(node));
            context.addFilter(TenantFilter.class, "/*", EnumSet.of(
                DispatcherType.INCLUDE, DispatcherType.REQUEST,
                DispatcherType.FORWARD, DispatcherType.ERROR, DispatcherType.ASYNC));
        } catch (InvalidParseOperationException e) {
            LOGGER.error(e.getMessage(), e);
            throw new VitamApplicationServerException(e.getMessage());
        }
    }

    /**
     * @return 0 for NO_SESSION, 1 for SESSION
     */
    protected int getSession() {
        return ServletContextHandler.NO_SESSIONS;
    }

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
     * @throws VitamApplicationServerException
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

    public boolean isStarted() {
        return vitamServer.isStarted();
    }

    public VitamServer getVitamServer() {
        return vitamServer;
    }

    /**
     *  Method to create VitamStarter only for IHM
     *
     * @param configurationType
     * @param configurationFile
     * @param businessApplication
     * @param adminApplication
     * @param customListeners
     * @return instance VitamStarter configured
     */
    public static VitamStarter createVitamStarterForIHM(
        Class<? extends VitamApplicationConfiguration> configurationType,
        String configurationFile,
        Class<? extends Application> businessApplication,
        Class<? extends Application> adminApplication,
        List<ServletContextListener> customListeners) {

        VitamStarter vitamStarter =
            new VitamStarter(configurationType, configurationFile, 
                businessApplication, adminApplication, customListeners);

        try (final InputStream yamlIS = PropertiesUtils.getConfigAsStream(configurationFile)) {
            final VitamApplicationConfiguration buildConfiguration =
                PropertiesUtils.readYaml(yamlIS, vitamStarter.getConfigurationType());

            ContextHandlerCollection contextHandlerCollection =
                vitamStarter.configureForIHM(buildConfiguration, configurationFile, buildConfiguration.getBaseUrl());

            vitamStarter.startServer(buildConfiguration, contextHandlerCollection);
        } catch (final IOException | VitamApplicationServerException e) {
            LOGGER.error(e);
            throw new IllegalStateException("Cannot start the " + vitamStarter.role + " Application Server", e);
        }

        return vitamStarter;        
    }

    /**
     * Method configure for IHM
     *
     * @param configuration
     * @param configurationFile
     * @param contextPath
     * @return ContextHandlerCollection
     */
    public final ContextHandlerCollection configureForIHM(VitamApplicationConfiguration configuration,
        String configurationFile, String contextPath) {

        ContextHandlerCollection applicationHandlers = new ContextHandlerCollection();

        final HandlerList handlerList = new HandlerList();

        final ResourceHandler staticContentHandler = new ResourceHandler();
        staticContentHandler.setDirectoriesListed(true);
        staticContentHandler.setWelcomeFiles(new String[] {"index.html"});
        staticContentHandler.setResourceBase(configuration.getStaticContent());
        final ContextHandler staticContext = new ContextHandler(configuration.getBaseUri());
        staticContext.setHandler(staticContentHandler);
        handlerList.addHandler(staticContext);

        try {
            handlerList.addHandler(buildApplicationHandler(configurationFile, configuration, contextPath, "@business", true));
            handlerList.addHandler(new DefaultHandler());
            applicationHandlers.addHandler(handlerList);
            applicationHandlers.addHandler(buildAdminHandler(configurationFile, configuration));
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
            throw new IllegalStateException(String.format("Cannot start the %s application server", role), e);
        }

        return applicationHandlers;
    }

    private void startServer(VitamApplicationConfiguration configuration, ContextHandlerCollection applicationHandlers)
        throws VitamApplicationServerException {
        final String jettyConfig = configuration.getJettyConfig();

        LOGGER.info(role + " starts with jetty config");
        vitamServer = VitamServerFactory.newVitamServerByJettyConf(jettyConfig);
        vitamServer.configure(applicationHandlers);
    }
    
    /**
     * Re Implement this method to construct your application specific handler if necessary. </br>
     * </br>
     * If extra register are needed, override the method getResources.</br>
     * If extra filter or action on context are needed, override setFilter.
     *
     * @param configurationFile
     * @param configuration
     * @param contextPath
     * @param virtualHost
     * @param hasSession
     * @return the generated Handler
     * @throws VitamApplicationServerException
     */
    protected Handler buildApplicationHandler(String configurationFile, VitamApplicationConfiguration configuration,
        String contextPath, String virtualHost, boolean hasSession)
        throws VitamApplicationServerException {
        final ServletHolder servletHolder = new ServletHolder(new HttpServletDispatcher());

        servletHolder.setInitParameter("javax.ws.rs.Application", businessApplication.getName());
        servletHolder.setInitParameter(CONFIGURATION_FILE_APPLICATION, configurationFile);

        final ServletContextHandler context = new ServletContextHandler(
            hasSession ? ServletContextHandler.SESSIONS : ServletContextHandler.NO_SESSIONS);

        context.addServlet(servletHolder, "/*");
        context.setContextPath(contextPath);

        // Authorization Filter
        if (VitamConfiguration.isFilterActivation() && !Strings.isNullOrEmpty(VitamConfiguration.getSecret())) {
            context.addFilter(AuthorizationFilter.class, "/*", EnumSet.of(
                DispatcherType.INCLUDE, DispatcherType.REQUEST,
                DispatcherType.FORWARD, DispatcherType.ERROR, DispatcherType.ASYNC));
        }
        
        if (!contextPath.equals(IHM_RECETTE)) {
            context.addFilter(XSRFFilter.class, "/*", EnumSet.of(
                DispatcherType.INCLUDE, DispatcherType.REQUEST,
                DispatcherType.FORWARD, DispatcherType.ERROR, DispatcherType.ASYNC));
        }

        context.setVirtualHosts(new String[] {virtualHost});

        if (configuration.isAuthentication()) {
            addShiroFilter(context);
        }
        if (configuration.isTenantFilter()) {
            addTenantFilter(context, configuration.getTenants());
        }
        StatisticsHandler stats = new StatisticsHandler();
        stats.setHandler(context);
        return stats;
    }


}
