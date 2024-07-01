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
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.VitamConstants;
import fr.gouv.vitam.common.model.rules.InheritedRuleCategoryResponseModel;
import fr.gouv.vitam.common.model.rules.InheritedRuleResponseModel;
import fr.gouv.vitam.common.model.rules.UnitInheritedRulesResponseModel;
import fr.gouv.vitam.metadata.core.rules.MetadataRuleService;
import fr.gouv.vitam.worker.core.exception.ProcessingStatusException;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class HoldRuleUtils {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(HoldRuleUtils.class);

    private HoldRuleUtils() {
        // Private constructor for utility class
    }

    public static InheritedRuleCategoryResponseModel parseHoldRuleCategory(JsonNode unit)
        throws ProcessingStatusException {
        try {
            JsonNode inheritedRules = unit.get(MetadataRuleService.INHERITED_RULES);

            UnitInheritedRulesResponseModel unitInheritedRulesResponseModel = JsonHandler.getFromJsonNode(
                inheritedRules,
                UnitInheritedRulesResponseModel.class
            );
            return unitInheritedRulesResponseModel.getRuleCategories().get(VitamConstants.TAG_RULE_HOLD);
        } catch (InvalidParseOperationException e) {
            throw new ProcessingStatusException(StatusCode.FATAL, "Could not parse unit information", e);
        }
    }

    public static Set<InheritedRuleResponseModel> listActiveHoldRules(
        String unitId,
        List<InheritedRuleResponseModel> holdRules,
        LocalDate expirationDate
    ) {
        if (holdRules.isEmpty()) {
            LOGGER.debug("No hold rules found for unit " + unitId);
            return Collections.emptySet();
        }

        Set<InheritedRuleResponseModel> activeHoldRules = holdRules
            .stream()
            .filter(holdRule -> {
                LocalDate endDate = LocalDateUtil.parseDate(holdRule.getEndDate());
                return (endDate == null || expirationDate.isBefore(endDate));
            })
            .collect(Collectors.toSet());

        if (activeHoldRules.isEmpty()) {
            LOGGER.debug("No active hold rule as of " + expirationDate + " for unit " + unitId);
        } else {
            LOGGER.info("Active hold rule(s) found " + activeHoldRules + " for unit " + unitId);
        }

        return activeHoldRules;
    }
}
