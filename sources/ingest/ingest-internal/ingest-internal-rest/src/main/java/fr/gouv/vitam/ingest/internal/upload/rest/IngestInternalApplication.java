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
package fr.gouv.vitam.ingest.internal.upload.rest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.VitamServer;
import fr.gouv.vitam.common.server.VitamServerFactory;
import fr.gouv.vitam.common.server.application.AbstractVitamApplication;
import fr.gouv.vitam.common.server.application.AdminStatusResource;
import fr.gouv.vitam.common.server.application.BasicVitamStatusServiceImpl;



/**
 * Ingest Internal web server application
 */
public class IngestInternalApplication
    extends AbstractVitamApplication<IngestInternalApplication, IngestInternalConfiguration> {

    private static final String INGEST_INTERNAL_APPLICATION_STARTS_ON_DEFAULT_PORT =
        "IngestInternalApplication Starts on default port";
    private static final String CONFIG_FILE_IS_A_MANDATORY_ARGUMENT = "Config file is a mandatory argument";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestInternalApplication.class);
    private static final String INGEST_INTERNAL_CONF_FILE_NAME = "ingest-internal.conf";
    private static IngestInternalConfiguration configuration;

    /**
     * Ingest Internal constructor
     */
    protected IngestInternalApplication() {
        super(IngestInternalApplication.class, IngestInternalConfiguration.class);
    }

    /**
     * runs Ingest Internal server app
     *
     * @param args
     */
    public static void main(String[] args) {
        try {
            final VitamServer vitamServer = startApplication(args);
            vitamServer.run();
        } catch (final Exception e) {
            LOGGER.error("Can not start Ingest External Application server. " + e.getMessage(), e);
            System.exit(1);
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
            VitamServer vitamServer = null;

            if (args != null && args.length >= 1) {

                try {
                    final File yamlFile = PropertiesUtils.findFile(args[0]);
                    configuration = PropertiesUtils.readYaml(yamlFile, IngestInternalConfiguration.class);
                    String jettyConfig = configuration.getJettyConfig();

                    LOGGER.info("Ingest Internal Starts with jetty config");
                    vitamServer = VitamServerFactory.newVitamServerByJettyConf(jettyConfig);

                } catch (FileNotFoundException e) {
                    LOGGER.warn(INGEST_INTERNAL_APPLICATION_STARTS_ON_DEFAULT_PORT + ", config file not found ", e);
                    vitamServer = VitamServerFactory.newVitamServerOnDefaultPort();
                } catch (IOException e) {
                    LOGGER.warn(INGEST_INTERNAL_APPLICATION_STARTS_ON_DEFAULT_PORT + ", config file io error ", e);
                    vitamServer = VitamServerFactory.newVitamServerOnDefaultPort();
                }
            } else {
                LOGGER.error(CONFIG_FILE_IS_A_MANDATORY_ARGUMENT);
                throw new IllegalArgumentException(CONFIG_FILE_IS_A_MANDATORY_ARGUMENT);
            }

            final IngestInternalApplication application = new IngestInternalApplication();
            application.configure(application.computeConfigurationPathFromInputArguments(args));
            if (vitamServer != null) {
                vitamServer.configure(application.getApplicationHandler());
            }
            return vitamServer;
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e.getMessage(), e);
            throw new IllegalStateException("Cannot start the Ingest Internal Application Server", e);
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
        resourceConfig.register(new IngestInternalResource(getConfiguration()));
        resourceConfig.register(new AdminStatusResource(new BasicVitamStatusServiceImpl()));
        final ServletContainer servletContainer = new ServletContainer(resourceConfig);
        final ServletHolder sh = new ServletHolder(servletContainer);
        final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        context.setContextPath("/");
        context.addServlet(sh, "/*");
        return context;
    }

    /**
     * Must return the name as a string of your configuration file. Example : "ingest-internal.conf"
     *
     * @return the name of the application configuration file
     */
    @Override
    protected String getConfigFilename() {
        return INGEST_INTERNAL_CONF_FILE_NAME;
    }
}
