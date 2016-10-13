/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital
 * archiving back-office system managing high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL 2.1
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated
 * <p>
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL 2.1 license and that you accept its terms.
 */
package fr.gouv.vitam.library;

import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.VitamServer;
import fr.gouv.vitam.common.server.VitamServerFactory;
import fr.gouv.vitam.common.server.application.AbstractVitamApplication;
import fr.gouv.vitam.common.server.application.AdminStatusResource;
import fr.gouv.vitam.common.server.application.BasicVitamStatusServiceImpl;
import fr.gouv.vitam.library.config.LibraryConfiguration;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

/**
 * Library static web server application
 */
public class LibraryApplication extends AbstractVitamApplication<LibraryApplication, LibraryConfiguration> {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LibraryApplication.class);
    private static final String LIBRARY_CONF_FILE_NAME = "library.conf";

    private VitamServer vitamServer;

    /**
     * LibraryApplication constructor
     */
    protected LibraryApplication() {
        super(LibraryApplication.class, LibraryConfiguration.class);
    }

    /**
     * runs LibraryApplication server app
     *
     * @param args
     */
    public static void main(String[] args) {
        try {
            final LibraryApplication application = new LibraryApplication();
            application.start(args);
            application.join();
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage());
        }
    }


    /**
     * Prepare the application to be run or started.
     *
     * @param args
     * @return the VitamServer
     * @throws IllegalStateException
     */
    public void start(String[] args) throws VitamApplicationServerException {
        if (vitamServer == null || vitamServer.getServer() == null) {
            LOGGER.info("Configuring server...");
            configure(computeConfigurationPathFromInputArguments(args));
            vitamServer = VitamServerFactory.newVitamServerByJettyConf(getConfiguration().getJettyConfig());
            vitamServer.configure(getApplicationHandler());
        }
        try {
            vitamServer.getServer().start();
        } catch (final Exception e) {
            throw new VitamApplicationServerException(e);
        }
    }

    /**
     * Join on a server (i.e. blocks until the server closes itself). Note : an interrupt (Kill signal, for example)
     * while joining is considered as a normal "stop" behaviour here.
     */
    private void join() {
        if (isStarted()) {
            try {
                vitamServer.getServer().join();
            } catch (InterruptedException e) {
                LOGGER.info("Interrupted while waiting for server to stop (join).");
            }
            vitamServer = null;
        } else {
            LOGGER.info("Server already stopped (or not even created) : nothing to join.");
        }
    }

    public boolean isStarted() {
        return vitamServer != null && vitamServer.getServer() != null && vitamServer.getServer().isStarted();
    }

    /**
     * Stops the server
     *
     * @throws Exception
     */

    public void stop() throws Exception {
        if (isStarted()) {
            vitamServer.getServer().stop();
        } else {
            LOGGER.info("Server already stopped (or not even created) : nothing to stop.");
        }
    }

    /**
     * Static handler : server static pages
     *
     * @return the generated Handler
     */
    @Override
    protected Handler buildApplicationHandler() {
        // Servlet for static resources
        final ServletHolder staticServlet = new ServletHolder("static-content", DefaultServlet.class);
        staticServlet.setInitParameter("resourceBase", getConfiguration().getDirectoryPath());
        staticServlet.setInitParameter("dirAllowed", "true");
        staticServlet.setInitParameter("pathInfoOnly", "true");

        // Jersey resources servlet : add admin resources
        final ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(JacksonFeature.class);
        resourceConfig.register(new AdminStatusResource(new BasicVitamStatusServiceImpl()));
        final ServletHolder jerseyServlet = new ServletHolder(new ServletContainer(resourceConfig));

        // Context handler for /
        final ServletContextHandler contextHandler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        contextHandler.setResourceBase(getConfiguration().getDirectoryPath());
        contextHandler.setContextPath("/");
        // Mixing everything
        contextHandler.addServlet(staticServlet, "/doc/*"); // TODO: externalise constant in config
        contextHandler.addServlet(jerseyServlet, "/*");
        return contextHandler;
    }

    /**
     * Name of the configuration file
     *
     * @return the name of the application configuration file
     */
    @Override
    protected String getConfigFilename() {
        return LIBRARY_CONF_FILE_NAME;
    }


}
