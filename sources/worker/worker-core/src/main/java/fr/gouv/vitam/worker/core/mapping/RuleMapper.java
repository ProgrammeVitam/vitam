package fr.gouv.vitam.worker.core.mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.datatype.XMLGregorianCalendar;

import fr.gouv.culture.archivesdefrance.seda.v2.RuleIdType;
import fr.gouv.vitam.common.model.unit.CommonRule;
import fr.gouv.vitam.common.model.unit.RuleCategoryModel;
import fr.gouv.vitam.common.model.unit.RuleModel;

/**
 * Map data base representation of rules to a jaxb representation =&gt; Map RuleCategoryModel to CommonRule
 */
public class RuleMapper {

    /**
     * This generic method is used to map data base model of rule to jaxb
     * 
     * @param rule
     * @return rule category
     */
    public RuleCategoryModel fillCommonRule(CommonRule rule) {
        if (rule == null) {
            return null;
        }

        boolean ruleUsed = false;
        RuleCategoryModel ruleCategoryModel = new RuleCategoryModel();
        RuleModel ruleModel = null;

        List<RuleModel> rules = new ArrayList<>();


        for (Object ruleOrStartDate : rule.getRuleAndStartDate()) {
            if (ruleOrStartDate instanceof RuleIdType) {
                ruleModel = new RuleModel();
                rules.add(ruleModel);
                String ruleId = ((RuleIdType) ruleOrStartDate).getValue();
                ruleModel.setRule(ruleId);
            }
            if (ruleOrStartDate instanceof XMLGregorianCalendar && ruleModel != null) {
                XMLGregorianCalendar startDate = (XMLGregorianCalendar) ruleOrStartDate;
                ruleModel.setStartDate(startDate.toString());
            }
        }

        if (!rules.isEmpty()) {
            ruleUsed = true;
            ruleCategoryModel.getRules().addAll(rules);
        }

        if (rule.isPreventInheritance() != null) {
            ruleUsed = true;
            ruleCategoryModel.setPreventInheritance(rule.isPreventInheritance());
        }

        if (rule.getRefNonRuleId().size() > 0) {
            ruleUsed = true;
            List<String> refNonRuleId =
                rule.getRefNonRuleId().stream().map(RuleIdType::getValue).collect(Collectors.toList());
            ruleCategoryModel.addAllPreventRulesId(refNonRuleId);
        }

        if (!ruleUsed) {
            return null;
        }
        return ruleCategoryModel;

    }

}
