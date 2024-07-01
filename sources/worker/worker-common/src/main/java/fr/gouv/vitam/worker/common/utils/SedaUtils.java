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
package fr.gouv.vitam.worker.common.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.SedaConfiguration;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.SedaVersion;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.digest.DigestTypeException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamKoRuntimeException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.IngestWorkflowConstants;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.utils.SupportedSedaVersions;
import fr.gouv.vitam.common.xml.ValidationXsdUtils;
import fr.gouv.vitam.common.xml.XMLInputFactoryUtils;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static fr.gouv.vitam.common.model.VitamConstants.URL_ENCODED_SEPARATOR;

/**
 * SedaUtils to read or split element from SEDA
 */
// the check is done with ParameterHelper and the WorkerParameters classes on the worker (WorkerImpl before the
// handler execute)
// If you absolutely need to check values in handler's methods, also use the ParameterCheker.
public class SedaUtils {

    static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(SedaUtils.class);

    private static final String MSG_PARSING_BDO = "Parsing Binary Data Object";
    private static final String CANNOT_READ_SEDA = "Can not read SEDA";
    private static final String MANIFEST_NOT_FOUND = "Manifest.xml Not Found";
    private static final String CONTAINS_OTHER_TYPE = "ContainsOtherType";
    private static final String INCORRECT_VERSION_FORMAT = "IncorrectVersionFormat";
    private static final String INCORRECT_URI = "IncorrectUri";
    private static final String INCORRECT_PHYSICAL_ID = "IncorrectPhysicalId";
    private static final int USAGE_POSITION = 0;
    private static final int VERSION_POSITION = 1;
    /**
     * nbAUExisting: number of the AU already existing
     */
    public static final String NB_AU_EXISTING = "nbAUExisting";
    public static final String INVALID_DATAOBJECT_VERSION = "INVALID_DATAOBJECT_VERSION";
    public static final String VALID_DATAOBJECT_VERSION = "VALID_DATAOBJECT_VERSION";

    private final HandlerIO handlerIO;

    private static final Pattern namespacePattern = Pattern.compile(
        "^fr:gouv:culture:archivesdefrance:seda:v([0-9]\\.[0-9]+)$"
    );
    private static final String SEDA_PARAMS_ARE_NOT_VALID = "Seda params are not valid!";

    private SedaIngestParams sedaIngestParams;

    protected SedaUtils(HandlerIO handlerIO) {
        this.handlerIO = handlerIO;
    }

    /**
     * get Mandatory values from seda
     *
     * @param params parameters of workspace server
     * @return message id
     * @throws ProcessingException throw when can't read or extract message id from SEDA
     */
    public Map<String, String> getMandatoryValues(WorkerParameters params) throws ProcessingException {
        ParametersChecker.checkNullOrEmptyParameters(params);
        Map<String, String> mandatoryValueMap = new HashMap<>();
        XMLEventReader reader = null;
        try (final InputStream xmlFile = loadIngestManifest()) {
            StringBuilder sedaComment = new StringBuilder();
            final XMLInputFactory xmlInputFactory = XMLInputFactoryUtils.newInstance();
            reader = xmlInputFactory.createXMLEventReader(xmlFile);
            while (true) {
                final XMLEvent event = reader.nextEvent();
                if (event.isStartElement()) {
                    final StartElement element = event.asStartElement();
                    manageMandatoryFields(mandatoryValueMap, reader, sedaComment, element);
                }
                if (event.isEndDocument()) {
                    break;
                }
            }

            if (sedaComment.length() > 0) {
                mandatoryValueMap.put(SedaConstants.TAG_COMMENT, sedaComment.toString());
            }
        } catch (final XMLStreamException | IOException e) {
            LOGGER.error(CANNOT_READ_SEDA, e);
            throw new ProcessingException(e);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (final XMLStreamException e) {
                SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            }
        }
        return mandatoryValueMap;
    }

