package fr.gouv.vitam.common.mapping.dip;

import fr.gouv.culture.archivesdefrance.seda.v2.RuleIdType;
import fr.gouv.vitam.common.model.unit.CommonRule;
import fr.gouv.vitam.common.model.unit.InheritanceModel;
import fr.gouv.vitam.common.model.unit.RuleCategoryModel;
import fr.gouv.vitam.common.model.unit.RuleModel;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static javax.xml.datatype.DatatypeFactory.newInstance;

/**
 * Map data base representation of rules to a jaxb representation
 * => Map RuleCategoryModel to CommonRule
 */
public class RuleMapper {

    /**
     * This generic method is used to map data base model of rule to jaxb
     * @param ruleCategory
     * @param commonRuleSupplier
     * @param <T>
     * @return
     * @throws DatatypeConfigurationException
     */
    public <T extends CommonRule> T fillCommonRule(RuleCategoryModel ruleCategory, Supplier<T> commonRuleSupplier)
        throws DatatypeConfigurationException {

        if (ruleCategory == null) {
            return null;
        }

        T commonRule = commonRuleSupplier.get();

        InheritanceModel inheritance = ruleCategory.getInheritance();
        if (inheritance != null) {
            commonRule.getRefNonRuleId().addAll(inheritance.getPreventRulesId().stream().map(ruleId -> {
                RuleIdType ruleIdType = new RuleIdType();
                ruleIdType.setValue(ruleId);
                return ruleIdType;
            }).collect(Collectors.toList()));

            commonRule.setPreventInheritance(inheritance.isPreventInheritance());
        }

        List<Object> ruleAndStartDate = new ArrayList<>();
        List<RuleModel> rules = ruleCategory.getRules();
        for (RuleModel rule : rules) {
            String startDate = rule.getStartDate();
            XMLGregorianCalendar xmlGregorianCalendar = newInstance().newXMLGregorianCalendar(startDate);
            RuleIdType ruleIdType = new RuleIdType();
            ruleIdType.setValue(rule.getRule());
            ruleAndStartDate.add(ruleIdType);
            ruleAndStartDate.add(xmlGregorianCalendar);
        }
        commonRule.getRuleAndStartDate().addAll(ruleAndStartDate);
        return commonRule;
    }

}
