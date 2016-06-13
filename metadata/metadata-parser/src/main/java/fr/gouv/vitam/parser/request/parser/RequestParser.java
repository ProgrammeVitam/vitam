/*******************************************************************************
 * This file is part of Vitam Project.
 *
 * Copyright Vitam (2012, 2015)
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
package fr.gouv.vitam.parser.request.parser;

import static fr.gouv.vitam.builder.request.construct.QueryHelper.and;
import static fr.gouv.vitam.builder.request.construct.QueryHelper.not;
import static fr.gouv.vitam.builder.request.construct.QueryHelper.or;
import static fr.gouv.vitam.parser.request.parser.query.QueryParserHelper.eq;
import static fr.gouv.vitam.parser.request.parser.query.QueryParserHelper.exists;
import static fr.gouv.vitam.parser.request.parser.query.QueryParserHelper.flt;
import static fr.gouv.vitam.parser.request.parser.query.QueryParserHelper.gt;
import static fr.gouv.vitam.parser.request.parser.query.QueryParserHelper.gte;
import static fr.gouv.vitam.parser.request.parser.query.QueryParserHelper.in;
import static fr.gouv.vitam.parser.request.parser.query.QueryParserHelper.isNull;
import static fr.gouv.vitam.parser.request.parser.query.QueryParserHelper.lt;
import static fr.gouv.vitam.parser.request.parser.query.QueryParserHelper.lte;
import static fr.gouv.vitam.parser.request.parser.query.QueryParserHelper.match;
import static fr.gouv.vitam.parser.request.parser.query.QueryParserHelper.matchPhrase;
import static fr.gouv.vitam.parser.request.parser.query.QueryParserHelper.matchPhrasePrefix;
import static fr.gouv.vitam.parser.request.parser.query.QueryParserHelper.missing;
import static fr.gouv.vitam.parser.request.parser.query.QueryParserHelper.mlt;
import static fr.gouv.vitam.parser.request.parser.query.QueryParserHelper.ne;
import static fr.gouv.vitam.parser.request.parser.query.QueryParserHelper.nin;
import static fr.gouv.vitam.parser.request.parser.query.QueryParserHelper.path;
import static fr.gouv.vitam.parser.request.parser.query.QueryParserHelper.prefix;
import static fr.gouv.vitam.parser.request.parser.query.QueryParserHelper.range;
import static fr.gouv.vitam.parser.request.parser.query.QueryParserHelper.regex;
import static fr.gouv.vitam.parser.request.parser.query.QueryParserHelper.search;
import static fr.gouv.vitam.parser.request.parser.query.QueryParserHelper.size;
import static fr.gouv.vitam.parser.request.parser.query.QueryParserHelper.term;
import static fr.gouv.vitam.parser.request.parser.query.QueryParserHelper.wildcard;

import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.builder.request.construct.Request;
import fr.gouv.vitam.builder.request.construct.configuration.GlobalDatas;
import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens;
import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.FILTERARGS;
import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.GLOBAL;
import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.QUERY;
import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.QUERYARGS;
import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.SELECTFILTER;
import fr.gouv.vitam.builder.request.construct.query.Query;
import fr.gouv.vitam.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.parser.request.construct.query.QueryDepthHelper;

/**
 * Partial Request Parser (common base): [ {root}, {query}, {filter} ] or { $roots: root, $query : query, $filter :
 * filter }
 *
 */
public abstract class RequestParser {
    private static final VitamLogger LOGGER =
        VitamLoggerFactory.getInstance(RequestParser.class);

    protected VarNameAdapter adapter;
    protected String sourceRequest;
    protected Request request;

    /**
     * Last computed Depth
     */
    protected int lastDepth = 0;
    /**
     * Contains queries to be computed by a full text index
     */
    protected boolean hasFullTextQuery = false;
    private boolean isQueryFullText = false;
    protected JsonNode rootNode;

    /**
     *
     */
    public RequestParser() {
        request = getNewRequest();
        adapter = new VarNameAdapter();
    }

