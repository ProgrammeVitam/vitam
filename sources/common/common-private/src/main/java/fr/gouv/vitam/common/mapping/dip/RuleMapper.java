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
package fr.gouv.vitam.common.mapping.dip;

import fr.gouv.culture.archivesdefrance.seda.v2.AppraisalRuleType;
import fr.gouv.culture.archivesdefrance.seda.v2.ClassificationRuleType;
import fr.gouv.culture.archivesdefrance.seda.v2.FinalActionAppraisalCodeType;
import fr.gouv.culture.archivesdefrance.seda.v2.FinalActionStorageCodeType;
import fr.gouv.culture.archivesdefrance.seda.v2.HoldRuleType;
import fr.gouv.culture.archivesdefrance.seda.v2.ObjectFactory;
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

    private final ObjectFactory objectFactory;

    public RuleMapper() {
        this.objectFactory = new ObjectFactory();
    }

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
            commonRule.getRefNonRuleId().addAll(mapPreventRuleIds(inheritance));
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
                XMLGregorianCalendar xmlGregorianCalendar = newInstance()
                    .newXMLGregorianCalendar(classificationReassessingDate);
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

    public HoldRuleType fillHoldRule(RuleCategoryModel ruleCategory) throws DatatypeConfigurationException {
        if (ruleCategory == null) {
            return null;
        }

        HoldRuleType holdRuleType = objectFactory.createHoldRuleType();

        InheritanceModel inheritance = ruleCategory.getInheritance();
        if (inheritance != null) {
            holdRuleType.getRefNonRuleId().addAll(mapPreventRuleIds(inheritance));
            if (holdRuleType.getRefNonRuleId().isEmpty()) {
                holdRuleType.setPreventInheritance(inheritance.isPreventInheritance());
            }
        }

        for (RuleModel rule : ruleCategory.getRules()) {
            // RuleId
            RuleIdType ruleIdType = objectFactory.createRuleIdType();
            ruleIdType.setValue(rule.getRule());
            holdRuleType.getHoldRuleDefGroup().add(objectFactory.createHoldRuleTypeRule(ruleIdType));

            // StartDate
            if (rule.getStartDate() != null) {
                holdRuleType
                    .getHoldRuleDefGroup()
                    .add(
                        objectFactory.createHoldRuleTypeStartDate(
                            newInstance().newXMLGregorianCalendar(rule.getStartDate())
                        )
                    );
            }

            // HoldEndDate
            if (rule.getHoldEndDate() != null) {
                holdRuleType
                    .getHoldRuleDefGroup()
                    .add(
                        objectFactory.createHoldRuleTypeHoldEndDate(
                            newInstance().newXMLGregorianCalendar(rule.getHoldEndDate())
                        )
                    );
            }

            // HoldOwner
            if (rule.getHoldOwner() != null) {
                holdRuleType.getHoldRuleDefGroup().add(objectFactory.createHoldRuleTypeHoldOwner(rule.getHoldOwner()));
            }

            // HoldReassessingDate
            if (rule.getHoldReassessingDate() != null) {
                holdRuleType
                    .getHoldRuleDefGroup()
                    .add(
                        objectFactory.createHoldRuleTypeHoldReassessingDate(
                            newInstance().newXMLGregorianCalendar(rule.getHoldReassessingDate())
                        )
                    );
            }

            // HoldReason
            if (rule.getHoldReason() != null) {
                holdRuleType
                    .getHoldRuleDefGroup()
                    .add(objectFactory.createHoldRuleTypeHoldReason(rule.getHoldReason()));
            }

            // PreventRearrangement
            if (rule.getPreventRearrangement() != null) {
                holdRuleType
                    .getHoldRuleDefGroup()
                    .add(objectFactory.createHoldRuleTypePreventRearrangement(rule.getPreventRearrangement()));
            }
        }

        return holdRuleType;
    }

    private List<RuleIdType> mapPreventRuleIds(InheritanceModel inheritance) {
        return inheritance
            .getPreventRulesId()
            .stream()
            .map(ruleId -> {
                RuleIdType ruleIdType = new RuleIdType();
                ruleIdType.setValue(ruleId);
                return ruleIdType;
            })
            .collect(Collectors.toList());
    }
}
