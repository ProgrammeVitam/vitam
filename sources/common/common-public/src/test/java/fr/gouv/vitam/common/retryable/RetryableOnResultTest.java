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
        RetryableOnResult<Integer, VitamRuntimeException> retryableOnResult = new RetryableOnResult<>(
            parameters,
            r -> r <= 3
        );

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
        RetryableOnResult<Integer, VitamRuntimeException> retryableOnResult = new RetryableOnResult<>(
            parameters,
            shouldRetryOnPredicate
        );

        // When
        Integer nbRetryDone = retryableOnResult.exec(counter::getAndIncrement);

        // Then
        assertThat(nbRetryDone).isEqualTo(3);
    }
}
