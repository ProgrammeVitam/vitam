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
package fr.gouv.vitam.processing.common.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.model.StatusCode;

/**
 * Step Object in process workflow
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProcessStep extends Step {
    private String id;
    private long elementProcessed;
    private long elementToProcess;
    private StatusCode stepStatusCode = StatusCode.UNKNOWN;

    
    /**
     * Constructor to initalize a Process Step with a Step object
     *
     * @param step the Step object
     * @param containerName the container name concerned by the process
     * @param workflowId the workflow ID concerned by the process
     * @param position the position of the step
     * @param elementToProcess number of element to process
     * @param elementProcessed number of element processed
     * @throws IllegalArgumentException if the step is null
     */
    public ProcessStep(Step step, long elementToProcess, long
        elementProcessed) {
        ParametersChecker.checkParameter("Step could not be null", step);
        this.setActions(step.getActions());
        this.setDistribution(step.getDistribution());
        this.setStepName(step.getStepName());
        this.setBehavior(step.getBehavior());
        this.setWorkerGroupId(step.getWorkerGroupId());
        this.elementProcessed = elementProcessed;
        this.elementToProcess = elementToProcess;
    }

    /**
     * Constructor to initalize a Process Step with a Step object
     *
     * @param step the Step object
     * @param containerName the container name concerned by the process
     * @param workflowId the workflow ID concerned by the process
     * @param position the position of the step
     * @param elementToProcess number of element to process
     * @param elementProcessed number of element processed
     * @throws IllegalArgumentException if the step is null
     */
    public ProcessStep(Step step, String containerName, String workflowId, int position, long elementToProcess, long
        elementProcessed) {
        ParametersChecker.checkParameter("containerName could not be null", containerName);
        ParametersChecker.checkParameter("workflowId could not be null", workflowId);
        ParametersChecker.checkParameter("position could not be null", position);
        ParametersChecker.checkParameter("Step could not be null", step);
        this.id = containerName + "_" + workflowId + "_" + position + "_" + step.getStepName();
        this.setActions(step.getActions());
        this.setDistribution(step.getDistribution());
        this.setStepName(step.getStepName());
        this.setBehavior(step.getBehavior());
        this.setWorkerGroupId(step.getWorkerGroupId());
        this.elementProcessed = elementProcessed;
        this.elementToProcess = elementToProcess;
    }


    // Used for tests
    ProcessStep() {

    }

    /**
     * @return the elementProcessed
     */
    public long getElementProcessed() {
        return elementProcessed;
    }

    /**
     * @param elementProcessed the elementProcessed to set
     *
     * @return the updated ProcessStep object
     */
    public ProcessStep setElementProcessed(long elementProcessed) {
        this.elementProcessed = elementProcessed;
        return this;
    }

    /**
     * @return the elementToProcess
     */
    public long getElementToProcess() {
        return elementToProcess;
    }

    /**
     * @param elementToProcess the elementToProcess to set
     *
     * @return the updated ProcessStep object
     */
    public ProcessStep setElementToProcess(long elementToProcess) {
        this.elementToProcess = elementToProcess;
        return this;
    }

    /**
     * @return the stepStatusCode
     */
    public StatusCode getStepStatusCode() {
        return stepStatusCode;
    }

    /**
     * @param stepStatusCode the stepStatusCode to set
     *
     * @return the updated ProcessStep object
     */
    public ProcessStep setStepStatusCode(StatusCode stepStatusCode) {
        this.stepStatusCode = stepStatusCode;
        return this;
    }

    /**
     * @return process unique ID
     */
    public String getId() {
        return id;
    }

    /**
     * {@inheritDoc}
     *
     * Considered equal two ProcessStep with the same id, step name and worker group id.
     */
    @Override
    public boolean equals(Object object) {
        if (object instanceof ProcessStep) {
            ProcessStep processStep = (ProcessStep) object;
            return this.id.equals(processStep.getId()) && this.getStepName().equals(processStep.getStepName()) &&
                this.getWorkerGroupId().equals(processStep.getWorkerGroupId());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id, this.getStepName(), this.getWorkerGroupId());
    }
}
