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
package fr.gouv.vitam.collect.internal.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.collect.common.dto.BulkAtomicUpdateResult;
import fr.gouv.vitam.collect.common.dto.BulkAtomicUpdateStatus;
import fr.gouv.vitam.collect.common.exception.CollectInternalException;
import fr.gouv.vitam.collect.common.exception.CollectInternalInvalidRequestException;
import fr.gouv.vitam.collect.internal.core.configuration.CollectInternalConfiguration;
import fr.gouv.vitam.collect.internal.core.repository.MetadataRepository;
import fr.gouv.vitam.common.InternalActionKeysRetriever;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.UpdateParserMultiple;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.metadata.api.model.UpdateUnit;
import fr.gouv.vitam.metadata.api.utils.BulkAtomicUpdateModelUtils;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.common.bulkatomicupdate.BulkSelectQueryParallelProcessor;
import fr.gouv.vitam.metadata.common.bulkatomicupdate.BulkSelectQueryResultFailure;
import fr.gouv.vitam.metadata.common.bulkatomicupdate.BulkSelectQueryResultOK;
import fr.gouv.vitam.metadata.common.bulkatomicupdate.QueryRestrictionConverter;
import org.apache.commons.collections4.ListUtils;

import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static fr.gouv.vitam.collect.internal.core.helpers.MetadataHelper.applyTransactionToQuery;

@ThreadSafe
public class BulkAtomicUpdateMetadataService {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(BulkAtomicUpdateMetadataService.class);

    private final MetadataRepository metadataRepository;
    private final MetaDataClientFactory metadataClientFactory;
    private final int threadPoolSize;
    private final int threadPoolQueueSize;
    private final int batchSize;

    public BulkAtomicUpdateMetadataService(MetadataRepository metadataRepository,
        MetaDataClientFactory metadataClientFactory, CollectInternalConfiguration configuration) {
        this.metadataRepository = metadataRepository;
        this.metadataClientFactory = metadataClientFactory;
        threadPoolSize = configuration.getBulkAtomicUpdateThreadPoolSize();
        threadPoolQueueSize = configuration.getBulkAtomicUpdateThreadPoolQueueSize();
        batchSize = configuration.getBulkAtomicUpdateBatchSize();
    }

    public List<BulkAtomicUpdateResult> bulkAtomicUpdateUnits(String transactionId, ArrayNode queries,
        boolean allowInternalFieldsUpdate)
        throws CollectInternalException {

        try {

            BulkAtomicUpdateReportAppender reportAppender = new BulkAtomicUpdateReportAppender();

            List<BulkSelectQueryResultOK> selectQueryResults
                = selectUnitIdsPerUpdateQueries(transactionId, reportAppender, queries, allowInternalFieldsUpdate);

            processUpdate(selectQueryResults, reportAppender);

            return reportAppender.exportResults();

        } catch (InvalidParseOperationException e) {
            throw new CollectInternalInvalidRequestException(e);
        }
    }

    public void checkThreshold(JsonNode updateQueriesJson)
        throws CollectInternalInvalidRequestException {
        Long queryThreshold = BulkAtomicUpdateModelUtils.getQueryThreshold(updateQueriesJson);
        long total = BulkAtomicUpdateModelUtils.queryCount(updateQueriesJson);
        long threshold = queryThreshold != null ? queryThreshold : VitamConfiguration.getQueriesThreshold();

        if (total > threshold) {
            LOGGER.error("Too many update queries. Platform threshold={}, queryThreshold={}, actual={}",
                VitamConfiguration.getQueriesThreshold(), queryThreshold, total);
            throw new CollectInternalInvalidRequestException("Too many update queries. Threshold exceeded.");
        }
    }

    private List<BulkSelectQueryResultOK> selectUnitIdsPerUpdateQueries(
        String transactionId,
        BulkAtomicUpdateReportAppender reportAppender,
        ArrayNode queries,
        boolean allowInternalFieldsUpdate) throws CollectInternalInvalidRequestException {

        List<BulkSelectQueryResultOK> selectUnitIdsResults = new ArrayList<>();

        try (MetaDataClient metadataClient = metadataClientFactory.getClient()) {
            BulkSelectQueryParallelProcessor bulkSelectQueryParallelProcessor =
                new BulkSelectQueryParallelProcessor(
                    metadataClient, new InternalActionKeysRetriever(),
                    threadPoolSize, threadPoolQueueSize, batchSize,
                    selectUnitIdsResults::add,
                    createFailureResultReportAppender(reportAppender),
                    createTransactionIdRestrictionConverter(transactionId),
                    allowInternalFieldsUpdate);
            bulkSelectQueryParallelProcessor.processQueries(queries.iterator());

            return selectUnitIdsResults;

        } catch (InvalidParseOperationException e) {
            throw new CollectInternalInvalidRequestException("Cannot process bulk atomic update. Invalid request", e);
        }
    }