    public void extractXmlNameSpaceAndSaveSedaParams(HandlerIO handlerIO, int sedaIngestParamsRankOutput)
        throws ProcessingException {
        try (final InputStream xmlFile = loadIngestManifest()) {
            final XMLInputFactory xmlInputFactory = XMLInputFactoryUtils.newInstance();
            final XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(xmlFile);
            if (xmlStreamReader.hasNext() && xmlStreamReader.getEventType() == XMLStreamConstants.START_DOCUMENT) {
                xmlStreamReader.next();
                String namespaceURI = xmlStreamReader.getNamespaceURI();
                if (namespaceURI == null) {
                    throw new ProcessingException("The namespace URI could not be readed from Manifest!");
                }
                extractAndSaveSedaIngestParams(handlerIO, sedaIngestParamsRankOutput, namespaceURI);
            }
        } catch (XMLStreamException e) {
            throw new ProcessingException(CheckSedaValidationStatus.NOT_XML_FILE.name(), e);
        } catch (IOException e) {
            throw new ProcessingException("A technical problem occured when reading Manifest!", e);
        }
    }

    private InputStream loadIngestManifest() throws ProcessingException {
        try {
            return this.handlerIO.getInputStreamFromWorkspace(
                    IngestWorkflowConstants.SEDA_FOLDER + "/" + IngestWorkflowConstants.SEDA_FILE
                );
        } catch (
            ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException | IOException e
        ) {
            LOGGER.error(MANIFEST_NOT_FOUND);
            throw new ProcessingException(e);
        }
    }

    private void manageMandatoryFields(
        Map<String, String> madatoryValueMap,
        XMLEventReader reader,
        StringBuilder sedaComment,
        StartElement element
    ) throws XMLStreamException {
        final QName contractName = new QName(sedaIngestParams.getNamespaceURI(), SedaConstants.TAG_ARCHIVAL_AGREEMENT);
        if (element.getName().equals(contractName)) {
            madatoryValueMap.put(SedaConstants.TAG_ARCHIVAL_AGREEMENT, reader.getElementText());
        }

        final QName messageObjectName = new QName(
            sedaIngestParams.getNamespaceURI(),
            SedaConstants.TAG_MESSAGE_IDENTIFIER
        );
        if (element.getName().equals(messageObjectName)) {
            madatoryValueMap.put(SedaConstants.TAG_MESSAGE_IDENTIFIER, reader.getElementText());
        }

        final QName profilName = new QName(sedaIngestParams.getNamespaceURI(), SedaConstants.TAG_ARCHIVE_PROFILE);
        if (element.getName().equals(profilName)) {
            madatoryValueMap.put(SedaConstants.TAG_ARCHIVE_PROFILE, reader.getElementText());
        }

        final QName submissionAgencyName = new QName(
            sedaIngestParams.getNamespaceURI(),
            SedaConstants.TAG_SUBMISSIONAGENCYIDENTIFIER
        );
        if (element.getName().equals(submissionAgencyName)) {
            madatoryValueMap.put(SedaConstants.TAG_SUBMISSIONAGENCYIDENTIFIER, reader.getElementText());
        }

        final QName commentName = new QName(sedaIngestParams.getNamespaceURI(), SedaConstants.TAG_COMMENT);
        if (element.getName().equals(commentName)) {
            if (!"".equals(sedaComment.toString())) {
                sedaComment.append("_");
            }
            sedaComment.append(reader.getElementText());
        }

        final QName originatingAgencyName = new QName(
            sedaIngestParams.getNamespaceURI(),
            SedaConstants.TAG_ORIGINATINGAGENCYIDENTIFIER
        );
        if (element.getName().equals(originatingAgencyName)) {
            madatoryValueMap.put(SedaConstants.TAG_ORIGINATINGAGENCYIDENTIFIER, reader.getElementText());
        }

        final QName acquisitionInformationName = new QName(
            sedaIngestParams.getNamespaceURI(),
            SedaConstants.TAG_ACQUISITIONINFORMATION
        );
        if (element.getName().equals(acquisitionInformationName)) {
            madatoryValueMap.put(SedaConstants.TAG_ACQUISITIONINFORMATION, reader.getElementText());
        }

        final QName legalStatusName = new QName(sedaIngestParams.getNamespaceURI(), SedaConstants.TAG_LEGALSTATUS);
        if (element.getName().equals(legalStatusName)) {
            madatoryValueMap.put(SedaConstants.TAG_LEGALSTATUS, reader.getElementText());
        }
    }

