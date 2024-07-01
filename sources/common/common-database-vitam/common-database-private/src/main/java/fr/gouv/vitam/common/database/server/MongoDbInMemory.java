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
package fr.gouv.vitam.common.database.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.database.builder.query.action.Action;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.database.collections.DynamicParserTokens;
import fr.gouv.vitam.common.database.parser.request.AbstractParser;
import fr.gouv.vitam.common.database.parser.request.GlobalDatasParser;
import fr.gouv.vitam.common.database.parser.request.adapter.VarNameAdapter;
import fr.gouv.vitam.common.database.parser.request.multiple.UpdateParserMultiple;
import fr.gouv.vitam.common.database.parser.request.single.UpdateParserSingle;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.DurationData;
import fr.gouv.vitam.common.model.QueryPattern;
import fr.gouv.vitam.common.model.administration.RuleType;
import fr.gouv.vitam.common.model.massupdate.ManagementMetadataAction;
import fr.gouv.vitam.common.model.massupdate.RuleAction;
import fr.gouv.vitam.common.model.massupdate.RuleActions;
import fr.gouv.vitam.common.model.massupdate.RuleCategoryAction;
import fr.gouv.vitam.common.model.massupdate.RuleCategoryActionDeletion;
import fr.gouv.vitam.common.model.unit.RuleModel;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.LocalDateUtil.SIMPLE_DATE_FORMAT;

/**
 * Tools to update a Mongo document (as json) with a dsl query.
 */
