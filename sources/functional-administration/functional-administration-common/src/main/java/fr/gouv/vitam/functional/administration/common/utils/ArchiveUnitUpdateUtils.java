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

package fr.gouv.vitam.functional.administration.common.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.query.action.SetAction;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.administration.RuleMeasurementEnum;
import fr.gouv.vitam.functional.administration.common.FileRules;

import javax.annotation.Nonnull;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.gouv.vitam.common.SedaConstants.TAG_RULES;
import static fr.gouv.vitam.common.model.RequestResponseOK.TAG_RESULTS;
import static fr.gouv.vitam.common.model.unit.RuleModel.END_DATE;
import static fr.gouv.vitam.common.model.unit.RuleModel.RULE;
import static fr.gouv.vitam.common.model.unit.RuleModel.START_DATE;
import static fr.gouv.vitam.functional.administration.common.FileRules.RULEID;

/**
 * ArchiveUnitUpdateUtils in order to deal with common update operations for units
 */
public class ArchiveUnitUpdateUtils {

    public static final String UNLIMITED_RULE_DURATION = "unlimited";

    private static final String _DIFF = "$diff";
    private static final String DIFF = "#diff";
    private static final String FINAL_DIFF = "diff";

    /**
     * Method used to get update query for an archive unit
     *
     * @param rulesForCategory
     * @param listRulesByType
     * @param query
     * @param key
     * @return
     * @throws InvalidCreateOperationException
     */
    public static boolean updateCategoryRules(
        JsonNode rulesForCategory,
        List<JsonNode> listRulesByType,
        UpdateMultiQuery query,
        String key
    ) throws InvalidCreateOperationException {
        ArrayNode updatedRulesFinalForCategory = JsonHandler.createArrayNode();
        boolean updateNeeded = false;
        for (JsonNode ruleToUpdate : rulesForCategory) {
            String updateRuleName = ruleToUpdate.get(RULE).asText();
            boolean findIt = false;
            for (JsonNode rule : listRulesByType) {
                // if the rule to be updated has no start date, then we dont do the maths,
                // that means the rule is not really active - even if the query that returns
                // the list of au to be updated has been updated, just in case, the test is still present
                if (
                    rule.get(RULEID).asText() != null &&
                    rule.get(RULEID).asText().equals(updateRuleName) &&
                    ruleToUpdate.get(START_DATE) != null
                ) {
                    updateNeeded = true;
                    findIt = true;
                    final JsonNode updatedRule = computeEndDate((ObjectNode) ruleToUpdate, rule);
                    updatedRulesFinalForCategory.add(updatedRule);
                }
            }
            if (!findIt) {
                updatedRulesFinalForCategory.add(ruleToUpdate);
            }
        }
        // Put newRules in a new action
        Map<String, JsonNode> action = new HashMap<>();
        action.put(VitamFieldsHelper.management() + "." + key + "." + TAG_RULES, updatedRulesFinalForCategory);

        query.addActions(new SetAction(action));
        return updateNeeded;
    }

    @Nonnull
    public static JsonNode computeEndDate(@Nonnull ObjectNode updatingRule, JsonNode ruleModel) {
        final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        if (!updatingRule.has(START_DATE)) {
            return updatingRule;
        }

        String startDateString = updatingRule.get(START_DATE).asText();
        String ruleId = updatingRule.get(RULE).asText();

        if (ParametersChecker.isNotEmpty(startDateString, ruleId)) {
            LocalDate startDate = LocalDate.parse(startDateString, timeFormatter);
            if (startDate.getYear() >= 9000) {
                throw new IllegalStateException("Wrong Start Date");
            }

            if (ruleModel.has(FileRules.RULEDURATION)) {
                final String duration = ruleModel.get(FileRules.RULEDURATION).asText();
                final String measurement = ruleModel.get(FileRules.RULEMEASUREMENT).asText();
                if (!UNLIMITED_RULE_DURATION.equalsIgnoreCase(duration)) {
                    final RuleMeasurementEnum ruleMeasurement = RuleMeasurementEnum.getEnumFromType(measurement);
                    updatingRule.put(
                        END_DATE,
                        startDate
                            .plus(Integer.parseInt(duration), ruleMeasurement.getTemporalUnit())
                            .format(timeFormatter)
                    );
                } else {
                    // remove existing endDate
                    updatingRule.remove(END_DATE);
                }
            }
        }

        return updatingRule;
    }

    /**
     * Common method to get the diff message
     *
     * @param diff
     * @param unitId
     * @return
     * @throws InvalidParseOperationException
     */
    public static String getDiffMessageFor(JsonNode diff, String unitId) throws InvalidParseOperationException {
        if (diff != null) {
            final JsonNode arrayNode = diff.has(_DIFF) ? diff.get(_DIFF) : diff.get(TAG_RESULTS);
            if (arrayNode != null) {
                for (final JsonNode diffNode : arrayNode) {
                    if (
                        diffNode.get(VitamFieldsHelper.id()) != null &&
                        unitId.equals(diffNode.get(VitamFieldsHelper.id()).textValue())
                    ) {
                        ObjectNode diffObject = JsonHandler.createObjectNode();
                        diffObject.set(FINAL_DIFF, diffNode.get(DIFF));
                        return JsonHandler.writeAsString(diffObject);
                    }
                }
            }
        }
        return "";
    }
}
