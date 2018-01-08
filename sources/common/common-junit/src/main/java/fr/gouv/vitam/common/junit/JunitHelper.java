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
package fr.gouv.vitam.common.junit;

import com.google.common.testing.GcFinalization;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.junit.VitamApplicationTestFactory.StartApplicationResponse;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.http.BindHttpException;
import org.elasticsearch.node.InternalSettingsPreparer;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeValidationException;
import org.elasticsearch.plugin.analysis.icu.AnalysisICUPlugin;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.transport.BindTransportException;
import org.elasticsearch.transport.Netty4Plugin;
import org.junit.rules.ExternalResource;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

//import org.elasticsearch.node.internal.InternalSettingsPreparer;

/**
 * This class allows to get an available port during Junit execution
 */
public class JunitHelper extends ExternalResource {
    private static final int WAIT_BETWEEN_TRY = 10;
    private static final int WAIT_AFTER_FULL_GC = 100;
    private static final int MAX_PORT = 65535;
    private static final int BUFFER_SIZE = 65536;
    private static final String COULD_NOT_FIND_A_FREE_TCP_IP_PORT_TO_START_EMBEDDED_SERVER_ON =
            "Could not find a free TCP/IP port to start embedded Server on";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(JunitHelper.class);

    private final Set<Integer> portAlreadyUsed = new HashSet<>();
    /**
     * Jetty port SystemProperty
     */
    private static final String PARAMETER_JETTY_SERVER_PORT = "jetty.port";
    public static final String PARAMETER_JETTY_SERVER_PORT_ADMIN = "jetty.port.admin";

    private static final JunitHelper JUNIT_HELPER = new JunitHelper();

    /**
     * Empty constructor
     */
    private JunitHelper() {
        // Empty
    }

    /**
     * @return the unique instance
     */
    public static final JunitHelper getInstance() {
        return JUNIT_HELPER;
    }


    /**
     * @return an available port if it exists
     * @throws IllegalStateException if no port available
     */
    public final synchronized int findAvailablePort() {
        return this.findAvailablePort(PARAMETER_JETTY_SERVER_PORT);
    }

    /**
     * @param environmentVariable if not null, set the port nomber in the system environment
     * @return an available port if it exists
     * @throws IllegalStateException if no port available
     */
    public final synchronized int findAvailablePort(String environmentVariable) {
        int port = getAvailablePort();

        if (PARAMETER_JETTY_SERVER_PORT.equals(environmentVariable) ||
                PARAMETER_JETTY_SERVER_PORT_ADMIN.equals(environmentVariable)) {
            setJettyPortSystemProperty(environmentVariable, port);
        }
        return port;
    }

    /**
     * Find an available port, set the Property jetty.port and call the factory to start the application in one
     * synchronized step.
     *
     * @param testFactory the {@link VitamApplicationTestFactory} to use
     * @return the available and used port if it exists and the started application
     * @throws IllegalStateException if no port available
     */
    public final synchronized StartApplicationResponse<?> findAvailablePortSetToApplication(
            VitamApplicationTestFactory<?> testFactory) {
        if (testFactory == null) {
            throw new IllegalStateException("Factory must not be null");
        }
        final int port = findAvailablePort();
        try {
            final StartApplicationResponse<?> response = testFactory.startVitamApplication(port);
            final int realPort = response.getServerPort();
            if (realPort <= 0) {
                portAlreadyUsed.remove(Integer.valueOf(port));
                throw new IllegalStateException(COULD_NOT_FIND_A_FREE_TCP_IP_PORT_TO_START_EMBEDDED_SERVER_ON);
            }
            if (realPort != port) {
                portAlreadyUsed.add(Integer.valueOf(realPort));
                portAlreadyUsed.remove(Integer.valueOf(port));
            }
            return response;
        } finally {
            unsetJettyPortSystemProperty();
        }
    }

    private final int getAvailablePort() {
        do {
            final Integer port = getPort();
            if (!portAlreadyUsed.contains(port)) {
                portAlreadyUsed.add(port);
                LOGGER.debug("Available port: " + port);
                return port.intValue();
            }
            try {
                Thread.sleep(WAIT_BETWEEN_TRY);
            } catch (final InterruptedException e) {
                SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            }
        } while (true);
    }

    /**
     * Remove the used port
     *
     * @param port to release
     */
    public final synchronized void releasePort(int port) {
        LOGGER.debug("Relaese port: " + port);
        portAlreadyUsed.remove(Integer.valueOf(port));
        unsetJettyPortSystemProperty();
    }

