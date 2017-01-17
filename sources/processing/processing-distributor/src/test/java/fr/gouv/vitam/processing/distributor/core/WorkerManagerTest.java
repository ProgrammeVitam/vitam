/**
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
 */
package fr.gouv.vitam.processing.distributor.core;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;

import javax.validation.constraints.NotNull;

import org.junit.Test;

import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.VitamSession;
import fr.gouv.vitam.common.thread.VitamThreadFactory.VitamThread;
import fr.gouv.vitam.processing.common.exception.ProcessingBadRequestException;
import fr.gouv.vitam.processing.common.exception.WorkerAlreadyExistsException;
import fr.gouv.vitam.processing.common.exception.WorkerFamilyNotFoundException;
import fr.gouv.vitam.processing.common.exception.WorkerNotFoundException;
import fr.gouv.vitam.processing.common.model.Action;
import fr.gouv.vitam.processing.common.model.ActionDefinition;
import fr.gouv.vitam.processing.common.model.ProcessBehavior;
import fr.gouv.vitam.processing.common.model.Step;
import fr.gouv.vitam.processing.common.parameter.DefaultWorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.processing.model.WorkerAsyncRequest;
import fr.gouv.vitam.worker.common.DescriptionStep;

/**
 * 
 */
public class WorkerManagerTest {

    private static final String WORKER_DESCRIPTION =
        "{ \"name\" : \"workername\", \"family\" : \"DefaultWorker\", \"capacity\" : 10, \"storage\" : 100," +
            "\"status\" : \"Active\", \"configuration\" : {\"serverHost\" : \"localhost\", \"serverPort\" : \"12345\" } }";


    private static final String BIG_WORKER_DESCRIPTION =
        "{ \"name\" : \"workername2\", \"family\" : \"BigWorker\", \"capacity\" : 10, \"storage\" : 100," +
            "\"status\" : \"Active\", \"configuration\" : {\"serverHost\" : \"localhost\", \"serverPort\" : \"12345\" } }";
    
    @Test
    public void givenBigWorkerFamilyAndStepOfBigWorkflowRunningOn() throws Exception {
        final String familyId = "BigWorker";
        final String workerId = "NewWorkerId2";
        RunnableMock rm = new RunnableMock();
        VitamThread vt = new VitamThread(rm, ThreadLocalRandom.current().nextLong(1000));
        VitamSessionMock vsm = new VitamSessionMock(vt);
        DefaultWorkerParameters params = WorkerParametersFactory.newWorkerParameters();
        params.setWorkerGUID(GUIDFactory.newGUID());
        final List<Step> steps = new ArrayList<>();
        final Step step = new Step().setStepName("TEST").setWorkerGroupId(familyId);
        final List<Action> actions = new ArrayList<>();
        final Action action = new Action();

        
        action.setActionDefinition(
            new ActionDefinition().setActionKey("DummyHandler").setBehavior(ProcessBehavior.NOBLOCKING));
        actions.add(action);
        step.setBehavior(ProcessBehavior.NOBLOCKING).setActions(actions);
        ProcessDistributorImpl processDistributor = ProcessDistributorImplFactory.getDefaultDistributor();
        DescriptionStep descriptionStep =
            new DescriptionStep(step, params);
        Semaphore waitingStepAllAsyncRequest = new Semaphore(1);
        WorkerAsyncRequest workerAsyncRequest =
            new WorkerAsyncRequest(descriptionStep, processDistributor, new HashSet<>(), step.getWorkerGroupId(),waitingStepAllAsyncRequest,vsm);
        WorkerManager.registerWorker(familyId, workerId, BIG_WORKER_DESCRIPTION);        
        WorkerManager.submitJob(workerAsyncRequest);
    }

    @Test
    public void givenCorrectQueueAndCorrectAsyncWhenSubmitJobThenOK() throws Exception {
        RunnableMock rm = new RunnableMock();
        VitamThread vt = new VitamThread(rm, ThreadLocalRandom.current().nextLong(1000));
        VitamSessionMock vsm = new VitamSessionMock(vt);
        DefaultWorkerParameters params = WorkerParametersFactory.newWorkerParameters();
        params.setWorkerGUID(GUIDFactory.newGUID());
        final List<Step> steps = new ArrayList<>();
        final Step step = new Step().setStepName("TEST");
        final List<Action> actions = new ArrayList<>();
        final Action action = new Action();
        final String familyId = "DefaultWorker";
        final String workerId = "NewWorkerId";
        
        action.setActionDefinition(
            new ActionDefinition().setActionKey("DummyHandler").setBehavior(ProcessBehavior.NOBLOCKING));
        actions.add(action);
        step.setBehavior(ProcessBehavior.NOBLOCKING).setActions(actions);
        ProcessDistributorImpl processDistributor = ProcessDistributorImplFactory.getDefaultDistributor();
        DescriptionStep descriptionStep =
            new DescriptionStep(step, params);
        Semaphore waitingStepAllAsyncRequest = new Semaphore(1);
        WorkerAsyncRequest workerAsyncRequest =
            new WorkerAsyncRequest(descriptionStep, processDistributor, new HashSet<>(), step.getWorkerGroupId(),waitingStepAllAsyncRequest,vsm);
        WorkerManager.registerWorker(familyId, workerId, WORKER_DESCRIPTION);
        
        WorkerManager.submitJob(workerAsyncRequest);
    }

