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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import fr.gouv.vitam.common.ParametersChecker;
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
import fr.gouv.vitam.processing.worker.api.Worker;
import fr.gouv.vitam.processing.worker.core.WorkerImpl;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

/**
 * The Process Distributor call the workers {@link Worker}and intercept the response for manage a post actions step
 *
 */
public class ProcessDistributorImpl implements ProcessDistributor {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProcessDistributorImpl.class);
    private static final String ELAPSED_TIME_MESSAGE = "Total elapsed time in execution of method distribute is :";

    private static final String EXCEPTION_MESSAGE =
        "runtime exceptions thrown by the Process distributor during runnig...";

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
        try {

            if (step.getDistribution().getKind().equals(DistributionKind.LIST)) {
                final WorkspaceClient workspaceClient =
                    new WorkspaceClientFactory().create(workParams.getServerConfiguration().getUrlWorkspace());
                final List<URI> objectsList = workspaceClient
                    .getListUriDigitalObjectFromFolder(workParams.getContainerName(), step.getDistribution().getElement());
                if (objectsList == null || objectsList.isEmpty()) {
                    responses.add(errorResponse);
                } else {
                    for (final URI objectUri : objectsList) {
                        if (availableWorkers.isEmpty()) {
                            LOGGER.info(errorResponse.getStatus().toString());
                            responses.add(errorResponse);
                            break;
                        } else {
                            // TODO distribution Management
                            responses.addAll(workers.get(0).run(workParams.setObjectName(objectUri.getPath()), step));
                        }
                    }
                }
            } else {
                if (availableWorkers.isEmpty()) {
                    LOGGER.info(errorResponse.getStatus().toString());
                    responses.add(errorResponse);
                } else {
                    responses.addAll(
                        workers.get(0).run(workParams.setObjectName(step.getDistribution().getElement()), step));
                }
            }


        } catch (final IllegalArgumentException e) {
            responses.add(errorResponse);
            LOGGER.error(e.getMessage());
        } catch (final HandlerNotFoundException e) {
            responses.add(errorResponse);
            LOGGER.error(e.getMessage());

        } catch (final Exception e) {
            responses.add(errorResponse);
            LOGGER.error(EXCEPTION_MESSAGE, e);
        } finally {
            LOGGER.info(ELAPSED_TIME_MESSAGE + (System.currentTimeMillis() - time) / 1000 + "s /stepName :" +
                getSaftyStepName(step) + "Status: " + responses.toString() + "/workflowId :" + workflowId);
        }

        return responses;
    }


    private String getSaftyStepName(Step step) {

        if (step == null || step.getStepName() == null) {
            return "";
        }

        return step.getStepName();
    }
}
