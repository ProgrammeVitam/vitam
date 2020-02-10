/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.metadata.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.VitamConstants;
import fr.gouv.vitam.metadata.core.model.InheritedRuleModel;

/**
 * POJO for the result of Inherited Rule
 * @deprecated : Use the new api /unitsWithInheritedRules instead. To be removed in future releases.
 */
public class UnitInheritedRule {

    private static final String SEPERATOR = "-";
    private static final String PATH = "path";
    private static final String PREVENTINHERITANCE = "PreventInheritance";
    private static final String PREVENTRULESID = "PreventRulesId";
    private static final String RULES = "Rules";

    /**
     * rule field name
     */
    public static final String RULE = "Rule";

    /**
     * inherited rule field name
     */
    public static final String INHERITED_RULE = "inheritedRule";

    /**
     * Map of category / ruleId / unitOrigin / Rules-with-paths
     */
    @JsonProperty("inheritedRule")
    private Map<String, Map<String, Map<String, InheritedRuleModel>>> inheritedRule;

    private String unitId;

    private Set<String> refNonRuleIds = new HashSet<>();

    /**
     * empty constructor for every unitNode except root
     */
    public UnitInheritedRule() {
        inheritedRule = new HashMap<>();
        unitId = "";
    }

    /**
     * @return a map of category / ruleId / unitOrigin / Rules-with-paths (inheritedRules)
     */
    public Map<String, Map<String, Map<String, InheritedRuleModel>>> getInheritedRule() {
        return inheritedRule;
    }

    /**
     * @param rules as a map
     * @return UnitInheritedRule in which inheritedRule setted
     */
    public UnitInheritedRule setInheritedRule(Map<String, Map<String, Map<String, InheritedRuleModel>>> rules) {
        this.inheritedRule = rules;
        return this;
    }

    /**
     * Concat UnitInheritedRule when unit have many parent
     *
     * @param parentRule of type UnitInheritedRule
     */
    public void concatRule(UnitInheritedRule parentRule) {
        for (Entry<String, Map<String, Map<String, InheritedRuleModel>>> paroleCategoryEntry : parentRule.inheritedRule
            .entrySet()) {
            String parentCategoryName = paroleCategoryEntry.getKey();
            Map<String, Map<String, InheritedRuleModel>> parentCategoryNode = paroleCategoryEntry.getValue();
            if (inheritedRule.containsKey(parentCategoryName)) {

                for (Entry<String, Map<String, InheritedRuleModel>> parentRuleEntry : parentCategoryNode.entrySet()) {
                    String parentRuleId = parentRuleEntry.getKey();
                    Map<String, Map<String, InheritedRuleModel>> selfCategoryNode =
                        inheritedRule.get(parentCategoryName);
                    // rule name
                    Map<String, InheritedRuleModel> selfRuleNode = selfCategoryNode.get(parentRuleId);
                    if (selfRuleNode != null) {
                        for (Entry<String, InheritedRuleModel> parentOriginEntry : parentCategoryNode.get(parentRuleId)
                            .entrySet()) {
                            String parentOriginId = parentOriginEntry.getKey();
                            InheritedRuleModel selfOriginDetailNode = selfRuleNode.get(parentOriginId);
                            InheritedRuleModel parentOriginDetailNode =
                                parentCategoryNode.get(parentRuleId).get(parentOriginId);

                            if (selfOriginDetailNode != null) {
                                if (!selfOriginDetailNode.getPath().equals(parentOriginDetailNode.getPath())) {
                                    ((ArrayNode) selfOriginDetailNode.getPath())
                                        .addAll((ArrayNode) parentOriginDetailNode.getPath());
                                }
                            } else {
                                // Do not take parent if the rule is defined in the child: Override it)
                                if (!selfRuleNode.containsKey(unitId)) {
                                    selfRuleNode.put(parentOriginId, parentOriginDetailNode);
                                }
                            }
                            if (parentOriginDetailNode.getOverrideBy() != null &&
                                !parentOriginDetailNode.getOverrideBy().isEmpty()) {
                                for (String reference : parentOriginDetailNode.getOverrideBy()) {
                                    selfRuleNode.remove(reference);
                                }
                            }
                            if (selfOriginDetailNode != null && selfOriginDetailNode.getOverrideBy() != null &&
                                !parentOriginDetailNode.getOverrideBy().isEmpty()) {
                                for (String reference : selfOriginDetailNode.getOverrideBy()) {
                                    selfRuleNode.remove(reference);
                                }
                            }
                        }
                    } else {
                        selfCategoryNode.put(parentRuleId, parentCategoryNode.get(parentRuleId));
                    }
                }
            } else {
                inheritedRule.put(parentCategoryName, parentCategoryNode);
            }
        }
    }

