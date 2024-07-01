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
package fr.gouv.vitam.functional.administration.core.griffin;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.functional.administration.core.format.model.FunctionalOperationModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GriffinReport {

    @JsonProperty("Operation")
    private FunctionalOperationModel operation;

    @JsonProperty("StatusCode")
    private StatusCode statusCode;

    @JsonProperty("PreviousGriffinsVersion")
    private String previousGriffinsVersion;

    @JsonProperty("PreviousGriffinsCreationDate")
    private String previousGriffinsCreationDate;

    @JsonProperty("NewGriffinsVersion")
    private String newGriffinsVersion;

    @JsonProperty("NewGriffinsCreationDate")
    private String newGriffinsCreationDate;

    @JsonProperty("RemovedIdentifiers")
    private Set<String> removedIdentifiers;

    @JsonProperty("AddedIdentifiers")
    private Set<String> addedIdentifiers;

    @JsonProperty("UpdatedIdentifiers")
    private Map<String, List<String>> updatedIdentifiers;

    @JsonProperty("Warnings")
    private List<String> warnings = new ArrayList<>();

    public GriffinReport() {
        // empty constructor
    }

    public GriffinReport(List<String> warnings) {
        this.warnings = warnings;
    }

    public static GriffinReport onlyWarning(GriffinReport griffinReport) {
        return new GriffinReport(griffinReport.getWarnings());
    }

    public FunctionalOperationModel getOperation() {
        return operation;
    }

    public GriffinReport setOperation(FunctionalOperationModel operation) {
        this.operation = operation;
        return this;
    }

    public StatusCode getStatusCode() {
        return statusCode;
    }

    public GriffinReport setStatusCode(StatusCode statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public Set<String> getRemovedIdentifiers() {
        return removedIdentifiers;
    }

    void setRemovedIdentifiers(Set<String> removedIdentifiers) {
        this.removedIdentifiers = removedIdentifiers;
    }

    public void addRemovedIdentifiers(String identifier) {
        this.removedIdentifiers.add(identifier);
    }

    public Set<String> getAddedIdentifiers() {
        return addedIdentifiers;
    }

    void setAddedIdentifiers(Set<String> addedIdentifiers) {
        this.addedIdentifiers = addedIdentifiers;
    }

    public void addAddedIdentifier(String identifier) {
        this.addedIdentifiers.add(identifier);
    }

    public Map<String, List<String>> getUpdatedIdentifiers() {
        return updatedIdentifiers;
    }

    public void setUpdatedIdentifiers(Map<String, List<String>> updatedIdentifiers) {
        this.updatedIdentifiers = updatedIdentifiers;
    }

    void addUpdatedIdentifiers(String identifier, List<String> diffs) {
        if (this.updatedIdentifiers == null) {
            this.updatedIdentifiers = new HashMap<>();
        }
        this.updatedIdentifiers.put(identifier, diffs);
    }

    List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    void addWarning(String message) {
        warnings.add(message);
    }

    String getPreviousGriffinsVersion() {
        return previousGriffinsVersion;
    }

    void setPreviousGriffinsVersion(String previousGriffinsVersion) {
        this.previousGriffinsVersion = previousGriffinsVersion;
    }

    String getPreviousGriffinsCreationDate() {
        return previousGriffinsCreationDate;
    }

    void setPreviousGriffinsCreationDate(String previousGriffinsCreationDate) {
        this.previousGriffinsCreationDate = previousGriffinsCreationDate;
    }

    String getNewGriffinsVersion() {
        return newGriffinsVersion;
    }

    void setNewGriffinsVersion(String newGriffinsVersion) {
        this.newGriffinsVersion = newGriffinsVersion;
    }

    String getNewGriffinsCreationDate() {
        return newGriffinsCreationDate;
    }

    public void setNewGriffinsCreationDate(String newGriffinsCreationDate) {
        this.newGriffinsCreationDate = newGriffinsCreationDate;
    }
}
