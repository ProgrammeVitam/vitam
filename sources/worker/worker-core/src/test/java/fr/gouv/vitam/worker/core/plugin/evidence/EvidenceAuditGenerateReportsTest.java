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
package fr.gouv.vitam.worker.core.plugin.evidence;

import fr.gouv.vitam.batch.report.client.BatchReportClient;
import fr.gouv.vitam.batch.report.client.BatchReportClientFactory;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.plugin.evidence.exception.EvidenceStatus;
import fr.gouv.vitam.worker.core.plugin.evidence.report.EvidenceAuditReportLine;
import fr.gouv.vitam.worker.core.plugin.evidence.report.EvidenceAuditReportService;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

/**
 * EvidenceAuditGenerateReportsTest class
 */
public class EvidenceAuditGenerateReportsTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Mock
    public HandlerIO handlerIO;

    @Mock
    WorkerParameters defaultWorkerParameters;

    private EvidenceAuditGenerateReports evidenceAuditGenerateReports;

    @Mock
    private BatchReportClientFactory batchReportFactory;

    @Mock
    private WorkspaceClientFactory workspaceClientFactory;

    @Mock
    private StorageClientFactory storageClientFactory;

    @Mock
    private EvidenceAuditReportService evidenceAuditReportService;

    @Mock
    private BatchReportClient batchReportClient;

    @Mock
    private WorkspaceClient workspaceClient;

    @Mock
    private StorageClient storageClient;

    private String processId;

    @Before
    public void setUp() throws Exception {
        given(batchReportFactory.getClient()).willReturn(batchReportClient);
        given(workspaceClientFactory.getClient()).willReturn(workspaceClient);
        given(storageClientFactory.getClient()).willReturn(storageClient);

        evidenceAuditReportService = new EvidenceAuditReportService(
            batchReportFactory,
            workspaceClientFactory,
            storageClientFactory
        );

        evidenceAuditGenerateReports = new EvidenceAuditGenerateReports(evidenceAuditReportService);

        processId = "aeaqaaaaaaebta56aaoc4alcdk4hlcqaaaaq";
    }

    @Test
    public void should_generate_reports_when_line_not_found() throws Exception {
        when(defaultWorkerParameters.getObjectName()).thenReturn("test");
        when(defaultWorkerParameters.getContainerName()).thenReturn(processId);
        File resourceFile = PropertiesUtils.getResourceFile("evidenceAudit/data.txt");
        when(handlerIO.getFileFromWorkspace("zip/test")).thenReturn(resourceFile);
        File file2 = tempFolder.newFile();
        File report = tempFolder.newFile();
        JsonHandler.writeAsFile("aeaqaaaaaaebta56aaoc4alcdk4hlcqaaaaq", file2);
        when(handlerIO.getFileFromWorkspace("fileNames/test")).thenReturn(file2);
        when(handlerIO.getFileFromWorkspace("data/aeaqaaaaaaebta56aaoc4alcdk4hlcqaaaaq")).thenReturn(
            PropertiesUtils.getResourceFile("evidenceAudit/test.json")
        );
        when(handlerIO.getNewLocalFile("aeaqaaaaaaebta56aaoc4alcdk4hlcqaaaaq")).thenReturn(report);
        given(handlerIO.getJsonFromWorkspace("evidenceOptions")).willReturn(
            JsonHandler.createObjectNode().put("correctiveOption", false)
        );

        ItemStatus execute = evidenceAuditGenerateReports.execute(defaultWorkerParameters, handlerIO);

        assertThat(execute.getGlobalStatus()).isEqualTo(StatusCode.OK);

        EvidenceAuditReportLine evidenceAuditReportLine = JsonHandler.getFromFile(
            report,
            EvidenceAuditReportLine.class
        );
        assertThat(evidenceAuditReportLine.getIdentifier()).isEqualTo("aeaqaaaaaaebta56aaoc4alcdk4hlcqaaaaq");
        assertThat(evidenceAuditReportLine.getEvidenceStatus()).isEqualTo(EvidenceStatus.KO);
        assertThat(evidenceAuditReportLine.getMessage()).isEqualTo(
            "Could not find matching traceability info in the file"
        );
    }

    @Test
    public void should_generate_reports_when_digest_invalid() throws Exception {
        when(defaultWorkerParameters.getObjectName()).thenReturn("test");
        File resourceFile = PropertiesUtils.getResourceFile("evidenceAudit/data_ko_unit.txt");
        when(handlerIO.getFileFromWorkspace("zip/test")).thenReturn(resourceFile);
        File file2 = tempFolder.newFile();
        File report = tempFolder.newFile();
        JsonHandler.writeAsFile("aeaqaaaaaaebta56aaoc4alcdk4hlcqaaaaq", file2);
        when(handlerIO.getFileFromWorkspace("fileNames/test")).thenReturn(file2);
        when(handlerIO.getFileFromWorkspace("data/aeaqaaaaaaebta56aaoc4alcdk4hlcqaaaaq")).thenReturn(
            PropertiesUtils.getResourceFile("evidenceAudit/test.json")
        );
        when(handlerIO.getNewLocalFile("aeaqaaaaaaebta56aaoc4alcdk4hlcqaaaaq")).thenReturn(report);
        given(handlerIO.getJsonFromWorkspace("evidenceOptions")).willReturn(
            JsonHandler.createObjectNode().put("correctiveOption", false)
        );

        ItemStatus execute = evidenceAuditGenerateReports.execute(defaultWorkerParameters, handlerIO);

        assertThat(execute.getGlobalStatus()).isEqualTo(StatusCode.OK);

        EvidenceAuditReportLine evidenceAuditReportLine = JsonHandler.getFromFile(
            report,
            EvidenceAuditReportLine.class
        );
        assertThat(evidenceAuditReportLine.getIdentifier()).isEqualTo("aeaqaaaaaaebta56aaoc4alcdk4hlcqaaaaq");
        assertThat(evidenceAuditReportLine.getEvidenceStatus()).isEqualTo(EvidenceStatus.KO);
        assertThat(evidenceAuditReportLine.getMessage()).contains("Traceability audit KO  Database check failure");
        assertThat(evidenceAuditReportLine.getStrategyId()).contains("default");
    }
}
