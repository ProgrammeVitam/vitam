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

package fr.gouv.vitam.metadata.common.bulkatomicupdate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Iterators;
import fr.gouv.vitam.common.InternalActionKeysRetriever;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.iterables.CountingIterator;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.thread.ExecutorUtils;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.client.MetaDataClient;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Handles execution of bulk select queries, in concurrent executors
 * This class is stateful, and supports concurrent access to public methods.
 */
public class BulkSelectQueryParallelProcessor {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(BulkSelectQueryParallelProcessor.class);

    private final MetaDataClient metadataClient;
    private final InternalActionKeysRetriever internalActionKeysRetriever;
    private final QueryRestrictionConverter queryRestrictionConverter;
    private final Consumer<BulkSelectQueryResultOK> successReporter;
    private final Consumer<BulkSelectQueryResultFailure> failureReporter;
    private final int threadPoolSize;
    private final int threadPoolQueueSize;
    private final int batchSize;
    private final AtomicInteger nbWarnings = new AtomicInteger(0);
    private final AtomicInteger nbOKs = new AtomicInteger(0);

    public BulkSelectQueryParallelProcessor(MetaDataClient metadataClient,
        InternalActionKeysRetriever internalActionKeysRetriever,
        int threadPoolSize, int threadPoolQueueSize, int batchSize,
        Consumer<BulkSelectQueryResultOK> successReporter,
        Consumer<BulkSelectQueryResultFailure> failureReporter,
        QueryRestrictionConverter queryRestrictionConverter) {
        this.metadataClient = metadataClient;
        this.internalActionKeysRetriever = internalActionKeysRetriever;
        this.queryRestrictionConverter = queryRestrictionConverter;
        this.successReporter = successReporter;
        this.failureReporter = failureReporter;
        this.threadPoolSize = threadPoolSize;
        this.threadPoolQueueSize = threadPoolQueueSize;
        this.batchSize = batchSize;
    }

    public void processQueries(Iterator<JsonNode> queryIterator) throws InvalidParseOperationException {
        final int tenantId = VitamThreadUtils.getVitamSession().getTenantId();
        String processId = VitamThreadUtils.getVitamSession().getRequestId();

        // Associate every query entry with its index position
        Iterator<CountingIterator.EntryWithIndex<JsonNode>> queryWithIndexIterator
            = new CountingIterator<>(queryIterator);

        // Group entries for bulk processing
        Iterator<List<CountingIterator.EntryWithIndex<JsonNode>>> queriesBulkIterator =
            Iterators.partition(queryWithIndexIterator, batchSize);

        // Process in thread pool. Any exception aborts execution
        AtomicBoolean fatalErrorOccurred = new AtomicBoolean(false);
        AtomicBoolean koErrorOccurred = new AtomicBoolean(false);
        ThreadPoolExecutor executor =
            ExecutorUtils.createScalableBatchExecutorService(threadPoolSize, threadPoolQueueSize);
        try {

            while (queriesBulkIterator.hasNext() && !fatalErrorOccurred.get()) {

                final List<CountingIterator.EntryWithIndex<JsonNode>> bulkQueriesToProcess = queriesBulkIterator.next();
                executor.submit(() -> {

                    VitamThreadUtils.getVitamSession().setTenantId(tenantId);
                    VitamThreadUtils.getVitamSession().setRequestId(processId);

                    if (fatalErrorOccurred.get() || koErrorOccurred.get()) {
                        throw new CancellationException("Job cancelled");
                    }

                    try {
                        processBulkQueries(bulkQueriesToProcess);
                    } catch (InvalidParseOperationException | IllegalArgumentException | MetaDataDocumentSizeException |
                             InvalidCreateOperationException e) {
                        koErrorOccurred.set(true);
                        LOGGER.error("An error occurred during bulk select query execution", e);
                    } catch (Exception e) {
                        fatalErrorOccurred.set(true);
                        LOGGER.error("An unexpected error occurred during bulk select query execution", e);
                    }
                }, executor);
            }

        } finally {
            awaitExecutorTermination(executor);
        }

        if (koErrorOccurred.get()) {
            throw new InvalidParseOperationException(
                "One or more KO errors occurred during bulk select query execution");
        }

        if (fatalErrorOccurred.get()) {
            throw new VitamRuntimeException("One or more FATAL errors occurred during bulk select query execution");
        }
    }

    private void processBulkQueries(List<CountingIterator.EntryWithIndex<JsonNode>> bulkQueriesToProcess)
        throws MetaDataExecutionException, MetaDataDocumentSizeException, InvalidParseOperationException,
        MetaDataClientServerException, InvalidCreateOperationException {

        List<CountingIterator.EntryWithIndex<JsonNode>> validQueries =
            reportAndFilterInvalidQueries(bulkQueriesToProcess);

        List<JsonNode> executableQueries = transformQueries(validQueries);

        List<List<String>> queriesResultUnitIds = bulkExecuteSelectQueries(metadataClient, executableQueries);

        handleResults(validQueries, queriesResultUnitIds);
    }

