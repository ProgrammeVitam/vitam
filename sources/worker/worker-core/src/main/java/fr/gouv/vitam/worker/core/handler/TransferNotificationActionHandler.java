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

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.StreamSupport;
import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.utils.LifecyclesSpliterator;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.IngestWorkflowConstants;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.LifeCycleStatusCode;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookDocument;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleObjectGroupInProcess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleUnitInProcess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookOperation;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameterName;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageClientException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.common.utils.DataObjectDetail;
import fr.gouv.vitam.worker.common.utils.ValidationXsdUtils;
import fr.gouv.vitam.worker.core.MarshallerObjectCache;
import fr.gouv.vitam.worker.core.impl.HandlerIOImpl;
import fr.gouv.vitam.worker.model.ArchiveUnitReplyTypeRoot;
import fr.gouv.vitam.worker.model.BinaryDataObjectTypeRoot;
import fr.gouv.vitam.worker.model.DataObjectTypeRoot;
import fr.gouv.vitam.worker.model.PhysicalDataObjectTypeRoot;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import org.bson.Document;
import org.xml.sax.SAXException;

/**
 * Transfer notification reply handler
 */
public class TransferNotificationActionHandler extends ActionHandler {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TransferNotificationActionHandler.class);

    private static final int ATR_RESULT_OUT_RANK = 0;
    private static final int ARCHIVE_UNIT_MAP_RANK = 0;
    private static final int DATAOBJECT_MAP_RANK = 1;
    private static final int BDO_OG_STORED_MAP_RANK = 2;
    private static final int DATAOBJECT_ID_TO_DATAOBJECT_DETAIL_MAP_RANK = 3;
    private static final int SEDA_PARAMETERS_RANK = 4;
    private static final int OBJECT_GROUP_ID_TO_GUID_MAP_RANK = 5;
    static final int HANDLER_IO_PARAMETER_NUMBER = 6;


    private static final String XML = ".xml";
    private static final String HANDLER_ID = "ATR_NOTIFICATION";
    private static final String NAMESPACE_URI = "fr:gouv:culture:archivesdefrance:seda:v2.0";
    private static final String XLINK_URI = "http://www.w3.org/1999/xlink";
    private static final String PREMIS_URI = "info:lc/xmlns/premis-v2";
    private static final String XSI_URI = "http://www.w3.org/2001/XMLSchema-instance";
    private static final String XSD_VERSION = "seda-2.0-main.xsd";

    private HandlerIO handlerIO;
    private static final String DEFAULT_STRATEGY = "default";
    private static final String EVENT_ID_PROCESS = "evIdProc";

    private final List<Class<?>> handlerInitialIOList = new ArrayList<>();
    private final MarshallerObjectCache marshallerObjectCache = new MarshallerObjectCache();
    private StatusCode workflowStatus = StatusCode.UNKNOWN;

    private boolean isBlankTestWorkflow = false;
    private static final String TEST_STATUS_PREFIX = "Test ";
    private String statusPrefix = "";

    /**
     * Constructor TransferNotificationActionHandler
     */
    public TransferNotificationActionHandler() {
        for (int i = 0; i < HANDLER_IO_PARAMETER_NUMBER; i++) {
            handlerInitialIOList.add(File.class);
        }
    }

    /**
     * @return HANDLER_ID
     */
    public static String getId() {
        return HANDLER_ID;
    }

    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO handler) {
        checkMandatoryParameters(params);
        final StorageClientFactory storageClientFactory = StorageClientFactory.getInstance();
        String eventDetailData;


        final ItemStatus itemStatus = new ItemStatus(HANDLER_ID);
        handlerIO = handler;
        try {
            workflowStatus =
                StatusCode.valueOf(params.getMapParameters().get(WorkerParameterName.workflowStatusKo));

            LogbookTypeProcess logbookTypeProcess = params.getLogbookTypeProcess();
            if (logbookTypeProcess != null && LogbookTypeProcess.INGEST_TEST.equals(logbookTypeProcess)) {
                isBlankTestWorkflow = true;
                statusPrefix = TEST_STATUS_PREFIX;
            }

            File atrFile;

            final LogbookOperation logbookOperation;
            try (LogbookOperationsClient client = LogbookOperationsClientFactory.getInstance().getClient()) {
                Select select = new Select();
                select.setQuery(QueryHelper.eq(EVENT_ID_PROCESS, params.getContainerName()));
                final JsonNode node = client.selectOperationById(params.getContainerName(), select.getFinalSelect());
                final JsonNode elmt = node.get("$results").get(0);
                if (elmt == null) {
                    LOGGER.error("Error while loading logbook operation: no result");
                    throw new ProcessingException("Error while loading logbook operation: no result");
                }
                logbookOperation = new LogbookOperation(elmt);
            } catch (final LogbookClientException e) {
                LOGGER.error("Error while loading logbook operation", e);
                throw new ProcessingException(e);
            } catch (InvalidCreateOperationException e) {
                LOGGER.error("Error while creating DSL query", e);
                throw new ProcessingException(e);
            }

            if (workflowStatus.isGreaterOrEqualToKo()) {
                atrFile = createATRKO(params, handlerIO, logbookOperation);
            } else {
                // CHeck is only done in OK mode since all parameters are optional
                checkMandatoryIOParameter(handler);
                atrFile = createATROK(params, handlerIO, logbookOperation);
            }
            // calculate digest by vitam alog
            final Digest vitamDigest = new Digest(VitamConfiguration.getDefaultDigestType());
            final String vitamDigestString = vitamDigest.update(atrFile).digestHex();

            LOGGER.debug(
                "DEBUG: \n\t" + vitamDigestString);
            // define eventDetailData
            eventDetailData =
                "{" +
                    "\"FileName\":\"" + "ATR_" + params.getContainerName() +
                    "\", \"MessageDigest\": \"" + vitamDigestString +
                    "\", \"Algorithm\": \"" + VitamConfiguration.getDefaultDigestType() + "\"}";

            itemStatus.setEvDetailData(eventDetailData);
            try {
                // TODO : Works for ATR_OK but not for some ATR_KO - need to be fixed
                ValidationXsdUtils.checkWithXSD(new FileInputStream(atrFile), XSD_VERSION);
            } catch (SAXException e) {
                if (e.getCause() == null) {
                    LOGGER.error("ATR File is not valid with the XSD", e);
                }
                LOGGER.error("ATR File is not a correct xml file", e);
            } catch (XMLStreamException e) {
                LOGGER.error("ATR File is not a correct xml file", e);
            }
            handler.addOuputResult(ATR_RESULT_OUT_RANK, atrFile, true, false);
            // store data object
            final ObjectDescription description = new ObjectDescription();
            description.setWorkspaceContainerGUID(params.getContainerName());
            description.setWorkspaceObjectURI(handler.getOutput(ATR_RESULT_OUT_RANK).getPath());
            try (final StorageClient storageClient = storageClientFactory.getClient()) {
                storageClient.storeFileFromWorkspace(
                    DEFAULT_STRATEGY,
                    DataCategory.REPORT,
                    params.getContainerName() + XML, description);

                if (!workflowStatus.isGreaterOrEqualToKo()) {
                    description.setWorkspaceObjectURI(
                        IngestWorkflowConstants.SEDA_FOLDER + "/" + IngestWorkflowConstants.SEDA_FILE);
                    storageClient.storeFileFromWorkspace(
                        DEFAULT_STRATEGY,
                        DataCategory.MANIFEST,
                        params.getContainerName() + XML, description);
                }
            }

            itemStatus.increment(StatusCode.OK);
        } catch (ProcessingException | ContentAddressableStorageException e) {
            LOGGER.error(e);
            itemStatus.increment(StatusCode.KO);
        } catch (URISyntaxException | InvalidParseOperationException |
            StorageClientException | IOException e) {
            LOGGER.error(e);
            itemStatus.increment(StatusCode.FATAL);
        }
        return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
    }

    /**
     * Serialize a Jaxb POJO object in the current XML stream
     *
     * @param jaxbPOJO jaxbPOJO
     * @throws ProcessingException ProcessingException
     */
    private void writeXMLFragment(Object jaxbPOJO, XMLStreamWriter xmlsw) throws ProcessingException {
        try {
            marshallerObjectCache.getMarshaller(jaxbPOJO.getClass()).marshal(jaxbPOJO, xmlsw);
        } catch (final JAXBException e) {
            throw new ProcessingException("Error on writing " + jaxbPOJO + "object", e);
        }

    }

    /**
     * createATROK When processing is Ok
     *
     * @param params  of type WorkerParameters
     * @param ioParam of type HandlerIO
     * @throws ProcessingException                ProcessingException
     * @throws URISyntaxException                 URISyntaxException
     * @throws ContentAddressableStorageException ContentAddressableStorageException
     * @throws IOException                        IOException
     * @throws InvalidParseOperationException     InvalidParseOperationException
     */
    private File createATROK(WorkerParameters params, HandlerIO ioParam, LogbookOperation logbookOperation)
        throws ProcessingException, URISyntaxException, ContentAddressableStorageException, IOException,
        InvalidParseOperationException {
        ParameterHelper.checkNullOrEmptyParameters(params);
        final File atrTmpFile = handlerIO.getNewLocalFile(handlerIO.getOutput(ATR_RESULT_OUT_RANK).getPath());

        // Pre-actions
        final InputStream archiveUnitMapTmpFile = new FileInputStream((File) handlerIO.getInput(ARCHIVE_UNIT_MAP_RANK));
        final Map<String, Object> archiveUnitSystemGuid = JsonHandler.getMapFromInputStream(archiveUnitMapTmpFile);
        final InputStream dataObjectMapTmpFile =
            new FileInputStream((File) handlerIO.getInput(DATAOBJECT_MAP_RANK));
        final Map<String, Object> dataObjectSystemGuid = JsonHandler.getMapFromInputStream(dataObjectMapTmpFile);
        final InputStream bdoObjectGroupStoredMapTmpFile =
            new FileInputStream((File) handlerIO.getInput(BDO_OG_STORED_MAP_RANK));
        final Map<String, Object> bdoObjectGroupSystemGuid =
            JsonHandler.getMapFromInputStream(bdoObjectGroupStoredMapTmpFile);
        final InputStream dataObjectToDetailDataObjectMapTmpFile =
            new FileInputStream((File) handlerIO.getInput(DATAOBJECT_ID_TO_DATAOBJECT_DETAIL_MAP_RANK));
        final Map<String, DataObjectDetail> dataObjectToDetailDataObject =
            JsonHandler.getMapFromInputStream(dataObjectToDetailDataObjectMapTmpFile, DataObjectDetail.class);
        final InputStream objectGroupSystemGuidTmpFile =
            new FileInputStream((File) handlerIO.getInput(OBJECT_GROUP_ID_TO_GUID_MAP_RANK));
        final Map<String, Object> objectGroupSystemGuid =
            JsonHandler.getMapFromInputStream(objectGroupSystemGuidTmpFile);

        final SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

        final JsonNode sedaParameters = JsonHandler.getFromFile((File) handlerIO.getInput(SEDA_PARAMETERS_RANK));
        final JsonNode infoATR =
            sedaParameters.get(SedaConstants.TAG_ARCHIVE_TRANSFER);
        final String messageIdentifier = infoATR.get(SedaConstants.TAG_MESSAGE_IDENTIFIER).asText();

        // creation of ATR report
        try (FileWriter artTmpFileWriter = new FileWriter(atrTmpFile)) {
            final XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();

            final XMLStreamWriter xmlsw = outputFactory.createXMLStreamWriter(artTmpFileWriter);
            xmlsw.writeStartDocument();

            xmlsw.writeStartElement(SedaConstants.TAG_ARCHIVE_TRANSFER_REPLY);

            xmlsw.writeNamespace(SedaConstants.NAMESPACE_XLINK, XLINK_URI);
            xmlsw.writeNamespace(SedaConstants.NAMESPACE_PR, PREMIS_URI);
            xmlsw.writeDefaultNamespace(NAMESPACE_URI);
            xmlsw.writeNamespace(SedaConstants.NAMESPACE_XSI, XSI_URI);
            xmlsw.writeAttribute(SedaConstants.NAMESPACE_XSI, XSI_URI, SedaConstants.ATTRIBUTE_SCHEMA_LOCATION,
                NAMESPACE_URI + XSD_VERSION);

            writeAttributeValue(xmlsw, SedaConstants.TAG_DATE, sdfDate.format(new Date()));
            writeAttributeValue(xmlsw, SedaConstants.TAG_MESSAGE_IDENTIFIER, params.getContainerName());

            writeAttributeValue(xmlsw, SedaConstants.TAG_ARCHIVAL_AGREEMENT,
                (infoATR.get(SedaConstants.TAG_ARCHIVAL_AGREEMENT) != null)
                    ? infoATR.get(SedaConstants.TAG_ARCHIVAL_AGREEMENT).textValue() : "");

            if (logbookOperation.get(LogbookMongoDbName.eventDetailData.getDbname()) != null) {
                final JsonNode evDetDataNode = JsonHandler.getFromString(
                    logbookOperation.get(LogbookMongoDbName.eventDetailData.getDbname()).toString());
                if (evDetDataNode.get(SedaConstants.TAG_ARCHIVE_PROFILE) != null) {
                    final String profilId = evDetDataNode.get(SedaConstants.TAG_ARCHIVE_PROFILE).asText();
                    writeAttributeValue(xmlsw, SedaConstants.TAG_ARCHIVE_PROFILE, profilId);
                }
            }

            xmlsw.writeStartElement(SedaConstants.TAG_CODE_LIST_VERSIONS);
            if (infoATR.get(SedaConstants.TAG_CODE_LIST_VERSIONS) != null) {
                writeAttributeValue(xmlsw, SedaConstants.TAG_REPLY_CODE_LIST_VERSION,
                    (infoATR.get(SedaConstants.TAG_CODE_LIST_VERSIONS)
                        .get(SedaConstants.TAG_REPLY_CODE_LIST_VERSION) != null)
                        ? infoATR.get(SedaConstants.TAG_CODE_LIST_VERSIONS)
                        .get(SedaConstants.TAG_REPLY_CODE_LIST_VERSION)
                        .textValue()
                        : "");
                writeAttributeValue(xmlsw, SedaConstants.TAG_MESSAGE_DIGEST_ALGORITHM_CODE_LIST_VERSION,
                    (infoATR.get(SedaConstants.TAG_CODE_LIST_VERSIONS)
                        .get(SedaConstants.TAG_MESSAGE_DIGEST_ALGORITHM_CODE_LIST_VERSION) != null)
                        ? infoATR.get(SedaConstants.TAG_CODE_LIST_VERSIONS)
                        .get(SedaConstants.TAG_MESSAGE_DIGEST_ALGORITHM_CODE_LIST_VERSION).textValue()
                        : "");
                writeAttributeValue(xmlsw, SedaConstants.TAG_FILE_FORMAT_CODE_LIST_VERSION,
                    (infoATR.get(SedaConstants.TAG_CODE_LIST_VERSIONS)
                        .get(SedaConstants.TAG_FILE_FORMAT_CODE_LIST_VERSION) != null)
                        ? infoATR.get(SedaConstants.TAG_CODE_LIST_VERSIONS)
                        .get(SedaConstants.TAG_FILE_FORMAT_CODE_LIST_VERSION)
                        .textValue()
                        : "");
            }
            xmlsw.writeEndElement(); // END SedaConstants.TAG_CODE_LIST_VERSIONS

            xmlsw.writeStartElement(SedaConstants.TAG_DATA_OBJECT_PACKAGE);

            writeAttributeValue(xmlsw, SedaConstants.TAG_DESCRIPTIVE_METADATA, null);

            xmlsw.writeStartElement(SedaConstants.TAG_MANAGEMENT_METADATA);
            xmlsw.writeStartElement(SedaConstants.TAG_REPLY_OUTCOME);

            // if status equals WARNING, we'll add informations here
            if (workflowStatus.WARNING.name().equals(workflowStatus.name())) {
                List<String> statusToBeChecked = new ArrayList();
                statusToBeChecked.add(StatusCode.WARNING.toString());
                final List<Document> logbookOperationEvents =
                    (List<Document>) logbookOperation.get(LogbookDocument.EVENTS.toString());
                xmlsw.writeStartElement(SedaConstants.TAG_OPERATION);
                for (final Document event : logbookOperationEvents) {
                    writeEvent(xmlsw, event, SedaConstants.TAG_OPERATION, statusToBeChecked);
                }
                xmlsw.writeEndElement(); // END SedaConstants.TAG_OPERATION
            }

            xmlsw.writeStartElement(SedaConstants.TAG_ARCHIVE_UNIT_LIST); // START ARCHIVE_UNIT_LIST
            if (archiveUnitSystemGuid != null) {
                if (workflowStatus.WARNING.name().equals(workflowStatus.name())) {
                    handleWarningArchiveUnits(archiveUnitSystemGuid, xmlsw, params);
                } else {
                    for (final Map.Entry<String, Object> entry : archiveUnitSystemGuid.entrySet()) {
                        final ArchiveUnitReplyTypeRoot au = new ArchiveUnitReplyTypeRoot();
                        au.setId(entry.getKey());
                        au.setSystemId(entry.getValue().toString());
                        writeXMLFragment(au, xmlsw);
                    }
                }
            }
            xmlsw.writeEndElement(); // END ARCHIVE_UNIT_LIST

            xmlsw.writeStartElement(SedaConstants.TAG_DATA_OBJECT_LIST);
            // Set to known which DOGIG has already be used in the XML
            if (dataObjectSystemGuid != null) {
                if (workflowStatus.WARNING.name().equals(workflowStatus.name())) {
                    handleWarningGots(dataObjectSystemGuid, xmlsw, params, bdoObjectGroupSystemGuid,
                        dataObjectToDetailDataObject, objectGroupSystemGuid);
                } else {
                    final Set<String> usedDataObjectGroup = new HashSet<>();
                    for (final Map.Entry<String, Object> entry : dataObjectSystemGuid.entrySet()) {
                        final String dataOGID = bdoObjectGroupSystemGuid.get(entry.getKey()).toString();
                        final DataObjectDetail dataObjectDetail = dataObjectToDetailDataObject.get(entry.getKey());
                        final DataObjectTypeRoot dotr;
                        if (dataObjectDetail.isPhysical()) {
                            dotr = new PhysicalDataObjectTypeRoot();
                        } else {
                            dotr = new BinaryDataObjectTypeRoot();
                        }
                        if (dataObjectSystemGuid.get(entry.getKey()) != null) {
                            dotr.setDataObjectSystemId(dataObjectSystemGuid.get(entry.getKey()).toString());
                        }

                        // final BinaryDataObjectTypeRoot dotr = new BinaryDataObjectTypeRoot();
                        dotr.setId(entry.getKey());
                        // Test if the DOGID has already be used . If so, use DOGRefID, else DOGID in the SEDA XML
                        if (usedDataObjectGroup.contains(dataOGID)) {
                            dotr.setDataObjectGroupRefId(dataOGID);
                        } else {
                            dotr.setDataObjectGroupId(dataOGID);
                            usedDataObjectGroup.add(dataOGID);
                        }
                        dotr.setDataObjectVersion(dataObjectDetail.getVersion());
                        dotr.setDataObjectGroupSystemId(objectGroupSystemGuid.get(dataOGID).toString());
                        writeXMLFragment(dotr, xmlsw);
                    }
                }
            }
            xmlsw.writeEndElement();// END DATA_OBJECT_LIST

            xmlsw.writeEndElement(); // END ARCHIVE_UNIT_LIST

            xmlsw.writeEndElement(); // END REPLY_OUTCOME
            xmlsw.writeEndElement(); // END MANAGEMENT_METADATA

            writeAttributeValue(xmlsw, SedaConstants.TAG_REPLY_CODE, statusPrefix + workflowStatus.name());

            writeAttributeValue(xmlsw, SedaConstants.TAG_MESSAGE_REQUEST_IDENTIFIER, messageIdentifier);


            if (!isBlankTestWorkflow) {
                writeAttributeValue(xmlsw, SedaConstants.TAG_GRANT_DATE, sdfDate.format(new Date()));
            }

            xmlsw.writeStartElement(SedaConstants.TAG_ARCHIVAL_AGENCY);
            if (infoATR.get(SedaConstants.TAG_ARCHIVAL_AGENCY) != null) {
                writeAttributeValue(xmlsw, SedaConstants.TAG_IDENTIFIER,
                    (infoATR.get(SedaConstants.TAG_ARCHIVAL_AGENCY).get(SedaConstants.TAG_IDENTIFIER) != null)
                        ? infoATR.get(SedaConstants.TAG_ARCHIVAL_AGENCY).get(SedaConstants.TAG_IDENTIFIER).textValue()
                        : "");
            }
            xmlsw.writeEndElement(); // END SedaConstants.TAG_ARCHIVAL_AGENCY


            xmlsw.writeStartElement(SedaConstants.TAG_TRANSFERRING_AGENCY);
            if (infoATR.get(SedaConstants.TAG_TRANSFERRING_AGENCY) != null) {
                writeAttributeValue(xmlsw, SedaConstants.TAG_IDENTIFIER,
                    (infoATR.get(SedaConstants.TAG_TRANSFERRING_AGENCY).get(SedaConstants.TAG_IDENTIFIER) != null)
                        ? infoATR.get(SedaConstants.TAG_TRANSFERRING_AGENCY).get(SedaConstants.TAG_IDENTIFIER)
                        .textValue()
                        : "");
            }
            xmlsw.writeEndElement(); // END SedaConstants.TAG_TRANSFERRING_AGENCY
            xmlsw.writeEndElement();

            xmlsw.writeEndDocument();
            xmlsw.flush();
            xmlsw.close();

        } catch (XMLStreamException | IOException e) {
            LOGGER.error("Error of response generation");
            throw new ProcessingException(e);
        }
        return atrTmpFile;
    }

    private void handleWarningGots(Map<String, Object> dataObjectSystemGuid, XMLStreamWriter xmlsw,
        WorkerParameters params, Map<String, Object> bdoObjectGroupSystemGuid,
        Map<String, DataObjectDetail> dataObjectToDetailDataObject, Map<String, Object> objectGroupSystemGuid)
        throws ProcessingException, XMLStreamException {
        List<String> statusToBeChecked = new ArrayList<>();
        statusToBeChecked.add(StatusCode.WARNING.toString());
        final Set<String> usedDataObjectGroup = new HashSet<>();
        try (LogbookLifeCyclesClient client = LogbookLifeCyclesClientFactory.getInstance().getClient()) {
            LifecyclesSpliterator<JsonNode>
                lifecyclesSpliterator = handleLogbookLifeCyclesObjectGroup(params.getContainerName(), client,
                LifeCycleStatusCode.LIFE_CYCLE_IN_PROCESS);

            final Map<String, String> objectGroupGuid = new HashMap<>();
            final Map<String, List<String>> dataObjectsForOG = new HashMap<>();

            for (final Map.Entry<String, Object> entry : bdoObjectGroupSystemGuid.entrySet()) {
                final String idOG = entry.getValue().toString();
                final String idObj = entry.getKey();
                if (!dataObjectsForOG.containsKey(idOG)) {
                    final List<String> listObj = new ArrayList<>();
                    listObj.add(idObj);
                    dataObjectsForOG.put(idOG, listObj);
                } else {
                    dataObjectsForOG.get(idOG).add(idObj);
                }
            }
            if (objectGroupSystemGuid != null) {
                for (final Map.Entry<String, Object> entry : objectGroupSystemGuid.entrySet()) {
                    final String guid = entry.getValue().toString();
                    objectGroupGuid.put(guid, entry.getKey());
                }
            }
            StreamSupport.stream(lifecyclesSpliterator, false)
                .map(LogbookLifeCycleObjectGroupInProcess::new)
                .forEach(logbookLifeCycleObjectGroup -> handleXmlCreationTagDataObjectGroup(dataObjectSystemGuid, xmlsw,
                    bdoObjectGroupSystemGuid,
                    dataObjectToDetailDataObject,
                    objectGroupSystemGuid, statusToBeChecked, usedDataObjectGroup, objectGroupGuid,
                    dataObjectsForOG,
                    logbookLifeCycleObjectGroup));

        } catch (LogbookClientException | IllegalArgumentException e) {
            throw new ProcessingException(e);
        }
    }

    private void handleXmlCreationTagDataObjectGroup(Map<String, Object> dataObjectSystemGuid, XMLStreamWriter xmlsw,
        Map<String, Object> bdoObjectGroupSystemGuid, Map<String, DataObjectDetail> dataObjectToDetailDataObject,
        Map<String, Object> objectGroupSystemGuid, List<String> statusToBeChecked, Set<String> usedDataObjectGroup,
        Map<String, String> objectGroupGuid, Map<String, List<String>> dataObjectsForOG,
        LogbookLifeCycleObjectGroupInProcess logbookLifeCycleObjectGroup) {
        try {
            final String eventIdentifier = null;
            xmlsw.writeStartElement(SedaConstants.TAG_DATA_OBJECT_GROUP);

            final String ogGUID =
                logbookLifeCycleObjectGroup != null &&
                    logbookLifeCycleObjectGroup.get(LogbookMongoDbName.objectIdentifier.getDbname()) != null
                    ? logbookLifeCycleObjectGroup.get(LogbookMongoDbName.objectIdentifier.getDbname())
                    .toString()
                    : "";
            String igId = "";
            if (objectGroupGuid.containsKey(ogGUID)) {
                igId = objectGroupGuid.get(ogGUID);
            }
            if (dataObjectsForOG.get(igId) != null) {
                List<Document> logbookLifeCycleObjectGroupEvents = new ArrayList<>();
                if (logbookLifeCycleObjectGroup != null) {
                    logbookLifeCycleObjectGroupEvents =
                        (List<Document>) logbookLifeCycleObjectGroup
                            .get(LogbookDocument.EVENTS.toString());
                }
                for (final String idObj : dataObjectsForOG.get(igId)) {
                    DataObjectDetail dataObjectDetail = dataObjectToDetailDataObject.get(idObj);
                    DataObjectTypeRoot dotr;
                    final String dataOGID = bdoObjectGroupSystemGuid.get(idObj).toString();
                    if (dataObjectDetail != null && dataObjectDetail.isPhysical()) {
                        dotr = new PhysicalDataObjectTypeRoot();
                    } else {
                        dotr = new BinaryDataObjectTypeRoot();
                    }
                    if (dataObjectSystemGuid.get(idObj) != null) {
                        dotr.setDataObjectSystemId(dataObjectSystemGuid.get(idObj).toString());
                    }
                    dotr.setId(idObj);
                    // Test if the DOGID has already be used . If so, use DOGRefID, else DOGID in the SEDA XML
                    if (usedDataObjectGroup.contains(dataOGID)) {
                        dotr.setDataObjectGroupRefId(dataOGID);
                    } else {
                        dotr.setDataObjectGroupId(dataOGID);
                        usedDataObjectGroup.add(dataOGID);
                    }
                    if (dataObjectDetail != null) {
                        dotr.setDataObjectVersion(dataObjectDetail.getVersion());
                    }
                    dotr.setDataObjectGroupSystemId(objectGroupSystemGuid.get(dataOGID).toString());
                    writeXMLFragment(dotr, xmlsw);
                    if (dataObjectSystemGuid.get(idObj) != null) {
                        for (final Document event : logbookLifeCycleObjectGroupEvents) {
                            writeEvent(xmlsw, new Document(event), SedaConstants.TAG_DATA_OBJECT_GROUP,
                                statusToBeChecked);
                        }
                    }
                }
            }
            xmlsw.writeEndElement(); // END SedaConstants.TAG_DATA_OBJECT_GROUP
        } catch (ProcessingException | XMLStreamException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private void handleWarningArchiveUnits(Map<String, Object> archiveUnitSystemGuid, XMLStreamWriter xmlsw,
        WorkerParameters params) throws ProcessingException, XMLStreamException {
        List<String> statusToBeChecked = new ArrayList();
        statusToBeChecked.add(StatusCode.WARNING.toString());

        Map<String, String> systemGuidArchiveUnitId = new HashMap<>();

        if (archiveUnitSystemGuid != null) {
            for (final Map.Entry<String, Object> entry : archiveUnitSystemGuid.entrySet()) {
                systemGuidArchiveUnitId.put(entry.getValue().toString(), entry.getKey());
            }
        }

        try (LogbookLifeCyclesClient client = LogbookLifeCyclesClientFactory.getInstance().getClient()) {
            try {
                LifecyclesSpliterator<JsonNode> lifecyclesSpliterator =
                    handlerLogbookLifeCycleUnit(params.getContainerName(), client,
                        LifeCycleStatusCode.LIFE_CYCLE_IN_PROCESS);

                StreamSupport.stream(lifecyclesSpliterator, false)
                    .map(LogbookLifeCycleUnitInProcess::new)
                    .forEach(logbookLifeCycleUnit -> handleXmlCreationTagArchiveUnit(xmlsw, statusToBeChecked,
                        systemGuidArchiveUnitId, logbookLifeCycleUnit));

            } catch (final LogbookClientException | IllegalStateException | IllegalArgumentException e) {
                throw new ProcessingException(e);
            }
        }

        if (systemGuidArchiveUnitId != null) {
            for (final Map.Entry<String, String> entry : systemGuidArchiveUnitId.entrySet()) {
                final ArchiveUnitReplyTypeRoot au = new ArchiveUnitReplyTypeRoot();
                au.setId(entry.getValue());
                au.setSystemId(entry.getKey());
                writeXMLFragment(au, xmlsw);
            }
        }

    }

    private void handleXmlCreationTagArchiveUnit(XMLStreamWriter xmlsw, List<String> statusToBeChecked,
        Map<String, String> systemGuidArchiveUnitId, LogbookLifeCycleUnitInProcess logbookLifeCycleUnit) {
        try {
            final List<Document> logbookLifeCycleUnitEvents =
                (List<Document>) logbookLifeCycleUnit
                    .get(LogbookDocument.EVENTS.toString());

            xmlsw.writeStartElement(SedaConstants.TAG_ARCHIVE_UNIT);

            if (!systemGuidArchiveUnitId.isEmpty() &&
                logbookLifeCycleUnit.get(SedaConstants.PREFIX_ID) != null &&
                systemGuidArchiveUnitId.get(logbookLifeCycleUnit.get(SedaConstants.PREFIX_ID).toString()) != null) {
                xmlsw.writeAttribute(SedaConstants.ATTRIBUTE_ID,
                    systemGuidArchiveUnitId
                        .get(logbookLifeCycleUnit.get(SedaConstants.PREFIX_ID).toString()));
                writeAttributeValue(xmlsw, SedaConstants.TAG_ARCHIVE_SYSTEM_ID,
                    logbookLifeCycleUnit.get(SedaConstants.PREFIX_ID).toString());
                systemGuidArchiveUnitId
                    .remove(logbookLifeCycleUnit.get(SedaConstants.PREFIX_ID).toString());
            }

            for (Document event : logbookLifeCycleUnitEvents) {
                writeEvent(xmlsw, event, SedaConstants.TAG_ARCHIVE_UNIT,
                    statusToBeChecked);
            }
            //                        }
            xmlsw.writeEndElement(); // END SedaConstants.TAG_ARCHIVE_UNIT

        } catch (XMLStreamException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * createATRKO when workflowStatus.isGreaterOrEqualToKo()
     *
     * @param params  of type WorkerParameters
     * @param ioParam of type HandlerIO
     * @throws ProcessingException                when execute process failed
     * @throws URISyntaxException                 URISyntaxException
     * @throws ContentAddressableStorageException ContentAddressableStorageException
     * @throws IOException                        IOException
     * @throws InvalidParseOperationException     InvalidParseOperationException
     */

    private File createATRKO(WorkerParameters params, HandlerIO ioParam, LogbookOperation logbookOperation)
        throws ProcessingException, URISyntaxException, ContentAddressableStorageException, IOException,
        InvalidParseOperationException {
        ParameterHelper.checkNullOrEmptyParameters(params);
        final File atrTmpFile = handlerIO.getNewLocalFile(handlerIO.getOutput(ATR_RESULT_OUT_RANK).getPath());

        // Pre-actions
        final SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

        JsonNode infoATR = null;
        String messageIdentifier = "";
        if (handlerIO.getInput(SEDA_PARAMETERS_RANK) != null) {
            final JsonNode sedaParameters = JsonHandler.getFromFile((File) handlerIO.getInput(SEDA_PARAMETERS_RANK));
            infoATR =
                sedaParameters.get(SedaConstants.TAG_ARCHIVE_TRANSFER);
            if (infoATR != null && infoATR.get(SedaConstants.TAG_MESSAGE_IDENTIFIER) != null) {
                messageIdentifier = infoATR.get(SedaConstants.TAG_MESSAGE_IDENTIFIER).asText();
            }
        }
        // creation of ATR report
        try (FileWriter artTmpFileWriter = new FileWriter(atrTmpFile)) {
            final XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();

            final XMLStreamWriter xmlsw = outputFactory.createXMLStreamWriter(artTmpFileWriter);
            xmlsw.writeStartDocument();

            xmlsw.writeStartElement(SedaConstants.TAG_ARCHIVE_TRANSFER_REPLY);

            xmlsw.writeNamespace(SedaConstants.NAMESPACE_XLINK, XLINK_URI);
            xmlsw.writeNamespace(SedaConstants.NAMESPACE_PR, PREMIS_URI);
            xmlsw.writeDefaultNamespace(NAMESPACE_URI);
            xmlsw.writeNamespace(SedaConstants.NAMESPACE_XSI, XSI_URI);
            xmlsw.writeAttribute(SedaConstants.NAMESPACE_XSI, XSI_URI, SedaConstants.ATTRIBUTE_SCHEMA_LOCATION,
                NAMESPACE_URI + XSD_VERSION);

            writeAttributeValue(xmlsw, SedaConstants.TAG_DATE, sdfDate.format(new Date()));
            writeAttributeValue(xmlsw, SedaConstants.TAG_MESSAGE_IDENTIFIER, params.getContainerName());

            if (logbookOperation.get(LogbookMongoDbName.eventDetailData.getDbname()) != null) {
                final JsonNode evDetDataNode = JsonHandler.getFromString(
                    logbookOperation.get(LogbookMongoDbName.eventDetailData.getDbname()).toString());
                if (evDetDataNode.get(SedaConstants.TAG_ARCHIVE_PROFILE) != null) {
                    final String profilId = evDetDataNode.get(SedaConstants.TAG_ARCHIVE_PROFILE).asText();
                    writeAttributeValue(xmlsw, SedaConstants.TAG_ARCHIVE_PROFILE, profilId);
                }
            }

            if (infoATR != null) {
                writeAttributeValue(xmlsw, SedaConstants.TAG_ARCHIVAL_AGREEMENT,
                    (infoATR.get(SedaConstants.TAG_ARCHIVAL_AGREEMENT) != null)
                        ? infoATR.get(SedaConstants.TAG_ARCHIVAL_AGREEMENT).textValue() : "");

                xmlsw.writeStartElement(SedaConstants.TAG_CODE_LIST_VERSIONS);
                if (infoATR.get(SedaConstants.TAG_CODE_LIST_VERSIONS) != null) {
                    writeAttributeValue(xmlsw, SedaConstants.TAG_REPLY_CODE_LIST_VERSION,
                        (infoATR.get(SedaConstants.TAG_CODE_LIST_VERSIONS)
                            .get(SedaConstants.TAG_REPLY_CODE_LIST_VERSION) != null)
                            ? infoATR.get(SedaConstants.TAG_CODE_LIST_VERSIONS)
                            .get(SedaConstants.TAG_REPLY_CODE_LIST_VERSION)
                            .textValue()
                            : "");
                    writeAttributeValue(xmlsw, SedaConstants.TAG_MESSAGE_DIGEST_ALGORITHM_CODE_LIST_VERSION,
                        (infoATR.get(SedaConstants.TAG_CODE_LIST_VERSIONS)
                            .get(SedaConstants.TAG_MESSAGE_DIGEST_ALGORITHM_CODE_LIST_VERSION) != null)
                            ? infoATR.get(SedaConstants.TAG_CODE_LIST_VERSIONS)
                            .get(SedaConstants.TAG_MESSAGE_DIGEST_ALGORITHM_CODE_LIST_VERSION).textValue()
                            : "");
                    writeAttributeValue(xmlsw, SedaConstants.TAG_FILE_FORMAT_CODE_LIST_VERSION,
                        (infoATR.get(SedaConstants.TAG_CODE_LIST_VERSIONS)
                            .get(SedaConstants.TAG_FILE_FORMAT_CODE_LIST_VERSION) != null)
                            ? infoATR.get(SedaConstants.TAG_CODE_LIST_VERSIONS)
                            .get(SedaConstants.TAG_FILE_FORMAT_CODE_LIST_VERSION)
                            .textValue()
                            : "");
                }
                xmlsw.writeEndElement(); // END SedaConstants.TAG_CODE_LIST_VERSIONS
            }

            xmlsw.writeStartElement(SedaConstants.TAG_DATA_OBJECT_PACKAGE);

            writeAttributeValue(xmlsw, SedaConstants.TAG_DESCRIPTIVE_METADATA, null);

            xmlsw.writeStartElement(SedaConstants.TAG_MANAGEMENT_METADATA);

            xmlsw.writeStartElement(SedaConstants.TAG_REPLY_OUTCOME);

            addKOReplyOutcomeIterator(xmlsw, params.getContainerName(), logbookOperation);

            xmlsw.writeEndElement(); // END REPLY_OUTCOME
            xmlsw.writeEndElement(); // END MANAGEMENT_METADATA
            xmlsw.writeEndElement(); // END DATA_OBJECT_PACKAGE

            writeAttributeValue(xmlsw, SedaConstants.TAG_REPLY_CODE, statusPrefix + workflowStatus.name());

            writeAttributeValue(xmlsw, SedaConstants.TAG_MESSAGE_REQUEST_IDENTIFIER, messageIdentifier);

            if (!isBlankTestWorkflow) {
                writeAttributeValue(xmlsw, SedaConstants.TAG_GRANT_DATE, sdfDate.format(new Date()));
            }


            xmlsw.writeStartElement(SedaConstants.TAG_ARCHIVAL_AGENCY);
            if (infoATR != null && infoATR.get(SedaConstants.TAG_ARCHIVAL_AGENCY) != null) {
                writeAttributeValue(xmlsw, SedaConstants.TAG_IDENTIFIER,
                    (infoATR.get(SedaConstants.TAG_ARCHIVAL_AGENCY).get(SedaConstants.TAG_IDENTIFIER) != null)
                        ? infoATR.get(SedaConstants.TAG_ARCHIVAL_AGENCY).get(SedaConstants.TAG_IDENTIFIER).textValue()
                        : "");
            }
            xmlsw.writeEndElement(); // END SedaConstants.TAG_ARCHIVAL_AGENCY


            xmlsw.writeStartElement(SedaConstants.TAG_TRANSFERRING_AGENCY);
            if (infoATR != null && infoATR.get(SedaConstants.TAG_TRANSFERRING_AGENCY) != null) {
                writeAttributeValue(xmlsw, SedaConstants.TAG_IDENTIFIER,
                    (infoATR.get(SedaConstants.TAG_TRANSFERRING_AGENCY).get(SedaConstants.TAG_IDENTIFIER) != null)
                        ? infoATR.get(SedaConstants.TAG_TRANSFERRING_AGENCY).get(SedaConstants.TAG_IDENTIFIER)
                        .textValue()
                        : "");
            }
            xmlsw.writeEndElement(); // END SedaConstants.TAG_TRANSFERRING_AGENCY
            xmlsw.writeEndElement();

            xmlsw.writeEndDocument();
            xmlsw.flush();
            xmlsw.close();

        } catch (XMLStreamException | IOException | InvalidCreateOperationException e) {
            LOGGER.error("Error of response generation");
            throw new ProcessingException(e);
        }
        return atrTmpFile;
    }

    private LifecyclesSpliterator<JsonNode> handlerLogbookLifeCycleUnit(String operationId,
        LogbookLifeCyclesClient client, LifeCycleStatusCode lifeCycleStatusCode) throws LogbookClientException {
        final Select select = new Select();
        LifecyclesSpliterator<JsonNode> scrollRequest = new LifecyclesSpliterator<>(select,
            query -> {
                RequestResponse response;
                try {
                    response = client.unitLifeCyclesByOperationIterator(operationId,
                        lifeCycleStatusCode, select.getFinalSelect());
                } catch (LogbookClientException | InvalidParseOperationException e) {
                    throw new IllegalStateException(e);
                }
                if (response.isOk()) {
                    return (RequestResponseOK) response;
                } else {
                    throw new IllegalStateException(
                        String.format("Error while loading logbook lifecycle Unit RequestResponse %d",
                            response.getHttpCode()));
                }
            }, VitamConfiguration.getDefaultOffset(), VitamConfiguration.getBatchSize());
        return scrollRequest;
    }


    /**
     * Add the KO (which could be KO or FATAL) replyOutcome to the ATR xml
     *
     * @param xmlsw         xml writer
     * @param containerName the operation identifier
     * @throws ProcessingException             thrown if a logbook could not be retrieved
     * @throws XMLStreamException              XMLStreamException
     * @throws FileNotFoundException           FileNotFoundException
     * @throws InvalidParseOperationException  InvalidParseOperationException
     * @throws InvalidCreateOperationException InvalidCreateOperationException
     */
    private void addKOReplyOutcomeIterator(XMLStreamWriter xmlsw, String containerName,
        LogbookOperation logbookOperation)
        throws ProcessingException, XMLStreamException, FileNotFoundException, InvalidParseOperationException,
        InvalidCreateOperationException {
        List<String> statusToBeChecked = new ArrayList();
        statusToBeChecked.add(StatusCode.FATAL.toString());
        statusToBeChecked.add(StatusCode.KO.toString());
        final List<Document> logbookOperationEvents =
            (List<Document>) logbookOperation.get(LogbookDocument.EVENTS.toString());
        xmlsw.writeStartElement(SedaConstants.TAG_OPERATION);
        for (final Document event : logbookOperationEvents) {
            writeEvent(xmlsw, event, SedaConstants.TAG_OPERATION, statusToBeChecked);
        }
        xmlsw.writeEndElement(); // END SedaConstants.TAG_OPERATION

        try (LogbookLifeCyclesClient client = LogbookLifeCyclesClientFactory.getInstance().getClient()) {
            try {

                Map<String, Object> archiveUnitSystemGuid = null;
                InputStream archiveUnitMapTmpFile = null;
                final File file = (File) handlerIO.getInput(ARCHIVE_UNIT_MAP_RANK);
                if (file != null) {
                    archiveUnitMapTmpFile = new FileInputStream(file);
                }
                Map<String, String> systemGuidArchiveUnitId = new HashMap<>();

                if (archiveUnitMapTmpFile != null) {
                    archiveUnitSystemGuid = JsonHandler.getMapFromInputStream(archiveUnitMapTmpFile);
                    if (archiveUnitSystemGuid != null) {
                        for (final Map.Entry<String, Object> entry : archiveUnitSystemGuid.entrySet()) {
                            systemGuidArchiveUnitId.put(entry.getValue().toString(), entry.getKey());
                        }
                    }
                }

                xmlsw.writeStartElement(SedaConstants.TAG_ARCHIVE_UNIT_LIST);

                LifecyclesSpliterator<JsonNode> scrollRequest =
                    handlerLogbookLifeCycleUnit(containerName, client, LifeCycleStatusCode.LIFE_CYCLE_IN_PROCESS);

                // Iterate over all response in LifecyclesSpliterator
                StreamSupport.stream(scrollRequest, false)
                    .map(LogbookLifeCycleUnitInProcess::new)
                    .forEach(logbookLifeCycleUnit -> handleXmlCreationTagArchiveUnitATRKo(xmlsw, statusToBeChecked,
                        systemGuidArchiveUnitId, logbookLifeCycleUnit));
                xmlsw.writeEndElement(); // END SedaConstants.TAG_ARCHIVE_UNIT_LIST
            } catch (final IllegalStateException | LogbookClientException e) {
                throw new ProcessingException(e);
            }
            try {

                Map<String, Object> doObjectGroupSystemGuid = new HashMap<>();
                final Map<String, String> objectGroupGuid = new HashMap<>();
                final Map<String, List<String>> dataObjectsForOG = new HashMap<>();
                final File file1 = (File) handlerIO.getInput(DATAOBJECT_MAP_RANK);
                final File file2 = (File) handlerIO.getInput(BDO_OG_STORED_MAP_RANK);
                final File file3 = (File) handlerIO.getInput(OBJECT_GROUP_ID_TO_GUID_MAP_RANK);
                final File file4 = (File) handlerIO.getInput(DATAOBJECT_ID_TO_DATAOBJECT_DETAIL_MAP_RANK);

                Map<String, Object> dataObjectSystemGuid;
                if (file1 != null && file2 != null) {
                    final InputStream binaryDataObjectMapTmpFile = new FileInputStream(file1);
                    final InputStream bdoObjectGroupStoredMapTmpFile = new FileInputStream(file2);

                    dataObjectSystemGuid = JsonHandler.getMapFromInputStream(binaryDataObjectMapTmpFile);
                    doObjectGroupSystemGuid = JsonHandler.getMapFromInputStream(bdoObjectGroupStoredMapTmpFile);
                } else {
                    dataObjectSystemGuid = new HashMap<>();
                }
                for (final Map.Entry<String, Object> entry : doObjectGroupSystemGuid.entrySet()) {
                    final String idOG = entry.getValue().toString();
                    final String idObj = entry.getKey();
                    if (!dataObjectsForOG.containsKey(idOG)) {
                        final List<String> listObj = new ArrayList<>();
                        listObj.add(idObj);
                        dataObjectsForOG.put(idOG, listObj);
                    } else {
                        dataObjectsForOG.get(idOG).add(idObj);
                    }
                }
                if (file3 != null) {
                    final InputStream objectGroupGuidMapTmpFile = new FileInputStream(file3);
                    final Map<String, Object> objectGroupGuidBefore =
                        JsonHandler.getMapFromInputStream(objectGroupGuidMapTmpFile);
                    if (objectGroupGuidBefore != null) {
                        for (final Map.Entry<String, Object> entry : objectGroupGuidBefore.entrySet()) {
                            final String guid = entry.getValue().toString();
                            objectGroupGuid.put(guid, entry.getKey());
                        }
                    }
                }

                Map<String, DataObjectDetail> dataObjectToDetailDataObject;

                if (file4 != null) {
                    final InputStream dataObjectToDetailDataObjectMapTmpFile = new FileInputStream(file4);
                    dataObjectToDetailDataObject =
                        JsonHandler.getMapFromInputStream(dataObjectToDetailDataObjectMapTmpFile,
                            DataObjectDetail.class);
                } else {
                    dataObjectToDetailDataObject = new HashMap<>();
                }

                xmlsw.writeStartElement(SedaConstants.TAG_DATA_OBJECT_LIST);
                LifecyclesSpliterator<JsonNode> lifecyclesSpliterator =
                    handleLogbookLifeCyclesObjectGroup(containerName, client,
                        LifeCycleStatusCode.LIFE_CYCLE_IN_PROCESS);
                StreamSupport.stream(lifecyclesSpliterator, false)
                    .map(LogbookLifeCycleObjectGroupInProcess::new)
                    .forEach(logbookLifeCycleObjectGroup -> handleXmlCreationTagDataObjectGroupForATRKo(xmlsw,
                        statusToBeChecked, objectGroupGuid, dataObjectsForOG, dataObjectSystemGuid,
                        dataObjectToDetailDataObject, logbookLifeCycleObjectGroup));
                xmlsw.writeEndElement(); // END SedaConstants.TAG_DATA_OBJECT_LIST
            } catch (final LogbookClientException | IllegalStateException | InvalidParseOperationException | IllegalArgumentException e) {
                throw new ProcessingException(e);
            }
        }
    }

    private void handleXmlCreationTagArchiveUnitATRKo(XMLStreamWriter xmlsw, List<String> statusToBeChecked,
        Map<String, String> systemGuidArchiveUnitId, LogbookLifeCycleUnitInProcess logbookLifeCycleUnit) {
        try {
            List<Document> logbookLifeCycleUnitEvents =
                (List<Document>) logbookLifeCycleUnit
                    .get(LogbookDocument.EVENTS.toString());

            xmlsw.writeStartElement(SedaConstants.TAG_ARCHIVE_UNIT);

            if (!systemGuidArchiveUnitId.isEmpty() &&
                logbookLifeCycleUnit.get(SedaConstants.PREFIX_ID) != null &&
                systemGuidArchiveUnitId
                    .get(logbookLifeCycleUnit.get(SedaConstants.PREFIX_ID).toString())
                    != null) {
                xmlsw.writeAttribute(SedaConstants.ATTRIBUTE_ID, systemGuidArchiveUnitId
                    .get(logbookLifeCycleUnit.get(SedaConstants.PREFIX_ID).toString()));
                writeAttributeValue(xmlsw, SedaConstants.TAG_ARCHIVE_SYSTEM_ID,
                    logbookLifeCycleUnit.get(SedaConstants.PREFIX_ID).toString());
            }

            for (final Document event : logbookLifeCycleUnitEvents) {
                writeEvent(xmlsw, event, SedaConstants.TAG_ARCHIVE_UNIT,
                    statusToBeChecked);
            }
            xmlsw.writeEndElement(); // END SedaConstants.TAG_ARCHIVE_UNIT
        } catch (XMLStreamException e) {
            throw new IllegalStateException(e);
        }
    }

    private void handleXmlCreationTagDataObjectGroupForATRKo(XMLStreamWriter xmlsw, List<String> statusToBeChecked,
        Map<String, String> objectGroupGuid, Map<String, List<String>> dataObjectsForOG,
        Map<String, Object> dataObjectSystemGuid, Map<String, DataObjectDetail> dataObjectToDetailDataObject,
        LogbookLifeCycleObjectGroupInProcess logbookLifeCycleObjectGroup) {
        try {

            final String eventIdentifier = null;
            xmlsw.writeStartElement(SedaConstants.TAG_DATA_OBJECT_GROUP);
            final String ogGUID =
                logbookLifeCycleObjectGroup.get(LogbookMongoDbName.objectIdentifier.getDbname()) !=
                    null
                    ? logbookLifeCycleObjectGroup.get(LogbookMongoDbName.objectIdentifier.getDbname())
                    .toString()
                    : "";

            String igId = "";
            if (objectGroupGuid.containsKey(ogGUID)) {
                igId = objectGroupGuid.get(ogGUID);
                xmlsw.writeAttribute(SedaConstants.ATTRIBUTE_ID, objectGroupGuid.get(ogGUID));
            }
            if (dataObjectsForOG.get(igId) != null) {
                for (final String idObj : dataObjectsForOG.get(igId)) {
                    if (dataObjectToDetailDataObject.get(idObj) != null &&
                        dataObjectToDetailDataObject.get(idObj).isPhysical()) {
                        xmlsw.writeStartElement(SedaConstants.TAG_PHYSICAL_DATA_OBJECT);
                    } else {
                        xmlsw.writeStartElement(SedaConstants.TAG_BINARY_DATA_OBJECT);
                    }
                    xmlsw.writeAttribute(SedaConstants.ATTRIBUTE_ID, idObj);
                    if (dataObjectSystemGuid.get(idObj) != null) {
                        writeAttributeValue(xmlsw, SedaConstants.TAG_DATA_OBJECT_SYSTEM_ID,
                            dataObjectSystemGuid.get(idObj).toString());
                    }
                    if (ogGUID != null && !ogGUID.isEmpty()) {
                        writeAttributeValue(xmlsw, SedaConstants.TAG_DATA_OBJECT_GROUP_SYSTEM_ID,
                            ogGUID);
                    }

                    xmlsw.writeEndElement(); // END TAG_BINARY_DATA_OBJECT OR TAG_PHYSICAL_DATA_OBJECT
                }

            }

            final List<Document> logbookLifeCycleObjectGroupEvents =
                (List<Document>) logbookLifeCycleObjectGroup.get(LogbookDocument.EVENTS.toString());
            if (logbookLifeCycleObjectGroupEvents != null) {
                for (final Document event : logbookLifeCycleObjectGroupEvents) {
                    writeEvent(xmlsw, event, SedaConstants.TAG_DATA_OBJECT_GROUP,
                        statusToBeChecked);
                }
            }

            xmlsw.writeEndElement(); // END SedaConstants.TAG_DATA_OBJECT_GROUP
        } catch (XMLStreamException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private LifecyclesSpliterator<JsonNode> handleLogbookLifeCyclesObjectGroup(String containerName,
        LogbookLifeCyclesClient client, LifeCycleStatusCode statusCode)
        throws LogbookClientException, ProcessingException {
        Select select = new Select();
        LifecyclesSpliterator<JsonNode> scrollRequest = new LifecyclesSpliterator<>(select,
            query -> {
                RequestResponse response;
                try {
                    response = client.objectGroupLifeCyclesByOperationIterator(containerName,
                        statusCode, select.getFinalSelect());
                } catch (InvalidParseOperationException | LogbookClientException e) {
                    throw new IllegalStateException(e);
                }
                if (response.isOk()) {
                    return (RequestResponseOK) response;
                } else {
                    throw new IllegalStateException(String.format(
                        "Error while loading logbook lifecycle objectGroup Bad Response %d",
                        response.getHttpCode()));
                }
            }, VitamConfiguration.getDefaultOffset(), VitamConfiguration.getBatchSize());
        return scrollRequest;
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        if (!handler.checkHandlerIO(1, handlerInitialIOList)) {
            throw new ProcessingException(HandlerIOImpl.NOT_CONFORM_PARAM);
        }
    }

    /**
     * Write an attribute with only one value
     *
     * @param writer    : The XMLStreamWriter on which the attribute is written
     * @param attribute Attribute Name
     * @param value     Attribute Value
     * @throws XMLStreamException XMLStreamException
     */

    private void writeAttributeValue(XMLStreamWriter writer, String attribute, String value)
        throws XMLStreamException {
        writer.writeStartElement(attribute);
        writer.writeCharacters(value);
        writer.writeEndElement();
    }

    /**
     * Write the event part of the xml in the KO ATR case
     *
     * @param xmlsw XMLStreamWriter
     * @param event event
     * @throws XMLStreamException XMLStreamException
     */
    private void writeEvent(XMLStreamWriter xmlsw, Document event, String eventType,
        List<String> statusToBeChecked)
        throws XMLStreamException {
        if (event.get(LogbookMongoDbName.outcome.getDbname()) != null &&
            statusToBeChecked.contains(event.get(LogbookMongoDbName.outcome.getDbname()).toString())) {

            xmlsw.writeStartElement(SedaConstants.TAG_EVENT);
            if (event.get(LogbookMongoDbName.eventType.getDbname()) != null) {
                writeAttributeValue(xmlsw, SedaConstants.TAG_EVENT_TYPE_CODE,
                    event.get(LogbookMongoDbName.eventType.getDbname()).toString());
                if (SedaConstants.TAG_OPERATION.equals(eventType)) {
                    writeAttributeValue(xmlsw, SedaConstants.TAG_EVENT_TYPE, VitamLogbookMessages
                        .getLabelOp(event.get(LogbookMongoDbName.eventType.getDbname()).toString()));
                } else if (SedaConstants.TAG_ARCHIVE_UNIT.equals(eventType) || SedaConstants.TAG_DATA_OBJECT_GROUP
                    .equals(eventType)) {
                    writeAttributeValue(xmlsw, SedaConstants.TAG_EVENT_TYPE, VitamLogbookMessages
                        .getFromFullCodeKey(event.get(LogbookMongoDbName.eventType.getDbname()).toString()));
                }
            }
            if (event.get(LogbookMongoDbName.eventDateTime.getDbname()) != null) {
                writeAttributeValue(xmlsw, SedaConstants.TAG_EVENT_DATE_TIME,
                    event.get(LogbookMongoDbName.eventDateTime.getDbname()).toString());
            }
            if (event.get(LogbookMongoDbName.outcome.getDbname()) != null) {
                writeAttributeValue(xmlsw, SedaConstants.TAG_EVENT_OUTCOME,
                    event.get(LogbookMongoDbName.outcome.getDbname()).toString());
            }
            if (event.get(LogbookMongoDbName.outcomeDetail.getDbname()) != null) {
                writeAttributeValue(xmlsw, SedaConstants.TAG_EVENT_OUTCOME_DETAIL,
                    event.get(LogbookMongoDbName.outcomeDetail.getDbname()).toString());
            }
            if (event.get(LogbookMongoDbName.outcomeDetailMessage.getDbname()) != null) {
                writeAttributeValue(xmlsw, SedaConstants.TAG_EVENT_OUTCOME_DETAIL_MESSAGE,
                    event.get(LogbookMongoDbName.outcomeDetailMessage.getDbname()).toString());
            }
            if (event.get(LogbookMongoDbName.eventDetailData.getDbname()) != null) {
                final String detailData = event.get(LogbookMongoDbName.eventDetailData.getDbname()).toString();
                if (detailData.contains(SedaConstants.EV_DET_TECH_DATA)) {
                    writeAttributeValue(xmlsw, SedaConstants.TAG_EVENT_DETAIL_DATA,
                        event.get(LogbookMongoDbName.eventDetailData.getDbname()).toString());
                }
            }
            xmlsw.writeEndElement(); // END SedaConstants.TAG_EVENT
        }
    }
}
