/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.retryable;

import fr.gouv.vitam.common.logging.VitamLogLevel;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static fr.gouv.vitam.common.logging.VitamLogLevel.ERROR;

public class RetryableParameters {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(Retryable.class);

    private final int nbRetry;
    private final int firstAttemptWaitingTime;
    private final int waitingTime;
    private final int randomRangeSleep;
    private final TimeUnit timeUnit;
    private final Consumer<String> log;

    public RetryableParameters(
        int nbRetry,
        int firstAttemptWaitingTime,
        int waitingTime,
        int randomRangeSleep,
        TimeUnit timeUnit,
        VitamLogLevel level
    ) {
        this.nbRetry = nbRetry;
        this.firstAttemptWaitingTime = firstAttemptWaitingTime;
        this.waitingTime = waitingTime;
        this.randomRangeSleep = randomRangeSleep;
        this.timeUnit = timeUnit;
        this.log = s -> LOGGER.log(level, s);
    }

    public RetryableParameters(
        int nbRetry,
        int firstAttemptWaitingTime,
        int waitingTime,
        int randomRangeSleep,
        TimeUnit timeUnit
    ) {
        this.nbRetry = nbRetry;
        this.firstAttemptWaitingTime = firstAttemptWaitingTime;
        this.waitingTime = waitingTime;
        this.randomRangeSleep = randomRangeSleep;
        this.timeUnit = timeUnit;
        this.log = s -> LOGGER.log(ERROR, s);
    }

    public int getNbRetry() {
        return nbRetry;
    }

    public int getFirstAttemptWaitingTime() {
        return firstAttemptWaitingTime;
    }

    public int getWaitingTime() {
        return waitingTime;
    }

    public int getRandomRangeSleep() {
        return randomRangeSleep;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public Consumer<String> getLog() {
        return log;
    }
}
