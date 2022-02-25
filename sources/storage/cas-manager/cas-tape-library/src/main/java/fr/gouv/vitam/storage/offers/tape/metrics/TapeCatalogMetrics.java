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
package fr.gouv.vitam.storage.offers.tape.metrics;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.metrics.VitamMetricsNames;
import fr.gouv.vitam.storage.engine.common.model.TapeState;
import fr.gouv.vitam.storage.offers.tape.exception.TapeCatalogException;
import fr.gouv.vitam.storage.offers.tape.impl.catalog.TapeCatalogRepository;
import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;

public class TapeCatalogMetrics {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TapeCatalogMetrics.class);

    private TapeCatalogMetrics() {
        // Empty private constructor
    }

    public static void initializeMetrics(TapeCatalogRepository tapeCatalogRepository) {

        Collector collector = new Collector() {
            @Override
            public List<MetricFamilySamples> collect() {

                try {

                    Map<TapeState, Integer> stats = tapeCatalogRepository.countByState();

                    List<MetricFamilySamples> mfs = new ArrayList<>();

                    mfs.add(new GaugeMetricFamily(VitamMetricsNames.VITAM_TAPE_OFFER_EMPTY_STATE_TAPE,
                        "Total number of tapes with EMPTY state",
                        stats.getOrDefault(TapeState.EMPTY, 0)));

                    mfs.add(new GaugeMetricFamily(VitamMetricsNames.VITAM_TAPE_OFFER_OPEN_STATE_TAPE,
                        "Total number of tapes with OPEN state",
                        stats.getOrDefault(TapeState.OPEN, 0)));

                    mfs.add(new GaugeMetricFamily(VitamMetricsNames.VITAM_TAPE_OFFER_FULL_STATE_TAPE,
                        "Total number of tapes with FULL state",
                        stats.getOrDefault(TapeState.FULL, 0)));

                    mfs.add(new GaugeMetricFamily(VitamMetricsNames.VITAM_TAPE_OFFER_CONFLICT_STATE_TAPE,
                        "Total number of tapes with CONFLICT state",
                        stats.getOrDefault(TapeState.CONFLICT, 0)));

                    return mfs;

                } catch (TapeCatalogException e) {
                    LOGGER.error("Could not get tape catalog stats", e);
                    return emptyList();
                }
            }
        };

        collector.register();
    }
}
