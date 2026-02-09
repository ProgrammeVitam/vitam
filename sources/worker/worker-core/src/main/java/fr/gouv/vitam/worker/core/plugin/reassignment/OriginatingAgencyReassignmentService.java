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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Iterators;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.BooleanQuery;
import fr.gouv.vitam.common.database.builder.query.CompareQuery;
import fr.gouv.vitam.common.database.builder.query.InQuery;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.query.action.Action;
import fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.OriginatingAgencyReassignmentRequest;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.model.UpdateUnit;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import org.apache.commons.collections4.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper.push;
import static fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper.set;
import static fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper.unset;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatusWithMessage;

public class OriginatingAgencyReassignmentService {

    private static final String REQUEST_JSON = "request.json";
    private static final String INVALID_REQUEST = "Invalid request";
    private static final String COULD_NOT_LOAD_REQUEST_FROM_WORKSPACE = "Could not load request from workspace";

    public OriginatingAgencyReassignmentService() {}

    public Map<String, List<String>> getUnitsParentsIdById(List<JsonNode> units) {
        if (CollectionUtils.isEmpty(units)) {
            return new HashMap<>();
        }
        return units
            .stream()
            .collect(
                Collectors.toMap(unitNode -> unitNode.get(VitamFieldsHelper.id()).asText(), this::getAllUnitParents)
            );
    }

    public Set<String> getUnitsIdsToInvalidateComputedInheritedRules(List<JsonNode> units) {
        if (CollectionUtils.isEmpty(units)) {
            return new HashSet<>();
        }
        return units
            .stream()
            .filter(
                unitNode ->
                    unitNode.has(VitamFieldsHelper.validComputedInheritedRules()) &&
                    unitNode.get(VitamFieldsHelper.validComputedInheritedRules()).asBoolean()
            )
            .map(unitNode -> unitNode.get(VitamFieldsHelper.id()).asText())
            .collect(Collectors.toSet());
    }

    public List<String> getAllUnitParents(JsonNode unit) {
        return StreamSupport.stream(unit.get(VitamFieldsHelper.allunitups()).spliterator(), false)
            .map(JsonNode::asText)
            .toList();
    }

    public Set<String> getUnitsOriginatingAgenciesByIds(
        HandlerIO handlerIO,
        List<String> parentUnitIds,
        String sourceOriginatingAgency
    )
        throws InvalidParseOperationException, InvalidCreateOperationException, MetaDataClientServerException, MetaDataExecutionException, MetaDataDocumentSizeException {
        Set<String> parentsIdsWithSourceOriginatingAgencies = new HashSet<>();
        SelectMultiQuery select = new SelectMultiQuery();
        try (MetaDataClient metaDataClient = handlerIO.getMetaDataClient()) {
            CompareQuery compareQuery = QueryHelper.eq(VitamFieldsHelper.originatingAgency(), sourceOriginatingAgency);
            InQuery unitsIdQuery = QueryHelper.in(VitamFieldsHelper.id(), parentUnitIds.toArray(String[]::new));
            BooleanQuery booleanQuery = QueryHelper.and().add(unitsIdQuery, compareQuery);
            select.setQuery(booleanQuery);
            select.addUsedProjection(VitamFieldsHelper.id());

            JsonNode results = metaDataClient.selectUnits(select.getFinalSelect()).get("$results");
            for (JsonNode result : results) {
                parentsIdsWithSourceOriginatingAgencies.add(result.get(VitamFieldsHelper.id()).asText());
            }
            return parentsIdsWithSourceOriginatingAgencies;
        }
    }

    public OriginatingAgencyReassignmentRequest loadRequestJsonFromWorkspace(HandlerIO handler)
        throws ProcessingException {
        try {
            return JsonHandler.getFromInputStream(
                handler.getInputStreamFromWorkspace(handler.getWorkFlowExecutionContext(), REQUEST_JSON),
                OriginatingAgencyReassignmentRequest.class
            );
        } catch (
            ContentAddressableStorageServerException
            | ContentAddressableStorageNotFoundException
            | IOException
            | InvalidParseOperationException e
        ) {
            throw new ProcessingException(COULD_NOT_LOAD_REQUEST_FROM_WORKSPACE, e);
        }
    }

