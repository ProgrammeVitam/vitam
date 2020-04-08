package fr.gouv.vitam.worker.core.plugin.preservation;

import fr.gouv.vitam.batch.report.model.PreservationStatus;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.administration.ActionTypePreservation;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.common.InternalActionKeysRetriever;
import fr.gouv.vitam.worker.core.plugin.preservation.model.ExtractedMetadataForAu;
import fr.gouv.vitam.worker.core.plugin.preservation.model.OutputPreservation;
import fr.gouv.vitam.worker.core.plugin.preservation.model.WorkflowBatchResult;
import fr.gouv.vitam.worker.core.plugin.preservation.model.WorkflowBatchResults;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.common.model.StatusCode.WARNING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

public class PreservationUnitMetadataSecurityChecksTest {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @InjectMocks
    private PreservationUnitMetadataSecurityChecks securityChecksPlugin;

    @Mock
    private InternalActionKeysRetriever internalActionKeysRetriever;

    private HandlerIO handler = new TestHandlerIO();

    @Test
    public void should_disable_lfc_when_not_GENERATE_action() throws Exception {
        // Given
        OutputPreservation output = new OutputPreservation();
        output.setStatus(PreservationStatus.OK);
        output.setAction(ActionTypePreservation.ANALYSE);
        List<WorkflowBatchResult.OutputExtra> outputExtras = Collections.singletonList(WorkflowBatchResult.OutputExtra.of(output));
        WorkflowBatchResult batchResult = WorkflowBatchResult.of("", "", "", "", outputExtras, "", "", Collections.emptyList());
        List<WorkflowBatchResult> workflowBatchResults = Collections.singletonList(batchResult);
        WorkflowBatchResults batchResults = new WorkflowBatchResults(Paths.get("tmp"), workflowBatchResults);
        handler.addOutputResult(0, batchResults);

        // When
        List<ItemStatus> itemStatuses = securityChecksPlugin.executeList(null, handler);

        // Then
        assertThat(itemStatuses).extracting(ItemStatus::isLifecycleEnable).containsOnly(false);
    }

    @Test
    public void should_check_extracted_metadata_and_fill_output_extra() throws Exception {
        // Given
        ExtractedMetadataForAu extractedMetadata = new ExtractedMetadataForAu();
        extractedMetadata.put("empty", Collections.singletonList("YEAH"));

        OutputPreservation output = new OutputPreservation();
        output.setStatus(PreservationStatus.OK);
        output.setAction(ActionTypePreservation.EXTRACT_AU);
        output.setExtractedMetadataAU(extractedMetadata);

        List<WorkflowBatchResult.OutputExtra> outputExtras = Collections.singletonList(WorkflowBatchResult.OutputExtra.of(output));
        WorkflowBatchResult batchResult = WorkflowBatchResult.of("", "", "", "", outputExtras, "", "", Collections.emptyList());
        List<WorkflowBatchResult> workflowBatchResults = Collections.singletonList(batchResult);

        WorkflowBatchResults batchResults = new WorkflowBatchResults(Paths.get("tmp"), workflowBatchResults);
        handler.addOutputResult(0, batchResults);

        given(internalActionKeysRetriever.getInternalKeyFields(any())).willReturn(Collections.emptyList());

        // When
        List<ItemStatus> itemStatuses = securityChecksPlugin.executeList(null, handler);

        // Then
        assertThat(itemStatuses).extracting(ItemStatus::getGlobalStatus).containsOnly(OK);
        assertThat(getWorkflowBatchResults())
            .extracting(w -> w.getOutputExtras().get(0).getExtractedMetadataAU())
            .extracting(Optional::get)
            .containsOnly(extractedMetadata);
    }

