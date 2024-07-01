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
package fr.gouv.vitam.worker.core.plugin.transfer.reply;

import fr.gouv.vitam.batch.report.client.BatchReportClient;
import fr.gouv.vitam.batch.report.client.BatchReportClientFactory;
import fr.gouv.vitam.batch.report.model.Report;
import fr.gouv.vitam.batch.report.model.ReportBody;
import fr.gouv.vitam.batch.report.model.ReportType;
import fr.gouv.vitam.batch.report.model.entry.TransferReplyUnitReportEntry;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
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

import static fr.gouv.vitam.worker.core.plugin.transfer.reply.TransferReplyReportService.JSONL_EXTENSION;
import static fr.gouv.vitam.worker.core.plugin.transfer.reply.TransferReplyReportService.WORKSPACE_REPORT_URI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class TransferReplyReportServiceTest {

    private static final String PROC_ID = "procId";

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

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

    @Mock
    private StorageClientFactory storageClientFactory;

    @Mock
    private StorageClient storageClient;

    @InjectMocks
    private TransferReplyReportService instance;

    @Before
    public void setUp() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        VitamThreadUtils.getVitamSession().setRequestId("opId");

        doReturn(batchReportClient).when(batchReportClientFactory).getClient();
        doReturn(workspaceClient).when(workspaceClientFactory).getClient();
        doReturn(storageClient).when(storageClientFactory).getClient();
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
        instance.appendEntries(PROC_ID, entries);

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
    public void isReportWrittenInWorkspace() throws Exception {
        // Given / When
        instance.isReportWrittenInWorkspace(PROC_ID);

        // Then
        verify(workspaceClient).isExistingObject(PROC_ID, WORKSPACE_REPORT_URI);
    }

    @Test
    @RunWithCustomExecutor
    public void storeReportToWorkspace() throws Exception {
        // Given / When
        Report report = mock(Report.class);
        instance.storeReportToWorkspace(report);

        // Then
        verify(batchReportClient).storeReportToWorkspace(report);
    }

    @Test
    @RunWithCustomExecutor
    public void storeReportToOffers() throws Exception {
        // Given / When
        instance.storeReportToOffers(PROC_ID);

        // Then
        ArgumentCaptor<ObjectDescription> descriptionArgumentCaptor = ArgumentCaptor.forClass(ObjectDescription.class);
        verify(storageClient).storeFileFromWorkspace(
            eq(VitamConfiguration.getDefaultStrategy()),
            eq(DataCategory.REPORT),
            eq(PROC_ID + JSONL_EXTENSION),
            descriptionArgumentCaptor.capture()
        );
        assertThat(descriptionArgumentCaptor.getValue().getWorkspaceContainerGUID()).isEqualTo(PROC_ID);
        assertThat(descriptionArgumentCaptor.getValue().getWorkspaceObjectURI()).isEqualTo(WORKSPACE_REPORT_URI);
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
