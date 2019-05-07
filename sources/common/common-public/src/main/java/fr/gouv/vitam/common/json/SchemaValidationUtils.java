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
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.SchemaValidationStatus.SchemaValidationStatusEnum;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.administration.OntologyModel;
import fr.gouv.vitam.common.model.administration.OntologyType;
import org.apache.commons.lang3.BooleanUtils;

import java.io.FileNotFoundException;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static com.fasterxml.jackson.databind.node.BooleanNode.FALSE;
import static com.fasterxml.jackson.databind.node.BooleanNode.TRUE;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;

/**
 * SchemaValidationUtils
 */
public class SchemaValidationUtils {

    /**
     * archive-unit-schema
     */
    public static final String ARCHIVE_UNIT_SCHEMA_FILENAME = "json-schema/archive-unit-schema.json";
    /**
     * access-contract-schema
     */
    public static final String ACCESS_CONTRACT_SCHEMA_FILENAME = "json-schema/access-contract-schema.json";
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
    public static final String AGENCIES_SCHEMA_FILENAME = "json-schema/agencies.schema.json";
    /**
     * archive-unit-profile.schema
     */
    public static final String ARCHIVE_UNIT_PROFILE_SCHEMA_FILENAME = "json-schema/archive-unit-profile.schema.json";
    /**
     * context.schema
     */
    public static final String CONTEXT_SCHEMA_FILENAME = "json-schema/context.schema.json";
    /**
     * file-format.schema
     */
    public static final String FILE_FORMAT_SCHEMA_FILENAME = "json-schema/file-format.schema.json";
    /**
     * file-rules.schema
     */
    public static final String FILE_RULES_SCHEMA_FILENAME = "json-schema/file-rules.schema.json";
    /**
     * ingest-contract.schema
     */
    public static final String INGEST_CONTRACT_SCHEMA_FILENAME = "json-schema/ingest-contract.schema.json";
    /**
     * profile.schema
     */
    public static final String PROFILE_SCHEMA_FILENAME = "json-schema/profile.schema.json";
    /**
     * security-profile.schema
     */
    public static final String SECURITY_PROFILE_SCHEMA_FILENAME = "json-schema/security-profile.schema.json";
    /**
     * schemaValidation
     */
    public static final String TAG_SCHEMA_VALIDATION = "schemaValidation";
    /**
     * schemaValidation
     */
    public static final String TAG_ONTOLOGY_FIELDS = "ontologyFields";
    /**
     * ontology.schema
     */
    public static final String ONTOLOGY_SCHEMA_FILENAME = "json-schema/ontology.schema.json";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(SchemaValidationUtils.class);
    private static final DateTimeFormatter XSD_DATATYPE_DATE_FORMATTER = new DateTimeFormatterBuilder()
        .appendPattern("u-MM-dd['T'HH:mm:ss[.SSS][.SS][.S][xxx][X]][xxx][X]")
        .toFormatter();
    private static final String PROPERTIES = "properties";
    private static final String ITEMS = "items";
    private static final String DATE_TIME_VITAM = "date-time-vitam";
    private static final List<String> SCHEMA_DECLARATION_TYPE = Arrays.asList(
        "$schema", "id", "type", "additionalProperties", "anyOf",
        "required", "description", "items", "title",
        "oneOf", "enum", "minLength", "minItems", "properties"
    );
    private JsonSchema jsonSchema;
    private boolean isExternal = false;

    /**
     * Constructor with a default schema filename
     *
     * @throws FileNotFoundException
     * @throws ProcessingException
     * @throws InvalidParseOperationException
     */
    public SchemaValidationUtils() throws FileNotFoundException, ProcessingException, InvalidParseOperationException {
        setSchema(ARCHIVE_UNIT_SCHEMA_FILENAME);
    }


    /**
     * Constructor with a specified schema filename
     *
     * @param schema schemaFilename or external json schema as a string
     * @throws FileNotFoundException
     * @throws ProcessingException
     * @throws InvalidParseOperationException
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
     * @throws FileNotFoundException
     * @throws ProcessingException
     * @throws InvalidParseOperationException
     */
    public SchemaValidationUtils(String schema, boolean external)
        throws FileNotFoundException, ProcessingException, InvalidParseOperationException {
        if (external) {
            setSchemaAsString(schema);
            isExternal = true;
        } else {
            setSchema(schema);
        }
    }

