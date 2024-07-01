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
package fr.gouv.vitam.worker.core.plugin.massprocessing;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.batch.report.client.BatchReportClient;
import fr.gouv.vitam.batch.report.client.BatchReportClientFactory;
import fr.gouv.vitam.batch.report.model.Report;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
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

import static fr.gouv.vitam.batch.report.model.ReportType.UPDATE_UNIT;
import static fr.gouv.vitam.common.model.StatusCode.FATAL;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.common.model.StatusCode.WARNING;
import static fr.gouv.vitam.worker.core.plugin.CommonReportService.JSONL_EXTENSION;
import static fr.gouv.vitam.worker.core.plugin.CommonReportService.WORKSPACE_REPORT_URI;
import static fr.gouv.vitam.worker.core.plugin.massprocessing.description.MassUpdateUnitsProcess.MASS_UPDATE_UNITS;
import static fr.gouv.vitam.worker.core.plugin.preservation.TestWorkerParameter.TestWorkerParameterBuilder.workerParameterBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class MassUpdateFinalizeTest {

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
    private MassUpdateFinalize massUpdateFinalize;

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
        handlerIO.setJsonFromWorkspace("query.json", JsonHandler.createObjectNode().put("Context", "request"));

        when(logbookOperationsClient.selectOperationById(operationId)).thenReturn(
            getLogbookOperationRequestResponseOK()
        );

        // When
        ItemStatus itemStatus = massUpdateFinalize.execute(workerParameter, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(OK);
    }

    @Test
    @RunWithCustomExecutor
    public void should_generate_report_WARNING() throws Exception {
        // Given
        String operationId = "MY_OPERATION_ID";

        WorkerParameters workerParameter = workerParameterBuilder()
            .withContainerName(operationId)
            .withWorkflowStatusKo(OK.name())
            .build();
        TestHandlerIO handlerIO = new TestHandlerIO();
        handlerIO.setJsonFromWorkspace("query.json", JsonHandler.createObjectNode().put("Context", "request"));

        when(logbookOperationsClient.selectOperationById(operationId)).thenReturn(
            getLogbookOperationRequestResponseWarning()
        );

        // When
        ItemStatus itemStatus = massUpdateFinalize.execute(workerParameter, handlerIO);

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

        int numberOfOK = 54;
        int numberOfKO = 42;

        ArgumentCaptor<Report> reportCaptor = ArgumentCaptor.forClass(Report.class);

        when(logbookOperationsClient.selectOperationById(operationId)).thenReturn(
            getLogbookOperationRequestResponseOK(numberOfOK, numberOfKO)
        );
        doNothing().when(batchReportClient).storeReportToWorkspace(reportCaptor.capture());

        // When
        massUpdateFinalize.execute(workerParameter, handlerIO);

        // Then
        assertThat(reportCaptor.getValue().getReportSummary().getVitamResults().getNbOk()).isEqualTo(numberOfOK);
        assertThat(reportCaptor.getValue().getReportSummary().getVitamResults().getNbKo()).isEqualTo(numberOfKO);

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
        InputStream report = PropertiesUtils.getResourceAsStream("massUpdateFinalize/report.jsonl");
        handlerIO.setJsonFromWorkspace("report.jsonl", JsonHandler.getFromInputStream(report));
        FileUtils.writeStringToFile(existingReport, "data", StandardCharsets.UTF_8);

        handlerIO.transferAtomicFileToWorkspace(WORKSPACE_REPORT_URI, existingReport);

        // When
        massUpdateFinalize.execute(workerParameter, handlerIO);

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
        massUpdateFinalize.execute(workerParameter, handlerIO);

        // Then
        verify(batchReportClient).storeReportToWorkspace(any());
    }

    @Test
    @RunWithCustomExecutor
    public void should_generate_report_FATAL() throws Exception {
        // Given
        String operationId = "MY_OPERATION_ID";

        WorkerParameters workerParameter = workerParameterBuilder().withContainerName(operationId).build();
        TestHandlerIO handlerIO = new TestHandlerIO();
        handlerIO.setJsonFromWorkspace("query.json", JsonHandler.createObjectNode().put("Context", "request"));

        when(logbookOperationsClient.selectOperationById(operationId)).thenThrow(
            new LogbookClientException("Client error cause FATAL.")
        );

        // When
        ItemStatus itemStatus = massUpdateFinalize.execute(workerParameter, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(FATAL);
    }

    @Test
    @RunWithCustomExecutor
    public void should_generate_report_Fatal() throws Exception {
        // Given
        String operationId = "MY_OPERATION_ID";

        WorkerParameters workerParameter = workerParameterBuilder().withContainerName(operationId).build();
        TestHandlerIO handlerIO = new TestHandlerIO();
        handlerIO.setJsonFromWorkspace("query.json", JsonHandler.createObjectNode().put("Context", "request"));

        when(logbookOperationsClient.selectOperationById(operationId)).thenThrow(
            new InvalidParseOperationException("Any error cause KO.")
        );

        // When
        ItemStatus itemStatus = massUpdateFinalize.execute(workerParameter, handlerIO);

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
        massUpdateFinalize.execute(workerParameter, handlerIO);

        // Then
        verify(batchReportClient).cleanupReport(operationId, UPDATE_UNIT);
    }

    private JsonNode getLogbookOperationRequestResponseOK() throws InvalidParseOperationException {
        return getLogbookOperationRequestResponseOK(3, 0);
    }

    private JsonNode getLogbookOperationRequestResponseWarning() throws InvalidParseOperationException {
        return getLogbookOperationRequestResponseOK(1, 2);
    }

    private JsonNode getLogbookOperationRequestResponseOK(int numberOfOK, int numberOfKO)
        throws InvalidParseOperationException {
        RequestResponseOK<LogbookOperation> logbookOperationResult = new RequestResponseOK<>();
        LogbookOperation operation = new LogbookOperation();
        LogbookEventOperation logbookEventOperation = new LogbookEventOperation();
        logbookEventOperation.setEvDetData(
            JsonHandler.unprettyPrint(JsonHandler.createObjectNode().put("data", "data"))
        );
        logbookEventOperation.setEvType(MASS_UPDATE_UNITS);
        logbookEventOperation.setOutMessg("My awesome message" + DETAILS + "OK:" + numberOfOK + " KO:" + numberOfKO);
        LogbookEventOperation logbookEventOperation1 = new LogbookEventOperation();
        logbookEventOperation1.setEvType("EVENT_TYPE");
        operation.setEvents(Arrays.asList(logbookEventOperation1, logbookEventOperation, logbookEventOperation1));
        operation.setRightsStatementIdentifier(
            JsonHandler.unprettyPrint(JsonHandler.createObjectNode().put("identifier", "identifier"))
        );
        logbookOperationResult.addResult(operation);
        return JsonHandler.toJsonNode(logbookOperationResult);
    }
}
