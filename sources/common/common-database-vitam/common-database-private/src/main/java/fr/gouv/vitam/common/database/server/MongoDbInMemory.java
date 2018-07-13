/**
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
 */

package fr.gouv.vitam.common.database.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.common.database.builder.query.action.Action;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.database.parser.query.ParserTokens;
import fr.gouv.vitam.common.database.parser.request.AbstractParser;
import fr.gouv.vitam.common.database.parser.request.GlobalDatasParser;
import fr.gouv.vitam.common.database.parser.request.adapter.VarNameAdapter;
import fr.gouv.vitam.common.database.parser.request.multiple.UpdateParserMultiple;
import fr.gouv.vitam.common.database.parser.request.single.UpdateParserSingle;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.QueryPattern;

/**
 * Tools to update a Mongo document (as json) with a dsl query.
 */
public class MongoDbInMemory {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MongoDbInMemory.class);

    private final JsonNode originalDocument;

    private JsonNode updatedDocument;

    /**
     * @param originalDocument
     */
    public MongoDbInMemory(JsonNode originalDocument) {
        this.originalDocument = originalDocument;
        updatedDocument = originalDocument.deepCopy();
    }

    /**
     * Update the originalDocument with the given request. If the Document is a MetadataDocument (Unit/ObjectGroup) it
     * should use a MultipleQuery Parser
     * @param request The given update request
     * @param isMultiple true if the UpdateParserMultiple must be used (Unit/ObjectGroup)
     * @param varNameAdapter VarNameAdapter to use
     * @return the updated document
     * @throws InvalidParseOperationException
     */
    public JsonNode getUpdateJson(JsonNode request, boolean isMultiple, VarNameAdapter varNameAdapter)
        throws InvalidParseOperationException {
        final AbstractParser<?> parser;

        if (isMultiple) {
            parser = new UpdateParserMultiple(varNameAdapter);
        } else {
            parser = new UpdateParserSingle(varNameAdapter);
        }

        parser.parse(request);

        return getUpdateJson(parser);
    }

    /**
     * Update the originalDocument with the given parser (containing the request)
     * @param requestParser The given parser containing the update request
     * @return the updated document
     * @throws InvalidParseOperationException
     */
    public JsonNode getUpdateJson(AbstractParser<?> requestParser) throws InvalidParseOperationException {
        List<Action> actions = requestParser.getRequest().getActions();
        if (actions == null || actions.isEmpty()) {
            LOGGER.info("No action on request");
            return updatedDocument;
        } else {
            for (Action action : actions) {
                final BuilderToken.UPDATEACTION req = action.getUPDATEACTION();
                final JsonNode content = action.getCurrentAction().get(req.exactToken());
                switch (req) {
                    case ADD:
                        add(req, content);
                        break;
                    case INC:
                        inc(req, content);
                        break;
                    case MIN:
                        min(req, content);
                        break;
                    case MAX:
                        max(req, content);
                        break;
                    case POP:
                        pop(req, content);
                        break;
                    case PULL:
                        pull(req, content);
                        break;
                    case PUSH:
                        push(req, content);
                        break;
                    case RENAME:
                        rename(req, content);
                        break;
                    case SET:
                        set(content);
                        break;
                    case UNSET:
                        unset(content);
                        break;
                    case SETREGEX:
                        setregex(content);
                        break;
                    default:
                        break;
                }
            }
        }
        return updatedDocument;
    }

    /**
     * Reset the updatedDocument with the original values
     */
    public void resetUpdatedAU() {
        updatedDocument = originalDocument.deepCopy();
    }

    private void inc(final BuilderToken.UPDATEACTION req, final JsonNode content)
        throws InvalidParseOperationException {
        final Map.Entry<String, JsonNode> element = JsonHandler.checkUnicity(req.exactToken(), content);
        final String fieldName = element.getKey();
        Double nodeValue = getNumberValue(req.name(), fieldName);

        JsonNode actionValue = element.getValue();
        if (!actionValue.isNumber()) {
            throw new InvalidParseOperationException("[" + "INC" + "]Action argument (" + actionValue +
                ") cannot be converted as number for field " + fieldName);
        }

        String[] fieldNamePath = fieldName.split("[.]");
        String lastNodeName = fieldNamePath[fieldNamePath.length - 1];
        ((ObjectNode) JsonHandler.getParentNodeByPath(updatedDocument, fieldName, false)).put(lastNodeName,
            element.getValue().asLong() + nodeValue);
    }

    private void unset(final JsonNode content) {
        final Iterator<JsonNode> iterator = content.elements();
        while (iterator.hasNext()) {
            final JsonNode element = iterator.next();
            String fieldName = element.asText();
            JsonNode node = JsonHandler.getParentNodeByPath(updatedDocument, fieldName, false);
            String[] fieldNamePath = fieldName.split("[.]");
            String lastNodeName = fieldNamePath[fieldNamePath.length - 1];
            ((ObjectNode) node).remove(lastNodeName);
        }
    }

    private void set(final JsonNode content) throws InvalidParseOperationException {
        final Iterator<Map.Entry<String, JsonNode>> iterator = content.fields();
        while (iterator.hasNext()) {
            final Map.Entry<String, JsonNode> element = iterator.next();
            if (ParserTokens.PROJECTIONARGS.isAnArray(element.getKey())) {
                ArrayNode arrayNode = GlobalDatasParser.getArray(element.getValue());
                JsonHandler.setNodeInPath((ObjectNode) updatedDocument, element.getKey(), arrayNode, true);
            } else {
                JsonHandler.setNodeInPath((ObjectNode) updatedDocument, element.getKey(), element.getValue(), true);
            }
        }
    }

    private void setregex(final JsonNode content) throws InvalidParseOperationException {
        QueryPattern queryPattern = JsonHandler.getFromJsonNodeLowerCamelCase(content, QueryPattern.class);
        String stringToSearch = originalDocument.get(queryPattern.getTarget()).asText();
        // The pattern to search for
        Pattern pattern = Pattern.compile(queryPattern.getControlPattern());
        Matcher matcher = pattern.matcher(stringToSearch);
        StringBuffer sb = new StringBuffer();
        // find & replace all matches
        while (matcher.find()) {
            matcher.appendReplacement(sb, queryPattern.getUpdatePattern());
        }
        if (org.apache.commons.lang3.StringUtils.isNotBlank(sb)) {
            matcher.appendTail(sb);
            ((ObjectNode) JsonHandler.getParentNodeByPath(updatedDocument, queryPattern.getTarget(), false)).
                put(queryPattern.getTarget(), sb.toString());
        }
    }

    private void min(final BuilderToken.UPDATEACTION req, final JsonNode content)
        throws InvalidParseOperationException {
        final Map.Entry<String, JsonNode> element = JsonHandler.checkUnicity(req.exactToken(), content);
        final String fieldName = element.getKey();
        Double nodeValue = getNumberValue(req.name(), fieldName);

        JsonNode actionValue = element.getValue();
        if (!actionValue.isNumber()) {
            throw new InvalidParseOperationException("[" + "MIN" + "]Action argument (" + actionValue +
                ") cannot be converted as number for field " + fieldName);
        }

        String[] fieldNamePath = fieldName.split("[.]");
        String lastNodeName = fieldNamePath[fieldNamePath.length - 1];
        ((ObjectNode) JsonHandler.getParentNodeByPath(updatedDocument, fieldName, false)).put(lastNodeName,
            Math.min(element.getValue().asDouble(), nodeValue));
    }

    private void max(final BuilderToken.UPDATEACTION req, final JsonNode content)
        throws InvalidParseOperationException {
        final Map.Entry<String, JsonNode> element = JsonHandler.checkUnicity(req.exactToken(), content);
        final String fieldName = element.getKey();
        Double nodeValue = getNumberValue(req.name(), fieldName);

        JsonNode actionValue = element.getValue();
        if (!actionValue.isNumber()) {
            throw new InvalidParseOperationException("[" + "MAX" + "]Action argument (" + actionValue +
                ") cannot be converted as number for field " + fieldName);
        }

        String[] fieldNamePath = fieldName.split("[.]");
        String lastNodeName = fieldNamePath[fieldNamePath.length - 1];
        ((ObjectNode) JsonHandler.getParentNodeByPath(updatedDocument, fieldName, false)).put(lastNodeName,
            Math.max(element.getValue().asDouble(), nodeValue));
    }

    private void rename(final BuilderToken.UPDATEACTION req, final JsonNode content)
        throws InvalidParseOperationException {
        final Map.Entry<String, JsonNode> element = JsonHandler.checkUnicity(req.exactToken(), content);
        final String fieldName = element.getKey();
        JsonNode value = JsonHandler.getNodeByPath(updatedDocument, fieldName, false);
        if (value == null) {
            throw new InvalidParseOperationException(
                "[" + "RENAME" + "]Can't rename field " + fieldName + " because it doesn't exist");
        }

        JsonNode parent = JsonHandler.getParentNodeByPath(updatedDocument, fieldName, false);
        String[] fieldNamePath = fieldName.split("[.]");
        String lastNodeName = fieldNamePath[fieldNamePath.length - 1];
        ((ObjectNode) parent).remove(lastNodeName);

        JsonHandler.setNodeInPath((ObjectNode) updatedDocument, element.getValue().asText(), value, true);
    }

    private void push(final BuilderToken.UPDATEACTION req, final JsonNode content)
        throws InvalidParseOperationException {
        final Map.Entry<String, JsonNode> element = JsonHandler.checkUnicity(req.exactToken(), content);
        final String fieldName = element.getKey();
        final ArrayNode array = (ArrayNode) element.getValue().get(BuilderToken.UPDATEACTIONARGS.EACH.exactToken());
        ArrayNode node = (ArrayNode) getArrayValue(req.name(), fieldName);
        final Iterator<JsonNode> iterator = array.elements();
        while (iterator.hasNext()) {
            node.add(iterator.next());
        }
    }

    private void pull(final BuilderToken.UPDATEACTION req, final JsonNode content)
        throws InvalidParseOperationException {
        final Map.Entry<String, JsonNode> element = JsonHandler.checkUnicity(req.exactToken(), content);
        final String fieldName = element.getKey();
        final ArrayNode array = (ArrayNode) element.getValue().get(BuilderToken.UPDATEACTIONARGS.EACH.exactToken());
        ArrayNode node = (ArrayNode) getArrayValue(req.name(), fieldName);
        final List<Integer> indexesToRemove = new ArrayList<>();
        final Iterator<JsonNode> iterator = array.elements();
        // TODO: optimize ! review loop order for the best !
        while (iterator.hasNext()) {
            JsonNode pullValue = iterator.next();
            Iterator<JsonNode> originIt = node.elements();
            int index = 0;
            while (originIt.hasNext()) {
                if (originIt.next().asText().equals(pullValue.asText())) {
                    indexesToRemove.add(index);
                }
                index++;
            }
        }
        Collections.sort(indexesToRemove);
        for (int i = indexesToRemove.size() - 1; i >= 0; i--) {
            node.remove(indexesToRemove.get(i));
        }
    }

    private void add(final BuilderToken.UPDATEACTION req, final JsonNode content)
        throws InvalidParseOperationException {
        final Map.Entry<String, JsonNode> element = JsonHandler.checkUnicity(req.exactToken(), content);
        final String fieldName = element.getKey();
        final ArrayNode array = (ArrayNode) element.getValue().get(BuilderToken.UPDATEACTIONARGS.EACH.exactToken());

        ArrayNode node = (ArrayNode) getArrayValue(req.name(), fieldName);
        final Iterator<JsonNode> iterator = array.elements();

        // TODO: optimize ! review loop order for the best !
        while (iterator.hasNext()) {
            JsonNode newNode = iterator.next();
            Iterator<JsonNode> originIt = node.elements();
            boolean mustAdd = true;
            while (originIt.hasNext()) {
                if (originIt.next().asText().equals(newNode.asText())) {
                    mustAdd = false;
                }
            }
            if (mustAdd) {
                node.add(newNode);
            }
        }
    }

    private void pop(final BuilderToken.UPDATEACTION req, final JsonNode content)
        throws InvalidParseOperationException {
        final Map.Entry<String, JsonNode> element = JsonHandler.checkUnicity(req.exactToken(), content);
        final String fieldName = element.getKey();
        ArrayNode node = (ArrayNode) getArrayValue(req.name(), fieldName);

        JsonNode actionValue = element.getValue();
        if (!actionValue.isNumber()) {
            throw new InvalidParseOperationException("[" + req.name() + "]Action argument (" + actionValue +
                ") cannot be converted as number for field " + fieldName);
        }

        int numberOfPop = Math.abs(actionValue.asInt());
        if (numberOfPop > node.size()) {
            throw new InvalidParseOperationException(
                "Cannot pop " + numberOfPop + "items from the field '" + fieldName + "' because it has less items");
        }
        if (actionValue.asInt() < 0) {
            for (int i = 0; i < numberOfPop; i++) {
                node.remove(0);
            }
        } else {
            for (int i = 0; i < numberOfPop; i++) {
                node.remove(node.size() - 1);
            }
        }
    }

    private double getNumberValue(final String actionName, final String fieldName)
        throws InvalidParseOperationException {
        JsonNode node = JsonHandler.getNodeByPath(updatedDocument, fieldName, false);
        if (node == null || !node.isNumber()) {
            String message = "This field '" + fieldName + "' is not a number, cannot do '" + actionName +
                "' action: " + node + " or unknow fieldName";
            LOGGER.error(message);
            throw new InvalidParseOperationException(message);
        }
        return node.asDouble();
    }

    private JsonNode getArrayValue(final String actionName, final String fieldName)
        throws InvalidParseOperationException {
        JsonNode node = JsonHandler.getNodeByPath(updatedDocument, fieldName, false);
        if (node == null || node instanceof NullNode) {
            LOGGER.info("Action '" + actionName + "' in item previously null '" + fieldName + "' or unknow");
            ObjectNode updatedDocumentAsObject = (ObjectNode) updatedDocument;
            updatedDocumentAsObject.set(fieldName, JsonHandler.createArrayNode());
            return updatedDocument.get(fieldName);
        }
        if (!node.isArray()) {
            String message =
                "This field '" + fieldName + "' is not an array, cannot do '" + actionName + "' action";
            LOGGER.error(message);
            throw new InvalidParseOperationException(message);
        }
        return node;
    }
}
