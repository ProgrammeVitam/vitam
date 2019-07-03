/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 *******************************************************************************/
package fr.gouv.vitam.worker.core.plugin.computeinheritedrules;

import com.fasterxml.jackson.databind.JsonNode;
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
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
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
import fr.gouv.vitam.worker.core.plugin.computeinheritedrules.model.StorageRule;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildBulkItemStatus;

/**
 * ComputeInheritedRulesActionPlugin
 */
public class ComputeInheritedRulesActionPlugin extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ComputeInheritedRulesActionPlugin.class);

    private static final String PLUGIN_NAME = "COMPUTE_INHERITED_RULES_ACTION_PLUGIN";
    private static final String INHERITED_RULES = "InheritedRules";

    private final MetaDataClientFactory metaDataClientFactory;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");



    public ComputeInheritedRulesActionPlugin() {
        this(MetaDataClientFactory.getInstance());
    }

    @VisibleForTesting
    ComputeInheritedRulesActionPlugin(MetaDataClientFactory metaDataClientFactory) {
        this.metaDataClientFactory = metaDataClientFactory;
    }

    /**
     * @param workerParameters
     * @param handler
     * @return
     * @throws ProcessingException
     */
    @Override
    public List<ItemStatus> executeList(WorkerParameters workerParameters, HandlerIO handler)
        throws ProcessingException {
        LOGGER.debug("execute MaxEndDatePlugin");

        try (MetaDataClient metaDataClient = metaDataClientFactory.getClient()) {
            SelectMultiQuery select = new SelectMultiQuery();
            InQuery query =
                QueryHelper.in(VitamFieldsHelper.id(), workerParameters.getObjectNameList().toArray(new String[0]));
            select.setQuery(query);
            JsonNode response = metaDataClient.selectUnitsWithInheritedRules(select.getFinalSelect());
            RequestResponseOK<JsonNode> requestResponseOK = RequestResponseOK.getFromJsonNode(response);
            List<JsonNode> archiveWithInheritedRules = requestResponseOK.getResults();

            int tenantId = VitamThreadUtils.getVitamSession().getTenantId();
            boolean indexAPIOutput = getConfigurationAPIOutPut(tenantId);
            boolean indexRulesById = getConfigurationRulesById(tenantId);

            for (JsonNode archiveUnit : archiveWithInheritedRules) {
                UnitInheritedRulesResponseModel unitInheritedResponseModel = JsonHandler
                    .getFromJsonNode(archiveUnit.get(INHERITED_RULES), UnitInheritedRulesResponseModel.class);
                String unitId = archiveUnit.get(VitamFieldsHelper.id()).textValue();

                //TODO Switch POJO to metadata-common
                Map<String, InheritedRuleCategoryResponseModel> rulesCategories =
                    unitInheritedResponseModel.getRuleCategories();

                Map<String, InheritedRule> inheritedRulesWithAllOption = rulesCategories
                    .entrySet()
                    .stream()
                    .flatMap(entry -> mapRulesCategoriesToCategoriesWithEndDateAndProperties(entry.getKey(),
                        entry.getValue(),
                        indexRulesById))
                    .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

                List<InheritedPropertyResponseModel> globalProperties =
                    unitInheritedResponseModel.getGlobalProperties();
                Map<String, Object> globalInheritedProperties = globalProperties.stream()
                    .map(property -> new SimpleEntry<>(property.getPropertyName(), property.getPropertyValue()))
                    .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

                JsonNode inheritedRulesAPIOutput =
                    (indexAPIOutput) ? JsonHandler.toJsonNode(unitInheritedResponseModel) : null;
                indexUnit(unitId, inheritedRulesWithAllOption, globalInheritedProperties, metaDataClient,
                    inheritedRulesAPIOutput, indexAPIOutput);
            }
        } catch (InvalidCreateOperationException | MetaDataException e) {
            throw new ProcessingException(e);
        } catch (final InvalidParseOperationException e) {
            LOGGER.error("File couldnt be converted into json", e);
            return buildBulkItemStatus(workerParameters, PLUGIN_NAME, StatusCode.KO);
        }

        return buildBulkItemStatus(workerParameters, PLUGIN_NAME, StatusCode.OK);
    }

    private boolean getConfigurationRulesById(int tenant) {
        List<String> indexInheritedRulesWithRulesIdByTenant =
            VitamConfiguration.getIndexInheritedRulesWithRulesIdByTenant();
        if (indexInheritedRulesWithRulesIdByTenant != null && !indexInheritedRulesWithRulesIdByTenant.isEmpty()) {
            List<Integer> allowedTenantOptionRuleId =
                indexInheritedRulesWithRulesIdByTenant
                    .stream()
                    .map(Integer::valueOf)
                    .filter(i -> i == tenant)
                    .collect(Collectors.toList());
            return !allowedTenantOptionRuleId.isEmpty();
        }
        return false;
    }

    private boolean getConfigurationAPIOutPut(int tenant) {
        List<String> indexInheritedRulesWithAPIV2OutputByTenant =
            VitamConfiguration.getIndexInheritedRulesWithAPIV2OutputByTenant();
        if (indexInheritedRulesWithAPIV2OutputByTenant != null &&
            !indexInheritedRulesWithAPIV2OutputByTenant.isEmpty()) {
            List<Integer> allowedTenantOptionsAPIV2Output =
                indexInheritedRulesWithAPIV2OutputByTenant.stream().map(Integer::valueOf).collect(
                    Collectors.toList());
            return allowedTenantOptionsAPIV2Output.contains(tenant);
        }
        return false;
    }

    private Stream<Entry<String, InheritedRule>> mapRulesCategoriesToCategoriesWithEndDateAndProperties(String category,
        InheritedRuleCategoryResponseModel categoryResponseModel, boolean indexRulesById) {
        Map<String, InheritedRule> inheritedRulesWithEndDateAndProperties = new HashMap<>();

        Optional<LocalDate> maxEndDateByCategory =
            categoryResponseModel.getRules().stream()
                .map(rule -> parseToLocalDate(rule.getEndDate()))
                .max(LocalDate::compareTo);

        Map<String, LocalDate> ruleIdToRuleMaxEndDate =
            categoryResponseModel.getRules()
                .stream()
                .map(this::mapToEntryOfRuleIdToMaxEndDate)
                .collect(
                    Collectors.toMap(Entry::getKey, Entry::getValue, (value1, value2) -> {
                        if (value1.isBefore(value2)) {
                            return value2;
                        }
                        return value1;
                    }));

        Map<String, PropertyValue> propertyNameToPropertyValue =
            mapInheritedPropertyResponseModelToPropertiesNameValue(categoryResponseModel.getProperties());

        LocalDate maxEndDate = null;
        //TODO get or else null Warning to MaxEndDate null Functional
        if (maxEndDateByCategory.isPresent()) {
            maxEndDate = maxEndDateByCategory.get();
        }

        addToMapAccordingToIndexRulesById(category, indexRulesById, inheritedRulesWithEndDateAndProperties,
            maxEndDate,
            ruleIdToRuleMaxEndDate, propertyNameToPropertyValue);
        return inheritedRulesWithEndDateAndProperties.entrySet().stream();
    }

    private Entry<String, LocalDate> mapToEntryOfRuleIdToMaxEndDate(InheritedRuleResponseModel rule) {
        return new SimpleEntry<>(rule.getRuleId(), parseToLocalDate(rule.getEndDate()));
    }

    private void addToMapAccordingToIndexRulesById(String category, boolean indexRulesById,
        Map<String, InheritedRule> inheritedRulesWithEndDateAndProperties,
        LocalDate maxEndDateByCategory,
        Map<String, LocalDate> computedInheritedRules,
        Map<String, PropertyValue> propertyNameToPropertyValue) {
        InheritedRule rule;
        switch(category) {
            case ComputedInheritedRules.CLASSIFICATION_RULE:
                rule = new ClassificationRule(maxEndDateByCategory,
                    new Properties(propertyNameToPropertyValue),
                    (indexRulesById) ? computedInheritedRules:null);
                break;
            case ComputedInheritedRules.STORAGE_RULE:
                rule = new StorageRule(maxEndDateByCategory,
                    new Properties(propertyNameToPropertyValue),
                    (indexRulesById) ? computedInheritedRules:null);
                break;
            case ComputedInheritedRules.APPRAISAL_RULE:
                rule = new AppraisalRule(maxEndDateByCategory,
                    new Properties(propertyNameToPropertyValue),
                    (indexRulesById) ? computedInheritedRules:null);
                break;
            default:
                rule = new InheritedRule(maxEndDateByCategory,
                    (indexRulesById) ? computedInheritedRules:null);
                break;

        }

        inheritedRulesWithEndDateAndProperties.put(category, rule);

    }


    private LocalDate parseToLocalDate(String dateToParse) {
        return LocalDate.parse(dateToParse, formatter);
    }

    private Map<String, PropertyValue> mapInheritedPropertyResponseModelToPropertiesNameValue(
        List<InheritedPropertyResponseModel> properties) {
        return properties.stream()
            .map(property -> mapPropertyToPropertyNameAndValue(property.getPropertyName(), property.getPropertyValue()))
            .collect(Collectors.toMap(
                Entry::getKey,
                Entry::getValue,
                PropertyValue::new
            ));
    }

    private SimpleEntry<String, PropertyValue> mapPropertyToPropertyNameAndValue(String propertyName,
        Object propertyValue) {
        return new SimpleEntry<>(propertyName, new PropertyValue(propertyValue));
    }

    private void indexUnit(String unitId, Map<String, InheritedRule> inheritedRules,
        Map<String, Object> globalInheritedProperties, MetaDataClient metaDataClient,
        JsonNode inheritedRulesAPIOutput, boolean indexAPIOutput)
        throws ProcessingException {
        ComputedInheritedRules computedInheritedRules =
            getComputedInheritedRulesAccordingToIndexAPIOutput(inheritedRules, inheritedRulesAPIOutput,
                globalInheritedProperties, indexAPIOutput);
        try {
            UpdateMultiQuery updateMultiQuery = new UpdateMultiQuery();
            Map<String, JsonNode> action = new HashMap<>();
            action.put(VitamFieldsHelper.computedInheritedRules(), JsonHandler.toJsonNode(computedInheritedRules));
            SetAction setComputedInheritedRules = new SetAction(action);
            updateMultiQuery.addActions(setComputedInheritedRules);

            metaDataClient.updateUnitById(updateMultiQuery.getFinalUpdateById(), unitId);

        } catch (InvalidParseOperationException | InvalidCreateOperationException | MetaDataExecutionException | MetaDataDocumentSizeException | MetaDataClientServerException | MetaDataNotFoundException e) {
            throw new ProcessingException("Could not index unit " + unitId, e);
        }
    }

    private ComputedInheritedRules getComputedInheritedRulesAccordingToIndexAPIOutput(
        Map<String, InheritedRule> inheritedRules,
        JsonNode inheritedRulesAPIOutput, Map<String, Object> globalInheritedProperties, boolean indexAPIOutput) {
        LocalDate indexationDate = Instant.now().atZone(ZoneId.systemDefault()).toLocalDate();
        String dateFormatted = indexationDate.format(formatter);
        if (indexAPIOutput) {
            return new ComputedInheritedRules(inheritedRules, inheritedRulesAPIOutput, globalInheritedProperties,
                dateFormatted);
        } else {
            return new ComputedInheritedRules(inheritedRules, dateFormatted);
        }
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) {
        throw new VitamRuntimeException("Not implemented");
    }

    public static String getId() {
        return PLUGIN_NAME;
    }

}
