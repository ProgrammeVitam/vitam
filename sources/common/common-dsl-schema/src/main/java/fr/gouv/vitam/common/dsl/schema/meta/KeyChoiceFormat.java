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

public class KeyChoiceFormat extends Format {

    public KeyChoiceFormat() {
        setMin(1); // TODO Document the fact that min is 1 by default for keychoice
    }

    @Override
    protected void resolve(Schema schema) {
        for (Map.Entry<String, Format> entry : choices.entrySet()) {
            entry.getValue().setName(entry.getKey());
        }
    }

    private Map<String, Format> choices;

    /**
     * Accessor for Jackson
     * set the map of the properties allowed for the object.
     */
    public Map<String, Format> getChoices() {
        return choices;
    }

    public void setChoices(Map<String, Format> choices) {
        this.choices = choices;
    }

    @Override
    public void validate(JsonNode node, Consumer<String> fieldReport, ValidatorEngine validator) {
        if (!node.isObject()) {
            validator.reportError(this, node, ValidationErrorMessage.Code.WRONG_JSON_TYPE, node.getNodeType().name());
            return;
        }

        for (Map.Entry<String, Format> entry : choices.entrySet()) {
            String name = entry.getKey();
            Format subProperty = entry.getValue();

            JsonNode value = null;
            if (name.equals("$subobject") && node.get(name) != null && node.get(name).fields().hasNext()) {
                value = node.get(name).fields().next().getValue();
            } else {
                value = node.get(name);
            }
            if (value != null) {
                fieldReport.accept(name);
                validator.pushContext(name);
                validator.validate(subProperty, value, null);
                validator.popContext();
                return; // Only one choice is accepted
            }
            // else next choice
        }
        // In case of error, let field consumption reporter report the error;
    }

    @Override
    public void walk(Consumer<Format> consumer) {
        consumer.accept(this);
        for (Format property : choices.values()) {
            property.walk(consumer);
        }
    }

    @Override
    public String debugInfo() {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        boolean notFirst = false;
        for (Map.Entry<String, Format> entry : choices.entrySet()) {
            if (notFirst) builder.append("}|{");
            builder.append(entry.getKey());
            builder.append(": ...");
            notFirst = true;
        }
        builder.append("}");
        return builder.toString();
    }
}
