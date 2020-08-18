package fr.gouv.vitam.common.model.administration;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FileRulesModelTest {

    @Test
    public void testNotEqualsWithNull() {
        FileRulesModel rule = new FileRulesModel("APP-00001",
                "AppraisalRule",
                "Dossier individuel d’agent civil",
                "ruleDescription",
                "80",
                "YEAR");
        assertFalse(rule.equals(null)); // FileRulesModel::equals is overrided so do not use AssertNotEquals(null, rule)
    }

    @Test
    public void testEquals() {
        FileRulesModel rule = new FileRulesModel("APP-00001",
                "AppraisalRule",
                "Dossier individuel d’agent civil",
                "ruleDescription",
                "80",
                "YEAR");

        FileRulesModel sameRule = new FileRulesModel("APP-00001",
                "AppraisalRule",
                "Dossier individuel d’agent civil",
                "ruleDescription",
                "80",
                "YEAR");
        assertTrue(rule.equals(sameRule)); // FileRulesModel::equals is overrided so do not use AssertNotEquals(null, rule)
    }

    @Test
    public void shouldReturnNotEqualsWhenRuleIdIsDifferent() {
        FileRulesModel rule = new FileRulesModel("APP-00001",
                "AppraisalRule",
                "Dossier individuel d’agent civil",
                "ruleDescription",
                "80",
                "YEAR");

        FileRulesModel notSameRule = new FileRulesModel("APP-00002",
                "AppraisalRule",
                "Dossier individuel d’agent civil",
                "ruleDescription",
                "80",
                "YEAR");
        assertFalse(rule.equals(notSameRule)); // FileRulesModel::equals is overrided so do not use AssertNotEquals(null, rule)
    }

    @Test
    public void shouldReturnNotEqualsWhenRuleTypeDifferent() {
        FileRulesModel rule = new FileRulesModel("APP-00001",
                "AccessRule",
                "Dossier individuel d’agent civil",
                "ruleDescription",
                "80",
                "YEAR");

        FileRulesModel notSameRule = new FileRulesModel("APP-00001",
                "AppraisalRule",
                "Dossier individuel d’agent civil",
                "ruleDescription",
                "80",
                "YEAR");
        assertFalse(rule.equals(notSameRule)); // FileRulesModel::equals is overrided so do not use AssertNotEquals(null, rule)
    }

    @Test
    public void shouldReturnNotEqualsWhenRuleValueIsDifferent() {
        FileRulesModel rule = new FileRulesModel("APP-00001",
                "AppraisalRule",
                "Dossier collectif d’agent civil",
                "ruleDescription",
                "80",
                "YEAR");

        FileRulesModel notSameRule = new FileRulesModel("APP-00001",
                "AppraisalRule",
                "Dossier individuel d’agent civil",
                "ruleDescription",
                "80",
                "YEAR");
        assertFalse(rule.equals(notSameRule)); // FileRulesModel::equals is overrided so do not use AssertNotEquals(null, rule)
    }

    @Test
    public void shouldReturnNotEqualsWhenRuleDescriptionIsDifferent() {
        FileRulesModel rule = new FileRulesModel("APP-00001",
                "AppraisalRule",
                "Dossier individuel d’agent civil",
                "ruleDescription",
                "80",
                "YEAR");

        FileRulesModel notSameRule = new FileRulesModel("APP-00001",
                "AppraisalRule",
                "Dossier individuel d’agent civil",
                "description de la règle",
                "80",
                "YEAR");
        assertFalse(rule.equals(notSameRule)); // FileRulesModel::equals is overrided so do not use AssertNotEquals(null, rule)
    }

    @Test
    public void shouldReturnNotEqualsWhenRuleDurationIsDifferent() {
        FileRulesModel rule = new FileRulesModel("APP-00001",
                "AppraisalRule",
                "Dossier individuel d’agent civil",
                "ruleDescription",
                "1200",
                "YEAR");

        FileRulesModel notSameRule = new FileRulesModel("APP-00001",
                "AppraisalRule",
                "Dossier individuel d’agent civil",
                "ruleDescription",
                "80",
                "YEAR");
        assertFalse(rule.equals(notSameRule)); // FileRulesModel::equals is overrided so do not use AssertNotEquals(null, rule)
    }

    @Test
    public void shouldReturnNotEqualsWhenRuleMeasuremnetIsDifferent() {
        FileRulesModel rule = new FileRulesModel("APP-00001",
                "AppraisalRule",
                "Dossier individuel d’agent civil",
                "ruleDescription",
                "80",
                "MONTH");

        FileRulesModel notSameRule = new FileRulesModel("APP-00001",
                "AppraisalRule",
                "Dossier individuel d’agent civil",
                "ruleDescription",
                "80",
                "YEAR");
        assertFalse(rule.equals(notSameRule)); // FileRulesModel::equals is overrided so do not use AssertNotEquals(null, rule)
    }

    @Test
    public void shouldReturnTrueIfRulesHaveSameId() {
        FileRulesModel rule = new FileRulesModel("APP-00001",
                "AppraisalRule",
                "Dossier individuel d’agent civil",
                "ruleDescription",
                "80",
                "MONTH");

        FileRulesModel sameRuleId = new FileRulesModel("APP-00001",
                "AppraisalRule",
                "Dossier individuel d’agent civil",
                "ruleDescription",
                "80",
                "YEAR");
        assertTrue(rule.hasSameRuleId(sameRuleId));
    }

    @Test
    public void shouldReturnFalseIfRulesHaveSameId() {
        FileRulesModel rule = new FileRulesModel("APP-00001",
                "AppraisalRule",
                "Dossier individuel d’agent civil",
                "ruleDescription",
                "80",
                "MONTH");

        FileRulesModel notSameRuleId = new FileRulesModel("APP-00002",
                "AppraisalRule",
                "Dossier individuel d’agent civil",
                "ruleDescription",
                "80",
                "MONTH");
        assertFalse(rule.hasSameRuleId(notSameRuleId));
    }
}