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

/**
 * Tests of multi select query schema for Metadata DSL query
 */
public class ValidatorSelectQueryMultipleTest {
    private static final String SELECT_QUERY_MULTIPLE_DSL_SCHEMA_JSON = "select-query-multiple-dsl-schema.json";

    private Validator loadSchema(ObjectMapper objectMapper, File dslSource)
        throws IOException, InvalidParseOperationException {
        try (InputStream inputStream = new FileInputStream(dslSource)) {
            final Schema schema = Schema.load(objectMapper, inputStream);
            TypeDef dslType = schema.getDefinitions().get("DSL");
            System.out.println(dslType.toString());
            TypeDef queryType = schema.getDefinitions().get("QUERY");
            System.out.println(queryType.toString());
            TypeDef filterType = schema.getDefinitions().get("FILTER");
            System.out.println(filterType.toString());
            return new Validator(schema);
        }
    }

    @Test
    public void should_not_retrieve_errors_when_select_multiple_complete_dsl()
        throws IOException, InvalidParseOperationException {
        JsonNode test1Json =
            JsonHandler.getFromFile(PropertiesUtils.getResourceFile("select_multiple_complete.json"));
        final Validator validator =
            loadSchema(new ObjectMapper(), PropertiesUtils.getResourceFile(SELECT_QUERY_MULTIPLE_DSL_SCHEMA_JSON));
        try {
            validator.validate(test1Json);
        } catch (ValidationException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void should_not_retrieve_errors_when_select_multiple_empty_root_dsl()
        throws IOException, InvalidParseOperationException {
        JsonNode test1Json =
            JsonHandler.getFromFile(PropertiesUtils.getResourceFile("select_multiple_empty_root.json"));
        final Validator validator =
            loadSchema(new ObjectMapper(), PropertiesUtils.getResourceFile(SELECT_QUERY_MULTIPLE_DSL_SCHEMA_JSON));
        try {
            validator.validate(test1Json);
        } catch (ValidationException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void should_not_retrieve_errors_when_select_multiple_no_root_dsl()
        throws IOException, InvalidParseOperationException {
        JsonNode test1Json =
            JsonHandler.getFromFile(PropertiesUtils.getResourceFile("select_multiple_no_root.json"));
        final Validator validator =
            loadSchema(new ObjectMapper(), PropertiesUtils.getResourceFile(SELECT_QUERY_MULTIPLE_DSL_SCHEMA_JSON));
        try {
            validator.validate(test1Json);
        } catch (ValidationException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void should_retrieve_errors_when_select_multiple_wrong_root_dsl()
        throws IOException, InvalidParseOperationException {
        JsonNode test1Json =
            JsonHandler.getFromFile(PropertiesUtils.getResourceFile("select_multiple_wrong_root.json"));
        final Validator validator =
            loadSchema(new ObjectMapper(), PropertiesUtils.getResourceFile(SELECT_QUERY_MULTIPLE_DSL_SCHEMA_JSON));
        assertThatThrownBy(() -> validator.validate(test1Json))
            .hasMessageContaining("$roots: guid[] ~ INVALID_VALUE: STRING");
    }

    @Test
    public void should_retrieve_errors_when_select_multiple_wrong_query_dsl()
        throws IOException, InvalidParseOperationException {
        JsonNode test1Json =
            JsonHandler.getFromFile(PropertiesUtils.getResourceFile("select_multiple_query_not_array.json"));
        final Validator validator =
            loadSchema(new ObjectMapper(), PropertiesUtils.getResourceFile(SELECT_QUERY_MULTIPLE_DSL_SCHEMA_JSON));
        assertThatThrownBy(() -> validator.validate(test1Json))
            .hasMessageContaining("$query: ROOT_QUERY[] ~ WRONG_JSON_TYPE: OBJECT")
            .hasMessageContaining("$query: ROOT_QUERY[] ~ INVALID_JSON_FIELD")
            .hasMessageContaining("$query: ROOT_QUERY[] ~ INVALID_JSON_FIELD");
    }

    @Test
    public void should_not_retrieve_errors_when_select_empty_query_dsl()
        throws IOException, InvalidParseOperationException {
        JsonNode test1Json =
            JsonHandler.getFromFile(PropertiesUtils.getResourceFile("select_multiple_empty_query.json"));
        final Validator validator =
            loadSchema(new ObjectMapper(), PropertiesUtils.getResourceFile(SELECT_QUERY_MULTIPLE_DSL_SCHEMA_JSON));
        try {
            validator.validate(test1Json);
        } catch (ValidationException e) {
            e.printStackTrace();
            fail();
        }
    }
}
