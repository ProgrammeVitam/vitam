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
package fr.gouv.vitam.common.database.parser.request.multiple;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.FILTERARGS;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.GLOBAL;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.QUERY;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.QUERYARGS;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.SELECTFILTER;
import fr.gouv.vitam.common.database.builder.request.configuration.GlobalDatas;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.RequestMultiple;
import fr.gouv.vitam.common.database.parser.query.ParserTokens;
import fr.gouv.vitam.common.database.parser.query.helper.QueryDepthHelper;
import fr.gouv.vitam.common.database.parser.request.AbstractParser;
import fr.gouv.vitam.common.database.parser.request.GlobalDatasParser;
import fr.gouv.vitam.common.database.parser.request.adapter.VarNameAdapter;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

import java.util.Map.Entry;

import static fr.gouv.vitam.common.database.parser.query.QueryParserHelper.path;

/**
 * Partial Request Parser (common base): { $roots: root, $query : query, $filter : filter }
 */
public abstract class RequestParserMultiple extends AbstractParser<RequestMultiple> {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(RequestParserMultiple.class);

    private static final int DEFAULT_RELATIVE_DEPTH = 1000;
    /**
     * Last computed Depth
     */
    protected int lastDepth = 0;

    /**
     * Constructor for Internal API
     */
    public RequestParserMultiple() {
        request = getNewRequest();
        adapter = new VarNameAdapter();
    }

