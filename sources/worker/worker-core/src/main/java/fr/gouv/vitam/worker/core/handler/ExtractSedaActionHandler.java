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
import java.io.FileReader;
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
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import de.odysseus.staxon.json.JsonXMLConfig;
import de.odysseus.staxon.json.JsonXMLConfigBuilder;
import de.odysseus.staxon.json.JsonXMLOutputFactory;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.exception.CycleFoundException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.graph.DirectedCycle;
import fr.gouv.vitam.common.graph.DirectedGraph;
import fr.gouv.vitam.common.graph.Graph;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.CompositeItemStatus;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleObjectGroupParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleUnitParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.utils.BinaryObjectInfo;
import fr.gouv.vitam.worker.common.utils.IngestWorkflowConstants;
import fr.gouv.vitam.worker.common.utils.SedaConstants;
import fr.gouv.vitam.worker.core.api.HandlerIO;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

/**
 * Handler class used to extract metaData. </br>
 * Create and put a new file (metadata extracted) json.json into container GUID
 *
 */

public class ExtractSedaActionHandler extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ExtractSedaActionHandler.class);

    // OUT RANK
    private static final int GRAPH_WITH_LONGEST_PATH_IO_RANK = 0;
    private static final int BDO_ID_TO_OG_ID_IO_RANK = 1;
    private static final int BDO_ID_TO_GUID_IO_RANK = 2;
    private static final int OG_ID_TO_GUID_IO_RANK = 3;
    private static final int OG_ID_TO_UNID_ID_IO_RANK = 4;
    private static final int BDO_ID_TO_VERSION_DO_IO_RANK = 5;
    private static final int UNIT_ID_TO_GUID_IO_RANK = 6;
    private static final int GLOBAL_SEDA_PARAMETERS_FILE_IO_RANK = 7;

    // FIXME P0 ne devrait pas être static
    private static final LogbookLifeCyclesClient LOGBOOK_LIFECYCLE_CLIENT = LogbookLifeCyclesClientFactory.getInstance()
        .getClient();
    private static final String HANDLER_ID = "CHECK_CONSISTENCY";
    private HandlerIO handlerIO;

    private static final String XML_EXTENSION = ".xml";
    private static final String JSON_EXTENSION = ".json";
    private static final String BINARY_DATA_OBJECT = "BinaryDataObject";
    private static final String DATA_OBJECT_GROUPID = "DataObjectGroupId";
    private static final String ARCHIVE_UNIT = "ArchiveUnit";
    private static final String BINARY_MASTER = "BinaryMaster";
    private static final String DATAOBJECT_PACKAGE = "DataObjectPackage";
    private static final String FILE_INFO = "FileInfo";
    private static final String METADATA = "Metadata";
    private static final String LIFE_CYCLE_EVENT_TYPE_PROCESS = "INGEST";
    // TODO P0 WORKFLOW will be in vitam-logbook file
    private static final String UNIT_LIFE_CYCLE_CREATION_EVENT_TYPE =
        "Check SIP – Units – Lifecycle Logbook Creation – Création du journal du cycle de vie des units";
    private static final String OG_LIFE_CYCLE_CREATION_EVENT_TYPE =
        "Check SIP – ObjectGroups – Lifecycle Logbook Creation – Création du journal du cycle de vie des groupes d’objets";
    private static final String LOGBOOK_LF_BAD_REQUEST_EXCEPTION_MSG = "LogbookClient Unsupported request";
    private static final String LOGBOOK_LF_OBJECT_EXISTS_EXCEPTION_MSG = "LifeCycle Object already exists";
    private static final String LOGBOOK_LF_RESOURCE_NOT_FOUND_EXCEPTION_MSG = "Logbook LifeCycle resource not found";
    private static final String LOGBOOK_SERVER_INTERNAL_EXCEPTION_MSG = "Logbook Server internal error";
    private static final String BINARY_DATA_OBJECT_VERSION_MUST_BE_UNIQUE = "ERROR: BinaryDataObject version must be unique";
    private static final String LEVEL = "level_";

    private static final String ARCHIVE_UNIT_ELEMENT_ID_ATTRIBUTE = "id";
    private static final String ARCHIVE_UNIT_REF_ID_TAG = "ArchiveUnitRefId";
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
    private static final String ARCHIVE_UNIT_TMP_FILE_PREFIX = "AU_TMP_";
    private static final String GLOBAL_SEDA_PARAMETERS_FILE = "globalSEDAParameters.json";

    private final Map<String, String> binaryDataObjectIdToGuid;
    private final Map<String, String> objectGroupIdToGuid;
    private final Map<String, String> objectGroupIdToGuidTmp;
    private final Map<String, String> unitIdToGuid;

    private final Map<String, String> binaryDataObjectIdToObjectGroupId;
    private final Map<String, List<String>> objectGroupIdToBinaryDataObjectId;
    private final Map<String, String> unitIdToGroupId;
    // this map contains binaryDataObject that not have DataObjectGroupId
    private final Map<String, GotObj> binaryDataObjectIdWithoutObjectGroupId;
    private final Map<String, List<String>> objectGroupIdToUnitId;
    private final Map<String, BinaryObjectInfo> objectGuidToBinaryObject;
    private final Map<String, String> binaryDataObjectIdToVersionDataObject;
    private final Map<String, LogbookParameters> guidToLifeCycleParameters;

    public static final int HANDLER_IO_PARAMETER_NUMBER = 8;
    private final HandlerIO handlerInitialIOList;

    /**
     * Constructor with parameter SedaUtilsFactory
     *
     */
    public ExtractSedaActionHandler() {
        binaryDataObjectIdToGuid = new HashMap<>();
        binaryDataObjectIdWithoutObjectGroupId = new HashMap<>();
        objectGroupIdToGuid = new HashMap<>();
        objectGroupIdToGuidTmp = new HashMap<>();
        unitIdToGuid = new HashMap<>();
        binaryDataObjectIdToObjectGroupId = new HashMap<>();
        objectGroupIdToBinaryDataObjectId = new HashMap<>();
        unitIdToGroupId = new HashMap<>();
        objectGroupIdToUnitId = new HashMap<>();
        guidToLifeCycleParameters = new HashMap<>();
        binaryDataObjectIdToVersionDataObject = new HashMap<>();
        objectGuidToBinaryObject = new HashMap<>();
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
    public CompositeItemStatus execute(WorkerParameters params, HandlerIO ioParam) {
        checkMandatoryParameters(params);
        handlerIO = ioParam;
        final ItemStatus itemStatus = new ItemStatus(HANDLER_ID);

        try {
            checkMandatoryIOParameter(ioParam);
            extractSEDA(params, itemStatus);
            itemStatus.increment(StatusCode.OK);

        } catch (final ProcessingException e) {
            LOGGER.debug("ProcessingException", e);
            itemStatus.increment(StatusCode.FATAL);

        } finally {
            // Empty all maps
            binaryDataObjectIdToGuid.clear();
            objectGroupIdToGuid.clear();
            objectGroupIdToGuidTmp.clear();
            unitIdToGuid.clear();
            binaryDataObjectIdWithoutObjectGroupId.clear();
            binaryDataObjectIdToObjectGroupId.clear();
            objectGroupIdToBinaryDataObjectId.clear();
            unitIdToGroupId.clear();
            objectGroupIdToUnitId.clear();
            guidToLifeCycleParameters.clear();
            objectGuidToBinaryObject.clear();
            binaryDataObjectIdToVersionDataObject.clear();
        }

        return new CompositeItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);

    }

    /**
     * Split Element from InputStream and write it to workspace
     *
     * @param params parameters of workspace server
     * @throws ProcessingException throw when can't read or extract element from SEDA
     */
    public void extractSEDA(WorkerParameters params, ItemStatus itemStatus) throws ProcessingException {
        ParameterHelper.checkNullOrEmptyParameters(params);
        final String containerId = params.getContainerName();
        // TODO P0: whould use worker configuration instead of the processing configuration
        //WorkspaceClientFactory.changeMode(params.getUrlWorkspace());
        try (final WorkspaceClient workspaceClient = WorkspaceClientFactory.getInstance().getClient()) {
            extractSEDAWithWorkspaceClient(workspaceClient, containerId, itemStatus);
        }
    }

    private void extractSEDAWithWorkspaceClient(WorkspaceClient client, String containerId, ItemStatus itemStatus)
        throws ProcessingException {
        ParametersChecker.checkParameter(WORKSPACE_MANDATORY_MSG, client);
        ParametersChecker.checkParameter("ContainerId is a mandatory parameter", containerId);
        ParametersChecker.checkParameter("itemStatus is a mandatory parameter", itemStatus);


        /**
         * Retrieves SEDA
         **/
        InputStream xmlFile = null;

        final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        XMLEventReader reader = null;
        final QName dataObjectName = new QName(SedaConstants.NAMESPACE_URI, BINARY_DATA_OBJECT);
        final QName unitName = new QName(SedaConstants.NAMESPACE_URI, ARCHIVE_UNIT);

        // Archive Unit Tree
        final ObjectNode archiveUnitTree = JsonHandler.createObjectNode();

        try {
            try {
                xmlFile = client.getObject(containerId,
                    IngestWorkflowConstants.SEDA_FOLDER + "/" + IngestWorkflowConstants.SEDA_FILE);
            } catch (ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException e) {
                LOGGER.error(MANIFEST_NOT_FOUND);
                throw new ProcessingException(e);
            }
            reader = xmlInputFactory.createXMLEventReader(xmlFile);
            final JsonXMLConfig config = new JsonXMLConfigBuilder().build();
            // This file will be a JSON representation of the SEDA manifest with an empty DataObjectPackage structure
            final FileWriter tmpFileWriter =
                new FileWriter(PropertiesUtils.fileFromTmpFolder(GLOBAL_SEDA_PARAMETERS_FILE));
            final XMLEventFactory eventFactory = XMLEventFactory.newInstance();
            final XMLEventWriter writer = new JsonXMLOutputFactory(config).createXMLEventWriter(tmpFileWriter);
            writer.add(eventFactory.createStartDocument());
            boolean globalMetadata = true;
            while (true) {
                final XMLEvent event = reader.nextEvent();

                // extract info for ATR
                // The DataObjectPackage EndElement is tested before the add condition as we need to add a empty
                // DAtaObjectPackage endElement event
                if (event.isEndElement() && event.asEndElement().getName().getLocalPart().equals(DATAOBJECT_PACKAGE)) {
                    globalMetadata = true;
                }

                if (event.isStartElement() &&
                    event.asStartElement().getName().getLocalPart()
                        .equals(SedaConstants.TAG_ORIGINATINGAGENCYIDENTIFIER)) {
                    String orgAgId = reader.getElementText();
                    writer.add(eventFactory.createStartElement("", SedaConstants.NAMESPACE_URI,
                        SedaConstants.TAG_ORIGINATINGAGENCYIDENTIFIER));
                    writer.add(eventFactory.createCharacters(orgAgId));
                    writer.add(eventFactory.createEndElement("", SedaConstants.NAMESPACE_URI,
                        SedaConstants.TAG_ORIGINATINGAGENCYIDENTIFIER));
                    globalMetadata = false;
                }

                if (event.isStartElement() &&
                    event.asStartElement().getName().getLocalPart()
                        .equals(SedaConstants.TAG_SUBMISSIONAGENCYIDENTIFIER)) {
                    String orgAgId = reader.getElementText();
                    writer.add(eventFactory.createStartElement("", SedaConstants.NAMESPACE_URI,
                        SedaConstants.TAG_SUBMISSIONAGENCYIDENTIFIER));
                    writer.add(eventFactory.createCharacters(orgAgId));
                    writer.add(eventFactory.createEndElement("", SedaConstants.NAMESPACE_URI,
                        SedaConstants.TAG_SUBMISSIONAGENCYIDENTIFIER));
                    globalMetadata = false;
                }

                // We add all the end but the start and end document and the event in the DataObjectPackage structure
                if (globalMetadata && event.getEventType() != XMLStreamConstants.START_DOCUMENT &&
                    event.getEventType() != XMLStreamConstants.END_DOCUMENT) {
                    writer.add(event);
                }
                // The DataObjectPackage StartElement is tested after the add event condition as we need to add an empty
                // DataObjectPackage startElement event
                if (event.isStartElement() &&
                    event.asStartElement().getName().getLocalPart().equals(DATAOBJECT_PACKAGE)) {
                    globalMetadata = false;
                }

                if (event.isStartElement()) {
                    final StartElement element = event.asStartElement();
                    if (element.getName().equals(unitName)) {
                        writeArchiveUnitToTmpDir(client, containerId, reader, element, archiveUnitTree,
                            IngestWorkflowConstants.ARCHIVE_UNIT_FOLDER);
                    } else if (element.getName().equals(dataObjectName)) {
                        final String objectGroupGuid = writeBinaryDataObjectInLocal(reader, element, containerId);
                        if (guidToLifeCycleParameters.get(objectGroupGuid) != null) {
                            guidToLifeCycleParameters.get(objectGroupGuid).setStatus(StatusCode.OK);
                            guidToLifeCycleParameters.get(objectGroupGuid)
                                .putParameterValue(LogbookParameterName.outcomeDetail, StatusCode.OK.name());
                            guidToLifeCycleParameters.get(objectGroupGuid).putParameterValue(
                                LogbookParameterName.outcomeDetailMessage,
                                VitamLogbookMessages.getCodeLfc(itemStatus.getItemId(), StatusCode.OK));
                            LOGBOOK_LIFECYCLE_CLIENT.update(guidToLifeCycleParameters.get(objectGroupGuid));
                        }
                    }
                }
                if (event.isEndDocument()) {
                    break;
                }
            }
            writer.add(eventFactory.createEndDocument());
            writer.close();
            // 1-detect cycle : if graph has a cycle throw CycleFoundException
            final DirectedCycle directedCycle = new DirectedCycle(new DirectedGraph(archiveUnitTree));
            if (directedCycle.isCyclic()) {
                throw new CycleFoundException(GRAPH_CYCLE_MSG);
            }


            // 2- create graph and create level
            createIngestLevelStackFile(client, containerId, new Graph(archiveUnitTree).getGraphWithLongestPaths(),
                (String) handlerIO.getOutput().get(GRAPH_WITH_LONGEST_PATH_IO_RANK));

            checkArchiveUnitIdReference();
            saveObjectGroupsToWorkspace(client, containerId);

            // Add parents to archive units and save them into workspace
            addParentsAndSaveArchiveUnitToWorkspace(client, archiveUnitTree, containerId,
                IngestWorkflowConstants.ARCHIVE_UNIT_FOLDER, itemStatus);


            // Save binaryDataObjectIdToGuid Map
            HandlerUtils.saveMap(containerId, binaryDataObjectIdToGuid,
                (String) handlerIO.getOutput().get(BDO_ID_TO_GUID_IO_RANK),
                client, true);

            // Save objectGroupIdToUnitId Map
            HandlerUtils.saveMap(containerId, objectGroupIdToUnitId,
                (String) handlerIO.getOutput().get(OG_ID_TO_UNID_ID_IO_RANK),
                client, true);
            // Save binaryDataObjectIdToVersionDataObject Map
            HandlerUtils.saveMap(containerId, binaryDataObjectIdToVersionDataObject,
                (String) handlerIO.getOutput().get(BDO_ID_TO_VERSION_DO_IO_RANK), client, true);

            // Save unitIdToGuid Map
            HandlerUtils.saveMap(containerId, unitIdToGuid,
                (String) handlerIO.getOutput().get(UNIT_ID_TO_GUID_IO_RANK), client, true);

            HandlerIO.transferFileFromTmpIntoWorkspace(client, GLOBAL_SEDA_PARAMETERS_FILE,
                (String) handlerIO.getOutput().get(GLOBAL_SEDA_PARAMETERS_FILE_IO_RANK), containerId,
                false);

        } catch (final XMLStreamException e) {
            LOGGER.error(CANNOT_READ_SEDA, e);
            throw new ProcessingException(e);
        } catch (final LogbookClientBadRequestException e) {
            LOGGER.error(LOGBOOK_LF_BAD_REQUEST_EXCEPTION_MSG, e);
            throw new ProcessingException(e);
        } catch (final LogbookClientNotFoundException e) {
            LOGGER.error(LOGBOOK_LF_RESOURCE_NOT_FOUND_EXCEPTION_MSG, e);
            throw new ProcessingException(e);
        } catch (final LogbookClientServerException e) {
            LOGGER.error(LOGBOOK_SERVER_INTERNAL_EXCEPTION_MSG, e);
            throw new ProcessingException(e);
            /*
             * } catch (InvalidParseOperationException e) { LOGGER.error(INVALID_INGEST_TREE_EXCEPTION_MSG, e); throw
             * new ProcessingException(e);
             */
        } catch (final CycleFoundException e) {
            LOGGER.error(CYCLE_FOUND_EXCEPTION, e);
            throw new ProcessingException(e);
        } catch (final IOException e) {
            LOGGER.error(SAVE_ARCHIVE_ID_TO_GUID_IOEXCEPTION_MSG, e);
            throw new ProcessingException(e);
        } finally {
            StreamUtils.closeSilently(xmlFile);
            if (reader != null) {
                try {
                    reader.close();
                } catch (XMLStreamException e) {
                    SysErrLogger.FAKE_LOGGER.ignoreLog(e);
                }
            }
        }
    }

    private void addParentsAndSaveArchiveUnitToWorkspace(WorkspaceClient client, ObjectNode archiveUnitTree,
        String containerId, String path, ItemStatus itemStatus)
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException,
        XMLStreamException, IOException, ProcessingException {

        // Finalize Archive units extraction process
        if (unitIdToGuid == null) {
            return;
        }

        final XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
        final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        final XMLEventFactory eventFactory = XMLEventFactory.newInstance();

        for (final Entry<String, String> element : unitIdToGuid.entrySet()) {

            final String unitGuid = element.getValue();
            final String unitId = element.getKey();

            // 1- Update created Unit life cycles
            if (guidToLifeCycleParameters.get(unitGuid) != null) {
                guidToLifeCycleParameters.get(unitGuid).setStatus(StatusCode.OK);
                guidToLifeCycleParameters.get(unitGuid)
                    .putParameterValue(LogbookParameterName.outcomeDetail, StatusCode.OK.name());
                guidToLifeCycleParameters.get(unitGuid).putParameterValue(
                    LogbookParameterName.outcomeDetailMessage,
                    VitamLogbookMessages.getCodeLfc(itemStatus.getItemId(), StatusCode.OK));
                LOGBOOK_LIFECYCLE_CLIENT.update(guidToLifeCycleParameters.get(unitGuid));
            }

            // 2- Update temporary files
            final File unitTmpFileForRead = PropertiesUtils.fileFromTmpFolder(ARCHIVE_UNIT_TMP_FILE_PREFIX + unitGuid);
            final File unitCompleteTmpFile = PropertiesUtils.fileFromTmpFolder(unitGuid);
            final XMLEventWriter writer = xmlOutputFactory.createXMLEventWriter(new FileWriter(unitCompleteTmpFile));
            final XMLEventReader reader = xmlInputFactory.createXMLEventReader(new FileReader(unitTmpFileForRead));

            // Add root tag
            writer.add(eventFactory.createStartDocument());
            writer.add(eventFactory.createStartElement("", "", IngestWorkflowConstants.ROOT_TAG));
            boolean startCopy = false;
            while (true) {
                final XMLEvent event = reader.nextEvent();
                if (event.isStartElement() && event.asStartElement().getName().getLocalPart().equals(ARCHIVE_UNIT)) {
                    startCopy = true;
                }

                if (startCopy) {
                    writer.add(event);
                }

                if (event.isEndElement() && event.asEndElement().getName().getLocalPart().equals(ARCHIVE_UNIT)) {
                    writer.add(eventFactory.createStartElement("", "", IngestWorkflowConstants.WORK_TAG));

                    // Get parents list
                    // Add _up tag
                    writer.add(eventFactory.createStartElement("", "", IngestWorkflowConstants.UP_FIELD));

                    if (archiveUnitTree.has(unitId)) {
                        final JsonNode archiveNode = archiveUnitTree.get(unitId);
                        if (archiveNode.has(IngestWorkflowConstants.UP_FIELD)) {
                            final JsonNode archiveUps = archiveNode.get(IngestWorkflowConstants.UP_FIELD);
                            if (archiveUps.isArray() && archiveUps.size() > 0) {
                                writer.add(eventFactory.createCharacters(getUnitParents((ArrayNode) archiveUps)));
                            }
                        }
                    }

                    writer.add(eventFactory.createEndElement("", "", IngestWorkflowConstants.UP_FIELD));
                    writer.add(eventFactory.createEndElement("", "", IngestWorkflowConstants.WORK_TAG));
                    break;
                }
            }

            writer.add(eventFactory.createEndElement("", "", IngestWorkflowConstants.ROOT_TAG));
            writer.add(eventFactory.createEndDocument());

            writer.close();
            reader.close();

            // Write to workspace
            try {
                client.putObject(containerId, path + "/" + unitGuid + XML_EXTENSION,
                    new FileInputStream(unitCompleteTmpFile));
            } catch (final ContentAddressableStorageServerException e) {
                LOGGER.error("Can not write to workspace ", e);
                if (!unitCompleteTmpFile.delete()) {
                    LOGGER.warn(FILE_COULD_NOT_BE_DELETED_MSG);
                }

                if (!unitTmpFileForRead.delete()) {
                    LOGGER.warn(FILE_COULD_NOT_BE_DELETED_MSG);
                }

                throw new ProcessingException(e);
            }

            if (!unitTmpFileForRead.delete()) {
                LOGGER.warn(FILE_COULD_NOT_BE_DELETED_MSG);
            }

            if (!unitCompleteTmpFile.delete()) {
                LOGGER.warn(FILE_COULD_NOT_BE_DELETED_MSG);
            }
        }
    }

    private String getUnitParents(ArrayNode parents) {
        final StringBuilder parentsList = new StringBuilder();
        for (final JsonNode currentParentNode : parents) {
            final String currentParentId = currentParentNode.asText();
            if (unitIdToGuid.containsKey(currentParentId)) {
                parentsList.append(unitIdToGuid.get(currentParentId));
                parentsList.append(IngestWorkflowConstants.UPS_SEPARATOR);
            }
        }

        // Remove last separator
        parentsList.replace(parentsList.length() - 1, parentsList.length(), "");
        return parentsList.toString();
    }

    private void writeArchiveUnitToTmpDir(WorkspaceClient client, String containerId, XMLEventReader reader,
        StartElement startElement, ObjectNode archiveUnitTree, String path) throws ProcessingException {

        try {
            // Get ArchiveUnit Id
            final String archiveUnitId = startElement.getAttributeByName(new QName(ARCHIVE_UNIT_ELEMENT_ID_ATTRIBUTE))
                .getValue();

            final List<String> createdGuids = extractArchiveUnitToLocalFile(reader, startElement,
                archiveUnitId, archiveUnitTree);

            if (createdGuids != null && !createdGuids.isEmpty()) {
                for (final String currentGuid : createdGuids) {
                    // Create Archive Unit LifeCycle
                    createUnitLifeCycle(currentGuid, containerId);
                }
            }
        } catch (final ProcessingException e) {
            LOGGER.error("Can not extract Object from SEDA XMLStreamException", e);
            throw e;
        } catch (final LogbookClientBadRequestException e) {
            LOGGER.error(LOGBOOK_LF_BAD_REQUEST_EXCEPTION_MSG, e);
            throw new ProcessingException(e);
        } catch (final LogbookClientAlreadyExistsException e) {
            LOGGER.error(LOGBOOK_LF_OBJECT_EXISTS_EXCEPTION_MSG, e);
            throw new ProcessingException(e);
        } catch (final LogbookClientServerException e) {
            LOGGER.error(LOGBOOK_SERVER_INTERNAL_EXCEPTION_MSG, e);
            throw new ProcessingException(e);
        }
    }

    private void checkArchiveUnitIdReference() throws ProcessingException {

        if (unitIdToGroupId != null && !unitIdToGroupId.isEmpty()) {
            for (final Entry<String, String> entry : unitIdToGroupId.entrySet()) {
                if (objectGroupIdToGuid.get(entry.getValue()) == null) {
                    final String groupId = binaryDataObjectIdToObjectGroupId.get(entry.getValue()); // the AU reference
                                                                                                    // an BDO
                    if (Strings.isNullOrEmpty(groupId)) {
                        throw new ProcessingException("Archive Unit references a BDO Id but is not correct");
                    } else {
                        if (!groupId.equals(entry.getValue())) {
                            throw new ProcessingException(
                                "The archive unit " + entry.getKey() + " references one BDO Id " + entry.getValue() +
                                    " while this BDO has a GOT id " + groupId);
                        }
                    }
                }
            }
        }
    }

    private String writeBinaryDataObjectInLocal(XMLEventReader reader, StartElement startElement, String containerId)
        throws ProcessingException {
        final String elementGuid = GUIDFactory.newGUID().toString();
        final File tmpFile = PropertiesUtils.fileFromTmpFolder(elementGuid + JSON_EXTENSION);
        final XMLEventFactory eventFactory = XMLEventFactory.newInstance();
        final JsonXMLConfig config = new JsonXMLConfigBuilder().build();
        String groupGuid = null;
        try {
            final FileWriter tmpFileWriter = new FileWriter(tmpFile);

            final XMLEventWriter writer = new JsonXMLOutputFactory(config).createXMLEventWriter(tmpFileWriter);

            final Iterator<?> it = startElement.getAttributes();
            String binaryObjectId = "";
            BinaryObjectInfo bo = new BinaryObjectInfo();

            if (it.hasNext()) {
                binaryObjectId = ((Attribute) it.next()).getValue();
                binaryDataObjectIdToGuid.put(binaryObjectId, elementGuid);
                binaryDataObjectIdToObjectGroupId.put(binaryObjectId, "");
                writer.add(eventFactory.createStartDocument());
                writer.add(eventFactory.createStartElement("", "", startElement.getName().getLocalPart()));
                writer.add(eventFactory.createStartElement("", "", SedaConstants.PREFIX_ID));
                writer.add(eventFactory.createCharacters(binaryObjectId));
                writer.add(eventFactory.createEndElement("", "", SedaConstants.PREFIX_ID));
            }
            while (true) {
                boolean writable = true;
                final XMLEvent event = reader.nextEvent();
                if (event.isEndElement()) {
                    final EndElement end = event.asEndElement();
                    if (BINARY_DATA_OBJECT.equals(end.getName().getLocalPart())) {
                        writer.add(event);
                        writer.add(eventFactory.createEndDocument());
                        objectGuidToBinaryObject.put(elementGuid, bo);
                        break;
                    }
                }

                if (event.isStartElement()) {
                    final String localPart = event.asStartElement().getName().getLocalPart();

                    // extract info for version DBO
                    switch (localPart) {
                        case SedaConstants.TAG_DO_VERSION: {
                            final String version = reader.getElementText();
                            binaryDataObjectIdToVersionDataObject.put(binaryObjectId, version);
                            bo.setVersion(version);
                            writer.add(eventFactory.createStartElement("", "", localPart));
                            writer.add(eventFactory.createCharacters(version));
                            writer.add(eventFactory.createEndElement("", "", localPart));
                            break;
                        }
                        case DATA_OBJECT_GROUPID: {
                            groupGuid = GUIDFactory.newGUID().toString();
                            final String groupId = reader.getElementText();
                            // Having DataObjectGroupID after a DataObjectGroupReferenceID in the XML flow .
                            // We get the GUID defined earlier during the DataObjectGroupReferenceID analysis
                            if (objectGroupIdToGuidTmp.get(groupId) != null) {
                                groupGuid = objectGroupIdToGuidTmp.get(groupId);
                                objectGroupIdToGuidTmp.remove(groupId);
                            }
                            binaryDataObjectIdToObjectGroupId.put(binaryObjectId, groupId);
                            objectGroupIdToGuid.put(groupId, groupGuid);

                            // Create OG lifeCycle
                            createObjectGroupLifeCycle(groupGuid, containerId);
                            if (objectGroupIdToBinaryDataObjectId.get(groupId) == null) {
                                final List<String> binaryOjectList = new ArrayList<String>();
                                binaryOjectList.add(binaryObjectId);
                                objectGroupIdToBinaryDataObjectId.put(groupId, binaryOjectList);
                            } else {
                                objectGroupIdToBinaryDataObjectId.get(groupId).add(binaryObjectId);
                            }

                            // Create new startElement for group with new guid
                            writer.add(eventFactory.createStartElement("", "", DATA_OBJECT_GROUPID));
                            writer.add(eventFactory.createCharacters(groupGuid));
                            writer.add(eventFactory.createEndElement("", "", DATA_OBJECT_GROUPID));
                            break;
                        }
                        case SedaConstants.TAG_DATA_OBJECT_GROUP_REFERENCEID: {
                            final String groupId = reader.getElementText();
                            String groupGuidTmp = GUIDFactory.newGUID().toString();
                            binaryDataObjectIdToObjectGroupId.put(binaryObjectId, groupId);
                            // The DataObjectGroupReferenceID is after DataObjectGroupID in the XML flow
                            if (objectGroupIdToBinaryDataObjectId.get(groupId) != null) {
                                objectGroupIdToBinaryDataObjectId.get(groupId).add(binaryObjectId);
                                groupGuidTmp = objectGroupIdToGuid.get(groupId);
                            } else {
                                // The DataObjectGroupReferenceID is before DataObjectGroupID in the XML flow
                                final List<String> binaryOjectList = new ArrayList<String>();
                                binaryOjectList.add(binaryObjectId);
                                objectGroupIdToBinaryDataObjectId.put(groupId, binaryOjectList);
                                objectGroupIdToGuidTmp.put(groupId, groupGuidTmp);

                            }

                            // Create new startElement for group with new guid
                            writer.add(eventFactory.createStartElement("", "", DATA_OBJECT_GROUPID));
                            writer.add(eventFactory.createCharacters(groupGuidTmp));
                            writer.add(eventFactory.createEndElement("", "", DATA_OBJECT_GROUPID));
                            break;
                        }
                        case SedaConstants.TAG_URI: {
                            final String uri = reader.getElementText();
                            bo.setUri(uri);
                            break;
                        }
                        case SedaConstants.TAG_SIZE: {
                            final long size = Long.parseLong(reader.getElementText());
                            bo.setSize(size);
                            break;
                        }
                        case SedaConstants.TAG_DIGEST: {
                            final String messageDigest = reader.getElementText();
                            bo.setMessageDigest(messageDigest);
                            final Iterator<?> it1 = event.asStartElement().getAttributes();

                            if (it1.hasNext()) {
                                String al = ((Attribute) it1.next()).getValue();
                                DigestType d = DigestType.fromValue(al);
                                bo.setAlgo(d);
                            }
                            break;
                        }
                        default:
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

            if (Strings.isNullOrEmpty(binaryDataObjectIdToObjectGroupId.get(binaryObjectId))) {
                // not have object group, must creat an technical object group
                LOGGER.debug("BDO {} not have an GDO", binaryObjectId);
                binaryDataObjectIdToObjectGroupId.remove(binaryObjectId);
                postBinaryDataObjectActions(elementGuid + JSON_EXTENSION, binaryObjectId);
            }


        } catch (final XMLStreamException e) {
            LOGGER.debug("Can not read input stream");
            throw new ProcessingException(e);
        } catch (final InvalidParseOperationException e) {
            LOGGER.debug("Can not parse binary data object json");
            throw new ProcessingException(e);
        } catch (final IOException e) {
            LOGGER.debug("Closing stream error");
            throw new ProcessingException(e);
        } catch (final LogbookClientBadRequestException e) {
            LOGGER.error(LOGBOOK_LF_BAD_REQUEST_EXCEPTION_MSG, e);
            throw new ProcessingException(e);
        } catch (final LogbookClientAlreadyExistsException e) {
            LOGGER.error(LOGBOOK_LF_OBJECT_EXISTS_EXCEPTION_MSG, e);
            throw new ProcessingException(e);
        } catch (final LogbookClientServerException e) {
            LOGGER.error(LOGBOOK_SERVER_INTERNAL_EXCEPTION_MSG, e);
            throw new ProcessingException(e);
        }

        return groupGuid;

    }

    /**
     * Post actions when reading binary data object in manifest
     *
     * @param jsonFileName
     * @param binaryDataOjectId
     * @return
     * @throws InvalidParseOperationException
     */
    private JsonNode postBinaryDataObjectActions(final String jsonFileName, final String binaryDataOjectId)
        throws InvalidParseOperationException {
        final File tmpJsonFile = PropertiesUtils.fileFromTmpFolder(jsonFileName);
        final JsonNode jsonBDO = JsonHandler.getFromFile(tmpJsonFile);
        // FIXME P0 are you sure it is ALWAYS a BINARY MASTER here ?
        binaryDataObjectIdToVersionDataObject.put(binaryDataOjectId, BINARY_MASTER);
        JsonNode objectNode = mapNewTechnicalDataObjectGroupToBDO(jsonBDO, binaryDataOjectId);
        objectNode = addExtraField(objectNode);
        JsonHandler.writeAsFile(objectNode,
            PropertiesUtils.fileFromTmpFolder(jsonFileName)); // write the new BinaryDataObject
        return objectNode;
    }

    /**
     * add fields that is missing in manifest
     *
     * @param objectNode
     * @return
     */
    private JsonNode addExtraField(JsonNode objectNode) {
        final ObjectNode bdoObjNode = (ObjectNode) objectNode.get(BINARY_DATA_OBJECT);
        if (bdoObjNode.get(SedaConstants.TAG_DO_VERSION) == null ||
            Strings.isNullOrEmpty(bdoObjNode.get(SedaConstants.TAG_DO_VERSION).textValue())) {
            bdoObjNode.put(SedaConstants.TAG_DO_VERSION, BINARY_MASTER);
        }
        return JsonHandler.createObjectNode().set(BINARY_DATA_OBJECT, bdoObjNode);
    }

    /**
     * Creation of the technical new object group and update the maps
     *
     * @param jsonBDO
     * @param binaryDataOjectId
     * @return
     */
    private JsonNode mapNewTechnicalDataObjectGroupToBDO(JsonNode jsonBDO, String binaryDataOjectId) {
        final JsonNode bdo = jsonBDO.get(BINARY_DATA_OBJECT);
        final ObjectNode bdoObjNode = (ObjectNode) bdo;

        final String technicalGotGuid = GUIDFactory.newObjectGroupGUID(0).toString();
        objectGroupIdToGuid.put(technicalGotGuid, technicalGotGuid); // update object group id guid
        bdoObjNode.put(DATA_OBJECT_GROUPID, technicalGotGuid);

        if (Strings.isNullOrEmpty(binaryDataObjectIdToObjectGroupId.get(binaryDataOjectId))) {
            binaryDataObjectIdToObjectGroupId.put(binaryDataOjectId, technicalGotGuid);
        } else {
            LOGGER.warn("unexpected state - binaryDataObjectIdToObjectGroupId contains the GOT and should not");
        }

        final String gotGuid = binaryDataObjectIdWithoutObjectGroupId.get(binaryDataOjectId) != null
            ? binaryDataObjectIdWithoutObjectGroupId.get(binaryDataOjectId).getGotGuid() : "";
        if (Strings.isNullOrEmpty(gotGuid)) {
            final GotObj gotObj = new GotObj(technicalGotGuid, false);
            binaryDataObjectIdWithoutObjectGroupId.put(binaryDataOjectId, gotObj);
            binaryDataObjectIdToObjectGroupId
                .put(binaryDataOjectId, technicalGotGuid); // update the list of bdo in the map
        } else {
            LOGGER.warn("unexpected state - binaryDataObjectIdWithoutObjectGroupId contains the GOT and should not");
        }

        List<String> listBDO = objectGroupIdToBinaryDataObjectId.get(technicalGotGuid);
        if (listBDO != null && !listBDO.contains(technicalGotGuid)) {
            listBDO.add(binaryDataOjectId);
            objectGroupIdToBinaryDataObjectId.put(technicalGotGuid, listBDO);
        } else {
            listBDO = new ArrayList<String>();
            listBDO.add(binaryDataOjectId);
            objectGroupIdToBinaryDataObjectId.put(technicalGotGuid, listBDO);
        }

        return jsonBDO;
    }

    private void createObjectGroupLifeCycle(String groupGuid, String containerId)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException {
        final LogbookLifeCycleObjectGroupParameters logbookLifecycleObjectGroupParameters =
            (LogbookLifeCycleObjectGroupParameters) initLogbookLifeCycleParameters(
                groupGuid, false, true);

        logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.eventIdentifierProcess,
            containerId);
        logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.eventIdentifier,
            GUIDFactory.newEventGUID(0).toString());
        logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.eventTypeProcess,
            LIFE_CYCLE_EVENT_TYPE_PROCESS);

        // TODO P0 WORKFLOW add treatment code for create objectGroup
        logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.eventType,
            HANDLER_ID);
        logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcome,
            StatusCode.STARTED.toString());
        logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcomeDetail,
            StatusCode.STARTED.toString());
        // TODO P0 WORKFLOW code outcomeDeatilsMessage started
        logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
            VitamLogbookMessages.getCodeLfc(HANDLER_ID, StatusCode.STARTED));
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
        LOGGER.debug("Begin createIngestLevelStackFile/containerId: {}", containerId);
        ParametersChecker.checkParameter("levelStackMap is a mandatory parameter", levelStackMap);
        ParametersChecker.checkParameter("unitIdToGuid is a mandatory parameter", unitIdToGuid);
        ParametersChecker.checkParameter(WORKSPACE_MANDATORY_MSG, client);

        File tempFile = null;
        try {
            tempFile = File.createTempFile(TMP_FOLDER, INGEST_LEVEL_STACK);
            // tempFile will be deleted on exit
            tempFile.deleteOnExit();
            // create level json object node
            final ObjectNode IngestLevelStack = JsonHandler.createObjectNode();
            for (final Entry<Integer, Set<String>> entry : levelStackMap.entrySet()) {
                final ArrayNode unitList = IngestLevelStack.withArray(LEVEL + entry.getKey());
                final Set<String> unitGuidList = entry.getValue();
                for (final String idXml : unitGuidList) {

                    final String unitGuid = unitIdToGuid.get(idXml);
                    if (unitGuid == null) {
                        throw new IllegalArgumentException("Unit guid not found in map");
                    }
                    unitList.add(unitGuid);
                }
                IngestLevelStack.set(LEVEL + entry.getKey(), unitList);
            }
            LOGGER.debug("IngestLevelStack: {}", IngestLevelStack);
            // create json file
            JsonHandler.writeAsFile(IngestLevelStack, tempFile);
            // put file in workspace
            client.putObject(containerId, path,
                new FileInputStream(tempFile));
        } catch (final IOException e) {
            LOGGER.error(e);
            throw new ProcessingException(e);
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            throw new ProcessingException(e);
        } catch (final ContentAddressableStorageServerException e) {
            LOGGER.error(e);
            throw new ProcessingException(e);
        } finally {
            if (tempFile != null) {
                tempFile.exists();
            }
        }
        LOGGER.info("End createIngestLevelStackFile/containerId:" + containerId);

    }

    /**
     * Get the object group id defined in binary data object or the binary data object without GO. In this map the new
     * technical object is created
     *
     * @param objIdRefByUnit il s'agit du DataObjectGroupReferenceId
     * @return
     */
    private String getNewGdoIdFromGdoByUnit(String objIdRefByUnit) throws ProcessingException {

        final String gotGuid = binaryDataObjectIdWithoutObjectGroupId.get(objIdRefByUnit) != null
            ? binaryDataObjectIdWithoutObjectGroupId.get(objIdRefByUnit).getGotGuid() : null;

        if (Strings.isNullOrEmpty(binaryDataObjectIdToObjectGroupId.get(objIdRefByUnit)) &&
            !Strings.isNullOrEmpty(gotGuid)) {

            // nominal case of bdo without go
            LOGGER.debug("The binary data object id " + objIdRefByUnit +
                ", is defined without the group object id " +
                binaryDataObjectIdWithoutObjectGroupId.get(objIdRefByUnit) +
                ". The technical group object guid is " + gotGuid);
            return gotGuid;

        } else if (!Strings.isNullOrEmpty(binaryDataObjectIdToObjectGroupId.get(objIdRefByUnit))) {
            LOGGER.debug("The binary data object id " + binaryDataObjectIdWithoutObjectGroupId.get(objIdRefByUnit) +
                " referenced defined with the group object id " + objIdRefByUnit);
            // il y a un BDO possédant le GO id
            return binaryDataObjectIdToObjectGroupId.get(objIdRefByUnit);
        } else if (binaryDataObjectIdToObjectGroupId.containsValue(objIdRefByUnit)) {
            // case objIdRefByUnit is an GO
            return objIdRefByUnit;
        } else {
            throw new ProcessingException(
                "The group id " + objIdRefByUnit + " doesn't reference an bdo or go and it not include in bdo");
        }
    }

    private LogbookParameters initLogbookLifeCycleParameters(String guid, boolean isArchive, boolean isObjectGroup) {
        LogbookParameters logbookLifeCycleParameters = guidToLifeCycleParameters.get(guid);
        if (logbookLifeCycleParameters == null) {
            logbookLifeCycleParameters = isArchive ? LogbookParametersFactory.newLogbookLifeCycleUnitParameters()
                : isObjectGroup ? LogbookParametersFactory.newLogbookLifeCycleObjectGroupParameters()
                    : LogbookParametersFactory.newLogbookOperationParameters();

            logbookLifeCycleParameters.putParameterValue(LogbookParameterName.objectIdentifier, guid);
        }
        return logbookLifeCycleParameters;
    }


    private void createUnitLifeCycle(String unitGuid, String containerId)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException {
        final LogbookLifeCycleUnitParameters logbookLifecycleUnitParameters =
            (LogbookLifeCycleUnitParameters) initLogbookLifeCycleParameters(
                unitGuid, true, false);

        logbookLifecycleUnitParameters.putParameterValue(LogbookParameterName.eventIdentifierProcess, containerId);
        logbookLifecycleUnitParameters.putParameterValue(LogbookParameterName.eventIdentifier,
            GUIDFactory.newEventGUID(0).toString());
        logbookLifecycleUnitParameters.putParameterValue(LogbookParameterName.eventTypeProcess,
            LIFE_CYCLE_EVENT_TYPE_PROCESS);
        logbookLifecycleUnitParameters.putParameterValue(LogbookParameterName.eventType,
            UNIT_LIFE_CYCLE_CREATION_EVENT_TYPE);
        logbookLifecycleUnitParameters.putParameterValue(LogbookParameterName.outcome,
            StatusCode.STARTED.toString());
        logbookLifecycleUnitParameters.putParameterValue(LogbookParameterName.outcomeDetail,
            StatusCode.STARTED.toString());
        logbookLifecycleUnitParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
            VitamLogbookMessages.getCodeLfc(HANDLER_ID, StatusCode.STARTED));
        LOGBOOK_LIFECYCLE_CLIENT.create(logbookLifecycleUnitParameters);

        // Update guidToLifeCycleParameters
        guidToLifeCycleParameters.put(unitGuid, logbookLifecycleUnitParameters);
    }


    private List<String> extractArchiveUnitToLocalFile(XMLEventReader reader, StartElement startElement,
        String archiveUnitId, ObjectNode archiveUnitTree) throws ProcessingException {

        // Map<String, File> archiveUnitToTmpFileMap = new HashMap<>();
        final List<String> archiveUnitGuids = new ArrayList<String>();


        final String elementGuid = GUIDFactory.newGUID().toString();
        boolean isReferencedArchive = false;

        final XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
        final XMLEventFactory eventFactory = XMLEventFactory.newInstance();
        final String elementID = ((Attribute) startElement.getAttributes().next()).getValue();
        final QName name = startElement.getName();
        int stack = 1;
        // TODO P0 : check why the use of this key (concatenation)
        // final File tmpFile = PropertiesUtils.fileFromTmpFolder(GUIDFactory.newGUID().toString() + elementGuid);
        final File tmpFile = PropertiesUtils.fileFromTmpFolder(ARCHIVE_UNIT_TMP_FILE_PREFIX + elementGuid);
        String groupGuid;
        XMLEventWriter writer;

        final QName unitName = new QName(SedaConstants.NAMESPACE_URI, ARCHIVE_UNIT);
        final QName archiveUnitRefIdTag = new QName(SedaConstants.NAMESPACE_URI, ARCHIVE_UNIT_REF_ID_TAG);

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
            writer.add(eventFactory.createStartElement("", SedaConstants.NAMESPACE_URI,
                startElement.getName().getLocalPart()));
            writer.add(eventFactory.createAttribute("id", elementGuid));
            while (true) {
                final XMLEvent event = reader.nextEvent();
                if (event.isStartElement() && event.asStartElement().getName().equals(name)) {
                    final String currentArchiveUnit = event.asStartElement()
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
                            writer.add(eventFactory.createStartElement("", "", SedaConstants.PREFIX_OG));
                            writer.add(eventFactory.createCharacters(groupGuid));
                            writer.add(eventFactory.createEndElement("", "", SedaConstants.PREFIX_OG));

                            writer.add(event);
                            break;
                        }
                    }
                }
                if (event.isStartElement() &&
                    SedaConstants.TAG_DATA_OBJECT_GROUP_REFERENCEID.equals(event.asStartElement().getName()
                        .getLocalPart())) {
                    final String groupId = reader.getElementText();
                    unitIdToGroupId.put(elementID, groupId);
                    if (objectGroupIdToUnitId.get(groupId) == null) {
                        final List<String> archiveUnitList = new ArrayList<>();
                        archiveUnitList.add(elementID);
                        if (!binaryDataObjectIdWithoutObjectGroupId.containsKey(groupId)) {
                            objectGroupIdToUnitId.put(groupId, archiveUnitList);
                        }
                    } else {
                        final List<String> archiveUnitList = objectGroupIdToUnitId.get(groupId);
                        archiveUnitList.add(elementID);
                        objectGroupIdToUnitId.put(groupId, archiveUnitList);
                    }
                    // Create new startElement for group with new guid
                    groupGuid = objectGroupIdToGuid.get(unitIdToGroupId.get(elementID));
                    final String newGroupId = getNewGdoIdFromGdoByUnit(unitIdToGroupId.get(elementID));
                    writer.add(eventFactory.createStartElement("", SedaConstants.NAMESPACE_URI,
                        SedaConstants.TAG_DATA_OBJECT_GROUP_REFERENCEID));
                    writer.add(eventFactory.createCharacters(newGroupId));
                    writer.add(eventFactory.createEndElement("", SedaConstants.NAMESPACE_URI,
                        SedaConstants.TAG_DATA_OBJECT_GROUP_REFERENCEID));

                } else if (event.isStartElement() &&
                    SedaConstants.TAG_DATA_OBJECT_REFERENCEID.equals(event.asStartElement().getName().getLocalPart())) {

                    final String objRefId = reader.getElementText();
                    unitIdToGroupId.put(elementID, objRefId);
                    if (objectGroupIdToUnitId.get(objRefId) == null) {
                        final List<String> archiveUnitList = new ArrayList<>();
                        archiveUnitList.add(elementID);
                        if (binaryDataObjectIdWithoutObjectGroupId.containsKey(objRefId)) {
                            final GotObj gotObj = binaryDataObjectIdWithoutObjectGroupId.get(objRefId);
                            final String gotGuid = gotObj.getGotGuid().toString();
                            objectGroupIdToUnitId.put(gotGuid, archiveUnitList);
                            unitIdToGroupId.put(elementID, gotGuid); // update unitIdToGroupId with new GOT
                            gotObj.setVisited(true); // update isVisited to true
                            binaryDataObjectIdWithoutObjectGroupId.put(objRefId, gotObj);
                        }
                    } else {
                        final List<String> archiveUnitList = objectGroupIdToUnitId.get(objRefId);
                        archiveUnitList.add(elementID);
                        objectGroupIdToUnitId.put(objRefId, archiveUnitList);
                    }

                    final String newGroupId = getNewGdoIdFromGdoByUnit(objRefId);
                    writer.add(eventFactory.createStartElement("", SedaConstants.NAMESPACE_URI,
                        SedaConstants.TAG_DATA_OBJECT_GROUP_REFERENCEID));
                    writer.add(eventFactory.createCharacters(newGroupId));
                    writer.add(eventFactory.createEndElement("", SedaConstants.NAMESPACE_URI,
                        SedaConstants.TAG_DATA_OBJECT_GROUP_REFERENCEID));


                } else if (event.isStartElement() && event.asStartElement().getName().equals(unitName)) {

                    // Update archiveUnitTree
                    final String nestedArchiveUnitId = event.asStartElement()
                        .getAttributeByName(new QName(ARCHIVE_UNIT_ELEMENT_ID_ATTRIBUTE)).getValue();

                    ObjectNode nestedArchiveUnitNode = (ObjectNode) archiveUnitTree.get(nestedArchiveUnitId);
                    if (nestedArchiveUnitNode == null) {
                        // Create new Archive Unit Node
                        nestedArchiveUnitNode = JsonHandler.createObjectNode();
                    }

                    // Add immediate parents
                    final ArrayNode parentsField = nestedArchiveUnitNode.withArray(SedaConstants.PREFIX_UP);
                    parentsField.add(archiveUnitId);

                    // Update global tree
                    archiveUnitTree.set(nestedArchiveUnitId, nestedArchiveUnitNode);

                    // Process Archive Unit element: recursive call
                    archiveUnitGuids.addAll(extractArchiveUnitToLocalFile(reader, event.asStartElement(),
                        nestedArchiveUnitId, archiveUnitTree));
                } else if (event.isStartElement() && event.asStartElement().getName().equals(archiveUnitRefIdTag)) {
                    // Referenced Child Archive Unit
                    final String childArchiveUnitRef = reader.getElementText();

                    ObjectNode childArchiveUnitNode = (ObjectNode) archiveUnitTree.get(childArchiveUnitRef);
                    if (childArchiveUnitNode == null) {
                        // Create new Archive Unit Node
                        childArchiveUnitNode = JsonHandler.createObjectNode();
                    }

                    // Reference Management during tree creation
                    final ArrayNode parentsField = childArchiveUnitNode.withArray(SedaConstants.PREFIX_UP);
                    parentsField.addAll((ArrayNode) archiveUnitTree.get(archiveUnitId).get(SedaConstants.PREFIX_UP));
                    archiveUnitTree.set(childArchiveUnitRef, childArchiveUnitNode);
                    archiveUnitTree.without(archiveUnitId);

                    // Set isReferencedArchive to true so we can remove this unit from unitIdToGuid (no lifeCycle for
                    // this unit because it will not be indexed)
                    isReferencedArchive = true;
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

        if (isReferencedArchive) {
            // Remove this unit from unitIdToGuid (no lifeCycle for this unit because it will not be indexed)
            unitIdToGuid.remove(elementID);

            // delete created temporary file
            tmpFile.delete();
        } else {
            archiveUnitGuids.add(elementGuid);
            // archiveUnitToTmpFileMap.put(elementGuid, tmpFile);
        }

        return archiveUnitGuids;
    }

    private void saveObjectGroupsToWorkspace(WorkspaceClient client, String containerId) throws ProcessingException {

        completeBinaryObjectToObjectGroupMap();

        // Save maps
        try {
            // Save binaryDataObjectIdToObjectGroupId
            HandlerUtils.saveMap(containerId, binaryDataObjectIdToObjectGroupId,
                (String) handlerIO.getOutput().get(BDO_ID_TO_OG_ID_IO_RANK), client,
                true);
            // Save objectGroupIdToGuid
            HandlerUtils.saveMap(containerId, objectGroupIdToGuid,
                (String) handlerIO.getOutput().get(OG_ID_TO_GUID_IO_RANK), client,
                true);
        } catch (final IOException e1) {
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
                final Map<String, ArrayList<JsonNode>> categoryMap = new HashMap<>();
                objectGroup.put(SedaConstants.PREFIX_ID, objectGroupGuid);
                objectGroup.put(SedaConstants.PREFIX_TENANT_ID, 0);
                List<String> versionList = new ArrayList<>();
                for (int index = 0; index < entry.getValue().size(); index++) {
                    final String id = entry.getValue().get(index);
                    final File binaryObjectFile = PropertiesUtils
                        .fileFromTmpFolder(binaryDataObjectIdToGuid.get(id) + JSON_EXTENSION);
                    final JsonNode binaryNode = JsonHandler.getFromFile(binaryObjectFile).get("BinaryDataObject");
                    String nodeCategory = "";
                    if (binaryNode.get(SedaConstants.TAG_DO_VERSION) != null) {
                        nodeCategory = binaryNode.get(SedaConstants.TAG_DO_VERSION).asText();
                        if (versionList.contains(nodeCategory)) {
                            LOGGER.error(BINARY_DATA_OBJECT_VERSION_MUST_BE_UNIQUE);
                            throw new ProcessingException(BINARY_DATA_OBJECT_VERSION_MUST_BE_UNIQUE);
                        }
                        versionList.add(nodeCategory);
                    }
                    ArrayList<JsonNode> nodeCategoryArray = categoryMap.get(nodeCategory);
                    if (nodeCategory.split("_").length == 1) {
                        nodeCategory += "_1";
                        ((ObjectNode) binaryNode).put(SedaConstants.TAG_DO_VERSION, nodeCategory);
                    }
                    if (nodeCategoryArray == null) {
                        nodeCategoryArray = new ArrayList<>();
                        nodeCategoryArray.add(binaryNode);
                    } else {
                        int binaryNodePosition = Integer.parseInt(nodeCategory.split("_")[1]) -1;
                        nodeCategoryArray.add(binaryNodePosition, binaryNode);
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

                if (objectGroupIdToUnitId != null && objectGroupIdToUnitId.size() != 0) {
                    if (objectGroupIdToUnitId.get(entry.getKey()) != null) {
                        for (final String objectGroupId : objectGroupIdToUnitId.get(entry.getKey())) {
                            if (unitIdToGuid.get(objectGroupId) != null) {
                                unitParent.add(unitIdToGuid.get(objectGroupId));
                            }
                        }
                    }
                }

                objectGroup.put(SedaConstants.PREFIX_TYPE, objectGroupType);
                objectGroup.set(SedaConstants.TAG_FILE_INFO, fileInfo);
                final ObjectNode qualifiersNode = getObjectGroupQualifiers(categoryMap);
                objectGroup.set(SedaConstants.PREFIX_QUALIFIERS, qualifiersNode);
                final ObjectNode workNode = getObjectGroupWork(categoryMap);
                objectGroup.set(SedaConstants.PREFIX_WORK, workNode);
                objectGroup.set(SedaConstants.PREFIX_UP, unitParent);
                objectGroup.put(SedaConstants.PREFIX_NB, entry.getValue().size());
                // Add operation to OPS
                objectGroup.putArray(SedaConstants.PREFIX_OPS).add(containerId);
                JsonHandler.writeAsFile(objectGroup, tmpFile);

                client.putObject(containerId,
                    IngestWorkflowConstants.OBJECT_GROUP_FOLDER + "/" + objectGroupGuid + JSON_EXTENSION,
                    new FileInputStream(tmpFile));
                if (!tmpFile.delete()) {
                    LOGGER.warn(FILE_COULD_NOT_BE_DELETED_MSG);
                }
                // Create unreferenced object group
                if (guidToLifeCycleParameters.get(objectGroupGuid) == null) {
                    createObjectGroupLifeCycle(objectGroupGuid, containerId);
                    // Update Object Group lifeCycle creation event
                    guidToLifeCycleParameters.get(objectGroupGuid).setStatus(StatusCode.OK);
                    guidToLifeCycleParameters.get(objectGroupGuid).putParameterValue(LogbookParameterName.outcomeDetail,
                        StatusCode.OK.name());
                    guidToLifeCycleParameters.get(objectGroupGuid).putParameterValue(
                        LogbookParameterName.outcomeDetailMessage,
                        VitamLogbookMessages.getCodeLfc(HANDLER_ID, StatusCode.OK));
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
            } catch (final LogbookClientBadRequestException e) {
                LOGGER.error(LOGBOOK_LF_BAD_REQUEST_EXCEPTION_MSG, e);
                throw new ProcessingException(e);
            } catch (final LogbookClientAlreadyExistsException e) {
                LOGGER.error(LOGBOOK_LF_OBJECT_EXISTS_EXCEPTION_MSG, e);
                throw new ProcessingException(e);
            } catch (final LogbookClientServerException e) {
                LOGGER.error(LOGBOOK_SERVER_INTERNAL_EXCEPTION_MSG, e);
                throw new ProcessingException(e);
            } catch (final LogbookClientNotFoundException e) {
                LOGGER.error(LOGBOOK_LF_RESOURCE_NOT_FOUND_EXCEPTION_MSG, e);
                throw new ProcessingException(e);
            }
        }
    }


    private ObjectNode getObjectGroupQualifiers(Map<String, ArrayList<JsonNode>> categoryMap) {
        final ObjectNode qualifierObject = JsonHandler.createObjectNode();
        for (final Entry<String, ArrayList<JsonNode>> entry : categoryMap.entrySet()) {
            final ObjectNode binaryNode = JsonHandler.createObjectNode();
            binaryNode.put(SedaConstants.TAG_NB, entry.getValue().size());
            final ArrayNode arrayNode = JsonHandler.createArrayNode();
            for (final JsonNode node : entry.getValue()) {
                final String id = node.findValue(SedaConstants.PREFIX_ID).textValue();
                String guid = binaryDataObjectIdToGuid.get(id);
                ((ObjectNode) node).put(SedaConstants.PREFIX_ID, guid);
                ((ObjectNode) node).put(SedaConstants.TAG_SIZE, objectGuidToBinaryObject.get(guid).getSize());
                ((ObjectNode) node).put(SedaConstants.TAG_URI, objectGuidToBinaryObject.get(guid).getUri());
                ((ObjectNode) node).put(SedaConstants.TAG_DIGEST,
                    objectGuidToBinaryObject.get(guid).getMessageDigest());
                ((ObjectNode) node).put(SedaConstants.ALGORITHM,
                    objectGuidToBinaryObject.get(guid).getAlgo().getName());
                arrayNode.add(node);
            }
            binaryNode.set(SedaConstants.TAG_VERSIONS, arrayNode);
            qualifierObject.set(entry.getKey(), binaryNode);
        }
        return qualifierObject;
    }

    private ObjectNode getObjectGroupWork(Map<String, ArrayList<JsonNode>> categoryMap) {
        final ObjectNode workObject = JsonHandler.createObjectNode();
        final ObjectNode qualifierObject = JsonHandler.createObjectNode();
        for (final Entry<String, ArrayList<JsonNode>> entry : categoryMap.entrySet()) {
            final ObjectNode binaryNode = JsonHandler.createObjectNode();
            binaryNode.put(SedaConstants.TAG_NB, entry.getValue().size());
            final ArrayNode arrayNode = JsonHandler.createArrayNode();
            for (final JsonNode node : entry.getValue()) {
                final ObjectNode objectNode = JsonHandler.createObjectNode();
                final String id = node.findValue(SedaConstants.PREFIX_ID).textValue();
                objectNode.put(SedaConstants.PREFIX_ID, id);
                objectNode.put(SedaConstants.TAG_SIZE, objectGuidToBinaryObject.get(id).getSize());
                objectNode.put(SedaConstants.TAG_URI, objectGuidToBinaryObject.get(id).getUri());
                objectNode.put(SedaConstants.TAG_DIGEST, objectGuidToBinaryObject.get(id).getMessageDigest());
                objectNode.put(SedaConstants.ALGORITHM, objectGuidToBinaryObject.get(id).getAlgo().getName());
                arrayNode.add(objectNode);
            }
            binaryNode.set(SedaConstants.TAG_VERSIONS, arrayNode);
            qualifierObject.set(entry.getKey(), binaryNode);
        }
        workObject.set(SedaConstants.PREFIX_QUALIFIERS, qualifierObject);
        return workObject;
    }

    private void completeBinaryObjectToObjectGroupMap() {
        for (final String key : binaryDataObjectIdToObjectGroupId.keySet()) {
            if ("".equals(binaryDataObjectIdToObjectGroupId.get(key))) {
                final List<String> binaryOjectList = new ArrayList<>();
                binaryOjectList.add(key);
                objectGroupIdToBinaryDataObjectId.put(GUIDFactory.newGUID().toString(), binaryOjectList);
                // TODO P0 Create OG / OG lifeCycle
            }
        }
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        if (handlerIO.getOutput().size() != handlerInitialIOList.getOutput().size()) {
            throw new ProcessingException(HandlerIO.NOT_ENOUGH_PARAM);
        } else if (!HandlerIO.checkHandlerIO(handlerIO, handlerInitialIOList)) {
            throw new ProcessingException(HandlerIO.NOT_CONFORM_PARAM);
        }
    }

    /**
     * This object content the new technical object group guid and the an boolean. It is created when the BDO not
     * nontains an GO with isVisited=false. When the list of AU is browsed, if an AU referenced and the BDO not contains
     * an GO, the boolean of this object change to true
     */
    private static class GotObj {
        private String gotGuid;
        private boolean isVisited;

        public GotObj(String gotGuid, boolean isVisited) {
            this.gotGuid = gotGuid;
            this.isVisited = isVisited;
        }

        public String getGotGuid() {
            return gotGuid;
        }

        @SuppressWarnings("unused")
        public void setGotGuid(String gotGuid) {
            this.gotGuid = gotGuid;
        }

        @SuppressWarnings("unused")
        public boolean isVisited() {
            return isVisited;
        }

        public void setVisited(boolean visited) {
            isVisited = visited;
        }
    }
}
