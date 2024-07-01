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
package fr.gouv.vitam.worker.core.plugin.bulkatomicupdate;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.batch.report.client.BatchReportClient;
import fr.gouv.vitam.batch.report.client.BatchReportClientFactory;
import fr.gouv.vitam.batch.report.model.Report;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.logbook.LogbookEventOperation;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.worker.core.plugin.preservation.TestHandlerIO;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static fr.gouv.vitam.batch.report.model.ReportType.BULK_UPDATE_UNIT;
import static fr.gouv.vitam.common.model.StatusCode.FATAL;
import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.common.model.StatusCode.WARNING;
import static fr.gouv.vitam.worker.core.plugin.CommonReportService.JSONL_EXTENSION;
import static fr.gouv.vitam.worker.core.plugin.CommonReportService.WORKSPACE_REPORT_URI;
import static fr.gouv.vitam.worker.core.plugin.bulkatomicupdate.BulkAtomicUpdateProcess.BULK_ATOMIC_UPDATE_UNITS_PLUGIN_NAME;
import static fr.gouv.vitam.worker.core.plugin.preservation.TestWorkerParameter.TestWorkerParameterBuilder.workerParameterBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class BulkAtomicUpdateFinalizeTest {

    private static final String DETAILS = " Detail= ";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Mock
    private LogbookOperationsClientFactory logbookOperationsClientFactory;

    @Mock
    private LogbookOperationsClient logbookOperationsClient;

    @Mock
    private BatchReportClientFactory batchReportClientFactory;

    @Mock
    private BatchReportClient batchReportClient;

    @Mock
    private StorageClientFactory storageClientFactory;

    @Mock
    private StorageClient storageClient;

    @InjectMocks
    private BulkAtomicUpdateFinalize bulkAtomicUpdateFinalize;

    @Before
    public void setup() {
        given(batchReportClientFactory.getClient()).willReturn(batchReportClient);
        given(logbookOperationsClientFactory.getClient()).willReturn(logbookOperationsClient);
        given(storageClientFactory.getClient()).willReturn(storageClient);
    }

    @Test
    @RunWithCustomExecutor
    public void should_generate_report_OK() throws Exception {
        // Given
        String operationId = "MY_OPERATION_ID";

        WorkerParameters workerParameter = workerParameterBuilder().withContainerName(operationId).build();
        TestHandlerIO handlerIO = new TestHandlerIO();

        JsonNode logbookOperationOK = JsonHandler.getFromFile(
            PropertiesUtils.findFile("BulkAtomicUpdateFinalize/logbook_ok.json")
        );
        when(logbookOperationsClient.selectOperationById(operationId)).thenReturn(logbookOperationOK);

        // When
        ItemStatus itemStatus = bulkAtomicUpdateFinalize.execute(workerParameter, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(OK);
    }

    @Test
    @RunWithCustomExecutor
    public void should_generate_report_WARNING_when_Prepare_WARNING() throws Exception {
        // Given
        String operationId = "MY_OPERATION_ID";

        WorkerParameters workerParameter = workerParameterBuilder()
            .withContainerName(operationId)
            .withWorkflowStatusKo(WARNING.name())
            .build();
        TestHandlerIO handlerIO = new TestHandlerIO();

        JsonNode logbookOperationWARNING = JsonHandler.getFromFile(
            PropertiesUtils.findFile("BulkAtomicUpdateFinalize/logbook_warning_1.json")
        );
        when(logbookOperationsClient.selectOperationById(operationId)).thenReturn(logbookOperationWARNING);

        // When
        ItemStatus itemStatus = bulkAtomicUpdateFinalize.execute(workerParameter, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(WARNING);
    }

    @Test
    @RunWithCustomExecutor
    public void should_generate_report_WARNING_when_Update_WARNING() throws Exception {
        // Given
        String operationId = "MY_OPERATION_ID";

        WorkerParameters workerParameter = workerParameterBuilder()
            .withContainerName(operationId)
            .withWorkflowStatusKo(WARNING.name())
            .build();
        TestHandlerIO handlerIO = new TestHandlerIO();

        JsonNode logbookOperationWARNING = JsonHandler.getFromFile(
            PropertiesUtils.findFile("BulkAtomicUpdateFinalize/logbook_warning_2.json")
        );
        when(logbookOperationsClient.selectOperationById(operationId)).thenReturn(logbookOperationWARNING);

        // When
        ItemStatus itemStatus = bulkAtomicUpdateFinalize.execute(workerParameter, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(WARNING);
    }

    @Test
    @RunWithCustomExecutor
    public void should_generate_report_WARNING_when_Update_WARNING_empty() throws Exception {
        // Given
        String operationId = "MY_OPERATION_ID";

        WorkerParameters workerParameter = workerParameterBuilder()
            .withContainerName(operationId)
            .withWorkflowStatusKo(WARNING.name())
            .build();
        TestHandlerIO handlerIO = new TestHandlerIO();

        JsonNode logbookOperationWARNING = JsonHandler.getFromFile(
            PropertiesUtils.findFile("BulkAtomicUpdateFinalize/logbook_warning_empty.json")
        );
        when(logbookOperationsClient.selectOperationById(operationId)).thenReturn(logbookOperationWARNING);

        // When
        ItemStatus itemStatus = bulkAtomicUpdateFinalize.execute(workerParameter, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(WARNING);
    }

    @Test
    @RunWithCustomExecutor
    public void should_generate_report_WARNING_when_Prepare_KO() throws Exception {
        // Given
        String operationId = "MY_OPERATION_ID";

        WorkerParameters workerParameter = workerParameterBuilder()
            .withContainerName(operationId)
            .withWorkflowStatusKo(KO.name())
            .build();
        TestHandlerIO handlerIO = new TestHandlerIO();

        JsonNode logbookOperationKO = JsonHandler.getFromFile(
            PropertiesUtils.findFile("BulkAtomicUpdateFinalize/logbook_ko.json")
        );
        when(logbookOperationsClient.selectOperationById(operationId)).thenReturn(logbookOperationKO);

        // When
        ItemStatus itemStatus = bulkAtomicUpdateFinalize.execute(workerParameter, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(WARNING);
    }

    @Test
    @RunWithCustomExecutor
    public void should_generate_report_WARNING_when_Threshold_KO() throws Exception {
        // Given
        String operationId = "MY_OPERATION_ID";

        WorkerParameters workerParameter = workerParameterBuilder()
            .withContainerName(operationId)
            .withWorkflowStatusKo(KO.name())
            .build();
        TestHandlerIO handlerIO = new TestHandlerIO();

        JsonNode logbookOperationKO = JsonHandler.getFromFile(
            PropertiesUtils.findFile("BulkAtomicUpdateFinalize/logbook_ko_threshold.json")
        );
        when(logbookOperationsClient.selectOperationById(operationId)).thenReturn(logbookOperationKO);

        // When
        ItemStatus itemStatus = bulkAtomicUpdateFinalize.execute(workerParameter, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(WARNING);
    }

    @Test
    @RunWithCustomExecutor
    public void should_create_report_with_number_of_OK_from_logbook() throws Exception {
        // Given
        String operationId = "MY_OPERATION_ID";

        WorkerParameters workerParameter = workerParameterBuilder()
            .withContainerName(operationId)
            .withWorkflowStatusKo(OK.name())
            .build();
        TestHandlerIO handlerIO = new TestHandlerIO();
        handlerIO.setJsonFromWorkspace("query.json", JsonHandler.createObjectNode().put("Context", "request"));

        int numberOfOKUpdate = 54;
        int numberOfKOUpdate = 42;

        ArgumentCaptor<Report> reportCaptor = ArgumentCaptor.forClass(Report.class);

        when(logbookOperationsClient.selectOperationById(operationId)).thenReturn(
            getLogbookOperationRequestResponseOK(0, 0, 0, numberOfOKUpdate, 0, numberOfKOUpdate)
        );
        doNothing().when(batchReportClient).storeReportToWorkspace(reportCaptor.capture());

        // When
        bulkAtomicUpdateFinalize.execute(workerParameter, handlerIO);

        // Then
        assertThat(reportCaptor.getValue().getReportSummary().getVitamResults().getNbOk()).isEqualTo(numberOfOKUpdate);
        assertThat(reportCaptor.getValue().getReportSummary().getVitamResults().getNbKo()).isEqualTo(numberOfKOUpdate);

        ArgumentCaptor<ObjectDescription> descriptionArgumentCaptor = ArgumentCaptor.forClass(ObjectDescription.class);
        verify(storageClient).storeFileFromWorkspace(
            eq(VitamConfiguration.getDefaultStrategy()),
            eq(DataCategory.REPORT),
            eq(operationId + JSONL_EXTENSION),
            descriptionArgumentCaptor.capture()
        );
        assertThat(descriptionArgumentCaptor.getValue().getWorkspaceContainerGUID()).isEqualTo(operationId);
        assertThat(descriptionArgumentCaptor.getValue().getWorkspaceObjectURI()).isEqualTo(WORKSPACE_REPORT_URI);
    }

    @Test
    @RunWithCustomExecutor
    public void should_not_regenerate_report_when_already_exists_in_workspace() throws Exception {
        // Given
        String operationId = "MY_OPERATION_ID";

        WorkerParameters workerParameter = workerParameterBuilder().withContainerName(operationId).build();
        TestHandlerIO handlerIO = new TestHandlerIO();

        File existingReport = tempFolder.newFile();
        InputStream report = PropertiesUtils.getResourceAsStream("BulkAtomicUpdateFinalize/report.jsonl");
        handlerIO.setJsonFromWorkspace("report.jsonl", JsonHandler.getFromInputStream(report));
        FileUtils.writeStringToFile(existingReport, "data", StandardCharsets.UTF_8);

        handlerIO.transferAtomicFileToWorkspace(WORKSPACE_REPORT_URI, existingReport);

        // When
        bulkAtomicUpdateFinalize.execute(workerParameter, handlerIO);

        // Then
        verify(batchReportClient, never()).storeReportToWorkspace(any());
        verifyZeroInteractions(logbookOperationsClient);

        ArgumentCaptor<ObjectDescription> descriptionArgumentCaptor = ArgumentCaptor.forClass(ObjectDescription.class);
        verify(storageClient).storeFileFromWorkspace(
            eq(VitamConfiguration.getDefaultStrategy()),
            eq(DataCategory.REPORT),
            eq(operationId + JSONL_EXTENSION),
            descriptionArgumentCaptor.capture()
        );
        assertThat(descriptionArgumentCaptor.getValue().getWorkspaceContainerGUID()).isEqualTo(operationId);
        assertThat(descriptionArgumentCaptor.getValue().getWorkspaceObjectURI()).isEqualTo(WORKSPACE_REPORT_URI);
    }

    @Test
    @RunWithCustomExecutor
    public void should_store_report_when_OK() throws Exception {
        // Given
        String operationId = "MY_OPERATION_ID";

        WorkerParameters workerParameter = workerParameterBuilder().withContainerName(operationId).build();
        TestHandlerIO handlerIO = new TestHandlerIO();
        handlerIO.setJsonFromWorkspace("query.json", JsonHandler.createObjectNode().put("Context", "request"));

        when(logbookOperationsClient.selectOperationById(operationId)).thenReturn(
            getLogbookOperationRequestResponseOK()
        );

        // When
        bulkAtomicUpdateFinalize.execute(workerParameter, handlerIO);

        // Then
        verify(batchReportClient).storeReportToWorkspace(any());
    }

    @Test
    @RunWithCustomExecutor
    public void should_generate_report_Fatal_Logbook_LogbookClientException() throws Exception {
        // Given
        String operationId = "MY_OPERATION_ID";

        WorkerParameters workerParameter = workerParameterBuilder().withContainerName(operationId).build();
        TestHandlerIO handlerIO = new TestHandlerIO();
        handlerIO.setJsonFromWorkspace("query.json", JsonHandler.createObjectNode().put("Context", "request"));

        when(logbookOperationsClient.selectOperationById(operationId)).thenThrow(
            new LogbookClientException("Client error cause FATAL.")
        );

        // When
        ItemStatus itemStatus = bulkAtomicUpdateFinalize.execute(workerParameter, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(FATAL);
    }

    @Test
    @RunWithCustomExecutor
    public void should_generate_report_Fatal_Logbook_InvalidParseOperationException() throws Exception {
        // Given
        String operationId = "MY_OPERATION_ID";

        WorkerParameters workerParameter = workerParameterBuilder().withContainerName(operationId).build();
        TestHandlerIO handlerIO = new TestHandlerIO();
        handlerIO.setJsonFromWorkspace("query.json", JsonHandler.createObjectNode().put("Context", "request"));

        when(logbookOperationsClient.selectOperationById(operationId)).thenThrow(
            new InvalidParseOperationException("Any error cause KO.")
        );

        // When
        ItemStatus itemStatus = bulkAtomicUpdateFinalize.execute(workerParameter, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(FATAL);
    }

    @Test
    @RunWithCustomExecutor
    public void should_generate_report_Fatal_BatchReport_VitamClientInternalException() throws Exception {
        // Given
        String operationId = "MY_OPERATION_ID";

        WorkerParameters workerParameter = workerParameterBuilder().withContainerName(operationId).build();
        TestHandlerIO handlerIO = new TestHandlerIO();
        handlerIO.setJsonFromWorkspace("query.json", JsonHandler.createObjectNode().put("Context", "request"));
        when(logbookOperationsClient.selectOperationById(operationId)).thenReturn(
            getLogbookOperationRequestResponseOK()
        );

        doThrow(new VitamClientInternalException("Any error cause KO."))
            .when(batchReportClient)
            .storeReportToWorkspace(any());

        // When
        ItemStatus itemStatus = bulkAtomicUpdateFinalize.execute(workerParameter, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(FATAL);
    }

    @Test
    @RunWithCustomExecutor
    public void should_generate_report_Fatal_Storage_StorageServerClientException() throws Exception {
        // Given
        String operationId = "MY_OPERATION_ID";

        WorkerParameters workerParameter = workerParameterBuilder().withContainerName(operationId).build();
        TestHandlerIO handlerIO = new TestHandlerIO();
        handlerIO.setJsonFromWorkspace("query.json", JsonHandler.createObjectNode().put("Context", "request"));
        when(logbookOperationsClient.selectOperationById(operationId)).thenReturn(
            getLogbookOperationRequestResponseOK()
        );

        when(
            storageClient.storeFileFromWorkspace(
                eq(VitamConfiguration.getDefaultStrategy()),
                eq(DataCategory.REPORT),
                any(),
                any()
            )
        ).thenThrow(new StorageServerClientException("Client error cause FATAL."));

        // When
        ItemStatus itemStatus = bulkAtomicUpdateFinalize.execute(workerParameter, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(FATAL);
    }

    @Test
    @RunWithCustomExecutor
    public void should_cleanup_reports_at_end() throws Exception {
        // Given
        String operationId = "MY_OPERATION_ID";

        WorkerParameters workerParameter = workerParameterBuilder().withContainerName(operationId).build();
        TestHandlerIO handlerIO = new TestHandlerIO();
        handlerIO.setJsonFromWorkspace("query.json", JsonHandler.createObjectNode().put("Context", "request"));

        when(logbookOperationsClient.selectOperationById(operationId)).thenReturn(
            getLogbookOperationRequestResponseOK()
        );

        // When
        bulkAtomicUpdateFinalize.execute(workerParameter, handlerIO);

        // Then
        verify(batchReportClient).cleanupReport(operationId, BULK_UPDATE_UNIT);
    }

    private JsonNode getLogbookOperationRequestResponseOK() throws InvalidParseOperationException {
        return getLogbookOperationRequestResponseOK(0, 0, 0, 3, 0, 0);
    }

    private JsonNode getLogbookOperationRequestResponseWarning() throws InvalidParseOperationException {
        return getLogbookOperationRequestResponseOK(0, 0, 0, 1, 0, 2);
    }

    private JsonNode getLogbookOperationRequestResponseKO() throws InvalidParseOperationException {
        return getLogbookOperationRequestResponseOK(0, 0, 0, 0, 0, 3);
    }

    private JsonNode getLogbookOperationRequestNoResponse() throws InvalidParseOperationException {
        return getLogbookOperationRequestResponseOK(0, 0, 0, 0, 0, 0);
    }

    private JsonNode getLogbookOperationRequestResponseOK(
        int numberOfOKPrepare,
        int numberOfWarningPrepare,
        int numberOfKOPrepare,
        int numberOfOKUpdate,
        int numberOfWarningUpdate,
        int numberOfKOUpdate
    ) throws InvalidParseOperationException {
        RequestResponseOK<LogbookOperation> logbookOperationResult = new RequestResponseOK<>();
        LogbookOperation operation = new LogbookOperation();
        LogbookEventOperation logbookEventOperationPrepare = new LogbookEventOperation();
        logbookEventOperationPrepare.setEvDetData(
            JsonHandler.unprettyPrint(JsonHandler.createObjectNode().put("data", "data"))
        );
        logbookEventOperationPrepare.setEvType(
            PrepareBulkAtomicUpdate.PREPARE_BULK_ATOMIC_UPDATE_UNIT_LIST_PLUGIN_NAME
        );
        logbookEventOperationPrepare.setOutMessg(
            "My awesome message prepare" + DETAILS + "OK:" + numberOfOKPrepare + " KO:" + numberOfKOPrepare
        );
        LogbookEventOperation logbookEventOperationUpdate = new LogbookEventOperation();
        logbookEventOperationUpdate.setEvDetData(
            JsonHandler.unprettyPrint(JsonHandler.createObjectNode().put("data", "data"))
        );
        logbookEventOperationUpdate.setEvType(BULK_ATOMIC_UPDATE_UNITS_PLUGIN_NAME);
        logbookEventOperationUpdate.setOutMessg(
            "My awesome message update" + DETAILS + "OK:" + numberOfOKUpdate + " KO:" + numberOfKOUpdate
        );
        LogbookEventOperation logbookEventOperation1 = new LogbookEventOperation();
        logbookEventOperation1.setEvType("EVENT_TYPE");
        operation.setEvents(
            Arrays.asList(
                logbookEventOperation1,
                logbookEventOperationPrepare,
                logbookEventOperationUpdate,
                logbookEventOperation1
            )
        );
        operation.setRightsStatementIdentifier(
            JsonHandler.unprettyPrint(JsonHandler.createObjectNode().put("identifier", "identifier"))
        );
        logbookOperationResult.addResult(operation);
        return JsonHandler.toJsonNode(logbookOperationResult);
    }
}
