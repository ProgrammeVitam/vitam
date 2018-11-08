
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
package fr.gouv.vitam.worker.common.utils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import fr.gouv.vitam.common.alert.AlertService;
import fr.gouv.vitam.common.logging.VitamLogLevel;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;

/**
 * StorageClientUtil class
 */
public class StorageClientUtil {

    public static final String UNKNOWN_HASH = "0000000000000000000000000000000000000000000000000000000000000000";

    /**
     * Check that all offers have digest information, and that all digests match.
     */
    public static String aggregateOfferDigests(Map<String, String> digestsByOfferId, DataCategory dataCategory,
        String objectId, AlertService alertService) {

        boolean offersAreNotSynchronised = false;

        if (digestsByOfferId.isEmpty()) {
            throw new IllegalStateException("No offer digest provided");
        }

        // Ensure all offers have digest information
        List<String> offersWithoutDigest = digestsByOfferId.entrySet()
            .stream()
            .filter(entry -> entry.getValue() == null)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        if (!offersWithoutDigest.isEmpty()) {
            alertService.createAlert(VitamLogLevel.ERROR,
                String.format("Missing metadata (%s - %s) digest from offers %s",
                    dataCategory, objectId, String.join(", ", offersWithoutDigest)));
            offersAreNotSynchronised = true;
        }

        // Ensure that all digests match
        Set<String> digests = digestsByOfferId.entrySet()
            .stream()
            .filter(entry -> entry.getValue() != null)
            .map(Map.Entry::getValue)
            .collect(Collectors.toSet());

        if (digests.size() > 1) {
            alertService.createAlert(VitamLogLevel.ERROR,
                String.format("Digest mismatch for metadata %s (%s). %s",
                    dataCategory, objectId,
                    digestsByOfferId.entrySet()
                        .stream()
                        .filter(entry -> entry.getValue() != null)
                        .map(entry -> entry.getKey() + ": " + entry.getValue())
                        .collect(Collectors.joining(","))));
            offersAreNotSynchronised = true;
        }

        if (offersAreNotSynchronised) {
            return UNKNOWN_HASH;
        }

        return digests.iterator().next();
    }
}
