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
package fr.gouv.vitam.worker.core.plugin.reclassification.dsl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import org.apache.commons.collections4.SetUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ReclassificationRequestDslParser {

    private static final String QUERY = "$query";
    private static final String ROOTS = "$roots";

    public ParsedReclassificationDslRequest parseReclassificationRequest(JsonNode reclassificationDslJson)
        throws InvalidParseOperationException {
        if (!reclassificationDslJson.isArray() || reclassificationDslJson.size() == 0) {
            throw new InvalidParseOperationException("Expected array of reclassification updates DSLs");
        }

        List<ParsedReclassificationDslRequestEntry> entries = new ArrayList<>();

        for (JsonNode singleReclassificationDslJson : reclassificationDslJson) {
            SelectMultiQuery selectMultiQuery = parseSelectMultiQuery(singleReclassificationDslJson);

            Set<String> attachments = parseReclassificationDslAttachments(singleReclassificationDslJson);
            Set<String> detachments = parseReclassificationDslDetachments(singleReclassificationDslJson);

            entries.add(new ParsedReclassificationDslRequestEntry(selectMultiQuery, attachments, detachments));
        }

        return new ParsedReclassificationDslRequest(entries);
    }

    private SelectMultiQuery parseSelectMultiQuery(JsonNode singleReclassificationDslJson)
        throws InvalidParseOperationException {
        JsonNode query = singleReclassificationDslJson.get(QUERY);
        JsonNode roots = singleReclassificationDslJson.get(ROOTS);

        ObjectNode selectDsl = JsonHandler.createObjectNode();
        selectDsl.set(QUERY, query);
        if (roots != null) {
            selectDsl.set(ROOTS, roots);
        }

        SelectParserMultiple selectParserMultiple = new SelectParserMultiple();
        selectParserMultiple.parse(selectDsl);

        return selectParserMultiple.getRequest();
    }

    private Set<String> parseReclassificationDslAttachments(JsonNode singleReclassificationDslJson)
        throws InvalidParseOperationException {
        return parseReclassificationDslAction(
            singleReclassificationDslJson,
            BuilderToken.UPDATEACTION.ADD.exactToken()
        );
    }

    private Set<String> parseReclassificationDslDetachments(JsonNode singleReclassificationDslJson)
        throws InvalidParseOperationException {
        return parseReclassificationDslAction(
            singleReclassificationDslJson,
            BuilderToken.UPDATEACTION.PULL.exactToken()
        );
    }

    private Set<String> parseReclassificationDslAction(JsonNode singleReclassificationDslJson, String action)
        throws InvalidParseOperationException {
        Set<String> result = null;

        JsonNode actionNode = singleReclassificationDslJson.get("$action");
        if (null == actionNode || !actionNode.isArray()) {
            throw new InvalidParseOperationException("Expected action array");
        }
        for (JsonNode actionEntry : actionNode) {
            if (!actionEntry.isObject()) {
                throw new InvalidParseOperationException("Expected object action entry");
            }
            for (Iterator<String> it = actionEntry.fieldNames(); it.hasNext();) {
                String actionName = it.next();
                if (action.equals(actionName)) {
                    if (result != null) {
                        throw new InvalidParseOperationException("Duplicate action " + actionName);
                    }
                    result = parseReclassificationAction(actionName, actionEntry);
                }
            }
        }

        if (result == null) {
            return SetUtils.emptySet();
        }
        return result;
    }

    private Set<String> parseReclassificationAction(String actionName, JsonNode actionEntry)
        throws InvalidParseOperationException {
        Set<String> result = new HashSet<>();
        JsonNode actionJsonNode = actionEntry.get(actionName);
        if (!actionJsonNode.isObject()) {
            throw new InvalidParseOperationException("Expected object node for action " + actionName);
        }
        ObjectNode actionObjectNode = (ObjectNode) actionJsonNode;

        if (actionObjectNode.size() != 1) {
            throw new InvalidParseOperationException("Expected single field name for action " + actionName);
        }
        String fieldName = actionObjectNode.fieldNames().next();
        if (!VitamFieldsHelper.unitups().equals(fieldName)) {
            throw new InvalidParseOperationException("Invalid field name for reclassification action " + fieldName);
        }
        JsonNode idsJsonNode = actionObjectNode.get(fieldName);
        if (!idsJsonNode.isArray() || idsJsonNode.size() == 0) {
            throw new InvalidParseOperationException("Expected array node of unit ids");
        }
        for (JsonNode jsonNode : idsJsonNode) {
            if (!jsonNode.isTextual()) {
                throw new InvalidParseOperationException("Expected unit id node");
            }
            result.add(jsonNode.asText());
        }
        return result;
    }
}
