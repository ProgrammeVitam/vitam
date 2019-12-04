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

package fr.gouv.vitam.processing.management.core;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.StateNotAllowedException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.PauseOrCancelAction;
import fr.gouv.vitam.common.model.processing.ProcessBehavior;
import fr.gouv.vitam.logbook.common.MessageLogbookEngineHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.processing.common.automation.IEventsProcessEngine;
import fr.gouv.vitam.processing.common.automation.IEventsState;
import fr.gouv.vitam.processing.common.exception.ProcessingEngineException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.exception.ProcessingStorageWorkspaceException;
import fr.gouv.vitam.processing.common.exception.StepsNotFoundException;
import fr.gouv.vitam.processing.common.model.PauseRecover;
import fr.gouv.vitam.processing.common.model.ProcessStep;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.data.core.management.ProcessDataManagement;
import fr.gouv.vitam.processing.data.core.management.WorkspaceProcessDataManagement;
import fr.gouv.vitam.processing.distributor.api.ProcessDistributor;
import fr.gouv.vitam.processing.engine.api.ProcessEngine;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * State Machine class implementing the Interface. Dealing with evolution of workflows
 */
public class StateMachine implements IEventsState, IEventsProcessEngine {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(StateMachine.class);

    private ProcessEngine processEngine;
    private ProcessWorkflow processWorkflow;
    private ProcessDataManagement dataManagement;
    private String operationId;

    private int stepIndex = -1;
    private int stepTotal;

    private boolean replayAfterFatal = false;

    private List<ProcessStep> steps;
    private ProcessStep currentStep = null;
    private boolean stepByStep = false;

    private StatusCode status = StatusCode.UNKNOWN;

    private ProcessState state;
    private volatile ProcessState targetState = null;
    private PauseOrCancelAction pauseCancelAction = PauseOrCancelAction.ACTION_RUN;

    private CompletableFuture<Boolean> waitMonitor;

    private Map<String, String> engineParams = Maps.newHashMap();
    private String messageIdentifier;
    private String prodService;

    private WorkspaceClientFactory workspaceClientFactory;
    private LogbookOperationsClientFactory logbookOperationsClientFactory;

    public StateMachine(ProcessWorkflow processWorkflow, ProcessEngine processEngine) {
        this(processWorkflow, processEngine, WorkspaceProcessDataManagement.getInstance(),
            WorkspaceClientFactory.getInstance(),
            LogbookOperationsClientFactory.getInstance());
    }

    @VisibleForTesting
    public StateMachine(ProcessWorkflow processWorkflow, ProcessEngine processEngine,
        ProcessDataManagement dataManagement,
        WorkspaceClientFactory workspaceClientFactory, LogbookOperationsClientFactory logbookOperationsClientFactory) {
        this.workspaceClientFactory = workspaceClientFactory;
        this.logbookOperationsClientFactory = logbookOperationsClientFactory;
        if (null == processWorkflow) {
            throw new IllegalArgumentException("The parameter processWorkflow must not be null");
        }
        if (null == processEngine) {
            throw new IllegalArgumentException("The parameter processEngine must not be null");
        }

        this.processWorkflow = processWorkflow;
        this.state = processWorkflow.getState();
        this.messageIdentifier = processWorkflow.getMessageIdentifier();
        this.prodService = processWorkflow.getProdService();
        this.steps = processWorkflow.getSteps();
        this.processEngine = processEngine;
        this.dataManagement = dataManagement;
        this.stepTotal = this.steps.size();
        operationId = processWorkflow.getOperationId();
        initStepIndex();
    }

