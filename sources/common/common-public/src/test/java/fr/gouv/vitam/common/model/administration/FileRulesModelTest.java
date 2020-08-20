/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
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