    /**
     * Get the default Vitam JsonSchemaFactory
     *
     * @return
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

        final JsonSchemaFactory factory = JsonSchemaFactory.newBuilder()
            .setValidationConfiguration(cfg).freeze();
        return factory;

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

    /**
     * Validate the json against the schema of the specified collectionName
     *
     * @param jsonNode
     * @param collectionName
     * @return a status ({@link SchemaValidationStatus})
     * @throws FileNotFoundException          if no schema has been found fot the specified collectionname
     * @throws InvalidParseOperationException
     * @throws ProcessingException
     */
    public SchemaValidationStatus validateJson(JsonNode jsonNode, String collectionName)
        throws FileNotFoundException, InvalidParseOperationException, ProcessingException {

        if ("AccessContract".equals(collectionName)) {
            setSchema(ACCESS_CONTRACT_SCHEMA_FILENAME);
        } else if ("AccessionRegisterDetail".equals(collectionName)) {
            // TODO : use ACCESSION_REGISTER_DETAIL_SCHEMA_FILENAME and fix validation
            return new SchemaValidationStatus("Correct file", SchemaValidationStatusEnum.VALID);
        } else if ("AccessionRegisterSummary".equals(collectionName)) {
            // TODO : should we validate by json schema (ACCESSION_REGISTER_SUMMARY_SCHEMA_FILENAME)
            return new SchemaValidationStatus("Correct file", SchemaValidationStatusEnum.VALID);
        } else if ("Agencies".equals(collectionName)) {
            setSchema(AGENCIES_SCHEMA_FILENAME);
        } else if ("ArchiveUnitProfile".equals(collectionName)) {
            setSchema(ARCHIVE_UNIT_PROFILE_SCHEMA_FILENAME);
        } else if ("Context".equals(collectionName)) {
            setSchema(CONTEXT_SCHEMA_FILENAME);
        } else if ("FileFormat".equals(collectionName)) {
            // TODO : should we validate by json schema (FILE_FORMAT_SCHEMA_FILENAME)
            return new SchemaValidationStatus("Correct file", SchemaValidationStatusEnum.VALID);
        } else if ("FileRules".equals(collectionName)) {
            setSchema(FILE_RULES_SCHEMA_FILENAME);
        } else if ("IngestContract".equals(collectionName)) {
            setSchema(INGEST_CONTRACT_SCHEMA_FILENAME);
        } else if ("Profile".equals(collectionName)) {
            setSchema(PROFILE_SCHEMA_FILENAME);
        } else if ("SecurityProfile".equals(collectionName)) {
            setSchema(SECURITY_PROFILE_SCHEMA_FILENAME);
        } else if ("Ontology".equals(collectionName)) {
            setSchema(ONTOLOGY_SCHEMA_FILENAME);
        } else if ("ArchiveUnitProfileSchema".equals(collectionName)) {
            // Archive Unit Profile Schema is set before and used for validation
            // no need to set it again here
            return validateJson(jsonNode);
        }
        // used for test
        else if ("CollectionSample".equals(collectionName)) {
            return new SchemaValidationStatus("Correct file", SchemaValidationStatusEnum.VALID);
        } else {
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
    protected SchemaValidationStatus validateJson(JsonNode jsonNode) {
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
            LOGGER.error("File is not a valid json file", e);
            return new SchemaValidationStatus("File is not a valid json file",
                SchemaValidationStatusEnum.NOT_JSON_FILE);
        }
        return new SchemaValidationStatus("Correct file", SchemaValidationStatusEnum.VALID);
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
            LOGGER.error("File is not a valid json file", e);
            return new SchemaValidationStatus("File is not a valid json file",
                SchemaValidationStatusEnum.NOT_JSON_FILE);
        }
        return new SchemaValidationStatus("Correct file", SchemaValidationStatusEnum.VALID);
    }