    /**
     * @param adapter
     *
     */
    public RequestParser(VarNameAdapter adapter) {
        request = getNewRequest();
        this.adapter = adapter;
    }

    /**
     *
     * @return a new Request
     */
    protected abstract Request getNewRequest();

    private void internalParse() throws InvalidParseOperationException {
        GlobalDatasParser.sanityRequestCheck(sourceRequest);
        if (request != null) {
            request.reset();
        } else {
            request = getNewRequest();
        }
        lastDepth = 0;
        hasFullTextQuery = false;
        if (rootNode == null || rootNode.isMissingNode()) {
            throw new InvalidParseOperationException(
                "The current Node is missing(empty): RequestRoot");
        }
        if (rootNode.isArray()) {
            // should be 3, but each could be empty ( '{}' )
            if (rootNode.size() > 0) {
                rootParse(rootNode.get(0));
                if (rootNode.size() > 1) {
                    queryParse(rootNode.get(1));
                    if (rootNode.size() > 2) {
                        filterParse(rootNode.get(2));
                    }
                }
            }
        } else {
            /*
             * not as array but composite as { $roots: root, $query : query, $filter : filter }
             */
            rootParse(rootNode.get(GLOBAL.ROOTS.exactToken()));
            queryParse(rootNode.get(GLOBAL.QUERY.exactToken()));
            filterParse(rootNode.get(GLOBAL.FILTER.exactToken()));
        }
    }

    /**
     *
     * @param jsonRequest containing a parsed JSON as [ {root}, {query}, {filter} ] or { $roots: root, $query : query,
     *        $filter : filter }
     * @throws InvalidParseOperationException
     */
    public abstract void parse(final JsonNode jsonRequest) throws InvalidParseOperationException;

    /**
     *
     * @param jsonRequest containing a parsed JSON as [ {root}, {query}, {filter} ] or { $roots: root, $query : query,
     *        $filter : filter }
     * @throws InvalidParseOperationException
     */
    protected void parseJson(final JsonNode jsonRequest) throws InvalidParseOperationException {
        rootNode = jsonRequest;
        sourceRequest = jsonRequest.toString();
        internalParse();
    }

    /**
     *
     * @param srcrequest containing a JSON as [ {root}, {query}, {filter} ] or { $roots: root, $query : query, $filter :
     *        filter }
     * @throws InvalidParseOperationException
     */
    public abstract void parse(final String srcrequest) throws InvalidParseOperationException;

    /**
     *
     * @param srcrequest containing a JSON as [ {root}, {query}, {filter} ] or { $roots: root, $query : query, $filter :
     *        filter }
     * @throws InvalidParseOperationException
     */
    protected void parseString(final String srcrequest) throws InvalidParseOperationException {
        sourceRequest = srcrequest;
        rootNode = JsonHandler.getFromString(srcrequest);
        internalParse();
    }

    /**
     *
     * @param query containing only the JSON query part (no filter neither roots)
     * @throws InvalidParseOperationException
     */
    protected void parseQueryOnly(final String query)
        throws InvalidParseOperationException {
        GlobalDatasParser.sanityRequestCheck(query);
        sourceRequest = query;
        if (request != null) {
            request.reset();
        } else {
            request = getNewRequest();
        }
        lastDepth = 0;
        hasFullTextQuery = false;
        rootNode = JsonHandler.getFromString(query);
        if (rootNode.isMissingNode()) {
            throw new InvalidParseOperationException(
                "The current Node is missing(empty): RequestRoot");
        }
        // Not as array and no filter
        rootParse(JsonHandler.createArrayNode());
        queryParse(rootNode);
        filterParse(JsonHandler.createObjectNode());
    }

    /**
     * Will be used as extra arguments in the first query
     *
     * @param rootNode
     * @throws InvalidParseOperationException
     */
    protected void rootParse(final JsonNode rootNode)
        throws InvalidParseOperationException {
        if (rootNode == null) {
            return;
        }
        GlobalDatas.sanityParametersCheck(rootNode.toString(), GlobalDatas.nbRoots);
        try {
            request.addRoots((ArrayNode) rootNode);
        } catch (final Exception e) {
            throw new InvalidParseOperationException(
                "Parse in error for Roots: " + rootNode, e);
        }
    }

