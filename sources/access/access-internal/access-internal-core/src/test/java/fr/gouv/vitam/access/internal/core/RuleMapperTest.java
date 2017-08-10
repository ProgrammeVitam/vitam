package fr.gouv.vitam.access.internal.core;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Lists;
import fr.gouv.culture.archivesdefrance.seda.v2.AccessRuleType;
import fr.gouv.culture.archivesdefrance.seda.v2.RuleIdType;
import fr.gouv.vitam.common.model.unit.RuleCategoryModel;
import fr.gouv.vitam.common.model.unit.RuleModel;
import org.apache.commons.codec.language.bm.RuleType;
import org.junit.Test;

public class RuleMapperTest {

    @Test
    public void should_map_common_rule() throws Exception {
        // Given
        RuleMapper ruleMapper = new RuleMapper();
        RuleCategoryModel ruleModel = new RuleCategoryModel();
        ruleModel.getRules().add(new RuleModel("AC-00023", "2017-04-01"));
        ruleModel.setPreventInheritance(true);
        ruleModel.addPreventRulesId(Lists.newArrayList("AC-00021", "AC-00022"));

        // When
        AccessRuleType accessRuleType = ruleMapper.fillCommonRule(ruleModel, AccessRuleType::new);

        // Then
        assertThat(accessRuleType.getRefNonRuleId()).extracting("value").containsExactly("AC-00021", "AC-00022");
        assertThat(accessRuleType.isPreventInheritance()).isTrue();
        assertThat(((RuleIdType)accessRuleType.getRuleAndStartDate().get(0)).getValue()).isEqualTo("AC-00023");
        assertThat(accessRuleType.getRuleAndStartDate().get(1).toString()).isEqualTo("2017-04-01");
    }

}
