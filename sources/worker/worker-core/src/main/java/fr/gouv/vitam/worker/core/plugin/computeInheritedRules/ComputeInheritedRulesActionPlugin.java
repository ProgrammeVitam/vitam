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
package fr.gouv.vitam.worker.core.plugin.computeInheritedRules;

import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildBulkItemStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap.SimpleEntry;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.BooleanQuery;
import fr.gouv.vitam.common.database.builder.query.CompareQuery;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.query.action.SetAction;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
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
import fr.gouv.vitam.worker.core.plugin.computeInheritedRules.model.ComputedInheritedRules;
import fr.gouv.vitam.worker.core.plugin.computeInheritedRules.model.InheritedRule;
import fr.gouv.vitam.worker.core.plugin.computeInheritedRules.model.Properties;
import fr.gouv.vitam.worker.core.plugin.computeInheritedRules.model.PropertyValue;
import fr.gouv.vitam.worker.core.plugin.computeInheritedRules.model.RuleMaxEndDate;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

/**
 * ComputeInheritedRulesActionPlugin
 */
public class ComputeInheritedRulesActionPlugin extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ComputeInheritedRulesActionPlugin.class);

    private static final String PLUGIN_NAME = "COMPUTE_INHERITED_RULES_ACTION_PLUGIN";
    static final String INHERITED_RULES = "InheritedRules";

    private final MetaDataClientFactory metaDataClientFactory;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");



    public ComputeInheritedRulesActionPlugin() {
        this(MetaDataClientFactory.getInstance());
    }

    @VisibleForTesting
    public ComputeInheritedRulesActionPlugin(MetaDataClientFactory metaDataClientFactory) {
        this.metaDataClientFactory = metaDataClientFactory;
    }

    /**
     * @param workerParameters
     * @param handler
     * @return
     * @throws ProcessingException
     * @throws ContentAddressableStorageServerException
     */
    @Override
    public List<ItemStatus> executeList(WorkerParameters workerParameters, HandlerIO handler)
        throws ProcessingException, ContentAddressableStorageServerException {
        LOGGER.debug("execute MaxEndDatePlugin");

        try (MetaDataClient metaDataClient = metaDataClientFactory.getClient()) {
            for (String unitId : workerParameters.getObjectNameList()) {

                SelectMultiQuery selectMultiple = new SelectMultiQuery();
                BooleanQuery query = new BooleanQuery(BuilderToken.QUERY.AND);
                //TODO bulk request + projection #id
                query.add(new CompareQuery(BuilderToken.QUERY.EQ, VitamFieldsHelper.id(), unitId));
                selectMultiple.setQuery(query);
                JsonNode response = metaDataClient.selectUnitsWithInheritedRules(selectMultiple.getFinalSelect());
                RequestResponseOK<JsonNode> requestResponseOK = RequestResponseOK.getFromJsonNode(response);
                List<JsonNode> archiveWithInheritedRules = requestResponseOK.getResults();
                int tenantId = VitamThreadUtils.getVitamSession().getTenantId();
                boolean indexAPIOutput = getConfigurationAPIOutPut(tenantId);
                boolean indexRulesById = getConfigurationRulesById(tenantId);

                if (archiveWithInheritedRules.size() > 0) {
                    UnitInheritedRulesResponseModel unitInheritedResponseModel = JsonHandler
                        .getFromJsonNode(archiveWithInheritedRules.get(0).get(INHERITED_RULES), UnitInheritedRulesResponseModel.class);
                    //TODO Switch POJO to metadata-common
                    Map<String, InheritedRuleCategoryResponseModel> rulesCategories = unitInheritedResponseModel.getRuleCategories();
                    List<InheritedPropertyResponseModel> globalInheritedProperties = unitInheritedResponseModel.getGlobalProperties();

                    Map<String, PropertyValue> globalProperties =
                        mapInheritedPropertyResponseModelToPropertiesNameValue(globalInheritedProperties);

                    Map<String, InheritedRule> inheritedRulesWithAllOption = rulesCategories
                        .entrySet()
                        .stream()
                        .flatMap(entry -> mapRulesCategoriesToCategoriesWithEndDateAndProperties(entry.getKey(), entry.getValue(),
                            indexRulesById))
                        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

                    //TODO if indexAPIOutput don't serialize unitInheritedResponseModel
                    indexUnit(unitId, inheritedRulesWithAllOption, metaDataClient, new Properties(globalProperties),
                        JsonHandler.toJsonNode(unitInheritedResponseModel), indexAPIOutput);

                }
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
        List<String> indexInheritedRulesWithRulesIdByTenant = VitamConfiguration.getIndexInheritedRulesWithRulesIdByTenant();
        if (indexInheritedRulesWithRulesIdByTenant != null && !indexInheritedRulesWithRulesIdByTenant.isEmpty()) {
            List<Integer> allowedTenantOptionRuleId =
                indexInheritedRulesWithRulesIdByTenant.stream().map(Integer::valueOf).collect(Collectors.toList());
            if (allowedTenantOptionRuleId.contains(tenant)) {
                return true;
            }
        }
        return false;
    }

    private boolean getConfigurationAPIOutPut(int tenant) {
        List<String> indexInheritedRulesWithAPIV2OutputByTenant = VitamConfiguration.getIndexInheritedRulesWithAPIV2OutputByTenant();
        if (indexInheritedRulesWithAPIV2OutputByTenant != null && !indexInheritedRulesWithAPIV2OutputByTenant.isEmpty()) {
            List<Integer> allowedTenantOptionsAPIV2Output = indexInheritedRulesWithAPIV2OutputByTenant.stream().map(Integer::valueOf).collect(
                Collectors.toList());
            if (allowedTenantOptionsAPIV2Output.contains(tenant)) {
                return true;
            }
        }
        return false;
    }

    private Stream<Entry<String, InheritedRule>> mapRulesCategoriesToCategoriesWithEndDateAndProperties(String category,
        InheritedRuleCategoryResponseModel categoryResponseModel, boolean indexRulesById) {
        Map<String, InheritedRule> inheritedRulesWithEndDateAndProperties = new HashMap<>();

        //TODO refactor because Lotfi don't want zoneId.systemDefault
        Optional<LocalDate> maxEndDateByCategory =
            categoryResponseModel.getRules().stream()
                .map(rule -> Date.from(ParseToLocalDate(rule.getEndDate()).atStartOfDay(ZoneId.systemDefault()).toInstant()))
                .max(Date::compareTo)
                .map(date -> date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());

        Map<String, RuleMaxEndDate> ruleIdToRuleMaxEndDate =
            categoryResponseModel.getRules()
                .stream()
                .map(this::mapToEntryOfRuleIdToMaxEndDate)
                .collect(
                    Collectors.toMap(Entry::getKey, Entry::getValue, (value1, value2) -> {
                        if (value1.getMaxEndDate().isBefore(value2.getMaxEndDate())) {
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

    private Entry<String, RuleMaxEndDate> mapToEntryOfRuleIdToMaxEndDate(InheritedRuleResponseModel rule) {
        return new SimpleEntry<>(rule.getRuleId(), new RuleMaxEndDate(ParseToLocalDate(rule.getEndDate())));
    }

    private void addToMapAccordingToIndexRulesById(String category, boolean indexRulesById,
        Map<String, InheritedRule> inheritedRulesWithEndDateAndProperties,
        LocalDate maxEndDateByCategory,
        Map<String, RuleMaxEndDate> computedInheritedRules,
        Map<String, PropertyValue> propertyNameToPropertyValue) {
        if (indexRulesById) {
            inheritedRulesWithEndDateAndProperties
                .put(category, new InheritedRule(maxEndDateByCategory, new Properties(propertyNameToPropertyValue), computedInheritedRules));
        } else {
            inheritedRulesWithEndDateAndProperties
                .put(category, new InheritedRule(maxEndDateByCategory, new Properties(propertyNameToPropertyValue)));
        }
    }

    private LocalDate ParseToLocalDate(String dateToParse) {
        return LocalDate.parse(dateToParse, formatter);
    }

    private Map<String, PropertyValue> mapInheritedPropertyResponseModelToPropertiesNameValue(List<InheritedPropertyResponseModel> properties) {
        return properties.stream()
            .map(property -> mapPropertyToPropertyNameAndValue(property.getPropertyName(), property.getPropertyValue()))
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    private SimpleEntry<String, PropertyValue> mapPropertyToPropertyNameAndValue(String propertyName, Object propertyValue) {
        return new SimpleEntry<>(propertyName, new PropertyValue(propertyValue));
    }

    private void indexUnit(String unitId, Map<String, InheritedRule> inheritedRules, MetaDataClient metaDataClient, Properties globalProperty,
        JsonNode inheritedRulesAPIOutput, boolean indexAPIOutput)
        throws ProcessingException {
        ComputedInheritedRules computedInheritedRules =
            getComputedInheritedRulesAccordingToIndexAPIOutput(inheritedRules, globalProperty, inheritedRulesAPIOutput, indexAPIOutput);
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

    private ComputedInheritedRules getComputedInheritedRulesAccordingToIndexAPIOutput(Map<String, InheritedRule> inheritedRules,
        Properties globalProperty, JsonNode inheritedRulesAPIOutput, boolean indexAPIOutput) {
        LocalDate indexationDate = Instant.now().atZone(ZoneId.systemDefault()).toLocalDate();
        String dateFormatted = indexationDate.format(formatter);
        if (indexAPIOutput) {
            return new ComputedInheritedRules(inheritedRules, globalProperty, inheritedRulesAPIOutput, dateFormatted);
        } else {
            return new ComputedInheritedRules(inheritedRules, globalProperty, dateFormatted);
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
