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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.io.CharStreams;

import de.odysseus.staxon.json.JsonXMLConfig;
import de.odysseus.staxon.json.JsonXMLConfigBuilder;
import de.odysseus.staxon.json.JsonXMLOutputFactory;
import fr.gouv.vitam.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.builder.request.construct.Insert;
import fr.gouv.vitam.client.MetaDataClient;
import fr.gouv.vitam.client.MetaDataClientFactory;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.WorkParams;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
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
    private static String File_CONF = "version.conf";
    private static final String NAMESPACE_URI = "fr:gouv:culture:archivesdefrance:seda:v2.0";
    private static final String SEDA_FILE = "manifest.xml";
    private static final String SEDA_VALIDATION_FILE = "seda-2.0-main.xsd";
    private static final String XML_EXTENSION = ".xml";
    private static final String JSON_EXTENSION = ".json";
    private static final String SEDA_FOLDER = "SIP";
    private static final String CONTENT_FOLDER = "content";
    private static final String BINARY_DATA_OBJECT = "BinaryDataObject";
    private static final String MESSAGE_IDENTIFIER = "MessageIdentifier";
    private static final String OBJECT_GROUP = "ObjectGroup";
    private static final String DATA_OBJECT_GROUPID = "DataObjectGroupId";
    private static final String ARCHIVE_UNIT = "ArchiveUnit";
    private static final String ARCHIVE_UNIT_FOLDER = "Units";
    private static final String BINARY_MASTER = "BinaryMaster";
    private static final String FILE_INFO = "FileInfo";
    private static final String METADATA = "Metadata";
    private static final String DATA_OBJECT_REFERENCEID = "DataObjectReferenceId";
    private static final String DATA_OBJECT_GROUP_REFERENCEID = "DataObjectGroupReferenceId";
    private static final String TAG_URI = "Uri";
    private static final String TAG_SIZE = "Size";
    private static final String TAG_DIGEST = "MessageDigest";
    private static final String TAG_VERSION = "DataObjectVersion";
    private static final String MSG_PARSING_BDO = "Parsing Binary Data Object";
    private static final String STAX_PROPERTY_PREFIX_OUTPUT_SIDE = "javax.xml.stream.isRepairingNamespaces";
    private static final String TAG_CONTENT = "Content";
    private static final String TAG_MANAGEMENT = "Management";
    private static final String TAG_OG = "_og";

    private final Map<String, String> binaryDataObjectIdToGuid;
    private final Map<String, String> objectGroupIdToGuid;
    private final Map<String, String> unitIdToGuid;

    private final Map<String, String> binaryDataObjectIdToObjectGroupId;
    private final Map<String, List<String>> objectGroupIdToBinaryDataObjectId;
    private final Map<String, String> unitIdToGroupId;
    private final Map<String, List<String>> objectGroupIdToUnitId;

    private final WorkspaceClientFactory workspaceClientFactory;
    private final MetaDataClientFactory metaDataClientFactory;

    // Messages for duplicate Uri from SEDA
    private static final String MSG_DUPLICATE_URI_MANIFEST = "Présence d'un URI en doublon dans le bordereau: ";

    protected SedaUtils(WorkspaceClientFactory workspaceFactory, MetaDataClientFactory metaDataFactory) {
        ParametersChecker.checkParameter("workspaceFactory is a mandatory parameter", workspaceFactory);
        binaryDataObjectIdToGuid = new HashMap<String, String>();
        objectGroupIdToGuid = new HashMap<String, String>();
        objectGroupIdToBinaryDataObjectId = new HashMap<String, List<String>>();
        unitIdToGuid = new HashMap<String, String>();
        binaryDataObjectIdToObjectGroupId = new HashMap<String, String>();
        objectGroupIdToUnitId = new HashMap<String, List<String>>();
        unitIdToGroupId = new HashMap<String, String>();
        workspaceClientFactory = workspaceFactory;
        metaDataClientFactory = metaDataFactory;
    }

    protected SedaUtils() {
        this(new WorkspaceClientFactory(), new MetaDataClientFactory());
    }

    /**
     * @return Map<String, String> reflects BinaryDataObject and File(GUID)
     */
    public Map<String, String> getBinaryDataObjectIdToGuid() {
        return binaryDataObjectIdToGuid;
    }

    /**
     * @return Map<String, String> reflects relation ObjectGroupId and BinaryDataObjectId
     */
    public Map<String, List<String>> getObjectGroupIdToBinaryDataObjectId() {
        return objectGroupIdToBinaryDataObjectId;
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
        return binaryDataObjectIdToObjectGroupId;
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

    /**
     * get Message Identifier from seda
     * @param WorkParams parameters of workspace server
     * @return message id
     * @throws ProcessingException throw when can't read or extract message id from SEDA
     */
    public String getMessageIdentifier(WorkParams params) throws ProcessingException {
        ParametersChecker.checkParameter("WorkParams is a mandatory parameter", params);
        final String containerId = params.getContainerName();
        String messageId = "";
        final WorkspaceClient client = workspaceClientFactory.create(params.getServerConfiguration().getUrlWorkspace());
        InputStream xmlFile = null;
        try {
            xmlFile = client.getObject(containerId, SEDA_FOLDER + "/" + SEDA_FILE);
        } catch (ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException e) {
            LOGGER.error("Manifest.xml Not Found");
            throw new ProcessingException(e);
        }

        final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        XMLEventReader reader = null;
        final QName messageObjectName = new QName(NAMESPACE_URI, MESSAGE_IDENTIFIER);

        try {
            reader = xmlInputFactory.createXMLEventReader(xmlFile);
            while (true) {
                final XMLEvent event = reader.nextEvent();
                if (event.isStartElement()) {
                    final StartElement element = event.asStartElement();
                    if (element.getName().equals(messageObjectName)) {
                        messageId = reader.getElementText();
                        break;
                    }
                }
                if (event.isEndDocument()) {
                    break;
                }
            }
            reader.close();
        } catch (final XMLStreamException e) {
            LOGGER.error("Can not read SEDA", e);
            throw new ProcessingException(e);
        } 

        return messageId;
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
                    if (element.getName().equals(unitName)) {
                        writeArchiveUnitToWorkspace(client, containerId, reader, element);
                    } else if (element.getName().equals(dataObjectName)) {
                        writeBinaryDataObjectInLocal(reader, element);
                    }
                }
                if (event.isEndDocument()) {
                    break;
                }
            }
            reader.close();
            checkArchiveUnitIdReference();
            saveObjectGroupsToWorkspace(client, containerId);
        } catch (final XMLStreamException e) {
            LOGGER.error("Can not read SEDA");
            throw new ProcessingException(e);
        }
    }

    private File extractArchiveUnitToLocalFile(String elementGuid, XMLEventReader reader,
        StartElement startElement) throws ProcessingException {

        final XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
        final XMLEventFactory eventFactory = XMLEventFactory.newInstance();
        final String elementID = ((Attribute) startElement.getAttributes().next()).getValue();
        final QName name = startElement.getName();
        int stack = 1;
        String groupGuid = "";
        final File tmpFile = PropertiesUtils.fileFromTmpFolder(GUIDFactory.newGUID().toString() + elementGuid);
        XMLEventWriter writer;
        try {
            tmpFile.createNewFile();
            writer = xmlOutputFactory.createXMLEventWriter(new FileWriter(tmpFile));
            unitIdToGuid.put(elementID, elementGuid);
            
            // add an empty objectgroup to each archive unit
            unitIdToGroupId.put(elementID, "");
            
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
                            // Create objectgroup reference id
                            writer.add(eventFactory.createStartElement("", "", TAG_OG));
                            writer.add(eventFactory.createCharacters(groupGuid));
                            writer.add(eventFactory.createEndElement("", "", TAG_OG));
                            
                            writer.add(event);
                            break;
                        }
                    }
                }
                if (event.isStartElement() && event.asStartElement().getName().getLocalPart() == DATA_OBJECT_GROUP_REFERENCEID) {
                    groupGuid = GUIDFactory.newGUID().toString();
                    final String groupId = reader.getElementText();
                    unitIdToGroupId.put(elementID, groupId);
                    if (objectGroupIdToUnitId.get(groupId) == null) {
                        ArrayList<String> archiveUnitList = new ArrayList<String>();
                        archiveUnitList.add(elementID);
                        objectGroupIdToUnitId.put(groupId, archiveUnitList);
                    } else {
                        List<String> archiveUnitList = objectGroupIdToUnitId.get(groupId);
                        archiveUnitList.add(elementID);
                        objectGroupIdToUnitId.put(groupId, archiveUnitList);
                    }
                    // Create new startElement for group with new guid
                    writer.add(eventFactory.createStartElement("", NAMESPACE_URI, DATA_OBJECT_GROUP_REFERENCEID));
                    writer.add(eventFactory.createCharacters(groupGuid));
                    writer.add(eventFactory.createEndElement("", NAMESPACE_URI, DATA_OBJECT_GROUP_REFERENCEID));
                } else {
                    writer.add(event);
                }

            }
            reader.close();
            writer.close();
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
        return tmpFile;
    }

    private void writeArchiveUnitToWorkspace(WorkspaceClient client, String containerId, XMLEventReader reader,
        StartElement startElement) throws ProcessingException {
        final String elementGuid = GUIDFactory.newGUID().toString();

        try {
            File tmpFile = extractArchiveUnitToLocalFile(elementGuid, reader, startElement);
            if (tmpFile != null) {
                client.putObject(containerId, ARCHIVE_UNIT_FOLDER + "/" + elementGuid + XML_EXTENSION,
                    new FileInputStream(tmpFile));
                tmpFile.delete();
            }
        } catch (final ProcessingException e) {
            LOGGER.error("Can not extract Object from SEDA XMLStreamException", e);
            throw e;
        } catch (final IOException e) {
            LOGGER.error("Can not extract Object from SEDA IOException " + elementGuid, e);
            throw new ProcessingException(e);
        } catch (ContentAddressableStorageServerException e) {
            LOGGER.error("Can not write to workspace ", e);
            throw new ProcessingException(e);
        }
    }

    private void checkArchiveUnitIdReference() throws ProcessingException {
        for (Entry<String, String> entry : unitIdToGroupId.entrySet()) {
            if (objectGroupIdToGuid.get(entry.getValue()) == null) {
                String groupId = binaryDataObjectIdToObjectGroupId.get(entry.getValue());
                if (groupId == null || groupId != "") {
                    throw new ProcessingException("Archive Unit reference Id is not correct");
                }
            }
        }
    }

    private void writeBinaryDataObjectInLocal(XMLEventReader reader,
        StartElement startElement) throws ProcessingException {
        final String elementGuid = GUIDFactory.newGUID().toString();
        final File tmpFile = PropertiesUtils.fileFromTmpFolder(elementGuid + ".json");
        final XMLEventFactory eventFactory = XMLEventFactory.newInstance();
        final JsonXMLConfig config = new JsonXMLConfigBuilder().build();
        try {
            FileWriter tmpFileWriter = new FileWriter(tmpFile);

            final XMLEventWriter writer = new JsonXMLOutputFactory(config).createXMLEventWriter(tmpFileWriter);

            final Iterator<?> it = startElement.getAttributes();
            String binaryOjectId = "";
            if (it.hasNext()) {
                binaryOjectId = ((Attribute) it.next()).getValue();
                binaryDataObjectIdToGuid.put(binaryOjectId, elementGuid);
                binaryDataObjectIdToObjectGroupId.put(binaryOjectId, "");
                writer.add(eventFactory.createStartDocument());
                writer.add(eventFactory.createStartElement("", "", startElement.getName().getLocalPart()));
                writer.add(eventFactory.createStartElement("", "", "_id"));
                writer.add(eventFactory.createCharacters(binaryOjectId));
                writer.add(eventFactory.createEndElement("", "", "_id"));
            }
            while (true) {
                boolean writable = true;
                final XMLEvent event = reader.nextEvent();
                if (event.isEndElement()) {
                    final EndElement end = event.asEndElement();
                    if (end.getName().getLocalPart() == BINARY_DATA_OBJECT) {                        
                        writer.add(event);
                        writer.add(eventFactory.createEndDocument());
                        break;
                    }
                }

                if (event.isStartElement()) {
                    String localPart = event.asStartElement().getName().getLocalPart();
                    if (localPart == DATA_OBJECT_GROUPID) {
                        final String groupGuid = GUIDFactory.newGUID().toString();
                        final String groupId = reader.getElementText();
                        binaryDataObjectIdToObjectGroupId.put(binaryOjectId, groupId);
                        objectGroupIdToGuid.put(groupId, groupGuid);

                        List<String> binaryOjectList = new ArrayList<String>();
                        binaryOjectList.add(binaryOjectId);
                        objectGroupIdToBinaryDataObjectId.put(groupId, binaryOjectList);

                        // Create new startElement for group with new guid
                        writer.add(eventFactory.createStartElement("", "", DATA_OBJECT_GROUPID));
                        writer.add(eventFactory.createCharacters(groupGuid));
                        writer.add(eventFactory.createEndElement("", "", DATA_OBJECT_GROUPID));
                    } else if (localPart == DATA_OBJECT_GROUP_REFERENCEID) {
                        final String groupId = reader.getElementText();
                        binaryDataObjectIdToObjectGroupId.put(binaryOjectId, groupId);
                        objectGroupIdToBinaryDataObjectId.get(groupId).add(binaryOjectId);

                        // Create new startElement for group with new guid
                        writer.add(eventFactory.createStartElement("", "", DATA_OBJECT_GROUPID));
                        writer.add(eventFactory.createCharacters(objectGroupIdToGuid.get(groupId)));
                        writer.add(eventFactory.createEndElement("", "", DATA_OBJECT_GROUPID));
                    } else if (localPart == "Uri") {
                        reader.getElementText();
                    } else {
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
        } catch (final XMLStreamException e) {
            LOGGER.debug("Can not read input stream");
            throw new ProcessingException(e);
        } catch (final IOException e) {
            LOGGER.debug("Closing stream error");
            throw new ProcessingException(e);
        }

    }

    private void completeBinaryObjectToObjectGroupMap() {
        for (String key: binaryDataObjectIdToObjectGroupId.keySet()) {
            if (binaryDataObjectIdToObjectGroupId.get(key) == "") {
                List<String> binaryOjectList = new ArrayList<String>();
                binaryOjectList.add(key);
                objectGroupIdToBinaryDataObjectId.put(GUIDFactory.newGUID().toString(), binaryOjectList);
            }
        }
    }

    private void saveObjectGroupsToWorkspace(WorkspaceClient client, String containerId) throws ProcessingException {

        completeBinaryObjectToObjectGroupMap();

        for (Entry<String, List<String>> entry : objectGroupIdToBinaryDataObjectId.entrySet()) {
            ObjectNode objectGroup = JsonHandler.createObjectNode();
            ObjectNode fileInfo = JsonHandler.createObjectNode();
            ArrayNode unitParent = JsonHandler.createArrayNode();
            String objectGroupType = "";
            String objectGroupGuid = objectGroupIdToGuid.get(entry.getKey());
            final File tmpFile = PropertiesUtils.fileFromTmpFolder(objectGroupGuid + JSON_EXTENSION);

            try {
                FileWriter tmpFileWriter = new FileWriter(tmpFile);
                Map<String, ArrayList<JsonNode>> categoryMap = new HashMap<String, ArrayList<JsonNode>>();
                objectGroup.put("_id", objectGroupGuid);
                objectGroup.put("_tenantId", 0);
                for (String id : entry.getValue()) {
                    File binaryObjectFile = PropertiesUtils.fileFromTmpFolder(binaryDataObjectIdToGuid.get(id) + JSON_EXTENSION);
                    JsonNode binaryNode = JsonHandler
                        .getFromFile(binaryObjectFile)
                        .get("BinaryDataObject");
                    String nodeCategory = binaryNode.get("DataObjectVersion").asText();
                    ArrayList<JsonNode> nodeCategoryArray = categoryMap.get(nodeCategory);
                    if (nodeCategoryArray == null) {
                        nodeCategoryArray = new ArrayList<JsonNode>();
                        nodeCategoryArray.add(binaryNode);
                    } else {
                        nodeCategoryArray.add(binaryNode);
                    }
                    categoryMap.put(nodeCategory, nodeCategoryArray);
                    if (BINARY_MASTER.equals(nodeCategory)) {
                        fileInfo = (ObjectNode) binaryNode.get(FILE_INFO);
                        objectGroupType = binaryNode.get(METADATA).fieldNames().next();
                    }
                    binaryObjectFile.delete();
                }
                
                for (String objectGroupId : objectGroupIdToUnitId.get(entry.getKey())) {
                    unitParent.add(unitIdToGuid.get(objectGroupId));
                }

                objectGroup.put("_type", objectGroupType);
                objectGroup.set("FileInfo", fileInfo);
                ObjectNode qualifiersNode =  getObjectGroupQualifiers(categoryMap);
                objectGroup.set("_qualifiers", qualifiersNode);
                objectGroup.set("_up", unitParent);
                tmpFileWriter.write(objectGroup.toString());
                tmpFileWriter.close();
                
                client.putObject(containerId, OBJECT_GROUP + "/" + objectGroupGuid + JSON_EXTENSION, new FileInputStream(tmpFile));
                tmpFile.delete(); 
            } catch (final InvalidParseOperationException e) {
                LOGGER.error("Can not parse ObjectGroup", e);
                throw new ProcessingException(e);
            } catch (final IOException e) {
                LOGGER.error("Can not write to tmp folder ", e);
                throw new ProcessingException(e);
            } catch (ContentAddressableStorageServerException e) {
                LOGGER.error("Workspace exception ", e);
                throw new ProcessingException(e);
            } 
        }

    }

    private ObjectNode getObjectGroupQualifiers(Map<String, ArrayList<JsonNode>> categoryMap) {
        ObjectNode qualifierObject = JsonHandler.createObjectNode();
        for (Entry<String, ArrayList<JsonNode>> entry : categoryMap.entrySet()) {
            ObjectNode binaryNode = JsonHandler.createObjectNode();
            binaryNode.put("nb", entry.getValue().size());
            ArrayNode arrayNode = JsonHandler.createArrayNode();
            for (JsonNode node : entry.getValue()) {
                arrayNode.add(node);
            }
            binaryNode.set("versions", arrayNode);
            qualifierObject.set(entry.getKey(), binaryNode);
        }
        return qualifierObject;
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
                metadataClient.insertUnit(insertRequest);
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
    
    /**
     * The function is used for retrieving ObjectGroup in workspace and use metadata client to index ObjectGroup
     * 
     * @param params work parameters
     * @throws ProcessingException when error in execution
     */
    public void indexObjectGroup(WorkParams params) throws ProcessingException {
        ParametersChecker.checkParameter("Work parameters is a mandatory parameter", params);

        final String containerId = params.getContainerName();
        final String objectName = params.getObjectName();
        ParametersChecker.checkParameter("Container id is a mandatory parameter", containerId);
        ParametersChecker.checkParameter("ObjectName id is a mandatory parameter", objectName);

        final WorkspaceClient workspaceClient =
            workspaceClientFactory.create(params.getServerConfiguration().getUrlWorkspace());
        final MetaDataClient metadataClient =
            metaDataClientFactory.create(params.getServerConfiguration().getUrlMetada());
        InputStream input = null;
        try {
            input = workspaceClient.getObject(containerId, OBJECT_GROUP + "/" + objectName);

            if (input != null) {
                String inputStreamString = CharStreams.toString(new InputStreamReader(input, "UTF-8"));
                final JsonNode json = JsonHandler.getFromString(inputStreamString);
                Insert insertRequest = new Insert().addData((ObjectNode) json);
                //metadataClient.insertObjectGroup(insertRequest.getFinalInsert().toString());
            } else {
                LOGGER.error("Object group not found");
                throw new ProcessingException("Object group not found");
            }
 

        } catch (final MetaDataExecutionException e) {
            LOGGER.debug("Metadata Server Error", e);
            throw new ProcessingException(e);
        } catch (InvalidParseOperationException | IOException e) {
            LOGGER.debug("Json wrong format", e);
            throw new ProcessingException(e);
        } catch (ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException e) {
            LOGGER.debug("Workspace Server Error", e);
            throw new ProcessingException(e);
        }

    }

    private JsonNode convertArchiveUnitToJson(InputStream input, String containerId, String objectName)
        throws InvalidParseOperationException, ProcessingException {
        ParametersChecker.checkParameter("Input stream is a mandatory parameter", input);
        ParametersChecker.checkParameter("Container id is a mandatory parameter", containerId);
        ParametersChecker.checkParameter("ObjectName id is a mandatory parameter", objectName);
        final File tmpFile = PropertiesUtils.fileFromTmpFolder(GUIDFactory.newGUID().toString());
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
                    String tag = startElement.getName().getLocalPart();
                    if (it.hasNext() && tag != TAG_CONTENT) {
                        writer.add(eventFactory.createStartElement("", "", tag));

                        if (tag == ARCHIVE_UNIT) {
                            writer.add(eventFactory.createStartElement("", "", "#id"));
                            writer.add(eventFactory.createCharacters(((Attribute) it.next()).getValue()));
                            writer.add(eventFactory.createEndElement("", "", "#id"));
                        }
                        eventWritable = false;
                    }

                    if (tag == TAG_CONTENT) {
                        eventWritable = false;
                    }
                    
                    if (tag == TAG_OG) {
                        contentWritable = true;
                    }

                    if (tag == TAG_MANAGEMENT) {
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
    
    
    /**
     * check if the version list of the manifest.xml in workspace is valid
     * 
     * @param params
     * @return list of unsupported version
     * @throws ProcessingException
     * @throws IOException
     * @throws URISyntaxException
     */
    public List<String> checkSupportedBinaryObjectVersion(WorkParams params) throws ProcessingException, IOException, URISyntaxException {
        ParametersChecker.checkParameter("WorkParams is a mandatory parameter", params);
        final String containerId = params.getContainerName();
        final WorkspaceClient client = workspaceClientFactory.create(params.getServerConfiguration().getUrlWorkspace());
        return isSedaVersionValid(client, containerId);
    }
    
    private List<String> isSedaVersionValid(WorkspaceClient client, 
        String containerId)throws ProcessingException, IOException, URISyntaxException {
        ParametersChecker.checkParameter("WorkspaceClient is a mandatory parameter", client);
        ParametersChecker.checkParameter("ContainerId is a mandatory parameter", containerId);

        InputStream xmlFile = null;
        List<String> invalidVersionList = new ArrayList<String>();
        try {
            xmlFile = client.getObject(containerId, SEDA_FOLDER + "/" + SEDA_FILE);
        } catch (ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException e) {
            LOGGER.error("Manifest.xml Not Found");
            throw new ProcessingException(e);
        }

        final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        XMLEventReader reader = null;

        try {
            reader = xmlInputFactory.createXMLEventReader(xmlFile);
            invalidVersionList = compareVersionList(reader, File_CONF);
            reader.close();
        } catch (final XMLStreamException e) {
            LOGGER.error("Can not read SEDA");
            throw new ProcessingException(e);
        }
        
        return invalidVersionList;
    }
    
    private SedaUtilInfo getBinaryObjectInfo(XMLEventReader evenReader)
            throws XMLStreamException, URISyntaxException{
        SedaUtilInfo sedaUtilInfo = new SedaUtilInfo();
        BinaryObjectInfo binaryObjectInfo = new BinaryObjectInfo();
        while (evenReader.hasNext()) {
            XMLEvent event = evenReader.nextEvent();

            if (event.isStartElement()) {
                StartElement startElement = event.asStartElement();
                
                if (startElement.getName().getLocalPart() == BINARY_DATA_OBJECT) {
                    event = evenReader.nextEvent();
                    Iterator<Attribute> attributes = startElement.getAttributes();
                    final String id = attributes.next().getValue();
                    binaryObjectInfo.setId(id);
                    
                    while (evenReader.hasNext()) {
                        event = evenReader.nextEvent();
                        if (event.isStartElement()) {
                            startElement = event.asStartElement();
                             
                            String tag = startElement.getName().getLocalPart();
                            if (tag == TAG_URI) {
                                final String uri = evenReader.getElementText();
                                binaryObjectInfo.setUri(new URI(uri));
                            }
                            
                            if (tag == TAG_VERSION) {
                                final String version = evenReader.getElementText();
                                binaryObjectInfo.setVersion(version);
                            }
                            
                            if (tag == TAG_DIGEST) {
                                final String messageDigest = evenReader.getElementText();
                                binaryObjectInfo.setMessageDigest(messageDigest);
                            }
                            
                            if (tag == TAG_SIZE) {
                                final int size = Integer.parseInt(evenReader.getElementText());
                                binaryObjectInfo.setSize(size);
                            }
                        }
                        
                        if (event.isEndElement() && event.asEndElement().getName().getLocalPart() == BINARY_DATA_OBJECT) {
                            sedaUtilInfo.setBinaryObjectMap(binaryObjectInfo);
                            binaryObjectInfo = new BinaryObjectInfo();
                            break;
                        }
                        
                    }
                }
            }
        }
        return sedaUtilInfo;
    }
    
    /**
     * @param evenReader XMLEventReader for the file manifest.xml
     * @return List<String> list of version for file manifest.xml
     * @throws XMLStreamException
     * @throws URISyntaxException
     */
    public List<String> manifestVersionList(XMLEventReader evenReader) 
            throws XMLStreamException, URISyntaxException{
        List<String> versionList = new ArrayList<String>();
        SedaUtilInfo sedaUtilInfo = getBinaryObjectInfo(evenReader);
        Map<String, BinaryObjectInfo> binaryObjectMap = sedaUtilInfo.getBinaryObjectMap();
        
        for (String mapKey : binaryObjectMap.keySet()) {
            if (!versionList.contains(binaryObjectMap.get(mapKey).getVersion())){
                versionList.add(binaryObjectMap.get(mapKey).getVersion());
            }
        }
        
        return versionList;
    }
    
    /**
     * compare if the version list of manifest.xml is included in or equal to the version list of version.conf
     * 
     * @param evenReader
     * @return list of unsupported version
     * @throws IOException
     * @throws XMLStreamException
     * @throws URISyntaxException
     */
    public List<String> compareVersionList(XMLEventReader evenReader, String fileConf) 
            throws IOException, XMLStreamException, URISyntaxException{
        
        File file = PropertiesUtils.findFile(fileConf);
        List<String> fileVersionList = SedaVersion.fileVersionList(file);
        List<String> manifestVersionList = manifestVersionList(evenReader);
        List<String> invalidVersionList = new ArrayList<String>();
        
        for (String s : manifestVersionList){
            if (!fileVersionList.contains(s)){
                LOGGER.info(s + ": invalid version");
                invalidVersionList.add(s);
            } else {
                LOGGER.info(s + ": valid version");
            }
        }
        return invalidVersionList;
    }
    
    /**
     * check the conformity of the binary object
     * 
     * @param params
     * @return List<String> list of the invalid digest message
     * @throws ProcessingException
     * @throws URISyntaxException
     * @throws ContentAddressableStorageNotFoundException
     * @throws ContentAddressableStorageServerException
     * @throws ContentAddressableStorageException
     */
    public List<String> checkConformityBinaryObject(WorkParams params) throws ProcessingException, URISyntaxException, ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException, ContentAddressableStorageException{
        ParametersChecker.checkParameter("WorkParams is a mandatory parameter", params);
        final String containerId = params.getContainerName();
        final WorkspaceClient client = workspaceClientFactory.create(params.getServerConfiguration().getUrlWorkspace());

        InputStream xmlFile = null;
        List<String> digestMessageInvalidList = new ArrayList<String>();
        try {
            xmlFile = client.getObject(containerId, SEDA_FOLDER + "/" + SEDA_FILE);
        } catch (ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException e) {
            LOGGER.error("Manifest.xml Not Found");
            throw new ProcessingException(e);
        }

        final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        XMLEventReader reader = null;

        try {
            reader = xmlInputFactory.createXMLEventReader(xmlFile);
            digestMessageInvalidList = compareDigestMessage(reader, client);
            reader.close();
        } catch (final XMLStreamException e) {
            LOGGER.error("Can not read SEDA");
            throw new ProcessingException(e);
        }
        return digestMessageInvalidList;
    }
    
    /**
     * compare the digest message between the manifest.xml and related uri content in workspace container
     * 
     * @param evenReader
     * @param client
     * @return List<String> list of the invalid digest message
     * @throws XMLStreamException
     * @throws URISyntaxException
     * @throws ContentAddressableStorageNotFoundException
     * @throws ContentAddressableStorageServerException
     * @throws ContentAddressableStorageException
     */
    public List<String> compareDigestMessage(XMLEventReader evenReader, WorkspaceClient client) 
        throws XMLStreamException, URISyntaxException, ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException, ContentAddressableStorageException{
        SedaUtilInfo sedaUtilInfo = getBinaryObjectInfo(evenReader);
        Map<String, BinaryObjectInfo> binaryObjectMap = sedaUtilInfo.getBinaryObjectMap();
        List<String> digestMessageInvalidList = new ArrayList<String>();
        
        for (String mapKey : binaryObjectMap.keySet()) {
            String uri = binaryObjectMap.get(mapKey).getUri().toString();
            String digestMessageManifest = binaryObjectMap.get(mapKey).getMessageDigest();
            DigestType algo = binaryObjectMap.get(mapKey).getAlgo();
            String digestMessage = client.computeObjectDigest(mapKey, SEDA_FOLDER + "/" + CONTENT_FOLDER + "/" + uri, algo);
            
            if(digestMessage != digestMessageManifest){
                LOGGER.info("Binary object Digest Message Invalid : " + uri );
                digestMessageInvalidList.add(digestMessageManifest);
            } else {
                LOGGER.info("Binary Object Digest Message Valid : " + uri);
            }            
        }
        
        return digestMessageInvalidList;
    }

}
