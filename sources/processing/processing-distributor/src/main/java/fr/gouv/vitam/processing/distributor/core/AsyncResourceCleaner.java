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
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.thread.VitamThreadFactory;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.processing.common.async.AccessRequestContext;
import fr.gouv.vitam.processing.common.config.ServerConfiguration;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageIllegalOperationClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Helper service that handles background cleanup of unused asynchronous resources from storage engine.
 * This service is used for all workflows that require async resource from tape offers.
 */
public class AsyncResourceCleaner {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AsyncResourceCleaner.class);

    private final StorageClientFactory storageClientFactory;

    // All access to this map in current code should be synchronized
    private final Map<String, AccessRequestContext> asyncResourcesToRemove;

    public AsyncResourceCleaner(ServerConfiguration serverConfiguration) {
        this(
            serverConfiguration,
            StorageClientFactory.getInstance(),
            Executors.newScheduledThreadPool(1, VitamThreadFactory.getInstance())
        );
    }

    @VisibleForTesting
    public AsyncResourceCleaner(
        ServerConfiguration serverConfiguration,
        StorageClientFactory storageClientFactory,
        ScheduledExecutorService scheduledExecutorService
    ) {
        this.storageClientFactory = storageClientFactory;
        this.asyncResourcesToRemove = new HashMap<>();

        scheduledExecutorService.scheduleWithFixedDelay(
            this::cleanupAsyncResources,
            serverConfiguration.getDelayAsyncResourceCleaner(),
            serverConfiguration.getDelayAsyncResourceCleaner(),
            TimeUnit.SECONDS
        );
    }

    /**
     * Clears asynchronous resources in background.
     */
    private void cleanupAsyncResources() {
        LOGGER.debug("Starting cleanupAsyncResources");
        String originalThreadName = Thread.currentThread().getName();
        try {
            Thread.currentThread().setName("CleanupAsyncResources-" + originalThreadName);

            VitamThreadUtils.getVitamSession().setTenantId(VitamConfiguration.getAdminTenant());
            VitamThreadUtils.getVitamSession()
                .setRequestId(GUIDFactory.newRequestIdGUID(VitamConfiguration.getAdminTenant()).getId());

            Map<String, AccessRequestContext> currentAsyncResourcesToRemove;
            synchronized (this.asyncResourcesToRemove) {
                currentAsyncResourcesToRemove = new HashMap<>(this.asyncResourcesToRemove);
            }

            if (currentAsyncResourcesToRemove.isEmpty()) {
                LOGGER.debug("Nothing to process");
                return;
            }

            Set<String> removedAccessRequestIds = new HashSet<>();
            try (StorageClient storageClient = this.storageClientFactory.getClient()) {
                for (String accessRequestId : currentAsyncResourcesToRemove.keySet()) {
                    AccessRequestContext accessRequestContext = currentAsyncResourcesToRemove.get(accessRequestId);

                    LOGGER.info(
                        "Removing access request {} for strategyId: {} / offerId: {}",
                        accessRequestId,
                        accessRequestContext.getStrategyId(),
                        accessRequestContext.getOfferId()
                    );
                    try {
                        storageClient.removeAccessRequest(
                            accessRequestContext.getStrategyId(),
                            accessRequestContext.getOfferId(),
                            accessRequestId,
                            true
                        );

                        LOGGER.info(
                            "Access request {} removed successfully for strategyId: {} / offerId: {}",
                            accessRequestId,
                            accessRequestContext.getStrategyId(),
                            accessRequestContext.getOfferId()
                        );

                        removedAccessRequestIds.add(accessRequestId);
                    } catch (StorageServerClientException | StorageIllegalOperationClientException e) {
                        LOGGER.error(
                            "Could not remove access request {} for strategyId: {} / offerId: {}",
                            accessRequestId,
                            accessRequestContext.getStrategyId(),
                            accessRequestContext.getOfferId(),
                            e
                        );
                        // We will retry next time...
                    }
                }
            }

            synchronized (this.asyncResourcesToRemove) {
                for (String removedAccessRequestId : removedAccessRequestIds) {
                    this.asyncResourcesToRemove.remove(removedAccessRequestId);
                }
            }
        } finally {
            Thread.currentThread().setName(originalThreadName);
        }
    }

    /**
     * Add async resources to clear
     *
     * @param asyncResources the async resources by request id map
     */
    public void markAsyncResourcesForRemoval(Map<String, AccessRequestContext> asyncResources) {
        synchronized (this.asyncResourcesToRemove) {
            Optional<String> existingAccessRequestId = asyncResources
                .keySet()
                .stream()
                .filter(this.asyncResourcesToRemove::containsKey)
                .findFirst();

            if (existingAccessRequestId.isPresent()) {
                throw new IllegalArgumentException(
                    "Duplicate access request id: '" + existingAccessRequestId.get() + "'"
                );
            }

            this.asyncResourcesToRemove.putAll(asyncResources);
        }
    }
}
