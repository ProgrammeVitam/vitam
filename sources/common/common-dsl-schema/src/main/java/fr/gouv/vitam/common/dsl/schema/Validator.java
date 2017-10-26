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
package fr.gouv.vitam.common.dsl.schema;

import static fr.gouv.vitam.common.dsl.schema.PrimitiveAnalysis.fromBoolean;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.dsl.schema.meta.Property;
import fr.gouv.vitam.common.dsl.schema.meta.Schema;
import fr.gouv.vitam.common.dsl.schema.meta.TDAnyKey;
import fr.gouv.vitam.common.dsl.schema.meta.TDArray;
import fr.gouv.vitam.common.dsl.schema.meta.TDEnum;
import fr.gouv.vitam.common.dsl.schema.meta.TDKeyChoice;
import fr.gouv.vitam.common.dsl.schema.meta.TDObject;
import fr.gouv.vitam.common.dsl.schema.meta.TDTypeChoice;
import fr.gouv.vitam.common.dsl.schema.meta.TDUnion;
import fr.gouv.vitam.common.dsl.schema.meta.TypeDef;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.exception.InvalidGuidOperationException;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

/**
 * DSL Validator from a Schema. WARNING: not thread safe. (note: Schema is thread safe)
 */
public class Validator {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(Validator.class);

    private Schema schema;

    private List<ValidationErrorMessage> errors;
    private List<Object> errorContext;

    /**
     * Initialize a JSON Validation (used primarily for the DSL)
     *
     * @param schema The Schema describing the DSL
     */
    public Validator(Schema schema) {
        this.schema = schema;
    }

    public void validate(JsonNode document) throws IllegalArgumentException, ValidationException {
        // TODO faire un version de validate où l'on peut choisir le type de départ (et pas forcément ce qui est marqué
        // par root)
        ParametersChecker.checkParameterDefault("document", document);
        String root = schema.getRoot();
        TypeDef rootType = schema.getDefinitions().get(root);
        errors = new ArrayList<>();
        errorContext = new ArrayList<>();

        Property syntheticRoot = new Property();
        syntheticRoot.setName(root);
        syntheticRoot.setType(rootType);

        validate(syntheticRoot, document, null);

        if (!errors.isEmpty()) {

            StringBuilder builder = new StringBuilder();
            for (ValidationErrorMessage error : errors) {
                builder.append(error.toString());
                builder.append("\n");
            }
            throw new ValidationException(
                VitamCodeHelper.toVitamError(VitamCode.GLOBAL_INVALID_DSL, builder.toString()));
        }
    }

    private void validate(Property property, JsonNode node, Consumer<String> fieldReport) {
        LOGGER.debug("DSL Parsing: " + property + " --- on: " + node);
        ParametersChecker.checkParameterDefault("property", property);
        ParametersChecker.checkParameterDefault("node", node);
        Consumer<String> localFieldReport;

        // Analyze from the point of view of the data
        Set<String> fields;

        if (fieldReport == null && node.isObject()) {
            // No need to report. We do local check.
            fields = new HashSet<>();
            for (Iterator<String> it = node.fieldNames(); it.hasNext();) {
                fields.add(it.next());
            }
            localFieldReport = fields::remove;
        } else {
            // We report field validation to upper level
            localFieldReport = fieldReport;
            fields = null;
        }

        if (node.isArray() || node.isObject()) {
            checkSize(property, node);
        }

        // Analyze from the point of view of the schema
        TypeDef type = property.getType();
        switch (type.getKind()) {
            case OBJECT:
                validateObject(property, node, localFieldReport);
                break;
            case ARRAY:
                validateArray(property, node); // no fieldReport, it consume all
                break;
            case REFERENCE:
                validateReference(property, node, localFieldReport);
                break;
            case UNION:
                validateUnion(property, node, localFieldReport);
                break;
            case KEY_CHOICE:
                validateKeyChoice(property, node, localFieldReport);
                break;
            case TYPE_CHOICE:
                validateTypeChoice(property, node, localFieldReport);
                break;
            case ANY_KEY:
                validateAnyKey(property, node, localFieldReport);
                break;
            case ENUM:
                validateEnum(property, node);
                break;
            default:
                throw new IllegalStateException();
        }

        if (fields != null) {
            // Check report
            for (String field : fields) {
                reportError(property, node.get(field), ValidationErrorMessage.Code.INVALID_JSON_FIELD, field);
            }
        }
    }