    /**
     * Herite rule from parent with unit management
     *
     * @param unitManagement as ObjectNode
     * @param unitId         as String
     * @return UnitInheritedRule created
     * @throws InvalidParseOperationException
     */
    public UnitInheritedRule createNewInheritedRule(ObjectNode unitManagement, String unitId)
        throws InvalidParseOperationException {
        UnitInheritedRule newRule = deepCopy(this);
        if (newRule.unitId.length() == 0) {
            newRule.unitId = unitId;
        }
        Map<String, ObjectNode> ruleCategoryFromUnit = new HashMap<>();
        Map<String, String> ruleIdTodReplace = new HashMap<>();
        List<String> parentCategoryList = new ArrayList<>();

        // By Category
        for (Entry<String, Map<String, Map<String, InheritedRuleModel>>> categoryEntry : newRule.inheritedRule
            .entrySet()) {
            String categoryName = categoryEntry.getKey();
            parentCategoryList.add(categoryName);
            Map<String, Map<String, InheritedRuleModel>> categoryValue = categoryEntry.getValue();
            ruleCategoryFromUnit.remove(categoryName);
            if (unitManagement.get(categoryName) == null ||
                unitManagement.get(categoryName).get(RULES) == null ||
                unitManagement.get(categoryName).get(RULES).size() == 0) {
                // Unit management does not contain category rule
                // By rule
                for (Entry<String, Map<String, InheritedRuleModel>> ruleEntry : categoryValue.entrySet()) {
                    Map<String, InheritedRuleModel> ruleValue = ruleEntry.getValue();
                    // By origin
                    for (Entry<String, InheritedRuleModel> ruleForOriginEntry : ruleValue.entrySet()) {
                        ArrayNode pathNode = (ArrayNode) ruleForOriginEntry.getValue().getPath();
                        updateOriginPath(pathNode, unitId);
                    }
                }
            } else {
                // Unit management contains category rule
                // By rule
                for (Entry<String, Map<String, InheritedRuleModel>> ruleEntry : categoryValue.entrySet()) {
                    String ruleId = ruleEntry.getKey();
                    Map<String, InheritedRuleModel> ruleValue = ruleEntry.getValue();
                    JsonNode unitManagementRuleCategory = unitManagement.get(categoryName);
                    compareInheritedRuleWithManagement(unitManagementRuleCategory, ruleId, ruleValue, unitId,
                        ruleIdTodReplace);
                }

                for (Entry<String, String> ruleIdEntry : ruleIdTodReplace.entrySet()) {
                    String originId = ruleIdEntry.getKey().split(SEPERATOR, 2)[0];
                    String ruleId = ruleIdEntry.getKey().split(SEPERATOR, 2)[1];

                    InheritedRuleModel ruleModel = categoryValue.get(ruleId).get(originId);
                    categoryValue.get(ruleId).remove(originId);
                    categoryValue.get(ruleId).put(ruleIdEntry.getValue(), ruleModel);
                }
                ruleIdTodReplace.clear();
            }

            if (unitManagement.get(categoryName) != null &&
                unitManagement.get(categoryName).get("Inheritance") != null) {
                addPreventRulesId(unitManagement.get(categoryName).get("Inheritance").get("PreventRulesId"));
            }
        }


        for (String ruleCategory : VitamConstants.getSupportedRules()) {
            JsonNode unitManagementRuleCategory = unitManagement.get(ruleCategory);
            if (unitManagementRuleCategory == null) {
                continue;
            }
            if (unitManagementRuleCategory.has("Inheritance")) {
                JsonNode inheritance = unitManagementRuleCategory.get("Inheritance");
                if (inheritance.has(PREVENTRULESID)) {
                    addPreventRulesId(inheritance.get(PREVENTRULESID));
                }
                computeRuleCategoryInheritancePrevent(inheritance, newRule, ruleCategory);
            }

            if (unitManagementRuleCategory.has(RULES)) {
                computeRuleCategoryInheritance(unitManagementRuleCategory, newRule, ruleCategory, ruleCategoryFromUnit,
                    parentCategoryList);
            }

        }

        for (Map.Entry<String, ObjectNode> entry : ruleCategoryFromUnit.entrySet()) {
            String categoryName = entry.getKey();
            ObjectNode category = entry.getValue();
            Map<String, Map<String, InheritedRuleModel>> rulesMap = new HashMap<>();
            if (newRule.inheritedRule.containsKey(categoryName)) {
                rulesMap = newRule.inheritedRule.get(categoryName);
            } else {
                newRule.inheritedRule.put(categoryName, rulesMap);
            }

            Iterator<String> categoryRules = category.fieldNames();
            while (categoryRules.hasNext()) {
                String idRule = categoryRules.next();
                JsonNode rule = category.get(idRule);
                Map<String, InheritedRuleModel> originsMap = new HashMap<>();
                if (rulesMap.containsKey(idRule)) {
                    originsMap = rulesMap.get(idRule);
                } else {
                    rulesMap.put(idRule, originsMap);
                }

                Iterator<String> origins = rule.fieldNames();
                while (origins.hasNext()) {
                    String idOrigin = origins.next();
                    JsonNode origin = rule.get(idOrigin);
                    InheritedRuleModel ruleModel = JsonHandler.getFromJsonNode(origin, InheritedRuleModel.class);
                    ruleModel.setPath((ArrayNode) origin.get("path"));
                    originsMap.put(idOrigin, ruleModel);
                }
            }
        }

        for (String categoryName : parentCategoryList) {
            if (newRule.inheritedRule.containsKey(categoryName)) {
                if (newRule.inheritedRule.get(categoryName).isEmpty()) {
                    newRule.inheritedRule.remove(categoryName);
                }
            }
        }
        return newRule;
    }

