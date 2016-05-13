package fr.gouv.vitam.metadata.rest;

import java.io.File;
import java.io.FileReader;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import fr.gouv.vitam.api.config.MetaDataConfiguration;

/**
 * MetaData web server application
 */
public class MetaDataApplication {
    private static final Logger LOGGER = Logger.getLogger(MetaDataApplication.class);
    private static final int DEFAULT_PORT = 8082;

    private static Server server;
    private MetaDataConfiguration configuration;

    private static final String CONFIG_FILE_IS_A_MANDATORY_ARGUMENT = "Config file is a mandatory argument";

    public static void main(String[] args) {
        try {
            new MetaDataApplication().configure(args);
            server.join();
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            System.exit(1);
        }

    }

    public void configure(String... arguments) throws Exception {
        // FIXME define a vitam config

        if (arguments.length >= 1) {
            try {
                FileReader yamlFile = new FileReader(new File(arguments[0]));
                ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
                configuration = mapper.readValue(yamlFile, MetaDataConfiguration.class);
                int serverPort = DEFAULT_PORT;

                if (arguments.length >= 2) {
                    serverPort = Integer.parseInt(arguments[1]);
                    if (serverPort <= 0) {
                        serverPort = DEFAULT_PORT;
                    }
                }
                run(configuration, serverPort);

            } catch (Exception e) {
                LOGGER.error(e.getMessage());
                throw new RuntimeException(e.getMessage());
            }

        } else {
            LOGGER.error(CONFIG_FILE_IS_A_MANDATORY_ARGUMENT);
            throw new IllegalArgumentException(CONFIG_FILE_IS_A_MANDATORY_ARGUMENT);
        }

    }

    public static void run(MetaDataConfiguration configuration, int serverPort) throws Exception {
        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(JacksonFeature.class);
        resourceConfig.register(new UnitResource(configuration));

        ServletContainer servletContainer = new ServletContainer(resourceConfig);
        ServletHolder sh = new ServletHolder(servletContainer);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        context.addServlet(sh, "/*");
        server = new Server(serverPort);
        server.setHandler(context);
        server.start();
    }

}
