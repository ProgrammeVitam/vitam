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
package fr.gouv.vitam.storage.offers.workspace.driver;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.VitamAutoCloseable;
import fr.gouv.vitam.storage.driver.Connection;
import fr.gouv.vitam.storage.driver.Driver;
import fr.gouv.vitam.storage.driver.exception.StorageDriverException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageOffer;

/**
 * Generic Driver implementation
 */
public class DriverImplNew implements Driver, VitamAutoCloseable {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DriverImplNew.class);


    private final String driverName;
    /**
     * All offers attached to this driver (dynamically allocated through connect)
     */
    private final Map<String, VitamClientFactory<ConnectionImpl>> connectionFactories = new ConcurrentHashMap<>();

    /**
     * Constructor using the driverName
     * 
     * @param driverName the name of the driver, which could be associated with many offers
     * @throws IllegalArgumentException if the driver already exists
     */
    protected DriverImplNew(String driverName) {
        this.driverName = driverName;
    }

    @Override
    public Connection connect(StorageOffer offer, Properties parameters) throws StorageDriverException {
        if (connectionFactories.containsKey(offer)) {
            try {
                final VitamClientFactory<ConnectionImpl> factory = connectionFactories.get(offer.getId());
                final ConnectionImpl connection = factory.getClient();
                connection.checkStatus();
                LOGGER.debug("Check status ok");
                return connection;
            } catch (final VitamApplicationServerException exception) {
                LOGGER.error("Service unavailable for Driver {} with Offer {}", driverName, offer.getId(), exception);
                throw new StorageDriverException("Driver " + driverName + " with Offer " + offer.getId(),
                    exception.getMessage(), exception);
            }
        }
        LOGGER.error("Driver {} has no Offer named {}", driverName, offer.getId());
        StorageNotFoundException exception =
            new StorageNotFoundException("Driver " + driverName + " has no Offer named " + offer.getId());
        throw new StorageDriverException("Driver " + driverName + " with Offer " + offer.getId(),
            exception.getMessage(), exception);
    }

    @Override
    public boolean isStorageOfferAvailable(String offer, Properties parameters) throws StorageDriverException {
        if (connectionFactories.containsKey(offer)) {
            try {
                final VitamClientFactory<ConnectionImpl> factory = connectionFactories.get(offer);
                final ConnectionImpl connection = factory.getClient();
                connection.checkStatus();
                LOGGER.debug("Check status ok");
                return true;
            } catch (final VitamApplicationServerException exception) {
                LOGGER.error("Service unavailable for Driver {} with Offer {}", driverName, offer, exception);
                return false;
            }
        }
        LOGGER.error("Driver {} has no Offer named {}", driverName, offer);
        return false;
    }

    /**
     * Remove one offer from the Driver (from DriverManager)
     * 
     * @param offer
     * @return True if the offer was removed, false if not existing
     */
    public boolean removeOffer(String offer) {
        if (connectionFactories.containsKey(offer)) {
            final VitamClientFactory<ConnectionImpl> factory = connectionFactories.remove(offer);
            factory.shutdown();
            return true;
        }
        LOGGER.error("Driver {} has no Offer named {}", driverName, offer);
        return false;
    }

    @Override
    public String getName() {
        return driverName;
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
    public void close() {
        for (Entry<String, VitamClientFactory<ConnectionImpl>> item : connectionFactories.entrySet()) {
            item.getValue().shutdown();
        }
        connectionFactories.clear();
    }

}
