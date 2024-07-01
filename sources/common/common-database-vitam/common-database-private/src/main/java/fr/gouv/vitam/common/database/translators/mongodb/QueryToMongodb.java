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
package fr.gouv.vitam.common.database.translators.mongodb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.mongodb.BasicDBObject;
import com.mongodb.client.model.Filters;
import fr.gouv.vitam.common.database.builder.query.BooleanQuery;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.QUERY;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.RANGEARGS;
import fr.gouv.vitam.common.database.parser.request.GlobalDatasParser;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import org.bson.BsonType;
import org.bson.conversions.Bson;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.exists;
import static com.mongodb.client.model.Filters.gt;
import static com.mongodb.client.model.Filters.gte;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Filters.lt;
import static com.mongodb.client.model.Filters.lte;
import static com.mongodb.client.model.Filters.ne;
import static com.mongodb.client.model.Filters.nin;
import static com.mongodb.client.model.Filters.nor;
import static com.mongodb.client.model.Filters.or;
import static com.mongodb.client.model.Filters.regex;
import static com.mongodb.client.model.Filters.size;

/**
 * Query to MongoDB
 */
public class QueryToMongodb {

    private static final String INVALID_RANGE_REQUEST_COMMAND = "Invalid Range request command: ";
    private static final String COMMAND_NOT_ALLOWED_WITH_MONGO_DB = "Command not allowed with MongoDB: ";

    private QueryToMongodb() {
        // Empty constructor
    }

    /**
     * @param field String
     * @param roots Set of String
     * @return the filter associated with the roots
     */
    public static Bson getRoots(final String field, final Collection<String> roots) {
        if (roots.size() == 1) {
            return eq(field, roots.iterator().next());
        }
        final String[] values = new String[roots.size()];
        int i = 0;
        for (final String node : roots) {
            values[i++] = node;
        }
        return in(field, values);
    }

    /**
     * Merge a request and a root filter
     *
     * @param command Bson
     * @param roots Bson
     * @return the complete request
     */
    public static Bson getFullCommand(final Bson command, final Bson roots) {
        return and(command, roots);
    }

    /**
     * @param query Query
     * @return the associated MongoDB BSON request
     * @throws InvalidParseOperationException if query could not parse to command
     */
    public static Bson getCommand(final Query query) throws InvalidParseOperationException {
        final QUERY req = query.getQUERY();
        final JsonNode content = query.getNode(req.exactToken());
        switch (req) {
            case AND:
            case NOT:
            case OR:
                return booleanCommand(query, req);
            case EXISTS:
            case MISSING:
                return exists(content.asText(), req == QUERY.EXISTS);
            case FLT:
            case MLT:
            case MATCH:
            case MATCH_ALL:
            case MATCH_PHRASE:
            case MATCH_PHRASE_PREFIX:
            case SEARCH:
            case SUBOBJECT:
                throw new InvalidParseOperationException(COMMAND_NOT_ALLOWED_WITH_MONGO_DB + req.exactToken());
            case NIN:
            case IN:
                return inCommand(req, content);
            case RANGE:
                return rangeCommand(req, content);
            case REGEX:
                return regexCommand(req, content);
            case TERM:
                return termCommand(content);
            case WILDCARD:
                return wildcardCommand(req, content);
            case EQ:
            case GT:
            case GTE:
            case LT:
            case LTE:
            case NE:
                return comparatorCommand(req, content);
            case ISNULL:
                return isNullCommand(content);
            case SIZE:
                return sizeCommand(req, content);
            case GEOMETRY:
            case BOX:
            case POLYGON:
            case CENTER:
            case GEOINTERSECTS:
            case GEOWITHIN:
            case PATH:
                return pathCommand(content);
            case NOP:
                return new BasicDBObject();
            default:
        }
        throw new InvalidParseOperationException("Invalid command: " + req.exactToken());
    }

    /**
     * @param content JsonNode
     * @return the IsNull Command
     */
    private static Bson isNullCommand(final JsonNode content) {
        return Filters.type(content.asText(), BsonType.NULL);
    }

    /**
     * @param content JsonNode
     * @return the Path Command
     */
    private static Bson pathCommand(final JsonNode content) {
        final ArrayNode array = (ArrayNode) content;
        final String[] values = new String[array.size()];
        int i = 0;
        for (final JsonNode node : array) {
            values[i++] = node.asText();
        }
        return in("_id", values);
    }

    /**
     * @param req QUERY
     * @param content JsonNode
     * @return the Size Command
     * @throws InvalidParseOperationException if check unicity is in error
     */
    private static Bson sizeCommand(final QUERY req, final JsonNode content) throws InvalidParseOperationException {
        final Entry<String, JsonNode> element = JsonHandler.checkUnicity(req.exactToken(), content);
        return size(element.getKey(), element.getValue().asInt());
    }

