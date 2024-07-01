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
import fr.gouv.vitam.common.thread.VitamThreadFactory;
import fr.gouv.vitam.processing.common.async.AccessRequestContext;
import fr.gouv.vitam.processing.common.config.ServerConfiguration;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class AsyncResourceCleanerTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private StorageClientFactory storageClientFactory;

    @Mock
    private StorageClient storageClient;

    @Mock
    private ScheduledExecutorService scheduledExecutor;

    private Runnable registeredTask;

    private AsyncResourceCleaner asyncResourceCleaner;

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
        serverConfiguration.setDelayAsyncResourceCleaner(5);
        asyncResourceCleaner = new AsyncResourceCleaner(serverConfiguration, storageClientFactory, scheduledExecutor);
    }

    @Test
    public void givenNoAccessRequestsWhenCleanupRunThenNoOp() throws Exception {
        // Given

        // When
        simulateBackgroundScheduledTasksRun3xTimes();

        // Then
        verifyZeroInteractions(storageClient);
    }

    @Test
    public void givenAccessRequestsWhenCleanupRunThenAccessRequestsRemoved() throws Exception {
        // Given
        doNothing().when(storageClient).removeAccessRequest(any(), any(), any(), anyBoolean());

        // When
        asyncResourceCleaner.markAsyncResourcesForRemoval(
            Map.of(
                "accessRequestId1",
                new AccessRequestContext("strategyId1", "offerId1"),
                "accessRequestId2",
                new AccessRequestContext("strategyId2", "offerId2")
            )
        );
        asyncResourceCleaner.markAsyncResourcesForRemoval(
            Map.of("accessRequestId3", new AccessRequestContext("strategyId1"))
        );
        simulateBackgroundScheduledTasksRun3xTimes();

        // Then
        verify(storageClient).removeAccessRequest("strategyId1", "offerId1", "accessRequestId1", true);
        verify(storageClient).removeAccessRequest("strategyId2", "offerId2", "accessRequestId2", true);
        verify(storageClient).removeAccessRequest("strategyId1", null, "accessRequestId3", true);
        verify(storageClient, atLeastOnce()).close();
        verifyZeroInteractions(storageClient);
    }

    @Test
    public void givenAccessRequestsWhenCleanupRunWithTemporaryErrorsThenAccessRequestsEventuallyRemoved()
        throws Exception {
        // Given
        doNothing().when(storageClient).removeAccessRequest(eq("strategyId1"), any(), any(), anyBoolean());
        // 1x failure than 1 success
        doThrow(new StorageServerClientException("error"))
            .doNothing()
            .when(storageClient)
            .removeAccessRequest("strategyId2", "offerId2", "accessRequestId3", true);

        // When
        asyncResourceCleaner.markAsyncResourcesForRemoval(
            Map.of(
                "accessRequestId1",
                new AccessRequestContext("strategyId1", "offerId1"),
                "accessRequestId2",
                new AccessRequestContext("strategyId1"),
                "accessRequestId3",
                new AccessRequestContext("strategyId2", "offerId2")
            )
        );
        simulateBackgroundScheduledTasksRun3xTimes();

        // Then
        verify(storageClient).removeAccessRequest("strategyId1", "offerId1", "accessRequestId1", true);
        verify(storageClient).removeAccessRequest("strategyId1", null, "accessRequestId2", true);
        verify(storageClient, times(2)).removeAccessRequest("strategyId2", "offerId2", "accessRequestId3", true);
        verify(storageClient, atLeastOnce()).close();
        verifyZeroInteractions(storageClient);
    }

    @Test
    public void givenExistingAccessRequestWhenAddingDuplicateAccessRequestThenKO() throws Exception {
        // Given
        doNothing().when(storageClient).removeAccessRequest(any(), any(), any(), anyBoolean());

        asyncResourceCleaner.markAsyncResourcesForRemoval(
            Map.of(
                "accessRequestId1",
                new AccessRequestContext("strategyId1", "offerId1"),
                "accessRequestId2",
                new AccessRequestContext("strategyId1"),
                "accessRequestId3",
                new AccessRequestContext("strategyId2", "offerId2")
            )
        );

        // When / Then

        assertThatThrownBy(
            () ->
                asyncResourceCleaner.markAsyncResourcesForRemoval(
                    Map.of("accessRequestId1", new AccessRequestContext("strategyId3"))
                )
        ).isInstanceOf(IllegalArgumentException.class);
    }

    private void simulateBackgroundScheduledTasksRun3xTimes() throws InterruptedException {
        for (int i = 0; i < 3; i++) {
            Thread thread = VitamThreadFactory.getInstance().newThread(() -> this.registeredTask.run());
            thread.start();
            thread.join();
        }
    }
}
