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

package fr.gouv.vitam.common.database.translators.elasticsearch;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.Script;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.AggregationBuilders;
import co.elastic.clients.elasticsearch._types.aggregations.CardinalityAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.DateRangeAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.SumAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.TermsAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.ValueCountAggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.json.JsonData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.database.builder.facet.Facet;
import fr.gouv.vitam.common.database.builder.query.BooleanQuery;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.FACETARGS;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.QUERY;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.QUERYARGS;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.RANGEARGS;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.SELECTFILTER;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.collections.DynamicParserTokens;
import fr.gouv.vitam.common.database.parser.query.QueryParserHelper;
import fr.gouv.vitam.common.database.parser.request.AbstractParser;
import fr.gouv.vitam.common.database.parser.request.GlobalDatasParser;
import fr.gouv.vitam.common.database.parser.request.adapter.VarNameAdapter;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchUtil.boolMust;
import static fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchUtil.boolShould;
import static fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchUtil.exists;
import static fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchUtil.getFieldSorts;
import static fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchUtil.getScoreSort;
import static fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchUtil.gtQuery;
import static fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchUtil.gteQuery;
import static fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchUtil.ltQuery;
import static fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchUtil.lteQuery;
import static fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchUtil.matchAll;
import static fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchUtil.matchAllQuery;
import static fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchUtil.matchPhrasePrefixQuery;
import static fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchUtil.matchPhraseQuery;
import static fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchUtil.matchQuery;
import static fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchUtil.mustNot;
import static fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchUtil.nestedQuery;
import static fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchUtil.regex;
import static fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchUtil.simpleQueryString;
import static fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchUtil.termQuery;
import static fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchUtil.termsQuery;
import static fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchUtil.wildcard;
import static java.lang.Math.min;

/**
 * Elasticsearch Translator
 */
public class QueryToElasticsearch {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(QueryToElasticsearch.class);

    private QueryToElasticsearch() {
        // Empty constructor
    }

    /**
     * @param field String
     * @param roots Set of String
     * @return the filter associated with the roots
     */
    public static co.elastic.clients.elasticsearch._types.query_dsl.Query getRoots(
        final String field,
        final Collection<String> roots
    ) {
        // NB: terms and not term since multiple values
        return termsQuery(field, roots);
    }

    /**
     * @param query
     * @param field String
     * @param roots Set of String
     */
    public static void addRoots(
        BoolQuery.Builder query,
        final String field,
        final Collection<String> roots,
        int depth
    ) {
        query.filter(
            boolShould(
                IntStream.rangeClosed(1, depth)
                    .mapToObj(i -> termsQuery(field + "." + i, roots))
                    .collect(Collectors.toList())
            )
        );
    }

    /**
     * Merge a request and a root filter
     *
     * @param command QueryBuilder
     * @param roots QueryBuilder
     * @return the complete request
     */
    public static co.elastic.clients.elasticsearch._types.query_dsl.Query getFullCommand(
        final co.elastic.clients.elasticsearch._types.query_dsl.Query command,
        final co.elastic.clients.elasticsearch._types.query_dsl.Query roots
    ) {
        return boolMust(command, roots);
    }

    /**
     * Generate sort list from order by ES query orders : {field1 : -1, field2 : 1} or [{field1 : -1, field2 :
     * 1},{field3 : -1}]<br>
     * <br>
     * <b>Note</b> : if the query contains a match and the collection allows to use score, the socre is added to the
     * sort<br>
     * <br>
     *
     * @param requestParser the original parser
     * @param score True will add score first
     * @param parserTokens
     * @return list of order by as sort objects
     */
    public static List<SortOptions> getSorts(
        final AbstractParser<?> requestParser,
        boolean score,
        DynamicParserTokens parserTokens
    ) {
        final JsonNode orderby = requestParser.getRequest().getFilter().get(SELECTFILTER.ORDERBY.exactToken());
        int size = score && requestParser.hasFullTextQuery() ? 1 : 0;
        if (orderby != null && !orderby.isEmpty()) {
            size += orderby.size();
        }
        final List<SortOptions> sorts = new ArrayList<>(size);
        if (orderby == null || orderby.isEmpty()) {
            if (score && requestParser.hasFullTextQuery()) {
                sorts.add(getScoreSort(SortOrder.Desc));
                return sorts;
            }
            return null;
        }
        final Iterator<Entry<String, JsonNode>> iterator = orderby.fields();
        if (!iterator.hasNext()) {
            if (score && requestParser.hasFullTextQuery()) {
                sorts.add(getScoreSort(SortOrder.Desc));
                return sorts;
            }
            return null;
        }
        boolean scoreNotAdded = true;
        while (iterator.hasNext()) {
            final Entry<String, JsonNode> entry = iterator.next();
            String key = entry.getKey();
            if (
                scoreNotAdded &&
                score &&
                requestParser.hasFullTextQuery() &&
                !parserTokens.isNotAnalyzed(entry.getKey())
            ) {
                // First time we get an analyzed sort by
                scoreNotAdded = false;
                if ("_score".equals(entry.getKey()) || "#score".equals(entry.getKey())) {
                    if (entry.getValue().asInt() < 0) {
                        sorts.add(getScoreSort(SortOrder.Desc));
                    } else {
                        sorts.add(getScoreSort(SortOrder.Asc));
                    }
                    continue;
                } else {
                    sorts.add(getScoreSort(SortOrder.Desc));
                }
            }

            if (entry.getValue().asInt() < 0) {
                sorts.add(getFieldSorts(key, SortOrder.Desc));
            } else {
                sorts.add(getFieldSorts(key, SortOrder.Asc));
            }
        }
        if (scoreNotAdded && score && requestParser.hasFullTextQuery()) {
            // Last filter if not yet added
            sorts.add(getScoreSort(SortOrder.Desc));
        }
        return sorts;
    }

