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
package fr.gouv.vitam.processing.data.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.exception.WorkflowNotFoundException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.ProcessExecutionStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.exception.StepsNotFoundException;
import fr.gouv.vitam.processing.common.model.ProcessBehavior;
import fr.gouv.vitam.processing.common.model.ProcessStep;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.processing.common.model.Step;
import fr.gouv.vitam.processing.common.model.WorkFlow;

/**
 * ProcessMonitoringImpl class implementing the ProcessMonitoring and using a concurrent HashMap to persist objects
 */
public class ProcessDataAccessImpl implements ProcessDataAccess {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProcessDataAccessImpl.class);

    private static final int DEFAULT_MAP_SIZE = 50;

    private static final String AUTHORIZATION_ACTION_CANCEL = "authorization action : CANCEL";

    private static final Map<Integer, Map<String, ProcessWorkflow>> WORKFLOWS_LIST = new ConcurrentHashMap<>();

    private static final ProcessDataAccessImpl INSTANCE = new ProcessDataAccessImpl();

    private static final String PROCESS_DOES_NOT_EXIST = "Process does not exist";
    private static final String STEP_DOES_NOT_EXIST = "Step does not exist";
    private static final String UNAUTHORIZED_ACTION = "Unauthorized action for current status :";

    private ProcessDataAccessImpl() {
        // doNothing
    }


    /**
     *
     * Get the Process Monitoring instance
     *
     * @return the ProcessMonitoring instance
     */
    public static ProcessDataAccessImpl getInstance() {
        return INSTANCE;
    }

    @Override
    public ProcessWorkflow initProcessWorkflow(WorkFlow workflow, String containerName, ProcessAction executionMode,
        LogbookTypeProcess logbookTypeProcess, Integer tenantId) {
        ParametersChecker.checkParameter("containerName is a mandatory parameter", containerName);
        ProcessWorkflow processWorkflow = new ProcessWorkflow();
        processWorkflow.setExecutionMode(executionMode);
        processWorkflow.setLogbookTypeProcess(logbookTypeProcess);
        processWorkflow.setOperationId(containerName);
        processWorkflow.setTenantId(tenantId);
        final Map<String, ProcessStep> orderedProcessSteps;
        if (workflow != null) {
            orderedProcessSteps = new LinkedHashMap<>();
            String uniqueId;
            int iterator = 0;
            for (final Step step : workflow.getSteps()) {
                uniqueId = containerName + "_" + workflow.getId() + "_" + iterator + "_" + step.getStepName();
                final ProcessStep processStep =
                    new ProcessStep(step, uniqueId, containerName, workflow.getId(), iterator, 0, 0);
                orderedProcessSteps.put(uniqueId, processStep);
                iterator++;
            }
            processWorkflow.setOrderedProcessStep(orderedProcessSteps);
        }
        addWorkflow(containerName, tenantId, processWorkflow);
        return processWorkflow;
    }

    @Override
    public void updateStep(String operationId, String uniqueStepId, long elementToProcess, boolean elementProcessed,
        Integer tenantId)
        throws ProcessingException {
        final Map<String, ProcessStep> orderedSteps = getWorkflowProcessSteps(operationId, tenantId);
        final ProcessStep step = orderedSteps.get(uniqueStepId);
        if (elementProcessed) {
            step.setElementProcessed(step.getElementProcessed() + 1);
        } else {
            step.setElementToProcess(elementToProcess);
        }
        orderedSteps.put(uniqueStepId, step);

        addProcessSteps(operationId, orderedSteps, tenantId);
    }



    @Override
    public Map<String, ProcessStep> getWorkflowProcessSteps(String operationId, Integer tenantId)
        throws StepsNotFoundException, WorkflowNotFoundException {
        ParametersChecker.checkParameter("processId is a mandatory parameter", operationId);
        ProcessWorkflow processWorkflow = getProcessWorkflow(operationId, tenantId);
        Map<String, ProcessStep> orderedSteps = processWorkflow.getOrderedProcessStep();
        if (orderedSteps == null || orderedSteps.isEmpty()) {
            throw new StepsNotFoundException(STEP_DOES_NOT_EXIST);
        }
        return orderedSteps;
    }



    @Override
    public ProcessStep nextStep(String operationId, Integer tenantId)
        throws StepsNotFoundException, WorkflowNotFoundException {
        ParametersChecker.checkParameter("processId is a mandatory parameter", operationId);
        ProcessWorkflow processWorkflow = getProcessWorkflow(operationId, tenantId);
        ProcessStep processStep = null;

        // if workflow canceled or finished
        if (processWorkflow.getExecutionStatus().ordinal() >= ProcessExecutionStatus.PAUSE.ordinal()) {
            return processStep;
        }
        Map<String, ProcessStep> orderedSteps = processWorkflow.getOrderedProcessStep();

        switch (processWorkflow.getExecutionMode()) {
            case NEXT:
                processStep = getFirstStepToBeExecuted(orderedSteps);
                if (processStep != null) {
                    processWorkflow.setExecutionStatus(ProcessExecutionStatus.PAUSE);
                }
                break;

            case RESUME:
                processStep = getFirstStepToBeExecuted(orderedSteps);
                processWorkflow.setExecutionStatus(ProcessExecutionStatus.RUNNING);
                break;

            default:
                break;
        }
        // null is permitted
        return processStep;
    }



    private ProcessStep getFirstStepToBeExecuted(
        Map<String, ProcessStep> orderedSteps) throws StepsNotFoundException {
        for (final Map.Entry<String, ProcessStep> entry : orderedSteps.entrySet()) {
            final ProcessStep step = entry.getValue();
            if (step.getStepStatusCode() == StatusCode.UNKNOWN && !ProcessBehavior.FINALLY.equals(step.getBehavior())) {
                return step;
            }
        }
        return null;
    }


    @Override
    public StatusCode getFinalWorkflowStatus(String processId, Integer tenantId) throws ProcessingException {
        ParametersChecker.checkParameter("processId is a mandatory parameter", processId);
        if (WORKFLOWS_LIST.containsKey(processId)) {
            StatusCode finalCode = StatusCode.UNKNOWN;
            final Map<String, ProcessStep> orderedSteps = getWorkflowProcessSteps(processId, tenantId);
            for (final ProcessStep step : orderedSteps.values()) {
                if (step != null) {
                    final StatusCode stepStatus = step.getStepStatusCode();
                    if (stepStatus != null) {
                        finalCode = finalCode.compareTo(stepStatus) < 0 ? stepStatus : finalCode;
                    }
                }
            }
            return finalCode;
        } else {
            LOGGER.error(PROCESS_DOES_NOT_EXIST);
            throw new ProcessingException(PROCESS_DOES_NOT_EXIST);
        }
    }


    @Override
    public void updateStepStatus(String processId, String uniqueId, StatusCode status, Integer tenantId)
        throws StepsNotFoundException, WorkflowNotFoundException {
        ParametersChecker.checkParameter("processId is a mandatory parameter", processId);
        ParametersChecker.checkParameter("uniqueId is a mandatory parameter", uniqueId);
        ParametersChecker.checkParameter("status is a mandatory parameter", status);
        final Map<String, ProcessStep> orderedSteps = getWorkflowProcessSteps(processId, tenantId);
        if (orderedSteps.containsKey(uniqueId)) {
            final ProcessStep step = orderedSteps.get(uniqueId);
            StatusCode stepStatusCode = step.getStepStatusCode();
            if (stepStatusCode != null) {
                stepStatusCode = stepStatusCode.compareTo(status) > 0
                    ? stepStatusCode : status;
            }
            step.setStepStatusCode(stepStatusCode);
            orderedSteps.put(uniqueId, step);
            addProcessSteps(processId, orderedSteps, tenantId);
            getProcessWorkflow(processId, tenantId).setGlobalStatusCode(status);
        } else {
            throw new StepsNotFoundException(STEP_DOES_NOT_EXIST + " /id: " + uniqueId);
        }
    }

    @Override
    public ProcessWorkflow cancelProcessWorkflow(String operationId, Integer tenantId)
        throws WorkflowNotFoundException, ProcessingException {

        ProcessWorkflow processWorkflow = getProcessWorkflow(operationId, tenantId);
        // if workflow canceled or finished
        if (processWorkflow.getExecutionStatus().ordinal() > ProcessExecutionStatus.CANCELLED.ordinal()) {
            throw new ProcessingException(AUTHORIZATION_ACTION_CANCEL);
        }
        return processWorkflow.setExecutionStatus(ProcessExecutionStatus.CANCELLED);
    }


    @Override
    public void clear(StatusCode statusCode, int number, Integer tenantId) {

        // TODO fix later
        // ProcessWorkflow processWorkflow;
        // for (Map<String, ProcessWorkflow> entry : WORKFLOWS_LIST.values()) {
        // if ((processWorkflow = entry.getValue()) != null) {
        // if (processWorkflow.getGlobalStatusCode() != null &&
        // processWorkflow.getGlobalStatusCode().compareTo(statusCode) == 1) {
        // WORKFLOWS_LIST.remove(entry.getKey());
        // LOGGER.info("Deleted process workflow : " + entry.getKey());
        // number--;
        // }
        // }
        // if (number == 0) {
        // break;
        // }
        //
        // }
    }

    @Override
    public ProcessWorkflow getProcessWorkflow(String processId, Integer tenantId) throws WorkflowNotFoundException {
        ParametersChecker.checkParameter("processId is a mandatory parameter", processId);
        ParametersChecker.checkParameter("tenantId is a mandatory parameter", tenantId);

        if (!WORKFLOWS_LIST.containsKey(tenantId) || WORKFLOWS_LIST.get(tenantId) == null ||
            !WORKFLOWS_LIST.get(tenantId).containsKey(processId)) {
            throw new WorkflowNotFoundException(PROCESS_DOES_NOT_EXIST);
        } else {
            return WORKFLOWS_LIST.get(tenantId).get(processId);
        }
    }

    /**
     * @param String processId
     * @param orderedWorkflow
     */
    private void addWorkflow(String processId, Integer tenantId, final ProcessWorkflow processWorkflow) {
        ParametersChecker.checkParameter("processId is a mandatory parameter", processId);
        // check maps
        // if (WORKFLOWS_LIST.get(tenantId).size() > DEFAULT_MAP_SIZE) {
        // clear(StatusCode.FATAL, 10, tenantId);
        // if (WORKFLOWS_LIST.size() > DEFAULT_MAP_SIZE) {
        // clear(StatusCode.KO, 10);
        // clear(StatusCode.OK, 5);
        // }
        // }
        // Need requestId
        if (!WORKFLOWS_LIST.containsKey(tenantId) ||  WORKFLOWS_LIST.get(tenantId)==null) {
            Map<String, ProcessWorkflow> operationsByTenant = new ConcurrentHashMap<>();
            operationsByTenant.put(processId, processWorkflow);
            WORKFLOWS_LIST.put(tenantId, operationsByTenant);
        } else {
            WORKFLOWS_LIST.get(tenantId).put(processId, processWorkflow);
        }
    }

    private void addProcessSteps(String processId, Map<String, ProcessStep> orderedProcessSteps, Integer tenantId)
        throws WorkflowNotFoundException {
        ParametersChecker.checkParameter("processId is a mandatory parameter", processId);
        getProcessWorkflow(processId, tenantId).setOrderedProcessStep(orderedProcessSteps);
    }


    @Override
    public String getWorkflowIdByProcessId(String processId, Integer tenantId) throws WorkflowNotFoundException {
        return getProcessWorkflow(processId, tenantId).getOperationId();
    }


    @Override
    public void updateProcessExecutionStatus(String processId, ProcessExecutionStatus executionStatus, Integer tenantId)
        throws WorkflowNotFoundException {
        getProcessWorkflow(processId, tenantId).setExecutionStatus(executionStatus);
    }


    @Override
    public ProcessStep getFinallyStep(String operationId, Integer tenantId)
        throws StepsNotFoundException, WorkflowNotFoundException {
        ParametersChecker.checkParameter("processId is a mandatory parameter", operationId);
        Map<String, ProcessStep> workflowProcessSteps = getWorkflowProcessSteps(operationId, tenantId);
        final String theLastKey = new ArrayList<>(workflowProcessSteps.keySet()).get(workflowProcessSteps.size() - 1);
        final ProcessStep lastStep = workflowProcessSteps.get(theLastKey);

        // check if it's a final step and execution status
        if (ProcessBehavior.FINALLY.equals(lastStep.getBehavior()) &&
            !ProcessExecutionStatus.PAUSE.equals(getProcessExecutionStatus(operationId, tenantId)) &&
            !ProcessExecutionStatus.CANCELLED.equals(getProcessExecutionStatus(operationId, tenantId))) {
            return lastStep;
        }
        return null;
    }


    @Override
    public ProcessExecutionStatus getProcessExecutionStatus(String id, Integer tenantId) throws WorkflowNotFoundException {
        return getProcessWorkflow(id, tenantId).getExecutionStatus();
    }

    @Override
    public List<ProcessWorkflow> getAllWorkflowProcess(Integer tenantId) {
        if (WORKFLOWS_LIST != null && WORKFLOWS_LIST.containsKey(tenantId)) {
            return new ArrayList<ProcessWorkflow>(WORKFLOWS_LIST.get(tenantId).values());
        }
        return new ArrayList<ProcessWorkflow>(0);
    }


    @Override
    public void prepareToRelaunch(String operationId, ProcessAction executionMode, Integer tenantId)
        throws ProcessingException {
        ProcessWorkflow processWorkflow = getProcessWorkflow(operationId, tenantId);
        // check current status (action not allowed for canceled , failed, completed process)
        if (processWorkflow.getExecutionStatus().ordinal() > ProcessExecutionStatus.PAUSE.ordinal()) {
            throw new ProcessingException(UNAUTHORIZED_ACTION + processWorkflow.getExecutionStatus());
        }
        processWorkflow.setExecutionStatus(ProcessExecutionStatus.RUNNING)
            .setExecutionMode(executionMode);
    }


    @Override
    public void updateMessageIdentifier(String operationId, String messageIdentifier, Integer tenantId) {
        ParametersChecker.checkParameter("Operation must be not null", operationId);
        ParametersChecker.checkParameter("messageIdentifier must be not null", messageIdentifier);
        ParametersChecker.checkParameter("tenantId must be not null", tenantId);
        getProcessWorkflow(operationId, tenantId).setMessageIdentifier(messageIdentifier);
    }


    @Override
    public String getMessageIdentifierByOperationId(String operationId, Integer tenantId) {
        ParametersChecker.checkParameter("Operationid must be not null", operationId);
        ParametersChecker.checkParameter("tenantId must be not null", tenantId);
        return getProcessWorkflow(operationId, tenantId).getMessageIdentifier();
    }
}
