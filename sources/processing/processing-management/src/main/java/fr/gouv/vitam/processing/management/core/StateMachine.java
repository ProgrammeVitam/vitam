/*
 *  Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *  <p>
 *  contact.vitam@culture.gouv.fr
 *  <p>
 *  This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 *  high volumetry securely and efficiently.
 *  <p>
 *  This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 *  software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 *  circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 *  <p>
 *  As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 *  users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 *  successive licensors have only limited liability.
 *  <p>
 *  In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 *  developing or reproducing the software by the user in light of its specific status of free software, that may mean
 *  that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 *  experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 *  software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 *  to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *  <p>
 *  The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 *  accept its terms.
 */

package fr.gouv.vitam.processing.management.core;

import com.google.common.collect.Maps;
import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.StateNotAllowedException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.StatusCode;
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
import fr.gouv.vitam.processing.common.model.ProcessBehavior;
import fr.gouv.vitam.processing.common.model.ProcessStep;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.data.core.management.ProcessDataManagement;
import fr.gouv.vitam.processing.data.core.management.WorkspaceProcessDataManagement;
import fr.gouv.vitam.processing.engine.api.ProcessEngine;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * T
 */
public class StateMachine implements IEventsState, IEventsProcessEngine {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(StateMachine.class);

    private static final String INGEST_WORKFLOW = "PROCESS_SIP_UNITARY";

    private static final String MESSAGE_IDENTIFIER = "messageIdentifier";
    private static final String PROD_SERVICE = "messageIdentifier";


    private ProcessEngine processEngine;
    private ProcessWorkflow processWorkflow;
    private ProcessDataManagement dataManagement;
    private String operationId = null;

    private int stepIndex = -1;
    private int stepTotal = 0;

    private Long globalStartTime = null;
    private Long globalEndTime = null;

    private List<ProcessStep> steps = new ArrayList<>();
    private ProcessStep currentStep = null;
    private boolean stepByStep = false;

    private StatusCode status = StatusCode.UNKNOWN;

    private ProcessState state = null;
    private ProcessState targetState = null;


    private Map<String, String > engineParams = Maps.newHashMap();
    private String messageIdentifier = null;
    private String prodService = null;

    public StateMachine(ProcessWorkflow processWorkflow, ProcessEngine processEngine) {
        if (null == processWorkflow)
            throw new IllegalArgumentException("The parameter processWorkflow must not be null");
        if (null == processEngine)
            throw new IllegalArgumentException("The parameter processEngine must not be null");

        this.processWorkflow = processWorkflow;
        this.state = processWorkflow.getState();
        this.messageIdentifier = processWorkflow.getMessageIdentifier();
        this.prodService = processWorkflow.getProdService();
        this.steps = processWorkflow.getSteps();
        this.processEngine = processEngine;
        this.dataManagement = WorkspaceProcessDataManagement.getInstance();
        this.stepTotal = this.steps.size();

        initStepIndex();
    }

    /**
     * This is important after a safe restart of the server
     * In case of the process workflow is stepBystep mode we continue from the last not yet treated step
     */
    private void initStepIndex() {
        final Iterator<ProcessStep> it = processWorkflow.getSteps().iterator();
        while(it.hasNext()) {
            final ProcessStep step = it.next();
            if (!StatusCode.UNKNOWN.equals(step.getStepStatusCode())
                && !ProcessBehavior.FINALLY.equals(step.getBehavior())) {
                stepIndex ++;
            } else {
                break;
            }
        }
    }

    @Override synchronized public void resume(WorkerParameters workerParameters)
        throws StateNotAllowedException, ProcessingException {
        this.state.eval(ProcessState.RUNNING);
        this.doRunning(workerParameters, ProcessState.RUNNING);
    }

    @Override synchronized public void next(WorkerParameters workerParameters)
        throws StateNotAllowedException, ProcessingException {
        this.state.eval(ProcessState.RUNNING);
        this.doRunning(workerParameters, ProcessState.PAUSE);

    }

    @Override synchronized public void pause() throws StateNotAllowedException {
        this.state.eval(ProcessState.PAUSE);
        this.doPause();
    }

    @Override synchronized public void cancel() throws StateNotAllowedException, ProcessingException {
        this.state.eval(ProcessState.COMPLETED);
        doCompleted();
    }

    /**
     * Change state of the process to pause
     * Can be called only from running state
     * If last step then change state to completed
     * @throws StateNotAllowedException
     */
    protected void doPause() throws StateNotAllowedException {
        if (isLastStep()) {
            targetState = ProcessState.COMPLETED;
        } else {
            targetState = ProcessState.PAUSE;
        }
    }

    /**
     * Change the state of the process to completed
     * Can be called only from running or pause state
     * If running state, the next step will be completed
     * @throws StateNotAllowedException
     */
    protected void doCompleted() throws StateNotAllowedException, ProcessingException {
        if (isRunning()) {
            targetState = ProcessState.COMPLETED;
        } else if (isPause()){
            status = StatusCode.FATAL;
            this.executeFinallyStep(null);
        }
    }

