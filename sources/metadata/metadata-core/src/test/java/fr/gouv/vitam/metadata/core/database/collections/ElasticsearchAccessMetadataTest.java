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
package fr.gouv.vitam.metadata.core.database.collections;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.collections.DynamicParserTokens;
import fr.gouv.vitam.common.database.collections.VitamDescriptionResolver;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.database.translators.elasticsearch.QueryToElasticsearch;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.metadata.core.config.ElasticsearchMetadataIndexManager;
import fr.gouv.vitam.metadata.core.utils.MappingLoaderTestUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static fr.gouv.vitam.metadata.core.database.collections.MetadataCollections.OBJECTGROUP;
import static fr.gouv.vitam.metadata.core.database.collections.MetadataCollections.UNIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.fail;

public class ElasticsearchAccessMetadataTest {

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    private static ElasticsearchAccessMetadata elasticsearchAccessMetadata;

    private static final Integer TENANT_ID_0 = 0;

    private static final String S1 = "{ \"title\":\"title1\", \"_max\": \"5\", \"_min\": \"2\"}";
    private static final String unit_with_max_5 =
        "{ \"_tenant\": 0, \"_id\": \"id2\", \"Title\":\"title1\", \"_max\": \"5\", \"_min\": \"2\"}";
    private static final String unit_with_max_4 =
        "{ \"_tenant\": 0, \"_id\": \"id1\", \"Title\":\"title1\", \"_max\": \"4\", \"_min\": \"2\"}";
    private static final String S1_OG =
        "{ \"Filename\":\"Vitam-Sensibilisation-API-V1.0.odp\", \"_max\": \"5\", \"_min\": \"2\"}";

    private static final String prefix = GUIDFactory.newGUID().getId();

    private static final ElasticsearchMetadataIndexManager indexManager =
        MetadataCollectionsTestUtils.createTestIndexManager(
            Collections.singletonList(TENANT_ID_0),
            Collections.emptyMap(),
            MappingLoaderTestUtils.getTestMappingLoader()
        );

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        List<ElasticsearchNode> esNodes = Lists.newArrayList(
            new ElasticsearchNode(ElasticsearchRule.getHost(), ElasticsearchRule.getPort())
        );

        elasticsearchAccessMetadata = new ElasticsearchAccessMetadata(
            ElasticsearchRule.VITAM_CLUSTER,
            esNodes,
            indexManager
        );

        UNIT.getVitamCollection().setName(prefix + UNIT.getClasz().getSimpleName());
        MetadataCollections.OBJECTGROUP.getVitamCollection()
            .setName(prefix + MetadataCollections.OBJECTGROUP.getClasz().getSimpleName());

