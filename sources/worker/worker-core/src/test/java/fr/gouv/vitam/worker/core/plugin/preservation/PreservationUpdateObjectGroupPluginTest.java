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
package fr.gouv.vitam.worker.core.plugin.preservation;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.batch.report.model.PreservationStatus;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.format.identification.model.FormatIdentifierResponse;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.ActionTypePreservation;
import fr.gouv.vitam.common.model.objectgroup.DbFormatIdentificationModel;
import fr.gouv.vitam.common.model.preservation.OtherMetadata;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.storage.engine.common.model.response.StoredInfoResult;
import fr.gouv.vitam.worker.core.plugin.preservation.model.ExtractedMetadata;
import fr.gouv.vitam.worker.core.plugin.preservation.model.InputPreservation;
import fr.gouv.vitam.worker.core.plugin.preservation.model.OutputPreservation;
import fr.gouv.vitam.worker.core.plugin.preservation.model.WorkflowBatchResult;
import fr.gouv.vitam.worker.core.plugin.preservation.model.WorkflowBatchResult.OutputExtra;
import fr.gouv.vitam.worker.core.plugin.preservation.model.WorkflowBatchResults;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static fr.gouv.vitam.common.model.StatusCode.FATAL;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.common.model.administration.ActionTypePreservation.EXTRACT;
import static fr.gouv.vitam.common.model.administration.ActionTypePreservation.GENERATE;
import static fr.gouv.vitam.common.model.administration.ActionTypePreservation.IDENTIFY;
import static fr.gouv.vitam.worker.core.plugin.preservation.TestWorkerParameter.TestWorkerParameterBuilder.workerParameterBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;

public class PreservationUpdateObjectGroupPluginTest {

    private final String GOT_ID = "GOT_ID";
    private final TestWorkerParameter parameter = workerParameterBuilder()
        .withContainerName("CONTAINER_NAME_TEST")
        .withRequestId("REQUEST_ID_TEST")
        .build();
    private final TestHandlerIO handlerIO = new TestHandlerIO();

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Mock
    private MetaDataClientFactory metaDataClientFactory;

    @Mock
    private MetaDataClient metaDataClient;

    private PreservationUpdateObjectGroupPlugin plugin;

    @Before
    public void setUp() throws Exception {
        given(metaDataClientFactory.getClient()).willReturn(metaDataClient);
        parameter.setObjectNameList(Collections.singletonList(GOT_ID));
        plugin = new PreservationUpdateObjectGroupPlugin(metaDataClientFactory);

        WorkflowBatchResults batchResults = getWorkflowBatchResults(getOutputPreservation(GENERATE));
        handlerIO.addOutputResult(0, batchResults);
        handlerIO.setInputs(batchResults);
    }

    @Test
    public void should_update_objectGroup_with_new_binary_to_same_source() throws Exception {
        // Given
        InputStream objectGroupStream = getClass().getResourceAsStream("/preservation/objectGroupDslResponse.json");

        RequestResponse<JsonNode> responseOK = new RequestResponseOK<JsonNode>()
            .addResult(JsonHandler.getFromInputStream(objectGroupStream))
            .setHttpCode(Response.Status.OK.getStatusCode());
        given(metaDataClient.getObjectGroupByIdRaw(ArgumentMatchers.any())).willReturn(responseOK);

        // When
        List<ItemStatus> itemStatuses = plugin.executeList(parameter, handlerIO);

        // Then
        assertThat(itemStatuses).extracting(ItemStatus::getGlobalStatus).containsOnly(OK);
    }

    @Test
    public void should_add_multiple_generation() throws Exception {
        // Given
        ArgumentCaptor<JsonNode> finalQueryCaptor = ArgumentCaptor.forClass(JsonNode.class);
        ArgumentCaptor<String> gotIdCaptor = ArgumentCaptor.forClass(String.class);

        WorkflowBatchResults batchResults = getWorkflowBatchResults(
            getOutputPreservation(GENERATE),
            getOutputPreservation(GENERATE),
            getOutputPreservation(GENERATE)
        );
        TestHandlerIO testHandlerIO = new TestHandlerIO();
        testHandlerIO.addOutputResult(0, batchResults);
        testHandlerIO.setInputs(batchResults);

        InputStream objectGroupStream = getClass().getResourceAsStream("/preservation/objectGroupDslResponse.json");

        RequestResponse<JsonNode> responseOK = new RequestResponseOK<JsonNode>()
            .addResult(JsonHandler.getFromInputStream(objectGroupStream))
            .setHttpCode(Response.Status.OK.getStatusCode());
        given(metaDataClient.getObjectGroupByIdRaw(ArgumentMatchers.any())).willReturn(responseOK);
        doNothing().when(metaDataClient).updateObjectGroupById(finalQueryCaptor.capture(), gotIdCaptor.capture());

        // When
        List<ItemStatus> itemStatuses = plugin.executeList(parameter, testHandlerIO);

        // Then
        assertThat(itemStatuses).extracting(ItemStatus::getGlobalStatus).containsOnly(OK);

        assertThat(
            finalQueryCaptor.getValue().at("/$action/2/$set/#qualifiers/0/versions/0/DataObjectVersion").asText()
        ).isEqualTo("BinaryMaster_1");
        assertThat(
            finalQueryCaptor.getValue().at("/$action/2/$set/#qualifiers/0/versions/1/DataObjectVersion").asText()
        ).isEqualTo("BinaryMaster_2");
        assertThat(
            finalQueryCaptor.getValue().at("/$action/2/$set/#qualifiers/0/versions/2/DataObjectVersion").asText()
        ).isEqualTo("BinaryMaster_3");
        assertThat(
            finalQueryCaptor.getValue().at("/$action/2/$set/#qualifiers/0/versions/3/DataObjectVersion").asText()
        ).isEqualTo("BinaryMaster_4");
    }

