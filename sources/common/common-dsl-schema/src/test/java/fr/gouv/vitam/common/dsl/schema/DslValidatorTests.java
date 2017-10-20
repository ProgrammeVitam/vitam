package fr.gouv.vitam.common.dsl.schema;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;

public class DslValidatorTests {

    @Test
    public void testValidatorSelectMultiple() throws InvalidParseOperationException, IOException, ValidationException {
        DslValidator dslValidator = new DslValidator();
        JsonNode selectMultipleQuery =
            JsonHandler.getFromFile(PropertiesUtils.findFile("select_multiple_complete.json"));
        dslValidator.validateSelectMultiple(selectMultipleQuery);
    }

    @Test
    public void testValidatorSelectSingle() throws InvalidParseOperationException, IOException, ValidationException {
        DslValidator dslValidator = new DslValidator();
        JsonNode selectSingleQuery = JsonHandler.getFromFile(PropertiesUtils.findFile("select_single_complete.json"));
        dslValidator.validateSelectSingle(selectSingleQuery);
    }

    @Test
    public void testValidatorGetById() throws InvalidParseOperationException, IOException, ValidationException {
        DslValidator dslValidator = new DslValidator();
        JsonNode getByIdQuery = JsonHandler.getFromFile(PropertiesUtils.findFile("get_by_id_complete.json"));
        dslValidator.validateGetById(getByIdQuery);
    }

    @Test
    public void testValidatorUpdateById() throws InvalidParseOperationException, IOException, ValidationException {
        DslValidator dslValidator = new DslValidator();
        JsonNode updateByIdQuery = JsonHandler.getFromFile(PropertiesUtils.findFile("update_by_id_complete.json"));
        dslValidator.validateUpdateById(updateByIdQuery);
    }

    @Test
    public void testValidatorSelectMultipleWithSingleQueryException()
        throws InvalidParseOperationException, IOException, ValidationException {
        DslValidator dslValidator = new DslValidator();
        JsonNode selectSingleQuery = JsonHandler.getFromFile(PropertiesUtils.findFile("select_single_complete.json"));
        assertThatThrownBy(() -> dslValidator.validateSelectMultiple(selectSingleQuery))
            .hasMessageContaining("Dsl query is not valid");
    }

    @Test
    public void testValidatorSelectSingleWithMultipleQueryException()
        throws InvalidParseOperationException, IOException, ValidationException {
        DslValidator dslValidator = new DslValidator();
        JsonNode selectMultipleQuery = JsonHandler.getFromFile(PropertiesUtils.findFile("select_multiple_complete.json"));
        assertThatThrownBy(() -> dslValidator.validateSelectSingle(selectMultipleQuery))
            .hasMessageContaining("Dsl query is not valid");
    }

}
