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
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.io.CharStreams;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.processing.common.exception.HandlerNotFoundException;
import fr.gouv.vitam.processing.common.exception.ProcessingBadRequestException;
import fr.gouv.vitam.processing.common.exception.WorkerAlreadyExistsException;
import fr.gouv.vitam.processing.common.exception.WorkerFamilyNotFoundException;
import fr.gouv.vitam.processing.common.exception.WorkerNotFoundException;
import fr.gouv.vitam.processing.common.model.DistributionKind;
import fr.gouv.vitam.processing.common.model.EngineResponse;
import fr.gouv.vitam.processing.common.model.ProcessResponse;
import fr.gouv.vitam.processing.common.model.StatusCode;
import fr.gouv.vitam.processing.common.model.Step;
import fr.gouv.vitam.processing.common.model.WorkerBean;
import fr.gouv.vitam.processing.common.parameter.DefaultWorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.distributor.api.ProcessDistributor;
import fr.gouv.vitam.processing.engine.core.monitoring.ProcessMonitoringImpl;
import fr.gouv.vitam.worker.client.WorkerClientConfiguration;
import fr.gouv.vitam.worker.client.WorkerClientFactory;
import fr.gouv.vitam.worker.client.WorkerClientFactory.WorkerClientType;
import fr.gouv.vitam.worker.common.DescriptionStep;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

/**
 * The Process Distributor call the workers and intercept the response for manage a post actions step
 *
 * <pre>
 * TODO : 
 * - handle listing of items through a limited arraylist (memory) and through iterative (async) listing from
 * Workspace 
 * - handle result in FATAL mode from one distributed item to stop the distribution in FATAL mode (do not
 * continue) 
 * - try to handle distribution on 1 or on many as the same loop (so using a default arrayList of 1)
 * - handle error level using order in enum in ProcessResponse.getGlobalProcessStatusCode instead of manually comparing: 
 *  {@code
 *    for (final EngineResponse response : responses) {
 *       tempStatusCode = response.getStatus();
 *       if (statusCode.ordinal() > tempStatusCode.ordinal()) {
 *           statusCode = tempStatusCode;
 *       }
 *      if (statusCode.ordinal() > StatusCode.KO.ordinal()) {
 *           break;
 *       }
 *     }
 *   }
 * </pre>
 */
