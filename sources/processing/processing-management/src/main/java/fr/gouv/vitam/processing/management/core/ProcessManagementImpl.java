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
package fr.gouv.vitam.processing.management.core;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.StateNotAllowedException;
import fr.gouv.vitam.common.exception.WorkflowNotFoundException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.ProcessPause;
import fr.gouv.vitam.common.model.ProcessQuery;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.ProcessDetail;
import fr.gouv.vitam.common.model.processing.WorkFlow;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.processing.common.automation.IEventsState;
import fr.gouv.vitam.processing.common.config.ServerConfiguration;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.exception.ProcessingStorageWorkspaceException;
import fr.gouv.vitam.processing.common.metrics.ProcessWorkflowMetricsCollector;
import fr.gouv.vitam.processing.common.model.ProcessStep;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.processing.common.parameter.WorkerParameterName;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.processing.data.core.ProcessDataAccess;
import fr.gouv.vitam.processing.data.core.ProcessDataAccessImpl;
import fr.gouv.vitam.processing.data.core.management.ProcessDataManagement;
import fr.gouv.vitam.processing.distributor.api.ProcessDistributor;
import fr.gouv.vitam.processing.engine.api.ProcessEngine;
import fr.gouv.vitam.processing.engine.core.ProcessEngineFactory;
import fr.gouv.vitam.processing.engine.core.operation.OperationContextException;
import fr.gouv.vitam.processing.engine.core.operation.OperationContextMonitor;
import fr.gouv.vitam.processing.management.api.ProcessManagement;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * ProcessManagementImpl implementation of ProcessManagement API
 */