    /**
     * Remove rules concerning by preventInheretance
     *
     * @param inheritance
     * @param newRule
     * @param unitRuleCategory
     */
    private void computeRuleCategoryInheritancePrevent(JsonNode inheritance, UnitInheritedRule newRule,
        String unitRuleCategory) {
        if (inheritance.has(PREVENTRULESID)) {
            ArrayNode nonRefRuleId = (ArrayNode) inheritance.get(PREVENTRULESID);
            for (int index = nonRefRuleId.size() - 1; index >= 0; index--) {
                String refNonRuleId = nonRefRuleId.get(index).asText();
                if (newRule.inheritedRule.get(unitRuleCategory) != null) {
                    newRule.inheritedRule.get(unitRuleCategory).remove(refNonRuleId);
                }
                refNonRuleIds.add(refNonRuleId);
            }
        }
        if (checkPreventInheritance(inheritance) && newRule.inheritedRule.get(unitRuleCategory) != null) {
            newRule.inheritedRule.get(unitRuleCategory).clear();
        }
    }

    /**
     * @param unitManagementRuleCategory the ruleCategory of the unit
     * @param newRule
     * @param unitRuleCategory           the name of the category
     * @param ruleCategoryFromUnit
     * @param parentCategoryList
     */
    private void computeRuleCategoryInheritance(JsonNode unitManagementRuleCategory, UnitInheritedRule newRule,
        String unitRuleCategory, Map<String, ObjectNode> ruleCategoryFromUnit,
        List<String> parentCategoryList) {
        Map<String, JsonNode> categoryFinalAction = new HashMap<>();
        Map<String, JsonNode> categoryClassificationLevel = new HashMap<>();
        Map<String, JsonNode> categoryClassificationOwner = new HashMap<>();
        for (JsonNode rule : unitManagementRuleCategory.get(RULES)) {
            ObjectNode ruleToEdit = (ObjectNode) rule;
            if (!parentCategoryList.contains(unitRuleCategory) && rule.has(RULE)) {
                ObjectNode ruleCategories =
                    createRuleCategories(unitManagementRuleCategory, ruleToEdit, newRule.unitId);
                String ruleName = rule.get(RULE).asText();
                JsonNode unitRuleNode = ruleCategories.get(ruleName);
                categoryFinalAction
                    .put(unitRuleCategory, unitRuleNode.get(newRule.unitId).get(SedaConstants.TAG_RULE_FINAL_ACTION));
                categoryClassificationLevel
                    .put(unitRuleCategory, unitRuleNode.get(newRule.unitId).get(SedaConstants.TAG_RULE_CLASSIFICATION_LEVEL));
                categoryClassificationOwner
                    .put(unitRuleCategory, unitRuleNode.get(newRule.unitId).get(SedaConstants.TAG_RULE_CLASSIFICATION_OWNER));

                ruleCategoryFromUnit.put(unitRuleCategory, ruleCategories);
                parentCategoryList.add(unitRuleCategory);
            } else {

                // Add FinalAction
                JsonNode finalActionNode = categoryFinalAction.get(unitRuleCategory);
                if (null != finalActionNode) {
                    ruleToEdit.set(SedaConstants.TAG_RULE_FINAL_ACTION, finalActionNode);
                }

                // Add ClassificationLevel
                JsonNode classificationLevel = categoryClassificationLevel.get(unitRuleCategory);
                if (null != classificationLevel) {
                    ruleToEdit.set(SedaConstants.TAG_RULE_CLASSIFICATION_LEVEL, classificationLevel);
                }
                // Add ClassificationOwner
                JsonNode classificationOwner = categoryClassificationOwner.get(unitRuleCategory);
                if (null != classificationOwner) {
                    ruleToEdit.set(SedaConstants.TAG_RULE_CLASSIFICATION_OWNER, classificationOwner);
                }

                addInheritedRuleFromManagement(newRule, ruleToEdit, unitRuleCategory, newRule.unitId,
                    ruleCategoryFromUnit);
            }
        }
    }

