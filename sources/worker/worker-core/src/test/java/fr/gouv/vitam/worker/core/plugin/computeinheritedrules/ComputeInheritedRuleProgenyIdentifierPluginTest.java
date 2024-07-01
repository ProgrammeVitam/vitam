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
package fr.gouv.vitam.worker.core.plugin.computeinheritedrules;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.batch.report.client.BatchReportClient;
import fr.gouv.vitam.batch.report.client.BatchReportClientFactory;
import fr.gouv.vitam.batch.report.model.ReportBody;
import fr.gouv.vitam.batch.report.model.ReportType;
import fr.gouv.vitam.batch.report.model.entry.UnitComputedInheritedRulesInvalidationReportEntry;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.processing.ProcessingUri;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static fr.gouv.vitam.worker.core.plugin.computeinheritedrules.ComputeInheritedRuleProgenyIdentifierPlugin.UNITS_JSONL_FILE_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ComputeInheritedRuleProgenyIdentifierPluginTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private MetaDataClientFactory metaDataClientFactory;

    @Mock
    private MetaDataClient metaDataClient;

    @Mock
    private BatchReportClientFactory batchReportClientFactory;

    @Mock
    private BatchReportClient batchReportClient;

    @Mock
    private WorkspaceClient workspaceClient;

    @Mock
    private WorkspaceClientFactory workspaceClientFactory;

    private ComputeInheritedRuleProgenyIdentifierPlugin computeInheritedRuleProgenyIdentifierPlugin;

    @Before
    public void setUp() throws Exception {
        when(metaDataClientFactory.getClient()).thenReturn(metaDataClient);
        when(batchReportClientFactory.getClient()).thenReturn(batchReportClient);
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);
        computeInheritedRuleProgenyIdentifierPlugin = new ComputeInheritedRuleProgenyIdentifierPlugin(
            metaDataClientFactory,
            batchReportClientFactory,
            workspaceClientFactory,
            2
        );
    }

    @Test
    public void should_generate_invalidation_distribution_file() throws Exception {
        // Given
        List<String> distributedUnits = Collections.singletonList("momaUnit");
        HandlerIO handlerIO = givenHandlerIo(distributedUnits);
        WorkerParameters workerParameters = givenWorkerParameters();
        when(workspaceClient.isExistingObject("processId", UNITS_JSONL_FILE_NAME)).thenReturn(false);

        List<String> progenyUnits = Collections.singletonList("daughter");
        List<String> allUnitsToInvalidate = new ArrayList<>();
        allUnitsToInvalidate.addAll(distributedUnits);
        allUnitsToInvalidate.addAll(progenyUnits);

        when(metaDataClient.selectUnits(any(JsonNode.class))).thenReturn(
            createMetadataResponseFromList(allUnitsToInvalidate)
        );
        ArgumentCaptor<ReportBody<UnitComputedInheritedRulesInvalidationReportEntry>> reportBodyArgumentCaptor =
            ArgumentCaptor.forClass(ReportBody.class);
        doNothing().when(batchReportClient).appendReportEntries(reportBodyArgumentCaptor.capture());

        // When
        computeInheritedRuleProgenyIdentifierPlugin.executeList(workerParameters, handlerIO);

        // Then
        ReportBody<UnitComputedInheritedRulesInvalidationReportEntry> reportBody = reportBodyArgumentCaptor.getValue();
        assertThat(reportBody.getProcessId()).isEqualTo("processId");
        assertThat(reportBody.getReportType()).isEqualTo(ReportType.UNIT_COMPUTED_INHERITED_RULES_INVALIDATION);
        assertThat(reportBody.getEntries()).extracting("unitId").containsAll(allUnitsToInvalidate);
        verify(batchReportClient).exportUnitsToInvalidate(anyString(), any());
        verify(workspaceClient, never()).deleteObject("processId", UNITS_JSONL_FILE_NAME);
    }

    @Test
    public void should_regenerate_invalidation_distribution_file() throws Exception {
        // Given
        List<String> distributedUnits = Collections.singletonList("momaUnit");
        HandlerIO handlerIO = givenHandlerIo(distributedUnits);
        WorkerParameters workerParameters = givenWorkerParameters();
        when(workspaceClient.isExistingObject("processId", UNITS_JSONL_FILE_NAME)).thenReturn(true);

        List<String> progenyUnits = Collections.singletonList("daughter");
        List<String> allUnitsToInvalidate = new ArrayList<>();
        allUnitsToInvalidate.addAll(distributedUnits);
        allUnitsToInvalidate.addAll(progenyUnits);

        when(metaDataClient.selectUnits(any(JsonNode.class))).thenReturn(
            createMetadataResponseFromList(allUnitsToInvalidate)
        );
        ArgumentCaptor<ReportBody<UnitComputedInheritedRulesInvalidationReportEntry>> reportBodyArgumentCaptor =
            ArgumentCaptor.forClass(ReportBody.class);
        doNothing().when(batchReportClient).appendReportEntries(reportBodyArgumentCaptor.capture());

        // When
        computeInheritedRuleProgenyIdentifierPlugin.executeList(workerParameters, handlerIO);

        // Then
        ReportBody<UnitComputedInheritedRulesInvalidationReportEntry> reportBody = reportBodyArgumentCaptor.getValue();
        assertThat(reportBody.getProcessId()).isEqualTo("processId");
        assertThat(reportBody.getReportType()).isEqualTo(ReportType.UNIT_COMPUTED_INHERITED_RULES_INVALIDATION);
        assertThat(reportBody.getEntries()).extracting("unitId").containsAll(allUnitsToInvalidate);
        verify(batchReportClient).exportUnitsToInvalidate(anyString(), any());
        verify(workspaceClient).deleteObject("processId", UNITS_JSONL_FILE_NAME);
    }

    @Test
    public void should_not_save_units_when_no_children() throws Exception {
        // Given
        HandlerIO handlerIO = givenHandlerIo(Collections.singletonList("BATMAN_UNIT"));
        WorkerParameters workerParameters = givenWorkerParameters();
        JsonNode emptyResponse = JsonHandler.toJsonNode(new RequestResponseOK<JsonNode>());

        when(metaDataClient.selectUnits(any())).thenReturn(emptyResponse);

        // When
        computeInheritedRuleProgenyIdentifierPlugin.executeList(workerParameters, handlerIO);

        // Then
        verify(batchReportClient, never()).appendReportEntries(any());
        verify(batchReportClient).exportUnitsToInvalidate(anyString(), any());
    }

    private WorkerParameters givenWorkerParameters() {
        WorkerParameters workerParameters = mock(WorkerParameters.class);
        when(workerParameters.getObjectNameList()).thenReturn(
            Collections.singletonList("aeeaaaaaagehslhxabfh2all2cnh4zyaaaaq")
        );
        return workerParameters;
    }

    private JsonNode createMetadataResponseFromList(List<String> unitsIds) throws InvalidParseOperationException {
        List<JsonNode> unitJsonList = unitsIds
            .stream()
            .map(object -> JsonHandler.createObjectNode().put(VitamFieldsHelper.id(), object))
            .collect(Collectors.toList());
        RequestResponse<JsonNode> results = new RequestResponseOK<JsonNode>().addAllResults(unitJsonList);
        Object response = Response.status(Response.Status.OK).entity(results.setHttpCode(200)).build().getEntity();
        return JsonHandler.toJsonNode(response);
    }

    private HandlerIO givenHandlerIo(List<String> unitsIds) throws Exception {
        HandlerIO handlerIO = mock(HandlerIO.class);
        ProcessingUri processingUri = mock(ProcessingUri.class);

        File distributionFile = temporaryFolder.newFile();
        unitsIds.forEach(unit -> {
            try {
                FileUtils.writeStringToFile(distributionFile, "{\"id\":\"" + unit + "\"}", Charset.defaultCharset());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        when(handlerIO.getInput(anyInt())).thenReturn(distributionFile);
        when(processingUri.getPath()).thenReturn("");
        when(handlerIO.getOutput(anyInt())).thenReturn(processingUri);
        when(handlerIO.getContainerName()).thenReturn("processId");

        return handlerIO;
    }
}
