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
package fr.gouv.vitam.logbook.operations.client;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.client.configuration.ClientConfiguration;
import fr.gouv.vitam.common.client.configuration.ClientConfigurationImpl;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;

import java.io.IOException;

/**
 * Logbook client factory<br />
 * <br />
 * Used to get a client logbook depending on its type.
 *
 * Example of operation creation:
 *
 * <pre>
 * {
 *     &#64;code
 *     // Retrieve default operation client
 *     LogbookClient client = LogbookClientFactory.getInstance().getLogbookOperationClient();
 *
 *     // Retrieve operation parameters class (check {@link LogbookParameterHelper} for more informations)
 *     LogbookParameters parameters = LogbookParametersFactory.newLogbookOperationParameters();
 *
 *     // Use setters
 *     parameters.setParameterValue(LogbookParameterName.eventTypeProcess, LogbookParameterName.eventTypeProcess
 *         .name()).setParameterValue(LogbookParameterName.outcome, StatusCode.STARTED.name());
 *
 *     client.create(parameters);
 * }
 * </pre>
 *
 * Example of operation update:
 *
 * <pre>
 * {
 *     &#64;code
 *     // Retrieve default operation client
 *     LogbookClient client = LogbookClientFactory.getInstance().getLogbookOperationClient();
 *
 *     // Retrieve operation parameters class (check {@link LogbookParameterHelper} for more informations)
 *     LogbookParameters parameters = LogbookParametersFactory.newLogbookOperationParameters();
 *
 *     // Event GUID
 *     parameters.setParameterValue(LogbookParameterName.eventIdentifier,
 *         GUIDFactory.newOperationIdGUID(tenantId).toString());
 *
 *     // Event type
 *     parameters.setParameterValue(LogbookParameterName.eventType, "UNZIP");
 *     parameters.setParameterValue(LogbookParameterName.outcome, StatusCode.STARTED.name());
 *
 *     client.update(parameters);
 * }
 * </pre>
 */

public class LogbookOperationsClientFactory extends VitamClientFactory<LogbookOperationsClient> {

    private static final String CONFIGURATION_FILENAME = "logbook-client.conf";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookOperationsClientFactory.class);
    private static final LogbookOperationsClientFactory LOGBOOK_CLIENT_FACTORY = new LogbookOperationsClientFactory();
    /**
     * RESOURCE PATH
     */
    public static final String RESOURCE_PATH = "/logbook/v1";

    private LogbookOperationsClientFactory() {
        // All requests from client are SMALL, but responses from server could be Huge
        // So Chunked mode inactive on client side
        super(changeConfigurationFile(CONFIGURATION_FILENAME), RESOURCE_PATH, false);
    }

    /**
     * Get the LogbookClientFactory instance
     *
     * @return the instance
     */
    public static final LogbookOperationsClientFactory getInstance() {
        return LOGBOOK_CLIENT_FACTORY;
    }

    /**
     * Get the default type logbook client
     *
     * @return the default logbook client
     */
    @Override
    public LogbookOperationsClient getClient() {
        LogbookOperationsClient client;
        switch (getVitamClientType()) {
            case MOCK:
                client = new LogbookOperationsClientMock();
                break;
            case PRODUCTION:
                client = new LogbookOperationsClientRest(this);
                break;
            default:
                throw new IllegalArgumentException("Log type unknown");
        }
        return client;
    }

    /**
     * Change client configuration from a Yaml files
     *
     * @param configurationPath the path to the configuration file
     * @return ClientConfiguration
     */
    static final ClientConfiguration changeConfigurationFile(String configurationPath) {
        ClientConfiguration configuration = null;
        try {
            configuration = PropertiesUtils.readYaml(
                PropertiesUtils.findFile(configurationPath),
                ClientConfigurationImpl.class
            );
        } catch (final IOException fnf) {
            LOGGER.debug("Error when retrieving configuration file {}, using mock", configurationPath, fnf);
        }
        if (configuration == null) {
            LOGGER.error("Error when retrieving configuration file {}, using mock", configurationPath);
        } else if (
            configuration.getServerHost() == null ||
            configuration.getServerHost().trim().isEmpty() ||
            configuration.getServerPort() <= 0
        ) {
            configuration = null;
        }
        return configuration;
    }

    /**
     * @param configuration null for MOCK
     */
    public static final void changeMode(ClientConfiguration configuration) {
        getInstance().initialisation(configuration, getInstance().getResourcePath());
    }
}
