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
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.alert.AlertService;
import fr.gouv.vitam.common.alert.AlertServiceImpl;
import fr.gouv.vitam.common.exception.StateNotAllowedException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.logging.VitamLogLevel;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.PauseOrCancelAction;
import fr.gouv.vitam.common.model.processing.ProcessBehavior;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.processing.common.automation.IEventsProcessEngine;
import fr.gouv.vitam.processing.common.automation.IEventsState;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.PauseRecover;
import fr.gouv.vitam.processing.common.model.ProcessStep;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.processing.data.core.management.ProcessDataManagement;
import fr.gouv.vitam.processing.data.core.management.WorkspaceProcessDataManagement;
import fr.gouv.vitam.processing.engine.api.ProcessEngine;
import fr.gouv.vitam.processing.engine.core.operation.OperationContextMonitor;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static fr.gouv.vitam.common.model.processing.PauseOrCancelAction.ACTION_CANCEL;
import static fr.gouv.vitam.common.model.processing.PauseOrCancelAction.ACTION_PAUSE;
import static fr.gouv.vitam.common.model.processing.PauseOrCancelAction.ACTION_RECOVER;
import static fr.gouv.vitam.common.model.processing.PauseOrCancelAction.ACTION_REPLAY;
import static fr.gouv.vitam.processing.common.model.PauseRecover.RECOVER_FROM_API_PAUSE;

/**
 * State Machine class implementing the Interface. Dealing with evolution of workflow
 */
public class StateMachine implements IEventsState, IEventsProcessEngine {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(StateMachine.class);

    private static final AlertService alertService = new AlertServiceImpl();

    private int stepIndex = 0;
    private ProcessStep currentStep = null;
    private CompletableFuture<Boolean> waitMonitor;
    private final ProcessWorkflow processWorkflow;

    private final ProcessEngine processEngine;
    private final ProcessDataManagement dataManagement;

    private final WorkspaceClientFactory workspaceClientFactory;

    @VisibleForTesting
    private static final long TIMEOUT = 30;

    public StateMachine(ProcessWorkflow processWorkflow, ProcessEngine processEngine) {
        this(
            processWorkflow,
            processEngine,
            WorkspaceProcessDataManagement.getInstance(),
            WorkspaceClientFactory.getInstance()
        );
    }

    @VisibleForTesting
    public StateMachine(
        ProcessWorkflow processWorkflow,
        ProcessEngine processEngine,
        ProcessDataManagement dataManagement,
        WorkspaceClientFactory workspaceClientFactory
    ) {
        ParametersChecker.checkParameter(
            "Parameters (processWorkflow, processEngine) are required",
            processWorkflow,
            processEngine
        );

        this.workspaceClientFactory = workspaceClientFactory;
        this.processWorkflow = processWorkflow;
        this.processEngine = processEngine;
        this.dataManagement = dataManagement;

        if (CollectionUtils.isEmpty(this.processWorkflow.getSteps())) {
            throw new IllegalArgumentException("At least one step is needed");
        }
        initStepIndex();
    }

    /**
     * This is important after a restart of the server
     * Find the last executed step to initialize the current step
     */
    private void initStepIndex() {
        int stepSize = processWorkflow.getSteps().size();

        for (ProcessStep step : processWorkflow.getSteps()) {
            final PauseOrCancelAction pauseOrCancelAction = step.getPauseOrCancelAction();
            final StatusCode stepStatus = step.getStepStatusCode();

            // If step action cancel needed then goto the final step
            if (ACTION_CANCEL.equals(pauseOrCancelAction)) {
                stepIndex = stepSize - 1;
                break;
            }

            // Old workflow can be ACTION_COMPLETE with STARTED status code
            // Step can be FATAL
            if (StatusCode.STARTED.equals(stepStatus) || StatusCode.FATAL.equals(stepStatus)) {
                step.setPauseOrCancelAction(ACTION_RECOVER);
                break;
            }

            if (step.isBlockingKO()) {
                stepIndex = stepSize - 1;
                break;
            }

            if (PauseOrCancelAction.ACTION_COMPLETE.equals(pauseOrCancelAction) && stepIndex + 1 < stepSize) {
                stepIndex++;
                continue;
            }

            if (ACTION_PAUSE.equals(pauseOrCancelAction) || step.getStepStatusCode().isGreaterOrEqualToStarted()) {
                step.setPauseOrCancelAction(ACTION_RECOVER);
            }

            break;
        }

        currentStep = this.processWorkflow.getSteps().get(stepIndex);
    }

