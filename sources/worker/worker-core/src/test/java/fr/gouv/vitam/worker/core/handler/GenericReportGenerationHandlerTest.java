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

package fr.gouv.vitam.worker.core.handler;

import fr.gouv.vitam.batch.report.model.Report;
import fr.gouv.vitam.batch.report.model.ReportResults;
import fr.gouv.vitam.batch.report.model.ReportSummary;
import fr.gouv.vitam.batch.report.model.ReportType;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.logbook.LogbookEventOperation;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.exception.ProcessingStatusException;
import fr.gouv.vitam.worker.core.plugin.CommonReportService;
import fr.gouv.vitam.worker.core.plugin.GenericReportGenerationHandler;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.annotation.Nonnull;
import java.util.Arrays;

import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.common.model.VitamConstants.DETAILS;
import static fr.gouv.vitam.worker.core.plugin.CommonReportService.WORKSPACE_REPORT_URI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GenericReportGenerationHandlerTest {

    private static final String OPERATION_ID = "MY_OPERATION_ID";
    private static final String ACTION_KEY = "ACTION_KEY";
    private static final String PLUGIN_ID = "PLUGIN_ID";

    private CommonReportService<?> reportService;
    private GenericReportGenerationHandler genericReportGenerationHandler;
    private HandlerIO handler;
    private WorkerParameters workerParameters;

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Before
    public void setup() {
        handler = mock(HandlerIO.class);
        workerParameters = mock(WorkerParameters.class);

        when(workerParameters.getContainerName()).thenReturn(OPERATION_ID);

        reportService = mock(CommonReportService.class);
    }

    @Test
    @RunWithCustomExecutor
    public void should_generate_report_OK() throws Exception {
        // When
        genericReportGenerationHandler = getGenericReportGenerationHandler(3, 0, 0);

        ArgumentCaptor<Report> reportCaptor = ArgumentCaptor.forClass(Report.class);
        doNothing().when(reportService).storeReportToWorkspace(reportCaptor.capture());

        ItemStatus itemStatus = genericReportGenerationHandler.execute(workerParameters, handler);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(OK);
        assertNotNull(reportCaptor.getValue());
        Report report = reportCaptor.getValue();

        assertThat(report.getOperationSummary().getEvId()).isEqualTo(OPERATION_ID);
        assertThat(report.getReportSummary().getReportType()).isEqualTo(ReportType.ELIMINATION_ACTION);
        assertThat(report.getReportSummary().getVitamResults().getNbOk()).isEqualTo(3);
        assertThat(report.getReportSummary().getVitamResults().getNbWarning()).isEqualTo(0);
        assertThat(report.getReportSummary().getVitamResults().getNbKo()).isEqualTo(0);

        assertThat(report.getContext().toString()).isEqualTo("{}");

        verify(reportService).cleanupReport(OPERATION_ID);
    }

    @Test
    @RunWithCustomExecutor
    public void should_generate_report_WARNING() throws Exception {
        // When
        genericReportGenerationHandler = getGenericReportGenerationHandler(1, 2, 1);

        ArgumentCaptor<Report> reportCaptor = ArgumentCaptor.forClass(Report.class);
        doNothing().when(reportService).storeReportToWorkspace(reportCaptor.capture());

        ItemStatus itemStatus = genericReportGenerationHandler.execute(workerParameters, handler);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(OK);
        assertNotNull(reportCaptor.getValue());
        Report report = reportCaptor.getValue();

        assertThat(report.getOperationSummary().getEvId()).isEqualTo(OPERATION_ID);
        assertThat(report.getReportSummary().getReportType()).isEqualTo(ReportType.ELIMINATION_ACTION);
        assertThat(report.getReportSummary().getVitamResults().getNbOk()).isEqualTo(1);
        assertThat(report.getReportSummary().getVitamResults().getNbWarning()).isEqualTo(2);
        assertThat(report.getReportSummary().getVitamResults().getNbKo()).isEqualTo(1);

        assertThat(report.getContext().toString()).isEqualTo("{}");
    }

    @Test
    @RunWithCustomExecutor
    public void should_not_regenerate_report_when_already_exists_in_workspace() throws Exception {
        // Given
        genericReportGenerationHandler = getGenericReportGenerationHandler(10, 0, 0);

        Report report = new Report();
        ReportSummary reportSummary = new ReportSummary(
            "",
            "",
            ReportType.AUDIT,
            new ReportResults(10, 0, 0),
            JsonHandler.createObjectNode()
        );
        report.setReportSummary(reportSummary);
        when(handler.getJsonFromWorkspace(eq(WORKSPACE_REPORT_URI))).thenReturn(JsonHandler.toJsonNode(report));

        when(reportService.isReportWrittenInWorkspace(eq(OPERATION_ID))).thenReturn(true);

        // When
        genericReportGenerationHandler.execute(workerParameters, handler);

        // Then
        verify(reportService, never()).storeReportToWorkspace(any());
    }

    @Test
    @RunWithCustomExecutor
    public void should_generate_report_FATAL() throws Exception {
        // Given
        genericReportGenerationHandler = getGenericReportGenerationHandler(1, 0, 0);

        doThrow(new ProcessingStatusException(StatusCode.FATAL, "Client error cause FATAL."))
            .when(reportService)
            .storeReportToWorkspace(any());
        // When
        ItemStatus itemStatus = genericReportGenerationHandler.execute(workerParameters, handler);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.FATAL);
    }

    @Nonnull
    private GenericReportGenerationHandler getGenericReportGenerationHandler(
        int numberOfOK,
        int numberOfWarning,
        int numberOfKO
    ) {
        return new GenericReportGenerationHandler(reportService) {
            @Override
            protected ReportType getReportType() {
                return ReportType.ELIMINATION_ACTION;
            }

            @Override
            protected String getLogbookActionKey() {
                return ACTION_KEY;
            }

            @Override
            protected String getPluginId() {
                return PLUGIN_ID;
            }

            @Override
            protected LogbookOperation getLogbookInformation(WorkerParameters param) {
                return getLogbookOperation(numberOfOK, numberOfWarning, numberOfKO);
            }
        };
    }

    @Nonnull
    private LogbookOperation getLogbookOperation(int numberOfOK, int numberOfWarning, int numberOfKO) {
        LogbookOperation operation = new LogbookOperation();
        LogbookEventOperation logbookEventOperation = new LogbookEventOperation();
        logbookEventOperation.setEvDetData(
            JsonHandler.unprettyPrint(JsonHandler.createObjectNode().put("data", "data"))
        );
        logbookEventOperation.setEvType(ACTION_KEY);
        logbookEventOperation.setOutMessg(
            "My awesome message" + DETAILS + "OK:" + numberOfOK + " WARNING:" + numberOfWarning + " KO:" + numberOfKO
        );
        LogbookEventOperation logbookEventOperation1 = new LogbookEventOperation();
        logbookEventOperation1.setEvType("EVENT_TYPE");
        operation.setEvents(Arrays.asList(logbookEventOperation1, logbookEventOperation, logbookEventOperation1));
        operation.setRightsStatementIdentifier(
            JsonHandler.unprettyPrint(JsonHandler.createObjectNode().put("identifier", "identifier"))
        );
        return operation;
    }
}
