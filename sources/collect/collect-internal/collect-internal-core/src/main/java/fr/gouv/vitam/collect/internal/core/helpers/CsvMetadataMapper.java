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

package fr.gouv.vitam.collect.internal.core.helpers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.json.JsonHandler;
import org.apache.commons.csv.CSVRecord;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

import static fr.gouv.vitam.common.SedaConstants.TAG_RULE_HOLD;

public class CsvMetadataMapper {

    public static final String CONTENT = "Content.";
    public static final String MANAGEMENT = "Management.";

    private static final Pattern ARRAY_PATTERN = Pattern.compile("(.+)\\[\\d+]$");

    private static final List<String> RULES_TYPES = List.of(
        SedaConstants.TAG_RULE_STORAGE,
        SedaConstants.TAG_RULE_APPRAISAL,
        SedaConstants.TAG_RULE_ACCESS,
        SedaConstants.TAG_RULE_DISSEMINATION,
        SedaConstants.TAG_RULE_REUSE,
        SedaConstants.TAG_RULE_CLASSIFICATION,
        TAG_RULE_HOLD
    );

    private CsvMetadataMapper() {}

    public static void fixSpecificManagementSedaFields(JsonNode unit) {
        for (String rule : RULES_TYPES) {
            final JsonNode node = unit.at("/#management/" + rule + "/Inheritance/PreventRulesId");
            if (node != null && node != MissingNode.getInstance() && !node.isArray()) {
                ((ObjectNode) unit.at("/#management/" + rule + "/Inheritance")).set(
                        "PreventRulesId",
                        JsonHandler.createArrayNode().add(node)
                    );
            }
        }
    }

    public static void unflatSingleElementInArrays(@Nonnull ObjectNode node) {
        List<String> toRemove = new ArrayList<>();
        Map<String, JsonNode> toAdd = new HashMap<>();
        final Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            var e = fields.next();
            Matcher matcher = ARRAY_PATTERN.matcher(e.getKey());
            if (matcher.find()) {
                final long count = StreamSupport.stream(
                    Spliterators.spliteratorUnknownSize(node.fieldNames(), Spliterator.ORDERED),
                    false
                )
                    .filter(key -> key.startsWith(e.getKey().replaceAll("\\[\\d+]$", "")) && key.endsWith("]"))
                    .count();
                if (count == 1) { // there is only one element
                    toAdd.put(matcher.group(1), e.getValue());
                    toRemove.add(e.getKey());
                }
            }
        }

        toAdd.forEach(node::set);
        toRemove.forEach(node::remove);
    }

    public static void mapManagement(ObjectNode node, List<String> headerNames, CSVRecord record) {
        headerNames
            .stream()
            .filter(e -> e.startsWith(MANAGEMENT))
            .filter(e -> !record.get(e).isEmpty())
            .forEach(path -> {
                final String value = record.get(path);
                final String rule = path.replaceAll("^Management\\.(\\w+)\\.(.+)$", "$1");
                if (RULES_TYPES.contains(rule)) {
                    final String field = path.replaceAll("Management\\." + rule + "\\.(\\w+)$", "$1");
                    switch (field) {
                        case "PreventInheritance":
                        case "RefNonRuleId":
                        case "FinalAction":
                            path = path
                                .replace("." + "PreventInheritance", ".Inheritance.PreventInheritance")
                                .replaceFirst("\\.RefNonRuleId$", ".Inheritance.PreventRulesId[0]");
                            break;
                        default:
                            path = path
                                .replaceAll(
                                    "Management\\." + rule + "\\.(\\w+)$",
                                    "Management\\." + rule + "\\.Rules\\.0\\.$1"
                                )
                                .replaceAll(
                                    "Management\\." + rule + "\\.(.+)\\.(\\d+)$",
                                    "Management\\." + rule + "\\.Rules\\.$2\\.$1"
                                );
                    }
                    node.put(parseManagementHeader(path), value);
                }
            });
    }

    private static String parseManagementHeader(String str) {
        return str
            .replaceFirst(MANAGEMENT, "#management.")
            .replaceAll("\\.(\\d+)$", "[$1]")
            .replaceAll("\\.(\\d+)\\.", "[$1].");
    }
}
