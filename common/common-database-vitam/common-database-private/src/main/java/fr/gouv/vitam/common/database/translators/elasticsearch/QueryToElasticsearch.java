/*******************************************************************************
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
 *******************************************************************************/
package fr.gouv.vitam.common.database.translators.elasticsearch;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.query.SimpleQueryStringFlag;
import org.elasticsearch.script.Script;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.common.database.builder.query.BooleanQuery;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.QUERY;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.QUERYARGS;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.RANGEARGS;
import fr.gouv.vitam.common.database.parser.query.ParserTokens;
import fr.gouv.vitam.common.database.parser.request.GlobalDatasParser;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;

/**
 * Elasticsearch Translator
 */
public class QueryToElasticsearch {

    private static final String FUZZINESS = "AUTO";

    private QueryToElasticsearch() {
        // Empty constructor
    }

    /**
     * @param field String
     * @param roots Set of String
     * @return the filter associated with the roots
     * @throws InvalidParseOperationException if field is not in roots
     */
    public static QueryBuilder getRoots(final String field, final Set<String> roots)
        throws InvalidParseOperationException {
        final String[] values = new String[roots.size()];
        int i = 0;
        for (final String node : roots) {
            values[i++] = node;
        }
        return QueryBuilders.termQuery(field, values);


    }

    /**
     * Merge a request and a root filter
     *
     * @param command QueryBuilder
     * @param roots QueryBuilder
     * @return the complete request
     */
    public static QueryBuilder getFullCommand(final QueryBuilder command, final QueryBuilder roots) {
        return QueryBuilders.boolQuery()
            .must(command)
            .filter(roots);

        // TODO add TenantId filter
    }

    /**
     *
     * @param query Query
     * @return the associated QueryBuilder
     * @throws InvalidParseOperationException if query could not parse to command
     */
    public static QueryBuilder getCommand(final Query query)
        throws InvalidParseOperationException {
        final QUERY req = query.getQUERY();
        final JsonNode content = query.getNode(req.exactToken());
        switch (req) {
            case AND:
            case NOT:
            case OR:
                return andOrNotCommand(req, query);
            case EXISTS:
            case MISSING:
                return existsMissingCommand(req, content);
            case FLT:
            case MLT:
                return xltCommand(req, content);
            case MATCH:
            case MATCH_PHRASE:
            case MATCH_PHRASE_PREFIX:
            case PREFIX:
                return matchCommand(req, content);
            case SEARCH:
                return searchCommand(req, content);
            case NIN:
            case IN:
                return inCommand(req, content);
            case RANGE:
                return rangeCommand(req, content);
            case REGEX:
                return regexCommand(req, content);
            case TERM:
                return termCommand(req, content);
            case WILDCARD:
                return wildcardCommand(req, content);
            case EQ:
            case NE:
                return eqCommand(req, content);
            case GT:
            case GTE:
            case LT:
            case LTE:
                return compareCommand(req, content);
            case ISNULL:
                return isNullCommand(req, content);
            case SIZE:
                return sizeCommand(req, content);
            case GEOMETRY:
            case BOX:
            case POLYGON:
            case CENTER:
            case GEOINTERSECTS:
            case GEOWITHIN:
            case PATH:
            case NOP:
            default:
        }
        throw new InvalidParseOperationException("Invalid command: " + req.exactToken());
    }

    /**
     * $size : { name : length }
     *
     * @param req QUERY
     * @param content JsonNode
     * @return the size Command
     * @throws InvalidParseOperationException if check unicity is in error
     */
    private static QueryBuilder sizeCommand(final QUERY query, final JsonNode content)
        throws InvalidParseOperationException {
        final Entry<String, JsonNode> element = JsonHandler.checkUnicity(query.exactToken(), content);
        Script script = new Script("doc['" + element.getKey() + "'].values.length == " + element.getValue());
        return QueryBuilders.scriptQuery(script);
    }

    /**
     * $gt : { name : value } $gte : { name : value } $lt : { name : value } $lte : { name : value }
     * 
     * @param req QUERY
     * @param content JsonNode
     * @return the compare Command
     * @throws InvalidParseOperationException if check unicity is in error
     */
    private static QueryBuilder compareCommand(final QUERY query, final JsonNode content)
        throws InvalidParseOperationException {
        final Entry<String, JsonNode> element = JsonHandler.checkUnicity(query.exactToken(), content);

        String key = element.getKey();

        // TODO remove after POC validation all DATE from Vitam code
        JsonNode node = element.getValue().findValue(ParserTokens.QUERYARGS.DATE.exactToken());
        if (node != null) {
            key += "." + ParserTokens.QUERYARGS.DATE.exactToken();
        } else {
            node = element.getValue();
        }

        final Object value = GlobalDatasParser.getValue(node);
        switch (query) {
            case GT:
                return QueryBuilders.rangeQuery(key).gt(value);
            case GTE:
                return QueryBuilders.rangeQuery(key).gte(value);
            case LT:
                return QueryBuilders.rangeQuery(key).lt(value);
            case LTE:
            default:
                return QueryBuilders.rangeQuery(key).lte(value);
        }

    }