    private void extractAndSaveSedaIngestParams(
        HandlerIO handlerIO,
        int sedaIngestParamsOutputRank,
        String xmLNameSpace
    ) throws ProcessingException {
        try {
            Pair<String, String> extractedNameSpaceAndSerdaVersion = validateNameSpaceAndSedaVersion(xmLNameSpace);
            sedaIngestParams = new SedaIngestParams(
                extractedNameSpaceAndSerdaVersion.getRight(),
                extractedNameSpaceAndSerdaVersion.getLeft()
            );
            File tempFile = handlerIO.getNewLocalFile(handlerIO.getOutput(sedaIngestParamsOutputRank).getPath());
            JsonHandler.writeAsFile(sedaIngestParams, tempFile);
            handlerIO.addOutputResult(sedaIngestParamsOutputRank, tempFile, true, false);
        } catch (InvalidParseOperationException e) {
            throw new ProcessingException(SEDA_PARAMS_ARE_NOT_VALID);
        }
    }

    /**
     * Returns PAIR of NameSpace and SedaVersion !
     *
     * @param namespaceFromManifest
     * @return
     * @throws ProcessingException
     */
    private Pair<String, String> validateNameSpaceAndSedaVersion(String namespaceFromManifest)
        throws ProcessingException {
        Matcher namespaceMatcher = namespacePattern.matcher(namespaceFromManifest);
        if (namespaceMatcher.find()) {
            return Pair.of(namespaceFromManifest, namespaceMatcher.group(1));
        }
        throw new ProcessingException("Invalid NameSpace pattern in Manifest !");
    }

    /**
     * The method is used to validate SEDA by VITAM SEDA XSD
     *
     * @return a status representing the validation of the file
     */
    public CheckSedaValidationStatus checkSedaValidation(ItemStatus itemStatus) {
        try (InputStream input = checkExistenceManifest()) {
            if (checkMultiManifest()) {
                return CheckSedaValidationStatus.MORE_THAN_ONE_MANIFEST;
            }
            if (!checkFolderContentNumber()) {
                return CheckSedaValidationStatus.MORE_THAN_ONE_FOLDER_CONTENT;
            }
            // Implement version check for every supported Seda version
            Optional<SupportedSedaVersions> sedaSupportedVersionModel = getSupportedSedaModelByVersion(
                sedaIngestParams.getVersion()
            );
            if (sedaSupportedVersionModel.isEmpty()) {
                LOGGER.warn(sedaIngestParams.getVersion() + " is not supported by Vitam!");
                return CheckSedaValidationStatus.UNSUPPORTED_SEDA_VERSION;
            }
            ValidationXsdUtils.getInstance()
                .checkWithXSD(input, sedaSupportedVersionModel.get().getVitamValidatorXSD());

            return CheckSedaValidationStatus.VALID;
        } catch (ProcessingException | IOException e) {
            LOGGER.error("Manifest xml file is not valid with the XSD", e);
            return CheckSedaValidationStatus.NO_FILE;
        } catch (final XMLStreamException e) {
            LOGGER.error("This is not an xml file", e);
            return CheckSedaValidationStatus.NOT_XML_FILE;
        } catch (final SAXException e) {
            LOGGER.error("The xml file is not conform with XSD Schema : ", e);
            // if the cause is null, that means the file is an xml, but it does not validate the XSD
            if (e.getCause() == null) {
                JsonNode errorNode = JsonHandler.createObjectNode().put(SedaConstants.EV_DET_TECH_DATA, e.getMessage());
                itemStatus.setEvDetailData(errorNode.toString());
                return CheckSedaValidationStatus.NOT_XSD_VALID;
            }
            return CheckSedaValidationStatus.NOT_XML_FILE;
        }
    }

    private Optional<SupportedSedaVersions> getSupportedSedaModelByVersion(String sedaVersion) {
        return Arrays.stream(SupportedSedaVersions.values())
            .filter(e -> e.getVersion().equals(sedaVersion))
            .findFirst();
    }

