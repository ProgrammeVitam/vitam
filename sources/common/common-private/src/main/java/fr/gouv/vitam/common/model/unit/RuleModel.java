/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.common.model.unit;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RuleModel {

    /**
     * rule id
     */
    @JsonProperty("Rule")
    private String rule;

    /**
     * start date
     */
    @JsonProperty("StartDate")
    private String startDate;

    /**
     * end date
     */
    @JsonProperty("EndDate")
    private String endDate;

    /**
     * hold end date
     */
    @JsonProperty("HoldEndDate")
    private String holdEndDate;

    /**
     * hold owner
     */
    @JsonProperty("HoldOwner")
    private String holdOwner;

    /**
     * hold reason
     */
    @JsonProperty("HoldReason")
    private String holdReason;

    /**
     * hold reassessing date
     */
    @JsonProperty("HoldReassessingDate")
    private String holdReassessingDate;

    /**
     * hold end date
     */
    @JsonProperty("PreventRearrangement")
    private Boolean preventRearrangement;

    public RuleModel() {
    }

    public RuleModel(String rule, String startDate) {
        this.rule = rule;
        this.startDate = startDate;
    }

    public String getRule() {
        return rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getHoldEndDate() {
        return holdEndDate;
    }

    public RuleModel setHoldEndDate(String holdEndDate) {
        this.holdEndDate = holdEndDate;
        return this;
    }

    public String getHoldOwner() {
        return holdOwner;
    }

    public RuleModel setHoldOwner(String holdOwner) {
        this.holdOwner = holdOwner;
        return this;
    }

    public String getHoldReason() {
        return holdReason;
    }

    public RuleModel setHoldReason(String holdReason) {
        this.holdReason = holdReason;
        return this;
    }

    public String getHoldReassessingDate() {
        return holdReassessingDate;
    }

    public RuleModel setHoldReassessingDate(String holdReassessingDate) {
        this.holdReassessingDate = holdReassessingDate;
        return this;
    }

    public Boolean getPreventRearrangement() {
        return preventRearrangement;
    }

    public RuleModel setPreventRearrangement(Boolean preventRearrangement) {
        this.preventRearrangement = preventRearrangement;
        return this;
    }
}
