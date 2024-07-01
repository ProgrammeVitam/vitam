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
package fr.gouv.vitam.common.database.server.mongodb;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import org.bson.Document;

import java.util.Map;

public class BsonHelper {

    private static final JsonFactory JSON_FACTORY = new JsonFactory();

    private static final ObjectMapper OBJECT_MAPPER = buildObjectMapper();

    private static final ObjectMapper buildObjectMapper() {
        final ObjectMapper objectMapper = new ObjectMapper(JSON_FACTORY);
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.UPPER_CAMEL_CASE);

        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        // Replace objectMapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, true);
        objectMapper
            .configOverride(Map.class)
            .setIncludeAsProperty(JsonInclude.Value.construct(JsonInclude.Include.ALWAYS, JsonInclude.Include.ALWAYS));
        objectMapper.setSerializationInclusion(JsonInclude.Include.ALWAYS); // Serialize all fields
        objectMapper.disable(SerializationFeature.INDENT_OUTPUT); // Unpretty print

        objectMapper.configure(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS, true);
        objectMapper.configure(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED, false);
        objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        objectMapper.configure(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS, true);
        objectMapper.configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, true);

        return objectMapper;
    }

    /**
     * Stringify mongo document
     *
     * @param document
     * @return Unpretty print object
     */
    public static String stringify(Document document) {
        try {
            return OBJECT_MAPPER.writeValueAsString(document);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Convert bson document to JsonNode
     */
    public static JsonNode fromDocumentToJsonNode(Document object) throws InvalidParseOperationException {
        try {
            return OBJECT_MAPPER.valueToTree(object);
        } catch (IllegalArgumentException e) {
            throw new InvalidParseOperationException(e);
        }
    }

    /**
     * Convert bson document to object
     */
    public static <T> T fromDocumentToObject(Document document, Class<T> clasz) throws InvalidParseOperationException {
        try {
            return OBJECT_MAPPER.convertValue(document, clasz);
        } catch (IllegalArgumentException e) {
            throw new InvalidParseOperationException(e);
        }
    }

    /**
     * Convert bson document to object
     */
    public static <T> T fromDocumentToObject(Document document, TypeReference<T> typeReference)
        throws InvalidParseOperationException {
        try {
            return OBJECT_MAPPER.convertValue(document, typeReference);
        } catch (IllegalArgumentException e) {
            throw new InvalidParseOperationException(e);
        }
    }
}
