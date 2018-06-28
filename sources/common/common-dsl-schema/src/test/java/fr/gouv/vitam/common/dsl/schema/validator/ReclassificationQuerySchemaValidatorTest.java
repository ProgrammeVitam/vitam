package fr.gouv.vitam.common.dsl.schema.validator;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.dsl.schema.ValidationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import org.junit.Test;

import java.io.IOException;

public class ReclassificationQuerySchemaValidatorTest {

    @Test
    public void testUpdateByIdValidator() throws InvalidParseOperationException, IOException, ValidationException {
        DslValidator dslValidator = new ReclassificationQuerySchemaValidator();
        JsonNode updateByIdQuery = JsonHandler.getFromFile(PropertiesUtils.findFile("reclassification_query_complete.json"));
        dslValidator.validate(updateByIdQuery);
    }
}
