package fr.gouv.vitam.worker.core.plugin.elimination.report;

import fr.gouv.vitam.batch.report.client.BatchReportClient;
import fr.gouv.vitam.batch.report.client.BatchReportClientFactory;
import fr.gouv.vitam.batch.report.model.ReportBody;
import fr.gouv.vitam.batch.report.model.ReportExportRequest;
import fr.gouv.vitam.batch.report.model.ReportType;
import fr.gouv.vitam.batch.report.model.entry.EliminationActionObjectGroupObjectVersion;
import fr.gouv.vitam.batch.report.model.entry.EliminationActionObjectGroupReportEntry;
import fr.gouv.vitam.batch.report.model.entry.EliminationActionUnitReportEntry;
import fr.gouv.vitam.batch.report.model.entry.ReportEntry;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.collection.CloseableIterator;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.worker.core.plugin.elimination.model.EliminationActionObjectGroupStatus;
import fr.gouv.vitam.worker.core.plugin.elimination.model.EliminationActionUnitStatus;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.apache.commons.collections4.IteratorUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static fr.gouv.vitam.worker.core.plugin.elimination.report.EliminationActionReportService.ACCESSION_REGISTER_REPORT_JSONL;
import static fr.gouv.vitam.worker.core.plugin.elimination.report.EliminationActionReportService.DISTINCT_REPORT_JSONL;
import static fr.gouv.vitam.worker.core.plugin.elimination.report.EliminationActionReportService.OBJECT_GROUP_REPORT_JSONL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
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
            new EliminationActionUnitReportEntry("unit1", "sp1", "opi1", "got1", EliminationActionUnitStatus.DELETED.name(), "Outcome - TEST"),
            new EliminationActionUnitReportEntry("unit2", "sp2", "opi2", "got2",
                EliminationActionUnitStatus.GLOBAL_STATUS_KEEP.name(), "Outcome - TEST")
        );

        // When
        instance.appendUnitEntries(PROC_ID, entries);

        // Then
        ArgumentCaptor<ReportBody> reportBodyArgumentCaptor = ArgumentCaptor.forClass(ReportBody.class);
        verify(batchReportClient).appendReportEntries(reportBodyArgumentCaptor.capture());

        ReportBody reportBody = reportBodyArgumentCaptor.getValue();
        assertThat(reportBody.getReportType()).isEqualTo(ReportType.ELIMINATION_ACTION_UNIT);
        assertThat(reportBody.getProcessId()).isEqualTo(PROC_ID);
        assertThat(reportBody.getEntries()).hasSize(2);
        ReportEntry entry = (ReportEntry) reportBody.getEntries().get(0);
        assertThat(entry).isOfAnyClassIn(EliminationActionUnitReportEntry.class);
        EliminationActionUnitReportEntry unitEntry = (EliminationActionUnitReportEntry)entry;
        assertThat(unitEntry.getUnitId()).isEqualTo("unit1");
        assertThat(unitEntry.getInitialOperation()).isEqualTo("opi1");
        assertThat(unitEntry.getOriginatingAgency()).isEqualTo("sp1");
        assertThat(unitEntry.getObjectGroupId()).isEqualTo("got1");
        assertThat(unitEntry.getStatus()).isEqualTo(EliminationActionUnitStatus.DELETED.name());
    }

    @Test
    @RunWithCustomExecutor
    public void appendEntries() throws Exception {
        // Given
        List<EliminationActionObjectGroupReportEntry> entries = Arrays.asList(
            new EliminationActionObjectGroupReportEntry("got1", "sp1", "opi1",
                null, new HashSet<>(Arrays.asList("o1", "o2")), EliminationActionObjectGroupStatus.DELETED.name(),
                Arrays.asList(
                    new EliminationActionObjectGroupObjectVersion("opi_o_1", 10L),
                    new EliminationActionObjectGroupObjectVersion("opi_o_2", 100L)), "Outcome - TEST"),
            new EliminationActionObjectGroupReportEntry("got2", "sp2", "opi2",
                new HashSet<>(Arrays.asList("unit3")), null, EliminationActionObjectGroupStatus.PARTIAL_DETACHMENT.name(),
                null, "Outcome - TEST")
        );

        // When
        instance.appendObjectGroupEntries(PROC_ID, entries);

        // Then
        ArgumentCaptor<ReportBody> reportBodyArgumentCaptor = ArgumentCaptor.forClass(ReportBody.class);
        verify(batchReportClient).appendReportEntries(reportBodyArgumentCaptor.capture());

        ReportBody reportBody = reportBodyArgumentCaptor.getValue();
        assertThat(reportBody.getReportType()).isEqualTo(ReportType.ELIMINATION_ACTION_OBJECTGROUP);
        assertThat(reportBody.getProcessId()).isEqualTo(PROC_ID);
        assertThat(reportBody.getEntries()).hasSize(2);

        ReportEntry entry = (ReportEntry) reportBody.getEntries().get(0);
        assertThat(entry).isOfAnyClassIn(EliminationActionObjectGroupReportEntry.class);
        EliminationActionObjectGroupReportEntry objectGroup = (EliminationActionObjectGroupReportEntry) entry;
        ReportEntry entry2 = (ReportEntry) reportBody.getEntries().get(1);
        assertThat(entry2).isOfAnyClassIn(EliminationActionObjectGroupReportEntry.class);
        EliminationActionObjectGroupReportEntry objectGroup2 = (EliminationActionObjectGroupReportEntry) entry2;

        assertThat(objectGroup.getObjectGroupId()).isEqualTo("got1");
        assertThat(objectGroup.getInitialOperation()).isEqualTo("opi1");
        assertThat(objectGroup.getOriginatingAgency()).isEqualTo("sp1");
        assertThat(objectGroup.getObjectIds()).containsExactly("o1", "o2");
        assertThat(objectGroup.getDeletedParentUnitIds()).isNull();
        assertThat(objectGroup.getStatus()).isEqualTo(EliminationActionObjectGroupStatus.DELETED.name());

        assertThat(objectGroup2.getObjectGroupId()).isEqualTo("got2");
        assertThat(objectGroup2.getInitialOperation()).isEqualTo("opi2");
        assertThat(objectGroup2.getOriginatingAgency()).isEqualTo("sp2");
        assertThat(objectGroup2.getObjectIds()).isNull();
        assertThat(objectGroup2.getDeletedParentUnitIds()).containsExactly("unit3");
        assertThat(objectGroup2.getStatus()).isEqualTo(EliminationActionObjectGroupStatus.PARTIAL_DETACHMENT.name());
    }

    @Test
    @RunWithCustomExecutor
    public void exportDistinctObjectGroups() throws Exception {

        // Given
        InputStream is =
            PropertiesUtils
                .getResourceAsStream("EliminationAction/EliminationActionUnitReportService/unitObjectGroups.jsonl");
        Response response = mock(Response.class);
        doReturn(is).when(response).readEntity(InputStream.class);
        doReturn(response).when(workspaceClient).getObject(PROC_ID, DISTINCT_REPORT_JSONL);

        // When
        CloseableIterator<String> entries =
            instance.exportDistinctObjectGroups(PROC_ID);

        // Then
        ArgumentCaptor<ReportExportRequest> reportExportRequestArgumentCaptor =
            ArgumentCaptor.forClass(ReportExportRequest.class);
        verify(batchReportClient).generateEliminationActionDistinctObjectGroupInUnitReport(eq(PROC_ID),
            reportExportRequestArgumentCaptor.capture());
        assertThat(reportExportRequestArgumentCaptor.getValue().getFilename())
            .isEqualTo(DISTINCT_REPORT_JSONL);

        assertThat(IteratorUtils.toList(entries)).containsExactly("got1", "got2");
    }

    @Test
    @RunWithCustomExecutor
    public void exportAccessionRegisters() throws Exception {

        // Given
        InputStream is = PropertiesUtils.getResourceAsStream(
            "EliminationAction/EliminationActionObjectGroupReportService/objectGroupReport.jsonl");
        Response response = mock(Response.class);
        doReturn(is).when(response).readEntity(InputStream.class);
        doReturn(response).when(workspaceClient).getObject(PROC_ID, OBJECT_GROUP_REPORT_JSONL);

        // When
        instance.exportAccessionRegisters(PROC_ID);

        // Then
        ArgumentCaptor<ReportExportRequest> reportExportRequestArgumentCaptor =
            ArgumentCaptor.forClass(ReportExportRequest.class);
        verify(batchReportClient)
            .generateEliminationActionAccessionRegisterReport(eq(PROC_ID), reportExportRequestArgumentCaptor.capture());
        assertThat(reportExportRequestArgumentCaptor.getValue().getFilename())
            .isEqualTo(ACCESSION_REGISTER_REPORT_JSONL);
    }

    @Test
    @RunWithCustomExecutor
    public void cleanupReport() throws Exception {

        // Given / When
        instance.cleanupReport(PROC_ID);

        // Then
        verify(batchReportClient).cleanupReport(PROC_ID, ReportType.ELIMINATION_ACTION_OBJECTGROUP);
        verify(batchReportClient).cleanupReport(PROC_ID, ReportType.ELIMINATION_ACTION_UNIT);
    }
}