    /**
     * This is important after a safe restart of the server In case of the process workflow is stepBystep mode we
     * continue from the last not yet treated step
     */
    private void initStepIndex() {
        final Iterator<ProcessStep> it = processWorkflow.getSteps().iterator();
        while (it.hasNext()) {
            final ProcessStep step = it.next();
            if (!PauseRecover.NO_RECOVER.equals(processWorkflow.getPauseRecover()) &&
                step.getPauseOrCancelAction().equals(PauseOrCancelAction.ACTION_PAUSE)) {
                step.setPauseOrCancelAction(PauseOrCancelAction.ACTION_RECOVER);

                break;
            } else if (!StatusCode.UNKNOWN.equals(step.getStepStatusCode()) &&
                !ProcessBehavior.FINALLY.equals(step.getBehavior())) {
                stepIndex++;
            } else {
                break;
            }
        }

        /**
         * Initialize currentStep needed for processWorkflow stopped from API PauseRecover.RECOVER_FROM_API_PAUSE when
         * execute the currentStep from stopped processWorkflow two possible case: 1: Start from stopped processWorkflow
         * without server restart 2: Start from stopped processWorkflow when server restarts As executeSteps check the
         * current step PauseOrCancelAction, currentStep is null when server restart. This is why we have to initialize
         * in the constructor the current step. This is valable only for processWorkflow where PauseRecover is
         * PauseRecover.RECOVER_FROM_API_PAUSE
         *
         */
        if (PauseRecover.RECOVER_FROM_API_PAUSE.equals(processWorkflow.getPauseRecover()) &&
            stepIndex + 1 <= stepTotal - 1) {
            currentStep = steps.get(stepIndex + 1);
        }
    }

    @Override
    synchronized public void resume(WorkerParameters workerParameters)
        throws StateNotAllowedException, ProcessingException {
        this.state.eval(ProcessState.RUNNING);
        this.doRunning(workerParameters, ProcessState.RUNNING);
    }

    @Override
    synchronized public void next(WorkerParameters workerParameters)
        throws StateNotAllowedException, ProcessingException {
        this.state.eval(ProcessState.RUNNING);
        this.doRunning(workerParameters, ProcessState.PAUSE);

    }

    @Override
    synchronized public void replay(WorkerParameters workerParameters)
        throws StateNotAllowedException, ProcessingException {
        this.state.eval(ProcessState.RUNNING);
        this.doReplay(workerParameters, ProcessState.PAUSE);

    }

    @Override
    synchronized public void pause()
        throws StateNotAllowedException {
        this.state.eval(ProcessState.PAUSE);
        this.doPause(PauseRecover.RECOVER_FROM_API_PAUSE);
    }

    @Override
    public void shutdown() throws StateNotAllowedException {
        this.doPause(PauseRecover.RECOVER_FROM_SERVER_PAUSE);
    }

    @Override
    synchronized public void cancel() throws StateNotAllowedException, ProcessingException {
        this.state.eval(ProcessState.COMPLETED);
        doCompleted();
    }

    @Override
    public boolean isDone() {
        return ProcessState.COMPLETED.equals(processWorkflow.getState()) ||
            ProcessState.PAUSE.equals(processWorkflow.getState());
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
        return processWorkflow.getContextId();
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
     * @throws StateNotAllowedException
     */
    protected void doPause(PauseRecover pauseRecover)
        throws StateNotAllowedException {
        if (PauseRecover.RECOVER_FROM_SERVER_PAUSE.equals(pauseRecover)) {
            this.waitMonitor = new CompletableFuture<>();
        }

        if (isLastStep()) {
            targetState = ProcessState.COMPLETED;
        } else {
            targetState = ProcessState.PAUSE;

            /**
             * As pause can occurs when step is in processEngine, but not yet in the distributor Wait max 1 second to be
             * sur that step is in tje distributor If pause is not applied after 1 second, then return without calling
             * waitMonitor.get bellow Else if pause applied then wait until distributor responds using waitMonitor.get
             * bellow
             */
            int retry = 100;
            boolean pausedApplied = this.processEngine.pause(operationId);
            while (!pausedApplied) {
                if (retry < 0) {
                    break;
                }

                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    LOGGER.error(e);
                }
                pausedApplied = this.processEngine.pause(operationId);
                retry--;
            }

            if (!pausedApplied) {
                LOGGER.error("Pause is not applied in the distributor !!!");
                return;
            }

            /**
             * We can just stop here (updating the current step) and the information will be passed in reference to the
             * worker task currentStep.setPauseOrCancelAction(PauseOrCancelAction.ACTION_PAUSE);
             */

            processWorkflow.setPauseRecover(pauseRecover);
        }

        if (PauseRecover.RECOVER_FROM_SERVER_PAUSE.equals(pauseRecover)) {

            try {
                this.waitMonitor.get(4, TimeUnit.SECONDS);
            } catch (Exception e) {
                LOGGER.error(e);
            } finally {

                boolean stop = false;
                if (ProcessState.PAUSE.equals(processWorkflow.getState())) {
                    stop = true;
                }

                if (ProcessState.COMPLETED.equals(processWorkflow.getState())) {
                    stop = true;
                }

                if (ProcessState.PAUSE.equals(state)) {
                    stop = true;
                }

                if (pauseCancelAction.equals(PauseOrCancelAction.ACTION_PAUSE)) {
                    stop = true;
                }


                if (stop) {
                    processWorkflow.setPauseRecover(pauseRecover);
                    // Be sur that ProcessWorkflow is persisted in workspace
                    this.persistProcessWorkflow();
                }
            }

        }

    }

