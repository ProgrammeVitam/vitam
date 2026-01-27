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
package fr.gouv.vitam.worker.core.plugin.bulkatomicupdate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.batch.report.client.BatchReportClient;
import fr.gouv.vitam.batch.report.model.ReportBody;
import fr.gouv.vitam.batch.report.model.ReportType;
import fr.gouv.vitam.batch.report.model.entry.BulkUpdateUnitMetadataReportEntry;
import fr.gouv.vitam.common.InternalActionKeysRetriever;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.utils.AccessContractRestrictionHelper;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.jsonl.JsonLineWriter;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.common.utils.BufferedConsumer;
import fr.gouv.vitam.metadata.api.utils.BulkAtomicUpdateModelUtils;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.common.bulkatomicupdate.BulkSelectQueryParallelProcessor;
import fr.gouv.vitam.metadata.common.bulkatomicupdate.BulkSelectQueryResultFailure;
import fr.gouv.vitam.metadata.common.bulkatomicupdate.BulkSelectQueryResultOK;
import fr.gouv.vitam.metadata.common.bulkatomicupdate.QueryRestrictionConverter;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import fr.gouv.vitam.worker.core.exception.ProcessingStatusException;
import fr.gouv.vitam.worker.core.handler.ActionHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Prepares the execution of atomic update queries.
 * Queries are executed in bulks, each bulk is run concurrently is a thread pool.
 * Queries are updated with access contract restrictions.
 * Query projection is set to "_id" field only.
 * Queries with internal fields are blocked ==> Report WARNING in batch report
 * Queries result size is limited to 2.
 * - If a single entry is found ==> Happy path, we append unitId to distribution file
 * - No entries found           ==> Report WARNING in batch report (no unit found)
 * - 2 entries found            ==> Report WARNING in batch report (multiple units found)
 * Report entries are buffered and sent as bulks to BatchReport (to reduce IOs to BatchReport)
 * Distribution file entries are buffered and written to disk in bulks  (to reduce IO contention)
 */
