package fr.gouv.vitam.worker.core.plugin.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.util.List;

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

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.batch.report.model.entry.AuditObjectGroupReportEntry;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameterName;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.plugin.audit.exception.AuditException;
import fr.gouv.vitam.worker.core.plugin.audit.model.AuditCheckObjectGroupResult;
import fr.gouv.vitam.worker.core.plugin.audit.model.AuditCheckObjectResult;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

public class AuditCheckObjectPluginTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
            VitamThreadPoolExecutor.getDefaultExecutor());

    @Mock
    private AuditExistenceService auditExistenceService;
    @Mock
    private AuditIntegrityService auditIntegrityService;
    @Mock
    private AuditReportService auditReportService;

    @Captor
    private ArgumentCaptor<List<AuditObjectGroupReportEntry>> auditReportEntryCaptor;
    @Captor
    private ArgumentCaptor<String> processIdCaptor;

    private AuditCheckObjectPlugin auditCheckObjectPlugin;

    @Before
    public void setUp() throws Exception {
        reset(auditIntegrityService);
        reset(auditExistenceService);
        reset(auditReportService);
        auditCheckObjectPlugin = new AuditCheckObjectPlugin(auditExistenceService, auditIntegrityService,
                auditReportService);
    }

    @RunWithCustomExecutor
    @Test
    public void shouldCheckExistenceObjectsOfObjectGroup()
            throws InvalidParseOperationException, ContentAddressableStorageServerException, ProcessingException,
            VitamClientInternalException, AuditException {

        // Given
        HandlerIO handler = mock(HandlerIO.class);
        VitamThreadUtils.getVitamSession().setTenantId(0);
        VitamThreadUtils.getVitamSession().setRequestId("opId");
        JsonNode jsonl = JsonHandler
                .getFromInputStream(getClass().getResourceAsStream("/AuditObjectWorkflow/objectGroup_1.json"));
        WorkerParameters params = WorkerParametersFactory.newWorkerParameters().setWorkerGUID(GUIDFactory.newGUID())
                .setContainerName(VitamThreadUtils.getVitamSession().getRequestId())
                .setRequestId(VitamThreadUtils.getVitamSession().getRequestId())
                .setProcessId(VitamThreadUtils.getVitamSession().getRequestId())
                .setObjectName("aebaaaaaaahgotryaauzialjp5zkhgyaaaaq").setObjectMetadata(jsonl.get("params"))
                .setCurrentStep("StepName");
        params.putParameterValue(WorkerParameterName.auditActions, "AUDIT_FILE_EXISTING");

        AuditCheckObjectGroupResult result = generateOkAuditResult();
        when(auditExistenceService.check(any())).thenReturn(result);
        Mockito.doNothing().when(auditReportService).appendAuditEntries(processIdCaptor.capture(),
                auditReportEntryCaptor.capture());

        // When
        ItemStatus status = auditCheckObjectPlugin.execute(params, handler);

        // Then
        assertThat(status.getGlobalStatus()).isEqualTo(StatusCode.OK);
        assertThat(status.getItemId()).isEqualTo(AuditCheckObjectPlugin.AUDIT_CHECK_OBJECT);
        assertThat(processIdCaptor.getValue()).isEqualTo("opId");
        assertThat(auditReportEntryCaptor.getValue().size()).isEqualTo(1);
        assertThat(auditReportEntryCaptor.getValue().get(0).getOutcome()).isEqualTo("AUDIT_FILE_EXISTING");
    }

    @RunWithCustomExecutor
    @Test
    public void shouldCheckIntegrityObjectsOfObjectGroup()
            throws InvalidParseOperationException, ContentAddressableStorageServerException, ProcessingException,
            VitamClientInternalException, AuditException {

        // Given
        HandlerIO handler = mock(HandlerIO.class);
        VitamThreadUtils.getVitamSession().setTenantId(0);
        VitamThreadUtils.getVitamSession().setRequestId("opId");
        JsonNode jsonl = JsonHandler
                .getFromInputStream(getClass().getResourceAsStream("/AuditObjectWorkflow/objectGroup_1.json"));
        WorkerParameters params = WorkerParametersFactory.newWorkerParameters().setWorkerGUID(GUIDFactory.newGUID())
                .setContainerName(VitamThreadUtils.getVitamSession().getRequestId())
                .setRequestId(VitamThreadUtils.getVitamSession().getRequestId())
                .setProcessId(VitamThreadUtils.getVitamSession().getRequestId())
                .setObjectName("aebaaaaaaahgotryaauzialjp5zkhgyaaaaq").setObjectMetadata(jsonl.get("params"))
                .setCurrentStep("StepName");
        params.putParameterValue(WorkerParameterName.auditActions, "AUDIT_FILE_INTEGRITY");

        AuditCheckObjectGroupResult result = generateOkAuditResult();

        when(auditIntegrityService.check(any())).thenReturn(result);
        Mockito.doNothing().when(auditReportService).appendAuditEntries(processIdCaptor.capture(),
                auditReportEntryCaptor.capture());

        // When
        ItemStatus status = auditCheckObjectPlugin.execute(params, handler);

        // Then
        assertThat(status.getGlobalStatus()).isEqualTo(StatusCode.OK);
        assertThat(status.getItemId()).isEqualTo(AuditCheckObjectPlugin.AUDIT_CHECK_OBJECT);
        assertThat(auditReportEntryCaptor.getValue().size()).isEqualTo(1);
        assertThat(auditReportEntryCaptor.getValue().get(0).getOutcome()).isEqualTo("AUDIT_FILE_INTEGRITY");
    }

    @RunWithCustomExecutor
    @Test
    public void shouldFatalWhenProcessingException()
            throws InvalidParseOperationException, ContentAddressableStorageServerException, ProcessingException,
            VitamClientInternalException, AuditException {

        // Given
        HandlerIO handler = mock(HandlerIO.class);
        VitamThreadUtils.getVitamSession().setTenantId(0);
        VitamThreadUtils.getVitamSession().setRequestId("opId");
        JsonNode jsonl = JsonHandler
                .getFromInputStream(getClass().getResourceAsStream("/AuditObjectWorkflow/objectGroup_1.json"));
        WorkerParameters params = WorkerParametersFactory.newWorkerParameters().setWorkerGUID(GUIDFactory.newGUID())
                .setContainerName(VitamThreadUtils.getVitamSession().getRequestId())
                .setRequestId(VitamThreadUtils.getVitamSession().getRequestId())
                .setProcessId(VitamThreadUtils.getVitamSession().getRequestId())
                .setObjectName("aebaaaaaaahgotryaauzialjp5zkhgyaaaaq").setObjectMetadata(jsonl.get("params"))
                .setCurrentStep("StepName");
        params.putParameterValue(WorkerParameterName.auditActions, "AUDIT_FILE_INTEGRITY");

        when(auditIntegrityService.check(any())).thenThrow(new AuditException(StatusCode.FATAL, "storage error"));

        // When
        ItemStatus status = auditCheckObjectPlugin.execute(params, handler);

        // Then
        assertThat(status.getGlobalStatus()).isEqualTo(StatusCode.FATAL);
        assertThat(status.getItemId()).isEqualTo(AuditCheckObjectPlugin.AUDIT_CHECK_OBJECT);
    }

    @RunWithCustomExecutor
    @Test
    public void shouldFatalWhenReportClientException()
            throws InvalidParseOperationException, ContentAddressableStorageServerException, ProcessingException,
            VitamClientInternalException, AuditException {

        // Given
        HandlerIO handler = mock(HandlerIO.class);
        VitamThreadUtils.getVitamSession().setTenantId(0);
        VitamThreadUtils.getVitamSession().setRequestId("opId");
        JsonNode jsonl = JsonHandler
                .getFromInputStream(getClass().getResourceAsStream("/AuditObjectWorkflow/objectGroup_1.json"));
        WorkerParameters params = WorkerParametersFactory.newWorkerParameters().setWorkerGUID(GUIDFactory.newGUID())
                .setContainerName(VitamThreadUtils.getVitamSession().getRequestId())
                .setRequestId(VitamThreadUtils.getVitamSession().getRequestId())
                .setProcessId(VitamThreadUtils.getVitamSession().getRequestId())
                .setObjectName("aebaaaaaaahgotryaauzialjp5zkhgyaaaaq").setObjectMetadata(jsonl.get("params"))
                .setCurrentStep("StepName");
        params.putParameterValue(WorkerParameterName.auditActions, "AUDIT_FILE_EXISTING");

        AuditCheckObjectGroupResult result = generateOkAuditResult();
        when(auditExistenceService.check(any())).thenReturn(result);
        Mockito.doThrow(new AuditException(StatusCode.FATAL, "report error")).when(auditReportService).appendAuditEntries(any(), any());

        // When
        ItemStatus status = auditCheckObjectPlugin.execute(params, handler);

        // Then
        assertThat(status.getGlobalStatus()).isEqualTo(StatusCode.FATAL);
        assertThat(status.getItemId()).isEqualTo(AuditCheckObjectPlugin.AUDIT_CHECK_OBJECT);
    }

    private AuditCheckObjectGroupResult generateOkAuditResult() {
        AuditCheckObjectGroupResult result = new AuditCheckObjectGroupResult();
        result.setIdObjectGroup("aebaaaaaaahgotryaauzialjp5zkhgyaaaaq");
        result.setStatus(StatusCode.OK);
        AuditCheckObjectResult objectResult = new AuditCheckObjectResult();
        objectResult.setIdObject("aeaaaaaaaahgotryaauzialjp5zkhgiaaaaq");
        objectResult.getOfferStatuses().put("offer-fs-1.service.int.consul", StatusCode.OK);
        objectResult.getOfferStatuses().put("offer-fs-2.service.int.consul", StatusCode.OK);
        result.getObjectStatuses().add(objectResult);
        return result;
    }

}
