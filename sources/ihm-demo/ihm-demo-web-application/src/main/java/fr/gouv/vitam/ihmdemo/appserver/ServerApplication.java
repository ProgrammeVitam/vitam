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
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.servlet.ServletContainer;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server2.VitamServer;
import fr.gouv.vitam.common.server2.application.AbstractVitamApplication;
import fr.gouv.vitam.common.server2.application.GenericExceptionMapper;

/**
 * Server application for ihm-demo
 */
public class ServerApplication extends AbstractVitamApplication<ServerApplication, WebApplicationConfig> {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ServerApplication.class);
    private static final String CONF_FILE_NAME = "ihm-demo.conf";
    private static final String MODULE_NAME = "ihm-demo";
    private static final String SHIRO_FILE = "shiro.ini";

    /**
     * ServerApplication constructor
     * 
     * @param configuration
     */
    public ServerApplication(String configuration) {
        super(WebApplicationConfig.class, configuration);
    }

    /**
     * ServerApplication constructor
     * 
     * @param configuration
     */
    ServerApplication(WebApplicationConfig configuration) {
        super(WebApplicationConfig.class, configuration);
    }

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
            final ServerApplication application = new ServerApplication(args[0]);
            application.run();
        } catch (final Exception e) {
            LOGGER.error(format(VitamServer.SERVER_CAN_NOT_START, MODULE_NAME) + e.getMessage(), e);
            System.exit(1);
        }
    }

    @Override
    protected int getSession() {
        return ServletContextHandler.SESSIONS;
    }

    @Override
    protected void platformSecretConfiguration() {
        // No PlatformSecretConfiguration for IHM
    }

    /**
     * replace original implementation to fit specific IHM configuration
     */
    @Override
    protected Handler buildApplicationHandler() throws VitamApplicationServerException {
        final ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(JacksonJsonProvider.class)
            .register(JacksonFeature.class)
            // Register a Generic Exception Mapper
            .register(new GenericExceptionMapper());

        // Register Jersey Metrics Listener
        clearAndconfigureMetrics();
        checkJerseyMetrics(resourceConfig);

        // Use chunk size also in response
        resourceConfig.property(ServerProperties.OUTBOUND_CONTENT_LENGTH_BUFFER, VitamConfiguration.getChunkSize());

        // Cleaner filter
        // resourceConfig.register(ConsumeAllAfterResponseFilter.class);
        registerInResourceConfig(resourceConfig);

        final ServletContainer servletContainer = new ServletContainer(resourceConfig);
        final ServletHolder sh = new ServletHolder(servletContainer);
        final ServletContextHandler context = new ServletContextHandler(getSession());
        // Removed setContextPath to be set later on for IHM
        context.addServlet(sh, "/*");

        // No Authorization Filter

        // Replace here setFilter by adapted one for IHM
        if (getConfiguration().isSecure()) {
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
        context.setContextPath(getConfiguration().getBaseUrl());
        context.setVirtualHosts(new String[] {getConfiguration().getServerHost()});

        // Static Content
        final ResourceHandler staticContentHandler = new ResourceHandler();
        staticContentHandler.setDirectoriesListed(true);
        staticContentHandler.setWelcomeFiles(new String[] {"index.html"});
        final URL webAppDir = Thread.currentThread().getContextClassLoader()
            .getResource(getConfiguration().getStaticContent());
        try {
            staticContentHandler.setResourceBase(webAppDir.toURI().toString());
        } catch (URISyntaxException e) {
            throw new VitamApplicationServerException("Web App Dir incorrect", e);
        }

        // wrap to context handler
        final ContextHandler staticContext = new ContextHandler("/ihm-demo"); /* the server uri path */
        staticContext.setHandler(staticContentHandler);

        // Set Handlers (Static content and REST API)
        final HandlerList handlerList = new HandlerList();
        handlerList.setHandlers(new Handler[] {staticContext, context, new DefaultHandler()});
        return handlerList;
    }

    @Override
    protected void registerInResourceConfig(ResourceConfig resourceConfig) {
        resourceConfig.register(new WebApplicationResource(getConfiguration()));
    }
}
