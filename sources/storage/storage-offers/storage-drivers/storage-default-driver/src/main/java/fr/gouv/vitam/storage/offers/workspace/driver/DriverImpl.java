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
import java.util.Properties;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client.configuration.ClientConfiguration;
import fr.gouv.vitam.common.client2.VitamClientFactory;
import fr.gouv.vitam.common.client2.configuration.ClientConfigurationImpl;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.storage.driver.Connection;
import fr.gouv.vitam.storage.driver.Driver;
import fr.gouv.vitam.storage.driver.exception.StorageDriverException;

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
            super(configuration, resourcePath);
            this.parameters = parameters;
        }

        @Override
        public ConnectionImpl getClient() {
            return new ConnectionImpl(this, parameters);
        }
        

        @Override
        public boolean isStorageOfferAvailable(String url, Properties parameters) throws StorageDriverException {
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
        public Connection connect(String url, Properties parameters) throws StorageDriverException {
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
    public ConnectionImpl connect(String url, Properties parameters) throws StorageDriverException {
        final InternalDriverFactory factory = new InternalDriverFactory(changeConfigurationUrl(url), RESOURCE_PATH, parameters);
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
     * For compatibility with old implementation
     * 
     * @param urlString
     */
    private static final ClientConfigurationImpl changeConfigurationUrl(String urlString) {
        ParametersChecker.checkParameter("URI is mandatory", urlString);
        try {
            URI url = new URI(urlString);
            LOGGER.info("Change configuration using " + url.getHost() + ":" + url.getPort());
            return new ClientConfigurationImpl(url.getHost(), url.getPort());
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Cannot parse the URI: " + urlString, e);
        }
    }

    @Override
    public boolean isStorageOfferAvailable(String url, Properties parameters) throws StorageDriverException {
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
