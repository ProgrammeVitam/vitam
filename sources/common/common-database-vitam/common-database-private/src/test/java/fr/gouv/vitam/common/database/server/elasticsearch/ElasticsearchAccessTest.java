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
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.CardinalityAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.CardinalityAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.SumAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.SumAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.TermsAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.ValueCountAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.ValueCountAggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.ResponseBody;
import co.elastic.clients.elasticsearch.indices.GetAliasResponse;
import co.elastic.clients.util.NamedValue;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.server.mongodb.CollectionSample;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchTestHelper;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchUtil.getFieldSorts;
import static fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchUtil.matchAll;
import static fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchUtil.matchQuery;
import static fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchUtil.rangeInclusiveQuery;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ElasticsearchAccessTest {

    @Rule
    public ElasticsearchRule elasticsearchRule = new ElasticsearchRule();

    private ElasticsearchAccess elasticsearchAccess;
    private final ElasticsearchIndexAlias myalias = ElasticsearchIndexAlias.ofFullIndexName("myalias");

    @Before
    public void initialize() throws VitamException {
        elasticsearchAccess = new ElasticsearchAccess(
            ElasticsearchRule.VITAM_CLUSTER,
            singletonList(new ElasticsearchNode(ElasticsearchRule.getHost(), ElasticsearchRule.getPort()))
        );
    }

    @After
    public void cleanup() {
        try {
            elasticsearchAccess.deleteIndexByAliasForTesting(myalias);
        } catch (Exception ignored) {}
        elasticsearchAccess.close();
    }

    @Test
    public void testElasticsearchAccess() {
        assertEquals(ElasticsearchRule.VITAM_CLUSTER, elasticsearchAccess.getClusterName());
        assertEquals(ElasticsearchRule.VITAM_CLUSTER, elasticsearchAccess.getInfo());
        assertNotNull(elasticsearchAccess.getClient());

        assertThat(elasticsearchAccess.checkConnection()).isTrue();
        elasticsearchAccess.close();
    }

    @Test
    public void testGetAliasForExistingAlias() throws Exception {
        elasticsearchRule.createIndex("myalias", "myindex", "{}");
        GetAliasResponse getAliasesResponse = elasticsearchAccess.getAlias(myalias);
        assertThat(getAliasesResponse.aliases()).containsOnlyKeys("myindex");
    }

    @Test
    public void testGetAliasForNonExistingAlias() throws IOException {
        assertThatThrownBy(() -> elasticsearchAccess.getAlias(myalias))
            .isInstanceOf(ElasticsearchException.class)
            .satisfies(e -> assertThat(((ElasticsearchException) e).status()).isEqualTo(404));
    }

    @Test
    public void testCreateIndexWithoutAlias() throws Exception {
        assertThat(elasticsearchAccess.existsIndex(myalias)).isFalse();

        ElasticsearchIndexAlias indexWithoutAlias = elasticsearchAccess.createIndexWithoutAlias(
            myalias,
            new ElasticsearchIndexSettings(2, 1, () -> "{}"),
            ElasticsearchTestHelper.loadElasticSearchSettings()
        );

        assertThat(elasticsearchAccess.existsAlias(myalias)).isFalse();
        assertThat(elasticsearchAccess.existsIndex(indexWithoutAlias)).isTrue();

        elasticsearchAccess.deleteIndexForTesting(indexWithoutAlias);
        assertThat(elasticsearchAccess.existsIndex(indexWithoutAlias)).isFalse();
    }

    @Test
    public void testIndexEntry() throws Exception {
        String id = GUIDFactory.newGUID().getId();
        CollectionSample document = new CollectionSample(
            JsonHandler.createObjectNode().put("_id", id).put("Identifier", "value")
        );

        elasticsearchAccess.createIndexAndAliasIfAliasNotExists(
            myalias,
            new ElasticsearchIndexSettings(1, 0, () -> "{}"),
            ElasticsearchTestHelper.loadElasticSearchSettings()
        );

        elasticsearchAccess.indexEntry(myalias, id, document);

        ResponseBody<ObjectNode> searchResponseAfter = elasticsearchAccess.search(
            myalias,
            matchAll(),
            null,
            null,
            0,
            100,
            null
        );
        assertThat(searchResponseAfter.hits().total()).isNotNull();
        assertThat(searchResponseAfter.hits().total().value()).isEqualTo(1L);
        assertThat(searchResponseAfter.hits().hits().get(0).id()).isEqualTo(id);
        assertThat(searchResponseAfter.hits().hits().get(0).source().fieldNames())
            .toIterable()
            .containsOnly("Identifier");
    }

    @Test
    public void testIndexEntryWithFacets() throws Exception {
        Map<String, VitamDocument<?>> documents = insertDataSet();

        // Define facets
        Map<String, Aggregation> facets = new HashMap<>();

        facets.put(
            "FacetOriginatingAgencyTerms",
            new TermsAggregation.Builder()
                .field("OriginatingAgency")
                .order(NamedValue.of("_key", SortOrder.Asc))
                .size(10)
                .build()
                ._toAggregation()
        );
        facets.put("FacetSizeSum", new SumAggregation.Builder().field("Size").build()._toAggregation());
        facets.put(
            "FacetOriginatingAgencyCount",
            new ValueCountAggregation.Builder().field("OriginatingAgency").build()._toAggregation()
        );
        facets.put(
            "FacetOriginatingAgencyCardinality",
            new CardinalityAggregation.Builder().field("OriginatingAgency").build()._toAggregation()
        );

        ResponseBody<ObjectNode> searchResponseAfter = elasticsearchAccess.search(
            myalias,
            matchAll(),
            null,
            null,
            0,
            100,
            facets
        );
        assertThat(searchResponseAfter.hits().total()).isNotNull();
        assertThat(searchResponseAfter.hits().total().value()).isEqualTo(documents.size());

        List<Hit<ObjectNode>> hits = searchResponseAfter.hits().hits();
        assertThat(hits.stream().map(Hit::id)).containsExactlyInAnyOrderElementsOf(documents.keySet());

        for (Hit<ObjectNode> hit : hits) {
            assertThat(hit.source().get("Identifier").asText()).isEqualTo("value" + hit.id());
        }
        assertThat(searchResponseAfter.aggregations()).isNotNull();
        assertThat(searchResponseAfter.aggregations().size()).isEqualTo(4);
        Map<String, Aggregate> aggregations = searchResponseAfter.aggregations();
        SumAggregate facetSizeSum = aggregations.get("FacetSizeSum").sum();
        StringTermsAggregate stringTermsAggregate = aggregations.get("FacetOriginatingAgencyTerms").sterms();
        ValueCountAggregate facetOriginatingAgencyCount = aggregations.get("FacetOriginatingAgencyCount").valueCount();
        CardinalityAggregate facetOriginatingAgencyCardinality = aggregations
            .get("FacetOriginatingAgencyCardinality")
            .cardinality();

        assertThat(facetSizeSum.value() != null ? facetSizeSum.value() : 0.0).isEqualTo(IntStream.range(0, 15).sum());
        assertThat(facetOriginatingAgencyCount.value() != null ? facetOriginatingAgencyCount.value() : 0.0).isEqualTo(
            12
        ); //12 non null value
        assertThat(facetOriginatingAgencyCardinality.value()).isEqualTo(3); // 3 non null different values
        assertThat(stringTermsAggregate.buckets()).isNotNull();
        assertThat(stringTermsAggregate.buckets().array().get(0).key()._toJsonString()).isEqualTo("Agency1");
        assertThat(stringTermsAggregate.buckets().array().get(0).docCount()).isEqualTo(7);
    }

    @Test
    public void testIndexEntries() throws Exception {
        Map<String, VitamDocument<?>> documents = insertDataSet();

        ResponseBody<ObjectNode> searchResponseAfter = elasticsearchAccess.search(
            myalias,
            matchAll(),
            null,
            null,
            0,
            100,
            null
        );
        assertThat(searchResponseAfter.hits().total()).isNotNull();
        assertThat(searchResponseAfter.hits().total().value()).isEqualTo(documents.size());

        List<Hit<ObjectNode>> hits = searchResponseAfter.hits().hits();
        assertThat(hits.stream().map(Hit::id)).containsExactlyInAnyOrderElementsOf(documents.keySet());

        for (Hit<ObjectNode> hit : hits) {
            assertThat(hit.source().fieldNames()).toIterable().contains("Identifier", "Name", "_tenant", "Size");
            assertThat(hit.source().get("Identifier").asText()).isEqualTo("value" + hit.id());
        }
    }

    @Test
    public void updateIndexEntry() throws Exception {
        Map<String, VitamDocument<?>> documents = insertDataSet();

        // When
        String updatedDocumentId = documents
            .keySet()
            .stream()
            .skip(RandomUtils.nextInt(0, documents.size()))
            .findFirst()
            .orElseThrow();
        CollectionSample newDocument = new CollectionSample(
            JsonHandler.createObjectNode().put("_id", updatedDocumentId).put("newKey", "newValue" + updatedDocumentId)
        );
        elasticsearchAccess.updateEntry(myalias, updatedDocumentId, newDocument);

        ResponseBody<ObjectNode> searchResponseAfter = elasticsearchAccess.search(
            myalias,
            matchAll(),
            null,
            null,
            0,
            100,
            null
        );
        assertThat(searchResponseAfter.hits().total()).isNotNull();
        assertThat(searchResponseAfter.hits().total().value()).isEqualTo(documents.size());

        List<Hit<ObjectNode>> hits = searchResponseAfter.hits().hits();
        assertThat(hits.stream().map(Hit::id)).containsExactlyInAnyOrderElementsOf(documents.keySet());

        for (Hit<ObjectNode> hit : hits) {
            if (hit.id().equals(updatedDocumentId)) {
                // Updated doc
                assertThat(hit.source().fieldNames()).toIterable().containsOnly("newKey");
                assertThat(hit.source().get("newKey").asText()).isEqualTo("newValue" + hit.id());
            } else {
                // Non updated documents
                assertThat(hit.source().fieldNames()).toIterable().contains("Identifier", "Name", "_tenant", "Size");
                assertThat(hit.source().get("Identifier").asText()).isEqualTo("value" + hit.id());
            }
        }
    }

    @Test
    public void testSearch() throws Exception {
        insertDataSet();

        // When
        ResponseBody<ObjectNode> searchResponse = elasticsearchAccess.search(
            myalias,
            rangeInclusiveQuery("_tenant", 9, 11),
            null,
            null,
            0,
            100,
            null
        );

        assertThat(searchResponse.hits().total()).isNotNull();
        assertThat(searchResponse.hits().total().value()).isEqualTo(3);

        List<Hit<ObjectNode>> hits = searchResponse.hits().hits();
        assertThat(hits.stream().map(i -> i.source().get("_tenant").asInt())).containsExactlyInAnyOrder(9, 10, 11);
    }

    @Test
    public void testSearchWithScrollingAndSorting() throws Exception {
        Map<String, VitamDocument<?>> documents = insertDataSet();

        // When
        List<SortOptions> sorts = singletonList(getFieldSorts("_tenant", SortOrder.Desc));
        Query query = matchQuery("Name", "Lorem ipsum");

        ResponseBody<ObjectNode> searchResponse1 = elasticsearchAccess.search(
            myalias,
            query,
            null,
            sorts,
            0,
            10,
            null,
            "START",
            5000,
            false
        );

        assertThat(searchResponse1.hits().total()).isNotNull();
        assertThat(searchResponse1.hits().total().value()).isEqualTo(documents.size());
        String scrollId1 = searchResponse1.scrollId();
        assertThat(scrollId1).isNotNull();
        List<Hit<ObjectNode>> hits1 = searchResponse1.hits().hits();
        assertThat(hits1.stream().map(i -> i.source().get("_tenant").asInt())).containsExactly(
            14,
            13,
            12,
            11,
            10,
            9,
            8,
            7,
            6,
            5
        );

        ResponseBody<ObjectNode> searchResponse2 = elasticsearchAccess.search(
            myalias,
            query,
            null,
            sorts,
            0,
            10,
            null,
            scrollId1,
            5000,
            false
        );

        assertThat(searchResponse2.hits().total()).isNotNull();
        assertThat(searchResponse2.hits().total().value()).isEqualTo(documents.size());
        String scrollId2 = searchResponse2.scrollId();
        assertThat(scrollId2).isEqualTo(scrollId1);
        List<Hit<ObjectNode>> hits2 = searchResponse2.hits().hits();
        assertThat(hits2.stream().map(i -> i.source().get("_tenant").asInt())).containsExactly(4, 3, 2, 1, 0);

        elasticsearchAccess.clearScroll(scrollId2);
    }

    @Test
    public void testDeleteEntry() throws Exception {
        Map<String, VitamDocument<?>> documents = insertDataSet();

        // When
        String deletedDocumentId = documents
            .keySet()
            .stream()
            .skip(RandomUtils.nextInt(0, documents.size()))
            .findFirst()
            .orElseThrow();
        elasticsearchAccess.delete(myalias, singletonList(deletedDocumentId));

        ResponseBody<ObjectNode> searchResponseAfter = elasticsearchAccess.search(
            myalias,
            QueryBuilders.matchAll().build()._toQuery(),
            null,
            null,
            0,
            100,
            null
        );
        assertThat(searchResponseAfter.hits().total()).isNotNull();
        assertThat(searchResponseAfter.hits().total().value()).isEqualTo(documents.size() - 1);

        List<Hit<ObjectNode>> hits = searchResponseAfter.hits().hits();
        assertThat(hits.stream().map(Hit::id)).containsExactlyInAnyOrderElementsOf(
            SetUtils.difference(documents.keySet(), singleton(deletedDocumentId))
        );
    }

    @Test
    public void testCheckConnectionValidNode() throws VitamException {
        ElasticsearchAccess validElasticsearchAccess = new ElasticsearchAccess(
            ElasticsearchRule.VITAM_CLUSTER,
            singletonList(new ElasticsearchNode(ElasticsearchRule.getHost(), ElasticsearchRule.getPort()))
        );
        assertThat(validElasticsearchAccess.checkConnection()).isTrue();
        validElasticsearchAccess.close();
    }

    @Test
    public void testCheckConnectionInvalidNode() throws VitamException {
        ElasticsearchAccess invalidElasticsearchAccess = new ElasticsearchAccess(
            ElasticsearchRule.VITAM_CLUSTER,
            singletonList(new ElasticsearchNode(ElasticsearchRule.getHost(), ElasticsearchRule.getPort() - 1))
        );
        assertThat(invalidElasticsearchAccess.checkConnection()).isFalse();
        invalidElasticsearchAccess.close();
    }

    private Map<String, VitamDocument<?>> insertDataSet() throws IOException, DatabaseException {
        Map<String, VitamDocument<?>> documents = new HashMap<>();

        for (int i = 0; i < 15; i++) {
            String id = GUIDFactory.newGUID().getId();
            String agencyValue = null; //3 values
            if (i < 7) {
                agencyValue = "Agency1"; //7 values
            }
            if (i >= 7 && i < 10) {
                agencyValue = "Agency2"; //3 values
            }
            if (i >= 10 && i < 12) {
                agencyValue = "Agency3"; //2 values
            }

            documents.put(
                id,
                new CollectionSample(
                    JsonHandler.createObjectNode()
                        .put("_id", id)
                        .put("Identifier", "value" + id)
                        .put("Name", "Lorem ipsum dolor sit amet, consectetur adipiscing elit")
                        .put("_tenant", i)
                        .put("Size", i)
                        .put("OriginatingAgency", agencyValue)
                )
            );
        }
        String mapping = FileUtils.readFileToString(
            PropertiesUtils.findFile("test-es-mapping.json"),
            StandardCharsets.UTF_8
        );

        elasticsearchAccess.createIndexAndAliasIfAliasNotExists(
            myalias,
            new ElasticsearchIndexSettings(2, 1, () -> mapping),
            ElasticsearchTestHelper.loadElasticSearchSettings()
        );

        elasticsearchAccess.indexEntries(myalias, documents.values(), true);

        return documents;
    }

    @Test
    public void testCreateIndexAndAliasIfAliasNotExistsNonExistingIndex() throws Exception {
        // Given
        assertThat(elasticsearchAccess.existsAlias(myalias)).isFalse();

        // When
        elasticsearchAccess.createIndexAndAliasIfAliasNotExists(
            myalias,
            new ElasticsearchIndexSettings(2, 1, () -> "{}"),
            ElasticsearchTestHelper.loadElasticSearchSettings()
        );

        // Then
        assertThat(elasticsearchAccess.existsAlias(myalias)).isTrue();

        GetAliasResponse getAliasesResponse = elasticsearchAccess.getAlias(myalias);
        assertThat(getAliasesResponse.aliases()).hasSize(1);
        ElasticsearchIndexAlias myindex = ElasticsearchIndexAlias.ofFullIndexName(
            getAliasesResponse.aliases().keySet().iterator().next()
        );
        assertThat(myalias.isValidAliasOfIndex(myindex)).isTrue();
    }

    @Test
    public void testCreateIndexAndAliasIfAliasNotExistsExistingIndex() throws Exception {
        // Given
        elasticsearchAccess.createIndexAndAliasIfAliasNotExists(
            myalias,
            new ElasticsearchIndexSettings(2, 1, () -> "{}"),
            ElasticsearchTestHelper.loadElasticSearchSettings()
        );
        waitCycle();
        assertThat(elasticsearchAccess.existsAlias(myalias)).isTrue();
        GetAliasResponse existingAliasResponse = elasticsearchAccess.getAlias(myalias);
        assertThat(existingAliasResponse.aliases()).hasSize(1);
        ElasticsearchIndexAlias myExistingIndex = ElasticsearchIndexAlias.ofFullIndexName(
            existingAliasResponse.aliases().keySet().iterator().next()
        );

        // When
        elasticsearchAccess.createIndexAndAliasIfAliasNotExists(
            myalias,
            new ElasticsearchIndexSettings(2, 1, () -> "{}"),
            ElasticsearchTestHelper.loadElasticSearchSettings()
        );

        // Then
        assertThat(elasticsearchAccess.existsAlias(myalias)).isTrue();

        GetAliasResponse getAliasesResponse = elasticsearchAccess.getAlias(myalias);
        assertThat(getAliasesResponse.aliases()).hasSize(1);
        assertThat(getAliasesResponse.aliases().keySet().iterator().next()).isEqualTo(myExistingIndex.getName());
    }

    @Test
    public void testSwitchIndex() throws Exception {
        // Given
        elasticsearchAccess.createIndexAndAliasIfAliasNotExists(
            myalias,
            new ElasticsearchIndexSettings(2, 1, () -> "{}"),
            ElasticsearchTestHelper.loadElasticSearchSettings()
        );
        waitCycle();
        ElasticsearchIndexAlias newIndex = elasticsearchAccess.createIndexWithoutAlias(
            myalias,
            new ElasticsearchIndexSettings(2, 1, () -> "{}"),
            ElasticsearchTestHelper.loadElasticSearchSettings()
        );
        ElasticsearchIndexAlias existingIndex = ElasticsearchIndexAlias.ofFullIndexName(
            elasticsearchAccess.getAlias(myalias).aliases().keySet().iterator().next()
        );
        assertThat(newIndex.getName()).isNotEqualTo(existingIndex.getName());

        // When
        elasticsearchAccess.switchIndex(myalias, newIndex);

        // Then
        ElasticsearchIndexAlias indexAfterAliasSwitch = ElasticsearchIndexAlias.ofFullIndexName(
            elasticsearchAccess.getAlias(myalias).aliases().keySet().iterator().next()
        );
        assertThat(indexAfterAliasSwitch.getName()).isEqualTo(newIndex.getName());

        // Cleanup old index
        elasticsearchAccess.deleteIndexForTesting(existingIndex);
    }

    @Test
    public void testSwitchIndexNonExistingAlias() throws Exception {
        // Given
        ElasticsearchIndexAlias nonExistingAlias = ElasticsearchIndexAlias.ofFullIndexName("unknown");
        ElasticsearchIndexAlias newIndex = elasticsearchAccess.createIndexWithoutAlias(
            myalias,
            new ElasticsearchIndexSettings(2, 1, () -> "{}"),
            ElasticsearchTestHelper.loadElasticSearchSettings()
        );

        // When / Then
        assertThatThrownBy(() -> elasticsearchAccess.switchIndex(nonExistingAlias, newIndex)).isInstanceOf(
            DatabaseException.class
        );

        // Cleanup
        elasticsearchAccess.deleteIndexForTesting(newIndex);
    }

    @Test
    public void testSwitchIndexNonExistingIndex() throws Exception {
        // Given
        elasticsearchAccess.createIndexAndAliasIfAliasNotExists(
            myalias,
            new ElasticsearchIndexSettings(2, 1, () -> "{}"),
            ElasticsearchTestHelper.loadElasticSearchSettings()
        );
        ElasticsearchIndexAlias newIndex = ElasticsearchIndexAlias.ofFullIndexName("unknown");

        // When / Then
        assertThatThrownBy(() -> elasticsearchAccess.switchIndex(myalias, newIndex)).isInstanceOf(
            DatabaseException.class
        );
    }

    private void waitCycle() {
        try {
            // Wait 1 second to avoid index name collision
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Shutdown", e);
        }
    }
}