    private List<CountingIterator.EntryWithIndex<JsonNode>> reportAndFilterInvalidQueries(
        List<CountingIterator.EntryWithIndex<JsonNode>> bulkQueriesToProcess) {
        List<CountingIterator.EntryWithIndex<JsonNode>> validQueries = new ArrayList<>();
        for (CountingIterator.EntryWithIndex<JsonNode> queryToProcess : bulkQueriesToProcess) {
            List<String> internalKeyFields = internalActionKeysRetriever.getInternalActionKeyFields(
                queryToProcess.getValue());
            if (!internalKeyFields.isEmpty()) {
                String message = String.format(BulkUpdateUnitReportKey.INVALID_DSL_QUERY.getMessage() + " : '%s'",
                    String.join(", ", internalKeyFields));
                reportQueryError(queryToProcess, BulkUpdateUnitReportKey.INVALID_DSL_QUERY, message);
            } else {
                validQueries.add(queryToProcess);
            }
        }
        return validQueries;
    }

    private List<List<String>> bulkExecuteSelectQueries(MetaDataClient metadataClient,
        List<JsonNode> executableQueries)
        throws MetaDataExecutionException, MetaDataDocumentSizeException, InvalidParseOperationException,
        MetaDataClientServerException {

        // Submit queries
        List<RequestResponseOK<JsonNode>> queriesResponses =
            metadataClient.selectUnitsBulk(executableQueries);

        // Check result size
        if (queriesResponses.size() != executableQueries.size()) {
            throw new IllegalStateException(String.format(
                "Partial response for selectUnitsBulk. Expected %d. Got %d",
                executableQueries.size(), queriesResponses.size()));
        }

        // Parse ids
        return queriesResponses.stream()
            .map(RequestResponseOK::getResults)
            .map(resultSet -> resultSet.stream().map(this::parseUnitId).collect(Collectors.toList()))
            .collect(Collectors.toList());
    }

    private List<JsonNode> transformQueries(List<CountingIterator.EntryWithIndex<JsonNode>> bulkQueriesToProcess)
        throws InvalidParseOperationException, InvalidCreateOperationException {
        // Update queries : apply access contract restrictions, limit result size, add projections...
        List<JsonNode> executableQueries = new ArrayList<>();
        for (CountingIterator.EntryWithIndex<JsonNode> jsonNodeEntryWithIndex : bulkQueriesToProcess) {
            JsonNode query = jsonNodeEntryWithIndex.getValue();
            ObjectNode jsonNodes = computeModifiedSelectQuery(query);
            executableQueries.add(jsonNodes);
        }
        return executableQueries;
    }

    /**
     * Create select DSL query from query in item and apply contract
     *
     * @param originalQuery update query sent by client
     * @return Select query with apply query restrictions (eg. access contract), result size limit, and projection set to #id
     * @throws InvalidParseOperationException query parsing error
     * @throws InvalidCreateOperationException error in application of contract
     */
    private ObjectNode computeModifiedSelectQuery(JsonNode originalQuery)
        throws InvalidParseOperationException, InvalidCreateOperationException {
        JsonNode securedQueryNode = this.queryRestrictionConverter.convert(originalQuery);
        SelectParserMultiple parser = new SelectParserMultiple();
        parser.parse(securedQueryNode);
        SelectMultiQuery multiQuery = parser.getRequest();
        multiQuery.getRoots().clear();
        // We set a limit at 2 results per request
        multiQuery.setLimitFilter(0, 2);
        // set projection to get only the id
        multiQuery.addUsedProjection(VitamFieldsHelper.id());
        return multiQuery.getFinalSelect();
    }

    private String parseUnitId(JsonNode unitJson) {
        return unitJson.get(VitamFieldsHelper.id()).textValue();
    }

    private void handleResults(List<CountingIterator.EntryWithIndex<JsonNode>> bulkQueriesToProcess,
        List<List<String>> queriesResultUnitIds) {
        for (int i = 0; i < bulkQueriesToProcess.size(); i++) {
            CountingIterator.EntryWithIndex<JsonNode> queryToProcess = bulkQueriesToProcess.get(i);
            List<String> unitIds = queriesResultUnitIds.get(i);

            int numberResults = unitIds.size();
            if (numberResults == 0) {
                reportQueryError(queryToProcess, BulkUpdateUnitReportKey.UNIT_NOT_FOUND,
                    BulkUpdateUnitReportKey.UNIT_NOT_FOUND.getMessage());
            } else if (numberResults >= 2) {
                reportQueryError(queryToProcess, BulkUpdateUnitReportKey.TOO_MANY_UNITS_FOUND,
                    BulkUpdateUnitReportKey.TOO_MANY_UNITS_FOUND.getMessage());
            } else {
                String unitId = unitIds.get(0);
                reportQueryOK(unitId, queryToProcess.getValue(), queryToProcess.getIndex());
            }
        }
    }

    /**
     * synchronized for thread safety
     */
    private synchronized void reportQueryOK(String unitId, JsonNode originalQuery, int queryIndex) {
        nbOKs.incrementAndGet();
        this.successReporter.accept(new BulkSelectQueryResultOK(queryIndex, originalQuery, unitId));
    }

    /**
     * synchronized for thread safety
     */
    private synchronized void reportQueryError(
        CountingIterator.EntryWithIndex<JsonNode> queryToProcess, BulkUpdateUnitReportKey reportKey, String message) {
        nbWarnings.incrementAndGet();
        this.failureReporter.accept(
            new BulkSelectQueryResultFailure(queryToProcess.getIndex(), queryToProcess.getValue(), reportKey, message));
    }

    public int getNbWarnings() {
        return nbWarnings.get();
    }

    public int getNbOKs() {
        return nbOKs.get();
    }

    private void awaitExecutorTermination(ThreadPoolExecutor executor) {
        try {
            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new VitamRuntimeException("Awaiting bulk atomic update jobs interrupted", e);
        }
    }
}
