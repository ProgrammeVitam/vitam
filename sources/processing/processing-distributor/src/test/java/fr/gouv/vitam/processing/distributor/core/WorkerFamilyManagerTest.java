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

import fr.gouv.vitam.processing.common.model.WorkerBean;
import fr.gouv.vitam.processing.common.model.WorkerRemoteConfiguration;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.fail;

public class WorkerFamilyManagerTest {

    @Test
    public void must_run_tasks() {
        WorkerFamilyManager wfm = new WorkerFamilyManager("familyId", 3);
        WorkerBean workerBean1 = new WorkerBean(
            "worker1",
            "familyId",
            1,
            "active",
            new WorkerRemoteConfiguration("host", 0)
        );
        WorkerBean workerBean2 = new WorkerBean(
            "worker2",
            "familyId",
            1,
            "active",
            new WorkerRemoteConfiguration("host", 0)
        );
        workerBean1.setWorkerId("workerId1");
        workerBean2.setWorkerId("workerId2");
        wfm.registerWorker(workerBean1);
        wfm.registerWorker(workerBean2);
        AtomicReference<String> threadName = new AtomicReference<>("");
        AtomicReference<CompletableFuture<Void>> task3 = new AtomicReference<>();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        // task to run on one of worker
        CompletableFuture<Void> task1 = CompletableFuture.runAsync(
            () -> {
                try {
                    countDownLatch.await();
                } catch (InterruptedException e) {
                    fail(e.getMessage());
                }
            },
            wfm.getExecutor(false)
        );
        // task
        CompletableFuture<Void> task2 = CompletableFuture.runAsync(
            () -> {
                // we add task 3 to the queue
                task3.set(
                    CompletableFuture.runAsync(
                        () -> {
                            // must be executed in worker 2
                            threadName.set(Thread.currentThread().getName());
                        },
                        wfm.getExecutor(false)
                    )
                );
                wfm.unregisterWorker("workerId1"); // unregister worker 1
                countDownLatch.countDown();
            },
            wfm.getExecutor(false)
        );
        // wait until task 1 and task 2 completed
        CompletableFuture.allOf(task1, task2).join();

        // wait until task 3 finish
        task3.get().join();
        wfm.unregisterWorker("workerId2");
        // verify that task 3 has been executed on worker 2
        Assert.assertEquals("WorkerExecutor_workerId2_0", threadName.get());

        CompletableFuture<Void> task4 = CompletableFuture.runAsync(() -> {}, wfm.getExecutor(false));

        // we don't execute task 4 because there is no worker registred
        Assertions.assertThatThrownBy(() -> task4.get(10, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
    }
}