    /**
     * @param query Query
     * @param parserTokens
     * @return the associated QueryBuilder
     * @throws InvalidParseOperationException if query could not parse to command
     */
    public static co.elastic.clients.elasticsearch._types.query_dsl.Query getCommand(
        final Query query,
        VarNameAdapter adapter,
        DynamicParserTokens parserTokens
    ) throws InvalidParseOperationException {
        final QUERY req = query.getQUERY();
        final JsonNode content = query.getNode(req.exactToken());
        switch (req) {
            case AND:
            case NOT:
            case OR:
                return andOrNotCommand(req, query, adapter, parserTokens);
            case EXISTS:
            case MISSING:
                return existsMissingCommand(req, content);
            case MATCH:
            case MATCH_ALL:
            case MATCH_PHRASE:
            case MATCH_PHRASE_PREFIX:
                return matchCommand(req, content, parserTokens);
            case SEARCH:
                return searchCommand(req, content, parserTokens);
            case SUBOBJECT:
                return nestedSearchCommand(req, content, adapter, parserTokens);
            case NIN:
            case IN:
                return inCommand(req, content, parserTokens);
            case RANGE:
                return rangeCommand(req, content, parserTokens);
            case REGEX:
                return regexCommand(req, content, parserTokens);
            case TERM:
                return termCommand(req, content, parserTokens);
            case WILDCARD:
                return wildcardCommand(req, content, parserTokens);
            case EQ:
            case NE:
                return eqCommand(req, content, parserTokens);
            case GT:
            case GTE:
            case LT:
            case LTE:
                return compareCommand(req, content, parserTokens);
            case ISNULL:
                return isNullCommand(req, content);
            case SIZE:
                return sizeCommand(req, content);
            case NOP:
                return matchAll();
            case GEOMETRY:
            case BOX:
            case POLYGON:
            case CENTER:
            case GEOINTERSECTS:
            case GEOWITHIN:
            default:
                throw new InvalidParseOperationException("Invalid command: " + req.exactToken());
        }
    }

    /**
     * $size : { name : length }
     *
     * @param query QUERY
     * @param content JsonNode
     * @return the size Command
     * @throws InvalidParseOperationException if check unicity is in error
     * @deprecated
     */
    private static co.elastic.clients.elasticsearch._types.query_dsl.Query sizeCommand(
        final QUERY query,
        final JsonNode content
    ) throws InvalidParseOperationException {
        // Unsupported command. May be deleted without prior notice.
        logUnsupportedCommand(query, content, "Deprecated. Should not be invoked anymore.");

        final Entry<String, JsonNode> element = JsonHandler.checkUnicity(query.exactToken(), content);
        final Script script = Script.of(
            s ->
                s.source(
                    source ->
                        source.scriptString("doc['" + element.getKey() + "'].values.length == " + element.getValue())
                )
        );

        if (element.getKey().equals(VitamDocument.ID)) {
            logWarnUnsupportedIdForCommand(query, content);
        }
        return QueryBuilders.script().script(script).build()._toQuery();
    }

    /**
     * $gt : { name : value } $gte : { name : value } $lt : { name : value } $lte : { name : value }
     *
     * @param query QUERY
     * @param content JsonNode
     * @return the compare Command
     * @throws InvalidParseOperationException if check unicity is in error
     */
    private static co.elastic.clients.elasticsearch._types.query_dsl.Query compareCommand(
        final QUERY query,
        final JsonNode content,
        DynamicParserTokens parserTokens
    ) throws InvalidParseOperationException {
        final Entry<String, JsonNode> element = JsonHandler.checkUnicity(query.exactToken(), content);

        String key = element.getKey();

        if (!parserTokens.isNotAnalyzed(key)) {
            // Unsupported mode. May be updated without prior notice.
            logUnsupportedCommand(query, content, "Analyzed field: '" + key + "'");
        } else {
            logCommand(query, content);
        }

        JsonNode node = element.getValue();
        Object value = GlobalDatasParser.getValue(node);

        switch (query) {
            case GT:
                return gtQuery(key, value);
            case GTE:
                return gteQuery(key, value);
            case LT:
                return ltQuery(key, value);
            case LTE:
            default:
                return lteQuery(key, value);
        }
    }

    /**
     * $search : { name : searchParameter }
     *
     * @param query QUERY
     * @param content JsonNode
     * @return the search Command
     * @throws InvalidParseOperationException if check unicity is in error
     */
    private static co.elastic.clients.elasticsearch._types.query_dsl.Query searchCommand(
        final QUERY query,
        final JsonNode content,
        DynamicParserTokens parserTokens
    ) throws InvalidParseOperationException {
        final Entry<String, JsonNode> element = JsonHandler.checkUnicity(query.exactToken(), content);
        final String attribute = element.getKey();

        if (parserTokens.isNotAnalyzed(attribute)) {
            // Unsupported mode. May be updated without prior notice.
            logUnsupportedCommand(query, content, "Not_analyzed field: '" + attribute + "'");
        } else {
            logCommand(query, content);
        }

        return simpleQueryString(attribute, element.getValue().asText());
    }

