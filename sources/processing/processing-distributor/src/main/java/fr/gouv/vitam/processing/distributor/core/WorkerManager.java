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

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.processing.common.exception.ProcessingBadRequestException;
import fr.gouv.vitam.processing.common.exception.WorkerAlreadyExistsException;
import fr.gouv.vitam.processing.common.exception.WorkerFamilyNotFoundException;
import fr.gouv.vitam.processing.common.exception.WorkerNotFoundException;
import fr.gouv.vitam.processing.common.model.WorkerBean;
import fr.gouv.vitam.processing.common.model.WorkerRemoteConfiguration;
import fr.gouv.vitam.processing.model.WorkerAsyncRequest;
import fr.gouv.vitam.processing.model.WorkerAsyncResponse;
import fr.gouv.vitam.worker.client.WorkerClient;
import fr.gouv.vitam.worker.client.WorkerClientConfiguration;
import fr.gouv.vitam.worker.client.WorkerClientFactory;
import fr.gouv.vitam.worker.client.exception.WorkerNotFoundClientException;
import fr.gouv.vitam.worker.client.exception.WorkerServerClientException;

/**
 * Manage the parallelism calls to worker in the same distributor
 */
public class WorkerManager {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WorkerManager.class);
    // No need to have a concurrent map while there is no dymanic add/remove of queues
    private static final ConcurrentMap<String, BlockingQueue<WorkerAsyncRequest>> STEP_BLOCKINGQUEUE_MAP =
        new ConcurrentHashMap<>();

    // The risk of collision between a register/unregister worker is not null
    private static final Map<String, Map<String, WorkerThreadManager>> WORKERS_LIST = new ConcurrentHashMap<>();
    private static final int DEFAULT_QUEUE_BACKLOG_SIZE = 20;

    private static final String WORKKER_DB_PATH = "worker.db";
    private static final File WORKKER_DB_FILE = PropertiesUtils.fileFromDataFolder(WORKKER_DB_PATH);

    /**
     * Vitam worker info
     */
    private static final String WORKER_INFO = "workerInfo";
    /**
     * Vitam worker id
     */
    private static final String WORKER_ID = "workerId";



    /**
     * Empty Constructor
     */
    private WorkerManager() {

    }

    /**
     * init the worker database
     */
    public static void initialize() {
        if (WORKKER_DB_FILE.exists()) {
            try {
                WorkerManager.loadWorkerList(WORKKER_DB_FILE);
            } catch (InvalidParseOperationException e) {
                LOGGER.error("Invalid syntax");
            }
        } else {
            LOGGER.warn("No worker list serialization file : " + WORKKER_DB_FILE.getName());
        }

    }

    /**
     * To load a registered worker list
     * 
     * @throws InvalidParseOperationException
     * 
     * @throws IOException
     * @throws JsonProcessingException
     * 
     */
    private static void loadWorkerList(File registerWorkerFile) throws InvalidParseOperationException {
        // Load the list of worker from database
        // for now it is a file content json data
        ArrayNode registeredWorkerList = null;
        try {
            registeredWorkerList = (ArrayNode) JsonHandler.getFromFile(registerWorkerFile);
        } catch (InvalidParseOperationException e) {
            LOGGER.error("Cannot load worker list from database.");
        }

        // load to the list of WORKERS_LIST
        int index = 0;
        while (registeredWorkerList.size() > 0 && index < registeredWorkerList.size()) {
            JsonNode worker = registeredWorkerList.get(index);
            WorkerBean workerBean =
                JsonHandler.getFromJsonNodeLowerCamelCase(worker.get(WORKER_INFO),
                    WorkerBean.class);
            String workerId = worker.get(WORKER_ID).asText();
            String familyId = workerBean.getFamily();
            WorkerRemoteConfiguration config = workerBean.getConfiguration();
            if (!checkStatusWorker(config.getServerHost(), config.getServerPort())) {
                marshallToDB();
                registeredWorkerList.remove(index);
            } else {
                index++;

                WorkerThreadManager workerThreadManager = new WorkerThreadManager(workerBean, familyId);

                if (WORKERS_LIST.get(familyId) != null) {
                    Map<String, WorkerThreadManager> familyWorkers = WORKERS_LIST.get(familyId);
                    familyWorkers.put(workerId, workerThreadManager);
                    WORKERS_LIST.put(familyId, familyWorkers);

                } else {
                    Map<String, WorkerThreadManager> familyWorkers = new ConcurrentHashMap<>();
                    familyWorkers.put(workerId, workerThreadManager);
                    WORKERS_LIST.put(familyId, familyWorkers);
                }
            }
        }
    }

    private static boolean checkStatusWorker(String serverHost, int serverPort) {
        WorkerClientConfiguration workerClientConfiguration =
            new WorkerClientConfiguration(serverHost, serverPort);
        WorkerClientFactory.changeMode(workerClientConfiguration);
        
        try (WorkerClient workerClient = WorkerClientFactory.getInstance(workerClientConfiguration).getClient()) {
            workerClient.checkStatus();
            return true;
        } catch (Exception e) {
            LOGGER.error("Worker server [" + serverHost + ":" + serverPort + "] is not active.", e);
            return false;
        }
    }

    /**
     * To register a worker in the processing
     * 
     * @param familyId : family of this worker
     * @param workerId : ID of the worker
     * @param workerInformation : Worker Json representation
     * @throws WorkerAlreadyExistsException : when the worker is already registered
     * @throws ProcessingBadRequestException if cannot register worker to family
     * @throws InvalidParseOperationException if worker description is not well-formed
     */
    public static void registerWorker(String familyId, String workerId, String workerInformation)
        throws WorkerAlreadyExistsException, ProcessingBadRequestException, InvalidParseOperationException {
        ParametersChecker.checkParameter("familyId is a mandatory argument", familyId);
        ParametersChecker.checkParameter("workerId is a mandatory argument", workerId);
        ParametersChecker.checkParameter("workerInformation is a mandatory argument", workerInformation);
        WorkerBean worker = null;
        try {
            worker = JsonHandler.getFromString(workerInformation, WorkerBean.class);
            if (!worker.getFamily().equals(familyId)) {
                throw new ProcessingBadRequestException("Cannot register a worker of another family!");
            } else {
                worker.setWorkerId(workerId);
            }

        } catch (final InvalidParseOperationException e) {
            LOGGER.error("Worker Information incorrect", e);
            throw new ProcessingBadRequestException("Worker description is incorrect");
        }

        // Create the blocking queue for familyId worker
        if (STEP_BLOCKINGQUEUE_MAP.get(familyId) == null) {
            STEP_BLOCKINGQUEUE_MAP.put(familyId,
                new ArrayBlockingQueue<WorkerAsyncRequest>(DEFAULT_QUEUE_BACKLOG_SIZE));
        }

        // Create the WorkerThreadManager for this new Worker
        final WorkerThreadManager workerThreadManager = new WorkerThreadManager(worker, familyId);
        if (WORKERS_LIST.get(familyId) != null) {
            final Map<String, WorkerThreadManager> familyWorkers = WORKERS_LIST.get(familyId);
            if (familyWorkers.get(workerId) != null) {
                LOGGER.warn("Worker already registered");
                throw new WorkerAlreadyExistsException("Worker already registered");
            } else {
                // Add the new WorkerThreadManager to the existing Family
                familyWorkers.put(workerId, workerThreadManager);
                // Note: not mandatory
                WORKERS_LIST.put(familyId, familyWorkers);
                VitamThreadPoolExecutor.getDefaultExecutor().execute(workerThreadManager);
            }
        } else {
            // Add the new WorkerThreadManager to the new Family
            // Note: Concurrent to prevent issue on Adding/Removing Workers
            final Map<String, WorkerThreadManager> familyWorkers = new ConcurrentHashMap<>();
            familyWorkers.put(workerId, workerThreadManager);
            WORKERS_LIST.put(familyId, familyWorkers);
            VitamThreadPoolExecutor.getDefaultExecutor().execute(workerThreadManager);
        }

        // update new worker in the database
        marshallToDB();
    }

    /**
     * To unregister a worker in the processing
     * 
     * @param familyId : family of this worker
     * @param workerId : ID of the worker
     * @throws WorkerFamilyNotFoundException : when the family is unknown
     * @throws WorkerNotFoundException : when the ID of the worker is unknown in the family
     * @throws InterruptedException if error in stopping thread
     */
    public static void unregisterWorker(String familyId, String workerId)
        throws WorkerFamilyNotFoundException, WorkerNotFoundException, InterruptedException {
        ParametersChecker.checkParameter("familyId is a mandatory argument", familyId);
        ParametersChecker.checkParameter("workerId is a mandatory argument", workerId);
        final Map<String, WorkerThreadManager> familyWorkers = WORKERS_LIST.get(familyId);
        if (familyWorkers != null) {
            WorkerThreadManager workerThreadManager = familyWorkers.get(workerId);
            if (workerThreadManager != null) {
                // Stop giving new tasks to this job
                workerThreadManager.stopWorkerThreadManager();
                // Waiting for the
                workerThreadManager.waitingRunningJobsDone(GlobalDataRest.TIMEOUT_END_WORKER_MILLISECOND);
                familyWorkers.remove(workerId);
                // delete from database
                marshallToDB();
            } else {
                LOGGER.error("Worker does not exist in this family");
                throw new WorkerNotFoundException("Worker does not exist in this family");
            }
        } else {
            LOGGER.error("Worker Family does not exist");
            throw new WorkerFamilyNotFoundException("Worker Family does not exist");
        }

    }

    /**
     * To submit a Job to the workerManager (blocking method)
     * 
     * @param queueID : ID of the queue on which the job must be submitted
     * @param workerAsyncRequest : Asynchronous request
     * @throws ProcessingBadRequestException : if the queueID is unknown
     * @throws InterruptedException if error in stopping thread
     */
    public static void submitJob(WorkerAsyncRequest workerAsyncRequest)
        throws ProcessingBadRequestException, InterruptedException {
        ParametersChecker.checkParameter("queue is a mandatory argument", workerAsyncRequest.getQueueID());
        ParametersChecker.checkParameter("workerAsyncRequest is a mandatory argument", workerAsyncRequest);
        BlockingQueue<WorkerAsyncRequest> blockingQueue = STEP_BLOCKINGQUEUE_MAP.get(workerAsyncRequest.getQueueID());
        if (blockingQueue != null) {
            blockingQueue.put(workerAsyncRequest);
        } else {
            throw new ProcessingBadRequestException(
                "Unknown queue in the workerManager : " + workerAsyncRequest.getQueueID());
        }

    }

    /**
     * To remove a Job from the workerManager (non blocking method)
     * 
     * @param queueID : ID of the queue from which the job must be removed
     * @param workerAsyncRequest : Asynchronous request that must be removed
     * @return true if the workerAsyncRequest was present, false if not
     * @throws ProcessingBadRequestException : if the queueID is unknown
     */
    public static boolean removeJobs(WorkerAsyncRequest workerAsyncRequest)
        throws ProcessingBadRequestException {
        BlockingQueue<WorkerAsyncRequest> blockingQueue = STEP_BLOCKINGQUEUE_MAP.get(workerAsyncRequest.getQueueID());
        if (blockingQueue != null) {
            return blockingQueue.remove(workerAsyncRequest);
        } else {
            throw new ProcessingBadRequestException(
                "Unknown queue in the workerManager : " + workerAsyncRequest.getQueueID());
        }
    }

    protected static Map<String, Map<String, WorkerThreadManager>> getWorkersList() {
        return WORKERS_LIST;
    }

    private synchronized static void marshallToDB() {
        if (!WORKKER_DB_FILE.exists()) {
            try {
                WORKKER_DB_FILE.createNewFile();
            } catch (IOException e) {
                LOGGER.warn("Cannot create worker list serialization file : " + WORKKER_DB_FILE.getName());
            }
        }
        ArrayNode registeredWorkers = JsonHandler.createArrayNode();
        for (Entry<String, Map<String, WorkerThreadManager>> family : WORKERS_LIST.entrySet()) {
            for (Entry<String, WorkerThreadManager> worker : family.getValue().entrySet()) {
                try {
                    String workerBean = JsonHandler.writeAsString(worker.getValue().getWorkerBean());
                    JsonNode workerDetails = JsonHandler.getFromString(
                        "{\"workerId\" : \"" + worker.getKey() + "\",\"workerInfo\" : " + workerBean + "}");
                    registeredWorkers.add(workerDetails);
                    if (!WORKKER_DB_FILE.exists()) {
                        WORKKER_DB_FILE.createNewFile();
                    }
                    JsonHandler.writeAsFile(registeredWorkers, WORKKER_DB_FILE);
                } catch (InvalidParseOperationException | IOException e) {
                    LOGGER.error("Cannot update database worker");
                }
            }
        }
    }

    /**
     * The WorkerThreadManager manages all the threads for a given Worker
     */
    private static class WorkerThreadManager implements Runnable {
        private WorkerBean workerBean;
        // No high need to have AtomicBoolean as it is only used to stop the WorkerThreadManager (ex: during the
        // unregister of a worker)
        private volatile boolean toBeRunnable = true;
        private final String queue;
        private final Semaphore semaphore;
        private volatile Thread myself;
        private final int capacity;

        public WorkerThreadManager(WorkerBean workerBean, String queue) {
            ParametersChecker.checkParameter("workerBean is a mandatory argument", workerBean);
            ParametersChecker.checkParameter("queue is a mandatory argument", queue);
            this.workerBean = workerBean;
            this.queue = queue;
            this.capacity = workerBean.getCapacity();
            this.semaphore = new Semaphore(capacity);

        }

        /**
         * Main forever method
         */
        @Override
        public void run() {
            try {
                // Register now its own thread
                myself = Thread.currentThread();
                // FIXME : when there is an unregisterWorker, the thread ends only after one more step (as it is
                // blocking on the take), but interrupt on stopWorkerThreadManager could partially resolve the issue
                // Order of the blocking call : first see if we have capacity in this worker (acquire the semaphore
                // token), then see if there is work to process .
                // So in this way, we don't take a task if can not treat it right now
                while (toBeRunnable) {
                    semaphore.acquire();
                    if (STEP_BLOCKINGQUEUE_MAP.get(queue) != null) {
                        WorkerAsyncRequest workerAsyncRequest = STEP_BLOCKINGQUEUE_MAP.get(queue).take();
                        VitamThreadPoolExecutor.getDefaultExecutor()
                            .execute(new WorkerThread(this, workerAsyncRequest));
                    }
                }
            } catch (InterruptedException e) { // NOSONAR already taken into account
                LOGGER.warn("Probably unregistring this Worker", e);
            }
        }

        /**
         * Stop the workerThreadManager, both using boolean and interruption
         */
        public void stopWorkerThreadManager() {
            toBeRunnable = false;
            if (myself != null) {
                myself.interrupt();
            }
        }

        public void waitingRunningJobsDone(long timeout) throws InterruptedException {
            long epoch = System.currentTimeMillis();
            while (System.currentTimeMillis() < (epoch + timeout)) {
                if (toBeRunnable && (semaphore.availablePermits() == capacity)) {
                    return;
                }
                Thread.sleep(1000);
            }
        }

        public WorkerBean getWorkerBean() {
            return workerBean;
        }

        public Semaphore getSemaphore() {
            return semaphore;
        }

    }

    /**
     * The Worker Thread manages the actions for one thread for a given Worker
     */
    private static class WorkerThread implements Runnable {
        private WorkerThreadManager workerThreadManager;
        private WorkerAsyncRequest workerAsyncRequest;

        public WorkerThread(WorkerThreadManager workerThreadManager, WorkerAsyncRequest workerAsyncRequest) {
            this.workerThreadManager = workerThreadManager;
            this.workerAsyncRequest = workerAsyncRequest;
        }

        /**
         *
         */
        @Override
        public void run() {
            ItemStatus actionsResponse = null;
            // As this thread is not a son of a request Rest but of the WorkerThread
            VitamThreadUtils.getVitamSession().setRequestId(workerAsyncRequest.getSession().getRequestId());
            VitamThreadUtils.getVitamSession().setTenantId(workerAsyncRequest.getSession().getTenantId());
            try {
                actionsResponse =
                    new ItemStatus(workerAsyncRequest.getDescriptionStep().getStep().getStepName());
                loadWorkerClient(workerThreadManager.getWorkerBean());
                WorkerClientConfiguration configuration = new WorkerClientConfiguration(
                    workerThreadManager.getWorkerBean().getConfiguration().getServerHost(),
                    workerThreadManager.getWorkerBean().getConfiguration().getServerPort());
                try (WorkerClient workerClient = WorkerClientFactory.getInstance(configuration).getClient()) {
                    actionsResponse =
                        workerClient.submitStep(workerAsyncRequest.getDescriptionStep());
                    // FIXME : à voir comment retraiter
                } catch (WorkerNotFoundClientException | WorkerServerClientException e) {
                    // Maybe resubmit, not throwing any state
                    // try {
                    // submitJob(workerAsyncRequest);
                    // and not setting actionsResponse (using a special boolean ?)
                    // } catch (ProcessingInternalServerException | ProcessingBadRequestException |
                    // InterruptedException e1) {
                    // but only once
                    // LOGGER.error(e);
                    // actionsResponse.increment(StatusCode.FATAL);
                    // }
                    // Or maybe having a way to return back to the ProcessDistributor the not launched task, letting him
                    // handling this one, but not removing it from the to be run
                    // and not setting actionsResponse (using a special boolean ?)
                    // Note: method to create
                    // workerAsyncRequest.callCallbackCannotRun(workerAsyncRequest);

                    // check status
                    boolean checkStatus = false;
                    int numberCallCheckStatus = 0;
                    while (!checkStatus && numberCallCheckStatus < GlobalDataRest.STATUS_CHECK_RETRY) {
                        checkStatus =
                            checkStatusWorker(workerThreadManager.getWorkerBean().getConfiguration().getServerHost(),
                                workerThreadManager.getWorkerBean().getConfiguration().getServerPort());
                        numberCallCheckStatus++;
                        if (!checkStatus) {
                            try {
                                this.wait(1000);
                            } catch (final InterruptedException e1) {
                                LOGGER.error(e);
                            }
                        }
                    }

                    if (!checkStatus) {
                        try {
                            WorkerManager.unregisterWorker(workerThreadManager.getWorkerBean().getFamily(),
                                workerThreadManager.getWorkerBean().getWorkerId());
                        } catch (WorkerFamilyNotFoundException | WorkerNotFoundException | InterruptedException e1) {
                            LOGGER.error("Cannot unregister the worker.");
                        }
                    }

                    LOGGER.error(e);
                    actionsResponse.increment(StatusCode.FATAL);
                }
            } catch (RuntimeException | Error e) {
                if (actionsResponse != null) {
                    actionsResponse.increment(StatusCode.FATAL);
                }
                LOGGER.error(e);
            } finally {
                // except using a special boolean for resubmit ?
                workerAsyncRequest
                    .callCallback(new WorkerAsyncResponse(workerAsyncRequest, actionsResponse));
                workerThreadManager.getSemaphore().release();
            }
        }

        private void loadWorkerClient(WorkerBean workerBean) {
            final WorkerClientConfiguration workerClientConfiguration =
                new WorkerClientConfiguration(workerBean.getConfiguration().getServerHost(),
                    workerBean.getConfiguration().getServerPort());
            WorkerClientFactory.changeMode(workerClientConfiguration);
        }
    }
}
