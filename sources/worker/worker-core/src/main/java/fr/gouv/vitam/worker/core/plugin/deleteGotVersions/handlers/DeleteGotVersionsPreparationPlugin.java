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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import fr.gouv.vitam.batch.report.model.entry.DeleteGotVersionsReportEntry;
import fr.gouv.vitam.batch.report.model.entry.ObjectGroupToDeleteReportEntry;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.database.utils.ScrollSpliterator;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.iterables.SpliteratorIterator;
import fr.gouv.vitam.common.json.JsonHandler;
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
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
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
import fr.gouv.vitam.worker.core.utils.GroupByObjectIterator;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.and;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.exists;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.in;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTION.FIELDS;
import static fr.gouv.vitam.common.database.parser.query.ParserTokens.PROJECTIONARGS.ID;
import static fr.gouv.vitam.common.database.parser.query.ParserTokens.PROJECTIONARGS.OBJECT;
import static fr.gouv.vitam.common.json.JsonHandler.createObjectNode;
import static fr.gouv.vitam.common.json.JsonHandler.getFromJsonNode;
import static fr.gouv.vitam.common.json.JsonHandler.getFromStringAsTypeReference;
import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.common.model.StatusCode.WARNING;
import static fr.gouv.vitam.common.model.administration.DataObjectVersionType.BINARY_MASTER;
import static fr.gouv.vitam.common.model.administration.DataObjectVersionType.PHYSICAL_MASTER;
import static fr.gouv.vitam.worker.core.plugin.ScrollSpliteratorHelper.createUnitScrollSplitIterator;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;

