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

package fr.gouv.vitam.processing.distributor.core;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;

import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.thread.VitamThreadFactory;
import fr.gouv.vitam.processing.common.model.WorkerBean;

// manage many worker per worker family
public class WorkerFamilyManager implements Executor {


    private BlockingQueue<Runnable> queue;

    private Map<String, WorkerExecutor> workers = new HashMap<>();

    public WorkerFamilyManager(int queueSize) {
        if (queueSize < 2) {
            throw new IllegalArgumentException("queue size must be greater than 2");
        }
        queue = new ArrayBlockingQueue<>(queueSize, true);
    }

    public void registerWorker(WorkerBean workerBean) {
        if (workers.containsKey(workerBean.getWorkerId())) {
            throw new IllegalArgumentException("worker already register");
        }
        WorkerExecutor executor = new WorkerExecutor(queue, workerBean);
        workers.put(workerBean.getWorkerId(), executor);
        for (int i = 0; i < workerBean.getCapacity(); i++) {
            final Thread thread = VitamThreadFactory.getInstance().newThread(executor);
            thread.setName("WorkerExecutor_"+GUIDFactory.newGUID().getId());
            thread.start();
        }
    }

    /**
     * @param workerId
     */
    public void unregisterWorker(String workerId) {

        final WorkerExecutor workerExecutor = workers.get(workerId);
        if (workerExecutor != null) {
            workerExecutor.stop();
            workers.remove(workerId);
        }
    }

    @Override
    public void execute(Runnable command) {
        try {
            queue.put(command);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, WorkerExecutor> getWorkers() {
        return workers;
    }
}
