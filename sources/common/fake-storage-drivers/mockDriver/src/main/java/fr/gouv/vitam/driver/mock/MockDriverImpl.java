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
package fr.gouv.vitam.driver.mock;

import fr.gouv.vitam.common.client.BasicClient;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.client.VitamRestEasyConfiguration;
import fr.gouv.vitam.common.client.configuration.ClientConfiguration;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.storage.driver.AbstractDriver;
import fr.gouv.vitam.storage.driver.Connection;
import fr.gouv.vitam.storage.driver.exception.StorageDriverException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageOffer;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.client.Client;

/**
 * Driver implementation for test only
 */
public class MockDriverImpl extends AbstractDriver {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MockDriverImpl.class);
    private static final String MOCK_DRIVER = "MockDriver";
    private static final Map<String, StorageOffer> STORAGE_OFFERS = new ConcurrentHashMap<>();
    
    static class MockClientFactory implements VitamClientFactoryInterface<ConnectionMockImpl> {
        final Properties parameters;
        /**
         * @param parameters
         */
        public MockClientFactory(Properties parameters) {
            this.parameters = parameters;
        }

        @Override
        public Client getHttpClient() {
            return null;
        }

        @Override
        public Client getHttpClient(boolean useChunkedMode) {
            return null;
        }

        @Override
        public ConnectionMockImpl getClient() {
            return new ConnectionMockImpl(MOCK_DRIVER, this, parameters);
        }

        @Override
        public String getResourcePath() {
            return null;
        }

        @Override
        public String getServiceUrl() {
            return null;
        }

        @Override
        public Map<VitamRestEasyConfiguration, Object> getDefaultConfigCient() {
            return null;
        }

        @Override
        public Map<VitamRestEasyConfiguration, Object> getDefaultConfigCient(boolean chunkedMode) {
            return null;
        }

        @Override
        public ClientConfiguration getClientConfiguration() {
            return null;
        }

        @Override
        public VitamClientType getVitamClientType() {
            return VitamClientType.MOCK;
        }

        @Override
        public VitamClientFactoryInterface<?> setVitamClientType(
            VitamClientType vitamClientType) {
            return this;
        }

        @Override
        public void changeResourcePath(String resourcePath) {
            // Empty
        }

        @Override
        public void changeServerPort(int port) {
            // Empty
        }

        @Override
        public void shutdown() {
            // Empty
        }

        @Override
        public void resume(Client client, boolean chunk) {
            // Empty
        }

    }
    
    @Override
    protected VitamClientFactoryInterface addInternalOfferAsFactory(final StorageOffer offer,
        final Properties parameters) {
        STORAGE_OFFERS.put(offer.getId(), offer);
        
        return new MockClientFactory(parameters);
    }

    @Override
    public Connection connect(String offerId) throws StorageDriverException {
        if (offerId.contains("fail")) {
            throw new StorageDriverException(getName(),
                "Intentionaly thrown");
        }
        if (connectionFactories.containsKey(offerId)) {
            final VitamClientFactoryInterface<? extends BasicClient> factory = connectionFactories.get(offerId);
            return (Connection) factory.getClient();
        }
        LOGGER.error("Driver {} has no Offer named {}", getName(), offerId);
        StorageNotFoundException exception =
            new StorageNotFoundException("Driver " + getName() + " has no Offer named " + offerId);
        throw new StorageDriverException("Driver " + getName() + " with Offer " + offerId,
            exception.getMessage(), exception);
    }

    @Override
    public boolean isStorageOfferAvailable(String offerId) throws StorageDriverException {
        if (offerId.contains("fail")) {
            throw new StorageDriverException(getName(),
                "Intentionaly thrown");
        }
        return true;
    }

    @Override
    public String getName() {
        return MOCK_DRIVER;
    }

    @Override
    public int getMajorVersion() {
        return 1;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }
}
