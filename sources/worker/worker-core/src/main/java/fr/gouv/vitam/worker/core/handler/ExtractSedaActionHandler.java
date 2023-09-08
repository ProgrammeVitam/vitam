/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
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
package fr.gouv.vitam.worker.core.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import de.odysseus.staxon.json.JsonXMLConfig;
import de.odysseus.staxon.json.JsonXMLConfigBuilder;
import de.odysseus.staxon.json.JsonXMLOutputFactory;
import fr.gouv.culture.archivesdefrance.seda.v2.ArchiveUnitType;
import fr.gouv.culture.archivesdefrance.seda.v2.BinaryDataObjectType;
import fr.gouv.culture.archivesdefrance.seda.v2.DataObjectGroupType;
import fr.gouv.culture.archivesdefrance.seda.v2.DataObjectOrArchiveUnitReferenceType;
import fr.gouv.culture.archivesdefrance.seda.v2.PhysicalDataObjectType;
import fr.gouv.culture.archivesdefrance.seda.v2.RelatedObjectReferenceType;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.InQuery;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.parser.query.ParserTokens;
import fr.gouv.vitam.common.exception.CycleFoundException;
import fr.gouv.vitam.common.exception.InvalidGuidOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.graph.DirectedCycle;
import fr.gouv.vitam.common.graph.DirectedGraph;
import fr.gouv.vitam.common.graph.Graph;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.IngestWorkflowConstants;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.MetadataType;
import fr.gouv.vitam.common.model.QueryProjection;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.UnitType;
import fr.gouv.vitam.common.model.administration.ContractsDetailsModel;
import fr.gouv.vitam.common.model.administration.IngestContractCheckState;
import fr.gouv.vitam.common.model.administration.IngestContractModel;
import fr.gouv.vitam.common.model.administration.OntologyModel;
import fr.gouv.vitam.common.model.administration.RuleType;
import fr.gouv.vitam.common.model.logbook.LogbookEvent;
import fr.gouv.vitam.common.model.unit.ManagementModel;
import fr.gouv.vitam.common.model.unit.RuleCategoryModel;
import fr.gouv.vitam.common.model.unit.RuleModel;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.performance.PerformanceLogger;
import fr.gouv.vitam.common.xml.XMLInputFactoryUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.model.LogbookLifeCycleObjectGroupModel;
import fr.gouv.vitam.logbook.common.model.LogbookLifeCycleUnitModel;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleObjectGroupParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleUnitParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCyclesClientHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.core.database.collections.Unit;
import fr.gouv.vitam.processing.common.exception.ArchiveUnitContainDataObjectException;
import fr.gouv.vitam.processing.common.exception.MetaDataContainSpecialCharactersException;
import fr.gouv.vitam.processing.common.exception.ExceptionType;
import fr.gouv.vitam.processing.common.exception.MissingFieldException;
import fr.gouv.vitam.processing.common.exception.ProcessingAttachmentRequiredException;
import fr.gouv.vitam.processing.common.exception.ProcessingAttachmentUnauthorizedException;
import fr.gouv.vitam.processing.common.exception.ProcessingDuplicatedVersionException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.exception.ProcessingMalformedDataException;
import fr.gouv.vitam.processing.common.exception.ProcessingManifestReferenceException;
import fr.gouv.vitam.processing.common.exception.ProcessingNotFoundException;
import fr.gouv.vitam.processing.common.exception.ProcessingNotValidLinkingException;
import fr.gouv.vitam.processing.common.exception.ProcessingObjectGroupEveryDataObjectVersionException;
import fr.gouv.vitam.processing.common.exception.ProcessingObjectGroupLifeCycleException;
import fr.gouv.vitam.processing.common.exception.ProcessingObjectGroupLinkingException;
import fr.gouv.vitam.processing.common.exception.ProcessingObjectGroupMasterMandatoryException;
import fr.gouv.vitam.processing.common.exception.ProcessingObjectReferenceException;
import fr.gouv.vitam.processing.common.exception.ProcessingTooManyUnitsFoundException;
import fr.gouv.vitam.processing.common.exception.ProcessingTooManyVersionsByUsageException;
import fr.gouv.vitam.processing.common.exception.ProcessingUnitLinkingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.common.utils.SedaUtils;
import fr.gouv.vitam.worker.common.utils.SedaUtilsFactory;
import fr.gouv.vitam.worker.core.exception.ProcessingStatusException;
import fr.gouv.vitam.worker.core.exception.WorkerspaceQueueException;
import fr.gouv.vitam.worker.core.extractseda.ExtractMetadataListener;
import fr.gouv.vitam.worker.core.extractseda.IngestContext;
import fr.gouv.vitam.worker.core.extractseda.IngestSession;
import fr.gouv.vitam.worker.core.impl.HandlerIOImpl;
import fr.gouv.vitam.worker.core.utils.JsonLineDataBase;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.SedaConstants.TAG_ARCHIVE_TRANSFER;
import static fr.gouv.vitam.common.database.parser.query.QueryParserHelper.ne;
import static fr.gouv.vitam.common.json.JsonHandler.createObjectNode;
import static fr.gouv.vitam.common.model.IngestWorkflowConstants.LOGBOOK_OG_FILE_SUFFIX;
import static fr.gouv.vitam.common.model.IngestWorkflowConstants.SEDA_FILE;
import static fr.gouv.vitam.common.model.IngestWorkflowConstants.SEDA_FOLDER;
import static fr.gouv.vitam.common.model.LifeCycleStatusCode.LIFE_CYCLE_IN_PROCESS;
import static fr.gouv.vitam.common.utils.SupportedSedaVersions.UNIFIED_NAMESPACE;
import static fr.gouv.vitam.logbook.common.parameters.LogbookParameterName.agentIdentifier;
import static fr.gouv.vitam.logbook.common.parameters.LogbookParameterName.eventDateTime;
import static fr.gouv.vitam.logbook.common.parameters.LogbookParameterName.eventIdentifier;
import static fr.gouv.vitam.logbook.common.parameters.LogbookParameterName.eventIdentifierProcess;
import static fr.gouv.vitam.logbook.common.parameters.LogbookParameterName.eventType;
import static fr.gouv.vitam.logbook.common.parameters.LogbookParameterName.eventTypeProcess;
import static fr.gouv.vitam.logbook.common.parameters.LogbookParameterName.objectIdentifier;
import static fr.gouv.vitam.logbook.common.parameters.LogbookParameterName.outcome;
import static fr.gouv.vitam.logbook.common.parameters.LogbookParameterName.outcomeDetail;
import static fr.gouv.vitam.logbook.common.parameters.LogbookParameterName.outcomeDetailMessage;
import static fr.gouv.vitam.logbook.common.parameters.LogbookParameterName.parentEventIdentifier;

/**
 * Handler class used to extract metaData. </br>
 * Create and put a new file (metadata extracted) json.json into container GUID
 */