    @Test
    public void should_update_objectGroup_with_new_binary_to_same_source_final_query() throws Exception {
        // Given
        ArgumentCaptor<JsonNode> finalQueryCaptor = ArgumentCaptor.forClass(JsonNode.class);
        ArgumentCaptor<String> gotIdCaptor = ArgumentCaptor.forClass(String.class);
        InputStream objectGroupStream = getClass().getResourceAsStream("/preservation/objectGroupDslResponse.json");

        RequestResponse<JsonNode> responseOK = new RequestResponseOK<JsonNode>()
            .addResult(JsonHandler.getFromInputStream(objectGroupStream))
            .setHttpCode(Response.Status.OK.getStatusCode());
        given(metaDataClient.getObjectGroupByIdRaw(ArgumentMatchers.any())).willReturn(responseOK);
        doNothing().when(metaDataClient).updateObjectGroupById(finalQueryCaptor.capture(), gotIdCaptor.capture());

        // When
        plugin.executeList(parameter, handlerIO);

        // Then
        assertThat(finalQueryCaptor.getValue().at("/$action/2/$set/#qualifiers/0/_nbc").intValue()).isEqualTo(2);
        assertThat(gotIdCaptor.getValue()).isEqualTo(GOT_ID);
    }

    @Test
    public void should_update_objectGroup_with_identify_binary() throws Exception {
        // Given
        ArgumentCaptor<JsonNode> finalQueryCaptor = ArgumentCaptor.forClass(JsonNode.class);
        ArgumentCaptor<String> gotIdCaptor = ArgumentCaptor.forClass(String.class);

        WorkflowBatchResults batchResults = getWorkflowBatchResults(getOutputPreservation(IDENTIFY));
        TestHandlerIO testHandlerIO = new TestHandlerIO();
        testHandlerIO.addOutputResult(0, batchResults);
        testHandlerIO.setInputs(batchResults);

        InputStream objectGroupStream = getClass().getResourceAsStream("/preservation/objectGroupDslResponse.json");

        RequestResponse<JsonNode> responseOK = new RequestResponseOK<JsonNode>()
            .addResult(JsonHandler.getFromInputStream(objectGroupStream))
            .setHttpCode(Response.Status.OK.getStatusCode());
        given(metaDataClient.getObjectGroupByIdRaw(ArgumentMatchers.any())).willReturn(responseOK);
        doNothing().when(metaDataClient).updateObjectGroupById(finalQueryCaptor.capture(), gotIdCaptor.capture());

        // When
        List<ItemStatus> itemStatuses = plugin.executeList(parameter, testHandlerIO);

        // Then
        assertThat(itemStatuses).extracting(ItemStatus::getGlobalStatus).containsOnly(OK);
        assertThat(
            finalQueryCaptor
                .getValue()
                .at("/$action/2/$set/#qualifiers/0/versions/0/FormatIdentification/FormatLitteral")
                .textValue()
        ).isEqualTo("Batman");
        assertThat(
            finalQueryCaptor
                .getValue()
                .at("/$action/2/$set/#qualifiers/0/versions/0/FormatIdentification/MimeType")
                .textValue()
        ).isEqualTo("text/winner");
    }

