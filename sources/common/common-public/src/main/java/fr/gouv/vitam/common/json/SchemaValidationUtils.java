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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.fge.jsonschema.cfg.ValidationConfiguration;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ListProcessingReport;
import com.github.fge.jsonschema.core.report.LogLevel;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.library.DraftV4Library;
import com.github.fge.jsonschema.library.Library;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.messages.JsonSchemaValidationBundle;
import com.github.fge.msgsimple.bundle.MessageBundle;
import com.github.fge.msgsimple.load.MessageBundles;
import fr.gouv.vitam.common.CharsetUtils;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.json.SchemaValidationStatus.SchemaValidationStatusEnum;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.administration.OntologyModel;
import org.apache.commons.lang3.BooleanUtils;

import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static com.fasterxml.jackson.databind.node.BooleanNode.FALSE;
import static com.fasterxml.jackson.databind.node.BooleanNode.TRUE;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;

/**
 * SchemaValidationUtils
 */
public class SchemaValidationUtils {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(SchemaValidationUtils.class);
    private static final String MANAGEMENT = "Management";
    private static final String CORRECT_FILE = "Correct file";
    private static final String FILE_IS_NOT_A_VALID_JSON_FILE = "File is not a valid json file";

    private JsonSchema jsonSchema;

    /**
     * archive-unit-schema
     */
    public static final String ARCHIVE_UNIT_SCHEMA_FILENAME = "json-schema/archive-unit-schema.json";
    /**
     * access-contract-schema
     */
    private static final String ACCESS_CONTRACT_SCHEMA_FILENAME = "json-schema/access-contract-schema.json";
    /**
     * accession-register-detail.schema
     */
    public static final String ACCESSION_REGISTER_DETAIL_SCHEMA_FILENAME =
        "json-schema/accession-register-detail.schema.json";
    /**
     * accession-register-summary.schema
     */
    public static final String ACCESSION_REGISTER_SUMMARY_SCHEMA_FILENAME =
        "json-schema/accession-register-summary.schema.json";
    /**
     * agencies.schema
     */
    private static final String AGENCIES_SCHEMA_FILENAME = "json-schema/agencies.schema.json";
    /**
     * archive-unit-profile.schema
     */
    private static final String ARCHIVE_UNIT_PROFILE_SCHEMA_FILENAME = "json-schema/archive-unit-profile.schema.json";
    /**
     * context.schema
     */
    private static final String CONTEXT_SCHEMA_FILENAME = "json-schema/context.schema.json";
    /**
     * file-format.schema
     */
    public static final String FILE_FORMAT_SCHEMA_FILENAME = "json-schema/file-format.schema.json";
    /**
     * file-rules.schema
     */
    private static final String FILE_RULES_SCHEMA_FILENAME = "json-schema/file-rules.schema.json";
    /**
     * ingest-contract.schema
     */
    private static final String INGEST_CONTRACT_SCHEMA_FILENAME = "json-schema/ingest-contract.schema.json";
    /**
     * profile.schema
     */
    private static final String PROFILE_SCHEMA_FILENAME = "json-schema/profile.schema.json";
    /**
     * security-profile.schema
     */
    private static final String SECURITY_PROFILE_SCHEMA_FILENAME = "json-schema/security-profile.schema.json";

    /**
     * schemaValidation
     */
    public static final String TAG_SCHEMA_VALIDATION = "schemaValidation";
    /**
     * ontology.schema
     */
    private static final String ONTOLOGY_SCHEMA_FILENAME = "json-schema/ontology.schema.json";


    private static final String GRIFFIN_SCHEMA = "json-schema/griffin-shema.schema.json";

    private static final String PRESERVATION_SCENARIO_SCHEMA = "json-schema/preservation-scenario-shema.schema.json";