    /**
     * Check Seda Validation status values
     */
    public enum CheckSedaValidationStatus {
        /**
         * VALID XML File
         */
        VALID,
        /**
         * XML File not valid against XSD
         */
        NOT_XSD_VALID,
        /**
         * File is not a XML
         */
        NOT_XML_FILE,
        /**
         * File not found
         */
        NO_FILE,
        /**
         * more than one manifest in SIP
         */
        MORE_THAN_ONE_MANIFEST,
        /**
         * More than one folder dans SIP
         */
        MORE_THAN_ONE_FOLDER_CONTENT,
        /**
         * If seda version is not supported by Vitam
         */
        UNSUPPORTED_SEDA_VERSION,
    }

    /**
     * check if there is manifest.xml file in the SIP
     */
    private InputStream checkExistenceManifest() throws IOException, ProcessingException {
        InputStream manifest;
        try {
            manifest = handlerIO.getInputStreamFromWorkspace(
                IngestWorkflowConstants.SEDA_FOLDER + "/" + IngestWorkflowConstants.SEDA_FILE
            );
        } catch (ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException e) {
            LOGGER.debug("Manifest not found");
            throw new ProcessingException("Manifest not found", e);
        }
        return manifest;
    }

    /**
     * check if there are many folder content in the SIP
     *
     * @throws ProcessingException
     */
    private boolean checkFolderContentNumber() throws ProcessingException {
        List<URI> list = handlerIO.getUriList(handlerIO.getContainerName(), IngestWorkflowConstants.SEDA_FOLDER);
        return list
            .stream()
            .filter(uri -> uri.toString().contains(URL_ENCODED_SEPARATOR))
            .map(content -> content.toString().split(URL_ENCODED_SEPARATOR)[0])
            .allMatch(x -> x.equalsIgnoreCase("content"));
    }

    /**
     * check if there are many file manifest.xml another in the SIP root
     *
     * @throws ProcessingException
     */
    private boolean checkMultiManifest() throws ProcessingException {
        List<URI> listURI = handlerIO.getUriList(handlerIO.getContainerName(), IngestWorkflowConstants.SEDA_FOLDER);

        return listURI.stream().filter(uri -> !uri.toString().contains(URL_ENCODED_SEPARATOR)).count() > 1;
    }

    /**
     * @return ExtractUriResponse - Object ExtractUriResponse contains listURI, listMessages and value boolean(error).
     * @throws ProcessingException - throw when error in execution.
     */
    public ExtractUriResponse getAllDigitalObjectUriFromManifest() throws ProcessingException {
        return parsingUriSEDAWithWorkspaceClient();
    }

    /**
     * Parsing file Manifest
     *
     * @return ExtractUriResponse - Object ExtractUriResponse contains listURI, listMessages and value boolean(error).
     */
    private ExtractUriResponse parsingUriSEDAWithWorkspaceClient() throws ProcessingException {
        InputStream xmlFile = null;
        LOGGER.debug(SedaUtils.MSG_PARSING_BDO);

        final ExtractUriResponse extractUriResponse = new ExtractUriResponse();

        // create URI list String for add elements uri from inputstream Seda
        final Set<URI> uriSet = new HashSet<>();
        // create String Messages list

        extractUriResponse.setUriSetManifest(uriSet);
        extractUriResponse.setErrorNumber(0);

        // Create the XML input factory
        final XMLInputFactory xmlInputFactory = XMLInputFactoryUtils.newInstance();
        // Create the XML output factory
        final XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();

        xmlOutputFactory.setProperty(SedaConstants.STAX_PROPERTY_PREFIX_OUTPUT_SIDE, Boolean.TRUE);

        final QName binaryDataObject = new QName(
            sedaIngestParams.getNamespaceURI(),
            SedaConstants.TAG_BINARY_DATA_OBJECT
        );
        XMLEventReader eventReader = null;
        try {
            try {
                xmlFile = handlerIO.getInputStreamFromWorkspace(
                    IngestWorkflowConstants.SEDA_FOLDER + "/" + IngestWorkflowConstants.SEDA_FILE
                );
            } catch (
                ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException | IOException e1
            ) {
                LOGGER.error("Workspace error: Can not get file", e1);
                throw new ProcessingException(e1);
            }

            // Create event reader
            eventReader = xmlInputFactory.createXMLEventReader(xmlFile);

            while (true) {
                final XMLEvent event = eventReader.nextEvent();
                // reach the start of an BinaryDataObject
                if (event.isStartElement()) {
                    final StartElement element = event.asStartElement();

                    if (element.getName().equals(binaryDataObject)) {
                        getUri(extractUriResponse, eventReader);
                    }
                }
                if (event.isEndDocument()) {
                    LOGGER.debug("data : " + event);
                    break;
                }
            }
            LOGGER.debug("End of extracting  Uri from manifest");
        } catch (XMLStreamException | UnsupportedEncodingException e) {
            LOGGER.error(e);
            throw new ProcessingException(e);
        } catch (URISyntaxException e) {
            LOGGER.error(e);
            throw new VitamKoRuntimeException(e);
        } finally {
            extractUriResponse.setErrorDuplicateUri(!extractUriResponse.getOutcomeMessages().isEmpty());
            try {
                if (eventReader != null) {
                    eventReader.close();
                }
            } catch (final XMLStreamException e) {
                SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            }
            StreamUtils.closeSilently(xmlFile);
        }
        return extractUriResponse;
    }