    @Test
    public void should_update_objectGroup_to_new_target_with_binary() throws Exception {
        // Given
        ArgumentCaptor<JsonNode> finalQueryCaptor = ArgumentCaptor.forClass(JsonNode.class);

        WorkflowBatchResults batchResults = getWorkflowBatchResults("Dissemination", getOutputPreservation(GENERATE));
        TestHandlerIO testHandlerIO = new TestHandlerIO();
        testHandlerIO.addOutputResult(0, batchResults);
        testHandlerIO.setInputs(batchResults);

        InputStream objectGroupStream = getClass().getResourceAsStream("/preservation/objectGroupDslResponse.json");

        RequestResponse<JsonNode> responseOK = new RequestResponseOK<JsonNode>()
            .addResult(JsonHandler.getFromInputStream(objectGroupStream))
            .setHttpCode(Response.Status.OK.getStatusCode());
        given(metaDataClient.getObjectGroupByIdRaw(ArgumentMatchers.any())).willReturn(responseOK);
        doNothing().when(metaDataClient).updateObjectGroupById(finalQueryCaptor.capture(), ArgumentMatchers.any());

        // When
        List<ItemStatus> itemStatuses = plugin.executeList(parameter, testHandlerIO);

        // Then
        assertThat(itemStatuses).extracting(ItemStatus::getGlobalStatus).containsOnly(OK);
        assertThat(finalQueryCaptor.getValue().at("/$action/2/$set/#qualifiers/1/qualifier").textValue()).isEqualTo(
            "Dissemination"
        );
        assertThat(finalQueryCaptor.getValue().at("/$action/2/$set/#qualifiers/1/_nbc").intValue()).isEqualTo(1);
    }

    @Test
    public void should_return_disable_lfc_item_status_when_nothing_to_update() throws Exception {
        // Given
        WorkflowBatchResults noGeneratedIOrIdentifiedBatch = getWorkflowBatchResults(getOutputPreservation(EXTRACT));

        TestHandlerIO testHandlerIO = new TestHandlerIO();
        testHandlerIO.addOutputResult(0, noGeneratedIOrIdentifiedBatch);
        testHandlerIO.setInputs(noGeneratedIOrIdentifiedBatch);

        // When
        List<ItemStatus> itemStatuses = plugin.executeList(parameter, testHandlerIO);

        // Then
        assertThat(itemStatuses).extracting(ItemStatus::isLifecycleEnable).containsOnly(false);
    }

    @Test
    public void should_update_identify_extract_and_generate() throws Exception {
        // Given
        ArgumentCaptor<JsonNode> finalQueryCaptor = ArgumentCaptor.forClass(JsonNode.class);
        ArgumentCaptor<String> gotIdCaptor = ArgumentCaptor.forClass(String.class);

        WorkflowBatchResults batchResults = getWorkflowBatchResults(
            getOutputPreservation(EXTRACT),
            getOutputPreservation(GENERATE),
            getOutputPreservation(IDENTIFY)
        );
        TestHandlerIO testHandlerIO = new TestHandlerIO();
        testHandlerIO.addOutputResult(0, batchResults);
        testHandlerIO.setInputs(batchResults);

        InputStream objectGroupStream = getClass().getResourceAsStream("/preservation/objectGroupDslResponse.json");

        RequestResponse<JsonNode> responseOK = new RequestResponseOK<JsonNode>()
            .addResult(JsonHandler.getFromInputStream(objectGroupStream))
            .setHttpCode(Response.Status.OK.getStatusCode());
        given(metaDataClient.getObjectGroupByIdRaw(ArgumentMatchers.any())).willReturn(responseOK);
        doNothing().when(metaDataClient).updateObjectGroupById(finalQueryCaptor.capture(), gotIdCaptor.capture());

        // When
        List<ItemStatus> itemStatuses = plugin.executeList(parameter, testHandlerIO);

        // Then
        assertThat(itemStatuses).extracting(ItemStatus::getGlobalStatus).containsOnly(OK);
        assertThat(finalQueryCaptor.getValue().at("/$action/2/$set/#qualifiers/0/_nbc").intValue()).isEqualTo(2);
        assertThat(finalQueryCaptor.getValue().at("/$action/1/$set/#nbobjects").intValue()).isEqualTo(2);
        assertThat(
            finalQueryCaptor
                .getValue()
                .at("/$action/2/$set/#qualifiers/0/versions/0/FormatIdentification/FormatLitteral")
                .textValue()
        ).isEqualTo("Batman");
        assertThat(
            finalQueryCaptor
                .getValue()
                .at("/$action/2/$set/#qualifiers/0/versions/0/FormatIdentification/MimeType")
                .textValue()
        ).isEqualTo("text/winner");
        assertThat(
            finalQueryCaptor.getValue().at("/$action/2/$set/#qualifiers/0/versions/0/OtherMetadata/GPS/0").textValue()
        ).isEqualTo("40.714, -74.006");
    }

