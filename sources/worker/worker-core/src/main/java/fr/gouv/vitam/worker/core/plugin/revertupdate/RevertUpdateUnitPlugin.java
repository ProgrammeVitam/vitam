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

package fr.gouv.vitam.worker.core.plugin.revertupdate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.batch.report.client.BatchReportClient;
import fr.gouv.vitam.batch.report.client.BatchReportClientFactory;
import fr.gouv.vitam.batch.report.model.ReportBody;
import fr.gouv.vitam.batch.report.model.ReportType;
import fr.gouv.vitam.batch.report.model.entry.UpdateUnitMetadataReportEntry;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.utils.MetadataDocumentHelper;
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
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.plugin.StoreMetadataObjectActionHandler;
import fr.gouv.vitam.worker.core.utils.PluginHelper.EventDetails;
import fr.gouv.vitam.workspace.api.exception.WorkspaceClientServerException;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.model.IngestWorkflowConstants.ARCHIVE_UNIT_FOLDER;
import static fr.gouv.vitam.common.model.StatusCode.FATAL;
import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.metadata.core.model.UpdateUnit.DIFF;
import static fr.gouv.vitam.metadata.core.model.UpdateUnit.ID;
import static fr.gouv.vitam.metadata.core.model.UpdateUnit.KEY;
import static fr.gouv.vitam.metadata.core.model.UpdateUnit.MESSAGE;
import static fr.gouv.vitam.metadata.core.model.UpdateUnit.STATUS;
import static fr.gouv.vitam.metadata.core.model.UpdateUnitKey.UNIT_METADATA_NO_CHANGES;
import static fr.gouv.vitam.storage.engine.common.model.DataCategory.UNIT;
import static fr.gouv.vitam.worker.core.plugin.massprocessing.description.MassUpdateUnitsProcess.MASS_UPDATE_UNITS;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;

public class RevertUpdateUnitPlugin  extends StoreMetadataObjectActionHandler {

    private static final VitamLogger LOGGER =
        VitamLoggerFactory.getInstance(RevertUpdateUnitPlugin.class);
    private static final String PLUGIN_ID = "REVERT_UPDATE_UNITS";
    private static final String UNIT_METADATA_REVERT = "UNIT_METADATA_REVERT";

    private final MetaDataClientFactory metadataClientFactory;
    private final LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory;
    private final StorageClientFactory storageClientFactory;
    private final BatchReportClientFactory batchReportClientFactory;

    @SuppressWarnings("unused")
    public RevertUpdateUnitPlugin() {
        this(StorageClientFactory.getInstance(), MetaDataClientFactory.getInstance(),
            LogbookLifeCyclesClientFactory.getInstance(), BatchReportClientFactory.getInstance());
    }

