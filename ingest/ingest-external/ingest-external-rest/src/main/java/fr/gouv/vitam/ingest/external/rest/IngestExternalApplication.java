/**
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
 */
package fr.gouv.vitam.ingest.external.rest;

import com.fasterxml.jackson.databind.JsonMappingException;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.VitamServer;
import fr.gouv.vitam.common.server.VitamServerFactory;
import fr.gouv.vitam.common.server.application.AbstractVitamApplication;
import fr.gouv.vitam.ingest.external.common.config.IngestExternalConfiguration;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Ingest External web application
 */
public final class IngestExternalApplication extends AbstractVitamApplication<IngestExternalApplication, IngestExternalConfiguration> {
    private static final String INGEST_EXTERNAL_APPLICATION_STARTS_ON_DEFAULT_PORT =
        "IngestExternalApplication Starts on default port";
    private static final String INGEST_EXTERNAL_APPLICATION_STARTS_ON_JETTY_CONFIG =
        "IngestExternalApplication Starts with jetty config";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestExternalApplication.class);
    private static final String INGEST_EXTERNAL_CONF_FILE_NAME = "ingest-external.conf";
    private static Server server;
    private static IngestExternalConfiguration configuration;

    /**
     * LogbookApplication constructor
     */
    protected IngestExternalApplication() {
        super(IngestExternalApplication.class, IngestExternalConfiguration.class);
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
            throw new IllegalStateException("Cannot start the Ingest External Application Server", exc);
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

            if (args!=null && args.length >= 1) {

                try {
                    final File yamlFile = PropertiesUtils.findFile(args[0]);
                    configuration = PropertiesUtils.readYaml(yamlFile, IngestExternalConfiguration.class);
                    String jettyConfig = configuration.getJettyConfig();

                    //TODO ne plus gerer le port en 2e parametres.
                    //TODO Essayer de le setter dans le fichier de conf jetty
                    if (args.length >= 2) {
                        try {
                            final int port = Integer.parseInt(args[1]);
                            if (port <= 0) {
                                LOGGER.info(INGEST_EXTERNAL_APPLICATION_STARTS_ON_DEFAULT_PORT);
                                vitamServer = VitamServerFactory.newVitamServerOnDefaultPort();
                            } else {
                                LOGGER.info("IngestExternalApplication Starts on port: " + port);
                                vitamServer = VitamServerFactory.newVitamServer(port);
                            }
                        } catch (final NumberFormatException e) {
                            LOGGER.info(INGEST_EXTERNAL_APPLICATION_STARTS_ON_DEFAULT_PORT);
                            vitamServer = VitamServerFactory.newVitamServerOnDefaultPort();
                        }
                    } else {
                        LOGGER.info(INGEST_EXTERNAL_APPLICATION_STARTS_ON_JETTY_CONFIG);
                        vitamServer = VitamServerFactory.newVitamServerByJettyConf(jettyConfig);
                    }

                } catch (FileNotFoundException e) {
                    LOGGER.info(INGEST_EXTERNAL_APPLICATION_STARTS_ON_DEFAULT_PORT + ", config file not found ", e);
                    vitamServer = VitamServerFactory.newVitamServerOnDefaultPort();
                } catch (JsonMappingException e) {
                    LOGGER.info(INGEST_EXTERNAL_APPLICATION_STARTS_ON_DEFAULT_PORT + ", config file parsing error ", e);
                    vitamServer = VitamServerFactory.newVitamServerOnDefaultPort();
                } catch (IOException e) {
                    LOGGER.info(INGEST_EXTERNAL_APPLICATION_STARTS_ON_DEFAULT_PORT + ", config file io error ", e);
                    vitamServer = VitamServerFactory.newVitamServerOnDefaultPort();
                }
            } else {
                LOGGER.info(INGEST_EXTERNAL_APPLICATION_STARTS_ON_DEFAULT_PORT+", empty config file");
                vitamServer = VitamServerFactory.newVitamServerOnDefaultPort();
            }

            final IngestExternalApplication application = new IngestExternalApplication();
            application.configure(application.computeConfigurationPathFromInputArguments(args));
            vitamServer.configure(application.getApplicationHandler());
            return vitamServer;
        } catch (final VitamApplicationServerException exc) {
            LOGGER.error(exc);
            throw new IllegalStateException("Cannot start the IngestExternal Application Server", exc);
        }
    }
    
    @Override
    protected String getConfigFilename() {
        return INGEST_EXTERNAL_CONF_FILE_NAME;
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
        resourceConfig.register(new IngestExternalResource(getConfiguration()));

        final ServletContainer servletContainer = new ServletContainer(resourceConfig);
        final ServletHolder sh = new ServletHolder(servletContainer);
        final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        context.setContextPath("/");
        context.addServlet(sh, "/*");
        return context;
    }

}