    @Test
    public void should_update_multiple_binary_generated() throws Exception {
        // Given
        ArgumentCaptor<JsonNode> finalQueryCaptor = ArgumentCaptor.forClass(JsonNode.class);
        ArgumentCaptor<String> gotIdCaptor = ArgumentCaptor.forClass(String.class);

        WorkflowBatchResults batchResults = getWorkflowBatchResults(
            getOutputPreservation(GENERATE),
            getOutputPreservation(GENERATE)
        );
        TestHandlerIO testHandlerIO = new TestHandlerIO();
        testHandlerIO.addOutputResult(0, batchResults);
        testHandlerIO.setInputs(batchResults);

        InputStream objectGroupStream = getClass().getResourceAsStream("/preservation/objectGroupDslResponse.json");

        RequestResponse<JsonNode> responseOK = new RequestResponseOK<JsonNode>()
            .addResult(JsonHandler.getFromInputStream(objectGroupStream))
            .setHttpCode(Response.Status.OK.getStatusCode());
        given(metaDataClient.getObjectGroupByIdRaw(ArgumentMatchers.any())).willReturn(responseOK);
        doNothing().when(metaDataClient).updateObjectGroupById(finalQueryCaptor.capture(), gotIdCaptor.capture());

        // When
        List<ItemStatus> itemStatuses = plugin.executeList(parameter, testHandlerIO);

        // Then
        assertThat(itemStatuses).extracting(ItemStatus::getGlobalStatus).containsOnly(OK);
        assertThat(finalQueryCaptor.getValue().at("/$action/2/$set/#qualifiers/0/_nbc").intValue()).isEqualTo(3);
        assertThat(finalQueryCaptor.getValue().at("/$action/1/$set/#nbobjects").intValue()).isEqualTo(3);
    }

    @Test
    public void should_update_extracted_metadata() throws Exception {
        // Given
        ArgumentCaptor<JsonNode> finalQueryCaptor = ArgumentCaptor.forClass(JsonNode.class);

        WorkflowBatchResults batchResults = getWorkflowBatchResults(getOutputPreservation(EXTRACT));
        TestHandlerIO testHandlerIO = new TestHandlerIO();
        testHandlerIO.addOutputResult(0, batchResults);
        testHandlerIO.setInputs(batchResults);

        InputStream objectGroupStream = getClass().getResourceAsStream("/preservation/objectGroupDslResponse.json");

        RequestResponse<JsonNode> responseOK = new RequestResponseOK<JsonNode>()
            .addResult(JsonHandler.getFromInputStream(objectGroupStream))
            .setHttpCode(Response.Status.OK.getStatusCode());
        given(metaDataClient.getObjectGroupByIdRaw(ArgumentMatchers.any())).willReturn(responseOK);
        doNothing().when(metaDataClient).updateObjectGroupById(finalQueryCaptor.capture(), ArgumentMatchers.any());

        // When
        List<ItemStatus> itemStatuses = plugin.executeList(parameter, testHandlerIO);

        // Then
        assertThat(itemStatuses).extracting(ItemStatus::getGlobalStatus).containsOnly(OK);
        assertThat(
            finalQueryCaptor.getValue().at("/$action/2/$set/#qualifiers/0/versions/0/OtherMetadata/GPS/0").textValue()
        ).isEqualTo("40.714, -74.006");
    }

    @Test
    public void should_update_extracted_metadata_with_raw_metadata() throws Exception {
        // Given
        ArgumentCaptor<JsonNode> finalQueryCaptor = ArgumentCaptor.forClass(JsonNode.class);

        WorkflowBatchResults batchResults = getWorkflowBatchResults(getOutputPreservationWithRawMetadata(EXTRACT));
        TestHandlerIO testHandlerIO = new TestHandlerIO();
        testHandlerIO.addOutputResult(0, batchResults);
        testHandlerIO.setInputs(batchResults);

        InputStream objectGroupStream = getClass().getResourceAsStream("/preservation/objectGroupDslResponse.json");

        RequestResponse<JsonNode> responseOK = new RequestResponseOK<JsonNode>()
            .addResult(JsonHandler.getFromInputStream(objectGroupStream))
            .setHttpCode(Response.Status.OK.getStatusCode());
        given(metaDataClient.getObjectGroupByIdRaw(ArgumentMatchers.any())).willReturn(responseOK);
        doNothing().when(metaDataClient).updateObjectGroupById(finalQueryCaptor.capture(), ArgumentMatchers.any());

        // When
        List<ItemStatus> itemStatuses = plugin.executeList(parameter, testHandlerIO);

        // Then
        assertThat(itemStatuses).extracting(ItemStatus::getGlobalStatus).containsOnly(OK);
        assertThat(
            finalQueryCaptor
                .getValue()
                .at("/$action/2/$set/#qualifiers/0/versions/0/OtherMetadata/RawMetadata/0")
                .textValue()
        ).isNotEmpty();
    }

