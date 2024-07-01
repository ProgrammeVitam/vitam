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
package fr.gouv.vitam.common.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import fr.gouv.vitam.common.VitamConfiguration;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Abstract vitam cache
 */
public abstract class AbstractVitamCache<T, V> implements VitamCache<T, V> {

    LoadingCache<T, V> cache;

    public AbstractVitamCache() {
        this(true, Math.max(Runtime.getRuntime().availableProcessors(), 32));
    }

    public AbstractVitamCache(boolean enableStats, int concurrencyLevel) {
        if (concurrencyLevel <= 0) {
            concurrencyLevel = Math.max(Runtime.getRuntime().availableProcessors(), 32);
        }

        buildCache(enableStats, concurrencyLevel);
    }

    private void buildCache(boolean enableStats, int concurrencyLevel) {
        CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder()
            .maximumSize(VitamConfiguration.getMaxCacheEntries());
        if (enableStats) {
            builder.recordStats();
        }

        cache = builder
            .softValues()
            .concurrencyLevel(concurrencyLevel)
            .expireAfterAccess(VitamConfiguration.getExpireCacheEntriesDelay(), TimeUnit.SECONDS)
            .build(
                new CacheLoader<T, V>() {
                    @Override
                    public V load(T key) {
                        return loadByKey(key);
                    }

                    @Override
                    public Map<T, V> loadAll(Iterable<? extends T> keys) {
                        return loadByKeys(keys);
                    }
                }
            );
    }

    protected abstract Map<T, V> loadByKeys(Iterable<? extends T> keys);

    protected abstract V loadByKey(T key);

    @Override
    public LoadingCache<T, V> getCache() {
        return cache;
    }
}
