/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 *******************************************************************************/
package fr.gouv.vitam.common.mapping.dip;

import fr.gouv.culture.archivesdefrance.seda.v2.ArchiveUnitType;
import fr.gouv.culture.archivesdefrance.seda.v2.ManagementType;
import fr.gouv.culture.archivesdefrance.seda.v2.RuleIdType;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.model.unit.ArchiveUnitModel;
import fr.gouv.vitam.common.model.unit.DescriptiveMetadataModel;
import fr.gouv.vitam.common.model.unit.RuleCategoryModel;
import fr.gouv.vitam.common.model.unit.RuleModel;
import org.junit.Test;

import javax.xml.datatype.XMLGregorianCalendar;

import static org.assertj.core.api.Assertions.assertThat;

public class ArchiveUnitMapperTest {

    @Test
    public void should_map_unit_with_empty_fields() throws Exception {
        //Given
        ArchiveUnitMapper archiveUnitMapper = new ArchiveUnitMapper();
        ArchiveUnitModel archiveUnitModel = new ArchiveUnitModel();
        archiveUnitModel.setId("1234564");
        archiveUnitModel.setDescriptiveMetadataModel(new DescriptiveMetadataModel());
        RuleCategoryModel rule;

        //AccessRule
        rule = generateRule(SedaConstants.TAG_RULE_ACCESS);
        archiveUnitModel.getManagement().setRuleCategoryModel(rule, SedaConstants.TAG_RULE_ACCESS);

        //AppraisalRule
        rule = generateRule(SedaConstants.TAG_RULE_APPRAISAL);
        archiveUnitModel.getManagement().setRuleCategoryModel(rule, SedaConstants.TAG_RULE_APPRAISAL);

        //ClassificationRule
        rule = generateRule(SedaConstants.TAG_RULE_CLASSIFICATION);
        rule.setClassificationLevel("fakeClassificationLevel");
        rule.setClassificationOwner("fakeClassificationOwner");
        rule.setNeedReassessingAuthorization(true);
        rule.setClassificationReassessingDate("2000-01-02");
        archiveUnitModel.getManagement().setRuleCategoryModel(rule, SedaConstants.TAG_RULE_CLASSIFICATION);

        //DisseminationRule
        rule = generateRule(SedaConstants.TAG_RULE_DISSEMINATION);
        archiveUnitModel.getManagement().setRuleCategoryModel(rule, SedaConstants.TAG_RULE_DISSEMINATION);

        //ReuseRule
        rule = generateRule(SedaConstants.TAG_RULE_REUSE);
        archiveUnitModel.getManagement().setRuleCategoryModel(rule, SedaConstants.TAG_RULE_REUSE);

        //StorageRule
        rule = generateRule(SedaConstants.TAG_RULE_STORAGE);
        archiveUnitModel.getManagement().setRuleCategoryModel(rule, SedaConstants.TAG_RULE_STORAGE);

        // When
        ArchiveUnitType archiveUnitType = archiveUnitMapper.map(archiveUnitModel);

        // Then
        assertThat(archiveUnitType.getId()).isEqualTo("1234564");
        ManagementType management = archiveUnitType.getManagement();
        assertThat(management).isNotNull();

        //AccessRule
        assertThat(management.getAccessRule()).isNotNull();
        assertThat(management.getAccessRule().isPreventInheritance()).isNull();
        assertThat(management.getAccessRule().getRefNonRuleId()).hasSize(2);
        assertThat(management.getAccessRule().getRefNonRuleId().get(0).getValue()).isIn("R1", "R2");
        assertThat(management.getAccessRule().getRefNonRuleId().get(1).getValue()).isIn("R1", "R2");
        assertThat(management.getAccessRule().getRuleAndStartDate()).hasSize(1);
        assertThat(management.getAccessRule().getRuleAndStartDate().get(0)).isInstanceOf(RuleIdType.class);
        RuleIdType ruleIdType = (RuleIdType) management.getAccessRule().getRuleAndStartDate().get(0);
        assertThat(ruleIdType.getValue()).isEqualTo("R3");

        //AppraisalRule
        assertThat(management.getAppraisalRule()).isNotNull();
        assertThat(management.getAppraisalRule().isPreventInheritance()).isNull();
        assertThat(management.getAppraisalRule().getRefNonRuleId()).hasSize(2);
        assertThat(management.getAppraisalRule().getRefNonRuleId().get(0).getValue()).isIn("R1", "R2");
        assertThat(management.getAppraisalRule().getRefNonRuleId().get(1).getValue()).isIn("R1", "R2");
        assertThat(management.getAppraisalRule().getRuleAndStartDate().get(0)).isInstanceOf(RuleIdType.class);
        ruleIdType = (RuleIdType) management.getAppraisalRule().getRuleAndStartDate().get(0);
        assertThat(ruleIdType.getValue()).isEqualTo("R3");
        assertThat(management.getAppraisalRule().getRuleAndStartDate().get(1)).isInstanceOf(XMLGregorianCalendar.class);
        XMLGregorianCalendar date = (XMLGregorianCalendar) management.getAppraisalRule().getRuleAndStartDate().get(1);
        assertThat(date.getYear()).isEqualTo(2000);
        assertThat(date.getMonth()).isEqualTo(1);
        assertThat(date.getDay()).isEqualTo(1);
        assertThat(management.getAppraisalRule().getFinalAction().value()).isEqualTo("Keep");

        //ClassificationRule
        assertThat(management.getClassificationRule()).isNotNull();
        assertThat(management.getClassificationRule().isPreventInheritance()).isNull();
        assertThat(management.getClassificationRule().getRefNonRuleId()).hasSize(2);
        assertThat(management.getClassificationRule().getRefNonRuleId().get(0).getValue()).isIn("R1", "R2");
        assertThat(management.getClassificationRule().getRefNonRuleId().get(1).getValue()).isIn("R1", "R2");
        assertThat(management.getClassificationRule().getRuleAndStartDate().get(0)).isInstanceOf(RuleIdType.class);
        ruleIdType = (RuleIdType) management.getClassificationRule().getRuleAndStartDate().get(0);
        assertThat(ruleIdType.getValue()).isEqualTo("R3");
        assertThat(management.getClassificationRule().getRuleAndStartDate().get(1))
            .isInstanceOf(XMLGregorianCalendar.class);
        date = (XMLGregorianCalendar) management.getClassificationRule().getRuleAndStartDate().get(1);
        assertThat(date.getYear()).isEqualTo(2000);
        assertThat(date.getMonth()).isEqualTo(1);
        assertThat(date.getDay()).isEqualTo(1);
        assertThat(management.getClassificationRule().isNeedReassessingAuthorization()).isTrue();
        date = management.getClassificationRule().getClassificationReassessingDate();
        assertThat(date.getYear()).isEqualTo(2000);
        assertThat(date.getMonth()).isEqualTo(1);
        assertThat(date.getDay()).isEqualTo(2);

        //DisseminationRule
        assertThat(management.getDisseminationRule()).isNotNull();
        assertThat(management.getDisseminationRule().isPreventInheritance()).isNull();
        assertThat(management.getDisseminationRule().getRefNonRuleId()).hasSize(2);
        assertThat(management.getDisseminationRule().getRefNonRuleId().get(0).getValue()).isIn("R1", "R2");
        assertThat(management.getDisseminationRule().getRefNonRuleId().get(1).getValue()).isIn("R1", "R2");
        assertThat(management.getDisseminationRule().getRuleAndStartDate().get(0)).isInstanceOf(RuleIdType.class);
        ruleIdType = (RuleIdType) management.getDisseminationRule().getRuleAndStartDate().get(0);
        assertThat(ruleIdType.getValue()).isEqualTo("R3");
        assertThat(management.getDisseminationRule().getRuleAndStartDate().get(1))
            .isInstanceOf(XMLGregorianCalendar.class);
        date = (XMLGregorianCalendar) management.getDisseminationRule().getRuleAndStartDate().get(1);
        assertThat(date.getYear()).isEqualTo(2000);
        assertThat(date.getMonth()).isEqualTo(1);
        assertThat(date.getDay()).isEqualTo(1);

        //ReuseRule
        assertThat(management.getReuseRule()).isNotNull();
        assertThat(management.getReuseRule().isPreventInheritance()).isNull();
        assertThat(management.getReuseRule().getRefNonRuleId()).hasSize(2);
        assertThat(management.getReuseRule().getRefNonRuleId().get(0).getValue()).isIn("R1", "R2");
        assertThat(management.getReuseRule().getRefNonRuleId().get(1).getValue()).isIn("R1", "R2");
        assertThat(management.getReuseRule().getRuleAndStartDate().get(0)).isInstanceOf(RuleIdType.class);
        ruleIdType = (RuleIdType) management.getReuseRule().getRuleAndStartDate().get(0);
        assertThat(ruleIdType.getValue()).isEqualTo("R3");
        assertThat(management.getReuseRule().getRuleAndStartDate().get(1)).isInstanceOf(XMLGregorianCalendar.class);
        date = (XMLGregorianCalendar) management.getReuseRule().getRuleAndStartDate().get(1);
        assertThat(date.getYear()).isEqualTo(2000);
        assertThat(date.getMonth()).isEqualTo(1);
        assertThat(date.getDay()).isEqualTo(1);

        //StorageRule
        assertThat(management.getStorageRule()).isNotNull();
        assertThat(management.getStorageRule().isPreventInheritance()).isNull();
        assertThat(management.getStorageRule().getRefNonRuleId()).hasSize(2);
        assertThat(management.getStorageRule().getRefNonRuleId().get(0).getValue()).isIn("R1", "R2");
        assertThat(management.getStorageRule().getRefNonRuleId().get(1).getValue()).isIn("R1", "R2");
        assertThat(management.getStorageRule().getRuleAndStartDate().get(0)).isInstanceOf(RuleIdType.class);
        ruleIdType = (RuleIdType) management.getStorageRule().getRuleAndStartDate().get(0);
        assertThat(ruleIdType.getValue()).isEqualTo("R3");
        assertThat(management.getStorageRule().getRuleAndStartDate().get(1)).isInstanceOf(XMLGregorianCalendar.class);
        date = (XMLGregorianCalendar) management.getStorageRule().getRuleAndStartDate().get(1);
        assertThat(date.getYear()).isEqualTo(2000);
        assertThat(date.getMonth()).isEqualTo(1);
        assertThat(date.getDay()).isEqualTo(1);
        assertThat(management.getStorageRule().getFinalAction().value()).isEqualTo("Copy");
    }



    /**
     * Generate rule
     *
     * @param type
     * @return
     */
    private RuleCategoryModel generateRule(String type) {
        RuleCategoryModel ruleCategoryModel = new RuleCategoryModel();
        ruleCategoryModel.addToPreventRulesId("R1");
        ruleCategoryModel.addToPreventRulesId("R2");
        ruleCategoryModel.setPreventInheritance(true);

        RuleModel rule = new RuleModel();
        rule.setRule("R3");
        rule.setStartDate("2000-01-01");

        switch (type) {
            case SedaConstants.TAG_RULE_STORAGE:
                ruleCategoryModel.setFinalAction("Copy");
                break;
            case SedaConstants.TAG_RULE_APPRAISAL:
                ruleCategoryModel.setFinalAction("Keep");

                break;
            case SedaConstants.TAG_RULE_ACCESS:
                rule.setStartDate(null);

            case SedaConstants.TAG_RULE_DISSEMINATION:
            case SedaConstants.TAG_RULE_REUSE:
            case SedaConstants.TAG_RULE_CLASSIFICATION:
                break;
            default:
                throw new IllegalArgumentException("Type cannot be " + type);
        }
        ruleCategoryModel.getRules().add(rule);

        return ruleCategoryModel;
    }

}
