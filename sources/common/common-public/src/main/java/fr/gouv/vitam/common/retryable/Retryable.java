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

import fr.gouv.vitam.common.exception.VitamRuntimeException;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public interface Retryable<T, E extends Exception> {
    T exec(DelegateRetry<T, E> delegate) throws E;

    void execute(DelegateRetryVoid<E> delegate) throws E;

    default void sleep(
        int attempt,
        String name,
        RetryableParameters param,
        SecureRandom randomSleep,
        Consumer<T> onResult,
        T type
    ) {
        onResult.accept(type);
        String resultString = type.toString();
        doSleep(attempt, name, param, randomSleep, resultString);
    }

    default void sleep(
        int attempt,
        String name,
        RetryableParameters param,
        SecureRandom randomSleep,
        Consumer<Exception> onException,
        Exception exception
    ) {
        onException.accept(exception);
        String stackTrace = ExceptionUtils.getStackTrace(exception);
        doSleep(attempt, name, param, randomSleep, stackTrace);
    }

    default void doSleep(
        int attempt,
        String name,
        RetryableParameters param,
        SecureRandom randomSleep,
        String toPrint
    ) {
        try {
            int randomRangeSleep = param.getRandomRangeSleep() == 0
                ? 0
                : randomSleep.nextInt(param.getRandomRangeSleep());

            long sleepTime = attempt == 1
                ? randomRangeSleep + param.getFirstAttemptWaitingTime()
                : randomRangeSleep + param.getWaitingTime();

            TimeUnit timeUnit = param.getTimeUnit();
            param
                .getLog()
                .accept(
                    String.format(
                        "Retryable='%s' - Will retry, attempt '%d' in '%d' %s. %s",
                        name,
                        attempt,
                        sleepTime,
                        timeUnit.name(),
                        toPrint
                    )
                );
            timeUnit.sleep(sleepTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new VitamRuntimeException(String.format("Error while trying to wait in retryable: '%s'.", name), e);
        }
    }
}
