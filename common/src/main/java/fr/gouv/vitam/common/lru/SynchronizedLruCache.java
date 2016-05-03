/**
 * This file is part of Vitam Project.
 * 
 * Copyright 2009, Frederic Bregier, and individual contributors by the author
 * tags. See the COPYRIGHT.txt in the distribution for a full listing of
 * individual contributors.
 * 
 * All Vitam Project is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 * 
 * Vitam is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * Vitam . If not, see <http://www.gnu.org/licenses/>.
 */

package fr.gouv.vitam.common.lru;

import java.util.Collection;
import java.util.Iterator;

/**
 * Threadsafe synchronized implementation of LruCache based on LinkedHashMap.
 * Threadsafety is provided by method synchronization.
 * 
 * This cache implementation should be used with low number of threads.
 * 
 * 
 * author Damian Momot
 * 
 * @param <K>
 *            Key
 * @param <V>
 *            Value
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
     * @param capacity
     *            max cache capacity
     * @param ttl
     *            time to live in milliseconds
     * @param initialCapacity
     *            initial cache capacity
     * @param loadFactor
     */
    public SynchronizedLruCache(int capacity, long ttl, int initialCapacity,
            float loadFactor) {
        super(ttl);
        cacheMap = new CapacityLruLinkedHashMap<K, InterfaceLruCacheEntry<V>>(
                capacity, initialCapacity, loadFactor);
    }

    /**
     * Creates new SynchronizedLruCache with DEFAULT_LOAD_FACTOR
     * 
     * @param capacity
     *            max cache capacity
     * @param ttl
     *            time to live in milliseconds
     * @param initialCapacity
     *            initial cache capacity
     */
    public SynchronizedLruCache(int capacity, long ttl, int initialCapacity) {
        this(capacity, ttl, initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    /**
     * Creates new SynchronizedLruCache with DEFAULT_LOAD_FACTOR and
     * DEFAULT_INITIAL_CAPACITY
     * 
     * @param capacity
     *            max cache capacity
     * @param ttl
     *            time to live in milliseconds
     */
    public SynchronizedLruCache(int capacity, long ttl) {
        this(capacity, ttl, DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR);
    }

    public synchronized void clear() {
        cacheMap.clear();
    }

    @Override
    public synchronized V get(K key) {
        return super.get(key);
    }

    public int getCapacity() {
        return cacheMap.getCapacity();
    }

    @Override
    protected InterfaceLruCacheEntry<V> getEntry(K key) {
        return cacheMap.get(key);
    }

    public synchronized int size() {
        return cacheMap.size();
    }

    public synchronized void put(K key, V value, long ttl) {
        super.put(key, value, ttl);
    }

    @Override
    protected void putEntry(K key, InterfaceLruCacheEntry<V> entry) {
        cacheMap.put(key, entry);
    }

    public synchronized V remove(K key) {
        InterfaceLruCacheEntry<V> cv = cacheMap.remove(key);
        if (cv != null) {
            return cv.getValue();
        }
        return null;
    }

    public synchronized int forceClearOldest() {
        long timeRef = System.currentTimeMillis();
        Collection<InterfaceLruCacheEntry<V>> collection = cacheMap.values();
        Iterator<InterfaceLruCacheEntry<V>> iterator = collection.iterator();
        int nb = 0;
        while (iterator.hasNext()) {
            InterfaceLruCacheEntry<V> v = iterator.next();
            if (!v.isStillValid(timeRef)) {
                iterator.remove();
                nb++;
            }
        }
        return nb;
    }

}
