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

package fr.gouv.vitam.collect.internal.helpers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.wnameless.json.unflattener.JsonUnflattener;
import fr.gouv.vitam.collect.internal.exception.CsvParseException;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.security.SanityChecker;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FilenameUtils;

import javax.annotation.Nonnull;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

import static fr.gouv.vitam.common.SedaConstants.TAG_RULE_HOLD;

public class CsvMetadataMapper {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CsvMetadataMapper.class);

    private static final String CONTENT = "Content.";
    private static final String MANAGEMENT = "Management.";
    private static final String FILE_FIELD = "File";
    private static final String MESSAGE_DIGEST = "MessageDigest";
    private static final String ALGORITHM = "Algorithm";

    private static final String ARRAY_REGEX = "\\.[\\d+]";
    private static final Pattern ARRAY_PATTERN = Pattern.compile("(.+)\\[\\d+\\]$");
    private static final Pattern ATTR_PATTERN = Pattern.compile("(.+)=\"(.+)\"");

    private static final List<String> RULES_TYPES =
        List.of(SedaConstants.TAG_RULE_STORAGE, SedaConstants.TAG_RULE_APPRAISAL, SedaConstants.TAG_RULE_ACCESS,
            SedaConstants.TAG_RULE_DISSEMINATION, SedaConstants.TAG_RULE_REUSE, SedaConstants.TAG_RULE_CLASSIFICATION,
            TAG_RULE_HOLD);

    private CsvMetadataMapper() {

    }

    public static Map.Entry<String, JsonNode> map(CSVRecord record, List<String> headerNames) {
        ObjectNode node = JsonHandler.createObjectNode();
        try {
            SanityChecker.checkJsonAll(JsonHandler.unprettyPrint(record.toMap()));
            mapContent(node, headerNames, record);
            mapManagement(node, headerNames, record);
            unflatSingleElementInArrays(node);
            final String json = JsonUnflattener.unflatten(node.toString());
            final JsonNode unit = JsonHandler.getFromString(json);
            fixSpeceficSedaFields(unit);
            return new AbstractMap.SimpleEntry<>(FilenameUtils.separatorsToUnix(record.get(FILE_FIELD)), unit);
        } catch (Exception e) {
            LOGGER.debug("Cannot parse json entry {}", node.toString());
            throw new CsvParseException(e);
        }
    }

    private static void fixSpeceficSedaFields(JsonNode unit) {
        for (String rule : RULES_TYPES) {
            final JsonNode node = unit.at("/#management/" + rule + "/Inheritance/PreventRulesId");
            if (node != null && node != MissingNode.getInstance() && !node.isArray()) {
                ((ObjectNode) unit.at("/#management/" + rule + "/Inheritance")).set("PreventRulesId",
                    JsonHandler.createArrayNode().add(node));
            }
        }
        for (String item : List.of("Event")) {
            final JsonNode node = unit.get(item);
            if (node != null) {
                if (!node.isArray()) {
                    ((ObjectNode) unit).set(item, JsonHandler.createArrayNode().add(node));
                }
                for (int i = 0; i < unit.get(item).size(); i++) {
                    final JsonNode subNode = unit.at("/" + item + "/" + i + "/" + "linkingAgentIdentifier");
                    if (subNode != null && subNode != MissingNode.getInstance()) {
                        if (!subNode.isArray()) {
                            ((ObjectNode) unit.at("/" + item + "/" + i)).set("linkingAgentIdentifier",
                                JsonHandler.createArrayNode().add(subNode));
                        }
                    }
                }
            }
        }

        for (String item : List.of("Addressee", "Agent", "AuthorizedAgent", "Recipient", "Sender", "Transmitter",
            "Writer")) {
            final JsonNode node = unit.get(item);
            if (node != null) {
                if (!node.isArray()) {
                    ((ObjectNode) unit).set(item, JsonHandler.createArrayNode().add(node));
                }
                arrayOfAgentTypeAndSignatureAndValidation(unit, "/" + item);
            }
        }

        for (String item : List.of("Signature")) {
            final JsonNode node = unit.get(item);
            if (node != null) {
                if (!node.isArray()) {
                    ((ObjectNode) unit).set(item, JsonHandler.createArrayNode().add(node));
                }
                for (int i = 0; i < unit.get(item).size(); i++) {
                    for (String subItem : List.of("Signer")) {
                        final JsonNode subNode = unit.at("/" + item + "/" + i + "/" + subItem);
                        if (subNode != null && subNode != MissingNode.getInstance()) {
                            if (!subNode.isArray()) {
                                ((ObjectNode) unit.at("/" + item + "/" + i)).set(subItem,
                                    JsonHandler.createArrayNode().add(subNode));
                            }
                            arrayOfAgentTypeAndSignatureAndValidation(unit, "/" + item + "/" + i + "/" + subItem);
                        }
                    }
                    AgentTypeAndSignatureAndValidationFix(unit, "/" + item + "/" + i + "/Validator");
                }
            }
        }

        for (String item : List.of("Spatial", "Temporal", "Juridictional")) {
            final JsonNode node = unit.at("/Coverage/" + item);
            if (node != null && node != MissingNode.getInstance() && !node.isArray()) {
                ((ObjectNode) unit.at("/Coverage")).set(item,
                    JsonHandler.createArrayNode().add(node));
            }
        }

        for (String item : List.of("IsVersionOf", "Replaces", "Requires", "IsPartOf", "References")) {
            final JsonNode node = unit.at("/RelatedObjectReference/" + item);
            if (node != null && node != MissingNode.getInstance() && !node.isArray()) {
                ((ObjectNode) unit.at("/RelatedObjectReference")).set(item,
                    JsonHandler.createArrayNode().add(node));
            }
        }

        {
            final JsonNode node = unit.at("/SystemId");
            if (node != null && node != MissingNode.getInstance() && !node.isArray()) {
                ((ObjectNode) unit).set("SystemId", JsonHandler.createArrayNode().add(node));
            }
        }
    }

    private static void arrayOfAgentTypeAndSignatureAndValidation(JsonNode unit, String path) {
        for (int i = 0; i < unit.at(path).size(); i++) {
            AgentTypeAndSignatureAndValidationFix(unit, path + "/" + i);
        }
    }

    private static void AgentTypeAndSignatureAndValidationFix(JsonNode unit, String path) {
        for (String subItem : List.of("Activity", "Function", "Identifier", "Mandate", "Nationality",
            "Position", "Role")) {
            final JsonNode subNode = unit.at(path + "/" + subItem);
            if (subNode != null && subNode != MissingNode.getInstance() && !subNode.isArray()) {
                ((ObjectNode) unit.at(path)).set(subItem,
                    JsonHandler.createArrayNode().add(subNode));
            }
        }
    }

    private static void unflatSingleElementInArrays(@Nonnull ObjectNode node) {
        List<String> toRemove = new ArrayList<>();
        Map<String, JsonNode> toAdd = new HashMap<>();
        final Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            var e = fields.next();
            Matcher matcher = ARRAY_PATTERN.matcher(e.getKey());
            if (matcher.find()) {
                final long count =
                    StreamSupport.stream(Spliterators.spliteratorUnknownSize(node.fieldNames(), Spliterator.ORDERED),
                            false)
                        .filter(key -> key.startsWith(e.getKey().replaceAll("\\[\\d+\\]$", "")) && key.endsWith("]"))
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

    private static void mapManagement(ObjectNode node, List<String> headerNames, CSVRecord record) {
        headerNames.stream().filter(e -> e.startsWith(MANAGEMENT)).filter(e -> !record.get(e).isEmpty())
            .forEach(path -> {
                final String value = record.get(path);
                final String rule = path.replaceAll("^Management\\.(\\w+)\\.(.+)$", "$1");
                if (RULES_TYPES.contains(rule)) {
                    final String field = path.replaceAll("Management\\." + rule + "\\.(\\w+)$", "$1");
                    switch (field) {
                        case "PreventInheritance":
                        case "RefNonRuleId":
                        case "FinalAction":
                            path = path.replace("." + "PreventInheritance", ".Inheritance.PreventInheritance")
                                .replaceFirst("\\.RefNonRuleId$", ".Inheritance.PreventRulesId[0]");
                            break;
                        default:
                            path = path.replaceAll("Management\\." + rule + "\\.(\\w+)$",
                                    "Management\\." + rule + "\\.Rules\\.0\\.$1")
                                .replaceAll("Management\\." + rule + "\\.(.+)\\.(\\d+)$",
                                    "Management\\." + rule + "\\.Rules\\.$2\\.$1");
                    }
                    node.put(parseManagementHeader(path), value);
                }
            });
    }

    private static void mapContent(ObjectNode node, List<String> headerNames, CSVRecord record) {
        headerNames.stream().filter(e -> e.startsWith(CONTENT)).filter(e -> !record.get(e).isEmpty()).forEach(e -> {
            final String value = record.get(e);
            if (e.endsWith(".attr")) {
                final String fieldName = e.replaceAll("\\.attr$", "");
                Matcher matcher = ATTR_PATTERN.matcher(value);
                if (matcher.find()) {
                    if (fieldName.startsWith(CONTENT + "Title") || fieldName.startsWith(CONTENT + "Description")) {
                        var field = fieldName.equals(fieldName.replaceAll(ARRAY_REGEX, "_")) ?
                            fieldName + "_" :
                            fieldName.replaceAll(ARRAY_REGEX, "_");
                        ObjectNode obj = (ObjectNode) Objects.requireNonNullElse(node.get(parseHeader(field)),
                            JsonHandler.createObjectNode());
                        obj.set(matcher.group(2), node.get(parseHeader(fieldName)));
                        node.set(parseHeader(field), obj);
                        node.remove(parseHeader(fieldName));
                    } else if (fieldName.equals(CONTENT + "Signature.ReferencedObject.SignedObjectDigest")) {
                        final ObjectNode obj = JsonHandler.createObjectNode();
                        obj.set(MESSAGE_DIGEST, node.get(parseHeader(fieldName)));
                        obj.put(ALGORITHM, matcher.group(2));
                        node.set(parseHeader(fieldName), obj);
                    }
                }
            } else if (e.startsWith(CONTENT + "Event")) {
                final String fieldName = e.replace(CONTENT + "Event.EventIdentifier", CONTENT + "Event.evId")
                    .replace(CONTENT + "Event.EventDateTime", CONTENT + "Event.evDateTime")
                    .replace(CONTENT + "Event.EventDetailData", CONTENT + "Event.evTypeDetail")
                    .replace(CONTENT + "Event.EventDetail", CONTENT + "Event.evDetData")
                    .replace(CONTENT + "Event.EventTypeCode", CONTENT + "Event.evTypeProc")
                    .replace(CONTENT + "Event.EventType", CONTENT + "Event.evType")
                    .replace(CONTENT + "Event.OutcomeDetailMessage", CONTENT + "Event.outMessg")
                    .replace(CONTENT + "Event.OutcomeDetail", CONTENT + "Event.outDetail")
                    .replace(CONTENT + "Event.Outcome", CONTENT + "Event.outcome")
                    .replace(CONTENT + "Event.LinkingAgentIdentifier", CONTENT + "Event.linkingAgentIdentifier")
                    .replaceAll(CONTENT + "Event.(\\d+).EventIdentifier", CONTENT + "Event.$1.evId")
                    .replaceAll(CONTENT + "Event.(\\d+).EventDateTime", CONTENT + "Event.$1.evDateTime")
                    .replaceAll(CONTENT + "Event.(\\d+).EventDetailData", CONTENT + "Event.$1.evTypeDetail")
                    .replaceAll(CONTENT + "Event.(\\d+).EventDetail", CONTENT + "Event.$1.evDetData")
                    .replaceAll(CONTENT + "Event.(\\d+).EventTypeCode", CONTENT + "Event.$1.evTypeProc")
                    .replaceAll(CONTENT + "Event.(\\d+).EventType", CONTENT + "Event.$1.evType")
                    .replaceAll(CONTENT + "Event.(\\d+).OutcomeDetailMessage", CONTENT + "Event.$1.outMessg")
                    .replaceAll(CONTENT + "Event.(\\d+).OutcomeDetail", CONTENT + "Event.$1.outDetail")
                    .replaceAll(CONTENT + "Event.(\\d+).Outcome", CONTENT + "Event.$1.outcome")
                    .replaceAll(CONTENT + "Event.(\\d+).LinkingAgentIdentifier", CONTENT + "Event.$1.linkingAgentIdentifier");

                node.put(parseHeader(fieldName), value);
            } else {
                node.put(parseHeader(e), value);
            }
        });
    }

    private static String parseHeader(String str) {
        return str.replaceFirst(CONTENT, "").replaceAll("\\.(\\d+)$", "[$1]").replaceAll("\\.(\\d+)\\.", "[$1].");
    }

    private static String parseManagementHeader(String str) {
        return str.replaceFirst(MANAGEMENT, "#management.").replaceAll("\\.(\\d+)$", "[$1]")
            .replaceAll("\\.(\\d+)\\.", "[$1].");
    }
}
