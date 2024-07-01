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
import fr.gouv.vitam.common.model.administration.FileRulesModel;
import fr.gouv.vitam.common.model.administration.RuleType;
import fr.gouv.vitam.common.model.massupdate.RuleAction;
import fr.gouv.vitam.common.model.massupdate.RuleActions;
import fr.gouv.vitam.common.model.massupdate.RuleCategoryAction;
import fr.gouv.vitam.common.model.massupdate.RuleCategoryActionDeletion;
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
import java.time.format.DateTimeFormatter;
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

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(
        UnitMetadataRulesUpdateCheckConsistency.class
    );

    /**
     * UNIT_METADATA_CHECK_CONSISTENCY
     */
    private static final String UNIT_METADATA_CHECK_CONSISTENCY = "UNIT_METADATA_CHECK_CONSISTENCY";
    private static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN);

    private static final String MESSAGE = "Message";
    private static final String ERROR = "Error";
    private static final String INFO = "Info ";
    private static final String ARCHIVE_UNIT_PROFILE = "ArchiveUnitProfile ";
    private static final String HOLD_REASON = "HoldReason";
    private static final String HOLD_OWNER = "HoldOwner";
    private static final String HOLD_END_DATE = "HoldEndDate";
    private static final String HOLD_REASSESSING_DATE = "HoldReassessingDate";
    private static final String ARCHIVE_UNIT_PROFILE_NOT_FOUND =
        "Error while get the ArchiveUnitProfile in Referential";

    /**
     * AdminManagementClientFactory
     */
    private final AdminManagementClientFactory adminManagementClientFactory;

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
    public UnitMetadataRulesUpdateCheckConsistency(AdminManagementClientFactory adminManagementClientFactory) {
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
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException {
        checkMandatoryParameters(param);
        final ItemStatus itemStatus = new ItemStatus(UNIT_METADATA_CHECK_CONSISTENCY);

        // FIXME: Use in/out in order to transfer json from a step to another ?
        JsonNode queryActions = handler.getJsonFromWorkspace("actions.json");
        if (JsonHandler.isNullOrEmpty(queryActions)) {
            itemStatus.increment(StatusCode.KO);
            return new ItemStatus(UNIT_METADATA_CHECK_CONSISTENCY).setItemsStatus(
                UNIT_METADATA_CHECK_CONSISTENCY,
                itemStatus
            );
        }

        try {
            Optional<JsonNode> errorEvDetData = getErrorFromActionQuery(queryActions);
            if (errorEvDetData.isPresent()) {
                LOGGER.error(
                    "Rule update request validation failed \n" + JsonHandler.unprettyPrint(errorEvDetData.get())
                );
                itemStatus.increment(StatusCode.KO);
                itemStatus.setMessage(VitamCode.UPDATE_UNIT_RULES_CONSISTENCY.name());
                itemStatus.setEvDetailData(JsonHandler.unprettyPrint(errorEvDetData.get()));
                return new ItemStatus(UNIT_METADATA_CHECK_CONSISTENCY).setItemsStatus(
                    UNIT_METADATA_CHECK_CONSISTENCY,
                    itemStatus
                );
            }
        } catch (final VitamException | IllegalStateException e) {
            throw new ProcessingException(e);
        }

        LOGGER.info("Rule update request validation succeeded");
        itemStatus.increment(StatusCode.OK);
        return new ItemStatus(UNIT_METADATA_CHECK_CONSISTENCY).setItemsStatus(
            UNIT_METADATA_CHECK_CONSISTENCY,
            itemStatus
        );
    }

    private Optional<JsonNode> getErrorFromActionQuery(JsonNode queryActions) throws InvalidParseOperationException {
        RuleActions ruleActions = JsonHandler.getFromJsonNode(queryActions, RuleActions.class);

        Optional<JsonNode> potentialErrorInManagement = computeErrorsForManagementMetadata(ruleActions);
        if (potentialErrorInManagement.isPresent()) {
            return potentialErrorInManagement;
        }

        Optional<JsonNode> updateError = ruleActions
            .getUpdate()
            .stream()
            .flatMap(x -> x.entrySet().stream())
            .map(this::computeErrorsForUpdate)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst();
        if (updateError.isPresent()) return updateError;

        Optional<JsonNode> addError = ruleActions
            .getAdd()
            .stream()
            .flatMap(x -> x.entrySet().stream())
            .map(this::computeErrorsForAdd)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst();
        if (addError.isPresent()) return addError;

        Optional<JsonNode> deleteError = ruleActions
            .getDelete()
            .stream()
            .flatMap(x -> x.entrySet().stream())
            .map(this::computeErrorsForDelete)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst();
        return deleteError;
    }

    private Optional<JsonNode> computeErrorsForManagementMetadata(RuleActions ruleActions) {
        if (
            ruleActions.getAddOrUpdateMetadata() != null &&
            ruleActions.getAddOrUpdateMetadata().getArchiveUnitProfile() != null
        ) {
            Optional<JsonNode> error = checkAUPId(ruleActions.getAddOrUpdateMetadata().getArchiveUnitProfile());
            if (error.isPresent()) {
                return error;
            }
        }
        return Optional.empty();
    }

    private Optional<JsonNode> checkAUPId(String aupId) {
        try {
            RequestResponse<ArchiveUnitProfileModel> aup = adminManagementClientFactory
                .getClient()
                .findArchiveUnitProfilesByID(aupId);
            if (!aup.isOk()) {
                throw new IllegalStateException(ARCHIVE_UNIT_PROFILE_NOT_FOUND);
            }

            ArchiveUnitProfileModel archiveUnitProfile =
                ((RequestResponseOK<ArchiveUnitProfileModel>) aup).getFirstResult();
            if (archiveUnitProfile == null) {
                throw new IllegalStateException(ARCHIVE_UNIT_PROFILE_NOT_FOUND);
            }

            if (!ArchiveUnitProfileStatus.ACTIVE.equals(archiveUnitProfile.getStatus())) {
                ObjectNode errorInfo = JsonHandler.createObjectNode();
                errorInfo.put(ERROR, VitamCode.UPDATE_UNIT_MANAGEMENT_METADATA_CONSISTENCY.name());
                errorInfo.put(MESSAGE, VitamCode.UPDATE_UNIT_MANAGEMENT_METADATA_CONSISTENCY.getMessage());
                errorInfo.put(INFO, ARCHIVE_UNIT_PROFILE + aupId + " is not ACTIVE");
                errorInfo.put("Code", "CHECK_UNIT_PROFILE_INACTIVE");
                return Optional.of(errorInfo);
            }

            if (
                archiveUnitProfile.getControlSchema() == null ||
                JsonHandler.isEmpty(archiveUnitProfile.getControlSchema())
            ) {
                ObjectNode errorInfo = JsonHandler.createObjectNode();
                errorInfo.put(ERROR, VitamCode.UPDATE_UNIT_MANAGEMENT_METADATA_CONSISTENCY.name());
                errorInfo.put(MESSAGE, VitamCode.UPDATE_UNIT_MANAGEMENT_METADATA_CONSISTENCY.getMessage());
                errorInfo.put(INFO, ARCHIVE_UNIT_PROFILE + aupId + " havn't a valid Control Schema");
                errorInfo.put("Code", "CHECK_UNIT_PROFILE_CONSISTENCY");
                return Optional.of(errorInfo);
            }
        } catch (ReferentialNotFoundException e) {
            ObjectNode errorInfo = JsonHandler.createObjectNode();
            errorInfo.put(ERROR, VitamCode.UPDATE_UNIT_MANAGEMENT_METADATA_CONSISTENCY.name());
            errorInfo.put(MESSAGE, VitamCode.UPDATE_UNIT_MANAGEMENT_METADATA_CONSISTENCY.getMessage());
            errorInfo.put(INFO, ARCHIVE_UNIT_PROFILE + aupId + " is not in database");
            errorInfo.put("Code", "CHECK_UNIT_PROFILE_UNKNOWN");
            return Optional.of(errorInfo);
        } catch (InvalidParseOperationException | AdminManagementClientServerException e) {
            throw new IllegalStateException(ARCHIVE_UNIT_PROFILE_NOT_FOUND);
        }

        return Optional.empty();
    }

    private Optional<JsonNode> checkDateFormat(
        String fieldName,
        String date,
        String category,
        boolean checkUpperLimit
    ) {
        try {
            if (date == null) return Optional.empty();
            LocalDate parsedDate = LocalDate.parse(date, DATE_TIME_FORMATTER);
            if (checkUpperLimit && parsedDate.getYear() >= 9000) {
                ObjectNode errorInfo = JsonHandler.createObjectNode();
                errorInfo.put(ERROR, VitamCode.UPDATE_UNIT_RULES_QUERY_CONSISTENCY.name());
                errorInfo.put(MESSAGE, VitamCode.UPDATE_UNIT_RULES_QUERY_CONSISTENCY.getMessage());
                errorInfo.put(INFO, "'" + fieldName + "' must be prior than year 9000 for category " + category);
                errorInfo.put("Code", "UNIT_METADATA_UPDATE_CHECK_RULES_DATE_UNAUTHORIZED");
                return Optional.of(errorInfo);
            }
        } catch (DateTimeParseException e) {
            ObjectNode errorInfo = JsonHandler.createObjectNode();
            errorInfo.put(ERROR, VitamCode.UPDATE_UNIT_RULES_QUERY_CONSISTENCY.name());
            errorInfo.put(MESSAGE, VitamCode.UPDATE_UNIT_RULES_QUERY_CONSISTENCY.getMessage());
            errorInfo.put(INFO, "'" + fieldName + "' must follow 'AAAA-MM-DD' format " + category);
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
            // Common format checks
            Optional<JsonNode> checkFieldFormatError = checkRuleActionFormat(entry, ruleAction);
            if (checkFieldFormatError.isPresent()) {
                return checkFieldFormatError;
            }

            // Required fields in "update" mode
            Optional<JsonNode> oldRuleToUpdateError = checkOldRuleToUpdate(entry, ruleAction);
            if (oldRuleToUpdateError.isPresent()) return oldRuleToUpdateError;

            Optional<JsonNode> emptyRuleActionError = checkEmptyRuleAction(entry, ruleAction);
            if (emptyRuleActionError.isPresent()) {
                return emptyRuleActionError;
            }

            // Forbidden fields in "update" mode
            if (null != ruleAction.getEndDate()) {
                return reportUnexpectedField(entry, "EndDate");
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
            // Common format checks
            Optional<JsonNode> checkFieldFormatError = checkRuleActionFormat(entry, ruleAction);
            if (checkFieldFormatError.isPresent()) {
                return checkFieldFormatError;
            }

            // Required fields for "add" mode
            Optional<JsonNode> checkRuleToAddError = checkRuleToAdd(entry, ruleAction);
            if (checkRuleToAddError.isPresent()) {
                return checkRuleToAddError;
            }

            // Forbidden fields in "add" mode
            if (null != ruleAction.getEndDate()) {
                return reportUnexpectedField(entry, "EndDate");
            }

            if (null != ruleAction.getOldRule()) {
                return reportUnexpectedField(entry, "OldRule");
            }

            if (Boolean.TRUE.equals(ruleAction.isDeleteStartDate())) {
                return reportUnexpectedField(entry, "DeleteStartDate");
            }

            if (Boolean.TRUE.equals(ruleAction.getDeleteHoldEndDate())) {
                return reportUnexpectedField(entry, "DeleteHoldEndDate");
            }

            if (Boolean.TRUE.equals(ruleAction.getDeleteHoldOwner())) {
                return reportUnexpectedField(entry, "DeleteHoldOwner");
            }

            if (Boolean.TRUE.equals(ruleAction.getDeleteHoldReassessingDate())) {
                return reportUnexpectedField(entry, "DeleteHoldReassessingDate");
            }

            if (Boolean.TRUE.equals(ruleAction.getDeleteHoldReason())) {
                return reportUnexpectedField(entry, "DeleteHoldReason");
            }

            if (Boolean.TRUE.equals(ruleAction.getDeletePreventRearrangement())) {
                return reportUnexpectedField(entry, "DeletePreventRearrangement");
            }
        }

        return Optional.empty();
    }

    private Optional<JsonNode> checkRuleActionFormat(
        Map.Entry<String, RuleCategoryAction> entry,
        RuleAction ruleAction
    ) {
        // Check field formats
        Optional<JsonNode> checkFieldFormatError = checkFieldFormats(entry, ruleAction);
        if (checkFieldFormatError.isPresent()) {
            return checkFieldFormatError;
        }

        // Check incompatible fields
        Optional<JsonNode> setAndDeleteOfSameFieldsError = checkSetAndDeleteOfSameFields(entry, ruleAction);
        if (setAndDeleteOfSameFieldsError.isPresent()) {
            return setAndDeleteOfSameFieldsError;
        }

        // Check fields HoldRule-only fields
        Optional<JsonNode> holdRuleFieldsErrors = checkReservedHoldRuleAttributes(entry, ruleAction);
        if (holdRuleFieldsErrors.isPresent()) {
            return holdRuleFieldsErrors;
        }

        return Optional.empty();
    }

    private Optional<JsonNode> checkFieldFormats(Map.Entry<String, RuleCategoryAction> entry, RuleAction ruleAction) {
        Optional<JsonNode> checkStartDateResponse = checkDateFormat(
            "StartDate",
            ruleAction.getStartDate(),
            entry.getKey(),
            true
        );
        if (checkStartDateResponse.isPresent()) {
            return checkStartDateResponse;
        }

        Optional<JsonNode> checkHoldEndDateResponse = checkDateFormat(
            HOLD_END_DATE,
            ruleAction.getHoldEndDate(),
            entry.getKey(),
            false
        );
        if (checkHoldEndDateResponse.isPresent()) {
            return checkHoldEndDateResponse;
        }

        Optional<JsonNode> checkHoldReassessingDateResponse = checkDateFormat(
            HOLD_REASSESSING_DATE,
            ruleAction.getHoldReassessingDate(),
            entry.getKey(),
            false
        );
        if (checkHoldReassessingDateResponse.isPresent()) {
            return checkHoldReassessingDateResponse;
        }

        if (null != ruleAction.getHoldOwner() && ruleAction.getHoldOwner().isBlank()) {
            return reportEmptyFieldFormat(entry, HOLD_OWNER);
        }

        if (null != ruleAction.getHoldReason() && ruleAction.getHoldReason().isBlank()) {
            return reportEmptyFieldFormat(entry, HOLD_REASON);
        }
        return Optional.empty();
    }

    private Optional<JsonNode> checkSetAndDeleteOfSameFields(
        Map.Entry<String, RuleCategoryAction> entry,
        RuleAction ruleAction
    ) {
        if (ruleAction.getStartDate() != null && Boolean.TRUE.equals(ruleAction.getDeleteStartDate())) {
            return reportSetAndDeleteOfSameField(entry, "StartDate");
        }

        if (ruleAction.getHoldEndDate() != null && Boolean.TRUE.equals(ruleAction.getDeleteHoldEndDate())) {
            return reportSetAndDeleteOfSameField(entry, HOLD_END_DATE);
        }

        if (ruleAction.getHoldReason() != null && Boolean.TRUE.equals(ruleAction.getDeleteHoldReason())) {
            return reportSetAndDeleteOfSameField(entry, HOLD_REASON);
        }

        if (
            ruleAction.getHoldReassessingDate() != null &&
            Boolean.TRUE.equals(ruleAction.getDeleteHoldReassessingDate())
        ) {
            return reportSetAndDeleteOfSameField(entry, HOLD_REASSESSING_DATE);
        }

        if (ruleAction.getHoldOwner() != null && Boolean.TRUE.equals(ruleAction.getDeleteHoldOwner())) {
            return reportSetAndDeleteOfSameField(entry, HOLD_OWNER);
        }

        if (
            ruleAction.getPreventRearrangement() != null &&
            Boolean.TRUE.equals(ruleAction.getDeletePreventRearrangement())
        ) {
            return reportSetAndDeleteOfSameField(entry, "PreventRearrangement");
        }
        return Optional.empty();
    }

    private Optional<JsonNode> checkReservedHoldRuleAttributes(
        Map.Entry<String, RuleCategoryAction> entry,
        RuleAction ruleAction
    ) {
        if (RuleType.HoldRule.isNameEquals(entry.getKey())) {
            return Optional.empty();
        }

        if (null != ruleAction.getHoldEndDate()) {
            return reportUnexpectedField(entry, HOLD_END_DATE);
        }
        if (null != ruleAction.getDeleteHoldEndDate()) {
            return reportUnexpectedField(entry, "DeleteHoldEndDate");
        }
        if (null != ruleAction.getHoldOwner()) {
            return reportUnexpectedField(entry, HOLD_OWNER);
        }
        if (null != ruleAction.getDeleteHoldOwner()) {
            return reportUnexpectedField(entry, "DeleteHoldOwner");
        }
        if (null != ruleAction.getHoldReassessingDate()) {
            return reportUnexpectedField(entry, HOLD_REASSESSING_DATE);
        }
        if (null != ruleAction.getDeleteHoldReassessingDate()) {
            return reportUnexpectedField(entry, "DeleteHoldReassessingDate");
        }
        if (null != ruleAction.getHoldReason()) {
            return reportUnexpectedField(entry, HOLD_REASON);
        }
        if (null != ruleAction.getDeleteHoldReason()) {
            return reportUnexpectedField(entry, "DeleteHoldReason");
        }
        if (null != ruleAction.getPreventRearrangement()) {
            return reportUnexpectedField(entry, "PreventRearrangement");
        }
        if (null != ruleAction.getDeletePreventRearrangement()) {
            return reportUnexpectedField(entry, "DeletePreventRearrangement");
        }
        return Optional.empty();
    }

    private Optional<JsonNode> checkOldRuleToUpdate(
        Map.Entry<String, RuleCategoryAction> entry,
        RuleAction ruleAction
    ) {
        if (StringUtils.isEmpty(ruleAction.getOldRule())) {
            ObjectNode errorInfo = JsonHandler.createObjectNode();
            errorInfo.put(ERROR, VitamCode.UPDATE_UNIT_RULES_QUERY_CONSISTENCY.name());
            errorInfo.put(MESSAGE, VitamCode.UPDATE_UNIT_RULES_QUERY_CONSISTENCY.getMessage());
            errorInfo.put(INFO, "Rule update should define an 'OldRule' to be updated for category " + entry.getKey());
            errorInfo.put("Code", "UNIT_RULES_MISSING_MANDATORY_FIELD");
            return Optional.of(errorInfo);
        }
        return Optional.empty();
    }

    private Optional<JsonNode> checkRuleToAdd(Map.Entry<String, RuleCategoryAction> entry, RuleAction ruleAction) {
        if (StringUtils.isEmpty(ruleAction.getRule())) {
            ObjectNode errorInfo = JsonHandler.createObjectNode();
            errorInfo.put(ERROR, VitamCode.UPDATE_UNIT_RULES_QUERY_CONSISTENCY.name());
            errorInfo.put(MESSAGE, VitamCode.UPDATE_UNIT_RULES_QUERY_CONSISTENCY.getMessage());
            errorInfo.put(INFO, "New rule must at least define the field 'Rule' for category " + entry.getKey());
            errorInfo.put("Code", "UNIT_RULES_MISSING_MANDATORY_FIELD");
            return Optional.of(errorInfo);
        }
        return Optional.empty();
    }

    private Optional<JsonNode> checkEmptyRuleAction(
        Map.Entry<String, RuleCategoryAction> entry,
        RuleAction ruleAction
    ) {
        if (isRuleActionEmpty(ruleAction)) {
            ObjectNode errorInfo = JsonHandler.createObjectNode();
            errorInfo.put(ERROR, VitamCode.UPDATE_UNIT_RULES_QUERY_CONSISTENCY.name());
            errorInfo.put(MESSAGE, VitamCode.UPDATE_UNIT_RULES_QUERY_CONSISTENCY.getMessage());
            errorInfo.put(INFO, "Rule update should define at least 1 updated field for category " + entry.getKey());
            errorInfo.put("Code", "UNIT_RULES_NOT_EXPECTED_FIELD");
            return Optional.of(errorInfo);
        }
        return Optional.empty();
    }

    private boolean isRuleActionEmpty(RuleAction ruleAction) {
        return (
            ruleAction.getRule() == null &&
            ruleAction.getStartDate() == null &&
            ruleAction.getHoldEndDate() == null &&
            ruleAction.getHoldOwner() == null &&
            ruleAction.getHoldReason() == null &&
            ruleAction.getHoldReassessingDate() == null &&
            ruleAction.getPreventRearrangement() == null &&
            !Boolean.TRUE.equals(ruleAction.getDeleteStartDate()) &&
            !Boolean.TRUE.equals(ruleAction.getDeleteHoldEndDate()) &&
            !Boolean.TRUE.equals(ruleAction.getDeleteHoldOwner()) &&
            !Boolean.TRUE.equals(ruleAction.getDeleteHoldReason()) &&
            !Boolean.TRUE.equals(ruleAction.getDeleteHoldReassessingDate()) &&
            !Boolean.TRUE.equals(ruleAction.getDeletePreventRearrangement())
        );
    }

    private Optional<JsonNode> reportUnexpectedField(Map.Entry<String, RuleCategoryAction> entry, String fieldName) {
        ObjectNode errorInfo = JsonHandler.createObjectNode();
        errorInfo.put(ERROR, VitamCode.UPDATE_UNIT_RULES_QUERY_CONSISTENCY.name());
        errorInfo.put(MESSAGE, VitamCode.UPDATE_UNIT_RULES_QUERY_CONSISTENCY.getMessage());
        errorInfo.put(INFO, "New rule must not define '" + fieldName + "' field for category " + entry.getKey());
        errorInfo.put("Code", "UNIT_RULES_UNEXPECTED_FIELD");
        return Optional.of(errorInfo);
    }

    private Optional<JsonNode> reportEmptyFieldFormat(Map.Entry<String, RuleCategoryAction> entry, String fieldName) {
        ObjectNode errorInfo = JsonHandler.createObjectNode();
        errorInfo.put(ERROR, VitamCode.UPDATE_UNIT_RULES_QUERY_CONSISTENCY.name());
        errorInfo.put(MESSAGE, VitamCode.UPDATE_UNIT_RULES_QUERY_CONSISTENCY.getMessage());
        errorInfo.put(INFO, "Empty string for '" + fieldName + "' field for category " + entry.getKey());
        errorInfo.put("Code", "UNIT_RULES_EMPTY_FIELD");
        return Optional.of(errorInfo);
    }

    private Optional<JsonNode> reportSetAndDeleteOfSameField(
        Map.Entry<String, RuleCategoryAction> entry,
        String fieldName
    ) {
        ObjectNode errorInfo = JsonHandler.createObjectNode();
        errorInfo.put(ERROR, VitamCode.UPDATE_UNIT_RULES_QUERY_CONSISTENCY.name());
        errorInfo.put(MESSAGE, VitamCode.UPDATE_UNIT_RULES_QUERY_CONSISTENCY.getMessage());
        errorInfo.put(
            INFO,
            "Cannot set '" + fieldName + "' + field value when 'Delete" + "' is set for category " + entry.getKey()
        );
        errorInfo.put("Code", "UNITS_RULES_INCONSISTENCY");
        return Optional.of(errorInfo);
    }

    private Optional<JsonNode> computeErrorsForCategory(Map.Entry<String, RuleCategoryAction> entry) {
        Optional<JsonNode> rulesIDErrors = computeOnlyRulesID(entry);
        if (rulesIDErrors.isPresent()) {
            return rulesIDErrors;
        }

        String categoryName = entry.getKey();
        RuleCategoryAction category = entry.getValue();

        if (SedaConstants.TAG_RULE_CLASSIFICATION.equals(categoryName)) {
            String classificationLevel = category.getClassificationLevel();
            if (classificationLevel != null && !ClassificationLevelUtil.checkClassificationLevel(classificationLevel)) {
                ObjectNode errorInfo = JsonHandler.createObjectNode();
                errorInfo.put(ERROR, VitamCode.UPDATE_UNIT_RULES_PROPERTY_CONSISTENCY.name());
                errorInfo.put(MESSAGE, VitamCode.UPDATE_UNIT_RULES_PROPERTY_CONSISTENCY.getMessage());
                errorInfo.put(INFO, classificationLevel + " is not a valid value for ClassificationLevel");
                errorInfo.put("Code", "CHECK_CLASSIFICATION_LEVEL");
                return Optional.of(errorInfo);
            }
        }

        return Optional.empty();
    }

    private Optional<JsonNode> computeOnlyRulesID(Map.Entry<String, RuleCategoryAction> entry) {
        String categoryName = entry.getKey();
        RuleCategoryAction category = entry.getValue();

        Set<String> rulesToCheck = new HashSet<>();
        if (!category.getRules().isEmpty()) {
            rulesToCheck.addAll(
                category
                    .getRules()
                    .stream()
                    .map(RuleAction::getRule)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet())
            );
            rulesToCheck.addAll(
                category
                    .getRules()
                    .stream()
                    .map(RuleAction::getOldRule)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet())
            );
        }
        if (category.getPreventRulesId() != null) {
            rulesToCheck.addAll(category.getPreventRulesId());
        }
        if (category.getPreventRulesIdToAdd() != null) {
            rulesToCheck.addAll(category.getPreventRulesIdToAdd());
        }

        return validateRuleIds(categoryName, rulesToCheck);
    }

    private Optional<JsonNode> validateRuleIds(String categoryName, Set<String> rulesToCheck) {
        for (String ruleID : rulesToCheck) {
            JsonNode ruleResponseInReferential;

            try {
                ruleResponseInReferential = adminManagementClientFactory.getClient().getRuleByID(ruleID);
            } catch (FileRulesException e) {
                ObjectNode errorInfo = JsonHandler.createObjectNode();
                errorInfo.put(ERROR, VitamCode.UPDATE_UNIT_RULES_CONSISTENCY.name());
                errorInfo.put(MESSAGE, VitamCode.UPDATE_UNIT_RULES_CONSISTENCY.getMessage());
                errorInfo.put(INFO, "Rule " + ruleID + " is not in database");
                errorInfo.put("Code", "UNITS_RULES_UNKNOWN");
                return Optional.of(errorInfo);
            } catch (AdminManagementClientServerException | InvalidParseOperationException e) {
                throw new IllegalStateException("Error while get the rule in Referential");
            }

            JsonNode ruleInReferential = ruleResponseInReferential.get("$results").get(0);
            if (!categoryName.equals(ruleInReferential.get(FileRulesModel.RULE_TYPE).asText())) {
                ObjectNode errorInfo = JsonHandler.createObjectNode();
                errorInfo.put(ERROR, VitamCode.UPDATE_UNIT_RULES_CONSISTENCY.name());
                errorInfo.put(MESSAGE, VitamCode.UPDATE_UNIT_RULES_CONSISTENCY.getMessage());
                errorInfo.put(
                    INFO,
                    "Rule " +
                    ruleID +
                    " is not in category " +
                    categoryName +
                    " but " +
                    ruleInReferential.get("RuleType").asText()
                );
                errorInfo.put("Code", "UNITS_RULES_INCONSISTENCY");
                return Optional.of(errorInfo);
            }
        }

        return Optional.empty();
    }

    private Optional<JsonNode> computeErrorsForDelete(
        Map.Entry<String, RuleCategoryActionDeletion> ruleCategoryActionDeletionEntry
    ) {
        String categoryName = ruleCategoryActionDeletionEntry.getKey();
        RuleCategoryActionDeletion ruleCategoryActionDeletion = ruleCategoryActionDeletionEntry.getValue();

        if (ruleCategoryActionDeletion == null) {
            return Optional.empty();
        }

        if (ruleCategoryActionDeletion.getPreventRulesIdToRemove() != null) {
            return validateRuleIds(categoryName, ruleCategoryActionDeletion.getPreventRulesIdToRemove());
        }
        return Optional.empty();
    }
}
