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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.StateNotAllowedException;
import fr.gouv.vitam.common.exception.WorkflowNotFoundException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessQuery;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.processing.common.automation.IEventsState;
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
import fr.gouv.vitam.processing.distributor.api.ProcessDistributor;
import fr.gouv.vitam.processing.distributor.api.ProcessDistributor;
import fr.gouv.vitam.processing.engine.api.ProcessEngine;
import fr.gouv.vitam.processing.engine.core.ProcessEngineFactory;
import fr.gouv.vitam.processing.engine.core.monitoring.ProcessMonitoring;
import fr.gouv.vitam.processing.engine.core.monitoring.ProcessMonitoringImpl;
import fr.gouv.vitam.processing.management.api.ProcessManagement;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ProcessManagementImpl implementation of ProcessManagement API
 */
public class ProcessManagementImpl implements ProcessManagement {


    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProcessManagementImpl.class);

    private static final String WORKFLOW_NOT_FOUND_MESSAGE = "Workflow doesn't exist";
    private static final Map<String, IEventsState> PROCESS_MONITORS = new ConcurrentHashMap<>();
    private static final String PROCESS_ID_FIELD = "operation_id";
    private static final String PROCESS_TYPE_FIELD = "processType";
    private static final String STEP_BY_STEP_FIELD = "stepByStep";
    private static final String GLOBAL_EXECUTION_STATE_FIELD = "globalState";
    private static final String PROCESS_DATE_FIELD = "processDate";
    private static final String PREVIOUS_STEP = "previousStep";
    private static final String NEXT_STEP = "nextStep";
    private static final String STEP_EXECUTION_STATUS_FIELD = "stepStatus";



    private ServerConfiguration config;
    private final ProcessDataAccess processData;
    private final Map<String, WorkFlow> poolWorkflow;
    private ProcessDistributor processDistributor;

    private ProcessMonitoring processMonitoring;

    /**
     * constructor of ProcessManagementImpl
     *
     * @param config configuration of process engine server
     * @param processDistributor
     * @throws ProcessingStorageWorkspaceException thrown when error occurred on loading paused process
     */
    public ProcessManagementImpl(ServerConfiguration config,
        ProcessDistributor processDistributor) throws ProcessingStorageWorkspaceException {

        ParametersChecker.checkParameter("Server config cannot be null", config);
        this.config = config;
        processData = ProcessDataAccessImpl.getInstance();
        poolWorkflow = new ConcurrentHashMap<>();
        this.processDistributor = processDistributor;
        new ProcessWorkFlowsCleaner(this, TimeUnit.HOURS);
        try {
            populateWorkflow("DefaultFilingSchemeWorkflow");
            populateWorkflow("DefaultHoldingSchemeWorkflow");
            populateWorkflow("DefaultIngestBlankTestWorkflow");
            populateWorkflow("DefaultIngestWorkflow");
            populateWorkflow("DefaultCheckTraceability");
        } catch (final WorkflowNotFoundException e) {
            LOGGER.error(WORKFLOW_NOT_FOUND_MESSAGE, e);
        }

        loadProcessFromWorkSpace(config.getUrlMetadata(), config.getUrlWorkspace(), this.processDistributor);

        processMonitoring = ProcessMonitoringImpl.getInstance();
    }

    /**
     * FOR TEST PURPOSE ONLY (mock processMonitoring)
     * @param config
     * @param processMonitoring
     * @throws ProcessingStorageWorkspaceException
     */
    public ProcessManagementImpl(ServerConfiguration config, ProcessMonitoring processMonitoring) throws
        ProcessingStorageWorkspaceException {
        ParametersChecker.checkParameter("Server config cannot be null", config);
        this.config = config;
        processData = ProcessDataAccessImpl.getInstance();
        poolWorkflows = new ConcurrentHashMap<>();

        try {
            populateWorkflow("DefaultFilingSchemeWorkflow");
            populateWorkflow("DefaultHoldingSchemeWorkflow");
            populateWorkflow("DefaultIngestBlankTestWorkflow");
            populateWorkflow("DefaultIngestWorkflow");
            populateWorkflow("DefaultCheckTraceability");
        } catch (final WorkflowNotFoundException e) {
            LOGGER.error(WORKFLOW_NOT_FOUND_MESSAGE, e);
        }

        loadProcessFromWorkSpace(config.getUrlMetadata(), config.getUrlWorkspace());

        this.processMonitoring = processMonitoring;
    }

    @Override
    public ProcessWorkflow init(WorkerParameters workerParameters, String workflowId,
        LogbookTypeProcess logbookTypeProcess, Integer tenantId)
        throws ProcessingException {

        // check data container and folder
        ProcessDataManagement dataManagement = WorkspaceProcessDataManagement.getInstance();
        dataManagement.createProcessContainer();
        dataManagement.createFolder(String.valueOf(ServerIdentity.getInstance().getServerId()));

        final ProcessWorkflow processWorkflow;
        if (ParametersChecker.isNotEmpty(workflowId)) {
            processWorkflow = processData
                .initProcessWorkflow(poolWorkflow.get(workflowId), workerParameters.getContainerName(),
                    logbookTypeProcess, tenantId);
        } else {
            processWorkflow = processData
                .initProcessWorkflow(null, workerParameters.getContainerName(), LogbookTypeProcess.INGEST, tenantId);
        }

        processWorkflow.setWorkflowId(workflowId);

        try {
            // TODO: create json workflow file, but immediately updated so keep this part ?)
            dataManagement.persistProcessWorkflow(String.valueOf(ServerIdentity.getInstance().getServerId()),
                workerParameters.getContainerName(), processWorkflow);
        } catch (InvalidParseOperationException e) {
            throw new ProcessingException(e);
        }


        workerParameters.setLogbookTypeProcess(logbookTypeProcess);
        WorkspaceClientFactory.changeMode(config.getUrlWorkspace());

        final ProcessEngine processEngine = ProcessEngineFactory.get().create(workerParameters, processDistributor);
        final StateMachine stateMachine = StateMachineFactory.get().create(processWorkflow, processEngine);
        processEngine.setCallback(stateMachine);

        PROCESS_MONITORS.put(workerParameters.getContainerName(), stateMachine);

        return processWorkflow;
    }


    /**
     * setWorkflow : populate a workflow to the pool
     *
     * @param workflowId as String
     * @throws WorkflowNotFoundException throw when workflow not found
     */
    public void populateWorkflow(String workflowId) throws WorkflowNotFoundException {
        ParametersChecker.checkParameter("workflowId is a mandatory parameter", workflowId);
        poolWorkflow.put(workflowId, ProcessPopulator.populate(workflowId));
    }


    @Override
    public ItemStatus next(WorkerParameters workerParameters, Integer tenantId) throws ProcessingException,
        StateNotAllowedException {

        final String operationId = workerParameters.getContainerName();

        final IEventsState stateMachine = PROCESS_MONITORS.get(operationId);

        if (null == stateMachine) {
            throw new ProcessingException(
                "StateMachine not found with id " + operationId + ". Handle INIT before next");
        }

        final ProcessWorkflow processWorkflow = findOneProcessWorkflow(operationId, tenantId);

        stateMachine.next(workerParameters);


        return new ItemStatus(operationId)
            .increment(processWorkflow.getStatus())
            .setGlobalState(processWorkflow.getState())
            .setLogbookTypeProcess(processWorkflow.getLogbookTypeProcess().toString());
    }

    @Override
    public ItemStatus resume(WorkerParameters workerParameters, Integer tenantId)
        throws ProcessingException, StateNotAllowedException {
        final String operationId = workerParameters.getContainerName();

        final IEventsState stateMachine = PROCESS_MONITORS.get(operationId);

        if (null == stateMachine) {
            throw new ProcessingException(
                "StateMachine not found with id " + operationId + ". Handle INIT before next");
        }

        final ProcessWorkflow processWorkflow = findOneProcessWorkflow(operationId, tenantId);

        stateMachine.resume(workerParameters);


        return new ItemStatus(operationId)
            .increment(processWorkflow.getStatus())
            .setGlobalState(processWorkflow.getState())
            .setLogbookTypeProcess(processWorkflow.getLogbookTypeProcess().toString());
    }

    @Override public ItemStatus pause(String operationId, Integer tenantId)
        throws ProcessingException, StateNotAllowedException {

        final IEventsState stateMachine = PROCESS_MONITORS.get(operationId);

        if (null == stateMachine) {
            throw new ProcessingException(
                "StateMachine not found with id " + operationId + ". Handle INIT before next");
        }

        final ProcessWorkflow processWorkflow = findOneProcessWorkflow(operationId, tenantId);

        stateMachine.pause();


        return new ItemStatus(operationId)
            .increment(processWorkflow.getStatus())
            .setGlobalState(processWorkflow.getState())
            .setLogbookTypeProcess(processWorkflow.getLogbookTypeProcess().toString());
    }

    @Override
    public ItemStatus cancel(String operationId, Integer tenantId)
        throws WorkflowNotFoundException, ProcessingException, StateNotAllowedException {


        final IEventsState stateMachine = PROCESS_MONITORS.get(operationId);

        if (null == stateMachine) {
            throw new ProcessingException(
                "StateMachine not found with id " + operationId + ". Handle INIT before next");
        }

        final ProcessWorkflow processWorkflow = findOneProcessWorkflow(operationId, tenantId);

        stateMachine.cancel();


        return new ItemStatus(operationId)
            .increment(processWorkflow.getStatus())
            .setGlobalState(processWorkflow.getState())
            .setLogbookTypeProcess(processWorkflow.getLogbookTypeProcess().toString());
    }

    @Override
    public void close() {
        // Nothing to do
    }

    @Override
    public List<ProcessWorkflow> findAllProcessWorkflow(Integer tenantId) {
        return processData.findAllProcessWorkflow(tenantId);
    }

    @Override
    public ProcessWorkflow findOneProcessWorkflow(String operationId, Integer tenantId) {
        return processData.findOneProcessWorkflow(operationId, tenantId);
    }

    @Override
    public Map<String, WorkFlow> getWorkflowDefinitions() {
        return poolWorkflows;
    }

    public Map<Integer, Map<String, ProcessWorkflow>>  getWorkFlowList() {
        return processData.getWorkFlowList();
    }

    @Override
    public Map<String, IEventsState> getProcessMonitorList() { return PROCESS_MONITORS; }

    private Map<String, IEventsState> loadProcessFromWorkSpace(String urlMetadata, String urlWorkspace) throws ProcessingStorageWorkspaceException {
        if (!PROCESS_MONITORS.isEmpty()) {
            return PROCESS_MONITORS;
        }

        final ProcessDataManagement datamanage = WorkspaceProcessDataManagement.getInstance();
        Map<String, ProcessWorkflow> map = datamanage.getProcessWorkflowFor(null,
            String.valueOf(ServerIdentity.getInstance().getServerId()));

        // Nothing to load
        if (map == null) {
            return PROCESS_MONITORS;
        }

        for (String operationId : map.keySet()) {
            ProcessWorkflow processWorkflow = map.get(operationId);
            if (processWorkflow.getState().equals(ProcessState.PAUSE)) {
                // Create & start ProcessEngine Thread
                WorkerParameters workerParameters =
                    WorkerParametersFactory.newWorkerParameters()
                        .setUrlMetadata(urlMetadata)
                        .setUrlWorkspace(urlWorkspace)
                        .setLogbookTypeProcess(processWorkflow.getLogbookTypeProcess())
                        .setContainerName(operationId);


                final ProcessEngine processEngine = ProcessEngineFactory.get().create(workerParameters, processDistributor);
                final StateMachine stateMachine = StateMachineFactory.get().create(processWorkflow, processEngine);
                processEngine.setCallback(stateMachine);

                PROCESS_MONITORS.put(workerParameters.getContainerName(), stateMachine);

            } else {
                if (StatusCode.UNKNOWN.equals(processWorkflow.getStatus())) {
                    processWorkflow.setStatus(StatusCode.UNKNOWN);
                    processWorkflow.setProcessCompletedDate(LocalDateTime.now());
                    processWorkflow.setState(ProcessState.COMPLETED);
                    try {
                        datamanage.persistProcessWorkflow(String.valueOf(ServerIdentity.getInstance()
                            .getServerId()), operationId, processWorkflow);
                    } catch (InvalidParseOperationException e) {
                        // TODO: just log error is the good solution (here, we set to failed and unknown status on wrong
                        // persisted process) ?
                        LOGGER.error("Cannot set UNKNONW status and FAILED execution status on workflow {}, check " +
                                "processing datas",
                            operationId, e);
                    }
                }
            }
            ProcessDataAccessImpl.getInstance().addToWorkflowList(processWorkflow);
        }
        return PROCESS_MONITORS;
    }

    private static String getNextStepId(List<ProcessStep> steps) {
        for (ProcessStep processStep : steps) {
            // Actually the first step in UNKNOWN status is the next step to start
            // This is an ugly method to retrieve it, but there are no more informations
            if (processStep.getElementProcessed() == 0
                && processStep.getElementToProcess() == 0
                && processStep.getStepStatusCode().equals(StatusCode.UNKNOWN)) {
                return processStep.getId();
            }
        }
        return null;
    }


    public List<JsonNode> getFilteredProcess(ProcessQuery query, Integer tenantId) {
        List<ProcessWorkflow> listWorkflows = processMonitoring.findAllProcessWorkflow(tenantId);
        listWorkflows.sort((a, b) -> b.getProcessDate().compareTo(a.getProcessDate()));

        List<JsonNode> results = new ArrayList<>();

        for (ProcessWorkflow processWorkflow : listWorkflows) {
            ObjectNode workflow = JsonHandler.createObjectNode();
            workflow = getNextAndPreviousSteps(processWorkflow, workflow);
            if (query.getId() != null && !query.getId().equals(processWorkflow.getOperationId())) {
                continue;
            }
            if (query.getStates() != null && !query.getStates().isEmpty() &&
                !query.getStates().contains(processWorkflow.getState().name())) {
                continue;
            }
            if (query.getStatuses() != null && !query.getStatuses().isEmpty() &&
                !query.getStatuses().contains(processWorkflow.getStatus().name())) {
                continue;
            }
            if (query.getWorkflows() != null && !query.getWorkflows().isEmpty() &&
                !query.getWorkflows().contains(processWorkflow.getWorkflowId())) {
                continue;
            }
            if (query.getListSteps() != null && !query.getListSteps().isEmpty()) {
                if (!isContainsStep(query.getListSteps(), workflow)) {
                    continue;
                }
            }
            if (query.getStartDateMin() != null && query.getStartDateMax() != null) {
                if (!isStartDateIn(query.getStartDateMin(), query.getStartDateMax(), processWorkflow)) {
                    continue;
                }
            }
            workflow.put(PROCESS_ID_FIELD, processWorkflow.getOperationId());
            workflow.put(PROCESS_TYPE_FIELD, processWorkflow.getLogbookTypeProcess().toString());
            workflow.put(STEP_BY_STEP_FIELD, processWorkflow.isStepByStep());
            workflow.put(GLOBAL_EXECUTION_STATE_FIELD, processWorkflow.getState().name());
            workflow.put(STEP_EXECUTION_STATUS_FIELD, processWorkflow.getStatus().name());
            workflow.put(PROCESS_DATE_FIELD, LocalDateUtil.getFormattedDate(processWorkflow.getProcessDate()));
            results.add(workflow);
        }
        return results;
    }

    private boolean isContainsStep(List<String> stepsName, ObjectNode workflow) {
        JsonNode previous = workflow.get(PREVIOUS_STEP);
        return previous != null && previous.asText() != null && stepsName.contains(previous.asText());
    }

    private boolean isStartDateIn(String startDateMin, String startDateMax, ProcessWorkflow processWorkflow) {
        // ugly ! can we have time here (on javascript date picker) ?
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        Date date = processWorkflow.getProcessDate();
        LocalDate ldt = LocalDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC).toLocalDate();
        LocalDate startDateTimeMin = LocalDate.parse(startDateMin, formatter);
        LocalDate startDateTimeMax = LocalDate.parse(startDateMax, formatter);
        return ((ldt.isBefore(startDateTimeMax) || ldt.isEqual(startDateTimeMax)) && (ldt.isAfter(startDateTimeMin)
            || ldt.isEqual(startDateTimeMin)));
    }

    // TODO: 5/27/17 refactor the following
    private ObjectNode getNextAndPreviousSteps(ProcessWorkflow processWorkflow, ObjectNode workflow) {
        String previousStep = "";
        String nextStep = "";
        String temporaryPreviousTask = "";
        Boolean currentStepFound = false;

        Iterator<ProcessStep> pwIterator = processWorkflow.getSteps().iterator();
        while (pwIterator.hasNext() && !currentStepFound) {

            final ProcessStep processStep = pwIterator.next();

            switch (processWorkflow.getState()) {
                case PAUSE:
                case RUNNING:
                    if (processStep.getStepStatusCode() == StatusCode.STARTED) {
                        previousStep = processStep.getStepName();
                        nextStep = pwIterator.hasNext() ? pwIterator.next().getStepName() : "";
                        workflow.put(STEP_EXECUTION_STATUS_FIELD, "STARTED");
                        currentStepFound = true;
                    } else {
                        if (processStep.getStepStatusCode() == StatusCode.UNKNOWN) {
                            previousStep = temporaryPreviousTask;
                            nextStep = processStep.getStepName();
                            currentStepFound = true;
                        }
                    }
                    break;
                case COMPLETED:
                    if (processStep.getStepStatusCode() == StatusCode.KO ||
                        processStep.getStepStatusCode() == StatusCode.STARTED) {
                        previousStep = processStep.getStepName();
                        workflow.put(STEP_EXECUTION_STATUS_FIELD, StatusCode.KO.toString());
                        currentStepFound = true;
                    } else {
                        if (processStep.getStepStatusCode() == StatusCode.UNKNOWN) {
                            previousStep = temporaryPreviousTask;
                            workflow.put(STEP_EXECUTION_STATUS_FIELD, StatusCode.KO.toString());
                            currentStepFound = true;
                        }
                    }
                    break;
                default:
                    break;
            }
            temporaryPreviousTask = processStep.getStepName();

            workflow.put(PREVIOUS_STEP, previousStep);
            workflow.put(NEXT_STEP, nextStep);
        }
        return workflow;
    }
    @Override
    public ServerConfiguration getConfiguration() {
        return config;

    }
}
