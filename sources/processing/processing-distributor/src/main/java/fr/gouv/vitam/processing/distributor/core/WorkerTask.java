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
package fr.gouv.vitam.processing.distributor.core;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.processing.Step;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.processing.common.async.AccessRequestContext;
import fr.gouv.vitam.processing.common.async.ProcessingRetryAsyncException;
import fr.gouv.vitam.processing.common.metrics.CommonProcessingMetrics;
import fr.gouv.vitam.processing.common.model.WorkerBean;
import fr.gouv.vitam.worker.client.WorkerClient;
import fr.gouv.vitam.worker.client.WorkerClientConfiguration;
import fr.gouv.vitam.worker.client.WorkerClientFactory;
import fr.gouv.vitam.worker.client.exception.WorkerClientException;
import fr.gouv.vitam.worker.client.exception.WorkerExecutorException;
import fr.gouv.vitam.worker.client.exception.WorkerNotFoundClientException;
import fr.gouv.vitam.worker.client.exception.WorkerServerClientException;
import fr.gouv.vitam.worker.client.exception.WorkerUnreachableException;
import fr.gouv.vitam.worker.common.DescriptionStep;
import io.prometheus.client.Histogram;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

// Task simulating a call to a worker
public class WorkerTask implements Supplier<WorkerTaskResult> {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WorkerTask.class);

    private final DescriptionStep descriptionStep;
    private final int tenantId;
    private final String requestId;
    private final String taskId;
    private final String contractId;
    private final String contextId;
    private final String applicationId;

    private final WorkerClientFactory workerClientFactory;
    private final Histogram.Timer taskWaitingTimeDuration;

    public WorkerTask(
        DescriptionStep descriptionStep,
        int tenantId,
        String requestId,
        String contractId,
        String contextId,
        String applicationId,
        WorkerClientFactory workerClientFactory
    ) {
        ParametersChecker.checkParameter("Params are required", descriptionStep, requestId);
        this.descriptionStep = descriptionStep;
        this.tenantId = tenantId;
        this.requestId = requestId;
        this.taskId = GUIDFactory.newGUID().getId();
        this.contractId = contractId;
        this.contextId = contextId;
        this.applicationId = applicationId;
        this.workerClientFactory = workerClientFactory;

        // Add metrics compute duration of waiting time before execution of the task
        taskWaitingTimeDuration = CommonProcessingMetrics.WORKER_TASKS_IDLE_DURATION_IN_QUEUE.labels(
            getStep().getWorkerGroupId(),
            descriptionStep.getWorkParams().getLogbookTypeProcess().name(),
            descriptionStep.getStep().getStepName()
        ).startTimer();
    }

    @Override
    public WorkerTaskResult get() {
        if (null != taskWaitingTimeDuration) {
            taskWaitingTimeDuration.observeDuration();
        }

        VitamThreadUtils.getVitamSession().setRequestId(requestId);
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        VitamThreadUtils.getVitamSession().setContractId(contractId);
        VitamThreadUtils.getVitamSession().setContextId(contextId);
        VitamThreadUtils.getVitamSession().setApplicationSessionId(applicationId);

        final WorkerBean workerBean = WorkerInformation.getWorkerThreadLocal().get().getWorkerBean();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
                "Start executing of task number :" +
                descriptionStep.getStep().getStepName() +
                " on worker: " +
                workerBean.getWorkerId()
            );
        }
        try {
            final WorkerClientConfiguration configuration = new WorkerClientConfiguration(
                workerBean.getConfiguration().getServerHost(),
                workerBean.getConfiguration().getServerPort()
            );

            /*
             * FIXME : Bug #8729 - Updating a static client configuration can cause :
             *  - Inconsistent state : non synchronized client configuration mutation may cause unexpected behaviour
             *  - Minor perf degradation (especially if HTTPS is enabled)
             */
            WorkerClientFactory.changeMode(configuration);
            WorkerClient workerClient;
            if (null == workerClientFactory) {
                workerClient = WorkerClientFactory.getInstance(configuration).getClient();
            } else {
                workerClient = workerClientFactory.getClient();
            }

            try {
                switch (getStep().getPauseOrCancelAction()) {
                    case ACTION_RUN:
                    case ACTION_RECOVER:
                    case ACTION_REPLAY:
                        return callWorker(workerBean, workerClient);
                    case ACTION_PAUSE:
                    case ACTION_CANCEL:
                        // Fail fast : if workflow is being paused or canceled, no need to execute it
                        return WorkerTaskResult.ofPausedOrCanceledTask(this);
                    case ACTION_COMPLETE:
                        throw new WorkerExecutorException(
                            workerBean.getWorkerId(),
                            "Step id: " +
                            getStep().getId() +
                            " and name :" +
                            getStep().getStepName() +
                            " already completed"
                        );
                    default:
                        throw new WorkerExecutorException(
                            workerBean.getWorkerId(),
                            "The default case should not be handled"
                        );
                }
            } catch (WorkerNotFoundClientException | WorkerServerClientException e) {
                checkWorkerAvailability(workerBean, workerClient, e);
                throw new WorkerExecutorException(workerBean.getWorkerId(), e);
            } finally {
                workerClient.close();
            }
        } catch (WorkerExecutorException e) {
            throw e;
        } catch (Exception e) {
            throw new WorkerExecutorException(workerBean.getWorkerId(), e);
        } finally {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(
                    "End executing of task number :" +
                    descriptionStep.getStep().getStepName() +
                    " on worker: " +
                    workerBean.getWorkerId()
                );
            }
        }
    }

    private void checkWorkerAvailability(WorkerBean workerBean, WorkerClient workerClient, WorkerClientException e) {
        // check status
        for (
            int numberCallCheckStatus = 0;
            numberCallCheckStatus < GlobalDataRest.STATUS_CHECK_RETRY;
            numberCallCheckStatus++
        ) {
            boolean checkStatus = checkStatusWorker(
                workerClient,
                workerBean.getConfiguration().getServerHost(),
                workerBean.getConfiguration().getServerPort()
            );
            if (checkStatus) {
                return;
            }
            try {
                TimeUnit.MILLISECONDS.sleep(1000);
            } catch (final InterruptedException e1) {
                LOGGER.warn(e);
                Thread.currentThread().interrupt();
            }
        }
        throw new WorkerUnreachableException(workerBean.getWorkerId(), e);
    }

    private WorkerTaskResult callWorker(WorkerBean workerBean, WorkerClient workerClient)
        throws WorkerNotFoundClientException, WorkerServerClientException {
        // Add metrics
        Histogram.Timer workerTaskTimer = CommonProcessingMetrics.WORKER_TASKS_EXECUTION_DURATION_HISTOGRAM.labels(
            workerBean.getFamily(),
            workerBean.getName(),
            descriptionStep.getWorkParams().getLogbookTypeProcess().name(),
            descriptionStep.getStep().getStepName()
        ).startTimer();
        try {
            ItemStatus itemStatus = workerClient.submitStep(descriptionStep);
            return WorkerTaskResult.ofProceededTask(this, itemStatus);
        } catch (ProcessingRetryAsyncException prae) {
            // In case of async add accessRequestIds to the async resources monitor for future retry and return null (no ItemStatus value)
            LOGGER.warn(prae);
            Map<String, AccessRequestContext> asyncResources = new HashMap<>();
            for (AccessRequestContext context : prae.getAccessRequestIdByContext().keySet()) {
                for (String accessRequestId : prae.getAccessRequestIdByContext().get(context)) {
                    asyncResources.put(accessRequestId, context);
                }
            }
            return WorkerTaskResult.ofTaskRequiringAsyncResourceAvailability(this, asyncResources);
        } finally {
            // Add metric on worker task execution duration
            workerTaskTimer.observeDuration();
        }
    }

    boolean checkStatusWorker(WorkerClient workerClient, String serverHost, int serverPort) {
        try {
            workerClient.checkStatus();
            return true;
        } catch (Exception e) {
            LOGGER.error("Worker server [" + serverHost + ":" + serverPort + "] is not active.", e);
            return false;
        }
    }

    public Step getStep() {
        return descriptionStep.getStep();
    }

    public List<String> getObjectNameList() {
        return descriptionStep.getWorkParams().getObjectNameList();
    }

    public String getTaskId() {
        return taskId;
    }

    public String getRequestId() {
        return requestId;
    }
}
