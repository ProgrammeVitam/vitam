/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
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
package fr.gouv.vitam.common.server;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.xml.XmlConfiguration;
import org.xml.sax.SAXException;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Basic implementation of a vitam server using embedded jetty as underlying app server
 */
public class BasicVitamServer implements VitamServer {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(BasicVitamServer.class);
    private static final String A_PROBLEM_OCCURRED_WHILE_ATTEMPTING_TO_START_THE_SERVER =
        "A problem occurred while attempting to start the server";
    /**
     * Default TEST ONLY Jetty config file
     */

    private int port;
    private Handler handler;
    private Server server;
    private XmlConfiguration serverConfiguration;
    private boolean configured = false;
    VitamThreadPoolExecutor vitamThreadPoolExecutor = new VitamThreadPoolExecutor();

    protected BasicVitamServer(int port, boolean withConnector) {
        ParametersChecker.checkValue("You must provide a valid port number", port, 1);
        this.port = port;
        server = new Server(vitamThreadPoolExecutor);
        if (withConnector) {
            final ServerConnector serverConnector = new ServerConnector(server);
            serverConnector.setPort(port);
            server.addConnector(serverConnector);
        }
    }

    /**
     * A Vitam server can only be instantiated with a given port to listen to
     *
     * @param port the port to listen to (must be a valid logical port number)
     * @throws IllegalArgumentException if port &lt;= 0
     */
    protected BasicVitamServer(int port) {
        this(port, true);
    }

    /**
     * A Vitam server can be instantiated with a jetty xml configuration file. This configuration file can be in : -
     * /vitam/conf, - resource folder - resource in classpath
     *
     * @param jettyConfigPath configuration file of jetty server
     * @throws VitamApplicationServerException if configuration not found, can't be parsed, can't be read or server
     * can't started
     */
    protected BasicVitamServer(final String jettyConfigPath) throws VitamApplicationServerException {
        try {
            LOGGER.info("Starting server with configuration file : " + jettyConfigPath);
            try (final Resource resource = Resource.newResource(PropertiesUtils.getConfigFile(jettyConfigPath))) {
                serverConfiguration = new XmlConfiguration(resource);
                server = new Server(vitamThreadPoolExecutor);
                server = (Server) serverConfiguration.configure(server);
                configured = true;

                LOGGER.info("Server started.");
            }
        } catch (final FileNotFoundException e) {
            setConfigured(false);
            LOGGER.error("Server configuration file not found.", e);
            throw new VitamApplicationServerException(e);
        } catch (final SAXException e) {
            setConfigured(false);
            LOGGER.error("Server configuration file can't be parsed.", e);
            throw new VitamApplicationServerException(e);
        } catch (final IOException e) {
            setConfigured(false);
            LOGGER.error("Server configuration file can't be read.", e);
            throw new VitamApplicationServerException(e);
        } catch (final Exception e) {
            setConfigured(false);
            LOGGER.error("Server can't be started.", e);
            throw new VitamApplicationServerException(e);
        }
    }

    @Override
    public void configure(Handler applicationHandler) throws VitamApplicationServerException {
        if (applicationHandler == null) {
            throw new VitamApplicationServerException("You must provide a handler to give to the server");
        }
        setHandler(applicationHandler);
        getServer().setHandler(applicationHandler);
        setConfigured(true);
    }

    @Override
    public void startAndJoin() throws VitamApplicationServerException {
        start();
        try {
            server.join();
        } catch (final Exception exc) {
            throw new VitamApplicationServerException(A_PROBLEM_OCCURRED_WHILE_ATTEMPTING_TO_START_THE_SERVER, exc);
        }
    }

    /**
     * For Junit tests, starts only, not join
     *
     * @throws VitamApplicationServerException
     */
    @Override
    public void start() throws VitamApplicationServerException {
        if (!isConfigured()) {
            throw new VitamApplicationServerException("You must configure the server before running");
        }
        try {
            server.start();
        } catch (final Exception exc) {
            throw new VitamApplicationServerException(A_PROBLEM_OCCURRED_WHILE_ATTEMPTING_TO_START_THE_SERVER, exc);
        }
    }

    /**
     * For Junit tests, stops the server
     *
     * @throws VitamApplicationServerException
     */
    @Override
    public void stop() throws VitamApplicationServerException {
        if (!isConfigured()) {
            throw new VitamApplicationServerException("You must configure the server before running");
        }
        try {
            server.stop();
        } catch (final Exception exc) {
            throw new VitamApplicationServerException(A_PROBLEM_OCCURRED_WHILE_ATTEMPTING_TO_START_THE_SERVER, exc);
        }
    }

    /**
     * Retrieving the underlying jetty server is restricted to sub-implementations only
     *
     * @return the underlying jetty server
     */
    @Override
    public Server getServer() {
        return server;
    }

    /**
     * Retrieving the server jetty configuration
     *
     * @return XmlConfiguration
     */
    @Override
    public XmlConfiguration getServerConfiguration() {
        return serverConfiguration;
    }

    /**
     * check if vitam server is started
     *
     * @return true if jetty server is started
     */
    @Override
    public boolean isStarted() {
        if (server != null) {
            return server.isStarted();
        } else {
            LOGGER.error("Jetty Server is null");
            return false;
        }
    }

    /**
     * check if vitam server is stopped
     *
     * @return true if jetty server is stopped
     */
    @Override
    public boolean isStopped() {
        if (server != null) {
            return server.isStopped();
        } else {
            LOGGER.error("Jetty Server is null");
            return false;
        }
    }

    /**
     * Retrieving the vitam server port.</br>
     *
     * If the server is started, this returns the real port used. If not, returns the supposely configured one.
     *
     * @return the vitam server port
     */
    @Override
    public int getPort() {
        if (server != null && server.isStarted()) {
            int length = server.getConnectors().length;
            for (int i = 0; i < length; i++) {
                ServerConnector c = ((ServerConnector) server.getConnectors()[i]);
                if (VitamServerInterface.BUSINESS_CONNECTOR_NAME.equals(c.getName())) return c.getLocalPort();
            }
        }
        return port;
    }

    /**
     * Retrieving all ports of the vitam server.</br>
     *
     * @return the vitam server port
     */
    @Override
    public int getAdminPort() {
        if (server != null && server.isStarted()) {
            int length = server.getConnectors().length;
            for (int i = 0; i < length; i++) {
                ServerConnector c = ((ServerConnector) server.getConnectors()[i]);
                if (VitamServerInterface.ADMIN_CONNECTOR_NAME.equals(c.getName())) return c.getLocalPort();
            }
        }
        return -1;
    }

    /**
     * check if is configured
     *
     * @return true if it is configured
     */
    @Override
    public boolean isConfigured() {
        return configured;
    }

    /**
     * setter configured status
     *
     * @param configured configured status
     */
    protected void setConfigured(boolean configured) {
        this.configured = configured;
    }

    /**
     * retrieving the handler
     *
     * @return the handler
     */
    @Override
    public Handler getHandler() {
        return handler;
    }

    /**
     * setter of the handler
     *
     * @param handler the handler to set
     */
    @Override
    public void setHandler(Handler handler) {
        ParametersChecker.checkParameter("Handler must not be nul", handler);
        this.handler = handler;
    }

    /**
     * @return the VitamThreadPoolExecutor used by the server
     */
    public VitamThreadPoolExecutor getVitamThreadPoolExecutor() {
        return vitamThreadPoolExecutor;
    }
}
