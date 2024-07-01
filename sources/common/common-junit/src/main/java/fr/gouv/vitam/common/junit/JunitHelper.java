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
package fr.gouv.vitam.common.junit;

import com.google.common.collect.Sets;
import com.google.common.testing.GcFinalization;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import org.apache.shiro.util.Assert;
import org.junit.rules.ExternalResource;

import javax.net.ServerSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

/**
 * This class allows to get an available port during Junit execution
 */
public class JunitHelper extends ExternalResource {

    private static final int WAIT_BETWEEN_TRY = 10;
    private static final int WAIT_AFTER_FULL_GC = 100;
    public static final int MIN_PORT = 11112;
    private static final int MAX_PORT = 65535;

    private static final int BUFFER_SIZE = 65536;
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

        if (
            PARAMETER_JETTY_SERVER_PORT.equals(environmentVariable) ||
            PARAMETER_JETTY_SERVER_PORT_ADMIN.equals(environmentVariable)
        ) {
            setJettyPortSystemProperty(environmentVariable, port);
        }
        return port;
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
        return SocketType.TCP.findAvailablePort(MIN_PORT, MAX_PORT);
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

    public static InputStream getPerByteInputStream(InputStream inputStream) {
        return new InputStream() {
            @Override
            public int read() throws IOException {
                return inputStream.read();
            }

            @Override
            public int read(byte[] b) throws IOException {
                return read(b, 0, b.length);
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                if (len == 0) {
                    return 0;
                }
                int read = read();
                if (read == -1) {
                    return -1;
                }
                b[off] = (byte) read;
                return 1;
            }
        };
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
     * @param port set to jetty server
     */
    public static final void setJettyPortSystemProperty(String environmentVariable, int port) {
        if (
            !PARAMETER_JETTY_SERVER_PORT.equals(environmentVariable) &&
            !PARAMETER_JETTY_SERVER_PORT_ADMIN.equals(environmentVariable)
        ) throw new IllegalArgumentException(
            "JunitHelper setJettyPortSystemProperty method, accept only [jetty.port or jetty.port.admin] params"
        );

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
        } catch (
            NoSuchMethodException
            | SecurityException
            | InstantiationException
            | IllegalAccessException
            | IllegalArgumentException
            | InvocationTargetException
            | UnsupportedOperationException e
        ) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }
    }

    private static final Set<Integer> usedPort = Sets.newConcurrentHashSet();

    /**
     * Copied from Spring SocketUtils
     */
    private enum SocketType {
        TCP {
            @Override
            protected boolean isPortAvailable(int port) {
                if (usedPort.contains(port)) {
                    return false;
                }

                try {
                    ServerSocket serverSocket = ServerSocketFactory.getDefault()
                        .createServerSocket(port, 1, InetAddress.getByName("localhost"));
                    serverSocket.close();
                    return true;
                } catch (Exception ex) {
                    return false;
                }
            }
        },

        UDP {
            @Override
            protected boolean isPortAvailable(int port) {
                if (usedPort.contains(port)) {
                    return false;
                }

                try {
                    DatagramSocket socket = new DatagramSocket(port, InetAddress.getByName("localhost"));
                    socket.close();
                    return true;
                } catch (Exception ex) {
                    return false;
                }
            }
        };

        /**
         * Determine if the specified port for this {@code SocketType} is
         * currently available on {@code localhost}.
         */
        protected abstract boolean isPortAvailable(int port);

        int findAvailablePort(int minPort, int maxPort) {
            Assert.isTrue(minPort > 0, "'minPort' must be greater than 0");
            Assert.isTrue(maxPort >= minPort, "'maxPort' must be greater than or equal to 'minPort'");
            Assert.isTrue(maxPort <= MAX_PORT, "'maxPort' must be less than or equal to " + MAX_PORT);

            Integer candidatePort = null;
            for (int port = minPort; port <= maxPort; port++) {
                if (!isPortAvailable(port)) {
                    continue;
                }

                usedPort.add(port);

                candidatePort = port;

                break;
            }

            if (candidatePort == null) {
                throw new IllegalStateException(
                    String.format(
                        "Could not find an available %s port in the range [%d, %d] after %d attempts",
                        name(),
                        minPort,
                        maxPort,
                        usedPort.size()
                    )
                );
            }

            return candidatePort;
        }
    }
}
