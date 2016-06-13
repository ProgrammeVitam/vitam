/**
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
 */
package fr.gouv.vitam.processing.common.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.xml.sax.SAXException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.odysseus.staxon.json.JsonXMLConfig;
import de.odysseus.staxon.json.JsonXMLConfigBuilder;
import de.odysseus.staxon.json.JsonXMLOutputFactory;
import fr.gouv.vitam.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.builder.request.construct.Insert;
import fr.gouv.vitam.client.MetaDataClient;
import fr.gouv.vitam.client.MetaDataClientFactory;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.WorkParams;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

/**
 * SedaUtils to read or split element from SEDA
 *
 */
public class SedaUtils {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(SedaUtils.class);

    private static String TMP_FOLDER = "/vitam/data/";
    private static final String NAMESPACE_URI = "fr:gouv:culture:archivesdefrance:seda:v2.0";
    private static final String SEDA_FILE = "manifest.xml";
    private static final String SEDA_VALIDATION_FILE = "seda-2.0-main.xsd";
    private static final String XML_EXTENSION = ".xml";
    private static final String SEDA_FOLDER = "SIP";
    private static final String BINARY_DATA_OBJECT = "BinaryDataObject";
    private static final String BINARY_DATA_FOLDER = "DataObjects";
    private static final String DATA_OBJECT_GROUPID = "DataObjectGroupId";
    private static final String ARCHIVE_UNIT = "ArchiveUnit";
    private static final String ARCHIVE_UNIT_FOLDER = "Units";
    private static final String DATA_OBJECT_REFERENCEID = "DataObjectReferenceId";
    private static final String TAG_URI = "Uri";
    private static final String MSG_PARSING_BDO = "Parsing Binary Data Object";
    private static final String STAX_PROPERTY_PREFIX_OUTPUT_SIDE = "javax.xml.stream.isRepairingNamespaces";

    private final Map<String, String> binaryDataObjectIdToGuid;
    private final Map<String, String> objectGroupIdToGuid;
    private final Map<String, String> unitIdToGuid;

    private final Map<String, String> binaryDataObjectIdToGroupId;
    private final Map<String, String> unitIdToGroupId;

    private final String tmpDirectory;
    private final WorkspaceClientFactory workspaceClientFactory;
    private final MetaDataClientFactory metaDataClientFactory;

    // Messages for duplicate Uri from SEDA
    private static final String MSG_DUPLICATE_URI_MANIFEST = "Présence d'un URI en doublon dans le bordereau: ";

    private static final HashMap<String, String> objectToGroupMapping = new HashMap<String, String>() {
        private static final long serialVersionUID = 1257583431403626689L;
        {
            put(BINARY_DATA_OBJECT, DATA_OBJECT_GROUPID);
            put(ARCHIVE_UNIT, DATA_OBJECT_REFERENCEID);
        }
    };

    private static final HashMap<String, String> objectToFolderMapping = new HashMap<String, String>() {
        private static final long serialVersionUID = -8520409868569154910L;

        {
            put(BINARY_DATA_OBJECT, BINARY_DATA_FOLDER);
            put(ARCHIVE_UNIT, ARCHIVE_UNIT_FOLDER);
        }
    };

    protected SedaUtils(WorkspaceClientFactory workspaceFactory, MetaDataClientFactory metaDataFactory) {
        ParametersChecker.checkParameter("workspaceFactory is a mandatory parameter", workspaceFactory);
        binaryDataObjectIdToGuid = new HashMap<String, String>();
        objectGroupIdToGuid = new HashMap<String, String>();
        unitIdToGuid = new HashMap<String, String>();
        binaryDataObjectIdToGroupId = new HashMap<String, String>();
        unitIdToGroupId = new HashMap<String, String>();
        tmpDirectory = TMP_FOLDER + GUIDFactory.newGUID().toString();
        workspaceClientFactory = workspaceFactory;
        metaDataClientFactory = metaDataFactory;
    }

    protected SedaUtils() {
        this(new WorkspaceClientFactory(), new MetaDataClientFactory());
    }

    /**
     * Get temporary folder
     *
     * @return folder name as String
     */
    public static String getTmpFolder() {
        return TMP_FOLDER;
    }


    /**
     * Set the temporary folder
     *
     * @param tmp as String
     */
    public static void setTmpFolder(String tmp) {
        TMP_FOLDER = tmp;
    }