    @Override
    public synchronized void resume(WorkerParameters workerParameters)
        throws StateNotAllowedException, ProcessingException {
        this.processWorkflow.getState().eval(ProcessState.RUNNING);
        this.doRunning(workerParameters, ProcessState.RUNNING);
    }

    @Override
    public synchronized void next(WorkerParameters workerParameters)
        throws StateNotAllowedException, ProcessingException {
        this.processWorkflow.getState().eval(ProcessState.RUNNING);
        this.doRunning(workerParameters, ProcessState.PAUSE);
    }

    @Override
    public synchronized void replay(WorkerParameters workerParameters)
        throws StateNotAllowedException, ProcessingException {
        this.processWorkflow.getState().eval(ProcessState.RUNNING);
        this.doReplay(workerParameters);
    }

    @Override
    public synchronized void pause() throws StateNotAllowedException {
        this.processWorkflow.getState().eval(ProcessState.PAUSE);
        this.doPause(RECOVER_FROM_API_PAUSE);
    }

    @Override
    public void shutdown() {
        this.doPause(PauseRecover.RECOVER_FROM_SERVER_PAUSE);
    }

    @Override
    public synchronized void cancel() throws StateNotAllowedException {
        this.processWorkflow.getState().eval(ProcessState.COMPLETED);
        doCancel();
    }

    @Override
    public boolean isDone() {
        return (
            ProcessState.COMPLETED.equals(processWorkflow.getState()) ||
            ProcessState.PAUSE.equals(processWorkflow.getState())
        );
    }

    @Override
    public int getTenant() {
        return processWorkflow.getTenantId();
    }

    @Override
    public String getWorkflowId() {
        return processWorkflow.getWorkflowId();
    }

    @Override
    public String getContextId() {
        return this.processWorkflow.getContextId();
    }

    @Override
    public Map<String, String> getWorkflowParameters() {
        return this.processWorkflow.getParameters();
    }

    @Override
    public boolean isStepByStep() {
        return this.processWorkflow.isStepByStep();
    }

    @Override
    public LogbookTypeProcess getLogbookTypeProcess() {
        return this.processWorkflow.getLogbookTypeProcess();
    }

    /**
     * Change state of the process to pause Can be called only from running state If last step then change state to
     * completed
     *
     * @param pauseRecover if RECOVER_FROM_SERVER_PAUSE then wait until pause is done
     */
    protected void doPause(PauseRecover pauseRecover) {
        if (isPause() || isCompleted()) {
            return;
        }

        // RECOVER_FROM_SERVER_PAUSE occurs when the server stops
        if (PauseRecover.RECOVER_FROM_SERVER_PAUSE.equals(pauseRecover)) {
            // Create wait monitor, to wait running operations (PAUSE or COMPLETED)
            this.waitMonitor = new CompletableFuture<>();
        }

        if (!isLastStep()) {
            if (processWorkflow.getTargetState() != ProcessState.COMPLETED) {
                this.processWorkflow.setTargetState(ProcessState.PAUSE);
            }

            // Tell distributor to immediately pause current step if the current step is not ActionComplete or ActionCancel
            switch (this.currentStep.getPauseOrCancelAction()) {
                case ACTION_COMPLETE:
                case ACTION_CANCEL:
                    break;
                default:
                    // Update the variable reference to be visible automatically by the distributor
                    this.currentStep.setPauseOrCancelAction(ACTION_PAUSE);
            }

            this.processWorkflow.setPauseRecover(pauseRecover);
        }

        tryPersistProcessWorkflow();

        if (PauseRecover.RECOVER_FROM_SERVER_PAUSE.equals(pauseRecover)) {
            try {
                this.waitMonitor.get(TIMEOUT, TimeUnit.SECONDS);
            } catch (Exception e) {
                String msg =
                    "[StateMachine] The operation " +
                    processWorkflow.getOperationId() +
                    " is not completed properly. To be safe, all workers must be restarted before starting processing > ";
                LOGGER.error(msg, e);
                alertService.createAlert(VitamLogLevel.ERROR, msg);
            }
        }
    }

