/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019) <p> contact.vitam@culture.gouv.fr <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently. <p> This software is governed by the CeCILL 2.1 license under French law and
 * abiding by the rules of distribution of free software. You can use, modify and/ or redistribute the software under
 * the terms of the CeCILL 2.1 license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info". <p> As a counterpart to the access to the source code and rights to copy, modify and
 * redistribute granted by the license, users are provided only with a limited warranty and the software's author, the
 * holder of the economic rights, and the successive licensors have only limited liability. <p> In this respect, the
 * user's attention is drawn to the risks associated with loading, using, modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software, that may mean that it is complicated to
 * manipulate, and that also therefore means that it is reserved for developers and experienced professionals having
 * in-depth computer knowledge. Users are therefore encouraged to load and test the software's suitability as regards
 * their requirements in conditions enabling the security of their systems and/or data to be ensured and, more
 * generally, to use and operate it in the same conditions as regards security. <p> The fact that you are presently
 * reading this means that you have had knowledge of the CeCILL 2.1 license and that you accept its terms.
 */
package fr.gouv.vitam.processing.distributor.v2;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.DistributorIndex;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.processing.common.exception.HandlerNotFoundException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.exception.WorkerFamilyNotFoundException;
import fr.gouv.vitam.processing.common.exception.WorkerNotFoundException;
import fr.gouv.vitam.processing.common.model.DistributionKind;
import fr.gouv.vitam.processing.common.model.Step;
import fr.gouv.vitam.processing.common.parameter.DefaultWorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParameterName;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.data.core.ProcessDataAccess;
import fr.gouv.vitam.processing.data.core.ProcessDataAccessImpl;
import fr.gouv.vitam.processing.data.core.management.ProcessDataManagement;
import fr.gouv.vitam.processing.data.core.management.WorkspaceProcessDataManagement;
import fr.gouv.vitam.processing.distributor.api.IWorkerManager;
import fr.gouv.vitam.processing.distributor.api.ProcessDistributor;
import fr.gouv.vitam.worker.client.exception.WorkerExecutorException;
import fr.gouv.vitam.worker.client.exception.WorkerUnreachableException;
import fr.gouv.vitam.worker.common.DescriptionStep;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * The Process Distributor call the workers and intercept the response for manage a post actions step
 * <p>
 *
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
 *       if (statusCode.ordinal() > tempStatusCode.ordinal()) {
 *           statusCode = tempStatusCode;
 *       }
 *      if (statusCode.ordinal() > StatusCode.KO.ordinal()) {
 *           break;
 *       }
 *     }
 *  </code>
 * </pre>
 */
