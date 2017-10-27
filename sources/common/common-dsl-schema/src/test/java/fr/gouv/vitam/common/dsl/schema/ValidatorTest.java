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
package fr.gouv.vitam.common.dsl.schema;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.dsl.schema.meta.Schema;
import fr.gouv.vitam.common.dsl.schema.meta.TypeDef;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;


public class ValidatorTest {
    private ObjectMapper mapper = new ObjectMapper();

    @Test
    public void should_retrieve_errors_when_send_malformed_filter_query_dsl()
        throws IOException, InvalidParseOperationException {

        JsonNode test1Json = mapper
            .readTree(PropertiesUtils.getResourceFile("test1.json")); // TODO faudra problement renommer test1.json
        final Validator validator = loadSchema(new ObjectMapper(), PropertiesUtils.getResourceFile("dsl.json"));
        assertThatThrownBy(() -> validator.validate(test1Json))
            .hasMessageContaining("$roots: guid[] ~ INVALID_VALUE: NUMBER ");
    }

    private Validator loadSchema(ObjectMapper objectMapper, File dslSource) throws IOException {
        try (InputStream inputStream = new FileInputStream(dslSource)) {
            final Schema schema = Schema.load(objectMapper, inputStream);
            TypeDef dslType = schema.getDefinitions().get("DSL");
            // System.out.println(dslType.toString());
            TypeDef queryType = schema.getDefinitions().get("QUERY");
            // System.out.println(queryType.toString());
            return new Validator(schema);
        }
    }

