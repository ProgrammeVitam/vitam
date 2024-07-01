/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.database.builder.query;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.QUERY;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;

import java.util.Date;
import java.util.Map;

/**
 * Query helper (common to all types: SELECT, UPDATE, INSET, DELETE)
 */
public class QueryHelper {

    protected QueryHelper() {
        // empty
    }

    /**
     * @param pathes primary list of path in the future PathQuery
     * @return a PathQuery
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final PathQuery path(final String... pathes) throws InvalidCreateOperationException {
        return new PathQuery(pathes);
    }

    /**
     * @return a BooleanQuery for AND operator
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final BooleanQuery and() throws InvalidCreateOperationException {
        return new BooleanQuery(QUERY.AND);
    }

    /**
     * @return a BooleanQuery for OR operator
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final BooleanQuery or() throws InvalidCreateOperationException {
        return new BooleanQuery(QUERY.OR);
    }

    /**
     * @return a BooleanQuery for NOT operator (using AND internally)
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final BooleanQuery not() throws InvalidCreateOperationException {
        return new BooleanQuery(QUERY.NOT);
    }

    /**
     * @param variableName key name
     * @param value of key
     * @return a CompareQuery using EQ comparator
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final CompareQuery eq(final String variableName, final boolean value)
        throws InvalidCreateOperationException {
        return new CompareQuery(QUERY.EQ, variableName, value);
    }

    /**
     * @param variableName key name
     * @param value of key
     * @return a CompareQuery using EQ comparator
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final CompareQuery eq(final String variableName, final long value)
        throws InvalidCreateOperationException {
        return new CompareQuery(QUERY.EQ, variableName, value);
    }

    /**
     * @param variableName key name
     * @param value of key
     * @return a CompareQuery using EQ comparator
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final CompareQuery eq(final String variableName, final double value)
        throws InvalidCreateOperationException {
        return new CompareQuery(QUERY.EQ, variableName, value);
    }

    /**
     * @param variableName key name
     * @param value of key
     * @return a CompareQuery using EQ comparator
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final CompareQuery eq(final String variableName, final String value)
        throws InvalidCreateOperationException {
        return new CompareQuery(QUERY.EQ, variableName, value);
    }

    /**
     * @param variableName key name
     * @param value of key
     * @return a CompareQuery using EQ comparator
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final CompareQuery eq(final String variableName, final Date value)
        throws InvalidCreateOperationException {
        return new CompareQuery(QUERY.EQ, variableName, value);
    }

    /**
     * @param variableName key name
     * @param value of key
     * @return a CompareQuery using NE (non equal) comparator
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final CompareQuery ne(final String variableName, final boolean value)
        throws InvalidCreateOperationException {
        return new CompareQuery(QUERY.NE, variableName, value);
    }

    /**
     * @param variableName key name
     * @param value of key
     * @return a CompareQuery using NE (non equal) comparator
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final CompareQuery ne(final String variableName, final long value)
        throws InvalidCreateOperationException {
        return new CompareQuery(QUERY.NE, variableName, value);
    }

    /**
     * @param variableName key name
     * @param value of key
     * @return a CompareQuery using NE (non equal) comparator
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final CompareQuery ne(final String variableName, final double value)
        throws InvalidCreateOperationException {
        return new CompareQuery(QUERY.NE, variableName, value);
    }

    /**
     * @param variableName key name
     * @param value of key
     * @return a CompareQuery using NE (non equal) comparator
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final CompareQuery ne(final String variableName, final String value)
        throws InvalidCreateOperationException {
        return new CompareQuery(QUERY.NE, variableName, value);
    }

    /**
     * @param variableName key name
     * @param value of key
     * @return a CompareQuery using NE (non equal) comparator
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final CompareQuery ne(final String variableName, final Date value)
        throws InvalidCreateOperationException {
        return new CompareQuery(QUERY.NE, variableName, value);
    }

    /**
     * @param variableName key name
     * @param value of key
     * @return a CompareQuery using LT (less than) comparator
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final CompareQuery lt(final String variableName, final boolean value)
        throws InvalidCreateOperationException {
        return new CompareQuery(QUERY.LT, variableName, value);
    }

    /**
     * @param variableName key name
     * @param value of key
     * @return a CompareQuery using LT (less than) comparator
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final CompareQuery lt(final String variableName, final long value)
        throws InvalidCreateOperationException {
        return new CompareQuery(QUERY.LT, variableName, value);
    }

    /**
     * @param variableName key name
     * @param value of key
     * @return a CompareQuery using LT (less than) comparator
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final CompareQuery lt(final String variableName, final double value)
        throws InvalidCreateOperationException {
        return new CompareQuery(QUERY.LT, variableName, value);
    }

    /**
     * @param variableName key name
     * @param value of key
     * @return a CompareQuery using LT (less than) comparator
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final CompareQuery lt(final String variableName, final String value)
        throws InvalidCreateOperationException {
        return new CompareQuery(QUERY.LT, variableName, value);
    }

    /**
     * @param variableName key name
     * @param value of key
     * @return a CompareQuery using LT (less than) comparator
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final CompareQuery lt(final String variableName, final Date value)
        throws InvalidCreateOperationException {
        return new CompareQuery(QUERY.LT, variableName, value);
    }

    /**
     * @param variableName key name
     * @param value of key
     * @return a CompareQuery using LTE (less than or equal) comparator
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final CompareQuery lte(final String variableName, final boolean value)
        throws InvalidCreateOperationException {
        return new CompareQuery(QUERY.LTE, variableName, value);
    }

    /**
     * @param variableName key name
     * @param value of key
     * @return a CompareQuery using LTE (less than or equal) comparator
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final CompareQuery lte(final String variableName, final long value)
        throws InvalidCreateOperationException {
        return new CompareQuery(QUERY.LTE, variableName, value);
    }

    /**
     * @param variableName key name
     * @param value of key
     * @return a CompareQuery using LTE (less than or equal) comparator
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final CompareQuery lte(final String variableName, final double value)
        throws InvalidCreateOperationException {
        return new CompareQuery(QUERY.LTE, variableName, value);
    }

    /**
     * @param variableName key name
     * @param value of key
     * @return a CompareQuery using LTE (less than or equal) comparator
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final CompareQuery lte(final String variableName, final String value)
        throws InvalidCreateOperationException {
        return new CompareQuery(QUERY.LTE, variableName, value);
    }

    /**
     * @param variableName key name
     * @param value of key
     * @return a CompareQuery using LTE (less than or equal) comparator
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final CompareQuery lte(final String variableName, final Date value)
        throws InvalidCreateOperationException {
        return new CompareQuery(QUERY.LTE, variableName, value);
    }

    /**
     * @param variableName key name
     * @param value of key
     * @return a CompareQuery using GT (greater than) comparator
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final CompareQuery gt(final String variableName, final boolean value)
        throws InvalidCreateOperationException {
        return new CompareQuery(QUERY.GT, variableName, value);
    }

    /**
     * @param variableName key name
     * @param value of key
     * @return a CompareQuery using GT (greater than) comparator
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final CompareQuery gt(final String variableName, final long value)
        throws InvalidCreateOperationException {
        return new CompareQuery(QUERY.GT, variableName, value);
    }

    /**
     * @param variableName key name
     * @param value of key
     * @return a CompareQuery using GT (greater than) comparator
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final CompareQuery gt(final String variableName, final double value)
        throws InvalidCreateOperationException {
        return new CompareQuery(QUERY.GT, variableName, value);
    }

    /**
     * @param variableName key name
     * @param value of key
     * @return a CompareQuery using GT (greater than) comparator
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final CompareQuery gt(final String variableName, final String value)
        throws InvalidCreateOperationException {
        return new CompareQuery(QUERY.GT, variableName, value);
    }

    /**
     * @param variableName key name
     * @param value of key
     * @return a CompareQuery using GT (greater than) comparator
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final CompareQuery gt(final String variableName, final Date value)
        throws InvalidCreateOperationException {
        return new CompareQuery(QUERY.GT, variableName, value);
    }

    /**
     * @param variableName key name
     * @param value of key
     * @return a CompareQuery using GTE (greater than or equal) comparator
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final CompareQuery gte(final String variableName, final boolean value)
        throws InvalidCreateOperationException {
        return new CompareQuery(QUERY.GTE, variableName, value);
    }

    /**
     * @param variableName key name
     * @param value of key
     * @return a CompareQuery using GTE (greater than or equal) comparator
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final CompareQuery gte(final String variableName, final long value)
        throws InvalidCreateOperationException {
        return new CompareQuery(QUERY.GTE, variableName, value);
    }

    /**
     * @param variableName key name
     * @param value of key
     * @return a CompareQuery using GTE (greater than or equal) comparator
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final CompareQuery gte(final String variableName, final double value)
        throws InvalidCreateOperationException {
        return new CompareQuery(QUERY.GTE, variableName, value);
    }

    /**
     * @param variableName key name
     * @param value of key
     * @return a CompareQuery using GTE (greater than or equal) comparator
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final CompareQuery gte(final String variableName, final String value)
        throws InvalidCreateOperationException {
        return new CompareQuery(QUERY.GTE, variableName, value);
    }

    /**
     * @param variableName key name
     * @param value of key
     * @return a CompareQuery using GTE (greater than or equal) comparator
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final CompareQuery gte(final String variableName, final Date value)
        throws InvalidCreateOperationException {
        return new CompareQuery(QUERY.GTE, variableName, value);
    }

    /**
     * @param variableName key name
     * @param value of key
     * @return a CompareQuery using SIZE comparator
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final CompareQuery size(final String variableName, final long value)
        throws InvalidCreateOperationException {
        return new CompareQuery(QUERY.SIZE, variableName, value);
    }

    /**
     * @param variableName key name
     * @return an ExistsQuery
     * @throws InvalidCreateOperationException using Exists operator
     */
    public static final ExistsQuery exists(final String variableName) throws InvalidCreateOperationException {
        return new ExistsQuery(QUERY.EXISTS, variableName);
    }

