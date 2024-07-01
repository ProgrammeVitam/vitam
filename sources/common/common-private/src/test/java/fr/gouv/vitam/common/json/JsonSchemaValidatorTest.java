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
        schemaValidator.validateJson(JsonHandler.createObjectNode().put("_id", "MyId").put("Title", "MyTitle"));

        assertThatThrownBy(
            () -> schemaValidator.validateJson(JsonHandler.createObjectNode().put("Title", "MyTitle"))
        ).isInstanceOf(JsonSchemaValidationException.class);
    }

    @Test
    public void givenConstructorWithInexistingSchemaThenException() {
        assertThatThrownBy(() -> JsonSchemaValidator.forBuiltInSchema("/no_such_file")).isInstanceOf(
            VitamRuntimeException.class
        );
    }

    @Test
    public void givenConstructorWithIncorrectSchemaThenException() {
        assertThatThrownBy(() -> JsonSchemaValidator.forBuiltInSchema("/test.conf")).isInstanceOf(
            VitamRuntimeException.class
        );
    }

    @Test
    public void forUserSchema() throws Exception {
        // Given
        String schema =
            "{\"$schema\":\"http://json-schema.org/draft-04/schema#\",\"id\":\"http://example.com/root.json\",\"type\":\"object\",\"additionalProperties\":true,\"anyOf\":[{\"required\":[\"_id\",\"Title\"]}],\"properties\":{\"_id\":{\"type\":\"string\"},\"Title\":{\"type\":\"string\"}}}";

        // When / Then
        JsonSchemaValidator schemaValidator = JsonSchemaValidator.forUserSchema(schema);

        schemaValidator.validateJson(JsonHandler.createObjectNode().put("_id", "MyId").put("Title", "MyTitle"));

        assertThatThrownBy(
            () -> schemaValidator.validateJson(JsonHandler.createObjectNode().put("Title", "MyTitle"))
        ).isInstanceOf(JsonSchemaValidationException.class);
    }

    @Test
    public void forUserSchemaWithInvalidSchema() {
        // Given
        String schema = "invalid";

        // When / Then
        assertThatThrownBy(() -> JsonSchemaValidator.forUserSchema(schema)).isInstanceOf(
            InvalidJsonSchemaException.class
        );
    }
}
