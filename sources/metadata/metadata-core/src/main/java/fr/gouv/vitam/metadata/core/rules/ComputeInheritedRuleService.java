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

import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.model.VitamConstants;
import fr.gouv.vitam.common.model.rules.InheritedPropertyResponseModel;
import fr.gouv.vitam.common.model.rules.InheritedRuleCategoryResponseModel;
import fr.gouv.vitam.common.model.rules.InheritedRuleResponseModel;
import fr.gouv.vitam.common.model.rules.UnitInheritedRulesResponseModel;
import fr.gouv.vitam.common.model.rules.UnitRuleModel;
import fr.gouv.vitam.common.model.unit.RuleCategoryModel;
import fr.gouv.vitam.common.model.unit.RuleModel;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.ListUtils.union;

/**
 * Unit inherited rules service
 */
public class ComputeInheritedRuleService {

    /**
     * Computes inherited rules given local unit rule definitions
     *
     * @param unitRulesById Map of local unit rules by unit id (the unit and all it's parents must be included)
     * @return the inherited rules by unit ids
     */
    public Map<String, UnitInheritedRulesResponseModel> computeInheritedRules(
        Map<String, UnitRuleModel> unitRulesById
    ) {
        Map<String, UnitInheritedRulesResponseModel> unitInheritedRulesByUnitIdMap = new HashMap<>();

        for (String unitId : unitRulesById.keySet()) {
            computeUnitInheritedRules(unitId, unitRulesById, unitInheritedRulesByUnitIdMap);
        }

        return unitInheritedRulesByUnitIdMap;
    }

    private void computeUnitInheritedRules(
        String unitId,
        Map<String, UnitRuleModel> unitRulesById,
        Map<String, UnitInheritedRulesResponseModel> unitInheritedRulesByUnitIdMap
    ) {
        if (unitInheritedRulesByUnitIdMap.containsKey(unitId)) {
            // Already computed
            return;
        }

        UnitRuleModel unitRuleModel = unitRulesById.get(unitId);

        // Ensure parent units have been proceeded before self
        for (String parentUnit : unitRuleModel.getUp()) {
            computeUnitInheritedRules(parentUnit, unitRulesById, unitInheritedRulesByUnitIdMap);
        }

        // Compute unit inherited rules
        UnitInheritedRulesResponseModel unitInheritedRules = computeUnitInheritedRulesFromParents(
            unitRuleModel,
            unitInheritedRulesByUnitIdMap
        );

        unitInheritedRulesByUnitIdMap.put(unitRuleModel.getId(), unitInheritedRules);
    }

    /**
     * Compute inherited rules using local and parent rules.
     *
     * Basic domain rules / constraints:
     * - Rules are organized by categories (AppraisalRule, ReuseRule...)
     * - Every category may declare rules and/or properties
     * - Rules & properties are inherited from parents unless :
     * --> PreventInheritance flag is defined
     * --> PreventRulesId is defined (rules only)
     * --> Child unit redefines a specific rule (same rule id) or property (property name)
     * - Rules have ids and optional attributes (eg: StartDate, EndDate, HoldOwner...)
     * - Properties may have implicit default value : If not explicitly defined (same property name, same Originating
     * Agency) => a default/implicit local property is assumed (which might redefine similar inherited properties
     * from a different Originating Agency)
     */
    private UnitInheritedRulesResponseModel computeUnitInheritedRulesFromParents(
        UnitRuleModel unitRuleModel,
        Map<String, UnitInheritedRulesResponseModel> unitInheritedRulesByUnitIdMap
    ) {
        UnitInheritedRulesResponseModel unitInheritedRules = new UnitInheritedRulesResponseModel();
        for (String ruleCategory : VitamConstants.getSupportedRules()) {
            InheritedRuleCategoryResponseModel inheritedRuleCategory = computeInheritedRuleCategory(
                unitRuleModel,
                unitInheritedRulesByUnitIdMap,
                ruleCategory
            );

            unitInheritedRules.setRuleCategory(ruleCategory, inheritedRuleCategory);
        }

        List<InheritedPropertyResponseModel> inheritedPropertyResponseModel = computeInheritedGlobalProperties(
            unitRuleModel,
            unitInheritedRulesByUnitIdMap
        );
        unitInheritedRules.setGlobalProperties(inheritedPropertyResponseModel);

        return unitInheritedRules;
    }