    /**
     * @param req QUERY
     * @param content JsonNode
     * @return the Comparator Command
     * @throws InvalidParseOperationException check unicity is in error
     */
    private static Bson comparatorCommand(final QUERY req, final JsonNode content)
        throws InvalidParseOperationException {
        final Entry<String, JsonNode> element = JsonHandler.checkUnicity(req.exactToken(), content);
        final Object value = GlobalDatasParser.getValue(element.getValue());
        switch (req) {
            case EQ:
                return eq(element.getKey(), value);
            case GT:
                return gt(element.getKey(), value);
            case GTE:
                return gte(element.getKey(), value);
            case LT:
                return lt(element.getKey(), value);
            case LTE:
                return lte(element.getKey(), value);
            case NE:
            default:
                return ne(element.getKey(), value);
        }
    }

    /**
     * @param req QUERY
     * @param content JsonNode
     * @return the Wildcard Command
     * @throws InvalidParseOperationException if check unicity is in error
     */
    private static Bson wildcardCommand(final QUERY req, final JsonNode content) throws InvalidParseOperationException {
        final Entry<String, JsonNode> element = JsonHandler.checkUnicity(req.exactToken(), content);
        String value = element.getValue().asText();
        value = value.replace('?', '.').replace("*", ".*");
        return regex(element.getKey(), value);
    }

    /**
     * @param content JsonNode
     * @return the Term Command
     * @throws InvalidParseOperationException if could not get JSON value
     */
    private static Bson termCommand(final JsonNode content) throws InvalidParseOperationException {
        final Iterator<Entry<String, JsonNode>> iterator = content.fields();
        final BasicDBObject bson = new BasicDBObject();
        while (iterator.hasNext()) {
            final Entry<String, JsonNode> element = iterator.next();
            bson.append(element.getKey(), GlobalDatasParser.getValue(element.getValue()));
        }
        return bson;
    }

    /**
     * @param req QUERY
     * @param content JsonNode
     * @return the Regex Command
     * @throws InvalidParseOperationException if check unicity is in error
     */
    private static Bson regexCommand(final QUERY req, final JsonNode content) throws InvalidParseOperationException {
        final Entry<String, JsonNode> element = JsonHandler.checkUnicity(req.exactToken(), content);
        return regex(element.getKey(), element.getValue().asText());
    }

    /**
     * @param req QUERY
     * @param content JsonNode
     * @return The Range command
     * @throws InvalidParseOperationException if could not get JSON value
     */
    private static Bson rangeCommand(final QUERY req, final JsonNode content) throws InvalidParseOperationException {
        final Entry<String, JsonNode> element = JsonHandler.checkUnicity(req.exactToken(), content);
        final String var = element.getKey();
        final BasicDBObject range = new BasicDBObject();
        for (final Iterator<Entry<String, JsonNode>> iterator = element.getValue().fields(); iterator.hasNext();) {
            final Entry<String, JsonNode> requestItem = iterator.next();
            try {
                final String key = requestItem.getKey();
                if (key.startsWith(BuilderToken.DEFAULT_PREFIX)) {
                    RANGEARGS.valueOf(key.substring(1).toUpperCase());
                    range.append(key, GlobalDatasParser.getValue(requestItem.getValue()));
                } else {
                    throw new InvalidParseOperationException(INVALID_RANGE_REQUEST_COMMAND + requestItem);
                }
            } catch (final IllegalArgumentException e) {
                throw new InvalidParseOperationException(INVALID_RANGE_REQUEST_COMMAND + requestItem, e);
            }
        }
        return new BasicDBObject(var, range);
    }

    /**
     * @param req QUERY
     * @param content JsonNode
     * @return the In Command
     * @throws InvalidParseOperationException if check unicity is in error or could not get JSON value
     */
    private static Bson inCommand(final QUERY req, final JsonNode content) throws InvalidParseOperationException {
        final Entry<String, JsonNode> element = JsonHandler.checkUnicity(req.exactToken(), content);
        final ArrayNode array = (ArrayNode) element.getValue();
        final Object[] values = new Object[array.size()];
        int i = 0;
        for (final JsonNode node : array) {
            values[i++] = GlobalDatasParser.getValue(node);
        }
        switch (req) {
            case NIN:
                return nin(element.getKey(), values);
            case IN:
            default:
                return in(element.getKey(), values);
        }
    }

    /**
     * @param query Query
     * @param req QUERY
     * @return the Boolean Command
     * @throws InvalidParseOperationException if could not compare two queries
     */
    private static Bson booleanCommand(final Query query, final QUERY req) throws InvalidParseOperationException {
        // using array of sub queries
        final BooleanQuery nthrequest = (BooleanQuery) query;
        final List<Query> sub = nthrequest.getQueries();
        switch (req) {
            case AND:
                return and(getCommands(sub));
            case NOT:
                if (sub.size() == 1) {
                    return nor(getCommand(sub.get(0)));
                }
                return nor(and(getCommands(sub)));
            case OR:
            default:
                return or(getCommands(sub));
        }
    }

    protected static Iterable<Bson> getCommands(final List<Query> queries) {
        return queries
            .stream()
            .map(query -> {
                try {
                    return getCommand(query);
                } catch (InvalidParseOperationException e) {
                    throw new RuntimeException("Invalid query", e);
                }
            })
            .collect(Collectors.toList());
    }
}
