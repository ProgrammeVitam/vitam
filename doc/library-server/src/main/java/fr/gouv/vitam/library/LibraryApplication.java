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
 * In this respect, the user's attention is drawn to the risks associated
 * <p>
 * with loading, using, modifying and/or developing or reproducing the software by the user in light of its specific
 * status of free software, that may mean that it is complicated to manipulate, and that also therefore means that it is
 * reserved for developers and experienced professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their requirements in conditions enabling the
 * security of their systems and/or data to be ensured and, more generally, to use and operate it in the same conditions
 * as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.library;

import static java.lang.String.format;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.servlet.ServletContainer;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server2.VitamServer;
import fr.gouv.vitam.common.server2.application.AbstractVitamApplication;
import fr.gouv.vitam.common.server2.application.ConsumeAllAfterResponseFilter;
import fr.gouv.vitam.common.server2.application.GenericExceptionMapper;
import fr.gouv.vitam.common.server2.application.resources.AdminStatusResource;
import fr.gouv.vitam.library.config.LibraryConfiguration;

/**
 * Library static web server application
 */
public class LibraryApplication extends AbstractVitamApplication<LibraryApplication, LibraryConfiguration> {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LibraryApplication.class);
    private static final String LIBRARY_CONF_FILE_NAME = "library.conf";

    /**
     * LibraryApplication constructor
     *
     * @param configuration
     */
    protected LibraryApplication(String configuration) {
        super(LibraryConfiguration.class, configuration);
    }

    /**
     * runs LibraryApplication server app
     *
     * @param args
     */
    public static void main(String[] args) {
        try {
            if (args == null || args.length == 0) {
                LOGGER.error(format(VitamServer.CONFIG_FILE_IS_A_MANDATORY_ARGUMENT, LIBRARY_CONF_FILE_NAME));
                throw new IllegalArgumentException(
                    format(VitamServer.CONFIG_FILE_IS_A_MANDATORY_ARGUMENT, LIBRARY_CONF_FILE_NAME));
            }
            final LibraryApplication application = new LibraryApplication(args[0]);
            application.run();
        } catch (final Exception e) {
            LOGGER.error(format(VitamServer.SERVER_CAN_NOT_START, "LibraryApplication") + e.getMessage(), e);
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
        resourceConfig.register(JacksonJsonProvider.class)
            .register(JacksonFeature.class)
            // Register a Generic Exception Mapper
            .register(new GenericExceptionMapper())
            .register(new AdminStatusResource());
        // Use chunk size also in response
        resourceConfig.property(ServerProperties.OUTBOUND_CONTENT_LENGTH_BUFFER, VitamConfiguration.getChunkSize());
        // Cleaner filter
        resourceConfig.register(ConsumeAllAfterResponseFilter.class);

        final ServletContainer servletContainer = new ServletContainer(resourceConfig);
        final ServletHolder sh = new ServletHolder(servletContainer);
        // Context handler for /
        final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        context.setResourceBase(getConfiguration().getDirectoryPath());
        context.setContextPath("/");

        // Mixing everything
        context.addServlet(staticServlet, "/doc/*"); // TODO P1: externalise constant in config
        context.addServlet(sh, "/*");

        return context;
    }

    @Override
    protected void registerInResourceConfig(ResourceConfig resourceConfig) {
        // Nothing
    }

}
