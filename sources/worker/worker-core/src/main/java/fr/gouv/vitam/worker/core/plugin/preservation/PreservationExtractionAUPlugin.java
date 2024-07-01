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
import fr.gouv.vitam.batch.report.client.BatchReportClient;
import fr.gouv.vitam.batch.report.client.BatchReportClientFactory;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ExtractedMetadata;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.plugin.preservation.model.WorkflowBatchResult;
import fr.gouv.vitam.worker.core.plugin.preservation.model.WorkflowBatchResult.OutputExtra;
import fr.gouv.vitam.worker.core.plugin.preservation.model.WorkflowBatchResults;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatusWithMessage;

public class PreservationExtractionAUPlugin extends ActionHandler {

    public static final String ITEM_ID = "PRESERVATION_EXTRACTION_AU";
    private final VitamLogger logger = VitamLoggerFactory.getInstance(PreservationExtractionAUPlugin.class);
    private final BatchReportClientFactory batchReportClientFactory;

    public PreservationExtractionAUPlugin() {
        this(BatchReportClientFactory.getInstance());
    }

    @VisibleForTesting
    public PreservationExtractionAUPlugin(BatchReportClientFactory batchReportClientFactory) {
        this.batchReportClientFactory = batchReportClientFactory;
    }

    @Override
    public List<ItemStatus> executeList(WorkerParameters workerParameters, HandlerIO handler)
        throws ProcessingException {
        try (BatchReportClient batchReportClient = batchReportClientFactory.getClient()) {
            logger.debug("Starting {}.", ITEM_ID);

            handler.setCurrentObjectId(WorkflowBatchResults.NAME);
            WorkflowBatchResults results = (WorkflowBatchResults) handler.getInput(0);

            List<ExtractedMetadata> extractedMetadataList = new ArrayList<>();

            List<ItemStatus> itemStatuses = new ArrayList<>();
            for (WorkflowBatchResult workflowBatchResult : results.getWorkflowBatchResults()) {
                List<OutputExtra> outputExtras = workflowBatchResult
                    .getOutputExtras()
                    .stream()
                    .filter(o -> !o.isInError() && o.isOkAndExtractedAu())
                    .collect(Collectors.toList());

                if (outputExtras.isEmpty()) {
                    ItemStatus itemStatus = new ItemStatus(ITEM_ID);
                    itemStatus.disableLfc();
                    itemStatuses.add(itemStatus);
                    continue;
                }
                ExtractedMetadata extractedMetadata = mergeExtractedMetadata(
                    workflowBatchResult.getGotId(),
                    handler.getContainerName(),
                    outputExtras,
                    workflowBatchResult.getUnitsForExtractionAU()
                );
                extractedMetadataList.add(extractedMetadata);
                itemStatuses.add(buildItemStatusWithMessage(ITEM_ID, OK, "Insert in batch Report OK."));
            }

            if (!extractedMetadataList.isEmpty()) {
                batchReportClient.storeExtractedMetadataForAu(extractedMetadataList);
            }

            handler.addOutputResult(0, results);
            return itemStatuses;
        } catch (VitamClientInternalException e) {
            throw new ProcessingException(e);
        } finally {
            logger.debug("End of {}.", ITEM_ID);
        }
    }

    private ExtractedMetadata mergeExtractedMetadata(
        String ogId,
        String processId,
        List<OutputExtra> outputExtras,
        List<String> unitsForExtractedAU
    ) {
        // We sadly choose to merge value when there are several extraction for units, we select the last one.
        var extractedMetadata = outputExtras
            .stream()
            .flatMap(output -> output.getOutput().getExtractedMetadataAU().entrySet().stream())
            .filter(e -> Objects.nonNull(e.getValue()))
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue, (v1, v2) -> v2));

        return new ExtractedMetadata(
            ogId,
            processId,
            VitamThreadUtils.getVitamSession().getTenantId(),
            unitsForExtractedAU,
            extractedMetadata
        );
    }
}