    /**
     * Change the state of the process to completed Can be called only from running or pause state If running state, the
     * next step will be completed
     */
    protected void doCancel() {
        var currentStatus = this.processWorkflow.getStatus();
        this.processWorkflow.setTargetState(ProcessState.COMPLETED);
        // Needed if retry after fatal in the final step
        this.processWorkflow.setTargetStatus(StatusCode.KO);
        this.processWorkflow.setStatus(StatusCode.KO);

        tryPersistProcessWorkflow();
        //FIXME: We can't force cancel on final step as we have, for any case, execute the final step
        if (isRunning()) {
            if (!isLastStep()) {
                // Tell distributor to immediately cancel current step
                this.currentStep.setPauseOrCancelAction(ACTION_CANCEL);
            }
            //If last step => just wait step execution ending

        } else if (isPause()) {
            // Workflow init but not started
            // When error occurs on externals (check anti-virus, not allowed name check, ...)
            if (StatusCode.UNKNOWN.equals(currentStatus)) {
                this.processWorkflow.setState(ProcessState.COMPLETED);
                finalizeOperation();
            } else {
                // Execute the final step
                this.processWorkflow.setState(ProcessState.RUNNING);
                final WorkerParameters workerParameters = WorkerParametersFactory.newWorkerParameters()
                    .setMap(processWorkflow.getParameters());
                this.executeFinalStep(workerParameters);
            }
        }
    }

    /**
     * Change state of the process to running Can be called only from pause state
     *
     * @param workerParameters the parameters to be passed to the distributor
     * @param targetState if true, run ony the next step
     * @throws ProcessingException
     */
    protected void doRunning(WorkerParameters workerParameters, ProcessState targetState) throws ProcessingException {
        if (null == targetState) {
            throw new ProcessingException("The targetState is required");
        }

        // Double check
        if (isRunning()) {
            throw new ProcessingException("doRunning not allowed on already running state");
        }

        this.processWorkflow.setState(ProcessState.RUNNING);
        this.processWorkflow.setTargetState(targetState);
        this.processWorkflow.setStepByStep(ProcessState.PAUSE.equals(targetState));

        findAndExecuteNextStep(workerParameters, false);
    }

    /**
     * Change state of the process to running Can be called only from pause state
     *
     * @param workerParameters the parameters to be passed to the distributor
     * @throws ProcessingException
     */
    protected void doReplay(WorkerParameters workerParameters) throws ProcessingException {
        // Double check
        if (isRunning()) {
            throw new ProcessingException("doRunning not allowed on already running state");
        }

        this.processWorkflow.setState(ProcessState.RUNNING);
        this.processWorkflow.setTargetState(ProcessState.PAUSE);
        this.processWorkflow.setStepByStep(true);

        findAndExecuteNextStep(workerParameters, true);
    }

