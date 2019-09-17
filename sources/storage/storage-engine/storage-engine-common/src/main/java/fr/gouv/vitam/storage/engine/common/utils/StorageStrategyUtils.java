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
package fr.gouv.vitam.storage.engine.common.utils;

import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.storage.engine.common.referential.model.OfferReference;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageStrategy;

import java.util.List;
import java.util.Optional;

/**
 * Storage strategy utils.
 */
public class StorageStrategyUtils {

    public static void checkStrategy(String storageStrategy, List<StorageStrategy> referentialStrategies,
            String variableName, boolean referentOfferMandatory)
            throws StorageStrategyNotFoundException, ReferentOfferNotFoundException {
        Optional<StorageStrategy> referentialStrategy = referentialStrategies.stream()
                .filter(strategy -> storageStrategy.equals(strategy.getId())).findFirst();
        if (!referentialStrategy.isPresent()) {
            throw new StorageStrategyNotFoundException("Strategy was not found", storageStrategy, variableName);
        }
        if (referentOfferMandatory) {
            Optional<StorageStrategy> defaultStrategy = referentialStrategies.stream()
                    .filter(strategy -> VitamConfiguration.getDefaultStrategy().equals(strategy.getId())).findFirst();
            if (!defaultStrategy.isPresent()) {
                throw new StorageStrategyNotFoundException("Strategy was not found", storageStrategy, variableName);
            }
            OfferReference referentOffer = defaultStrategy.get().getOffers().stream()
                    .filter(offer -> offer.isEnabled() && offer.isReferent()).findFirst().get();
            if (referentialStrategy.get().getOffers().stream()
                    .filter(offer -> referentOffer.getId().equals(offer.getId())).count() < 1) {
                throw new ReferentOfferNotFoundException("Strategy does not contains mandatory referent offer ",
                        storageStrategy, referentOffer.getId(), variableName);
            }
        }
    }

}
