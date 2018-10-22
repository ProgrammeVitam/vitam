package fr.gouv.vitam.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

public class BatchRulesUpdateInfo {
    @JsonProperty("QueryAction")
    private JsonNode queryAction;

    @JsonProperty("RulesToDurationData")
    private Map<String, DurationData> rulesToDurationData;

    public BatchRulesUpdateInfo(JsonNode queryAction,
        Map<String, DurationData> rulesToDurationData) {
        this.queryAction = queryAction;
        this.rulesToDurationData = rulesToDurationData;
    }

    public BatchRulesUpdateInfo() {
        // Empty constructor for Jackson
    }

    public JsonNode getQueryAction() {
        return queryAction;
    }

    public Map<String, DurationData> getRulesToDurationData() {
        return rulesToDurationData;
    }

    public void setQueryAction(JsonNode queryAction) {
        this.queryAction = queryAction;
    }

    public void setRulesToDurationData(
        Map<String, DurationData> rulesToDurationData) {
        this.rulesToDurationData = rulesToDurationData;
    }
}
