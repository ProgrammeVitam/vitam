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
package fr.gouv.vitam.metadata.core.metrics;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.time.LogicalClockRule;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import org.junit.Rule;
import org.junit.Test;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class MetadataMetadataReconstructionMetricsCacheTest {

    @Rule
    public LogicalClockRule logicalClock = new LogicalClockRule();

    @Test
    public void getFreshlyReconstructedDocumentMetrics() {
        // Given
        MetadataReconstructionMetricsCache cache = new MetadataReconstructionMetricsCache(10L, TimeUnit.MINUTES);
        logicalClock.freezeTime();

        // When
        cache.registerLastDocumentReconstructionDate(MetadataCollections.UNIT, 0, "default", LocalDateUtil.now());
        Duration elapsed = cache.getDocumentReconstructionLatency(MetadataCollections.UNIT, 0, "default");

        // Then
        assertThat(elapsed).isEqualTo(Duration.ZERO);
    }

    @Test
    public void getFreshlyReconstructedGraphMetrics() {
        // Given
        MetadataReconstructionMetricsCache cache = new MetadataReconstructionMetricsCache(10L, TimeUnit.MINUTES);
        logicalClock.freezeTime();

        // When
        cache.registerLastGraphReconstructionDate(MetadataCollections.UNIT, LocalDateUtil.now());
        Duration elapsed = cache.getGraphReconstructionLatency(MetadataCollections.UNIT);

        // Then
        assertThat(elapsed).isEqualTo(Duration.ZERO);
    }

    @Test
    public void getDocumentMetricsAfterDelay() {
        // Given
        MetadataReconstructionMetricsCache cache = new MetadataReconstructionMetricsCache(10L, TimeUnit.MINUTES);
        logicalClock.freezeTime();

        // When
        cache.registerLastDocumentReconstructionDate(MetadataCollections.UNIT, 0, "default", LocalDateUtil.now());
        logicalClock.logicalSleep(5, ChronoUnit.MINUTES);
        Duration elapsed = cache.getDocumentReconstructionLatency(MetadataCollections.UNIT, 0, "default");

        // Then
        assertThat(elapsed).isEqualTo(Duration.of(5, ChronoUnit.MINUTES));
    }

    @Test
    public void getGraphMetricsAfterDelay() {
        // Given
        MetadataReconstructionMetricsCache cache = new MetadataReconstructionMetricsCache(10L, TimeUnit.MINUTES);
        logicalClock.freezeTime();

        // When
        cache.registerLastGraphReconstructionDate(MetadataCollections.UNIT, LocalDateUtil.now());
        logicalClock.logicalSleep(5, ChronoUnit.MINUTES);
        Duration elapsed = cache.getGraphReconstructionLatency(MetadataCollections.UNIT);

        // Then
        assertThat(elapsed).isEqualTo(Duration.of(5, ChronoUnit.MINUTES));
    }

    @Test
    public void getDocumentMetricsAfterExpiration() {
        // Given
        MetadataReconstructionMetricsCache cache = new MetadataReconstructionMetricsCache(10L, TimeUnit.MINUTES);
        logicalClock.freezeTime();

        // When
        cache.registerLastDocumentReconstructionDate(MetadataCollections.UNIT, 0, "default", LocalDateUtil.now());
        logicalClock.logicalSleep(11, ChronoUnit.MINUTES);
        Duration elapsed = cache.getDocumentReconstructionLatency(MetadataCollections.UNIT, 0, "default");

        // Then
        assertThat(elapsed).isNull();
    }

    @Test
    public void getGraphMetricsAfterExpiration() {
        // Given
        MetadataReconstructionMetricsCache cache = new MetadataReconstructionMetricsCache(10L, TimeUnit.MINUTES);
        logicalClock.freezeTime();

        // When
        cache.registerLastGraphReconstructionDate(MetadataCollections.UNIT, LocalDateUtil.now());
        logicalClock.logicalSleep(11, ChronoUnit.MINUTES);
        Duration elapsed = cache.getGraphReconstructionLatency(MetadataCollections.UNIT);

        // Then
        assertThat(elapsed).isNull();
    }

    @Test
    public void getUpdatedDocumentMetrics() {
        // Given
        MetadataReconstructionMetricsCache cache = new MetadataReconstructionMetricsCache(10L, TimeUnit.MINUTES);
        logicalClock.freezeTime();

        // When
        cache.registerLastDocumentReconstructionDate(MetadataCollections.UNIT, 0, "default", LocalDateUtil.now());
        logicalClock.logicalSleep(2, ChronoUnit.MINUTES);
        cache.registerLastDocumentReconstructionDate(MetadataCollections.UNIT, 0, "default", LocalDateUtil.now());
        logicalClock.logicalSleep(3, ChronoUnit.MINUTES);
        Duration elapsed = cache.getDocumentReconstructionLatency(MetadataCollections.UNIT, 0, "default");

        // Then
        assertThat(elapsed).isEqualTo(Duration.of(3, ChronoUnit.MINUTES));
    }

    @Test
    public void getUpdatedGraphMetrics() {
        // Given
        MetadataReconstructionMetricsCache cache = new MetadataReconstructionMetricsCache(10L, TimeUnit.MINUTES);
        logicalClock.freezeTime();

        // When
        cache.registerLastGraphReconstructionDate(MetadataCollections.UNIT, LocalDateUtil.now());
        logicalClock.logicalSleep(2, ChronoUnit.MINUTES);
        cache.registerLastGraphReconstructionDate(MetadataCollections.UNIT, LocalDateUtil.now());
        logicalClock.logicalSleep(3, ChronoUnit.MINUTES);
        Duration elapsed = cache.getGraphReconstructionLatency(MetadataCollections.UNIT);

        // Then
        assertThat(elapsed).isEqualTo(Duration.of(3, ChronoUnit.MINUTES));
    }

    @Test
    public void testMultipleDocumentMetrics() {
        // Given
        MetadataReconstructionMetricsCache cache = new MetadataReconstructionMetricsCache(10L, TimeUnit.MINUTES);
        logicalClock.freezeTime();

        // When
        cache.registerLastDocumentReconstructionDate(MetadataCollections.UNIT, 0, "default", LocalDateUtil.now());
        cache.registerLastDocumentReconstructionDate(
            MetadataCollections.UNIT,
            0,
            "custom",
            LocalDateUtil.now().minus(2, ChronoUnit.MINUTES)
        );

        logicalClock.logicalSleep(9, ChronoUnit.MINUTES);

        cache.registerLastDocumentReconstructionDate(MetadataCollections.UNIT, 0, "default", LocalDateUtil.now());
        cache.registerLastDocumentReconstructionDate(
            MetadataCollections.UNIT,
            1,
            "default",
            LocalDateUtil.now().minus(15, ChronoUnit.MINUTES)
        );
        cache.registerLastDocumentReconstructionDate(
            MetadataCollections.OBJECTGROUP,
            1,
            "default",
            LocalDateUtil.now().minus(30, ChronoUnit.MINUTES)
        );

        logicalClock.logicalSleep(2, ChronoUnit.MINUTES);

        // Then
        assertThat(cache.getDocumentReconstructionLatency(MetadataCollections.UNIT, 0, "default")).isEqualTo(
            Duration.of(2, ChronoUnit.MINUTES)
        );
        assertThat(cache.getDocumentReconstructionLatency(MetadataCollections.UNIT, 0, "custom")).isNull();
        assertThat(cache.getDocumentReconstructionLatency(MetadataCollections.UNIT, 1, "default")).isEqualTo(
            Duration.of(17, ChronoUnit.MINUTES)
        );
        assertThat(cache.getDocumentReconstructionLatency(MetadataCollections.OBJECTGROUP, 1, "default")).isEqualTo(
            Duration.of(32, ChronoUnit.MINUTES)
        );
    }

    @Test
    public void testMultipleGraphMetrics() {
        // Given
        MetadataReconstructionMetricsCache cache = new MetadataReconstructionMetricsCache(10L, TimeUnit.MINUTES);
        logicalClock.freezeTime();

        // When
        cache.registerLastGraphReconstructionDate(MetadataCollections.UNIT, LocalDateUtil.now());
        cache.registerLastGraphReconstructionDate(
            MetadataCollections.OBJECTGROUP,
            LocalDateUtil.now().minus(2, ChronoUnit.MINUTES)
        );

        logicalClock.logicalSleep(9, ChronoUnit.MINUTES);
        cache.registerLastGraphReconstructionDate(
            MetadataCollections.UNIT,
            LocalDateUtil.now().minus(15, ChronoUnit.MINUTES)
        );

        logicalClock.logicalSleep(2, ChronoUnit.MINUTES);

        // Then
        assertThat(cache.getGraphReconstructionLatency(MetadataCollections.UNIT)).isEqualTo(
            Duration.of(17, ChronoUnit.MINUTES)
        );
        assertThat(cache.getGraphReconstructionLatency(MetadataCollections.OBJECTGROUP)).isNull();
    }
}
