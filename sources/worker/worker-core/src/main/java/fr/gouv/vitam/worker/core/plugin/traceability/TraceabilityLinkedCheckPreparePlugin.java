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

package fr.gouv.vitam.worker.core.plugin.traceability;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.parser.request.single.SelectParserSingle;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.WorkspaceConstants;
import fr.gouv.vitam.common.model.logbook.LogbookEvent;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import fr.gouv.vitam.worker.core.distribution.JsonLineWriter;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.utils.PluginHelper;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static fr.gouv.vitam.common.model.RequestResponseOK.TAG_RESULTS;
import static fr.gouv.vitam.common.model.logbook.LogbookEvent.EV_TYPE;
import static fr.gouv.vitam.logbook.common.parameters.Contexts.LOGBOOK_STORAGE_TRACEABILITY;
import static fr.gouv.vitam.logbook.common.parameters.Contexts.LOGBOOK_TRACEABILITY;
import static fr.gouv.vitam.logbook.common.parameters.Contexts.OBJECTGROUP_LFC_TRACEABILITY;
import static fr.gouv.vitam.logbook.common.parameters.Contexts.UNIT_LFC_TRACEABILITY;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;

public class TraceabilityLinkedCheckPreparePlugin extends ActionHandler {

    private static final String PLUGIN_NAME = "TRACEABILITY_LINKED_CHECK_PREPARE";
    private static final String[] EVENT_TRACEABILITY_TYPES = {
        LOGBOOK_TRACEABILITY.getEventType(),
        LOGBOOK_STORAGE_TRACEABILITY.getEventType(),
        UNIT_LFC_TRACEABILITY.getEventType(),
        OBJECTGROUP_LFC_TRACEABILITY.getEventType(),
    };

    static final String LOGBOOK_OPERATIONS_JSONL_FILE = "logbookOperations.jsonl";

    private final LogbookOperationsClientFactory logbookOperationsClientFactory;

    public TraceabilityLinkedCheckPreparePlugin() {
        this(LogbookOperationsClientFactory.getInstance());
    }

    @VisibleForTesting
    TraceabilityLinkedCheckPreparePlugin(LogbookOperationsClientFactory logbookOperationsClientFactory) {
        this.logbookOperationsClientFactory = logbookOperationsClientFactory;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException {
        ItemStatus itemStatus = new ItemStatus(PLUGIN_NAME);
        JsonNode dslQuery = filterQuery(handler.getJsonFromWorkspace(WorkspaceConstants.QUERY));
        File logbookOperationDistributionFile = handler.getNewLocalFile(LOGBOOK_OPERATIONS_JSONL_FILE);

        List<String> skippedOperations = new ArrayList<>();

        try (LogbookOperationsClient logbookOperationsClient = logbookOperationsClientFactory.getClient()) {
            JsonNode logbookOperations = logbookOperationsClient.selectOperation(dslQuery);
            JsonNode results = logbookOperations.get(TAG_RESULTS);

            try (
                JsonLineWriter logbookOperationsWriter = new JsonLineWriter(
                    new FileOutputStream(logbookOperationDistributionFile)
                )
            ) {
                for (int i = 0; i < results.size(); i++) {
                    JsonNode logbookOperation = results.get(i);
                    JsonNode eventDetail = JsonHandler.getFromString(
                        logbookOperation.get(LogbookMongoDbName.eventDetailData.getDbname()).asText()
                    );
                    if (eventDetail instanceof NullNode || eventDetail.isEmpty()) {
                        skippedOperations.add(logbookOperation.get(LogbookEvent.EV_ID).asText());
                    } else {
                        JsonLineModel entry = new JsonLineModel(
                            logbookOperation.get(LogbookEvent.EV_ID).textValue(),
                            null,
                            eventDetail
                        );
                        logbookOperationsWriter.addEntry(entry);
                    }
                }
            }
            handler.transferFileToWorkspace(
                LOGBOOK_OPERATIONS_JSONL_FILE,
                logbookOperationDistributionFile,
                true,
                false
            );
        } catch (IOException | LogbookClientException | InvalidParseOperationException e) {
            return buildItemStatus(PLUGIN_NAME, StatusCode.FATAL, e.getMessage());
        } finally {
            FileUtils.deleteQuietly(logbookOperationDistributionFile);
        }

        if (!skippedOperations.isEmpty()) {
            itemStatus.setItemsStatus(
                buildItemStatus(
                    PLUGIN_NAME,
                    StatusCode.WARNING,
                    PluginHelper.EventDetails.of(
                        String.format(
                            "These operations %s does not contains data. they will be skipped !",
                            skippedOperations.toString()
                        )
                    )
                )
            );
            return itemStatus;
        }

        itemStatus.setItemsStatus(buildItemStatus(PLUGIN_NAME, StatusCode.OK));
        return itemStatus;
    }

    private JsonNode filterQuery(JsonNode dslQuery) throws ProcessingException {
        try {
            final SelectParserSingle parser = new SelectParserSingle();
            parser.parse(dslQuery);
            parser.addCondition(QueryHelper.in(EV_TYPE, EVENT_TRACEABILITY_TYPES));
            return parser.getRootNode();
        } catch (InvalidParseOperationException | InvalidCreateOperationException e) {
            throw new ProcessingException(e);
        }
    }
}