    @Test
    public void should_check_extracted_metadata_and_fill_output_extra_KO() throws Exception {
        // Given
        OutputPreservation output = getOutputPreservationExtracted("<!doctype html><html lang=\"en\"><head>  <meta charset=\"utf-8\">  <title>The HTML5 Herald</title>  <meta name=\"description\" content=\"The HTML5 example\">  <meta name=\"author\" content=\"yeah\">  <link rel=\"stylesheet\" href=\"css/styles.css?v=1.0\"></head><body>  <script src=\"js/scripts.js\"></script></body></html>");

        List<WorkflowBatchResult.OutputExtra> outputExtras = Collections.singletonList(WorkflowBatchResult.OutputExtra.of(output));
        WorkflowBatchResult batchResult = WorkflowBatchResult.of("", "", "", "", outputExtras, "", "", Collections.emptyList());
        List<WorkflowBatchResult> workflowBatchResults = Collections.singletonList(batchResult);

        WorkflowBatchResults batchResults = new WorkflowBatchResults(Paths.get("tmp"), workflowBatchResults);
        handler.addOutputResult(0, batchResults);

        // When
        List<ItemStatus> itemStatuses = securityChecksPlugin.executeList(null, handler);

        // Then
        assertThat(itemStatuses).extracting(ItemStatus::getGlobalStatus).containsOnly(KO);
    }

    @Test
    public void should_check_extracted_metadata_and_fill_output_extra_WARNING() throws Exception {
        // Given
        OutputPreservation output = getOutputPreservationExtracted("<!doctype html><html lang=\"en\"><head>  <meta charset=\"utf-8\">  <title>The HTML5 Herald</title>  <meta name=\"description\" content=\"The HTML5 example\">  <meta name=\"author\" content=\"yeah\">  <link rel=\"stylesheet\" href=\"css/styles.css?v=1.0\"></head><body>  <script src=\"js/scripts.js\"></script></body></html>");
        OutputPreservation output2 = getOutputPreservationExtracted("yeah");

        List<WorkflowBatchResult.OutputExtra> outputExtras = Arrays.asList(WorkflowBatchResult.OutputExtra.of(output), WorkflowBatchResult.OutputExtra.of(output2));
        WorkflowBatchResult batchResult = WorkflowBatchResult.of("", "", "", "", outputExtras, "", "", Collections.emptyList());
        List<WorkflowBatchResult> workflowBatchResults = Collections.singletonList(batchResult);

        WorkflowBatchResults batchResults = new WorkflowBatchResults(Paths.get("tmp"), workflowBatchResults);
        handler.addOutputResult(0, batchResults);

        given(internalActionKeysRetriever.getInternalKeyFields(any())).willReturn(Collections.emptyList());

        // When
        List<ItemStatus> itemStatuses = securityChecksPlugin.executeList(null, handler);

        // Then
        assertThat(itemStatuses).extracting(ItemStatus::getGlobalStatus).containsOnly(WARNING);
    }

    @Test
    public void should_check_if_it_contains_internal_key() throws Exception {
        // Given
        OutputPreservation output = getOutputPreservationExtracted("_us");

        List<WorkflowBatchResult.OutputExtra> outputExtras = Collections.singletonList(WorkflowBatchResult.OutputExtra.of(output));
        WorkflowBatchResult batchResult = WorkflowBatchResult.of("", "", "", "", outputExtras, "", "", Collections.emptyList());
        List<WorkflowBatchResult> workflowBatchResults = Collections.singletonList(batchResult);

        WorkflowBatchResults batchResults = new WorkflowBatchResults(Paths.get("tmp"), workflowBatchResults);
        handler.addOutputResult(0, batchResults);

        given(internalActionKeysRetriever.getInternalKeyFields(any())).willReturn(Collections.singletonList("_us"));

        // When
        List<ItemStatus> itemStatuses = securityChecksPlugin.executeList(null, handler);

        // Then
        assertThat(itemStatuses).extracting(ItemStatus::getGlobalStatus).containsOnly(KO);
    }

    private OutputPreservation getOutputPreservationExtracted(String metadata) {
        ExtractedMetadataForAu extractedMetadata2 = new ExtractedMetadataForAu();
        extractedMetadata2.put("empty", Collections.singletonList(metadata));

        OutputPreservation output2 = new OutputPreservation();
        output2.setStatus(PreservationStatus.OK);
        output2.setAction(ActionTypePreservation.EXTRACT_AU);
        output2.setExtractedMetadataAU(extractedMetadata2);
        return output2;
    }

    private List<WorkflowBatchResult> getWorkflowBatchResults() {
        return ((WorkflowBatchResults) handler.getInput(0)).getWorkflowBatchResults();
    }
}