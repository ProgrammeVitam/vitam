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
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.io.IOUtils;

import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleObjectGroupParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.EngineResponse;
import fr.gouv.vitam.processing.common.model.OutcomeMessage;
import fr.gouv.vitam.processing.common.model.ProcessResponse;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.utils.BinaryObjectInfo;
import fr.gouv.vitam.worker.common.utils.IngestWorkflowConstants;
import fr.gouv.vitam.worker.common.utils.SedaUtilInfo;
import fr.gouv.vitam.worker.common.utils.SedaUtils;
import fr.gouv.vitam.worker.core.api.HandlerIO;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

/**
 * Check conformity handler
 */
public class CheckConformityActionHandler extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CheckConformityActionHandler.class);
    private static final String HANDLER_ID = "CheckConformity";
    LogbookOperationParameters parameters = LogbookParametersFactory.newLogbookOperationParameters();
    private static final LogbookLifeCyclesClient LOGBOOK_LIFECYCLE_CLIENT = LogbookLifeCyclesClientFactory.getInstance()
        .getLogbookLifeCyclesClient();

    public static final String JSON_EXTENSION = ".json";
    public static final String LIFE_CYCLE_EVENT_TYPE_PROCESS = "INGEST";
    public static final String UNIT_LIFE_CYCLE_CREATION_EVENT_TYPE =
        "Check SIP – Units – Lifecycle Logbook Creation – Création du journal du cycle de vie des units";
    private static final String OG_LIFE_CYCLE_CHECK_BDO_EVENT_TYPE =
        "Check SIP – ObjectGroups – Digest - Vérification de l’empreinte";
    private static final String LOGBOOK_LF_MAPS_PARSING_EXCEPTION_MSG = "Parse Object Groups/BDO Maps error";
    private static final String LOGBOOK_CLIENT_EXCEPTION = "Logbook client exception";
    public static final String TXT_EXTENSION = ".txt";
    private static final String CANNOT_READ_SEDA = "Can not read SEDA";
    private HandlerIO handlerIO;

    /**
     * Empty constructor CheckConformityActionHandler
     *
     */
    public CheckConformityActionHandler() {
        // empty constructor
    }

    /**
     * @return HANDLER_ID
     */
    public static final String getId() {
        return HANDLER_ID;
    }

    @Override
    public EngineResponse execute(WorkerParameters params, HandlerIO handler) throws ProcessingException {
        checkMandatoryParameters(params);

        final EngineResponse response = new ProcessResponse().setStatus(StatusCode.OK).setOutcomeMessages(HANDLER_ID,
            OutcomeMessage.CHECK_CONFORMITY_OK);
        handlerIO = handler;
        try {
            checkMandatoryIOParameter(handlerIO);
            final List<String> digestMessageInvalidList = checkConformityBinaryObject(params);
            if (!digestMessageInvalidList.isEmpty()) {
                response.setErrorNumber(digestMessageInvalidList.size());
                response.setStatus(StatusCode.KO);
                response.setOutcomeMessages(HANDLER_ID, OutcomeMessage.CHECK_CONFORMITY_KO);
            }
        } catch (ProcessingException | ContentAddressableStorageException e) {
            LOGGER.error(e);
            response.setStatus(StatusCode.KO);
            response.setOutcomeMessages(HANDLER_ID, OutcomeMessage.CHECK_CONFORMITY_KO);
        }
        return response;
    }

    /**
     * check the conformity of the binary object
     *
     * @param params worker parameter
     * @return List of the invalid digest message
     * @throws ProcessingException when error in execution
     * @throws ContentAddressableStorageException
     * @throws ContentAddressableStorageServerException
     * @throws ContentAddressableStorageNotFoundException
     */
    public List<String> checkConformityBinaryObject(WorkerParameters params)
        throws ProcessingException, ContentAddressableStorageNotFoundException,
        ContentAddressableStorageServerException, ContentAddressableStorageException {
        ParameterHelper.checkNullOrEmptyParameters(params);
        final String containerId = params.getContainerName();
        final WorkspaceClient client = WorkspaceClientFactory.create(params.getUrlWorkspace());



        final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        XMLEventReader reader = null;

        try (InputStream xmlFile = new FileInputStream((File) handlerIO.getInput().get(0))) {
            reader = xmlInputFactory.createXMLEventReader(xmlFile);
            final List<String> digestMessageInvalidList = compareDigestMessage(reader, client, containerId);
            reader.close();
            xmlFile.close();
            return digestMessageInvalidList;
        } catch (final XMLStreamException e) {
            LOGGER.error(CANNOT_READ_SEDA);
            throw new ProcessingException(e);
        } catch (final LogbookClientException e) {
            LOGGER.error(LOGBOOK_CLIENT_EXCEPTION, e);
            throw new ProcessingException(e);
        } catch (InvalidParseOperationException | IOException e) {
            LOGGER.error(LOGBOOK_LF_MAPS_PARSING_EXCEPTION_MSG);
            throw new ProcessingException(e);
        }
    }

    /**
     * Compare the digest message between the manifest.xml and related uri content in workspace container
     *
     *
     * @param evenReader manifest xml reader
     * @param client workspace client instance
     * @param containerId container id
     * @return a list of invalid digest messages
     * @throws XMLStreamException
     * @throws URISyntaxException
     * @throws ContentAddressableStorageNotFoundException
     * @throws ContentAddressableStorageServerException
     * @throws ContentAddressableStorageException
     * @throws InvalidParseOperationException
     * @throws LogbookClientBadRequestException
     * @throws LogbookClientAlreadyExistsException
     * @throws LogbookClientServerException
     * @throws LogbookClientNotFoundException
     * @throws IOException
     * @throws ProcessingException
     */
    private List<String> compareDigestMessage(XMLEventReader evenReader, WorkspaceClient client, String containerId)
        throws XMLStreamException, ContentAddressableStorageNotFoundException,
        ContentAddressableStorageServerException, ContentAddressableStorageException,
        InvalidParseOperationException, LogbookClientBadRequestException, LogbookClientAlreadyExistsException,
        LogbookClientServerException, LogbookClientNotFoundException, IOException, ProcessingException {

        final SedaUtilInfo sedaUtilInfo = SedaUtils.getBinaryObjectInfo(evenReader);
        final Map<String, BinaryObjectInfo> binaryObjectMap = sedaUtilInfo.getBinaryObjectMap();
        final List<String> digestMessageInvalidList = new ArrayList<>();

        final InputStream firstMapTmpFile = new FileInputStream((File) handlerIO.getInput().get(1));
        final Map<String, Object> binaryDataObjectIdToObjectGroupIdBackupMap = JsonHandler.getMapFromInputStream(firstMapTmpFile);
        final InputStream secondMapTmpFile = new FileInputStream((File) handlerIO.getInput().get(2));
        final Map<String, Object> objectGroupIdToGuidBackupMap = JsonHandler.getMapFromInputStream(secondMapTmpFile);

        for (final String mapKey : binaryObjectMap.keySet()) {

            // Update OG lifecycle
            final String bdoXmlId = binaryObjectMap.get(mapKey).getId();
            final String objectGroupId = (String) binaryDataObjectIdToObjectGroupIdBackupMap.get(bdoXmlId);
            LogbookLifeCycleObjectGroupParameters logbookLifeCycleObjGrpParam = null;
            if (objectGroupId != null) {
                final String objectGroupGuid = (String) objectGroupIdToGuidBackupMap.get(objectGroupId);
                logbookLifeCycleObjGrpParam = updateObjectGroupLifeCycleOnBdoCheck(objectGroupGuid, bdoXmlId,
                    containerId);
            }

            final String uri = binaryObjectMap.get(mapKey).getUri().toString();
            final String digestMessageManifest = binaryObjectMap.get(mapKey).getMessageDigest();
            final DigestType algo = binaryObjectMap.get(mapKey).getAlgo();
            final String digestMessage =
                client.computeObjectDigest(containerId, IngestWorkflowConstants.SEDA_FOLDER + "/" + uri, algo);
            if (!digestMessage.equals(digestMessageManifest)) {
                LOGGER.debug("Binary object Digest Message Invalid : " + uri);
                digestMessageInvalidList.add(digestMessageManifest);

                // Set KO status
                if (logbookLifeCycleObjGrpParam != null) {
                    logbookLifeCycleObjGrpParam.putParameterValue(LogbookParameterName.outcome,
                        StatusCode.WARNING.name());
                    logbookLifeCycleObjGrpParam.putParameterValue(LogbookParameterName.outcomeDetail,
                        StatusCode.WARNING.name());
                    logbookLifeCycleObjGrpParam.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                        OutcomeMessage.CHECK_BDO_KO.value() + " " + binaryObjectMap.get(mapKey).getId());
                    LOGBOOK_LIFECYCLE_CLIENT.update(logbookLifeCycleObjGrpParam);
                }
            } else {
                LOGGER.debug("Binary Object Digest Message Valid : " + uri);

                // Set OK status
                if (logbookLifeCycleObjGrpParam != null) {
                    logbookLifeCycleObjGrpParam.putParameterValue(LogbookParameterName.outcome,
                        StatusCode.OK.name());
                    logbookLifeCycleObjGrpParam.putParameterValue(LogbookParameterName.outcomeDetail,
                        StatusCode.OK.name());
                    logbookLifeCycleObjGrpParam.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                        OutcomeMessage.CHECK_BDO_OK.value());
                    LOGBOOK_LIFECYCLE_CLIENT.update(logbookLifeCycleObjGrpParam);
                }
            }
        }

        return digestMessageInvalidList;
    }


    private LogbookLifeCycleObjectGroupParameters updateObjectGroupLifeCycleOnBdoCheck(String objectGroupGuid,
        String bdoXmlId, String containerId) throws LogbookClientBadRequestException,
        LogbookClientAlreadyExistsException, LogbookClientServerException, LogbookClientNotFoundException {

        final LogbookLifeCycleObjectGroupParameters logbookLifecycleObjectGroupParameters =
            (LogbookLifeCycleObjectGroupParameters) initLogbookLifeCycleParameters(
                objectGroupGuid, true);

        logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.eventIdentifierProcess,
            containerId);
        logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.eventIdentifier, bdoXmlId);
        logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.eventTypeProcess,
            LIFE_CYCLE_EVENT_TYPE_PROCESS);
        logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.eventType,
            OG_LIFE_CYCLE_CHECK_BDO_EVENT_TYPE);
        logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcome,
            StatusCode.STARTED.toString());
        logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcomeDetail,
            StatusCode.STARTED.toString());
        logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
            OutcomeMessage.CHECK_BDO.value());
        LOGBOOK_LIFECYCLE_CLIENT.update(logbookLifecycleObjectGroupParameters);

        return logbookLifecycleObjectGroupParameters;
    }

    private LogbookParameters initLogbookLifeCycleParameters(String guid, boolean isObjectGroup) {
        final LogbookParameters logbookLifeCycleParameters =
            LogbookParametersFactory.newLogbookLifeCycleObjectGroupParameters();
        logbookLifeCycleParameters.putParameterValue(LogbookParameterName.objectIdentifier, guid);
        return logbookLifeCycleParameters;
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // TODO Add Workspace:SIP/manifest.xml and check it

    }
}
