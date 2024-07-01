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
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RetryableOnExceptionTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Test
    public void should_throw_exception_at_end() {
        // Given
        RetryableParameters parameters = new RetryableParameters(3, 1, 1, 1, MILLISECONDS);
        RetryableOnException<Void, VitamRuntimeException> retryable = new RetryableOnException<>(parameters);

        // When
        ThrowingCallable runRetryable = () ->
            retryable.exec(() -> {
                throw new VitamRuntimeException("throw");
            });

        // Then
        assertThatThrownBy(runRetryable).isInstanceOf(VitamRuntimeException.class);
    }

    @Test
    public void should_execute_runnable_times() {
        // Given
        AtomicInteger counter = new AtomicInteger();
        int times = 5;
        DelegateRetry<Void, VitamRuntimeException> delegate = () -> {
            counter.incrementAndGet();
            throw new VitamRuntimeException("throw");
        };
        RetryableParameters parameters = new RetryableParameters(times, 0, 0, 1, MILLISECONDS);
        RetryableOnException<Void, VitamRuntimeException> retryable = new RetryableOnException<>(parameters);

        // When
        ThrowingCallable runRetryable = () -> retryable.exec(delegate);

        // Then
        assertThatThrownBy(runRetryable).isNotNull();
        assertThat(counter.get()).isEqualTo(times);
    }

    @Test
    public void should_not_throw_exception_if_number_of_retries_not_set() {
        // Given
        AtomicInteger counter = new AtomicInteger();
        DelegateRetry<Void, VitamRuntimeException> delegate = () -> {
            counter.incrementAndGet();
            if (counter.get() < 3) {
                throw new VitamRuntimeException("throw");
            }
            return null;
        };
        RetryableParameters parameters = new RetryableParameters(5, 0, 0, 1, MILLISECONDS);
        RetryableOnException<Void, VitamRuntimeException> retryable = new RetryableOnException<>(parameters);

        // When
        ThrowingCallable runRetryable = () -> retryable.exec(delegate);

        // Then
        assertThatCode(runRetryable).doesNotThrowAnyException();
    }

    @Test
    public void should_execute_number_of_times() {
        // Given
        AtomicInteger counter = new AtomicInteger();
        DelegateRetryVoid<VitamRuntimeException> delegate = () -> {
            counter.incrementAndGet();
            if (counter.get() < 3) {
                throw new VitamRuntimeException("throw");
            }
        };
        RetryableParameters parameters = new RetryableParameters(3, 1, 1, 1, MILLISECONDS);
        RetryableOnException<Void, VitamRuntimeException> retryable = new RetryableOnException<>(parameters);

        // When
        retryable.execute(delegate);

        // Then
        assertThat(counter.get()).isEqualTo(3);
    }

    @Test
    public void should_throw_when_shouldRetry_predicate_is_false() {
        // Given
        AtomicInteger counter = new AtomicInteger();
        DelegateRetryVoid<VitamRuntimeException> delegate = () -> {
            counter.incrementAndGet();
            throw new VitamRuntimeException("throw");
        };
        RetryableParameters parameters = new RetryableParameters(3, 1, 1, 1, MILLISECONDS);
        RetryableOnException<Void, VitamRuntimeException> retryable = new RetryableOnException<>(
            parameters,
            e -> false
        );

        // When
        ThrowingCallable runRetryable = () -> retryable.execute(delegate);

        // Then
        assertThatThrownBy(runRetryable).isInstanceOf(VitamRuntimeException.class);
        assertThat(counter.get()).isEqualTo(1);
    }

    @Test
    public void should_never_throw_IllegalStateException_when_retry_finish() {
        // Given
        DelegateRetry<Void, VitamRuntimeException> delegate = () -> {
            throw new VitamRuntimeException("throw");
        };
        RetryableParameters parameters = new RetryableParameters(1, 1, 1, 1, MILLISECONDS);
        RetryableOnException<Void, VitamRuntimeException> retryable = new RetryableOnException<>(parameters);

        // When
        ThrowingCallable runRetryable = () -> retryable.exec(delegate);

        // Then
        assertThatThrownBy(runRetryable).isInstanceOf(VitamRuntimeException.class);
    }
}