public class DeleteGotVersionsPreparationPlugin extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DeleteGotVersionsPreparationPlugin.class);

    private static final String PLUGIN_NAME = "DELETE_GOT_VERSIONS_PREPARATION";
    public static final String FIRST_BINARY_MASTER = BINARY_MASTER.getName() + "_1";
    public static final String UNITS_BY_GOT_FILE = "unitsByGot.jsonl";
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

            // Create UnitsByOBjectGroup jsonl file
            createUnitsByGotFile(handler, metaDataClient, deleteGotVersionsRequest);

            // Create objectGroupWithDetails distribution file
            createObjectGroupDistributionFile(handler, deleteGotVersionsRequest, param);

            return buildItemStatus(PLUGIN_NAME, OK,
                createObjectNode().put("query", deleteGotVersionsRequest.getDslQuery().toString()));

        } catch (Exception e) {
            LOGGER.error(String.format("Delete got versions preparation failed with status [%s]", KO), e);
            ObjectNode error = createObjectNode().put("error", e.getMessage());
            return buildItemStatus(PLUGIN_NAME, KO, error);
        }
    }

    private List<Pair<String, List<String>>> getJsonLinesEntries(File unitsByObjectGroupeFile) {
        List<Pair<String, List<String>>> reportEntries = new ArrayList<>();
        try (InputStream inputStream = new FileInputStream(unitsByObjectGroupeFile);
            JsonLineGenericIterator<JsonLineModel> jsonLineIterator =
                new JsonLineGenericIterator<>(inputStream, new TypeReference<>() {
                })) {
            while (jsonLineIterator.hasNext()) {
                JsonLineModel entry = jsonLineIterator.next();
                reportEntries.add(Pair.of(entry.getId(), JsonHandler.getFromJsonNode(entry.getParams(), List.class)));
            }
        } catch (IOException | InvalidParseOperationException e) {
            e.printStackTrace();
        }
        return reportEntries;
    }

    private void createObjectGroupDistributionFile(HandlerIO handler,
        DeleteGotVersionsRequest deleteGotVersionsRequest, WorkerParameters param)
        throws VitamException, IOException, ProcessingStatusException {
        File unitsByObjectGroupeFile = handler.getFileFromWorkspace(UNITS_BY_GOT_FILE);
        List<Pair<String, List<String>>> jsonLineEntries = getJsonLinesEntries(unitsByObjectGroupeFile);
        Iterator<List<Pair<String, List<String>>>> unitsByGotPartition =
            Iterators.partition(jsonLineEntries.iterator(), VitamConfiguration.getBatchSize());
        Map<Pair<String, Set<String>>, JsonNode> objectGroupRawsGlobal = new HashMap<>();
        while (unitsByGotPartition.hasNext()) {
            List<Pair<String, List<String>>> unitsByGot = unitsByGotPartition.next();
            Map<String, ObjectGroupResponse> objectGroupRawsPartition = getObjectGroups(unitsByGot);
            objectGroupRawsPartition
                .forEach((gotId, objectGroupResponse) -> objectGroupRawsGlobal
                    .put(Pair.of(gotId, extractUnitsIds(gotId, unitsByGot)),
                        generateGotWithDetails(objectGroupResponse, deleteGotVersionsRequest)));
        }
        // write distribution file
        generateDistributionFile(objectGroupRawsGlobal, handler);

        // create report entries
        createReport(param, objectGroupRawsGlobal);
    }

    private void createReport(WorkerParameters param, Map<Pair<String, Set<String>>, JsonNode> objectGroupRawsGlobal)
        throws ProcessingStatusException, InvalidParseOperationException {
        final Integer tenantId = VitamThreadUtils.getVitamSession().getTenantId();
        List<Object> deleteGotVersionsReportEntries = new ArrayList<>();
        for (Entry<Pair<String, Set<String>>, JsonNode> entry : objectGroupRawsGlobal.entrySet()) {
            List<ObjectGroupToDeleteReportEntry> objectGroupToDeleteReportEntries = JsonHandler.getFromJsonNode(
                entry.getValue(), new TypeReference<>() {
                });
            deleteGotVersionsReportEntries.add(
                new DeleteGotVersionsReportEntry(GUIDFactory.newGUID().toString(), param.getRequestId(), tenantId,
                    now().toString(), entry.getKey().getLeft(), entry.getKey().getRight(),
                    objectGroupToDeleteReportEntries, null));
        }
        if (!deleteGotVersionsReportEntries.isEmpty()) {
            reportService.appendEntries(param.getContainerName(), deleteGotVersionsReportEntries);
        }
    }

    private Set<String> extractUnitsIds(String gotId, List<Pair<String, List<String>>> unitsByGot) {
        return unitsByGot.stream().filter(elmt -> elmt.getLeft().equals(gotId)).map(Pair::getRight)
            .flatMap(List::stream)
            .collect(Collectors.toSet());
    }

    private Map<String, ObjectGroupResponse> getObjectGroups(List<Pair<String, List<String>>> unitsByGot)
        throws ProcessingException {

        try {
            Select select = new Select();
            String[] ids = unitsByGot.stream().map(Pair::getLeft).toArray(String[]::new);
            select.setQuery(in("#id", ids));

            ObjectNode finalSelect = select.getFinalSelect();
            JsonNode response = metaDataClientFactory.getClient().selectObjectGroups(finalSelect);

            JsonNode results = response.get("$results");
            List<ObjectGroupResponse> resultsResponse =
                getFromStringAsTypeReference(results.toString(), new TypeReference<>() {
                });
            return resultsResponse.stream().collect(Collectors
                .toMap(ObjectGroupResponse::getId, objectGroup -> objectGroup));
        } catch (InvalidParseOperationException | InvalidFormatException | MetaDataExecutionException |
            MetaDataDocumentSizeException | MetaDataClientServerException | InvalidCreateOperationException e) {
            e.printStackTrace();
            throw new ProcessingException("A problem occured when retrieving ObjectGroups :  " + e.getMessage());
        }
    }

    private JsonNode generateGotWithDetails(ObjectGroupResponse objectGroup,
        DeleteGotVersionsRequest deleteGotVersionsRequest) {

        List<ObjectGroupToDeleteReportEntry> objectGroupReportEntries = new ArrayList<>();
        try {
            List<QualifiersModel> qualifiers = objectGroup.getQualifiers();
            Optional<QualifiersModel> optionalQualifierToUpdate = qualifiers.stream()
                .filter(elmt -> elmt.getQualifier().equals(deleteGotVersionsRequest.getUsageName()))
                .findFirst();

            if (optionalQualifierToUpdate.isEmpty()) {
                final String errorMsg = String.format("No qualifier of Object group matches with %s usage",
                    deleteGotVersionsRequest.getUsageName());
                objectGroupReportEntries.add(new ObjectGroupToDeleteReportEntry(KO, errorMsg, null));
                return JsonHandler.toJsonNode(objectGroupReportEntries);
            }

            QualifiersModel qualifierToUpdate = optionalQualifierToUpdate.get();
            if (qualifierToUpdate.getVersions() == null || qualifierToUpdate.getVersions().isEmpty()) {
                final String errorMsg =
                    String.format("No versions associated to the qualifier of Object group for the %s usage",
                        deleteGotVersionsRequest.getUsageName());
                objectGroupReportEntries.add(new ObjectGroupToDeleteReportEntry(KO, errorMsg, null));
                return JsonHandler.toJsonNode(objectGroupReportEntries);
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

                // Check if last version
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

            return JsonHandler.toJsonNode(objectGroupReportEntries);
        } catch (InvalidParseOperationException e) {
            e.printStackTrace();
        }
        return null;
    }

    private VersionsModelCustomized customizeVersionModel(VersionsModel versionModelToDelete, String opIngest) {
        VersionsModelCustomized versionsModelCustomized = new VersionsModelCustomized();
        versionsModelCustomized.setId(versionModelToDelete.getId());
        versionsModelCustomized.setOpi(opIngest);
        versionsModelCustomized.setOpc(versionModelToDelete.getOpi());
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

        if (isVersionsDupliated(deleteGotVersionsRequest)) {
            return Pair.of(KO, "Duplicated versions are detected.");
        }
        return Pair.of(OK, null);
    }

    private boolean isVersionsDupliated(DeleteGotVersionsRequest deleteGotVersionsRequest) {
        return deleteGotVersionsRequest.getSpecificVersions().stream().distinct().count() !=
            deleteGotVersionsRequest.getSpecificVersions().size();
    }

    private void createUnitsByGotFile(HandlerIO handler, MetaDataClient metaDataClient,
        DeleteGotVersionsRequest deleteGotVersionsRequest) throws VitamException {
        SelectMultiQuery selectMultiQuery = prepareUnitsWithObjectGroupsQuery(deleteGotVersionsRequest.getDslQuery());
        ScrollSpliterator<JsonNode> scrollRequest = createUnitScrollSplitIterator(metaDataClient, selectMultiQuery);
        Iterator<JsonNode> iterator = new SpliteratorIterator<>(scrollRequest);
        Iterator<Pair<String, String>> gotIdUnitIdIterator = getGotIdUnitIdIterator(iterator);
        Iterator<List<Pair<String, List<String>>>> bulksUnitsByObjectGroup =
            Iterators.partition(new GroupByObjectIterator(gotIdUnitIdIterator), VitamConfiguration.getBatchSize());
        generateUnitsByGotFile(bulksUnitsByObjectGroup, handler);
    }

    private DeleteGotVersionsRequest loadRequest(HandlerIO handler)
        throws ProcessingException, InvalidParseOperationException {
        JsonNode inputRequest = handler.getJsonFromWorkspace("deleteGotVersionsRequest");
        return getFromJsonNode(inputRequest, DeleteGotVersionsRequest.class);
    }

    private SelectMultiQuery prepareUnitsWithObjectGroupsQuery(JsonNode initialQuery) {
        try {
            SelectParserMultiple parser = new SelectParserMultiple();
            parser.parse(initialQuery);
            SelectMultiQuery selectMultiQuery = parser.getRequest();
            ObjectNode projectionNode = getQueryProjectionToApply();
            selectMultiQuery.setProjection(projectionNode);
            selectMultiQuery.addOrderByAscFilter(OBJECT.exactToken());
            List<Query> queryList = new ArrayList<>(parser.getRequest().getQueries());

            if (queryList.isEmpty()) {
                selectMultiQuery.addQueries(and().add(exists(OBJECT.exactToken())).setDepthLimit(0));
                return selectMultiQuery;
            }

            for (int i = 0; i < queryList.size(); i++) {
                final Query query = queryList.get(i);
                Query restrictedQuery = and().add(exists(OBJECT.exactToken()), query);
                parser.getRequest().getQueries().set(i, restrictedQuery);
            }
            return selectMultiQuery;
        } catch (InvalidParseOperationException | InvalidCreateOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private ObjectNode getQueryProjectionToApply() {
        ObjectNode projectionNode = JsonHandler.createObjectNode();
        ObjectNode fields = JsonHandler.createObjectNode();

        fields.put(ID.exactToken(), 1);
        fields.put(OBJECT.exactToken(), 1);
        projectionNode.set(FIELDS.exactToken(), fields);

        return projectionNode;
    }

    private Iterator<Pair<String, String>> getGotIdUnitIdIterator(Iterator<JsonNode> iterator) {
        return IteratorUtils.transformedIterator(
            iterator,
            item -> new ImmutablePair<>(
                item.get(OBJECT.exactToken()).asText(),
                item.get(ID.exactToken()).asText()
            )
        );
    }

    private void generateUnitsByGotFile(Iterator<List<Pair<String, List<String>>>> unitsByObjectGroup,
        HandlerIO handler)
        throws VitamException {
        File objectGroupsIdsFile = handler.getNewLocalFile("units_by_object_groups.jsonl");
        try (final FileOutputStream outputStream = new FileOutputStream(objectGroupsIdsFile);
            JsonLineWriter writer = new JsonLineWriter(outputStream)) {
            while (unitsByObjectGroup.hasNext()) {
                List<Pair<String, List<String>>> unitsByObjectGroupByRange = unitsByObjectGroup.next();
                for (Pair<String, List<String>> unitsByGot : unitsByObjectGroupByRange) {
                    writer.addEntry(
                        new JsonLineModel(unitsByGot.getLeft(), null, JsonHandler.toJsonNode(unitsByGot.getRight())));
                }
            }
        } catch (IOException e) {
            throw new VitamException("Could not save distribution file", e);
        }
        handler.transferFileToWorkspace(UNITS_BY_GOT_FILE, objectGroupsIdsFile, true, false);
    }

    private void generateDistributionFile(Map<Pair<String, Set<String>>, JsonNode> objectGroupRawsGlobal,
        HandlerIO handler)
        throws VitamException {
        File objectGroupsIdsFile = handler.getNewLocalFile("object_groups_to_update.jsonl");
        try (final FileOutputStream outputStream = new FileOutputStream(objectGroupsIdsFile);
            JsonLineWriter writer = new JsonLineWriter(outputStream)) {

            for (Entry<Pair<String, Set<String>>, JsonNode> objectGroupEntry : objectGroupRawsGlobal.entrySet()) {
                writer.addEntry(
                    new JsonLineModel(objectGroupEntry.getKey().getLeft(), null, objectGroupEntry.getValue()));
            }
        } catch (IOException e) {
            throw new VitamException("Could not save distribution file", e);
        }
        handler.transferFileToWorkspace(DISTRIBUTION_FILE_OG, objectGroupsIdsFile, true, false);
    }
}
