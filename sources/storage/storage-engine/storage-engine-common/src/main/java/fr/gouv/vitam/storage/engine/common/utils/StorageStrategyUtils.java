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
package fr.gouv.vitam.storage.engine.common.utils;

import fr.gouv.vitam.storage.engine.common.referential.model.OfferReference;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageStrategy;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Storage strategy utils.
 */
public class StorageStrategyUtils {

    public static void checkStrategy(
        String storageStrategy,
        List<StorageStrategy> referentialStrategies,
        String variableName,
        boolean referentOffersMandatory
    ) throws StorageStrategyNotFoundException, StorageStrategyReferentOfferException {
        Optional<StorageStrategy> referentialStrategy = referentialStrategies
            .stream()
            .filter(strategy -> storageStrategy.equals(strategy.getId()))
            .findFirst();
        if (!referentialStrategy.isPresent()) {
            throw new StorageStrategyNotFoundException("Strategy was not found", storageStrategy, variableName);
        }

        if (referentOffersMandatory) {
            long nbReferentialOffers = referentialStrategy
                .get()
                .getOffers()
                .stream()
                .filter(OfferReference::isReferent)
                .filter(OfferReference::isEnabled)
                .count();
            if (nbReferentialOffers != 1) {
                throw new StorageStrategyReferentOfferException(
                    String.format(
                        "Strategy contains %s referent offer(s). The strategy must contains one and only one referent offer ",
                        nbReferentialOffers
                    ),
                    storageStrategy,
                    variableName
                );
            }
        }
    }

    public static List<String> loadOfferIds(String storageStrategyId, List<StorageStrategy> storageStrategies)
        throws StorageStrategyNotFoundException {
        Optional<StorageStrategy> storageStrategy = storageStrategies
            .stream()
            .filter(strategy -> strategy.getId().equals(storageStrategyId))
            .findFirst();
        if (!storageStrategy.isPresent()) {
            throw new StorageStrategyNotFoundException(String.format("Could not find strategy %s", storageStrategyId));
        }
        return storageStrategy
            .get()
            .getOffers()
            .stream()
            .filter(offer -> offer.isEnabled())
            .map(offer -> offer.getId())
            .collect(Collectors.toList());
    }

    public static boolean checkReferentOfferUsageInStrategiesValid(List<StorageStrategy> storageStrategies) {
        // check if a referent offer is not used in more than one strategies
        Map<String, String> referentOfferByStrategy = storageStrategies
            .stream()
            .filter(
                strategy ->
                    strategy
                        .getOffers()
                        .stream()
                        .filter(OfferReference::isReferent)
                        .filter(OfferReference::isEnabled)
                        .map(OfferReference::getId)
                        .count() ==
                    1
            )
            .collect(
                Collectors.toMap(
                    StorageStrategy::getId,
                    strategy ->
                        strategy
                            .getOffers()
                            .stream()
                            .filter(OfferReference::isReferent)
                            .filter(OfferReference::isEnabled)
                            .map(OfferReference::getId)
                            .findFirst()
                            .get()
                )
            );
        return referentOfferByStrategy.keySet().size() == new HashSet<>(referentOfferByStrategy.values()).size();
    }
}