    /**
     * Filter part
     */
    protected void filterParse(final JsonNode rootNode)
        throws InvalidParseOperationException {
        if (rootNode == null) {
            return;
        }
        GlobalDatas.sanityParametersCheck(rootNode.toString(), GlobalDatas.nbFilters);
        try {
            request.setFilter(rootNode);
        } catch (final Exception e) {
            throw new InvalidParseOperationException(
                "Parse in error for Filter: " + rootNode, e);
        }
    }

    /**
     * [ query, query ] or { query } if one level only
     *
     * @param rootNode
     * @throws InvalidParseOperationException
     */
    protected void queryParse(final JsonNode rootNode)
        throws InvalidParseOperationException {
        if (rootNode == null) {
            return;
        }
        try {
            if (rootNode.isArray()) {
                // level are described as array entries, each being single
                // element (no name)
                int i = 0;
                for (final JsonNode level : rootNode) {
                    // now parse sub element as single command/value
                    analyzeRootQuery(level);
                    if (i == 1 && request.getQueries().get(i).getQUERY() == QUERY.PATH) {
                        throw new InvalidParseOperationException(
                            "Parse in error for Query since PATH is only allowed as first query: " + (i + 1));
                    }
                    i++;
                }
            } else {
                // 1 level only: might have 2 fields (request, exactdepth)
                analyzeRootQuery(rootNode);
            }
        } catch (final Exception e) {
            throw new InvalidParseOperationException(
                "Parse in error for Query: " + rootNode, e);
        }
    }

    /**
     * { expression, $exactdepth : exactdepth, $depth : /- depth }, $exactdepth and $depth being optional (mutual
     * exclusive)
     *
     * @param command
     * @throws InvalidParseOperationException
     * @throws InvalidCreateOperationException
     */
    protected void analyzeRootQuery(final JsonNode command)
        throws InvalidParseOperationException,
        InvalidCreateOperationException {
        if (command == null) {
            throw new InvalidParseOperationException("Not correctly parsed");
        }
        isQueryFullText = false;
        int relativedepth = 1; // default is immediate next level
        int exactdepth = 0; // default is to not specify any exact exactdepth
                            // (implicit)
        boolean isDepth = false;
        // first verify if exactdepth is set
        if (command.has(QUERYARGS.EXACTDEPTH.exactToken())) {
            final JsonNode jdepth =
                ((ObjectNode) command).remove(QUERYARGS.EXACTDEPTH.exactToken());
            if (jdepth != null) {
                exactdepth = jdepth.asInt();
                if (exactdepth == -1) {
                    exactdepth = GlobalDatasParser.MAXDEPTH;
                }
                isDepth = true;
            }
            ((ObjectNode) command).remove(QUERYARGS.DEPTH.exactToken());
        } else if (command.has(QUERYARGS.DEPTH.exactToken())) {
            final JsonNode jdepth =
                ((ObjectNode) command).remove(QUERYARGS.DEPTH.exactToken());
            if (jdepth != null) {
                relativedepth = jdepth.asInt();
                isDepth = true;
            }
        }
        // Root may be empty: ok since it means validate all "root nodes"
        if (command.size() == 0) {
            return;
        }
        // now single element
        final Entry<String, JsonNode> queryItem =
            JsonHandler.checkUnicity("RootRequest", command);
        Query query = null;
        if (queryItem.getKey().equalsIgnoreCase(QUERY.PATH.exactToken())) {
            if (isDepth) {
                throw new InvalidParseOperationException(
                    "Invalid combined command Depth and Path: " + command);
            }
            final int prevDepth = lastDepth;
            final ArrayNode array = (ArrayNode) queryItem.getValue();
            query = path(array, adapter);
            lastDepth = query.getExtraInfo();
            LOGGER.debug("Depth step: {}:{}", lastDepth, lastDepth - prevDepth);
        } else {
            query = analyzeOneCommand(queryItem.getKey(), queryItem.getValue());
            final int prevDepth = lastDepth;
            if (exactdepth > 0) {
                lastDepth = exactdepth;
            } else if (relativedepth != 0) {
                lastDepth += relativedepth;
            }
            LOGGER.debug("Depth step: {}:{}:{}:{}:{}", lastDepth, lastDepth - prevDepth,
                relativedepth, exactdepth, isDepth);
        }
        QueryDepthHelper.HELPER.setDepths(query.setFullText(isQueryFullText),
            exactdepth, relativedepth);
        hasFullTextQuery |= isQueryFullText;
        request.addQueries(query);
    }

