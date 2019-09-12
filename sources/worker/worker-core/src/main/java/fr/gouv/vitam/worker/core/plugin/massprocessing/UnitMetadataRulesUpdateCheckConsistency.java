/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.worker.core.plugin.massprocessing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.ArchiveUnitProfileModel;
import fr.gouv.vitam.common.model.administration.ArchiveUnitProfileStatus;
import fr.gouv.vitam.common.model.massupdate.RuleAction;
import fr.gouv.vitam.common.model.massupdate.RuleActions;
import fr.gouv.vitam.common.model.massupdate.RuleCategoryAction;
import fr.gouv.vitam.common.utils.ClassificationLevelUtil;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialNotFoundException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Check update permissions.
 */
public class UnitMetadataRulesUpdateCheckConsistency extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(UnitMetadataRulesUpdateCheckConsistency.class);

    /**
     * UNIT_METADATA_CHECK_CONSISTENCY
     */
    private static final String UNIT_METADATA_CHECK_CONSISTENCY = "UNIT_METADATA_CHECK_CONSISTENCY";

    /**
     * AdminManagementClientFactory
     */
    private AdminManagementClientFactory adminManagementClientFactory;

    /**
     * Constructor.
     */
    public UnitMetadataRulesUpdateCheckConsistency() {
        this(AdminManagementClientFactory.getInstance());
    }

    /**
     * Constructor.
     *
     * @param adminManagementClientFactory admin management client
     */
    @VisibleForTesting
    public UnitMetadataRulesUpdateCheckConsistency(
        AdminManagementClientFactory adminManagementClientFactory) {
        this.adminManagementClientFactory = adminManagementClientFactory;
    }

    /**
     * Execute an action
     *
     * @param param {@link WorkerParameters}
     * @param handler the handlerIo
     * @return CompositeItemStatus:response contains a list of functional message and status code
     * @throws ProcessingException if an error is encountered when executing the action
     */
    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler)
        throws ProcessingException {

        checkMandatoryParameters(param);
        final ItemStatus itemStatus = new ItemStatus(UNIT_METADATA_CHECK_CONSISTENCY);

        // FIXME: Use in/out in order to transfer json from a step to another ?
        JsonNode queryActions = handler.getJsonFromWorkspace("actions.json");
        if (JsonHandler.isNullOrEmpty(queryActions)) {
            itemStatus.increment(StatusCode.KO);
            return new ItemStatus(UNIT_METADATA_CHECK_CONSISTENCY)
                .setItemsStatus(UNIT_METADATA_CHECK_CONSISTENCY, itemStatus);
        }

        try {
            JsonNode errorEvDetData = getErrorFromActionQuery(queryActions);
            if (errorEvDetData != null) {
                itemStatus.increment(StatusCode.KO);
                itemStatus.setMessage(VitamCode.UPDATE_UNIT_RULES_CONSISTENCY.name());
                itemStatus.setEvDetailData(JsonHandler.unprettyPrint(errorEvDetData));
                return new ItemStatus(UNIT_METADATA_CHECK_CONSISTENCY)
                    .setItemsStatus(UNIT_METADATA_CHECK_CONSISTENCY, itemStatus);
            }
            ;
        } catch (final VitamException | IllegalStateException e) {
            throw new ProcessingException(e);
        }

        itemStatus.increment(StatusCode.OK);
        return new ItemStatus(UNIT_METADATA_CHECK_CONSISTENCY)
            .setItemsStatus(UNIT_METADATA_CHECK_CONSISTENCY, itemStatus);
    }

    private JsonNode getErrorFromActionQuery(JsonNode queryActions) throws InvalidParseOperationException {
        RuleActions ruleActions = JsonHandler.getFromJsonNode(queryActions, RuleActions.class);

        Optional<JsonNode> potentialErrorInManagement = computeErrorsForManagementMetadata(ruleActions);
        if (potentialErrorInManagement.isPresent()) {
            return potentialErrorInManagement.get();
        }

        Optional<JsonNode> checkError = ruleActions.getUpdate().stream()
            .flatMap(x -> x.entrySet().stream())
            .map(this::computeErrorsForUpdate)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst();
        if (checkError.isPresent())
            return checkError.get();

        checkError = ruleActions.getAdd().stream()
            .flatMap(x -> x.entrySet().stream())
            .map(this::computeErrorsForAdd)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst();
        if (checkError.isPresent())
            return checkError.get();

        return checkError.orElse(null);
    }

    private Optional<JsonNode> computeErrorsForManagementMetadata(RuleActions ruleActions) {
        if (ruleActions.getAddOrUpdateMetadata() != null
            && ruleActions.getAddOrUpdateMetadata().getArchiveUnitProfile() != null) {
            Optional<JsonNode> error = checkAUPId(ruleActions.getAddOrUpdateMetadata().getArchiveUnitProfile());
            if (error != null) {
                return error;
            }
        }
        return Optional.empty();
    }

    private Optional<JsonNode> checkAUPId(String aupId) {
        try {
            RequestResponse<ArchiveUnitProfileModel> aup =
                adminManagementClientFactory.getClient().findArchiveUnitProfilesByID(aupId);
            if (!aup.isOk()) {
                throw new IllegalStateException("Error while get the ArchiveUnitProfile in Referential");
            }

            ArchiveUnitProfileModel archiveUnitProfile =
                ((RequestResponseOK<ArchiveUnitProfileModel>) aup).getFirstResult();
            if (archiveUnitProfile == null) {
                throw new IllegalStateException("Error while get the ArchiveUnitProfile in Referential");
            }

            if (!ArchiveUnitProfileStatus.ACTIVE.equals(archiveUnitProfile.getStatus())) {
                ObjectNode errorInfo = JsonHandler.createObjectNode();
                errorInfo.put("Error", VitamCode.UPDATE_UNIT_MANAGEMENT_METADATA_CONSISTENCY.name());
                errorInfo.put("Message", VitamCode.UPDATE_UNIT_MANAGEMENT_METADATA_CONSISTENCY.getMessage());
                errorInfo.put("Info ", "ArchiveUnitProfile " + aupId + " is not ACTIVE");
                errorInfo.put("Code", "CHECK_UNIT_PROFILE_INACTIVE");
                return Optional.of(errorInfo);
            }

            if (archiveUnitProfile.getControlSchema() == null ||
                JsonHandler.isEmpty(archiveUnitProfile.getControlSchema())) {
                ObjectNode errorInfo = JsonHandler.createObjectNode();
                errorInfo.put("Error", VitamCode.UPDATE_UNIT_MANAGEMENT_METADATA_CONSISTENCY.name());
                errorInfo.put("Message", VitamCode.UPDATE_UNIT_MANAGEMENT_METADATA_CONSISTENCY.getMessage());
                errorInfo.put("Info ", "ArchiveUnitProfile " + aupId + " havn't a valid Control Schema");
                errorInfo.put("Code", "CHECK_UNIT_PROFILE_CONSISTENCY");
                return Optional.of(errorInfo);
            }

        } catch (ReferentialNotFoundException e) {
            ObjectNode errorInfo = JsonHandler.createObjectNode();
            errorInfo.put("Error", VitamCode.UPDATE_UNIT_MANAGEMENT_METADATA_CONSISTENCY.name());
            errorInfo.put("Message", VitamCode.UPDATE_UNIT_MANAGEMENT_METADATA_CONSISTENCY.getMessage());
            errorInfo.put("Info ", "ArchiveUnitProfile " + aupId + " is not in database");
            errorInfo.put("Code", "CHECK_UNIT_PROFILE_UNKNOWN");
            return Optional.of(errorInfo);
        } catch (InvalidParseOperationException | AdminManagementClientServerException e) {
            throw new IllegalStateException("Error while get the ArchiveUnitProfile in Referential");
        }

        return Optional.empty();
    }

    private Optional<JsonNode> checkDate(String startDateAsString, String category) {
        try {
            if (startDateAsString == null)
                return Optional.empty();
            LocalDate startDate = LocalDate.parse(startDateAsString);
            if (startDate.getYear() >= 9000) {
                ObjectNode errorInfo = JsonHandler.createObjectNode();
                errorInfo.put("Error", VitamCode.UPDATE_UNIT_RULES_QUERY_CONSISTENCY.name());
                errorInfo.put("Message", VitamCode.UPDATE_UNIT_RULES_QUERY_CONSISTENCY.getMessage());
                errorInfo.put("Info ", "'StartDate' must be prior than year 9000 for category " + category);
                errorInfo.put("Code", "UNIT_METADATA_UPDATE_CHECK_RULES_DATE_UNAUTHORIZED");
                return Optional.of(errorInfo);
            }
        } catch (DateTimeParseException e) {
            ObjectNode errorInfo = JsonHandler.createObjectNode();
            errorInfo.put("Error", VitamCode.UPDATE_UNIT_RULES_QUERY_CONSISTENCY.name());
            errorInfo.put("Message", VitamCode.UPDATE_UNIT_RULES_QUERY_CONSISTENCY.getMessage());
            errorInfo.put("Info ", "'StartDate' must follow 'AAAA-MM-DD' format " + category);
            errorInfo.put("Code", "UNIT_METADATA_UPDATE_CHECK_RULES_DATE_WRONG_FORMAT");
            return Optional.of(errorInfo);
        }
        return Optional.empty();

    }

    private Optional<JsonNode> computeErrorsForUpdate(Map.Entry<String, RuleCategoryAction> entry) {
        Optional<JsonNode> response = computeErrorsForCategory(entry);
        if (response.isPresent()) {
            return response;
        }

        if (entry.getValue().getRules() == null) {
            return Optional.empty();
        }

        for (RuleAction ruleAction : entry.getValue().getRules()) {
            if (StringUtils.isEmpty(ruleAction.getOldRule())) {
                ObjectNode errorInfo = JsonHandler.createObjectNode();
                errorInfo.put("Error", VitamCode.UPDATE_UNIT_RULES_QUERY_CONSISTENCY.name());
                errorInfo.put("Message", VitamCode.UPDATE_UNIT_RULES_QUERY_CONSISTENCY.getMessage());
                errorInfo.put("Info ", "Rule update should define an 'OldRule' to be updated for category " + entry.getKey());
                errorInfo.put("Code", "UNIT_RULES_MISSING_MANDATORY_FIELD");
                return Optional.of(errorInfo);
            }

            if (StringUtils.isEmpty(ruleAction.getRule())
                && StringUtils.isEmpty(ruleAction.getStartDate())
                && (ruleAction == null || Boolean.FALSE.equals(ruleAction.isDeleteStartDate()))) {

                ObjectNode errorInfo = JsonHandler.createObjectNode();
                errorInfo.put("Error", VitamCode.UPDATE_UNIT_RULES_QUERY_CONSISTENCY.name());
                errorInfo.put("Message", VitamCode.UPDATE_UNIT_RULES_QUERY_CONSISTENCY.getMessage());
                errorInfo.put("Info ", "Rule update should define new 'Rule', 'StartDate' or 'DeleteStartDate' for category " + entry.getKey());
                errorInfo.put("Code", "UNIT_RULES_NOT_EXPECTED_FIELD");
                return Optional.of(errorInfo);
            }

            Optional<JsonNode> checkDateResponse = checkDate(ruleAction.getStartDate(), entry.getKey());
            if (checkDateResponse.isPresent()) {
                return checkDateResponse;
            }
        }

        return Optional.empty();
    }

    private Optional<JsonNode> computeErrorsForAdd(Map.Entry<String, RuleCategoryAction> entry) {
        Optional<JsonNode> response = computeErrorsForCategory(entry);
        if (response.isPresent()) {
            return response;
        }

        for (RuleAction ruleAction : entry.getValue().getRules()) {
            if (StringUtils.isEmpty(ruleAction.getRule())) {
                ObjectNode errorInfo = JsonHandler.createObjectNode();
                errorInfo.put("Error", VitamCode.UPDATE_UNIT_RULES_QUERY_CONSISTENCY.name());
                errorInfo.put("Message", VitamCode.UPDATE_UNIT_RULES_QUERY_CONSISTENCY.getMessage());
                errorInfo.put("Info ", "New rule must at least define the field 'Rule' for category " + entry.getKey());
                errorInfo.put("Code", "UNIT_RULES_MISSING_MANDATORY_FIELD");
                return Optional.of(errorInfo);
            }

            if (StringUtils.isNotEmpty(ruleAction.getOldRule()) ||
                Boolean.TRUE.equals(ruleAction.isDeleteStartDate())) {
                ObjectNode errorInfo = JsonHandler.createObjectNode();
                errorInfo.put("Error", VitamCode.UPDATE_UNIT_RULES_QUERY_CONSISTENCY.name());
                errorInfo.put("Message", VitamCode.UPDATE_UNIT_RULES_QUERY_CONSISTENCY.getMessage());
                errorInfo.put("Info ", "New rule must not define 'OldRule' nor 'DeleteStartDate' fields for category " + entry.getKey());
                errorInfo.put("Code", "UNIT_RULES_NOT_EXPECTED_FIELD");
                return Optional.of(errorInfo);
            }

            Optional<JsonNode> checkDateResponse = checkDate(ruleAction.getStartDate(), entry.getKey());
            if (checkDateResponse.isPresent()) {
                return checkDateResponse;
            }
        }

        return Optional.empty();
    }

    private Optional<JsonNode> computeErrorsForCategory(Map.Entry<String, RuleCategoryAction> entry) {
        JsonNode rulesIDErrors = computeOnlyRulesID(entry);
        if (rulesIDErrors != null) {
            return Optional.of(rulesIDErrors);
        }

        String categoryName = entry.getKey();
        RuleCategoryAction category = entry.getValue();

        if (SedaConstants.TAG_RULE_CLASSIFICATION.equals(categoryName)) {
            String classificationLevel = category.getClassificationLevel();
            if (classificationLevel != null && !ClassificationLevelUtil.checkClassificationLevel(classificationLevel)) {
                ObjectNode errorInfo = JsonHandler.createObjectNode();
                errorInfo.put("Error", VitamCode.UPDATE_UNIT_RULES_PROPERTY_CONSISTENCY.name());
                errorInfo.put("Message", VitamCode.UPDATE_UNIT_RULES_PROPERTY_CONSISTENCY.getMessage());
                errorInfo.put("Info ", classificationLevel + " is not a valid value for ClassificationLevel");
                errorInfo.put("Code", "CHECK_CLASSIFICATION_LEVEL");
                return Optional.of(errorInfo);
            }
        }

        return Optional.empty();
    }

    private JsonNode computeOnlyRulesID(Map.Entry<String, RuleCategoryAction> entry) {
        String categoryName = entry.getKey();
        RuleCategoryAction category = entry.getValue();

        Set<String> rulesToCheck = new HashSet<>();
        if (!category.getRules().isEmpty()) {
            rulesToCheck.addAll(
                category.getRules().stream()
                    .map(RuleAction::getRule)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet())
            );
            rulesToCheck.addAll(
                category.getRules().stream()
                    .map(RuleAction::getOldRule)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet())
            );
        }
        if (category.getPreventRulesId() != null) {
            rulesToCheck.addAll(category.getPreventRulesId());
        }

        for (String ruleID : rulesToCheck) {
            JsonNode ruleResponseInReferential;

            try {
                ruleResponseInReferential = adminManagementClientFactory.getClient().getRuleByID(ruleID);
            } catch (FileRulesException e) {
                ObjectNode errorInfo = JsonHandler.createObjectNode();
                errorInfo.put("Error", VitamCode.UPDATE_UNIT_RULES_CONSISTENCY.name());
                errorInfo.put("Message", VitamCode.UPDATE_UNIT_RULES_CONSISTENCY.getMessage());
                errorInfo.put("Info ", "Rule " + ruleID + " is not in database");
                errorInfo.put("Code", "UNITS_RULES_UNKNOWN");
                return errorInfo;
            } catch (AdminManagementClientServerException | InvalidParseOperationException e) {
                throw new IllegalStateException("Error while get the rule in Referential");
            }

            JsonNode ruleInReferential = ruleResponseInReferential.get("$results").get(0);
            if (!categoryName.equals(ruleInReferential.get("RuleType").asText())) {
                ObjectNode errorInfo = JsonHandler.createObjectNode();
                errorInfo.put("Error", VitamCode.UPDATE_UNIT_RULES_CONSISTENCY.name());
                errorInfo.put("Message", VitamCode.UPDATE_UNIT_RULES_CONSISTENCY.getMessage());
                errorInfo.put("Info ", "Rule " + ruleID + " is not in category " + categoryName + " but " +
                    ruleInReferential.get("RuleType").asText());
                errorInfo.put("Code", "UNITS_RULES_INCONSISTENCY");
                return errorInfo;
            }
        }

        return null;
    }

}
