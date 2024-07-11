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
package fr.gouv.vitam.metadata.core.validation;

import com.amazonaws.util.CollectionUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import fr.gouv.vitam.common.CharsetUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.OntologyLoader;
import fr.gouv.vitam.common.model.administration.OntologyModel;
import org.apache.commons.lang3.BooleanUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;

public class OntologyValidator {

    private static final DateTimeFormatter XSD_DATATYPE_DATE_FORMATTER = new DateTimeFormatterBuilder()
        .appendOptional(
            DateTimeFormatter.ofPattern("u-MM-dd['T'HH:mm:ss[.SSSSSS][.SSSSS][.SSSS][.SSS][.SS][.S][xxx][X]][xxx][X]")
        )
        .appendOptional(
            DateTimeFormatter.ofPattern("u/MM/dd['T'HH:mm:ss[.SSSSSS][.SSSSS][.SSSS][.SSS][.SS][.S][xxx][X]][xxx][X]")
        )
        .toFormatter();

    private static float MAX_UTF8_BYTES_PER_CHAR = StandardCharsets.UTF_8.newEncoder().maxBytesPerChar();

    private final OntologyLoader ontologyLoader;

    public OntologyValidator(OntologyLoader ontologyLoader) {
        this.ontologyLoader = ontologyLoader;
    }

    /**
     * Verify and replace fields.
     *
     * @param jsonNode to modify
     * @return error list
     */
    public ObjectNode verifyAndReplaceFields(JsonNode jsonNode) throws MetadataValidationException {
        ObjectNode transformedJsonNode = jsonNode.deepCopy();

        List<OntologyModel> ontologyModels = ontologyLoader.loadOntologies();
        Map<String, OntologyModel> ontologyModelMap = ontologyModels
            .stream()
            .collect(Collectors.toMap(OntologyModel::getIdentifier, oM -> oM));

        List<String> errors = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> iterator = transformedJsonNode.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            verifyLine(transformedJsonNode, ontologyModelMap, errors, entry);
        }

        if (!errors.isEmpty()) {
            String error =
                "metadata contains fields declared in ontology with a wrong format : " +
                CollectionUtils.join(errors, ",");
            throw new MetadataValidationException(MetadataValidationErrorCode.ONTOLOGY_VALIDATION_FAILURE, error);
        }

