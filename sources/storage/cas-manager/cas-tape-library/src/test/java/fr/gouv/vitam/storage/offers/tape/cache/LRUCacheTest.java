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

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.alert.AlertService;
import fr.gouv.vitam.common.logging.VitamLogLevel;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.time.LogicalClockRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.google.common.util.concurrent.Uninterruptibles.awaitUninterruptibly;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class LRUCacheTest {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LRUCacheTest.class);

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Rule
    public LogicalClockRule logicalClock = new LogicalClockRule();

    @Mock
    private Consumer<String> evictionListener;

    @Mock
    private AlertService alertService;

    @Mock
    private Executor evictionExecutor;

    private final AtomicBoolean failedExecutor = new AtomicBoolean(false);
    private final AtomicReference<CountDownLatch> beforeExecutionRef = new AtomicReference<>();
    private final AtomicReference<CountDownLatch> afterExecutionRef = new AtomicReference<>();

    @Before
    public void before() {
        doAnswer(args -> {
            CountDownLatch beforeExecution = new CountDownLatch(1);
            CountDownLatch afterExecution = new CountDownLatch(1);

            if (
                !beforeExecutionRef.compareAndSet(null, beforeExecution) ||
                !afterExecutionRef.compareAndSet(null, afterExecution)
            ) {
                failedExecutor.set(true);
                throw new IllegalStateException("Existing CountDownLatch");
            }

            Runnable command = args.getArgument(0);
            new Thread(() -> {
                try {
                    awaitUninterruptibly(beforeExecution);
                    command.run();
                } catch (Exception e) {
                    LOGGER.error("Executor command failed with exception", e);
                    failedExecutor.set(true);
                } finally {
                    afterExecution.countDown();
                }
            }).start();
            return null;
        })
            .when(evictionExecutor)
            .execute(any());
    }

    @After
    public void afterTests() {
        assertThat(failedExecutor.get()).withFailMessage("Executor command failed with exception").isFalse();

        // Ensure no more interactions with mocks
        verifyNoMoreInteractions(evictionListener, alertService, evictionExecutor);
    }

    @Test
    public void testInitialization_givenEmptyInitialEntriesThenOK() {
        // Given
        Stream<LRUCacheEntry<String>> initialEntries = Stream.empty();
        Supplier<LRUCacheEvictionJudge<String>> evictionOracleSupplier = evictAllEntries();

        // When
        LRUCache<String> instance = new LRUCache<>(
            1000,
            900,
            800,
            evictionOracleSupplier,
            evictionListener,
            initialEntries,
            evictionExecutor,
            alertService
        );

        // Then
        verifyNoBackgroundEviction();
        assertThat(instance.getMaxCapacity()).isEqualTo(1000L);
        assertThat(instance.getEvictionCapacity()).isEqualTo(900L);
        assertThat(instance.getSafeCapacity()).isEqualTo(800L);
        assertThat(instance.getCurrentCapacity()).isEqualTo(0L);
    }

    @Test
    public void testInitialization_givenNonEmptyInitialEntriesThenOK() {
        // Given
        Stream<LRUCacheEntry<String>> initialEntries = Stream.of(
            createEntry("key1", 100),
            createEntry("key2", 100),
            createEntry("key3", 100),
            createEntry("key4", 100)
        );
        Supplier<LRUCacheEvictionJudge<String>> evictionOracleSupplier = evictAllEntries();

        // When
        LRUCache<String> instance = new LRUCache<>(
            1000,
            900,
            800,
            evictionOracleSupplier,
            evictionListener,
            initialEntries,
            evictionExecutor,
            alertService
        );

        // Then
        verifyNoBackgroundEviction();
        assertThat(instance.getCurrentCapacity()).isEqualTo(400L);

        assertCacheContainsEntries(instance, "key1", "key2", "key3", "key4");
        assertCacheDoesNotContainReservedEntries(instance, "key1", "key2", "key3", "key4");
    }

    @Test
    public void testInitialization_givenMaxCapacityReachedOnInitializationThenBackgroundEvictionStarted() {
        // Given
        Stream<LRUCacheEntry<String>> initialEntries = Stream.of(
            createEntry("key1", 300),
            createEntry("key2", 300),
            createEntry("key3", 300),
            createEntry("key4", 300)
        );

        Supplier<LRUCacheEvictionJudge<String>> evictionOracleSupplier = evictAllEntries();

        // When
        LRUCache<String> instance = new LRUCache<>(
            1000,
            900,
            800,
            evictionOracleSupplier,
            evictionListener,
            initialEntries,
            evictionExecutor,
            alertService
        );

        // Then
        assertCacheContainsEntries(instance, "key1", "key2", "key3", "key4");
        assertCacheDoesNotContainReservedEntries(instance, "key1", "key2", "key3", "key4");
        assertThat(instance.getCurrentCapacity()).isEqualTo(1200);
        verifyNoMoreInteractions(evictionListener);

        // When background eviction done
        awaitBackgroundEvictionTermination();

        // Then
        assertCacheContainsEntries(instance, "key3", "key4");
        assertCacheDoesNotContainEntries(instance, "key1", "key2");
        assertCacheDoesNotContainReservedEntries(instance, "key1", "key2", "key3", "key4");
        assertThat(instance.getCurrentCapacity()).isEqualTo(600);

        InOrder inOrder = inOrder(evictionListener);
        inOrder.verify(evictionListener).accept("key1");
        inOrder.verify(evictionListener).accept("key2");
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testInitialization_givenDuplicatesInInitialEntriesThenKO() {
        // Given
        Stream<LRUCacheEntry<String>> initialEntries = Stream.of(
            createEntry("key1", 100),
            createEntry("key2", 100),
            createEntry("key1", 100)
        );
        Supplier<LRUCacheEvictionJudge<String>> evictionOracleSupplier = evictAllEntries();

        // When / Then
        assertThatCode(
            () ->
                new LRUCache<>(
                    1000,
                    900,
                    800,
                    evictionOracleSupplier,
                    evictionListener,
                    initialEntries,
                    evictionExecutor,
                    alertService
                )
        ).isInstanceOf(IllegalArgumentException.class);
        verifyNoBackgroundEviction();
    }

    @Test
    public void testReservationCreation_givenEnoughCacheCapacityWhenReservingEntryThenEntryReservedAndNoEviction() {
        // Given
        Stream<LRUCacheEntry<String>> initialEntries = Stream.of(
            createEntry("key1", 100),
            createEntry("key2", 100),
            createEntry("key3", 100)
        );
        Supplier<LRUCacheEvictionJudge<String>> evictionOracleSupplier = evictAllEntries();
        LRUCache<String> instance = new LRUCache<>(
            1000,
            900,
            800,
            evictionOracleSupplier,
            evictionListener,
            initialEntries,
            evictionExecutor,
            alertService
        );

        // When
        instance.reserveEntry(createEntry("key4", 100));

        // Then
        verifyNoBackgroundEviction();
        assertCacheContainsEntries(instance, "key1", "key2", "key3");
        assertCacheDoesNotContainEntries(instance, "key4");
        assertCacheContainsReservedEntries(instance, "key4");
        assertCacheDoesNotContainReservedEntries(instance, "key1", "key2", "key3");
        assertThat(instance.getCurrentCapacity()).isEqualTo(400);
    }

    @Test
    public void testReservationCreation_givenExistingEntryWhenReservingDuplicateEntryThenKO() {
        // Given
        Stream<LRUCacheEntry<String>> initialEntries = Stream.of(
            createEntry("key1", 100),
            createEntry("key2", 100),
            createEntry("key3", 100)
        );
        Supplier<LRUCacheEvictionJudge<String>> evictionOracleSupplier = evictAllEntries();
        LRUCache<String> instance = new LRUCache<>(
            1000,
            900,
            800,
            evictionOracleSupplier,
            evictionListener,
            initialEntries,
            evictionExecutor,
            alertService
        );

        // When / Then
        assertThatThrownBy(() -> instance.reserveEntry(createEntry("key2", 100))).isInstanceOf(
            IllegalArgumentException.class
        );
        verifyNoBackgroundEviction();

        assertCacheContainsEntries(instance, "key1", "key2", "key3");
        assertCacheDoesNotContainReservedEntries(instance, "key1", "key2", "key3");
        assertThat(instance.getCurrentCapacity()).isEqualTo(300);
    }

    @Test
    public void testReservationCreation_givenExistingReservedEntryWhenReservingDuplicateEntryThenKO() {
        // Given
        Stream<LRUCacheEntry<String>> initialEntries = Stream.of(
            createEntry("key1", 100),
            createEntry("key2", 100),
            createEntry("key3", 100)
        );
        Supplier<LRUCacheEvictionJudge<String>> evictionOracleSupplier = evictAllEntries();
        LRUCache<String> instance = new LRUCache<>(
            1000,
            900,
            800,
            evictionOracleSupplier,
            evictionListener,
            initialEntries,
            evictionExecutor,
            alertService
        );
        instance.reserveEntry(createEntry("key4", 100));

        // When / Then
        assertThatThrownBy(() -> instance.reserveEntry(createEntry("key4", 100))).isInstanceOf(
            IllegalArgumentException.class
        );
        verifyNoBackgroundEviction();

        assertCacheContainsEntries(instance, "key1", "key2", "key3");
        assertCacheDoesNotContainEntries(instance, "key4");
        assertCacheContainsReservedEntries(instance, "key4");
        assertCacheDoesNotContainReservedEntries(instance, "key1", "key2", "key3");
        assertThat(instance.getCurrentCapacity()).isEqualTo(400);
    }

    @Test
    public void testReservationCreation_givenEvictionCapacityReachedWhenReservingNewEntryThenOldestEntriesAreEvictedAsynchronously() {
        // Given
        Stream<LRUCacheEntry<String>> initialEntries = Stream.of(
            createEntry("key1", 100_000_000_000_000L),
            createEntry("key2", 200_000_000_000_000L),
            createEntry("key3", 300_000_000_000_000L),
            createEntry("key4", 100_000_000_000_000L)
        );

        Supplier<LRUCacheEvictionJudge<String>> evictionOracleSupplier = evictAllEntries();

        LRUCache<String> instance = new LRUCache<>(
            1000_000_000_000_000L,
            900_000_000_000_000L,
            800_000_000_000_000L,
            evictionOracleSupplier,
            evictionListener,
            initialEntries,
            evictionExecutor,
            alertService
        );

        // When
        instance.reserveEntry(createEntry("key5", 200_000_000_000_000L));
        instance.reserveEntry(createEntry("key6", 50_000_000_000_000L));

        // Then
        assertCacheContainsEntries(instance, "key1", "key2", "key3", "key4");
        assertCacheDoesNotContainEntries(instance, "key5", "key6");
        assertCacheContainsReservedEntries(instance, "key5", "key6");
        assertCacheDoesNotContainReservedEntries(instance, "key1", "key2", "key3", "key4");
        assertThat(instance.getCurrentCapacity()).isEqualTo(950_000_000_000_000L);

        verifyNoMoreInteractions(evictionListener);

        // When : Background eviction finished
        awaitBackgroundEvictionTermination();

        // Then
        assertCacheContainsEntries(instance, "key3", "key4");
        assertCacheDoesNotContainEntries(instance, "key1", "key2", "key5", "key6");
        assertCacheContainsReservedEntries(instance, "key5", "key6");
        assertCacheDoesNotContainReservedEntries(instance, "key1", "key2", "key3", "key4");
        assertThat(instance.getCurrentCapacity()).isEqualTo(650_000_000_000_000L);

        InOrder inOrder = inOrder(evictionListener);
        inOrder.verify(evictionListener).accept("key1");
        inOrder.verify(evictionListener).accept("key2");
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testReservationCreation_givenMaxCapacityReachedWhenReservingNewEntryThenKOAndSecurityAlert() {
        // Given
        Stream<LRUCacheEntry<String>> initialEntries = Stream.of(
            createEntry("key1", 100),
            createEntry("key2", 300),
            createEntry("key3", 300)
        );

        Supplier<LRUCacheEvictionJudge<String>> evictionOracleSupplier = evictAllEntries();

        LRUCache<String> instance = new LRUCache<>(
            1000,
            900,
            800,
            evictionOracleSupplier,
            evictionListener,
            initialEntries,
            evictionExecutor,
            alertService
        );
        instance.reserveEntry(createEntry("key4", 200));

        // When / Then
        assertThatThrownBy(() -> instance.reserveEntry(createEntry("key5", 300))).isInstanceOf(
            IllegalStateException.class
        );

        verify(alertService).createAlert(eq(VitamLogLevel.ERROR), anyString());

        assertCacheContainsEntries(instance, "key1", "key2", "key3");
        assertCacheDoesNotContainEntries(instance, "key4", "key5");
        assertCacheContainsReservedEntries(instance, "key4");
        assertCacheDoesNotContainReservedEntries(instance, "key1", "key2", "key3");
        assertThat(instance.getCurrentCapacity()).isEqualTo(900);
        verifyNoMoreInteractions(evictionListener);

        // When : Background eviction finished
        awaitBackgroundEvictionTermination();

        // Then
        assertCacheContainsEntries(instance, "key3");
        assertCacheDoesNotContainEntries(instance, "key1", "key2", "key4", "key5");
        assertCacheContainsReservedEntries(instance, "key4");
        assertCacheDoesNotContainReservedEntries(instance, "key1", "key2", "key3", "key5");
        assertThat(instance.getCurrentCapacity()).isEqualTo(500);

        InOrder inOrder = inOrder(evictionListener);
        inOrder.verify(evictionListener).accept("key1");
        inOrder.verify(evictionListener).accept("key2");
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testReservationCreation_givenEvictionCapacityReachedWithLockedEntriesWhenReservingNewEntryThenOldestNonLockedEntriesAreEvictedAsynchronously() {
        // Given
        Stream<LRUCacheEntry<String>> initialEntries = Stream.of(
            createEntry("key1", 100),
            createEntry("key2", 200),
            createEntry("key3", 300),
            createEntry("key4", 100)
        );

        Supplier<LRUCacheEvictionJudge<String>> evictionOracleSupplier = evictAllEntriesBut("key2");
        LRUCache<String> instance = new LRUCache<>(
            1000,
            900,
            800,
            evictionOracleSupplier,
            evictionListener,
            initialEntries,
            evictionExecutor,
            alertService
        );

        // When
        instance.reserveEntry(createEntry("key5", 250));

        // Then
        assertCacheContainsEntries(instance, "key1", "key2", "key3", "key4");
        assertCacheDoesNotContainEntries(instance, "key5");
        assertCacheContainsReservedEntries(instance, "key5");
        assertCacheDoesNotContainReservedEntries(instance, "key1", "key2", "key3", "key4");
        assertThat(instance.getCurrentCapacity()).isEqualTo(950);

        verifyNoMoreInteractions(evictionListener);

        // When : Background eviction finished
        awaitBackgroundEvictionTermination();

        // Then
        assertCacheContainsEntries(instance, "key2", "key4");
        assertCacheDoesNotContainEntries(instance, "key1", "key3", "key5");
        assertCacheContainsReservedEntries(instance, "key5");
        assertCacheDoesNotContainReservedEntries(instance, "key1", "key2", "key3", "key4");
        assertThat(instance.getCurrentCapacity()).isEqualTo(550);

        InOrder inOrder = inOrder(evictionListener);
        inOrder.verify(evictionListener).accept("key1");
        inOrder.verify(evictionListener).accept("key3");
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testReservationCreation_givenOldEntriesWithRecentlyUpdatedLastAccessWhenReservingNewEntriesToCacheThenRecentlyUpdatedEntriesAreNotEvicted() {
        // Given
        Stream<LRUCacheEntry<String>> initialEntries = Stream.of(
            createEntry("key1", 50),
            createEntry("key2", 100),
            createEntry("key3", 300),
            createEntry("key4", 200)
        );
        Supplier<LRUCacheEvictionJudge<String>> evictionOracleSupplier = evictAllEntries();
        LRUCache<String> instance = new LRUCache<>(
            1000,
            900,
            800,
            evictionOracleSupplier,
            evictionListener,
            initialEntries,
            evictionExecutor,
            alertService
        );

        // When
        boolean key1Updated = instance.updateEntryAccessTimestamp("key1", getNextInstant());
        boolean key3Updated = instance.updateEntryAccessTimestamp("key3", getNextInstant());
        instance.reserveEntry(createEntry("key5", 250));

        // Then
        awaitBackgroundEvictionTermination();

        assertCacheContainsEntries(instance, "key1", "key3");
        assertCacheDoesNotContainEntries(instance, "key2", "key4", "key5");
        assertCacheContainsReservedEntries(instance, "key5");
        assertCacheDoesNotContainReservedEntries(instance, "key1", "key2", "key3", "key4");
        assertThat(instance.getCurrentCapacity()).isEqualTo(600);

        assertThat(key1Updated).isTrue();
        assertThat(key3Updated).isTrue();
        InOrder inOrder = inOrder(evictionListener);
        inOrder.verify(evictionListener).accept("key2");
        inOrder.verify(evictionListener).accept("key4");
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testReservationCreation_givenAlmostAllEntriesLockedAndEvictionCapacityReachedWhenReservingNewEntryThenAllNonLockedEntriesAreEvictedAndSecurityAlert() {
        // Given
        Stream<LRUCacheEntry<String>> initialEntries = Stream.of(
            createEntry("key1", 100),
            createEntry("key2", 200),
            createEntry("key3", 550)
        );
        Supplier<LRUCacheEvictionJudge<String>> evictionOracleSupplier = evictAllEntriesBut("key2", "key3");
        LRUCache<String> instance = new LRUCache<>(
            1000,
            900,
            800,
            evictionOracleSupplier,
            evictionListener,
            initialEntries,
            evictionExecutor,
            alertService
        );

        // When
        instance.reserveEntry(createEntry("key4", 100));

        // Then
        awaitBackgroundEvictionTermination();

        assertCacheContainsEntries(instance, "key2", "key3");
        assertCacheDoesNotContainEntries(instance, "key1", "key4");
        assertCacheContainsReservedEntries(instance, "key4");
        assertCacheDoesNotContainReservedEntries(instance, "key1", "key2", "key3");
        assertThat(instance.getCurrentCapacity()).isEqualTo(850);

        verify(evictionListener).accept("key1");
        verify(alertService).createAlert(eq(VitamLogLevel.WARN), anyString());
    }

    @Test
    public void testReservationCreation_givenAllOldEntriesLockedWhenReservingNewEntryThenNoEntryEvictedSecurityAlert() {
        // Given
        Stream<LRUCacheEntry<String>> initialEntries = Stream.of(createEntry("key1", 100), createEntry("key2", 750));
        Supplier<LRUCacheEvictionJudge<String>> evictionOracleSupplier = evictAllEntriesBut("key1", "key2");
        LRUCache<String> instance = new LRUCache<>(
            1000,
            900,
            800,
            evictionOracleSupplier,
            evictionListener,
            initialEntries,
            evictionExecutor,
            alertService
        );

        // When
        instance.reserveEntry(createEntry("key3", 100));

        // Then
        awaitBackgroundEvictionTermination();

        assertCacheContainsEntries(instance, "key1", "key2");
        assertCacheDoesNotContainEntries(instance, "key3");
        assertCacheContainsReservedEntries(instance, "key3");
        assertCacheDoesNotContainReservedEntries(instance, "key1", "key2");
        assertThat(instance.getCurrentCapacity()).isEqualTo(950);
        verify(alertService).createAlert(eq(VitamLogLevel.WARN), anyString());
    }

    @Test
    public void testReservationCreation_givenEvictedEntryWhenReservingAgainSameEntryThenOK() {
        // Given
        Stream<LRUCacheEntry<String>> initialEntries = Stream.of(createEntry("key1", 500));

        Supplier<LRUCacheEvictionJudge<String>> evictionOracleSupplier = evictAllEntries();
        LRUCache<String> instance = new LRUCache<>(
            1000,
            900,
            800,
            evictionOracleSupplier,
            evictionListener,
            initialEntries,
            evictionExecutor,
            alertService
        );

        // When
        instance.reserveEntry(createEntry("key2", 400));
        instance.confirmReservation("key2");

        // Then
        awaitBackgroundEvictionTermination();
        assertCacheContainsEntries(instance, "key2");
        assertCacheDoesNotContainEntries(instance, "key1");
        assertCacheDoesNotContainReservedEntries(instance, "key1", "key2");
        assertThat(instance.getCurrentCapacity()).isEqualTo(400);

        verify(evictionListener).accept("key1");
        verifyNoMoreInteractions(evictionListener);

        // When : Entry key1 reserved again
        instance.reserveEntry(createEntry("key1", 500));

        // Then
        awaitBackgroundEvictionTermination(2);
        assertCacheDoesNotContainEntries(instance, "key1", "key2");
        assertCacheContainsReservedEntries(instance, "key1");
        assertCacheDoesNotContainReservedEntries(instance, "key2");
        assertThat(instance.getCurrentCapacity()).isEqualTo(500);

        verify(evictionListener).accept("key2");
        verifyNoMoreInteractions(evictionListener);
    }

    @Test
    public void testReservationCreation_givenBackgroundEvictionProcessRunningWhenReservingNewEntryThenOK() {
        // Given
        Stream<LRUCacheEntry<String>> initialEntries = Stream.of(
            createEntry("key1", 300),
            createEntry("key2", 300),
            createEntry("key3", 200)
        );

        Supplier<LRUCacheEvictionJudge<String>> evictionOracleSupplier = evictAllEntries();

        LRUCache<String> instance = new LRUCache<>(
            1000,
            900,
            800,
            evictionOracleSupplier,
            evictionListener,
            initialEntries,
            evictionExecutor,
            alertService
        );

        // When
        instance.reserveEntry(createEntry("key4", 100));

        // Then : Background process started
        verify(evictionExecutor).execute(any());

        assertCacheContainsEntries(instance, "key1", "key2", "key3");
        assertCacheDoesNotContainEntries(instance, "key4");
        assertCacheContainsReservedEntries(instance, "key4");
        assertCacheDoesNotContainReservedEntries(instance, "key1", "key2", "key3");
        assertThat(instance.getCurrentCapacity()).isEqualTo(900);

        // When : Another entry reserved
        instance.reserveEntry(createEntry("key5", 50));

        // Then
        assertCacheContainsEntries(instance, "key1", "key2", "key3");
        assertCacheDoesNotContainEntries(instance, "key4", "key5");
        assertCacheContainsReservedEntries(instance, "key4", "key5");
        assertCacheDoesNotContainReservedEntries(instance, "key1", "key2", "key3");
        assertThat(instance.getCurrentCapacity()).isEqualTo(950);

        verifyNoMoreInteractions(evictionListener);

        // When : Background eviction finished
        awaitBackgroundEvictionTermination();

        // Then
        assertCacheContainsEntries(instance, "key2", "key3");
        assertCacheDoesNotContainEntries(instance, "key1", "key4", "key5");
        assertCacheContainsReservedEntries(instance, "key4", "key5");
        assertCacheDoesNotContainReservedEntries(instance, "key1", "key2", "key3");
        assertThat(instance.getCurrentCapacity()).isEqualTo(650);

        verify(evictionListener).accept("key1");
        verifyNoMoreInteractions(evictionListener);
    }

    @Test
    public void testReservationCreation_givenCacheEvictionJudgeCreationExceptionDuringBackgroundThenSecurityAlert() {
        // Given
        Stream<LRUCacheEntry<String>> initialEntries = Stream.of(
            createEntry("key1", 200),
            createEntry("key2", 300),
            createEntry("key3", 300)
        );

        RuntimeException runtimeException = new RuntimeException();
        Supplier<LRUCacheEvictionJudge<String>> evictionOracleSupplier = () -> {
            throw runtimeException;
        };
        LRUCache<String> instance = new LRUCache<>(
            1000,
            900,
            800,
            evictionOracleSupplier,
            evictionListener,
            initialEntries,
            evictionExecutor,
            alertService
        );

        // When
        instance.reserveEntry(createEntry("key4", 100));

        // Then
        awaitBackgroundEvictionTermination();
        verify(alertService).createAlert(eq(VitamLogLevel.ERROR), anyString(), eq(runtimeException));

        assertCacheContainsEntries(instance, "key1", "key2", "key3");
        assertCacheDoesNotContainEntries(instance, "key4");
        assertCacheContainsReservedEntries(instance, "key4");
        assertCacheDoesNotContainReservedEntries(instance, "key1", "key2", "key3");
        assertThat(instance.getCurrentCapacity()).isEqualTo(900);
    }

    @Test
    public void testReservationCreation_givenNullCacheEvictionJudgeCreatedDuringBackgroundEvictionThenSecurityAlert() {
        // Given
        Stream<LRUCacheEntry<String>> initialEntries = Stream.of(
            createEntry("key1", 200),
            createEntry("key2", 300),
            createEntry("key3", 300)
        );

        Supplier<LRUCacheEvictionJudge<String>> evictionOracleSupplier = () -> null;
        LRUCache<String> instance = new LRUCache<>(
            1000,
            900,
            800,
            evictionOracleSupplier,
            evictionListener,
            initialEntries,
            evictionExecutor,
            alertService
        );

        // When
        instance.reserveEntry(createEntry("key4", 100));

        // Then
        awaitBackgroundEvictionTermination();
        verify(alertService).createAlert(eq(VitamLogLevel.ERROR), anyString(), any());

        assertCacheContainsEntries(instance, "key1", "key2", "key3");
        assertCacheDoesNotContainEntries(instance, "key4");
        assertCacheContainsReservedEntries(instance, "key4");
        assertCacheDoesNotContainReservedEntries(instance, "key1", "key2", "key3");
        assertThat(instance.getCurrentCapacity()).isEqualTo(900);
    }

    @Test
    public void testReservationCreation_givenCacheEvictionJudgeExceptionDuringBackgroundThenSecurityAlert() {
        // Given
        Stream<LRUCacheEntry<String>> initialEntries = Stream.of(
            createEntry("key1", 50),
            createEntry("key2", 250),
            createEntry("key3", 400)
        );

        RuntimeException runtimeException = new RuntimeException();
        Supplier<LRUCacheEvictionJudge<String>> evictionOracleSupplier = () ->
            entryKey -> {
                if (entryKey.equals("key2")) {
                    throw runtimeException;
                }
                return true;
            };
        LRUCache<String> instance = new LRUCache<>(
            1000,
            900,
            800,
            evictionOracleSupplier,
            evictionListener,
            initialEntries,
            evictionExecutor,
            alertService
        );

        // When
        instance.reserveEntry(createEntry("key4", 200));

        // Then
        awaitBackgroundEvictionTermination();
        verify(alertService).createAlert(eq(VitamLogLevel.ERROR), anyString(), eq(runtimeException));

        assertCacheContainsEntries(instance, "key2", "key3");
        assertCacheDoesNotContainEntries(instance, "key1", "key4");
        assertCacheContainsReservedEntries(instance, "key4");
        assertCacheDoesNotContainReservedEntries(instance, "key1", "key2", "key3");
        assertThat(instance.getCurrentCapacity()).isEqualTo(850);

        verify(evictionListener).accept("key1");
        verifyNoMoreInteractions(evictionListener);
    }

    @Test
    public void testReservationCreation_givenEvictionListenerExceptionDuringBackgroundEvictionThenSecurityAlert() {
        // Given
        Stream<LRUCacheEntry<String>> initialEntries = Stream.of(
            createEntry("key1", 50),
            createEntry("key2", 250),
            createEntry("key3", 400)
        );

        RuntimeException runtimeException = new RuntimeException();
        doThrow(runtimeException).when(evictionListener).accept("key2");

        Supplier<LRUCacheEvictionJudge<String>> evictionOracleSupplier = evictAllEntries();
        LRUCache<String> instance = new LRUCache<>(
            1000,
            900,
            800,
            evictionOracleSupplier,
            evictionListener,
            initialEntries,
            evictionExecutor,
            alertService
        );

        // When
        instance.reserveEntry(createEntry("key4", 200));

        // Then
        awaitBackgroundEvictionTermination();
        verify(alertService).createAlert(eq(VitamLogLevel.ERROR), anyString(), eq(runtimeException));

        assertCacheContainsEntries(instance, "key3");
        assertCacheDoesNotContainEntries(instance, "key1", "key2", "key4");
        assertCacheContainsReservedEntries(instance, "key4");
        assertCacheDoesNotContainReservedEntries(instance, "key1", "key2", "key3");
        assertThat(instance.getCurrentCapacity()).isEqualTo(600);

        InOrder inOrder = inOrder(evictionListener);
        inOrder.verify(evictionListener).accept("key1");
        inOrder.verify(evictionListener).accept("key2");
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testUpdateAccessTimestamp_givenNonExistingEntryWhenUpdatingItsAccessTimestampThenIgnored() {
        // Given
        Stream<LRUCacheEntry<String>> initialEntries = Stream.of(createEntry("key1", 100), createEntry("key2", 100));
        Supplier<LRUCacheEvictionJudge<String>> evictionOracleSupplier = evictAllEntries();
        LRUCache<String> instance = new LRUCache<>(
            1000,
            900,
            800,
            evictionOracleSupplier,
            evictionListener,
            initialEntries,
            evictionExecutor,
            alertService
        );

        // When
        boolean key3Updated = instance.updateEntryAccessTimestamp("key3", getNextInstant());

        // Then
        assertThat(key3Updated).isFalse();
        assertCacheContainsEntries(instance, "key1", "key2");
        assertCacheDoesNotContainEntries(instance, "key3");
        assertCacheDoesNotContainReservedEntries(instance, "key1", "key2", "key3");
        assertThat(instance.getCurrentCapacity()).isEqualTo(200);
    }

    @Test
    public void testUpdateAccessTimestamp_givenReservedEntryWhenUpdatingItsAccessTimestampThenEntryIsEvictedLast() {
        // Given
        Stream<LRUCacheEntry<String>> initialEntries = Stream.of(createEntry("key1", 50), createEntry("key2", 50));
        Supplier<LRUCacheEvictionJudge<String>> evictionOracleSupplier = evictAllEntries();
        LRUCache<String> instance = new LRUCache<>(
            1000,
            900,
            800,
            evictionOracleSupplier,
            evictionListener,
            initialEntries,
            evictionExecutor,
            alertService
        );

        // When
        instance.reserveEntry(createEntry("key3", 50));
        instance.reserveEntry(createEntry("key4", 50));
        boolean key3Updated = instance.updateEntryAccessTimestamp("key3", getNextInstant());
        instance.confirmReservation("key3");
        instance.confirmReservation("key4");

        instance.reserveEntry(createEntry("key5", 799));

        // Then
        awaitBackgroundEvictionTermination();

        assertThat(key3Updated).isTrue();
        assertCacheDoesNotContainEntries(instance, "key1", "key2", "key3", "key4", "key5");
        assertCacheContainsReservedEntries(instance, "key5");
        assertCacheDoesNotContainReservedEntries(instance, "key1", "key2", "key3", "key4");
        assertThat(instance.getCurrentCapacity()).isEqualTo(799);

        InOrder inOrder = inOrder(evictionListener);
        inOrder.verify(evictionListener).accept("key1");
        inOrder.verify(evictionListener).accept("key2");
        inOrder.verify(evictionListener).accept("key4");
        inOrder.verify(evictionListener).accept("key3");
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testUpdateAccessTimestamp_givenExistingEntryAddedDuringEvictionProcessWhenUpdatingItsAccessTimestampThenEntryIsEvictedLast() {
        // Given
        Stream<LRUCacheEntry<String>> initialEntries = Stream.of(createEntry("key1", 850));
        Supplier<LRUCacheEvictionJudge<String>> evictionOracleSupplier = evictAllEntries();
        LRUCache<String> instance = new LRUCache<>(
            1000,
            900,
            800,
            evictionOracleSupplier,
            evictionListener,
            initialEntries,
            evictionExecutor,
            alertService
        );

        // When : Update key2 access time during background process
        instance.reserveEntry(createEntry("key2", 70));
        instance.confirmReservation("key2");

        instance.reserveEntry(createEntry("key3", 70));
        instance.confirmReservation("key3");

        boolean key3Updated = instance.updateEntryAccessTimestamp("key2", getNextInstant());

        // Then
        awaitBackgroundEvictionTermination();

        assertThat(key3Updated).isTrue();
        assertCacheContainsEntries(instance, "key2", "key3");
        assertCacheDoesNotContainEntries(instance, "key1");
        assertCacheDoesNotContainReservedEntries(instance, "key1", "key2", "key3");
        assertThat(instance.getCurrentCapacity()).isEqualTo(140);

        verify(evictionListener).accept("key1");
        verifyNoMoreInteractions(evictionListener);

        // When
        instance.reserveEntry(createEntry("key4", 760));

        // Then
        awaitBackgroundEvictionTermination(2);

        assertCacheDoesNotContainEntries(instance, "key1", "key2", "key3", "key4");
        assertCacheContainsReservedEntries(instance, "key4");
        assertCacheDoesNotContainReservedEntries(instance, "key1", "key2", "key3");
        assertThat(instance.getCurrentCapacity()).isEqualTo(760);

        InOrder inOrder = inOrder(evictionListener);
        inOrder.verify(evictionListener).accept("key3");
        inOrder.verify(evictionListener).accept("key2");
        inOrder.verifyNoMoreInteractions();

        verifyNoMoreInteractions(evictionListener);
    }

    @Test
    public void testUpdateAccessTimestamp_givenExistingEntryWhenUpdatingItsAccessTimestampThenEntryIsEvictedLast() {
        // Given
        Stream<LRUCacheEntry<String>> initialEntries = Stream.of(
            createEntry("key1", 50),
            createEntry("key2", 50),
            createEntry("key3", 50),
            createEntry("key4", 50)
        );
        Supplier<LRUCacheEvictionJudge<String>> evictionOracleSupplier = evictAllEntries();
        LRUCache<String> instance = new LRUCache<>(
            1000,
            900,
            800,
            evictionOracleSupplier,
            evictionListener,
            initialEntries,
            evictionExecutor,
            alertService
        );

        // When
        boolean key3Updated = instance.updateEntryAccessTimestamp("key3", getNextInstant());
        instance.reserveEntry(createEntry("key5", 799));

        // Then
        awaitBackgroundEvictionTermination();

        assertThat(key3Updated).isTrue();
        assertCacheDoesNotContainEntries(instance, "key1", "key2", "key3", "key4", "key5");
        assertCacheContainsReservedEntries(instance, "key5");
        assertCacheDoesNotContainReservedEntries(instance, "key1", "key2", "key3", "key4");
        assertThat(instance.getCurrentCapacity()).isEqualTo(799);

        InOrder inOrder = inOrder(evictionListener);
        inOrder.verify(evictionListener).accept("key1");
        inOrder.verify(evictionListener).accept("key2");
        inOrder.verify(evictionListener).accept("key4");
        inOrder.verify(evictionListener).accept("key3");
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testReservationConfirmation_givenExistingEntryReservationWhenConfirmingReservationThenEntryIsAdded() {
        // Given
        Stream<LRUCacheEntry<String>> initialEntries = Stream.of(createEntry("key1", 100), createEntry("key2", 100));
        Supplier<LRUCacheEvictionJudge<String>> evictionOracleSupplier = evictAllEntries();
        LRUCache<String> instance = new LRUCache<>(
            1000,
            900,
            800,
            evictionOracleSupplier,
            evictionListener,
            initialEntries,
            evictionExecutor,
            alertService
        );
        instance.reserveEntry(createEntry("key3", 100));

        // When
        instance.confirmReservation("key3");

        // Then
        verifyNoBackgroundEviction();

        assertCacheContainsEntries(instance, "key1", "key2", "key3");
        assertCacheDoesNotContainReservedEntries(instance, "key1", "key2", "key3");
        assertThat(instance.getCurrentCapacity()).isEqualTo(300);
    }

    @Test
    public void testReservationConfirmation_givenUnknownEntryReservationWhenConfirmingReservationThenKO() {
        // Given
        Stream<LRUCacheEntry<String>> initialEntries = Stream.of(createEntry("key1", 100), createEntry("key2", 100));
        Supplier<LRUCacheEvictionJudge<String>> evictionOracleSupplier = evictAllEntries();
        LRUCache<String> instance = new LRUCache<>(
            1000,
            900,
            800,
            evictionOracleSupplier,
            evictionListener,
            initialEntries,
            evictionExecutor,
            alertService
        );

        // When / Then
        assertThatThrownBy(() -> instance.confirmReservation("key3")).isInstanceOf(IllegalArgumentException.class);

        assertCacheContainsEntries(instance, "key1", "key2");
        assertCacheDoesNotContainEntries(instance, "key3");
        assertCacheDoesNotContainReservedEntries(instance, "key1", "key2", "key3");
        assertThat(instance.getCurrentCapacity()).isEqualTo(200);

        verifyNoBackgroundEviction();
    }

    @Test
    public void testReservationConfirmation_givenExistingEntryReservationWhenDoubleConfirmingReservationThenKO() {
        // Given
        Stream<LRUCacheEntry<String>> initialEntries = Stream.of(createEntry("key1", 100), createEntry("key2", 100));
        Supplier<LRUCacheEvictionJudge<String>> evictionOracleSupplier = evictAllEntries();
        LRUCache<String> instance = new LRUCache<>(
            1000,
            900,
            800,
            evictionOracleSupplier,
            evictionListener,
            initialEntries,
            evictionExecutor,
            alertService
        );
        instance.reserveEntry(createEntry("key3", 100));
        instance.confirmReservation("key3");

        // When / Then
        assertThatThrownBy(() -> instance.confirmReservation("key3")).isInstanceOf(IllegalArgumentException.class);

        // Then
        assertCacheContainsEntries(instance, "key1", "key2", "key3");
        assertCacheDoesNotContainReservedEntries(instance, "key1", "key2", "key3");
        assertThat(instance.getCurrentCapacity()).isEqualTo(300);

        verifyNoBackgroundEviction();
    }

    @Test
    public void testReservationConfirmation_givenEvictedAndReservedAgainEntryWhenConfirmingReservationThenOK() {
        // Given
        Stream<LRUCacheEntry<String>> initialEntries = Stream.of(createEntry("key1", 500));

        Supplier<LRUCacheEvictionJudge<String>> evictionOracleSupplier = evictAllEntries();
        LRUCache<String> instance = new LRUCache<>(
            1000,
            900,
            800,
            evictionOracleSupplier,
            evictionListener,
            initialEntries,
            evictionExecutor,
            alertService
        );

        // When
        instance.reserveEntry(createEntry("key2", 400));
        instance.confirmReservation("key2");

        // Then
        awaitBackgroundEvictionTermination();
        assertCacheContainsEntries(instance, "key2");
        assertCacheDoesNotContainEntries(instance, "key1");
        assertCacheDoesNotContainReservedEntries(instance, "key1", "key2");
        assertThat(instance.getCurrentCapacity()).isEqualTo(400);

        verify(evictionListener).accept("key1");
        verifyNoMoreInteractions(evictionListener);

        // When : Entry added again
        instance.reserveEntry(createEntry("key1", 500));
        instance.confirmReservation("key1");

        // Then
        awaitBackgroundEvictionTermination(2);
        assertCacheContainsEntries(instance, "key1");
        assertCacheDoesNotContainEntries(instance, "key2");
        assertCacheDoesNotContainReservedEntries(instance, "key1", "key2");
        assertThat(instance.getCurrentCapacity()).isEqualTo(500);

        verify(evictionListener).accept("key2");
        verifyNoMoreInteractions(evictionListener);
    }

    @Test
    public void testReservationConfirmation_givenBackgroundEvictionProcessRunningWhenConfirmingReservingOfNewEntryThenOK() {
        // Given
        Stream<LRUCacheEntry<String>> initialEntries = Stream.of(
            createEntry("key1", 300),
            createEntry("key2", 300),
            createEntry("key3", 200)
        );

        Supplier<LRUCacheEvictionJudge<String>> evictionOracleSupplier = evictAllEntries();

        LRUCache<String> instance = new LRUCache<>(
            1000,
            900,
            800,
            evictionOracleSupplier,
            evictionListener,
            initialEntries,
            evictionExecutor,
            alertService
        );

        // When
        instance.reserveEntry(createEntry("key4", 100));

        // Then : Background process started
        verify(evictionExecutor).execute(any());

        assertCacheContainsEntries(instance, "key1", "key2", "key3");
        assertCacheDoesNotContainEntries(instance, "key4");
        assertCacheContainsReservedEntries(instance, "key4");
        assertCacheDoesNotContainReservedEntries(instance, "key1", "key2", "key3");
        assertThat(instance.getCurrentCapacity()).isEqualTo(900);

        // When : Another entry reserved
        instance.reserveEntry(createEntry("key5", 50));
        instance.confirmReservation("key5");

        // Then
        assertCacheContainsEntries(instance, "key1", "key2", "key3", "key5");
        assertCacheDoesNotContainEntries(instance, "key4");
        assertCacheContainsReservedEntries(instance, "key4");
        assertCacheDoesNotContainReservedEntries(instance, "key1", "key2", "key3", "key5");
        assertThat(instance.getCurrentCapacity()).isEqualTo(950);

        verifyNoMoreInteractions(evictionListener);

        // When : Background eviction finished
        awaitBackgroundEvictionTermination();

        // Then
        assertCacheContainsEntries(instance, "key2", "key3", "key5");
        assertCacheDoesNotContainEntries(instance, "key1", "key4");
        assertCacheContainsReservedEntries(instance, "key4");
        assertCacheDoesNotContainReservedEntries(instance, "key1", "key2", "key3", "key5");
        assertThat(instance.getCurrentCapacity()).isEqualTo(650);

        verify(evictionListener).accept("key1");
        verifyNoMoreInteractions(evictionListener);
    }

    @Test
    public void testReservationCancellation_givenExistingEntryReservationWhenCancelingReservationThenEntryIsNoMoreReservedAndCacheSpaceFreed() {
        // Given
        Stream<LRUCacheEntry<String>> initialEntries = Stream.of(createEntry("key1", 100), createEntry("key2", 100));
        Supplier<LRUCacheEvictionJudge<String>> evictionOracleSupplier = evictAllEntries();
        LRUCache<String> instance = new LRUCache<>(
            1000,
            900,
            800,
            evictionOracleSupplier,
            evictionListener,
            initialEntries,
            evictionExecutor,
            alertService
        );
        instance.reserveEntry(createEntry("key3", 100));

        // When
        instance.cancelReservation("key3");

        // Then
        verifyNoBackgroundEviction();
        assertCacheContainsEntries(instance, "key1", "key2");
        assertCacheDoesNotContainEntries(instance, "key3");
        assertCacheDoesNotContainReservedEntries(instance, "key1", "key2", "key3");
        assertThat(instance.getCurrentCapacity()).isEqualTo(200);
    }

    @Test
    public void testReservationCancellation_givenUnknownEntryReservationWhenCancelingReservationThenKO() {
        // Given
        Stream<LRUCacheEntry<String>> initialEntries = Stream.of(createEntry("key1", 100), createEntry("key2", 100));
        Supplier<LRUCacheEvictionJudge<String>> evictionOracleSupplier = evictAllEntries();
        LRUCache<String> instance = new LRUCache<>(
            1000,
            900,
            800,
            evictionOracleSupplier,
            evictionListener,
            initialEntries,
            evictionExecutor,
            alertService
        );

        // When / Then
        assertThatThrownBy(() -> instance.cancelReservation("key3")).isInstanceOf(IllegalArgumentException.class);

        assertCacheContainsEntries(instance, "key1", "key2");
        assertCacheDoesNotContainEntries(instance, "key3");
        assertCacheDoesNotContainReservedEntries(instance, "key1", "key2", "key3");
        assertThat(instance.getCurrentCapacity()).isEqualTo(200);

        verifyNoBackgroundEviction();
    }

    @Test
    public void testReservationCancellation_givenExistingEntryReservationWhenDoubleCancelingReservationThenKO() {
        // Given
        Stream<LRUCacheEntry<String>> initialEntries = Stream.of(createEntry("key1", 100), createEntry("key2", 100));
        Supplier<LRUCacheEvictionJudge<String>> evictionOracleSupplier = evictAllEntries();
        LRUCache<String> instance = new LRUCache<>(
            1000,
            900,
            800,
            evictionOracleSupplier,
            evictionListener,
            initialEntries,
            evictionExecutor,
            alertService
        );
        instance.reserveEntry(createEntry("key3", 100));
        instance.cancelReservation("key3");

        // When / Then
        assertThatThrownBy(() -> instance.cancelReservation("key3")).isInstanceOf(IllegalArgumentException.class);

        // Then
        assertCacheContainsEntries(instance, "key1", "key2");
        assertCacheDoesNotContainEntries(instance, "key3");
        assertCacheDoesNotContainReservedEntries(instance, "key1", "key2", "key3");
        assertThat(instance.getCurrentCapacity()).isEqualTo(200);

        verifyNoBackgroundEviction();
    }

    @Test
    public void testReservationCancellation_givenEvictedAndReservedAgainEntryWhenCancelingReservationThenOK() {
        // Given
        Stream<LRUCacheEntry<String>> initialEntries = Stream.of(createEntry("key1", 500));

        Supplier<LRUCacheEvictionJudge<String>> evictionOracleSupplier = evictAllEntries();
        LRUCache<String> instance = new LRUCache<>(
            1000,
            900,
            800,
            evictionOracleSupplier,
            evictionListener,
            initialEntries,
            evictionExecutor,
            alertService
        );

        // When
        instance.reserveEntry(createEntry("key2", 400));
        instance.confirmReservation("key2");

        // Then
        awaitBackgroundEvictionTermination();
        assertCacheContainsEntries(instance, "key2");
        assertCacheDoesNotContainEntries(instance, "key1");
        assertCacheDoesNotContainReservedEntries(instance, "key1", "key2");
        assertThat(instance.getCurrentCapacity()).isEqualTo(400);

        verify(evictionListener).accept("key1");
        verifyNoMoreInteractions(evictionListener);

        // When : Entry reserved again, and canceled
        instance.reserveEntry(createEntry("key1", 500));
        instance.cancelReservation("key1");

        // Then (Eviction process did nothing since no more memory capacity exceeded)
        awaitBackgroundEvictionTermination(2);
        assertCacheContainsEntries(instance, "key2");
        assertCacheDoesNotContainEntries(instance, "key1");
        assertCacheDoesNotContainReservedEntries(instance, "key1", "key2");
        assertThat(instance.getCurrentCapacity()).isEqualTo(400);
        verifyNoMoreInteractions(evictionListener);
    }

    private LRUCacheEntry<String> createEntry(String key, long weight) {
        Instant nextInstant = getNextInstant();
        return new LRUCacheEntry<>(key, weight, nextInstant);
    }

    private Instant getNextInstant() {
        logicalClock.logicalSleep(1, ChronoUnit.MINUTES);
        return LocalDateUtil.now().toInstant(ZoneOffset.UTC);
    }

    private void assertCacheDoesNotContainEntries(LRUCache<String> instance, String... keys) {
        for (String key : keys) {
            assertThat(instance.containsEntry(key)).withFailMessage("Expecting " + key + " to not exist").isFalse();
        }
    }

    private void assertCacheContainsEntries(LRUCache<String> instance, String... keys) {
        for (String key : keys) {
            assertThat(instance.containsEntry(key)).withFailMessage("Expecting " + key + " to exist").isTrue();
        }
    }

    private void assertCacheDoesNotContainReservedEntries(LRUCache<String> instance, String... keys) {
        for (String key : keys) {
            assertThat(instance.isReservedEntry(key))
                .withFailMessage("Expecting " + key + " to be not reserved")
                .isFalse();
        }
    }

    private void assertCacheContainsReservedEntries(LRUCache<String> instance, String... keys) {
        for (String key : keys) {
            assertThat(instance.isReservedEntry(key)).withFailMessage("Expecting " + key + " to be reserved").isTrue();
        }
    }

    private Supplier<LRUCacheEvictionJudge<String>> evictAllEntries() {
        return () -> entryKey -> true;
    }

    private Supplier<LRUCacheEvictionJudge<String>> evictAllEntriesBut(String... entries) {
        return () -> entryKey -> Arrays.stream(entries).noneMatch(entryKey::equals);
    }

    public void awaitBackgroundEvictionTermination() {
        awaitBackgroundEvictionTermination(1);
    }

    public void awaitBackgroundEvictionTermination(int times) {
        verify(evictionExecutor, times(times)).execute(any());
        beforeExecutionRef.get().countDown();
        awaitUninterruptibly(afterExecutionRef.get());
        beforeExecutionRef.set(null);
        afterExecutionRef.set(null);
    }

    private void verifyNoBackgroundEviction() {
        verify(evictionExecutor, never()).execute(any());
    }
}
