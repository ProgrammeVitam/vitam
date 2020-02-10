/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.worker.core.plugin.elimination;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.VitamConstants;
import fr.gouv.vitam.common.model.elimination.EliminationRequestBody;
import fr.gouv.vitam.metadata.core.rules.MetadataRuleService;
import fr.gouv.vitam.common.model.rules.InheritedRuleCategoryResponseModel;
import fr.gouv.vitam.common.model.rules.UnitInheritedRulesResponseModel;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.exception.ProcessingStatusException;
import fr.gouv.vitam.worker.core.plugin.elimination.model.EliminationAnalysisResult;
import fr.gouv.vitam.worker.core.plugin.elimination.model.EliminationEventDetails;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

import java.io.IOException;
import java.time.LocalDate;

public final class EliminationUtils {

    private static final String REQUEST_JSON = "request.json";
    private static final String INVALID_REQUEST = "Invalid request";
    private static final String COULD_NOT_LOAD_REQUEST_FROM_WORKSPACE = "Could not load request from workspace";

    private EliminationUtils() {
        // Private constructor
    }

    public static EliminationAnalysisResult computeEliminationAnalysisForUnitWithInheritedRules(JsonNode unit,
        EliminationAnalysisService eliminationAnalysisService,
        WorkerParameters param,
        LocalDate expirationDate) throws ProcessingStatusException {

        InheritedRuleCategoryResponseModel inheritedRuleCategory =
            EliminationUtils.parseAppraisalRuleCategory(unit);

        String originatingAgency = unit.has(VitamFieldsHelper.originatingAgency()) ?
            unit.get(VitamFieldsHelper.originatingAgency()).asText() :
            null;

        return eliminationAnalysisService.analyzeElimination(
            param.getRequestId(),
            inheritedRuleCategory.getRules(),
            inheritedRuleCategory.getProperties(),
            expirationDate,
            originatingAgency);
    }


    private static InheritedRuleCategoryResponseModel parseAppraisalRuleCategory(JsonNode unit)
        throws ProcessingStatusException {

        try {
            JsonNode inheritedRules = unit.get(MetadataRuleService.INHERITED_RULES);

            UnitInheritedRulesResponseModel unitInheritedRulesResponseModel =
                JsonHandler.getFromJsonNode(inheritedRules, UnitInheritedRulesResponseModel.class);

            return unitInheritedRulesResponseModel.getRuleCategories().get(VitamConstants.TAG_RULE_APPRAISAL);

        } catch (InvalidParseOperationException e) {
            throw new ProcessingStatusException(StatusCode.FATAL, "Could not parse unit information", e);
        }
    }

    public static EliminationRequestBody loadRequestJsonFromWorkspace(HandlerIO handler) throws ProcessingStatusException {
        try {
            return JsonHandler.getFromInputStream(
                handler.getInputStreamFromWorkspace(REQUEST_JSON), EliminationRequestBody.class);
        } catch (ContentAddressableStorageServerException | ContentAddressableStorageNotFoundException | IOException e) {
            throw new ProcessingStatusException(StatusCode.FATAL, COULD_NOT_LOAD_REQUEST_FROM_WORKSPACE, e);
        } catch (InvalidParseOperationException e) {
            EliminationEventDetails eventDetails = new EliminationEventDetails()
                .setError(INVALID_REQUEST);
            throw new ProcessingStatusException(StatusCode.KO, eventDetails, INVALID_REQUEST, e);
        }
    }
}