    /**
     * Change the state of the process to completed Can be called only from running or pause state If running state, the
     * next step will be completed
     *
     * @throws StateNotAllowedException
     */
    protected void doCompleted() throws StateNotAllowedException, ProcessingException {
        if (isRunning()) {
            targetState = ProcessState.COMPLETED;
            if (!isLastStep()) {
                this.processEngine.cancel(operationId);
                /**
                 * We can just stop here (updating the current step) and the information will be passed in reference to
                 * the worker task currentStep.setPauseOrCancelAction(PauseOrCancelAction.ACTION_CANCEL);
                 */
            }
        } else if (isPause()) {
            status = StatusCode.FATAL;
            targetState = ProcessState.COMPLETED;
            if (stepIndex == -1) {
                state = ProcessState.COMPLETED;
                this.persistProcessWorkflow();
            } else {
                state = ProcessState.RUNNING;
                this.executeFinallyStep(null);
            }
        }

    }

    /**
     * Change state of the process to running Can be called only from pause state
     *
     * @param workerParameters the parameters to be passed to the distributor
     * @param targetState if true, run ony the next step
     * @throws StateNotAllowedException
     */
    protected void doRunning(WorkerParameters workerParameters, ProcessState targetState)
        throws ProcessingException {


        if (null == targetState)
            throw new ProcessingException("The targetState is required");
        // Double check
        if (isRunning()) {
            throw new ProcessingException("doRunning not allowed on already running state");
        }

        this.state = ProcessState.RUNNING;
        this.targetState = targetState;
        stepByStep = ProcessState.PAUSE.equals(targetState);

        // if pause after fatal, force replay last step (if resume or next)
        replayAfterFatal = (PauseRecover.RECOVER_FROM_API_PAUSE.equals(processWorkflow.getPauseRecover())
            && StatusCode.FATAL.equals(processWorkflow.getStatus()));

        executeSteps(workerParameters, processWorkflow.getPauseRecover(), replayAfterFatal);
    }

    /**
     * Change state of the process to running Can be called only from pause state
     *
     * @param workerParameters the parameters to be passed to the distributor
     * @param targetState if true, run ony the next step
     * @throws StateNotAllowedException
     */
    protected void doReplay(WorkerParameters workerParameters, ProcessState targetState)
        throws ProcessingException {
        if (null == targetState)
            throw new ProcessingException("The targetState is required");
        // Double check
        if (isRunning()) {
            throw new ProcessingException("doRunning not allowed on already running state");
        }

        this.state = ProcessState.RUNNING;
        this.targetState = targetState;
        stepByStep = ProcessState.PAUSE.equals(targetState);

        // check if pause after FATAL
        replayAfterFatal = (PauseRecover.RECOVER_FROM_API_PAUSE.equals(processWorkflow.getPauseRecover())
            && StatusCode.FATAL.equals(processWorkflow.getStatus()));

        // here, we need to add something in order to tell that we want the current step to be re executed
        executeSteps(workerParameters, processWorkflow.getPauseRecover(), true);
    }