    @Test
    public void should_split_raw_metadata_to_avoid_es_indexing_error() throws Exception {
        // Given
        ArgumentCaptor<JsonNode> finalQueryCaptor = ArgumentCaptor.forClass(JsonNode.class);

        WorkflowBatchResults batchResults = getWorkflowBatchResults(getOutputRawMetadata(EXTRACT));
        TestHandlerIO testHandlerIO = new TestHandlerIO();
        testHandlerIO.addOutputResult(0, batchResults);
        testHandlerIO.setInputs(batchResults);

        InputStream objectGroupStream = getClass().getResourceAsStream("/preservation/objectGroupDslResponse.json");

        RequestResponse<JsonNode> responseOK = new RequestResponseOK<JsonNode>()
            .addResult(JsonHandler.getFromInputStream(objectGroupStream))
            .setHttpCode(Response.Status.OK.getStatusCode());
        given(metaDataClient.getObjectGroupByIdRaw(ArgumentMatchers.any())).willReturn(responseOK);
        doNothing().when(metaDataClient).updateObjectGroupById(finalQueryCaptor.capture(), ArgumentMatchers.any());

        // When
        List<ItemStatus> itemStatuses = plugin.executeList(parameter, testHandlerIO);

        // Then
        assertThat(itemStatuses).extracting(ItemStatus::getGlobalStatus).containsOnly(OK);
        assertThat(
            finalQueryCaptor.getValue().at("/$action/2/$set/#qualifiers/0/versions/0/OtherMetadata/RawMetadata").size()
        ).isEqualTo(2);
        assertThat(
            finalQueryCaptor
                .getValue()
                .at("/$action/2/$set/#qualifiers/0/versions/0/OtherMetadata/RawMetadata/0")
                .textValue()
                .length()
        ).isEqualTo(VitamConfiguration.getTextMaxLength());
    }

    @Test
    public void should_return_fatal_item_status_when_not_FormatIdentifierResponse() throws Exception {
        // Given
        OutputPreservation outputPreservation = getOutputPreservation(IDENTIFY);
        outputPreservation.setFormatIdentification(null); // <-- here no FormatIdentifierResponse

        WorkflowBatchResults batchResults = getWorkflowBatchResults(outputPreservation);
        TestHandlerIO testHandlerIO = new TestHandlerIO();
        testHandlerIO.addOutputResult(0, batchResults);
        testHandlerIO.setInputs(batchResults);

        InputStream objectGroupStream = getClass().getResourceAsStream("/preservation/objectGroupDslResponse.json");

        RequestResponse<JsonNode> responseOK = new RequestResponseOK<JsonNode>()
            .addResult(JsonHandler.getFromInputStream(objectGroupStream))
            .setHttpCode(Response.Status.OK.getStatusCode());
        given(metaDataClient.getObjectGroupByIdRaw(ArgumentMatchers.any())).willReturn(responseOK);

        // When
        List<ItemStatus> itemStatuses = plugin.executeList(parameter, testHandlerIO);

        // Then
        assertThat(itemStatuses).extracting(ItemStatus::getGlobalStatus).containsOnly(FATAL);
    }

    @Test
    public void should_return_fatal_item_status_when_no_last_qualifiers_version() throws Exception {
        // Given
        WorkflowBatchResults batchResults = getWorkflowBatchResults(getOutputPreservation(GENERATE));
        TestHandlerIO testHandlerIO = new TestHandlerIO();
        testHandlerIO.addOutputResult(0, batchResults);
        testHandlerIO.setInputs(batchResults);

        InputStream objectGroupStream = getClass()
            .getResourceAsStream("/preservation/objectGroupDslResponseWithoutBinaryMaster.json"); // <-- here no BinaryMaster

        RequestResponse<JsonNode> responseOK = new RequestResponseOK<JsonNode>()
            .addResult(JsonHandler.getFromInputStream(objectGroupStream))
            .setHttpCode(Response.Status.OK.getStatusCode());
        given(metaDataClient.getObjectGroupByIdRaw(ArgumentMatchers.any())).willReturn(responseOK);

        // When
        List<ItemStatus> itemStatuses = plugin.executeList(parameter, testHandlerIO);

        // Then
        assertThat(itemStatuses).extracting(ItemStatus::getGlobalStatus).containsOnly(FATAL);
    }

    @Test
    public void should_return_fatal_item_status_when_no_formatResult_in_batch_result() throws Exception {
        // Given
        WorkflowBatchResults batchResults = getWorkflowBatchResults(
            emptyFormatOutputExtra(getOutputPreservation(GENERATE))
        ); // <-- here no format
        TestHandlerIO testHandlerIO = new TestHandlerIO();
        testHandlerIO.addOutputResult(0, batchResults);
        testHandlerIO.setInputs(batchResults);

        InputStream objectGroupStream = getClass().getResourceAsStream("/preservation/objectGroupDslResponse.json");

        RequestResponse<JsonNode> responseOK = new RequestResponseOK<JsonNode>()
            .addResult(JsonHandler.getFromInputStream(objectGroupStream))
            .setHttpCode(Response.Status.OK.getStatusCode());
        given(metaDataClient.getObjectGroupByIdRaw(ArgumentMatchers.any())).willReturn(responseOK);

        // When
        List<ItemStatus> itemStatuses = plugin.executeList(parameter, testHandlerIO);

        // Then
        assertThat(itemStatuses).extracting(ItemStatus::getGlobalStatus).containsOnly(FATAL);
    }

