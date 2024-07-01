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
package fr.gouv.vitam.storage.offers.tape.metrics;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.metrics.VitamMetricsNames;
import fr.gouv.vitam.storage.engine.common.model.QueueMessageType;
import fr.gouv.vitam.storage.engine.common.model.QueueState;
import fr.gouv.vitam.storage.offers.tape.exception.QueueException;
import fr.gouv.vitam.storage.offers.tape.impl.queue.QueueRepositoryImpl;
import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;

public class OrderQueueMetrics {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(OrderQueueMetrics.class);

    private OrderQueueMetrics() {
        // Empty private constructor
    }

    public static void initializeMetrics(QueueRepositoryImpl queueRepository) {
        Collector collector = new Collector() {
            @Override
            public List<MetricFamilySamples> collect() {
                try {
                    Map<Pair<QueueState, QueueMessageType>, Integer> stats = queueRepository.countByStateAndType();

                    List<MetricFamilySamples> mfs = new ArrayList<>();

                    addMetrics(
                        mfs,
                        stats,
                        VitamMetricsNames.VITAM_TAPE_OFFER_COUNT_READY_READ_ORDERS,
                        "Number of read orders with READY state",
                        QueueState.READY,
                        List.of(QueueMessageType.ReadOrder)
                    );

                    addMetrics(
                        mfs,
                        stats,
                        VitamMetricsNames.VITAM_TAPE_OFFER_COUNT_RUNNING_READ_ORDERS,
                        "Number of read orders with RUNNING state",
                        QueueState.RUNNING,
                        List.of(QueueMessageType.ReadOrder)
                    );

                    addMetrics(
                        mfs,
                        stats,
                        VitamMetricsNames.VITAM_TAPE_OFFER_COUNT_ERROR_READ_ORDERS,
                        "Number of read orders with ERROR state",
                        QueueState.ERROR,
                        List.of(QueueMessageType.ReadOrder)
                    );

                    addMetrics(
                        mfs,
                        stats,
                        VitamMetricsNames.VITAM_TAPE_OFFER_COUNT_READY_WRITE_ORDERS,
                        "Number of write orders with READY state",
                        QueueState.READY,
                        List.of(QueueMessageType.WriteOrder, QueueMessageType.WriteBackupOrder)
                    );

                    addMetrics(
                        mfs,
                        stats,
                        VitamMetricsNames.VITAM_TAPE_OFFER_COUNT_RUNNING_WRITE_ORDERS,
                        "Number of write orders with RUNNING state",
                        QueueState.RUNNING,
                        List.of(QueueMessageType.WriteOrder, QueueMessageType.WriteBackupOrder)
                    );

                    addMetrics(
                        mfs,
                        stats,
                        VitamMetricsNames.VITAM_TAPE_OFFER_COUNT_ERROR_WRITE_ORDERS,
                        "Number of write orders with ERROR state",
                        QueueState.ERROR,
                        List.of(QueueMessageType.WriteOrder, QueueMessageType.WriteBackupOrder)
                    );

                    return mfs;
                } catch (QueueException e) {
                    LOGGER.error("Could not get order queue stats", e);
                    return emptyList();
                }
            }
        };

        collector.register();
    }

    private static void addMetrics(
        List<Collector.MetricFamilySamples> mfs,
        Map<Pair<QueueState, QueueMessageType>, Integer> stats,
        String metricName,
        String help,
        QueueState queueState,
        List<QueueMessageType> queueMessageTypes
    ) {
        GaugeMetricFamily gaugeMetricFamily = new GaugeMetricFamily(metricName, help, emptyList());
        int total = 0;
        for (QueueMessageType queueMessageType : queueMessageTypes) {
            total += stats.getOrDefault(new ImmutablePair<>(queueState, queueMessageType), 0);
        }
        gaugeMetricFamily.addMetric(emptyList(), total);
        mfs.add(gaugeMetricFamily);
    }
}
