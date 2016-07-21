/*******************************************************************************
 * This file is part of Vitam Project.
 *
 * Copyright Vitam (2012, 2016)
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL license as circulated
 * by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL license and that you
 * accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.common.database.translators.mongodb;

import static com.mongodb.client.model.Updates.addEachToSet;
import static com.mongodb.client.model.Updates.inc;
import static com.mongodb.client.model.Updates.max;
import static com.mongodb.client.model.Updates.min;
import static com.mongodb.client.model.Updates.popFirst;
import static com.mongodb.client.model.Updates.popLast;
import static com.mongodb.client.model.Updates.pullAll;
import static com.mongodb.client.model.Updates.pushEach;
import static com.mongodb.client.model.Updates.rename;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.bson.conversions.Bson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.mongodb.BasicDBObject;

import fr.gouv.vitam.common.database.builder.query.action.Action;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.UPDATEACTION;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.UPDATEACTIONARGS;
import fr.gouv.vitam.common.database.parser.request.GlobalDatasParser;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;

/**
 * UpdateAction to MongoDB
 *
 */
public class UpdateActionToMongodb {

    private UpdateActionToMongodb() {
        // Empty constructor
    }

    /**
     *
     * @param action
     * @return the associated MongoDB BSON request
     * @throws InvalidParseOperationException
     */
    public static Bson getCommand(final Action action)
        throws InvalidParseOperationException {
        final UPDATEACTION req = action.getUPDATEACTION();
        final JsonNode content = action.getCurrentAction().get(req.exactToken());
        switch (req) {
            case ADD:
                return addCommand(req, content);
            case INC:
                return incCommand(req, content);
            case MIN:
                return minCommand(req, content);
            case MAX:
                return maxCommand(req, content);
            case POP:
                return popCommand(req, content);
            case PULL:
                return pullCommand(req, content);
            case PUSH:
                return pushCommand(req, content);
            case RENAME:
                return renameCommand(req, content);
            case SET:
                return setCommand(req, content);
            case UNSET:
                return unsetCommand(req, content);
            default:
        }
        throw new InvalidParseOperationException("Invalid command: " + req.exactToken());
    }

    /**
     * @param req
     * @param content
     * @return the Unset Command
     */
    private static Bson unsetCommand(final UPDATEACTION req, final JsonNode content) {
        final Iterator<JsonNode> iterator = content.elements();
        final BasicDBObject doc = new BasicDBObject();
        while (iterator.hasNext()) {
            final JsonNode element = iterator.next();
            doc.append(element.asText(), "");
        }
        return new BasicDBObject(req.exactToken(), doc);
    }

    /**
     * @param req
     * @param content
     * @return the Set Command
     * @throws InvalidParseOperationException
     */
    private static Bson setCommand(final UPDATEACTION req, final JsonNode content)
        throws InvalidParseOperationException {
        final Iterator<Entry<String, JsonNode>> iterator = content.fields();
        final BasicDBObject doc = new BasicDBObject();
        while (iterator.hasNext()) {
            final Entry<String, JsonNode> element = iterator.next();
            doc.append(element.getKey(),
                GlobalDatasParser.getValue(element.getValue()));
        }
        return new BasicDBObject(req.exactToken(), doc);
    }

    /**
     * @param req
     * @param content
     * @return the Rename Command
     * @throws InvalidParseOperationException
     */
    private static Bson renameCommand(final UPDATEACTION req, final JsonNode content)
        throws InvalidParseOperationException {
        final Entry<String, JsonNode> element =
            JsonHandler.checkUnicity(req.exactToken(), content);
        return rename(element.getKey(), element.getValue().asText());
    }

