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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterators;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.storage.AccessRequestStatus;
import fr.gouv.vitam.common.thread.VitamThreadFactory;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.processing.common.async.AccessRequestContext;
import fr.gouv.vitam.processing.common.async.AccessRequestResult;
import fr.gouv.vitam.processing.common.async.AccessRequestValue;
import fr.gouv.vitam.processing.common.async.AsyncResourceBulkId;
import fr.gouv.vitam.processing.common.async.AsyncResourceCallback;
import fr.gouv.vitam.processing.common.async.WorkflowInterruptionChecker;
import fr.gouv.vitam.processing.common.config.ServerConfiguration;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageIllegalOperationClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.model.storage.AccessRequestStatus.EXPIRED;
import static fr.gouv.vitam.common.model.storage.AccessRequestStatus.NOT_FOUND;
import static fr.gouv.vitam.common.model.storage.AccessRequestStatus.NOT_READY;
import static fr.gouv.vitam.common.model.storage.AccessRequestStatus.READY;

/**
 * Global processing service monitoring regularly the status of asynchronous resources from storage engine. This service is used for all workflows who need to read a resource on a tape offer.<br>
 */
public class AsyncResourcesMonitor {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AsyncResourcesMonitor.class);

    private final StorageClientFactory storageClientFactory;

    // All access to this map in current code should be synchronized
    private final MultiValuedMap<AccessRequestContext, AccessRequestValue> asyncResources;

    public AsyncResourcesMonitor(ServerConfiguration serverConfiguration) {
        this(
            serverConfiguration,
            StorageClientFactory.getInstance(),
            Executors.newScheduledThreadPool(1, VitamThreadFactory.getInstance())
        );
    }

    @VisibleForTesting
    public AsyncResourcesMonitor(
        ServerConfiguration serverConfiguration,
        StorageClientFactory storageClientFactory,
        ScheduledExecutorService scheduledExecutorService
    ) {
        this.storageClientFactory = storageClientFactory;
        this.asyncResources = new ArrayListValuedHashMap<>();

        scheduledExecutorService.scheduleWithFixedDelay(
            this::checkAsyncResourcesStatuses,
            serverConfiguration.getDelayAsyncResourceMonitor(),
            serverConfiguration.getDelayAsyncResourceMonitor(),
            TimeUnit.SECONDS
        );
    }

    /**
     * Check all the asynchronous resources by key (strategy id / offer id) in storage engine and handle all the results by bulk id.
     * When a workflow is interrupted (paused or canceled), its access requests are excluded.
     */
    private void checkAsyncResourcesStatuses() {
        LOGGER.debug("Starting checkAsyncResourcesStatuses");
        VitamThreadUtils.getVitamSession().setTenantId(VitamConfiguration.getAdminTenant());
        VitamThreadUtils.getVitamSession()
            .setRequestId(GUIDFactory.newRequestIdGUID(VitamConfiguration.getAdminTenant()).getId());
        String originalThreadName = Thread.currentThread().getName();
        try {
            Thread.currentThread().setName("AsyncResourcesMonitor-" + originalThreadName);

            MultiValuedMap<AccessRequestContext, AccessRequestValue> currentAsyncResources;
            synchronized (this.asyncResources) {
                if (this.asyncResources.isEmpty()) {
                    LOGGER.debug("Not async resources");
                    return;
                }

                // synchronized clone of the map for treatments
                currentAsyncResources = new ArrayListValuedHashMap<>(this.asyncResources);
            }

            // Pre-filter async resources of interrupted (paused or canceled) workflows
            Set<AsyncResourceBulkId> interruptedBulkIds = abortAsyncResourcesOfInterruptedWorkflows(
                currentAsyncResources
            );

            MultiValuedMap<AccessRequestContext, AccessRequestValue> asyncResourcesOfActiveWorkflows =
                new ArrayListValuedHashMap<>();
            currentAsyncResources
                .entries()
                .stream()
                .filter(entry -> !interruptedBulkIds.contains(entry.getValue().getBulkId()))
                .forEach(entry -> asyncResourcesOfActiveWorkflows.put(entry.getKey(), entry.getValue()));

            // Check async resource statuses
            MultiValuedMap<AccessRequestContext, AccessRequestValue> accessRequestsToRemove =
                handleAsyncResourcesToCheck(asyncResourcesOfActiveWorkflows);

            synchronized (this.asyncResources) {
                // Remove interrupted async resources from map
                currentAsyncResources
                    .entries()
                    .stream()
                    .filter(entry -> interruptedBulkIds.contains(entry.getValue().getBulkId()))
                    .forEach(entry -> asyncResources.removeMapping(entry.getKey(), entry.getValue()));

                // Remove finished async resources from map
                accessRequestsToRemove
                    .entries()
                    .forEach(entry -> this.asyncResources.removeMapping(entry.getKey(), entry.getValue()));
            }
        } catch (Exception e) {
            LOGGER.error("An error occurred during async resource monitoring", e);
        } finally {
            Thread.currentThread().setName(originalThreadName);
        }
    }

    /**
     * Checks workflow interruptions (pause or cancel requests) of monitoring access requests.
     * When a workflow in interrupted, its callback is invoked to notify caller, and its access requests are removed.
     *
     * @param currentAsyncResources async resources to check
     * @return the set of bulkIds to remove
     */
    private Set<AsyncResourceBulkId> abortAsyncResourcesOfInterruptedWorkflows(
        MultiValuedMap<AccessRequestContext, AccessRequestValue> currentAsyncResources
    ) {
        // Map workflowInterruptionCheckers by bulkId
        Map<AsyncResourceBulkId, WorkflowInterruptionChecker> workflowInterruptionCheckerByBulkId =
            currentAsyncResources
                .values()
                .stream()
                .collect(
                    Collectors.toMap(
                        AccessRequestValue::getBulkId,
                        AccessRequestValue::getWorkflowInterruptionChecker,
                        // Deduplicate workflowInterruptionCheckers (same per bulkId)
                        (workflowInterruptionChecker1, workflowInterruptionChecker2) -> workflowInterruptionChecker1
                    )
                );

        // Select bulkId of interrupted workflows
        Set<AsyncResourceBulkId> interruptedBulkIds = workflowInterruptionCheckerByBulkId
            .entrySet()
            .stream()
            .filter(entry -> {
                // accessRequestsByWorkerBulk map is guaranteed to have non-empty list of values
                if (entry.getValue().isAlive()) {
                    LOGGER.debug("Workflow {} is still alive", entry.getKey().getRequestId());
                    return false;
                }
                LOGGER.info(
                    "Workflow {} has been interrupted (paused or canceled). Access requests will be removed.",
                    entry.getKey().getRequestId()
                );
                return true;
            })
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());

        // Map callbacks of interrupted workflows by bulkId
        Map<AsyncResourceBulkId, AsyncResourceCallback> asyncCallbackOfInterruptedWorkflows = currentAsyncResources
            .values()
            .stream()
            .filter(entry -> interruptedBulkIds.contains(entry.getBulkId()))
            .collect(
                Collectors.toMap(
                    AccessRequestValue::getBulkId,
                    AccessRequestValue::getCallback,
                    // Deduplicate callbacks (same per bulkId)
                    (callback1, callback2) -> callback1
                )
            );

        // Notify callbacks
        for (Map.Entry<
            AsyncResourceBulkId,
            AsyncResourceCallback
        > entry : asyncCallbackOfInterruptedWorkflows.entrySet()) {
            tryNotifyWorkflowCallback(entry.getValue());
        }

        return interruptedBulkIds;
    }

    private MultiValuedMap<AccessRequestContext, AccessRequestValue> handleAsyncResourcesToCheck(
        MultiValuedMap<AccessRequestContext, AccessRequestValue> activeAsyncResources
    ) {
        // retrieve all
        List<AccessRequestResult> accessRequestResults = retrieveAccessRequestStatusesInStorage(activeAsyncResources);

        // group by bulk id and compute every bulk result
        Map<AsyncResourceBulkId, List<AccessRequestResult>> accessRequestsByWorkerBulk = accessRequestResults
            .stream()
            .collect(Collectors.groupingBy(value -> value.getValue().getBulkId()));
        MultiValuedMap<AccessRequestContext, AccessRequestValue> accessRequestsToRemove =
            new ArrayListValuedHashMap<>();
        for (AsyncResourceBulkId bulkId : accessRequestsByWorkerBulk.keySet()) {
            accessRequestsToRemove.putAll(handleBulkResults(accessRequestsByWorkerBulk.get(bulkId), bulkId));
        }
        return accessRequestsToRemove;
    }

    /**
     * Handle access request results by bulk.
     * Results are computed :
     * - if at least one result is EXPIRED or NOT_FOUND then accessRequests linked to bulk are considered as processed
     * - else if at least on is NOT_READY then nothing is done and the accessRequest will be checked again later
     * - else if all are READY then accessRequests linked to bulk are considered as processed
     * The AccessRequest processing actions are :
     * - call of callback function linked to the accessRequest
     * - remove the async resource linked to the accessRequest from the current service
     * <p>
     * Error cases :
     * - one or more statuses are unknown values
     * - one or more statuses are "null" values
     *
     * @param accessRequestResultsForBulk list of access results for the bulk
     * @param bulkId the id of the bulk
     * @return the list of access request to remove from the current service since they are considered as "processed"
     * @throws IllegalStateException when one of defined error cases
     */
    private MultiValuedMap<AccessRequestContext, AccessRequestValue> handleBulkResults(
        List<AccessRequestResult> accessRequestResultsForBulk,
        AsyncResourceBulkId bulkId
    ) throws IllegalStateException {
        LOGGER.info("Handle results for bulk {} of request id {}", bulkId.getTaskId(), bulkId.getRequestId());
        MultiValuedMap<AccessRequestContext, AccessRequestValue> accessRequestsToRemove =
            new ArrayListValuedHashMap<>();
        Set<AccessRequestStatus> bulkResults = accessRequestResultsForBulk
            .stream()
            .map(AccessRequestResult::getStatus)
            .collect(Collectors.toSet());
        if (bulkResults.contains(null)) {
            throw new IllegalStateException(
                String.format(
                    "At least one result is null for bulk %s of request id %s",
                    bulkId.getRequestId(),
                    bulkId.getRequestId()
                )
            );
        }
        if (bulkResults.contains(EXPIRED) || bulkResults.contains(NOT_FOUND)) {
            // KO : remove and callback
            LOGGER.warn(
                "At least one access request EXPIRED or NOT_FOUND: {}",
                accessRequestResultsForBulk
                    .stream()
                    .collect(
                        Collectors.toMap(
                            accessRequestResult -> accessRequestResult.getValue().getAccessRequestId(),
                            AccessRequestResult::getStatus
                        )
                    )
            );
            accessRequestResultsForBulk.forEach(
                accessRequestResult ->
                    accessRequestsToRemove.put(accessRequestResult.getContext(), accessRequestResult.getValue())
            );

            tryNotifyWorkflowCallback(accessRequestResultsForBulk.stream().findFirst().orElseThrow().getCallback());
        } else if (bulkResults.contains(NOT_READY)) {
            // Nothing to do
            LOGGER.info("NOT_READY");
        } else if (bulkResults.contains(READY)) {
            // OK : remove and callback
            LOGGER.info("READY");
            accessRequestResultsForBulk.forEach(
                accessRequestResult ->
                    accessRequestsToRemove.put(accessRequestResult.getContext(), accessRequestResult.getValue())
            );

            tryNotifyWorkflowCallback(accessRequestResultsForBulk.stream().findFirst().orElseThrow().getCallback());
        } else if (bulkResults.size() > 0) {
            // default on  value : error
            throw new IllegalStateException(
                String.format(
                    "At least one result contains an invalid status value for bulk %s of request id %s",
                    bulkId.getTaskId(),
                    bulkId.getRequestId()
                )
            );
        }
        return accessRequestsToRemove;
    }

    private void tryNotifyWorkflowCallback(AsyncResourceCallback callback) {
        try {
            callback.notifyWorkflow();
        } catch (Exception e) {
            LOGGER.error("Async resource callback failed", e);
        }
    }

    /**
     * Retrieve accessRequest statuses in storage engine bulked by strategy Id / offer Id.
     *
     * @param currentAsyncResources list of async resources
     * @return list of results for async resources
     */
    private List<AccessRequestResult> retrieveAccessRequestStatusesInStorage(
        MultiValuedMap<AccessRequestContext, AccessRequestValue> currentAsyncResources
    ) {
        List<AccessRequestResult> accessRequestResults = new ArrayList<>();
        try (StorageClient storageClient = storageClientFactory.getClient()) {
            for (AccessRequestContext accessRequestGroupKey : currentAsyncResources.keySet()) {
                Collection<AccessRequestValue> accessRequestGroupValues = currentAsyncResources.get(
                    accessRequestGroupKey
                );
                Iterator<List<AccessRequestValue>> accessRequestGroupValuesIterator = Iterators.partition(
                    accessRequestGroupValues.iterator(),
                    VitamConfiguration.getBatchSize()
                );
                while (accessRequestGroupValuesIterator.hasNext()) {
                    List<AccessRequestValue> accessRequestValuesBulk = accessRequestGroupValuesIterator.next();
                    List<String> accessRequestIdBulk = accessRequestValuesBulk
                        .stream()
                        .map(AccessRequestValue::getAccessRequestId)
                        .collect(Collectors.toList());
                    Map<String, AccessRequestStatus> results = storageClient.checkAccessRequestStatuses(
                        accessRequestGroupKey.getStrategyId(),
                        accessRequestGroupKey.getOfferId(),
                        accessRequestIdBulk,
                        true
                    );
                    accessRequestResults.addAll(
                        accessRequestValuesBulk
                            .stream()
                            .map(
                                accessRequestValue ->
                                    new AccessRequestResult(
                                        accessRequestValue,
                                        accessRequestGroupKey,
                                        results.get(accessRequestValue.getAccessRequestId())
                                    )
                            )
                            .collect(Collectors.toList())
                    );
                }
            }
        } catch (StorageServerClientException | StorageIllegalOperationClientException e) {
            // We will retry later
            LOGGER.error("Exception while retrieving accessRequest statuses", e);
            accessRequestResults = Collections.emptyList();
        }
        return accessRequestResults;
    }

    /**
     * Add all the async resources of a bulk to monitor its statuses until it's either available or its accessRequest needs to be re-created.
     *
     * @param asyncResources map of accessRequestIds with it couple strategyId/OfferId for each
     * @param requestId request id of the workflow waiting for the resources availability
     * @param taskId distribution task id
     * @param workflowInterruptionChecker a non-blocking / stateless function that check workflow interruption status.
     * @param callback callback function called to notify the workflow execution waiting for the resource of its availability (or need to re-create)
     */
    public void watchAsyncResourcesForBulk(
        Map<String, AccessRequestContext> asyncResources,
        String requestId,
        String taskId,
        WorkflowInterruptionChecker workflowInterruptionChecker,
        AsyncResourceCallback callback
    ) {
        MultiValuedMap<AccessRequestContext, AccessRequestValue> asyncResourcesForBulk = new ArrayListValuedHashMap<>();
        for (Map.Entry<String, AccessRequestContext> asyncResource : asyncResources.entrySet()) {
            asyncResourcesForBulk.put(
                asyncResource.getValue(),
                new AccessRequestValue(asyncResource.getKey(), requestId, taskId, workflowInterruptionChecker, callback)
            );
        }
        synchronized (this.asyncResources) {
            this.asyncResources.putAll(asyncResourcesForBulk);
        }
    }
}
