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


import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.plugin.evidence.report.EvidenceAuditReportService;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;

import static fr.gouv.vitam.common.json.JsonHandler.getFromInputStream;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class EvidenceAuditPrepareReportTest {
    @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule public TemporaryFolder tempFolder = new TemporaryFolder();
    private EvidenceAuditPrepareReport evidenceAuditPrepareReport;
    private static final String JOP_EVIDENCE_AUDIT_RESULTS_OK = "/evidenceAudit/JOP_EVIDENCE_AUDIT_OK.json";
    private static final String JOP_EVIDENCE_AUDIT_RESULTS_WARNING = "/evidenceAudit/JOP_EVIDENCE_AUDIT_WARNING.json";
    @Mock public HandlerIO handlerIO;
    @Mock
    WorkerParameters defaultWorkerParameters;
    @Mock
    private EvidenceAuditReportService evidenceAuditReportService;
    @Mock
    private LogbookOperationsClientFactory logbookOperationsClientFactory;
    @Mock
    private LogbookOperationsClient logbookClient;
    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @Before
    public void setUp() throws Exception {
        when(logbookOperationsClientFactory.getClient()).thenReturn(logbookClient);
        evidenceAuditPrepareReport =
            new EvidenceAuditPrepareReport(evidenceAuditReportService, logbookOperationsClientFactory);
    }


    @RunWithCustomExecutor
    @Test
    public void shouldFinalizeAuditWithOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        VitamThreadUtils.getVitamSession().setRequestId("aeeaaaaaacfpuagsaauscallwn433oaaaaaq");
        File file = tempFolder.newFile();
        when(handlerIO.getNewLocalFile("report.json")).thenReturn(file);
        when(logbookClient.selectOperationById(any()))
            .thenReturn(getFromInputStream(getClass().getResourceAsStream(JOP_EVIDENCE_AUDIT_RESULTS_OK)));

        ItemStatus execute = evidenceAuditPrepareReport.execute(defaultWorkerParameters, handlerIO);
        Assertions.assertThat(execute.getGlobalStatus()).isEqualTo(StatusCode.OK);
    }

    @RunWithCustomExecutor
    @Test
    public void shouldFinalizeAuditWithWarning() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        VitamThreadUtils.getVitamSession().setRequestId("aeeaaaaaacfpuagsaauscallwn434oaaaaaq");
        File file = tempFolder.newFile();
        when(handlerIO.getNewLocalFile("report.json")).thenReturn(file);
        when(logbookClient.selectOperationById(any()))
            .thenReturn(getFromInputStream(getClass().getResourceAsStream(JOP_EVIDENCE_AUDIT_RESULTS_WARNING)));

        ItemStatus execute = evidenceAuditPrepareReport.execute(defaultWorkerParameters, handlerIO);
        Assertions.assertThat(execute.getGlobalStatus()).isEqualTo(StatusCode.OK);
    }
}
