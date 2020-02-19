/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.Distribution;
import fr.gouv.vitam.common.model.processing.DistributionKind;
import fr.gouv.vitam.common.model.processing.DistributionType;
import fr.gouv.vitam.common.model.processing.PauseOrCancelAction;
import fr.gouv.vitam.common.model.processing.Step;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.config.ServerConfiguration;
import fr.gouv.vitam.processing.common.exception.HandlerNotFoundException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.exception.WorkerFamilyNotFoundException;
import fr.gouv.vitam.processing.common.model.DistributorIndex;
import fr.gouv.vitam.processing.common.model.PauseRecover;
import fr.gouv.vitam.processing.common.parameter.DefaultWorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParameterName;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.data.core.ProcessDataAccess;
import fr.gouv.vitam.processing.data.core.ProcessDataAccessImpl;
import fr.gouv.vitam.processing.data.core.management.ProcessDataManagement;
import fr.gouv.vitam.processing.data.core.management.WorkspaceProcessDataManagement;
import fr.gouv.vitam.processing.distributor.api.IWorkerManager;
import fr.gouv.vitam.processing.distributor.api.ProcessDistributor;
import fr.gouv.vitam.worker.client.WorkerClientFactory;
import fr.gouv.vitam.worker.client.exception.PauseCancelException;
import fr.gouv.vitam.worker.client.exception.WorkerUnreachableException;
import fr.gouv.vitam.worker.common.DescriptionStep;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.client.WorkspaceBufferingInputStream;
import org.apache.commons.collections4.iterators.PeekingIterator;

import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.google.common.base.MoreObjects.firstNonNull;

/**
 * The Process Distributor call the workers and intercept the response for manage a post actions step
 * <p>
 * <p>
 * <pre>
 * TODO P1:
 * - handle listing of items through a limited arraylist (memory) and through iterative (async) listing from
 * Workspace
 * - handle result in FATAL mode from one distributed item to stop the distribution in FATAL mode (do not
 * continue)
 * - try to handle distribution on 1 or on many as the same loop (so using a default arrayList of 1)
 * - handle error level using order in enum in ProcessResponse.getGlobalProcessStatusCode instead of manually comparing:
 *  <code>
 *    for (final EngineResponse response : responses) {
 *       tempStatusCode = response.getStatus();
 *       if (statusCode.ordinal() &gt; tempStatusCode.ordinal()) {
 *           statusCode = tempStatusCode;
 *       }
 *      if (statusCode.ordinal() &gt; StatusCode.KO.ordinal()) {
 *           break;
 *       }
 *     }
 *  </code>
 * </pre>
 */
