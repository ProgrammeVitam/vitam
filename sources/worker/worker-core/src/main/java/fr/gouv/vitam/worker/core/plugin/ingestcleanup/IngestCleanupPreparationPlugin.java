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
package fr.gouv.vitam.worker.core.plugin.ingestcleanup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.batch.report.model.PurgeAccessionRegisterModel;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.utils.MetadataDocumentHelper;
import fr.gouv.vitam.common.database.utils.ScrollSpliterator;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AccessionRegisterDetailModel;
import fr.gouv.vitam.common.model.objectgroup.ObjectGroupResponse;
import fr.gouv.vitam.common.model.objectgroup.VersionsModel;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameterName;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import fr.gouv.vitam.worker.core.distribution.JsonLineWriter;
import fr.gouv.vitam.worker.core.exception.ProcessingStatusException;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.plugin.ScrollSpliteratorHelper;
import fr.gouv.vitam.worker.core.plugin.ingestcleanup.report.CleanupReportManager;
import fr.gouv.vitam.worker.core.plugin.purge.PurgeObjectGroupParams;
import org.apache.commons.collections4.ListUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.exists;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;

public class IngestCleanupPreparationPlugin extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestCleanupPreparationPlugin.class);
    private static final String INGEST_CLEANUP_PREPARATION = "INGEST_CLEANUP_PREPARATION";

    public static final String UNITS_TO_DELETE_JSONL = "units.jsonl";
    public static final String OBJECT_GROUPS_TO_DELETE_JSONL = "objectGroups.jsonl";
    public static final String ACCESSION_REGISTERS_JSONL = "accession_register.jsonl";

    private final MetaDataClientFactory metaDataClientFactory;
    private final AdminManagementClientFactory adminManagementClientFactory;

    public IngestCleanupPreparationPlugin() {
        this(MetaDataClientFactory.getInstance(), AdminManagementClientFactory.getInstance());
    }

    @VisibleForTesting
    IngestCleanupPreparationPlugin(
        MetaDataClientFactory metaDataClientFactory,
        AdminManagementClientFactory adminManagementClientFactory
    ) {
        this.metaDataClientFactory = metaDataClientFactory;
        this.adminManagementClientFactory = adminManagementClientFactory;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException {
        try {
            String ingestOperationId = param.getParameterValue(WorkerParameterName.ingestOperationIdToCleanup);

            PurgeAccessionRegisterModel accessionRegisterModel = new PurgeAccessionRegisterModel();
            accessionRegisterModel.setOpi(ingestOperationId);

            CleanupReportManager cleanupReportManager = loadCleanupReportManager(handler);

            prepareUnits(handler, ingestOperationId, accessionRegisterModel, cleanupReportManager);
            prepareObjectGroups(handler, ingestOperationId, accessionRegisterModel, cleanupReportManager);

            prepareAccessingRegister(handler, ingestOperationId, accessionRegisterModel);

            cleanupReportManager.persistReportDataToWorkspace(handler);

            LOGGER.info("Ingest cleanup preparation succeeded");
            return buildItemStatus(INGEST_CLEANUP_PREPARATION, StatusCode.OK);
        } catch (ProcessingStatusException e) {
            LOGGER.error(String.format("Ingest cleanup preparation failed with status [%s]", e.getStatusCode()), e);
            return buildItemStatus(INGEST_CLEANUP_PREPARATION, e.getStatusCode(), e.getEventDetails());
        }
    }

    private CleanupReportManager loadCleanupReportManager(HandlerIO handler)
        throws ProcessingStatusException, ProcessingException {
        Optional<CleanupReportManager> cleanupReportManager = CleanupReportManager.loadReportDataFromWorkspace(handler);
        if (!cleanupReportManager.isPresent()) {
            throw new ProcessingException("Could not load report");
        }
        return cleanupReportManager.get();
    }

    private void prepareUnits(
        HandlerIO handler,
        String ingestOperationId,
        PurgeAccessionRegisterModel accessionRegisterModel,
        CleanupReportManager cleanupReportManager
    ) throws ProcessingStatusException {
        File unitsToDeleteFile = handler.getNewLocalFile(UNITS_TO_DELETE_JSONL);
        try (
            MetaDataClient client = metaDataClientFactory.getClient();
            FileOutputStream fos = new FileOutputStream(unitsToDeleteFile);
            JsonLineWriter writer = new JsonLineWriter(fos)
        ) {
            SelectMultiQuery query = new SelectMultiQuery();
            query.addUsedProjection(
                VitamFieldsHelper.id(),
                VitamFieldsHelper.originatingAgency(),
                VitamFieldsHelper.storage()
            );
            query.addQueries(eq(VitamFieldsHelper.initialOperation(), ingestOperationId));

            ScrollSpliterator<JsonNode> scrollRequest = ScrollSpliteratorHelper.createUnitScrollSplitIterator(
                client,
                query,
                VitamConfiguration.getBatchSize()
            );

            scrollRequest.forEachRemaining(unit -> {
                cleanupReportManager.reportDeletedUnit(unit.get(VitamFieldsHelper.id()).asText());
                writeToUnitDistributionFile(writer, unit);
                updateUnitAccessingRegisterData(accessionRegisterModel, unit);
            });
        } catch (IOException | InvalidParseOperationException | InvalidCreateOperationException | RuntimeException e) {
            throw new ProcessingStatusException(StatusCode.FATAL, "Could not create unit distribution file", e);
        }
        copyDistributionFileToWorkspace(handler, unitsToDeleteFile, UNITS_TO_DELETE_JSONL);
    }

    private void writeToUnitDistributionFile(JsonLineWriter writer, JsonNode unit) {
        try {
            String id = unit.get(VitamFieldsHelper.id()).asText();
            String strategyId = MetadataDocumentHelper.getStrategyIdFromUnit(unit);

            ObjectNode params = JsonHandler.createObjectNode().put("id", id).put("strategyId", strategyId);

            writer.addEntry(new JsonLineModel(id, null, params));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void updateUnitAccessingRegisterData(PurgeAccessionRegisterModel accessionRegisterModel, JsonNode unit) {
        accessionRegisterModel.setOriginatingAgency(unit.get(VitamFieldsHelper.originatingAgency()).asText());
        accessionRegisterModel.setTotalUnits(accessionRegisterModel.getTotalUnits() + 1);
    }

    private void prepareObjectGroups(
        HandlerIO handler,
        String ingestOperationId,
        PurgeAccessionRegisterModel accessionRegisterModel,
        CleanupReportManager cleanupReportManager
    ) throws ProcessingStatusException {
        File objectGroupsToDeleteFile = handler.getNewLocalFile(OBJECT_GROUPS_TO_DELETE_JSONL);

        try (
            MetaDataClient client = metaDataClientFactory.getClient();
            FileOutputStream fos = new FileOutputStream(objectGroupsToDeleteFile);
            JsonLineWriter writer = new JsonLineWriter(fos)
        ) {
            SelectMultiQuery query = new SelectMultiQuery();
            query.addQueries(eq(VitamFieldsHelper.initialOperation(), ingestOperationId));

            ScrollSpliterator<JsonNode> scrollRequest = ScrollSpliteratorHelper.createObjectGroupScrollSplitIterator(
                client,
                query,
                VitamConfiguration.getBatchSize()
            );

            scrollRequest.forEachRemaining(objectGroupNode -> {
                try {
                    ObjectGroupResponse objectGroup = JsonHandler.getFromJsonNode(
                        objectGroupNode,
                        ObjectGroupResponse.class
                    );

                    cleanupReportManager.reportDeletedObjectGroup(
                        objectGroup.getId(),
                        listBinaryObjectIds(objectGroup)
                    );
                    writeToObjectGroupDistributionFile(writer, objectGroup);
                    updateObjectGroupAccessingRegisterData(accessionRegisterModel, objectGroup);
                } catch (IOException | InvalidParseOperationException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (IOException | InvalidCreateOperationException | RuntimeException e) {
            throw new ProcessingStatusException(StatusCode.FATAL, "Could not create object group distribution file", e);
        }

        copyDistributionFileToWorkspace(handler, objectGroupsToDeleteFile, OBJECT_GROUPS_TO_DELETE_JSONL);
    }

    private void writeToObjectGroupDistributionFile(JsonLineWriter writer, ObjectGroupResponse objectGroup)
        throws IOException, InvalidParseOperationException {
        PurgeObjectGroupParams params = PurgeObjectGroupParams.fromObjectGroup(objectGroup);
        writer.addEntry(new JsonLineModel(objectGroup.getId(), null, JsonHandler.toJsonNode(params)));
    }

    private void updateObjectGroupAccessingRegisterData(
        PurgeAccessionRegisterModel accessionRegisterModel,
        ObjectGroupResponse objectGroup
    ) {
        accessionRegisterModel.setTotalObjectGroups(accessionRegisterModel.getTotalObjectGroups() + 1);
        accessionRegisterModel.setTotalObjects(accessionRegisterModel.getTotalObjects() + getObjectCount(objectGroup));
        accessionRegisterModel.setTotalSize(accessionRegisterModel.getTotalSize() + getTotalObjectSize(objectGroup));
    }

    private List<String> listBinaryObjectIds(ObjectGroupResponse objectGroup) {
        return ListUtils.emptyIfNull(objectGroup.getQualifiers())
            .stream()
            .flatMap(qualifier -> ListUtils.emptyIfNull(qualifier.getVersions()).stream())
            .filter(version -> version.getPhysicalId() == null)
            .map(VersionsModel::getId)
            .collect(Collectors.toList());
    }

    private Long getObjectCount(ObjectGroupResponse objectGroup) {
        return ListUtils.emptyIfNull(objectGroup.getQualifiers())
            .stream()
            .mapToLong(qualifier -> ListUtils.emptyIfNull(qualifier.getVersions()).size())
            .sum();
    }

    private Long getTotalObjectSize(ObjectGroupResponse objectGroup) {
        return ListUtils.emptyIfNull(objectGroup.getQualifiers())
            .stream()
            .flatMap(qualifier -> ListUtils.emptyIfNull(qualifier.getVersions()).stream())
            .filter(version -> version.getPhysicalId() == null)
            .mapToLong(VersionsModel::getSize)
            .sum();
    }

    private void prepareAccessingRegister(
        HandlerIO handler,
        String ingestOperationId,
        PurgeAccessionRegisterModel accessionRegisterModel
    ) throws ProcessingStatusException {
        File accessionRegisterFile = handler.getNewLocalFile(ACCESSION_REGISTERS_JSONL);

        try (
            FileOutputStream fos = new FileOutputStream(accessionRegisterFile);
            JsonLineWriter writer = new JsonLineWriter(fos)
        ) {
            if (!hasAccessionRegisterDetails(ingestOperationId)) {
                LOGGER.warn("Accession register details not found...");
            } else if (accessionRegisterModel.getTotalUnits() == 0 && accessionRegisterModel.getTotalObjects() == 0) {
                LOGGER.warn("No accession register details to update");
            } else {
                writer.addEntry(
                    new JsonLineModel(ingestOperationId, null, JsonHandler.toJsonNode(accessionRegisterModel))
                );
            }
        } catch (IOException | RuntimeException | InvalidParseOperationException e) {
            throw new ProcessingStatusException(
                StatusCode.FATAL,
                "Could not create accession register distribution file",
                e
            );
        }
        copyDistributionFileToWorkspace(handler, accessionRegisterFile, ACCESSION_REGISTERS_JSONL);
    }

    private boolean hasAccessionRegisterDetails(String operationId) throws ProcessingStatusException {
        try (AdminManagementClient adminManagementClient = adminManagementClientFactory.getClient()) {
            Select select = new Select();
            select.setQuery(
                QueryHelper.and()
                    .add(QueryHelper.eq(AccessionRegisterDetailModel.OPI, operationId), exists(VitamFieldsHelper.id()))
            );
            RequestResponse<AccessionRegisterDetailModel> accessionRegisterDetail =
                adminManagementClient.getAccessionRegisterDetail(select.getFinalSelect());

            if (accessionRegisterDetail.isOk()) {
                List<AccessionRegisterDetailModel> results =
                    ((RequestResponseOK<AccessionRegisterDetailModel>) accessionRegisterDetail).getResults();
                return !results.isEmpty();
            }
            throw new ProcessingStatusException(
                StatusCode.FATAL,
                "Could not check existing accessing register " + accessionRegisterDetail.getStatus()
            );
        } catch (ReferentialException | InvalidParseOperationException | InvalidCreateOperationException e) {
            throw new ProcessingStatusException(StatusCode.FATAL, "Could not check existing accessing register", e);
        }
    }

    private void copyDistributionFileToWorkspace(HandlerIO handler, File jsonlFile, String fileName)
        throws ProcessingStatusException {
        try {
            handler.transferFileToWorkspace(fileName, jsonlFile, true, false);
        } catch (ProcessingException e) {
            throw new ProcessingStatusException(
                StatusCode.FATAL,
                "Could not write object group distribution file " + fileName,
                e
            );
        }
    }
}