    public Set<String> computeUnitsIdsWithAtLeastOneParentHasOriginatingAgenciesEqualTo(
        HandlerIO handlerIO,
        List<JsonNode> units,
        String sourceOriginatingAgency
    )
        throws MetaDataExecutionException, InvalidCreateOperationException, MetaDataClientServerException, InvalidParseOperationException, MetaDataDocumentSizeException {
        Set<String> unitsWithInheritedOriginatingAgency = new HashSet<>();
        Map<String, List<String>> unitsParentIdById = this.getUnitsParentsIdById(units);

        Set<String> allParentsIds = unitsParentIdById
            .values()
            .stream()
            .flatMap(List::stream)
            .collect(Collectors.toSet());

        Iterator<List<String>> parentIdsIterator = Iterators.partition(
            allParentsIds.iterator(),
            VitamConfiguration.getBatchSize()
        );
        Set<String> parentsIdsWithSourceOriginatingAgencies = new HashSet<>();
        while (parentIdsIterator.hasNext()) {
            List<String> partitionParentIds = parentIdsIterator.next();
            parentsIdsWithSourceOriginatingAgencies.addAll(
                this.getUnitsOriginatingAgenciesByIds(handlerIO, partitionParentIds, sourceOriginatingAgency)
            );
        }

        for (Map.Entry<String, List<String>> entryParent : unitsParentIdById.entrySet()) {
            String unitId = entryParent.getKey();
            List<String> parentIds = entryParent.getValue();

            if (CollectionUtils.isEmpty(parentIds)) {
                continue;
            }

            boolean hasParentInheritingSourceOriginatingAgency = parentIds
                .stream()
                .anyMatch(parentsIdsWithSourceOriginatingAgencies::contains);
            if (hasParentInheritingSourceOriginatingAgency) {
                unitsWithInheritedOriginatingAgency.add(unitId);
            }
        }
        return unitsWithInheritedOriginatingAgency;
    }

    public List<JsonNode> buildUnitsOriginatingAgencyReassignmentUpdateQueries(
        HandlerIO handlerIO,
        List<JsonNode> units,
        String sourceOriginatingAgency,
        String targetOriginatingAgency,
        String processId
    ) throws ProcessingException {
        try {
            return buildUnitsOriginatingAgencyAndOriginatingAgenciesUpdateQueries(
                handlerIO,
                units,
                sourceOriginatingAgency,
                targetOriginatingAgency,
                true,
                processId
            );
        } catch (
            MetaDataExecutionException
            | InvalidCreateOperationException
            | MetaDataClientServerException
            | InvalidParseOperationException
            | MetaDataDocumentSizeException e
        ) {
            throw new ProcessingException("Error on generating originating agency queries ", e);
        }
    }

    public List<JsonNode> buildUnitsOriginatingAgenciesComputingUpdateQueries(
        HandlerIO handlerIO,
        List<JsonNode> units,
        String sourceOriginatingAgency,
        String targetOriginatingAgency,
        String processId
    ) throws ProcessingException {
        try {
            return buildUnitsOriginatingAgencyAndOriginatingAgenciesUpdateQueries(
                handlerIO,
                units,
                sourceOriginatingAgency,
                targetOriginatingAgency,
                false,
                processId
            );
        } catch (
            MetaDataExecutionException
            | InvalidCreateOperationException
            | MetaDataClientServerException
            | InvalidParseOperationException
            | MetaDataDocumentSizeException e
        ) {
            throw new ProcessingException("Error on generating originating agencies queries ", e);
        }
    }

    private List<JsonNode> buildUnitsOriginatingAgencyAndOriginatingAgenciesUpdateQueries(
        HandlerIO handlerIO,
        List<JsonNode> units,
        String sourceOriginatingAgency,
        String targetOriginatingAgency,
        boolean updateMainOriginatingAgency,
        String processId
    )
        throws InvalidCreateOperationException, InvalidParseOperationException, MetaDataClientServerException, MetaDataExecutionException, MetaDataDocumentSizeException {
        Set<String> parentsIdWithUnchangedOriginatingAgency =
            this.computeUnitsIdsWithAtLeastOneParentHasOriginatingAgenciesEqualTo(
                    handlerIO,
                    units,
                    sourceOriginatingAgency
                );

        Set<String> unitsIdsToInvalidateComputedInheritedRules =
            this.getUnitsIdsToInvalidateComputedInheritedRules(units);

        List<JsonNode> updateMultiQueryList = new ArrayList<>();
        for (JsonNode unit : units) {
            updateMultiQueryList.add(
                buildUpdateUnitsMultiQuery(
                    unit,
                    targetOriginatingAgency,
                    sourceOriginatingAgency,
                    unitsIdsToInvalidateComputedInheritedRules,
                    parentsIdWithUnchangedOriginatingAgency,
                    updateMainOriginatingAgency,
                    processId
                ).getFinalUpdate()
            );
        }
        return updateMultiQueryList;
    }

