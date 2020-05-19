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
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.config.ServerConfiguration;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.exception.WorkerFamilyNotFoundException;
import fr.gouv.vitam.processing.common.metrics.CommonProcessingMetrics;
import fr.gouv.vitam.processing.common.model.DistributorIndex;
import fr.gouv.vitam.processing.common.model.PauseRecover;
import fr.gouv.vitam.processing.common.model.ProcessStep;
import fr.gouv.vitam.processing.common.parameter.DefaultWorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.data.core.management.ProcessDataManagement;
import fr.gouv.vitam.processing.data.core.management.WorkspaceProcessDataManagement;
import fr.gouv.vitam.processing.distributor.api.IWorkerManager;
import fr.gouv.vitam.processing.distributor.api.ProcessDistributor;
import fr.gouv.vitam.worker.client.WorkerClientFactory;
import fr.gouv.vitam.worker.client.exception.PauseCancelException;
import fr.gouv.vitam.worker.client.exception.WorkerUnreachableException;
import fr.gouv.vitam.worker.common.DescriptionStep;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import fr.gouv.vitam.workspace.client.WorkspaceBufferingInputStream;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import io.prometheus.client.Histogram;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.google.common.base.MoreObjects.firstNonNull;

/**
 * The Process Distributor call the workers and intercept the response for manage a post actions step
 * <p>
 * <p>
 * <pre>
 * - handle listing of items through a limited arraylist (memory) and through iterative (async) listing from
 * Workspace
 * - handle result in FATAL mode from one distributed item to stop the distribution in FATAL mode (do not
 * continue)
 * - try to handle distribution on 1 or on many as the same loop (so using a default arrayList of 1)
 * - handle error level using order in enum in ProcessResponse.getGlobalProcessStatusCode instead of manually comparing:
 * </pre>
 */
