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
package fr.gouv.vitam.common.database.server.elasticsearch;

import fr.gouv.vitam.common.model.FacetBucket;
import fr.gouv.vitam.common.model.FacetResult;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.bucket.filter.Filters;
import org.elasticsearch.search.aggregations.bucket.filter.FiltersAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.InternalNested;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.range.DateRangeAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.range.Range;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    public static FacetResult transformFromEsAggregation(Aggregation aggregation) {
        FacetResult facetResult = new FacetResult();
        facetResult.setName(aggregation.getName());
        String aggType = aggregation.getType();
        switch (aggType) {
            case DateRangeAggregationBuilder.NAME:
                facetResult.setBuckets(extractBucketRangeAggregation(aggregation));
                break;
            case StringTerms.NAME:
                facetResult.setBuckets(extractBucketTermsAggregation(aggregation));
                break;
            case FiltersAggregationBuilder.NAME:
                facetResult.setBuckets(extractBucketFiltersAggregation(aggregation));
                break;
            case NestedAggregationBuilder.NAME:
                facetResult.setBuckets(extractBucketNestedAggregation(aggregation));
                break;
            default:
                break;
        }
        return facetResult;
    }

    /**
     * Transform es filters aggregation buckets to FacetBucket
     *
     * @param aggregation es aggregation
     * @return list of FacetBucket
     */
    private static List<FacetBucket> extractBucketFiltersAggregation(Aggregation aggregation) {
        List<? extends Filters.Bucket> buckets = ((Filters) aggregation).getBuckets();
        List<FacetBucket> facetBuckets = new ArrayList<>();
        buckets.stream()
            .forEach(bucket -> facetBuckets.add(new FacetBucket(bucket.getKeyAsString(), bucket.getDocCount())));
        return facetBuckets;
    }

    /**
     * Transform es terms aggregation buckets to FacetBucket
     *
     * @param aggregation es aggregation
     * @return list of FacetBucket
     */
    private static List<FacetBucket> extractBucketTermsAggregation(Aggregation aggregation) {
        List<? extends Bucket> buckets = ((Terms) aggregation).getBuckets();
        List<FacetBucket> facetBuckets = new ArrayList<>();
        buckets.stream()
            .forEach(bucket -> facetBuckets.add(new FacetBucket(bucket.getKeyAsString(), bucket.getDocCount())));
        return facetBuckets;
    }

    /**
     * Transform es terms aggregation buckets to FacetBucket
     *
     * @param aggregation es aggregation
     * @return list of FacetBucket
     */
    private static List<FacetBucket> extractBucketNestedAggregation(Aggregation aggregation) {
        List<Aggregation> aggregations = ((InternalNested) aggregation).getAggregations().asList();
        if (aggregations.isEmpty()) {
            return Collections.emptyList();
        } else {
            Aggregation agg = aggregations.get(0);
            return transformFromEsAggregation(agg).getBuckets();
        }
    }

    /**
     * Transform es range aggregation buckets to FacetBucket
     *
     * @param aggregation es aggregation
     * @return list of FacetBucket
     */
    private static List<FacetBucket> extractBucketRangeAggregation(Aggregation aggregation) {
        List<? extends Range.Bucket> buckets = ((Range) aggregation).getBuckets();
        List<FacetBucket> facetBuckets = new ArrayList<>();
        buckets.stream()
            .forEach(bucket -> facetBuckets.add(new FacetBucket(bucket.getKeyAsString(), bucket.getDocCount())));
        return facetBuckets;
    }
}
