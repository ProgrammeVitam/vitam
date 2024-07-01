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

package fr.gouv.vitam.common.thread;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class ExecutorUtils {

    private ExecutorUtils() {
        // private constructor for static class
    }

    public static ThreadPoolExecutor createScalableBatchExecutorService(int maxBatchThreadPoolSize) {
        // Do not use a corePoolSize < maximumPoolSize with a LinkedBlockingQueue based queue
        // Otherwise, maximumPoolSize will never be reached
        // See https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/concurrent/ThreadPoolExecutor.html
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
            maxBatchThreadPoolSize,
            maxBatchThreadPoolSize,
            1L,
            TimeUnit.MINUTES,
            new LinkedBlockingQueue<>(),
            VitamThreadFactory.getInstance()
        );
        threadPoolExecutor.allowCoreThreadTimeOut(true);
        return threadPoolExecutor;
    }

    public static ThreadPoolExecutor createScalableBatchExecutorService(
        int maxBatchThreadPoolSize,
        int maxQueueSizeBeforeBlocking
    ) {
        // ThreadPool executor with limited queue size
        // Once queue size is reached, producer is blocked to limit active workset
        // https://stackoverflow.com/a/52059445/106971
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
            maxBatchThreadPoolSize,
            maxBatchThreadPoolSize,
            1L,
            TimeUnit.MINUTES,
            new ThreadPoolQueue<>(maxQueueSizeBeforeBlocking),
            VitamThreadFactory.getInstance()
        );
        threadPoolExecutor.allowCoreThreadTimeOut(true);
        return threadPoolExecutor;
    }

    private static class ThreadPoolQueue<T> extends ArrayBlockingQueue<T> {

        public ThreadPoolQueue(int capacity) {
            super(capacity);
        }

        @Override
        public boolean offer(T e) {
            try {
                put(e);
            } catch (InterruptedException e1) {
                Thread.currentThread().interrupt();
                return false;
            }
            return true;
        }
    }
}
