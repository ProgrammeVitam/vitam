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

package fr.gouv.vitam.worker.core.plugin.reassignment;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Iterators;
import fr.gouv.vitam.batch.report.client.BatchReportClient;
import fr.gouv.vitam.batch.report.model.ReportExportRequest;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.BooleanQuery;
import fr.gouv.vitam.common.database.builder.query.CompareQuery;
import fr.gouv.vitam.common.database.builder.query.InQuery;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.jsonl.JsonLineIterator;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.OriginatingAgencyReassignmentRequest;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import fr.gouv.vitam.worker.core.exception.ProcessingStatusException;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import org.apache.commons.collections4.CollectionUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;

/**
 * OriginatingAgencyReassignmentObjectGroupPreparationPlugin
 */
public class OriginatingAgencyReassignmentObjectGroupPreparationPlugin extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(
        OriginatingAgencyReassignmentObjectGroupPreparationPlugin.class
    );

    private static final String PLUGIN_NAME = "ORIGINATING_AGENCY_REASSIGNMENT_OBJECT_GROUP_PREPARATION";

    static final String GOTS_IDS_TO_UPDATE_JSONL_FILE_NAME = "object_groups_to_update_sp.jsonl";

    private static final int INTERMEDIATE_OG_FILE_OUT_RANK = 0;

    private final OriginatingAgencyReassignmentService originatingAgencyReassignmentService;

    public OriginatingAgencyReassignmentObjectGroupPreparationPlugin() {
        // Default constructor for workflow initialization by Worker
        originatingAgencyReassignmentService = new OriginatingAgencyReassignmentService();
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException {
        try {
            handleObjectGroupReassignmentGenerationDistributions(handler, param);

            return buildItemStatus(PLUGIN_NAME, StatusCode.OK, null);
        } catch (ProcessingStatusException e) {
            LOGGER.error(
                "originating agencies reassignment preparation failed with status [" + e.getMessage() + "]",
                e
            );
            return buildItemStatus(PLUGIN_NAME, e.getStatusCode(), e);
        }
    }

    private void handleObjectGroupReassignmentGenerationDistributions(HandlerIO handler, WorkerParameters param)
        throws ProcessingStatusException {
        File gotIdsToFillInIntermediateFile = (File) handler.getInput(INTERMEDIATE_OG_FILE_OUT_RANK);
        try {
            final OriginatingAgencyReassignmentRequest reassignmentRequest =
                originatingAgencyReassignmentService.loadRequestJsonFromWorkspace(handler);

            if (!reassignmentRequest.isPropagateToObjectGroups()) {
                //create an empty distribution file for GOts update step
                createEmptyDistributionFile(handler);
                return;
            }

            exportIntermediateObjectGroupIdToUpdateOriginatingAgenciesFileToBatchReport(
                handler,
                gotIdsToFillInIntermediateFile,
                reassignmentRequest
            );

            try (BatchReportClient batchReportClient = handler.getBatchReportClient()) {
                batchReportClient.exportObjectGroupsReassignmentToUpdateOriginatingAgency(
                    handler.getContainerName(),
                    new ReportExportRequest(GOTS_IDS_TO_UPDATE_JSONL_FILE_NAME),
                    param.getExecutionContext()
                );
            }
        } catch (IOException | VitamClientInternalException | ProcessingException e) {
            throw new ProcessingStatusException(StatusCode.FATAL, e.getMessage());
        }
    }

    private void createEmptyDistributionFile(HandlerIO handler)
        throws IOException, ProcessingStatusException, ProcessingException {
        File gotToUpdateDistributionFile = handler.getNewLocalFile(
            handler.getWorkFlowExecutionContext(),
            GOTS_IDS_TO_UPDATE_JSONL_FILE_NAME
        );
        boolean newFileCreated = gotToUpdateDistributionFile.createNewFile();

        if (!newFileCreated) {
            throw new ProcessingStatusException(
                StatusCode.FATAL,
                "Could not create an empty file for step distribution"
            );
        }
        handler.transferFileToWorkspace(GOTS_IDS_TO_UPDATE_JSONL_FILE_NAME, gotToUpdateDistributionFile, true, false);
    }

    public void exportIntermediateObjectGroupIdToUpdateOriginatingAgenciesFileToBatchReport(
        HandlerIO handler,
        File gotIdsToFillFile,
        OriginatingAgencyReassignmentRequest reassignmentRequest
    ) throws ProcessingStatusException {
        try (
            BatchReportClient batchReportClient = handler.getBatchReportClient();
            InputStream inputStream = new FileInputStream(gotIdsToFillFile)
        ) {
            Iterator<List<JsonLineModel>> partitions = Iterators.partition(
                new JsonLineIterator<>(inputStream, new TypeReference<>() {}),
                VitamConfiguration.getBatchSize()
            );

            while (partitions.hasNext()) {
                Set<String> objectGroupIds = partitions
                    .next()
                    .stream()
                    .map(JsonLineModel::getId)
                    .collect(Collectors.toSet());

                Set<String> updatableOriginatingAgencies = filterObjectGroupsIdHavingOriginatingAgency(
                    handler,
                    objectGroupIds,
                    reassignmentRequest.getSourceOriginatingAgency()
                );

                originatingAgencyReassignmentService.appendReassignmentObjectGroupIdToUpdateOriginatingAgencyToBatchReport(
                    handler,
                    updatableOriginatingAgencies,
                    batchReportClient
                );
            }
        } catch (
            MetaDataExecutionException
            | InvalidCreateOperationException
            | MetaDataClientServerException
            | InvalidParseOperationException
            | MetaDataDocumentSizeException
            | IOException e
        ) {
            throw new ProcessingStatusException(StatusCode.FATAL, e.getMessage(), e);
        }
    }

    private Set<String> filterObjectGroupsIdHavingOriginatingAgency(
        HandlerIO handlerIO,
        Collection<String> objectGroupsIds,
        String originatingAgency
    )
        throws InvalidParseOperationException, InvalidCreateOperationException, MetaDataExecutionException, MetaDataClientServerException, MetaDataDocumentSizeException {
        if (CollectionUtils.isEmpty(objectGroupsIds)) {
            return new HashSet<>();
        }
        Set<String> filteredObjectGroupsIdsByOriginatingAgency = new HashSet<>();
        try (MetaDataClient metaDataClient = handlerIO.getMetaDataClient()) {
            SelectMultiQuery selectMultiQuery = new SelectMultiQuery();

            InQuery inQuery = QueryHelper.in(VitamFieldsHelper.id(), objectGroupsIds.toArray(new String[0]));

            CompareQuery originatingAgencyQuery = QueryHelper.eq(
                VitamFieldsHelper.originatingAgency(),
                originatingAgency
            );
            BooleanQuery searchQuery = QueryHelper.and().add(inQuery, originatingAgencyQuery);
            selectMultiQuery.setQuery(searchQuery);

            selectMultiQuery.addUsedProjection(VitamFieldsHelper.id(), VitamFieldsHelper.originatingAgency());

            JsonNode results = metaDataClient.selectObjectGroups(selectMultiQuery.getFinalSelect()).get("$results");

            for (JsonNode result : results) {
                filteredObjectGroupsIdsByOriginatingAgency.add(result.get(VitamFieldsHelper.id()).asText());
            }
            return filteredObjectGroupsIdsByOriginatingAgency;
        }
    }

    public static String getId() {
        return PLUGIN_NAME;
    }
}
