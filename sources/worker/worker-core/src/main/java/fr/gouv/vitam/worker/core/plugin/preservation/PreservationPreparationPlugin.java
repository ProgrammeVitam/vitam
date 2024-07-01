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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterators;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.database.utils.ScrollSpliterator;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.iterables.SpliteratorIterator;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.PreservationRequest;
import fr.gouv.vitam.common.model.PreservationVersion;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.preservation.GriffinByFormat;
import fr.gouv.vitam.common.model.administration.preservation.GriffinModel;
import fr.gouv.vitam.common.model.administration.preservation.PreservationScenarioModel;
import fr.gouv.vitam.common.model.objectgroup.FormatIdentificationModel;
import fr.gouv.vitam.common.model.objectgroup.ObjectGroupResponse;
import fr.gouv.vitam.common.model.objectgroup.VersionsModel;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialNotFoundException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.distribution.JsonLineGenericIterator;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import fr.gouv.vitam.worker.core.distribution.JsonLineWriter;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.plugin.preservation.model.PreservationDistributionLine;
import fr.gouv.vitam.worker.core.utils.GroupByObjectIterator;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.and;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.exists;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.in;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTION.FIELDS;
import static fr.gouv.vitam.common.database.parser.query.ParserTokens.PROJECTIONARGS.ID;
import static fr.gouv.vitam.common.database.parser.query.ParserTokens.PROJECTIONARGS.OBJECT;
import static fr.gouv.vitam.common.json.JsonHandler.createObjectNode;
import static fr.gouv.vitam.common.json.JsonHandler.getFromJsonNode;
import static fr.gouv.vitam.common.json.JsonHandler.getFromStringAsTypeReference;
import static fr.gouv.vitam.common.json.JsonHandler.writeToInpustream;
import static fr.gouv.vitam.common.model.PreservationVersion.FIRST;
import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.common.model.administration.preservation.GriffinModel.TAG_IDENTIFIER;
import static fr.gouv.vitam.worker.core.plugin.ScrollSpliteratorHelper.createUnitScrollSplitIterator;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;
import static java.util.stream.Collectors.toMap;

