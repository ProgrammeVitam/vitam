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

import fr.gouv.vitam.common.ParametersChecker;

/**
 * Step Object in process workflow
 */

public class ProcessStep extends Step {

    private long elementProcessed;
    private long elementToProcess;
    private StatusCode stepStatusCode;
    

    /**
     * Constructor to initalize a Process Step with a Step object
     * 
     * @param step the Step object 
     * @param elementToProcess number of element to process
     * @param elementProcessed number of element processed
     * @throws IllegalArgumentException if the step is null
     */
    public ProcessStep(Step step, long elementToProcess, long elementProcessed) throws IllegalArgumentException {
        ParametersChecker.checkParameter("Step could not be null", step);
        this.setActions(step.getActions());
        this.setDistribution(step.getDistribution());
        this.setStepName(step.getStepName());
        this.setBehavior(step.getBehavior());
        this.setWorkerGroupId(step.getWorkerGroupId());
        this.elementProcessed = elementProcessed;
        this.elementToProcess = elementToProcess;
        
    }
    

    //Used for tests
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

}
