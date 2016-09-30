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
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

import org.junit.rules.ExternalResource;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

/**
 * This class allows to get an available port during Junit execution
 */
public class JunitHelper extends ExternalResource {
    private static final String COULD_NOT_FIND_A_FREE_TCP_IP_PORT_TO_START_EMBEDDED_SERVER_ON =
        "Could not find a free TCP/IP port to start embedded Server on";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(JunitHelper.class);

    private static final Set<Integer> PORT_ALREADY_USED = new HashSet<>();

    /**
     * Empty constructor
     */
    public JunitHelper() {
        // Empty constructor
    }

    /**
     * @return an available port if it exists
     * @throws IllegalStateException if no port available
     */
    public final int findAvailablePort() {
        synchronized (PORT_ALREADY_USED) {
            do {
                final Integer port = getPort();
                if (!PORT_ALREADY_USED.contains(port)) {
                    PORT_ALREADY_USED.add(port);
                    LOGGER.debug("Available port: " + port);
                    return port.intValue();
                }
            } while (true);
        }
    }

    /**
     * Remove the used port
     * 
     * @param port
     */
    public final void releasePort(int port) {
        synchronized (PORT_ALREADY_USED) {
            LOGGER.debug("Relaese port: " + port);
            PORT_ALREADY_USED.remove(Integer.valueOf(port));
        }
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
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535");
        }
        try (Socket socket = new Socket(host, port)) {
            return true;
        } catch (final IOException e) {
            LOGGER.warn("The server is not listening on specified port", e);
            return false;
        }
    }
}
