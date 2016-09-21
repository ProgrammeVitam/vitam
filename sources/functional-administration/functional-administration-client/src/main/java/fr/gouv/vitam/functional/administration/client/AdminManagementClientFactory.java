/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.functional.administration.client;

import java.io.IOException;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.VitamServerFactory;
import fr.gouv.vitam.common.server.application.configuration.ClientConfiguration;
import fr.gouv.vitam.common.server.application.configuration.ClientConfigurationImpl;

/**
 * admin management client factory use to get client by type "rest" or "mock"
 */
public class AdminManagementClientFactory {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AdminManagementClientFactory.class);
    private static AdminManagementClientType defaultClientType;
    private static final AdminManagementClientFactory ADMIN_MANAGEMENT_CLIENT_FACTORY =
        new AdminManagementClientFactory();
    private static final String CONFIGURATION_FILENAME = "functional-administration-client.conf";

    private String server = "localhost";
    private int port = VitamServerFactory.getDefaultPort();

    private AdminManagementClientFactory() {
        changeConfigurationFile(CONFIGURATION_FILENAME);
    }

    /**
     * Set the AdminManagementClientFactory configuration
     *
     * @param type
     * @param server hostname
     * @param port port to use
     */
    static final void setConfiguration(AdminManagementClientType type, String server, int port) {
        changeDefaultClientType(type);
        if (type == AdminManagementClientType.REST_CLIENT) {
            ParametersChecker.checkParameter("Server cannot be null", server);
            ParametersChecker.checkValue("port", port, 1);
        }
        ADMIN_MANAGEMENT_CLIENT_FACTORY.server = server;
        ADMIN_MANAGEMENT_CLIENT_FACTORY.port = port;
    }

    /**
     * Get the AdminManagementClientFactory instance
     *
     * @return the instance
     */
    public static final AdminManagementClientFactory getInstance() {
        return ADMIN_MANAGEMENT_CLIENT_FACTORY;
    }

    /**
     * Get the default type admin management client
     *
     * @return the default admin management client
     */
    public AdminManagementClient getAdminManagementClient() {
        AdminManagementClient client;
        switch (defaultClientType) {
            case MOCK_CLIENT:
                client = new AdminManagementClientMock();
                break;
            case REST_CLIENT:
                client = new AdminManagementClientRest(server, port);
                break;
            default:
                throw new IllegalArgumentException("Client type unknown");
        }
        return client;
    }

    /**
     * Modify the default AdminManagement client type
     *
     * @param type the client type to set
     * @throws IllegalArgumentException if type null
     */
    static void changeDefaultClientType(AdminManagementClientType type) {
        ParametersChecker.checkParameter("type is a mandatory parameter", type);
        defaultClientType = type;
    }

    /**
     * Change client configuration from a Yaml files
     *
     * @param configurationPath the path to the configuration file
     */
    public final void changeConfigurationFile(String configurationPath) {
        changeDefaultClientType(AdminManagementClientType.MOCK_CLIENT);
        ClientConfiguration configuration = null;
        try {
            configuration = PropertiesUtils.readYaml(PropertiesUtils.findFile(configurationPath),
                ClientConfigurationImpl.class);
        } catch (final IOException fnf) {
            // TODO : See how to alert on the use of the mock system (can be dangerous in production to run with the
            // mock)
            LOGGER.error("Error when retrieving configuration file {}, using mock",
                configurationPath,
                fnf);
        }
        if (configuration == null) {
            LOGGER.error("Error when retrieving configuration file {}, using mock",
                configurationPath);
        } else {
            server = configuration.getServerHost();
            port = configuration.getServerPort();
            changeDefaultClientType(AdminManagementClientType.REST_CLIENT);
        }
    }

    /**
     * enum to define client type
     */
    public enum AdminManagementClientType {
        /**
         * To use only in MOCK
         */
        MOCK_CLIENT,
        /**
         * Use real service (need server to be set)
         */
        REST_CLIENT
    }

}
