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
package fr.gouv.vitam.worker.core.mapping;

import fr.gouv.culture.archivesdefrance.seda.v2.HoldRuleType;
import fr.gouv.culture.archivesdefrance.seda.v2.RuleIdType;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.model.unit.CommonRule;
import fr.gouv.vitam.common.model.unit.RuleCategoryModel;
import fr.gouv.vitam.common.model.unit.RuleModel;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
            List<String> refNonRuleId = rule
                .getRefNonRuleId()
                .stream()
                .map(RuleIdType::getValue)
                .collect(Collectors.toList());
            ruleCategoryModel.addAllPreventRulesId(refNonRuleId);
        }

        if (!ruleUsed) {
            return null;
        }
        return ruleCategoryModel;
    }

    public RuleCategoryModel fillHoldRule(HoldRuleType rule) {
        if (rule == null) {
            return null;
        }

        boolean ruleUsed = false;
        RuleCategoryModel ruleCategoryModel = new RuleCategoryModel();
        RuleModel ruleModel = null;

        List<RuleModel> rules = new ArrayList<>();

        for (JAXBElement<?> ruleEntry : rule.getHoldRuleDefGroup()) {
            String fieldLocalName = ruleEntry.getName().getLocalPart();
            switch (fieldLocalName) {
                case SedaConstants.TAG_RULE_RULE:
                    ruleModel = new RuleModel();
                    rules.add(ruleModel);
                    String ruleId = ((RuleIdType) ruleEntry.getValue()).getValue();
                    ruleModel.setRule(ruleId);
                    break;
                case SedaConstants.TAG_RULE_START_DATE:
                    assert (ruleModel != null);
                    XMLGregorianCalendar startDate = (XMLGregorianCalendar) ruleEntry.getValue();
                    ruleModel.setStartDate(startDate.toString());
                    break;
                case SedaConstants.TAG_RULE_HOLD_END_DATE:
                    assert (ruleModel != null);
                    XMLGregorianCalendar holdEndDate = (XMLGregorianCalendar) ruleEntry.getValue();
                    ruleModel.setHoldEndDate(holdEndDate.toString());
                    break;
                case SedaConstants.TAG_RULE_HOLD_OWNER:
                    assert (ruleModel != null);
                    String holdOwner = (String) ruleEntry.getValue();
                    ruleModel.setHoldOwner(holdOwner);
                    break;
                case SedaConstants.TAG_RULE_HOLD_REASSESSING_DATE:
                    assert (ruleModel != null);
                    XMLGregorianCalendar holdReassessingDate = (XMLGregorianCalendar) ruleEntry.getValue();
                    ruleModel.setHoldReassessingDate(holdReassessingDate.toString());
                    break;
                case SedaConstants.TAG_RULE_HOLD_REASON:
                    assert (ruleModel != null);
                    String holdReason = (String) ruleEntry.getValue();
                    ruleModel.setHoldReason(holdReason);
                    break;
                case SedaConstants.TAG_RULE_PREVENT_REARRANGEMENT:
                    assert (ruleModel != null);
                    Boolean preventRearrangement = (Boolean) ruleEntry.getValue();
                    ruleModel.setPreventRearrangement(preventRearrangement);
                    break;
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

        if (!rule.getRefNonRuleId().isEmpty()) {
            ruleUsed = true;
            List<String> refNonRuleId = rule
                .getRefNonRuleId()
                .stream()
                .map(RuleIdType::getValue)
                .collect(Collectors.toList());
            ruleCategoryModel.addAllPreventRulesId(refNonRuleId);
        }

        if (!ruleUsed) {
            return null;
        }
        return ruleCategoryModel;
    }
}