    private void addInheritedRuleFromManagement(UnitInheritedRule newRule, ObjectNode unitRuleNode,
        String unitRuleCategory, String unitId, Map<String, ObjectNode> ruleCategoryFromUnit) {
        if (unitRuleNode.has(RULE)) {
            String ruleId = unitRuleNode.get(RULE).asText();

            if (checkPreventInheritance(unitRuleNode) && newRule.inheritedRule.get(unitRuleCategory) != null) {
                newRule.inheritedRule.get(unitRuleCategory).clear();
            }

            if (!ruleCategoryFromUnit.containsKey(unitRuleCategory)) {
                ruleCategoryFromUnit.put(unitRuleCategory, new ObjectNode(null));
            }

            if (!newRule.inheritedRule.containsKey(unitRuleCategory) ||
                !newRule.inheritedRule.get(unitRuleCategory).containsKey(ruleId) ||
                ruleCategoryFromUnit.get(unitRuleCategory).get(ruleId) == null) {
                ObjectNode unitNode = createNewRuleWithOrigin(unitRuleNode, unitId);
                ruleCategoryFromUnit.get(unitRuleCategory).set(ruleId, unitNode);
            }
        } else {
            if (checkPreventInheritance(unitRuleNode)) {
                newRule.inheritedRule.remove(unitRuleCategory);
            }
        }
    }

    private void compareInheritedRuleWithManagement(JsonNode unitManagementRuleCategory, String ruleId,
        Map<String, InheritedRuleModel> ruleValue, String unitId,
        Map<String, String> ruleIdTodReplace) throws InvalidParseOperationException {

        if (null == unitManagementRuleCategory) {
            return;
        }

        ArrayNode unitRuleNodes = (ArrayNode) unitManagementRuleCategory.get(RULES);

        if (null == unitRuleNodes) {
            return;
        }

        for (JsonNode unitRule : unitRuleNodes) {

            String unitRuleId = null;
            if (unitRule.get(RULE) != null) {
                unitRuleId = unitRule.get(RULE).asText();
            }

            ObjectNode modifiedUnitRule = (ObjectNode) unitRule;
            modifiedUnitRule.set(SedaConstants.TAG_RULE_FINAL_ACTION,
                unitManagementRuleCategory.get(SedaConstants.TAG_RULE_FINAL_ACTION));

            if (unitRuleId == null || !unitRuleId.equals(ruleId)) {
                // Unit management contains category rule but not the ruleId
                for (Entry<String, InheritedRuleModel> originEntry : ruleValue.entrySet()) {
                    ArrayNode pathNode = originEntry.getValue().getPath();
                    updateOriginPath(pathNode, unitId);
                }

            } else {
                // Unit management contains category rule with the same ruleId
                for (Entry<String, InheritedRuleModel> originEntry : ruleValue.entrySet()) {
                    String originId = originEntry.getKey();
                    InheritedRuleModel originValue = originEntry.getValue();
                    InheritedRuleModel copiedRule = createNewOriginModel((ObjectNode) unitRule,
                        JsonHandler.createArrayNode().add(JsonHandler.createArrayNode().add(unitId)));
                    ruleValue.put(originId, copiedRule);
                    ruleIdTodReplace.put(originId + SEPERATOR + ruleId, unitId);
                    if (originValue.getOverrideBy() == null) {
                        originValue.setOverrideBy(new ArrayList<>());
                    }
                    originValue.getOverrideBy().add(unitId);
                }
            }
        }
    }