    /**
     * $subobject : { name : searchParameter }
     *
     * @param query QUERY
     * @param content JsonNode
     * @return the search Command
     * @throws InvalidParseOperationException if check unicity is in error
     */
    private static co.elastic.clients.elasticsearch._types.query_dsl.Query nestedSearchCommand(
        final QUERY query,
        final JsonNode content,
        VarNameAdapter adapter,
        DynamicParserTokens parserTokens
    ) throws InvalidParseOperationException {
        final Entry<String, JsonNode> element = JsonHandler.checkUnicity(query.exactToken(), content);
        final String attribute = element.getKey();

        if (parserTokens.isNotAnalyzed(attribute)) {
            // Unsupported mode. May be updated without prior notice.
            logUnsupportedCommand(query, content, "Not_analyzed field: '" + attribute + "'");
        } else {
            logCommand(query, content);
        }

        if (content == null || !content.fields().hasNext() || !content.fields().next().getValue().fields().hasNext()) {
            throw new InvalidParseOperationException("$subobject query is not valid");
        }

        String path = content.fields().next().getKey();
        JsonNode subQueryJson = content.fields().next().getValue();
        Query subQuery;
        try {
            subQuery = QueryParserHelper.query(
                subQueryJson.fields().next().getKey(),
                subQueryJson.fields().next().getValue(),
                adapter
            );
        } catch (InvalidCreateOperationException e) {
            throw new InvalidParseOperationException("$subobject query is not valid");
        }
        return nestedQuery(path, getCommand(subQuery, adapter, parserTokens));
    }

    /**
     * $match : { name : words }
     *
     * @param query QUERY
     * @param content JsonNode
     * @return the match Command
     * @throws InvalidParseOperationException if check unicity is in error
     */
    private static co.elastic.clients.elasticsearch._types.query_dsl.Query matchCommand(
        final QUERY query,
        final JsonNode content,
        DynamicParserTokens parserTokens
    ) throws InvalidParseOperationException {
        final JsonNode max = ((ObjectNode) content).remove(QUERYARGS.MAX_EXPANSIONS.exactToken());
        final Entry<String, JsonNode> element = JsonHandler.checkUnicity(query.exactToken(), content);
        final String attribute = element.getKey();

        // Unsupported match over analyzed field
        if (parserTokens.isNotAnalyzed(attribute)) {
            return matchCommandOverNonAnalyzedField(query, content, element, attribute);
        }

        // Unsupported max_expansions
        if (max != null && !max.isMissingNode()) {
            return matchCommandWithMaxExpansions(query, content, max, element);
        }

        logCommand(query, content);

        switch (query) {
            case MATCH:
                return matchQuery(element.getKey(), element.getValue().asText());
            case MATCH_ALL:
                return matchAllQuery(element.getKey(), element.getValue().asText());
            case MATCH_PHRASE:
                return matchPhraseQuery(element.getKey(), element.getValue().asText());
            case MATCH_PHRASE_PREFIX:
                return matchPhrasePrefixQuery(element.getKey(), element.getValue().asText());
            default:
                throw new InvalidParseOperationException("Not correctly parsed: " + query);
        }
    }

    /**
     * $match : { name : words, $max_expansions : n }. Unsupported $max_expansions mode to be removed later.
     *
     * @param query QUERY
     * @param content JsonNode
     * @return the match Command
     * @throws InvalidParseOperationException if check unicity is in error
     * @deprecated Unsupported case. Should/will be removed without prior notice.
     */
    private static co.elastic.clients.elasticsearch._types.query_dsl.Query matchCommandWithMaxExpansions(
        QUERY query,
        JsonNode content,
        JsonNode max,
        Entry<String, JsonNode> element
    ) throws InvalidParseOperationException {
        logUnsupportedCommand(query, content, "Unsupported max_expansions operator");

        switch (query) {
            case MATCH:
                return matchQuery(element.getKey(), element.getValue().asText(), max.asInt());
            case MATCH_ALL:
                return matchAllQuery(element.getKey(), element.getValue().asText(), max.asInt());
            case MATCH_PHRASE:
                // Note: the method maxExpansions(max.asInt()) is removed in ES5, with no documented replacement.
                return matchPhraseQuery(element.getKey(), element.getValue().asText());
            case MATCH_PHRASE_PREFIX:
                return matchPhrasePrefixQuery(element.getKey(), element.getValue().asText(), max.asInt());
            default:
                throw new InvalidParseOperationException("Not correctly parsed: " + query);
        }
    }

    /**
     * $match : { name : words } for non analyzed fields. Unsupported use case to be removed later.
     *
     * @param query QUERY
     * @param content JsonNode
     * @return the match Command
     * @throws InvalidParseOperationException if check unicity is in error
     * @deprecated Unsupported cases. Should/will be removed without prior notice.
     */
    private static co.elastic.clients.elasticsearch._types.query_dsl.Query matchCommandOverNonAnalyzedField(
        QUERY query,
        JsonNode content,
        Entry<String, JsonNode> element,
        String attribute
    ) {
        logUnsupportedCommand(query, content, "Not_analyzed field: '" + attribute + "'");

        switch (query) {
            case MATCH:
                return termsQuery(element.getKey(), element.getValue().toString().split(" "));
            case MATCH_ALL:
            case MATCH_PHRASE:
            case MATCH_PHRASE_PREFIX:
            default:
                return termQuery(element.getKey(), element.getValue().toString());
        }
    }

