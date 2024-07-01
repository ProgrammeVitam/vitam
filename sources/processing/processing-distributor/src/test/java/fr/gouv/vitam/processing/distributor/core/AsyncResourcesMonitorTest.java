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
package fr.gouv.vitam.processing.distributor.core;

import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.storage.AccessRequestStatus;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadFactory;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.processing.common.async.AccessRequestContext;
import fr.gouv.vitam.processing.common.async.AsyncResourceCallback;
import fr.gouv.vitam.processing.common.async.WorkflowInterruptionChecker;
import fr.gouv.vitam.processing.common.config.ServerConfiguration;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import org.apache.commons.collections4.ListUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static fr.gouv.vitam.common.mockito.UnorderedListMatcher.eqUnorderedList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class AsyncResourcesMonitorTest {

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private StorageClientFactory storageClientFactory;

    @Mock
    private StorageClient storageClient;

    @Mock
    private ScheduledExecutorService scheduledExecutor;

    private Runnable registeredTask;

    private AsyncResourcesMonitor asyncResourcesMonitor;

    @Before
    public void setup() throws Exception {
        when(storageClientFactory.getClient()).thenReturn(storageClient);

        doAnswer(args -> {
            registeredTask = args.getArgument(0);
            return null;
        })
            .when(scheduledExecutor)
            .scheduleWithFixedDelay(any(), anyLong(), anyLong(), any());

        VitamConfiguration.setAdminTenant(1);
        ServerConfiguration serverConfiguration = new ServerConfiguration();
        serverConfiguration.setDelayAsyncResourceMonitor(5);
        asyncResourcesMonitor = new AsyncResourcesMonitor(serverConfiguration, storageClientFactory, scheduledExecutor);
    }

    @Test
    public void givenAsyncResourcesWhenAllStatusesReadyThenAsyncCallbackCalled() throws Exception {
        // Given
        when(
            storageClient.checkAccessRequestStatuses(
                eq("strategyId1"),
                eq("offerId1"),
                eqUnorderedList("accessRequestId1", "accessRequestId2", "accessRequestId3"),
                eq(true)
            )
        ).thenReturn(
            Map.of(
                "accessRequestId1",
                AccessRequestStatus.READY,
                "accessRequestId2",
                AccessRequestStatus.READY,
                "accessRequestId3",
                AccessRequestStatus.READY
            )
        );

        // When
        AsyncResourceCallback bulkCallback = mock(AsyncResourceCallback.class);
        WorkflowInterruptionChecker alwaysAlive = () -> true;

        watchAsyncResourcesBulk(
            Map.of(
                "accessRequestId1",
                new AccessRequestContext("strategyId1", "offerId1"),
                "accessRequestId2",
                new AccessRequestContext("strategyId1", "offerId1"),
                "accessRequestId3",
                new AccessRequestContext("strategyId1", "offerId1")
            ),
            alwaysAlive,
            bulkCallback
        );

        simulateBackgroundScheduledTasksRun3xTimes();

        // Then
        verify(bulkCallback).notifyWorkflow();
        verify(storageClient, times(1)).checkAccessRequestStatuses(any(), any(), any(), anyBoolean());
        verify(storageClient, atLeastOnce()).close();
        verifyNoMoreInteractions(storageClient);
    }

    @Test
    public void givenAsyncResourcesWhenOneStatusNotReadyThenCallbackNotCalled() throws Exception {
        // Given
        when(
            storageClient.checkAccessRequestStatuses(
                eq("strategyId1"),
                eq("offerId1"),
                eqUnorderedList("accessRequestId1", "accessRequestId2", "accessRequestId3"),
                eq(true)
            )
        ).thenReturn(
            Map.of(
                "accessRequestId1",
                AccessRequestStatus.READY,
                "accessRequestId2",
                AccessRequestStatus.NOT_READY,
                "accessRequestId3",
                AccessRequestStatus.READY
            )
        );

        // When
        AsyncResourceCallback bulkCallback = mock(AsyncResourceCallback.class);
        WorkflowInterruptionChecker alwaysAlive = () -> true;

        watchAsyncResourcesBulk(
            Map.of(
                "accessRequestId1",
                new AccessRequestContext("strategyId1", "offerId1"),
                "accessRequestId2",
                new AccessRequestContext("strategyId1", "offerId1"),
                "accessRequestId3",
                new AccessRequestContext("strategyId1", "offerId1")
            ),
            alwaysAlive,
            bulkCallback
        );

        simulateBackgroundScheduledTasksRun3xTimes();

        // Then
        verifyNoMoreInteractions(bulkCallback);
        verify(storageClient, times(3)).checkAccessRequestStatuses(any(), any(), any(), anyBoolean());
        verify(storageClient, atLeastOnce()).close();
        verifyNoMoreInteractions(storageClient);
    }

    @Test
    public void givenAsyncResourcesWhenOneStatusNotFoundThenCallbackCalled() throws Exception {
        // Given
        when(
            storageClient.checkAccessRequestStatuses(
                eq("strategyId1"),
                eq("offerId1"),
                eqUnorderedList("accessRequestId1", "accessRequestId2", "accessRequestId3"),
                eq(true)
            )
        ).thenReturn(
            Map.of(
                "accessRequestId1",
                AccessRequestStatus.READY,
                "accessRequestId2",
                AccessRequestStatus.READY,
                "accessRequestId3",
                AccessRequestStatus.NOT_FOUND
            )
        );

        // When
        AsyncResourceCallback bulkCallback = mock(AsyncResourceCallback.class);
        WorkflowInterruptionChecker alwaysAlive = () -> true;

        watchAsyncResourcesBulk(
            Map.of(
                "accessRequestId1",
                new AccessRequestContext("strategyId1", "offerId1"),
                "accessRequestId2",
                new AccessRequestContext("strategyId1", "offerId1"),
                "accessRequestId3",
                new AccessRequestContext("strategyId1", "offerId1")
            ),
            alwaysAlive,
            bulkCallback
        );

        simulateBackgroundScheduledTasksRun3xTimes();

        // Then
        verify(bulkCallback).notifyWorkflow();
        verify(storageClient, times(1)).checkAccessRequestStatuses(any(), any(), any(), anyBoolean());
        verify(storageClient, atLeastOnce()).close();
        verifyNoMoreInteractions(storageClient);
    }

    @Test
    public void givenAsyncResourcesWhenAllStatusReadyForAllStrategiesInSameBulkThenAsyncCallback() throws Exception {
        // Given
        when(
            storageClient.checkAccessRequestStatuses(
                eq("strategyId1"),
                eq("offerId1"),
                eq(List.of("accessRequestId1")),
                eq(true)
            )
        ).thenReturn(Map.of("accessRequestId1", AccessRequestStatus.READY));
        when(
            storageClient.checkAccessRequestStatuses(
                eq("strategyId2"),
                eq("offerId1"),
                eq(List.of("accessRequestId2")),
                eq(true)
            )
        ).thenReturn(Map.of("accessRequestId2", AccessRequestStatus.READY));
        when(
            storageClient.checkAccessRequestStatuses(
                eq("strategyId3"),
                eq("offerId1"),
                eq(List.of("accessRequestId3")),
                eq(true)
            )
        ).thenReturn(Map.of("accessRequestId3", AccessRequestStatus.READY));
        when(
            storageClient.checkAccessRequestStatuses(
                eq("strategyId1"),
                eq(null),
                eq(List.of("accessRequestId4")),
                eq(true)
            )
        ).thenReturn(Map.of("accessRequestId4", AccessRequestStatus.READY));

        // When
        AsyncResourceCallback bulkCallback = mock(AsyncResourceCallback.class);
        WorkflowInterruptionChecker alwaysAlive = () -> true;

        watchAsyncResourcesBulk(
            Map.of(
                "accessRequestId1",
                new AccessRequestContext("strategyId1", "offerId1"),
                "accessRequestId2",
                new AccessRequestContext("strategyId2", "offerId1"),
                "accessRequestId3",
                new AccessRequestContext("strategyId3", "offerId1"),
                "accessRequestId4",
                new AccessRequestContext("strategyId1")
            ),
            alwaysAlive,
            bulkCallback
        );

        simulateBackgroundScheduledTasksRun3xTimes();

        // Then
        verify(bulkCallback).notifyWorkflow();

        // Expected one invocation per strategy
        verify(storageClient, times(1)).checkAccessRequestStatuses(
            eq("strategyId1"),
            eq("offerId1"),
            eqUnorderedList("accessRequestId1"),
            eq(true)
        );
        verify(storageClient, times(1)).checkAccessRequestStatuses(
            eq("strategyId2"),
            eq("offerId1"),
            eqUnorderedList("accessRequestId2"),
            eq(true)
        );
        verify(storageClient, times(1)).checkAccessRequestStatuses(
            eq("strategyId3"),
            eq("offerId1"),
            eqUnorderedList("accessRequestId3"),
            eq(true)
        );
        verify(storageClient, times(1)).checkAccessRequestStatuses(
            eq("strategyId1"),
            eq(null),
            eqUnorderedList("accessRequestId4"),
            eq(true)
        );
        verify(storageClient, atLeastOnce()).close();
        verifyNoMoreInteractions(storageClient);
    }

    @Test
    public void givenAsyncResourcesWhenAStatusNotReadyForOneStrategiesInSameBulkThenNoAsyncCallback() throws Exception {
        // Given
        when(
            storageClient.checkAccessRequestStatuses(
                eq("strategyId1"),
                eq("offerId1"),
                eq(List.of("accessRequestId1")),
                eq(true)
            )
        ).thenReturn(Map.of("accessRequestId1", AccessRequestStatus.READY));
        when(
            storageClient.checkAccessRequestStatuses(
                eq("strategyId2"),
                eq("offerId1"),
                eq(List.of("accessRequestId2")),
                eq(true)
            )
        ).thenReturn(Map.of("accessRequestId2", AccessRequestStatus.NOT_READY));
        when(
            storageClient.checkAccessRequestStatuses(
                eq("strategyId3"),
                eq("offerId1"),
                eq(List.of("accessRequestId3")),
                eq(true)
            )
        ).thenReturn(Map.of("accessRequestId3", AccessRequestStatus.NOT_READY));

        // When
        AsyncResourceCallback bulkCallback = mock(AsyncResourceCallback.class);
        WorkflowInterruptionChecker alwaysAlive = () -> true;

        watchAsyncResourcesBulk(
            Map.of(
                "accessRequestId1",
                new AccessRequestContext("strategyId1", "offerId1"),
                "accessRequestId2",
                new AccessRequestContext("strategyId2", "offerId1"),
                "accessRequestId3",
                new AccessRequestContext("strategyId3", "offerId1")
            ),
            alwaysAlive,
            bulkCallback
        );

        simulateBackgroundScheduledTasksRun3xTimes();

        // Then
        verifyNoMoreInteractions(bulkCallback);

        // Expected 3 invocations per strategy
        verify(storageClient, times(3)).checkAccessRequestStatuses(
            eq("strategyId1"),
            eq("offerId1"),
            eqUnorderedList("accessRequestId1"),
            eq(true)
        );
        verify(storageClient, times(3)).checkAccessRequestStatuses(
            eq("strategyId2"),
            eq("offerId1"),
            eqUnorderedList("accessRequestId2"),
            eq(true)
        );
        verify(storageClient, times(3)).checkAccessRequestStatuses(
            eq("strategyId3"),
            eq("offerId1"),
            eqUnorderedList("accessRequestId3"),
            eq(true)
        );
        verify(storageClient, atLeastOnce()).close();
        verifyNoMoreInteractions(storageClient);
    }

    @Test
    public void givenAsyncResourcesWhenStatusReadyForSomeBulksThenOnlyTheseBulksAsyncCallback() throws Exception {
        // Given
        when(
            storageClient.checkAccessRequestStatuses(
                eq("strategyId1"),
                eq("offerId1"),
                eqUnorderedList("accessRequestId1", "accessRequestId2", "accessRequestId3"),
                eq(true)
            )
        ).thenReturn(
            Map.of(
                "accessRequestId1",
                AccessRequestStatus.READY,
                "accessRequestId2",
                AccessRequestStatus.READY,
                "accessRequestId3",
                AccessRequestStatus.NOT_READY
            )
        );
        when(
            storageClient.checkAccessRequestStatuses(
                eq("strategyId1"),
                eq("offerId1"),
                eq(List.of("accessRequestId3")),
                eq(true)
            )
        ).thenReturn(Map.of("accessRequestId3", AccessRequestStatus.NOT_READY));

        // When
        AsyncResourceCallback bulkCallback1 = mock(AsyncResourceCallback.class);
        AsyncResourceCallback bulkCallback2 = mock(AsyncResourceCallback.class);
        AsyncResourceCallback bulkCallback3 = mock(AsyncResourceCallback.class);
        WorkflowInterruptionChecker alwaysAlive = () -> true;

        watchAsyncResourcesBulk(
            Map.of("accessRequestId1", new AccessRequestContext("strategyId1", "offerId1")),
            "requestId1",
            alwaysAlive,
            bulkCallback1
        );
        watchAsyncResourcesBulk(
            Map.of("accessRequestId2", new AccessRequestContext("strategyId1", "offerId1")),
            "requestId1",
            alwaysAlive,
            bulkCallback2
        );
        watchAsyncResourcesBulk(
            Map.of("accessRequestId3", new AccessRequestContext("strategyId1", "offerId1")),
            "requestId2",
            alwaysAlive,
            bulkCallback3
        );

        simulateBackgroundScheduledTasksRun3xTimes();

        // Then
        verify(bulkCallback1).notifyWorkflow();
        verify(bulkCallback2).notifyWorkflow();
        verifyNoMoreInteractions(bulkCallback3);

        verify(storageClient, times(1)).checkAccessRequestStatuses(
            eq("strategyId1"),
            eq("offerId1"),
            eqUnorderedList("accessRequestId1", "accessRequestId2", "accessRequestId3"),
            eq(true)
        );
        verify(storageClient, times(2)).checkAccessRequestStatuses(
            eq("strategyId1"),
            eq("offerId1"),
            eq(List.of("accessRequestId3")),
            eq(true)
        );
        verify(storageClient, atLeastOnce()).close();
        verifyNoMoreInteractions(storageClient);
    }

    @Test
    public void givenAsyncResourcesWhenOneStatusMissingThenCallbackNotCalled() throws Exception {
        // Given
        when(
            storageClient.checkAccessRequestStatuses(
                eq("strategyId1"),
                eq("offerId1"),
                eqUnorderedList("accessRequestId1", "accessRequestId2", "accessRequestId3"),
                eq(true)
            )
        ).thenReturn(
            Map.of("accessRequestId1", AccessRequestStatus.READY, "accessRequestId2", AccessRequestStatus.READY)
        );

        // When
        AsyncResourceCallback bulkCallback = mock(AsyncResourceCallback.class);
        WorkflowInterruptionChecker alwaysAlive = () -> true;

        watchAsyncResourcesBulk(
            Map.of(
                "accessRequestId1",
                new AccessRequestContext("strategyId1", "offerId1"),
                "accessRequestId2",
                new AccessRequestContext("strategyId1", "offerId1"),
                "accessRequestId3",
                new AccessRequestContext("strategyId1", "offerId1")
            ),
            alwaysAlive,
            bulkCallback
        );

        simulateBackgroundScheduledTasksRun3xTimes();

        // Then
        verifyNoMoreInteractions(bulkCallback);
        verify(storageClient, times(3)).checkAccessRequestStatuses(any(), any(), any(), anyBoolean());
        verify(storageClient, atLeastOnce()).close();
        verifyNoMoreInteractions(storageClient);
    }

    @Test
    public void givenAsyncResourcesWhenOneStatusNullThenCallbackNotCalled() throws Exception {
        // Given
        Map<String, AccessRequestStatus> resultMap = new HashMap<>();
        resultMap.put("accessRequestId1", null);
        when(
            storageClient.checkAccessRequestStatuses(
                eq("strategyId1"),
                eq("offerId1"),
                eq(List.of("accessRequestId1")),
                eq(true)
            )
        ).thenReturn(resultMap);

        // When
        AsyncResourceCallback bulkCallback = mock(AsyncResourceCallback.class);
        WorkflowInterruptionChecker alwaysAlive = () -> true;

        watchAsyncResourcesBulk(
            Map.of("accessRequestId1", new AccessRequestContext("strategyId1", "offerId1")),
            alwaysAlive,
            bulkCallback
        );

        simulateBackgroundScheduledTasksRun3xTimes();

        // Then
        verifyNoMoreInteractions(bulkCallback);
        verify(storageClient, times(3)).checkAccessRequestStatuses(any(), any(), any(), anyBoolean());
        verify(storageClient, atLeastOnce()).close();
        verifyNoMoreInteractions(storageClient);
    }

    @Test
    public void givenAsyncResourcesStorageExceptionThenCallbackNotCalled() throws Exception {
        // Given
        when(
            storageClient.checkAccessRequestStatuses(
                eq("strategyId1"),
                eq("offerId1"),
                eq(List.of("accessRequestId1")),
                eq(true)
            )
        ).thenThrow(new StorageServerClientException("Something bad happened"));

        // When
        AsyncResourceCallback bulkCallback = mock(AsyncResourceCallback.class);
        WorkflowInterruptionChecker alwaysAlive = () -> true;

        watchAsyncResourcesBulk(
            Map.of("accessRequestId1", new AccessRequestContext("strategyId1", "offerId1")),
            alwaysAlive,
            bulkCallback
        );

        simulateBackgroundScheduledTasksRun3xTimes();

        // Then
        verifyNoMoreInteractions(bulkCallback);
        verify(storageClient, times(3)).checkAccessRequestStatuses(any(), any(), any(), anyBoolean());
        verify(storageClient, atLeastOnce()).close();
        verifyNoMoreInteractions(storageClient);
    }

    @Test
    public void givenAsyncResourcesWhenSameAccessRequestDifferentBulkThenCallbackCalled() throws Exception {
        // Given
        when(
            storageClient.checkAccessRequestStatuses(
                eq("strategyId1"),
                eq("offerId1"),
                eqUnorderedList("accessRequestId1", "accessRequestId2", "accessRequestId1"),
                eq(true)
            )
        ).thenReturn(
            Map.of("accessRequestId1", AccessRequestStatus.READY, "accessRequestId2", AccessRequestStatus.READY)
        );

        // When
        AsyncResourceCallback bulkCallback1 = mock(AsyncResourceCallback.class);
        AsyncResourceCallback bulkCallback2 = mock(AsyncResourceCallback.class);
        AsyncResourceCallback bulkCallback3 = mock(AsyncResourceCallback.class);
        WorkflowInterruptionChecker alwaysAlive = () -> true;

        watchAsyncResourcesBulk(
            Map.of("accessRequestId1", new AccessRequestContext("strategyId1", "offerId1")),
            "requestId1",
            alwaysAlive,
            bulkCallback1
        );
        watchAsyncResourcesBulk(
            Map.of("accessRequestId2", new AccessRequestContext("strategyId1", "offerId1")),
            "requestId1",
            alwaysAlive,
            bulkCallback2
        );
        watchAsyncResourcesBulk(
            Map.of("accessRequestId1", new AccessRequestContext("strategyId1", "offerId1")),
            "requestId2",
            alwaysAlive,
            bulkCallback3
        );

        simulateBackgroundScheduledTasksRun3xTimes();

        // Then
        verify(bulkCallback1).notifyWorkflow();
        verify(bulkCallback2).notifyWorkflow();
        verify(bulkCallback3).notifyWorkflow();

        verify(storageClient, times(1)).checkAccessRequestStatuses(
            eq("strategyId1"),
            eq("offerId1"),
            eqUnorderedList("accessRequestId1", "accessRequestId2", "accessRequestId1"),
            eq(true)
        );
        verify(storageClient, atLeastOnce()).close();
        verifyNoMoreInteractions(storageClient);
    }

    @Test
    public void givenAsyncResourcesWhenWorkflowPausedOrCanceledThenCallbackCalled() throws Exception {
        // Given
        when(
            storageClient.checkAccessRequestStatuses(
                eq("strategyId1"),
                eq(null),
                eqUnorderedList("accessRequestId1"),
                eq(true)
            )
        ).thenReturn(Map.of("accessRequestId1", AccessRequestStatus.NOT_READY));

        when(
            storageClient.checkAccessRequestStatuses(
                eq("strategyId1"),
                eq("offerId1"),
                eqUnorderedList("accessRequestId3"),
                eq(true)
            )
        ).thenReturn(Map.of("accessRequestId3", AccessRequestStatus.NOT_READY));
        WorkflowInterruptionChecker workflow1Interrupted = () -> false;
        WorkflowInterruptionChecker workflow2Alive = () -> true;

        // When
        AsyncResourceCallback bulkCallback1 = mock(AsyncResourceCallback.class);
        AsyncResourceCallback bulkCallback2 = mock(AsyncResourceCallback.class);

        watchAsyncResourcesBulk(
            Map.of(
                "accessRequestId1",
                new AccessRequestContext("strategyId1", "offerId1"),
                "accessRequestId2",
                new AccessRequestContext("strategyId1")
            ),
            "requestId1",
            workflow1Interrupted,
            bulkCallback1
        );
        watchAsyncResourcesBulk(
            Map.of("accessRequestId3", new AccessRequestContext("strategyId1", "offerId1")),
            "requestId2",
            workflow2Alive,
            bulkCallback2
        );

        simulateBackgroundScheduledTasksRun3xTimes();

        // Then
        verify(bulkCallback1).notifyWorkflow();
        verifyZeroInteractions(bulkCallback2);

        // Ensure access request of interrupted workflow never checked
        verify(storageClient, times(3)).checkAccessRequestStatuses(
            eq("strategyId1"),
            eq("offerId1"),
            eqUnorderedList("accessRequestId3"),
            eq(true)
        );
        verify(storageClient, atLeastOnce()).close();
        verifyNoMoreInteractions(storageClient);
    }

    @Test
    public void givenLargeAsyncResourceBulksWhenAllStatusesReadyThenAsyncCallbackCalled() throws Exception {
        // Given
        Set<String> accessRequestIds = IntStream.rangeClosed(1, VitamConfiguration.getBatchSize() + 1)
            .mapToObj(i -> "accessRequestId" + i)
            .collect(Collectors.toSet());

        when(
            storageClient.checkAccessRequestStatuses(eq("strategyId1"), eq("offerId1"), anyList(), eq(true))
        ).thenAnswer(args -> {
            List<String> argAccessRequestIds = args.getArgument(2);
            return argAccessRequestIds
                .stream()
                .collect(
                    Collectors.toMap(accessRequestId -> accessRequestId, accessRequestId -> AccessRequestStatus.READY)
                );
        });

        // When
        AsyncResourceCallback bulkCallback = mock(AsyncResourceCallback.class);
        WorkflowInterruptionChecker alwaysAlive = () -> true;

        watchAsyncResourcesBulk(
            accessRequestIds
                .stream()
                .collect(
                    Collectors.toMap(
                        accessRequestId -> accessRequestId,
                        accessRequestId -> new AccessRequestContext("strategyId1", "offerId1")
                    )
                ),
            alwaysAlive,
            bulkCallback
        );

        simulateBackgroundScheduledTasksRun3xTimes();

        // Then
        verify(bulkCallback).notifyWorkflow();
        ArgumentCaptor<List<String>> checkedAccessRequestIdArgumentCaptor = ArgumentCaptor.forClass(List.class);
        verify(storageClient, times(2)).checkAccessRequestStatuses(
            any(),
            any(),
            checkedAccessRequestIdArgumentCaptor.capture(),
            eq(true)
        );

        assertThat(checkedAccessRequestIdArgumentCaptor.getAllValues().get(0)).hasSize(
            VitamConfiguration.getBatchSize()
        );
        assertThat(checkedAccessRequestIdArgumentCaptor.getAllValues().get(1)).hasSize(1);
        assertThat(
            ListUtils.union(
                checkedAccessRequestIdArgumentCaptor.getAllValues().get(0),
                checkedAccessRequestIdArgumentCaptor.getAllValues().get(1)
            )
        ).containsExactlyInAnyOrderElementsOf(accessRequestIds);

        verify(storageClient, atLeastOnce()).close();
        verifyNoMoreInteractions(storageClient);
    }

    private void watchAsyncResourcesBulk(
        Map<String, AccessRequestContext> asyncResources,
        WorkflowInterruptionChecker livenessChecker,
        AsyncResourceCallback callback
    ) {
        watchAsyncResourcesBulk(asyncResources, "requestId", livenessChecker, callback);
    }

    private void watchAsyncResourcesBulk(
        Map<String, AccessRequestContext> asyncResources,
        String requestId,
        WorkflowInterruptionChecker workflowInterruptionChecker,
        AsyncResourceCallback callback
    ) {
        asyncResourcesMonitor.watchAsyncResourcesForBulk(
            asyncResources,
            requestId,
            GUIDFactory.newGUID().getId(),
            workflowInterruptionChecker,
            callback
        );
    }

    private void simulateBackgroundScheduledTasksRun3xTimes() throws InterruptedException {
        for (int i = 0; i < 3; i++) {
            Thread thread = VitamThreadFactory.getInstance().newThread(() -> this.registeredTask.run());
            thread.start();
            thread.join();
        }
    }
}
