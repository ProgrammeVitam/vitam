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
package fr.gouv.vitam.worker.core.plugin.migration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
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
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.LifeCycleStatusCode;
import fr.gouv.vitam.common.model.MetadataStorageHelper;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
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
import java.util.List;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.model.IngestWorkflowConstants.ARCHIVE_UNIT_FOLDER;
import static fr.gouv.vitam.common.model.StatusCode.FATAL;
import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.metadata.core.model.UpdateUnit.DIFF;
import static fr.gouv.vitam.metadata.core.model.UpdateUnit.ID;
import static fr.gouv.vitam.storage.engine.common.model.DataCategory.UNIT;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;

/**
 * MigrationUnits class
 */
public class MigrationUnits extends StoreMetadataObjectActionHandler {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MigrationUnitPrepare.class);
    private static final String UNIT_UPDATE_MIGRATION = "UPDATE_MIGRATION_UNITS";

    private static final String MIGRATION_UNITS = "MIGRATION_UNITS";
    public static final String LFC_UPDATE_MIGRATION_UNITS = "LFC.UPDATE_MIGRATION_UNITS";
    private final MetaDataClientFactory metaDataClientFactory;
    private final LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory;
    private final StorageClientFactory storageClientFactory;

    @VisibleForTesting
    public MigrationUnits(MetaDataClientFactory metaDataClientFactory,
        LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory, StorageClientFactory storageClientFactory) {
        super(storageClientFactory);
        this.metaDataClientFactory = metaDataClientFactory;
        this.logbookLifeCyclesClientFactory = logbookLifeCyclesClientFactory;
        this.storageClientFactory = storageClientFactory;
    }

    public MigrationUnits() {
        this(MetaDataClientFactory.getInstance(), LogbookLifeCyclesClientFactory.getInstance(),
            StorageClientFactory.getInstance());
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler)
        throws ProcessingException {
        throw new IllegalStateException("UnsupportedOperation");
    }

    @Override
    public List<ItemStatus> executeList(WorkerParameters workerParameters, HandlerIO handler)
        throws ProcessingException {
        try (MetaDataClient metaDataClient = metaDataClientFactory.getClient();
            LogbookLifeCyclesClient logbookLifeCyclesClientFactoryClient = logbookLifeCyclesClientFactory.getClient();
            StorageClient storageClient = storageClientFactory.getClient()) {

            // Add operationID to #operations
            UpdateMultiQuery multiQuery = new UpdateMultiQuery();
            multiQuery.addActions(UpdateActionHelper
                .push(VitamFieldsHelper.operations(), VitamThreadUtils.getVitamSession().getRequestId()));


            // remove search part (useless)
            multiQuery.resetQueries();

            // set the units to update
            List<String> units = workerParameters.getObjectNameList();
            multiQuery.resetRoots().addRoots(units.toArray(new String[0]));

            // call update BULK service
            RequestResponse<JsonNode> requestResponse = metaDataClient.updateUnitBulk(multiQuery.getFinalUpdate());

            // Prepare rapport
            if (requestResponse != null && requestResponse.isOk()) {

                return ((RequestResponseOK<JsonNode>) requestResponse).getResults()
                    .stream()
                    .map(result -> postMigation(workerParameters, handler, metaDataClient,
                        logbookLifeCyclesClientFactoryClient, storageClient, result))
                    .collect(Collectors.toList());
            }

            throw new ProcessingException("Error when trying to update units.");
        } catch (Exception e) {
            throw new ProcessingException(e);
        }
    }

    private ItemStatus postMigation(WorkerParameters workerParameters, HandlerIO handler, MetaDataClient metaDataClient,
        LogbookLifeCyclesClient logbookLifeCyclesClientFactoryClient, StorageClient storageClient,
        JsonNode unitNode) {
        String unitId = unitNode.get(ID).asText();
        String diff = unitNode.get(DIFF).asText();

        try {
            writeLfcToMongo(logbookLifeCyclesClientFactoryClient, workerParameters, unitId, diff);
        } catch (LogbookClientServerException | LogbookClientNotFoundException | InvalidParseOperationException | InvalidGuidOperationException e) {
            LOGGER.error(e);
            return buildItemStatus(MIGRATION_UNITS, FATAL,
                EventDetails.of(String.format("Error '%s' while updating UNIT LFC.", e.getMessage())));
        } catch (LogbookClientBadRequestException e) {
            LOGGER.error(e);
            return buildItemStatus(MIGRATION_UNITS, KO,
                EventDetails.of(String.format("Error '%s' while updating UNIT LFC.", e.getMessage())));
        }

        try {
            storeUnitAndLfcToOffer(metaDataClient, logbookLifeCyclesClientFactoryClient, storageClient, handler,
                workerParameters, unitId, unitId + ".json");
        } catch (VitamException e) {
            LOGGER.error(e);
            return buildItemStatus(MIGRATION_UNITS, FATAL,
                EventDetails.of(String.format("Error while storing UNIT with LFC '%s'.", e.getMessage())));
        }
        return buildItemStatus(MIGRATION_UNITS, OK, EventDetails.of("Update OK"));
    }



    private boolean lfcAlreadyWrittenInMongo(LogbookLifeCyclesClient lfcClient, String unitId,
        String currentOperationId) throws VitamException {
        JsonNode lfc = lfcClient.getRawUnitLifeCycleById(unitId);
        LogbookLifecycle unitLFC = JsonHandler.getFromJsonNode(lfc, LogbookLifecycle.class);
        return unitLFC.getEvents().stream().anyMatch(e -> e.getEvIdProc().equals(currentOperationId));
    }

    private void writeLfcToMongo(LogbookLifeCyclesClient lfcClient, WorkerParameters param, String unitId, String diff)
        throws LogbookClientNotFoundException, LogbookClientBadRequestException, LogbookClientServerException,
        InvalidParseOperationException, InvalidGuidOperationException {

        LogbookLifeCycleParameters logbookLfcParam = LogbookParameterHelper.newLogbookLifeCycleUnitParameters(
            GUIDFactory.newEventGUID(ParameterHelper.getTenantParameter()),
            VitamLogbookMessages.getEventTypeLfc(UNIT_UPDATE_MIGRATION),
            GUIDReader.getGUID(param.getContainerName()),
            param.getLogbookTypeProcess(),
            OK,
            VitamLogbookMessages.getOutcomeDetailLfc(UNIT_UPDATE_MIGRATION, OK),
            VitamLogbookMessages.getCodeLfc(UNIT_UPDATE_MIGRATION, OK),
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

    private void storeUnitAndLfcToOffer(MetaDataClient mdClient, LogbookLifeCyclesClient lfcClient,
        StorageClient storageClient, HandlerIO handler, WorkerParameters params, String guid, String fileName)
        throws VitamException {
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
        storageClient.storeFileFromWorkspace(strategyId, description.getType(), description.getObjectName(),
            description);
    }

}
