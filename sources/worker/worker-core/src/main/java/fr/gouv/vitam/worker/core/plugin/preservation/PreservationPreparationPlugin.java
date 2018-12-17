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
package fr.gouv.vitam.worker.core.plugin.preservation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
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
import fr.gouv.vitam.common.iterables.BulkIterator;
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
import fr.gouv.vitam.common.model.administration.GriffinByFormat;
import fr.gouv.vitam.common.model.administration.GriffinModel;
import fr.gouv.vitam.common.model.administration.PreservationScenarioModel;
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
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import fr.gouv.vitam.worker.core.distribution.JsonLineWriter;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.plugin.preservation.model.PreservationDistributionLine;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
import static fr.gouv.vitam.common.json.JsonHandler.getFromStringAsTypeRefence;
import static fr.gouv.vitam.common.json.JsonHandler.toJsonNode;
import static fr.gouv.vitam.common.model.PreservationVersion.FIRST;
import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.common.model.administration.GriffinModel.TAG_IDENTIFIER;
import static fr.gouv.vitam.worker.core.plugin.ScrollSpliteratorHelper.createUnitScrollSplitIterator;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toMap;

public class PreservationPreparationPlugin extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(PreservationPreparationPlugin.class);

    private static final String PRESERVATION_PREPARATION = "PRESERVATION_PREPARATION";

    private AdminManagementClientFactory adminManagementClientFactory;

    private MetaDataClientFactory metaDataClientFactory;

    private PreservationRequest preservationRequest;

    private PreservationScenarioModel scenarioModel;

    private Map<String, GriffinModel> griffinModelListForScenario;

    private File objectGroupsBigFileToPreserve;

    private UniqueIdHandler uniqueIdHandler = new UniqueIdHandler();

    public PreservationPreparationPlugin() {
        this(AdminManagementClientFactory.getInstance(), MetaDataClientFactory.getInstance());
    }

    @VisibleForTesting PreservationPreparationPlugin(AdminManagementClientFactory adminManagementClientFactory,
        MetaDataClientFactory metaDataClientFactory) {
        this.adminManagementClientFactory = adminManagementClientFactory;
        this.metaDataClientFactory = metaDataClientFactory;

    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler)
        throws ContentAddressableStorageServerException {

        try (MetaDataClient metaDataClient = metaDataClientFactory.getClient()) {

            computePreparation(handler, metaDataClient);

            return buildItemStatus(PRESERVATION_PREPARATION, StatusCode.OK, createObjectNode());

        } catch (Exception e) {
            LOGGER.error(String.format("Preservation action failed with status [%s]", KO), e);
            ObjectNode error = createObjectNode().put("error", e.getMessage());
            return buildItemStatus(PRESERVATION_PREPARATION, KO, error);
        }
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

    private void computePreparation(HandlerIO handler, MetaDataClient metaDataClient)
        throws VitamException {

        prepareElements(handler);

        JsonNode initialQuery = preservationRequest.getDslQuery();

        SelectMultiQuery selectMultiQuery = prepareUnitsWithObjectGroupsQuery(initialQuery);

        ScrollSpliterator<JsonNode> scrollRequest = createUnitScrollSplitIterator(metaDataClient, selectMultiQuery);

        Iterator<JsonNode> iterator = new SpliteratorIterator<>(scrollRequest);

        Iterator<Pair<String, String>> gotIdUnitIdIterator = getGotIdUnitIdIterator(iterator);

        Iterator<Pair<String, List<String>>> unitsByObjectGroupIterator =
            new GroupByObjectIterator(gotIdUnitIdIterator);

        Iterator<List<Pair<String, List<String>>>> bulkIterator =
            new BulkIterator<>(unitsByObjectGroupIterator, VitamConfiguration.getBatchSize());

        try (final FileOutputStream outputStream = new FileOutputStream(objectGroupsBigFileToPreserve);
            JsonLineWriter writer = new JsonLineWriter(outputStream)) {

            while (bulkIterator.hasNext()) {
                List<Pair<String, List<String>>> bulkToProcess = bulkIterator.next();

                process(bulkToProcess, preservationRequest, writer);
            }

        } catch (IOException e) {
            throw new VitamException("Could not save distribution file", e);
        }

        handler.transferFileToWorkspace("distributionFile.jsonl", objectGroupsBigFileToPreserve, false, false);
    }


    private void process(List<Pair<String, List<String>>> bulkToProcess, PreservationRequest preservationRequest,
        JsonLineWriter writer) {

        Map<String,List<String>> tempMap = new HashMap<>();
        bulkToProcess.forEach(item -> tempMap.put(item.getKey(), item.getValue()));

        List<ObjectGroupResponse> objectModelsForUnitResults = getObjectModelsForUnitResults(tempMap.keySet());

        List<JsonLineModel> jsonLineModelListToDistribute = new ArrayList<>();

        for (ObjectGroupResponse objectGroup : objectModelsForUnitResults) {

            Collection<String> unitIds = tempMap.get(objectGroup.getId());

            String unitId = unitIds.iterator().next();

            List<JsonLineModel> modelList = collectLineModelPerQualifier(preservationRequest, unitId, objectGroup);

            jsonLineModelListToDistribute.addAll(modelList);
        }

        //Sort is necessary for optimizing distribution
        jsonLineModelListToDistribute.sort(comparing(JsonLineModel::getDistribGroup));


        writeLisToFile(jsonLineModelListToDistribute, writer);

    }

    private void writeLisToFile(List<JsonLineModel> jsonLineModelListToDistribute, JsonLineWriter writer) {
        try {

            for (JsonLineModel model : jsonLineModelListToDistribute) {
                writer.addEntry(model);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Something bas happened", e);
        }
    }

    private List<JsonLineModel> collectLineModelPerQualifier(PreservationRequest preservationRequest, String unitId,
        ObjectGroupResponse objectGroup) {

        List<JsonLineModel> jsonLineModelList = new ArrayList<>();

        for (String qualifier : preservationRequest.getUsages()) {

            PreservationVersion version = preservationRequest.getVersion();

            Optional<VersionsModel> versionsModelOptional = (version == FIRST) ?
                objectGroup.getFirstVersionsModel(qualifier) : objectGroup.getLastVersionsModel(qualifier);

            if (!versionsModelOptional.isPresent()) {
                throw new VitamRuntimeException("Format not found for object group  '" + objectGroup.getId() + "'");
            }

            VersionsModel versionsModel = versionsModelOptional.get();
            FormatIdentificationModel formatIdentificationModel = versionsModel.getFormatIdentification();

            String formatId = formatIdentificationModel.getFormatId();
            Optional<GriffinByFormat> griffinByFormat = scenarioModel.getGriffinByFormat(formatId);

            if (!griffinByFormat.isPresent()) {
                throw new VitamRuntimeException("Format not found for object group  '" + objectGroup.getId() + "'");
            }

            GriffinByFormat griffinByFormatModel = griffinByFormat.get();

            String griffinId = griffinByFormatModel.getGriffinIdentifier();

            int distributionGroup = uniqueIdHandler.getUniqCodeForFormat(formatId);

            JsonLineModel lineModel = new JsonLineModel();

            lineModel.setDistribGroup(distributionGroup);
            lineModel.setId(objectGroup.getId());

            GriffinModel griffinModel = griffinModelListForScenario.get(griffinId);

            PreservationDistributionLine preservationDistributionLine = getPreservationDistributionLine(objectGroup.getId(), unitId, versionsModel, formatIdentificationModel.getFormatId(), griffinByFormatModel, griffinModel, qualifier);
            try {
                lineModel.setParams(toJsonNode(preservationDistributionLine));
            } catch (InvalidParseOperationException e) {
                throw new VitamRuntimeException(e);
            }

            jsonLineModelList.add(lineModel);
        }

        return jsonLineModelList;
    }

    private PreservationDistributionLine getPreservationDistributionLine(String objectGroupId, String unitId, VersionsModel version, String format, GriffinByFormat griffinByFormatModel, GriffinModel griffinModel, String qualifier) {

        PreservationDistributionLine preservationDistributionLine = new PreservationDistributionLine();

        preservationDistributionLine.setId(objectGroupId);
        preservationDistributionLine.setFormatId(format);
        preservationDistributionLine.setFilename(version.getFileInfoModel().getFilename());
        preservationDistributionLine.setActionPreservationList(griffinByFormatModel.getActionDetail());
        preservationDistributionLine.setUnitId(unitId);
        preservationDistributionLine.setGriffinId(griffinModel.getExecutableName());
        preservationDistributionLine.setObjectId(version.getId());
        preservationDistributionLine.setDebug(griffinByFormatModel.isDebug());
        preservationDistributionLine.setTimeout(griffinByFormatModel.getMaxSize());
        preservationDistributionLine.setUsage(qualifier);

        return preservationDistributionLine;
    }

    private List<ObjectGroupResponse> getObjectModelsForUnitResults(Collection<String> objectGroupIds) {
        try {

            Select select = new Select();
            String[] ids = objectGroupIds.toArray(new String[0]);
            select.setQuery(in("#id", ids));

            ObjectNode finalSelect = select.getFinalSelect();
            JsonNode response = metaDataClientFactory.getClient().selectObjectGroups(finalSelect);

            JsonNode results = response.get("$results");
            return getFromStringAsTypeRefence(results.toString(), new TypeReference<List<ObjectGroupResponse>>() {
            });

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

    private void prepareElements(HandlerIO handler) throws ProcessingException {

        try (AdminManagementClient adminManagementClient = adminManagementClientFactory.getClient()) {
            JsonNode inputRequest = handler.getJsonFromWorkspace("preservationRequest");

            preservationRequest = getFromJsonNode(inputRequest, PreservationRequest.class);
            scenarioModel = getScenarioModel(adminManagementClient, preservationRequest.getScenarioIdentifier());
            griffinModelListForScenario = getListOfGriffinGivenScenario(adminManagementClient, scenarioModel);
            objectGroupsBigFileToPreserve = handler.getNewLocalFile("object_groups_to_preserve.jsonl");

        } catch (Exception e) {
            throw new ProcessingException("Preconditions Failed", e);
        }
    }

    private Map<String, GriffinModel> getListOfGriffinGivenScenario(AdminManagementClient adminManagementClient,
        PreservationScenarioModel scenarioModel)
        throws InvalidCreateOperationException, AdminManagementClientServerException, InvalidParseOperationException,
        ReferentialNotFoundException, ProcessingException {

        Set<String> allGriffinIdentifiers = scenarioModel.getAllGriffinIdentifiers();

        Select select = new Select();
        select.setQuery(in(TAG_IDENTIFIER, allGriffinIdentifiers.toArray(new String[0])));
        ObjectNode finalSelect = select.getFinalSelect();

        RequestResponse griffinResponse = adminManagementClient.findGriffin(finalSelect);

        @SuppressWarnings("unchecked")
        List<GriffinModel> griffinModels = ((RequestResponseOK<GriffinModel>) griffinResponse).getResults();

        if (griffinModels == null || griffinModels.isEmpty()) {
            throw new ProcessingException("Griffin not found");
        }

        return griffinModels.stream().collect(toMap(GriffinModel::getIdentifier, gr -> gr));
    }

    private PreservationScenarioModel getScenarioModel(AdminManagementClient adminManagementClient, String identifier)
        throws ProcessingException, AdminManagementClientServerException, ReferentialNotFoundException,
        InvalidParseOperationException {

        RequestResponse<PreservationScenarioModel> response = adminManagementClient.findPreservationByID(identifier);

        PreservationScenarioModel model = ((RequestResponseOK<PreservationScenarioModel>) response).getFirstResult();

        if (model == null) {
            throw new ProcessingException("Griffin " + identifier + " not found");
        }
        return model;
    }

    static class UniqueIdHandler {

        private final Map<String, Integer> uniqueIdMap = new HashMap<>();

        int getUniqCodeForFormat(String id) {
            if (uniqueIdMap.containsKey(id)) {
                return uniqueIdMap.get(id);
            }
            int newId = uniqueIdMap.size() + 1;
            uniqueIdMap.put(id, newId);
            return newId;
        }
    }
}
