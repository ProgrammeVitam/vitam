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
package fr.gouv.vitam.worker.common.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.io.IOUtils;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

/**
 * SedaUtils to read or split element from SEDA
 *
 */
// TODO P0 : remove parameterChecker when it's a handler method
// the check is done with ParameterHelper and the WorkerParameters classes on the worker (WorkerImpl before the
// handler execute)
// If you absolutely need to check values in handler's methods, also use the ParameterCheker.
public class SedaUtils {

    static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(SedaUtils.class);
    public static final String NAMESPACE_URI = "fr:gouv:culture:archivesdefrance:seda:v2.0";
    private static final String SEDA_VALIDATION_FILE = "seda-vitam-2.0-main.xsd";

    private static final String MSG_PARSING_BDO = "Parsing Binary Data Object";

    private static final String CANNOT_READ_SEDA = "Can not read SEDA";
    private static final String MANIFEST_NOT_FOUND = "Manifest.xml Not Found";
    private static final int VERSION_POSITION = 0;

    /**
     * nbAUExisting: number of the AU already existing
     */
    public static final String NB_AU_EXISTING = "nbAUExisting";

    private final Map<String, String> binaryDataObjectIdToGuid;
    private final Map<String, String> objectGroupIdToGuid;
    // TODO P1 : utiliser une structure avec le GUID et le témoin de passage du DataObjectGroupID .
    // objectGroup referenced before declaration
    private final Map<String, String> unitIdToGuid;

    private final Map<String, String> binaryDataObjectIdToObjectGroupId;
    private final Map<String, List<String>> objectGroupIdToBinaryDataObjectId;
    private final Map<String, String> unitIdToGroupId;
    private final HandlerIO handlerIO;

    protected SedaUtils(HandlerIO handlerIO) {
        binaryDataObjectIdToGuid = new HashMap<>();
        objectGroupIdToGuid = new HashMap<>();
        objectGroupIdToBinaryDataObjectId = new HashMap<>();
        unitIdToGuid = new HashMap<>();
        binaryDataObjectIdToObjectGroupId = new HashMap<>();
        unitIdToGroupId = new HashMap<>();
        this.handlerIO = handlerIO;
    }

    /**
     * @return A map reflects BinaryDataObject and File(GUID)
     */
    public Map<String, String> getBinaryDataObjectIdToGuid() {
        return binaryDataObjectIdToGuid;
    }

    /**
     * @return A map reflects relation ObjectGroupId and BinaryDataObjectId
     */
    public Map<String, List<String>> getObjectGroupIdToBinaryDataObjectId() {
        return objectGroupIdToBinaryDataObjectId;
    }

    /**
     * @return A map reflects ObjectGroup and File(GUID)
     */
    public Map<String, String> getObjectGroupIdToGuid() {
        return objectGroupIdToGuid;
    }

    /**
     * @return A map reflects Unit and File(GUID)
     */
    public Map<String, String> getUnitIdToGuid() {
        return unitIdToGuid;
    }

    /**
     * @return A map reflects BinaryDataObject and ObjectGroup
     */
    public Map<String, String> getBinaryDataObjectIdToGroupId() {
        return binaryDataObjectIdToObjectGroupId;
    }

    /**
     * @return A map reflects Unit and ObjectGroup
     */
    public Map<String, String> getUnitIdToGroupId() {
        return unitIdToGroupId;
    }

