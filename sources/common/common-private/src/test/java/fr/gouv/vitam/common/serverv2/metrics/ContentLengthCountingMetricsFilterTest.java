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

package fr.gouv.vitam.common.serverv2.metrics;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.metrics.RequestLengthCountingInputStreamMetrics;
import fr.gouv.vitam.common.metrics.ResponseLengthCountingOutputStreamMetrics;
import fr.gouv.vitam.common.stream.StreamUtils;
import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import org.apache.commons.io.input.NullInputStream;
import org.apache.commons.io.output.NullOutputStream;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ContentLengthCountingMetricsFilterTest {

    @Test
    public void test_count_input_bytes_enabled_and_count_output_disabled() throws IOException {
        ContentLengthCountingMetricsFilter filter = new ContentLengthCountingMetricsFilter(true, false);
        ContainerRequestContext containerRequestContext = mock(ContainerRequestContext.class);
        when(containerRequestContext.getEntityStream()).thenReturn(new NullInputStream(1));
        when(containerRequestContext.getHeaderString(eq(GlobalDataRest.X_TENANT_ID))).thenReturn("0");
        when(containerRequestContext.getMethod()).thenReturn("PUT");

        ArgumentCaptor<InputStream> captor = ArgumentCaptor.forClass(InputStream.class);

        filter.filter(containerRequestContext);
        verify(containerRequestContext, Mockito.times(2)).getEntityStream();
        verify(containerRequestContext).setEntityStream(captor.capture());

        assertThat(captor.getValue()).isInstanceOf(RequestLengthCountingInputStreamMetrics.class);
        StreamUtils.closeSilently(captor.getValue());

        verify(containerRequestContext, times(1)).getMethod();

        ContainerResponseContext containerResponseContext = mock(ContainerResponseContext.class);
        filter.filter(containerRequestContext, containerResponseContext);
        verify(containerRequestContext, times(1)).getMethod();
        verify(containerRequestContext, Mockito.times(2)).getEntityStream();

        Iterator<Collector.MetricFamilySamples> it = CollectorRegistry.defaultRegistry
            .metricFamilySamples()
            .asIterator();

        boolean foundRequestMetric = false;
        while (it.hasNext()) {
            Collector.MetricFamilySamples next = it.next();
            if (next.name.equals("vitam_requests_size_bytes")) {
                foundRequestMetric = true;
                assertThat(
                    next.samples
                        .stream()
                        .anyMatch(
                            o ->
                                (o.name.equals("vitam_requests_size_bytes_count") &&
                                    o.labelValues.get(0).equals("0") &&
                                    o.labelValues.get(1).equals("PUT") &&
                                    o.value == 1)
                        )
                ).isTrue();

                assertThat(
                    next.samples
                        .stream()
                        .anyMatch(
                            o ->
                                (o.name.equals("vitam_requests_size_bytes_sum") &&
                                    o.labelValues.get(0).equals("0") &&
                                    o.labelValues.get(1).equals("PUT") &&
                                    o.value == 1)
                        )
                ).isTrue();
            }
        }
        assertThat(foundRequestMetric).isTrue();
    }

    @Test
    public void test_count_input_bytes_disabled_and_count_output_enabled() throws IOException {
        ContentLengthCountingMetricsFilter filter = new ContentLengthCountingMetricsFilter(false, true);

        ContainerRequestContext containerRequestContext = mock(ContainerRequestContext.class);
        when(containerRequestContext.getEntityStream()).thenReturn(null);
        when(containerRequestContext.getHeaderString(eq(GlobalDataRest.X_TENANT_ID))).thenReturn("1");
        when(containerRequestContext.getMethod()).thenReturn("GET");

        filter.filter(containerRequestContext);

        verify(containerRequestContext, never()).getEntityStream();
        verify(containerRequestContext, never()).getMethod();

        ContainerResponseContext containerResponseContext = mock(ContainerResponseContext.class);
        NullOutputStream nullOutputStream = new NullOutputStream();

        when(containerResponseContext.getEntityStream()).thenReturn(nullOutputStream);

        filter.filter(containerRequestContext, containerResponseContext);
        verify(containerResponseContext, Mockito.times(2)).getEntityStream();
        ArgumentCaptor<OutputStream> captor = ArgumentCaptor.forClass(OutputStream.class);
        verify(containerResponseContext).setEntityStream(captor.capture());

        assertThat(captor.getValue()).isInstanceOf(ResponseLengthCountingOutputStreamMetrics.class);

        captor.getValue().write("Tests".getBytes());
        captor.getValue().close();
        verify(containerRequestContext).getMethod();

        Iterator<Collector.MetricFamilySamples> it = CollectorRegistry.defaultRegistry
            .metricFamilySamples()
            .asIterator();
        boolean foundResponseMetric = false;
        while (it.hasNext()) {
            Collector.MetricFamilySamples next = it.next();
            if (next.name.equals("vitam_responses_size_bytes")) {
                foundResponseMetric = true;
                assertThat(
                    next.samples
                        .stream()
                        .anyMatch(
                            o ->
                                (o.name.equals("vitam_responses_size_bytes_count") &&
                                    o.labelValues.get(0).equals("1") &&
                                    o.labelValues.get(1).equals("GET") &&
                                    o.value == 1)
                        )
                ).isTrue();

                assertThat(
                    next.samples
                        .stream()
                        .anyMatch(
                            o ->
                                (o.name.equals("vitam_responses_size_bytes_sum") &&
                                    o.labelValues.get(0).equals("1") &&
                                    o.labelValues.get(1).equals("GET") &&
                                    o.value == 5)
                        )
                ).isTrue();
            }
        }
        assertThat(foundResponseMetric).isTrue();
    }

    @Test
    public void test_count_input_bytes_enabled_and_count_output_enabled() throws IOException {
        ContentLengthCountingMetricsFilter filter = new ContentLengthCountingMetricsFilter(true, true);
        ContainerRequestContext containerRequestContext = mock(ContainerRequestContext.class);
        when(containerRequestContext.getEntityStream()).thenReturn(new NullInputStream(3));
        when(containerRequestContext.getHeaderString(eq(GlobalDataRest.X_TENANT_ID))).thenReturn("2");
        when(containerRequestContext.getMethod()).thenReturn("POST");

        ArgumentCaptor<InputStream> captorInput = ArgumentCaptor.forClass(InputStream.class);

        filter.filter(containerRequestContext);
        verify(containerRequestContext, Mockito.times(2)).getEntityStream();
        verify(containerRequestContext).setEntityStream(captorInput.capture());

        assertThat(captorInput.getValue()).isInstanceOf(RequestLengthCountingInputStreamMetrics.class);
        StreamUtils.closeSilently(captorInput.getValue());

        verify(containerRequestContext, times(1)).getMethod();

        ContainerResponseContext containerResponseContext = mock(ContainerResponseContext.class);
        NullOutputStream nullOutputStream = new NullOutputStream();
        when(containerResponseContext.getEntityStream()).thenReturn(nullOutputStream);

        filter.filter(containerRequestContext, containerResponseContext);
        verify(containerResponseContext, Mockito.times(2)).getEntityStream();
        ArgumentCaptor<OutputStream> captorOutput = ArgumentCaptor.forClass(OutputStream.class);
        verify(containerResponseContext).setEntityStream(captorOutput.capture());

        captorOutput.getValue().write("Tests".getBytes());
        captorOutput.getValue().close();
        verify(containerRequestContext, times(2)).getMethod();

        Iterator<Collector.MetricFamilySamples> it = CollectorRegistry.defaultRegistry
            .metricFamilySamples()
            .asIterator();

        boolean foundRequestMetric = false;
        boolean foundResponseMetric = false;
        while (it.hasNext()) {
            Collector.MetricFamilySamples next = it.next();
            if (next.name.equals("vitam_responses_size_bytes")) {
                foundResponseMetric = true;
                assertThat(
                    next.samples
                        .stream()
                        .anyMatch(
                            o ->
                                (o.name.equals("vitam_responses_size_bytes_count") &&
                                    o.labelValues.get(0).equals("2") &&
                                    o.labelValues.get(1).equals("POST") &&
                                    o.value == 1)
                        )
                ).isTrue();

                assertThat(
                    next.samples
                        .stream()
                        .anyMatch(
                            o ->
                                (o.name.equals("vitam_responses_size_bytes_sum") &&
                                    o.labelValues.get(0).equals("2") &&
                                    o.labelValues.get(1).equals("POST") &&
                                    o.value == 5)
                        )
                ).isTrue();
            }

            if (next.name.equals("vitam_requests_size_bytes")) {
                foundRequestMetric = true;
                assertThat(
                    next.samples
                        .stream()
                        .anyMatch(
                            o ->
                                (o.name.equals("vitam_requests_size_bytes_count") &&
                                    o.labelValues.get(0).equals("2") &&
                                    o.labelValues.get(1).equals("POST") &&
                                    o.value == 1)
                        )
                ).isTrue();

                assertThat(
                    next.samples
                        .stream()
                        .anyMatch(
                            o ->
                                (o.name.equals("vitam_requests_size_bytes_sum") &&
                                    o.labelValues.get(0).equals("2") &&
                                    o.labelValues.get(1).equals("POST") &&
                                    o.value == 3)
                        )
                ).isTrue();
            }
        }
        assertThat(foundRequestMetric).isTrue();
        assertThat(foundResponseMetric).isTrue();
    }
}
