package fr.gouv.vitam.ihmrecette.appserver.populate;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StorageModel {

    @JsonProperty("_nbc")
    private int nbCopy;

    @JsonProperty("offerIds")
    private List<String> offerIds;

    @JsonProperty("strategyId")
    private String strategyId;

    // use only ofr jackson
    public StorageModel() {
    }

    public StorageModel(int nbCopy, String strategyId, List<String> offerIds) {
        this.nbCopy = nbCopy;
        this.strategyId = strategyId;
        this.offerIds = offerIds;
    }

    public int getNbCopy() {
        return nbCopy;
    }

    public List<String> getOfferIds() {
        return offerIds;
    }

    public void setOfferIds(List<String> offerIds) {
        this.offerIds = offerIds;
    }

    public String getStrategyId() {
        return strategyId;
    }

    public void addOfferId(String offerId) {
        offerIds.add(offerId);
    }

}
