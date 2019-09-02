package fr.gouv.vitam.common.retryable;

import fr.gouv.vitam.common.exception.VitamRuntimeException;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;

public class RetryableOnResultTest {
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Test
    public void should_retry_on_predicate() {
        // Given
        AtomicInteger counter = new AtomicInteger();
        DelegateRetry<Integer, VitamRuntimeException> delegate = () -> {
            counter.incrementAndGet();
            return counter.get();
        };
        RetryableParameters parameters = new RetryableParameters(3, 1, 1, 1, MILLISECONDS);
        RetryableOnResult<Integer, VitamRuntimeException> retryableOnResult = new RetryableOnResult<>(parameters, r -> r <= 3);

        // When
        Integer result = retryableOnResult.exec(delegate);

        // Then
        assertThat(result).isEqualTo(3);
    }

    @Test
    public void should_retry_on_predicate_times() {
        // Given
        AtomicInteger counter = new AtomicInteger();
        int nbRetry = 10;
        Predicate<Integer> shouldRetryOnPredicate = r -> r < 3;
        RetryableParameters parameters = new RetryableParameters(nbRetry, 1, 1, 1, MILLISECONDS);
        RetryableOnResult<Integer, VitamRuntimeException> retryableOnResult = new RetryableOnResult<>(parameters, shouldRetryOnPredicate);

        // When
        Integer nbRetryDone = retryableOnResult.exec(counter::getAndIncrement);

        // Then
        assertThat(nbRetryDone).isEqualTo(3);
    }
}