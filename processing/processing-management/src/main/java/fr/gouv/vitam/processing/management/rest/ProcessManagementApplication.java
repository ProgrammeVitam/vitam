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
package fr.gouv.vitam.processing.management.rest;

import static java.lang.String.format;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.VitamServer;
import fr.gouv.vitam.common.server.VitamServerFactory;
import fr.gouv.vitam.common.server.application.AbstractVitamApplication;
import fr.gouv.vitam.processing.common.config.ServerConfiguration;
import fr.gouv.vitam.processing.distributor.rest.ProcessDistributorResource;


/**
 * The process management application is to launch process engine vitamServer
 */
public class ProcessManagementApplication
    extends AbstractVitamApplication<ProcessManagementApplication, ServerConfiguration> {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProcessManagementApplication.class);
    private static final String CONF_FILE_NAME = "processing.conf";
    private static final String MODULE_NAME = "processing";
    public static final String PARAMETER_JETTY_SERVER_PORT = "jetty.processing.port";

    private static final ProcessManagementApplication APPLICATION = new ProcessManagementApplication();
    private static VitamServer vitamServer;

    /**
     * ProcessManagementApplication constructor
     */
    public ProcessManagementApplication() {
        super(ProcessManagementApplication.class, ServerConfiguration.class);
    }

    /**
     * Start a service of ProcessManagement with the args as config
     *
     * @param args as String
     */
    public static void main(String[] args) {
        try {
            APPLICATION.startApplication(args);

            if (vitamServer != null && vitamServer.isStarted()) {
                vitamServer.join();
            }

        } catch (final Exception e) {
            LOGGER.error(format(VitamServer.SERVER_CAN_NOT_START, MODULE_NAME) + e.getMessage(), e);
            System.exit(1);
        }
    }

    /**
     * Parses command-line arguments and runs the APPLICATION.
     *
     * @param arguments the command-line arguments
     * @throws RuntimeException Thrown if something goes wrong
     */

    public static void startApplication(String... arguments) throws RuntimeException, VitamApplicationServerException {

        if (arguments == null || arguments.length == 0) {
            LOGGER.error(format(VitamServer.CONFIG_FILE_IS_A_MANDATORY_ARGUMENT, CONF_FILE_NAME));
            throw new IllegalArgumentException(format(VitamServer.CONFIG_FILE_IS_A_MANDATORY_ARGUMENT,
                CONF_FILE_NAME));
        }

        APPLICATION.configure(APPLICATION.computeConfigurationPathFromInputArguments(arguments));
        run(APPLICATION.getConfiguration());
    }


    /**
     * run a vitamServer instance with the configuration and port
     *
     * @param configuration as ServerConfiguration
     * @throws Exception Thrown if something goes wrong
     */
    public static void run(ServerConfiguration configuration) throws VitamApplicationServerException {
        APPLICATION.setConfiguration(configuration);
        final ServletContextHandler context = (ServletContextHandler) APPLICATION.buildApplicationHandler();
        vitamServer = VitamServerFactory.newVitamServerByJettyConf(configuration.getJettyConfig());
        vitamServer.configure(context);

        try {
            vitamServer.start();
        } catch (Exception e) {
            LOGGER.error(format(VitamServer.SERVER_CAN_NOT_START, MODULE_NAME) + e.getMessage(), e);
            throw new VitamApplicationServerException(
                format(VitamServer.SERVER_CAN_NOT_START, MODULE_NAME) + e.getMessage(), e);
        }
    }


    /**
     * stop the lauched vitamServer
     *
     * @throws Exception if the application can not be stopped
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
        resourceConfig.register(MultiPartFeature.class);
        resourceConfig.register(new ProcessManagementResource(getConfiguration()));
        resourceConfig.register(new ProcessDistributorResource(getConfiguration()));

        final ServletContainer servletContainer = new ServletContainer(resourceConfig);
        final ServletHolder sh = new ServletHolder(servletContainer);
        final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        context.addServlet(sh, "/*");
        return context;
    }

    /**
     * Must return the name as a string of your configuration file. Example : "logbook.conf"
     *
     * @return the name of the application configuration file
     */
    @Override
    protected String getConfigFilename() {
        return CONF_FILE_NAME;
    }
}
