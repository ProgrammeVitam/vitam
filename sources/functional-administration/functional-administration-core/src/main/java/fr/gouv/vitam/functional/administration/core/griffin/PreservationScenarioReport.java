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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PreservationScenarioReport {

    @JsonProperty("Operation")
    private FunctionalOperationModel operation;

    @JsonProperty("StatusCode")
    private StatusCode statusCode;

    @JsonProperty("PreviousScenariosCreationDate")
    private String previousScenariosCreationDate;

    @JsonProperty("NewScenariosVersion")
    private String newScenariosVersion;

    @JsonProperty("NewScenariosCreationDate")
    private String newScenariosCreationDate;

    @JsonProperty("RemovedIdentifiers")
    private Set<String> removedIdentifiers = new HashSet<>();

    @JsonProperty("AddedIdentifiers")
    private Set<String> addedIdentifiers = new HashSet<>();

    @JsonProperty("UpdatedIdentifiers")
    private Map<String, List<String>> updatedIdentifiers = new HashMap<>();

    @JsonProperty("Warnings")
    private List<String> warnings = new ArrayList<>();

    public PreservationScenarioReport() {
        // empty constructor
    }

    public FunctionalOperationModel getOperation() {
        return operation;
    }

    public PreservationScenarioReport setOperation(FunctionalOperationModel operation) {
        this.operation = operation;
        return this;
    }

    public StatusCode getStatusCode() {
        return statusCode;
    }

    public PreservationScenarioReport setStatusCode(StatusCode statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public Set<String> getRemovedIdentifiers() {
        return removedIdentifiers;
    }

    public void addRemovedIdentifiers(String identifier) {
        this.removedIdentifiers.add(identifier);
    }

    public Set<String> getAddedIdentifiers() {
        return addedIdentifiers;
    }

    public void addAddedIdentifier(String identifier) {
        this.addedIdentifiers.add(identifier);
    }

    public Map<String, List<String>> getUpdatedIdentifiers() {
        return updatedIdentifiers;
    }

    void addUpdatedIdentifiers(String identifier, List<String> diffs) {
        this.updatedIdentifiers.put(identifier, diffs);
    }

    List<String> getWarnings() {
        return warnings;
    }

    void addWarning(String message) {
        warnings.add(message);
    }

    String getPreviousScenariosCreationDate() {
        return previousScenariosCreationDate;
    }

    public void setPreviousScenariosCreationDate(String previousScenariosCreationDate) {
        this.previousScenariosCreationDate = previousScenariosCreationDate;
    }

    String getNewScenariosVersion() {
        return newScenariosVersion;
    }

    public void setNewScenariosVersion(String newScenariosVersion) {
        this.newScenariosVersion = newScenariosVersion;
    }

    String getNewScenariosCreationDate() {
        return newScenariosCreationDate;
    }

    public void setNewScenariosCreationDate(String newScenariosCreationDate) {
        this.newScenariosCreationDate = newScenariosCreationDate;
    }

    void setRemovedIdentifiers(Set<String> removedIdentifiers) {
        this.removedIdentifiers = removedIdentifiers;
    }

    void setAddedIdentifiers(Set<String> addedIdentifiers) {
        this.addedIdentifiers = addedIdentifiers;
    }

    public void setUpdatedIdentifiers(Map<String, List<String>> updatedIdentifiers) {
        this.updatedIdentifiers = updatedIdentifiers;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }
}
