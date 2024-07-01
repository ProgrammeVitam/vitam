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

import fr.gouv.vitam.batch.report.client.BatchReportClient;
import fr.gouv.vitam.batch.report.client.BatchReportClientFactory;
import fr.gouv.vitam.batch.report.model.PreservationStatus;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ExtractedMetadata;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.administration.ActionTypePreservation;
import fr.gouv.vitam.common.model.administration.preservation.ActionPreservation;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.plugin.preservation.model.ExtractedMetadataForAu;
import fr.gouv.vitam.worker.core.plugin.preservation.model.OutputPreservation;
import fr.gouv.vitam.worker.core.plugin.preservation.model.PreservationDistributionLine;
import fr.gouv.vitam.worker.core.plugin.preservation.model.WorkflowBatchResult;
import fr.gouv.vitam.worker.core.plugin.preservation.model.WorkflowBatchResults;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.common.model.StatusCode.UNKNOWN;
import static fr.gouv.vitam.worker.core.plugin.preservation.TestWorkerParameter.TestWorkerParameterBuilder.workerParameterBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

public class PreservationExtractionAUPluginTest {

    private final TestWorkerParameter parameter = workerParameterBuilder()
        .withContainerName("CONTAINER_NAME_TEST")
        .withRequestId("REQUEST_ID_TEST")
        .build();

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private BatchReportClientFactory batchReportFactory;

    @Mock
    private BatchReportClient batchReportClient;

    @Captor
    ArgumentCaptor<List<ExtractedMetadata>> listArgumentCaptor;

    private PreservationExtractionAUPlugin plugin;

    private HandlerIO handler = new TestHandlerIO();

    @Before
    public void setUp() throws Exception {
        given(batchReportFactory.getClient()).willReturn(batchReportClient);

        PreservationDistributionLine preservationDistributionLine = new PreservationDistributionLine(
            "fmt/43",
            "photo.jpg",
            Collections.singletonList(new ActionPreservation(ActionTypePreservation.ANALYSE)),
            "test",
            "unitId",
            "TEST_ID",
            true,
            45,
            "gotId",
            "BinaryMaster",
            "BinaryMaster",
            "other_binary_strategy",
            "ScenarioId",
            "griffinIdentifier",
            Collections.singleton("key")
        );
        parameter.setObjectNameList(Collections.singletonList("gotId"));
        parameter.setObjectMetadataList(
            Collections.singletonList(JsonHandler.toJsonNode(preservationDistributionLine))
        );

        VitamThreadUtils.getVitamSession().setTenantId(0);

        plugin = new PreservationExtractionAUPlugin(batchReportFactory);
    }

    @Test
    @RunWithCustomExecutor
    public void should_extract_AU_Metadata_OK() throws Exception {
        // Given
        ExtractedMetadataForAu extractedMetadataForAu = new ExtractedMetadataForAu();
        extractedMetadataForAu.put("key", Collections.singletonList("value"));

        OutputPreservation output = new OutputPreservation();
        output.setStatus(PreservationStatus.OK);
        output.setAction(ActionTypePreservation.EXTRACT_AU);
        output.setOutputName("outputName");
        output.setExtractedMetadataAU(extractedMetadataForAu);
        List<WorkflowBatchResult.OutputExtra> outputExtras = Arrays.asList(WorkflowBatchResult.OutputExtra.of(output));
        List<WorkflowBatchResult> workflowBatchResults = Collections.singletonList(
            WorkflowBatchResult.of("", "", "", "", outputExtras, "", "", Collections.singletonList("unitId"))
        );
        WorkflowBatchResults batchResults = new WorkflowBatchResults(Paths.get("tmp"), workflowBatchResults);
        handler.addOutputResult(0, batchResults);

        // When
        List<ItemStatus> itemStatuses = plugin.executeList(parameter, handler);

        // Then
        assertThat(itemStatuses).extracting(ItemStatus::getGlobalStatus).containsOnly(OK);
    }

