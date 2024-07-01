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
package fr.gouv.vitam.worker.core.plugin.reclassification.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Set;

public class ReclassificationEventDetails {

    @JsonProperty("error")
    private String error;

    @JsonProperty("missingOrForbiddenUnits")
    private Set<String> missingOrForbiddenUnits;

    @JsonProperty("notFoundUnits")
    private Set<String> notFoundUnits;

    @JsonProperty("illegalUnitTypeAttachments")
    private List<IllegalUnitTypeAttachment> illegalUnitTypeAttachments;

    @JsonProperty("unitsWithCycles")
    private Set<String> unitsWithCycles;

    @JsonProperty("addParents")
    private Set<String> addedParents;

    @JsonProperty("removedParents")
    private Set<String> removedParents;

    @JsonProperty("unitsBlockedByHoldRules")
    private Set<String> unitsBlockedByHoldRules;

    public ReclassificationEventDetails() {
        // Empty constructor for deserialization
    }

    public String getError() {
        return error;
    }

    public ReclassificationEventDetails setError(String error) {
        this.error = error;
        return this;
    }

    public Set<String> getMissingOrForbiddenUnits() {
        return missingOrForbiddenUnits;
    }

    public ReclassificationEventDetails setMissingOrForbiddenUnits(Set<String> missingOrForbiddenUnits) {
        this.missingOrForbiddenUnits = missingOrForbiddenUnits;
        return this;
    }

    public Set<String> getNotFoundUnits() {
        return notFoundUnits;
    }

    public ReclassificationEventDetails setNotFoundUnits(Set<String> notFoundUnits) {
        this.notFoundUnits = notFoundUnits;
        return this;
    }

    public List<IllegalUnitTypeAttachment> getIllegalUnitTypeAttachments() {
        return illegalUnitTypeAttachments;
    }

    public ReclassificationEventDetails setIllegalUnitTypeAttachments(
        List<IllegalUnitTypeAttachment> illegalUnitTypeAttachments
    ) {
        this.illegalUnitTypeAttachments = illegalUnitTypeAttachments;
        return this;
    }

    public Set<String> getUnitsWithCycles() {
        return unitsWithCycles;
    }

    public ReclassificationEventDetails setUnitsWithCycles(Set<String> unitsWithCycles) {
        this.unitsWithCycles = unitsWithCycles;
        return this;
    }

    public Set<String> getAddedParents() {
        return addedParents;
    }

    public ReclassificationEventDetails setAddedParents(Set<String> addedParents) {
        this.addedParents = addedParents;
        return this;
    }

    public Set<String> getRemovedParents() {
        return removedParents;
    }

    public ReclassificationEventDetails setRemovedParents(Set<String> removedParents) {
        this.removedParents = removedParents;
        return this;
    }

    public Set<String> getUnitsBlockedByHoldRules() {
        return unitsBlockedByHoldRules;
    }

    public ReclassificationEventDetails setUnitsBlockedByHoldRules(Set<String> unitsBlockedByHoldRules) {
        this.unitsBlockedByHoldRules = unitsBlockedByHoldRules;
        return this;
    }
}
