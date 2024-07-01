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

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.processing.common.metrics.CommonProcessingMetrics;
import fr.gouv.vitam.processing.common.model.WorkerBean;
import fr.gouv.vitam.worker.client.exception.WorkerExecutorException;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * manage one worker with n thread
 */
public class WorkerExecutor implements Runnable {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WorkerExecutor.class);
    private final AtomicBoolean mustStop;
    private final PriorityTaskQueue<Runnable> queue;
    private final WorkerBean workerBean;

    public WorkerExecutor(PriorityTaskQueue<Runnable> queue, WorkerBean workerBean) {
        this.workerBean = workerBean;
        this.mustStop = new AtomicBoolean(false);
        this.queue = queue;
    }

    @Override
    public void run() {
        try {
            WorkerInformation.getWorkerThreadLocal().get().setWorkerBean(workerBean);
            while (!mustStop.get()) {
                Runnable task = queue.take();

                // if current thread must stop, we add the taken task to the queue to be treated by another thread
                if (mustStop.get()) {
                    queue.addHighPriorityEntry(task);
                    break;
                }

                // Add metric on the number of tasks in the queue
                CommonProcessingMetrics.WORKER_TASKS_IN_QUEUE.labels(workerBean.getFamily()).dec();

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Start task run on worker: " + workerBean.getName());
                }

                try {
                    task.run();
                } finally {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("End task run on worker: " + workerBean.getName());
                    }
                }
            }
        } catch (Exception e) {
            throw new WorkerExecutorException(workerBean.getWorkerId(), e);
        }
    }

    /**
     * send a message to notify all thread that the worker will be stop.
     */
    public void stop() {
        mustStop.set(true);
    }

    public WorkerBean getWorkerBean() {
        return workerBean;
    }
}