public class ProcessDistributorImpl implements ProcessDistributor {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProcessDistributorImpl.class);
    private static final String ELAPSED_TIME_MESSAGE = "Total elapsed time in execution of method distribute is :";
    private static final String EXEC = "Exec";
    private static final String XML_EXTENSION = ".xml";
    private static final String EXCEPTION_MESSAGE =
        "runtime exceptions thrown by the Process distributor during runnig...";
    private static final String ELEMENT_UNITS = "Units";
    private static final String INGEST_LEVEL_STACK = "ingestLevelStack.json";

    private static final Map<String, NavigableMap<String, WorkerBean>> WORKERS_LIST = new HashMap<>();
    private final List<String> availableWorkers = new ArrayList<>();

    /**
     * Constructor with parameter worker
     *
     * @param worker worker implementation
     */
    ProcessDistributorImpl(WorkerBean workerBean, String workerId, String familyId) {
        ParametersChecker.checkParameter("workerBean is a mandatory parameter", workerBean);
        ParametersChecker.checkParameter("workerId is a mandatory parameter", workerId);
        ParametersChecker.checkParameter("familyId is a mandatory parameter", familyId);
        workerBean.setWorkerId(workerId);
        NavigableMap<String, WorkerBean> workers = new TreeMap<>();
        workers.put(workerId, workerBean);
        WORKERS_LIST.put(familyId, workers);
        availableWorkers.add(workerId);
    }

    /**
     * Method used for test purpose
     * 
     */
    Map<String, NavigableMap<String, WorkerBean>> getWorkersList() {
        return WORKERS_LIST;
    }

    /**
     * Empty constructor
     */
    public ProcessDistributorImpl() {}

    // FIXME : make this method (distribute()) more generic
    @Override
    public List<EngineResponse> distribute(WorkerParameters workParams, Step step, String workflowId) {
        ParametersChecker.checkParameter("WorkParams is a mandatory parameter", workParams);
        ParametersChecker.checkParameter("Step is a mandatory parameter", step);
        ParametersChecker.checkParameter("workflowId is a mandatory parameter", workflowId);
        final long time = System.currentTimeMillis();
        final EngineResponse errorResponse = new ProcessResponse();
        errorResponse.setStatus(StatusCode.FATAL);
        final List<EngineResponse> responses = new ArrayList<>();
        String processId = workParams.getProcessId();
        String uniqueStepId = workParams.getStepUniqId();
        try {

            if (step.getDistribution().getKind().equals(DistributionKind.LIST)) {
                final WorkspaceClient workspaceClient =
                    WorkspaceClientFactory.create(workParams.getUrlWorkspace());
                List<URI> objectsList = null;

                // Test regarding Unit to be indexed
                if (step.getDistribution().getElement().equals(ELEMENT_UNITS)) {
                    objectsList = new ArrayList<URI>();

                    // get the file to retrieve the GUID
                    InputStream levelFile =
                        workspaceClient.getObject(workParams.getContainerName(), EXEC + "/" + INGEST_LEVEL_STACK);
                    final String inputStreamString = CharStreams.toString(new InputStreamReader(levelFile, "UTF-8"));
                    final JsonNode levelFileJson = JsonHandler.getFromString(inputStreamString);
                    Iterator<Entry<String, JsonNode>> iteratorlLevelFile = levelFileJson.fields();
                    while (iteratorlLevelFile.hasNext()) {
                        Entry<String, JsonNode> guidFieldList = iteratorlLevelFile.next();
                        JsonNode guid = guidFieldList.getValue();
                        if (guid != null && guid.size() > 0) {
                            for (final JsonNode _idGuid : guid) {
                                // include the GUID in the new URI
                                objectsList.add(new URI(_idGuid.asText() + XML_EXTENSION));
                            }
                        }
                    }
                } else {
                    //
                    objectsList = workspaceClient
                        .getListUriDigitalObjectFromFolder(workParams.getContainerName(),
                            step.getDistribution().getElement());
                }

                // Iterate over Objects List
                if (objectsList == null || objectsList.isEmpty()) {
                    responses.add(errorResponse);
                } else {
                    // update the number of element to process
                    ProcessMonitoringImpl.getInstance().updateStep(processId, uniqueStepId, objectsList.size(), false);
                    for (final URI objectUri : objectsList) {
                        if (availableWorkers.isEmpty()) {
                            LOGGER.debug(errorResponse.getStatus().toString());
                            responses.add(errorResponse);
                            break;
                        } else {
                            // Load configuration
                            // TODO : management of parallel distribution and availability
                            loadWorkerClient(WORKERS_LIST.get("defaultFamily").firstEntry().getValue());
                            // run step
                            workParams.setObjectName(objectUri.getPath());
                            responses.addAll(
                                WorkerClientFactory.getInstance().getWorkerClient().submitStep("requestId",
                                    new DescriptionStep(step, (DefaultWorkerParameters) workParams)));
                            // update the number of processed element
                            ProcessMonitoringImpl.getInstance().updateStep(processId, uniqueStepId, 0, true);
                        }
                    }
                }
            } else {
                // update the number of element to process
                ProcessMonitoringImpl.getInstance().updateStep(processId, uniqueStepId, 1, false);
                if (availableWorkers.isEmpty()) {                    
                    LOGGER.debug(errorResponse.getStatus().toString());
                    responses.add(errorResponse);
                } else {
                    // TODO : management of parallel distribution and availability
                    loadWorkerClient(WORKERS_LIST.get("defaultFamily").firstEntry().getValue());
                    workParams.setObjectName(step.getDistribution().getElement());
                    responses.addAll(
                        WorkerClientFactory.getInstance().getWorkerClient().submitStep("requestId",
                            new DescriptionStep(step, (DefaultWorkerParameters) workParams)));
                    // update the number of processed element
                    ProcessMonitoringImpl.getInstance().updateStep(processId, uniqueStepId, 0, true);
                }
            }

        } catch (final IllegalArgumentException e) {
            responses.add(errorResponse);
            LOGGER.error("Illegal Argument Exception", e);
        } catch (final HandlerNotFoundException e) {
            responses.add(errorResponse);
            LOGGER.error("Handler Not Found Exception", e);

        } catch (final Exception e) {
            responses.add(errorResponse);
            LOGGER.error(EXCEPTION_MESSAGE, e);
        } finally {
            LOGGER.debug(ELAPSED_TIME_MESSAGE + (System.currentTimeMillis() - time) / 1000 + "s /stepName :" +
                getSafetyStepName(step) + "Status: " + responses.toString() + "/workflowId :" + workflowId);
        }

        return responses;
    }

    private void loadWorkerClient(WorkerBean workerBean) {
        WorkerClientConfiguration workerClientConfiguration =
            new WorkerClientConfiguration(workerBean.getConfiguration().getServerHost(),
                workerBean.getConfiguration().getServerPort());
        WorkerClientFactory.setConfiguration(WorkerClientType.WORKER, workerClientConfiguration);
    }


    private String getSafetyStepName(Step step) {

        if (step == null || step.getStepName() == null) {
            return "";
        }
        return step.getStepName();
    }

    @Override
    public void registerWorker(String familyId, String workerId, String workerInformation)
        throws WorkerAlreadyExistsException, ProcessingBadRequestException {
        LOGGER.debug("Worker Information " + familyId + " " + workerId + " " + workerInformation);
        WorkerBean worker = null;
        try {
            worker = JsonHandler.getFromString(workerInformation, WorkerBean.class);
            worker.setWorkerId(workerId);
        } catch (InvalidParseOperationException e) {
            LOGGER.error("Worker Information incorrect", e);
            throw new ProcessingBadRequestException("Worker description is incorrect");
        }
        if (WORKERS_LIST.get(familyId) != null) {
            LOGGER.debug("Family known");
            NavigableMap<String, WorkerBean> familyWorkers = WORKERS_LIST.get(familyId);
            if (familyWorkers.get(workerId) != null) {
                LOGGER.error("Worker already registered");
                throw new WorkerAlreadyExistsException("Worker already registered");
            } else {
                familyWorkers.put(workerId, worker);
                WORKERS_LIST.put(familyId, familyWorkers);
                availableWorkers.add(workerId);
            }
        } else {
            LOGGER.debug("Family unknown");
            NavigableMap<String, WorkerBean> familyWorkers = new TreeMap<String, WorkerBean>();

            familyWorkers.put(workerId, worker);
            WORKERS_LIST.put(familyId, familyWorkers);
            availableWorkers.add(workerId);
        }
    }

    @Override
    public void unregisterWorker(String familyId, String workerId)
        throws WorkerNotFoundException, WorkerFamilyNotFoundException {
        NavigableMap<String, WorkerBean> familyWorkers = WORKERS_LIST.get(familyId);
        if (familyWorkers != null) {
            if (familyWorkers.get(workerId) != null) {
                familyWorkers.remove(workerId);
                WORKERS_LIST.put(familyId, familyWorkers);
            } else {
                LOGGER.error("Worker does not exist in this family");
                throw new WorkerNotFoundException("Worker does not exist in this family");
            }
        } else {
            LOGGER.error("Worker Family does not exist");
            throw new WorkerFamilyNotFoundException("Worker Family does not exist");
        }
    }
}
