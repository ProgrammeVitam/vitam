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

package fr.gouv.vitam.functional.administration.common.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.json.JsonHandler;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static fr.gouv.vitam.common.model.unit.RuleModel.END_DATE;
import static fr.gouv.vitam.common.model.unit.RuleModel.START_DATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ArchiveUnitUpdateUtilsTest {

    private static final String RULE_ID = "RULE-ID";
    private static final String UPDATING_RULE =
        "{\"Rule\":\"RULE-ID\",\"StartDate\":\"2000-01-01\",\"PreventRearrangement\":true}";
    private static final String RULE_MODEL_1_YEAR =
        "{\"RuleId\":\"RULE-ID\",\"RuleType\":\"RULE_TYPE\",\"RuleValue\":\"RULE_VALUE\",\"RuleDescription\":\"RULE_DESCRIPTION\",\"RuleDuration\":\"1\",\"RuleMeasurement\":\"Year\",\"CreationDate\":\"CREATION_DATE\",\"UpdateDate\":\"UPDATE_DATE\"}";
    private static final String RULE_MODEL_UNLIMITED =
        "{\"RuleId\":\"RULE-ID\",\"RuleType\":\"RULE_TYPE\",\"RuleValue\":\"RULE_VALUE\",\"RuleDescription\":\"RULE_DESCRIPTION\",\"RuleDuration\":\"unlimited\",\"RuleMeasurement\":\"Year\",\"CreationDate\":\"CREATION_DATE\",\"UpdateDate\":\"UPDATE_DATE\"}";
    private static final String RULE_MODEL_WITHOUT_DURATION =
        "{\"RuleId\":\"RULE-ID\",\"RuleType\":\"RULE_TYPE\",\"RuleValue\":\"RULE_VALUE\",\"RuleDescription\":\"RULE_DESCRIPTION\",\"RuleMeasurement\":\"Year\",\"CreationDate\":\"CREATION_DATE\",\"UpdateDate\":\"UPDATE_DATE\"}";

    private ObjectNode updatingRule;

    @Before
    public void setUp() throws Exception {
        updatingRule = JsonHandler.getFromString(UPDATING_RULE, ObjectNode.class);
    }

    @Test
    public void should_compute_endDate_when_rule_duration_is_present() throws Exception {
        JsonNode ruleModel = JsonHandler.getFromString(RULE_MODEL_1_YEAR);
        JsonNode updatedRule = ArchiveUnitUpdateUtils.computeEndDate(updatingRule, ruleModel);
        assertThat(updatedRule.get(END_DATE)).isNotNull();
        assertThat(updatedRule.get(END_DATE).asText()).isEqualTo("2001-01-01");
    }

    @Test
    public void should_compute_endDate_when_rule_duration_is_unlimited() throws Exception {
        JsonNode ruleModel = JsonHandler.getFromString(RULE_MODEL_UNLIMITED);
        JsonNode updatedRule = ArchiveUnitUpdateUtils.computeEndDate(updatingRule, ruleModel);
        assertThat(updatedRule.get(END_DATE)).isNull();
    }

    @Test
    public void should_compute_endDate_when_rule_duration_is_not_present() throws Exception {
        JsonNode ruleModel = JsonHandler.getFromString(RULE_MODEL_WITHOUT_DURATION);
        JsonNode updatedRule = ArchiveUnitUpdateUtils.computeEndDate(updatingRule, ruleModel);
        assertThat(updatedRule.get(END_DATE)).isNull();
    }

    @Test
    public void should_update_category_rules_when_rule_is_present() throws Exception {
        JsonNode ruleModel = JsonHandler.getFromString(RULE_MODEL_1_YEAR);

        boolean result = ArchiveUnitUpdateUtils.updateCategoryRules(
            JsonHandler.createArrayNode().add(updatingRule),
            List.of(ruleModel),
            new UpdateMultiQuery(),
            RULE_ID
        );

        assertTrue(result);
    }

    @Test
    public void should_not_update_category_rules_when_rule_not_found() throws Exception {
        JsonNode ruleModel = JsonHandler.getFromString(RULE_MODEL_1_YEAR);
        updatingRule.remove(START_DATE);

        boolean result = ArchiveUnitUpdateUtils.updateCategoryRules(
            JsonHandler.createArrayNode().add(updatingRule),
            List.of(ruleModel),
            new UpdateMultiQuery(),
            RULE_ID
        );

        assertFalse(result);
    }
}
