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
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.ActionTypePreservation;
import fr.gouv.vitam.common.model.administration.preservation.ActionPreservation;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.processing.common.async.AccessRequestContext;
import fr.gouv.vitam.processing.common.async.ProcessingRetryAsyncException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.request.BulkObjectAvailabilityRequest;
import fr.gouv.vitam.storage.engine.common.model.response.BulkObjectAvailabilityResponse;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.plugin.preservation.model.PreservationDistributionLine;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static fr.gouv.vitam.worker.core.plugin.preservation.TestWorkerParameter.TestWorkerParameterBuilder.workerParameterBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

public class PreservationCheckResourceAvailabilityTest {

    private final String objectId = "TEST_ID";
    private final String accessRequestId = "ACCESS_TEST_ID";

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    private TestWorkerParameter parameter;
    private PreservationCheckResourceAvailability plugin;

    @Mock
    private StorageClientFactory storageClientFactory;

    @Mock
    private StorageClient storageClient;

    private final HandlerIO handler = new TestHandlerIO();

    @Before
    public void setup() throws Exception {
        reset(storageClientFactory);
        reset(storageClient);
        given(storageClientFactory.getClient()).willReturn(storageClient);

        plugin = new PreservationCheckResourceAvailability(storageClientFactory);

        VitamThreadUtils.getVitamSession().setTenantId(0);
    }

    @Test
    @RunWithCustomExecutor
    public void given_available_resource_should_return_ok() throws Exception {
        // Given
        initOneLineWorkflowContext();
        BulkObjectAvailabilityRequest request = new BulkObjectAvailabilityRequest(
            DataCategory.OBJECT,
            List.of(objectId)
        );
        BulkObjectAvailabilityResponse response = new BulkObjectAvailabilityResponse(true);
        given(
            storageClient.checkBulkObjectAvailability(eq("other_binary_strategy"), any(), refEq(request, "objectNames"))
        ).willReturn(response);

        // When
        List<ItemStatus> pluginResult = plugin.executeList(parameter, handler);

        // Then
        verify(storageClient).checkBulkObjectAvailability(
            eq("other_binary_strategy"),
            any(),
            refEq(request, "objectNames")
        );
        assertThat(pluginResult.size()).isEqualTo(1);
        assertThat(pluginResult.get(0).getGlobalStatus()).isEqualTo(StatusCode.OK);
    }

    @Test
    @RunWithCustomExecutor
    public void given_unavailable_resource_should_throw_retry_exception() throws Exception {
        // Given
        initOneLineWorkflowContext();
        BulkObjectAvailabilityRequest request = new BulkObjectAvailabilityRequest(
            DataCategory.OBJECT,
            List.of(objectId)
        );
        BulkObjectAvailabilityResponse response = new BulkObjectAvailabilityResponse(false);
        Map<AccessRequestContext, List<String>> accessRequestsCreated = Map.of(
            new AccessRequestContext("other_binary_strategy", null),
            List.of(accessRequestId)
        );
        ProcessingRetryAsyncException exception = new ProcessingRetryAsyncException(accessRequestsCreated);
        given(
            storageClient.checkBulkObjectAvailability(eq("other_binary_strategy"), any(), refEq(request, "objectNames"))
        ).willReturn(response);
        given(
            storageClient.createAccessRequestIfRequired(
                eq("other_binary_strategy"),
                eq(null),
                eq(DataCategory.OBJECT),
                eq(List.of(objectId))
            )
        ).willReturn(Optional.of(accessRequestId));

        // When + Then
        assertThatThrownBy(() -> plugin.executeList(parameter, handler))
            .isInstanceOf(ProcessingRetryAsyncException.class)
            .isEqualToComparingFieldByField(exception);

        // Then
        verify(storageClient).checkBulkObjectAvailability(
            eq("other_binary_strategy"),
            any(),
            refEq(request, "objectNames")
        );
        verify(storageClient).createAccessRequestIfRequired(
            eq("other_binary_strategy"),
            eq(null),
            eq(DataCategory.OBJECT),
            eq(List.of(objectId))
        );
    }

    @Test
    @RunWithCustomExecutor
    public void given_storage_serverclientexception_should_throw_processing_exception() throws Exception {
        // Given
        initOneLineWorkflowContext();
        BulkObjectAvailabilityRequest request = new BulkObjectAvailabilityRequest(
            DataCategory.OBJECT,
            List.of(objectId)
        );
        given(
            storageClient.checkBulkObjectAvailability(eq("other_binary_strategy"), any(), refEq(request, "objectNames"))
        ).willThrow(new StorageServerClientException("Something bad happened"));

        // When + Then
        assertThatThrownBy(() -> plugin.executeList(parameter, handler)).isInstanceOf(ProcessingException.class);

        // Then
        verify(storageClient).checkBulkObjectAvailability(
            eq("other_binary_strategy"),
            any(),
            refEq(request, "objectNames")
        );
    }

