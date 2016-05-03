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

import static com.mongodb.client.model.Filters.*;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.bson.BSON;
import org.bson.conversions.Bson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.mongodb.BasicDBObject;

import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.QUERY;
import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.RANGEARGS;
import fr.gouv.vitam.builder.request.construct.query.BooleanQuery;
import fr.gouv.vitam.builder.request.construct.query.Query;
import fr.gouv.vitam.parser.request.parser.GlobalDatasParser;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

/**
 * Query to MongoDB
 *
 */
public class QueryToMongodb {
    private static final VitamLogger LOGGER =
            VitamLoggerFactory.getInstance(QueryToMongodb.class);

    private QueryToMongodb() {
    }

    /**
     * @param field
     * @param roots
     * @return the filter associated with the roots
     * @throws InvalidParseOperationException 
     */
    public static Bson getRoots(final String field, final Set<String> roots) throws InvalidParseOperationException {
        if (roots.size() == 1) {
            return eq(field, roots.iterator().next());
        }
        String[] values = new String[roots.size()];
        int i = 0;
        for (String node : roots) {
            values[i++] = node;
        }
        return in(field, values);
    }

    /**
     * Merge a request and a root filter
     * 
     * @param command
     * @param roots
     * @return the complete request
     */
    public static Bson getFullCommand(final Bson command, final Bson roots) {
        return and(command, roots);
    }

    /**
     * 
     * @param query
     * @return the associated MongoDB BSON request
     * @throws InvalidParseOperationException
     */
    public static Bson getCommand(final Query query)
            throws InvalidParseOperationException {
        QUERY req = query.getQUERY();
        JsonNode content = query.getNode(req.exactToken());
        switch (req) {
            case and:
            case not:
            case or: {
                // using array of sub queries
                BooleanQuery nthrequest = (BooleanQuery) query;
                List<Query> sub = nthrequest.getQueries();
                switch (req) {
                    case and:
                        return and(getCommands(sub));
                    case not:
                        if (sub.size() == 1) {
                            return nor(getCommand(sub.get(0)));
                        }
                        return nor(and(getCommands(sub)));
                    case or:
                        return or(getCommands(sub));
                    default:
                }
                break;
            }
            case exists:
            case missing: {
                return exists(content.asText(), req == QUERY.exists);
            }
            case flt:
            case mlt: {
                throw new InvalidParseOperationException(
                        "Command not allowed with MongoDB: " + req.exactToken());
            }
            case match:
            case match_phrase:
            case match_phrase_prefix:
            case prefix: {
                throw new InvalidParseOperationException(
                        "Command not allowed with MongoDB: " + req.exactToken());
            }
            case nin:
            case in: {
                final Entry<String, JsonNode> element =
                        JsonHandler.checkUnicity(req.exactToken(), content);
                ArrayNode array = (ArrayNode) element.getValue();
                Object[] values = new Object[array.size()];
                int i = 0;
                for (JsonNode node : array) {
                    values[i++] = GlobalDatasParser.getValue(node);
                }
                switch (req) {
                    case nin:
                        return nin(element.getKey(), values);
                    case in:
                        return in(element.getKey(), values);
                    default:
                }
                break;
            }
            case range: {
                final Entry<String, JsonNode> element =
                        JsonHandler.checkUnicity(req.exactToken(), content);
                String var = element.getKey();
                BasicDBObject range = new BasicDBObject();
                for (final Iterator<Entry<String, JsonNode>> iterator =
                        element.getValue().fields(); iterator.hasNext();) {
                    final Entry<String, JsonNode> requestItem = iterator.next();
                    try {
                        final String key = requestItem.getKey();
                        if (key.startsWith("$")) {
                            RANGEARGS.valueOf(key.substring(1));
                            range.append(key,
                                    GlobalDatasParser.getValue(requestItem.getValue()));
                        } else {
                            throw new InvalidParseOperationException(
                                    "Invalid Range request command: " + requestItem);
                        }
                    } catch (final IllegalArgumentException e) {
                        throw new InvalidParseOperationException(
                                "Invalid Range request command: " + requestItem, e);
                    }
                }
                return new BasicDBObject(var, range);
            }
            case regex: {
                final Entry<String, JsonNode> element =
                        JsonHandler.checkUnicity(req.exactToken(), content);
                return regex(element.getKey(), element.getValue().asText());
            }
            case term: {
                Iterator<Entry<String, JsonNode>> iterator = content.fields();
                BasicDBObject bson = new BasicDBObject();
                while (iterator.hasNext()) {
                    Entry<String, JsonNode> element = iterator.next();
                    bson.append(element.getKey(),
                            GlobalDatasParser.getValue(element.getValue()));
                }
                return bson;
            }
            case wildcard: {
                final Entry<String, JsonNode> element =
                        JsonHandler.checkUnicity(req.exactToken(), content);
                String value = element.getValue().asText();
                value = value.replace('?', '.').replace("*", ".*");
                return regex(element.getKey(), value);
            }
            case eq:
            case gt:
            case gte:
            case lt:
            case lte:
            case ne: {
                final Entry<String, JsonNode> element =
                        JsonHandler.checkUnicity(req.exactToken(), content);
                final Object value = GlobalDatasParser.getValue(element.getValue());
                switch (req) {
                    case eq:
                        return eq(element.getKey(), value);
                    case gt:
                        return gt(element.getKey(), value);
                    case gte:
                        return gte(element.getKey(), value);
                    case lt:
                        return lt(element.getKey(), value);
                    case lte:
                        return lte(element.getKey(), value);
                    case ne:
                        return ne(element.getKey(), value);
                    default:
                }
                break;
            }
            case search: {
                throw new InvalidParseOperationException(
                        "Command not allowed with MongoDB: " + req.exactToken());
            }
            case isNull: {
                return new BasicDBObject().append(content.asText(),
                        new BasicDBObject("$type", BSON.NULL));
            }
            case size: {
                final Entry<String, JsonNode> element =
                        JsonHandler.checkUnicity(req.exactToken(), content);
                return size(element.getKey(), element.getValue().asInt());
            }
            case geometry:
            case box:
            case polygon:
            case center:
            case geoIntersects:
            case geoWithin:
            case near: {
                throw new InvalidParseOperationException(
                        "Unimplemented command: " + req.exactToken());
            }
            case path:  {
                ArrayNode array = (ArrayNode) content;
                String[] values = new String[array.size()];
                int i = 0;
                for (JsonNode node : array) {
                    values[i++] = node.asText();
                }
                return in("_id", values);
            }
            default:
        }
        throw new InvalidParseOperationException("Invalid command: " + req.exactToken());
    }

    protected static Iterable<Bson> getCommands(final List<Query> queries) {
        return new Iterable<Bson>() {
            @Override
            public Iterator<Bson> iterator() {
                return new Iterator<Bson>() {
                    private int rank = 0;

                    @Override
                    public Bson next() {
                        try {
                            return getCommand(queries.get(rank++));
                        } catch (InvalidParseOperationException e) {
                            // error but ignore ?
                            LOGGER.error("Bad request", e);
                            return null;
                        }
                    }

                    @Override
                    public boolean hasNext() {
                        return rank < queries.size();
                    }
                };
            }
        };
    }

}
