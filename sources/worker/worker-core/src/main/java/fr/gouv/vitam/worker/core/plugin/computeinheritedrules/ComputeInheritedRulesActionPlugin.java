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
package fr.gouv.vitam.worker.core.plugin.computeinheritedrules;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.InQuery;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.query.action.SetAction;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.rules.InheritedPropertyResponseModel;
import fr.gouv.vitam.common.model.rules.InheritedRuleCategoryResponseModel;
import fr.gouv.vitam.common.model.rules.InheritedRuleResponseModel;
import fr.gouv.vitam.common.model.rules.UnitInheritedRulesResponseModel;
import fr.gouv.vitam.common.model.unit.ComputedInheritedRuleModel;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.metadata.api.exception.MetaDataException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.plugin.computeinheritedrules.model.AppraisalRule;
import fr.gouv.vitam.worker.core.plugin.computeinheritedrules.model.ClassificationRule;
import fr.gouv.vitam.worker.core.plugin.computeinheritedrules.model.ComputedInheritedRules;
import fr.gouv.vitam.worker.core.plugin.computeinheritedrules.model.InheritedRule;
import fr.gouv.vitam.worker.core.plugin.computeinheritedrules.model.Properties;
import fr.gouv.vitam.worker.core.plugin.computeinheritedrules.model.PropertyValue;
import fr.gouv.vitam.worker.core.plugin.computeinheritedrules.model.RuleCategoryInheritanceOrigin;
import fr.gouv.vitam.worker.core.plugin.computeinheritedrules.model.StorageRule;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

import static fr.gouv.vitam.worker.core.plugin.computeinheritedrules.model.ComputedInheritedRules.ACCESS_RULE;
import static fr.gouv.vitam.worker.core.plugin.computeinheritedrules.model.ComputedInheritedRules.APPRAISAL_RULE;
import static fr.gouv.vitam.worker.core.plugin.computeinheritedrules.model.ComputedInheritedRules.CLASSIFICATION_RULE;
import static fr.gouv.vitam.worker.core.plugin.computeinheritedrules.model.ComputedInheritedRules.DISSEMINATION_RULE;
import static fr.gouv.vitam.worker.core.plugin.computeinheritedrules.model.ComputedInheritedRules.HOLD_RULE;
import static fr.gouv.vitam.worker.core.plugin.computeinheritedrules.model.ComputedInheritedRules.REUSE_RULE;
import static fr.gouv.vitam.worker.core.plugin.computeinheritedrules.model.ComputedInheritedRules.STORAGE_RULE;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildBulkItemStatus;
import static java.util.function.Predicate.not;

