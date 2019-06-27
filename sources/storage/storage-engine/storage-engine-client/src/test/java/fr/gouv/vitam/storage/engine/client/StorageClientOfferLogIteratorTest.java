package fr.gouv.vitam.storage.engine.client;

import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.OfferLogAction;
import fr.gouv.vitam.storage.engine.common.model.Order;
import org.junit.Before;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class StorageClientOfferLogIteratorTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private StorageClientFactory storageClientFactory;
    @Mock
    private StorageClient storageClient;

    @Before
    public void init() {
        doReturn(storageClient).when(storageClientFactory).getClient();
    }

    @Test
    public void testEmpty() throws Exception {

        // Given
        StorageClientOfferLogIterator
            offerLogIterator = new StorageClientOfferLogIterator(storageClientFactory, VitamConfiguration.getDefaultStrategy(), Order.DESC,
            DataCategory.UNIT, 1000, null);
        doReturn(new RequestResponseOK<OfferLog>())
            .when(storageClient).getOfferLogs(VitamConfiguration.getDefaultStrategy(), DataCategory.UNIT, null, 1000, Order.DESC);

        // When / Then
        assertThat(offerLogIterator.hasNext()).isFalse();
        assertThatThrownBy(offerLogIterator::next).isInstanceOf(NoSuchElementException.class);

        verify(storageClient).getOfferLogs(VitamConfiguration.getDefaultStrategy(), DataCategory.UNIT, null, 1000, Order.DESC);
        verify(storageClient).close();
        verifyNoMoreInteractions(storageClient);
    }

    @Test
    public void testOnePage() throws Exception {

        // Given
        StorageClientOfferLogIterator
            offerLogIterator = new StorageClientOfferLogIterator(storageClientFactory, VitamConfiguration.getDefaultStrategy(), Order.DESC,
            DataCategory.UNIT, 1000, null);

        doReturn(new RequestResponseOK<OfferLog>().addAllResults(Arrays.asList(
            new OfferLog(500L, null, "0_unit", "file1", OfferLogAction.WRITE),
            new OfferLog(400L, null, "0_unit", "file2", OfferLogAction.WRITE),
            new OfferLog(300L, null, "0_unit", "file3", OfferLogAction.WRITE))))
            .when(storageClient).getOfferLogs(VitamConfiguration.getDefaultStrategy(), DataCategory.UNIT, null, 1000, Order.DESC);

        // When / Then
        assertThat(offerLogIterator.hasNext()).isTrue();
        assertThat(offerLogIterator.next().getFileName()).isEqualTo("file1");
        assertThat(offerLogIterator.hasNext()).isTrue();
        assertThat(offerLogIterator.next().getFileName()).isEqualTo("file2");
        assertThat(offerLogIterator.hasNext()).isTrue();
        assertThat(offerLogIterator.next().getFileName()).isEqualTo("file3");
        assertThat(offerLogIterator.hasNext()).isFalse();
        assertThatThrownBy(offerLogIterator::next).isInstanceOf(NoSuchElementException.class);

        verify(storageClient).getOfferLogs(VitamConfiguration.getDefaultStrategy(), DataCategory.UNIT, null, 1000, Order.DESC);
        verify(storageClient).close();
        verifyNoMoreInteractions(storageClient);
    }

    @Test
    public void testExactlyOnePage() throws Exception {

        // Given
        StorageClientOfferLogIterator
            offerLogIterator = new StorageClientOfferLogIterator(storageClientFactory, VitamConfiguration.getDefaultStrategy(), Order.DESC,
            DataCategory.UNIT, 2, null);

        doReturn(new RequestResponseOK<OfferLog>().addAllResults(Arrays.asList(
            new OfferLog(500L, null, "0_unit", "file1", OfferLogAction.WRITE),
            new OfferLog(400L, null, "0_unit", "file2", OfferLogAction.WRITE))))
            .when(storageClient).getOfferLogs(VitamConfiguration.getDefaultStrategy(), DataCategory.UNIT, null, 2, Order.DESC);

        doReturn(new RequestResponseOK<OfferLog>())
            .when(storageClient).getOfferLogs(VitamConfiguration.getDefaultStrategy(), DataCategory.UNIT, 399L, 2, Order.DESC);

        // When / Then
        assertThat(offerLogIterator.hasNext()).isTrue();
        assertThat(offerLogIterator.next().getFileName()).isEqualTo("file1");
        assertThat(offerLogIterator.hasNext()).isTrue();
        assertThat(offerLogIterator.next().getFileName()).isEqualTo("file2");
        assertThat(offerLogIterator.hasNext()).isFalse();
        assertThatThrownBy(offerLogIterator::next).isInstanceOf(NoSuchElementException.class);

        verify(storageClient).getOfferLogs(VitamConfiguration.getDefaultStrategy(), DataCategory.UNIT, null, 2, Order.DESC);
        verify(storageClient).getOfferLogs(VitamConfiguration.getDefaultStrategy(), DataCategory.UNIT, 399L, 2, Order.DESC);
        verify(storageClient, times(2)).close();
        verifyNoMoreInteractions(storageClient);
    }

    @Test
    public void testMultiPages() throws Exception {

        // Given
        StorageClientOfferLogIterator
            offerLogIterator =
            new StorageClientOfferLogIterator(storageClientFactory, VitamConfiguration.getDefaultStrategy(), Order.DESC, DataCategory.UNIT,
                2, null);

        doReturn(new RequestResponseOK<OfferLog>().addAllResults(Arrays.asList(
            new OfferLog(500L, null, "0_unit", "file1", OfferLogAction.WRITE),
            new OfferLog(400L, null, "0_unit", "file2", OfferLogAction.WRITE))))
            .when(storageClient).getOfferLogs(VitamConfiguration.getDefaultStrategy(), DataCategory.UNIT, null, 2, Order.DESC);

        doReturn(new RequestResponseOK<OfferLog>().addAllResults(Arrays.asList(
            new OfferLog(300L, null, "0_unit", "file3", OfferLogAction.WRITE))))
            .when(storageClient).getOfferLogs(VitamConfiguration.getDefaultStrategy(), DataCategory.UNIT, 399L, 2, Order.DESC);

        // When / Then
        assertThat(offerLogIterator.hasNext()).isTrue();
        assertThat(offerLogIterator.next().getFileName()).isEqualTo("file1");
        assertThat(offerLogIterator.hasNext()).isTrue();
        assertThat(offerLogIterator.next().getFileName()).isEqualTo("file2");
        assertThat(offerLogIterator.hasNext()).isTrue();
        assertThat(offerLogIterator.next().getFileName()).isEqualTo("file3");
        assertThat(offerLogIterator.hasNext()).isFalse();
        assertThatThrownBy(offerLogIterator::next).isInstanceOf(NoSuchElementException.class);

        verify(storageClient).getOfferLogs(VitamConfiguration.getDefaultStrategy(), DataCategory.UNIT, null, 2, Order.DESC);
        verify(storageClient).getOfferLogs(VitamConfiguration.getDefaultStrategy(), DataCategory.UNIT, 399L, 2, Order.DESC);
        verify(storageClient, times(2)).close();
        verifyNoMoreInteractions(storageClient);
    }

    @Test
    public void testOnePageWithOffset() throws Exception {

        // Given
        StorageClientOfferLogIterator
            offerLogIterator = new StorageClientOfferLogIterator(storageClientFactory, VitamConfiguration.getDefaultStrategy(), Order.DESC,
            DataCategory.UNIT, 1000, 600L);

        doReturn(new RequestResponseOK<OfferLog>().addAllResults(Arrays.asList(
            new OfferLog(500L, null, "0_unit", "file1", OfferLogAction.WRITE),
            new OfferLog(400L, null, "0_unit", "file2", OfferLogAction.WRITE),
            new OfferLog(300L, null, "0_unit", "file3", OfferLogAction.WRITE))))
            .when(storageClient).getOfferLogs(VitamConfiguration.getDefaultStrategy(), DataCategory.UNIT, 600L, 1000, Order.DESC);

        // When / Then
        assertThat(offerLogIterator.hasNext()).isTrue();
        assertThat(offerLogIterator.next().getFileName()).isEqualTo("file1");
        assertThat(offerLogIterator.hasNext()).isTrue();
        assertThat(offerLogIterator.next().getFileName()).isEqualTo("file2");
        assertThat(offerLogIterator.hasNext()).isTrue();
        assertThat(offerLogIterator.next().getFileName()).isEqualTo("file3");
        assertThat(offerLogIterator.hasNext()).isFalse();
        assertThatThrownBy(offerLogIterator::next).isInstanceOf(NoSuchElementException.class);

        verify(storageClient).getOfferLogs(VitamConfiguration.getDefaultStrategy(), DataCategory.UNIT, 600L, 1000, Order.DESC);
        verify(storageClient).close();
        verifyNoMoreInteractions(storageClient);
    }
}
