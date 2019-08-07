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

import com.google.common.collect.Lists;
import fr.gouv.culture.archivesdefrance.seda.v2.AccessRuleType;
import fr.gouv.culture.archivesdefrance.seda.v2.DisseminationRuleType;
import fr.gouv.culture.archivesdefrance.seda.v2.RuleIdType;
import fr.gouv.culture.archivesdefrance.seda.v2.StorageRuleType;
import fr.gouv.vitam.common.model.unit.RuleCategoryModel;
import fr.gouv.vitam.common.model.unit.RuleModel;
import org.junit.Test;

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
        assertThat(((RuleIdType)accessRuleType.getRuleAndStartDate().get(0)).getValue()).isEqualTo("AC-00023");
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
        assertThat(((RuleIdType)accessRuleType.getRuleAndStartDate().get(0)).getValue()).isEqualTo("AC-00023");
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
        assertThat(disseminationRuleType.getRefNonRuleId()).extracting("value").containsExactly("DIS-0000X", "DIS-0000Y");
        assertThat(disseminationRuleType.isPreventInheritance()).isNull();
        assertThat(((RuleIdType)disseminationRuleType.getRuleAndStartDate().get(0)).getValue()).isEqualTo("RX");
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
        assertThat(((RuleIdType)disseminationRuleType.getRuleAndStartDate().get(0)).getValue()).isEqualTo("RX");
        assertThat(disseminationRuleType.getRuleAndStartDate()).hasSize(1);
    }
}
