/*******************************************************************************
 * This file is part of Vitam Project.
 *
 * Copyright Vitam (2012, 2015)
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL license as circulated
 * by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL license and that you
 * accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.access.rest;

import java.io.File;
import java.io.FileReader;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import fr.gouv.vitam.access.config.AccessConfiguration;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.VitamServer;
import fr.gouv.vitam.common.server.VitamServerFactory;
import fr.gouv.vitam.common.server.application.AbstractVitamApplication;

/**
 * Access web server application
 */
public class AccessApplication extends AbstractVitamApplication<AccessApplication, AccessConfiguration> {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AccessApplication.class);
    private static final String ACCESS_CONF_FILE_NAME = "access.conf";

    private static Server server;
    private AccessConfiguration configuration;
    private static final String CONFIG_FILE_IS_A_MANDATORY_ARGUMENT = "Config file is a mandatory argument";

    /**
     * AccessApplication constructor
     */
    protected AccessApplication() {
        super(AccessApplication.class, AccessConfiguration.class);
    }

    /**
     * runs AccessApplication server app
     *
     * @param args
     */
    public static void main(String[] args) {
        try {
            final VitamServer vitamServer = startApplication(args);
            vitamServer.run();
        } catch (final VitamApplicationServerException exc) {
            LOGGER.error(exc);
            throw new IllegalStateException("Cannot start the Access Application Server", exc);
        }
    }


    private static void run(AccessConfiguration configuration, int serverPort) throws Exception {
        final ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(JacksonFeature.class);

        final ServletContainer servletContainer = new ServletContainer(resourceConfig);
        final ServletHolder sh = new ServletHolder(servletContainer);
        final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        context.addServlet(sh, "/*");
        server = new Server(serverPort);
        server.setHandler(context);
        server.start();
    }

    /**
     * Prepare the application to be run or started.
     *
     * @param args
     * @return the VitamServer
     * @throws IllegalStateException
     */
    public static VitamServer startApplication(String[] args) {
        try {
            VitamServer vitamServer;
            if (args != null && args.length >= 2) {
                try {
                    final int port = Integer.parseInt(args[1]);
                    if (port <= 0) {
                        LOGGER.info("AccessApplication Starts on default port");
                        vitamServer = VitamServerFactory.newVitamServerOnDefaultPort();
                    } else {
                        LOGGER.info("AccessApplication Starts on port: " + port);
                        vitamServer = VitamServerFactory.newVitamServer(port);
                    }
                } catch (final NumberFormatException e) {
                    LOGGER.info("The port " + args + " is not a number. AccessApplication Starts on default port", e);
                    vitamServer = VitamServerFactory.newVitamServerOnDefaultPort();
                }
            } else {
                LOGGER.info("AccessApplication Starts on default port");
                vitamServer = VitamServerFactory.newVitamServerOnDefaultPort();
            }
            final AccessApplication application = new AccessApplication();
            application.configure(application.computeConfigurationPathFromInputArguments(args));
            vitamServer.configure(application.getApplicationHandler());
            return vitamServer;
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e.getMessage(), e);
            throw new IllegalStateException("Cannot start the Access Application Server", e);
        }
    }

    /**
     * Implement this method to construct your application specific handler
     *
     * @return the generated Handler
     */
    @Override
    protected Handler buildApplicationHandler() {
        final ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(JacksonFeature.class);
        resourceConfig.register(new AccessResourceImpl(getConfiguration()));

        final ServletContainer servletContainer = new ServletContainer(resourceConfig);
        final ServletHolder sh = new ServletHolder(servletContainer);
        final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        context.setContextPath("/");
        context.addServlet(sh, "/*");
        return context;
    }

    /**
     * Must return the name as a string of your configuration file. Example : "access.conf"
     *
     * @return the name of the application configuration file
     */
    @Override
    protected String getConfigFilename() {
        return ACCESS_CONF_FILE_NAME;
    }

    /**
     * read the configured parameters of launched server from the file
     *
     * @param arguments : name of configured file
     * @throws RuntimeException
     */
    public void configure(String... arguments) throws Exception {

        if (arguments.length >= 1) {
            try {
                final FileReader yamlFile = new FileReader(new File(arguments[0]));
                final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
                configuration = mapper.readValue(yamlFile, AccessConfiguration.class);
                int serverPort = VitamServerFactory.getDefaultPort();

                if (arguments.length >= 2) {
                    serverPort = Integer.parseInt(arguments[1]);
                    if (serverPort <= 0) {
                        serverPort = VitamServerFactory.getDefaultPort();
                    }
                }
                run(configuration, serverPort);

            } catch (final Exception e) {
                LOGGER.error(e.getMessage(), e);
                throw e;
            }

        } else {
            LOGGER.error(CONFIG_FILE_IS_A_MANDATORY_ARGUMENT);
            throw new IllegalArgumentException(CONFIG_FILE_IS_A_MANDATORY_ARGUMENT);
        }

    }
}
