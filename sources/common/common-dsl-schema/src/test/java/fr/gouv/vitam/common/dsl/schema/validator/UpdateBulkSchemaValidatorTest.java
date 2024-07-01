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
package fr.gouv.vitam.common.dsl.schema.validator;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.dsl.schema.ValidationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.json.JsonHandler;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Update Bulk Schema Validator Tests
 */
public class UpdateBulkSchemaValidatorTest {

    @Test
    public void testUpdateBulkSchemaValidator()
        throws InvalidParseOperationException, IOException, ValidationException {
        DslValidator dslValidator = new UpdateBulkSchemaValidator();
        JsonNode updateQuery = JsonHandler.getFromFile(PropertiesUtils.findFile("bulk/update_bulk_complete.json"));
        assertThatCode(() -> dslValidator.validate(updateQuery)).doesNotThrowAnyException();
    }

    @Test
    public void testUpdateBulkSchemaValidatorWithNoThreshold()
        throws InvalidParseOperationException, IOException, ValidationException {
        DslValidator dslValidator = new UpdateBulkSchemaValidator();
        JsonNode updateQuery = JsonHandler.getFromFile(PropertiesUtils.findFile("bulk/update_bulk_no_threshold.json"));
        assertThatCode(() -> dslValidator.validate(updateQuery)).doesNotThrowAnyException();
    }

    @Test
    public void testUpdateBulkSchemaValidatorWithRoot()
        throws InvalidParseOperationException, IOException, ValidationException {
        DslValidator dslValidator = new UpdateBulkSchemaValidator();
        JsonNode updateQuery = JsonHandler.getFromFile(PropertiesUtils.findFile("bulk/update_bulk_with_root.json"));
        assertThatThrownBy(() -> dslValidator.validate(updateQuery)).hasMessageContaining("Dsl query is not valid");
    }

    @Test
    public void testUpdateBulkSchemaValidatorWithNoQueries()
        throws InvalidParseOperationException, IOException, ValidationException {
        DslValidator dslValidator = new UpdateBulkSchemaValidator();
        JsonNode updateQuery = JsonHandler.getFromFile(PropertiesUtils.findFile("bulk/update_bulk_no_queries.json"));
        assertThatThrownBy(() -> dslValidator.validate(updateQuery)).hasMessageContaining("Dsl query is not valid");
    }

    @Test
    public void testUpdateBulkSchemaValidatorWithEmptyQueries()
        throws InvalidParseOperationException, IOException, ValidationException {
        DslValidator dslValidator = new UpdateBulkSchemaValidator();
        JsonNode updateQuery = JsonHandler.getFromFile(PropertiesUtils.findFile("bulk/update_bulk_empty_queries.json"));
        assertThatThrownBy(() -> dslValidator.validate(updateQuery)).hasMessageContaining("Dsl query is not valid");
    }

    @Test
    public void testUpdateBulkSchemaValidatorNoActionException()
        throws InvalidParseOperationException, IOException, ValidationException {
        DslValidator dslValidator = new UpdateBulkSchemaValidator();
        JsonNode updateQuery = JsonHandler.getFromFile(PropertiesUtils.findFile("bulk/update_bulk_no_action.json"));
        assertThatThrownBy(() -> dslValidator.validate(updateQuery)).hasMessageContaining("Dsl query is not valid");
    }

    @Test
    public void testUpdateBulkSchemaValidatorEmptyActionException()
        throws InvalidParseOperationException, IOException, ValidationException {
        DslValidator dslValidator = new UpdateBulkSchemaValidator();
        JsonNode updateQuery = JsonHandler.getFromFile(PropertiesUtils.findFile("bulk/update_bulk_empty_action.json"));
        assertThatThrownBy(() -> dslValidator.validate(updateQuery)).hasMessageContaining("Dsl query is not valid");
    }

    @Test
    public void testUpdateBulkSchemaValidatorNoQueryException()
        throws InvalidParseOperationException, IOException, ValidationException {
        DslValidator dslValidator = new UpdateBulkSchemaValidator();
        JsonNode updateQuery = JsonHandler.getFromFile(PropertiesUtils.findFile("bulk/update_bulk_no_query.json"));
        assertThatThrownBy(() -> dslValidator.validate(updateQuery)).hasMessageContaining("Dsl query is not valid");
    }

    @Test
    public void testUpdateBulkSchemaValidatorEmptyQueryException()
        throws InvalidParseOperationException, IOException, ValidationException {
        DslValidator dslValidator = new UpdateBulkSchemaValidator();
        JsonNode updateQuery = JsonHandler.getFromFile(PropertiesUtils.findFile("bulk/update_bulk_empty_query.json"));
        assertThatThrownBy(() -> dslValidator.validate(updateQuery)).hasMessageContaining("Dsl query is not valid");
    }