    /**
     * $in : { name : [ value1, value2, ... ] }
     *
     * @param query QUERY
     * @param content JsonNode
     * @return the in Command
     * @throws InvalidParseOperationException if check unicity is in error
     */
    private static co.elastic.clients.elasticsearch._types.query_dsl.Query inCommand(
        final QUERY query,
        final JsonNode content,
        DynamicParserTokens parserTokens
    ) throws InvalidParseOperationException {
        final Entry<String, JsonNode> element = JsonHandler.checkUnicity(query.exactToken(), content);
        String key = element.getKey();

        // Unsupported command for analyzed field
        if (!parserTokens.isNotAnalyzed(key)) {
            return inCommandOverAnalyzedField(query, content);
        }

        List<JsonNode> nodes = new ArrayList<>();
        JsonNode node = element.getValue();
        if (node instanceof ArrayNode) {
            logCommand(query, content);

            for (JsonNode jsonNode : node) {
                nodes.add(jsonNode);
            }
        } else {
            // TODO : Check no usages from whitin Vitam & remove this unsupported usecase.
            logUnsupportedCommand(query, content, "Expecting value list");
            nodes.add(node);
        }

        final Set<Object> set = new HashSet<>();
        for (final JsonNode value : nodes) {
            set.add(getAsObject(value));
        }
        final co.elastic.clients.elasticsearch._types.query_dsl.Query query2 = QueryBuilders.terms()
            .field(key)
            .terms(
                tt ->
                    tt.value(set.stream().map(value -> FieldValue.of(JsonData.of(value))).collect(Collectors.toList()))
            )
            .build()
            ._toQuery();
        if (query == QUERY.NIN) {
            return mustNot(query2);
        }
        return query2;
    }

    /**
     * $in : { name : [ value1, value2, ... ] } for analyzed fields. Unsupported use case to be removed later.
     *
     * @param query QUERY
     * @param content JsonNode
     * @return the in Command
     * @throws InvalidParseOperationException if check unicity is in error
     * @deprecated Unsupported cases should/will be removed without prior notice.
     */
    private static co.elastic.clients.elasticsearch._types.query_dsl.Query inCommandOverAnalyzedField(
        final QUERY query,
        final JsonNode content
    ) throws InvalidParseOperationException {
        final Entry<String, JsonNode> element = JsonHandler.checkUnicity(query.exactToken(), content);
        String key = element.getKey();

        logUnsupportedCommand(query, content, "Analyzed field: '" + key + "'");

        List<JsonNode> nodes = new ArrayList<>();
        JsonNode node = element.getValue();
        if (node instanceof ArrayNode) {
            for (JsonNode jsonNode : node) {
                nodes.add(jsonNode);
            }
        } else {
            nodes.add(node);
        }

        final Set<Object> set = new HashSet<>();
        for (final JsonNode value : nodes) {
            set.add(getAsObject(value));
        }
        final co.elastic.clients.elasticsearch._types.query_dsl.Query query2;

        final BoolQuery.Builder builder = new BoolQuery.Builder().minimumShouldMatch("1");
        for (final Object object : set) {
            builder.should(
                QueryBuilders.match(m -> m.field(key).query(FieldValue.of(JsonData.of(object))).operator(Operator.Or))
            );
        }
        query2 = builder.build()._toQuery();

        if (query == QUERY.NIN) {
            return mustNot(query2);
        }
        return query2;
    }

    /**
     * $range : { name : { $gte : value, $lte : value } }
     *
     * @param query QUERY
     * @param content JsonNode
     * @return the range Command
     * @throws InvalidParseOperationException if check unicity is in error
     */
    private static co.elastic.clients.elasticsearch._types.query_dsl.Query rangeCommand(
        final QUERY query,
        final JsonNode content,
        DynamicParserTokens parserTokens
    ) throws InvalidParseOperationException {
        final Entry<String, JsonNode> element = JsonHandler.checkUnicity(query.exactToken(), content);

        String key = element.getKey();

        if (!parserTokens.isNotAnalyzed(key)) {
            // Unsupported mode. May be updated without prior notice.
            logUnsupportedCommand(query, content, "Analyzed field: '" + key + "'");
        } else {
            logCommand(query, content);
        }

        if (VitamDocument.ID.equals(key)) {
            logWarnUnsupportedIdForCommand(query, content);
        }

        final Map<RANGEARGS, JsonNode> rangeParams = new HashMap<>();

        for (final Iterator<Entry<String, JsonNode>> iterator = element.getValue().fields(); iterator.hasNext();) {
            final Entry<String, JsonNode> requestItem = iterator.next();
            RANGEARGS arg;
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

            rangeParams.put(arg, requestItem.getValue());
        }
        RangeQuery.Builder range = QueryBuilders.range();
        range.untyped(u -> {
            u.field(key);

            if (rangeParams.containsKey(RANGEARGS.GT)) {
                u.gt(JsonData.of(getAsObject(rangeParams.get(RANGEARGS.GT))));
            }
            if (rangeParams.containsKey(RANGEARGS.GTE)) {
                u.gte(JsonData.of(getAsObject(rangeParams.get(RANGEARGS.GTE))));
            }
            if (rangeParams.containsKey(RANGEARGS.LT)) {
                u.lt(JsonData.of(getAsObject(rangeParams.get(RANGEARGS.LT))));
            }
            if (rangeParams.containsKey(RANGEARGS.LTE)) {
                u.lte(JsonData.of(getAsObject(rangeParams.get(RANGEARGS.LTE))));
            }

            return u;
        });

        return range.build()._toQuery();
    }

