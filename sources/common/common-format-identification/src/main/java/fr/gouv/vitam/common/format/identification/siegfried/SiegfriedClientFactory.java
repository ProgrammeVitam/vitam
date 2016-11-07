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

package fr.gouv.vitam.common.format.identification.siegfried;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.VitamServerFactory;

/**
 * Siegfield Client factory
 */
public final class SiegfriedClientFactory {

    /**
     * Default client operation type
     */
    private static SiegfriedClientType defaultClientType;
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(SiegfriedClientFactory.class);
    private static final SiegfriedClientFactory Siegfried_CLIENT_FACTORY = new SiegfriedClientFactory();

    private String server = "localhost";
    private int port = VitamServerFactory.getDefaultPort();

    private SiegfriedClientFactory() {
        changeConfiguration(null, 0);
    }

    /**
     * Set the SiegfriedClientFactory configuration
     *
     * @param type
     * @param server hostname
     * @param port port to use
     * @throws IllegalArgumentException if type null or if type is OPERATIONS and server is null or empty or port <= 0
     */
    static final void setConfiguration(SiegfriedClientType type, String server, int port) {
        changeDefaultClientType(type);
        if (type == SiegfriedClientType.NORMAL) {
            ParametersChecker.checkParameter("Server cannot be null or empty with OPERATIONS", server);
            ParametersChecker.checkValue("port", port, 1);
        }
        Siegfried_CLIENT_FACTORY.server = server;
        Siegfried_CLIENT_FACTORY.port = port;
    }

    /**
     * Get the SiegfriedClientFactory instance
     *
     * @return the instance
     */
    public static final SiegfriedClientFactory getInstance() {
        return Siegfried_CLIENT_FACTORY;
    }

    /**
     * Get the default type Siegfried client
     *
     * @return the default Siegfried client
     */
    public SiegfriedClient getSiegfriedClient() {
        SiegfriedClient client;
        switch (defaultClientType) {
            case MOCK:
                client = new SiegfriedClientMock();
                break;
            case NORMAL:
                client = new SiegfriedClientRest(server, port);
                break;
            default:
                throw new IllegalArgumentException("Log type unknown");
        }
        return client;
    }

    /**
     * Get the default Siegfried client type
     *
     * @return the default Siegfried client type
     */
    public static SiegfriedClientType getDefaultSiegfriedClientType() {
        return defaultClientType;
    }

    /**
     * Modify the default Siegfried client type
     *
     * @param type the client type to set
     * @throws IllegalArgumentException if type null
     */
    static void changeDefaultClientType(SiegfriedClientType type) {
        if (type == null) {
            throw new IllegalArgumentException();
        }
        defaultClientType = type;
    }

    /**
     * Change client configuration from server/host params
     *
     * @param server the server param
     * @param port the port params
     */
    public final void changeConfiguration(String server, int port) {
        changeDefaultClientType(SiegfriedClientType.MOCK);

        if (server == null) {
            LOGGER.info("No configuration - use mock");
        } else {
            Siegfried_CLIENT_FACTORY.server = server;
            Siegfried_CLIENT_FACTORY.port = port;
            changeDefaultClientType(SiegfriedClientType.NORMAL);
        }
    }

    /**
     * enum to define client type
     */
    public enum SiegfriedClientType {
        /**
         * To use only in MOCK: READ operations are not supported (default)
         */
        MOCK,
        /**
         * Use real service (need server to be set)
         */
        NORMAL
    }
}
