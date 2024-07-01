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
package fr.gouv.vitam.worker.core.plugin.preservation.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.common.model.administration.preservation.ActionPreservation;

import java.util.List;
import java.util.Set;

public class PreservationDistributionLine {

    @JsonProperty("id")
    private String id;

    @JsonProperty("formatId")
    private String formatId;

    @JsonProperty("filename")
    private String filename;

    @JsonProperty("actions")
    private List<ActionPreservation> actionPreservationList;

    @JsonProperty("unitId")
    private String unitId;

    @JsonProperty("objectId")
    private String objectId;

    @JsonProperty("debug")
    private boolean debug;

    @JsonProperty("timeout")
    private int timeout;

    @JsonProperty("targetUse")
    private String targetUse;

    @JsonProperty("sourceUse")
    private String sourceUse;

    @JsonProperty("sourceStrategy")
    private String sourceStrategy;

    @JsonProperty("griffinId")
    private String griffinId;

    @JsonProperty("preservationScenarioId")
    private String scenarioId;

    @JsonProperty("griffinIdentifier")
    private String griffinIdentifier;

    @JsonProperty("unitsForExtractionAU")
    private Set<String> unitsForExtractionAU;

    public PreservationDistributionLine() {}

    public PreservationDistributionLine(
        String formatId,
        String filename,
        List<ActionPreservation> actionPreservationList,
        String unitId,
        String griffinId,
        String objectId,
        boolean debug,
        int timeout,
        String id,
        String targetUse,
        String sourceUse,
        String sourceStrategy,
        String scenarioId,
        String griffinIdentifier,
        Set<String> unitsForExtractionAU
    ) {
        this.id = id;
        this.formatId = formatId;
        this.filename = filename;
        this.actionPreservationList = actionPreservationList;
        this.unitId = unitId;
        this.objectId = objectId;
        this.debug = debug;
        this.timeout = timeout;
        this.targetUse = targetUse;
        this.sourceUse = sourceUse;
        this.sourceStrategy = sourceStrategy;
        this.griffinId = griffinId;
        this.scenarioId = scenarioId;
        this.griffinIdentifier = griffinIdentifier;
        this.unitsForExtractionAU = unitsForExtractionAU;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFormatId() {
        return formatId;
    }

    public void setFormatId(String formatId) {
        this.formatId = formatId;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public List<ActionPreservation> getActionPreservationList() {
        return actionPreservationList;
    }

    public void setActionPreservationList(List<ActionPreservation> actionPreservationList) {
        this.actionPreservationList = actionPreservationList;
    }

    public String getUnitId() {
        return unitId;
    }

    public void setUnitId(String unitId) {
        this.unitId = unitId;
    }

    public String getGriffinId() {
        return griffinId;
    }

    public void setGriffinId(String griffinId) {
        this.griffinId = griffinId;
    }

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String getTargetUse() {
        return targetUse;
    }

    public void setTargetUse(String targetUse) {
        this.targetUse = targetUse;
    }

    public String getSourceUse() {
        return sourceUse;
    }

    public void setSourceUse(String sourceUse) {
        this.sourceUse = sourceUse;
    }

    public String getSourceStrategy() {
        return sourceStrategy;
    }

    public void setSourceStrategy(String sourceStrategy) {
        this.sourceStrategy = sourceStrategy;
    }

    public String getScenarioId() {
        return scenarioId;
    }

    public void setScenarioId(String scenarioId) {
        this.scenarioId = scenarioId;
    }

    public String getGriffinIdentifier() {
        return griffinIdentifier;
    }

    public void setGriffinIdentifier(String griffinIdentifier) {
        this.griffinIdentifier = griffinIdentifier;
    }

    public Set<String> getUnitsForExtractionAU() {
        return unitsForExtractionAU;
    }

    public void setUnitsForExtractionAU(Set<String> unitsForExtractionAU) {
        this.unitsForExtractionAU = unitsForExtractionAU;
    }
}
