/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.worker.core.plugin.massprocessing.description;

import static fr.gouv.vitam.common.model.StatusCode.FATAL;
import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.common.model.StatusCode.WARNING;
import static fr.gouv.vitam.metadata.core.model.UpdateUnit.DIFF;
import static fr.gouv.vitam.metadata.core.model.UpdateUnit.ID;
import static fr.gouv.vitam.metadata.core.model.UpdateUnit.KEY;
import static fr.gouv.vitam.metadata.core.model.UpdateUnit.MESSAGE;
import static fr.gouv.vitam.metadata.core.model.UpdateUnit.STATUS;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;

import fr.gouv.vitam.batch.report.client.BatchReportClient;
import fr.gouv.vitam.batch.report.client.BatchReportClientFactory;
import fr.gouv.vitam.batch.report.model.ReportBody;
import fr.gouv.vitam.batch.report.model.ReportType;
import fr.gouv.vitam.batch.report.model.entry.UpdateUnitMetadataReportEntry;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.database.parser.request.multiple.UpdateParserMultiple;
import fr.gouv.vitam.common.database.utils.MetadataDocumentHelper;
import fr.gouv.vitam.common.exception.InvalidGuidOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.CanonicalJsonFormatter;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.IngestWorkflowConstants;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.LifeCycleStatusCode;
import fr.gouv.vitam.common.model.MetadataStorageHelper;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.VitamSession;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.plugin.StoreMetadataObjectActionHandler;
import fr.gouv.vitam.worker.core.utils.PluginHelper.EventDetails;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.api.exception.WorkspaceClientServerException;

