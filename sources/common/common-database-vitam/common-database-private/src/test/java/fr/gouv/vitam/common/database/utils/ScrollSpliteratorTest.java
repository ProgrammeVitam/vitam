package fr.gouv.vitam.common.database.utils;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Spliterator;
import java.util.function.Function;

import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.model.RequestResponseOK;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.BDDMockito;

public class ScrollSpliteratorTest {

    @Test
    public void should_invoke_function_when_try_to_advance() {
        // Given
        Function function = mock(Function.class);
        BDDMockito.given(function.apply(any(SelectMultiQuery.class))).willReturn(new RequestResponseOK<Long>());

        SelectMultiQuery query = new SelectMultiQuery();
        Spliterator<Long> longSpliterator = new ScrollSpliterator<>(query, function, 3, 4);

        // When
        longSpliterator.forEachRemaining(item -> {});

        // Then
        verify(function).apply(query);
    }

    @Test
    public void should_invoke_function_two_times_when_try_to_advance() {
        // Given
        Function function = mock(Function.class);
        RequestResponseOK<Long> requestResponseOK = new RequestResponseOK<>();
        requestResponseOK.setHits(4, 0, 3, 3);
        BDDMockito.given(function.apply(any(SelectMultiQuery.class))).willReturn(requestResponseOK);


        SelectMultiQuery query = new SelectMultiQuery();
        Spliterator<Long> longSpliterator = new ScrollSpliterator<>(query, function, 3, 4);

        // When
        longSpliterator.forEachRemaining(item -> {});

        // Then
        Assertions.assertThat(longSpliterator.estimateSize()).isEqualTo(4);
        verify(function, times(2)).apply(query);
    }

}