    /**
     * @return Map<String, String> reflects BinaryDataObject and File(GUID)
     */
    public Map<String, String> getBinaryDataObjectIdToGuid() {
        return binaryDataObjectIdToGuid;
    }

    /**
     * @return Map<String, String> reflects ObjectGroup and File(GUID)
     */
    public Map<String, String> getObjectGroupIdToGuid() {
        return objectGroupIdToGuid;
    }

    /**
     * @return Map<String, String> reflects Unit and File(GUID)
     */
    public Map<String, String> getUnitIdToGuid() {
        return unitIdToGuid;
    }

    /**
     * @return Map<String, String> reflects BinaryDataObject and ObjectGroup
     */
    public Map<String, String> getBinaryDataObjectIdToGroupId() {
        return binaryDataObjectIdToGroupId;
    }

    /**
     * @return Map<String, String> reflects Unit and ObjectGroup
     */
    public Map<String, String> getUnitIdToGroupId() {
        return unitIdToGroupId;
    }


    /**
     * Split Element from InputStream and write it to workspace
     *
     * @param workParams parameters of workspace server
     * @param workspaceClientFactory workspace client factory
     * @throws ProcessingException throw when can't read or extract element from SEDA
     */
    public void extractSEDA(WorkParams params) throws ProcessingException {
        ParametersChecker.checkParameter("WorkParams is a mandatory parameter", params);
        final String containerId = params.getContainerName();
        final WorkspaceClient client = workspaceClientFactory.create(params.getServerConfiguration().getUrlWorkspace());
        extractSEDAWithWorkspaceClient(client, containerId);
    }

    private void extractSEDAWithWorkspaceClient(WorkspaceClient client, String containerId) throws ProcessingException {
        ParametersChecker.checkParameter("WorkspaceClient is a mandatory parameter", client);
        ParametersChecker.checkParameter("ContainerId is a mandatory parameter", containerId);

        /**
         * Retrieves SEDA
         **/
        InputStream xmlFile = null;
        try {
            xmlFile = client.getObject(containerId, SEDA_FOLDER + "/" + SEDA_FILE);
        } catch (ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException e) {
            LOGGER.error("Manifest.xml Not Found");
            throw new ProcessingException(e);
        }

        final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        XMLEventReader reader = null;
        final QName dataObjectName = new QName(NAMESPACE_URI, BINARY_DATA_OBJECT);
        final QName unitName = new QName(NAMESPACE_URI, ARCHIVE_UNIT);

        try {
            reader = xmlInputFactory.createXMLEventReader(xmlFile);
            while (true) {
                final XMLEvent event = reader.nextEvent();
                if (event.isStartElement()) {
                    final StartElement element = event.asStartElement();
                    if (element.getName().equals(unitName) || element.getName().equals(dataObjectName)) {
                        writeToWorkspace(client, containerId, reader, element,
                            objectToGroupMapping.get(element.getName().getLocalPart()));
                    }
                }
                if (event.isEndDocument()) {
                    break;
                }
            }
            reader.close();
        } catch (final XMLStreamException e) {
            LOGGER.error("Can not read SEDA");
            throw new ProcessingException(e);
        }
    }

