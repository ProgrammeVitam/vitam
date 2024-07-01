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

package fr.gouv.vitam.worker.core.plugin.deleteGotVersions;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.batch.report.client.BatchReportClient;
import fr.gouv.vitam.batch.report.client.BatchReportClientFactory;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.DeleteGotVersionsRequest;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.plugin.deleteGotVersions.handlers.DeleteGotVersionsFinalizationPlugin;
import fr.gouv.vitam.worker.core.plugin.deleteGotVersions.services.DeleteGotVersionsReportService;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.util.List;

import static fr.gouv.vitam.common.json.JsonHandler.createArrayNode;
import static fr.gouv.vitam.common.json.JsonHandler.getFromFile;
import static fr.gouv.vitam.common.json.JsonHandler.toJsonNode;
import static fr.gouv.vitam.common.model.StatusCode.FATAL;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.common.model.administration.DataObjectVersionType.BINARY_MASTER;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DeleteGotVersionsFinalizationPluginTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @InjectMocks
    private DeleteGotVersionsFinalizationPlugin deleteGotVersionsFinalizationPlugin;

    @Mock
    private LogbookOperationsClientFactory logbookOperationsClientFactory;

    @Mock
    private LogbookOperationsClient logbookOperationsClient;

    @Mock
    private BatchReportClientFactory batchReportClientFactory;

    @Mock
    private BatchReportClient batchReportClient;

    @Mock
    private HandlerIO handlerIO;

    @Mock
    private WorkerParameters params;

    @Mock
    private DeleteGotVersionsReportService deleteGotVersionsReportService;

    private static final String LOGBOOK_OPERATION_MODEL = "deleteGotVersions/logbookOperationModel.json";
    private static final String DELETE_GOT_VERSIONS_REPORTS_OK_FILE =
        "deleteGotVersions/deleteGotVersionsReportOk.json";

    @Before
    public void setUp() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        when(logbookOperationsClientFactory.getClient()).thenReturn(logbookOperationsClient);
        when(batchReportClientFactory.getClient()).thenReturn(batchReportClient);
        deleteGotVersionsFinalizationPlugin = new DeleteGotVersionsFinalizationPlugin(
            deleteGotVersionsReportService,
            logbookOperationsClient
        );
    }

    @Test
    @RunWithCustomExecutor
    public void givenReportsThenDeleteGotVersionsFinalizationOk() throws Exception {
        File deleteGotVersionsFile = PropertiesUtils.getResourceFile(DELETE_GOT_VERSIONS_REPORTS_OK_FILE);
        when(batchReportClient.readComputedDetailsFromReport(any(), any())).thenReturn(
            getFromFile(deleteGotVersionsFile)
        );
        JsonNode logbookOperation = JsonHandler.getFromFileAsTypeReference(
            PropertiesUtils.getResourceFile(LOGBOOK_OPERATION_MODEL),
            new TypeReference<>() {}
        );
        when(logbookOperationsClient.selectOperationById(any())).thenReturn(logbookOperation);

        DeleteGotVersionsRequest deleteGotVersionsRequest = new DeleteGotVersionsRequest(
            new Select().getFinalSelect(),
            BINARY_MASTER.getName(),
            List.of(2)
        );
        when(handlerIO.getInputStreamFromWorkspace("deleteGotVersionsRequest")).thenReturn(
            IOUtils.toInputStream(toJsonNode(deleteGotVersionsRequest).toString(), "UTF-8")
        );

        ItemStatus itemStatus = deleteGotVersionsFinalizationPlugin.execute(params, handlerIO);

        assertEquals(OK, itemStatus.getGlobalStatus());
        verify(deleteGotVersionsReportService, times(1)).storeReportToWorkspace(any());
    }

    @Test
    @RunWithCustomExecutor
    public void givenInexistantReportThenDeleteGotVersionsFinalizationOk() throws Exception {
        when(batchReportClient.readComputedDetailsFromReport(any(), any())).thenReturn(createArrayNode());
        JsonNode logbookOperation = JsonHandler.getFromFileAsTypeReference(
            PropertiesUtils.getResourceFile(LOGBOOK_OPERATION_MODEL),
            new TypeReference<>() {}
        );
        when(logbookOperationsClient.selectOperationById(any())).thenReturn(logbookOperation);
        DeleteGotVersionsRequest deleteGotVersionsRequest = new DeleteGotVersionsRequest(
            new Select().getFinalSelect(),
            "UsageNameTest",
            List.of(1, 2)
        );
        when(handlerIO.getInputStreamFromWorkspace("deleteGotVersionsRequest")).thenReturn(
            IOUtils.toInputStream(toJsonNode(deleteGotVersionsRequest).toString(), "UTF-8")
        );

        ItemStatus itemStatus = deleteGotVersionsFinalizationPlugin.execute(params, handlerIO);

        assertEquals(OK, itemStatus.getGlobalStatus());
        verify(deleteGotVersionsReportService, times(1)).storeReportToWorkspace(any());
    }

    @Test
    @RunWithCustomExecutor
    public void givenUnavailableReportThenDeleteGotVersionsFinalizationFATAL() throws Exception {
        when(batchReportClient.readComputedDetailsFromReport(any(), any())).thenReturn(null);
        JsonNode logbookOperation = JsonHandler.getFromFileAsTypeReference(
            PropertiesUtils.getResourceFile(LOGBOOK_OPERATION_MODEL),
            new TypeReference<>() {}
        );
        when(logbookOperationsClient.selectOperationById(any())).thenReturn(logbookOperation);

        ItemStatus itemStatus = deleteGotVersionsFinalizationPlugin.execute(params, handlerIO);

        assertEquals(FATAL, itemStatus.getGlobalStatus());
        verify(deleteGotVersionsReportService, times(0)).storeReportToWorkspace(any());
    }

    @Test
    @RunWithCustomExecutor
    public void givenExistReportThenIdempotencyOk() throws Exception {
        when(deleteGotVersionsReportService.isReportWrittenInWorkspace(any())).thenReturn(true);
        when(batchReportClient.readComputedDetailsFromReport(any(), any())).thenReturn(null);
        JsonNode logbookOperation = JsonHandler.getFromFileAsTypeReference(
            PropertiesUtils.getResourceFile(LOGBOOK_OPERATION_MODEL),
            new TypeReference<>() {}
        );
        when(logbookOperationsClient.selectOperationById(any())).thenReturn(logbookOperation);

        ItemStatus itemStatus = deleteGotVersionsFinalizationPlugin.execute(params, handlerIO);

        assertEquals(OK, itemStatus.getGlobalStatus());
        verify(deleteGotVersionsReportService, times(0)).storeReportToWorkspace(any());
    }
}
