package fr.gouv.vitam.ihmrecette.appserver.populate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties({"_nbc", "offerIds"})
public class StorageModel {

    @JsonProperty("strategyId")
    private String strategyId;

    // use only for jackson
    public StorageModel() {
    }

    public StorageModel(String strategyId) {
        this.strategyId = strategyId;
    }

    public String getStrategyId() {
        return strategyId;
    }

}
