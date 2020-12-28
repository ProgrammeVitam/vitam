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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterators;
import fr.gouv.vitam.batch.report.client.BatchReportClient;
import fr.gouv.vitam.batch.report.client.BatchReportClientFactory;
import fr.gouv.vitam.batch.report.model.ReportBody;
import fr.gouv.vitam.batch.report.model.ReportType;
import fr.gouv.vitam.batch.report.model.entry.BulkUpdateUnitMetadataReportEntry;
import fr.gouv.vitam.common.InternalActionKeysRetriever;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.database.utils.AccessContractRestrictionHelper;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.core.database.configuration.GlobalDatasDb;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import fr.gouv.vitam.worker.core.distribution.JsonLineWriter;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import org.apache.commons.collections4.CollectionUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Prepare execute execute each query in query.json by :<br>
 * - checking query validity (see MassUpdateCheck and add tests for _mgt) => if
 * KO detail in batch-report and not in distrib<br>
 * - adding limit 2 filter and projection "_id" <br>
 * - adding accessContract limits <br>
 * - executing the select query (in bulk of 8? metadata) <br>
 * - checking number of results : 1 result => OK/add to distribution, 0 or more
 * than 1 results => WARNING/add detail batch-report and not in distrib<br>
 * - saving the distrib file<br>
 *
 * TODO 7269 : add parallel execution <br>
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

    /**
     * METADATA_SELECT_BATCH_SIZE
     */
    private static final int METADATA_SELECT_BATCH_SIZE = 8;

    private final MetaDataClientFactory metaDataClientFactory;
    private final BatchReportClientFactory batchReportClientFactory;
    private final InternalActionKeysRetriever internalActionKeysRetriever;


    /**
     * TODO 7269 : Batch size
     */
    private final int batchSize;

    /**
     * Constructor.
     */
    public PrepareBulkAtomicUpdate() {
        this(MetaDataClientFactory.getInstance(), BatchReportClientFactory.getInstance(),
            new InternalActionKeysRetriever(), GlobalDatasDb.LIMIT_LOAD);
    }

    /**
     * Constructor.
     *
     * @param metaDataClientFactory
     * @param internalActionKeysRetriever
     * @param batchSize parallel batch size
     */
    @VisibleForTesting
    PrepareBulkAtomicUpdate(MetaDataClientFactory metaDataClientFactory,
        BatchReportClientFactory batchReportClientFactory, InternalActionKeysRetriever internalActionKeysRetriever,
        int batchSize) {
        this.metaDataClientFactory = metaDataClientFactory;
        this.batchReportClientFactory = batchReportClientFactory;
        this.internalActionKeysRetriever = internalActionKeysRetriever;
        this.batchSize = batchSize;
    }

    /**
     * Execute an action
     *
     * @param param {@link WorkerParameters}
     * @param handler the handlerIo
     * @return CompositeItemStatus:response contains a list of functional message
     * and status code
     * @throws ProcessingException if an error is encountered when executing the
     * action
     */
    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException {

        final ItemStatus itemStatus = new ItemStatus(PREPARE_BULK_ATOMIC_UPDATE_UNIT_LIST_PLUGIN_NAME);
        final int tenantId = VitamThreadUtils.getVitamSession().getTenantId();

        try (MetaDataClient metatadaClient = metaDataClientFactory.getClient();
            BatchReportClient batchReportClient = batchReportClientFactory.getClient()) {

            // Retrieve inputs
            JsonNode accessContractNode = handler.getJsonFromWorkspace(ACCESS_CONTRACT_NAME_IN);
            AccessContractModel accessContractModel = JsonHandler.getFromJsonNode(accessContractNode,
                AccessContractModel.class);
            JsonNode queryInNode = handler.getJsonFromWorkspace(QUERY_NAME_IN);
            ArrayNode queriesNodes = ((ArrayNode) queryInNode.get("queries"));


            // Create distrib file
            final String distribFileName = handler.getOutput(DISTRIBUTION_FILE_RANK).getPath();
            final File distribFile = handler.getNewLocalFile(distribFileName);

            try (JsonLineWriter jsonLineWriter = new JsonLineWriter(new FileOutputStream(distribFile))) {
                // START BULK
                Iterator<List<JsonNode>> queriesBulkIterator =
                    Iterators.partition(queriesNodes.iterator(), METADATA_SELECT_BATCH_SIZE);
                while (queriesBulkIterator.hasNext()) {
                    List<JsonNode> bulkQueriesToProcess = queriesBulkIterator.next();
                    executeBulk(param, itemStatus, tenantId, metatadaClient, batchReportClient, accessContractModel,
                        bulkQueriesToProcess, distribFile);
                }
                // END Bulk

            } catch (IOException | VitamRuntimeException | IllegalStateException e) {
                throw new ProcessingException("Could not generate and save file", e);
            }
            // move file to workspace
            handler.transferFileToWorkspace(distribFileName, distribFile, true, false);

            // set status OK
            itemStatus.increment(StatusCode.OK);

        } catch (BadRequestException e) {
            LOGGER.error(e);
            itemStatus.increment(StatusCode.KO);
        } catch (InvalidParseOperationException | ProcessingException e) {
            LOGGER.error(e);
            itemStatus.increment(StatusCode.FATAL);
        }
        return new ItemStatus(PREPARE_BULK_ATOMIC_UPDATE_UNIT_LIST_PLUGIN_NAME)
            .setItemsStatus(PREPARE_BULK_ATOMIC_UPDATE_UNIT_LIST_PLUGIN_NAME, itemStatus);
    }

    /**
     * Execute prepare plugin on a bulk of queries
     *
     * @param param plugin params
     * @param itemStatus plugin status
     * @param tenantId tenantId
     * @param metatadaClient metadata client
     * @param batchReportClient batch report client
     * @param accessContractModel access contract
     * @param queriesNodes list of queries in bulk
     * @param distribFile distribution file
     * @throws BadRequestException error in query
     * @throws ProcessingException clients errors
     */
    private void executeBulk(WorkerParameters param, final ItemStatus itemStatus, final int tenantId,
        MetaDataClient metatadaClient, BatchReportClient batchReportClient, AccessContractModel accessContractModel,
        List<JsonNode> queriesNodes,
        final File distribFile)
        throws BadRequestException, ProcessingException {

        try {

            BulkAtomicUpdateQueryPrepareBulk bulkItems = new BulkAtomicUpdateQueryPrepareBulk();
            queriesNodes.forEach(queryDetailNode -> {
                bulkItems.getItems().add(new BulkAtomicUpdateQueryPrepareItem(queryDetailNode));
            });

            // Verification of queries
            bulkItems.getItems().forEach(item -> validateQuery(item, param.getContainerName(), tenantId));
            // Generate modifies queries
            for (BulkAtomicUpdateQueryPrepareItem item : bulkItems.getItems()) {
                if (item.isValid()) {
                    computeModifiedQuery(item, accessContractModel);
                }
            }

            List<BulkAtomicUpdateQueryPrepareItem> validItems =
                bulkItems.getItems().stream().filter(item -> item.isValid()).collect(Collectors.toList());
            List<JsonNode> executableQueries =
                validItems.stream().map(item -> item.getModifiedQuery()).collect(Collectors.toList());
            List<RequestResponseOK<JsonNode>> queriesResponses = metatadaClient.selectUnitsBulk(executableQueries);


            for (int queryIndex = 0; queryIndex < executableQueries.size(); queryIndex++) {
                RequestResponseOK<JsonNode> queryResponse = queriesResponses.get(queryIndex);
                BulkAtomicUpdateQueryPrepareItem item = validItems.get(queryIndex);

                int numberResults = queryResponse.getResults().size();
                if (numberResults == 0) {
                    BulkUpdateUnitMetadataReportEntry entry = new BulkUpdateUnitMetadataReportEntry(
                        tenantId,
                        param.getContainerName(),
                        GUIDFactory.newGUID().getId(),
                        JsonHandler.unprettyPrint(item.getOriginalQuery()),
                        null,
                        BulkUpdateUnitReportKey.UNIT_NOT_FOUND.name(), StatusCode.WARNING,
                        String.format("%s.%s", PREPARE_BULK_ATOMIC_UPDATE_UNIT_LIST_PLUGIN_NAME, StatusCode.WARNING),
                        BulkUpdateUnitReportKey.UNIT_NOT_FOUND.getMessage());
                    item.setResult(entry);
                } else if (numberResults >= 2) {
                    BulkUpdateUnitMetadataReportEntry entry = new BulkUpdateUnitMetadataReportEntry(
                        tenantId,
                        param.getContainerName(),
                        GUIDFactory.newGUID().getId(),
                        JsonHandler.unprettyPrint(item.getOriginalQuery()),
                        null,
                        BulkUpdateUnitReportKey.TOO_MANY_UNITS_FOUND.name(), StatusCode.WARNING,
                        String.format("%s.%s", PREPARE_BULK_ATOMIC_UPDATE_UNIT_LIST_PLUGIN_NAME, StatusCode.WARNING),
                        BulkUpdateUnitReportKey.TOO_MANY_UNITS_FOUND.getMessage());
                    item.setResult(entry);
                } else {
                    item.setUnitId(queryResponse.getResults().get(0).get("#id").textValue());
                }
            }

            // Add valid in ditrib
            if (CollectionUtils.isNotEmpty(bulkItems.getValidItems())) {
                writeToDistributionFile(bulkItems.getValidItems(), distribFile);
                itemStatus.increment(StatusCode.OK, bulkItems.getValidItems().size());
            }

            // send report error in report
            if (CollectionUtils.isNotEmpty(bulkItems.getReportEntries())) {
                ReportBody<BulkUpdateUnitMetadataReportEntry> reportBody = new ReportBody<>();
                reportBody.setProcessId(param.getProcessId());
                reportBody.setReportType(ReportType.BULK_UPDATE_UNIT);
                reportBody.setEntries(bulkItems.getReportEntries());
                batchReportClient.appendReportEntries(reportBody);
                itemStatus.increment(StatusCode.WARNING, bulkItems.getReportEntries().size());
            }
        } catch (InvalidParseOperationException | IllegalArgumentException | MetaDataDocumentSizeException | InvalidCreateOperationException e) {
            throw new BadRequestException("Client error while executing select requests ", e);
        } catch (MetaDataExecutionException | MetaDataClientServerException | VitamClientInternalException e) {
            throw new ProcessingException("Server error while executing select requests ", e);
        }
    }

    /**
     * Append to the distribution file
     *
     * @param items bulk of queries
     * @param distribFile distribution file as à jsonLine
     * @throws ProcessingException
     */
    private void writeToDistributionFile(final List<BulkAtomicUpdateQueryPrepareItem> items, File distribFile)
        throws ProcessingException {

        boolean isEmpty = !(distribFile.exists() && distribFile.length() != 0);
        try (JsonLineWriter jsonLineWriter = new JsonLineWriter(new FileOutputStream(distribFile, true), isEmpty)) {
            items.forEach(item -> {
                try {
                    jsonLineWriter.addEntry(getJsonLineForItem(item));
                } catch (IOException e) {
                    throw new VitamRuntimeException(e);
                }
            });

        } catch (IOException | VitamRuntimeException | IllegalStateException e) {
            throw new ProcessingException("Could not generate and save file", e);
        }
    }

    private JsonLineModel getJsonLineForItem(BulkAtomicUpdateQueryPrepareItem item) {
        ObjectNode params = JsonHandler.createObjectNode();
        params.set("originQuery", item.getOriginalQuery());
        return new JsonLineModel(item.getUnitId(), null, params);
    }

    /**
     * Verify "_xx" fields not present in update
     *
     * @param item item containing query
     * @param containerName containerName
     * @param tenantId tenantId
     */
    private void validateQuery(BulkAtomicUpdateQueryPrepareItem item, String containerName, int tenantId) {
        List<String> internalKeyFields =
            internalActionKeysRetriever.getInternalActionKeyFields(item.getOriginalQuery());
        if (!internalKeyFields.isEmpty()) {
            String message = String.format(BulkUpdateUnitReportKey.INVALID_DSL_QUERY.getMessage() + " : '%s'",
                String.join(", ", internalKeyFields));
            BulkUpdateUnitMetadataReportEntry entry = new BulkUpdateUnitMetadataReportEntry(
                tenantId,
                containerName,
                GUIDFactory.newGUID().getId(),
                JsonHandler.unprettyPrint(item.getOriginalQuery()),
                null,
                BulkUpdateUnitReportKey.INVALID_DSL_QUERY.name(),
                StatusCode.WARNING,
                String.format("%s.%s", PREPARE_BULK_ATOMIC_UPDATE_UNIT_LIST_PLUGIN_NAME, StatusCode.WARNING),
                message);
            item.setResult(entry);
        }
    }

    /**
     * Create select DSL query from query in item and apply contract
     *
     * @param item query item
     * @param accessContract accessContract
     * @throws InvalidParseOperationException query parsing error
     * @throws InvalidCreateOperationException error in application of contract
     */
    private void computeModifiedQuery(BulkAtomicUpdateQueryPrepareItem item, AccessContractModel accessContract)
        throws InvalidParseOperationException, InvalidCreateOperationException {
        JsonNode securedQueryNode = AccessContractRestrictionHelper
            .applyAccessContractRestrictionForUnitForSelect(item.getOriginalQuery(), accessContract);
        SelectParserMultiple parser = new SelectParserMultiple();
        parser.parse(securedQueryNode);
        SelectMultiQuery multiQuery = parser.getRequest();
        multiQuery.getRoots().clear();
        // We set a limit at 2 results per request
        multiQuery.setLimitFilter(0, 2);
        // set projection to get only the id
        multiQuery.setProjection(JsonHandler.getFromString("{\"$fields\": { \"#id\": 1}}"));
        item.setModifiedQuery(multiQuery.getFinalSelect());

    }

}