    private void getUri(ExtractUriResponse extractUriResponse, XMLEventReader evenReader)
        throws XMLStreamException, URISyntaxException, UnsupportedEncodingException {
        while (evenReader.hasNext()) {
            XMLEvent event = evenReader.nextEvent();

            if (event.isStartElement()) {
                final StartElement startElement = event.asStartElement();

                // If we have an Tag Uri element equal Uri into SEDA
                if (SedaConstants.TAG_URI.equals(startElement.getName().getLocalPart())) {
                    event = evenReader.nextEvent();
                    final String uri = event.asCharacters().getData();
                    // Check element is duplicate
                    checkDuplicatedUri(extractUriResponse, uri);
                    extractUriResponse.getUriSetManifest().add(new URI(URLEncoder.encode(uri, StandardCharsets.UTF_8)));
                    break;
                }
            }
        }
    }

    private void checkDuplicatedUri(ExtractUriResponse extractUriResponse, String uriString) throws URISyntaxException {
        if (extractUriResponse.getUriSetManifest().contains(new URI(uriString))) {
            extractUriResponse.setErrorNumber(extractUriResponse.getErrorNumber() + 1);
        }
    }

    /**
     * check if the version list of the manifest.xml in workspace is valid
     *
     * @param params worker parameter
     * @return map containing unsupported version
     * @throws ProcessingException throws when error occurs
     */
    public Map<String, Map<String, String>> checkSupportedDataObjectVersion(WorkerParameters params)
        throws ProcessingException {
        ParametersChecker.checkNullOrEmptyParameters(params);
        return isSedaVersionValid();
    }

    private Map<String, Map<String, String>> isSedaVersionValid() throws ProcessingException {
        InputStream xmlFile = null;
        Map<String, Map<String, String>> versionMap;
        XMLEventReader reader = null;
        try {
            try {
                xmlFile = handlerIO.getInputStreamFromWorkspace(
                    IngestWorkflowConstants.SEDA_FOLDER + "/" + IngestWorkflowConstants.SEDA_FILE
                );
            } catch (
                ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException | IOException e
            ) {
                LOGGER.error(MANIFEST_NOT_FOUND);
                throw new ProcessingException(e);
            }

            final XMLInputFactory xmlInputFactory = XMLInputFactoryUtils.newInstance();
            reader = xmlInputFactory.createXMLEventReader(xmlFile);
            versionMap = compareVersionList(reader);
        } catch (final XMLStreamException e) {
            LOGGER.error(CANNOT_READ_SEDA);
            throw new ProcessingException(e);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (final XMLStreamException e) {
                SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            }
            StreamUtils.closeSilently(xmlFile);
        }

        return versionMap;
    }

