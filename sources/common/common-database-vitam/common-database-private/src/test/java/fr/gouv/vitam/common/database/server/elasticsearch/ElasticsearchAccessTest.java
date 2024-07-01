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

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.server.mongodb.CollectionSample;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.GetAliasesResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        GetAliasesResponse getAliasesResponse = elasticsearchAccess.getAlias(myalias);
        assertThat(getAliasesResponse.status()).isEqualTo(RestStatus.OK);
        assertThat(getAliasesResponse.getAliases()).containsOnlyKeys("myindex");
    }

    @Test
    public void testGetAliasForNonExistingAlias() throws IOException {
        GetAliasesResponse getAliasesResponse = elasticsearchAccess.getAlias(myalias);
        assertThat(getAliasesResponse.status()).isEqualTo(RestStatus.NOT_FOUND);
    }

    @Test
    public void testCreateIndexWithoutAlias() throws Exception {
        assertThat(elasticsearchAccess.existsIndex(myalias)).isFalse();
        ElasticsearchIndexAlias indexWithoutAlias = elasticsearchAccess.createIndexWithoutAlias(
            myalias,
            new ElasticsearchIndexSettings(2, 1, () -> "{}")
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
            new ElasticsearchIndexSettings(2, 1, () -> "{}")
        );

        elasticsearchAccess.indexEntry(myalias, id, document);

        SearchResponse searchResponseAfter = elasticsearchAccess.search(
            myalias,
            QueryBuilders.matchAllQuery(),
            null,
            null,
            null,
            0,
            100
        );
        assertThat(searchResponseAfter.getHits().getTotalHits().value).isEqualTo(1L);
        assertThat(searchResponseAfter.getHits().getAt(0).getId()).isEqualTo(id);
        assertThat(searchResponseAfter.getHits().getAt(0).getSourceAsMap()).containsOnlyKeys("Identifier");
    }

    @Test
    public void testIndexEntries() throws Exception {
        Map<String, VitamDocument<?>> documents = insertDataSet();

        SearchResponse searchResponseAfter = elasticsearchAccess.search(
            myalias,
            QueryBuilders.matchAllQuery(),
            null,
            null,
            null,
            0,
            100
        );
        assertThat(searchResponseAfter.getHits().getTotalHits().value).isEqualTo(documents.size());

        List<SearchHit> hits = IteratorUtils.toList(searchResponseAfter.getHits().iterator());
        assertThat(hits.stream().map(SearchHit::getId)).containsExactlyInAnyOrderElementsOf(documents.keySet());

        for (SearchHit hit : hits) {
            assertThat(hit.getSourceAsMap()).containsOnlyKeys("Identifier", "Name", "_tenant");
            assertThat(hit.getSourceAsMap().get("Identifier")).isEqualTo("value" + hit.getId());
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

        SearchResponse searchResponseAfter = elasticsearchAccess.search(
            myalias,
            QueryBuilders.matchAllQuery(),
            null,
            null,
            null,
            0,
            100
        );
        assertThat(searchResponseAfter.getHits().getTotalHits().value).isEqualTo(documents.size());

        List<SearchHit> hits = IteratorUtils.toList(searchResponseAfter.getHits().iterator());
        assertThat(hits.stream().map(SearchHit::getId)).containsExactlyInAnyOrderElementsOf(documents.keySet());

        for (SearchHit hit : hits) {
            if (hit.getId().equals(updatedDocumentId)) {
                // Updated doc
                assertThat(hit.getSourceAsMap()).containsOnlyKeys("newKey");
                assertThat(hit.getSourceAsMap().get("newKey")).isEqualTo("newValue" + hit.getId());
            } else {
                // Non updated documents
                assertThat(hit.getSourceAsMap()).containsOnlyKeys("Identifier", "Name", "_tenant");
                assertThat(hit.getSourceAsMap().get("Identifier")).isEqualTo("value" + hit.getId());
            }
        }
    }

    @Test
    public void testSearch() throws Exception {
        insertDataSet();

        // When
        SearchResponse searchResponse = elasticsearchAccess.search(
            myalias,
            QueryBuilders.rangeQuery("_tenant").gt(8).lte(11),
            null,
            null,
            null,
            0,
            100
        );

        assertThat(searchResponse.getHits().getTotalHits().value).isEqualTo(3);

        List<SearchHit> hits = IteratorUtils.toList(searchResponse.getHits().iterator());
        assertThat(hits.stream().map(i -> i.getSourceAsMap().get("_tenant"))).containsExactlyInAnyOrder(9, 10, 11);
    }

    @Test
    public void testSearchWithScrollingAndSorting() throws Exception {
        Map<String, VitamDocument<?>> documents = insertDataSet();

        // When
        List<SortBuilder<?>> sorts = singletonList(SortBuilders.fieldSort("_tenant").order(SortOrder.DESC));
        QueryBuilder query = QueryBuilders.matchQuery("Name", "Lorem ipsum");

        SearchResponse searchResponse1 = elasticsearchAccess.search(
            myalias,
            query,
            null,
            null,
            sorts,
            0,
            10,
            null,
            "START",
            5000,
            false
        );

        assertThat(searchResponse1.getHits().getTotalHits().value).isEqualTo(documents.size());
        String scrollId1 = searchResponse1.getScrollId();
        assertThat(scrollId1).isNotNull();
        List<SearchHit> hits1 = IteratorUtils.toList(searchResponse1.getHits().iterator());
        assertThat(hits1.stream().map(i -> i.getSourceAsMap().get("_tenant"))).containsExactly(
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

        SearchResponse searchResponse2 = elasticsearchAccess.search(
            myalias,
            query,
            null,
            null,
            sorts,
            0,
            10,
            null,
            scrollId1,
            5000,
            false
        );

        assertThat(searchResponse2.getHits().getTotalHits().value).isEqualTo(documents.size());
        String scrollId2 = searchResponse2.getScrollId();
        assertThat(scrollId2).isEqualTo(scrollId1);
        List<SearchHit> hits2 = IteratorUtils.toList(searchResponse2.getHits().iterator());
        assertThat(hits2.stream().map(i -> i.getSourceAsMap().get("_tenant"))).containsExactly(4, 3, 2, 1, 0);

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

        SearchResponse searchResponseAfter = elasticsearchAccess.search(
            myalias,
            QueryBuilders.matchAllQuery(),
            null,
            null,
            null,
            0,
            100
        );
        assertThat(searchResponseAfter.getHits().getTotalHits().value).isEqualTo(documents.size() - 1);

        List<SearchHit> hits = IteratorUtils.toList(searchResponseAfter.getHits().iterator());
        assertThat(hits.stream().map(SearchHit::getId)).containsExactlyInAnyOrderElementsOf(
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
            documents.put(
                id,
                new CollectionSample(
                    JsonHandler.createObjectNode()
                        .put("_id", id)
                        .put("Identifier", "value" + id)
                        .put("Name", "Lorem ipsum dolor sit amet, consectetur adipiscing elit")
                        .put("_tenant", i)
                )
            );
        }
        String mapping = FileUtils.readFileToString(
            PropertiesUtils.findFile("test-es-mapping.json"),
            StandardCharsets.UTF_8
        );

        elasticsearchAccess.createIndexAndAliasIfAliasNotExists(
            myalias,
            new ElasticsearchIndexSettings(2, 1, () -> mapping)
        );

        elasticsearchAccess.indexEntries(myalias, documents.values());

        return documents;
    }

    @Test
    public void testCreateIndexAndAliasIfAliasNotExistsNonExistingIndex() throws Exception {
        // Given
        assertThat(elasticsearchAccess.existsAlias(myalias)).isFalse();

        // When
        elasticsearchAccess.createIndexAndAliasIfAliasNotExists(
            myalias,
            new ElasticsearchIndexSettings(2, 1, () -> "{}")
        );

        // Then
        assertThat(elasticsearchAccess.existsAlias(myalias)).isTrue();

        GetAliasesResponse getAliasesResponse = elasticsearchAccess.getAlias(myalias);
        assertThat(getAliasesResponse.status()).isEqualTo(RestStatus.OK);
        assertThat(getAliasesResponse.getAliases()).hasSize(1);
        ElasticsearchIndexAlias myindex = ElasticsearchIndexAlias.ofFullIndexName(
            getAliasesResponse.getAliases().keySet().iterator().next()
        );
        assertThat(myalias.isValidAliasOfIndex(myindex)).isTrue();
    }

    @Test
    public void testCreateIndexAndAliasIfAliasNotExistsExistingIndex() throws Exception {
        // Given
        elasticsearchAccess.createIndexAndAliasIfAliasNotExists(
            myalias,
            new ElasticsearchIndexSettings(2, 1, () -> "{}")
        );
        waitCycle();
        assertThat(elasticsearchAccess.existsAlias(myalias)).isTrue();
        GetAliasesResponse existingAliasResponse = elasticsearchAccess.getAlias(myalias);
        assertThat(existingAliasResponse.status()).isEqualTo(RestStatus.OK);
        assertThat(existingAliasResponse.getAliases()).hasSize(1);
        ElasticsearchIndexAlias myExistingIndex = ElasticsearchIndexAlias.ofFullIndexName(
            existingAliasResponse.getAliases().keySet().iterator().next()
        );

        // When
        elasticsearchAccess.createIndexAndAliasIfAliasNotExists(
            myalias,
            new ElasticsearchIndexSettings(2, 1, () -> "{}")
        );

        // Then
        assertThat(elasticsearchAccess.existsAlias(myalias)).isTrue();

        GetAliasesResponse getAliasesResponse = elasticsearchAccess.getAlias(myalias);
        assertThat(getAliasesResponse.status()).isEqualTo(RestStatus.OK);
        assertThat(getAliasesResponse.getAliases()).hasSize(1);
        assertThat(getAliasesResponse.getAliases().keySet().iterator().next()).isEqualTo(myExistingIndex.getName());
    }

    @Test
    public void testSwitchIndex() throws Exception {
        // Given
        elasticsearchAccess.createIndexAndAliasIfAliasNotExists(
            myalias,
            new ElasticsearchIndexSettings(2, 1, () -> "{}")
        );
        waitCycle();
        ElasticsearchIndexAlias newIndex = elasticsearchAccess.createIndexWithoutAlias(
            myalias,
            new ElasticsearchIndexSettings(2, 1, () -> "{}")
        );
        ElasticsearchIndexAlias existingIndex = ElasticsearchIndexAlias.ofFullIndexName(
            elasticsearchAccess.getAlias(myalias).getAliases().keySet().iterator().next()
        );
        assertThat(newIndex.getName()).isNotEqualTo(existingIndex.getName());

        // When
        elasticsearchAccess.switchIndex(myalias, newIndex);

        // Then
        ElasticsearchIndexAlias indexAfterAliasSwitch = ElasticsearchIndexAlias.ofFullIndexName(
            elasticsearchAccess.getAlias(myalias).getAliases().keySet().iterator().next()
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
            new ElasticsearchIndexSettings(2, 1, () -> "{}")
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
            new ElasticsearchIndexSettings(2, 1, () -> "{}")
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