    @Test
    public void should_return_fatal_item_status_when_no_size_in_batch_result() throws Exception {
        // Given
        WorkflowBatchResults batchResults = getWorkflowBatchResults(
            emptySizeOutputExtra(getOutputPreservation(GENERATE))
        ); // <-- here no Size
        TestHandlerIO testHandlerIO = new TestHandlerIO();
        testHandlerIO.addOutputResult(0, batchResults);
        testHandlerIO.setInputs(batchResults);

        InputStream objectGroupStream = getClass().getResourceAsStream("/preservation/objectGroupDslResponse.json");

        RequestResponse<JsonNode> responseOK = new RequestResponseOK<JsonNode>()
            .addResult(JsonHandler.getFromInputStream(objectGroupStream))
            .setHttpCode(Response.Status.OK.getStatusCode());
        given(metaDataClient.getObjectGroupByIdRaw(ArgumentMatchers.any())).willReturn(responseOK);

        // When
        List<ItemStatus> itemStatuses = plugin.executeList(parameter, testHandlerIO);

        // Then
        assertThat(itemStatuses).extracting(ItemStatus::getGlobalStatus).containsOnly(FATAL);
    }

    @Test
    public void should_return_fatal_item_status_when_no_hash_in_batch_result() throws Exception {
        // Given
        WorkflowBatchResults batchResults = getWorkflowBatchResults(
            emptyHashOutputExtra(getOutputPreservation(GENERATE))
        ); // <-- here no HASH
        TestHandlerIO testHandlerIO = new TestHandlerIO();
        testHandlerIO.addOutputResult(0, batchResults);
        testHandlerIO.setInputs(batchResults);

        InputStream objectGroupStream = getClass().getResourceAsStream("/preservation/objectGroupDslResponse.json");

        RequestResponse<JsonNode> responseOK = new RequestResponseOK<JsonNode>()
            .addResult(JsonHandler.getFromInputStream(objectGroupStream))
            .setHttpCode(Response.Status.OK.getStatusCode());
        given(metaDataClient.getObjectGroupByIdRaw(ArgumentMatchers.any())).willReturn(responseOK);

        // When
        List<ItemStatus> itemStatuses = plugin.executeList(parameter, testHandlerIO);

        // Then
        assertThat(itemStatuses).extracting(ItemStatus::getGlobalStatus).containsOnly(FATAL);
    }

    @Test
    public void should_return_fatal_item_status_when_no_storedInfo_in_batch_result() throws Exception {
        // Given
        WorkflowBatchResults batchResults = getWorkflowBatchResults(
            emptyStoredInfoOutputExtra(getOutputPreservation(GENERATE))
        ); // <-- here no StoredInfo
        TestHandlerIO testHandlerIO = new TestHandlerIO();
        testHandlerIO.addOutputResult(0, batchResults);
        testHandlerIO.setInputs(batchResults);

        InputStream objectGroupStream = getClass().getResourceAsStream("/preservation/objectGroupDslResponse.json");

        RequestResponse<JsonNode> responseOK = new RequestResponseOK<JsonNode>()
            .addResult(JsonHandler.getFromInputStream(objectGroupStream))
            .setHttpCode(Response.Status.OK.getStatusCode());
        given(metaDataClient.getObjectGroupByIdRaw(ArgumentMatchers.any())).willReturn(responseOK);

        // When
        List<ItemStatus> itemStatuses = plugin.executeList(parameter, testHandlerIO);

        // Then
        assertThat(itemStatuses).extracting(ItemStatus::getGlobalStatus).containsOnly(FATAL);
    }

    @Test
    public void should_not_update_when_no_changes_in_identification() throws Exception {
        // Given
        WorkflowBatchResults batchResults = getWorkflowBatchResults(getOutputPreservationWithPlainTextBinary(IDENTIFY));
        TestHandlerIO testHandlerIO = new TestHandlerIO();
        testHandlerIO.addOutputResult(0, batchResults);
        testHandlerIO.setInputs(batchResults);

        InputStream objectGroupStream = getClass().getResourceAsStream("/preservation/objectGroupDslResponse.json");

        RequestResponse<JsonNode> responseOK = new RequestResponseOK<JsonNode>()
            .addResult(JsonHandler.getFromInputStream(objectGroupStream))
            .setHttpCode(Response.Status.OK.getStatusCode());
        given(metaDataClient.getObjectGroupByIdRaw(ArgumentMatchers.any())).willReturn(responseOK);

        // When
        List<ItemStatus> itemStatuses = plugin.executeList(parameter, testHandlerIO);

        // Then
        assertThat(itemStatuses).extracting(ItemStatus::isLifecycleEnable).containsOnly(false);
    }

