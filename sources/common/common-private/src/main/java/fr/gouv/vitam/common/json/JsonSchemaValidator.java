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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonschema.cfg.ValidationConfiguration;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ListProcessingReport;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.library.DraftV4Library;
import com.github.fge.jsonschema.library.Library;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.messages.JsonSchemaValidationBundle;
import com.github.fge.msgsimple.bundle.MessageBundle;
import com.github.fge.msgsimple.load.MessageBundles;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;

import javax.annotation.concurrent.Immutable;
import java.io.IOException;
import java.io.InputStream;

@Immutable
public class JsonSchemaValidator {

    private static final JsonSchemaFactory JSON_SCHEMA_FACTORY = getJsonSchemaFactory();
    private final JsonSchema jsonSchema;

    private static final String DATE_TIME_VITAM = "date-time-vitam";

    public static JsonSchemaValidator forBuiltInSchema(String schemaFilename) {
        try (InputStream is = JsonSchemaValidator.class.getResourceAsStream(schemaFilename)) {
            JsonNode schemaJson = JsonHandler.getFromInputStream(is);
            JsonSchema jsonSchema = JSON_SCHEMA_FACTORY.getJsonSchema(schemaJson);
            return new JsonSchemaValidator(jsonSchema);
        } catch (InvalidParseOperationException | ProcessingException | IOException e) {
            throw new VitamRuntimeException("Could not initialize built-in schema file " + schemaFilename);
        }
    }

    public static JsonSchemaValidator forUserSchema(String schemaJsonAsString) throws InvalidJsonSchemaException {
        try {
            JsonNode schemaAsJson = JsonHandler.getFromString(schemaJsonAsString);
            ProcessingReport pr = JSON_SCHEMA_FACTORY.getSyntaxValidator().validateSchema(schemaAsJson);
            if (!pr.isSuccess()) {
                throw new InvalidJsonSchemaException("External Schema is not valid");
            }
            JsonSchema jsonSchema = JSON_SCHEMA_FACTORY.getJsonSchema(schemaAsJson);
            return new JsonSchemaValidator(jsonSchema);
        } catch (InvalidParseOperationException | ProcessingException e) {
            throw new InvalidJsonSchemaException("External Schema is not valid", e);
        }
    }

    private JsonSchemaValidator(JsonSchema jsonSchema) {
        this.jsonSchema = jsonSchema;
    }

    /**
     * Get the default Vitam JsonSchemaFactory
     *
     * @return JsonSchemaFactory
     */
    private static JsonSchemaFactory getJsonSchemaFactory() {
        // override for date format
        final Library library = DraftV4Library.get()
            .thaw()
            .addFormatAttribute(DATE_TIME_VITAM, VitamDateTimeAttribute.getInstance())
            .freeze();

        final MessageBundle bundle = MessageBundles.getBundle(JsonSchemaValidationBundle.class);
        final ValidationConfiguration cfg = ValidationConfiguration.newBuilder()
            .setDefaultLibrary("http://vitam-json-schema.org/draft-04/schema#", library)
            .setValidationMessages(bundle)
            .freeze();

        return JsonSchemaFactory.newBuilder().setValidationConfiguration(cfg).freeze();
    }

    /**
     * Validate a json with a schema
     */
    public void validateJson(JsonNode jsonNode) throws JsonSchemaValidationException {
        try {
            ProcessingReport report = jsonSchema.validate(jsonNode);

            if (!report.isSuccess()) {
                JsonNode error = ((ListProcessingReport) report).asJson();
                ObjectNode errorNode = JsonHandler.createObjectNode();
                errorNode.set("validateJson", error);
                throw new JsonSchemaValidationException("Document schema validation failed : \n" + errorNode);
            }
        } catch (ProcessingException e) {
            throw new JsonSchemaValidationException("Document schema validation failed", e);
        }
    }
}
