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
package fr.gouv.vitam.worker.core.plugin.common;

import com.google.common.collect.Iterators;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.processing.common.async.AccessRequestContext;
import fr.gouv.vitam.processing.common.async.ProcessingRetryAsyncException;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.request.BulkObjectAvailabilityRequest;
import fr.gouv.vitam.storage.engine.common.model.response.BulkObjectAvailabilityResponse;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Abstract service of resource availability plugins. These plugins should be added at the beginning of any workflow step who contains an action that download or copy from a resource that could be stored on an async offer.
 */
public abstract class CheckResourceAvailability extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CheckResourceAvailability.class);

    private final StorageClientFactory storageClientFactory;

    protected CheckResourceAvailability(StorageClientFactory storage) {
        this.storageClientFactory = storage;
    }

    /**
     * Check for multiple types resource synchronous availability in storage and create access requests for unavailable objects
     *
     * @param objectIdsByContextbyType object ids by context (strategy/offer) by type
     * @throws StorageServerClientException exception from storage
     * @throws ProcessingRetryAsyncException exception thrown when some resources are unavailable
     */
    protected void checkResourcesAvailabilityByTypes(
        Map<DataCategory, Map<AccessRequestContext, List<String>>> objectIdsByContextbyType
    ) throws StorageServerClientException, ProcessingRetryAsyncException {
        LOGGER.info("Check if resources are available for multiple categories.");
        Map<AccessRequestContext, List<String>> accessRequestsCreated = new HashMap<>();
        for (DataCategory type : objectIdsByContextbyType.keySet()) {
            MultiValuedMap<AccessRequestContext, String> unavailableResources = extractUnavailableResources(
                objectIdsByContextbyType.get(type),
                type
            );
            if (!unavailableResources.isEmpty()) {
                LOGGER.info("Some resources are unavailable, creation of accessRequests.");
                Map<AccessRequestContext, List<String>> createAccessRequestsForType = createAccessRequests(
                    unavailableResources,
                    type
                );
                for (AccessRequestContext context : createAccessRequestsForType.keySet()) {
                    accessRequestsCreated
                        .computeIfAbsent(context, (x -> new ArrayList<>()))
                        .addAll(createAccessRequestsForType.get(context));
                }
            }
        }
        if (!accessRequestsCreated.isEmpty()) {
            throw new ProcessingRetryAsyncException(accessRequestsCreated);
        }
    }

    /**
     * Check resource synchronous availability in storage and create access requests for unavailable objects
     *
     * @param objectIdsByContext object ids by context (strategy/offer)
     * @param type data category type
     * @throws StorageServerClientException exception from storage
     * @throws ProcessingRetryAsyncException exception thrown when some resources are unavailable
     */
    protected void checkResourcesAvailability(
        Map<AccessRequestContext, List<String>> objectIdsByContext,
        DataCategory type
    ) throws StorageServerClientException, ProcessingRetryAsyncException {
        LOGGER.info("Check if resources are available.");
        MultiValuedMap<AccessRequestContext, String> unavailableResources = extractUnavailableResources(
            objectIdsByContext,
            type
        );
        if (!unavailableResources.isEmpty()) {
            LOGGER.info("Some resources are unavailable, creation of accessRequests.");
            Map<AccessRequestContext, List<String>> accessRequestsCreated = createAccessRequests(
                unavailableResources,
                type
            );
            throw new ProcessingRetryAsyncException(accessRequestsCreated);
        }
    }

    /**
     * Check resource synchronous availability in storage and return the unavailable ones
     *
     * @param objectIdsByContext object ids by context (strategy/offer)
     * @param type data category type
     * @return unavailable objects ids by context
     * @throws StorageServerClientException exception from storage
     */
    private MultiValuedMap<AccessRequestContext, String> extractUnavailableResources(
        Map<AccessRequestContext, List<String>> objectIdsByContext,
        DataCategory type
    ) throws StorageServerClientException {
        LOGGER.debug("Extract the resources that are unavailable");
        try (StorageClient storageClient = storageClientFactory.getClient()) {
            MultiValuedMap<AccessRequestContext, String> unavailableObjects = new ArrayListValuedHashMap<>();
            for (AccessRequestContext context : objectIdsByContext.keySet()) {
                Collection<String> objectIds = objectIdsByContext.get(context);
                Iterator<List<String>> objectIdsIterator = Iterators.partition(
                    objectIds.iterator(),
                    VitamConfiguration.getBatchSize()
                );
                while (objectIdsIterator.hasNext()) {
                    List<String> objectIdsBulk = objectIdsIterator.next();
                    BulkObjectAvailabilityRequest request = new BulkObjectAvailabilityRequest(type, objectIdsBulk);
                    BulkObjectAvailabilityResponse response = storageClient.checkBulkObjectAvailability(
                        context.getStrategyId(),
                        context.getOfferId(),
                        request
                    );
                    if (!response.getAreObjectsAvailable()) {
                        for (String objectName : request.getObjectNames()) {
                            unavailableObjects.put(context, objectName);
                        }
                    }
                }
            }
            return unavailableObjects;
        }
    }

    /**
     * Create access requests for given objects
     *
     * @param objectIdsByContext object ids by context (strategy/offer)
     * @param type data category type
     * @return accessRequests ids by strategy/offer context
     * @throws StorageServerClientException exception from storage
     */
    private Map<AccessRequestContext, List<String>> createAccessRequests(
        MultiValuedMap<AccessRequestContext, String> objectIdsByContext,
        DataCategory type
    ) throws StorageServerClientException {
        LOGGER.debug("Create access requests if required");
        try (StorageClient storageClient = storageClientFactory.getClient()) {
            Map<AccessRequestContext, List<String>> accessRequests = new HashMap<>();
            for (AccessRequestContext context : objectIdsByContext.keySet()) {
                Collection<String> objectIds = objectIdsByContext.get(context);
                Iterator<List<String>> objectIdsIterator = Iterators.partition(
                    objectIds.iterator(),
                    VitamConfiguration.getBatchSize()
                );
                while (objectIdsIterator.hasNext()) {
                    List<String> objectIdsBulk = objectIdsIterator.next();
                    objectIdsBulk.forEach(item -> {
                        LOGGER.debug(
                            "Create access requests if required for " +
                            context.getStrategyId() +
                            " " +
                            context.getOfferId() +
                            " " +
                            type +
                            " " +
                            item
                        );
                    });
                    Optional<String> accessRequestId = storageClient.createAccessRequestIfRequired(
                        context.getStrategyId(),
                        context.getOfferId(),
                        type,
                        objectIdsBulk
                    );
                    accessRequestId.ifPresentOrElse(
                        id -> accessRequests.computeIfAbsent(context, (x -> new ArrayList<>())).add(id),
                        () -> LOGGER.warn("Access Request was not created")
                    );
                }
            }
            return accessRequests;
        }
    }
}
