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
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.EngineResponse;
import fr.gouv.vitam.processing.common.model.OutcomeMessage;
import fr.gouv.vitam.processing.common.model.ProcessResponse;
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
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

/**
 * Transfer notification reply handler
 */
public class TransferNotificationActionHandler extends ActionHandler {

    private static final String XML = ".xml";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TransferNotificationActionHandler.class);
    private static final String HANDLER_ID = "TransferNotification";
    LogbookOperationParameters parameters = LogbookParametersFactory.newLogbookOperationParameters();
    private static final String NAMESPACE_URI = "fr:gouv:culture:archivesdefrance:seda:v2.0";
    public static final String JSON_EXTENSION = ".json";

    private static final String ATR_FILE_NAME = "responseReply.xml";
    private HandlerIO handlerIO;
    private final StorageClientFactory storageClientFactory;
    private static final String DEFAULT_TENANT = "0";
    private static final String DEFAULT_STRATEGY = "default";

    private final HandlerIO handlerInitialIOList = new HandlerIO(HANDLER_ID);
    private final MarshallerObjectCache marshallerObjectCache = new MarshallerObjectCache();
    public static final int HANDLER_IO_PARAMETER_NUMBER = 5;

    /**
     * Constructor TransferNotificationActionHandler
     * 
     */
    public TransferNotificationActionHandler() {
        storageClientFactory = StorageClientFactory.getInstance();
        for (int i = 0; i < HANDLER_IO_PARAMETER_NUMBER; i++) {
            handlerInitialIOList.addInput(File.class);
        }
    }

    /**
     * @return HANDLER_ID
     */
    public static final String getId() {
        return HANDLER_ID;
    }

    @Override
    public EngineResponse execute(WorkerParameters params, HandlerIO handler) {
        checkMandatoryParameters(params);

        final EngineResponse response = new ProcessResponse().setStatus(StatusCode.OK).setOutcomeMessages(HANDLER_ID,
            OutcomeMessage.ATR_OK);

        handlerIO = handler;

        try {
            checkMandatoryIOParameter(handler);
            final File atrFile = createATR(params, handlerIO);
            // FIXME : Fix bug on jenkin org.xml.sax.SAXParseException: src-resolve: Cannot resolve the name 'xml:id' to
            // a(n) 'attribute declaration' component.
            // if (new ValidationXsdUtils().checkWithXSD(new FileInputStream(atrFile), SEDA_VALIDATION_FILE)) {
            HandlerIO.transferFileFromTmpIntoWorkspace(
                WorkspaceClientFactory.create(params.getUrlWorkspace()),
                params.getContainerName() + ATR_FILE_NAME,
                IngestWorkflowConstants.ATR_FOLDER + "/" + ATR_FILE_NAME,
                params.getContainerName(),
                true);
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
        } catch (ProcessingException | ContentAddressableStorageException e) {
            LOGGER.error(e);
            response.setStatus(StatusCode.KO).setOutcomeMessages(HANDLER_ID, OutcomeMessage.ATR_KO);
        } catch (URISyntaxException | InvalidParseOperationException |
            StorageClientException | IOException e) {
            LOGGER.error(e);
            response.setStatus(StatusCode.FATAL).setOutcomeMessages(HANDLER_ID, OutcomeMessage.ATR_KO);
            // } catch (SAXException | XMLStreamException e) {
            // LOGGER.error(e);
            // response.setStatus(StatusCode.FATAL).setOutcomeMessages(HANDLER_ID, OutcomeMessage.ATR_KO);
        }
        return response;
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
    private File createATR(WorkerParameters params, HandlerIO ioParam)
        throws ProcessingException, ContentAddressableStorageNotFoundException,
        ContentAddressableStorageServerException, URISyntaxException, ContentAddressableStorageException, IOException,
        InvalidParseOperationException {
        ParameterHelper.checkNullOrEmptyParameters(params);
        final String atrPath = params.getContainerName() + ATR_FILE_NAME;
        final File atrTmpFile = PropertiesUtils.fileFromTmpFolder(atrPath);

        // Pre-actions
        final InputStream archiveUnitMapTmpFile = new FileInputStream((File) handlerIO.getInput().get(0));
        final InputStream binaryDataObjectMapTmpFile = new FileInputStream((File) handlerIO.getInput().get(1));
        final InputStream bdoObjectGroupStoredMapTmpFile = new FileInputStream((File) handlerIO.getInput().get(2));
        final InputStream binaryDataObjectIdToVersionDataObjectMapTmpFile =
            new FileInputStream((File) handlerIO.getInput().get(3));

        final Map<String, Object> archiveUnitSystemGuid = JsonHandler.getMapFromInputStream(archiveUnitMapTmpFile);
        final Map<String, Object> binaryDataObjectSystemGuid =
            JsonHandler.getMapFromInputStream(binaryDataObjectMapTmpFile);
        final Map<String, Object> bdoObjectGroupSystemGuid =
            JsonHandler.getMapFromInputStream(bdoObjectGroupStoredMapTmpFile);
        final Map<String, Object> bdoVersionDataObject =
            JsonHandler.getMapFromInputStream(binaryDataObjectIdToVersionDataObjectMapTmpFile);
        final SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

        final JsonNode sedaParameters = JsonHandler.getFromFile((File) handlerIO.getInput().get(4));
        final JsonNode infoATR =
            sedaParameters.get(SedaConstants.TAG_ARCHIVE_TRANSFER).get(SedaConstants.TAG_ARCHIVE_TRANSFER);
        final String messageIdentifier = infoATR.get(SedaConstants.TAG_MESSAGE_IDENTIFIER).textValue();
        // creation of ATR report
        try {
            final XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
            final FileWriter artTmpFileWriter = new FileWriter(atrTmpFile);

            final XMLStreamWriter xmlsw = outputFactory.createXMLStreamWriter(artTmpFileWriter);
            xmlsw.writeStartDocument();

            xmlsw.writeStartElement(SedaConstants.TAG_ARCHIVE_TRANSFER_REPLY);

            xmlsw.writeNamespace("xlink", "http://www.w3.org/1999/xlink");
            xmlsw.writeNamespace("pr", "info:lc/xmlns/premis-v2");
            xmlsw.writeDefaultNamespace(NAMESPACE_URI);
            xmlsw.writeNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
            xmlsw.writeAttribute("xsi", "http://www.w3.org/2001/XMLSchema-instance", "schemaLocation",
                NAMESPACE_URI + " seda-2.0-main.xsd");

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
}
