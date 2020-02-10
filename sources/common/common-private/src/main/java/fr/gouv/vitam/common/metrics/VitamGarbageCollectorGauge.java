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
package fr.gouv.vitam.common.metrics;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Clock;
import com.codahale.metrics.Gauge;

/**
 * Garbage Collector Gauge
 */
public class VitamGarbageCollectorGauge implements Gauge<Double> {

    private static final Clock clock = Clock.defaultClock();
    private final List<GarbageCollectorMXBean> garbageCollectors;
    private long lastTick;
    private long lastGcElapsedTime;

    /**
     * Creates a new gauge for all discoverable garbage collectors.
     */
    public VitamGarbageCollectorGauge() {
        this(ManagementFactory.getGarbageCollectorMXBeans());
    }

    /**
     * Creates a new gauge for the given collection of garbage collectors.
     *
     * @param garbageCollectors the garbage collectors
     */
    public VitamGarbageCollectorGauge(Collection<GarbageCollectorMXBean> garbageCollectors) {
        this.garbageCollectors = new ArrayList<>(garbageCollectors);

        lastTick = clock.getTick();
        lastGcElapsedTime = getElapsedGCTime();
    }

    private long getElapsedGCTime() {
        long time = 0L;

        for (final GarbageCollectorMXBean gc : garbageCollectors) {
            if (gc.getCollectionTime() != -1) {
                time += gc.getCollectionTime();
            }
        }

        return time;
    }

    // TODO P1
    // Do not calculate the delta between call and previous call but instead keep a continuous calculation on the X past
    // minutes.
    @Override
    public Double getValue() {
        final long currentTick = clock.getTick();
        final long currentGcElapsedTime = getElapsedGCTime();
        Double value =
            (double) (currentGcElapsedTime - lastGcElapsedTime) /
                (double) TimeUnit.NANOSECONDS.toMillis(currentTick - lastTick);

        if (value.isNaN()) {
            value = (double) 0;
        }

        lastTick = currentTick;
        lastGcElapsedTime = currentGcElapsedTime;

        return value;
    }
}
