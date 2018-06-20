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
package fr.gouv.vitam.common.database.parser.request;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.and;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.not;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.or;
import static fr.gouv.vitam.common.database.parser.query.QueryParserHelper.eq;
import static fr.gouv.vitam.common.database.parser.query.QueryParserHelper.exists;
import static fr.gouv.vitam.common.database.parser.query.QueryParserHelper.flt;
import static fr.gouv.vitam.common.database.parser.query.QueryParserHelper.gt;
import static fr.gouv.vitam.common.database.parser.query.QueryParserHelper.gte;
import static fr.gouv.vitam.common.database.parser.query.QueryParserHelper.in;
import static fr.gouv.vitam.common.database.parser.query.QueryParserHelper.isNull;
import static fr.gouv.vitam.common.database.parser.query.QueryParserHelper.lt;
import static fr.gouv.vitam.common.database.parser.query.QueryParserHelper.lte;
import static fr.gouv.vitam.common.database.parser.query.QueryParserHelper.match;
import static fr.gouv.vitam.common.database.parser.query.QueryParserHelper.matchAll;
import static fr.gouv.vitam.common.database.parser.query.QueryParserHelper.matchPhrase;
import static fr.gouv.vitam.common.database.parser.query.QueryParserHelper.matchPhrasePrefix;
import static fr.gouv.vitam.common.database.parser.query.QueryParserHelper.missing;
import static fr.gouv.vitam.common.database.parser.query.QueryParserHelper.mlt;
import static fr.gouv.vitam.common.database.parser.query.QueryParserHelper.ne;
import static fr.gouv.vitam.common.database.parser.query.QueryParserHelper.nin;
import static fr.gouv.vitam.common.database.parser.query.QueryParserHelper.range;
import static fr.gouv.vitam.common.database.parser.query.QueryParserHelper.regex;
import static fr.gouv.vitam.common.database.parser.query.QueryParserHelper.search;
import static fr.gouv.vitam.common.database.parser.query.QueryParserHelper.size;
import static fr.gouv.vitam.common.database.parser.query.QueryParserHelper.term;
import static fr.gouv.vitam.common.database.parser.query.QueryParserHelper.wildcard;

import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;

import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.request.AbstractRequest;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.FILTERARGS;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.QUERY;
import fr.gouv.vitam.common.database.builder.request.configuration.GlobalDatas;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.parser.query.QueryParserHelper;
import fr.gouv.vitam.common.database.parser.request.adapter.VarNameAdapter;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;

import java.util.Iterator;

/**
 * Abstract class implementing Parser for a Request
 *
 * Common abstract for both Multiple and Single Request
 *
 * @param <E> is one of RequestMultiple or RequestSingle
 */
public abstract class AbstractParser<E extends AbstractRequest> {

    protected VarNameAdapter adapter;
    protected String sourceRequest;

    protected E request;

    /**
     * Contains queries to be computed by a full text index
     */
    protected boolean hasFullTextQuery = false;
    /**
     * Current analyzed query to be computed by a full text index
     */
    protected boolean hasFullTextCurrentQuery = false;
    protected JsonNode rootNode;

    /**
     * @return the source
     */
    public String getSource() {
        return sourceRequest;
    }


    /**
     * @return the rootNode
     */
    public JsonNode getRootNode() {
        return rootNode;
    }


    /**
     * @return the Request
     */
    public E getRequest() {
        return request;
    }

    /**
     *
     * @return a new Request
     */
    protected abstract E getNewRequest();

    /**
     *
     * @param jsonRequest containing a parsed JSON as [ {root}, {query}, {filter} ] or { $roots: root, $query : query,
     *        $filter : filter }
     * @throws InvalidParseOperationException if jsonRequest could not parse to JSON
     */
    public abstract void parse(final JsonNode jsonRequest) throws InvalidParseOperationException;


    /**
     * @param jsonRequest containing a parsed JSON as { $roots: root, $query : query, $filter : filter }
     * @throws InvalidParseOperationException if jsonRequest could not parse to JSON
     */
    protected void parseJson(final JsonNode jsonRequest) throws InvalidParseOperationException {
        rootNode = jsonRequest;
        sourceRequest = JsonHandler.unprettyPrint(jsonRequest);
    }

