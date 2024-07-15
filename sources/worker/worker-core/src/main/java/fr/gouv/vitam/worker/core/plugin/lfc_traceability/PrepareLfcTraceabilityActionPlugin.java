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
package fr.gouv.vitam.worker.core.plugin.lfc_traceability;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Iterators;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.collection.CloseableIterator;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.performance.PerformanceLogger;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.model.TraceabilityEvent;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookDocument;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookOperation;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.distribution.JsonLineGenericIterator;
import fr.gouv.vitam.worker.core.distribution.JsonLineWriter;
import fr.gouv.vitam.worker.core.handler.ActionHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName.eventDetailData;
import static fr.gouv.vitam.logbook.common.traceability.LogbookTraceabilityHelper.INITIAL_START_DATE;

public abstract class PrepareLfcTraceabilityActionPlugin extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(PrepareLfcTraceabilityActionPlugin.class);

    private static final int LAST_OPERATION_LIFECYCLES_IN_RANK = 0;
    private static final int TRACEABILITY_INFORMATION_OUT_RANK = 0;
    private static final int LFC_AND_METADATA_OUT_RANK = 1;
    public static final TypeReference<JsonNode> JSON_NODE_TYPE_REFERENCE = new TypeReference<>() {};

    private final MetaDataClientFactory metaDataClientFactory;
    private final LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory;
    private final int batchSize;

    public PrepareLfcTraceabilityActionPlugin() {
        this(
            MetaDataClientFactory.getInstance(),
            LogbookLifeCyclesClientFactory.getInstance(),
            VitamConfiguration.getBatchSize()
        );
    }

    @VisibleForTesting
    PrepareLfcTraceabilityActionPlugin(
        MetaDataClientFactory metaDataClientFactory,
        LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory,
        int batchSize
    ) {
        this.metaDataClientFactory = metaDataClientFactory;
        this.logbookLifeCyclesClientFactory = logbookLifeCyclesClientFactory;
        this.batchSize = batchSize;
    }

    protected StatusCode selectAndExportLifecyclesWithMetadata(
        int temporizationDelayInSeconds,
        int lifecycleTraceabilityMaxEntries,
        String eventType,
        HandlerIO handlerIO
    ) throws ProcessingException, InvalidParseOperationException, LogbookClientException {
        final LogbookOperation lastTraceabilityOperation = loadLastOperationTraceabilityLifecycle(handlerIO);

        LocalDateTime traceabilityStartDate = getTraceabilityOperationStartDate(lastTraceabilityOperation);
        LocalDateTime traceabilityEndDate = LocalDateUtil.now().minusSeconds(temporizationDelayInSeconds);

        int nbExportedEntries = 0;
        LocalDateTime maxEntryLastPersistedDate = traceabilityStartDate;

        File lfcWithMetadataFile = handlerIO.getNewLocalFile(handlerIO.getOutput(LFC_AND_METADATA_OUT_RANK).getPath());

        // List / export metadata
        try (
            InputStream is = exportRawLifecyclesByLastPersistedDate(
                logbookLifeCyclesClientFactory,
                traceabilityStartDate,
                traceabilityEndDate,
                lifecycleTraceabilityMaxEntries
            );
            CloseableIterator<JsonNode> rawLifecycleIterator = new JsonLineGenericIterator<>(
                is,
                JSON_NODE_TYPE_REFERENCE
            );
            OutputStream os = new FileOutputStream(lfcWithMetadataFile);
            JsonLineWriter jsonLineWriter = new JsonLineWriter(os)
        ) {
            Iterator<List<JsonNode>> bulkRawLifecycleIterator = Iterators.partition(rawLifecycleIterator, batchSize);

            while (bulkRawLifecycleIterator.hasNext()) {
                List<JsonNode> rawLifecycleToProceed = bulkRawLifecycleIterator.next();

                Set<String> currentBatchIds = rawLifecycleToProceed
                    .stream()
                    .map(item -> item.get(LogbookDocument.ID).textValue())
                    .collect(Collectors.toSet());

                for (JsonNode item : rawLifecycleToProceed) {
                    nbExportedEntries++;

                    String entryLastPersistedDateStr = item.get(LogbookDocument.LAST_PERSISTED_DATE).asText();
                    LocalDateTime entryLastPersistedDate = LocalDateUtil.parseMongoFormattedDate(
                        entryLastPersistedDateStr
                    );
                    if (entryLastPersistedDate.isAfter(maxEntryLastPersistedDate)) {
                        maxEntryLastPersistedDate = entryLastPersistedDate;
                    }
                }

                Stopwatch loadTraceabilityMetadata = Stopwatch.createStarted();
                Map<String, JsonNode> rawMetadataByIds = getRawMetadata(currentBatchIds, metaDataClientFactory);
                PerformanceLogger.getInstance()
                    .log(
                        stepName(),
                        actionName(),
                        "LOAD_TRACEABILITY_METADATA",
                        loadTraceabilityMetadata.elapsed(TimeUnit.MILLISECONDS)
                    );

                for (JsonNode rawLfc : rawLifecycleToProceed) {
                    String id = rawLfc.get(LogbookDocument.ID).textValue();
                    JsonNode rawMetadata = rawMetadataByIds.get(id);

                    // FIXME : Elimination during traceability (very improbable, but possible)
                    if (rawMetadata == null) {
                        throw new ProcessingException("Metadata not found for id " + id);
                    }

                    LfcMetadataPair lfcMetadataPair = new LfcMetadataPair(rawMetadata, rawLfc);

                    jsonLineWriter.addEntry(lfcMetadataPair);
                }
            }
        } catch (IOException e) {
            throw new ProcessingException("Could not export lfc and metadata for traceability", e);
        }

        handlerIO.addOutputResult(LFC_AND_METADATA_OUT_RANK, lfcWithMetadataFile, false, false);

        boolean maxEntriesReached = nbExportedEntries >= lifecycleTraceabilityMaxEntries;
        if (maxEntriesReached) {
            // Override end date if max entries reached
            traceabilityEndDate = maxEntryLastPersistedDate;
        }

        exportTraceabilityInformation(
            handlerIO,
            traceabilityStartDate,
            traceabilityEndDate,
            nbExportedEntries,
            maxEntriesReached
        );

        LOGGER.info("Metadata traceability entries: " + nbExportedEntries);

        return (nbExportedEntries > 0) ? StatusCode.OK : StatusCode.WARNING;
    }

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

    private LogbookOperation loadLastOperationTraceabilityLifecycle(HandlerIO handlerIO)
        throws InvalidParseOperationException {
        JsonNode logbookOperation = JsonHandler.getFromFile(
            (File) handlerIO.getInput(LAST_OPERATION_LIFECYCLES_IN_RANK)
        );
        if (logbookOperation.isEmpty()) {
            return null;
        }
        return JsonHandler.getFromJsonNode(logbookOperation, LogbookOperation.class);
    }

    private void exportTraceabilityInformation(
        HandlerIO handlerIO,
        LocalDateTime traceabilityStartDate,
        LocalDateTime traceabilityEndDate,
        long nbEntries,
        boolean maxEntriesReached
    ) throws InvalidParseOperationException, ProcessingException {
        ObjectNode traceabilityInformation = JsonHandler.createObjectNode();
        traceabilityInformation.put("startDate", LocalDateUtil.getFormattedDateTimeForMongo(traceabilityStartDate));
        traceabilityInformation.put("endDate", LocalDateUtil.getFormattedDateTimeForMongo(traceabilityEndDate));
        traceabilityInformation.put("nbEntries", nbEntries);
        traceabilityInformation.put("maxEntriesReached", maxEntriesReached);

        // export in workspace

        File tempFile = handlerIO.getNewLocalFile(handlerIO.getOutput(TRACEABILITY_INFORMATION_OUT_RANK).getPath());
        // Create json file
        JsonHandler.writeAsFile(traceabilityInformation, tempFile);
        handlerIO.addOutputResult(TRACEABILITY_INFORMATION_OUT_RANK, tempFile, false, false);
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // NOP.
    }

    protected abstract InputStream exportRawLifecyclesByLastPersistedDate(
        LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory,
        LocalDateTime selectionStartDate,
        LocalDateTime selectionEndDate,
        int lifecycleTraceabilityMaxEntries
    ) throws LogbookClientException, InvalidParseOperationException, IOException;

    protected abstract Map<String, JsonNode> getRawMetadata(
        Set<String> ids,
        MetaDataClientFactory metaDataClientFactory
    ) throws ProcessingException;

    protected abstract String stepName();

    protected abstract String actionName();
}