    private void pushContext(Object name) {
        errorContext.add(name);
    }

    private void popContext() {
        errorContext.remove(errorContext.size() - 1);
    }

    private void validateUnion(Property property, JsonNode node, Consumer<String> fieldReport) {
        TDUnion union = property.getType();

        Property expandedProperty = syntheticProperty(property);

        for (TypeDef typeDef : union.getUnion()) {
            expandedProperty.setType(typeDef);

            validate(expandedProperty, node, fieldReport);
        }
    }

    private void validateReference(Property property, JsonNode node, Consumer<String> fieldReport) {
        String reference = property.getType().getReference();
        switch (validatePrimitive(reference, node, fieldReport)) {
            case PRIMITIVE_KO:
                reportError(property, node, ValidationErrorMessage.Code.INVALID_VALUE, node.getNodeType().name());
                break;
            case PRIMITIVE_OK:
                // All goes well, nothing to do
                break;
            case NOT_PRIMITIVE:
                validateNotPrimitive(property, node, fieldReport, reference);
                break;
        }
    }

    private void validateEnum(Property property, JsonNode node) {
        TDEnum enums = property.getType();

        if (!enums.getEnum().contains(node)) {
            reportError(property, node, ValidationErrorMessage.Code.INVALID_VALUE, node.getNodeType().name());
        }
    }

    private void validateNotPrimitive(Property property, JsonNode node, Consumer<String> fieldReport,
        String reference) {
        TypeDef referred = schema.getDefinitions().get(reference);
        if (referred == null) {
            reportError(property, node, ValidationErrorMessage.Code.INVALID_TYPE, reference);
        } else {
            Property expandedProperty = syntheticProperty(property);
            expandedProperty.setType(referred);

            validate(expandedProperty, node, fieldReport);
        }
    }

    private void reportError(Property property, JsonNode node, ValidationErrorMessage.Code code, String message) {
        // errorContext is destructively pushed and popped. So we copy it before exporting to error message.
        List<Object> errorContextCopy = new ArrayList<>(errorContext);

        errors.add(new ValidationErrorMessage(node, property, code, message, errorContextCopy));
    }


    private PrimitiveAnalysis validatePrimitive(String type, JsonNode node, Consumer<String> fieldReport) {
        switch (type) {
            case "guid":
                return fromBoolean(node.isTextual() && isValidGuid(node.textValue()));
            case "integer":
                return fromBoolean(node.isInt());
            case "posinteger":
                return fromBoolean(node.isInt() && node.intValue() >= 0);
            case "string":
                return fromBoolean(node.isTextual());
            case "anyvalue":
                return fromBoolean(node.isValueNode());
            case "anyarray":
                return fromBoolean(node.isArray());
            case "any": {
                consumeAllFields(node, fieldReport);
                return PrimitiveAnalysis.PRIMITIVE_OK;
            }
            default:
                return PrimitiveAnalysis.NOT_PRIMITIVE;
        }
    }

    private void consumeAllFields(JsonNode node, Consumer<String> fieldReport) {
        if (fieldReport != null) {
            for (Iterator<String> it = node.fieldNames(); it.hasNext();) {
                final String name = it.next();
                fieldReport.accept(name);
            }
        }
    }


    private void validateArray(Property property, JsonNode node) {
        if (!node.isArray()) {
            reportError(property, node, ValidationErrorMessage.Code.WRONG_JSON_TYPE, node.getNodeType().name());
            return;
        }

        TDArray type = property.getType();

        Property itemProperty = syntheticProperty(property);
        itemProperty.setType(type.getArray());
        itemProperty.setReportingType(property.getType()); // help type report
        for (JsonNode item : node) {
            validate(itemProperty, item, null);
        }
    }

