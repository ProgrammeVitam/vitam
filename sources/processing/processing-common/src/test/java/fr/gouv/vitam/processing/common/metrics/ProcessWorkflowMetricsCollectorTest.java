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

package fr.gouv.vitam.processing.common.metrics;

import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import io.prometheus.client.Collector;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

public class ProcessWorkflowMetricsCollectorTest {

    @Test
    public void test_collect_metrics_from_empty_map() {
        ProcessWorkflowMetricsCollector processWorkflowMetricsCollector = ProcessWorkflowMetricsCollector.getInstance();
        processWorkflowMetricsCollector.initialize(new ConcurrentHashMap<>());
        assertThat(processWorkflowMetricsCollector.collect().iterator().hasNext()).isTrue();
        assertThat(processWorkflowMetricsCollector.collect().iterator().next().samples).hasSize(0);
    }

    @Test
    public void test_collect_metrics_from_not_map() {
        ConcurrentHashMap<Integer, Map<String, ProcessWorkflow>> workflowMap = new ConcurrentHashMap<>();

        HashMap<String, ProcessWorkflow> tenant_0_workflow = new HashMap<>();
        HashMap<String, ProcessWorkflow> tenant_1_workflow = new HashMap<>();
        workflowMap.put(0, tenant_0_workflow);
        workflowMap.put(1, tenant_1_workflow);

        tenant_0_workflow.put(
            "op_0_1",
            new ProcessWorkflow(LogbookTypeProcess.INGEST, StatusCode.WARNING, ProcessState.RUNNING)
        );
        tenant_0_workflow.put(
            "op_0_2",
            new ProcessWorkflow(LogbookTypeProcess.INGEST, StatusCode.FATAL, ProcessState.PAUSE)
        );
        tenant_0_workflow.put(
            "op_0_3",
            new ProcessWorkflow(LogbookTypeProcess.INGEST, StatusCode.OK, ProcessState.COMPLETED)
        );
        tenant_0_workflow.put(
            "op_0_4",
            new ProcessWorkflow(LogbookTypeProcess.AUDIT, StatusCode.WARNING, ProcessState.RUNNING)
        );
        tenant_0_workflow.put(
            "op_0_5",
            new ProcessWorkflow(LogbookTypeProcess.TRACEABILITY, StatusCode.FATAL, ProcessState.PAUSE)
        );
        tenant_0_workflow.put(
            "op_0_6",
            new ProcessWorkflow(LogbookTypeProcess.ELIMINATION, StatusCode.OK, ProcessState.COMPLETED)
        );

        tenant_1_workflow.put(
            "op_1_1",
            new ProcessWorkflow(LogbookTypeProcess.INGEST, StatusCode.OK, ProcessState.RUNNING)
        );
        tenant_1_workflow.put(
            "op_1_2",
            new ProcessWorkflow(LogbookTypeProcess.CHECK, StatusCode.FATAL, ProcessState.PAUSE)
        );
        tenant_1_workflow.put(
            "op_1_3",
            new ProcessWorkflow(LogbookTypeProcess.INGEST, StatusCode.OK, ProcessState.COMPLETED)
        );
        tenant_1_workflow.put(
            "op_1_4",
            new ProcessWorkflow(LogbookTypeProcess.AUDIT, StatusCode.WARNING, ProcessState.RUNNING)
        );
        tenant_1_workflow.put(
            "op_1_5",
            new ProcessWorkflow(LogbookTypeProcess.TRACEABILITY, StatusCode.FATAL, ProcessState.PAUSE)
        );
        tenant_1_workflow.put(
            "op_1_6",
            new ProcessWorkflow(LogbookTypeProcess.ELIMINATION, StatusCode.OK, ProcessState.COMPLETED)
        );

        ProcessWorkflowMetricsCollector processWorkflowMetricsCollector = ProcessWorkflowMetricsCollector.getInstance();
        processWorkflowMetricsCollector.initialize(workflowMap);

        assertThat(processWorkflowMetricsCollector.collect()).hasSize(1);
        assertThat(processWorkflowMetricsCollector.collect().iterator().hasNext()).isTrue();
        List<Collector.MetricFamilySamples.Sample> samples = processWorkflowMetricsCollector
            .collect()
            .iterator()
            .next()
            .samples;
        assertThat(samples).hasSize(8);

        assertThat(samples.get(0).labelValues).contains("INGEST", "PAUSE", "FATAL");
        assertThat(samples.get(0).value).isEqualTo(1l);

        assertThat(samples.get(1).labelValues).contains("INGEST", "RUNNING", "OK");
        assertThat(samples.get(1).value).isEqualTo(1l);

        assertThat(samples.get(2).labelValues).contains("INGEST", "RUNNING", "WARNING");
        assertThat(samples.get(2).value).isEqualTo(1l);

        assertThat(samples.get(3).labelValues).contains("CHECK", "PAUSE", "FATAL");
        assertThat(samples.get(3).value).isEqualTo(1l);

        assertThat(samples.get(4).labelValues).contains("INGEST", "COMPLETED", "OK");
        assertThat(samples.get(4).value).isEqualTo(2l);

        assertThat(samples.get(5).labelValues).contains("AUDIT", "RUNNING", "WARNING");
        assertThat(samples.get(5).value).isEqualTo(2l);

        assertThat(samples.get(6).labelValues).contains("TRACEABILITY", "PAUSE", "FATAL");
        assertThat(samples.get(6).value).isEqualTo(2l);

        assertThat(samples.get(7).labelValues).contains("ELIMINATION", "COMPLETED", "OK");
        assertThat(samples.get(7).value).isEqualTo(2l);
    }
}
