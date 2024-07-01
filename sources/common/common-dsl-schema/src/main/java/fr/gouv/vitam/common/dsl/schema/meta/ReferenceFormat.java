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
import com.fasterxml.jackson.databind.node.NullNode;
import fr.gouv.vitam.common.dsl.schema.ValidationErrorMessage;
import fr.gouv.vitam.common.exception.InvalidGuidOperationException;
import fr.gouv.vitam.common.guid.GUIDReader;

import java.util.function.Consumer;

import static fr.gouv.vitam.common.dsl.schema.meta.PrimitiveAnalysis.NOT_PRIMITIVE;
import static fr.gouv.vitam.common.dsl.schema.meta.PrimitiveAnalysis.fromBoolean;

public class ReferenceFormat extends Format {

    private String type;
    private Format referred;

    /**
     * Accessor for Jackson
     * set the map of the properties allowed for the object.
     */
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public void setMax(int max) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setMin(Integer min) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void validate(JsonNode node, Consumer<String> fieldReport, ValidatorEngine validator) {
        if (referred != null) {
            validateNotPrimitive(node, fieldReport, validator);
        } else {
            switch (validatePrimitive(type, node, fieldReport)) {
                case PRIMITIVE_KO:
                    validator.reportError(
                        this,
                        node,
                        ValidationErrorMessage.Code.INVALID_VALUE,
                        node.getNodeType().name()
                    );
                    break;
                case PRIMITIVE_OK:
                    // All goes well, nothing to do
                    break;
                case NOT_PRIMITIVE:
                    throw new RuntimeException("Internal inconsistency in reference resolution");
            }
        }
    }

    @Override
    public void walk(Consumer<Format> consumer) {
        consumer.accept(this);
    }

    private void validateNotPrimitive(JsonNode node, Consumer<String> fieldReport, ValidatorEngine validator) {
        validator.validate(referred, node, fieldReport);
    }

    private PrimitiveAnalysis validatePrimitive(String typeName, JsonNode node, Consumer<String> fieldReport) {
        switch (typeName) {
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

    private boolean isValidGuid(String guid) {
        try {
            return GUIDReader.getGUID(guid) != null;
        } catch (InvalidGuidOperationException e) {
            return false;
        }
    }

    @Override
    public String debugInfo() {
        if (referred == null) return type;
        else return referred.debugInfo();
    }

    @Override
    protected void resolve(Schema schema) {
        Consumer<String> reportNothing = string -> {
            // do nothing
        };

        // Test if the reference points to a primitive type (testing with a null value), should return PRIMITIVE_KO
        if (validatePrimitive(type, NullNode.getInstance(), reportNothing) == NOT_PRIMITIVE) {
            referred = schema.getType(type);
            if (referred == null) {
                throw new IllegalArgumentException("Type " + type + " invalid: not a primitive nor a composite type");
            }
        }
    }
}
