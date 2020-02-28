/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
 * LRU cache interface.
 *
 *
 * author Damian Momot
 *
 * @param <K> Key
 * @param <V> Value
 *
 */
public interface InterfaceLruCache<K, V> {
    /**
     * Removes all entries from cache
     */
    void clear();

    /**
     * Removes all oldest entries from cache (ttl based)
     *
     * @return the number of removed entries
     */
    int forceClearOldest();

    /**
     * Checks whether cache contains valid entry for key
     *
     * @param key
     * @return true if cache contains key and entry is valid
     */
    boolean contains(K key);

    /**
     * Returns value cached with key.
     *
     * @param key
     * @return value or null if key doesn't exist or entry is not valid
     */
    V get(K key);

    /**
     * Tries to get element from cache. If get fails callback is used to create element and returned value is stored in
     * cache.
     *
     * Default TTL is used
     *
     * @param key
     * @param callback
     * @return Value
     * @throws VitamException
     * @throws Exception if callback throws exception
     */
    V get(K key, Callable<V> callback) throws VitamException;

    /**
     * Tries to get element from cache. If get fails callback is used to create element and returned value is stored in
     * cache
     *
     * @param key
     * @param callback
     * @param ttl time to live in milliseconds
     * @return Value
     * @throws VitamException
     * @throws Exception if callback throws exception
     */
    V get(K key, Callable<V> callback, long ttl) throws VitamException;

    /**
     * Returns cache capacity
     *
     * @return capacity of cache
     */
    int getCapacity();

    /**
     * Returns number of entries stored in cache (including invalid ones)
     *
     * @return number of entries
     */
    int size();

    /**
     * Returns cache TTL
     *
     * @return ttl in milliseconds
     */
    long getTtl();

    /**
     * Set a new TTL (for newly set objects only, not changing old values).
     *
     * @param ttl
     */
    void setNewTtl(long ttl);

    /**
     * Checks whether cache is empty.
     *
     * If any entry exists (including invalid one) this method will return true
     *
     * @return true if no entries are stored in cache
     */
    boolean isEmpty();

    /**
     * Puts value under key into cache. Default TTL is used
     *
     * @param key
     * @param value
     */
    void put(K key, V value);

    /**
     * Puts value under key into cache with desired TTL
     *
     * @param key
     * @param value
     * @param ttl time to live in milliseconds
     */
    void put(K key, V value, long ttl);

    /**
     * Removes entry from cache (if exists)
     *
     * @param key
     * @return the value if it still exists
     */
    V remove(K key);

    /**
     * Update the TTL of the associated object if it still exists
     *
     * @param key
     */
    void updateTtl(K key);
}