public class ProcessManagementImpl implements ProcessManagement {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProcessManagementImpl.class);
    // FIXME WHY SO STATIC ?
    private static final Map<String, IEventsState> PROCESS_MONITORS = new ConcurrentHashMap<>();
    private final OperationContextMonitor operationContextMonitor;

    private ServerConfiguration config;
    private final ProcessDataAccess processDataAccess;
    private final Map<String, WorkFlow> poolWorkflow;
    private ProcessDistributor processDistributor;
    private ProcessDataManagement workspaceProcessDataManagement;
    private final Map<Integer, List<LogbookTypeProcess>> pausedProcessesByTenant;
    private final List<Integer> pausedTenants;
    private final List<LogbookTypeProcess> pausedTypeProcesses;
    private Boolean pauseAll;

    public ProcessManagementImpl(ServerConfiguration config, ProcessDistributor processDistributor)
        throws ProcessingStorageWorkspaceException {
        this(
            config,
            processDistributor,
            ProcessDataAccessImpl.getInstance(),
            processDistributor.getProcessDataManagement(),
            new OperationContextMonitor()
        );
    }

    @VisibleForTesting
    public ProcessManagementImpl(
        ServerConfiguration config,
        ProcessDistributor processDistributor,
        ProcessDataAccess processDataAccess,
        ProcessDataManagement processDataManagement,
        OperationContextMonitor operationContextMonitor
    ) throws ProcessingStorageWorkspaceException {
        this.operationContextMonitor = operationContextMonitor;

        ParametersChecker.checkParameter("Server config cannot be null", config);
        this.config = config;
        this.processDataAccess = processDataAccess;
        poolWorkflow = new ConcurrentHashMap<>();
        this.pausedProcessesByTenant = new ConcurrentHashMap<>();
        this.pausedTenants = new ArrayList<>();
        this.pausedTypeProcesses = new ArrayList<>();
        pauseAll = Boolean.FALSE;
        this.processDistributor = processDistributor;
        this.workspaceProcessDataManagement = processDataManagement;

        ProcessWorkflowMetricsCollector.getInstance().initialize(this.processDataAccess.getWorkFlowList());

        new ProcessWorkFlowsCleaner(this, TimeUnit.HOURS);
        new WorkflowsLoader(this);

        // load all workflow
        ProcessPopulator.loadWorkflow(poolWorkflow);

        loadProcessFromWorkSpace(config.getUrlMetadata(), config.getUrlWorkspace());
    }

    @Override
    public void startProcess() {
        /**
         * Do not start process in test mode Before test you should add SystemPropertyUtil.set("vitam.test.junit",
         * "true");
         */
        if (VitamConfiguration.isIntegrationTest()) {
            return;
        }
        for (String operationId : PROCESS_MONITORS.keySet()) {
            final IEventsState stateMachine = PROCESS_MONITORS.get(operationId);

            if (!stateMachine.isRecover()) {
                continue;
            }

            try {
                VitamThreadUtils.getVitamSession().setTenantId(stateMachine.getTenant());
                VitamThreadUtils.getVitamSession().setRequestId(operationId);
                VitamThreadUtils.getVitamSession().setContextId(stateMachine.getContextId());

                final WorkerParameters workerParameters = WorkerParametersFactory.newWorkerParameters()
                    .setMap(stateMachine.getWorkflowParameters())
                    .setUrlMetadata(config.getUrlMetadata())
                    .setUrlWorkspace(config.getUrlWorkspace())
                    .setLogbookTypeProcess(stateMachine.getLogbookTypeProcess())
                    .setContainerName(operationId)
                    .setRequestId(operationId)
                    .setWorkflowIdentifier(stateMachine.getWorkflowId());

                if (stateMachine.isStepByStep()) {
                    stateMachine.next(workerParameters);
                } else {
                    stateMachine.resume(workerParameters);
                }
            } catch (StateNotAllowedException | ProcessingException e) {
                LOGGER.error("Error while pause the processWorkflow : " + operationId, e);
            }
        }
    }

    /**
     * This method is used to properly stop all ProcessWorkflow Call stop on all running process workflow and propagate
     * this stop to the distributor The distributor should : - Unregister all worker and complete all opened connections
     * to the workers - Stop properly waiting tasks - Save index of element to be used in the next start
     */
    @Override
    public void stopProcess() {
        final CountDownLatch countDownLatch = new CountDownLatch(PROCESS_MONITORS.size());
        for (String operationId : PROCESS_MONITORS.keySet()) {
            final IEventsState stateMachine = PROCESS_MONITORS.get(operationId);

            if (stateMachine.isDone()) {
                countDownLatch.countDown();
                continue;
            }

            VitamThreadPoolExecutor.getDefaultExecutor()
                .execute(() -> {
                    try {
                        stateMachine.shutdown();
                    } finally {
                        countDownLatch.countDown();
                    }
                });
        }
        try {
            countDownLatch.await(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error(e);
        }

        PROCESS_MONITORS.clear();
    }

    @Override
    public ProcessWorkflow init(WorkerParameters workerParameters, String workflowId) throws ProcessingException {
        // check data container and folder
        workspaceProcessDataManagement.createProcessContainer();
        workspaceProcessDataManagement.createFolder(VitamConfiguration.getWorkspaceWorkflowsFolder());

        Optional<WorkFlow> workFlow = poolWorkflow
            .values()
            .stream()
            .filter(w -> StringUtils.equals(w.getId(), workflowId))
            .findFirst();
        if (!workFlow.isPresent()) {
            throw new ProcessingException("Workflow (" + workflowId + ") not found");
        }

        String workflowIdentifier = workFlow.get().getIdentifier();
        final ProcessWorkflow processWorkflow = processDataAccess.initProcessWorkflow(
            workFlow.get(),
            workerParameters.getContainerName()
        );
        processWorkflow.setWorkflowId(workflowIdentifier);

        for (WorkerParameterName workerParameterName : workerParameters.getMapParameters().keySet()) {
            processWorkflow
                .getParameters()
                .put(workerParameterName.name(), workerParameters.getParameterValue(workerParameterName));
        }

        try {
            workspaceProcessDataManagement.persistProcessWorkflow(
                VitamConfiguration.getWorkspaceWorkflowsFolder(),
                processWorkflow
            );
        } catch (InvalidParseOperationException e) {
            throw new ProcessingException(e);
        }

        workerParameters.setLogbookTypeProcess(processWorkflow.getLogbookTypeProcess());
        workerParameters.setWorkflowIdentifier(workflowIdentifier);

        final ProcessEngine processEngine = ProcessEngineFactory.get().create(workerParameters, processDistributor);
        final StateMachine stateMachine = StateMachineFactory.get().create(processWorkflow, processEngine);
        processEngine.setStateMachineCallback(stateMachine);

        PROCESS_MONITORS.put(workerParameters.getContainerName(), stateMachine);

        // Try to backup operation context in the offer.
        try {
            switch (processWorkflow.getLogbookTypeProcess()) {
                case INGEST:
                case MASTERDATA:
                case TRACEABILITY:
                case INGEST_TEST:
                case AUDIT:
                case DATA_MIGRATION:
                    LOGGER.debug(
                        "Backup operation context. No operation context for the process type " +
                        processWorkflow.getLogbookTypeProcess()
                    );
                    break;
                default:
                    operationContextMonitor.backup(
                        VitamConfiguration.getDefaultStrategy(),
                        workerParameters.getContainerName(),
                        processWorkflow.getLogbookTypeProcess()
                    );
            }
        } catch (OperationContextException e) {
            LOGGER.error("Unable to backup operation context from the workspace", e);
        }

        return processWorkflow;
    }

    @Override
    public ItemStatus next(WorkerParameters workerParameters, Integer tenantId)
        throws ProcessingException, StateNotAllowedException {
        return execute(workerParameters.getContainerName(), tenantId, workerParameters, null, ProcessAction.NEXT);
    }

    @Override
    public ItemStatus resume(WorkerParameters workerParameters, Integer tenantId, boolean useForcedPause)
        throws ProcessingException, StateNotAllowedException {
        return execute(
            workerParameters.getContainerName(),
            tenantId,
            workerParameters,
            useForcedPause,
            ProcessAction.RESUME
        );
    }

    @Override
    public ItemStatus replay(WorkerParameters workerParameters, Integer tenantId)
        throws ProcessingException, StateNotAllowedException {
        return execute(workerParameters.getContainerName(), tenantId, workerParameters, null, ProcessAction.REPLAY);
    }

    @Override
    public ItemStatus pause(String operationId, Integer tenantId) throws ProcessingException, StateNotAllowedException {
        return execute(operationId, tenantId, null, null, ProcessAction.PAUSE);
    }

    @Override
    public ItemStatus cancel(String operationId, Integer tenantId)
        throws WorkflowNotFoundException, ProcessingException, StateNotAllowedException {
        return execute(operationId, tenantId, null, null, null);
    }

    public ItemStatus execute(
        String operationId,
        Integer tenantId,
        WorkerParameters workerParameters,
        Boolean useForcedPause,
        ProcessAction action
    ) throws ProcessingException, StateNotAllowedException {
        final IEventsState stateMachine = PROCESS_MONITORS.get(operationId);

        if (null == stateMachine) {
            throw new StateNotAllowedException("Operation " + operationId + "does not exists or already completed");
        }

        final ProcessWorkflow processWorkflow = findOneProcessWorkflow(operationId, tenantId);

        if (null == action) {
            stateMachine.cancel();

            return new ItemStatus(operationId)
                .increment(processWorkflow.getStatus())
                .setGlobalState(processWorkflow.getState())
                .setLogbookTypeProcess(processWorkflow.getLogbookTypeProcess().toString());
        }

        switch (action) {
            case NEXT:
                stateMachine.next(workerParameters);
                break;
            case RESUME:
                if (Boolean.TRUE.equals(useForcedPause) && isPauseForced(processWorkflow, tenantId)) {
                    return next(workerParameters, tenantId);
                }

                stateMachine.resume(workerParameters);
                break;
            case REPLAY:
                stateMachine.replay(workerParameters);
                break;
            case PAUSE:
                stateMachine.pause();
                break;
            default:
                throw new IllegalArgumentException("Action not managed :" + action);
        }

        return new ItemStatus(operationId)
            .increment(processWorkflow.getStatus())
            .setGlobalState(processWorkflow.getState())
            .setLogbookTypeProcess(processWorkflow.getLogbookTypeProcess().toString());
    }

    /**
     * Check if the processWorkflow.logbookTypeProcess or the tenantId are paused
     *
     * @param processWorkflow
     * @param tenantId
     * @return
     */
    private boolean isPauseForced(ProcessWorkflow processWorkflow, Integer tenantId) {
        //Check the pauseAll param, if true all the processes for all the tenants are paused
        if (Boolean.TRUE.equals(pauseAll)) {
            return true;
        }

        //Check the list of paused tenant
        if (pausedTenants.contains(tenantId)) {
            return true;
        }

        //Get the logbookTypeProcess of the workflow
        LogbookTypeProcess logbookTypeProcess = processWorkflow.getLogbookTypeProcess();

        //Check the list of paused processes
        if (pausedTypeProcesses.contains(logbookTypeProcess)) {
            return true;
        }

        //Check the list of paused process for the given tenant
        List<LogbookTypeProcess> pausedWorklowsByTenant = pausedProcessesByTenant.get(tenantId);
        if (pausedWorklowsByTenant != null && pausedWorklowsByTenant.contains(logbookTypeProcess)) {
            return true;
        }

        return false;
    }

    @Override
    public void forcePause(ProcessPause pause) throws ProcessingException {
        String type = pause.getType();
        Integer tenantId = pause.getTenant();
        Boolean pauseAll = pause.getPauseAll();

        if (type == null && tenantId == null && pauseAll == null) {
            throw new ProcessingException("Type, tenant and pauseAll param cannot all be null");
        }

        this.pauseAll = pause.getPauseAll();

        LogbookTypeProcess processType = null;
        if (type != null && !type.isEmpty()) {
            try {
                processType = LogbookTypeProcess.getLogbookTypeProcess(type);
            } catch (IllegalArgumentException e) {
                throw new ProcessingException("Type " + type + " is not a valid process type");
            }
        }

        if (processType != null && tenantId != null) {
            //Get the list of paused process for the given tenant
            List<LogbookTypeProcess> pausedWorklowsByTenant = pausedProcessesByTenant.get(tenantId);
            if (pausedWorklowsByTenant == null) {
                pausedWorklowsByTenant = new ArrayList<LogbookTypeProcess>();
            }
            if (!pausedWorklowsByTenant.contains(processType)) {
                pausedWorklowsByTenant.add(tenantId, processType);
            }
            pausedProcessesByTenant.put(tenantId, pausedWorklowsByTenant);
        } else if (processType == null && tenantId != null) {
            if (!pausedTenants.contains(tenantId)) {
                pausedTenants.add(tenantId);
            }
        } else if (processType != null && tenantId == null) {
            if (!pausedTypeProcesses.contains(processType)) {
                pausedTypeProcesses.add(processType);
            }
        }
    }

    @Override
    public void removeForcePause(ProcessPause pause) throws ProcessingException {
        String type = pause.getType();
        Integer tenantId = pause.getTenant();
        Boolean pauseAll = pause.getPauseAll();

        if (type == null && tenantId == null && pauseAll == null) {
            throw new ProcessingException("Type, tenant and pauseAll param cannot all be null");
        }
        //Remove the pauseAll
        if (Boolean.FALSE.equals(pauseAll)) {
            this.pauseAll = pauseAll;
        }

        LogbookTypeProcess processType = null;
        if (type != null && !type.isEmpty()) {
            try {
                processType = LogbookTypeProcess.getLogbookTypeProcess(type);
            } catch (IllegalArgumentException e) {
                throw new ProcessingException("Type " + type + "is not a valid process type");
            }
        }

        if (processType != null && tenantId != null) {
            //Get the list of paused process for the given tenant
            List<LogbookTypeProcess> pausedWorklowsByTenant = pausedProcessesByTenant.get(tenantId);
            if (pausedWorklowsByTenant != null && !pausedWorklowsByTenant.isEmpty()) {
                pausedWorklowsByTenant.remove(processType);
            }
            //remove the tenant from the pausedTenants list and from the pausedProcessesByTenant map
        } else if (processType == null && tenantId != null) {
            if (pausedTenants.contains(tenantId)) {
                pausedTenants.remove(tenantId);
            }
            if (pausedProcessesByTenant.containsKey(tenantId)) {
                pausedProcessesByTenant.remove(tenantId);
            }
            //remove the processType from the paused pausedTypeProcesses list and from the pausedProcessesByTenant map
        } else if (processType != null && tenantId == null) {
            if (pausedTypeProcesses.contains(processType)) {
                pausedTypeProcesses.remove(processType);
            }
            LogbookTypeProcess pr = processType;
            pausedProcessesByTenant.forEach((id, v) -> {
                if (v != null && !v.isEmpty()) {
                    v.remove(pr);
                }
            });
        }
    }

    @Override
    public void close() {
        // Nothing to do
    }

    @Override
    public List<ProcessWorkflow> findAllProcessWorkflow(Integer tenantId) {
        return processDataAccess.findAllProcessWorkflow(tenantId);
    }

    @Override
    public ProcessWorkflow findOneProcessWorkflow(String operationId, Integer tenantId) {
        return processDataAccess.findOneProcessWorkflow(operationId, tenantId);
    }

    @Override
    public Map<String, WorkFlow> getWorkflowDefinitions() {
        return poolWorkflow;
    }

    @Override
    public void reloadWorkflowDefinitions() {
        Integer period = this.getConfiguration().getWorkflowRefreshPeriod();
        long fromDate = Instant.now().minus(period, ChronoUnit.HOURS).toEpochMilli();

        ProcessPopulator.reloadWorkflow(poolWorkflow, fromDate);
    }

    public Map<Integer, Map<String, ProcessWorkflow>> getWorkFlowList() {
        return processDataAccess.getWorkFlowList();
    }

    @Override
    public Map<String, IEventsState> getProcessMonitorList() {
        return PROCESS_MONITORS;
    }

    private Map<String, IEventsState> loadProcessFromWorkSpace(String urlMetadata, String urlWorkspace)
        throws ProcessingStorageWorkspaceException {
        if (!PROCESS_MONITORS.isEmpty()) {
            return PROCESS_MONITORS;
        }

        Map<String, ProcessWorkflow> map = workspaceProcessDataManagement.getProcessWorkflowFor(
            null,
            VitamConfiguration.getWorkspaceWorkflowsFolder()
        );

        // Nothing to load
        if (map == null) {
            return PROCESS_MONITORS;
        }

        for (String operationId : map.keySet()) {
            ProcessWorkflow processWorkflow = map.get(operationId);
            if (processWorkflow.getState().equals(ProcessState.PAUSE)) {
                // Create StateMachine & ProcessEngine
                WorkerParameters workerParameters = WorkerParametersFactory.newWorkerParameters()
                    .setMap(processWorkflow.getParameters()) // Start with inject original process workflow parameters
                    .setUrlMetadata(urlMetadata)
                    .setUrlWorkspace(urlWorkspace)
                    .setLogbookTypeProcess(processWorkflow.getLogbookTypeProcess())
                    .setContainerName(operationId)
                    .setWorkflowIdentifier(processWorkflow.getWorkflowId());

                final ProcessEngine processEngine = ProcessEngineFactory.get()
                    .create(workerParameters, this.processDistributor);
                final StateMachine stateMachine = StateMachineFactory.get().create(processWorkflow, processEngine);
                processEngine.setStateMachineCallback(stateMachine);

                PROCESS_MONITORS.put(workerParameters.getContainerName(), stateMachine);
            }

            this.processDataAccess.addToWorkflowList(processWorkflow);
        }
        return PROCESS_MONITORS;
    }

    public List<ProcessDetail> getFilteredProcess(ProcessQuery query, Integer tenantId) {
        List<ProcessWorkflow> listWorkflow = this.findAllProcessWorkflow(tenantId);
        listWorkflow.sort((a, b) -> b.getProcessDate().compareTo(a.getProcessDate()));

        List<ProcessDetail> results = new ArrayList<>();

        for (ProcessWorkflow processWorkflow : listWorkflow) {
            ProcessDetail workflow = new ProcessDetail();
            getNextAndPreviousSteps(processWorkflow, workflow);
            if (query.getId() != null && !query.getId().equals(processWorkflow.getOperationId())) {
                continue;
            }
            if (
                query.getStates() != null &&
                !query.getStates().isEmpty() &&
                !query.getStates().contains(processWorkflow.getState().name())
            ) {
                continue;
            }
            if (
                query.getStatuses() != null &&
                !query.getStatuses().isEmpty() &&
                !query.getStatuses().contains(processWorkflow.getStatus().name())
            ) {
                continue;
            }
            if (
                query.getWorkflows() != null &&
                !query.getWorkflows().isEmpty() &&
                !query.getWorkflows().contains(processWorkflow.getWorkflowId())
            ) {
                continue;
            }
            if (query.getListSteps() != null && !query.getListSteps().isEmpty()) {
                if (!isContainsStep(query.getListSteps(), workflow)) {
                    continue;
                }
            }
            if (
                query.getListProcessTypes() != null &&
                !query.getListProcessTypes().isEmpty() &&
                !query.getListProcessTypes().contains(processWorkflow.getLogbookTypeProcess().toString())
            ) {
                continue;
            }
            if (query.getStartDateMin() != null && query.getStartDateMax() != null) {
                if (!isStartDateIn(query.getStartDateMin(), query.getStartDateMax(), processWorkflow)) {
                    continue;
                }
            }
            workflow.setOperationId(processWorkflow.getOperationId());
            workflow.setProcessType(processWorkflow.getLogbookTypeProcess().toString());
            workflow.setStepByStep(processWorkflow.isStepByStep());
            workflow.setGlobalState(processWorkflow.getState().name());
            workflow.setStepStatus(processWorkflow.getStatus().name());
            workflow.setProcessDate(processWorkflow.getProcessDate());
            results.add(workflow);
        }
        return results;
    }

    private boolean isContainsStep(List<String> stepsName, ProcessDetail workflow) {
        String previous = workflow.getPreviousStep();
        return previous != null && !previous.isEmpty() && stepsName.contains(previous);
    }

    private boolean isStartDateIn(String startDateMin, String startDateMax, ProcessWorkflow processWorkflow) {
        // ugly ! can we have time here (on javascript date picker) ?
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String date = processWorkflow.getProcessDate();
        LocalDate ldt = LocalDateTime.parse(date).toLocalDate();
        LocalDate startDateTimeMin = LocalDate.parse(startDateMin, formatter);
        LocalDate startDateTimeMax = LocalDate.parse(startDateMax, formatter);
        return (
            (ldt.isBefore(startDateTimeMax) || ldt.isEqual(startDateTimeMax)) &&
            (ldt.isAfter(startDateTimeMin) || ldt.isEqual(startDateTimeMin))
        );
    }

    // TODO: 03/04/2020 double check if this method return the correct result
    private void getNextAndPreviousSteps(ProcessWorkflow processWorkflow, ProcessDetail workflow) {
        String previousStep = "";
        String nextStep = "";
        String temporaryPreviousTask = "";
        boolean currentStepFound = false;

        Iterator<ProcessStep> pwIterator = processWorkflow.getSteps().iterator();
        while (pwIterator.hasNext() && !currentStepFound) {
            final ProcessStep processStep = pwIterator.next();

            switch (processWorkflow.getState()) {
                case PAUSE:
                case RUNNING:
                    if (processStep.getStepStatusCode() == StatusCode.STARTED) {
                        previousStep = processStep.getStepName();
                        nextStep = pwIterator.hasNext() ? pwIterator.next().getStepName() : "";
                        workflow.setStepStatus("STARTED");
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
                    if (
                        processStep.getStepStatusCode() == StatusCode.KO ||
                        processStep.getStepStatusCode() == StatusCode.STARTED
                    ) {
                        previousStep = processStep.getStepName();
                        workflow.setStepStatus(StatusCode.KO.toString());
                        currentStepFound = true;
                    } else {
                        if (processStep.getStepStatusCode() == StatusCode.UNKNOWN) {
                            previousStep = temporaryPreviousTask;
                            workflow.setStepStatus(StatusCode.KO.toString());
                            currentStepFound = true;
                        }
                    }
                    break;
                default:
                    break;
            }
            temporaryPreviousTask = processStep.getStepName();

            workflow.setPreviousStep(previousStep);
            workflow.setNextStep(nextStep);
        }
    }

    @Override
    public ServerConfiguration getConfiguration() {
        return config;
    }
}