    @Test(expected = ProcessingBadRequestException.class)
    public void givenInCorrectQueueAndCorrectAsyncWhenSubmitJobThenOK() throws Exception {
        RunnableMock rm = new RunnableMock();
        VitamThread vt = new VitamThread(rm, ThreadLocalRandom.current().nextLong(1000));
        VitamSessionMock vsm = new VitamSessionMock(vt);
        ProcessDistributorImpl processDistributor = ProcessDistributorImplFactory.getDefaultDistributor();
        DescriptionStep descriptionStep =
            new DescriptionStep(new Step(), WorkerParametersFactory.newWorkerParameters());
        Semaphore waitingStepAllAsyncRequest = new Semaphore(1);
        WorkerAsyncRequest workerAsyncRequest =
            new WorkerAsyncRequest(descriptionStep, processDistributor, new HashSet<>(), "FAKE_QUEUE",waitingStepAllAsyncRequest,vsm);
        WorkerManager.submitJob(workerAsyncRequest);
    }

    @Test
    public void givenProcessDistributorWhenRegisterWorkerThenOK() throws Exception {
        final String familyId = "DefaultWorker";
        final String workerId = "NewWorkerId" + GUIDFactory.newGUID().getId();
        WorkerManager.registerWorker(familyId, workerId, WORKER_DESCRIPTION);
        assertTrue(WorkerManager.getWorkersList().size() > 0);
    }

    @Test(expected = WorkerAlreadyExistsException.class)
    public void givenProcessDistributorWhenRegisterExistingWorkerThenProcessingException() throws Exception {
        final String familyId = "DefaultWorker";
        final String workerId = "NewWorkerId1"+ GUIDFactory.newGUID().getId();;
        WorkerManager.registerWorker(familyId, workerId, WORKER_DESCRIPTION);
        WorkerManager.registerWorker(familyId, workerId, WORKER_DESCRIPTION);
    }


    @Test
    public void givenProcessDistributorWhenUnRegisterExistingWorkerThenOK() throws Exception {
        final String familyId = "DefaultWorker";
        final String workerId = "NewWorkerId2"+ GUIDFactory.newGUID().getId();
        WorkerManager.registerWorker(familyId, workerId, WORKER_DESCRIPTION);
        final int sizeBefore = WorkerManager.getWorkersList().get(familyId).size();
        WorkerManager.unregisterWorker(familyId, workerId);
        final int sizeAfter = WorkerManager.getWorkersList().get(familyId).size();
        assertTrue(sizeBefore > sizeAfter);
    }

    @Test(expected = WorkerFamilyNotFoundException.class)
    public void givenProcessDistributorWhenUnRegisterNonExistingFamilyThenProcessingException() throws Exception {
        final String familyId = "NewFamilyId" + GUIDFactory.newGUID().getId();
        final String workerId = "NewWorkerId1";
        WorkerManager.unregisterWorker(familyId, workerId);
    }

    @Test(expected = WorkerNotFoundException.class)
    public void givenProcessDistributorWhenUnRegisterNonExistingWorkerThenProcessingException() throws Exception {
        final String familyId = "DefaultWorker" ;
        final String workerId = "NewWorkerId3" + GUIDFactory.newGUID().getId();
        final String workerUnknownId = "UnknownWorkerId";
        WorkerManager.registerWorker(familyId, workerId, WORKER_DESCRIPTION);
        WorkerManager.unregisterWorker(familyId, workerUnknownId);
    }

    @Test(expected = ProcessingBadRequestException.class)
    public void givenProcessDistributorWhenRegisterIncorrectJsonNodeThenProcessingException() throws Exception {
        final String familyId = "NewFamilyId";
        final String workerId = "NewWorkerId4" + GUIDFactory.newGUID().getId();
        WorkerManager.registerWorker(familyId, workerId, "{\"fakeKey\" : \"fakeValue\"}");
    }

    @Test
    public void givenProcessDistributorWhenRegisterWorkerExistingFamilyThenOK() throws Exception {
        final String familyId = "DefaultWorker" ;
        final String workerId = "NewWorkerId5"+ GUIDFactory.newGUID().getId();
        WorkerManager.registerWorker(familyId, workerId, WORKER_DESCRIPTION);
        assertTrue(WorkerManager.getWorkersList().size() > 0);
    }
    
    private class RunnableMock implements Runnable{
        public RunnableMock(){
            
        }
        
        @Override
        public void run(){
            
        }
    }
    
    private class VitamSessionMock extends VitamSession{
        /**
         * @param owningThread
         */
        public VitamSessionMock(VitamThread owningThread) {
            super(owningThread);
        }

        @Override
        public String getRequestId() {
            return "";
        }

        @Override
        public void setRequestId(String newRequestId) {
        }

        @Override
        public void setRequestId(GUID guid) {
        }

        @Override
        public void mutateFrom(@NotNull VitamSession newSession) {
        }
        
        @Override
        public void erase() {
        }

        @Override
        public void checkValidRequestId() {
        }


        @Override
        public String toString() {
            return "";
        }


    }

}
