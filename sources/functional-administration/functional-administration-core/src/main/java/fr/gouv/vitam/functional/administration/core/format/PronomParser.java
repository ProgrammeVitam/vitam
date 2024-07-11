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
package fr.gouv.vitam.functional.administration.core.format;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.xml.ValidationXsdUtils;
import fr.gouv.vitam.common.xml.XMLInputFactoryUtils;
import fr.gouv.vitam.functional.administration.common.exception.FileFormatException;
import fr.gouv.vitam.functional.administration.common.exception.InvalidFileFormatParseException;
import fr.gouv.vitam.functional.administration.core.format.model.FileFormatModel;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.xml.sax.SAXException;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * PronomParser parse the xml pronom file to get the info on file format
 */

public class PronomParser {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(PronomParser.class);

    private static final String TAG_FFSIGNATUREFILE = "FFSignatureFile";
    private static final String TAG_FILEFORMAT = "FileFormat";
    private static final String TAG_EXTENSION = "Extension";
    private static final String TAG_HASPRIORITYOVERFILEFORMATID = "HasPriorityOverFileFormatID";
    private static final String ATTR_PUID = "PUID";
    private static final String ATTR_NAME = "Name";
    private static final String ATTR_ID = "ID";
    private static final String ATTR_VERSION = "Version";
    private static final String ATTR_CREATEDDATE = "DateCreated";
    private static final String EXTERNAL_MIME_TYPE = "MIMEType";

    private PronomParser() {
        // Empty
    }

    /**
     * Parse the file Pronom and transform it to an ArrayNode
     *
     * @param xmlPronomFile pronom file
     * @return : the list of file format as ArrayNode
     * @throws FileFormatException if exception occurred when get pronom data
     */
    @SuppressWarnings("unchecked")
    public static List<FileFormatModel> getPronom(File xmlPronomFile) throws FileFormatException {
        boolean bExtension = false;
        boolean bFileFormat = false;
        boolean bPriorityOverId = false;

        validateSchema(xmlPronomFile);

        List<FileFormatModel> fileFormatModels = new ArrayList<>();

        final Map<String, String> idToPUID = new HashMap<>();

        String creationDate = null;
        String pronomVersion = null;

        FileFormatModel fileFormatModel = new FileFormatModel();
        MultiValuedMap<String, String> hasPriorityOverFileFormatIDByPuid = new ArrayListValuedHashMap<>();

        String updateDate = LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now());

        try (InputStream xmlPronom = new FileInputStream(xmlPronomFile)) {
            final XMLInputFactory xmlInputFactory = XMLInputFactoryUtils.newInstance();

            final XMLEventReader eventReader = xmlInputFactory.createXMLEventReader(xmlPronom);
            while (eventReader.hasNext()) {
                final XMLEvent event = eventReader.nextEvent();
                switch (event.getEventType()) {
                    case XMLStreamConstants.START_ELEMENT:
                        final StartElement startElement = event.asStartElement();
                        String qName = startElement.getName().getLocalPart();
                        if (qName.equalsIgnoreCase(TAG_FFSIGNATUREFILE)) {
                            final Iterator<Attribute> attributes = startElement.getAttributes();
                            while (attributes.hasNext()) {
                                final Attribute attribute = attributes.next();
                                switch (attribute.getName().toString()) {
                                    case ATTR_CREATEDDATE:
                                        // Depend on the date format in the file
                                        creationDate = LocalDateUtil.getFormattedDateForMongo(attribute.getValue());
                                        break;
                                    case ATTR_VERSION:
                                        pronomVersion = attribute.getValue();
                                        break;
                                }
                            }
                        } else if (qName.equalsIgnoreCase(TAG_FILEFORMAT)) {
                            fileFormatModel = new FileFormatModel()
                                .setVersionPronom(pronomVersion)
                                .setCreatedDate(creationDate)
                                .setUpdateDate(updateDate);

                            bFileFormat = true;

                            final Iterator<Attribute> attributes = startElement.getAttributes();
                            String id = null;
                            while (attributes.hasNext()) {
                                final Attribute attribute = attributes.next();
                                String value = attribute.getValue();
                                switch (attribute.getName().getLocalPart()) {
                                    case EXTERNAL_MIME_TYPE:
                                        fileFormatModel.setMimeType(value.replace(",", ";"));
                                        break;
                                    case ATTR_VERSION:
                                        fileFormatModel.setVersion(value);
                                        break;
                                    case ATTR_NAME:
                                        fileFormatModel.setName(value);
                                        break;
                                    case ATTR_PUID:
                                        fileFormatModel.setPuid(value);
                                        break;
                                    case ATTR_ID:
                                        id = value;
                                        break;
                                }
                            }

                            idToPUID.put(id, fileFormatModel.getPuid());
                        } else if (qName.equalsIgnoreCase(TAG_EXTENSION)) {
                            bExtension = true;
                        } else if (qName.equalsIgnoreCase(TAG_HASPRIORITYOVERFILEFORMATID)) {
                            bPriorityOverId = true;
                        }

                        break;
                    case XMLStreamConstants.CHARACTERS:
                        final Characters characters = event.asCharacters();
                        if (bExtension && bFileFormat) {
                            fileFormatModel.getExtension().add(characters.getData());
                            bExtension = false;
                        }

                        if (bPriorityOverId && bFileFormat) {
                            hasPriorityOverFileFormatIDByPuid.put(fileFormatModel.getPuid(), characters.getData());
                            bPriorityOverId = false;
                        }

                        break;
                    case XMLStreamConstants.END_ELEMENT:
                        final EndElement endElement = event.asEndElement();
                        qName = endElement.getName().getLocalPart();
                        if (qName.equalsIgnoreCase(TAG_FILEFORMAT)) {
                            fileFormatModels.add(fileFormatModel);
                            fileFormatModel = null;
                        }
                        break;
                }
            }
        } catch (final XMLStreamException e) {
            throw new InvalidFileFormatParseException("Invalid xml file format", e);
        } catch (IOException e) {
            throw new InvalidFileFormatParseException("Could not load xml file format", e);
        }

        for (FileFormatModel formatModel : fileFormatModels) {
            for (String id : hasPriorityOverFileFormatIDByPuid.get(formatModel.getPuid())) {
                formatModel.getHasPriorityOverFileFormatID().add(idToPUID.get(id));
            }
        }

        return fileFormatModels;
    }

    private static void validateSchema(File xmlPronomFile) throws InvalidFileFormatParseException {
        try (FileInputStream fis = new FileInputStream(xmlPronomFile)) {
            if (!ValidationXsdUtils.getInstance().checkWithXSD(fis, "DROID_SignatureFile_Vitam.xsd")) {
                throw new InvalidFileFormatParseException("Schema validation failed for xml file format");
            }
        } catch (SAXException | XMLStreamException e) {
            throw new InvalidFileFormatParseException("Invalid xml file format", e);
        } catch (IOException e) {
            throw new InvalidFileFormatParseException("Could not load xml file format", e);
        }
    }
}