    /**
     * get Message Identifier from seda
     *
     * @param params parameters of workspace server
     * @return message id
     * @throws ProcessingException throw when can't read or extract message id from SEDA
     */
    public Map<String, Object> getMandatoryValues(WorkerParameters params) throws ProcessingException {
        ParameterHelper.checkNullOrEmptyParameters(params);
        Map<String, Object> madatoryValueMap = new HashMap<>();
        XMLEventReader reader = null;
        InputStream xmlFile = null;
        try {
            try {
                xmlFile = handlerIO.getInputStreamFromWorkspace(
                    IngestWorkflowConstants.SEDA_FOLDER + "/" + IngestWorkflowConstants.SEDA_FILE);
            } catch (ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException |
                IOException e) {
                LOGGER.error(MANIFEST_NOT_FOUND);
                throw new ProcessingException(e);
            }

            final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
            final QName messageObjectName = new QName(NAMESPACE_URI, SedaConstants.TAG_MESSAGE_IDENTIFIER);
            final QName originatingAgencyName = new QName(NAMESPACE_URI, SedaConstants.TAG_ORIGINATINGAGENCYIDENTIFIER);
            final QName contractName = new QName(NAMESPACE_URI, SedaConstants.TAG_ARCHIVAL_AGREEMENT);

            reader = xmlInputFactory.createXMLEventReader(xmlFile);
            while (true) {
                final XMLEvent event = reader.nextEvent();
                if (event.isStartElement()) {
                    final StartElement element = event.asStartElement();
                    if (element.getName().equals(contractName)) {
                        madatoryValueMap.put(SedaConstants.TAG_ARCHIVAL_AGREEMENT, reader.getElementText());
                    }
                    if (element.getName().equals(messageObjectName)) {
                        madatoryValueMap.put(SedaConstants.TAG_MESSAGE_IDENTIFIER, reader.getElementText());
                    }
                    if (element.getName().equals(originatingAgencyName)) {
                        madatoryValueMap.put(SedaConstants.TAG_ORIGINATINGAGENCYIDENTIFIER, reader.getElementText());
                        break;
                    }
                }
                if (event.isEndDocument()) {
                    break;
                }
            }
        } catch (final XMLStreamException e) {
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
            StreamUtils.closeSilently(xmlFile);
        }

        return madatoryValueMap;
    }

    /**
     * The method is used to validate SEDA by XSD
     *
     * @param params worker parameter
     * @return a status representing the validation of the file
     */
    public CheckSedaValidationStatus checkSedaValidation(WorkerParameters params) {
        ParameterHelper.checkNullOrEmptyParameters(params);
        final InputStream input;
        try {
            input = checkExistenceManifest();
            if (checkMultiManifest()) {
                return CheckSedaValidationStatus.MORE_THAN_ONE_MANIFEST;
            }
            if (!checkFolderContentNumber()) {
                return CheckSedaValidationStatus.MORE_THAN_ONE_FOLDER_CONTENT;
            }
            new ValidationXsdUtils().checkWithXSD(input, SEDA_VALIDATION_FILE);
            return CheckSedaValidationStatus.VALID;
        } catch (ProcessingException | IOException e) {
            LOGGER.error("Manifest.xml not found", e);
            return CheckSedaValidationStatus.NO_FILE;
        } catch (final XMLStreamException e) {
            LOGGER.error("Manifest.xml is not a correct xml file", e);
            return CheckSedaValidationStatus.NOT_XML_FILE;
        } catch (final SAXException e) {
            // if the cause is null, that means the file is an xml, but it does not validate the XSD
            if (e.getCause() == null) {
                LOGGER.error("Manifest.xml is not valid with the XSD", e);
                return CheckSedaValidationStatus.NOT_XSD_VALID;
            }
            LOGGER.error("Manifest.xml is not a correct xml file", e);
            return CheckSedaValidationStatus.NOT_XML_FILE;
        }
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
        MORE_THAN_ONE_FOLDER_CONTENT;
    }

    /**
     * check if there is manifest.xml file in the SIP
     */
    private InputStream checkExistenceManifest()
        throws IOException, ProcessingException {
        InputStream manifest = null;
        try {
            manifest = handlerIO.getInputStreamFromWorkspace(
                IngestWorkflowConstants.SEDA_FOLDER + "/" + IngestWorkflowConstants.SEDA_FILE);
        } catch (ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException e) {
            LOGGER.error("Manifest not found");
            throw new ProcessingException("Manifest not found", e);
        }
        return manifest;
    }

    /**
     * check if there are many folder content in the SIP
     */
    private boolean checkFolderContentNumber() throws ProcessingException {
        List<URI> list = handlerIO.getUriList(handlerIO.getContainerName(), IngestWorkflowConstants.SEDA_FOLDER);
        String contentName = null;
        for (int i = 0; i < list.size(); i++) {
            String s = list.get(i).toString();
            if (s.contains("/")) {
                String directory = s.split("/")[0];
                if (directory.equalsIgnoreCase("content")) {
                    if (contentName == null) {
                        contentName = directory;
                    } else {
                        if (!contentName.equals(directory)) {
                            return false;
                        }
                    }
                } else {
                    return false;
                }

            }
        }

        return true;
    }

