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

package the.driver;

import java.util.Map;
import java.util.Properties;

import javax.ws.rs.client.Client;

import fr.gouv.vitam.common.client.MockOrRestClient;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.client.VitamRestEasyConfiguration;
import fr.gouv.vitam.common.client.configuration.ClientConfiguration;
import fr.gouv.vitam.storage.driver.AbstractDriver;
import fr.gouv.vitam.storage.driver.Connection;
import fr.gouv.vitam.storage.driver.exception.StorageDriverException;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageOffer;

/**
 * Implementation of driver
 */
public class TheDriver extends AbstractDriver {

    /**
     * Is Storage Offer Available
     * 
     * @param s
     * @param properties
     * @return false
     * @throws StorageDriverException
     */
    public boolean isStorageOfferAvailable(String s, Properties properties) throws StorageDriverException {
        return false;
    }

    public String getName() {
        return null;
    }

    public int getMajorVersion() {
        return 0;
    }

    public int getMinorVersion() {
        return 0;
    }

    public Connection connect(String offerId) throws StorageDriverException {
        return null;
    }

    @Override
    protected VitamClientFactoryInterface addInternalOfferAsFactory(final StorageOffer offer,
        final Properties parameters) {
        VitamClientFactoryInterface factory = new VitamClientFactoryInterface() {

            public Client getHttpClient() {
                return null;
            }

            public Client getHttpClient(boolean useChunkedMode) {
                return null;
            }

            public MockOrRestClient getClient() {
                return null;
            }

            public String getResourcePath() {
                return null;
            }

            public String getServiceUrl() {
                return null;
            }

            public Map<VitamRestEasyConfiguration, Object> getDefaultConfigCient() {
                return null;
            }

            public Map<VitamRestEasyConfiguration, Object> getDefaultConfigCient(boolean chunkedMode) {
                return null;
            }

            public ClientConfiguration getClientConfiguration() {
                return null;
            }

            public VitamClientType getVitamClientType() {
                return null;
            }

            public VitamClientFactoryInterface setVitamClientType(VitamClientType vitamClientType) {
                return null;
            }

            public void changeResourcePath(String resourcePath) {}

            public void changeServerPort(int port) {}

            public void shutdown() {}

            @Override
            public void resume(Client client, boolean chunk) {
                // Empty
            }
        };
        return factory;
    }
}
