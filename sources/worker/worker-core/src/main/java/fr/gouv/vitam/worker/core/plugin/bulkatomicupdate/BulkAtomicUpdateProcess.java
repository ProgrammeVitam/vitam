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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterators;
import fr.gouv.vitam.batch.report.client.BatchReportClient;
import fr.gouv.vitam.batch.report.client.BatchReportClientFactory;
import fr.gouv.vitam.batch.report.model.ReportBody;
import fr.gouv.vitam.batch.report.model.ReportType;
import fr.gouv.vitam.batch.report.model.entry.BulkUpdateUnitMetadataReportEntry;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.database.parser.request.multiple.UpdateParserMultiple;
import fr.gouv.vitam.common.database.utils.MetadataDocumentHelper;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InvalidGuidOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.CanonicalJsonFormatter;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.LifeCycleStatusCode;
import fr.gouv.vitam.common.model.MetadataStorageHelper;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.VitamSession;
import fr.gouv.vitam.common.model.logbook.LogbookLifecycle;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.core.model.UpdateUnit;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.plugin.StoreMetadataObjectActionHandler;
import fr.gouv.vitam.worker.core.utils.PluginHelper.EventDetails;
import fr.gouv.vitam.workspace.api.exception.WorkspaceClientServerException;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static fr.gouv.vitam.common.model.IngestWorkflowConstants.ARCHIVE_UNIT_FOLDER;
import static fr.gouv.vitam.common.model.StatusCode.FATAL;
import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.metadata.core.model.UpdateUnitKey.UNIT_METADATA_NO_CHANGES;
import static fr.gouv.vitam.storage.engine.common.model.DataCategory.UNIT;
import static fr.gouv.vitam.worker.core.plugin.bulkatomicupdate.BulkUpdateUnitReportKey.ERROR_METADATA_UPDATE;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;

/**
 * Update from distribution :<br>
 * - execute update (bulk metadata)<br>
 * - compute results : if not updated (because nothing to do) => WARNING/ add
 * batch-report line, if KO/ add batch-report line, if OK to the next<br>
 * - in case of OK update and store the lfc of the unit with diff and add to
 * batch report<br>
 *
 */
