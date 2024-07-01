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
package fr.gouv.vitam.model.validation.jsonschema;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

public class JsonSchemaFieldParser {

    public static Map<String, JsonSchemaField> parseJsonSchemaFields(InputStream jsonSchemaInputStream)
        throws InvalidParseOperationException {
        JsonNode externalSchema = JsonHandler.getFromInputStream(jsonSchemaInputStream);

        if (externalSchema.has("definitions")) {
            for (Iterator<String> it = externalSchema.get("definitions").fieldNames(); it.hasNext();) {
                String fieldName = it.next();
                if (!fieldName.equals("date-opt-time") && !fieldName.equals("nullable-date-opt-time")) {
                    throw new IllegalStateException("Unknown definition type " + fieldName);
                }
            }
        }

        List<JsonSchemaField> schemaFields = new ArrayList<>();
        processObject(externalSchema, "", schemaFields);
        return schemaFields.stream().collect(toMap(JsonSchemaField::getFullPath, e -> e));
    }

    private static void processObject(JsonNode objectDefinition, String basePath, List<JsonSchemaField> schemaFields) {
        if (!objectDefinition.has("additionalProperties") || objectDefinition.get("additionalProperties").asBoolean()) {
            throw new IllegalStateException("Expected additionalProperties=false node for " + basePath);
        }

        if (!objectDefinition.has("properties")) {
            throw new IllegalStateException("Expected properties node for " + basePath);
        }

        processProperties(objectDefinition.get("properties"), basePath, schemaFields);
    }

    private static void processProperties(JsonNode currentJson, String basePath, List<JsonSchemaField> schemaFields) {
        final Iterator<Map.Entry<String, JsonNode>> iterator = currentJson.fields();

        while (iterator.hasNext()) {
            final Map.Entry<String, JsonNode> entry = iterator.next();
            String key = entry.getKey();
            JsonNode value = entry.getValue();
            String fullPath = basePath.isEmpty() ? key : basePath + "." + key;

            processType(value, fullPath, schemaFields, false);
        }
    }

    private static void processType(
        JsonNode value,
        String fullPath,
        List<JsonSchemaField> schemaFields,
        boolean isArray
    ) {
        if (value.has("type") && value.get("type").textValue().equals("array")) {
            if (isArray) {
                throw new IllegalStateException("Arrays of arrays not supported for " + fullPath);
            }
            processType(value.get("items"), fullPath, schemaFields, true);
            return;
        }

        JsonSchemaFieldType jsonSchemaFieldType;
        if (value.has("$ref")) {
            if (
                !value.get("$ref").textValue().equals("#/definitions/date-opt-time") &&
                !value.get("$ref").textValue().equals("#/definitions/nullable-date-opt-time")
            ) {
                throw new IllegalStateException("Invalid reference $ref " + value.get("$ref"));
            }
            jsonSchemaFieldType = JsonSchemaFieldType.DATE;
        } else if (value.has("enum")) {
            jsonSchemaFieldType = JsonSchemaFieldType.ENUM;
        } else if (value.has("type")) {
            switch (value.get("type").textValue()) {
                case "object": {
                    jsonSchemaFieldType = JsonSchemaFieldType.OBJECT;
                    processObject(value, fullPath, schemaFields);
                    break;
                }
                case "string": {
                    jsonSchemaFieldType = JsonSchemaFieldType.STRING;
                    break;
                }
                case "boolean": {
                    jsonSchemaFieldType = JsonSchemaFieldType.BOOLEAN;
                    break;
                }
                case "integer": {
                    jsonSchemaFieldType = JsonSchemaFieldType.INTEGER;
                    break;
                }
                case "number": {
                    jsonSchemaFieldType = JsonSchemaFieldType.NUMERIC;
                    break;
                }
                default:
                    throw new IllegalStateException("Unexpected value: " + value.get("type").textValue());
            }
        } else {
            throw new IllegalStateException("Expected enum or type or $ref at " + fullPath);
        }
        schemaFields.add(new JsonSchemaField(fullPath, jsonSchemaFieldType, isArray));
    }
}
