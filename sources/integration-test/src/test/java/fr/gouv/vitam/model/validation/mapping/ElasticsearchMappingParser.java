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
package fr.gouv.vitam.model.validation.mapping;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class ElasticsearchMappingParser {

    public Map<String, ElasticsearchMappingType> parseMapping(JsonNode jsonNode) {
        Map<String, ElasticsearchMappingType> result = new TreeMap<>();
        parseAndValidateMappingFile(jsonNode, "", result);
        return result;
    }

    private void parseAndValidateMappingFile(
        JsonNode jsonNode,
        String parentMappingPath,
        Map<String, ElasticsearchMappingType> result
    ) {
        JsonNode enabled = jsonNode.get("enabled");
        if (enabled != null) {
            if (!enabled.asText().equals("false")) {
                throw new IllegalStateException("Expected enable to be false when specified");
            }
            result.put(parentMappingPath, ElasticsearchMappingType.NOT_INDEXED);
            return;
        }

        JsonNode dynamicTemplates = jsonNode.get("dynamic_templates");
        if (dynamicTemplates != null) {
            parseDynamicTemplates(dynamicTemplates);
        }

        JsonNode type = jsonNode.get("type");
        JsonNode properties = jsonNode.get("properties");

        if (!parentMappingPath.isEmpty() && properties != null && type == null) {
            type = new TextNode("object");
        }
        if (type != null) {
            parseType(jsonNode, parentMappingPath, result, type);
        }

        if (properties != null) {
            parseProperties(parentMappingPath, result, properties);
        }

        if (type == null && properties == null) {
            throw new IllegalStateException("Expected Type or Properties " + parentMappingPath);
        }
    }

    private void parseDynamicTemplates(JsonNode dynamicTemplates) {
        if (!dynamicTemplates.isArray()) {
            throw new IllegalStateException("dynamic templates should be an array");
        }

        for (JsonNode template : dynamicTemplates) {
            if (template.size() > 1) {
                throw new IllegalStateException("dynamic template should have only a name field");
            }

            JsonNode templateProperties = template.iterator().next();
            JsonNode mapping = templateProperties.get("mapping");
            if (mapping == null) {
                throw new IllegalStateException("dynamic template does not have a valid mapping");
            }

            String[] availableMatchConditions = {
                "match_mapping_type",
                "match",
                "match_pattern",
                "unmatch",
                "path_match",
                "path_unmatch",
            };
            if (((ObjectNode) templateProperties).retain(availableMatchConditions).size() == 0) {
                throw new IllegalStateException("dynamic template does not have a valid match condition");
            }
        }
    }

    private void parseType(
        JsonNode jsonNode,
        String parentMappingPath,
        Map<String, ElasticsearchMappingType> result,
        JsonNode typeNode
    ) {
        ElasticsearchMappingType mappingType;
        switch (typeNode.asText()) {
            case "date":
                JsonNode format = jsonNode.get("format");

                if (!"strict_date_optional_time".equals(format.asText())) {
                    throw new IllegalStateException("Unexpected date format " + format.asText());
                }

                mappingType = ElasticsearchMappingType.DATETIME;
                break;
            case "keyword":
                mappingType = ElasticsearchMappingType.KEYWORD;
                break;
            case "text":
                mappingType = ElasticsearchMappingType.TEXT;
                break;
            case "boolean":
                mappingType = ElasticsearchMappingType.BOOLEAN;
                break;
            case "long":
                mappingType = ElasticsearchMappingType.LONG;
                break;
            case "double":
                mappingType = ElasticsearchMappingType.DOUBLE;
                break;
            case "object":
                mappingType = ElasticsearchMappingType.OBJECT;
                break;
            case "nested":
                mappingType = ElasticsearchMappingType.NESTED_OBJECT;
                break;
            default:
                throw new IllegalStateException("Unexpected type " + typeNode.asText());
        }

        result.put(parentMappingPath, mappingType);
    }

    private void parseProperties(
        String parentMappingPath,
        Map<String, ElasticsearchMappingType> result,
        JsonNode properties
    ) {
        for (Iterator<Map.Entry<String, JsonNode>> it = properties.fields(); it.hasNext();) {
            Map.Entry<String, JsonNode> entry = it.next();

            if ("dynamic_templates".equals(entry.getKey())) {
                parseDynamicTemplates(entry.getValue());
            } else if (parentMappingPath.isEmpty()) {
                parseAndValidateMappingFile(entry.getValue(), entry.getKey(), result);
            } else {
                parseAndValidateMappingFile(entry.getValue(), parentMappingPath + "." + entry.getKey(), result);
            }
        }
    }
}