public class BulkAtomicUpdateProcess extends StoreMetadataObjectActionHandler {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ActionHandler.class);

    private static final String UNIT_METADATA_UPDATE = "UNIT_METADATA_UPDATE";
    public static final String BULK_ATOMIC_UPDATE_UNITS_PLUGIN_NAME = "BULK_ATOMIC_UPDATE_UNITS";
    private static final String ORIGINAL_QUERY_ROOT_KEY = "originQuery";

    /**
     * METADATA_UPDATE_BATCH_SIZE
     */
    private static final int METADATA_UPDATE_BATCH_SIZE = 8;

    private final MetaDataClientFactory metaDataClientFactory;
    private final LogbookLifeCyclesClientFactory lfcClientFactory;
    private final StorageClientFactory storageClientFactory;
    private final BatchReportClientFactory batchReportClientFactory;

    public BulkAtomicUpdateProcess() {
        this(
            MetaDataClientFactory.getInstance(),
            LogbookLifeCyclesClientFactory.getInstance(),
            StorageClientFactory.getInstance(),
            BatchReportClientFactory.getInstance()
        );
    }

    @VisibleForTesting
    public BulkAtomicUpdateProcess(MetaDataClientFactory metaDataClientFactory, LogbookLifeCyclesClientFactory lfcClientFactory,
                                   StorageClientFactory storageClientFactory, BatchReportClientFactory batchReportClientFactory) {
        super(storageClientFactory);
        this.metaDataClientFactory = metaDataClientFactory;
        this.lfcClientFactory = lfcClientFactory;
        this.storageClientFactory = storageClientFactory;
        this.batchReportClientFactory = batchReportClientFactory;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler)
        throws ProcessingException {
        throw new IllegalStateException("UnsupportedOperation");
    }

    @Override
    public List<ItemStatus> executeList(WorkerParameters workerParameters, HandlerIO handler) throws ProcessingException {
        try (MetaDataClient mdClient = metaDataClientFactory.getClient();
            LogbookLifeCyclesClient lfcClient = lfcClientFactory.getClient();
            StorageClient storageClient = storageClientFactory.getClient();
            BatchReportClient batchReportClient = batchReportClientFactory.getClient()) {

            Iterator<List<String>> unitsIterator = Iterators.partition(workerParameters.getObjectNameList().iterator(), METADATA_UPDATE_BATCH_SIZE);
            List<ItemStatus> itemsStatus = new ArrayList<>();

            int batchOffset = 0;
            while (unitsIterator.hasNext()) {
                List<String> bulkUnits = unitsIterator.next();
                itemsStatus.addAll(executeBulk(workerParameters, handler, mdClient, lfcClient, storageClient, batchReportClient, bulkUnits, batchOffset));
                batchOffset += METADATA_UPDATE_BATCH_SIZE;
            }

            return itemsStatus;
        } catch (BadRequestException e) {
            LOGGER.error("Client error while executing update requests", e);
            throw new ProcessingException(e);
        }
    }

    private List<ItemStatus> executeBulk(WorkerParameters workerParameters, HandlerIO handler, MetaDataClient mdClient, LogbookLifeCyclesClient lfcClient,
                             StorageClient storageClient, BatchReportClient batchReportClient, List<String> bulkUnits, int batchOffset)
            throws BadRequestException, ProcessingException {

        try {
            // Retrieve each unitId, and each query, from params
            BulkAtomicUpdateQueryProcessBulk processBulk = new BulkAtomicUpdateQueryProcessBulk();
            List<JsonNode> queries = workerParameters.getObjectMetadataList();
    
            // Associate each unitId with its query
            for(int unitIndex = 0; unitIndex < bulkUnits.size(); unitIndex++) {
                JsonNode query = queries.get(batchOffset + unitIndex).get(ORIGINAL_QUERY_ROOT_KEY);
                BulkAtomicUpdateQueryProcessItem item = new BulkAtomicUpdateQueryProcessItem(bulkUnits.get(unitIndex), query);
                // We replace the query node from the request by a roots node
                generateUpdateQuery(item);
                processBulk.getItems().add(item);
            }
    
            // Call to the metadata client for the update
            RequestResponse<JsonNode> requestResponse = mdClient.atomicUpdateBulk(processBulk.getItemsFinalQuery());
    
            // Analyze response
            if (requestResponse.isOk()) {
                List<JsonNode> itemResults = ((RequestResponseOK<JsonNode>) requestResponse).getResults();
                for(int resultIndex = 0; resultIndex < itemResults.size(); resultIndex++) {
                    checkUnitUpdateResponse(workerParameters, handler, mdClient, lfcClient,
                            storageClient, processBulk.getItems().get(resultIndex), itemResults.get(resultIndex));
                }
            } else {
                throw new ProcessingException("Error when trying to update units.");
            }
    
            // Create ReportBody from all entries
            ReportBody<BulkUpdateUnitMetadataReportEntry> reportBody = new ReportBody<>();
            reportBody.setProcessId(workerParameters.getProcessId());
            reportBody.setReportType(ReportType.BULK_UPDATE_UNIT);
            reportBody.setEntries(processBulk.getReportEntries());
    
            if (!reportBody.getEntries().isEmpty()) {
                batchReportClient.appendReportEntries(reportBody);
            }
    
            return processBulk.getItemsStatus();
        } catch (InvalidParseOperationException | IllegalArgumentException | MetaDataDocumentSizeException | InvalidCreateOperationException e) {
            throw new BadRequestException("Client error while executing select requests ", e);
        } catch (MetaDataExecutionException | MetaDataNotFoundException | MetaDataClientServerException | VitamClientInternalException  e) {
            throw new ProcessingException("Server error while executing select requests ", e);
        }
    }

    private boolean lfcAlreadyWrittenInMongo(LogbookLifeCyclesClient lfcClient, String unitId, String currentOperationId) throws VitamException {
        JsonNode lfc = lfcClient.getRawUnitLifeCycleById(unitId);
        LogbookLifecycle unitLFC =  JsonHandler.getFromJsonNode(lfc, LogbookLifecycle.class);
        return unitLFC.getEvents().stream().anyMatch(e -> e.getEvIdProc().equals(currentOperationId));
    }

    private void writeLfcToMongo(LogbookLifeCyclesClient lfcClient, WorkerParameters param, String unitId, String diff)
        throws LogbookClientNotFoundException, LogbookClientBadRequestException, LogbookClientServerException,
            InvalidParseOperationException, InvalidGuidOperationException {

        LogbookLifeCycleParameters logbookLfcParam = LogbookParameterHelper.newLogbookLifeCycleUnitParameters(
                GUIDFactory.newEventGUID(ParameterHelper.getTenantParameter()),
                VitamLogbookMessages.getEventTypeLfc(UNIT_METADATA_UPDATE),
                GUIDReader.getGUID(param.getContainerName()),
                param.getLogbookTypeProcess(),
                OK,
                VitamLogbookMessages.getOutcomeDetailLfc(UNIT_METADATA_UPDATE, OK),
                VitamLogbookMessages.getCodeLfc(UNIT_METADATA_UPDATE, OK),
                GUIDReader.getGUID(unitId)
        );

        logbookLfcParam.putParameterValue(LogbookParameterName.eventDetailData, getEvDetDataForDiff(diff));
        lfcClient.update(logbookLfcParam, LifeCycleStatusCode.LIFE_CYCLE_COMMITTED);
    }

    private String getEvDetDataForDiff(String diff) throws InvalidParseOperationException {
        if (diff == null) {
            return "";
        }

        ObjectNode diffObject = JsonHandler.createObjectNode();
        diffObject.put("diff", diff);
        return JsonHandler.writeAsString(diffObject);
    }

    private void storeUnitAndLfcToOffer(MetaDataClient mdClient, LogbookLifeCyclesClient lfcClient,
                                        StorageClient storageClient, HandlerIO handler, WorkerParameters params,
                                        String guid, String fileName) throws VitamException {
        // get metadata
        JsonNode unit = selectMetadataDocumentRawById(guid, UNIT, mdClient);
        String strategyId = MetadataDocumentHelper.getStrategyIdFromRawUnitOrGot(unit);
        
        MetadataDocumentHelper.removeComputedFieldsFromUnit(unit);

        // get lfc
        JsonNode lfc = getRawLogbookLifeCycleById(guid, UNIT, lfcClient);

        // create file for storage (in workspace or temp or memory)
        JsonNode docWithLfc = MetadataStorageHelper.getUnitWithLFC(unit, lfc);

        // transfer json to workspace
        try {
            InputStream is = CanonicalJsonFormatter.serialize(docWithLfc);
            handler.transferInputStreamToWorkspace(ARCHIVE_UNIT_FOLDER + "/" + fileName, is, null, false);
        } catch (ProcessingException e) {
            LOGGER.error(params.getObjectName(), e);
            throw new WorkspaceClientServerException(e);
        }

        // call storage (save in offers)
        String uri = ARCHIVE_UNIT_FOLDER + File.separator + fileName;
        ObjectDescription description = new ObjectDescription(UNIT, params.getContainerName(), fileName, uri);

        // store metadata object from workspace and set itemStatus
        storageClient.storeFileFromWorkspace(strategyId, description.getType(), description.getObjectName(), description);
    }

    private void generateUpdateQuery(BulkAtomicUpdateQueryProcessItem item) throws InvalidParseOperationException,
            InvalidCreateOperationException {
        // parse multi query
        UpdateParserMultiple parser = new UpdateParserMultiple();
        parser.parse(item.getOriginalQuery());

        UpdateMultiQuery multiQuery = parser.getRequest();
        // Add operationID to #operations
        multiQuery.addActions(UpdateActionHelper.push(
                VitamFieldsHelper.operations(), VitamThreadUtils.getVitamSession().getRequestId()));
        // remove search part (useless)
        multiQuery.resetQueries();
        // set the unit to update
        multiQuery.resetRoots().addRoots(item.getUnitId());

        item.setFinalQuery(multiQuery.getFinalUpdate());
    }

    private void buildReport(WorkerParameters workerParameters, String key, StatusCode status, String message,
                             BulkAtomicUpdateQueryProcessItem item) {
        // Each result is sent to the batch report
        VitamSession vitamSession = VitamThreadUtils.getVitamSession();
        BulkUpdateUnitMetadataReportEntry entry = new BulkUpdateUnitMetadataReportEntry(
                vitamSession.getTenantId(),
                workerParameters.getContainerName(),
                GUIDFactory.newGUID().getId(),
                JsonHandler.unprettyPrint(item.getOriginalQuery()),
                item.getUnitId(),
                key,
                status,
                String.format("%s.%s", BULK_ATOMIC_UPDATE_UNITS_PLUGIN_NAME, status),
                message
        );
        item.setReportEntry(entry);
    }

    private void checkUnitUpdateResponse(WorkerParameters workerParameters, HandlerIO handler,
                                               MetaDataClient mdClient, LogbookLifeCyclesClient lfcClient,
                                               StorageClient storageClient, BulkAtomicUpdateQueryProcessItem item,
                                               JsonNode updateResult) throws InvalidParseOperationException {
        // Analyze of each atomic update operation response
        if(RequestResponse.isRequestResponseOk(updateResult)) {
            RequestResponseOK<UpdateUnit> responseOK = RequestResponseOK.getFromJsonNode(updateResult, UpdateUnit.class);
            // There is only one change per request
            ItemStatus itemStatus = postUpdate(workerParameters, handler, mdClient, lfcClient, storageClient, item, responseOK.getFirstResult());
            item.setStatus(itemStatus);
        } else {
            VitamError error = VitamError.getFromJsonNode(updateResult);
            buildReport(workerParameters,ERROR_METADATA_UPDATE.name(), KO, error.getDescription(), item);
            item.setStatus(buildItemStatus(BULK_ATOMIC_UPDATE_UNITS_PLUGIN_NAME, KO, EventDetails.of(error.getDescription())));
        }
    }

    private ItemStatus postUpdate(WorkerParameters workerParameters, HandlerIO handler, MetaDataClient mdClient,
                                  LogbookLifeCyclesClient lfcClient, StorageClient storageClient, BulkAtomicUpdateQueryProcessItem item, UpdateUnit unitNode) {
        String unitId = unitNode.getUnitId();
        String key = unitNode.getKey().name();
        String statusAsString = unitNode.getStatus().name();
        StatusCode status = StatusCode.valueOf(statusAsString);
        String message = unitNode.getMessage();

        String diff = unitNode.getDiff();

        if (!KO.equals(status) && !FATAL.equals(status) && !OK.equals(status)) {
            throw new VitamRuntimeException(String.format("Status must be of type KO, FATAL or OK here '%s'.", status));
        }

        buildReport(workerParameters, key, status, message, item);

        if (KO.equals(status) || FATAL.equals(status)) {
            return buildItemStatus(BULK_ATOMIC_UPDATE_UNITS_PLUGIN_NAME, status, EventDetails.of(message));
        }

        //TODO 7269 : Check when item did not change after update (not the UNIT_METADATA_NO_CHANGES which is idempotence)

        if (UNIT_METADATA_NO_CHANGES.name().equals(key)) {
            try {
                if (lfcAlreadyWrittenInMongo(lfcClient, unitId, workerParameters.getContainerName())) {
                    LOGGER.warn(String.format("There is no changes on the unit '%s', and LFC already written in mongo, this unit will be save in offer.", unitId));
                    storeUnitAndLfcToOffer(mdClient, lfcClient, storageClient, handler, workerParameters, unitId, unitId + ".json");
                    return buildItemStatus(BULK_ATOMIC_UPDATE_UNITS_PLUGIN_NAME, OK, EventDetails.of("Bulk atomic update OK"));
                }
            } catch (VitamException e) {
                LOGGER.error(e);
                return buildItemStatus(BULK_ATOMIC_UPDATE_UNITS_PLUGIN_NAME, FATAL, EventDetails.of(String.format("Error while storing UNIT with LFC '%s'.", e.getMessage())));
            }
        }

        try {
            writeLfcToMongo(lfcClient, workerParameters, unitId, diff);
        } catch (LogbookClientServerException | LogbookClientNotFoundException | InvalidParseOperationException | InvalidGuidOperationException e) {
            LOGGER.error(e);
            return buildItemStatus(BULK_ATOMIC_UPDATE_UNITS_PLUGIN_NAME, FATAL, EventDetails.of(String.format("Error '%s' while updating UNIT LFC.", e.getMessage())));
        } catch (LogbookClientBadRequestException e) {
            LOGGER.error(e);
            return buildItemStatus(BULK_ATOMIC_UPDATE_UNITS_PLUGIN_NAME, KO, EventDetails.of(String.format("Error '%s' while updating UNIT LFC.", e.getMessage())));
        }

        try {
            storeUnitAndLfcToOffer(mdClient, lfcClient, storageClient, handler, workerParameters, unitId, unitId + ".json");
        } catch (VitamException e) {
            LOGGER.error(e);
            return buildItemStatus(BULK_ATOMIC_UPDATE_UNITS_PLUGIN_NAME, FATAL, EventDetails.of(String.format("Error while storing UNIT with LFC '%s'.", e.getMessage())));
        }
        return buildItemStatus(BULK_ATOMIC_UPDATE_UNITS_PLUGIN_NAME, OK, EventDetails.of("Bulk atomic update OK"));
    }
}