    private InheritedRuleCategoryResponseModel computeInheritedRuleCategory(
        UnitRuleModel unitRuleModel,
        Map<String, UnitInheritedRulesResponseModel> unitInheritedRulesByUnitIdMap,
        String ruleCategory
    ) {
        RuleCategoryModel ruleCategoryModel = unitRuleModel.getManagementModel().getRuleCategoryModel(ruleCategory);

        List<InheritedRuleResponseModel> rules = computeInheritedRules(
            unitRuleModel,
            unitInheritedRulesByUnitIdMap,
            ruleCategory,
            ruleCategoryModel
        );

        List<InheritedPropertyResponseModel> properties = computeInheritedProperties(
            unitRuleModel,
            unitInheritedRulesByUnitIdMap,
            ruleCategory,
            ruleCategoryModel
        );

        return new InheritedRuleCategoryResponseModel(rules, properties);
    }

    private List<InheritedRuleResponseModel> computeInheritedRules(
        UnitRuleModel unitRuleModel,
        Map<String, UnitInheritedRulesResponseModel> unitInheritedRulesByUnitIdMap,
        String ruleCategory,
        RuleCategoryModel ruleCategoryModel
    ) {
        List<InheritedRuleResponseModel> localRules = computeLocalRules(unitRuleModel, ruleCategoryModel);

        List<InheritedRuleResponseModel> inheritedRules = getInheritedRules(
            unitRuleModel,
            unitInheritedRulesByUnitIdMap,
            ruleCategory,
            ruleCategoryModel
        );

        return union(localRules, inheritedRules);
    }

    private List<InheritedRuleResponseModel> computeLocalRules(
        UnitRuleModel unitRuleModel,
        RuleCategoryModel ruleCategoryModel
    ) {
        if (ruleCategoryModel == null) {
            return Collections.emptyList();
        }

        return ruleCategoryModel
            .getRules()
            .stream()
            .map(
                ruleModel ->
                    new InheritedRuleResponseModel(
                        unitRuleModel.getId(),
                        unitRuleModel.getOriginatingAgency(),
                        singletonList(singletonList(unitRuleModel.getId())),
                        ruleModel.getRule(),
                        ruleModel.getRuleAttributes()
                    )
            )
            .collect(Collectors.toList());
    }

    private List<InheritedRuleResponseModel> getInheritedRules(
        UnitRuleModel unitRuleModel,
        Map<String, UnitInheritedRulesResponseModel> unitInheritedRulesByUnitIdMap,
        String ruleCategory,
        RuleCategoryModel ruleCategoryModel
    ) {
        if (ruleCategoryModel != null && ruleCategoryModel.isPreventInheritance()) {
            // Nothing to inherit from parents
            return Collections.emptyList();
        }

        Set<String> filteredRuleIds = new HashSet<>();
        if (ruleCategoryModel != null) {
            // Do not inherit locally redefined rules
            if (ruleCategoryModel.getRules() != null) {
                ruleCategoryModel.getRules().stream().map(RuleModel::getRule).forEach(filteredRuleIds::add);
            }

            // Do not inherit explicitly excluded rules
            if (
                ruleCategoryModel.getInheritance() != null &&
                ruleCategoryModel.getInheritance().getPreventRulesId() != null
            ) {
                filteredRuleIds.addAll(ruleCategoryModel.getInheritance().getPreventRulesId());
            }
        }

        List<InheritedRuleResponseModel> inheritedRulesFromParents = new ArrayList<>();
        for (String parentUnitId : unitRuleModel.getUp()) {
            InheritedRuleCategoryResponseModel parentUnitInheritedRuleCategory = unitInheritedRulesByUnitIdMap
                .get(parentUnitId)
                .getRuleCategories()
                .get(ruleCategory);

            parentUnitInheritedRuleCategory
                .getRules()
                .stream()
                .filter(rule -> !filteredRuleIds.contains(rule.getRuleId()))
                .forEach(
                    rule ->
                        inheritedRulesFromParents.add(
                            new InheritedRuleResponseModel(
                                rule.getUnitId(),
                                rule.getOriginatingAgency(),
                                prependPaths(rule.getPaths(), unitRuleModel.getId()),
                                rule.getRuleId(),
                                rule.getExtendedRuleAttributes()
                            )
                        )
                );
        }

        return mergeInheritedRulesFromParents(inheritedRulesFromParents);
    }

