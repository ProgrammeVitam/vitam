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

package fr.gouv.vitam.common.database.server.elasticsearch;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.json.JsonData;
import fr.gouv.vitam.common.exception.DatabaseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Elasticsearch Util
 */
public class ElasticsearchUtil {

    public static Query matchAll() {
        return QueryBuilders.matchAll().build()._toQuery();
    }

    public static Query termQuery(String key, int value) {
        return QueryBuilders.term(t -> t.field(key).value(value));
    }

    public static Query termQuery(String key, long value) {
        return QueryBuilders.term(t -> t.field(key).value(value));
    }

    public static Query termQuery(String key, boolean value) {
        return QueryBuilders.term(t -> t.field(key).value(value));
    }

    public static Query termQuery(String key, String value) {
        return QueryBuilders.term(t -> t.field(key).value(value));
    }

    public static Query termsQuery(String key, String... values) {
        return QueryBuilders.terms(
            builder ->
                builder
                    .field(key)
                    .terms(v -> v.value(Arrays.stream(values).map(FieldValue::of).collect(Collectors.toList())))
        );
    }

    public static Query termsQuery(String key, Collection<String> values) {
        return QueryBuilders.terms(
            builder ->
                builder.field(key).terms(v -> v.value(values.stream().map(FieldValue::of).collect(Collectors.toList())))
        );
    }

    public static Query lteQuery(String key, Object value) {
        return QueryBuilders.range(r -> r.untyped(u -> u.field(key).lte(JsonData.of(value))));
    }

    public static Query ltQuery(String key, Object value) {
        return QueryBuilders.range(r -> r.untyped(u -> u.field(key).lt(JsonData.of(value))));
    }

    public static Query gteQuery(String key, Object value) {
        return QueryBuilders.range(r -> r.untyped(u -> u.field(key).gte(JsonData.of(value))));
    }

    public static Query gtQuery(String key, Object value) {
        return QueryBuilders.range(r -> r.untyped(u -> u.field(key).gt(JsonData.of(value))));
    }

    public static Query rangeInclusiveQuery(String key, int minInclusive, int maxInclusive) {
        return QueryBuilders.range(
            r -> r.untyped(u -> u.field(key).gte(JsonData.of(minInclusive)).lte(JsonData.of(maxInclusive)))
        );
    }

    /**
     * Acts like an "AND" operator: All clauses must match
     */
    public static Query boolMust(Query... queries) {
        BoolQuery.Builder bool = QueryBuilders.bool();
        for (Query query : queries) {
            bool.must(query);
        }
        return bool.build()._toQuery();
    }

    /**
     * Acts as an "OR" operator: At least one clause must match.
     */
    public static Query boolShould(Query... queries) {
        BoolQuery.Builder bool = QueryBuilders.bool();
        for (Query query : queries) {
            bool.should(query);
        }
        return bool.build()._toQuery();
    }

    /**
     * Acts as an "OR" operator: At least one clause must match.
     */
    public static Query boolShould(Collection<Query> queries) {
        BoolQuery.Builder bool = QueryBuilders.bool();
        for (Query query : queries) {
            bool.should(query);
        }
        return bool.build()._toQuery();
    }

    public static Query mustNot(Query query) {
        return QueryBuilders.bool(b -> b.mustNot(query));
    }

    public static Query exists(String key) {
        return QueryBuilders.exists(e -> e.field(key));
    }

    public static Query regex(String key, String value) {
        return QueryBuilders.regexp(r -> r.field(key).value(value));
    }

    public static Query wildcard(String key, String val) {
        return QueryBuilders.wildcard(i -> i.field(key).wildcard(val));
    }

    public static Query simpleQueryString(String key, String value) {
        return QueryBuilders.simpleQueryString(i -> i.fields(key).query(value));
    }

    public static Query matchQuery(String key, String query) {
        return QueryBuilders.match(m -> m.field(key).query(query).operator(Operator.Or));
    }

    public static Query matchQuery(String key, String query, int maxExpansions) {
        return QueryBuilders.match(m -> m.field(key).query(query).maxExpansions(maxExpansions).operator(Operator.Or));
    }

    public static Query matchAllQuery(String key, String query) {
        return QueryBuilders.match(m -> m.field(key).query(query).operator(Operator.And));
    }

    public static Query matchAllQuery(String key, String query, int maxExpansions) {
        return QueryBuilders.match(m -> m.field(key).query(query).maxExpansions(maxExpansions).operator(Operator.And));
    }

    public static Query matchPhraseQuery(String key, String query) {
        return QueryBuilders.matchPhrase(m -> m.field(key).query(query));
    }

    public static Query matchPhrasePrefixQuery(String key, String query) {
        return QueryBuilders.matchPhrasePrefix(m -> m.field(key).query(query));
    }

    public static Query matchPhrasePrefixQuery(String key, String query, int maxExpansions) {
        return QueryBuilders.matchPhrasePrefix(m -> m.field(key).query(query).maxExpansions(maxExpansions));
    }

    public static Query nestedQuery(String path, Query command) {
        return QueryBuilders.nested().path(path).query(command).scoreMode(ChildScoreMode.Avg).build()._toQuery();
    }

    public static SortOptions getScoreSort(SortOrder order) {
        return SortOptions.of(so -> so.score(s -> s.order(order)));
    }

    public static SortOptions getFieldSorts(String key, SortOrder order) {
        return SortOptions.of(so -> so.field(f -> f.field(key).order(order)));
    }

    public static SortOptions getDocSorts(SortOrder order) {
        return SortOptions.of(so -> so.doc(d -> d.order(order)));
    }

    /**
     * @param is InputStream of the file json
     * @return String mapping
     * @throws IOException
     */
    public static String transferJsonToMapping(InputStream is) throws IOException {
        final String mapping;
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(is))) {
            mapping = buffer.lines().collect(Collectors.joining("\n"));
        }
        return mapping;
    }

    public static Time timeOfMilliseconds(int duration) {
        return new Time.Builder().time(duration + "ms").build();
    }

    public static String buildFailureMessage(BulkResponse response) {
        StringBuilder sb = new StringBuilder();
        sb.append("Bulk execution failed: ");

        for (int i = 0; i < response.items().size(); i++) {
            var item = response.items().get(i);
            if (item.error() != null) {
                sb
                    .append("\nat ")
                    .append(i)
                    .append("[")
                    .append(item.index())
                    .append("] - ")
                    .append(item.operationType())
                    .append(" - ")
                    .append(item.id())
                    .append(" - ")
                    .append(item.error());
            }
        }

        return sb.toString();
    }

    public static DatabaseException toDatabaseException(String msg, Exception e) {
        if (e instanceof ElasticsearchException) {
            return new DatabaseException(msg + " - " + ((ElasticsearchException) e).response(), e);
        }
        return new DatabaseException(msg, e);
    }

    public static DatabaseException toDatabaseException(Exception e) {
        if (e instanceof ElasticsearchException) {
            return new DatabaseException(e.getMessage() + " - " + ((ElasticsearchException) e).response(), e);
        }
        return new DatabaseException(e);
    }
}
