package fr.gouv.vitam.batch.report.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PreservationStatsModel {
    @JsonProperty("nbUnits")
    private int nbUnits;

    @JsonProperty("nbObjectGroups")
    private int nbObjectGroups;

    @JsonProperty("nbStatusKos")
    private int nbStatusKos;

    @JsonProperty("nbActionAnaylses")
    private int nbActionsAnaylse;

    @JsonProperty("nbActionGenerates")
    private int nbActionsGenerate;

    @JsonProperty("nbActionIdentifys")
    private int nbActionsIdentify;

    @JsonProperty("nbActionExtracts")
    private int nbActionsExtract;

    @JsonProperty("nbAnalyseValids")
    private int nbAnalysesValid;

    @JsonProperty("nbAnalyseNotValids")
    private int nbAnalysesNotValid;

    @JsonProperty("nbAnalyseWrongFormats")
    private int nbAnalysesWrongFormat;

    public PreservationStatsModel(int nbUnits, int nbObjectGroups, int nbStatusKos, int nbActionsAnaylse, int nbActionsGenerate, int nbActionsIdentify, int nbActionsExtract, int nbAnalysesValid, int nbAnalysesNotValid, int nbAnalysesWrongFormat) {
        this.nbUnits = nbUnits;
        this.nbObjectGroups = nbObjectGroups;
        this.nbStatusKos = nbStatusKos;
        this.nbActionsAnaylse = nbActionsAnaylse;
        this.nbActionsGenerate = nbActionsGenerate;
        this.nbActionsIdentify = nbActionsIdentify;
        this.nbActionsExtract = nbActionsExtract;
        this.nbAnalysesValid = nbAnalysesValid;
        this.nbAnalysesNotValid = nbAnalysesNotValid;
        this.nbAnalysesWrongFormat = nbAnalysesWrongFormat;
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

    public int getNbActionsAnaylse() {
        return nbActionsAnaylse;
    }

    public void setNbActionsAnaylse(int nbActionsAnaylse) {
        this.nbActionsAnaylse = nbActionsAnaylse;
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

    public int getNbAnalysesValid() {
        return nbAnalysesValid;
    }

    public void setNbAnalysesValid(int nbAnalysesValid) {
        this.nbAnalysesValid = nbAnalysesValid;
    }

    public int getNbAnalysesNotValid() {
        return nbAnalysesNotValid;
    }

    public void setNbAnalysesNotValid(int nbAnalysesNotValid) {
        this.nbAnalysesNotValid = nbAnalysesNotValid;
    }

    public int getNbAnalysesWrongFormat() {
        return nbAnalysesWrongFormat;
    }

    public void setNbAnalysesWrongFormat(int nbAnalysesWrongFormat) {
        this.nbAnalysesWrongFormat = nbAnalysesWrongFormat;
    }
}