    private List<List<String>> prependPaths(List<List<String>> paths, String unitId) {
        List<List<String>> result = new ArrayList<>();
        for (List<String> path : paths) {
            List<String> newPath = new ArrayList<>(path.size() + 1);
            newPath.add(unitId);
            newPath.addAll(path);
            result.add(newPath);
        }
        return result;
    }

    /**
     * Merges rules inherited from multiple paths (same origin unit id & same rule id).
     */
    private List<InheritedRuleResponseModel> mergeInheritedRulesFromParents(
        List<InheritedRuleResponseModel> inheritedRulesFromParents
    ) {
        Map<Pair<String, String>, List<InheritedRuleResponseModel>> inheritedRulesByUnitIdAndRuleId =
            inheritedRulesFromParents.stream().collect(groupingBy(rule -> Pair.of(rule.getUnitId(), rule.getRuleId())));

        List<InheritedRuleResponseModel> result = new ArrayList<>();
        for (List<InheritedRuleResponseModel> inheritedRulesToMerge : inheritedRulesByUnitIdAndRuleId.values()) {
            InheritedRuleResponseModel firstInheritedRule = inheritedRulesToMerge.get(0);

            InheritedRuleResponseModel inheritedRule;

            if (inheritedRulesToMerge.size() == 1) {
                inheritedRule = firstInheritedRule;
            } else {
                List<List<String>> mergedPaths = inheritedRulesToMerge
                    .stream()
                    .flatMap(r -> r.getPaths().stream())
                    .collect(Collectors.toList());

                inheritedRule = new InheritedRuleResponseModel(
                    firstInheritedRule.getUnitId(),
                    firstInheritedRule.getOriginatingAgency(),
                    mergedPaths,
                    firstInheritedRule.getRuleId(),
                    firstInheritedRule.getExtendedRuleAttributes()
                );
            }
            result.add(inheritedRule);
        }

        return result;
    }

    private List<InheritedPropertyResponseModel> computeInheritedProperties(
        UnitRuleModel unitRuleModel,
        Map<String, UnitInheritedRulesResponseModel> unitInheritedRulesByUnitIdMap,
        String ruleCategory,
        RuleCategoryModel ruleCategoryModel
    ) {
        List<InheritedPropertyResponseModel> localProperties = computeLocalProperties(unitRuleModel, ruleCategoryModel);

        List<InheritedPropertyResponseModel> inheritedProperties = getInheritedProperties(
            unitRuleModel,
            unitInheritedRulesByUnitIdMap,
            ruleCategory,
            ruleCategoryModel
        );

        List<InheritedPropertyResponseModel> explicitlyDeclaredProperties = union(localProperties, inheritedProperties);

        List<InheritedPropertyResponseModel> implicitProperties = computeImplicitProperties(
            ruleCategory,
            unitRuleModel,
            explicitlyDeclaredProperties
        );

        // Combine implicit properties with explicitly declared properties
        // Implicit properties redefine similar inherited properties (same PropertyName) from a different Originating Agency

        Set<String> implicitPropertyNames = implicitProperties
            .stream()
            .map(InheritedPropertyResponseModel::getPropertyName)
            .collect(Collectors.toSet());

        return union(
            implicitProperties,
            explicitlyDeclaredProperties
                .stream()
                .filter(p -> !implicitPropertyNames.contains(p.getPropertyName()))
                .collect(Collectors.toList())
        );
    }

    private List<InheritedPropertyResponseModel> computeLocalProperties(
        UnitRuleModel unitRuleModel,
        RuleCategoryModel ruleCategoryModel
    ) {
        if (ruleCategoryModel == null) {
            return Collections.emptyList();
        }

        return ruleCategoryModel
            .getProperties()
            .entrySet()
            .stream()
            .map(
                property ->
                    new InheritedPropertyResponseModel(
                        unitRuleModel.getId(),
                        unitRuleModel.getOriginatingAgency(),
                        singletonList(singletonList(unitRuleModel.getId())),
                        property.getKey(),
                        property.getValue()
                    )
            )
            .collect(Collectors.toList());
    }

