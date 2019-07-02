package fr.gouv.vitam.worker.core.plugin.lfc_traceability;

import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.alert.AlertService;
import fr.gouv.vitam.common.logging.VitamLogLevel;
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

    @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock private AlertService alertService;

    @Test
    public void validateMetadataValidSingleOffer() {

        // Given
        Map<String, String> offerDigests = new HashMap<>();
        offerDigests.put(OFFER_1, DIGEST_1);
        DigestValidator instance = new DigestValidator(alertService);

        // When
        DigestValidationDetails digestValidationDetails =
            instance.validateMetadataDigest("id", VitamConfiguration.getDefaultStrategy(), DIGEST_1, offerDigests);

        // Then
        assertThat(digestValidationDetails.hasInconsistencies()).isFalse();
        assertThat(digestValidationDetails.getOfferIds()).containsExactlyInAnyOrder(OFFER_1);
        assertThat(digestValidationDetails.getStrategyId()).isEqualTo(VitamConfiguration.getDefaultStrategy());
        assertThat(digestValidationDetails.getGlobalDigest()).isEqualTo(DIGEST_1);
        assertThat(digestValidationDetails.getDigestInDb()).isEqualTo(DIGEST_1);
        assertThat(digestValidationDetails.getDigestByOfferId()).isEqualTo(offerDigests);

        checkStats(instance, 1, 0, 0, 0, 0, 0);
        verifyNoMoreInteractions(alertService);
    }

    @Test
    public void validateMetadataValidMultipleOffers() {

        // Given
        Map<String, String> offerDigests = new HashMap<>();
        offerDigests.put(OFFER_1, DIGEST_1);
        offerDigests.put(OFFER_2, DIGEST_1);
        DigestValidator instance = new DigestValidator(alertService);

        // When
        DigestValidationDetails digestValidationDetails =
            instance.validateMetadataDigest("id", VitamConfiguration.getDefaultStrategy(), DIGEST_1, offerDigests);

        // Then
        assertThat(digestValidationDetails.hasInconsistencies()).isFalse();
        assertThat(digestValidationDetails.getOfferIds()).containsExactlyInAnyOrder(OFFER_1, OFFER_2);
        assertThat(digestValidationDetails.getStrategyId()).isEqualTo(VitamConfiguration.getDefaultStrategy());
        assertThat(digestValidationDetails.getGlobalDigest()).isEqualTo(DIGEST_1);
        assertThat(digestValidationDetails.getDigestInDb()).isEqualTo(DIGEST_1);
        assertThat(digestValidationDetails.getDigestByOfferId()).isEqualTo(offerDigests);

        checkStats(instance, 1, 0, 0, 0, 0, 0);
        verifyNoMoreInteractions(alertService);
    }

    @Test
    public void validateMetadataMissingOfferDigest() {

        // Given
        Map<String, String> offerDigests = new HashMap<>();
        offerDigests.put(OFFER_1, null);
        offerDigests.put(OFFER_2, DIGEST_1);
        DigestValidator instance = new DigestValidator(alertService);

        // When
        DigestValidationDetails digestValidationDetails =
            instance.validateMetadataDigest("id", VitamConfiguration.getDefaultStrategy(), DIGEST_1, offerDigests);

        // Then
        assertThat(digestValidationDetails.hasInconsistencies()).isTrue();
        assertThat(digestValidationDetails.getOfferIds()).containsExactlyInAnyOrder(OFFER_1, OFFER_2);
        assertThat(digestValidationDetails.getStrategyId()).isEqualTo(VitamConfiguration.getDefaultStrategy());
        assertThat(digestValidationDetails.getGlobalDigest()).isEqualTo(DIGEST_1);
        assertThat(digestValidationDetails.getDigestInDb()).isEqualTo(DIGEST_1);
        assertThat(digestValidationDetails.getDigestByOfferId()).isEqualTo(offerDigests);

        checkStats(instance, 0, 1, 0, 0, 0, 0);

        verify(alertService).createAlert(eq(VitamLogLevel.WARN), anyString());
        verifyNoMoreInteractions(alertService);
    }

    @Test
    public void validateMetadataInvalidOfferDigest() {

        // Given
        Map<String, String> offerDigests = new HashMap<>();
        offerDigests.put(OFFER_1, DIGEST_1);
        offerDigests.put(OFFER_2, DIGEST_2);
        DigestValidator instance = new DigestValidator(alertService);

        // When
        DigestValidationDetails digestValidationDetails =
            instance.validateMetadataDigest("id", VitamConfiguration.getDefaultStrategy(), DIGEST_1, offerDigests);

        // Then
        assertThat(digestValidationDetails.hasInconsistencies()).isTrue();
        assertThat(digestValidationDetails.getOfferIds()).containsExactlyInAnyOrder(OFFER_1, OFFER_2);
        assertThat(digestValidationDetails.getStrategyId()).isEqualTo(VitamConfiguration.getDefaultStrategy());
        assertThat(digestValidationDetails.getGlobalDigest()).isEqualTo(INVALID_HASH);
        assertThat(digestValidationDetails.getDigestInDb()).isEqualTo(DIGEST_1);
        assertThat(digestValidationDetails.getDigestByOfferId()).isEqualTo(offerDigests);

        checkStats(instance, 0, 0, 1, 0, 0, 0);

        verify(alertService).createAlert(eq(VitamLogLevel.ERROR), anyString());
        verifyNoMoreInteractions(alertService);
    }


    @Test
    public void validateObjectValidSingleOffer() {

        // Given
        Map<String, String> offerDigests = new HashMap<>();
        offerDigests.put(OFFER_1, DIGEST_1);
        DigestValidator instance = new DigestValidator(alertService);

        // When
        DigestValidationDetails digestValidationDetails =
            instance.validateObjectDigest("id", VitamConfiguration.getDefaultStrategy(), DIGEST_1, offerDigests);

        // Then
        assertThat(digestValidationDetails.hasInconsistencies()).isFalse();
        assertThat(digestValidationDetails.getOfferIds()).containsExactlyInAnyOrder(OFFER_1);
        assertThat(digestValidationDetails.getStrategyId()).isEqualTo(VitamConfiguration.getDefaultStrategy());
        assertThat(digestValidationDetails.getGlobalDigest()).isEqualTo(DIGEST_1);
        assertThat(digestValidationDetails.getDigestInDb()).isEqualTo(DIGEST_1);
        assertThat(digestValidationDetails.getDigestByOfferId()).isEqualTo(offerDigests);

        checkStats(instance, 0, 0, 0, 1, 0, 0);
        verifyNoMoreInteractions(alertService);
    }

    @Test
    public void validateObjectValidMultipleOffers() {

        // Given
        Map<String, String> offerDigests = new HashMap<>();
        offerDigests.put(OFFER_1, DIGEST_1);
        offerDigests.put(OFFER_2, DIGEST_1);
        DigestValidator instance = new DigestValidator(alertService);

        // When
        DigestValidationDetails digestValidationDetails =
            instance.validateObjectDigest("id", VitamConfiguration.getDefaultStrategy(), DIGEST_1, offerDigests);

        // Then
        assertThat(digestValidationDetails.hasInconsistencies()).isFalse();
        assertThat(digestValidationDetails.getOfferIds()).containsExactlyInAnyOrder(OFFER_1, OFFER_2);
        assertThat(digestValidationDetails.getStrategyId()).isEqualTo(VitamConfiguration.getDefaultStrategy());
        assertThat(digestValidationDetails.getGlobalDigest()).isEqualTo(DIGEST_1);
        assertThat(digestValidationDetails.getDigestInDb()).isEqualTo(DIGEST_1);
        assertThat(digestValidationDetails.getDigestByOfferId()).isEqualTo(offerDigests);

        checkStats(instance, 0, 0, 0, 1, 0, 0);
        verifyNoMoreInteractions(alertService);
    }

    @Test
    public void validateObjectMissingOfferDigest() {

        // Given
        Map<String, String> offerDigests = new HashMap<>();
        offerDigests.put(OFFER_1, null);
        offerDigests.put(OFFER_2, DIGEST_1);
        DigestValidator instance = new DigestValidator(alertService);

        // When
        DigestValidationDetails digestValidationDetails =
            instance.validateObjectDigest("id", VitamConfiguration.getDefaultStrategy(), DIGEST_1, offerDigests);

        // Then
        assertThat(digestValidationDetails.hasInconsistencies()).isTrue();
        assertThat(digestValidationDetails.getOfferIds()).containsExactlyInAnyOrder(OFFER_1, OFFER_2);
        assertThat(digestValidationDetails.getStrategyId()).isEqualTo(VitamConfiguration.getDefaultStrategy());
        assertThat(digestValidationDetails.getGlobalDigest()).isEqualTo(DIGEST_1);
        assertThat(digestValidationDetails.getDigestInDb()).isEqualTo(DIGEST_1);
        assertThat(digestValidationDetails.getDigestByOfferId()).isEqualTo(offerDigests);

        checkStats(instance, 0, 0, 0, 0, 1, 0);

        verify(alertService).createAlert(eq(VitamLogLevel.WARN), anyString());
        verifyNoMoreInteractions(alertService);
    }

    @Test
    public void validateObjectInvalidOfferDigest() {

        // Given
        Map<String, String> offerDigests = new HashMap<>();
        offerDigests.put(OFFER_1, DIGEST_1);
        offerDigests.put(OFFER_2, DIGEST_2);
        DigestValidator instance = new DigestValidator(alertService);

        // When
        DigestValidationDetails digestValidationDetails =
            instance.validateObjectDigest("id", VitamConfiguration.getDefaultStrategy(), DIGEST_1, offerDigests);

        // Then
        assertThat(digestValidationDetails.hasInconsistencies()).isTrue();
        assertThat(digestValidationDetails.getOfferIds()).containsExactlyInAnyOrder(OFFER_1, OFFER_2);
        assertThat(digestValidationDetails.getStrategyId()).isEqualTo(VitamConfiguration.getDefaultStrategy());
        assertThat(digestValidationDetails.getGlobalDigest()).isEqualTo(INVALID_HASH);
        assertThat(digestValidationDetails.getDigestInDb()).isEqualTo(DIGEST_1);
        assertThat(digestValidationDetails.getDigestByOfferId()).isEqualTo(offerDigests);

        checkStats(instance, 0, 0, 0, 0, 0, 1);

        verify(alertService).createAlert(eq(VitamLogLevel.ERROR), anyString());
        verifyNoMoreInteractions(alertService);
    }

    private void checkStats(DigestValidator instance, int nbMetadataOK, int nbMetadataWarnings, int nbMetadataErrors,
        int nbObjectOK, int nbObjectWarnings, int nbObjectErrors) {

        assertThat(instance.getMetadataValidationStatistics().getNbOK()).isEqualTo(nbMetadataOK);
        assertThat(instance.getMetadataValidationStatistics().getNbWarnings()).isEqualTo(nbMetadataWarnings);
        assertThat(instance.getMetadataValidationStatistics().getNbErrors()).isEqualTo(nbMetadataErrors);

        assertThat(instance.getObjectValidationStatistics().getNbOK()).isEqualTo(nbObjectOK);
        assertThat(instance.getObjectValidationStatistics().getNbWarnings()).isEqualTo(nbObjectWarnings);
        assertThat(instance.getObjectValidationStatistics().getNbErrors()).isEqualTo(nbObjectErrors);
    }
}
