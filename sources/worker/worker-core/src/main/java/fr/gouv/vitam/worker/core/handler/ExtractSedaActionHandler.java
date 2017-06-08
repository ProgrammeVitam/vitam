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

import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS.UNITTYPE;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import de.odysseus.staxon.json.JsonXMLConfig;
import de.odysseus.staxon.json.JsonXMLConfigBuilder;
import de.odysseus.staxon.json.JsonXMLOutputFactory;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.single.Select;
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
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.client.model.IngestContractModel;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.*;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ArchiveUnitContainDataObjectException;
import fr.gouv.vitam.processing.common.exception.ArchiveUnitContainSpecialCharactersException;
import fr.gouv.vitam.processing.common.exception.MissingFieldException;
import fr.gouv.vitam.processing.common.exception.ProcessingDuplicatedVersionException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.exception.ProcessingManifestReferenceException;
import fr.gouv.vitam.processing.common.exception.ProcessingUnitNotFoundException;
import fr.gouv.vitam.processing.common.exception.*;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.common.utils.DataObjectDetail;
import fr.gouv.vitam.worker.common.utils.DataObjectInfo;
import fr.gouv.vitam.worker.common.utils.IngestWorkflowConstants;
import fr.gouv.vitam.worker.common.utils.SedaConstants;
import fr.gouv.vitam.worker.common.utils.SedaUtils;
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
    private static final int DO_ID_TO_OG_ID_IO_RANK = 1;
    private static final int DO_ID_TO_GUID_IO_RANK = 2;
    private static final int OG_ID_TO_GUID_IO_RANK = 3;
    public static final int OG_ID_TO_UNID_ID_IO_RANK = 4;
    private static final int BDO_ID_TO_VERSION_DO_IO_RANK = 5;
    private static final int UNIT_ID_TO_GUID_IO_RANK = 6;
    private static final int GLOBAL_SEDA_PARAMETERS_FILE_IO_RANK = 7;
    public static final int OG_ID_TO_GUID_IO_MEMORY_RANK = 8;
    private static final int HANDLER_IO_OUT_PARAMETER_NUMBER = 9;


    private static final String HANDLER_ID = "CHECK_MANIFEST";
    private static final String LFC_INITIAL_CREATION_EVENT_TYPE = "LFC_CREATION";
    private static final String LFC_CREATION_SUB_TASK_ID = "LFC_CREATION";
    private static final String LFC_CREATION_SUB_TASK_FULL_ID = HANDLER_ID + "." + LFC_CREATION_SUB_TASK_ID;
    private static final String ATTACHMENT_IDS = "attachmentIds";
    private HandlerIO handlerIO;


    private static final String EV_DETAIL_REQ = "EvDetailReq";
    private static final String JSON_EXTENSION = ".json";
    private static final String BINARY_DATA_OBJECT = "BinaryDataObject";
    private static final String PHYSICAL_DATA_OBJECT = "PhysicalDataObject";
    private static final String DATA_OBJECT_GROUPID = "DataObjectGroupId";
    private static final String ARCHIVE_UNIT = "ArchiveUnit";
    private static final String BINARY_MASTER = "BinaryMaster";
    private static final String PHYSICAL_MASTER = "PhysicalMaster";
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
    private static final String DATA_OBJECT_VERSION_MUST_BE_UNIQUE = "ERROR: DataObject version must be unique";
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
    private static final String CONTRACT_NAME = "Name";
    private static final String FILING_UNIT = "FILING_UNIT";

    private final Map<String, String> dataObjectIdToGuid;
    private final Map<String, String> objectGroupIdToGuid;
    private final Map<String, String> objectGroupIdToGuidTmp;
    private final Map<String, String> unitIdToGuid;
    private final Set<String> existingUnitGuids;
    private final Set<String> physicalDataObjetsGuids;

    private final Map<String, String> dataObjectIdToObjectGroupId;
    private final Map<String, List<String>> objectGroupIdToDataObjectId;
    private final Map<String, String> unitIdToGroupId;
    // this map contains DataObject that not have DataObjectGroupId
    private final Map<String, GotObj> dataObjectIdWithoutObjectGroupId;
    private final Map<String, List<String>> objectGroupIdToUnitId;
    private final Map<String, DataObjectInfo> objectGuidToDataObject;
    private final Map<String, DataObjectDetail> dataObjectIdToDetailDataObject;
    private final Map<String, LogbookLifeCycleParameters> guidToLifeCycleParameters;

    private final List<Class<?>> handlerInitialIOList = new ArrayList<>();
    private File globalSedaParametersFile;
    private final Map<String, Set<String>> unitIdToSetOfRuleId;
    private final Map<String, StringWriter> mngtMdRuleIdToRulesXml;
    private int nbAUExisting = 0;

    private static final String MISSING_REQUIRED_GLOBAL_INFORMATIONS =
        "Global required informations are not found after extracting the manifest.xml";

    private static String prodService = null;
    private static String contractName = null;
    private static String filingParentId = null;
    private ObjectNode archiveUnitTree;


    private UnitType workflowUnitTYpe = UnitType.INGEST;

    /**
     * Constructor with parameter SedaUtilsFactory
     */
    public ExtractSedaActionHandler() {
        dataObjectIdToGuid = new HashMap<>();
        dataObjectIdWithoutObjectGroupId = new HashMap<>();
        objectGroupIdToGuid = new HashMap<>();
        objectGroupIdToGuidTmp = new HashMap<>();
        unitIdToGuid = new HashMap<>();
        dataObjectIdToObjectGroupId = new HashMap<>();
        objectGroupIdToDataObjectId = new HashMap<>();
        unitIdToGroupId = new HashMap<>();
        objectGroupIdToUnitId = new HashMap<>();
        guidToLifeCycleParameters = new HashMap<>();
        dataObjectIdToDetailDataObject = new HashMap<>();
        objectGuidToDataObject = new HashMap<>();
        mngtMdRuleIdToRulesXml = new HashMap<>();
        unitIdToSetOfRuleId = new HashMap<>();
        existingUnitGuids = new HashSet<>();
        physicalDataObjetsGuids = new HashSet<>();
    }

    /**
     * @return HANDLER_ID
     */
    public static final String getId() {
        return HANDLER_ID;
    }

    /**
     * @return HandlerIO
     */
    public HandlerIO getHandlerIO() {
        return handlerIO;
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
            ObjectNode evDetData = extractSEDA(params, globalCompositeItemStatus);
            globalCompositeItemStatus.getData().put(LogbookParameterName.eventDetailData.name(),
                JsonHandler.unprettyPrint(evDetData));
            globalCompositeItemStatus.increment(StatusCode.OK);
        } catch (final ProcessingDuplicatedVersionException e) {
            LOGGER.debug("ProcessingException: duplicated version", e);
            globalCompositeItemStatus.increment(StatusCode.KO);
        } catch (final ProcessingUnitNotFoundException e) {
            LOGGER.debug("ProcessingException : unit not found", e);
            globalCompositeItemStatus.increment(StatusCode.KO);
        } catch (final ProcessingUnauthorizeException e) {
            LOGGER.debug("ProcessingException : unit not found", e);
            globalCompositeItemStatus.increment(StatusCode.KO);
        } catch (final ProcessingManifestReferenceException e) {
            LOGGER.debug("ProcessingException : reference incorrect in Manifest", e);
            globalCompositeItemStatus.increment(StatusCode.KO);
        } catch (final MissingFieldException e) {
            LOGGER.debug("MissingFieldException", e);
            globalCompositeItemStatus.increment(StatusCode.KO);
        } catch (final ArchiveUnitContainDataObjectException e) {
            LOGGER.debug("ProcessingException: archive unit contain an data object declared object group.", e);
            globalCompositeItemStatus.increment(StatusCode.KO);
        } catch (final ArchiveUnitContainSpecialCharactersException e) {
            LOGGER.debug("ProcessingException: archive unit contains special characters.", e);
            globalCompositeItemStatus.increment(StatusCode.KO);
        } catch (final ProcessingException e) {
            LOGGER.debug("ProcessingException", e);
            globalCompositeItemStatus.increment(StatusCode.FATAL);
        } catch (final CycleFoundException e) {
            LOGGER.debug("ProcessingException: cycle found", e);
            globalCompositeItemStatus.increment(StatusCode.KO);
        } finally {
            // Empty all maps
            dataObjectIdToGuid.clear();
            objectGroupIdToGuidTmp.clear();
            unitIdToGuid.clear();
            dataObjectIdWithoutObjectGroupId.clear();
            dataObjectIdToObjectGroupId.clear();
            objectGroupIdToDataObjectId.clear();
            unitIdToGroupId.clear();
            guidToLifeCycleParameters.clear();
            objectGuidToDataObject.clear();
            dataObjectIdToDetailDataObject.clear();
            // Except if they are to be used in MEMORY just after in the same STEP
            // objectGroupIdToGuid
            // objectGroupIdToUnitId
        }

        if (prodService != null) {
            LOGGER.debug("productor service: " + prodService);
            globalCompositeItemStatus.getData().put(LogbookParameterName.agentIdentifierOriginating.name(),
                prodService);
        }
        return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, globalCompositeItemStatus);

    }

    /**
     * Split Element from InputStream and write it to workspace
     *
     * @param params parameters of workspace server
     * @param globalCompositeItemStatus the global status
     * @throws ProcessingException throw when can't read or extract element from SEDA
     * @throws CycleFoundException when a cycle is found in data extract
     */
    public ObjectNode extractSEDA(WorkerParameters params, ItemStatus globalCompositeItemStatus)
        throws ProcessingException, CycleFoundException {
        ParameterHelper.checkNullOrEmptyParameters(params);
        final String containerId = params.getContainerName();
        try (LogbookLifeCyclesClient logbookLifeCycleClient =
            LogbookLifeCyclesClientFactory.getInstance().getClient()) {
            return extractSEDAWithWorkspaceClient(containerId, globalCompositeItemStatus,
                logbookLifeCycleClient, params.getLogbookTypeProcess());
        }
    }

    private ObjectNode extractSEDAWithWorkspaceClient(String containerId, ItemStatus globalCompositeItemStatus,
        LogbookLifeCyclesClient logbookLifeCycleClient, LogbookTypeProcess typeProcess)
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
        final QName physicalDataObjectName = new QName(SedaConstants.NAMESPACE_URI, PHYSICAL_DATA_OBJECT);
        final QName unitName = new QName(SedaConstants.NAMESPACE_URI, ARCHIVE_UNIT);

        // Archive Unit Tree
        archiveUnitTree = JsonHandler.createObjectNode();

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

            ObjectNode evDetData = JsonHandler.createObjectNode();
            evDetData.put("evDetDataType", LogbookEvDetDataType.MASTER.name());

            while (true) {
                final XMLEvent event = reader.nextEvent();

                // extract info for ATR
                // The DataObjectPackage EndElement is tested before the add condition as we need to add a empty
                // DataObjectPackage endElement event
                if (event.isEndElement() && event.asEndElement().getName().getLocalPart().equals(DATAOBJECT_PACKAGE)) {
                    globalMetadata = true;
                }

                if (event.isStartElement() && event.asStartElement().getName().getLocalPart()
                    .equals(SedaConstants.TAG_ARCHIVAL_AGREEMENT)) {
                    contractName = reader.getElementText();
                    writer.add(eventFactory.createStartElement("", SedaConstants.NAMESPACE_URI,
                        SedaConstants.TAG_ARCHIVAL_AGREEMENT));
                    writer.add(eventFactory.createCharacters(contractName));
                    writer.add(eventFactory.createEndElement("", SedaConstants.NAMESPACE_URI,
                        SedaConstants.TAG_ARCHIVAL_AGREEMENT));
                    continue;
                }

                if (event.isStartElement() && event.asStartElement().getName().getLocalPart()
                    .equals(SedaConstants.TAG_ORIGINATINGAGENCYIDENTIFIER)) {

                    final String orgAgId = reader.getElementText();
                    prodService = orgAgId;

                    // Check if the OriginatingAgency was really set
                    if (orgAgId != null && !orgAgId.isEmpty()) {
                        globalRequiredInfosFound.add(SedaConstants.TAG_ORIGINATINGAGENCYIDENTIFIER);
                    }

                    writer.add(eventFactory.createStartElement("", SedaConstants.NAMESPACE_URI,
                        SedaConstants.TAG_ORIGINATINGAGENCYIDENTIFIER));
                    writer.add(eventFactory.createCharacters(orgAgId));
                    writer.add(eventFactory.createEndElement("", SedaConstants.NAMESPACE_URI,
                        SedaConstants.TAG_ORIGINATINGAGENCYIDENTIFIER));

                    writer.add(
                        eventFactory.createStartElement("", SedaConstants.NAMESPACE_URI, SedaUtils.NB_AU_EXISTING));
                    writer.add(eventFactory.createCharacters(String.valueOf(nbAUExisting)));
                    writer
                        .add(eventFactory.createEndElement("", SedaConstants.NAMESPACE_URI, SedaUtils.NB_AU_EXISTING));

                    globalMetadata = false;
                }

                // Bug #2324 - lets check the serviceLevel value
                if (event.isStartElement() && event.asStartElement().getName().getLocalPart()
                    .equals(SedaConstants.TAG_SERVICE_LEVEL)) {
                    final String serviceLevel = reader.getElementText();
                    writer.add(eventFactory.createStartElement("", SedaConstants.NAMESPACE_URI,
                        SedaConstants.TAG_SERVICE_LEVEL));
                    writer.add(eventFactory.createCharacters(serviceLevel));
                    writer.add(eventFactory.createEndElement("", SedaConstants.NAMESPACE_URI,
                        SedaConstants.TAG_SERVICE_LEVEL));
                    globalMetadata = false;
                }

                if (event.isStartElement() && event.asStartElement().getName().getLocalPart()
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
                            logbookLifeCycleClient, typeProcess);
                    } else if (element.getName().equals(dataObjectName) ||
                        element.getName().equals(physicalDataObjectName)) {
                        final String objectGroupGuid =
                            writeDataObjectInLocal(reader, element, containerId, logbookLifeCycleClient,
                                typeProcess);

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
            saveObjectGroupsToWorkspace(containerId, logbookLifeCycleClient, typeProcess, prodService);

            // Add parents to archive units and save them into workspace
            finalizeAndSaveArchiveUnitToWorkspace(archiveUnitTree, containerId,
                IngestWorkflowConstants.ARCHIVE_UNIT_FOLDER, globalCompositeItemStatus, logbookLifeCycleClient);


            // Save DataObjectIdToGuid Map
            HandlerUtils.saveMap(handlerIO, dataObjectIdToGuid, DO_ID_TO_GUID_IO_RANK, true);

            // Save objectGroupIdToUnitId Map
            handlerIO.addOuputResult(OG_ID_TO_UNID_ID_IO_RANK, objectGroupIdToUnitId);
            // Save dataObjectIdToDetailDataObject Map
            HandlerUtils.saveMap(handlerIO, dataObjectIdToDetailDataObject, BDO_ID_TO_VERSION_DO_IO_RANK, true);

            // Save unitIdToGuid Map
            HandlerUtils.saveMap(handlerIO, unitIdToGuid, UNIT_ID_TO_GUID_IO_RANK, true);

            // Fill evDetData
            try {
                JsonNode metadataAsJson =
                    JsonHandler.getFromFile(globalSedaParametersFile).get(SedaConstants.TAG_ARCHIVE_TRANSFER);

                JsonNode comments = metadataAsJson.get(SedaConstants.TAG_COMMENT);

                if (comments != null && comments.isArray()) {
                    ArrayNode commentsArray = (ArrayNode) comments;
                    for (JsonNode node : commentsArray) {
                        String comment = null, lang = null;
                        if (node.isTextual()) {
                            comment = node.asText();
                        } else {
                            lang = node.get('@' + SedaConstants.TAG_ATTRIBUTE_LANG).asText();
                            comment = node.get("$").asText();
                        }

                        JsonNode oldComment = evDetData.get(EV_DETAIL_REQ);
                        String evDetReq = null;
                        if (oldComment != null) {
                            evDetReq = oldComment.asText();
                        }

                        if (evDetReq == null || "fr".equalsIgnoreCase(lang)) {
                            evDetData.put(EV_DETAIL_REQ, comment);
                        }

                        evDetData.put(EV_DETAIL_REQ + (ParametersChecker.isNotEmpty(lang) ? "_" + lang : ""), comment);
                        LOGGER.debug("evDetData after comment: " + evDetData);
                    }

                } else if (comments != null && comments.isTextual()) {
                    evDetData.put(EV_DETAIL_REQ, comments.asText());
                }

                JsonNode date = metadataAsJson.get(SedaConstants.TAG_DATE);
                if (date != null) {
                    LOGGER.debug("Find a date: " + date);
                    evDetData.put("EvDateTimeReq", date.asText());
                }

                JsonNode archAgreement = metadataAsJson.get(SedaConstants.TAG_ARCHIVAL_AGREEMENT);
                if (archAgreement != null) {
                    LOGGER.debug("Find an archival agreement: " + archAgreement);
                    evDetData.put("ArchivalAgreement", archAgreement.asText());
                }

                JsonNode transfAgency = metadataAsJson.get(SedaConstants.TAG_TRANSFERRING_AGENCY);
                if (transfAgency != null) {
                    JsonNode identifier = transfAgency.get(SedaConstants.TAG_IDENTIFIER);
                    if (identifier != null) {
                        LOGGER.debug("Find a transfAgency: " + transfAgency);
                        evDetData.put("AgIfTrans", identifier.asText());
                    }
                }

                JsonNode dataObjPack = metadataAsJson.get(SedaConstants.TAG_DATA_OBJECT_PACKAGE);
                if (dataObjPack != null) {
                    JsonNode serviceLevel = dataObjPack.get(SedaConstants.TAG_SERVICE_LEVEL);
                    if (serviceLevel != null) {
                        LOGGER.debug("Find a service Level: " + serviceLevel);
                        evDetData.put("ServiceLevel", serviceLevel.asText());
                    } else {
                        LOGGER.debug("Put a null ServiceLevel (No service Level)");
                        evDetData.set("ServiceLevel", (ObjectNode) null);
                    }
                } else {
                    LOGGER.debug("Put a null ServiceLevel (No Data Object Package)");
                    evDetData.set("ServiceLevel", (ObjectNode) null);
                }

                if (existingUnitGuids.size() > 0) {
                    ArrayNode attachmentNode = JsonHandler.createArrayNode();
                    existingUnitGuids.forEach(attachmentNode::add);
                    evDetData.set(ATTACHMENT_IDS, attachmentNode);
                }

            } catch (InvalidParseOperationException e) {
                LOGGER.error("Can't parse globalSedaPareters", e);
                throw new ProcessingException(e);
            }

            handlerIO.addOuputResult(GLOBAL_SEDA_PARAMETERS_FILE_IO_RANK, globalSedaParametersFile, false);

            return evDetData;
        } catch (final XMLStreamException | InvalidParseOperationException e) {
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
        } catch (final ArchiveUnitContainDataObjectException e) {
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
            StringWriter stringWriterRule = new StringWriter();
            final JsonXMLConfig config =
                new JsonXMLConfigBuilder().autoArray(true).autoPrimitive(true).prettyPrint(true)
                    .namespaceDeclarations(false).build();
            final XMLOutputFactory xmlOutputFactory = new JsonXMLOutputFactory(config);
            XMLEventWriter xw =
                xmlOutputFactory.createXMLEventWriter(stringWriterRule);
            final XMLEventFactory eventFactory = XMLEventFactory.newInstance();
            String currentRuleId = null;

            xw.add(eventFactory.createStartDocument());
            // Add start element
            xw.add(eventFactory.createStartElement("", "", GLOBAL_MGT_RULE_TAG));
            xw.add(eventFactory.createStartElement("", "", element.getName().getLocalPart()));
            while (true) {

                XMLEvent event = reader.nextEvent();

                if (event.isEndElement() &&
                    currentRuleInProcess.equalsIgnoreCase(((EndElement) event).getName().getLocalPart())) {
                    xw.add(eventFactory.createEndElement("", "", event.asEndElement().getName().getLocalPart()));
                    // Add to map
                    mngtMdRuleIdToRulesXml.put(currentRuleId, stringWriterRule);
                    stringWriterRule.close();
                    break;
                }

                if (event.isStartElement() &&
                    SedaConstants.TAG_RULE_RULE.equals(event.asStartElement().getName().getLocalPart())) {

                    // A new rule was found => close the current stringWriterRule and add it to map
                    if (currentRuleId != null) {
                        xw.add(eventFactory.createEndElement("", "", GLOBAL_MGT_RULE_TAG));
                        xw.add(eventFactory.createEndDocument());
                        stringWriterRule.close();
                        mngtMdRuleIdToRulesXml.put(currentRuleId, stringWriterRule);

                        // Start a new build of a stringWriterRule
                        stringWriterRule = new StringWriter();
                        xw = xmlOutputFactory.createXMLEventWriter(stringWriterRule);
                        xw.add(eventFactory.createStartDocument());
                        xw.add(eventFactory.createStartElement("", "", GLOBAL_MGT_RULE_TAG));
                        xw.add(eventFactory.createStartElement("", "", element.getName().getLocalPart()));
                    }
                    xw.add(eventFactory.createStartElement("", "", SedaConstants.TAG_RULE_RULE));
                    event = (XMLEvent) reader.next();
                    xw.add(event);
                    if (event.isCharacters()) {
                        currentRuleId = event.asCharacters().getData();
                    }
                    event = (XMLEvent) reader.next();
                    if (event.isStartElement()) {
                        xw.add(
                            eventFactory.createStartElement("", "", event.asStartElement().getName().getLocalPart()));
                    } else if (event.isCharacters()) {
                        xw.add(event.asCharacters());
                    } else if (event.isEndElement()) {
                        xw.add(eventFactory.createEndElement("", "", event.asEndElement().getName().getLocalPart()));
                    }

                    continue;
                }

                if (event.isStartElement()) {
                    xw.add(eventFactory.createStartElement("", "", event.asStartElement().getName().getLocalPart()));
                } else if (event.isCharacters()) {
                    xw.add(event.asCharacters());
                } else if (event.isEndElement()) {
                    xw.add(eventFactory.createEndElement("", "", event.asEndElement().getName().getLocalPart()));
                }
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
        XMLStreamException, IOException, ProcessingException, InvalidParseOperationException {

        // Finalize Archive units extraction process
        if (unitIdToGuid == null) {
            return;
        }

        for (final Entry<String, String> element : unitIdToGuid.entrySet()) {

            final String unitGuid = element.getValue();
            final String unitId = element.getKey();
            boolean isRootArchive = true;

            // 1- Update created Unit life cycles
            addFinalStatusToUnitLifeCycle(unitGuid, unitId, containerId, logbookLifeCycleClient);

            // 2- Update temporary files
            final File unitTmpFileForRead = handlerIO.getNewLocalFile(ARCHIVE_UNIT_TMP_FILE_PREFIX + unitGuid);
            final File unitCompleteTmpFile = handlerIO.getNewLocalFile(unitGuid);

            // Get the archiveUnit
            ObjectNode archiveUnit = (ObjectNode) JsonHandler.getFromFile(unitTmpFileForRead);

            // Management rules id to add
            Set<String> globalMgtIdExtra = new HashSet<>();

            addWorkInformations(archiveUnit, unitId, unitGuid, isRootArchive, archiveUnitTree, globalMgtIdExtra);

            updateManagementAndAppendGlobalMgtRule(archiveUnit, globalMgtIdExtra);

            // sanityChecker
            try {
                SanityChecker.checkJsonAll(archiveUnit);
            } catch (InvalidParseOperationException e) {
                LOGGER.error("Sanity Checker failed for Archive Unit " + unitGuid);
                // delete created temporary file
                throw new ArchiveUnitContainSpecialCharactersException(e);
            } finally {
                if (!unitTmpFileForRead.delete()) {
                    LOGGER.warn(FILE_COULD_NOT_BE_DELETED_MSG);
                }
            }

            // Write to new File
            JsonHandler.writeAsFile(archiveUnit, unitCompleteTmpFile);

            // Write to workspace
            try {
                handlerIO.transferFileToWorkspace(path + "/" + unitGuid + JSON_EXTENSION, unitCompleteTmpFile, true);
            } finally {
                if (!unitTmpFileForRead.delete()) {
                    LOGGER.warn(FILE_COULD_NOT_BE_DELETED_MSG);
                }
            }
        }
    }

    /**
     * Merge global rules to specific archive rules and clean management node
     *
     * @param archiveUnit archiveUnit
     * @param globalMgtIdExtra list of global management rule ids
     * @throws InvalidParseOperationException
     */
    private void updateManagementAndAppendGlobalMgtRule(ObjectNode archiveUnit, Set<String> globalMgtIdExtra)
        throws InvalidParseOperationException {

        ObjectNode archiveUnitNode = (ObjectNode) archiveUnit.get(SedaConstants.TAG_ARCHIVE_UNIT);
        ObjectNode managmentNode;
        if (archiveUnitNode.has(SedaConstants.TAG_MANAGEMENT) &&
            archiveUnitNode.get(SedaConstants.TAG_MANAGEMENT) instanceof ObjectNode) {
            managmentNode = (ObjectNode) archiveUnitNode.get(SedaConstants.TAG_MANAGEMENT);
        } else {
            managmentNode = JsonHandler.createObjectNode();
        }


        for (final String ruleId : globalMgtIdExtra) {

            final StringWriter stringWriter = mngtMdRuleIdToRulesXml.get(ruleId);
            JsonNode stringWriterNode = JsonHandler.getFromString(stringWriter.toString());
            JsonNode generalRuleNode = stringWriterNode.get(GLOBAL_MGT_RULE_TAG);

            Iterator<String> ruleTypes = generalRuleNode.fieldNames();
            while (ruleTypes.hasNext()) {
                String ruleType = ruleTypes.next();
                ObjectNode generalRuleTypeNode = (ObjectNode) generalRuleNode.get(ruleType);
                JsonNode managmentRuleTypeNode;
                if (managmentNode.has(ruleType)) {
                    managmentRuleTypeNode = managmentNode.get(ruleType);
                    if (!managmentNode.isArray()) {
                        if (managmentRuleTypeNode.isArray()) {
                            managmentRuleTypeNode =
                                JsonHandler.createArrayNode().addAll((ArrayNode) managmentRuleTypeNode);
                        } else {
                            managmentRuleTypeNode = JsonHandler.createArrayNode().add(managmentRuleTypeNode);
                        }
                    }
                } else {
                    managmentRuleTypeNode = JsonHandler.createArrayNode();
                }

                if (!checkContainsPreventInheritance((ArrayNode) managmentRuleTypeNode)) {
                    ((ArrayNode) managmentRuleTypeNode).add(generalRuleTypeNode);
                    managmentNode.set(ruleType, managmentRuleTypeNode);
                }
            }
        }

        // Ensures every ruleType node and RefNonRuleId is an array
        for (String supportedRuleType : SedaConstants.getSupportedRules()) {
            if (managmentNode.has(supportedRuleType)) {
                JsonNode managmentRuleTypeNode = managmentNode.get(supportedRuleType);
                if (!managmentRuleTypeNode.isArray()) {
                    managmentRuleTypeNode =
                        JsonHandler.createArrayNode().add(managmentRuleTypeNode);
                    managmentNode.set(supportedRuleType, managmentRuleTypeNode);
                }

                for (int indexRule = 0; indexRule < ((ArrayNode) managmentRuleTypeNode).size(); indexRule++) {
                    JsonNode ruleNode = ((ArrayNode) managmentRuleTypeNode).get(indexRule);
                    if (ruleNode.get(SedaConstants.TAG_RULE_REF_NON_RULE_ID) != null &&
                        !ruleNode.get(SedaConstants.TAG_RULE_REF_NON_RULE_ID).isArray()) {
                        JsonNode refNonRuleIdNode =
                            JsonHandler.createArrayNode().add(ruleNode.get(SedaConstants.TAG_RULE_REF_NON_RULE_ID));
                        ((ObjectNode) ruleNode).set(SedaConstants.TAG_RULE_REF_NON_RULE_ID, refNonRuleIdNode);
                        ((ArrayNode) managmentRuleTypeNode).set(indexRule, ruleNode);

                    }
                }
            }
        }

        archiveUnitNode.set(SedaConstants.TAG_MANAGEMENT, managmentNode);
    }

    private boolean checkContainsPreventInheritance(ArrayNode ruleTypeNode) {
        for (JsonNode ruleNode : ruleTypeNode) {
            if (ruleNode.has(SedaConstants.TAG_RULE_PREVENT_INHERITANCE)) {
                if (ruleNode.get(SedaConstants.TAG_RULE_PREVENT_INHERITANCE) instanceof BooleanNode) {
                    return ruleNode.get(SedaConstants.TAG_RULE_PREVENT_INHERITANCE).asBoolean();
                }
            }
        }
        return false;
    }

    private void addWorkInformations(ObjectNode archiveUnit, String unitId, String unitGuid, boolean isRootArchive,
        ObjectNode archiveUnitTree, Set<String> globalMgtIdExtra) throws XMLStreamException {

        ObjectNode workNode = JsonHandler.createObjectNode();

        // Get parents list
        ArrayNode upNode = JsonHandler.createArrayNode();
        isRootArchive = addParentsToTmpFile(upNode, unitId, archiveUnitTree);
        if (upNode.isEmpty(null)) {
            linkToArchiveUnitDeclaredInTheIngestContract(upNode);
        }
        workNode.set(IngestWorkflowConstants.UP_FIELD, upNode);

        // Determine rules to apply
        ArrayNode rulesNode = JsonHandler.createArrayNode();
        globalMgtIdExtra.addAll(getMgtRulesToApplyByUnit(rulesNode, unitId, isRootArchive));
        workNode.set(IngestWorkflowConstants.RULES, rulesNode);

        // Add existing guid
        if (existingUnitGuids.contains(unitGuid)) {
            workNode.put(IngestWorkflowConstants.EXISTING_TAG, Boolean.TRUE);
        }

        archiveUnit.set(SedaConstants.PREFIX_WORK, workNode);
    }

    private void addFinalStatusToUnitLifeCycle(String unitGuid, String unitId, String containerId,
        LogbookLifeCyclesClient logbookLifeCycleClient)
        throws LogbookClientNotFoundException, LogbookClientBadRequestException, LogbookClientServerException {
        if (guidToLifeCycleParameters.get(unitGuid) != null) {
            final LogbookLifeCycleParameters llcp = guidToLifeCycleParameters.get(unitGuid);
            llcp.setBeginningLog(HANDLER_ID, null, null);
            handlerIO.getHelper().updateDelegate(llcp);
            // TODO : add else case
            if (!existingUnitGuids.contains(unitGuid)) {
                llcp.setFinalStatus(LFC_CREATION_SUB_TASK_FULL_ID, null, StatusCode.OK, null);
                handlerIO.getHelper().updateDelegate(llcp);
            }
            llcp.setFinalStatus(HANDLER_ID, null, StatusCode.OK, null);


            List<String> parentAttachments = existAttachmentUnitAsParentOnTree(unitId);
            if (parentAttachments.size() > 0) {
                ObjectNode evDetData = JsonHandler.createObjectNode();
                ArrayNode arrayNode = JsonHandler.createArrayNode();
                parentAttachments.forEach(arrayNode::add);
                evDetData.set(ATTACHMENT_IDS, arrayNode);
                String wellFormedJson = null;
                try {
                    wellFormedJson = JsonHandler.writeAsString(evDetData);
                } catch (InvalidParseOperationException e) {
                    LOGGER.error("unable to generate evDetData, incomplete journal generation", e);
                }
                llcp.putParameterValue(LogbookParameterName.eventDetailData, wellFormedJson);
            }
            handlerIO.getHelper().updateDelegate(llcp);
            logbookLifeCycleClient.bulkUpdateUnit(containerId,
                handlerIO.getHelper().removeUpdateDelegate(unitGuid));
        }
    }

    private List<String> existAttachmentUnitAsParentOnTree(String unitId) {
        List<String> parents = new ArrayList<>();
        if (archiveUnitTree.has(unitId)) {
            JsonNode archiveNode = archiveUnitTree.get(unitId);
            if (archiveNode.has(IngestWorkflowConstants.UP_FIELD)) {
                final JsonNode archiveUps = archiveNode.get(IngestWorkflowConstants.UP_FIELD);
                if (archiveUps.isArray() && archiveUps.size() > 0) {
                    ArrayNode archiveUpsArray = (ArrayNode) archiveUps;
                    for (JsonNode jsonNode : archiveUpsArray) {
                        String archiveUnitId = jsonNode.textValue();
                        String guid = unitIdToGuid.get(archiveUnitId);

                        if (existingUnitGuids.contains(guid)) {
                            parents.add(guid);
                        }
                    }
                }
            }
        }
        return parents;
    }

    private boolean addParentsToTmpFile(ArrayNode upNode, String unitId, ObjectNode archiveUnitTree)
        throws XMLStreamException {

        boolean isRootArchive = true;
        if (archiveUnitTree.has(unitId)) {
            final JsonNode archiveNode = archiveUnitTree.get(unitId);
            if (archiveNode.has(IngestWorkflowConstants.UP_FIELD)) {
                final JsonNode archiveUps = archiveNode.get(IngestWorkflowConstants.UP_FIELD);
                if (archiveUps.isArray() && archiveUps.size() > 0) {
                    upNode.add(getUnitParents((ArrayNode) archiveUps));
                    isRootArchive = false;
                }
            }
        }

        return isRootArchive;
    }

    private Set<String> getMgtRulesToApplyByUnit(ArrayNode rulesNode, String unitId, boolean isRootArchive)
        throws XMLStreamException {

        String listRulesForCurrentUnit = "";
        if (unitIdToSetOfRuleId != null && unitIdToSetOfRuleId.containsKey(unitId)) {
            listRulesForCurrentUnit = getListOfRulesFormater(unitIdToSetOfRuleId.get(unitId));
        }

        String listRulesForAuRoot = "";
        Set<String> globalMgtIdExtra = new HashSet<>();

        if (isRootArchive) {
            // Add rules from global Management Data (only new
            // ones)
            if (mngtMdRuleIdToRulesXml != null && !mngtMdRuleIdToRulesXml.isEmpty()) {
                globalMgtIdExtra.clear();
                globalMgtIdExtra.addAll(mngtMdRuleIdToRulesXml.keySet());
            }

            if (!globalMgtIdExtra.isEmpty() && unitIdToSetOfRuleId != null && unitIdToSetOfRuleId.get(unitId) != null &&
                !unitIdToSetOfRuleId.get(unitId).isEmpty()) {
                globalMgtIdExtra.removeAll(unitIdToSetOfRuleId.get(unitId));
            }

            if (!globalMgtIdExtra.isEmpty()) {
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

        if (ParametersChecker.isNotEmpty(rules.toString())) {
            rulesNode.add(rules.toString());
        }

        return globalMgtIdExtra;
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
        StartElement startElement, ObjectNode archiveUnitTree, LogbookLifeCyclesClient logbookLifeCycleClient,
        LogbookTypeProcess logbookTypeProcess)
        throws ProcessingException {

        try {

            // Get ArchiveUnit Id
            final String archiveUnitId = startElement.getAttributeByName(new QName(ARCHIVE_UNIT_ELEMENT_ID_ATTRIBUTE))
                .getValue();

            // final List<String> createdGuids = extractArchiveUnitToLocalFileOld(reader, startElement, archiveUnitId,
            // archiveUnitTree, logbookLifeCycleClient);
            final List<String> createdGuids = extractArchiveUnitToLocalFile(reader, startElement, archiveUnitId,
                archiveUnitTree, logbookLifeCycleClient);

            if (createdGuids != null && !createdGuids.isEmpty()) {
                for (final String currentGuid : createdGuids) {
                    // Create Archive Unit LifeCycle
                    if (!existingUnitGuids.contains(currentGuid)) {
                        createUnitLifeCycle(currentGuid, containerId, logbookLifeCycleClient, logbookTypeProcess);
                    } else {
                        updateUnitLifeCycle(currentGuid, containerId, logbookTypeProcess);
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
                    final String groupId = dataObjectIdToObjectGroupId.get(entry.getValue()); // the AU reference
                    // an BDO
                    if (Strings.isNullOrEmpty(groupId)) {
                        throw new ProcessingException("Archive Unit references a BDO Id but is not correct");
                    } else {
                        //
                        if (!groupId.equals(entry.getValue())) {
                            throw new ArchiveUnitContainDataObjectException(
                                "The archive unit " + entry.getKey() + " references one BDO Id " + entry.getValue() +
                                    " while this BDO has a GOT id " + groupId);
                        }
                    }
                }
            }
        }
    }

    private String writeDataObjectInLocal(XMLEventReader reader, StartElement startElement, String containerId,
        LogbookLifeCyclesClient logbookLifeCycleClient, LogbookTypeProcess typeProcess)
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
            String dataObjectId = "";
            final DataObjectInfo bo = new DataObjectInfo();
            final DataObjectDetail detail = new DataObjectDetail();

            if (it.hasNext()) {
                dataObjectId = ((Attribute) it.next()).getValue();
                dataObjectIdToGuid.put(dataObjectId, elementGuid);
                dataObjectIdToObjectGroupId.put(dataObjectId, "");
                writer.add(eventFactory.createStartDocument());
                writer.add(eventFactory.createStartElement("", "", startElement.getName().getLocalPart()));
                writer.add(eventFactory.createStartElement("", "", SedaConstants.PREFIX_ID));
                writer.add(eventFactory.createCharacters(dataObjectId));
                writer.add(eventFactory.createEndElement("", "", SedaConstants.PREFIX_ID));
            }
            while (true) {
                boolean writable = true;
                final XMLEvent event = reader.nextEvent();
                if (event.isEndElement()) {
                    final EndElement end = event.asEndElement();
                    if (BINARY_DATA_OBJECT.equals(end.getName().getLocalPart()) ||
                        PHYSICAL_DATA_OBJECT.equals(end.getName().getLocalPart())) {
                        writer.add(event);
                        writer.add(eventFactory.createEndDocument());
                        objectGuidToDataObject.put(elementGuid, bo);
                        if (PHYSICAL_DATA_OBJECT.equals(end.getName().getLocalPart())) {
                            physicalDataObjetsGuids.add(elementGuid);
                            detail.setPhysical(true);
                        }
                        dataObjectIdToDetailDataObject.put(dataObjectId, detail);
                        break;
                    }

                }

                if (event.isStartElement()) {
                    final String localPart = event.asStartElement().getName().getLocalPart();

                    // extract info for version DBO
                    switch (localPart) {
                        case SedaConstants.TAG_DO_VERSION: {
                            final String version = reader.getElementText();
                            detail.setVersion(version);
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
                            dataObjectIdToObjectGroupId.put(dataObjectId, groupId);
                            objectGroupIdToGuid.put(groupId, groupGuid);

                            // Create OG lifeCycle
                            createObjectGroupLifeCycle(groupGuid, containerId, logbookLifeCycleClient, typeProcess);
                            if (objectGroupIdToDataObjectId.get(groupId) == null) {
                                final List<String> dataOjectList = new ArrayList<>();
                                dataOjectList.add(dataObjectId);
                                objectGroupIdToDataObjectId.put(groupId, dataOjectList);
                            } else {
                                objectGroupIdToDataObjectId.get(groupId).add(dataObjectId);
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
                            dataObjectIdToObjectGroupId.put(dataObjectId, groupId);
                            // The DataObjectGroupReferenceID is after
                            // DataObjectGroupID in the XML flow
                            if (objectGroupIdToDataObjectId.get(groupId) != null) {
                                objectGroupIdToDataObjectId.get(groupId).add(dataObjectId);
                                groupGuidTmp = objectGroupIdToGuid.get(groupId);
                            } else {
                                // The DataObjectGroupReferenceID is before DataObjectGroupID in the XML flow
                                final List<String> dataOjectList = new ArrayList<>();
                                dataOjectList.add(dataObjectId);
                                objectGroupIdToDataObjectId.put(groupId, dataOjectList);
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
                        case "PhysicalDimensions": {
                            extractPhysicalDimensions(reader, writer, eventFactory, event.asStartElement());
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

            if (Strings.isNullOrEmpty(dataObjectIdToObjectGroupId.get(dataObjectId))) {
                // not have object group, must creat an technical object group
                LOGGER.debug("DO {} not have an GDO", dataObjectId);
                dataObjectIdToObjectGroupId.remove(dataObjectId);
                postDataObjectActions(elementGuid + JSON_EXTENSION, dataObjectId);
            }


        } catch (final XMLStreamException e) {
            LOGGER.debug("Can not read input stream");
            throw new ProcessingException(e);
        } catch (final InvalidParseOperationException e) {
            LOGGER.debug("Can not parse data object json");
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
     * Extract all physical dimensions and manage unit attributes : replace dimensions containing a unit attribute by an
     * object with format: <myDimension> <unit>unitValue</unit> <value>dimensionValue</value> </myDimension>
     *
     * @param reader
     * @param writer
     * @param eventFactory
     * @param startElement
     * @throws XMLStreamException
     */
    private void extractPhysicalDimensions(XMLEventReader reader, XMLEventWriter writer, XMLEventFactory eventFactory,
        StartElement startElement) throws XMLStreamException {
        final QName physicalDimensionsName = startElement.getName();
        writer.add(eventFactory.createStartElement("", "", physicalDimensionsName.getLocalPart()));
        while (true) {
            XMLEvent event = reader.nextEvent();
            if (event.isStartElement()) {
                writer.add(eventFactory.createStartElement("", "",
                    event.asStartElement().getName().getLocalPart()));
                Iterator<Attribute> attributes = (Iterator<Attribute>) event.asStartElement().getAttributes();
                while (attributes.hasNext()) {
                    Attribute attribute = attributes.next();
                    if (SedaConstants.TAG_ATTRIBUTE_UNIT.equals(attribute.getName().getLocalPart())) {
                        writer.add(eventFactory.createStartElement("", "", SedaConstants.TAG_ATTRIBUTE_UNIT));
                        writer.add(eventFactory.createCharacters(attribute.getValue()));
                        writer.add(eventFactory.createEndElement("", "", SedaConstants.TAG_ATTRIBUTE_UNIT));
                        event = reader.nextEvent();
                        writer.add(eventFactory.createStartElement("", "", SedaConstants.TAG_VALUE));
                        writer.add(event.asCharacters());
                        writer.add(eventFactory.createEndElement("", "", SedaConstants.TAG_VALUE));
                    }
                }
            } else if (event.isCharacters()) {
                writer.add(event.asCharacters());
            } else if (event.isEndElement()) {
                String endElementName = event.asEndElement().getName().getLocalPart();
                writer.add(eventFactory.createEndElement("", "", event.asEndElement().getName().getLocalPart()));
                if (physicalDimensionsName.getLocalPart().equals(endElementName)) {
                    return;
                }
            } else if (event.isEndDocument()) {
                writer.add(eventFactory.createEndDocument());
            }

        }

    }

    /**
     * Post actions when reading data object in manifest
     *
     * @param jsonFileName
     * @param dataOjectId
     * @return
     * @throws InvalidParseOperationException
     */
    private JsonNode postDataObjectActions(final String jsonFileName, final String dataOjectId)
        throws InvalidParseOperationException {
        final File tmpJsonFile = handlerIO.getNewLocalFile(jsonFileName);
        final JsonNode jsonBDO = JsonHandler.getFromFile(tmpJsonFile);
        JsonNode objectNode = mapNewTechnicalDataObjectGroupToDO(jsonBDO, dataOjectId);
        objectNode = addExtraField(objectNode);
        if (objectNode.get(BINARY_DATA_OBJECT) != null) {
            DataObjectDetail detail = new DataObjectDetail();
            detail.setVersion(objectNode.get(BINARY_DATA_OBJECT).get(SedaConstants.TAG_DO_VERSION).textValue());
            dataObjectIdToDetailDataObject.put(dataOjectId, detail);
        } else if (objectNode.get(PHYSICAL_DATA_OBJECT) != null) {
            DataObjectDetail detail = new DataObjectDetail();
            detail.setVersion(objectNode.get(PHYSICAL_DATA_OBJECT).get(SedaConstants.TAG_DO_VERSION).textValue());
            dataObjectIdToDetailDataObject.put(dataOjectId, detail);
        } else {
            throw new InvalidParseOperationException("Object should have a Binary or a Physical subnode");
        }
        JsonHandler.writeAsFile(objectNode,
            handlerIO.getNewLocalFile(jsonFileName)); // write the new DataObject
        return objectNode;
    }

    /**
     * add fields that is missing in manifest
     *
     * @param objectNode
     * @return
     */
    private JsonNode addExtraField(JsonNode objectNode) throws InvalidParseOperationException {
        if (objectNode.get(BINARY_DATA_OBJECT) != null) {
            final ObjectNode bdoObjNode = (ObjectNode) objectNode.get(BINARY_DATA_OBJECT);
            if (bdoObjNode.get(SedaConstants.TAG_DO_VERSION) == null ||
                Strings.isNullOrEmpty(bdoObjNode.get(SedaConstants.TAG_DO_VERSION).textValue())) {
                bdoObjNode.put(SedaConstants.TAG_DO_VERSION, BINARY_MASTER);
            }
            return JsonHandler.createObjectNode().set(BINARY_DATA_OBJECT, bdoObjNode);
        } else if (objectNode.get(PHYSICAL_DATA_OBJECT) != null) {
            final ObjectNode pdoObjNode = (ObjectNode) objectNode.get(PHYSICAL_DATA_OBJECT);
            if (pdoObjNode.get(SedaConstants.TAG_DO_VERSION) == null ||
                Strings.isNullOrEmpty(pdoObjNode.get(SedaConstants.TAG_DO_VERSION).textValue())) {
                pdoObjNode.put(SedaConstants.TAG_DO_VERSION, PHYSICAL_MASTER);
            }
            return JsonHandler.createObjectNode().set(PHYSICAL_DATA_OBJECT, pdoObjNode);
        }
        throw new InvalidParseOperationException("Data Object is neither Binary nor Physical");
    }

    /**
     * Creation of the technical new object group and update the maps
     *
     * @param jsonDataObject
     * @param dataOjectId
     * @return
     */
    private JsonNode mapNewTechnicalDataObjectGroupToDO(JsonNode jsonDataObject, String dataOjectId) {
        JsonNode dataObject = jsonDataObject.get(BINARY_DATA_OBJECT);
        if (dataObject == null) {
            dataObject = jsonDataObject.get(PHYSICAL_DATA_OBJECT);
        }
        final ObjectNode dataObjectNode = (ObjectNode) dataObject;

        final String technicalGotGuid =
            GUIDFactory.newObjectGroupGUID(ParameterHelper.getTenantParameter()).toString();
        objectGroupIdToGuid.put(technicalGotGuid, technicalGotGuid); // update object group id guid
        dataObjectNode.put(DATA_OBJECT_GROUPID, technicalGotGuid);

        if (Strings.isNullOrEmpty(dataObjectIdToObjectGroupId.get(dataOjectId))) {
            dataObjectIdToObjectGroupId.put(dataOjectId, technicalGotGuid);
        } else {
            LOGGER.warn("unexpected state - dataObjectIdToObjectGroupId contains the GOT and should not");
        }

        final String gotGuid = dataObjectIdWithoutObjectGroupId.get(dataOjectId) != null
            ? dataObjectIdWithoutObjectGroupId.get(dataOjectId).getGotGuid() : "";
        if (Strings.isNullOrEmpty(gotGuid)) {
            final GotObj gotObj = new GotObj(technicalGotGuid, false);
            dataObjectIdWithoutObjectGroupId.put(dataOjectId, gotObj);
            dataObjectIdToObjectGroupId
                .put(dataOjectId, technicalGotGuid); // update the list of bdo in the map
        } else {
            LOGGER.warn("unexpected state - dataObjectIdWithoutObjectGroupId contains the GOT and should not");
        }

        List<String> listDO = objectGroupIdToDataObjectId.get(technicalGotGuid);
        if (listDO != null && !listDO.contains(technicalGotGuid)) {
            listDO.add(dataOjectId);
            objectGroupIdToDataObjectId.put(technicalGotGuid, listDO);
        } else {
            listDO = new ArrayList<>();
            listDO.add(dataOjectId);
            objectGroupIdToDataObjectId.put(technicalGotGuid, listDO);
        }

        return jsonDataObject;
    }

    private void createObjectGroupLifeCycle(String groupGuid, String containerId,
        LogbookLifeCyclesClient logbookLifeCycleClient, LogbookTypeProcess typeProcess)
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
            typeProcess.name());

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
     * Get the object group id defined in data object or the data object without GO. In this map the new technical
     * object is created
     *
     * @param objIdRefByUnit il s'agit du DataObjectGroupReferenceId
     * @return
     */
    private String getNewGdoIdFromGdoByUnit(String objIdRefByUnit) throws ProcessingManifestReferenceException {

        final String gotGuid = dataObjectIdWithoutObjectGroupId.get(objIdRefByUnit) != null
            ? dataObjectIdWithoutObjectGroupId.get(objIdRefByUnit).getGotGuid() : null;

        if (Strings.isNullOrEmpty(dataObjectIdToObjectGroupId.get(objIdRefByUnit)) &&
            !Strings.isNullOrEmpty(gotGuid)) {

            // nominal case of do without go
            LOGGER.debug("The data object id " + objIdRefByUnit +
                ", is defined without the group object id " +
                dataObjectIdWithoutObjectGroupId.get(objIdRefByUnit) +
                ". The technical group object guid is " + gotGuid);

            return gotGuid;

        } else if (!Strings.isNullOrEmpty(dataObjectIdToObjectGroupId.get(objIdRefByUnit))) {
            LOGGER.debug("The data object id " + dataObjectIdWithoutObjectGroupId.get(objIdRefByUnit) +
                " referenced defined with the group object id " + objIdRefByUnit);
            // il y a un DO possédant le GO id
            return dataObjectIdToObjectGroupId.get(objIdRefByUnit);
        } else if (dataObjectIdToObjectGroupId.containsValue(objIdRefByUnit)) {
            // case objIdRefByUnit is an GO
            return objIdRefByUnit;
        } else {
            throw new ProcessingManifestReferenceException(
                "The group id " + objIdRefByUnit +
                    " doesn't reference a data object or go and it not include in data object");
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
        LogbookLifeCyclesClient logbookLifeCycleClient, LogbookTypeProcess logbookTypeProcess)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException {
        final LogbookLifeCycleUnitParameters logbookLifecycleUnitParameters =
            (LogbookLifeCycleUnitParameters) initLogbookLifeCycleParameters(
                unitGuid, true, false);

        logbookLifecycleUnitParameters.setBeginningLog(LFC_INITIAL_CREATION_EVENT_TYPE, null, null);

        logbookLifecycleUnitParameters.putParameterValue(LogbookParameterName.eventIdentifierProcess, containerId);
        logbookLifecycleUnitParameters.putParameterValue(LogbookParameterName.eventIdentifier,
            GUIDFactory.newEventGUID(ParameterHelper.getTenantParameter()).toString());
        logbookLifecycleUnitParameters.putParameterValue(LogbookParameterName.eventTypeProcess,
            logbookTypeProcess.name());

        logbookLifeCycleClient.create(logbookLifecycleUnitParameters);

        // Update guidToLifeCycleParameters
        guidToLifeCycleParameters.put(unitGuid, logbookLifecycleUnitParameters);
    }

    private void updateUnitLifeCycle(String unitGuid, String containerId, LogbookTypeProcess logbookTypeProcess)
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
            logbookTypeProcess.name());

        // Update guidToLifeCycleParameters
        guidToLifeCycleParameters.put(unitGuid, logbookLifecycleUnitParameters);
    }

    private List<String> extractArchiveUnitToLocalFile(XMLEventReader reader, StartElement startElement,
        String archiveUnitId, ObjectNode archiveUnitTree, LogbookLifeCyclesClient logbookLifeCycleClient)
        throws ProcessingException {

        final JsonXMLConfig config =
            new JsonXMLConfigBuilder().autoArray(true).autoPrimitive(true).prettyPrint(true)
                .namespaceDeclarations(false).build();

        final List<String> archiveUnitGuids = new ArrayList<>();

        String existingElementGuid = null;
        String elementGuid = GUIDFactory.newUnitGUID(ParameterHelper.getTenantParameter()).toString();
        boolean isReferencedArchive = false;

        final XMLEventFactory eventFactory = XMLEventFactory.newInstance();
        final String elementID = ((Attribute) startElement.getAttributes().next()).getValue();
        final QName name = startElement.getName();
        int stack = 1;
        File tmpFile = handlerIO.getNewLocalFile(ARCHIVE_UNIT_TMP_FILE_PREFIX + elementGuid);
        String groupGuid;
        XMLEventWriter writerJson;

        final QName unitName = new QName(SedaConstants.NAMESPACE_URI, ARCHIVE_UNIT);

        // Add new node in archiveUnitNode
        ObjectNode archiveUnitNode = (ObjectNode) archiveUnitTree.get(archiveUnitId);
        if (archiveUnitNode == null) {
            // Create node
            archiveUnitNode = JsonHandler.createObjectNode();
            // or go search for it
        }

        // Add new Archive Unit Entry
        archiveUnitTree.set(archiveUnitId, archiveUnitNode);

        // ObjectNode archiveUnitFile = JsonHandler.createObjectNode();

        try {

            writerJson = new JsonXMLOutputFactory(config).createXMLEventWriter(new FileWriter(tmpFile));
            unitIdToGuid.put(elementID, elementGuid);

            // Create new startElement for object with new guid
            writerJson.add(eventFactory.createStartDocument());
            writerJson.add(eventFactory.createStartElement("", "", startElement.getName().getLocalPart()));

            writerJson.add(eventFactory.createStartElement("", "", SedaConstants.PREFIX_ID));
            writerJson.add(eventFactory.createCharacters(elementGuid));
            writerJson.add(eventFactory.createEndElement("", "", SedaConstants.PREFIX_ID));
            String currentRuleType = null;
            boolean ruleInProgress = false;
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

                if (event.isStartElement()) {

                    switch (event.asStartElement().getName().getLocalPart()) {
                        case SedaConstants.TAG_RULE_ACCESS:
                        case SedaConstants.TAG_RULE_REUSE:
                        case SedaConstants.TAG_RULE_STORAGE:
                        case SedaConstants.TAG_RULE_APPRAISAL:
                        case SedaConstants.TAG_RULE_CLASSIFICATION:
                        case SedaConstants.TAG_RULE_DISSEMINATION:
                            writerJson.add(eventFactory.createStartElement("", "",
                                event.asStartElement().getName().getLocalPart()));
                            currentRuleType = event.asStartElement().getName().getLocalPart();
                            ruleInProgress = false;
                            break;
                        case SedaConstants.TAG_RULE_RULE:
                            extractRuleTag(reader, writerJson, eventFactory, elementID, currentRuleType,
                                ruleInProgress);
                            ruleInProgress = true;
                            break;
                        case SedaConstants.TAG_ARCHIVE_UNIT:
                            extractArchiveUnitTag(reader, event, archiveUnitId,
                                archiveUnitTree, archiveUnitGuids, logbookLifeCycleClient);
                            break;
                        case SedaConstants.TAG_DATA_OBJECT_GROUP_REFERENCEID:
                            extractDataObjectGroupReferenceIdTag(reader, writerJson, eventFactory, elementID);
                            break;
                        case SedaConstants.TAG_DATA_OBJECT_REFERENCEID:
                            extractDataObjectReferenceIdTag(reader, writerJson, eventFactory, elementID);
                            break;
                        case ARCHIVE_UNIT_REF_ID_TAG:
                            extractArchiveUnitRefIdTag(reader, archiveUnitId, archiveUnitTree);
                            // Set isReferencedArchive to true so we can remove this
                            // unit from unitIdToGuid (no lifeCycle for
                            // this unit because it will not be indexed)
                            isReferencedArchive = true;
                            break;
                        case SedaConstants.TAG_ARCHIVE_SYSTEM_ID:
                            existingElementGuid = extractSystemIdTag(reader, writerJson, eventFactory,
                                existingUpdateOperation);
                            break;
                        case SedaConstants.UPDATE_OPERATION:
                            existingUpdateOperation = true;
                            writerJson.add(eventFactory.createStartElement("", "",
                                event.asStartElement().getName().getLocalPart()));
                            break;
                        default:
                            writerJson.add(eventFactory.createStartElement("", "",
                                event.asStartElement().getName().getLocalPart()));
                            // FIXME define a way to get attributes
                            /*
                             * Iterator<Attribute> attributes = (Iterator<Attribute>)
                             * event.asStartElement().getAttributes(); while (attributes.hasNext()) { Attribute
                             * attribute = attributes.next(); writerJson.add(attribute); }
                             */
                            break;
                    }

                } else if (event.isCharacters()) {
                    writerJson.add(event.asCharacters());
                } else if (event.isEndElement()) {
                    QName endName = event.asEndElement().getName();
                    if (name.equals(endName)) {
                        stack--;
                        if (stack == 0) {
                            // Create objectgroup reference id
                            groupGuid = objectGroupIdToGuid.get(unitIdToGroupId.get(elementID));
                            writerJson.add(eventFactory.createStartElement("", "", SedaConstants.PREFIX_OG));
                            writerJson.add(eventFactory.createCharacters(groupGuid));
                            writerJson.add(eventFactory.createEndElement("", "", SedaConstants.PREFIX_OG));
                            break;
                        }
                    } else if (SedaConstants.TAG_RULE_ACCESS.equals(endName.getLocalPart()) ||
                        SedaConstants.TAG_RULE_REUSE.equals(endName.getLocalPart()) ||
                        SedaConstants.TAG_RULE_STORAGE.equals(endName.getLocalPart()) ||
                        SedaConstants.TAG_RULE_APPRAISAL.equals(endName.getLocalPart()) ||
                        SedaConstants.TAG_RULE_CLASSIFICATION.equals(endName.getLocalPart()) ||
                        SedaConstants.TAG_RULE_DISSEMINATION.equals(endName.getLocalPart())) {
                        writerJson
                            .add(eventFactory.createEndElement("", "", event.asEndElement().getName().getLocalPart()));
                        currentRuleType = null;
                        ruleInProgress = false;
                    } else {
                        writerJson
                            .add(eventFactory.createEndElement("", "", event.asEndElement().getName().getLocalPart()));
                    }
                } else if (event.isEndDocument()) {
                    writerJson.add(eventFactory.createEndDocument());
                }

            }
            writerJson.add(eventFactory.createEndElement("", "", startElement.getName().getLocalPart()));
            writerJson.add(eventFactory.createEndDocument());

            reader.close();
            writerJson.close();
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

    private void extractArchiveUnitTag(XMLEventReader reader, XMLEvent event, String archiveUnitId,
        ObjectNode archiveUnitTree, List<String> archiveUnitGuids, LogbookLifeCyclesClient logbookLifeCycleClient)
        throws ProcessingException {
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
    }

    private void extractArchiveUnitRefIdTag(XMLEventReader reader, String archiveUnitId, ObjectNode archiveUnitTree)
        throws XMLStreamException {
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

    }

    private void extractDataObjectReferenceIdTag(XMLEventReader reader, XMLEventWriter writerJson,
        XMLEventFactory eventFactory, String elementID)
        throws XMLStreamException, ProcessingManifestReferenceException {
        final String objRefId = reader.getElementText();
        unitIdToGroupId.put(elementID, objRefId);
        if (objectGroupIdToUnitId.get(objRefId) == null) {
            final List<String> archiveUnitList = new ArrayList<>();
            archiveUnitList.add(elementID);
            if (dataObjectIdWithoutObjectGroupId.containsKey(objRefId)) {
                final GotObj gotObj = dataObjectIdWithoutObjectGroupId.get(objRefId);
                final String gotGuid = gotObj.getGotGuid();
                objectGroupIdToUnitId.put(gotGuid, archiveUnitList);
                unitIdToGroupId.put(elementID, gotGuid); // update unitIdToGroupId with new GOT
                gotObj.setVisited(true); // update isVisited to true
                dataObjectIdWithoutObjectGroupId.put(objRefId, gotObj);
            }
        } else {
            final List<String> archiveUnitList = objectGroupIdToUnitId.get(objRefId);
            archiveUnitList.add(elementID);
            objectGroupIdToUnitId.put(objRefId, archiveUnitList);
        }

        final String newGroupId = getNewGdoIdFromGdoByUnit(objRefId);
        writerJson.add(eventFactory.createStartElement("", "", SedaConstants.TAG_DATA_OBJECT_GROUP_REFERENCEID));
        writerJson.add(eventFactory.createCharacters(newGroupId));
        writerJson.add(eventFactory.createEndElement("", "", SedaConstants.TAG_DATA_OBJECT_GROUP_REFERENCEID));

    }

    private void extractDataObjectGroupReferenceIdTag(XMLEventReader reader, XMLEventWriter writerJson,
        XMLEventFactory eventFactory,
        String elementID) throws XMLStreamException, ProcessingManifestReferenceException {
        final String groupId = reader.getElementText();
        unitIdToGroupId.put(elementID, groupId);
        if (objectGroupIdToUnitId.get(groupId) == null) {
            final List<String> archiveUnitList = new ArrayList<>();
            archiveUnitList.add(elementID);
            if (!dataObjectIdWithoutObjectGroupId.containsKey(groupId)) {
                objectGroupIdToUnitId.put(groupId, archiveUnitList);
            }
        } else {
            final List<String> archiveUnitList = objectGroupIdToUnitId.get(groupId);
            archiveUnitList.add(elementID);
            objectGroupIdToUnitId.put(groupId, archiveUnitList);
        }
        // Create new startElement for group with new guid
        final String newGroupId = getNewGdoIdFromGdoByUnit(unitIdToGroupId.get(elementID));
        writerJson.add(eventFactory.createStartElement("", "", SedaConstants.TAG_DATA_OBJECT_GROUP_REFERENCEID));
        writerJson.add(eventFactory.createCharacters(newGroupId));
        writerJson.add(eventFactory.createEndElement("", "", SedaConstants.TAG_DATA_OBJECT_GROUP_REFERENCEID));
    }

    private void extractRuleTag(XMLEventReader reader, XMLEventWriter writerJson, XMLEventFactory eventFactory,
        String elementID, String currentRuleType, boolean ruleInProgress) throws XMLStreamException {
        Set<String> setRuleIds = unitIdToSetOfRuleId.get(elementID);
        if (setRuleIds == null) {
            setRuleIds = new HashSet<>();
        }
        final String idRule = reader.getElementText();
        setRuleIds.add(idRule);
        unitIdToSetOfRuleId.put(elementID, setRuleIds);

        if (ruleInProgress) {
            writerJson.add(eventFactory.createEndElement("", "", currentRuleType));
            writerJson.add(eventFactory.createStartElement("", "", currentRuleType));
        }
        writerJson.add(eventFactory.createStartElement("", "", SedaConstants.TAG_RULE_RULE));
        writerJson.add(eventFactory.createCharacters(idRule));
        writerJson.add(eventFactory.createEndElement("", "", SedaConstants.TAG_RULE_RULE));
    }

    private String extractSystemIdTag(XMLEventReader reader, XMLEventWriter writerJson, XMLEventFactory eventFactory,
        Boolean existingUpdateOperation)
        throws XMLStreamException {
        // referencing existing element
        String elementText = reader.getElementText();
        String existingElementGuid = existingUpdateOperation ? elementText : null;
        writerJson.add(eventFactory.createStartElement("", "", SedaConstants.TAG_ARCHIVE_SYSTEM_ID));
        writerJson.add(eventFactory.createCharacters(elementText));
        writerJson.add(eventFactory.createEndElement("", "", SedaConstants.TAG_ARCHIVE_SYSTEM_ID));
        return existingElementGuid;
    }

    /**
     * Update unit file with existing archive unit. Will set the existing in the file.
     *
     * @param elementGuid archive unit guid
     * @param tmpFile     unit xml file
     * @param unitName    xml unit qualifier
     * @return the modified unit file
     * @throws ProcessingException thrown when an exception occurred while adding the data
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
             String type =    existingData.get("$results").get(0).get("_unitType").asText() ;
             UnitType dataUnitTye = UnitType.valueOf(type);

            if ( dataUnitTye.ordinal() < workflowUnitTYpe.ordinal() ){
                LOGGER.error("Linking not allowed  {}", elementGuid);
                throw new ProcessingUnitNotFoundException("Linking Unauthorized ");

            }
            nbAUExisting++;

            JsonNode archiveUnit = JsonHandler.getFromFile(tmpFile);
            ObjectNode archiveUnitNode = (ObjectNode) archiveUnit.get(SedaConstants.TAG_ARCHIVE_UNIT);
            archiveUnitNode.put(SedaConstants.PREFIX_ID, elementGuid);
            JsonHandler.writeAsFile(archiveUnit, newTmpFile);
            tmpFile.delete();
        } catch (InvalidParseOperationException | FactoryConfigurationError e) {
            LOGGER.error(e.getMessage());
            throw new ProcessingException(e);
        }
        return newTmpFile;
    }

    private void saveObjectGroupsToWorkspace(String containerId,
        LogbookLifeCyclesClient logbookLifeCycleClient, LogbookTypeProcess typeProcess, String prodService)
        throws ProcessingException {

        completeDataObjectToObjectGroupMap();

        // Save maps
        try {
            // Save dataObjectIdToObjectGroupId
            HandlerUtils.saveMap(handlerIO, dataObjectIdToObjectGroupId, DO_ID_TO_OG_ID_IO_RANK, true);
            // Save objectGroupIdToGuid
            HandlerUtils.saveMap(handlerIO, objectGroupIdToGuid, OG_ID_TO_GUID_IO_RANK, true);
            handlerIO.addOuputResult(OG_ID_TO_GUID_IO_MEMORY_RANK, objectGroupIdToGuid);
        } catch (final IOException e1) {
            LOGGER.error("Can not write to tmp folder ", e1);
            throw new ProcessingException(e1);
        }

        for (final Entry<String, List<String>> entry : objectGroupIdToDataObjectId.entrySet()) {
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
                    final File dataObjectFile =
                        handlerIO.getNewLocalFile(dataObjectIdToGuid.get(id) + JSON_EXTENSION);
                    JsonNode dataObjectNode = JsonHandler.getFromFile(dataObjectFile).get(BINARY_DATA_OBJECT);
                    if (dataObjectNode == null) {
                        dataObjectNode = JsonHandler.getFromFile(dataObjectFile).get(PHYSICAL_DATA_OBJECT);
                    }
                    String nodeCategory = "";
                    if (dataObjectNode.get(SedaConstants.TAG_DO_VERSION) != null) {
                        nodeCategory = dataObjectNode.get(SedaConstants.TAG_DO_VERSION).asText();
                        if (versionList.contains(nodeCategory)) {
                            LOGGER.error(DATA_OBJECT_VERSION_MUST_BE_UNIQUE);
                            throw new ProcessingDuplicatedVersionException(DATA_OBJECT_VERSION_MUST_BE_UNIQUE);
                        }
                        versionList.add(nodeCategory);
                    }
                    ArrayList<JsonNode> nodeCategoryArray = categoryMap.get(nodeCategory);
                    if (nodeCategory.split("_").length == 1) {
                        final String nodeCategoryNumbered = nodeCategory + "_1";
                        ((ObjectNode) dataObjectNode).put(SedaConstants.TAG_DO_VERSION, nodeCategoryNumbered);
                    }
                    if (nodeCategoryArray == null) {
                        nodeCategoryArray = new ArrayList<>();
                        nodeCategoryArray.add(dataObjectNode);
                    } else {
                        final int dataObjectNodePosition = Integer.parseInt(nodeCategory.split("_")[1]) - 1;
                        nodeCategoryArray.add(dataObjectNodePosition, dataObjectNode);
                    }
                    categoryMap.put(nodeCategory, nodeCategoryArray);
                    if (BINARY_MASTER.equals(nodeCategory)) {

                        fileInfo = (ObjectNode) dataObjectNode.get(FILE_INFO);
                        if (dataObjectNode.get(METADATA) != null) {
                            objectGroupType = dataObjectNode.get(METADATA).fieldNames().next();
                        }
                    }
                    if (!dataObjectFile.delete()) {
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
                objectGroup.put(SedaConstants.TAG_ORIGINATINGAGENCY, prodService);

                JsonHandler.writeAsFile(objectGroup, tmpFile);

                handlerIO.transferFileToWorkspace(
                    IngestWorkflowConstants.OBJECT_GROUP_FOLDER + "/" + objectGroupGuid + JSON_EXTENSION,
                    tmpFile, true);
                // Create unreferenced object group
                if (guidToLifeCycleParameters.get(objectGroupGuid) == null) {
                    createObjectGroupLifeCycle(objectGroupGuid, containerId, logbookLifeCycleClient, typeProcess);

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
            final ObjectNode objectNode = JsonHandler.createObjectNode();
            objectNode.put(SedaConstants.TAG_NB, entry.getValue().size());
            final ArrayNode arrayNode = JsonHandler.createArrayNode();
            for (final JsonNode node : entry.getValue()) {
                final String id = node.findValue(SedaConstants.PREFIX_ID).textValue();
                final String guid = dataObjectIdToGuid.get(id);
                updateObjectNode((ObjectNode) node, guid);
                arrayNode.add(node);
            }
            objectNode.set(SedaConstants.TAG_VERSIONS, arrayNode);
            qualifierObject.set(entry.getKey(), objectNode);
        }
        return qualifierObject;
    }

    private ObjectNode getObjectGroupWork(Map<String, ArrayList<JsonNode>> categoryMap) {
        final ObjectNode workObject = JsonHandler.createObjectNode();
        final ObjectNode qualifierObject = JsonHandler.createObjectNode();
        for (final Entry<String, ArrayList<JsonNode>> entry : categoryMap.entrySet()) {
            final ObjectNode dataObjectNode = JsonHandler.createObjectNode();
            dataObjectNode.put(SedaConstants.TAG_NB, entry.getValue().size());
            final ArrayNode arrayNode = JsonHandler.createArrayNode();
            for (final JsonNode node : entry.getValue()) {
                final ObjectNode objectNode = JsonHandler.createObjectNode();
                final String id = node.findValue(SedaConstants.PREFIX_ID).textValue();
                updateObjectNode(objectNode, id);
                if (physicalDataObjetsGuids.contains(id)) {
                    objectNode.set(SedaConstants.TAG_PHYSICAL_ID, node.get(SedaConstants.TAG_PHYSICAL_ID));
                }

                arrayNode.add(objectNode);
            }
            dataObjectNode.set(SedaConstants.TAG_VERSIONS, arrayNode);
            qualifierObject.set(entry.getKey(), dataObjectNode);
        }
        workObject.set(SedaConstants.PREFIX_QUALIFIERS, qualifierObject);
        return workObject;
    }

    /**
     * Update data object json node with data from maps
     *
     * @param objectNode data object json node
     * @param guid       guid of data object
     */
    private void updateObjectNode(final ObjectNode objectNode, String guid) {
        objectNode.put(SedaConstants.PREFIX_ID, guid);
        if (objectGuidToDataObject.get(guid).getSize() != null) {
            objectNode.put(SedaConstants.TAG_SIZE, objectGuidToDataObject.get(guid).getSize());
        }
        if (objectGuidToDataObject.get(guid).getUri() != null) {
            objectNode.put(SedaConstants.TAG_URI, objectGuidToDataObject.get(guid).getUri());
        }
        if (objectGuidToDataObject.get(guid).getMessageDigest() != null) {
            objectNode.put(SedaConstants.TAG_DIGEST,
                objectGuidToDataObject.get(guid).getMessageDigest());
        }
        if (objectGuidToDataObject.get(guid).getAlgo() != null) {
            objectNode.put(SedaConstants.ALGORITHM,
                objectGuidToDataObject.get(guid).getAlgo().getName());
        }
    }


    /**
     * Load data of an existing archive unit by its vitam id.
     *
     * @param archiveUnitId guid of archive unit
     * @return AU response
     * @throws ProcessingUnitNotFoundException thrown if unit not found
     * @throws ProcessingException             thrown if a metadata exception occured
     */
    private JsonNode loadExistingArchiveUnit(String archiveUnitId) throws ProcessingException {

        try (MetaDataClient metadataClient = MetaDataClientFactory.getInstance().getClient()) {
            final SelectParserMultiple selectRequest = new SelectParserMultiple();
            final SelectMultiQuery request = selectRequest.getRequest().reset();
            return metadataClient.selectUnitbyId(request.getFinalSelect(), archiveUnitId);

        } catch (final MetaDataException e) {
            LOGGER.error("Internal Server Error", e);
            throw new ProcessingException(e);

        } catch (final InvalidParseOperationException e) {
            LOGGER.error("Existing Unit was not found", e);
            throw new ProcessingUnitNotFoundException("Existing Unit was not found");
        }
    }


    private void completeDataObjectToObjectGroupMap() {
        for (final String key : dataObjectIdToObjectGroupId.keySet()) {
            if ("".equals(dataObjectIdToObjectGroupId.get(key))) {
                final List<String> dataOjectList = new ArrayList<>();
                dataOjectList.add(key);
                objectGroupIdToDataObjectId.put(GUIDFactory.newGUID().toString(), dataOjectList);
                // TODO P0 Create OG / OG lifeCycle
            }
        }
    }

    private void linkToArchiveUnitDeclaredInTheIngestContract(ArrayNode upNode) {
        findArchiveUnitDeclaredInTheIngestContract();
        if (filingParentId != null) {
            upNode.add(filingParentId);
        }
    }

    private void findArchiveUnitDeclaredInTheIngestContract() {
        try (final AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient();
            final MetaDataClient metaDataClient = MetaDataClientFactory.getInstance().getClient()) {

            if (contractName != null) {
                Select select = new Select();
                select.setQuery(QueryHelper.eq(CONTRACT_NAME, contractName));
                JsonNode queryDsl = select.getFinalSelect();
                RequestResponse<IngestContractModel> referenceContracts = client.findIngestContracts(queryDsl);
                if (referenceContracts.isOk()) {
                    List<IngestContractModel> results = ((RequestResponseOK) referenceContracts).getResults();
                    if (!results.isEmpty()) {
                        for (IngestContractModel result : results) {
                            filingParentId = result.getFilingParentId();
                        }
                    }

                    if (filingParentId != null) {
                        select = new Select();
                        select.setQuery(QueryHelper.eq(UNITTYPE.exactToken(), FILING_UNIT).setDepthLimit(0));
                        queryDsl = select.getFinalSelect();
                        JsonNode res = metaDataClient.selectUnitbyId(queryDsl, filingParentId).get("$results").get(0);

                        ObjectNode archiveUnit = JsonHandler.createObjectNode();
                        createArchiveUnitDeclaredInTheIngestContract(archiveUnit, res);
                        saveArchiveUnitDeclaredInTheIngestContract(archiveUnit, filingParentId);
                    }

                }
            }


        } catch (AdminManagementClientServerException | InvalidParseOperationException e) {
            LOGGER.error("Contract found but inactive: ", e);
        } catch (InvalidCreateOperationException e) {
            LOGGER.error("Contract not found :", e);
        } catch (MetaDataExecutionException | MetaDataDocumentSizeException | MetaDataClientServerException e) {
            LOGGER.error("Metadata does not work :", e);
        } catch (ProcessingException e) {
            LOGGER.error("Cannot store the archive unit declared in the ingest contract :", e);
        }
    }

    private void createArchiveUnitDeclaredInTheIngestContract(ObjectNode archiveUnit, JsonNode res) {
        archiveUnit.set(SedaConstants.TAG_ARCHIVE_UNIT, res);

        // Add _work information
        ObjectNode workNode = JsonHandler.createObjectNode();
        workNode.put(IngestWorkflowConstants.EXISTING_TAG, Boolean.TRUE);

        archiveUnit.set(SedaConstants.PREFIX_WORK, workNode);
    }

    private void saveArchiveUnitDeclaredInTheIngestContract(ObjectNode archiveUnit,
        String filingParentId) throws InvalidParseOperationException, ProcessingException {

        final File unitCompleteTmpFile = handlerIO.getNewLocalFile(filingParentId);

        // Write to new File
        JsonHandler.writeAsFile(archiveUnit, unitCompleteTmpFile);

        // Write to workspace
        handlerIO.transferFileToWorkspace(
            IngestWorkflowConstants.ARCHIVE_UNIT_FOLDER + "/" + filingParentId + JSON_EXTENSION, unitCompleteTmpFile,
            true);
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        if (!handler.checkHandlerIO(HANDLER_IO_OUT_PARAMETER_NUMBER, handlerInitialIOList)) {
            throw new ProcessingException(HandlerIOImpl.NOT_CONFORM_PARAM);
        }
    }
    public UnitType getWorkflowUnitTYpe() {
        return workflowUnitTYpe;
    }

    public void setWorkflowUnitTYpe(UnitType workflowUnitTYpe) {
        this.workflowUnitTYpe = workflowUnitTYpe;
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
