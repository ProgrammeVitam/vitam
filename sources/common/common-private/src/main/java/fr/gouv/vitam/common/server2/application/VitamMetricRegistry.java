package fr.gouv.vitam.common.server2.application;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;

import fr.gouv.vitam.common.ParametersChecker;

/**
 * A class extending the {@ MetricRegistry} to expose safe functions to register metrics.
 */
final public class VitamMetricRegistry extends MetricRegistry {
    private static final String VITAM_METRIC_REGISTRY_PARAMS = "VitamMetricRegistry parameters";

    /**
     * {@ VitamMetricRegistry} constructor
     */
    public VitamMetricRegistry() {
        // empty
    }

    /**
     * Return the {@link Metric} registered under this name; or create and register a new {@code metric} if none is
     * registered.
     *
     * @param name the name of the metric
     * @return a new or pre-existing {@code metric}
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends Metric> T register(String name, T metric) {
        ParametersChecker.checkParameter(VITAM_METRIC_REGISTRY_PARAMS, name, metric);

        if (!super.getMetrics().containsKey(name)) {
            super.register(name, metric);
        }

        return (T) super.getMetrics().get(name);
    }


}
