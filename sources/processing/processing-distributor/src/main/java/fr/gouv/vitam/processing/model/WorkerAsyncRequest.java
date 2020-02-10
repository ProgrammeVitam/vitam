/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.processing.model;

import java.util.Set;
import java.util.concurrent.Semaphore;

import fr.gouv.vitam.common.model.VitamSession;
import fr.gouv.vitam.processing.distributor.api.Callbackable;
import fr.gouv.vitam.worker.common.DescriptionStep;

/**
 * Class for manage the launch of an "async" request
 */
public class WorkerAsyncRequest {
    // Argument of the call
    private final DescriptionStep descriptionStep;
    // Queue (family) to use
    private final String queueID;
    // Object for callback
    private final Callbackable<WorkerAsyncResponse> callingProcess;
    private final Thread callerThread;
    private final VitamSession session;
    private final Set<WorkerAsyncRequest> currentRunningObjectsInStep;
    private final Semaphore waitingStepAllAsyncRequest;
    
    
    /**
     * Default constructor
     * 
     * @param descriptionStep the step description
     * @param callingProcess the callback
     * @param currentRunningObjectsInStep the current object in step
     * @param queueId the queue id
     * @param waitingStepAllAsyncRequest the waiting step
     * @param session the session id
     */
    
    public WorkerAsyncRequest(DescriptionStep descriptionStep, Callbackable<WorkerAsyncResponse> callingProcess,
        Set<WorkerAsyncRequest> currentRunningObjectsInStep, String queueId,Semaphore waitingStepAllAsyncRequest,VitamSession session) {
        this.descriptionStep = descriptionStep;
        this.callingProcess = callingProcess;
        this.callerThread = Thread.currentThread();
        this.currentRunningObjectsInStep = currentRunningObjectsInStep;
        this.queueID = queueId;
        this.waitingStepAllAsyncRequest = waitingStepAllAsyncRequest;
        this.session = session;
    }

    /**
     * Arguments of the call
     * 
     * @return the descriptionStep
     */
    public DescriptionStep getDescriptionStep() {
        return descriptionStep;
    }

    /**
     * call the callback at the end of the process
     * 
     * @param workerAsyncResponse of type {@link WorkerAsyncResponse}
     */
    public void callCallback(WorkerAsyncResponse workerAsyncResponse) {
        callingProcess.callbackResponse(workerAsyncResponse);
    }

    /**
     * 
     * @return the Thread of the callerThread (ex: to interrupt it)
     */
    public Thread getCallerThread() {
        return callerThread;
    }

    /**
     * @return the currentRunningObjectsInStep
     */
    public Set<WorkerAsyncRequest> getCurrentRunningObjectsInStep() {
        return currentRunningObjectsInStep;
    }

    /**
     * @return the queueID
     */
    public String getQueueID() {
        return queueID;
    }

    /**
     * @return the waitingStepAllAsyncRequest
     */
    public Semaphore getWaitingStepAllAsyncRequest() {
        return waitingStepAllAsyncRequest;
    }

    /**
     * @return the session
     */
    public VitamSession getSession() {
        return session;
    }

}
