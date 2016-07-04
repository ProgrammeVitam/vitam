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
package fr.gouv.vitam.ihmdemo.appserver;

import java.io.FileReader;
import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.VitamServerFactory;

/**
 * Server application for ihm-demo
 */
public class ServerApplication {

	private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ServerApplication.class);
	private static final String DEFAULT_WEB_APP_CONTEXT = "/vitam/ihm-demo/api";
	private static final String DEFAULT_STATIC_CONTENT = "webapp";
	private static Server server;

	/**
	 * Start a service of IHM Web Application with the args as config
	 *
	 * @param args
	 *            as String
	 */
	public static void main(String[] args) throws URISyntaxException {
		try {
			final String configFile = args.length >= 1 ? args[0] : null;
			new ServerApplication().configure(configFile);
			server.join();
		} catch (final Exception e) {
			LOGGER.error("Can not start ihm server ", e);
			System.exit(1);
		}
	}

	protected void configure(String configFile) throws Exception {
		try {

			WebApplicationConfig configuration = new WebApplicationConfig();

			if (configFile != null) {
				// Get configuration parameters from Configuration File
				final FileReader yamlFile = new FileReader(configFile);
				final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
				configuration = mapper.readValue(yamlFile, WebApplicationConfig.class);
			} else {
				// Set default parameters
				configuration.setDefaultContext(DEFAULT_WEB_APP_CONTEXT);
				configuration.setPort(VitamServerFactory.getDefaultPort());
				configuration.setVirtualHosts(new String[] {});
				configuration.setStaticContent(DEFAULT_STATIC_CONTENT);
			}

			run(configuration);

		} catch (final Exception e) {
			LOGGER.error("Can not config ihm server ", e);
			throw e;
		}
	}

	/**
	 * run a server instance with the configuration
	 *
	 * @param configuration
	 *            as WebApplicationConfig
	 * @throws Exception
	 */
	public static void run(WebApplicationConfig configuration) throws Exception {
		server = new Server(configuration.getPort());
		// Servlet Container (REST resource)
		final ResourceConfig resourceConfig = new ResourceConfig();
		resourceConfig.register(new WebApplicationResource());
		final ServletContainer servletContainer = new ServletContainer(resourceConfig);
		final ServletHolder restResourceHolder = new ServletHolder(servletContainer);

		final ServletContextHandler restResourceContext = new ServletContextHandler(ServletContextHandler.SESSIONS);
		restResourceContext.setContextPath(configuration.getDefaultContext());
		restResourceContext.setVirtualHosts(configuration.getVirtualHosts());
		restResourceContext.addServlet(restResourceHolder, "/*");

		// Static Content
		final ResourceHandler staticContentHandler = new ResourceHandler();
		staticContentHandler.setDirectoriesListed(true);
		staticContentHandler.setWelcomeFiles(new String[] { "index.html" });
		final URL webAppDir = Thread.currentThread().getContextClassLoader()
				.getResource(configuration.getStaticContent());
		staticContentHandler.setResourceBase(webAppDir.toURI().toString());

		// Set Handlers (Static content and REST API)
		final HandlerList handlerList = new HandlerList();
		handlerList.setHandlers(new Handler[] { staticContentHandler, restResourceContext, new DefaultHandler() });

		server.setHandler(handlerList);
		server.start();
	}

	/**
	 * stop a workspace server
	 *
	 * @throws Exception
	 */
	public static void stop() throws Exception {
		server.stop();
	}

}
