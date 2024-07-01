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

import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class DynamicGauge extends Collector implements Collector.Describable {

    private final String name;
    private final String help;
    private final List<String> labelNames;

    private final Supplier<Map<List<String>, Double>> metricsProvider;

    /***
     *
     * @param name name of the Gauge
     * @param help Humain friendly description of the Gauge
     * @param labelNames label names
     * @param metricsSupplier a side-effect-free / non-blocking function returning the gauge value per label values
     */
    public DynamicGauge(
        String name,
        String help,
        List<String> labelNames,
        Supplier<Map<List<String>, Double>> metricsSupplier
    ) {
        this.name = name;
        this.help = help;
        this.labelNames = labelNames;
        this.metricsProvider = metricsSupplier;
    }

    @Override
    public List<MetricFamilySamples> describe() {
        return List.of(new GaugeMetricFamily(this.name, this.help, this.labelNames));
    }

    @Override
    public List<MetricFamilySamples> collect() {
        GaugeMetricFamily metricFamily = new GaugeMetricFamily(this.name, this.help, this.labelNames);
        Map<List<String>, Double> metricValuesByLabelValues = this.metricsProvider.get();

        for (Map.Entry<List<String>, Double> entry : metricValuesByLabelValues.entrySet()) {
            if (entry.getValue() != null) {
                metricFamily.addMetric(entry.getKey(), entry.getValue());
            }
        }

        return List.of(metricFamily);
    }
}