    /**
     * Execute steps of the workflow and manage index of the current step Call engine to execute the current step
     *
     * @param workerParameters
     */
    protected void executeSteps(WorkerParameters workerParameters, PauseRecover pauseRecover, boolean backwards)
        throws ProcessingException {
        if (backwards) {
            stepIndex--;
        }
        /**
         * This is required when pause action origin is API, we have to We have to check if we passe to the next step or
         * continue with the current step Be careful, when server starts, the currentStep is already initialized in
         * initIndex With stepIndex +1 without increment stepIndex
         */
        if (PauseRecover.RECOVER_FROM_API_PAUSE.equals(pauseRecover)) {

            /**
             * currentStep.getPauseOrCancelAction().equals(PauseOrCancelAction.ACTION_PAUSE)) This situation occurs when
             * pause the processWorkflow without restart server In this case, the initIndex is not used to update
             * pauseCancelAction
             */
            if (currentStep.getPauseOrCancelAction().equals(PauseOrCancelAction.ACTION_PAUSE)) {
                currentStep.setPauseOrCancelAction(PauseOrCancelAction.ACTION_RECOVER);

            } else {
                /**
                 * As the restart of the server can occurs with processWorkflow in pause mode After start of the server
                 * the current step in PauseOrCancelAction.ACTION_PAUSE is already initiated in the constructor and
                 * updated to be PauseOrCancelAction.ACTION_RECOVER In this case, we have not to passe to the next step,
                 * but continue with the current step As the currentStep in the constructor is initialized with
                 * stepIndex + 1 without increment stepIndex So increment of stepIndex is required
                 *
                 * In fact, currentStep = steps.get(stepIndex); bellow will have the correct current step
                 *
                 */

                stepIndex++;

                /**
                 * In case where ActionPause was occurred, But the distributor has completed the current executed step
                 * In this case, the distributor update PauseOrCancelAction of the step to be ACTION_COMPLETE So, we
                 * have a processWorkflow in RECOVER_FROM_API_PAUSE but the current step is ACTION_COMPLETE As the
                 * current step is ACTION_COMPLETE, we have to execute the next step but without using the
                 * distributorIndex To prevent "You run the wrong step" exception thrown by the distributor when using
                 * the distributorIndex but the step id persisted in the index do not match the current executed step To
                 * disable using distributorIndex, in this cas, we have to start engine with PauseRecover.NO_RECOVER
                 */
                pauseRecover = PauseRecover.NO_RECOVER;
                processWorkflow.setPauseRecover(PauseRecover.NO_RECOVER);
            }
        } else {
            stepIndex++;
        }

        if (stepIndex <= stepTotal - 1) {
            currentStep = steps.get(stepIndex);

            if (backwards) {
                currentStep.setPauseOrCancelAction(PauseOrCancelAction.ACTION_REPLAY);
            }
        } else {
            throw new StepsNotFoundException("No step found in the process workflow");
        }

        if (!PauseRecover.NO_RECOVER.equals(pauseRecover)) {
            processWorkflow.setPauseRecover(PauseRecover.NO_RECOVER);
        }

        if (!this.persistProcessWorkflow()) {
            // As the workspace throw an exception just update logbook and in memory
            status = StatusCode.FATAL;
            this.finalizeLogbook(workerParameters);
            throw new ProcessingException(
                "StateMachine can't persist ProcessWorkflow > see log of the persistProcessWorkflow method");
        }

        if (null != currentStep) {
            engineParams.put(SedaConstants.TAG_MESSAGE_IDENTIFIER, messageIdentifier);
            engineParams.put(SedaConstants.TAG_ORIGINATINGAGENCY, prodService);
            try {
                workerParameters.setPreviousStep(backwards ? currentStep.getStepName() : null);
                this.processEngine.start(currentStep, workerParameters, engineParams, pauseRecover);
            } catch (ProcessingEngineException e) {
                onError(e, workerParameters);
            }
        }
    }

