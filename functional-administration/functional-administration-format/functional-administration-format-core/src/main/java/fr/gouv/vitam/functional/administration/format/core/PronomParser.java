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

package fr.gouv.vitam.functional.administration.format.core;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.functional.administration.common.FileFormat;
import fr.gouv.vitam.functional.administration.common.exception.FileFormatException;
import fr.gouv.vitam.functional.administration.common.exception.InvalidFileFormatParseException;
import fr.gouv.vitam.functional.administration.common.exception.JsonNodeFormatCreationException;

/**
 * PronomParser parse the xml pronom file to get the info on file format
 */

public class PronomParser {

    /**
     * FileFormat prefix which indicate Inheritance
     */
    private static final String FMT = "fmt/";

    private static final String VERSION_PRONOM = "VersionPronom";
    private static final String TAG_FFSIGNATUREFILE = "FFSignatureFile";
    private static final String TAG_FILEFORMAT = "FileFormat";
    private static final String TAG_EXTENSION = "Extension";
    private static final String TAG_HASPRIORITYOVERFILEFORMATID = "HasPriorityOverFileFormatID";
    private static final String ATTR_PUID = "PUID";
    private static final String ATTR_ID = "ID";
    private static final String CREATED_DATE = "CreatedDate";
    private static final String ATTR_VERSION = "Version";
    private static final String ATTR_CREATEDDATE = "DateCreated";

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(PronomParser.class);

    /**
     * getPronom
     * 
     * @param xmlPronom as InputStream
     * @return : the list of file format as ArrayNode
     * @throws FileFormatException
     */
    @SuppressWarnings("unchecked")
    public static ArrayNode getPronom(InputStream xmlPronom) throws FileFormatException {
        FileFormat pronomFormat = new FileFormat();
        FileFormat fileFormat0 = new FileFormat();
        boolean bExtension = false;
        boolean bFileFormat = false;
        boolean bPriorityOverId = false;

        JsonNode jsonPronom = null;
        ArrayNode jsonFileFormatList = JsonHandler.createArrayNode();

        List<String> extensions = new ArrayList<String>();
        List<String> priorityOverIdList = new ArrayList<String>();
        Map<String, String> idToPUID = new HashMap<>();

        try {
            final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
            XMLEventReader eventReader = xmlInputFactory.createXMLEventReader(xmlPronom);
            while (eventReader.hasNext()) {
                XMLEvent event = eventReader.nextEvent();
                switch (event.getEventType()) {
                    case XMLStreamConstants.START_ELEMENT:
                        StartElement startElement = event.asStartElement();
                        String qName = startElement.getName().getLocalPart();
                        if (qName.equalsIgnoreCase(TAG_FFSIGNATUREFILE)) {
                            Iterator<Attribute> attributes = startElement.getAttributes();
                            while (attributes.hasNext()) {
                                Attribute attribute = attributes.next();
                                switch (attribute.getName().toString()) {
                                    case ATTR_CREATEDDATE:
                                        fileFormat0.setCreatedDate(attribute.getValue());
                                        break;
                                    case ATTR_VERSION:
                                        fileFormat0.setPronomVersion(attribute.getValue());
                                        break;
                                }

                            }
                        } else if (qName.equalsIgnoreCase(TAG_FILEFORMAT)) {
                            extensions.clear();
                            priorityOverIdList.clear();
                            bFileFormat = true;

                            Iterator<Attribute> attributes = startElement.getAttributes();
                            Map<String, Object> attributesMap = new HashMap<>();
                            while (attributes.hasNext()) {
                                Attribute attribute = attributes.next();
                                attributesMap.put(attribute.getName().toString(), attribute.getValue());
                            }
                            idToPUID.put(attributesMap.get(ATTR_ID).toString(),
                                attributesMap.get(ATTR_PUID).toString());
                            attributesMap.remove(ATTR_ID);
                            pronomFormat = getNewFileFormatFromAttributes(fileFormat0, attributesMap);
                        } else if (qName.equalsIgnoreCase(TAG_EXTENSION)) {
                            bExtension = true;

                        } else if (qName.equalsIgnoreCase(TAG_HASPRIORITYOVERFILEFORMATID)) {
                            bPriorityOverId = true;

                        }

                        break;

                    case XMLStreamConstants.CHARACTERS:
                        Characters characters = event.asCharacters();
                        if (bExtension && bFileFormat) {
                            extensions.add(characters.getData());
                            bExtension = false;
                        }

                        if (bPriorityOverId && bFileFormat) {
                            priorityOverIdList.add(characters.getData());
                            bPriorityOverId = false;
                        }

                        break;

                    case XMLStreamConstants.END_ELEMENT:
                        EndElement endElement = event.asEndElement();
                        qName = endElement.getName().getLocalPart();
                        if (qName.equalsIgnoreCase(TAG_FILEFORMAT)) {
                            pronomFormat.setExtension(extensions);
                            pronomFormat.setPriorityOverIdList(priorityOverIdList);
                            //Add default value
                            pronomFormat.setAlert(false);
                            pronomFormat.setComment("");
                            pronomFormat.setGroup("");
                            
                            copyAttributesFromFileFormat(pronomFormat, fileFormat0);
                            jsonPronom = JsonHandler.getFromString(pronomFormat.toJson());
                            jsonFileFormatList.add(jsonPronom);
                            bFileFormat = false;
                        }
                        break;
                }
            }
        } catch (XMLStreamException e) {
            LOGGER.error(e.getMessage());
            throw new InvalidFileFormatParseException("Invalid xml file format");
        } catch (InvalidParseOperationException e) {
            LOGGER.error(e.getMessage());
            throw new JsonNodeFormatCreationException("Invalid object to create a json");
        }

        for (Iterator<JsonNode> it = jsonFileFormatList.elements(); it.hasNext();) {
            ObjectNode node = (ObjectNode) it.next();
            ArrayNode priorityVersionList = (ArrayNode) node.get(TAG_HASPRIORITYOVERFILEFORMATID);
            if (priorityVersionList != null) {
                ArrayNode newPriorityVersionList = JsonHandler.createArrayNode();
                for (Iterator<JsonNode> iterator = priorityVersionList.elements(); iterator.hasNext();) {
                    TextNode childId = (TextNode) iterator.next();
                    newPriorityVersionList.add(idToPUID.get(childId.asText()));
                }
                node.set(TAG_HASPRIORITYOVERFILEFORMATID, newPriorityVersionList);
            }
        }

        return jsonFileFormatList;
    }

