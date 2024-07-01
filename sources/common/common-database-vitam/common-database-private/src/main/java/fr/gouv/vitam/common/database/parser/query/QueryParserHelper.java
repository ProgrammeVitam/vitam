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
package fr.gouv.vitam.common.database.parser.query;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.database.builder.query.NopQuery;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.QUERY;
import fr.gouv.vitam.common.database.builder.request.configuration.GlobalDatas;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.parser.request.adapter.VarNameAdapter;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;

import java.util.Map.Entry;

/**
 * Query from Parser Helper
 */
public class QueryParserHelper extends QueryHelper {

    protected QueryParserHelper() {}

    /**
     * @param array primary list of path in the future PathQuery
     * @param adapter VarNameAdapter
     * @return a PathQuery
     */
    public static final PathQuery path(final JsonNode array, final VarNameAdapter adapter) {
        return new PathQuery(QUERY.PATH, array, adapter);
    }

    /**
     * @param command JsonNode
     * @param adapter VarNameAdapter
     * @return a CompareQuery using EQ comparator
     * @throws InvalidParseOperationException if could not parse to JSON
     */
    public static final CompareQuery eq(final JsonNode command, final VarNameAdapter adapter)
        throws InvalidParseOperationException {
        return new CompareQuery(QUERY.EQ, command, adapter);
    }

    /**
     * @param command JsonNode
     * @param adapter VarNameAdapter
     * @return a CompareQuery using NE comparator
     * @throws InvalidParseOperationException if could not parse to JSON
     */
    public static final CompareQuery ne(final JsonNode command, final VarNameAdapter adapter)
        throws InvalidParseOperationException {
        return new CompareQuery(QUERY.NE, command, adapter);
    }

    /**
     * @param command JsonNode
     * @param adapter VarNameAdapter
     * @return a CompareQuery using LT (less than) comparator
     * @throws InvalidParseOperationException if could not parse to JSON
     */
    public static final CompareQuery lt(final JsonNode command, final VarNameAdapter adapter)
        throws InvalidParseOperationException {
        return new CompareQuery(QUERY.LT, command, adapter);
    }

    /**
     * @param command JsonNode
     * @param adapter VarNameAdapter
     * @return a CompareQuery using LTE (less than or equal) comparator
     * @throws InvalidParseOperationException if could not parse to JSON
     */
    public static final CompareQuery lte(final JsonNode command, final VarNameAdapter adapter)
        throws InvalidParseOperationException {
        return new CompareQuery(QUERY.LTE, command, adapter);
    }

    /**
     * @param command JsonNode
     * @param adapter VarNameAdapter
     * @return a CompareQuery using GT (greater than) comparator
     * @throws InvalidParseOperationException if could not parse to JSON
     */
    public static final CompareQuery gt(final JsonNode command, final VarNameAdapter adapter)
        throws InvalidParseOperationException {
        return new CompareQuery(QUERY.GT, command, adapter);
    }

    /**
     * @param command JsonNode
     * @param adapter VarNameAdapter
     * @return a CompareQuery using GTE (greater than or equal) comparator
     * @throws InvalidParseOperationException if could not parse to JSON
     */
    public static final CompareQuery gte(final JsonNode command, final VarNameAdapter adapter)
        throws InvalidParseOperationException {
        return new CompareQuery(QUERY.GTE, command, adapter);
    }

    /**
     * @param command JsonNode
     * @param adapter VarNameAdapter
     * @return a CompareQuery using SIZE comparator
     * @throws InvalidParseOperationException if could not parse to JSON
     */
    public static final CompareQuery size(final JsonNode command, final VarNameAdapter adapter)
        throws InvalidParseOperationException {
        return new CompareQuery(QUERY.SIZE, command, adapter);
    }

    /**
     * @param command JsonNode
     * @param adapter VarNameAdapter
     * @return an ExistsQuery
     * @throws InvalidCreateOperationException using Exists operator
     * @throws InvalidParseOperationException if could not parse to JSON
     */
    public static final ExistsQuery exists(final JsonNode command, final VarNameAdapter adapter)
        throws InvalidCreateOperationException, InvalidParseOperationException {
        return new ExistsQuery(QUERY.EXISTS, command, adapter);
    }

