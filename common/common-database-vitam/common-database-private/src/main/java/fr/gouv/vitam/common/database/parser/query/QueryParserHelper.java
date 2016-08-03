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
package fr.gouv.vitam.common.database.parser.query;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.QUERY;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.parser.request.adapter.VarNameAdapter;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;

/**
 * Query from Parser Helper
 *
 */
public class QueryParserHelper extends QueryHelper {

    protected QueryParserHelper() {}

    /**
     *
     * @param array primary list of path in the future PathQuery
     * @param adapter VarNameAdapter
     * @return a PathQuery
     */
    public static final PathQuery path(final JsonNode array, final VarNameAdapter adapter) {
        return new PathQuery(QUERY.PATH, array, adapter);
    }

    /**
     *
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
     *
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
     *
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
     *
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
     *
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
     *
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
     *
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
     *
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
     *
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
     *
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
     *
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
     *
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
     *
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
     *
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
     *
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
     *
     * @param command JsonNode
     * @param adapter VarNameAdapter
     * @return a MatchQuery using PREFIX operator
     * @throws InvalidParseOperationException if could not parse to JSON
     */
    public static final MatchQuery prefix(final JsonNode command, final VarNameAdapter adapter)
        throws InvalidParseOperationException {
        return new MatchQuery(QUERY.PREFIX, command, adapter);
    }

    /**
     *
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
     *
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
     *
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
     *
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
     *
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
     *
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
     *
     * @param command JsonNode
     * @param adapter VarNameAdapter
     * @return a RangeQuery
     * @throws InvalidParseOperationException if could not parse to JSON
     */
    public static final RangeQuery range(final JsonNode command, final VarNameAdapter adapter)
        throws InvalidParseOperationException {
        return new RangeQuery(QUERY.RANGE, command, adapter);
    }
}
