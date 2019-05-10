package fr.gouv.vitam.common.iterables;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BulkBufferingEntryIteratorTest {

    @Test
    public void testEmpty() {

        // Given
        Supplier<List<String>> supplier = mock(Supplier.class);
        Iterator<String> instance = new BulkBufferingEntryIterator<String>(10) {
            @Override
            protected List<String> loadNextChunk(int chunkSize) {
                return supplier.get();
            }
        };
        when(supplier.get()).thenReturn(Collections.emptyList());

        // When / Then
        assertThat(instance.hasNext()).isFalse();
        assertThatThrownBy(instance::next).isInstanceOf(NoSuchElementException.class);

        verify(supplier).get();
    }

    @Test
    public void testOnePage() {

        // Given
        Supplier<List<String>> supplier = mock(Supplier.class);
        Iterator<String> instance = new BulkBufferingEntryIterator<String>(10) {
            @Override
            protected List<String> loadNextChunk(int chunkSize) {
                return supplier.get();
            }
        };
        when(supplier.get()).thenReturn(Arrays.asList("1", "2", "3"));

        // When / Then
        assertThat(instance.hasNext()).isTrue();
        assertThat(instance.next()).isEqualTo("1");
        assertThat(instance.hasNext()).isTrue();
        assertThat(instance.next()).isEqualTo("2");
        assertThat(instance.hasNext()).isTrue();
        assertThat(instance.next()).isEqualTo("3");
        assertThat(instance.hasNext()).isFalse();
        assertThatThrownBy(instance::next).isInstanceOf(NoSuchElementException.class);

        verify(supplier).get();
    }

    @Test
    public void testExactlyOnePage() {

        // Given
        Supplier<List<String>> supplier = mock(Supplier.class);
        Iterator<String> instance = new BulkBufferingEntryIterator<String>(3) {
            @Override
            protected List<String> loadNextChunk(int chunkSize) {
                return supplier.get();
            }
        };
        when(supplier.get()).thenReturn(
            Arrays.asList("1", "2", "3"),
            Collections.emptyList());

        // When / Then
        assertThat(instance.hasNext()).isTrue();
        assertThat(instance.next()).isEqualTo("1");
        assertThat(instance.hasNext()).isTrue();
        assertThat(instance.next()).isEqualTo("2");
        assertThat(instance.hasNext()).isTrue();
        assertThat(instance.next()).isEqualTo("3");
        assertThat(instance.hasNext()).isFalse();
        assertThatThrownBy(instance::next).isInstanceOf(NoSuchElementException.class);

        verify(supplier, times(2)).get();
    }

    @Test
    public void testMultiPages() {

        // Given
        Supplier<List<String>> supplier = mock(Supplier.class);
        Iterator<String> instance = new BulkBufferingEntryIterator<String>(2) {
            @Override
            protected List<String> loadNextChunk(int chunkSize) {
                return supplier.get();
            }
        };
        when(supplier.get()).thenReturn(
            Arrays.asList("1", "2"),
            Collections.singletonList("3"));

        // When / Then
        assertThat(instance.hasNext()).isTrue();
        assertThat(instance.next()).isEqualTo("1");
        assertThat(instance.hasNext()).isTrue();
        assertThat(instance.next()).isEqualTo("2");
        assertThat(instance.hasNext()).isTrue();
        assertThat(instance.next()).isEqualTo("3");
        assertThat(instance.hasNext()).isFalse();
        assertThatThrownBy(instance::next).isInstanceOf(NoSuchElementException.class);

        verify(supplier, times(2)).get();
    }
}