public class ExtractSedaActionHandler extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ExtractSedaActionHandler.class);
    private static final TypeReference<List<LogbookEvent>> LIST_TYPE_REFERENCE = new TypeReference<>() {
    };
    private static final PerformanceLogger PERFORMANCE_LOGGER = PerformanceLogger.getInstance();

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
    private static final int EXISTING_GOT_RANK = 9;
    private static final int GUID_TO_UNIT_ID_IO_RANK = 10;
    private static final int HANDLER_IO_OUT_PARAMETER_NUMBER = 15;
    private static final int ONTOLOGY_IO_RANK = 11;
    private static final int EXISTING_GOT_TO_NEW_GOT_GUID_FOR_ATTACHMENT_RANK = 12;
    private static final int EXISTING_UNITS_GUID_FOR_ATTACHMENT_RANK = 13;
    private static final int EXISTING_GOTS_GUID_FOR_ATTACHMENT_RANK = 14;

    // IN RANK
    private static final int UNIT_TYPE_INPUT_RANK = 1;
    private static final int STORAGE_INFO_INPUT_RANK = 2;
    private static final int CONTRACTS_INPUT_RANK = 3;

    private static final String TRANSFORM_XSLT_PATH = "transform.xsl";

    private static final String HANDLER_ID = "CHECK_MANIFEST";
    private static final String SUBTASK_LOOP = "CHECK_MANIFEST_LOOP";
    public static final String SUBTASK_ERROR_PARSE_ATTACHMENT = "ERROR_PARSE_ATTACHMENT";
    public static final String SUBTASK_EMPTY_KEY_ATTACHMENT = "EMPTY_KEY_ATTACHMENT";
    public static final String SUBTASK_NULL_LINK_PARENT_ID_ATTACHMENT = "NULL_LINK_PARENT_ID_ATTACHMENT";
    public static final String SUBTASK_TOO_MANY_FOUND_ATTACHMENT = "TOO_MANY_FOUND_ATTACHMENT";
    public static final String SUBTASK_TOO_MANY_VERSION_BY_USAGE = "TOO_MANY_VERSION_BY_USAGE";
    public static final String SUBTASK_NOT_FOUND_ATTACHMENT = "NOT_FOUND_ATTACHMENT";
    public static final String SUBTASK_ATTACHMENT_REQUIRED = "ATTACHMENT_REQUIRED";
    public static final String SUBTASK_UNAUTHORIZED_ATTACHMENT = "UNAUTHORIZED_ATTACHMENT";
    public static final String SUBTASK_UNAUTHORIZED_ATTACHMENT_BY_CONTRACT = "UNAUTHORIZED_ATTACHMENT_BY_CONTRACT";
    public static final String SUBTASK_UNAUTHORIZED_ATTACHMENT_BY_BAD_SP = "SUBTASK_UNAUTHORIZED_ATTACHMENT_BY_BAD_SP";
    public static final String SUBTASK_INVALID_GUID_ATTACHMENT = "INVALID_GUID_ATTACHMENT";
    public static final String SUBTASK_MODIFY_PARENT_EXISTING_UNIT_UNAUTHORIZED =
        "MODIFY_PARENT_EXISTING_UNIT_UNAUTHORIZED";

    private static final String SUBTASK_MALFORMED = "CHECK_MANIFEST_MALFORMED_DATA";
    private static final String AU_REFRENCES_MULTIPLE_GOT = "ARCHIVEUNIT_REFERENCES_MULTIPLE_OBJECTGROUP";
    private static final String EXISTING_OG_NOT_DECLARED = "EXISTING_OG_NOT_DECLARED";
    private static final String MASTER_MANDATORY_REQUIRED = "MASTER_MANDATORY_REQUIRED";
    private static final String ATTACHMENT_OBJECTGROUP = "ATTACHMENT_OBJECTGROUP";
    private static final String LFC_INITIAL_CREATION_EVENT_TYPE = "LFC_CREATION";
    private static final String LFC_CREATION_SUB_TASK_ID = "LFC_CREATION";
    private static final String ATTACHMENT_IDS = "_up";
    private static final String OBJECT_GROUP_ID = "_og";
    private static final String TRANSFER_AGENCY = "TransferringAgency";
    private static final String ARCHIVAL_AGENCY = "ArchivalAgency";
    private static final int BATCH_SIZE = 50;
    private static final String RULES = "Rules";
    private static final int MAX_ELASTIC_REQUEST_SIZE = 1000;
    private static final String ORIGIN_ANGENCY_NAME = "originatingAgency";
    private static final String ORIGIN_ANGENCY_SUBMISSION = "submissionAgency";
    private static final String ARCHIVAl_AGREEMENT = "ArchivalAgreement";
    private static final String ARCHIVAl_PROFIL = "ArchivalProfile";

    private static final String EV_DETAIL_REQ = "EvDetailReq";
    private static final String JSON_EXTENSION = ".json";
    private static final String DATA_OBJECT_GROUP = "DataObjectGroup";
    private static final String BINARY_DATA_OBJECT = "BinaryDataObject";
    private static final String PHYSICAL_DATA_OBJECT = "PhysicalDataObject";
    private static final String ARCHIVE_UNIT = "ArchiveUnit";
    private static final String BINARY_MASTER = "BinaryMaster";
    private static final String PHYSICAL_MASTER = "PhysicalMaster";
    private static final String DATAOBJECT_PACKAGE = "DataObjectPackage";
    private static final String FILE_INFO = "FileInfo";
    private static final String METADATA = "Metadata";
    private static final String LOGBOOK_LF_BAD_REQUEST_EXCEPTION_MSG = "LogbookClient Unsupported request";
    private static final String LOGBOOK_LF_OBJECT_EXISTS_EXCEPTION_MSG = "LifeCycle Object already exists";
    private static final String LOGBOOK_LF_RESOURCE_NOT_FOUND_EXCEPTION_MSG = "Logbook LifeCycle resource not found";
    private static final String LOGBOOK_SERVER_INTERNAL_EXCEPTION_MSG = "Logbook Server internal error";
    private static final String DATA_OBJECT_VERSION_MUST_BE_UNIQUE = "ERROR: DataObject version must be unique";
    private static final String LEVEL = "level_";

    private static final String GRAPH_CYCLE_MSG = "The Archive Unit graph in the SEDA file has a cycle";
    private static final String CYCLE_FOUND_EXCEPTION = "Seda has an archive unit cycle ";
    private static final String SAVE_ARCHIVE_ID_TO_GUID_IOEXCEPTION_MSG =
        "Can not save unitToGuidMap to temporary file";
    private static final String FILE_COULD_NOT_BE_DELETED_MSG = "File could not be deleted";
    private static final String CANNOT_READ_SEDA = "Can not read SEDA";
    private static final String MANIFEST_NOT_FOUND = "Manifest.xml Not Found";
    private static final String ARCHIVE_UNIT_TMP_FILE_PREFIX = "AU_TMP_";
    private static final String MISSING_STORAGE_INFO = "Missing one or more storage infos";
    private static final String GLOBAL_MGT_RULE_TAG = "GLOBAL_MGT_RULE";

    private final static List<Class<?>> HANDLER_INPUT_IO_LIST =
        Arrays.asList(String.class, String.class, File.class, File.class);

    private final static String namespaceURI = UNIFIED_NAMESPACE;

    private static JAXBContext jaxbContext;

    static {
        try {
            jaxbContext = JAXBContext.newInstance(ArchiveUnitType.class.getPackage().getName() +
                ":fr.gouv.vitam.common.model.unit:fr.gouv.vitam.common.model.objectgroup");
        } catch (JAXBException e) {
            LOGGER.error("unable to create jaxb context", e);
        }
    }

    private final MetaDataClientFactory metaDataClientFactory;
    private final AdminManagementClientFactory adminManagementClientFactory;
    private final TransformerFactory transformerFactory;
    private final SedaUtilsFactory sedaUtilsFactory;
    private final static boolean asyncIO = true;
    private final LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory;


    /**
     * Constructor with parameter SedaUtilsFactory
     */
    public ExtractSedaActionHandler() {
        this(MetaDataClientFactory.getInstance(), AdminManagementClientFactory.getInstance(),
            LogbookLifeCyclesClientFactory.getInstance());
    }

    @VisibleForTesting
    ExtractSedaActionHandler(MetaDataClientFactory metaDataClientFactory,
        AdminManagementClientFactory adminManagementClientFactory,
        LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory) {
        this.metaDataClientFactory = metaDataClientFactory;
        this.adminManagementClientFactory = adminManagementClientFactory;
        this.logbookLifeCyclesClientFactory = logbookLifeCyclesClientFactory;
        this.transformerFactory = TransformerFactory.newInstance();
        this.sedaUtilsFactory = SedaUtilsFactory.getInstance();
    }

    /**
     * @return HANDLER_ID
     */
    public static String getId() {
        return HANDLER_ID;
    }

    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO handlerIO) {
        // Initialize Ingest Session
        IngestSession ingestSession = new IngestSession();
        return execute(params, handlerIO, ingestSession);
    }

    @VisibleForTesting
    ItemStatus execute(WorkerParameters params, HandlerIO handlerIO, IngestSession ingestSession) {
        checkMandatoryParameters(params);
        final ItemStatus globalCompositeItemStatus = new ItemStatus(HANDLER_ID);



        try {
            IngestContext ingestContext = retrieveIngestContext(params, handlerIO);


            Map<String, Long> filesWithParamsFromWorkspace =
                handlerIO.getFilesWithParamsFromWorkspace(handlerIO.getContainerName(), SEDA_FOLDER);
            ingestSession.getFileWithParmsFromFolder().putAll(filesWithParamsFromWorkspace);

            if (asyncIO) {
                handlerIO.enableAsync(true);
            }

            checkMandatoryIOParameter(handlerIO);

            final SedaUtils sedaUtils = sedaUtilsFactory.createSedaUtilsWithSedaIngestParams(handlerIO);
            final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

            ingestContext.setSedaVersion(sedaUtils.getSedaIngestParams().getVersion());

            JsonLineDataBase unitsDatabase = new JsonLineDataBase(handlerIO.getNewLocalFile("units.jsonl"));
            JsonLineDataBase objectsDatabase = new JsonLineDataBase(handlerIO.getNewLocalFile("objects.jsonl"));

            ExtractMetadataListener listener =
                new ExtractMetadataListener(handlerIO, ingestContext, ingestSession, unitsDatabase, objectsDatabase,
                    metaDataClientFactory);

            unmarshaller.setListener(listener);
            ObjectNode evDetData =
                extractSEDA(handlerIO, unmarshaller, ingestContext, ingestSession, unitsDatabase, objectsDatabase,
                    globalCompositeItemStatus);

            if (!ingestSession.getExistingUnitGuids().isEmpty()) {
                evDetData.set(ATTACHMENT_IDS, JsonHandler.toJsonNode(ingestSession.getExistingUnitGuids()));
            }

            globalCompositeItemStatus.setEvDetailData(JsonHandler.unprettyPrint(evDetData));
            globalCompositeItemStatus.setMasterData(LogbookParameterName.eventDetailData.name(),
                JsonHandler.unprettyPrint(evDetData));
            globalCompositeItemStatus.increment(StatusCode.OK);

            if (asyncIO) {
                handlerIO.enableAsync(false);
            }
            ObjectNode agIdExt = buildAgIdExt(ingestContext);
            /*
             * setting agIdExt information
             */
            if (agIdExt.size() > 0) {
                globalCompositeItemStatus.setMasterData(LogbookMongoDbName.agIdExt.getDbname(), agIdExt.toString());
                globalCompositeItemStatus.setData(LogbookMongoDbName.agIdExt.getDbname(), agIdExt.toString());
            }

            extractOntology(handlerIO);

            ObjectNode rightsStatementIdentifier = buildRightsStatementIdentifier(ingestContext);
            /*
             * setting rightsStatementIdentifier information
             */
            if (rightsStatementIdentifier.size() > 0) {
                globalCompositeItemStatus.setData(LogbookMongoDbName.rightsStatementIdentifier.getDbname(),
                    rightsStatementIdentifier.toString());
                globalCompositeItemStatus.setMasterData(LogbookMongoDbName.rightsStatementIdentifier.getDbname(),
                    rightsStatementIdentifier.toString());
                ObjectNode data = (ObjectNode) JsonHandler.getFromString(globalCompositeItemStatus.getEvDetailData());
                data.set(LogbookMongoDbName.rightsStatementIdentifier.getDbname(), rightsStatementIdentifier);
                globalCompositeItemStatus.setEvDetailData(data.toString());
                globalCompositeItemStatus.setData(LogbookMongoDbName.rightsStatementIdentifier.getDbname(),
                    rightsStatementIdentifier.toString());
            }

        } catch (final ProcessingDuplicatedVersionException e) {
            LOGGER.debug("ProcessingException: duplicated version", e);
            globalCompositeItemStatus.increment(StatusCode.KO);
        } catch (final ProcessingNotFoundException e) {
            LOGGER.debug("ProcessingException : " + e.getType() + " not found", e);

            String message;

            if (e.getType() == ExceptionType.UNIT) {
                message = getMessageItemStatusAUNotFound(e.getManifestId(), e.getGuid(), e.isValidGuid());
            } else {
                message = getMessageItemStatusOGNotFound(e.getManifestId(), e.getGuid(), e.isValidGuid());
            }
            updateDetailItemStatus(globalCompositeItemStatus, message, e.getTaskKey());
            globalCompositeItemStatus.increment(StatusCode.KO);

        } catch (final ProcessingTooManyUnitsFoundException e) {
            LOGGER.debug("ProcessingException : multiple units found", e);
            updateDetailItemStatus(globalCompositeItemStatus,
                getMessageItemStatusAUNotFound(e.getUnitId(), e.getUnitGuid(), e.isValidGuid()),
                SUBTASK_TOO_MANY_FOUND_ATTACHMENT);
            globalCompositeItemStatus.increment(StatusCode.KO);
        } catch (final ProcessingTooManyVersionsByUsageException e) {
            LOGGER.debug("ProcessingException :", e);
            updateDetailItemStatus(globalCompositeItemStatus,
                JsonHandler.unprettyPrint(JsonHandler.createObjectNode().put("MsgError", e.getMessage())),
                SUBTASK_TOO_MANY_VERSION_BY_USAGE);
            globalCompositeItemStatus.increment(StatusCode.KO);
        } catch (final ProcessingMalformedDataException e) {
            LOGGER.debug("ProcessingException : Missing or malformed data in the manifest", e);
            ObjectNode error = JsonHandler.createObjectNode();
            error.put("error", e.getMessage());
            updateDetailItemStatus(globalCompositeItemStatus, JsonHandler.unprettyPrint(error), SUBTASK_MALFORMED);
            globalCompositeItemStatus.increment(StatusCode.KO);
        } catch (final ProcessingObjectReferenceException e) {
            LOGGER.error("ProcessingObjectReferenceException: archive unit references more than one got");
            ObjectNode error = JsonHandler.createObjectNode();
            error.put("error", e.getMessage());
            updateDetailItemStatus(globalCompositeItemStatus, JsonHandler.unprettyPrint(error),
                AU_REFRENCES_MULTIPLE_GOT);
            globalCompositeItemStatus.increment(StatusCode.KO);
        } catch (final ProcessingManifestReferenceException e) {
            LOGGER.debug("ProcessingException : reference incorrect in Manifest", e);

            updateItemStatusForManifestReferenceException(globalCompositeItemStatus, e);
            globalCompositeItemStatus.increment(StatusCode.KO);

        } catch (final MissingFieldException e) {
            LOGGER.debug("MissingFieldException", e);
            globalCompositeItemStatus.increment(StatusCode.KO);
        } catch (final ProcessingObjectGroupMasterMandatoryException e) {
            globalCompositeItemStatus.increment(StatusCode.KO);
            updateDetailItemStatus(globalCompositeItemStatus,
                getMessageItemStatusGOTMasterMandatory(e.getObjectGroupId()), MASTER_MANDATORY_REQUIRED);
        } catch (ProcessingObjectGroupEveryDataObjectVersionException e) {
            globalCompositeItemStatus.increment(StatusCode.KO);
            updateDetailItemStatus(globalCompositeItemStatus,
                getMessageItemStatusGOTEveryDataObjectVersion(e.getUnitId(), e.getObjectGroupId()),
                ATTACHMENT_OBJECTGROUP);
        } catch (final ArchiveUnitContainDataObjectException e) {
            LOGGER.debug("ProcessingException: archive unit contain an data object declared object group.", e);
            globalCompositeItemStatus.setEvDetailData(e.getEventDetailData());
            updateDetailItemStatus(globalCompositeItemStatus,
                getMessageItemStatusAUDeclaringObject(e.getUnitId(), e.getBdoId(), e.getGotId()),
                EXISTING_OG_NOT_DECLARED);
            globalCompositeItemStatus.increment(StatusCode.KO);
        } catch (final MetaDataContainSpecialCharactersException e) {
            LOGGER.debug("ProcessingException: archive unit contains special characters.", e);
            globalCompositeItemStatus.increment(StatusCode.KO);
        } catch (final ProcessingUnitLinkingException e) {
            LOGGER.debug("ProcessingException: Linking FILING_UNIT or HOLDING_UNIT to INGEST Unauthorized", e);
            updateDetailItemStatus(globalCompositeItemStatus, getMessageItemStatusAULinkingException(e),
                SUBTASK_UNAUTHORIZED_ATTACHMENT);
            globalCompositeItemStatus.increment(StatusCode.KO);
        } catch (final ProcessingAttachmentUnauthorizedException e) {
            updateDetailItemStatus(globalCompositeItemStatus, e.getMessage(),
                SUBTASK_UNAUTHORIZED_ATTACHMENT_BY_CONTRACT);
            globalCompositeItemStatus.increment(StatusCode.KO);
        } catch (final ProcessingNotValidLinkingException e) {
            updateDetailItemStatus(globalCompositeItemStatus, e.getMessage(),
                SUBTASK_UNAUTHORIZED_ATTACHMENT_BY_BAD_SP);
            globalCompositeItemStatus.increment(StatusCode.KO);
        } catch (final ProcessingAttachmentRequiredException e) {
            updateDetailItemStatus(globalCompositeItemStatus, e.getMessage(), SUBTASK_ATTACHMENT_REQUIRED);
            globalCompositeItemStatus.increment(StatusCode.KO);
        } catch (final ProcessingObjectGroupLifeCycleException e) {
            updateDetailItemStatus(globalCompositeItemStatus,
                e.getMessage(), null);
            globalCompositeItemStatus.increment(StatusCode.KO);
        } catch (final ProcessingObjectGroupLinkingException e) {
            updateDetailItemStatus(globalCompositeItemStatus,
                getMessageItemStatusGOTLinkingException(e.getUnitId(), e.getObjectGroupId()),
                SUBTASK_UNAUTHORIZED_ATTACHMENT);
            globalCompositeItemStatus.increment(StatusCode.KO);
        } catch (final ProcessingException | WorkerspaceQueueException | InvalidParseOperationException e) {
            LOGGER.debug("ProcessingException ", e);
            globalCompositeItemStatus.increment(StatusCode.FATAL);
        } catch (final CycleFoundException e) {
            LOGGER.debug("ProcessingException: cycle found", e);
            globalCompositeItemStatus.setEvDetailData(e.getEventDetailData());
            updateDetailItemStatus(globalCompositeItemStatus, e.getCycle(), SUBTASK_LOOP);
            globalCompositeItemStatus.increment(StatusCode.KO);
        } catch (JAXBException e) {
            LOGGER.error("unable to create ExtractSeda handler, unmarshaller failed", e);
            globalCompositeItemStatus.increment(StatusCode.FATAL);
        }

        return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, globalCompositeItemStatus);
    }

    private IngestContext retrieveIngestContext(WorkerParameters params, HandlerIO handlerIO)
        throws InvalidParseOperationException {
        // Initialize Ingest Context
        final ContractsDetailsModel contracts =
            JsonHandler.getFromFile((File) handlerIO.getInput(CONTRACTS_INPUT_RANK), ContractsDetailsModel.class);
        IngestContext ingestContext = new IngestContext();
        ingestContext.setWorkflowUnitType(getUnitType(handlerIO));
        ingestContext.setTypeProcess(params.getLogbookTypeProcess());
        ingestContext.setOperationId(params.getContainerName());
        ingestContext.setIngestContract(contracts.getIngestContractModel());
        ingestContext.setManagementContractModel(contracts.getManagementContractModel());
        return ingestContext;
    }

    private static ObjectNode buildRightsStatementIdentifier(IngestContext ingestContext) {
        ObjectNode rightsStatementIdentifier = JsonHandler.createObjectNode();
        IngestContractModel ingestContract = ingestContext.getIngestContract();
        if (ingestContract != null) {
            LOGGER.debug("contract name  is: " + ingestContract.getIdentifier());
            rightsStatementIdentifier.put(ARCHIVAl_AGREEMENT, ingestContract.getIdentifier());
        }
        final String archivalProfile = ingestContext.getArchivalProfile();
        if (archivalProfile != null) {
            LOGGER.debug("archivalProfile  is: " + archivalProfile);
            rightsStatementIdentifier.put(ARCHIVAl_PROFIL, archivalProfile);
        }
        return rightsStatementIdentifier;
    }

    private static ObjectNode buildAgIdExt(IngestContext ingestContext) {
        final ObjectNode agIdExt = JsonHandler.createObjectNode();

        final String originatingAgency = ingestContext.getOriginatingAgency();
        if (originatingAgency != null) {
            LOGGER.debug("supplier service is: " + originatingAgency);
            agIdExt.put(ORIGIN_ANGENCY_NAME, originatingAgency);
        }
        final String transferringAgency = ingestContext.getTransferringAgency();
        if (transferringAgency != null) {
            LOGGER.debug("Find a transfAgency: " + transferringAgency);
            agIdExt.put(TRANSFER_AGENCY, transferringAgency);
        }
        final String archivalAgency = ingestContext.getArchivalAgency();
        if (archivalAgency != null) {
            LOGGER.debug("Find a archivalAgency: " + archivalAgency);
            agIdExt.put(ARCHIVAL_AGENCY, archivalAgency);
        }
        final String submissionAgencyIdentifier = ingestContext.getSubmissionAgencyIdentifier();
        if (submissionAgencyIdentifier != null) {
            LOGGER.debug("Find a submissionAgencyIdentifier: " + submissionAgencyIdentifier);
            agIdExt.put(ORIGIN_ANGENCY_SUBMISSION, submissionAgencyIdentifier);
        }
        return agIdExt;
    }

    private void updateItemStatusForManifestReferenceException(ItemStatus globalCompositeItemStatus,
        ProcessingManifestReferenceException e) {
        String message;
        String key = null;
        ObjectNode error = JsonHandler.createObjectNode();
        ObjectNode errorDetail = JsonHandler.createObjectNode();

        if (e.getType() == ExceptionType.UNIT) {
            errorDetail.put("ManifestUnitId", e.getManifestId());
            errorDetail.put(SedaConstants.TAG_ARCHIVE_SYSTEM_ID, e.getUnitGuid());
            errorDetail.put("ParentUnitId", e.getUnitParentId());
            errorDetail.put("Message", e.getMessage());
            error.set(e.getManifestId(), errorDetail);
            message = JsonHandler.unprettyPrint(error);
            key = SUBTASK_MODIFY_PARENT_EXISTING_UNIT_UNAUTHORIZED;
        } else {
            errorDetail.put("ManifestGotId", e.getManifestId());
            errorDetail.put("Message", e.getMessage());
            error.set(e.getManifestId(), errorDetail);
            message = JsonHandler.unprettyPrint(error);
        }

        updateDetailItemStatus(globalCompositeItemStatus, message, key);
    }

    private UnitType getUnitType(HandlerIO handlerIO) {
        return UnitType.valueOf(UnitType.getUnitTypeString((String) handlerIO.getInput(UNIT_TYPE_INPUT_RANK)));
    }

    private String getMessageItemStatusAUDeclaringObject(final String unitId, final String bdoId, final String gotId) {
        ObjectNode error = JsonHandler.createObjectNode();
        error.put(SedaConstants.TAG_ARCHIVE_UNIT, unitId);
        error.put(SedaConstants.TAG_BINARY_DATA_OBJECT_ID, bdoId);
        error.put(SedaConstants.TAG_DATA_OBJECT_GROUPE_ID, gotId);
        return JsonHandler.unprettyPrint(error);
    }

    private String getMessageItemStatusGOTEveryDataObjectVersion(final String unitId, String objectGroupId) {
        ObjectNode error = JsonHandler.createObjectNode();
        error.put(SedaConstants.TAG_ARCHIVE_UNIT, unitId);
        error.put(SedaConstants.TAG_DATA_OBJECT_REFERENCEID, objectGroupId);
        return JsonHandler.unprettyPrint(error);
    }

    private String getMessageItemStatusGOTMasterMandatory(final String gotId) {
        ObjectNode error = JsonHandler.createObjectNode();
        error.put(SedaConstants.TAG_DATA_OBJECT_GROUPE_ID, gotId);
        return JsonHandler.unprettyPrint(error);
    }

    private String getMessageItemStatusAUNotFound(final String unitId, String unitGuid, boolean isGuid) {
        if (isGuid) {
            unitGuid =
                "[MetadataName:" + ParserTokens.PROJECTIONARGS.ID.exactToken() + ", MetadataValue : " + unitGuid + "]";

        }
        ObjectNode error = JsonHandler.createObjectNode();
        ObjectNode errorDetail = JsonHandler.createObjectNode();
        errorDetail.put(SedaConstants.TAG_ARCHIVE_UNIT, unitGuid);
        error.set(unitId, errorDetail);
        return JsonHandler.unprettyPrint(error);
    }

    private String getMessageItemStatusGOTLinkingException(final String unitGuid, final String got) {
        ObjectNode error = JsonHandler.createObjectNode();
        error.put(SedaConstants.TAG_ARCHIVE_UNIT, unitGuid);
        error.put(SedaConstants.TAG_DATA_OBJECT_REFERENCEID, got);
        return JsonHandler.unprettyPrint(error);
    }

    private String getMessageItemStatusAULinkingException(ProcessingUnitLinkingException e) {
        ObjectNode error = JsonHandler.createObjectNode();
        error.put(SedaConstants.TAG_ARCHIVE_UNIT, e.getManifestId());
        if (e.getUnitType() != null) {
            error.put("ExistingUnitType", e.getUnitType().name());
        }
        error.put("IngestUnitType", e.getIngestType().name());
        error.put("message", e.getMessage());

        return JsonHandler.unprettyPrint(error);
    }

    private String getMessageItemStatusOGNotFound(final String unitId, String objectGroupGuid, boolean isGuid) {
        if (isGuid) {
            objectGroupGuid = "[MetadataName:" + ParserTokens.PROJECTIONARGS.ID.exactToken() + ", MetadataValue : " +
                objectGroupGuid + "]";

        }
        ObjectNode error = JsonHandler.createObjectNode();
        ObjectNode errorDetail = JsonHandler.createObjectNode();
        errorDetail.put(SedaConstants.TAG_DATA_OBJECT_GROUP, objectGroupGuid);
        error.set(unitId, errorDetail);
        return JsonHandler.unprettyPrint(error);
    }


    /**
     * Split Element from InputStream and write it to workspace
     *
     * @param handlerIO
     * @param unmarshaller
     * @param ingestContext the ingest context
     * @param globalCompositeItemStatus the global status
     * @throws ProcessingException throw when can't read or extract element from SEDA
     * @throws CycleFoundException when a cycle is found in data extract
     */
    private ObjectNode extractSEDA(HandlerIO handlerIO, Unmarshaller unmarshaller, IngestContext ingestContext,
        IngestSession ingestSession, JsonLineDataBase unitsDatabase, JsonLineDataBase objectsDatabase,
        ItemStatus globalCompositeItemStatus)
        throws ProcessingException, CycleFoundException {
        ParametersChecker.checkParameter("ContainerId is a mandatory parameter", ingestContext);
        ParametersChecker.checkParameter("itemStatus is a mandatory parameter", globalCompositeItemStatus);

        /**
         * Retrieves SEDA
         **/
        final XMLInputFactory xmlInputFactory = XMLInputFactoryUtils.newInstance();
        XMLEventReader reader = null;

        final QName dataObjectGroupName = new QName(namespaceURI, DATA_OBJECT_GROUP);
        final QName dataObjectName = new QName(namespaceURI, BINARY_DATA_OBJECT);
        final QName physicalDataObjectName = new QName(namespaceURI, PHYSICAL_DATA_OBJECT);
        final QName unitName = new QName(namespaceURI, ARCHIVE_UNIT);

        try (InputStream xmlFile = getTransformedXmlAsInputStream(handlerIO)) {
            reader = xmlInputFactory.createXMLEventReader(xmlFile);
            final JsonXMLConfig config =
                new JsonXMLConfigBuilder().autoArray(true).autoPrimitive(true).prettyPrint(true)
                    .namespaceDeclarations(false).build();
            // This file will be a JSON representation of the SEDA manifest with an empty DataObjectPackage structure
            final File globalSedaParametersFile =
                handlerIO.getNewLocalFile(handlerIO.getOutput(GLOBAL_SEDA_PARAMETERS_FILE_IO_RANK).getPath());
            final FileWriter tmpFileWriter = new FileWriter(globalSedaParametersFile);
            final XMLEventFactory eventFactory = XMLEventFactory.newInstance();
            final XMLEventWriter writer = new JsonXMLOutputFactory(config).createXMLEventWriter(tmpFileWriter);
            writer.add(eventFactory.createStartDocument());
            boolean globalMetadata = true;

            // Retrieve storage info
            final JsonNode storageInfo = JsonHandler.getFromFile((File) handlerIO.getInput(STORAGE_INFO_INPUT_RANK));

            JsonNode storageUnitInfo = storageInfo.get(VitamConfiguration.getDefaultStrategy());
            JsonNode storageObjectGroupInfo = storageInfo.get(VitamConfiguration.getDefaultStrategy());
            JsonNode storageObjectInfo = storageInfo.get(VitamConfiguration.getDefaultStrategy());
            if (ingestContext.getManagementContractModel() != null &&
                ingestContext.getManagementContractModel().getStorage() != null) {
                if (StringUtils.isNotBlank(ingestContext.getManagementContractModel().getStorage().getUnitStrategy())) {
                    storageUnitInfo =
                        storageInfo.get(ingestContext.getManagementContractModel().getStorage().getUnitStrategy());
                }
                if (StringUtils.isNotBlank(
                    ingestContext.getManagementContractModel().getStorage().getObjectGroupStrategy())) {
                    storageObjectGroupInfo = storageInfo.get(
                        ingestContext.getManagementContractModel().getStorage().getObjectGroupStrategy());
                }
                if (StringUtils.isNotBlank(
                    ingestContext.getManagementContractModel().getStorage().getObjectStrategy())) {
                    storageObjectInfo =
                        storageInfo.get(ingestContext.getManagementContractModel().getStorage().getObjectStrategy());
                }
            }

            if (storageUnitInfo == null || storageObjectGroupInfo == null || storageObjectInfo == null) {
                LOGGER.error(MISSING_STORAGE_INFO);
                throw new ProcessingException(MISSING_STORAGE_INFO);
            }

            ObjectNode evDetData = JsonHandler.createObjectNode();

            Stopwatch xmlParserStopwatch = Stopwatch.createStarted();

            while (true) {
                final XMLEvent event = reader.peek();
                if (event.isStartElement() && event.asStartElement().getName().equals(unitName)) {
                    extractMetadataUsingMarshellar(reader, unmarshaller, handlerIO, ingestContext, ingestSession,
                        ArchiveUnitType.class);
                    continue;
                }
                if (event.isStartElement() && event.asStartElement().getName().equals(dataObjectGroupName)) {
                    extractMetadataUsingMarshellar(reader, unmarshaller, handlerIO, ingestContext, ingestSession,
                        DataObjectGroupType.class);
                    continue;
                }

                if (event.isStartElement() && event.asStartElement().getName().equals(dataObjectName)) {
                    extractMetadataUsingMarshellar(reader, unmarshaller, handlerIO, ingestContext, ingestSession,
                        BinaryDataObjectType.class);
                    continue;
                }
                if (event.isStartElement() && event.asStartElement().getName().equals(physicalDataObjectName)) {
                    extractMetadataUsingMarshellar(reader, unmarshaller, handlerIO, ingestContext, ingestSession,
                        PhysicalDataObjectType.class);
                    continue;
                }

                reader.nextEvent();

                // extract info for ATR
                // The DataObjectPackage EndElement is tested before the add condition as we need to add a empty
                // DataObjectPackage endElement event
                if (event.isEndElement() && event.asEndElement().getName().getLocalPart().equals(DATAOBJECT_PACKAGE)) {
                    globalMetadata = true;
                }

                if (event.isStartElement() &&
                    event.asStartElement().getName().getLocalPart().equals(SedaConstants.TAG_ARCHIVAL_AGREEMENT)) {
                    String ingestContractIdentifier = reader.getElementText();
                    updateGlobalSedaFile(writer, eventFactory, SedaConstants.TAG_ARCHIVAL_AGREEMENT,
                        ingestContractIdentifier);
                    continue;
                }

                if (event.isStartElement() &&
                    event.asStartElement().getName().getLocalPart().equals(SedaConstants.TAG_ARCHIVE_PROFILE)) {
                    updateGlobalSedaFile(writer, eventFactory, SedaConstants.TAG_ARCHIVE_PROFILE,
                        reader.getElementText());
                    continue;
                }

                if (event.isStartElement() && event.asStartElement().getName().getLocalPart()
                    .equals(SedaConstants.TAG_ORIGINATINGAGENCYIDENTIFIER)) {

                    if (!UnitType.HOLDING_UNIT.equals(ingestContext.getWorkflowUnitType())) {
                        String originatingAgency = reader.getElementText();
                        ingestContext.setOriginatingAgency(originatingAgency);
                        ingestSession.getOriginatingAgencies().add(originatingAgency);
                        updateGlobalSedaFile(writer, eventFactory, SedaConstants.TAG_ORIGINATINGAGENCYIDENTIFIER,
                            originatingAgency);
                        for (String currentAgency : ingestSession.getOriginatingAgencies()) {
                            updateGlobalSedaFile(writer, eventFactory, SedaConstants.TAG_ORIGINATINGAGENCYIDENTIFIERS,
                                currentAgency);
                        }
                    }
                    globalMetadata = false;
                }

                // Bug #2324 - lets check the serviceLevel value
                if (event.isStartElement() &&
                    event.asStartElement().getName().getLocalPart().equals(SedaConstants.TAG_SERVICE_LEVEL)) {
                    final String serviceLevel = reader.getElementText();
                    updateGlobalSedaFile(writer, eventFactory, SedaConstants.TAG_SERVICE_LEVEL, serviceLevel);
                    globalMetadata = false;
                }

                if (event.isStartElement() &&
                    event.asStartElement().getName().getLocalPart().equals(SedaConstants.TAG_ACQUISITIONINFORMATION)) {
                    final String acquisitionInformation = reader.getElementText();
                    updateGlobalSedaFile(writer, eventFactory, SedaConstants.TAG_ACQUISITIONINFORMATION,
                        acquisitionInformation);
                    globalMetadata = false;
                }

                if (event.isStartElement() &&
                    event.asStartElement().getName().getLocalPart().equals(SedaConstants.TAG_LEGALSTATUS)) {
                    final String legalStatus = reader.getElementText();
                    updateGlobalSedaFile(writer, eventFactory, SedaConstants.TAG_LEGALSTATUS, legalStatus);
                    globalMetadata = false;
                }

                if (event.isStartElement() && event.asStartElement().getName().getLocalPart()
                    .equals(SedaConstants.TAG_SUBMISSIONAGENCYIDENTIFIER)) {
                    String submissionAgencyIdentifier = reader.getElementText();
                    ingestContext.setSubmissionAgencyIdentifier(submissionAgencyIdentifier);
                    updateGlobalSedaFile(writer, eventFactory, SedaConstants.TAG_SUBMISSIONAGENCYIDENTIFIER,
                        submissionAgencyIdentifier);
                    globalMetadata = false;
                }

                if (event.isStartElement() &&
                    event.asStartElement().getName().getLocalPart().equals(SedaConstants.TAG_RULE_NEED_AUTHORISATION)) {
                    String globalNeedAuthorization = reader.getElementText();
                    ingestContext.setGlobalNeedAuthorization(globalNeedAuthorization);
                    globalMetadata = false;
                }

                // Process rules : build mgtRulesMap
                if (event.isStartElement() &&
                    SedaConstants.getSupportedRules().contains(event.asStartElement().getName().getLocalPart())) {
                    final StartElement element = event.asStartElement();
                    parseMetadataManagementRules(ingestSession, reader, element,
                        event.asStartElement().getName().getLocalPart());
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


                if (event.isEndDocument()) {
                    break;
                }
            }

            boolean attachmentsNotAuthorizedByIngestContract = ingestContext.getIngestContract() != null &&
                IngestContractCheckState.REQUIRED.equals(ingestContext.getIngestContract().getCheckParentLink()) &&
                ingestSession.getExistingUnitGuids().isEmpty();
            if (attachmentsNotAuthorizedByIngestContract) {
                throw new ProcessingAttachmentRequiredException(
                    "ingest contract requires at least one existing archive unit to attach, but not found in manifest");
            }

            long elapsed = xmlParserStopwatch.elapsed(TimeUnit.MILLISECONDS);

            PERFORMANCE_LOGGER.log("STP_INGEST_CONTROL_SIP", "CHECK_DATAOBJECTPACKAGE", "extractSeda.xml.parse",
                elapsed);

            writer.add(eventFactory.createEndDocument());
            writer.close();

            // save maps
            saveGuids(handlerIO, ingestSession);

            // Fill evDetData EvDetailReq, ArchivalAgreement, ArchivalProfile and ServiceLevel properties
            try {
                JsonNode metadataAsJson = JsonHandler.getFromFile(globalSedaParametersFile).get(TAG_ARCHIVE_TRANSFER);

                JsonNode comments = metadataAsJson.get(SedaConstants.TAG_COMMENT);

                if (comments != null && comments.isArray()) {
                    ArrayNode commentsArray = (ArrayNode) comments;
                    for (JsonNode node : commentsArray) {
                        String comment;
                        String lang = null;
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

                        JsonNode oldCommentCurrentLang =
                            evDetData.get(EV_DETAIL_REQ + (ParametersChecker.isNotEmpty(lang) ? "_" + lang : ""));
                        if (oldCommentCurrentLang != null) {
                            comment = oldCommentCurrentLang.asText() + "_" + comment;
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
                    evDetData.put("EvDateTimeReq", date.asText().trim());
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
                        String transferringAgency = identifier.asText();
                        ingestContext.setTransferringAgency(transferringAgency);
                    }
                }
                JsonNode archivalAgencyContent = metadataAsJson.get(SedaConstants.TAG_ARCHIVAL_AGENCY);
                if (archivalAgencyContent != null) {
                    JsonNode identifier = archivalAgencyContent.get(SedaConstants.TAG_IDENTIFIER);
                    if (identifier != null) {
                        String archivalAgency = identifier.asText();
                        ingestContext.setArchivalAgency(archivalAgency);
                    }
                }
                JsonNode dataObjPack = metadataAsJson.get(SedaConstants.TAG_DATA_OBJECT_PACKAGE);
                if (dataObjPack != null) {
                    JsonNode serviceLevel = dataObjPack.get(SedaConstants.TAG_SERVICE_LEVEL);

                    JsonNode archivalProfileElement = dataObjPack.get(SedaConstants.TAG_ARCHIVE_PROFILE);
                    if (archivalProfileElement != null) {
                        LOGGER.debug("Find an archival profile: " + archivalProfileElement.asText());
                        evDetData.put(SedaConstants.TAG_ARCHIVE_PROFILE, archivalProfileElement.asText());
                        String archivalProfile = archivalProfileElement.asText();
                        ingestContext.setArchivalProfile(archivalProfile);
                    }
                    if (serviceLevel != null) {
                        LOGGER.debug("Find a service Level: " + serviceLevel);
                        evDetData.put("ServiceLevel", serviceLevel.asText());
                    } else {
                        LOGGER.debug("Put a null ServiceLevel (No service Level)");
                        evDetData.set("ServiceLevel", null);
                    }
                    JsonNode acquisitionInformation = dataObjPack.get(SedaConstants.TAG_ACQUISITIONINFORMATION);
                    if (acquisitionInformation != null) {
                        LOGGER.debug("Find AcquisitionInformation : " + acquisitionInformation);
                        evDetData.put(SedaConstants.TAG_ACQUISITIONINFORMATION, acquisitionInformation.asText());
                    }

                    JsonNode legalStatus = dataObjPack.get(SedaConstants.TAG_LEGALSTATUS);
                    if (legalStatus != null) {
                        LOGGER.debug("Find legalStatus : " + legalStatus);
                        evDetData.put(SedaConstants.TAG_LEGALSTATUS, legalStatus.asText());
                    }


                } else {
                    LOGGER.debug("Put a null ServiceLevel (No Data Object Package)");
                    evDetData.set("ServiceLevel", null);
                }

            } catch (InvalidParseOperationException e) {
                LOGGER.error("Can't parse globalSedaPareters", e);
                throw new ProcessingException(e);
            }

            String evDetDataJson = JsonHandler.unprettyPrint(evDetData);

            // 2-detect cycle : if graph has a cycle throw CycleFoundException
            // Define Treatment DirectedCycle detection

            Stopwatch checkCycle = Stopwatch.createStarted();

            checkCycle(handlerIO, ingestContext, ingestSession, evDetDataJson);

            PERFORMANCE_LOGGER.log("STP_INGEST_CONTROL_SIP", "CHECK_DATAOBJECTPACKAGE", "extractSeda.checkCycle",
                checkCycle.elapsed(TimeUnit.MILLISECONDS));

            // 2- create graph and create level
            // Define Treatment Graph and Level Creation
            createIngestLevelStackFile(handlerIO, ingestSession,
                new Graph(ingestSession.getArchiveUnitTree()).getGraphWithLongestPaths(),
                GRAPH_WITH_LONGEST_PATH_IO_RANK);

            checkArchiveUnitIdReference(ingestSession, evDetDataJson);

            Stopwatch saveObjectGroupToWorkspaceStopWatch = Stopwatch.createStarted();

            checkMasterIsMandatoryAndCheckCanAddObjectToExistingObjectGroup(ingestSession,
                ingestContext.getIngestContract());
            saveObjectGroupsToWorkspace(handlerIO, ingestContext, ingestSession, objectsDatabase,
                storageObjectGroupInfo,
                storageObjectInfo);

            PERFORMANCE_LOGGER.log("STP_INGEST_CONTROL_SIP", "CHECK_DATAOBJECTPACKAGE", "extractSeda.saveObjectGroup",
                saveObjectGroupToWorkspaceStopWatch.elapsed(TimeUnit.MILLISECONDS));


            // Add parents to archive units and save them into workspace

            Stopwatch saveArchiveUnitStopWatch = Stopwatch.createStarted();

            finalizeAndSaveArchiveUnitToWorkspace(handlerIO, ingestContext, ingestSession,
                unitsDatabase,
                storageUnitInfo);

            PERFORMANCE_LOGGER.log("STP_INGEST_CONTROL_SIP", "CHECK_DATAOBJECTPACKAGE", "extractSeda.saveArchiveUnit",
                saveArchiveUnitStopWatch.elapsed(TimeUnit.MILLISECONDS));

            handlerIO.addOutputResult(GLOBAL_SEDA_PARAMETERS_FILE_IO_RANK, globalSedaParametersFile, false, asyncIO);

            return evDetData;
        } catch (final XMLStreamException | InvalidParseOperationException | TransformerException e) {
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
            throw e;
        } catch (final IOException e) {
            LOGGER.error(SAVE_ARCHIVE_ID_TO_GUID_IOEXCEPTION_MSG, e);
            throw new ProcessingException(e);
        } catch (final ArchiveUnitContainDataObjectException e) {
            LOGGER.error("Archive Unit Reference to BDO", e);
            throw e;
        } catch (ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException e) {
            LOGGER.error(MANIFEST_NOT_FOUND);
            throw new ProcessingException(e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (final XMLStreamException e) {
                    SysErrLogger.FAKE_LOGGER.ignoreLog(e);
                }
            }
        }
    }

    private void updateGlobalSedaFile(XMLEventWriter writer, XMLEventFactory eventFactory, String key, String value)
        throws XMLStreamException {
        writer.add(eventFactory.createStartElement("", namespaceURI, key));
        writer.add(eventFactory.createCharacters(value));
        writer.add(eventFactory.createEndElement("", namespaceURI, key));
    }

    /**
     * Transform xml file to a clean xml file (whithout comments and indent)
     *
     * @param handlerIO used to get xml file
     * @return xml file as InputStream
     * @throws TransformerException, IOException, ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException
     */
    @Nonnull
    private InputStream getTransformedXmlAsInputStream(HandlerIO handlerIO)
        throws TransformerException, IOException, ContentAddressableStorageNotFoundException,
        ContentAddressableStorageServerException {

        File xmlFile = handlerIO.getFileFromWorkspace(SEDA_FOLDER + "/" + SEDA_FILE);
        File cleanManifest = handlerIO.getNewLocalFile("_" + SEDA_FILE);
        Source xsl = new StreamSource(PropertiesUtils.getResourceAsStream(TRANSFORM_XSLT_PATH));

        Transformer transformer = transformerFactory.newTransformer(xsl);
        transformer.setErrorListener(new ErrorListener() {
            @Override
            public void warning(TransformerException exception) {
                LOGGER.warn("An error occurred while processing SEDA transformation", exception);
            }

            @Override
            public void error(TransformerException exception) throws TransformerException {
                throw exception;
            }

            @Override
            public void fatalError(TransformerException exception) throws TransformerException {
                throw exception;
            }
        });

        transformer.transform(new StreamSource(xmlFile), new StreamResult(cleanManifest));

        return new FileInputStream(cleanManifest);
    }

    private void handleJaxbUnmarshalRuntimeException(HandlerIO handlerIO, IngestContext ingestContext,
        IngestSession ingestSession, RuntimeException e)
        throws IOException, ProcessingException, InvalidParseOperationException, LogbookClientNotFoundException,
        LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException {
        if (e.getCause() instanceof ProcessingNotFoundException) {
            ProcessingNotFoundException exception = (ProcessingNotFoundException) e.getCause();

            if (exception.getType() == ExceptionType.UNIT && exception.isValidGuid()) {
                ingestSession.getUnitIdToGuid().put(exception.getManifestId(), exception.getGuid());
                ingestSession.getGuidToUnitId().put(exception.getGuid(), exception.getManifestId());
                saveGuids(handlerIO, ingestSession);

                createLifeCycleForError(ingestSession.getGuidToLifeCycleParameters(), exception.getTaskKey(),
                    getMessageItemStatusAUNotFound(exception.getManifestId(), exception.getGuid(),
                        exception.isValidGuid()), exception.getGuid(), true, false, ingestContext.getOperationId(),
                    ingestContext.getTypeProcess());

                throw exception;
            }

            if (exception.getType() == ExceptionType.GOT) {
                ingestSession.getDataObjectIdToGuid().put(exception.getGuid(), exception.getGuid());
                saveGuids(handlerIO, ingestSession);
                createLifeCycleForError(ingestSession.getGuidToLifeCycleParameters(), exception.getTaskKey(),
                    getMessageItemStatusOGNotFound(exception.getManifestId(), exception.getGuid(),
                        exception.isValidGuid()), exception.getGuid(), false, true, ingestContext.getOperationId(),
                    ingestContext.getTypeProcess());
                throw exception;
            }
        }

        if (e.getCause() instanceof ProcessingException) {
            throw (ProcessingException) e.getCause();
        }
        throw e;
    }

    private void saveGuids(HandlerIO handlerIO, IngestSession ingestSession) throws IOException, ProcessingException {
        // Save DataObjectIdToGuid Map
        HandlerUtils.saveMap(handlerIO, ingestSession.getDataObjectIdToGuid(), DO_ID_TO_GUID_IO_RANK, true, asyncIO);
        // Save objectGroupIdToUnitId Map
        handlerIO.addOutputResult(OG_ID_TO_UNID_ID_IO_RANK, ingestSession.getObjectGroupIdToUnitId(), asyncIO);
        // Save dataObjectIdToDetailDataObject Map
        HandlerUtils.saveMap(handlerIO, ingestSession.getDataObjectIdToDetailDataObject(), BDO_ID_TO_VERSION_DO_IO_RANK,
            true, asyncIO);
        // Save unitIdToGuid Map post unmarshalling
        HandlerUtils.saveMap(handlerIO, ingestSession.getUnitIdToGuid(), UNIT_ID_TO_GUID_IO_RANK, true, asyncIO);
        // Save guidToUnitId Map post unmarshalling
        HandlerUtils.saveMap(handlerIO, ingestSession.getGuidToUnitId(), GUID_TO_UNIT_ID_IO_RANK, true, asyncIO);

        HandlerUtils.saveMap(handlerIO, ingestSession.getExistingGOTGUIDToNewGotGUIDInAttachment(),
            EXISTING_GOT_TO_NEW_GOT_GUID_FOR_ATTACHMENT_RANK, true, asyncIO);

        HandlerUtils.saveSet(handlerIO, ingestSession.getExistingUnitGuids(), EXISTING_UNITS_GUID_FOR_ATTACHMENT_RANK,
            true, asyncIO);

        HandlerUtils.saveSet(handlerIO, ingestSession.getExistingGOTs().keySet(),
            EXISTING_GOTS_GUID_FOR_ATTACHMENT_RANK, true, asyncIO);
    }

    /**
     * @param handlerIO
     * @param evDetData
     * @throws CycleFoundException
     * @throws LogbookClientNotFoundException
     * @throws InvalidParseOperationException
     * @throws LogbookClientBadRequestException
     * @throws LogbookClientServerException
     */
    private void checkCycle(HandlerIO handlerIO, IngestContext ingestContext, IngestSession ingestSession,
        String evDetData) throws CycleFoundException, LogbookClientNotFoundException, InvalidParseOperationException,
        LogbookClientBadRequestException, LogbookClientServerException {
        final DirectedGraph directedGraph = new DirectedGraph(ingestSession.getArchiveUnitTree());
        final DirectedCycle directedCycle = new DirectedCycle(directedGraph);
        if (!directedCycle.isCyclic()) {
            return;
        }

        String cycleMessage = null;
        if (directedCycle.getCycle() != null && !directedCycle.getCycle().isEmpty()) {
            if (directedCycle.getCycle().size() <= 20) {
                List<String> cycle = new ArrayList<>();
                for (Integer index : directedCycle.getCycle()) {
                    cycle.add(directedGraph.getId(index));
                }
                cycleMessage = "Cycle : " + Arrays.toString(cycle.toArray());
            }

            // update lifecycle of the first node
            String unitGuid = ingestSession.getUnitIdToGuid().get(directedGraph.getId(directedCycle.getCycle().get(0)));
            final LogbookLifeCycleParameters llcp = ingestSession.getGuidToLifeCycleParameters().get(unitGuid);
            llcp.setFinalStatus(SUBTASK_LOOP, null, StatusCode.KO, null);
            ObjectNode llcEvDetData = JsonHandler.createObjectNode();
            llcEvDetData.put(SedaConstants.EV_DET_TECH_DATA, cycleMessage);
            String wellFormedJson = JsonHandler.writeAsString(llcEvDetData);
            llcp.putParameterValue(LogbookParameterName.eventDetailData, wellFormedJson);
            handlerIO.getHelper().updateDelegate(llcp);
            bulkLifeCycleUnit(handlerIO, ingestContext.getOperationId(), Lists.newArrayList(unitGuid));
        }
        throw new CycleFoundException(GRAPH_CYCLE_MSG, cycleMessage, evDetData);

    }

    private void parseMetadataManagementRules(IngestSession ingestSession, XMLEventReader reader, StartElement element,
        String currentRuleInProcess) throws ProcessingException {
        try {
            StringWriter stringWriterRule = new StringWriter();
            final JsonXMLConfig config =
                new JsonXMLConfigBuilder().autoArray(true).autoPrimitive(true).prettyPrint(true)
                    .namespaceDeclarations(false).build();
            final XMLOutputFactory xmlOutputFactory = new JsonXMLOutputFactory(config);
            XMLEventWriter xw = xmlOutputFactory.createXMLEventWriter(stringWriterRule);
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
                    // use temporary id (avoid using null key for different rule category)
                    ingestSession.getMngtMdRuleIdToRulesXml()
                        .put(Objects.requireNonNullElse(currentRuleId, currentRuleInProcess), stringWriterRule);
                    stringWriterRule.close();
                    break;
                }

                if (event.isStartElement() &&
                    SedaConstants.TAG_RULE_RULE.equals(event.asStartElement().getName().getLocalPart())) {

                    // A new rule was found => close the current stringWriterRule and add it to map
                    if (currentRuleId != null) {
                        xw.add(eventFactory.createEndElement("", "", GLOBAL_MGT_RULE_TAG));
                        xw.add(eventFactory.createEndDocument());
                        ingestSession.getMngtMdRuleIdToRulesXml().put(currentRuleId, stringWriterRule);
                        stringWriterRule.close();

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

    private void finalizeAndSaveArchiveUnitToWorkspace(HandlerIO handlerIO, IngestContext ingestContext,
        IngestSession ingestSession, JsonLineDataBase unitsDatabase, JsonNode storageUnitInfo)
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException,
        ProcessingException, InvalidParseOperationException {

        // Finalize Archive units extraction process
        if (ingestSession.getUnitIdToGuid() == null) {
            return;
        }

        List<String> uuids = new ArrayList<>();

        for (final Entry<String, String> element : ingestSession.getUnitIdToGuid().entrySet()) {

            final String unitGuid = element.getValue();
            // Do not treat LFC of existing ObjectGroup
            if (ingestSession.getExistingUnitGuids().contains(unitGuid)) {
                continue;
            }
            final String manifestUnitId = element.getKey();
            boolean isRootArchive = true;
            List<LogbookEvent> logbookLifeCycle = null;

            // 1- create Unit life cycles
            createUnitLifeCycle(handlerIO, ingestSession, unitGuid, ingestContext.getOperationId());

            // 2- Update temporary files
            final File unitCompleteTmpFile = handlerIO.getNewLocalFile(unitGuid);

            // Get the archiveUnit
            ObjectNode archiveUnit = (ObjectNode) unitsDatabase.read(unitGuid);

            JsonNode logbookLifeCycleAsNode = archiveUnit.get("LogbookLifeCycleExternal");
            if (logbookLifeCycleAsNode != null) {
                logbookLifeCycle = JsonHandler.getFromJsonNode(logbookLifeCycleAsNode, LIST_TYPE_REFERENCE);
            }

            // Management rules id to add
            Set<String> globalMgtIdExtra = new HashSet<>();

            // Add storage information to archive unit
            addStorageInformation(archiveUnit, storageUnitInfo);

            addValidComputedInheritedRulesInformation(ingestContext, archiveUnit);

            isRootArchive =
                attachmentByIngestContractAndManageRulesInformation(ingestContext, ingestSession, archiveUnit,
                    manifestUnitId, unitGuid, globalMgtIdExtra);

            updateManagementAndAppendGlobalMgtRule(ingestContext, ingestSession, archiveUnit, globalMgtIdExtra,
                isRootArchive);

            if (ingestSession.getIsThereManifestRelatedReferenceRemained().get(manifestUnitId) != null &&
                ingestSession.getIsThereManifestRelatedReferenceRemained().get(manifestUnitId)) {
                postReplaceInternalReferenceForRelatedObjectReference(ingestSession, archiveUnit);
            }
            // Write to new File
            JsonHandler.writeAsFile(archiveUnit, unitCompleteTmpFile);
            // Write to workspace
            handlerIO.transferFileToWorkspace(
                IngestWorkflowConstants.ARCHIVE_UNIT_FOLDER + File.separator + unitGuid + JSON_EXTENSION,
                unitCompleteTmpFile, true, asyncIO);

            // 3- Update created Unit life cycles
            addFinalStatusToUnitLifeCycle(handlerIO, ingestContext, ingestSession, unitGuid, manifestUnitId,
                isRootArchive);

            if (logbookLifeCycle != null) {
                createExternalLifeCycleLogbook(handlerIO, logbookLifeCycle, ingestContext.getOperationId(), unitGuid);
            }

            uuids.add(unitGuid);

            if (uuids.size() == BATCH_SIZE) {
                bulkLifeCycleUnit(handlerIO, ingestContext.getOperationId(), uuids);
                uuids.clear();
            }
        }

        // finish missing AU
        if (!uuids.isEmpty()) {
            bulkLifeCycleUnit(handlerIO, ingestContext.getOperationId(), uuids);
            uuids.clear();
        }
    }

    private void createExternalLifeCycleLogbook(HandlerIO handlerIO, List<LogbookEvent> externalModelEvents,
        String processId, String guid) throws LogbookClientNotFoundException, ProcessingMalformedDataException {
        LogbookLifeCycleParameters parent = createExternalParentLogbookLifeCycle(processId, guid);
        handlerIO.getHelper().updateDelegate(parent);

        for (LogbookEvent eventModel : externalModelEvents) {
            LogbookLifeCycleParameters external =
                toLogbookLifeCycleParameters(eventModel, parent.getParameterValue(eventIdentifier), processId, guid);
            handlerIO.getHelper().updateDelegateWithKey(guid, external);
        }
    }

    private LogbookLifeCycleParameters createExternalParentLogbookLifeCycle(String processId, String guid) {
        LogbookLifeCycleParameters parent =
            new LogbookLifeCycleParameters(LogbookParameterHelper.getDefaultLifeCycleMandatory());
        Map<String, String> parameters = new HashMap<>();
        parameters.put(eventIdentifier.name(), GUIDFactory.newGUID().getId());
        parameters.put(eventType.name(), "LFC.EXTERNAL_LOGBOOK");
        parameters.put(eventDateTime.name(), LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()));
        parameters.put(eventTypeProcess.name(), LogbookTypeProcess.INGEST.name());
        parameters.put(eventIdentifierProcess.name(), processId);
        parameters.put(outcome.name(), StatusCode.OK.name());
        parameters.put(outcomeDetail.name(), "LFC.EXTERNAL_LOGBOOK.OK");
        parameters.put(outcomeDetailMessage.name(),
            "Succès de la récupération des journaux de cycle de vie de l’archive transférée");
        parameters.put(objectIdentifier.name(), guid);
        parent.setMap(parameters);
        return parent;
    }

    private LogbookLifeCycleParameters toLogbookLifeCycleParameters(LogbookEvent eventModel, String parentId,
        String identifierProcess, String guid) throws ProcessingMalformedDataException {
        LogbookLifeCycleParameters logbookLifeCycleParameters =
            new LogbookLifeCycleParameters(Collections.singleton(LogbookParameterName.eventDateTime));

        checkEveDateTime(eventModel);

        Map<String, String> parameters = new HashMap<>();
        parameters.put(eventIdentifier.name(), eventModel.getEvId());
        parameters.put(parentEventIdentifier.name(),
            StringUtils.isBlank(eventModel.getEvParentId()) ? parentId : eventModel.getEvParentId());
        parameters.put(eventType.name(), eventModel.getEvType());
        parameters.put(eventDateTime.name(), eventModel.getEvDateTime());
        parameters.put(eventIdentifierProcess.name(),
            StringUtils.isBlank(eventModel.getEvIdProc()) ? identifierProcess : eventModel.getEvIdProc());
        parameters.put(eventTypeProcess.name(), eventModel.getEvTypeProc());
        parameters.put(outcome.name(), eventModel.getOutcome());
        parameters.put(outcomeDetail.name(), eventModel.getOutDetail());
        parameters.put(outcomeDetailMessage.name(), eventModel.getOutMessg());
        parameters.put(agentIdentifier.name(), eventModel.getAgId());
        parameters.put(objectIdentifier.name(),
            StringUtils.isBlank(eventModel.getObId()) ? guid : eventModel.getObId());

        logbookLifeCycleParameters.setMap(parameters);
        return logbookLifeCycleParameters;
    }

    private void checkEveDateTime(LogbookEvent eventModel) throws ProcessingMalformedDataException {
        String evDateTime = eventModel.getEvDateTime();
        LocalDateTime now = LocalDateUtil.now();
        LocalDateTime localDateTime =
            LocalDateUtil.parseMongoFormattedDate(Objects.requireNonNull(evDateTime, "EventDateTime cannot be null."));
        if (localDateTime.isAfter(now)) {
            throw new ProcessingMalformedDataException(
                String.format("EventDateTime in Logbook Event cannot be in the future, here '%s' is after '%s'.",
                    evDateTime, now));
        }
    }

    private void addStorageInformation(ObjectNode archiveUnit, JsonNode storageUnitInfo) {
        ObjectNode archiveUnitNode = (ObjectNode) archiveUnit.get(SedaConstants.TAG_ARCHIVE_UNIT);
        ObjectNode storage = JsonHandler.createObjectNode();
        storage.put(SedaConstants.STRATEGY_ID, storageUnitInfo.get(SedaConstants.STRATEGY_ID).asText());
        archiveUnitNode.set(SedaConstants.STORAGE, storage);
    }

    private void addValidComputedInheritedRulesInformation(IngestContext ingestContext, ObjectNode archiveUnit) {
        if (ingestContext.getIngestContract() != null &&
            ingestContext.getIngestContract().isComputeInheritedRulesAtIngest()) {
            ObjectNode archiveUnitNode = (ObjectNode) archiveUnit.get(SedaConstants.TAG_ARCHIVE_UNIT);
            archiveUnitNode.put(Unit.VALID_COMPUTED_INHERITED_RULES, false);
        }
    }

    /**
     * <p>
     * Finalize filling of sytemGUID for all reference items of RelatedObjectReference (RelationGroup) instead of
     * internal seda id (defined in manifest).<br>
     * not set yet by first pass call (one parsing) in method
     * {@see fr.gouv.vitam.worker.core.extractseda.ArchiveUnitListener#replaceInternalReferenceForRelatedObjectReference(String, DescriptiveMetadataModel)}
     * </p>
     *
     * @param archiveUnit
     * @throws InvalidParseOperationException if json serialization fail
     */
    private void postReplaceInternalReferenceForRelatedObjectReference(IngestSession ingestSession,
        ObjectNode archiveUnit) throws InvalidParseOperationException {

        ObjectNode archiveUnitNode = (ObjectNode) archiveUnit.get(SedaConstants.TAG_ARCHIVE_UNIT);
        RelatedObjectReferenceType archiveUnitRelatedObjectReference;

        if (archiveUnitNode.has(SedaConstants.TAG_RELATED_OBJECT_REFERENCE) &&
            archiveUnitNode.get(SedaConstants.TAG_RELATED_OBJECT_REFERENCE) instanceof ObjectNode) {
            archiveUnitRelatedObjectReference =
                JsonHandler.getFromJsonNode(archiveUnitNode.get(SedaConstants.TAG_RELATED_OBJECT_REFERENCE),
                    RelatedObjectReferenceType.class);

            fillArchiveUnitReference(ingestSession, archiveUnitRelatedObjectReference.getIsVersionOf());
            fillArchiveUnitReference(ingestSession, archiveUnitRelatedObjectReference.getReplaces());
            fillArchiveUnitReference(ingestSession, archiveUnitRelatedObjectReference.getRequires());
            fillArchiveUnitReference(ingestSession, archiveUnitRelatedObjectReference.getIsPartOf());
            fillArchiveUnitReference(ingestSession, archiveUnitRelatedObjectReference.getReferences());

            ObjectNode archiveUnitRelatedObjectReferenceNode =
                (ObjectNode) JsonHandler.toJsonNode(archiveUnitRelatedObjectReference);
            archiveUnitNode.set(SedaConstants.TAG_RELATED_OBJECT_REFERENCE, archiveUnitRelatedObjectReferenceNode);
        }

    }

    private void fillArchiveUnitReference(IngestSession ingestSession,
        List<DataObjectOrArchiveUnitReferenceType> dataObjectOrArchiveUnitReference) {

        for (DataObjectOrArchiveUnitReferenceType relatedObjectReferenceItem : dataObjectOrArchiveUnitReference) {

            String archiveUnitRefId = relatedObjectReferenceItem.getArchiveUnitRefId();

            if (archiveUnitRefId != null && (ingestSession.getUnitIdToGuid().containsKey(archiveUnitRefId))) {
                relatedObjectReferenceItem.setArchiveUnitRefId(ingestSession.getUnitIdToGuid().get(archiveUnitRefId));
            }

            String repositoryArchiveUnitPID = relatedObjectReferenceItem.getRepositoryArchiveUnitPID();
            if (repositoryArchiveUnitPID != null &&
                (ingestSession.getUnitIdToGuid().containsKey(repositoryArchiveUnitPID))) {
                relatedObjectReferenceItem.setRepositoryArchiveUnitPID(
                    ingestSession.getUnitIdToGuid().get(repositoryArchiveUnitPID));
            }

            String repositoryObjectPID = relatedObjectReferenceItem.getRepositoryObjectPID();
            if (repositoryObjectPID != null && (ingestSession.getUnitIdToGuid().containsKey(repositoryObjectPID))) {
                relatedObjectReferenceItem.setRepositoryObjectPID(
                    ingestSession.getUnitIdToGuid().get(repositoryObjectPID));
            }

            String externalReference = relatedObjectReferenceItem.getExternalReference();
            if (externalReference != null) {
                relatedObjectReferenceItem.setExternalReference(externalReference);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void bulkLifeCycleUnit(HandlerIO handlerIO, String containerId, List<String> uuids)
        throws LogbookClientBadRequestException, LogbookClientServerException {
        List<LogbookLifeCycleUnitModel> collect =
            uuids.stream().filter(value -> handlerIO.getHelper().containsUpdate(value)).map(
                    value -> new LogbookLifeCycleUnitModel(value,
                        (Queue<LogbookLifeCycleUnitParameters>) handlerIO.getHelper().removeUpdateDelegate(value)))
                .collect(Collectors.toList());
        try (LogbookLifeCyclesClient logbookLifeCycleClient = logbookLifeCyclesClientFactory.getClient()) {
            logbookLifeCycleClient.bulkUnit(containerId, collect);
        } catch (LogbookClientAlreadyExistsException e) {
            throw new VitamRuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private void bulkLifeCycleObjectGroup(HandlerIO handlerIO, String containerId, List<String> uuids)
        throws LogbookClientBadRequestException, LogbookClientServerException {
        List<LogbookLifeCycleObjectGroupModel> collect =
            uuids.stream().filter(value -> handlerIO.getHelper().containsCreate(value)).map(
                    value -> new LogbookLifeCycleObjectGroupModel(value,
                        (Queue<LogbookLifeCycleObjectGroupParameters>) handlerIO.getHelper().removeCreateDelegate(value)))
                .collect(Collectors.toList());
        try (LogbookLifeCyclesClient logbookLifeCycleClient = logbookLifeCyclesClientFactory.getClient()) {
            logbookLifeCycleClient.bulkObjectGroup(containerId, collect);
        } catch (LogbookClientAlreadyExistsException e) {
            throw new VitamRuntimeException(e);
        }
    }

    /**
     * Merge global rules to specific archive rules and clean management node
     *
     * @param ingestContext
     * @param ingestSession
     * @param archiveUnit archiveUnit
     * @param globalMgtIdExtra list of global management rule ids
     * @param isRootArchive true if the AU is root
     * @throws InvalidParseOperationException
     */
    private void updateManagementAndAppendGlobalMgtRule(IngestContext ingestContext, IngestSession ingestSession,
        ObjectNode archiveUnit, Set<String> globalMgtIdExtra, boolean isRootArchive)
        throws InvalidParseOperationException {

        ObjectNode archiveUnitNode = (ObjectNode) archiveUnit.get(SedaConstants.TAG_ARCHIVE_UNIT);
        ManagementModel archiveUnitManagementModel;

        if (archiveUnitNode.has(Unit.MANAGEMENT) && archiveUnitNode.get(Unit.MANAGEMENT) instanceof ObjectNode) {
            archiveUnitManagementModel =
                JsonHandler.getFromJsonNode(archiveUnitNode.get(Unit.MANAGEMENT), ManagementModel.class);
        } else {
            archiveUnitManagementModel = new ManagementModel();
        }
        for (final String ruleId : globalMgtIdExtra) {
            final StringWriter stringWriter = ingestSession.getMngtMdRuleIdToRulesXml().get(ruleId);
            JsonNode stringWriterNode = JsonHandler.getFromString(stringWriter.toString());
            JsonNode globalMgtRuleNode = stringWriterNode.get(GLOBAL_MGT_RULE_TAG);
            Iterator<String> ruleTypes = globalMgtRuleNode.fieldNames();

            while (ruleTypes.hasNext()) {
                String ruleType = ruleTypes.next();
                JsonNode globalMgtRuleTypeNode = globalMgtRuleNode.get(ruleType);
                if (globalMgtRuleTypeNode.isArray()) {
                    for (JsonNode globalMgtRuleTypeItemNode : globalMgtRuleTypeNode) {
                        mergeRule(globalMgtRuleTypeItemNode, archiveUnitManagementModel, ruleType);
                    }
                } else {
                    mergeRule(globalMgtRuleTypeNode, archiveUnitManagementModel, ruleType);
                }
            }
        }
        if (isRootArchive && archiveUnitManagementModel != null && ingestContext.getGlobalNeedAuthorization() != null) {
            if (archiveUnitManagementModel.isNeedAuthorization() == null) {
                archiveUnitManagementModel.setNeedAuthorization(
                    Boolean.valueOf(ingestContext.getGlobalNeedAuthorization()));
            }
        }
        ObjectNode archiveUnitMgtNode = (ObjectNode) JsonHandler.toJsonNode(archiveUnitManagementModel);
        if (archiveUnitMgtNode != null) {
            for (RuleType ruleType : RuleType.values()) {
                String name = ruleType.name();
                if (archiveUnitMgtNode.get(name) != null && archiveUnitMgtNode.get(name).get(RULES) != null &&
                    archiveUnitMgtNode.get(name).get(RULES).size() == 0) {
                    ObjectNode ruleNode = (ObjectNode) archiveUnitMgtNode.get(name);
                    ruleNode.remove(RULES);
                }
            }
        }
        archiveUnitNode.set(SedaConstants.PREFIX_MGT, archiveUnitMgtNode);
    }

    /**
     * Merge global management rule in root units management rules.
     *
     * @param globalMgtRuleNode global management node
     * @param archiveUnitManagementModel rule management model
     * @param ruleType category of rule
     * @throws InvalidParseOperationException
     */
    private void mergeRule(JsonNode globalMgtRuleNode, ManagementModel archiveUnitManagementModel, String ruleType)
        throws InvalidParseOperationException {
        RuleCategoryModel ruleCategoryModel = archiveUnitManagementModel.getRuleCategoryModel(ruleType);
        if (ruleCategoryModel == null) {
            ruleCategoryModel = new RuleCategoryModel();
        }
        if (ruleCategoryModel.isPreventInheritance()) {
            return;
        }

        RuleModel ruleModel = JsonHandler.getFromJsonNode(globalMgtRuleNode, RuleModel.class);
        if (ruleModel.getRule() != null) {
            if (ruleCategoryModel.getInheritance() != null &&
                ruleCategoryModel.getInheritance().getPreventRulesId() != null &&
                ruleCategoryModel.getInheritance().getPreventRulesId().contains(ruleModel.getRule())) {
                return;
            } else {
                ruleCategoryModel.getRules().add(ruleModel);
            }
        }
        if (globalMgtRuleNode.has(SedaConstants.TAG_RULE_PREVENT_INHERITANCE)) {
            ruleCategoryModel.setPreventInheritance(
                globalMgtRuleNode.get(SedaConstants.TAG_RULE_PREVENT_INHERITANCE).asBoolean());
        }
        if (globalMgtRuleNode.has(SedaConstants.TAG_RULE_REF_NON_RULE_ID)) {
            if (globalMgtRuleNode.get(SedaConstants.TAG_RULE_REF_NON_RULE_ID).isArray()) {
                for (JsonNode refNonRuleId : globalMgtRuleNode.get(SedaConstants.TAG_RULE_REF_NON_RULE_ID)) {
                    ruleCategoryModel.addToPreventRulesId(refNonRuleId.asText());
                }
            } else {
                ruleCategoryModel.addToPreventRulesId(
                    globalMgtRuleNode.get(SedaConstants.TAG_RULE_REF_NON_RULE_ID).asText());
            }
        }

        if (globalMgtRuleNode.has(SedaConstants.TAG_RULE_CLASSIFICATION_LEVEL)) {
            ruleCategoryModel.setClassificationLevel(
                globalMgtRuleNode.get(SedaConstants.TAG_RULE_CLASSIFICATION_LEVEL).asText());
        }
        if (globalMgtRuleNode.has(SedaConstants.TAG_RULE_CLASSIFICATION_OWNER)) {
            ruleCategoryModel.setClassificationOwner(
                globalMgtRuleNode.get(SedaConstants.TAG_RULE_CLASSIFICATION_OWNER).asText());
        }
        if (globalMgtRuleNode.has(SedaConstants.TAG_RULE_CLASSIFICATION_AUDIENCE)) {
            ruleCategoryModel.setClassificationAudience(
                globalMgtRuleNode.get(SedaConstants.TAG_RULE_CLASSIFICATION_AUDIENCE).asText());
        }
        if (globalMgtRuleNode.has(SedaConstants.TAG_RULE_CLASSIFICATION_REASSESSING_DATE)) {
            ruleCategoryModel.setClassificationReassessingDate(
                globalMgtRuleNode.get(SedaConstants.TAG_RULE_CLASSIFICATION_REASSESSING_DATE).asText());
        }
        if (globalMgtRuleNode.has(SedaConstants.TAG_RULE_CLASSIFICATION_NEED_REASSESSING_AUTHORIZATION)) {
            ruleCategoryModel.setNeedReassessingAuthorization(
                globalMgtRuleNode.get(SedaConstants.TAG_RULE_CLASSIFICATION_NEED_REASSESSING_AUTHORIZATION)
                    .asBoolean());
        }

        JsonNode finalAction = globalMgtRuleNode.get(SedaConstants.TAG_RULE_FINAL_ACTION);
        if (finalAction != null && ruleCategoryModel.getFinalAction() == null) {
            ruleCategoryModel.setFinalAction(finalAction.asText());
        }

        archiveUnitManagementModel.setRuleCategoryModel(ruleCategoryModel, ruleType);

    }

    private boolean attachmentByIngestContractAndManageRulesInformation(IngestContext ingestContext,
        IngestSession ingestSession, ObjectNode archiveUnit, String manifestUnitId, String unitGuid,
        Set<String> globalMgtIdExtra) {
        ObjectNode workNode = JsonHandler.createObjectNode();
        ArrayNode upNode = JsonHandler.createArrayNode();
        // Check if unit is root ?
        boolean isUnitRoot = true;
        final JsonNode archiveNode = ingestSession.getArchiveUnitTree().get(manifestUnitId);
        if (archiveNode != null) {
            // add archive units parents and originating agency
            final JsonNode archiveUps = archiveNode.get(IngestWorkflowConstants.UP_FIELD);
            if (null != archiveUps && archiveUps.isArray() && archiveUps.size() > 0) {
                // Attachment to existing unit should be done by Graph build
                ArrayNode ups = (ArrayNode) archiveUps;
                for (JsonNode parent : ups) {
                    // Convert from manifest id to guid
                    upNode.add(ingestSession.getUnitIdToGuid().get(parent.asText()));
                    // If all parents are already exists, then consider this unit as root
                    // If at least one parent does not exists, then consider this unit as not root
                    boolean atLeastOneParentDoesNotExists =
                        !(ingestSession.getExistingUnitGuids().contains(parent.asText()) ||
                            ingestSession.getExistingUnitGuids()
                                .contains(ingestSession.getUnitIdToGuid().get(parent.asText())));
                    if (atLeastOneParentDoesNotExists) {
                        isUnitRoot = false;
                    }
                }
            }
        }

        if (ingestContext.getIngestContract() != null &&
            !Strings.isNullOrEmpty(ingestContext.getIngestContract().getLinkParentId()) && isUnitRoot) {
            upNode.add(ingestContext.getIngestContract().getLinkParentId());
        }

        workNode.set(IngestWorkflowConstants.UP_FIELD, upNode);


        // Determine rules to apply
        ArrayNode rulesNode = JsonHandler.createArrayNode();
        globalMgtIdExtra.addAll(getMgtRulesToApplyByUnit(ingestSession, rulesNode, manifestUnitId, isUnitRoot));
        workNode.set(IngestWorkflowConstants.RULES, rulesNode);

        // Add existing guid
        if (ingestSession.getExistingUnitGuids().contains(unitGuid)) {
            workNode.put(IngestWorkflowConstants.EXISTING_TAG, Boolean.TRUE);
        }

        archiveUnit.set(SedaConstants.PREFIX_WORK, workNode);

        return isUnitRoot;
    }

    private void createUnitLifeCycle(HandlerIO handlerIO, IngestSession ingestSession, String unitGuid,
        String containerId) throws LogbookClientNotFoundException {

        if (ingestSession.getGuidToLifeCycleParameters().get(unitGuid) != null &&
            (!ingestSession.getExistingUnitGuids().contains(unitGuid))) {
            LogbookLifeCycleUnitParameters unitLifeCycle =
                createUnitLifeCycle(ingestSession.getGuidToLifeCycleParameters(), unitGuid, containerId,
                    LogbookTypeProcess.INGEST);

            handlerIO.getHelper().updateDelegate(unitLifeCycle);
        }
    }

    private void addFinalStatusToUnitLifeCycle(HandlerIO handlerIO, IngestContext ingestContext,
        IngestSession ingestSession, String unitGuid, String unitId, boolean isRootArchive)
        throws LogbookClientNotFoundException {

        if (ingestSession.getGuidToLifeCycleParameters().get(unitGuid) != null) {
            final LogbookLifeCycleParameters llcp = ingestSession.getGuidToLifeCycleParameters().get(unitGuid);
            String eventId = GUIDFactory.newEventGUID(ParameterHelper.getTenantParameter()).toString();
            LogbookLifeCycleParameters subLlcp = null;
            // TODO : add else case
            if (!ingestSession.getExistingUnitGuids().contains(unitGuid)) {
                subLlcp = LogbookLifeCyclesClientHelper.copy(llcp);
                // generate new eventId for task
                subLlcp.putParameterValue(eventIdentifier,
                    GUIDFactory.newEventGUID(ParameterHelper.getTenantParameter()).toString());
                // set parentEventId
                subLlcp.putParameterValue(parentEventIdentifier, eventId);
                // set status for sub task
                subLlcp.setFinalStatus(HANDLER_ID, LFC_CREATION_SUB_TASK_ID, StatusCode.OK, null);
            }
            // generate new eventId for task
            llcp.putParameterValue(eventIdentifier, eventId);
            // set status for task
            llcp.setFinalStatus(HANDLER_ID, null, StatusCode.OK, null);

            Set<String> parentAttachments = existAttachmentUnitAsParentOnTree(ingestSession, unitId);

            if (isRootArchive && ingestContext.getIngestContract() != null &&
                ingestContext.getIngestContract().getLinkParentId() != null) {
                parentAttachments.add(ingestContext.getIngestContract().getLinkParentId());
            }

            ObjectNode evDetData = JsonHandler.createObjectNode();

            if (!parentAttachments.isEmpty()) {
                ArrayNode arrayNode = JsonHandler.createArrayNode();
                parentAttachments.forEach(arrayNode::add);
                evDetData.set(ATTACHMENT_IDS, arrayNode);
            }

            if (ingestSession.getUnitIdToGroupId().containsKey(unitId) &&
                ingestSession.getExistingGOTs().containsKey(ingestSession.getUnitIdToGroupId().get(unitId))) {
                evDetData.put(OBJECT_GROUP_ID, ingestSession.getUnitIdToGroupId().get(unitId));
            }

            try {
                String wellFormedJson = JsonHandler.writeAsString(evDetData);
                llcp.putParameterValue(LogbookParameterName.eventDetailData, wellFormedJson);
            } catch (InvalidParseOperationException e) {
                LOGGER.error("unable to generate evDetData, incomplete journal generation", e);
            }

            // update delegate
            handlerIO.getHelper().updateDelegate(llcp);
            if (!ingestSession.getExistingUnitGuids().contains(unitGuid)) {
                handlerIO.getHelper().updateDelegate(subLlcp);
            }

            // FIXME: use bulk
            // logbookLifeCycleClient.bulkUpdateUnit(containerId, handlerIO.getHelper().removeUpdateDelegate(unitGuid));
        }
    }

    private LogbookLifeCycleUnitParameters createUnitLifeCycle(
        Map<String, LogbookLifeCycleParameters> guidToLifeCycleParameters, String unitGuid, String containerId,
        LogbookTypeProcess logbookTypeProcess) {
        final LogbookLifeCycleUnitParameters logbookLifecycleUnitParameters =
            (LogbookLifeCycleUnitParameters) initLogbookLifeCycleParameters(guidToLifeCycleParameters, unitGuid, true,
                false);

        logbookLifecycleUnitParameters.setFinalStatus(LFC_INITIAL_CREATION_EVENT_TYPE, null, StatusCode.OK, null);

        logbookLifecycleUnitParameters.putParameterValue(LogbookParameterName.eventIdentifierProcess, containerId);
        logbookLifecycleUnitParameters.putParameterValue(eventIdentifier,
            GUIDFactory.newEventGUID(ParameterHelper.getTenantParameter()).toString());
        logbookLifecycleUnitParameters.putParameterValue(eventTypeProcess, logbookTypeProcess.name());

        return logbookLifecycleUnitParameters;
    }


    private Set<String> existAttachmentUnitAsParentOnTree(IngestSession ingestSession, String unitId) {
        Set<String> parents = new HashSet<>();
        if (ingestSession.getArchiveUnitTree().has(unitId)) {
            JsonNode archiveNode = ingestSession.getArchiveUnitTree().get(unitId);
            if (archiveNode.has(IngestWorkflowConstants.UP_FIELD)) {
                final JsonNode archiveUps = archiveNode.get(IngestWorkflowConstants.UP_FIELD);
                if (archiveUps.isArray() && archiveUps.size() > 0) {
                    ArrayNode archiveUpsArray = (ArrayNode) archiveUps;
                    for (JsonNode jsonNode : archiveUpsArray) {
                        String archiveUnitId = jsonNode.textValue();
                        String guid = ingestSession.getUnitIdToGuid().get(archiveUnitId);

                        if (ingestSession.getExistingUnitGuids().contains(guid)) {
                            parents.add(guid);
                        }
                    }
                }
            }
        }
        return parents;
    }

    private Set<String> getMgtRulesToApplyByUnit(IngestSession ingestSession, ArrayNode rulesNode,
        String manifestUnitId, boolean isRootArchive) {

        String listRulesForCurrentUnit = "";
        if (ingestSession.getUnitIdToSetOfRuleId() != null &&
            ingestSession.getUnitIdToSetOfRuleId().containsKey(manifestUnitId)) {
            listRulesForCurrentUnit =
                getListOfRulesFormater(ingestSession.getUnitIdToSetOfRuleId().get(manifestUnitId));
        }

        String listRulesForAuRoot = "";
        Set<String> globalMgtIdExtra = new HashSet<>();

        if (isRootArchive) {
            // Add rules from global Management Data (only new ones)
            if (ingestSession.getMngtMdRuleIdToRulesXml() != null &&
                !ingestSession.getMngtMdRuleIdToRulesXml().isEmpty()) {
                globalMgtIdExtra.addAll(ingestSession.getMngtMdRuleIdToRulesXml().keySet());
            }

            if (!globalMgtIdExtra.isEmpty() && ingestSession.getUnitIdToSetOfRuleId() != null &&
                ingestSession.getUnitIdToSetOfRuleId().get(manifestUnitId) != null &&
                !ingestSession.getUnitIdToSetOfRuleId().get(manifestUnitId).isEmpty()) {
                globalMgtIdExtra.removeAll(ingestSession.getUnitIdToSetOfRuleId().get(manifestUnitId));
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

    private void checkArchiveUnitIdReference(IngestSession ingestSession, String llcEvDetData)
        throws ProcessingException {
        if (ingestSession.getUnitIdToGroupId() != null && !ingestSession.getUnitIdToGroupId().isEmpty()) {
            for (final Entry<String, String> entry : ingestSession.getUnitIdToGroupId().entrySet()) {
                if (ingestSession.getObjectGroupIdToGuid().get(entry.getValue()) == null) {
                    final String groupId =
                        ingestSession.getDataObjectIdToObjectGroupId().get(entry.getValue()); // the AU reference
                    // an BDO
                    if (Strings.isNullOrEmpty(groupId)) {
                        throw new ProcessingException("Archive Unit references a BDO Id but is not correct");
                    } else {
                        if (!groupId.equals(entry.getValue())) {
                            throw new ArchiveUnitContainDataObjectException(
                                "The archive unit " + entry.getKey() + " references one BDO Id " + entry.getValue() +
                                    " while this BDO has a GOT id " + groupId, entry.getKey(), entry.getValue(),
                                groupId, llcEvDetData);
                        }
                    }
                }
            }
        }
    }

    private void createObjectGroupLifeCycle(HandlerIO handlerIO,
        Map<String, LogbookLifeCycleParameters> guidToLifeCycleParameters, String groupGuid, String containerId,
        LogbookTypeProcess typeProcess) throws LogbookClientAlreadyExistsException {
        final LogbookLifeCycleObjectGroupParameters logbookLifecycleObjectGroupParameters =
            (LogbookLifeCycleObjectGroupParameters) initLogbookLifeCycleParameters(guidToLifeCycleParameters, groupGuid,
                false, true);
        logbookLifecycleObjectGroupParameters.setFinalStatus(LFC_INITIAL_CREATION_EVENT_TYPE, null, StatusCode.OK,
            null);

        logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.eventIdentifierProcess,
            containerId);
        logbookLifecycleObjectGroupParameters.putParameterValue(eventIdentifier,
            GUIDFactory.newEventGUID(ParameterHelper.getTenantParameter()).toString());
        logbookLifecycleObjectGroupParameters.putParameterValue(eventTypeProcess, typeProcess.name());

        handlerIO.getHelper().createDelegate(logbookLifecycleObjectGroupParameters);

        // Update guidToLifeCycleParameters
        guidToLifeCycleParameters.put(groupGuid, logbookLifecycleObjectGroupParameters);
    }

    /**
     * create level stack on Json file
     *
     * @param handlerIO
     * @param levelStackMap
     * @param rank
     * @throws ProcessingException
     */
    private void createIngestLevelStackFile(HandlerIO handlerIO, IngestSession ingestSession,
        Map<Integer, Set<String>> levelStackMap, int rank) throws ProcessingException {
        LOGGER.debug("Begin createIngestLevelStackFile/containerId: ", handlerIO.getContainerName());
        ParametersChecker.checkParameter("levelStackMap is a mandatory parameter", levelStackMap);
        ParametersChecker.checkParameter("unitIdToGuid is a mandatory parameter", ingestSession.getUnitIdToGuid());

        try {
            File tempFile = handlerIO.getNewLocalFile(handlerIO.getOutput(rank).getPath());
            // create level json object node
            final ObjectNode ingestLevelStack = JsonHandler.createObjectNode();
            for (final Entry<Integer, Set<String>> entry : levelStackMap.entrySet()) {
                final ArrayNode unitList = ingestLevelStack.withArray(LEVEL + entry.getKey());
                final Set<String> unitGuidList = entry.getValue();
                Set<String> alreadyAdded = new HashSet<>();
                for (final String idXml : unitGuidList) {

                    final String unitGuid = ingestSession.getUnitIdToGuid().get(idXml);
                    if (unitGuid == null) {
                        throw new IllegalArgumentException("Unit guid not found in map");
                    }

                    if (ingestSession.getExistingUnitGuids().contains(unitGuid)) {
                        continue;
                    }

                    if (!alreadyAdded.contains(unitGuid)) {
                        alreadyAdded.add(unitGuid);
                        unitList.add(unitGuid);
                    }
                }
                ingestLevelStack.set(LEVEL + entry.getKey(), unitList);
            }
            LOGGER.debug("IngestLevelStack: {}", ingestLevelStack);
            // create json file
            JsonHandler.writeAsFile(ingestLevelStack, tempFile);
            // put file in workspace
            handlerIO.addOutputResult(rank, tempFile, true, asyncIO);
        } catch (final IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error(e);
            throw new ProcessingException(e);
        }
        LOGGER.info("End createIngestLevelStackFile/containerId:" + handlerIO.getContainerName());

    }


    private LogbookParameters initLogbookLifeCycleParameters(
        Map<String, LogbookLifeCycleParameters> guidToLifeCycleParameters, String guid, boolean isArchive,
        boolean isObjectGroup) {
        LogbookParameters logbookLifeCycleParameters = guidToLifeCycleParameters.get(guid);
        if (logbookLifeCycleParameters == null) {
            logbookLifeCycleParameters = isArchive ?
                LogbookParameterHelper.newLogbookLifeCycleUnitParameters() :
                isObjectGroup ?
                    LogbookParameterHelper.newLogbookLifeCycleObjectGroupParameters() :
                    LogbookParameterHelper.newLogbookOperationParameters();


            logbookLifeCycleParameters.putParameterValue(LogbookParameterName.objectIdentifier, guid);
        }
        return logbookLifeCycleParameters;
    }


    private void checkMasterIsMandatoryAndCheckCanAddObjectToExistingObjectGroup(IngestSession ingestSession,
        IngestContractModel ingestContract) throws ProcessingException {
        Set<String> updatedObjectGroupIds = getUpdatedObjectGroupIds(ingestSession);
        checkMasterMandatory(ingestSession, ingestContract, updatedObjectGroupIds);
        checkIngestContractForObjectGroupAttachment(ingestSession, ingestContract,
            ingestSession.getUsageToObjectGroupId(), updatedObjectGroupIds);
    }

    private Set<String> getUpdatedObjectGroupIds(IngestSession ingestSession) {
        Set<String> updatedUnitsId = ingestSession.getUnitIdToGuid().keySet().stream().filter(
            unitId -> ingestSession.getExistingUnitIdWithExistingObjectGroup()
                .containsKey(ingestSession.getUnitIdToGuid().get(unitId))).collect(Collectors.toSet());
        return ingestSession.getUnitIdToGroupId().entrySet().stream()
            .filter(entry -> updatedUnitsId.contains(entry.getKey())).map(Entry::getValue).collect(Collectors.toSet());

    }

    private void checkMasterMandatory(IngestSession ingestSession, IngestContractModel contract,
        Set<String> updatedObjectGroupIds) throws ProcessingObjectGroupMasterMandatoryException {
        if (contract.isMasterMandatory()) {
            List<String> objectIdWithoutMaster =
                ingestSession.getDataObjectGroupMasterMandatory().entrySet().stream().filter(entry -> !entry.getValue())
                    .map(Entry::getKey).collect(Collectors.toList());
            List<String> objectIdWithoutUpdatedOG =
                objectIdWithoutMaster.stream().filter(objectId -> !updatedObjectGroupIds.contains(objectId))
                    .collect(Collectors.toList());
            if (!objectIdWithoutUpdatedOG.isEmpty()) {
                String objectGroupsId = String.join(" , ", objectIdWithoutMaster);
                throw new ProcessingObjectGroupMasterMandatoryException(
                    String.format("BinaryMaster or PhysicalMaster is not present for objectGroup : %s", objectGroupsId),
                    objectGroupsId);
            }
        }

    }

    private void checkIngestContractForObjectGroupAttachment(IngestSession ingestSession, IngestContractModel contract,
        Multimap<String, String> usages, Set<String> updatedObjectGroupIds)
        throws ProcessingObjectGroupEveryDataObjectVersionException {

        if (!ingestSession.getExistingUnitIdWithExistingObjectGroup().isEmpty() &&
            !contract.isEveryDataObjectVersion()) {
            final Set<String> dataObjectVersion = contract.getDataObjectVersion();
            if (dataObjectVersion != null && !dataObjectVersion.isEmpty()) {
                Set<String> usageInObjectVersion =
                    usages.asMap().entrySet().stream().filter(entry -> dataObjectVersion.contains(entry.getKey()))
                        .map(entry -> {
                            Set<String> values = new HashSet<>(entry.getValue());
                            List<String> updatedObjectGroupGuids = updatedObjectGroupIds.stream().map(e -> ingestSession.getObjectGroupIdToGuid().get(e)).collect(Collectors.toList());
                            values.retainAll(updatedObjectGroupGuids);
                            return new AbstractMap.SimpleEntry<>(entry.getKey(), values);
                        }).filter(entry -> !entry.getValue().isEmpty()).map(Entry::getKey).collect(Collectors.toSet());
                if (usageInObjectVersion.size() == dataObjectVersion.size()) {
                    return;
                }
            }
            List<String> ogId = new ArrayList<>();

            ingestSession.getExistingUnitIdWithExistingObjectGroup().keySet().forEach(
                unitGuid -> ingestSession.getUnitIdToGuid().entrySet().stream()
                    .filter(entry -> entry.getValue().contains(unitGuid)).findFirst()
                    .ifPresent(unitmap -> ogId.add(ingestSession.getUnitIdToGroupId().get(unitmap.getKey()))));
            String objectGroupsId = String.join(" , ", ogId);
            String unitsId = String.join(" , ", ingestSession.getExistingUnitIdWithExistingObjectGroup().keySet());
            throw new ProcessingObjectGroupEveryDataObjectVersionException(
                "Ingest Contract don't authorized ObjectGroup attachment", unitsId, objectGroupsId);
        }
    }

    private void saveObjectGroupsToWorkspace(HandlerIO handlerIO, IngestContext ingestContext,
        IngestSession ingestSession, JsonLineDataBase objectsDatabase, JsonNode storageObjectGroupInfo,
        JsonNode storageObjectInfo) throws ProcessingException {
        boolean existingGot = false;
        Map<String, ObjectNode> listObjectToValidate = new HashMap<>();

        // Save maps
        try {
            // Save dataObjectIdToObjectGroupId
            HandlerUtils.saveMap(handlerIO, ingestSession.getDataObjectIdToObjectGroupId(), DO_ID_TO_OG_ID_IO_RANK,
                true, asyncIO);
            // Save objectGroupIdToGuid
            HandlerUtils.saveMap(handlerIO, ingestSession.getObjectGroupIdToGuid(), OG_ID_TO_GUID_IO_RANK, true,
                asyncIO);
            handlerIO.addOutputResult(OG_ID_TO_GUID_IO_MEMORY_RANK, ingestSession.getObjectGroupIdToGuid(), asyncIO);
        } catch (final IOException e1) {
            LOGGER.error("Can not write to tmp folder ", e1);
            throw new ProcessingException(e1);
        }

        // check if folder OBJECT_GROUP_FOLDER is not empty, if it is not, that means we 're in the replay mode, so lets
        // purge it
        try {
            if (handlerIO.removeFolder(IngestWorkflowConstants.OBJECT_GROUP_FOLDER)) {
                LOGGER.warn(
                    "Folder has been deleted, it's a replay for this operation : " + ingestContext.getOperationId());
            }
        } catch (ContentAddressableStorageException e1) {
            LOGGER.warn("Couldn't delete folder", e1);
        }

        List<String> uuids = new ArrayList<>();


        for (final Entry<String, List<String>> entry : ingestSession.getObjectGroupIdToDataObjectId().entrySet()) {
            final ObjectNode objectGroup = JsonHandler.createObjectNode();
            ObjectNode fileInfo = JsonHandler.createObjectNode();
            final ArrayNode unitParent = JsonHandler.createArrayNode();
            String objectGroupType = "";

            String unitParentGUID = null;
            if (ingestSession.getObjectGroupIdToUnitId() != null &&
                ingestSession.getObjectGroupIdToUnitId().size() != 0 &&
                ingestSession.getObjectGroupIdToUnitId().get(entry.getKey()) != null) {
                for (final String unitId : ingestSession.getObjectGroupIdToUnitId().get(entry.getKey())) {
                    if (ingestSession.getUnitIdToGuid().get(unitId) != null) {
                        unitParentGUID = ingestSession.getUnitIdToGuid().get(unitId);
                        unitParent.add(unitParentGUID);
                    }
                }
            }

            String objectGroupGuid = ingestSession.getObjectGroupIdToGuid().get(entry.getKey());
            if (ingestSession.getExistingUnitIdWithExistingObjectGroup().containsKey(unitParentGUID)) {
                existingGot = true;
                objectGroupGuid = ingestSession.getExistingUnitIdWithExistingObjectGroup().get(unitParentGUID);
                // Override the value
                ingestSession.getObjectGroupIdToGuid().put(entry.getKey(), objectGroupGuid);
            }

            final File tmpFile = handlerIO.getNewLocalFile(objectGroupGuid + JSON_EXTENSION);

            uuids.add(objectGroupGuid);

            try {

                final Map<String, List<JsonNode>> categoryMap = new HashMap<>();
                objectGroup.put(SedaConstants.PREFIX_ID, objectGroupGuid);
                objectGroup.put(SedaConstants.PREFIX_TENANT_ID, ParameterHelper.getTenantParameter());

                final List<String> versionList = new ArrayList<>();
                final Set<String> dataObjectVersions = new HashSet<>();
                for (int index = 0; index < entry.getValue().size(); index++) {
                    final String id = entry.getValue().get(index);
                    JsonNode dataObjectNode = objectsDatabase.read(ingestSession.getDataObjectIdToGuid().get(id));

                    boolean isPhysical = ingestSession.getPhysicalDataObjetsGuids()
                        .contains(ingestSession.getDataObjectIdToGuid().get(id));

                    // Force DataObjectGroupId to be equals to objectGroupGuid in Binary or Physical object metadata
                    ((ObjectNode) dataObjectNode).put(SedaConstants.TAG_DATA_OBJECT_GROUPE_ID, objectGroupGuid);

                    String nodeCategory = "";
                    if (dataObjectNode.get(SedaConstants.TAG_DO_VERSION) != null) {
                        nodeCategory = dataObjectNode.get(SedaConstants.TAG_DO_VERSION).asText();
                    }

                    if (Strings.isNullOrEmpty(nodeCategory)) {
                        if (isPhysical) {
                            nodeCategory = PHYSICAL_MASTER;
                        } else {
                            nodeCategory = BINARY_MASTER;
                        }
                    }

                    if (versionList.contains(nodeCategory)) {
                        LOGGER.error(DATA_OBJECT_VERSION_MUST_BE_UNIQUE);
                        throw new ProcessingDuplicatedVersionException(DATA_OBJECT_VERSION_MUST_BE_UNIQUE);
                    }
                    versionList.add(nodeCategory);

                    List<JsonNode> nodeCategoryArray = categoryMap.get(nodeCategory);
                    String[] array = nodeCategory.split("_");
                    String realCategory = array[0];

                    // FIXME ugly fix of the BUG 5178. Do not allow multiple dataObjectVersion by usage if not adding objects to existing GOT
                    // TODO To be deleted when the bug 5178 is properly fixed
                    if (!existingGot && dataObjectVersions.contains(realCategory)) {
                        // When ingest multiple dataObjectVersion by usage is not allowed
                        throw new ProcessingTooManyVersionsByUsageException(
                            "[Not allowed for first ingest] Too many versions found for the usage (" + realCategory +
                                ") of the object group (" + entry.getKey() + ")");
                    }
                    dataObjectVersions.add(realCategory);

                    if (array.length == 1) {
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
                        if (dataObjectNode.get(METADATA) != null && !dataObjectNode.get(METADATA).isNull() &&
                            !dataObjectNode.get(METADATA).isEmpty()) {
                            objectGroupType = dataObjectNode.get(METADATA).fieldNames().next();
                        }
                    }
                }

                File newLocalFile =
                    handlerIO.getNewLocalFile(objectGroupGuid + LOGBOOK_OG_FILE_SUFFIX + JSON_EXTENSION);
                if (newLocalFile.exists()) {
                    List<LogbookEvent> events =
                        JsonHandler.getFromFileAsTypeReference(newLocalFile, LIST_TYPE_REFERENCE);

                    if (!events.isEmpty()) {
                        createExternalLifeCycleLogbook(handlerIO, events, ingestContext.getOperationId(),
                            objectGroupGuid);
                    }
                }

                objectGroup.put(SedaConstants.PREFIX_TYPE, objectGroupType);
                objectGroup.set(SedaConstants.TAG_FILE_INFO, fileInfo);
                final ArrayNode qualifiersNode =
                    getObjectGroupQualifiers(ingestSession, categoryMap, ingestContext.getOperationId());
                objectGroup.set(SedaConstants.PREFIX_QUALIFIERS, qualifiersNode);
                final ObjectNode workNode =
                    getObjectGroupWork(ingestSession, categoryMap, ingestContext.getOperationId(), storageObjectInfo);
                objectGroup.set(SedaConstants.PREFIX_WORK, workNode);
                objectGroup.set(SedaConstants.PREFIX_UP, unitParent);

                objectGroup.put(SedaConstants.PREFIX_NB, entry.getValue().size());
                // Add operation to OPS
                objectGroup.putArray(SedaConstants.PREFIX_OPS).add(ingestContext.getOperationId());
                objectGroup.put(SedaConstants.PREFIX_OPI, ingestContext.getOperationId());
                objectGroup.put(SedaConstants.PREFIX_ORIGINATING_AGENCY, ingestContext.getOriginatingAgency());
                objectGroup.set(SedaConstants.PREFIX_ORIGINATING_AGENCIES,
                    JsonHandler.createArrayNode().add(ingestContext.getOriginatingAgency()));
                ObjectNode storageObjectGroup = JsonHandler.createObjectNode();
                storageObjectGroup.put(SedaConstants.STRATEGY_ID,
                    storageObjectGroupInfo.get(SedaConstants.STRATEGY_ID).asText());
                objectGroup.set(SedaConstants.STORAGE, storageObjectGroup);
                // In case of attachment, this will be true, we will then add information about existing og in work
                if (existingGot) {
                    String existingOg = ingestSession.getExistingUnitIdWithExistingObjectGroup().get(unitParentGUID);
                    workNode.put(SedaConstants.TAG_DATA_OBJECT_GROUP_EXISTING_REFERENCEID, existingOg);
                    if (existingOg != null) {
                        listObjectToValidate.put(existingOg, objectGroup);
                    }
                }
                JsonHandler.writeAsFile(objectGroup, tmpFile);

                handlerIO.transferFileToWorkspace(
                    IngestWorkflowConstants.OBJECT_GROUP_FOLDER + "/" + objectGroupGuid + JSON_EXTENSION, tmpFile, true,
                    asyncIO);
                // Create unreferenced object group
                createObjectGroupLifeCycle(handlerIO, ingestSession.getGuidToLifeCycleParameters(), objectGroupGuid,
                    ingestContext.getOperationId(), ingestContext.getTypeProcess());

                if (!existingGot) {
                    // Update Object Group lifeCycle creation event
                    // Set new eventId for task and set status then update delegate
                    String eventId = GUIDFactory.newEventGUID(ParameterHelper.getTenantParameter()).toString();
                    handlerIO.getHelper().updateDelegate(
                        (LogbookLifeCycleObjectGroupParameters) ingestSession.getGuidToLifeCycleParameters()
                            .get(objectGroupGuid).setFinalStatus(HANDLER_ID, null, StatusCode.OK, null)
                            .putParameterValue(eventIdentifier, eventId));
                    // Add creation sub task event (add new eventId and set status for subtask before update delegate)
                    handlerIO.getHelper().updateDelegate(
                        (LogbookLifeCycleObjectGroupParameters) ingestSession.getGuidToLifeCycleParameters()
                            .get(objectGroupGuid)
                            .setFinalStatus(HANDLER_ID, LFC_CREATION_SUB_TASK_ID, StatusCode.OK, null)
                            .putParameterValue(eventIdentifier,
                                GUIDFactory.newEventGUID(ParameterHelper.getTenantParameter()).toString())
                            .putParameterValue(parentEventIdentifier, eventId));

                }

                if (uuids.size() == BATCH_SIZE) {
                    bulkLifeCycleObjectGroup(handlerIO, ingestContext.getOperationId(), uuids);
                    uuids.clear();
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

        if (!uuids.isEmpty()) {
            try {
                bulkLifeCycleObjectGroup(handlerIO, ingestContext.getOperationId(), uuids);
                uuids.clear();
            } catch (LogbookClientBadRequestException | LogbookClientServerException e) {
                throw new VitamRuntimeException(e);
            }
        }

        manageExistingObjectGroups(handlerIO, ingestContext, ingestSession, uuids);
        // Check Linking to Object Group by bad SP
        if (!listObjectToValidate.isEmpty()) {
            try {
                checkOriginatingAgencyAttachementConformity(ingestContext.getOriginatingAgency(), listObjectToValidate);
            } catch (ProcessingStatusException e) {
                throw new ProcessingException(e);
            }
        }
    }

    private void checkOriginatingAgencyAttachementConformity(String originatingAgency,
        Map<String, ObjectNode> listObjectToValidate)
        throws ProcessingStatusException, ProcessingNotValidLinkingException {

        Set<String> objectGroupIds = listObjectToValidate.keySet();
        boolean objectGroupsExistedIdsWithSp = loadExistingObjectGroups(originatingAgency, objectGroupIds);

        if (!objectGroupsExistedIdsWithSp) {
            throw new ProcessingNotValidLinkingException(
                "Not allowed object attachement of originating agency (" + originatingAgency +
                    ") to other originating agency");
        }

    }

    private boolean loadExistingObjectGroups(String originatingAgency, Set<String> objectGroupIds)
        throws ProcessingStatusException {

        try (MetaDataClient client = metaDataClientFactory.getClient()) {

            for (List<String> ids : ListUtils.partition(new ArrayList<>(objectGroupIds), MAX_ELASTIC_REQUEST_SIZE)) {

                SelectMultiQuery selectMultiQuery = new SelectMultiQuery();
                selectMultiQuery.addRoots(ids.toArray(new String[0]));
                selectMultiQuery.addQueries(ne(VitamFieldsHelper.originatingAgency(), originatingAgency));
                ObjectNode objectNode = createObjectNode();
                objectNode.put(VitamFieldsHelper.originatingAgency(), 1);
                objectNode.put(VitamFieldsHelper.id(), 1);
                JsonNode projection = createObjectNode().set("$fields", objectNode);
                selectMultiQuery.setProjection(projection);

                JsonNode jsonNode = client.selectObjectGroups(selectMultiQuery.getFinalSelect());
                RequestResponseOK<JsonNode> requestResponseOK = RequestResponseOK.getFromJsonNode(jsonNode);

                if (!requestResponseOK.getResults().isEmpty()) {
                    return false;
                }
            }
            return true;

        } catch (MetaDataExecutionException | MetaDataDocumentSizeException | MetaDataClientServerException |
                 InvalidParseOperationException | InvalidCreateOperationException e) {
            throw new ProcessingStatusException(StatusCode.FATAL, "Could not load object groups", e);
        }
    }

    /**
     * Particular treatment for existing object group
     *
     * @param handlerIO
     * @param uuids
     * @throws ProcessingException
     */
    private void manageExistingObjectGroups(HandlerIO handlerIO, IngestContext ingestContext,
        IngestSession ingestSession, List<String> uuids) throws ProcessingException {
        Set<String> toIgnore = new HashSet<>();
        if (!ingestSession.getExistingGOTs().isEmpty()) {
            try (LogbookLifeCyclesClient logbookLifeCyclesClient = logbookLifeCyclesClientFactory.getClient()) {
                for (List<String> partition : Lists.partition(new ArrayList<>(ingestSession.getExistingGOTs().keySet()),
                    VitamConfiguration.getBatchSize())) {
                    Select select = new Select();
                    InQuery evId = QueryHelper.in(VitamFieldsHelper.id(), partition.toArray(String[]::new));
                    select.setQuery(evId);
                    select.addUsedProjection(VitamFieldsHelper.id(), LogbookEvent.OB_ID, LogbookEvent.EV_ID_PROC);
                    JsonNode request = logbookLifeCyclesClient.selectObjectGroupLifeCycle(select.getFinalSelect(),
                        LIFE_CYCLE_IN_PROCESS);
                    RequestResponseOK<JsonNode> requestResponseOK = RequestResponseOK.getFromJsonNode(request);
                    for (JsonNode json : requestResponseOK.getResults()) {
                        if (!json.get(LogbookEvent.EV_ID_PROC).asText().equals(handlerIO.getContainerName())) {
                            throw new ProcessingObjectGroupLifeCycleException(LOGBOOK_LF_OBJECT_EXISTS_EXCEPTION_MSG,
                                json.get(LogbookEvent.OB_ID).asText());
                        } else {
                            toIgnore.add(json.get(VitamFieldsHelper.id()).asText());
                        }
                    }
                }
            } catch (LogbookClientNotFoundException e) {
                LOGGER.debug("No logbook currently in process");
            } catch (LogbookClientException | InvalidCreateOperationException | InvalidParseOperationException e) {
                LOGGER.error(e.getMessage(), e);
                throw new ProcessingException(e);
            }
        }

        final Set<String> collect =
            ingestSession.getExistingGOTs().entrySet().stream().filter(e -> e.getValue() != null).map(Entry::getKey)
                .filter(Predicate.not(toIgnore::contains)).map(e -> e + JSON_EXTENSION).collect(Collectors.toSet());

        File existingGotsFile = handlerIO.getNewLocalFile(handlerIO.getOutput(EXISTING_GOT_RANK).getPath());

        try {
            JsonHandler.writeAsFile(collect, existingGotsFile);
        } catch (InvalidParseOperationException e) {
            throw new ProcessingException(e);
        }
        handlerIO.addOutputResult(EXISTING_GOT_RANK, existingGotsFile, true, false);

        // Update LFC of exiting object group
        for (String gotGuid : ingestSession.getExistingGOTs().keySet()) {
            try {

                // Get original information from got and save them in LFC in order to keep possibility to rollback if
                // ingest >= KO
                JsonNode existingGot = ingestSession.getExistingGOTs().get(gotGuid);
                if (existingGot == null) {
                    // Idempotency, GOT already treated. @see ArchiveUnitListener for more information
                    continue;
                }

                uuids.add(gotGuid);

                createObjectGroupLifeCycle(handlerIO, ingestSession.getGuidToLifeCycleParameters(), gotGuid,
                    ingestContext.getOperationId(), ingestContext.getTypeProcess());

                if (uuids.size() == BATCH_SIZE) {
                    bulkLifeCycleObjectGroup(handlerIO, ingestContext.getOperationId(), uuids);
                    uuids.clear();
                }

            } catch (final LogbookClientBadRequestException e) {
                LOGGER.error(LOGBOOK_LF_BAD_REQUEST_EXCEPTION_MSG, e);
                throw new ProcessingException(e);
            } catch (final LogbookClientServerException e) {
                LOGGER.error(LOGBOOK_SERVER_INTERNAL_EXCEPTION_MSG, e);
                throw new ProcessingException(e);
            } catch (final LogbookClientAlreadyExistsException e) {
                LOGGER.error(LOGBOOK_LF_OBJECT_EXISTS_EXCEPTION_MSG, e);
                throw new ProcessingException(e);
            }
        }

        if (!uuids.isEmpty()) {
            try {
                bulkLifeCycleObjectGroup(handlerIO, ingestContext.getOperationId(), uuids);
                uuids.clear();
            } catch (LogbookClientBadRequestException | LogbookClientServerException e) {
                throw new VitamRuntimeException(e);
            }
        }
    }

    private ArrayNode getObjectGroupQualifiers(IngestSession ingestSession, Map<String, List<JsonNode>> categoryMap,
        String containerId) {
        final ArrayNode qualifiersArray = JsonHandler.createArrayNode();
        for (final Entry<String, List<JsonNode>> entry : categoryMap.entrySet()) {
            final ObjectNode objectNode = JsonHandler.createObjectNode();
            // fix qualifier_version in qualifier field
            String qualifier;
            if (entry.getKey().contains("_")) {
                qualifier = entry.getKey().split("_")[0];
                objectNode.put(SedaConstants.PREFIX_QUALIFIER, qualifier);
            } else {
                qualifier = entry.getKey();
                objectNode.put(SedaConstants.PREFIX_QUALIFIER, qualifier);
            }
            objectNode.put(SedaConstants.TAG_NB, entry.getValue().size());
            final ArrayNode arrayNode = JsonHandler.createArrayNode();
            for (final JsonNode node : entry.getValue()) {
                final String id = node.findValue(SedaConstants.PREFIX_ID).textValue();
                final String guid = ingestSession.getDataObjectIdToGuid().get(id);
                updateObjectNode(ingestSession, (ObjectNode) node, guid, PHYSICAL_MASTER.equals(qualifier),
                    containerId);
                arrayNode.add(node);
            }
            objectNode.set(SedaConstants.TAG_VERSIONS, arrayNode);
            qualifiersArray.add(objectNode);
        }
        return qualifiersArray;
    }

    private ObjectNode getObjectGroupWork(IngestSession ingestSession, Map<String, List<JsonNode>> categoryMap,
        String containerId, JsonNode storageObjectInfo) {
        final ObjectNode workObject = JsonHandler.createObjectNode();
        final ObjectNode qualifierObject = JsonHandler.createObjectNode();
        for (final Entry<String, List<JsonNode>> entry : categoryMap.entrySet()) {
            final ObjectNode dataObjectNode = JsonHandler.createObjectNode();
            dataObjectNode.put(SedaConstants.TAG_NB, entry.getValue().size());
            final ArrayNode arrayNode = JsonHandler.createArrayNode();
            for (final JsonNode node : entry.getValue()) {
                final ObjectNode objectNode = JsonHandler.createObjectNode();
                final String id = node.findValue(SedaConstants.PREFIX_ID).textValue();
                boolean phsyical = ingestSession.getPhysicalDataObjetsGuids().contains(id);
                updateObjectNode(ingestSession, objectNode, id, phsyical, containerId);
                if (phsyical) {
                    objectNode.set(SedaConstants.TAG_PHYSICAL_ID, node.get(SedaConstants.TAG_PHYSICAL_ID));
                } else {
                    ObjectNode storageObject = JsonHandler.createObjectNode();
                    storageObject.put(SedaConstants.STRATEGY_ID,
                        storageObjectInfo.get(SedaConstants.STRATEGY_ID).asText());
                    objectNode.set(SedaConstants.STORAGE, storageObject);
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
     * @param guid guid of data object
     * @param isPhysical is this object a physical object
     */

    private void updateObjectNode(IngestSession ingestSession, final ObjectNode objectNode, String guid,
        boolean isPhysical, String containerId) {
        objectNode.put(SedaConstants.PREFIX_ID, guid);
        objectNode.put(SedaConstants.PREFIX_OPI, containerId);
        if (!isPhysical) {
            if (ingestSession.getObjectGuidToDataObject().get(guid).getSize() != null) {
                objectNode.put(SedaConstants.TAG_SIZE, ingestSession.getObjectGuidToDataObject().get(guid).getSize());
                ObjectNode diffSizeJson = ingestSession.getObjectGuidToDataObject().get(guid).getDiffSizeJson();
                if (!JsonHandler.isNullOrEmpty(diffSizeJson)) {
                    ObjectNode work = JsonHandler.createObjectNode();
                    work.put(IngestWorkflowConstants.IS_SIZE_INCORRECT,
                        ingestSession.getObjectGuidToDataObject().get(guid).getSizeIncorrect());
                    work.set(IngestWorkflowConstants.DIFF_SIZE_JSON, diffSizeJson);
                    objectNode.set(SedaConstants.PREFIX_WORK, work);
                }
            }
            if (ingestSession.getObjectGuidToDataObject().get(guid).getUri() != null) {
                objectNode.put(SedaConstants.TAG_URI, ingestSession.getObjectGuidToDataObject().get(guid).getUri());
            }
            if (ingestSession.getObjectGuidToDataObject().get(guid).getMessageDigest() != null) {
                objectNode.put(SedaConstants.TAG_DIGEST,
                    ingestSession.getObjectGuidToDataObject().get(guid).getMessageDigest());
            }
            if (ingestSession.getObjectGuidToDataObject().get(guid).getAlgo() != null) {
                objectNode.put(SedaConstants.ALGORITHM,
                    ingestSession.getObjectGuidToDataObject().get(guid).getAlgo().getName());
            }
        }
    }

    private void createLifeCycleForError(Map<String, LogbookLifeCycleParameters> guidToLifeCycleParameters,
        String subTask, String message, String guid, boolean isArchive, boolean isObjectGroup, String containerId,
        LogbookTypeProcess logbookTypeProcess)
        throws InvalidParseOperationException, LogbookClientNotFoundException, LogbookClientBadRequestException,
        LogbookClientAlreadyExistsException, LogbookClientServerException {

        final LogbookLifeCycleParameters lfcParameters =
            (LogbookLifeCycleParameters) initLogbookLifeCycleParameters(guidToLifeCycleParameters, guid, isArchive,
                isObjectGroup);
        lfcParameters.setFinalStatus(LFC_INITIAL_CREATION_EVENT_TYPE, null, StatusCode.KO, null);
        lfcParameters.putParameterValue(LogbookParameterName.eventIdentifierProcess, containerId);
        lfcParameters.putParameterValue(eventIdentifier,
            GUIDFactory.newEventGUID(ParameterHelper.getTenantParameter()).toString());
        lfcParameters.putParameterValue(eventTypeProcess, logbookTypeProcess.name());

        // do not create a lifecycle if guid is incorrect.
        try (LogbookLifeCyclesClient logbookLifeCycleClient = logbookLifeCyclesClientFactory.getClient()) {
            GUIDReader.getGUID(guid);
            logbookLifeCycleClient.create(lfcParameters);
            guidToLifeCycleParameters.put(guid, lfcParameters);

            lfcParameters.setFinalStatus(HANDLER_ID, subTask, StatusCode.KO, null);
            ObjectNode llcEvDetData = JsonHandler.createObjectNode();
            llcEvDetData.put(SedaConstants.EV_DET_TECH_DATA, message);
            lfcParameters.putParameterValue(LogbookParameterName.eventDetailData,
                JsonHandler.writeAsString(llcEvDetData));
            logbookLifeCycleClient.update(lfcParameters);

        } catch (final InvalidGuidOperationException e) {
            LOGGER.error("ID is not a GUID: " + guid, e);
        }
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        if (!handler.checkHandlerIO(HANDLER_IO_OUT_PARAMETER_NUMBER, HANDLER_INPUT_IO_LIST)) {
            throw new ProcessingException(HandlerIOImpl.NOT_CONFORM_PARAM);
        }
    }

    private void extractOntology(HandlerIO handlerIO) throws ProcessingException {
        Select selectOntologies = new Select();
        List<OntologyModel> ontologyModelList = new ArrayList<>();
        try (AdminManagementClient adminClient = adminManagementClientFactory.getClient()) {
            selectOntologies.setQuery(QueryHelper.in(OntologyModel.TAG_COLLECTIONS, MetadataType.UNIT.getName()));
            Map<String, Integer> projection = new HashMap<>();
            projection.put(OntologyModel.TAG_IDENTIFIER, 1);
            projection.put(OntologyModel.TAG_TYPE, 1);
            QueryProjection queryProjection = new QueryProjection();
            queryProjection.setFields(projection);
            selectOntologies.setProjection(JsonHandler.toJsonNode(queryProjection));
            RequestResponse<OntologyModel> responseOntologies =
                adminClient.findOntologies(selectOntologies.getFinalSelect());
            if (responseOntologies != null && responseOntologies.isOk() &&
                !((RequestResponseOK<OntologyModel>) responseOntologies).getResults().isEmpty()) {
                ontologyModelList = ((RequestResponseOK<OntologyModel>) responseOntologies).getResults();
            }
            File tempFile = handlerIO.getNewLocalFile(handlerIO.getOutput(ONTOLOGY_IO_RANK).getPath());
            // create json file
            JsonHandler.writeAsFile(ontologyModelList, tempFile);
            // put file in workspace
            handlerIO.addOutputResult(ONTOLOGY_IO_RANK, tempFile, true, false);
        } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
            LOGGER.error("Could not get ontology", e);
            throw new ProcessingException(e);
        }
    }

    private void extractMetadataUsingMarshellar(XMLEventReader reader, Unmarshaller unmarshaller, HandlerIO handlerIO,
        IngestContext ingestContext, IngestSession ingestSession, Class<?> clasz)
        throws InvalidParseOperationException, LogbookClientAlreadyExistsException, LogbookClientNotFoundException,
        IOException, LogbookClientBadRequestException, LogbookClientServerException, ProcessingException {
        try {
            unmarshaller.unmarshal(reader, clasz);
        } catch (RuntimeException e) {
            handleJaxbUnmarshalRuntimeException(handlerIO, ingestContext, ingestSession, e);
        } catch (JAXBException e) {
            throw new InvalidParseOperationException(e);
        }
    }
}