    private void writeToWorkspace(WorkspaceClient client, String containerId, XMLEventReader reader,
        StartElement startElement,
        String groupName) throws ProcessingException {

        final XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
        final XMLEventFactory eventFactory = XMLEventFactory.newInstance();
        final String elementID = ((Attribute) startElement.getAttributes().next()).getValue();
        final String elementGuid = GUIDFactory.newGUID().toString();
        final QName name = startElement.getName();
        int stack = 1;
        final File tmpFile = new File(tmpDirectory + elementGuid);
        LOGGER.info("Get tmpFile");

        XMLEventWriter writer;
        try {
            tmpFile.createNewFile();
            writer = xmlOutputFactory.createXMLEventWriter(new FileWriter(tmpFile));
            getAdaptMap(name.getLocalPart()).put(elementID, elementGuid);
            // Create new startElement for object with new guid
            writer.add(eventFactory.createStartElement("", NAMESPACE_URI, startElement.getName().getLocalPart()));
            writer.add(eventFactory.createAttribute("id", elementGuid));
            // TODO allow recursive
            while (true) {
                final XMLEvent event = reader.nextEvent();
                if (event.isStartElement() && event.asStartElement().getName().equals(name)) {
                    stack++;
                }
                if (event.isEndElement()) {
                    final EndElement end = event.asEndElement();
                    if (end.getName().equals(name)) {
                        stack--;
                        if (stack == 0) {
                            writer.add(event);
                            break;
                        }
                    }
                }
                if (event.isStartElement() && event.asStartElement().getName().getLocalPart() == groupName) {
                    final String groupGuid = GUIDFactory.newGUID().toString();
                    final String groupId = reader.getElementText();
                    getAdaptMap(groupName).put(elementID, groupId);
                    // Create new startElement for group with new guid
                    writer.add(eventFactory.createStartElement("", NAMESPACE_URI, groupName));
                    writer.add(eventFactory.createCharacters(groupGuid));
                    writer.add(eventFactory.createEndElement("", NAMESPACE_URI, groupName));
                } else {
                    writer.add(event);
                }

            }
            writer.close();
            if (tmpFile != null) {
                client.putObject(containerId,
                    objectToFolderMapping.get(name.getLocalPart()) + "/" + elementGuid + XML_EXTENSION,
                    new FileInputStream(tmpFile));
                tmpFile.delete();
            }
        } catch (final XMLStreamException e) {
            LOGGER.error("Can not extract Object from SEDA XMLStreamException");
            throw new ProcessingException(e);
        } catch (final IOException e) {
            LOGGER.error("Can not extract Object from SEDA IOException " + elementGuid);
            throw new ProcessingException(e);
        } catch (final Exception e) {
            LOGGER.error(e.getMessage());
            throw new ProcessingException(e);
        }
    }

    private Map<String, String> getAdaptMap(String element) {
        Map<String, String> result = null;
        switch (element) {
            case BINARY_DATA_OBJECT:
                result = binaryDataObjectIdToGuid;
                break;
            case ARCHIVE_UNIT:
                result = unitIdToGuid;
                break;
            case DATA_OBJECT_GROUPID:
                result = binaryDataObjectIdToGroupId;
                break;
            case DATA_OBJECT_REFERENCEID:
                result = unitIdToGroupId;
                break;
            default:
                break;
        }

        return result;
    }

    /**
     * The method is used to validate SEDA by XSD
     *
     * @param params
     * @return boolean true/false
     * @throws IOException
     */
    public boolean checkSedaValidation(WorkParams params) throws IOException {
        ParametersChecker.checkParameter("WorkParams is a mandatory parameter", params);
        final String containerId = params.getContainerName();
        final WorkspaceClient client = workspaceClientFactory.create(params.getServerConfiguration().getUrlWorkspace());
        try {
            final InputStream input = checkExistenceManifest(client, containerId);
            return new ValidationXsdUtils().checkWithXSD(input, SEDA_VALIDATION_FILE);

        } catch (ProcessingException | XMLStreamException | SAXException e) {
            LOGGER.error("Manifest.xml is not valid ", e);
            return false;
        }
    }

    /**
     * The function is used for checking the existence of the file manifest.xml in workspace
     *
     * @param client
     * @param guid
     * @return true (if manifest.xml exists), true (if not)
     * @throws IOException
     * @throws ProcessingException
     */
    private InputStream checkExistenceManifest(WorkspaceClient client, String guid)
        throws IOException, ProcessingException {
        ParametersChecker.checkParameter("WorkspaceClient is a mandatory parameter", client);
        ParametersChecker.checkParameter("guid is a mandatory parameter", guid);
        InputStream manifest = null;
        try {
            manifest = client.getObject(guid, SEDA_FOLDER + "/" + SEDA_FILE);
        } catch (ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException e) {
            LOGGER.error("Manifest not found");
            throw new ProcessingException("Manifest not found", e);
        }
        return manifest;
    }