public class PreservationPreparationPlugin extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(PreservationPreparationPlugin.class);

    private static final String PRESERVATION_PREPARATION = "PRESERVATION_PREPARATION";
    private static final TypeReference<JsonLineModel> JSON_LINE_MODEL_TYPE_REFERENCE = new TypeReference<>() {};

    private final AdminManagementClientFactory adminManagementClientFactory;

    private final WorkspaceClientFactory workspaceClientFactory;

    private final MetaDataClientFactory metaDataClientFactory;

    public PreservationPreparationPlugin() {
        this(
            AdminManagementClientFactory.getInstance(),
            MetaDataClientFactory.getInstance(),
            WorkspaceClientFactory.getInstance()
        );
    }

    @VisibleForTesting
    PreservationPreparationPlugin(
        AdminManagementClientFactory adminManagementClientFactory,
        MetaDataClientFactory metaDataClientFactory,
        WorkspaceClientFactory workspaceClientFactory
    ) {
        this.adminManagementClientFactory = adminManagementClientFactory;
        this.metaDataClientFactory = metaDataClientFactory;
        this.workspaceClientFactory = workspaceClientFactory;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) {
        try (MetaDataClient metaDataClient = metaDataClientFactory.getClient()) {
            PreservationRequest preservationRequest = loadPreservationRequest(handler);
            computePreparation(handler, metaDataClient, preservationRequest, param.getRequestId());
            return buildItemStatus(
                PRESERVATION_PREPARATION,
                StatusCode.OK,
                createObjectNode().put("query", preservationRequest.getDslQuery().toString())
            );
        } catch (Exception e) {
            LOGGER.error(String.format("Preservation action failed with status [%s]", KO), e);
            ObjectNode error = createObjectNode().put("error", e.getMessage());
            return buildItemStatus(PRESERVATION_PREPARATION, KO, error);
        }
    }

    private Iterator<Pair<String, String>> getGotIdUnitIdIterator(Iterator<JsonNode> iterator) {
        return IteratorUtils.transformedIterator(
            iterator,
            item -> new ImmutablePair<>(item.get(OBJECT.exactToken()).asText(), item.get(ID.exactToken()).asText())
        );
    }

    private void computePreparation(
        HandlerIO handler,
        MetaDataClient metaDataClient,
        PreservationRequest preservationRequest,
        String requestId
    ) throws VitamException {
        PreservationScenarioModel scenarioModel;
        Map<String, GriffinModel> griffinModelListForScenario;

        try (
            AdminManagementClient adminManagementClient = adminManagementClientFactory.getClient();
            WorkspaceClient workspaceClient = workspaceClientFactory.getClient()
        ) {
            scenarioModel = getScenarioModel(adminManagementClient, preservationRequest.getScenarioIdentifier());
            griffinModelListForScenario = getListOfGriffinGivenScenario(adminManagementClient, scenarioModel);
            Set<GriffinModel> griffins = new HashSet<>(griffinModelListForScenario.values());
            workspaceClient.putObject(requestId, "preservationScenarioModel", writeToInpustream(scenarioModel));
            workspaceClient.putObject(requestId, "griffinModel", writeToInpustream(griffins));
        } catch (Exception e) {
            throw new ProcessingException(String.format("Preconditions Failed :  %s", e.getMessage()), e);
        }

        SelectMultiQuery selectMultiQuery = prepareUnitsWithObjectGroupsQuery(preservationRequest.getDslQuery());

        ScrollSpliterator<JsonNode> scrollRequest = createUnitScrollSplitIterator(metaDataClient, selectMultiQuery);
        Iterator<JsonNode> iterator = new SpliteratorIterator<>(scrollRequest);

        Iterator<Pair<String, String>> gotIdUnitIdIterator = getGotIdUnitIdIterator(iterator);
        Iterator<List<Pair<String, List<String>>>> bulksUnitsByObjectGroup = Iterators.partition(
            new GroupByObjectIterator(gotIdUnitIdIterator),
            VitamConfiguration.getBatchSize()
        );

        HashMap<String, File> distributionFileByFormat = new HashMap<>();

        while (bulksUnitsByObjectGroup.hasNext()) {
            List<Pair<String, List<String>>> bulkUnitsByObjectGroup = bulksUnitsByObjectGroup.next();
            processBulk(
                bulkUnitsByObjectGroup,
                preservationRequest,
                handler,
                distributionFileByFormat,
                scenarioModel,
                griffinModelListForScenario
            );
        }

        mergeDistributionFiles(handler, distributionFileByFormat);
    }

    private PreservationRequest loadPreservationRequest(HandlerIO handler)
        throws ProcessingException, InvalidParseOperationException {
        JsonNode inputRequest = handler.getJsonFromWorkspace("preservationRequest");
        return getFromJsonNode(inputRequest, PreservationRequest.class);
    }

    private void processBulk(
        List<Pair<String, List<String>>> bulkUnitsByObjectGroup,
        PreservationRequest preservationRequest,
        HandlerIO handler,
        HashMap<String, File> distributionFileByFormat,
        PreservationScenarioModel scenarioModel,
        Map<String, GriffinModel> griffinModelListForScenario
    ) {
        Map<String, Set<String>> unitsByObjectGroup = new HashMap<>();
        for (Pair<String, List<String>> item : bulkUnitsByObjectGroup) {
            unitsByObjectGroup.put(item.getKey(), new HashSet<>(item.getValue()));
        }

        List<ObjectGroupResponse> objectModelsForUnitResults = getObjectModelsForUnitResults(
            unitsByObjectGroup.keySet()
        );

        MultiValuedMap<String, PreservationDistributionLine> preservationDistributionsByFormatId =
            new ArrayListValuedHashMap<>();

        for (ObjectGroupResponse objectGroup : objectModelsForUnitResults) {
            Set<String> unitIds = unitsByObjectGroup.get(objectGroup.getId());

            String unitId = unitIds.iterator().next();

            PreservationDistributionLine preservationDistributionLine = createPreservationDistributionLine(
                preservationRequest,
                unitId,
                objectGroup,
                scenarioModel,
                griffinModelListForScenario,
                unitIds
            );

            if (preservationDistributionLine != null) {
                preservationDistributionsByFormatId.put(
                    preservationDistributionLine.getFormatId(),
                    preservationDistributionLine
                );
            }
        }

        writeDistributionFilesByFormatId(handler, distributionFileByFormat, preservationDistributionsByFormatId);
    }

    private void writeDistributionFilesByFormatId(
        HandlerIO handler,
        HashMap<String, File> distributionFileByFormat,
        MultiValuedMap<String, PreservationDistributionLine> preservationDistributionsByFormatId
    ) {
        for (String formatId : preservationDistributionsByFormatId.keySet()) {
            boolean isEmpty = !distributionFileByFormat.containsKey(formatId);

            File distributionFile = distributionFileByFormat.computeIfAbsent(
                formatId,
                unused -> handler.getNewLocalFile(GUIDFactory.newGUID().toString())
            );

            try (
                final FileOutputStream outputStream = new FileOutputStream(distributionFile, true);
                JsonLineWriter writer = new JsonLineWriter(outputStream, isEmpty)
            ) {
                for (PreservationDistributionLine preservationDistributionLine : preservationDistributionsByFormatId.get(
                    formatId
                )) {
                    writer.addEntry(
                        new JsonLineModel(
                            preservationDistributionLine.getId(),
                            null,
                            JsonHandler.toJsonNode(preservationDistributionLine)
                        )
                    );
                }
            } catch (IOException | InvalidParseOperationException e) {
                throw new VitamRuntimeException("Could not persist distribution file", e);
            }
        }
    }

    private void mergeDistributionFiles(HandlerIO handler, HashMap<String, File> distributionFileByFormat)
        throws VitamException {
        File objectGroupsBigFileToPreserve = handler.getNewLocalFile("object_groups_to_preserve.jsonl");
        try (
            final FileOutputStream outputStream = new FileOutputStream(objectGroupsBigFileToPreserve);
            JsonLineWriter writer = new JsonLineWriter(outputStream)
        ) {
            int cpt = 0;
            for (File distributionFileForFormatId : distributionFileByFormat.values()) {
                cpt++;

                try (
                    final InputStream inputStream = new FileInputStream(distributionFileForFormatId);
                    JsonLineGenericIterator<JsonLineModel> jsonLineIterator = new JsonLineGenericIterator<>(
                        inputStream,
                        JSON_LINE_MODEL_TYPE_REFERENCE
                    )
                ) {
                    while (jsonLineIterator.hasNext()) {
                        JsonLineModel model = jsonLineIterator.next();
                        model.setDistribGroup(cpt);
                        writer.addEntry(model);
                    }
                }

                FileUtils.deleteQuietly(distributionFileForFormatId);
            }
        } catch (IOException e) {
            throw new VitamException("Could not save distribution file", e);
        }

        handler.transferFileToWorkspace("distributionFileOG.jsonl", objectGroupsBigFileToPreserve, false, false);
    }

    private PreservationDistributionLine createPreservationDistributionLine(
        PreservationRequest preservationRequest,
        String unitId,
        ObjectGroupResponse objectGroup,
        PreservationScenarioModel scenarioModel,
        Map<String, GriffinModel> griffinModelListForScenario,
        Set<String> unitsForThisOG
    ) {
        String targetQualifier = preservationRequest.getTargetUsage();
        String sourceQualifier = preservationRequest.getSourceUsage();

        PreservationVersion version = preservationRequest.getVersion();

        Optional<VersionsModel> versionsModelOptional = (version == FIRST)
            ? objectGroup.getFirstVersionsModel(sourceQualifier)
            : objectGroup.getLastVersionsModel(sourceQualifier);

        if (versionsModelOptional.isEmpty()) {
            return null;
        }

        VersionsModel versionsModel = versionsModelOptional.get();
        FormatIdentificationModel formatIdentificationModel = versionsModel.getFormatIdentification();

        String formatId = formatIdentificationModel.getFormatId();
        Optional<GriffinByFormat> griffinByFormat = scenarioModel.getGriffinByFormat(formatId);

        if (griffinByFormat.isEmpty()) {
            return null;
        }

        GriffinByFormat griffinByFormatModel = griffinByFormat.get();
        String griffinId = griffinByFormatModel.getGriffinIdentifier();
        GriffinModel griffinModel = griffinModelListForScenario.get(griffinId);

        return createLine(
            objectGroup.getId(),
            unitId,
            versionsModel,
            formatIdentificationModel.getFormatId(),
            griffinByFormatModel,
            griffinModel,
            targetQualifier,
            sourceQualifier,
            versionsModel.getStorage().getStrategyId(),
            scenarioModel.getIdentifier(),
            unitsForThisOG
        );
    }

    private PreservationDistributionLine createLine(
        String objectGroupId,
        String unitId,
        VersionsModel version,
        String format,
        GriffinByFormat griffinByFormatModel,
        GriffinModel griffinModel,
        String targetQualifier,
        String sourceQualifier,
        String sourceStrategy,
        String scenarioId,
        Set<String> unitsForThisOG
    ) {
        PreservationDistributionLine line = new PreservationDistributionLine();
        line.setId(objectGroupId);
        line.setFormatId(format);
        line.setFilename(version.getFileInfoModel().getFilename());
        line.setActionPreservationList(griffinByFormatModel.getActionDetail());
        line.setUnitId(unitId);
        line.setGriffinId(griffinModel.getExecutableName());
        line.setObjectId(version.getId());
        line.setDebug(griffinByFormatModel.isDebug());
        line.setTimeout(griffinByFormatModel.getTimeOut());
        line.setTargetUse(targetQualifier);
        line.setSourceUse(sourceQualifier);
        line.setSourceStrategy(sourceStrategy);
        line.setScenarioId(scenarioId);
        line.setGriffinIdentifier(griffinModel.getIdentifier());
        line.setUnitsForExtractionAU(unitsForThisOG);
        return line;
    }

    private List<ObjectGroupResponse> getObjectModelsForUnitResults(Collection<String> objectGroupIds) {
        try {
            Select select = new Select();
            String[] ids = objectGroupIds.toArray(new String[0]);
            select.setQuery(in("#id", ids));

            ObjectNode finalSelect = select.getFinalSelect();
            JsonNode response = metaDataClientFactory.getClient().selectObjectGroups(finalSelect);

            JsonNode results = response.get("$results");
            return getFromStringAsTypeReference(results.toString(), new TypeReference<List<ObjectGroupResponse>>() {});
        } catch (VitamException | InvalidFormatException | InvalidCreateOperationException e) {
            throw new IllegalStateException(e);
        }
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

    private Map<String, GriffinModel> getListOfGriffinGivenScenario(
        AdminManagementClient adminManagementClient,
        PreservationScenarioModel scenarioModel
    )
        throws InvalidCreateOperationException, AdminManagementClientServerException, InvalidParseOperationException, ReferentialNotFoundException, ProcessingException {
        Set<String> allGriffinIdentifiers = scenarioModel.getAllGriffinIdentifiers();

        Select select = new Select();
        select.setQuery(in(TAG_IDENTIFIER, allGriffinIdentifiers.toArray(new String[0])));
        ObjectNode finalSelect = select.getFinalSelect();

        RequestResponse<GriffinModel> griffinResponse = adminManagementClient.findGriffin(finalSelect);

        List<GriffinModel> griffinModels = ((RequestResponseOK<GriffinModel>) griffinResponse).getResults();

        if (griffinModels == null || griffinModels.isEmpty()) {
            throw new ProcessingException("Griffin not found");
        }

        return griffinModels.stream().collect(toMap(GriffinModel::getIdentifier, gr -> gr));
    }

    private PreservationScenarioModel getScenarioModel(AdminManagementClient adminManagementClient, String identifier)
        throws ProcessingException, AdminManagementClientServerException, ReferentialNotFoundException, InvalidParseOperationException {
        RequestResponse<PreservationScenarioModel> response = adminManagementClient.findPreservationByID(identifier);

        PreservationScenarioModel model = ((RequestResponseOK<PreservationScenarioModel>) response).getFirstResult();

        if (model == null) {
            throw new ProcessingException("Griffin " + identifier + " not found");
        }
        return model;
    }
}
