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
package fr.gouv.vitam.common.dsl.schema.meta;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.dsl.schema.ValidationErrorMessage;
import fr.gouv.vitam.common.dsl.schema.ValidationException;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * DSL Validator from a Schema. WARNING: not thread safe. (note: Schema is thread safe)
 */
public class ValidatorEngine {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ValidatorEngine.class);

    private Schema schema;

    private List<ValidationErrorMessage> errors;
    private List<Object> errorContext;

    /**
     * Initialize a JSON Validation (used primarily for the DSL)
     *
     * @param schema The Schema describing the DSL
     */
    public ValidatorEngine(Schema schema) {
        this.schema = schema;
    }

    public void validate(JsonNode document, String root) throws IllegalArgumentException, ValidationException {
        ParametersChecker.checkParameterDefault("document", document);
        Format rootType = schema.getType(root);
        if (rootType == null) {
            throw new IllegalArgumentException("Unknown Schema type: " + root);
        }
        errors = new ArrayList<>();
        errorContext = new ArrayList<>();

        validate(rootType, document, null);

        if (!errors.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            for (ValidationErrorMessage error : errors) {
                builder.append(error.toString());
                builder.append("\n");
            }
            throw new ValidationException(
                VitamCodeHelper.toVitamError(VitamCode.GLOBAL_INVALID_DSL, builder.toString())
            );
        }
    }

    protected void validate(Format propertyFormat, JsonNode node, Consumer<String> fieldReport) {
        if (propertyFormat.getName() == null) throw new IllegalStateException();
        LOGGER.debug("DSL Parsing: " + propertyFormat + " --- on: " + node);
        ParametersChecker.checkParameterDefault("property", propertyFormat);
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
        } else if (fieldReport == null) {
            fields = new HashSet<>();
            localFieldReport = fields::remove;
        } else {
            // We report field validation to upper level
            localFieldReport = fieldReport;
            fields = null;
        }

        if (node.isArray() || node.isObject()) {
            checkSize(propertyFormat, node);
        }

        // Analyze from the point of view of the schema
        propertyFormat.validate(node, localFieldReport, this);

        if (fields != null) {
            // Check report
            for (String field : fields) {
                reportError(propertyFormat, node.get(field), ValidationErrorMessage.Code.INVALID_JSON_FIELD, field);
            }
        }
    }

    protected void pushContext(Object name) {
        errorContext.add(name);
    }

    protected void popContext() {
        errorContext.remove(errorContext.size() - 1);
    }

    protected void reportError(Format propertyFormat, JsonNode node, ValidationErrorMessage.Code code, String message) {
        // errorContext is destructively pushed and popped. So we copy it before exporting to error message.
        List<Object> errorContextCopy = new ArrayList<>(errorContext);

        errors.add(new ValidationErrorMessage(node, propertyFormat, code, message, errorContextCopy));
    }

    private void checkSize(Format format, JsonNode node) {
        if (node.size() < format.getMin()) {
            reportError(
                format,
                node,
                ValidationErrorMessage.Code.ELEMENT_TOO_SHORT,
                node.size() + " < " + format.getMin()
            );
        }
        if (node.size() > format.getMax()) {
            reportError(
                format,
                node,
                ValidationErrorMessage.Code.ELEMENT_TOO_LONG,
                node.size() + " > " + format.getMax()
            );
        }
    }
}