    private static Consumer<BulkSelectQueryResultFailure> createFailureResultReportAppender(
        BulkAtomicUpdateReportAppender reportAppender) {
        return (result) -> reportAppender.append(
            result.getQueryIndex(),
            new BulkAtomicUpdateResult(BulkAtomicUpdateStatus.KO, null, result.getMessage())
        );
    }

    private static QueryRestrictionConverter createTransactionIdRestrictionConverter(String transactionId) {
        return (JsonNode userQuery) -> {
            SelectParserMultiple parser = new SelectParserMultiple();
            parser.parse(userQuery);
            applyTransactionToQuery(transactionId, parser.getRequest());
            return parser.getRequest().getFinalSelect();
        };
    }


    private void processUpdate(List<BulkSelectQueryResultOK> selectQueryResults,
        BulkAtomicUpdateReportAppender reportAppender) throws CollectInternalException, InvalidParseOperationException {

        List<List<BulkSelectQueryResultOK>> selectQueryResultBulks =
            ListUtils.partition(selectQueryResults, batchSize);

        for (List<BulkSelectQueryResultOK> selectQueryResultBulk : selectQueryResultBulks) {
            processUpdateBatch(selectQueryResultBulk, reportAppender);
        }
    }

    private void processUpdateBatch(List<BulkSelectQueryResultOK> selectQueryResults,
        BulkAtomicUpdateReportAppender reportAppender)
        throws InvalidParseOperationException, CollectInternalException {

        List<JsonNode> updateQueries = new ArrayList<>();
        for (BulkSelectQueryResultOK selectQueryResult : selectQueryResults) {
            UpdateParserMultiple updateParserMultiple = new UpdateParserMultiple();
            updateParserMultiple.parse(selectQueryResult.getQuery());
            updateParserMultiple.getRequest().resetRoots();
            updateParserMultiple.getRequest().resetQueries();
            updateParserMultiple.getRequest().addRoots(selectQueryResult.getUnitId());
            ObjectNode updateQuery = updateParserMultiple.getRequest().getFinalUpdate();
            updateQueries.add(updateQuery);
        }

        RequestResponseOK<JsonNode> atomicBulkResults =
            (RequestResponseOK<JsonNode>) metadataRepository.atomicBulkUpdate(updateQueries);

        if (atomicBulkResults.getResults().size() != updateQueries.size()) {
            LOGGER.error("Invalid response size for " + JsonHandler.unprettyPrint(updateQueries) + ", got : " +
                JsonHandler.unprettyPrint(atomicBulkResults));
            throw new IllegalStateException("Expected " + updateQueries.size() + " update results, got " +
                atomicBulkResults.getResults().size());
        }

        List<JsonNode> results = atomicBulkResults.getResults();
        for (int i = 0; i < results.size(); i++) {
            JsonNode result = results.get(i);
            BulkSelectQueryResultOK queryToProcess = selectQueryResults.get(i);
            RequestResponseOK<UpdateUnit> responseOK =
                RequestResponseOK.getFromJsonNode(result, UpdateUnit.class);

            UpdateUnit unitUpdateStatus = responseOK.getResults().get(0);
            if (unitUpdateStatus == null) {
                throw new IllegalStateException("Missing unit update status");
            }

            StatusCode status = unitUpdateStatus.getStatus();
            if (StatusCode.OK == status) {
                LOGGER.debug("Unit " + unitUpdateStatus.getUnitId() + " updated successfully !\n" +
                    unitUpdateStatus.getDiff());
                reportAppender.append(queryToProcess.getQueryIndex(), new BulkAtomicUpdateResult(
                    BulkAtomicUpdateStatus.OK, unitUpdateStatus.getUnitId(), null));
            } else {
                LOGGER.debug("Unit " + unitUpdateStatus.getUnitId() + " update failed with message: " +
                    unitUpdateStatus.getKey() + " - " + unitUpdateStatus.getMessage());
                reportAppender.append(queryToProcess.getQueryIndex(), new BulkAtomicUpdateResult(
                    BulkAtomicUpdateStatus.KO, unitUpdateStatus.getUnitId(), unitUpdateStatus.getMessage()));
            }
        }
    }
}
