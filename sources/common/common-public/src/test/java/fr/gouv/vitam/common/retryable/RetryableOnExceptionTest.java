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
        ThrowingCallable runRetryable = () -> retryable.exec(() -> { throw new VitamRuntimeException("throw"); });

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
        RetryableOnException<Void, VitamRuntimeException> retryable = new RetryableOnException<>(parameters, e -> false);

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