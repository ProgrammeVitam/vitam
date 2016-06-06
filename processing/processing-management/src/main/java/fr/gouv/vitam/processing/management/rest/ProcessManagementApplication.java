/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 * 
 * This software is a computer program whose purpose is to implement a digital 
 * archiving back-office system managing high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL 2.1
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL 2.1 license and that you accept its terms.
 */
package fr.gouv.vitam.processing.management.rest;

import java.io.File;
import java.io.FileReader;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.processing.common.config.ServerConfiguration;



/**
 * The process management application is to launch process engine server
 */
public class ProcessManagementApplication {
   
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProcessManagementApplication.class);    

    private static final int DEFAULT_PORT = 8082;
    private static Server server;

    private static final String CONFIG_FILE_IS_A_MANDATORY_ARGUMENT = "Config file is a mandatory argument";
   
    /** Start a service of ProcessManagement with the args as config
     * @param args as String
     */
    public static void main(String[] args) {
        try {
            new ProcessManagementApplication().configure(args);
            server.join();
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            System.exit(1);
        }
    }
    
    /** read the configured parameters of lauched server from the file
     * @param arguments : name of configured file
     * @throws RuntimeException
     */
    public void configure(String... arguments) throws RuntimeException {
        if (arguments.length >= 1) {
            try {
                FileReader yamlFile = new FileReader(new File(arguments[0]));
                ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
                ServerConfiguration configuration = new ServerConfiguration();
                configuration = mapper.readValue(yamlFile, ServerConfiguration.class);

                int serverPort = DEFAULT_PORT;

                if (arguments.length >= 2) {
                    serverPort = Integer.parseInt(arguments[1]);
                    if (serverPort < 0) {
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
     * run a server instance with the configuration and port 
     * @param configuration as ServerConfiguration
     * @param serverPort port number of launched server
     * @throws Exception
     */
    public static void run(ServerConfiguration configuration, int serverPort) throws Exception {

        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(JacksonFeature.class);
        resourceConfig.register(MultiPartFeature.class);
        resourceConfig.register(new ProcessManagementResource(configuration));

        ServletContainer servletContainer = new ServletContainer(resourceConfig);
        ServletHolder sh = new ServletHolder(servletContainer);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        context.addServlet(sh, "/*");
        server = new Server(serverPort);
        server.setHandler(context);
        server.start();
    }    
    
    /**
     * stop the lauched server
     * @throws Exception
     */
    public void stop() throws Exception {
        server.stop();
    }
        
}