    @Test
    @RunWithCustomExecutor
    public void should_extract_AU_Metadata_UNKNOWN() throws Exception {
        // Given
        ExtractedMetadataForAu extractedMetadataForAu = new ExtractedMetadataForAu();
        extractedMetadataForAu.put("key", Collections.singletonList("value"));

        OutputPreservation output = new OutputPreservation();
        output.setStatus(PreservationStatus.OK);
        output.setAction(ActionTypePreservation.IDENTIFY);
        output.setOutputName("outputName");
        output.setExtractedMetadataAU(extractedMetadataForAu);
        List<WorkflowBatchResult.OutputExtra> outputExtras = Collections.singletonList(
            WorkflowBatchResult.OutputExtra.of(output)
        );
        List<WorkflowBatchResult> workflowBatchResults = Collections.singletonList(
            WorkflowBatchResult.of("", "", "", "", outputExtras, "", "", Collections.singletonList("unitId"))
        );
        WorkflowBatchResults batchResults = new WorkflowBatchResults(Paths.get("tmp"), workflowBatchResults);
        handler.addOutputResult(0, batchResults);

        // When
        List<ItemStatus> itemStatuses = plugin.executeList(parameter, handler);

        // Then
        assertThat(itemStatuses).extracting(ItemStatus::getGlobalStatus).containsOnly(UNKNOWN);
    }

    @Test
    @RunWithCustomExecutor
    public void should_return_item_statues_with_KO_when_store_extracted_metadata_fails() throws Exception {
        // Given
        ExtractedMetadataForAu extractedMetadataForAu = new ExtractedMetadataForAu();
        extractedMetadataForAu.put("key", "val");

        OutputPreservation output = new OutputPreservation();
        output.setStatus(PreservationStatus.OK);
        output.setAction(ActionTypePreservation.EXTRACT_AU);
        output.setOutputName("outputName");
        output.setExtractedMetadataAU(extractedMetadataForAu);
        List<WorkflowBatchResult.OutputExtra> outputExtras = Collections.singletonList(
            WorkflowBatchResult.OutputExtra.of(output)
        );
        List<WorkflowBatchResult> workflowBatchResults = Collections.singletonList(
            WorkflowBatchResult.of("", "", "", "", outputExtras, "", "", Collections.singletonList("unitId"))
        );
        WorkflowBatchResults batchResults = new WorkflowBatchResults(Paths.get("tmp"), workflowBatchResults);
        handler.addOutputResult(0, batchResults);

        doThrow(new VitamClientInternalException("yes it fails"))
            .when(batchReportClient)
            .storeExtractedMetadataForAu(anyList());

        // When
        ThrowingCallable shouldThrow = () -> plugin.executeList(parameter, handler);

        // Then
        assertThatThrownBy(shouldThrow).isInstanceOf(ProcessingException.class);
    }

    @Test
    @RunWithCustomExecutor
    public void should_remove_null_values() throws Exception {
        // Given
        ExtractedMetadataForAu extractedMetadataForAu = new ExtractedMetadataForAu();
        extractedMetadataForAu.put("key", Collections.singletonList("value"));
        extractedMetadataForAu.put("yeah", null);

        OutputPreservation output = new OutputPreservation();
        output.setStatus(PreservationStatus.OK);
        output.setAction(ActionTypePreservation.EXTRACT_AU);
        output.setOutputName("outputName");
        output.setExtractedMetadataAU(extractedMetadataForAu);
        List<WorkflowBatchResult.OutputExtra> outputExtras = Collections.singletonList(
            WorkflowBatchResult.OutputExtra.of(output)
        );
        List<WorkflowBatchResult> workflowBatchResults = Collections.singletonList(
            WorkflowBatchResult.of("", "", "", "", outputExtras, "", "", Collections.singletonList("unitId"))
        );
        WorkflowBatchResults batchResults = new WorkflowBatchResults(Paths.get("tmp"), workflowBatchResults);
        handler.addOutputResult(0, batchResults);
        doNothing().when(batchReportClient).storeExtractedMetadataForAu(listArgumentCaptor.capture());

        // When
        List<ItemStatus> itemStatuses = plugin.executeList(parameter, handler);

        // Then
        assertThat(itemStatuses).extracting(ItemStatus::getGlobalStatus).containsOnly(OK);
        assertThat(listArgumentCaptor.getValue())
            .extracting(ExtractedMetadata::getMetadata)
            .containsExactly(Map.of("key", Collections.singletonList("value")));
    }
}
