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
package fr.gouv.vitam.ihmdemo.appserver;

import static java.lang.String.format;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.EnumSet;

import javax.servlet.DispatcherType;

import org.apache.shiro.web.env.EnvironmentLoaderListener;
import org.apache.shiro.web.servlet.ShiroFilter;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import com.google.common.base.Strings;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.VitamServer;
import fr.gouv.vitam.common.server.VitamServerFactory;

/**
 * Server application for ihm-demo
 */
public class ServerApplication {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ServerApplication.class);
    private static VitamServer vitamServer;
    private static final String CONF_FILE_NAME = "ihm-demo.conf";
    private static final String MODULE_NAME = "ihm-demo";
    private static final String SHIRO_FILE = "shiro.ini";
    private static WebApplicationConfig configuration = null;


    /**
     * Start a service of IHM Web Application with the args as config
     *
     * @param args as String
     * @throws URISyntaxException the string could not be passed as a URI reference
     */
    public static void main(String[] args) throws URISyntaxException {
        try {
            if (args == null || args.length == 0) {
                LOGGER.error(format(VitamServer.CONFIG_FILE_IS_A_MANDATORY_ARGUMENT, CONF_FILE_NAME));
                throw new IllegalArgumentException(
                    format(VitamServer.CONFIG_FILE_IS_A_MANDATORY_ARGUMENT, CONF_FILE_NAME));
            }
            ServerApplication.configure(args[0]);

            if (vitamServer != null && vitamServer.isStarted()) {
                vitamServer.getServer().join();
            }
        } catch (final Exception e) {
            LOGGER.error(format(VitamServer.SERVER_CAN_NOT_START, MODULE_NAME) + e.getMessage(), e);
            System.exit(1);
        }
    }

    protected static void configure(String configFile) throws Exception {
        try {

            configuration = new WebApplicationConfig();

            if (configFile != null) {
                // Get configuration parameters from Configuration File
                final File yamlFile = PropertiesUtils.findFile(configFile);
                configuration = PropertiesUtils.readYaml(yamlFile, WebApplicationConfig.class);
            } else {
                LOGGER.error(format(VitamServer.CONFIG_FILE_IS_A_MANDATORY_ARGUMENT, CONF_FILE_NAME));
                throw new IllegalArgumentException(
                    format(VitamServer.CONFIG_FILE_IS_A_MANDATORY_ARGUMENT, CONF_FILE_NAME));
            }

            run(configuration);

        } catch (final Exception e) {
            LOGGER.error(format(VitamServer.CAN_CONFIGURE_SERVER, MODULE_NAME) + e.getMessage(), e);
            throw e;
        }
    }

    /**
     * run a server instance with the configuration the configuration is never null at this time. It is already
     * instantiate before.
     *
     * @param configuration as WebApplicationConfig
     * @param configuration as WebApplicationConfig
     * @throws Exception the server could not be started
     */
    public static void run(WebApplicationConfig configuration) throws Exception {
        if (configuration == null) {
            throw new VitamApplicationServerException("Configuration not found");
        }

        if (!Strings.isNullOrEmpty(configuration.getJettyConfig())) {
            final String jettyConfig = configuration.getJettyConfig();
            vitamServer = VitamServerFactory.newVitamServerByJettyConf(jettyConfig);
        } else {
            throw new VitamApplicationServerException("jetty config is mandatory");
        }

        // Servlet Container (REST resource)
        final ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(new WebApplicationResource());
        final ServletContainer servletContainer = new ServletContainer(resourceConfig);
        final ServletHolder restResourceHolder = new ServletHolder(servletContainer);

        final ServletContextHandler restResourceContext = new ServletContextHandler(ServletContextHandler.SESSIONS);

        if (configuration.isSecure()) {
            File shiroFile = null;

            try {
                shiroFile = PropertiesUtils.findFile(SHIRO_FILE);
                restResourceContext.setInitParameter("shiroConfigLocations", "file:" + shiroFile.getAbsolutePath());
            } catch (final FileNotFoundException e) {
                LOGGER.error(e.getMessage(), e);
                throw new VitamApplicationServerException(e.getMessage());
            }
            restResourceContext.addEventListener(new EnvironmentLoaderListener());
            restResourceContext.addFilter(ShiroFilter.class, "/*", EnumSet.of(
                DispatcherType.INCLUDE, DispatcherType.REQUEST,
                DispatcherType.FORWARD, DispatcherType.ERROR, DispatcherType.ASYNC));
        }

        restResourceContext.setContextPath(configuration.getBaseUrl());
        restResourceContext.setVirtualHosts(new String[] {configuration.getServerHost()});
        restResourceContext.addServlet(restResourceHolder, "/*");

        // Static Content
        final ResourceHandler staticContentHandler = new ResourceHandler();
        staticContentHandler.setDirectoriesListed(true);
        staticContentHandler.setWelcomeFiles(new String[] {"index.html"});
        final URL webAppDir = Thread.currentThread().getContextClassLoader()
            .getResource(configuration.getStaticContent());
        staticContentHandler.setResourceBase(webAppDir.toURI().toString());

        // wrap to context handler
        final ContextHandler staticContext = new ContextHandler("/ihm-demo"); /* the server uri path */
        staticContext.setHandler(staticContentHandler);

        // Set Handlers (Static content and REST API)
        final HandlerList handlerList = new HandlerList();
        handlerList.setHandlers(new Handler[] {staticContext, restResourceContext, new DefaultHandler()});

        vitamServer.getServer().setHandler(handlerList);
        vitamServer.start();
    }

    /**
     * stop a workspace server
     *
     * @throws Exception the server could not be stopped
     */

    public static void stop() throws Exception {
        if (vitamServer != null && vitamServer.isStarted()) {
            vitamServer.stop();
        }
    }

    /**
     * @return the WebApplicationConfig object
     */
    public static WebApplicationConfig getWebApplicationConfig() {
        return configuration;
    }

    /**
     * Sets the WebApplicationConfig attribute
     * @param webConfiguration 
     *
     */
    public static void setWebApplicationConfig(WebApplicationConfig webConfiguration) {
        configuration = webConfiguration;
    }
}