    /**
     * $regex : { name : regex }
     *
     * @param query QUERY
     * @param content JsonNode
     * @return the regex Command
     * @throws InvalidParseOperationException if check unicity is in error
     */
    private static co.elastic.clients.elasticsearch._types.query_dsl.Query regexCommand(
        final QUERY query,
        final JsonNode content,
        DynamicParserTokens parserTokens
    ) throws InvalidParseOperationException {
        final Entry<String, JsonNode> entry = JsonHandler.checkUnicity(query.exactToken(), content);
        String key = entry.getKey();

        // Analyzed fields are not supported
        if (!parserTokens.isNotAnalyzed(key)) {
            return regexCommandOverAnalyzedField(query, content);
        }

        // special case of _id (cannot be queried)
        if (key.equals(VitamDocument.ID)) {
            return regexCommandOverIdField(query, content);
        }

        String value = entry.getValue().asText();
        logCommand(query, content);

        return regex(key, value);
    }

    /**
     * Handles $regex : { name : regex } for _id field. Unsupported use case to be removed later.
     *
     * @param query QUERY
     * @param content JsonNode
     * @return the regex Command
     * @throws InvalidParseOperationException if check unicity is in error
     * @deprecated Unsupported cases should/will be removed without prior notice.
     */
    private static co.elastic.clients.elasticsearch._types.query_dsl.Query regexCommandOverIdField(
        final QUERY query,
        final JsonNode content
    ) throws InvalidParseOperationException {
        final Entry<String, JsonNode> entry = JsonHandler.checkUnicity(query.exactToken(), content);

        String key = entry.getKey();

        logUnsupportedCommand(query, content, "Unsupported ID field");

        String value = entry
            .getValue()
            .asText()
            .replaceAll("[\\.\\?\\+\\*\\|\\{\\}\\[\\]\\(\\)\\\"\\\\\\#\\@\\&\\<\\>\\~]", " ");
        String[] values = removeAllDoubleSpace(value).split(" ");
        return QueryBuilders.terms(
            t ->
                t
                    .field(key)
                    .terms(tt -> tt.value(Arrays.stream(values).map(FieldValue::of).collect(Collectors.toList())))
        );
    }

    /**
     * Handles $regex : { name : regex } for analyzed fields. Unsupported use case to be removed later.
     *
     * @param query QUERY
     * @param content JsonNode
     * @return the regex Command
     * @throws InvalidParseOperationException if check unicity is in error
     * @deprecated Unsupported cases should/will be removed without prior notice.
     */
    private static co.elastic.clients.elasticsearch._types.query_dsl.Query regexCommandOverAnalyzedField(
        final QUERY query,
        final JsonNode content
    ) throws InvalidParseOperationException {
        final Entry<String, JsonNode> entry = JsonHandler.checkUnicity(query.exactToken(), content);

        // special case of _id
        String key = entry.getKey();
        String value = "/" + entry.getValue().asText() + "/";

        logUnsupportedCommand(query, content, "Analyzed field: '" + key + "'");

        return regex(key, value);
    }

    /**
     * $term : { name : term, name : term }
     *
     * @param query QUERY
     * @param content JsonNode
     * @return the term Command
     * @throws InvalidParseOperationException if check unicity is in error
     */
    private static co.elastic.clients.elasticsearch._types.query_dsl.Query termCommand(
        final QUERY query,
        final JsonNode content,
        DynamicParserTokens parserTokens
    ) throws InvalidParseOperationException {
        // Unsupported command. May be deleted without prior notice.
        logUnsupportedCommand(query, content, "Deprecated. Should not be invoked anymore.");

        boolean multiple = false;
        BoolQuery.Builder query2 = null;
        if (content.size() > 1) {
            multiple = true;
            query2 = QueryBuilders.bool();
        }
        for (final Iterator<Entry<String, JsonNode>> iterator = content.fields(); iterator.hasNext();) {
            final Entry<String, JsonNode> requestItem = iterator.next();
            String key = requestItem.getKey();
            if (VitamDocument.ID.equals(key)) {
                logWarnUnsupportedIdForCommand(query, content);
            }
            JsonNode node = requestItem.getValue();

            if (node.isNumber()) {
                if (!multiple) {
                    return QueryBuilders.term(t -> t.field(key).value(FieldValue.of(JsonData.of(getAsObject(node)))));
                }
                query2.must(QueryBuilders.term(t -> t.field(key).value(FieldValue.of(JsonData.of(getAsObject(node))))));
            } else {
                final String val = node.asText();
                co.elastic.clients.elasticsearch._types.query_dsl.Query query3;
                if (parserTokens.isNotAnalyzed(key)) {
                    query3 = termQuery(key, val);
                } else {
                    query3 = matchAllQuery(key, val);
                }
                if (!multiple) {
                    return query3;
                }
                query2.must(query3);
            }
        }
        return query2.build()._toQuery();
    }

    /**
     * $wildcard : { name : expression }
     *
     * @param query QUERY
     * @param content JsonNode
     * @return the wildcard Command
     */
    private static co.elastic.clients.elasticsearch._types.query_dsl.Query wildcardCommand(
        final QUERY query,
        final JsonNode content,
        DynamicParserTokens parserTokens
    ) throws InvalidParseOperationException {
        final Entry<String, JsonNode> entry = JsonHandler.checkUnicity(query.exactToken(), content);
        String key = entry.getKey();
        final JsonNode node = entry.getValue();
        String val = node.asText();

        if (!parserTokens.isNotAnalyzed(key)) {
            // Unsupported wildcard with analyzed field
            logUnsupportedCommand(query, content, "Analyzed field: '" + key + "'");
        } else {
            // Not analyzed mode...
            if (key.equals(VitamDocument.ID)) {
                // special case of _id (cannot be queried)
                logUnsupportedCommand(query, content, "Unsupported ID field");
            } else {
                // OK Mode
                logCommand(query, content);
            }
        }

        return wildcard(key, val);
    }