    private static final DateTimeFormatter XSD_DATATYPE_DATE_FORMATTER = new DateTimeFormatterBuilder()
        .appendPattern("u-MM-dd['T'HH:mm:ss[.SSS][.SS][.S][xxx][X]][xxx][X]")
        .toFormatter();

    private static final String PROPERTIES = "properties";

    private static final String ITEMS = "items";

    private static final String DATE_TIME_VITAM = "date-time-vitam";

    private static final List<String> SCHEMA_DECLARATION_TYPE = Arrays.asList("$schema",
        "id", "type", "additionalProperties", "anyOf", "required", "description", ITEMS, "title",
        "oneOf", "enum", "minLength", "minItems", PROPERTIES);

    public static Set<String> listOfAvailableAttributes() {
        try {
            Set<String> attributes = new HashSet<>();
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(ARCHIVE_UNIT_SCHEMA_FILENAME))
                .get(PROPERTIES)
                .get(MANAGEMENT)
                .get(PROPERTIES)
                .forEach(p -> p.path(PROPERTIES).fieldNames().forEachRemaining(attributes::add));
            if (attributes.isEmpty()) {
                throw new IllegalStateException("Cannot initialize set of available management attributes");
            }
            return attributes;
        } catch (InvalidParseOperationException | FileNotFoundException e) {
            throw new VitamRuntimeException(e);
        }
    }

    /**
     * Constructor with a default schema filename
     *
     * @throws FileNotFoundException          FileNotFoundException
     * @throws ProcessingException            ProcessingException
     * @throws InvalidParseOperationException InvalidParseOperationException
     */
    public SchemaValidationUtils() throws FileNotFoundException, ProcessingException, InvalidParseOperationException {
        setSchema(ARCHIVE_UNIT_SCHEMA_FILENAME);
    }


    /**
     * Constructor with a specified schema filename
     *
     * @param schema schemaFilename or external json schema as a string
     * @throws FileNotFoundException          FileNotFoundException
     * @throws ProcessingException            ProcessingException
     * @throws InvalidParseOperationException InvalidParseOperationException
     */
    protected SchemaValidationUtils(String schema)
        throws FileNotFoundException, ProcessingException, InvalidParseOperationException {
        this(schema, false);
    }

    /**
     * Constructor with a specified schema filename or an external json schema as a string
     *
     * @param schema   schemaFilename or external json schema as a string
     * @param external true if the schema is provided as a string
     * @throws FileNotFoundException          FileNotFoundException
     * @throws ProcessingException            ProcessingException
     * @throws InvalidParseOperationException InvalidParseOperationException
     */
    public SchemaValidationUtils(String schema, boolean external)
        throws FileNotFoundException, ProcessingException, InvalidParseOperationException {
        if (external) {
            setSchemaAsString(schema);
        } else {
            setSchema(schema);
        }
    }

    /**
     * Get the default Vitam JsonSchemaFactory
     *
     * @return JsonSchemaFactory
     */
    private static JsonSchemaFactory getJsonSchemaFactory() {
        // override for date format
        final Library library = DraftV4Library.get().thaw()
            .addFormatAttribute(DATE_TIME_VITAM, VitamDateTimeAttribute.getInstance())
            .freeze();

        final MessageBundle bundle = MessageBundles.getBundle(JsonSchemaValidationBundle.class);
        final ValidationConfiguration cfg = ValidationConfiguration.newBuilder()
            .setDefaultLibrary("http://vitam-json-schema.org/draft-04/schema#", library)
            .setValidationMessages(bundle).freeze();

        return JsonSchemaFactory.newBuilder()
            .setValidationConfiguration(cfg).freeze();
    }

    private void setSchemaAsString(String schemaJsonAsString)
        throws ProcessingException, InvalidParseOperationException {

        JsonNode schemaAsJson = null;
        try {
            schemaAsJson = JsonHandler.getFromString(schemaJsonAsString);
        } catch (InvalidParseOperationException e) {
            LOGGER.error("Could not parse schema", e);
            throw e;
        }
        final JsonSchemaFactory factory = getJsonSchemaFactory();
        ProcessingReport pr = factory.getSyntaxValidator().validateSchema(schemaAsJson);
        if (pr.isSuccess()) {
            jsonSchema = factory.getJsonSchema(schemaAsJson);
        } else {
            throw new ProcessingException("External Schema is not valid");
        }
    }

    private void setSchema(String schemaFilename)
        throws FileNotFoundException, ProcessingException, InvalidParseOperationException {
        final JsonSchemaFactory factory = getJsonSchemaFactory();
        // build archive schema validator
        JsonNode schemaJson =
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(schemaFilename));
        jsonSchema = factory.getJsonSchema(schemaJson);
    }


    public SchemaValidationStatus validateJson(JsonNode jsonNode, String collectionName)
        throws FileNotFoundException, InvalidParseOperationException, ProcessingException {

        switch (collectionName) {
            case "AccessContract":
                setSchema(ACCESS_CONTRACT_SCHEMA_FILENAME);
                break;
            case "Agencies":
                setSchema(AGENCIES_SCHEMA_FILENAME);
                break;
            case "ArchiveUnitProfile":
                setSchema(ARCHIVE_UNIT_PROFILE_SCHEMA_FILENAME);
                break;
            case "Context":
                setSchema(CONTEXT_SCHEMA_FILENAME);
                break;
            case "FileRules":
                setSchema(FILE_RULES_SCHEMA_FILENAME);
                break;
            case "IngestContract":
                setSchema(INGEST_CONTRACT_SCHEMA_FILENAME);
                break;
            case "Profile":
                setSchema(PROFILE_SCHEMA_FILENAME);
                break;
            case "SecurityProfile":
                setSchema(SECURITY_PROFILE_SCHEMA_FILENAME);
                break;
            case "Ontology":
                setSchema(ONTOLOGY_SCHEMA_FILENAME);
                break;
            case "Griffin":
                setSchema(GRIFFIN_SCHEMA);
                break;
            case "PreservationScenario":
                setSchema(PRESERVATION_SCENARIO_SCHEMA);
                break;
            case "ArchiveUnitProfileSchema":
                // Archive Unit Profile Schema is set before and used for validation
                // no need to set it again here
                break;
            case "FileFormat":
            case "CollectionSample":
            case "AccessionRegisterDetail":
            case "AccessionRegisterSummary":
                return new SchemaValidationStatus(CORRECT_FILE, SchemaValidationStatusEnum.VALID);
            default:
                throw new FileNotFoundException("No json schema found for collection " + collectionName);
        }

        return validateJson(jsonNode);
    }

    /**
     * Validate a json with a schema
     *
     * @param jsonNode the json to be validated
     * @return a status ({@link SchemaValidationStatus})
     */
    private SchemaValidationStatus validateJson(JsonNode jsonNode) {
        try {
            ProcessingReport report = jsonSchema.validate(jsonNode);

            if (!report.isSuccess()) {
                JsonNode error = ((ListProcessingReport) report).asJson();
                ObjectNode errorNode = JsonHandler.createObjectNode();
                errorNode.set("validateJson", error);
                LOGGER.error("Json is not valid : \n" + errorNode.toString());
                return new SchemaValidationStatus(errorNode.toString(),
                    SchemaValidationStatusEnum.NOT_AU_JSON_VALID);
            }

        } catch (ProcessingException e) {
            LOGGER.error(FILE_IS_NOT_A_VALID_JSON_FILE, e);
            return new SchemaValidationStatus(FILE_IS_NOT_A_VALID_JSON_FILE,
                SchemaValidationStatusEnum.NOT_JSON_FILE);
        }
        return new SchemaValidationStatus(CORRECT_FILE, SchemaValidationStatusEnum.VALID);
    }

    /**
     * Validate a json with the schema archive-unit-schema
     *
     * @param archiveUnit the json to be validated
     * @return a status ({@link SchemaValidationStatus})
     */
    public SchemaValidationStatus validateUnit(JsonNode archiveUnit) {
        try {
            ProcessingReport report = jsonSchema.validate(archiveUnit);
            if (!report.isSuccess()) {
                JsonNode error = ((ListProcessingReport) report).asJson();
                ObjectNode errorNode = JsonHandler.createObjectNode();
                errorNode.set("validateUnitReport", error);
                LOGGER.error("Archive unit is not valid : \n" + errorNode.toString());
                int errorIndex = getIndexForErrorLevelObjectNode(error);
                String instancePointer = error.get(errorIndex).get("instance").get("pointer").asText();
                if (instancePointer.contains("StartDate") || instancePointer.contains("EndDate")) {
                    return new SchemaValidationStatus(errorNode.toString(),
                        SchemaValidationStatusEnum.RULE_DATE_FORMAT);
                }
                if (error.get(0).get("required") != null && error.get(0).get("missing") != null) {
                    return new SchemaValidationStatus(errorNode.toString(),
                        SchemaValidationStatusEnum.EMPTY_REQUIRED_FIELD);
                }
                return new SchemaValidationStatus(errorNode.toString(),
                    SchemaValidationStatusEnum.NOT_AU_JSON_VALID);
            }
            if (archiveUnit.get(SedaConstants.TAG_RULE_START_DATE) != null &&
                archiveUnit.get(SedaConstants.TAG_RULE_END_DATE) != null) {
                final Date startDate = LocalDateUtil.getDate(
                    archiveUnit.get(SedaConstants.TAG_RULE_START_DATE).asText());
                final Date endDate = LocalDateUtil.getDate(
                    archiveUnit.get(SedaConstants.TAG_RULE_END_DATE).asText());

                LOGGER.debug("in SchemaValidationUtils class, StartDate=" + startDate + " EndDate=" + endDate);

                if (endDate.before(startDate)) {
                    final String errorMessage =
                        "EndDate is before StartDate, unit Title : " + archiveUnit.get("Title").asText();
                    ObjectNode error = JsonHandler.createObjectNode();
                    error.put("Error", errorMessage);
                    ObjectNode errorNode = JsonHandler.createObjectNode();
                    errorNode.set(SedaConstants.EV_DET_TECH_DATA, error);
                    LOGGER.error(errorMessage);
                    return new SchemaValidationStatus(errorNode.toString(),
                        SchemaValidationStatusEnum.RULE_BAD_START_END_DATE,
                        archiveUnit.get(SedaConstants.PREFIX_ID).asText());
                }
            }
        } catch (ProcessingException | ParseException e) {
            LOGGER.error(FILE_IS_NOT_A_VALID_JSON_FILE, e);
            return new SchemaValidationStatus(FILE_IS_NOT_A_VALID_JSON_FILE,
                SchemaValidationStatusEnum.NOT_JSON_FILE);
        }
        return new SchemaValidationStatus(CORRECT_FILE, SchemaValidationStatusEnum.VALID);
    }

    private int getIndexForErrorLevelObjectNode(JsonNode error) {
        int errorIndex = 0;
        for (int index = 0; index <= LogLevel.values().length; index++) {
            if (error.get(index) != null && error.get(index).get("level") != null &&
                error.get(index).get("level").asText().equalsIgnoreCase(LogLevel.ERROR.toString())) {
                errorIndex = index;
                break;
            }
        }
        return errorIndex;
    }

    /**
     * Validate a json for insert or update with a schema
     *
     * @param archiveUnitCopy the json to be validated
     * @return a status ({@link SchemaValidationStatus})
     */
    public SchemaValidationStatus validateInsertOrUpdateUnit(ObjectNode archiveUnitCopy) {
        if (archiveUnitCopy.get("_mgt") != null) {
            final JsonNode value = archiveUnitCopy.remove("_mgt");
            archiveUnitCopy.set(MANAGEMENT, value);
        }

        if (archiveUnitCopy.get("#management") != null) {
            final JsonNode value = archiveUnitCopy.remove("#management");
            archiveUnitCopy.set(MANAGEMENT, value);
        }

        return validateUnit(archiveUnitCopy);
    }


    /**
     * Get fields list declared in schema
     *
     * @param schemaJsonAsString schemaJsonAsString
     * @return a the list of fields declared in the schema
     */
    public List<String> extractFieldsFromSchema(String schemaJsonAsString)
        throws InvalidParseOperationException {
        List<String> listProperties = new ArrayList<>();
        JsonNode externalSchema = JsonHandler.getFromString(schemaJsonAsString);
        if (externalSchema != null && externalSchema.get(PROPERTIES) != null) {
            extractPropertyFromJsonNode(externalSchema.get(PROPERTIES), listProperties);
        }
        return listProperties;

    }



    private void extractPropertyFromJsonNode(JsonNode currentJson, List<String> listProperties) {
        final Iterator<Entry<String, JsonNode>> iterator = currentJson.fields();
        while (iterator.hasNext()) {
            final Entry<String, JsonNode> entry = iterator.next();
            String key = entry.getKey();
            JsonNode value = entry.getValue();
            if (value == null) {
                continue;
            }
            if (value.isObject() || value.isArray()) {
                // if subproperties
                extractPropertyFromJsonNode(value, listProperties);
            }

            if (!SCHEMA_DECLARATION_TYPE.contains(key) && value.isObject() && value.get(PROPERTIES) == null &&
                (value.get(ITEMS) == null || (value.get(ITEMS) != null && value.get(ITEMS).get(PROPERTIES) == null)) &&
                !listProperties.contains(key)) {
                listProperties.add(key);
            }
        }
    }


    /**
     * Verify and replace fields.
     *
     * @param node             to modify
     * @param ontologyModelMap where are ontologies
     * @param errors           of replacement
     */
    public void verifyAndReplaceFields(JsonNode node, Map<String, OntologyModel> ontologyModelMap,
        List<String> errors) {
        Iterator<Entry<String, JsonNode>> iterator = node.fields();
        while (iterator.hasNext()) {
            Entry<String, JsonNode> entry = iterator.next();
            verifyLine(node, ontologyModelMap, errors, entry);
        }
    }

    private void verifyLine(JsonNode node, Map<String, OntologyModel> ontologyModelMap, List<String> errors,
        Entry<String, JsonNode> entry) {
        String fieldName = entry.getKey();

        OntologyModel ontology = ontologyModelMap.get(fieldName);
        JsonNode fieldValue = entry.getValue();
        if (fieldValue == null || fieldValue.isMissingNode()) {
            return;
        }

        if (fieldValue.isObject()) {
            verifyAndReplaceFields(fieldValue, ontologyModelMap, errors);

            return;
        }
        if (fieldValue.isArray()) {
            for (int i = 0; i < fieldValue.size(); i++) {
                JsonNode jn = fieldValue.get(i);
                verifyField(ontologyModelMap, errors, fieldName, ontology, (ArrayNode) fieldValue, i, jn);
            }
            return;
        }
        errors.addAll(replacePropertyField(fieldValue, ontology, node, fieldName));

    }

    private void verifyField(Map<String, OntologyModel> ontologyModelMap, List<String> errors, String fieldName,
        OntologyModel ontology, ArrayNode fieldValue, int i, JsonNode jn) {
        if (null == jn || null == jn.asText()) {
            return;
        }

        if (jn.isObject() || jn.isArray()) {
            verifyAndReplaceFields(jn, ontologyModelMap, errors);
            return;
        }
        try {
            fieldValue
                .set(i, checkFieldLengthAndForceFieldTyping(fieldName, jn, ontology));
        } catch (IllegalArgumentException | DateTimeParseException e) {
            errors.add(String.format("Error '%s' on field '%s'.", e.getMessage(), fieldName));
        }

    }


    /**
     * Example of fieldContainer:  fieldContainer = "{'fieldName': 'fieldValue'}"
     *
     * @param fieldValue     fieldValue
     * @param ontology       ontology
     * @param fieldContainer fieldContainer
     * @param fieldName      fieldName
     * @return List
     */
    private List<String> replacePropertyField(JsonNode fieldValue, OntologyModel ontology, JsonNode fieldContainer,
        String fieldName) {
        ObjectNode objectNodeContainer = (ObjectNode) fieldContainer;
        if (!fieldValue.isNull() && !fieldValue.isMissingNode()) {
            try {
                objectNodeContainer.set(fieldName,
                    checkFieldLengthAndForceFieldTyping(fieldName, fieldValue, ontology));
            } catch (IllegalArgumentException | DateTimeParseException e) {
                return Collections.singletonList(String
                    .format("Error: <%s> on field '%s'.", e.getMessage(), fieldName));
            }
        }
        return Collections.emptyList();
    }


    private JsonNode checkFieldLengthAndForceFieldTyping(String fieldName, JsonNode fieldValueNode,
        OntologyModel ontologyModel) {
        // In case where no ontology is provided
        if (null == ontologyModel) {
            return validateValueLength(fieldName, fieldValueNode, VitamConfiguration.getTextMaxLength(),
                "Not accepted value for the text field (%s) whose UTF8 encoding is longer than the max length " + VitamConfiguration.getTextMaxLength());
        }

        switch (ontologyModel.getType()) {
            case TEXT:
                return validateValueLength(fieldName, fieldValueNode, VitamConfiguration.getTextMaxLength(),
                    "Not accepted value for the text field (%s) whose UTF8 encoding is longer than the max length " + VitamConfiguration.getTextMaxLength());
            case GEO_POINT:
            case ENUM:
            case KEYWORD:
                return validateValueLength(fieldName, fieldValueNode,
                    VitamConfiguration.getKeywordMaxLength(),
                    "Not accepted value for the Keyword field (%s) whose UTF8 encoding is longer than the max length " + VitamConfiguration.getTextMaxLength());

            case DOUBLE:
                return new DoubleNode(Double.parseDouble(fieldValueNode.asText()));
            case DATE:
                return new TextNode(mapDateToOntology(fieldValueNode.asText()));
            case LONG:
                return new LongNode(Long.parseLong(fieldValueNode.asText()));
            case BOOLEAN:
                return BooleanNode.valueOf(
                        BooleanUtils.toBoolean(fieldValueNode.asText().toLowerCase(), "true", "false"));
            default:
                throw new IllegalStateException(
                    String.format("Not implemented for type %s", fieldValueNode.asText()));
        }
    }

    /**
     * Check if value length is accepted or is longer than expected
     *
     * @param fieldName      fieldName
     * @param fieldValueNode fieldValueNode
     * @param maxLength      maxLength
     * @param message        message
     * @return JsonNode
     */
    private JsonNode validateValueLength(String fieldName, JsonNode fieldValueNode, int maxLength,
        String message) {
        if (null == fieldValueNode || null == fieldValueNode.asText()) {
            return null;
        } else if (fieldValueNode.asText().getBytes(CharsetUtils.UTF8).length > maxLength) {
            throw new IllegalArgumentException(String.format(
                message,
                fieldName));
        }
        return fieldValueNode;
    }

    private String mapDateToOntology(String field) {
        TemporalAccessor parse = XSD_DATATYPE_DATE_FORMATTER.parse(field);
        return parse.isSupported(HOUR_OF_DAY)
            ? parse.query(LocalDateTime::from).format(ISO_LOCAL_DATE_TIME)
            : parse.query(LocalDate::from).format(ISO_LOCAL_DATE);
    }
}
