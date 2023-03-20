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

import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.metrics.GaugeUtils;
import fr.gouv.vitam.common.metrics.VitamMetricsNames;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;

import javax.annotation.concurrent.ThreadSafe;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@ThreadSafe
public final class MetadataReconstructionMetrics {

    private static final String TENANT_LABEL = "tenant";
    private static final String COLLECTION_LABEL = "collection";
    private static final String STRATEGY_LABEL = "strategy";
    private final static AtomicBoolean isInitialized = new AtomicBoolean(false);

    private MetadataReconstructionMetrics() {
    }

    public static synchronized void initialize(List<String> storageStrategies,
        MetadataReconstructionMetricsCache reconstructionMetricsCache) {

        if (isInitialized.get()) {
            return;
        }

        GaugeUtils.createCustomGauge(
            VitamMetricsNames.VITAM_METADATA_RECONSTRUCTION_LATENCY_SECONDS,
            "Metadata document reconstruction latency (in seconds)",
            List.of(TENANT_LABEL, COLLECTION_LABEL, STRATEGY_LABEL),
            () -> collectDocumentReconstructionMetrics(storageStrategies, reconstructionMetricsCache)
        ).register();

        GaugeUtils.createCustomGauge(
            VitamMetricsNames.VITAM_METADATA_GRAPH_RECONSTRUCTION_LATENCY_SECONDS,
            "Metadata graph reconstruction latency (in seconds)",
            List.of(COLLECTION_LABEL),
            () -> collectGraphReconstructionMetrics(reconstructionMetricsCache)
        ).register();

        isInitialized.set(true);
    }

    private static Map<List<String>, Double> collectDocumentReconstructionMetrics(List<String> storageStrategies,
        MetadataReconstructionMetricsCache reconstructionMetricsCache) {

        Map<List<String>, Double> metricsByLabelValues = new HashMap<>();

        List<MetadataCollections> metadataCollections =
            List.of(MetadataCollections.UNIT, MetadataCollections.OBJECTGROUP);

        for (MetadataCollections metadataCollection : metadataCollections) {
            for (Integer tenant : VitamConfiguration.getTenants()) {
                for (String storageStrategy : storageStrategies) {

                    Duration durationSinceLastReconstruction = reconstructionMetricsCache.
                        getDocumentReconstructionLatency(metadataCollection, tenant, storageStrategy);

                    List<String> labelValues = List.of(
                        Integer.toString(tenant),
                        metadataCollection.getName().toLowerCase(),
                        storageStrategy
                    );

                    metricsByLabelValues.put(labelValues, getReconstructionLatency(durationSinceLastReconstruction));
                }
            }

        }
        return metricsByLabelValues;
    }

    private static Map<List<String>, Double> collectGraphReconstructionMetrics(
        MetadataReconstructionMetricsCache reconstructionMetricsCache) {

        Map<List<String>, Double> metricsByLabelValues = new HashMap<>();

        List<MetadataCollections> metadataCollections =
            List.of(MetadataCollections.UNIT, MetadataCollections.OBJECTGROUP);

        for (MetadataCollections metadataCollection : metadataCollections) {

            Duration durationSinceLastReconstruction = reconstructionMetricsCache.
                getGraphReconstructionLatency(metadataCollection);

            List<String> labelValues = List.of(metadataCollection.getName().toLowerCase());

            metricsByLabelValues.put(labelValues, getReconstructionLatency(durationSinceLastReconstruction));
        }
        return metricsByLabelValues;
    }

    private static double getReconstructionLatency(Duration durationSinceLastReconstruction) {

        // Returns :
        //   - Actual latency (in seconds) when available (eg. 100 seconds)
        //   - +∞ (positive infinity) when no latency information is available for current metadata instance (reconstruction is KO, server just restarted, reconstruction happened on other instances...)
        //
        // Aggregated latency can be computed using a simple "min"/"max" PromQL operators.
        // PromQL queries would be :
        //   - `min by (tenant, strategy) (vitam_metadata_reconstruction_metadata_latency_seconds)`: Tenant/strategy aggregated document reconstruction latency (across metadata instances)
        //   - `max (min by (tenant, strategy) (vitam_metadata_reconstruction_metadata_latency_seconds))`: Global aggregated document reconstruction latency
        //   - `min by (vitam_metadata_reconstruction_graph_latency_seconds)`: Graph reconstruction latency (across metadata instances)

        return durationSinceLastReconstruction == null ?
            Double.POSITIVE_INFINITY :
            Math.max(0.0, durationSinceLastReconstruction.toSeconds());
    }
}
