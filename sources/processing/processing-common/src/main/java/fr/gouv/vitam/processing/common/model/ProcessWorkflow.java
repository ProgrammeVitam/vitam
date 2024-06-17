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

package fr.gouv.vitam.processing.common.model;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Proccess Workflow contains a different operations and status attribute
 */
public class ProcessWorkflow {

    private static final String MANDATORY_PARAMETER = "Mandatory parameter";

    private String workflowId;

    private String operationId;

    private String contextId;

    private String applicationId;

    private String messageIdentifier;

    private String prodService;

    private List<ProcessStep> steps = new ArrayList<>();

    private String processDate = LocalDateUtil.nowFormatted();

    private LocalDateTime processCompletedDate;

    private LogbookTypeProcess logbookTypeProcess;

    private Integer tenantId;

    private StatusCode status = StatusCode.UNKNOWN;
    private ProcessState state = ProcessState.PAUSE;
    private StatusCode targetStatus;
    private volatile ProcessState targetState;

    private boolean stepByStep = false;

    /**
     * This Should be :
     * PauseRecover.RECOVER_FROM_API_PAUSE when pause origin is API
     * PauseRecover.RECOVER_FROM_SERVER_PAUSE when pause origin is SERVER
     *
     * Should be updated to PauseRecover.NO_RECOVER after the execution of the next step
     */
    private PauseRecover pauseRecover = PauseRecover.NO_RECOVER;

    private Map<String, String> parameters = new HashMap<>();

    public ProcessWorkflow() {}

    @VisibleForTesting
    public ProcessWorkflow(LogbookTypeProcess logbookTypeProcess, StatusCode status, ProcessState state) {
        this.logbookTypeProcess = logbookTypeProcess;
        this.status = status;
        this.state = state;
    }

    /**
     * Set the state of the workflow process
     *
     * @return ProcessState
     */
    public ProcessState getState() {
        return state;
    }

    /**
     * Get the state of the workflow process
     *
     * @param state
     */
    public ProcessWorkflow setState(ProcessState state) {
        if (state != null) {
            this.state = state;
        }
        return this;
    }

    public ProcessState getTargetState() {
        return targetState;
    }

    public ProcessWorkflow setTargetState(ProcessState targetState) {
        this.targetState = targetState;
        return this;
    }

    public List<ProcessStep> getSteps() {
        return steps;
    }

    public ProcessWorkflow setSteps(List<ProcessStep> steps) {
        this.steps = steps;
        return this;
    }

    /**
     * get the status of the processWorkflow
     *
     * @return StatusCode
     */
    public StatusCode getStatus() {
        return status;
    }

    /**
     * set the status of the workflow
     *
     * @param status
     * @return this
     */
    public ProcessWorkflow setStatus(StatusCode status) {
        ParametersChecker.checkParameter(MANDATORY_PARAMETER, status);
        this.status = (this.status.compareTo(status) < 0 || this.status.equals(StatusCode.FATAL))
            ? status
            : this.status;

        return this;
    }

    public StatusCode getTargetStatus() {
        return targetStatus;
    }

    public ProcessWorkflow setTargetStatus(StatusCode targetStatus) {
        this.targetStatus = targetStatus;
        return this;
    }

    public boolean isStepByStep() {
        return stepByStep;
    }

    public ProcessWorkflow setStepByStep(boolean stepByStep) {
        this.stepByStep = stepByStep;
        return this;
    }

    /**
     * @return the processDate
     */
    public String getProcessDate() {
        return processDate;
    }

    /**
     * @param processDate the processDate to set
     * @return this
     */
    public ProcessWorkflow setProcessDate(String processDate) {
        this.processDate = processDate;
        return this;
    }

    /**
     * @return the operationId
     */

    public String getOperationId() {
        return operationId;
    }

    /**
     * @param operationId the operationId to set
     * @return this
     */

    public ProcessWorkflow setOperationId(String operationId) {
        this.operationId = operationId;
        return this;
    }

    /**
     * @return the messageIdentifier
     */
    public String getMessageIdentifier() {
        return messageIdentifier;
    }

    /**
     * @param messageIdentifier the messageIdentifier to set
     * @return this
     */
    public ProcessWorkflow setMessageIdentifier(String messageIdentifier) {
        this.messageIdentifier = messageIdentifier;
        return this;
    }

    /**
     * @return the prodService
     */

    public String getProdService() {
        return prodService;
    }

    /**
     * @param prodService the prodService to set
     * @return this
     */
    public ProcessWorkflow setProdService(String prodService) {
        this.prodService = prodService;
        return this;
    }

    /**
     * @return the logbookTypeProcess
     */
    public LogbookTypeProcess getLogbookTypeProcess() {
        return logbookTypeProcess;
    }

    /**
     * @param logbookTypeProcess the logbookTypeProcess
     * @return this
     */
    public ProcessWorkflow setLogbookTypeProcess(LogbookTypeProcess logbookTypeProcess) {
        this.logbookTypeProcess = logbookTypeProcess;
        return this;
    }

    /**
     * @return the tenant
     */
    public Integer getTenantId() {
        return tenantId;
    }

    /**
     * @param tenantId to set
     * @return this
     */
    public ProcessWorkflow setTenantId(Integer tenantId) {
        this.tenantId = tenantId;
        return this;
    }

    /**
     * @return the workflow IDENTIFIER
     */
    public String getWorkflowId() {
        return workflowId;
    }

    /**
     * @param workflowId the workflow IDENTIFIER
     * @return current instance
     */
    public ProcessWorkflow setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
        return this;
    }

    /**
     * @return The context id
     */
    public String getContextId() {
        return contextId;
    }

    /**
     * @param contextId the context ID
     * @return current instance
     */
    public ProcessWorkflow setContextId(String contextId) {
        this.contextId = contextId;
        return this;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public ProcessWorkflow setApplicationId(String applicationId) {
        this.applicationId = applicationId;
        return this;
    }

    /*
     * Complete date
     * @return
     */
    public LocalDateTime getProcessCompletedDate() {
        return processCompletedDate;
    }

    public ProcessWorkflow setProcessCompletedDate(LocalDateTime processCompletedDate) {
        this.processCompletedDate = processCompletedDate;
        return this;
    }

    public PauseRecover getPauseRecover() {
        return pauseRecover;
    }

    public ProcessWorkflow setPauseRecover(PauseRecover pauseRecover) {
        this.pauseRecover = pauseRecover;
        return this;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public ProcessWorkflow setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
        return this;
    }
}
