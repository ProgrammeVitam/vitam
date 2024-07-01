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

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.alert.AlertService;
import fr.gouv.vitam.common.logging.VitamLogLevel;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

import javax.annotation.concurrent.ThreadSafe;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Cache implementation with Least Recently Used (LRU) eviction policy, weighed entry capacity management, and eviction filtering for in-use/non-deletable entries.
 * Before adding an entry, a reservation is done to ensure enough capacity is available. Once reservation done, entry can be added (by confirming reservation) ou canceled (by canceling reservation).
 * Eviction handling is done asynchronously when reserving new entries in the cache reaches eviction capacity threshold. Reserved entries are not evicted since they are not yet added.
 * If cache capacity exceeds its max capacity threshold, the entry reservation will fail with an exception.
 * The eviction process ends when target eviction capacity threshold is reached, or no more entries to evict.
 * During eviction process, entry eviction can be controlled using a {@link LRUCacheEvictionJudge <T>}, which allows filtering of entries that cannot be evicted (currently locked).
 * After an entry is evicted, a listener is invoked for eviction notification.
 * When created, a cache must be initialized with a list of initial list of entries. Background eviction process may be triggered at the end of initialization is required.
 * This class is Thread-Safe (all public methods are synchronized).
 *
 * @param <T> the type of entry keys maintained by this cache.
 */
