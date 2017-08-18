package fr.gouv.vitam.common.model.unit;

import java.util.List;

import fr.gouv.culture.archivesdefrance.seda.v2.RuleIdType;

public interface CommonRule {

    List<Object> getRuleAndStartDate();

    Boolean isPreventInheritance();
    void setPreventInheritance(Boolean value);

    List<RuleIdType> getRefNonRuleId();
}
