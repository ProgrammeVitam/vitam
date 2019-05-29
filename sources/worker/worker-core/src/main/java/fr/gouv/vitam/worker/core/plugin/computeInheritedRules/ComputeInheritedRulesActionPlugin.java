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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
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
import fr.gouv.vitam.common.model.StatusCode;
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
import fr.gouv.vitam.worker.core.plugin.computeInheritedRules.model.Rule;
import fr.gouv.vitam.worker.core.utils.PluginHelper;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

/**
 * ComputeInheritedRulesActionPlugin
 */
public class ComputeInheritedRulesActionPlugin extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ComputeInheritedRulesActionPlugin.class);

    private static final String WORKSPACE_SERVER_ERROR = "Workspace Server Error";
    private static final String PLUGIN_NAME = "COMPUTE_INHERITED_RULES_ACTION_PLUGIN";

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

        try {
            for(String unitId : workerParameters.getObjectNameList()) {
                SelectMultiQuery selectMultiple = new SelectMultiQuery();
                BooleanQuery query = new BooleanQuery(BuilderToken.QUERY.AND);
                query.add(new CompareQuery(BuilderToken.QUERY.EQ, VitamFieldsHelper.id(), unitId));
                selectMultiple.setQuery(query);

                JsonNode archiveWithInheritedRules = metaDataClientFactory.getClient().selectUnitsWithInheritedRules(selectMultiple.getFinalSelect())
                        .get("$results");

                Map<String, InheritedRule> inheritedRules = new HashMap<>();
                if (archiveWithInheritedRules.size() > 0) {
                    JsonNode ruleCategories = archiveWithInheritedRules.get(0).get("InheritedRules");
                    Iterator<Map.Entry<String, JsonNode>> ruleCatIterator = ruleCategories.fields();
                    while (ruleCatIterator.hasNext()) {
                        Map.Entry<String, JsonNode> ruleCategory = ruleCatIterator.next();
                        String category = ruleCategory.getKey();
                        for(JsonNode ruleJson : ruleCategory.getValue().get("Rules")) {
                            LocalDate endDate = LocalDate.parse(ruleJson.get("EndDate").asText(), formatter);
                            String ruleId = ruleJson.get("Rule").asText();
                            Rule rule = new Rule(ruleId, endDate);
                            if(inheritedRules.get(category) == null) {
                                inheritedRules.put(category, new InheritedRule(rule));
                            } else {
                                inheritedRules.get(category).addRule(rule);
                            }
                        }
                    }
                }
                indexUnit(unitId, inheritedRules);
            }
        } catch (InvalidCreateOperationException | MetaDataException e) {
            throw new ProcessingException(e);
        } catch (final InvalidParseOperationException e) {
            LOGGER.error("File couldnt be converted into json", e);
            return PluginHelper.buildBulkItemStatus(workerParameters, PLUGIN_NAME, StatusCode.KO);
        }

        return PluginHelper.buildBulkItemStatus(workerParameters, PLUGIN_NAME, StatusCode.OK);
    }

    private void indexUnit(String unitId, Map<String, InheritedRule> inheritedRules)
        throws ProcessingException {

        ComputedInheritedRules computedInheritedRules = new ComputedInheritedRules(inheritedRules);
        try (MetaDataClient client = this.metaDataClientFactory.getClient()) {

            UpdateMultiQuery updateMultiQuery = new UpdateMultiQuery();
            Map<String, JsonNode> action = new HashMap<>();
            action.put(VitamFieldsHelper.computedInheritedRules(), JsonHandler.toJsonNode(computedInheritedRules));
            SetAction setComputedInheritedRules = new SetAction(action);
            updateMultiQuery.addActions(setComputedInheritedRules);

            client.updateUnitbyId(updateMultiQuery.getFinalUpdateById(), unitId);

        } catch (InvalidParseOperationException | InvalidCreateOperationException | MetaDataExecutionException | MetaDataDocumentSizeException | MetaDataClientServerException | MetaDataNotFoundException e) {
            throw new ProcessingException("Could not index unit " + unitId, e);
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
