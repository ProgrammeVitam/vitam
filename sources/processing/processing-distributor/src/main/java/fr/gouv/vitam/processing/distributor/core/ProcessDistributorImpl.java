/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 *******************************************************************************/
package fr.gouv.vitam.processing.distributor.core;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.processing.common.exception.HandlerNotFoundException;
import fr.gouv.vitam.processing.common.exception.ProcessingBadRequestException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.DistributionKind;
import fr.gouv.vitam.processing.common.model.Step;
import fr.gouv.vitam.processing.common.parameter.DefaultWorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParameterName;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.distributor.api.Callbackable;
import fr.gouv.vitam.processing.distributor.api.ProcessDistributor;
import fr.gouv.vitam.processing.engine.core.monitoring.ProcessMonitoringImpl;
import fr.gouv.vitam.processing.model.WorkerAsyncRequest;
import fr.gouv.vitam.processing.model.WorkerAsyncResponse;
import fr.gouv.vitam.worker.common.DescriptionStep;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

/**
 * The Process Distributor call the workers and intercept the response for manage a post actions step
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
public class ProcessDistributorImpl implements ProcessDistributor, Callbackable<WorkerAsyncResponse> {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProcessDistributorImpl.class);
    private static final String UNITS_LEVEL = "UnitsLevel";
    private static final String XML_EXTENSION = ".xml";
    private static final String EXCEPTION_MESSAGE =
        "runtime exceptions thrown by the Process distributor during runnig...";
    private static final String ELEMENT_UNITS = "Units";
    private static final String INGEST_LEVEL_STACK = "ingestLevelStack.json";
    private static final String OBJECTS_LIST_EMPTY = "OBJECTS_LIST_EMPTY";


    /**
     * Temporary method for distribution supporting multi-list
     * 
     * @param workParams
     * @param step
     * @param workflowId
     * @return the final step status
     */
    @Override
    public ItemStatus distribute(WorkerParameters workParams, Step step, String workflowId) {
        ParametersChecker.checkParameter("WorkParams is a mandatory parameter", workParams);
        ParametersChecker.checkParameter("Step is a mandatory parameter", step);
        ParametersChecker.checkParameter("workflowId is a mandatory parameter", workflowId);
        final String processId = workParams.getProcessId();
        final String uniqueStepId = workParams.getStepUniqId();
        final Set<WorkerAsyncRequest> currentRunningObjectsInStep = ConcurrentHashMap.newKeySet();
        step.setStepResponses(new ItemStatus(step.getStepName()));
        Semaphore waitingStepAllAsyncRequest = new Semaphore(1);
        try {
            // update workParams
            workParams.putParameterValue(WorkerParameterName.workflowStatusKo,
                ProcessMonitoringImpl.getInstance().getFinalWorkflowStatus(processId).name());
            List<String> objectsList = new ArrayList<>();
            if (step.getDistribution().getKind().equals(DistributionKind.LIST)) {
                try (final WorkspaceClient workspaceClient = WorkspaceClientFactory.getInstance().getClient()) {
                    // Test regarding Unit to be indexed
                    if (ELEMENT_UNITS.equals(step.getDistribution().getElement())) {
                        // get the file to retrieve the GUID
                        final InputStream levelFile =
                            (InputStream) workspaceClient.getObject(workParams.getContainerName(),
                                UNITS_LEVEL + "/" + INGEST_LEVEL_STACK).getEntity();
                        final JsonNode levelFileJson = JsonHandler.getFromInputStream(levelFile);
                        final Iterator<Entry<String, JsonNode>> iteratorlLevelFile = levelFileJson.fields();
                        while (iteratorlLevelFile.hasNext()) {
                            final Entry<String, JsonNode> guidFieldList = iteratorlLevelFile.next();
                            final JsonNode guid = guidFieldList.getValue();
                            if (guid != null && guid.size() > 0) {
                                for (final JsonNode _idGuid : guid) {
                                    // include the GUID in the new URI
                                    objectsList.add(_idGuid.asText() + XML_EXTENSION);
                                }
                                distributeOnList(workParams, step, processId, uniqueStepId,
                                    currentRunningObjectsInStep, objectsList,waitingStepAllAsyncRequest);
                                objectsList.clear();
                            }
                        }
                    } else {
                        // List from Storage
                        List<URI> objectsListUri = workspaceClient
                            .getListUriDigitalObjectFromFolder(workParams.getContainerName(),
                                step.getDistribution().getElement());
                        for (URI uri : objectsListUri) {
                            objectsList.add(uri.getPath());
                        }
                        // Iterate over Objects List
                        distributeOnList(workParams, step, processId, uniqueStepId, currentRunningObjectsInStep,
                            objectsList,waitingStepAllAsyncRequest);
                    }
                }
            } else {
                // update the number of element to process
                objectsList.add(step.getDistribution().getElement());
                distributeOnList(workParams, step, processId, uniqueStepId, currentRunningObjectsInStep,
                    objectsList,waitingStepAllAsyncRequest);
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
        } finally {
            waitingStepAllAsyncRequest.release();
        }
        return step.getStepResponses();
    }

    /**
     * @param workParams
     * @param step
     * @param processId
     * @param uniqueStepId
     * @param currentRunningObjectsInStep
     * @param objectsList
     * @throws ProcessingException
     * @throws InterruptedException
     */
    private void distributeOnList(WorkerParameters workParams, Step step, final String processId,
        final String uniqueStepId, final Set<WorkerAsyncRequest> currentRunningObjectsInStep, List<String> objectsList,Semaphore waitingStepAllAsyncRequest)
        throws ProcessingException {
        if (objectsList == null || objectsList.isEmpty()) {
            step.getStepResponses().setItemsStatus(OBJECTS_LIST_EMPTY,
                getItemStatus(OBJECTS_LIST_EMPTY, StatusCode.WARNING));
            return;
        }
        // update the number of element to process
        ProcessMonitoringImpl.getInstance().updateStep(processId, uniqueStepId, objectsList.size(), false);
        // Initial acquire
        try {
            waitingStepAllAsyncRequest.acquire();
            for (final String objectUri : objectsList) {
                workParams.setObjectName(objectUri);
                // blocking call to submit a new Job
                if (!submitNewJob(workParams, step, currentRunningObjectsInStep,waitingStepAllAsyncRequest)) {
                    break;
                }
            }
        } catch (InterruptedException e) { // NOSONAR ignore since cannot block here
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        } finally {
            // Now waiting for all submitted jobs to finish
            waitEndOfStep(currentRunningObjectsInStep,waitingStepAllAsyncRequest);
        }
    }

    /**
     * Submit a new job to step to be executed for one item to WorkerManager
     * 
     * @param workParams
     * @param step
     * @param currentRunningObjectsInStep
     * @return True if the submission is ok without issue, else False to stop the distribution process
     * @throws ProcessingBadRequestException
     */
    private final boolean submitNewJob(WorkerParameters workParams, Step step,
        Set<WorkerAsyncRequest> currentRunningObjectsInStep,Semaphore waitingStepAllAsyncRequest)
        throws ProcessingBadRequestException {
        // Need to do a new INstance of WorkParams as its contains the ObjectName which is
        // different for each instance
        // The step object MUST NOT BE copied (deep or shallow) as it contains the state of the
        // State (ex: aggregate compositeStatus)
        WorkerAsyncRequest workerAsyncRequest = new WorkerAsyncRequest(
            new DescriptionStep(step, ((DefaultWorkerParameters) workParams).newInstance()),
            this, currentRunningObjectsInStep, step.getWorkerGroupId(), waitingStepAllAsyncRequest,VitamThreadUtils.getVitamSession());
        currentRunningObjectsInStep.add(workerAsyncRequest);
        
        try {
            // This call is blocking on queue if full
            WorkerManager.submitJob(workerAsyncRequest);
            if (Thread.interrupted()) {
                // To check if the ProcessDistributor was interrupt in between, implying a break of the Process
                throw new InterruptedException("ProcessDistributor thread has been interrupted");
            }
        } catch (InterruptedException e) { // NOSONAR already taken into account
            // As a Fatal or KO and Blocking was raised by a Worker, finalize current jobs
            Set<WorkerAsyncRequest> tempSet = new HashSet<>();
            // First remove all Running Jobs
            for (WorkerAsyncRequest war : currentRunningObjectsInStep) {
                // Remove from the Running JobsSet if the job was still in the BlockingQueue (not
                // currently consumed by the worker)
                if (WorkerManager.removeJobs(war)) {
                    tempSet.add(war);
                }
            }
            // Finalize the waiting for finalization job set
            currentRunningObjectsInStep.removeAll(tempSet);
            // Leave the loop
            return false;
        }
        return true;
    }

    /**
     * Add response to the current step (use for async)
     * 
     * @param workerAsyncResponse
     */
    @Override
    public synchronized void callbackResponse(WorkerAsyncResponse workerAsyncResponse) {
        ItemStatus actionsResponse = workerAsyncResponse.getCompositeItemStatus();
        Step step = workerAsyncResponse.getWorkerAsyncRequest().getDescriptionStep().getStep();
        WorkerParameters workParams = workerAsyncResponse.getWorkerAsyncRequest().getDescriptionStep().getWorkParams();
        // First, set the result
        if (actionsResponse != null && step.getStepResponses() != null) {
            step.getStepResponses().setItemsStatus(actionsResponse);
        }
        // Then remove this task from the submitted job set
        workerAsyncResponse.getWorkerAsyncRequest().getCurrentRunningObjectsInStep()
            .remove(workerAsyncResponse.getWorkerAsyncRequest());
        // If there is an error, we stop the distribution of the step
        // Note: should it be the before last action ? (just before notify)
        if (step.getStepResponses().getGlobalStatus().isGreaterOrEqualToFatal()) {
            workerAsyncResponse.getWorkerAsyncRequest().getCallerThread().interrupt();
        }
        // Now notify the Distributor if it is waiting on the Running Job set to be empty
        if (workerAsyncResponse.getWorkerAsyncRequest().getCurrentRunningObjectsInStep().isEmpty()) {
            try {
                ProcessMonitoringImpl.getInstance().updateStep(workParams.getProcessId(), workParams.getStepUniqId(), 0,
                    true);
            } catch (ProcessingException |RuntimeException e ) {
                if (step.getStepResponses() != null) {
                    step.getStepResponses().increment(StatusCode.FATAL);
                }
                LOGGER.error("Should never happen", e);
            }
            // Final release
            if (workerAsyncResponse.getWorkerAsyncRequest().getWaitingStepAllAsyncRequest() != null) {
                workerAsyncResponse.getWorkerAsyncRequest().getWaitingStepAllAsyncRequest().release();
            }
        }
    }

    private ItemStatus getItemStatus(String label, StatusCode statusCode) {
        return new ItemStatus(label).increment(statusCode);
    }

    /**
     * Waits the end of the Step (join of all the async worker task)
     * 
     * @param currentRunningObjectsInStep
     */
    private void waitEndOfStep(Set<WorkerAsyncRequest> currentRunningObjectsInStep,Semaphore waitingStepAllAsyncRequest) {
        // Note: While is necessary since it can be interrupted by following unachieved tasks
        while (!currentRunningObjectsInStep.isEmpty()) {
            Thread.yield();
            // Final acquire
            try {
                waitingStepAllAsyncRequest.tryAcquire(VitamConfiguration.getWaitingDelay(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) { // NOSONAR : ignore exception
                // Empty
            }
        }
        // For next step in list of list
        waitingStepAllAsyncRequest.release();
    }
    
    @Override
    public void close() {
        // Nothing
    }
}
