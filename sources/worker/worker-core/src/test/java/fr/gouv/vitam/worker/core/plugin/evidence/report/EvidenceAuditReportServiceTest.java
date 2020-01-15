package fr.gouv.vitam.worker.core.plugin.evidence.report;

import fr.gouv.vitam.batch.report.client.BatchReportClient;
import fr.gouv.vitam.batch.report.client.BatchReportClientFactory;
import fr.gouv.vitam.batch.report.model.Report;
import fr.gouv.vitam.batch.report.model.ReportType;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class EvidenceAuditReportServiceTest {

    public static final String JSONL_EXTENSION = ".jsonl";
    public static final String WORKSPACE_REPORT_URI = "report.jsonl";

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

    @Mock
    private StorageClientFactory storageClientFactory;
    @Mock
    private StorageClient storageClient;

    @InjectMocks
    private EvidenceAuditReportService instance;

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
        verify(storageClient)
            .storeFileFromWorkspace(eq(VitamConfiguration.getDefaultStrategy()), eq(DataCategory.REPORT),
                eq(PROC_ID + JSONL_EXTENSION), descriptionArgumentCaptor.capture());
        assertThat(descriptionArgumentCaptor.getValue().getWorkspaceContainerGUID()).isEqualTo(PROC_ID);
        assertThat(descriptionArgumentCaptor.getValue().getWorkspaceObjectURI()).isEqualTo(
            WORKSPACE_REPORT_URI);
    }

    @Test
    @RunWithCustomExecutor
    public void cleanupReport() throws Exception {

        // Given / When
        instance.cleanupReport(PROC_ID);

        // Then
        verify(batchReportClient).cleanupReport(PROC_ID, ReportType.EVIDENCE_AUDIT);
    }
}
