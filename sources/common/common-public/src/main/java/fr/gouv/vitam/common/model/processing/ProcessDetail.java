/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.model.processing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Model for the detail of a process.
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public class ProcessDetail {

    @JsonProperty("stepStatus")
    private String stepStatus;

    @JsonProperty("previousStep")
    private String previousStep;

    @JsonProperty("nextStep")
    private String nextStep;

    @JsonProperty("operationId")
    private String operationId;

    @JsonProperty("processType")
    private String processType;

    @JsonProperty("stepByStep")
    private boolean stepByStep;

    @JsonProperty("globalState")
    private String globalState;

    @JsonProperty("processDate")
    private String processDate;

    /**
     * @return stepStatus
     */
    public String getStepStatus() {
        return stepStatus;
    }

    /**
     * @param stepStatus
     */
    public void setStepStatus(String stepStatus) {
        this.stepStatus = stepStatus;
    }

    /**
     * @return previousStep
     */
    public String getPreviousStep() {
        return previousStep;
    }

    /**
     * @param previousStep
     */
    public void setPreviousStep(String previousStep) {
        this.previousStep = previousStep;
    }

    /**
     * @return nextStep
     */
    public String getNextStep() {
        return nextStep;
    }

    /**
     * @param nextStep
     */
    public void setNextStep(String nextStep) {
        this.nextStep = nextStep;
    }

    /**
     * @return operationId
     */
    public String getOperationId() {
        return operationId;
    }

    /**
     * @param operationId
     */
    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    /**
     * @return processType
     */
    public String getProcessType() {
        return processType;
    }

    /**
     * @param processType
     */
    public void setProcessType(String processType) {
        this.processType = processType;
    }

    /**
     * @return stepByStep
     */
    public boolean isStepByStep() {
        return stepByStep;
    }

    /**
     * @param stepByStep
     */
    public void setStepByStep(boolean stepByStep) {
        this.stepByStep = stepByStep;
    }

    /**
     * @return globalState
     */
    public String getGlobalState() {
        return globalState;
    }

    /**
     * @param globalState
     */
    public void setGlobalState(String globalState) {
        this.globalState = globalState;
    }

    /**
     * @return processDate
     */
    public String getProcessDate() {
        return processDate;
    }

    /**
     * @param processDate
     */
    public void setProcessDate(String processDate) {
        this.processDate = processDate;
    }
}
