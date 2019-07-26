/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
package fr.gouv.vitam.storage.engine.server.offersynchronization;

import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.storage.engine.server.exception.RuntimeStorageException;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class RetryableRunnable implements Runnable {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(RetryableRunnable.class);
    private final static int DEFAULT_NUMBER_OF_RETRIES = 3;
    private static final int DEFAULT_FIRST_ATTEMPT_WAITING_TIME = 15;
    private static final int DEFAULT_WAITING_TIME = 30;

    private final AtomicInteger counter = new AtomicInteger();

    private final int times;
    private final Runnable runnable;
    private final int firstAttemptWaitingTime;
    private final int waitingTime;

    private RetryableRunnable(int times, Runnable runnable, int firstAttemptWaitingTime, int waitingTime) {
        this.times = times;
        this.runnable = runnable;
        this.firstAttemptWaitingTime = firstAttemptWaitingTime;
        this.waitingTime = waitingTime;
    }

    public static RetryableRunnable from(Runnable runnable) {
        return new RetryableRunnable(DEFAULT_NUMBER_OF_RETRIES, runnable, DEFAULT_FIRST_ATTEMPT_WAITING_TIME, DEFAULT_WAITING_TIME);
    }

    public static RetryableRunnable from(int times, Runnable runnable, int firstAttemptWaitingTime, int waitingTime) {
        return new RetryableRunnable(times, runnable, firstAttemptWaitingTime, waitingTime);
    }

    @Override
    public void run() {
        while (counter.getAndIncrement() < times) {
            try {
                runnable.run();
                return;
            } catch (RuntimeStorageException e) {
                if (counter.get() >= times) {
                    throw e;
                }
                LOGGER.warn("Got an exception '{}' from a retryable runnable '{}'.", e.getMessage(), runnable.toString());
                sleep();
                LOGGER.warn("Retry runnable '{}', attempt '{}' of '{}'.", runnable.toString(), counter.get(), times);
            }
        }
    }

    private void sleep() {
        try {
            LOGGER.warn("Will retry runnable '{}' in '{}' seconds.", runnable.toString(), getSleepTimeInSeconds());
            TimeUnit.SECONDS.sleep(getSleepTimeInSeconds());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new VitamRuntimeException(String.format("Error while trying to wait in retryable runnable: '%s'.", runnable.toString()), e);
        }
    }

    private long getSleepTimeInSeconds() {
        boolean isFirstAttempt = counter.get() == 1;
        if (isFirstAttempt) {
            return firstAttemptWaitingTime;
        }
        return waitingTime;
    }
}
