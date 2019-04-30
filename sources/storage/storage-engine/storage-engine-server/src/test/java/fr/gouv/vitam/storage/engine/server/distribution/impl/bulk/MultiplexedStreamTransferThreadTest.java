package fr.gouv.vitam.storage.engine.server.distribution.impl.bulk;

import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.storage.driver.Connection;
import fr.gouv.vitam.storage.driver.Driver;
import fr.gouv.vitam.storage.driver.exception.StorageDriverException;
import fr.gouv.vitam.storage.driver.model.StorageBulkPutRequest;
import fr.gouv.vitam.storage.driver.model.StorageBulkPutResult;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageOffer;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.InputStream;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class MultiplexedStreamTransferThreadTest {

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @RunWithCustomExecutor
    @Test
    public void testTransferThread() throws Exception {

        // Given
        InputStream is = mock(InputStream.class);
        Driver driver = mock(Driver.class);
        StorageOffer storageOffer = mock(StorageOffer.class);
        Connection connection = mock(Connection.class);
        doReturn("OfferId").when(storageOffer).getId();
        doReturn(connection).when(driver).connect("OfferId");
        StorageBulkPutResult result = mock(StorageBulkPutResult.class);
        doReturn(result).when(connection).bulkPutObjects(any(StorageBulkPutRequest.class));

        MultiplexedStreamTransferThread multiplexedStreamTransferThread = new MultiplexedStreamTransferThread(
            2, DataCategory.UNIT, Arrays.asList("ob1", "ob2"), is, 100L, driver, storageOffer, DigestType.SHA512);

        // When
        StorageBulkPutResult storageBulkPutResult = multiplexedStreamTransferThread.call();

        // Then
        ArgumentCaptor<StorageBulkPutRequest> storageBulkPutRequest =
            ArgumentCaptor.forClass(StorageBulkPutRequest.class);
        verify(connection).bulkPutObjects(storageBulkPutRequest.capture());
        assertThat(storageBulkPutRequest.getValue().getObjectIds()).containsExactly("ob1", "ob2");
        assertThat(storageBulkPutRequest.getValue().getDataStream()).isEqualTo(is);
        assertThat(storageBulkPutRequest.getValue().getSize()).isEqualTo(100L);
        assertThat(storageBulkPutRequest.getValue().getDigestType()).isEqualTo(DigestType.SHA512);
        assertThat(storageBulkPutRequest.getValue().getTenantId()).isEqualTo(2);

        assertThat(storageBulkPutResult).isSameAs(result);

        verify(connection).close();
        verify(is).close();
    }

    @RunWithCustomExecutor
    @Test
    public void testTransferThreadWithErrorDuringConnection() throws Exception {

        // Given
        InputStream is = mock(InputStream.class);
        Driver driver = mock(Driver.class);
        StorageOffer storageOffer = mock(StorageOffer.class);
        doReturn("OfferId").when(storageOffer).getId();

        Exception ex = mock(StorageDriverException.class);
        doThrow(ex).when(driver).connect("OfferId");

        MultiplexedStreamTransferThread multiplexedStreamTransferThread = new MultiplexedStreamTransferThread(
            2, DataCategory.UNIT, Arrays.asList("ob1", "ob2"), is, 100L, driver, storageOffer, DigestType.SHA512);

        // When / When
        assertThatThrownBy(multiplexedStreamTransferThread::call)
            .isEqualTo(ex);

        verify(is).close();
    }

    @RunWithCustomExecutor
    @Test
    public void testTransferThreadWithErrorDuringTransfer() throws Exception {

        // Given
        InputStream is = mock(InputStream.class);
        Driver driver = mock(Driver.class);
        StorageOffer storageOffer = mock(StorageOffer.class);
        Connection connection = mock(Connection.class);
        doReturn("OfferId").when(storageOffer).getId();
        doReturn(connection).when(driver).connect("OfferId");

        Exception ex = mock(StorageDriverException.class);
        doThrow(ex).when(connection).bulkPutObjects(any(StorageBulkPutRequest.class));

        MultiplexedStreamTransferThread multiplexedStreamTransferThread = new MultiplexedStreamTransferThread(
            2, DataCategory.UNIT, Arrays.asList("ob1", "ob2"), is, 100L, driver, storageOffer, DigestType.SHA512);

        // When / When
        assertThatThrownBy(multiplexedStreamTransferThread::call)
            .isEqualTo(ex);

        verify(connection).close();
        verify(is).close();
    }
}