    @VisibleForTesting
    public RevertUpdateUnitPlugin(StorageClientFactory storageClientFactory,
        MetaDataClientFactory metadataClientFactory, LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory,
        BatchReportClientFactory batchReportClientFactory) {
        super(storageClientFactory);
        this.metadataClientFactory = metadataClientFactory;
        this.logbookLifeCyclesClientFactory = logbookLifeCyclesClientFactory;
        this.storageClientFactory = storageClientFactory;
        this.batchReportClientFactory = batchReportClientFactory;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException {

        try (MetaDataClient metadataClient = metadataClientFactory.getClient();
            LogbookLifeCyclesClient logbookLifeCyclesClient = logbookLifeCyclesClientFactory.getClient();
            StorageClient storageClient = storageClientFactory.getClient();
            BatchReportClient batchReportClient = batchReportClientFactory.getClient()
        ) {
            JsonNode query = JsonHandler.getFromString(param.getObjectName());
            // call update BULK service
            RequestResponse<JsonNode> requestResponse = metadataClient.updateUnitBulk(query);

            // Prepare rapport
            List<UpdateUnitMetadataReportEntry> failingEntries = new ArrayList<>();
            if (requestResponse != null && requestResponse.isOk()) {
                ItemStatus itemStatus = ((RequestResponseOK<JsonNode>) requestResponse).getResults()
                    .stream()
                    .map(result -> postUpdate(param, handler, metadataClient, logbookLifeCyclesClient, storageClient,
                        failingEntries, result))
                    .reduce(new ItemStatus(PLUGIN_ID), ItemStatus::setItemsStatus);

                ReportBody<UpdateUnitMetadataReportEntry> reportBody = new ReportBody<>();
                reportBody.setProcessId(param.getProcessId());
                reportBody.setReportType(ReportType.UPDATE_UNIT);
                reportBody.setEntries(failingEntries);

                if (!failingEntries.isEmpty()) {
                    batchReportClient.appendReportEntries(reportBody);
                }
                return itemStatus;
            }

            throw new ProcessingException("Error when trying to update units.");
        } catch (InvalidParseOperationException | MetaDataExecutionException | MetaDataDocumentSizeException | MetaDataClientServerException | MetaDataNotFoundException | VitamClientInternalException e) {
            throw new ProcessingException(e);
        }
    }


    /******************************** TO REFACTOR *********************************************/



    private ItemStatus postUpdate(WorkerParameters workerParameters, HandlerIO handler, MetaDataClient mdClient, LogbookLifeCyclesClient lfcClient, StorageClient storageClient, List<UpdateUnitMetadataReportEntry> failingEntries, JsonNode unitNode) {
        String unitId = unitNode.get(ID).asText();
        String key = unitNode.get(KEY).asText();
        String statusAsString = unitNode.get(STATUS).asText();
        StatusCode status = StatusCode.valueOf(statusAsString);
        String message = unitNode.get(MESSAGE).asText();

        String diff = unitNode.get(DIFF).asText();

        if (!KO.equals(status) && !FATAL.equals(status) && !OK.equals(status)) {
            throw new VitamRuntimeException(String.format("Status must be of type KO, FATAL or OK here '%s'.", status));
        }

        if (KO.equals(status) || FATAL.equals(status)) {
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
            failingEntries.add(entry);
            return buildItemStatus(MASS_UPDATE_UNITS, status, EventDetails.of(message));
        }

        if (UNIT_METADATA_NO_CHANGES.name().equals(key)) {
            try {
                if (lfcAlreadyWrittenInMongo(lfcClient, unitId, workerParameters.getContainerName())) {
                    LOGGER.warn(String.format("There is no changes on the unit '%s', and LFC already written in mongo, this unit will be save in offer.", unitId));
                    storeUnitAndLfcToOffer(mdClient, lfcClient, storageClient, handler, workerParameters, unitId, unitId + ".json");
                    return buildItemStatus(MASS_UPDATE_UNITS, OK, EventDetails.of("Mass update OK"));
                }
            } catch (VitamException e) {
                LOGGER.error(e);
                return buildItemStatus(MASS_UPDATE_UNITS, FATAL, EventDetails.of(String.format("Error while storing UNIT with LFC '%s'.", e.getMessage())));
            }
        }

        try {
            writeLfcToMongo(lfcClient, workerParameters, unitId, diff);
        } catch (LogbookClientServerException | LogbookClientNotFoundException | InvalidParseOperationException | InvalidGuidOperationException e) {
            LOGGER.error(e);
            return buildItemStatus(MASS_UPDATE_UNITS, FATAL, EventDetails.of(String.format("Error '%s' while updating UNIT LFC.", e.getMessage())));
        } catch (LogbookClientBadRequestException e) {
            LOGGER.error(e);
            return buildItemStatus(MASS_UPDATE_UNITS, KO, EventDetails
                .of(String.format("Error '%s' while updating UNIT LFC.", e.getMessage())));
        }

        try {
            storeUnitAndLfcToOffer(mdClient, lfcClient, storageClient, handler, workerParameters, unitId, unitId + ".json");
        } catch (VitamException e) {
            LOGGER.error(e);
            return buildItemStatus(MASS_UPDATE_UNITS, FATAL, EventDetails
                .of(String.format("Error while storing UNIT with LFC '%s'.", e.getMessage())));
        }
        return buildItemStatus(MASS_UPDATE_UNITS, OK, EventDetails.of("Mass update OK"));
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
            VitamLogbookMessages.getEventTypeLfc(UNIT_METADATA_REVERT),
            GUIDReader.getGUID(param.getContainerName()),
            param.getLogbookTypeProcess(),
            OK,
            VitamLogbookMessages.getOutcomeDetailLfc(UNIT_METADATA_REVERT, OK),
            VitamLogbookMessages.getCodeLfc(UNIT_METADATA_REVERT, OK),
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
        diffObject.put("version", VitamConfiguration.getDiffVersion());
        return JsonHandler.writeAsString(diffObject);
    }

    private void storeUnitAndLfcToOffer(MetaDataClient mdClient, LogbookLifeCyclesClient lfcClient, StorageClient storageClient, HandlerIO handler, WorkerParameters params, String guid, String fileName) throws
        VitamException {
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
}
