/*******************************************************************************
 * This file is part of Vitam Project.
 * 
 * Copyright Vitam (2012, 2015)
 *
 * This software is governed by the CeCILL 2.1 license under French law and
 * abiding by the rules of distribution of free software. You can use, modify
 * and/ or redistribute the software under the terms of the CeCILL license as
 * circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify
 * and redistribute granted by the license, users are provided only with a
 * limited warranty and the software's author, the holder of the economic
 * rights, and the successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with
 * loading, using, modifying and/or developing or reproducing the software by
 * the user in light of its specific status of free software, that may mean that
 * it is complicated to manipulate, and that also therefore means that it is
 * reserved for developers and experienced professionals having in-depth
 * computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling
 * the security of their systems and/or data to be ensured and, more generally,
 * to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.core.database.collections.translator.mongodb;

import static com.mongodb.client.model.Updates.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.bson.conversions.Bson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.mongodb.BasicDBObject;

import fr.gouv.vitam.builder.request.construct.action.Action;
import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.UPDATEACTION;
import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.UPDATEACTIONARGS;
import fr.gouv.vitam.parser.request.parser.GlobalDatasParser;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;

/**
 * UpdateAction to MongoDB
 *
 */
public class UpdateActionToMongodb {

    /**
     * 
     * @param action
     * @return the associated MongoDB BSON request
     * @throws InvalidParseOperationException
     */
    public static Bson getCommand(final Action action)
            throws InvalidParseOperationException {
        UPDATEACTION req = action.getUPDATEACTION();
        JsonNode content = action.getCurrentAction().get(req.exactToken());
        switch (req) {
            case ADD: {
                final Entry<String, JsonNode> element =
                        JsonHandler.checkUnicity(req.exactToken(), content);
                String fieldName = element.getKey();
                ArrayNode array = (ArrayNode) element.getValue()
                        .get(UPDATEACTIONARGS.EACH.exactToken());
                List<Object> list = new ArrayList<>(array.size());
                Iterator<JsonNode> iterator = array.elements();
                while (iterator.hasNext()) {
                    list.add(GlobalDatasParser.getValue(iterator.next()));
                }
                return addEachToSet(fieldName, list);
            }
            case INC: {
                final Entry<String, JsonNode> element =
                        JsonHandler.checkUnicity(req.exactToken(), content);
                return inc(element.getKey(), element.getValue().asLong());
            }
            case MIN: {
                final Entry<String, JsonNode> element =
                        JsonHandler.checkUnicity(req.exactToken(), content);
                return min(element.getKey(),
                        GlobalDatasParser.getValue(element.getValue()));
            }
            case MAX: {
                final Entry<String, JsonNode> element =
                        JsonHandler.checkUnicity(req.exactToken(), content);
                return max(element.getKey(),
                        GlobalDatasParser.getValue(element.getValue()));
            }
            case POP: {
                final Entry<String, JsonNode> element =
                        JsonHandler.checkUnicity(req.exactToken(), content);
                if (element.getValue().asInt() < 0) {
                    return popFirst(element.getKey());
                }
                return popLast(element.getKey());
            }
            case PULL: {
                final Entry<String, JsonNode> element =
                        JsonHandler.checkUnicity(req.exactToken(), content);
                String fieldName = element.getKey();
                ArrayNode array = (ArrayNode) element.getValue()
                        .get(UPDATEACTIONARGS.EACH.exactToken());
                List<Object> list = new ArrayList<>(array.size());
                Iterator<JsonNode> iterator = array.elements();
                while (iterator.hasNext()) {
                    list.add(GlobalDatasParser.getValue(iterator.next()));
                }
                return pullAll(fieldName, list);
            }
            case PUSH: {
                final Entry<String, JsonNode> element =
                        JsonHandler.checkUnicity(req.exactToken(), content);
                String fieldName = element.getKey();
                ArrayNode array = (ArrayNode) element.getValue()
                        .get(UPDATEACTIONARGS.EACH.exactToken());
                List<Object> list = new ArrayList<>(array.size());
                Iterator<JsonNode> iterator = array.elements();
                while (iterator.hasNext()) {
                    list.add(GlobalDatasParser.getValue(iterator.next()));
                }
                return pushEach(fieldName, list);
            }
            case RENAME: {
                final Entry<String, JsonNode> element =
                        JsonHandler.checkUnicity(req.exactToken(), content);
                return rename(element.getKey(), element.getValue().asText());
            }
            case SET: {
                Iterator<Entry<String, JsonNode>> iterator = content.fields();
                BasicDBObject doc = new BasicDBObject();
                while (iterator.hasNext()) {
                    Entry<String, JsonNode> element = iterator.next();
                    doc.append(element.getKey(),
                            GlobalDatasParser.getValue(element.getValue()));
                }
                return new BasicDBObject(req.exactToken(), doc);
            }
            case UNSET: {
                Iterator<JsonNode> iterator = content.elements();
                BasicDBObject doc = new BasicDBObject();
                while (iterator.hasNext()) {
                    JsonNode element = iterator.next();
                    doc.append(element.asText(), "");
                }
                return new BasicDBObject(req.exactToken(), doc);
            }
            default:
        }
        throw new InvalidParseOperationException("Invalid command: " + req.exactToken());
    }
}