    private int getIndexForErrorLevelObjectNode(JsonNode error) {
        int errorIndex = 0;
        for (int index = 0; index <= LogLevel.values().length; index++) {
            if (error.get(index) != null) {
                if (error.get(index).get("level") != null &&
                    error.get(index).get("level").asText().equalsIgnoreCase(LogLevel.ERROR.toString())) {
                    errorIndex = index;
                    break;
                }

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
            archiveUnitCopy.set("Management", value);
        }

        if (archiveUnitCopy.get("#management") != null) {
            final JsonNode value = archiveUnitCopy.remove("#management");
            archiveUnitCopy.set("Management", value);
        }

        return validateUnit(archiveUnitCopy);
    }


    /**
     * Get fields list declared in schema
     *
     * @param schemaJsonAsString
     * @return a the list of fields declared in the schema
     * @throws InvalidParseOperationException
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
            if (value != null && value.isObject() || value.isArray()) {
                // if subproperties
                extractPropertyFromJsonNode(value, listProperties);
            }

            if (!SCHEMA_DECLARATION_TYPE.contains(key) && value.isObject() && value.get(PROPERTIES) == null &&
                (value.get(ITEMS) == null || (value.get(ITEMS) != null && value.get(ITEMS).get(PROPERTIES) == null))) {
                if (!listProperties.contains(key)) {
                    listProperties.add(key);
                }
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
            final Entry<String, JsonNode> entry = iterator.next();
            String fieldName = entry.getKey();
            JsonNode fieldValue = entry.getValue();
            if (fieldValue == null || fieldValue.isMissingNode()) {
                continue;
            }
            if (ontologyModelMap.containsKey(fieldName)) {
                errors.addAll(replaceProperFieldWithType(fieldValue, ontologyModelMap.get(fieldName), node, fieldName));
            } else if (fieldValue.isObject()) {
                verifyAndReplaceFields(fieldValue, ontologyModelMap, errors);
            } else if (fieldValue.isArray()) {
                fieldValue.forEach(o -> verifyAndReplaceFields(o, ontologyModelMap, errors));
            }
        }
    }

    private List<String> replaceProperFieldWithType(JsonNode archiveUnitFragment, OntologyModel ontology,
        JsonNode parent, String fieldName) {
        if (archiveUnitFragment.isArray()) {
            return replacePropertyFieldArray((ArrayNode) archiveUnitFragment, archiveUnitFragment.deepCopy(), ontology,
                fieldName);
        }
        return replacePropertyField(archiveUnitFragment, ontology, parent, fieldName);
    }

    private List<String> replacePropertyFieldArray(ArrayNode originalFields, ArrayNode copyFields,
        OntologyModel ontology, String fieldName) {
        ArrayList<String> errors = new ArrayList<>();
        for (int i = 0; i < copyFields.size(); i++) {
            String field = copyFields.get(i).asText();
            if (field == null) {
                continue;
            }
            try {
                originalFields.set(i, mapFieldToOntology(field, ontology.getType()));
            } catch (IllegalArgumentException e) {
                errors.add(error(ontology, fieldName, e));
            }
        }
        return errors;
    }

    private List<String> replacePropertyField(JsonNode archiveUnitFragment, OntologyModel ontology, JsonNode parent,
        String fieldName) {
        ObjectNode objectNodeParent = (ObjectNode) parent;
        if (archiveUnitFragment.isTextual() && parent.isObject() && parent.get(ontology.getIdentifier()) != null) {
            String field = archiveUnitFragment.asText();
            try {
                objectNodeParent.set(ontology.getIdentifier(), mapFieldToOntology(field, ontology.getType()));
            } catch (IllegalArgumentException | DateTimeParseException e) {
                return Collections.singletonList(String
                    .format("Error: <%s> on field '%s' should be of type '%s'.", e.getMessage(), fieldName,
                        ontology.getType().name()));
            }
        }
        return Collections.emptyList();
    }

    private String error(OntologyModel ontology, String fieldName, RuntimeException e) {
        return String.format("Error '%s' on field '%s' should be of type '%s'.", e.getMessage(), fieldName,
            ontology.getType().name());
    }

    private JsonNode mapFieldToOntology(String field, OntologyType type) {
        switch (type) {
            case DOUBLE:
                return new DoubleNode(Double.parseDouble(field));
            case DATE:
                return new TextNode(mapDateToOntology(field));
            case LONG:
                return new LongNode(Long.parseLong(field));
            case BOOLEAN:
                return BooleanNode.valueOf(BooleanUtils.toBoolean(field.toLowerCase(), TRUE.asText(), FALSE.asText()));
            default:
                LOGGER.warn(String.format("Not implemented for type %s", field));
                throw new IllegalStateException(String.format("Not implemented for type %s", field));
        }
    }

    private String mapDateToOntology(String field) {
        TemporalAccessor parse = XSD_DATATYPE_DATE_FORMATTER.parse(field);
        return parse.isSupported(HOUR_OF_DAY)
            ? parse.query(LocalDateTime::from).format(ISO_LOCAL_DATE_TIME)
            : parse.query(LocalDate::from).format(ISO_LOCAL_DATE);
    }
}
