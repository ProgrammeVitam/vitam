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
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.odysseus.staxon.json.JsonXMLConfig;
import de.odysseus.staxon.json.JsonXMLConfigBuilder;
import de.odysseus.staxon.json.JsonXMLOutputFactory;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.CycleFoundException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.graph.DirectedCycle;
import fr.gouv.vitam.common.graph.DirectedGraph;
import fr.gouv.vitam.common.graph.Graph;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleObjectGroupParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleUnitParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOutcome;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCycleClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.EngineResponse;
import fr.gouv.vitam.processing.common.model.OutcomeMessage;
import fr.gouv.vitam.processing.common.model.ProcessResponse;
import fr.gouv.vitam.processing.common.model.StatusCode;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.utils.IngestWorkflowConstants;
import fr.gouv.vitam.worker.core.api.HandlerIO;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

/**
 *
 * ExtractContentActionHandler handler class used to extract metaData .Create and put a new file (metadata extracted)
 * json.json into container GUID
 *
 */
public class ExtractSedaActionHandler extends ActionHandler {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ExtractSedaActionHandler.class);
    private static final LogbookLifeCycleClient LOGBOOK_LIFECYCLE_CLIENT = LogbookLifeCyclesClientFactory.getInstance()
        .getLogbookLifeCyclesClient();
    private static final String HANDLER_ID = "ExtractSeda";
    private HandlerIO handlerIO;

    private static final String NAMESPACE_URI = "fr:gouv:culture:archivesdefrance:seda:v2.0";
    private static final String XML_EXTENSION = ".xml";
    public static final String JSON_EXTENSION = ".json";
    private static final String BINARY_DATA_OBJECT = "BinaryDataObject";
    private static final String DATA_OBJECT_GROUPID = "DataObjectGroupId";
    private static final String ARCHIVE_UNIT = "ArchiveUnit";
    private static final String BINARY_MASTER = "BinaryMaster";
    private static final String FILE_INFO = "FileInfo";
    private static final String METADATA = "Metadata";
    private static final String DATA_OBJECT_GROUP_REFERENCEID = "DataObjectGroupReferenceId";
    private static final String TAG_OG = "_og";
    private static final String TAG_ID = "_id";
    public static final String LIFE_CYCLE_EVENT_TYPE_PROCESS = "INGEST";
    public static final String UNIT_LIFE_CYCLE_CREATION_EVENT_TYPE =
        "Check SIP – Units – Lifecycle Logbook Creation – Création du journal du cycle de vie des units";
    private static final String OG_LIFE_CYCLE_CREATION_EVENT_TYPE =
        "Check SIP – ObjectGroups – Lifecycle Logbook Creation – Création du journal du cycle de vie des groupes d’objets";
    private static final String LOGBOOK_LF_BAD_REQUEST_EXCEPTION_MSG = "LogbookClient Unsupported request";
    private static final String LOGBOOK_LF_OBJECT_EXISTS_EXCEPTION_MSG = "LifeCycle Object already exists";
    private static final String LOGBOOK_LF_RESOURCE_NOT_FOUND_EXCEPTION_MSG = "Logbook LifeCycle resource not found";
    private static final String LOGBOOK_SERVER_INTERNAL_EXCEPTION_MSG = "Logbook Server internal error";
    public static final String TXT_EXTENSION = ".txt";
    private static final String LEVEL = "level_";

    private static final String ARCHIVE_UNIT_ELEMENT_ID_ATTRIBUTE = "id";
    private static final String ARCHIVE_UNIT_REF_ID_TAG = "ArchiveUnitRefId";
    public static final String UP_FIELD = "_up";
    private static final String INVALID_INGEST_TREE_EXCEPTION_MSG =
        "INGEST_TREE invalid, can not save to temporary file";
    private static final String GRAPH_CYCLE_MSG =
        "The Archive Unit graph in the SEDA file has a cycle";
    private static final String TMP_FOLDER = "vitam" + File.separator + "temp";
    private static final String INGEST_LEVEL_STACK = "ingestLevelStack.json";
    private static final String CYCLE_FOUND_EXCEPTION = "Seda has an archive unit cycle ";
    private static final String SAVE_ARCHIVE_ID_TO_GUID_IOEXCEPTION_MSG =
        "Can not save unitToGuidMap to temporary file";
    private static final String WORKSPACE_MANDATORY_MSG = "WorkspaceClient is a mandatory parameter";
    private static final String FILE_COULD_NOT_BE_DELETED_MSG = "File could not be deleted";
    private static final String CANNOT_READ_SEDA = "Can not read SEDA";
    private static final String MANIFEST_NOT_FOUND = "Manifest.xml Not Found";

    private Map<String, String> binaryDataObjectIdToGuid;
    private Map<String, String> objectGroupIdToGuid;
    private Map<String, String> objectGroupIdToGuidTmp;
    private Map<String, String> unitIdToGuid;

    private Map<String, String> binaryDataObjectIdToObjectGroupId;
    private Map<String, List<String>> objectGroupIdToBinaryDataObjectId;
    private Map<String, String> unitIdToGroupId;
    private Map<String, List<String>> objectGroupIdToUnitId;

    private Map<String, LogbookParameters> guidToLifeCycleParameters;
    
    private static final int HANDLER_IO_PARAMETER_NUMBER = 7;
    private HandlerIO handlerInitialIOList;
    /**
     * Constructor with parameter SedaUtilsFactory
     *
     * @param factory SedaUtils factory
     */
    public ExtractSedaActionHandler() {
        binaryDataObjectIdToGuid = new HashMap<>();
        objectGroupIdToGuid = new HashMap<>();
        objectGroupIdToGuidTmp = new HashMap<>();
        unitIdToGuid = new HashMap<>();
        binaryDataObjectIdToObjectGroupId = new HashMap<>();
        objectGroupIdToBinaryDataObjectId = new HashMap<>();
        unitIdToGroupId = new HashMap<>();
        objectGroupIdToUnitId = new HashMap<>();
        guidToLifeCycleParameters = new HashMap<>();
        handlerInitialIOList = new HandlerIO("");
        for (int i = 0; i < HANDLER_IO_PARAMETER_NUMBER; i++) {
            handlerInitialIOList.addOutput(String.class);
        }
        
    }

    /**
     * @return HANDLER_ID
     */
    public static final String getId() {
        return HANDLER_ID;
    }

    @Override
    public EngineResponse execute(WorkerParameters params, HandlerIO ioParam) {
        checkMandatoryParameters(params);
        handlerIO = ioParam;
        LOGGER.info("ExtractContentActionHandler running ...");
        final EngineResponse response = new ProcessResponse().setStatus(StatusCode.OK).setOutcomeMessages(HANDLER_ID, OutcomeMessage.EXTRACT_MANIFEST_OK);

        try {
            checkMandatoryIOParameter(handlerIO);
            checkMandatoryIOParameter(ioParam);
            extractSEDA(params);
        } catch (final ProcessingException e) {
            LOGGER.debug("ProcessingException", e);
            response.setStatus(StatusCode.KO).setOutcomeMessages(HANDLER_ID, OutcomeMessage.EXTRACT_MANIFEST_KO);
        } finally {
            //Empty all maps  
            binaryDataObjectIdToGuid = new HashMap<>();
            objectGroupIdToGuid = new HashMap<>();
            objectGroupIdToGuidTmp = new HashMap<>();
            unitIdToGuid = new HashMap<>();
            binaryDataObjectIdToObjectGroupId = new HashMap<>();
            objectGroupIdToBinaryDataObjectId = new HashMap<>();
            unitIdToGroupId = new HashMap<>();
            objectGroupIdToUnitId = new HashMap<>();
            guidToLifeCycleParameters = new HashMap<>();
        }

        LOGGER.debug("ExtractSedaActionHandler response: " + response.getStatus().name());
        return response;
    }
    
    /**
     * Split Element from InputStream and write it to workspace
     *
     * @param params parameters of workspace server
     * @throws ProcessingException throw when can't read or extract element from SEDA
     */
    public void extractSEDA(WorkerParameters params) throws ProcessingException {
        ParameterHelper.checkNullOrEmptyParameters(params);
        final String containerId = params.getContainerName();
        // TODO : whould use worker configuration instead of the processing configuration        
        final WorkspaceClient client = WorkspaceClientFactory.create(params.getUrlWorkspace());
        extractSEDAWithWorkspaceClient(client, containerId);
    }
    
    private void extractSEDAWithWorkspaceClient(WorkspaceClient client, String containerId) throws ProcessingException {
        ParametersChecker.checkParameter(WORKSPACE_MANDATORY_MSG, client);
        ParametersChecker.checkParameter("ContainerId is a mandatory parameter", containerId);

        /**
         * Retrieves SEDA
         **/
        InputStream xmlFile = null;
        try {
            xmlFile = client.getObject(containerId,
                IngestWorkflowConstants.SEDA_FOLDER + "/" + IngestWorkflowConstants.SEDA_FILE);
        } catch (ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException e) {
            LOGGER.error(MANIFEST_NOT_FOUND);
            throw new ProcessingException(e);
        }

        final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        XMLEventReader reader = null;
        final QName dataObjectName = new QName(NAMESPACE_URI, BINARY_DATA_OBJECT);
        final QName unitName = new QName(NAMESPACE_URI, ARCHIVE_UNIT);

        // Archive Unit Tree
        ObjectNode archiveUnitTree = JsonHandler.createObjectNode();

        try {
            reader = xmlInputFactory.createXMLEventReader(xmlFile);
            while (true) {
                final XMLEvent event = reader.nextEvent();
                if (event.isStartElement()) {
                    final StartElement element = event.asStartElement();
                    if (element.getName().equals(unitName)) {
                        writeArchiveUnitToWorkspace(client, containerId, reader, element, archiveUnitTree, IngestWorkflowConstants.ARCHIVE_UNIT_FOLDER);
                    } else if (element.getName().equals(dataObjectName)) {
                        String objectGroupGuid = writeBinaryDataObjectInLocal(reader, element, containerId);
                        if (guidToLifeCycleParameters.get(objectGroupGuid) != null) {
                            guidToLifeCycleParameters.get(objectGroupGuid).setStatus(LogbookOutcome.OK);
                            guidToLifeCycleParameters.get(objectGroupGuid)
                                .putParameterValue(LogbookParameterName.outcomeDetail, LogbookOutcome.OK.name());
                            guidToLifeCycleParameters.get(objectGroupGuid).putParameterValue(
                                LogbookParameterName.outcomeDetailMessage,
                                OutcomeMessage.CREATE_LOGBOOK_LIFECYCLE_OK.value());
                            LOGBOOK_LIFECYCLE_CLIENT.update(guidToLifeCycleParameters.get(objectGroupGuid));
                        }
                    }
                }
                if (event.isEndDocument()) {
                    break;
                }
            }
            reader.close();

            // Update created Unit life cycles
            for (String unitGuid : unitIdToGuid.values()) {
                if (guidToLifeCycleParameters.get(unitGuid) != null) {
                    guidToLifeCycleParameters.get(unitGuid).setStatus(LogbookOutcome.OK);
                    guidToLifeCycleParameters.get(unitGuid)
                        .putParameterValue(LogbookParameterName.outcomeDetail, LogbookOutcome.OK.name());
                    guidToLifeCycleParameters.get(unitGuid).putParameterValue(
                        LogbookParameterName.outcomeDetailMessage,
                        OutcomeMessage.CREATE_LOGBOOK_LIFECYCLE_OK.value());
                    LOGBOOK_LIFECYCLE_CLIENT.update(guidToLifeCycleParameters.get(unitGuid));
                }
            }

            // Save Archive Unit Tree
            // Create temporary file to store archive unit tree
            final File archiveTreeTmpFile = PropertiesUtils
                .fileFromTmpFolder(
                    IngestWorkflowConstants.ARCHIVE_TREE_TMP_FILE_NAME_PREFIX + containerId + JSON_EXTENSION);
            JsonHandler.writeAsFile(archiveUnitTree, archiveTreeTmpFile);
            transferFileFromTmpIntoWorkspace(client,
                IngestWorkflowConstants.ARCHIVE_TREE_TMP_FILE_NAME_PREFIX + containerId + JSON_EXTENSION,
                (String) handlerIO.getOutput().get(1),
                containerId, true);
            // check cycle and create level stack; will be used when indexing unit
            // 1-detect cycle : if graph has a cycle throw CycleFoundException
            DirectedCycle directedCycle = new DirectedCycle(new DirectedGraph(archiveUnitTree));
            if (directedCycle.isCyclic()) {
                throw new CycleFoundException(GRAPH_CYCLE_MSG);
            }
            // Save unitToGuid Map
            saveMap(containerId, unitIdToGuid, (String) handlerIO.getOutput().get(2), client, true);

            // 2- create graph and create level
            createIngestLevelStackFile(client, containerId, new Graph(archiveUnitTree).getGraphWithLongestPaths(), (String) handlerIO.getOutput().get(0));

            checkArchiveUnitIdReference();
            saveObjectGroupsToWorkspace(client, containerId);

            // Save binaryDataObjectIdToGuid Map
            saveMap(containerId, binaryDataObjectIdToGuid, (String) handlerIO.getOutput().get(4), client, true);
            
         // Save objectGroupIdToUnitId Map
            saveMap(containerId, objectGroupIdToUnitId, (String) handlerIO.getOutput().get(6), client, true);

        } catch (final XMLStreamException e) {
            LOGGER.error(CANNOT_READ_SEDA);
            throw new ProcessingException(e);
        } catch (LogbookClientBadRequestException e) {
            LOGGER.error(LOGBOOK_LF_BAD_REQUEST_EXCEPTION_MSG, e);
            throw new ProcessingException(e);
        } catch (LogbookClientNotFoundException e) {
            LOGGER.error(LOGBOOK_LF_RESOURCE_NOT_FOUND_EXCEPTION_MSG, e);
            throw new ProcessingException(e);
        } catch (LogbookClientServerException e) {
            LOGGER.error(LOGBOOK_SERVER_INTERNAL_EXCEPTION_MSG, e);
            throw new ProcessingException(e);
        } catch (InvalidParseOperationException e) {
            LOGGER.error(INVALID_INGEST_TREE_EXCEPTION_MSG, e);
            throw new ProcessingException(e);
        } catch (CycleFoundException e) {
            LOGGER.error(CYCLE_FOUND_EXCEPTION, e);
            throw new ProcessingException(e);
        } catch (IOException e) {
            LOGGER.error(SAVE_ARCHIVE_ID_TO_GUID_IOEXCEPTION_MSG, e);
            throw new ProcessingException(e);
        }
    }
    

    private void writeArchiveUnitToWorkspace(WorkspaceClient client, String containerId, XMLEventReader reader,
        StartElement startElement, ObjectNode archiveUnitTree, String path) throws ProcessingException {

        try {
            // Get ArchiveUnit Id
            String archiveUnitId = startElement.getAttributeByName(new QName(ARCHIVE_UNIT_ELEMENT_ID_ATTRIBUTE))
                .getValue();
            final Map<String, File> archiveUnitGuidToFileMap = extractArchiveUnitToLocalFile(reader, startElement,
                archiveUnitId, archiveUnitTree);

            if (archiveUnitGuidToFileMap != null && !archiveUnitGuidToFileMap.isEmpty()) {
                for (Entry<String, File> unitEntry : archiveUnitGuidToFileMap.entrySet()) {
                    File tmpFile = unitEntry.getValue();
                    client.putObject(containerId, path + "/" + unitEntry.getKey() + XML_EXTENSION,
                        new FileInputStream(tmpFile));

                    // Create Archive Unit LifeCycle
                    createUnitLifeCycle(unitEntry.getKey(), containerId);

                    if (!tmpFile.delete()) {
                        LOGGER.warn(FILE_COULD_NOT_BE_DELETED_MSG);
                    }
                }
            }
        } catch (final ProcessingException e) {
            LOGGER.error("Can not extract Object from SEDA XMLStreamException", e);
            throw e;
        } catch (final IOException e) {
            LOGGER.error("Can not extract Object from SEDA IOException ", e);
            throw new ProcessingException(e);
        } catch (final ContentAddressableStorageServerException e) {
            LOGGER.error("Can not write to workspace ", e);
            throw new ProcessingException(e);
        } catch (LogbookClientBadRequestException e) {
            LOGGER.error(LOGBOOK_LF_BAD_REQUEST_EXCEPTION_MSG, e);
            throw new ProcessingException(e);
        } catch (LogbookClientAlreadyExistsException e) {
            LOGGER.error(LOGBOOK_LF_OBJECT_EXISTS_EXCEPTION_MSG, e);
            throw new ProcessingException(e);
        } catch (LogbookClientServerException e) {
            LOGGER.error(LOGBOOK_SERVER_INTERNAL_EXCEPTION_MSG, e);
            throw new ProcessingException(e);
        }
    }

    private void checkArchiveUnitIdReference() throws ProcessingException {
        for (final Entry<String, String> entry : unitIdToGroupId.entrySet()) {
            if (objectGroupIdToGuid.get(entry.getValue()) == null) {
                final String groupId = binaryDataObjectIdToObjectGroupId.get(entry.getValue());
                if (groupId == null || groupId != "") {
                    throw new ProcessingException("Archive Unit reference Id is not correct");
                }
            }
        }
    }

    private String writeBinaryDataObjectInLocal(XMLEventReader reader, StartElement startElement, String containerId)
        throws ProcessingException {
        final String elementGuid = GUIDFactory.newGUID().toString();
        final File tmpFile = PropertiesUtils.fileFromTmpFolder(elementGuid + ".json");
        final XMLEventFactory eventFactory = XMLEventFactory.newInstance();
        final JsonXMLConfig config = new JsonXMLConfigBuilder().build();
        String groupGuid = null;
        try {
            final FileWriter tmpFileWriter = new FileWriter(tmpFile);

            final XMLEventWriter writer = new JsonXMLOutputFactory(config).createXMLEventWriter(tmpFileWriter);

            final Iterator<?> it = startElement.getAttributes();
            String binaryOjectId = "";
            if (it.hasNext()) {
                binaryOjectId = ((Attribute) it.next()).getValue();
                binaryDataObjectIdToGuid.put(binaryOjectId, elementGuid);
                binaryDataObjectIdToObjectGroupId.put(binaryOjectId, "");
                writer.add(eventFactory.createStartDocument());
                writer.add(eventFactory.createStartElement("", "", startElement.getName().getLocalPart()));
                writer.add(eventFactory.createStartElement("", "", "_id"));
                writer.add(eventFactory.createCharacters(binaryOjectId));
                writer.add(eventFactory.createEndElement("", "", "_id"));
            }
            while (true) {
                boolean writable = true;
                final XMLEvent event = reader.nextEvent();
                if (event.isEndElement()) {
                    final EndElement end = event.asEndElement();
                    if (end.getName().getLocalPart() == BINARY_DATA_OBJECT) {
                        writer.add(event);
                        writer.add(eventFactory.createEndDocument());
                        break;
                    }
                }

                if (event.isStartElement()) {
                    final String localPart = event.asStartElement().getName().getLocalPart();
                    if (localPart == DATA_OBJECT_GROUPID) {
                        groupGuid = GUIDFactory.newGUID().toString();
                        final String groupId = reader.getElementText();
                        // Having DataObjectGroupID after a DataObjectGroupReferenceID in the XML flow .
                        // We get the GUID defined earlier during the DataObjectGroupReferenceID analysis
                        if (objectGroupIdToGuidTmp.get(groupId) != null) {
                            groupGuid = objectGroupIdToGuidTmp.get(groupId);
                            objectGroupIdToGuidTmp.remove(groupId);
                        }
                        binaryDataObjectIdToObjectGroupId.put(binaryOjectId, groupId);
                        objectGroupIdToGuid.put(groupId, groupGuid);

                        // Create OG lifeCycle
                        createObjectGroupLifeCycle(groupGuid, containerId);
                        if (objectGroupIdToBinaryDataObjectId.get(groupId) == null) {
                            final List<String> binaryOjectList = new ArrayList<String>();
                            binaryOjectList.add(binaryOjectId);
                            objectGroupIdToBinaryDataObjectId.put(groupId, binaryOjectList);
                        } else {
                            objectGroupIdToBinaryDataObjectId.get(groupId).add(binaryOjectId);
                        }

                        // Create new startElement for group with new guid
                        writer.add(eventFactory.createStartElement("", "", DATA_OBJECT_GROUPID));
                        writer.add(eventFactory.createCharacters(groupGuid));
                        writer.add(eventFactory.createEndElement("", "", DATA_OBJECT_GROUPID));
                    } else if (localPart == DATA_OBJECT_GROUP_REFERENCEID) {
                        final String groupId = reader.getElementText();
                        String groupGuidTmp = GUIDFactory.newGUID().toString();
                        binaryDataObjectIdToObjectGroupId.put(binaryOjectId, groupId);
                        // The DataObjectGroupReferenceID is after DataObjectGroupID in the XML flow
                        if (objectGroupIdToBinaryDataObjectId.get(groupId) != null) {
                            objectGroupIdToBinaryDataObjectId.get(groupId).add(binaryOjectId);
                            groupGuidTmp = objectGroupIdToGuid.get(groupId);
                        } else {
                            // The DataObjectGroupReferenceID is before DataObjectGroupID in the XML flow
                            final List<String> binaryOjectList = new ArrayList<String>();
                            binaryOjectList.add(binaryOjectId);
                            objectGroupIdToBinaryDataObjectId.put(groupId, binaryOjectList);
                            objectGroupIdToGuidTmp.put(groupId, groupGuidTmp);

                        }

                        // Create new startElement for group with new guid
                        writer.add(eventFactory.createStartElement("", "", DATA_OBJECT_GROUPID));
                        writer.add(eventFactory.createCharacters(groupGuidTmp));
                        writer.add(eventFactory.createEndElement("", "", DATA_OBJECT_GROUPID));
                    } else if (localPart == "Uri") {
                        reader.getElementText();
                    } else {
                        writer.add(eventFactory.createStartElement("", "", localPart));
                    }

                    writable = false;
                }

                if (writable) {
                    writer.add(event);
                }
            }
            reader.close();
            writer.close();
            tmpFileWriter.close();

        } catch (final XMLStreamException e) {
            LOGGER.debug("Can not read input stream");
            throw new ProcessingException(e);
        } catch (final IOException e) {
            LOGGER.debug("Closing stream error");
            throw new ProcessingException(e);
        } catch (LogbookClientBadRequestException e) {
            LOGGER.error(LOGBOOK_LF_BAD_REQUEST_EXCEPTION_MSG, e);
            throw new ProcessingException(e);
        } catch (LogbookClientAlreadyExistsException e) {
            LOGGER.error(LOGBOOK_LF_OBJECT_EXISTS_EXCEPTION_MSG, e);
            throw new ProcessingException(e);
        } catch (LogbookClientServerException e) {
            LOGGER.error(LOGBOOK_SERVER_INTERNAL_EXCEPTION_MSG, e);
            throw new ProcessingException(e);
        }

        return groupGuid;

    }
    

    private void transferFileFromTmpIntoWorkspace(WorkspaceClient client, String tmpFileSubpath,
        String workspaceFilePath,
        String workspaceContainerId, boolean removeTmpFile) throws ProcessingException {
        final File firstMapTmpFile = PropertiesUtils.fileFromTmpFolder(tmpFileSubpath);
        try {
            client.putObject(workspaceContainerId, workspaceFilePath, new FileInputStream(firstMapTmpFile));
            if (removeTmpFile && !firstMapTmpFile.delete()) {
                LOGGER.warn(FILE_COULD_NOT_BE_DELETED_MSG + " " +
                    tmpFileSubpath);
            }

        } catch (ContentAddressableStorageServerException e) {
            LOGGER.error("Can not save in workspace file " + tmpFileSubpath);
            throw new ProcessingException(e);
        } catch (FileNotFoundException e) {
            LOGGER.error("Can not get file " + tmpFileSubpath);
            throw new ProcessingException(e);
        }
    }
    

    private void createObjectGroupLifeCycle(String groupGuid, String containerId)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException {
        LogbookLifeCycleObjectGroupParameters logbookLifecycleObjectGroupParameters =
            (LogbookLifeCycleObjectGroupParameters) initLogbookLifeCycleParameters(
                groupGuid, false, true);

        logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.eventIdentifierProcess,
            containerId);
        logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.eventIdentifier,
            GUIDFactory.newGUID().toString());
        logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.eventTypeProcess,
            LIFE_CYCLE_EVENT_TYPE_PROCESS);
        logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.eventType,
            OG_LIFE_CYCLE_CREATION_EVENT_TYPE);
        logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcome,
            LogbookOutcome.STARTED.toString());
        logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcomeDetail,
            LogbookOutcome.STARTED.toString());
        logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
            OutcomeMessage.CREATE_LOGBOOK_LIFECYCLE.value());
        LOGBOOK_LIFECYCLE_CLIENT.create(logbookLifecycleObjectGroupParameters);

        // Update guidToLifeCycleParameters
        guidToLifeCycleParameters.put(groupGuid, logbookLifecycleObjectGroupParameters);
    }
    
    /**
     * create level stack on Json file
     * 
     * @param client workspace client
     * @param containerId
     * @param levelStackMap
     * @throws ProcessingException
     */
    private void createIngestLevelStackFile(WorkspaceClient client, String containerId,
        Map<Integer, Set<String>> levelStackMap, String path) throws ProcessingException {
        LOGGER.info("Begin createIngestLevelStackFile/containerId:" + containerId);
        ParametersChecker.checkParameter("levelStackMap is a mandatory parameter", levelStackMap);
        ParametersChecker.checkParameter("unitIdToGuid is a mandatory parameter", unitIdToGuid);
        ParametersChecker.checkParameter(WORKSPACE_MANDATORY_MSG, client);

        File tempFile = null;
        try {
            tempFile = File.createTempFile(TMP_FOLDER, INGEST_LEVEL_STACK);
            // tempFile will be deleted on exit
            tempFile.deleteOnExit();
            // create level json object node
            ObjectNode IngestLevelStack = JsonHandler.createObjectNode();
            for (Entry<Integer, Set<String>> entry : levelStackMap.entrySet()) {
                ArrayNode unitList = IngestLevelStack.withArray(LEVEL + entry.getKey());
                Set<String> unitGuidList = entry.getValue();
                for (String idXml : unitGuidList) {

                    String unitGuid = unitIdToGuid.get(idXml);
                    if (unitGuid == null) {
                        throw new IllegalArgumentException("Unit guid not found in map");
                    }
                    unitList.add(unitGuid);
                }
                IngestLevelStack.set(LEVEL + entry.getKey(), unitList);
            }
            LOGGER.debug("IngestLevelStack:" + IngestLevelStack.toString());
            // create json file
            JsonHandler.writeAsFile(IngestLevelStack, tempFile);
            // put file in workspace
            client.putObject(containerId, path,
                new FileInputStream(tempFile));
        } catch (IOException e) {
            LOGGER.error(e);
            throw new ProcessingException(e);
        } catch (InvalidParseOperationException e) {
            LOGGER.error(e);
            throw new ProcessingException(e);
        } catch (ContentAddressableStorageServerException e) {
            LOGGER.error(e);
            throw new ProcessingException(e);
        } finally {
            if (tempFile != null) {
                tempFile.exists();
            }
        }
        LOGGER.info("End createIngestLevelStackFile/containerId:" + containerId);

    }
    

    private LogbookParameters initLogbookLifeCycleParameters(String guid, boolean isArchive, boolean isObjectGroup) {
        LogbookParameters logbookLifeCycleParameters = guidToLifeCycleParameters.get(guid);
        if (logbookLifeCycleParameters == null) {
            logbookLifeCycleParameters = isArchive ? LogbookParametersFactory.newLogbookLifeCycleUnitParameters()
                : (isObjectGroup ? LogbookParametersFactory.newLogbookLifeCycleObjectGroupParameters()
                    : LogbookParametersFactory.newLogbookOperationParameters());

            logbookLifeCycleParameters.putParameterValue(LogbookParameterName.objectIdentifier, guid);
        }
        return logbookLifeCycleParameters;
    }
    

    private void createUnitLifeCycle(String unitGuid, String containerId)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException {
        LogbookLifeCycleUnitParameters logbookLifecycleUnitParameters =
            (LogbookLifeCycleUnitParameters) initLogbookLifeCycleParameters(
                unitGuid, true, false);

        logbookLifecycleUnitParameters.putParameterValue(LogbookParameterName.eventIdentifierProcess, containerId);
        logbookLifecycleUnitParameters.putParameterValue(LogbookParameterName.eventIdentifier,
            GUIDFactory.newGUID().toString());
        logbookLifecycleUnitParameters.putParameterValue(LogbookParameterName.eventTypeProcess,
            LIFE_CYCLE_EVENT_TYPE_PROCESS);
        logbookLifecycleUnitParameters.putParameterValue(LogbookParameterName.eventType,
            UNIT_LIFE_CYCLE_CREATION_EVENT_TYPE);
        logbookLifecycleUnitParameters.putParameterValue(LogbookParameterName.outcome,
            LogbookOutcome.STARTED.toString());
        logbookLifecycleUnitParameters.putParameterValue(LogbookParameterName.outcomeDetail,
            LogbookOutcome.STARTED.toString());
        logbookLifecycleUnitParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
            LogbookOutcome.STARTED.toString());
        LOGBOOK_LIFECYCLE_CLIENT.create(logbookLifecycleUnitParameters);

        // Update guidToLifeCycleParameters
        guidToLifeCycleParameters.put(unitGuid, logbookLifecycleUnitParameters);
    }

    
    private Map<String, File> extractArchiveUnitToLocalFile(XMLEventReader reader, StartElement startElement,
        String archiveUnitId, ObjectNode archiveUnitTree)
            throws ProcessingException {

        Map<String, File> archiveUnitToTmpFileMap = new HashMap<>();
        final String elementGuid = GUIDFactory.newGUID().toString();

        final XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
        final XMLEventFactory eventFactory = XMLEventFactory.newInstance();
        final String elementID = ((Attribute) startElement.getAttributes().next()).getValue();
        final QName name = startElement.getName();
        int stack = 1;
        String groupGuid = "";
        final File tmpFile = PropertiesUtils.fileFromTmpFolder(GUIDFactory.newGUID().toString() + elementGuid);
        XMLEventWriter writer;

        final QName unitName = new QName(NAMESPACE_URI, ARCHIVE_UNIT);
        final QName archiveUnitRefIdTag = new QName(NAMESPACE_URI, ARCHIVE_UNIT_REF_ID_TAG);

        // Add new node in archiveUnitNode
        ObjectNode archiveUnitNode = (ObjectNode) archiveUnitTree.get(archiveUnitId);
        if (archiveUnitNode == null) {
            // Create node
            archiveUnitNode = JsonHandler.createObjectNode();
        }

        // Add new Archive Unit Entry
        archiveUnitTree.set(archiveUnitId, archiveUnitNode);

        try {
            tmpFile.createNewFile();
            writer = xmlOutputFactory.createXMLEventWriter(new FileWriter(tmpFile));
            unitIdToGuid.put(elementID, elementGuid);

            // Create new startElement for object with new guid
            writer.add(eventFactory.createStartElement("", NAMESPACE_URI, startElement.getName().getLocalPart()));
            writer.add(eventFactory.createAttribute("id", elementGuid));
            // TODO allow recursive
            while (true) {
                final XMLEvent event = reader.nextEvent();
                if (event.isStartElement() && event.asStartElement().getName().equals(name)) {
                    String currentArchiveUnit = event.asStartElement()
                        .getAttributeByName(new QName(ARCHIVE_UNIT_ELEMENT_ID_ATTRIBUTE)).getValue();
                    if (archiveUnitId.equalsIgnoreCase(currentArchiveUnit)) {
                        stack++;
                    }
                }

                if (event.isEndElement()) {
                    final EndElement end = event.asEndElement();
                    if (end.getName().equals(name)) {
                        stack--;
                        if (stack == 0) {
                            // Create objectgroup reference id
                            groupGuid = objectGroupIdToGuid.get(unitIdToGroupId.get(elementID));
                            writer.add(eventFactory.createStartElement("", "", TAG_OG));
                            writer.add(eventFactory.createCharacters(groupGuid));
                            writer.add(eventFactory.createEndElement("", "", TAG_OG));

                            writer.add(event);
                            break;
                        }
                    }
                }
                if (event.isStartElement() &&
                    event.asStartElement().getName().getLocalPart() == DATA_OBJECT_GROUP_REFERENCEID) {
                    final String groupId = reader.getElementText();
                    unitIdToGroupId.put(elementID, groupId);
                    if (objectGroupIdToUnitId.get(groupId) == null) {
                        final ArrayList<String> archiveUnitList = new ArrayList<>();
                        archiveUnitList.add(elementID);
                        objectGroupIdToUnitId.put(groupId, archiveUnitList);
                    } else {
                        final List<String> archiveUnitList = objectGroupIdToUnitId.get(groupId);
                        archiveUnitList.add(elementID);
                        objectGroupIdToUnitId.put(groupId, archiveUnitList);
                    }
                    // Create new startElement for group with new guid
                    groupGuid = objectGroupIdToGuid.get(unitIdToGroupId.get(elementID));
                    writer.add(eventFactory.createStartElement("", NAMESPACE_URI, DATA_OBJECT_GROUP_REFERENCEID));
                    writer.add(eventFactory.createCharacters(groupGuid));
                    writer.add(eventFactory.createEndElement("", NAMESPACE_URI, DATA_OBJECT_GROUP_REFERENCEID));

                } else if (event.isStartElement() && event.asStartElement().getName().equals(unitName)) {

                    // Update archiveUnitTree
                    String nestedArchiveUnitId = event.asStartElement()
                        .getAttributeByName(new QName(ARCHIVE_UNIT_ELEMENT_ID_ATTRIBUTE)).getValue();

                    ObjectNode nestedArchiveUnitNode = (ObjectNode) archiveUnitTree.get(nestedArchiveUnitId);
                    if (nestedArchiveUnitNode == null) {
                        // Create new Archive Unit Node
                        nestedArchiveUnitNode = JsonHandler.createObjectNode();
                    }

                    // Add immediate parents
                    ArrayNode parentsField = nestedArchiveUnitNode.withArray(UP_FIELD);
                    parentsField.add(archiveUnitId);

                    // Update global tree
                    archiveUnitTree.set(nestedArchiveUnitId, nestedArchiveUnitNode);

                    // Process Archive Unit element: recursive call
                    archiveUnitToTmpFileMap.putAll(extractArchiveUnitToLocalFile(reader, event.asStartElement(),
                        nestedArchiveUnitId, archiveUnitTree));
                } else if (event.isStartElement() && event.asStartElement().getName().equals(archiveUnitRefIdTag)) {
                    // Referenced Child Archive Unit
                    String childArchiveUnitRef = reader.getElementText();

                    ObjectNode childArchiveUnitNode = (ObjectNode) archiveUnitTree.get(childArchiveUnitRef);
                    if (childArchiveUnitNode == null) {
                        // Create new Archive Unit Node
                        childArchiveUnitNode = JsonHandler.createObjectNode();
                    }

                    // Reference Management during tree creation
                    ArrayNode parentsField = childArchiveUnitNode.withArray(UP_FIELD);
                    parentsField.addAll((ArrayNode) archiveUnitTree.get(archiveUnitId).get("_up"));
                    archiveUnitTree.set(childArchiveUnitRef, childArchiveUnitNode);
                    archiveUnitTree.without(archiveUnitId);
                } else {
                    writer.add(event);
                }

            }
            reader.close();
            writer.close();
        } catch (final XMLStreamException e) {
            LOGGER.error("Can not extract Object from SEDA XMLStreamException");
            throw new ProcessingException(e);
        } catch (final IOException e) {
            LOGGER.error("Can not extract Object from SEDA IOException " + elementGuid);
            throw new ProcessingException(e);
        } catch (final Exception e) {
            LOGGER.error(e.getMessage());
            throw new ProcessingException(e);
        }

        archiveUnitToTmpFileMap.put(elementGuid, tmpFile);
        return archiveUnitToTmpFileMap;
    }
    

    private void saveMap(String containerId, Map<String, ?> map, String fileName, WorkspaceClient client,
        boolean removeTmpFile)
            throws IOException, ProcessingException {

        String tmpFilePath = containerId + fileName.split("/")[1];
        final File firstMapTmpFile = PropertiesUtils
            .fileFromTmpFolder(tmpFilePath);

        final FileWriter firstMapTmpFileWriter = new FileWriter(firstMapTmpFile);
        firstMapTmpFileWriter.write(JsonHandler.prettyPrint(map));
        firstMapTmpFileWriter.flush();
        firstMapTmpFileWriter.close();

        transferFileFromTmpIntoWorkspace(client, tmpFilePath, fileName, containerId,
            removeTmpFile);

    }
    

    private void saveObjectGroupsToWorkspace(WorkspaceClient client, String containerId) throws ProcessingException {

        completeBinaryObjectToObjectGroupMap();

        // Save maps
        try {
            // Save binaryDataObjectIdToObjectGroupId
            saveMap(containerId, binaryDataObjectIdToObjectGroupId, (String) handlerIO.getOutput().get(3), client, true);
            // Save objectGroupIdToGuid
            saveMap(containerId, objectGroupIdToGuid, (String) handlerIO.getOutput().get(5), client, true);
        } catch (IOException e1) {
            LOGGER.error("Can not write to tmp folder ", e1);
            throw new ProcessingException(e1);
        }

        for (final Entry<String, List<String>> entry : objectGroupIdToBinaryDataObjectId.entrySet()) {
            final ObjectNode objectGroup = JsonHandler.createObjectNode();
            ObjectNode fileInfo = JsonHandler.createObjectNode();
            final ArrayNode unitParent = JsonHandler.createArrayNode();
            String objectGroupType = "";
            final String objectGroupGuid = objectGroupIdToGuid.get(entry.getKey());
            final File tmpFile = PropertiesUtils.fileFromTmpFolder(objectGroupGuid + JSON_EXTENSION);

            try {
                final FileWriter tmpFileWriter = new FileWriter(tmpFile);
                final Map<String, ArrayList<JsonNode>> categoryMap = new HashMap<>();
                objectGroup.put("_id", objectGroupGuid);
                objectGroup.put("_tenantId", 0);
                for (final String id : entry.getValue()) {
                    final File binaryObjectFile = PropertiesUtils
                        .fileFromTmpFolder(binaryDataObjectIdToGuid.get(id) + JSON_EXTENSION);
                    final JsonNode binaryNode = JsonHandler.getFromFile(binaryObjectFile).get("BinaryDataObject");
                    String nodeCategory = "BinaryMaster";
                    if (binaryNode.get("DataObjectVersion") != null) {
                        nodeCategory = binaryNode.get("DataObjectVersion").asText();
                    }
                    ArrayList<JsonNode> nodeCategoryArray = categoryMap.get(nodeCategory);
                    if (nodeCategoryArray == null) {
                        nodeCategoryArray = new ArrayList<>();
                        nodeCategoryArray.add(binaryNode);
                    } else {
                        nodeCategoryArray.add(binaryNode);
                    }
                    categoryMap.put(nodeCategory, nodeCategoryArray);
                    if (BINARY_MASTER.equals(nodeCategory)) {

                        fileInfo = (ObjectNode) binaryNode.get(FILE_INFO);
                        if (binaryNode.get(METADATA) != null) {
                            objectGroupType = binaryNode.get(METADATA).fieldNames().next();
                        }
                    }
                    if (!binaryObjectFile.delete()) {
                        LOGGER.warn(FILE_COULD_NOT_BE_DELETED_MSG);
                    }
                }

                for (final String objectGroupId : objectGroupIdToUnitId.get(entry.getKey())) {
                    unitParent.add(unitIdToGuid.get(objectGroupId));
                }

                objectGroup.put("_type", objectGroupType);
                objectGroup.set("FileInfo", fileInfo);
                final ObjectNode qualifiersNode = getObjectGroupQualifiers(categoryMap);
                objectGroup.set("_qualifiers", qualifiersNode);
                objectGroup.set("_up", unitParent);
                objectGroup.put("_nb", entry.getValue().size());
                tmpFileWriter.write(objectGroup.toString());
                tmpFileWriter.close();

                client.putObject(containerId, IngestWorkflowConstants.OBJECT_GROUP_FOLDER + "/" + objectGroupGuid + JSON_EXTENSION,
                    new FileInputStream(tmpFile));
                if (!tmpFile.delete()) {
                    LOGGER.warn(FILE_COULD_NOT_BE_DELETED_MSG);
                }

                // Create unreferenced object group
                if (guidToLifeCycleParameters.get(objectGroupGuid) == null) {
                    createObjectGroupLifeCycle(objectGroupGuid, containerId);

                    // Update Object Group lifeCycle creation event
                    guidToLifeCycleParameters.get(objectGroupGuid).setStatus(LogbookOutcome.OK);
                    guidToLifeCycleParameters.get(objectGroupGuid).putParameterValue(LogbookParameterName.outcomeDetail,
                        LogbookOutcome.OK.name());
                    guidToLifeCycleParameters.get(objectGroupGuid).putParameterValue(
                        LogbookParameterName.outcomeDetailMessage,
                        OutcomeMessage.CREATE_LOGBOOK_LIFECYCLE_OK.value());
                    LOGBOOK_LIFECYCLE_CLIENT.update(guidToLifeCycleParameters.get(objectGroupGuid));
                }

            } catch (final InvalidParseOperationException e) {
                LOGGER.error("Can not parse ObjectGroup", e);
                throw new ProcessingException(e);
            } catch (final IOException e) {
                LOGGER.error("Can not write to tmp folder ", e);
                throw new ProcessingException(e);
            } catch (final ContentAddressableStorageServerException e) {
                LOGGER.error("Workspace exception ", e);
                throw new ProcessingException(e);
            } catch (LogbookClientBadRequestException e) {
                LOGGER.error(LOGBOOK_LF_BAD_REQUEST_EXCEPTION_MSG, e);
                throw new ProcessingException(e);
            } catch (LogbookClientAlreadyExistsException e) {
                LOGGER.error(LOGBOOK_LF_OBJECT_EXISTS_EXCEPTION_MSG, e);
                throw new ProcessingException(e);
            } catch (LogbookClientServerException e) {
                LOGGER.error(LOGBOOK_SERVER_INTERNAL_EXCEPTION_MSG, e);
                throw new ProcessingException(e);
            } catch (LogbookClientNotFoundException e) {
                LOGGER.error(LOGBOOK_LF_RESOURCE_NOT_FOUND_EXCEPTION_MSG, e);
                throw new ProcessingException(e);
            }
        }
    }
    

    private ObjectNode getObjectGroupQualifiers(Map<String, ArrayList<JsonNode>> categoryMap) {
        final ObjectNode qualifierObject = JsonHandler.createObjectNode();
        for (final Entry<String, ArrayList<JsonNode>> entry : categoryMap.entrySet()) {
            final ObjectNode binaryNode = JsonHandler.createObjectNode();
            binaryNode.put("nb", entry.getValue().size());
            final ArrayNode arrayNode = JsonHandler.createArrayNode();
            for (final JsonNode node : entry.getValue()) {
                String id = node.findValue(TAG_ID).textValue();
                ((ObjectNode) node).put(TAG_ID, binaryDataObjectIdToGuid.get(id));
                arrayNode.add(node);
            }
            binaryNode.set("versions", arrayNode);
            qualifierObject.set(entry.getKey(), binaryNode);
        }
        return qualifierObject;
    }
    
    private void completeBinaryObjectToObjectGroupMap() {
        for (final String key : binaryDataObjectIdToObjectGroupId.keySet()) {
            if ("".equals(binaryDataObjectIdToObjectGroupId.get(key))) {
                final List<String> binaryOjectList = new ArrayList<>();
                binaryOjectList.add(key);
                objectGroupIdToBinaryDataObjectId.put(GUIDFactory.newGUID().toString(), binaryOjectList);
                // TODO Create OG / OG lifeCycle
            }
        }
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        if (handlerIO.getOutput().size() != handlerInitialIOList.getOutput().size()) {
            throw new ProcessingException(HandlerIO.NOT_ENOUGH_PARAM);
        } else if (!HandlerIO.checkHandlerIO(handlerIO, this.handlerInitialIOList)) {
            throw new ProcessingException(HandlerIO.NOT_CONFORM_PARAM);
        }
    }

}
