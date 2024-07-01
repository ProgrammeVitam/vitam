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
package fr.gouv.vitam.worker.core.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.database.builder.query.CompareQuery;
import fr.gouv.vitam.common.database.builder.query.ExistsQuery;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.utils.ScrollSpliterator;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.UpdateWorkflowConstants;
import fr.gouv.vitam.common.model.administration.FileRulesModel;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import fr.gouv.vitam.worker.core.distribution.JsonLineWriter;
import fr.gouv.vitam.worker.core.plugin.ScrollSpliteratorHelper;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.and;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.exists;

/**
 * ListArchiveUnitsAction Handler.<br>
 */

public class ListArchiveUnitsActionHandler extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ListArchiveUnitsActionHandler.class);

    private static final String HANDLER_ID = "LIST_ARCHIVE_UNITS";
    private static final String JSON = ".json";
    private static final int AU_TO_BE_UPDATED_RANK = 0;
    private final MetaDataClientFactory metaDataClientFactory;

    public ListArchiveUnitsActionHandler() {
        this(MetaDataClientFactory.getInstance());
    }

    public ListArchiveUnitsActionHandler(MetaDataClientFactory metaDataClientFactory) {
        this.metaDataClientFactory = metaDataClientFactory;
    }

    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO handler) {
        final ItemStatus itemStatus = new ItemStatus(HANDLER_ID);
        try {
            Map<String, List<FileRulesModel>> archiveUnitGuidAndRulesToBeUpdated = new HashMap<>();
            List<String> archiveUnitsToBeUpdated = new ArrayList<>();
            // Task number one, lets get the list of archive units to be updated
            selectListOfArchiveUnitsForUpdate(handler, archiveUnitGuidAndRulesToBeUpdated, archiveUnitsToBeUpdated);
            exportToWorkspace(handler, archiveUnitGuidAndRulesToBeUpdated, archiveUnitsToBeUpdated);
            itemStatus.increment(StatusCode.OK);
        } catch (ProcessingException e) {
            LOGGER.error("Fatal : ", e);
            itemStatus.increment(StatusCode.FATAL);
        }
        return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
    }

    private void selectListOfArchiveUnitsForUpdate(
        HandlerIO handlerIO,
        Map<String, List<FileRulesModel>> archiveUnitGuidAndRulesToBeUpdated,
        List<String> archiveUnitsToBeUpdated
    ) throws ProcessingException {
        InputStream input = null;
        try {
            input = handlerIO.getInputStreamFromWorkspace(
                UpdateWorkflowConstants.PROCESSING_FOLDER + "/" + UpdateWorkflowConstants.UPDATED_RULES_JSON
            );
            JsonNode rulesUpdated = JsonHandler.getFromInputStream(input);
            if (rulesUpdated.isArray() && rulesUpdated.size() > 0) {
                for (final JsonNode objNode : rulesUpdated) {
                    searchForInvolvedArchiveUnit(
                        JsonHandler.getFromJsonNode(objNode, FileRulesModel.class),
                        archiveUnitGuidAndRulesToBeUpdated,
                        archiveUnitsToBeUpdated
                    );
                }
            }
        } catch (
            ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException | IOException e
        ) {
            LOGGER.error("Workspace error: Cannot get file", e);
            throw new ProcessingException(e);
        } catch (InvalidParseOperationException e) {
            LOGGER.error("Cannot parse json", e);
            throw new ProcessingException(e);
        } catch (InvalidCreateOperationException e) {
            LOGGER.error("Metadata error: Cannot request Metadata", e);
            throw new ProcessingException(e);
        } finally {
            StreamUtils.closeSilently(input);
        }
    }

    private void searchForInvolvedArchiveUnit(
        FileRulesModel fileRule,
        Map<String, List<FileRulesModel>> archiveUnitGuidAndRulesToBeUpdated,
        List<String> archiveUnitsToBeUpdated
    ) throws InvalidCreateOperationException {
        final SelectMultiQuery selectMultiple = new SelectMultiQuery();
        StringBuffer sb = new StringBuffer();
        sb.append("#management.").append(fileRule.getRuleType()).append(".Rules").append(".Rule");
        try (MetaDataClient metaDataClient = metaDataClientFactory.getClient()) {
            ObjectNode projectionNode = JsonHandler.createObjectNode();
            ObjectNode objectNode = JsonHandler.createObjectNode();
            objectNode.put("#id", 1);
            projectionNode.set("$fields", objectNode);
            ArrayNode arrayNode = JsonHandler.createArrayNode();
            CompareQuery ruleQuery = eq(sb.toString(), fileRule.getRuleId());
            StringBuffer sbexists = new StringBuffer();
            sbexists.append("#management.").append(fileRule.getRuleType()).append(".Rules").append(".StartDate");
            ExistsQuery existsQuery = exists(sbexists.toString());
            selectMultiple.setQuery(and().add(ruleQuery, existsQuery).setDepthLimit(0));
            selectMultiple.addRoots(arrayNode);
            selectMultiple.addProjection(projectionNode);
            LOGGER.debug("Selected Query For linked unit: " + selectMultiple.getFinalSelect().toString());

            ScrollSpliterator<JsonNode> scrollRequest = ScrollSpliteratorHelper.createUnitScrollSplitIterator(
                metaDataClient,
                selectMultiple
            );

            StreamSupport.stream(scrollRequest, false).forEach(item -> {
                String auGuid = item.get("#id").asText();
                if (!archiveUnitsToBeUpdated.contains(auGuid)) {
                    archiveUnitsToBeUpdated.add(auGuid);
                }
                if (archiveUnitGuidAndRulesToBeUpdated.get(auGuid) == null) {
                    final List<FileRulesModel> rulesList = new ArrayList<>();
                    rulesList.add(fileRule);
                    archiveUnitGuidAndRulesToBeUpdated.put(auGuid, rulesList);
                } else {
                    final List<FileRulesModel> rulesList = archiveUnitGuidAndRulesToBeUpdated.get(auGuid);
                    rulesList.add(fileRule);
                    archiveUnitGuidAndRulesToBeUpdated.put(auGuid, rulesList);
                }
            });
        }
    }

    private void exportToWorkspace(
        HandlerIO handlerIO,
        Map<String, List<FileRulesModel>> archiveUnitGuidAndRulesToBeUpdated,
        List<String> archiveUnitsToBeUpdated
    ) throws ProcessingException {
        String distribFileName = handlerIO.getOutput(AU_TO_BE_UPDATED_RANK).getPath();
        File distribFile = handlerIO.getNewLocalFile(distribFileName);

        try (JsonLineWriter jsonLineWriter = new JsonLineWriter(new FileOutputStream(distribFile))) {
            final ArrayNode guidArrayNode = JsonHandler.createArrayNode();
            for (String guid : archiveUnitsToBeUpdated) {
                guidArrayNode.add(guid);
                jsonLineWriter.addEntry(new JsonLineModel(guid));
            }
        } catch (IOException e) {
            throw new ProcessingException(e);
        }

        // list of archive units to be updated
        handlerIO.addOutputResult(AU_TO_BE_UPDATED_RANK, distribFile, true, false);

        try {
            for (String key : archiveUnitGuidAndRulesToBeUpdated.keySet()) {
                final File archiveUnitTempFile = handlerIO.getNewLocalFile(
                    UpdateWorkflowConstants.UNITS_FOLDER + "/" + key + JSON
                );
                final ArrayNode rulesArrayNode = JsonHandler.createArrayNode();
                List<FileRulesModel> rules = archiveUnitGuidAndRulesToBeUpdated.get(key);
                for (FileRulesModel rule : rules) {
                    rulesArrayNode.add(JsonHandler.toJsonNode(rule));
                }
                JsonHandler.writeAsFile(rulesArrayNode, archiveUnitTempFile);
                try {
                    handlerIO.transferFileToWorkspace(
                        UpdateWorkflowConstants.UNITS_FOLDER + "/" + key + JSON,
                        archiveUnitTempFile,
                        true,
                        false
                    );
                } finally {
                    if (!archiveUnitTempFile.delete()) {
                        LOGGER.warn("File couldnt be deleted");
                    }
                }
            }
        } catch (InvalidParseOperationException e) {
            LOGGER.error("Metadata error: Cannot request Metadata", e);
            throw new ProcessingException(e);
        }
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // Nothing to check
    }

    /**
     * @return HANDLER_ID
     */
    public static final String getId() {
        return HANDLER_ID;
    }
}