    /**
     * @param evenReader of seda
     * @return Seda Info object
     * @throws ProcessingException if cannot get BinaryObject info
     */
    public SedaUtilInfo getDataObjectInfo(XMLEventReader evenReader) throws ProcessingException {
        final SedaUtilInfo sedaUtilInfo = new SedaUtilInfo();
        DataObjectInfo dataObjectInfo = new DataObjectInfo();
        while (evenReader.hasNext()) {
            XMLEvent event;
            try {
                event = evenReader.nextEvent();

                if (event.isStartElement()) {
                    StartElement startElement = event.asStartElement();

                    if (SedaConstants.TAG_BINARY_DATA_OBJECT.equals(startElement.getName().getLocalPart())) {
                        final String id = startElement.getAttributes().next().getValue();
                        dataObjectInfo.setId(id);
                        dataObjectInfo.setType(SedaConstants.TAG_BINARY_DATA_OBJECT);
                        evenReader.nextEvent();

                        while (evenReader.hasNext()) {
                            event = evenReader.nextEvent();
                            if (event.isStartElement()) {
                                startElement = event.asStartElement();

                                final String tag = startElement.getName().getLocalPart();

                                switch (tag) {
                                    case SedaConstants.TAG_URI:
                                        final String uri = evenReader.getElementText();
                                        if (StringUtils.isBlank(uri)) {
                                            LOGGER.debug("Erreur Emply URI");
                                            break;
                                        }
                                        dataObjectInfo.setUri(uri);
                                        break;
                                    case SedaConstants.TAG_DO_VERSION:
                                        final String version = evenReader.getElementText();
                                        dataObjectInfo.setVersion(version);
                                        break;
                                    case SedaConstants.TAG_DIGEST:
                                        dataObjectInfo.setAlgo(
                                            DigestType.fromValue(
                                                StringUtils.trimToEmpty(startElement.getAttributes().next().getValue())
                                            )
                                        );
                                        final String messageDigest = StringUtils.trimToEmpty(
                                            evenReader.getElementText()
                                        );
                                        dataObjectInfo.setMessageDigest(messageDigest);
                                        break;
                                    case SedaConstants.TAG_SIZE:
                                        final long size = Long.parseLong(evenReader.getElementText());
                                        dataObjectInfo.setSize(size);
                                        break;
                                }
                            }

                            if (
                                event.isEndElement() &&
                                SedaConstants.TAG_BINARY_DATA_OBJECT.equals(
                                    event.asEndElement().getName().getLocalPart()
                                )
                            ) {
                                sedaUtilInfo.setDataObjectMap(dataObjectInfo);
                                dataObjectInfo = new DataObjectInfo();
                                break;
                            }
                        }
                    }
                    if (SedaConstants.TAG_PHYSICAL_DATA_OBJECT.equals(startElement.getName().getLocalPart())) {
                        final String id = startElement.getAttributes().next().getValue();
                        dataObjectInfo.setId(id);
                        dataObjectInfo.setType(SedaConstants.TAG_PHYSICAL_DATA_OBJECT);
                        evenReader.nextEvent();

                        while (evenReader.hasNext()) {
                            event = evenReader.nextEvent();
                            if (event.isStartElement()) {
                                startElement = event.asStartElement();

                                final String tag = startElement.getName().getLocalPart();
                                if (SedaConstants.TAG_DO_VERSION.equals(tag)) {
                                    dataObjectInfo.setVersion(evenReader.getElementText());
                                } else if (SedaConstants.TAG_PHYSICAL_ID.equals(tag)) {
                                    dataObjectInfo.setPhysicalId(evenReader.getElementText());
                                }
                            }

                            if (
                                event.isEndElement() &&
                                SedaConstants.TAG_PHYSICAL_DATA_OBJECT.equals(
                                    event.asEndElement().getName().getLocalPart()
                                )
                            ) {
                                sedaUtilInfo.setDataObjectMap(dataObjectInfo);
                                dataObjectInfo = new DataObjectInfo();
                                break;
                            }
                        }
                    }
                }
            } catch (final XMLStreamException e) {
                LOGGER.error("Can not get DataObject info");
                throw new ProcessingException(e);
            } catch (DigestTypeException d) {
                throw new SedaUtilsException(d);
            }
        }
        return sedaUtilInfo;
    }

