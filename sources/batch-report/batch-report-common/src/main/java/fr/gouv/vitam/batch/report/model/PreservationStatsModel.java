package fr.gouv.vitam.batch.report.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class PreservationStatsModel {
    @JsonProperty("nbUnits")
    private int nbUnits;

    @JsonProperty("nbObjectGroups")
    private int nbObjectGroups;

    @JsonProperty("nbStatusKo")
    private int nbStatusKos;

    @JsonProperty("nbStatusWarning")
    private int nbStatusWarning;

    @JsonProperty("nbActionAnalyses")
    private int nbActionsAnalyse;

    @JsonProperty("nbActionGenerates")
    private int nbActionsGenerate;

    @JsonProperty("nbActionIdentifys")
    private int nbActionsIdentify;

    @JsonProperty("nbActionExtracts")
    private int nbActionsExtract;

    @JsonProperty("analyseResults")
    private Map<String, Integer> analyseResults;

    public PreservationStatsModel(int nbUnits, int nbObjectGroups, int nbStatusKos, int nbActionsAnalyse, int nbActionsGenerate,
        int nbActionsIdentify, int nbActionsExtract, Map<String, Integer> analyseResults, int nbStatusWarning) {
        this.nbUnits = nbUnits;
        this.nbObjectGroups = nbObjectGroups;
        this.nbStatusKos = nbStatusKos;
        this.nbActionsAnalyse = nbActionsAnalyse;
        this.nbActionsGenerate = nbActionsGenerate;
        this.nbActionsIdentify = nbActionsIdentify;
        this.nbActionsExtract = nbActionsExtract;
        this.analyseResults = analyseResults;
        this.nbStatusWarning = nbStatusWarning;
    }

    public int getNbUnits() {
        return nbUnits;
    }

    public void setNbUnits(int nbUnits) {
        this.nbUnits = nbUnits;
    }

    public int getNbObjectGroups() {
        return nbObjectGroups;
    }

    public void setNbObjectGroups(int nbObjectGroups) {
        this.nbObjectGroups = nbObjectGroups;
    }

    public int getNbStatusKos() {
        return nbStatusKos;
    }

    public void setNbStatusKos(int nbStatusKos) {
        this.nbStatusKos = nbStatusKos;
    }

    public int getNbActionsAnalyse() {
        return nbActionsAnalyse;
    }

    public void setNbActionsAnalyse(int nbActionsAnalyse) {
        this.nbActionsAnalyse = nbActionsAnalyse;
    }

    public int getNbActionsGenerate() {
        return nbActionsGenerate;
    }

    public void setNbActionsGenerate(int nbActionsGenerate) {
        this.nbActionsGenerate = nbActionsGenerate;
    }

    public int getNbActionsIdentify() {
        return nbActionsIdentify;
    }

    public void setNbActionsIdentify(int nbActionsIdentify) {
        this.nbActionsIdentify = nbActionsIdentify;
    }

    public int getNbActionsExtract() {
        return nbActionsExtract;
    }

    public void setNbActionsExtract(int nbActionsExtract) {
        this.nbActionsExtract = nbActionsExtract;
    }

    public Map<String, Integer> getAnalyseResults() {
        return analyseResults;
    }

    public void setAnalyseResults(Map<String, Integer> analyseResults) {
        this.analyseResults = analyseResults;
    }

    public int getNbStatusWarning() {
        return nbStatusWarning;
    }

    public void setNbStatusWarning(int nbStatusWarning) {
        this.nbStatusWarning = nbStatusWarning;
    }
}
