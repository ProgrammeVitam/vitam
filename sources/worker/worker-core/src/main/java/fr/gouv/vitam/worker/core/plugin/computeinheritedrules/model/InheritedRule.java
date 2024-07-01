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
package fr.gouv.vitam.worker.core.plugin.computeinheritedrules.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.common.model.unit.ComputedInheritedRuleModel;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.SetUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * InheritedRule
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class InheritedRule {

    private static final String MAX_END_DATE = "MaxEndDate";
    private static final String END_DATES = "EndDates";
    private static final String RULES = "Rules";
    private static final String INHERITANCE_ORIGIN = "InheritanceOrigin";
    private static final String INHERITED_RULE_IDS = "InheritedRuleIds";

    @JsonProperty(MAX_END_DATE)
    private LocalDate maxEndDate;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty(INHERITANCE_ORIGIN)
    private RuleCategoryInheritanceOrigin ruleCategoryInheritanceOrigin;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonProperty(INHERITED_RULE_IDS)
    private Set<String> inheritedRuleIds;

    @JsonProperty(END_DATES)
    private Map<String, LocalDate> ruleIdToRule = new HashMap<>();

    @JsonProperty(RULES)
    private List<ComputedInheritedRuleModel> rules = new ArrayList<>();

    public InheritedRule() {}

    public InheritedRule(
        LocalDate maxEndDate,
        Map<String, LocalDate> ruleIdToRule,
        List<ComputedInheritedRuleModel> rules,
        RuleCategoryInheritanceOrigin ruleCategoryInheritanceOrigin,
        Set<String> inheritedRuleIds
    ) {
        this.maxEndDate = maxEndDate;
        this.ruleIdToRule = MapUtils.emptyIfNull(ruleIdToRule);
        this.rules = ListUtils.emptyIfNull(rules);
        this.inheritedRuleIds = SetUtils.emptyIfNull(inheritedRuleIds);
        this.ruleCategoryInheritanceOrigin = ruleCategoryInheritanceOrigin;
    }

    public InheritedRule(LocalDate maxEndDate) {
        this.maxEndDate = maxEndDate;
    }

    public LocalDate getMaxEndDate() {
        return maxEndDate;
    }

    public void setMaxEndDate(LocalDate maxEndDate) {
        this.maxEndDate = maxEndDate;
    }

    List<String> parsePropertiesByName(String propertyName, Properties properties) {
        PropertyValue property = properties.getPropertyValue(propertyName);
        if (property != null) {
            return property
                .getValues()
                .stream()
                .map(object -> Objects.toString(object, null))
                .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    public Map<String, LocalDate> getRuleIdToRule() {
        return ruleIdToRule;
    }

    public void setRuleIdToRule(String ruleId, LocalDate ruleMaxEndDate) {
        this.ruleIdToRule.put(ruleId, ruleMaxEndDate);
    }

    public List<ComputedInheritedRuleModel> getRules() {
        return rules;
    }

    public void setRules(List<ComputedInheritedRuleModel> rules) {
        this.rules = rules;
    }

    public RuleCategoryInheritanceOrigin getRuleCategoryInheritanceOrigin() {
        return ruleCategoryInheritanceOrigin;
    }

    public InheritedRule setRuleCategoryInheritanceOrigin(RuleCategoryInheritanceOrigin ruleCategoryInheritanceOrigin) {
        this.ruleCategoryInheritanceOrigin = ruleCategoryInheritanceOrigin;
        return this;
    }

    public Set<String> getInheritedRuleIds() {
        return inheritedRuleIds;
    }

    public InheritedRule setInheritedRuleIds(Set<String> inheritedRuleIds) {
        this.inheritedRuleIds = inheritedRuleIds;
        return this;
    }
}
