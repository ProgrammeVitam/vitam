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
package fr.gouv.vitam.worker.core.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.model.TraceabilityEvent;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookDocument;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookOperation;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.metadata.core.database.collections.MetadataDocument;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.worker.common.HandlerIO;

import java.io.File;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static fr.gouv.vitam.common.LocalDateUtil.getFormattedDateForMongo;
import static fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName.eventDetailData;
import static fr.gouv.vitam.logbook.common.traceability.LogbookTraceabilityHelper.INITIAL_START_DATE;

/**
 * ListLifecycleTraceabilityAction Plugin
 */
public abstract class ListLifecycleTraceabilityActionHandler extends ActionHandler {
    private static final VitamLogger LOGGER =
        VitamLoggerFactory.getInstance(ListLifecycleTraceabilityActionHandler.class);

    private static final String JSON_EXTENSION = ".json";

    private boolean asyncIO = false;

    private static final int LAST_OPERATION_LIFECYCLES_RANK = 0;
    private static final int TRACEABILITY_INFORMATION_RANK = 1;

    private final LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory;
    private final LogbookOperationsClientFactory logbookOperationsClientFactory;
    private final int batchSize;

    /**
     * Empty constructor ListLifecycleTraceabilityActionPlugin
     */
    public ListLifecycleTraceabilityActionHandler() {
        this(LogbookLifeCyclesClientFactory.getInstance(), LogbookOperationsClientFactory.getInstance(),
            VitamConfiguration.getBatchSize());
    }

    /**
     * Constructor for testing
     */
    @VisibleForTesting
    ListLifecycleTraceabilityActionHandler(LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory,
        LogbookOperationsClientFactory logbookOperationsClientFactory, int batchSize) {
        this.logbookLifeCyclesClientFactory = logbookLifeCyclesClientFactory;
        this.logbookOperationsClientFactory = logbookOperationsClientFactory;
        this.batchSize = batchSize;
    }

    protected void selectAndExportLifecycles(int temporizationDelayInSeconds,
        int lifecycleTraceabilityMaxEntries, String workspaceMetadataFolder, String eventType, HandlerIO handlerIO)
        throws ProcessingException, InvalidParseOperationException, LogbookClientException,
        InvalidCreateOperationException {

        final LogbookOperation lastTraceabilityOperation = findLastOperationTraceabilityLifecycle(
            eventType);

        exportLastOperationTraceabilityLifecycle(handlerIO, lastTraceabilityOperation);

        LocalDateTime traceabilityStartDate = getTraceabilityOperationStartDate(lastTraceabilityOperation);
        LocalDateTime traceabilityEndDate = LocalDateUtil.now()
            .minusSeconds(temporizationDelayInSeconds);

        LocalDateTime selectionStartDate = traceabilityStartDate;
        LocalDateTime selectionEndDate = traceabilityEndDate;

        boolean maxEntriesReached = false;
        int nbExportedEntries = 0;
        LocalDateTime maxEntryLastPersistedDate = traceabilityStartDate;

        // List / export metadata
        try (LogbookLifeCyclesClient logbookLifeCyclesClient =
            logbookLifeCyclesClientFactory.getClient()) {

            Set<String> lastBatchIds = new HashSet<>();

            while (true) {

                int remaining = lifecycleTraceabilityMaxEntries - nbExportedEntries;
                if (remaining == 0) {
                    maxEntriesReached = true;
                    break;
                }

                int limit = Math.min(batchSize, remaining);
                List<JsonNode> rawLifecycles =
                    getRawLifecyclesByLastPersistedDate(selectionStartDate, selectionEndDate, logbookLifeCyclesClient,
                        limit);

                Set<String> currentBatchIds = new HashSet<>();

                for (JsonNode item : rawLifecycles) {

                    // Skip entry if already proceeded in last bulk
                    String itemId = item.get(LogbookDocument.ID).textValue();
                    if (lastBatchIds.contains(itemId)) {
                        continue;
                    }
                    currentBatchIds.add(itemId);

                    exportToWorkspace(handlerIO, item, workspaceMetadataFolder);

                    nbExportedEntries++;

                    String entryLastPersistedDateStr = item.get(LogbookDocument.LAST_PERSISTED_DATE).asText();
                    LocalDateTime entryLastPersistedDate =
                        LocalDateUtil.parseMongoFormattedDate(entryLastPersistedDateStr);
                    if (entryLastPersistedDate.isAfter(maxEntryLastPersistedDate)) {
                        maxEntryLastPersistedDate = entryLastPersistedDate;
                    }
                }

                if (rawLifecycles.size() < limit) {
                    // No more entries to proceed. Done
                    break;
                }

                // Mark current bulk ids for next bulk
                lastBatchIds = currentBatchIds;
                selectionStartDate = maxEntryLastPersistedDate;
            }
        }

        if (maxEntriesReached) {
            // Override end date if max entries reached
            traceabilityEndDate = maxEntryLastPersistedDate;
        }

        exportTraceabilityInformation(handlerIO, traceabilityStartDate, traceabilityEndDate, nbExportedEntries,
            maxEntriesReached);
    }

