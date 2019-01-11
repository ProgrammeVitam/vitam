package fr.gouv.vitam.storage.engine.server.offersynchronization;

import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.server.distribution.StorageDistribution;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OfferSyncServiceTest {

    private static final String SOURCE = "source";
    private static final String TARGET = "target";
    private static final DataCategory DATA_CATEGORY = DataCategory.UNIT;
    private static final int TENANT_ID = 2;
    private static final Long OFFSET = null;

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @Mock RestoreOfferBackupService restoreOfferBackupService;
    @Mock StorageDistribution distribution;

    @Before
    public void setup() {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(TENANT_ID));
    }

    @Test
    @RunWithCustomExecutor
    public void startSynchronizationShouldSucceedOnFirstStart() {

        // Given
        OfferSyncProcess offerSyncProcess = mock(OfferSyncProcess.class);
        OfferSyncService instance = spy(new OfferSyncService(restoreOfferBackupService, distribution, 1000, 16));
        doNothing().when(instance).runSynchronizationAsync(any(), any(), any(), anyLong(), any());
        when(instance.createOfferSyncProcess()).thenReturn(offerSyncProcess);

        // When
        boolean result = instance.startSynchronization(SOURCE, TARGET, DATA_CATEGORY, OFFSET);

        // Then
        verify(instance).runSynchronizationAsync(SOURCE, TARGET, DATA_CATEGORY, OFFSET, offerSyncProcess);
        assertThat(result).isTrue();
    }

    @Test
    @RunWithCustomExecutor
    public void startSynchronizationShouldFailIfAnotherProcessIsAlreadyRunning() {

        // Given
        OfferSyncProcess offerSyncProcess1 = mock(OfferSyncProcess.class);
        OfferSyncProcess offerSyncProcess2 = mock(OfferSyncProcess.class);
        when(offerSyncProcess1.isRunning()).thenReturn(true);

        OfferSyncService instance = spy(new OfferSyncService(restoreOfferBackupService, distribution, 1000, 16));
        doNothing().when(instance).runSynchronizationAsync(any(), any(), any(), anyLong(), any());
        when(instance.createOfferSyncProcess()).thenReturn(offerSyncProcess1, offerSyncProcess2);

        // When
        boolean result1 = instance.startSynchronization(SOURCE, TARGET, DATA_CATEGORY, OFFSET);
        boolean result2 = instance.startSynchronization(SOURCE, TARGET, DATA_CATEGORY, OFFSET);

        // Then
        verify(instance).runSynchronizationAsync(SOURCE, TARGET, DATA_CATEGORY, OFFSET, offerSyncProcess1);
        verify(instance, never())
            .runSynchronizationAsync(SOURCE, TARGET, DATA_CATEGORY, OFFSET, offerSyncProcess2);
        assertThat(result1).isTrue();
        assertThat(result2).isFalse();
    }

    @Test
    @RunWithCustomExecutor
    public void startSynchronizationShouldSuccessIfPreviousProcessEnded() {

        // Given
        OfferSyncProcess offerSyncProcess1 = mock(OfferSyncProcess.class);
        OfferSyncProcess offerSyncProcess2 = mock(OfferSyncProcess.class);
        when(offerSyncProcess1.isRunning()).thenReturn(false);

        OfferSyncService instance = spy(new OfferSyncService(restoreOfferBackupService, distribution, 1000, 16));
        doNothing().when(instance).runSynchronizationAsync(any(), any(), any(), anyLong(), any());
        when(instance.createOfferSyncProcess()).thenReturn(offerSyncProcess1, offerSyncProcess2);

        // When
        boolean result1 = instance.startSynchronization(SOURCE, TARGET, DATA_CATEGORY, OFFSET);
        boolean result2 = instance.startSynchronization(SOURCE, TARGET, DATA_CATEGORY, OFFSET);

        // Then
        verify(instance).runSynchronizationAsync(SOURCE, TARGET, DATA_CATEGORY, OFFSET, offerSyncProcess1);
        verify(instance).runSynchronizationAsync(SOURCE, TARGET, DATA_CATEGORY, OFFSET, offerSyncProcess2);
        assertThat(result1).isTrue();
        assertThat(result2).isTrue();
    }

    @Test
    @RunWithCustomExecutor
    public void isRunningShouldReturnFalseWhenNoProcessStarted() {

        // Given
        OfferSyncService instance = spy(new OfferSyncService(restoreOfferBackupService, distribution, 1000, 16));

        // When
        boolean isRunning = instance.isRunning();

        // Then
        assertThat(isRunning).isFalse();
    }

    @Test
    @RunWithCustomExecutor
    public void isRunningShouldReturnTrueWhenProcessRunning() {

        // Given
        OfferSyncProcess offerSyncProcess = mock(OfferSyncProcess.class);
        when(offerSyncProcess.isRunning()).thenReturn(true);

        OfferSyncService instance = spy(new OfferSyncService(restoreOfferBackupService, distribution, 1000, 16));
        doNothing().when(instance).runSynchronizationAsync(any(), any(), any(), anyLong(), any());
        when(instance.createOfferSyncProcess()).thenReturn(offerSyncProcess);

        // When
        boolean processStarted = instance.startSynchronization(SOURCE, TARGET, DATA_CATEGORY, OFFSET);
        boolean isRunning = instance.isRunning();

        // Then
        assertThat(processStarted).isTrue();
        assertThat(isRunning).isTrue();
    }

    @Test
    @RunWithCustomExecutor
    public void isRunningShouldReturnFalseWhenPreviousProcessEnded() {

        // Given
        OfferSyncProcess offerSyncProcess = mock(OfferSyncProcess.class);
        when(offerSyncProcess.isRunning()).thenReturn(false);

        OfferSyncService instance = spy(new OfferSyncService(restoreOfferBackupService, distribution, 1000, 16));
        doNothing().when(instance).runSynchronizationAsync(any(), any(), any(), anyLong(), any());
        when(instance.createOfferSyncProcess()).thenReturn(offerSyncProcess);

        // When
        boolean processStarted = instance.startSynchronization(SOURCE, TARGET, DATA_CATEGORY, OFFSET);
        boolean isRunning = instance.isRunning();

        // Then
        assertThat(processStarted).isTrue();
        assertThat(isRunning).isFalse();
    }

    @Test
    @RunWithCustomExecutor
    public void getLastSynchronizationStatusShouldNullWhenNoProcessStarted() {

        // Given
        OfferSyncService instance = spy(new OfferSyncService(restoreOfferBackupService, distribution, 1000, 16));

        // When
        OfferSyncStatus status = instance.getLastSynchronizationStatus();

        // Then
        assertThat(status).isNull();
    }

    @Test
    @RunWithCustomExecutor
    public void getLastSynchronizationStatusShouldReturnStatusWhenProcessRunning() {

        // Given
        OfferSyncStatus offerSyncStatus = mock(OfferSyncStatus.class);
        OfferSyncProcess offerSyncProcess = mock(OfferSyncProcess.class);
        when(offerSyncProcess.isRunning()).thenReturn(true);
        when(offerSyncProcess.getOfferSyncStatus()).thenReturn(offerSyncStatus);


        OfferSyncService instance = spy(new OfferSyncService(restoreOfferBackupService, distribution, 1000, 16));
        doNothing().when(instance).runSynchronizationAsync(any(), any(), any(), anyLong(), any());
        when(instance.createOfferSyncProcess()).thenReturn(offerSyncProcess);

        // When
        boolean processStarted = instance.startSynchronization(SOURCE, TARGET, DATA_CATEGORY, OFFSET);
        OfferSyncStatus status = instance.getLastSynchronizationStatus();

        // Then
        assertThat(processStarted).isTrue();
        assertThat(status).isEqualTo(offerSyncStatus);
    }

    @Test
    @RunWithCustomExecutor
    public void getLastSynchronizationStatusShouldReturnStatusWhenProcessEnded() {

        // Given
        OfferSyncStatus offerSyncStatus = mock(OfferSyncStatus.class);
        OfferSyncProcess offerSyncProcess = mock(OfferSyncProcess.class);
        when(offerSyncProcess.isRunning()).thenReturn(false);
        when(offerSyncProcess.getOfferSyncStatus()).thenReturn(offerSyncStatus);


        OfferSyncService instance = spy(new OfferSyncService(restoreOfferBackupService, distribution, 1000, 16));
        doNothing().when(instance).runSynchronizationAsync(any(), any(), any(), anyLong(), any());
        when(instance.createOfferSyncProcess()).thenReturn(offerSyncProcess);

        // When
        boolean processStarted = instance.startSynchronization(SOURCE, TARGET, DATA_CATEGORY, OFFSET);
        OfferSyncStatus status = instance.getLastSynchronizationStatus();

        // Then
        assertThat(processStarted).isTrue();
        assertThat(status).isEqualTo(offerSyncStatus);
    }

    @Test
    @RunWithCustomExecutor
    public void runSynchronizationAsyncShouldStartSynchronization() throws Exception {

        // Given
        CountDownLatch countDownLatch = new CountDownLatch(1);
        OfferSyncProcess offerSyncProcess = mock(OfferSyncProcess.class);
        doAnswer((args) -> {
            countDownLatch.countDown();
            return null;
        }).when(offerSyncProcess).synchronize(any(), any(), any(), anyLong());

        OfferSyncService instance = new OfferSyncService(restoreOfferBackupService, distribution, 1000, 16);

        // When
        instance.runSynchronizationAsync(SOURCE, TARGET, DATA_CATEGORY, OFFSET, offerSyncProcess);
        countDownLatch.await(1, TimeUnit.MINUTES);

        // Then
        verify(offerSyncProcess).synchronize(SOURCE, TARGET, DATA_CATEGORY, OFFSET);
    }
}