public class ProcessDistributorImpl implements ProcessDistributor {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProcessDistributorImpl.class);
    private static final String UNITS_LEVEL = "UnitsLevel";
    private static final String JSON_EXTENSION = ".json";
    private static final String EXCEPTION_MESSAGE =
        "runtime exceptions thrown by the Process distributor during runnig...";
    private static final String INGEST_LEVEL_STACK = "ingestLevelStack.json";
    private static final String OBJECTS_LIST_EMPTY = "OBJECTS_LIST_EMPTY";
    private static final String ELEMENT_UNITS = "Units";
    public static final String DISTRIBUTOR_INDEX = "distributorIndex";

    private final ProcessDataAccess processDataAccess;
    private ProcessDataManagement processDataManagement;
    private final IWorkerManager workerManager;

    /**
     * Empty constructor
     *
     * @param workerManager
     */
    public ProcessDistributorImpl(IWorkerManager workerManager) {
        this.workerManager = workerManager;
        processDataAccess = ProcessDataAccessImpl.getInstance();
        processDataManagement = WorkspaceProcessDataManagement.getInstance();
    }

    /**
     * Temporary method for distribution supporting multi-list
     *
     * @param workParams of type {@link WorkerParameters}
     * @param step the execution step
     * @param operationId the operation id
     * @return the final step status
     */
    @Override
    public ItemStatus distribute(WorkerParameters workParams, Step step, String operationId) {

        ParametersChecker.checkParameter("WorkParams is a mandatory parameter", workParams);
        ParametersChecker.checkParameter("Step is a mandatory parameter", step);
        ParametersChecker.checkParameter("workflowId is a mandatory parameter", operationId);

        final int tenantId = VitamThreadUtils.getVitamSession().getTenantId();

        step.setStepResponses(new ItemStatus(step.getStepName()));

        try {
            // update workParams
            workParams.putParameterValue(WorkerParameterName.workflowStatusKo,
                processDataAccess.findOneProcessWorkflow(operationId, tenantId).getStatus().name());

            List<String> objectsList = new ArrayList<>();
            if (step.getDistribution().getKind().equals(DistributionKind.LIST)) {
                try (final WorkspaceClient workspaceClient = WorkspaceClientFactory.getInstance().getClient()) {


                    // Test regarding Unit to be indexed
                    if (ELEMENT_UNITS.equalsIgnoreCase(step.getDistribution().getElement())) {
                        // get the file to retrieve the GUID
                        final Response response = workspaceClient.getObject(workParams.getContainerName(),
                            UNITS_LEVEL + "/" + INGEST_LEVEL_STACK);
                        final JsonNode levelFileJson;
                        try {
                            final InputStream levelFile = (InputStream) response.getEntity();
                            levelFileJson = JsonHandler.getFromInputStream(levelFile);
                        } finally {
                            workspaceClient.consumeAnyEntityAndClose(response);
                        }
                        final Iterator<Entry<String, JsonNode>> iteratorLevelFile = levelFileJson.fields();

                        while (iteratorLevelFile.hasNext()) {
                            final Entry<String, JsonNode> guidFieldList = iteratorLevelFile.next();
                            final JsonNode guid = guidFieldList.getValue();
                            if (guid != null && guid.size() > 0) {
                                for (final JsonNode _idGuid : guid) {
                                    // include the GUID in the new URI
                                    objectsList.add(_idGuid.asText() + JSON_EXTENSION);
                                }
                                distributeOnList(workParams, step, objectsList, tenantId);
                                objectsList.clear();
                            }
                        }

                    } else {
                        // List from Storage
                        final List<URI> objectsListUri =
                            JsonHandler.getFromStringAsTypeRefence(
                                workspaceClient.getListUriDigitalObjectFromFolder(workParams.getContainerName(),
                                    step.getDistribution().getElement())
                                    .toJsonNode().get("$results").get(0).toString(),
                                new TypeReference<List<URI>>() {});
                        for (URI uri : objectsListUri) {
                            objectsList.add(uri.getPath());
                        }
                        // Iterate over Objects List
                        distributeOnList(workParams, step, objectsList, tenantId);
                    }
                }
            } else {
                // update the number of element to process
                if (step.getDistribution().getElement() == null ||
                    step.getDistribution().getElement().trim().isEmpty()) {
                    objectsList.add(workParams.getContainerName());
                } else {
                    objectsList.add(step.getDistribution().getElement());
                }

                distributeOnList(workParams, step, objectsList, tenantId);
            }
        } catch (final IllegalArgumentException e) {
            step.getStepResponses().increment(StatusCode.FATAL);
            LOGGER.error("Illegal Argument Exception", e);
        } catch (final HandlerNotFoundException e) {
            step.getStepResponses().increment(StatusCode.FATAL);
            LOGGER.error("Handler Not Found Exception", e);

        } catch (final Exception e) {
            step.getStepResponses().increment(StatusCode.FATAL);
            LOGGER.error(EXCEPTION_MESSAGE, e);
        }

        return step.getStepResponses();
    }

    /**
     * @param workerParameters
     * @param step
     * @param objectsList
     * @param tenantId
     * @throws ProcessingException
     */
    private void distributeOnList(WorkerParameters workerParameters, Step step, List<String> objectsList,
        Integer tenantId)
        throws ProcessingException {

        final String container = workerParameters.getContainerName();
        final String requestId = VitamThreadUtils.getVitamSession().getRequestId();
        final String uniqueStepId = step.getId();

        if (objectsList == null || objectsList.isEmpty()) {
            step.getStepResponses().setItemsStatus(OBJECTS_LIST_EMPTY,
                new ItemStatus(OBJECTS_LIST_EMPTY).increment(StatusCode.WARNING));
            return;
        }
        // update the number of element to process before start
        processDataAccess.updateStep(container, uniqueStepId, objectsList.size(), false, tenantId);

        int offset = 0;
        int sizeList = objectsList.size();
        // TODO : for the moment, the restart of a workflow is not handled precisely (when dealing with list) so this
        // code is commented, but later on, it should be used.
        // But it has to be adapted : by adding the information about the level handled (cause now, the test on the
        // offset is wrong if two level contains more than 10 elements or by counting the total number of object handled, and doing proper subtractions.

        // if (sizeList > VitamConfiguration.getDistributeurBatchSize()) {
        // try {
        // DistributorIndex index = processDataManagement.getDistributorIndex(container, DISTRIBUTOR_INDEX);
        // if (null != index) {
        // offset = index.getOffset();
        // step.setStepResponses(index.getItemStatus());
        // }
        //
        // } catch (Exception e) {
        // LOGGER.warn("Can't get distibutor index from workspace", e);
        // }
        // }
        while (offset < sizeList) {
            int nextOffset = sizeList > offset + VitamConfiguration.getDistributeurBatchSize()
                ? offset + VitamConfiguration.getDistributeurBatchSize() : sizeList;

            List<String> subList = objectsList.subList(offset, nextOffset);

            List<CompletableFuture<ItemStatus>> completableFutureList = new ArrayList<>();
            subList.forEach(objectUri -> {
                workerParameters.setObjectName(objectUri);
                final WorkerTask task = new WorkerTask(
                    new DescriptionStep(step, ((DefaultWorkerParameters) workerParameters).newInstance()),
                    tenantId, requestId);
                completableFutureList.add(prepare(task));

            });
            CompletableFuture<List<ItemStatus>> sequence = sequence(completableFutureList);

            CompletableFuture<ItemStatus> reduce = sequence
                .thenApplyAsync((List<ItemStatus> is) -> is.stream().reduce(step.getStepResponses(),
                    ItemStatus::setItemsStatus));

            try {
                // store information
                final ItemStatus itemStatus = reduce.get();
                offset = nextOffset;
                if (sizeList > VitamConfiguration.getDistributeurBatchSize()) {
                    DistributorIndex distributorIndex =
                        new DistributorIndex(offset, itemStatus, requestId, uniqueStepId);
                    try {
                        processDataManagement.persistDistributorIndex(container, DISTRIBUTOR_INDEX, distributorIndex);
                        LOGGER.debug("Store for the container " + container + " the DistributorIndex offset" + offset +
                            " GlobalStatus " + itemStatus.getGlobalStatus());
                    } catch (Exception e) {
                        LOGGER.error("Error while persist DistributorIndex", e);
                    }
                }

            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        // update the number of element to process at the end
        processDataAccess.updateStep(container, step.getId(), 0, true, tenantId);
    }


    private CompletableFuture<ItemStatus> prepare(WorkerTask task) {
        Step step = task.getStep();
        final WorkerFamilyManager wmf = workerManager.findWorkerBy(step.getWorkerGroupId());
        if (null == wmf) {
            LOGGER.error("No WorkerFamilyManager found for : " + step.getWorkerGroupId());
            return CompletableFuture.completedFuture(new ItemStatus(step.getStepName()).increment(StatusCode.FATAL));
        }

        return CompletableFuture
            .supplyAsync(task, wmf)
            .exceptionally((completionException) -> {
                LOGGER.error("Exception occured when executing task", completionException);
                Throwable cause = completionException.getCause();
                if (cause instanceof WorkerUnreachableException) {
                    WorkerUnreachableException wue = (WorkerUnreachableException) cause;
                    try {
                        workerManager.unregisterWorker(step.getWorkerGroupId(), wue.getWorkerId());
                    } catch (WorkerFamilyNotFoundException | WorkerNotFoundException | InterruptedException e1) {
                        LOGGER.error("Exception while unrigster worker " + wue.getWorkerId(), cause);
                    }
                } else if (cause instanceof WorkerExecutorException) {
                    LOGGER.error(cause);
                } else {
                    // FIXME: 7/31/17 treat this kind of exception
                }

                // TODO: 6/29/17 Should re-execute the step when worker becomme up
                return new ItemStatus(step.getStepName()).increment(StatusCode.FATAL);
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
}