    /**
     * Compute the QUERY from command
     *
     * @param queryroot
     * @return the QUERY
     * @throws InvalidParseOperationException
     */
    protected static final QUERY getRequestId(final String queryroot)
        throws InvalidParseOperationException {
        if (!queryroot.startsWith("$")) {
            throw new InvalidParseOperationException(
                "Incorrect request $command: " + queryroot);
        }
        final String command = queryroot.substring(1).toUpperCase();
        QUERY query = null;
        try {
            query = QUERY.valueOf(command);
        } catch (final IllegalArgumentException e) {
            throw new InvalidParseOperationException(
                "Invalid request command: " + command, e);
        }
        return query;
    }

    protected Query[] analyzeArrayCommand(final QUERY query, final JsonNode commands)
        throws InvalidParseOperationException,
        InvalidCreateOperationException {
        if (commands == null) {
            throw new InvalidParseOperationException("Not correctly parsed: " + query);
        }
        int nb = 0;
        final Query[] queries;
        if (commands.isArray()) {
            queries = new Query[commands.size()];
            // multiple elements in array
            for (final JsonNode subcommand : commands) {
                // one item
                final Entry<String, JsonNode> requestItem =
                    JsonHandler.checkUnicity(query.exactToken(), subcommand);
                final Query subquery =
                    analyzeOneCommand(requestItem.getKey(), requestItem.getValue());
                queries[nb++] = subquery;
            }
        } else {
            throw new InvalidParseOperationException(
                "Boolean operator needs an array of expression: " + commands);
        }
        if (query == QUERY.NOT) {
            if (queries.length == 1) {
                return queries;
            } else {
                final Query[] and = new Query[1];
                and[0] = and().add(queries);
                return and;
            }
        } else {
            return queries;
        }
    }

    /**
     * Check if the command is allowed using the "standard" database
     *
     * @param query
     * @return true if only valid in "index" database support
     */
    protected static boolean isCommandAsFullText(QUERY query) {
        switch (query) {
            case FLT:
            case MLT:
            case MATCH:
            case MATCH_PHRASE:
            case MATCH_PHRASE_PREFIX:
            case PREFIX:
                return true;
            default:
                return false;
        }
    }

