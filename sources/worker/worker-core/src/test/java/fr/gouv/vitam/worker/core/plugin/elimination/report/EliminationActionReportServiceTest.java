package fr.gouv.vitam.worker.core.plugin.elimination.report;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.batch.report.client.BatchReportClient;
import fr.gouv.vitam.batch.report.client.BatchReportClientFactory;
import fr.gouv.vitam.batch.report.model.ReportBody;
import fr.gouv.vitam.batch.report.model.ReportExportRequest;
import fr.gouv.vitam.batch.report.model.ReportType;
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
import static fr.gouv.vitam.worker.core.plugin.elimination.report.EliminationActionReportService.UNIT_REPORT_JSONL;
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
            new EliminationActionUnitReportEntry("unit1", "sp1", "opi1", "got1", EliminationActionUnitStatus.DELETED),
            new EliminationActionUnitReportEntry("unit2", "sp2", "opi2", "got2",
                EliminationActionUnitStatus.GLOBAL_STATUS_KEEP)
        );

        // When
        instance.appendUnitEntries(PROC_ID, entries);

        // Then
        ArgumentCaptor<ReportBody> reportBodyArgumentCaptor = ArgumentCaptor.forClass(ReportBody.class);
        verify(batchReportClient).appendReportEntries(reportBodyArgumentCaptor.capture());

        ReportBody<JsonNode> reportBody = reportBodyArgumentCaptor.getValue();
        assertThat(reportBody.getReportType()).isEqualTo(ReportType.ELIMINATION_ACTION_UNIT);
        assertThat(reportBody.getProcessId()).isEqualTo(PROC_ID);
        assertThat(reportBody.getEntries()).hasSize(2);
        assertThat(reportBody.getEntries().get(0).get("id").asText()).isEqualTo("unit1");
        assertThat(reportBody.getEntries().get(0).get("opi").asText()).isEqualTo("opi1");
        assertThat(reportBody.getEntries().get(0).get("originatingAgency").asText()).isEqualTo("sp1");
        assertThat(reportBody.getEntries().get(0).get("objectGroupId").asText()).isEqualTo("got1");
        assertThat(reportBody.getEntries().get(0).get("status").asText())
            .isEqualTo(EliminationActionUnitStatus.DELETED.name());
    }

    @Test
    @RunWithCustomExecutor
    public void appendEntries() throws Exception {


        // Given
        List<EliminationActionObjectGroupReportEntry> entries = Arrays.asList(
            new EliminationActionObjectGroupReportEntry("got1", "sp1", "opi1",
                null, new HashSet<>(Arrays.asList("o1", "o2")), EliminationActionObjectGroupStatus.DELETED,
                Arrays.asList(
                    new EliminationActionObjectGroupObjectVersion("opi_o_1", 10L),
                    new EliminationActionObjectGroupObjectVersion("opi_o_2", 100L))),
            new EliminationActionObjectGroupReportEntry("got2", "sp2", "opi2",
                new HashSet<>(Arrays.asList("unit3")), null, EliminationActionObjectGroupStatus.PARTIAL_DETACHMENT,
                null)
        );

        // When
        instance.appendObjectGroupEntries(PROC_ID, entries);

        // Then
        ArgumentCaptor<ReportBody<JsonNode>> reportBodyArgumentCaptor = ArgumentCaptor.forClass(ReportBody.class);
        verify(batchReportClient).appendReportEntries(reportBodyArgumentCaptor.capture());

        ReportBody<JsonNode> reportBody = reportBodyArgumentCaptor.getValue();
        assertThat(reportBody.getReportType()).isEqualTo(ReportType.ELIMINATION_ACTION_OBJECTGROUP);
        assertThat(reportBody.getProcessId()).isEqualTo(PROC_ID);
        assertThat(reportBody.getEntries()).hasSize(2);
        assertThat(reportBody.getEntries().get(0).get("id").asText()).isEqualTo("got1");
        assertThat(reportBody.getEntries().get(0).get("opi").asText()).isEqualTo("opi1");
        assertThat(reportBody.getEntries().get(0).get("originatingAgency").asText()).isEqualTo("sp1");
        assertThat(reportBody.getEntries().get(0).get("objectIds")).hasSize(2);
        assertThat(reportBody.getEntries().get(0).get("objectIds").get(0).asText()).isEqualTo("o1");
        assertThat(reportBody.getEntries().get(0).get("objectIds").get(1).asText()).isEqualTo("o2");
        assertThat(reportBody.getEntries().get(0).get("deletedParentUnitIds")).isNull();
        assertThat(reportBody.getEntries().get(0).get("status").asText())
            .isEqualTo(EliminationActionObjectGroupStatus.DELETED.name());


        assertThat(reportBody.getEntries().get(1).get("id").asText()).isEqualTo("got2");
        assertThat(reportBody.getEntries().get(1).get("opi").asText()).isEqualTo("opi2");
        assertThat(reportBody.getEntries().get(1).get("originatingAgency").asText()).isEqualTo("sp2");
        assertThat(reportBody.getEntries().get(1).get("objectIds")).isNull();
        assertThat(reportBody.getEntries().get(1).get("deletedParentUnitIds")).hasSize(1);
        assertThat(reportBody.getEntries().get(1).get("deletedParentUnitIds").get(0).asText()).isEqualTo("unit3");
        assertThat(reportBody.getEntries().get(1).get("status").asText())
            .isEqualTo(EliminationActionObjectGroupStatus.PARTIAL_DETACHMENT.name());
    }

    @Test
    @RunWithCustomExecutor
    public void exportUnits() throws Exception {

        // Given
        InputStream is =
            PropertiesUtils
                .getResourceAsStream("EliminationAction/EliminationActionUnitReportService/unitReport.jsonl");
        Response response = mock(Response.class);
        doReturn(is).when(response).readEntity(InputStream.class);
        doReturn(response).when(workspaceClient).getObject(PROC_ID, UNIT_REPORT_JSONL);

        // When
        CloseableIterator<EliminationActionUnitReportEntry> entries =
            instance.exportUnits(PROC_ID);

        // Then
        ArgumentCaptor<ReportExportRequest> reportExportRequestArgumentCaptor =
            ArgumentCaptor.forClass(ReportExportRequest.class);
        verify(batchReportClient)
            .generateEliminationActionUnitReport(eq(PROC_ID), reportExportRequestArgumentCaptor.capture());
        assertThat(reportExportRequestArgumentCaptor.getValue().getFilename()).isEqualTo(
            UNIT_REPORT_JSONL);

        List<EliminationActionUnitReportEntry> entryList = IteratorUtils.toList(entries);
        assertThat(entryList).hasSize(2);
        assertThat(entryList.get(0).getUnitId()).isEqualTo("unit1");
        assertThat(entryList.get(0).getInitialOperation()).isEqualTo("opi1");
        assertThat(entryList.get(0).getOriginatingAgency()).isEqualTo("sp1");
        assertThat(entryList.get(0).getObjectGroupId()).isEqualTo("got1");
        assertThat(entryList.get(0).getStatus()).isEqualTo(EliminationActionUnitStatus.DELETED);
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
    public void exportObjectGroups() throws Exception {

        // Given
        InputStream is = PropertiesUtils.getResourceAsStream(
            "EliminationAction/EliminationActionObjectGroupReportService/objectGroupReport.jsonl");
        Response response = mock(Response.class);
        doReturn(is).when(response).readEntity(InputStream.class);
        doReturn(response).when(workspaceClient).getObject(PROC_ID, OBJECT_GROUP_REPORT_JSONL);

        // When
        CloseableIterator<EliminationActionObjectGroupReportExportEntry> entries =
            instance.exportObjectGroups(PROC_ID);

        // Then
        ArgumentCaptor<ReportExportRequest> reportExportRequestArgumentCaptor =
            ArgumentCaptor.forClass(ReportExportRequest.class);
        verify(batchReportClient)
            .generateEliminationActionObjectGroupReport(eq(PROC_ID), reportExportRequestArgumentCaptor.capture());
        assertThat(reportExportRequestArgumentCaptor.getValue().getFilename())
            .isEqualTo(OBJECT_GROUP_REPORT_JSONL);

        List<EliminationActionObjectGroupReportExportEntry> entryList = IteratorUtils.toList(entries);
        assertThat(entryList).hasSize(2);
        assertThat(entryList.get(0).getObjectGroupId()).isEqualTo("got1");
        assertThat(entryList.get(0).getInitialOperation()).isEqualTo("opi1");
        assertThat(entryList.get(0).getOriginatingAgency()).isEqualTo("sp1");
        assertThat(entryList.get(0).getObjectIds()).containsExactlyInAnyOrder("o1", "o2");
        assertThat(entryList.get(0).getDeletedParentUnitIds()).isNull();
        assertThat(entryList.get(0).getStatus()).isEqualTo(EliminationActionObjectGroupStatus.DELETED);


        assertThat(entryList.get(1).getObjectGroupId()).isEqualTo("got2");
        assertThat(entryList.get(1).getObjectIds()).isNull();
        assertThat(entryList.get(1).getDeletedParentUnitIds()).containsExactlyInAnyOrder("unit3");
        assertThat(entryList.get(1).getStatus()).isEqualTo(EliminationActionObjectGroupStatus.PARTIAL_DETACHMENT);
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
