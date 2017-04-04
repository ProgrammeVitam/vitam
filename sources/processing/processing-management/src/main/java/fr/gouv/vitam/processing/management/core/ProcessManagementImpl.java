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
package fr.gouv.vitam.processing.management.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.WorkflowNotFoundException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.ProcessExecutionStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.server.application.AsyncInputStreamHelper;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.processing.common.config.ServerConfiguration;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.exception.ProcessingStorageWorkspaceException;
import fr.gouv.vitam.processing.common.model.ProcessStep;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.processing.common.model.WorkFlow;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.processing.common.utils.ProcessPopulator;
import fr.gouv.vitam.processing.data.core.ProcessDataAccess;
import fr.gouv.vitam.processing.data.core.ProcessDataAccessImpl;
import fr.gouv.vitam.processing.data.core.management.ProcessDataManagement;
import fr.gouv.vitam.processing.data.core.management.WorkspaceProcessDataManagement;
import fr.gouv.vitam.processing.engine.api.ProcessEngine;
import fr.gouv.vitam.processing.engine.core.ProcessEngineImpl;
import fr.gouv.vitam.processing.engine.core.ProcessEngineImplFactory;
import fr.gouv.vitam.processing.management.api.ProcessManagement;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

/**
 * ProcessManagementImpl implementation of ProcessManagement API
 */