    @Test
    public void should_retrieve_errors_when_validate_null() throws IOException, InvalidParseOperationException {
        final Validator validator = loadSchema(new ObjectMapper(), PropertiesUtils.getResourceFile("dsl.json"));
        try {
            validator.validate(null);
            fail();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (ValidationException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void should_not_retrieve_errors_when_search_article_request() throws Exception {
        JsonNode test1Json = mapper.readTree(PropertiesUtils.getResourceFile("operator_match_request.json"));
        final Validator validator = loadSchema(mapper, PropertiesUtils.getResourceFile("dsl.json"));
        validator.validate(test1Json);
    }

    @Test
    public void should_not_retrieve_errors_when_select_by_title_without_root_depth() throws Exception {
        JsonNode test1Json =
            mapper.readTree(PropertiesUtils.getResourceFile("operator_in_request.json"));
        final Validator validator = loadSchema(new ObjectMapper(), PropertiesUtils.getResourceFile("dsl.json"));
        validator.validate(test1Json);
    }

    @Test
    public void should_not_retrieve_errors_when_send_gte_request() throws Exception {
        JsonNode test1Json =
            mapper.readTree(PropertiesUtils.getResourceFile("operator_gte_request.json"));
        final Validator validator = loadSchema(new ObjectMapper(), PropertiesUtils.getResourceFile("dsl.json"));
        validator.validate(test1Json);
    }

    @Test
    public void should_not_retrieve_errors_when_send_lte_request() throws Exception {
        JsonNode test1Json =
            mapper.readTree(PropertiesUtils.getResourceFile("operator_lte_request.json"));
        final Validator validator = loadSchema(new ObjectMapper(), PropertiesUtils.getResourceFile("dsl.json"));
        validator.validate(test1Json);
    }



    @Test
    public void should_not_retrieve_errors_when_send_lt_request() throws Exception {
        JsonNode test1Json =
            mapper.readTree(PropertiesUtils.getResourceFile("operator_lt_request.json"));
        final Validator validator = loadSchema(new ObjectMapper(), PropertiesUtils.getResourceFile("dsl.json"));
        validator.validate(test1Json);
    }

    @Test
    public void should_not_retrieve_errors_when_send_gt_request() throws Exception {
        JsonNode test1Json =
            mapper.readTree(PropertiesUtils.getResourceFile("operator_gt_request.json"));
        final Validator validator = loadSchema(new ObjectMapper(), PropertiesUtils.getResourceFile("dsl.json"));
        validator.validate(test1Json);
    }


    @Test
    public void should_not_retrieve_errors_when_send_match_all_request() throws Exception {
        JsonNode test1Json =
            mapper.readTree(PropertiesUtils.getResourceFile("operator_match_all_request.json"));
        final Validator validator = loadSchema(new ObjectMapper(), PropertiesUtils.getResourceFile("dsl.json"));

        validator.validate(test1Json);
    }

    @Test
    public void should_not_retrieve_errors_when_send_match_phrase_request() throws Exception {
        JsonNode test1Json =
            mapper.readTree(PropertiesUtils.getResourceFile("operator_match_phrase_request.json"));
        final Validator validator = loadSchema(new ObjectMapper(), PropertiesUtils.getResourceFile("dsl.json"));
        validator.validate(test1Json);
    }

    @Test
    public void should_not_retrieve_errors_when_send_match_phrase_prefix_request() throws Exception {
        JsonNode test1Json =
            mapper.readTree(PropertiesUtils.getResourceFile("operator_match_phrase_prefix_request.json"));
        final Validator validator = loadSchema(new ObjectMapper(), PropertiesUtils.getResourceFile("dsl.json"));
        validator.validate(test1Json);
    }

    @Test
    public void should_not_retrieve_errors_when_send_not_request() throws Exception {
        JsonNode test1Json =
            mapper.readTree(PropertiesUtils.getResourceFile("operator_not_request.json"));
        final Validator validator = loadSchema(new ObjectMapper(), PropertiesUtils.getResourceFile("dsl.json"));
        validator.validate(test1Json);
    }

    @Test
    public void should_not_retrieve_errors_when_send_ne_request() throws Exception {
        JsonNode test1Json =
            mapper.readTree(PropertiesUtils.getResourceFile("operator_ne_request.json"));
        final Validator validator = loadSchema(new ObjectMapper(), PropertiesUtils.getResourceFile("dsl.json"));
        validator.validate(test1Json);
    }

    @Test
    public void should_not_retrieve_errors_when_send_range_request() throws Exception {
        // FIXME with new model range
        JsonNode test1Json =
            mapper.readTree(PropertiesUtils.getResourceFile("operator_range_request.json"));
        final Validator validator = loadSchema(new ObjectMapper(), PropertiesUtils.getResourceFile("dsl.json"));
        validator.validate(test1Json);
    }

    @Test
    public void should_retrieve_errors_when_send_range_with_invalid_interval_request() throws Exception {
        // FIXME with new model range
        JsonNode test1Json =
            mapper.readTree(PropertiesUtils.getResourceFile("operator_range_empty_or_one_value_request.json"));
        final Validator validator = loadSchema(new ObjectMapper(), PropertiesUtils.getResourceFile("dsl.json"));
        assertThatThrownBy(() -> validator.validate(test1Json))
            .hasMessageContaining("ELEMENT_TOO_SHORT: 1 < 2").hasMessageContaining("ELEMENT_TOO_SHORT: 0 < 2");
    }

    @Test
    public void should_retrieve_errors_when_send_exists_request_with_invalid_field() throws Exception {
        JsonNode test1Json =
            mapper.readTree(PropertiesUtils.getResourceFile("operator_exists_with_invalid_field_request.json"));
        final Validator validator = loadSchema(new ObjectMapper(), PropertiesUtils.getResourceFile("dsl.json"));
        assertThatThrownBy(() -> validator.validate(test1Json)).hasMessageContaining("$exists: string ~ INVALID_JSON_FIELD: Title")
            .hasMessageContaining("$exists: string ~ INVALID_VALUE: OBJECT ~ found json");

    }

    @Test
    public void should_not_retrieve_errors_when_send_exists() throws Exception {
        JsonNode test1Json =
            mapper.readTree(PropertiesUtils.getResourceFile("operator_exists_request.json"));
        final Validator validator = loadSchema(new ObjectMapper(), PropertiesUtils.getResourceFile("dsl.json"));
        validator.validate(test1Json);
    }

    @Test
    public void should_retrieve_errors_when_send_search_request_with_invalid_field() throws Exception {
        JsonNode test1Json =
            JsonHandler.getFromFile(PropertiesUtils.getResourceFile("operator_search_invalid_request.json"));
        final Validator validator = loadSchema(new ObjectMapper(), PropertiesUtils.getResourceFile("dsl.json"));
        assertThatThrownBy(() -> validator.validate(test1Json))
            .hasMessageContaining(
                "Validating $roots: guid[] ~ INVALID_VALUE: STRING ~ hint: Tableau d'identifiants d'AU racines ~ found json: \\\"azdazdazdaz\\\" ~ path: [$roots]")
            .hasMessageContaining(
                "Validating $search: {[key]: anyvalue} ~ ELEMENT_TOO_SHORT: 0 < 1 ~ found json: {} ~ path: [$query, $search]");
    }

    @Test
    public void should_not_retrieve_errors_when_send_size_request() throws Exception {
        JsonNode test1Json =
            mapper.readTree(PropertiesUtils.getResourceFile("operator_size_request.json"));
        final Validator validator = loadSchema(new ObjectMapper(), PropertiesUtils.getResourceFile("dsl.json"));
        validator.validate(test1Json);
    }

    @Test
    public void should_retrieve_errors_when_send_size_request_with_invalid_field() throws Exception {
        JsonNode test1Json =
            mapper.readTree(PropertiesUtils.getResourceFile("operator_size_invalid_request.json"));
        final Validator validator = loadSchema(new ObjectMapper(), PropertiesUtils.getResourceFile("dsl.json"));
        assertThatThrownBy(() -> validator.validate(test1Json))
            .hasMessageContaining(
                "Validating $roots: guid[] ~ INVALID_VALUE: STRING ~ hint: Tableau d'identifiants d'AU racines ~ found json: \\\"azdazdazdaz\\\" ~ path: [$roots]")
            .hasMessageContaining(
                "Validating $size: {[key]: integer} ~ ELEMENT_TOO_SHORT: 0 < 1 ~ found json: {} ~ path: [$query, $size]");
    }

}
