/*******************************************************************************
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
 *******************************************************************************/
package fr.gouv.vitam.common.database.utils;

import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Spliterator;
import java.util.function.Function;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.model.RequestResponseOK;
import org.junit.Test;

public class LifecyclesSpliteratorTest {


    @Test
    public void should_invoke_function_when_try_to_advance() {
        // Given
        Function function = mock(Function.class);
        RequestResponseOK<Long> longRequestResponseOK = new RequestResponseOK<>();

        // longRequestResponseOK.setHits(new DatabaseCursor(4L, Long.valueOf(Integer.valueOf("3")), 4L));
        given(function.apply(any(Select.class))).willReturn(longRequestResponseOK);

        Select query = new Select();
        Spliterator<Long> longSpliterator = new LifecyclesSpliterator<>(query, function, 0, 4);

        // When
        longSpliterator.forEachRemaining(item -> {
        });

        // Then
        // actually LifecyclesSpliterator execute query one more time to check if shall stop
        // change to verify(function).apply(query); after fixing LifecyclesSpliterator
        verify(function, times(2)).apply(query);
    }

    @Test
    public void should_invoke_function_two_times_when_try_to_advance() {
        // Given
        Function function = mock(Function.class);
        RequestResponseOK<Long> requestResponseOK1 = new RequestResponseOK<>();
        requestResponseOK1.addResult(1L);
        requestResponseOK1.addResult(2L);
        requestResponseOK1.addResult(3L);
        requestResponseOK1.setHits(4, 0, 3, 3);
        
        RequestResponseOK<Long> requestResponseOK2 = new RequestResponseOK<>();
        requestResponseOK2.addResult(4L);
        requestResponseOK2.setHits(4, 3, 3, 1);

        RequestResponseOK<Long> requestResponseOK3 = new RequestResponseOK<>();
        
        given(function.apply(any(Select.class))).willReturn(requestResponseOK1)
            .willReturn(requestResponseOK2)
            .willReturn(requestResponseOK3);
        
        Select query = new Select();
        Spliterator<Long> longSpliterator = new LifecyclesSpliterator<>(query, function, 3, 4);

        // When
        longSpliterator.forEachRemaining(item -> {
        });

        // Then
        // actually LifecyclesSpliterator execute query one more time to check if shall stop
        // change to verify(function, times(2)).apply(query); after fixing LifecyclesSpliterator
        verify(function, times(3)).apply(query);
    }
}
