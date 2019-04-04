package fr.gouv.vitam.worker.core.plugin.audit;

import static fr.gouv.vitam.common.json.JsonHandler.getFromInputStream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

import fr.gouv.vitam.batch.report.model.Report;
import fr.gouv.vitam.batch.report.model.ReportType;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameterName;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.plugin.audit.exception.AuditException;

public class AuditFinalizePluginTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
            VitamThreadPoolExecutor.getDefaultExecutor());

    @Mock
    private AuditReportService auditReportService;
    @Mock
    private LogbookOperationsClientFactory logbookOperationsClientFactory;
    @Mock
    private LogbookOperationsClient logbookClient;

    private static final String JOP_RESULTS_OK = "/AuditObjectWorkflow/operationForFinalize.json";
    private static final String PROCESS_ID_OK = "aeeaaaaaachlmb32abcaealj3uvnzgaaaaaq";
    private static final String JOP_RESULTS_KO = "/AuditObjectWorkflow/operationForFinalizeKo.json";
    private static final String PROCESS_ID_KO = "aeeaaaaaachlmb32abcaealj3uvnzgaaaaaq";

    private AuditFinalizePlugin auditFinalizePlugin;

    @Captor
    private ArgumentCaptor<Report> reportInfosCaptor;

    @Before
    public void setUp() throws Exception {

        auditFinalizePlugin = new AuditFinalizePlugin(auditReportService, logbookOperationsClientFactory);

        when(logbookOperationsClientFactory.getClient()).thenReturn(logbookClient);
    }

    @RunWithCustomExecutor
    @Test
    public void should_ok_report_when_ok_logbook() throws Exception {

        // Given
        HandlerIO handler = mock(HandlerIO.class);
        VitamThreadUtils.getVitamSession().setTenantId(0);
        VitamThreadUtils.getVitamSession().setRequestId(PROCESS_ID_OK);
        WorkerParameters workerParameters = WorkerParametersFactory.newWorkerParameters()
                .setWorkerGUID(GUIDFactory.newGUID())
                .setContainerName(VitamThreadUtils.getVitamSession().getRequestId())
                .setRequestId(VitamThreadUtils.getVitamSession().getRequestId())
                .setProcessId(VitamThreadUtils.getVitamSession().getRequestId()).setObjectId("0")
                .setCurrentStep("StepName");
        workerParameters.putParameterValue(WorkerParameterName.auditActions, "AUDIT_FILE_EXISTING");
        workerParameters.putParameterValue(WorkerParameterName.auditType, "tenant");
        Mockito.doNothing().when(auditReportService).storeReport(reportInfosCaptor.capture());
        when(logbookClient.selectOperationById(any()))
                .thenReturn(getFromInputStream(getClass().getResourceAsStream(JOP_RESULTS_OK)));

        // When
        ItemStatus response = auditFinalizePlugin.execute(workerParameters, handler);

        // Then
        assertEquals(StatusCode.OK, response.getGlobalStatus());
        assertThat(reportInfosCaptor.getValue().getContext()).isNotNull();
        assertThat(reportInfosCaptor.getValue().getContext().get("auditType").asText()).isEqualTo("tenant");
        assertThat(reportInfosCaptor.getValue().getContext().get("objectId").asText()).isEqualTo("0");
        assertThat(reportInfosCaptor.getValue().getContext().get("auditActions").asText())
                .isEqualTo("AUDIT_FILE_EXISTING");
        assertThat(reportInfosCaptor.getValue().getOperationSummary()).isNotNull();
        assertThat(reportInfosCaptor.getValue().getOperationSummary().getRightsStatementIdentifier()
                .get("AccessContract").asText()).isEqualTo("ContratTNR");
        assertThat(reportInfosCaptor.getValue().getOperationSummary().getEvDetData()).isEmpty();
        assertThat(reportInfosCaptor.getValue().getOperationSummary().getEvId()).isEqualTo(PROCESS_ID_OK);
        assertThat(reportInfosCaptor.getValue().getOperationSummary().getEvType())
                .isEqualTo("AUDIT_CHECK_OBJECT.AUDIT_CHECK_OBJECT");
        assertThat(reportInfosCaptor.getValue().getOperationSummary().getOutcome())
                .isEqualTo("AUDIT_CHECK_OBJECT.AUDIT_CHECK_OBJECT.OK");
        assertThat(reportInfosCaptor.getValue().getOperationSummary().getOutMsg())
                .isEqualTo("Succès de l'audit de l'existence et de l'intégrité des objets Detail=  OK:25");
        assertThat(reportInfosCaptor.getValue().getReportSummary()).isNotNull();
        assertThat(reportInfosCaptor.getValue().getReportSummary().getExtendedInfo()).isEmpty();
        assertThat(reportInfosCaptor.getValue().getReportSummary().getEvStartDateTime())
                .isEqualTo("2019-04-02T08:42:33.715");
        assertThat(reportInfosCaptor.getValue().getReportSummary().getReportType()).isEqualTo(ReportType.AUDIT);

    }

    @RunWithCustomExecutor
    @Test
    public void should_ko_report_when_ko_logbook() throws Exception {

        // Given
        HandlerIO handler = mock(HandlerIO.class);
        VitamThreadUtils.getVitamSession().setTenantId(0);
        VitamThreadUtils.getVitamSession().setRequestId(PROCESS_ID_KO);
        WorkerParameters workerParameters = WorkerParametersFactory.newWorkerParameters()
                .setWorkerGUID(GUIDFactory.newGUID())
                .setContainerName(VitamThreadUtils.getVitamSession().getRequestId())
                .setRequestId(VitamThreadUtils.getVitamSession().getRequestId())
                .setProcessId(VitamThreadUtils.getVitamSession().getRequestId()).setObjectId("0")
                .setCurrentStep("StepName");
        workerParameters.putParameterValue(WorkerParameterName.auditActions, "AUDIT_FILE_EXISTING");
        workerParameters.putParameterValue(WorkerParameterName.auditType, "tenant");
        Mockito.doNothing().when(auditReportService).storeReport(reportInfosCaptor.capture());
        when(logbookClient.selectOperationById(any()))
                .thenReturn(getFromInputStream(getClass().getResourceAsStream(JOP_RESULTS_KO)));

        // When
        ItemStatus response = auditFinalizePlugin.execute(workerParameters, handler);

        // Then
        assertEquals(StatusCode.OK, response.getGlobalStatus());
        assertThat(reportInfosCaptor.getValue().getContext()).isNotNull();
        assertThat(reportInfosCaptor.getValue().getContext().get("auditType").asText()).isEqualTo("tenant");
        assertThat(reportInfosCaptor.getValue().getContext().get("objectId").asText()).isEqualTo("0");
        assertThat(reportInfosCaptor.getValue().getContext().get("auditActions").asText())
                .isEqualTo("AUDIT_FILE_EXISTING");
        assertThat(reportInfosCaptor.getValue().getOperationSummary()).isNotNull();
        assertThat(reportInfosCaptor.getValue().getOperationSummary().getRightsStatementIdentifier()
                .get("AccessContract").asText()).isEqualTo("ContratTNR");
        assertThat(reportInfosCaptor.getValue().getOperationSummary().getEvDetData()).isEmpty();
        assertThat(reportInfosCaptor.getValue().getOperationSummary().getEvId()).isEqualTo(PROCESS_ID_KO);
        assertThat(reportInfosCaptor.getValue().getOperationSummary().getEvType())
                .isEqualTo("AUDIT_CHECK_OBJECT.AUDIT_CHECK_OBJECT");
        assertThat(reportInfosCaptor.getValue().getOperationSummary().getOutcome())
                .isEqualTo("AUDIT_CHECK_OBJECT.AUDIT_CHECK_OBJECT.KO");
        assertThat(reportInfosCaptor.getValue().getOperationSummary().getOutMsg())
                .isEqualTo("Échec de l'audit de l'existence et de l'intégrité des objets Detail=  OK:91 KO:1");
        assertThat(reportInfosCaptor.getValue().getReportSummary()).isNotNull();
        assertThat(reportInfosCaptor.getValue().getReportSummary().getExtendedInfo()).isEmpty();
        assertThat(reportInfosCaptor.getValue().getReportSummary().getEvStartDateTime())
                .isEqualTo("2019-04-02T08:29:27.567");
        assertThat(reportInfosCaptor.getValue().getReportSummary().getReportType()).isEqualTo(ReportType.AUDIT);

    }

    @RunWithCustomExecutor
    @Test
    public void should_fatal_when_logbook_exception() throws Exception {

        // Given
        HandlerIO handler = mock(HandlerIO.class);
        VitamThreadUtils.getVitamSession().setTenantId(0);
        VitamThreadUtils.getVitamSession().setRequestId(PROCESS_ID_OK);
        WorkerParameters workerParameters = WorkerParametersFactory.newWorkerParameters()
                .setWorkerGUID(GUIDFactory.newGUID())
                .setContainerName(VitamThreadUtils.getVitamSession().getRequestId())
                .setRequestId(VitamThreadUtils.getVitamSession().getRequestId())
                .setProcessId(VitamThreadUtils.getVitamSession().getRequestId()).setObjectId("0")
                .setCurrentStep("StepName");
        workerParameters.putParameterValue(WorkerParameterName.auditActions, "AUDIT_FILE_EXISTING");
        workerParameters.putParameterValue(WorkerParameterName.auditType, "tenant");
        Mockito.doNothing().when(auditReportService).storeReport(reportInfosCaptor.capture());
        when(logbookClient.selectOperationById(any())).thenThrow(new LogbookClientException("Logbook exception"));

        // When
        ItemStatus response = auditFinalizePlugin.execute(workerParameters, handler);

        // Then
        assertEquals(StatusCode.FATAL, response.getGlobalStatus());
    }

    @RunWithCustomExecutor
    @Test
    public void should_fatal_when_report_exception() throws Exception {

        // Given
        HandlerIO handler = mock(HandlerIO.class);
        VitamThreadUtils.getVitamSession().setTenantId(0);
        VitamThreadUtils.getVitamSession().setRequestId(PROCESS_ID_OK);
        WorkerParameters workerParameters = WorkerParametersFactory.newWorkerParameters()
                .setWorkerGUID(GUIDFactory.newGUID())
                .setContainerName(VitamThreadUtils.getVitamSession().getRequestId())
                .setRequestId(VitamThreadUtils.getVitamSession().getRequestId())
                .setProcessId(VitamThreadUtils.getVitamSession().getRequestId()).setObjectId("0")
                .setCurrentStep("StepName");
        workerParameters.putParameterValue(WorkerParameterName.auditActions, "AUDIT_FILE_EXISTING");
        workerParameters.putParameterValue(WorkerParameterName.auditType, "tenant");
        Mockito.doThrow(new AuditException(StatusCode.FATAL, "audit report exception")).when(auditReportService)
                .storeReport(reportInfosCaptor.capture());
        when(logbookClient.selectOperationById(any()))
                .thenReturn(getFromInputStream(getClass().getResourceAsStream(JOP_RESULTS_OK)));

        // When
        ItemStatus response = auditFinalizePlugin.execute(workerParameters, handler);

        // Then
        assertEquals(StatusCode.FATAL, response.getGlobalStatus());
    }

}
