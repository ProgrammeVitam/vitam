/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.model.massupdate;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RuleAction {

    @JsonProperty("OldRule")
    private String oldRule;
    
    @JsonProperty("Rule")
    private String rule;

    @JsonProperty("StartDate")
    private String startDate;

    /**
     * @deprecated unused / forbidden field. Do not delete (breaks public API)
     */
    @JsonProperty("EndDate")
    private String endDate;

    @JsonProperty("DeleteStartDate")
    private Boolean deleteStartDate;

    @JsonProperty("HoldEndDate")
    private String holdEndDate;

    @JsonProperty("DeleteHoldEndDate")
    private Boolean deleteHoldEndDate;

    @JsonProperty("HoldOwner")
    private String holdOwner;

    @JsonProperty("DeleteHoldOwner")
    private Boolean deleteHoldOwner;

    @JsonProperty("HoldReason")
    private String holdReason;

    @JsonProperty("DeleteHoldReason")
    private Boolean deleteHoldReason;

    @JsonProperty("HoldReassessingDate")
    private String holdReassessingDate;

    @JsonProperty("DeleteHoldReassessingDate")
    private Boolean deleteHoldReassessingDate;

    @JsonProperty("PreventRearrangement")
    private Boolean preventRearrangement;

    @JsonProperty("DeletePreventRearrangement")
    private Boolean deletePreventRearrangement;

    public String getOldRule() {
        return oldRule;
    }

    public RuleAction setOldRule(String oldRule) {
        this.oldRule = oldRule;
        return this;
    }

    public String getRule() {
        return rule;
    }

    public RuleAction setRule(String rule) {
        this.rule = rule;
        return this;
    }

    public String getStartDate() {
        return startDate;
    }

    public RuleAction setStartDate(String startDate) {
        this.startDate = startDate;
        return this;
    }

    /**
     * @deprecated unused / forbidden field. Do not delete (breaks public API)
     */
    public String getEndDate() {
        return endDate;
    }

    /**
     * @deprecated unused / forbidden field. Do not delete (breaks public API)
     */
    public RuleAction setEndDate(String endDate) {
        this.endDate = endDate;
        return this;
    }

    public Boolean isDeleteStartDate() {
        return deleteStartDate;
    }

    public RuleAction setDeleteStartDate(Boolean deleteStartDate) {
        this.deleteStartDate = deleteStartDate;
        return this;
    }

    public Boolean getDeleteStartDate() {
        return deleteStartDate;
    }

    public String getHoldEndDate() {
        return holdEndDate;
    }

    public RuleAction setHoldEndDate(String holdEndDate) {
        this.holdEndDate = holdEndDate;
        return this;
    }

    public Boolean getDeleteHoldEndDate() {
        return deleteHoldEndDate;
    }

    public RuleAction setDeleteHoldEndDate(Boolean deleteHoldEndDate) {
        this.deleteHoldEndDate = deleteHoldEndDate;
        return this;
    }

    public String getHoldOwner() {
        return holdOwner;
    }

    public RuleAction setHoldOwner(String holdOwner) {
        this.holdOwner = holdOwner;
        return this;
    }

    public Boolean getDeleteHoldOwner() {
        return deleteHoldOwner;
    }

    public RuleAction setDeleteHoldOwner(Boolean deleteHoldOwner) {
        this.deleteHoldOwner = deleteHoldOwner;
        return this;
    }

    public String getHoldReason() {
        return holdReason;
    }

    public RuleAction setHoldReason(String holdReason) {
        this.holdReason = holdReason;
        return this;
    }

    public Boolean getDeleteHoldReason() {
        return deleteHoldReason;
    }

    public RuleAction setDeleteHoldReason(Boolean deleteHoldReason) {
        this.deleteHoldReason = deleteHoldReason;
        return this;
    }

    public String getHoldReassessingDate() {
        return holdReassessingDate;
    }

    public RuleAction setHoldReassessingDate(String holdReassessingDate) {
        this.holdReassessingDate = holdReassessingDate;
        return this;
    }

    public Boolean getDeleteHoldReassessingDate() {
        return deleteHoldReassessingDate;
    }

    public RuleAction setDeleteHoldReassessingDate(Boolean deleteHoldReassessingDate) {
        this.deleteHoldReassessingDate = deleteHoldReassessingDate;
        return this;
    }

    public Boolean getPreventRearrangement() {
        return preventRearrangement;
    }

    public RuleAction setPreventRearrangement(Boolean preventRearrangement) {
        this.preventRearrangement = preventRearrangement;
        return this;
    }

    public Boolean getDeletePreventRearrangement() {
        return deletePreventRearrangement;
    }

    public RuleAction setDeletePreventRearrangement(Boolean deletePreventRearrangement) {
        this.deletePreventRearrangement = deletePreventRearrangement;
        return this;
    }
}