    /**
     * @param evenReader XMLEventReader for the file manifest.xml
     * @return List of version for file manifest.xml
     * @throws ProcessingException when error in execution
     */

    public Map<String, List<DataObjectInfo>> manifestVersionList(XMLEventReader evenReader) throws ProcessingException {
        final Map<String, List<DataObjectInfo>> versionListByType = new HashMap<>();
        final SedaUtilInfo sedaUtilInfo = getDataObjectInfo(evenReader);
        final Map<String, DataObjectInfo> dataObjectMap = sedaUtilInfo.getDataObjectMap();

        // init
        List<DataObjectInfo> physicalObjectsVersion = new ArrayList<>();
        List<DataObjectInfo> binaryObjectsVersion = new ArrayList<>();

        for (final String mapKey : dataObjectMap.keySet()) {
            if (SedaConstants.TAG_PHYSICAL_DATA_OBJECT.equals(dataObjectMap.get(mapKey).getType())) {
                physicalObjectsVersion.add(dataObjectMap.get(mapKey));
            } else {
                binaryObjectsVersion.add(dataObjectMap.get(mapKey));
            }
        }
        versionListByType.put(SedaConstants.TAG_BINARY_DATA_OBJECT, binaryObjectsVersion);
        versionListByType.put(SedaConstants.TAG_PHYSICAL_DATA_OBJECT, physicalObjectsVersion);
        return versionListByType;
    }

    /**
     * compare if the version list of manifest.xml is included in or equal to the version list of version.conf
     *
     * @param eventReader xml event reader
     * @return map containing the error code and the unsupported version
     * @throws ProcessingException when error in execution
     */
    public Map<String, Map<String, String>> compareVersionList(XMLEventReader eventReader) throws ProcessingException {
        SedaVersion sedaVersion;
        try {
            sedaVersion = SedaConfiguration.getSupportedVerion();
        } catch (IOException e) {
            LOGGER.error(e);
            throw new ProcessingException(e);
        }
        final Map<String, List<DataObjectInfo>> manifestVersionListByType = manifestVersionList(eventReader);
        final Map<String, String> invalidVersionMap = new HashMap<>();
        final Map<String, String> validVersionMap = new HashMap<>();
        final Map<String, Map<String, String>> versionMap = new HashMap<>();

        for (Map.Entry<String, List<DataObjectInfo>> manifestVersionEntry : manifestVersionListByType.entrySet()) {
            List<String> fileVersions = sedaVersion.getVersionForType(manifestVersionEntry.getKey());
            for (final DataObjectInfo doi : manifestVersionEntry.getValue()) {
                if (doi.getVersion() != null) {
                    validVersionMap.put(doi.getId(), "");
                    final String[] versionParts = doi.getVersion().split("_");
                    String errorCode = manifestVersionEntry.getKey();
                    if (versionParts.length > 2 || !fileVersions.contains(versionParts[USAGE_POSITION])) {
                        List<String> otherFileVersions = sedaVersion.getVersionForOtherType(
                            manifestVersionEntry.getKey()
                        );
                        if (otherFileVersions.contains(versionParts[USAGE_POSITION])) {
                            errorCode += CONTAINS_OTHER_TYPE;
                        } else {
                            errorCode += INCORRECT_VERSION_FORMAT;
                        }
                        invalidVersionMap.put(doi.getId() + "_" + errorCode, doi.getVersion());
                        validVersionMap.remove(doi.getId());
                        continue;
                    }
                    if (versionParts.length == 2) {
                        // lets check the version (it should be an even int)
                        try {
                            int currentVersion = Integer.parseInt(versionParts[VERSION_POSITION]);
                            if (currentVersion < 0) {
                                invalidVersionMap.put(
                                    doi.getId() + "_" + errorCode + "_" + INCORRECT_VERSION_FORMAT,
                                    doi.getVersion()
                                );
                                validVersionMap.remove(doi.getId());
                            }
                        } catch (NumberFormatException e) {
                            LOGGER.warn("Wrong version ", e);
                            invalidVersionMap.put(
                                doi.getId() + "_" + errorCode + "_" + INCORRECT_VERSION_FORMAT,
                                doi.getVersion()
                            );
                            validVersionMap.remove(doi.getId());
                        }
                    }
                    if (SedaConstants.TAG_BINARY_DATA_OBJECT.equals(doi.getType())) {
                        if (Strings.isNullOrEmpty(doi.getUri())) {
                            invalidVersionMap.put(
                                doi.getId() + "_" + errorCode + "_" + INCORRECT_URI,
                                SedaConstants.TAG_URI
                            );
                            validVersionMap.remove(doi.getId());
                        }
                    } else if (Strings.isNullOrEmpty(doi.getPhysicalId())) {
                        invalidVersionMap.put(
                            doi.getId() + "_" + errorCode + "_" + INCORRECT_PHYSICAL_ID,
                            SedaConstants.TAG_PHYSICAL_ID
                        );
                        validVersionMap.remove(doi.getId());
                    }
                }
            }
        }
        versionMap.put(INVALID_DATAOBJECT_VERSION, invalidVersionMap);
        versionMap.put(VALID_DATAOBJECT_VERSION, validVersionMap);
        return versionMap;
    }

