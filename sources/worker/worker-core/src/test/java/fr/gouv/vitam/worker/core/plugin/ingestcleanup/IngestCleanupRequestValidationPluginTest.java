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
package fr.gouv.vitam.worker.core.plugin.ingestcleanup;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.ProcessDetail;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameterName;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.worker.core.plugin.preservation.TestHandlerIO;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.FileNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

public class IngestCleanupRequestValidationPluginTest {

    private static final String INGEST_OPERATION_ID = "aeeaaaaaacesicexaah6kalo7e62mmqaaaaq";

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private ProcessingManagementClientFactory processingManagementClientFactory;

    @Mock
    private ProcessingManagementClient processingManagementClient;

    @Mock
    private LogbookOperationsClientFactory logbookOperationsClientFactory;

    @Mock
    private LogbookOperationsClient logbookOperationsClient;

    @InjectMocks
    private IngestCleanupRequestValidationPlugin instance;

    private WorkerParameters params;
    private TestHandlerIO handlerIO;

    @Before
    public void setUp() throws Exception {
        doReturn(processingManagementClient).when(processingManagementClientFactory).getClient();
        doReturn(logbookOperationsClient).when(logbookOperationsClientFactory).getClient();
        VitamThreadUtils.getVitamSession().setTenantId(0);
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(0));

        params = WorkerParametersFactory.newWorkerParameters()
            .putParameterValue(WorkerParameterName.ingestOperationIdToCleanup, INGEST_OPERATION_ID)
            .putParameterValue(WorkerParameterName.containerName, VitamThreadUtils.getVitamSession().getRequestId());
        handlerIO = new TestHandlerIO().setContainerName(VitamThreadUtils.getVitamSession().getRequestId());
    }

    @RunWithCustomExecutor
    @Test
    public void testUnknownLogbookOperationThenKO() throws Exception {
        // Given
        givenNoLogbookOperation();
        givenNoActiveProcess();

        // When
        ItemStatus itemStatus = instance.execute(params, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
    }

    @RunWithCustomExecutor
    @Test
    public void testNotAnIngestLogbookOperationThenKO() throws Exception {
        // Given
        givenLogbookOperation("IngestCleanup/RequestValidation/notAnIngestOperation.json");
        givenNoActiveProcess();

        // When
        ItemStatus itemStatus = instance.execute(params, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
    }

    @RunWithCustomExecutor
    @Test
    public void testIngestOKWithoutActiveProcessThenKO() throws Exception {
        // Given
        givenLogbookOperation("IngestCleanup/RequestValidation/ingestLogbookOk.json");
        givenNoActiveProcess();

        // When
        ItemStatus itemStatus = instance.execute(params, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
    }

    @RunWithCustomExecutor
    @Test
    public void testIngestOKWithActiveCompletedProcessThenKO() throws Exception {
        // Given
        givenLogbookOperation("IngestCleanup/RequestValidation/ingestLogbookOk.json");
        givenProcessState(ProcessState.COMPLETED, StatusCode.OK);

        // When
        ItemStatus itemStatus = instance.execute(params, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
    }

    @RunWithCustomExecutor
    @Test
    public void testIngestWarnWithoutActiveProcessThenKO() throws Exception {
        // Given
        givenLogbookOperation("IngestCleanup/RequestValidation/ingestLogbookWarn.json");
        givenNoActiveProcess();

        // When
        ItemStatus itemStatus = instance.execute(params, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
    }

    @RunWithCustomExecutor
    @Test
    public void testIngestWarnWithActiveCompletedProcessThenKO() throws Exception {
        // Given
        givenLogbookOperation("IngestCleanup/RequestValidation/ingestLogbookWarn.json");
        givenProcessState(ProcessState.COMPLETED, StatusCode.WARNING);

        // When
        ItemStatus itemStatus = instance.execute(params, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
    }

    @RunWithCustomExecutor
    @Test
    public void testIngestKOWithoutActiveProcessThenOK() throws Exception {
        // Given
        givenLogbookOperation("IngestCleanup/RequestValidation/ingestLogbookKO.json");
        givenNoActiveProcess();

        // When
        ItemStatus itemStatus = instance.execute(params, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
    }

    @RunWithCustomExecutor
    @Test
    public void testIngestKOWithActiveCompletedProcessThenOK() throws Exception {
        // Given
        givenLogbookOperation("IngestCleanup/RequestValidation/ingestLogbookKO.json");
        givenProcessState(ProcessState.COMPLETED, StatusCode.KO);

        // When
        ItemStatus itemStatus = instance.execute(params, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
    }

    @RunWithCustomExecutor
    @Test
    public void testIngestFatalWithoutActiveProcessThenOK() throws Exception {
        // Given
        givenLogbookOperation("IngestCleanup/RequestValidation/ingestLogbookFATAL.json");
        givenNoActiveProcess();

        // When
        ItemStatus itemStatus = instance.execute(params, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
    }

    @RunWithCustomExecutor
    @Test
    public void testIngestFatalWithActiveCompletedProcessThenOK() throws Exception {
        // Given
        givenLogbookOperation("IngestCleanup/RequestValidation/ingestLogbookFATAL.json");
        givenProcessState(ProcessState.COMPLETED, StatusCode.FATAL);

        // When
        ItemStatus itemStatus = instance.execute(params, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
    }

    @RunWithCustomExecutor
    @Test
    public void testIngestKilledWithoutActiveProcessThenOK() throws Exception {
        // Given
        givenLogbookOperation("IngestCleanup/RequestValidation/ingestLogbookAbort.json");
        givenNoActiveProcess();

        // When
        ItemStatus itemStatus = instance.execute(params, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
    }

    @RunWithCustomExecutor
    @Test
    public void testIngestInProgressPausedThenKO() throws Exception {
        // Given
        givenLogbookOperation("IngestCleanup/RequestValidation/ingestLogbookAbort.json");
        givenProcessState(ProcessState.PAUSE, StatusCode.FATAL);

        // When
        ItemStatus itemStatus = instance.execute(params, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
    }

    @RunWithCustomExecutor
    @Test
    public void testIngestInProgressRunningThenKO() throws Exception {
        // Given
        givenLogbookOperation("IngestCleanup/RequestValidation/ingestLogbookAbort.json");
        givenProcessState(ProcessState.RUNNING, StatusCode.FATAL);

        // When
        ItemStatus itemStatus = instance.execute(params, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
    }

    private void givenLogbookOperation(String resourcesFile)
        throws LogbookClientException, InvalidParseOperationException, FileNotFoundException {
        // Ingest with OK Status
        doReturn(JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(resourcesFile)))
            .when(logbookOperationsClient)
            .selectOperationById(INGEST_OPERATION_ID);
    }

    private void givenNoLogbookOperation() throws LogbookClientException, InvalidParseOperationException {
        // Ingest with OK Status
        doThrow(new LogbookClientNotFoundException("not found"))
            .when(logbookOperationsClient)
            .selectOperationById(INGEST_OPERATION_ID);
    }

    private void givenNoActiveProcess() throws VitamClientException {
        // No active process (garbage collected)
        doReturn(new RequestResponseOK<ProcessDetail>()).when(processingManagementClient).listOperationsDetails(any());
    }

    private void givenProcessState(ProcessState processState, StatusCode statusCode) throws VitamClientException {
        ProcessDetail processDetail = new ProcessDetail();
        processDetail.setGlobalState(processState.name());
        processDetail.setStepStatus(statusCode.name());
        doReturn(new RequestResponseOK<ProcessDetail>().addResult(processDetail))
            .when(processingManagementClient)
            .listOperationsDetails(any());
    }
}