    /**
     * @param variableName key name
     * @return an ExistsQuery using Missing operator
     * @throws InvalidCreateOperationException when creating query errors
     * @deprecated Use $not + $exists
     */
    @Deprecated
    public static final ExistsQuery missing(final String variableName) throws InvalidCreateOperationException {
        return new ExistsQuery(QUERY.MISSING, variableName);
    }

    /**
     * @param variableName key name
     * @return an ExistsQuery using isNull operator
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final ExistsQuery isNull(final String variableName) throws InvalidCreateOperationException {
        return new ExistsQuery(QUERY.ISNULL, variableName);
    }

    /**
     * @param variableName key name
     * @param value of key
     * @return an InQuery using IN operator
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final InQuery in(final String variableName, final boolean... value)
        throws InvalidCreateOperationException {
        return new InQuery(QUERY.IN, variableName, value);
    }

    /**
     * @param variableName key name
     * @param value of key
     * @return an InQuery using IN operator
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final InQuery in(final String variableName, final long... value)
        throws InvalidCreateOperationException {
        return new InQuery(QUERY.IN, variableName, value);
    }

    /**
     * @param variableName key name
     * @param value of key
     * @return an InQuery using IN operator
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final InQuery in(final String variableName, final double... value)
        throws InvalidCreateOperationException {
        return new InQuery(QUERY.IN, variableName, value);
    }

    /**
     * @param variableName key name
     * @param value of key
     * @return an InQuery using IN operator
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final InQuery in(final String variableName, final String... value)
        throws InvalidCreateOperationException {
        return new InQuery(QUERY.IN, variableName, value);
    }

    /**
     * @param variableName key name
     * @param value of key
     * @return an InQuery using IN operator
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final InQuery in(final String variableName, final Date... value)
        throws InvalidCreateOperationException {
        return new InQuery(QUERY.IN, variableName, value);
    }

    /**
     * @param variableName key name
     * @param value of key
     * @return an InQuery using NIN (not in) operator
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final InQuery nin(final String variableName, final boolean... value)
        throws InvalidCreateOperationException {
        return new InQuery(QUERY.NIN, variableName, value);
    }

    /**
     * @param variableName key name
     * @param value of key
     * @return an InQuery using NIN (not in) operator
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final InQuery nin(final String variableName, final long... value)
        throws InvalidCreateOperationException {
        return new InQuery(QUERY.NIN, variableName, value);
    }

    /**
     * @param variableName key name
     * @param value of key
     * @return an InQuery using NIN (not in) operator
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final InQuery nin(final String variableName, final double... value)
        throws InvalidCreateOperationException {
        return new InQuery(QUERY.NIN, variableName, value);
    }

    /**
     * @param variableName key name
     * @param value of key
     * @return an InQuery using NIN (not in) operator
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final InQuery nin(final String variableName, final String... value)
        throws InvalidCreateOperationException {
        return new InQuery(QUERY.NIN, variableName, value);
    }

    /**
     * @param variableName key name
     * @param value of key
     * @return an InQuery using NIN (not in) operator
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final InQuery nin(final String variableName, final Date... value)
        throws InvalidCreateOperationException {
        return new InQuery(QUERY.NIN, variableName, value);
    }

    /**
     * @param variableName key name
     * @param value of key
     * @return a MatchQuery using MATCH operator
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final MatchQuery match(final String variableName, final String value)
        throws InvalidCreateOperationException {
        return new MatchQuery(QUERY.MATCH, variableName, value);
    }

    /**
     * @param variableName key name
     * @param value of key
     * @return a MatchQuery using MATCH operator
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final MatchQuery matchAll(final String variableName, final String value)
        throws InvalidCreateOperationException {
        return new MatchQuery(QUERY.MATCH_ALL, variableName, value);
    }

    /**
     * @param variableName key name
     * @param value of key
     * @return a MatchQuery using MATCH_PHRASE operator
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final MatchQuery matchPhrase(final String variableName, final String value)
        throws InvalidCreateOperationException {
        return new MatchQuery(QUERY.MATCH_PHRASE, variableName, value);
    }

    /**
     * @param variableName key name
     * @param value of key
     * @return a MatchQuery using MATCH_PHRASE_PREFIX operator
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final MatchQuery matchPhrasePrefix(final String variableName, final String value)
        throws InvalidCreateOperationException {
        return new MatchQuery(QUERY.MATCH_PHRASE_PREFIX, variableName, value);
    }

    /**
     * @param variableName key name
     * @param value of key
     * @return a SearchQuery using REGEX operator
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final SearchQuery regex(final String variableName, final String value)
        throws InvalidCreateOperationException {
        return new SearchQuery(QUERY.REGEX, variableName, value);
    }

    /**
     * @param variableName key name
     * @param value of key
     * @return a SearchQuery using SEARCH operator
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final SearchQuery search(final String variableName, final String value)
        throws InvalidCreateOperationException {
        return new SearchQuery(QUERY.SEARCH, variableName, value);
    }

    /**
     * @param variableName key name
     * @param value of key
     * @return a NestedQuery using Nested Search operator
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final NestedQuery nestedSearch(final String variableName, final JsonNode value)
        throws InvalidCreateOperationException {
        return new NestedQuery(QUERY.SUBOBJECT, variableName, value);
    }

    /**
     * @param variableName key name
     * @param value of key
     * @return a TermQuery
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final TermQuery term(final String variableName, final String value)
        throws InvalidCreateOperationException {
        return new TermQuery(variableName, value);
    }

    /**
     * @param variableName key name
     * @param value of key
     * @return a TermQuery
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final TermQuery term(final String variableName, final long value)
        throws InvalidCreateOperationException {
        return new TermQuery(variableName, value);
    }

    /**
     * @param variableName key name
     * @param value of key
     * @return a TermQuery
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final TermQuery term(final String variableName, final double value)
        throws InvalidCreateOperationException {
        return new TermQuery(variableName, value);
    }

    /**
     * @param variableName key name
     * @param value of key
     * @return a TermQuery
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final TermQuery term(final String variableName, final boolean value)
        throws InvalidCreateOperationException {
        return new TermQuery(variableName, value);
    }

    /**
     * @param variableName key name
     * @param value of key
     * @return a TermQuery
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final TermQuery term(final String variableName, final Date value)
        throws InvalidCreateOperationException {
        return new TermQuery(variableName, value);
    }

    /**
     * @param variableNameValue key nameValue Map of VariableName of Value
     * @return a TermQuery
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final TermQuery term(final Map<String, Object> variableNameValue)
        throws InvalidCreateOperationException {
        return new TermQuery(variableNameValue);
    }

    /**
     * @param variableName key name
     * @param value of key
     * @return a WildcardQuery
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final WildcardQuery wildcard(final String variableName, final String value)
        throws InvalidCreateOperationException {
        return new WildcardQuery(variableName, value);
    }

    /**
     * @param value of key
     * @param variableName key name
     * @return a MltQuery using a FLT (fuzzy like this) operator
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final MltQuery flt(final String value, final String... variableName)
        throws InvalidCreateOperationException {
        return new MltQuery(QUERY.FLT, value, variableName);
    }

    /**
     * @param value of key
     * @param variableName key name
     * @return a MltQuery using a MLT (more like this) operator
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final MltQuery mlt(final String value, final String... variableName)
        throws InvalidCreateOperationException {
        return new MltQuery(QUERY.MLT, value, variableName);
    }

    /**
     * @param variableName key name
     * @param min value
     * @param includeMin include min value
     * @param max value
     * @param includeMax include max value
     * @return a RangeQuery
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final RangeQuery range(
        final String variableName,
        final long min,
        final boolean includeMin,
        final long max,
        final boolean includeMax
    ) throws InvalidCreateOperationException {
        final QUERY rmin = includeMin ? QUERY.GTE : QUERY.GT;
        final QUERY rmax = includeMax ? QUERY.LTE : QUERY.LT;
        return new RangeQuery(variableName, rmin, min, rmax, max);
    }

    /**
     * @param variableName key name
     * @param min value
     * @param includeMin include min value
     * @param max value
     * @param includeMax include max value
     * @return a RangeQuery
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final RangeQuery range(
        final String variableName,
        final double min,
        final boolean includeMin,
        final double max,
        final boolean includeMax
    ) throws InvalidCreateOperationException {
        final QUERY rmin = includeMin ? QUERY.GTE : QUERY.GT;
        final QUERY rmax = includeMax ? QUERY.LTE : QUERY.LT;
        return new RangeQuery(variableName, rmin, min, rmax, max);
    }

    /**
     * @param variableName key name
     * @param min value
     * @param includeMin include min value
     * @param max value
     * @param includeMax include max value
     * @return a RangeQuery
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final RangeQuery range(
        final String variableName,
        final String min,
        final boolean includeMin,
        final String max,
        final boolean includeMax
    ) throws InvalidCreateOperationException {
        final QUERY rmin = includeMin ? QUERY.GTE : QUERY.GT;
        final QUERY rmax = includeMax ? QUERY.LTE : QUERY.LT;
        return new RangeQuery(variableName, rmin, min, rmax, max);
    }

    /**
     * @param variableName key name
     * @param min value
     * @param includeMin include min value
     * @param max value
     * @param includeMax include max value
     * @return a RangeQuery
     * @throws InvalidCreateOperationException when creating query errors
     */
    public static final RangeQuery range(
        final String variableName,
        final Date min,
        final boolean includeMin,
        final Date max,
        final boolean includeMax
    ) throws InvalidCreateOperationException {
        final QUERY rmin = includeMin ? QUERY.GTE : QUERY.GT;
        final QUERY rmax = includeMax ? QUERY.LTE : QUERY.LT;
        return new RangeQuery(variableName, rmin, min, rmax, max);
    }
}
