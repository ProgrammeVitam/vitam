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
package fr.gouv.vitam.processing.distributor.core;

import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PriorityTaskQueueTest {

    @Test
    public void givenBadQueueSizeThanKO() {
        assertThatThrownBy(() -> new PriorityTaskQueue<String>(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PriorityTaskQueue<String>(-10)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void givenNullEntryWhenAddingRegularEntryThenKO() {
        // Given
        PriorityTaskQueue<String> instance = new PriorityTaskQueue<>(10);

        // When / Then
        assertThatThrownBy(() -> instance.addRegularEntry(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void givenNullEntryWhenAddingHighPriorityEntryThenKO() {
        // Given
        PriorityTaskQueue<String> instance = new PriorityTaskQueue<>(10);

        // When / Then
        assertThatThrownBy(() -> instance.addHighPriorityEntry(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void givenEmptyQueueWhenAddingRegularEntryThenOK() throws InterruptedException {
        // Given
        PriorityTaskQueue<String> instance = new PriorityTaskQueue<>(10);

        // When
        instance.addRegularEntry("entry1");

        // Then
        assertThat(instance.size()).isEqualTo(1);
        assertThat(instance.take()).isEqualTo("entry1");
    }

    @Test
    public void givenEmptyQueueWhenAddingPriorityEntryThenOK() throws InterruptedException {
        // Given
        PriorityTaskQueue<String> instance = new PriorityTaskQueue<>(10);

        // When
        instance.addHighPriorityEntry("entry1");

        // Then
        assertThat(instance.size()).isEqualTo(1);
        assertThat(instance.take()).isEqualTo("entry1");
    }

    @Test
    public void givenEmptyQueueWhenTryTakingElementBlockingUntilRegularEntryAdded() throws Exception {
        // Given
        PriorityTaskQueue<String> instance = new PriorityTaskQueue<>(10);

        // When
        CompletableFuture<String> completableFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return instance.take();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        // Then
        assertThat(instance.size()).isEqualTo(0);
        TimeUnit.SECONDS.sleep(2);
        assertThat(completableFuture.isDone()).isFalse();

        // When
        instance.addRegularEntry("entry1");

        // Then
        String result = completableFuture.get(2, TimeUnit.SECONDS);
        assertThat(result).isEqualTo("entry1");
        assertThat(instance.size()).isEqualTo(0);
    }

    @Test
    public void givenRegularAndHighPriorityEntriesWhenTakeEntriesThenHighPriorityEntriesSelectedFirst()
        throws Exception {
        // Given
        PriorityTaskQueue<String> instance = new PriorityTaskQueue<>(10);
        instance.addRegularEntry("entry1");
        instance.addHighPriorityEntry("entry2");
        instance.addHighPriorityEntry("entry3");
        instance.addRegularEntry("entry4");

        // When
        List<String> results = new ArrayList<>();
        while (instance.size() > 0) {
            results.add(instance.take());
        }

        // Then
        assertThat(results).hasSize(4);
        assertThat(results).containsExactly("entry2", "entry3", "entry1", "entry4");
    }

    @Test
    public void givenMultipleThreadsAddingEntriesToQueueThenOK() throws Exception {
        // Given
        ExecutorService executor = Executors.newFixedThreadPool(10);
        PriorityTaskQueue<String> instance = new PriorityTaskQueue<>(10);

        // 5 "regular" writers
        List<CompletableFuture<Void>> regularCompletableFutures = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            String writerId = "regular-" + i;
            regularCompletableFutures.add(
                CompletableFuture.runAsync(
                    () -> {
                        for (int j = 0; j < 10; j++) {
                            try {
                                TimeUnit.MILLISECONDS.sleep(ThreadLocalRandom.current().nextInt(0, 200));
                                instance.addRegularEntry(writerId + "-" + j);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                throw new RuntimeException(e);
                            }
                        }
                    },
                    executor
                )
            );
        }

        // 5 "priority" writers
        List<CompletableFuture<Void>> highPriorityCompletableFutures = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            String writerId = "high-priority-" + i;
            highPriorityCompletableFutures.add(
                CompletableFuture.runAsync(
                    () -> {
                        for (int j = 0; j < 10; j++) {
                            try {
                                TimeUnit.MILLISECONDS.sleep(ThreadLocalRandom.current().nextInt(0, 200));
                                instance.addHighPriorityEntry(writerId + "-" + j);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                throw new RuntimeException(e);
                            }
                        }
                    },
                    executor
                )
            );
        }

        // Then
        TimeUnit.MILLISECONDS.sleep(4000);

        // Expected all 50 high priority entries to be added, and 10 regular entries
        assertThat(instance.size()).isEqualTo(50 + 10);

        assertThat(highPriorityCompletableFutures)
            .withFailMessage("High priority entries should be non-blocking")
            .allMatch(CompletableFuture::isDone);

        assertThat(regularCompletableFutures)
            .withFailMessage("Regular entries should be blocking")
            .noneMatch(CompletableFuture::isDone);

        // When
        List<String> results = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            results.add(instance.take());
        }

        // Then
        assertThat(instance.size()).isEqualTo(0);

        // Check entry order : Ensure high-priority entries are retrieved before regular entries
        for (int i = 0; i < 50; i++) {
            assertThat(results.get(i)).startsWith("high-priority-");
        }
        for (int i = 50; i < 100; i++) {
            assertThat(results.get(i)).startsWith("regular-");
        }

        // Check entry order : ensure entry order per writer
        ArrayListValuedHashMap<String, Integer> entriesByWriter = new ArrayListValuedHashMap<>();
        results.forEach(
            entry ->
                entriesByWriter.put(
                    StringUtils.substringBeforeLast(entry, "-"),
                    Integer.parseInt(StringUtils.substringAfterLast(entry, "-"))
                )
        );
        for (String writerId : entriesByWriter.keySet()) {
            assertThat(entriesByWriter.get(writerId)).containsExactlyInAnyOrderElementsOf(
                IntStream.range(0, 10).boxed().collect(Collectors.toList())
            );
        }

        // Cleanup
        executor.shutdown();
        assertThat(executor.awaitTermination(2, TimeUnit.SECONDS)).isTrue();
    }
}
