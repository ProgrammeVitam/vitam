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
package fr.gouv.vitam.functional.administration.core.format.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.common.model.StatusCode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FormatImportReport {

    @JsonProperty("Operation")
    private FunctionalOperationModel operation;

    @JsonProperty("StatusCode")
    private StatusCode statusCode;

    @JsonProperty("PreviousPronomVersion")
    private String previousPronomVersion;

    @JsonProperty("PreviousPronomCreationDate")
    private String previousPronomCreationDate;

    @JsonProperty("NewPronomVersion")
    private String newPronomVersion;

    @JsonProperty("NewPronomCreationDate")
    private String newPronomCreationDate;

    @JsonProperty("RemovedPUIDs")
    private Set<String> removedPuids = new HashSet<>();

    @JsonProperty("AddedPUIDs")
    private Set<String> addedPuids = new HashSet<>();

    @JsonProperty("UpdatedPUIDs")
    private Map<String, List<String>> updatedPuids = new HashMap<>();

    @JsonProperty("Warnings")
    private List<String> warnings = new ArrayList<>();

    public FormatImportReport() {}

    public FunctionalOperationModel getOperation() {
        return operation;
    }

    public FormatImportReport setOperation(FunctionalOperationModel operation) {
        this.operation = operation;
        return this;
    }

    public StatusCode getStatusCode() {
        return statusCode;
    }

    public FormatImportReport setStatusCode(StatusCode statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public String getPreviousPronomVersion() {
        return previousPronomVersion;
    }

    public FormatImportReport setPreviousPronomVersion(String previousPronomVersion) {
        this.previousPronomVersion = previousPronomVersion;
        return this;
    }

    public String getPreviousPronomCreationDate() {
        return previousPronomCreationDate;
    }

    public FormatImportReport setPreviousPronomCreationDate(String previousPronomCreationDate) {
        this.previousPronomCreationDate = previousPronomCreationDate;
        return this;
    }

    public String getNewPronomVersion() {
        return newPronomVersion;
    }

    public FormatImportReport setNewPronomVersion(String newPronomVersion) {
        this.newPronomVersion = newPronomVersion;
        return this;
    }

    public String getNewPronomCreationDate() {
        return newPronomCreationDate;
    }

    public FormatImportReport setNewPronomCreationDate(String newPronomCreationDate) {
        this.newPronomCreationDate = newPronomCreationDate;
        return this;
    }

    public Set<String> getRemovedPuids() {
        return removedPuids;
    }

    public void addRemovedPuids(String puid) {
        this.removedPuids.add(puid);
    }

    public Set<String> getAddedPuids() {
        return addedPuids;
    }

    public void addAddedPuid(String puid) {
        this.addedPuids.add(puid);
    }

    public Map<String, List<String>> getUpdatedPuids() {
        return updatedPuids;
    }

    public void addUpdatedPuids(String puid, List<String> diffs) {
        this.updatedPuids.put(puid, diffs);
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void addWarning(String message) {
        warnings.add(message);
    }
}