    /**
     * Execute the finally step of the workflow Update global status of the workflow Persist the process workflow
     *
     * @param workerParameters
     */
    protected void executeFinallyStep(WorkerParameters workerParameters) {
        if (!this.persistProcessWorkflow()) {
            // As the workspace throw an exception just update logbook and in memory
            status = StatusCode.FATAL;
            this.finalizeLogbook(workerParameters);
            return;
        }

        stepIndex = stepTotal - 1;
        currentStep = steps.get(stepTotal - 1);
        if (null != currentStep) {
            engineParams.put(SedaConstants.TAG_MESSAGE_IDENTIFIER, messageIdentifier);
            engineParams.put(SedaConstants.TAG_ORIGINATINGAGENCY, prodService);
            try {
                this.processEngine.start(currentStep, workerParameters, engineParams, PauseRecover.NO_RECOVER);
            } catch (Exception e) {
                onError(e, workerParameters);
            }
        }
    }

    @Override
    synchronized public void onUpdate(StatusCode statusCode) {
        StatusCode stepStatusCode = currentStep.getStepStatusCode();
        if (stepStatusCode != null) {
            // if replay after FATAL and Process in PauseFromAPI, accept newest statusCode otherwise increment status
            stepStatusCode = (stepStatusCode.compareTo(statusCode) < 0 || replayAfterFatal)
                ? statusCode : stepStatusCode;
        }
        currentStep.setStepStatusCode(stepStatusCode);

        // if replay after FATAL and Process in PauseFromAPI, compute newest statusCode otherwise increment status
        if (replayAfterFatal) {
            this.status = recomputeProcessWorkflowStatus(statusCode);
        } else if (this.status.compareTo(statusCode) < 0) {
            this.status = statusCode;
        }

        // only force status update for replayed step
        if (replayAfterFatal) {
            replayAfterFatal = false;
        }
    }

    /**
     * recompute processWorkflow statusCode
     *
     * @param statusCode initial statusCode
     * @return the computed statusCode
     */
    private StatusCode recomputeProcessWorkflowStatus(StatusCode statusCode) {
        StatusCode computedStatus = statusCode;
        for (int i = 0; i < stepIndex; i++) {
            StatusCode previousStatusCode = this.steps.get(i).getStepStatusCode();
            if (previousStatusCode.compareTo(computedStatus) > 0) {
                computedStatus = previousStatusCode;
            }
        }

        return computedStatus;
    }

    @Override
    synchronized public void onUpdate(String messageIdentifier, String prodService) {
        if (null != messageIdentifier)
            this.messageIdentifier = messageIdentifier;
        if (null != prodService)
            this.prodService = prodService;
    }

    @Override
    synchronized public void onError(Throwable throwable, WorkerParameters workerParameters) {
        LOGGER.error("Error in Engine", throwable);
        status = StatusCode.FATAL;

        state = ProcessState.PAUSE;
        targetState = ProcessState.PAUSE;

        // To enable recover when replay after FATAL
        processWorkflow.setPauseRecover(PauseRecover.RECOVER_FROM_API_PAUSE);

        this.persistProcessWorkflow();
    }

    @Override
    synchronized public void onPauseOrCancel(PauseOrCancelAction pauseCancelAction, WorkerParameters workerParameters) {
        try {
            switch (pauseCancelAction) {
                case ACTION_PAUSE:
                    state = ProcessState.PAUSE;
                    this.persistProcessWorkflow();
                    targetState = ProcessState.PAUSE;
                    break;
                case ACTION_CANCEL:
                    status = StatusCode.FATAL;
                    targetState = ProcessState.COMPLETED;
                    this.executeFinallyStep(workerParameters);
                    break;
            }
        } finally {
            this.pauseCancelAction = pauseCancelAction;

            if (null != waitMonitor) {
                this.waitMonitor.complete(Boolean.TRUE);
            }
        }

    }