    /**
     * @param command JsonNode
     * @param adapter VarNameAdapter
     * @return an ExistsQuery using Missing operator
     * @throws InvalidCreateOperationException using Exists operator
     * @throws InvalidParseOperationException if could not parse to JSON
     */
    public static final ExistsQuery missing(final JsonNode command, final VarNameAdapter adapter)
        throws InvalidCreateOperationException, InvalidParseOperationException {
        return new ExistsQuery(QUERY.MISSING, command, adapter);
    }

    /**
     * @param command JsonNode
     * @param adapter VarNameAdapter
     * @return an ExistsQuery using isNull operator
     * @throws InvalidCreateOperationException using Exists operator
     * @throws InvalidParseOperationException if could not parse to JSON
     */
    public static final ExistsQuery isNull(final JsonNode command, final VarNameAdapter adapter)
        throws InvalidCreateOperationException, InvalidParseOperationException {
        return new ExistsQuery(QUERY.ISNULL, command, adapter);
    }

    /**
     * @param command JsonNode
     * @param adapter VarNameAdapter
     * @return an InQuery using IN operator
     * @throws InvalidParseOperationException if could not parse to JSON
     */
    public static final InQuery in(final JsonNode command, final VarNameAdapter adapter)
        throws InvalidParseOperationException {
        return new InQuery(QUERY.IN, command, adapter);
    }

    /**
     * @param command JsonNode
     * @param adapter VarNameAdapter
     * @return an InQuery using NIN (not in) operator
     * @throws InvalidParseOperationException if could not parse to JSON
     */
    public static final InQuery nin(final JsonNode command, final VarNameAdapter adapter)
        throws InvalidParseOperationException {
        return new InQuery(QUERY.NIN, command, adapter);
    }

    /**
     * @param command JsonNode
     * @param adapter VarNameAdapter
     * @return a MatchQuery using MATCH operator
     * @throws InvalidParseOperationException if could not parse to JSON
     */
    public static final MatchQuery match(final JsonNode command, final VarNameAdapter adapter)
        throws InvalidParseOperationException {
        return new MatchQuery(QUERY.MATCH, command, adapter);
    }

    /**
     * @param command JsonNode
     * @param adapter VarNameAdapter
     * @return a MatchQuery using MATCH operator
     * @throws InvalidParseOperationException if could not parse to JSON
     */
    public static final MatchQuery matchAll(final JsonNode command, final VarNameAdapter adapter)
        throws InvalidParseOperationException {
        return new MatchQuery(QUERY.MATCH_ALL, command, adapter);
    }

    /**
     * @param command JsonNode
     * @param adapter VarNameAdapter
     * @return a MatchQuery using MATCH_PHRASE operator
     * @throws InvalidParseOperationException if could not parse to JSON
     */
    public static final MatchQuery matchPhrase(final JsonNode command, final VarNameAdapter adapter)
        throws InvalidParseOperationException {
        return new MatchQuery(QUERY.MATCH_PHRASE, command, adapter);
    }

    /**
     * @param command JsonNode
     * @param adapter VarNameAdapter
     * @return a MatchQuery using MATCH_PHRASE_PREFIX operator
     * @throws InvalidParseOperationException if could not parse to JSON
     */
    public static final MatchQuery matchPhrasePrefix(final JsonNode command, final VarNameAdapter adapter)
        throws InvalidParseOperationException {
        return new MatchQuery(QUERY.MATCH_PHRASE_PREFIX, command, adapter);
    }

    /**
     * @param command JsonNode
     * @param adapter VarNameAdapter
     * @return a SearchQuery using REGEX operator
     * @throws InvalidParseOperationException if could not parse to JSON
     */
    public static final SearchQuery regex(final JsonNode command, final VarNameAdapter adapter)
        throws InvalidParseOperationException {
        return new SearchQuery(QUERY.REGEX, command, adapter);
    }

    /**
     * @param command JsonNode
     * @param adapter VarNameAdapter
     * @return a SearchQuery using SEARCH operator
     * @throws InvalidParseOperationException if could not parse to JSON
     */
    public static final SearchQuery search(final JsonNode command, final VarNameAdapter adapter)
        throws InvalidParseOperationException {
        return new SearchQuery(QUERY.SEARCH, command, adapter);
    }