    private WorkflowBatchResults getWorkflowBatchResults(OutputPreservation... outputPreservation)
        throws InvalidParseOperationException {
        return getWorkflowBatchResults("BinaryMaster", outputPreservation);
    }

    @Test
    public void should_write_differences_in_logbook_in_extraction() throws Exception {
        ArgumentCaptor<JsonNode> finalQueryCaptor = ArgumentCaptor.forClass(JsonNode.class);

        WorkflowBatchResults batchResults = getWorkflowBatchResults(getOutputPreservation(EXTRACT));
        TestHandlerIO testHandlerIO = new TestHandlerIO();
        testHandlerIO.addOutputResult(0, batchResults);
        testHandlerIO.setInputs(batchResults);

        InputStream objectGroupStream = getClass().getResourceAsStream("/preservation/objectGroupDslResponse.json");

        RequestResponse<JsonNode> responseOK = new RequestResponseOK<JsonNode>()
            .addResult(JsonHandler.getFromInputStream(objectGroupStream))
            .setHttpCode(Response.Status.OK.getStatusCode());
        given(metaDataClient.getObjectGroupByIdRaw(ArgumentMatchers.any())).willReturn(responseOK);
        doNothing().when(metaDataClient).updateObjectGroupById(finalQueryCaptor.capture(), ArgumentMatchers.any());

        // When
        List<ItemStatus> itemStatuses = plugin.executeList(parameter, testHandlerIO);
        // Then
        InputStream differences = getClass().getResourceAsStream("/preservation/Differences.json");
        JsonNode fromInputStream = JsonHandler.getFromInputStream(differences);
        String differencesEventDetailData = JsonHandler.unprettyPrint(fromInputStream);
        assertThat(itemStatuses.get(0).getData("eventDetailData")).isEqualTo(differencesEventDetailData);
    }

    private List<OutputExtra> emptyFormatOutputExtra(OutputPreservation... outputPreservation) {
        StoredInfoResult value = new StoredInfoResult();
        return Stream.of(outputPreservation)
            .map(
                o ->
                    new OutputExtra(
                        o,
                        "binaryGUID",
                        Optional.of(12L),
                        Optional.of("hash"),
                        Optional.empty(),
                        Optional.of(value),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()
                    )
            )
            .collect(Collectors.toList());
    }

    private List<OutputExtra> emptyHashOutputExtra(OutputPreservation... outputPreservation) {
        StoredInfoResult value = new StoredInfoResult();
        FormatIdentifierResponse format = new FormatIdentifierResponse(
            "Plain Text File",
            "text/plain",
            "x-fmt/111",
            ""
        );
        return Stream.of(outputPreservation)
            .map(
                o ->
                    new OutputExtra(
                        o,
                        "binaryGUID",
                        Optional.of(12L),
                        Optional.empty(),
                        Optional.of(format),
                        Optional.of(value),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()
                    )
            )
            .collect(Collectors.toList());
    }

    private List<OutputExtra> emptySizeOutputExtra(OutputPreservation... outputPreservation) {
        StoredInfoResult value = new StoredInfoResult();
        FormatIdentifierResponse format = new FormatIdentifierResponse(
            "Plain Text File",
            "text/plain",
            "x-fmt/111",
            ""
        );
        return Stream.of(outputPreservation)
            .map(
                o ->
                    new OutputExtra(
                        o,
                        "binaryGUID",
                        Optional.empty(),
                        Optional.of("hash"),
                        Optional.of(format),
                        Optional.of(value),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()
                    )
            )
            .collect(Collectors.toList());
    }

    private List<OutputExtra> emptyStoredInfoOutputExtra(OutputPreservation... outputPreservation) {
        FormatIdentifierResponse format = new FormatIdentifierResponse(
            "Plain Text File",
            "text/plain",
            "x-fmt/111",
            ""
        );
        return Stream.of(outputPreservation)
            .map(
                o ->
                    new OutputExtra(
                        o,
                        "binaryGUID",
                        Optional.of(12L),
                        Optional.of("hash"),
                        Optional.of(format),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()
                    )
            )
            .collect(Collectors.toList());
    }

    private WorkflowBatchResults getWorkflowBatchResults(List<OutputExtra> outputExtras) {
        WorkflowBatchResult batchResult = WorkflowBatchResult.of(
            GOT_ID,
            "unitId",
            "BinaryMaster",
            "requestId",
            outputExtras,
            "BinaryMaster",
            "other_binary_strategy",
            Collections.emptyList()
        );
        return new WorkflowBatchResults(Paths.get("tmp"), Collections.singletonList(batchResult));
    }

