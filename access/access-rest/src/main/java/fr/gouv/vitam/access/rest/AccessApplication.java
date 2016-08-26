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

import fr.gouv.vitam.access.config.AccessConfiguration;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.VitamServer;
import fr.gouv.vitam.common.server.VitamServerFactory;
import fr.gouv.vitam.common.server.application.AbstractVitamApplication;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import java.io.File;

/**
 * Access web server application
 */
public class AccessApplication extends AbstractVitamApplication<AccessApplication, AccessConfiguration> {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AccessApplication.class);
    private static final String ACCESS_CONF_FILE_NAME = "access.conf";

    private static VitamServer vitamServer;
    private static Server server;
    private static AccessConfiguration configuration;
    private static final String CONFIG_FILE_IS_A_MANDATORY_ARGUMENT = "Config file is a mandatory argument";

    /**
     * AccessApplication constructor
     */
    public AccessApplication() {
        super(AccessApplication.class, AccessConfiguration.class);
    }

    /**
     * runs AccessApplication server app
     *
     * @param args
     */
    public static void main(String[] args) {
        try {
            startApplication(args);

            //TODO centraliser ce join dans un abstract parent
            if (server!=null && server.isStarted()) {
                server.join();
            } else if (vitamServer!=null && vitamServer.getServer()!=null &&
                vitamServer.getServer().isStarted()) {

                vitamServer.getServer().join();
            }

        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage());
        }
    }


    private static void run(AccessConfiguration configuration, int serverPort) throws Exception {
        final ServletContextHandler context = getAccessServletContext(configuration);
        server = new Server(serverPort);
        server.setHandler(context);
        server.start();
    }

    private static void run(AccessConfiguration configuration) throws Exception {
        final ServletContextHandler context = getAccessServletContext(configuration);
        String jettyConfig = configuration.getJettyConfig();
        vitamServer = VitamServerFactory.newVitamServerByJettyConf(jettyConfig);
        vitamServer.configure(context);
        vitamServer.getServer().start();
    }

    private static ServletContextHandler getAccessServletContext(AccessConfiguration configuration) {
        final ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(JacksonFeature.class);
        resourceConfig.register(new AccessResourceImpl(configuration));
        final ServletContainer servletContainer = new ServletContainer(resourceConfig);
        final ServletHolder sh = new ServletHolder(servletContainer);
        final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        context.addServlet(sh, "/*");
        return context;
    }

    /**
     * Prepare the application to be run or started.
     *
     * @param args
     * @return the VitamServer
     * @throws IllegalStateException
     */
    public static void startApplication(String[] args) {
        try {
            final AccessApplication application = new AccessApplication();
            application.configure(application.computeConfigurationPathFromInputArguments(args));

            if (args!=null && args.length >= 1) {
                try {
                    final File yamlFile = PropertiesUtils.findFile(args[0]);
                    configuration = PropertiesUtils.readYaml(yamlFile, AccessConfiguration.class);

                    //TODO ne plus gerer le port en 2e parametres.
                    //TODO Essayer de le setter dans le fichier de conf jetty
                    if (args.length >= 2) {
                        int serverPort = Integer.parseInt(args[1]);
                        if (serverPort <= 0) {
                            serverPort = VitamServerFactory.getDefaultPort();
                        }
                        run(configuration, serverPort);
                    } else {
                        run(configuration);
                    }
                } catch (final Exception e) {
                    LOGGER.error(e.getMessage(), e);
                    throw new RuntimeException(e.getMessage());
                }

            } else {
                LOGGER.error(CONFIG_FILE_IS_A_MANDATORY_ARGUMENT);
                throw new IllegalArgumentException(CONFIG_FILE_IS_A_MANDATORY_ARGUMENT);
            }

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
    public synchronized void configure(String... arguments) throws Exception {

        if (arguments.length >= 1) {
            try {
                final File yamlFile = PropertiesUtils.findFile(arguments[0]);
                configuration = PropertiesUtils.readYaml(yamlFile, AccessConfiguration.class);

                int serverPort = VitamServerFactory.getDefaultPort();
                if (arguments.length >= 2) {
                    serverPort = Integer.parseInt(arguments[1]);
                    if (serverPort <= 0) {
                        serverPort = VitamServerFactory.getDefaultPort();
                    }
                    run(configuration, serverPort);
                } else {
                    run(configuration);
                }

            } catch (final Exception e) {
                LOGGER.error(e.getMessage(), e);
                throw e;
            }
        } else {
            LOGGER.error(CONFIG_FILE_IS_A_MANDATORY_ARGUMENT);
            throw new IllegalArgumentException(CONFIG_FILE_IS_A_MANDATORY_ARGUMENT);
        }

    }

    /**
     * retrive the jetty server
     * @return jetty server
     */
    public static Server getServer() {
        return server;
    }


    /**
     * retrieve the vitam server
     * @return vitam server
     */
    public static VitamServer getVitamServer() {
        return vitamServer;
    }

    /**
     * Stops the server
     * @throws Exception
     */
    public static void stop() throws Exception {
        //TODO centraliser ce stop dans un abstract parent
        if (server!=null && server.isStarted()) {
            server.stop();
        } else if (vitamServer!=null && vitamServer.getServer()!=null &&
            vitamServer.getServer().isStarted()) {
            vitamServer.getServer().stop();
        }
    }
}
