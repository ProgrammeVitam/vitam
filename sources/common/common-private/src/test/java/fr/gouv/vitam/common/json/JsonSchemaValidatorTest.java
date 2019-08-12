package fr.gouv.vitam.common.json;

import fr.gouv.vitam.common.exception.VitamRuntimeException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JsonSchemaValidatorTest {

    @Test
    public void forBuiltInSchema() throws Exception {

        // Given
        String schemaFilename = "/test_schema.json";

        // When / Then
        JsonSchemaValidator schemaValidator = JsonSchemaValidator.forBuiltInSchema(schemaFilename);

        // Then
        schemaValidator.validateJson(JsonHandler.createObjectNode()
            .put("_id", "MyId")
            .put("Title", "MyTitle")
        );

        assertThatThrownBy(() ->
            schemaValidator.validateJson(JsonHandler.createObjectNode()
                .put("Title", "MyTitle")
            )
        ).isInstanceOf(JsonSchemaValidationException.class);
    }

    @Test
    public void givenConstructorWithInexistingSchemaThenException() {
        assertThatThrownBy(() -> JsonSchemaValidator.forBuiltInSchema("/no_such_file"))
            .isInstanceOf(VitamRuntimeException.class);
    }

    @Test
    public void givenConstructorWithIncorrectSchemaThenException() {
        assertThatThrownBy(() -> JsonSchemaValidator.forBuiltInSchema("/test.conf"))
            .isInstanceOf(VitamRuntimeException.class);
    }

    @Test
    public void forUserSchema() throws Exception {

        // Given
        String schema =
            "{\"$schema\":\"http://json-schema.org/draft-04/schema#\",\"id\":\"http://example.com/root.json\",\"type\":\"object\",\"additionalProperties\":true,\"anyOf\":[{\"required\":[\"_id\",\"Title\"]}],\"properties\":{\"_id\":{\"type\":\"string\"},\"Title\":{\"type\":\"string\"}}}";

        // When / Then
        JsonSchemaValidator schemaValidator = JsonSchemaValidator.forUserSchema(schema);

        schemaValidator.validateJson(JsonHandler.createObjectNode()
            .put("_id", "MyId")
            .put("Title", "MyTitle")
        );

        assertThatThrownBy(() ->
            schemaValidator.validateJson(JsonHandler.createObjectNode()
                .put("Title", "MyTitle")
            )
        ).isInstanceOf(JsonSchemaValidationException.class);
    }

    @Test
    public void forUserSchemaWithInvalidSchema() {

        // Given
        String schema = "invalid";

        // When / Then
        assertThatThrownBy(() -> JsonSchemaValidator.forUserSchema(schema))
            .isInstanceOf(InvalidJsonSchemaException.class);
    }
}
