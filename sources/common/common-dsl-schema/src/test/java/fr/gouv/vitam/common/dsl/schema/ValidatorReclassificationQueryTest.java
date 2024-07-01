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
package fr.gouv.vitam.common.dsl.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.dsl.schema.meta.Format;
import fr.gouv.vitam.common.dsl.schema.meta.Schema;
import fr.gouv.vitam.common.json.JsonHandler;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests of reclassification query schema validator
 */
public class ValidatorReclassificationQueryTest {

    private static final String RECLASSIFICATION_QUERY_DSL_SCHEMA_JSON = "reclassification-query-dsl-schema.json";

    private Schema loadSchema(ObjectMapper objectMapper, File dslSource) throws Exception {
        try (InputStream inputStream = new FileInputStream(dslSource)) {
            final Schema schema = Schema.getSchema().loadTypes(inputStream).build();
            Format dslType = schema.getType("DSL");
            System.out.println(dslType.toString());
            Format actionType = schema.getType("ACTION");
            System.out.println(actionType.toString());
            return schema;
        }
    }

    @Test
    public void testValidate_OK_complete() throws Exception {
        JsonNode test1Json = JsonHandler.getFromFile(
            PropertiesUtils.getResourceFile("reclassification_query_complete.json")
        );
        final Schema schema = loadSchema(
            new ObjectMapper(),
            PropertiesUtils.getResourceFile(RECLASSIFICATION_QUERY_DSL_SCHEMA_JSON)
        );
        Validator.validate(schema, "DSL", test1Json);
    }

    @Test
    public void testValidate_AttachmentOK() throws Exception {
        JsonNode test1Json = JsonHandler.getFromFile(
            PropertiesUtils.getResourceFile("reclassification_query_ok_attachment_only.json")
        );
        final Schema schema = loadSchema(
            new ObjectMapper(),
            PropertiesUtils.getResourceFile(RECLASSIFICATION_QUERY_DSL_SCHEMA_JSON)
        );
        Validator.validate(schema, "DSL", test1Json);
    }

    @Test
    public void testValidate_DetachmentOK() throws Exception {
        JsonNode test1Json = JsonHandler.getFromFile(
            PropertiesUtils.getResourceFile("reclassification_query_ok_detachment_only.json")
        );
        final Schema schema = loadSchema(
            new ObjectMapper(),
            PropertiesUtils.getResourceFile(RECLASSIFICATION_QUERY_DSL_SCHEMA_JSON)
        );
        Validator.validate(schema, "DSL", test1Json);
    }

    @Test
    public void testValidate_KoEmpty() throws Exception {
        JsonNode test1Json = JsonHandler.getFromFile(
            PropertiesUtils.getResourceFile("reclassification_query_ko_empty.json")
        );
        final Schema schema = loadSchema(
            new ObjectMapper(),
            PropertiesUtils.getResourceFile(RECLASSIFICATION_QUERY_DSL_SCHEMA_JSON)
        );
        assertThatThrownBy(() -> Validator.validate(schema, "DSL", test1Json)).hasMessageContaining(
            "{$roots: ..., $query: ..., $action: ...}[] ~ ELEMENT_TOO_SHORT: 0 < 1"
        );
    }

    @Test
    public void testValidate_KoInvalidAction() throws Exception {
        JsonNode test1Json = JsonHandler.getFromFile(
            PropertiesUtils.getResourceFile("reclassification_query_ko_invalid_action.json")
        );
        final Schema schema = loadSchema(
            new ObjectMapper(),
            PropertiesUtils.getResourceFile(RECLASSIFICATION_QUERY_DSL_SCHEMA_JSON)
        );
        assertThatThrownBy(() -> Validator.validate(schema, "DSL", test1Json)).hasMessageContaining(
            "INVALID_JSON_FIELD: $unkown"
        );
    }

    @Test
    public void testValidate_KoEmptyAction() throws Exception {
        JsonNode test1Json = JsonHandler.getFromFile(
            PropertiesUtils.getResourceFile("reclassification_query_ko_empty_action.json")
        );
        final Schema schema = loadSchema(
            new ObjectMapper(),
            PropertiesUtils.getResourceFile(RECLASSIFICATION_QUERY_DSL_SCHEMA_JSON)
        );
        assertThatThrownBy(() -> Validator.validate(schema, "DSL", test1Json)).hasMessageContaining(
            "ELEMENT_TOO_SHORT: 0 < 1"
        );
    }

    @Test
    public void testValidate_KoInvalidAddFieldName() throws Exception {
        JsonNode test1Json = JsonHandler.getFromFile(
            PropertiesUtils.getResourceFile("reclassification_query_ko_invalid_add_field_name.json")
        );
        final Schema schema = loadSchema(
            new ObjectMapper(),
            PropertiesUtils.getResourceFile(RECLASSIFICATION_QUERY_DSL_SCHEMA_JSON)
        );
        assertThatThrownBy(() -> Validator.validate(schema, "DSL", test1Json)).hasMessageContaining(
            "INVALID_JSON_FIELD: Invalid"
        );
    }

    @Test
    public void testValidate_KoInvalidPullFieldName() throws Exception {
        JsonNode test1Json = JsonHandler.getFromFile(
            PropertiesUtils.getResourceFile("reclassification_query_ko_invalid_pull_field_name.json")
        );
        final Schema schema = loadSchema(
            new ObjectMapper(),
            PropertiesUtils.getResourceFile(RECLASSIFICATION_QUERY_DSL_SCHEMA_JSON)
        );
        assertThatThrownBy(() -> Validator.validate(schema, "DSL", test1Json)).hasMessageContaining(
            "INVALID_JSON_FIELD: Invalid"
        );
    }

    @Test
    public void testValidate_KoMissingQuery() throws Exception {
        JsonNode test1Json = JsonHandler.getFromFile(
            PropertiesUtils.getResourceFile("reclassification_query_ko_missing_query.json")
        );
        final Schema schema = loadSchema(
            new ObjectMapper(),
            PropertiesUtils.getResourceFile(RECLASSIFICATION_QUERY_DSL_SCHEMA_JSON)
        );
        assertThatThrownBy(() -> Validator.validate(schema, "DSL", test1Json)).hasMessageContaining(
            "ELEMENT_TOO_SHORT: 0 < 1"
        );
    }

    @Test
    public void testValidate_KoMultiQueries() throws Exception {
        JsonNode test1Json = JsonHandler.getFromFile(
            PropertiesUtils.getResourceFile("reclassification_query_ko_multiqueries.json")
        );
        final Schema schema = loadSchema(
            new ObjectMapper(),
            PropertiesUtils.getResourceFile(RECLASSIFICATION_QUERY_DSL_SCHEMA_JSON)
        );
        assertThatThrownBy(() -> Validator.validate(schema, "DSL", test1Json)).hasMessageContaining(
            "ELEMENT_TOO_LONG: 2 > 1"
        );
    }
}