    /**
     * Change state of the process to running
     * Can be called only from pause state
     * @param workerParameters the parameters to be passed to the distributor
     * @param targetState if true, run ony the next step
     * @throws StateNotAllowedException
     */
    protected void doRunning(WorkerParameters workerParameters, ProcessState targetState)
        throws ProcessingException {
        if (null == targetState) throw new ProcessingException("The targetState is required");
        // Double check
        if (isRunning()) {
            throw new ProcessingException("doRunning not allowed on already running state");
        }

        operationId = workerParameters.getContainerName();
        this.state = ProcessState.RUNNING;
        this.targetState = targetState;
        stepByStep = ProcessState.PAUSE.equals(targetState);

        executeSteps(workerParameters);
    }

    /**
     * Execute steps of the workflow and manage index of the current step
     * Call engine to execute the current step
     *@param workerParameters
     */
    protected void executeSteps(WorkerParameters workerParameters)
        throws ProcessingException {
        stepIndex ++;
        if (stepIndex == 0) {
            globalStartTime = Calendar.getInstance().getTimeInMillis();
        }

        if (stepIndex <= stepTotal -1) {
            currentStep = steps.get(stepIndex);
        } else {
            throw new StepsNotFoundException("No step found in the process workflow");
        }

        if (!this.persistProcessWorkflow()) {
            // As the workspace throw an exception just update logbook and in memory
            status = StatusCode.FATAL;
            this.finalizeLogbook();
            throw new ProcessingException("StateMachine can't persist ProcessWorkflow > see log of the persistProcessWorkflow method");
        }

        if (null != currentStep) {
            engineParams.put(MESSAGE_IDENTIFIER, messageIdentifier);
            engineParams.put(PROD_SERVICE, prodService);
            try {
                this.processEngine.start(currentStep, workerParameters, engineParams);
            } catch (ProcessingEngineException e) {
                LOGGER.error("ProcessEngine error ", e);
                status = StatusCode.FATAL;
                try {
                    this.finalizeLogbook();
                } finally {
                    this.persistProcessWorkflow();
                }
            }
        }
    }

    /**
     * Execute the finally step of the workflow
     * Update global status of the workflow
     * Persist the process workflow
     *@param workerParameters
     */
    protected void executeFinallyStep(WorkerParameters workerParameters) {
        if (!this.persistProcessWorkflow()) {
            // As the workspace throw an exception just update logbook and in memory
            status = StatusCode.FATAL;
            this.finalizeLogbook();
            return;
        }

        stepIndex = stepTotal - 1;
        currentStep = steps.get(stepTotal -1);
        if (null != currentStep) {
            engineParams.put(MESSAGE_IDENTIFIER, messageIdentifier);
            engineParams.put(PROD_SERVICE, prodService);
            try {
                this.processEngine.start(currentStep, workerParameters, engineParams);
            } catch (ProcessingEngineException e) {
                LOGGER.error("ProcessEngine error", e);
                status = StatusCode.FATAL;
                try {
                    this.finalizeLogbook();
                } finally {
                    this.persistProcessWorkflow();
                }
            }
        }
    }

    @Override synchronized public void onUpdate(StatusCode statusCode) {
        StatusCode stepStatusCode = currentStep.getStepStatusCode();
        if (stepStatusCode != null) {
            stepStatusCode = stepStatusCode.compareTo(statusCode) > 0
                ? stepStatusCode : statusCode;
        }
        currentStep.setStepStatusCode(stepStatusCode);
        this.status = this.status.compareTo(statusCode) > 0 ? this.status : statusCode;
    }

    @Override synchronized public void onUpdate(String messageIdentifier, String prodService) {
        if (null != messageIdentifier) this.messageIdentifier = messageIdentifier;
        if (null != prodService) this.prodService = prodService;
    }

    @Override synchronized public void onError(Throwable throwable, WorkerParameters workerParameters) {
        LOGGER.error("Error in Engine", throwable);
        status = StatusCode.FATAL;

        if (!isLastStep()) {
            this.executeFinallyStep(workerParameters);
        } else {
            try {
                this.finalizeLogbook();
            } finally {
                this.persistProcessWorkflow();
            }
        }

        globalEndTime = Calendar.getInstance().getTimeInMillis();
    }

