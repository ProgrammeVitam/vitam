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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Capacity based LRU version of LinkedHashMap
 * 
 * 
 * author Damian Momot
 * 
 */
class CapacityLruLinkedHashMap<K, V> extends LinkedHashMap<K, V> {
    /**
     * 
     */
    private static final long serialVersionUID = -5064888079959689619L;

    private final int capacity;

    /**
     * Creates LRU LinkedHashMap
     * 
     * @param capacity
     * @param initialCapacity
     * @param loadFactor
     * @throws IllegalArgumentException
     *             if capacity is not positive
     */
    protected CapacityLruLinkedHashMap(int capacity, int initialCapacity,
            float loadFactor) {
        super(initialCapacity, loadFactor, true);

        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }

        this.capacity = capacity;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > capacity;
    }

    /**
     * Returns capacity of map
     * 
     * @return the capacity
     */
    public int getCapacity() {
        return this.capacity;
    }

}