    private static UpdateMultiQuery buildUpdateUnitsMultiQuery(
        JsonNode unit,
        String targetOriginatingAgency,
        String sourceOriginatingAgency,
        Set<String> unitsIdsToInvalidateComputedInheritedRules,
        Set<String> parentsIdWithUnchangedOriginatingAgency,
        boolean updateMainOriginatingAgency,
        String processId
    ) throws InvalidCreateOperationException, InvalidParseOperationException {
        List<Action> actions = new ArrayList<>();
        String unitId = unit.get(VitamFieldsHelper.id()).asText();

        boolean shouldRemoveSourceAgency = !parentsIdWithUnchangedOriginatingAgency.contains(unitId);
        boolean shouldInvalidateComputedInheritedRules = unitsIdsToInvalidateComputedInheritedRules.contains(unitId);

        if (shouldInvalidateComputedInheritedRules) {
            actions.add(set(VitamFieldsHelper.validComputedInheritedRules(), false));
            actions.add(unset(VitamFieldsHelper.computedInheritedRules()));
        }

        //update field #graph_last_persisted_date
        actions.add(set(VitamFieldsHelper.graph_last_persisted_date(), LocalDateUtil.nowFormatted()));

        String currentOriginatingAgency = unit.get(VitamFieldsHelper.originatingAgency()).asText();
        actions.add(UpdateActionHelper.add(VitamFieldsHelper.originatingAgencies(), targetOriginatingAgency));

        if (updateMainOriginatingAgency) {
            //set SP and SPS (for main units)
            actions.add(UpdateActionHelper.set(VitamFieldsHelper.originatingAgency(), targetOriginatingAgency));
            //add current operation to #OPS field
            actions.add(push(VitamFieldsHelper.operations(), processId));
            if (shouldRemoveSourceAgency) {
                actions.add(UpdateActionHelper.pull(VitamFieldsHelper.originatingAgencies(), sourceOriginatingAgency));
            }
        } else {
            //set Only SPS (for children units)
            if (!Objects.equals(currentOriginatingAgency, sourceOriginatingAgency) && shouldRemoveSourceAgency) {
                actions.add(UpdateActionHelper.pull(VitamFieldsHelper.originatingAgencies(), sourceOriginatingAgency));
            }
        }

        UpdateMultiQuery updateMultiQuery = new UpdateMultiQuery();
        updateMultiQuery.addRoots(unitId);

        for (Action action : actions) {
            updateMultiQuery.addActions(action);
        }
        return updateMultiQuery;
    }

    public List<ItemStatus> buildItemsStatusResponse(RequestResponse<JsonNode> requestResponse, String pluginId)
        throws InvalidParseOperationException {
        List<ItemStatus> itemStatuses = new ArrayList<>();
        List<JsonNode> results = ((RequestResponseOK<JsonNode>) requestResponse).getResults();

        for (JsonNode unitItemResult : results) {
            RequestResponseOK<UpdateUnit> responseOK = RequestResponseOK.getFromJsonNode(
                unitItemResult,
                UpdateUnit.class
            );

            UpdateUnit unitAsNode = responseOK.getFirstResult();

            itemStatuses.add(buildItemStatusWithMessage(pluginId, unitAsNode.getStatus(), unitAsNode.getDiff()));
        }
        return itemStatuses;
    }

    public List<JsonNode> findUnitsByIds(HandlerIO handlerIO, List<String> unitsIds)
        throws InvalidParseOperationException, InvalidCreateOperationException, MetaDataExecutionException, MetaDataClientServerException, MetaDataDocumentSizeException {
        List<JsonNode> units = new ArrayList<>();
        try (MetaDataClient metaDataClient = handlerIO.getMetaDataClient()) {
            InQuery unitsIdQuery = QueryHelper.in(VitamFieldsHelper.id(), unitsIds.toArray(String[]::new));

            SelectMultiQuery select = new SelectMultiQuery();
            select.setQuery(unitsIdQuery);

            select.addUsedProjection(
                VitamFieldsHelper.id(),
                VitamFieldsHelper.originatingAgency(),
                VitamFieldsHelper.originatingAgencies(),
                VitamFieldsHelper.unitups(),
                VitamFieldsHelper.allunitups(),
                VitamFieldsHelper.validComputedInheritedRules()
            );

            JsonNode results = metaDataClient.selectUnits(select.getFinalSelect()).get("$results");
            for (JsonNode result : results) {
                units.add(result);
            }
            return units;
        }
    }
}
