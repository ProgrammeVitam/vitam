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
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

/**
 * Vitam Server factory for REST server
 */
public class VitamServerFactory {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(VitamServerFactory.class);

    private static final int DEFAULT_PORT = 8082;
    /**
     * Default Server REST port
     */
    private static int defaultPort = DEFAULT_PORT;

    private VitamServerFactory() {
        // Empty constructor
    }

    /**
     * @return a VitamServer with the default port
     */
    public static VitamServer newVitamServerOnDefaultPort() {
        return newVitamServer(defaultPort);
    }

    /**
     * Set a new Default Port
     *
     * @param port
     * @throws IllegalArgumentException if port &lt;= 0
     */
    public static void setDefaultPort(int port) {
        ParametersChecker.checkValue("Port", port, 1);
        defaultPort = port;
    }

    /**
     * Get the Default Port
     *
     * @return the default current port
     */
    public static int getDefaultPort() {
        return defaultPort;
    }

    /**
     * @param port
     * @return a VitamServer with the specified port
     * @throws IllegalArgumentException if port &lt;= 0
     */
    public static VitamServer newVitamServer(int port) {
        ParametersChecker.checkValue("Port", port, 1);
        return new BasicVitamServer(port);
    }

    public static VitamServer newVitamServerWithoutConnector(int port) {
        ParametersChecker.checkValue("Port", port, 1);
        return new BasicVitamServer(port, false);
    }

    /**
     * Create a Vitam Server by jetty config
     *
     * @param jettyConfigFile
     * @return vitam server
     * @throws VitamApplicationServerException
     */
    public static VitamServer newVitamServerByJettyConf(final String jettyConfigFile)
        throws VitamApplicationServerException {
        try {
            ParametersChecker.checkParameter("jetty config file", jettyConfigFile);
            return new BasicVitamServer(jettyConfigFile);
        } catch (final IllegalArgumentException e) {
            LOGGER.error(
                "Jetty server can not run with this jetty config file : " + jettyConfigFile + " " + e.getMessage(),
                e
            );
            throw new VitamApplicationServerException(
                "Jetty server can not run with this jetty config file : " + jettyConfigFile
            );
        }
    }
}
