/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.database.collections.VitamDescriptionResolver;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.database.translators.elasticsearch.QueryToElasticsearch;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ElasticsearchAccessMetadataTest {
    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    private final static String HOST_NAME = "127.0.0.1";
    private static ElasticsearchAccessMetadata elasticsearchAccessMetadata;

    private static final Integer TENANT_ID_0 = 0;

    private static final String S1 = "{ \"title\":\"title1\", \"_max\": \"5\", \"_min\": \"2\"}";
    private static final String unit_with_max_5 = "{  \"Title\":\"title1\", \"_max\": \"5\", \"_min\": \"2\"}";
    private static final String unit_with_max_4 = "{  \"Title\":\"title1\", \"_max\": \"4\", \"_min\": \"2\"}";
    private static final String S1_OG =
        "{ \"Filename\":\"Vitam-Sensibilisation-API-V1.0.odp\", \"_max\": \"5\", \"_min\": \"2\"}";

    private final static String prefix = GUIDFactory.newGUID().getId();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {


        elasticsearchAccessMetadata = new ElasticsearchAccessMetadata(ElasticsearchRule.VITAM_CLUSTER,
            Lists.newArrayList(new ElasticsearchNode(HOST_NAME, ElasticsearchRule.TCP_PORT)));

        elasticsearchAccessMetadata.addIndex(MetadataCollections.UNIT, TENANT_ID_0);

        MetadataCollections.UNIT.getVitamCollection()
            .setName(prefix + MetadataCollections.UNIT.getClasz().getSimpleName());
        MetadataCollections.OBJECTGROUP.getVitamCollection()
            .setName(prefix + MetadataCollections.OBJECTGROUP.getClasz().getSimpleName());

        elasticsearchAccessMetadata.addIndex(MetadataCollections.UNIT, TENANT_ID_0);
        elasticsearchAccessMetadata.addIndex(MetadataCollections.OBJECTGROUP, TENANT_ID_0);

    }

    @AfterClass
    public static void tearDownAfterClass() {
        elasticsearchAccessMetadata.deleteIndex(MetadataCollections.UNIT, TENANT_ID_0);
        elasticsearchAccessMetadata.deleteIndex(MetadataCollections.OBJECTGROUP, TENANT_ID_0);
        elasticsearchAccessMetadata.close();
    }

    @After
    public void after() {
        elasticsearchAccessMetadata.purgeIndex(MetadataCollections.UNIT.getName());
        elasticsearchAccessMetadata.purgeIndex(MetadataCollections.OBJECTGROUP.getName());
    }

    @Test
    public void should_ordering_result_when_scollId_and_ordering_is_passe_to_the_request() throws Exception {
        // Given
        String query = "{\n" +
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

        // add index
        Map<String, String> res = elasticsearchAccessMetadata.addIndex(MetadataCollections.UNIT, TENANT_ID_0);
        assertThat(res).hasSize(1);
        assertThat(res.keySet().iterator().next()).isEqualTo(prefix + "unit_0");

        // add unit
        final String id = GUIDFactory.newUnitGUID(TENANT_ID_0).toString();
        final String id2 = GUIDFactory.newUnitGUID(TENANT_ID_0).toString();
        assertThat(
            elasticsearchAccessMetadata.addEntryIndex(MetadataCollections.UNIT, TENANT_ID_0, id, unit_with_max_4))
            .isTrue();
        assertThat(
            elasticsearchAccessMetadata.addEntryIndex(MetadataCollections.UNIT, TENANT_ID_0, id2, unit_with_max_5))
            .isTrue();

        elasticsearchAccessMetadata.refreshIndex(MetadataCollections.UNIT, TENANT_ID_0);

        JsonNode queryNode = JsonHandler.getFromString(query);
        SelectParserMultiple parser = new SelectParserMultiple();
        parser.parse(queryNode);
        List<SortBuilder> sorts = new ArrayList<>();
        List<Query> queries = parser.getRequest().getQueries();
        DynamicParserTokens parserTokens = new DynamicParserTokens(new VitamDescriptionResolver(Collections.emptyList()), Collections.emptyList());
        for (Query elasticQuery : queries) {
            SortBuilder sortBuilder = new FieldSortBuilder("_max");
            sortBuilder.order(SortOrder.DESC);
            sorts.add(sortBuilder);
            BoolQueryBuilder queryBuilder = new BoolQueryBuilder()
                .must(QueryToElasticsearch.getCommand(elasticQuery, new MongoDbVarNameAdapter(), parserTokens));
            // When
            Result result = elasticsearchAccessMetadata
                .search(MetadataCollections.UNIT, TENANT_ID_0, VitamCollection.getTypeunique(), queryBuilder, sorts, 0,
                    10_000, null, "START", 0);
            // Then
            assertThat(result.getNbResult()).isEqualTo(2);
            assertThat(result.getCurrentIds().indexOf(id)).isEqualTo(1);
            assertThat(result.getCurrentIds().indexOf(id2)).isEqualTo(0);
        }

        elasticsearchAccessMetadata
            .deleteEntryIndex(MetadataCollections.UNIT, TENANT_ID_0, VitamCollection.getTypeunique(), id);
        elasticsearchAccessMetadata
            .deleteEntryIndex(MetadataCollections.UNIT, TENANT_ID_0, VitamCollection.getTypeunique(), id2);
    }

    @Test
    public void testElasticsearchAccessMetadatas() throws InvalidParseOperationException {
        // add index
        Map<String, String> res = elasticsearchAccessMetadata.addIndex(MetadataCollections.UNIT, TENANT_ID_0);
        assertThat(res).hasSize(1);
        assertThat(res.keySet().iterator().next()).isEqualTo(prefix + "unit_0");        // add unit
        final String id = GUIDFactory.newUnitGUID(TENANT_ID_0).toString();
        assertEquals(true, elasticsearchAccessMetadata.addEntryIndex(MetadataCollections.UNIT, TENANT_ID_0, id, S1));
        // delete unit
        try {
            elasticsearchAccessMetadata
                .deleteEntryIndex(MetadataCollections.UNIT, TENANT_ID_0, VitamCollection.getTypeunique(), id);
        } catch (final MetaDataExecutionException | MetaDataNotFoundException e) {
            fail(e.getMessage());
        }

        try {
            elasticsearchAccessMetadata
                .deleteEntryIndex(MetadataCollections.UNIT, TENANT_ID_0, VitamCollection.getTypeunique(),
                    GUIDFactory.newUnitGUID(TENANT_ID_0).toString());
            fail("Unit not found");

        } catch (final MetaDataExecutionException | MetaDataNotFoundException e) {
            // Success if exception happen.
        }

        // delete index
        assertEquals(true, elasticsearchAccessMetadata.deleteIndex(MetadataCollections.UNIT, TENANT_ID_0));

        @SuppressWarnings("unchecked")
        final Map<String, String> targetMap =
            (Map<String, String>) (Object) JsonHandler.getMapFromString(S1);
        // add entries
        elasticsearchAccessMetadata.addEntryIndexes(MetadataCollections.UNIT, TENANT_ID_0, targetMap);
    }

    @Test
    public void testElasticsearchUpdateAccessMetadatas() throws Exception {

        // add index
        Map<String, String> res = elasticsearchAccessMetadata.addIndex(MetadataCollections.UNIT, TENANT_ID_0);
        assertThat(res).hasSize(1);
        assertThat(res.keySet().iterator().next()).isEqualTo(prefix + "unit_0");
        // add unit
        final String id = GUIDFactory.newUnitGUID(TENANT_ID_0).toString();
        assertEquals(true, elasticsearchAccessMetadata.addEntryIndex(MetadataCollections.UNIT, TENANT_ID_0, id, S1));
    }

    @Test
    public void testElasticsearchAccessOGMetadatas() throws MetaDataExecutionException, MetaDataNotFoundException {

        // add index
        Map<String, String> res = elasticsearchAccessMetadata.addIndex(MetadataCollections.OBJECTGROUP, TENANT_ID_0);
        assertThat(res).hasSize(1);
        assertThat(res.keySet().iterator().next()).isEqualTo(prefix + "objectgroup_0");

        // add OG
        final String id = GUIDFactory.newUnitGUID(TENANT_ID_0).toString();
        assertEquals(true, elasticsearchAccessMetadata
            .addEntryIndex(MetadataCollections.OBJECTGROUP, TENANT_ID_0, id, S1_OG));

        elasticsearchAccessMetadata.refreshIndex(MetadataCollections.OBJECTGROUP, TENANT_ID_0);


        // delete OG
        elasticsearchAccessMetadata
            .deleteEntryIndex(MetadataCollections.OBJECTGROUP, TENANT_ID_0, VitamCollection.getTypeunique(), id);

        // delete index
        assertEquals(true, elasticsearchAccessMetadata.deleteIndex(MetadataCollections.OBJECTGROUP, TENANT_ID_0));

    }

    @Test(expected = MetaDataExecutionException.class)
    public void testElasticSearchDeleteOGAccessMetadatNoSuchIndexThrowException()
        throws MetaDataExecutionException, MetaDataNotFoundException {
        elasticsearchAccessMetadata
            .deleteEntryIndex(MetadataCollections.OBJECTGROUP, TENANT_ID_0, VitamCollection.getTypeunique(),
                GUIDFactory.newObjectGroupGUID(TENANT_ID_0).toString());
    }

    @Test
    public void testElasticsearchUpdateOGAccessMetadatas() throws Exception {

        // add index
        Map<String, String> res = elasticsearchAccessMetadata.addIndex(MetadataCollections.OBJECTGROUP, TENANT_ID_0);
        assertThat(res).hasSize(1);
        assertThat(res.keySet().iterator().next()).isEqualTo(prefix + "objectgroup_0");

        // add OG
        final String id = GUIDFactory.newObjectGroupGUID(TENANT_ID_0).toString();
        assertEquals(true, elasticsearchAccessMetadata
            .addEntryIndex(MetadataCollections.OBJECTGROUP, TENANT_ID_0, id, S1_OG));

    }

    @Test
    @RunWithCustomExecutor
    public void testElasticsearchOGAccessMetadatas() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        // add index
        Map<String, String> res = elasticsearchAccessMetadata.addIndex(MetadataCollections.OBJECTGROUP, TENANT_ID_0);
        assertThat(res).hasSize(1);
        assertThat(res.keySet().iterator().next()).isEqualTo(prefix + "objectgroup_0");

        // add OG
        final String id = GUIDFactory.newObjectGroupGUID(TENANT_ID_0).toString();
        assertEquals(true, elasticsearchAccessMetadata
            .addEntryIndex(MetadataCollections.OBJECTGROUP, TENANT_ID_0, id, S1_OG));
    }
}