    private List<InheritedPropertyResponseModel> getInheritedProperties(
        UnitRuleModel unitRuleModel,
        Map<String, UnitInheritedRulesResponseModel> unitInheritedRulesByUnitIdMap,
        String ruleCategory,
        RuleCategoryModel ruleCategoryModel
    ) {
        if (ruleCategoryModel != null && ruleCategoryModel.isPreventInheritance()) {
            // Nothing to inherit from parents
            return Collections.emptyList();
        }

        Set<String> filteredProperties = new HashSet<>();

        // Do not inherit locally redefined properties
        if (ruleCategoryModel != null && ruleCategoryModel.getProperties() != null) {
            filteredProperties.addAll(ruleCategoryModel.getProperties().keySet());
        }

        List<InheritedPropertyResponseModel> inheritedPropertiesFromParents = new ArrayList<>();
        for (String parentUnitId : unitRuleModel.getUp()) {
            InheritedRuleCategoryResponseModel parentUnitInheritedRuleCategory = unitInheritedRulesByUnitIdMap
                .get(parentUnitId)
                .getRuleCategories()
                .get(ruleCategory);

            parentUnitInheritedRuleCategory
                .getProperties()
                .stream()
                .filter(property -> !filteredProperties.contains(property.getPropertyName()))
                .forEach(
                    property ->
                        inheritedPropertiesFromParents.add(
                            new InheritedPropertyResponseModel(
                                property.getUnitId(),
                                property.getOriginatingAgency(),
                                prependPaths(property.getPaths(), unitRuleModel.getId()),
                                property.getPropertyName(),
                                property.getPropertyValue()
                            )
                        )
                );
        }

        return mergeInheritedPropertiesFromParents(inheritedPropertiesFromParents);
    }

    /**
     * Merges properties inherited from multiple paths (same origin unit id & same property name).
     */
    private List<InheritedPropertyResponseModel> mergeInheritedPropertiesFromParents(
        List<InheritedPropertyResponseModel> inheritedPropertiesFromParents
    ) {
        Map<Pair<String, String>, List<InheritedPropertyResponseModel>> inheritedPropertiesByUnitIdAndPropertyName =
            inheritedPropertiesFromParents
                .stream()
                .collect(groupingBy(property -> Pair.of(property.getUnitId(), property.getPropertyName())));

        List<InheritedPropertyResponseModel> result = new ArrayList<>();
        for (List<
            InheritedPropertyResponseModel
        > inheritedPropertiesToMerge : inheritedPropertiesByUnitIdAndPropertyName.values()) {
            InheritedPropertyResponseModel firstInheritedProperty = inheritedPropertiesToMerge.get(0);

            InheritedPropertyResponseModel inheritedProperty;

            if (inheritedPropertiesToMerge.size() == 1) {
                inheritedProperty = firstInheritedProperty;
            } else {
                List<List<String>> mergedPaths = inheritedPropertiesToMerge
                    .stream()
                    .flatMap(r -> r.getPaths().stream())
                    .collect(Collectors.toList());

                inheritedProperty = new InheritedPropertyResponseModel(
                    firstInheritedProperty.getUnitId(),
                    firstInheritedProperty.getOriginatingAgency(),
                    mergedPaths,
                    firstInheritedProperty.getPropertyName(),
                    firstInheritedProperty.getPropertyValue()
                );
            }

            result.add(inheritedProperty);
        }

        return result;
    }

