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
package fr.gouv.vitam.logbook.common.server.reconstruction;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.time.LogicalClockRule;
import org.junit.Rule;
import org.junit.Test;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class LogbookReconstructionMetricsCacheTest {

    @Rule
    public LogicalClockRule logicalClock = new LogicalClockRule();

    @Test
    public void getFreshlyReconstructed() {
        // Given
        LogbookReconstructionMetricsCache cache = new LogbookReconstructionMetricsCache(10L, TimeUnit.MINUTES);
        logicalClock.freezeTime();

        // When
        cache.registerLastReconstructedDocumentDate(0, LocalDateUtil.now());
        Duration elapsed = cache.getLogbookOperationReconstructionLatency(0);

        // Then
        assertThat(elapsed).isEqualTo(Duration.ZERO);
    }

    @Test
    public void getMetricAfterDelay() {
        // Given
        LogbookReconstructionMetricsCache cache = new LogbookReconstructionMetricsCache(10L, TimeUnit.MINUTES);
        logicalClock.freezeTime();

        // When
        cache.registerLastReconstructedDocumentDate(0, LocalDateUtil.now());
        logicalClock.logicalSleep(5, ChronoUnit.MINUTES);
        Duration elapsed = cache.getLogbookOperationReconstructionLatency(0);

        // Then
        assertThat(elapsed).isEqualTo(Duration.of(5, ChronoUnit.MINUTES));
    }

    @Test
    public void getMetricAfterExpiration() {
        // Given
        LogbookReconstructionMetricsCache cache = new LogbookReconstructionMetricsCache(10L, TimeUnit.MINUTES);
        logicalClock.freezeTime();

        // When
        cache.registerLastReconstructedDocumentDate(0, LocalDateUtil.now());
        logicalClock.logicalSleep(11, ChronoUnit.MINUTES);
        Duration elapsed = cache.getLogbookOperationReconstructionLatency(0);

        // Then
        assertThat(elapsed).isNull();
    }

    @Test
    public void getUpdatedMetric() {
        // Given
        LogbookReconstructionMetricsCache cache = new LogbookReconstructionMetricsCache(10L, TimeUnit.MINUTES);
        logicalClock.freezeTime();

        // When
        cache.registerLastReconstructedDocumentDate(0, LocalDateUtil.now());
        logicalClock.logicalSleep(2, ChronoUnit.MINUTES);
        cache.registerLastReconstructedDocumentDate(0, LocalDateUtil.now());
        logicalClock.logicalSleep(3, ChronoUnit.MINUTES);
        Duration elapsed = cache.getLogbookOperationReconstructionLatency(0);

        // Then
        assertThat(elapsed).isEqualTo(Duration.of(3, ChronoUnit.MINUTES));
    }

    @Test
    public void testComplex() {
        // Given
        LogbookReconstructionMetricsCache cache = new LogbookReconstructionMetricsCache(10L, TimeUnit.MINUTES);
        logicalClock.freezeTime();

        // When
        cache.registerLastReconstructedDocumentDate(0, LocalDateUtil.now());
        cache.registerLastReconstructedDocumentDate(2, LocalDateUtil.now().minus(2, ChronoUnit.MINUTES));

        logicalClock.logicalSleep(9, ChronoUnit.MINUTES);

        cache.registerLastReconstructedDocumentDate(0, LocalDateUtil.now());
        cache.registerLastReconstructedDocumentDate(1, LocalDateUtil.now().minus(15, ChronoUnit.MINUTES));
        cache.registerLastReconstructedDocumentDate(3, LocalDateUtil.now().minus(30, ChronoUnit.MINUTES));

        logicalClock.logicalSleep(2, ChronoUnit.MINUTES);

        // Then
        assertThat(cache.getLogbookOperationReconstructionLatency(0)).isEqualTo(Duration.of(2, ChronoUnit.MINUTES));
        assertThat(cache.getLogbookOperationReconstructionLatency(2)).isNull();
        assertThat(cache.getLogbookOperationReconstructionLatency(1)).isEqualTo(Duration.of(17, ChronoUnit.MINUTES));
        assertThat(cache.getLogbookOperationReconstructionLatency(3)).isEqualTo(Duration.of(32, ChronoUnit.MINUTES));
    }
}