    @Override synchronized public void onComplete(ItemStatus itemStatus, WorkerParameters workerParameters) {
        if (!isLastStep()) {
            final StatusCode statusCode = itemStatus.getGlobalStatus();
            // update global status Process workFlow and process Step
            onUpdate(statusCode);

            // if the step has been defined as Blocking and stepStatus is KO or FATAL
            // then stop the process
            if (itemStatus.shallStop(currentStep.getBehavior().equals(ProcessBehavior.BLOCKING))) {
                this.executeFinallyStep(workerParameters);
            } else if (!isCompleted()) {
                if (ProcessState.COMPLETED.equals(targetState)) {
                    status = StatusCode.FATAL;
                    targetState = ProcessState.COMPLETED;
                    this.executeFinallyStep(workerParameters);

                } else if (ProcessState.PAUSE.equals(targetState)) {
                    state = ProcessState.PAUSE;
                    targetState = ProcessState.PAUSE;

                    this.persistProcessWorkflow();
                } else {
                    targetState = ProcessState.RUNNING;
                    try {
                        this.executeSteps(workerParameters);
                    } catch (ProcessingException e) {
                        LOGGER.error("ProcessEngine error > ", e);
                    }
                }
            }
        } else {
            try {
                this.finalizeLogbook();
            } finally {
                this.persistProcessWorkflow();
            }

            engineParams.clear();
            globalEndTime = Calendar.getInstance().getTimeInMillis();
        }
    }

    /**
     * Check if the state is running
     * @return true if the current state is running
     */
    protected boolean isRunning() {
        return ProcessState.RUNNING.equals(state);
    }

    /**
     * Check if the state is pause
     * @return true if the current state is pause
     */
    protected boolean isPause() {
        return ProcessState.PAUSE.equals(state);
    }

    /**
     * Check if the state is completed
     * @return true if the current state is completed
     */
    protected boolean isCompleted() {
        return ProcessState.COMPLETED.equals(state);
    }

    /**
     * Check if the current step is the last one
     * @return true if the current step is the last one
     */
    protected boolean isLastStep() {
        return stepIndex >= (stepTotal -1);
    }

    /**
     * Persist the process workflow in the workspace
     * @return true is success, false else
     */
    protected boolean persistProcessWorkflow() {
        processWorkflow.setMessageIdentifier(messageIdentifier);
        processWorkflow.setProdService(prodService);
        processWorkflow.setStepByStep(stepByStep);
        processWorkflow.setStatus(status);
        processWorkflow.setState(state);
        try {
            dataManagement.persistProcessWorkflow(String.valueOf(ServerIdentity.getInstance().getServerId()),
                operationId, processWorkflow);
            return true;
        } catch (InvalidParseOperationException | ProcessingStorageWorkspaceException e) {
            LOGGER.error("Cannot persist process workflow file, set status to FAILED, do retry ...", e);
            // Retry after 5 second
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e1) {}
            try {
                dataManagement.persistProcessWorkflow(String.valueOf(ServerIdentity.getInstance().getServerId()),
                    operationId, processWorkflow);
                return true;
            } catch (InvalidParseOperationException | ProcessingStorageWorkspaceException ex) {
                LOGGER.error("Retry > Cannot persist process workflow file, set status to FAILED", e);
                return false;
            }
        }
    }

    /**
     * Create the final logbook entry for the corresponding process workflow
     * This entry was created in ingest internal and as the process is full async we moved it to here
     */
    protected void finalizeLogbook() {

        final LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
        try {
            final GUID operationGuid = GUIDReader.getGUID(operationId);
            logbook(logbookClient, operationGuid, processWorkflow.getLogbookTypeProcess(), status);

        } catch (Exception e) {
            LOGGER.error("Error while finalize logbook of the process workflow, do retry ...", e);

            // Retry after 5 second
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e1) {}

            try {
                final GUID operationGuid = GUIDReader.getGUID(operationId);
                logbook(logbookClient, operationGuid, processWorkflow.getLogbookTypeProcess(), status);

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
            processWorkflow.setState(state);
            if (null != logbookClient) {
                logbookClient.close();
            }

            cleanWorkspace();
        }
    }


    private void cleanWorkspace() {
        try (WorkspaceClient workspaceClient = WorkspaceClientFactory.getInstance().getClient()) {
            if (workspaceClient.isExistingContainer(operationId)) {
                workspaceClient.deleteContainer(operationId, true);
            }
        } catch (Exception e) {
            LOGGER.error("Error while clear the container "+operationId+" from the workspace", e);
        }
    }

    private void logbook(LogbookOperationsClient client, GUID operationGuid, LogbookTypeProcess logbookTypeProcess, StatusCode statusCode) throws Exception {
        MessageLogbookEngineHelper messageLogbookEngineHelper = new MessageLogbookEngineHelper(logbookTypeProcess);
        final LogbookOperationParameters parameters = LogbookParametersFactory
            .newLogbookOperationParameters(
                operationGuid,
                logbookTypeProcess.name(),
                operationGuid,
                logbookTypeProcess,
                statusCode,
                messageLogbookEngineHelper.getLabelOp(logbookTypeProcess.name(), statusCode),
                operationGuid);
        parameters.putParameterValue(LogbookParameterName.outcomeDetail,
            messageLogbookEngineHelper.getOutcomeDetail(logbookTypeProcess.name(), statusCode));
        client.update(parameters);
    }
}
