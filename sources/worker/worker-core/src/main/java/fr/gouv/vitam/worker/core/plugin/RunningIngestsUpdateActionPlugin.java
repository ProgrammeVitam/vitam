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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamDBException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.UpdateWorkflowConstants;
import fr.gouv.vitam.functional.administration.common.utils.ArchiveUnitUpdateUtils;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.common.utils.ArchiveUnitLifecycleUpdateUtils;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;

/**
 * CheckArchiveUnitSchema Plugin.<br>
 */

public class RunningIngestsUpdateActionPlugin extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(RunningIngestsUpdateActionPlugin.class);

    private static final String RUNNING_INGESTS_UPDATE_TASK_ID = "UPDATE_RUNNING_INGESTS";
    private static final int RANK_RUNNING_INGESTS_FILE = 0;
    private final Map<String, List<JsonNode>> updatedRulesByType = new HashMap<>();
    private final ProcessingManagementClientFactory processingManagementClientFactory;
    private final StoreMetaDataUnitActionPlugin storeMetaDataUnitActionPlugin;
    private final MetaDataClientFactory metaDataClientFactory;
    private final ArchiveUnitLifecycleUpdateUtils archiveUnitLifecycleUpdateUtils =
        new ArchiveUnitLifecycleUpdateUtils();

    private static final String RESULTS = "$results";
    private static final String ID = "#id";

    private static final String MANAGEMENT_KEY = "#management";
    private static final String FIELDS_KEY = "$fields";
    private static final String RULES_KEY = "Rules";

    private static final long SLEEP_TIME = 10000L;
    private static final long NB_TRY = 600; // equivalent to 60 minutes

    private static final String PROCESS_ID_FIELD = "operationId";

    /**
     * Empty constructor UnitsRulesComputePlugin
     */
    public RunningIngestsUpdateActionPlugin() {
        this.storeMetaDataUnitActionPlugin = new StoreMetaDataUnitActionPlugin();
        this.processingManagementClientFactory = ProcessingManagementClientFactory.getInstance();
        this.metaDataClientFactory = MetaDataClientFactory.getInstance();
    }

    @VisibleForTesting
    public RunningIngestsUpdateActionPlugin(
        ProcessingManagementClientFactory processingManagementClientFactory,
        MetaDataClientFactory metaDataClientFactory,
        StoreMetaDataUnitActionPlugin storeMetaDataUnitActionPlugin
    ) {
        this.processingManagementClientFactory = processingManagementClientFactory;
        this.metaDataClientFactory = metaDataClientFactory;
        this.storeMetaDataUnitActionPlugin = storeMetaDataUnitActionPlugin;
    }

    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO handler) {
        final ItemStatus itemStatus = new ItemStatus(RUNNING_INGESTS_UPDATE_TASK_ID);
        try {
            try {
                getRunningIngests(params, handler);
            } catch (VitamDBException e) {
                LOGGER.error(e);
            }
            itemStatus.increment(StatusCode.OK);
        } catch (ProcessingException e) {
            LOGGER.error("Processing exception", e);
            itemStatus.increment(StatusCode.FATAL);
        } catch (InvalidParseOperationException e) {
            LOGGER.error("Cannot parse json", e);
            itemStatus.increment(StatusCode.KO);
        }
        return new ItemStatus(RUNNING_INGESTS_UPDATE_TASK_ID).setItemsStatus(
            RUNNING_INGESTS_UPDATE_TASK_ID,
            itemStatus
        );
    }

    private void getRunningIngests(WorkerParameters params, HandlerIO handler)
        throws ProcessingException, InvalidParseOperationException, VitamDBException {
        InputStream inputStream = null;
        try {
            inputStream = handler.getInputStreamFromWorkspace(
                UpdateWorkflowConstants.PROCESSING_FOLDER + "/" + UpdateWorkflowConstants.UPDATED_RULES_JSON
            );
            JsonNode rulesUpdated = JsonHandler.getFromInputStream(inputStream);
            for (final JsonNode rule : rulesUpdated) {
                if (!updatedRulesByType.containsKey(rule.get("RuleType").asText())) {
                    List<JsonNode> listRulesByType = new ArrayList<>();
                    listRulesByType.add(rule);
                    updatedRulesByType.put(rule.get("RuleType").asText(), listRulesByType);
                } else {
                    List<JsonNode> listRulesByType = updatedRulesByType.get(rule.get("RuleType").asText());
                    listRulesByType.add(rule);
                    updatedRulesByType.put(rule.get("RuleType").asText(), listRulesByType);
                }
            }
            // do something with this file
            if (rulesUpdated.isArray() && rulesUpdated.size() > 0) {
                JsonNode runningIngests = JsonHandler.getFromFile((File) handler.getInput(RANK_RUNNING_INGESTS_FILE));
                if (runningIngests.isArray() && runningIngests.size() > 0) {
                    List<JsonNode> listIngest = JsonHandler.toArrayList((ArrayNode) runningIngests);
                    long nbTry = 0;
                    while (true) {
                        for (Iterator<JsonNode> it = listIngest.iterator(); it.hasNext();) {
                            JsonNode currentIngest = it.next();
                            checkAndProcessIngest(currentIngest, it, params, handler);
                        }
                        if (listIngest.size() == 0) {
                            break;
                        } else {
                            try {
                                Thread.sleep(SLEEP_TIME);
                            } catch (InterruptedException e) {
                                SysErrLogger.FAKE_LOGGER.ignoreLog(e);
                            }
                            if (nbTry == NB_TRY) {
                                break;
                            }
                            nbTry++;
                        }
                    }
                }
            } else {
                LOGGER.warn("No rules updated");
            }
        } catch (
            ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException | IOException e
        ) {
            LOGGER.error("Workspace error: Cannot get file", e);
            throw new ProcessingException(e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    private void checkAndProcessIngest(
        JsonNode currentIngest,
        Iterator<JsonNode> iterator,
        WorkerParameters params,
        HandlerIO handlerIO
    ) throws ProcessingException, VitamDBException {
        String operationId = currentIngest.get(PROCESS_ID_FIELD).asText();

        final WorkerParameters paramsCopy = WorkerParametersFactory.newWorkerParameters();
        paramsCopy.setContainerName(params.getContainerName());
        paramsCopy.setCurrentStep(params.getCurrentStep());
        paramsCopy.setUrlWorkspace(params.getUrlWorkspace());
        paramsCopy.setUrlMetadata(params.getUrlMetadata());
        paramsCopy.setObjectNameList(new ArrayList<>());
        try (
            MetaDataClient metaDataClient = metaDataClientFactory.getClient();
            ProcessingManagementClient processingManagementClient = processingManagementClientFactory.getClient()
        ) {
            ItemStatus status = processingManagementClient.getOperationProcessStatus(operationId);
            if (ProcessState.COMPLETED.equals(status.getGlobalState())) {
                // Treat only OK or WARNING ingests else remove from list and return
                if (status.getGlobalStatus().isGreaterOrEqualToKo()) {
                    iterator.remove();
                    return;
                }

                ObjectNode projectionNode = JsonHandler.createObjectNode();
                final SelectMultiQuery selectMultiple = new SelectMultiQuery();
                ObjectNode objectNode = JsonHandler.createObjectNode();
                objectNode.put(ID, 1);
                objectNode.put(MANAGEMENT_KEY, 1);
                projectionNode.set(FIELDS_KEY, objectNode);
                ArrayNode arrayNode = JsonHandler.createArrayNode();
                selectMultiple.setQuery(
                    QueryHelper.and().add(QueryHelper.in(VitamFieldsHelper.operations(), operationId))
                );
                selectMultiple.addRoots(arrayNode);
                selectMultiple.addProjection(projectionNode);
                final JsonNode unitsResultNode = metaDataClient.selectUnits(selectMultiple.getFinalSelect());
                if (unitsResultNode != null) {
                    ArrayNode resultUnitsArray = (ArrayNode) unitsResultNode.get(RESULTS);
                    // if size > 0, that means AU are to be checked updated
                    if (resultUnitsArray != null && resultUnitsArray.size() > 0) {
                        for (final JsonNode unitNode : resultUnitsArray) {
                            UpdateMultiQuery query = new UpdateMultiQuery();
                            String auGuid = unitNode.get(ID).asText();
                            JsonNode managementNode = unitNode.get(MANAGEMENT_KEY);
                            int nbUpdates = 0;
                            for (String key : updatedRulesByType.keySet()) {
                                JsonNode categoryNode = managementNode.get(key);
                                if (categoryNode != null && categoryNode.get(RULES_KEY) != null) {
                                    if (
                                        ArchiveUnitUpdateUtils.updateCategoryRules(
                                            categoryNode.get(RULES_KEY),
                                            updatedRulesByType.get(key),
                                            query,
                                            key
                                        )
                                    ) {
                                        nbUpdates++;
                                    }
                                }
                            }
                            if (nbUpdates > 0) {
                                try {
                                    query.addActions(
                                        UpdateActionHelper.push(
                                            VitamFieldsHelper.operations(),
                                            params.getContainerName()
                                        )
                                    );
                                    JsonNode updateResultJson = metaDataClient.updateUnitById(
                                        query.getFinalUpdate(),
                                        auGuid
                                    );
                                    archiveUnitLifecycleUpdateUtils.logLifecycle(
                                        params,
                                        auGuid,
                                        StatusCode.OK,
                                        ArchiveUnitUpdateUtils.getDiffMessageFor(updateResultJson, auGuid),
                                        handlerIO.getLifecyclesClient()
                                    );
                                    archiveUnitLifecycleUpdateUtils.commitLifecycle(
                                        params.getContainerName(),
                                        auGuid,
                                        handlerIO.getLifecyclesClient()
                                    );

                                    // Save updated archive unit in the storage offer
                                    paramsCopy.setObjectName(auGuid);
                                    saveMetadataWithLfcInTheStorage(paramsCopy, handlerIO);
                                } catch (
                                    MetaDataExecutionException
                                    | MetaDataDocumentSizeException
                                    | MetaDataClientServerException
                                    | InvalidCreateOperationException
                                    | InvalidParseOperationException
                                    | MetaDataNotFoundException e
                                ) {
                                    try {
                                        handlerIO
                                            .getLifecyclesClient()
                                            .rollBackUnitsByOperation(params.getContainerName());
                                    } catch (
                                        LogbookClientBadRequestException
                                        | LogbookClientNotFoundException
                                        | LogbookClientServerException ex
                                    ) {
                                        LOGGER.error("Couldn't rollback lifecycles", ex);
                                    }
                                    throw new ProcessingException(e);
                                }
                            }
                        }
                    }
                }
                // deal with archive units then remove the process from the list
                iterator.remove();
            }
        } catch (
            VitamClientException
            | InternalServerException
            | BadRequestException
            | MetaDataExecutionException
            | MetaDataDocumentSizeException
            | MetaDataClientServerException
            | InvalidParseOperationException
            | InvalidCreateOperationException e
        ) {
            throw new ProcessingException(e);
        }
    }

    /**
     * @param workerParameters
     * @throws ProcessingException
     */
    private void saveMetadataWithLfcInTheStorage(WorkerParameters workerParameters, HandlerIO handlerIO)
        throws ProcessingException {
        try {
            storeMetaDataUnitActionPlugin.storeDocumentsWithLfc(
                workerParameters,
                handlerIO,
                singletonList(workerParameters.getObjectName())
            );
        } catch (VitamException e) {
            throw new ProcessingException(e);
        }
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // Nothing to check
    }
}