public class ComputeInheritedRulesActionPlugin extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ComputeInheritedRulesActionPlugin.class);

    private static final String PLUGIN_NAME = "COMPUTE_INHERITED_RULES_ACTION_PLUGIN";
    private static final String INHERITED_RULES = "InheritedRules";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final BinaryOperator<LocalDate> DATE_MERGER = (date, date2) -> {
        if (date.isBefore(date2)) {
            return date2;
        }

        return date;
    };
    private static final BinaryOperator<Object> PROPERTY_MERGER = (property, property2) -> {
        List<Object> list = new ArrayList<>();
        if (property instanceof Collection<?>) {
            list.addAll((Collection) property);
        } else {
            list.add(property);
        }

        list.add(property2);
        return list;
    };

    private final MetaDataClientFactory metaDataClientFactory;

    public ComputeInheritedRulesActionPlugin() {
        this(MetaDataClientFactory.getInstance());
    }

    @VisibleForTesting
    ComputeInheritedRulesActionPlugin(MetaDataClientFactory metaDataClientFactory) {
        this.metaDataClientFactory = metaDataClientFactory;
    }

    @Override
    public List<ItemStatus> executeList(WorkerParameters workerParameters, HandlerIO handler)
        throws ProcessingException {
        LOGGER.debug("execute ComputeInheritedRulesActionPlugin");

        try (MetaDataClient metaDataClient = metaDataClientFactory.getClient()) {
            SelectMultiQuery select = new SelectMultiQuery();
            InQuery query = QueryHelper.in(
                VitamFieldsHelper.id(),
                workerParameters.getObjectNameList().toArray(new String[0])
            );
            select.setQuery(query);

            JsonNode response = metaDataClient.selectUnitsWithInheritedRules(select.getFinalSelect());

            RequestResponseOK<JsonNode> requestResponseOK = RequestResponseOK.getFromJsonNode(response);
            List<JsonNode> archiveWithInheritedRules = requestResponseOK.getResults();

            String tenantId = VitamThreadUtils.getVitamSession().getTenantId().toString();
            boolean isApiIndexable = isIndexable(
                tenantId,
                VitamConfiguration.getIndexInheritedRulesWithAPIV2OutputByTenant()
            );
            boolean isRuleIndexable = isIndexable(
                tenantId,
                VitamConfiguration.getIndexInheritedRulesWithRulesIdByTenant()
            );

            for (JsonNode archiveUnit : archiveWithInheritedRules) {
                UnitInheritedRulesResponseModel unitInheritedResponseModel = JsonHandler.getFromJsonNode(
                    archiveUnit.get(INHERITED_RULES),
                    UnitInheritedRulesResponseModel.class
                );
                String unitId = archiveUnit.get(VitamFieldsHelper.id()).textValue();

                //TODO Switch POJO to metadata-common
                Map<String, InheritedRule> inheritedRulesWithAllOption = unitInheritedResponseModel
                    .getRuleCategories()
                    .entrySet()
                    .stream()
                    .map(
                        entry ->
                            mapToCategoriesWithEndDateAndProperties(entry.getKey(), entry.getValue(), isRuleIndexable)
                    )
                    .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

                Map<String, Object> globalInheritedProperties = unitInheritedResponseModel
                    .getGlobalProperties()
                    .stream()
                    .collect(
                        Collectors.toMap(
                            InheritedPropertyResponseModel::getPropertyName,
                            InheritedPropertyResponseModel::getPropertyValue,
                            PROPERTY_MERGER
                        )
                    );

                JsonNode inheritedRulesAPIOutput = isApiIndexable
                    ? JsonHandler.toJsonNode(unitInheritedResponseModel)
                    : null;

                ComputedInheritedRules computedInheritedRules = getComputedInheritedRules(
                    inheritedRulesWithAllOption,
                    inheritedRulesAPIOutput,
                    globalInheritedProperties,
                    isApiIndexable
                );

                ObjectNode updateMultiQuery = getUpdateQuery(computedInheritedRules);
                metaDataClient.updateUnitById(updateMultiQuery, unitId);
            }

            return buildBulkItemStatus(workerParameters, PLUGIN_NAME, StatusCode.OK);
        } catch (InvalidCreateOperationException | MetaDataException e) {
            throw new ProcessingException(e);
        } catch (final InvalidParseOperationException e) {
            LOGGER.error("File couldn't be converted into json", e);
            return buildBulkItemStatus(workerParameters, PLUGIN_NAME, StatusCode.KO);
        }
    }

    private ObjectNode getUpdateQuery(ComputedInheritedRules computedInheritedRules)
        throws InvalidParseOperationException, InvalidCreateOperationException {
        Map<String, JsonNode> action = new HashMap<>();
        action.put(VitamFieldsHelper.computedInheritedRules(), JsonHandler.toJsonNode(computedInheritedRules));
        action.put(VitamFieldsHelper.validComputedInheritedRules(), JsonHandler.toJsonNode(true));

        SetAction setComputedInheritedRules = new SetAction(action);

        UpdateMultiQuery updateMultiQuery = new UpdateMultiQuery();
        updateMultiQuery.addActions(setComputedInheritedRules);
        return updateMultiQuery.getFinalUpdateById();
    }

    private boolean isIndexable(String tenant, List<String> indexByTenants) {
        if (indexByTenants == null || indexByTenants.isEmpty()) {
            return false;
        }

        return indexByTenants.contains(tenant);
    }

    private Entry<String, InheritedRule> mapToCategoriesWithEndDateAndProperties(
        String category,
        InheritedRuleCategoryResponseModel categoryResponseModel,
        boolean isRuleIndexable
    ) {
        LocalDate maxEndDate = categoryResponseModel
            .getRules()
            .stream()
            .filter(rule -> rule.getEndDate() != null)
            .map(rule -> parseToLocalDate(rule.getEndDate()))
            .max(LocalDate::compareTo)
            .orElse(null);

        Map<String, LocalDate> ruleIdToRuleMaxEndDate = categoryResponseModel
            .getRules()
            .stream()
            .filter(rule -> rule.getEndDate() != null)
            .collect(
                Collectors.toMap(
                    InheritedRuleResponseModel::getRuleId,
                    rule -> parseToLocalDate(rule.getEndDate()),
                    DATE_MERGER
                )
            );

        Map<String, PropertyValue> propertyNameToPropertyValue = mapInheritedPropertyResponseModelToPropertiesNameValue(
            categoryResponseModel.getProperties()
        );

        Set<String> inheritedRuleIds = categoryResponseModel
            .getRules()
            .stream()
            .filter(not(this::isRuleLocallyDefined))
            .map(InheritedRuleResponseModel::getRuleId)
            .collect(Collectors.toSet());

        boolean hasLocalRules = categoryResponseModel.getRules().stream().anyMatch(this::isRuleLocallyDefined);
        boolean hasInheritedRules = !inheritedRuleIds.isEmpty();

        RuleCategoryInheritanceOrigin ruleCategoryInheritanceOrigin;
        if (hasLocalRules) {
            if (hasInheritedRules) {
                ruleCategoryInheritanceOrigin = RuleCategoryInheritanceOrigin.LOCAL_AND_INHERITED;
            } else {
                ruleCategoryInheritanceOrigin = RuleCategoryInheritanceOrigin.LOCAL;
            }
        } else {
            if (hasInheritedRules) {
                ruleCategoryInheritanceOrigin = RuleCategoryInheritanceOrigin.INHERITED;
            } else {
                ruleCategoryInheritanceOrigin = null;
            }
        }

        return getInheritedRuleByCategory(
            category,
            isRuleIndexable,
            maxEndDate,
            ruleIdToRuleMaxEndDate,
            propertyNameToPropertyValue,
            ruleCategoryInheritanceOrigin,
            inheritedRuleIds
        );
    }

    private boolean isRuleLocallyDefined(InheritedRuleResponseModel rule) {
        // Locally defined rules have a single path containing only the unitId. i.e. "paths": [["currentUnitGuid"]]
        return rule.getPaths().size() == 1 && rule.getPaths().get(0).size() == 1;
    }

    private Entry<String, InheritedRule> getInheritedRuleByCategory(
        String category,
        boolean isRuleIndexable,
        LocalDate maxEndDateByCategory,
        Map<String, LocalDate> computedInheritedRules,
        Map<String, PropertyValue> propertyNameToPropertyValue,
        RuleCategoryInheritanceOrigin ruleCategoryInheritanceOrigin,
        Set<String> inheritedRuleIds
    ) {
        Properties properties = new Properties(propertyNameToPropertyValue);
        Map<String, LocalDate> ruleIdToRule = isRuleIndexable ? computedInheritedRules : null;

        List<ComputedInheritedRuleModel> rules = ruleIdToRule == null
            ? Collections.emptyList()
            : ruleIdToRule
                .entrySet()
                .stream()
                .map(entry -> new ComputedInheritedRuleModel(entry.getKey(), entry.getValue().toString()))
                .collect(Collectors.toList());

        switch (category) {
            case CLASSIFICATION_RULE:
                return new SimpleImmutableEntry<>(
                    category,
                    new ClassificationRule(
                        maxEndDateByCategory,
                        properties,
                        ruleIdToRule,
                        rules,
                        ruleCategoryInheritanceOrigin,
                        inheritedRuleIds
                    )
                );
            case STORAGE_RULE:
                return new SimpleImmutableEntry<>(
                    category,
                    new StorageRule(
                        maxEndDateByCategory,
                        properties,
                        ruleIdToRule,
                        rules,
                        ruleCategoryInheritanceOrigin,
                        inheritedRuleIds
                    )
                );
            case APPRAISAL_RULE:
                return new SimpleImmutableEntry<>(
                    category,
                    new AppraisalRule(
                        maxEndDateByCategory,
                        properties,
                        ruleIdToRule,
                        rules,
                        ruleCategoryInheritanceOrigin,
                        inheritedRuleIds
                    )
                );
            case DISSEMINATION_RULE:
            case ACCESS_RULE:
            case REUSE_RULE:
            case HOLD_RULE:
                return new SimpleImmutableEntry<>(
                    category,
                    new InheritedRule(
                        maxEndDateByCategory,
                        ruleIdToRule,
                        rules,
                        ruleCategoryInheritanceOrigin,
                        inheritedRuleIds
                    )
                );
            default:
                throw new VitamRuntimeException(String.format("Category rule cannot be of type '%s'.", category));
        }
    }

    private LocalDate parseToLocalDate(String dateToParse) {
        if (dateToParse == null) {
            return null;
        }

        return LocalDate.parse(dateToParse, DATE_TIME_FORMATTER);
    }

    private Map<String, PropertyValue> mapInheritedPropertyResponseModelToPropertiesNameValue(
        List<InheritedPropertyResponseModel> properties
    ) {
        return properties
            .stream()
            .collect(
                Collectors.toMap(
                    InheritedPropertyResponseModel::getPropertyName,
                    property -> new PropertyValue(property.getPropertyValue()),
                    PropertyValue::new
                )
            );
    }

    private ComputedInheritedRules getComputedInheritedRules(
        Map<String, InheritedRule> inheritedRules,
        JsonNode inheritedRulesAPIOutput,
        Map<String, Object> globalInheritedProperties,
        boolean isApiIndexable
    ) {
        String dateFormatted = Instant.now().atZone(ZoneId.systemDefault()).toLocalDate().format(DATE_TIME_FORMATTER);

        if (isApiIndexable) {
            return new ComputedInheritedRules(
                inheritedRules,
                inheritedRulesAPIOutput,
                globalInheritedProperties,
                dateFormatted
            );
        }
        return new ComputedInheritedRules(inheritedRules, dateFormatted, globalInheritedProperties);
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) {
        throw new VitamRuntimeException("Not implemented");
    }

    public static String getId() {
        return PLUGIN_NAME;
    }
}