    /**
     * @return the hasFullTextQuery
     */
    public final boolean hasFullTextQuery() {
        return hasFullTextQuery;
    }


    protected Query[] analyzeArrayCommand(final QUERY query, final JsonNode commands)
        throws InvalidParseOperationException,
        InvalidCreateOperationException {
        if (commands == null) {
            throw new InvalidParseOperationException("Not correctly parsed: " + query);
        }
        int nb = 0;
        Query[] queries;
        if (commands.isArray()) {
            queries = new Query[commands.size()];
            // multiple elements in array
            for (final JsonNode subcommand : commands) {
                // one item
                final Entry<String, JsonNode> requestItem =
                    JsonHandler.checkUnicity(query.exactToken(), subcommand);
                final Query subquery =
                    analyzeOneCommand(requestItem.getKey(), requestItem.getValue());
                if (subquery == null) {
                    // NOP
                    continue;
                }
                queries[nb++] = subquery;
            }
            if (nb != queries.length) {
                final Query[] newQueries = new Query[nb];
                for (int i = 0; i < nb; i++) {
                    newQueries[i] = queries[i];
                }
                queries = newQueries;
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
     * @param query QUERY
     * @return true if only valid in "index" database support
     */
    protected static boolean isCommandAsFullText(QUERY query) {
        switch (query) {
            case FLT:
            case MLT:
            case MATCH:
            case MATCH_ALL:
            case MATCH_PHRASE:
            case MATCH_PHRASE_PREFIX:
                return true;
            default:
                return false;
        }
    }



    /**
     * Compute the QUERY from command
     *
     * @param queryroot String
     * @return the QUERY
     * @throws InvalidParseOperationException if queryroot could not parse to JSON
     */
    public static final QUERY getRequestId(final String queryroot)
        throws InvalidParseOperationException {
        if (!queryroot.startsWith(BuilderToken.DEFAULT_PREFIX)) {
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

    protected Query analyzeOneCommand(final String refCommand, final JsonNode command)
        throws InvalidParseOperationException,
        InvalidCreateOperationException {
        final QUERY query = getRequestId(refCommand);
        hasFullTextCurrentQuery |= isCommandAsFullText(query);
        switch (query) {
            case FLT:
            case MLT:
            case MATCH:
            case MATCH_ALL:
            case MATCH_PHRASE:
            case MATCH_PHRASE_PREFIX:
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
            case MATCH_ALL:
                return matchAll(command, adapter);
            case MATCH_PHRASE:
                return matchPhrase(command, adapter);
            case MATCH_PHRASE_PREFIX:
                return matchPhrasePrefix(command, adapter);
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
            case NOP:
                return null;
            case GEOMETRY:
            case BOX:
            case POLYGON:
            case CENTER:
            case GEOINTERSECTS:
            case GEOWITHIN:
            case NEAR:
                throw new InvalidParseOperationException(
                    "Unimplemented command: " + refCommand);
            case PATH:
                throw new InvalidParseOperationException(
                    "Invalid position for command: " + refCommand);
            default:
                throw new InvalidParseOperationException(
                    "Invalid command: " + refCommand);
        }
    }

    /**
     * @return int the depth of the query
     */
    public abstract int getLastDepth();


    /**
     * @return FILTERARGS the filter argument
     */
    public abstract FILTERARGS model();


    /**
     * @return true if not time out
     */
    public abstract boolean hintNoTimeout();


    /**
     * @return true if not cache hint
     */
    public abstract boolean hintCache();

    protected void parseOrderByFilter(JsonNode filterNode) throws InvalidParseOperationException {
        if (filterNode.has(BuilderToken.SELECTFILTER.ORDERBY.exactToken())) {
            final ObjectNode node = (ObjectNode) filterNode.get(BuilderToken.SELECTFILTER.ORDERBY.exactToken());
            ObjectNode finalNode = node.deepCopy();
            final Iterator<String> names = node.fieldNames();
            while (names.hasNext()) {
                final String name = names.next();
                final String dbName = adapter.getVariableName(name);

                // Force update rootNode with correct dbName (replace '#' by '_')
                if (null != dbName) {
                    final JsonNode value = finalNode.remove(name);
                    finalNode.set(dbName, value);
                }
            }
            node.removeAll();
            node.setAll(finalNode);
        }
    }
}
