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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.io.CharStreams;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleObjectGroupParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOutcome;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCycleClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.EngineResponse;
import fr.gouv.vitam.processing.common.model.ProcessResponse;
import fr.gouv.vitam.processing.common.model.StatusCode;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.StorageCollectionType;
import fr.gouv.vitam.storage.engine.client.exception.StorageClientException;
import fr.gouv.vitam.storage.engine.common.model.request.CreateObjectDescription;
import fr.gouv.vitam.storage.engine.common.model.response.StoredInfoResult;
import fr.gouv.vitam.worker.common.utils.BinaryObjectInfo;
import fr.gouv.vitam.worker.common.utils.IngestWorkflowConstants;
import fr.gouv.vitam.worker.common.utils.SedaUtilInfo;
import fr.gouv.vitam.worker.common.utils.SedaUtils;
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

    private static final String HANDLER_ID = "StoreObjectGroup";
    private static final String OG_LIFE_CYCLE_STORE_BDO_EVENT_TYPE = "Stockage des groupes d'objets - Stockage d'objet";
    private static final String SIP = "SIP/";

    //TODO should not be a private attribute -> to refactor
    private LogbookLifeCycleObjectGroupParameters logbookLifecycleObjectGroupParameters = LogbookParametersFactory
        .newLogbookLifeCycleObjectGroupParameters();
    private static final StorageClient STORAGE_CLIENT = StorageClientFactory.getInstance().getStorageClient();
    private static final LogbookLifeCycleClient LOGBOOK_LIFECYCLE_CLIENT = LogbookLifeCyclesClientFactory.getInstance()
        .getLogbookLifeCyclesClient();

    private static final String DEFAULT_TENANT = "0";
    private static final String DEFAULT_STRATEGY = "default";
    public static final String JSON_EXTENSION = ".json";
    private static final String CANNOT_READ_SEDA = "Can not read SEDA";
    private static final String MANIFEST_NOT_FOUND = "Manifest.xml Not Found";

    private static final String LOGBOOK_LF_BAD_REQUEST_EXCEPTION_MSG = "LogbookClient Unsupported request";
    private static final String LOGBOOK_LF_RESOURCE_NOT_FOUND_EXCEPTION_MSG = "Logbook LifeCycle resource not found";
    private static final String LOGBOOK_SERVER_INTERNAL_EXCEPTION_MSG = "Logbook Server internal error";
    private static final String LOGBOOK_LF_STORAGE_MSG = "Stockage des objets";
    private static final String LOGBOOK_LF_STORAGE_OK_MSG = "Stockage des objets réalisé avec succès";
    private static final String LOGBOOK_LF_STORAGE_KO_MSG = "Stockage des objets en erreur";
    private static final String LOGBOOK_LF_STORAGE_BDO_MSG = "Stockage de l'objet";
    private static final String LOGBOOK_LF_STORAGE_BDO_KO_MSG = "Stockage de l'objet en erreur";

    private HandlerIO handlerIO;

    private HandlerIO handlerInitialIOList;

    /**
     * Constructor with parameter SedaUtilsFactory
     *
     * @param factory the sedautils factory
     */
    public StoreObjectGroupActionHandler() {
        handlerInitialIOList = new HandlerIO("");
        handlerInitialIOList.addInput(File.class);
        handlerInitialIOList.addInput(File.class);
    }

    /**
     * @return HANDLER_ID
     */
    public static final String getId() {
        return HANDLER_ID;
    }


    @Override
    public EngineResponse execute(WorkerParameters params, HandlerIO action) {
        checkMandatoryParameters(params);
        LOGGER.info("StoreObjectGroupActionHandler running ...");
        handlerIO = action;

        final EngineResponse response = new ProcessResponse().setStatus(StatusCode.OK);

        try {
            checkMandatoryParamerter(handlerIO);
            // Update lifecycle of object group : STARTED
            updateLifeCycleParametersLogbookByStep(params, SedaUtils.LIFE_CYCLE_EVENT_TYPE_PROCESS);
            updateLifeCycle();

            Map<String, BinaryObjectInfo> storageObjectInfos = retrieveStorageInformationForObjectGroup(params);
            for (Map.Entry<String, BinaryObjectInfo> storageObjectInfo : storageObjectInfos.entrySet()) {
                storeObject(params, storageObjectInfo.getKey(), storageObjectInfo.getValue());
            }
        } catch (final ProcessingException e) {
            LOGGER.error(e);
            response.setStatus(StatusCode.KO);
        }

        // Update lifecycle of object group : OK/KO
        try {
            updateLifeCycleParametersLogbookByStep(params, SedaUtils.LIFE_CYCLE_EVENT_TYPE_PROCESS);
            logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcome,
                response.getStatus().toString());
            if (StatusCode.OK.equals(response.getStatus())) {
                logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcomeDetail,
                    LogbookOutcome.OK.name());
                logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                    LOGBOOK_LF_STORAGE_OK_MSG);
            } else {
                logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcomeDetail,
                    LogbookOutcome.KO.name());
                logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                    LOGBOOK_LF_STORAGE_KO_MSG);
            }
            updateLifeCycle();
        } catch (ProcessingException e) {
            LOGGER.warn(e);
            if (StatusCode.OK.equals(response.getStatus())) {
                response.setStatus(StatusCode.WARNING);
            }
        }

        LOGGER.debug("StoreObjectGroupActionHandler response: " + response.getStatus().name());
        return response;
    }

    /**
     * Store a binary data object with the storage engine.
     * 
     * @param params worker parameters
     * @param objectGUID the object guid
     * @param storageObjectInfo informations on the binary data object needed by the storage engine
     * @throws ProcessingException throws when error occurs
     */
    private void storeObject(WorkerParameters params, String objectGUID, BinaryObjectInfo storageObjectInfo)
        throws ProcessingException {
        LOGGER.debug("Storing object with guid: " + objectGUID);
        try {
            // update lifecycle of objectGroup with detail of object : STARTED
            updateLifeCycleParametersLogbookForBdo(params, storageObjectInfo.getId());
            updateLifeCycle();

            // store binary data object
            CreateObjectDescription description = new CreateObjectDescription();
            description.setWorkspaceContainerGUID(params.getContainerName());
            description.setWorkspaceObjectURI(SIP + storageObjectInfo.getUri().toString());
            StoredInfoResult result =
                STORAGE_CLIENT.storeFileFromWorkspace(DEFAULT_TENANT, DEFAULT_STRATEGY, StorageCollectionType.OBJECTS,
                    objectGUID, description);

            // update lifecycle of objectGroup with detail of object : OK
            logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcome,
                StatusCode.OK.toString());
            logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcomeDetail,
                StatusCode.OK.toString());
            logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                result.getInfo());
            updateLifeCycle();
        } catch (StorageClientException e) {
            LOGGER.error(e);
            // update lifecycle of objectGroup with detail of object : KO
            logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcome,
                StatusCode.KO.toString());
            logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcomeDetail,
                StatusCode.KO.toString());
            logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                LOGBOOK_LF_STORAGE_BDO_KO_MSG);
            updateLifeCycle();
            throw new ProcessingException(e);
        }
    }

    /**
     * Update the lifecycle with the current ObjectGroup lifecycle parameters.
     * 
     * @throws ProcessingException throws when error occurs
     */
    private void updateLifeCycle() throws ProcessingException {

        try {
            LOGBOOK_LIFECYCLE_CLIENT.update(logbookLifecycleObjectGroupParameters);
        } catch (LogbookClientBadRequestException e) {
            LOGGER.error(LOGBOOK_LF_BAD_REQUEST_EXCEPTION_MSG, e);
            throw new ProcessingException(e);
        } catch (LogbookClientServerException e) {
            LOGGER.error(LOGBOOK_SERVER_INTERNAL_EXCEPTION_MSG, e);
            throw new ProcessingException(e);
        } catch (LogbookClientNotFoundException e) {
            LOGGER.error(LOGBOOK_LF_RESOURCE_NOT_FOUND_EXCEPTION_MSG, e);
            throw new ProcessingException(e);
        }
    }

    /**
     * Update current lifecycle parameters with ObjectGroup lifecycle parameters. <br>
     * 
     * @param params worker parameters
     * @param typeProcess type process event
     * @return updated parameters
     */
    private void updateLifeCycleParametersLogbookByStep(WorkerParameters params, String typeProcess) {
        String extension = FilenameUtils.getExtension(params.getObjectName());
        logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.objectIdentifier,
            params.getObjectName().replace("." + extension, ""));
        logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.eventIdentifierProcess,
            params.getContainerName());
        logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.eventIdentifier,
            GUIDFactory.newGUID().toString());
        logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.eventTypeProcess,
            typeProcess);
        logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.eventType,
            params.getCurrentStep());
        logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcome,
            LogbookOutcome.STARTED.toString());
        logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcomeDetail,
            LogbookOutcome.STARTED.toString());
        logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
            LOGBOOK_LF_STORAGE_MSG);
    }

    /**
     * Update current ObjectGroup lifecycle parameters with binary data object lifecycle parameters. <br>
     * TODO : for now we use the SEDA BinaryDataObject id, but shoud be the binary data object GUID when it exists
     * 
     * @param params worker parameters
     * @param bdoId binary data object id
     */
    private void updateLifeCycleParametersLogbookForBdo(WorkerParameters params, String bdoId) {
        String extension = FilenameUtils.getExtension(params.getObjectName());
        logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.eventIdentifierProcess,
            params.getObjectName().replace("." + extension, ""));
        logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.eventIdentifier, bdoId);
        logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.eventType,
            OG_LIFE_CYCLE_STORE_BDO_EVENT_TYPE);
        logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcome,
            LogbookOutcome.STARTED.toString());
        logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcomeDetail,
            LogbookOutcome.STARTED.toString());
        logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
            LOGBOOK_LF_STORAGE_BDO_MSG);
    }


    /**
     * Retrieve the binary data object infos linked to the object group. <br>
     * TODO : should not need to parse the manifest.xml to link a binary data object present in workspace to an object
     * group. To refactor when Object Group and Binary Data Object link is defined. TODO : during the next refactoring
     * of Sedautils, it has to be refactored to StoreObjectGroupActionHandler
     * 
     * @param params worker parameters
     * @return tha map of binary data object information with their object GUID as key
     * @throws ProcessingException throws when error occurs
     */
    public Map<String, BinaryObjectInfo> retrieveStorageInformationForObjectGroup(WorkerParameters params)
        throws ProcessingException {
        ParameterHelper.checkNullOrEmptyParameters(params);
        final String containerId = params.getContainerName();
        final String objectName = params.getObjectName();
        ParametersChecker.checkParameter("Container id is a mandatory parameter", containerId);
        ParametersChecker.checkParameter("ObjectName id is a mandatory parameter", objectName);
        // TODO : whould use worker configuration instead of the processing configuration
        final WorkspaceClient workspaceClient =
            WorkspaceClientFactory.create(params.getUrlWorkspace());

        // retrieve SEDA FILE and get the list of objectsDatas
        Map<String, BinaryObjectInfo> binaryObjectsToStore = new HashMap<>();
        // Get binary objects informations of the SIP
        SedaUtilInfo sedaUtilInfo = getSedaUtilInfo(workspaceClient, containerId);
        // Get objectGroup objects ids
        final JsonNode jsonOG = getJsonFromWorkspace(workspaceClient, containerId, (File) handlerIO.getInput().get(1));

        // Filter on objectGroup objects ids to retrieve only binary objects informations linked to the ObjectGroup
        JsonNode qualifiers = jsonOG.get("_qualifiers");
        if (qualifiers == null) {
            return binaryObjectsToStore;
        }

        List<JsonNode> versions = qualifiers.findValues("versions");
        if (versions == null || versions.isEmpty()) {
            return binaryObjectsToStore;
        }

        String objectIdToGuidStoredContent;
        try {
            InputStream objectIdToGuidMapFile = new FileInputStream((File) handlerIO.getInput().get(0));
            objectIdToGuidStoredContent = IOUtils.toString(objectIdToGuidMapFile, "UTF-8");
            LOGGER.info(objectIdToGuidStoredContent);
        } catch (IOException e) {
            LOGGER.error(e);
            throw new ProcessingException(e);
        }
        Map<String, Object> objectIdToGuidStoredMap = null;
        try {
            objectIdToGuidStoredMap = JsonHandler.getMapFromString(objectIdToGuidStoredContent);
        } catch (InvalidParseOperationException e) {
            LOGGER.error(e);
            throw new ProcessingException(e);
        }

        Map<Object, String> guidToObjectIdMap =
            objectIdToGuidStoredMap.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

        for (JsonNode version : versions) {
            for (JsonNode binaryObject : version) {
                String binaryObjectId = guidToObjectIdMap.get(binaryObject.get("_id").asText());
                Optional<Entry<String, BinaryObjectInfo>> objectEntry =
                    sedaUtilInfo.getBinaryObjectMap().entrySet().stream()
                    .filter(entry -> entry.getKey().equals(binaryObjectId)).findFirst();
                if (objectEntry.isPresent()) {
                    binaryObjectsToStore.put(binaryObject.get("_id").asText(), objectEntry.get().getValue());
                }
            }
        }

        return binaryObjectsToStore;

    }


    /**
     * Parse SEDA file manifest.xml to retrieve all its binary data objects informations as a SedaUtilInfo.
     * 
     * @param workspaceClient workspace connector
     * @param containerId container id
     * @return SedaUtilInfo
     * @throws ProcessingException throws when error occurs
     */
    private SedaUtilInfo getSedaUtilInfo(WorkspaceClient workspaceClient, String containerId)
        throws ProcessingException {
        InputStream xmlFile = null;
        try {
            xmlFile = workspaceClient.getObject(containerId,
                IngestWorkflowConstants.SEDA_FOLDER + "/" + IngestWorkflowConstants.SEDA_FILE);
        } catch (ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException e) {
            LOGGER.error(MANIFEST_NOT_FOUND);
            IOUtils.closeQuietly(xmlFile);
            throw new ProcessingException(e);
        }

        final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();

        SedaUtilInfo sedaUtilInfo = null;
        XMLEventReader reader = null;
        try {
            reader = xmlInputFactory.createXMLEventReader(xmlFile);
            sedaUtilInfo = SedaUtils.getBinaryObjectInfo(reader);
            return sedaUtilInfo;
        } catch (final XMLStreamException e) {
            LOGGER.error(CANNOT_READ_SEDA);
            throw new ProcessingException(e);
        } finally {
            IOUtils.closeQuietly(xmlFile);
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (XMLStreamException e) {
                // nothing to throw
                LOGGER.info("Can not close XML reader SEDA", e);
            }
        }

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
    private JsonNode getJsonFromWorkspace(WorkspaceClient workspaceClient, String containerId, File file)
        throws ProcessingException {
        try {
            final String inputStreamString = CharStreams.toString(new InputStreamReader( new FileInputStream(file), "UTF-8"));
            return JsonHandler.getFromString(inputStreamString);

        } catch (InvalidParseOperationException | IOException e) {
            LOGGER.debug("Json wrong format", e);
            throw new ProcessingException(e);
        }
    }

    @Override
    public void checkMandatoryParamerter(HandlerIO handler) throws ProcessingException {
        if (handlerIO.getOutput().size() != handlerInitialIOList.getOutput().size()) {
            throw new ProcessingException(HandlerIO.NOT_ENOUGH_PARAM);
        } else if (!HandlerIO.checkHandlerIO(handlerIO, this.handlerInitialIOList)) {
            throw new ProcessingException(HandlerIO.NOT_CONFORM_PARAM);
        }
    }

}
