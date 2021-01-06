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
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.batch.report.client.BatchReportClient;
import fr.gouv.vitam.batch.report.model.ReportBody;
import fr.gouv.vitam.batch.report.model.ReportType;
import fr.gouv.vitam.batch.report.model.entry.BulkUpdateUnitMetadataReportEntry;
import fr.gouv.vitam.common.InternalActionKeysRetriever;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.database.utils.AccessContractRestrictionHelper;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import fr.gouv.vitam.worker.core.distribution.JsonLineWriter;
import fr.gouv.vitam.worker.core.utils.CountingIterator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Handles execution of bulk select queries
 * This class is stateful, and supports concurrent access to public methods.
 */
public class BulkSelectQueryParallelProcessor implements AutoCloseable {

    private static final String ORIGIN_QUERY_KEY = "originQuery";
    private static final String QUERY_INDEX_KEY = "queryIndex";
    private final String pluginName;
    private final String processId;
    private final int tenantId;
    private final MetaDataClient metadataClient;
    private final BatchReportClient batchReportClient;
    private final InternalActionKeysRetriever internalActionKeysRetriever;
    private final AccessContractModel accessContractModel;
    private final JsonLineWriter jsonLineWriter;

    private final List<BulkUpdateUnitMetadataReportEntry> bufferedReportEntries = new ArrayList<>();
    private final List<JsonLineModel> bufferedDistributionFileEntries = new ArrayList<>();
    private final AtomicInteger nbWarnings = new AtomicInteger(0);
    private final AtomicInteger nbOKs = new AtomicInteger(0);

    public BulkSelectQueryParallelProcessor(String pluginName, String processId, int tenantId,
        MetaDataClient metadataClient, BatchReportClient batchReportClient,
        InternalActionKeysRetriever internalActionKeysRetriever,
        AccessContractModel accessContractModel,
        JsonLineWriter jsonLineWriter) {
        this.pluginName = pluginName;
        this.processId = processId;
        this.tenantId = tenantId;
        this.metadataClient = metadataClient;
        this.batchReportClient = batchReportClient;
        this.internalActionKeysRetriever = internalActionKeysRetriever;
        this.accessContractModel = accessContractModel;
        this.jsonLineWriter = jsonLineWriter;
    }

    public void processBulkQueries(List<CountingIterator.EntryWithIndex<JsonNode>> bulkQueriesToProcess)
        throws MetaDataExecutionException, MetaDataDocumentSizeException, InvalidParseOperationException,
        MetaDataClientServerException, InvalidCreateOperationException, IOException, VitamClientInternalException {

        List<CountingIterator.EntryWithIndex<JsonNode>> validQueries =
            reportAndFilterInvalidQueries(bulkQueriesToProcess);

        List<JsonNode> executableQueries = transformQueries(accessContractModel, validQueries);

        List<List<String>> queriesResultUnitIds = bulkExecuteSelectQueries(metadataClient, executableQueries);

        handleResults(validQueries, queriesResultUnitIds);
    }

