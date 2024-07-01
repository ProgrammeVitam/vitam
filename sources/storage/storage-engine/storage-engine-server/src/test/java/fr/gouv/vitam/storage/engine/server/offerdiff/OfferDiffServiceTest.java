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

package fr.gouv.vitam.storage.engine.server.offerdiff;

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
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OfferDiffServiceTest {

    private static final String OFFER1 = "offer1";
    private static final String OFFER2 = "offer2";
    private static final DataCategory DATA_CATEGORY = DataCategory.UNIT;
    private static final int TENANT_ID = 2;

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    StorageDistribution distribution;

    @Before
    public void setup() {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(TENANT_ID));
    }

    @Test
    @RunWithCustomExecutor
    public void startDiffShouldSucceedOnFirstStart() {
        // Given
        OfferDiffProcess offerDiffProcess = mock(OfferDiffProcess.class);
        OfferDiffService instance = spy(new OfferDiffService(distribution));
        when(instance.createOfferDiffProcess(OFFER1, OFFER2, DATA_CATEGORY)).thenReturn(offerDiffProcess);

        // When
        boolean result = instance.startOfferDiff(OFFER1, OFFER2, DATA_CATEGORY);

        // Then
        assertThat(result).isTrue();
        verify(instance).runDiffAsync(offerDiffProcess);
    }

    @Test
    @RunWithCustomExecutor
    public void startDiffShouldFailIfAnotherProcessIsAlreadyRunning() {
        // Given
        OfferDiffProcess offerDiffProcess1 = mock(OfferDiffProcess.class);
        OfferDiffProcess offerDiffProcess2 = mock(OfferDiffProcess.class);
        when(offerDiffProcess1.isRunning()).thenReturn(true);

        OfferDiffService instance = spy(new OfferDiffService(distribution));
        doNothing().when(instance).runDiffAsync(offerDiffProcess1);
        when(instance.createOfferDiffProcess(OFFER1, OFFER2, DATA_CATEGORY)).thenReturn(
            offerDiffProcess1,
            offerDiffProcess2
        );

        // When
        boolean result1 = instance.startOfferDiff(OFFER1, OFFER2, DATA_CATEGORY);
        boolean result2 = instance.startOfferDiff(OFFER1, OFFER2, DATA_CATEGORY);

        // Then
        assertThat(result1).isTrue();
        assertThat(result2).isFalse();
        verify(instance).runDiffAsync(offerDiffProcess1);
        verify(instance, never()).runDiffAsync(offerDiffProcess2);
    }

    @Test
    @RunWithCustomExecutor
    public void startDiffShouldSuccessIfPreviousProcessEnded() {
        // Given
        OfferDiffProcess offerDiffProcess1 = mock(OfferDiffProcess.class);
        OfferDiffProcess offerDiffProcess2 = mock(OfferDiffProcess.class);
        when(offerDiffProcess1.isRunning()).thenReturn(false);

        OfferDiffService instance = spy(new OfferDiffService(distribution));
        doNothing().when(instance).runDiffAsync(offerDiffProcess1);
        when(instance.createOfferDiffProcess(OFFER1, OFFER2, DATA_CATEGORY)).thenReturn(
            offerDiffProcess1,
            offerDiffProcess2
        );

        // When
        boolean result1 = instance.startOfferDiff(OFFER1, OFFER2, DATA_CATEGORY);
        boolean result2 = instance.startOfferDiff(OFFER1, OFFER2, DATA_CATEGORY);

        // Then
        assertThat(result1).isTrue();
        assertThat(result2).isTrue();
        verify(instance).runDiffAsync(offerDiffProcess1);
        verify(instance).runDiffAsync(offerDiffProcess2);
    }

    @Test
    @RunWithCustomExecutor
    public void isRunningShouldReturnFalseWhenNoProcessStarted() {
        // Given
        OfferDiffService instance = new OfferDiffService(distribution);

        // When
        boolean isRunning = instance.isRunning();

        // Then
        assertThat(isRunning).isFalse();
    }

    @Test
    @RunWithCustomExecutor
    public void isRunningShouldReturnTrueWhenProcessRunning() {
        // Given
        OfferDiffProcess offerDiffProcess = mock(OfferDiffProcess.class);
        when(offerDiffProcess.isRunning()).thenReturn(true);

        OfferDiffService instance = spy(new OfferDiffService(distribution));
        doNothing().when(instance).runDiffAsync(offerDiffProcess);
        when(instance.createOfferDiffProcess(OFFER1, OFFER2, DATA_CATEGORY)).thenReturn(offerDiffProcess);

        // When
        boolean processStarted = instance.startOfferDiff(OFFER1, OFFER2, DATA_CATEGORY);
        boolean isRunning = instance.isRunning();

        // Then
        assertThat(processStarted).isTrue();
        assertThat(isRunning).isTrue();
    }

    @Test
    @RunWithCustomExecutor
    public void isRunningShouldReturnFalseWhenPreviousProcessEnded() {
        // Given
        OfferDiffProcess offerDiffProcess = mock(OfferDiffProcess.class);
        when(offerDiffProcess.isRunning()).thenReturn(false);

        OfferDiffService instance = spy(new OfferDiffService(distribution));
        doNothing().when(instance).runDiffAsync(offerDiffProcess);
        when(instance.createOfferDiffProcess(OFFER1, OFFER2, DATA_CATEGORY)).thenReturn(offerDiffProcess);

        // When
        boolean processStarted = instance.startOfferDiff(OFFER1, OFFER2, DATA_CATEGORY);
        boolean isRunning = instance.isRunning();

        // Then
        assertThat(processStarted).isTrue();
        assertThat(isRunning).isFalse();
    }

    @Test
    @RunWithCustomExecutor
    public void getLastDiffStatusShouldNullWhenNoProcessStarted() {
        // Given
        OfferDiffService instance = spy(new OfferDiffService(distribution));

        // When
        OfferDiffStatus status = instance.getLastOfferDiffStatus();

        // Then
        assertThat(status).isNull();
    }

    @Test
    @RunWithCustomExecutor
    public void getLastDiffStatusShouldReturnStatusWhenProcessRunning() {
        // Given
        OfferDiffStatus offerDiffStatus = mock(OfferDiffStatus.class);
        OfferDiffProcess offerDiffProcess = mock(OfferDiffProcess.class);
        when(offerDiffProcess.isRunning()).thenReturn(true);
        when(offerDiffProcess.getOfferDiffStatus()).thenReturn(offerDiffStatus);

        OfferDiffService instance = spy(new OfferDiffService(distribution));
        doNothing().when(instance).runDiffAsync(offerDiffProcess);
        when(instance.createOfferDiffProcess(OFFER1, OFFER2, DATA_CATEGORY)).thenReturn(offerDiffProcess);

        // When
        boolean processStarted = instance.startOfferDiff(OFFER1, OFFER2, DATA_CATEGORY);
        OfferDiffStatus status = instance.getLastOfferDiffStatus();

        // Then
        assertThat(processStarted).isTrue();
        assertThat(status).isEqualTo(offerDiffStatus);
    }

    @Test
    @RunWithCustomExecutor
    public void getLastDiffStatusShouldReturnStatusWhenProcessEnded() {
        // Given
        OfferDiffStatus offerDiffStatus = mock(OfferDiffStatus.class);
        OfferDiffProcess offerDiffProcess = mock(OfferDiffProcess.class);
        when(offerDiffProcess.isRunning()).thenReturn(false);
        when(offerDiffProcess.getOfferDiffStatus()).thenReturn(offerDiffStatus);

        OfferDiffService instance = spy(new OfferDiffService(distribution));
        doNothing().when(instance).runDiffAsync(offerDiffProcess);
        when(instance.createOfferDiffProcess(OFFER1, OFFER2, DATA_CATEGORY)).thenReturn(offerDiffProcess);

        // When
        boolean processStarted = instance.startOfferDiff(OFFER1, OFFER2, DATA_CATEGORY);
        OfferDiffStatus status = instance.getLastOfferDiffStatus();

        // Then
        assertThat(processStarted).isTrue();
        assertThat(status).isEqualTo(offerDiffStatus);
    }

    @Test
    @RunWithCustomExecutor
    public void runDiffAsyncShouldStartDiff() throws Exception {
        // Given
        CountDownLatch countDownLatch = new CountDownLatch(1);
        OfferDiffProcess offerDiffProcess = mock(OfferDiffProcess.class);
        doAnswer(args -> {
            countDownLatch.countDown();
            return null;
        })
            .when(offerDiffProcess)
            .run();

        OfferDiffService instance = new OfferDiffService(distribution);

        // When
        instance.runDiffAsync(offerDiffProcess);

        countDownLatch.await(1, TimeUnit.MINUTES);

        // Then
        verify(offerDiffProcess).run();
    }
}
