/*
 *  Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *  <p>
 *  contact.vitam@culture.gouv.fr
 *  <p>
 *  This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 *  high volumetry securely and efficiently.
 *  <p>
 *  This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 *  software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 *  circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 *  <p>
 *  As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 *  users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 *  successive licensors have only limited liability.
 *  <p>
 *  In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 *  developing or reproducing the software by the user in light of its specific status of free software, that may mean
 *  that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 *  experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 *  software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 *  to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *  <p>
 *  The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 *  accept its terms.
 */

package fr.gouv.vitam.processing.distributor.v2;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.processing.common.model.Step;
import fr.gouv.vitam.processing.common.model.WorkerBean;
import fr.gouv.vitam.worker.client.WorkerClient;
import fr.gouv.vitam.worker.client.WorkerClientConfiguration;
import fr.gouv.vitam.worker.client.WorkerClientFactory;
import fr.gouv.vitam.worker.client.exception.WorkerExecutorException;
import fr.gouv.vitam.worker.client.exception.WorkerNotFoundClientException;
import fr.gouv.vitam.worker.client.exception.WorkerServerClientException;
import fr.gouv.vitam.worker.client.exception.WorkerUnreachableException;
import fr.gouv.vitam.worker.common.DescriptionStep;

import java.util.function.Supplier;

// Task simulating a call to a worker
public class WorkerTask implements Supplier<ItemStatus> {
    private static final VitamLogger
        LOGGER = VitamLoggerFactory.getInstance(WorkerTask.class);

    private final DescriptionStep descriptionStep;
    private final int tenantId;
    private final String requestId;

    public WorkerTask(DescriptionStep descriptionStep, int tenantId, String requestId) {
        ParametersChecker.checkParameter("Params are required", descriptionStep, requestId);
        this.descriptionStep = descriptionStep;
        this.tenantId = tenantId;
        this.requestId = requestId;
    }

    @Override
    public ItemStatus get() {
        VitamThreadUtils.getVitamSession().setRequestId(requestId);
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);

        final WorkerBean workerBean = WorkerInformation.getWorkerThreadLocal().get().getWorkerBean();
        LOGGER.info("Start executing of task number :" + descriptionStep.getStep().getStepName() + " on worker: " + workerBean.getWorkerId());
        try {

            final WorkerClientConfiguration configuration = new WorkerClientConfiguration(
                workerBean.getConfiguration().getServerHost(),
                workerBean.getConfiguration().getServerPort());

            WorkerClientFactory.changeMode(configuration);

            try (WorkerClient workerClient = WorkerClientFactory.getInstance(configuration).getClient()) {
                return workerClient.submitStep(descriptionStep);
            } catch (WorkerNotFoundClientException | WorkerServerClientException e) {
                // check status
                boolean checkStatus = false;
                int numberCallCheckStatus = 0;
                while (!checkStatus && numberCallCheckStatus < GlobalDataRest.STATUS_CHECK_RETRY) {
                    checkStatus =
                        checkStatusWorker(workerBean.getConfiguration().getServerHost(),
                            workerBean.getConfiguration().getServerPort());
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
                    LOGGER.error(e);
                    throw new WorkerUnreachableException(workerBean.getWorkerId(), e);
                }
                LOGGER.error(e);
                throw new WorkerExecutorException(e);
            }

        } catch (Exception e) {
            LOGGER.error(e);
            throw new WorkerExecutorException(e);
        } finally {
            LOGGER.info("End executing of task number :" + descriptionStep.getStep().getStepName() + " on worker: " + workerBean.getWorkerId());
        }
    }

    boolean checkStatusWorker(String serverHost, int serverPort) {
        WorkerClientConfiguration workerClientConfiguration =
            new WorkerClientConfiguration(serverHost, serverPort);
        WorkerClientFactory.changeMode(workerClientConfiguration);
        WorkerClient workerClient = WorkerClientFactory.getInstance(workerClientConfiguration).getClient();
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
}
