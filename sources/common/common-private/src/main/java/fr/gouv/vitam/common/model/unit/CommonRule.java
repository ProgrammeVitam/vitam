package fr.gouv.vitam.common.model.unit;

import java.util.List;

import fr.gouv.culture.archivesdefrance.seda.v2.RuleIdType;

/**
 * Common rule Interface
 */
public interface CommonRule {

    /**
     * Gets the value of the ruleAndStartDate property.
     * 
     * @return the ruleAndStartDate property
     */
    List<Object> getRuleAndStartDate();

    /**
     * Gets the value of the preventInheritance property.
     * 
     * @return the preventInheritance
     */
    Boolean isPreventInheritance();

    /**
     * Sets the value of the preventInheritance property.
     * 
     * @param value true or false
     */
    void setPreventInheritance(Boolean value);

    /**
     * Gets the value of the refNonRuleId property.
     * 
     * @return refNonRuleId
     */
    List<RuleIdType> getRefNonRuleId();
}
