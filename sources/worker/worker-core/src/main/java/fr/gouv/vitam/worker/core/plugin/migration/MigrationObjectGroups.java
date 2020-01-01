/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 *******************************************************************************/
package fr.gouv.vitam.worker.core.plugin.migration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.database.utils.MetadataDocumentHelper;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUID;
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
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleObjectGroupParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
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
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;

import java.io.File;
import java.io.InputStream;

import static fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory.newLogbookLifeCycleObjectGroupParameters;
import static fr.gouv.vitam.worker.core.plugin.migration.MigrationHelper.checkMigrationEvents;

/**
 * MigrationUnits class
 */
public class MigrationObjectGroups extends ActionHandler {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MigrationUnitPrepare.class);
    private static final String OBJECT_GROUPS_UPDATE_MIGRATION = "UPDATE_MIGRATION_OBJECT_GROUPS";
    private static final String JSON = ".json";
    private static final String OB_ID = "obId";
    private static final String $RESULTS = "$results";
    private static final String LIFE_CYCLE_NOT_FOUND = "LifeCycle not found";
    private static final String DEFAULT_STRATEGY = "default";

    private static final String MIGRATION_OBJECT_GROUPS = "MIGRATION_OBJECT_GROUPS";
    public static final String LFC_UPDATE_MIGRATION_OBJECT = "LFC.UPDATE_MIGRATION_OBJECT_GROUPS";
    private MetaDataClientFactory metaDataClientFactory;
    private LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory;
    private StorageClientFactory storageClientFactory;

    @VisibleForTesting
    public MigrationObjectGroups(MetaDataClientFactory metaDataClientFactory,
        LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory, StorageClientFactory storageClientFactory) {
        this.metaDataClientFactory = metaDataClientFactory;
        this.logbookLifeCyclesClientFactory = logbookLifeCyclesClientFactory;
        this.storageClientFactory = storageClientFactory;
    }

    /**
     * Constructor
     */
    public MigrationObjectGroups() {
        this(MetaDataClientFactory.getInstance(), LogbookLifeCyclesClientFactory.getInstance(),
            StorageClientFactory.getInstance());
    }


    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler)
        throws ProcessingException, ContentAddressableStorageServerException {
        ItemStatus itemStatus = new ItemStatus(MIGRATION_OBJECT_GROUPS);
        String objectGroupId = param.getObjectName();

        try (
            MetaDataClient metaDataClient = metaDataClientFactory.getClient();
            LogbookLifeCyclesClient logbookLifeCyclesClient = logbookLifeCyclesClientFactory.getClient();
            StorageClient storageClient = storageClientFactory.getClient()
        ) {

            //// get lfc
            JsonNode lastLFC = getRawObjectGroupLifeCycleById(objectGroupId);
            boolean doMigration = checkMigrationEvents(lastLFC, LFC_UPDATE_MIGRATION_OBJECT);

            if (doMigration) {

                UpdateMultiQuery updateMultiQuery = new UpdateMultiQuery();

                metaDataClient.updateObjectGroupById(updateMultiQuery.getFinalUpdate(), objectGroupId);

                LogbookLifeCycleObjectGroupParameters logbookLCParam =
                    createParameters(GUIDReader.getGUID(param.getContainerName()), StatusCode.OK,
                        GUIDReader.getGUID(objectGroupId), OBJECT_GROUPS_UPDATE_MIGRATION);

                logbookLifeCyclesClient.update(logbookLCParam, LifeCycleStatusCode.LIFE_CYCLE_COMMITTED);
            }

            final String fileName = objectGroupId + JSON;

            //TODO Call StoreMetaDataObjectGroupActionPlugin
            //// get metadata
            JsonNode objectGroupMetadata = getObjectGroupMetadata(objectGroupId);

            MetadataDocumentHelper.removeComputedFieldsFromObjectGroup(objectGroupMetadata);


            //// create file for storage (in workspace or temp or memory)
            JsonNode lfc = getRawObjectGroupLifeCycleById(objectGroupId);
            JsonNode docWithLfc = MetadataStorageHelper.getGotWithLFC(objectGroupMetadata, lfc);

            // transfer json to workspace
            InputStream is = CanonicalJsonFormatter.serialize(docWithLfc);
            handler
                .transferInputStreamToWorkspace(IngestWorkflowConstants.OBJECT_GROUP_FOLDER + "/" + fileName, is,
                    null, false);

            final ObjectDescription description =
                new ObjectDescription(DataCategory.OBJECTGROUP, param.getContainerName(),
                    fileName, IngestWorkflowConstants.OBJECT_GROUP_FOLDER + File.separator + fileName);
            // store binary data object
            storageClient.storeFileFromWorkspace(DEFAULT_STRATEGY, description.getType(),
                description.getObjectName(),
                description);

            cleanupObjectGroupFromWorkspace(param, handler, fileName);

        } catch (VitamException e) {
            LOGGER.error(e);
            return itemStatus.increment(StatusCode.FATAL);
        }
        itemStatus.increment(StatusCode.OK);
        return new ItemStatus(MIGRATION_OBJECT_GROUPS).setItemsStatus(MIGRATION_OBJECT_GROUPS, itemStatus);
    }

    private void cleanupObjectGroupFromWorkspace(WorkerParameters param, HandlerIO handler, String fileName)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {
        try (WorkspaceClient client = handler.getWorkspaceClientFactory().getClient()) {
            client.deleteObject(param.getContainerName(), IngestWorkflowConstants.OBJECT_GROUP_FOLDER + "/" + fileName);
        }
    }

    /**
     * @param objectGroupId id
     * @return unit metadata as json
     */
    JsonNode getObjectGroupMetadata(String objectGroupId) throws ProcessingException {
        MetaDataClient metaDataClient = metaDataClientFactory.getClient();
        final String error = String.format("No such ObjectGroup metadata '%s'", objectGroupId);
        RequestResponse<JsonNode> requestResponse;

        JsonNode jsonResponse = null;
        try {
            requestResponse = metaDataClient.getObjectGroupByIdRaw(objectGroupId);

            if (requestResponse.isOk()) {
                jsonResponse = requestResponse.toJsonNode();
            }
            JsonNode jsonNode;
            // check response
            if (jsonResponse == null) {
                LOGGER.error(error);
                throw new ProcessingException(error);
            }
            jsonNode = jsonResponse.get($RESULTS);
            // if result = 0 then throw Exception
            if (jsonNode == null || jsonNode.size() == 0) {
                LOGGER.error(error);
                throw new VitamException(error);
            }

            // return a single node
            return jsonNode.get(0);

        } catch (VitamException e) {
            LOGGER.error(e);
            throw new ProcessingException(e);
        }
    }

    /**
     * retrieve the raw LFC for the object group
     *
     * @param idDocument document uuid
     * @return the raw LFC
     * @throws ProcessingException if no result found or error during parsing response from logbook client
     */
    private JsonNode getRawObjectGroupLifeCycleById(String idDocument)
        throws ProcessingException {
        try {
            LogbookLifeCyclesClient client = logbookLifeCyclesClientFactory.getClient();
            return client.getRawObjectGroupLifeCycleById(idDocument);
        } catch (final InvalidParseOperationException | LogbookClientException e) {
            throw new ProcessingException(e);
        }
    }

    private LogbookLifeCycleObjectGroupParameters createParameters(GUID eventIdentifierProcess,
        StatusCode logbookOutcome, GUID objectIdentifier, String action) {
        final LogbookTypeProcess eventTypeProcess = LogbookTypeProcess.UPDATE;
        final GUID updateGuid = GUIDFactory.newEventGUID(ParameterHelper.getTenantParameter());
        LogbookLifeCycleObjectGroupParameters parameters = newLogbookLifeCycleObjectGroupParameters(updateGuid,
            VitamLogbookMessages.getEventTypeLfc(action),
            eventIdentifierProcess,
            eventTypeProcess, logbookOutcome,
            VitamLogbookMessages.getOutcomeDetailLfc(action, logbookOutcome),
            VitamLogbookMessages.getCodeLfc(action, logbookOutcome), objectIdentifier);
        ObjectNode objectNode = JsonHandler.createObjectNode();
        parameters.putParameterValue(LogbookParameterName.eventDetailData,
            objectNode.textValue());
        return parameters;
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
    }
}
