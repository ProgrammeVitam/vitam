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
    private static final int INTERVAL = 5;
    private static final TimeUnit INTERVAL_UNIT = TimeUnit.MINUTES;
    private final List<GarbageCollectorMXBean> garbageCollectors;
    private long tick;
    private long gcDelta;

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

        tick = clock.getTick();
        gcDelta = getElapsedGCTime();
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
        final long gcElaspedTime = getElapsedGCTime();
        Double value =
            (double) (gcElaspedTime - gcDelta) / (double) TimeUnit.NANOSECONDS.toMillis(currentTick - tick);

        if (value.isNaN()) {
            value = (double) 0;
            // System.out.println("GC elapsed time since last call: " + (gcElaspedTime - gcDelta));
            // System.out.println("Elapsed time since last call: " + TimeUnit.NANOSECONDS.toMillis((currentTick -
            // tick)));
        }

        tick = currentTick;
        gcDelta = gcElaspedTime;

        return value;
    }
}
