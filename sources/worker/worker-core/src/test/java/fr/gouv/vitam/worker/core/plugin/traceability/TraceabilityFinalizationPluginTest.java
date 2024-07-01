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

import fr.gouv.vitam.batch.report.model.Report;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.exception.ProcessingStatusException;
import fr.gouv.vitam.worker.core.plugin.traceability.service.TraceabilityReportService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static fr.gouv.vitam.common.json.JsonHandler.getFromInputStream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TraceabilityFinalizationPluginTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Mock
    private TraceabilityReportService traceabilityReportService;

    @Mock
    private LogbookOperationsClientFactory logbookOperationsClientFactory;

    @Mock
    private LogbookOperationsClient logbookClient;

    private static final String JOP_RESULTS_OK = "/AuditObjectWorkflow/operationForFinalize.json";
    private static final String PROCESS_ID_OK = "aeeaaaaaachlmb32abcaealj3uvnzgaaaaaq";
    private static final String JOP_RESULTS_KO = "/AuditObjectWorkflow/operationForFinalizeKo.json";
    private static final String PROCESS_ID_KO = "aeeaaaaaachlmb32abcaealj3uvnzgaaaaaq";
    private static final String JOP_RESULTS_WARNING = "/AuditObjectWorkflow/operationForFinalizeWarning.json";
    private static final String PROCESS_ID_WARNING = "aeeaaaaaachgzebuab35oalj5r7ng7aaaaaq";
    private static final String REQUEST_ID = "aeeaaaaaachgzebuab36baljbr9ng8aaaaaq";

    private TraceabilityFinalizationPlugin traceabilityFinalizationPlugin;
    private HandlerIO handler;
    private WorkerParameters workerParameters;

    @Captor
    private ArgumentCaptor<Report> reportInfosCaptor;

    @Before
    public void setUp() throws Exception {
        traceabilityFinalizationPlugin = new TraceabilityFinalizationPlugin(
            traceabilityReportService,
            logbookOperationsClientFactory
        );
        lenient().when(logbookOperationsClientFactory.getClient()).thenReturn(logbookClient);
        handler = mock(HandlerIO.class);
        workerParameters = mock(WorkerParameters.class);

        lenient().when(workerParameters.getRequestId()).thenReturn(REQUEST_ID);
    }

    @Test
    public void should_ok_report_when_ok_logbook() throws Exception {
        // Given
        when(workerParameters.getContainerName()).thenReturn(PROCESS_ID_KO);

        Mockito.doNothing().when(traceabilityReportService).storeReportToWorkspace(reportInfosCaptor.capture());
        when(logbookClient.selectOperationById(any())).thenReturn(
            getFromInputStream(getClass().getResourceAsStream(JOP_RESULTS_OK))
        );
        when(traceabilityReportService.isReportWrittenInWorkspace(anyString())).thenReturn(false);
        // When
        ItemStatus response = traceabilityFinalizationPlugin.execute(workerParameters, handler);

        // Then
        assertEquals(StatusCode.OK, response.getGlobalStatus());

        verify(traceabilityReportService).isReportWrittenInWorkspace(PROCESS_ID_KO);
        verify(traceabilityReportService).storeReportToWorkspace(any());
        verify(traceabilityReportService).storeReportToOffers(PROCESS_ID_KO);
        verify(traceabilityReportService).cleanupReport(PROCESS_ID_KO);

        assertThat(reportInfosCaptor.getValue().getContext()).isNotNull();
    }

    @Test
    public void should_ok_report_when_report_already_writted_to_workspace() throws Exception {
        // Given
        when(workerParameters.getContainerName()).thenReturn(PROCESS_ID_KO);

        when(logbookClient.selectOperationById(any())).thenReturn(
            getFromInputStream(getClass().getResourceAsStream(JOP_RESULTS_OK))
        );
        when(traceabilityReportService.isReportWrittenInWorkspace(anyString())).thenReturn(true);
        // When
        ItemStatus response = traceabilityFinalizationPlugin.execute(workerParameters, handler);

        // Then
        assertEquals(StatusCode.OK, response.getGlobalStatus());

        verify(traceabilityReportService).isReportWrittenInWorkspace(PROCESS_ID_KO);
        verify(traceabilityReportService, never()).storeReportToWorkspace(any());
        verify(traceabilityReportService).storeReportToOffers(PROCESS_ID_KO);
        verify(traceabilityReportService).cleanupReport(PROCESS_ID_KO);
    }

    @Test
    public void should_ko_report_when_ko_logbook() throws Exception {
        // Given
        when(workerParameters.getContainerName()).thenReturn(PROCESS_ID_KO);

        Mockito.doNothing().when(traceabilityReportService).storeReportToWorkspace(reportInfosCaptor.capture());
        when(logbookClient.selectOperationById(any())).thenReturn(
            getFromInputStream(getClass().getResourceAsStream(JOP_RESULTS_KO))
        );
        when(traceabilityReportService.isReportWrittenInWorkspace(anyString())).thenReturn(false);

        // When
        ItemStatus response = traceabilityFinalizationPlugin.execute(workerParameters, handler);

        // Then
        assertEquals(StatusCode.OK, response.getGlobalStatus());
        assertThat(reportInfosCaptor.getValue().getContext()).isNotNull();
    }

    @Test
    public void should_warning_report_when_warning_logbook() throws Exception {
        // Given
        when(workerParameters.getContainerName()).thenReturn(PROCESS_ID_WARNING);

        Mockito.doNothing().when(traceabilityReportService).storeReportToWorkspace(reportInfosCaptor.capture());
        when(logbookClient.selectOperationById(any())).thenReturn(
            getFromInputStream(getClass().getResourceAsStream(JOP_RESULTS_WARNING))
        );
        when(traceabilityReportService.isReportWrittenInWorkspace(anyString())).thenReturn(false);

        // When
        ItemStatus response = traceabilityFinalizationPlugin.execute(workerParameters, handler);

        // Then
        assertEquals(StatusCode.OK, response.getGlobalStatus());
        assertThat(reportInfosCaptor.getValue().getContext()).isNotNull();
    }

    @Test
    public void should_fatal_when_logbook_exception() throws Exception {
        // Given
        when(workerParameters.getContainerName()).thenReturn(PROCESS_ID_OK);

        Mockito.doNothing().when(traceabilityReportService).storeReportToWorkspace(reportInfosCaptor.capture());
        when(logbookClient.selectOperationById(any())).thenThrow(new LogbookClientException("Logbook exception"));
        when(traceabilityReportService.isReportWrittenInWorkspace(anyString())).thenReturn(false);

        // When
        ItemStatus response = traceabilityFinalizationPlugin.execute(workerParameters, handler);

        // Then
        assertEquals(StatusCode.FATAL, response.getGlobalStatus());
    }

    @Test
    public void should_fatal_when_report_exception() throws Exception {
        // Given
        when(workerParameters.getContainerName()).thenReturn(PROCESS_ID_OK);

        Mockito.doThrow(new ProcessingStatusException(StatusCode.FATAL, "traceability report exception"))
            .when(traceabilityReportService)
            .storeReportToWorkspace(reportInfosCaptor.capture());
        when(logbookClient.selectOperationById(any())).thenReturn(
            getFromInputStream(getClass().getResourceAsStream(JOP_RESULTS_OK))
        );
        when(traceabilityReportService.isReportWrittenInWorkspace(anyString())).thenReturn(false);

        // When
        ItemStatus response = traceabilityFinalizationPlugin.execute(workerParameters, handler);

        // Then
        assertEquals(StatusCode.FATAL, response.getGlobalStatus());
    }
}