    private final int getPort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        } catch (final IOException e) {
            LOGGER.error(COULD_NOT_FIND_A_FREE_TCP_IP_PORT_TO_START_EMBEDDED_SERVER_ON, e);
            throw new IllegalStateException(COULD_NOT_FIND_A_FREE_TCP_IP_PORT_TO_START_EMBEDDED_SERVER_ON, e);
        }
    }

    /**
     * @param port the port to check on localhost
     * @return True if the port is used by the localhost server
     * @throws IllegalArgumentException if the port is not between 1 and 65535
     */
    public final boolean isListeningOn(int port) {
        return isListeningOn(null, port);
    }

    /**
     * @param host the host to check
     * @param port the port to check on host
     * @return True if the port is used by the specified host
     * @throws IllegalArgumentException if the port is not between 1 and 65535
     */
    public final boolean isListeningOn(String host, int port) {
        if (port < 1 || port > MAX_PORT) {
            throw new IllegalArgumentException("Port must be between 1 and 65535");
        }
        try (Socket socket = new Socket(host, port)) {
            return true;
        } catch (final IOException e) {
            LOGGER.warn("The server is not listening on specified port", e);
            return false;
        }
    }

    /**
     * Read and close the inputStream using buffer read (read(buffer))
     *
     * @param inputStream to read and close
     * @return the size of the inputStream read
     */
    public static final long consumeInputStream(InputStream inputStream) {
        long read = 0;
        if (inputStream == null) {
            return read;
        }
        final byte[] buffer = new byte[BUFFER_SIZE];
        try {
            int len = 0;
            while ((len = inputStream.read(buffer)) >= 0) {
                read += len;
            }
        } catch (final IOException e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }
        try {
            inputStream.close();
        } catch (final IOException e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }
        return read;
    }

    /**
     * Read and close the inputStream one byte at a time (read())
     *
     * @param inputStream to read and close
     * @return the size of the inputStream read
     */
    public static final long consumeInputStreamPerByte(InputStream inputStream) {
        long read = 0;
        if (inputStream == null) {
            return read;
        }
        try {
            while (inputStream.read() >= 0) {
                read++;
            }
        } catch (final IOException e) {
            LOGGER.debug(e);
        }
        try {
            inputStream.close();
        } catch (final IOException e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }
        return read;
    }

    /**
     * For benchmark: clean the used memory using a full GC.</br>
     * </br>
     * Usage:</br>
     * JunitHelper.awaitFullGc();</br>
     * long firstAvailableMemory = Runtime.getRuntime().freeMemory();</br>
     * ... do some tests consuming memory JunitHelper.awaitFullGc();</br>
     * long secondAvailableMemory = Runtime.getRuntime().freeMemory();</br>
     * long usedMemory = firstAvailableMemory - secondAvailableMemory;
     */
    public static final void awaitFullGc() {
        GcFinalization.awaitFullGc();
        try {
            Thread.sleep(WAIT_AFTER_FULL_GC);
        } catch (final InterruptedException e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }
    }

    /**
     * Set JettyPort System Property
     *
     * @param environmentVariable
     * @param port                set to jetty server
     */
    public static final void setJettyPortSystemProperty(String environmentVariable, int port) {
        if (!PARAMETER_JETTY_SERVER_PORT.equals(environmentVariable) &&
                !PARAMETER_JETTY_SERVER_PORT_ADMIN.equals(environmentVariable))
            throw new IllegalArgumentException(
                    "JunitHelper setJettyPortSystemProperty method, accept only [jetty.port or jetty.port.admin] params");

        SystemPropertyUtil.set(environmentVariable, Integer.toString(port));
    }

    /**
     * Unset JettyPort System Property
     */
    public static final void unsetJettyPortSystemProperty() {
        SystemPropertyUtil.clear(PARAMETER_JETTY_SERVER_PORT);
        SystemPropertyUtil.clear(PARAMETER_JETTY_SERVER_PORT_ADMIN);
    }

    /**
     * Utility to check empty private constructor
     *
     * @param clasz class template
     */
    public static final void testPrivateConstructor(Class<?> clasz) {
        // Get the empty constructor
        Constructor<?> c;
        try {
            c = clasz.getDeclaredConstructor();
            // Set it accessible
            c.setAccessible(true);
            // finally call the constructor
            c.newInstance();
        } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException |
                IllegalArgumentException | InvocationTargetException | UnsupportedOperationException e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }
    }

    /**
     * Class to help to build and stop an Elasticsearch server
     */
    public static class ElasticsearchTestConfiguration {
        int tcpPort;
        int httpPort;
        File elasticsearchHome;
        Node node;

        /**
         * @return the associated TCP PORT
         */
        public int getTcpPort() {
            return tcpPort;
        }

        /**
         * @return the associated HTTP PORT
         */
        public int getHttpPort() {
            return httpPort;
        }

        /**
         * @return the associated Home
         */
        public File getElasticsearchHome() {
            return elasticsearchHome;
        }

        /**
         * @return the associated Node
         */
        public Node getNode() {
            return node;
        }
    }

    private static final void tryStartElasticsearch(ElasticsearchTestConfiguration config, TemporaryFolder tempFolder,
                                                    String clusterName) {
        try {
            config.elasticsearchHome = tempFolder.newFolder();
            final Settings settings = Settings.builder()
                    .put("http.enabled", true)
                    .put("http.type", "netty4")
                    //.put("discovery.zen.ping.multicast.enabled", false)
                    .put("transport.tcp.port", config.tcpPort)
                    .put("transport.type", "netty4")
                    .put("http.port", config.httpPort)
                    // The node.client setting has been removed.
                    // Instead, each node role needs to be set separately using the existing node.master, node.data and node.ingest.
                    // By default a node is a master-eligible node and a data node, plus it can pre-process documents through ingest pipelines.
                    //.put("node.client", false)
                    .put("cluster.name", clusterName)
                    .put("path.home", config.elasticsearchHome.getCanonicalPath())
                    .put("plugin.mandatory", "org.elasticsearch.plugin.analysis.icu.AnalysisICUPlugin")
                    .put("transport.tcp.connect_timeout", "1s")
                    .put("transport.profiles.tcp.connect_timeout", "1s")
                    .put("thread_pool.search.size", 4)
                    .put("thread_pool.search.queue_size", VitamConfiguration.getNumberEsQueue())
                    .put("thread_pool.bulk.queue_size", VitamConfiguration.getNumberEsQueue())
                    .build();

            final List<Class<? extends Plugin>> plugins = Arrays.asList(Netty4Plugin.class, AnalysisICUPlugin.class);
            config.node =
                    new NodeWithPlugins(InternalSettingsPreparer.prepareEnvironment(settings, null), plugins);
            config.node.start();
        } catch (BindTransportException | BindHttpException | IOException | NodeValidationException e) {
            LOGGER.error(e);
            config.node = null;
            try {
                Thread.sleep(WAIT_BETWEEN_TRY);
            } catch (final InterruptedException e1) {
                SysErrLogger.FAKE_LOGGER.ignoreLog(e1);
            }
        }
    }

    /**
     * Helper to start an Elasticsearch server (unrecommended version)
     *
     * @param tempFolder  the TemporaryFolder declared as ClassRule within the Junit class
     * @param clusterName the cluster name
     * @param tcpPort     the given TcpPort
     * @param httpPort    the given HttpPort
     * @return the ElasticsearchTestConfiguration to pass to stopElasticsearchForTest
     * @throws VitamApplicationServerException if the Elasticsearch server cannot be started
     */
    public static final ElasticsearchTestConfiguration startElasticsearchForTest(TemporaryFolder tempFolder,
                                                                                 String clusterName, int tcpPort, int httpPort) throws VitamApplicationServerException {
        final ElasticsearchTestConfiguration config = new ElasticsearchTestConfiguration();
        config.httpPort = httpPort;
        config.tcpPort = tcpPort;
        for (int i = 0; i < VitamConfiguration.getRetryNumber(); i++) {
            tryStartElasticsearch(config, tempFolder, clusterName);
            if (config.node != null) {
                return config;
            }
        }
        throw new VitamApplicationServerException("Cannot start Elasticsearch");
    }

    /**
     * Helper to start an Elasticsearch server (recommended version)
     *
     * @param tempFolder  the TemporaryFolder declared as ClassRule within the Junit class
     * @param clusterName the cluster name
     * @return the ElasticsearchTestConfiguration to pass to stopElasticsearchForTest
     * @throws VitamApplicationServerException if the Elasticsearch server cannot be started
     */
    public static final ElasticsearchTestConfiguration startElasticsearchForTest(TemporaryFolder tempFolder,
                                                                                 String clusterName) throws VitamApplicationServerException {
        final JunitHelper junitHelper = getInstance();
        final ElasticsearchTestConfiguration config = new ElasticsearchTestConfiguration();
        for (int i = 0; i < VitamConfiguration.getRetryNumber(); i++) {
            config.tcpPort = junitHelper.findAvailablePort();
            config.httpPort = junitHelper.findAvailablePort();
            tryStartElasticsearch(config, tempFolder, clusterName);
            if (config.node == null) {
                junitHelper.releasePort(config.tcpPort);
                junitHelper.releasePort(config.httpPort);
                config.node = null;
            } else {
                return config;
            }
        }
        throw new VitamApplicationServerException("Cannot start Elasticsearch");
    }

    /**
     * Stop the Elasticsearch server started through start ElasticsearchForTest
     *
     * @param config the ElasticsearchTestConfiguration
     */
    public static final void stopElasticsearchForTest(ElasticsearchTestConfiguration config) {
        if (config != null) {
            final JunitHelper junitHelper = getInstance();
            if (config.node != null) {
                try {
                    config.node.close();
                } catch (IOException e) {
                    SysErrLogger.FAKE_LOGGER.ignoreLog(e);
                    config.node = null;
                }
            }
            junitHelper.releasePort(config.tcpPort);
            junitHelper.releasePort(config.httpPort);
        }
    }

}