    @Override
    synchronized public void onComplete(ItemStatus itemStatus, WorkerParameters workerParameters) {
        if (!isLastStep()) {
            final StatusCode statusCode = itemStatus.getGlobalStatus();
            // update global status Process workFlow and process Step
            onUpdate(statusCode);

            // if the step has been defined as Blocking and stepStatus is KO or FATAL
            // then stop the process
            if (itemStatus.shallStop(currentStep.getBehavior().equals(ProcessBehavior.BLOCKING))) {
                if (statusCode.isGreaterOrEqualToFatal()) {
                    state = ProcessState.PAUSE;
                    targetState = ProcessState.PAUSE;

                    // To enable recover when replay after FATAL
                    processWorkflow.setPauseRecover(PauseRecover.RECOVER_FROM_API_PAUSE);

                    this.persistProcessWorkflow();
                } else {
                    this.executeFinallyStep(workerParameters);
                }
            } else if (!isCompleted()) {
                if (ProcessState.COMPLETED.equals(targetState)) {
                    status = StatusCode.FATAL;
                    targetState = ProcessState.COMPLETED;
                    this.executeFinallyStep(workerParameters);

                } else if (ProcessState.PAUSE.equals(targetState)) {
                    state = ProcessState.PAUSE;
                    targetState = ProcessState.PAUSE;
                    try {
                        this.persistProcessWorkflow();
                    } finally {
                        if (null != waitMonitor) {
                            waitMonitor.complete(Boolean.FALSE);
                        }
                    }

                } else {
                    targetState = ProcessState.RUNNING;
                    try {
                        this.executeSteps(workerParameters, PauseRecover.NO_RECOVER, false);
                    } catch (ProcessingException e) {
                        LOGGER.error("ProcessEngine error > ", e);
                    }
                }
            }
        } else {
            try {
                if (itemStatus.getGlobalStatus().isGreaterOrEqualToFatal()) {
                    status = StatusCode.FATAL;

                    state = ProcessState.PAUSE;
                    targetState = ProcessState.PAUSE;

                    processWorkflow.setPauseRecover(PauseRecover.RECOVER_FROM_API_PAUSE);
                } else {
                    this.finalizeLogbook(workerParameters);
                }
            } finally {
                this.persistProcessWorkflow();
            }

            engineParams.clear();
        }
    }

    /**
     * Check if the state is running
     *
     * @return true if the current state is running
     */
    protected boolean isRunning() {
        return ProcessState.RUNNING.equals(state);
    }

    /**
     * Check if the state is pause
     *
     * @return true if the current state is pause
     */
    protected boolean isPause() {
        return ProcessState.PAUSE.equals(state);
    }

    /**
     * Check if the state is completed
     *
     * @return true if the current state is completed
     */
    public boolean isCompleted() {
        return ProcessState.COMPLETED.equals(state);
    }

    @Override
    public boolean isRecover() {
        return !ProcessState.COMPLETED.equals(state) &&
            PauseRecover.RECOVER_FROM_SERVER_PAUSE.equals(processWorkflow.getPauseRecover());
    }

    /**
     * Check if the current step is the last one
     *
     * @return true if the current step is the last one
     */
    protected boolean isLastStep() {
        return stepIndex >= (stepTotal - 1);
    }

