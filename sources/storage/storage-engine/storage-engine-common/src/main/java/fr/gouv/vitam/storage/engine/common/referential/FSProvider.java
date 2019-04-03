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

package fr.gouv.vitam.storage.engine.common.referential;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.exception.StorageTechnicalException;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageOffer;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageStrategy;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * File system implementation of the storage strategy and storage offer provider
 */
class FSProvider implements StorageStrategyProvider, StorageOfferProvider, StorageOfferHACapabilityProvider {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(FSProvider.class);
    private static final String STRATEGY_FILENAME = "static-strategy.json";
    private static final String OFFER_FILENAME = "static-offer.json";
    private StorageStrategy storageStrategy;

    private volatile Map<String, StorageOffer> storageOffers;

    /**
     * Package protected to avoid out of scope instance creation
     */
    FSProvider() {
        initReferentials();
    }

    /**
     * Convenient enum to describe the different kind of referential handled by
     * thi provider
     */
    private enum ReferentialType {
        STRATEGY, OFFER
    }

    @Override
    public StorageStrategy getStorageStrategy(String idStrategy) throws StorageTechnicalException {
        // TODO P1 : only 1 strategy for now, need to use this id in later
        // implementation
        if (storageStrategy != null) {
            return storageStrategy;
        }
        try {
            loadReferential(ReferentialType.STRATEGY);
        } catch (IOException | InvalidParseOperationException exc) {
            throw new StorageTechnicalException(exc);
        }
        return storageStrategy;

    }

    @Override
    public StorageOffer getStorageOffer(String idOffer) throws StorageException {
        return getFilteredStorageOffer(idOffer, false);//get only active one
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StorageOffer getStorageOfferForHA(String idOffer, boolean includeDisabled)
        throws StorageException {
        return getFilteredStorageOffer(idOffer, includeDisabled);//get all (active and inactive)
    }

    private StorageOffer getFilteredStorageOffer(String idOffer, boolean includeAllOfferState) throws StorageException {
        if (storageOffers == null) {
            try {
                loadReferential(ReferentialType.OFFER);
            } catch (IOException | InvalidParseOperationException exc) {
                throw new StorageTechnicalException(exc);
            }
        }
        StorageOffer offer = storageOffers.get(idOffer);
        if (offer == null || (!includeAllOfferState && !offer.isEnabled())) {
            throw new StorageNotFoundException(
                String.format("Storage offer with id %s is not found, disabled or not defined in strategy", idOffer));
        }

        return offer;
    }

    private void initReferentials() {
        try {
            loadReferential(ReferentialType.STRATEGY);
        } catch (final IOException exc) {
            LOGGER.warn("Couldn't load " + STRATEGY_FILENAME + " file", exc);
        } catch (final InvalidParseOperationException exc) {
            LOGGER.warn("Couldn't parse " + STRATEGY_FILENAME + " file", exc);
        }
        try {
            loadReferential(ReferentialType.OFFER);
        } catch (final IOException exc) {
            LOGGER.warn("Couldn't load " + OFFER_FILENAME + " file", exc);
        } catch (final InvalidParseOperationException exc) {
            LOGGER.warn("Couldn't parse " + OFFER_FILENAME + " file", exc);
        }

    }

    private void loadReferential(ReferentialType type) throws IOException, InvalidParseOperationException {
        switch (type) {
            case STRATEGY:
                storageStrategy = JsonHandler.getFromFileLowerCamelCase(PropertiesUtils.findFile(STRATEGY_FILENAME),
                    StorageStrategy.class);
                break;
            case OFFER:
                if (storageStrategy == null) {
                    throw new InvalidParseOperationException("storageStrategy is null when loading storage offer");
                }
                StorageOffer[] storageOffersArray = JsonHandler
                    .getFromFileLowerCamelCase(PropertiesUtils.findFile(OFFER_FILENAME), StorageOffer[].class);
                storageOffers = new HashMap<>();
                boolean foundReferentOffer = false;
                for (StorageOffer offer : storageOffersArray) {
                    boolean isReferent = storageStrategy.isStorageOfferReferent(offer.getId());
                    if (offer.isAsyncRead() && isReferent) {
                        throw new IllegalArgumentException("Offer (" + offer.getId() +
                            ") is referent and asyncRead. Referent offer mustn't be asyncRead");
                    }

                    if (isReferent) {
                        foundReferentOffer = true;
                    }

                    offer.setEnabled(storageStrategy.isStorageOfferEnabled(offer.getId()));
                    storageOffers.put(offer.getId(), offer);
                }

                if (!foundReferentOffer) {
                    throw new IllegalArgumentException(
                        "No referent offer found! At least, one referent offer is required");
                }
                break;
            default:
                LOGGER.error("Referential loading not implemented for type: " + type);
        }
    }

    /**
     * For Junit only
     *
     * @param strategy the new strategy
     */
    void setStorageStrategy(StorageStrategy strategy) {
        storageStrategy = strategy;
    }

    /**
     * For Junit only
     *
     * @param offer the new offer
     */
    void setStorageOffer(StorageOffer offer) {
        if (offer == null) {
            storageOffers = null;
        } else {
            storageOffers.put(offer.getId(), offer);
        }
    }
}
