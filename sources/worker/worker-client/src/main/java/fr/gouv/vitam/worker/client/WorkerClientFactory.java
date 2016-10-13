/*******************************************************************************
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
 *******************************************************************************/
package fr.gouv.vitam.worker.client;

import java.io.IOException;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

/**
 * <p>
 * WorkerClient factory
 * </p>
 * <p>
 * Use to get a worker client in function of its type.
 *
 * Example :
 * </p>
 * 
 * <pre>
 * {
 *     &#064;code
 *     // Retrieve default worker client
 *     WorkerClient client = WorkerClientFactory.getInstance().getWorkerClient();
 * 
 *     // Exists
 *     client.exists(asyncId);
 * }
 * </pre>
 *
 * You can change the type of the client to get. The types are define into the enum {@link WorkerClientType}. Use the
 * changeDefaultClientType method to change the client type.
 *
 */
public class WorkerClientFactory {

    private static WorkerClientType defaultWorkerClientType;
    private static final String CONFIGURATION_FILENAME = "worker-client.conf";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WorkerClientFactory.class);
    private static final WorkerClientFactory WORKER_CLIENT_FACTORY = new WorkerClientFactory();

    private WorkerClientConfiguration clientConfiguration = null;

    private WorkerClientFactory() {
        changeConfigurationFile(CONFIGURATION_FILENAME);
    }

    /**
     * Set the WorkerClientFactory configuration
     *
     * @param configuration the configuration to us
     * @throws IllegalArgumentException if server is null or empty or port is less than or equal to 0
     */
    static final void setConfiguration(WorkerClientConfiguration configuration) {
        checkConfiguration(configuration);
        WORKER_CLIENT_FACTORY.clientConfiguration = configuration;
    }

    /**
     * Set the WorkerClientFactory configuration
     *
     * @param type the worker type
     * @param configuration the client configuration
     * @throws IllegalArgumentException if type null or if type is WORKER and server is null or empty or port is less than or equal to 0
     */
    public static final void setConfiguration(WorkerClientType type, WorkerClientConfiguration configuration) {
        changeDefaultClientType(type);
        if (type == WorkerClientType.WORKER) {
            checkConfiguration(configuration);
        }
        WORKER_CLIENT_FACTORY.clientConfiguration = configuration;
    }

    /**
     * Get the WorkerClientFactory instance
     *
     * @return the instance
     */
    public static final WorkerClientFactory getInstance() {
        return WORKER_CLIENT_FACTORY;
    }

    /**
     * Get the default worker client
     *
     * @return the default worker client
     */
    public WorkerClient getWorkerClient() {
        WorkerClient client;
        switch (defaultWorkerClientType) {
            case MOCK_WORKER:
                client = new WorkerClientMock();
                break;
            case WORKER:
                client = new WorkerClientRest(clientConfiguration, WorkerClient.RESOURCE_PATH, true);
                break;
            default:
                throw new IllegalArgumentException("Worker client type unknown");
        }
        return client;
    }

    /**
     * Get the default worker client type
     *
     * @return the default worker client type
     */
    public static WorkerClientType getDefaultWorkerClientType() {
        return defaultWorkerClientType;
    }

    /**
     * Change client configuration from a Yaml files
     *
     * @param configurationPath the path to the configuration file
     */
    public final void changeConfigurationFile(String configurationPath) {
        changeDefaultClientType(WorkerClientType.MOCK_WORKER);
        WorkerClientConfiguration configuration = null;
        try {
            configuration = PropertiesUtils.readYaml(PropertiesUtils.findFile(configurationPath),
                WorkerClientConfiguration.class);
        } catch (final IOException fnf) {
            LOGGER
                .warn("Error when retrieving configuration file {}, using mock",
                    CONFIGURATION_FILENAME,
                    fnf);
        }
        if (configuration == null) {
            this.clientConfiguration = null;
            LOGGER.warn("Error when retrieving configuration file {}, using mock",
                CONFIGURATION_FILENAME);
        } else {
            checkConfiguration(configuration);
            this.clientConfiguration = configuration;
            changeDefaultClientType(WorkerClientType.WORKER);
        }
    }

    private static void checkConfiguration(WorkerClientConfiguration configuration) {
        ParametersChecker.checkParameter("Configuration cannot be null", configuration);
        ParametersChecker.checkParameter("Server cannot be null or empty", configuration.getServerHost());
        ParametersChecker.checkValue("Server port cannot be null", configuration.getServerPort(), 1);
    }

    /**
     * Modify the default worker client type
     *
     * @param type the client type to set
     * @throws IllegalArgumentException if type null
     */
    static void changeDefaultClientType(WorkerClientType type) {
        if (type == null) {
            throw new IllegalArgumentException();
        }
        defaultWorkerClientType = type;
    }

    /**
     * enum to define client type
     */
    public enum WorkerClientType {
        /**
         * To use only in MOCK
         */
        MOCK_WORKER,
        /**
         * Use real service
         */
        WORKER
    }

}