    /**
     * Parse SEDA file manifest.xml to retrieve all its binary data objects informations as a SedaUtilInfo.
     *
     * @return SedaUtilInfo
     * @throws ProcessingException throws when error occurs
     */
    private SedaUtilInfo getSedaUtilInfo() throws ProcessingException {
        SedaUtilInfo sedaUtilInfo;
        XMLEventReader reader = null;
        try (
            InputStream xmlFile = handlerIO.getInputStreamFromWorkspace(
                IngestWorkflowConstants.SEDA_FOLDER + "/" + IngestWorkflowConstants.SEDA_FILE
            )
        ) {
            final XMLInputFactory xmlInputFactory = XMLInputFactoryUtils.newInstance();
            reader = xmlInputFactory.createXMLEventReader(xmlFile);
            sedaUtilInfo = getDataObjectInfo(reader);
            return sedaUtilInfo;
        } catch (final XMLStreamException e) {
            LOGGER.error(CANNOT_READ_SEDA);
            throw new ProcessingException(e);
        } catch (
            ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException | IOException e
        ) {
            LOGGER.error(MANIFEST_NOT_FOUND);
            throw new ProcessingException(e);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (final XMLStreamException e) {
                // nothing to throw
                LOGGER.debug("Can not close XML reader SEDA", e);
            }
        }
    }

    /**
     * Compute the total size of objects listed in the manifest.xml file
     *
     * @param params worker parameters
     * @return the computed size of all BinaryObjects
     * @throws ProcessingException when error in getting binary object info
     */
    public long computeTotalSizeOfObjectsInManifest(WorkerParameters params) throws ProcessingException {
        ParametersChecker.checkNullOrEmptyParameters(params);
        final String containerId = params.getContainerName();
        ParametersChecker.checkParameter("Container id is a mandatory parameter", containerId);
        return computeBinaryObjectsSizeFromManifest();
    }

    /**
     * Compute the total size of objects listed in the manifest.xml file
     *
     * @return the computed size of all BinaryObjects
     * @throws ProcessingException when error in getting binary object info
     */

    private long computeBinaryObjectsSizeFromManifest() throws ProcessingException {
        long size = 0;
        final SedaUtilInfo sedaUtilInfo = getSedaUtilInfo();
        final Map<String, DataObjectInfo> dataObjectMap = sedaUtilInfo.getDataObjectMap();
        for (final String mapKey : dataObjectMap.keySet()) {
            final Long binaryObjectSize = dataObjectMap.get(mapKey).getSize();
            if (binaryObjectSize != null && binaryObjectSize > 0) {
                size += binaryObjectSize;
            }
        }
        return size;
    }

    public SedaIngestParams getSedaIngestParams() {
        return this.sedaIngestParams;
    }

    public void setSedaIngestParams(SedaIngestParams sedaIngestParams) {
        this.sedaIngestParams = sedaIngestParams;
    }
}
