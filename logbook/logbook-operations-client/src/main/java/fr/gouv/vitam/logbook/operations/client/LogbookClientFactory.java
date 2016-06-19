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
package fr.gouv.vitam.logbook.operations.client;

import java.io.IOException;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.VitamServerFactory;
import fr.gouv.vitam.common.server.application.configuration.ClientConfiguration;
import fr.gouv.vitam.common.server.application.configuration.ClientConfigurationImpl;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;

/**
 * LogbookClient factory <br />
 * <br />
 * Use to get a logbook client in function of its type.
 *
 * Example for create operation:
 *
 * <pre>
 * {
 *     &#64;code
 *     // Retrieve default operation client
 *     LogbookClient client = LogbookClientFactory.getInstance().getLogbookOperationClient();
 *
 *     // Retrieve operation parameters class (check {@link LogbookParametersFactory} for more informations)
 *     LogbookParameters parameters = LogbookParametersFactory.newLogbookOperationParameters();
 *
 *     // Use setters
 *     parameters.setParameterValue(LogbookParameterName.eventTypeProcess, LogbookParameterName.eventTypeProcess
 *         .name()).setParameterValue(LogbookParameterName.outcome, LogbookOutcome.STARTED.name());
 *
 *     client.create(parameters);
 * }
 * </pre>
 *
 * Example for update operation:
 *
 * <pre>
 * {
 *     &#64;code
 *     // Retrieve default operation client
 *     LogbookClient client = LogbookClientFactory.getInstance().getLogbookOperationClient();
 *
 *     // Retrieve operation parameters class (check {@link LogbookParametersFactory} for more informations)
 *     LogbookParameters parameters = LogbookParametersFactory.newLogbookOperationParameters();
 *
 *     // Event GUID
 *     parameters.setParameterValue(LogbookParameterName.eventIdentifier,
 *         GUIDFactory.newOperationIdGUID(tenantId).toString());
 *
 *     // Event type
 *     parameters.setParameterValue(LogbookParameterName.eventType, "UNZIP");
 *     parameters.setParameterValue(LogbookParameterName.outcome, LogbookOutcome.STARTED.name());
 *
 *     client.update(parameters);
 * }
 * </pre>
 */
public final class LogbookClientFactory {

    /**
     * Default client operation type
     */
    private static LogbookClientType defaultOperationsClientType;
    private static final String CONFIGURATION_FILENAME = "logbook-client.conf";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookClientFactory.class);
    private static final LogbookClientFactory LOGBOOK_CLIENT_FACTORY = new LogbookClientFactory();

    private String server = "localhost";
    private int port = VitamServerFactory.DEFAULT_PORT;

    private LogbookClientFactory() {
        changeConfigurationFile(CONFIGURATION_FILENAME);
    }

    /**
     * Set the LogbookClientFactory configuration
     *
     * @param type
     * @param server hostname
     * @param port port to use
     * @throws IllegalArgumentException if type null or if type is OPERATIONS and server is null or empty or port <= 0
     */
    static final void setConfiguration(LogbookClientType type, String server, int port) {
        changeDefaultClientType(type);
        if (type == LogbookClientType.OPERATIONS) {
            ParametersChecker.checkParameter("Server cannot be null or empty with OPERATIONS", server);
            ParametersChecker.checkValue("port", port, 1);
        }
        LOGBOOK_CLIENT_FACTORY.server = server;
        LOGBOOK_CLIENT_FACTORY.port = port;
    }

    /**
     * Get the LogbookClientFactory instance
     *
     * @return the instance
     */
    public static final LogbookClientFactory getInstance() {
        return LOGBOOK_CLIENT_FACTORY;
    }

    /**
     * Get the default type logbook client
     *
     * @return the default logbook client
     */
    public LogbookClient getLogbookOperationClient() {
        LogbookClient client;
        switch (defaultOperationsClientType) {
            case MOCK_OPERATIONS:
                client = new LogbookOperationsClientMock();
                break;
            case OPERATIONS:
                client = new LogbookOperationsClientRest(server, port);
                break;
            default:
                throw new IllegalArgumentException("Log type unknown");
        }
        return client;
    }

    /**
     * Modify the default logbook client type
     *
     * @param type the client type to set
     * @throws IllegalArgumentException if type null
     */
    static void changeDefaultClientType(LogbookClientType type) {
        if (type == null) {
            throw new IllegalArgumentException();
        }
        defaultOperationsClientType = type;
    }

    /**
     * Get the default logbook client type
     *
     * @return the default logbook client type
     */
    public static LogbookClientType getDefaultLogbookClientType() {
        return defaultOperationsClientType;
    }

    /**
     * Change client configuration from a Yaml files
     *
     * @param configurationPath the path to the configuration file
     */
    public final void changeConfigurationFile(String configurationPath) {
        changeDefaultClientType(LogbookClientType.MOCK_OPERATIONS);
        ClientConfiguration configuration = null;
        try {
            configuration = PropertiesUtils.readYaml(PropertiesUtils.findFile(configurationPath),
                ClientConfigurationImpl.class);
        } catch (final IOException fnf) {
            LOGGER.debug(String.format("Error when retrieving configuration file %s, using mock",
                CONFIGURATION_FILENAME),
                fnf);
        }
        if (configuration == null) {
            LOGGER.debug(String.format("Error when retrieving configuration file %s, using mock",
                CONFIGURATION_FILENAME));
        } else {
            server = configuration.getServerHost();
            port = configuration.getServerPort();
            changeDefaultClientType(LogbookClientType.OPERATIONS);
        }
    }

    /**
     * enum to define client type
     */
    public enum LogbookClientType {
        /**
         * To use only in MOCK: READ operations are not supported (default)
         */
        MOCK_OPERATIONS,
        /**
         * Use real service (need server to be set)
         */
        OPERATIONS
    }
}
