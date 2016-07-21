/**
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
 */
package fr.gouv.vitam.processing.worker.handler;

import java.util.List;

import org.apache.commons.io.FilenameUtils;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleObjectGroupParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOutcome;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCycleClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.EngineResponse;
import fr.gouv.vitam.processing.common.model.ProcessResponse;
import fr.gouv.vitam.processing.common.model.StatusCode;
import fr.gouv.vitam.processing.common.model.WorkParams;
import fr.gouv.vitam.processing.common.utils.BinaryObjectInfo;
import fr.gouv.vitam.processing.common.utils.SedaUtils;
import fr.gouv.vitam.processing.common.utils.SedaUtilsFactory;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.StorageCollectionType;
import fr.gouv.vitam.storage.engine.client.exception.StorageClientException;
import fr.gouv.vitam.storage.engine.common.model.request.CreateObjectDescription;
import fr.gouv.vitam.storage.engine.common.model.response.StoredInfoResult;

/**
 * StoreObjectGroup Handler.<br>
 */
public class StoreObjectGroupActionHandler extends ActionHandler {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IndexObjectGroupActionHandler.class);

    private static final String HANDLER_ID = "StoreObjectGroup";
    private static final String OG_LIFE_CYCLE_STORE_BDO_EVENT_TYPE = "STORE_BDO";

    private final SedaUtilsFactory sedaUtilsFactory;
    //TODO should not be a private attribute -> to refactor
    private LogbookLifeCycleObjectGroupParameters logbookLifecycleObjectGroupParameters = LogbookParametersFactory
        .newLogbookLifeCycleObjectGroupParameters();
    private static final StorageClient STORAGE_CLIENT = StorageClientFactory.getInstance().getStorageClient();
    private static final LogbookLifeCycleClient LOGBOOK_LIFECYCLE_CLIENT = LogbookLifeCyclesClientFactory.getInstance()
        .getLogbookLifeCyclesClient();
    LogbookOperationParameters parameters = LogbookParametersFactory.newLogbookOperationParameters();

    private static final String DEFAULT_TENANT = "0";
    private static final String DEFAULT_STRATEGY = "default";

    private static final String LOGBOOK_LF_BAD_REQUEST_EXCEPTION_MSG = "LogbookClient Unsupported request";
    private static final String LOGBOOK_LF_RESOURCE_NOT_FOUND_EXCEPTION_MSG = "Logbook LifeCycle resource not found";
    private static final String LOGBOOK_SERVER_INTERNAL_EXCEPTION_MSG = "Logbook Server internal error";
    private static final String LOGBOOK_LF_STORAGE_OK_MSG = "Stockage des objets réalisé avec succès";
    private static final String LOGBOOK_LF_STORAGE_KO_MSG = "Stockage des objets en erreur";
    private static final String LOGBOOK_LF_STORAGE_BDO_KO_MSG = "Stockage de l'objet en erreur";

    /**
     * Constructor with parameter SedaUtilsFactory
     *
     * @param factory
     */
    public StoreObjectGroupActionHandler(SedaUtilsFactory factory) {
        sedaUtilsFactory = factory;
    }

    /**
     * @return HANDLER_ID
     */
    public static final String getId() {
        return HANDLER_ID;
    }


    @Override
    public EngineResponse execute(WorkParams params) {
        ParametersChecker.checkParameter("params is a mandatory parameter", params);
        ParametersChecker.checkParameter("ServerConfiguration is a mandatory parameter",
            params.getServerConfiguration());
        LOGGER.info("StoreObjectGroupActionHandler running ...");

        final EngineResponse response = new ProcessResponse().setStatus(StatusCode.OK);

        final SedaUtils sedaUtils = sedaUtilsFactory.create();

        try {
            // Update lifecycle of object group : STARTED
            updateLifeCycleParametersLogbookByStep(params, SedaUtils.LIFE_CYCLE_EVENT_TYPE_PROCESS);
            updateLifeCycle();

            List<BinaryObjectInfo> storageObjectInfos = sedaUtils.retrieveStorageInformationForObjectGroup(params);
            for (BinaryObjectInfo storageObjectInfo : storageObjectInfos) {
                storeObject(params, storageObjectInfo);
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
                logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                    LOGBOOK_LF_STORAGE_OK_MSG);
            } else {
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

        LOGGER.info("StoreObjectGroupActionHandler response: " + response.getStatus().value());
        return response;
    }

    /**
     * Store a binary data object with the storage engine.
     * 
     * @param params worker parameters
     * @param storageObjectInfo informations on the binary data object needed by the storage engine
     * @throws ProcessingException throws when error occurs
     */
    private void storeObject(WorkParams params, BinaryObjectInfo storageObjectInfo)
        throws ProcessingException {
        try {
            // update lifecycle of objectGroup with detail of object : STARTED
            updateLifeCycleParametersLogbookForBdo(params, storageObjectInfo.getId());
            updateLifeCycle();

            // store binary data object
            CreateObjectDescription description = new CreateObjectDescription();
            description.setWorkspaceContainerGUID(params.getContainerName());
            description.setWorkspaceObjectURI(storageObjectInfo.getUri().toString());
            StoredInfoResult result =
                STORAGE_CLIENT.storeFileFromWorkspace(DEFAULT_TENANT, DEFAULT_STRATEGY, StorageCollectionType.OBJECTS,
                    storageObjectInfo.getMessageDigest(), description);

            // update lifecycle of objectGroup with detail of object : OK
            logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcome,
                StatusCode.OK.toString());
            logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                result.getInfo());
            updateLifeCycle();
        } catch (StorageClientException e) {
            LOGGER.error(e);
            // update lifecycle of objectGroup with detail of object : KO
            logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcome,
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
    private void updateLifeCycleParametersLogbookByStep(WorkParams params, String typeProcess) {
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
            LogbookOutcome.STARTED.toString());
    }

    /**
     * Update current ObjectGroup lifecycle parameters with binary data object lifecycle parameters. <br>
     * TODO : for now we use the SEDA BinaryDataObject id, but shoud be the binary data object GUID when it exists
     * 
     * @param params worker parameters
     * @param bdoId binary data object id
     */
    private void updateLifeCycleParametersLogbookForBdo(WorkParams params, String bdoId) {
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
            LogbookOutcome.STARTED.toString());
    }

}
