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

    public PreservationStatsModel(
        int nbUnits,
        int nbObjectGroups,
        int nbStatusKos,
        int nbActionsAnalyse,
        int nbActionsGenerate,
        int nbActionsIdentify,
        int nbActionsExtract,
        Map<String, Integer> analyseResults,
        int nbStatusWarning
    ) {
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
