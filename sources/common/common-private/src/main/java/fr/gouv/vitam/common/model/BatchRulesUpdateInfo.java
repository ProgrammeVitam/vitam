package fr.gouv.vitam.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.common.model.massupdate.RuleActions;

import java.util.List;
import java.util.Map;

public class BatchRulesUpdateInfo {

    @JsonProperty("UnitIds")
    private List<String> unitIds;

    @JsonProperty("RuleActions")
    private RuleActions ruleActions;

    @JsonProperty("RulesToDurationData")
    private Map<String, DurationData> rulesToDurationData;

    public BatchRulesUpdateInfo() {
        // Empty constructor for Jackson
    }

    public BatchRulesUpdateInfo(List<String> unitIds, RuleActions ruleActions,
        Map<String, DurationData> rulesToDurationData) {
        this.unitIds = unitIds;
        this.ruleActions = ruleActions;
        this.rulesToDurationData = rulesToDurationData;
    }

    public List<String> getUnitIds() {
        return unitIds;
    }

    public BatchRulesUpdateInfo setUnitIds(List<String> unitIds) {
        this.unitIds = unitIds;
        return this;
    }

    public RuleActions getRuleActions() {
        return ruleActions;
    }

    public BatchRulesUpdateInfo setRuleActions(RuleActions ruleActions) {
        this.ruleActions = ruleActions;
        return this;
    }

    public Map<String, DurationData> getRulesToDurationData() {
        return rulesToDurationData;
    }

    public BatchRulesUpdateInfo setRulesToDurationData(
        Map<String, DurationData> rulesToDurationData) {
        this.rulesToDurationData = rulesToDurationData;
        return this;
    }
}
