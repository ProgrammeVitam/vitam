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

package fr.gouv.vitam.antivirus.client;

import fr.gouv.vitam.antivirus.client.invoker.ApiClient;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.client.configuration.ClientConfiguration;
import fr.gouv.vitam.common.client.configuration.SecureClientConfiguration;
import fr.gouv.vitam.common.client.configuration.SecureClientConfigurationImpl;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import jakarta.ws.rs.client.Client;

import java.io.IOException;

/**
 * Factory to get antivirus client instances.
 */
public class AntivirusClientFactory {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AntivirusClientFactory.class);
    private static final AntivirusClientFactory ANTIVIRUS_CLIENT_FACTORY = new AntivirusClientFactory();
    private static final String CONFIGURATION_FILENAME = "antivirus-client.conf";

    protected ClientConfiguration clientConfiguration;
    protected ApiClient apiClient;

    public AntivirusClientFactory() {
        this(changeConfigurationFile(CONFIGURATION_FILENAME));
    }

    public AntivirusClientFactory(ClientConfiguration configuration) {
        this.initialisation(configuration);
    }

    void initialisation(ClientConfiguration configuration) {
        this.clientConfiguration = configuration;
        if (configuration == null) {
            return;
        }
        // Check configuration
        ParametersChecker.checkParameter("Host cannot be null", configuration.getServerHost());
        ParametersChecker.checkValue("Port has invalid value", configuration.getServerPort(), 1);
        Client client = VitamOpenAPIClientFactoryUtils.buildHttpClient(configuration);
        // Build the api client from the http client
        this.apiClient = new VitamApiClient();
        this.apiClient.setHttpClient(client);
        this.apiClient.setBasePath(
                (configuration.isSecure() ? "https" : "http") +
                "://" +
                configuration.getServerHost() +
                ":" +
                configuration.getServerPort()
            );
    }

    /**
     * Get the Antivirus client
     */
    public AntivirusApi getAntivirusApi() {
        return new AntivirusApi(apiClient);
    }

    static SecureClientConfiguration changeConfigurationFile(String configurationPath) {
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
     * Get the AntivirusClientFactory instance
     *
     * @return the instance
     */
    public static AntivirusClientFactory getInstance() {
        return ANTIVIRUS_CLIENT_FACTORY;
    }

    public static void changeMode(SecureClientConfiguration configuration) {
        getInstance().initialisation(configuration);
    }

    public static void changeMode(String configurationFile) {
        SecureClientConfiguration configuration = changeConfigurationFile(configurationFile);
        getInstance().initialisation(configuration);
    }

    public void changeServerPort(int port) {
        this.initialisation(clientConfiguration.setServerPort(port));
    }

    /**
     * Get the Api Client (for tests)
     */
    protected ApiClient getApiClient() {
        return apiClient;
    }
}
