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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleObjectGroupParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.StorageCollectionType;
import fr.gouv.vitam.storage.engine.client.exception.StorageClientException;
import fr.gouv.vitam.storage.engine.common.model.request.CreateObjectDescription;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.common.utils.IngestWorkflowConstants;
import fr.gouv.vitam.worker.common.utils.LogbookLifecycleWorkerHelper;
import fr.gouv.vitam.worker.common.utils.SedaConstants;

/**
 * StoreObjectGroup Handler.<br>
 */
public class StoreObjectGroupActionHandler extends ActionHandler {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IndexObjectGroupActionHandler.class);

    private static final String HANDLER_ID = "OG_STORAGE";
    private static final String STORING_OBJECT_TASK_ID = "OBJECT_STORAGE_SUB_TASK";
    private static final String STORING_OBJECT_FULL_TASK_ID = HANDLER_ID + "." + STORING_OBJECT_TASK_ID;
    private static final String SIP = "SIP/";

    // FIXME P1 should not be a private attribute -> to refactor
    private final LogbookLifeCycleObjectGroupParameters logbookLifecycleObjectGroupParameters = LogbookParametersFactory
        .newLogbookLifeCycleObjectGroupParameters();
    private final StorageClientFactory storageClientFactory;
    private static final String DEFAULT_TENANT = "0";

    private static final String DEFAULT_STRATEGY = "default";

    private static final String LOGBOOK_LF_RESOURCE_NOT_FOUND_EXCEPTION_MSG = "Logbook LifeCycle resource not found";
    private HandlerIO handlerIO;


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
    public ItemStatus execute(WorkerParameters params, HandlerIO actionDefinition) {
        checkMandatoryParameters(params);
        handlerIO = actionDefinition;
        final ItemStatus itemStatus = new ItemStatus(HANDLER_ID);
        final String objectID = LogbookLifecycleWorkerHelper.getObjectID(params);
        try {
            try {
                checkMandatoryIOParameter(actionDefinition);

                // Update lifecycle of object group : STARTED
                LogbookLifecycleWorkerHelper.updateLifeCycleStartStep(handlerIO.getHelper(),
                    logbookLifecycleObjectGroupParameters, params, HANDLER_ID, LogbookTypeProcess.INGEST);

                // get list of object group's objects
                final Map<String, String> objectGuids = getMapOfObjectsIdsAndUris(params);
                // get list of object uris
                for (final Map.Entry<String, String> objectGuid : objectGuids.entrySet()) {
                    // Execute action on the object
                    storeObject(params, objectGuid.getKey(), objectGuid.getValue(), itemStatus);
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
                logbookLifecycleObjectGroupParameters.setFinalStatus(HANDLER_ID, null, itemStatus.getGlobalStatus(),
                    null);

                LogbookLifecycleWorkerHelper.setLifeCycleFinalEventStatusByStep(handlerIO.getHelper(),
                    logbookLifecycleObjectGroupParameters, itemStatus);
            } catch (final ProcessingException e) {
                LOGGER.error(e);
                itemStatus.increment(StatusCode.FATAL);
            }
        } finally {
            try {
                handlerIO.getLifecyclesClient().bulkUpdateObjectGroup(params.getContainerName(),
                    handlerIO.getHelper().removeUpdateDelegate(objectID));
            } catch (LogbookClientNotFoundException | LogbookClientBadRequestException |
                LogbookClientServerException e) {
                LOGGER.error(e);
                itemStatus.increment(StatusCode.FATAL);
            }
        }
        return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
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
    private void storeObject(WorkerParameters params, String objectGUID, String objectUri, ItemStatus itemStatus)
        throws ProcessingException, StorageClientException {
        LOGGER.debug("Storing object with guid: " + objectGUID);
        ParametersChecker.checkParameter("objectUri id is a mandatory parameter", objectUri);
        try {
            // store binary data object
            final CreateObjectDescription description = new CreateObjectDescription();
            description.setWorkspaceContainerGUID(params.getContainerName());
            description.setWorkspaceObjectURI(SIP + objectUri);

            try (final StorageClient storageClient = storageClientFactory.getClient()) {
                storageClient.storeFileFromWorkspace(DEFAULT_TENANT,
                    DEFAULT_STRATEGY, StorageCollectionType.OBJECTS, objectGUID, description);
            }

            // update lifecycle of objectGroup with detail of object : OK
            updateLifeCycleParametersLogbookForBdo(params, objectGUID);
            logbookLifecycleObjectGroupParameters.setFinalStatus(STORING_OBJECT_FULL_TASK_ID, null, StatusCode.OK,
                null);
            updateLifeCycle();


        } catch (final StorageClientException e) {
            LOGGER.error(e);
            itemStatus.increment(StatusCode.FATAL);
            // update lifecycle of objectGroup with detail of object : KO
            logbookLifecycleObjectGroupParameters.setFinalStatus(STORING_OBJECT_FULL_TASK_ID, null, StatusCode.FATAL,
                null);
            updateLifeCycle();
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
        // Get objectGroup objects ids
        jsonOG = handlerIO.getJsonFromWorkspace(
            IngestWorkflowConstants.OBJECT_GROUP_FOLDER + "/" + objectName);
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
     * Update the lifecycle with the current ObjectGroup lifecycle parameters.
     *
     * @param logbookLifeCycleClient logbook LifeCycle Client
     * @throws ProcessingException throws when error occurs
     */
    private void updateLifeCycle() throws ProcessingException {

        try {
            handlerIO.getHelper().updateDelegate(logbookLifecycleObjectGroupParameters);
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
