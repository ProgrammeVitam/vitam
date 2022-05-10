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

package fr.gouv.vitam.storage.engine.common.metrics;

import fr.gouv.vitam.common.metrics.VitamMetricsNames;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import org.apache.commons.io.input.NullInputStream;
import org.junit.Test;

import java.util.Iterator;

import static org.assertj.core.api.Assertions.assertThat;

public class UploadCountingInputStreamMetricsTest {

    private static final String STRATEGY_1 = "strategy_1";
    private static final String OFFER_1 = "offer_1";
    private static final String ORIGIN = "origin";

    @Test
    public void test_vitam_storage_upload_size_bytes() {

        // Given
        UploadCountingInputStreamMetrics uploadCountingInputStreamMetrics =
            new UploadCountingInputStreamMetrics(1, STRATEGY_1, OFFER_1, ORIGIN,
                DataCategory.UNIT, 1, new NullInputStream(3));

        // When
        StreamUtils.closeSilently(uploadCountingInputStreamMetrics);

        // Then
        Iterator<Collector.MetricFamilySamples> it =
            CollectorRegistry.defaultRegistry.metricFamilySamples().asIterator();

        boolean foundRequestMetric = false;
        while (it.hasNext()) {
            Collector.MetricFamilySamples next = it.next();
            if (next.name.equals(VitamMetricsNames.VITAM_STORAGE_UPLOAD_SIZE_BYTES)) {
                foundRequestMetric = true;
                assertThat(next.samples.stream().anyMatch(
                    o -> (o.name.equals(VitamMetricsNames.VITAM_STORAGE_UPLOAD_SIZE_BYTES + "_count") &&
                        o.labelValues.get(0).equals("1") &&
                        o.labelValues.get(1).equals("strategy_1") &&
                        o.labelValues.get(2).equals("offer_1") &&
                        o.labelValues.get(3).equals("origin") &&
                        o.labelValues.get(4).equals("UNIT") &&
                        o.labelValues.get(5).equals("1") &&
                        o.value == 1
                    ))).isTrue();

                assertThat(next.samples.stream().anyMatch(
                    o -> (o.name.equals(VitamMetricsNames.VITAM_STORAGE_UPLOAD_SIZE_BYTES + "_sum") &&
                        o.labelValues.get(0).equals("1") &&
                        o.labelValues.get(1).equals("strategy_1") &&
                        o.labelValues.get(2).equals("offer_1") &&
                        o.labelValues.get(3).equals("origin") &&
                        o.labelValues.get(4).equals("UNIT") &&
                        o.labelValues.get(5).equals("1") &&
                        o.value == 3))).isTrue();
            }
        }
        assertThat(foundRequestMetric).isTrue();
    }
}