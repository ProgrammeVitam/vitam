/*******************************************************************************
 * This file is part of Vitam Project.
 *
 * Copyright Vitam (2012, 2016)
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL license as circulated
 * by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL license and that you
 * accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.processing.distributor.core;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.io.CharStreams;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.processing.common.exception.HandlerNotFoundException;
import fr.gouv.vitam.processing.common.model.DistributionKind;
import fr.gouv.vitam.processing.common.model.EngineResponse;
import fr.gouv.vitam.processing.common.model.ProcessResponse;
import fr.gouv.vitam.processing.common.model.StatusCode;
import fr.gouv.vitam.processing.common.model.Step;
import fr.gouv.vitam.processing.common.model.WorkParams;
import fr.gouv.vitam.processing.distributor.api.ProcessDistributor;
import fr.gouv.vitam.processing.engine.core.monitoring.ProcessMonitoringImpl;
import fr.gouv.vitam.processing.worker.api.Worker;
import fr.gouv.vitam.processing.worker.core.WorkerImpl;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

/**
 * The Process Distributor call the workers {@link Worker}and intercept the response for manage a post actions step
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

    private final List<Worker> workers = new ArrayList<Worker>();
    private final List<String> availableWorkers = new ArrayList<String>();

    /**
     * Constructor with parameter workerImpl
     *
     * @param workerImpl {@link WorkerImpl} worker implementation
     */
    protected ProcessDistributorImpl(WorkerImpl workerImpl) {
        ParametersChecker.checkParameter("workerImpl is a mandatory parameter", workerImpl);
        final Worker worker1 = workerImpl;
        workers.add(worker1);
        availableWorkers.add(worker1.getWorkerId());
    }

    /**
     * Empty constructor
     */
    protected ProcessDistributorImpl() {
        final Worker worker1 = new WorkerImpl();
        workers.add(worker1);
        availableWorkers.add(worker1.getWorkerId());
    }

    @Override
    public List<EngineResponse> distribute(WorkParams workParams, Step step, String workflowId) {
        ParametersChecker.checkParameter("WorkParams is a mandatory parameter", workParams);
        ParametersChecker.checkParameter("Step is a mandatory parameter", step);
        ParametersChecker.checkParameter("workflowId is a mandatory parameter", workflowId);
        final long time = System.currentTimeMillis();
        final EngineResponse errorResponse = new ProcessResponse();
        errorResponse.setStatus(StatusCode.FATAL);
        final List<EngineResponse> responses = new ArrayList<>();
        String processId = (String) workParams.getAdditionalProperties().get(WorkParams.PROCESS_ID);
        String uniqueStepId = (String) workParams.getAdditionalProperties().get(WorkParams.STEP_ID);
        try {

            if (step.getDistribution().getKind().equals(DistributionKind.LIST)) {
                final WorkspaceClient workspaceClient =
                    WorkspaceClientFactory.create(workParams.getServerConfiguration().getUrlWorkspace());
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
                            LOGGER.info(errorResponse.getStatus().toString());
                            responses.add(errorResponse);
                            break;
                        } else {
                            // TODO distribution Management
                            responses.addAll(workers.get(0).run(workParams.setObjectName(objectUri.getPath()), step));
                            // update the number of processed element
                            ProcessMonitoringImpl.getInstance().updateStep(processId, uniqueStepId, 0, true);
                        }
                    }
                }
            } else {
                // update the number of element to process
                ProcessMonitoringImpl.getInstance().updateStep(processId, uniqueStepId, 1, false);
                if (availableWorkers.isEmpty()) {
                    LOGGER.info(errorResponse.getStatus().toString());
                    responses.add(errorResponse);
                } else {
                    responses.addAll(
                        workers.get(0).run(workParams.setObjectName(step.getDistribution().getElement()), step));
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
            LOGGER.info(ELAPSED_TIME_MESSAGE + (System.currentTimeMillis() - time) / 1000 + "s /stepName :" +
                getSafetyStepName(step) + "Status: " + responses.toString() + "/workflowId :" + workflowId);
        }

        return responses;
    }


    private String getSafetyStepName(Step step) {

        if (step == null || step.getStepName() == null) {
            return "";
        }

        return step.getStepName();
    }
}