public class ProcessManagementImpl implements ProcessManagement {


    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProcessManagementImpl.class);

    private static final String WORKFLOW_NOT_FOUND_MESSAGE = "Workflow doesn't exist";

    private static final ProcessExecutionStatus UNAUTHORIZED_ACTION = null;

    private static final Map<String, List<Object>> PROCESS_MONITORS = new HashMap<>();

    private static final int PROCESS_ENGINE_INDEX = 0;

    private static final int MONITOR_INDEX = 1;

    private ServerConfiguration serverConfig;
    private final ProcessDataAccess processData;
    private final Map<String, WorkFlow> poolWorkflows;

    /**
     * constructor of ProcessManagementImpl
     *
     * @param serverConfig configuration of process engine server
     * @throws ProcessingStorageWorkspaceException thrown when error occurred on loading paused process
     */
    public ProcessManagementImpl(ServerConfiguration serverConfig) throws ProcessingStorageWorkspaceException {
        ParametersChecker.checkParameter("Server config cannot be null", serverConfig);
        this.serverConfig = serverConfig;
        processData = ProcessDataAccessImpl.getInstance();
        poolWorkflows = new ConcurrentHashMap<>();
        
        try {
            setWorkflow("DefaultFilingSchemeWorkflow");
            setWorkflow("DefaultHoldingSchemeWorkflow");
            setWorkflow("DefaultIngestBlankTestWorkflow");
            setWorkflow("DefaultIngestWorkflow");
            setWorkflow("DefaultCheckTraceability");
        } catch (final WorkflowNotFoundException e) {
            LOGGER.error(WORKFLOW_NOT_FOUND_MESSAGE, e);
        }

        // Init ProcessMonitor for recovery
        loadProcessMonitor();
    }

    /**
     * set the server configuration
     *
     * @param serverConfig configuration of process engine server
     * @return ProcessManagementImpl instance with serverConfig is setted
     */
    public ProcessManagementImpl setServerConfig(ServerConfiguration serverConfig) {
        ParametersChecker.checkParameter("Server config cannot be null", serverConfig);
        this.serverConfig = serverConfig;
        return this;
    }

    /**
     * get the server configuration
     *
     * @return serverConfig of type ServerConfiguration
     */
    public ServerConfiguration getServerConfig() {
        return serverConfig;
    }



    @Override
    public ProcessWorkflow initWorkflow(WorkerParameters workParams, String workflowId,
        LogbookTypeProcess logbookTypeProcess, AsyncResponse asyncResponse, Integer tenantId)
        throws ProcessingException {

        // check data container and folder
        ProcessDataManagement dataManagement = WorkspaceProcessDataManagement.getInstance();
        dataManagement.createProcessContainer();
        dataManagement.createFolder(String.valueOf(ServerIdentity.getInstance().getServerId()));

        ProcessWorkflow createdProcessWorkflow = null;
        if (StringUtils.isNotBlank(workflowId)) {
            createdProcessWorkflow =
                processData.initProcessWorkflow(poolWorkflows.get(workflowId), workParams.getContainerName(),
                    ProcessAction.INIT, logbookTypeProcess, tenantId);
        } else {
            createdProcessWorkflow =
                processData.initProcessWorkflow(null, workParams.getContainerName(), ProcessAction.INIT,
                    LogbookTypeProcess.INGEST, tenantId);
        }

        try {
            // TODO: create json workflow file, but immediately updated so keep this part ?)
            dataManagement.persistProcessWorkflow(String.valueOf(ServerIdentity.getInstance().getServerId()),
                workParams.getContainerName(), createdProcessWorkflow);
        } catch (InvalidParseOperationException e) {
            throw new ProcessingException(e);
        }

        // Create & start ProcessEngine Thread
        Object monitor = new Object();

        workParams.setUrlMetadata(serverConfig.getUrlMetadata());
        workParams.setUrlWorkspace(serverConfig.getUrlWorkspace());
        workParams.setLogbookTypeProcess(logbookTypeProcess);
        WorkspaceClientFactory.changeMode(serverConfig.getUrlWorkspace());

        ProcessEngine processEngineOperation =
            new ProcessEngineImplFactory().create(workParams, monitor, asyncResponse);

        List<Object> operationParams = new ArrayList<>();
        operationParams.add(processEngineOperation);
        operationParams.add(monitor);
        PROCESS_MONITORS.put(workParams.getContainerName(), operationParams);

        VitamThreadPoolExecutor.getDefaultExecutor()
            .execute(processEngineOperation);

        return createdProcessWorkflow;
    }

    /**
     * setWorkflow : populate a workflow to the pool
     *
     * @param workflowId as String
     * @throws WorkflowNotFoundException throw when workflow not found
     */
    public void setWorkflow(String workflowId) throws WorkflowNotFoundException {
        ParametersChecker.checkParameter("workflowId is a mandatory parameter", workflowId);
        poolWorkflows.put(workflowId, ProcessPopulator.populate(workflowId));
    }


    @Override
    public ItemStatus submitWorkflow(WorkerParameters workParams, String workflowId, ProcessAction executionMode,
        AsyncResponse asyncResponse, Integer tenantId) throws ProcessingException {
        String operationId = workParams.getContainerName();

        ProcessWorkflow processWorkflow = processData.getProcessWorkflow(operationId, tenantId);
        if (processWorkflow.getExecutionStatus().ordinal() > ProcessExecutionStatus.PAUSE.ordinal()) {
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse, Response.status(Status.UNAUTHORIZED).build());
            throw new ProcessingException(UNAUTHORIZED_ACTION + processWorkflow.getExecutionStatus().toString());
        }

        Object monitor = PROCESS_MONITORS.get(operationId).get(MONITOR_INDEX);
        ProcessEngineImpl processEngine =
            (ProcessEngineImpl) PROCESS_MONITORS.get(operationId).get(PROCESS_ENGINE_INDEX);
        processEngine.setAsyncResponse(asyncResponse);
        processEngine.setWorkerParameters(workParams);

        // Change execution parameters
        processData.prepareToRelaunch(workParams.getContainerName(), executionMode, tenantId);
        // persist running state
        try {
            WorkspaceProcessDataManagement.getInstance().persistProcessWorkflow(String.valueOf(ServerIdentity.getInstance().getServerId()),
                workParams.getContainerName(), processData.getProcessWorkflow(operationId, tenantId));
        } catch (InvalidParseOperationException e) {
            throw new ProcessingException(e);
        }

        if (ProcessAction.NEXT.equals(executionMode) || ProcessAction.RESUME.equals(executionMode)) {
            // Execute next step or continue to the end : Notify the suspended process engine thread
            synchronized (monitor) {
                monitor.notify();
            }
        }

        return new ItemStatus(operationId).increment(processWorkflow.getGlobalStatusCode())
            .setGlobalExecutionStatus(processWorkflow.getExecutionStatus());
    }

    @Override
    public ItemStatus cancelProcessWorkflow(String operationId, Integer tenantId, AsyncResponse asyncResponse)
        throws WorkflowNotFoundException, ProcessingException {

        try {
            ProcessWorkflow processWorkflow = processData.getProcessWorkflow(operationId, tenantId);
            if (processWorkflow.getExecutionStatus().ordinal() > ProcessExecutionStatus.PAUSE.ordinal()) {
                AsyncInputStreamHelper.asyncResponseResume(asyncResponse,
                    Response.status(Status.UNAUTHORIZED).build());
                throw new ProcessingException(UNAUTHORIZED_ACTION + processWorkflow.getExecutionStatus().toString());
            }

            // Deal with canceling suspended workFlow
            boolean isSuspendedWorkflow =
                processWorkflow.getExecutionStatus().ordinal() == ProcessExecutionStatus.PAUSE.ordinal();
            processWorkflow = processData.cancelProcessWorkflow(operationId, tenantId);
            if (isSuspendedWorkflow) {
                AsyncInputStreamHelper.asyncResponseResume(asyncResponse,
                    Response.status(Status.OK)
                        .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATUS, ProcessExecutionStatus.CANCELLED)
                        .build());

                return new ItemStatus(operationId).increment(processWorkflow.getGlobalStatusCode())
                    .setGlobalExecutionStatus(processWorkflow.getExecutionStatus());
            }

            // Deal with other cases
            Object monitor = PROCESS_MONITORS.get(operationId).get(MONITOR_INDEX);
            ProcessEngineImpl processEngine =
                (ProcessEngineImpl) PROCESS_MONITORS.get(operationId).get(PROCESS_ENGINE_INDEX);
            processEngine.setAsyncResponse(asyncResponse);

            synchronized (monitor) {
                monitor.notify();
            }

            return new ItemStatus(operationId).increment(processWorkflow.getGlobalStatusCode())
                .setGlobalExecutionStatus(processWorkflow.getExecutionStatus());

        } catch (WorkflowNotFoundException e) {
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse, Response.status(Status.NOT_FOUND).build());
            throw e;
        }
    }

    @Override
    public ItemStatus pauseProcessWorkFlow(String operationId, Integer tenantId, AsyncResponse asyncResponse)
        throws ProcessingException {
        ProcessWorkflow processWorkflow = processData.getProcessWorkflow(operationId, tenantId);
        if (processWorkflow.getExecutionStatus().ordinal() >= ProcessExecutionStatus.PAUSE.ordinal()) {
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse, Response.status(Status.UNAUTHORIZED).build());
            throw new ProcessingException(UNAUTHORIZED_ACTION + processWorkflow.getExecutionStatus().toString());
        }

        // Update ProcessEngine Thread
        ProcessEngineImpl processEngine =
            (ProcessEngineImpl) PROCESS_MONITORS.get(operationId).get(PROCESS_ENGINE_INDEX);
        processEngine.setAsyncResponse(asyncResponse);

        processData.updateProcessExecutionStatus(operationId, ProcessExecutionStatus.PAUSE, tenantId);
        return new ItemStatus(operationId).increment(processWorkflow.getGlobalStatusCode())
            .setGlobalExecutionStatus(processWorkflow.getExecutionStatus());
    }

    @Override
    public void close() {
        // Nothing to do
    }

    @Override
    public List<ProcessWorkflow> getAllWorkflowProcess(Integer tenantId) {
        return processData.getAllWorkflowProcess(tenantId);
    }

    @Override
    public ProcessWorkflow getWorkflowProcessById(String operationId, Integer tenantId) {
        return processData.getProcessWorkflow(operationId, tenantId);
    }

    private Map<String, List<Object>> loadProcessMonitor() throws ProcessingStorageWorkspaceException {
        if (!PROCESS_MONITORS.isEmpty()) {
            return PROCESS_MONITORS;
        }
        Map<String, ProcessWorkflow> map = null;

        map = WorkspaceProcessDataManagement.getInstance().getProcessWorkflowFor(null, String.valueOf
            (ServerIdentity.getInstance().getServerId()));

        // Nothing to load
        if (map == null) {
            return PROCESS_MONITORS;
        }

        for (String operationId : map.keySet()) {
            ProcessWorkflow processWorkflow = map.get(operationId);
            if (processWorkflow.getExecutionStatus().equals(ProcessExecutionStatus.PAUSE)) {
                // Create & start ProcessEngine Thread
                Object monitor = new Object();
                WorkerParameters workParams = WorkerParametersFactory.newWorkerParameters();
                workParams.setUrlMetadata(serverConfig.getUrlMetadata());
                workParams.setUrlWorkspace(serverConfig.getUrlWorkspace());
                workParams.setLogbookTypeProcess(processWorkflow.getLogbookTypeProcess());

                workParams.setContainerName(operationId);
                workParams.setCurrentStep(getNextStepId(processWorkflow.getOrderedProcessStep()));

                // asyncResponse to null because actually no instance of
                ProcessEngine processEngineOperation =
                    new ProcessEngineImplFactory().createForRecovery(workParams, monitor);

                List<Object> operationParams = new ArrayList<>();
                operationParams.add(processEngineOperation);
                operationParams.add(monitor);
                PROCESS_MONITORS.put(workParams.getContainerName(), operationParams);

                VitamThreadPoolExecutor.getDefaultExecutor()
                    .execute(processEngineOperation);
            } else {
                processWorkflow.setGlobalStatusCode(StatusCode.UNKNOWN);
                processWorkflow.setExecutionStatus(ProcessExecutionStatus.FAILED);
                try {
                    WorkspaceProcessDataManagement.getInstance().persistProcessWorkflow(String.valueOf(ServerIdentity.getInstance()
                        .getServerId()), operationId, processWorkflow);
                } catch (InvalidParseOperationException e) {
                    // TODO: just log error is the good solution (here, we set to failed and unknown status on wrong
                    // persisted process) ?
                    LOGGER.error("Cannot set UNKNONW status and FAILED execution status on workflow {}, check " +
                            "processing datas",
                        operationId, e);
                }
                processData.addToWorkflowList(processWorkflow);
            }
        }
        return PROCESS_MONITORS;
    }

    private String getNextStepId(Map<String, ProcessStep> orderedSteps) {
        for (ProcessStep processStep : orderedSteps.values()) {
            // Actually the first step in UNKNOWN status is the next step to execute
            // This is an ugly method to retrieve it, but there are no more informations
            if (processStep.getElementProcessed() == 0 && processStep.getElementToProcess() == 0 && processStep
                .getStepStatusCode().equals(StatusCode.UNKNOWN)) {
                return processStep.getId();
            }
        }
        return null;
    }
}
