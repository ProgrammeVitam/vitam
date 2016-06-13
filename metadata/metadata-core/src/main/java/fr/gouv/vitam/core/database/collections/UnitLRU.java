/*******************************************************************************
 * This file is part of Vitam Project.
 *
 * Copyright Vitam (2012, 2015)
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL license as circulated
 * by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL license and that you
 * accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.core.database.collections;

import static com.mongodb.client.model.Filters.eq;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import fr.gouv.vitam.common.lru.SynchronizedLruCache;
import fr.gouv.vitam.core.database.configuration.GlobalDatasDb;

/**
 * UNIT LRU in memory
 *
 */
public class UnitLRU implements Map<String, Unit> {
    /**
     * Synchronized LRU cache
     */
    final SynchronizedLruCache<String, Unit> LRU_UnitCached =
        new SynchronizedLruCache<String, Unit>(GlobalDatasDb.MAXLRU, GlobalDatasDb.TTLMS);

    /**
     * Empty constructor
     */
    public UnitLRU() {
        // empty
    }

    @Override
    public void clear() {
        LRU_UnitCached.clear();
    }

    /**
     * Clean the oldest Units from the cache
     */
    public void forceClearOldest() {
        LRU_UnitCached.forceClearOldest();
    }

    @Override
    public boolean containsKey(Object key) {
        return LRU_UnitCached.contains((String) key);
    }

    @Override
    public boolean containsValue(Object value) {
        return true;
    }

    @Override
    public Set<Entry<String, Unit>> entrySet() {
        return Collections.emptySet();
    }

    /**
     * Load from database if not already in cache. If already in cache, update TTL.
     * 
     * @param key
     * @return the associated Unit from Cache (only VITAM PROJECTION)
     */
    @Override
    public Unit get(Object key) {
        Unit unit = LRU_UnitCached.get((String) key);
        if (unit == null) {
            unit = (Unit) MongoDbMetadataHelper.select(MongoDbAccess.VitamCollections.Cunit,
                eq(VitamDocument.ID, key), Unit.UNIT_VITAM_PROJECTION).first();
            if (unit == null) {
                return null;
            }
            LRU_UnitCached.put((String) key, unit);
        } else {
            LRU_UnitCached.updateTtl((String) key);
        }
        return unit;
    }

    @Override
    public boolean isEmpty() {
        return LRU_UnitCached.isEmpty();
    }

    @Override
    public Set<String> keySet() {
        return null;
    }

    @Override
    public Unit put(String key, Unit value) {
        LRU_UnitCached.put(key, value);
        return value;
    }

    @Override
    public void putAll(Map<? extends String, ? extends Unit> map) {
        for (final java.util.Map.Entry<? extends String, ? extends Unit> entry : map.entrySet()) {
            LRU_UnitCached.put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public Unit remove(Object key) {
        return LRU_UnitCached.remove((String) key);
    }

    @Override
    public int size() {
        return LRU_UnitCached.size();
    }

    @Override
    public Collection<Unit> values() {
        return null;
    }
}
