package fr.gouv.vitam.worker.core.plugin.transfer.reply;

import fr.gouv.vitam.batch.report.client.BatchReportClient;
import fr.gouv.vitam.batch.report.client.BatchReportClientFactory;
import fr.gouv.vitam.batch.report.model.ReportBody;
import fr.gouv.vitam.batch.report.model.ReportType;
import fr.gouv.vitam.batch.report.model.entry.TransferReplyUnitReportEntry;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
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

public class TransferReplyReportServiceTest {


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
    private TransferReplyReportService instance;

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
        List<TransferReplyUnitReportEntry> entries = Arrays.asList(
            new TransferReplyUnitReportEntry("unit1", TransferReplyUnitStatus.ALREADY_DELETED.name()),
            new TransferReplyUnitReportEntry("unit2", TransferReplyUnitStatus.ALREADY_DELETED.name())
        );

        // When
        instance.appendUnitEntries(PROC_ID, entries);

        // Then
        ArgumentCaptor<ReportBody> reportBodyArgumentCaptor = ArgumentCaptor.forClass(ReportBody.class);
        verify(batchReportClient).appendReportEntries(reportBodyArgumentCaptor.capture());

        ReportBody<TransferReplyUnitReportEntry> reportBody = reportBodyArgumentCaptor.getValue();
        assertThat(reportBody.getReportType()).isEqualTo(ReportType.TRANSFER_REPLY_UNIT);
        assertThat(reportBody.getProcessId()).isEqualTo(PROC_ID);
        assertThat(reportBody.getEntries()).hasSize(2);
        TransferReplyUnitReportEntry unitEntry = reportBody.getEntries().get(0);
        assertThat(unitEntry.getId()).isEqualTo("unit1");
        assertThat(unitEntry.getStatus()).isEqualTo(TransferReplyUnitStatus.ALREADY_DELETED.name());
    }

    @Test
    @RunWithCustomExecutor
    public void cleanupReport() throws Exception {

        // Given / When
        instance.cleanupReport(PROC_ID);

        // Then
        verify(batchReportClient).cleanupReport(PROC_ID, ReportType.TRANSFER_REPLY_UNIT);
    }
}
