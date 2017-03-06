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
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.stream.FactoryConfigurationError;
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

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import de.odysseus.staxon.json.JsonXMLConfig;
import de.odysseus.staxon.json.JsonXMLConfigBuilder;
import de.odysseus.staxon.json.JsonXMLOutputFactory;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.builder.request.multiple.Select;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.exception.CycleFoundException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.graph.DirectedCycle;
import fr.gouv.vitam.common.graph.DirectedGraph;
import fr.gouv.vitam.common.graph.Graph;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleObjectGroupParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleUnitParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.metadata.api.exception.MetaDataException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ArchiveUnitContainBinaryDataObjectException;
import fr.gouv.vitam.processing.common.exception.MissingFieldException;
import fr.gouv.vitam.processing.common.exception.ProcessingDuplicatedVersionException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.exception.ProcessingManifestReferenceException;
import fr.gouv.vitam.processing.common.exception.ProcessingUnitNotFoundException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.common.utils.BinaryObjectInfo;
import fr.gouv.vitam.worker.common.utils.IngestWorkflowConstants;
import fr.gouv.vitam.worker.common.utils.SedaConstants;
import fr.gouv.vitam.worker.core.impl.HandlerIOImpl;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

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
    private static final int OG_ID_TO_GUID_IO_MEMORY_RANK = 8;
    private static final int HANDLER_IO_OUT_PARAMETER_NUMBER = 9;


    private static final String HANDLER_ID = "CHECK_MANIFEST";
    private static final String LFC_INITIAL_CREATION_EVENT_TYPE = "LFC_CREATION";
    private static final String LFC_CREATION_SUB_TASK_ID = "LFC_CREATION";
    private static final String LFC_CREATION_SUB_TASK_FULL_ID = HANDLER_ID + "." + LFC_CREATION_SUB_TASK_ID;
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
    // TODO P0 WORKFLOW will be in vitam-logbook file
    private static final String UNIT_LIFE_CYCLE_CREATION_EVENT_TYPE =
        "Check SIP – Units – Lifecycle Logbook Creation – Création du journal du cycle de vie des units";
    private static final String LOGBOOK_LF_BAD_REQUEST_EXCEPTION_MSG = "LogbookClient Unsupported request";
    private static final String LOGBOOK_LF_OBJECT_EXISTS_EXCEPTION_MSG = "LifeCycle Object already exists";
    private static final String LOGBOOK_LF_RESOURCE_NOT_FOUND_EXCEPTION_MSG = "Logbook LifeCycle resource not found";
    private static final String LOGBOOK_SERVER_INTERNAL_EXCEPTION_MSG = "Logbook Server internal error";
    private static final String BINARY_DATA_OBJECT_VERSION_MUST_BE_UNIQUE =
        "ERROR: BinaryDataObject version must be unique";
    private static final String LEVEL = "level_";

    private static final String ARCHIVE_UNIT_ELEMENT_ID_ATTRIBUTE = "id";
    private static final String ARCHIVE_UNIT_REF_ID_TAG = "ArchiveUnitRefId";
    private static final String GRAPH_CYCLE_MSG =
        "The Archive Unit graph in the SEDA file has a cycle";
    private static final String CYCLE_FOUND_EXCEPTION = "Seda has an archive unit cycle ";
    private static final String SAVE_ARCHIVE_ID_TO_GUID_IOEXCEPTION_MSG =
        "Can not save unitToGuidMap to temporary file";
    private static final String FILE_COULD_NOT_BE_DELETED_MSG = "File could not be deleted";
    private static final String CANNOT_READ_SEDA = "Can not read SEDA";
    private static final String MANIFEST_NOT_FOUND = "Manifest.xml Not Found";
    private static final String ARCHIVE_UNIT_TMP_FILE_PREFIX = "AU_TMP_";
    private static final String GLOBAL_MGT_RULE_TAG = "GLOBAL_MGT_RULE";

    private final Map<String, String> binaryDataObjectIdToGuid;
    private final Map<String, String> objectGroupIdToGuid;
    private final Map<String, String> objectGroupIdToGuidTmp;
    private final Map<String, String> unitIdToGuid;
    private final Set<String> existingUnitGuids;

    private final Map<String, String> binaryDataObjectIdToObjectGroupId;
    private final Map<String, List<String>> objectGroupIdToBinaryDataObjectId;
    private final Map<String, String> unitIdToGroupId;
    // this map contains binaryDataObject that not have DataObjectGroupId
    private final Map<String, GotObj> binaryDataObjectIdWithoutObjectGroupId;
    private final Map<String, List<String>> objectGroupIdToUnitId;
    private final Map<String, BinaryObjectInfo> objectGuidToBinaryObject;
    private final Map<String, String> binaryDataObjectIdToVersionDataObject;
    private final Map<String, LogbookLifeCycleParameters> guidToLifeCycleParameters;

    private final List<Class<?>> handlerInitialIOList = new ArrayList<>();
    private File globalSedaParametersFile;
    private final Map<String, Set<String>> unitIdToSetOfRuleId;
    private final Map<String, StringWriter> mngtMdRuleIdToRulesXml;

    private static final List<String> REQUIRED_GLOBAL_INFORMATIONS = initGlobalRequiredInformations();
    private static final String MISSING_REQUIRED_GLOBAL_INFORMATIONS =
        "Global required informations are not found after extracting the manifest.xml";

    /**
     * Constructor with parameter SedaUtilsFactory
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
        mngtMdRuleIdToRulesXml = new HashMap<>();
        unitIdToSetOfRuleId = new HashMap<>();
        existingUnitGuids = new HashSet<>();
    }

    /**
     * @return HANDLER_ID
     */
    public static final String getId() {
        return HANDLER_ID;
    }

    private static final List<String> initGlobalRequiredInformations() {
        List<String> globalRequiredInfos = new ArrayList<>();
        globalRequiredInfos.add(SedaConstants.TAG_ORIGINATINGAGENCYIDENTIFIER);

        return globalRequiredInfos;
    }


    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO ioParam) {
        checkMandatoryParameters(params);
        handlerIO = ioParam;
        final ItemStatus globalCompositeItemStatus = new ItemStatus(HANDLER_ID);

        try {
            checkMandatoryIOParameter(ioParam);
            globalSedaParametersFile =
                handlerIO.getNewLocalFile(handlerIO.getOutput(GLOBAL_SEDA_PARAMETERS_FILE_IO_RANK).getPath());
            extractSEDA(params, globalCompositeItemStatus);
            globalCompositeItemStatus.increment(StatusCode.OK);

        } catch (final ProcessingDuplicatedVersionException e) {
            LOGGER.debug("ProcessingException: duplicated version", e);
            globalCompositeItemStatus.increment(StatusCode.KO);
        } catch (final ProcessingUnitNotFoundException e) {
            LOGGER.debug("ProcessingException : unit not found", e);
            globalCompositeItemStatus.increment(StatusCode.KO);
        } catch (final ProcessingManifestReferenceException e) {
            LOGGER.debug("ProcessingException : reference incorrect in Manifest", e);
            globalCompositeItemStatus.increment(StatusCode.KO);
        } catch (final MissingFieldException e) {
            LOGGER.debug("MissingFieldException", e);
            globalCompositeItemStatus.increment(StatusCode.KO);
        } catch (final ArchiveUnitContainBinaryDataObjectException e) {
            LOGGER.debug("ProcessingException: archive unit contain an binary data object declared object group.", e);
            globalCompositeItemStatus.increment(StatusCode.KO);
        } catch (final ProcessingException e) {
            LOGGER.debug("ProcessingException", e);
            globalCompositeItemStatus.increment(StatusCode.FATAL);
        } catch (final CycleFoundException e) {
            LOGGER.debug("ProcessingException: cycle found", e);
            globalCompositeItemStatus.increment(StatusCode.KO);
        } finally {
            // Empty all maps
            binaryDataObjectIdToGuid.clear();
            objectGroupIdToGuidTmp.clear();
            unitIdToGuid.clear();
            binaryDataObjectIdWithoutObjectGroupId.clear();
            binaryDataObjectIdToObjectGroupId.clear();
            objectGroupIdToBinaryDataObjectId.clear();
            unitIdToGroupId.clear();
            guidToLifeCycleParameters.clear();
            objectGuidToBinaryObject.clear();
            binaryDataObjectIdToVersionDataObject.clear();
            // Except if they are to be used in MEMORY just after in the same STEP
            // objectGroupIdToGuid
            // objectGroupIdToUnitId
        }

        return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, globalCompositeItemStatus);

    }

    /**
     * Split Element from InputStream and write it to workspace
     *
     * @param params parameters of workspace server
     * @param globalCompositeItemStatus
     * @throws ProcessingException throw when can't read or extract element from SEDA
     * @throws CycleFoundException
     */
    public void extractSEDA(WorkerParameters params, ItemStatus globalCompositeItemStatus)
        throws ProcessingException, CycleFoundException {
        ParameterHelper.checkNullOrEmptyParameters(params);
        final String containerId = params.getContainerName();
        try (LogbookLifeCyclesClient logbookLifeCycleClient =
            LogbookLifeCyclesClientFactory.getInstance().getClient()) {
            extractSEDAWithWorkspaceClient(containerId, globalCompositeItemStatus, logbookLifeCycleClient);
        }
    }

    private void extractSEDAWithWorkspaceClient(String containerId,
        ItemStatus globalCompositeItemStatus,
        LogbookLifeCyclesClient logbookLifeCycleClient)
        throws ProcessingException, CycleFoundException {
        ParametersChecker.checkParameter("ContainerId is a mandatory parameter", containerId);
        ParametersChecker.checkParameter("itemStatus is a mandatory parameter", globalCompositeItemStatus);


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
                xmlFile = handlerIO.getInputStreamFromWorkspace(
                    IngestWorkflowConstants.SEDA_FOLDER + "/" + IngestWorkflowConstants.SEDA_FILE);
            } catch (ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException e) {
                LOGGER.error(MANIFEST_NOT_FOUND);
                throw new ProcessingException(e);
            }
            reader = xmlInputFactory.createXMLEventReader(xmlFile);
            final JsonXMLConfig config =
                new JsonXMLConfigBuilder().autoArray(true).autoPrimitive(true).prettyPrint(true)
                    .namespaceDeclarations(false).build();
            // This file will be a JSON representation of the SEDA manifest with an empty DataObjectPackage structure
            final FileWriter tmpFileWriter =
                new FileWriter(globalSedaParametersFile);
            final XMLEventFactory eventFactory = XMLEventFactory.newInstance();
            final XMLEventWriter writer = new JsonXMLOutputFactory(config).createXMLEventWriter(tmpFileWriter);
            writer.add(eventFactory.createStartDocument());
            boolean globalMetadata = true;
            List<String> globalRequiredInfosFound = new ArrayList<>();

            while (true) {
                final XMLEvent event = reader.nextEvent();

                // extract info for ATR
                // The DataObjectPackage EndElement is tested before the add condition as we need to add a empty
                // DataObjectPackage endElement event
                if (event.isEndElement() && event.asEndElement().getName().getLocalPart().equals(DATAOBJECT_PACKAGE)) {
                    globalMetadata = true;
                }

                if (event.isStartElement() &&
                    event.asStartElement().getName().getLocalPart()
                        .equals(SedaConstants.TAG_ORIGINATINGAGENCYIDENTIFIER)) {
                    final String orgAgId = reader.getElementText();
                    writer.add(eventFactory.createStartElement("", SedaConstants.NAMESPACE_URI,
                        SedaConstants.TAG_ORIGINATINGAGENCYIDENTIFIER));
                    writer.add(eventFactory.createCharacters(orgAgId));
                    writer.add(eventFactory.createEndElement("", SedaConstants.NAMESPACE_URI,
                        SedaConstants.TAG_ORIGINATINGAGENCYIDENTIFIER));
                    globalMetadata = false;

                    globalRequiredInfosFound.add(SedaConstants.TAG_ORIGINATINGAGENCYIDENTIFIER);
                }

                if (event.isStartElement() &&
                    event.asStartElement().getName().getLocalPart()
                        .equals(SedaConstants.TAG_SUBMISSIONAGENCYIDENTIFIER)) {
                    final String orgAgId = reader.getElementText();
                    writer.add(eventFactory.createStartElement("", SedaConstants.NAMESPACE_URI,
                        SedaConstants.TAG_SUBMISSIONAGENCYIDENTIFIER));
                    writer.add(eventFactory.createCharacters(orgAgId));
                    writer.add(eventFactory.createEndElement("", SedaConstants.NAMESPACE_URI,
                        SedaConstants.TAG_SUBMISSIONAGENCYIDENTIFIER));
                    globalMetadata = false;
                }
                // Process rules : build mgtRulesMap
                if (event.isStartElement() &&
                    SedaConstants.getSupportedRules().contains(event.asStartElement().getName().getLocalPart())) {
                    final StartElement element = event.asStartElement();
                    parseMetadataManagementRules(reader, element, event.asStartElement().getName().getLocalPart());
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
                        writeArchiveUnitToTmpDir(containerId, reader, element, archiveUnitTree,
                            logbookLifeCycleClient);
                    } else if (element.getName().equals(dataObjectName)) {
                        final String objectGroupGuid =
                            writeBinaryDataObjectInLocal(reader, element, containerId, logbookLifeCycleClient);
                        if (guidToLifeCycleParameters.get(objectGroupGuid) != null) {
                            handlerIO.getHelper()
                                .updateDelegate((LogbookLifeCycleObjectGroupParameters) guidToLifeCycleParameters
                                    .get(objectGroupGuid).setBeginningLog(HANDLER_ID, null, null));

                            // Add creation sub task event
                            handlerIO.getHelper()
                                .updateDelegate((LogbookLifeCycleObjectGroupParameters) guidToLifeCycleParameters
                                    .get(objectGroupGuid).setFinalStatus(LFC_CREATION_SUB_TASK_FULL_ID,
                                        null,
                                        StatusCode.OK,
                                        null));

                            handlerIO.getHelper()
                                .updateDelegate((LogbookLifeCycleObjectGroupParameters) guidToLifeCycleParameters
                                    .get(objectGroupGuid).setFinalStatus(HANDLER_ID, null,
                                        StatusCode.OK,
                                        null));
                            logbookLifeCycleClient.bulkCreateObjectGroup(containerId,
                                handlerIO.getHelper().removeCreateDelegate(objectGroupGuid));
                        }
                    }
                }
                if (event.isEndDocument()) {
                    break;
                }
            }
            writer.add(eventFactory.createEndDocument());
            writer.close();

            // 1- Check if required informations exist
            for (String currentInfo : REQUIRED_GLOBAL_INFORMATIONS) {
                if (!globalRequiredInfosFound.contains(currentInfo)) {
                    throw new MissingFieldException(MISSING_REQUIRED_GLOBAL_INFORMATIONS);
                }
            }

            // 2-detect cycle : if graph has a cycle throw CycleFoundException
            // Define Treatment DirectedCycle detection
            final DirectedCycle directedCycle = new DirectedCycle(new DirectedGraph(archiveUnitTree));
            if (directedCycle.isCyclic()) {
                throw new CycleFoundException(GRAPH_CYCLE_MSG);
            }


            // 2- create graph and create level
            // Define Treatment Graph and Level Creation
            createIngestLevelStackFile(new Graph(archiveUnitTree).getGraphWithLongestPaths(),
                GRAPH_WITH_LONGEST_PATH_IO_RANK);

            checkArchiveUnitIdReference();
            saveObjectGroupsToWorkspace(containerId, logbookLifeCycleClient);

            // Add parents to archive units and save them into workspace
            finalizeAndSaveArchiveUnitToWorkspace(archiveUnitTree, containerId,
                IngestWorkflowConstants.ARCHIVE_UNIT_FOLDER, globalCompositeItemStatus, logbookLifeCycleClient);


            // Save binaryDataObjectIdToGuid Map
            HandlerUtils.saveMap(handlerIO, binaryDataObjectIdToGuid, BDO_ID_TO_GUID_IO_RANK, true);

            // Save objectGroupIdToUnitId Map
            handlerIO.addOuputResult(OG_ID_TO_UNID_ID_IO_RANK, objectGroupIdToUnitId);
            // Save binaryDataObjectIdToVersionDataObject Map
            HandlerUtils.saveMap(handlerIO, binaryDataObjectIdToVersionDataObject, BDO_ID_TO_VERSION_DO_IO_RANK, true);

            // Save unitIdToGuid Map
            HandlerUtils.saveMap(handlerIO, unitIdToGuid, UNIT_ID_TO_GUID_IO_RANK, true);

            handlerIO.addOuputResult(GLOBAL_SEDA_PARAMETERS_FILE_IO_RANK, globalSedaParametersFile, false);

        } catch (final XMLStreamException e) {
            LOGGER.error(CANNOT_READ_SEDA, e);
            throw new ProcessingException(e);
        } catch (final LogbookClientBadRequestException e) {
            LOGGER.error(LOGBOOK_LF_BAD_REQUEST_EXCEPTION_MSG, e);
            throw new ProcessingException(e);
        } catch (final LogbookClientNotFoundException e) {
            LOGGER.error(LOGBOOK_LF_RESOURCE_NOT_FOUND_EXCEPTION_MSG, e);
            throw new ProcessingException(e);
        } catch (final LogbookClientAlreadyExistsException e) {
            LOGGER.error(LOGBOOK_LF_OBJECT_EXISTS_EXCEPTION_MSG, e);
            throw new ProcessingException(e);
        } catch (final LogbookClientServerException e) {
            LOGGER.error(LOGBOOK_SERVER_INTERNAL_EXCEPTION_MSG, e);
            throw new ProcessingException(e);
        } catch (final CycleFoundException e) {
            LOGGER.error(CYCLE_FOUND_EXCEPTION, e);
            throw new CycleFoundException(e);
        } catch (final IOException e) {
            LOGGER.error(SAVE_ARCHIVE_ID_TO_GUID_IOEXCEPTION_MSG, e);
            throw new ProcessingException(e);
        } catch (final ArchiveUnitContainBinaryDataObjectException e) {
            LOGGER.error("Archive Unit Reference to BDO", e);
            throw e;
        } finally {
            StreamUtils.closeSilently(xmlFile);
            if (reader != null) {
                try {
                    reader.close();
                } catch (final XMLStreamException e) {
                    SysErrLogger.FAKE_LOGGER.ignoreLog(e);
                }
            }
        }
    }

    private void parseMetadataManagementRules(XMLEventReader reader, StartElement element, String currentRuleInProcess)
        throws ProcessingException {
        try {
            final StringWriter stringWriterRule = new StringWriter();
            final XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
            final XMLEventWriter xw =
                xmlOutputFactory.createXMLEventWriter(stringWriterRule);
            final XMLEventFactory eventFactory = XMLEventFactory.newInstance();
            String currentRuleId = null;

            // Add start element
            xw.add(eventFactory.createStartElement("", "", GLOBAL_MGT_RULE_TAG));
            xw.add(element);
            while (true) {
                XMLEvent event = reader.nextEvent();

                if (event.isEndElement() &&
                    currentRuleInProcess.equalsIgnoreCase(((EndElement) event).getName().getLocalPart())) {
                    xw.add(event);

                    // Add to map
                    mngtMdRuleIdToRulesXml.put(currentRuleId, stringWriterRule);
                    stringWriterRule.close();
                    break;
                }

                if (event.isStartElement() &&
                    SedaConstants.TAG_RULE_RULE.equals(event.asStartElement().getName().getLocalPart())) {
                    xw.add(event);
                    event = (XMLEvent) reader.next();
                    xw.add(event);
                    if (event.isCharacters()) {
                        currentRuleId = event.asCharacters().getData();
                    }
                    event = (XMLEvent) reader.next();
                    xw.add(event);
                    continue;
                }

                xw.add(event);
            }
            xw.add(eventFactory.createEndDocument());
        } catch (XMLStreamException | IOException e) {
            LOGGER.error(CANNOT_READ_SEDA, e);
            throw new ProcessingException(e);
        }
    }

    private void finalizeAndSaveArchiveUnitToWorkspace(ObjectNode archiveUnitTree,
        String containerId, String path, ItemStatus itemStatus, LogbookLifeCyclesClient logbookLifeCycleClient)
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
            boolean isRootArchive = true;
            boolean mgtRulesAdded = false;

            // 1- Update created Unit life cycles
            if (guidToLifeCycleParameters.get(unitGuid) != null) {
                final LogbookLifeCycleParameters llcp = guidToLifeCycleParameters.get(unitGuid);
                llcp.setBeginningLog(HANDLER_ID, null, null);
                handlerIO.getHelper().updateDelegate(llcp);
                // TODO : add else case
                if (!existingUnitGuids.contains(unitGuid)) {
                    llcp.setFinalStatus(LFC_CREATION_SUB_TASK_FULL_ID, null, StatusCode.OK,
                        null);
                    handlerIO.getHelper().updateDelegate(llcp);
                }
                llcp.setFinalStatus(HANDLER_ID, null, StatusCode.OK,
                    null);
                handlerIO.getHelper().updateDelegate(llcp);
                logbookLifeCycleClient.bulkUpdateUnit(containerId,
                    handlerIO.getHelper().removeUpdateDelegate(unitGuid));
            }

            // 2- Update temporary files
            final File unitTmpFileForRead = handlerIO.getNewLocalFile(ARCHIVE_UNIT_TMP_FILE_PREFIX + unitGuid);
            final File unitCompleteTmpFile = handlerIO.getNewLocalFile(unitGuid);
            final XMLEventWriter writer = xmlOutputFactory.createXMLEventWriter(new FileWriter(unitCompleteTmpFile));
            final XMLEventReader reader = xmlInputFactory.createXMLEventReader(new FileReader(unitTmpFileForRead));

            // Add root tag
            writer.add(eventFactory.createStartDocument());
            writer.add(eventFactory.createStartElement("", "", IngestWorkflowConstants.ROOT_TAG));
            boolean startCopy = false;
            // management rules id to add
            Set<String> globalMgtIdExtra = null;
            while (true) {
                final XMLEvent event = reader.nextEvent();
                if (event.isStartElement() && ARCHIVE_UNIT.equals(event.asStartElement().getName().getLocalPart())) {
                    startCopy = true;
                }

                if (startCopy) {
                    if (event.isStartElement() &&
                        ARCHIVE_UNIT.equals(event.asStartElement().getName().getLocalPart())) {

                        // add start work tag
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
                                    isRootArchive = false;
                                }
                            }
                        }
                        writer.add(eventFactory.createEndElement("", "", IngestWorkflowConstants.UP_FIELD));

                        String listRulesForCurrentUnit = "";
                        if (unitIdToSetOfRuleId != null && unitIdToSetOfRuleId.containsKey(unitId)) {
                            listRulesForCurrentUnit = getListOfRulesFormater(unitIdToSetOfRuleId.get(unitId));
                        }
                        String listRulesForAuRoot = "";
                        if (isRootArchive) {
                            if (mngtMdRuleIdToRulesXml != null && !mngtMdRuleIdToRulesXml.isEmpty()) {
                                globalMgtIdExtra = mngtMdRuleIdToRulesXml.keySet();
                            }
                            // All MngtRuleMetadata Must be shown in RootArchive
                            if (globalMgtIdExtra != null && !globalMgtIdExtra.isEmpty()) {
                                listRulesForAuRoot = getListOfRulesFormater(globalMgtIdExtra);
                            }
                        }

                        final StringBuilder rules = new StringBuilder();
                        if (!Strings.isNullOrEmpty(listRulesForCurrentUnit)) {
                            rules.append(listRulesForCurrentUnit);
                        }
                        if (!Strings.isNullOrEmpty(listRulesForAuRoot)) {
                            rules.append(listRulesForAuRoot);
                        }

                        if (!StringUtils.isBlank(rules)) {
                            writer.add(eventFactory.createStartElement("", "", IngestWorkflowConstants.RULES));
                            writer.add(eventFactory.createCharacters(rules.toString()));
                            writer.add(eventFactory.createEndElement("", "", IngestWorkflowConstants.RULES));
                        }

                        if (existingUnitGuids.contains(unitGuid)) {
                            writer.add(eventFactory.createStartElement("", "", IngestWorkflowConstants.EXISTING_TAG));
                            writer.add(eventFactory.createCharacters("true"));
                            writer.add(eventFactory.createEndElement("", "", IngestWorkflowConstants.EXISTING_TAG));
                        }

                        // add existing tag
                        writer.add(eventFactory.createEndElement("", "", IngestWorkflowConstants.WORK_TAG));
                    } else if (event.isEndElement() &&
                        (SedaConstants.TAG_MANAGEMENT.equals(((EndElement) event).getName().getLocalPart()) ||
                            ARCHIVE_UNIT.equals(event.asEndElement().getName().getLocalPart()))) {
                        if (SedaConstants.TAG_MANAGEMENT.equals(((EndElement) event).getName().getLocalPart())) {
                            mgtRulesAdded = true;
                        }

                        if (ARCHIVE_UNIT.equals(event.asEndElement().getName().getLocalPart()) && !mgtRulesAdded &&
                            (isRootArchive && globalMgtIdExtra != null)) {
                            writer.add(eventFactory.createStartElement("", "", SedaConstants.TAG_MANAGEMENT));
                        }
                        if (isRootArchive && globalMgtIdExtra != null) {
                            final XMLInputFactory inputFactory = XMLInputFactory.newInstance();

                            // Add inherited rules from Management bloc
                            for (final String id : globalMgtIdExtra) {
                                final StringWriter stringWriter = mngtMdRuleIdToRulesXml.get(id);
                                final StringReader stringReader = new StringReader(stringWriter.toString());
                                final XMLEventReader xmlEventReaderRule =
                                    inputFactory.createXMLEventReader(stringReader);
                                boolean startCopyRule = false;
                                while (true) {
                                    final XMLEvent eventRule = xmlEventReaderRule.nextEvent();
                                    if (eventRule.isStartElement() &&
                                        GLOBAL_MGT_RULE_TAG
                                            .equals(eventRule.asStartElement().getName().getLocalPart())) {
                                        startCopyRule = true;
                                        continue;
                                    }

                                    if (eventRule.isEndElement() &&
                                        GLOBAL_MGT_RULE_TAG.equals(eventRule.asEndElement().getName().getLocalPart())) {
                                        break;
                                    }
                                    if (startCopyRule) {
                                        writer.add(eventRule);
                                    }
                                }
                            }
                        }

                        if (ARCHIVE_UNIT.equals(event.asEndElement().getName().getLocalPart()) && !mgtRulesAdded &&
                            (isRootArchive && globalMgtIdExtra != null)) {
                            writer.add(eventFactory.createEndElement("", "", SedaConstants.TAG_MANAGEMENT));
                        }

                        if (SedaConstants.TAG_MANAGEMENT.equals(((EndElement) event).getName().getLocalPart())) {
                            writer.add(event);
                            continue;
                        }
                    }
                    writer.add(event);
                }

                if (event.isEndElement() && ARCHIVE_UNIT.equals(event.asEndElement().getName().getLocalPart())) {
                    break;
                }
            }
            writer.add(eventFactory.createEndElement("", "", IngestWorkflowConstants.ROOT_TAG));
            writer.close();
            reader.close();

            // Write to workspace
            try {
                handlerIO.transferFileToWorkspace(path + "/" + unitGuid + XML_EXTENSION, unitCompleteTmpFile, true);
            } finally {
                if (!unitTmpFileForRead.delete()) {
                    LOGGER.warn(FILE_COULD_NOT_BE_DELETED_MSG);
                }
            }
        }
    }

    private String getListOfRulesFormater(Set<String> rulesId) {
        final StringBuilder sbRules = new StringBuilder();
        if (rulesId != null) {
            for (final String ruleId : rulesId) {
                sbRules.append(ruleId).append(SedaConstants.RULE_SEPARATOR);
            }
        }
        return sbRules.toString();
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

    private void writeArchiveUnitToTmpDir(String containerId, XMLEventReader reader,
        StartElement startElement, ObjectNode archiveUnitTree, LogbookLifeCyclesClient logbookLifeCycleClient)
        throws ProcessingException {

        try {

            // Get ArchiveUnit Id
            final String archiveUnitId = startElement.getAttributeByName(new QName(ARCHIVE_UNIT_ELEMENT_ID_ATTRIBUTE))
                .getValue();

            final List<String> createdGuids = extractArchiveUnitToLocalFile(reader, startElement,
                archiveUnitId, archiveUnitTree, logbookLifeCycleClient);

            if (createdGuids != null && !createdGuids.isEmpty()) {
                for (final String currentGuid : createdGuids) {
                    // Create Archive Unit LifeCycle
                    if (!existingUnitGuids.contains(currentGuid)) {
                        createUnitLifeCycle(currentGuid, containerId, logbookLifeCycleClient);
                    } else {
                        updateUnitLifeCycle(currentGuid, containerId);
                    }
                }
            }
        } catch (final ProcessingManifestReferenceException e) {
            LOGGER.error("Reference issue within the SEDA", e);
            throw e;
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
                        //
                        if (!groupId.equals(entry.getValue())) {
                            throw new ArchiveUnitContainBinaryDataObjectException(
                                "The archive unit " + entry.getKey() + " references one BDO Id " + entry.getValue() +
                                    " while this BDO has a GOT id " + groupId);
                        }
                    }
                }
            }
        }
    }

    private String writeBinaryDataObjectInLocal(XMLEventReader reader, StartElement startElement, String containerId,
        LogbookLifeCyclesClient logbookLifeCycleClient)
        throws ProcessingException {
        final String elementGuid = GUIDFactory.newGUID().toString();
        final File tmpFile = handlerIO.getNewLocalFile(elementGuid + JSON_EXTENSION);
        final XMLEventFactory eventFactory = XMLEventFactory.newInstance();
        final JsonXMLConfig config = new JsonXMLConfigBuilder().autoArray(true).autoPrimitive(true).prettyPrint(true)
            .namespaceDeclarations(false).build();
        String groupGuid = null;
        try {
            final FileWriter tmpFileWriter = new FileWriter(tmpFile);

            final XMLEventWriter writer = new JsonXMLOutputFactory(config).createXMLEventWriter(tmpFileWriter);

            final Iterator<?> it = startElement.getAttributes();
            String binaryObjectId = "";
            final BinaryObjectInfo bo = new BinaryObjectInfo();

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
                            groupGuid = GUIDFactory.newObjectGroupGUID(ParameterHelper.getTenantParameter())
                                .toString();
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
                            createObjectGroupLifeCycle(groupGuid, containerId, logbookLifeCycleClient);
                            if (objectGroupIdToBinaryDataObjectId.get(groupId) == null) {
                                final List<String> binaryOjectList = new ArrayList<>();
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
                            // The DataObjectGroupReferenceID is after
                            // DataObjectGroupID in the XML flow
                            if (objectGroupIdToBinaryDataObjectId.get(groupId) != null) {
                                objectGroupIdToBinaryDataObjectId.get(groupId).add(binaryObjectId);
                                groupGuidTmp = objectGroupIdToGuid.get(groupId);
                            } else {
                                // The DataObjectGroupReferenceID is before DataObjectGroupID in the XML flow
                                final List<String> binaryOjectList = new ArrayList<>();
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
                                final String al = ((Attribute) it1.next()).getValue();
                                final DigestType d = DigestType.fromValue(al);
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
        final File tmpJsonFile = handlerIO.getNewLocalFile(jsonFileName);
        final JsonNode jsonBDO = JsonHandler.getFromFile(tmpJsonFile);
        JsonNode objectNode = mapNewTechnicalDataObjectGroupToBDO(jsonBDO, binaryDataOjectId);
        objectNode = addExtraField(objectNode);
        // No check on objectNode BINARY_DATA_OBJECT node, cannot be null or empty
        binaryDataObjectIdToVersionDataObject.put(binaryDataOjectId,
            objectNode.get(BINARY_DATA_OBJECT).get(SedaConstants.TAG_DO_VERSION).textValue());
        JsonHandler.writeAsFile(objectNode,
            handlerIO.getNewLocalFile(jsonFileName)); // write the new BinaryDataObject
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

        final String technicalGotGuid =
            GUIDFactory.newObjectGroupGUID(ParameterHelper.getTenantParameter()).toString();
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
            listBDO = new ArrayList<>();
            listBDO.add(binaryDataOjectId);
            objectGroupIdToBinaryDataObjectId.put(technicalGotGuid, listBDO);
        }

        return jsonBDO;
    }

    private void createObjectGroupLifeCycle(String groupGuid, String containerId,
        LogbookLifeCyclesClient logbookLifeCycleClient)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException {
        final LogbookLifeCycleObjectGroupParameters logbookLifecycleObjectGroupParameters =
            (LogbookLifeCycleObjectGroupParameters) initLogbookLifeCycleParameters(
                groupGuid, false, true);
        logbookLifecycleObjectGroupParameters.setBeginningLog(LFC_INITIAL_CREATION_EVENT_TYPE, null, null);

        logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.eventIdentifierProcess,
            containerId);
        logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.eventIdentifier,
            GUIDFactory.newEventGUID(ParameterHelper.getTenantParameter()).toString());
        logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.eventTypeProcess,
            LogbookTypeProcess.INGEST.name());

        handlerIO.getHelper().createDelegate(logbookLifecycleObjectGroupParameters);

        // Update guidToLifeCycleParameters
        guidToLifeCycleParameters.put(groupGuid, logbookLifecycleObjectGroupParameters);
    }

    /**
     * create level stack on Json file
     *
     * @param levelStackMap
     * @param rank
     * @throws ProcessingException
     */
    private void createIngestLevelStackFile(Map<Integer, Set<String>> levelStackMap, int rank)
        throws ProcessingException {
        LOGGER.debug("Begin createIngestLevelStackFile/containerId: ", handlerIO.getContainerName());
        ParametersChecker.checkParameter("levelStackMap is a mandatory parameter", levelStackMap);
        ParametersChecker.checkParameter("unitIdToGuid is a mandatory parameter", unitIdToGuid);

        File tempFile = null;
        try {
            tempFile = handlerIO.getNewLocalFile(handlerIO.getOutput(rank).getPath());
            // create level json object node
            final ObjectNode ingestLevelStack = JsonHandler.createObjectNode();
            for (final Entry<Integer, Set<String>> entry : levelStackMap.entrySet()) {
                final ArrayNode unitList = ingestLevelStack.withArray(LEVEL + entry.getKey());
                final Set<String> unitGuidList = entry.getValue();
                for (final String idXml : unitGuidList) {

                    final String unitGuid = unitIdToGuid.get(idXml);
                    if (unitGuid == null) {
                        throw new IllegalArgumentException("Unit guid not found in map");
                    }
                    unitList.add(unitGuid);
                }
                ingestLevelStack.set(LEVEL + entry.getKey(), unitList);
            }
            LOGGER.debug("IngestLevelStack: {}", ingestLevelStack);
            // create json file
            JsonHandler.writeAsFile(ingestLevelStack, tempFile);
            // put file in workspace
            handlerIO.addOuputResult(rank, tempFile, true);
        } catch (final IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error(e);
            throw new ProcessingException(e);
        }
        LOGGER.info("End createIngestLevelStackFile/containerId:" + handlerIO.getContainerName());

    }

    /**
     * Get the object group id defined in binary data object or the binary data object without GO. In this map the new
     * technical object is created
     *
     * @param objIdRefByUnit il s'agit du DataObjectGroupReferenceId
     * @return
     */
    private String getNewGdoIdFromGdoByUnit(String objIdRefByUnit) throws ProcessingManifestReferenceException {

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
            throw new ProcessingManifestReferenceException(
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

    private void createUnitLifeCycle(String unitGuid, String containerId,
        LogbookLifeCyclesClient logbookLifeCycleClient)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException {
        final LogbookLifeCycleUnitParameters logbookLifecycleUnitParameters =
            (LogbookLifeCycleUnitParameters) initLogbookLifeCycleParameters(
                unitGuid, true, false);

        logbookLifecycleUnitParameters.setBeginningLog(LFC_INITIAL_CREATION_EVENT_TYPE, null, null);

        logbookLifecycleUnitParameters.putParameterValue(LogbookParameterName.eventIdentifierProcess, containerId);
        logbookLifecycleUnitParameters.putParameterValue(LogbookParameterName.eventIdentifier,
            GUIDFactory.newEventGUID(ParameterHelper.getTenantParameter()).toString());
        logbookLifecycleUnitParameters.putParameterValue(LogbookParameterName.eventTypeProcess,
            LogbookTypeProcess.INGEST.name());

        logbookLifeCycleClient.create(logbookLifecycleUnitParameters);

        // Update guidToLifeCycleParameters
        guidToLifeCycleParameters.put(unitGuid, logbookLifecycleUnitParameters);
    }

    private void updateUnitLifeCycle(String unitGuid, String containerId)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException {
        final LogbookLifeCycleUnitParameters logbookLifecycleUnitParameters =
            (LogbookLifeCycleUnitParameters) initLogbookLifeCycleParameters(
                unitGuid, true, false);

        // TODO : add update message
        // logbookLifecycleUnitParameters.setBeginningLog(LFC_INITIAL_CREATION_EVENT_TYPE, null, null);

        logbookLifecycleUnitParameters.putParameterValue(LogbookParameterName.eventIdentifierProcess, containerId);
        logbookLifecycleUnitParameters.putParameterValue(LogbookParameterName.eventIdentifier,
            GUIDFactory.newEventGUID(0).toString());
        logbookLifecycleUnitParameters.putParameterValue(LogbookParameterName.eventTypeProcess,
            LogbookTypeProcess.INGEST.name());

        // Update guidToLifeCycleParameters
        guidToLifeCycleParameters.put(unitGuid, logbookLifecycleUnitParameters);
    }

    private List<String> extractArchiveUnitToLocalFile(XMLEventReader reader, StartElement startElement,
        String archiveUnitId, ObjectNode archiveUnitTree, LogbookLifeCyclesClient logbookLifeCycleClient)
        throws ProcessingException {

        final List<String> archiveUnitGuids = new ArrayList<>();

        String existingElementGuid = null;
        String elementGuid = GUIDFactory.newUnitGUID(ParameterHelper.getTenantParameter()).toString();
        boolean isReferencedArchive = false;

        final XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
        final XMLEventFactory eventFactory = XMLEventFactory.newInstance();
        final String elementID = ((Attribute) startElement.getAttributes().next()).getValue();
        final QName name = startElement.getName();
        int stack = 1;
        File tmpFile = handlerIO.getNewLocalFile(ARCHIVE_UNIT_TMP_FILE_PREFIX + elementGuid);
        String groupGuid;
        XMLEventWriter writer;

        final QName unitName = new QName(SedaConstants.NAMESPACE_URI, ARCHIVE_UNIT);
        final QName archiveUnitRefIdTag = new QName(SedaConstants.NAMESPACE_URI, ARCHIVE_UNIT_REF_ID_TAG);
        final QName ruleTag = new QName(SedaConstants.NAMESPACE_URI, SedaConstants.TAG_RULE_RULE);
        final QName systemIdTag = new QName(SedaConstants.NAMESPACE_URI, SedaConstants.TAG_ARCHIVE_SYSTEM_ID);
        final QName updateOperationTag = new QName(SedaConstants.NAMESPACE_URI, SedaConstants.UPDATE_OPERATION);

        // Add new node in archiveUnitNode
        ObjectNode archiveUnitNode = (ObjectNode) archiveUnitTree.get(archiveUnitId);
        if (archiveUnitNode == null) {
            // Create node
            archiveUnitNode = JsonHandler.createObjectNode();
            // or go search for it
        }

        // Add new Archive Unit Entry
        archiveUnitTree.set(archiveUnitId, archiveUnitNode);

        try {
            writer = xmlOutputFactory.createXMLEventWriter(new FileWriter(tmpFile));
            unitIdToGuid.put(elementID, elementGuid);

            // Create new startElement for object with new guid
            writer.add(eventFactory.createStartElement("", SedaConstants.NAMESPACE_URI,
                startElement.getName().getLocalPart()));
            writer.add(eventFactory.createAttribute("id", elementGuid));
            boolean existingUpdateOperation = false;
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
                            final String gotGuid = gotObj.getGotGuid();
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
                        nestedArchiveUnitId, archiveUnitTree, logbookLifeCycleClient));
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

                    // Set isReferencedArchive to true so we can remove this
                    // unit from unitIdToGuid (no lifeCycle for
                    // this unit because it will not be indexed)
                    isReferencedArchive = true;

                } else if (event.isStartElement() && ruleTag.equals(event.asStartElement().getName())) {
                    Set<String> setRuleIds = unitIdToSetOfRuleId.get(elementID);
                    if (setRuleIds == null) {
                        setRuleIds = new HashSet<>();
                    }
                    final String idRule = reader.getElementText();
                    setRuleIds.add(idRule);
                    unitIdToSetOfRuleId.put(elementID, setRuleIds);

                    writer.add(
                        eventFactory.createStartElement("", SedaConstants.NAMESPACE_URI, SedaConstants.TAG_RULE_RULE));
                    writer.add(eventFactory.createCharacters(idRule));
                    writer.add(
                        eventFactory.createEndElement("", SedaConstants.NAMESPACE_URI, SedaConstants.TAG_RULE_RULE));
                } else if (event.isStartElement() && updateOperationTag.equals(event.asStartElement().getName())) {
                    existingUpdateOperation = true;
                    writer.add(event);
                } else if (event.isStartElement() && systemIdTag.equals(event.asStartElement().getName())) {
                    // referencing existing element
                    String elementText = reader.getElementText();
                    existingElementGuid = existingUpdateOperation ? elementText : null;
                    writer.add(
                        eventFactory.createStartElement("", SedaConstants.NAMESPACE_URI,
                            SedaConstants.TAG_ARCHIVE_SYSTEM_ID));
                    writer.add(eventFactory.createCharacters(elementText));
                    writer.add(
                        eventFactory.createEndElement("", SedaConstants.NAMESPACE_URI,
                            SedaConstants.TAG_ARCHIVE_SYSTEM_ID));
                } else {
                    writer.add(event);
                }
            }
            reader.close();
            writer.close();
        } catch (final XMLStreamException e) {
            LOGGER.error("Can not extract Object from SEDA XMLStreamException");
            // delete created temporary file
            tmpFile.delete();
            throw new ProcessingException(e);
        } catch (final IOException e) {
            LOGGER.error("Can not extract Object from SEDA IOException " + elementGuid);
            // delete created temporary file
            tmpFile.delete();
            throw new ProcessingException(e);
        } catch (final ProcessingManifestReferenceException e) {
            LOGGER.error("Can not extract Object from SEDA IOException " + elementGuid);
            // delete created temporary file
            tmpFile.delete();
            throw e;
        } catch (final Exception e) {
            LOGGER.error(e.getMessage());
            // delete created temporary file
            tmpFile.delete();
            throw new ProcessingException(e);
        }

        // If exists unit, replace the created id everywhere and
        if (existingElementGuid != null) {
            tmpFile = addExistingDataArchiveUnitToFile(existingElementGuid, tmpFile, unitName);
            unitIdToGuid.put(elementID, existingElementGuid);
            existingUnitGuids.add(existingElementGuid);
            archiveUnitGuids.remove(elementGuid);
            archiveUnitGuids.add(existingElementGuid);
            elementGuid = existingElementGuid;
        }

        if (isReferencedArchive) {
            // Remove this unit from unitIdToGuid (no lifeCycle for this unit
            // because it will not be indexed)
            unitIdToGuid.remove(elementID);

            // delete created temporary file
            tmpFile.delete();
        } else {
            archiveUnitGuids.add(elementGuid);
        }

        return archiveUnitGuids;
    }

    /**
     * Update unit file with existing archive unit. Will set the existing in the file.
     * 
     * @param elementGuid archive unit guid
     * @param tmpFile unit xml file
     * @param unitName xml unit qualifier
     * @return the modified unit file
     * @throws ProcessingException thrwon when an exception occurred while adding the data
     */
    private File addExistingDataArchiveUnitToFile(String elementGuid, File tmpFile, final QName unitName)
        throws ProcessingException {
        File newTmpFile = handlerIO.getNewLocalFile(ARCHIVE_UNIT_TMP_FILE_PREFIX + elementGuid);
        try {

            JsonNode existingData = loadExistingArchiveUnit(elementGuid);

            if (existingData == null || existingData.get("$results") == null ||
                existingData.get("$results").size() == 0) {
                LOGGER.error("Existing Unit was not found {}", elementGuid);
                throw new ProcessingUnitNotFoundException("Existing Unit was not found");
            }

            final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
            final XMLEventReader xmlOldReader = xmlInputFactory.createXMLEventReader(new FileInputStream(tmpFile));
            final XMLOutputFactory xmlExistingOutputFactory = XMLOutputFactory.newInstance();
            final XMLEventWriter xmlWriter =
                xmlExistingOutputFactory.createXMLEventWriter(new FileWriter(newTmpFile));
            final XMLEventFactory eventFactory = XMLEventFactory.newInstance();

            while (true) {
                final XMLEvent event = xmlOldReader.nextEvent();

                if (event.isEndDocument()) {
                    break;
                }

                if (event.isStartElement() &&
                    unitName.getLocalPart().equals(event.asStartElement().getName().getLocalPart())) {
                    xmlWriter.add(eventFactory.createStartElement("", SedaConstants.NAMESPACE_URI,
                        SedaConstants.TAG_ARCHIVE_UNIT));
                    xmlWriter.add(eventFactory.createAttribute("id", elementGuid));

                } else {
                    xmlWriter.add(event);
                }
            }


            xmlOldReader.close();
            xmlWriter.close();
            tmpFile.delete();
        } catch (XMLStreamException | FactoryConfigurationError | IOException e) {
            LOGGER.error(e.getMessage());
            throw new ProcessingException(e);
        }
        return newTmpFile;
    }

    private void saveObjectGroupsToWorkspace(String containerId,
        LogbookLifeCyclesClient logbookLifeCycleClient) throws ProcessingException {

        completeBinaryObjectToObjectGroupMap();

        // Save maps
        try {
            // Save binaryDataObjectIdToObjectGroupId
            HandlerUtils.saveMap(handlerIO, binaryDataObjectIdToObjectGroupId, BDO_ID_TO_OG_ID_IO_RANK, true);
            // Save objectGroupIdToGuid
            HandlerUtils.saveMap(handlerIO, objectGroupIdToGuid, OG_ID_TO_GUID_IO_RANK, true);
            handlerIO.addOuputResult(OG_ID_TO_GUID_IO_MEMORY_RANK, objectGroupIdToGuid);
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
            final File tmpFile = handlerIO.getNewLocalFile(objectGroupGuid + JSON_EXTENSION);

            try {
                final Map<String, ArrayList<JsonNode>> categoryMap = new HashMap<>();
                objectGroup.put(SedaConstants.PREFIX_ID, objectGroupGuid);
                objectGroup.put(SedaConstants.PREFIX_TENANT_ID, ParameterHelper.getTenantParameter());
                final List<String> versionList = new ArrayList<>();
                for (int index = 0; index < entry.getValue().size(); index++) {
                    final String id = entry.getValue().get(index);
                    final File binaryObjectFile =
                        handlerIO.getNewLocalFile(binaryDataObjectIdToGuid.get(id) + JSON_EXTENSION);
                    final JsonNode binaryNode = JsonHandler.getFromFile(binaryObjectFile).get("BinaryDataObject");
                    String nodeCategory = "";
                    if (binaryNode.get(SedaConstants.TAG_DO_VERSION) != null) {
                        nodeCategory = binaryNode.get(SedaConstants.TAG_DO_VERSION).asText();
                        if (versionList.contains(nodeCategory)) {
                            LOGGER.error(BINARY_DATA_OBJECT_VERSION_MUST_BE_UNIQUE);
                            throw new ProcessingDuplicatedVersionException(BINARY_DATA_OBJECT_VERSION_MUST_BE_UNIQUE);
                        }
                        versionList.add(nodeCategory);
                    }
                    ArrayList<JsonNode> nodeCategoryArray = categoryMap.get(nodeCategory);
                    if (nodeCategory.split("_").length == 1) {
                        final String nodeCategoryNumbered = nodeCategory + "_1";
                        ((ObjectNode) binaryNode).put(SedaConstants.TAG_DO_VERSION, nodeCategoryNumbered);
                    }
                    if (nodeCategoryArray == null) {
                        nodeCategoryArray = new ArrayList<>();
                        nodeCategoryArray.add(binaryNode);
                    } else {
                        final int binaryNodePosition = Integer.parseInt(nodeCategory.split("_")[1]) - 1;
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

                handlerIO.transferFileToWorkspace(
                    IngestWorkflowConstants.OBJECT_GROUP_FOLDER + "/" + objectGroupGuid + JSON_EXTENSION,
                    tmpFile, true);
                // Create unreferenced object group
                if (guidToLifeCycleParameters.get(objectGroupGuid) == null) {
                    createObjectGroupLifeCycle(objectGroupGuid, containerId, logbookLifeCycleClient);

                    // Update Object Group lifeCycle creation event
                    handlerIO.getHelper()
                        .updateDelegate((LogbookLifeCycleObjectGroupParameters) guidToLifeCycleParameters
                            .get(objectGroupGuid).setBeginningLog(HANDLER_ID, null, null));

                    // Add creation sub task event
                    handlerIO.getHelper()
                        .updateDelegate((LogbookLifeCycleObjectGroupParameters) guidToLifeCycleParameters
                            .get(objectGroupGuid).setFinalStatus(LFC_CREATION_SUB_TASK_FULL_ID,
                                null,
                                StatusCode.OK,
                                null));

                    handlerIO.getHelper()
                        .updateDelegate((LogbookLifeCycleObjectGroupParameters) guidToLifeCycleParameters
                            .get(objectGroupGuid).setFinalStatus(HANDLER_ID, null, StatusCode.OK,
                                null));
                    logbookLifeCycleClient.bulkCreateObjectGroup(containerId,
                        handlerIO.getHelper().removeCreateDelegate(objectGroupGuid));
                }

            } catch (final InvalidParseOperationException e) {
                LOGGER.error("Can not parse ObjectGroup", e);
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
                final String guid = binaryDataObjectIdToGuid.get(id);
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


    /**
     * Load data of an existing archive unit by its vitam id.
     * 
     * @param archiveUnitId guid of archive unit
     * @return AU response
     * @throws ProcessingUnitNotFoundException thrown if unit not found
     * @throws ProcessingException thrown if a metadata exception occured
     */
    private JsonNode loadExistingArchiveUnit(String archiveUnitId) throws ProcessingException {

        try (MetaDataClient metadataClient = MetaDataClientFactory.getInstance().getClient()) {
            final SelectParserMultiple selectRequest = new SelectParserMultiple();
            final Select request = selectRequest.getRequest().reset();
            return metadataClient.selectUnitbyId(request.getFinalSelect(), archiveUnitId);

        } catch (final MetaDataException e) {
            LOGGER.error("Internal Server Error", e);
            throw new ProcessingException(e);

        } catch (final InvalidParseOperationException e) {
            LOGGER.error("Existing Unit was not found", e);
            throw new ProcessingUnitNotFoundException("Existing Unit was not found");
        }
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
        if (!handler.checkHandlerIO(HANDLER_IO_OUT_PARAMETER_NUMBER, handlerInitialIOList)) {
            throw new ProcessingException(HandlerIOImpl.NOT_CONFORM_PARAM);
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
