/*
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
package fr.gouv.vitam.common.json;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * SchemaValidationUtils
 */
public class SchemaValidationUtils {

    private static final String PROPERTIES = "properties";

    private static final String ITEMS = "items";

    private static final List<String> SCHEMA_DECLARATION_TYPE = Arrays.asList("$schema",
        "id", "type", "additionalProperties", "anyOf", "required", "description", ITEMS, "title",
        "oneOf", "enum", "minLength", "minItems", PROPERTIES);

    /**
     * Get fields list declared in schema
     *
     * @param schemaJsonAsString schemaJsonAsString
     * @return a the list of fields declared in the schema
     */
    public static List<String> extractFieldsFromSchema(String schemaJsonAsString)
        throws InvalidParseOperationException {
        List<String> listProperties = new ArrayList<>();
        JsonNode externalSchema = JsonHandler.getFromString(schemaJsonAsString);
        if (externalSchema != null && externalSchema.get(PROPERTIES) != null) {
            extractPropertyFromJsonNode(externalSchema.get(PROPERTIES), listProperties);
        }
        return listProperties;

    }

    private static void extractPropertyFromJsonNode(JsonNode currentJson, List<String> listProperties) {
        final Iterator<Map.Entry<String, JsonNode>> iterator = currentJson.fields();
        while (iterator.hasNext()) {
            final Map.Entry<String, JsonNode> entry = iterator.next();
            String key = entry.getKey();
            JsonNode value = entry.getValue();
            if (value == null) {
                continue;
            }
            if (value.isObject() || value.isArray()) {
                // if subproperties
                extractPropertyFromJsonNode(value, listProperties);
            }

            if (!SCHEMA_DECLARATION_TYPE.contains(key) && value.isObject() && value.get(PROPERTIES) == null &&
                (value.get(ITEMS) == null || (value.get(ITEMS) != null && value.get(ITEMS).get(PROPERTIES) == null)) &&
                !listProperties.contains(key)) {
                listProperties.add(key);
            }
        }
    }
}
