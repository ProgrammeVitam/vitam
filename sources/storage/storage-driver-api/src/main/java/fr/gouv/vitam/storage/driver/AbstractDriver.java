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
package fr.gouv.vitam.storage.driver;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.storage.driver.exception.StorageDriverException;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageOffer;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class must be the reference to create new drivers implementation compatible with vitam
 */
public abstract class AbstractDriver implements Driver {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AbstractDriver.class);

    protected final Map<String, VitamClientFactoryInterface<? extends AbstractConnection>> connectionFactories =
        new ConcurrentHashMap<>();

    @Override
    public boolean isStorageOfferAvailable(String offerId) throws StorageDriverException {
        try {
            ParametersChecker.checkParameter("StorageId Cannot be null", offerId);
        } catch (IllegalArgumentException e) {
            LOGGER.info(e);
            return false;
        }

        if (connectionFactories.containsKey(offerId)) {
            try {
                final VitamClientFactoryInterface<? extends AbstractConnection> factory = connectionFactories.get(
                    offerId
                );
                try (final AbstractConnection connection = factory.getClient()) {
                    connection.checkStatus();
                }
                LOGGER.debug("Check status ok");
                return true;
            } catch (final VitamApplicationServerException exception) {
                LOGGER.error("Service unavailable for Driver {} with Offer {}", getName(), offerId, exception);
                return false;
            }
        }
        LOGGER.error("Driver {} has no Offer named {}", getName(), offerId);
        return false;
    }

    @Override
    public final boolean addOffer(StorageOffer offer, Properties parameters) {
        if (!connectionFactories.containsKey(offer.getId())) {
            connectionFactories.put(offer.getId(), addInternalOfferAsFactory(offer, parameters));
        }
        return false;
    }

    /**
     * This method must be implemented in the final Driver Implementation to add the ClientFactory to the driver
     *
     * @param offer
     * @param parameters
     * @return true if added
     */
    protected abstract VitamClientFactoryInterface<? extends AbstractConnection> addInternalOfferAsFactory(
        StorageOffer offer,
        Properties parameters
    );

    @Override
    public final boolean removeOffer(String offer) {
        if (connectionFactories.containsKey(offer)) {
            final VitamClientFactoryInterface<? extends AbstractConnection> factory = connectionFactories.remove(offer);
            factory.shutdown();
            return true;
        }
        LOGGER.error("Driver {} has no Offer named {}", this.getName(), offer);
        return false;
    }

    @Override
    public final boolean hasOffer(String offerId) {
        return connectionFactories.containsKey(offerId);
    }

    @Override
    public void close() {
        for (Map.Entry<
            String,
            VitamClientFactoryInterface<? extends AbstractConnection>
        > item : connectionFactories.entrySet()) {
            item.getValue().shutdown();
        }
        connectionFactories.clear();
    }
}
