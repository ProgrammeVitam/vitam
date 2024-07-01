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

import fr.gouv.vitam.common.exception.VitamException;

import java.util.concurrent.Callable;

/**
 * Base class for concrete implementations
 *
 *
 * author Damian Momot
 *
 * @param <K> Key
 * @param <V> Value
 */
abstract class AbstractLruCache<K, V> implements InterfaceLruCache<K, V> {

    private long ttl;

    /**
     * Constructs BaseLruCache
     *
     * @param ttl
     * @throws IllegalArgumentException if ttl is not positive
     */
    protected AbstractLruCache(long ttl) {
        if (ttl <= 0) {
            throw new IllegalArgumentException("ttl must be positive");
        }

        this.ttl = ttl;
    }

    @Override
    public boolean contains(K key) {
        // can't use contains because of expiration policy
        final V value = get(key);

        return value != null;
    }

    /**
     * Creates new LruCacheEntry<V>.
     *
     * It can be used to change implementation of LruCacheEntry
     *
     * @param value
     * @param ttl
     * @return LruCacheEntry<V>
     */
    protected InterfaceLruCacheEntry<V> createEntry(V value, long ttl) {
        return new StrongReferenceCacheEntry<>(value, ttl);
    }

    @Override
    public V get(K key) {
        return getValue(key);
    }

    @Override
    public V get(K key, Callable<V> callback) throws VitamException {
        return get(key, callback, ttl);
    }

    @Override
    public V get(K key, Callable<V> callback, long ttl) throws VitamException {
        V value = get(key);

        // if element doesn't exist create it using callback
        if (value == null) {
            try {
                value = callback.call();
            } catch (final Exception e) {
                throw new VitamException(e);
            }
            put(key, value, ttl);
        }

        return value;
    }

    @Override
    public long getTtl() {
        return ttl;
    }

    @Override
    public void setNewTtl(long ttl) {
        if (ttl <= 0) {
            throw new IllegalArgumentException("ttl must be positive");
        }
        this.ttl = ttl;
    }

    /**
     * Returns LruCacheEntry mapped by key or null if it does not exist
     *
     * @param key
     * @return LruCacheEntry<V>
     */
    protected abstract InterfaceLruCacheEntry<V> getEntry(K key);

    @Override
    public void updateTtl(K key) {
        final InterfaceLruCacheEntry<V> cacheEntry = getEntry(key);
        if (cacheEntry != null) {
            cacheEntry.resetTime(ttl);
        }
    }

    /**
     * Tries to retrieve value by it's key. Automatically removes entry if it's not valid (LruCacheEntry.getValue()
     * returns null)
     *
     * @param key
     * @return Value
     */
    protected V getValue(K key) {
        V value = null;

        final InterfaceLruCacheEntry<V> cacheEntry = getEntry(key);

        if (cacheEntry != null) {
            value = cacheEntry.getValue();

            // autoremove entry from cache if it's not valid
            if (value == null) {
                remove(key);
            }
        }

        return value;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public void put(K key, V value) {
        put(key, value, ttl);
    }

    @Override
    public void put(K key, V value, long ttl) {
        if (value != null) {
            putEntry(key, createEntry(value, ttl));
        }
    }

    /**
     * Puts entry into cache
     *
     * @param key
     * @param entry
     */
    protected abstract void putEntry(K key, InterfaceLruCacheEntry<V> entry);
}
