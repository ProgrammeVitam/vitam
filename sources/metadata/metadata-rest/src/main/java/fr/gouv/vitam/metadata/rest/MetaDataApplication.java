/*******************************************************************************
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
 *******************************************************************************/
package fr.gouv.vitam.metadata.rest;

import static java.lang.String.format;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import fr.gouv.vitam.api.config.MetaDataConfiguration;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.VitamServer;
import fr.gouv.vitam.common.server.VitamServerFactory;
import fr.gouv.vitam.common.server.application.AbstractVitamApplication;

/**
 * MetaData web server application
 */
public class MetaDataApplication extends AbstractVitamApplication<MetaDataApplication, MetaDataConfiguration> {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MetaDataApplication.class);

    private static VitamServer vitamServer;
    private static final String CONF_FILE_NAME = "metadata.conf";
    private static final String MODULE_NAME = "MataData";
    public static final String PARAMETER_JETTY_SERVER_PORT = "jetty.metadata.port";

    /**
     * Metadata application constructor
     */
    public MetaDataApplication() {
        super(MetaDataApplication.class, MetaDataConfiguration.class);
    }

    /**
     * Start a service of MetaData with the args as config
     *
     * @param args as String array
     */

    public static void main(String[] args) {
        try {
            if (args == null || args.length == 0) {
                LOGGER.error(format(VitamServer.CONFIG_FILE_IS_A_MANDATORY_ARGUMENT, CONF_FILE_NAME));
                throw new IllegalArgumentException(format(VitamServer.CONFIG_FILE_IS_A_MANDATORY_ARGUMENT,
                    CONF_FILE_NAME));
            }

            new MetaDataApplication().configure(args[0]);

            if (vitamServer != null && vitamServer.isStarted()) {
                vitamServer.join();
            }
        } catch (final Exception e) {
            LOGGER.error(format(VitamServer.SERVER_CAN_NOT_START, MODULE_NAME) + e.getMessage(), e);
            System.exit(1);
        }
    }


    /**
     * read the configured parameters of launched server from the file
     *
     * @param configFile : name of configured file
     * @throws Exception
     */
    public void configure(String configFile) throws VitamException {
        try {
            MetaDataApplication application = new MetaDataApplication();
            application.configure(application.computeConfigurationPathFromInputArguments(configFile));
            run(application.getConfiguration());
        } catch (VitamApplicationServerException e) {
            LOGGER.error(format(VitamServer.SERVER_CAN_NOT_START, MODULE_NAME) + e.getMessage(), e);
            throw new VitamApplicationServerException(
                format(VitamServer.SERVER_CAN_NOT_START, MODULE_NAME) + e.getMessage(), e);
        }
    }

    /**
     * run a server instance with the configuration only
     *
     * @param configuration as MetaDataConfiguration
     * @throws Exception
     */

    public static void run(MetaDataConfiguration configuration) throws VitamApplicationServerException {
        final ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(JacksonFeature.class);
        resourceConfig.register(new MetaDataResource(configuration));

        final ServletContainer servletContainer = new ServletContainer(resourceConfig);
        final ServletHolder sh = new ServletHolder(servletContainer);
        final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        context.addServlet(sh, "/*");
        String jettyConfig = configuration.getJettyConfig();
        vitamServer = VitamServerFactory.newVitamServerByJettyConf(jettyConfig);
        vitamServer.getServer().setHandler(context);

        try {
            vitamServer.getServer().start();
        } catch (Exception e) {
            LOGGER.error(format(VitamServer.SERVER_CAN_NOT_START, MODULE_NAME) + e.getMessage(), e);
            throw new VitamApplicationServerException(
                format(VitamServer.SERVER_CAN_NOT_START, MODULE_NAME) + e.getMessage(), e);
        }
    }

    /**
     * Stops the server
     *
     * @throws Exception
     */
    public static void stop() throws Exception {

        if (vitamServer != null && vitamServer.isStarted()) {
            vitamServer.stop();
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
        resourceConfig.register(new MetaDataResource(getConfiguration()));

        final ServletContainer servletContainer = new ServletContainer(resourceConfig);
        final ServletHolder sh = new ServletHolder(servletContainer);
        final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        context.setContextPath("/");
        context.addServlet(sh, "/*");
        return context;
    }

    /**
     * Must return the name as a string of your configuration file. metadata.conf
     *
     * @return the name of the application configuration file
     */
    @Override
    protected String getConfigFilename() {
        return CONF_FILE_NAME;
    }
}
