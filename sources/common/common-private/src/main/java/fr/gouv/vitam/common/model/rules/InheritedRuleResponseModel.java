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
package fr.gouv.vitam.common.model.rules;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.common.model.unit.RuleModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pojo for computed inherited rule
 */
public class InheritedRuleResponseModel extends BaseInheritedResponseModel {

    @JsonProperty("Rule")
    private String ruleId;

    @JsonIgnore
    private Map<String, Object> extendedRuleAttributes = new HashMap<>();

    @JsonAnyGetter
    public Map<String, Object> getExtendedRuleAttributes() {
        return extendedRuleAttributes;
    }

    @JsonAnySetter
    public void setAny(String key, Object value) {
        this.extendedRuleAttributes.put(key, value);
    }

    public InheritedRuleResponseModel() {
        // Empty constructor for deserialization
    }

    public InheritedRuleResponseModel(
        String unitId,
        String originatingAgency,
        List<List<String>> paths,
        String ruleId,
        Map<String, Object> extendedRuleAttributes
    ) {
        super(unitId, originatingAgency, paths);
        this.ruleId = ruleId;
        this.extendedRuleAttributes = extendedRuleAttributes;
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    @JsonIgnore
    public String getStartDate() {
        return (String) this.extendedRuleAttributes.get(RuleModel.START_DATE);
    }

    public void setStartDate(String startDate) {
        this.extendedRuleAttributes.put(RuleModel.START_DATE, startDate);
    }

    @JsonIgnore
    public String getEndDate() {
        return (String) this.extendedRuleAttributes.get(RuleModel.END_DATE);
    }

    public void setEndDate(String endDate) {
        this.extendedRuleAttributes.put(RuleModel.START_DATE, endDate);
    }
}
