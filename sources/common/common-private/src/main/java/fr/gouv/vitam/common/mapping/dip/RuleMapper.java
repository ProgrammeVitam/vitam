package fr.gouv.vitam.common.mapping.dip;

import fr.gouv.culture.archivesdefrance.seda.v2.AppraisalRuleType;
import fr.gouv.culture.archivesdefrance.seda.v2.ClassificationRuleType;
import fr.gouv.culture.archivesdefrance.seda.v2.FinalActionAppraisalCodeType;
import fr.gouv.culture.archivesdefrance.seda.v2.FinalActionStorageCodeType;
import fr.gouv.culture.archivesdefrance.seda.v2.RuleIdType;
import fr.gouv.culture.archivesdefrance.seda.v2.StorageRuleType;
import fr.gouv.vitam.common.ParametersChecker;
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
 * Map data base representation of rules to a jaxb representation =&tg; Map RuleCategoryModel to CommonRule
 */
public class RuleMapper {

    /**
     * This generic method is used to map data base model of rule to jaxb
     *
     * @param ruleCategory
     * @param commonRuleSupplier
     * @param <T>
     * @return rule category
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
            if (commonRule.getRefNonRuleId().isEmpty()) {
                commonRule.setPreventInheritance(inheritance.isPreventInheritance());
            }
        }

        List<Object> ruleAndStartDate = new ArrayList<>();
        List<RuleModel> rules = ruleCategory.getRules();
        for (RuleModel rule : rules) {
            RuleIdType ruleIdType = new RuleIdType();
            if (ParametersChecker.isNotEmpty(rule.getRule())) {
                ruleIdType.setValue(rule.getRule());
                ruleAndStartDate.add(ruleIdType);
            }

            String startDate = rule.getStartDate();
            if (ParametersChecker.isNotEmpty(startDate)) {
                XMLGregorianCalendar xmlGregorianCalendar = newInstance().newXMLGregorianCalendar(startDate);
                ruleAndStartDate.add(xmlGregorianCalendar);
            }
        }

        commonRule.getRuleAndStartDate().addAll(ruleAndStartDate);

        // Case ClassificationRuleType manage other fields
        if (commonRule instanceof ClassificationRuleType) {
            ClassificationRuleType crt = (ClassificationRuleType) commonRule;
            crt.setClassificationAudience(ruleCategory.getClassificationAudience());
            crt.setClassificationLevel(ruleCategory.getClassificationLevel());
            crt.setClassificationOwner(ruleCategory.getClassificationOwner());
            crt.setNeedReassessingAuthorization(ruleCategory.isNeedReassessingAuthorization());
            String classificationReassessingDate = ruleCategory.getClassificationReassessingDate();
            if (ParametersChecker.isNotEmpty(classificationReassessingDate)) {
                XMLGregorianCalendar xmlGregorianCalendar =
                    newInstance().newXMLGregorianCalendar(classificationReassessingDate);
                crt.setClassificationReassessingDate(xmlGregorianCalendar);
            }
        }

        // FinalAction for StorageRuleType and AppraisalRuleType
        String finalAction = ruleCategory.getFinalAction();
        if (ParametersChecker.isNotEmpty(finalAction)) {
            try {
                if (commonRule instanceof StorageRuleType) {
                    StorageRuleType srt = (StorageRuleType) commonRule;
                    srt.setFinalAction(FinalActionStorageCodeType.fromValue(finalAction));
                } else if (commonRule instanceof AppraisalRuleType) {
                    AppraisalRuleType art = (AppraisalRuleType) commonRule;
                    art.setFinalAction(FinalActionAppraisalCodeType.fromValue(finalAction));
                }
            } catch (IllegalArgumentException e) {
                throw new DatatypeConfigurationException(e);
            }
        }

        return commonRule;
    }

}
