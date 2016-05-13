package fr.gouv.vitam.workspace.rest;

import java.io.File;
import java.io.FileReader;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.jackson.JacksonFeature;

import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import fr.gouv.vitam.workspace.api.config.StorageConfiguration;

/**
 * The Workspace application.
 *
 */
public class WorkspaceApplication {
    private static final Logger LOGGER = Logger.getLogger(WorkspaceApplication.class);

    private static final int DEFAULT_PORT = 8082;
    private static Server server;

    private static final String CONFIG_FILE_IS_A_MANDATORY_ARGUMENT = "Config file is a mandatory argument";

    public static void main(String[] args) {
        try {
            new WorkspaceApplication().configure(args);
            server.join();
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Parses command-line arguments and runs the application.
     * 
     * @param arguments
     *            the command-line arguments
     * @throws RuntimeException
     *             Thrown if something goes wrong
     */

    public void configure(String... arguments) throws RuntimeException {
        // FIXME define a vitam config

        if (arguments.length >= 1) {
            try {
                FileReader yamlFile = new FileReader(new File(arguments[0]));
                ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
                StorageConfiguration configuration = new StorageConfiguration();
                configuration = mapper.readValue(yamlFile, StorageConfiguration.class);

                int serverPort = DEFAULT_PORT;

                if (arguments.length >= 2) {
                    serverPort = Integer.parseInt(arguments[1]);
                    if (serverPort >= 0) {
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

    /**
     * Run workspace server
     * 
     * @param configuration
     *            Storage Configuration
     * @throws Exception
     *             Thrown if something goes wrong
     */
    public void run(StorageConfiguration configuration, int serverPort) throws Exception {

        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(JacksonFeature.class);
        resourceConfig.register(MultiPartFeature.class);
        resourceConfig.register(new WorkspaceResource(configuration));

        ServletContainer servletContainer = new ServletContainer(resourceConfig);
        ServletHolder sh = new ServletHolder(servletContainer);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        context.addServlet(sh, "/*");
        server = new Server(serverPort);
        server.setHandler(context);
        server.start();

    }

    public void stop() throws Exception {
        server.stop();
    }

}