    /**
     * @param workerParameters
     * @param replayCurrentStep
     */
    protected void findAndExecuteNextStep(WorkerParameters workerParameters, boolean replayCurrentStep) {
        if (replayCurrentStep && stepIndex > 0) {
            stepIndex--;
        }

        // If, for any reason, the current step is blocking KO or ACTION_CANCEL then goto the final step
        boolean cancelRequired = ACTION_CANCEL.equals(currentStep.getPauseOrCancelAction());
        boolean mustExecuteTheFinalStep =
            (!replayCurrentStep && currentStep.isBlockingKO() && !isLastStep()) || cancelRequired;

        if (mustExecuteTheFinalStep) {
            executeFinalStep(workerParameters);
            return;
        }

        currentStep = this.processWorkflow.getSteps().get(stepIndex);

        boolean paused = currentStep.getPauseOrCancelAction().equals(ACTION_PAUSE);
        if (paused) {
            currentStep.setPauseOrCancelAction(ACTION_RECOVER);
            processWorkflow.setPauseRecover(RECOVER_FROM_API_PAUSE);
        } else {
            processWorkflow.setPauseRecover(PauseRecover.NO_RECOVER);
        }

        if (replayCurrentStep) {
            currentStep.setPauseOrCancelAction(ACTION_REPLAY);
            processWorkflow.setPauseRecover(PauseRecover.NO_RECOVER);
        }

        this.tryPersistProcessWorkflow();

        try {
            if (isLastStep()) {
                currentStep.setLastStep(true);
            }

            workerParameters.setPreviousStep(replayCurrentStep ? currentStep.getStepName() : null);
            this.processEngine.start(currentStep, workerParameters);
        } catch (Exception e) {
            onError(e);
        }
    }

    /**
     * Execute the final step of the workflow Update global status of the workflow Persist the process workflow
     *
     * @param workerParameters
     */
    protected void executeFinalStep(WorkerParameters workerParameters) {
        this.tryPersistProcessWorkflow();
        stepIndex = this.processWorkflow.getSteps().size() - 1;
        currentStep = this.processWorkflow.getSteps().get(stepIndex);

        currentStep.setLastStep(true);

        try {
            this.processEngine.start(currentStep, workerParameters);
        } catch (Exception e) {
            onError(e);
        }
    }

    @Override
    public synchronized void onUpdate(StatusCode statusCode) {
        StatusCode stepStatusCode = currentStep.getStepStatusCode();

        // If replay after FATAL and Process in PauseFromAPI, accept newest statusCode otherwise increment status
        boolean replayAfterFatal = StatusCode.FATAL.equals(this.processWorkflow.getStatus());
        stepStatusCode = (stepStatusCode.compareTo(statusCode) < 0 || replayAfterFatal) ? statusCode : stepStatusCode;

        currentStep.setStepStatusCode(stepStatusCode);

        // If replay after FATAL and Process in PauseFromAPI, compute newest statusCode otherwise increment status
        if (replayAfterFatal) {
            recomputeProcessWorkflowStatus(statusCode);
        } else if (this.processWorkflow.getStatus().compareTo(statusCode) < 0) {
            this.processWorkflow.setStatus(statusCode);
        }

        if (!this.tryPersistProcessWorkflow() && StatusCode.STARTED.equals(statusCode)) {
            throw new VitamRuntimeException(
                "Operation:" +
                processWorkflow.getOperationId() +
                ", stepIndex : " +
                stepIndex +
                ", stepIndexName: " +
                currentStep.getStepName() +
                ", Cannot continue with unreachable workspace ..."
            );
        }
    }

    @Override
    public StatusCode getCurrentProcessWorkflowStatus() {
        return this.processWorkflow.getStatus();
    }

    /**
     * recompute processWorkflow statusCode
     *
     * @param statusCode initial statusCode
     */
    private void recomputeProcessWorkflowStatus(StatusCode statusCode) {
        StatusCode computedStatus = statusCode;
        for (int i = 0; i <= stepIndex; i++) {
            StatusCode previousStatusCode = this.processWorkflow.getSteps().get(i).getStepStatusCode();
            if (previousStatusCode.compareTo(computedStatus) > 0) {
                computedStatus = previousStatusCode;
            }
        }

        this.processWorkflow.setStatus(computedStatus);
    }

