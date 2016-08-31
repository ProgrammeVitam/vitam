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

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.VitamServer;
import fr.gouv.vitam.common.server.VitamServerFactory;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Server application for ihm-demo
 */
public class ServerApplication {

	private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ServerApplication.class);
	private static final String DEFAULT_WEB_APP_CONTEXT = "/ihm-demo";
	private static final String DEFAULT_STATIC_CONTENT = "webapp";
	private static final String DEFAULT_HOST = "localhost";
	private static Server server;
	private static VitamServer vitamServer;

	/**
	 * Start a service of IHM Web Application with the args as config
	 * 
	 * @param args 
	 *     as String
	 * @throws URISyntaxException
	 *     the string could not be passed as a URI reference
	 */
	public static void main(String[] args) throws URISyntaxException {
		try {
			final String configFile = args.length >= 1 ? args[0] : null;
			new ServerApplication().configure(configFile);

			//TODO centraliser ce join dans un abstract parent
			if (server!=null && server.isStarted()) {
				server.join();
			} else if (vitamServer!=null && vitamServer.getServer()!=null &&
				vitamServer.getServer().isStarted()) {
				vitamServer.getServer().join();
			}
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
				final File yamlFile = PropertiesUtils.findFile(configFile);
				configuration = PropertiesUtils.readYaml(yamlFile, WebApplicationConfig.class);
			} else {
				// Set default parameters
				configuration.setBaseUrl(DEFAULT_WEB_APP_CONTEXT);
				configuration.setPort(VitamServerFactory.getDefaultPort());
				configuration.setServerHost(DEFAULT_HOST);
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
	 * the configuration is never null at this time. It is already instantiate before.
	 *
	 * @param configuration
	 *            as WebApplicationConfig
	 * @throws Exception
	 *     the server could not be started        
	 */
	public static void run(WebApplicationConfig configuration) throws Exception {
		if(configuration == null){
			throw new VitamApplicationServerException("Configuration not found");
		}

		boolean isServerWithJettyConfig = false;
		if(configuration.getPort()!=0) {
			server = new Server(configuration.getPort());
		} else if(!StringUtils.isBlank(configuration.getJettyConfig())){
			String jettyConfig = configuration.getJettyConfig();
			vitamServer = VitamServerFactory.newVitamServerByJettyConf(jettyConfig);
			isServerWithJettyConfig = true;
		}

		// Servlet Container (REST resource)
		final ResourceConfig resourceConfig = new ResourceConfig();
		resourceConfig.register(new WebApplicationResource());
		final ServletContainer servletContainer = new ServletContainer(resourceConfig);
		final ServletHolder restResourceHolder = new ServletHolder(servletContainer);

		final ServletContextHandler restResourceContext = new ServletContextHandler(ServletContextHandler.SESSIONS);
		restResourceContext.setContextPath(configuration.getBaseUrl());
		restResourceContext.setVirtualHosts(new String[] { configuration.getServerHost() });
		restResourceContext.addServlet(restResourceHolder, "/*");

		// Static Content
		final ResourceHandler staticContentHandler = new ResourceHandler();
		staticContentHandler.setDirectoriesListed(true);
		staticContentHandler.setWelcomeFiles(new String[] { "index.html" });
		final URL webAppDir = Thread.currentThread().getContextClassLoader()
				.getResource(configuration.getStaticContent());
		staticContentHandler.setResourceBase(webAppDir.toURI().toString());
		
		//wrap to context handler
		ContextHandler staticContext = new ContextHandler("/ihm-demo"); /* the server uri path */
		staticContext.setHandler(staticContentHandler);

		// Set Handlers (Static content and REST API)
		final HandlerList handlerList = new HandlerList();
		handlerList.setHandlers(new Handler[] { staticContext, restResourceContext, new DefaultHandler() });

		if(!isServerWithJettyConfig) {
			server.setHandler(handlerList);
			server.start();
		} else {
			vitamServer.getServer().setHandler(handlerList);
			vitamServer.getServer().start();
		}
	}

	/**
	 * stop a workspace server
	 *
	 * @throws Exception
	 *     the server could not be stopped
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
