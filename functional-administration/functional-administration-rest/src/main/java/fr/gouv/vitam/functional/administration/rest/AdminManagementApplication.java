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

package fr.gouv.vitam.functional.administration.rest;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.VitamServer;
import fr.gouv.vitam.common.server.VitamServerFactory;
import fr.gouv.vitam.common.server.application.AbstractVitamApplication;

/**
 * admin management web application
 */
public final class AdminManagementApplication extends AbstractVitamApplication<AdminManagementApplication, AdminManagementConfiguration> {
    private static final String ADMIN_MANAGEMENT_APPLICATION_STARTS_ON_DEFAULT_PORT =
        "AdminManagementApplication starts on default port";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AdminManagementApplication.class);
    private static final String ADMIN_MANAGEMENT_CONF_FILE_NAME = "functional-administration.conf";

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
     * @throws IllegalStateException
     */
    public static void main(String[] args) {
        try {
            final VitamServer vitamServer = startApplication(args);
            vitamServer.run();
        } catch (final VitamApplicationServerException exc) {
            LOGGER.error(exc);
            throw new IllegalStateException("Cannot start the AdminManagement Application Server", exc);
        }
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
                        LOGGER.info(ADMIN_MANAGEMENT_APPLICATION_STARTS_ON_DEFAULT_PORT);
                        vitamServer = VitamServerFactory.newVitamServerOnDefaultPort();
                    } else {
                        LOGGER.info("AdminManagementApplication starts on port: " + port);
                        vitamServer = VitamServerFactory.newVitamServer(port);
                    }
                } catch (final NumberFormatException e) {
                    LOGGER.info(ADMIN_MANAGEMENT_APPLICATION_STARTS_ON_DEFAULT_PORT);
                    vitamServer = VitamServerFactory.newVitamServerOnDefaultPort();
                }
            } else {
                LOGGER.info(ADMIN_MANAGEMENT_APPLICATION_STARTS_ON_DEFAULT_PORT);
                vitamServer = VitamServerFactory.newVitamServerOnDefaultPort();
            }
            final AdminManagementApplication application = new AdminManagementApplication();
            application.configure(application.computeConfigurationPathFromInputArguments(args));
            vitamServer.configure(application.getApplicationHandler());
            return vitamServer;
        } catch (final VitamApplicationServerException exc) {
            LOGGER.error(exc);
            throw new IllegalStateException("Cannot start the AdminManagement Application Server", exc);
        }
    }

    @Override
    protected Handler buildApplicationHandler() {
        final ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(JacksonFeature.class);
        resourceConfig.register(new AdminManagementResource(getConfiguration()));

        final ServletContainer servletContainer = new ServletContainer(resourceConfig);
        final ServletHolder sh = new ServletHolder(servletContainer);
        final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        context.setContextPath("/");
        context.addServlet(sh, "/*");
        return context;
    }

    @Override
    protected String getConfigFilename() {
        return ADMIN_MANAGEMENT_CONF_FILE_NAME;
    }
}