    @Test
    @RunWithCustomExecutor
    public void given_3_available_resource_should_return_3_ok() throws Exception {
        // Given
        initMultiLinesWorkflowContext(3);
        BulkObjectAvailabilityRequest request = new BulkObjectAvailabilityRequest(
            DataCategory.OBJECT,
            List.of(objectId + "0", objectId + "1", objectId + "2")
        );
        BulkObjectAvailabilityResponse response = new BulkObjectAvailabilityResponse(true);
        given(
            storageClient.checkBulkObjectAvailability(eq("other_binary_strategy"), any(), refEq(request, "objectNames"))
        ).willReturn(response);

        // When
        List<ItemStatus> pluginResult = plugin.executeList(parameter, handler);

        // Then
        verify(storageClient).checkBulkObjectAvailability(
            eq("other_binary_strategy"),
            any(),
            refEq(request, "objectNames")
        );
        assertThat(pluginResult.size()).isEqualTo(3);
        assertThat(pluginResult.get(0).getGlobalStatus()).isEqualTo(StatusCode.OK);
        assertThat(pluginResult.get(1).getGlobalStatus()).isEqualTo(StatusCode.OK);
        assertThat(pluginResult.get(2).getGlobalStatus()).isEqualTo(StatusCode.OK);
    }

    @Test
    @RunWithCustomExecutor
    public void given_3_available_resource_in_diff_strategies_should_return_3_ok() throws Exception {
        // Given
        initMultiLinesWorkflowContextByStrategies("strategy_1", "strategy_2", "strategy_1");

        BulkObjectAvailabilityRequest request1 = new BulkObjectAvailabilityRequest(
            DataCategory.OBJECT,
            List.of(objectId + "0", objectId + "2")
        );
        BulkObjectAvailabilityRequest request2 = new BulkObjectAvailabilityRequest(
            DataCategory.OBJECT,
            List.of(objectId + "1")
        );
        BulkObjectAvailabilityResponse response1 = new BulkObjectAvailabilityResponse(true);
        BulkObjectAvailabilityResponse response2 = new BulkObjectAvailabilityResponse(true);
        given(
            storageClient.checkBulkObjectAvailability(eq("strategy_1"), any(), refEq(request1, "objectNames"))
        ).willReturn(response1);
        given(
            storageClient.checkBulkObjectAvailability(eq("strategy_2"), any(), refEq(request2, "objectNames"))
        ).willReturn(response2);

        // When
        List<ItemStatus> pluginResult = plugin.executeList(parameter, handler);

        // Then
        verify(storageClient).checkBulkObjectAvailability(eq("strategy_1"), any(), refEq(request1, "objectNames"));
        verify(storageClient).checkBulkObjectAvailability(eq("strategy_2"), any(), refEq(request2, "objectNames"));
        assertThat(pluginResult.size()).isEqualTo(3);
        assertThat(pluginResult.get(0).getGlobalStatus()).isEqualTo(StatusCode.OK);
        assertThat(pluginResult.get(1).getGlobalStatus()).isEqualTo(StatusCode.OK);
        assertThat(pluginResult.get(2).getGlobalStatus()).isEqualTo(StatusCode.OK);
    }

