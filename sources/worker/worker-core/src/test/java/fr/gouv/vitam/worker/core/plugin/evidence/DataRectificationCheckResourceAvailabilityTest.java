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

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
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
import fr.gouv.vitam.worker.core.plugin.preservation.TestWorkerParameter;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DataRectificationCheckResourceAvailabilityTest {

    private final String unitId = "aeaqaaaaaaeaaaababk6gal5sqs3hiyaaaaq";
    private final String unitId2 = "aeaqaaaaaaeaaaababk6gal5sqs3hiyaaaar";
    private final String objectGroupId = "aebaaaaaaaeaaaababk6gal5sqs3g2aaaaaq";
    private final String objectGroupId2 = "aebaaaaaaaeaaaababk6gal5sqs3g2aaaaar";
    private final String objectId = "aeaaaaaaaackemvrabfuealm66lqsiqaaaaq";
    private final String objectId2 = "aeaaaaaaaackemvrabfuealm66lqsiqaaaar";
    private final String objectId3 = "aeaaaaaaaackemvrabfuealm66lqsiqaaaas";
    private final String accessRequestId = "ACCESS_TEST_ID";

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private TestWorkerParameter parameter;

    @Mock
    private StorageClientFactory storageClientFactory;

    @InjectMocks
    private DataRectificationCheckResourceAvailability plugin;

    @Mock
    private HandlerIO handler;

    @Mock
    private StorageClient storageClient;

    @Before
    public void setUp() throws Exception {
        reset(storageClientFactory);
        reset(storageClient);
        given(storageClientFactory.getClient()).willReturn(storageClient);
        plugin = new DataRectificationCheckResourceAvailability(storageClientFactory);
    }

    @After
    public void cleanup() {
        // Restore default batch size
        VitamConfiguration.setBatchSize(1000);
    }

    @Test
    @RunWithCustomExecutor
    public void given_available_resource_should_return_ok() throws Exception {
        // Given
        // Unit
        File file = PropertiesUtils.getResourceFile("DataRectificationCheckResourceAvailability/reportKOUnit.json");
        when(handler.getFileFromWorkspace("alter/" + unitId)).thenReturn(file);

        initWorkflowContext(unitId);
        BulkObjectAvailabilityRequest request = new BulkObjectAvailabilityRequest(DataCategory.UNIT, List.of(unitId));
        BulkObjectAvailabilityResponse response = new BulkObjectAvailabilityResponse(true);
        given(
            storageClient.checkBulkObjectAvailability(eq("default"), any(), refEq(request, "objectNames"))
        ).willReturn(response);

        // When
        List<ItemStatus> pluginResult = plugin.executeList(parameter, handler);

        // Then
        verify(storageClient).checkBulkObjectAvailability(eq("default"), any(), refEq(request, "objectNames"));
        assertThat(pluginResult.size()).isEqualTo(1);
        assertThat(pluginResult.get(0).getGlobalStatus()).isEqualTo(StatusCode.OK);
    }

    @Test
    @RunWithCustomExecutor
    public void given_unavailable_resource_should_throw_retry_exception() throws Exception {
        // Given
        // Unit
        File file = PropertiesUtils.getResourceFile("DataRectificationCheckResourceAvailability/reportKOUnit.json");
        when(handler.getFileFromWorkspace("alter/" + unitId)).thenReturn(file);

        initWorkflowContext(unitId);
        BulkObjectAvailabilityRequest request = new BulkObjectAvailabilityRequest(DataCategory.UNIT, List.of(unitId));
        BulkObjectAvailabilityResponse response = new BulkObjectAvailabilityResponse(false);
        Map<AccessRequestContext, List<String>> accessRequestsCreated = Map.of(
            new AccessRequestContext("default", "default-bis"),
            List.of(accessRequestId)
        );
        ProcessingRetryAsyncException exception = new ProcessingRetryAsyncException(accessRequestsCreated);
        given(
            storageClient.checkBulkObjectAvailability(eq("default"), eq("default-bis"), refEq(request, "objectNames"))
        ).willReturn(response);
        given(
            storageClient.createAccessRequestIfRequired(
                eq("default"),
                eq("default-bis"),
                eq(DataCategory.UNIT),
                eq(List.of(unitId + ".json"))
            )
        ).willReturn(Optional.of(accessRequestId));
        // When + Then
        assertThatThrownBy(() -> plugin.executeList(parameter, handler))
            .isInstanceOf(ProcessingRetryAsyncException.class)
            .isEqualToComparingFieldByField(exception);

        // Then
        verify(storageClient).checkBulkObjectAvailability(
            eq("default"),
            eq("default-bis"),
            any(BulkObjectAvailabilityRequest.class)
        );
        verify(storageClient).createAccessRequestIfRequired(
            eq("default"),
            eq("default-bis"),
            eq(DataCategory.UNIT),
            eq(List.of(unitId + ".json"))
        );
    }

    @Test
    @RunWithCustomExecutor
    public void given_storage_serverclientexception_should_throw_processing_exception() throws Exception {
        // Given
        // Unit
        File file = PropertiesUtils.getResourceFile("DataRectificationCheckResourceAvailability/reportKOUnit.json");
        when(handler.getFileFromWorkspace("alter/" + unitId)).thenReturn(file);

        initWorkflowContext(unitId);
        BulkObjectAvailabilityRequest request = new BulkObjectAvailabilityRequest(DataCategory.UNIT, List.of(unitId));
        given(
            storageClient.checkBulkObjectAvailability(eq("default"), eq("default-bis"), refEq(request, "objectNames"))
        ).willThrow(new StorageServerClientException("Something bad happened"));

        // When + Then
        assertThatThrownBy(() -> plugin.executeList(parameter, handler)).isInstanceOf(ProcessingException.class);

        // Then
        verify(storageClient).checkBulkObjectAvailability(
            eq("default"),
            eq("default-bis"),
            refEq(request, "objectNames")
        );
    }

    @Test
    @RunWithCustomExecutor
    public void given_2_available_resources_same_type_should_return_2_ok() throws Exception {
        // Given
        // Unit
        File file1 = PropertiesUtils.getResourceFile("DataRectificationCheckResourceAvailability/reportKOUnit.json");
        when(handler.getFileFromWorkspace("alter/" + unitId)).thenReturn(file1);
        // Unit
        File file2 = PropertiesUtils.getResourceFile("DataRectificationCheckResourceAvailability/reportKOUnit2.json");
        when(handler.getFileFromWorkspace("alter/" + unitId2)).thenReturn(file2);
        initWorkflowContext(unitId, unitId2);
        BulkObjectAvailabilityRequest request = new BulkObjectAvailabilityRequest(
            DataCategory.UNIT,
            List.of(unitId, unitId2)
        );
        BulkObjectAvailabilityResponse response = new BulkObjectAvailabilityResponse(true);
        given(
            storageClient.checkBulkObjectAvailability(eq("default"), eq("default-bis"), refEq(request, "objectNames"))
        ).willReturn(response);

        // When
        List<ItemStatus> pluginResult = plugin.executeList(parameter, handler);

        // Then
        verify(storageClient).checkBulkObjectAvailability(
            eq("default"),
            eq("default-bis"),
            refEq(request, "objectNames")
        );
        assertThat(pluginResult.size()).isEqualTo(2);
        assertThat(pluginResult.get(0).getGlobalStatus()).isEqualTo(StatusCode.OK);
        assertThat(pluginResult.get(1).getGlobalStatus()).isEqualTo(StatusCode.OK);
    }

    @Test
    @RunWithCustomExecutor
    public void given_2_available_resource_in_diff_strategies_should_return_2_ok() throws Exception {
        // Given
        // Object Group
        File file1 = PropertiesUtils.getResourceFile(
            "DataRectificationCheckResourceAvailability/reportKOObjectGroup.json"
        );
        when(handler.getFileFromWorkspace("alter/" + objectGroupId)).thenReturn(file1);
        // Object Group
        File file2 = PropertiesUtils.getResourceFile(
            "DataRectificationCheckResourceAvailability/reportKOObjectGroup2.json"
        );
        when(handler.getFileFromWorkspace("alter/" + objectGroupId2)).thenReturn(file2);
        initWorkflowContext(objectGroupId, objectGroupId2);

        BulkObjectAvailabilityRequest request1 = new BulkObjectAvailabilityRequest(
            DataCategory.OBJECTGROUP,
            List.of(objectGroupId)
        );
        BulkObjectAvailabilityRequest request2 = new BulkObjectAvailabilityRequest(
            DataCategory.OBJECTGROUP,
            List.of(objectGroupId2)
        );
        BulkObjectAvailabilityResponse response1 = new BulkObjectAvailabilityResponse(true);
        BulkObjectAvailabilityResponse response2 = new BulkObjectAvailabilityResponse(true);
        given(
            storageClient.checkBulkObjectAvailability(eq("default"), eq("default-bis"), refEq(request1, "objectNames"))
        ).willReturn(response1);
        given(
            storageClient.checkBulkObjectAvailability(eq("other"), eq("default-bis"), refEq(request2, "objectNames"))
        ).willReturn(response2);

        // When
        List<ItemStatus> pluginResult = plugin.executeList(parameter, handler);

        // Then
        verify(storageClient).checkBulkObjectAvailability(
            eq("default"),
            eq("default-bis"),
            refEq(request1, "objectNames")
        );
        verify(storageClient).checkBulkObjectAvailability(
            eq("other"),
            eq("default-bis"),
            refEq(request2, "objectNames")
        );
        assertThat(pluginResult.size()).isEqualTo(2);
        assertThat(pluginResult.get(0).getGlobalStatus()).isEqualTo(StatusCode.OK);
        assertThat(pluginResult.get(1).getGlobalStatus()).isEqualTo(StatusCode.OK);
    }

    @Test
    @RunWithCustomExecutor
    public void given_1_on_3_unavailable_resource_in_diff_context_should_return_1_accessRequest() throws Exception {
        // Given
        // Object
        File file1 = PropertiesUtils.getResourceFile("DataRectificationCheckResourceAvailability/reportKOObject.json");
        when(handler.getFileFromWorkspace("alter/" + objectId)).thenReturn(file1);
        // Object
        File file2 = PropertiesUtils.getResourceFile("DataRectificationCheckResourceAvailability/reportKOObject2.json");
        when(handler.getFileFromWorkspace("alter/" + objectId2)).thenReturn(file2);
        // Object
        File file3 = PropertiesUtils.getResourceFile("DataRectificationCheckResourceAvailability/reportKOObject3.json");
        when(handler.getFileFromWorkspace("alter/" + objectId3)).thenReturn(file3);
        initWorkflowContext(objectId, objectId2, objectId3);

        BulkObjectAvailabilityRequest request1 = new BulkObjectAvailabilityRequest(
            DataCategory.OBJECT,
            List.of(objectId, objectId2)
        );
        BulkObjectAvailabilityRequest request2 = new BulkObjectAvailabilityRequest(
            DataCategory.OBJECT,
            List.of(objectId3)
        );
        BulkObjectAvailabilityResponse response1 = new BulkObjectAvailabilityResponse(true);
        BulkObjectAvailabilityResponse response2 = new BulkObjectAvailabilityResponse(false);
        Map<AccessRequestContext, List<String>> accessRequestsCreated = Map.of(
            new AccessRequestContext("other", "default"),
            List.of(accessRequestId)
        );
        ProcessingRetryAsyncException exception = new ProcessingRetryAsyncException(accessRequestsCreated);
        given(
            storageClient.checkBulkObjectAvailability(eq("default"), eq("default-bis"), refEq(request1, "objectNames"))
        ).willReturn(response1);
        given(
            storageClient.checkBulkObjectAvailability(eq("other"), eq("default"), refEq(request2, "objectNames"))
        ).willReturn(response2);
        given(
            storageClient.createAccessRequestIfRequired(
                eq("other"),
                eq("default"),
                eq(DataCategory.OBJECT),
                eq(List.of(objectId3))
            )
        ).willReturn(Optional.of(accessRequestId));

        // When
        assertThatThrownBy(() -> {
            plugin.executeList(parameter, handler);
        })
            .isInstanceOf(ProcessingRetryAsyncException.class)
            .isEqualToComparingFieldByField(exception);

        // Then
        verify(storageClient).checkBulkObjectAvailability(
            eq("default"),
            eq("default-bis"),
            refEq(request1, "objectNames")
        );
        verify(storageClient).checkBulkObjectAvailability(eq("other"), eq("default"), refEq(request2, "objectNames"));
        verify(storageClient).createAccessRequestIfRequired(
            eq("other"),
            eq("default"),
            eq(DataCategory.OBJECT),
            eq(List.of(objectId3))
        );
    }

    @Test
    @RunWithCustomExecutor
    public void given_3_on_3_unavailable_resource_in_diff_context_type_should_return_3_accessRequest_for_same_context()
        throws Exception {
        // Given
        // Unit
        File file1 = PropertiesUtils.getResourceFile("DataRectificationCheckResourceAvailability/reportKOUnit.json");
        when(handler.getFileFromWorkspace("alter/" + unitId)).thenReturn(file1);
        // Object Group
        File file2 = PropertiesUtils.getResourceFile(
            "DataRectificationCheckResourceAvailability/reportKOObjectGroup.json"
        );
        when(handler.getFileFromWorkspace("alter/" + objectGroupId)).thenReturn(file2);
        // Object
        File file3 = PropertiesUtils.getResourceFile("DataRectificationCheckResourceAvailability/reportKOObject.json");
        when(handler.getFileFromWorkspace("alter/" + objectId)).thenReturn(file3);
        initWorkflowContext(unitId, objectGroupId, objectId);

        BulkObjectAvailabilityRequest request1 = new BulkObjectAvailabilityRequest(
            DataCategory.UNIT,
            List.of(unitId + ".json")
        );
        BulkObjectAvailabilityRequest request2 = new BulkObjectAvailabilityRequest(
            DataCategory.OBJECTGROUP,
            List.of(objectGroupId + ".json")
        );
        BulkObjectAvailabilityRequest request3 = new BulkObjectAvailabilityRequest(
            DataCategory.OBJECT,
            List.of(objectId)
        );
        BulkObjectAvailabilityResponse response = new BulkObjectAvailabilityResponse(false);
        given(
            storageClient.checkBulkObjectAvailability(eq("default"), eq("default-bis"), refEq(request1, "objectNames"))
        ).willReturn(response);
        given(
            storageClient.checkBulkObjectAvailability(eq("default"), eq("default-bis"), refEq(request2, "objectNames"))
        ).willReturn(response);
        given(
            storageClient.checkBulkObjectAvailability(eq("default"), eq("default-bis"), refEq(request3, "objectNames"))
        ).willReturn(response);

        given(
            storageClient.createAccessRequestIfRequired(
                eq("default"),
                eq("default-bis"),
                eq(DataCategory.UNIT),
                eq(List.of(unitId + ".json"))
            )
        ).willReturn(Optional.of("accessRequestId1"));

        given(
            storageClient.createAccessRequestIfRequired(
                eq("default"),
                eq("default-bis"),
                eq(DataCategory.OBJECTGROUP),
                eq(List.of(objectGroupId + ".json"))
            )
        ).willReturn(Optional.of("accessRequestId2"));

        given(
            storageClient.createAccessRequestIfRequired(
                eq("default"),
                eq("default-bis"),
                eq(DataCategory.OBJECT),
                eq(List.of(objectId))
            )
        ).willReturn(Optional.of("accessRequestId3"));

        // When
        Throwable thrown = catchThrowable(() -> plugin.executeList(parameter, handler));

        // Then
        assertThat(thrown)
            .isInstanceOf(ProcessingRetryAsyncException.class)
            .hasFieldOrProperty("accessRequestIdByContext");
        ProcessingRetryAsyncException exc = (ProcessingRetryAsyncException) thrown;
        assertThat(exc.getAccessRequestIdByContext()).containsKey(new AccessRequestContext("default", "default-bis"));
        assertThat(exc.getAccessRequestIdByContext().get(new AccessRequestContext("default", "default-bis"))).contains(
            "accessRequestId1"
        );
        assertThat(exc.getAccessRequestIdByContext().get(new AccessRequestContext("default", "default-bis"))).contains(
            "accessRequestId2"
        );
        assertThat(exc.getAccessRequestIdByContext().get(new AccessRequestContext("default", "default-bis"))).contains(
            "accessRequestId3"
        );

        verify(storageClient, times(3)).checkBulkObjectAvailability(
            eq("default"),
            eq("default-bis"),
            any(BulkObjectAvailabilityRequest.class)
        );
        verify(storageClient).createAccessRequestIfRequired(
            eq("default"),
            eq("default-bis"),
            eq(DataCategory.UNIT),
            eq(List.of(unitId + ".json"))
        );
        verify(storageClient).createAccessRequestIfRequired(
            eq("default"),
            eq("default-bis"),
            eq(DataCategory.OBJECTGROUP),
            eq(List.of(objectGroupId + ".json"))
        );
        verify(storageClient).createAccessRequestIfRequired(
            eq("default"),
            eq("default-bis"),
            eq(DataCategory.OBJECT),
            eq(List.of(objectId))
        );
    }

    @Test
    @RunWithCustomExecutor
    public void given_3_on_3_unavailable_resource_in_same_context_type_batch_size_1_should_return_2_accessRequest_for_same_context()
        throws Exception {
        // Given
        VitamConfiguration.setBatchSize(1);
        // Object
        File file1 = PropertiesUtils.getResourceFile("DataRectificationCheckResourceAvailability/reportKOObject.json");
        when(handler.getFileFromWorkspace("alter/" + objectId)).thenReturn(file1);
        // Object
        File file2 = PropertiesUtils.getResourceFile("DataRectificationCheckResourceAvailability/reportKOObject2.json");
        when(handler.getFileFromWorkspace("alter/" + objectId2)).thenReturn(file2);
        // Object
        File file3 = PropertiesUtils.getResourceFile("DataRectificationCheckResourceAvailability/reportKOObject4.json");
        when(handler.getFileFromWorkspace("alter/" + objectId3)).thenReturn(file3);
        initWorkflowContext(objectId, objectId2, objectId3);

        BulkObjectAvailabilityRequest requestObject = new BulkObjectAvailabilityRequest(DataCategory.OBJECT, List.of());
        BulkObjectAvailabilityResponse response1 = new BulkObjectAvailabilityResponse(false);
        BulkObjectAvailabilityResponse response2 = new BulkObjectAvailabilityResponse(false);
        BulkObjectAvailabilityResponse response3 = new BulkObjectAvailabilityResponse(false);
        given(
            storageClient.checkBulkObjectAvailability(
                eq("default"),
                eq("default-bis"),
                refEq(requestObject, "objectNames")
            )
        ).willReturn(response1, response2, response3);
        given(
            storageClient.createAccessRequestIfRequired(
                eq("default"),
                eq("default-bis"),
                eq(DataCategory.OBJECT),
                eq(List.of(objectId))
            )
        ).willReturn(Optional.of("accessRequestId1"));
        given(
            storageClient.createAccessRequestIfRequired(
                eq("default"),
                eq("default-bis"),
                eq(DataCategory.OBJECT),
                eq(List.of(objectId2))
            )
        ).willReturn(Optional.of("accessRequestId2"));
        given(
            storageClient.createAccessRequestIfRequired(
                eq("default"),
                eq("default-bis"),
                eq(DataCategory.OBJECT),
                eq(List.of(objectId3))
            )
        ).willReturn(Optional.of("accessRequestId3"));

        // When
        Throwable thrown = catchThrowable(() -> plugin.executeList(parameter, handler));

        // Then
        assertThat(thrown)
            .isInstanceOf(ProcessingRetryAsyncException.class)
            .hasFieldOrProperty("accessRequestIdByContext");
        ProcessingRetryAsyncException exc = (ProcessingRetryAsyncException) thrown;
        assertThat(exc.getAccessRequestIdByContext()).containsKey(new AccessRequestContext("default", "default-bis"));
        assertThat(exc.getAccessRequestIdByContext().get(new AccessRequestContext("default", "default-bis"))).contains(
            "accessRequestId1"
        );
        assertThat(exc.getAccessRequestIdByContext().get(new AccessRequestContext("default", "default-bis"))).contains(
            "accessRequestId2"
        );
        assertThat(exc.getAccessRequestIdByContext().get(new AccessRequestContext("default", "default-bis"))).contains(
            "accessRequestId3"
        );

        verify(storageClient, times(3)).checkBulkObjectAvailability(
            eq("default"),
            eq("default-bis"),
            any(BulkObjectAvailabilityRequest.class)
        );
        verify(storageClient).createAccessRequestIfRequired(
            eq("default"),
            eq("default-bis"),
            eq(DataCategory.OBJECT),
            eq(List.of(objectId))
        );
        verify(storageClient).createAccessRequestIfRequired(
            eq("default"),
            eq("default-bis"),
            eq(DataCategory.OBJECT),
            eq(List.of(objectId2))
        );
        verify(storageClient).createAccessRequestIfRequired(
            eq("default"),
            eq("default-bis"),
            eq(DataCategory.OBJECT),
            eq(List.of(objectId3))
        );
    }

    private void initWorkflowContext(String... filenames) {
        parameter = workerParameterBuilder()
            .withContainerName("CONTAINER_NAME_TEST")
            .withRequestId("REQUEST_ID_TEST")
            .build();
        List<String> objectNameList = List.of(filenames);
        parameter.setObjectNameList(objectNameList);
    }
}