    @Deprecated
    private static String removeAllDoubleSpace(String value) {
        String oldValue = value;
        String newValue = oldValue.replace("  ", " ");
        while (newValue.length() != oldValue.length()) {
            oldValue = newValue;
            newValue = oldValue.replace("  ", " ");
        }
        return newValue;
    }

    /**
     * $eq : { name : value }
     *
     * @param query QUERY
     * @param content JsonNode
     * @return the eq Command
     * @throws InvalidParseOperationException if check unicity is in error
     */
    private static co.elastic.clients.elasticsearch._types.query_dsl.Query eqCommand(
        final QUERY query,
        final JsonNode content,
        DynamicParserTokens parserTokens
    ) throws InvalidParseOperationException {
        final Entry<String, JsonNode> entry = JsonHandler.checkUnicity(query.exactToken(), content);

        String key = entry.getKey();
        JsonNode node = entry.getValue();

        // Unsupported use case.
        if (!parserTokens.isNotAnalyzed(key)) {
            return eqCommandOverAnalyzedField(query, content);
        }

        logCommand(query, content);

        final co.elastic.clients.elasticsearch._types.query_dsl.Query query2 = QueryBuilders.term(
            t -> t.field(key).value(FieldValue.of(JsonData.of(getAsObject(node))))
        );
        if (query == QUERY.NE) {
            return mustNot(query2);
        }
        return query2;
    }

    /**
     * $eq : { name : value } for analyzed fields. Unsupported use case to be removed later.
     *
     * @param query QUERY
     * @param content JsonNode
     * @return the eq Command
     * @throws InvalidParseOperationException if check unicity is in error
     * @deprecated Unsupported cases should/will be removed without prior notice.
     */
    private static co.elastic.clients.elasticsearch._types.query_dsl.Query eqCommandOverAnalyzedField(
        QUERY query,
        JsonNode content
    ) throws InvalidParseOperationException {
        final Entry<String, JsonNode> entry = JsonHandler.checkUnicity(query.exactToken(), content);

        String key = entry.getKey();
        JsonNode node = entry.getValue();

        // Unsupported eq with analyzed field
        logUnsupportedCommand(query, content, "Analyzed field: '" + key + "'");

        final co.elastic.clients.elasticsearch._types.query_dsl.Query query2 = QueryBuilders.match(
            m -> m.field(key).query(FieldValue.of(JsonData.of(getAsObject(node)))).operator(Operator.And)
        );
        if (query == QUERY.NE) {
            return mustNot(query2);
        }
        return query2;
    }

    /**
     * $exists : name
     *
     * @param query QUERY
     * @param content JsonNode
     * @return the exist Command
     * @throws InvalidParseOperationException if check unicity is in error
     */
    private static co.elastic.clients.elasticsearch._types.query_dsl.Query existsMissingCommand(
        final QUERY query,
        final JsonNode content
    ) throws InvalidParseOperationException {
        String fieldname = content.asText();
        if (VitamDocument.ID.equals(fieldname)) {
            logWarnUnsupportedIdForCommand(query, content);
        }
        final co.elastic.clients.elasticsearch._types.query_dsl.Query existsQuery = exists(fieldname);
        switch (query) {
            case MISSING:
                // Unsupported command. May be deleted without prior notice.
                logUnsupportedCommand(query, content, "Deprecated. Should not be invoked anymore.");

                return mustNot(existsQuery);
            case EXISTS:
                logCommand(query, content);

                return existsQuery;
            default:
                throw new InvalidParseOperationException("Not correctly parsed: " + query);
        }
    }

    /**
     * $isNull : name
     *
     * @param query QUERY
     * @param content JsonNode
     * @return the isNull Command
     */
    private static co.elastic.clients.elasticsearch._types.query_dsl.Query isNullCommand(
        final QUERY query,
        final JsonNode content
    ) {
        // Unsupported command. May be deleted without prior notice.
        logUnsupportedCommand(query, content, "Deprecated. Should not be invoked anymore.");

        String fieldname = content.asText();
        if (VitamDocument.ID.equals(fieldname)) {
            logWarnUnsupportedIdForCommand(query, content);
        }
        return mustNot(exists(fieldname));
    }

    /**
     * $and : [ expression1, expression2, ... ] $or : [ expression1, expression2, ... ] $not : [ expression1,
     * expression2, ... ]
     *
     * @param req QUERY
     * @param query JsonNode
     * @return the and Or Not Command
     * @throws InvalidParseOperationException if check unicity is in error
     */
    private static co.elastic.clients.elasticsearch._types.query_dsl.Query andOrNotCommand(
        final QUERY query,
        final Query req,
        VarNameAdapter adapter,
        DynamicParserTokens parserTokens
    ) throws InvalidParseOperationException {
        logCommand(query, req.getCurrentObject());

        final BooleanQuery nthrequest = (BooleanQuery) req;
        final List<Query> sub = nthrequest.getQueries();
        final BoolQuery.Builder boolQueryBuilder = QueryBuilders.bool();
        for (Query value : sub) {
            switch (query) {
                case AND:
                    boolQueryBuilder.must(getCommand(value, adapter, parserTokens));
                    break;
                case NOT:
                    boolQueryBuilder.mustNot(getCommand(value, adapter, parserTokens));
                    break;
                case OR:
                default:
                    boolQueryBuilder.minimumShouldMatch("1").should(getCommand(value, adapter, parserTokens));
            }
        }
        return boolQueryBuilder.build()._toQuery();
    }

