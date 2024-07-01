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

import fr.gouv.vitam.batch.report.model.PreservationStatus;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.administration.ActionTypePreservation;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.plugin.preservation.model.ExtractedMetadataForAu;
import fr.gouv.vitam.worker.core.plugin.preservation.model.OutputPreservation;
import fr.gouv.vitam.worker.core.plugin.preservation.model.WorkflowBatchResult;
import fr.gouv.vitam.worker.core.plugin.preservation.model.WorkflowBatchResult.OutputExtra;
import fr.gouv.vitam.worker.core.plugin.preservation.model.WorkflowBatchResults;
import org.apache.commons.text.StringEscapeUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.common.model.StatusCode.WARNING;
import static fr.gouv.vitam.worker.core.plugin.preservation.PreservationTesseractPlugin.TEXT_CONTENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PreservationTesseractPluginTest {

    private static final String TEXT_37619_LENGTH_FILE = "PreservationTesseractPlugin/text_37619_length.txt";
    private static final String TEXT_352312_LENGTH_FILE = "PreservationTesseractPlugin/text_352312_length.txt";

    private final HandlerIO handler = new TestHandlerIO();

    private PreservationTesseractPlugin preservationTesseractPlugin;

    @Before
    public void setup() {
        preservationTesseractPlugin = new PreservationTesseractPlugin();
    }

    @Test
    public void should_split_text_content_when_length_excede_vitam_text_maximum_length()
        throws ProcessingException, IOException {
        final String tesseractOutput = Files.lines(PropertiesUtils.getResourcePath(TEXT_37619_LENGTH_FILE)).collect(
            Collectors.joining()
        );
        OutputPreservation output = getOutputPreservationExtracted(tesseractOutput);

        List<OutputExtra> outputExtras = Collections.singletonList(
            OutputExtra.withExtractedMetadataForAu(OutputExtra.of(output), output.getExtractedMetadataAU())
        );

        WorkflowBatchResult batchResult = WorkflowBatchResult.of(
            "",
            "",
            "",
            "",
            outputExtras,
            "",
            "",
            Collections.emptyList()
        );
        List<WorkflowBatchResult> workflowBatchResults = Collections.singletonList(batchResult);
        WorkflowBatchResults batchResults = new WorkflowBatchResults(Paths.get("tmp"), workflowBatchResults);
        handler.addOutputResult(0, batchResults);

        List<ItemStatus> itemStatuses = preservationTesseractPlugin.executeList(null, handler);

        // Then
        Optional<ExtractedMetadataForAu> extractedMetadataForAu =
            ((WorkflowBatchResults) handler.getInput(0)).getWorkflowBatchResults()
                .get(0)
                .getOutputExtras()
                .get(0)
                .getExtractedMetadataAU();
        assertThat(itemStatuses).extracting(ItemStatus::getGlobalStatus).contains(OK);
        assertTrue(extractedMetadataForAu.isPresent());
        Object TextContent = extractedMetadataForAu.get().get(TEXT_CONTENT);
        assertTrue(TextContent instanceof List);
        assertEquals(2, ((List) TextContent).size());
        assertThat(StringEscapeUtils.escapeJava((String) ((List) TextContent).get(0)).length()).isLessThan(
            VitamConfiguration.getTextMaxLength()
        );
    }

    @Test
    public void should_split_text_content_when_length_excede_vitam_textContent_maximum_length()
        throws ProcessingException, IOException {
        final String tesseractOutput = Files.lines(PropertiesUtils.getResourcePath(TEXT_352312_LENGTH_FILE)).collect(
            Collectors.joining()
        );
        OutputPreservation output = getOutputPreservationExtracted(tesseractOutput);

        List<OutputExtra> outputExtras = Collections.singletonList(
            OutputExtra.withExtractedMetadataForAu(OutputExtra.of(output), output.getExtractedMetadataAU())
        );

        WorkflowBatchResult batchResult = WorkflowBatchResult.of(
            "",
            "",
            "",
            "",
            outputExtras,
            "",
            "",
            Collections.emptyList()
        );
        List<WorkflowBatchResult> workflowBatchResults = Collections.singletonList(batchResult);
        WorkflowBatchResults batchResults = new WorkflowBatchResults(Paths.get("tmp"), workflowBatchResults);
        handler.addOutputResult(0, batchResults);

        List<ItemStatus> itemStatuses = preservationTesseractPlugin.executeList(null, handler);

        // Then
        Optional<ExtractedMetadataForAu> extractedMetadataForAu =
            ((WorkflowBatchResults) handler.getInput(0)).getWorkflowBatchResults()
                .get(0)
                .getOutputExtras()
                .get(0)
                .getExtractedMetadataAU();
        assertThat(itemStatuses).extracting(ItemStatus::getGlobalStatus).contains(WARNING);
        assertTrue(extractedMetadataForAu.isPresent());
        Object TextContent = extractedMetadataForAu.get().get(TEXT_CONTENT);
        assertTrue(TextContent instanceof List);
        assertThat(((List) TextContent).size()).isEqualTo(10);
        assertThat(StringEscapeUtils.escapeJava((String) ((List) TextContent).get(0)).length()).isLessThan(
            VitamConfiguration.getTextMaxLength()
        );
    }

    private OutputPreservation getOutputPreservationExtracted(String metadata) {
        ExtractedMetadataForAu extractedMetadata = new ExtractedMetadataForAu();
        extractedMetadata.put(TEXT_CONTENT, metadata);

        OutputPreservation output = new OutputPreservation();
        output.setStatus(PreservationStatus.OK);
        output.setAction(ActionTypePreservation.EXTRACT_AU);
        output.setExtractedMetadataAU(extractedMetadata);
        return output;
    }
}
