/**
 * This file is part of Vitam Project.
 *
 * Copyright 2009, Frederic Bregier, and individual contributors by the author
 * tags. See the COPYRIGHT.txt in the distribution for a full listing of
 * individual contributors.
 *
 * All Vitam Project is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * Vitam is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Vitam . If not, see <http://www.gnu.org/licenses/>.
 */
package fr.gouv.vitam.common.json;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import fr.gouv.vitam.common.exception.InvalidParseOperationException;

/**
 * XML handler using Json handler below
 *
 * 
 *
 */
public final class XmlHandler {

    /**
     * Default JacksonXmlModule
     */
    private static final JacksonXmlModule XMLMODULE = new JacksonXmlModule();
    /**
     * Default XmlMapper
     */
    private static final XmlMapper XML_MAPPER;

    static {
        XMLMODULE.setDefaultUseWrapper(false);
        XML_MAPPER = new XmlMapper(XMLMODULE);
        XML_MAPPER.registerModule(new JavaTimeModule());
        XML_MAPPER.setPropertyNamingStrategy(PropertyNamingStrategy.UPPER_CAMEL_CASE);
        XML_MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        XML_MAPPER.configure(SerializationFeature.INDENT_OUTPUT, false);// not yet supported
        XML_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        XML_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        XML_MAPPER.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
        XML_MAPPER.configure(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS, false);
        XML_MAPPER.configure(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED,
                false);
        XML_MAPPER.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        XML_MAPPER.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        XML_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        XML_MAPPER.configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, true);
    }

    private XmlHandler() {
    }

    /**
     *
     * @param value
     * @param clasz
     * @return the object of type clasz
     * @throws InvalidParseOperationException
     */
    public static final <T> T getFromString(final String value, final Class<T> clasz)
            throws InvalidParseOperationException {
        try {
            return XML_MAPPER.readValue(value, clasz);
        } catch (IOException e) {
            throw new InvalidParseOperationException(e);
        }
    }

    /**
     *
     * @param file
     * @param clasz
     * @return the corresponding object
     * @throws InvalidParseOperationException
     */
    public static final Object getFromFile(File file, Class<?> clasz)
            throws InvalidParseOperationException {
        try {
            return XML_MAPPER.readValue(file, clasz);
        } catch (final IOException e) {
            throw new InvalidParseOperationException(e);
        }
    }

    /**
     *
     * @param object
     * @return the XML representation of the object
     * @throws InvalidParseOperationException
     */
    public static final String writeAsString(final Object object)
            throws InvalidParseOperationException {
        try {
            return XML_MAPPER.writeValueAsString(object);
        } catch (final JsonProcessingException e) {
            throw new InvalidParseOperationException(e);
        }
    }

    /**
     *
     * @param object
     * @param file
     * @throws InvalidParseOperationException
     */
    public static final void writeAsFile(final Object object, File file)
            throws InvalidParseOperationException {
        try {
            XML_MAPPER.writeValue(file, object);
        } catch (final IOException e) {
            throw new InvalidParseOperationException(e);
        }
    }

    /*
     * @param unformattedXml
     * @return the formatted xml Keep if needed later on: depend on xerces 2.4.0
     * public static String format(String unformattedXml) { try { final Document
     * document = parseXmlFile(unformattedXml); OutputFormat format = new
     * OutputFormat(document); format.setLineWidth(0);
     * format.setIndenting(true); format.setIndent(2); Writer out = new
     * StringWriter(); XMLSerializer serializer = new XMLSerializer(out,
     * format); serializer.serialize(document); return out.toString(); } catch
     * (IOException e) { throw new RuntimeException(e); } } private static
     * Document parseXmlFile(String in) { try { DocumentBuilderFactory dbf =
     * DocumentBuilderFactory.newInstance(); DocumentBuilder db =
     * dbf.newDocumentBuilder(); InputSource is = new InputSource(new
     * StringReader(in)); return db.parse(is); } catch
     * (ParserConfigurationException e) { throw new RuntimeException(e); } catch
     * (SAXException e) { throw new RuntimeException(e); } catch (IOException e)
     * { throw new RuntimeException(e); } }
     */
}
