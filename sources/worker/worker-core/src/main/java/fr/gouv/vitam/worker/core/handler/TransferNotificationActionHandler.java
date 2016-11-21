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
import java.util.ArrayList;
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

import org.bson.Document;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.client2.VitamRequestIterator;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookDocument;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleObjectGroup;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleUnit;
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
import fr.gouv.vitam.storage.engine.client.StorageCollectionType;
import fr.gouv.vitam.storage.engine.client.exception.StorageClientException;
import fr.gouv.vitam.storage.engine.common.model.request.CreateObjectDescription;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.common.utils.IngestWorkflowConstants;
import fr.gouv.vitam.worker.common.utils.SedaConstants;
import fr.gouv.vitam.worker.core.MarshallerObjectCache;
import fr.gouv.vitam.worker.core.impl.HandlerIOImpl;
import fr.gouv.vitam.worker.model.ArchiveUnitReplyTypeRoot;
import fr.gouv.vitam.worker.model.DataObjectTypeRoot;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

/**
 * Transfer notification reply handler
 */
public class TransferNotificationActionHandler extends ActionHandler {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TransferNotificationActionHandler.class);

    private static final int ATR_RESULT_OUT_RANK = 0;
    private static final int ARCHIVE_UNIT_MAP_RANK = 0;
    private static final int BINARY_DATAOBJECT_MAP_RANK = 1;
    private static final int BDO_OG_STORED_MAP_RANK = 2;
    private static final int BINARYDATAOBJECT_ID_TO_VERSION_DATAOBJECT_MAP_RANK = 3;
    private static final int SEDA_PARAMETERS_RANK = 4;
    private static final int OBJECT_GROUP_ID_TO_GUID_MAP_RANK = 5;
    static final int HANDLER_IO_PARAMETER_NUMBER = 6;


    private static final String XML = ".xml";
    private static final String HANDLER_ID = "ATR_NOTIFICATION";
    private static final String NAMESPACE_URI = "fr:gouv:culture:archivesdefrance:seda:v2.0";
    private static final String XLINK_URI = "http://www.w3.org/1999/xlink";
    private static final String PREMIS_URI = "info:lc/xmlns/premis-v2";
    private static final String XSI_URI = "http://www.w3.org/2001/XMLSchema-instance";
    private static final String XSD_VERSION = " seda-2.0-main.xsd";

    private HandlerIO handlerIO;
    private static final String DEFAULT_TENANT = "0";
    private static final String DEFAULT_STRATEGY = "default";

    private final List<Class<?>> handlerInitialIOList = new ArrayList<>();
    private final MarshallerObjectCache marshallerObjectCache = new MarshallerObjectCache();
    private StatusCode workflowStatus = StatusCode.UNKNOWN;
    
    /**
     * Constructor TransferNotificationActionHandler
     * 
     * @throws IOException
     * 
     */
    public TransferNotificationActionHandler() {
        for (int i = 0; i < HANDLER_IO_PARAMETER_NUMBER; i++) {
            handlerInitialIOList.add(File.class);
        }
    }

    /**
     * @return HANDLER_ID
     */
    public static final String getId() {
        return HANDLER_ID;
    }

    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO handler) {
        checkMandatoryParameters(params);
        StorageClientFactory storageClientFactory = StorageClientFactory.getInstance();

        final ItemStatus itemStatus = new ItemStatus(HANDLER_ID);

        handlerIO = handler;

        try {
            workflowStatus =
                StatusCode.valueOf(params.getMapParameters().get(WorkerParameterName.workflowStatusKo));
            File atrFile;
            if (workflowStatus.isGreaterOrEqualToKo()) {
                atrFile = createATRKO(params, handlerIO);
            } else {
                // CHeck is only done in OK mode since all parameters are optional
                checkMandatoryIOParameter(handler);
                atrFile = createATROK(params, handlerIO);
            }
            // FIXME P0 : Fix bug on jenkin org.xml.sax.SAXParseException: src-resolve: Cannot resolve the name 'xml:id'
            // to
            // a(n) 'attribute declaration' component.
            // if (new ValidationXsdUtils().checkWithXSD(new FileInputStream(atrFile), SEDA_VALIDATION_FILE)) {
            handler.addOuputResult(ATR_RESULT_OUT_RANK, atrFile, true);
            // store binary data object
            final CreateObjectDescription description = new CreateObjectDescription();
            description.setWorkspaceContainerGUID(params.getContainerName());
            description.setWorkspaceObjectURI(handler.getOutput(ATR_RESULT_OUT_RANK).getPath());
            try (final StorageClient storageClient = storageClientFactory.getClient()) {
                storageClient.storeFileFromWorkspace(
                    DEFAULT_TENANT,
                    DEFAULT_STRATEGY,
                    StorageCollectionType.REPORTS,
                    params.getContainerName() + XML, description);

                if (!workflowStatus.isGreaterOrEqualToKo()) {
                    description.setWorkspaceObjectURI(
                        IngestWorkflowConstants.SEDA_FOLDER + "/" + IngestWorkflowConstants.SEDA_FILE);
                    storageClient.storeFileFromWorkspace(
                        DEFAULT_TENANT,
                        DEFAULT_STRATEGY,
                        StorageCollectionType.MANIFESTS,
                        params.getContainerName() + XML, description);
                }
            }

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
        return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
    }

    /**
     * Serialize a Jaxb POJO object in the current XML stream
     *
     * @param jaxbPOJO
     * @throws ProcessingException
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
        final File atrTmpFile = handlerIO.getNewLocalFile(handlerIO.getOutput(ATR_RESULT_OUT_RANK).getPath());

        // Pre-actions
        final InputStream archiveUnitMapTmpFile = new FileInputStream((File) handlerIO.getInput(ARCHIVE_UNIT_MAP_RANK));
        final Map<String, Object> archiveUnitSystemGuid = JsonHandler.getMapFromInputStream(archiveUnitMapTmpFile);
        final InputStream binaryDataObjectMapTmpFile =
            new FileInputStream((File) handlerIO.getInput(BINARY_DATAOBJECT_MAP_RANK));
        final Map<String, Object> binaryDataObjectSystemGuid =
            JsonHandler.getMapFromInputStream(binaryDataObjectMapTmpFile);
        final InputStream bdoObjectGroupStoredMapTmpFile =
            new FileInputStream((File) handlerIO.getInput(BDO_OG_STORED_MAP_RANK));
        final Map<String, Object> bdoObjectGroupSystemGuid =
            JsonHandler.getMapFromInputStream(bdoObjectGroupStoredMapTmpFile);
        final InputStream binaryDataObjectIdToVersionDataObjectMapTmpFile =
            new FileInputStream((File) handlerIO.getInput(BINARYDATAOBJECT_ID_TO_VERSION_DATAOBJECT_MAP_RANK));
        final Map<String, Object> bdoVersionDataObject =
            JsonHandler.getMapFromInputStream(binaryDataObjectIdToVersionDataObjectMapTmpFile);
        final SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

        final JsonNode sedaParameters = JsonHandler.getFromFile((File) handlerIO.getInput(SEDA_PARAMETERS_RANK));
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
            final Set<String> usedDataObjectGroup = new HashSet<>();
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

            writeAttributeValue(xmlsw, SedaConstants.TAG_REPLY_CODE, workflowStatus.name());
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
        File atrTmpFile = handlerIO.getNewLocalFile(handlerIO.getOutput(ATR_RESULT_OUT_RANK).getPath());

        // Pre-actions
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

        JsonNode infoATR = null;
        String messageIdentifier = null;
        if (handlerIO.getInput(SEDA_PARAMETERS_RANK) != null) {
            final JsonNode sedaParameters = JsonHandler.getFromFile((File) handlerIO.getInput(SEDA_PARAMETERS_RANK));
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

            addKOReplyOutcomeIterator(xmlsw, params.getContainerName());

            xmlsw.writeEndElement(); // END REPLY_OUTCOME
            xmlsw.writeEndElement(); // END MANAGEMENT_METADATA

            writeAttributeValue(xmlsw, SedaConstants.TAG_REPLY_CODE, workflowStatus.name());
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
    private void addKOReplyOutcomeIterator(XMLStreamWriter xmlsw, String containerName)
        throws ProcessingException, XMLStreamException, FileNotFoundException, InvalidParseOperationException {

        // FIXME P0 Unused ?
        Map<String, Object> bdoVersionDataObject = null;

        if (handlerIO.getInput(BINARYDATAOBJECT_ID_TO_VERSION_DATAOBJECT_MAP_RANK) != null) {
            final InputStream binaryDataObjectIdToVersionDataObjectMapTmpFile =
                new FileInputStream((File) handlerIO.getInput(BINARYDATAOBJECT_ID_TO_VERSION_DATAOBJECT_MAP_RANK));
            bdoVersionDataObject =
                JsonHandler.getMapFromInputStream(binaryDataObjectIdToVersionDataObjectMapTmpFile);
        }


        final LogbookOperation logbookOperation;
        try (LogbookOperationsClient client = LogbookOperationsClientFactory.getInstance().getClient()) {
            JsonNode node = client.selectOperationbyId(containerName);
            // FIXME P0 hack since Jackson cannot parse it correctly
            //RequestResponseOK response = JsonHandler.getFromJsonNode(node, RequestResponseOK.class);
            //logbookOperation = JsonHandler.getFromJsonNode(response.getResult(), LogbookOperation.class);
            JsonNode elmt = node.get("$results").get(0);
            if (elmt == null) {
                LOGGER.error("Error while loading logbook operation: no result");
                throw new ProcessingException("Error while loading logbook operation: no result");
            }
            logbookOperation = new LogbookOperation(elmt);
        } catch (LogbookClientException e) {
            LOGGER.error("Error while loading logbook operation", e);
            throw new ProcessingException(e);
        }

        List<Document> logbookOperationEvents =
            (List<Document>) logbookOperation.get(LogbookDocument.EVENTS.toString());
        xmlsw.writeStartElement(SedaConstants.TAG_OPERATION);
        for (Document event : logbookOperationEvents) {
            writeEvent(xmlsw, event, SedaConstants.TAG_OPERATION, null);
        }
        xmlsw.writeEndElement(); // END SedaConstants.TAG_OPERATION

        try (LogbookLifeCyclesClient client = LogbookLifeCyclesClientFactory.getInstance().getClient()) {
            try (VitamRequestIterator iterator = client.unitLifeCyclesByOperationIterator(containerName)) {
                Map<String, Object> archiveUnitSystemGuid = null;
                InputStream archiveUnitMapTmpFile = null;
                File file = (File) handlerIO.getInput(ARCHIVE_UNIT_MAP_RANK);
                if (file != null) {
                    archiveUnitMapTmpFile = new FileInputStream(file);
                }
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
                while (iterator.hasNext()) {
                    LogbookLifeCycleUnit logbookLifeCycleUnit =
                        new LogbookLifeCycleUnit(iterator.next());
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
                        writeAttributeValue(xmlsw, SedaConstants.TAG_ARCHIVE_SYSTEM_ID,
                            logbookLifeCycleUnit.get(SedaConstants.PREFIX_ID).toString());
                    }

                    for (Document event : logbookLifeCycleUnitEvents) {
                        writeEvent(xmlsw, event, SedaConstants.TAG_ARCHIVE_UNIT, null);
                    }
                    xmlsw.writeEndElement(); // END SedaConstants.TAG_ARCHIVE_UNIT
                }
                xmlsw.writeEndElement(); // END SedaConstants.TAG_ARCHIVE_UNIT_LIST
            } catch (LogbookClientException e) {
                LOGGER.error("Error while loading logbook lifecycle units", e);
                throw new ProcessingException(e);
            }
            try (VitamRequestIterator iterator = client.objectGroupLifeCyclesByOperationIterator(containerName)) {
                Map<String, Object> binaryDataObjectSystemGuid = new HashMap<>();
                Map<String, Object> bdoObjectGroupSystemGuid = new HashMap<>();
                Map<String, String> objectGroupGuid = new HashMap<>();
                Map<String, List<String>> dataObjectsForOG = new HashMap<>();
                File file1 = (File) handlerIO.getInput(BINARY_DATAOBJECT_MAP_RANK);
                File file2 = (File) handlerIO.getInput(BDO_OG_STORED_MAP_RANK);
                File file3 = (File) handlerIO.getInput(OBJECT_GROUP_ID_TO_GUID_MAP_RANK);
                if (file1 != null && file2 != null) {
                    InputStream binaryDataObjectMapTmpFile = new FileInputStream(file1);
                    InputStream bdoObjectGroupStoredMapTmpFile =
                        new FileInputStream(file2);
                    binaryDataObjectSystemGuid = JsonHandler.getMapFromInputStream(binaryDataObjectMapTmpFile);
                    bdoObjectGroupSystemGuid = JsonHandler.getMapFromInputStream(bdoObjectGroupStoredMapTmpFile);
                }
                for (final Map.Entry<String, Object> entry : bdoObjectGroupSystemGuid.entrySet()) {
                    String idOG = entry.getValue().toString();
                    String idObj = entry.getKey();
                    if (!dataObjectsForOG.containsKey(idOG)) {
                        List<String> listObj = new ArrayList<>();
                        listObj.add(idObj);
                        dataObjectsForOG.put(idOG, listObj);
                    } else {
                        dataObjectsForOG.get(idOG).add(idObj);
                    }
                }
                if (file3 != null) {
                    InputStream objectGroupGuidMapTmpFile = new FileInputStream(file3);
                    Map<String, Object> objectGroupGuidBefore =
                        JsonHandler.getMapFromInputStream(objectGroupGuidMapTmpFile);
                    if (objectGroupGuidBefore != null) {
                        for (Map.Entry<String, Object> entry : objectGroupGuidBefore.entrySet()) {
                            String guid = entry.getValue().toString();
                            objectGroupGuid.put(guid, entry.getKey());
                        }
                    }
                }

                xmlsw.writeStartElement(SedaConstants.TAG_DATA_OBJECT_LIST);
                while (iterator.hasNext()) {

                    LogbookLifeCycleObjectGroup logbookLifeCycleObjectGroup =
                        new LogbookLifeCycleObjectGroup(iterator.next());

                    String eventIdentifier = null;
                    xmlsw.writeStartElement(SedaConstants.TAG_DATA_OBJECT_GROUP);

                    String ogGUID =
                        logbookLifeCycleObjectGroup.get(LogbookMongoDbName.objectIdentifier.getDbname()) != null
                            ? logbookLifeCycleObjectGroup.get(LogbookMongoDbName.objectIdentifier.getDbname())
                                .toString()
                            : "";
                    String igId = "";
                    if (objectGroupGuid.containsKey(ogGUID)) {
                        igId = objectGroupGuid.get(ogGUID);
                        xmlsw.writeAttribute(SedaConstants.ATTRIBUTE_ID, objectGroupGuid.get(ogGUID));
                    }
                    if (dataObjectsForOG.get(igId) != null) {
                        for (String idObj : dataObjectsForOG.get(igId)) {
                            xmlsw.writeStartElement(SedaConstants.TAG_BINARY_DATA_OBJECT);
                            xmlsw.writeAttribute(SedaConstants.ATTRIBUTE_ID, idObj);
                            if (binaryDataObjectSystemGuid.get(idObj) != null) {
                                writeAttributeValue(xmlsw, SedaConstants.TAG_BINARY_DATA_OBJECT_SYSTEM_ID,
                                    binaryDataObjectSystemGuid.get(idObj).toString());
                            }
                            xmlsw.writeEndElement(); // END SedaConstants.TAG_BINARY_DATA_OBJECT
                        }

                    }
                    List<Document> logbookLifeCycleObjectGroupEvents =
                        (List<Document>) logbookLifeCycleObjectGroup.get(LogbookDocument.EVENTS.toString());
                    for (Document event : logbookLifeCycleObjectGroupEvents) {
                        writeEvent(xmlsw, event, SedaConstants.TAG_DATA_OBJECT_GROUP, eventIdentifier);
                    }
                    xmlsw.writeEndElement(); // END SedaConstants.TAG_DATA_OBJECT_GROUP
                }
                xmlsw.writeEndElement(); // END SedaConstants.TAG_DATA_OBJECT_LIST
            } catch (LogbookClientException e) {
                LOGGER.error("Error while loading logbook lifecycle ObjectGroups", e);
                throw new ProcessingException(e);
            }
        }
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
        if (event.get(LogbookMongoDbName.outcome.getDbname()) != null &&
            (StatusCode.FATAL.toString().equals(event.get(LogbookMongoDbName.outcome.getDbname()).toString()) ||
                StatusCode.KO.toString()
                    .equals(event.get(LogbookMongoDbName.outcome.getDbname()).toString()))) {

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
                        .getLabelLfc(event.get(LogbookMongoDbName.eventType.getDbname()).toString()));
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
            xmlsw.writeEndElement(); // END SedaConstants.TAG_EVENT
        }
    }
}
