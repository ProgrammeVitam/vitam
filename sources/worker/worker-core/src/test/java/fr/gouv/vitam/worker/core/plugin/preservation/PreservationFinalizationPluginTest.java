/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
package fr.gouv.vitam.worker.core.plugin.preservation;

import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.plugin.preservation.service.PreservationReportService;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class PreservationFinalizationPluginTest {

    public static final String CONTAINER = "CONTAINER";
    private PreservationFinalizationPlugin plugin;

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private PreservationReportService preservationReportService;

    @Before
    public void setUp() throws Exception {
        plugin = new PreservationFinalizationPlugin(preservationReportService);
    }

    @Test
    public void should_finalize_preservation_report() throws Exception {
        // Given
        WorkerParameters parameter = mock(WorkerParameters.class);
        HandlerIO handler = mock(HandlerIO.class);
        doReturn(CONTAINER).when(handler).getContainerName();
        doReturn(CONTAINER).when(parameter).getContainerName();
        doReturn(CONTAINER).when(parameter).getRequestId();
        doReturn(false).when(preservationReportService).isReportWrittenInWorkspace(CONTAINER);

        // When
        ItemStatus itemStatus = plugin.execute(parameter, handler);
        // Then
        Assertions.assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
        Assertions.assertThat(itemStatus.getItemId()).isEqualTo("PRESERVATION_FINALIZATION");
        verify(preservationReportService).isReportWrittenInWorkspace(CONTAINER);
        verify(preservationReportService).storeReportToWorkspace(CONTAINER, CONTAINER);
        verify(preservationReportService).storeReportToOffers(CONTAINER);
        verify(preservationReportService).cleanupReport(CONTAINER);
    }

    @Test
    public void should_finalize_preservation_report_with_existing_report() throws Exception {
        // Given
        WorkerParameters parameter = mock(WorkerParameters.class);
        HandlerIO handler = mock(HandlerIO.class);
        doReturn(CONTAINER).when(handler).getContainerName();
        doReturn(CONTAINER).when(parameter).getContainerName();
        doReturn(CONTAINER).when(parameter).getRequestId();
        doReturn(true).when(preservationReportService).isReportWrittenInWorkspace(CONTAINER);

        // When
        ItemStatus itemStatus = plugin.execute(parameter, handler);
        // Then
        Assertions.assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
        Assertions.assertThat(itemStatus.getItemId()).isEqualTo("PRESERVATION_FINALIZATION");
        verify(preservationReportService).isReportWrittenInWorkspace(CONTAINER);
        verify(preservationReportService, never()).storeReportToWorkspace(CONTAINER, CONTAINER);
        verify(preservationReportService).storeReportToOffers(CONTAINER);
        verify(preservationReportService).cleanupReport(CONTAINER);
    }
}