    abstract protected List<JsonNode> getRawLifecyclesByLastPersistedDate(LocalDateTime startDate,
        LocalDateTime endDate,
        LogbookLifeCyclesClient logbookLifeCyclesClient, int limit)
        throws LogbookClientException, InvalidParseOperationException;

    private LocalDateTime getTraceabilityOperationStartDate(LogbookOperation lastTraceabilityOperation)
        throws InvalidParseOperationException {
        if (lastTraceabilityOperation == null) {
            return INITIAL_START_DATE;
        } else {
            final String evDetData = (String) lastTraceabilityOperation.get(eventDetailData.getDbname());
            TraceabilityEvent traceabilityEvent = JsonHandler.getFromString(evDetData, TraceabilityEvent.class);
            return LocalDateUtil.parseMongoFormattedDate(traceabilityEvent.getEndDate());
        }
    }

    private LogbookOperation findLastOperationTraceabilityLifecycle(String eventType)
        throws InvalidCreateOperationException, InvalidParseOperationException, LogbookClientException {
        final Select select = new Select();
        final Query type = QueryHelper.eq("evTypeProc", LogbookTypeProcess.TRACEABILITY.name());
        final Query findEvent = QueryHelper
            .eq(String.format("%s.%s", LogbookDocument.EVENTS, LogbookMongoDbName.outcomeDetail.getDbname()),
                eventType + ".OK");

        select.setLimitFilter(0, 1);
        select.setQuery(QueryHelper.and().add(type, findEvent));

        select.addOrderByDescFilter("evDateTime");
        try (LogbookOperationsClient logbookOperationsClient =
            logbookOperationsClientFactory.getClient()) {
            RequestResponseOK requestResponseOK =
                RequestResponseOK.getFromJsonNode(logbookOperationsClient.selectOperation(select.getFinalSelect()));
            List<ObjectNode> foundOperation = requestResponseOK.getResults();
            if (foundOperation != null && foundOperation.size() >= 1) {
                return new LogbookOperation(foundOperation.get(0));
            }

            LOGGER.debug("Logbook not found, this is the first Operation of this type");
            return null;
        }
    }

    private void exportToWorkspace(HandlerIO handlerIO, JsonNode item,
        String workspaceMetadataFolder) throws ProcessingException {

        String metadataId = item.get(MetadataDocument.ID).asText();
        handlerIO.transferJsonToWorkspace(workspaceMetadataFolder, metadataId + JSON_EXTENSION,
            item, true, asyncIO);
    }

    private void exportTraceabilityInformation(
        HandlerIO handlerIO,
        LocalDateTime traceabilityStartDate,
        LocalDateTime traceabilityEndDate,
        long nbEntries, boolean maxEntriesReached) throws InvalidParseOperationException, ProcessingException {

        ObjectNode traceabilityInformation = JsonHandler.createObjectNode();
        traceabilityInformation.put("startDate", getFormattedDateForMongo(traceabilityStartDate));
        traceabilityInformation.put("endDate", getFormattedDateForMongo(traceabilityEndDate));
        traceabilityInformation.put("nbEntries", nbEntries);
        traceabilityInformation.put("maxEntriesReached", maxEntriesReached);

        // export in workspace
        File tempFile = handlerIO.getNewLocalFile(handlerIO.getOutput(TRACEABILITY_INFORMATION_RANK).getPath());
        // Create json file
        JsonHandler.writeAsFile(traceabilityInformation, tempFile);
        handlerIO.addOuputResult(TRACEABILITY_INFORMATION_RANK, tempFile, true, false);
    }

    private void exportLastOperationTraceabilityLifecycle(HandlerIO handlerIO,
        LogbookOperation lastOperationTraceability)
        throws InvalidParseOperationException, ProcessingException {
        File tempFile = handlerIO.getNewLocalFile(handlerIO.getOutput(LAST_OPERATION_LIFECYCLES_RANK).getPath());
        if (lastOperationTraceability == null) {
            // empty json file
            JsonHandler.writeAsFile(JsonHandler.createObjectNode(), tempFile);
            handlerIO.addOuputResult(LAST_OPERATION_LIFECYCLES_RANK, tempFile, true, false);
        } else {
            // Create json file
            JsonHandler.writeAsFile(lastOperationTraceability, tempFile);
            handlerIO.addOuputResult(LAST_OPERATION_LIFECYCLES_RANK, tempFile, true, false);
        }
    }
}