    /**
     * Create ES facets from request parser
     *
     * @param requestParser parser
     * @return list of facets
     * @throws InvalidParseOperationException if could not create ES facets
     */
    public static Map<String, Aggregation> getFacets(
        final AbstractParser<?> requestParser,
        DynamicParserTokens parserTokens
    ) throws InvalidParseOperationException {
        Map<String, Aggregation> aggregations = new HashMap<>();
        if (requestParser.getRequest() instanceof SelectMultiQuery || requestParser.getRequest() instanceof Select) {
            List<Facet> facets = null;
            if (requestParser.getRequest() instanceof SelectMultiQuery) {
                facets = ((SelectMultiQuery) requestParser.getRequest()).getFacets();
            } else if (requestParser.getRequest() instanceof Select) {
                facets = ((Select) requestParser.getRequest()).getFacets();
            }
            if (CollectionUtils.isNotEmpty(facets)) {
                for (Facet facet : facets) {
                    switch (facet.getCurrentTokenFACET()) {
                        case TERMS:
                            termsFacet(aggregations, facet);
                            break;
                        case DATE_RANGE:
                            dateRangeFacet(aggregations, facet);
                            break;
                        case FILTERS:
                            filtersFacet(aggregations, facet, requestParser.getAdapter(), parserTokens);
                            break;
                        case SUM:
                            sumFacet(aggregations, facet);
                            break;
                        case CARDINALITY:
                            cardinalityFacet(aggregations, facet);
                            break;
                        case COUNT:
                            countFacet(aggregations, facet);
                            break;
                        default:
                            throw new IllegalStateException("Unexpected value: " + facet.getCurrentTokenFACET());
                    }
                }
            }
        }
        return aggregations;
    }

    /**
     * Add date_range es facet from facet
     *
     * @param aggregations es facets
     * @param facet facet
     */
    private static void dateRangeFacet(Map<String, Aggregation> aggregations, Facet facet) {
        JsonNode dateRange = facet.getCurrentFacet().get(facet.getCurrentTokenFACET().exactToken());
        DateRangeAggregation.Builder dateRangeBuilder = AggregationBuilders.dateRange();
        dateRangeBuilder.field(dateRange.get(FACETARGS.FIELD.exactToken()).asText());
        dateRangeBuilder.format(dateRange.get(FACETARGS.FORMAT.exactToken()).asText());
        JsonNode ranges = dateRange.get(FACETARGS.RANGES.exactToken());
        ranges.forEach(item -> {
            JsonNode from = item.get(FACETARGS.FROM.exactToken());
            JsonNode to = item.get(FACETARGS.TO.exactToken());
            if (from != null && !(from instanceof NullNode) && to != null && !(to instanceof NullNode)) {
                dateRangeBuilder.ranges(r -> r.from(o -> o.expr(from.asText())).to(o -> o.expr(to.asText())));
            } else if (from != null && !(from instanceof NullNode)) {
                dateRangeBuilder.ranges(r -> r.from(o -> o.expr(from.asText())));
            } else if (to != null && !(to instanceof NullNode)) {
                dateRangeBuilder.ranges(r -> r.to(o -> o.expr(to.asText())));
            }
        });

        if (dateRange.get(FACETARGS.SUBOBJECT.exactToken()) != null) {
            aggregations.put(
                facet.getName(),
                new Aggregation.Builder()
                    .nested(n -> n.path(dateRange.get(FACETARGS.SUBOBJECT.exactToken()).asText()))
                    .aggregations(facet.getName(), dateRangeBuilder.build()._toAggregation())
                    .build()
            );
            return;
        }

        aggregations.put(facet.getName(), dateRangeBuilder.build()._toAggregation());
    }

    /**
     * Add terms es facet from facet
     *
     * @param aggregations es facets
     * @param facet facet
     */
    private static void termsFacet(Map<String, Aggregation> aggregations, Facet facet) {
        JsonNode terms = facet.getCurrentFacet().get(facet.getCurrentTokenFACET().exactToken());
        String fieldName = terms.get(FACETARGS.FIELD.exactToken()).asText();
        TermsAggregation.Builder termsBuilder = AggregationBuilders.terms();
        termsBuilder.field(fieldName);
        if (terms.has(FACETARGS.SIZE.exactToken())) {
            int size = terms.get(FACETARGS.SIZE.exactToken()).asInt();
            termsBuilder.size(size);
            termsBuilder.shardSize((int) min(Integer.MAX_VALUE, ((long) size * 3 + 10))); // This is used to get accurate results
        }

        if (terms.get(FACETARGS.SUBOBJECT.exactToken()) != null) {
            aggregations.put(
                facet.getName(),
                new Aggregation.Builder()
                    .nested(n -> n.path(terms.get(FACETARGS.SUBOBJECT.exactToken()).asText()))
                    .aggregations(facet.getName(), termsBuilder.build()._toAggregation())
                    .build()
            );
            return;
        }

        aggregations.put(facet.getName(), termsBuilder.build()._toAggregation());
    }

    /**
     * Add sum aggregation es facet from facet
     *
     * @param aggregations es facets
     * @param facet facet
     */
    private static void sumFacet(Map<String, Aggregation> aggregations, Facet facet) {
        JsonNode sumNode = facet.getCurrentFacet().get(facet.getCurrentTokenFACET().exactToken());
        String fieldName = sumNode.get(FACETARGS.FIELD.exactToken()).asText();
        SumAggregation.Builder sumBuilder = AggregationBuilders.sum();
        sumBuilder.field(fieldName);

        if (sumNode.get(FACETARGS.SUBOBJECT.exactToken()) != null) {
            aggregations.put(
                facet.getName(),
                new Aggregation.Builder()
                    .nested(n -> n.path(sumNode.get(FACETARGS.SUBOBJECT.exactToken()).asText()))
                    .aggregations(facet.getName(), sumBuilder.build()._toAggregation())
                    .build()
            );
            return;
        }

        aggregations.put(facet.getName(), sumBuilder.build()._toAggregation());
    }