    /**
     * @param command JsonNode
     * @param adapter VarNameAdapter
     * @return a SearchQuery using nested search mode
     * @throws InvalidParseOperationException if could not parse to JSON
     */
    public static final SearchQuery nestedSearch(final JsonNode command, final VarNameAdapter adapter)
        throws InvalidParseOperationException {
        return new SearchQuery(QUERY.SUBOBJECT, command, adapter);
    }

    /**
     * @param command JsonNode
     * @param adapter VarNameAdapter
     * @return a TermQuery
     * @throws InvalidParseOperationException if could not parse to JSON
     */
    public static final TermQuery term(final JsonNode command, final VarNameAdapter adapter)
        throws InvalidParseOperationException {
        return new TermQuery(QUERY.TERM, command, adapter);
    }

    /**
     * @param command JsonNode
     * @param adapter VarNameAdapter
     * @return a WildcardQuery
     * @throws InvalidParseOperationException if could not parse to JSON
     */
    public static final WildcardQuery wildcard(final JsonNode command, final VarNameAdapter adapter)
        throws InvalidParseOperationException {
        return new WildcardQuery(QUERY.WILDCARD, command, adapter);
    }

    /**
     * @param command JsonNode
     * @param adapter VarNameAdapter
     * @return a MltQuery using a FLT (fuzzy like this) operator
     * @throws InvalidParseOperationException if could not parse to JSON
     */
    public static final MltQuery flt(final JsonNode command, final VarNameAdapter adapter)
        throws InvalidParseOperationException {
        return new MltQuery(QUERY.FLT, command, adapter);
    }

    /**
     * @param command JsonNode
     * @param adapter VarNameAdapter
     * @return a MltQuery using a MLT (more like this) operator
     * @throws InvalidParseOperationException if could not parse to JSON
     */
    public static final MltQuery mlt(final JsonNode command, final VarNameAdapter adapter)
        throws InvalidParseOperationException {
        return new MltQuery(QUERY.MLT, command, adapter);
    }

    /**
     * @param command JsonNode
     * @param adapter VarNameAdapter
     * @return a RangeQuery
     * @throws InvalidParseOperationException if could not parse to JSON
     */
    public static final RangeQuery range(final JsonNode command, final VarNameAdapter adapter)
        throws InvalidParseOperationException {
        return new RangeQuery(QUERY.RANGE, command, adapter);
    }

    /**
     * Constructs a null operation (nop) query, meaning there is no 'where' demand.
     *
     * @return a NopQuery
     * @throws InvalidCreateOperationException using NOP operator
     */
    public static final NopQuery nop() throws InvalidCreateOperationException {
        return new NopQuery();
    }

