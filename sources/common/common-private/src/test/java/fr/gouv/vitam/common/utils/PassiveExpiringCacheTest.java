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
package fr.gouv.vitam.common.utils;

import fr.gouv.vitam.common.metrics.PassiveExpiringCache;
import fr.gouv.vitam.common.time.LogicalClockRule;
import org.junit.Rule;
import org.junit.Test;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class PassiveExpiringCacheTest {

    @Rule
    public LogicalClockRule logicalClock = new LogicalClockRule();

    @Test
    public void getNonExistingEntry() {
        // Given
        PassiveExpiringCache<String, String> cache = new PassiveExpiringCache<>(10L, TimeUnit.MINUTES);

        // When
        String value = cache.get("key");

        // Then
        assertThat(value).isNull();
    }

    @Test
    public void getExistingNonExpiredEntry() {
        // Given
        PassiveExpiringCache<String, String> cache = new PassiveExpiringCache<>(10L, TimeUnit.MINUTES);

        // When
        cache.put("key", "value");
        logicalClock.logicalSleep(9, ChronoUnit.MINUTES);
        String value = cache.get("key");

        // Then
        assertThat(value).isEqualTo("value");
    }

    @Test
    public void getExistingExpiredEntry() {
        // Given
        PassiveExpiringCache<String, String> cache = new PassiveExpiringCache<>(10L, TimeUnit.MINUTES);

        // When
        cache.put("key", "value");
        logicalClock.logicalSleep(11, ChronoUnit.MINUTES);
        String value = cache.get("key");

        // Then
        assertThat(value).isNull();
    }

    @Test
    public void replaceNonExpiredEntry() {
        // Given
        PassiveExpiringCache<String, String> cache = new PassiveExpiringCache<>(10L, TimeUnit.MINUTES);

        // When
        cache.put("key", "value1");
        logicalClock.logicalSleep(9, ChronoUnit.MINUTES);
        cache.put("key", "value2");
        logicalClock.logicalSleep(9, ChronoUnit.MINUTES);
        String value = cache.get("key");

        // Then
        assertThat(value).isEqualTo("value2");
    }

    @Test
    public void replaceExpiredEntry() {
        // Given
        PassiveExpiringCache<String, String> cache = new PassiveExpiringCache<>(10L, TimeUnit.MINUTES);

        // When
        cache.put("key", "value1");
        logicalClock.logicalSleep(11, ChronoUnit.MINUTES);
        cache.put("key", "value2");
        logicalClock.logicalSleep(9, ChronoUnit.MINUTES);
        String value = cache.get("key");

        // Then
        assertThat(value).isEqualTo("value2");
    }

    @Test
    public void replaceNonExpiredEntryAfterExpirationOfNewValue() {
        // Given
        PassiveExpiringCache<String, String> cache = new PassiveExpiringCache<>(10L, TimeUnit.MINUTES);

        // When
        cache.put("key", "value1");
        logicalClock.logicalSleep(9, ChronoUnit.MINUTES);
        cache.put("key", "value2");
        logicalClock.logicalSleep(11, ChronoUnit.MINUTES);
        String value = cache.get("key");

        // Then
        assertThat(value).isNull();
    }

    @Test
    public void replaceExpiredEntryAfterExpirationOfNewValue() {
        // Given
        PassiveExpiringCache<String, String> cache = new PassiveExpiringCache<>(10L, TimeUnit.MINUTES);

        // When
        cache.put("key", "value1");
        logicalClock.logicalSleep(11, ChronoUnit.MINUTES);
        cache.put("key", "value2");
        logicalClock.logicalSleep(11, ChronoUnit.MINUTES);
        String value = cache.get("key");

        // Then
        assertThat(value).isNull();
    }

    @Test
    public void testComplex() {
        // Given
        PassiveExpiringCache<String, String> cache = new PassiveExpiringCache<>(10L, TimeUnit.MINUTES);

        // When
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.put("key3", "value3");
        cache.put("key4", "value4");

        logicalClock.logicalSleep(6, ChronoUnit.MINUTES);
        cache.put("key2", "value2-2");

        logicalClock.logicalSleep(2, ChronoUnit.MINUTES);
        cache.put("key1", "value1-2");
        cache.put("key1", "value1-3");

        logicalClock.logicalSleep(3, ChronoUnit.MINUTES);
        cache.put("key4", "value4-2");

        logicalClock.logicalSleep(6, ChronoUnit.MINUTES);

        // Then
        assertThat(cache.get("key1")).isEqualTo("value1-3");
        assertThat(cache.get("key2")).isNull();
        assertThat(cache.get("key3")).isNull();
        assertThat(cache.get("key4")).isEqualTo("value4-2");
    }
}
