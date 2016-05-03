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

/**
 * 
 * author Damian Momot
 * 
 */
class StrongReferenceCacheEntry<V> implements InterfaceLruCacheEntry<V> {
    private final V value;

    private long expirationTime;

    /**
     * Creates StrongReferencyCacheEntry with desired ttl
     * 
     * @param value
     * @param ttl
     *            time to live in milliseconds
     * @throws IllegalArgumentException
     *             if ttl is not positive
     */
    StrongReferenceCacheEntry(V value, long ttl) {
        if (ttl <= 0) {
            throw new IllegalArgumentException("ttl must be positive");
        }
        this.value = value;
        expirationTime = System.currentTimeMillis() + ttl;
    }

    /**
     * Returns value if entry is valid, null otherwise.
     * 
     * Entry is invalid if it's expired
     * 
     * @return value if entry is valid
     */
    public V getValue() {
        if (System.currentTimeMillis() > expirationTime) {
            return null;
        } else {
            return value;
        }
    }

    public boolean isStillValid(long timeRef) {
        return (timeRef <= expirationTime);
    }

    public boolean resetTime(long ttl) {
        expirationTime = System.currentTimeMillis() + ttl;
        return true;
    }
}
