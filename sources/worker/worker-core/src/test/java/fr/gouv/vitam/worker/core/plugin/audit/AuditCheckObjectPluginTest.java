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
package fr.gouv.vitam.worker.core.plugin.audit;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.batch.report.model.entry.AuditObjectGroupReportEntry;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.processing.common.parameter.WorkerParameterName;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.exception.ProcessingStatusException;
import fr.gouv.vitam.worker.core.plugin.audit.model.AuditCheckObjectGroupResult;
import fr.gouv.vitam.worker.core.plugin.audit.model.AuditCheckObjectResult;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class AuditCheckObjectPluginTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

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
        auditCheckObjectPlugin = new AuditCheckObjectPlugin(
            auditExistenceService,
            auditIntegrityService,
            auditReportService
        );
    }

    @RunWithCustomExecutor
    @Test
    public void shouldCheckExistenceObjectsOfObjectGroup() throws Exception {
        // Given
        HandlerIO handler = mock(HandlerIO.class);
        VitamThreadUtils.getVitamSession().setTenantId(0);
        VitamThreadUtils.getVitamSession().setRequestId("opId");
        JsonNode jsonl = JsonHandler.getFromInputStream(
            getClass().getResourceAsStream("/AuditObjectWorkflow/objectGroup_1.json")
        );
        WorkerParameters params = WorkerParametersFactory.newWorkerParameters()
            .setWorkerGUID(GUIDFactory.newGUID().getId())
            .setContainerName(VitamThreadUtils.getVitamSession().getRequestId())
            .setRequestId(VitamThreadUtils.getVitamSession().getRequestId())
            .setProcessId(VitamThreadUtils.getVitamSession().getRequestId())
            .setObjectName("aebaaaaaaahgotryaauzialjp5zkhgyaaaaq")
            .setObjectMetadata(jsonl.get("params"))
            .setCurrentStep("StepName");
        params.putParameterValue(WorkerParameterName.auditActions, "AUDIT_FILE_EXISTING");

        AuditCheckObjectGroupResult result = generateOkAuditResult();
        when(auditExistenceService.check(any(), any())).thenReturn(result);
        doNothing().when(auditReportService).appendEntries(processIdCaptor.capture(), auditReportEntryCaptor.capture());
        when(handler.getInput(0)).thenReturn(PropertiesUtils.getResourceFile("AuditObjectWorkflow/strategies.json"));

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
    public void shouldCheckIntegrityObjectsOfObjectGroup() throws Exception {
        // Given
        HandlerIO handler = mock(HandlerIO.class);
        VitamThreadUtils.getVitamSession().setTenantId(0);
        VitamThreadUtils.getVitamSession().setRequestId("opId");
        JsonNode jsonl = JsonHandler.getFromInputStream(
            getClass().getResourceAsStream("/AuditObjectWorkflow/objectGroup_1.json")
        );
        WorkerParameters params = WorkerParametersFactory.newWorkerParameters()
            .setWorkerGUID(GUIDFactory.newGUID().getId())
            .setContainerName(VitamThreadUtils.getVitamSession().getRequestId())
            .setRequestId(VitamThreadUtils.getVitamSession().getRequestId())
            .setProcessId(VitamThreadUtils.getVitamSession().getRequestId())
            .setObjectName("aebaaaaaaahgotryaauzialjp5zkhgyaaaaq")
            .setObjectMetadata(jsonl.get("params"))
            .setCurrentStep("StepName");
        params.putParameterValue(WorkerParameterName.auditActions, "AUDIT_FILE_INTEGRITY");

        AuditCheckObjectGroupResult result = generateOkAuditResult();

        when(auditIntegrityService.check(any(), any())).thenReturn(result);
        doNothing().when(auditReportService).appendEntries(processIdCaptor.capture(), auditReportEntryCaptor.capture());
        when(handler.getInput(0)).thenReturn(PropertiesUtils.getResourceFile("AuditObjectWorkflow/strategies.json"));

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
    public void shouldFatalWhenProcessingException() throws Exception {
        // Given
        HandlerIO handler = mock(HandlerIO.class);
        VitamThreadUtils.getVitamSession().setTenantId(0);
        VitamThreadUtils.getVitamSession().setRequestId("opId");
        JsonNode jsonl = JsonHandler.getFromInputStream(
            getClass().getResourceAsStream("/AuditObjectWorkflow/objectGroup_1.json")
        );
        WorkerParameters params = WorkerParametersFactory.newWorkerParameters()
            .setWorkerGUID(GUIDFactory.newGUID().getId())
            .setContainerName(VitamThreadUtils.getVitamSession().getRequestId())
            .setRequestId(VitamThreadUtils.getVitamSession().getRequestId())
            .setProcessId(VitamThreadUtils.getVitamSession().getRequestId())
            .setObjectName("aebaaaaaaahgotryaauzialjp5zkhgyaaaaq")
            .setObjectMetadata(jsonl.get("params"))
            .setCurrentStep("StepName");
        params.putParameterValue(WorkerParameterName.auditActions, "AUDIT_FILE_INTEGRITY");

        when(auditIntegrityService.check(any(), any())).thenThrow(
            new ProcessingStatusException(StatusCode.FATAL, "storage error")
        );

        // When
        ItemStatus status = auditCheckObjectPlugin.execute(params, handler);

        // Then
        assertThat(status.getGlobalStatus()).isEqualTo(StatusCode.FATAL);
        assertThat(status.getItemId()).isEqualTo(AuditCheckObjectPlugin.AUDIT_CHECK_OBJECT);
    }

    @RunWithCustomExecutor
    @Test
    public void shouldFatalWhenReportClientException() throws Exception {
        // Given
        HandlerIO handler = mock(HandlerIO.class);
        VitamThreadUtils.getVitamSession().setTenantId(0);
        VitamThreadUtils.getVitamSession().setRequestId("opId");
        JsonNode jsonl = JsonHandler.getFromInputStream(
            getClass().getResourceAsStream("/AuditObjectWorkflow/objectGroup_1.json")
        );
        WorkerParameters params = WorkerParametersFactory.newWorkerParameters()
            .setWorkerGUID(GUIDFactory.newGUID().getId())
            .setContainerName(VitamThreadUtils.getVitamSession().getRequestId())
            .setRequestId(VitamThreadUtils.getVitamSession().getRequestId())
            .setProcessId(VitamThreadUtils.getVitamSession().getRequestId())
            .setObjectName("aebaaaaaaahgotryaauzialjp5zkhgyaaaaq")
            .setObjectMetadata(jsonl.get("params"))
            .setCurrentStep("StepName");
        params.putParameterValue(WorkerParameterName.auditActions, "AUDIT_FILE_EXISTING");

        AuditCheckObjectGroupResult result = generateOkAuditResult();
        when(auditExistenceService.check(any(), any())).thenReturn(result);
        Mockito.doThrow(new ProcessingStatusException(StatusCode.FATAL, "report error"))
            .when(auditReportService)
            .appendEntries(any(), any());

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