    private WorkflowBatchResults getWorkflowBatchResults(String targetUse, OutputPreservation... outputPreservation)
        throws InvalidParseOperationException {
        FormatIdentifierResponse format = new FormatIdentifierResponse(
            "Plain Text File",
            "text/plain",
            "x-fmt/111",
            ""
        );
        ExtractedMetadata extractedMetadata = new ExtractedMetadata();
        OtherMetadata otherMetadata = new OtherMetadata();
        otherMetadata.put("GPS", Collections.singletonList("40.714, -74.006"));
        InputStream rawInputStream = getClass().getResourceAsStream("/preservation/rawMetadata_big_result.json");
        JsonNode rawMetadata = JsonHandler.getFromInputStream(rawInputStream);
        extractedMetadata.setRawMetadata(rawMetadata.get("rawMetadata_big_content").asText());
        extractedMetadata.setOtherMetadata(otherMetadata);
        StoredInfoResult value = new StoredInfoResult();
        List<OutputExtra> outputExtras = Stream.of(outputPreservation)
            .map(
                o ->
                    new OutputExtra(
                        o,
                        "binaryGUID",
                        Optional.of(12L),
                        Optional.of("hash"),
                        Optional.of(format),
                        Optional.of(value),
                        Optional.of(extractedMetadata),
                        Optional.empty(),
                        Optional.empty()
                    )
            )
            .collect(Collectors.toList());

        WorkflowBatchResult batchResult = WorkflowBatchResult.of(
            GOT_ID,
            "unitId",
            targetUse,
            "requestId",
            outputExtras,
            "BinaryMaster",
            "other_binary_strategy",
            Collections.emptyList()
        );
        return new WorkflowBatchResults(Paths.get("tmp"), Collections.singletonList(batchResult));
    }

    private OutputPreservation getOutputPreservation(ActionTypePreservation action) {
        OutputPreservation output = new OutputPreservation();
        output.setStatus(PreservationStatus.OK);
        output.setAction(action);
        output.setInputPreservation(new InputPreservation("aeaaaaaaaahiu6xhaaksgalhnbwn3siaaaaq", "fmt/43"));
        output.setFormatIdentification(new DbFormatIdentificationModel("Batman", "text/winner", "x-fmt/42"));
        ExtractedMetadata extractedMetadata = new ExtractedMetadata();
        OtherMetadata otherMetadata = new OtherMetadata();
        otherMetadata.put("GPS", Collections.singletonList("40.714, -74.006"));
        extractedMetadata.setOtherMetadata(otherMetadata);
        output.setExtractedMetadata(extractedMetadata);
        return output;
    }

    private OutputPreservation getOutputPreservationWithRawMetadata(ActionTypePreservation action) {
        OutputPreservation output = new OutputPreservation();
        output.setStatus(PreservationStatus.OK);
        output.setAction(action);
        output.setInputPreservation(new InputPreservation("aeaaaaaaaahiu6xhaaksgalhnbwn3siaaaaq", "fmt/43"));
        output.setFormatIdentification(new DbFormatIdentificationModel("Batman", "text/winner", "x-fmt/42"));
        ExtractedMetadata extractedMetadata = new ExtractedMetadata();
        extractedMetadata.setRawMetadata("rawMetadata : {plop}");
        output.setExtractedMetadata(extractedMetadata);
        return output;
    }

    private OutputPreservation getOutputPreservationWithPlainTextBinary(ActionTypePreservation action) {
        OutputPreservation output = new OutputPreservation();
        output.setStatus(PreservationStatus.OK);
        output.setAction(action);
        output.setInputPreservation(new InputPreservation("aeaaaaaaaahiu6xhaaksgalhnbwn3siaaaaq", "x-fmt/111"));
        output.setFormatIdentification(new DbFormatIdentificationModel("Plain Text File", "text/plain", "x-fmt/111"));
        return output;
    }

    private OutputPreservation getOutputRawMetadata(ActionTypePreservation action) throws Exception {
        OutputPreservation output = new OutputPreservation();
        output.setStatus(PreservationStatus.OK);
        output.setAction(action);
        output.setInputPreservation(new InputPreservation("aeaaaaaaaahiu6xhaaksgalhnbwn3siaaaaq", "fmt/43"));
        output.setFormatIdentification(new DbFormatIdentificationModel("fakeFormat", "text/winner", "x-fmt/42"));
        ExtractedMetadata extractedMetadata = new ExtractedMetadata();
        InputStream rawInputStream = getClass().getResourceAsStream("/preservation/rawMetadata_big_result.json");
        JsonNode rawMetadata = JsonHandler.getFromInputStream(rawInputStream);
        extractedMetadata.setRawMetadata(rawMetadata.get("rawMetadata_big_content").asText());
        output.setExtractedMetadata(extractedMetadata);
        return output;
    }
}
