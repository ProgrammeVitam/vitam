package fr.gouv.vitam.worker.common;

import fr.gouv.culture.archivesdefrance.seda.v2.RuleIdType;

import java.util.List;

public interface CommonRule {

    List<Object> getRuleAndStartDate();

    Boolean isPreventInheritance();

    List<RuleIdType> getRefNonRuleId();
}