    @Test
    @RunWithCustomExecutor
    public void given_1_on_3_unavailable_resource_in_diff_strategies_should_return_1_accessRequest() throws Exception {
        // Given
        initMultiLinesWorkflowContextByStrategies("strategy_1", "strategy_2", "strategy_1");

        BulkObjectAvailabilityRequest request1 = new BulkObjectAvailabilityRequest(
            DataCategory.OBJECT,
            List.of(objectId + "0", objectId + "2")
        );
        BulkObjectAvailabilityRequest request2 = new BulkObjectAvailabilityRequest(
            DataCategory.OBJECT,
            List.of(objectId + "1")
        );
        BulkObjectAvailabilityResponse response1 = new BulkObjectAvailabilityResponse(true);
        BulkObjectAvailabilityResponse response2 = new BulkObjectAvailabilityResponse(false);
        given(
            storageClient.checkBulkObjectAvailability(eq("strategy_1"), any(), refEq(request1, "objectNames"))
        ).willReturn(response1);
        given(
            storageClient.checkBulkObjectAvailability(eq("strategy_2"), any(), refEq(request2, "objectNames"))
        ).willReturn(response2);
        given(
            storageClient.createAccessRequestIfRequired(
                eq("strategy_2"),
                eq(null),
                eq(DataCategory.OBJECT),
                eq(List.of(objectId + "1"))
            )
        ).willReturn(Optional.of(accessRequestId));

        // When
        Throwable thrown = catchThrowable(() -> plugin.executeList(parameter, handler));

        // Then
        assertThat(thrown)
            .isInstanceOf(ProcessingRetryAsyncException.class)
            .hasFieldOrProperty("accessRequestIdByContext");
        ProcessingRetryAsyncException exc = (ProcessingRetryAsyncException) thrown;
        assertThat(exc.getAccessRequestIdByContext()).containsKey(new AccessRequestContext("strategy_2", null));
        assertThat(exc.getAccessRequestIdByContext().get(new AccessRequestContext("strategy_2", null))).contains(
            accessRequestId
        );

        verify(storageClient).checkBulkObjectAvailability(eq("strategy_1"), any(), refEq(request1, "objectNames"));
        verify(storageClient).checkBulkObjectAvailability(eq("strategy_2"), any(), refEq(request2, "objectNames"));
        verify(storageClient).createAccessRequestIfRequired(
            eq("strategy_2"),
            eq(null),
            eq(DataCategory.OBJECT),
            eq(List.of(objectId + "1"))
        );
    }

    private void initOneLineWorkflowContext() throws InvalidParseOperationException {
        parameter = workerParameterBuilder()
            .withContainerName("CONTAINER_NAME_TEST")
            .withRequestId("REQUEST_ID_TEST")
            .build();
        PreservationDistributionLine preservationDistributionLine = getPreservationDistributionLine(
            "gotId",
            "other_binary_strategy",
            objectId
        );
        parameter.setObjectNameList(Collections.singletonList("gotId"));
        parameter.setObjectMetadataList(
            Collections.singletonList(JsonHandler.toJsonNode(preservationDistributionLine))
        );
    }

    private void initMultiLinesWorkflowContextByStrategies(String... strategies) throws InvalidParseOperationException {
        parameter = workerParameterBuilder()
            .withContainerName("CONTAINER_NAME_TEST")
            .withRequestId("REQUEST_ID_TEST")
            .build();
        List<JsonNode> objectMetadataList = new ArrayList<>();
        List<String> objectNameList = new ArrayList<>();
        for (int index = 0; index < strategies.length; index++) {
            PreservationDistributionLine preservationDistributionLine = getPreservationDistributionLine(
                "gotId" + index,
                strategies[index],
                objectId + index
            );
            objectMetadataList.add(JsonHandler.toJsonNode(preservationDistributionLine));
            objectNameList.add("gotId" + index);
        }
        parameter.setObjectNameList(objectNameList);
        parameter.setObjectMetadataList(objectMetadataList);
    }

    private void initMultiLinesWorkflowContext(int nbLines) throws InvalidParseOperationException {
        parameter = workerParameterBuilder()
            .withContainerName("CONTAINER_NAME_TEST")
            .withRequestId("REQUEST_ID_TEST")
            .build();
        List<JsonNode> objectMetadataList = new ArrayList<>();
        List<String> objectNameList = new ArrayList<>();
        for (int index = 0; index < nbLines; index++) {
            PreservationDistributionLine preservationDistributionLine = getPreservationDistributionLine(
                "gotId" + index,
                "other_binary_strategy",
                objectId + index
            );
            objectMetadataList.add(JsonHandler.toJsonNode(preservationDistributionLine));
            objectNameList.add("gotId" + index);
        }
        parameter.setObjectNameList(objectNameList);
        parameter.setObjectMetadataList(objectMetadataList);
    }

    private PreservationDistributionLine getPreservationDistributionLine(
        String id,
        String sourceStrategy,
        String objectId
    ) {
        String griffinId = "griffinId-my-test";
        return new PreservationDistributionLine(
            "fmt/43",
            "photo.jpg",
            Collections.singletonList(new ActionPreservation(ActionTypePreservation.ANALYSE)),
            "unitId",
            griffinId,
            objectId,
            true,
            45,
            id,
            "BinaryMaster",
            "BinaryMaster",
            sourceStrategy,
            "ScenarioId",
            "griffinIdentifier",
            new HashSet<>(Arrays.asList("unitId", "otherUnitIdBatman"))
        );
    }
}
