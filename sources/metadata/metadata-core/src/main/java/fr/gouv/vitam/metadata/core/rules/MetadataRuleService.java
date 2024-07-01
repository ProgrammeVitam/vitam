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
package fr.gouv.vitam.metadata.core.rules;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.parser.request.multiple.RequestParserHelper;
import fr.gouv.vitam.common.database.parser.request.multiple.RequestParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamDBException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.rules.UnitInheritedRulesResponseModel;
import fr.gouv.vitam.common.model.rules.UnitRuleModel;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.core.MetaDataImpl;
import fr.gouv.vitam.metadata.core.model.MetadataResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class MetadataRuleService {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MetadataRuleService.class);
    private static final int MAX_PRINTED_MISSING_UNITS = 10;
    public static final String INHERITED_RULES = "InheritedRules";

    /**
     * By default ES does not allow more than 1024 clauses in selects
     * item IN ( "val1",... "val9") accounts for 9 clauses
     */
    private static final int MAX_ELASTIC_SEARCH_IN_REQUEST_SIZE = 1000;

    private final ComputeInheritedRuleService computeInheritedRuleService;
    private final MetaDataImpl metaData;

    public MetadataRuleService(MetaDataImpl metaData) {
        this(new ComputeInheritedRuleService(), metaData);
    }

    @VisibleForTesting
    MetadataRuleService(ComputeInheritedRuleService computeInheritedRuleService, MetaDataImpl metaData) {
        this.computeInheritedRuleService = computeInheritedRuleService;
        this.metaData = metaData;
    }

    /**
     * Select units by DSL and computes inherited rules for matching units
     *
     * @param selectQuery the query DSL
     * @return the selected units with there inherited rules
     */
    public MetadataResult selectUnitsWithInheritedRules(JsonNode selectQuery)
        throws InvalidParseOperationException, MetaDataExecutionException, BadRequestException, VitamDBException, MetaDataNotFoundException, MetaDataDocumentSizeException {
        LOGGER.debug("selectUnitsWithInheritedRules / selectQuery: " + selectQuery);

        RequestParserMultiple parser = RequestParserHelper.getParser(selectQuery);
        if (!(parser instanceof SelectParserMultiple)) {
            throw new InvalidParseOperationException("Expected a select query");
        }

        // Set target collection in hint
        final SelectMultiQuery request = (SelectMultiQuery) parser.getRequest();

        ObjectNode fieldsProjection = (ObjectNode) request
            .getProjection()
            .get(BuilderToken.PROJECTION.FIELDS.exactToken());
        if (fieldsProjection != null && fieldsProjection.size() > 0) {
            // Check obsolete #rules
            if (fieldsProjection.has(BuilderToken.GLOBAL.RULES.exactToken())) {
                throw new InvalidParseOperationException(
                    "Invalid " + BuilderToken.GLOBAL.RULES.exactToken() + " projection"
                );
            }

            // Ensure unit id is included (required for inherited rule computation)
            fieldsProjection.put(VitamFieldsHelper.id(), 1);
        }

        final MetadataResult metadataResult = metaData.selectUnitsByQuery(request.getFinalSelect());

        // Compute inherited rules
        computeInheritedRulesForUnits(metadataResult.getResults());

        return metadataResult;
    }

    private void computeInheritedRulesForUnits(List<JsonNode> results)
        throws InvalidParseOperationException, MetaDataNotFoundException, MetaDataDocumentSizeException, MetaDataExecutionException, BadRequestException, VitamDBException {
        List<String> unitIds = new ArrayList<>();
        for (JsonNode jsonNode : results) {
            unitIds.add(jsonNode.get(VitamFieldsHelper.id()).asText());
        }

        Map<String, UnitRuleModel> unitRulesByIdsMap = loadUnitRuleHierarchy(unitIds);

        Map<String, UnitInheritedRulesResponseModel> inheritedRules = computeInheritedRuleService.computeInheritedRules(
            unitRulesByIdsMap
        );

        for (JsonNode jsonNode : results) {
            String id = jsonNode.get(VitamFieldsHelper.id()).asText();
            ((ObjectNode) jsonNode).set(INHERITED_RULES, JsonHandler.toJsonNode(inheritedRules.get(id)));
        }
    }

    private Map<String, UnitRuleModel> loadUnitRuleHierarchy(Collection<String> unitIds)
        throws InvalidParseOperationException, MetaDataNotFoundException, MetaDataDocumentSizeException, MetaDataExecutionException, BadRequestException, VitamDBException {
        // Result map
        Map<String, UnitRuleModel> unitRulesById = new HashMap<>();

        // The remaining units to load
        Set<String> unitsToLoad = new HashSet<>(unitIds);

        while (!unitsToLoad.isEmpty()) {
            // Load units by bulk (ES $in query size is limited)
            Set<String> bulkIds = unitsToLoad
                .stream()
                .limit(MAX_ELASTIC_SEARCH_IN_REQUEST_SIZE)
                .collect(Collectors.toSet());

            // Pre-fill with null values (in case entry is not found)
            for (String bulkId : bulkIds) {
                unitRulesById.put(bulkId, null);
            }

            // Load bulk units
            List<UnitRuleModel> foundUnitRules = loadBulkUnitRules(bulkIds);
            for (UnitRuleModel unitRuleModel : foundUnitRules) {
                unitRulesById.put(unitRuleModel.getId(), unitRuleModel);
                for (String up : unitRuleModel.getUp()) {
                    if (!unitRulesById.containsKey(up)) {
                        unitsToLoad.add(up);
                    }
                }
            }

            unitsToLoad.removeAll(bulkIds);
        }

        // Ensure all units have been loaded
        List<String> notFoundUnits = unitRulesById
            .entrySet()
            .stream()
            .filter(entry -> entry.getValue() == null)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        if (!notFoundUnits.isEmpty()) {
            String unitsToPrint = notFoundUnits
                .stream()
                .limit(MAX_PRINTED_MISSING_UNITS)
                .collect(Collectors.joining(",", "[", "]"));
            throw new MetaDataNotFoundException("Could not find " + notFoundUnits.size() + " units: " + unitsToPrint);
        }

        return unitRulesById;
    }

    private List<UnitRuleModel> loadBulkUnitRules(Set<String> unitIds)
        throws InvalidParseOperationException, MetaDataNotFoundException, MetaDataDocumentSizeException, MetaDataExecutionException, BadRequestException, VitamDBException {
        SelectMultiQuery select = new SelectMultiQuery();
        select.addRoots(unitIds.toArray(new String[0]));
        select.addUsedProjection(
            VitamFieldsHelper.id(),
            VitamFieldsHelper.unitups(),
            VitamFieldsHelper.originatingAgency(),
            VitamFieldsHelper.management()
        );

        final MetadataResult metadataResult = metaData.selectUnitsByQuery(select.getFinalSelect());

        List<UnitRuleModel> unitRules = new ArrayList<>();
        for (JsonNode unitRuleJsonNode : metadataResult.getResults()) {
            unitRules.add(JsonHandler.getFromJsonNode(unitRuleJsonNode, UnitRuleModel.class));
        }

        return unitRules;
    }
}
