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

package fr.gouv.vitam.worker.core.utils;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.rules.InheritedRuleCategoryResponseModel;
import fr.gouv.vitam.common.model.rules.InheritedRuleResponseModel;
import fr.gouv.vitam.worker.core.exception.ProcessingStatusException;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class HoldRuleUtilsTest {

    @Test
    public void testParseHoldRuleCategory() throws Exception {
        // Given
        JsonNode unit = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream("InheritedRules/unitWithInheritedHoldRules2.json")
        );

        // When
        InheritedRuleCategoryResponseModel holdRuleCategory = HoldRuleUtils.parseHoldRuleCategory(unit);

        // Then
        assertThat(holdRuleCategory.getProperties()).isEmpty();
        assertThat(holdRuleCategory.getRules()).hasSize(3);

        assertThat(holdRuleCategory.getRules().get(0).getRuleId()).isEqualTo("HOL-00001");
        assertThat(holdRuleCategory.getRules().get(0).getEndDate()).isEqualTo("2001-01-01");

        assertThat(holdRuleCategory.getRules().get(1).getRuleId()).isEqualTo("HOL-00002");
        assertThat(holdRuleCategory.getRules().get(1).getEndDate()).isEqualTo("2051-01-01");
        assertThat(
            holdRuleCategory.getRules().get(1).getExtendedRuleAttributes().get(SedaConstants.TAG_RULE_HOLD_END_DATE)
        ).isEqualTo("2051-01-01");

        assertThat(holdRuleCategory.getRules().get(2).getRuleId()).isEqualTo("HOL-00004");
        assertThat(holdRuleCategory.getRules().get(2).getEndDate()).isNull();
    }

    @Test
    public void testListActiveHoldRulesWithEmptyHoldRuleListThenEmptyResult() throws Exception {
        // Given
        InheritedRuleCategoryResponseModel holdRuleCategory = loadHoldRuleTestSet(
            "InheritedRules/unitWithInheritedHoldRules1.json"
        );
        LocalDate expirationDate = LocalDate.parse("2001-01-01", DateTimeFormatter.ISO_LOCAL_DATE);

        // When
        Set<InheritedRuleResponseModel> activeHoldRules = HoldRuleUtils.listActiveHoldRules(
            "unit4",
            holdRuleCategory.getRules(),
            expirationDate
        );

        // Then
        assertThat(activeHoldRules).isEmpty();
    }

    @Test
    public void testListActiveHoldRules() throws Exception {
        // Given
        InheritedRuleCategoryResponseModel holdRuleCategory = loadHoldRuleTestSet(
            "InheritedRules/unitWithInheritedHoldRules2.json"
        );
        LocalDate expirationDate = LocalDate.parse("2000-12-31", DateTimeFormatter.ISO_LOCAL_DATE);

        // When
        Set<InheritedRuleResponseModel> activeHoldRules = HoldRuleUtils.listActiveHoldRules(
            "unit4",
            holdRuleCategory.getRules(),
            expirationDate
        );

        // Then
        assertThat(activeHoldRules).hasSize(3);
        assertThat(activeHoldRules.stream().map(InheritedRuleResponseModel::getRuleId)).containsExactlyInAnyOrder(
            "HOL-00001",
            "HOL-00002",
            "HOL-00004"
        );
    }

    @Test
    public void testListActiveHoldRulesWithExpiredRule() throws Exception {
        // Given
        InheritedRuleCategoryResponseModel holdRuleCategory = loadHoldRuleTestSet(
            "InheritedRules/unitWithInheritedHoldRules2.json"
        );
        LocalDate expirationDate = LocalDate.parse("2001-01-01", DateTimeFormatter.ISO_LOCAL_DATE);

        // When
        Set<InheritedRuleResponseModel> activeHoldRules = HoldRuleUtils.listActiveHoldRules(
            "unit4",
            holdRuleCategory.getRules(),
            expirationDate
        );

        // Then
        assertThat(activeHoldRules).hasSize(2);
        assertThat(activeHoldRules.stream().map(InheritedRuleResponseModel::getRuleId)).containsExactlyInAnyOrder(
            "HOL-00002",
            "HOL-00004"
        );
    }

    private InheritedRuleCategoryResponseModel loadHoldRuleTestSet(String resourceFile)
        throws InvalidParseOperationException, FileNotFoundException, ProcessingStatusException {
        JsonNode unit = JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(resourceFile));
        return HoldRuleUtils.parseHoldRuleCategory(unit);
    }
}
