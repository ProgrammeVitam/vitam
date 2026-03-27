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
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.database.utils.ScrollSpliterator;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.iterables.SpliteratorIterator;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.jsonl.JsonLineIterator;
import fr.gouv.vitam.common.jsonl.JsonLineWriter;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.OriginatingAgencyReassignmentRequest;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AgenciesModel;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialNotFoundException;
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
import fr.gouv.vitam.worker.core.utils.PluginHelper;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;

/**
 * OriginatingAgencyReassignmentPrepareUnitsPlugin
 */
public class OriginatingAgencyReassignmentPreparationPlugin extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(
        OriginatingAgencyReassignmentPreparationPlugin.class
    );

    private static final String PLUGIN_NAME = "ORIGINATING_AGENCY_REASSIGNMENT_PREPARATION";

    public static final String UNITS_TO_UPDATE_FILE = "units_to_update.jsonl";

    private static final String INTERMEDIATE_OG_IDS_FILE_NAME = "intermediate_og_ids.jsonl";

    public static final String OBJECT_GROUPS_TO_UPDATE_JSONL_FILE = "object_groups_to_update_sp.jsonl";

    public static final String REASSIGNMENT_STATISTICS_JSON_FILE = "reassignment_statistics.json";

    /**
     * FIXME: to be removed once Vitam V9.1 reassignment workflow restrictions removed
     */
    @Deprecated
    public static boolean _____Enable_Temporary_V91_Restrictions_____ = true;

    private static final String VERSIONS = "versions";
    private static final String OBJECT_OPI = "#qualifiers.versions.#opi";

    private final OriginatingAgencyReassignmentService originatingAgencyReassignmentService;

    public OriginatingAgencyReassignmentPreparationPlugin() {
        // Default constructor for workflow initialization by Worker
        originatingAgencyReassignmentService = new OriginatingAgencyReassignmentService();
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException {
        try {
            final OriginatingAgencyReassignmentRequest reassignmentRequest =
                originatingAgencyReassignmentService.loadRequestJsonFromWorkspace(handler);

            validationRequest(handler, reassignmentRequest);

            return processRequest(handler, param, reassignmentRequest);
        } catch (ProcessingStatusException e) {
            LOGGER.error(
                "originating agencies reassignment preparation failed with status [" + e.getMessage() + "]",
                e
            );
            return buildItemStatus(PLUGIN_NAME, e.getStatusCode(), e.getEventDetails());
        }
    }

    private void validationRequest(HandlerIO handler, OriginatingAgencyReassignmentRequest reassignmentRequest)
        throws ProcessingStatusException {
        checkDifferentSourceAndDestinationAgencies(reassignmentRequest);

        checkTargetOriginatingAgency(handler, reassignmentRequest.getTargetOriginatingAgency());
    }

    private void checkDifferentSourceAndDestinationAgencies(OriginatingAgencyReassignmentRequest reassignmentRequest)
        throws ProcessingStatusException {
        if (
            Objects.equals(
                reassignmentRequest.getSourceOriginatingAgency(),
                reassignmentRequest.getTargetOriginatingAgency()
            )
        ) {
            throw new ProcessingStatusException(
                StatusCode.KO,
                "sourceOriginatingAgency should be different to targetOriginatingAgency"
            );
        }
    }

    private void checkTargetOriginatingAgency(HandlerIO handler, String targetOriginatingAgency)
        throws ProcessingStatusException {
        LOGGER.debug("Check originating agency check agency with id {}", targetOriginatingAgency);
        try (AdminManagementClient adminManagementClient = handler.getAdminManagementClient()) {
            final RequestResponse<AgenciesModel> agencyByIdResponse = adminManagementClient.getAgencyById(
                targetOriginatingAgency
            );

            List<AgenciesModel> agenciesModels = ((RequestResponseOK<AgenciesModel>) agencyByIdResponse).getResults();
            if (agenciesModels.isEmpty()) {
                throw new ReferentialNotFoundException("originating agency check failed, not found");
            }
        } catch (AdminManagementClientServerException | InvalidParseOperationException e) {
            throw new ProcessingStatusException(StatusCode.FATAL, "Originating agency check failed failed", e);
        } catch (ReferentialNotFoundException e) {
            ObjectNode evDetData = JsonHandler.createObjectNode();
            String message = "originating agency check failed, not found";
            evDetData.put("error", message);
            throw new ProcessingStatusException(
                StatusCode.KO,
                evDetData,
                "Originating agency check failed, not found",
                e
            );
        }
    }

    private ItemStatus processRequest(
        HandlerIO handler,
        WorkerParameters param,
        OriginatingAgencyReassignmentRequest reassignmentRequest
    ) throws ProcessingStatusException {
        File unitToUpdateDistributionFile = handler.getNewLocalFile(
            handler.getWorkFlowExecutionContext(),
            UNITS_TO_UPDATE_FILE
        );

        File ogIdsIntermediateFile = handler.getNewLocalFile(
            handler.getWorkFlowExecutionContext(),
            INTERMEDIATE_OG_IDS_FILE_NAME
        );

        try {
            // Generate unit distribution + intermediate distribution for ObjectGroups
            MetadataStats unitStats = generateUnitsDistributionsAndPrepareObjectGroupDistributions(
                reassignmentRequest,
                handler,
                unitToUpdateDistributionFile,
                ogIdsIntermediateFile
            );

            // Generate Object Group distribution from intermediate distribution
            MetadataStats objectGroupStats = handleObjectGroupReassignmentGenerationDistributions(
                handler,
                param,
                ogIdsIntermediateFile,
                reassignmentRequest
            );

            // Validate selection (limitation of Vitam 9.1)
            validateSelectionScopeTemporaryLimitation(handler, unitStats, objectGroupStats);

            // Write stats
            persistReassignmentStatistics(handler, unitStats, objectGroupStats);

            // Report status
            if (unitStats.nbEntries() == 0 && objectGroupStats.nbEntries() == 0) {
                return buildItemStatus(
                    PLUGIN_NAME,
                    StatusCode.KO,
                    PluginHelper.EventDetails.of("No metadata selected")
                );
            }
            if (reassignmentRequest.isPropagateToObjectGroups() && objectGroupStats.nbEntries() == 0) {
                return buildItemStatus(PLUGIN_NAME, StatusCode.WARNING, null);
            }
            return buildItemStatus(PLUGIN_NAME, StatusCode.OK, null);
        } finally {
            FileUtils.deleteQuietly(unitToUpdateDistributionFile);
            FileUtils.deleteQuietly(ogIdsIntermediateFile);
        }
    }

    private MetadataStats generateUnitsDistributionsAndPrepareObjectGroupDistributions(
        OriginatingAgencyReassignmentRequest reassignmentRequest,
        HandlerIO handler,
        File unitToUpdateDistributionFile,
        File ogIdsIntermediateFile
    ) throws ProcessingStatusException {
        try (MetaDataClient metadataClient = handler.getMetaDataClient()) {
            SelectMultiQuery selectMultiQuery = createUnitSelectMultiple(reassignmentRequest.getDslRequest());
            ScrollSpliterator<JsonNode> unitScrollSpliterator = ScrollSpliteratorHelper.createUnitScrollSplitIterator(
                metadataClient,
                selectMultiQuery
            );

            Iterator<JsonNode> unitIterator = new SpliteratorIterator<>(unitScrollSpliterator);
            Set<String> initialOperations = new HashSet<>();
            int nbUnits = 0;

            try (
                final FileOutputStream exportUnitsFileOutputStream = new FileOutputStream(unitToUpdateDistributionFile);
                JsonLineWriter<JsonLineModel> unitsToUpdateWriter = new JsonLineWriter<>(exportUnitsFileOutputStream);
                final OutputStream exportOgIdsOutputStream = new FileOutputStream(ogIdsIntermediateFile);
                JsonLineWriter<String> exportOgFileWriter = new JsonLineWriter<>(exportOgIdsOutputStream)
            ) {
                while (unitIterator.hasNext()) {
                    JsonNode unit = unitIterator.next();
                    String unitId = unit.get(VitamFieldsHelper.id()).asText();

                    boolean unitToUpdateOriginatingAgency = validateAndCheckToAddUnitToDistributionFile(
                        reassignmentRequest,
                        unit
                    );

                    if (unitToUpdateOriginatingAgency) {
                        nbUnits++;
                        initialOperations.add(unit.get(VitamFieldsHelper.initialOperation()).asText());
                        // Write units sorted by level #max
                        unitsToUpdateWriter.addEntry(
                            new JsonLineModel(unitId, unit.get(VitamFieldsHelper.max()).asInt(), unit)
                        );
                    }

                    // To avoid ES timeout (bug 15611), write object groups to a temp file, before writing inserting them into BatchReport
                    if (reassignmentRequest.isPropagateToObjectGroups() && unit.has(VitamFieldsHelper.object())) {
                        exportOgFileWriter.addEntry(unit.get(VitamFieldsHelper.object()).asText());
                    }
                }
            }

            // Transfer file to workspace but keep it locally. We'll be needing it later on
            handler.transferFileToWorkspace(UNITS_TO_UPDATE_FILE, unitToUpdateDistributionFile, false, false);

            return new MetadataStats(nbUnits, initialOperations);
        } catch (IOException | InvalidParseOperationException | ProcessingException e) {
            throw new ProcessingStatusException(StatusCode.FATAL, "Could not prepare units to update", e);
        }
    }

    private boolean validateAndCheckToAddUnitToDistributionFile(
        OriginatingAgencyReassignmentRequest reassignmentRequest,
        JsonNode unit
    ) throws ProcessingStatusException {
        if (!unit.has(VitamFieldsHelper.originatingAgency())) {
            throw new ProcessingStatusException(StatusCode.KO, "This action is not allowed for Holding units ");
        }
        String currentOriginatingAgency = unit.get(VitamFieldsHelper.originatingAgency()).asText();

        boolean validSourceAndTargetRequest =
            Objects.equals(currentOriginatingAgency, reassignmentRequest.getSourceOriginatingAgency()) ||
            Objects.equals(currentOriginatingAgency, reassignmentRequest.getTargetOriginatingAgency());

        if (!validSourceAndTargetRequest) {
            throw new ProcessingStatusException(
                StatusCode.KO,
                "OriginatingAgency does not match source neither target Originating Agency"
            );
        }
        return Objects.equals(currentOriginatingAgency, reassignmentRequest.getSourceOriginatingAgency());
    }

    private SelectMultiQuery createUnitSelectMultiple(JsonNode initialQuery) throws InvalidParseOperationException {
        SelectParserMultiple parser = new SelectParserMultiple();
        parser.parse(initialQuery);
        SelectMultiQuery selectMultiQuery = parser.getRequest();

        selectMultiQuery.resetUsedProjection();
        selectMultiQuery.addUsedProjection(
            VitamFieldsHelper.id(),
            VitamFieldsHelper.max(),
            VitamFieldsHelper.originatingAgency(),
            VitamFieldsHelper.originatingAgencies(),
            VitamFieldsHelper.initialOperation(),
            VitamFieldsHelper.unitups(),
            VitamFieldsHelper.object(),
            VitamFieldsHelper.allunitups(),
            VitamFieldsHelper.validComputedInheritedRules()
        );
        selectMultiQuery.addOrderByAscFilter(VitamFieldsHelper.max());
        return selectMultiQuery;
    }

    private MetadataStats handleObjectGroupReassignmentGenerationDistributions(
        HandlerIO handler,
        WorkerParameters param,
        File ogIdsToFillInIntermediateFile,
        OriginatingAgencyReassignmentRequest reassignmentRequest
    ) throws ProcessingStatusException {
        try {
            if (!reassignmentRequest.isPropagateToObjectGroups()) {
                // Create an empty distribution file for OGs update step
                createEmptyObjectGroupDistributionFile(handler);
                return new MetadataStats(0, Collections.emptySet());
            }

            exportIntermediateObjectGroupIdToUpdateOriginatingAgenciesFileToBatchReport(
                handler,
                ogIdsToFillInIntermediateFile,
                reassignmentRequest
            );

            try (BatchReportClient batchReportClient = handler.getBatchReportClient()) {
                batchReportClient.exportReassignmentObjectGroups(
                    handler.getContainerName(),
                    new ReportExportRequest(OBJECT_GROUPS_TO_UPDATE_JSONL_FILE),
                    param.getExecutionContext()
                );
            }

            // Compute stats after object group deduplication in batch report
            File ogToUpdateDistributionFile = null;
            try {
                ogToUpdateDistributionFile = handler.getFileFromWorkspace(
                    handler.getWorkFlowExecutionContext(),
                    OBJECT_GROUPS_TO_UPDATE_JSONL_FILE
                );
                try (
                    JsonLineIterator<JsonLineModel> iterator = JsonLineIterator.fromFile(
                        ogToUpdateDistributionFile,
                        new TypeReference<>() {}
                    )
                ) {
                    int nbObjectGroups = 0;
                    Set<String> initialOperations = new HashSet<>();
                    while (iterator.hasNext()) {
                        JsonLineModel entry = iterator.next();
                        initialOperations.add(entry.getParams().get(VitamFieldsHelper.initialOperation()).asText());
                        nbObjectGroups++;
                    }
                    return new MetadataStats(nbObjectGroups, initialOperations);
                }
            } finally {
                FileUtils.deleteQuietly(ogToUpdateDistributionFile);
            }
        } catch (
            IOException
            | VitamClientInternalException
            | ProcessingException
            | ContentAddressableStorageNotFoundException
            | ContentAddressableStorageServerException e
        ) {
            throw new ProcessingStatusException(
                StatusCode.FATAL,
                "An error occurred during object group distribution",
                e
            );
        }
    }

    private void createEmptyObjectGroupDistributionFile(HandlerIO handler)
        throws IOException, ProcessingStatusException, ProcessingException {
        File ogToUpdateIntermediateFile = handler.getNewLocalFile(
            handler.getWorkFlowExecutionContext(),
            OBJECT_GROUPS_TO_UPDATE_JSONL_FILE
        );
        boolean newFileCreated = ogToUpdateIntermediateFile.createNewFile();

        if (!newFileCreated) {
            throw new ProcessingStatusException(
                StatusCode.FATAL,
                "Could not create an empty file for step distribution"
            );
        }

        // Transfer file to workspace but keep it locally. We'll be needing it later on
        handler.transferFileToWorkspace(OBJECT_GROUPS_TO_UPDATE_JSONL_FILE, ogToUpdateIntermediateFile, false, false);
    }

    public void exportIntermediateObjectGroupIdToUpdateOriginatingAgenciesFileToBatchReport(
        HandlerIO handler,
        File ogIdsToFillFile,
        OriginatingAgencyReassignmentRequest reassignmentRequest
    ) throws ProcessingStatusException {
        try (
            BatchReportClient batchReportClient = handler.getBatchReportClient();
            InputStream inputStream = new FileInputStream(ogIdsToFillFile)
        ) {
            Iterator<List<String>> partitions = Iterators.partition(
                new JsonLineIterator<>(inputStream, new TypeReference<>() {}),
                VitamConfiguration.getBatchSize()
            );

            while (partitions.hasNext()) {
                // Deduplicate GOTs if any
                Set<String> objectGroupIds = new HashSet<>(partitions.next());

                List<ObjectNode> updatableObjectGroups = filterObjectGroupsHavingOriginatingAgency(
                    handler,
                    objectGroupIds,
                    reassignmentRequest.getSourceOriginatingAgency()
                );

                originatingAgencyReassignmentService.appendReassignmentObjectGroupIdToUpdateOriginatingAgencyToBatchReport(
                    handler,
                    updatableObjectGroups,
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

    private List<ObjectNode> filterObjectGroupsHavingOriginatingAgency(
        HandlerIO handlerIO,
        Set<String> objectGroupsIds,
        String originatingAgency
    )
        throws InvalidParseOperationException, InvalidCreateOperationException, MetaDataExecutionException, MetaDataClientServerException, MetaDataDocumentSizeException, ProcessingStatusException {
        if (CollectionUtils.isEmpty(objectGroupsIds)) {
            return Collections.emptyList();
        }
        try (MetaDataClient metaDataClient = handlerIO.getMetaDataClient()) {
            SelectMultiQuery selectMultiQuery = new SelectMultiQuery();

            InQuery inQuery = QueryHelper.in(VitamFieldsHelper.id(), objectGroupsIds.toArray(new String[0]));

            CompareQuery originatingAgencyQuery = QueryHelper.eq(
                VitamFieldsHelper.originatingAgency(),
                originatingAgency
            );
            BooleanQuery searchQuery = QueryHelper.and().add(inQuery, originatingAgencyQuery);
            selectMultiQuery.setQuery(searchQuery);

            selectMultiQuery.addUsedProjection(
                VitamFieldsHelper.id(),
                VitamFieldsHelper.initialOperation(),
                OBJECT_OPI
            );

            JsonNode results = metaDataClient.selectObjectGroups(selectMultiQuery.getFinalSelect()).get("$results");

            List<ObjectNode> result = new ArrayList<>();
            for (JsonNode objectGroup : results) {
                validateObjectGroupOpiTemporaryLimitation(objectGroup);
                result.add((ObjectNode) objectGroup);
            }
            return result;
        }
    }

    /**
     * @deprecated Limitation of Vitam 9.1 : ObjectGroups cannot have the same opi.
     * In this version, Vitam does not support partial originating agency update for a single AccessionRegisterDetail.
     * Only full AccessionRegisterDetail update is supported.
     * This is mainly done to ensure that we won't end with 2 AccessRegisterDetails for the same preservation operation,
     * with the same Originating Agency.
     */
    private void validateObjectGroupOpiTemporaryLimitation(JsonNode objectGroup) throws ProcessingStatusException {
        if (!_____Enable_Temporary_V91_Restrictions_____) {
            return;
        }
        String opi = getInitialOperation(objectGroup);
        for (JsonNode qualifier : objectGroup.get(VitamFieldsHelper.qualifiers())) {
            for (JsonNode version : qualifier.get(VERSIONS)) {
                String objectOpi = getInitialOperation(version);
                if (!Objects.equals(opi, objectOpi)) {
                    ObjectNode evDetData = JsonHandler.createObjectNode();
                    evDetData.put("objectGroupId", objectGroup.get(VitamFieldsHelper.id()).asText());
                    evDetData.put("opi", opi);
                    evDetData.put("objectOpi", objectOpi);
                    throw new ProcessingStatusException(
                        StatusCode.KO,
                        evDetData,
                        "An object group have objects from other ingest or preservation operations"
                    );
                }
            }
        }
    }

    private void persistReassignmentStatistics(
        HandlerIO handler,
        MetadataStats unitStats,
        MetadataStats objectGroupStats
    ) throws ProcessingStatusException {
        try {
            ReassignmentStatistics reassignmentStats = new ReassignmentStatistics(
                unitStats.nbEntries(),
                objectGroupStats.nbEntries()
            );
            File reassignmentStatsFile = handler.getNewLocalFile(
                handler.getWorkFlowExecutionContext(),
                REASSIGNMENT_STATISTICS_JSON_FILE
            );
            JsonHandler.writeAsFile(reassignmentStats, reassignmentStatsFile);
            handler.transferFileToWorkspace(REASSIGNMENT_STATISTICS_JSON_FILE, reassignmentStatsFile, true, false);
        } catch (ProcessingException | InvalidParseOperationException e) {
            throw new ProcessingStatusException(
                StatusCode.FATAL,
                "An error occurred during reassignment statistics persistence",
                e
            );
        }
    }

    /**
     * @deprecated Limitation of Vitam 9.1 : Originating agency reassignment can only update whole OPIs : all current
     * units + objectGroups + objects of the same OPI must be updated as a whole.
     * This is because in this version, Vitam does not support partial originating agency update for a single
     * AccessionRegisterDetail. Only full AccessionRegisterDetail update is supported.
     */
    private void validateSelectionScopeTemporaryLimitation(
        HandlerIO handler,
        MetadataStats unitStats,
        MetadataStats objectGroupStats
    ) throws ProcessingStatusException {
        if (!_____Enable_Temporary_V91_Restrictions_____) {
            return;
        }
        try {
            Set<String> combinedInitialOperations = new HashSet<>();
            combinedInitialOperations.addAll(unitStats.initialOperations());
            combinedInitialOperations.addAll(objectGroupStats.initialOperations());

            if (combinedInitialOperations.size() > 1000) {
                throw new ProcessingStatusException(StatusCode.KO, "Too many #opi selected by query");
            }

            validateUnitSelectionScope(handler, combinedInitialOperations, unitStats.nbEntries());

            validateObjectGroupSelectionScope(handler, combinedInitialOperations, objectGroupStats.nbEntries());
        } catch (
            InvalidCreateOperationException
            | InvalidParseOperationException
            | MetaDataExecutionException
            | MetaDataDocumentSizeException
            | MetaDataClientServerException e
        ) {
            throw new ProcessingStatusException(StatusCode.FATAL, "Could not validate reassignment scope", e);
        }
    }

    /**
     * @deprecated Limitation of Vitam 9.1 :
     */
    private void validateUnitSelectionScope(HandlerIO handler, Set<String> initialOperations, int expectedTotal)
        throws InvalidCreateOperationException, InvalidParseOperationException, MetaDataExecutionException, MetaDataDocumentSizeException, MetaDataClientServerException, ProcessingStatusException {
        try (MetaDataClient metaDataClient = handler.getMetaDataClient()) {
            SelectMultiQuery selectMultiQuery = new SelectMultiQuery();
            selectMultiQuery.setQuery(
                QueryHelper.in(VitamFieldsHelper.initialOperation(), initialOperations.toArray(new String[0]))
            );
            selectMultiQuery.addUsedProjection(VitamFieldsHelper.id());
            selectMultiQuery.setLimitFilter(0L, 1L);
            selectMultiQuery.trackTotalHits(true);

            JsonNode result = metaDataClient.selectUnits(selectMultiQuery.getFinalSelect());
            RequestResponseOK<JsonNode> requestResponse = RequestResponseOK.getFromJsonNode(result, JsonNode.class);
            int total = (int) requestResponse.getHits().getTotal();

            if (total != expectedTotal) {
                ObjectNode evDetData = JsonHandler.createObjectNode();
                evDetData.put("error", "Invalid query perimeter");
                evDetData.put("nbUnitSelected", total);
                evDetData.put("nbUnitAvailable", expectedTotal);
                throw new ProcessingStatusException(
                    StatusCode.KO,
                    evDetData,
                    "Invalid query perimeter. Selected " + expectedTotal + " out of " + total + " units"
                );
            }
        }
    }

    /**
     * @deprecated Limitation of Vitam 9.1 :
     */
    private void validateObjectGroupSelectionScope(HandlerIO handler, Set<String> initialOperations, int expectedTotal)
        throws InvalidParseOperationException, MetaDataExecutionException, MetaDataClientServerException, MetaDataDocumentSizeException, ProcessingStatusException, InvalidCreateOperationException {
        try (MetaDataClient metaDataClient = handler.getMetaDataClient()) {
            SelectMultiQuery selectMultiQuery = new SelectMultiQuery();
            // Query both object group #opi, and object #opi to ensure
            selectMultiQuery.setQuery(
                QueryHelper.or()
                    .add(
                        QueryHelper.in(VitamFieldsHelper.initialOperation(), initialOperations.toArray(new String[0])),
                        QueryHelper.in(OBJECT_OPI, initialOperations.toArray(new String[0]))
                    )
            );
            selectMultiQuery.addUsedProjection(VitamFieldsHelper.id());
            selectMultiQuery.setLimitFilter(0L, 1L);
            selectMultiQuery.trackTotalHits(true);

            JsonNode result = metaDataClient.selectObjectGroups(selectMultiQuery.getFinalSelect());
            RequestResponseOK<JsonNode> requestResponse = RequestResponseOK.getFromJsonNode(result, JsonNode.class);
            int total = (int) requestResponse.getHits().getTotal();

            if (total != expectedTotal) {
                ObjectNode evDetData = JsonHandler.createObjectNode();
                evDetData.put("error", "Invalid query perimeter");
                evDetData.put("nbObjectGroupsSelected", total);
                evDetData.put("nbObjectGroupsAvailable", expectedTotal);
                throw new ProcessingStatusException(
                    StatusCode.KO,
                    evDetData,
                    "Invalid query perimeter. Selected " + expectedTotal + " out of " + total + " object groups"
                );
            }
        }
    }

    private String getInitialOperation(JsonNode metadata) {
        return metadata.get(VitamFieldsHelper.initialOperation()).asText();
    }

    public static String getId() {
        return PLUGIN_NAME;
    }

    private record MetadataStats(Integer nbEntries, Set<String> initialOperations) {}
}
