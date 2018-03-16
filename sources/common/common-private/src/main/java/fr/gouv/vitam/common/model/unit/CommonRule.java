package fr.gouv.vitam.common.model.unit;

import java.util.List;

import fr.gouv.culture.archivesdefrance.seda.v2.RuleIdType;

/**
 * Common rule Interface
 */
public interface CommonRule extends CommonRuleBase{

    /**
     * Gets the value of the refNonRuleId property.
     * 
     * @return refNonRuleId
     */
    List<RuleIdType> getRefNonRuleId();
}
