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
package fr.gouv.vitam.worker.core.plugin.purge;

import fr.gouv.vitam.batch.report.client.BatchReportClient;
import fr.gouv.vitam.batch.report.client.BatchReportClientFactory;
import fr.gouv.vitam.batch.report.model.ReportBody;
import fr.gouv.vitam.batch.report.model.ReportExportRequest;
import fr.gouv.vitam.batch.report.model.ReportType;
import fr.gouv.vitam.batch.report.model.entry.PurgeObjectGroupObjectVersion;
import fr.gouv.vitam.batch.report.model.entry.PurgeObjectGroupReportEntry;
import fr.gouv.vitam.batch.report.model.entry.PurgeUnitReportEntry;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.collection.CloseableIterator;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static fr.gouv.vitam.worker.core.plugin.purge.PurgeReportService.ACCESSION_REGISTER_REPORT_JSONL;
import static fr.gouv.vitam.worker.core.plugin.purge.PurgeReportService.DISTINCT_REPORT_JSONL;
import static fr.gouv.vitam.worker.core.plugin.purge.PurgeReportService.OBJECT_GROUP_REPORT_JSONL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class PurgeReportServiceTest {


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
    private PurgeReportService instance;

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
        List<PurgeUnitReportEntry> entries = Arrays.asList(
            new PurgeUnitReportEntry("unit1", "sp1", "opi1", "got1", PurgeUnitStatus.DELETED.name(), "INGEST"),
            new PurgeUnitReportEntry("unit2", "sp2", "opi2", "got2",
                PurgeUnitStatus.NON_DESTROYABLE_HAS_CHILD_UNITS.name(), "INGEST")
        );

        // When
        instance.appendUnitEntries(PROC_ID, entries);

        // Then
        ArgumentCaptor<ReportBody<PurgeUnitReportEntry>> reportBodyArgumentCaptor = ArgumentCaptor.forClass(ReportBody.class);
        verify(batchReportClient).appendReportEntries(reportBodyArgumentCaptor.capture());

        ReportBody<PurgeUnitReportEntry> reportBody = reportBodyArgumentCaptor.getValue();
        assertThat(reportBody.getReportType()).isEqualTo(ReportType.PURGE_UNIT);
        assertThat(reportBody.getProcessId()).isEqualTo(PROC_ID);
        assertThat(reportBody.getEntries()).hasSize(2);
        PurgeUnitReportEntry unitEntry = reportBody.getEntries().get(0);
        assertThat(unitEntry.getId()).isEqualTo("unit1");
        assertThat(unitEntry.getInitialOperation()).isEqualTo("opi1");
        assertThat(unitEntry.getOriginatingAgency()).isEqualTo("sp1");
        assertThat(unitEntry.getObjectGroupId()).isEqualTo("got1");
        assertThat(unitEntry.getStatus()).isEqualTo(PurgeUnitStatus.DELETED.name());
    }

    @Test
    @RunWithCustomExecutor
    public void appendEntries() throws Exception {
        // Given
        List<PurgeObjectGroupReportEntry> entries = Arrays.asList(
            new PurgeObjectGroupReportEntry("got1", "sp1", "opi1",
                null, new HashSet<>(Arrays.asList("o1", "o2")), PurgeObjectGroupStatus.DELETED.name(),
                Arrays.asList(
                    new PurgeObjectGroupObjectVersion("opi_o_1", 10L),
                    new PurgeObjectGroupObjectVersion("opi_o_2", 100L))),
            new PurgeObjectGroupReportEntry("got2", "sp2", "opi2",
                new HashSet<>(Collections.singletonList("unit3")), null, PurgeObjectGroupStatus.PARTIAL_DETACHMENT.name(),
                null)
        );

        // When
        instance.appendObjectGroupEntries(PROC_ID, entries);

        // Then
        ArgumentCaptor<ReportBody<PurgeObjectGroupReportEntry>> reportBodyArgumentCaptor = ArgumentCaptor.forClass(ReportBody.class);
        verify(batchReportClient).appendReportEntries(reportBodyArgumentCaptor.capture());

        ReportBody<PurgeObjectGroupReportEntry> reportBody = reportBodyArgumentCaptor.getValue();
        assertThat(reportBody.getReportType()).isEqualTo(ReportType.PURGE_OBJECTGROUP);
        assertThat(reportBody.getProcessId()).isEqualTo(PROC_ID);
        assertThat(reportBody.getEntries()).hasSize(2);

        PurgeObjectGroupReportEntry objectGroup = reportBody.getEntries().get(0);
        PurgeObjectGroupReportEntry objectGroup2 = reportBody.getEntries().get(1);

        assertThat(objectGroup.getId()).isEqualTo("got1");
        assertThat(objectGroup.getInitialOperation()).isEqualTo("opi1");
        assertThat(objectGroup.getOriginatingAgency()).isEqualTo("sp1");
        assertThat(objectGroup.getObjectIds()).containsExactly("o1", "o2");
        assertThat(objectGroup.getDeletedParentUnitIds()).isNull();
        assertThat(objectGroup.getStatus()).isEqualTo(PurgeObjectGroupStatus.DELETED.name());

        assertThat(objectGroup2.getId()).isEqualTo("got2");
        assertThat(objectGroup2.getInitialOperation()).isEqualTo("opi2");
        assertThat(objectGroup2.getOriginatingAgency()).isEqualTo("sp2");
        assertThat(objectGroup2.getObjectIds()).isNull();
        assertThat(objectGroup2.getDeletedParentUnitIds()).containsExactly("unit3");
        assertThat(objectGroup2.getStatus()).isEqualTo(PurgeObjectGroupStatus.PARTIAL_DETACHMENT.name());
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
        verify(batchReportClient).generatePurgeDistinctObjectGroupInUnitReport(eq(PROC_ID),
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
            .generatePurgeAccessionRegisterReport(eq(PROC_ID), reportExportRequestArgumentCaptor.capture());
        assertThat(reportExportRequestArgumentCaptor.getValue().getFilename())
            .isEqualTo(ACCESSION_REGISTER_REPORT_JSONL);
    }

    @Test
    @RunWithCustomExecutor
    public void cleanupReport() throws Exception {

        // Given / When
        instance.cleanupReport(PROC_ID);

        // Then
        verify(batchReportClient).cleanupReport(PROC_ID, ReportType.PURGE_OBJECTGROUP);
        verify(batchReportClient).cleanupReport(PROC_ID, ReportType.PURGE_UNIT);
    }
}