    /**
     * $mlt : { $fields : [ name1, name2 ], $like : like_text } $flt : { $fields : [ name1, name2 ], $like : like_text }
     *
     * @param req QUERY
     * @param content JsonNode
     * @return the xlt Command
     * @throws InvalidParseOperationException if check unicity is in error
     */
    private static QueryBuilder xltCommand(final QUERY query, final JsonNode content)
        throws InvalidParseOperationException {
        final ArrayNode fields = (ArrayNode) content.get(QUERYARGS.FIELDS.exactToken());
        final JsonNode like = content.get(QUERYARGS.LIKE.exactToken());
        if (fields == null || like == null) {
            throw new InvalidParseOperationException("Incorrect command: " + query.exactToken() + " : " + query);
        }
        String[] names = new String[fields.size()];
        int i = 0;
        for (JsonNode name : fields) {
            names[i++] = name.toString();
        }
        switch (query) {
            case FLT:
                String slike = like.toString();
                if (names.length > 0) {
                    BoolQueryBuilder builder = QueryBuilders.boolQuery();
                    for (int j = 0; j < names.length; j++) {
                        builder.should(QueryBuilders.matchQuery(names[j], slike).fuzziness(FUZZINESS));
                    }
                    return builder;
                } else {
                    return QueryBuilders.matchQuery(names[0], slike).fuzziness(FUZZINESS);
                }
            case MLT:
            default:
                return QueryBuilders.moreLikeThisQuery(names).addLikeText(like.toString());
        }
    }

    /**
     * $search : { name : searchParameter }
     *
     * @param req QUERY
     * @param content JsonNode
     * @return the search Command
     * @throws InvalidParseOperationException if check unicity is in error
     */
    private static QueryBuilder searchCommand(final QUERY query, final JsonNode content)
        throws InvalidParseOperationException {
        final Entry<String, JsonNode> element = JsonHandler.checkUnicity(query.exactToken(), content);
        return QueryBuilders.simpleQueryStringQuery(element.getValue().toString()).field(element.getKey());
    }

    /**
     * $match : { name : words, $max_expansions : n }
     *
     * @param req QUERY
     * @param content JsonNode
     * @return the match Command
     * @throws InvalidParseOperationException if check unicity is in error
     */
    private static QueryBuilder matchCommand(final QUERY query, final JsonNode content)
        throws InvalidParseOperationException {
        final JsonNode max = ((ObjectNode) content).remove(QUERYARGS.MAX_EXPANSIONS.exactToken());
        final Entry<String, JsonNode> element = JsonHandler.checkUnicity(query.exactToken(), content);
        final String attribute = element.getKey();
        if ((query == QUERY.MATCH_PHRASE_PREFIX || query == QUERY.MATCH_PHRASE) && isAttributeNotAnalyzed(attribute)) {
            return QueryBuilders.prefixQuery(element.getKey(), element.getValue().toString());
        } else {
            QUERY query2 = query;
            if (query == QUERY.PREFIX) {
                query2 = QUERY.MATCH_PHRASE_PREFIX;
            }
            if (max != null && !max.isMissingNode()) {
                switch (query2) {
                    case MATCH:
                        return QueryBuilders.matchQuery(element.getKey(), element.getValue().toString())
                            .maxExpansions(max.asInt());
                    case MATCH_PHRASE:
                        return QueryBuilders.matchPhraseQuery(element.getKey(), element.getValue().toString())
                            .maxExpansions(max.asInt());
                    case MATCH_PHRASE_PREFIX:
                        return QueryBuilders.matchPhrasePrefixQuery(element.getKey(), element.getValue().toString())
                            .maxExpansions(max.asInt());
                    default:
                        throw new InvalidParseOperationException("Not correctly parsed: " + query);
                }
            } else {
                switch (query) {
                    case MATCH:
                        return QueryBuilders.matchQuery(element.getKey(), element.getValue().toString());
                    case MATCH_PHRASE:
                        return QueryBuilders.matchPhraseQuery(element.getKey(), element.getValue().toString());
                    case MATCH_PHRASE_PREFIX:
                        return QueryBuilders.matchPhrasePrefixQuery(element.getKey(), element.getValue().toString());
                    default:
                        throw new InvalidParseOperationException("Not correctly parsed: " + query);
                }
            }
        }
    }

