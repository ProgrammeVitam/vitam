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
package fr.gouv.vitam.common.lru;

import java.util.Collection;
import java.util.Iterator;

/**
 * Threadsafe synchronized implementation of LruCache based on LinkedHashMap. Threadsafety is provided by method
 * synchronization.
 *
 * This cache implementation should be used with low number of threads.
 *
 *
 * author Damian Momot
 *
 * @param <K> Key
 * @param <V> Value
 */
public class SynchronizedLruCache<K, V> extends AbstractLruCache<K, V> {

    /**
     * Initial capacity
     */
    public static final int DEFAULT_INITIAL_CAPACITY = 16;

    /**
     * Load factor
     */
    public static final float DEFAULT_LOAD_FACTOR = 0.75f;

    private final CapacityLruLinkedHashMap<K, InterfaceLruCacheEntry<V>> cacheMap;

    /**
     * Creates new SynchronizedLruCache
     *
     * @param capacity max cache capacity
     * @param ttl time to live in milliseconds
     * @param initialCapacity initial cache capacity
     * @param loadFactor
     */
    public SynchronizedLruCache(int capacity, long ttl, int initialCapacity, float loadFactor) {
        super(ttl);
        cacheMap = new CapacityLruLinkedHashMap<>(capacity, initialCapacity, loadFactor);
    }

    /**
     * Creates new SynchronizedLruCache with DEFAULT_LOAD_FACTOR
     *
     * @param capacity max cache capacity
     * @param ttl time to live in milliseconds
     * @param initialCapacity initial cache capacity
     */
    public SynchronizedLruCache(int capacity, long ttl, int initialCapacity) {
        this(capacity, ttl, initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    /**
     * Creates new SynchronizedLruCache with DEFAULT_LOAD_FACTOR and DEFAULT_INITIAL_CAPACITY
     *
     * @param capacity max cache capacity
     * @param ttl time to live in milliseconds
     */
    public SynchronizedLruCache(int capacity, long ttl) {
        this(capacity, ttl, DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR);
    }

    @Override
    public synchronized void clear() {
        cacheMap.clear();
    }

    @Override
    public synchronized V get(K key) { // NOSONAR do not remove since synchronized
        return super.get(key);
    }

    @Override
    public int getCapacity() {
        return cacheMap.getCapacity();
    }

    @Override
    protected InterfaceLruCacheEntry<V> getEntry(K key) {
        return cacheMap.get(key);
    }

    @Override
    public synchronized int size() {
        return cacheMap.size();
    }

    @Override
    public synchronized void put(K key, V value, long ttl) { // NOSONAR do not remove (synchronized)
        super.put(key, value, ttl);
    }

    @Override
    protected void putEntry(K key, InterfaceLruCacheEntry<V> entry) {
        cacheMap.put(key, entry);
    }

    @Override
    public synchronized V remove(K key) {
        final InterfaceLruCacheEntry<V> cv = cacheMap.remove(key);
        if (cv != null) {
            return cv.getValue();
        }
        return null;
    }

    @Override
    public synchronized int forceClearOldest() {
        final long timeRef = System.currentTimeMillis();
        final Collection<InterfaceLruCacheEntry<V>> collection = cacheMap.values();
        final Iterator<InterfaceLruCacheEntry<V>> iterator = collection.iterator();
        int nb = 0;
        while (iterator.hasNext()) {
            final InterfaceLruCacheEntry<V> v = iterator.next();
            if (!v.isStillValid(timeRef)) {
                iterator.remove();
                nb++;
            }
        }
        return nb;
    }
}