public class PrepareBulkAtomicUpdate extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(PrepareBulkAtomicUpdate.class);
    public static final String PREPARE_BULK_ATOMIC_UPDATE_UNIT_LIST_PLUGIN_NAME =
        "PREPARE_BULK_ATOMIC_UPDATE_UNIT_LIST";
    private static final String ORIGIN_QUERY_KEY = "originQuery";
    private static final String QUERY_INDEX_KEY = "queryIndex";

    // INPUTS
    private static final String QUERY_NAME_IN = "query.json";
    private static final String ACCESS_CONTRACT_NAME_IN = "accessContract.json";

    // OUTPUTS
    private static final int DISTRIBUTION_FILE_RANK = 0;

    private final InternalActionKeysRetriever internalActionKeysRetriever;
    private final int batchSize;
    private final int threadPoolSize;
    private final int threadPoolQueueSize;

    /**
     * Constructor.
     */
    public PrepareBulkAtomicUpdate() {
        this(
            new InternalActionKeysRetriever(),
            VitamConfiguration.getBulkAtomicUpdateBatchSize(),
            VitamConfiguration.getBulkAtomicUpdateThreadPoolSize(),
            VitamConfiguration.getBulkAtomicUpdateThreadPoolQueueSize()
        );
    }

    /**
     * Constructor.
     *
     * @param internalActionKeysRetriever DSL query field name validator
     * @param batchSize batch size for processing
     * @param threadPoolSize max threads that can be run in concurrently is thread pool
     * @param threadPoolQueueSize number of jobs that can be queued before blocking (limits workload memory usage)
     */
    @VisibleForTesting
    PrepareBulkAtomicUpdate(
        InternalActionKeysRetriever internalActionKeysRetriever,
        int batchSize,
        int threadPoolSize,
        int threadPoolQueueSize
    ) {
        this.internalActionKeysRetriever = internalActionKeysRetriever;
        this.batchSize = batchSize;
        this.threadPoolSize = threadPoolSize;
        this.threadPoolQueueSize = threadPoolQueueSize;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException {
        try {
            // Retrieve inputs
            AccessContractModel accessContractModel = loadAccessContract(handler);
            Iterator<JsonNode> queryIterator = loadQueries(handler);

            // Process queries and generate distribution file / report
            final String distributionFileName = handler.getOutput(DISTRIBUTION_FILE_RANK).getPath();
            final File distributionFile = handler.getNewLocalFile(distributionFileName);

            ItemStatus itemStatus;
            try (
                JsonLineWriter jsonLineWriter = new JsonLineWriter(new FileOutputStream(distributionFile));
                MetaDataClient metadataClient = handler.getMetaDataClient();
                BatchReportClient batchReportClient = handler.getBatchReportClient()
            ) {
                itemStatus = processQueries(
                    metadataClient,
                    batchReportClient,
                    accessContractModel,
                    queryIterator,
                    jsonLineWriter
                );
            }

            // move file to workspace
            handler.transferFileToWorkspace(distributionFileName, distributionFile, true, false);

            return new ItemStatus(PREPARE_BULK_ATOMIC_UPDATE_UNIT_LIST_PLUGIN_NAME).setItemsStatus(
                PREPARE_BULK_ATOMIC_UPDATE_UNIT_LIST_PLUGIN_NAME,
                itemStatus
            );
        } catch (IOException | RuntimeException | ProcessingException e) {
            LOGGER.error("Bulk atomic update preparation failed", e);
            return buildFatalItemStatus(StatusCode.FATAL);
        } catch (ProcessingStatusException e) {
            LOGGER.error("Bulk atomic update preparation failed", e);
            return buildFatalItemStatus(e.getStatusCode());
        }
    }

    private ItemStatus buildFatalItemStatus(StatusCode statusCode) {
        final ItemStatus itemStatus = new ItemStatus(PREPARE_BULK_ATOMIC_UPDATE_UNIT_LIST_PLUGIN_NAME);
        itemStatus.increment(statusCode);
        return new ItemStatus(PREPARE_BULK_ATOMIC_UPDATE_UNIT_LIST_PLUGIN_NAME).setItemsStatus(
            PREPARE_BULK_ATOMIC_UPDATE_UNIT_LIST_PLUGIN_NAME,
            itemStatus
        );
    }

    private AccessContractModel loadAccessContract(HandlerIO handler) throws ProcessingStatusException {
        try {
            JsonNode accessContractNode = handler.getJsonFromWorkspace(ACCESS_CONTRACT_NAME_IN);
            return JsonHandler.getFromJsonNode(accessContractNode, AccessContractModel.class);
        } catch (InvalidParseOperationException | ProcessingException ex) {
            throw new ProcessingStatusException(StatusCode.FATAL, "Could not load access contract", ex);
        }
    }

    private Iterator<JsonNode> loadQueries(HandlerIO handler) throws ProcessingStatusException {
        try {
            JsonNode queryNodes = handler.getJsonFromWorkspace(QUERY_NAME_IN);
            return BulkAtomicUpdateModelUtils.getQueries(queryNodes).iterator();
        } catch (ProcessingException ex) {
            throw new ProcessingStatusException(StatusCode.FATAL, "Could not load queries", ex);
        }
    }

    private ItemStatus processQueries(
        MetaDataClient metadataClient,
        BatchReportClient batchReportClient,
        AccessContractModel accessContractModel,
        Iterator<JsonNode> queryIterator,
        JsonLineWriter jsonLineWriter
    ) throws ProcessingStatusException {
        try (
            BufferedConsumer<BulkSelectQueryResultOK> successReporter = createSuccessReporter(jsonLineWriter);
            BufferedConsumer<BulkSelectQueryResultFailure> failureReporter = createFailureReporter(batchReportClient)
        ) {
            BulkSelectQueryParallelProcessor bulkSelectQueryParallelProcessor = new BulkSelectQueryParallelProcessor(
                metadataClient,
                internalActionKeysRetriever,
                threadPoolSize,
                threadPoolQueueSize,
                batchSize,
                successReporter,
                failureReporter,
                createAccessContractRestrictionConverter(accessContractModel),
                false
            );

            bulkSelectQueryParallelProcessor.processQueries(queryIterator);

            final ItemStatus itemStatus = new ItemStatus(PREPARE_BULK_ATOMIC_UPDATE_UNIT_LIST_PLUGIN_NAME);
            if (bulkSelectQueryParallelProcessor.getNbOKs() > 0) {
                itemStatus.increment(StatusCode.OK, bulkSelectQueryParallelProcessor.getNbOKs());
            }
            if (bulkSelectQueryParallelProcessor.getNbWarnings() > 0) {
                itemStatus.increment(StatusCode.WARNING, bulkSelectQueryParallelProcessor.getNbWarnings());
            }
            return itemStatus;
        } catch (InvalidParseOperationException e) {
            throw new ProcessingStatusException(StatusCode.KO, "Query processing failed with KO", e);
        }
    }

    private static BufferedConsumer<BulkSelectQueryResultOK> createSuccessReporter(JsonLineWriter jsonLineWriter) {
        return new BufferedConsumer<>(VitamConfiguration.getBatchSize(), (List<BulkSelectQueryResultOK> entries) -> {
            List<JsonLineModel> lines = entries
                .stream()
                .map(PrepareBulkAtomicUpdate::createJsonLineEntry)
                .collect(Collectors.toList());

            try {
                jsonLineWriter.addEntries(lines);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    private static JsonLineModel createJsonLineEntry(BulkSelectQueryResultOK bulkSelectQueryResultOK) {
        ObjectNode params = JsonHandler.createObjectNode();
        params.set(ORIGIN_QUERY_KEY, bulkSelectQueryResultOK.getQuery());
        params.put(QUERY_INDEX_KEY, bulkSelectQueryResultOK.getQueryIndex());
        return new JsonLineModel(bulkSelectQueryResultOK.getUnitId(), null, params);
    }

    private static BufferedConsumer<BulkSelectQueryResultFailure> createFailureReporter(
        BatchReportClient batchReportClient
    ) {
        int tenantId = VitamThreadUtils.getVitamSession().getTenantId();
        String processId = VitamThreadUtils.getVitamSession().getRequestId();

        return new BufferedConsumer<>(VitamConfiguration.getBatchSize(), (List<
            BulkSelectQueryResultFailure
        > entries) -> {
            List<BulkUpdateUnitMetadataReportEntry> bufferedReportEntries = entries
                .stream()
                .map(
                    entry ->
                        new BulkUpdateUnitMetadataReportEntry(
                            tenantId,
                            processId,
                            Integer.toString(entry.getQueryIndex()),
                            JsonHandler.unprettyPrint(entry.getQuery()),
                            null,
                            entry.getBulkUpdateUnitReportKey().name(),
                            StatusCode.WARNING,
                            String.format(
                                "%s.%s",
                                PREPARE_BULK_ATOMIC_UPDATE_UNIT_LIST_PLUGIN_NAME,
                                StatusCode.WARNING
                            ),
                            entry.getMessage()
                        )
                )
                .collect(Collectors.toList());

            try {
                ReportBody<BulkUpdateUnitMetadataReportEntry> reportBody = new ReportBody<>();
                reportBody.setProcessId(processId);
                reportBody.setReportType(ReportType.BULK_UPDATE_UNIT);
                reportBody.setEntries(new ArrayList<>(bufferedReportEntries));
                batchReportClient.appendReportEntries(reportBody);
            } catch (VitamClientInternalException e) {
                throw new VitamRuntimeException(e);
            }
        });
    }

    private static QueryRestrictionConverter createAccessContractRestrictionConverter(
        AccessContractModel accessContractModel
    ) {
        return originalQuery ->
            AccessContractRestrictionHelper.applyAccessContractRestrictionForUnitForSelect(
                originalQuery,
                accessContractModel
            );
    }
}
