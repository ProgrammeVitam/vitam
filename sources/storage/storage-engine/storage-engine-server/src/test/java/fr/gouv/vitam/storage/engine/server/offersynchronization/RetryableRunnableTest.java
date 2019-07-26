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

import fr.gouv.vitam.storage.engine.server.exception.RuntimeStorageException;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RetryableRunnableTest {
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Test
    public void should_throw_exception_at_end() {
        // Given
        AtomicInteger counter = new AtomicInteger();
        Runnable runnable = RetryableRunnable.from(5, () -> {
            counter.incrementAndGet();
            throw new RuntimeStorageException("throw");
        }, 0, 0);

        // When
        ThrowingCallable runRetryableRunnable = runnable::run;

        // Then
        assertThatThrownBy(runRetryableRunnable).isInstanceOf(RuntimeStorageException.class);
    }

    @Test
    public void should_execute_runnable_times() {
        // Given
        AtomicInteger counter = new AtomicInteger();
        Runnable runnable = RetryableRunnable.from(5, () -> {
            counter.incrementAndGet();
            throw new RuntimeStorageException("throw");
        }, 0, 0);

        // When
        ThrowingCallable runRetryableRunnable = runnable::run;

        // Then
        assertThatThrownBy(runRetryableRunnable).isNotNull();
        assertThat(counter.get()).isEqualTo(5);
    }

    @Test
    public void should_not_throw_exception_if_number_of_retries_not_set() {
        // Given
        AtomicInteger counter = new AtomicInteger();
        Runnable runnable = RetryableRunnable.from(5, () -> {
            counter.incrementAndGet();
            if (counter.get() < 3) {
                throw new RuntimeStorageException("throw");
            }
        }, 0, 0);

        // When
        ThrowingCallable runRunnable = runnable::run;

        // Then
        assertThatCode(runRunnable).doesNotThrowAnyException();
    }

    @Test
    public void should_execute_number_of_times() {
        // Given
        AtomicInteger counter = new AtomicInteger();
        Runnable runnable = RetryableRunnable.from(5, () -> {
            counter.incrementAndGet();
            if (counter.get() < 3) {
                throw new RuntimeStorageException("throw");
            }
        }, 0, 0);

        // When
        runnable.run();

        // Then
        assertThat(counter.get()).isEqualTo(3);
    }
}