        return transformedJsonNode;
    }

    private void verifyAndReplaceFields(
        JsonNode node,
        Map<String, OntologyModel> ontologyModelMap,
        List<String> errors
    ) {
        Iterator<Map.Entry<String, JsonNode>> iterator = node.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            verifyLine(node, ontologyModelMap, errors, entry);
        }
    }

    private void verifyLine(
        JsonNode node,
        Map<String, OntologyModel> ontologyModelMap,
        List<String> errors,
        Map.Entry<String, JsonNode> entry
    ) {
        String fieldName = entry.getKey();

        OntologyModel ontology = ontologyModelMap.get(fieldName);
        JsonNode fieldValue = entry.getValue();
        if (fieldValue == null || fieldValue.isMissingNode()) {
            return;
        }

        if (fieldValue.isObject()) {
            verifyAndReplaceFields(fieldValue, ontologyModelMap, errors);

            return;
        }
        if (fieldValue.isArray()) {
            for (int i = 0; i < fieldValue.size(); i++) {
                JsonNode jn = fieldValue.get(i);
                verifyField(ontologyModelMap, errors, fieldName, ontology, (ArrayNode) fieldValue, i, jn);
            }
            return;
        }
        errors.addAll(replacePropertyField(fieldValue, ontology, node, fieldName));
    }

    private void verifyField(
        Map<String, OntologyModel> ontologyModelMap,
        List<String> errors,
        String fieldName,
        OntologyModel ontology,
        ArrayNode fieldValue,
        int i,
        JsonNode jn
    ) {
        if (null == jn || null == jn.asText()) {
            return;
        }

        if (jn.isObject() || jn.isArray()) {
            verifyAndReplaceFields(jn, ontologyModelMap, errors);
            return;
        }
        try {
            fieldValue.set(i, checkFieldLengthAndForceFieldTyping(fieldName, jn, ontology));
        } catch (IllegalArgumentException | DateTimeParseException e) {
            errors.add(String.format("Error '%s' on field '%s'.", e.getMessage(), fieldName));
        }
    }

    /**
     * Example of fieldContainer:  fieldContainer = "{'fieldName': 'fieldValue'}"
     *
     * @param fieldValue fieldValue
     * @param ontology ontology
     * @param fieldContainer fieldContainer
     * @param fieldName fieldName
     * @return List
     */
    private List<String> replacePropertyField(
        JsonNode fieldValue,
        OntologyModel ontology,
        JsonNode fieldContainer,
        String fieldName
    ) {
        ObjectNode objectNodeContainer = (ObjectNode) fieldContainer;
        if (!fieldValue.isNull() && !fieldValue.isMissingNode()) {
            try {
                objectNodeContainer.set(
                    fieldName,
                    checkFieldLengthAndForceFieldTyping(fieldName, fieldValue, ontology)
                );
            } catch (IllegalArgumentException | DateTimeParseException e) {
                return Collections.singletonList(
                    String.format("Error: <%s> on field '%s'.", e.getMessage(), fieldName)
                );
            }
        }
        return Collections.emptyList();
    }

    private JsonNode checkFieldLengthAndForceFieldTyping(
        String fieldName,
        JsonNode fieldValueNode,
        OntologyModel ontologyModel
    ) {
        // In case where no ontology is provided
        if (null == ontologyModel) {
            return validateValueLength(
                fieldName,
                fieldValueNode,
                VitamConfiguration.getTextMaxLength(),
                "Not accepted value for the text field (%s) whose UTF8 encoding is longer than the max length " +
                VitamConfiguration.getTextMaxLength()
            );
        }

        switch (ontologyModel.getType()) {
            case TEXT:
                return validateValueLength(
                    fieldName,
                    fieldValueNode,
                    VitamConfiguration.getTextMaxLength(),
                    "Not accepted value for the text field (%s) whose UTF8 encoding is longer than the max length " +
                    VitamConfiguration.getTextMaxLength()
                );
            case GEO_POINT:
            case ENUM:
            // FIXME : Useless?
            case KEYWORD:
                return validateValueLength(
                    fieldName,
                    fieldValueNode,
                    VitamConfiguration.getKeywordMaxLength(),
                    "Not accepted value for the Keyword field (%s) whose UTF8 encoding is longer than the max length " +
                    VitamConfiguration.getTextMaxLength()
                );
            case DOUBLE:
                if (fieldValueNode.isDouble()) {
                    return fieldValueNode;
                }
                return new DoubleNode(Double.parseDouble(fieldValueNode.asText()));
            case DATE:
                return new TextNode(mapDateToOntology(fieldValueNode.asText()));
            case LONG:
                if (fieldValueNode.isLong()) {
                    return fieldValueNode;
                }
                return new LongNode(Long.parseLong(fieldValueNode.asText()));
            case BOOLEAN:
                if (fieldValueNode.isBoolean()) {
                    return fieldValueNode;
                }
                return BooleanNode.valueOf(
                    BooleanUtils.toBoolean(fieldValueNode.asText().toLowerCase(), "true", "false")
                );
            default:
                throw new IllegalStateException(String.format("Not implemented for type %s", fieldValueNode.asText()));
        }
    }

    /**
     * Check if value length is accepted or is longer than expected
     *
     * @param fieldName fieldName
     * @param fieldValueNode fieldValueNode
     * @param maxUtf8Length maxUtf8Length
     * @param message message
     * @return JsonNode
     */
    private JsonNode validateValueLength(String fieldName, JsonNode fieldValueNode, int maxUtf8Length, String message) {
        if (null == fieldValueNode) {
            return null;
        }
        String textValue = fieldValueNode.asText();

        if (stringExceedsMaxLuceneUtf8StorageSize(textValue, maxUtf8Length)) {
            throw new IllegalArgumentException(String.format(message, fieldName));
        }

        if (fieldValueNode.isTextual()) {
            return fieldValueNode;
        }
        return new TextNode(textValue);
    }

    /**
     * ES stores data in lucene indexes that have 32Kb max limit per token.
     */
    public static boolean stringExceedsMaxLuceneUtf8StorageSize(String textValue, int maxUtf8Length) {
        /*
         * Optimisation : most string values are small, and will never exceed max storage length.
         * We'll check if max string length (in chars) can never exceed max storage length in UTF-8.
         * UTF-8 encoding is a var-length encoding. Some chars (eg. a-z0-9) will be encoded as 1 byte,
         * some will be encoded as 2 bytes, and some as 3 bytes... A char cannot exceed
         * StandardCharsets.UTF_8.newEncoder().maxBytesPerChar() = 3.
         * if a string value has 100 chars, it will NEVER exceed 300 bytes on disk
         */
        if (textValue.length() * MAX_UTF8_BYTES_PER_CHAR < maxUtf8Length) {
            return false;
        }

        return textValue.getBytes(CharsetUtils.UTF8).length >= maxUtf8Length;
    }

    private String mapDateToOntology(String field) {
        TemporalAccessor parse = XSD_DATATYPE_DATE_FORMATTER.parse(field);
        return parse.isSupported(HOUR_OF_DAY)
            ? parse.query(LocalDateTime::from).format(ISO_LOCAL_DATE_TIME)
            : parse.query(LocalDate::from).format(ISO_LOCAL_DATE);
    }
}
