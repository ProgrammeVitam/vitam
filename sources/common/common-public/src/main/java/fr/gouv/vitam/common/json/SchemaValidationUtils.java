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
package fr.gouv.vitam.common.json;

import java.io.FileNotFoundException;
import java.text.ParseException;
import java.util.Date;
import java.util.Iterator;

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

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.SchemaValidationStatus.SchemaValidationStatusEnum;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

public class SchemaValidationUtils {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(SchemaValidationUtils.class);

    private JsonSchema archiveUnitSchema;

    public static final String SCHEMA_FILENAME = "json-schema/archive-unit-schema.json";

    /**
     * Constructor with a default schema filename
     * 
     * @throws FileNotFoundException
     * @throws ProcessingException
     * @throws InvalidParseOperationException
     */
    public SchemaValidationUtils() throws FileNotFoundException, ProcessingException, InvalidParseOperationException {
        setSchema(SCHEMA_FILENAME);
    }

    /**
     * Constructor with a specified schema filename
     * 
     * @param schemaFilename
     * @throws FileNotFoundException
     * @throws ProcessingException
     * @throws InvalidParseOperationException
     */
    protected SchemaValidationUtils(String schemaFilename)
        throws FileNotFoundException, ProcessingException, InvalidParseOperationException {
        setSchema(schemaFilename);
    }

    private void setSchema(String schemaFilename)
        throws FileNotFoundException, ProcessingException, InvalidParseOperationException {
        // override for date format
        final Library library = DraftV4Library.get().thaw()
            .addFormatAttribute("date-time-vitam", VitamDateTimeAttribute.getInstance())
            .freeze();
        final MessageBundle bundle = MessageBundles.getBundle(JsonSchemaValidationBundle.class);
        final ValidationConfiguration cfg = ValidationConfiguration.newBuilder()
            .setDefaultLibrary("http://vitam-json-schema.org/draft-04/schema#", library)
            .setValidationMessages(bundle).freeze();
        final JsonSchemaFactory factory = JsonSchemaFactory.newBuilder()
            .setValidationConfiguration(cfg).freeze();

        // build archive schema validator

        JsonNode archiveUnitSchemaJson =
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(schemaFilename));
        archiveUnitSchema = factory.getJsonSchema(archiveUnitSchemaJson);
    }

    /**
     * Validate a json with a schema
     * 
     * @param archiveUnit the json to be validated
     * @return a status ({@link SchemaValidationStatus})
     */
    public SchemaValidationStatus validateUnit(JsonNode archiveUnit) {
        try {
            ProcessingReport report = archiveUnitSchema.validate(archiveUnit);
            if (!report.isSuccess()) {
                JsonNode error = (( ListProcessingReport ) report).asJson();
                ObjectNode errorNode = JsonHandler.createObjectNode();
                errorNode.set( "validateUnitReport",  error);
                LOGGER.error("Archive unit is not valid : \n" + errorNode.toString());
                return new SchemaValidationStatus(errorNode.toString(),
                    SchemaValidationStatusEnum.NOT_AU_JSON_VALID);
            }
            if (archiveUnit.get(SedaConstants.TAG_RULE_START_DATE) != null && 
                archiveUnit.get(SedaConstants.TAG_RULE_END_DATE) != null) {
                final Date startDate = LocalDateUtil.getDate(
                    archiveUnit.get(SedaConstants.TAG_RULE_START_DATE).asText());
                final Date endDate = LocalDateUtil.getDate(
                    archiveUnit.get(SedaConstants.TAG_RULE_END_DATE).asText());
                if (endDate.before(startDate)) {
                    final String errorMessage = "EndDate is before StartDate, unit Title : " + archiveUnit.get("Title").asText();
                    LOGGER.error(errorMessage);
                    return new SchemaValidationStatus(errorMessage,
                        SchemaValidationStatusEnum.NOT_AU_JSON_VALID);
                }  
            }
        } catch (ProcessingException | ParseException e) {
            LOGGER.error("File is not a valid json file", e);
            return new SchemaValidationStatus("File is not a valid json file",
                SchemaValidationStatusEnum.NOT_JSON_FILE);
        }
        return new SchemaValidationStatus("Correct file", SchemaValidationStatusEnum.VALID);
    }

    /**
     * Validate a json for update with a schema
     * 
     * @param archiveUnit the json to be validated
     * @return a status ({@link SchemaValidationStatus})
     */
    public SchemaValidationStatus validateUpdateUnit(JsonNode archiveUnit) {
        ObjectNode unitCopy = ((ObjectNode) archiveUnit).deepCopy();
        final Iterator<String> names = unitCopy.fieldNames();
        while (names.hasNext()) {
            String name = names.next();
            if ("_mgt".equals(name)) {
                final JsonNode value = unitCopy.remove(name);
                unitCopy.set("Management", value);
                break;
            }
        }
        return validateUnit(unitCopy);
    }

}
