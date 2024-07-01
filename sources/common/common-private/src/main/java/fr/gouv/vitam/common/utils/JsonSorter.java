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
package fr.gouv.vitam.common.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Helper class for sorting json array of objects by keys
 *
 * For test usages only. Not designed for production code
 */
@VisibleForTesting
public final class JsonSorter {

    private JsonSorter() {
        // Static helper
    }

    /**
     * Sorts a arrays of objects based on sort keys
     * For test usages only. Not designed for production code
     */
    @VisibleForTesting
    public static void sortJsonEntriesByKeys(JsonNode jsonNode, List<String> orderedKeys) {
        /*
         * Missing field < null field < field="A" < field = "Z"
         *
         * eg. [{"key1":"a","key2":"c"},{"key1":"a","key2":"b"}] -> [{"key1":"a","key2":"b"},{"key1":"a","key2":"c"}]
         */

        if (jsonNode == null) return;

        for (Iterator<JsonNode> it = jsonNode.elements(); it.hasNext();) {
            JsonNode value = it.next();
            sortJsonEntriesByKeys(value, orderedKeys);
        }

        if (jsonNode.isArray() && jsonNode.size() > 1 && jsonNode.get(0).isObject()) {
            ArrayNode arrayNode = (ArrayNode) jsonNode;

            List<ObjectNode> items = new ArrayList<>();
            jsonNode.forEach(i -> items.add((ObjectNode) i));

            items.sort((node1, node2) -> {
                for (String sortKey : orderedKeys) {
                    JsonNode jsonValue1 = node1.get(sortKey);
                    JsonNode jsonValue2 = node2.get(sortKey);

                    if (jsonValue1 == null && jsonValue2 == null) {
                        // Skip...
                    } else if (jsonValue1 == null) {
                        return -1;
                    } else if (jsonValue2 == null) {
                        return 1;
                    } else {
                        if (jsonValue1.isNull() && jsonValue2.isNull()) {
                            // Skip
                        } else if (jsonValue1.isNull()) {
                            return -1;
                        } else if (jsonValue2.isNull()) {
                            return 1;
                        } else {
                            int sort = jsonValue1.asText().compareTo(node2.get(sortKey).asText());
                            if (sort != 0) return sort;
                        }
                    }
                }

                return 1;
            });

            arrayNode.removeAll();
            arrayNode.addAll(items);
        }
    }
}
