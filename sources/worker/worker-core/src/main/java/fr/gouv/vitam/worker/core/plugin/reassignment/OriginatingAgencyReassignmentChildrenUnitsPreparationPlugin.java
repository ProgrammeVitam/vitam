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
import fr.gouv.vitam.batch.report.model.ReportBody;
import fr.gouv.vitam.batch.report.model.ReportExportRequest;
import fr.gouv.vitam.batch.report.model.ReportType;
import fr.gouv.vitam.batch.report.model.entry.OriginatingAgencyReassignmentUpdateReportEntry;
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
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.iterables.SpliteratorIterator;
import fr.gouv.vitam.common.jsonl.JsonLineIterator;
import fr.gouv.vitam.common.jsonl.JsonLineWriter;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.WorkFlowExecutionContext;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import fr.gouv.vitam.worker.core.exception.ProcessingStatusException;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.plugin.ScrollSpliteratorHelper;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;

/**
 * OriginatingAgencyReassignmentChildrenUnitsPreparationPlugin
 */
public class OriginatingAgencyReassignmentChildrenUnitsPreparationPlugin extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(
        OriginatingAgencyReassignmentChildrenUnitsPreparationPlugin.class
    );
    private static final TypeReference<JsonLineModel> TYPE_REFERENCE = new TypeReference<>() {};
    private static final String UNITS_TO_UPDATE_FILE_NAME = "units_to_update.jsonl";
    private static final String INTERMEDIATE_UNITS_IDS_FILE_NAME = "intermediate_units_ids.jsonl";

    private static final String PLUGIN_NAME = "ORIGINATING_AGENCY_REASSIGNMENT_CHILDREN_UNITS_UPDATE_PREPARATION";

    static final String UNITS_CHILDREN_SPS_TO_UPDATE_JSONL_FILE_NAME = "unitsChildrenToUpdateSps.jsonl";

    public OriginatingAgencyReassignmentChildrenUnitsPreparationPlugin() {
        // Default constructor for workflow initialization by Worker
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException {
        LOGGER.info("starting generating originating agencies reassignment children distribution ");
        String processId = handler.getContainerName();

        try {
            exportUnitsChildrenToUpdateOriginatingAgencies(handler, param, processId);

            return buildItemStatus(getId(), StatusCode.OK, null);
        } catch (ProcessingStatusException e) {
            LOGGER.error(
                "Error generating unit children for updating originating agencies " + " [" + e.getStatusCode() + "]",
                e
            );
            return buildItemStatus(getId(), e.getStatusCode(), e.getEventDetails());
        }
    }

    private void exportUnitsChildrenToUpdateOriginatingAgencies(
        HandlerIO handler,
        WorkerParameters param,
        String processId
    ) throws ProcessingStatusException {
        try (BatchReportClient batchReportClient = handler.getBatchReportClient()) {
            checkExistingDistributionFile(handler);

            File unitsJsonlFile = handler.getFileFromWorkspace(
                WorkFlowExecutionContext.VITAM,
                UNITS_TO_UPDATE_FILE_NAME
            );

            File unitIdsToFillInIntermediateFile = handler.getNewLocalFile(
                WorkFlowExecutionContext.VITAM,
                INTERMEDIATE_UNITS_IDS_FILE_NAME
            );

            try (
                InputStream inputStream = new FileInputStream(unitsJsonlFile);
                final OutputStream exportUnitIdsOutputStream = new FileOutputStream(unitIdsToFillInIntermediateFile);
                JsonLineWriter<JsonLineModel> exportUnitFileWriter = new JsonLineWriter<>(exportUnitIdsOutputStream)
            ) {
                JsonLineIterator<JsonLineModel> lines = new JsonLineIterator<>(inputStream, TYPE_REFERENCE);

                Iterator<List<JsonLineModel>> bulkLines = Iterators.partition(lines, VitamConfiguration.getBatchSize());
                while (bulkLines.hasNext()) {
                    List<JsonLineModel> unitsToBatch = bulkLines.next();
                    findAndSaveUnitsChildrenInIntermediateFile(handler, unitsToBatch, processId, exportUnitFileWriter);
                }
                exportUnitIdsOutputStream.flush();
            }

            exportIntermediateUnitIdFileToBatchReport(handler, unitIdsToFillInIntermediateFile, processId);

            batchReportClient.exportUnitsToComputeOriginatingAgencies(
                processId,
                new ReportExportRequest(UNITS_CHILDREN_SPS_TO_UPDATE_JSONL_FILE_NAME),
                param.getExecutionContext()
            );
        } catch (
            InvalidCreateOperationException
            | InvalidParseOperationException
            | ContentAddressableStorageNotFoundException
            | ContentAddressableStorageServerException
            | IOException
            | VitamClientInternalException e
        ) {
            throw new ProcessingStatusException(StatusCode.FATAL, "Error on extraction unit children ", e);
        }
    }

    private void exportIntermediateUnitIdFileToBatchReport(HandlerIO handler, File unitIdsToFillFile, String processId)
        throws IOException {
        try (
            BatchReportClient batchReportClient = handler.getBatchReportClient();
            InputStream inputStream = new FileInputStream(unitIdsToFillFile)
        ) {
            JsonLineIterator<JsonLineModel> lines = new JsonLineIterator<>(inputStream, TYPE_REFERENCE);

            Iterator<List<JsonLineModel>> bulkLines = Iterators.partition(lines, VitamConfiguration.getBatchSize());
            while (bulkLines.hasNext()) {
                List<JsonLineModel> unitsToBatch = bulkLines.next();
                //deduplication before sending to batch report
                //to avoid common children to be updated multiple times
                Set<String> unitIds = unitsToBatch.stream().map(JsonLineModel::getId).collect(Collectors.toSet());
                appendUnitIdsToBatchReport(batchReportClient, processId, unitIds);
            }
        }
    }

    private void checkExistingDistributionFile(HandlerIO handler)
        throws ContentAddressableStorageServerException, ContentAddressableStorageNotFoundException {
        try (WorkspaceClient workspaceClient = handler.getWorkspaceClient(handler.getWorkFlowExecutionContext())) {
            if (
                workspaceClient.isExistingObject(
                    handler.getContainerName(),
                    UNITS_CHILDREN_SPS_TO_UPDATE_JSONL_FILE_NAME
                )
            ) {
                LOGGER.warn(
                    UNITS_CHILDREN_SPS_TO_UPDATE_JSONL_FILE_NAME + " already exists. Will be overridden with fresh data"
                );
                workspaceClient.deleteObject(handler.getContainerName(), UNITS_CHILDREN_SPS_TO_UPDATE_JSONL_FILE_NAME);
            }
        }
    }

    private void findAndSaveUnitsChildrenInIntermediateFile(
        HandlerIO handler,
        List<JsonLineModel> unitsToBatch,
        String operationId,
        JsonLineWriter<JsonLineModel> exportUnitFileWriter
    ) throws InvalidCreateOperationException, InvalidParseOperationException, IOException {
        try (MetaDataClient metaDataClient = handler.getMetaDataClient()) {
            String[] parentIds = unitsToBatch.stream().map(JsonLineModel::getId).toArray(String[]::new);
            InQuery childrenUnitsQuery = QueryHelper.in(VitamFieldsHelper.allunitups(), parentIds);
            //Exclude already updated units
            CompareQuery excludeAlreadyUpdatedUnitsQuery = QueryHelper.ne(VitamFieldsHelper.operations(), operationId);
            BooleanQuery childrenNotHandledUnitsQuery = QueryHelper.and()
                .add(childrenUnitsQuery, excludeAlreadyUpdatedUnitsQuery);

            SelectMultiQuery select = new SelectMultiQuery();
            select.setQuery(childrenNotHandledUnitsQuery);
            select.addUsedProjection(VitamFieldsHelper.id());

            // Query against ES via cursor
            Iterator<JsonNode> unitIterator = new SpliteratorIterator<>(
                ScrollSpliteratorHelper.createUnitScrollSplitIterator(metaDataClient, select)
            );

            Iterator<List<JsonNode>> unitIdIteratorByGroup = Iterators.partition(
                unitIterator,
                VitamConfiguration.getBatchSize()
            );

            // performance issue due to ES timeout bug 15611:
            // solution: write to file before write it to BatchReport,

            while (unitIdIteratorByGroup.hasNext()) {
                List<JsonNode> unitNodes = unitIdIteratorByGroup.next();

                List<JsonLineModel> lines = unitNodes
                    .stream()
                    .map(unitNode -> new JsonLineModel(unitNode.get(VitamFieldsHelper.id()).asText()))
                    .toList();
                exportUnitFileWriter.addEntries(lines);
            }
        }
    }

    private void appendUnitIdsToBatchReport(
        BatchReportClient batchReportClient,
        String operationId,
        Set<String> unitsIds
    ) {
        try {
            List<OriginatingAgencyReassignmentUpdateReportEntry> entries = unitsIds
                .stream()
                .map(OriginatingAgencyReassignmentUpdateReportEntry::new)
                .toList();
            ReportBody<OriginatingAgencyReassignmentUpdateReportEntry> report = new ReportBody<>(
                operationId,
                ReportType.ORIGINATING_AGENCY_REASSIGNMENT_UNIT_AGENCIES_COMPUTING,
                entries
            );
            batchReportClient.appendReportEntries(report);
        } catch (VitamClientInternalException e) {
            throw new VitamRuntimeException(e);
        }
    }

    public static String getId() {
        return PLUGIN_NAME;
    }
}
