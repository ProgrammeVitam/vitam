/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.external.client.configuration;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client.configuration.ClientConfiguration;

/**
 * ClientConfiguration interface's implementation
 */
public class ClientConfigurationImpl implements ClientConfiguration {

    private static final String CONFIGURATION_PARAMETERS = "ClientConfiguration parameters";
    private String serverHost;
    private int serverPort;

    /**
     * Empty ClientConfiguration constructor for YAMLFactory
     */
    public ClientConfigurationImpl() {
        // nothing
    }

    /**
     * ClientConfiguration constructor
     *
     * @param serverHost server IP address
     * @param serverPort server port
     * @throws IllegalArgumentException if serverHost is null or empty or serverPort &lt;= 0
     */
    public ClientConfigurationImpl(String serverHost, int serverPort) {
        ParametersChecker.checkParameter(CONFIGURATION_PARAMETERS, serverHost);
        if (serverPort <= 0) {
            throw new IllegalArgumentException("Port most be positive");
        }
        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }

    @Override
    public String getServerHost() {
        return serverHost;
    }

    @Override
    public int getServerPort() {
        return serverPort;
    }

    @Override
    public ClientConfigurationImpl setServerHost(String serverHost) {
        ParametersChecker.checkParameter(CONFIGURATION_PARAMETERS, serverHost);
        this.serverHost = serverHost;
        return this;
    }

    @Override
    public ClientConfigurationImpl setServerPort(int serverPort) {
        if (serverPort <= 0) {
            throw new IllegalArgumentException("Port most be positive");
        }
        this.serverPort = serverPort;
        return this;
    }

    @Override
    public boolean isSecure() {
        return false;
    }
}
