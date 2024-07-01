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
package fr.gouv.vitam.worker.core.plugin.traceability;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.batch.report.client.BatchReportClient;
import fr.gouv.vitam.batch.report.client.BatchReportClientFactory;
import fr.gouv.vitam.batch.report.model.OperationSummary;
import fr.gouv.vitam.batch.report.model.Report;
import fr.gouv.vitam.batch.report.model.ReportBody;
import fr.gouv.vitam.batch.report.model.ReportResults;
import fr.gouv.vitam.batch.report.model.ReportSummary;
import fr.gouv.vitam.batch.report.model.ReportType;
import fr.gouv.vitam.batch.report.model.TraceabilityError;
import fr.gouv.vitam.batch.report.model.entry.TraceabilityReportEntry;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.worker.core.plugin.traceability.service.TraceabilityReportService;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.ArrayList;
import java.util.List;

import static fr.gouv.vitam.worker.core.plugin.audit.AuditReportService.JSONL_EXTENSION;
import static fr.gouv.vitam.worker.core.plugin.audit.AuditReportService.WORKSPACE_REPORT_URI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

public class TraceabilityReportServiceTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private BatchReportClientFactory batchReportFactory;

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
    private TraceabilityReportService traceabilityReportService;

    private static final String PROCESS_ID = "PROCESS_ID";
    private static final int TENANT_ID = 0;

    @Before
    public void setUp() {
        lenient().when((batchReportFactory.getClient())).thenReturn(batchReportClient);
        lenient().when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);
        lenient().when((storageClientFactory.getClient())).thenReturn(storageClient);
    }

    @Test
    public void should_append_audit_report_entries() throws Exception {
        // Given
        TraceabilityReportEntry auditReportEntry = new TraceabilityReportEntry(
            "OP_ID",
            "STORAGE",
            "KO",
            "Operation is KO",
            TraceabilityError.FILE_NOT_FOUND,
            null,
            null,
            null,
            null
        );

        List<TraceabilityReportEntry> reports = new ArrayList<>();
        reports.add(auditReportEntry);

        // When
        ThrowingCallable appendPreservation = () -> traceabilityReportService.appendEntries(PROCESS_ID, reports);

        // Then
        assertThatCode(appendPreservation).doesNotThrowAnyException();

        ArgumentCaptor<ReportBody<TraceabilityReportEntry>> reportBodyArgumentCaptor = ArgumentCaptor.forClass(
            ReportBody.class
        );
        verify(batchReportClient).appendReportEntries(reportBodyArgumentCaptor.capture());
        assertThat(reportBodyArgumentCaptor.getValue().getProcessId()).isEqualTo(PROCESS_ID);
        assertThat(reportBodyArgumentCaptor.getValue().getEntries()).isEqualTo(reports);
        assertThat(reportBodyArgumentCaptor.getValue().getReportType()).isEqualTo(ReportType.TRACEABILITY);
    }

    @Test
    public void should_check_report_existence_in_workspace_does_not_throw_any_exception() throws Exception {
        // Given / When
        ThrowingCallable checkReportExistence = () -> traceabilityReportService.isReportWrittenInWorkspace(PROCESS_ID);

        // Then
        assertThatCode(checkReportExistence).doesNotThrowAnyException();
        verify(workspaceClient).isExistingObject(PROCESS_ID, WORKSPACE_REPORT_URI);
    }

    @Test
    public void should_store_to_workspace_does_not_throw_any_exception() throws Exception {
        OperationSummary operationSummary = new OperationSummary(
            TENANT_ID,
            PROCESS_ID,
            "",
            "",
            "",
            "",
            JsonHandler.createObjectNode(),
            JsonHandler.createObjectNode()
        );
        ReportSummary reportSummary = new ReportSummary(
            null,
            null,
            ReportType.TRACEABILITY,
            new ReportResults(),
            JsonHandler.createObjectNode()
        );
        JsonNode context = JsonHandler.createObjectNode();

        Report reportInfo = new Report(operationSummary, reportSummary, context);

        // Given / When
        ThrowingCallable exportReport = () -> traceabilityReportService.storeReportToWorkspace(reportInfo);

        // Then
        assertThatCode(exportReport).doesNotThrowAnyException();
        verify(batchReportClient).storeReportToWorkspace(reportInfo);
    }

    @Test
    public void should_store_file_to_offers() throws Exception {
        // Given / When
        ThrowingCallable exportReport = () -> traceabilityReportService.storeReportToOffers(PROCESS_ID);

        // Then
        assertThatCode(exportReport).doesNotThrowAnyException();
        ArgumentCaptor<ObjectDescription> descriptionArgumentCaptor = ArgumentCaptor.forClass(ObjectDescription.class);
        verify(storageClient).storeFileFromWorkspace(
            eq(VitamConfiguration.getDefaultStrategy()),
            eq(DataCategory.REPORT),
            eq(PROCESS_ID + JSONL_EXTENSION),
            descriptionArgumentCaptor.capture()
        );
        assertThat(descriptionArgumentCaptor.getValue().getWorkspaceContainerGUID()).isEqualTo(PROCESS_ID);
        assertThat(descriptionArgumentCaptor.getValue().getWorkspaceObjectURI()).isEqualTo(WORKSPACE_REPORT_URI);
    }

    @Test
    public void should_delete_does_not_throw_any_exception() throws Exception {
        // Given / When
        ThrowingCallable exportReport = () -> traceabilityReportService.cleanupReport(PROCESS_ID);

        // Then
        assertThatCode(exportReport).doesNotThrowAnyException();
        verify(batchReportClient).cleanupReport(PROCESS_ID, ReportType.TRACEABILITY);
    }
}
