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

import fr.gouv.vitam.batch.report.model.PreservationStatus;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.administration.ActionTypePreservation;
import fr.gouv.vitam.common.model.preservation.OtherMetadata;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.plugin.preservation.model.ExtractedMetadata;
import fr.gouv.vitam.worker.core.plugin.preservation.model.OutputPreservation;
import fr.gouv.vitam.worker.core.plugin.preservation.model.WorkflowBatchResult;
import fr.gouv.vitam.worker.core.plugin.preservation.model.WorkflowBatchResult.OutputExtra;
import fr.gouv.vitam.worker.core.plugin.preservation.model.WorkflowBatchResults;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
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

public class PreservationMetadataSecurityChecksTest {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();
    @InjectMocks
    private PreservationMetadataSecurityChecks securityChecksPlugin;

    private HandlerIO handler = new TestHandlerIO();

    @Test
    public void should_disable_lfc_when_not_GENERATE_action() throws Exception {
        // Given
        OutputPreservation output = new OutputPreservation();
        output.setStatus(PreservationStatus.OK);
        output.setAction(ActionTypePreservation.ANALYSE);
        List<OutputExtra> outputExtras = Collections.singletonList(OutputExtra.of(output));
        WorkflowBatchResult batchResult = WorkflowBatchResult.of("", "", "", "", outputExtras, "", "");
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
        OtherMetadata otherMetadata = new OtherMetadata();
        otherMetadata.put("empty", Collections.singletonList("YEAH"));

        ExtractedMetadata extractedMetadata = new ExtractedMetadata();
        extractedMetadata.setOtherMetadata(otherMetadata);

        OutputPreservation output = new OutputPreservation();
        output.setStatus(PreservationStatus.OK);
        output.setAction(ActionTypePreservation.EXTRACT);
        output.setExtractedMetadata(extractedMetadata);

        List<OutputExtra> outputExtras = Collections.singletonList(OutputExtra.of(output));
        WorkflowBatchResult batchResult = WorkflowBatchResult.of("", "", "", "", outputExtras, "", "");
        List<WorkflowBatchResult> workflowBatchResults = Collections.singletonList(batchResult);

        WorkflowBatchResults batchResults = new WorkflowBatchResults(Paths.get("tmp"), workflowBatchResults);
        handler.addOutputResult(0, batchResults);

        // When
        List<ItemStatus> itemStatuses = securityChecksPlugin.executeList(null, handler);

        // Then
        assertThat(itemStatuses).extracting(ItemStatus::getGlobalStatus).containsOnly(OK);
        assertThat(getWorkflowBatchResults())
            .extracting(w -> w.getOutputExtras().get(0).getExtractedMetadata())
            .extracting(Optional::get)
            .containsOnly(extractedMetadata);
    }

    @Test
    public void should_check_extracted_metadata_and_fill_output_extra_KO() throws Exception {
        // Given
        OutputPreservation output = getOutputPreservationExtracted("<!doctype html><html lang=\"en\"><head>  <meta charset=\"utf-8\">  <title>The HTML5 Herald</title>  <meta name=\"description\" content=\"The HTML5 example\">  <meta name=\"author\" content=\"yeah\">  <link rel=\"stylesheet\" href=\"css/styles.css?v=1.0\"></head><body>  <script src=\"js/scripts.js\"></script></body></html>");

        List<OutputExtra> outputExtras = Collections.singletonList(OutputExtra.of(output));
        WorkflowBatchResult batchResult = WorkflowBatchResult.of("", "", "", "", outputExtras, "", "");
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

        List<OutputExtra> outputExtras = Arrays.asList(OutputExtra.of(output), OutputExtra.of(output2));
        WorkflowBatchResult batchResult = WorkflowBatchResult.of("", "", "", "", outputExtras, "", "");
        List<WorkflowBatchResult> workflowBatchResults = Collections.singletonList(batchResult);

        WorkflowBatchResults batchResults = new WorkflowBatchResults(Paths.get("tmp"), workflowBatchResults);
        handler.addOutputResult(0, batchResults);

        // When
        List<ItemStatus> itemStatuses = securityChecksPlugin.executeList(null, handler);

        // Then
        assertThat(itemStatuses).extracting(ItemStatus::getGlobalStatus).containsOnly(WARNING);
    }

    private OutputPreservation getOutputPreservationExtracted(String metadata) {
        OtherMetadata otherMetadata2 = new OtherMetadata();
        otherMetadata2.put("empty", Collections.singletonList(metadata));

        ExtractedMetadata extractedMetadata2 = new ExtractedMetadata();
        extractedMetadata2.setOtherMetadata(otherMetadata2);

        OutputPreservation output2 = new OutputPreservation();
        output2.setStatus(PreservationStatus.OK);
        output2.setAction(ActionTypePreservation.EXTRACT);
        output2.setExtractedMetadata(extractedMetadata2);
        return output2;
    }

    private List<WorkflowBatchResult> getWorkflowBatchResults() {
        return ((WorkflowBatchResults) handler.getInput(0)).getWorkflowBatchResults();
    }
}