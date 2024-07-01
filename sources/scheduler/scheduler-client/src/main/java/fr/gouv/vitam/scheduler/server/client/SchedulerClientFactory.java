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
package fr.gouv.vitam.scheduler.server.client;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.client.configuration.ClientConfiguration;
import fr.gouv.vitam.common.client.configuration.ClientConfigurationImpl;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

import java.io.IOException;

public class SchedulerClientFactory extends VitamClientFactory<SchedulerClient> {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(SchedulerClientFactory.class);
    private static final String CONFIGURATION_FILENAME = "scheduler-client.conf";
    private static final String RESOURCE_PATH = "/scheduler/v1";

    private static final SchedulerClientFactory META_DATA_CLIENT_FACTORY = new SchedulerClientFactory();

    private SchedulerClientFactory() {
        // All requests from client are SMALL, but responses from server could be Huge
        // So Chunked mode inactive on client side
        super(changeConfigurationFile(), RESOURCE_PATH, false);
    }

    /**
     * Change client configuration from a Yaml files
     *
     * @return ClientConfiguration
     */
    static ClientConfiguration changeConfigurationFile() {
        ClientConfiguration configuration = null;
        try {
            configuration = PropertiesUtils.readYaml(
                PropertiesUtils.findFile(SchedulerClientFactory.CONFIGURATION_FILENAME),
                ClientConfigurationImpl.class
            );
        } catch (final IOException fnf) {
            LOGGER.debug(
                "Error when retrieving configuration file {}, using mock",
                SchedulerClientFactory.CONFIGURATION_FILENAME,
                fnf
            );
        }
        if (configuration == null) {
            LOGGER.error(
                "Error when retrieving configuration file {}, using mock",
                SchedulerClientFactory.CONFIGURATION_FILENAME
            );
        }
        return configuration;
    }

    /**
     * @param configuration null for MOCK
     */
    public static void changeMode(ClientConfiguration configuration) {
        getInstance().initialisation(configuration, getInstance().getResourcePath());
    }

    /**
     * Get factory instance
     *
     * @return the factory instance
     */
    public static SchedulerClientFactory getInstance() {
        return META_DATA_CLIENT_FACTORY;
    }

    @Override
    public SchedulerClient getClient() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Actually only one client implementation exists, so ignore client type value");
        }
        SchedulerClient client;
        switch (getVitamClientType()) {
            case MOCK:
                throw new IllegalArgumentException("Not implemented");
            case PRODUCTION:
                client = new SchedulerClientRest(this);
                break;
            default:
                throw new IllegalArgumentException("Scheduler type unknown");
        }
        return client;
    }
}