    /**
     * Constructor for Metadata
     *
     * @param adapter VarNameAdapter
     */
    public RequestParserMultiple(VarNameAdapter adapter) {
        request = getNewRequest();
        this.adapter = adapter;
    }

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
            throw new InvalidParseOperationException("The current Node is missing(empty): RequestRoot");
        }

        /*
         * not as array but composite as { $roots: root, $query : query, $filter : filter }
         */
        rootParse(rootNode.get(GLOBAL.ROOTS.exactToken()));
        queryParse(rootNode.get(GLOBAL.QUERY.exactToken()));
        filterParse(rootNode.get(GLOBAL.FILTER.exactToken()));
    }

    @Override
    protected void parseJson(final JsonNode jsonRequest) throws InvalidParseOperationException {
        super.parseJson(jsonRequest);
        internalParse();
    }

    /**
     * @param query containing only the JSON query part (no filter neither roots)
     * @throws InvalidParseOperationException if query could not parse to JSON
     */
    protected void parseQueryOnly(final String query) throws InvalidParseOperationException {
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
            throw new InvalidParseOperationException("The current Node is missing(empty): RequestRoot");
        }
        // Not as array and no filter
        rootParse(JsonHandler.createArrayNode());
        queryParse(rootNode);
        filterParse(JsonHandler.createObjectNode());
    }

    /**
     * Will be used as extra arguments in the first query
     *
     * @param rootNode JsonNode the root of the request
     * @throws InvalidParseOperationException if rootNode could not parse to JSON
     */
    protected void rootParse(final JsonNode rootNode) throws InvalidParseOperationException {
        if (rootNode == null) {
            return;
        }
        GlobalDatas.sanityParametersCheck(rootNode.toString(), GlobalDatas.NB_ROOTS);
        try {
            request.addRoots((ArrayNode) rootNode);
        } catch (final Exception e) {
            throw new InvalidParseOperationException("Parse in error for Roots: " + rootNode, e);
        }
    }

    /**
     * Filter part
     *
     * @param rootNode JsonNode The filter of the request
     * @throws InvalidParseOperationException if rootNode could not parse to JSON
     */
    protected void filterParse(final JsonNode rootNode) throws InvalidParseOperationException {
        if (rootNode == null) {
            return;
        }
        GlobalDatas.sanityParametersCheck(rootNode.toString(), GlobalDatas.NB_FILTERS);
        try {
            // Check valid variable names first
            parseOrderByFilter(rootNode);
            request.setFilter(rootNode);
        } catch (final Exception e) {
            throw new InvalidParseOperationException("Parse in error for Filter: " + rootNode, e);
        }
    }

    /**
     * [ query, query ] or { query } if one level only
     *
     * @param rootNode JsonNode the query of the request
     * @throws InvalidParseOperationException if rootNode could not parse to JSON
     */
    protected void queryParse(final JsonNode rootNode) throws InvalidParseOperationException {
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
                            "Parse in error for Query since PATH is only allowed as first query: " + (i + 1)
                        );
                    }
                    i++;
                }
            } else {
                // 1 level only: might have 2 fields (request, exactdepth)
                analyzeRootQuery(rootNode);
            }
        } catch (final Exception e) {
            throw new InvalidParseOperationException("Parse in error for Query: " + rootNode, e);
        }
    }

    /**
     * { expression, $exactdepth : exactdepth, $depth : /- depth }, $exactdepth and $depth being optional (mutual
     * exclusive)
     *
     * @param command JsonNode
     * @throws InvalidParseOperationException if query could not parse to JSON
     * @throws InvalidCreateOperationException if could not create query in JSON
     */
    protected void analyzeRootQuery(final JsonNode command)
        throws InvalidParseOperationException, InvalidCreateOperationException {
        if (command == null) {
            throw new InvalidParseOperationException("Not correctly parsed");
        }
        // new Query to analyze, so reset to false
        hasFullTextCurrentQuery = false;
        // default is 1
        int relativedepth = 1;
        // default is to not specify any exact exactdepth (implicit)
        int exactdepth = 0;
        boolean isDepth = false;
        // first verify if exactdepth is set
        if (command.has(QUERYARGS.EXACTDEPTH.exactToken())) {
            final JsonNode jdepth = ((ObjectNode) command).remove(QUERYARGS.EXACTDEPTH.exactToken());
            if (jdepth != null) {
                exactdepth = jdepth.asInt();
                if (exactdepth == -1) {
                    exactdepth = GlobalDatas.MAXDEPTH;
                }
                isDepth = true;
            }
            ((ObjectNode) command).remove(QUERYARGS.DEPTH.exactToken());
        } else if (command.has(QUERYARGS.DEPTH.exactToken())) {
            final JsonNode jdepth = ((ObjectNode) command).remove(QUERYARGS.DEPTH.exactToken());
            if (jdepth != null) {
                relativedepth = jdepth.asInt();
                isDepth = true;
            }
        } else {
            // this means depth is not set, so instead of setting a value equals to one, lets put the default value
            relativedepth = DEFAULT_RELATIVE_DEPTH;
        }
        // Root may be empty: ok since it means validate all "root nodes"
        if (command.size() == 0) {
            return;
        }
        // now single element
        final Entry<String, JsonNode> queryItem = JsonHandler.checkUnicity("RootRequest", command);
        Query query = null;
        if (queryItem.getKey().equalsIgnoreCase(QUERY.PATH.exactToken())) {
            if (isDepth) {
                throw new InvalidParseOperationException("Invalid combined command Depth and Path: " + command);
            }
            final int prevDepth = lastDepth;
            final ArrayNode array = (ArrayNode) queryItem.getValue();
            query = path(array, adapter);
            lastDepth = query.getExtraInfo();
            LOGGER.debug("Depth step: {}:{}", lastDepth, lastDepth - prevDepth);
        } else {
            query = analyzeOneCommand(queryItem.getKey(), queryItem.getValue());
            if (query == null) {
                // NOP
                return;
            }
            final int prevDepth = lastDepth;
            if (exactdepth > 0) {
                lastDepth = exactdepth;
            } else if (relativedepth != 0) {
                lastDepth += relativedepth;
            }
            LOGGER.debug(
                "Depth step: {}:{}:{}:{}:{}",
                lastDepth,
                lastDepth - prevDepth,
                relativedepth,
                exactdepth,
                isDepth
            );
        }

        if (adapter.metadataAdapter()) {
            QueryDepthHelper.HELPER.setDepths(query.setFullText(hasFullTextCurrentQuery), exactdepth, relativedepth);
        } else {
            query.setFullText(hasFullTextCurrentQuery);
            if (exactdepth != 0) {
                query.setExactDepthLimit(exactdepth);
            } else {
                query.setRelativeDepthLimit(relativedepth);
            }
        }
        hasFullTextQuery |= hasFullTextCurrentQuery;
        request.addQueries(query);
    }

    @Override
    public String toString() {
        return new StringBuilder().append(request.toString()).append("\n\tLastLevel: ").append(lastDepth).toString();
    }

    /**
     * @return the Request
     */
    @Override
    public RequestMultiple getRequest() {
        return request;
    }

    /**
     * @return the lastDepth
     */
    @Override
    public final int getLastDepth() {
        return lastDepth;
    }

    /**
     * @return True if the hint contains cache
     */
    @Override
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
    @Override
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
    @Override
    public FILTERARGS model() {
        final JsonNode jsonNode = request.getFilter().get(SELECTFILTER.HINT.exactToken());
        if (jsonNode != null) {
            final ArrayNode array = (ArrayNode) jsonNode;
            for (final JsonNode node : array) {
                if (FILTERARGS.UNITS.exactToken().equals(node.asText())) {
                    return FILTERARGS.UNITS;
                } else if (FILTERARGS.OBJECTGROUPS.exactToken().equals(node.asText())) {
                    return FILTERARGS.OBJECTGROUPS;
                }
            }
        }
        return FILTERARGS.UNITS;
    }

    /**
     * Returns whether total hits is computed (defaults to false).
     * See ES documentation for more details on "track_total_hits"
     */
    public boolean trackTotalHits() {
        final JsonNode jsonNode = request.getFilter().get(SELECTFILTER.TRACK_TOTAL_HITS.exactToken());
        return jsonNode != null && jsonNode.isBoolean() && jsonNode.booleanValue();
    }

    /**
     * get ScrollId
     *
     * @return the limit
     */
    public String getFinalScrollId() {
        final JsonNode node = request.getFilter().get(SELECTFILTER.SCROLL_ID.exactToken());
        if (node != null) {
            return node.asText();
        }
        return "";
    }

    /**
     * get ScrollTimeout
     *
     * @return ScrollTimeout
     */
    public int getFinalScrollTimeout() {
        final JsonNode node = request.getFilter().get(SELECTFILTER.SCROLL_TIMEOUT.exactToken());
        if (node != null) {
            return node.asInt();
        }
        return 0;
    }
}
