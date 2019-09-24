package fr.gouv.vitam.worker.core.plugin.computeinheritedrules;

import fr.gouv.vitam.batch.report.client.BatchReportClient;
import fr.gouv.vitam.batch.report.client.BatchReportClientFactory;
import fr.gouv.vitam.batch.report.model.ReportType;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import org.junit.Test;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ComputeInheritedRulesFinalizationPluginTest {

    @Test
    public void testCleanup() throws Exception {

        // Given
        BatchReportClientFactory batchReportClientFactory = mock(BatchReportClientFactory.class);
        BatchReportClient batchReportClient = mock(BatchReportClient.class);
        doReturn(batchReportClient).when(batchReportClientFactory).getClient();
        ComputeInheritedRulesFinalizationPlugin instance =
            new ComputeInheritedRulesFinalizationPlugin(batchReportClientFactory);

        WorkerParameters workerParameters = mock(WorkerParameters.class);
        HandlerIO handler = mock(HandlerIO.class);
        doReturn("container").when(handler).getContainerName();

        // When
        instance.execute(workerParameters, handler);

        // Then
        verify(batchReportClient).cleanupReport("container", ReportType.UNIT_COMPUTED_INHERITED_RULES_INVALIDATION);
    }
}