    @Override
    public synchronized void onUpdate(String messageIdentifier, String originatingAgency) {
        if (null != messageIdentifier) {
            this.processWorkflow.setMessageIdentifier(messageIdentifier);
        }

        if (null != originatingAgency) {
            this.processWorkflow.setProdService(originatingAgency);
        }
    }

    @Override
    public synchronized void onError(Throwable throwable) {
        LOGGER.error("Error in Engine", throwable);
        // To enable recover when replay after FATAL
        this.processWorkflow.setPauseRecover(RECOVER_FROM_API_PAUSE);

        this.processWorkflow.setTargetState(ProcessState.PAUSE);
        this.processWorkflow.setStatus(StatusCode.FATAL);
        this.processWorkflow.setState(ProcessState.PAUSE);
        this.currentStep.setPauseOrCancelAction(ACTION_PAUSE);

        alertService.createAlert(
            VitamLogLevel.WARN,
            String.format(
                "[StateMachine] Process %s (%s) failed. Operation State: %s, Status: %s",
                processWorkflow.getOperationId(),
                processWorkflow.getLogbookTypeProcess(),
                processWorkflow.getState(),
                processWorkflow.getStatus()
            ),
            throwable
        );

        try {
            this.tryPersistProcessWorkflow();
        } catch (VitamRuntimeException e) {
            LOGGER.error(e);
        }
    }

    @Override
    public synchronized void onProcessEngineCancel(WorkerParameters workerParameters) {
        this.executeFinalStep(workerParameters);
    }

    @Override
    public synchronized void onProcessEngineCompleteStep(ItemStatus itemStatus, WorkerParameters workerParameters) {
        final StatusCode statusCode = itemStatus.getGlobalStatus();
        // update global status Process workFlow and process Step
        onUpdate(statusCode);

        if (!isLastStep()) {
            // if the step has been defined as Blocking and stepStatus is KO or FATAL
            // then stop the process
            if (itemStatus.shallStop(currentStep.getBehavior().equals(ProcessBehavior.BLOCKING))) {
                if (statusCode.isGreaterOrEqualToFatal()) {
                    // To enable recover when replay after FATAL
                    this.processWorkflow.setPauseRecover(RECOVER_FROM_API_PAUSE);
                    this.processWorkflow.setTargetState(ProcessState.PAUSE);
                    this.processWorkflow.setState(ProcessState.PAUSE);
                    this.currentStep.setPauseOrCancelAction(ACTION_PAUSE);

                    this.tryPersistProcessWorkflow();
                } else {
                    this.executeFinalStep(workerParameters);
                }
            } else {
                final ProcessState targetState = this.processWorkflow.getTargetState();

                if (ProcessState.COMPLETED.equals(targetState)) {
                    // Force status code to KO
                    this.processWorkflow.setTargetStatus(StatusCode.KO);
                    this.executeFinalStep(workerParameters);
                } else {
                    stepIndex++;
                    if (ProcessState.PAUSE.equals(targetState)) {
                        this.processWorkflow.setState(ProcessState.PAUSE);
                        try {
                            this.tryPersistProcessWorkflow();
                        } finally {
                            if (null != waitMonitor) {
                                waitMonitor.complete(Boolean.TRUE);
                            }
                        }
                    } else {
                        this.findAndExecuteNextStep(workerParameters, false);
                    }
                }
            }
        } else {
            try {
                if (itemStatus.getGlobalStatus().isGreaterOrEqualToFatal()) {
                    this.processWorkflow.setPauseRecover(RECOVER_FROM_API_PAUSE);
                    this.processWorkflow.setTargetState(ProcessState.PAUSE);
                    this.processWorkflow.setState(ProcessState.PAUSE);
                    this.currentStep.setPauseOrCancelAction(ACTION_PAUSE);

                    this.tryPersistProcessWorkflow();
                } else {
                    this.finalizeOperation();
                }
            } finally {
                if (waitMonitor != null) {
                    waitMonitor.complete(Boolean.TRUE);
                }
            }
        }
    }

