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
package fr.gouv.vitam.common.metrics;

import fr.gouv.vitam.common.LocalDateUtil;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.concurrent.ThreadSafe;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * {@link org.apache.commons.collections4.map.PassiveExpiringMap}-like cache implementation with Vitam LocalDate (simplifies time-based unit tests)
 */
@ThreadSafe
public final class PassiveExpiringCache<K, V> {

    private final Duration cacheValidityDuration;
    private final Map<K, Pair<Instant, V>> cacheMap;

    public PassiveExpiringCache(long cacheDuration, TimeUnit cacheDurationUnit) {
        this.cacheValidityDuration = Duration.of(cacheDuration, cacheDurationUnit.toChronoUnit());

        // Access to cache is synchronized
        this.cacheMap = new HashMap<>();
    }

    public synchronized void put(K key, V value) {
        cacheMap.put(key, new ImmutablePair<>(LocalDateUtil.getInstant(), value));
    }

    public synchronized V get(K key) {
        Pair<Instant, V> valueWithTimestamp = this.cacheMap.get(key);
        if (valueWithTimestamp == null) {
            return null;
        }

        Instant now = LocalDateUtil.getInstant();
        if (Duration.between(valueWithTimestamp.getLeft(), now).compareTo(cacheValidityDuration) > 0) {
            // Cache entry expired
            this.cacheMap.remove(key);
            return null;
        }

        return valueWithTimestamp.getRight();
    }
}
