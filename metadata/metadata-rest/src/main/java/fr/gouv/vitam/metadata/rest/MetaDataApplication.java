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

public class MetaDataApplication {
	private static final Logger LOGGER = Logger.getLogger(MetaDataApplication.class);
	private static final int DEFAULT_PORT = 8082;

	public MetaDataApplication(int serverPort, MetaDataConfiguration configuration) throws Exception {

		ResourceConfig resourceConfig = new ResourceConfig();
		resourceConfig.register(JacksonFeature.class);
		resourceConfig.register(new UnitResource(configuration));

		ServletContainer servletContainer = new ServletContainer(resourceConfig);
		ServletHolder sh = new ServletHolder(servletContainer);
		Server server = new Server(serverPort);
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/");
		context.addServlet(sh, "/*");
		server.setHandler(context);

		server.start();
		server.join();
	}

	public static void main(String[] args) throws Exception {
		int serverPort = DEFAULT_PORT;
		MetaDataConfiguration configuration = null;
		if (args.length >= 1) {
			try {
				FileReader yamlFile = new FileReader(new File(args[0]));
				ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
				configuration = mapper.readValue(yamlFile, MetaDataConfiguration.class);
				
				new MetaDataApplication(serverPort, configuration);

			} catch (Exception e) {
				LOGGER.error("Internal server error!");
			}
		}

	}

}