    /**
     * Check if the state is running
     *
     * @return true if the current state is running
     */
    protected boolean isRunning() {
        return ProcessState.RUNNING.equals(processWorkflow.getState());
    }

    /**
     * Check if the state is pause
     *
     * @return true if the current state is pause
     */
    protected boolean isPause() {
        return ProcessState.PAUSE.equals(processWorkflow.getState());
    }

    /**
     * Check if the state is completed
     *
     * @return true if the current state is completed
     */
    public boolean isCompleted() {
        return ProcessState.COMPLETED.equals(processWorkflow.getState());
    }

    @Override
    public boolean isRecover() {
        return (
            !ProcessState.COMPLETED.equals(processWorkflow.getState()) &&
            PauseRecover.RECOVER_FROM_SERVER_PAUSE.equals(processWorkflow.getPauseRecover())
        );
    }

    /**
     * Check if the current step is the last one
     *
     * @return true if the current step is the last one
     */
    protected boolean isLastStep() {
        return stepIndex == this.processWorkflow.getSteps().size() - 1;
    }

    /**
     * Persist the process workflow in the workspace
     *
     * @return true is success, false else
     */
    protected boolean tryPersistProcessWorkflow() {
        if (
            ProcessState.COMPLETED.equals(processWorkflow.getState()) &&
            null == processWorkflow.getProcessCompletedDate()
        ) {
            processWorkflow.setProcessCompletedDate(LocalDateUtil.now());
        }

        try {
            dataManagement.persistProcessWorkflow(VitamConfiguration.getWorkspaceWorkflowsFolder(), processWorkflow);
            return true;
        } catch (Exception e) {
            LOGGER.error("Cannot persist process workflow > ", e);
            return false;
        }
    }

    protected void finalizeOperation() {
        if (null != this.processWorkflow.getTargetStatus()) {
            this.processWorkflow.setStatus(this.processWorkflow.getTargetStatus());
        }

        this.processWorkflow.setState(ProcessState.COMPLETED);
        this.processWorkflow.setTargetState(ProcessState.COMPLETED);

        if (!tryPersistProcessWorkflow()) {
            alertService.createAlert(
                VitamLogLevel.WARN,
                "[StateMachine] The latest of ProcessWorkflow (" +
                processWorkflow.getOperationId() +
                ") not saved in workspace. Expected > State: COMPLETED, Status: " +
                processWorkflow.getStatus()
            );
        }

        dataManagement.removeOperationContainer(this.processWorkflow, this.workspaceClientFactory);

        silentlyCleanBackupOperation();
    }

    private void silentlyCleanBackupOperation() {
        try {
            switch (processWorkflow.getLogbookTypeProcess()) {
                case INGEST:
                case MASTERDATA:
                case TRACEABILITY:
                case INGEST_TEST:
                case AUDIT:
                case DATA_MIGRATION:
                case COMPUTE_INHERITED_RULES:
                    LOGGER.debug(
                        "Cleanup operation context. No operation context for the process type " +
                        processWorkflow.getLogbookTypeProcess()
                    );
                    break;
                default:
                    OperationContextMonitor operationContextMonitor = new OperationContextMonitor();
                    operationContextMonitor.deleteBackup(
                        VitamConfiguration.getDefaultStrategy(),
                        processWorkflow.getOperationId(),
                        processWorkflow.getLogbookTypeProcess()
                    );
            }
        } catch (Exception e) {
            String msg =
                "Cleaning the offer temporary backup of the operation (" +
                processWorkflow.getOperationId() +
                ") failed. Operation State: COMPLETED, Status: " +
                processWorkflow.getStatus();

            LOGGER.error(msg, e);
            alertService.createAlert(VitamLogLevel.WARN, "[StateMachine] " + msg);
        }
    }

    public ProcessStep getCurrentStep() {
        return currentStep;
    }

    public int getStepIndex() {
        return stepIndex;
    }
}
