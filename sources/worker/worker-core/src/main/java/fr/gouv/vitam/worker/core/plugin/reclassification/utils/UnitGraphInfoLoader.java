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
package fr.gouv.vitam.worker.core.plugin.reclassification.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Iterators;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.utils.AccessContractRestrictionHelper;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamDBException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.VitamConstants;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.rules.InheritedRuleCategoryResponseModel;
import fr.gouv.vitam.common.model.rules.UnitInheritedRulesResponseModel;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.core.rules.MetadataRuleService;
import fr.gouv.vitam.worker.core.plugin.reclassification.model.UnitGraphInfo;
import org.apache.commons.collections4.ListUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Helper class for db access via DSL for reclassification
 */
public class UnitGraphInfoLoader {

    /**
     * By default ES does not allow more than 1024 clauses in selects
     * item IN ( "val1",... "val9") accounts for 9 clauses
     */
    static final int MAX_ELASTIC_SEARCH_IN_REQUEST_SIZE = 1000;

    private static final String RESULTS = "$results";

    /**
     * Find unit ids by dsl accessible through the access contract
     *
     * @param metaDataClient the metadata client
     * @param select the selection dsl
     * @param accessContractModel the access contract
     */
    public Set<String> selectUnitsByQueryDslAndAccessContract(
        MetaDataClient metaDataClient,
        SelectMultiQuery select,
        AccessContractModel accessContractModel
    )
        throws InvalidParseOperationException, VitamDBException, MetaDataDocumentSizeException, MetaDataExecutionException, MetaDataClientServerException, InvalidCreateOperationException {
        // Only return document id
        select.addUsedProjection(VitamFieldsHelper.id());
        JsonNode selectWithAccessContractFilter =
            AccessContractRestrictionHelper.applyAccessContractRestrictionForUnitForSelect(
                select.getFinalSelect(),
                accessContractModel
            );

        JsonNode resultJson = metaDataClient.selectUnits(selectWithAccessContractFilter);

        Set<String> foundUnitIds = new HashSet<>();
        for (JsonNode node : resultJson.get(RESULTS)) {
            String id = node.get(VitamFieldsHelper.id()).asText();
            foundUnitIds.add(id);
        }

        return foundUnitIds;
    }

    /**
     * Find unit ids accessible through the access contract
     *
     * @param metaDataClient the metadata client
     * @param unitIds the unit ids to check
     * @param accessContractModel the access contract
     */
    public Set<String> selectUnitsByIdsAndAccessContract(
        MetaDataClient metaDataClient,
        Set<String> unitIds,
        AccessContractModel accessContractModel
    )
        throws InvalidParseOperationException, InvalidCreateOperationException, VitamDBException, MetaDataDocumentSizeException, MetaDataExecutionException, MetaDataClientServerException {
        Set<String> foundUnitIds = new HashSet<>();

        for (List<String> ids : ListUtils.partition(new ArrayList<>(unitIds), MAX_ELASTIC_SEARCH_IN_REQUEST_SIZE)) {
            SelectMultiQuery select = new SelectMultiQuery();
            select.setQuery(QueryHelper.in(VitamFieldsHelper.id(), ids.toArray(new String[0])));

            foundUnitIds.addAll(selectUnitsByQueryDslAndAccessContract(metaDataClient, select, accessContractModel));
        }
        return foundUnitIds;
    }

