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
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.fail;

/**
 * Tests of multi select query schema for Metadata DSL query
 */
public class ValidatorSelectQueryMultipleTest {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ValidatorSelectQueryMultipleTest.class);
    private static final String SELECT_QUERY_MULTIPLE_DSL_SCHEMA_JSON = "select-query-multiple-dsl-schema.json";

    private Schema loadSchema(ObjectMapper objectMapper, File dslSource)
        throws IOException, InvalidParseOperationException {
        try (InputStream inputStream = new FileInputStream(dslSource)) {
            final Schema schema = Schema.getSchema().loadTypes(inputStream).build();
            Format dslType = schema.getType("DSL");
            LOGGER.debug(dslType.toString());
            Format queryType = schema.getType("QUERY");
            LOGGER.debug(queryType.toString());
            Format filterType = schema.getType("FILTER");
            LOGGER.debug(filterType.toString());
            Format projectionType = schema.getType("PROJECTION");
            LOGGER.debug(projectionType.toString());
            Format facetType = schema.getType("FACET");
            LOGGER.debug(facetType.toString());
            return schema;
        }
    }

    @Test
    public void should_not_retrieve_errors_when_select_multiple_complete_dsl()
        throws IOException, InvalidParseOperationException {
        JsonNode test1Json = JsonHandler.getFromFile(PropertiesUtils.getResourceFile("select_multiple_complete.json"));
        final Schema schema = loadSchema(
            new ObjectMapper(),
            PropertiesUtils.getResourceFile(SELECT_QUERY_MULTIPLE_DSL_SCHEMA_JSON)
        );
        try {
            Validator.validate(schema, "DSL", test1Json);
        } catch (ValidationException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void should_not_retrieve_errors_when_select_multiple_empty_root_dsl()
        throws IOException, InvalidParseOperationException {
        JsonNode test1Json = JsonHandler.getFromFile(
            PropertiesUtils.getResourceFile("select_multiple_empty_root.json")
        );
        final Schema schema = loadSchema(
            new ObjectMapper(),
            PropertiesUtils.getResourceFile(SELECT_QUERY_MULTIPLE_DSL_SCHEMA_JSON)
        );
        try {
            Validator.validate(schema, "DSL", test1Json);
        } catch (ValidationException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void should_not_retrieve_errors_when_select_multiple_no_root_dsl()
        throws IOException, InvalidParseOperationException {
        JsonNode test1Json = JsonHandler.getFromFile(PropertiesUtils.getResourceFile("select_multiple_no_root.json"));
        final Schema schema = loadSchema(
            new ObjectMapper(),
            PropertiesUtils.getResourceFile(SELECT_QUERY_MULTIPLE_DSL_SCHEMA_JSON)
        );
        try {
            Validator.validate(schema, "DSL", test1Json);
        } catch (ValidationException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void should_not_retrieve_errors_when_select_multiple_empty_query_dsl()
        throws IOException, InvalidParseOperationException {
        JsonNode test1Json = JsonHandler.getFromFile(
            PropertiesUtils.getResourceFile("select_multiple_root_empty_query.json")
        );
        final Schema schema = loadSchema(
            new ObjectMapper(),
            PropertiesUtils.getResourceFile(SELECT_QUERY_MULTIPLE_DSL_SCHEMA_JSON)
        );
        try {
            Validator.validate(schema, "DSL", test1Json);
        } catch (ValidationException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void should_retrieve_errors_when_select_multiple_no_query_dsl()
        throws IOException, InvalidParseOperationException {
        JsonNode test1Json = JsonHandler.getFromFile(PropertiesUtils.getResourceFile("select_multiple_no_query.json"));
        final Schema schema = loadSchema(
            new ObjectMapper(),
            PropertiesUtils.getResourceFile(SELECT_QUERY_MULTIPLE_DSL_SCHEMA_JSON)
        );
        assertThatThrownBy(() -> Validator.validate(schema, "DSL", test1Json)).hasMessageMatching(
            ".*Validating \\$query: .*\\[\\] ~ MANDATORY.*"
        );
    }

    @Test
    public void should_retrieve_errors_when_select_multiple_wrong_root_dsl()
        throws IOException, InvalidParseOperationException {
        JsonNode test1Json = JsonHandler.getFromFile(
            PropertiesUtils.getResourceFile("select_multiple_wrong_root.json")
        );
        final Schema schema = loadSchema(
            new ObjectMapper(),
            PropertiesUtils.getResourceFile(SELECT_QUERY_MULTIPLE_DSL_SCHEMA_JSON)
        );
        assertThatThrownBy(() -> Validator.validate(schema, "DSL", test1Json)).hasMessageContaining(
            "$roots: guid[] ~ INVALID_VALUE: STRING"
        );
    }

    @Test
    public void should_retrieve_errors_when_select_multiple_wrong_query_dsl()
        throws IOException, InvalidParseOperationException {
        JsonNode test1Json = JsonHandler.getFromFile(
            PropertiesUtils.getResourceFile("select_multiple_query_not_array.json")
        );
        final Schema schema = loadSchema(
            new ObjectMapper(),
            PropertiesUtils.getResourceFile(SELECT_QUERY_MULTIPLE_DSL_SCHEMA_JSON)
        );
        assertThatThrownBy(() -> Validator.validate(schema, "DSL", test1Json))
            .hasMessageContaining("$query:")
            .hasMessageContaining("...} & {$depth: ...}[] ~ WRONG_JSON_TYPE: OBJECT")
            .hasMessageContaining("...} & {$depth: ...}[] ~ INVALID_JSON_FIELD")
            .hasMessageContaining("...} & {$depth: ...}[] ~ INVALID_JSON_FIELD");
    }

    @Test
    public void should_not_retrieve_errors_when_select_empty_query_dsl()
        throws IOException, InvalidParseOperationException {
        JsonNode test1Json = JsonHandler.getFromFile(
            PropertiesUtils.getResourceFile("select_multiple_empty_query.json")
        );
        final Schema schema = loadSchema(
            new ObjectMapper(),
            PropertiesUtils.getResourceFile(SELECT_QUERY_MULTIPLE_DSL_SCHEMA_JSON)
        );
        try {
            Validator.validate(schema, "DSL", test1Json);
        } catch (ValidationException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void should_retrieve_errors_when_select_multiple_unknown_key_in_query_dsl() throws Exception {
        JsonNode test1Json = JsonHandler.getFromFile(
            PropertiesUtils.getResourceFile("select_multiple_unknown_key.json")
        );
        final Schema schema = loadSchema(
            new ObjectMapper(),
            PropertiesUtils.getResourceFile(SELECT_QUERY_MULTIPLE_DSL_SCHEMA_JSON)
        );

        assertThatThrownBy(() -> Validator.validate(schema, "DSL", test1Json)).hasMessageContaining(
            "INVALID_JSON_FIELD: unknown ~ found json: \\\"no_validation\\\" ~ path: []\\n\""
        );
    }

    @Test
    public void should_not_retrieve_errors_when_select_multiple_complete_facet()
        throws IOException, InvalidParseOperationException {
        JsonNode test1Json = JsonHandler.getFromFile(
            PropertiesUtils.getResourceFile("select_multiple_complete_facet.json")
        );
        final Schema schema = loadSchema(
            new ObjectMapper(),
            PropertiesUtils.getResourceFile(SELECT_QUERY_MULTIPLE_DSL_SCHEMA_JSON)
        );
        assertThatCode(() -> Validator.validate(schema, "DSL", test1Json)).doesNotThrowAnyException();
    }

    @Test
    public void should_retrieve_errors_when_select_multiple_complete_facet_no_name_dsl() throws Exception {
        JsonNode test1Json = JsonHandler.getFromFile(
            PropertiesUtils.getResourceFile("select_multiple_complete_facet_no_name.json")
        );
        final Schema schema = loadSchema(
            new ObjectMapper(),
            PropertiesUtils.getResourceFile(SELECT_QUERY_MULTIPLE_DSL_SCHEMA_JSON)
        );

        assertThatThrownBy(() -> Validator.validate(schema, "DSL", test1Json))
            .hasMessageContaining("$facets")
            .hasMessageContaining("ELEMENT_TOO_SHORT")
            .hasMessageContaining("$name: string ~ MANDATORY");
    }

    @Test
    public void should_retrieve_errors_when_select_multiple_complete_facet_no_facet_spec_dsl() throws Exception {
        JsonNode test1Json = JsonHandler.getFromFile(
            PropertiesUtils.getResourceFile("select_multiple_complete_facet_no_facet_spec.json")
        );
        final Schema schema = loadSchema(
            new ObjectMapper(),
            PropertiesUtils.getResourceFile(SELECT_QUERY_MULTIPLE_DSL_SCHEMA_JSON)
        );

        assertThatThrownBy(() -> Validator.validate(schema, "DSL", test1Json))
            .hasMessageContaining("$facets")
            .hasMessageContaining("ELEMENT_TOO_SHORT");
    }

    @Test
    public void should_not_retrieve_errors_when_select_multiple_complete_facet_empty() throws Exception {
        JsonNode test1Json = JsonHandler.getFromFile(
            PropertiesUtils.getResourceFile("select_multiple_complete_facet_empty.json")
        );
        final Schema schema = loadSchema(
            new ObjectMapper(),
            PropertiesUtils.getResourceFile(SELECT_QUERY_MULTIPLE_DSL_SCHEMA_JSON)
        );

        assertThatCode(() -> Validator.validate(schema, "DSL", test1Json)).doesNotThrowAnyException();
    }

    @Test
    public void should_retrieve_errors_when_select_multiple_complete_facet_not_an_array() throws Exception {
        JsonNode test1Json = JsonHandler.getFromFile(
            PropertiesUtils.getResourceFile("select_multiple_complete_facet_not_an_array.json")
        );
        final Schema schema = loadSchema(
            new ObjectMapper(),
            PropertiesUtils.getResourceFile(SELECT_QUERY_MULTIPLE_DSL_SCHEMA_JSON)
        );

        assertThatThrownBy(() -> Validator.validate(schema, "DSL", test1Json))
            .hasMessageContaining("$facets")
            .hasMessageContaining("WRONG_JSON_TYPE: OBJECT");
    }

    @Test
    public void should_retrieve_errors_when_select_multiple_complete_facet_date_Ranges_no_From_no_To()
        throws Exception {
        JsonNode test1Json = JsonHandler.getFromFile(
            PropertiesUtils.getResourceFile("select_multiple_complete_facet_no_from_no_To.json")
        );
        final Schema schema = loadSchema(
            new ObjectMapper(),
            PropertiesUtils.getResourceFile(SELECT_QUERY_MULTIPLE_DSL_SCHEMA_JSON)
        );
        assertThatThrownBy(() -> Validator.validate(schema, "DSL", test1Json))
            .hasMessageContaining("$ranges")
            .hasMessageContaining("ELEMENT_TOO_SHORT");
    }

    @Test
    public void should_retrieve_errors_when_select_multiple_complete_empty_ranges() throws Exception {
        JsonNode test1Json = JsonHandler.getFromFile(
            PropertiesUtils.getResourceFile("select_multiple_complete_facet_empty_ranges.json")
        );
        final Schema schema = loadSchema(
            new ObjectMapper(),
            PropertiesUtils.getResourceFile(SELECT_QUERY_MULTIPLE_DSL_SCHEMA_JSON)
        );
        assertThatThrownBy(() -> Validator.validate(schema, "DSL", test1Json))
            .hasMessageContaining("$ranges")
            .hasMessageContaining("ELEMENT_TOO_SHORT");
    }

    @Test
    public void should_not_retrieve_errors_when_select_multiple_complete_facet_date_Ranges() throws Exception {
        JsonNode test1Json = JsonHandler.getFromFile(
            PropertiesUtils.getResourceFile("select_multiple_complete_facet_ok_Date_range.json")
        );
        final Schema schema = loadSchema(
            new ObjectMapper(),
            PropertiesUtils.getResourceFile(SELECT_QUERY_MULTIPLE_DSL_SCHEMA_JSON)
        );

        assertThatCode(() -> Validator.validate(schema, "DSL", test1Json)).doesNotThrowAnyException();
    }

    @Test
    public void should_not_retrieve_errors_when_select_multiple_complete_facet_date_Ranges_one_from() throws Exception {
        JsonNode test1Json = JsonHandler.getFromFile(
            PropertiesUtils.getResourceFile("select_multiple_complete_facet_ok_Date_range_one_from.json")
        );
        final Schema schema = loadSchema(
            new ObjectMapper(),
            PropertiesUtils.getResourceFile(SELECT_QUERY_MULTIPLE_DSL_SCHEMA_JSON)
        );

        assertThatCode(() -> Validator.validate(schema, "DSL", test1Json)).doesNotThrowAnyException();
    }

    @Test
    public void should_retrieve_errors_when_select_multiple_complete_facet_terms_no_size() throws Exception {
        JsonNode test1Json = JsonHandler.getFromFile(
            PropertiesUtils.getResourceFile("select_multiple_complete_facet_terms_no_size.json")
        );
        final Schema schema = loadSchema(
            new ObjectMapper(),
            PropertiesUtils.getResourceFile(SELECT_QUERY_MULTIPLE_DSL_SCHEMA_JSON)
        );

        assertThatThrownBy(() -> Validator.validate(schema, "DSL", test1Json)).hasMessageContaining(
            "$size: posinteger ~ MANDATORY"
        );
    }

    @Test
    public void should_retrieve_errors_when_select_multiple_complete_facet_terms_no_order() throws Exception {
        JsonNode test1Json = JsonHandler.getFromFile(
            PropertiesUtils.getResourceFile("select_multiple_complete_facet_terms_no_order.json")
        );
        final Schema schema = loadSchema(
            new ObjectMapper(),
            PropertiesUtils.getResourceFile(SELECT_QUERY_MULTIPLE_DSL_SCHEMA_JSON)
        );

        assertThatThrownBy(() -> Validator.validate(schema, "DSL", test1Json))
            .hasMessageContaining("Validating $order")
            .hasMessageContaining("MANDATORY");
    }

    // FIXME : remove ignore when fixing bug #4353
    @Test
    public void should_retrieve_errors_when_select_multiple_complete_facet_terms_invalid_order() throws Exception {
        JsonNode test1Json = JsonHandler.getFromFile(
            PropertiesUtils.getResourceFile("select_multiple_complete_facet_terms_invalid_order.json")
        );
        final Schema schema = loadSchema(
            new ObjectMapper(),
            PropertiesUtils.getResourceFile(SELECT_QUERY_MULTIPLE_DSL_SCHEMA_JSON)
        );

        assertThatThrownBy(() -> Validator.validate(schema, "DSL", test1Json))
            .hasMessageContaining("Validating $order")
            .hasMessageContaining("WRONG");
    }

    @Test
    public void should_not_retrieve_errors_when_select_multiple_complete_facet_terms_valid_order() throws Exception {
        JsonNode test1Json = JsonHandler.getFromFile(
            PropertiesUtils.getResourceFile("select_multiple_complete_facet_terms_valid_order.json")
        );
        final Schema schema = loadSchema(
            new ObjectMapper(),
            PropertiesUtils.getResourceFile(SELECT_QUERY_MULTIPLE_DSL_SCHEMA_JSON)
        );

        assertThatCode(() -> Validator.validate(schema, "DSL", test1Json)).doesNotThrowAnyException();
    }

    @Test
    public void should_retrieve_errors_when_select_multiple_complete_facet_filters_empty_filters() throws Exception {
        JsonNode test1Json = JsonHandler.getFromFile(
            PropertiesUtils.getResourceFile("select_multiple_complete_facet_filters_empty_filters.json")
        );
        final Schema schema = loadSchema(
            new ObjectMapper(),
            PropertiesUtils.getResourceFile(SELECT_QUERY_MULTIPLE_DSL_SCHEMA_JSON)
        );

        assertThatThrownBy(() -> Validator.validate(schema, "DSL", test1Json))
            .hasMessageContaining("Validating $query_filters")
            .hasMessageContaining("MANDATORY");
    }

    @Test
    public void should_retrieve_errors_when_select_multiple_complete_facet_filters_empty_filters_filters()
        throws Exception {
        JsonNode test1Json = JsonHandler.getFromFile(
            PropertiesUtils.getResourceFile("select_multiple_complete_facet_filters_empty_filters_filters.json")
        );
        final Schema schema = loadSchema(
            new ObjectMapper(),
            PropertiesUtils.getResourceFile(SELECT_QUERY_MULTIPLE_DSL_SCHEMA_JSON)
        );

        assertThatThrownBy(() -> Validator.validate(schema, "DSL", test1Json))
            .hasMessageContaining("Validating $query_filters")
            .hasMessageContaining("ELEMENT_TOO_SHORT");
    }

    @Test
    public void should_retrieve_errors_when_select_multiple_complete_facet_filters_query_no_name() throws Exception {
        JsonNode test1Json = JsonHandler.getFromFile(
            PropertiesUtils.getResourceFile("select_multiple_complete_facet_filters_query_no_name.json")
        );
        final Schema schema = loadSchema(
            new ObjectMapper(),
            PropertiesUtils.getResourceFile(SELECT_QUERY_MULTIPLE_DSL_SCHEMA_JSON)
        );

        assertThatThrownBy(() -> Validator.validate(schema, "DSL", test1Json))
            .hasMessageContaining("Validating $name")
            .hasMessageContaining("MANDATORY");
    }

    @Test
    public void should_retrieve_errors_when_select_multiple_complete_facet_filters_query_no_query() throws Exception {
        JsonNode test1Json = JsonHandler.getFromFile(
            PropertiesUtils.getResourceFile("select_multiple_complete_facet_filters_query_no_query.json")
        );
        final Schema schema = loadSchema(
            new ObjectMapper(),
            PropertiesUtils.getResourceFile(SELECT_QUERY_MULTIPLE_DSL_SCHEMA_JSON)
        );

        assertThatThrownBy(() -> Validator.validate(schema, "DSL", test1Json))
            .hasMessageContaining("Validating $query")
            .hasMessageContaining("MANDATORY");
    }
}
