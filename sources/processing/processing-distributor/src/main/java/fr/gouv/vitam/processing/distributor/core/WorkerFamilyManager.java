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

import fr.gouv.vitam.common.thread.VitamThreadFactory;
import fr.gouv.vitam.processing.common.metrics.CommonProcessingMetrics;
import fr.gouv.vitam.processing.common.model.WorkerBean;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

// manage many worker per worker family
public class WorkerFamilyManager {

    private final String family;

    private final PriorityTaskQueue<Runnable> queue;

    private final Map<String, WorkerExecutor> workers = new ConcurrentHashMap<>();

    public WorkerFamilyManager(String family, int queueSize) {
        if (queueSize < 2) {
            throw new IllegalArgumentException("queue size must be greater than 2");
        }
        this.family = family;
        queue = new PriorityTaskQueue<>(queueSize);
    }

    public void registerWorker(WorkerBean workerBean) {
        workers.computeIfAbsent(workerBean.getWorkerId(), key -> {
            WorkerExecutor executor = new WorkerExecutor(queue, workerBean);
            for (int i = 0; i < workerBean.getCapacity(); i++) {
                final Thread thread = VitamThreadFactory.getInstance().newThread(executor);
                thread.setName("WorkerExecutor_" + workerBean.getWorkerId() + "_" + i);
                thread.start();
            }

            CommonProcessingMetrics.REGISTERED_WORKERS.labels(family).inc();

            return executor;
        });
    }

    /**
     * @param workerId the id of worker to unregister
     */
    public void unregisterWorker(String workerId) {
        final WorkerExecutor workerExecutor = workers.get(workerId);
        if (workerExecutor != null) {
            workerExecutor.stop();
            workers.remove(workerId);

            CommonProcessingMetrics.REGISTERED_WORKERS.labels(family).dec();
        }
    }

    public Executor getExecutor(boolean isHighPriorityTask) {
        return command -> {
            try {
                CommonProcessingMetrics.WORKER_TASKS_IN_QUEUE.labels(family).inc();

                if (isHighPriorityTask) {
                    queue.addHighPriorityEntry(command);
                } else {
                    queue.addRegularEntry(command);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        };
    }

    public Map<String, WorkerExecutor> getWorkers() {
        return workers;
    }

    public String getFamily() {
        return family;
    }
}