public class ProcessDistributorImpl implements ProcessDistributor {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProcessDistributorImpl.class);

    private static final String AN_EXCEPTION_HAS_BEEN_THROWN_WHEN_TRYING_TO_GET_DISTIBUTOR_INDEX_FROM_WORKSPACE =
        "An exception has been thrown when trying to get distibutor index from workspace";
    private static final String DISTRIBUTOR_INDEX_NOT_FOUND_FOR_THE_OPERATION =
        "DistributorIndex not found for the operation";
    private static final String AN_EXCEPTION_HAS_BEEN_THROWN_WHEN_TRYING_TO_PERSIST_DISTRIBUTOR_INDEX =
        "An Exception has been thrown when trying to persist DistributorIndex";

    private final ProcessDataAccess processDataAccess;
    private final ProcessDataManagement processDataManagement;
    private final IWorkerManager workerManager;
    private final Map<String, Step> currentSteps = new HashMap<>();
    private final WorkspaceClientFactory workspaceClientFactory;
    private final WorkerClientFactory workerClientFactory;
    private final ServerConfiguration serverConfiguration;

    /**
     * Empty constructor
     *
     * @param workerManager
     * @param serverConfiguration
     */
    public ProcessDistributorImpl(IWorkerManager workerManager, ServerConfiguration serverConfiguration) {
        this(workerManager, serverConfiguration, ProcessDataAccessImpl.getInstance(),
            WorkspaceProcessDataManagement.getInstance(), WorkspaceClientFactory.getInstance(), null);
    }

    @VisibleForTesting
    public ProcessDistributorImpl(IWorkerManager workerManager, ServerConfiguration serverConfiguration,
        ProcessDataAccess processDataAccess, ProcessDataManagement processDataManagement,
        WorkspaceClientFactory workspaceClientFactory, WorkerClientFactory workerClientFactory) {
        this.workerManager = workerManager;
        this.serverConfiguration = serverConfiguration;
        this.processDataAccess = processDataAccess;
        this.processDataManagement = processDataManagement;
        this.workspaceClientFactory = workspaceClientFactory;
        this.workerClientFactory = workerClientFactory;
        ParametersChecker
            .checkParameter("Parameters are required.", workerManager, serverConfiguration, processDataAccess,
                processDataManagement, workspaceClientFactory);
    }

    @Override
    synchronized public boolean pause(String operationId) {
        ParametersChecker.checkParameter("The parameter operationId is required", operationId);
        final Step step = currentSteps.get(operationId);
        if (null != step) {
            step.setPauseOrCancelAction(PauseOrCancelAction.ACTION_PAUSE);
            return true;
        }
        return false;
    }

    @Override
    synchronized public boolean cancel(String operationId) {
        ParametersChecker.checkParameter("The parameter operationId is required", operationId);
        final Step step = currentSteps.get(operationId);
        if (null != step) {
            step.setPauseOrCancelAction(PauseOrCancelAction.ACTION_CANCEL);
            return true;
        }
        return false;
    }

    /**
     * Temporary method for distribution supporting multi-list
     *
     * @param workParams of type {@link WorkerParameters}
     * @param step the execution step
     * @param operationId the operation id
     * @param pauseRecover prevent recover from pause action
     * @return the final step status
     */
    @Override
    public ItemStatus distribute(WorkerParameters workParams, Step step, String operationId,
        PauseRecover pauseRecover) {
        ParametersChecker.checkParameter("WorkParams is a mandatory parameter", workParams);
        ParametersChecker.checkParameter("Step is a mandatory parameter", step);
        ParametersChecker.checkParameter("workflowId is a mandatory parameter", operationId);
        ParametersChecker.checkParameter("pauseRecover is a mandatory parameter", pauseRecover);
        /*
         * use index only if pauseRecover of the processWorkflow
         * is PauseRecover.RECOVER_FROM_API_PAUSE or PauseRecover.RECOVER_FROM_SERVER_PAUSE
         * and pauseCancelAction of the step is PauseOrCancelAction.ACTION_RECOVER
         */
        boolean useDistributorIndex = !PauseRecover.NO_RECOVER.equals(pauseRecover) &&
            PauseOrCancelAction.ACTION_RECOVER.equals(step.getPauseOrCancelAction());

        final int tenantId = VitamThreadUtils.getVitamSession().getTenantId();
        step.setStepResponses(new ItemStatus(step.getStepName()));

        // Explicitly refreshes ElasticSearch indexes for the current tenant
        // (Everything written is now searchable)
        try (MetaDataClient metadataClient = MetaDataClientFactory.getInstance().getClient()) {
            try {
                metadataClient.refreshUnits();
                metadataClient.refreshObjectGroups();
            } catch (MetaDataClientServerException e) {
                LOGGER.error("Error while refresh metadata indexes", e);
                return step.getStepResponses().increment(StatusCode.FATAL);
            }
        }
        try {
            currentSteps.put(operationId, step);
            // update workParams
            workParams.putParameterValue(WorkerParameterName.workflowStatusKo,
                processDataAccess.findOneProcessWorkflow(operationId, tenantId).getStatus().name());

            List<String> objectsList = new ArrayList<>();
            if (step.getDistribution().getKind().equals(DistributionKind.LIST_ORDERING_IN_FILE)) {
                try (final WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {
                    // Test regarding Unit to be indexed
                    if (DistributionType.Units == step.getDistribution().getType()) {
                        // get the file to retrieve the GUID
                        Response response = null;
                        InputStream levelFile = null;
                        final JsonNode levelFileJson;
                        try {
                            response = workspaceClient.getObject(workParams.getContainerName(),
                                step.getDistribution().getElement());
                            levelFile = (InputStream) response.getEntity();
                            levelFileJson = JsonHandler.getFromInputStream(levelFile);
                        } finally {
                            StreamUtils.closeSilently(levelFile);
                            workspaceClient.consumeAnyEntityAndClose(response);
                        }
                        final Iterator<Entry<String, JsonNode>> iteratorLevelFile = levelFileJson.fields();
                        while (iteratorLevelFile.hasNext()) {
                            final Entry<String, JsonNode> guidFieldList = iteratorLevelFile.next();
                            final String level = guidFieldList.getKey();
                            final JsonNode guid = guidFieldList.getValue();
                            if (guid != null && guid.size() > 0) {
                                for (final JsonNode _idGuid : guid) {
                                    // include the GUID in the new URI
                                    objectsList.add(_idGuid.asText() + JSON_EXTENSION);
                                }
                                boolean distributorIndexUsed =
                                    distributeOnList(workParams, step, level, objectsList, useDistributorIndex,
                                        tenantId);
                                /*
                                 * If the distributorIndex is used in the previous level
                                 * Then do not use index in the next level
                                 */
                                if (useDistributorIndex && distributorIndexUsed) {
                                    useDistributorIndex = false;
                                }
                                objectsList.clear();

                                // If fatal occurs, do not continue distribution
                                if (step.getStepResponses().getGlobalStatus().isGreaterOrEqualToFatal()) {
                                    break;
                                }
                            }
                        }
                    }
                }
            } else if (step.getDistribution().getKind().equals(DistributionKind.LIST_IN_DIRECTORY)) {
                // List from Storage
                try (final WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {

                    final List<URI> objectsListUri =
                        JsonHandler.getFromStringAsTypeReference(
                            workspaceClient.getListUriDigitalObjectFromFolder(workParams.getContainerName(),
                                step.getDistribution().getElement())
                                .toJsonNode().get("$results").get(0).toString(),
                            new TypeReference<List<URI>>() {
                            });
                    for (URI uri : objectsListUri) {
                        objectsList.add(uri.getPath());
                    }
                    workParams.setObjectMetadataList(Collections.emptyList());
                    // Iterate over Objects List
                    distributeOnList(workParams, step, NOLEVEL, objectsList, useDistributorIndex, tenantId);
                }
            } else if (step.getDistribution().getKind().equals(DistributionKind.LIST_IN_FILE)) {
                try (final WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {
                    // List from Workspace
                    Response response = null;
                    final JsonNode ogIdList;
                    try {
                        response =
                            workspaceClient
                                .getObject(workParams.getContainerName(), step.getDistribution().getElement());
                        ogIdList = JsonHandler.getFromInputStream((InputStream) response.getEntity());
                    } finally {
                        workspaceClient.consumeAnyEntityAndClose(response);
                    }
                    if (ogIdList.isArray()) {
                        for (JsonNode node : ogIdList) {
                            objectsList.add(node.textValue());
                        }
                    }
                    // Iterate over Objects List
                    distributeOnList(workParams, step, NOLEVEL, objectsList, useDistributorIndex, tenantId);
                }
            } else if (step.getDistribution().getKind().equals(DistributionKind.LIST_IN_JSONL_FILE)) {

                // distribute on stream

                File tmpDirectory = new File(VitamConfiguration.getVitamTmpFolder());

                try (InputStream inputStream = new WorkspaceBufferingInputStream(workspaceClientFactory,
                    workParams.getContainerName(), step.getDistribution().getElement(),
                    serverConfiguration.getMaxDistributionOnDiskBufferSize(),
                    serverConfiguration.getMaxDistributionInMemoryBufferSize(), tmpDirectory);
                    BufferedReader br = new BufferedReader(
                        new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

                    distributeOnStream(workParams, step, br, useDistributorIndex, tenantId);
                }

            } else {
                // update the number of element to process
                if (step.getDistribution().getElement() == null ||
                    step.getDistribution().getElement().trim().isEmpty()) {
                    objectsList.add(workParams.getContainerName());
                } else {
                    objectsList.add(step.getDistribution().getElement());
                }
                distributeOnList(workParams, step, NOLEVEL, objectsList, useDistributorIndex, tenantId);
            }
        } catch (final IllegalArgumentException e) {
            step.getStepResponses().increment(StatusCode.FATAL);
            LOGGER.error("Illegal Argument Exception", e);
        } catch (final HandlerNotFoundException e) {
            step.getStepResponses().increment(StatusCode.FATAL);
            LOGGER.error("Handler Not Found Exception", e);
        } catch (final PauseCancelException e) {
            // Pause or Cancel occurred
            return step.getStepResponses();
        } catch (final Exception e) {
            step.getStepResponses().increment(StatusCode.FATAL);
            LOGGER.error(EXCEPTION_MESSAGE, e);
        } finally {
            currentSteps.remove(operationId);
        }
        return step.setPauseOrCancelAction(PauseOrCancelAction.ACTION_COMPLETE).getStepResponses();
    }

    /**
     * The returned boolean is used in case where useDistributorIndex is true
     * if the returned boolean false, means that we want that useDistributorIndex should keep true
     * Else if the returned boolean true, means we have already used the distibutorIndex,
     * Then in case of multi-level for the next level do not use the distributorIndex
     *
     * @param workerParameters
     * @param step
     * @param objectsList
     * @param tenantId
     * @return return true if distributor index is used false else
     * @throws ProcessingException
     */
    private boolean distributeOnList(WorkerParameters workerParameters, Step step, String level,
        List<String> objectsList, boolean initFromDistributorIndex, Integer tenantId) throws ProcessingException {

        final String operationId = workerParameters.getContainerName();
        final String requestId = VitamThreadUtils.getVitamSession().getRequestId();
        final String contractId = VitamThreadUtils.getVitamSession().getContractId();
        final String contextId = VitamThreadUtils.getVitamSession().getContextId();
        final String applicationId = VitamThreadUtils.getVitamSession().getApplicationSessionId();
        final String uniqueStepId = step.getId();

        if (objectsList == null || objectsList.isEmpty()) {
            step.getStepResponses().setItemsStatus(OBJECTS_LIST_EMPTY,
                new ItemStatus(OBJECTS_LIST_EMPTY).increment(step.getDistribution().getStatusOnEmptyDistribution()));
            return false;
        }

        int offset = 0;
        int sizeList = objectsList.size();
        boolean updateElementToProcess = true;
        DistributorIndex distributorIndex;

        final List<String> remainingElementsFromRecover = new ArrayList<>();
        /*
         * initFromDistributorIndex true if start after stop
         *
         * Get the distributor Index from the workspace
         * the current step identifier should be equals to the step identifier in the distributorIndex
         * else the current step is not correctly initialized in th state machine
         *
         * In the current step in case of the multiple level,
         * if the current level is not equals to the level in the initFromDistributorIndex
         * Then return false to passe to the next step
         */
        if (initFromDistributorIndex) {
            try {
                distributorIndex = processDataManagement.getDistributorIndex(DISTRIBUTOR_INDEX, operationId);
                if (null == distributorIndex) {
                    throw new ProcessingException("DistributorIndex not found for the operation" + operationId);
                }

                if (!distributorIndex.getStepId().equals(step.getId())) {
                    throw new ProcessingException(
                        "You run the wrong step " + step.getId() + ". The step from saved distributor index is : " +
                            distributorIndex.getStepId());
                }
                /*
                 * Handle the next level if the current level is not equals to the distributorIndex level
                 * This mean that the current level us already treated
                 */
                if (!distributorIndex.getLevel().equals(level)) {
                    return false;
                }
                /*
                 * If all elements of the step are treated then response with the ItemStatus of the distributorIndex
                 */
                if (distributorIndex.isLevelFinished()) {
                    step.setStepResponses(distributorIndex.getItemStatus());
                    return true;
                }
                /*
                 * Initialize from distributor index
                 */
                offset = distributorIndex.getOffset();
                distributorIndex.getItemStatus().getItemsStatus()
                    .remove(PauseOrCancelAction.ACTION_PAUSE.name());
                step.setStepResponses(distributorIndex.getItemStatus());
                /*
                 * As elements to process are calculated before stop of the server,
                 * do not recalculate them after restart
                 */
                updateElementToProcess = false;
                if (null != distributorIndex.getRemainingElements()) {
                    remainingElementsFromRecover.addAll(distributorIndex.getRemainingElements());
                }

            } catch (Exception e) {
                throw new ProcessingException("Can't get distibutor index from workspace", e);
            }
        }
        /*
         * Update only if level is finished in the distributorIndex
         * In the cas of multiple level, we add the size of each level
         * Prevent adding twice the size of the current executing level
         */
        if (updateElementToProcess) {
            // update the number of element to process before start
            processDataAccess.updateStep(operationId, uniqueStepId, sizeList, false, tenantId);
        }

        final Set<ItemStatus> cancelled = new HashSet<>();
        final Set<ItemStatus> paused = new HashSet<>();
        boolean fatalOccurred = false;

        while (offset < sizeList && !fatalOccurred) {

            int bulkSize = findBulkSize(step.getDistribution());
            int batchSize = VitamConfiguration.getDistributeurBatchSize() * bulkSize;

            int nextOffset = sizeList > offset + batchSize ? offset + batchSize : sizeList;
            List<String> subList = objectsList.subList(offset, nextOffset);
            List<CompletableFuture<ItemStatus>> completableFutureList = new ArrayList<>();
            List<WorkerTask> currentWorkerTaskList = new ArrayList<>();

            /*
             * When server stop and in the batch of elements we have remaining elements (not yet treated)
             * Then after restart we treat only those not yet treated elements of this batch
             * If all elements of the batch were treated,
             * then at this point, we are automatically in the new batch
             * and we have to treat all elements of this batch
             */
            boolean emptyRemainingElements = remainingElementsFromRecover.isEmpty();

            if (!emptyRemainingElements) {
                subList = new ArrayList<>(subList);
                subList.retainAll(remainingElementsFromRecover);
            }

            prepareCurrentWorkerTaskAndCompletableLists(workerParameters, step, tenantId, operationId, requestId,
                contractId, contextId, applicationId, bulkSize, subList, completableFutureList, currentWorkerTaskList);

            CompletableFuture<List<ItemStatus>> sequence = sequence(completableFutureList);

            CompletableFuture<ItemStatus> reduce =
                getItemStatusCompletableFuture(step, cancelled, paused, sequence);

            try {
                // store information
                final ItemStatus itemStatus = reduce.get();
                /*
                 * As pause can occurs on not started WorkerTask,
                 * so we have to get the corresponding elements in order to execute them after restart
                 */
                List<String> remainingElements = new ArrayList<>();
                currentWorkerTaskList.forEach(e -> {
                    if (!e.isCompleted()) {
                        remainingElements.add(e.getObjectName());
                    }
                });


                if (itemStatus.getGlobalStatus().isGreaterOrEqualToFatal()) {
                    // Do not update index as we have to restart from old saved index
                    checkCancelledOrPaused(cancelled, paused);
                    return true;
                }
                if (remainingElements.isEmpty()) {
                    offset = nextOffset;
                }

                distributorIndex =
                    new DistributorIndex(level, offset, itemStatus, requestId, uniqueStepId, remainingElements);

                // All elements of the current level are treated so finish it
                if (offset >= sizeList) {
                    distributorIndex.setLevelFinished(true);
                }

                // update persisted DistributorIndex if not Fatal
                updatePersitedDistributorIndexIfNotFatal(operationId, offset, distributorIndex, itemStatus,
                    "Error while persist DistributorIndex");

                checkCancelledOrPaused(cancelled, paused);

            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }


        }
        return true;
    }

    private void updatePersitedDistributorIndexIfNotFatal(String operationId, int offset,
        DistributorIndex distributorIndex, ItemStatus itemStatus, String message) throws ProcessingException {
        try {
            processDataManagement.persistDistributorIndex(DISTRIBUTOR_INDEX, operationId, distributorIndex);
            LOGGER
                .debug("Store for the container " + operationId + " the DistributorIndex offset" + offset +
                    " GlobalStatus " + itemStatus.getGlobalStatus());
        } catch (Exception e) {
            throw new ProcessingException(message, e);
        }
    }

    @VisibleForTesting
    Integer findBulkSize(Distribution distribution) {
        return firstNonNull(distribution.getBulkSize(), VitamConfiguration.getWorkerBulkSize());
    }

    /**
     * Distribution on stream.
     *
     * @param workerParameters workerParameters
     * @param step step
     * @param bufferedReader
     * @param initFromDistributorIndex
     * @param tenantId
     * @return
     */
    private boolean distributeOnStream(WorkerParameters workerParameters, Step step,
        BufferedReader bufferedReader, boolean initFromDistributorIndex, Integer tenantId)
        throws ProcessingException {

        final String operationId = workerParameters.getContainerName();
        final String requestId = VitamThreadUtils.getVitamSession().getRequestId();
        final String contractId = VitamThreadUtils.getVitamSession().getContractId();
        final String contextId = VitamThreadUtils.getVitamSession().getContextId();
        final String applicationId = VitamThreadUtils.getVitamSession().getApplicationSessionId();

        // initialization
        int offset = 0;

        boolean updateElementToProcess = true;
        DistributorIndex distributorIndex;
        final List<String> remainingElementsFromRecover = new ArrayList<>();

        /*
         * Check if the initialization is from the DistributorIndex :
         *
         * initFromDistributorIndex true if start after stop
         *
         * Get the distributor Index from the workspace
         * the current step identifier should be equals to the step identifier in the distributorIndex
         * else the current step is not correctly initialized in th state machine
         *
         * In the current step in case of the multiple level,
         * if the current level is not equals to the level in the initFromDistributorIndex
         * Then return false to passe to the next step
         */


        if (initFromDistributorIndex) {

            try {
                distributorIndex = processDataManagement.getDistributorIndex(DISTRIBUTOR_INDEX, operationId);
                if (distributorIndex == null) {
                    throw new ProcessingException(DISTRIBUTOR_INDEX_NOT_FOUND_FOR_THE_OPERATION + operationId);
                }

                if (!distributorIndex.getStepId().equals(step.getId())) {
                    throw new ProcessingException(
                        "You run the wrong step " + step.getId() + ". The step from saved distributor index is : " +
                            distributorIndex.getStepId());
                }



                /*
                 * If all elements of the step are treated then response with the ItemStatus of the distributorIndex
                 */
                if (distributorIndex.isLevelFinished()) {
                    step.setStepResponses(distributorIndex.getItemStatus());
                    return true;
                }

                /*
                 * Initialization from DistributorIndex
                 */
                offset = distributorIndex.getOffset();

                skipOffsetLines(bufferedReader, offset);

                distributorIndex.getItemStatus().getItemsStatus()
                    .remove(PauseOrCancelAction.ACTION_PAUSE.name());
                step.setStepResponses(distributorIndex.getItemStatus());

                /*
                 * As elements to process are calculated before stop of the server,
                 * do not recalculate them after restart
                 */
                updateElementToProcess = false;
                if (distributorIndex.getRemainingElements() != null &&
                    !distributorIndex.getRemainingElements().isEmpty()) {

                    remainingElementsFromRecover.addAll(distributorIndex.getRemainingElements());
                }

            } catch (VitamException e) {
                throw new ProcessingException(
                    AN_EXCEPTION_HAS_BEEN_THROWN_WHEN_TRYING_TO_GET_DISTIBUTOR_INDEX_FROM_WORKSPACE, e);
            }
        }

        final Set<ItemStatus> cancelled = new HashSet<>();
        final Set<ItemStatus> paused = new HashSet<>();


        int bulkSize = findBulkSize(step.getDistribution());
        int globalBatchSize = VitamConfiguration.getDistributeurBatchSize() * bulkSize;

        PeekingIterator<String> linesPeekIterator = new PeekingIterator<>(bufferedReader.lines().iterator());

        boolean isEmptyDistribution = !initFromDistributorIndex && !linesPeekIterator.hasNext();
        if (isEmptyDistribution) {
            step.getStepResponses().setItemsStatus(OBJECTS_LIST_EMPTY,
                new ItemStatus(OBJECTS_LIST_EMPTY).increment(step.getDistribution().getStatusOnEmptyDistribution()));
            return false;
        }

        while (linesPeekIterator.hasNext()) {

            int nextOffset = offset + globalBatchSize;
            List<JsonLineModel> distributionList = new ArrayList<>();
            List<CompletableFuture<ItemStatus>> completableFutureList = new ArrayList<>();
            List<WorkerTask> currentWorkerTaskList = new ArrayList<>();

            for (int i = offset; i < nextOffset && linesPeekIterator.hasNext(); i++) {

                JsonLineModel currentJsonLineModel = readJsonLineModelFromBufferFromString(linesPeekIterator.next());

                distributionList.add(currentJsonLineModel);

                JsonLineModel nextJsonLineModel = null;

                if (linesPeekIterator.hasNext()) {
                    nextJsonLineModel = readJsonLineModelFromBufferFromString(linesPeekIterator.peek());
                }

                boolean isLevelCompatible =
                    nextJsonLineModel != null &&
                        currentJsonLineModel.getDistribGroup() != null &&
                        nextJsonLineModel.getDistribGroup() != null &&
                        !currentJsonLineModel.getDistribGroup().equals(nextJsonLineModel.getDistribGroup());
                //consider Level
                if (isLevelCompatible) {
                    break;
                }
            }

            /*
             * Update only if level is finished in the distributorIndex
             * In the cas of multiple level, we add the size of each level
             * Prevent adding twice the size of the current executing level
             */
            if (updateElementToProcess) {
                // update the number of elements to process before start
                processDataAccess.updateStep(operationId, step.getId(), distributionList.size(), false, tenantId);
            }

            /*
             * When server stop and in the batch of elements we have remaining elements (not yet treated)
             * Then after restart we treat only those not yet treated elements of this batch
             * If all elements of the batch were treated,
             * then at this point, we are automatically in the new batch
             * and we have to treat all elements of this batch
             */
            if (!remainingElementsFromRecover.isEmpty()) {

                ArrayList<JsonLineModel> retainedList = new ArrayList<>();
                for (JsonLineModel model : distributionList) {
                    if (remainingElementsFromRecover.contains(model.getId())) {
                        retainedList.add(model);
                    }
                }
                distributionList = retainedList;
            }

            prepareCurrentWorkerTaskAndCompletableListsOnStream(workerParameters, step, tenantId, operationId,
                requestId, contractId, contextId, applicationId, bulkSize, distributionList, completableFutureList,
                currentWorkerTaskList);

            CompletableFuture<List<ItemStatus>> sequence = sequence(completableFutureList);

            CompletableFuture<ItemStatus> reduce = getItemStatusCompletableFuture(step, cancelled, paused, sequence);

            try {
                // store information
                final ItemStatus itemStatus = reduce.get();
                /*
                 * As pause can occurs on not started WorkerTask,
                 * so we have to get the corresponding elements in order to execute them after restart
                 */
                List<String> remainingElements =
                    currentWorkerTaskList.stream().filter(x -> !x.isCompleted()).map(WorkerTask::getObjectName)
                        .collect(Collectors.toList());

                if (itemStatus.getGlobalStatus().isGreaterOrEqualToFatal()) {
                    // Do not update index as we have to restart from old saved index
                    checkCancelledOrPaused(cancelled, paused);
                    return true;

                }

                if (remainingElements.isEmpty()) {
                    offset = nextOffset;
                }
                // update && persist DistributorIndex if not Fatal
                distributorIndex =
                    new DistributorIndex(ProcessDistributor.NOLEVEL, offset, itemStatus, requestId, step.getId(),
                        remainingElements);
                // All elements of the current level are treated so finish it
                if (!linesPeekIterator.hasNext()) {
                    distributorIndex.setLevelFinished(true);
                }
                updatePersitedDistributorIndexIfNotFatal(operationId, offset, distributorIndex, itemStatus,
                    AN_EXCEPTION_HAS_BEEN_THROWN_WHEN_TRYING_TO_PERSIST_DISTRIBUTOR_INDEX);


                checkCancelledOrPaused(cancelled, paused);

            } catch (InterruptedException | ExecutionException e) {

                throw new ProcessingException(e);
            }
        }
        return true;
    }

    private void skipOffsetLines(BufferedReader bufferedReader, int offset) throws ProcessingException {
        for (int i = 0; i < offset; i++) {
            try {
                bufferedReader.readLine();
            } catch (IOException e) {
                throw new ProcessingException(e);
            }
        }
    }

    private JsonLineModel readJsonLineModelFromBufferFromString(String value) throws ProcessingException {
        try {
            return JsonHandler.getFromString(value, JsonLineModel.class);
        } catch (InvalidParseOperationException e) {
            throw new ProcessingException("Invalid Model", e);
        }
    }

    private void prepareCurrentWorkerTaskAndCompletableLists(WorkerParameters workerParameters, Step step,
        Integer tenantId, String operationId, String requestId, String contractId, String contextId,
        String applicationId,
        int bulkSize, List<String> subList, List<CompletableFuture<ItemStatus>> completableFutureList,
        List<WorkerTask> currentWorkerTaskList) {
        int subOffset = 0;
        int subListSize = subList.size();

        while (subOffset < subListSize) {
            int nextSubOffset = Math.min(subListSize, subOffset + bulkSize);

            // split the list of items to be processed according to the capacity of the workers
            List<String> newSubList = subList.subList(subOffset, nextSubOffset);

            // prepare & instanciate the worker tasks
            workerParameters.setObjectNameList(newSubList);

            final WorkerTask workerTask =
                new WorkerTask(
                    new DescriptionStep(step, ((DefaultWorkerParameters) workerParameters).newInstance()),
                    tenantId, requestId, contractId, contextId, applicationId, workerClientFactory);

            currentWorkerTaskList.add(workerTask);
            completableFutureList.add(prepare(workerTask, operationId, tenantId));

            subOffset = nextSubOffset;
        }
    }

    private void prepareCurrentWorkerTaskAndCompletableListsOnStream(WorkerParameters workerParameters, Step step,
        Integer tenantId, String operationId, String requestId, String contractId, String contextId,
        String applicationId,
        int bulkSize, List<JsonLineModel> distributionList, List<CompletableFuture<ItemStatus>> completableFutureList,
        List<WorkerTask> currentWorkerTaskList) {
        int distribOffSet = 0;
        int distributionSize = distributionList.size();

        while (distribOffSet < distributionSize) {
            int nextDistributionOffset = Math.min(distributionSize, distribOffSet + bulkSize);

            // split the list of items to be processed according to the capacity of the workers
            List<JsonLineModel> newSubList = distributionList.subList(distribOffSet, nextDistributionOffset);

            // prepare & instantiate the worker tasks
            workerParameters
                .setObjectNameList(newSubList.stream().map(JsonLineModel::getId).collect(Collectors.toList()));
            workerParameters
                .setObjectMetadataList(newSubList.stream().map(JsonLineModel::getParams).collect(Collectors.toList()));

            final WorkerTask workerTask =
                new WorkerTask(
                    new DescriptionStep(step, ((DefaultWorkerParameters) workerParameters).newInstance()),
                    tenantId, requestId, contractId, contextId, applicationId, workerClientFactory);

            currentWorkerTaskList.add(workerTask);
            completableFutureList.add(prepare(workerTask, operationId, tenantId));

            distribOffSet = nextDistributionOffset;
        }
    }

    private CompletableFuture<ItemStatus> getItemStatusCompletableFuture(Step step, Set<ItemStatus> cancelled,
        Set<ItemStatus> paused, CompletableFuture<List<ItemStatus>> sequence) {

        return sequence
            .thenApplyAsync((List<ItemStatus> is) -> is.stream()
                .reduce(step.getStepResponses(), (identity, iterationItemStatus) -> {
                    // compute cancelled actions
                    if (PauseOrCancelAction.ACTION_CANCEL.name().equals(iterationItemStatus.getItemId()) &&
                        iterationItemStatus.getGlobalStatus().equals(StatusCode.UNKNOWN)) {
                        cancelled.add(iterationItemStatus);
                    }
                    // compute paused actions
                    if (PauseOrCancelAction.ACTION_PAUSE.name().equals(iterationItemStatus.getItemId()) &&
                        iterationItemStatus.getGlobalStatus().equals(StatusCode.UNKNOWN)) {
                        paused.add(iterationItemStatus);
                    }
                    return identity.setItemsStatus(iterationItemStatus);
                }));

    }

    private void checkCancelledOrPaused(Set<ItemStatus> cancelled, Set<ItemStatus> paused) {
        if (!cancelled.isEmpty()) {
            throw new PauseCancelException(PauseOrCancelAction.ACTION_CANCEL);
        }
        if (!paused.isEmpty()) {
            throw new PauseCancelException(PauseOrCancelAction.ACTION_PAUSE);
        }
    }

    /**
     * @param task task
     * @param operationId
     * @param tenantId
     * @return
     */
    private CompletableFuture<ItemStatus> prepare(WorkerTask task, String operationId, int tenantId) {
        Step step = task.getStep();
        final WorkerFamilyManager wmf = workerManager.findWorkerBy(step.getWorkerGroupId());
        if (null == wmf) {

            LOGGER.error("No WorkerFamilyManager found for : " + step.getWorkerGroupId());
            return CompletableFuture.completedFuture(new ItemStatus(step.getStepName()).increment(StatusCode.FATAL));
        }
        return CompletableFuture
            .supplyAsync(task, wmf)
            .exceptionally((completionException) -> {
                LOGGER.error("Exception occurred when executing task", completionException);
                Throwable cause = completionException.getCause();
                ObjectNode evDetDetail = JsonHandler.createObjectNode();

                if (cause instanceof WorkerUnreachableException) {
                    WorkerUnreachableException wue = (WorkerUnreachableException) cause;
                    evDetDetail.put("Error", "Distributor lost connection with worker (" + wue.getWorkerId() +
                        "). The worker will be unregistered.");
                    try {
                        LOGGER.warn(
                            "The worker (" + step.getWorkerGroupId() + ") will be unregistered as it is Unreachable",
                            wue.getWorkerId());
                        workerManager.unregisterWorker(step.getWorkerGroupId(), wue.getWorkerId());
                    } catch (WorkerFamilyNotFoundException | IOException e1) {
                        LOGGER.error("Exception while unregister worker " + wue.getWorkerId(), cause);
                    }
                } else {
                    evDetDetail.put("Error", "Error occurred while handling step by the distributor");
                }

                return new ItemStatus(step.getStepName())
                    .setItemsStatus(step.getStepName(),
                        new ItemStatus(step.getStepName()).setEvDetailData(JsonHandler.unprettyPrint(evDetDetail))
                            .increment(StatusCode.FATAL));
            })
            .thenApply(is -> {
                //Do not update processed if pause or cancel occurs or if status is Fatal
                if (StatusCode.UNKNOWN.equals(is.getGlobalStatus()) || StatusCode.FATAL.equals(is.getGlobalStatus())) {
                    return is;
                }
                // update processed elements
                processDataAccess
                    .updateStep(operationId, step.getId(), task.getObjectNameList().size(), true, tenantId);
                return is;
            });
    }

    private static <T> CompletableFuture<List<T>> sequence(List<CompletableFuture<T>> futures) {
        CompletableFuture<Void> allDoneFuture =
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
        return allDoneFuture
            .thenApply(v -> futures.stream().map(CompletableFuture::join).collect(Collectors.<T>toList()));
    }

    @Override
    public void close() {
        // Nothing
    }


    @Override
    public ProcessDataAccess getProcessDataAccess() {
        return processDataAccess;
    }

    @Override
    public ProcessDataManagement getProcessDataManagement() {
        return processDataManagement;
    }

    @Override
    public IWorkerManager getWorkerManager() {
        return workerManager;
    }

    @Override
    public WorkspaceClientFactory getWorkspaceClientFactory() {
        return workspaceClientFactory;
    }

    @Override
    public WorkerClientFactory getWorkerClientFactory() {
        return workerClientFactory;
    }
}
