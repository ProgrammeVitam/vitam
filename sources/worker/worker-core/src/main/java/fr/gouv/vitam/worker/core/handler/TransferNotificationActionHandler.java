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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.culture.archivesdefrance.seda.v2.ArchiveTransferReplyType;
import fr.gouv.culture.archivesdefrance.seda.v2.ArchiveUnitType;
import fr.gouv.culture.archivesdefrance.seda.v2.CodeListVersionsType;
import fr.gouv.culture.archivesdefrance.seda.v2.CodeType;
import fr.gouv.culture.archivesdefrance.seda.v2.DataObjectGroupType;
import fr.gouv.culture.archivesdefrance.seda.v2.DataObjectPackageType;
import fr.gouv.culture.archivesdefrance.seda.v2.DescriptiveMetadataContentType;
import fr.gouv.culture.archivesdefrance.seda.v2.DescriptiveMetadataType;
import fr.gouv.culture.archivesdefrance.seda.v2.EventLogBookOgType;
import fr.gouv.culture.archivesdefrance.seda.v2.EventType;
import fr.gouv.culture.archivesdefrance.seda.v2.IdentifierType;
import fr.gouv.culture.archivesdefrance.seda.v2.LogBookOgType;
import fr.gouv.culture.archivesdefrance.seda.v2.LogBookType;
import fr.gouv.culture.archivesdefrance.seda.v2.ManagementMetadataType;
import fr.gouv.culture.archivesdefrance.seda.v2.ManagementType;
import fr.gouv.culture.archivesdefrance.seda.v2.MinimalDataObjectType;
import fr.gouv.culture.archivesdefrance.seda.v2.ObjectFactory;
import fr.gouv.culture.archivesdefrance.seda.v2.OperationType;
import fr.gouv.culture.archivesdefrance.seda.v2.OrganizationWithIdType;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.collection.CloseableIterator;
import fr.gouv.vitam.common.collection.CloseableIteratorUtils;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.IngestWorkflowConstants;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.LifeCycleStatusCode;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.utils.SupportedSedaVersions;
import fr.gouv.vitam.common.xml.ValidationXsdUtils;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookDocument;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleObjectGroupInProcess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleUnitInProcess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookOperation;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
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
import fr.gouv.vitam.worker.common.utils.SedaIngestParams;
import fr.gouv.vitam.worker.core.MarshallerObjectCache;
import fr.gouv.vitam.worker.core.impl.HandlerIOImpl;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import org.bson.Document;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static javax.xml.datatype.DatatypeFactory.newInstance;

/**
 * Transfer notification reply handler
 */
