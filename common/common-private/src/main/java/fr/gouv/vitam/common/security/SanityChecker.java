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
package fr.gouv.vitam.common.security;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.json.JsonSanitizer;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

/**
 * Checker for Sanity of XML and Json <br>
 * <br>
 * Json : check if json is not exceed the limit size, if json does not contain script tag <br>
 * XML: check if XML file is not exceed the limit size, and it does not contain CDATA, ENTITY or SCRIPT tag
 *
 */

public class SanityChecker {
    // TODO : issue with public static which is not final
    // defaut parameters for XML check
    
    /*µ
     * max size of xml tag value 
     */
    public static int limitValueTagSize = 1000000;
    /**
     *  max size of xml file 
     */
    public static double limitFileSize = 1000000000;
    private static final String CDATA_TAG_UNESCAPED = "<![CDATA[";
    private static final String CDATA_TAG_ESCAPED = "&lt;![CDATA[";
    private static final String ENTITY_TAG_UNESCAPED = "<!ENTITY";
    private static final String ENTITY_TAG_ESCAPED = "&lt;!ENTITY";
    private static final String SCRIPT_TAG_UNESCAPED = "<script>";
    private static final String SCRIPT_TAG_ESCAPED = "&lt;script&gt;";

    // defaut parameters for Json check
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(SanityChecker.class);

    private static final String tagStart =
        "\\<\\w+((\\s+\\w+(\\s*\\=\\s*(?:\".*?\"|'.*?'|[^'\"\\>\\s]+))?)+\\s*|\\s*)\\>";
    private static final String tagEnd =
        "\\</\\w+\\>";
    private static final String tagSelfClosing =
        "\\<\\w+((\\s+\\w+(\\s*\\=\\s*(?:\".*?\"|'.*?'|[^'\"\\>\\s]+))?)+\\s*|\\s*)/\\>";
    private static final String htmlEntity =
        "&[a-zA-Z][a-zA-Z0-9]+;";
    private static final Pattern htmlPattern = Pattern.compile(
        "(" + tagStart + ".*" + tagEnd + ")|(" + tagSelfClosing + ")|(" + htmlEntity + ")",
        Pattern.DOTALL);

    /**
     *  max size of json value field  
     */
    public static final int limitSize = 100;

    /**
     * max size of json  
     */
    public static int limitJsonSize = 1000000000;

    /**
     * checkXMLSanityTagValueSize
     * 
     * @param xmlFile xml file
     * @throws IOException when read file error
     * @throws InvalidParseOperationException when parse file error
     */
    public static void checkXMLSanityTagValueSize(File xmlFile)
        throws InvalidParseOperationException, IOException {
        InputStream xmlStream = new FileInputStream(xmlFile);

        final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        // read XML input stream
        try {
            XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(xmlStream);
            while (reader.hasNext()) {

                int event = reader.next();
                if (event == XMLStreamConstants.CHARACTERS) {
                    if (!reader.getText().trim().equals("")) {
                        checkSanitySizeValueTag(reader.getText().trim(), limitValueTagSize);
                    }
                }
            }
        } catch (XMLStreamException e) {
            LOGGER.error(e.getMessage());
        }
        xmlStream.close();
    }

    /**
     * checkSanitySizeValueTag : check the size of one tag value
     * 
     * @param tagValue as String, exceed limit size as integer
     * @return true if the value tag size not exceed limit
     */
    private static void checkSanitySizeValueTag(String tagValue, int size)
        throws InvalidParseOperationException {
        if (tagValue != null && tagValue.length() > size) {
            throw new InvalidParseOperationException("Tag value size exceeds sanity check of " + size);
        }
    }


    /**
     * CheckXMLSanityFileSize : check size of xml file
     * 
     * @param xmlFile as File
     * @throws IOException when read file exception
     */
    public static void checkXMLSanityFileSize(File xmlFile) throws IOException {
        if (xmlFile.length() > limitFileSize) {
            throw new IOException("File size exceeds sanity check of " + xmlFile.length());
        }
    }

    /**
     * CheckXMLSanityTags : check invalid tag contains of a xml file
     * 
     * @param xmlFile : XML file path as String
     * @throws IOException when read file error
     * @throws InvalidParseOperationException when parse file error
     */

