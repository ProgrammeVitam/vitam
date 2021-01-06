/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterators;
import fr.gouv.vitam.batch.report.client.BatchReportClient;
import fr.gouv.vitam.batch.report.client.BatchReportClientFactory;
import fr.gouv.vitam.common.InternalActionKeysRetriever;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.thread.ExecutorUtils;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.distribution.JsonLineWriter;
import fr.gouv.vitam.worker.core.exception.ProcessingStatusException;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.utils.CountingIterator;
import fr.gouv.vitam.worker.core.utils.CountingIterator.EntryWithIndex;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Prepare execute execute each query in query.json.
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

    // INPUTS
    private static final String QUERY_NAME_IN = "query.json";
    private static final String ACCESS_CONTRACT_NAME_IN = "accessContract.json";

    // OUTPUTS
    private static final int DISTRIBUTION_FILE_RANK = 0;

    private final MetaDataClientFactory metaDataClientFactory;
    private final BatchReportClientFactory batchReportClientFactory;
    private final InternalActionKeysRetriever internalActionKeysRetriever;
    private final int batchSize;
    private final int threadPoolSize;
    private final int threadPoolQueueSize;

    /**
     * Constructor.
     */
    public PrepareBulkAtomicUpdate() {
        this(MetaDataClientFactory.getInstance(), BatchReportClientFactory.getInstance(),
            new InternalActionKeysRetriever(), VitamConfiguration.getBulkAtomicUpdateBatchSize(),
            VitamConfiguration.getBulkAtomicUpdateThreadPoolSize(),
            VitamConfiguration.getBulkAtomicUpdateThreadPoolQueueSize());
    }

    /**
     * Constructor.
     *
     * @param metaDataClientFactory metadata client factory
     * @param batchReportClientFactory batch report client factory
     * @param internalActionKeysRetriever
     * @param batchSize batch size for processing
     * @param threadPoolSize max threads that can be run in concurrently is thread pool
     * @param threadPoolQueueSize number of jobs that can be queued before blocking (limits workload memory usage)
     */
    @VisibleForTesting
    PrepareBulkAtomicUpdate(MetaDataClientFactory metaDataClientFactory,
        BatchReportClientFactory batchReportClientFactory,
        InternalActionKeysRetriever internalActionKeysRetriever, int batchSize, int threadPoolSize,
        int threadPoolQueueSize) {
        this.metaDataClientFactory = metaDataClientFactory;
        this.batchReportClientFactory = batchReportClientFactory;
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
            try (JsonLineWriter jsonLineWriter = new JsonLineWriter(new FileOutputStream(distributionFile));
                MetaDataClient metadataClient = metaDataClientFactory.getClient();
                BatchReportClient batchReportClient = batchReportClientFactory.getClient()) {

                itemStatus = processQueries(param.getProcessId(), metadataClient, batchReportClient,
                    accessContractModel, queryIterator, jsonLineWriter);
            }

            // move file to workspace
            handler.transferFileToWorkspace(distributionFileName, distributionFile, true, false);

            return new ItemStatus(PREPARE_BULK_ATOMIC_UPDATE_UNIT_LIST_PLUGIN_NAME)
                .setItemsStatus(PREPARE_BULK_ATOMIC_UPDATE_UNIT_LIST_PLUGIN_NAME, itemStatus);

        } catch (IOException | VitamRuntimeException | IllegalStateException | ProcessingException e) {
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
        return new ItemStatus(PREPARE_BULK_ATOMIC_UPDATE_UNIT_LIST_PLUGIN_NAME)
            .setItemsStatus(PREPARE_BULK_ATOMIC_UPDATE_UNIT_LIST_PLUGIN_NAME, itemStatus);
    }

    private AccessContractModel loadAccessContract(HandlerIO handler)
        throws ProcessingStatusException {
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
            return queryNodes.get(BulkAtomicUpdateModelUtils.QUERIES).iterator();
        } catch (ProcessingException ex) {
            throw new ProcessingStatusException(StatusCode.FATAL, "Could not load queries", ex);
        }
    }

    private ItemStatus processQueries(String processId,
        MetaDataClient metadataClient,
        BatchReportClient batchReportClient, AccessContractModel accessContractModel, Iterator<JsonNode> queryIterator,
        JsonLineWriter jsonLineWriter) throws ProcessingStatusException {

        final int tenantId = VitamThreadUtils.getVitamSession().getTenantId();

        // Associate every query entry with its index position
        Iterator<EntryWithIndex<JsonNode>> queryWithIndexIterator
            = new CountingIterator<>(queryIterator);

        // Group entries for bulk processing
        Iterator<List<EntryWithIndex<JsonNode>>> queriesBulkIterator =
            Iterators.partition(queryWithIndexIterator, batchSize);

        try (BulkSelectQueryParallelProcessor bulkSelectQueryParallelProcessor = new BulkSelectQueryParallelProcessor(
            PREPARE_BULK_ATOMIC_UPDATE_UNIT_LIST_PLUGIN_NAME, processId, tenantId,
            metadataClient, batchReportClient, internalActionKeysRetriever, accessContractModel, jsonLineWriter)) {

            // Process in thread pool. Any exception aborts execution
            AtomicBoolean fatalErrorOccurred = new AtomicBoolean(false);
            AtomicBoolean koErrorOccurred = new AtomicBoolean(false);
            ThreadPoolExecutor executor =
                ExecutorUtils.createScalableBatchExecutorService(threadPoolSize, threadPoolQueueSize);

            while (queriesBulkIterator.hasNext() && !fatalErrorOccurred.get()) {

                final List<EntryWithIndex<JsonNode>> bulkQueriesToProcess = queriesBulkIterator.next();
                executor.submit(() -> {

                    VitamThreadUtils.getVitamSession().setTenantId(tenantId);

                    if (fatalErrorOccurred.get() || koErrorOccurred.get()) {
                        throw new CancellationException("Job cancelled");
                    }

                    try {
                        bulkSelectQueryParallelProcessor.processBulkQueries(bulkQueriesToProcess);
                    } catch (InvalidParseOperationException | IllegalArgumentException | MetaDataDocumentSizeException | InvalidCreateOperationException e) {
                        koErrorOccurred.set(true);
                        LOGGER.error("An error occurred during bulk select query execution", e);
                    } catch (MetaDataExecutionException | MetaDataClientServerException | VitamClientInternalException | IOException | RuntimeException e) {
                        fatalErrorOccurred.set(true);
                        LOGGER.error("An unexpected error occurred during bulk select query execution", e);
                    }
                }, executor);
            }

            awaitExecutorTermination(executor);

            if (koErrorOccurred.get()) {
                throw new ProcessingStatusException(StatusCode.KO,
                    "One or more KO errors occurred during bulk select query execution");
            }

            if (fatalErrorOccurred.get()) {
                throw new ProcessingStatusException(StatusCode.FATAL,
                    "One or more FATAL errors occurred during bulk select query execution");
            }

            final ItemStatus itemStatus = new ItemStatus(PREPARE_BULK_ATOMIC_UPDATE_UNIT_LIST_PLUGIN_NAME);
            if (bulkSelectQueryParallelProcessor.getNbOKs() > 0) {
                itemStatus.increment(StatusCode.OK, bulkSelectQueryParallelProcessor.getNbOKs());
            }
            if (bulkSelectQueryParallelProcessor.getNbWarnings() > 0) {
                itemStatus.increment(StatusCode.WARNING, bulkSelectQueryParallelProcessor.getNbWarnings());
            }
            return itemStatus;

        } catch (VitamClientInternalException | IOException e) {
            throw new ProcessingStatusException(StatusCode.FATAL,
                "An error occurred during bulk select query execution", e);
        }
    }

    private void awaitExecutorTermination(ThreadPoolExecutor executor) throws ProcessingStatusException {
        try {
            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProcessingStatusException(StatusCode.FATAL,
                "Awaiting bulk atomic update jobs interrupted", e);
        }
    }
}