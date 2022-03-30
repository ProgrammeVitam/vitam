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

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

/**
 * A class extending the MetricRegistry to expose safe functions to register metrics.
 */
public final class VitamMetricRegistry extends MetricRegistry {
    private static final String VITAM_METRIC_REGISTRY_PARAMS = "VitamMetricRegistry parameters";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(VitamMetricRegistry.class);

    /**
     * VitamMetricRegistry constructor
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
        } else {
            // Erase previous metric, warn the user and register the new metric.
            LOGGER.warn("Metric " + name + " already exists. Erasing and replacing with the new one.");
            super.remove(name);
            super.register(name, metric);
        }

        return (T) super.getMetrics().get(name);
    }


}
