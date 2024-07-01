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

import fr.gouv.vitam.common.ParametersChecker;

import javax.annotation.concurrent.ThreadSafe;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A task queue backed by a "regular priority" bounded blocking queue, and a "high-priority" non-blocking queue.
 * Adding regular entries may be blocking is max capacity reached.
 * Adding high-priority entries is non-blocking.
 * Retrieving entries from queue may be blocking if queue is empty (no high-priority or regular entry present).
 * High-priority entries have precedence over regular entries.
 *
 * This class is thread-safe.
 *
 * @param <T> the type of entries held in this queue
 */
@ThreadSafe
public class PriorityTaskQueue<T> {

    private final Lock lock = new ReentrantLock(true);
    private final Condition notEmpty = lock.newCondition();
    private final Condition notFullRegularQueue = lock.newCondition();

    private final Queue<T> regularQueue = new LinkedList<>();
    private final Queue<T> priorityQueue = new LinkedList<>();
    private final int maxRegularQueueSize;

    /**
     * Constructor of the {@link PriorityTaskQueue<T>}
     *
     * @param maxRegularQueueSize the max size of regular queue.
     * @throws IllegalArgumentException if queue size in invalid.
     */
    public PriorityTaskQueue(int maxRegularQueueSize) {
        ParametersChecker.checkValue("Max queue size must be > 0", maxRegularQueueSize, 1);
        this.maxRegularQueueSize = maxRegularQueueSize;
    }

    /**
     * Adds a regular entry to the queue.
     * Attempts to add entries on full regular queue causes caller thread to be blocked until enough space is available.
     *
     * @param entry the entry to add to regular queue
     * @throws IllegalArgumentException if entry is null
     * @throws InterruptedException if caller thread is interrupted
     */
    public void addRegularEntry(T entry) throws InterruptedException {
        ParametersChecker.checkParameter("Missing queue entry", entry);

        lock.lock();
        try {
            while (regularQueue.size() == maxRegularQueueSize) {
                notFullRegularQueue.await();
            }

            regularQueue.add(entry);
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Adds a high-priority entry to the queue.
     *
     * @param entry the entry to add to high-priority queue
     */
    public void addHighPriorityEntry(T entry) {
        ParametersChecker.checkParameter("Missing queue entry", entry);

        lock.lock();
        try {
            priorityQueue.add(entry);
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Removes and returns next entry from the queue.
     * High-priority entries have precedence over regular entries.
     * Attempts to remove entries from empty queue causes caller thread to be blocked until new entries are added to queue.
     *
     * @return the next entry removed from queue
     * @throws InterruptedException if caller thread is interrupted
     */
    public T take() throws InterruptedException {
        lock.lock();
        try {
            while (priorityQueue.isEmpty() && regularQueue.isEmpty()) {
                notEmpty.await();
            }

            if (!priorityQueue.isEmpty()) {
                return priorityQueue.remove();
            }

            T result = regularQueue.remove();
            notFullRegularQueue.signal();
            return result;
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        lock.lock();
        try {
            return priorityQueue.size() + regularQueue.size();
        } finally {
            lock.unlock();
        }
    }
}
