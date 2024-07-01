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

import com.google.common.collect.Lists;
import fr.gouv.culture.archivesdefrance.seda.v2.AccessRuleType;
import fr.gouv.culture.archivesdefrance.seda.v2.DisseminationRuleType;
import fr.gouv.culture.archivesdefrance.seda.v2.HoldRuleType;
import fr.gouv.culture.archivesdefrance.seda.v2.RuleIdType;
import fr.gouv.culture.archivesdefrance.seda.v2.StorageRuleType;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.model.unit.RuleCategoryModel;
import fr.gouv.vitam.common.model.unit.RuleModel;
import org.junit.Test;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class RuleMapperTest {

    @Test
    public void should_map_common_rule() throws Exception {
        // Given
        RuleMapper ruleMapper = new RuleMapper();
        RuleCategoryModel ruleModel = new RuleCategoryModel();
        ruleModel.getRules().add(new RuleModel("AC-00023", "2017-04-01"));
        ruleModel.setPreventInheritance(true);
        ruleModel.addAllPreventRulesId(Lists.newArrayList("AC-00021", "AC-00022"));

        // When
        AccessRuleType accessRuleType = ruleMapper.fillCommonRule(ruleModel, AccessRuleType::new);

        // Then
        assertThat(accessRuleType.getRefNonRuleId()).extracting("value").containsExactly("AC-00021", "AC-00022");
        assertThat(accessRuleType.isPreventInheritance()).isNull();
        assertThat(((RuleIdType) accessRuleType.getRuleAndStartDate().get(0)).getValue()).isEqualTo("AC-00023");
        assertThat(accessRuleType.getRuleAndStartDate().get(1).toString()).isEqualTo("2017-04-01");
    }

    @Test
    public void should_map_empty_rule() throws Exception {
        // Given
        RuleMapper ruleMapper = new RuleMapper();
        RuleCategoryModel ruleModel = new RuleCategoryModel();
        ruleModel.getRules().add(new RuleModel(null, null));

        // When
        StorageRuleType storageRuleType = ruleMapper.fillCommonRule(ruleModel, StorageRuleType::new);

        // Then
        assertThat(storageRuleType.getRuleAndStartDate().size()).isEqualTo(0);
    }

    @Test
    public void ruleWithNullStartDateOK() throws Exception {
        // Given
        RuleMapper ruleMapper = new RuleMapper();
        RuleCategoryModel ruleModel = new RuleCategoryModel();
        ruleModel.getRules().add(new RuleModel("AC-00023", null));
        ruleModel.setPreventInheritance(true);
        ruleModel.addAllPreventRulesId(Lists.newArrayList("AC-00021", "AC-00022"));

        // When
        AccessRuleType accessRuleType = ruleMapper.fillCommonRule(ruleModel, AccessRuleType::new);

        // Then
        assertThat(accessRuleType.getRefNonRuleId()).extracting("value").containsExactly("AC-00021", "AC-00022");
        assertThat(accessRuleType.isPreventInheritance()).isNull();
        assertThat(((RuleIdType) accessRuleType.getRuleAndStartDate().get(0)).getValue()).isEqualTo("AC-00023");
        assertThat(accessRuleType.getRuleAndStartDate()).hasSize(1);
    }

    @Test
    public void shouldNotMapRuleCategoryWithPreventInheritanceAndNonEmptyRefNonRuleId() throws Exception {
        // Given
        RuleMapper ruleMapper = new RuleMapper();
        RuleCategoryModel ruleModel = new RuleCategoryModel();
        ruleModel.getRules().add(new RuleModel("RX", null));
        ruleModel.setPreventInheritance(false);
        ruleModel.addAllPreventRulesId(Lists.newArrayList("DIS-0000X", "DIS-0000Y"));

        // When
        DisseminationRuleType disseminationRuleType = ruleMapper.fillCommonRule(ruleModel, DisseminationRuleType::new);

        // Then
        assertThat(disseminationRuleType.getRefNonRuleId())
            .extracting("value")
            .containsExactly("DIS-0000X", "DIS-0000Y");
        assertThat(disseminationRuleType.isPreventInheritance()).isNull();
        assertThat(((RuleIdType) disseminationRuleType.getRuleAndStartDate().get(0)).getValue()).isEqualTo("RX");
        assertThat(disseminationRuleType.getRuleAndStartDate()).hasSize(1);
    }

    @Test
    public void shouldMapRuleCategoryWithPreventInheritanceAndEmptyRefNonRuleId() throws Exception {
        // Given
        RuleMapper ruleMapper = new RuleMapper();
        RuleCategoryModel ruleModel = new RuleCategoryModel();
        ruleModel.getRules().add(new RuleModel("RX", null));
        ruleModel.setPreventInheritance(false);
        ruleModel.addAllPreventRulesId(Lists.newArrayList());

        // When
        DisseminationRuleType disseminationRuleType = ruleMapper.fillCommonRule(ruleModel, DisseminationRuleType::new);

        // Then
        assertThat(disseminationRuleType.getRefNonRuleId()).extracting("value").asList().isEmpty();
        assertThat(disseminationRuleType.isPreventInheritance()).isFalse();
        assertThat(((RuleIdType) disseminationRuleType.getRuleAndStartDate().get(0)).getValue()).isEqualTo("RX");
        assertThat(disseminationRuleType.getRuleAndStartDate()).hasSize(1);
    }

    @Test
    public void should_map_hold_rule() throws Exception {
        // Given
        RuleMapper ruleMapper = new RuleMapper();

        RuleCategoryModel ruleCategoryModel = new RuleCategoryModel();

        RuleModel ruleModel1 = new RuleModel();
        ruleModel1.setRule("HOL-00001");
        ruleModel1.setStartDate("2001-12-23");
        ruleModel1.setEndDate("2011-12-23");
        ruleModel1.setHoldReason("Reason");
        ruleModel1.setHoldReassessingDate("2021-12-23");

        RuleModel ruleModel2 = new RuleModel();
        ruleModel2.setRule("HOL-00002");
        ruleModel2.setHoldEndDate("2021-12-23");
        ruleModel2.setHoldOwner("Owner");
        ruleModel2.setPreventRearrangement(true);

        RuleModel ruleModel3 = new RuleModel();
        ruleModel3.setRule("HOL-00003");

        ruleCategoryModel.getRules().add(ruleModel1);
        ruleCategoryModel.getRules().add(ruleModel2);
        ruleCategoryModel.getRules().add(ruleModel3);
        ruleCategoryModel.setPreventInheritance(null);
        ruleCategoryModel.addAllPreventRulesId(Collections.emptyList());

        // When
        HoldRuleType accessRuleType = ruleMapper.fillHoldRule(ruleCategoryModel);

        // Then
        checkRuleId(accessRuleType.getHoldRuleDefGroup().get(0), "HOL-00001");
        checkStartDate(accessRuleType.getHoldRuleDefGroup().get(1), "2001-12-23");
        checkHoldReassessingDate(accessRuleType.getHoldRuleDefGroup().get(2), "2021-12-23");
        checkHoldReason(accessRuleType.getHoldRuleDefGroup().get(3), "Reason");

        checkRuleId(accessRuleType.getHoldRuleDefGroup().get(4), "HOL-00002");
        checkHoldEndDate(accessRuleType.getHoldRuleDefGroup().get(5), "2021-12-23");
        checkHoldOwner(accessRuleType.getHoldRuleDefGroup().get(6), "Owner");
        checkPreventRearrangement(accessRuleType.getHoldRuleDefGroup().get(7), true);

        checkRuleId(accessRuleType.getHoldRuleDefGroup().get(8), "HOL-00003");

        // No more entries
        assertThat(accessRuleType.getHoldRuleDefGroup()).hasSize(9);

        assertThat(accessRuleType.getRefNonRuleId()).isEmpty();
        assertThat(accessRuleType.isPreventInheritance()).isNull();
    }

    @Test
    public void should_map_hold_rule_ref_non_rule_ids() throws Exception {
        // Given
        RuleMapper ruleMapper = new RuleMapper();

        RuleCategoryModel ruleCategoryModel = new RuleCategoryModel();

        RuleModel ruleModel1 = new RuleModel();
        ruleModel1.setRule("HOL-00001");

        ruleCategoryModel.getRules().add(ruleModel1);
        ruleCategoryModel.setPreventInheritance(null);
        ruleCategoryModel.addAllPreventRulesId(Arrays.asList("HOL-00002", "HOL-00003"));

        // When
        HoldRuleType accessRuleType = ruleMapper.fillHoldRule(ruleCategoryModel);

        // Then
        checkRuleId(accessRuleType.getHoldRuleDefGroup().get(0), "HOL-00001");
        // No more entries
        assertThat(accessRuleType.getHoldRuleDefGroup()).hasSize(1);

        assertThat(accessRuleType.getRefNonRuleId()).hasSize(2);
        assertThat(accessRuleType.getRefNonRuleId().get(0).getValue()).isEqualTo("HOL-00002");
        assertThat(accessRuleType.getRefNonRuleId().get(1).getValue()).isEqualTo("HOL-00003");
        assertThat(accessRuleType.isPreventInheritance()).isNull();
    }

    @Test
    public void should_map_hold_rule_prevent_inheritance() throws Exception {
        // Given
        RuleMapper ruleMapper = new RuleMapper();

        RuleCategoryModel ruleCategoryModel = new RuleCategoryModel();
        ruleCategoryModel.setPreventInheritance(true);
        ruleCategoryModel.addAllPreventRulesId(Collections.emptyList());

        // When
        HoldRuleType accessRuleType = ruleMapper.fillHoldRule(ruleCategoryModel);

        // Then
        assertThat(accessRuleType.getHoldRuleDefGroup()).isEmpty();
        assertThat(accessRuleType.getRefNonRuleId()).isEmpty();
        assertThat(accessRuleType.isPreventInheritance()).isEqualTo(true);
    }

    private void checkRuleId(JAXBElement<?> jaxbElement, String ruleId) {
        assertThat(jaxbElement.getName().getLocalPart()).isEqualTo(SedaConstants.TAG_RULE_RULE);
        assertThat(((RuleIdType) jaxbElement.getValue()).getValue()).isEqualTo(ruleId);
    }

    private void checkStartDate(JAXBElement<?> jaxbElement, String startDate) {
        checkDate(jaxbElement, startDate, SedaConstants.TAG_RULE_START_DATE);
    }

    private void checkHoldEndDate(JAXBElement<?> jaxbElement, String holdEndDate) {
        checkDate(jaxbElement, holdEndDate, SedaConstants.TAG_RULE_HOLD_END_DATE);
    }

    private void checkHoldOwner(JAXBElement<?> jaxbElement, String holdOwner) {
        assertThat(jaxbElement.getName().getLocalPart()).isEqualTo(SedaConstants.TAG_RULE_HOLD_OWNER);
        assertThat(jaxbElement.getValue()).isEqualTo(holdOwner);
    }

    private void checkHoldReassessingDate(JAXBElement<?> jaxbElement, String holdReassessingDate) {
        checkDate(jaxbElement, holdReassessingDate, SedaConstants.TAG_RULE_HOLD_REASSESSING_DATE);
    }

    private void checkHoldReason(JAXBElement<?> jaxbElement, String holdReason) {
        assertThat(jaxbElement.getName().getLocalPart()).isEqualTo(SedaConstants.TAG_RULE_HOLD_REASON);
        assertThat(jaxbElement.getValue()).isEqualTo(holdReason);
    }

    private void checkPreventRearrangement(JAXBElement<?> jaxbElement, boolean preventRearrangement) {
        assertThat(jaxbElement.getName().getLocalPart()).isEqualTo(SedaConstants.TAG_RULE_PREVENT_REARRANGEMENT);
        assertThat(jaxbElement.getValue()).isInstanceOf(Boolean.class);
        assertThat(((Boolean) jaxbElement.getValue())).isEqualTo(preventRearrangement);
    }

    private void checkDate(JAXBElement<?> jaxbElement, String expectedDate, String tagName) {
        assertThat(jaxbElement.getName().getLocalPart()).isEqualTo(tagName);
        assertThat(jaxbElement.getValue()).isInstanceOf(XMLGregorianCalendar.class);
        assertThat(jaxbElement.getValue().toString()).isEqualTo(expectedDate);
    }
}
