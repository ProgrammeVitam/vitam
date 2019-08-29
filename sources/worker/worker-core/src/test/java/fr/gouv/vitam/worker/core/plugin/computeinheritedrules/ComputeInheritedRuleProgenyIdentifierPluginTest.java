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

package fr.gouv.vitam.worker.core.plugin.computeinheritedrules;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import fr.gouv.vitam.batch.report.client.BatchReportClient;
import fr.gouv.vitam.batch.report.client.BatchReportClientFactory;
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
import fr.gouv.vitam.worker.core.distribution.JsonLineGenericIterator;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
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
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
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

    private ComputeInheritedRuleProgenyIdentifierPlugin computeInheritedRuleProgenyIdentifierPlugin;
    private static final TypeReference<JsonLineModel> TYPE_REFERENCE = new TypeReference<JsonLineModel>() {};

    @Before
    public void setUp() throws Exception {
        when(metaDataClientFactory.getClient()).thenReturn(metaDataClient);
        when(batchReportClientFactory.getClient()).thenReturn(batchReportClient);
        computeInheritedRuleProgenyIdentifierPlugin = new ComputeInheritedRuleProgenyIdentifierPlugin(metaDataClientFactory, batchReportClientFactory, 2);
    }

    @Test
    public void should_generate_invalidation_distribution_file() throws Exception {
        // Given
        List<String> distributedUnits = Collections.singletonList("momaUnit");
        File unitToInvalidateFile = temporaryFolder.newFile();

        HandlerIO handlerIO = givenHandlerIo(distributedUnits, unitToInvalidateFile);
        WorkerParameters workerParameters = givenWorkerParameters();

        List<String> progenyUnits = Collections.singletonList("daughter");
        List<String> allUnitsToInvalidate = new ArrayList<>();
        allUnitsToInvalidate.addAll(distributedUnits);
        allUnitsToInvalidate.addAll(progenyUnits);

        when(metaDataClient.selectUnits(any(JsonNode.class))).thenReturn(createMetadataResponseFromList(allUnitsToInvalidate));
        when(batchReportClient.getUnitsToInvalidate(anyString())).thenReturn(createBatchReportResponseFromList(allUnitsToInvalidate));
        ArgumentCaptor<List<String>> listArgumentCaptor = ArgumentCaptor.forClass(List.class);
        doNothing().when(batchReportClient).saveUnitsAndProgeny(anyString(), listArgumentCaptor.capture());

        // When
        computeInheritedRuleProgenyIdentifierPlugin.executeList(workerParameters, handlerIO);

        // Then
        JsonLineGenericIterator<JsonLineModel> lines = new JsonLineGenericIterator<>(new FileInputStream(unitToInvalidateFile), TYPE_REFERENCE);
        List<String> unitIdsInResultingFile = lines.stream().map(JsonLineModel::getId).collect(Collectors.toList());

        assertThat(listArgumentCaptor.getValue()).containsAll(allUnitsToInvalidate);
        assertThat(unitIdsInResultingFile.size()).isEqualTo(allUnitsToInvalidate.size());
        assertThat(unitIdsInResultingFile).containsAll(allUnitsToInvalidate);
    }

    @Test
    public void should_not_save_units_when_no_children() throws Exception {
        // Given
        HandlerIO handlerIO = givenHandlerIo(Collections.singletonList("BATMAN_UNIT"), temporaryFolder.newFile());
        WorkerParameters workerParameters = givenWorkerParameters();
        JsonNode emptyResponse = JsonHandler.toJsonNode(new RequestResponseOK<JsonNode>());

        when(metaDataClient.selectUnits(any())).thenReturn(emptyResponse);
        when(batchReportClient.getUnitsToInvalidate(anyString())).thenReturn(emptyResponse);                   // <--- here return response with parent

        // When
        computeInheritedRuleProgenyIdentifierPlugin.executeList(workerParameters, handlerIO);

        // Then
        verify(batchReportClient, never()).saveUnitsAndProgeny(anyString(), anyList());
    }

    @Test
    public void should_throw_exception_when_result_no_id_projection() throws Exception {
        // Given
        HandlerIO handlerIO = givenHandlerIo(Collections.singletonList("BATMAN_UNIT"), temporaryFolder.newFile());
        WorkerParameters workerParameters = givenWorkerParameters();

        List<JsonNode> unitJsonList = Stream.of("JOKER_ID")
            .map(object -> JsonHandler.createObjectNode().put("#BATMAN_PROJECTION", object))
            .collect(Collectors.toList());

        RequestResponse<JsonNode> results = new RequestResponseOK<JsonNode>().addAllResults(unitJsonList);
        Object response = Response.status(Response.Status.OK).entity(results.setHttpCode(200)).build().getEntity();
        when(metaDataClient.selectUnits(any(JsonNode.class))).thenReturn(JsonHandler.toJsonNode(response));                 // <--- here no #id field

        JsonNode emptyResponse = JsonHandler.toJsonNode(new RequestResponseOK<JsonNode>());
        when(batchReportClient.getUnitsToInvalidate(anyString())).thenReturn(emptyResponse);

        // When
        ThrowingCallable pluginExecution = () -> computeInheritedRuleProgenyIdentifierPlugin.executeList(workerParameters, handlerIO);

        // Then
        assertThatThrownBy(pluginExecution).isInstanceOf(NullPointerException.class);
    }

    private WorkerParameters givenWorkerParameters() {
        WorkerParameters workerParameters = mock(WorkerParameters.class);
        when(workerParameters.getObjectNameList()).thenReturn(Collections.singletonList("aeeaaaaaagehslhxabfh2all2cnh4zyaaaaq"));
        return workerParameters;
    }

    private JsonNode createMetadataResponseFromList(List<String> unitsIds) throws InvalidParseOperationException {
        List<JsonNode> unitJsonList = unitsIds.stream()
            .map(object -> JsonHandler.createObjectNode().put(VitamFieldsHelper.id(), object))
            .collect(Collectors.toList());
        RequestResponse<JsonNode> results = new RequestResponseOK<JsonNode>().addAllResults(unitJsonList);
        Object response = Response.status(Response.Status.OK).entity(results.setHttpCode(200)).build().getEntity();
        return JsonHandler.toJsonNode(response);
    }

    private JsonNode createBatchReportResponseFromList(List<String> unitsIds) throws InvalidParseOperationException {
        List<JsonNode> unitJsonList = unitsIds.stream()
            .map(TextNode::new)
            .collect(Collectors.toList());
        RequestResponse<JsonNode> results = new RequestResponseOK<JsonNode>().addAllResults(unitJsonList);
        Object response = Response.status(Response.Status.OK).entity(results.setHttpCode(200)).build().getEntity();
        return JsonHandler.toJsonNode(response);
    }

    private HandlerIO givenHandlerIo(List<String> unitsIds, File unitToInvalidateFile) throws Exception {
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
        when(handlerIO.getNewLocalFile(anyString())).thenReturn(unitToInvalidateFile);
        when(processingUri.getPath()).thenReturn("");
        when(handlerIO.getOutput(anyInt())).thenReturn(processingUri);
        when(handlerIO.getContainerName()).thenReturn("processId");

        return handlerIO;
    }
}
