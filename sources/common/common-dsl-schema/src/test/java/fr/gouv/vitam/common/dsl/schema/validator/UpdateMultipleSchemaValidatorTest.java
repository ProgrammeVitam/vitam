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
 * Update Multiple Schema Validator Tests
 */
public class UpdateMultipleSchemaValidatorTest {

    @Test
    public void testUpdateMultipleSchemaValidator()
        throws InvalidParseOperationException, IOException, ValidationException {
        DslValidator dslValidator = new UpdateMultipleSchemaValidator();
        JsonNode updateQuery = JsonHandler.getFromFile(PropertiesUtils.findFile("update_multiple_complete.json"));
        dslValidator.validate(updateQuery);
    }

    @Test
    public void testUpdateMultipleSchemaValidatorWithEmptyRoot()
        throws InvalidParseOperationException, IOException, ValidationException {
        DslValidator dslValidator = new UpdateMultipleSchemaValidator();
        JsonNode updateQuery = JsonHandler.getFromFile(PropertiesUtils.findFile("update_multiple_empty_root.json"));
        dslValidator.validate(updateQuery);
    }

    @Test
    public void testUpdateMultipleSchemaValidatorWithNoRoot()
        throws InvalidParseOperationException, IOException, ValidationException {
        DslValidator dslValidator = new UpdateMultipleSchemaValidator();
        JsonNode updateQuery = JsonHandler.getFromFile(PropertiesUtils.findFile("update_multiple_no_root.json"));
        dslValidator.validate(updateQuery);
    }

    @Test
    public void testUpdateMultipleSchemaValidatorWithNoThreshold()
        throws InvalidParseOperationException, IOException, ValidationException {
        DslValidator dslValidator = new UpdateMultipleSchemaValidator();
        JsonNode updateQuery = JsonHandler.getFromFile(
            PropertiesUtils.findFile("update_multiple_with_no_threshold.json")
        );
        dslValidator.validate(updateQuery);
    }

    @Test
    public void testUpdateMultipleSchemaValidatorWithSingleQueryException()
        throws InvalidParseOperationException, IOException, ValidationException {
        DslValidator dslValidator = new UpdateMultipleSchemaValidator();
        JsonNode updateQuery = JsonHandler.getFromFile(
            PropertiesUtils.findFile("update_multiple_with_single_query.json")
        );
        assertThatThrownBy(() -> dslValidator.validate(updateQuery)).hasMessageContaining("Dsl query is not valid");
    }

    @Test
    public void testUpdateMultipleSchemaValidatorWithInvalidThresholdException()
        throws InvalidParseOperationException, IOException, ValidationException {
        DslValidator dslValidator = new UpdateMultipleSchemaValidator();
        JsonNode updateQuery = JsonHandler.getFromFile(
            PropertiesUtils.findFile("update_multiple_with_invalid_threshold.json")
        );
        assertThatThrownBy(() -> dslValidator.validate(updateQuery)).hasMessageContaining("Dsl query is not valid");
    }

    @Test
    public void testUpdateMultipleSchemaValidatorWithFilterException()
        throws InvalidParseOperationException, IOException, ValidationException {
        DslValidator dslValidator = new UpdateMultipleSchemaValidator();
        JsonNode updateQuery = JsonHandler.getFromFile(PropertiesUtils.findFile("update_multiple_with_filter.json"));
        assertThatThrownBy(() -> dslValidator.validate(updateQuery)).hasMessageContaining("Dsl query is not valid");
    }

    @Test
    public void testUpdateMultipleSchemaValidatorWithProjectionException()
        throws InvalidParseOperationException, IOException, ValidationException {
        DslValidator dslValidator = new UpdateMultipleSchemaValidator();
        JsonNode updateQuery = JsonHandler.getFromFile(
            PropertiesUtils.findFile("update_multiple_with_projection.json")
        );
        assertThatThrownBy(() -> dslValidator.validate(updateQuery)).hasMessageContaining("Dsl query is not valid");
    }

    @Test
    public void testUpdateMultipleSchemaValidatorRegexPatternWithoutUpdatePatternException()
        throws InvalidParseOperationException, IOException, ValidationException {
        DslValidator dslValidator = new UpdateMultipleSchemaValidator();
        JsonNode updateQuery = JsonHandler.getFromFile(
            PropertiesUtils.findFile("update_multiple_regex_pattern_without_controlPattern.json")
        );
        assertThatThrownBy(() -> dslValidator.validate(updateQuery)).hasMessageContaining("Dsl query is not valid");
    }

    @Test
    public void testUpdateMultipleSchemaValidatorRegexPatternWithoutControlPatternException()
        throws InvalidParseOperationException, IOException, ValidationException {
        DslValidator dslValidator = new UpdateMultipleSchemaValidator();
        JsonNode updateQuery = JsonHandler.getFromFile(
            PropertiesUtils.findFile("update_multiple_regex_pattern_without_updatePattern.json")
        );
        assertThatThrownBy(() -> dslValidator.validate(updateQuery)).hasMessageContaining("Dsl query is not valid");
    }

    @Test
    public void should_internalActionKeyRetriever_tests_be_valid() throws Exception {
        // All test case for the class fr.gouv.vitam.common.InternalActionKeysRetriever must be valid,
        // so if changes are made in the DSL of update-mass-query-dsl-schema.json it must be change also
        // in the InternalActionKeysRetriever class.

        // Given
        DslValidator dslValidator = new UpdateMultipleSchemaValidator();

        List<JsonNode> testFiles = List.of(
            JsonHandler.getFromFile(PropertiesUtils.findFile("queryActionSetInternalField.json")),
            JsonHandler.getFromFile(PropertiesUtils.findFile("queryActionSetInternalField2.json")),
            JsonHandler.getFromFile(PropertiesUtils.findFile("queryActionSetInternalField3.json")),
            JsonHandler.getFromFile(PropertiesUtils.findFile("queryActionSetInternalField4.json")),
            JsonHandler.getFromFile(PropertiesUtils.findFile("queryActionSetInternalField5.json")),
            JsonHandler.getFromFile(PropertiesUtils.findFile("queryActionSetExternalField.json")),
            JsonHandler.getFromFile(PropertiesUtils.findFile("queryActionSetInternalFieldREGEX.json"))
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