@ThreadSafe
public class LRUCache<T> {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LRUCache.class);

    private final long maxCapacity;
    private final long evictionCapacity;
    private final long safeCapacity;

    private final Supplier<LRUCacheEvictionJudge<T>> lruCacheEvictionJudgeFactory;
    private final Consumer<T> evictionListener;
    private final Executor evictionExecutor;
    private final AlertService alertService;

    private final LRUQueue<T> lruQueue;
    private final Map<T, LRUCacheEntry<T>> pendingEntryQueue;
    private final Map<T, LRUCacheEntry<T>> reservedEntryMap;
    private final Map<T, Long> cacheEntryWeightMap;
    private final AtomicBoolean isAsyncEvictionRunning = new AtomicBoolean();

    private long currentCapacity = 0L;

    /**
     * @param maxCapacity Max cache storage capacity. Once reached, adding new entries fails.
     * @param evictionCapacity Once reached, background eviction process is started to purge cache of old unused entries.
     * @param safeCapacity Safe capacity level. Cache eviction process stops when cache capacity is bellow safe cache threshold.
     * @param evictionJudgeFactory factory that instantiates an eviction judge ({@link LRUCacheEvictionJudge}) used by background eviction process for entry eviction filtering. A new eviction judge is created before each eviction.
     * @param evictionListener listener invoked (synchronously) when an expired entry as been evicted from cache.
     * @param initialEntries stream of cache entries to add to cache. Entries mush have distinct keys.
     * @param evictionExecutor Executor for running eviction process asynchronously.
     * @param alertService alert service that is used for reporting cache capacity alerts.
     * @throws IllegalArgumentException when provided parameters have illegal values.
     */
    public LRUCache(
        long maxCapacity,
        long evictionCapacity,
        long safeCapacity,
        Supplier<LRUCacheEvictionJudge<T>> evictionJudgeFactory,
        Consumer<T> evictionListener,
        Stream<LRUCacheEntry<T>> initialEntries,
        Executor evictionExecutor,
        AlertService alertService
    ) throws IllegalArgumentException {
        ParametersChecker.checkValue(
            "maxCapacity mush be greater than evictionCapacity",
            maxCapacity,
            evictionCapacity + 1
        );
        ParametersChecker.checkValue(
            "evictionCapacity must be greater than safeCapacity",
            evictionCapacity,
            safeCapacity + 1
        );
        ParametersChecker.checkValue("safeCapacity must be positive", safeCapacity, 1);
        ParametersChecker.checkParameter("Missing evictionJudgeFactory", evictionJudgeFactory);
        ParametersChecker.checkParameter("Missing evictionListener", evictionListener);
        ParametersChecker.checkParameter("Missing initial entries", initialEntries);
        ParametersChecker.checkParameter("Missing evictionExecutor", evictionExecutor);
        ParametersChecker.checkParameter("Missing alert service", alertService);

        this.maxCapacity = maxCapacity;
        this.evictionCapacity = evictionCapacity;
        this.safeCapacity = safeCapacity;
        this.lruCacheEvictionJudgeFactory = evictionJudgeFactory;
        this.evictionListener = evictionListener;
        this.evictionExecutor = evictionExecutor;
        this.alertService = alertService;

        this.lruQueue = new LRUQueue<>();
        this.pendingEntryQueue = new LinkedHashMap<>();
        this.reservedEntryMap = new HashMap<>();
        this.cacheEntryWeightMap = new HashMap<>();

        initializeCache(initialEntries);
    }

    private void initializeCache(Stream<LRUCacheEntry<T>> initialEntries) throws IllegalArgumentException {
        initialEntries.forEach(entry -> {
            // Check entry
            ParametersChecker.checkParameter("Null entry", entry);
            ParametersChecker.checkParameter("Null entry key", entry.getKey());
            ParametersChecker.checkParameter("Null last access instant", entry.getLastAccessInstant());
            ParametersChecker.checkParameter("Entry weight must be positive", entry.getWeight());

            if (this.lruQueue.contains(entry.getKey())) {
                throw new IllegalArgumentException("Duplicate key " + entry.getKey());
            }

            this.lruQueue.add(entry.getKey(), entry.getLastAccessInstant().toEpochMilli());
            this.cacheEntryWeightMap.put(entry.getKey(), entry.getWeight());
            this.currentCapacity += entry.getWeight();

            LOGGER.debug(
                "Added an entry '{}' with weight {} and last access time: {} during initialization",
                entry.getKey(),
                entry.getWeight(),
                entry.getLastAccessInstant()
            );
        });

        startAsyncEvictionProcessIfNeeded();
    }

    /**
     * Reserve cache capacity for a new entry to add the cache.
     *
     * An entry reservation can be confirmed using the {@code confirmReservation()} method, or canceled using the {@code cancelReservation()} method.
     *
     * If entry already exists in the cache, an {@link IllegalArgumentException} is thrown.
     *
     * @param entry the entry to reserve in cache
     * @throws IllegalArgumentException when provided parameters have illegal values.
     * @throws IllegalStateException when cache max capacity is reached.
     */
    public synchronized void reserveEntry(LRUCacheEntry<T> entry)
        throws IllegalArgumentException, IllegalStateException {
        // Check entry
        ParametersChecker.checkParameter("Null entry", entry);
        ParametersChecker.checkParameter("Null entry key", entry.getKey());
        ParametersChecker.checkParameter("Null last access instant", entry.getLastAccessInstant());
        ParametersChecker.checkParameter("Entry weight must be positive", entry.getWeight());

        // Check duplicates
        if (
            this.lruQueue.contains(entry.getKey()) ||
            this.pendingEntryQueue.containsKey(entry.getKey()) ||
            this.reservedEntryMap.containsKey(entry.getKey())
        ) {
            throw new IllegalArgumentException("Entry '" + entry.getKey() + "' already exists in the cache.");
        }

        // If max capacity exceeded, reject entry !
        if (this.currentCapacity + entry.getWeight() >= this.maxCapacity) {
            String alertMessage = String.format(
                "Cannot add entry '%s'. Cache capacity exceeded. Max capacity: %d. Eviction capacity: %d." +
                " Safe capacity: %d. Current capacity: %d. Entry capacity to reserve : %d",
                entry.getKey(),
                this.maxCapacity,
                this.evictionCapacity,
                this.safeCapacity,
                this.currentCapacity,
                entry.getWeight()
            );
            alertService.createAlert(VitamLogLevel.ERROR, alertMessage);
            throw new IllegalStateException(alertMessage);
        }

        // Reserve capacity
        this.currentCapacity += entry.getWeight();
        this.reservedEntryMap.put(entry.getKey(), entry);

        // Start async eviction process if capacity exceeds asyncEvictionCapacity
        startAsyncEvictionProcessIfNeeded();
    }

    /**
     * Confirm the reservation of a reserved entry and adds it to the cache.
     *
     * @param entryKey the entry key whose reservation is to be confirmed.
     * @throws IllegalArgumentException when provided parameters have illegal values, or entry is not reserved in cache.
     */
    public synchronized void confirmReservation(T entryKey) throws IllegalArgumentException {
        LRUCacheEntry<T> entry = reservedEntryMap.remove(entryKey);
        if (entry == null) {
            throw new IllegalArgumentException(
                "Mo active reservation for entry " + entryKey + ". Reservation already confirmed or canceled?"
            );
        }

        if (isAsyncEvictionRunning.get()) {
            // Async eviction still running ==> Do not add entry to queue otherwise, created LRUCacheEvictionJudge in eviction process does not handle new entries.
            // We'll add entry to a pending entry queue
            pendingEntryQueue.put(entryKey, entry);
        } else {
            lruQueue.add(entryKey, entry.getLastAccessInstant().toEpochMilli());
        }
        cacheEntryWeightMap.put(entryKey, entry.getWeight());
    }

    /**
     * Cancels the reservation of a reserved entry and free-up cache capacity accordingly.
     *
     * @param entryKey the entry key whose reservation is to be canceled.
     * @throws IllegalArgumentException when provided parameters have illegal values, or entry is not reserved in cache.
     */
    public synchronized void cancelReservation(T entryKey) {
        LRUCacheEntry<T> entry = reservedEntryMap.remove(entryKey);
        if (entry == null) {
            throw new IllegalArgumentException(
                "Mo active reservation for entry " + entryKey + ". Reservation already confirmed or canceled?"
            );
        }
        currentCapacity -= entry.getWeight();
    }

    /**
     * Try update an existing (reserved or added) entry last access timestamp.
     * If entry does not already exist (anymore?) in cache, no update occurs.
     *
     * @param entryKey the key of the entry to update
     * @return {@code true} if the entry timestamp has been updated, {@code false} if entry does not exist in cache.
     * @throws IllegalArgumentException when provided parameters have illegal values.
     */
    public synchronized boolean updateEntryAccessTimestamp(T entryKey, Instant updatedLastAccessInstant)
        throws IllegalArgumentException {
        ParametersChecker.checkParameter("Missing entry key", entryKey);
        ParametersChecker.checkParameter("Null last access instant", updatedLastAccessInstant);

        // Reserved entry
        LRUCacheEntry<T> existingReservedEntry = this.reservedEntryMap.get(entryKey);
        if (existingReservedEntry != null) {
            this.reservedEntryMap.put(
                    entryKey,
                    new LRUCacheEntry<>(entryKey, existingReservedEntry.getWeight(), updatedLastAccessInstant)
                );
            LOGGER.debug("Updated reserved entry '{}' timestamp", entryKey);
            return true;
        }

        // Confirmed entry pending to be added to queue
        LRUCacheEntry<T> existingPendingEntry = this.pendingEntryQueue.get(entryKey);
        if (existingPendingEntry != null) {
            // LinkedHashMap.put() preserves initial ordering when updating an entry.
            this.pendingEntryQueue.put(
                    entryKey,
                    new LRUCacheEntry<>(entryKey, existingPendingEntry.getWeight(), updatedLastAccessInstant)
                );
            LOGGER.debug("Updated pending cache entry '{}' access time to {}", entryKey, updatedLastAccessInstant);
            return true;
        }

        // Entries in LRUQueue
        if (this.lruQueue.update(entryKey, updatedLastAccessInstant.toEpochMilli())) {
            LOGGER.debug("Updated entry '{}' timestamp in queue", entryKey, updatedLastAccessInstant);
            return true;
        }

        // Entry not found
        LOGGER.debug("Ignoring Entry '{}'. Entry not found in cache. Concurrent purge?", entryKey);
        return false;
    }

    /**
     * Returns {@code true} if this cache contains the specified entry.
     * Reserved entries (not yet confirmed) or non-existing entries in the cache return {@code false}
     *
     * @param entryKey the entry key whose presence in this cache is to be tested
     * @return {@code true} if the entry exists in the cache, {@code false} otherwise.
     * @throws IllegalArgumentException when provided parameters have illegal values.
     */
    public synchronized boolean containsEntry(T entryKey) throws IllegalArgumentException {
        ParametersChecker.checkParameter("Missing entry key", entryKey);

        return this.lruQueue.contains(entryKey) || this.pendingEntryQueue.containsKey(entryKey);
    }

    /**
     * Returns {@code true} if specified entry key is reserved in the cache.
     *
     * @param entryKey the entry key whose reservation status is to be tested
     * @return {@code true} if the entry is reserved, otherwise {@code false}.
     * @throws IllegalArgumentException when provided parameters have illegal values.
     */
    public synchronized boolean isReservedEntry(T entryKey) throws IllegalArgumentException {
        ParametersChecker.checkParameter("Missing entry key", entryKey);

        return this.reservedEntryMap.containsKey(entryKey);
    }

    /**
     * Returns {@code true} if specified entry key is reserved in the cache.
     *
     * @param entryKey the entry key whose reservation status is to be tested
     * @return {@code true} if the entry is reserved, otherwise {@code false}.
     * @throws IllegalArgumentException when provided parameters have illegal values.
     */
    public synchronized LRUCacheEntry<T> getReservedEntry(T entryKey) throws IllegalArgumentException {
        ParametersChecker.checkParameter("Missing entry key", entryKey);

        return this.reservedEntryMap.get(entryKey);
    }

    /**
     * @return Max cache storage capacity of the cache. Once reached, adding new entries fails.
     */
    public long getMaxCapacity() {
        return maxCapacity;
    }

    /**
     * @return eviction capacity threshold. Once reached, background eviction process is started to purge cache of old unused entries.
     */
    public long getEvictionCapacity() {
        return evictionCapacity;
    }

    /**
     * @return Safe capacity level threshold. Cache eviction process stops when cache capacity is bellow safe cache threshold.
     */
    public long getSafeCapacity() {
        return safeCapacity;
    }

    /**
     * @return Current cache capacity.
     */
    public synchronized long getCurrentCapacity() {
        return currentCapacity;
    }

    private void startAsyncEvictionProcessIfNeeded() {
        if (this.currentCapacity >= this.evictionCapacity && !isAsyncEvictionRunning.get()) {
            isAsyncEvictionRunning.set(true);
            evictionExecutor.execute(this::asyncEvictionProcess);
            LOGGER.info(
                "Cache capacity exceeded. Background eviction process started. Max capacity: {}. " +
                "Eviction capacity: {}. Safe capacity: {}. Current capacity: {}",
                this.maxCapacity,
                this.evictionCapacity,
                this.safeCapacity,
                this.currentCapacity
            );
        }
    }

    private void asyncEvictionProcess() throws IllegalStateException {
        String initialThreadName = Thread.currentThread().getName();
        try {
            Thread.currentThread().setName("CacheEvictionProcess-" + initialThreadName);

            // Initialize eviction judge asynchronously (can be time-consuming)
            LRUCacheEvictionJudge<T> lruCacheEvictionJudge = prepareEvictionJudge();

            // Evict old entries (synchronized)
            evictOldEntries(lruCacheEvictionJudge);
        } catch (RuntimeException e) {
            LOGGER.error(e);
            alertService.createAlert(VitamLogLevel.ERROR, "Cache eviction process failed", e);
        } finally {
            finalizeEvictionProcess();

            Thread.currentThread().setName(initialThreadName);
        }
    }

    private LRUCacheEvictionJudge<T> prepareEvictionJudge() {
        LOGGER.debug("Preparing cache eviction judge...");
        LRUCacheEvictionJudge<T> lruCacheEvictionJudge = lruCacheEvictionJudgeFactory.get();
        if (lruCacheEvictionJudge == null) {
            throw new IllegalStateException("Null eviction judge");
        }
        LOGGER.debug("Cache eviction judge initialized.");
        return lruCacheEvictionJudge;
    }

    private synchronized void evictOldEntries(LRUCacheEvictionJudge<T> lruCacheEvictionJudge) {
        LOGGER.info(
            "Cache capacity exceeded. Trying to free some disk space. Max capacity: {}. " +
            "Eviction capacity: {}. Safe capacity: {}. Current capacity: {}",
            this.maxCapacity,
            this.evictionCapacity,
            this.safeCapacity,
            this.currentCapacity
        );

        // Evict oldest non-deletable entries until evictionTargetCapacity is reached
        Iterator<T> lruEntryIterator = this.lruQueue.iterator();
        while (lruEntryIterator.hasNext() && this.currentCapacity >= this.safeCapacity) {
            T entryKeyToEvict = lruEntryIterator.next();

            if (!lruCacheEvictionJudge.canEvictEntry(entryKeyToEvict)) {
                LOGGER.info(
                    "Entry {} has not been accessed recently, but is non deletable from cache",
                    entryKeyToEvict
                );
                continue;
            }

            // Evict entry
            LOGGER.info("Evicting entry: " + entryKeyToEvict);
            this.currentCapacity -= this.cacheEntryWeightMap.get(entryKeyToEvict);
            lruEntryIterator.remove();
            this.cacheEntryWeightMap.remove(entryKeyToEvict);

            // Report eviction
            this.evictionListener.accept(entryKeyToEvict);
        }

        // If target memory capacity reached : OK
        if (this.currentCapacity < this.safeCapacity) {
            LOGGER.info(
                "Enough space freed. Max capacity: {}. Eviction capacity: {}. Safe capacity: {}. Current " +
                "capacity: {}",
                this.maxCapacity,
                this.evictionCapacity,
                this.safeCapacity,
                this.currentCapacity
            );
            return;
        }

        // Target memory capacity cannot be reached, a warning should be emitted.
        String alertMessage = String.format(
            "Critical cache level. Max capacity: %d. Eviction capacity: %d. Safe capacity: %d. Current " +
            "capacity: %d",
            this.maxCapacity,
            this.evictionCapacity,
            this.safeCapacity,
            this.currentCapacity
        );
        LOGGER.warn(alertMessage);
        alertService.createAlert(VitamLogLevel.WARN, alertMessage);
    }

    private synchronized void finalizeEvictionProcess() {
        // Move pending entries to cache
        for (LRUCacheEntry<T> lruCacheEntry : this.pendingEntryQueue.values()) {
            this.lruQueue.add(lruCacheEntry.getKey(), lruCacheEntry.getLastAccessInstant().toEpochMilli());
        }
        this.pendingEntryQueue.clear();

        // Report end of eviction process
        isAsyncEvictionRunning.set(false);
    }

    public boolean isCacheEvictionRunning() {
        return isAsyncEvictionRunning.get();
    }
}
