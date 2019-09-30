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
package fr.gouv.vitam.metadata.core;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.database.collections.DynamicParserTokens;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.metadata.core.database.collections.ElasticsearchAccessMetadata;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.database.collections.ObjectGroup;
import org.junit.Test;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class ElasticsearchMappingParseTest {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ElasticsearchMappingParseTest.class);

    @Test
    public void testObjectGroupElasticsearchMapping() throws Exception {

        parseAndValidateMappingFile(ElasticsearchAccessMetadata.MAPPING_OBJECT_GROUP_FILE, MetadataCollections.OBJECTGROUP);
    }

    @Test
    public void testUnitElasticsearchMapping() throws Exception {
        parseAndValidateMappingFile(ElasticsearchAccessMetadata.MAPPING_UNIT_FILE, MetadataCollections.UNIT);
    }

    private void parseAndValidateMappingFile(String resourceFileName, MetadataCollections collection) throws InvalidParseOperationException {

        Map<String, String> result = parseMapping(resourceFileName);

        DynamicParserTokens parserTokens = new DynamicParserTokens(collection.getVitamDescriptionResolver(), Collections.emptyList());

        for (Map.Entry<String, String> entry : result.entrySet()) {

            String fieldName = entry.getKey();
            String fieldType = entry.getValue();

            boolean isNotAnalyzed = parserTokens.isNotAnalyzed(fieldName);
            switch (fieldType) {
                case "text":
                    assertThat(isNotAnalyzed)
                        .withFailMessage("Expected isNotAnalyzed=false for key=" + fieldName + " / type=" + fieldType)
                        .isFalse();
                    break;
                case "dateTime":
                case "boolean":
                case "long":
                case "double":
                case "keyword":
                case "notIndexed":
                    assertThat(isNotAnalyzed)
                        .withFailMessage("Expected isNotAnalyzed=true for key=" + fieldName + " / type=" + fieldType)
                        .isTrue();
                    break;
                case "object":
                case "nested":
                    assertThat(parserTokens.isNotAnalyzed(fieldName + ".Any")).isFalse();
                    break;
                default:
                    fail("Unexpected type " + fieldType);
            }
        }

        LOGGER.info("\nParsed mapping file: " + resourceFileName + "\n============================" +
            result.toString().replaceAll(", ", "\n").replaceAll("\\{", "\n").replaceAll("}", "\n"));
    }

    private Map<String, String> parseMapping(String resourceFileName) throws InvalidParseOperationException {
        JsonNode jsonNode =
            JsonHandler.getFromInputStream(ObjectGroup.class.getResourceAsStream(resourceFileName));

        Map<String, String> result = new TreeMap<>();

        String parentMappingPath = "";

        parseAndValidateMappingFile(jsonNode.fields().next().getValue(), parentMappingPath, result);
        return result;
    }

    private void parseAndValidateMappingFile(JsonNode jsonNode, String parentMappingPath, Map<String, String> result) {

        assertThat(jsonNode).isNotNull();

        JsonNode enabled = jsonNode.get("enabled");
        if (enabled != null) {
            assertThat(enabled.asText()).isEqualTo("false");
            result.put(parentMappingPath, "notIndexed");
            return;
        }

        JsonNode type = jsonNode.get("type");
        if (type != null) {
            parseType(jsonNode, parentMappingPath, result, type);
            return;
        }

        JsonNode properties = jsonNode.get("properties");

        if (properties != null) {
            parseProperties(parentMappingPath, result, properties);
            return;
        }

        fail("Expected Type or Properties");
    }

    private void parseType(JsonNode jsonNode, String parentMappingPath, Map<String, String> result, JsonNode type) {
        String typeStr;
        switch (type.asText()) {
            case "date":
                JsonNode format = jsonNode.get("format");
                assertThat(format).isNotNull();

                switch (format.asText()) {
                    case "strict_date_optional_time":
                        typeStr = "dateTime";
                        break;
                    default:
                        throw new IllegalStateException("Unexpected date format " + format.asText());
                }
                break;
            case "keyword":
            case "text":
            case "boolean":
            case "long":
            case "double":
                typeStr = type.asText();
                break;
            case "object":
                typeStr = "object";
                break;
            case "nested":
                typeStr = "nested";
                break;
            default:
                throw new IllegalStateException("Unexpected type " + type.asText());

        }

        result.put(parentMappingPath, typeStr);
    }

    private void parseProperties(String parentMappingPath, Map<String, String> result, JsonNode properties) {
        for (Iterator<Map.Entry<String, JsonNode>> it = properties.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> entry = it.next();

            if (parentMappingPath.isEmpty()) {
                parseAndValidateMappingFile(entry.getValue(), entry.getKey(), result);
            } else {
                parseAndValidateMappingFile(entry.getValue(), parentMappingPath + "." + entry.getKey(), result);
            }
        }
    }
}
