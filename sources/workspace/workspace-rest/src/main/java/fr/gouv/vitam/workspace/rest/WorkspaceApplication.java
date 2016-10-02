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
package fr.gouv.vitam.workspace.rest;

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
import fr.gouv.vitam.common.server.application.AdminStatusResource;
import fr.gouv.vitam.common.server.application.BasicVitamStatusServiceImpl;

/**
 * The Workspace APPLICATION.
 */
public class WorkspaceApplication extends AbstractVitamApplication<WorkspaceApplication, WorkspaceConfiguration> {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WorkspaceApplication.class);
    private static final String CONF_FILE_NAME = "workspace.conf";
    private static final String MODULE_NAME = "Access";
    public static final String PARAMETER_JETTY_SERVER_PORT = "jetty.workspace.port";

    private static final WorkspaceApplication APPLICATION = new WorkspaceApplication();
    private static VitamServer vitamServer;


    /**
     * WorkspaceApplication constructor
     */
    public WorkspaceApplication() {
        super(WorkspaceApplication.class, WorkspaceConfiguration.class);
    }

    /**
     * runs the APPLICATION
     *
     * @param args indicate the APPLICATION server config
     */
    public static void main(String[] args) {
        try {
            startApplication(args);

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
     * @param arguments the command-line arguments not null & not empty
     * @throws IllegalArgumentException Thrown if arguments goes wrong
     * @throws VitamApplicationServerException Thrown if something goes wrong
     */

    public static void startApplication(String... arguments)
        throws IllegalArgumentException, VitamApplicationServerException {

        if (arguments == null || arguments.length == 0) {
            LOGGER.error(format(VitamServer.CONFIG_FILE_IS_A_MANDATORY_ARGUMENT, CONF_FILE_NAME));
            throw new IllegalArgumentException(format(VitamServer.CONFIG_FILE_IS_A_MANDATORY_ARGUMENT,
                CONF_FILE_NAME));
        }

        APPLICATION.configure(APPLICATION.computeConfigurationPathFromInputArguments(arguments));
        run(APPLICATION.getConfiguration());
    }

    /**
     * Start workspace application with the configuration class used by unit test
     *
     * @param configuration
     * @throws IllegalArgumentException
     * @throws VitamApplicationServerException
     */
    public static void startApplication(WorkspaceConfiguration configuration)
        throws IllegalArgumentException, VitamApplicationServerException {

        if (configuration == null) {
            LOGGER.error(format(VitamServer.CONFIGURATION_IS_A_MANDATORY_ARGUMENT, "WorkspaceConfiguration"));
            throw new IllegalArgumentException(
                format(VitamServer.CONFIGURATION_IS_A_MANDATORY_ARGUMENT, "WorkspaceConfiguration"));
        }

        APPLICATION.setConfiguration(configuration);
        run(APPLICATION.getConfiguration());
    }


    /**
     * Run workspace server
     *
     * @param configuration Workspace Configuration
     * @throws Exception Thrown if something goes wrong
     */
    public static void run(WorkspaceConfiguration configuration) throws VitamApplicationServerException {

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
     * stop a workspace server
     *
     * @throws Exception in case of error
     */
    public static void stop() throws Exception {
        vitamServer.stop();
    }

    /**
     * Implement this method to construct your APPLICATION specific handler
     *
     * @return the generated Handler
     */
    @Override
    protected Handler buildApplicationHandler() {
        final ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(JacksonFeature.class);
        resourceConfig.register(MultiPartFeature.class);
        resourceConfig.register(new WorkspaceResource(getConfiguration()));
        resourceConfig.register(new AdminStatusResource(new BasicVitamStatusServiceImpl()));
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
     * @return the name of the APPLICATION configuration file
     */
    @Override
    protected String getConfigFilename() {
        return CONF_FILE_NAME;
    }
}