    protected Query analyzeOneCommand(final String refCommand, final JsonNode command)
        throws InvalidParseOperationException,
        InvalidCreateOperationException {
        final QUERY query = getRequestId(refCommand);
        isQueryFullText |= isCommandAsFullText(query);
        switch (query) {
            case FLT:
            case MLT:
            case MATCH:
            case MATCH_PHRASE:
            case MATCH_PHRASE_PREFIX:
            case PREFIX:
            case NIN:
            case IN:
            case RANGE:
            case REGEX:
            case TERM:
            case WILDCARD:
            case EQ:
            case NE:
            case GT:
            case GTE:
            case LT:
            case LTE:
            case SEARCH:
            case SIZE:
                GlobalDatas.sanityValueCheck(command.toString());
                break;
            default:
        }
        switch (query) {
            case AND:
                return and().add(analyzeArrayCommand(query, command));
            case NOT:
                return not().add(analyzeArrayCommand(query, command));
            case OR:
                return or().add(analyzeArrayCommand(query, command));
            case EXISTS:
                return exists(command, adapter);
            case MISSING:
                return missing(command, adapter);
            case ISNULL:
                return isNull(command, adapter);
            case FLT:
                return flt(command, adapter);
            case MLT:
                return mlt(command, adapter);
            case MATCH:
                return match(command, adapter);
            case MATCH_PHRASE:
                return matchPhrase(command, adapter);
            case MATCH_PHRASE_PREFIX:
                return matchPhrasePrefix(command, adapter);
            case PREFIX:
                return prefix(command, adapter);
            case NIN:
                return nin(command, adapter);
            case IN:
                return in(command, adapter);
            case RANGE:
                return range(command, adapter);
            case REGEX:
                return regex(command, adapter);
            case TERM:
                return term(command, adapter);
            case WILDCARD:
                return wildcard(command, adapter);
            case EQ:
                return eq(command, adapter);
            case NE:
                return ne(command, adapter);
            case GT:
                return gt(command, adapter);
            case GTE:
                return gte(command, adapter);
            case LT:
                return lt(command, adapter);
            case LTE:
                return lte(command, adapter);
            case SEARCH:
                return search(command, adapter);
            case SIZE:
                return size(command, adapter);
            case GEOMETRY:
            case BOX:
            case POLYGON:
            case CENTER:
            case GEOINTERSECTS:
            case GEOWITHIN:
            case NEAR: {
                throw new InvalidParseOperationException(
                    "Unimplemented command: " + refCommand);
            }
            case PATH: {
                throw new InvalidParseOperationException(
                    "Invalid position for command: " + refCommand);
            }
            default:
                throw new InvalidParseOperationException(
                    "Invalid command: " + refCommand);
        }
    }

    @Override
    public String toString() {
        return new StringBuilder().append(request.toString()).append("\n\tLastLevel: ").append(lastDepth).toString();
    }

    /**
     * @return the Request
     */
    public Request getRequest() {
        return request;
    }

    /**
     * @return the source
     */
    public String getSource() {
        return sourceRequest;
    }

    /**
     * @return the lastDepth
     */
    public final int getLastDepth() {
        return lastDepth;
    }

    /**
     * @return the hasFullTextQuery
     */
    public final boolean hasFullTextQuery() {
        return hasFullTextQuery;
    }

    /**
     * @return True if the hint contains cache
     */
    public boolean hintCache() {
        final JsonNode jsonNode = request.getFilter().get(SELECTFILTER.HINT.exactToken());
        if (jsonNode == null) {
            // default
            return false;
        }
        final ArrayNode array = (ArrayNode) jsonNode;
        for (final JsonNode node : array) {
            if (ParserTokens.FILTERARGS.CACHE.exactToken().equals(node.asText())) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return True if the hint contains notimeout
     */
    public boolean hintNoTimeout() {
        final JsonNode jsonNode = request.getFilter().get(SELECTFILTER.HINT.exactToken());
        if (jsonNode != null) {
            final ArrayNode array = (ArrayNode) jsonNode;
            for (final JsonNode node : array) {
                if (ParserTokens.FILTERARGS.NOTIMEOUT.exactToken().equals(node.asText())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @return the model between Units/ObjectGroups/Objects (in that order)
     */
    public FILTERARGS model() {
        final JsonNode jsonNode = request.getFilter().get(SELECTFILTER.HINT.exactToken());
        if (jsonNode != null) {
            final ArrayNode array = (ArrayNode) jsonNode;
            for (final JsonNode node : array) {
                if (FILTERARGS.UNITS.exactToken().equals(node.asText())) {
                    return FILTERARGS.UNITS;
                } else if (FILTERARGS.OBJECTGROUPS.exactToken().equals(node.asText())) {
                    return FILTERARGS.OBJECTGROUPS;
                } else if (FILTERARGS.OBJECTS.exactToken().equals(node.asText())) {
                    return FILTERARGS.OBJECTS;
                }
            }
        }
        return FILTERARGS.UNITS;
    }

}