    /**
     * Add value_count aggregation es facet from facet
     *
     * @param aggregations es facets
     * @param facet facet
     */
    private static void countFacet(Map<String, Aggregation> aggregations, Facet facet) {
        JsonNode valueCountNode = facet.getCurrentFacet().get(facet.getCurrentTokenFACET().exactToken());
        String fieldName = valueCountNode.get(FACETARGS.FIELD.exactToken()).asText();
        ValueCountAggregation.Builder valueCountBuilder = AggregationBuilders.valueCount();
        valueCountBuilder.field(fieldName);

        if (valueCountNode.get(FACETARGS.SUBOBJECT.exactToken()) != null) {
            aggregations.put(
                facet.getName(),
                new Aggregation.Builder()
                    .nested(n -> n.path(valueCountNode.get(FACETARGS.SUBOBJECT.exactToken()).asText()))
                    .aggregations(facet.getName(), valueCountBuilder.build()._toAggregation())
                    .build()
            );
            return;
        }

        aggregations.put(facet.getName(), valueCountBuilder.build()._toAggregation());
    }

    /**
     * Add cardinality aggregation es facet from facet
     *
     * @param aggregations es facets
     * @param facet facet
     */
    private static void cardinalityFacet(Map<String, Aggregation> aggregations, Facet facet) {
        JsonNode vardinalityNode = facet.getCurrentFacet().get(facet.getCurrentTokenFACET().exactToken());
        String fieldName = vardinalityNode.get(FACETARGS.FIELD.exactToken()).asText();
        CardinalityAggregation.Builder cardinalityBuilder = AggregationBuilders.cardinality();
        cardinalityBuilder.field(fieldName);

        if (vardinalityNode.get(FACETARGS.SUBOBJECT.exactToken()) != null) {
            aggregations.put(
                facet.getName(),
                new Aggregation.Builder()
                    .nested(n -> n.path(vardinalityNode.get(FACETARGS.SUBOBJECT.exactToken()).asText()))
                    .aggregations(facet.getName(), cardinalityBuilder.build()._toAggregation())
                    .build()
            );
            return;
        }

        aggregations.put(facet.getName(), cardinalityBuilder.build()._toAggregation());
    }

    /**
     * Add filters es facet from facet
     *
     * @param aggregations es facets
     * @param facet facet
     */
    private static void filtersFacet(
        Map<String, Aggregation> aggregations,
        Facet facet,
        VarNameAdapter adapter,
        DynamicParserTokens parserTokens
    ) throws InvalidParseOperationException {
        JsonNode filtersFacetNode = facet.getCurrentFacet().get(facet.getCurrentTokenFACET().exactToken());

        Map<String, Query> filtersMap = new HashMap<>();
        ArrayNode filtersNode = (ArrayNode) filtersFacetNode.get(FACETARGS.QUERY_FILTERS.exactToken());
        for (JsonNode node : filtersNode) {
            String key = node.get(FACETARGS.NAME.exactToken()).asText();
            JsonNode queryNode = node.get(FACETARGS.QUERY.exactToken());
            final Entry<String, JsonNode> queryItem = JsonHandler.checkUnicity("RootRequest", queryNode);
            try {
                Query query = QueryParserHelper.query(queryItem.getKey(), queryItem.getValue(), adapter);
                filtersMap.put(key, query);
            } catch (InvalidCreateOperationException e) {
                throw new InvalidParseOperationException(e);
            }
        }

        Map<String, co.elastic.clients.elasticsearch._types.query_dsl.Query> keyFilters = new HashMap<>();
        for (Entry<String, Query> entry : filtersMap.entrySet()) {
            keyFilters.put(entry.getKey(), getCommand(entry.getValue(), adapter, parserTokens));
        }
        Aggregation filtersBuilder = AggregationBuilders.filters(f -> f.filters(b -> b.keyed(keyFilters)));
        aggregations.put(facet.getName(), filtersBuilder);
    }

    private static Object getAsObject(JsonNode value) {
        if (value.isBoolean()) {
            return value.asBoolean();
        } else if (value.isInt() || value.isLong()) {
            return value.asLong();
        } else if (value.isFloat() || value.isDouble()) {
            return value.asDouble();
        } else {
            return value.asText();
        }
    }

    /**
     * Helper method for logging/dumping supported queries
     */
    private static void logCommand(QUERY query, JsonNode content) {
        LOGGER.debug(String.format("Command #QUERY: %s . Content: %s", query, content));
    }

    /**
     * Helper method for logging tricky queries dealing with "id" field Aim of this log is to check if search based on
     * id field is used with other operator than eq, ne, in, nin
     */
    private static void logWarnUnsupportedIdForCommand(QUERY query, JsonNode content) {
        LOGGER.warn(String.format("Command #QUERY: %s using id is not recommended. Content: %s", query, content));
    }

    /**
     * Logs a warning message for unsupported cases. Unsupported cases should/will be removed without prior notice.
     *
     * @param query the query
     * @param content the json content
     * @param message the error message
     * @deprecated Used to dump unsupported usages for queries.
     */
    private static void logUnsupportedCommand(QUERY query, JsonNode content, String message) {
        LOGGER.warn(String.format("UNSUPPORTED command #QUERY: %s. Message: %s. Content: %s", query, message, content));
    }
}
