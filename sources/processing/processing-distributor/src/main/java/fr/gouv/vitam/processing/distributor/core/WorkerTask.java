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
package fr.gouv.vitam.processing.distributor.core;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.PauseOrCancelAction;
import fr.gouv.vitam.common.model.processing.Step;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.processing.common.model.WorkerBean;
import fr.gouv.vitam.processing.common.model.WorkerTaskState;
import fr.gouv.vitam.worker.client.WorkerClient;
import fr.gouv.vitam.worker.client.WorkerClientConfiguration;
import fr.gouv.vitam.worker.client.WorkerClientFactory;
import fr.gouv.vitam.worker.client.exception.WorkerExecutorException;
import fr.gouv.vitam.worker.client.exception.WorkerNotFoundClientException;
import fr.gouv.vitam.worker.client.exception.WorkerServerClientException;
import fr.gouv.vitam.worker.client.exception.WorkerUnreachableException;
import fr.gouv.vitam.worker.common.DescriptionStep;

// Task simulating a call to a worker
public class WorkerTask implements Supplier<ItemStatus> {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WorkerTask.class);

    private final DescriptionStep descriptionStep;
    private final int tenantId;
    private final String requestId;
    private final String contractId;
    private final String contextId;
    private final String applicationId;
    private volatile WorkerTaskState workerTaskState = WorkerTaskState.PENDING;

    private WorkerClientFactory workerClientFactory = null;


    public WorkerTask(DescriptionStep descriptionStep, int tenantId, String requestId, String contractId,
        String contextId, String applicationId) {
        ParametersChecker.checkParameter("Params are required", descriptionStep, requestId);
        this.descriptionStep = descriptionStep;
        this.tenantId = tenantId;
        this.requestId = requestId;
        this.contractId = contractId;
        this.contextId = contextId;
        this.applicationId = applicationId;
    }

    public WorkerTask(DescriptionStep descriptionStep, int tenantId, String requestId, String contractId,
        String contextId, String applicationId, WorkerClientFactory workerClientFactory) {
        ParametersChecker.checkParameter("Params are required", descriptionStep, requestId);
        this.descriptionStep = descriptionStep;
        this.tenantId = tenantId;
        this.requestId = requestId;
        this.contractId = contractId;
        this.contextId = contextId;
        this.applicationId = applicationId;
        this.workerClientFactory = workerClientFactory;
    }

    @Override
    public ItemStatus get() {
        VitamThreadUtils.getVitamSession().setRequestId(requestId);
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        VitamThreadUtils.getVitamSession().setContractId(contractId);
        VitamThreadUtils.getVitamSession().setContextId(contextId);
        VitamThreadUtils.getVitamSession().setApplicationSessionId(applicationId);

        final WorkerBean workerBean = WorkerInformation.getWorkerThreadLocal().get().getWorkerBean();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Start executing of task number :" + descriptionStep.getStep().getStepName() + " on worker: " +
                workerBean.getWorkerId());
        }
        try {

            final WorkerClientConfiguration configuration = new WorkerClientConfiguration(
                workerBean.getConfiguration().getServerHost(),
                workerBean.getConfiguration().getServerPort());

            WorkerClientFactory.changeMode(configuration);
            WorkerClient workerClient;
            if (null == workerClientFactory) {
                workerClient = WorkerClientFactory.getInstance(configuration).getClient();
            } else {
                workerClient = workerClientFactory.getClient();

            }

            try {

                switch (descriptionStep.getStep().getPauseOrCancelAction()) {
                    case ACTION_RUN:
                    case ACTION_RECOVER:
                    case ACTION_REPLAY:
                        workerTaskState = WorkerTaskState.RUNNING;
                        return workerClient.submitStep(descriptionStep);
                    case ACTION_PAUSE:
                        // The current elements will be persisted in the distributorIndex in the remaining elements
                        workerTaskState = WorkerTaskState.PAUSE;
                        return new ItemStatus(PauseOrCancelAction.ACTION_PAUSE.name())
                            .setItemsStatus(PauseOrCancelAction.ACTION_PAUSE.name(),
                                new ItemStatus(PauseOrCancelAction.ACTION_PAUSE.name())
                                    .increment(StatusCode.UNKNOWN));
                    case ACTION_COMPLETE:
                        throw new WorkerExecutorException(workerBean.getWorkerId(), "Step already completed");
                    case ACTION_CANCEL:
                        workerTaskState = WorkerTaskState.CANCEL;
                        return new ItemStatus(PauseOrCancelAction.ACTION_CANCEL.name())
                            .setItemsStatus(PauseOrCancelAction.ACTION_CANCEL.name(),
                                new ItemStatus(PauseOrCancelAction.ACTION_CANCEL.name())
                                    .increment(StatusCode.UNKNOWN));
                    default:
                        throw new WorkerExecutorException(workerBean.getWorkerId(),
                            "The default case should not be handled");
                }
            } catch (WorkerNotFoundClientException | WorkerServerClientException e) {
                // check status
                boolean checkStatus = false;
                int numberCallCheckStatus = 0;
                while (!checkStatus && numberCallCheckStatus < GlobalDataRest.STATUS_CHECK_RETRY) {
                    checkStatus =
                        checkStatusWorker(workerClient, workerBean.getConfiguration().getServerHost(),
                            workerBean.getConfiguration().getServerPort());
                    numberCallCheckStatus++;
                    if (!checkStatus) {
                        try {
                            TimeUnit.MILLISECONDS.sleep(1000);
                        } catch (final InterruptedException e1) {
                            LOGGER.warn(e);
                            Thread.currentThread().interrupt();
                        }
                    }
                }
                if (!checkStatus) {
                    throw new WorkerUnreachableException(workerBean.getWorkerId(), e);
                }
                throw new WorkerExecutorException(workerBean.getWorkerId(), e);

            } finally {
                workerClient.close();
            }

        } catch (WorkerUnreachableException e) {
            throw e;
        } catch (WorkerExecutorException e) {
            throw e;
        } catch (Exception e) {
            throw new WorkerExecutorException(workerBean.getWorkerId(), e);
        } finally {
            if (!WorkerTaskState.PAUSE.equals(workerTaskState) && !WorkerTaskState.CANCEL.equals(workerTaskState)) {
                workerTaskState = WorkerTaskState.COMPLETED;
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER
                    .debug("End executing of task number :" + descriptionStep.getStep().getStepName() + " on worker: " +
                        workerBean.getWorkerId());
            }
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

    public String getObjectName() {
        return descriptionStep.getWorkParams().getObjectName();
    }

    public List<String> getObjectNameList() {
        return descriptionStep.getWorkParams().getObjectNameList();
    }

    public boolean isCompleted() {
        return WorkerTaskState.COMPLETED.equals(workerTaskState);
    }
}
