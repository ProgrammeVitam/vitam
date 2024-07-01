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

import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.plugin.evidence.report.EvidenceAuditReportService;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static fr.gouv.vitam.worker.core.plugin.CommonReportService.WORKSPACE_REPORT_URI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class EvidenceAuditFinalizeTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @InjectMocks
    private EvidenceAuditFinalize evidenceAuditFinalize;

    @Mock
    public HandlerIO handlerIO;

    @Mock
    WorkerParameters defaultWorkerParameters;

    @Mock
    private EvidenceAuditReportService evidenceAuditReportService;

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @RunWithCustomExecutor
    @Test
    public void shouldFinalizeAuditWhenReportFound() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(0);
        String containerName = GUIDFactory.newGUID().getId();
        VitamThreadUtils.getVitamSession().setRequestId(containerName);
        doReturn(containerName).when(defaultWorkerParameters).getContainerName();
        doReturn(true).when(handlerIO).isExistingFileInWorkspace(WORKSPACE_REPORT_URI);

        // When
        ItemStatus itemStatus = evidenceAuditFinalize.execute(defaultWorkerParameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
        assertThat(itemStatus.getEvDetailData()).isEqualTo("{}");
        verify(evidenceAuditReportService).storeReportToOffers(containerName);
        verify(evidenceAuditReportService).cleanupReport(containerName);
    }

    @RunWithCustomExecutor
    @Test
    public void shouldFinalizeAuditWhenReportNotFound() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(0);
        String containerName = GUIDFactory.newGUID().getId();
        VitamThreadUtils.getVitamSession().setRequestId(containerName);
        doReturn(containerName).when(defaultWorkerParameters).getContainerName();
        doReturn(false).when(handlerIO).isExistingFileInWorkspace(WORKSPACE_REPORT_URI);

        // When
        ItemStatus itemStatus = evidenceAuditFinalize.execute(defaultWorkerParameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.WARNING);
        assertThat(itemStatus.getEvDetailData()).contains("No report generated");
        verify(evidenceAuditReportService, never()).storeReportToOffers(containerName);
        verify(evidenceAuditReportService).cleanupReport(containerName);
    }
}