    public static void checkXMLSanityTags(File xmlFile) throws InvalidParseOperationException, IOException {
        Reader fileReader = null;
        try {
            fileReader = new FileReader(xmlFile);
        } catch (FileNotFoundException e) {
            LOGGER.error(e.getMessage());
        }
        BufferedReader bufReader = new BufferedReader(fileReader);
        String line = null;
        try {
            line = bufReader.readLine();
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
        // TODO : use an iterator
        while (line != null) {
            checkSanityTags(line, CDATA_TAG_ESCAPED);
            checkSanityTags(line, CDATA_TAG_UNESCAPED);
            checkSanityTags(line, ENTITY_TAG_ESCAPED);
            checkSanityTags(line, ENTITY_TAG_UNESCAPED);
            checkSanityTags(line, CDATA_TAG_UNESCAPED);
            checkSanityTags(line, SCRIPT_TAG_UNESCAPED);
            checkSanityTags(line, SCRIPT_TAG_ESCAPED);
            try {
                line = bufReader.readLine();
            } catch (IOException e) {
                LOGGER.error(e.getMessage());
            }
        }
        bufReader.close();
    }

    /**
     * checkSanityTags : check if there is an invalid tag
     * 
     * @param invalidTag, data to check as String
     * @return boolean true if no invalid tag found else false
     */
    private static void checkSanityTags(String dataLine, String invalidTag) throws InvalidParseOperationException {
        if (dataLine != null && invalidTag != null && (dataLine.contains(invalidTag))) {
            throw new InvalidParseOperationException("Invalid tag sanity check of : " + invalidTag);
        }
    }

    /**
     * checkXMLAll : check xml sanity all aspect : size, tag size, invalid tag
     * 
     * @param xmlFile as File
     * @return boolean : true if all check ok else false
     * @throws InvalidParseOperationException when parse file error
     * @throws IOException when read file error
     */
    public static boolean checkXMLAll(File xmlFile) throws InvalidParseOperationException, IOException {
        try {
            checkXMLSanityFileSize(xmlFile);
            checkXMLSanityTagValueSize(xmlFile);
            checkXMLSanityTags(xmlFile);
        }catch (InvalidParseOperationException e){
            return false;
        }
        return true;
    }

    /**
     * checkJsonAll : Check sanity of json : size, invalid tag
     * 
     * @param json as JsonNode
     * @return boolean true if sanity check of json is good, false if not
     * @throws InvalidParseOperationException when parse json error
     */
    public static boolean checkJsonAll(JsonNode json) throws InvalidParseOperationException {
        String jsonish = JsonHandler.writeAsString(json);
        String wellFormedJson = JsonSanitizer.sanitize(jsonish);
        try {
            checkJsonFileSize(json);
        } catch (InvalidParseOperationException e) {
            return false;
        }
        if ((checkJsonSanity(JsonHandler.getFromString(wellFormedJson)).size() == 0)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * checkJsonSanity : check sanity of json and find invalid key
     * 
     * @param json as JsonNode
     * @return list of the key whose value is in illegal size
     */
    public static List<String> checkJsonSanity(JsonNode json) {
        List<String> invalidKeyList = new ArrayList<String>();
        Iterator<Map.Entry<String, JsonNode>> fields = json.fields();

        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();

            String key = entry.getKey();
            JsonNode value = entry.getValue();

            if (!value.isValueNode()) {
                invalidKeyList.addAll(checkJsonSanity(value));
            } else {
                try {
                    if (htmlPattern.matcher(JsonHandler.writeAsString(value)).find()) {
                        invalidKeyList.add(key);
                    } else {
                        checkJsonValueSize(JsonHandler.writeAsString(value), limitSize);
                    }
                } catch (InvalidParseOperationException e) {
                    LOGGER.error(e.getMessage());
                    invalidKeyList.add(key);
                }
            }
        }

        return invalidKeyList;
    }

    protected static final void checkJsonValueSize(String arg, int size)
        throws InvalidParseOperationException {
        ParametersChecker.checkParameter("size is a mandatory parameter", size);
        if (arg != null && arg.length() > size) {
            throw new InvalidParseOperationException(
                "json value size exceeds sanity check of " + size);
        }
    }

    /**
     * checkJsonFileSize
     * 
     * @param json as JsonNode
     * @return boolean : true if json size is not exceed limit else false
     * @throws InvalidParseOperationException if json size exceed limit
     */
    public static void checkJsonFileSize(JsonNode json) throws InvalidParseOperationException {
        if (JsonHandler.writeAsString(json).length() > limitJsonSize) {
            throw new InvalidParseOperationException(
                "Json size exceeds sanity check : " + JsonHandler.writeAsString(json).length());
        }
    }

}