public class ProcessDistributorImpl implements ProcessDistributor {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProcessDistributorImpl.class);

    private static final String AN_EXCEPTION_HAS_BEEN_THROWN_WHEN_TRYING_TO_GET_DISTIBUTOR_INDEX_FROM_WORKSPACE =
        "An exception has been thrown when trying to get distributor index from workspace";
    private static final String AN_EXCEPTION_HAS_BEEN_THROWN_WHEN_TRYING_TO_PERSIST_DISTRIBUTOR_INDEX =
        "An Exception has been thrown when trying to persist DistributorIndex";

    private static final TypeReference<List<URI>> LIST_URI_TYPE_REFERENCE = new TypeReference<>() {
    };

    private final ProcessDataManagement processDataManagement;
    private final IWorkerManager workerManager;
    private final WorkspaceClientFactory workspaceClientFactory;
    private final MetaDataClientFactory metaDataClientFactory;
    private final WorkerClientFactory workerClientFactory;
    private final ServerConfiguration serverConfiguration;

    /**
     * Empty constructor
     *
     * @param workerManager
     * @param serverConfiguration
     */
    public ProcessDistributorImpl(IWorkerManager workerManager, ServerConfiguration serverConfiguration) {
        this(workerManager, serverConfiguration, WorkspaceProcessDataManagement.getInstance(),
            WorkspaceClientFactory.getInstance(), MetaDataClientFactory.getInstance(), null);
    }

    @VisibleForTesting
    public ProcessDistributorImpl(IWorkerManager workerManager, ServerConfiguration serverConfiguration,
        ProcessDataManagement processDataManagement, WorkspaceClientFactory workspaceClientFactory,
        MetaDataClientFactory metaDataClientFactory, WorkerClientFactory workerClientFactory) {
        this.workerManager = workerManager;
        this.serverConfiguration = serverConfiguration;
        this.workspaceClientFactory = workspaceClientFactory;
        this.metaDataClientFactory = metaDataClientFactory;
        this.workerClientFactory = workerClientFactory;
        this.processDataManagement = processDataManagement;
        ParametersChecker
            .checkParameter("Parameters are required.", workerManager, serverConfiguration, processDataManagement,
                metaDataClientFactory, workspaceClientFactory);
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
         * use index only if pauseCancelAction of the step is PauseOrCancelAction.ACTION_RECOVER
         */
        boolean useDistributorIndex = PauseOrCancelAction.ACTION_RECOVER.equals(step.getPauseOrCancelAction());

        final int tenantId = VitamThreadUtils.getVitamSession().getTenantId();
        step.setStepResponses(new ItemStatus(step.getStepName()));

        // Explicitly refreshes ElasticSearch indexes for the current tenant
        // (Everything written is now searchable)
        try (MetaDataClient metadataClient = metaDataClientFactory.getClient()) {
            try {
                metadataClient.refreshUnits();
                metadataClient.refreshObjectGroups();
            } catch (MetaDataClientServerException e) {
                LOGGER.error("Error while refresh metadata indexes", e);
                return step.getStepResponses().increment(StatusCode.FATAL);
            }
        }
        try {
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
                            LIST_URI_TYPE_REFERENCE);
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
        } catch (final PauseCancelException e) {
            LOGGER.debug("Operation Cancel", e);
            // Pause or Cancel occurred
            return step.getStepResponses();
        } catch (final Exception e) {
            step.getStepResponses().increment(StatusCode.FATAL);
            LOGGER.error(EXCEPTION_MESSAGE, e);
        }

        if (step.getStepResponses().getGlobalStatus().isGreaterOrEqualToFatal()) {
            return step.getStepResponses();
        } else {
            return step.setPauseOrCancelAction(PauseOrCancelAction.ACTION_COMPLETE).getStepResponses();
        }
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
                Optional<DistributorIndex> distributorIndexOptional =
                    processDataManagement.getDistributorIndex(operationId);

                // If we have saved an index and it concerns the current step
                if (distributorIndexOptional.isPresent() &&
                    distributorIndexOptional.get().getStepId().equals(step.getId())) {

                    DistributorIndex distributorIndex = distributorIndexOptional.get();

                    /*
                     * Handle the next level if the current level is not equals to the distributorIndex level
                     * This mean that the current level is already treated
                     */
                    if (!distributorIndex.getLevel().equals(level)) {
                        return false;
                    }

                    /*
                     * If all elements of the step are treated then response with the ItemStatus of the distributorIndex
                     */
                    if (distributorIndex.isLevelFinished()) {
                        step.setStepResponses(distributorIndex.getItemStatus());
                        step.getStepResponses().clearStatusMeterFatal();
                        return true;
                    }
                    /*
                     * Initialize from distributor index
                     */
                    offset = distributorIndex.getOffset();

                    step.setStepResponses(distributorIndex.getItemStatus());
                    /*
                     * As elements to process are calculated before stop of the server,
                     * do not recalculate them after restart
                     */
                    updateElementToProcess = false;
                    if (null != distributorIndex.getRemainingElements()) {
                        remainingElementsFromRecover.addAll(distributorIndex.getRemainingElements());
                    }
                }
            } catch (Exception e) {
                throw new ProcessingException("Can't get distributor index from workspace", e);
            }
        }

        step.getStepResponses().clearStatusMeterFatal();
        /*
         * Update only if level is finished in the distributorIndex
         * In the cas of multiple level, we add the size of each level
         * Prevent adding twice the size of the current executing level
         */
        if (updateElementToProcess) {
            // update the number of element to process before start
            ProcessStep processStep = (ProcessStep) step;
            processStep.getElementToProcess().addAndGet(sizeList);
        }

        while (offset < sizeList) {

            int bulkSize = findBulkSize(step.getDistribution());
            int batchSize = VitamConfiguration.getDistributeurBatchSize() * bulkSize;

            int nextOffset = Math.min(sizeList, offset + batchSize);
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
                remainingElementsFromRecover.clear();
            }

            prepareCurrentWorkerTaskAndCompletableLists(workerParameters, step, tenantId, requestId,
                contractId, contextId, applicationId, bulkSize, subList, completableFutureList, currentWorkerTaskList);

            CompletableFuture<List<ItemStatus>> sequence = sequence(completableFutureList);

            CompletableFuture<ItemStatus> reduce = getItemStatusCompletableFuture(step, sequence);

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
                        remainingElements.addAll(e.getObjectNameList());
                    }
                });

                if (itemStatus.getGlobalStatus().isGreaterOrEqualToFatal()) {
                    // We have to restart all the current offset
                    remainingElements.clear();
                } else if (remainingElements.isEmpty()) {
                    offset = nextOffset;
                }

                DistributorIndex distributorIndex =
                    new DistributorIndex(level, offset, itemStatus, requestId, uniqueStepId, remainingElements);

                // All elements of the current level are treated so finish it
                if (offset >= sizeList && !itemStatus.getGlobalStatus().isGreaterOrEqualToFatal()) {
                    distributorIndex.setLevelFinished(true);
                }

                // update persisted DistributorIndex if not Fatal
                updatePersistedDistributorIndexIfNotFatal(operationId, offset, distributorIndex, itemStatus,
                    "Error while persist DistributorIndex");

                checkCancelledOrPaused(step);

                if (itemStatus.getGlobalStatus().isGreaterOrEqualToFatal()) {

                    return true;
                }

            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
        return true;
    }

    private void updatePersistedDistributorIndexIfNotFatal(String operationId, int offset,
        DistributorIndex distributorIndex, ItemStatus itemStatus, String message) throws ProcessingException {
        try {
            processDataManagement.persistDistributorIndex(operationId, distributorIndex);
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
    private void distributeOnStream(WorkerParameters workerParameters, Step step,
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
                Optional<DistributorIndex> distributorIndexOptional =
                    processDataManagement.getDistributorIndex(operationId);
                // If we have saved an index and it concerns the current step
                if (distributorIndexOptional.isPresent() &&
                    distributorIndexOptional.get().getStepId().equals(step.getId())) {

                    DistributorIndex distributorIndex = distributorIndexOptional.get();

                    /*
                     * If all elements of the step are treated then response with the ItemStatus of the distributorIndex
                     */
                    if (distributorIndex.isLevelFinished()) {
                        step.setStepResponses(distributorIndex.getItemStatus());
                        step.getStepResponses().clearStatusMeterFatal();
                        return;
                    }

                    /*
                     * Initialization from DistributorIndex
                     */
                    offset = distributorIndex.getOffset();

                    skipOffsetLines(bufferedReader, offset);

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
                }
            } catch (VitamException e) {
                throw new ProcessingException(
                    AN_EXCEPTION_HAS_BEEN_THROWN_WHEN_TRYING_TO_GET_DISTIBUTOR_INDEX_FROM_WORKSPACE, e);
            }
        }


        step.getStepResponses().clearStatusMeterFatal();


        int bulkSize = findBulkSize(step.getDistribution());
        int globalBatchSize = VitamConfiguration.getDistributeurBatchSize() * bulkSize;

        PeekingIterator<String> linesPeekIterator = new PeekingIterator<>(bufferedReader.lines().iterator());

        boolean isEmptyDistribution = !initFromDistributorIndex && !linesPeekIterator.hasNext();
        if (isEmptyDistribution) {
            step.getStepResponses().setItemsStatus(OBJECTS_LIST_EMPTY,
                new ItemStatus(OBJECTS_LIST_EMPTY).increment(step.getDistribution().getStatusOnEmptyDistribution()));
            return;
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
                ProcessStep processStep = (ProcessStep) step;
                processStep.getElementToProcess().addAndGet(distributionList.size());
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
                remainingElementsFromRecover.clear();
            }

            prepareCurrentWorkerTaskAndCompletableListsOnStream(workerParameters, step, tenantId, requestId, contractId,
                contextId, applicationId, bulkSize, distributionList, completableFutureList,
                currentWorkerTaskList);

            CompletableFuture<List<ItemStatus>> sequence = sequence(completableFutureList);

            CompletableFuture<ItemStatus> reduce = getItemStatusCompletableFuture(step, sequence);

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
                        remainingElements.addAll(e.getObjectNameList());
                    }
                });

                if (itemStatus.getGlobalStatus().isGreaterOrEqualToFatal()) {
                    remainingElements.clear();
                } else if (remainingElements.isEmpty()) {
                    offset = nextOffset;
                }
                // update && persist DistributorIndex if not Fatal
                DistributorIndex distributorIndex =
                    new DistributorIndex(ProcessDistributor.NOLEVEL, offset, itemStatus, requestId, step.getId(),
                        remainingElements);
                // All elements of the current level are treated so finish it
                if (!linesPeekIterator.hasNext() && !itemStatus.getGlobalStatus().isGreaterOrEqualToFatal()) {
                    distributorIndex.setLevelFinished(true);
                }
                updatePersistedDistributorIndexIfNotFatal(operationId, offset, distributorIndex, itemStatus,
                    AN_EXCEPTION_HAS_BEEN_THROWN_WHEN_TRYING_TO_PERSIST_DISTRIBUTOR_INDEX);


                checkCancelledOrPaused(step);

                if (itemStatus.getGlobalStatus().isGreaterOrEqualToFatal()) {
                    return;
                }

            } catch (InterruptedException | ExecutionException e) {

                throw new ProcessingException(e);
            }
        }
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
        Integer tenantId, String requestId, String contractId, String contextId,
        String applicationId,
        int bulkSize, List<String> subList, List<CompletableFuture<ItemStatus>> completableFutureList,
        List<WorkerTask> currentWorkerTaskList) {
        int subOffset = 0;
        int subListSize = subList.size();

        while (subOffset < subListSize) {
            int nextSubOffset = Math.min(subListSize, subOffset + bulkSize);

            // split the list of items to be processed according to the capacity of the workers
            List<String> newSubList = subList.subList(subOffset, nextSubOffset);

            // prepare & instantiate the worker tasks
            workerParameters.setObjectNameList(newSubList);

            final WorkerTask workerTask =
                new WorkerTask(
                    new DescriptionStep(step, ((DefaultWorkerParameters) workerParameters).newInstance()),
                    tenantId, requestId, contractId, contextId, applicationId, workerClientFactory);

            currentWorkerTaskList.add(workerTask);
            completableFutureList.add(prepare(workerTask, workerParameters.getLogbookTypeProcess()));

            subOffset = nextSubOffset;
        }
    }

    private void prepareCurrentWorkerTaskAndCompletableListsOnStream(WorkerParameters workerParameters, Step step,
        Integer tenantId, String requestId, String contractId, String contextId,
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
            completableFutureList.add(prepare(workerTask, workerParameters.getLogbookTypeProcess()));

            distribOffSet = nextDistributionOffset;
        }
    }

    private CompletableFuture<ItemStatus> getItemStatusCompletableFuture(Step step,
        CompletableFuture<List<ItemStatus>> sequence) {

        return sequence
            .thenApplyAsync((List<ItemStatus> is) -> is.stream()
                // Do not compute PAUSE and CANCEL actions
                .filter(i -> !PauseOrCancelAction.ACTION_CANCEL.name().equals(i.getItemId()))
                .filter(i -> !PauseOrCancelAction.ACTION_PAUSE.name().equals(i.getItemId()))
                // Reduce remaining item status
                .reduce(step.getStepResponses(), ItemStatus::setItemsStatus));

    }

    private void checkCancelledOrPaused(Step step) {
        switch (step.getPauseOrCancelAction()) {
            case ACTION_CANCEL:
            case ACTION_PAUSE:
                throw new PauseCancelException(step.getPauseOrCancelAction());
            default:
                // Nothing to do
        }
    }

    private CompletableFuture<ItemStatus> prepare(WorkerTask task, LogbookTypeProcess logbookTypeProcess) {
        Step step = task.getStep();
        final WorkerFamilyManager wmf = workerManager.findWorkerBy(step.getWorkerGroupId());
        if (null == wmf) {

            LOGGER.error("No WorkerFamilyManager found for : " + step.getWorkerGroupId());
            return CompletableFuture.completedFuture(new ItemStatus(step.getStepName()).increment(StatusCode.FATAL));
        }

        // Add metrics compute duration of waiting time before execution of the task
        Histogram.Timer taskWaitingTimeDuration = CommonProcessingMetrics.WORKER_TASKS_IDLE_DURATION_IN_QUEUE
            .labels(wmf.getFamily(), logbookTypeProcess.name().toLowerCase(), step.getStepName())
            .startTimer();

        task.setTimer(taskWaitingTimeDuration);

        // Add metrics increment as new task created
        CommonProcessingMetrics.CURRENTLY_INSTANTIATED_TASKS
            .labels(wmf.getFamily(), logbookTypeProcess.name().toLowerCase(), step.getStepName())
            .inc();

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

                // Decrement as this task is completed
                CommonProcessingMetrics.CURRENTLY_INSTANTIATED_TASKS
                    .labels(wmf.getFamily(), logbookTypeProcess.name().toLowerCase(), step.getStepName())
                    .dec();

                //Do not update processed if pause or cancel occurs or if status is Fatal
                if (StatusCode.UNKNOWN.equals(is.getGlobalStatus()) || StatusCode.FATAL.equals(is.getGlobalStatus())) {
                    return is;
                }
                // update processed elements
                ProcessStep processStep = (ProcessStep) step;
                processStep.getElementProcessed().addAndGet(task.getObjectNameList().size());
                return is;
            });
    }

    private static <T> CompletableFuture<List<T>> sequence(List<CompletableFuture<T>> futures) {
        CompletableFuture<Void> allDoneFuture =
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        return allDoneFuture
            .thenApply(v -> futures.stream().map(CompletableFuture::join).collect(Collectors.toList()));
    }

    @Override
    public ProcessDataManagement getProcessDataManagement() {
        return processDataManagement;
    }
}