    /**
     * check if there are many file manifest.xml another in the SIP root
     */
    private boolean checkMultiManifest() throws ProcessingException {
        List<URI> listURI = handlerIO.getUriList(handlerIO.getContainerName(), IngestWorkflowConstants.SEDA_FOLDER);
        int countManifest = 0;
        for (int i = 0; i < listURI.size(); i++) {
            if (!listURI.get(i).toString().contains("/")) {
                countManifest++;
                if (countManifest > 1) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     *
     * @param params - parameters of workspace server
     * @return ExtractUriResponse - Object ExtractUriResponse contains listURI, listMessages and value boolean(error).
     * @throws ProcessingException - throw when error in execution.
     */
    public ExtractUriResponse getAllDigitalObjectUriFromManifest(WorkerParameters params)
        throws ProcessingException {
        return parsingUriSEDAWithWorkspaceClient();
    }

    /**
     * Parsing file Manifest
     *
     * @return ExtractUriResponse - Object ExtractUriResponse contains listURI, listMessages and value boolean(error).
     * @throws XMLStreamException-This Exception class is used to report well format SEDA.
     */
    private ExtractUriResponse parsingUriSEDAWithWorkspaceClient()
        throws ProcessingException {
        InputStream xmlFile = null;
        LOGGER.debug(SedaUtils.MSG_PARSING_BDO);

        final ExtractUriResponse extractUriResponse = new ExtractUriResponse();

        // create URI list String for add elements uri from inputstream Seda
        final List<URI> listUri = new ArrayList<>();
        // create String Messages list
        final List<String> listMessages = new ArrayList<>();

        extractUriResponse.setUriListManifest(listUri);
        extractUriResponse.setErrorNumber(listMessages.size());

        // Create the XML input factory
        final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        // Create the XML output factory
        final XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();

        xmlOutputFactory.setProperty(SedaConstants.STAX_PROPERTY_PREFIX_OUTPUT_SIDE, Boolean.TRUE);

        final QName binaryDataObject = new QName(SedaUtils.NAMESPACE_URI, SedaConstants.TAG_BINARY_DATA_OBJECT);
        XMLEventReader eventReader = null;
        try {
            try {
                xmlFile = handlerIO.getInputStreamFromWorkspace(
                    IngestWorkflowConstants.SEDA_FOLDER + "/" + IngestWorkflowConstants.SEDA_FILE);
            } catch (ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException |
                IOException e1) {
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

        } catch (XMLStreamException | URISyntaxException e) {
            LOGGER.error(e);
            throw new ProcessingException(e);
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
        throws XMLStreamException, URISyntaxException {

        while (evenReader.hasNext()) {
            XMLEvent event = evenReader.nextEvent();

            if (event.isStartElement()) {
                final StartElement startElement = event.asStartElement();

                // If we have an Tag Uri element equal Uri into SEDA
                if (startElement.getName().getLocalPart() == SedaConstants.TAG_URI) {
                    event = evenReader.nextEvent();
                    final String uri = event.asCharacters().getData();
                    // Check element is duplicate
                    checkDuplicatedUri(extractUriResponse, uri);
                    extractUriResponse.getUriListManifest().add(new URI(uri));
                    break;
                }
            }
        }
    }

    private void checkDuplicatedUri(ExtractUriResponse extractUriResponse, String uriString) throws URISyntaxException {

        if (extractUriResponse.getUriListManifest().contains(new URI(uriString))) {
            extractUriResponse.setErrorNumber(extractUriResponse.getErrorNumber() + 1);
        }
    }

    /**
     * check if the version list of the manifest.xml in workspace is valid
     *
     * @param params worker parameter
     * @return list of unsupported version
     * @throws ProcessingException throws when error occurs
     */
    public List<String> checkSupportedDataObjectVersion(WorkerParameters params)
        throws ProcessingException {
        ParameterHelper.checkNullOrEmptyParameters(params);
        return isSedaVersionValid();
    }

    private List<String> isSedaVersionValid() throws ProcessingException {
        InputStream xmlFile = null;
        List<String> invalidVersionList;
        XMLEventReader reader = null;
        try {
            try {
                xmlFile = handlerIO.getInputStreamFromWorkspace(
                    IngestWorkflowConstants.SEDA_FOLDER + "/" + IngestWorkflowConstants.SEDA_FILE);
            } catch (ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException |
                IOException e) {
                LOGGER.error(MANIFEST_NOT_FOUND);
                throw new ProcessingException(e);
            }

            final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
            reader = xmlInputFactory.createXMLEventReader(xmlFile);
            invalidVersionList = compareVersionList(reader);
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

        return invalidVersionList;
    }

    /**
     * @param evenReader of seda
     * @return Seda Info object
     * @throws ProcessingException if cannot get BinaryObject info
     */
    public SedaUtilInfo getDataObjectInfo(XMLEventReader evenReader)
        throws ProcessingException {
        final SedaUtilInfo sedaUtilInfo = new SedaUtilInfo();
        DataObjectInfo dataObjectInfo = new DataObjectInfo();
        while (evenReader.hasNext()) {
            XMLEvent event;
            try {
                event = evenReader.nextEvent();

                if (event.isStartElement()) {
                    StartElement startElement = event.asStartElement();

                    if (SedaConstants.TAG_BINARY_DATA_OBJECT.equals(startElement.getName().getLocalPart())) {
                        final String id = ((Attribute) startElement.getAttributes().next()).getValue();
                        dataObjectInfo.setId(id);
                        evenReader.nextEvent();

                        while (evenReader.hasNext()) {
                            event = evenReader.nextEvent();
                            if (event.isStartElement()) {
                                startElement = event.asStartElement();

                                final String tag = startElement.getName().getLocalPart();
                                switch (tag) {
                                    case SedaConstants.TAG_URI:
                                        final String uri = evenReader.getElementText();
                                        dataObjectInfo.setUri(uri);
                                        break;
                                    case SedaConstants.TAG_DO_VERSION:
                                        final String version = evenReader.getElementText();
                                        dataObjectInfo.setVersion(version);
                                        break;
                                    case SedaConstants.TAG_DIGEST:
                                        dataObjectInfo.setAlgo(DigestType
                                            .fromValue(((Attribute) startElement.getAttributes().next()).getValue()));
                                        final String messageDigest = evenReader.getElementText();
                                        dataObjectInfo.setMessageDigest(messageDigest);
                                        break;
                                    case SedaConstants.TAG_SIZE:
                                        final long size = Long.parseLong(evenReader.getElementText());
                                        dataObjectInfo.setSize(size);
                                        break;
                                    default:
                                        break;
                                }
                            }

                            if (event.isEndElement() &&
                                SedaConstants.TAG_BINARY_DATA_OBJECT
                                    .equals(event.asEndElement().getName().getLocalPart())) {
                                sedaUtilInfo.setDataObjectMap(dataObjectInfo);
                                dataObjectInfo = new DataObjectInfo();
                                break;
                            }

                        }
                    }
                    if (SedaConstants.TAG_PHYSICAL_DATA_OBJECT.equals(startElement.getName().getLocalPart())) {
                        final String id = ((Attribute) startElement.getAttributes().next()).getValue();
                        dataObjectInfo.setId(id);
                        evenReader.nextEvent();

                        while (evenReader.hasNext()) {
                            event = evenReader.nextEvent();
                            if (event.isStartElement()) {
                                startElement = event.asStartElement();

                                final String tag = startElement.getName().getLocalPart();
                                if (SedaConstants.TAG_DO_VERSION.equals(tag)) {
                                    dataObjectInfo.setVersion(evenReader.getElementText());
                                }
                            }

                            if (event.isEndElement() &&
                                SedaConstants.TAG_PHYSICAL_DATA_OBJECT
                                    .equals(event.asEndElement().getName().getLocalPart())) {
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
            }
        }
        return sedaUtilInfo;
    }

    /**
     * @param evenReader XMLEventReader for the file manifest.xml
     * @return List of version for file manifest.xml
     * @throws ProcessingException when error in execution
     */

    public Map<String, Set<String>> manifestVersionList(XMLEventReader evenReader)
        throws ProcessingException {
        final Map<String, Set<String>> versionListByType = new HashMap<>();
        final SedaUtilInfo sedaUtilInfo = getDataObjectInfo(evenReader);
        final Map<String, DataObjectInfo> dataObjectMap = sedaUtilInfo.getDataObjectMap();

        // init
        Set<String> physicalObjectsVersion = new HashSet<>();
        Set<String> binaryObjectsVersion = new HashSet<>();


        for (final String mapKey : dataObjectMap.keySet()) {
            // add physicalId ?
            if (dataObjectMap.get(mapKey).getUri() == null) {
                physicalObjectsVersion.add(dataObjectMap.get(mapKey).getVersion());
            } else {
                binaryObjectsVersion.add(dataObjectMap.get(mapKey).getVersion());
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
     * @return list of unsupported version
     * @throws ProcessingException when error in execution
     */
    public List<String> compareVersionList(XMLEventReader eventReader)
        throws ProcessingException {

        File file;

        try {
            file = PropertiesUtils.findFile("version.conf");
        } catch (final FileNotFoundException e) {
            LOGGER.error("Can not get config file ");
            throw new ProcessingException(e);
        }

        SedaVersion sedaVersion;

        try {
            sedaVersion = PropertiesUtils.readYaml(file, SedaVersion.class);
        } catch (final IOException e) {
            LOGGER.error("Can not read config file");
            LOGGER.error(e);
            throw new ProcessingException(e);
        }

        final Map<String, Set<String>> manifestVersionListByType = manifestVersionList(eventReader);
        final List<String> invalidVersionList = new ArrayList<>();

        for (Map.Entry<String, Set<String>> manifestVersionEntry : manifestVersionListByType.entrySet()) {
            for (final String version : manifestVersionEntry.getValue()) {
                List<String> fileVersions = sedaVersion.getVersionForType(manifestVersionEntry.getKey());
                if (version != null) {
                    final String versionParts[] = version.split("_");
                    if (versionParts.length > 2 || !fileVersions.contains(versionParts[VERSION_POSITION])) {
                        invalidVersionList.add(version);
                    }
                }
            }
        }
        return invalidVersionList;
    }

    /**
     * Parse SEDA file manifest.xml to retrieve all its binary data objects informations as a SedaUtilInfo.
     *
     * @return SedaUtilInfo
     * @throws ProcessingException throws when error occurs
     */
    private SedaUtilInfo getSedaUtilInfo()
        throws ProcessingException {
        InputStream xmlFile = null;

        SedaUtilInfo sedaUtilInfo;
        XMLEventReader reader = null;
        try {
            try {
                xmlFile = handlerIO.getInputStreamFromWorkspace(
                    IngestWorkflowConstants.SEDA_FOLDER + "/" + IngestWorkflowConstants.SEDA_FILE);
            } catch (ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException |
                IOException e) {
                LOGGER.error(MANIFEST_NOT_FOUND);
                IOUtils.closeQuietly(xmlFile);
                throw new ProcessingException(e);
            }

            final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
            reader = xmlInputFactory.createXMLEventReader(xmlFile);
            sedaUtilInfo = getDataObjectInfo(reader);
            return sedaUtilInfo;
        } catch (final XMLStreamException e) {
            LOGGER.error(CANNOT_READ_SEDA);
            throw new ProcessingException(e);
        } finally {
            IOUtils.closeQuietly(xmlFile);
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
    public long computeTotalSizeOfObjectsInManifest(WorkerParameters params)
        throws ProcessingException {
        ParameterHelper.checkNullOrEmptyParameters(params);
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

    private long computeBinaryObjectsSizeFromManifest()
        throws ProcessingException {
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

    /**
     * Get the size of the manifest file
     *
     * @param params worker parameters
     * @return the size of the manifest
     * @throws ProcessingException if json seda data is null or seda does not contain size attribute
     */
    public long getManifestSize(WorkerParameters params)
        throws ProcessingException {
        ParameterHelper.checkNullOrEmptyParameters(params);
        final String containerId = params.getContainerName();
        ParametersChecker.checkParameter("Container id is a mandatory parameter", containerId);
        try (final WorkspaceClient client = WorkspaceClientFactory.getInstance().getClient()) {
            final JsonNode jsonSeda = getObjectInformation(client, containerId,
                IngestWorkflowConstants.SEDA_FOLDER + "/" + IngestWorkflowConstants.SEDA_FILE);
            if (jsonSeda == null || jsonSeda.get("size") == null) {
                LOGGER.error("Error while getting object size : " + IngestWorkflowConstants.SEDA_FILE);
                throw new ProcessingException("Json response cannot be null and must contains a 'size' attribute");
            }
            return jsonSeda.get("size").asLong();
        }
    }


    /**
     * Retrieve information about an object.
     *
     * @param workspaceClient workspace connector
     * @param containerId container id
     * @param pathToObject path to the object
     * @return JsonNode containing information about the object
     * @throws ProcessingException throws when error occurs
     */
    private JsonNode getObjectInformation(WorkspaceClient workspaceClient, String containerId,
        String pathToObject)
        throws ProcessingException {
        try {
            return workspaceClient.getObjectInformation(containerId, pathToObject)
                .toJsonNode().get("$results").get(0);
        } catch (ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException e) {
            LOGGER.error(IngestWorkflowConstants.SEDA_FILE + " Not Found");
            throw new ProcessingException(e);
        }
    }

}
