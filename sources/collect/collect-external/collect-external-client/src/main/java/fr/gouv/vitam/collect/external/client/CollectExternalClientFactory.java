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
package fr.gouv.vitam.collect.external.client;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.client.configuration.ClientConfiguration;
import fr.gouv.vitam.common.external.client.VitamClientFactory;
import fr.gouv.vitam.common.external.client.configuration.SecureClientConfigurationImpl;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

import java.io.IOException;

/**
 * Collect Client Factory class. Provide a rest or a mock version of the client
 */
public class CollectExternalClientFactory extends VitamClientFactory<CollectExternalClient> {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CollectExternalClientFactory.class);
    private static final String CONFIGURATION_FILENAME = "collect-external-client.conf";
    private static final String RESOURCE_PATH = "/collect-external/v1";
    private static final CollectExternalClientFactory COLLECT_CLIENT_FACTORY = new CollectExternalClientFactory();

    protected CollectExternalClientFactory() {
        super(changeConfigurationFile(CONFIGURATION_FILENAME), RESOURCE_PATH, false);
    }

    /**
     * Get the AccessClientFactory instance
     *
     * @return the instance
     */
    public static CollectExternalClientFactory getInstance() {
        return COLLECT_CLIENT_FACTORY;
    }

    /**
     * Change client configuration from a Yaml files
     *
     * @param configurationPath the path to the configuration file
     */
    static ClientConfiguration changeConfigurationFile(String configurationPath) {
        ClientConfiguration configuration = null;
        try {
            configuration = PropertiesUtils.readYaml(
                PropertiesUtils.findFile(configurationPath),
                SecureClientConfigurationImpl.class
            );
        } catch (final IOException fnf) {
            LOGGER.debug("Error when retrieving configuration file {}, using mock", CONFIGURATION_FILENAME, fnf);
        }
        if (configuration == null) {
            LOGGER.error("Error when retrieving configuration file {}, using mock", CONFIGURATION_FILENAME);
        }
        return configuration;
    }

    /**
     * @param configurationFile Generate MOCK vitamClientType for null configuration
     */
    public static void changeMode(String configurationFile) {
        ClientConfiguration configuration = changeConfigurationFile(configurationFile);
        getInstance().initialisation(configuration, getInstance().getResourcePath());
    }

    /**
     * @param configuration Generate MOCK vitamClientType for null configuration
     */
    static void changeMode(ClientConfiguration configuration) {
        getInstance().initialisation(configuration, getInstance().getResourcePath());
    }

    @Override
    public CollectExternalClient getClient() {
        CollectExternalClient client;
        switch (getVitamClientType()) {
            case PRODUCTION:
                client = new CollectExternalClientRest(this);
                break;
            default:
                throw new IllegalArgumentException("Collect client type unknown");
        }
        return client;
    }
}
