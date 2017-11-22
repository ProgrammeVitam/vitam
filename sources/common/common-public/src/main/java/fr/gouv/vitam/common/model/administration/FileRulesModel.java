package fr.gouv.vitam.common.model.administration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import fr.gouv.vitam.common.model.ModelConstants;

/**
 * POJO java use for mapping @{@link fr.gouv.vitam.functional.administration.common.FileRules}
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class FileRulesModel {

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

    @JsonProperty("RuleId")
    private String ruleId;

    @JsonProperty("RuleType")
    private String ruleType;

    @JsonProperty("RuleValue")
    private String ruleValue;

    @JsonProperty("RuleDescription")
    private String ruleDescription;

    @JsonProperty("RuleDuration")
    private String ruleDuration;

    @JsonProperty("RuleMeasurement")
    private String ruleMeasurement;

    @JsonProperty("CreationDate")
    private String creationDate;

    @JsonProperty("UpdateDate")
    private String updateDate;

    public FileRulesModel() {
    }

    public FileRulesModel(String ruleId, String ruleType, String ruleValue, String ruleDescription,
        String ruleDuration, String ruleMeasurement) {
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

    public String getRuleType() {
        return ruleType;
    }

    public void setRuleType(String ruleType) {
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

    public String getRuleMeasurement() {
        return ruleMeasurement;
    }

    public void setRuleMeasurement(String ruleMeasurement) {
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
        boolean same = false;
        if (this == obj) {
            return true;
        }
        if (obj != null && obj instanceof FileRulesModel) {
            final FileRulesModel objectToCompare = (FileRulesModel) obj;
            if (this.getRuleId().equals(objectToCompare.getRuleId())) {
                same = true;
            }
        }
        return same;
    }

    @Override
    public String toString() {
        return "id=" + id + ", tenant=" + tenant + ", ruleId=" + ruleId + ", ruleType=" + ruleType +
            ", ruleValue=" + ruleValue + ", ruleDescription=" + ruleDescription + ", ruleDuration=" + ruleDuration +
            ", ruleMeasurement=" + ruleMeasurement + ", creationDate=" + creationDate + ", updateDate=" + updateDate +
            ", version=" + version;
    }
}