public class TransferNotificationActionHandler extends ActionHandler {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TransferNotificationActionHandler.class);

    // Output params
    private static final int ATR_RESULT_OUT_RANK = 0;

    //  input Params
    private static final int ARCHIVE_UNIT_MAP_RANK = 0;
    private static final int DATAOBJECT_MAP_RANK = 1;
    private static final int BDO_OG_STORED_MAP_RANK = 2;
    private static final int DATAOBJECT_ID_TO_DATAOBJECT_DETAIL_MAP_RANK = 3;
    private static final int SEDA_PARAMETERS_RANK = 4;
    private static final int OBJECT_GROUP_ID_TO_GUID_MAP_RANK = 5;
    private static final int EXISTING_GOT_TO_NEW_GOT_GUID_FOR_ATTACHMENT_RANK = 6;
    private static final int SEDA_INGEST_PARAMS = 7;
    public static final String SEDA_INGEST_PARAMS_FILE = "Maps/sedaParams.json";

    static final int HANDLER_IO_PARAMETER_NUMBER = 8;

    private static final String XML = ".xml";
    private static final String HANDLER_ID = "ATR_NOTIFICATION";
    private static final ObjectFactory objectFactory = new ObjectFactory();

    private static final String EVENT_ID_PROCESS = "evIdProc";
    private static final String TEST_STATUS_PREFIX = "Test ";
    private static final String ATR_SEDA_2_2_TRANSFORMER = "transform-atr-seda-2.2.xsl";
    private static final String ATR_SEDA_2_1_TRANSFORMER = "transform-atr-seda-2.1.xsl";
    private static final String ATR_SEDA_2_3_TRANSFORMER = "transform-atr-seda-2.3.xsl";

    private final LogbookOperationsClientFactory logbookOperationsClientFactory;
    private final StorageClientFactory storageClientFactory;
    private final ValidationXsdUtils validationXsdUtils;

    private final TransformerFactory transformerFactory;

    public TransferNotificationActionHandler() {
        this(LogbookOperationsClientFactory.getInstance(), StorageClientFactory.getInstance(),
            ValidationXsdUtils.getInstance());
    }

    @VisibleForTesting
    TransferNotificationActionHandler(
        LogbookOperationsClientFactory logbookOperationsClientFactory, StorageClientFactory storageClientFactory,
        ValidationXsdUtils validationXsdUtils) {
        this.logbookOperationsClientFactory = logbookOperationsClientFactory;
        this.storageClientFactory = storageClientFactory;
        this.validationXsdUtils = validationXsdUtils;
        this.transformerFactory = TransformerFactory.newInstance();
    }

    /**
     * @return HANDLER_ID
     */
    public static String getId() {
        return HANDLER_ID;
    }

    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO handlerIO) {
        checkMandatoryParameters(params);
        String eventDetailData;

        final ItemStatus itemStatus = new ItemStatus(HANDLER_ID);
        try {
            StatusCode workflowStatus =
                StatusCode.valueOf(params.getMapParameters().get(WorkerParameterName.workflowStatusKo));

            File atrFile;

            final LogbookOperation logbookOperation = getLogbookOperation(params);

            String atrObjectName = handlerIO.getOutput(ATR_RESULT_OUT_RANK).getPath();
            // Generate ATR only if not already generated (idempotency)
            try (WorkspaceClient workspaceClient = handlerIO.getWorkspaceClientFactory().getClient()) {

                boolean atrAlreadyGenerated =
                    workspaceClient.isExistingObject(params.getContainerName(), atrObjectName);

                SedaIngestParams sedaIngestParams;
                if (handlerIO.isExistingFileInWorkspace(SEDA_INGEST_PARAMS_FILE)) {
                    sedaIngestParams =
                        JsonHandler.getFromFile(handlerIO.getInput(SEDA_INGEST_PARAMS, File.class),
                            SedaIngestParams.class);
                    if (sedaIngestParams == null) {
                        throw new ProcessingException("Seda ingest params is empty!");
                    }
                } else {
                    // IF any problem occured before executing the CHECK_SEDA action,
                    // the ATR generation will be based on default SEDA version wich is 2.2
                    LOGGER.warn("Seda Ingest has not been created!");
                    sedaIngestParams = new SedaIngestParams(SupportedSedaVersions.SEDA_2_2.getVersion(),
                        SupportedSedaVersions.SEDA_2_2.getNamespaceURI());
                }

                if (atrAlreadyGenerated) {
                    atrFile = handlerIO.getFileFromWorkspace(atrObjectName);
                    checkAtrFile(atrFile, sedaIngestParams);
                } else {
                    atrFile = createATR(params, handlerIO, logbookOperation, workflowStatus, sedaIngestParams);
                    try (InputStream is = new FileInputStream(atrFile)) {
                        workspaceClient.putAtomicObject(handlerIO.getContainerName(), atrObjectName, is,
                            atrFile.length());
                    }
                }
            }

            // calculate digest by vitam alog
            String vitamDigestString;
            try (FileInputStream inputStream = new FileInputStream(atrFile)) {
                Digest vitamDigest = new Digest(VitamConfiguration.getDefaultDigestType());
                vitamDigestString = vitamDigest.update(inputStream).digestHex();
            }

            LOGGER.debug(
                "DEBUG: \n\t" + vitamDigestString);
            // define eventDetailData
            eventDetailData =
                "{" +
                    "\"FileName\":\"" + "ATR_" + params.getContainerName() +
                    "\", \"MessageDigest\": \"" + vitamDigestString +
                    "\", \"Algorithm\": \"" + VitamConfiguration.getDefaultDigestType() + "\"}";

            itemStatus.setEvDetailData(eventDetailData);

            // store data object
            final ObjectDescription description = new ObjectDescription();
            description.setWorkspaceContainerGUID(params.getContainerName());
            description.setWorkspaceObjectURI(atrObjectName);
            try (final StorageClient storageClient = storageClientFactory.getClient()) {
                storageClient.storeFileFromWorkspace(
                    VitamConfiguration.getDefaultStrategy(),
                    DataCategory.REPORT,
                    params.getContainerName() + XML, description);

                if (!workflowStatus.isGreaterOrEqualToKo()) {

                    description.setWorkspaceObjectURI(
                        IngestWorkflowConstants.SEDA_FOLDER + "/" + IngestWorkflowConstants.SEDA_FILE);
                    storageClient.storeFileFromWorkspace(
                        VitamConfiguration.getDefaultStrategy(),
                        DataCategory.MANIFEST,
                        params.getContainerName() + XML, description);
                }
            }

            itemStatus.increment(StatusCode.OK);
        } catch (InvalidParseOperationException | StorageClientException | IOException | ProcessingException |
                 ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException e) {
            LOGGER.error(e);
            itemStatus.increment(StatusCode.FATAL);
        }

        return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
    }

    private void checkAtrFile(File atrFile, SedaIngestParams sedaIngestParams) throws IOException {
        try {
            validationXsdUtils.checkWithXSD(new FileInputStream(atrFile), sedaIngestParams.getSedaValidatorXSD());
        } catch (SAXException e) {
            if (e.getCause() == null) {
                // In case of an exception while parsing the manifest file, when the exception is thrown before parsing
                // complete basic elements for constructing a valid ATR file, this cause errors in conformity with the
                // XSD schema file, so we construct an ATR file using the minimum of informations to reply clients
                // with the important errors, even with empty tags that broke the schema conformity.
                LOGGER.error("ATR File is not valid with the XSD", e);
            }
            LOGGER.error("ATR File is not a correct xml file", e);
        } catch (XMLStreamException e) {
            LOGGER.error("ATR File is not a correct xml file", e);
        }
    }

    private LogbookOperation getLogbookOperation(WorkerParameters params)
        throws InvalidParseOperationException, ProcessingException {
        LogbookOperation logbookOperation;
        try (LogbookOperationsClient client = logbookOperationsClientFactory.getClient()) {
            Select select = new Select();
            select.setQuery(QueryHelper.eq(EVENT_ID_PROCESS, params.getContainerName()));
            final JsonNode node = client.selectOperationById(params.getContainerName());
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
        return logbookOperation;
    }

    /**
     * create ATR in all  processing cases (Ok or KO)
     *
     * @param params of type WorkerParameters
     * @param handlerIO of type HandlerIO
     * @param workflowStatus
     * @param sedaIngestParams
     * @throws ProcessingException ProcessingException
     * @throws InvalidParseOperationException InvalidParseOperationException
     */
    private File createATR(WorkerParameters params, HandlerIO handlerIO, LogbookOperation logbookOperation,
        StatusCode workflowStatus, SedaIngestParams sedaIngestParams)
        throws ProcessingException, InvalidParseOperationException {

        ParametersChecker.checkNullOrEmptyParameters(params);

        final File atrFile = handlerIO.getNewLocalFile(handlerIO.getOutput(ATR_RESULT_OUT_RANK).getPath());

        // creation of ATR OK/KO report
        try {

            ArchiveTransferReplyType archiveTransferReply = objectFactory.createArchiveTransferReplyType();

            addFirstLevelBaseInformations(archiveTransferReply, params, logbookOperation, handlerIO, workflowStatus);

            List<String> statusToBeChecked = new ArrayList<>();

            //ATR KO
            if (workflowStatus.isGreaterOrEqualToKo()) {
                statusToBeChecked.add(StatusCode.FATAL.toString());
                statusToBeChecked.add(StatusCode.KO.toString());
            } else { //ATR OK
                // CHeck is only done in OK mode since all parameters are optional
                checkMandatoryIOParameter(handlerIO);
                statusToBeChecked.add(StatusCode.WARNING.toString());
            }

            // if KO ATR KO or ATR Ok and status equals WARNING, we'll add informations here
            if (workflowStatus.isGreaterOrEqualToKo() || StatusCode.WARNING.name().equals(workflowStatus.name())) {
                addOperation(archiveTransferReply, logbookOperation, statusToBeChecked);
            }

            addDataObjectPackage(handlerIO, archiveTransferReply, params.getContainerName(), statusToBeChecked,
                workflowStatus);

            MarshallerObjectCache marshallerObjectCache = new MarshallerObjectCache();
            Marshaller archiveTransferReplyMarshaller =
                marshallerObjectCache.getMarshaller(ArchiveTransferReplyType.class);
            archiveTransferReplyMarshaller
                .setProperty(Marshaller.JAXB_SCHEMA_LOCATION,
                    sedaIngestParams.getNamespaceURI() + " " + sedaIngestParams.getSedaValidatorXSD());
            File atrWithUnifiedSedaVersion = handlerIO.getNewLocalFile("_atr_unified_seda.xml");
            archiveTransferReplyMarshaller.marshal(archiveTransferReply, atrWithUnifiedSedaVersion);
            transformAtrFile(sedaIngestParams.getVersion(), atrFile, atrWithUnifiedSedaVersion);

        } catch (IOException e) {
            throw new ProcessingException(e);
        } catch (JAXBException e) {
            String msgErr = "Error on marshalling object archiveTransferReply";
            throw new ProcessingException(msgErr, e);
        } catch (TransformerException e) {
            String msgErr = "Error on transforming ATR file!";
            throw new ProcessingException(msgErr, e);
        }

        return atrFile;
    }

    private void transformAtrFile(String sedaVersion, File atrFile, File atrWithUnifiedSedaVersion)
        throws FileNotFoundException, TransformerException {
        Source xsl;
        if (sedaVersion.equals(SupportedSedaVersions.SEDA_2_1.getVersion())) {
            xsl = new StreamSource(PropertiesUtils.getResourceAsStream(ATR_SEDA_2_1_TRANSFORMER));
        } else if (sedaVersion.equals(SupportedSedaVersions.SEDA_2_3.getVersion())) {
            xsl = new StreamSource(PropertiesUtils.getResourceAsStream(ATR_SEDA_2_3_TRANSFORMER));
        } else {
            // Default to SEDA 2.2 version
            xsl = new StreamSource(PropertiesUtils.getResourceAsStream(ATR_SEDA_2_2_TRANSFORMER));
        }
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
        transformer.transform(new StreamSource(atrWithUnifiedSedaVersion), new StreamResult(atrFile));
    }


    private void addFirstLevelBaseInformations(ArchiveTransferReplyType archiveTransferReply, WorkerParameters params,
        LogbookOperation logbookOperation, HandlerIO handlerIO,
        StatusCode workflowStatus)
        throws InvalidParseOperationException {

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

        archiveTransferReply.setDate(buildXMLGregorianCalendar());

        archiveTransferReply.setMessageIdentifier(params.getContainerName());
        //to attach to DataObjectPackage
        ManagementMetadataType mgmtMetadata = objectFactory.createManagementMetadataType();
        DescriptiveMetadataType descMetaData = objectFactory.createDescriptiveMetadataType();
        DataObjectPackageType dataObjectPackage = objectFactory.createDataObjectPackageType();

        dataObjectPackage.setDescriptiveMetadata(descMetaData);
        dataObjectPackage.setManagementMetadata(mgmtMetadata);
        archiveTransferReply.setDataObjectPackage(dataObjectPackage);


        if (logbookOperation.get(LogbookMongoDbName.eventDetailData.getDbname()) != null) {
            final JsonNode evDetDataNode = JsonHandler.getFromString(
                logbookOperation.get(LogbookMongoDbName.eventDetailData.getDbname()).toString());
            if (evDetDataNode.get(SedaConstants.TAG_ARCHIVE_PROFILE) != null) {

                final String profilId = evDetDataNode.get(SedaConstants.TAG_ARCHIVE_PROFILE).asText();
                mgmtMetadata.setArchivalProfile(buildIdentifierType(profilId));
            }
        }

        if (infoATR != null) {
            archiveTransferReply.setArchivalAgreement(
                buildIdentifierType(
                    (infoATR.get(SedaConstants.TAG_ARCHIVAL_AGREEMENT) != null)
                        ? infoATR.get(SedaConstants.TAG_ARCHIVAL_AGREEMENT).textValue()
                        : ""
                )
            );
        }

        CodeListVersionsType codeListVersions = objectFactory.createCodeListVersionsType();
        archiveTransferReply.setCodeListVersions(codeListVersions);

        if (infoATR != null && infoATR.get(SedaConstants.TAG_CODE_LIST_VERSIONS) != null &&
            infoATR.get(SedaConstants.TAG_CODE_LIST_VERSIONS).get(SedaConstants.TAG_REPLY_CODE_LIST_VERSION) != null) {
            codeListVersions.setReplyCodeListVersion(buildCodeType(
                infoATR.get(SedaConstants.TAG_CODE_LIST_VERSIONS).get(SedaConstants.TAG_REPLY_CODE_LIST_VERSION)
                    .textValue()));
        } else {
            codeListVersions.setReplyCodeListVersion(buildCodeType(""));
        }

        if (infoATR != null && infoATR.get(SedaConstants.TAG_CODE_LIST_VERSIONS) != null &&
            infoATR.get(SedaConstants.TAG_CODE_LIST_VERSIONS)
                .get(SedaConstants.TAG_MESSAGE_DIGEST_ALGORITHM_CODE_LIST_VERSION) != null) {
            codeListVersions.setMessageDigestAlgorithmCodeListVersion(buildCodeType(
                infoATR.get(SedaConstants.TAG_CODE_LIST_VERSIONS)
                    .get(SedaConstants.TAG_MESSAGE_DIGEST_ALGORITHM_CODE_LIST_VERSION).textValue()));
        } else {
            codeListVersions
                .setMessageDigestAlgorithmCodeListVersion(buildCodeType(""));
        }

        if (infoATR != null && infoATR.get(SedaConstants.TAG_CODE_LIST_VERSIONS) != null &&
            infoATR.get(SedaConstants.TAG_CODE_LIST_VERSIONS).get(SedaConstants.TAG_FILE_FORMAT_CODE_LIST_VERSION) !=
                null) {
            codeListVersions.setFileFormatCodeListVersion(buildCodeType(
                infoATR.get(SedaConstants.TAG_CODE_LIST_VERSIONS).get(SedaConstants.TAG_FILE_FORMAT_CODE_LIST_VERSION)
                    .textValue()));
        } else {
            codeListVersions.setFileFormatCodeListVersion(buildCodeType(""));
        }

        LogbookTypeProcess logbookTypeProcess = params.getLogbookTypeProcess();
        if (LogbookTypeProcess.INGEST_TEST.equals(logbookTypeProcess)) {
            archiveTransferReply.setReplyCode(TEST_STATUS_PREFIX + workflowStatus.name());
        } else {
            archiveTransferReply.setReplyCode(workflowStatus.name());
            archiveTransferReply.setGrantDate(buildXMLGregorianCalendar());
        }

        archiveTransferReply.setMessageRequestIdentifier(buildIdentifierType(messageIdentifier));

        if (infoATR != null && infoATR.get(SedaConstants.TAG_ARCHIVAL_AGENCY) != null) {
            archiveTransferReply.setArchivalAgency(
                buildOrganizationWithIdType(

                    (infoATR.get(SedaConstants.TAG_ARCHIVAL_AGENCY).get(SedaConstants.TAG_IDENTIFIER) != null)
                        ? infoATR.get(SedaConstants.TAG_ARCHIVAL_AGENCY).get(SedaConstants.TAG_IDENTIFIER).textValue()
                        : ""
                )
            );
        }

        if (infoATR != null && infoATR.get(SedaConstants.TAG_TRANSFERRING_AGENCY) != null) {
            archiveTransferReply.setTransferringAgency(
                buildOrganizationWithIdType(

                    (infoATR.get(SedaConstants.TAG_TRANSFERRING_AGENCY).get(SedaConstants.TAG_IDENTIFIER) != null)
                        ? infoATR.get(SedaConstants.TAG_TRANSFERRING_AGENCY).get(SedaConstants.TAG_IDENTIFIER)
                        .textValue()
                        : ""
                )
            );
        }
    }

    private XMLGregorianCalendar buildXMLGregorianCalendar() {
        try {
            final SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
            return newInstance().newXMLGregorianCalendar(sdfDate.format(new Date()));
        } catch (DatatypeConfigurationException e) {
            LOGGER.error("The implementation of DatatypeFactory is not available or cannot be instantiated", e);
        }
        return null;
    }

    private IdentifierType buildIdentifierType(String value) {
        IdentifierType identifierType = objectFactory.createIdentifierType();
        identifierType.setValue(value);

        return identifierType;
    }

    private CodeType buildCodeType(String value) {
        CodeType codeType = objectFactory.createCodeType();
        codeType.setValue(value);

        return codeType;
    }

    private OrganizationWithIdType buildOrganizationWithIdType(String value) {
        OrganizationWithIdType organizationWithIdType = objectFactory.createOrganizationWithIdType();
        organizationWithIdType.setIdentifier(buildIdentifierType(value));

        return organizationWithIdType;
    }



    private CloseableIterator<JsonNode> handlerLogbookLifeCycleUnit(String operationId,
        LogbookLifeCyclesClient client, LifeCycleStatusCode lifeCycleStatusCode) {
        try {
            Select select = new Select();
            return client.unitLifeCyclesByOperationIterator(operationId,
                lifeCycleStatusCode, select.getFinalSelect());
        } catch (LogbookClientException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Add DataObjectPackage element to the ATR xml
     *
     * @param archiveTransferReply the archiveTransferReplyType object to populate
     * @param containerName the operation identifier
     * @param statusToBeChecked depends of ATR status (KO={FATAL,KO} or OK=Warning)
     * @param workflowStatus
     * @throws ProcessingException thrown if a logbook could not be retrieved
     * @throws FileNotFoundException FileNotFoundException
     * @throws InvalidParseOperationException InvalidParseOperationException
     */
    private void addDataObjectPackage(HandlerIO handlerIO, ArchiveTransferReplyType archiveTransferReply,
        String containerName,
        List<String> statusToBeChecked, StatusCode workflowStatus)
        throws ProcessingException, FileNotFoundException, InvalidParseOperationException {

        try (LogbookLifeCyclesClient client = handlerIO.getLifecyclesClient()) {
            ////Build DescriptiveMetadata/List(ArchiveUnit)
            try {

                Map<String, Object> archiveUnitSystemGuid;
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
                        //build archiveUnit List
                        List<ArchiveUnitType> auList =
                            archiveTransferReply.getDataObjectPackage().getDescriptiveMetadata().getArchiveUnit();
                        //case KO or OK with warning
                        if (workflowStatus.isGreaterOrEqualToKo() ||
                            StatusCode.WARNING.name().equals(workflowStatus.name())) {

                            try (CloseableIterator<JsonNode> lifecyclesSpliterator =
                                handlerLogbookLifeCycleUnit(containerName, client,
                                    LifeCycleStatusCode.LIFE_CYCLE_IN_PROCESS)) {

                                CloseableIteratorUtils.map(lifecyclesSpliterator, LogbookLifeCycleUnitInProcess::new)
                                    .forEachRemaining(logbookLifeCycleUnit -> auList.add(
                                        buildArchiveUnit(statusToBeChecked,
                                            systemGuidArchiveUnitId, logbookLifeCycleUnit))
                                    );
                            }
                        } else {
                            //set only archiveUnit(id,systemId) List
                            auList.addAll(buildListOfSimpleArchiveUnitWithoutEvents(archiveUnitSystemGuid));
                        }
                    }
                }

            } catch (final IllegalStateException e) {
                throw new ProcessingException("Exception when building ArchiveUnitList for ArchiveTransferReply KO", e);
            }

            //Build DataObjectGroup
            final Map<String, String> objectGroupGuid = new HashMap<>();
            final Map<String, List<String>> dataObjectsForOG = new HashMap<>();
            final File dataObjectMapTmpFile = (File) handlerIO.getInput(DATAOBJECT_MAP_RANK);
            final File bdoObjectGroupStoredMapTmpFile = (File) handlerIO.getInput(BDO_OG_STORED_MAP_RANK);
            final File objectGroupSystemGuidTmpFile = (File) handlerIO.getInput(OBJECT_GROUP_ID_TO_GUID_MAP_RANK);
            final File dataObjectToDetailDataObjectMapTmpFile =
                (File) handlerIO.getInput(DATAOBJECT_ID_TO_DATAOBJECT_DETAIL_MAP_RANK);
            final File existingGOTGUIDToNewGotGUIDInAttachmentMapTmpFile =
                (File) handlerIO.getInput(EXISTING_GOT_TO_NEW_GOT_GUID_FOR_ATTACHMENT_RANK);

            Map<String, Object> dataObjectSystemGuid = getDataObjectSystemGuid(dataObjectMapTmpFile);
            Map<String, Object> bdoObjectGroupSystemGuid = getBdoObjectGroupSystemGuid(bdoObjectGroupStoredMapTmpFile);

            try {

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

                Map<String, Object> objectGroupSystemGuid = null;

                if (objectGroupSystemGuidTmpFile != null) {
                    final InputStream objectGroupGuidMapFIS = new FileInputStream(objectGroupSystemGuidTmpFile);
                    objectGroupSystemGuid =
                        JsonHandler.getMapFromInputStream(objectGroupGuidMapFIS);
                    if (objectGroupSystemGuid != null) {
                        for (final Map.Entry<String, Object> entry : objectGroupSystemGuid.entrySet()) {
                            final String guid = entry.getValue().toString();
                            objectGroupGuid.put(guid, entry.getKey());
                        }
                    }
                }

                Map<String, DataObjectDetail> dataObjectToDetailDataObject;

                if (dataObjectToDetailDataObjectMapTmpFile != null) {
                    final InputStream dataObjectToDetailDataObjectMapFIS =
                        new FileInputStream(dataObjectToDetailDataObjectMapTmpFile);
                    dataObjectToDetailDataObject =
                        JsonHandler.getMapFromInputStream(dataObjectToDetailDataObjectMapFIS,
                            DataObjectDetail.class);
                } else {
                    dataObjectToDetailDataObject = new HashMap<>();
                }

                final Map<String, String> existingGOTGUIDToNewGotGUIDInAttachment;

                if (existingGOTGUIDToNewGotGUIDInAttachmentMapTmpFile != null) {
                    final InputStream existingGOTGUIDToNewGotGUIDInAttachmentMapFIS =
                        new FileInputStream(existingGOTGUIDToNewGotGUIDInAttachmentMapTmpFile);
                    existingGOTGUIDToNewGotGUIDInAttachment =
                        JsonHandler.getMapFromInputStream(existingGOTGUIDToNewGotGUIDInAttachmentMapFIS, String.class);
                } else {
                    existingGOTGUIDToNewGotGUIDInAttachment = new HashMap<>();
                }

                //build DataObjectGroup object List
                List<Object> dataObjectGroupList = archiveTransferReply.getDataObjectPackage()
                    .getDataObjectGroupOrBinaryDataObjectOrPhysicalDataObject();

                if (dataObjectSystemGuid != null) {
                    //case KO or OK with warning
                    if (workflowStatus.isGreaterOrEqualToKo() ||
                        StatusCode.WARNING.name().equals(workflowStatus.name())) {

                        try (CloseableIterator<JsonNode> lifecyclesSpliterator =
                            handleLogbookLifeCyclesObjectGroup(containerName, client)) {

                            CloseableIteratorUtils.map(lifecyclesSpliterator, LogbookLifeCycleObjectGroupInProcess::new)
                                .forEachRemaining(logbookLifeCycleObjectGroup -> dataObjectGroupList.add(
                                    buildDataObjectGroup(statusToBeChecked, objectGroupGuid, dataObjectsForOG,
                                        dataObjectSystemGuid,
                                        dataObjectToDetailDataObject, existingGOTGUIDToNewGotGUIDInAttachment,
                                        logbookLifeCycleObjectGroup))
                                );
                        }

                    } else {

                        dataObjectGroupList.addAll(
                            buildListOfSimpleDataObjectGroup(dataObjectsForOG, dataObjectSystemGuid,
                                dataObjectToDetailDataObject, objectGroupSystemGuid,
                                existingGOTGUIDToNewGotGUIDInAttachment)
                        );
                    }
                }

            } catch (final IllegalStateException | InvalidParseOperationException |
                           IllegalArgumentException e) {
                throw new ProcessingException("Exception when building DataObjectGroup for ArchiveTransferReply KO", e);
            }
        }
    }

    private Map<String, Object> getBdoObjectGroupSystemGuid(File bdoObjectGroupStoredMapTmpFile)
        throws InvalidParseOperationException, ProcessingException {
        if (bdoObjectGroupStoredMapTmpFile == null) {
            return new HashMap<>();
        }

        try (InputStream bdoObjectGroupStoredMapTmpFIStream = new FileInputStream(bdoObjectGroupStoredMapTmpFile)) {
            return JsonHandler.getMapFromInputStream(bdoObjectGroupStoredMapTmpFIStream);
        } catch (IOException e) {
            throw new ProcessingException(e);
        }

    }

    private Map<String, Object> getDataObjectSystemGuid(File dataObjectMapTmpFile)
        throws ProcessingException, InvalidParseOperationException {
        if (dataObjectMapTmpFile == null) {
            return new HashMap<>();
        }

        try (InputStream binaryDataObjectMapTmpFIS = new FileInputStream(dataObjectMapTmpFile)) {
            return JsonHandler.getMapFromInputStream(binaryDataObjectMapTmpFIS);
        } catch (IOException e) {
            throw new ProcessingException(e);
        }

    }

    private List<ArchiveUnitType> buildListOfSimpleArchiveUnitWithoutEvents(Map<String, Object> archiveUnitSystemGuid) {

        List<ArchiveUnitType> archiveUnitTypeList = new ArrayList<>();

        for (final Map.Entry<String, Object> entry : archiveUnitSystemGuid.entrySet()) {
            ArchiveUnitType archiveUnit = objectFactory.createArchiveUnitType();
            DescriptiveMetadataContentType descContent = new DescriptiveMetadataContentType();

            archiveUnit.setId(entry.getKey());
            descContent.getSystemId().add(entry.getValue().toString());
            archiveUnit.setContent(descContent);
            archiveUnitTypeList.add(archiveUnit);
        }

        return archiveUnitTypeList;
    }

    private ArchiveUnitType buildArchiveUnit(List<String> statusToBeChecked,
        Map<String, String> systemGuidArchiveUnitId, LogbookLifeCycleUnitInProcess logbookLifeCycleUnit) {

        List<Document> logbookLifeCycleUnitEvents = (List<Document>) logbookLifeCycleUnit.get(LogbookDocument.EVENTS);

        ArchiveUnitType archiveUnit = objectFactory.createArchiveUnitType();
        DescriptiveMetadataContentType descMetadataContent = objectFactory.createDescriptiveMetadataContentType();

        if (!systemGuidArchiveUnitId.isEmpty() &&
            logbookLifeCycleUnit.get(SedaConstants.PREFIX_ID) != null &&
            systemGuidArchiveUnitId
                .get(logbookLifeCycleUnit.get(SedaConstants.PREFIX_ID).toString()) != null) {

            archiveUnit.setId(systemGuidArchiveUnitId
                .get(logbookLifeCycleUnit.get(SedaConstants.PREFIX_ID).toString())
            );

            descMetadataContent.getSystemId().add(
                logbookLifeCycleUnit.get(SedaConstants.PREFIX_ID).toString()
            );
        }

        archiveUnit.setContent(descMetadataContent);

        ManagementType archiveUnitMgmt = objectFactory.createManagementType();

        if (logbookLifeCycleUnitEvents != null && !logbookLifeCycleUnitEvents.isEmpty()) {
            LogBookType logbook = new LogBookType();
            for (final Document document : logbookLifeCycleUnitEvents) {
                EventType eventObject =
                    buildEventByContainerType(document, SedaConstants.TAG_ARCHIVE_UNIT, statusToBeChecked, null);
                if (eventObject != null) {
                    logbook.getEvent().add(eventObject);
                }
            }

            if (!logbook.getEvent().isEmpty()) {
                archiveUnitMgmt.setLogBook(logbook);
            }
        }

        archiveUnit.setManagement(archiveUnitMgmt);

        return archiveUnit;

    }

    private List<DataObjectGroupType> buildListOfSimpleDataObjectGroup(Map<String, List<String>> dataObjectsForOG,
        Map<String, Object> dataObjectSystemGuid, Map<String, DataObjectDetail> dataObjectToDetailDataObject,
        Map<String, Object> objectGroupSystemGuid, Map<String, String> existingGOTGUIDToNewGotGUIDInAttachment) {

        final List<DataObjectGroupType> dataObjectGroupList = new ArrayList<>();

        for (final Map.Entry<String, List<String>> dataObjectGroupEntry : dataObjectsForOG.entrySet()) {

            DataObjectGroupType dataObjectGroup = objectFactory.createDataObjectGroupType();

            String dataObjectGroupId = dataObjectGroupEntry.getKey();

            Object dataObjectGroupSystemIdObject = objectGroupSystemGuid.get(dataObjectGroupId);
            String dataObjectGroupSystemId = dataObjectGroupSystemIdObject.toString();
            //case of GOT attachment
            String finalDataObjectGroupSystemId = dataObjectGroupSystemId;
            dataObjectGroupSystemId = existingGOTGUIDToNewGotGUIDInAttachment.entrySet().stream()
                .filter(key -> key.getValue().equals(finalDataObjectGroupSystemId))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(finalDataObjectGroupSystemId);

            dataObjectGroup.setId(dataObjectGroupId);

            for (final String dataObjectId : dataObjectGroupEntry.getValue()) {
                MinimalDataObjectType binaryOrPhysicalDataObject;

                final DataObjectDetail dataObjectDetail = dataObjectToDetailDataObject.get(dataObjectId);

                if (dataObjectDetail.isPhysical()) {
                    binaryOrPhysicalDataObject = objectFactory.createPhysicalDataObjectType();
                } else {
                    binaryOrPhysicalDataObject = objectFactory.createBinaryDataObjectType();
                }

                binaryOrPhysicalDataObject.setId(dataObjectId);
                binaryOrPhysicalDataObject.setDataObjectGroupSystemId(dataObjectGroupSystemId);

                String dataObjectSystemGUID = dataObjectSystemGuid.get(dataObjectId).toString();
                if (dataObjectSystemGUID != null) {
                    binaryOrPhysicalDataObject.setDataObjectSystemId(dataObjectSystemGUID);
                }

                binaryOrPhysicalDataObject.setDataObjectVersion(dataObjectDetail.getVersion());

                //add  dataObject to dataObjectGroup
                dataObjectGroup.getBinaryDataObjectOrPhysicalDataObject().add(binaryOrPhysicalDataObject);

            }
            //add ObjectGroup object to result list
            dataObjectGroupList.add(dataObjectGroup);
        }

        return dataObjectGroupList;
    }

    private DataObjectGroupType buildDataObjectGroup(
        List<String> statusToBeChecked,
        Map<String, String> objectGroupGuid, Map<String, List<String>> dataObjectsForOG,
        Map<String, Object> dataObjectSystemGuid, Map<String, DataObjectDetail> dataObjectToDetailDataObject,
        Map<String, String> existingGOTGUIDToNewGotGUIDInAttachment,
        LogbookLifeCycleObjectGroupInProcess logbookLifeCycleObjectGroup) {

        Map<String, String> dataObjectSystemGUIDToID = new TreeMap<>();

        DataObjectGroupType dataObjectGroup = objectFactory.createDataObjectGroupType();

        String ogGUID =
            logbookLifeCycleObjectGroup.get(LogbookMongoDbName.objectIdentifier.getDbname()) != null
                ? logbookLifeCycleObjectGroup.get(LogbookMongoDbName.objectIdentifier.getDbname())
                .toString()
                : "";
        String existingObjectGroupSystemGUID = ogGUID;
        //look, in case of GOT attachment, mapping (existing Got in DB --> new GOT)
        if (existingGOTGUIDToNewGotGUIDInAttachment.containsKey(ogGUID)) {
            ogGUID = existingGOTGUIDToNewGotGUIDInAttachment.get(ogGUID);
        }

        String igId = "";
        if (objectGroupGuid.containsKey(ogGUID)) {
            igId = objectGroupGuid.get(ogGUID);
            dataObjectGroup.setId(igId);
        }

        if (dataObjectsForOG.get(igId) != null) {
            for (final String idObj : dataObjectsForOG.get(igId)) {
                MinimalDataObjectType binaryOrPhysicalDataObject;

                if (dataObjectToDetailDataObject.get(idObj) != null &&
                    dataObjectToDetailDataObject.get(idObj).isPhysical()) {
                    binaryOrPhysicalDataObject = objectFactory.createPhysicalDataObjectType();
                } else {
                    binaryOrPhysicalDataObject = objectFactory.createBinaryDataObjectType();
                }

                binaryOrPhysicalDataObject.setId(idObj);
                String dataObjectSystemGUID = dataObjectSystemGuid.get(idObj).toString();
                if (dataObjectSystemGUID != null) {
                    binaryOrPhysicalDataObject.setDataObjectSystemId(dataObjectSystemGUID);
                    dataObjectSystemGUIDToID.put(dataObjectSystemGUID, idObj);
                }
                if (existingObjectGroupSystemGUID != null && !existingObjectGroupSystemGUID.isEmpty()) {
                    binaryOrPhysicalDataObject.setDataObjectGroupSystemId(existingObjectGroupSystemGUID);
                }

                binaryOrPhysicalDataObject.setDataObjectVersion(
                    dataObjectToDetailDataObject.get(idObj) != null ?
                        dataObjectToDetailDataObject.get(idObj).getVersion() :
                        "");

                //add  dataObject to dataObjectGroup
                dataObjectGroup.getBinaryDataObjectOrPhysicalDataObject().add(binaryOrPhysicalDataObject);
            }
        }

        final List<Document> logbookLifeCycleObjectGroupEvents =
            (List<Document>) logbookLifeCycleObjectGroup.get(LogbookDocument.EVENTS);
        if (logbookLifeCycleObjectGroupEvents != null) {
            dataObjectGroup.setLogBook(new LogBookOgType());
            for (final Document eventDoc : logbookLifeCycleObjectGroupEvents) {
                String objectSystemId = eventDoc.get(LogbookMongoDbName.objectIdentifier.getDbname()).toString();
                String objectId = dataObjectSystemGUIDToID.get(objectSystemId);
                Object objectObject = findDataObjectById(dataObjectGroup, objectId);
                dataObjectGroup.getLogBook().getEvent().add(
                    (EventLogBookOgType) buildEventByContainerType(eventDoc,
                        SedaConstants.TAG_DATA_OBJECT_GROUP,
                        statusToBeChecked, objectObject)
                );
            }
        }

        return dataObjectGroup;
    }

    private Object findDataObjectById(DataObjectGroupType dataObjectGroup, String objectId) {
        MinimalDataObjectType dataObject = null;
        if (objectId != null) {

            for (MinimalDataObjectType object : dataObjectGroup.getBinaryDataObjectOrPhysicalDataObject()) {
                if (objectId.equals(object.getId())) {
                    dataObject = object;
                    break;
                }
            }
        }

        return dataObject;
    }

    private void addOperation(ArchiveTransferReplyType archiveTransferReply, LogbookOperation logbookOperation,
        List<String> statusToBeChecked) {

        final List<Document> logbookOperationEvents =
            (List<Document>) logbookOperation.get(LogbookDocument.EVENTS);

        OperationType operation = objectFactory.createOperationType();
        List<EventType> eventList = new ArrayList<>();

        for (final Document event : logbookOperationEvents) {
            eventList.add(buildEventByContainerType(event, SedaConstants.TAG_OPERATION, statusToBeChecked, null));
        }

        operation.getEvent().addAll(eventList);
        archiveTransferReply.setOperation(operation);
    }

    private CloseableIterator<JsonNode> handleLogbookLifeCyclesObjectGroup(String containerName,
        LogbookLifeCyclesClient client) {
        try {
            Select select = new Select();
            return client.objectGroupLifeCyclesByOperationIterator(containerName,
                LifeCycleStatusCode.LIFE_CYCLE_IN_PROCESS, select.getFinalSelect());
        } catch (LogbookClientException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        List<Class<?>> handlerInitialIOList = new ArrayList<>();
        for (int i = 0; i < HANDLER_IO_PARAMETER_NUMBER; i++) {
            handlerInitialIOList.add(File.class);
        }
        if (!handler.checkHandlerIO(1, handlerInitialIOList)) {

            throw new ProcessingException(HandlerIOImpl.NOT_CONFORM_PARAM);
        }
    }

    /**
     * Construct the event object for a given object type in managementMetadata item of ATR
     * Type can be : OperationType, ArchiveUnitType or DataObjectGroupType item
     *
     * @param document mongo document holding event infos
     * @param dataObjectToReference DataObject to reference from the LoogBook/event object to create
     * @return EventType object for operationType and ArchiveUnitType
     * DataObjectGroupType.Event for DataObjectGroupType
     */
    private EventType buildEventByContainerType(Document document, String eventType,
        List<String> statusToBeChecked, Object dataObjectToReference) {
        EventType eventObject = null;

        if (document.get(LogbookMongoDbName.outcome.getDbname()) != null &&
            statusToBeChecked.contains(document.get(LogbookMongoDbName.outcome.getDbname()).toString())) {

            //case of DataObjectGroupType, must return an DataObjectGroupType.Event type object
            if (SedaConstants.TAG_DATA_OBJECT_GROUP.equals(eventType)) {
                eventObject = new EventLogBookOgType();

                if (dataObjectToReference != null) {
                    ((EventLogBookOgType) eventObject).setDataObjectReferenceId(dataObjectToReference);
                }
            } else {
                eventObject = objectFactory.createEventType();
            }

            if (document.get(LogbookMongoDbName.eventType.getDbname()) != null) {
                eventObject.setEventTypeCode(document.get(LogbookMongoDbName.eventType.getDbname()).toString());

                if (SedaConstants.TAG_OPERATION.equals(eventType)) {
                    eventObject.setEventType(
                        VitamLogbookMessages
                            .getLabelOp(document.get(LogbookMongoDbName.eventType.getDbname()).toString())
                    );
                } else if (SedaConstants.TAG_ARCHIVE_UNIT.equals(eventType) || SedaConstants.TAG_DATA_OBJECT_GROUP
                    .equals(eventType)) {
                    eventObject.setEventType(VitamLogbookMessages
                        .getFromFullCodeKey(document.get(LogbookMongoDbName.eventType.getDbname()).toString())
                    );
                }
            }
            if (document.get(LogbookMongoDbName.eventDateTime.getDbname()) != null) {
                eventObject.setEventDateTime(
                    document.get(LogbookMongoDbName.eventDateTime.getDbname()).toString()
                );
            }
            if (document.get(LogbookMongoDbName.outcome.getDbname()) != null) {
                eventObject.setOutcome(
                    document.get(LogbookMongoDbName.outcome.getDbname()).toString()
                );
            }
            if (document.get(LogbookMongoDbName.outcomeDetail.getDbname()) != null) {
                eventObject.setOutcomeDetail(
                    document.get(LogbookMongoDbName.outcomeDetail.getDbname()).toString()
                );
            }
            if (document.get(LogbookMongoDbName.outcomeDetailMessage.getDbname()) != null) {
                eventObject.setOutcomeDetailMessage(
                    document.get(LogbookMongoDbName.outcomeDetailMessage.getDbname()).toString()
                );
            }
            if (document.get(LogbookMongoDbName.eventDetailData.getDbname()) != null) {
                final String detailData = document.get(LogbookMongoDbName.eventDetailData.getDbname()).toString();
                if (detailData.contains(SedaConstants.EV_DET_TECH_DATA)) {
                    eventObject.setEventDetailData(
                        document.get(LogbookMongoDbName.eventDetailData.getDbname()).toString()
                    );
                }
            }

        }
        return eventObject;
    }
}