public class MassUpdateUnitsProcess extends StoreMetadataObjectActionHandler {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ActionHandler.class);

    public static final String MASS_UPDATE_UNITS = "MASS_UPDATE_UNITS";
    private static final String UNIT_METADATA_UPDATE = "UNIT_METADATA_UPDATE";
    private static final String JSON = ".json";

    private MetaDataClientFactory metaDataClientFactory;
    private LogbookLifeCyclesClientFactory lfcClientFactory;
    private StorageClientFactory storageClientFactory;
    private BatchReportClientFactory batchReportClientFactory;

    public MassUpdateUnitsProcess() {
        this(MetaDataClientFactory.getInstance(), LogbookLifeCyclesClientFactory.getInstance(),
            StorageClientFactory.getInstance(),
                BatchReportClientFactory.getInstance());
    }

    @VisibleForTesting
    public MassUpdateUnitsProcess(MetaDataClientFactory metaDataClientFactory, LogbookLifeCyclesClientFactory lfcClientFactory,
        StorageClientFactory storageClientFactory, BatchReportClientFactory batchReportClientFactory) {
        super(storageClientFactory);
        this.metaDataClientFactory = metaDataClientFactory;
        this.lfcClientFactory = lfcClientFactory;
        this.storageClientFactory = storageClientFactory;
        this.batchReportClientFactory = batchReportClientFactory;
    }

    /**
     * Execute an action
     * @param param {@link WorkerParameters}
     * @param handler the handlerIo
     * @return CompositeItemStatus:response contains a list of functional message and status code
     * @throws ProcessingException if an error is encountered when executing the action
     * @throws ContentAddressableStorageServerException if a storage exception is encountered when executing the action
     */
    @Override public ItemStatus execute(WorkerParameters param, HandlerIO handler)
        throws ProcessingException, ContentAddressableStorageServerException {
        throw new IllegalStateException("UnsupporedOperation");
    }

    @Override
    public List<ItemStatus> executeList(WorkerParameters workerParameters, HandlerIO handler) throws ProcessingException {
        try (MetaDataClient mdClient = metaDataClientFactory.getClient();
            LogbookLifeCyclesClient lfcClient = lfcClientFactory.getClient();
            StorageClient storageClient = storageClientFactory.getClient();
            BatchReportClient batchReportClient = batchReportClientFactory.getClient()) {

            // get initial query string
            JsonNode queryNode = handler.getJsonFromWorkspace("query.json");

            // parse multi query
            UpdateParserMultiple parser = new UpdateParserMultiple();
            parser.parse(queryNode);

            // Add operationID to #operations
            parser.getRequest().addActions(UpdateActionHelper.push(
                VitamFieldsHelper.operations(), VitamThreadUtils.getVitamSession().getRequestId()));

            UpdateMultiQuery multiQuery = parser.getRequest();

            // remove search part (useless)
            multiQuery.resetQueries();

            // set the units to update
            List<String> units = workerParameters.getObjectNameList();
            multiQuery.resetRoots().addRoots(units.toArray(new String[0]));

            // call update BULK service
            RequestResponse<JsonNode> requestResponse = mdClient.updateUnitBulk(multiQuery.getFinalUpdate());

            // Prepare rapport
            List<UpdateUnitMetadataReportEntry> entries = new ArrayList<>();
            if (requestResponse != null && requestResponse.isOk()) {
                List<ItemStatus> itemStatuses = ((RequestResponseOK<JsonNode>) requestResponse).getResults()
                    .stream()
                    .map(result -> postUpdate(workerParameters, handler, mdClient, lfcClient, storageClient, entries, result))
                    .collect(Collectors.toList());

                ReportBody<UpdateUnitMetadataReportEntry> reportBody = new ReportBody<>();
                reportBody.setProcessId(workerParameters.getProcessId());
                reportBody.setReportType(ReportType.UPDATE_UNIT);
                reportBody.setEntries(entries);

                if (!entries.isEmpty()) {
                    batchReportClient.appendReportEntries(reportBody);
                }

                return itemStatuses;
            }

            throw new ProcessingException("Error when trying to update units.");
        } catch (Exception e) {
            throw new ProcessingException(e);
        }
    }

    private ItemStatus postUpdate(WorkerParameters workerParameters, HandlerIO handler, MetaDataClient mdClient, LogbookLifeCyclesClient lfcClient, StorageClient storageClient, List<UpdateUnitMetadataReportEntry> entries, JsonNode result) {
        String unitId = result.get(ID).asText();
        String key = result.get(KEY).asText();
        String statusAsString = result.get(STATUS).asText();
        StatusCode status = StatusCode.valueOf(statusAsString);
        String message = result.get(MESSAGE).asText();

        String diff = result.get(DIFF).asText();

        if ((!"null".equals(diff) || !StringUtils.isBlank(diff)) && (status.equals(OK) || status.equals(WARNING))) {
            try {
                writeLfcForUpdateUnit(lfcClient, workerParameters, unitId, diff);
            } catch (LogbookClientServerException | LogbookClientNotFoundException | InvalidParseOperationException | InvalidGuidOperationException e) {
                return buildItemStatus(MASS_UPDATE_UNITS, FATAL, EventDetails.of(String.format("Error '%s' while updating UNIT LFC.", e.getMessage())));
            } catch (LogbookClientBadRequestException e) {
                return buildItemStatus(MASS_UPDATE_UNITS, KO, EventDetails.of(String.format("Error '%s' while updating UNIT LFC.", e.getMessage())));
            }

            try {
                saveUnitWithLfc(mdClient, lfcClient, storageClient, handler, workerParameters, unitId, unitId + JSON);
            } catch (VitamException e) {
                // rollback LFC
                try {
                    lfcClient.rollBackUnitsByOperation(workerParameters.getContainerName());
                } catch (LogbookClientNotFoundException | LogbookClientBadRequestException | LogbookClientServerException e1) {
                    LOGGER.error(String.format("Error while storing UNIT with LFC %s.", e));
                    return buildItemStatus(MASS_UPDATE_UNITS, FATAL, EventDetails.of(String.format("Error while storing UNIT with LFC %s.", e)));
                }
            }
        }

        if (!OK.equals(status)) {
            VitamSession vitamSession = VitamThreadUtils.getVitamSession();
            UpdateUnitMetadataReportEntry entry = new UpdateUnitMetadataReportEntry(
                vitamSession.getTenantId(),
                workerParameters.getContainerName(),
                unitId,
                key,
                status,
                String.format("%s.%s", MASS_UPDATE_UNITS, status),
                message
            );
            entries.add(entry);
            return buildItemStatus(MASS_UPDATE_UNITS, KO, EventDetails.of(message));
        }

        return buildItemStatus(MASS_UPDATE_UNITS, OK, EventDetails.of("Mass update OK"));
    }

    private void writeLfcForUpdateUnit(LogbookLifeCyclesClient lfcClient, WorkerParameters param, String unitId, String diff)
        throws LogbookClientNotFoundException, LogbookClientBadRequestException, LogbookClientServerException,
            InvalidParseOperationException, InvalidGuidOperationException {

        LogbookLifeCycleParameters logbookLfcParam =
            LogbookParametersFactory.newLogbookLifeCycleUnitParameters(
                GUIDFactory.newEventGUID(ParameterHelper.getTenantParameter()),
                VitamLogbookMessages.getEventTypeLfc(UNIT_METADATA_UPDATE),
                GUIDReader.getGUID(param.getContainerName()),
                param.getLogbookTypeProcess(),
                OK,
                VitamLogbookMessages.getOutcomeDetailLfc(UNIT_METADATA_UPDATE, OK),
                VitamLogbookMessages.getCodeLfc(UNIT_METADATA_UPDATE, OK),
                GUIDReader.getGUID(unitId));
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

    /**
     * Store Unit with LFC by storing UNIT+LFC in workspace then storing in offers.
     * 
     * @param mdClient      metadataClient
     * @param lfcClient     logbook lifecycle client
     * @param storageClient storage client
     * @param handler       handler IO
     * @param params        handler parameters
     * @param guid          unit guid
     * @param fileName      stored unit file name
     * @throws VitamException when an error occurs
     */
    protected void saveUnitWithLfc(MetaDataClient mdClient, LogbookLifeCyclesClient lfcClient,
            StorageClient storageClient, HandlerIO handler, WorkerParameters params, String guid, String fileName)
            throws VitamException {

        //// get metadata
        JsonNode unit = selectMetadataDocumentRawById(guid, DataCategory.UNIT, mdClient);
        String strategyId = MetadataDocumentHelper.getStrategyIdFromRawUnitOrGot(unit);
        
        MetadataDocumentHelper.removeComputedFieldsFromUnit(unit);

        //// get lfc
        JsonNode lfc = getRawLogbookLifeCycleById(guid, DataCategory.UNIT, lfcClient);

        //// create file for storage (in workspace or temp or memory)
        JsonNode docWithLfc = MetadataStorageHelper.getUnitWithLFC(unit, lfc);

        // transfer json to workspace
        try {
            InputStream is = CanonicalJsonFormatter.serialize(docWithLfc);
            handler.transferInputStreamToWorkspace(IngestWorkflowConstants.ARCHIVE_UNIT_FOLDER + "/" + fileName, is,
                    null, false);
        } catch (ProcessingException e) {
            LOGGER.error(params.getObjectName(), e);
            throw new WorkspaceClientServerException(e);
        }

        // call storage (save in offers)
        // object Description
        final ObjectDescription description = new ObjectDescription(DataCategory.UNIT, params.getContainerName(),
                fileName, IngestWorkflowConstants.ARCHIVE_UNIT_FOLDER + File.separator + fileName);

        // store metadata object from workspace and set itemStatus
        storageClient.storeFileFromWorkspace(strategyId, description.getType(), description.getObjectName(),
                description);
    }

}