    /**
     * Transform command to query
     *
     * @param refCommand ref of command
     * @param command command
     * @param adapter dapater
     * @return query
     * @throws InvalidParseOperationException if could not parse to JSON
     * @throws InvalidCreateOperationException if could not create the query
     */
    public static final Query query(final String refCommand, final JsonNode command, final VarNameAdapter adapter)
        throws InvalidParseOperationException, InvalidCreateOperationException {
        final QUERY query = getRequestId(refCommand);
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
            case SUBOBJECT:
                GlobalDatas.sanityValueCheck(command.toString());
                break;
            default:
        }
        Query dslQuery = null;
        Query[] subQueries = null;
        switch (query) {
            case AND:
                subQueries = analyzeArrayCommand(query, command, adapter);
                dslQuery = and().add(subQueries);
                for (Query subQuery : subQueries) {
                    dslQuery.setFullText(dslQuery.isFullText() || isCommandAsFullText(subQuery.getQUERY()));
                }
                break;
            case NOT:
                subQueries = analyzeArrayCommand(query, command, adapter);
                dslQuery = not().add(analyzeArrayCommand(query, command, adapter));
                for (Query subQuery : subQueries) {
                    dslQuery.setFullText(dslQuery.isFullText() || isCommandAsFullText(subQuery.getQUERY()));
                }

                break;
            case OR:
                subQueries = analyzeArrayCommand(query, command, adapter);
                dslQuery = or().add(analyzeArrayCommand(query, command, adapter));
                for (Query subQuery : subQueries) {
                    dslQuery.setFullText(dslQuery.isFullText() || isCommandAsFullText(subQuery.getQUERY()));
                }
                break;
            case EXISTS:
                dslQuery = exists(command, adapter);
                break;
            case MISSING:
                dslQuery = missing(command, adapter);
                break;
            case ISNULL:
                dslQuery = isNull(command, adapter);
                break;
            case FLT:
                dslQuery = flt(command, adapter);
                break;
            case MLT:
                dslQuery = mlt(command, adapter);
                break;
            case MATCH:
                dslQuery = match(command, adapter);
                break;
            case MATCH_ALL:
                dslQuery = matchAll(command, adapter);
                break;
            case MATCH_PHRASE:
                dslQuery = matchPhrase(command, adapter);
                break;
            case MATCH_PHRASE_PREFIX:
                dslQuery = matchPhrasePrefix(command, adapter);
                break;
            case NIN:
                dslQuery = nin(command, adapter);
                break;
            case IN:
                dslQuery = in(command, adapter);
                break;
            case RANGE:
                dslQuery = range(command, adapter);
                break;
            case REGEX:
                dslQuery = regex(command, adapter);
                break;
            case TERM:
                dslQuery = term(command, adapter);
                break;
            case WILDCARD:
                dslQuery = wildcard(command, adapter);
                break;
            case EQ:
                dslQuery = eq(command, adapter);
                break;
            case NE:
                dslQuery = ne(command, adapter);
                break;
            case GT:
                dslQuery = gt(command, adapter);
                break;
            case GTE:
                dslQuery = gte(command, adapter);
                break;
            case LT:
                dslQuery = lt(command, adapter);
                break;
            case LTE:
                dslQuery = lte(command, adapter);
                break;
            case SEARCH:
                dslQuery = search(command, adapter);
                break;
            case SUBOBJECT:
                dslQuery = nestedSearch(command, adapter);
                break;
            case SIZE:
                dslQuery = size(command, adapter);
                break;
            case NOP:
                dslQuery = null;
                break;
            case GEOMETRY:
            case BOX:
            case POLYGON:
            case CENTER:
            case GEOINTERSECTS:
            case GEOWITHIN:
            case NEAR:
                throw new InvalidParseOperationException("Unimplemented command: " + refCommand);
            case PATH:
                throw new InvalidParseOperationException("Invalid position for command: " + refCommand);
            default:
                throw new InvalidParseOperationException("Invalid command: " + refCommand);
        }
        if (dslQuery != null) {
            dslQuery.setFullText(dslQuery.isFullText() || isCommandAsFullText(query));
        }
        return dslQuery;
    }

    /**
     * Compute the QUERY from command
     *
     * @param queryroot String
     * @return the QUERY
     * @throws InvalidParseOperationException if queryroot could not parse to JSON
     */
    public static final QUERY getRequestId(final String queryroot) throws InvalidParseOperationException {
        if (!queryroot.startsWith(BuilderToken.DEFAULT_PREFIX)) {
            throw new InvalidParseOperationException("Incorrect request $command: " + queryroot);
        }
        final String command = queryroot.substring(1).toUpperCase();
        QUERY query;
        try {
            query = QUERY.valueOf(command);
        } catch (final IllegalArgumentException e) {
            throw new InvalidParseOperationException("Invalid request command: " + command, e);
        }
        return query;
    }

    /**
     * Analyze an array of commands
     *
     * @param query query
     * @param commands commands
     * @param adapter adapter
     * @return array of Queries
     * @throws InvalidParseOperationException if could not parse to JSON
     * @throws InvalidCreateOperationException if could not create the query
     */
    public static final Query[] analyzeArrayCommand(
        final QUERY query,
        final JsonNode commands,
        final VarNameAdapter adapter
    ) throws InvalidParseOperationException, InvalidCreateOperationException {
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
                final Entry<String, JsonNode> requestItem = JsonHandler.checkUnicity(query.exactToken(), subcommand);
                final Query subquery = query(requestItem.getKey(), requestItem.getValue(), adapter);
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
            throw new InvalidParseOperationException("Boolean operator needs an array of expression: " + commands);
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
}
