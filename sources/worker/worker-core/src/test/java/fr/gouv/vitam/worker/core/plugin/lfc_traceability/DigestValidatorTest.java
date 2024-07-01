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

import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.alert.AlertService;
import fr.gouv.vitam.common.logging.VitamLogLevel;
import fr.gouv.vitam.worker.core.exception.ProcessingStatusException;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.HashMap;
import java.util.Map;

import static fr.gouv.vitam.worker.core.plugin.lfc_traceability.DigestValidator.INVALID_HASH;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class DigestValidatorTest {

    private static final String OFFER_1 = "offer1";
    private static final String OFFER_2 = "offer2";
    private static final String DIGEST_1 = "digest1";
    private static final String DIGEST_2 = "digest2";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private AlertService alertService;

    @Test
    public void validateMetadataValidSingleOffer() throws ProcessingStatusException {
        // Given
        Map<String, String> offerDigests = new HashMap<>();
        offerDigests.put(OFFER_1, DIGEST_1);
        DigestValidator instance = new DigestValidator(alertService);

        // When
        DigestValidationDetails digestValidationDetails = instance.validateMetadataDigest(
            "id",
            VitamConfiguration.getDefaultStrategy(),
            DIGEST_1,
            offerDigests
        );

        // Then
        assertThat(digestValidationDetails.hasInconsistencies()).isFalse();
        assertThat(digestValidationDetails.getOfferIds()).containsExactlyInAnyOrder(OFFER_1);
        assertThat(digestValidationDetails.getStrategyId()).isEqualTo(VitamConfiguration.getDefaultStrategy());
        assertThat(digestValidationDetails.getGlobalDigest()).isEqualTo(DIGEST_1);
        assertThat(digestValidationDetails.getDigestInDb()).isEqualTo(DIGEST_1);
        assertThat(digestValidationDetails.getDigestByOfferId()).isEqualTo(offerDigests);

        checkStats(instance, 1, 0, 0, 0);
        verifyNoMoreInteractions(alertService);
    }

    @Test
    public void validateMetadataValidMultipleOffers() throws ProcessingStatusException {
        // Given
        Map<String, String> offerDigests = new HashMap<>();
        offerDigests.put(OFFER_1, DIGEST_1);
        offerDigests.put(OFFER_2, DIGEST_1);
        DigestValidator instance = new DigestValidator(alertService);

        // When
        DigestValidationDetails digestValidationDetails = instance.validateMetadataDigest(
            "id",
            VitamConfiguration.getDefaultStrategy(),
            DIGEST_1,
            offerDigests
        );

        // Then
        assertThat(digestValidationDetails.hasInconsistencies()).isFalse();
        assertThat(digestValidationDetails.getOfferIds()).containsExactlyInAnyOrder(OFFER_1, OFFER_2);
        assertThat(digestValidationDetails.getStrategyId()).isEqualTo(VitamConfiguration.getDefaultStrategy());
        assertThat(digestValidationDetails.getGlobalDigest()).isEqualTo(DIGEST_1);
        assertThat(digestValidationDetails.getDigestInDb()).isEqualTo(DIGEST_1);
        assertThat(digestValidationDetails.getDigestByOfferId()).isEqualTo(offerDigests);

        checkStats(instance, 1, 0, 0, 0);
        verifyNoMoreInteractions(alertService);
    }

    @Test
    public void validateMetadataMissingOfferDigest() throws ProcessingStatusException {
        // Given
        Map<String, String> offerDigests = new HashMap<>();
        offerDigests.put(OFFER_1, null);
        offerDigests.put(OFFER_2, DIGEST_1);
        DigestValidator instance = new DigestValidator(alertService);

        // When
        DigestValidationDetails digestValidationDetails = instance.validateMetadataDigest(
            "id",
            VitamConfiguration.getDefaultStrategy(),
            DIGEST_1,
            offerDigests
        );

        // Then
        assertThat(digestValidationDetails.hasInconsistencies()).isTrue();
        assertThat(digestValidationDetails.getOfferIds()).containsExactlyInAnyOrder(OFFER_1, OFFER_2);
        assertThat(digestValidationDetails.getStrategyId()).isEqualTo(VitamConfiguration.getDefaultStrategy());
        assertThat(digestValidationDetails.getGlobalDigest()).isEqualTo(INVALID_HASH);
        assertThat(digestValidationDetails.getDigestInDb()).isEqualTo(DIGEST_1);
        assertThat(digestValidationDetails.getDigestByOfferId()).isEqualTo(offerDigests);

        checkStats(instance, 0, 1, 0, 0);

        verify(alertService).createAlert(eq(VitamLogLevel.WARN), anyString());
        verifyNoMoreInteractions(alertService);
    }

    @Test
    public void validateMetadataAllOfferDigestKO() throws ProcessingStatusException {
        // Given
        Map<String, String> offerDigests = new HashMap<>();
        offerDigests.put(OFFER_1, null);
        offerDigests.put(OFFER_2, DIGEST_2);
        DigestValidator instance = new DigestValidator(alertService);

        // When
        DigestValidationDetails digestValidationDetails = instance.validateObjectDigest(
            "id",
            VitamConfiguration.getDefaultStrategy(),
            DIGEST_1,
            offerDigests
        );

        //Then
        assertThat(digestValidationDetails.hasInconsistencies()).isTrue();
        assertThat(digestValidationDetails.getOfferIds()).containsExactlyInAnyOrder(OFFER_1, OFFER_2);
        assertThat(digestValidationDetails.getStrategyId()).isEqualTo(VitamConfiguration.getDefaultStrategy());
        assertThat(digestValidationDetails.getGlobalDigest()).isEqualTo(INVALID_HASH);
        assertThat(digestValidationDetails.getDigestInDb()).isEqualTo(DIGEST_1);
        assertThat(digestValidationDetails.getDigestByOfferId()).isEqualTo(offerDigests);
        checkStats(instance, 0, 0, 0, 1);

        checkStats(instance, 0, 0, 0, 1);

        verify(alertService).createAlert(eq(VitamLogLevel.WARN), anyString());
    }

    @Test
    public void validateMetadataInvalidOfferDigest() throws ProcessingStatusException {
        // Given
        Map<String, String> offerDigests = new HashMap<>();
        offerDigests.put(OFFER_1, DIGEST_1);
        offerDigests.put(OFFER_2, DIGEST_2);
        DigestValidator instance = new DigestValidator(alertService);

        // When
        DigestValidationDetails digestValidationDetails = instance.validateMetadataDigest(
            "id",
            VitamConfiguration.getDefaultStrategy(),
            DIGEST_1,
            offerDigests
        );

        // Then
        assertThat(digestValidationDetails.hasInconsistencies()).isTrue();
        assertThat(digestValidationDetails.getOfferIds()).containsExactlyInAnyOrder(OFFER_1, OFFER_2);
        assertThat(digestValidationDetails.getStrategyId()).isEqualTo(VitamConfiguration.getDefaultStrategy());
        assertThat(digestValidationDetails.getGlobalDigest()).isEqualTo(INVALID_HASH);
        assertThat(digestValidationDetails.getDigestInDb()).isEqualTo(DIGEST_1);
        assertThat(digestValidationDetails.getDigestByOfferId()).isEqualTo(offerDigests);

        checkStats(instance, 0, 1, 0, 0);

        verify(alertService).createAlert(eq(VitamLogLevel.ERROR), anyString());
        verifyNoMoreInteractions(alertService);
    }

    @Test
    public void validateObjectValidSingleOffer() throws ProcessingStatusException {
        // Given
        Map<String, String> offerDigests = new HashMap<>();
        offerDigests.put(OFFER_1, DIGEST_1);
        DigestValidator instance = new DigestValidator(alertService);

        // When
        DigestValidationDetails digestValidationDetails = instance.validateObjectDigest(
            "id",
            VitamConfiguration.getDefaultStrategy(),
            DIGEST_1,
            offerDigests
        );

        // Then
        assertThat(digestValidationDetails.hasInconsistencies()).isFalse();
        assertThat(digestValidationDetails.getOfferIds()).containsExactlyInAnyOrder(OFFER_1);
        assertThat(digestValidationDetails.getStrategyId()).isEqualTo(VitamConfiguration.getDefaultStrategy());
        assertThat(digestValidationDetails.getGlobalDigest()).isEqualTo(DIGEST_1);
        assertThat(digestValidationDetails.getDigestInDb()).isEqualTo(DIGEST_1);
        assertThat(digestValidationDetails.getDigestByOfferId()).isEqualTo(offerDigests);

        checkStats(instance, 0, 0, 1, 0);
        verifyNoMoreInteractions(alertService);
    }

    @Test
    public void validateObjectValidMultipleOffers() throws ProcessingStatusException {
        // Given
        Map<String, String> offerDigests = new HashMap<>();
        offerDigests.put(OFFER_1, DIGEST_1);
        offerDigests.put(OFFER_2, DIGEST_1);
        DigestValidator instance = new DigestValidator(alertService);

        // When
        DigestValidationDetails digestValidationDetails = instance.validateObjectDigest(
            "id",
            VitamConfiguration.getDefaultStrategy(),
            DIGEST_1,
            offerDigests
        );

        // Then
        assertThat(digestValidationDetails.hasInconsistencies()).isFalse();
        assertThat(digestValidationDetails.getOfferIds()).containsExactlyInAnyOrder(OFFER_1, OFFER_2);
        assertThat(digestValidationDetails.getStrategyId()).isEqualTo(VitamConfiguration.getDefaultStrategy());
        assertThat(digestValidationDetails.getGlobalDigest()).isEqualTo(DIGEST_1);
        assertThat(digestValidationDetails.getDigestInDb()).isEqualTo(DIGEST_1);
        assertThat(digestValidationDetails.getDigestByOfferId()).isEqualTo(offerDigests);

        checkStats(instance, 0, 0, 1, 0);
        verifyNoMoreInteractions(alertService);
    }

    @Test
    public void validateObjectMissingOfferDigest() throws ProcessingStatusException {
        // Given
        Map<String, String> offerDigests = new HashMap<>();
        offerDigests.put(OFFER_1, null);
        offerDigests.put(OFFER_2, DIGEST_1);
        DigestValidator instance = new DigestValidator(alertService);

        // When
        DigestValidationDetails digestValidationDetails = instance.validateObjectDigest(
            "id",
            VitamConfiguration.getDefaultStrategy(),
            DIGEST_1,
            offerDigests
        );

        // Then
        assertThat(digestValidationDetails.hasInconsistencies()).isTrue();
        assertThat(digestValidationDetails.getOfferIds()).containsExactlyInAnyOrder(OFFER_1, OFFER_2);
        assertThat(digestValidationDetails.getStrategyId()).isEqualTo(VitamConfiguration.getDefaultStrategy());
        assertThat(digestValidationDetails.getGlobalDigest()).isEqualTo(INVALID_HASH);
        assertThat(digestValidationDetails.getDigestInDb()).isEqualTo(DIGEST_1);
        assertThat(digestValidationDetails.getDigestByOfferId()).isEqualTo(offerDigests);

        checkStats(instance, 0, 0, 0, 1);

        verify(alertService).createAlert(eq(VitamLogLevel.WARN), anyString());
        verifyNoMoreInteractions(alertService);
    }

    @Test
    public void validateObjectAllOfferDigestKO() throws ProcessingStatusException {
        // Given
        Map<String, String> offerDigests = new HashMap<>();
        offerDigests.put(OFFER_1, null);
        offerDigests.put(OFFER_2, DIGEST_2);
        DigestValidator instance = new DigestValidator(alertService);

        // When
        DigestValidationDetails digestValidationDetails = instance.validateObjectDigest(
            "id",
            VitamConfiguration.getDefaultStrategy(),
            DIGEST_1,
            offerDigests
        );

        //Then
        assertThat(digestValidationDetails.hasInconsistencies()).isTrue();
        assertThat(digestValidationDetails.getOfferIds()).containsExactlyInAnyOrder(OFFER_1, OFFER_2);
        assertThat(digestValidationDetails.getStrategyId()).isEqualTo(VitamConfiguration.getDefaultStrategy());
        assertThat(digestValidationDetails.getGlobalDigest()).isEqualTo(INVALID_HASH);
        assertThat(digestValidationDetails.getDigestInDb()).isEqualTo(DIGEST_1);
        assertThat(digestValidationDetails.getDigestByOfferId()).isEqualTo(offerDigests);
        checkStats(instance, 0, 0, 0, 1);

        verify(alertService).createAlert(eq(VitamLogLevel.WARN), anyString());
    }

    @Test
    public void validateObjectInvalidOfferDigest() throws ProcessingStatusException {
        // Given
        Map<String, String> offerDigests = new HashMap<>();
        offerDigests.put(OFFER_1, DIGEST_1);
        offerDigests.put(OFFER_2, DIGEST_2);
        DigestValidator instance = new DigestValidator(alertService);

        // When
        DigestValidationDetails digestValidationDetails = instance.validateObjectDigest(
            "id",
            VitamConfiguration.getDefaultStrategy(),
            DIGEST_1,
            offerDigests
        );

        // Then
        assertThat(digestValidationDetails.hasInconsistencies()).isTrue();
        assertThat(digestValidationDetails.getOfferIds()).containsExactlyInAnyOrder(OFFER_1, OFFER_2);
        assertThat(digestValidationDetails.getStrategyId()).isEqualTo(VitamConfiguration.getDefaultStrategy());
        assertThat(digestValidationDetails.getGlobalDigest()).isEqualTo(INVALID_HASH);
        assertThat(digestValidationDetails.getDigestInDb()).isEqualTo(DIGEST_1);
        assertThat(digestValidationDetails.getDigestByOfferId()).isEqualTo(offerDigests);

        checkStats(instance, 0, 0, 0, 1);

        verify(alertService).createAlert(eq(VitamLogLevel.ERROR), anyString());
        verifyNoMoreInteractions(alertService);
    }

    private void checkStats(
        DigestValidator instance,
        int nbMetadataOK,
        int nbMetadataWarnings,
        int nbObjectOK,
        int nbObjectWarnings
    ) {
        assertThat(instance.getMetadataValidationStatistics().getNbOK()).isEqualTo(nbMetadataOK);
        assertThat(instance.getMetadataValidationStatistics().getNbWarnings()).isEqualTo(nbMetadataWarnings);

        assertThat(instance.getObjectValidationStatistics().getNbOK()).isEqualTo(nbObjectOK);
        assertThat(instance.getObjectValidationStatistics().getNbWarnings()).isEqualTo(nbObjectWarnings);
    }
}
