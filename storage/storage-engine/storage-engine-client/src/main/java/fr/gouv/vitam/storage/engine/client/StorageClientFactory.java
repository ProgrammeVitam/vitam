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
package fr.gouv.vitam.storage.engine.client;

import java.io.IOException;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

/**
 * <p>
 * StorageClient factory
 * </p>
 * <p>
 * Use to get a storage client in function of its type.
 *
 * Example :
 * </p>
 * 
 * <pre>
 * {
 *     &#64;code
 *     // Retrieve default storage client
 *     StorageClient client = StorageClientFactory.getInstance().getStorageClient();
 *
 *     // Exists
 *     client.exists(tenantId, strategyId);
 * }
 * </pre>
 *
 * You can change the type of the client to get. The types are define into the enum {@link StorageClientType}. Use the
 * changeDefaultClientType method to change the client type.
 *
 */
public class StorageClientFactory {

    private static StorageClientType defaultStorageClientType;
    private static final String CONFIGURATION_FILENAME = "storage-client.conf";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(StorageClientFactory.class);
    private static final StorageClientFactory STORAGE_CLIENT_FACTORY = new StorageClientFactory();

    private StorageClientConfiguration clientConfiguration = null;

    private StorageClientFactory() {
        changeConfigurationFile(CONFIGURATION_FILENAME);
    }

    /**
     * Set the StorageClientFactory configuration
     *
     * @param configuration the configuration to us
     * @throws IllegalArgumentException if server is null or empty or port <= 0
     */
    static void setConfiguration(StorageClientConfiguration configuration) {
        checkConfiguration(configuration);
        STORAGE_CLIENT_FACTORY.clientConfiguration = configuration;
    }

    /**
     * Set the StorageClientFactory configuration
     *
     * @param type the storage type
     * @param configuration the client configuration
     * @throws IllegalArgumentException if type null or if type is STORAGE and server is null or empty or port <= 0
     */
    static void setConfiguration(StorageClientType type, StorageClientConfiguration configuration) {
        changeDefaultClientType(type);
        if (type == StorageClientType.STORAGE) {
            checkConfiguration(configuration);
        }
        STORAGE_CLIENT_FACTORY.clientConfiguration = configuration;
    }

    /**
     * Get the StorageClientFactory instance
     *
     * @return the instance
     */
    public static StorageClientFactory getInstance() {
        return STORAGE_CLIENT_FACTORY;
    }

    /**
     * Get the default storage client
     *
     * @return the default storage client
     */
    public StorageClient getStorageClient() {
        StorageClient client;
        switch (defaultStorageClientType) {
            case MOCK_STORAGE:
                client = new StorageClientMock();
                break;
            case STORAGE:
                client = new StorageClientRest(clientConfiguration, StorageClient.RESOURCE_PATH, true);
                break;
            default:
                throw new IllegalArgumentException(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_CLIENT_UNKNOWN));
        }
        return client;
    }

    /**
     * Get the default storage client type
     *
     * @return the default storage client type
     */
    public static StorageClientType getDefaultStorageClientType() {
        return defaultStorageClientType;
    }

    /**
     * Change client configuration from a Yaml files
     *
     * @param configurationPath the path to the configuration file
     */
    public final void changeConfigurationFile(String configurationPath) {
        changeDefaultClientType(StorageClientType.MOCK_STORAGE);
        StorageClientConfiguration configuration = null;
        try {
            configuration = PropertiesUtils.readYaml(PropertiesUtils.findFile(configurationPath),
                StorageClientConfiguration.class);
        } catch (final IOException fnf) {
                LOGGER
                    .warn(String.format("Error when retrieving configuration file %s, using mock",
                        CONFIGURATION_FILENAME),
                        fnf);
        }
        if (configuration == null) {
            this.clientConfiguration = null;
            LOGGER.warn(String.format("Error when retrieving configuration file %s, using mock",
                CONFIGURATION_FILENAME));
        } else {
            checkConfiguration(configuration);
            this.clientConfiguration = configuration;
            changeDefaultClientType(StorageClientType.STORAGE);
        }
    }

    private static void checkConfiguration(StorageClientConfiguration configuration) {
        ParametersChecker.checkParameter("Configuration cannot be null", configuration);
        ParametersChecker.checkParameter("Server cannot be null or empty", configuration.getServerHost());
        ParametersChecker.checkValue("Server port cannot be null", configuration.getServerPort(), 1);
        ParametersChecker.checkParameter("Server context path cannot be null",
            configuration.getServerContextPath());
    }

    /**
     * Modify the default storage client type
     *
     * @param type the client type to set
     * @throws IllegalArgumentException if type null
     */
    static void changeDefaultClientType(StorageClientType type) {
        if (type == null) {
            throw new IllegalArgumentException();
        }
        defaultStorageClientType = type;
    }

    /**
     * enum to define client type
     */
    public enum StorageClientType {
        /**
         * To use only in MOCK
         */
        MOCK_STORAGE,
        /**
         * Use real service
         */
        STORAGE
    }
}
