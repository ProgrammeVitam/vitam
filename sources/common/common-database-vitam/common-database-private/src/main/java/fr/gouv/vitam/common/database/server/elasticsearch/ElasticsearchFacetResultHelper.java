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

import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.FiltersBucket;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.RangeBucket;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import fr.gouv.vitam.common.model.FacetBucket;
import fr.gouv.vitam.common.model.FacetResult;
import fr.gouv.vitam.common.model.SingleValueFacet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * ElasticsearchFacetResultHelper for mapping ES object to Vitam FacetResult
 */
public class ElasticsearchFacetResultHelper {

    /**
     * Transform an es Aggregation result to a FacetResult object
     *
     * @param aggregation es aggregation result
     * @return FacetResult
     */
    public static FacetResult transformFromEsAggregation(String name, Aggregate aggregation) {
        FacetResult facetResult;
        Aggregate.Kind aggType = aggregation._kind();
        switch (aggType) {
            case DateRange:
                facetResult = FacetResult.createBucketFacetResult(name, extractBucketDateRangeAggregation(aggregation));
                break;
            case Sterms:
                facetResult = FacetResult.createBucketFacetResult(
                    name,
                    extractBucketStringTermsAggregation(aggregation)
                );
                break;
            case Sum:
                facetResult = FacetResult.createSingleValueFacetResult(name, extractSumAggregation(aggregation));
                break;
            case Cardinality:
                facetResult = FacetResult.createSingleValueFacetResult(
                    name,
                    extractCardinalityAggregation(aggregation)
                );
                break;
            case ValueCount:
                facetResult = FacetResult.createSingleValueFacetResult(name, extractValueCountAggregation(aggregation));
                break;
            case Lterms:
                facetResult = FacetResult.createBucketFacetResult(name, extractBucketLongTermsAggregation(aggregation));
                break;
            case Filters:
                facetResult = FacetResult.createBucketFacetResult(name, extractBucketFiltersAggregation(aggregation));
                break;
            case Nested:
                facetResult = extractFacetResultNestedAggregation(aggregation);
                break;
            default:
                throw new IllegalStateException("Unexpected aggregate type " + aggType);
        }
        return facetResult;
    }

    /**
     * Transform es filters aggregation buckets to FacetBucket
     *
     * @param aggregation es aggregation
     * @return list of FacetBucket
     */
    private static List<FacetBucket> extractBucketFiltersAggregation(Aggregate aggregation) {
        Map<String, FiltersBucket> buckets = aggregation.filters().buckets().keyed();
        List<FacetBucket> facetBuckets = new ArrayList<>();
        buckets.forEach((key, bucket) -> facetBuckets.add(new FacetBucket(key, bucket.docCount())));
        return facetBuckets;
    }

    /**
     * Transform es String Terms aggregation buckets to FacetBucket
     *
     * @param aggregation es aggregation
     * @return list of FacetBucket
     */
    private static List<FacetBucket> extractBucketStringTermsAggregation(Aggregate aggregation) {
        List<StringTermsBucket> buckets = aggregation.sterms().buckets().array();
        List<FacetBucket> facetBuckets = new ArrayList<>();
        buckets.forEach(bucket -> facetBuckets.add(new FacetBucket(bucket.key().stringValue(), bucket.docCount())));
        return facetBuckets;
    }

    /**
     * Transform es Sum aggregation to SumFacet
     *
     * @param aggregation es aggregation
     * @return SumFacet
     */
    private static SingleValueFacet extractSumAggregation(Aggregate aggregation) {
        double sumValue = aggregation.sum().value() != null ? aggregation.sum().value() : 0.0;
        return new SingleValueFacet(sumValue);
    }

    /**
     * Transform es value_count aggregation to SingleValueFacet
     *
     * @param aggregation es aggregation
     * @return SingleValueFacet
     */
    private static SingleValueFacet extractValueCountAggregation(Aggregate aggregation) {
        double countValue = aggregation.valueCount().value() != null ? aggregation.valueCount().value() : 0.0;
        return new SingleValueFacet(countValue);
    }

    /**
     * Transform es cardinality aggregation to SingleValueFacet
     *
     * @param aggregation es aggregation
     * @return SingleValueFacet
     */
    private static SingleValueFacet extractCardinalityAggregation(Aggregate aggregation) {
        double value = aggregation.cardinality().value();
        return new SingleValueFacet(value);
    }

    /**
     * Transform es Long Terms aggregation buckets to FacetBucket
     *
     * @param aggregation es aggregation
     * @return list of FacetBucket
     */
    private static List<FacetBucket> extractBucketLongTermsAggregation(Aggregate aggregation) {
        List<LongTermsBucket> buckets = aggregation.lterms().buckets().array();
        List<FacetBucket> facetBuckets = new ArrayList<>();
        buckets.forEach(bucket -> {
            // Long values are returned as "key", other types (eg. boolean) are returned as "key_as_string"
            String value = bucket.keyAsString() != null ? bucket.keyAsString() : Long.toString(bucket.key());
            facetBuckets.add(new FacetBucket(value, bucket.docCount()));
        });
        return facetBuckets;
    }

    /**
     * Transform es terms aggregation buckets to FacetBucket
     *
     * @param aggregation es aggregation
     * @return list of FacetBucket
     */
    private static List<FacetBucket> extractBucketNestedAggregation(Aggregate aggregation) {
        Map<String, Aggregate> aggregations = aggregation.nested().aggregations();
        if (aggregations.isEmpty()) {
            return Collections.emptyList();
        } else {
            String key = aggregations.keySet().iterator().next();
            return transformFromEsAggregation(key, aggregations.get(key)).getBuckets();
        }
    }

    /**
     * Transform es terms aggregation buckets to FacetBucket
     *
     * @param aggregation es aggregation
     * @return list of FacetBucket
     */
    private static FacetResult extractFacetResultNestedAggregation(Aggregate aggregation) {
        Map<String, Aggregate> aggregations = aggregation.nested().aggregations();
        if (aggregations.isEmpty()) {
            return null;
        } else {
            String key = aggregations.keySet().iterator().next();
            return transformFromEsAggregation(key, aggregations.get(key));
        }
    }

    /**
     * Transform es range aggregation buckets to FacetBucket
     *
     * @param aggregation es aggregation
     * @return list of FacetBucket
     */
    private static List<FacetBucket> extractBucketDateRangeAggregation(Aggregate aggregation) {
        List<RangeBucket> buckets = aggregation.dateRange().buckets().array();
        List<FacetBucket> facetBuckets = new ArrayList<>();
        buckets.forEach(bucket -> facetBuckets.add(new FacetBucket(bucket.key(), bucket.docCount())));
        return facetBuckets;
    }
}
