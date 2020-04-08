/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.ActionTypePreservation;
import fr.gouv.vitam.common.model.administration.preservation.ActionPreservation;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.core.model.UpdateUnit;
import fr.gouv.vitam.metadata.core.model.UpdateUnitKey;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.plugin.preservation.model.PreservationDistributionLine;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.List;

import static fr.gouv.vitam.common.model.StatusCode.FATAL;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.worker.core.plugin.preservation.TestWorkerParameter.TestWorkerParameterBuilder.workerParameterBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;

public class PreservationInsertionAuMetadataTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @Mock
    private MetaDataClientFactory metaDataClientFactory;

    @Mock
    private MetaDataClient metaDataClient;

    private PreservationInsertionAuMetadata plugin;

    private final TestWorkerParameter parameter = workerParameterBuilder()
            .withContainerName("CONTAINER_NAME_TEST")
            .withRequestId("REQUEST_ID_TEST")
            .build();

    private HandlerIO handler = new TestHandlerIO();

    @Before
    public void setUp() throws Exception {
        given(metaDataClientFactory.getClient()).willReturn(metaDataClient);
        plugin = new PreservationInsertionAuMetadata(metaDataClientFactory);

        PreservationDistributionLine preservationDistributionLine = new PreservationDistributionLine("fmt/43", "photo.jpg",
                Collections.singletonList(new ActionPreservation(ActionTypePreservation.EXTRACT_AU)), "test", "unitId", "objectId",
                true, 45, "auId", "BinaryMaster", "BinaryMaster", "other_binary_strategy",
                "ScenarioId", "griffinIdentifier", Collections.singleton("key"));
        parameter.setObjectNameList(Collections.singletonList("unitId"));
        parameter.setObjectMetadataList(Collections.singletonList(JsonHandler.toJsonNode(preservationDistributionLine)));
    }

    @Test
    @RunWithCustomExecutor
    public void should_update_extracted_Metadata() throws Exception {
        // Given
        RequestResponseOK<JsonNode> responseOK = new RequestResponseOK<>();
        JsonNode updatedUnit = JsonHandler.toJsonNode(
                new UpdateUnit(
                        "UNIT_ID",
                        StatusCode.OK,
                        UpdateUnitKey.UNIT_METADATA_UPDATE,
                        "Extracted  Metada updated successfully.",
                        "UNKNOWN diff"
                )
        );
        responseOK.addResult(updatedUnit);
        RequestResponseOK<Object> objectRequestResponseOK = new RequestResponseOK<>().addResult(updatedUnit);
        given(metaDataClient.updateUnitById(any(), eq("unitId"))).willReturn(JsonHandler.toJsonNode(objectRequestResponseOK));

        // When
        List<ItemStatus> itemStatuses = plugin.executeList(parameter, handler);

        // Then
        assertThat(itemStatuses.size()).isEqualTo(1);
        assertThat(itemStatuses.get(0)).extracting(ItemStatus::getGlobalStatus).isEqualTo(OK);
        assertThat(itemStatuses.get(0)).extracting(ItemStatus::getItemId).isEqualTo("PRESERVATION_INSERTION_AU_METADATA");
    }

    @Test
    @RunWithCustomExecutor
    public void should_throw_MetaData_exception_when_update_extracted_Metadata() throws Exception {
        // Given
        doThrow(new MetaDataExecutionException("Exception when updating unit by MDClient")).
                when(metaDataClient).updateUnitById(any(), eq("unitId"));

        // When
        ThrowingCallable shouldThrow = () -> plugin.executeList(parameter, handler);

        // Then
        assertThatThrownBy(shouldThrow).isInstanceOf(ProcessingException.class);
    }

    @Test
    @RunWithCustomExecutor
    public void should_update_includes_operation_that_modify_it() throws Exception {
        // Given
        RequestResponseOK<JsonNode> responseOK = new RequestResponseOK<>();
        JsonNode updatedUnit = JsonHandler.toJsonNode(
            new UpdateUnit(
                "RESPONSE_ID",
                StatusCode.OK,
                UpdateUnitKey.UNIT_METADATA_UPDATE,
                "Extracted  Metada updated successfully.",
                "UNKNOWN diff"
            )
        );
        responseOK.addResult(updatedUnit);
        ArgumentCaptor<JsonNode> captor = ArgumentCaptor.forClass(JsonNode.class);
        RequestResponseOK<Object> objectRequestResponseOK = new RequestResponseOK<>().addResult(updatedUnit);
        given(metaDataClient.updateUnitById(captor.capture(), eq("unitId"))).willReturn(JsonHandler.toJsonNode(objectRequestResponseOK));

        // When
        plugin.executeList(parameter, handler);

        // Then
        assertThat(captor.getValue().get("$action").get(1).get("$push").get("#operations").get(0).asText()).isEqualTo(parameter.getRequestId());
    }

    @Test
    public void should_throw_if_wrong_status() throws Exception {
        // Given
        RequestResponseOK<JsonNode> responseOK = new RequestResponseOK<>();
        JsonNode updatedUnit = JsonHandler.toJsonNode(
            new UpdateUnit(
                "RESPONSE_ID",
                StatusCode.STARTED,
                UpdateUnitKey.UNIT_METADATA_UPDATE,
                "Extracted  Metada updated successfully.",
                "UNKNOWN diff"
            )
        );
        responseOK.addResult(updatedUnit);
        ArgumentCaptor<JsonNode> captor = ArgumentCaptor.forClass(JsonNode.class);
        RequestResponseOK<Object> objectRequestResponseOK = new RequestResponseOK<>().addResult(updatedUnit);
        given(metaDataClient.updateUnitById(captor.capture(), eq("unitId"))).willReturn(JsonHandler.toJsonNode(objectRequestResponseOK));

        // When
        ThrowingCallable shouldThrow = () -> plugin.executeList(parameter, handler);

        // Then
        assertThatThrownBy(shouldThrow).isInstanceOf(VitamRuntimeException.class);
    }

    @Test
    public void should_add_item_status_KO_or_FATAL_when_update_status_KO_or_FATAL() throws Exception {
        // Given
        RequestResponseOK<JsonNode> responseOK = new RequestResponseOK<>();
        JsonNode updatedUnit = JsonHandler.toJsonNode(
            new UpdateUnit(
                "UNIT_ID",
                StatusCode.FATAL,
                UpdateUnitKey.UNIT_METADATA_UPDATE,
                "Extracted  Metada updated successfully.",
                "UNKNOWN diff"
            )
        );
        responseOK.addResult(updatedUnit);
        RequestResponseOK<Object> objectRequestResponseOK = new RequestResponseOK<>().addResult(updatedUnit);
        given(metaDataClient.updateUnitById(any(), eq("unitId"))).willReturn(JsonHandler.toJsonNode(objectRequestResponseOK));

        // When
        List<ItemStatus> itemStatuses = plugin.executeList(parameter, handler);

        // Then
        assertThat(itemStatuses.size()).isEqualTo(1);
        assertThat(itemStatuses.get(0)).extracting(ItemStatus::getGlobalStatus).isEqualTo(FATAL);
        assertThat(itemStatuses.get(0)).extracting(ItemStatus::getItemId).isEqualTo("PRESERVATION_INSERTION_AU_METADATA");
    }
}