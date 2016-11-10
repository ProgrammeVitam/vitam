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
package fr.gouv.vitam.worker.core.handler;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.CompositeItemStatus;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleObjectGroupParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.StorageCollectionType;
import fr.gouv.vitam.storage.engine.client.exception.StorageClientException;
import fr.gouv.vitam.storage.engine.common.model.request.CreateObjectDescription;
import fr.gouv.vitam.worker.common.utils.IngestWorkflowConstants;
import fr.gouv.vitam.worker.common.utils.LogbookLifecycleWorkerHelper;
import fr.gouv.vitam.worker.common.utils.SedaConstants;
import fr.gouv.vitam.worker.core.api.HandlerIO;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

/**
 * StoreObjectGroup Handler.<br>
 */
public class StoreObjectGroupActionHandler extends ActionHandler {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IndexObjectGroupActionHandler.class);

    private static final String HANDLER_ID = "OG_STORAGE";
    private static final String SIP = "SIP/";

    // FIXME P1 should not be a private attribute -> to refactor
    private final LogbookLifeCycleObjectGroupParameters logbookLifecycleObjectGroupParameters = LogbookParametersFactory
        .newLogbookLifeCycleObjectGroupParameters();
    private final StorageClientFactory storageClientFactory;
    private static final String DEFAULT_TENANT = "0";
    private static final int TENANT = 0;

    private static final String DEFAULT_STRATEGY = "default";

    private static final String LOGBOOK_LF_BAD_REQUEST_EXCEPTION_MSG = "LogbookClient Unsupported request";
    private static final String LOGBOOK_LF_RESOURCE_NOT_FOUND_EXCEPTION_MSG = "Logbook LifeCycle resource not found";
    private static final String LOGBOOK_SERVER_INTERNAL_EXCEPTION_MSG = "Logbook Server internal error";
    // FIXME P0 WORKFLOW will be in vitam-logbook file
    private static final String LOGBOOK_LF_STORAGE_MSG = "Stockage des objets";
    private static final String LOGBOOK_LF_STORAGE_OK_MSG = "Stockage des objets réalisé avec succès";
    private static final String LOGBOOK_LF_STORAGE_KO_MSG = "Stockage des objets en erreur";
    private static final String LOGBOOK_LF_STORAGE_BDO_MSG = "Stockage de l'objet";
    private static final String LOGBOOK_LF_STORAGE_BDO_KO_MSG = "Stockage de l'objet en erreur";
    private static final String OG_LIFE_CYCLE_STORE_BDO_EVENT_TYPE = "Stockage des groupes d'objets - Stockage d'objet";

    /**
     * Constructor
     */
    public StoreObjectGroupActionHandler() {
        storageClientFactory = StorageClientFactory.getInstance();
    }

    /**
     * Constructor with parameter storageClientFactory, for tests
     *
     * @param storageClientFactory the storage client factory
     */
    StoreObjectGroupActionHandler(StorageClientFactory storageClientFactory) {
        this.storageClientFactory = storageClientFactory;
    }

    /**
     * @return HANDLER_ID
     */
    public static final String getId() {
        return HANDLER_ID;
    }

    @Override
    public CompositeItemStatus execute(WorkerParameters params, HandlerIO actionDefinition) {
        checkMandatoryParameters(params);
        final ItemStatus itemStatus = new ItemStatus(HANDLER_ID);
        try (
            LogbookLifeCyclesClient logbookLifeCycleClient = LogbookLifeCyclesClientFactory.getInstance().getClient()) {
            try {
                checkMandatoryIOParameter(actionDefinition);
                // Update lifecycle of object group : STARTED
                LogbookLifecycleWorkerHelper.updateLifeCycleStartStep(logbookLifeCycleClient,
                    logbookLifecycleObjectGroupParameters, params);

                // get list of object group's objects
                final Map<String, String> objectGuids = getMapOfObjectsIdsAndUris(params);
                // get list of object uris
                for (final Map.Entry<String, String> objectGuid : objectGuids.entrySet()) {
                    // Execute action on the object
                    storeObject(params, objectGuid.getKey(), objectGuid.getValue(), itemStatus, logbookLifeCycleClient);
                }
            } catch (final StorageClientException e) {
                LOGGER.error(e);
            } catch (final ProcessingException e) {
                LOGGER.error(e);
                itemStatus.increment(StatusCode.FATAL);
            }

            if (StatusCode.UNKNOWN.equals(itemStatus.getGlobalStatus())) {
                itemStatus.increment(StatusCode.OK);
            }

            // Update lifecycle of object group : OK/KO
            try {
                LogbookLifecycleWorkerHelper.setLifeCycleFinalEventStatusByStep(logbookLifeCycleClient,
                    logbookLifecycleObjectGroupParameters, itemStatus);
            } catch (final ProcessingException e) {
                LOGGER.error(e);
                itemStatus.increment(StatusCode.FATAL);
            }
        }
        return new CompositeItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
    }

    /**
     * Store a binary data object with the storage engine.
     *
     * @param params worker parameters
     * @param objectGUID the object guid
     * @param objectUri the object uri
     * @param itemStatus item status
     * @param logbookLifeCycleClient logbook LifeCycle Client
     * @throws StorageClientException throws when a storage error occurs
     * @throws ProcessingException throws when unexpected error occurs
     */
    private void storeObject(WorkerParameters params, String objectGUID, String objectUri, ItemStatus itemStatus,
        LogbookLifeCyclesClient logbookLifeCycleClient)
        throws ProcessingException, StorageClientException {
        LOGGER.debug("Storing object with guid: " + objectGUID);
        ParametersChecker.checkParameter("objectUri id is a mandatory parameter", objectUri);
        try {
            // store binary data object
            final CreateObjectDescription description = new CreateObjectDescription();
            description.setWorkspaceContainerGUID(params.getContainerName());
            description.setWorkspaceObjectURI(SIP + objectUri);

            try (final StorageClient storageClient = storageClientFactory.getClient()) {
                storageClientFactory.getClient().storeFileFromWorkspace(DEFAULT_TENANT,
                    DEFAULT_STRATEGY, StorageCollectionType.OBJECTS, objectGUID, description);
            }

            // update lifecycle of objectGroup with detail of object : OK
            updateLifeCycleParametersLogbookForBdo(params, objectGUID);
            logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcome,
                StatusCode.OK.toString());
            logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcomeDetail,
                VitamLogbookMessages.getOutcomeDetailLfc(itemStatus.getItemId(), StatusCode.OK));
            logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                VitamLogbookMessages.getCodeLfc(itemStatus.getItemId(), StatusCode.OK));
            updateLifeCycle(logbookLifeCycleClient);
        } catch (final StorageClientException e) {
            LOGGER.error(e);
            itemStatus.increment(StatusCode.KO);
            // update lifecycle of objectGroup with detail of object : KO
            logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcome,
                StatusCode.KO.toString());
            logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcomeDetail,
                VitamLogbookMessages.getOutcomeDetailLfc(itemStatus.getItemId(), StatusCode.KO));
            logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                VitamLogbookMessages.getCodeLfc(itemStatus.getItemId(), StatusCode.KO));
            updateLifeCycle(logbookLifeCycleClient);
            throw e;
        }
    }

    /**
     * Get the list of objects linked to the current object group
     *
     * @param params worker parameters
     * @return the list of object guid
     * @throws ProcessingException throws when error occurs while retrieving the object group file from workspace
     */
    private Map<String, String> getMapOfObjectsIdsAndUris(WorkerParameters params) throws ProcessingException {
        final Map<String, String> binaryObjectsToStore = new HashMap<>();
        final String containerId = params.getContainerName();
        final String objectName = params.getObjectName();
        ParametersChecker.checkParameter("Container id is a mandatory parameter", containerId);
        ParametersChecker.checkParameter("ObjectName id is a mandatory parameter", objectName);
        final JsonNode jsonOG;
        // WorkspaceClientFactory.changeMode(params.getUrlWorkspace());
        try (final WorkspaceClient workspaceClient = WorkspaceClientFactory.getInstance().getClient()) {
            // Get objectGroup objects ids
            jsonOG = getJsonFromWorkspace(workspaceClient, containerId,
                IngestWorkflowConstants.OBJECT_GROUP_FOLDER + "/" + objectName);
        }
        // Filter on objectGroup objects ids to retrieve only binary objects
        // informations linked to the ObjectGroup
        final JsonNode work = jsonOG.get(SedaConstants.PREFIX_WORK);
        final JsonNode qualifiers = work.get(SedaConstants.PREFIX_QUALIFIERS);
        if (qualifiers == null) {
            return binaryObjectsToStore;
        }

        final List<JsonNode> versions = qualifiers.findValues(SedaConstants.TAG_VERSIONS);
        if (versions == null || versions.isEmpty()) {
            return binaryObjectsToStore;
        }
        for (final JsonNode version : versions) {
            for (final JsonNode binaryObject : version) {
                binaryObjectsToStore.put(binaryObject.get(SedaConstants.PREFIX_ID).asText(),
                    binaryObject.get(SedaConstants.TAG_URI).asText());
            }
        }

        return binaryObjectsToStore;
    }

    /**
     * Retrieve a json file as a {@link JsonNode} from the workspace.
     *
     * @param workspaceClient workspace connector
     * @param containerId container id
     * @param jsonFilePath path in workspace of the json File
     * @return JsonNode of the json file
     * @throws ProcessingException throws when error occurs
     */
    public JsonNode getJsonFromWorkspace(WorkspaceClient workspaceClient, String containerId, String jsonFilePath)
        throws ProcessingException {
        try (InputStream is = workspaceClient.getObject(containerId, jsonFilePath)) {
            if (is != null) {
                return JsonHandler.getFromInputStream(is, JsonNode.class);
            } else {
                LOGGER.error("Object group not found");
                throw new ProcessingException("Object group not found");
            }

        } catch (InvalidParseOperationException | IOException e) {
            LOGGER.debug("Json wrong format", e);
            throw new ProcessingException(e);
        } catch (ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException e) {
            LOGGER.debug("Workspace Server Error", e);
            throw new ProcessingException(e);
        }
    }

    /**
     * Update the lifecycle with the current ObjectGroup lifecycle parameters.
     * 
     * @param logbookLifeCycleClient logbook LifeCycle Client
     * @throws ProcessingException throws when error occurs
     */
    private void updateLifeCycle(LogbookLifeCyclesClient logbookLifeCycleClient) throws ProcessingException {

        try {
            logbookLifeCycleClient.update(logbookLifecycleObjectGroupParameters);
        } catch (final LogbookClientBadRequestException e) {
            LOGGER.error(LOGBOOK_LF_BAD_REQUEST_EXCEPTION_MSG, e);
            throw new ProcessingException(e);
        } catch (final LogbookClientServerException e) {
            LOGGER.error(LOGBOOK_SERVER_INTERNAL_EXCEPTION_MSG, e);
            throw new ProcessingException(e);
        } catch (final LogbookClientNotFoundException e) {
            LOGGER.error(LOGBOOK_LF_RESOURCE_NOT_FOUND_EXCEPTION_MSG, e);
            throw new ProcessingException(e);
        }
    }

    /**
     * Update current ObjectGroup lifecycle parameters with binary data object lifecycle parameters. <br>
     *
     * @param params worker parameters
     * @param bdoId binary data object id
     */
    private void updateLifeCycleParametersLogbookForBdo(WorkerParameters params, String bdoId) {
        final String extension = FilenameUtils.getExtension(params.getObjectName());
        logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.eventIdentifierProcess,
            params.getObjectName().replace("." + extension, ""));
        logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.eventIdentifier, bdoId);
        logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.eventType,
            HANDLER_ID);
        logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcome,
            StatusCode.STARTED.toString());
        logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcomeDetail,
            VitamLogbookMessages.getOutcomeDetailLfc(HANDLER_ID, StatusCode.STARTED));
        logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
            VitamLogbookMessages.getCodeLfc(HANDLER_ID, StatusCode.STARTED));
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // TODO P0 Add objectGroup.json add input and check it
    }

}
