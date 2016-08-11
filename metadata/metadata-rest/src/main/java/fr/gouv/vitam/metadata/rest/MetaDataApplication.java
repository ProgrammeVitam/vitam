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

import fr.gouv.vitam.api.config.MetaDataConfiguration;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.VitamServer;
import fr.gouv.vitam.common.server.VitamServerFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import java.io.File;

/**
 * MetaData web server application
 */
public class MetaDataApplication {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MetaDataApplication.class);

    private static Server server;
    private static VitamServer vitamServer;
    private MetaDataConfiguration configuration;

    private static final String CONFIG_FILE_IS_A_MANDATORY_ARGUMENT = "Config file is a mandatory argument";

    /**
     * Start a service of MetaData with the args as config
     *
     * @param args as String array
     */

    public static void main(String[] args) {
        try {
            new MetaDataApplication().configure(args);

            //TODO centraliser ce join dans un abstract parent
            if (server!=null && server.isStarted()) {
                server.join();
            } else if (vitamServer!=null && vitamServer.getServer()!=null &&
                vitamServer.getServer().isStarted()) {

                vitamServer.getServer().join();
            }
        } catch (final Exception e) {
            LOGGER.error(e.getMessage());
            System.exit(1);
        }
    }


    /**
     * read the configured parameters of lauched server from the file
     *
     * @param arguments : name of configured file
     * @throws Exception
     */
    public void configure(String... arguments) throws Exception {
        // FIXME REVIEW define a real vitam config (see Logbook)

        if (arguments.length >= 1) {
            try {
                final File yamlFile = PropertiesUtils.findFile(arguments[0]);
                configuration = PropertiesUtils.readYaml(yamlFile, MetaDataConfiguration.class);

                //TODO ne plus gerer le port en 2e parametres.
                //TODO Essayer de le setter dans le fichier de conf jetty
                if (arguments.length >= 2) {
                    int serverPort = Integer.parseInt(arguments[1]);
                    if (serverPort <= 0) {
                        serverPort = VitamServerFactory.getDefaultPort();
                    }
                    run(configuration, serverPort);
                } else {
                    run(configuration);
                }
            } catch (final Exception e) {
                LOGGER.error(e.getMessage());
                throw new RuntimeException(e.getMessage());
            }

        } else {
            LOGGER.error(CONFIG_FILE_IS_A_MANDATORY_ARGUMENT);
            throw new IllegalArgumentException(CONFIG_FILE_IS_A_MANDATORY_ARGUMENT);
        }

    }

    /**
     * run a server instance with the configuration only
     *
     * @param configuration as MetaDataConfiguration
     * @throws Exception
     */

    public static void run(MetaDataConfiguration configuration) throws Exception {
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
        vitamServer.getServer().start();
    }

    /**
     * run a server instance with the configuration and port
     *
     * @param configuration as MetaDataConfiguration
     * @param serverPort port number of launched server
     * @throws Exception
     */
    public static void run(MetaDataConfiguration configuration, int serverPort) throws Exception {
        final ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(JacksonFeature.class);
        resourceConfig.register(new MetaDataResource(configuration));

        final ServletContainer servletContainer = new ServletContainer(resourceConfig);
        final ServletHolder sh = new ServletHolder(servletContainer);
        final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        context.addServlet(sh, "/*");
        server = new Server(serverPort);
        server.setHandler(context);
        server.start();
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