    private static FileFormat getNewFileFormatFromAttributes(FileFormat fileFormat, Map<String, Object> attributes) {
        FileFormat newFileFormat = new FileFormat();
        if (attributes.get(ATTR_PUID).toString().startsWith(FMT)) {
            for (String i : fileFormat.keySet()) {
                newFileFormat.put(i, fileFormat.get(i));
            }
            newFileFormat.putAll(attributes);
        } else {
            newFileFormat.append(CREATED_DATE, fileFormat.getString(CREATED_DATE));
            newFileFormat.append(VERSION_PRONOM, fileFormat.getString(VERSION_PRONOM));
            newFileFormat.putAll(attributes);
        }
        return newFileFormat;
    }

    private static void copyAttributesFromFileFormat(FileFormat fileFormatSource, FileFormat fileFormatDest) {
        if (fileFormatSource.getString(ATTR_PUID).startsWith(FMT)) {
            for (String i : fileFormatSource.keySet()) {
                if (!ATTR_PUID.equals(i) && !TAG_HASPRIORITYOVERFILEFORMATID.equals(i)) {
                    fileFormatDest.put(i, fileFormatSource.get(i));
                }
            }
        } else {
            fileFormatDest.clear();
            fileFormatDest.append(CREATED_DATE, fileFormatSource.getString(CREATED_DATE));
            fileFormatDest.append(VERSION_PRONOM, fileFormatSource.getString(VERSION_PRONOM));
        }
    }
}
