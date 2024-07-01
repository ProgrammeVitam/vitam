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
package fr.gouv.vitam.common.stream;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Producer-Consumer lock implementation : (https://en.wikipedia.org/wiki/Producer%E2%80%93consumer_problem)
 * Inspired from https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/locks/Condition.html implementation
 * Implements {@link AutoCloseable} - When closed, all locks are unlocked.
 */
public class ProducerConsumerLock implements AutoCloseable {

    private final Lock lock = new ReentrantLock();
    private final Condition canWrite = lock.newCondition();
    private final Condition canRead = lock.newCondition();

    private int writeCapacity;
    private int readCapacity;
    private volatile boolean closed = false;

    public ProducerConsumerLock(int bufferCapacity) {
        writeCapacity = bufferCapacity;
        readCapacity = 0;
    }

    /**
     * Waits until enough units are available for write, or lock closed.
     *
     * @return true if enough write units reserved. false if closed.
     */
    public boolean tryBeginProduce(int units) throws InterruptedException {
        lock.lock();
        try {
            while (!closed && writeCapacity < units) {
                canWrite.await();
            }
            writeCapacity -= units;

            return !closed;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Notifies consumer of available units to consume.
     */
    public void endProduce(int units) {
        lock.lock();
        try {
            readCapacity += units;
            canRead.signal();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Waits until 1..units are available for read, or lock closed.
     *
     * @return The number of available units to read (1 .. units). 0 if closed.
     */
    public int tryBeginConsume(int units) throws InterruptedException {
        lock.lock();
        try {
            while (!closed && readCapacity == 0) {
                canRead.await();
            }

            int immediatelyAvailable = Math.min(units, readCapacity);
            readCapacity -= immediatelyAvailable;

            return immediatelyAvailable;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Notifies writer of available units to write.
     */
    public void endConsume(int units) {
        lock.lock();
        try {
            writeCapacity += units;
            canWrite.signal();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Closes locks and notifies reader &amp; producer
     */
    public void close() {
        lock.lock();
        try {
            closed = true;
            canRead.signal();
            canWrite.signal();
        } finally {
            lock.unlock();
        }
    }
}
