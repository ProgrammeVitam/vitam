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

package fr.gouv.vitam.common.server;

import fr.gouv.vitam.common.ParametersChecker;

/**
 * Vitam Server factory for REST server
 */
public class VitamServerFactory {
    /**
     * Default Server REST port
     */
    public static final int DEFAULT_PORT = 8082;

    private VitamServerFactory() {
        // Empty constructor
    }

    /**
     *
     * @return a VitamServer with the default port
     */
    public static VitamServer newVitamServerOnDefaultPort() {
        return newVitamServer(DEFAULT_PORT);
    }

    /**
     *
     * @param port
     * @return a VitamServer with the specified port
     * @throws IllegalArgumentException if port <= 0
     */
    public static VitamServer newVitamServer(int port) {
        ParametersChecker.checkValue("Port", port, 1);
        return new BasicVitamServer(port);
    }

}
