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
package fr.gouv.vitam.worker.client;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * WorkerClient factory
 * </p>
 * <p>
 * Used to get a worker client depending on its type.
 *
 * Example :
 * </p>
 *
 * <pre>
 * {
 *     &#064;code
 *     // Retrieves a  worker client given a configuration
 *       WorkerClientConfiguration configuration1 = new  WorkerClientConfiguration("localhost",8076);
 *     WorkerClient client = WorkerClientFactory.getInstance(configuration1).getWorkerClient();
 *
 *     // Exists
 *     client.exists(asyncId);
 * }
 * </pre>
 *
 * You can change the type of the client to get. The types are defined in the enum {@link WorkerClient}. Use the
 * changeDefaultClientType method to change the client type.
 */

public class WorkerClientFactory extends VitamClientFactory<WorkerClient> {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WorkerClientFactory.class);
    private static final String CONFIGURATION_FILENAME = "worker-client.conf";

    private static final Map<WorkerClientConfiguration, WorkerClientFactory> workersSetFactory =
        new ConcurrentHashMap<>();
    private static final WorkerClientFactory defaultWorkerClientFactory = new WorkerClientFactory(null);
    /**
     * RESOURCE PATH
     */
    public static final String RESOURCE_PATH = "/worker/v1";

    /**
     * Get the WorkerClientFactory instance given a configuration
     *
     * @param configuration the worker client configuration
     */
    private WorkerClientFactory(WorkerClientConfiguration configuration) {
        super(configuration, RESOURCE_PATH, false);
    }

    /**
     * get Specifique workerfactory instance
     *
     * @param configuration the worker client configuration
     * @return an instance of WorkerClientFactory
     */
    public static WorkerClientFactory getInstance(WorkerClientConfiguration configuration) {
        if (configuration == null) {
            return defaultWorkerClientFactory;
        }
        workersSetFactory.computeIfAbsent(configuration, k -> new WorkerClientFactory(configuration));
        return workersSetFactory.get(configuration);
    }

    /**
     * Get the default worker client
     *
     * @return the default worker client
     */
    @Override
    public WorkerClient getClient() {
        WorkerClient client;
        switch (getVitamClientType()) {
            case MOCK:
                client = new WorkerClientMock();
                break;
            case PRODUCTION:
                client = new WorkerClientRest(this);
                break;
            default:
                throw new IllegalArgumentException("Worker client type unknown");
        }
        return client;
    }

    /**
     * Change client configuration from a Yaml files
     *
     * @param configurationPath the path to the configuration file
     * @return un instance of WorkerClientConfiguration
     */
    public static final WorkerClientConfiguration changeConfigurationFile(String configurationPath) {
        WorkerClientConfiguration configuration = null;
        try {
            configuration = PropertiesUtils.readYaml(
                PropertiesUtils.findFile(configurationPath),
                WorkerClientConfiguration.class
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
     * @param configuration null for MOCK
     */
    public static final void changeMode(WorkerClientConfiguration configuration) {
        getInstance(configuration).initialisation(configuration, getInstance(configuration).getResourcePath());
    }
}
