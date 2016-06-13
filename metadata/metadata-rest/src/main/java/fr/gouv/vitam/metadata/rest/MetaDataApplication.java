package fr.gouv.vitam.metadata.rest;

import java.io.File;
import java.io.FileReader;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import fr.gouv.vitam.api.config.MetaDataConfiguration;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

/**
 * MetaData web server application
 */
public class MetaDataApplication {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MetaDataApplication.class);
    private static final int DEFAULT_PORT = 8082;

    private static Server server;
    private MetaDataConfiguration configuration;

    private static final String CONFIG_FILE_IS_A_MANDATORY_ARGUMENT = "Config file is a mandatory argument";

    // TODO: comment
    public static void main(String[] args) {
        try {
            new MetaDataApplication().configure(args);
            server.join();
        } catch (final Exception e) {
            LOGGER.error(e.getMessage());
            System.exit(1);
        }

    }

    // TODO: comment and probably protected
    public void configure(String... arguments) throws Exception {
        // TODO REVIEW define a real vitam config

        if (arguments.length >= 1) {
            try {
                final FileReader yamlFile = new FileReader(new File(arguments[0]));
                final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
                configuration = mapper.readValue(yamlFile, MetaDataConfiguration.class);
                int serverPort = DEFAULT_PORT;

                if (arguments.length >= 2) {
                    serverPort = Integer.parseInt(arguments[1]);
                    if (serverPort <= 0) {
                        serverPort = DEFAULT_PORT;
                    }
                }
                run(configuration, serverPort);

            } catch (final Exception e) {
                LOGGER.error(e.getMessage());
                throw new RuntimeException(e.getMessage());
            }

        } else {
            LOGGER.error(CONFIG_FILE_IS_A_MANDATORY_ARGUMENT);
            throw new IllegalArgumentException(CONFIG_FILE_IS_A_MANDATORY_ARGUMENT);
        }

    }

    // TODO: comment and probably protected
    public static void run(MetaDataConfiguration configuration, int serverPort) throws Exception {
        final ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(JacksonFeature.class);
        resourceConfig.register(new UnitResource(configuration));

        final ServletContainer servletContainer = new ServletContainer(resourceConfig);
        final ServletHolder sh = new ServletHolder(servletContainer);
        final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        context.addServlet(sh, "/*");
        server = new Server(serverPort);
        server.setHandler(context);
        server.start();
    }

}
