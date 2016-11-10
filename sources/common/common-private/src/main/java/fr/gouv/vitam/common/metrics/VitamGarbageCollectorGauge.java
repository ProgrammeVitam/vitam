package fr.gouv.vitam.common.metrics;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Clock;
import com.codahale.metrics.Gauge;

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
            (double) (currentGcElapsedTime - lastGcElapsedTime) / (double) TimeUnit.NANOSECONDS.toMillis(currentTick - lastTick);

        if (value.isNaN()) {
            value = (double) 0;
        }

        lastTick = currentTick;
        lastGcElapsedTime = currentGcElapsedTime;

        return value;
    }
}
