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
package fr.gouv.vitam.common.retryable;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class RetryableOnException<T, E extends Exception> implements Retryable<T, E> {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(RetryableOnException.class);
    private static final Consumer<Exception> NOOP = e -> {};
    private static final Predicate<Exception> ALL = e -> true;

    private final AtomicInteger counter = new AtomicInteger();
    private final Random randomSleep = new Random();

    private final Predicate<Exception> retryOn;
    private final Consumer<Exception> onException;

    private RetryableParameters param;

    public RetryableOnException(RetryableParameters param, Predicate<Exception> retryOn, Consumer<Exception> onException) {
        this.param = Objects.requireNonNull(param);
        this.retryOn = Objects.requireNonNull(retryOn);
        this.onException = Objects.requireNonNull(onException);
    }

    public RetryableOnException(RetryableParameters param, Predicate<Exception> retryOn) {
        this(param, retryOn, NOOP);
    }

    public RetryableOnException(RetryableParameters param) {
        this(param, ALL, NOOP);
    }

    @Override
    public T exec(DelegateRetry<T, E> delegate) throws E {
        while (counter.getAndIncrement() < param.getNbRetry()) {
            try {
                LOGGER.warn("Retryable='{}' - Attempt '{}' of '{}'.", delegate.toString(), counter.get(), param.getNbRetry());
                return delegate.call();
            } catch (Exception e) {
                if (counter.get() >= param.getNbRetry() || retryOn.negate().test(e)) {
                    LOGGER.warn("Retryable='{}' - Stop retry at attempt '{}' and throw exception of type '{}'.", counter.get(), e.getClass().getSimpleName());
                    throw e;
                }
                manageCatch(e, delegate.toString());
            }
        }

        throw new IllegalStateException("Unreachable statement.");
    }

    @Override
    public void execute(DelegateRetryVoid<E> delegate) throws E {
        while (counter.getAndIncrement() < param.getNbRetry()) {
            try {
                LOGGER.warn("Retryable='{}' - Attempt '{}' of '{}'.", delegate.toString(), counter.get(), param.getNbRetry());
                delegate.call();
                return;
            } catch (Exception e) {
                if (counter.get() >= param.getNbRetry() || retryOn.negate().test(e)) {
                    LOGGER.warn("Retryable='{}' - Stop retry at attempt '{}' and throw exception of type '{}'.", counter.get(), e.getClass().getSimpleName());
                    throw e;
                }
                manageCatch(e, delegate.toString());
            }
        }

        throw new IllegalStateException("Unreachable statement.");
    }

    private void manageCatch(Exception e, String name) {
        onException.accept(e);
        LOGGER.warn(String.format("Retryable='%s' - Got an exception of type '%s'.", name, e.getClass().getSimpleName()), e);
        sleep(counter.get(), name, LOGGER, param, randomSleep);
    }
}
