package fr.gouv.vitam.worker.common.utils;

import fr.gouv.vitam.common.alert.AlertService;
import fr.gouv.vitam.common.logging.VitamLogLevel;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.assertThatThrownBy;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class StorageClientUtilTest {

    @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock private AlertService alertService;

    private static final String DIGEST_1 = "digest1";
    private static final String DIGEST_2 = "digest2";

    @Test
    public void given_no_digest_should_throw_exception() {

        // Given
        Map<String, String> offerDigests = new HashMap<>();

        // When / Then
        assertThatThrownBy(
            () -> StorageClientUtil.aggregateOfferDigests(offerDigests, DataCategory.UNIT, "objectId", alertService)
        ).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void given_single_digest_should_return_digest() {

        // Given
        Map<String, String> offerDigests = new HashMap<>();
        offerDigests.put("offer1", DIGEST_1);

        // When
        String digest =
            StorageClientUtil.aggregateOfferDigests(offerDigests, DataCategory.UNIT, "objectId", alertService);

        // Then
        assertThat(digest).isEqualTo(DIGEST_1);
        verifyNoMoreInteractions(alertService);
    }

    @Test
    public void given_multiple_similar_digest_should_return_digest() {

        // Given
        Map<String, String> offerDigests = new HashMap<>();
        offerDigests.put("offer1", DIGEST_1);
        offerDigests.put("offer2", DIGEST_1);
        offerDigests.put("offer3", DIGEST_1);

        // When
        String digest =
            StorageClientUtil.aggregateOfferDigests(offerDigests, DataCategory.UNIT, "objectId", alertService);

        // Then
        assertThat(digest).isEqualTo(DIGEST_1);
        verifyNoMoreInteractions(alertService);
    }

    @Test
    public void given_distinct_digests_should_return_digest() {

        // Given
        Map<String, String> offerDigests = new HashMap<>();
        offerDigests.put("offer1", DIGEST_1);
        offerDigests.put("offer2", DIGEST_2);
        offerDigests.put("offer3", DIGEST_1);

        // When
        String digest =
            StorageClientUtil.aggregateOfferDigests(offerDigests, DataCategory.UNIT, "objectId", alertService);

        // Then
        assertThat(digest).isEqualTo(StorageClientUtil.UNKNOWN_HASH);
        verify(alertService).createAlert(eq(VitamLogLevel.ERROR), anyString());
        verifyNoMoreInteractions(alertService);
    }

    @Test
    public void given_null_digests_should_return_digest() {

        // Given
        Map<String, String> offerDigests = new HashMap<>();
        offerDigests.put("offer1", null);

        // When
        String digest =
            StorageClientUtil.aggregateOfferDigests(offerDigests, DataCategory.UNIT, "objectId", alertService);

        // Then
        assertThat(digest).isEqualTo(StorageClientUtil.UNKNOWN_HASH);
        verify(alertService).createAlert(eq(VitamLogLevel.ERROR), anyString());
        verifyNoMoreInteractions(alertService);
    }

    @Test
    public void given_null_and_non_null_digests_should_return_digest() {

        // Given
        Map<String, String> offerDigests = new HashMap<>();
        offerDigests.put("offer1", null);
        offerDigests.put("offer2", DIGEST_1);

        // When
        String digest =
            StorageClientUtil.aggregateOfferDigests(offerDigests, DataCategory.UNIT, "objectId", alertService);

        // Then
        assertThat(digest).isEqualTo(StorageClientUtil.UNKNOWN_HASH);
        verify(alertService).createAlert(eq(VitamLogLevel.ERROR), anyString());
        verifyNoMoreInteractions(alertService);
    }

    @Test
    public void given_null_and_distinct_digests_should_return_digest() {

        // Given
        Map<String, String> offerDigests = new HashMap<>();
        offerDigests.put("offer1", null);
        offerDigests.put("offer2", DIGEST_1);
        offerDigests.put("offer3", DIGEST_2);

        // When
        String digest =
            StorageClientUtil.aggregateOfferDigests(offerDigests, DataCategory.UNIT, "objectId", alertService);

        // Then
        assertThat(digest).isEqualTo(StorageClientUtil.UNKNOWN_HASH);
        verify(alertService, times(2)).createAlert(eq(VitamLogLevel.ERROR), anyString());
        verifyNoMoreInteractions(alertService);
    }
}
