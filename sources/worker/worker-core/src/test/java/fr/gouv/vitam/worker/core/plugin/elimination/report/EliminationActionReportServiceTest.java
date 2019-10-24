package fr.gouv.vitam.worker.core.plugin.elimination.report;

import fr.gouv.vitam.batch.report.client.BatchReportClient;
import fr.gouv.vitam.batch.report.client.BatchReportClientFactory;
import fr.gouv.vitam.batch.report.model.ReportBody;
import fr.gouv.vitam.batch.report.model.ReportType;
import fr.gouv.vitam.batch.report.model.entry.EliminationActionUnitReportEntry;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.worker.core.plugin.elimination.model.EliminationActionUnitStatus;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

public class EliminationActionReportServiceTest {


    private static final String PROC_ID = "procId";
    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private BatchReportClientFactory batchReportClientFactory;

    @Mock
    private BatchReportClient batchReportClient;

    @Mock
    private WorkspaceClientFactory workspaceClientFactory;

    @Mock
    private WorkspaceClient workspaceClient;


    @InjectMocks
    private EliminationActionReportService instance;

    @Before
    public void setUp() throws Exception {

        VitamThreadUtils.getVitamSession().setTenantId(0);
        VitamThreadUtils.getVitamSession().setRequestId("opId");

        doReturn(batchReportClient).when(batchReportClientFactory).getClient();
        doReturn(workspaceClient).when(workspaceClientFactory).getClient();
    }

    @Test
    @RunWithCustomExecutor
    public void appendUnitEntries() throws Exception {

        // Given
        List<EliminationActionUnitReportEntry> entries = Arrays.asList(
            new EliminationActionUnitReportEntry("unit1", "sp1", "opi1", "got1",
                EliminationActionUnitStatus.GLOBAL_STATUS_CONFLICT.name()),
            new EliminationActionUnitReportEntry("unit2", "sp2", "opi2", "got2",
                EliminationActionUnitStatus.GLOBAL_STATUS_KEEP.name())
        );

        // When
        instance.appendUnitEntries(PROC_ID, entries);

        // Then
        ArgumentCaptor<ReportBody> reportBodyArgumentCaptor = ArgumentCaptor.forClass(ReportBody.class);
        verify(batchReportClient).appendReportEntries(reportBodyArgumentCaptor.capture());

        ReportBody<EliminationActionUnitReportEntry> reportBody = reportBodyArgumentCaptor.getValue();
        assertThat(reportBody.getReportType()).isEqualTo(ReportType.ELIMINATION_ACTION_UNIT);
        assertThat(reportBody.getProcessId()).isEqualTo(PROC_ID);
        assertThat(reportBody.getEntries()).hasSize(2);
        EliminationActionUnitReportEntry unitEntry = reportBody.getEntries().get(0);
        assertThat(unitEntry.getId()).isEqualTo("unit1");
        assertThat(unitEntry.getInitialOperation()).isEqualTo("opi1");
        assertThat(unitEntry.getOriginatingAgency()).isEqualTo("sp1");
        assertThat(unitEntry.getObjectGroupId()).isEqualTo("got1");
        assertThat(unitEntry.getStatus()).isEqualTo(EliminationActionUnitStatus.GLOBAL_STATUS_CONFLICT.name());
    }

    @Test
    @RunWithCustomExecutor
    public void cleanupReport() throws Exception {

        // Given / When
        instance.cleanupReport(PROC_ID);

        // Then
        verify(batchReportClient).cleanupReport(PROC_ID, ReportType.ELIMINATION_ACTION_UNIT);
    }
}
