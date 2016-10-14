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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

import org.junit.rules.ExternalResource;

import com.google.common.testing.GcFinalization;

import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.junit.VitamApplicationTestFactory.StartApplicationResponse;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

/**
 * This class allows to get an available port during Junit execution
 */
public class JunitHelper extends ExternalResource {
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
    private static final JunitHelper JUNIT_HELPER = new JunitHelper();

    /**
     * Empty constructor
     */
    private JunitHelper() {
        // Empty
    }

    /**
     *
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
        return getAvailablePort();
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
        final int port = getAvailablePort();
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
                setJettyPortSystemProperty(port);
                return port.intValue();
            }
        } while (true);
    }

    /**
     * Remove the used port
     *
     * @param port
     */
    public final synchronized void releasePort(int port) {
        LOGGER.debug("Relaese port: " + port);
        portAlreadyUsed.remove(Integer.valueOf(port));
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
     * @param inputStream
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
     * @param inputStream
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
     * @param port
     */
    public static final void setJettyPortSystemProperty(int port) {
        SystemPropertyUtil.set(PARAMETER_JETTY_SERVER_PORT, Integer.toString(port));
    }

    /**
     * Unset JettyPort System Property
     */
    public static final void unsetJettyPortSystemProperty() {
        SystemPropertyUtil.clear(PARAMETER_JETTY_SERVER_PORT);
    }

    /**
     * Utility to check empty private constructor
     *
     * @param clasz
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
}
