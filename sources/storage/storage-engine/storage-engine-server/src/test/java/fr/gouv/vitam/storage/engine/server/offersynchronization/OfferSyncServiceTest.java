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
package fr.gouv.vitam.storage.engine.server.offersynchronization;

import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.request.OfferPartialSyncItem;
import fr.gouv.vitam.storage.engine.server.distribution.StorageDistribution;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OfferSyncServiceTest {

    private static final String SOURCE = "source";
    private static final String TARGET = "target";
    private static final DataCategory DATA_CATEGORY = DataCategory.UNIT;
    private static final int TENANT_ID = 2;
    private static final Long OFFSET = null;

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    RestoreOfferBackupService restoreOfferBackupService;

    @Mock
    StorageDistribution distribution;

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
        OfferSyncService instance = spy(
            new OfferSyncService(restoreOfferBackupService, distribution, 1000, 16, 1, 1, 1, 1)
        );
        doNothing().when(instance).runSynchronizationAsync(any(), any(), any(), any(), anyLong(), any());
        when(instance.createOfferSyncProcess()).thenReturn(offerSyncProcess);

        // When
        boolean result = instance.startSynchronization(
            SOURCE,
            TARGET,
            VitamConfiguration.getDefaultStrategy(),
            DATA_CATEGORY,
            OFFSET
        );

        // Then
        verify(instance).runSynchronizationAsync(
            SOURCE,
            TARGET,
            VitamConfiguration.getDefaultStrategy(),
            DATA_CATEGORY,
            OFFSET,
            offerSyncProcess
        );
        assertThat(result).isTrue();
    }

    @Test
    @RunWithCustomExecutor
    public void startSynchronizationShouldFailIfAnotherProcessIsAlreadyRunning() {
        // Given
        OfferSyncProcess offerSyncProcess1 = mock(OfferSyncProcess.class);
        OfferSyncProcess offerSyncProcess2 = mock(OfferSyncProcess.class);
        when(offerSyncProcess1.isRunning()).thenReturn(true);

        OfferSyncService instance = spy(
            new OfferSyncService(restoreOfferBackupService, distribution, 1000, 16, 1, 1, 1, 1)
        );
        doNothing().when(instance).runSynchronizationAsync(any(), any(), any(), any(), anyLong(), any());
        when(instance.createOfferSyncProcess()).thenReturn(offerSyncProcess1, offerSyncProcess2);

        // When
        boolean result1 = instance.startSynchronization(
            SOURCE,
            TARGET,
            VitamConfiguration.getDefaultStrategy(),
            DATA_CATEGORY,
            OFFSET
        );
        boolean result2 = instance.startSynchronization(
            SOURCE,
            TARGET,
            VitamConfiguration.getDefaultStrategy(),
            DATA_CATEGORY,
            OFFSET
        );

        // Then
        verify(instance).runSynchronizationAsync(
            SOURCE,
            TARGET,
            VitamConfiguration.getDefaultStrategy(),
            DATA_CATEGORY,
            OFFSET,
            offerSyncProcess1
        );
        verify(instance, never()).runSynchronizationAsync(
            SOURCE,
            TARGET,
            VitamConfiguration.getDefaultStrategy(),
            DATA_CATEGORY,
            OFFSET,
            offerSyncProcess2
        );
        assertThat(result1).isTrue();
        assertThat(result2).isFalse();
    }

    @Test
    @RunWithCustomExecutor
    public void startSynchronizationShouldFailIfAnotherPartialSyncProcessIsAlreadyRunning() {
        // Given
        OfferSyncProcess offerSyncProcess1 = mock(OfferSyncProcess.class);
        OfferSyncProcess offerSyncProcess2 = mock(OfferSyncProcess.class);
        when(offerSyncProcess1.isRunning()).thenReturn(true);

        OfferSyncService instance = spy(
            new OfferSyncService(restoreOfferBackupService, distribution, 1000, 16, 1, 1, 1, 1)
        );
        doNothing().when(instance).runSynchronizationAsync(anyString(), anyString(), anyString(), anyList(), any());
        when(instance.createOfferSyncProcess()).thenReturn(offerSyncProcess1, offerSyncProcess2);

        // When
        ArrayList<OfferPartialSyncItem> items1 = new ArrayList<>();
        ArrayList<OfferPartialSyncItem> items2 = new ArrayList<>();
        boolean result1 = instance.startSynchronization(
            SOURCE,
            TARGET,
            VitamConfiguration.getDefaultStrategy(),
            items1
        );
        boolean result2 = instance.startSynchronization(
            SOURCE,
            TARGET,
            VitamConfiguration.getDefaultStrategy(),
            items2
        );

        // Then
        verify(instance).runSynchronizationAsync(
            SOURCE,
            TARGET,
            VitamConfiguration.getDefaultStrategy(),
            items1,
            offerSyncProcess1
        );
        verify(instance, never()).runSynchronizationAsync(
            SOURCE,
            TARGET,
            VitamConfiguration.getDefaultStrategy(),
            items2,
            offerSyncProcess2
        );
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

        OfferSyncService instance = spy(
            new OfferSyncService(restoreOfferBackupService, distribution, 1000, 16, 1, 1, 1, 1)
        );
        doNothing().when(instance).runSynchronizationAsync(any(), any(), any(), any(), anyLong(), any());
        when(instance.createOfferSyncProcess()).thenReturn(offerSyncProcess1, offerSyncProcess2);

        // When
        boolean result1 = instance.startSynchronization(
            SOURCE,
            TARGET,
            VitamConfiguration.getDefaultStrategy(),
            DATA_CATEGORY,
            OFFSET
        );
        boolean result2 = instance.startSynchronization(
            SOURCE,
            TARGET,
            VitamConfiguration.getDefaultStrategy(),
            DATA_CATEGORY,
            OFFSET
        );

        // Then
        verify(instance).runSynchronizationAsync(
            SOURCE,
            TARGET,
            VitamConfiguration.getDefaultStrategy(),
            DATA_CATEGORY,
            OFFSET,
            offerSyncProcess1
        );
        verify(instance).runSynchronizationAsync(
            SOURCE,
            TARGET,
            VitamConfiguration.getDefaultStrategy(),
            DATA_CATEGORY,
            OFFSET,
            offerSyncProcess2
        );
        assertThat(result1).isTrue();
        assertThat(result2).isTrue();
    }

    @Test
    @RunWithCustomExecutor
    public void isRunningShouldReturnFalseWhenNoProcessStarted() {
        // Given
        OfferSyncService instance = spy(
            new OfferSyncService(restoreOfferBackupService, distribution, 1000, 16, 1, 1, 1, 1)
        );

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

        OfferSyncService instance = spy(
            new OfferSyncService(restoreOfferBackupService, distribution, 1000, 16, 1, 1, 1, 1)
        );
        doNothing().when(instance).runSynchronizationAsync(any(), any(), any(), any(), anyLong(), any());
        when(instance.createOfferSyncProcess()).thenReturn(offerSyncProcess);

        // When
        boolean processStarted = instance.startSynchronization(
            SOURCE,
            TARGET,
            VitamConfiguration.getDefaultStrategy(),
            DATA_CATEGORY,
            OFFSET
        );
        boolean isRunning = instance.isRunning();

        // Then
        assertThat(processStarted).isTrue();
        assertThat(isRunning).isTrue();
    }

    @Test
    @RunWithCustomExecutor
    public void isRunningShouldReturnFalseWhenPreviousProcessEnded() throws Exception {
        // Given
        OfferSyncProcess offerSyncProcess = mock(OfferSyncProcess.class);
        when(offerSyncProcess.isRunning()).thenReturn(false);

        try (
            OfferSyncService instance = spy(
                new OfferSyncService(restoreOfferBackupService, distribution, 1000, 16, 1, 1, 1, 1)
            )
        ) {
            doNothing().when(instance).runSynchronizationAsync(any(), any(), any(), any(), anyLong(), any());
            when(instance.createOfferSyncProcess()).thenReturn(offerSyncProcess);

            // When
            boolean processStarted = instance.startSynchronization(
                SOURCE,
                TARGET,
                VitamConfiguration.getDefaultStrategy(),
                DATA_CATEGORY,
                OFFSET
            );
            boolean isRunning = instance.isRunning();

            // Then
            assertThat(processStarted).isTrue();
            assertThat(isRunning).isFalse();
        }
    }

    @Test
    @RunWithCustomExecutor
    public void getLastSynchronizationStatusShouldNullWhenNoProcessStarted() throws Exception {
        // Given
        try (
            OfferSyncService instance = spy(
                new OfferSyncService(restoreOfferBackupService, distribution, 1000, 16, 1, 1, 1, 1)
            )
        ) {
            // When
            OfferSyncStatus status = instance.getLastSynchronizationStatus();

            // Then
            assertThat(status).isNull();
        }
    }

    @Test
    @RunWithCustomExecutor
    public void getLastSynchronizationStatusShouldReturnStatusWhenProcessRunning() throws Exception {
        // Given
        OfferSyncStatus offerSyncStatus = mock(OfferSyncStatus.class);
        OfferSyncProcess offerSyncProcess = mock(OfferSyncProcess.class);
        when(offerSyncProcess.isRunning()).thenReturn(true);
        when(offerSyncProcess.getOfferSyncStatus()).thenReturn(offerSyncStatus);

        try (
            OfferSyncService instance = spy(
                new OfferSyncService(restoreOfferBackupService, distribution, 1000, 16, 1, 1, 1, 1)
            )
        ) {
            doNothing().when(instance).runSynchronizationAsync(any(), any(), any(), any(), anyLong(), any());
            when(instance.createOfferSyncProcess()).thenReturn(offerSyncProcess);

            // When
            boolean processStarted = instance.startSynchronization(
                SOURCE,
                TARGET,
                VitamConfiguration.getDefaultStrategy(),
                DATA_CATEGORY,
                OFFSET
            );
            OfferSyncStatus status = instance.getLastSynchronizationStatus();

            // Then
            assertThat(processStarted).isTrue();
            assertThat(status).isEqualTo(offerSyncStatus);
        }
    }

    @Test
    @RunWithCustomExecutor
    public void getLastSynchronizationStatusShouldReturnStatusWhenProcessEnded() throws Exception {
        // Given
        OfferSyncStatus offerSyncStatus = mock(OfferSyncStatus.class);
        OfferSyncProcess offerSyncProcess = mock(OfferSyncProcess.class);
        when(offerSyncProcess.isRunning()).thenReturn(false);
        when(offerSyncProcess.getOfferSyncStatus()).thenReturn(offerSyncStatus);

        try (
            OfferSyncService instance = spy(
                new OfferSyncService(restoreOfferBackupService, distribution, 1000, 16, 1, 1, 1, 1)
            )
        ) {
            doNothing().when(instance).runSynchronizationAsync(any(), any(), any(), any(), anyLong(), any());
            when(instance.createOfferSyncProcess()).thenReturn(offerSyncProcess);

            // When
            boolean processStarted = instance.startSynchronization(
                SOURCE,
                TARGET,
                VitamConfiguration.getDefaultStrategy(),
                DATA_CATEGORY,
                OFFSET
            );
            OfferSyncStatus status = instance.getLastSynchronizationStatus();

            // Then
            assertThat(processStarted).isTrue();
            assertThat(status).isEqualTo(offerSyncStatus);
        }
    }

    @Test
    @RunWithCustomExecutor
    public void runSynchronizationAsyncShouldStartSynchronization() throws Exception {
        // Given
        CountDownLatch countDownLatch = new CountDownLatch(1);
        OfferSyncProcess offerSyncProcess = mock(OfferSyncProcess.class);
        doAnswer(args -> {
            countDownLatch.countDown();
            return null;
        })
            .when(offerSyncProcess)
            .synchronize(any(), any(), any(), any(), any(), eq(OFFSET));

        try (
            OfferSyncService instance = new OfferSyncService(
                restoreOfferBackupService,
                distribution,
                1000,
                1,
                1,
                1,
                16,
                1
            )
        ) {
            // When
            instance.runSynchronizationAsync(
                SOURCE,
                TARGET,
                VitamConfiguration.getDefaultStrategy(),
                DATA_CATEGORY,
                OFFSET,
                offerSyncProcess
            );
            countDownLatch.await(1, TimeUnit.MINUTES);

            // Then
            verify(offerSyncProcess).synchronize(
                instance.getExecutor(),
                SOURCE,
                TARGET,
                VitamConfiguration.getDefaultStrategy(),
                DATA_CATEGORY,
                OFFSET
            );
        }
    }
}