    /**
     * @param params work parameters
     * @throws ProcessingException when error in execution
     */
    public void indexArchiveUnit(WorkParams params) throws ProcessingException {
        ParametersChecker.checkParameter("Work parameters is a mandatory parameter", params);

        final String containerId = params.getContainerName();
        final String objectName = params.getObjectName();
        ParametersChecker.checkParameter("Container id is a mandatory parameter", containerId);
        ParametersChecker.checkParameter("ObjectName id is a mandatory parameter", objectName);

        final WorkspaceClient workspaceClient =
            workspaceClientFactory.create(params.getServerConfiguration().getUrlWorkspace());
        final MetaDataClient metadataClient =
            metaDataClientFactory.create(params.getServerConfiguration().getUrlMetada());
        InputStream input;
        try {
            input = workspaceClient.getObject(containerId, ARCHIVE_UNIT_FOLDER + "/" + objectName);

            if (input != null) {
                final JsonNode json = convertArchiveUnitToJson(input, containerId, objectName).get(ARCHIVE_UNIT);
                final String insertRequest = new Insert().addData((ObjectNode) json).getFinalInsert().toString();
                metadataClient.insert(insertRequest);
            } else {
                LOGGER.error("Archive unit not found");
                throw new ProcessingException("Archive unit not found");
            }

        } catch (final InvalidParseOperationException e) {
            LOGGER.debug("Archive unit json invalid");
            throw new ProcessingException(e);
        } catch (final MetaDataExecutionException e) {
            LOGGER.debug("Internal Server Error");
            throw new ProcessingException(e);
        } catch (ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException e) {
            LOGGER.debug("Workspace Server Error");
            throw new ProcessingException(e);
        }

    }


    private JsonNode convertArchiveUnitToJson(InputStream input, String containerId, String objectName)
        throws InvalidParseOperationException, ProcessingException {
        ParametersChecker.checkParameter("Input stream is a mandatory parameter", input);
        ParametersChecker.checkParameter("Container id is a mandatory parameter", containerId);
        ParametersChecker.checkParameter("ObjectName id is a mandatory parameter", objectName);
        final File tmpFile = new File(TMP_FOLDER + objectName);
        FileWriter tmpFileWriter = null;
        final XMLEventFactory eventFactory = XMLEventFactory.newInstance();
        final JsonXMLConfig config = new JsonXMLConfigBuilder()
            .build();
        JsonNode data = null;
        try {
            tmpFileWriter = new FileWriter(tmpFile);
            final XMLEventReader reader = XMLInputFactory.newInstance().createXMLEventReader(input);

            final XMLEventWriter writer = new JsonXMLOutputFactory(config).createXMLEventWriter(tmpFileWriter);
            boolean contentWritable = true;
            while (true) {
                final XMLEvent event = reader.nextEvent();
                boolean eventWritable = true;
                if (event.isStartElement()) {
                    final StartElement startElement = event.asStartElement();
                    final Iterator<?> it = startElement.getAttributes();

                    if (it.hasNext()) {
                        writer.add(eventFactory.createStartElement("", "", startElement.getName().getLocalPart()));

                        if (startElement.getName().getLocalPart() == ARCHIVE_UNIT) {
                            writer.add(eventFactory.createStartElement("", "", "#id"));
                            writer.add(eventFactory.createCharacters(((Attribute) it.next()).getValue()));
                            writer.add(eventFactory.createEndElement("", "", "#id"));
                        }
                        eventWritable = false;
                    }

                    if (startElement.getName().getLocalPart() == "Content") {
                        eventWritable = false;
                    }

                    if (startElement.getName().getLocalPart() == "Management") {
                        writer.add(eventFactory.createStartElement("", "", "_mgt"));
                        eventWritable = false;
                    }
                }

                if (event.isEndElement()) {

                    if (event.asEndElement().getName().getLocalPart() == ARCHIVE_UNIT) {
                        eventWritable = false;
                    }

                    if (event.asEndElement().getName().getLocalPart() == "Content") {
                        eventWritable = false;
                        contentWritable = false;
                    }

                    if (event.asEndElement().getName().getLocalPart() == "Management") {
                        writer.add(eventFactory.createEndElement("", "", "_mgt"));
                        eventWritable = false;
                    }
                }


                if (event.isEndDocument()) {
                    writer.add(event);
                    break;
                }
                if (eventWritable && contentWritable) {
                    writer.add(event);
                }
            }
            reader.close();
            writer.close();
            input.close();
            tmpFileWriter.close();
            data = JsonHandler.getFromFile(tmpFile);
            tmpFile.delete();
        } catch (final XMLStreamException e) {
            LOGGER.debug("Can not read input stream");
            throw new ProcessingException(e);
        } catch (final IOException e) {
            LOGGER.debug("Closing stream error");
            throw new ProcessingException(e);
        }
        return data;
    }

