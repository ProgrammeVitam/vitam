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
package fr.gouv.vitam.worker.core.plugin.preservation;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.batch.report.client.BatchReportClient;
import fr.gouv.vitam.batch.report.client.BatchReportClientFactory;
import fr.gouv.vitam.batch.report.model.Report;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.worker.core.plugin.preservation.model.ContextPreservationReport;
import fr.gouv.vitam.worker.core.plugin.preservation.service.PreservationReportService;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;

import static fr.gouv.vitam.worker.core.plugin.preservation.PreservationFinalizationPlugin.PRESERVATION;
import static fr.gouv.vitam.worker.core.plugin.preservation.TestWorkerParameter.TestWorkerParameterBuilder.workerParameterBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PreservationFinalizationPluginTest {

    private static final String CONTAINER_NAME = "CONTAINER_NAME_TEST";

    private final TestWorkerParameter parameter = workerParameterBuilder()
        .withContainerName(CONTAINER_NAME)
        .withRequestId(CONTAINER_NAME)
        .build();

    private PreservationFinalizationPlugin plugin;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private WorkspaceClientFactory workspaceClientFactory;

    @Mock
    private WorkspaceClient workspaceClient;

    @Mock
    private BatchReportClientFactory batchReportFactory;

    @Mock
    private BatchReportClient batchReportClient;

    @Mock
    private LogbookOperationsClientFactory logbookOperationsClientFactory;

    @Mock
    private LogbookOperationsClient logbookOperationsClient;

    @Mock
    private PreservationReportService preservationReportService;

    @Before
    public void setUp() throws Exception {
        given(batchReportFactory.getClient()).willReturn(batchReportClient);
        given(workspaceClientFactory.getClient()).willReturn(workspaceClient);
        given(logbookOperationsClientFactory.getClient()).willReturn(logbookOperationsClient);
        plugin = new PreservationFinalizationPlugin(preservationReportService, logbookOperationsClient);
    }

    @Test
    @RunWithCustomExecutor
    public void should_finalize_preservation_report() throws Exception {
        // Given
        File newLocalFile = tempFolder.newFile();
        TestHandlerIO handlerIO = new TestHandlerIO();
        handlerIO.setNewLocalFile(newLocalFile);
        try (
            InputStream resourceAsStream = getClass().getResourceAsStream("/preservation/preservationRequest");
            InputStream scenarioInputStream = getClass().getResourceAsStream("/preservation/preservationDocument")
        ) {
            populateTestHandlerIo(handlerIO, resourceAsStream, scenarioInputStream);

            JsonNode logbookOperationJson = JsonHandler.getFromInputStream(
                getClass().getResourceAsStream("/preservation/logbookOperationOk.json")
            );
            given(logbookOperationsClient.selectOperationById(anyString())).willReturn(logbookOperationJson);
            when(preservationReportService.isReportWrittenInWorkspace(anyString())).thenReturn(false);

            // When
            ItemStatus itemStatus = plugin.execute(parameter, handlerIO);
            // Then
            assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
            assertThat(itemStatus.getItemId()).isEqualTo("PRESERVATION_FINALIZATION");
        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_assert_report_operation_OK() throws Exception {
        // Given
        File newLocalFile = tempFolder.newFile();
        TestHandlerIO handlerIO = new TestHandlerIO();
        handlerIO.setNewLocalFile(newLocalFile);
        try (
            InputStream resourceAsStream = getClass().getResourceAsStream("/preservation/preservationRequest");
            InputStream expectedInputStream = getClass().getResourceAsStream("/preservation/expectedReport.json");
            InputStream scenarioInputStream = getClass().getResourceAsStream("/preservation/preservationDocument")
        ) {
            populateTestHandlerIo(handlerIO, resourceAsStream, scenarioInputStream);

            ContextPreservationReport expectedReport = JsonHandler.getFromInputStream(
                expectedInputStream,
                ContextPreservationReport.class
            );
            JsonNode logbookOperationJson = JsonHandler.getFromInputStream(
                getClass().getResourceAsStream("/preservation/logbookOperationOk.json")
            );
            given(logbookOperationsClient.selectOperationById(anyString())).willReturn(logbookOperationJson);
            ArgumentCaptor<Report> reportArgumentCaptor = ArgumentCaptor.forClass(Report.class);
            doNothing().when(preservationReportService).storeReportToWorkspace(reportArgumentCaptor.capture());
            when(preservationReportService.isReportWrittenInWorkspace(anyString())).thenReturn(false);
            // When
            ItemStatus itemStatus = plugin.execute(parameter, handlerIO);
            // Then

            verify(preservationReportService).isReportWrittenInWorkspace(CONTAINER_NAME);
            verify(preservationReportService).storeReportToWorkspace(any());
            verify(preservationReportService).storeReportToOffers(CONTAINER_NAME);
            verify(preservationReportService).cleanupReport(CONTAINER_NAME);

            Report report = reportArgumentCaptor.getValue();
            assertThat(report.getOperationSummary().getOutDetail()).isEqualTo("PRESERVATION.OK");
            assertThat(report.getOperationSummary().getOutMsg()).isEqualTo(
                VitamLogbookMessages.getCodeOp(PRESERVATION, StatusCode.OK)
            );
            assertThat(report.getOperationSummary().getOutcome()).isEqualTo("OK");
            assertThat(report.getOperationSummary().getEvDetData()).isEqualTo(null);
            ContextPreservationReport contextPreservationReport = JsonHandler.getFromJsonNode(
                report.getContext(),
                ContextPreservationReport.class
            );
            assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
            assertThat(itemStatus.getItemId()).isEqualTo("PRESERVATION_FINALIZATION");
            assertThat(contextPreservationReport.getDslQuery()).isEqualTo(expectedReport.getDslQuery());
            assertThat(JsonHandler.unprettyPrint(contextPreservationReport.getGriffinModel())).isEqualTo(
                JsonHandler.unprettyPrint(expectedReport.getGriffinModel())
            );
            assertThat(JsonHandler.unprettyPrint(contextPreservationReport.getPreservationScenarioModel())).isEqualTo(
                JsonHandler.unprettyPrint(expectedReport.getPreservationScenarioModel())
            );
        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_not_regenerate_report_when_report_already_stored_in_workspace() throws Exception {
        // Given
        File newLocalFile = tempFolder.newFile();
        TestHandlerIO handlerIO = new TestHandlerIO();
        handlerIO.setNewLocalFile(newLocalFile);
        try (
            InputStream resourceAsStream = getClass().getResourceAsStream("/preservation/preservationRequest");
            InputStream scenarioInputStream = getClass().getResourceAsStream("/preservation/preservationDocument")
        ) {
            populateTestHandlerIo(handlerIO, resourceAsStream, scenarioInputStream);

            JsonNode logbookOperationJson = JsonHandler.getFromInputStream(
                getClass().getResourceAsStream("/preservation/logbookOperationOk.json")
            );
            given(logbookOperationsClient.selectOperationById(anyString())).willReturn(logbookOperationJson);
            when(preservationReportService.isReportWrittenInWorkspace(anyString())).thenReturn(true);
            // When
            ItemStatus itemStatus = plugin.execute(parameter, handlerIO);
            // Then

            verify(preservationReportService).isReportWrittenInWorkspace(CONTAINER_NAME);
            verify(preservationReportService, never()).storeReportToWorkspace(any());
            verify(preservationReportService).storeReportToOffers(CONTAINER_NAME);
            verify(preservationReportService).cleanupReport(CONTAINER_NAME);
        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_assert_report_operation_KO() throws Exception {
        // Given
        File newLocalFile = tempFolder.newFile();
        TestHandlerIO handlerIO = new TestHandlerIO();
        handlerIO.setNewLocalFile(newLocalFile);
        try (
            InputStream resourceAsStream = getClass().getResourceAsStream("/preservation/preservationRequest");
            InputStream scenarioInputStream = getClass().getResourceAsStream("/preservation/preservationDocument")
        ) {
            populateTestHandlerIo(handlerIO, resourceAsStream, scenarioInputStream);
            JsonNode logbookOperationJson = JsonHandler.getFromInputStream(
                getClass().getResourceAsStream("/preservation/logbookOperationKo.json")
            );
            given(logbookOperationsClient.selectOperationById(anyString())).willReturn(logbookOperationJson);
            doNothing().when(preservationReportService).storeReportToWorkspace(any());
            when(preservationReportService.isReportWrittenInWorkspace(anyString())).thenReturn(false);
            // When
            ItemStatus itemStatus = plugin.execute(parameter, handlerIO);
            // Then
            assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
            assertThat(itemStatus.getItemId()).isEqualTo("PRESERVATION_FINALIZATION");
        }
    }

    private void populateTestHandlerIo(
        TestHandlerIO handlerIO,
        InputStream resourceAsStream,
        InputStream scenarioInputStream
    ) throws URISyntaxException, ProcessingException {
        URL griffinUrl = getClass().getResource("/preservation/griffinModel");
        File file = new File(griffinUrl.toURI());
        handlerIO.transferFileToWorkspace("griffinModel", file, false, false);
        handlerIO.setMapOfInputStreamFromWorkspace("preservationRequest", resourceAsStream);
        handlerIO.setMapOfInputStreamFromWorkspace("preservationScenarioModel", scenarioInputStream);
    }
}
