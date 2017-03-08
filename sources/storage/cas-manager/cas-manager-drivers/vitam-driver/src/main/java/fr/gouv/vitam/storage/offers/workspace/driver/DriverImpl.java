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
package fr.gouv.vitam.storage.offers.workspace.driver;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.client.configuration.ClientConfiguration;
import fr.gouv.vitam.common.client.configuration.ClientConfigurationImpl;
import fr.gouv.vitam.common.client.configuration.SSLConfiguration;
import fr.gouv.vitam.common.client.configuration.SSLKey;
import fr.gouv.vitam.common.client.configuration.SecureClientConfigurationImpl;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.storage.driver.Connection;
import fr.gouv.vitam.storage.driver.Driver;
import fr.gouv.vitam.storage.driver.exception.StorageDriverException;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageOffer;

/**
 * Workspace Driver Implementation
 */
public class DriverImpl implements Driver {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DriverImpl.class);

    private static final String DRIVER_NAME = "WorkspaceDriver";
    private static final String RESOURCE_PATH = "/offer/v1";

    private static final DriverImpl DRIVER_IMPL = new DriverImpl();

    static class InternalDriverFactory extends VitamClientFactory<ConnectionImpl> implements Driver {
        final Properties parameters;

        protected InternalDriverFactory(ClientConfiguration configuration, String resourcePath, Properties parameters) {
            super(configuration, resourcePath, true, false, true, true);
            enableUseAuthorizationFilter();
            this.parameters = parameters;
        }

        @Override
        public ConnectionImpl getClient() {
            return new ConnectionImpl(this, parameters);
        }


        @Override
        public boolean isStorageOfferAvailable(String configurationPath, Properties parameters) throws StorageDriverException {
            return true;
        }

        @Override
        public String getName() {
            return DRIVER_NAME;
        }

        @Override
        public int getMajorVersion() {
            return 0;
        }

        @Override
        public int getMinorVersion() {
            return 0;
        }

        @Override
        public Connection connect(StorageOffer offer, Properties parameters) throws StorageDriverException {
            throw new UnsupportedOperationException("The internal factory does not support this method");
        }

    }

    /**
     * Constructor
     */
    public DriverImpl() {
        // Empty
    }

    /**
     * Get the ProcessingManagementClientFactory instance
     *
     * @return the instance
     */
    public static final DriverImpl getInstance() {
        return DRIVER_IMPL;
    }

    @Override
    public ConnectionImpl connect(StorageOffer offer, Properties parameters) throws StorageDriverException {
        final InternalDriverFactory factory =
            new InternalDriverFactory(changeConfigurationFile(offer), RESOURCE_PATH, parameters);
        try {
            final ConnectionImpl connection = factory.getClient();
            connection.checkStatus();
            return connection;
        } catch (final VitamApplicationServerException exception) {
            throw new StorageDriverException(DRIVER_NAME, StorageDriverException.ErrorCode.INTERNAL_SERVER_ERROR,
                exception.getMessage(), exception);
        }
    }

    
    /**
     * Change client configuration from a Yaml files
     *
     * @param configurationPath the path to the configuration file
     * @return ClientConfiguration
     */
    static final ClientConfiguration changeConfigurationFile(StorageOffer offer) {
        ClientConfiguration configuration = null;
        ParametersChecker.checkParameter("StorageOffer cannot be null", offer);
        try {
            final URI url = new URI(offer.getBaseUrl());
            
            Map<String, String> param = offer.getParameters();
            
            if (param != null){
                List<SSLKey> keystoreList = new ArrayList<>();
                List<SSLKey> truststoreList = new ArrayList<>();
                keystoreList.add(new SSLKey(param.get("keyStore-keyPath"), param.get("keyStore-keyPassword")));
                truststoreList.add(new SSLKey(param.get("trustStore-keyPath"), param.get("trustStore-keyPassword")));
                
                configuration = new SecureClientConfigurationImpl(url.getHost(), url.getPort(),
                    true, new SSLConfiguration(keystoreList, truststoreList));
            } else {
                configuration = new ClientConfigurationImpl(url.getHost(), url.getPort());
            }
            
        } catch (final URISyntaxException e) {
            throw new IllegalStateException("Cannot parse the URI: ", e);
        }
        return configuration;
    }

    @Override
    public boolean isStorageOfferAvailable(String configurationPath, Properties parameters) throws StorageDriverException {
        return true;
    }

    @Override
    public String getName() {
        return DRIVER_NAME;
    }

    @Override
    public int getMajorVersion() {
        return 0;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

}
