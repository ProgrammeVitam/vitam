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
package fr.gouv.vitam.storage.engine.common.referential;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.exception.StorageTechnicalException;
import fr.gouv.vitam.storage.engine.common.referential.model.OfferReference;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageOffer;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageStrategy;
import fr.gouv.vitam.storage.engine.common.utils.StorageStrategyUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * File system implementation of the storage strategy and storage offer provider
 */
class FileStorageProvider implements StorageStrategyProvider, StorageOfferProvider {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(FileStorageProvider.class);

    private static final String STRATEGY_FILENAME = "static-strategy.json";
    private static final String OFFER_FILENAME = "static-offer.json";

    private static final String NO_STRATEGY_MSG = "No strategy found! At least a 'default' strategy is required";
    private static final String NO_DEFAULT_STRATEGY_MSG =
        "No 'default' strategy found! One and only one 'default' strategy is required";
    private static final String TOO_MANY_DEFAULT_STRATEGY_MSG =
        "More than one 'default' strategy found! One and only one 'default' strategy is required";
    private static final String NO_REFERENT_OFFER_MSG =
        "The 'default' strategy does not contains a 'referent' offer! One and only one 'referent' offer is required in the 'default' strategy";
    private static final String TOO_MANY_REFERENT_OFFERS_MSG =
        "The 'default' strategy contains more than one 'referent' offer! One and only one 'referent' offer is required in the 'default' strategy";
    private static final String OTHER_STRATEGY_TOO_MANY_REFERENT_OFFER_MSG =
        "One or more strategies (%s) contains more than one 'referent' offer! At most one 'referent' offer is valid in a strategy";

    private volatile Map<String, StorageOffer> storageOffers;
    private volatile Map<String, StorageStrategy> storageStrategies;

    /**
     * Package protected to avoid out of scope instance creation
     */
    FileStorageProvider() {
        initReferentials();
    }

    /**
     * Convenient enum to describe the different kind of referential handled by
     * thi provider
     */
    private enum ReferentialType {
        STRATEGY,
        OFFER,
    }

    @Override
    public StorageStrategy getStorageStrategy(String idStrategy) throws StorageTechnicalException {
        if (storageStrategies == null) {
            try {
                loadReferential(ReferentialType.STRATEGY);
            } catch (IOException | InvalidParseOperationException exc) {
                throw new StorageTechnicalException(exc);
            }
        }

        if (storageStrategies.containsKey(idStrategy)) {
            return storageStrategies.get(idStrategy);
        } else {
            throw new StorageTechnicalException("Storage strategy '" + idStrategy + "' invalid");
        }
    }

    @Override
    public StorageOffer getStorageOffer(String idOffer) throws StorageException {
        return getFilteredStorageOffer(idOffer, false); //get only active one
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StorageOffer getStorageOffer(String idOffer, boolean includeDisabled) throws StorageException {
        return getFilteredStorageOffer(idOffer, includeDisabled); //get all (active and inactive)
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
                String.format("Storage offer with id %s is not found, disabled or not defined in strategy", idOffer)
            );
        }

        return offer;
    }

    @Override
    public Map<String, StorageStrategy> getStorageStrategies() throws StorageTechnicalException {
        return storageStrategies;
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
                loadStrategies();
                break;
            case OFFER:
                loadOffers();
                break;
            default:
                LOGGER.error("Referential loading not implemented for type: " + type);
        }
    }

    private void loadStrategies() throws InvalidParseOperationException, FileNotFoundException {
        StorageStrategy[] storageStrategiesArray = JsonHandler.getFromFileLowerCamelCase(
            PropertiesUtils.findFile(STRATEGY_FILENAME),
            StorageStrategy[].class
        );
        if (storageStrategiesArray == null || storageStrategiesArray.length < 1) {
            throw new IllegalArgumentException(NO_STRATEGY_MSG);
        }
        List<StorageStrategy> storageStrategiesList = Arrays.asList(storageStrategiesArray);

        // validate that 'default' strategy is present
        List<StorageStrategy> defaultStrategies = storageStrategiesList
            .stream()
            .filter(strategy -> VitamConfiguration.getDefaultStrategy().equals(strategy.getId()))
            .collect(Collectors.toList());
        if (defaultStrategies.isEmpty()) {
            throw new IllegalArgumentException(NO_DEFAULT_STRATEGY_MSG);
        } else if (defaultStrategies.size() > 1) {
            throw new IllegalArgumentException(TOO_MANY_DEFAULT_STRATEGY_MSG);
        }

        // check if an active 'referent' offer is present in default strategy
        List<OfferReference> referentOffers = defaultStrategies
            .get(0)
            .getOffers()
            .stream()
            .filter(OfferReference::isReferent)
            .filter(OfferReference::isEnabled)
            .collect(Collectors.toList());
        if (referentOffers.isEmpty()) {
            throw new IllegalArgumentException(NO_REFERENT_OFFER_MSG);
        }
        if (referentOffers.size() > 1) {
            throw new IllegalArgumentException(TOO_MANY_REFERENT_OFFERS_MSG);
        }

        // check if any strategy has more than one 'referent' offer
        Set<String> invalidStrategies = storageStrategiesList
            .stream()
            .filter(
                storageStrategy ->
                    storageStrategy
                        .getOffers()
                        .stream()
                        .filter(OfferReference::isReferent)
                        .filter(OfferReference::isEnabled)
                        .count() >
                    1
            )
            .map(StorageStrategy::getId)
            .collect(Collectors.toSet());
        if (!invalidStrategies.isEmpty()) {
            throw new IllegalArgumentException(
                String.format(OTHER_STRATEGY_TOO_MANY_REFERENT_OFFER_MSG, String.join(",", invalidStrategies))
            );
        }

        // check if a referent offer is not used in more than one strategies
        if (!StorageStrategyUtils.checkReferentOfferUsageInStrategiesValid(storageStrategiesList)) {
            LOGGER.warn("One or more offers are referents in more than one strategy.");
        }

        storageStrategies = storageStrategiesList
            .stream()
            .collect(Collectors.toMap(StorageStrategy::getId, storageStrategy -> storageStrategy));
        storageStrategies.values().forEach(StorageStrategy::postInit);
    }

    private void loadOffers() throws InvalidParseOperationException, FileNotFoundException {
        if (storageStrategies == null || storageStrategies.isEmpty()) {
            throw new InvalidParseOperationException("storageStrategies is null when loading storage offer");
        }
        StorageOffer[] storageOffersArray = JsonHandler.getFromFileLowerCamelCase(
            PropertiesUtils.findFile(OFFER_FILENAME),
            StorageOffer[].class
        );
        storageOffers = new HashMap<>();

        for (StorageOffer offer : storageOffersArray) {
            boolean isEnabled = storageStrategies
                .values()
                .stream()
                .anyMatch(strategy -> strategy.isStorageOfferEnabled(offer.getId()));

            offer.setEnabled(isEnabled);
            storageOffers.put(offer.getId(), offer);
            storageStrategies.values().forEach(StorageStrategy::postInit);
        }
    }

    /**
     * For Junit only
     *
     * @param storageStrategies the new storageStrategies
     */
    @VisibleForTesting
    void setStorageStrategies(Map<String, StorageStrategy> storageStrategies) {
        this.storageStrategies = storageStrategies;
    }

    /**
     * For Junit only
     *
     * @param offer the new offer
     */
    @VisibleForTesting
    void setStorageOffer(StorageOffer offer) {
        if (offer == null) {
            storageOffers = null;
        } else {
            storageOffers.put(offer.getId(), offer);
        }
    }
}
