/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.storage.offers.tape.cache;

import org.junit.Test;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class LRUQueueTest {

    @Test
    public void emptyQueue() {
        // Given
        LRUQueue<String> instance = new LRUQueue<>();

        // When : no item added

        // Then
        assertThat(instance.isEmpty()).isTrue();
        assertThat(instance.size()).isEqualTo(0);
        assertThat(instance.iterator()).isEmpty();
        assertThat(instance.update("UNKNOWN", 2L)).isFalse();
        assertThat(instance.contains("UNKNOWN")).isFalse();
        assertThat(instance.remove("UNKNOWN")).isFalse();
    }

    @Test
    public void addSingleElementToQueue() {
        // Given
        LRUQueue<String> instance = new LRUQueue<>();

        // When
        instance.add("Entry1", 0L);

        // Then
        assertThat(instance.isEmpty()).isFalse();
        assertThat(instance.size()).isEqualTo(1);
        assertThat(instance.iterator()).containsExactly("Entry1");
    }

    @Test
    public void addMultipleElementsToQueue() {
        // Given
        LRUQueue<String> instance = new LRUQueue<>();

        // When
        instance.add("Entry1", 0L);
        instance.add("Entry2", -1L);
        instance.add("Entry3", 10L);

        // Then
        assertThat(instance.isEmpty()).isFalse();
        assertThat(instance.size()).isEqualTo(3);
        assertThat(instance.iterator()).containsExactly("Entry2", "Entry1", "Entry3");
        assertThat(instance.contains("UNKNOWN")).isFalse();
    }

    @Test
    public void tryAddExistingEntry() {
        // Given
        LRUQueue<String> instance = new LRUQueue<>();
        instance.add("Entry1", 2L);
        instance.add("Entry2", 1L);

        // When / Then
        assertThatThrownBy(() -> instance.add("Entry1", 2L)).isInstanceOf(IllegalArgumentException.class);

        assertThat(instance.isEmpty()).isFalse();
        assertThat(instance.size()).isEqualTo(2);
        assertThat(instance.iterator()).containsExactly("Entry2", "Entry1");
    }

    @Test
    public void updateExistingEntryTimestamp() {
        // Given
        LRUQueue<String> instance = new LRUQueue<>();
        instance.add("Entry2", 1L);
        instance.add("Entry1", 2L);

        // Then
        assertThat(instance.iterator()).containsExactly("Entry2", "Entry1");

        // When
        boolean updated = instance.update("Entry2", 3L);

        // Then
        assertThat(updated).isTrue();
        assertThat(instance.isEmpty()).isFalse();
        assertThat(instance.size()).isEqualTo(2);
        assertThat(instance.iterator()).containsExactly("Entry1", "Entry2");
    }

    @Test
    public void updateNonExistingEntryTimestamp() {
        // Given
        LRUQueue<String> instance = new LRUQueue<>();
        instance.add("Entry2", 1L);
        instance.add("Entry1", 2L);

        // Then
        assertThat(instance.iterator()).containsExactly("Entry2", "Entry1");

        // When
        boolean updated = instance.update("UNKNOWN", 3L);

        // Then
        assertThat(updated).isFalse();
        assertThat(instance.isEmpty()).isFalse();
        assertThat(instance.size()).isEqualTo(2);
        assertThat(instance.iterator()).containsExactly("Entry2", "Entry1");
    }

    @Test
    public void removeExistingUniqueEntry() {
        // Given
        LRUQueue<String> instance = new LRUQueue<>();
        instance.add("Entry1", 2L);

        // When
        boolean removed = instance.remove("Entry1");

        // Then
        assertThat(removed).isTrue();
        assertThat(instance.isEmpty()).isTrue();
        assertThat(instance.size()).isEqualTo(0);
        assertThat(instance.iterator()).isEmpty();
    }

    @Test
    public void removeExistingEntry() {
        // Given
        LRUQueue<String> instance = new LRUQueue<>();
        instance.add("Entry2", 1L);
        instance.add("Entry1", 2L);

        // When
        boolean removed = instance.remove("Entry2");

        // Then
        assertThat(removed).isTrue();
        assertThat(instance.isEmpty()).isFalse();
        assertThat(instance.size()).isEqualTo(1);
        assertThat(instance.iterator()).containsExactly("Entry1");
    }

    @Test
    public void removeNonExistingEntry() {
        // Given
        LRUQueue<String> instance = new LRUQueue<>();
        instance.add("Entry2", 1L);
        instance.add("Entry1", 2L);

        // When
        boolean updated = instance.update("UNKNOWN", 3L);

        // Then
        assertThat(updated).isFalse();
        assertThat(instance.isEmpty()).isFalse();
        assertThat(instance.size()).isEqualTo(2);
        assertThat(instance.iterator()).containsExactly("Entry2", "Entry1");
    }

    @Test
    public void iterateOverEmptyQueue() {
        // Given
        LRUQueue<String> instance = new LRUQueue<>();

        // When
        Iterator<String> iterator = instance.iterator();

        // Then
        assertThat(iterator).isEmpty();
    }

    @Test
    public void iterateOverSingleEntryQueue() {
        // Given
        LRUQueue<String> instance = new LRUQueue<>();
        instance.add("Entry1", 0L);

        // When
        Iterator<String> iterator = instance.iterator();

        // Then
        assertThat(iterator).containsExactly("Entry1");
    }

    @Test
    public void iterateOverSortedQueue() {
        // Given
        LRUQueue<String> instance = new LRUQueue<>();
        instance.add("Entry1", -20_000_000_000_000L);
        instance.add("Entry2", -10L);
        instance.add("Entry3", 0L);
        instance.add("Entry4", 10L);
        instance.add("Entry5", 20_000_000_000_000L);
        instance.add("Entry6", 100_000_000_000_000L);

        // When
        Iterator<String> iterator = instance.iterator();

        // Then
        assertThat(iterator).containsExactly("Entry1", "Entry2", "Entry3", "Entry4", "Entry5", "Entry6");
    }

    @Test
    public void iterateOverUnsortedQueue() {
        // Given
        LRUQueue<String> instance = new LRUQueue<>();
        instance.add("Entry1", 10L);
        instance.add("Entry2", -10L);
        instance.add("Entry3", 100_000_000_000_000L);
        instance.add("Entry4", 0L);
        instance.add("Entry5", 20_000_000_000_000L);
        instance.add("Entry6", -20_000_000_000_000L);

        // When
        Iterator<String> iterator = instance.iterator();

        // Then
        assertThat(iterator).containsExactly("Entry6", "Entry2", "Entry4", "Entry1", "Entry5", "Entry3");
    }

    @Test
    public void iterateOverUpdatedEntryTimestamps() {
        // Given
        LRUQueue<String> instance = new LRUQueue<>();
        instance.add("Entry1", 10L);
        instance.add("Entry2", -10L);
        instance.add("Entry3", 20_000_000_000_000L);
        instance.update("Entry2", 25L);

        // When
        Iterator<String> iterator = instance.iterator();

        // Then
        assertThat(iterator).containsExactly("Entry1", "Entry2", "Entry3");
    }

    @Test
    public void removeEntriesWhileIteratingOverQueueThenAddEntries() {
        // Given
        LRUQueue<String> instance = new LRUQueue<>();
        instance.add("Entry1", 10L);
        instance.add("Entry2", -10L);
        instance.add("Entry3", 100_000_000_000_000L);
        instance.add("Entry4", 0L);
        instance.add("Entry5", 20_000_000_000_000L);
        instance.add("Entry6", -20_000_000_000_000L);

        // Check before
        assertThat(instance.iterator()).containsExactly("Entry6", "Entry2", "Entry4", "Entry1", "Entry5", "Entry3");

        // When
        Iterator<String> iterator = instance.iterator();
        while (iterator.hasNext()) {
            String entry = iterator.next();
            int i = Integer.parseInt(entry.substring("Entry".length()));
            if (i % 2 == 0) {
                iterator.remove();
            }
        }

        // Then
        assertThat(instance.isEmpty()).isFalse();
        assertThat(instance.size()).isEqualTo(3);
        assertThat(instance.iterator()).containsExactly("Entry1", "Entry5", "Entry3");

        // When
        instance.add("Entry2", -1000L);
        instance.add("Entry7", 1000L);

        // Then
        assertThat(instance.isEmpty()).isFalse();
        assertThat(instance.size()).isEqualTo(5);
        assertThat(instance.iterator()).containsExactly("Entry2", "Entry1", "Entry7", "Entry5", "Entry3");
    }

    @Test
    public void testComplexLargeDataSet() {
        // Given
        SecureRandom random = new SecureRandom();
        LRUQueue<String> instance = new LRUQueue<>();
        Map<String, Long> expectedTimestampMap = new HashMap<>();
        int dataSetSize = 10_000;

        // When
        // Insert random data
        IntStream.range(0, dataSetSize)
            // Shuffle (using N x PrimaryNumber % max)
            .map(i -> (i * 7) % dataSetSize)
            .forEach(i -> {
                long randomTimestamp = random.nextLong();
                instance.add("Entry" + i, randomTimestamp);
                expectedTimestampMap.put("Entry" + i, randomTimestamp);
            });

        // Remove some entries explicitly
        for (int i = 4; i < dataSetSize; i += 53) {
            instance.remove("Entry" + i);
            expectedTimestampMap.remove("Entry" + i);
        }

        // Remove some entries using iterator
        Iterator<String> deleteIterator = instance.iterator();
        while (deleteIterator.hasNext()) {
            String entry = deleteIterator.next();
            int i = Integer.parseInt(entry.substring("Entry".length()));
            if (i % 53 == 7) {
                deleteIterator.remove();
                expectedTimestampMap.remove(entry);
            }
        }

        // Update random keys
        for (int i = 9; i < dataSetSize; i += 53) {
            long randomTimestamp = random.nextLong();
            instance.update("Entry" + i, randomTimestamp);
            expectedTimestampMap.put("Entry" + i, randomTimestamp);
        }

        // Insert some random keys
        for (int i = 0; i < 30; i++) {
            long randomTimestamp = random.nextLong();
            instance.add("Entry" + (i + dataSetSize), randomTimestamp);
            expectedTimestampMap.put("Entry" + (i + dataSetSize), randomTimestamp);
        }

        // Then
        assertThat(instance.size()).isEqualTo(expectedTimestampMap.size());
        assertThat(instance.contains("Entry2")).isTrue();
        assertThat(instance.contains("Entry4")).isFalse();
        assertThat(instance.contains("Entry7")).isFalse();
        assertThat(instance.contains("Entry9")).isTrue();
        assertThat(instance.contains("Entry10029")).isTrue();

        Iterator<String> iterator = instance.iterator();
        Long lastEntryTimestamp = Long.MIN_VALUE;

        for (int i = 0; i < expectedTimestampMap.size(); i++) {
            assertThat(iterator.hasNext()).isTrue();
            String currentEntry = iterator.next();
            Long currentEntryTimestamp = expectedTimestampMap.get(currentEntry);
            assertThat(currentEntryTimestamp).isGreaterThanOrEqualTo(lastEntryTimestamp);
            lastEntryTimestamp = currentEntryTimestamp;
        }
        assertThat(iterator.hasNext()).isFalse();
    }
}
