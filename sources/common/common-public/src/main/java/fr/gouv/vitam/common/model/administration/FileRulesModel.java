/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
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

package fr.gouv.vitam.common.model.administration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.common.model.ModelConstants;
import org.apache.commons.lang3.builder.EqualsBuilder;

/**
 * POJO java use for mapping @{@link fr.gouv.vitam.functional.administration.common.FileRules}
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class FileRulesModel {

    public static final String RULE_ID = "RuleId";
    public static final String RULE_TYPE = "RuleType";
    public static final String RULE_VALUE = "RuleValue";
    public static final String RULE_DESCRIPTION = "RuleDescription";
    public static final String RULE_DURATION = "RuleDuration";
    public static final String RULE_MEASUREMENT = "RuleMeasurement";

    /**
     * unique id
     */
    @JsonProperty(ModelConstants.HASH + ModelConstants.TAG_ID)
    private String id;

    /**
     * tenant id
     */
    @JsonProperty(ModelConstants.HASH + ModelConstants.TAG_TENANT)
    private Integer tenant;

    /**
     * document version
     */
    @JsonProperty(ModelConstants.HASH + ModelConstants.TAG_VERSION)
    private Integer version;

    @JsonProperty(RULE_ID)
    private String ruleId;

    @JsonProperty(RULE_TYPE)
    private RuleType ruleType;

    @JsonProperty(RULE_VALUE)
    private String ruleValue;

    @JsonProperty(RULE_DESCRIPTION)
    private String ruleDescription;

    @JsonProperty(RULE_DURATION)
    private String ruleDuration;

    @JsonProperty(RULE_MEASUREMENT)
    private RuleMeasurementEnum ruleMeasurement;

    @JsonProperty("CreationDate")
    private String creationDate;

    @JsonProperty("UpdateDate")
    private String updateDate;

    public FileRulesModel() {}

    public FileRulesModel(
        String ruleId,
        RuleType ruleType,
        String ruleValue,
        String ruleDescription,
        String ruleDuration,
        RuleMeasurementEnum ruleMeasurement
    ) {
        this.ruleId = ruleId;
        this.ruleType = ruleType;
        this.ruleValue = ruleValue;
        this.ruleDescription = ruleDescription;
        this.ruleDuration = ruleDuration;
        this.ruleMeasurement = ruleMeasurement;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getTenant() {
        return tenant;
    }

    public void setTenant(Integer tenant) {
        this.tenant = tenant;
    }

    public RuleType getRuleType() {
        return ruleType;
    }

    public void setRuleType(RuleType ruleType) {
        this.ruleType = ruleType;
    }

    public String getRuleValue() {
        return ruleValue;
    }

    public void setRuleValue(String ruleValue) {
        this.ruleValue = ruleValue;
    }

    public String getRuleDescription() {
        return ruleDescription;
    }

    public void setRuleDescription(String ruleDescription) {
        this.ruleDescription = ruleDescription;
    }

    public String getRuleDuration() {
        return ruleDuration;
    }

    public void setRuleDuration(String ruleDuration) {
        this.ruleDuration = ruleDuration;
    }

    public RuleMeasurementEnum getRuleMeasurement() {
        return ruleMeasurement;
    }

    public void setRuleMeasurement(RuleMeasurementEnum ruleMeasurement) {
        this.ruleMeasurement = ruleMeasurement;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public String getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(String updateDate) {
        this.updateDate = updateDate;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((ruleId == null) ? 0 : ruleId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof FileRulesModel)) {
            return false;
        }

        final FileRulesModel objectToCompare = (FileRulesModel) obj;
        return new EqualsBuilder()
            .append(this.ruleId, objectToCompare.getRuleId())
            .append(this.ruleDuration, objectToCompare.getRuleDuration())
            .append(this.ruleMeasurement, objectToCompare.getRuleMeasurement())
            .append(this.ruleDescription, objectToCompare.getRuleDescription())
            .append(this.ruleValue, objectToCompare.getRuleValue())
            .append(this.ruleType, objectToCompare.getRuleType())
            .isEquals();
    }

    public boolean hasSameRuleId(FileRulesModel rule) {
        return rule != null && rule.getRuleId().equals(this.ruleId);
    }

    @Override
    public String toString() {
        return (
            "ruleId=" +
            ruleId +
            ", ruleType=" +
            ruleType +
            ", ruleValue=" +
            ruleValue +
            ", ruleDescription=" +
            ruleDescription +
            ", ruleDuration=" +
            ruleDuration +
            ", ruleMeasurement=" +
            ruleMeasurement
        );
    }
}
