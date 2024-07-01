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
package fr.gouv.vitam.worker.core.plugin.lfc_traceability;

import fr.gouv.vitam.common.alert.AlertService;
import fr.gouv.vitam.common.logging.VitamLogLevel;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.logbook.common.model.EntryTraceabilityStatistics;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class DigestValidator {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DigestValidator.class);

    public static final String INVALID_HASH = "INVALID_HASH";

    private final AlertService alertService;

    private int nbMetadataOK = 0;
    private int nbMetadataWarnings = 0;
    private int nbMetadataErrors = 0;

    private int nbObjectOK = 0;
    private int nbObjectWarnings = 0;
    private int nbObjectErrors = 0;

    public DigestValidator(AlertService alertService) {
        this.alertService = alertService;
    }

    public DigestValidationDetails validateMetadataDigest(
        String id,
        String strategyId,
        String digestInDb,
        Map<String, String> digestByOfferId
    ) {
        DigestValidationDetails digestValidationDetails = validateDigest(
            id,
            strategyId,
            digestInDb,
            digestByOfferId,
            alertService,
            "metadata"
        );

        if (digestValidationDetails.hasInconsistencies()) {
            nbMetadataWarnings++;
        } else {
            nbMetadataOK++;
        }

        return digestValidationDetails;
    }

    public DigestValidationDetails validateObjectDigest(
        String id,
        String strategyId,
        String digestInDb,
        Map<String, String> digestByOfferId
    ) {
        DigestValidationDetails digestValidationDetails = validateDigest(
            id,
            strategyId,
            digestInDb,
            digestByOfferId,
            alertService,
            "binary object"
        );

        if (digestValidationDetails.hasInconsistencies()) {
            nbObjectWarnings++;
        } else {
            nbObjectOK++;
        }

        return digestValidationDetails;
    }

    private DigestValidationDetails validateDigest(
        String id,
        String strategyId,
        String digestInDb,
        Map<String, String> digestByOfferId,
        AlertService alertService,
        String objectType
    ) {
        List<String> offersWithoutDigest = digestByOfferId
            .entrySet()
            .stream()
            .filter(entry -> entry.getValue() == null)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        if (!offersWithoutDigest.isEmpty()) {
            alertService.createAlert(
                VitamLogLevel.WARN,
                String.format(
                    "Missing digest for %s with id=%s in offers %s. Digest in db : %s",
                    objectType,
                    id,
                    String.join(", ", offersWithoutDigest),
                    digestInDb
                )
            );
        }

        // Ensure that all digests match db
        Map<String, String> offersWithInconsistentDigest = digestByOfferId
            .entrySet()
            .stream()
            .filter(entry -> entry.getValue() != null && !digestInDb.equals(entry.getValue()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (!offersWithInconsistentDigest.isEmpty()) {
            alertService.createAlert(
                VitamLogLevel.ERROR,
                String.format(
                    "Digest mismatch for %s (%s). %s. Digest in db : %s",
                    objectType,
                    id,
                    offersWithInconsistentDigest
                        .entrySet()
                        .stream()
                        .map(entry -> entry.getKey() + ":" + entry.getValue())
                        .collect(Collectors.joining(", ", "{", "}")),
                    digestInDb
                )
            );
        }

        Predicate<String> notEqual = x -> !digestInDb.equals(x);
        boolean atLeastOneOfferKO = digestByOfferId.values().stream().anyMatch(notEqual);

        //all offers are KO
        boolean allOffersKO = digestByOfferId.values().stream().allMatch(notEqual);

        String globalDigest = atLeastOneOfferKO ? INVALID_HASH : digestInDb;
        Set<String> offerIds = digestByOfferId.keySet();
        return new DigestValidationDetails(
            strategyId,
            offerIds,
            globalDigest,
            digestInDb,
            digestByOfferId,
            atLeastOneOfferKO,
            allOffersKO
        );
    }

    public EntryTraceabilityStatistics getMetadataValidationStatistics() {
        return new EntryTraceabilityStatistics(nbMetadataOK, nbMetadataWarnings, nbMetadataErrors);
    }

    public EntryTraceabilityStatistics getObjectValidationStatistics() {
        return new EntryTraceabilityStatistics(nbObjectOK, nbObjectWarnings, nbObjectErrors);
    }

    public boolean hasInconsistencies() {
        return this.nbMetadataErrors + this.nbMetadataWarnings + this.nbObjectErrors + this.nbObjectWarnings > 0;
    }
}