    @Test
    public void testUpdateBulkSchemaValidatorWithInvalidThresholdException()
        throws InvalidParseOperationException, IOException, ValidationException {
        DslValidator dslValidator = new UpdateBulkSchemaValidator();
        JsonNode updateQuery = JsonHandler.getFromFile(
            PropertiesUtils.findFile("bulk/update_bulk_with_invalid_threshold.json")
        );
        assertThatThrownBy(() -> dslValidator.validate(updateQuery)).hasMessageContaining("Dsl query is not valid");
    }

    @Test
    public void testUpdateBulkSchemaValidatorWithFilterException()
        throws InvalidParseOperationException, IOException, ValidationException {
        DslValidator dslValidator = new UpdateBulkSchemaValidator();
        JsonNode updateQuery = JsonHandler.getFromFile(PropertiesUtils.findFile("bulk/update_bulk_with_filter.json"));
        assertThatThrownBy(() -> dslValidator.validate(updateQuery)).hasMessageContaining("Dsl query is not valid");
    }

    @Test
    public void testUpdateBulkSchemaValidatorWithProjectionException()
        throws InvalidParseOperationException, IOException, ValidationException {
        DslValidator dslValidator = new UpdateBulkSchemaValidator();
        JsonNode updateQuery = JsonHandler.getFromFile(
            PropertiesUtils.findFile("bulk/update_bulk_with_projection.json")
        );
        assertThatThrownBy(() -> dslValidator.validate(updateQuery)).hasMessageContaining("Dsl query is not valid");
    }

    @Test
    public void testUpdateBulkSchemaValidatorRegexPatternWithoutUpdatePatternException()
        throws InvalidParseOperationException, IOException, ValidationException {
        DslValidator dslValidator = new UpdateBulkSchemaValidator();
        JsonNode updateQuery = JsonHandler.getFromFile(
            PropertiesUtils.findFile("bulk/update_bulk_regex_pattern_without_controlPattern.json")
        );
        assertThatThrownBy(() -> dslValidator.validate(updateQuery)).hasMessageContaining("Dsl query is not valid");
    }

    @Test
    public void testUpdateBulkSchemaValidatorRegexPatternWithoutControlPatternException()
        throws InvalidParseOperationException, IOException, ValidationException {
        DslValidator dslValidator = new UpdateBulkSchemaValidator();
        JsonNode updateQuery = JsonHandler.getFromFile(
            PropertiesUtils.findFile("bulk/update_bulk_regex_pattern_without_updatePattern.json")
        );
        assertThatThrownBy(() -> dslValidator.validate(updateQuery)).hasMessageContaining("Dsl query is not valid");
    }

    @Test
    public void should_internalActionKeyRetriever_tests_be_valid() throws Exception {
        // All test case for the class fr.gouv.vitam.common.InternalActionKeysRetriever must be valid,
        // so if changes are made in the DSL of update-bulk-query-dsl-schema.json it must be change also
        // in the InternalActionKeysRetriever class.

        // Given
        DslValidator dslValidator = new UpdateBulkSchemaValidator();

        List<JsonNode> testFiles = List.of(
            JsonHandler.getFromFile(PropertiesUtils.findFile("bulk/queryActionSetInternalField.json")),
            JsonHandler.getFromFile(PropertiesUtils.findFile("bulk/queryActionSetInternalField2.json")),
            JsonHandler.getFromFile(PropertiesUtils.findFile("bulk/queryActionSetInternalField3.json")),
            JsonHandler.getFromFile(PropertiesUtils.findFile("bulk/queryActionSetInternalField4.json")),
            JsonHandler.getFromFile(PropertiesUtils.findFile("bulk/queryActionSetInternalField5.json")),
            JsonHandler.getFromFile(PropertiesUtils.findFile("bulk/queryActionSetExternalField.json")),
            JsonHandler.getFromFile(PropertiesUtils.findFile("bulk/queryActionSetInternalFieldREGEX.json"))
        );

        // When
        ThrowingCallable testInternalKeysRetrieverTestFiles = () ->
            testFiles.forEach(node -> {
                try {
                    dslValidator.validate(node);
                } catch (ValidationException e) {
                    throw new VitamRuntimeException(e);
                }
            });

        // Then
        assertThatCode(testInternalKeysRetrieverTestFiles).doesNotThrowAnyException();
    }
}
