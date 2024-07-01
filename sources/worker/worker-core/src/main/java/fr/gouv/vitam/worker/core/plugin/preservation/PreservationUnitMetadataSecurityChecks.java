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

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.InternalActionKeysRetriever;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.plugin.preservation.model.ExtractedMetadataForAu;
import fr.gouv.vitam.worker.core.plugin.preservation.model.WorkflowBatchResult;
import fr.gouv.vitam.worker.core.plugin.preservation.model.WorkflowBatchResult.OutputExtra;
import fr.gouv.vitam.worker.core.plugin.preservation.model.WorkflowBatchResults;
import fr.gouv.vitam.worker.core.utils.PluginHelper.EventDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.common.model.StatusCode.WARNING;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatusSubItems;
import static java.util.function.Predicate.not;

public class PreservationUnitMetadataSecurityChecks extends ActionHandler {

    public static final String ITEM_ID = "PRESERVATION_UNIT_METADATA_SECURITY_CHECKS";
    private final VitamLogger logger = VitamLoggerFactory.getInstance(PreservationUnitMetadataSecurityChecks.class);

    private final InternalActionKeysRetriever internalActionKeysRetriever;

    public PreservationUnitMetadataSecurityChecks() {
        this(new InternalActionKeysRetriever());
    }

    @VisibleForTesting
    public PreservationUnitMetadataSecurityChecks(InternalActionKeysRetriever internalActionKeysRetriever) {
        this.internalActionKeysRetriever = internalActionKeysRetriever;
    }

    @Override
    public List<ItemStatus> executeList(WorkerParameters workerParameters, HandlerIO handler)
        throws ProcessingException {
        logger.debug("Starting {}.", ITEM_ID);

        handler.setCurrentObjectId(WorkflowBatchResults.NAME);
        WorkflowBatchResults results = (WorkflowBatchResults) handler.getInput(0);

        List<ItemStatus> itemStatuses = new ArrayList<>();
        List<WorkflowBatchResult> workflowBatchResults = new ArrayList<>();

        for (WorkflowBatchResult workflowBatchResult : results.getWorkflowBatchResults()) {
            List<OutputExtra> outputExtras = workflowBatchResult
                .getOutputExtras()
                .stream()
                .filter(OutputExtra::isOkAndExtractedAu)
                .map(this::checkMetadataAndAddExtractedMetadata)
                .collect(Collectors.toList());

            if (outputExtras.isEmpty()) {
                workflowBatchResults.add(workflowBatchResult);
                ItemStatus itemStatus = new ItemStatus(ITEM_ID);
                itemStatuses.add(itemStatus);
                continue;
            }

            itemStatuses.add(getItemStatus(outputExtras));

            workflowBatchResults.add(
                WorkflowBatchResult.of(
                    workflowBatchResult,
                    getOutputExtrasWithoutErrors(workflowBatchResult, outputExtras)
                )
            );
        }

        handler.addOutputResult(0, new WorkflowBatchResults(results.getBatchDirectory(), workflowBatchResults));

        return itemStatuses;
    }

    private List<OutputExtra> getOutputExtrasWithoutErrors(
        WorkflowBatchResult workflowBatchResult,
        List<OutputExtra> outputExtras
    ) {
        Stream<OutputExtra> nonExtractedOrOkOutputExtra = workflowBatchResult
            .getOutputExtras()
            .stream()
            .filter(not(OutputExtra::isOkAndExtractedAu));

        Stream<OutputExtra> extractedOutputExtraSanitize = outputExtras.stream().filter(not(OutputExtra::isInError));

        return Stream.concat(nonExtractedOrOkOutputExtra, extractedOutputExtraSanitize).collect(Collectors.toList());
    }

    private ItemStatus getItemStatus(List<OutputExtra> outputExtras) {
        Stream<String> subBinaryItemIds = outputExtras.stream().map(OutputExtra::getBinaryGUID);
        String error = outputExtras
            .stream()
            .filter(o -> o.getError().isPresent())
            .map(o -> o.getError().get())
            .collect(Collectors.joining(","));
        if (outputExtras.stream().allMatch(OutputExtra::isInError)) {
            return buildItemStatusSubItems(ITEM_ID, subBinaryItemIds, KO, EventDetails.of(error));
        }
        if (outputExtras.stream().noneMatch(OutputExtra::isInError)) {
            return buildItemStatusSubItems(ITEM_ID, subBinaryItemIds, OK, EventDetails.of("All metadata are OK."));
        }
        return buildItemStatusSubItems(ITEM_ID, subBinaryItemIds, WARNING, EventDetails.of(error));
    }

    private OutputExtra checkMetadataAndAddExtractedMetadata(OutputExtra output) {
        try {
            ExtractedMetadataForAu extractedMetadataAU = output.getOutput().getExtractedMetadataAU();
            SanityChecker.checkJsonAll(JsonHandler.unprettyPrint(extractedMetadataAU));
            List<String> internalKeyFields = internalActionKeysRetriever.getInternalKeyFields(
                JsonHandler.toJsonNode(extractedMetadataAU)
            );
            if (!internalKeyFields.isEmpty()) {
                String message = String.format(
                    "Extracted metadata contains these forbidden internal keys: '%s'.",
                    internalKeyFields
                );
                logger.error(message);
                return OutputExtra.inError(message);
            }
            return OutputExtra.withExtractedMetadataForAu(output, output.getOutput().getExtractedMetadataAU());
        } catch (InvalidParseOperationException e) {
            logger.error(e);
            return OutputExtra.inError(e.getMessage());
        }
    }
}
