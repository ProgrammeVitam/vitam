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
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.dsl.schema.meta.Format;
import fr.gouv.vitam.common.dsl.schema.meta.Schema;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.fail;

/**
 * Tests of get by id query schema for Metadata, Masterdata and logbook DSL query
 */
public class ValidatorGetByIdQueryTest {

    private static final String GET_BY_ID_QUERY_DSL_SCHEMA_JSON = "get-by-id-query-dsl-schema.json";

    private Schema loadSchema(File dslSource) throws IOException {
        try (InputStream inputStream = new FileInputStream(dslSource)) {
            final Schema schema = Schema.getSchema().loadTypes(inputStream).build();
            Format dslType = schema.getType("DSL");
            System.out.println(dslType.toString());
            Format projectionType = schema.getType("PROJECTION");
            System.out.println(projectionType.toString());
            return schema;
        }
    }

    @Test
    public void should_not_retrieve_errors_when_get_by_id_complete_dsl()
        throws IOException, InvalidParseOperationException {
        JsonNode test1Json = JsonHandler.getFromFile(PropertiesUtils.getResourceFile("get_by_id_complete.json"));
        final Schema schema = loadSchema(PropertiesUtils.getResourceFile(GET_BY_ID_QUERY_DSL_SCHEMA_JSON));
        try {
            Validator.validate(schema, "DSL", test1Json);
        } catch (ValidationException e) {
            fail();
        }
    }

    @Test
    public void should_not_retrieve_errors_when_get_by_id_empty_projection_dsl()
        throws IOException, InvalidParseOperationException {
        JsonNode test1Json = JsonHandler.getFromFile(
            PropertiesUtils.getResourceFile("get_by_id_empty_projection.json")
        );
        final Schema schema = loadSchema(PropertiesUtils.getResourceFile(GET_BY_ID_QUERY_DSL_SCHEMA_JSON));
        try {
            Validator.validate(schema, "DSL", test1Json);
        } catch (ValidationException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void should_retrieve_errors_when_get_by_id_no_projection_dsl()
        throws IOException, InvalidParseOperationException {
        JsonNode test1Json = JsonHandler.getFromFile(PropertiesUtils.getResourceFile("get_by_id_no_projection.json"));
        final Schema schema = loadSchema(PropertiesUtils.getResourceFile(GET_BY_ID_QUERY_DSL_SCHEMA_JSON));
        assertThatThrownBy(() -> Validator.validate(schema, "DSL", test1Json)).hasMessageMatching(
            ".*Validating \\$projection: .* ~ MANDATORY.*"
        );
    }

    @Test
    public void should_retrieve_errors_when_get_by_id_query_dsl() throws IOException, InvalidParseOperationException {
        JsonNode test1Json = JsonHandler.getFromFile(PropertiesUtils.getResourceFile("get_by_id_query.json"));
        final Schema schema = loadSchema(PropertiesUtils.getResourceFile(GET_BY_ID_QUERY_DSL_SCHEMA_JSON));
        assertThatThrownBy(() -> Validator.validate(schema, "DSL", test1Json)).hasMessageContaining(
            "INVALID_JSON_FIELD: $query ~ found json: {} ~ path: []"
        );
    }

    @Test
    public void should_retrieve_errors_when_unknown_key_is_present_in_get_by_id() throws Exception {
        JsonNode test1Json = JsonHandler.getFromFile(PropertiesUtils.getResourceFile("get_by_id_unknown_key.json"));
        final Schema schema = loadSchema(PropertiesUtils.getResourceFile(GET_BY_ID_QUERY_DSL_SCHEMA_JSON));
        assertThatThrownBy(() -> Validator.validate(schema, "DSL", test1Json))
            .hasMessageContaining("INVALID_JSON_FIELD: $unknownKey ~ found json: \\\"novalidation\\\" ~ path: []")
            .hasMessageContaining("INVALID_JSON_FIELD: $unknown2 ~ found json: {} ~ path: []");
    }
}