    /**
     * Persist the process workflow in the workspace
     *
     * @return true is success, false else
     */
    protected boolean persistProcessWorkflow() {
        processWorkflow.setMessageIdentifier(messageIdentifier);
        processWorkflow.setProdService(prodService);
        processWorkflow.setStepByStep(stepByStep);
        processWorkflow.setStatus(status);
        processWorkflow.setState(state);

        if (ProcessState.COMPLETED.equals(state)) {
            processWorkflow.setProcessCompletedDate(LocalDateTime.now());
        }

        try {
            dataManagement.persistProcessWorkflow(VitamConfiguration.getWorkspaceWorkflowsFolder(),
                operationId, processWorkflow);
            return true;
        } catch (InvalidParseOperationException | ProcessingStorageWorkspaceException e) {
            LOGGER.error("Cannot persist process workflow file, set status to FAILED, do retry ...", e);
            // Retry after 5 second
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e1) {
            }
            try {
                dataManagement.persistProcessWorkflow(VitamConfiguration.getWorkspaceWorkflowsFolder(),
                    operationId, processWorkflow);
                return true;
            } catch (InvalidParseOperationException | ProcessingStorageWorkspaceException ex) {
                LOGGER.error("Retry > Cannot persist process workflow file, set status to FAILED", e);
                return false;
            }
        }
    }

    /**
     * Create the final logbook entry for the corresponding process workflow This entry was created in ingest internal
     * and as the process is full async we moved it to here
     */
    protected void finalizeLogbook(WorkerParameters workParams) {

        final LogbookOperationsClient logbookClient = logbookOperationsClientFactory.getClient();
        try {
            final GUID operationGuid = GUIDReader.getGUID(operationId);
            final GUID eventGuid = GUIDFactory.newEventGUID(operationGuid);
            logbook(logbookClient, eventGuid, operationGuid, processWorkflow.getLogbookTypeProcess(), status, workParams
                .getWorkflowIdentifier(), GUIDReader.getGUID(workParams.getRequestId()));

        } catch (Exception e) {
            LOGGER.error("Error while finalize logbook of the process workflow, do retry ...", e);

            // Retry after 5 second
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e1) {
            }

            try {
                final GUID operationGuid = GUIDReader.getGUID(operationId);
                final GUID eventGuid = GUIDFactory.newEventGUID(operationGuid);
                logbook(logbookClient, eventGuid, operationGuid, processWorkflow.getLogbookTypeProcess(), status,
                    workParams
                        .getWorkflowIdentifier(), GUIDReader.getGUID(workParams.getRequestId()));

            } catch (Exception ex) {
                LOGGER.error("Retry > error while finalize logbook of the process workflow", e);
            }
        } finally {
            processWorkflow.setMessageIdentifier(messageIdentifier);
            processWorkflow.setProdService(prodService);
            processWorkflow.setStepByStep(stepByStep);
            processWorkflow.setStatus(status);
            state = ProcessState.COMPLETED;
            targetState = ProcessState.COMPLETED;
            processWorkflow.setProcessCompletedDate(LocalDateTime.now());
            processWorkflow.setState(state);
            if (null != logbookClient) {
                logbookClient.close();
            }

            cleanWorkspace();
        }
    }


    private void cleanWorkspace() {
        try (WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {
            if (workspaceClient.isExistingContainer(operationId)) {
                workspaceClient.deleteContainer(operationId, true);
            }

            if (workspaceClient
                .isExistingObject(ProcessDataManagement.PROCESS_CONTAINER,
                    ProcessDistributor.DISTRIBUTOR_INDEX + "/" + operationId + ".json")) {
                workspaceClient.deleteObject(ProcessDataManagement.PROCESS_CONTAINER,
                    ProcessDistributor.DISTRIBUTOR_INDEX + "/" + operationId + ".json");
            }
        } catch (Exception e) {
            LOGGER.error("Error while clear the container " + operationId + " from the workspace", e);
        }
    }

    private void logbook(LogbookOperationsClient client, GUID eventIdentifier, GUID operationGuid,
        LogbookTypeProcess logbookTypeProcess,
        StatusCode statusCode, String eventType, GUID requestId) throws Exception {
        MessageLogbookEngineHelper messageLogbookEngineHelper = new MessageLogbookEngineHelper(logbookTypeProcess);
        final LogbookOperationParameters parameters = LogbookParametersFactory
            .newLogbookOperationParameters(
                eventIdentifier,
                eventType,
                operationGuid,
                logbookTypeProcess,
                statusCode,
                messageLogbookEngineHelper.getLabelOp(eventType, statusCode),
                requestId);
        parameters.putParameterValue(LogbookParameterName.outcomeDetail,
            messageLogbookEngineHelper.getOutcomeDetail(eventType, statusCode));
        client.update(parameters);
    }


}
