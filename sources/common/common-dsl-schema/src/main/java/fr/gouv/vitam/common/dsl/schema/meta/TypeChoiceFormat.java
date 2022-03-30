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
import fr.gouv.vitam.common.dsl.schema.ValidationErrorMessage;

import java.util.Map;
import java.util.function.Consumer;

import static fr.gouv.vitam.common.dsl.schema.meta.JsonTypeName.fromJsonNodeType;

public class TypeChoiceFormat extends Format {

    @Override
    protected void resolve(Schema schema) {
        for (Format format : choices.values()) {
            format.setReportingType(this);
        }
    }

    private Map<JsonTypeName, Format> choices;

    /**
     * Accessor for Jackson
     * set the map of the json types allowed for the object.
     */
    public void setChoices(
        Map<JsonTypeName, Format> choices) {
        this.choices = choices;
    }

    @Override
    public void validate(JsonNode node, Consumer<String> fieldReport, ValidatorEngine validator) {

        JsonTypeName typeName = fromJsonNodeType(node.getNodeType());
        Format choosen = choices.get(typeName);

        if (choosen == null) {
            validator.reportError(this, node, ValidationErrorMessage.Code.NO_VIABLE_ALTERNATIVE, typeName.toString());
        } else {
            validator.validate(choosen, node, fieldReport);
        }
    }

    @Override
    public void walk(Consumer<Format> consumer) {
        consumer.accept(this);
        for (Format format : choices.values()) {
            format.walk(consumer);
        }
    }

    @Override
    public String debugInfo() {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        boolean notFirst = false;
        for (Map.Entry<JsonTypeName, Format> entry : choices.entrySet()) {
            if (notFirst)
                builder.append("}|{");
            builder.append(entry.getKey());
            builder.append("-> ");
            builder.append(entry.getValue().debugInfo());
            notFirst = true;
        }
        builder.append("}");
        return builder.toString();
    }

}
