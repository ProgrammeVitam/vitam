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
package fr.gouv.vitam.common.json;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.Lists;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.logging.SysErrLogger;

/**
 * JSON handler using Json format
 *
 *
 *
 */
public final class JsonHandler {
    private static final String OBJECT = "object";
    /**
     * Default JsonFactory
     */
    private static final JsonFactory JSONFACTORY = new JsonFactory();
    /**
     * Default ObjectMapper
     */
    private static final ObjectMapper OBJECT_MAPPER;
    /**
     * Default ObjectMapperUnprettyPrint
     */
    private static final ObjectMapper OBJECT_MAPPER_UNPRETTY;
    /**
     * Default ObjectMapperLowerCamelCase
     */
    private static final ObjectMapper OBJECT_MAPPER_LOWER_CAMEL_CASE;

    static {
        OBJECT_MAPPER = buildObjectMapper();
        OBJECT_MAPPER_UNPRETTY = buildObjectMapper();
        OBJECT_MAPPER_UNPRETTY.disable(SerializationFeature.INDENT_OUTPUT);
        OBJECT_MAPPER_LOWER_CAMEL_CASE = buildObjectMapper();
        OBJECT_MAPPER_LOWER_CAMEL_CASE.setPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE);
        OBJECT_MAPPER_LOWER_CAMEL_CASE.disable(SerializationFeature.INDENT_OUTPUT);
    }

    private JsonHandler() {
        // Empty constructor
    }

    private static final ObjectMapper buildObjectMapper() {
        final ObjectMapper objectMapper = new ObjectMapper(JSONFACTORY);
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.UPPER_CAMEL_CASE);
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, true);
        objectMapper.configure(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS, true);
        objectMapper.configure(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED,
            false);
        objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        objectMapper.configure(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS, true);
        objectMapper.configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, true);
        return objectMapper;
    }

    /**
     * @return the current factory
     */
    public static final JsonNodeFactory getFactory() {
        return OBJECT_MAPPER.getNodeFactory();
    }

    /**
     *
     * @return an empty ObjectNode
     */
    public static final ObjectNode createObjectNode() {
        return OBJECT_MAPPER.createObjectNode();
    }

    /**
     * @return an empty ArrayNode
     */
    public static final ArrayNode createArrayNode() {
        return OBJECT_MAPPER.createArrayNode();
    }

    /**
     *
     * @param value
     * @return the jsonNode (ObjectNode or ArrayNode)
     * @throws InvalidParseOperationException
     */
    public static final JsonNode getFromString(final String value)
        throws InvalidParseOperationException {
        try {
            ParametersChecker.checkParameter("value", value);
            return OBJECT_MAPPER.readTree(value);
        } catch (final IOException | IllegalArgumentException e) {
            throw new InvalidParseOperationException(e);
        }
    }

    /**
     *
     * @param file
     * @return the jsonNode (ObjectNode or ArrayNode)
     * @throws InvalidParseOperationException
     */
    public static final JsonNode getFromFile(final File file)
        throws InvalidParseOperationException {
        try {
            ParametersChecker.checkParameter("File", file);
            return OBJECT_MAPPER.readTree(file);
        } catch (final IOException | IllegalArgumentException e) {
            throw new InvalidParseOperationException(e);
        }
    }

    /**
     *
     * @param stream
     * @return the jsonNode (ObjectNode or ArrayNode)
     * @throws InvalidParseOperationException
     */
    public static final JsonNode getFromInputStream(final InputStream stream)
        throws InvalidParseOperationException {
        try {
            ParametersChecker.checkParameter("InputStream", stream);
            return OBJECT_MAPPER.readTree(stream);
        } catch (final IOException | IllegalArgumentException e) {
            throw new InvalidParseOperationException(e);
        }
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
            ParametersChecker.checkParameter("value or class", value, clasz);
            return OBJECT_MAPPER.readValue(value, clasz);
        } catch (final IOException | IllegalArgumentException e) {
            throw new InvalidParseOperationException(e);
        }
    }

    /**
     *
     * @param value
     * @param clasz 
     * @param parameterClazz 
     * @return the object of type clasz
     * @throws InvalidParseOperationException
     */
    public static final <T> T getFromString(final String value, final Class<T> clasz, Class<?> parameterClazz )
        throws InvalidParseOperationException {
        try {
            ParametersChecker.checkParameter("value, class or parameterClazz", value, clasz, parameterClazz);
            JavaType type = OBJECT_MAPPER.getTypeFactory().constructParametricType(clasz, parameterClazz);
            return OBJECT_MAPPER.readValue(value, type);
        } catch (final IOException | IllegalArgumentException e) {
            throw new InvalidParseOperationException(e);
        }
    }


    /**
     *
     * @param value
     * @param clasz
     * @return the object of type clasz
     * @throws InvalidParseOperationException
     */
    public static final <T> T getFromStringLowerCamelCase(final String value, final Class<T> clasz)
        throws InvalidParseOperationException {
        try {
            ParametersChecker.checkParameter("value or class", value, clasz);
            return OBJECT_MAPPER_LOWER_CAMEL_CASE.readValue(value, clasz);
        } catch (final IOException | IllegalArgumentException e) {
            throw new InvalidParseOperationException(e);
        }
    }

    /**
     *
     * @param value
     * @return the jsonNode (ObjectNode or ArrayNode)
     * @throws InvalidParseOperationException
     */
    public static final JsonNode getFromBytes(final byte[] value)
        throws InvalidParseOperationException {
        try {
            ParametersChecker.checkParameter("value", value);
            return OBJECT_MAPPER.readTree(value);
        } catch (final IOException | IllegalArgumentException e) {
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
    public static final <T> T getFromFile(File file, Class<T> clasz)
        throws InvalidParseOperationException {
        try {
            ParametersChecker.checkParameter("File or class", file, clasz);
            return OBJECT_MAPPER.readValue(file, clasz);
        } catch (final IOException | IllegalArgumentException e) {
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
    public static final <T> T getFromFileLowerCamelCase(File file, Class<T> clasz)
        throws InvalidParseOperationException {
        try {
            ParametersChecker.checkParameter("File or class", file, clasz);
            return OBJECT_MAPPER_LOWER_CAMEL_CASE.readValue(file, clasz);
        } catch (final IOException | IllegalArgumentException e) {
            throw new InvalidParseOperationException(e);
        }
    }

    /**
     *
     * @param jsonNode
     * @param clasz
     * @return the corresponding object
     * @throws InvalidParseOperationException
     */
    public static final <T> T getFromJsonNode(JsonNode jsonNode, Class<T> clasz)
        throws InvalidParseOperationException {
        try {
            ParametersChecker.checkParameter("JsonNode or class", jsonNode, clasz);
            return OBJECT_MAPPER.treeToValue(jsonNode, clasz);
        } catch (final JsonProcessingException e) {
            throw new InvalidParseOperationException(e);
        }
    }

    /**
     *
     * @param jsonNode
     * @param clasz
     * @return the corresponding object
     * @throws InvalidParseOperationException
     */
    public static final <T> T getFromJsonNodeLowerCamelCase(JsonNode jsonNode, Class<T> clasz)
        throws InvalidParseOperationException {
        try {
            ParametersChecker.checkParameter("JsonNode or class", jsonNode, clasz);
            return OBJECT_MAPPER_LOWER_CAMEL_CASE.treeToValue(jsonNode, clasz);
        } catch (final JsonProcessingException e) {
            throw new InvalidParseOperationException(e);
        }
    }

    /**
     *
     * @param object
     * @return the Json representation of the object
     * @throws InvalidParseOperationException
     */
    public static final JsonNode toJsonNode(final Object object)
        throws InvalidParseOperationException {
        try {
            ParametersChecker.checkParameter(OBJECT, object);
            return OBJECT_MAPPER.convertValue(object, JsonNode.class);
        } catch (final IllegalArgumentException e) {
            throw new InvalidParseOperationException(e);
        }
    }

    /**
     *
     * @param object
     * @return the Json representation of the object (shall be prettyPrint)
     * @throws InvalidParseOperationException
     */
    public static final String writeAsString(final Object object)
        throws InvalidParseOperationException {
        try {
            ParametersChecker.checkParameter(OBJECT, object);
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (final JsonProcessingException | IllegalArgumentException e) {
            throw new InvalidParseOperationException(e);
        }
    }

    /**
     *
     * @param object
     * @return the Json representation of the object in Pretty Print format
     */
    public static String prettyPrint(Object object) {
        try {
            ParametersChecker.checkParameter(OBJECT, object);
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter()
                .writeValueAsString(object);
        } catch (final JsonProcessingException | IllegalArgumentException e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            return "{}";
        }
    }

    /**
     *
     * @param object
     * @return the Json representation of the object in UnPretty Print format
     */
    public static String unprettyPrint(Object object) {
        try {
            ParametersChecker.checkParameter(OBJECT, object);
            return OBJECT_MAPPER_UNPRETTY.writeValueAsString(object);
        } catch (final JsonProcessingException | IllegalArgumentException e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            return "{}";
        }
    }

    /**
     *
     * @param object
     * @return the Json representation of the object in UnPretty Print format
     */
    public static String unprettyPrintLowerCamelCase(Object object) {
        try {
            ParametersChecker.checkParameter(OBJECT, object);
            return OBJECT_MAPPER_LOWER_CAMEL_CASE.writeValueAsString(object);
        } catch (final JsonProcessingException | IllegalArgumentException e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            return "{}";
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
            ParametersChecker.checkParameter("object or file", object, file);
            OBJECT_MAPPER.writeValue(file, object);
        } catch (final IOException | IllegalArgumentException e) {
            throw new InvalidParseOperationException(e);
        }
    }

    /**
     *
     * @param object
     * @param outputStream
     * @throws InvalidParseOperationException
     */
    public static final void writeAsOutputStream(final Object object, OutputStream outputStream)
        throws InvalidParseOperationException {
        try {
            ParametersChecker.checkParameter("object or file", object, outputStream);
            OBJECT_MAPPER.writeValue(outputStream, object);
        } catch (final IOException | IllegalArgumentException e) {
            throw new InvalidParseOperationException(e);
        }
    }

    /**
     * Check if JsonNodes are not null and not empty
     *
     * @param message default message within exception
     * @param nodes
     * @throws IllegalArgumentException if nodes are null or empty
     */
    public static final void checkNullOrEmpty(final String message, final JsonNode... nodes) {
        if (nodes != null) {
            for (final JsonNode jsonNode : nodes) {
                if (jsonNode == null || jsonNode.size() == 0) {
                    throw new IllegalArgumentException(message);
                }
            }
        } else {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * node should have only one property
     *
     * @param nodeName name to print in case of error
     * @param node
     * @return the couple property name and property value
     * @throws InvalidParseOperationException
     */
    public static final Entry<String, JsonNode> checkUnicity(final String nodeName,
        final JsonNode node)
        throws InvalidParseOperationException {
        if (node == null || node.isMissingNode()) {
            throw new InvalidParseOperationException(
                "The current Node is missing(empty): " + nodeName + ":" + node);
        }
        if (node.isValueNode()) {
            // not allowed
            throw new InvalidParseOperationException(
                "The current Node is a simple value and should not: " + nodeName + ":" + node);
        }
        final int size = node.size();
        if (size > 1) {
            throw new InvalidParseOperationException(
                "More than one element in current Node: " + nodeName + ":" + node);
        }
        if (size == 0) {
            throw new InvalidParseOperationException(
                "Not enough element (0) in current Node: " + nodeName + ":" + node);
        }
        final Iterator<Entry<String, JsonNode>> iterator = node.fields();
        return iterator.next();
    }

    /**
     * node should have only one property ; simple value is allowed
     *
     * @param nodeName name to print in case of error
     * @param node
     * @return the couple property name and property value
     * @throws InvalidParseOperationException
     */
    public static final Entry<String, JsonNode> checkLaxUnicity(final String nodeName,
        final JsonNode node)
        throws InvalidParseOperationException {
        if (node == null || node.isMissingNode()) {
            throw new InvalidParseOperationException(
                "The current Node is missing(empty): " + nodeName + ":" + node);
        }
        if (node.isValueNode()) {
            // already one node
            return new Entry<String, JsonNode>() {
                @Override
                public JsonNode setValue(final JsonNode value) {
                    throw new IllegalArgumentException("Cannot set Value");
                }

                @Override
                public JsonNode getValue() {
                    return node;
                }

                @Override
                public String getKey() {
                    return null;
                }
            };
        }
        final int size = node.size();
        if (size > 1) {
            throw new InvalidParseOperationException(
                "More than one element in current Node: " + nodeName + ":" + node);
        }
        if (size == 0) {
            throw new InvalidParseOperationException(
                "Not enough element (0) in current Node: " + nodeName + ":" + node);
        }
        final Iterator<Entry<String, JsonNode>> iterator = node.fields();
        return iterator.next();
    }

    /**
     *
     * @param value
     * @return the corresponding HashMap
     * @throws InvalidParseOperationException
     */
    public static final Map<String, Object> getMapFromString(final String value)
        throws InvalidParseOperationException {
        if (value != null && !value.isEmpty()) {
            Map<String, Object> info = null;
            try {
                info = OBJECT_MAPPER.readValue(value,
                    new TypeReference<Map<String, Object>>() {});
            } catch (final IOException e) {
                throw new InvalidParseOperationException(e);
            }
            if (info == null) {
                info = new HashMap<>();
            }
            return info;
        } else {
            return new HashMap<>();
        }
    }

    /**
     *
     * @param value
     * @return the corresponding HashMap
     * @throws InvalidParseOperationException
     */
    public static final Map<String, String> getMapStringFromString(final String value)
        throws InvalidParseOperationException {
        if (value != null && !value.isEmpty()) {
            Map<String, String> info = null;
            try {
                info = OBJECT_MAPPER.readValue(value,
                    new TypeReference<Map<String, String>>() {});
            } catch (final IOException e) {
                throw new InvalidParseOperationException(e);
            }
            if (info == null) {
                info = new HashMap<>();
            }
            return info;
        } else {
            return new HashMap<>();
        }
    }

    /**
     *
     * @param inputStream
     * @return the corresponding HashMap
     * @throws InvalidParseOperationException
     */
    public static final Map<String, Object> getMapFromInputStream(final InputStream inputStream)
        throws InvalidParseOperationException {
        ParametersChecker.checkParameter("InputStream", inputStream);
        Map<String, Object> info = null;
        try {
            info = OBJECT_MAPPER.readValue(inputStream,
                new TypeReference<Map<String, Object>>() {});
        } catch (final IOException e) {
            throw new InvalidParseOperationException(e);
        } finally {
            try {
                inputStream.close();
            } catch (final IOException e) {
                SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            }
        }
        if (info == null) {
            info = new HashMap<>();
        }
        return info;
    }

    /**
     * transform an inputStream into a {@link Map<String, T>}
     * @param inputStream
     * @param parameterClazz type of the value on the Map
     * @param <T>
     * @return the corresponding HashMap
     * @throws InvalidParseOperationException
     */
    public static final <T> Map<String, T> getMapFromInputStream(final InputStream inputStream, Class<T> parameterClazz)
        throws InvalidParseOperationException {
        ParametersChecker.checkParameter("InputStream", inputStream);
        Map<String, T> info ;
        try {
            JavaType type = OBJECT_MAPPER.getTypeFactory().constructParametricType(Map.class, String.class, parameterClazz);
            info = OBJECT_MAPPER.readValue(inputStream, type);
        } catch (final IOException e) {
            throw new InvalidParseOperationException(e);
        } finally {
            try {
                inputStream.close();
            } catch (final IOException e) {
                SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            }
        }
        if (info == null) {
            info = new HashMap<>();
        }
        return info;
    }


    /**
     *
     * @param inputStream
     * @param clasz
     * @return the corresponding object
     * @throws InvalidParseOperationException
     */
    public static final <T> T getFromInputStream(InputStream inputStream, Class<T> clasz)
        throws InvalidParseOperationException {
        try {
            ParametersChecker.checkParameter("InputStream or class", inputStream, clasz);
            return OBJECT_MAPPER.readValue(inputStream, clasz);
        } catch (final IOException | IllegalArgumentException e) {
            throw new InvalidParseOperationException(e);
        } finally {
            try {
                inputStream.close();
            } catch (final IOException e) {
                SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            }
        }
    }


    /**
     * From one ArrayNode, get a new ArrayNode from offset to limit items
     *
     * @param array
     * @param offset
     * @param limit
     * @return Sub ArrayNode
     */
    public static ArrayNode getSubArrayNode(ArrayNode array, int offset, int limit) {

        final ArrayNode subResult = createArrayNode();
        int i = 0;
        final Iterator<JsonNode> iterator = array.elements();
        for (; i < offset && iterator.hasNext(); i++) {
            iterator.next();
        }
        for (i = offset; i < offset + limit && iterator.hasNext(); i++) {
            subResult.add(iterator.next());
        }

        return subResult;
    }

    /**
     * transform an {@link ArrayNode} (JSON Array) to an {@link java.util.ArrayList}
     * @param arrayNode {@link ArrayNode} to transform
     * @return list corresponding to the arrayNode in parameter
     */
    public static List toArrayList(ArrayNode arrayNode) {
        return Lists.newArrayList(arrayNode.iterator());
    }
}
