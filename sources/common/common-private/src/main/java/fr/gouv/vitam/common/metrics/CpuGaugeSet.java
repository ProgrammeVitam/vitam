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

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Get the cpu metrics
 */
public class CpuGaugeSet implements MetricSet {
    private final OperatingSystemMXBean operatingSystemMXBean;

    private CpuGaugeSet(OperatingSystemMXBean operatingSystemMXBean) {
        this.operatingSystemMXBean = operatingSystemMXBean;
    }

    @Override
    public Map<String, Metric> getMetrics() {
        if (!(operatingSystemMXBean instanceof com.sun.management.OperatingSystemMXBean)) {
            return Collections.emptyMap();
        }

        final com.sun.management.OperatingSystemMXBean osMxBean =
            (com.sun.management.OperatingSystemMXBean) operatingSystemMXBean;

        final Map<String, Metric> gauges = new HashMap<>();

        gauges.put("process-cpu-load-percentage-percent",
            (Gauge<Double>) () -> osMxBean.getProcessCpuLoad());

        gauges.put("system-cpu-load-percentage-percent",
            (Gauge<Double>) () -> osMxBean.getSystemCpuLoad());

        //gauges.put("system-load-average", (Gauge<Double>) () -> osMxBean.getSystemLoadAverage());

        //gauges.put("process-cpu-time-ns", (Gauge<Long>) () -> osMxBean.getProcessCpuTime());

        return Collections.unmodifiableMap(gauges);
    }

    public static CpuGaugeSet create() {
        return new CpuGaugeSet(ManagementFactory.getOperatingSystemMXBean());
    }

}