    /**
     * @param req
     * @param content
     * @return the Push Command
     * @throws InvalidParseOperationException
     */
    private static Bson pushCommand(final UPDATEACTION req, final JsonNode content)
        throws InvalidParseOperationException {
        final Entry<String, JsonNode> element =
            JsonHandler.checkUnicity(req.exactToken(), content);
        final String fieldName = element.getKey();
        final ArrayNode array = (ArrayNode) element.getValue()
            .get(UPDATEACTIONARGS.EACH.exactToken());
        final List<Object> list = new ArrayList<>(array.size());
        final Iterator<JsonNode> iterator = array.elements();
        while (iterator.hasNext()) {
            list.add(GlobalDatasParser.getValue(iterator.next()));
        }
        return pushEach(fieldName, list);
    }

    /**
     * @param req
     * @param content
     * @return the Pull Command
     * @throws InvalidParseOperationException
     */
    private static Bson pullCommand(final UPDATEACTION req, final JsonNode content)
        throws InvalidParseOperationException {
        final Entry<String, JsonNode> element =
            JsonHandler.checkUnicity(req.exactToken(), content);
        final String fieldName = element.getKey();
        final ArrayNode array = (ArrayNode) element.getValue()
            .get(UPDATEACTIONARGS.EACH.exactToken());
        final List<Object> list = new ArrayList<>(array.size());
        final Iterator<JsonNode> iterator = array.elements();
        while (iterator.hasNext()) {
            list.add(GlobalDatasParser.getValue(iterator.next()));
        }
        return pullAll(fieldName, list);
    }

    /**
     * @param req
     * @param content
     * @return the Pop Command
     * @throws InvalidParseOperationException
     */
    private static Bson popCommand(final UPDATEACTION req, final JsonNode content)
        throws InvalidParseOperationException {
        final Entry<String, JsonNode> element =
            JsonHandler.checkUnicity(req.exactToken(), content);
        if (element.getValue().asInt() < 0) {
            return popFirst(element.getKey());
        }
        return popLast(element.getKey());
    }

    /**
     * @param req
     * @param content
     * @return the Max Command
     * @throws InvalidParseOperationException
     */
    private static Bson maxCommand(final UPDATEACTION req, final JsonNode content)
        throws InvalidParseOperationException {
        final Entry<String, JsonNode> element =
            JsonHandler.checkUnicity(req.exactToken(), content);
        return max(element.getKey(),
            GlobalDatasParser.getValue(element.getValue()));
    }

    /**
     * @param req
     * @param content
     * @return the Min Command
     * @throws InvalidParseOperationException
     */
    private static Bson minCommand(final UPDATEACTION req, final JsonNode content)
        throws InvalidParseOperationException {
        final Entry<String, JsonNode> element =
            JsonHandler.checkUnicity(req.exactToken(), content);
        return min(element.getKey(),
            GlobalDatasParser.getValue(element.getValue()));
    }

    /**
     * @param req
     * @param content
     * @return the Inc Command
     * @throws InvalidParseOperationException
     */
    private static Bson incCommand(final UPDATEACTION req, final JsonNode content)
        throws InvalidParseOperationException {
        final Entry<String, JsonNode> element =
            JsonHandler.checkUnicity(req.exactToken(), content);
        return inc(element.getKey(), element.getValue().asLong());
    }

    /**
     * @param req
     * @param content
     * @return the Add Command
     * @throws InvalidParseOperationException
     */
    private static Bson addCommand(final UPDATEACTION req, final JsonNode content)
        throws InvalidParseOperationException {
        final Entry<String, JsonNode> element =
            JsonHandler.checkUnicity(req.exactToken(), content);
        final String fieldName = element.getKey();
        final ArrayNode array = (ArrayNode) element.getValue()
            .get(UPDATEACTIONARGS.EACH.exactToken());
        final List<Object> list = new ArrayList<>(array.size());
        final Iterator<JsonNode> iterator = array.elements();
        while (iterator.hasNext()) {
            list.add(GlobalDatasParser.getValue(iterator.next()));
        }
        return addEachToSet(fieldName, list);
    }
}