    /**
     * $in : { name : [ value1, value2, ... ] }
     *
     * @param req QUERY
     * @param content JsonNode
     * @return the in Command
     * @throws InvalidParseOperationException if check unicity is in error
     */
    private static QueryBuilder inCommand(final QUERY query, final JsonNode content)
        throws InvalidParseOperationException {
        final Entry<String, JsonNode> element = JsonHandler.checkUnicity(query.exactToken(), content);
        String key = element.getKey();
        List<JsonNode> nodes = element.getValue().findValues(ParserTokens.QUERYARGS.DATE.exactToken());

        if (nodes != null && !nodes.isEmpty()) {
            key += "." + ParserTokens.QUERYARGS.DATE.exactToken();
        }
        Set<Object> set = new HashSet<Object>();
        for (final JsonNode value : nodes) {
            set.add(getAsObject(value));
        }
        QueryBuilder query2 = QueryBuilders.termsQuery(key, set);
        if (query == QUERY.NIN) {
            QueryBuilder query3 = QueryBuilders.boolQuery().mustNot(query2);
            return query3;
        }
        return query2;
    }


    /**
     * $range : { name : { $gte : value, $lte : value } }
     *
     * @param req QUERY
     * @param content JsonNode
     * @return the range Command
     * @throws InvalidParseOperationException if check unicity is in error
     */
    private static QueryBuilder rangeCommand(final QUERY query, final JsonNode content)
        throws InvalidParseOperationException {
        final Entry<String, JsonNode> element = JsonHandler.checkUnicity(query.exactToken(), content);

        String key = element.getKey();
        JsonNode node = element.getValue().findValue(ParserTokens.QUERYARGS.DATE.exactToken());
        if (node != null) {
            key += "." + ParserTokens.QUERYARGS.DATE.exactToken();
        }
        RangeQueryBuilder range = QueryBuilders.rangeQuery(key);
        for (final Iterator<Entry<String, JsonNode>> iterator = element.getValue().fields(); iterator.hasNext();) {
            final Entry<String, JsonNode> requestItem = iterator.next();
            RANGEARGS arg = null;
            try {
                final String skey = requestItem.getKey();
                if (skey.startsWith("$")) {
                    arg = RANGEARGS.valueOf(skey.substring(1).toUpperCase());
                } else {
                    throw new InvalidParseOperationException("Invalid Range query command: " + requestItem);
                }
            } catch (final IllegalArgumentException e) {
                throw new InvalidParseOperationException("Invalid Range query command: " + requestItem, e);
            }
            node = requestItem.getValue().findValue(ParserTokens.QUERYARGS.DATE.exactToken());
            if (node == null) {
                node = requestItem.getValue();
            }
            switch (arg) {
                case GT:
                    range.gt(getAsObject(node));
                    break;
                case GTE:
                    range.gte(getAsObject(node));
                    break;
                case LT:
                    range.lt(getAsObject(node));
                    break;
                case LTE:
                default:
                    range.lte(getAsObject(node));
            }
        }
        return range;
    }


    /**
     * $regex : { name : regex }
     *
     * @param req QUERY
     * @param content JsonNode
     * @return the regex Command
     * @throws InvalidParseOperationException if check unicity is in error
     */
    private static QueryBuilder regexCommand(final QUERY query, final JsonNode content)
        throws InvalidParseOperationException {
        final Entry<String, JsonNode> entry = JsonHandler.checkUnicity(query.exactToken(), content);
        return QueryBuilders.regexpQuery(entry.getKey(), "/" + entry.getValue().asText() + "/");
    }

    /**
     * $term : { name : term, name : term }
     *
     * @param req QUERY
     * @param content JsonNode
     * @return the term Command
     * @throws InvalidParseOperationException if check unicity is in error
     */
    private static QueryBuilder termCommand(final QUERY query, final JsonNode content)
        throws InvalidParseOperationException {

        boolean multiple = false;
        QueryBuilder query2 = null;
        if (content.size() > 1) {
            multiple = true;
            query2 = QueryBuilders.boolQuery();
        }
        for (final Iterator<Entry<String, JsonNode>> iterator = content.fields(); iterator.hasNext();) {
            final Entry<String, JsonNode> requestItem = iterator.next();
            String key = requestItem.getKey();
            JsonNode node = requestItem.getValue().findValue(ParserTokens.QUERYARGS.DATE.exactToken());
            boolean isDate = false;
            if (node == null) {
                node = requestItem.getValue();
            } else {
                isDate = true;
                key += "." + ParserTokens.QUERYARGS.DATE.exactToken();
            }
            if (node.isNumber()) {
                if (!multiple) {
                    return QueryBuilders.termQuery(key, getAsObject(node));
                }
                return ((BoolQueryBuilder) query2).must(QueryBuilders.termQuery(key, getAsObject(node)));
            } else {
                final String val = node.asText();
                QueryBuilder query3 = null;
                if (isAttributeNotAnalyzed(key) || isDate) {
                    query3 = QueryBuilders.termQuery(key, val);
                } else {
                    query3 = QueryBuilders.simpleQueryStringQuery("\"" + val + "\"").field(key)
                        .flags(SimpleQueryStringFlag.PHRASE);
                }
                if (!multiple) {
                    return query3;
                }
                return ((BoolQueryBuilder) query2).must(query3);
            }
        }
        return query2;
    }


