/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.json.JsonHandler.JSON_SET_FOR_ACTION_DSL_REGEX;

public class InternalActionKeysRetriever {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(InternalActionKeysRetriever.class);

    private static final String INTERPUNCT = ".";

    private static final String ACTION = "$action";
    private static final String ACTION_KEY_SET = "$set";
    private static final String ACTION_KEY_UNSET = "$unset";

    private static final String ACTION_KEY_SETREGEX = "$setregex";
    private static final String ACTION_SETREGEX_KEY_TARGET = "$target";

    private final String internalPrefix;
    private final String internalPrefixFromAccess;
    private final String internalPrefixForMongo;

    public InternalActionKeysRetriever() {
        this("#", "_", "$");
    }

    @VisibleForTesting
    public InternalActionKeysRetriever(
        String internalPrefix,
        String internalPrefixFromAccess,
        String internalPrefixForMongo
    ) {
        this.internalPrefix = internalPrefix;
        this.internalPrefixFromAccess = internalPrefixFromAccess;
        this.internalPrefixForMongo = internalPrefixForMongo;
    }

    public List<String> getInternalActionKeyFields(JsonNode query) {
        JsonNode action = query.get(ACTION);
        if (isElementEmpty(action)) {
            LOGGER.info("The action has no internal action key fields.");
            return Collections.emptyList();
        }
        return getActionKeyFields(action);
    }

    public List<String> getInternalKeyFields(JsonNode node) {
        return getSetUnsetKeys(node);
    }

    private List<String> getActionKeyFields(JsonNode node) {
        List<String> internalKeyFields = new ArrayList<>();
        if (!node.isArray()) {
            LOGGER.info("The action has no internal action key fields.");
            return Collections.emptyList();
        }

        Iterator<JsonNode> actionElements = node.elements();
        while (actionElements.hasNext()) {
            JsonNode actionElement = actionElements.next();

            Iterator<Entry<String, JsonNode>> fields = actionElement.fields();
            while (fields.hasNext()) {
                internalKeyFields.addAll(getInternalActionKeyElementFields(fields));
            }
        }
        return internalKeyFields;
    }

    private List<String> getInternalActionKeyElementFields(Iterator<Entry<String, JsonNode>> fields) {
        Entry<String, JsonNode> field = fields.next();
        String key = field.getKey();
        if (ACTION_KEY_SET.equalsIgnoreCase(key) || ACTION_KEY_UNSET.equalsIgnoreCase(key)) {
            return getSetUnsetKeys(field.getValue());
        }
        if (key.equalsIgnoreCase(ACTION_KEY_SETREGEX)) {
            return getSetRegexTargetKey(field.getValue());
        }
        return Collections.emptyList();
    }

    private List<String> getSetRegexTargetKey(JsonNode node) {
        // When we use '$setregex' DSL operator we must check if a internal field is use, so we check the value of '$target'.
        JsonNode element = node.get(ACTION_SETREGEX_KEY_TARGET);
        if (isElementEmpty(element)) {
            LOGGER.info("The $target element of $setregex action is empty.");
            return Collections.emptyList();
        }
        String value = element.textValue();
        if (isInternal(value)) {
            return Collections.singletonList(value);
        }
        return Collections.emptyList();
    }

    private boolean isElementEmpty(JsonNode element) {
        return element == null || element.isNull() || element.isMissingNode();
    }

    private List<String> getSetUnsetKeys(JsonNode node) {
        List<String> internalKeyFields = new ArrayList<>();
        if (node.isObject()) {
            Iterator<Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Entry<String, JsonNode> field = fields.next();
                JsonNode value = field.getValue();

                String key = field.getKey();
                List<String> interpunctInternalKeyFields = interpunctInternalKeyFields(key);
                internalKeyFields.addAll(interpunctInternalKeyFields);

                if (isInternal(key)) {
                    internalKeyFields.add(key);
                }

                if (value.isObject() || value.isArray()) {
                    internalKeyFields.addAll(getSetUnsetKeys(value));
                }
            }
        }
        if (node.isArray()) {
            Iterator<JsonNode> elements = node.elements();
            while (elements.hasNext()) {
                JsonNode element = elements.next();
                if (element.isObject() || element.isArray()) {
                    internalKeyFields.addAll(getSetUnsetKeys(element));
                }
            }
        }

        return internalKeyFields;
    }

    private List<String> interpunctInternalKeyFields(String interpunctKey) {
        if (!interpunctKey.contains(INTERPUNCT)) {
            return Collections.emptyList();
        }
        List<String> interpuctKeyFields = Arrays.asList(interpunctKey.split(JSON_SET_FOR_ACTION_DSL_REGEX));
        if (interpuctKeyFields.isEmpty()) {
            return Collections.emptyList();
        }

        return interpuctKeyFields.stream().filter(this::isInternal).collect(Collectors.toList());
    }

    private boolean isInternal(String key) {
        return (
            key.startsWith(internalPrefix) ||
            key.startsWith(internalPrefixFromAccess) ||
            key.startsWith(internalPrefixForMongo)
        );
    }
}
