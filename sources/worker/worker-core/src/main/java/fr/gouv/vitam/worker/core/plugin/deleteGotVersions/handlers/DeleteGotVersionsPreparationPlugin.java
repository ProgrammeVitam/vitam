/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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

package fr.gouv.vitam.worker.core.plugin.deleteGotVersions.handlers;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import fr.gouv.vitam.batch.report.model.entry.DeleteGotVersionsReportEntry;
import fr.gouv.vitam.batch.report.model.entry.ObjectGroupToDeleteReportEntry;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.DeleteGotVersionsRequest;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.DataObjectVersionType;
import fr.gouv.vitam.common.model.objectgroup.ObjectGroupResponse;
import fr.gouv.vitam.common.model.objectgroup.QualifiersModel;
import fr.gouv.vitam.common.model.objectgroup.VersionsModel;
import fr.gouv.vitam.common.model.objectgroup.VersionsModelCustomized;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.distribution.JsonLineGenericIterator;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import fr.gouv.vitam.worker.core.distribution.JsonLineWriter;
import fr.gouv.vitam.worker.core.exception.ProcessingStatusException;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.plugin.deleteGotVersions.services.DeleteGotVersionsReportService;
import fr.gouv.vitam.worker.core.utils.PluginHelper;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.LocalDateUtil.now;
import static fr.gouv.vitam.common.json.JsonHandler.createObjectNode;
import static fr.gouv.vitam.common.json.JsonHandler.getFromJsonNode;
import static fr.gouv.vitam.common.json.JsonHandler.getFromJsonNodeList;
import static fr.gouv.vitam.common.json.JsonHandler.toJsonNode;
import static fr.gouv.vitam.common.model.StatusCode.FATAL;
import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.common.model.StatusCode.WARNING;
import static fr.gouv.vitam.common.model.administration.DataObjectVersionType.BINARY_MASTER;
import static fr.gouv.vitam.common.model.administration.DataObjectVersionType.PHYSICAL_MASTER;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;

