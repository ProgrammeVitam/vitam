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
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang.BooleanUtils;
import org.bson.Document;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.client.MongoCursor;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.CompositeItemStatus;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.server.LogbookDbAccess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookDocument;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleMongoDbName;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleObjectGroup;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleUnit;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookOperation;
import fr.gouv.vitam.logbook.common.server.exception.LogbookDatabaseException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookNotFoundException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameterName;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.StorageCollectionType;
import fr.gouv.vitam.storage.engine.client.exception.StorageClientException;
import fr.gouv.vitam.storage.engine.common.model.request.CreateObjectDescription;
import fr.gouv.vitam.worker.common.utils.IngestWorkflowConstants;
import fr.gouv.vitam.worker.common.utils.SedaConstants;
import fr.gouv.vitam.worker.core.MarshallerObjectCache;
import fr.gouv.vitam.worker.core.api.HandlerIO;
import fr.gouv.vitam.worker.model.ArchiveUnitReplyTypeRoot;
import fr.gouv.vitam.worker.model.DataObjectTypeRoot;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

/**
 * Transfer notification reply handler
 */
public class TransferNotificationActionHandler extends ActionHandler {

    private static final String XML = ".xml";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TransferNotificationActionHandler.class);
    private static final String HANDLER_ID = "ATR_NOTIFICATION";
    LogbookOperationParameters parameters = LogbookParametersFactory.newLogbookOperationParameters();
    private static final String NAMESPACE_URI = "fr:gouv:culture:archivesdefrance:seda:v2.0";
    private static final String XLINK_URI = "http://www.w3.org/1999/xlink";
    private static final String PREMIS_URI = "info:lc/xmlns/premis-v2";
    private static final String XSI_URI = "http://www.w3.org/2001/XMLSchema-instance";
    private static final String XSD_VERSION = " seda-2.0-main.xsd";
    public static final String JSON_EXTENSION = ".json";

    private static final String ATR_FILE_NAME = "responseReply.xml";
    private HandlerIO handlerIO;
    private final StorageClientFactory storageClientFactory;
    private static final String DEFAULT_TENANT = "0";
    private static final String DEFAULT_STRATEGY = "default";

    private final HandlerIO handlerInitialIOList = new HandlerIO(HANDLER_ID);
    private final MarshallerObjectCache marshallerObjectCache = new MarshallerObjectCache();
    public static final int HANDLER_IO_PARAMETER_NUMBER = 5;

    private final LogbookDbAccess mongoDbAccess;

    /**
     * Constructor TransferNotificationActionHandler with parameter mongoDbAccess
     * 
     * @param mongoDbAccess mongoDbAccess
     * @throws IOException
     * 
     */
    public TransferNotificationActionHandler(LogbookDbAccess mongoDbAccess) {
        storageClientFactory = StorageClientFactory.getInstance();
        for (int i = 0; i < HANDLER_IO_PARAMETER_NUMBER; i++) {
            handlerInitialIOList.addInput(File.class);
        }
        this.mongoDbAccess = mongoDbAccess;
    }

    /**
     * @return HANDLER_ID
     */
    public static final String getId() {
        return HANDLER_ID;
    }

    @Override
    public CompositeItemStatus execute(WorkerParameters params, HandlerIO handler) {
        checkMandatoryParameters(params);

        final ItemStatus itemStatus = new ItemStatus(HANDLER_ID);

        handlerIO = handler;

        try {
            Boolean isWorkflowKo =
                BooleanUtils.toBoolean(params.getMapParameters().get(WorkerParameterName.workflowStatusKo));
            File atrFile;
            if (isWorkflowKo) {
                atrFile = createATRKO(params, handlerIO);
            } else {
                checkMandatoryIOParameter(handler);
                atrFile = createATROK(params, handlerIO);
            }
            // FIXME : Fix bug on jenkin org.xml.sax.SAXParseException: src-resolve: Cannot resolve the name 'xml:id' to
            // a(n) 'attribute declaration' component.
            // if (new ValidationXsdUtils().checkWithXSD(new FileInputStream(atrFile), SEDA_VALIDATION_FILE)) {
            try (final WorkspaceClient workspaceClient = WorkspaceClientFactory.create(params.getUrlWorkspace())) {
                HandlerIO.transferFileFromTmpIntoWorkspace(
                    workspaceClient,
                    params.getContainerName() + ATR_FILE_NAME,
                    IngestWorkflowConstants.ATR_FOLDER + "/" + ATR_FILE_NAME,
                    params.getContainerName(),
                    true);
            }
            // store binary data object
            final CreateObjectDescription description = new CreateObjectDescription();
            description.setWorkspaceContainerGUID(params.getContainerName());
            description.setWorkspaceObjectURI(IngestWorkflowConstants.ATR_FOLDER + "/" + ATR_FILE_NAME);
            storageClientFactory.getStorageClient().storeFileFromWorkspace(
                DEFAULT_TENANT,
                DEFAULT_STRATEGY,
                StorageCollectionType.REPORTS,
                params.getContainerName() + XML, description);
            // }
            itemStatus.increment(StatusCode.OK);
        } catch (ProcessingException | ContentAddressableStorageException e) {
            LOGGER.error(e);
            itemStatus.increment(StatusCode.KO);
        } catch (URISyntaxException | InvalidParseOperationException |
            StorageClientException | IOException e) {
            LOGGER.error(e);
            itemStatus.increment(StatusCode.FATAL);
            // } catch (SAXException | XMLStreamException e) {
            // LOGGER.error(e);
            // itemStatus.increment(new WorkflowStatusCode(StatusCode.FATAL));
        }
        return new CompositeItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
    }

    /**
     * Serialize a Jaxb POJO object in the current XML stream
     *
     * @param jaxbPOJO
     * @throws VitamSedaException
     */
    private void writeXMLFragment(Object jaxbPOJO, XMLStreamWriter xmlsw) throws ProcessingException {
        try {
            marshallerObjectCache.getMarshaller(jaxbPOJO.getClass()).marshal(jaxbPOJO, xmlsw);
        } catch (final JAXBException e) {
            throw new ProcessingException("Error on writing " + jaxbPOJO + "object", e);
        }

    }

    /**
     * @param params of type WorkerParameters
     * @param ioParam of type HandlerIO
     * @throws ProcessingException when execute process failed
     * @throws ContentAddressableStorageNotFoundException
     * @throws ContentAddressableStorageServerException
     * @throws URISyntaxException
     * @throws ContentAddressableStorageException
     * @throws IOException
     * @throws InvalidParseOperationException
     */
    private File createATROK(WorkerParameters params, HandlerIO ioParam)
        throws ProcessingException, ContentAddressableStorageNotFoundException,
        ContentAddressableStorageServerException, URISyntaxException, ContentAddressableStorageException, IOException,
        InvalidParseOperationException {
        ParameterHelper.checkNullOrEmptyParameters(params);
        final String atrPath = params.getContainerName() + ATR_FILE_NAME;
        final File atrTmpFile = PropertiesUtils.fileFromTmpFolder(atrPath);

        // Pre-actions
        final InputStream archiveUnitMapTmpFile = new FileInputStream((File) handlerIO.getInput().get(0));
        final Map<String, Object> archiveUnitSystemGuid = JsonHandler.getMapFromInputStream(archiveUnitMapTmpFile);
        final InputStream binaryDataObjectMapTmpFile = new FileInputStream((File) handlerIO.getInput().get(1));
        final Map<String, Object> binaryDataObjectSystemGuid =
            JsonHandler.getMapFromInputStream(binaryDataObjectMapTmpFile);
        final InputStream bdoObjectGroupStoredMapTmpFile = new FileInputStream((File) handlerIO.getInput().get(2));
        final Map<String, Object> bdoObjectGroupSystemGuid =
            JsonHandler.getMapFromInputStream(bdoObjectGroupStoredMapTmpFile);
        final InputStream binaryDataObjectIdToVersionDataObjectMapTmpFile =
            new FileInputStream((File) handlerIO.getInput().get(3));
        final Map<String, Object> bdoVersionDataObject =
            JsonHandler.getMapFromInputStream(binaryDataObjectIdToVersionDataObjectMapTmpFile);
        final SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

        final JsonNode sedaParameters = JsonHandler.getFromFile((File) handlerIO.getInput().get(4));
        JsonNode infoATR =
            sedaParameters.get(SedaConstants.TAG_ARCHIVE_TRANSFER);
        String messageIdentifier = infoATR.get(SedaConstants.TAG_MESSAGE_IDENTIFIER).textValue();
        // creation of ATR report
        try {
            final XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
            final FileWriter artTmpFileWriter = new FileWriter(atrTmpFile);

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
            writeAttributeValue(xmlsw, SedaConstants.TAG_MESSAGE_IDENTIFIER, params.getProcessId());
            writeAttributeValue(xmlsw, SedaConstants.TAG_ARCHIVAL_AGREEMENT,
                infoATR.get(SedaConstants.TAG_ARCHIVAL_AGREEMENT).textValue());

            xmlsw.writeStartElement(SedaConstants.TAG_CODE_LIST_VERSIONS);
            writeAttributeValue(xmlsw, SedaConstants.TAG_REPLY_CODE_LIST_VERSION,
                infoATR.get(SedaConstants.TAG_CODE_LIST_VERSIONS).get(SedaConstants.TAG_REPLY_CODE_LIST_VERSION)
                    .textValue());
            writeAttributeValue(xmlsw, SedaConstants.TAG_MESSAGE_DIGEST_ALGORITHM_CODE_LIST_VERSION,
                infoATR.get(SedaConstants.TAG_CODE_LIST_VERSIONS)
                    .get(SedaConstants.TAG_MESSAGE_DIGEST_ALGORITHM_CODE_LIST_VERSION).textValue());
            writeAttributeValue(xmlsw, SedaConstants.TAG_FILE_FORMAT_CODE_LIST_VERSION,
                infoATR.get(SedaConstants.TAG_CODE_LIST_VERSIONS).get(SedaConstants.TAG_FILE_FORMAT_CODE_LIST_VERSION)
                    .textValue());
            xmlsw.writeEndElement(); // END SedaConstants.TAG_CODE_LIST_VERSIONS

            xmlsw.writeStartElement(SedaConstants.TAG_DATA_OBJECT_PACKAGE);

            writeAttributeValue(xmlsw, SedaConstants.TAG_DESCRIPTIVE_METADATA, null);

            xmlsw.writeStartElement(SedaConstants.TAG_MANAGEMENT_METADATA);
            xmlsw.writeStartElement(SedaConstants.TAG_REPLY_OUTCOME);

            xmlsw.writeStartElement(SedaConstants.TAG_ARCHIVE_UNIT_LIST);
            if (archiveUnitSystemGuid != null) {
                for (final Map.Entry<String, Object> entry : archiveUnitSystemGuid.entrySet()) {
                    final ArchiveUnitReplyTypeRoot au = new ArchiveUnitReplyTypeRoot();
                    au.setId(entry.getKey());
                    au.setSystemId(entry.getValue().toString());
                    writeXMLFragment(au, xmlsw);
                }
            }

            xmlsw.writeEndElement(); // END ARCHIVE_UNIT_LIST
            xmlsw.writeStartElement(SedaConstants.TAG_DATA_OBJECT_LIST);
            // Set to known which DOGIG has already be used in the XML
            final Set<String> usedDataObjectGroup = new HashSet<String>();
            if (binaryDataObjectSystemGuid != null) {
                for (final Map.Entry<String, Object> entry : binaryDataObjectSystemGuid.entrySet()) {
                    final String dataOGID = bdoObjectGroupSystemGuid.get(entry.getKey()).toString();
                    final String dataBDOVersion = bdoVersionDataObject.get(entry.getKey()).toString();
                    final DataObjectTypeRoot dotr = new DataObjectTypeRoot();
                    dotr.setId(entry.getKey());
                    // Test if the DOGID has already be used . If so, use DOGRefID, else DOGID in the SEDA XML
                    if (usedDataObjectGroup.contains(dataOGID)) {
                        dotr.setDataObjectGroupRefId(dataOGID);
                    } else {
                        dotr.setDataObjectGroupId(dataOGID);
                        usedDataObjectGroup.add(dataOGID);
                    }
                    dotr.setDataObjectVersion(dataBDOVersion);
                    dotr.setDataObjectGroupSystemId(entry.getValue().toString());
                    writeXMLFragment(dotr, xmlsw);
                }
            }
            xmlsw.writeEndElement();// END DATA_OBJECT_LIST

            xmlsw.writeEndElement(); // END ARCHIVE_UNIT_LIST


            xmlsw.writeEndElement(); // END REPLY_OUTCOME
            xmlsw.writeEndElement(); // END MANAGEMENT_METADATA

            writeAttributeValue(xmlsw, SedaConstants.TAG_REPLY_CODE, "ReplyCode0");
            writeAttributeValue(xmlsw, SedaConstants.TAG_MESSAGE_REQUEST_IDENTIFIER, messageIdentifier);
            writeAttributeValue(xmlsw, SedaConstants.TAG_GRANT_DATE, sdfDate.format(new Date()));

            xmlsw.writeStartElement(SedaConstants.TAG_ARCHIVAL_AGENCY);
            writeAttributeValue(xmlsw, SedaConstants.TAG_IDENTIFIER,
                infoATR.get(SedaConstants.TAG_ARCHIVAL_AGENCY).get(SedaConstants.TAG_IDENTIFIER).textValue());
            xmlsw.writeEndElement(); // END SedaConstants.TAG_ARCHIVAL_AGENCY


            xmlsw.writeStartElement(SedaConstants.TAG_TRANSFERRING_AGENCY);
            writeAttributeValue(xmlsw, SedaConstants.TAG_IDENTIFIER,
                infoATR.get(SedaConstants.TAG_TRANSFERRING_AGENCY).get(SedaConstants.TAG_IDENTIFIER).textValue());
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

    /**
     * @param params of type WorkerParameters
     * @param ioParam of type HandlerIO
     * @throws ProcessingException when execute process failed
     * @throws ContentAddressableStorageNotFoundException
     * @throws ContentAddressableStorageServerException
     * @throws URISyntaxException
     * @throws ContentAddressableStorageException
     * @throws IOException
     * @throws InvalidParseOperationException
     */
    private File createATRKO(WorkerParameters params, HandlerIO ioParam)
        throws ProcessingException, ContentAddressableStorageNotFoundException,
        ContentAddressableStorageServerException, URISyntaxException, ContentAddressableStorageException, IOException,
        InvalidParseOperationException {
        ParameterHelper.checkNullOrEmptyParameters(params);
        String atrPath = params.getContainerName() + ATR_FILE_NAME;
        File atrTmpFile = PropertiesUtils.fileFromTmpFolder(atrPath);

        // Pre-actions
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

        JsonNode infoATR = null;
        String messageIdentifier = null;
        if (handlerIO.getInput().get(4) != null) {
            final JsonNode sedaParameters = JsonHandler.getFromFile((File) handlerIO.getInput().get(4));
            infoATR =
                sedaParameters.get(SedaConstants.TAG_ARCHIVE_TRANSFER).get(SedaConstants.TAG_ARCHIVE_TRANSFER);
            if (infoATR != null && infoATR.get(SedaConstants.TAG_MESSAGE_IDENTIFIER) != null) {
                messageIdentifier = infoATR.get(SedaConstants.TAG_MESSAGE_IDENTIFIER).textValue();
            }
        }
        // creation of ATR report
        try {
            XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
            final FileWriter artTmpFileWriter = new FileWriter(atrTmpFile);

            XMLStreamWriter xmlsw = outputFactory.createXMLStreamWriter(artTmpFileWriter);
            xmlsw.writeStartDocument();

            xmlsw.writeStartElement(SedaConstants.TAG_ARCHIVE_TRANSFER_REPLY);

            xmlsw.writeNamespace(SedaConstants.NAMESPACE_XLINK, XLINK_URI);
            xmlsw.writeNamespace(SedaConstants.NAMESPACE_PR, PREMIS_URI);
            xmlsw.writeDefaultNamespace(NAMESPACE_URI);
            xmlsw.writeNamespace(SedaConstants.NAMESPACE_XSI, XSI_URI);
            xmlsw.writeAttribute(SedaConstants.NAMESPACE_XSI, XSI_URI, SedaConstants.ATTRIBUTE_SCHEMA_LOCATION,
                NAMESPACE_URI + XSD_VERSION);

            writeAttributeValue(xmlsw, SedaConstants.TAG_DATE, sdfDate.format(new Date()));
            writeAttributeValue(xmlsw, SedaConstants.TAG_MESSAGE_IDENTIFIER, params.getProcessId());

            if (infoATR != null) {
                writeAttributeValue(xmlsw, SedaConstants.TAG_ARCHIVAL_AGREEMENT,
                    infoATR.get(SedaConstants.TAG_ARCHIVAL_AGREEMENT).textValue());

                xmlsw.writeStartElement(SedaConstants.TAG_CODE_LIST_VERSIONS);
                writeAttributeValue(xmlsw, SedaConstants.TAG_REPLY_CODE_LIST_VERSION,
                    infoATR.get(SedaConstants.TAG_CODE_LIST_VERSIONS).get(SedaConstants.TAG_REPLY_CODE_LIST_VERSION)
                        .textValue());
                writeAttributeValue(xmlsw, SedaConstants.TAG_MESSAGE_DIGEST_ALGORITHM_CODE_LIST_VERSION,
                    infoATR.get(SedaConstants.TAG_CODE_LIST_VERSIONS)
                        .get(SedaConstants.TAG_MESSAGE_DIGEST_ALGORITHM_CODE_LIST_VERSION).textValue());
                writeAttributeValue(xmlsw, SedaConstants.TAG_FILE_FORMAT_CODE_LIST_VERSION,
                    infoATR.get(SedaConstants.TAG_CODE_LIST_VERSIONS)
                        .get(SedaConstants.TAG_FILE_FORMAT_CODE_LIST_VERSION)
                        .textValue());
                xmlsw.writeEndElement(); // END SedaConstants.TAG_CODE_LIST_VERSIONS
            }

            xmlsw.writeStartElement(SedaConstants.TAG_DATA_OBJECT_PACKAGE);

            writeAttributeValue(xmlsw, SedaConstants.TAG_DESCRIPTIVE_METADATA, null);

            xmlsw.writeStartElement(SedaConstants.TAG_MANAGEMENT_METADATA);

            xmlsw.writeStartElement(SedaConstants.TAG_REPLY_OUTCOME);

            addKOReplyOutcome(xmlsw, params.getContainerName());

            xmlsw.writeEndElement(); // END REPLY_OUTCOME
            xmlsw.writeEndElement(); // END MANAGEMENT_METADATA

            writeAttributeValue(xmlsw, SedaConstants.TAG_REPLY_CODE, "ReplyCode0");
            if (messageIdentifier != null) {
                writeAttributeValue(xmlsw, SedaConstants.TAG_MESSAGE_REQUEST_IDENTIFIER, messageIdentifier);
            }

            writeAttributeValue(xmlsw, SedaConstants.TAG_GRANT_DATE, sdfDate.format(new Date()));


            xmlsw.writeStartElement(SedaConstants.TAG_ARCHIVAL_AGENCY);
            if (infoATR != null) {
                writeAttributeValue(xmlsw, SedaConstants.TAG_IDENTIFIER,
                    infoATR.get(SedaConstants.TAG_ARCHIVAL_AGENCY).get(SedaConstants.TAG_IDENTIFIER).textValue());
            }
            xmlsw.writeEndElement(); // END SedaConstants.TAG_ARCHIVAL_AGENCY


            xmlsw.writeStartElement(SedaConstants.TAG_TRANSFERRING_AGENCY);
            if (infoATR != null) {
                writeAttributeValue(xmlsw, SedaConstants.TAG_IDENTIFIER,
                    infoATR.get(SedaConstants.TAG_TRANSFERRING_AGENCY).get(SedaConstants.TAG_IDENTIFIER).textValue());
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


    /**
     * Add the KO (which could be KO or FATAL) replyOutcome to the ATR xml
     * 
     * @param xmlsw xml writer
     * @param containerName the operation identifier
     * @throws ProcessingException thrown if a logbook could not be retrieved
     * @throws XMLStreamException
     * @throws FileNotFoundException
     * @throws InvalidParseOperationException
     */
    private void addKOReplyOutcome(XMLStreamWriter xmlsw, String containerName)
        throws ProcessingException, XMLStreamException, FileNotFoundException, InvalidParseOperationException {

        LogbookOperation logbookOperation = getLogbookOperation(containerName);
        if (logbookOperation != null) {
            List<Document> logbookOperationEvents =
                (List<Document>) logbookOperation.get(LogbookDocument.EVENTS.toString());
            xmlsw.writeStartElement(SedaConstants.TAG_OPERATION);
            for (Document event : logbookOperationEvents) {
                writeEvent(xmlsw, event, SedaConstants.TAG_OPERATION, null);
            }
            xmlsw.writeEndElement(); // END SedaConstants.TAG_OPERATION

            try (MongoCursor<LogbookLifeCycleUnit> logbookLifeCycleUnits = getLogbookLifecycleUnits(containerName)) {
                if (logbookLifeCycleUnits != null) {
                    InputStream archiveUnitMapTmpFile = null;
                    if (handlerIO.getInput().get(0) != null) {
                        archiveUnitMapTmpFile = new FileInputStream((File) handlerIO.getInput().get(0));
                    }
                    Map<String, Object> archiveUnitSystemGuid = null;
                    Map<String, String> systemGuidArchiveUnitId = null;

                    if (archiveUnitMapTmpFile != null) {
                        archiveUnitSystemGuid = JsonHandler.getMapFromInputStream(archiveUnitMapTmpFile);
                        if (archiveUnitSystemGuid != null) {
                            systemGuidArchiveUnitId = new HashMap<>();
                            for (Map.Entry<String, Object> entry : archiveUnitSystemGuid.entrySet()) {
                                systemGuidArchiveUnitId.put(entry.getValue().toString(), entry.getKey());
                            }
                        }
                    }

                    xmlsw.writeStartElement(SedaConstants.TAG_ARCHIVE_UNIT_LIST);
                    while (logbookLifeCycleUnits.hasNext()) {
                        LogbookLifeCycleUnit logbookLifeCycleUnit = logbookLifeCycleUnits.next();
                        List<Document> logbookLifeCycleUnitEvents =
                            (List<Document>) logbookLifeCycleUnit.get(LogbookDocument.EVENTS.toString());
                        xmlsw.writeStartElement(SedaConstants.TAG_ARCHIVE_UNIT);

                        if (systemGuidArchiveUnitId != null &&
                            logbookLifeCycleUnit.get(SedaConstants.PREFIX_ID) != null &&
                            systemGuidArchiveUnitId
                                .get(logbookLifeCycleUnit.get(SedaConstants.PREFIX_ID).toString()) != null) {
                            xmlsw.writeAttribute(SedaConstants.ATTRIBUTE_ID,
                                systemGuidArchiveUnitId
                                    .get(logbookLifeCycleUnit.get(SedaConstants.PREFIX_ID).toString()));
                        }

                        for (Document event : logbookLifeCycleUnitEvents) {
                            writeEvent(xmlsw, event, SedaConstants.TAG_ARCHIVE_UNIT, null);
                        }
                        xmlsw.writeEndElement(); // END SedaConstants.TAG_ARCHIVE_UNIT
                    }
                    logbookLifeCycleUnits.close();
                }
                xmlsw.writeEndElement(); // END SedaConstants.TAG_ARCHIVE_UNIT_LIST
            }

            MongoCursor<LogbookLifeCycleObjectGroup> logbookLifeCycleObjectGroups =
                getLogbookLifecycleObjectGroups(containerName);
            if (logbookLifeCycleObjectGroups != null) {
                Map<String, Object> binaryDataObjectSystemGuid = null;
                Map<String, Object> bdoObjectGroupSystemGuid = null;
                if (handlerIO.getInput().get(1) != null && handlerIO.getInput().get(2) != null) {
                    InputStream binaryDataObjectMapTmpFile = new FileInputStream((File) handlerIO.getInput().get(1));
                    InputStream bdoObjectGroupStoredMapTmpFile =
                        new FileInputStream((File) handlerIO.getInput().get(2));
                    binaryDataObjectSystemGuid = JsonHandler.getMapFromInputStream(binaryDataObjectMapTmpFile);
                    bdoObjectGroupSystemGuid = JsonHandler.getMapFromInputStream(bdoObjectGroupStoredMapTmpFile);
                }
                Map<String, String> guidObjectToObjectGroup = new HashMap<>();
                Map<String, String> guidToIdObject = new HashMap<>();
                // Set to known which DOGIG has already be used in the XML
                Set<String> usedDataObjectGroup = new HashSet<>();
                if (binaryDataObjectSystemGuid != null && bdoObjectGroupSystemGuid != null) {
                    for (Map.Entry<String, Object> entry : binaryDataObjectSystemGuid.entrySet()) {
                        String dataOGID = bdoObjectGroupSystemGuid.get(entry.getKey()).toString();
                        // String dataBDOVersion = bdoVersionDataObject.get(entry.getKey()).toString();
                        // DataObjectTypeRoot dotr = new DataObjectTypeRoot();
                        // dotr.setId(entry.getKey());
                        // // Test if the DOGID has already be used . If so, use DOGRefID, else DOGID in the SEDA XML
                        // if (usedDataObjectGroup.contains(dataOGID)) {
                        // dotr.setDataObjectGroupRefId(dataOGID);
                        // } else {
                        // dotr.setDataObjectGroupId(dataOGID);
                        // usedDataObjectGroup.add(dataOGID);
                        // }
                        // dotr.setDataObjectVersion(dataBDOVersion);
                        // dotr.setDataObjectGroupSystemId(entry.getValue().toString());
                        guidObjectToObjectGroup.put(entry.getValue().toString(), dataOGID);
                        guidToIdObject.put(entry.getValue().toString(), entry.getKey());
                    }
                }

                xmlsw.writeStartElement(SedaConstants.TAG_DATA_OBJECT_LIST);
                while (logbookLifeCycleObjectGroups.hasNext()) {

                    LogbookLifeCycleObjectGroup logbookLifeCycleObjectGroup = logbookLifeCycleObjectGroups.next();
                    List<Document> logbookLifeCycleObjectGroupEvents =
                        (List<Document>) logbookLifeCycleObjectGroup.get(LogbookDocument.EVENTS.toString());

                    boolean firstIteration = true;
                    String eventIdentifier = null;
                    for (Document event : logbookLifeCycleObjectGroupEvents) {
                        if (firstIteration) {
                            firstIteration = false;
                            xmlsw.writeStartElement(SedaConstants.TAG_DATA_OBJECT_GROUP);
                            if (event.get(LogbookMongoDbName.eventIdentifier.getDbname()) != null &&
                                guidObjectToObjectGroup.containsKey(
                                    event.get(LogbookMongoDbName.eventIdentifier.getDbname()).toString())) {
                                xmlsw.writeAttribute(SedaConstants.ATTRIBUTE_ID,
                                    event.get(LogbookMongoDbName.eventIdentifier.getDbname()).toString());
                            }
                        }

                        xmlsw.writeStartElement(SedaConstants.TAG_BINARY_DATA_OBJECT);
                        if (event.get(LogbookMongoDbName.eventIdentifier.getDbname()) != null && guidToIdObject
                            .containsKey(event.get(LogbookMongoDbName.eventIdentifier.getDbname()).toString())) {
                            eventIdentifier = guidToIdObject
                                .get(event.get(LogbookMongoDbName.eventIdentifier.getDbname()).toString());
                            xmlsw.writeAttribute(SedaConstants.ATTRIBUTE_ID, eventIdentifier);
                        }
                        if (event.get(LogbookMongoDbName.eventIdentifier.getDbname()) != null) {
                            writeAttributeValue(xmlsw, SedaConstants.TAG_BINARY_DATA_OBJECT_SYSTEM_ID,
                                event.get(LogbookMongoDbName.eventIdentifier.getDbname()).toString());
                        }
                        xmlsw.writeEndElement(); // END SedaConstants.TAG_BINARY_DATA_OBJECT

                        writeEvent(xmlsw, event, SedaConstants.TAG_DATA_OBJECT_GROUP, eventIdentifier);
                    }
                    if (!firstIteration) {
                        xmlsw.writeEndElement(); // END SedaConstants.TAG_DATA_OBJECT_GROUP
                    }
                }
                logbookLifeCycleObjectGroups.close();
                xmlsw.writeEndElement(); // END SedaConstants.TAG_DATA_OBJECT_LIST
            }
        }
    }

    /**
     * Retrieve the logbook operation of the current operation <br>
     * 
     * @param containerName operation identifier
     * @return the logbook opetaion
     * @throws ProcessingException thrown when an error occured wile retrieving the logbook operation in mongo
     */
    // TODO : should use the logbook client with a rest api when REST cursors are implemented in logbook
    private LogbookOperation getLogbookOperation(String containerName) throws ProcessingException {
        try {
            return mongoDbAccess.getLogbookOperation(containerName);
        } catch (LogbookDatabaseException | LogbookNotFoundException e) {
            LOGGER.error("Error while loading logbook operation", e);
            throw new ProcessingException(e);
        }
    }

    /**
     * Retrieve the logbook lifecycle units of the current operation <br>
     * 
     * @param containerName operation identifier
     * @return mongo cursor on the lifecycle units
     * @throws ProcessingException thrown when an error occured wile retrieving the logbook lifecycle units in mongo
     */
    // TODO : should use the logbook client with a rest api when REST cursors are implemented in logbook
    // FIXME : should filter only on events with an outcome equals FATAL or KO
    private MongoCursor<LogbookLifeCycleUnit> getLogbookLifecycleUnits(String containerName)
        throws ProcessingException {
        try {
            final Select select = new Select();
            select.setQuery(QueryHelper.eq(
                LogbookLifeCycleMongoDbName.getLogbookLifeCycleMongoDbName(LogbookParameterName.eventIdentifierProcess)
                    .getDbname(),
                containerName));
            JsonNode selectRequest = JsonHandler.getFromString(select.getFinalSelect().toString());

            return mongoDbAccess.getLogbookLifeCycleUnits(selectRequest);
        } catch (LogbookDatabaseException | LogbookNotFoundException | InvalidParseOperationException |
            InvalidCreateOperationException e) {
            LOGGER.error("Error while loading logbook lifecycle units", e);
            throw new ProcessingException(e);
        }
    }

    /**
     * Retrieve the logbook lifecycle object groups of the current operation <br>
     * 
     * @param containerName operation identifier
     * @return mongo cursor on the lifecycle object groups
     * @throws ProcessingException thrown when an error occured wile retrieving the logbook lifecycle object groups in
     *         mongo
     */
    // TODO : should use the logbook client with a rest api when REST cursors are implemented in logbook
    // FIXME : should filter only on events with an outcome equals FATAL or KO    
    private MongoCursor<LogbookLifeCycleObjectGroup> getLogbookLifecycleObjectGroups(String idProc)
        throws ProcessingException {
        try {
            final Select select = new Select();
            select
                .setQuery(QueryHelper.eq(
                    LogbookLifeCycleMongoDbName
                        .getLogbookLifeCycleMongoDbName(LogbookParameterName.eventIdentifierProcess).getDbname(),
                    idProc));
            JsonNode selectRequest = JsonHandler.getFromString(select.getFinalSelect().toString());

            return mongoDbAccess.getLogbookLifeCycleObjectGroups(selectRequest);
        } catch (LogbookDatabaseException | LogbookNotFoundException | InvalidParseOperationException |
            InvalidCreateOperationException e) {
            LOGGER.error("Error while loading logbook lifecycle units", e);
            throw new ProcessingException(e);
        }
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        if (handler.getInput().size() != handlerInitialIOList.getInput().size()) {
            throw new ProcessingException(HandlerIO.NOT_ENOUGH_PARAM);
        } else if (!HandlerIO.checkHandlerIO(handlerIO, handlerInitialIOList)) {
            throw new ProcessingException(HandlerIO.NOT_CONFORM_PARAM);
        }
    }

    /**
     * Write an attribute with only one value
     *
     * @param writer : The XMLStreamWriter on which the attribute is written
     * @param attribute
     * @param value
     * @throws XMLStreamException
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
     * @param xmlsw
     * @param event
     * @throws XMLStreamException
     */
    private void writeEvent(XMLStreamWriter xmlsw, Document event, String eventType, String eventIdentifierManifest)
        throws XMLStreamException {
        // TODO : NOT_IMPLEMENTED_YET to be replaced everywhere its used
        final String NOT_IMPLEMENTED_YET = "TO_BE_FILLED_AFTER_WF_TF";
        if (event.get(LogbookMongoDbName.outcome.getDbname()) != null &&
            (StatusCode.FATAL.toString().equals(event.get(LogbookMongoDbName.outcome.getDbname()).toString()) ||
                StatusCode.KO.toString()
                    .equals(event.get(LogbookMongoDbName.outcome.getDbname()).toString()))) {

            if (SedaConstants.TAG_DATA_OBJECT_GROUP.equals(eventType) && eventIdentifierManifest != null) {
                writeAttributeValue(xmlsw, SedaConstants.TAG_BINARY_DATA_OBJECT_ID, eventIdentifierManifest);
            }

            xmlsw.writeStartElement(SedaConstants.TAG_EVENT);

            if (event.get(LogbookMongoDbName.eventType.getDbname()) != null) {
                writeAttributeValue(xmlsw, SedaConstants.TAG_EVENT_TYPE,
                    event.get(LogbookMongoDbName.eventType.getDbname()).toString());
            }
            if (NOT_IMPLEMENTED_YET != null) {
                writeAttributeValue(xmlsw, SedaConstants.TAG_EVENT_TYPE_CODE, NOT_IMPLEMENTED_YET);
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
            if (NOT_IMPLEMENTED_YET != null) {
                writeAttributeValue(xmlsw, SedaConstants.TAG_EVENT_OUTCOME_DETAIL_MESSAGE_CODE,
                    NOT_IMPLEMENTED_YET);
            }
            xmlsw.writeEndElement(); // END SedaConstants.TAG_EVENT
        }
    }
}
