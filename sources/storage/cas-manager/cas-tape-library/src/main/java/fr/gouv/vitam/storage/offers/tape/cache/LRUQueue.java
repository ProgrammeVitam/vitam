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

import javax.annotation.concurrent.NotThreadSafe;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

/**
 * Sorted Least Recently Used (LRU) queue implementation.
 * Entries are sorted using {@code long} timestamp. Oldest entries are returned first.
 * The {@link LRUQueue} class is NOT thread safe. Concurrent access must be synchronized.
 *
 * @param <T> the type of elements maintained by this queue.
 */
@NotThreadSafe
public class LRUQueue<T> {

    private final Map<T, Long> lastAccessTimestamp;
    private final TreeSet<T> entries;

    public LRUQueue() {
        // Entries are stored is a sorted set (TreeSet) using a timestamp comparator (HashMap<T: Key, Long: timestamp>)
        this.lastAccessTimestamp = new HashMap<>();
        Comparator<T> entryTimestampComparator = Comparator.comparing(this.lastAccessTimestamp::get);
        this.entries = new TreeSet<>(entryTimestampComparator);
    }

    /**
     * Adds an entry to the queue.
     * If entry already exists, an {@link IllegalArgumentException} is thrown.
     *
     * @param entry the entry to add to the queue
     * @param timestamp the entry timestamp to set
     * @throws IllegalArgumentException if entry already exists
     */
    public void add(T entry, long timestamp) throws IllegalArgumentException {
        ParametersChecker.checkParameter("Null entry", entry);

        if (this.lastAccessTimestamp.containsKey(entry)) {
            throw new IllegalArgumentException("Duplicate entry");
        }

        // Add entry with new timestamp
        this.lastAccessTimestamp.put(entry, timestamp);
        this.entries.add(entry);
    }

    /**
     * Updates an existing entry timestamp
     *
     * @param entry the existing entry to update
     * @param timestamp the entry timestamp to update
     * @return {@code true} is entry has been updated, {@code false} if entry was not found.
     */
    public boolean update(T entry, long timestamp) {
        ParametersChecker.checkParameter("Null entry", entry);

        if (!this.lastAccessTimestamp.containsKey(entry)) {
            return false;
        }

        // Force remove entry
        this.entries.remove(entry);

        // Add entry with new timestamp
        this.lastAccessTimestamp.put(entry, timestamp);
        this.entries.add(entry);
        return true;
    }

    /**
     * Returns {@code true} if this queue contains the specified entry.
     *
     * @param entry the entry whose presence in this queue is to be tested
     * @return {@code true} is the entry exists in the queue.
     */
    public boolean contains(T entry) {
        return this.lastAccessTimestamp.containsKey(entry);
    }

    /**
     * Removes an entry from the queue
     *
     * @param entry the entry to remove
     * @return {@code true} is the entry was removed, {@code false} otherwise.
     */
    public boolean remove(T entry) {
        if (!lastAccessTimestamp.containsKey(entry)) {
            return false;
        }

        this.entries.remove(entry);
        this.lastAccessTimestamp.remove(entry);
        return true;
    }

    /**
     * Returns an iterator over the queue. Older entries are returned first.
     * Iterator supports {@link Iterator#remove()} to remove entries while iterating.
     *
     * @return An iterator over the elements of the queue.
     */
    public Iterator<T> iterator() {
        Iterator<T> queueIterator = this.entries.iterator();

        return new Iterator<>() {
            private T lastEntry;

            @Override
            public boolean hasNext() {
                return queueIterator.hasNext();
            }

            @Override
            public T next() {
                lastEntry = queueIterator.next();
                return lastEntry;
            }

            @Override
            public void remove() {
                queueIterator.remove();
                // Ensure lastAccessTimestamp is purged
                lastAccessTimestamp.remove(lastEntry);
            }
        };
    }

    /**
     * @return {@code true} is the queue is empty, {@code false} otherwise.
     */
    public boolean isEmpty() {
        return this.entries.isEmpty();
    }

    /**
     * @return queue size
     */
    public int size() {
        return this.entries.size();
    }
}
