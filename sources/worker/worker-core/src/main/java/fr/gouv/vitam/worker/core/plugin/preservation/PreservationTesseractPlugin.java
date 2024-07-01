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

import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.metadata.core.validation.OntologyValidator;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.plugin.preservation.model.ExtractedMetadataForAu;
import fr.gouv.vitam.worker.core.plugin.preservation.model.WorkflowBatchResult;
import fr.gouv.vitam.worker.core.plugin.preservation.model.WorkflowBatchResults;
import fr.gouv.vitam.worker.core.utils.PluginHelper;
import org.apache.commons.text.StringEscapeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.common.model.StatusCode.WARNING;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatusSubItems;

public class PreservationTesseractPlugin extends ActionHandler {

    public static final String TEXT_CONTENT = "TextContent";

    private static final int WORKFLOWBATCHRESULTS_IN_MEMORY = 0;
    private static final String ITEM_ID = "TESSERACT_SPLIT_TEXT_CONTENT";

    public PreservationTesseractPlugin() {}

    @Override
    public List<ItemStatus> executeList(WorkerParameters workerParameters, HandlerIO handler)
        throws ProcessingException {
        WorkflowBatchResults results = (WorkflowBatchResults) handler.getInput(WORKFLOWBATCHRESULTS_IN_MEMORY);

        List<ItemStatus> itemStatuses = new ArrayList<>();
        List<WorkflowBatchResult> workflowBatchResults = new ArrayList<>();

        for (WorkflowBatchResult workflowBatchResult : results.getWorkflowBatchResults()) {
            List<WorkflowBatchResult.OutputExtra> outputExtras = workflowBatchResult
                .getOutputExtras()
                .stream()
                .filter(WorkflowBatchResult.OutputExtra::isOkAndExtractedAu)
                .collect(Collectors.toList());

            if (outputExtras.isEmpty()) {
                workflowBatchResults.add(workflowBatchResult);
                ItemStatus itemStatus = new ItemStatus(ITEM_ID);
                itemStatuses.add(itemStatus);
                continue;
            }

            outputExtras.forEach(
                o ->
                    o
                        .getExtractedMetadataAU()
                        .ifPresent(metadata -> computeTextContent(metadata, itemStatuses, o.getBinaryGUID()))
            );

            workflowBatchResults.add(WorkflowBatchResult.of(workflowBatchResult, outputExtras));
        }

        handler.addOutputResult(0, new WorkflowBatchResults(results.getBatchDirectory(), workflowBatchResults));
        return itemStatuses;
    }

    private void computeTextContent(
        ExtractedMetadataForAu extractedMetadataForAu,
        List<ItemStatus> itemStatuses,
        String subBinaryItemIds
    ) {
        extractedMetadataForAu.computeIfPresent(TEXT_CONTENT, (key, value) -> {
            String textValue = String.valueOf(value);
            final int maxUtf8TextContentLength = VitamConfiguration.getTextContentMaxLength();
            final int maxUtf8Length = VitamConfiguration.getTextMaxLength();
            String encodedString = StringEscapeUtils.escapeJava(textValue);
            if (OntologyValidator.stringExceedsMaxLuceneUtf8StorageSize(textValue, maxUtf8TextContentLength)) {
                encodedString = encodedString.substring(0, maxUtf8TextContentLength);
                itemStatuses.add(
                    buildItemStatusSubItems(
                        ITEM_ID,
                        Stream.of(subBinaryItemIds),
                        WARNING,
                        PluginHelper.EventDetails.of("TextContent metadata exceeds the limit")
                    )
                );
            } else {
                itemStatuses.add(
                    buildItemStatusSubItems(
                        ITEM_ID,
                        Stream.of(subBinaryItemIds),
                        OK,
                        PluginHelper.EventDetails.of("All metadata are OK.")
                    )
                );
            }
            // split text content if it exceeds the lucene limit
            return splitText(encodedString, maxUtf8Length - 1);
        });
    }

    private List<String> splitText(String text, int textLength) {
        List<String> result = new ArrayList<>();

        Pattern pattern = Pattern.compile("\\b.{1," + (textLength - 1) + "}\\b\\W?");
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            result.add(StringEscapeUtils.unescapeJava(matcher.group()));
        }
        return result;
    }
}