    private List<InheritedPropertyResponseModel> computeImplicitProperties(
        String ruleCategory,
        UnitRuleModel unitRuleModel,
        List<InheritedPropertyResponseModel> explicitlyDeclaredProperties
    ) {
        // HOLDING_UNIT cannot declare rules & properties.
        boolean isHoldingUnit = unitRuleModel.getOriginatingAgency() == null;
        if (isHoldingUnit) {
            return emptyList();
        }

        Set<String> explicitlyDefinedPropertyNames = explicitlyDeclaredProperties
            .stream()
            .filter(p -> unitRuleModel.getOriginatingAgency().equals(p.getOriginatingAgency()))
            .map(InheritedPropertyResponseModel::getPropertyName)
            .collect(toSet());

        // For now, implicit rules are limited to AppraisalRule category & "Keep" as default value for "FinalAction" property.
        if (
            ruleCategory.equals(VitamConstants.TAG_RULE_APPRAISAL) &&
            !explicitlyDefinedPropertyNames.contains(RuleCategoryModel.FINAL_ACTION)
        ) {
            // Add an implicit property
            return singletonList(
                new InheritedPropertyResponseModel(
                    unitRuleModel.getId(),
                    unitRuleModel.getOriginatingAgency(),
                    singletonList(singletonList(unitRuleModel.getId())),
                    RuleCategoryModel.FINAL_ACTION,
                    VitamConstants.AppraisalRuleFinalAction.KEEP.value()
                )
            );
        }

        return emptyList();
    }

    private List<InheritedPropertyResponseModel> computeInheritedGlobalProperties(
        UnitRuleModel unitRuleModel,
        Map<String, UnitInheritedRulesResponseModel> unitInheritedRulesByUnitIdMap
    ) {
        Map<String, Object> globalProperties = new HashMap<>();
        if (unitRuleModel.getManagementModel().isNeedAuthorization() != null) {
            globalProperties.put(
                SedaConstants.TAG_RULE_NEED_AUTHORISATION,
                unitRuleModel.getManagementModel().isNeedAuthorization()
            );
        }
        List<InheritedPropertyResponseModel> localProperties = computeLocalGlobalProperties(
            unitRuleModel,
            globalProperties
        );

        List<InheritedPropertyResponseModel> inheritedProperties = getInheritedGlobalProperties(
            unitRuleModel,
            unitInheritedRulesByUnitIdMap,
            globalProperties
        );
        return union(localProperties, inheritedProperties);
    }

    private List<InheritedPropertyResponseModel> computeLocalGlobalProperties(
        UnitRuleModel unitRuleModel,
        Map<String, Object> globalProperties
    ) {
        if (globalProperties == null) {
            return Collections.emptyList();
        }
        return globalProperties
            .entrySet()
            .stream()
            .map(
                globalProperty ->
                    new InheritedPropertyResponseModel(
                        unitRuleModel.getId(),
                        unitRuleModel.getOriginatingAgency(),
                        singletonList(singletonList(unitRuleModel.getId())),
                        globalProperty.getKey(),
                        globalProperty.getValue()
                    )
            )
            .collect(Collectors.toList());
    }

    private List<InheritedPropertyResponseModel> getInheritedGlobalProperties(
        UnitRuleModel unitRuleModel,
        Map<String, UnitInheritedRulesResponseModel> unitInheritedRulesByUnitIdMap,
        Map<String, Object> globalProperties
    ) {
        Set<String> filteredProperties = new HashSet<>();

        // Do not inherit locally redefined properties
        if (globalProperties != null && globalProperties.get(SedaConstants.TAG_RULE_NEED_AUTHORISATION) != null) {
            filteredProperties.addAll(globalProperties.keySet());
        }

        List<InheritedPropertyResponseModel> inheritedPropertiesFromParents = new ArrayList<>();
        for (String parentUnitId : unitRuleModel.getUp()) {
            List<InheritedPropertyResponseModel> parentUnitInheritedGlobalProperties = unitInheritedRulesByUnitIdMap
                .get(parentUnitId)
                .getGlobalProperties();

            parentUnitInheritedGlobalProperties
                .stream()
                .filter(property -> !filteredProperties.contains(property.getPropertyName()))
                .forEach(
                    property ->
                        inheritedPropertiesFromParents.add(
                            new InheritedPropertyResponseModel(
                                property.getUnitId(),
                                property.getOriginatingAgency(),
                                prependPaths(property.getPaths(), unitRuleModel.getId()),
                                property.getPropertyName(),
                                property.getPropertyValue()
                            )
                        )
                );
        }

        return mergeInheritedPropertiesFromParents(inheritedPropertiesFromParents);
    }
}