    private ObjectNode createRuleCategories(JsonNode unitManagementRuleCategory, ObjectNode unitRule, String unitId) {
        String ruleId = "";
        if (unitRule.has(RULE)) {
            ruleId = unitRule.get(RULE).textValue();
        }
        ObjectNode newValue = JsonHandler.createObjectNode();
        newValue.setAll(unitRule);
        newValue.remove(RULE);
        newValue.set(PATH, JsonHandler.createArrayNode().add(JsonHandler.createArrayNode().add(unitId)));

        // Add FinalAction
        JsonNode finalActionNode = unitManagementRuleCategory.get(SedaConstants.TAG_RULE_FINAL_ACTION);
        if (null != finalActionNode) {
            newValue.set(SedaConstants.TAG_RULE_FINAL_ACTION, finalActionNode);
        }

        // Add ClassificationLevel
        JsonNode classificationLevel = unitManagementRuleCategory.get(SedaConstants.TAG_RULE_CLASSIFICATION_LEVEL);
        if (null != classificationLevel) {
            newValue.set(SedaConstants.TAG_RULE_CLASSIFICATION_LEVEL, classificationLevel);
        }
        // Add ClassificationOwner
        JsonNode classificationOwner = unitManagementRuleCategory.get(SedaConstants.TAG_RULE_CLASSIFICATION_OWNER);
        if (null != classificationOwner) {
            newValue.set(SedaConstants.TAG_RULE_CLASSIFICATION_OWNER, classificationOwner);
        }

        // Create Origin of rule (arrayNode)
        ObjectNode ruleOrigin = JsonHandler.createObjectNode();
        ruleOrigin.set(unitId, newValue);

        // Create Category of rule (arrayNode)
        ObjectNode ruleCategory = JsonHandler.createObjectNode();
        ruleCategory.set(ruleId, ruleOrigin);
        return ruleCategory;
    }

    private void updateOriginPath(ArrayNode pathNode, String unitId) {
        for (JsonNode subPath : pathNode) {
            ArrayNode pathArray = (ArrayNode) subPath;
            boolean shouldBeAdded = true;
            for (JsonNode path : pathArray) {
                shouldBeAdded = path.asText().equals(unitId) ? false : shouldBeAdded;
            }
            if (shouldBeAdded) {
                pathArray.add(unitId);
            }
        }
    }

    private InheritedRuleModel createNewOriginModel(ObjectNode unitRuleNode, ArrayNode pathNode)
        throws InvalidParseOperationException {
        InheritedRuleModel newValue = JsonHandler.getFromJsonNode(unitRuleNode, InheritedRuleModel.class);
        newValue.setRule(null);
        newValue.setPath(pathNode);
        return newValue;
    }

    private ObjectNode createNewRuleWithOrigin(ObjectNode unitRuleNode, String unitId) {
        ArrayNode pathNode = JsonHandler.createArrayNode().add(JsonHandler.createArrayNode().add(unitId));
        ObjectNode newOrigin = JsonHandler.createObjectNode();
        ObjectNode newRule = JsonHandler.createObjectNode();
        newOrigin.setAll(unitRuleNode);
        newOrigin.remove(RULE);
        newOrigin.set(PATH, pathNode);
        newRule.set(unitId, newOrigin);
        return newRule;
    }

    private boolean checkPreventInheritance(JsonNode unitRuleNode) {
        if (unitRuleNode.has(PREVENTINHERITANCE)) {
            if (unitRuleNode.get(PREVENTINHERITANCE).asText().equals("true")) {
                return true;
            }
        }
        return false;
    }

    private void addPreventRulesId(JsonNode preventRulesIdNode) {
        if (preventRulesIdNode != null && preventRulesIdNode.isArray()) {
            for (JsonNode ruleId : (ArrayNode) preventRulesIdNode) {
                refNonRuleIds.add(ruleId.asText());
            }
        }
    }

    /**
     * check inheritedRule set if empty
     *
     * @return boolean value
     */
    @JsonIgnore
    public boolean isEmpty() {
        return inheritedRule.isEmpty();
    }

    /**
     * Deep copy a new unit rule
     *
     * @param unit as UnitInheritedRule
     * @return UnitInheritedRule where unit is copied
     * @throws InvalidParseOperationException
     */
    private UnitInheritedRule deepCopy(UnitInheritedRule unit) throws InvalidParseOperationException {
        UnitInheritedRule newRule = new UnitInheritedRule();
        JsonNode oldUnitNode = JsonHandler.toJsonNode(unit);
        newRule = JsonHandler.getFromJsonNode(oldUnitNode, UnitInheritedRule.class);
        newRule.refNonRuleIds.clear();
        return newRule;
    }


}