public class DeleteGotVersionsPreparationPlugin extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DeleteGotVersionsPreparationPlugin.class);

    private static final String PLUGIN_NAME = "DELETE_GOT_VERSIONS_PREPARATION";
    public static final String FIRST_BINARY_MASTER = BINARY_MASTER.getName() + "_1";
    public static final String DISTRIBUTION_FILE_OG = "distributionFileOG.jsonl";

    private final MetaDataClientFactory metaDataClientFactory;
    private final DeleteGotVersionsReportService reportService;

    public DeleteGotVersionsPreparationPlugin() {
        this(MetaDataClientFactory.getInstance(), new DeleteGotVersionsReportService());
    }

    @VisibleForTesting
    public DeleteGotVersionsPreparationPlugin(MetaDataClientFactory metaDataClientFactory,
        DeleteGotVersionsReportService reportService) {
        this.metaDataClientFactory = metaDataClientFactory;
        this.reportService = reportService;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) {

        try (MetaDataClient metaDataClient = metaDataClientFactory.getClient()) {

            // Validate Request
            DeleteGotVersionsRequest deleteGotVersionsRequest = loadRequest(handler);
            Pair<StatusCode, String> requestValidation = validateRequest(deleteGotVersionsRequest);
            if (requestValidation.getLeft().isGreaterOrEqualToWarn()) {
                ObjectNode errorNode = createObjectNode();
                errorNode.put("error", requestValidation.getRight());
                return buildItemStatus(PLUGIN_NAME, requestValidation.getLeft(), errorNode);
            }

            // Get generated unitsByGot InputStream
            InputStream unitsByGotInputStream =
                PluginHelper.createUnitsByGotFile(metaDataClient, deleteGotVersionsRequest, handler);

            // Create objectGroupWithDetails distribution file
            createObjectGroupDistributionFile(handler, deleteGotVersionsRequest, param, unitsByGotInputStream,
                metaDataClient);

            return buildItemStatus(PLUGIN_NAME, OK);

        } catch (Exception e) {
            LOGGER.error(String.format("Delete got versions preparation failed with status [%s]", FATAL), e);
            ObjectNode error = createObjectNode().put("error", e.getMessage());
            return buildItemStatus(PLUGIN_NAME, FATAL, error);
        }
    }

    private void createObjectGroupDistributionFile(HandlerIO handler, DeleteGotVersionsRequest deleteGotVersionsRequest,
        WorkerParameters param, InputStream unitsByGotInputStream, MetaDataClient metaDataClient)
        throws VitamException, ProcessingStatusException, IOException {
        File objectGroupsIdsFile = handler.getNewLocalFile("object_groups_to_update.jsonl");
        try (OutputStream outputStream = new FileOutputStream(objectGroupsIdsFile);
            JsonLineGenericIterator<JsonNode> jsonLineIterator =
                new JsonLineGenericIterator<>(unitsByGotInputStream, new TypeReference<>() {
                });
            JsonLineWriter writer = new JsonLineWriter(outputStream)) {
            Iterator<List<JsonNode>> jsonLineIteratorPartition =
                Iterators.partition(jsonLineIterator, VitamConfiguration.getBatchSize());
            while (jsonLineIteratorPartition.hasNext()) {
                List<UnitsByGotModel> unitsByGot =
                    getFromJsonNodeList(jsonLineIteratorPartition.next(), new TypeReference<>() {
                    });
                Map<String, Set<String>> unitsByGotConvertedMap =
                    unitsByGot.stream()
                        .collect(Collectors.toMap(UnitsByGotModel::getGotId, UnitsByGotModel::getUnitIds));
                String[] gotIds = unitsByGot.stream().map(UnitsByGotModel::getGotId).toArray(String[]::new);
                Map<String, ObjectGroupResponse> objectGroupRowsPartition =
                    PluginHelper.getObjectGroups(gotIds, metaDataClient);
                if (objectGroupRowsPartition == null || objectGroupRowsPartition.isEmpty()) {
                    throw new IllegalStateException("No objects Group found in database!");
                }
                Map<Pair<String, Set<String>>, List<ObjectGroupToDeleteReportEntry>> objectGroupRowsForReport =
                    new HashMap<>();
                objectGroupRowsPartition.forEach((gotId, objectGroupResponse) -> {
                    List<ObjectGroupToDeleteReportEntry> gotWIthDetailsForDistribution =
                        generateGotWithDetails(objectGroupResponse, deleteGotVersionsRequest);
                    objectGroupRowsForReport
                        .put(Pair.of(gotId, unitsByGotConvertedMap.get(gotId)),
                            gotWIthDetailsForDistribution);
                    try {
                        JsonLineModel jsonLineModel =
                            new JsonLineModel(gotId, null, toJsonNode(gotWIthDetailsForDistribution));
                        writer.addEntry(jsonLineModel);
                    } catch (IOException | InvalidParseOperationException e) {
                        throw new RuntimeException("Problem occured when writing data to distribution file", e);
                    }
                });
                // create report entries per partition
                createReport(param, objectGroupRowsForReport);
            }
        }

        // write distribution file
        handler.transferFileToWorkspace(DISTRIBUTION_FILE_OG, objectGroupsIdsFile, true, false);
    }

    private void createReport(WorkerParameters param,
        Map<Pair<String, Set<String>>, List<ObjectGroupToDeleteReportEntry>> objectGroupRawsGlobal)
        throws ProcessingStatusException {
        final Integer tenantId = VitamThreadUtils.getVitamSession().getTenantId();
        List<Object> deleteGotVersionsReportEntries = new ArrayList<>();
        for (Entry<Pair<String, Set<String>>, List<ObjectGroupToDeleteReportEntry>> entry : objectGroupRawsGlobal
            .entrySet()) {
            deleteGotVersionsReportEntries.add(
                new DeleteGotVersionsReportEntry(entry.getKey().getLeft(), param.getRequestId(), tenantId,
                    now().toString(), entry.getKey().getLeft(), entry.getKey().getRight(),
                    entry.getValue(), null));
        }
        if (!deleteGotVersionsReportEntries.isEmpty()) {
            reportService.appendEntries(param.getContainerName(), deleteGotVersionsReportEntries);
        }
    }

    private List<ObjectGroupToDeleteReportEntry> generateGotWithDetails(ObjectGroupResponse objectGroup,
        DeleteGotVersionsRequest deleteGotVersionsRequest) {
        List<ObjectGroupToDeleteReportEntry> objectGroupReportEntries = new ArrayList<>();
        List<QualifiersModel> qualifiers = objectGroup.getQualifiers();
        Optional<QualifiersModel> optionalQualifierToUpdate = qualifiers.stream()
            .filter(elmt -> elmt.getQualifier().equals(deleteGotVersionsRequest.getUsageName()))
            .findFirst();

        if (optionalQualifierToUpdate.isEmpty()) {
            final String errorMsg = String.format("No qualifier of Object group matches with %s usage",
                deleteGotVersionsRequest.getUsageName());
            objectGroupReportEntries.add(new ObjectGroupToDeleteReportEntry(KO, errorMsg, null));
            return objectGroupReportEntries;
        }

        QualifiersModel qualifierToUpdate = optionalQualifierToUpdate.get();
        if (qualifierToUpdate.getVersions() == null || qualifierToUpdate.getVersions().isEmpty()) {
            final String errorMsg =
                String.format("No versions associated to the qualifier of Object group for the %s usage",
                    deleteGotVersionsRequest.getUsageName());
            objectGroupReportEntries.add(new ObjectGroupToDeleteReportEntry(KO, errorMsg, null));
            return objectGroupReportEntries;
        }

        List<VersionsModelCustomized> deletedVersions = new ArrayList<>();
        for (Integer version : deleteGotVersionsRequest.getSpecificVersions()) {
            Optional<VersionsModel> specificVersionModel =
                qualifierToUpdate.getVersions().stream().filter(elmt -> elmt.getDataObjectVersion().equals(
                    deleteGotVersionsRequest.getUsageName() + "_" + version)).findFirst();

            if (specificVersionModel.isEmpty()) {
                ObjectGroupToDeleteReportEntry objectGroupToDeleteReportEntry =
                    new ObjectGroupToDeleteReportEntry(WARNING,
                        String.format("Qualifier with this specific version %s is inexistant!", version), null);
                objectGroupReportEntries.add(objectGroupToDeleteReportEntry);
                continue;
            }

            VersionsModel versionModelToDelete = specificVersionModel.get();
            if (checkForbiddenVersion(versionModelToDelete)) {
                ObjectGroupToDeleteReportEntry objectGroupToDeleteReportEntry =
                    new ObjectGroupToDeleteReportEntry(WARNING,
                        String.format("Qualifier with forbidden version %s has been detected!", version), null);
                objectGroupReportEntries.add(objectGroupToDeleteReportEntry);
                continue;
            }

            if (checkLastUsageVersion(deleteGotVersionsRequest.getUsageName(), qualifierToUpdate, version)) {
                ObjectGroupToDeleteReportEntry objectGroupToDeleteReportEntry =
                    new ObjectGroupToDeleteReportEntry(WARNING,
                        String.format("The last version of %s usage cannot be deleted.",
                            deleteGotVersionsRequest.getUsageName()), null);
                objectGroupReportEntries.add(objectGroupToDeleteReportEntry);
                continue;
            }

            deletedVersions.add(customizeVersionModel(versionModelToDelete, objectGroup.getOpi()));
        }

        if (!deletedVersions.isEmpty()) {
            ObjectGroupToDeleteReportEntry objectGroupToDeleteReportEntry =
                new ObjectGroupToDeleteReportEntry(OK, null, deletedVersions);
            objectGroupReportEntries.add(objectGroupToDeleteReportEntry);
        }

        return objectGroupReportEntries;
    }

    private VersionsModelCustomized customizeVersionModel(VersionsModel versionModelToDelete, String opIngest) {
        VersionsModelCustomized versionsModelCustomized = new VersionsModelCustomized();
        versionsModelCustomized.setId(versionModelToDelete.getId());
        versionsModelCustomized.setOpIngest(opIngest);
        versionsModelCustomized.setOpCurrent(versionModelToDelete.getOpi());
        versionsModelCustomized.setSize(versionModelToDelete.getSize());
        versionsModelCustomized.setDataObjectVersion(versionModelToDelete.getDataObjectVersion());
        if (versionModelToDelete.getStorage() != null) {
            versionsModelCustomized.setStrategyId(versionModelToDelete.getStorage().getStrategyId());
        }
        return versionsModelCustomized;
    }

    private boolean checkLastUsageVersion(String usageName, QualifiersModel qualifierToUpdate, Integer version) {
        qualifierToUpdate.getVersions()
            .sort(Comparator.comparingInt(this::extractVersionFromVersionModel));
        VersionsModel lastVersion = Iterables.getLast(qualifierToUpdate.getVersions());
        return lastVersion.getDataObjectVersion().equals(usageName + "_" + version);
    }

    private int extractVersionFromVersionModel(VersionsModel objectVersion) {
        return Integer.parseInt(objectVersion.getDataObjectVersion().split("_")[1]);
    }

    private boolean checkForbiddenVersion(VersionsModel specificVersionMode) {
        return FIRST_BINARY_MASTER.equals(specificVersionMode.getDataObjectVersion()) ||
            specificVersionMode.getDataObjectVersion().startsWith(PHYSICAL_MASTER.getName());
    }

    private Pair<StatusCode, String> validateRequest(DeleteGotVersionsRequest deleteGotVersionsRequest) {
        if (Arrays.stream(DataObjectVersionType.values())
            .noneMatch(elmt -> elmt.getName().equals(deleteGotVersionsRequest.getUsageName()))) {
            return Pair.of(KO, "Usage name is unknown.");
        }

        if (deleteGotVersionsRequest.getSpecificVersions() == null ||
            deleteGotVersionsRequest.getSpecificVersions().isEmpty()) {
            return Pair.of(KO, "Specific versions list is empty.");
        }

        if (isVersionsDuplicated(deleteGotVersionsRequest)) {
            return Pair.of(KO, "Duplicated versions are detected.");
        }
        return Pair.of(OK, null);
    }

    private boolean isVersionsDuplicated(DeleteGotVersionsRequest deleteGotVersionsRequest) {
        return deleteGotVersionsRequest.getSpecificVersions().stream().distinct().count() !=
            deleteGotVersionsRequest.getSpecificVersions().size();
    }

    private DeleteGotVersionsRequest loadRequest(HandlerIO handler)
        throws ProcessingException, InvalidParseOperationException {
        JsonNode inputRequest = handler.getJsonFromWorkspace("deleteGotVersionsRequest");
        return getFromJsonNode(inputRequest, DeleteGotVersionsRequest.class);
    }
}


class UnitsByGotModel {
    @JsonProperty("id")
    public String gotId;

    @JsonProperty("params")
    public Set<String> unitIds;

    public UnitsByGotModel(String gotId, Set<String> unitIds) {
        this.gotId = gotId;
        this.unitIds = unitIds;
    }

    public UnitsByGotModel() {
    }

    public String getGotId() {
        return gotId;
    }

    public void setGotId(String gotId) {
        this.gotId = gotId;
    }

    public Set<String> getUnitIds() {
        return unitIds;
    }

    public void setUnitIds(Set<String> unitIds) {
        this.unitIds = unitIds;
    }
}
