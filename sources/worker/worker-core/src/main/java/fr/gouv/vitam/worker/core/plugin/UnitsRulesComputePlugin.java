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
package fr.gouv.vitam.worker.core.plugin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.database.builder.query.BooleanQuery;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.IngestWorkflowConstants;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.RuleMeasurementEnum;
import fr.gouv.vitam.common.model.administration.RuleType;
import fr.gouv.vitam.common.model.unit.ManagementModel;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.FileRules;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesException;
import fr.gouv.vitam.functional.administration.common.utils.ArchiveUnitUpdateUtils;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.exception.InvalidRuleException;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.or;

/**
 * UnitsRulesCompute Plugin.<br>
 */

public class UnitsRulesComputePlugin extends ActionHandler {

    private static final String WORKSPACE_SERVER_ERROR = "Workspace Server Error";

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(UnitsRulesComputePlugin.class);

    private static final String CHECK_RULES_TASK_ID = "UNITS_RULES_COMPUTE";
    private static final String FILE_COULD_NOT_BE_DELETED_MSG = "File could not be deleted";
    private static final String AU_PREFIX_WITH_END_DATE = "WithEndDte_";
    private static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd";
    private static final String CHECKS_RULES = "Rules checks problem: missing parameters";
    private static final String NON_EXISTING_RULE = "Rule %s does not exist";
    private static final String BAD_CATEGORY_RULE = "Rule %s don't match expected category %s but was %s";

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN);

    private static final int UNIT_INPUT_RANK = 0;
    private final AdminManagementClientFactory adminManagementClientFactory;

    /**
     * Empty constructor UnitsRulesComputePlugin
     */
    public UnitsRulesComputePlugin() {
        this(AdminManagementClientFactory.getInstance());
    }

    @VisibleForTesting
    public UnitsRulesComputePlugin(AdminManagementClientFactory adminManagementClientFactory) {
        this.adminManagementClientFactory = adminManagementClientFactory;
    }

    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO handler) {
        LOGGER.debug("UNITS_RULES_COMPUTE in execute");
        final long time = System.currentTimeMillis();
        final ItemStatus itemStatus = new ItemStatus(CHECK_RULES_TASK_ID);

        try {
            calculateMaturityDate(params, itemStatus, handler);
            itemStatus.increment(StatusCode.OK);
        } catch (InvalidRuleException e) {
            itemStatus.increment(StatusCode.KO);
            UnitRulesComputeStatus status = e.getUnitRulesComputeStatus();
            switch (status) {
                case REF_INCONSISTENCY:
                    itemStatus.setGlobalOutcomeDetailSubcode(status.name());
                    itemStatus.setEvDetailData(e.getMessage());
                    itemStatus.increment(StatusCode.KO);
                    break;
                case UNKNOWN:
                case CONSISTENCY:
                    itemStatus.setGlobalOutcomeDetailSubcode(status.name());
                    itemStatus.increment(StatusCode.KO);

                    ItemStatus is = new ItemStatus(status.name());
                    is.setEvDetailData(e.getMessage());
                    is.increment(StatusCode.KO);

                    itemStatus.setSubTaskStatus(e.getObjectId(), is);
                    itemStatus.setItemId(status.name());

                    return new ItemStatus(itemStatus.getItemId()).setItemsStatus(itemStatus.getItemId(), itemStatus);
            }
        } catch (final ProcessingException e) {
            LOGGER.debug(e);
            final ObjectNode object = JsonHandler.createObjectNode();
            object.put("UnitRuleCompute", e.getMessage());
            itemStatus.setEvDetailData(object.toString());
            itemStatus.increment(StatusCode.KO);
        }

        LOGGER.debug("[exit] execute... /Elapsed Time:" + (System.currentTimeMillis() - time) / 1000 + "s");
        return new ItemStatus(CHECK_RULES_TASK_ID).setItemsStatus(CHECK_RULES_TASK_ID, itemStatus);
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // Nothing to check
    }

    private void calculateMaturityDate(WorkerParameters params, ItemStatus itemStatus, HandlerIO handlerIO)
        throws ProcessingException {
        ParametersChecker.checkNullOrEmptyParameters(params);
        final String containerId = params.getContainerName();
        final String objectName = params.getObjectName();

        try {
            JsonNode archiveUnit = null;
            if (handlerIO.getInput().size() > 0) {
                archiveUnit = (JsonNode) handlerIO.getInput(UNIT_INPUT_RANK);
            } else {
                try (
                    InputStream inputStream = handlerIO.getInputStreamFromWorkspace(
                        IngestWorkflowConstants.ARCHIVE_UNIT_FOLDER + "/" + objectName
                    )
                ) {
                    archiveUnit = JsonHandler.getFromInputStream(inputStream);
                }
            }

            parseRulesAndUpdateEndDate(archiveUnit, objectName, containerId, handlerIO);
        } catch (
            IOException
            | ContentAddressableStorageNotFoundException
            | ContentAddressableStorageServerException
            | InvalidParseOperationException e
        ) {
            LOGGER.error(WORKSPACE_SERVER_ERROR, e);
            throw new ProcessingException(e);
        }
    }

    /**
     * findRulesValueQueryBuilders: select query
     *
     * @param rulesId
     * @return the JsonNode answer
     * @throws InvalidCreateOperationException
     * @throws InvalidParseOperationException
     * @throws AdminManagementClientServerException
     * @throws ProcessingException
     */

    private JsonNode findRulesValueQueryBuilders(Set<String> rulesId)
        throws InvalidCreateOperationException, InvalidParseOperationException, IOException, ProcessingException {
        final Select select = new Select();
        select.addOrderByDescFilter(FileRules.RULEID);
        final BooleanQuery query = or();
        for (final String ruleId : rulesId) {
            query.add(eq(FileRules.RULEID, ruleId));
        }
        select.setQuery(query);

        try (AdminManagementClient adminManagementClient = adminManagementClientFactory.getClient()) {
            return adminManagementClient.getRules(select.getFinalSelect());
        } catch (final VitamException e) {
            throw new ProcessingException(e);
        }
    }

    /**
     * Check archiveUnit json file and add end date for rules.
     *
     * @param archiveUnit archiveUnit json file
     * @param objectName json file name
     * @param containerName
     * @throws IOException
     * @throws ProcessingException
     */
    private void parseRulesAndUpdateEndDate(
        JsonNode archiveUnit,
        String objectName,
        String containerName,
        HandlerIO handlerIO
    ) throws IOException, ProcessingException {
        final File fileWithEndDate = handlerIO.getNewLocalFile(AU_PREFIX_WITH_END_DATE + objectName);
        try {
            // Archive unit nodes
            JsonNode archiveUnitNode = archiveUnit.get(SedaConstants.TAG_ARCHIVE_UNIT);
            JsonNode workNode = archiveUnit.get(SedaConstants.PREFIX_WORK);
            JsonNode managementNode = archiveUnitNode.get(SedaConstants.PREFIX_MGT);

            JsonNode unitTileNode = archiveUnitNode.get("Title");
            JsonNode unitIdNode = archiveUnitNode.get(SedaConstants.PREFIX_ID);
            String unitTile = "";
            if (null != unitTileNode) {
                unitTile = archiveUnitNode.asText();
            }
            String unitId = "";
            if (null != unitIdNode) {
                unitId = unitIdNode.asText();
            }

            validatePreventRuleCategory(unitTile, managementNode);
            // temp data
            JsonNode rulesResults;

            // rules to apply
            Set<String> rulesToApply = new HashSet<>();
            if (workNode.get(SedaConstants.TAG_RULE_APPLING_TO_ROOT_ARCHIVE_UNIT) != null) {
                if (workNode.get(SedaConstants.TAG_RULE_APPLING_TO_ROOT_ARCHIVE_UNIT).isArray()) {
                    // FIXME replace with always real arrayNode
                    ArrayNode rulesToApplyArray = (ArrayNode) workNode.get(
                        SedaConstants.TAG_RULE_APPLING_TO_ROOT_ARCHIVE_UNIT
                    );
                    if (rulesToApplyArray.size() > 0) {
                        rulesToApply = Sets.newHashSet(
                            Splitter.on(SedaConstants.RULE_SEPARATOR)
                                .omitEmptyStrings()
                                .split(rulesToApplyArray.iterator().next().asText())
                        );
                    }
                } else {
                    rulesToApply = Sets.newHashSet(
                        Splitter.on(SedaConstants.RULE_SEPARATOR).split(
                            workNode.get(SedaConstants.TAG_RULE_APPLING_TO_ROOT_ARCHIVE_UNIT).asText()
                        )
                    );
                }
            }
            if (rulesToApply.isEmpty()) {
                LOGGER.debug("Archive unit does not have rules");
                return;
            }

            // search ref rules
            rulesResults = findRulesValueQueryBuilders(rulesToApply);
            LOGGER.debug(
                "rulesResults for archive unit id: " +
                objectName +
                " && containerName is :" +
                containerName +
                " is:" +
                rulesResults
            );

            // update all rules
            for (String ruleType : SedaConstants.getSupportedRules()) {
                JsonNode ruleTypeNode = managementNode.get(ruleType);
                if (
                    ruleTypeNode == null ||
                    ruleTypeNode.get(SedaConstants.TAG_RULES) == null ||
                    ruleTypeNode.get(SedaConstants.TAG_RULES).size() == 0 ||
                    ruleTypeNode.get(SedaConstants.TAG_RULES).findValues(SedaConstants.TAG_RULE_RULE).size() == 0
                ) {
                    LOGGER.debug("no rules of type " + ruleType + " found");
                    continue;
                }
                if (ruleTypeNode.get(SedaConstants.TAG_RULES).isArray()) {
                    ArrayNode ruleNodes = (ArrayNode) ruleTypeNode.get(SedaConstants.TAG_RULES);
                    for (JsonNode ruleNode : ruleNodes) {
                        computeRuleNode((ObjectNode) ruleNode, rulesResults, ruleType, unitId);
                    }
                } else {
                    LOGGER.debug(
                        "ruleTypeNode of type " + ruleType + "." + SedaConstants.TAG_RULES + " should be an array"
                    );
                    throw new ProcessingException("ruleTypeNode.Rules should be an array");
                }
            }
            JsonHandler.writeAsFile(archiveUnit, fileWithEndDate);
        } catch (InvalidParseOperationException | InvalidCreateOperationException e) {
            LOGGER.error(e);
            throw new ProcessingException(e);
        }

        // Write to workspace
        try {
            handlerIO.transferFileToWorkspace(
                IngestWorkflowConstants.ARCHIVE_UNIT_FOLDER + File.separator + objectName,
                fileWithEndDate,
                true,
                false
            );
        } catch (final ProcessingException e) {
            LOGGER.error("Can not write to workspace ", e);
            if (!fileWithEndDate.delete()) {
                LOGGER.warn(FILE_COULD_NOT_BE_DELETED_MSG);
            }
            throw e;
        }
    }

    private void validatePreventRuleCategory(String unit, JsonNode managementNode)
        throws InvalidParseOperationException, InvalidFormatException, ProcessingException {
        if (null == managementNode || !managementNode.elements().hasNext()) {
            return;
        }
        final StringBuffer report = new StringBuffer();
        ManagementModel managementModel = JsonHandler.getFromJsonNode(managementNode, ManagementModel.class);
        try (AdminManagementClient adminManagementClient = adminManagementClientFactory.getClient()) {
            if (null != managementModel.getAccess()) {
                if (
                    null != managementModel.getAccess().getInheritance() &&
                    null != managementModel.getAccess().getInheritance().getPreventRulesId()
                ) {
                    validatePreventRuleCategory(
                        unit,
                        SedaConstants.TAG_RULE_ACCESS,
                        report,
                        managementModel.getAccess().getInheritance().getPreventRulesId(),
                        adminManagementClient
                    );
                }
            }

            if (null != managementModel.getAppraisal()) {
                if (
                    null != managementModel.getAppraisal().getInheritance() &&
                    null != managementModel.getAppraisal().getInheritance().getPreventRulesId()
                ) {
                    validatePreventRuleCategory(
                        unit,
                        SedaConstants.TAG_RULE_APPRAISAL,
                        report,
                        managementModel.getAppraisal().getInheritance().getPreventRulesId(),
                        adminManagementClient
                    );
                }
            }

            if (null != managementModel.getDissemination()) {
                if (
                    null != managementModel.getDissemination().getInheritance() &&
                    null != managementModel.getDissemination().getInheritance().getPreventRulesId()
                ) {
                    validatePreventRuleCategory(
                        unit,
                        SedaConstants.TAG_RULE_DISSEMINATION,
                        report,
                        managementModel.getDissemination().getInheritance().getPreventRulesId(),
                        adminManagementClient
                    );
                }
            }

            if (null != managementModel.getStorage()) {
                if (
                    null != managementModel.getStorage().getInheritance() &&
                    null != managementModel.getStorage().getInheritance().getPreventRulesId()
                ) {
                    validatePreventRuleCategory(
                        unit,
                        SedaConstants.TAG_RULE_STORAGE,
                        report,
                        managementModel.getStorage().getInheritance().getPreventRulesId(),
                        adminManagementClient
                    );
                }
            }

            if (null != managementModel.getReuse()) {
                if (
                    null != managementModel.getReuse().getInheritance() &&
                    null != managementModel.getReuse().getInheritance().getPreventRulesId()
                ) {
                    validatePreventRuleCategory(
                        unit,
                        SedaConstants.TAG_RULE_REUSE,
                        report,
                        managementModel.getReuse().getInheritance().getPreventRulesId(),
                        adminManagementClient
                    );
                }
            }

            if (null != managementModel.getClassification()) {
                if (
                    null != managementModel.getClassification().getInheritance() &&
                    null != managementModel.getClassification().getInheritance().getPreventRulesId()
                ) {
                    validatePreventRuleCategory(
                        unit,
                        SedaConstants.TAG_RULE_CLASSIFICATION,
                        report,
                        managementModel.getClassification().getInheritance().getPreventRulesId(),
                        adminManagementClient
                    );
                }
            }

            if (null != managementModel.getHold()) {
                if (
                    null != managementModel.getHold().getInheritance() &&
                    null != managementModel.getHold().getInheritance().getPreventRulesId()
                ) {
                    validatePreventRuleCategory(
                        unit,
                        SedaConstants.TAG_RULE_HOLD,
                        report,
                        managementModel.getHold().getInheritance().getPreventRulesId(),
                        adminManagementClient
                    );
                }
            }
        } catch (final VitamException e) {
            throw new ProcessingException(e);
        }

        final String errors = report.toString();
        if (ParametersChecker.isNotEmpty(errors)) {
            ObjectNode json = JsonHandler.createObjectNode();
            json.put("evDetTechData", errors);
            throw new InvalidRuleException(UnitRulesComputeStatus.REF_INCONSISTENCY, JsonHandler.unprettyPrint(json));
        }
    }

    private void validatePreventRuleCategory(
        String unit,
        String ruleType,
        StringBuffer report,
        Collection<String> ruleIds,
        AdminManagementClient adminManagementClient
    )
        throws FileRulesException, InvalidParseOperationException, InvalidFormatException, AdminManagementClientServerException {
        if (null == ruleIds) {
            return;
        }

        for (String ruleId : ruleIds) {
            JsonNode rulesInDB = adminManagementClient.getRuleByID(ruleId);
            if (null == rulesInDB) {
                report
                    .append("In the unit ")
                    .append(unit)
                    .append(" the rule id ")
                    .append(ruleId)
                    .append(" in the RuleType ")
                    .append(ruleType)
                    .append(" is not found in db ; ");
                continue;
            }

            RequestResponse<FileRules> fr = JsonHandler.getFromStringAsTypeReference(
                rulesInDB.toString(),
                new TypeReference<RequestResponseOK<FileRules>>() {}
            );
            if (fr.isOk()) {
                RequestResponseOK<FileRules> frok = (RequestResponseOK<FileRules>) fr;
                Iterator<FileRules> it = frok.getResults().iterator();

                if (it.hasNext()) {
                    RuleType rr = it.next().getRuletype();
                    if (!ruleType.equals(rr.name())) {
                        report
                            .append("In the unit ")
                            .append(unit)
                            .append(" the rule id ")
                            .append(ruleId)
                            .append(" is in the wrong RuleType ")
                            .append(" it should be in the RuleType ")
                            .append(ruleType)
                            .append(" ; ");
                    }
                } else {
                    report
                        .append("In the unit ")
                        .append(unit)
                        .append(" the rule id ")
                        .append(ruleId)
                        .append(" not found in db")
                        .append(" ; ");
                }
            } else {
                VitamError vr = (VitamError) fr;
                report
                    .append("In the unit ")
                    .append(unit)
                    .append(" error while getting rule id : ")
                    .append(ruleId)
                    .append(" : ")
                    .append(vr.getMessage())
                    .append(" : ")
                    .append(vr.getDescription())
                    .append(" ; ");
            }
        }
    }

    /**
     * Compute enddate for rule node
     *
     * @param ruleNode current ruleNode from archive unit
     * @param rulesResults rules referential
     * @param ruleType current rule type
     * @throws ProcessingException
     */
    private void computeRuleNode(ObjectNode ruleNode, JsonNode rulesResults, String ruleType, String unitId)
        throws ProcessingException {
        String ruleId = ruleNode.get(SedaConstants.TAG_RULE_RULE).asText();
        String startDate = ruleNode.hasNonNull(SedaConstants.TAG_RULE_START_DATE)
            ? ruleNode.get(SedaConstants.TAG_RULE_START_DATE).asText()
            : null;
        String holdEndDate = ruleNode.hasNonNull(SedaConstants.TAG_RULE_HOLD_END_DATE)
            ? ruleNode.get(SedaConstants.TAG_RULE_HOLD_END_DATE).asText()
            : null;

        JsonNode referencedRule = checkRuleNodeByID(ruleId, ruleType, rulesResults, unitId);

        if (hasUndefinedHoldRuleDuration(ruleType, referencedRule)) {
            ensureStartDateIsBeforeHoldEndDate(startDate, holdEndDate, unitId, ruleId);

            computeEndDateForUndefinedHoldRuleDuration(ruleNode, ruleType, unitId, ruleId, holdEndDate);
            return;
        }

        ensureHoldEndRuleNotDefined(unitId, ruleId, holdEndDate);

        LocalDate endDate = computeEndDateForRuleWithDuration(startDate, ruleId, ruleType, referencedRule);
        if (endDate != null) {
            ruleNode.put(SedaConstants.TAG_RULE_END_DATE, endDate.format(DATE_TIME_FORMATTER));
        }
    }

    private void ensureHoldEndRuleNotDefined(String unitId, String ruleId, String holdEndDate)
        throws InvalidRuleException {
        if (holdEndDate != null) {
            String errorMessage = String.format(
                "Cannot set %s for rule %s with defined duration.",
                SedaConstants.TAG_RULE_HOLD_END_DATE,
                ruleId
            );
            ObjectNode json = JsonHandler.createObjectNode();
            json.put("evDetTechData", errorMessage);
            throw new InvalidRuleException(UnitRulesComputeStatus.CONSISTENCY, JsonHandler.unprettyPrint(json), unitId);
        }
    }

    private boolean hasUndefinedHoldRuleDuration(String ruleType, JsonNode referencedRule) {
        return SedaConstants.TAG_RULE_HOLD.equals(ruleType) && !referencedRule.hasNonNull(FileRules.RULEDURATION);
    }

    private void computeEndDateForUndefinedHoldRuleDuration(
        ObjectNode ruleNode,
        String ruleType,
        String unitId,
        String ruleId,
        String holdEndDate
    ) {
        if (holdEndDate == null) {
            LOGGER.debug(
                String.format(
                    "EndDate cannot be computed for %s rule %s for unit %s since " +
                    "no HoldEndDate provided & rule has no defined duration",
                    ruleType,
                    ruleId,
                    unitId
                )
            );
            return;
        }

        LOGGER.debug(
            String.format(
                "EndDate will be set to HoldEndDate for %s rule %s for unit %s since " +
                "hold rule has no defined duration",
                ruleType,
                ruleId,
                unitId
            )
        );
        ruleNode.put(SedaConstants.TAG_RULE_END_DATE, holdEndDate);
    }

    private void ensureStartDateIsBeforeHoldEndDate(String startDate, String holdEndDate, String unitId, String ruleId)
        throws InvalidRuleException {
        if (startDate != null && holdEndDate != null) {
            LocalDate localStartDate = LocalDate.parse(startDate, DATE_TIME_FORMATTER);
            LocalDate localHoldEndDate = LocalDate.parse(holdEndDate, DATE_TIME_FORMATTER);

            if (localHoldEndDate.isBefore(localStartDate)) {
                String errorMessage =
                    "Illegal " +
                    SedaConstants.TAG_RULE_HOLD_END_DATE +
                    "(" +
                    holdEndDate +
                    ") for " +
                    ruleId +
                    "." +
                    " Cannot be lower than " +
                    SedaConstants.TAG_RULE_START_DATE +
                    "(" +
                    startDate +
                    ")";
                ObjectNode json = JsonHandler.createObjectNode();
                json.put("evDetTechData", errorMessage);
                throw new InvalidRuleException(
                    UnitRulesComputeStatus.CONSISTENCY,
                    JsonHandler.unprettyPrint(json),
                    unitId
                );
            }
        }
    }

    private JsonNode checkRuleNodeByID(String ruleId, String ruleType, JsonNode jsonResult, String unitId)
        throws InvalidRuleException {
        String ruleTypeWithWrongCategory = null;
        if (jsonResult != null && ParametersChecker.isNotEmpty(ruleId, ruleType)) {
            final ArrayNode rulesResult = (ArrayNode) jsonResult.get("$results");
            for (final JsonNode rule : rulesResult) {
                if (rule.get(FileRules.RULEID) != null && rule.get(FileRules.RULETYPE) != null) {
                    final String ruleIdFromList = rule.get(FileRules.RULEID).asText();
                    final String ruleTypeFromList = rule.get(FileRules.RULETYPE).asText();
                    if (ruleId.equals(ruleIdFromList)) {
                        if (ruleType.equals(ruleTypeFromList)) {
                            // Find a good Rule
                            return rule;
                        } else {
                            ruleTypeWithWrongCategory = ruleTypeFromList;
                        }
                    }
                }
            }
        }
        if (ruleTypeWithWrongCategory != null) {
            String errorMessage = String.format(BAD_CATEGORY_RULE, ruleId, ruleType, ruleTypeWithWrongCategory);
            ObjectNode json = JsonHandler.createObjectNode();
            json.put("evDetTechData", errorMessage);
            throw new InvalidRuleException(UnitRulesComputeStatus.CONSISTENCY, JsonHandler.unprettyPrint(json), unitId);
        }
        // Don't find any matching Rule
        String errorMessage = String.format(NON_EXISTING_RULE, ruleId);
        ObjectNode json = JsonHandler.createObjectNode();
        json.put("evDetTechData", errorMessage);
        throw new InvalidRuleException(UnitRulesComputeStatus.UNKNOWN, JsonHandler.unprettyPrint(json), unitId);
    }

    private LocalDate computeEndDateForRuleWithDuration(
        String startDateString,
        String ruleId,
        String currentRuleType,
        JsonNode ruleNode
    ) throws ProcessingException {
        if (!ParametersChecker.isNotEmpty(startDateString)) {
            return null;
        }
        if (ParametersChecker.isNotEmpty(ruleId, currentRuleType)) {
            LocalDate startDate = LocalDate.parse(startDateString, DATE_TIME_FORMATTER);

            if (checkRulesParameters(ruleNode)) {
                final String duration = ruleNode.get(FileRules.RULEDURATION).asText();
                final String measurement = ruleNode.get(FileRules.RULEMEASUREMENT).asText();
                if (duration.equalsIgnoreCase(ArchiveUnitUpdateUtils.UNLIMITED_RULE_DURATION)) {
                    return null;
                }
                final RuleMeasurementEnum ruleMeasurement = RuleMeasurementEnum.getEnumFromType(measurement);
                return startDate.plus(Integer.parseInt(duration), ruleMeasurement.getTemporalUnit());
            } else {
                throw new ProcessingException(CHECKS_RULES);
            }
        }
        return null;
    }

    /**
     * @param ruleNode
     */
    private boolean checkRulesParameters(JsonNode ruleNode) {
        return (
            ruleNode != null &&
            ruleNode.get(FileRules.RULEDURATION) != null &&
            ruleNode.get(FileRules.RULEMEASUREMENT) != null
        );
    }

    /**
     * Unit rules compute status values
     */
    public enum UnitRulesComputeStatus {
        /**
         * Unknow management rules
         */
        UNKNOWN,
        /**
         * Inconsistency rules
         */
        REF_INCONSISTENCY,
        /**
         * Consistency
         */
        CONSISTENCY,
    }
}