    private void validateAnyKey(Property property, JsonNode node, Consumer<String> fieldReport) {
        if (!node.isObject()) {
            reportError(property, node, ValidationErrorMessage.Code.WRONG_JSON_TYPE, node.getNodeType().name());
            return;
        }

        TDAnyKey type = property.getType();

        Property itemProperty = syntheticProperty(property);
        itemProperty.setType(type.getAnykey());
        itemProperty.setReportingType(property.getType()); // help type report
        for (Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext();) {
            Map.Entry<String, JsonNode> item = it.next();
            String name = item.getKey();
            pushContext(name);
            fieldReport.accept(name);
            validate(itemProperty, item.getValue(), null);
            popContext();
        }
    }

    private void checkSize(Property property, JsonNode node) {
        TypeDef type = property.getType();

        if (node.size() < type.getMin()) {
            reportError(property, node, ValidationErrorMessage.Code.ELEMENT_TOO_SHORT,
                node.size() + " < " + type.getMin());
        }
        if (node.size() > type.getMax()) {
            reportError(property, node, ValidationErrorMessage.Code.ELEMENT_TOO_LONG,
                node.size() + " > " + type.getMax());
        }
    }


    private void validateTypeChoice(Property property, JsonNode node, Consumer<String> fieldReport) {
        TDTypeChoice type = property.getType();

        TDTypeChoice.TypeName typeName = TDTypeChoice.fromJsonNodeType(node.getNodeType());
        TypeDef choosen = type.getTypechoice().get(typeName);

        if (choosen == null) {
            reportError(property, node, ValidationErrorMessage.Code.NO_VIABLE_ALTERNATIVE, typeName.toString());
        } else {
            Property itemProperty = syntheticProperty(property);
            itemProperty.setType(choosen);

            validate(itemProperty, node, fieldReport);
        }
    }

    private Property syntheticProperty(Property property) {
        Property itemProperty = new Property();
        itemProperty.setName(property.getName());
        itemProperty.setHint(property.getHint());
        return itemProperty;
    }

    private void validateKeyChoice(Property property, JsonNode node, Consumer<String> fieldReport) {
        if (!node.isObject()) {
            reportError(property, node, ValidationErrorMessage.Code.WRONG_JSON_TYPE, node.getNodeType().name());
            return;
        }

        TDKeyChoice keyChoice = property.getType();

        for (Map.Entry<String, Property> entry : keyChoice.getKeychoice().entrySet()) {
            String name = entry.getKey();
            Property subProperty = entry.getValue();

            JsonNode value = node.get(name);
            if (value != null) {
                fieldReport.accept(name);
                pushContext(name);
                validate(subProperty, value, null);
                popContext();
                return; // Only one choice is accepted
            }
            // else next choice
        }
        // In case of error, let field consumption reporter report the error;
    }

    private void validateObject(Property property, JsonNode node, Consumer<String> fieldReport) {
        if (!node.isObject()) {
            reportError(property, node, ValidationErrorMessage.Code.WRONG_JSON_TYPE, node.getNodeType().name());
            return;
        }

        TDObject type = property.getType();

        // We are looking for each property of the schema in the document
        for (Map.Entry<String, Property> entry : type.getObject().entrySet()) {
            String name = entry.getKey();
            Property subProperty = entry.getValue();

            pushContext(name);

            JsonNode value = node.get(name);
            if (value == null) {
                if (!subProperty.isOptional()) {
                    reportError(subProperty, node, ValidationErrorMessage.Code.MANDATORY, null);
                }
                // else nothing to do. It is optional!
            } else {
                fieldReport.accept(name);
                validate(subProperty, value, null);
            }

            popContext();
        }

    }

    private boolean isValidGuid(String guid) {
        try {
            return GUIDReader.getGUID(guid) != null;
        } catch (InvalidGuidOperationException e) {
            return false;
        }
    }


}