    /**
     * $wildcard : { name : term }
     *
     * @param refCommand
     * @param command
     */
    private static QueryBuilder wildcardCommand(final QUERY query, final JsonNode content)
        throws InvalidParseOperationException {
        final Entry<String, JsonNode> entry = JsonHandler.checkUnicity(query.exactToken(), content);
        final String key = entry.getKey();
        final JsonNode node = entry.getValue();
        final String val = node.asText();
        return QueryBuilders.wildcardQuery(key, val);
    }

    /**
     * $eq : { name : value }
     *
     * @param req QUERY
     * @param content JsonNode
     * @return the eq Command
     * @throws InvalidParseOperationException if check unicity is in error
     */
    private static QueryBuilder eqCommand(final QUERY query, final JsonNode content)
        throws InvalidParseOperationException {
        final Entry<String, JsonNode> entry = JsonHandler.checkUnicity(query.exactToken(), content);

        String key = entry.getKey();
        JsonNode node = entry.getValue().findValue(ParserTokens.QUERYARGS.DATE.exactToken());
        boolean isDate = false;
        if (node == null) {
            node = entry.getValue();
        } else {
            isDate = true;
            key += "." + ParserTokens.QUERYARGS.DATE.exactToken();
        }
        if (isAttributeNotAnalyzed(key) || isDate) {
            QueryBuilder query2 = QueryBuilders.termQuery(key, getAsObject(node));
            if (query == QUERY.NE) {
                return QueryBuilders.boolQuery().mustNot(query2);
            }
            return query2;
        } else {
            QueryBuilder query2 = QueryBuilders.simpleQueryStringQuery("\"" + getAsObject(node) + "\"").field(key)
                .flags(SimpleQueryStringFlag.PHRASE);
            if (query == QUERY.NE) {
                return QueryBuilders.boolQuery().mustNot(query2);
            }
            return query2;
        }
    }

    /**
     * $exists : name
     *
     * @param req QUERY
     * @param content JsonNode
     * @return the exist Command
     * @throws InvalidParseOperationException if check unicity is in error
     */
    private static QueryBuilder existsMissingCommand(final QUERY query, final JsonNode content)
        throws InvalidParseOperationException {

        final String fieldname = content.asText();
        QueryBuilder queryBuilder = QueryBuilders.existsQuery(fieldname);
        switch (query) {
            case MISSING:
                return QueryBuilders.boolQuery().mustNot(queryBuilder);
            case EXISTS:
            default:
                return queryBuilder;
        }
    }

    /**
     * $isNull : name
     *
     * @param req QUERY
     * @param content JsonNode
     * @return the isNull Command
     * @throws InvalidParseOperationException if check unicity is in error
     */
    private static QueryBuilder isNullCommand(final QUERY query, final JsonNode content)
        throws InvalidParseOperationException {
        final String fieldname = content.asText();
        return QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery(fieldname));
    }


    /**
     * $and : [ expression1, expression2, ... ] $or : [ expression1, expression2, ... ] $not : [ expression1,
     * expression2, ... ]
     *
     * @param req QUERY
     * @param content JsonNode
     * @return the and Or Not Command
     * @throws InvalidParseOperationException if check unicity is in error
     */
    private static QueryBuilder andOrNotCommand(final QUERY query, final Query req)
        throws InvalidParseOperationException {

        final BooleanQuery nthrequest = (BooleanQuery) req;
        final List<Query> sub = nthrequest.getQueries();

        final BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        for (int i = 0; i < sub.size(); i++) {
            switch (query) {
                case AND:
                    boolQueryBuilder.must(getCommand(sub.get(i)));
                    break;
                case NOT:
                    boolQueryBuilder.mustNot(getCommand(sub.get(i)));
                    break;
                case OR:
                default:
                    boolQueryBuilder.should(getCommand(sub.get(i)));
            }
        }
        return boolQueryBuilder;
    }


    /**
     * @param JsonNode
     * @return JsonNode as Object
     */

    private static final Object getAsObject(JsonNode value) {
        if (value.isBoolean()) {
            return value.asBoolean();
        } else if (value.canConvertToLong()) {
            return value.asLong();
        } else if (value.isDouble()) {
            return value.asDouble();
        } else {
            return value.asText();
        }
    }

    /*
     * Returns True if this attribute is not analyzed by ElasticSearch, else False
     */
    private static boolean isAttributeNotAnalyzed(String attributeName) {

        // TODO returns True if this attribute is not analyzed by ElasticSearch, else False
        return true;
    }

}
