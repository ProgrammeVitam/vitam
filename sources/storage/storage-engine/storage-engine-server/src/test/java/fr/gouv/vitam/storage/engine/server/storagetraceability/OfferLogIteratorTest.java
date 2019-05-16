package fr.gouv.vitam.storage.engine.server.storagetraceability;

import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.OfferLogAction;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.storage.engine.server.distribution.StorageDistribution;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class OfferLogIteratorTest {

    private static final String STRATEGY = "default";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private StorageDistribution storageDistribution;

    @Test
    public void testEmpty() throws Exception {

        // Given
        OfferLogIterator
            offerLogIterator = new OfferLogIterator(STRATEGY, Order.DESC, DataCategory.UNIT,
            storageDistribution, 1000);
        doReturn(new RequestResponseOK<OfferLog>())
            .when(storageDistribution).getOfferLogs(STRATEGY, DataCategory.UNIT, null, 1000, Order.DESC);

        // When / Then
        assertThat(offerLogIterator.hasNext()).isFalse();
        assertThatThrownBy(offerLogIterator::next).isInstanceOf(NoSuchElementException.class);

        verify(storageDistribution).getOfferLogs(STRATEGY, DataCategory.UNIT, null, 1000, Order.DESC);
        verifyNoMoreInteractions(storageDistribution);
    }

    @Test
    public void testOnePage() throws Exception {

        // Given
        OfferLogIterator
            offerLogIterator = new OfferLogIterator(STRATEGY, Order.DESC, DataCategory.UNIT,
            storageDistribution, 1000);

        doReturn(new RequestResponseOK<OfferLog>().addAllResults(Arrays.asList(
            new OfferLog(500L, null, "0_unit", "file1", OfferLogAction.WRITE),
            new OfferLog(400L, null, "0_unit", "file2", OfferLogAction.WRITE),
            new OfferLog(300L, null, "0_unit", "file3", OfferLogAction.WRITE))))
            .when(storageDistribution).getOfferLogs(STRATEGY, DataCategory.UNIT, null, 1000, Order.DESC);

        // When / Then
        assertThat(offerLogIterator.hasNext()).isTrue();
        assertThat(offerLogIterator.next().getFileName()).isEqualTo("file1");
        assertThat(offerLogIterator.hasNext()).isTrue();
        assertThat(offerLogIterator.next().getFileName()).isEqualTo("file2");
        assertThat(offerLogIterator.hasNext()).isTrue();
        assertThat(offerLogIterator.next().getFileName()).isEqualTo("file3");
        assertThat(offerLogIterator.hasNext()).isFalse();
        assertThatThrownBy(offerLogIterator::next).isInstanceOf(NoSuchElementException.class);

        verify(storageDistribution).getOfferLogs(STRATEGY, DataCategory.UNIT, null, 1000, Order.DESC);
        verifyNoMoreInteractions(storageDistribution);
    }

    @Test
    public void testExactlyOnePage() throws Exception {

        // Given
        OfferLogIterator
            offerLogIterator = new OfferLogIterator(STRATEGY, Order.DESC, DataCategory.UNIT,
            storageDistribution, 2);

        doReturn(new RequestResponseOK<OfferLog>().addAllResults(Arrays.asList(
            new OfferLog(500L, null, "0_unit", "file1", OfferLogAction.WRITE),
            new OfferLog(400L, null, "0_unit", "file2", OfferLogAction.WRITE))))
            .when(storageDistribution).getOfferLogs(STRATEGY, DataCategory.UNIT, null, 2, Order.DESC);

        doReturn(new RequestResponseOK<OfferLog>())
            .when(storageDistribution).getOfferLogs(STRATEGY, DataCategory.UNIT, 399L, 2, Order.DESC);

        // When / Then
        assertThat(offerLogIterator.hasNext()).isTrue();
        assertThat(offerLogIterator.next().getFileName()).isEqualTo("file1");
        assertThat(offerLogIterator.hasNext()).isTrue();
        assertThat(offerLogIterator.next().getFileName()).isEqualTo("file2");
        assertThat(offerLogIterator.hasNext()).isFalse();
        assertThatThrownBy(offerLogIterator::next).isInstanceOf(NoSuchElementException.class);

        verify(storageDistribution).getOfferLogs(STRATEGY, DataCategory.UNIT, null, 2, Order.DESC);
        verify(storageDistribution).getOfferLogs(STRATEGY, DataCategory.UNIT, 399L, 2, Order.DESC);
        verifyNoMoreInteractions(storageDistribution);
    }

    @Test
    public void testMultiPages() throws Exception {

        // Given
        OfferLogIterator
            offerLogIterator = new OfferLogIterator(STRATEGY, Order.DESC, DataCategory.UNIT,
            storageDistribution, 2);

        doReturn(new RequestResponseOK<OfferLog>().addAllResults(Arrays.asList(
            new OfferLog(500L, null, "0_unit", "file1", OfferLogAction.WRITE),
            new OfferLog(400L, null, "0_unit", "file2", OfferLogAction.WRITE))))
            .when(storageDistribution).getOfferLogs(STRATEGY, DataCategory.UNIT, null, 2, Order.DESC);

        doReturn(new RequestResponseOK<OfferLog>().addAllResults(Arrays.asList(
            new OfferLog(300L, null, "0_unit", "file3", OfferLogAction.WRITE))))
            .when(storageDistribution).getOfferLogs(STRATEGY, DataCategory.UNIT, 399L, 2, Order.DESC);

        // When / Then
        assertThat(offerLogIterator.hasNext()).isTrue();
        assertThat(offerLogIterator.next().getFileName()).isEqualTo("file1");
        assertThat(offerLogIterator.hasNext()).isTrue();
        assertThat(offerLogIterator.next().getFileName()).isEqualTo("file2");
        assertThat(offerLogIterator.hasNext()).isTrue();
        assertThat(offerLogIterator.next().getFileName()).isEqualTo("file3");
        assertThat(offerLogIterator.hasNext()).isFalse();
        assertThatThrownBy(offerLogIterator::next).isInstanceOf(NoSuchElementException.class);

        verify(storageDistribution).getOfferLogs(STRATEGY, DataCategory.UNIT, null, 2, Order.DESC);
        verify(storageDistribution).getOfferLogs(STRATEGY, DataCategory.UNIT, 399L, 2, Order.DESC);
        verifyNoMoreInteractions(storageDistribution);
    }
}