    private List<CountingIterator.EntryWithIndex<JsonNode>> reportAndFilterInvalidQueries(
        List<CountingIterator.EntryWithIndex<JsonNode>> bulkQueriesToProcess) throws VitamClientInternalException {
        List<CountingIterator.EntryWithIndex<JsonNode>> validQueries = new ArrayList<>();
        for (CountingIterator.EntryWithIndex<JsonNode> queryToProcess : bulkQueriesToProcess) {
            List<String> internalKeyFields = internalActionKeysRetriever.getInternalActionKeyFields(
                queryToProcess.getValue());
            if (!internalKeyFields.isEmpty()) {
                String message = String.format(BulkUpdateUnitReportKey.INVALID_DSL_QUERY.getMessage() + " : '%s'",
                    String.join(", ", internalKeyFields));
                BulkUpdateUnitMetadataReportEntry reportEntry =
                    createFailureReportEntry(tenantId, processId, queryToProcess,
                        BulkUpdateUnitReportKey.INVALID_DSL_QUERY, message);
                appendFailureReportEntry(reportEntry);
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

    private List<JsonNode> transformQueries(AccessContractModel accessContractModel,
        List<CountingIterator.EntryWithIndex<JsonNode>> bulkQueriesToProcess)
        throws InvalidParseOperationException, InvalidCreateOperationException {
        // Update queries : apply access contract restrictions, limit result size, add projections...
        List<JsonNode> executableQueries = new ArrayList<>();
        for (CountingIterator.EntryWithIndex<JsonNode> jsonNodeEntryWithIndex : bulkQueriesToProcess) {
            JsonNode query = jsonNodeEntryWithIndex.getValue();
            ObjectNode jsonNodes = computeModifiedSelectQuery(accessContractModel, query);
            executableQueries.add(jsonNodes);
        }
        return executableQueries;
    }

    /**
     * Create select DSL query from query in item and apply contract
     *
     * @param accessContract accessContract
     * @param originalQuery update query sent by client
     * @return Select query with apply access contract restrictions, result size limit, and projection set to #id
     * @throws InvalidParseOperationException query parsing error
     * @throws InvalidCreateOperationException error in application of contract
     */
    private ObjectNode computeModifiedSelectQuery(AccessContractModel accessContract, JsonNode originalQuery)
        throws InvalidParseOperationException, InvalidCreateOperationException {
        JsonNode securedQueryNode = AccessContractRestrictionHelper
            .applyAccessContractRestrictionForUnitForSelect(originalQuery, accessContract);
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
        List<List<String>> queriesResultUnitIds) throws VitamClientInternalException, IOException {
        for (int i = 0; i < bulkQueriesToProcess.size(); i++) {
            CountingIterator.EntryWithIndex<JsonNode> queryToProcess = bulkQueriesToProcess.get(i);
            List<String> unitIds = queriesResultUnitIds.get(i);

            int numberResults = unitIds.size();
            if (numberResults == 0) {
                appendFailureReportEntry(createFailureReportEntry(tenantId, processId,
                    queryToProcess, BulkUpdateUnitReportKey.UNIT_NOT_FOUND,
                    BulkUpdateUnitReportKey.UNIT_NOT_FOUND.getMessage()));
            } else if (numberResults >= 2) {
                appendFailureReportEntry(createFailureReportEntry(tenantId, processId,
                    queryToProcess, BulkUpdateUnitReportKey.TOO_MANY_UNITS_FOUND,
                    BulkUpdateUnitReportKey.TOO_MANY_UNITS_FOUND.getMessage()));
            } else {
                String unitId = unitIds.get(0);
                appendUnitToUpdate(unitId, queryToProcess.getValue(), queryToProcess.getIndex());
            }
        }
    }

    /**
     * Write unit info to update to distribution file.
     * Data is buffered to avoid IO locks.
     * Method is synchronized to handle concurrent invocations.
     *
     * @param unitId unit id to update
     * @param originalQuery query as provided by client
     * @param queryIndex 0-based index of the query provided by the client
     */
    private synchronized void appendUnitToUpdate(String unitId, JsonNode originalQuery, int queryIndex)
        throws IOException {
        bufferedDistributionFileEntries.add(createJsonLineEntry(unitId, originalQuery, queryIndex));
        nbOKs.incrementAndGet();

        // Flush buffered report entries to limit memory usage
        if (bufferedDistributionFileEntries.size() >= VitamConfiguration.getBatchSize()) {
            flushBufferedUnitsToDistributionFile();
        }
    }

    private JsonLineModel createJsonLineEntry(String unitId, JsonNode originalQuery, int queryIndex) {
        ObjectNode params = JsonHandler.createObjectNode();
        params.set(ORIGIN_QUERY_KEY, originalQuery);
        params.put(QUERY_INDEX_KEY, queryIndex);
        return new JsonLineModel(unitId, null, params);
    }

    private void flushBufferedUnitsToDistributionFile() throws IOException {
        try {
            for (JsonLineModel entry : bufferedDistributionFileEntries) {
                jsonLineWriter.addEntry(entry);
            }
        } finally {
            bufferedDistributionFileEntries.clear();
        }
    }

    private BulkUpdateUnitMetadataReportEntry createFailureReportEntry(int tenantId, String processId,
        CountingIterator.EntryWithIndex<JsonNode> queryToProcess, BulkUpdateUnitReportKey reportKey, String message) {
        return new BulkUpdateUnitMetadataReportEntry(
            tenantId,
            processId,
            Integer.toString(queryToProcess.getIndex()),
            JsonHandler.unprettyPrint(queryToProcess.getValue()),
            null,
            reportKey.name(),
            StatusCode.WARNING,
            String.format("%s.%s", pluginName, StatusCode.WARNING),
            message);
    }

    /**
     * Append report entries to be persisted to BatchReport server.
     * Data is buffered in memory, and writes are done in batch mode.
     * Method is synchronized to handle concurrent invocations.
     *
     * @param reportEntry report entry to persist
     */
    private synchronized void appendFailureReportEntry(BulkUpdateUnitMetadataReportEntry reportEntry)
        throws VitamClientInternalException {
        bufferedReportEntries.add(reportEntry);
        nbWarnings.incrementAndGet();

        // Flush buffered report entries to limit memory usage
        if (bufferedReportEntries.size() >= VitamConfiguration.getBatchSize()) {
            flushBufferedReportEntries();
        }
    }

    private void flushBufferedReportEntries() throws VitamClientInternalException {
        if (bufferedReportEntries.isEmpty()) {
            return;
        }
        try {
            ReportBody<BulkUpdateUnitMetadataReportEntry> reportBody = new ReportBody<>();
            reportBody.setProcessId(processId);
            reportBody.setReportType(ReportType.BULK_UPDATE_UNIT);
            reportBody.setEntries(new ArrayList<>(bufferedReportEntries));
            batchReportClient.appendReportEntries(reportBody);
        } finally {
            bufferedReportEntries.clear();
        }
    }

    public int getNbWarnings() {
        return nbWarnings.get();
    }

    public int getNbOKs() {
        return nbOKs.get();
    }

    @Override
    public synchronized void close() throws IOException, VitamClientInternalException {
        flushBufferedUnitsToDistributionFile();
        flushBufferedReportEntries();
    }
}
