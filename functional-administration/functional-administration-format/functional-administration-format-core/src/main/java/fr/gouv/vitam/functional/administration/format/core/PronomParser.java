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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.functional.administration.common.FileFormat;
import fr.gouv.vitam.functional.administration.common.exception.FileFormatException;
import fr.gouv.vitam.functional.administration.common.exception.FileFormatNotFoundException;
import fr.gouv.vitam.functional.administration.common.exception.InvalidFileFormatParseException;
import fr.gouv.vitam.functional.administration.common.exception.JsonNodeFormatCreationException;

/**
 * PronomParser parse the xml pronom file to get the info on file format
 */

public class PronomParser {

    private static final String TAG_FFSIGNATUREFILE = "FFSignatureFile";
    private static final String TAG_FILEFORMAT = "FileFormat";
    private static final String TAG_EXTENSION = "Extension";
    private static final String TAG_PRIVIOUSVERION = "HasPriorityOverFileFormatID";
    private static final String ATTR_MIMETYPE = "MIMEType";
    private static final String ATTR_NAME = "Name";
    private static final String ATTR_PUID = "PUID";
    private static final String ATTR_VERSION = "Version";
    private static final String ATTR_CREATEDDATE = "DateCreated";

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(PronomParser.class);

    /**
     * getPronom
     * 
     * @param pronomFile as File
     * @return : the list of file format as ArrayNode
     * @throws FileFormatException 
     */
    public static ArrayNode getPronom(File pronomFile) throws FileFormatException {
        FileFormat pronomFormat = new FileFormat();
        boolean bExtension = false;
        boolean bFileFormat = false;
        boolean bPriorityOverId = false;
        String pronomVersion = null;
        String createdDate = null;

        JsonNode jsonPronom = null;
        ArrayNode jsonFileFormatList = JsonHandler.createArrayNode();

        List<String> mimeTypes = new ArrayList<String>();
        List<String> extensions = new ArrayList<String>();
        List<String> priorityOverIdList = new ArrayList<String>();

        try {
            InputStream xmlPronomStream = new FileInputStream(pronomFile);
            final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
            XMLEventReader eventReader = xmlInputFactory.createXMLEventReader(xmlPronomStream);
            while (eventReader.hasNext()) {
                XMLEvent event = eventReader.nextEvent();
                switch (event.getEventType()) {
                    case XMLStreamConstants.START_ELEMENT:
                        StartElement startElement = event.asStartElement();
                        String qName = startElement.getName().getLocalPart();
                        if (qName.equalsIgnoreCase(TAG_FILEFORMAT)) {
                            pronomFormat = new FileFormat();
                            pronomFormat.setCreatedDate(createdDate);
                            pronomFormat.setPronomVersion(pronomVersion);
                            extensions.clear();
                            priorityOverIdList.clear();
                            mimeTypes.clear();

                            bFileFormat = true;
                            Iterator<Attribute> attributes = startElement.getAttributes();
                            while (attributes.hasNext()) {
                                Attribute attribute = attributes.next();
                                switch (attribute.getName().toString()) {

                                    case ATTR_MIMETYPE:
                                        mimeTypes.add(attribute.getValue());
                                        pronomFormat.setMimeType(mimeTypes);
                                        break;
                                    case ATTR_NAME:
                                        pronomFormat.setName(attribute.getValue());
                                        break;
                                    case ATTR_PUID:
                                        pronomFormat.setPUID(attribute.getValue());
                                        break;
                                    case ATTR_VERSION:
                                        pronomFormat.setVersion(attribute.getValue());
                                        break;
                                }
                            }
                        } else if (qName.equalsIgnoreCase(TAG_EXTENSION)) {
                            bExtension = true;

                        } else if (qName.equalsIgnoreCase(TAG_PRIVIOUSVERION)) {
                            bPriorityOverId = true;

                        } else if (qName.equalsIgnoreCase(TAG_FFSIGNATUREFILE)) {
                            Iterator<Attribute> attributes = startElement.getAttributes();
                            while (attributes.hasNext()) {
                                Attribute attribute = attributes.next();
                                switch (attribute.getName().toString()) {
                                    case ATTR_CREATEDDATE:
                                        createdDate = attribute.getValue();
                                        break;
                                    case ATTR_VERSION:
                                        pronomVersion = attribute.getValue();
                                        break;
                                }

                            }
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
                            jsonPronom = JsonHandler.getFromString(pronomFormat.toJson());
                            jsonFileFormatList.add(jsonPronom);
                            bFileFormat = false;
                        }
                        break;
                }
            }
        } catch (FileNotFoundException e) {
            LOGGER.error(e.getMessage());
            throw new FileFormatNotFoundException("Not found referential format file");
        } catch (XMLStreamException e) {
            LOGGER.error(e.getMessage());
            throw new InvalidFileFormatParseException("Invalid xml file format");            
        } catch (InvalidParseOperationException e) {
            LOGGER.error(e.getMessage());
            throw new JsonNodeFormatCreationException("Invalid object to create a json");
        }

        return jsonFileFormatList;
    }
}
