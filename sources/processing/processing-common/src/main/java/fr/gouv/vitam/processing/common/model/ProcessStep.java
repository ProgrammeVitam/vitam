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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.Step;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Step Object in process workflow
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProcessStep extends Step {

    private AtomicLong elementProcessed = new AtomicLong(0);
    private AtomicLong elementToProcess = new AtomicLong(0);
    private StatusCode stepStatusCode = StatusCode.UNKNOWN;

    @JsonIgnore
    private boolean lastStep = false;

    @VisibleForTesting
    public ProcessStep(Step step, AtomicLong elementToProcess, AtomicLong elementProcessed, String id) {
        this(step, elementToProcess, elementProcessed);
        setId(id);
    }

    /**
     * Constructor to initialize a Process Step with a Step object
     *
     * @param step the Step object
     * @param elementToProcess number of element to process
     * @param elementProcessed number of element processed
     * @throws IllegalArgumentException if the step is null
     */
    public ProcessStep(Step step, AtomicLong elementToProcess, AtomicLong elementProcessed) {
        ParametersChecker.checkParameter("Step could not be null", step);
        setActions(step.getActions());
        setDistribution(step.getDistribution());
        setStepName(step.getStepName());
        setBehavior(step.getBehavior());
        setWorkerGroupId(step.getWorkerGroupId());
        setId(step.getId());
        this.elementProcessed = elementProcessed;
        this.elementToProcess = elementToProcess;
    }

    @VisibleForTesting
    public ProcessStep(
        Step step,
        String id,
        String containerName,
        String workflowId,
        int position,
        AtomicLong elementToProcess,
        AtomicLong elementProcessed
    ) {
        this(step, containerName, workflowId, position, elementToProcess, elementProcessed);
        setId(id);
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
    public ProcessStep(
        Step step,
        String containerName,
        String workflowId,
        int position,
        AtomicLong elementToProcess,
        AtomicLong elementProcessed
    ) {
        ParametersChecker.checkParameter("containerName could not be null", containerName);
        ParametersChecker.checkParameter("workflowId could not be null", workflowId);
        ParametersChecker.checkParameter("position could not be null", position);
        ParametersChecker.checkParameter("Step could not be null", step);
        setId(step.getId());
        setActions(step.getActions());
        setDistribution(step.getDistribution());
        setStepName(step.getStepName());
        setBehavior(step.getBehavior());
        setWorkerGroupId(step.getWorkerGroupId());
        setWaitFor(step.getWaitFor());
        this.elementProcessed = elementProcessed;
        this.elementToProcess = elementToProcess;
    }

    // Used for tests
    ProcessStep() {}

    @JsonIgnore
    public boolean isBlockingKO() {
        return isBlocking() && StatusCode.KO.equals(getStepStatusCode());
    }

    /**
     * @return the elementProcessed
     */
    @VisibleForTesting
    public AtomicLong getElementProcessed() {
        return elementProcessed;
    }

    /**
     * @param elementProcessed the elementProcessed to set
     * @return the updated ProcessStep object
     */
    public ProcessStep setElementProcessed(AtomicLong elementProcessed) {
        this.elementProcessed = elementProcessed;
        return this;
    }

    /**
     * @return the elementToProcess
     */
    @VisibleForTesting
    public AtomicLong getElementToProcess() {
        return elementToProcess;
    }

    /**
     * @param elementToProcess the elementToProcess to set
     * @return the updated ProcessStep object
     */
    public ProcessStep setElementToProcess(AtomicLong elementToProcess) {
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
     * @return the updated ProcessStep object
     */
    public ProcessStep setStepStatusCode(StatusCode stepStatusCode) {
        this.stepStatusCode = stepStatusCode;
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * Considered equal two ProcessStep with the same id, step name and worker group id.
     */
    @Override
    public boolean equals(Object object) {
        if (object instanceof ProcessStep) {
            final ProcessStep processStep = (ProcessStep) object;
            return (
                getId().equals(processStep.getId()) &&
                getStepName().equals(processStep.getStepName()) &&
                getWorkerGroupId().equals(processStep.getWorkerGroupId())
            );
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getStepName(), getWorkerGroupId());
    }

    @Override
    public String toString() {
        return (
            "" +
            this.getStepName() +
            " " +
            this.getActions() +
            " " +
            this.getDistribution().getKind() +
            " " +
            this.getDistribution().getElement() +
            " " +
            this.getId()
        );
    }

    public void setLastStep(boolean lastStep) {
        this.lastStep = lastStep;
    }

    public boolean getLastStep() {
        return lastStep;
    }
}