public class MongoDbInMemory {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MongoDbInMemory.class);

    private static final String RULES_KEY = "Rules";
    private static final String MANAGEMENT_KEY = "_mgt";
    private static final String FINAL_ACTION_KEY = "FinalAction";
    private static final String INHERITANCE = "Inheritance";
    private static final String PREVENT_INHERITANCE = "PreventInheritance";
    private static final String PREVENT_RULES_ID = "PreventRulesId";
    private static final String ACTION_ARGUMENT = "]Action argument (";
    private static final String EXPECTED_VALUE = ") expected value array for field ";
    private static final String COULD_NOT_CONVERTED = ") cannot be converted as number for field ";

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(SIMPLE_DATE_FORMAT);

    private static final SerializerProvider EMPTY_SERIALIZER_FOR_OBJECT_NODE = null;

    private final JsonNode originalDocument;
    private final DynamicParserTokens parserTokens;

    private JsonNode updatedDocument;
    private final Set<String> updatedFields;

    /**
     * @param originalDocument
     * @param parserTokens
     */
    public MongoDbInMemory(JsonNode originalDocument, DynamicParserTokens parserTokens) {
        this.originalDocument = originalDocument;
        this.parserTokens = parserTokens;
        updatedDocument = originalDocument.deepCopy();
        updatedFields = new HashSet<>();
    }

    /**
     * Update the originalDocument with the given request. If the Document is a MetadataDocument (Unit/ObjectGroup) it
     * should use a MultipleQuery Parser
     *
     * @param request The given update request
     * @param isMultiple true if the UpdateParserMultiple must be used (Unit/ObjectGroup)
     * @param varNameAdapter VarNameAdapter to use
     * @return the updated document
     * @throws InvalidParseOperationException
     */
    public JsonNode getUpdateJson(JsonNode request, boolean isMultiple, VarNameAdapter varNameAdapter)
        throws InvalidParseOperationException {
        final AbstractParser<?> parser;

        if (isMultiple) {
            parser = new UpdateParserMultiple(varNameAdapter);
        } else {
            parser = new UpdateParserSingle(varNameAdapter);
        }

        parser.parse(request);

        return getUpdateJson(parser);
    }

    /**
     * Update the originalDocument with the given parser (containing the request)
     *
     * @param requestParser The given parser containing the update request
     * @return the updated document
     * @throws InvalidParseOperationException
     */
    public JsonNode getUpdateJson(AbstractParser<?> requestParser) throws InvalidParseOperationException {
        List<Action> actions = requestParser.getRequest().getActions();
        if (actions == null || actions.isEmpty()) {
            LOGGER.info("No action on request");
            return updatedDocument;
        } else {
            for (Action action : actions) {
                final BuilderToken.UPDATEACTION req = action.getUPDATEACTION();
                final JsonNode content = action.getCurrentAction().get(req.exactToken());
                switch (req) {
                    case ADD:
                        add(req, content);
                        break;
                    case INC:
                        inc(req, content);
                        break;
                    case MIN:
                        min(req, content);
                        break;
                    case MAX:
                        max(req, content);
                        break;
                    case POP:
                        pop(req, content);
                        break;
                    case PULL:
                        pull(req, content);
                        break;
                    case PUSH:
                        push(req, content);
                        break;
                    case RENAME:
                        rename(req, content);
                        break;
                    case SET:
                        set(content);
                        break;
                    case UNSET:
                        unset(content);
                        break;
                    case SETREGEX:
                        setRegex(content);
                        break;
                    default:
                        break;
                }
            }
        }
        return updatedDocument;
    }

    /**
     * Update the originalDocument with the given ruleActions
     *
     * @param ruleActions The given ruleActions containing the updates
     * @return the updated document
     * @throws InvalidParseOperationException
     */
    public JsonNode getUpdateJsonForRule(RuleActions ruleActions, Map<String, DurationData> bindRuleToDuration)
        throws InvalidParseOperationException, RuleUpdateException {
        if (ruleActions != null) {
            final ObjectNode initialMgt = (ObjectNode) getOrCreateEmptyNodeByName(
                updatedDocument,
                MANAGEMENT_KEY,
                false
            );

            applyAddRuleAction(ruleActions.getAdd(), initialMgt, bindRuleToDuration);
            applyUpdateRuleAction(ruleActions.getUpdate(), initialMgt, bindRuleToDuration);
            applyDeleteRuleAction(ruleActions.getDelete(), initialMgt);

            updateArchiveUnitProfile(ruleActions);

            JsonHandler.setNodeInPath((ObjectNode) updatedDocument, MANAGEMENT_KEY, initialMgt, true);
        }

        return updatedDocument;
    }

    private void applyAddRuleAction(
        final List<Map<String, RuleCategoryAction>> ruleActions,
        final ObjectNode initialMgt,
        Map<String, DurationData> bindRuleToDuration
    ) throws RuleUpdateException, InvalidParseOperationException {
        if (ruleActions == null || ruleActions.isEmpty()) return;

        for (Map<String, RuleCategoryAction> item : ruleActions) {
            for (Entry<String, RuleCategoryAction> entry : item.entrySet()) {
                String category = entry.getKey();
                RuleCategoryAction ruleCategoryAction = entry.getValue();

                ObjectNode initialRuleCategory = (ObjectNode) getOrCreateEmptyNodeByName(initialMgt, category, false);

                handleFinalAction(initialRuleCategory, ruleCategoryAction, category);
                handleClassificationProperties(initialRuleCategory, ruleCategoryAction, category);
                handleInheritanceProperties(initialRuleCategory, ruleCategoryAction);
                handleAddPreventRulesProperties(initialRuleCategory, ruleCategoryAction);

                // add rules
                if (!ruleCategoryAction.getRules().isEmpty()) {
                    ArrayNode initialRules = (ArrayNode) getOrCreateEmptyNodeByName(
                        initialRuleCategory,
                        RULES_KEY,
                        true
                    );
                    for (RuleAction ruleAction : ruleCategoryAction.getRules()) {
                        if (!hasRuleDefined(ruleAction.getRule(), initialRules)) {
                            JsonNode newRule = getJsonNodeFromRuleAction(category, ruleAction, bindRuleToDuration);
                            initialRules.add(newRule);
                        }
                    }
                    initialRuleCategory.set(RULES_KEY, initialRules);
                }

                // set category
                initialMgt.set(category, initialRuleCategory);
            }
        }
    }

    private boolean hasRuleDefined(final String ruleId, final ArrayNode rules) {
        for (JsonNode rule : rules) {
            if (ruleId.equals(rule.get("Rule").textValue())) {
                return true;
            }
        }
        return false;
    }

    private void applyUpdateRuleAction(
        final List<Map<String, RuleCategoryAction>> ruleActions,
        final ObjectNode initialMgt,
        Map<String, DurationData> bindRuleToDuration
    ) throws RuleUpdateException {
        if (ruleActions == null || ruleActions.isEmpty()) return;

        for (Map<String, RuleCategoryAction> item : ruleActions) {
            for (Entry<String, RuleCategoryAction> entry : item.entrySet()) {
                String category = entry.getKey();
                RuleCategoryAction ruleCategoryAction = entry.getValue();

                ObjectNode initialRuleCategory = (ObjectNode) getOrCreateEmptyNodeByName(initialMgt, category, false);

                handleFinalAction(initialRuleCategory, ruleCategoryAction, category);
                handleClassificationProperties(initialRuleCategory, ruleCategoryAction, category);
                handleInheritanceProperties(initialRuleCategory, ruleCategoryAction);

                if (!ruleCategoryAction.getRules().isEmpty()) {
                    Map<String, RuleAction> rulesToUpdate = ruleCategoryAction
                        .getRules()
                        .stream()
                        .collect(Collectors.toMap(RuleAction::getOldRule, Function.identity()));
                    ArrayNode initialRules = (ArrayNode) getOrCreateEmptyNodeByName(
                        initialRuleCategory,
                        RULES_KEY,
                        true
                    );
                    for (JsonNode initialRule : initialRules) {
                        ObjectNode node = (ObjectNode) initialRule;
                        String actualRule = node.get(RuleModel.RULE).asText();
                        if (rulesToUpdate.containsKey(actualRule)) {
                            updateJsonNodeUsingRuleAction(
                                category,
                                node,
                                rulesToUpdate.get(actualRule),
                                bindRuleToDuration
                            );
                        }
                    }
                    initialRuleCategory.set(RULES_KEY, initialRules);
                }

                initialMgt.set(category, initialRuleCategory);
            }
        }
    }

    private void applyDeleteRuleAction(
        List<Map<String, RuleCategoryActionDeletion>> ruleActions,
        ObjectNode initialMgt
    ) {
        ruleActions
            .stream()
            .flatMap(categoryItems -> categoryItems.entrySet().stream())
            .forEach(categoryItem -> deletionRule(initialMgt, categoryItem.getKey(), categoryItem.getValue()));
    }

    private void deletionRule(ObjectNode initialMgt, String categoryName, RuleCategoryActionDeletion category) {
        AtomicBoolean updatedInheritance = new AtomicBoolean(false);
        ObjectNode initialRuleCategory = (ObjectNode) getOrCreateEmptyNodeByName(initialMgt, categoryName, false);
        ObjectNode inheritance = (ObjectNode) initialRuleCategory.get(INHERITANCE);

        if (initialMgt.isEmpty(EMPTY_SERIALIZER_FOR_OBJECT_NODE)) {
            return;
        }
        if (Objects.isNull(category) || category.isEmpty()) {
            initialMgt.remove(categoryName);
            return;
        }

        JsonNode categoryAsJsonNode = initialMgt.path(categoryName);
        if (categoryAsJsonNode.isMissingNode() || categoryAsJsonNode.isNull() || !categoryAsJsonNode.isObject()) {
            return;
        }

        ObjectNode initialCategory = (ObjectNode) categoryAsJsonNode;

        if (Objects.nonNull(category.getRules())) {
            category
                .getRules()
                .ifPresent(rules -> {
                    List<String> rulesToDelete = rules.stream().map(RuleAction::getRule).collect(Collectors.toList());
                    ArrayNode initialRules = (ArrayNode) getOrCreateEmptyNodeByName(initialCategory, RULES_KEY, true);
                    ArrayNode filteredRules = JsonHandler.createArrayNode();
                    initialRules.forEach(node -> {
                        if (!rulesToDelete.contains(node.get(RuleModel.RULE).asText())) {
                            filteredRules.add(node);
                        }
                    });
                    initialCategory.set(RULES_KEY, filteredRules);
                });
        }

        if (inheritance == null) {
            inheritance = JsonHandler.createObjectNode();
        }

        // add preventInheritance
        if (category.getPreventInheritance() != null) {
            updatedInheritance.set(true);
            inheritance.put(PREVENT_INHERITANCE, category.getPreventInheritance().get());
        }

        if (CollectionUtils.isNotEmpty(category.getPreventRulesIdToRemove())) {
            if (CollectionUtils.isNotEmpty(category.getPreventRulesId())) {
                LOGGER.error(
                    "Invalid request. Cannot set both PreventRulesIdToRemove & PreventRulesId. " +
                    JsonHandler.unprettyPrint(category)
                );
                throw new IllegalStateException(
                    "Invalid request. Cannot set both PreventRulesIdToRemove & PreventRulesId."
                );
            }

            List<String> preventRulesToDelete = new ArrayList<>(category.getPreventRulesIdToRemove());
            ArrayNode initialRules = (ArrayNode) getOrCreateEmptyNodeByName(inheritance, PREVENT_RULES_ID, true);
            ArrayNode filteredRules = JsonHandler.createArrayNode();
            initialRules.forEach(node -> {
                if (!preventRulesToDelete.contains(node.textValue())) {
                    filteredRules.add(node);
                }
            });
            inheritance.set(PREVENT_RULES_ID, filteredRules);
        }

        if (updatedInheritance.get()) {
            initialRuleCategory.set(INHERITANCE, inheritance);
        }

        if (Objects.nonNull(category.getRules()) && category.getRules().isEmpty()) {
            initialCategory.remove(SedaConstants.TAG_RULE_RULE);
        }
        if (Objects.nonNull(category.getFinalAction())) {
            initialCategory.remove(SedaConstants.TAG_RULE_FINAL_ACTION);
        }
        if (Objects.nonNull(category.getClassificationAudience())) {
            initialCategory.remove(SedaConstants.TAG_RULE_CLASSIFICATION_AUDIENCE);
        }
        if (Objects.nonNull(category.getClassificationReassessingDate())) {
            initialCategory.remove(SedaConstants.TAG_RULE_CLASSIFICATION_REASSESSING_DATE);
        }
        if (Objects.nonNull(category.getNeedReassessingAuthorization())) {
            initialCategory.remove(SedaConstants.TAG_RULE_CLASSIFICATION_NEED_REASSESSING_AUTHORIZATION);
        }

        JsonNode inheritanceAsJsonNode = initialCategory.path(INHERITANCE);
        if (
            inheritanceAsJsonNode.isMissingNode() || inheritanceAsJsonNode.isNull() || !inheritanceAsJsonNode.isObject()
        ) {
            return;
        }

        ObjectNode initialInheritance = (ObjectNode) inheritanceAsJsonNode;
        if (Objects.nonNull(category.getPreventInheritance())) {
            initialInheritance.remove(SedaConstants.TAG_RULE_PREVENT_INHERITANCE);
        }
        if (
            Objects.nonNull(category.getPreventRulesId()) &&
            CollectionUtils.isEmpty(category.getPreventRulesIdToRemove())
        ) {
            initialInheritance.remove(PREVENT_RULES_ID);
        }

        if (initialInheritance.isEmpty(EMPTY_SERIALIZER_FOR_OBJECT_NODE)) {
            initialCategory.remove(INHERITANCE);
        }

        if (initialCategory.isEmpty(EMPTY_SERIALIZER_FOR_OBJECT_NODE)) {
            initialMgt.remove(categoryName);
        }
    }

    private JsonNode getOrCreateEmptyNodeByName(JsonNode parent, String fieldName, boolean acceptArray) {
        return parent.hasNonNull(fieldName)
            ? parent.get(fieldName)
            : (acceptArray ? JsonHandler.createArrayNode() : JsonHandler.createObjectNode());
    }

    private Boolean shouldDeleteAUP(RuleActions ruleActions) {
        ManagementMetadataAction deleteActions = ruleActions.getDeleteMetadata();
        return deleteActions != null && deleteActions.getArchiveUnitProfile() != null;
    }

    private void updateArchiveUnitProfile(RuleActions ruleActions) {
        if (shouldDeleteAUP(ruleActions)) {
            ((ObjectNode) updatedDocument).remove(SedaConstants.TAG_ARCHIVE_UNIT_PROFILE);
            return;
        }

        ManagementMetadataAction newMetadata = ruleActions.getAddOrUpdateMetadata();
        if (newMetadata != null && StringUtils.isNotBlank(newMetadata.getArchiveUnitProfile())) {
            ((ObjectNode) updatedDocument).put(
                    SedaConstants.TAG_ARCHIVE_UNIT_PROFILE,
                    newMetadata.getArchiveUnitProfile()
                );
        }
    }

    private void handleFinalAction(
        ObjectNode initialRuleCategory,
        RuleCategoryAction ruleCategoryAction,
        String category
    ) {
        if (SedaConstants.TAG_RULE_APPRAISAL.equals(category) || SedaConstants.TAG_RULE_STORAGE.equals(category)) {
            String finalAction = ruleCategoryAction.getFinalAction();
            if (finalAction != null) {
                initialRuleCategory.put(FINAL_ACTION_KEY, finalAction);
            }
        }
    }

    private void handleClassificationProperties(
        ObjectNode initialRuleCategory,
        RuleCategoryAction ruleCategoryAction,
        String category
    ) {
        if (!SedaConstants.TAG_RULE_CLASSIFICATION.equals(category)) {
            return;
        }

        String classificationLevel = ruleCategoryAction.getClassificationLevel();
        if (classificationLevel != null) {
            initialRuleCategory.put(SedaConstants.TAG_RULE_CLASSIFICATION_LEVEL, classificationLevel);
        }

        String classificationOwner = ruleCategoryAction.getClassificationOwner();
        if (classificationOwner != null) {
            initialRuleCategory.put(SedaConstants.TAG_RULE_CLASSIFICATION_OWNER, classificationOwner);
        }

        String classificationReassessingDate = ruleCategoryAction.getClassificationReassessingDate();
        if (classificationReassessingDate != null) {
            initialRuleCategory.put(
                SedaConstants.TAG_RULE_CLASSIFICATION_REASSESSING_DATE,
                classificationReassessingDate
            );
        }

        String classificationAudience = ruleCategoryAction.getClassificationAudience();
        if (classificationAudience != null) {
            initialRuleCategory.put(SedaConstants.TAG_RULE_CLASSIFICATION_AUDIENCE, classificationAudience);
        }

        Boolean needReassessingAuthorization = ruleCategoryAction.getNeedReassessingAuthorization();
        if (needReassessingAuthorization != null) {
            initialRuleCategory.put(
                SedaConstants.TAG_RULE_CLASSIFICATION_NEED_REASSESSING_AUTHORIZATION,
                needReassessingAuthorization
            );
        }
    }

    private void handleAddPreventRulesProperties(ObjectNode initialRuleCategory, RuleCategoryAction ruleCategoryAction)
        throws InvalidParseOperationException {
        if (CollectionUtils.isEmpty(ruleCategoryAction.getPreventRulesIdToAdd())) {
            return;
        }

        if (CollectionUtils.isNotEmpty(ruleCategoryAction.getPreventRulesId())) {
            LOGGER.error(
                "Invalid request. Cannot set both PreventRulesIdToAdd & PreventRulesId. " +
                JsonHandler.unprettyPrint(ruleCategoryAction)
            );
            throw new IllegalStateException("Invalid request. Cannot set both PreventRulesIdToAdd & PreventRulesId.");
        }

        ObjectNode inheritance = (ObjectNode) initialRuleCategory.get(INHERITANCE);
        if (inheritance == null) {
            inheritance = JsonHandler.createObjectNode();
        }

        Set<String> preventRuleIds = ruleCategoryAction.getPreventRulesIdToAdd();
        JsonNode initialRules = getOrCreateEmptyNodeByName(inheritance, PREVENT_RULES_ID, true);
        preventRuleIds.addAll(JsonHandler.getFromJsonNode(initialRules, new TypeReference<List<String>>() {}));
        inheritance.set(PREVENT_RULES_ID, preventRulesToNode(preventRuleIds));

        initialRuleCategory.set(INHERITANCE, inheritance);
    }

    private void handleInheritanceProperties(ObjectNode initialRuleCategory, RuleCategoryAction ruleCategoryAction) {
        boolean updatedInheritance = false;
        ObjectNode inheritance = (ObjectNode) initialRuleCategory.get(INHERITANCE);

        if (inheritance == null) {
            inheritance = JsonHandler.createObjectNode();
        }

        // add preventInheritance
        Boolean preventInheritance = ruleCategoryAction.getPreventInheritance();
        if (preventInheritance != null) {
            updatedInheritance = true;
            inheritance.put(PREVENT_INHERITANCE, preventInheritance);
        }

        // add preventRulesId
        Set<String> preventRuleIds = ruleCategoryAction.getPreventRulesId();
        if (preventRuleIds != null && CollectionUtils.isEmpty(ruleCategoryAction.getPreventRulesIdToAdd())) {
            updatedInheritance = true;
            inheritance.set(PREVENT_RULES_ID, preventRulesToNode(preventRuleIds));
        }

        if (updatedInheritance) {
            initialRuleCategory.set(INHERITANCE, inheritance);
        }
    }

    private ArrayNode preventRulesToNode(Set<String> preventRuleIdsToAdd) {
        try {
            return (ArrayNode) JsonHandler.toJsonNode(preventRuleIdsToAdd);
        } catch (InvalidParseOperationException e) {
            throw new VitamRuntimeException("Cannot transform preventRulesId.", e);
        }
    }

    private JsonNode getJsonNodeFromRuleAction(
        String category,
        RuleAction ruleAction,
        Map<String, DurationData> bindRuleToDuration
    ) throws RuleUpdateException {
        ObjectNode newRule = JsonHandler.createObjectNode();
        String ruleId = ruleAction.getRule();
        newRule.put(RuleModel.RULE, ruleId);

        applyAttributeUpdates(newRule, ruleAction);

        computeEndDate(newRule, category, bindRuleToDuration);

        return newRule;
    }

    private void updateJsonNodeUsingRuleAction(
        String category,
        ObjectNode unitRule,
        RuleAction ruleAction,
        Map<String, DurationData> bindRuleToDuration
    ) throws RuleUpdateException {
        if (ruleAction.getRule() != null) {
            unitRule.put(RuleModel.RULE, ruleAction.getRule());
        }

        applyAttributeUpdates(unitRule, ruleAction);

        computeEndDate(unitRule, category, bindRuleToDuration);
    }

    private void applyAttributeUpdates(ObjectNode unitRule, RuleAction ruleAction) {
        if (ruleAction.getStartDate() != null) {
            unitRule.put(RuleModel.START_DATE, ruleAction.getStartDate());
        }
        if (Boolean.TRUE.equals(ruleAction.getDeleteStartDate())) {
            unitRule.remove(RuleModel.START_DATE);
        }

        if (ruleAction.getHoldEndDate() != null) {
            unitRule.put(RuleModel.HOLD_END_DATE, ruleAction.getHoldEndDate());
        }
        if (Boolean.TRUE.equals(ruleAction.getDeleteHoldEndDate())) {
            unitRule.remove(RuleModel.HOLD_END_DATE);
        }

        if (ruleAction.getHoldOwner() != null) {
            unitRule.put(RuleModel.HOLD_OWNER, ruleAction.getHoldOwner());
        }
        if (Boolean.TRUE.equals(ruleAction.getDeleteHoldOwner())) {
            unitRule.remove(RuleModel.HOLD_OWNER);
        }

        if (ruleAction.getHoldReason() != null) {
            unitRule.put(RuleModel.HOLD_REASON, ruleAction.getHoldReason());
        }
        if (Boolean.TRUE.equals(ruleAction.getDeleteHoldReason())) {
            unitRule.remove(RuleModel.HOLD_REASON);
        }

        if (ruleAction.getHoldReassessingDate() != null) {
            unitRule.put(RuleModel.HOLD_REASSESSING_DATE, ruleAction.getHoldReassessingDate());
        }
        if (Boolean.TRUE.equals(ruleAction.getDeleteHoldReassessingDate())) {
            unitRule.remove(RuleModel.HOLD_REASSESSING_DATE);
        }

        if (ruleAction.getPreventRearrangement() != null) {
            unitRule.put(RuleModel.PREVENT_REARRANGEMENT, ruleAction.getPreventRearrangement());
        }
        if (Boolean.TRUE.equals(ruleAction.getDeletePreventRearrangement())) {
            unitRule.remove(RuleModel.PREVENT_REARRANGEMENT);
        }
    }

    private void ensureStartDateIsBeforeHoldEndDate(ObjectNode unitRule) throws RuleUpdateException {
        if (unitRule.has(RuleModel.START_DATE) && unitRule.has(RuleModel.HOLD_END_DATE)) {
            String startDate = unitRule.get(RuleModel.START_DATE).asText();
            String holdEndDate = unitRule.get(RuleModel.HOLD_END_DATE).asText();

            LocalDate localStartDate = LocalDate.parse(startDate, DATE_TIME_FORMATTER);
            LocalDate localHoldEndDate = LocalDate.parse(holdEndDate, DATE_TIME_FORMATTER);

            if (localHoldEndDate.isBefore(localStartDate)) {
                throw new RuleUpdateException(
                    RuleUpdateErrorCode.HOLD_END_DATE_BEFORE_START_DATE,
                    "HoldEndDate (" + holdEndDate + ") cannot be before StartDate (" + startDate + ")"
                );
            }
        }
    }

    private void ensureNoHoldEndDateSet(ObjectNode unitRule) throws RuleUpdateException {
        if (unitRule.has(RuleModel.HOLD_END_DATE)) {
            throw new RuleUpdateException(
                RuleUpdateErrorCode.HOLD_END_DATE_ONLY_ALLOWED_FOR_HOLD_RULE_WITH_UNDEFINED_DURATION,
                String.format(
                    "HoldEndDate (%s) cannot be defined for rule %s",
                    unitRule.get(RuleModel.HOLD_END_DATE).asText(),
                    unitRule.get(RuleModel.RULE).asText()
                )
            );
        }
    }

    private void computeEndDate(ObjectNode unitRule, String ruleCategory, Map<String, DurationData> bindRuleToDuration)
        throws RuleUpdateException {
        String ruleId = unitRule.get(RuleModel.RULE).asText();
        boolean isHoldRuleWithUndefinedDuration =
            RuleType.HoldRule.isNameEquals(ruleCategory) &&
            bindRuleToDuration.containsKey(ruleId) &&
            bindRuleToDuration.get(ruleId).getDurationUnit() == null;

        if (isHoldRuleWithUndefinedDuration) {
            if (unitRule.has(RuleModel.HOLD_END_DATE)) {
                String holdEndDate = unitRule.get(RuleModel.HOLD_END_DATE).asText();
                unitRule.put(RuleModel.END_DATE, holdEndDate);
            } else {
                unitRule.remove(RuleModel.END_DATE);
            }

            ensureStartDateIsBeforeHoldEndDate(unitRule);
            return;
        }

        ensureNoHoldEndDateSet(unitRule);
        unitRule.remove(RuleModel.END_DATE);

        DurationData durationData = bindRuleToDuration.get(ruleId);

        if (
            durationData == null ||
            unitRule.path(RuleModel.START_DATE).isNull() ||
            unitRule.path(RuleModel.START_DATE).isMissingNode()
        ) {
            return;
        }

        String startDateString = unitRule.get(RuleModel.START_DATE).textValue();
        if (startDateString == null) {
            return;
        }
        LocalDate startDate = LocalDateUtil.getLocalDateFromSimpleFormattedDate(startDateString);

        Integer duration = durationData.getDurationValue();
        TemporalUnit temporalUnit = durationData.getDurationUnit();

        LocalDate endDate = startDate.plus(duration, temporalUnit);
        unitRule.put(RuleModel.END_DATE, LocalDateUtil.getFormattedSimpleDate(endDate));
    }

    /**
     * Reset the updatedDocument with the original values
     */
    @VisibleForTesting
    public void resetUpdatedAU() {
        updatedDocument = originalDocument.deepCopy();
        updatedFields.clear();
    }

    private void inc(final BuilderToken.UPDATEACTION req, final JsonNode content)
        throws InvalidParseOperationException {
        final Entry<String, JsonNode> element = JsonHandler.checkUnicity(req.exactToken(), content);
        final String fieldName = element.getKey();
        Number nodeValue = getNumberValue(req.name(), fieldName);

        JsonNode actionValue = element.getValue();

        if (!actionValue.isNumber()) {
            throw new InvalidParseOperationException(
                "[ INC" + ACTION_ARGUMENT + actionValue + COULD_NOT_CONVERTED + fieldName
            );
        }

        String[] fieldNamePath = fieldName.split("[.]");
        String lastNodeName = fieldNamePath[fieldNamePath.length - 1];

        final ObjectNode parentNode = (ObjectNode) JsonHandler.getParentNodeByPath(updatedDocument, fieldName, false);
        if (parentNode != null) {
            if (nodeValue.getClass().equals(Integer.class)) {
                parentNode.put(lastNodeName, element.getValue().intValue() + nodeValue.intValue());
            } else if (nodeValue.getClass().equals(Long.class)) {
                parentNode.put(lastNodeName, element.getValue().longValue() + nodeValue.longValue());
            } else if (nodeValue.getClass().equals(Float.class)) {
                parentNode.put(lastNodeName, element.getValue().floatValue() + nodeValue.floatValue());
            } else if (nodeValue.getClass().equals(Double.class)) {
                parentNode.put(lastNodeName, element.getValue().doubleValue() + nodeValue.doubleValue());
            } else if (nodeValue.getClass().equals(BigDecimal.class)) {
                parentNode.put(
                    lastNodeName,
                    element.getValue().decimalValue().add(BigDecimal.valueOf(nodeValue.doubleValue()))
                );
            } else if (nodeValue.getClass().equals(BigInteger.class)) {
                parentNode.put(
                    lastNodeName,
                    element.getValue().bigIntegerValue().add(BigInteger.valueOf(nodeValue.intValue()))
                );
            }
        }
        updatedFields.add(fieldName);
    }

    private void unset(final JsonNode content) {
        final Iterator<JsonNode> iterator = content.elements();
        while (iterator.hasNext()) {
            final JsonNode element = iterator.next();
            String fieldName = element.asText();
            JsonNode node = JsonHandler.getParentNodeByPath(updatedDocument, fieldName, false);
            if (node != null) {
                String[] fieldNamePath = fieldName.split("[.]");
                String lastNodeName = fieldNamePath[fieldNamePath.length - 1];
                ((ObjectNode) node).remove(lastNodeName);
                updatedFields.add(fieldName);
            }
        }
    }

    private void set(final JsonNode content) throws InvalidParseOperationException {
        final Iterator<Entry<String, JsonNode>> iterator = content.fields();
        while (iterator.hasNext()) {
            final Entry<String, JsonNode> element = iterator.next();
            String fieldName = element.getKey();
            if (parserTokens.isAnArray(fieldName)) {
                ArrayNode arrayNode = GlobalDatasParser.getArray(element.getValue());
                JsonHandler.setNodeInPath((ObjectNode) updatedDocument, fieldName, arrayNode, true);
            } else {
                JsonHandler.setNodeInPath((ObjectNode) updatedDocument, fieldName, element.getValue(), true);
            }
            updatedFields.add(fieldName);
        }
    }

    private void setRegex(final JsonNode content) throws InvalidParseOperationException {
        QueryPattern queryPattern = JsonHandler.getFromJsonNodeLowerCamelCase(content, QueryPattern.class);
        String fieldName = queryPattern.getTarget();
        ObjectNode parentObjectNode = (ObjectNode) JsonHandler.getParentNodeByPath(updatedDocument, fieldName, false);

        String lastFieldName = JsonHandler.getLastFieldName(fieldName);
        if (parentObjectNode == null || !parentObjectNode.has(lastFieldName)) {
            return;
        }
        JsonNode jsonNode = parentObjectNode.get(lastFieldName);

        Pattern pattern = Pattern.compile(queryPattern.getControlPattern());

        if (jsonNode.isTextual()) {
            // Update text field
            String stringToSearch = jsonNode.asText();
            String newString = replaceAll(pattern, stringToSearch, queryPattern.getUpdatePattern());
            if (!stringToSearch.equals(newString)) {
                parentObjectNode.put(lastFieldName, newString);
                updatedFields.add(fieldName);
            }
        } else if (jsonNode.isArray()) {
            // Update array field
            ArrayNode arrayNode = (ArrayNode) jsonNode;
            for (int i = 0; i < arrayNode.size(); i++) {
                JsonNode item = arrayNode.get(i);
                if (item.isTextual()) {
                    String stringToSearch = item.asText();
                    String newString = replaceAll(pattern, stringToSearch, queryPattern.getUpdatePattern());
                    if (!stringToSearch.equals(newString)) {
                        arrayNode.set(i, new TextNode(newString));
                        updatedFields.add(fieldName);
                    }
                }
            }
        }
    }

    private String replaceAll(Pattern pattern, String stringToSearch, String replacement) {
        return pattern.matcher(stringToSearch).replaceAll(replacement);
    }

    private void min(final BuilderToken.UPDATEACTION req, final JsonNode content)
        throws InvalidParseOperationException {
        final Entry<String, JsonNode> element = JsonHandler.checkUnicity(req.exactToken(), content);
        final String fieldName = element.getKey();
        Number nodeValue = getNumberValue(req.name(), fieldName);

        JsonNode actionValue = element.getValue();
        if (!actionValue.isNumber()) {
            throw new InvalidParseOperationException(
                "[ MIN" + ACTION_ARGUMENT + actionValue + COULD_NOT_CONVERTED + fieldName
            );
        }

        String[] fieldNamePath = fieldName.split("[.]");
        String lastNodeName = fieldNamePath[fieldNamePath.length - 1];
        final ObjectNode parentNode = (ObjectNode) JsonHandler.getParentNodeByPath(updatedDocument, fieldName, false);
        if (parentNode != null) {
            if (nodeValue.getClass().equals(Integer.class)) {
                parentNode.put(lastNodeName, Math.min(element.getValue().intValue(), nodeValue.intValue()));
            } else if (nodeValue.getClass().equals(Long.class)) {
                parentNode.put(lastNodeName, Math.min(element.getValue().longValue(), nodeValue.longValue()));
            } else if (nodeValue.getClass().equals(Float.class)) {
                parentNode.put(lastNodeName, Math.min(element.getValue().floatValue(), nodeValue.floatValue()));
            } else if (nodeValue.getClass().equals(Double.class)) {
                parentNode.put(lastNodeName, Math.min(element.getValue().doubleValue(), nodeValue.doubleValue()));
            } else if (nodeValue.getClass().equals(BigDecimal.class)) {
                parentNode.put(
                    lastNodeName,
                    element.getValue().decimalValue().min(BigDecimal.valueOf(nodeValue.doubleValue()))
                );
            } else if (nodeValue.getClass().equals(BigInteger.class)) {
                parentNode.put(
                    lastNodeName,
                    element.getValue().bigIntegerValue().min(BigInteger.valueOf(nodeValue.intValue()))
                );
            }
        }
        updatedFields.add(fieldName);
    }

    private void max(final BuilderToken.UPDATEACTION req, final JsonNode content)
        throws InvalidParseOperationException {
        final Entry<String, JsonNode> element = JsonHandler.checkUnicity(req.exactToken(), content);
        final String fieldName = element.getKey();
        Number nodeValue = getNumberValue(req.name(), fieldName);

        JsonNode actionValue = element.getValue();
        if (!actionValue.isNumber()) {
            throw new InvalidParseOperationException(
                "[ MAX" + ACTION_ARGUMENT + actionValue + COULD_NOT_CONVERTED + fieldName
            );
        }

        String[] fieldNamePath = fieldName.split("[.]");
        String lastNodeName = fieldNamePath[fieldNamePath.length - 1];
        final ObjectNode parentNode = (ObjectNode) JsonHandler.getParentNodeByPath(updatedDocument, fieldName, false);
        if (parentNode != null) {
            if (nodeValue.getClass().equals(Integer.class)) {
                parentNode.put(lastNodeName, Math.max(element.getValue().intValue(), nodeValue.intValue()));
            } else if (nodeValue.getClass().equals(Long.class)) {
                parentNode.put(lastNodeName, Math.max(element.getValue().longValue(), nodeValue.longValue()));
            } else if (nodeValue.getClass().equals(Float.class)) {
                parentNode.put(lastNodeName, Math.max(element.getValue().floatValue(), nodeValue.floatValue()));
            } else if (nodeValue.getClass().equals(Double.class)) {
                parentNode.put(lastNodeName, Math.max(element.getValue().doubleValue(), nodeValue.doubleValue()));
            } else if (nodeValue.getClass().equals(BigDecimal.class)) {
                parentNode.put(
                    lastNodeName,
                    element.getValue().decimalValue().max(BigDecimal.valueOf(nodeValue.doubleValue()))
                );
            } else if (nodeValue.getClass().equals(BigInteger.class)) {
                parentNode.put(
                    lastNodeName,
                    element.getValue().bigIntegerValue().max(BigInteger.valueOf(nodeValue.intValue()))
                );
            }
        }
        updatedFields.add(fieldName);
    }

    private void rename(final BuilderToken.UPDATEACTION req, final JsonNode content)
        throws InvalidParseOperationException {
        final Entry<String, JsonNode> element = JsonHandler.checkUnicity(req.exactToken(), content);
        final String fieldName = element.getKey();
        JsonNode value = JsonHandler.getNodeByPath(updatedDocument, fieldName, false);
        if (value == null) {
            throw new InvalidParseOperationException(
                "[" + "RENAME" + "]Can't rename field " + fieldName + " because it doesn't exist"
            );
        }

        JsonNode parent = JsonHandler.getParentNodeByPath(updatedDocument, fieldName, false);
        String[] fieldNamePath = fieldName.split("[.]");
        String lastNodeName = fieldNamePath[fieldNamePath.length - 1];
        if (parent != null) {
            ((ObjectNode) parent).remove(lastNodeName);
        }
        String newFieldName = element.getValue().asText();
        JsonHandler.setNodeInPath((ObjectNode) updatedDocument, newFieldName, value, true);
        updatedFields.add(fieldName);
        updatedFields.add(newFieldName);
    }

    private void push(final BuilderToken.UPDATEACTION req, final JsonNode content)
        throws InvalidParseOperationException {
        final Entry<String, JsonNode> element = JsonHandler.checkUnicity(req.exactToken(), content);
        final String fieldName = element.getKey();
        if (!(element.getValue() instanceof ArrayNode)) {
            throw new InvalidParseOperationException(
                "[ PUSH" + ACTION_ARGUMENT + element.getValue() + EXPECTED_VALUE + fieldName
            );
        }
        final ArrayNode array = (ArrayNode) element.getValue();
        ArrayNode node = (ArrayNode) getArrayValue(req.name(), fieldName);
        final Iterator<JsonNode> iterator = array.elements();
        while (iterator.hasNext()) {
            node.add(iterator.next());
        }
        updatedFields.add(fieldName);
    }

    private void pull(final BuilderToken.UPDATEACTION req, final JsonNode content)
        throws InvalidParseOperationException {
        final Entry<String, JsonNode> element = JsonHandler.checkUnicity(req.exactToken(), content);
        final String fieldName = element.getKey();
        if (!(element.getValue() instanceof ArrayNode)) {
            throw new InvalidParseOperationException(
                "[ PULL" + ACTION_ARGUMENT + element.getValue() + EXPECTED_VALUE + fieldName
            );
        }
        final ArrayNode array = (ArrayNode) element.getValue();
        ArrayNode node = (ArrayNode) getArrayValue(req.name(), fieldName);
        final List<Integer> indexesToRemove = new ArrayList<>();
        final Iterator<JsonNode> iterator = array.elements();
        // TODO: optimize ! review loop order for the best !
        while (iterator.hasNext()) {
            JsonNode pullValue = iterator.next();
            Iterator<JsonNode> originIt = node.elements();
            int index = 0;
            while (originIt.hasNext()) {
                if (originIt.next().asText().equals(pullValue.asText())) {
                    indexesToRemove.add(index);
                }
                index++;
            }
        }
        Collections.sort(indexesToRemove);
        for (int i = indexesToRemove.size() - 1; i >= 0; i--) {
            node.remove(indexesToRemove.get(i));
        }
        if (!indexesToRemove.isEmpty()) {
            updatedFields.add(fieldName);
        }
    }

    private void add(final BuilderToken.UPDATEACTION req, final JsonNode content)
        throws InvalidParseOperationException {
        final Entry<String, JsonNode> element = JsonHandler.checkUnicity(req.exactToken(), content);
        final String fieldName = element.getKey();
        if (!(element.getValue() instanceof ArrayNode)) {
            throw new InvalidParseOperationException(
                "[ ADD" + ACTION_ARGUMENT + element.getValue() + EXPECTED_VALUE + fieldName
            );
        }
        final ArrayNode array = (ArrayNode) element.getValue();

        ArrayNode node = (ArrayNode) getArrayValue(req.name(), fieldName);
        final Iterator<JsonNode> iterator = array.elements();

        // TODO: optimize ! review loop order for the best !
        while (iterator.hasNext()) {
            JsonNode newNode = iterator.next();
            Iterator<JsonNode> originIt = node.elements();
            boolean mustAdd = true;
            while (originIt.hasNext()) {
                if (originIt.next().asText().equals(newNode.asText())) {
                    mustAdd = false;
                }
            }
            if (mustAdd) {
                node.add(newNode);
                updatedFields.add(fieldName);
            }
        }
    }

    private void pop(final BuilderToken.UPDATEACTION req, final JsonNode content)
        throws InvalidParseOperationException {
        final Entry<String, JsonNode> element = JsonHandler.checkUnicity(req.exactToken(), content);
        final String fieldName = element.getKey();
        ArrayNode node = (ArrayNode) getArrayValue(req.name(), fieldName);

        JsonNode actionValue = element.getValue();
        if (!actionValue.isNumber()) {
            throw new InvalidParseOperationException(
                "[" + req.name() + ACTION_ARGUMENT + actionValue + COULD_NOT_CONVERTED + fieldName
            );
        }

        int numberOfPop = Math.abs(actionValue.asInt());

        if (numberOfPop == 0) {
            return;
        }

        if (numberOfPop > node.size()) {
            throw new InvalidParseOperationException(
                "Cannot pop " + numberOfPop + "items from the field '" + fieldName + "' because it has less items"
            );
        }
        if (actionValue.asInt() < 0) {
            for (int i = 0; i < numberOfPop; i++) {
                node.remove(0);
            }
        } else {
            for (int i = 0; i < numberOfPop; i++) {
                node.remove(node.size() - 1);
            }
        }
        updatedFields.add(fieldName);
    }

    private Number getNumberValue(final String actionName, final String fieldName)
        throws InvalidParseOperationException {
        JsonNode node = JsonHandler.getNodeByPath(updatedDocument, fieldName, false);
        if (node == null || !node.isNumber()) {
            String message =
                "This field '" +
                fieldName +
                "' is not a number, cannot do '" +
                actionName +
                "' action: " +
                node +
                " or unknow fieldName";
            LOGGER.error(message);
            throw new InvalidParseOperationException(message);
        }
        return node.numberValue();
    }

    private JsonNode getArrayValue(final String actionName, final String fieldName)
        throws InvalidParseOperationException {
        JsonNode node = JsonHandler.getNodeByPath(updatedDocument, fieldName, false);
        if (node == null || node instanceof NullNode) {
            LOGGER.info("Action '" + actionName + "' in item previously null '" + fieldName + "' or unknow");
            ObjectNode updatedDocumentAsObject = (ObjectNode) updatedDocument;
            updatedDocumentAsObject.set(fieldName, JsonHandler.createArrayNode());
            return updatedDocument.get(fieldName);
        }
        if (!node.isArray()) {
            String message = "This field '" + fieldName + "' is not an array, cannot do '" + actionName + "' action";
            LOGGER.error(message);
            throw new InvalidParseOperationException(message);
        }
        return node;
    }

    public Set<String> getUpdatedFields() {
        return updatedFields;
    }
}