    /**
     * Load unit graph with all parents recursively.
     *
     * @param unitIds the units ids
     */
    public Map<String, UnitGraphInfo> selectAllUnitGraphByIds(MetaDataClient metaDataClient, Set<String> unitIds)
        throws InvalidParseOperationException, MetaDataExecutionException, MetaDataDocumentSizeException, MetaDataClientServerException, InvalidCreateOperationException {
        // Result map
        Map<String, UnitGraphInfo> unitGraphById = new HashMap<>();

        // The remaining units to load
        Set<String> unitsToLoad = new HashSet<>(unitIds);

        while (!unitsToLoad.isEmpty()) {
            // Load units by bulk (ES $in query size is limited)
            Set<String> bulkIds = unitsToLoad
                .stream()
                .limit(MAX_ELASTIC_SEARCH_IN_REQUEST_SIZE)
                .collect(Collectors.toSet());

            // Pre-fill with null values (in case entry not found)
            for (String bulkId : bulkIds) {
                unitGraphById.put(bulkId, null);
            }

            // Load bulk units
            List<UnitGraphInfo> loadedUnits = loadBulkUnitGraph(metaDataClient, bulkIds);
            for (UnitGraphInfo loadedUnit : loadedUnits) {
                unitGraphById.put(loadedUnit.getId(), loadedUnit);
                for (String up : loadedUnit.getUp()) {
                    if (!unitGraphById.containsKey(up)) {
                        unitsToLoad.add(up);
                    }
                }
            }

            unitsToLoad.removeAll(bulkIds);
        }

        return unitGraphById;
    }

    private List<UnitGraphInfo> loadBulkUnitGraph(MetaDataClient metaDataClient, Collection<String> bulkIds)
        throws InvalidCreateOperationException, InvalidParseOperationException, MetaDataExecutionException, MetaDataDocumentSizeException, MetaDataClientServerException {
        SelectMultiQuery select = new SelectMultiQuery();
        select.setQuery(QueryHelper.in(VitamFieldsHelper.id(), bulkIds.toArray(new String[0])));

        ObjectNode projection = JsonHandler.createObjectNode();
        ObjectNode fields = JsonHandler.createObjectNode();
        fields.put(VitamFieldsHelper.id(), 1);
        fields.put(VitamFieldsHelper.unitups(), 1);
        fields.put(VitamFieldsHelper.unitType(), 1);
        select.setProjection(projection);

        JsonNode resultJson = metaDataClient.selectUnits(select.getFinalSelect()).get(RESULTS);

        UnitGraphInfo[] unitGraphInfo = JsonHandler.getFromJsonNode(resultJson, UnitGraphInfo[].class);
        return Arrays.asList(unitGraphInfo);
    }

    public Map<String, InheritedRuleCategoryResponseModel> loadInheritedHoldRules(
        MetaDataClient metaDataClient,
        Set<String> unitsIdToRearrange
    )
        throws InvalidCreateOperationException, InvalidParseOperationException, MetaDataDocumentSizeException, MetaDataExecutionException, MetaDataClientServerException {
        // Result map
        Map<String, InheritedRuleCategoryResponseModel> result = new HashMap<>();

        // Load units by bulk (ES $in query size is limited)
        Iterator<List<String>> idIterator = Iterators.partition(
            unitsIdToRearrange.iterator(),
            MAX_ELASTIC_SEARCH_IN_REQUEST_SIZE
        );

        while (idIterator.hasNext()) {
            List<String> bulkIds = idIterator.next();

            SelectMultiQuery select = new SelectMultiQuery();
            select.setQuery(QueryHelper.in(VitamFieldsHelper.id(), bulkIds.toArray(new String[0])));
            select.addUsedProjection(VitamFieldsHelper.id());

            JsonNode resultJson = metaDataClient.selectUnitsWithInheritedRules(select.getFinalSelect());

            RequestResponseOK<JsonNode> requestResponseOK = RequestResponseOK.getFromJsonNode(resultJson);
            for (JsonNode unitJson : requestResponseOK.getResults()) {
                String unitId = unitJson.get(VitamFieldsHelper.id()).asText();

                JsonNode inheritedRules = unitJson.get(MetadataRuleService.INHERITED_RULES);
                UnitInheritedRulesResponseModel unitInheritedRulesResponseModel = JsonHandler.getFromJsonNode(
                    inheritedRules,
                    UnitInheritedRulesResponseModel.class
                );
                InheritedRuleCategoryResponseModel inheritedHoldRules = unitInheritedRulesResponseModel
                    .getRuleCategories()
                    .get(VitamConstants.TAG_RULE_HOLD);

                result.put(unitId, inheritedHoldRules);
            }
        }

        return result;
    }
}
