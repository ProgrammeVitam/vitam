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
import fr.gouv.vitam.batch.report.model.entry.OriginatingAgencyReassignmentUnitUpdateReportEntry;
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
import fr.gouv.vitam.worker.core.plugin.ScrollSpliteratorHelper;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;

/**
 * OriginatingAgencyReassignmentUnitsChildrenPreparationPlugin
 */
public class OriginatingAgencyReassignmentUnitsChildrenPreparationPlugin extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(
        OriginatingAgencyReassignmentUnitsChildrenPreparationPlugin.class
    );
    private static final TypeReference<JsonLineModel> TYPE_REFERENCE = new TypeReference<>() {};
    private static final String UNITS_TO_UPDATE_FILE_NAME = "units_to_update.jsonl";

    private static final String INTERMEDIATE_GOTS_CHILDREN_IDS_FILE_NAME = "intermediate_gots_children_ids.jsonl";

    private static final String INTERMEDIATE_UNITS_IDS_FILE_NAME = "intermediate_units_ids.jsonl";

    private static final String PLUGIN_NAME = "ORIGINATING_AGENCY_REASSIGNMENT_UNITS_CHILDREN_PREPARATION";

    private static final String UNITS_CHILDREN_SPS_TO_UPDATE_JSONL_FILE_NAME = "unitsChildrenToUpdateSps.jsonl";

    private static final String GOTS_IDS_TO_UPDATE_JSONL_FILE_NAME = "object_groups_to_update_sps.jsonl";

    private final OriginatingAgencyReassignmentService originatingAgencyReassignmentService;

    public OriginatingAgencyReassignmentUnitsChildrenPreparationPlugin() {
        // Default constructor for workflow initialization by Worker
        originatingAgencyReassignmentService = new OriginatingAgencyReassignmentService();
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException {
        LOGGER.info("starting generating originating agencies reassignment children distribution ");
        String processId = handler.getContainerName();
        try {
            return exportUnitsChildrenToUpdateOriginatingAgencies(handler, param, processId);
        } catch (ProcessingStatusException e) {
            LOGGER.error(
                "Error generating reassignment distribution unit children " + " [" + e.getStatusCode() + "]",
                e
            );
            return buildItemStatus(getId(), e.getStatusCode(), e.getEventDetails());
        }
    }

    private ItemStatus exportUnitsChildrenToUpdateOriginatingAgencies(
        HandlerIO handler,
        WorkerParameters params,
        String processId
    ) throws ProcessingStatusException {
        File unitIdsToFillInIntermediateFile = handler.getNewLocalFile(
            WorkFlowExecutionContext.VITAM,
            INTERMEDIATE_UNITS_IDS_FILE_NAME
        );
        File gotIdsToFillInIntermediateFile = handler.getNewLocalFile(
            WorkFlowExecutionContext.VITAM,
            INTERMEDIATE_GOTS_CHILDREN_IDS_FILE_NAME
        );
        try (BatchReportClient batchReportClient = handler.getBatchReportClient()) {
            deleteDistributionFileIfExists(handler, UNITS_CHILDREN_SPS_TO_UPDATE_JSONL_FILE_NAME);
            deleteDistributionFileIfExists(handler, GOTS_IDS_TO_UPDATE_JSONL_FILE_NAME);

            generateMainUnitChildrenIdsInIntermediateFile(
                handler,
                processId,
                unitIdsToFillInIntermediateFile,
                gotIdsToFillInIntermediateFile
            );

            //read from intermediate file :
            // Generate children units  distribution using batch report
            // generate object group ids distribution using batch report

            generateChildrenUnitsAndTheirObjectGroupsDistributions(
                handler,
                unitIdsToFillInIntermediateFile,
                gotIdsToFillInIntermediateFile,
                processId
            );

            batchReportClient.exportUnitsToComputeOriginatingAgencies(
                processId,
                new ReportExportRequest(UNITS_CHILDREN_SPS_TO_UPDATE_JSONL_FILE_NAME),
                params.getExecutionContext()
            );

            batchReportClient.exportObjectGroupsReassignmentToComputeOriginatingAgencies(
                handler.getContainerName(),
                new ReportExportRequest(GOTS_IDS_TO_UPDATE_JSONL_FILE_NAME),
                params.getExecutionContext()
            );

            return buildItemStatus(getId(), StatusCode.OK, null);
        } catch (VitamClientInternalException e) {
            throw new ProcessingStatusException(StatusCode.FATAL, e.getMessage());
        } finally {
            FileUtils.deleteQuietly(unitIdsToFillInIntermediateFile);
            FileUtils.deleteQuietly(gotIdsToFillInIntermediateFile);
        }
    }

    private void generateMainUnitChildrenIdsInIntermediateFile(
        HandlerIO handler,
        String processId,
        File unitIdsToFillInIntermediateFile,
        File gotIdsToFillInIntermediateFile
    ) throws ProcessingStatusException {
        try {
            File unitsJsonlFile = handler.getFileFromWorkspace(
                WorkFlowExecutionContext.VITAM,
                UNITS_TO_UPDATE_FILE_NAME
            );

            try (
                InputStream inputStream = new FileInputStream(unitsJsonlFile);
                final OutputStream exportUnitIdsOutputStream = new FileOutputStream(unitIdsToFillInIntermediateFile);
                final OutputStream exportObjectGroupIdsOutputStream = new FileOutputStream(
                    gotIdsToFillInIntermediateFile,
                    true
                );
                JsonLineWriter<JsonLineModel> exportUnitFileWriter = new JsonLineWriter<>(exportUnitIdsOutputStream);
                JsonLineWriter<JsonLineModel> exportObjectGroupFileWriter = new JsonLineWriter<>(
                    exportObjectGroupIdsOutputStream
                )
            ) {
                Iterator<List<JsonLineModel>> bulkLines = Iterators.partition(
                    new JsonLineIterator<>(inputStream, TYPE_REFERENCE),
                    VitamConfiguration.getBatchSize()
                );
                while (bulkLines.hasNext()) {
                    List<JsonLineModel> unitsToBatch = bulkLines.next();

                    Set<String> unitsObjectGroupsIds = extractObjectGroupIdsFromMainUnits(unitsToBatch);

                    // Write main units objectGroups Ids in intermediate file
                    addIdsToIntermediateFile(exportObjectGroupFileWriter, unitsObjectGroupsIds);

                    //extract and write children units Ids in intermediate file
                    extractAndWriteUnitChildrenInIntermediateFile(
                        handler,
                        processId,
                        unitsToBatch,
                        exportUnitFileWriter,
                        exportObjectGroupFileWriter
                    );
                }
            }
        } catch (
            IOException
            | ContentAddressableStorageNotFoundException
            | ContentAddressableStorageServerException
            | InvalidCreateOperationException
            | InvalidParseOperationException e
        ) {
            throw new ProcessingStatusException(StatusCode.FATAL, e.getMessage());
        }
    }

    private Set<String> extractObjectGroupIdsFromMainUnits(List<JsonLineModel> mainUnitsLines) {
        return mainUnitsLines
            .stream()
            .filter(unitData -> unitData.getParams() != null)
            .filter(unitData -> unitData.getParams().has(VitamFieldsHelper.object()))
            .map(unitData -> {
                return unitData.getParams().get(VitamFieldsHelper.object()).asText();
            })
            .collect(Collectors.toSet());
    }

    private void generateChildrenUnitsAndTheirObjectGroupsDistributions(
        HandlerIO handler,
        File unitIdsToFillFile,
        File gotIdsToFillInIntermediateFile,
        String processId
    ) throws ProcessingStatusException {
        try {
            //add children units ids to  batch report and
            appendChildUnitsPartition(handler, processId, unitIdsToFillFile);

            exportIntermediateObjectGroupIdToComputeOriginatingAgenciesFileToBatchReport(
                processId,
                handler,
                gotIdsToFillInIntermediateFile
            );
        } catch (IOException e) {
            throw new ProcessingStatusException(StatusCode.FATAL, e.getMessage());
        }
    }

    private void appendChildUnitsPartition(HandlerIO handler, String processId, File unitIdsToFillFile)
        throws IOException {
        try (InputStream inputStream = new FileInputStream(unitIdsToFillFile);) {
            Iterator<List<JsonLineModel>> bulkLines = Iterators.partition(
                new JsonLineIterator<>(inputStream, TYPE_REFERENCE),
                VitamConfiguration.getBatchSize()
            );

            while (bulkLines.hasNext()) {
                List<JsonLineModel> childrenUnitsToBatch = bulkLines.next();
                //deduplication before sending to batch report
                //to avoid common children to be updated multiple times

                //add extracted children unit Ids to batchReport
                appendChildrenUnitIdsToBatchReport(handler, processId, childrenUnitsToBatch);
            }
        }
    }

    private void addIdsToIntermediateFile(JsonLineWriter<JsonLineModel> exportFileWriter, Collection<String> ids)
        throws IOException {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }

        List<JsonLineModel> entries = ids.stream().map(JsonLineModel::new).toList();
        exportFileWriter.addEntries(entries);
    }

    public void exportIntermediateObjectGroupIdToComputeOriginatingAgenciesFileToBatchReport(
        String processId,
        HandlerIO handler,
        File gotIdsToFillFile
    ) throws ProcessingStatusException {
        try (
            BatchReportClient batchReportClient = handler.getBatchReportClient();
            InputStream inputStream = new FileInputStream(gotIdsToFillFile)
        ) {
            JsonLineIterator<JsonLineModel> lines = new JsonLineIterator<>(inputStream, new TypeReference<>() {});

            Iterator<List<JsonLineModel>> bulkLines = Iterators.partition(lines, VitamConfiguration.getBatchSize());
            while (bulkLines.hasNext()) {
                List<JsonLineModel> objectGroupLines = bulkLines.next();

                Set<String> objectGroupIds = objectGroupLines
                    .stream()
                    .map(JsonLineModel::getId)
                    .collect(Collectors.toSet());
                List<String> notHandledObjectGroupIds =
                    originatingAgencyReassignmentService.filterObjectGroupIdsByOperation(
                        handler,
                        processId,
                        objectGroupIds
                    );
                if (CollectionUtils.isEmpty(notHandledObjectGroupIds)) {
                    continue;
                }

                originatingAgencyReassignmentService.appendReassignmentObjectGroupIdsToComputeOriginatingAgenciesToBatchReport(
                    handler,
                    objectGroupIds,
                    batchReportClient
                );
            }
        } catch (
            IOException
            | InvalidParseOperationException
            | InvalidCreateOperationException
            | MetaDataExecutionException
            | MetaDataClientServerException
            | MetaDataDocumentSizeException e
        ) {
            LOGGER.error("originating agency reassignment failed on computing gots [" + StatusCode.FATAL + "]", e);
            throw new ProcessingStatusException(StatusCode.FATAL, e.getMessage(), e);
        }
    }

    private void deleteDistributionFileIfExists(HandlerIO handler, String distributionFileName)
        throws ProcessingStatusException {
        try (WorkspaceClient workspaceClient = handler.getWorkspaceClient(handler.getWorkFlowExecutionContext())) {
            if (workspaceClient.isExistingObject(handler.getContainerName(), distributionFileName)) {
                LOGGER.warn(distributionFileName + " already exists. Will be overridden with fresh data");
                workspaceClient.deleteObject(handler.getContainerName(), distributionFileName);
            }
        } catch (ContentAddressableStorageServerException | ContentAddressableStorageNotFoundException e) {
            throw new ProcessingStatusException(StatusCode.FATAL, e.getMessage());
        }
    }

    private SelectMultiQuery getUnitChildrenWithObjectsQuery(String operationId, List<JsonLineModel> unitsToBatch)
        throws InvalidCreateOperationException, InvalidParseOperationException {
        String[] unitParentIds = unitsToBatch.stream().map(JsonLineModel::getId).toArray(String[]::new);
        InQuery childrenUnitsQuery = QueryHelper.in(VitamFieldsHelper.allunitups(), unitParentIds);
        //Exclude already updated units
        CompareQuery excludeAlreadyUpdatedUnitsQuery = QueryHelper.ne(VitamFieldsHelper.operations(), operationId);
        BooleanQuery childrenNotHandledUnitsQuery = QueryHelper.and()
            .add(childrenUnitsQuery, excludeAlreadyUpdatedUnitsQuery);

        SelectMultiQuery select = new SelectMultiQuery();
        select.setQuery(childrenNotHandledUnitsQuery);
        select.addUsedProjection(VitamFieldsHelper.id(), VitamFieldsHelper.object());
        return select;
    }

    private void extractAndWriteUnitChildrenInIntermediateFile(
        HandlerIO handler,
        String operationId,
        List<JsonLineModel> unitsToBatch,
        JsonLineWriter<JsonLineModel> exportUnitFileWriter,
        JsonLineWriter<JsonLineModel> exportObjectGroupFileWriter
    ) throws IOException, InvalidCreateOperationException, InvalidParseOperationException {
        try (MetaDataClient metaDataClient = handler.getMetaDataClient()) {
            // Query against ES via cursor
            Iterator<JsonNode> unitChildrenIterator = new SpliteratorIterator<>(
                ScrollSpliteratorHelper.createUnitScrollSplitIterator(
                    metaDataClient,
                    getUnitChildrenWithObjectsQuery(operationId, unitsToBatch)
                )
            );

            Iterator<List<JsonNode>> unitChildrenIdsIteratorByGroup = Iterators.partition(
                unitChildrenIterator,
                VitamConfiguration.getBatchSize()
            );

            // performance issue due to ES timeout bug 15611:

            // solution: write unit children ids and theirs object to file before write it to BatchReport,
            while (unitChildrenIdsIteratorByGroup.hasNext()) {
                List<JsonNode> unitNodes = unitChildrenIdsIteratorByGroup.next();
                //Export child units
                List<JsonLineModel> unitChildrenLines = unitNodes
                    .stream()
                    .map(unitNode -> new JsonLineModel(unitNode.get(VitamFieldsHelper.id()).asText(), null, null))
                    .toList();

                if (CollectionUtils.isNotEmpty(unitChildrenLines)) {
                    exportUnitFileWriter.addEntries(unitChildrenLines);
                }

                //Export child units object group ids
                List<JsonLineModel> objectGroupIdsLines = unitNodes
                    .stream()
                    .filter(unitNode -> unitNode.has(VitamFieldsHelper.object()))
                    .map(unitNode -> unitNode.get(VitamFieldsHelper.object()).asText())
                    .distinct()
                    .map(objectGroupId -> new JsonLineModel(objectGroupId, null, null))
                    .toList();
                if (CollectionUtils.isNotEmpty(objectGroupIdsLines)) {
                    exportObjectGroupFileWriter.addEntries(objectGroupIdsLines);
                }
            }
        }
    }

    private void appendChildrenUnitIdsToBatchReport(
        HandlerIO handlerIO,
        String operationId,
        List<JsonLineModel> unitsToBatch
    ) {
        try (BatchReportClient batchReportClient = handlerIO.getBatchReportClient()) {
            if (CollectionUtils.isEmpty(unitsToBatch)) {
                return;
            }
            Set<String> unitIds = unitsToBatch.stream().map(JsonLineModel::getId).collect(Collectors.toSet());

            if (CollectionUtils.isEmpty(unitIds)) {
                return;
            }
            List<OriginatingAgencyReassignmentUnitUpdateReportEntry> entries = unitIds
                .stream()
                .map(OriginatingAgencyReassignmentUnitUpdateReportEntry::new)
                .toList();
            ReportBody<OriginatingAgencyReassignmentUnitUpdateReportEntry> report = new ReportBody<>(
                operationId,
                ReportType.REASSIGNMENT_UNITS_ORIGINATING_AGENCIES_COMPUTE,
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