    /**
     *
     * @param params - parameters of workspace server
     * @return ExtractUriResponse - Object ExtractUriResponse contains listURI, listMessages and value boolean(error).
     * @throws ProcessingException - throw when can't read or extract element from SEDA.
     * @throws XMLStreamException -This Exception class is used to report well format SEDA.
     */
    public ExtractUriResponse getAllDigitalObjectUriFromManifest(WorkParams params)
        throws ProcessingException, XMLStreamException {
        final String guid = params.getContainerName();
        final WorkspaceClient client = workspaceClientFactory.create(params.getServerConfiguration().getUrlWorkspace());
        final ExtractUriResponse extractUriResponse = parsingUriSEDAWithWorkspaceClient(client, guid);
        return extractUriResponse;
    }


    /**
     * Parsing file Manifest
     *
     * @param client - the InputStream to read from
     * @param guid - Identification file seda.
     * @return ExtractUriResponse - Object ExtractUriResponse contains listURI, listMessages and value boolean(error).
     * @throws XMLStreamException-This Exception class is used to report well format SEDA.
     */
    private ExtractUriResponse parsingUriSEDAWithWorkspaceClient(WorkspaceClient client, String guid)
        throws ProcessingException, XMLStreamException {

        /**
         * Extract SEDA
         **/
        InputStream xmlFile = null;

        try {
            xmlFile = client.getObject(guid, SEDA_FOLDER + "/" + SEDA_FILE);
        } catch (ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException e1) {
            LOGGER.error("Workspace error: Can not get file");
            throw new ProcessingException(e1.getMessage());
        }
        LOGGER.info(SedaUtils.MSG_PARSING_BDO);

        final ExtractUriResponse extractUriResponse = new ExtractUriResponse();

        // create URI list String for add elements uri from inputstream Seda
        final List<URI> listUri = new ArrayList<URI>();
        // create String Messages list
        final List<String> listMessages = new ArrayList<>();

        extractUriResponse.setUriListManifest(listUri);
        extractUriResponse.setMessages(listMessages);

        // Create the XML input factory
        final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        // Create the XML output factory
        final XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();

        xmlOutputFactory.setProperty(SedaUtils.STAX_PROPERTY_PREFIX_OUTPUT_SIDE, Boolean.TRUE);

        // Create event reader
        final XMLEventReader evenReader = xmlInputFactory.createXMLEventReader(xmlFile);

        final QName binaryDataObject = new QName(SedaUtils.NAMESPACE_URI, SedaUtils.BINARY_DATA_OBJECT);

        try {

            while (true) {
                final XMLEvent event = evenReader.nextEvent();
                // reach the start of an BinaryDataObject
                if (event.isStartElement()) {
                    final StartElement element = event.asStartElement();

                    if (element.getName().equals(binaryDataObject)) {
                        getUri(extractUriResponse, evenReader);
                    }
                }
                if (event.isEndDocument()) {
                    LOGGER.info("data : " + event);
                    break;
                }
            }
            LOGGER.info("End of extracting  Uri from manifest");
            evenReader.close();

        } catch (XMLStreamException | URISyntaxException e) {
            LOGGER.error(e.getMessage());
            throw new ProcessingException(e);
        } finally {
            extractUriResponse.setErrorDuplicateUri(!extractUriResponse.getMessages().isEmpty());
        }
        return extractUriResponse;
    }

    /**
     * Using Stax to split element Uri of Binary Data Object.
     *
     * @param extractUriResponse - list Uri of Binary Data Object and list Message and value error.
     * @param evenReader -
     * @throws XMLStreamException - This Exception class is used to report well-format SEDA.
     * @throws URISyntaxException - if some information could not be parsed while creating a URI.
     */
    private void getUri(ExtractUriResponse extractUriResponse, XMLEventReader evenReader)
        throws XMLStreamException, URISyntaxException {

        while (evenReader.hasNext()) {
            XMLEvent event = evenReader.nextEvent();

            if (event.isStartElement()) {
                final StartElement startElement = event.asStartElement();

                // If we have an Tag Uri element equal Uri into SEDA
                if (startElement.getName().getLocalPart() == SedaUtils.TAG_URI) {
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

    /**
     * Check element duplicate from UriListManifest.
     *
     * @param extractUriResponse - List contains listURI , listMessages and value error
     * @param uriString - Value of uri in SEDA.
     * @throws URISyntaxException - if some information could not be parsed while creating a URI
     */
    private void checkDuplicatedUri(ExtractUriResponse extractUriResponse, String uriString) throws URISyntaxException {

        if (extractUriResponse.getUriListManifest().contains(new URI(uriString))) {
            extractUriResponse.getMessages().add(SedaUtils.MSG_DUPLICATE_URI_MANIFEST + uriString);
        }
    }

}
