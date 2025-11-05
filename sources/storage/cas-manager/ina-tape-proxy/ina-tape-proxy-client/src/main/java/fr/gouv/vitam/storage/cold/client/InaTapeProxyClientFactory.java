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
package fr.gouv.vitam.storage.cold.client;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.client.VitamResteasyClientFactory;
import fr.gouv.vitam.common.client.configuration.ClientConfiguration;
import fr.gouv.vitam.common.client.configuration.SecureClientConfiguration;
import fr.gouv.vitam.common.client.configuration.SecureClientConfigurationImpl;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import jakarta.ws.rs.client.Client;

import java.io.IOException;

/**
 * Factory to get Cold Storage client instances.
 */
public class InaTapeProxyClientFactory {

    private static final String CONFIGURATION_FILENAME = "ina-tape-proxy-client.conf";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(InaTapeProxyClientFactory.class);
    private static final InaTapeProxyClientFactory INSTANCE = new InaTapeProxyClientFactory();

    protected ClientConfiguration clientConfiguration;
    protected ConfiguredApiClient apiClient;

    // Default constructor: uses the default configuration file
    public InaTapeProxyClientFactory() {
        this(loadConfigurationFromFile(CONFIGURATION_FILENAME));
    }

    // Constructor with explicit configuration
    public InaTapeProxyClientFactory(ClientConfiguration configuration) {
        initialize(configuration);
    }

    // Initialization logic: validate config, build HTTP client, create API client
    void initialize(ClientConfiguration configuration) {
        this.clientConfiguration = configuration;
        if (configuration == null) return;

        // Validate mandatory parameters
        ParametersChecker.checkParameter("Host cannot be null", configuration.getServerHost());
        ParametersChecker.checkValue("Port has invalid value", configuration.getServerPort(), 1);

        // Build the HTTP client using fr.gouv.vitam.storage.cold.client.VitamClientBuilder
        Client httpClient = new VitamResteasyClientFactory().buildClient(configuration);

        // Create the ColdStorageClient and set base path
        this.apiClient = new ConfiguredApiClient();
        this.apiClient.setHttpClient(httpClient);
        this.apiClient.setBasePath(
                (configuration.isSecure() ? "https" : "http") +
                "://" +
                configuration.getServerHost() +
                ":" +
                configuration.getServerPort()
            );
    }

    /**
     * Get the ColdStorage API client
     */
    public fr.gouv.vitam.storage.cold.client.InaTapeProxyApi getInaTapeProxyClient() {
        return new fr.gouv.vitam.storage.cold.client.InaTapeProxyApi(apiClient);
    }

    /**
     * Load configuration from YAML file
     */
    static SecureClientConfiguration loadConfigurationFromFile(String configurationPath) {
        SecureClientConfiguration configuration = null;
        try {
            configuration = PropertiesUtils.readYaml(
                PropertiesUtils.findFile(configurationPath),
                SecureClientConfigurationImpl.class
            );
        } catch (IOException e) {
            LOGGER.error("Error while loading configuration file {}", configurationPath, e);
        }
        if (configuration == null) {
            LOGGER.error("Error while loading configuration file {}", configurationPath);
        }
        return configuration;
    }

    /**
     * Get singleton instance of fr.gouv.vitam.storage.cold.client.ColdStorageClientFactory
     */
    public static InaTapeProxyClientFactory getInstance() {
        return INSTANCE;
    }

    /**
     * Change configuration at runtime
     */
    public static void changeMode(SecureClientConfiguration configuration) {
        getInstance().initialize(configuration);
    }

    public static void changeMode(String configurationFile) {
        SecureClientConfiguration configuration = loadConfigurationFromFile(configurationFile);
        getInstance().initialize(configuration);
    }

    /**
     * Change server port dynamically
     */
    public void changeServerPort(int port) {
        this.initialize(clientConfiguration.setServerPort(port));
    }

    /**
     * Get the underlying API client (for testing)
     */
    protected ConfiguredApiClient getApiClient() {
        return apiClient;
    }
}
