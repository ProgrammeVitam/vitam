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

package fr.gouv.vitam.functional.administration.rest;

import static java.lang.String.format;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.VitamServer;
import fr.gouv.vitam.common.server.VitamServerFactory;
import fr.gouv.vitam.common.server.application.AbstractVitamApplication;
import fr.gouv.vitam.common.server.application.AdminStatusResource;
import fr.gouv.vitam.common.server.application.BasicVitamStatusServiceImpl;

/**
 * admin management web application
 */
public final class AdminManagementApplication
    extends AbstractVitamApplication<AdminManagementApplication, AdminManagementConfiguration> {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AdminManagementApplication.class);
    private static final AdminManagementApplication APPLICATION = new AdminManagementApplication();
    public static final String CONF_FILE_NAME = "functional-administration.conf";
    private static final String MODULE_NAME = "functional-administration";
    private static VitamServer vitamServer;
    private int junitPort = -1;
    
    /**
     * AdminManagementApplication constructor
     */
    protected AdminManagementApplication() {
        super(AdminManagementApplication.class, AdminManagementConfiguration.class);
    }

    /**
     * Main method to run the application (doing start and join)
     *
     * @param args command line parameters
     * @throws IllegalStateException when state exception
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
     * Prepare the application to be run or started._
     *
     * @param args programme
     * @return the VitamServer
     * @throws IllegalStateException when state exception
     */
    public static void startApplication(String[] args) throws VitamException {
        try {
            if (args == null || args.length == 0) {
                LOGGER.error(format(VitamServer.CONFIG_FILE_IS_A_MANDATORY_ARGUMENT, CONF_FILE_NAME));
                throw new VitamApplicationServerException(format(VitamServer.CONFIG_FILE_IS_A_MANDATORY_ARGUMENT,
                    CONF_FILE_NAME));
            }

            APPLICATION.configure(APPLICATION.computeConfigurationPathFromInputArguments(args[0]));
            run(APPLICATION.getConfiguration());

        } catch (final VitamApplicationServerException e) {
            LOGGER.error(format(VitamServer.SERVER_CAN_NOT_START, MODULE_NAME) + e.getMessage(), e);
            throw new VitamException(format(VitamServer.SERVER_CAN_NOT_START, MODULE_NAME) + e.getMessage(), e);
        }
    }

    /**
     * Prepare the application in Junit mode
     *
     * @param port the port from Junit mode
     */
    // FIXME Junit
    static void setupApplication(int port) throws VitamException {
        APPLICATION.junitPort = port;
    }

    private static void run(AdminManagementConfiguration configuration) throws VitamApplicationServerException {
        final ServletContextHandler context = (ServletContextHandler) APPLICATION.buildApplicationHandler();
        final String jettyConfig = configuration.getJettyConfig();
        vitamServer = VitamServerFactory.newVitamServerByJettyConf(jettyConfig);
        vitamServer.configure(context);
        vitamServer.start();
    }

    @Override
    protected Handler buildApplicationHandler() {
        // FIXME Junit
        if (junitPort != -1) {
            getConfiguration().setDbPort(junitPort);
        }
        final ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(JacksonFeature.class);
        resourceConfig.register(new AdminManagementResource(getConfiguration()));
        resourceConfig.register(new AdminStatusResource(new BasicVitamStatusServiceImpl()));
        final ServletContainer servletContainer = new ServletContainer(resourceConfig);
        final ServletHolder sh = new ServletHolder(servletContainer);
        final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        context.setContextPath("/");
        context.addServlet(sh, "/*");
        return context;
    }

    @Override
    protected String getConfigFilename() {
        return CONF_FILE_NAME;
    }

    /**
     * Stops the vitam server
     *
     * @throws Exception
     */
    public static void stop() throws VitamApplicationServerException {
        if (vitamServer != null && vitamServer.isStarted()) {
            vitamServer.stop();
        }
    }
}