        elasticsearchAccessMetadata.createIndexesAndAliases(OBJECTGROUP, UNIT);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        elasticsearchAccessMetadata.deleteIndexByAliasForTesting(
            indexManager.getElasticsearchIndexAliasResolver(UNIT).resolveIndexName(TENANT_ID_0)
        );
        elasticsearchAccessMetadata.deleteIndexByAliasForTesting(
            indexManager.getElasticsearchIndexAliasResolver(OBJECTGROUP).resolveIndexName(TENANT_ID_0)
        );
        elasticsearchAccessMetadata.close();
    }

    @After
    public void after() throws Exception {
        elasticsearchAccessMetadata.purgeIndexForTesting(
            indexManager.getElasticsearchIndexAliasResolver(UNIT).resolveIndexName(TENANT_ID_0)
        );
        elasticsearchAccessMetadata.purgeIndexForTesting(
            indexManager.getElasticsearchIndexAliasResolver(OBJECTGROUP).resolveIndexName(TENANT_ID_0)
        );
    }

    @Test
    public void should_ordering_result_when_scollId_and_ordering_is_passe_to_the_request() throws Exception {
        // Given
        String query =
            "{\n" +
            "  \"$roots\": [],\n" +
            "  \"$query\": [\n" +
            "    {\n" +
            "      \"$eq\": {\n" +
            "        \"Title\": \"title1\"\n" +
            "      }\n" +
            "    }\n" +
            "  ],\n" +
            "  \"$filter\":{\n" +
            "    \"$scrollId\": \"START\",\n" +
            "    \"$orderby\": {\n" +
            "      \"#max\": -1\n" +
            "    }\n" +
            "  },\n" +
            "  \"$projection\":{\n" +
            "    \"$fields\":{\n" +
            "      \"#id\":1,\n" +
            "      \"#max\":1\n" +
            "    }\n" +
            "  }\n" +
            "}";

        // add unit
        final String id = GUIDFactory.newUnitGUID(TENANT_ID_0).toString();
        final String id2 = GUIDFactory.newUnitGUID(TENANT_ID_0).toString();

        assertThatCode(
            () ->
                elasticsearchAccessMetadata.indexEntry(
                    UNIT,
                    TENANT_ID_0,
                    id,
                    JsonHandler.getFromString(unit_with_max_4, Unit.class)
                )
        ).doesNotThrowAnyException();

        assertThatCode(
            () ->
                elasticsearchAccessMetadata.indexEntry(
                    UNIT,
                    TENANT_ID_0,
                    id2,
                    JsonHandler.getFromString(unit_with_max_5, Unit.class)
                )
        ).doesNotThrowAnyException();

        elasticsearchAccessMetadata.refreshIndex(UNIT, TENANT_ID_0);

        JsonNode queryNode = JsonHandler.getFromString(query);
        SelectParserMultiple parser = new SelectParserMultiple();
        parser.parse(queryNode);
        List<SortBuilder<?>> sorts = new ArrayList<>();
        List<Query> queries = parser.getRequest().getQueries();
        DynamicParserTokens parserTokens = new DynamicParserTokens(
            new VitamDescriptionResolver(Collections.emptyList()),
            Collections.emptyList()
        );
        Query elasticQuery = queries.get(0);
        SortBuilder<?> sortBuilder = new FieldSortBuilder("_max");
        sortBuilder.order(SortOrder.DESC);
        sorts.add(sortBuilder);
        BoolQueryBuilder queryBuilder = new BoolQueryBuilder()
            .must(QueryToElasticsearch.getCommand(elasticQuery, new MongoDbVarNameAdapter(), parserTokens));
        // When
        Result<?> result = elasticsearchAccessMetadata.search(
            UNIT,
            TENANT_ID_0,
            queryBuilder,
            sorts,
            0,
            10_000,
            null,
            "START",
            0,
            false
        );
        // Then
        assertThat(result.getNbResult()).isEqualTo(2);
        assertThat(result.getCurrentIds().indexOf(id)).isEqualTo(1);
        assertThat(result.getCurrentIds().indexOf(id2)).isEqualTo(0);

        // Test clear scroll ony if no result found
        queryBuilder = new BoolQueryBuilder()
            .must(QueryToElasticsearch.getCommand(elasticQuery, new MongoDbVarNameAdapter(), parserTokens));

        // As limit == 1, first call should return only 1 document even 2 documents found
        result = elasticsearchAccessMetadata.search(
            UNIT,
            TENANT_ID_0,
            queryBuilder,
            sorts,
            0,
            1,
            null,
            "START",
            5000,
            false
        );

        assertThat(result.getNbResult()).isEqualTo(1);
        assertThat(result.getTotal()).isEqualTo(2);
        assertThat(result.getCurrentIds()).contains(id2);

        // Next result should return the remaining documents
        result = elasticsearchAccessMetadata.search(
            UNIT,
            TENANT_ID_0,
            queryBuilder,
            sorts,
            -1,
            1,
            null,
            result.scrollId,
            5000,
            false
        );

        assertThat(result.getNbResult()).isEqualTo(1);
        assertThat(result.getTotal()).isEqualTo(2);
        assertThat(result.getCurrentIds()).contains(id);

        // Third call result should be empty
        result = elasticsearchAccessMetadata.search(
            UNIT,
            TENANT_ID_0,
            queryBuilder,
            sorts,
            -1,
            1,
            null,
            result.scrollId,
            5000,
            false
        );

        assertThat(result.getNbResult()).isEqualTo(0);

        // Last call should fail as scroll is cleared event scrollTimeout = 5000
        try {
            elasticsearchAccessMetadata.search(
                UNIT,
                TENANT_ID_0,
                queryBuilder,
                sorts,
                -1,
                1,
                null,
                result.scrollId,
                5000,
                false
            );
            fail("should throw an exception (scroll id not found)");
        } catch (Exception e) {
            // Elasticsearch exception [type=search_context_missing_exception, reason=No search context found for id]
        }

        elasticsearchAccessMetadata.delete(UNIT, Lists.newArrayList(id, id2), TENANT_ID_0);
    }

    @Test
    public void testElasticsearchAccessMetadatas() {
        final String id = GUIDFactory.newUnitGUID(TENANT_ID_0).toString();
        assertThatCode(
            () ->
                elasticsearchAccessMetadata.indexEntry(UNIT, TENANT_ID_0, id, JsonHandler.getFromString(S1, Unit.class))
        ).doesNotThrowAnyException();

        // delete index
        assertThatCode(
            () -> elasticsearchAccessMetadata.deleteIndexByAliasForTesting(UNIT, TENANT_ID_0)
        ).doesNotThrowAnyException();
    }

    @Test
    public void testElasticsearchUpdateAccessMetadatas() {
        // add unit
        final String id = GUIDFactory.newUnitGUID(TENANT_ID_0).toString();
        assertThatCode(
            () ->
                elasticsearchAccessMetadata.indexEntry(UNIT, TENANT_ID_0, id, JsonHandler.getFromString(S1, Unit.class))
        ).doesNotThrowAnyException();
    }

    @Test
    public void testElasticsearchAccessOGMetadatas() throws Exception {
        // add OG
        final String id = GUIDFactory.newUnitGUID(TENANT_ID_0).toString();
        assertThatCode(
            () ->
                elasticsearchAccessMetadata.indexEntry(
                    OBJECTGROUP,
                    TENANT_ID_0,
                    id,
                    JsonHandler.getFromString(S1_OG, ObjectGroup.class)
                )
        ).doesNotThrowAnyException();

        elasticsearchAccessMetadata.refreshIndex(OBJECTGROUP, TENANT_ID_0);

        // delete index
        assertThatCode(
            () -> elasticsearchAccessMetadata.deleteIndexByAliasForTesting(OBJECTGROUP, TENANT_ID_0)
        ).doesNotThrowAnyException();
    }

    @Test
    public void testElasticsearchUpdateOGAccessMetadatas() {
        // add OG
        final String id = GUIDFactory.newObjectGroupGUID(TENANT_ID_0).toString();

        assertThatCode(
            () ->
                elasticsearchAccessMetadata.indexEntry(
                    OBJECTGROUP,
                    TENANT_ID_0,
                    id,
                    JsonHandler.getFromString(S1_OG, ObjectGroup.class)
                )
        ).doesNotThrowAnyException();
    }
}
