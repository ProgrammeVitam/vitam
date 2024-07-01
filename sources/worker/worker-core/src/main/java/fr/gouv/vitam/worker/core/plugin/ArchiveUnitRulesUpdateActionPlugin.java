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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.UpdateWorkflowConstants;
import fr.gouv.vitam.common.model.VitamAutoCloseable;
import fr.gouv.vitam.functional.administration.common.utils.ArchiveUnitUpdateUtils;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.common.utils.ArchiveUnitLifecycleUpdateUtils;
import fr.gouv.vitam.worker.core.handler.ActionHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ArchiveUnitRulesUpdateAction Plugin.<br>
 */
public class ArchiveUnitRulesUpdateActionPlugin extends ActionHandler implements VitamAutoCloseable {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ArchiveUnitRulesUpdateActionPlugin.class);

    private static final String UPDATE_UNIT_RULES_TASK_ID = "UPDATE_UNIT_RULES";
    private static final String RULE_TYPE = "RuleType";

    private static final String RESULTS = "$results";
    private static final String ARCHIVE_UNIT_NOT_FOUND = "Archive unit not found";
    private static final String MANAGEMENT_KEY = "#management";
    private static final String FIELDS_KEY = "$fields";
    private static final String RULES_KEY = "Rules";

    private final ArchiveUnitLifecycleUpdateUtils archiveUnitLifecycleUpdateUtils =
        new ArchiveUnitLifecycleUpdateUtils();

    private final MetaDataClientFactory metaDataClientFactory;

    public ArchiveUnitRulesUpdateActionPlugin() {
        this(MetaDataClientFactory.getInstance());
    }

    @VisibleForTesting
    public ArchiveUnitRulesUpdateActionPlugin(MetaDataClientFactory metaDataClientFactory) {
        this.metaDataClientFactory = metaDataClientFactory;
    }

    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO handler) {
        checkMandatoryParameters(params);
        LOGGER.debug("ArchiveUnitRulesUpdateActionPlugin running ...");

        final ItemStatus itemStatus = new ItemStatus(UPDATE_UNIT_RULES_TASK_ID);
        // Get ArchiveUnit information
        try (MetaDataClient metaDataClient = metaDataClientFactory.getClient()) {
            String archiveUnitId = params.getObjectName().split("\\.")[0];
            final SelectParserMultiple selectRequest = new SelectParserMultiple();
            ObjectNode projectionNode = JsonHandler.createObjectNode();
            ObjectNode objectNode = JsonHandler.createObjectNode();
            objectNode.put(MANAGEMENT_KEY, 1);
            projectionNode.set(FIELDS_KEY, objectNode);
            final SelectMultiQuery request = selectRequest.getRequest().reset().addProjection(projectionNode);
            JsonNode jsonResponse = metaDataClient.selectUnitbyId(request.getFinalSelect(), archiveUnitId);

            JsonNode archiveUnitNode = jsonResponse.get(RESULTS);
            // if result = 0 then throw Exception
            if (archiveUnitNode.size() == 0) {
                throw new MetaDataNotFoundException(ARCHIVE_UNIT_NOT_FOUND);
            }
            archiveUnitNode = archiveUnitNode.get(0);
            JsonNode managementNode = archiveUnitNode.get(MANAGEMENT_KEY);

            UpdateMultiQuery query = new UpdateMultiQuery();

            Map<String, List<JsonNode>> updatedRulesByType = new HashMap<>();

            final JsonNode rulesForAU = handler.getJsonFromWorkspace(
                UpdateWorkflowConstants.UNITS_FOLDER + "/" + params.getObjectName()
            );

            for (final JsonNode rule : rulesForAU) {
                if (!updatedRulesByType.containsKey(rule.get(RULE_TYPE).asText())) {
                    List<JsonNode> listRulesByType = new ArrayList<>();
                    listRulesByType.add(rule);
                    updatedRulesByType.put(rule.get(RULE_TYPE).asText(), listRulesByType);
                } else {
                    List<JsonNode> listRulesByType = updatedRulesByType.get(rule.get(RULE_TYPE).asText());
                    listRulesByType.add(rule);
                    updatedRulesByType.put(rule.get(RULE_TYPE).asText(), listRulesByType);
                }
            }
            int nbUpdates = 0;
            for (Map.Entry<String, List<JsonNode>> entry : updatedRulesByType.entrySet()) {
                JsonNode categoryNode = managementNode.get(entry.getKey());
                if (
                    categoryNode != null &&
                    categoryNode.get(RULES_KEY) != null &&
                    ArchiveUnitUpdateUtils.updateCategoryRules(
                        categoryNode.get(RULES_KEY),
                        entry.getValue(),
                        query,
                        entry.getKey()
                    )
                ) {
                    nbUpdates++;
                }
            }
            // if at least one action is set
            if (nbUpdates > 0) {
                query.addActions(UpdateActionHelper.push(VitamFieldsHelper.operations(), params.getContainerName()));
                JsonNode updateResultJson = metaDataClient.updateUnitById(query.getFinalUpdate(), archiveUnitId);
                String diffMessage = ArchiveUnitUpdateUtils.getDiffMessageFor(updateResultJson, archiveUnitId);
                archiveUnitLifecycleUpdateUtils.logLifecycle(
                    params,
                    archiveUnitId,
                    StatusCode.OK,
                    diffMessage,
                    handler.getLifecyclesClient()
                );
                archiveUnitLifecycleUpdateUtils.commitLifecycle(
                    params.getContainerName(),
                    archiveUnitId,
                    handler.getLifecyclesClient()
                );
            }
            itemStatus.increment(StatusCode.OK);
        } catch (ProcessingException e) {
            LOGGER.error("Exception while getting AU information : ", e);
            itemStatus.increment(StatusCode.FATAL);
        } catch (InvalidParseOperationException e) {
            LOGGER.error("Exception while parsing json : ", e);
            itemStatus.increment(StatusCode.KO);
        } catch (
            MetaDataExecutionException
            | MetaDataDocumentSizeException
            | MetaDataClientServerException
            | InvalidCreateOperationException e
        ) {
            LOGGER.error("Exception with metadata: ", e);
            itemStatus.increment(StatusCode.FATAL);
        } catch (MetaDataNotFoundException e) {
            LOGGER.error("Archive unit not found : ", e);
            itemStatus.increment(StatusCode.KO);
        }

        LOGGER.debug("ArchiveUnitRulesUpdateActionPlugin response: " + itemStatus.getGlobalStatus());
        return new ItemStatus(UPDATE_UNIT_RULES_TASK_ID).setItemsStatus(UPDATE_UNIT_RULES_TASK_ID, itemStatus);
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // Do not know...
    }
}
