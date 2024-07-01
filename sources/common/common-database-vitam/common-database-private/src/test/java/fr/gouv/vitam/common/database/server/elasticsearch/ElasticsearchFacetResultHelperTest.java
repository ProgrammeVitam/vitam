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

import fr.gouv.vitam.common.model.FacetResult;
import org.elasticsearch.search.aggregations.bucket.range.DateRangeAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.range.Range;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * ElasticsearchFacetResultHelperTest.
 */
public class ElasticsearchFacetResultHelperTest {

    @Test
    public void should_return_valid_result_when_valid_terms_aggregation() {
        // given
        Terms terms = Mockito.mock(Terms.class);
        Mockito.when(terms.getName()).thenReturn("Name");
        Mockito.when(terms.getType()).thenReturn(StringTerms.NAME);

        Bucket bucket1 = mock(StringTerms.Bucket.class);
        Mockito.when(bucket1.getKeyAsString()).thenReturn("Value1");
        Mockito.when(bucket1.getDocCount()).thenReturn(2L);
        Bucket bucket2 = mock(StringTerms.Bucket.class);
        Mockito.when(bucket2.getKeyAsString()).thenReturn("Value2");
        Mockito.when(bucket2.getDocCount()).thenReturn(1L);
        List bucketList = new ArrayList();
        bucketList.add(bucket1);
        bucketList.add(bucket2);
        Mockito.when(terms.getBuckets()).thenReturn(bucketList);

        // when
        FacetResult facet = ElasticsearchFacetResultHelper.transformFromEsAggregation(terms);

        // then
        assertThat(facet.getName()).isEqualTo("Name");
        assertThat(facet.getBuckets()).hasSize(2);
        assertThat(facet.getBuckets().get(0).getValue()).isEqualTo("Value1");
        assertThat(facet.getBuckets().get(0).getCount()).isEqualTo(2L);
        assertThat(facet.getBuckets().get(1).getValue()).isEqualTo("Value2");
        assertThat(facet.getBuckets().get(1).getCount()).isEqualTo(1L);
    }

    @Test
    public void should_return_valid_result_when_valid_date_range_aggregation() {
        // given
        Range range = Mockito.mock(Range.class);
        Mockito.when(range.getName()).thenReturn("EndDate");
        Mockito.when(range.getType()).thenReturn(DateRangeAggregationBuilder.NAME);

        Range.Bucket bucket1 = mock(Range.Bucket.class);
        Mockito.when(bucket1.getKeyAsString()).thenReturn("2000-2010");
        Mockito.when(bucket1.getDocCount()).thenReturn(2L);
        Range.Bucket bucket2 = mock(Range.Bucket.class);
        Mockito.when(bucket2.getKeyAsString()).thenReturn("1800");
        Mockito.when(bucket2.getDocCount()).thenReturn(1L);
        List bucketList = new ArrayList();
        bucketList.add(bucket1);
        bucketList.add(bucket2);
        Mockito.when(range.getBuckets()).thenReturn(bucketList);

        // when
        FacetResult facet = ElasticsearchFacetResultHelper.transformFromEsAggregation(range);

        // then
        assertThat(facet.getName()).isEqualTo("EndDate");
        assertThat(facet.getBuckets()).hasSize(2);
        assertThat(facet.getBuckets().get(0).getValue()).isEqualTo("2000-2010");
        assertThat(facet.getBuckets().get(0).getCount()).isEqualTo(2L);
        assertThat(facet.getBuckets().get(1).getValue()).isEqualTo("1800");
        assertThat(facet.getBuckets().get(1).getCount()).isEqualTo(1L);
    }

    @Test
    public void should_return_valid_result_when_empty_buckets_terms_aggregation() {
        // given
        Terms terms = Mockito.mock(Terms.class);
        Mockito.when(terms.getName()).thenReturn("Name");
        Mockito.when(terms.getType()).thenReturn(StringTerms.NAME);

        List bucketList = new ArrayList();
        Mockito.when(terms.getBuckets()).thenReturn(bucketList);

        // when
        FacetResult facet = ElasticsearchFacetResultHelper.transformFromEsAggregation(terms);

        // then
        assertThat(facet.getName()).isEqualTo("Name");
        assertThat(facet.getBuckets()).hasSize(0);
    }
}
