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

import com.mongodb.client.MongoCollection;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.api.impl.VitamElasticsearchRepository;
import fr.gouv.vitam.common.database.api.impl.VitamMongoRepository;
import fr.gouv.vitam.common.database.index.model.ReindexationOK;
import fr.gouv.vitam.common.database.index.model.SwitchIndexResult;
import fr.gouv.vitam.common.database.server.elasticsearch.model.ElasticsearchCollections;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.mongo.MongoRule;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomUtils;
import org.assertj.core.util.Lists;
import org.bson.Document;
import org.elasticsearch.client.GetAliasesResponse;
import org.elasticsearch.common.Strings;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.xcontent.XContentBuilder;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.xcontent.XContentFactory.jsonBuilder;

public class IndexationHelperTest {

    private static final String INDEX = "index" + GUIDFactory.newGUID().getId();
    private static final String ALIAS = "alias" + GUIDFactory.newGUID().getId();

    private static final String TEST_ES_MAPPING_JSON = "test-es-mapping.json";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @ClassRule
    public static MongoRule mongoRule = new MongoRule(
        MongoDbAccess.getMongoClientSettingsBuilder(AgenciesTest.class),
        ALIAS
    );

    @ClassRule
    public static ElasticsearchRule elasticsearchRule = new ElasticsearchRule(ALIAS + "_0", ALIAS + "_1", ALIAS + "_2");

    private static ElasticsearchAccess elasticsearchAccess;
    private final IndexationHelper indexationHelper = IndexationHelper.getInstance();

    @BeforeClass
    public static void setUp() throws Exception {
        ArrayList<ElasticsearchNode> esNodes = Lists.newArrayList(
            new ElasticsearchNode(ElasticsearchRule.getHost(), ElasticsearchRule.getPort())
        );
        elasticsearchAccess = new ElasticsearchAccess(ElasticsearchRule.VITAM_CLUSTER, esNodes);

        elasticsearchRule.createIndex(ALIAS, INDEX, "{}");
        elasticsearchRule.createIndex(ALIAS + "_0", INDEX + "_0_initialindex", "{}");
        elasticsearchRule.createIndex(ALIAS + "_mygrp", INDEX + "_mygrp_initialindex", "{}");
    }

    @AfterClass
    public static void afterClass() {
        mongoRule.handleAfterClass();
        elasticsearchRule.purgeIndices();
        elasticsearchAccess.close();
    }

    @After
    public void after() {
        elasticsearchRule.deleteIndexesWithoutClose();
    }

    @Test
    public void should_reindex_and_switch_index_for_multiple_documents_with_tenant()
        throws IOException, DatabaseException {
        // Given
        final MongoCollection<Document> collection = mongoRule.getMongoCollection(ALIAS);
        List<Integer> tenants = Arrays.asList(0, 1, 2);
        ElasticsearchIndexAliasResolver indexAliasResolver = tenantId -> {
            switch (tenantId) {
                case 0:
                    return ElasticsearchIndexAlias.ofMultiTenantCollection(ALIAS, tenantId);
                case 1:
                case 2:
                    return ElasticsearchIndexAlias.ofMultiTenantCollection(ALIAS, "mygrp");
                default:
                    throw new IllegalStateException("Unexpected value: " + tenantId);
            }
        };

        Map<String, Integer> mapIdsTenants = populating(collection, tenants, indexAliasResolver);

        String mapping = FileUtils.readFileToString(
            PropertiesUtils.findFile(TEST_ES_MAPPING_JSON),
            StandardCharsets.UTF_8
        );
        ElasticsearchIndexAlias indexAlias0 = indexAliasResolver.resolveIndexName(0);
        ElasticsearchIndexAlias indexAliasGrp = indexAliasResolver.resolveIndexName(1);
        ElasticsearchIndexSettings indexSettings = new ElasticsearchIndexSettings(2, 1, () -> mapping);

        // When
        ReindexationOK reindexTenant0 = indexationHelper.reindex(
            collection,
            elasticsearchAccess,
            indexAlias0,
            indexSettings,
            ElasticsearchCollections.OBJECTGROUP,
            singletonList(0),
            null
        );
        ReindexationOK reindexTenantGroup = indexationHelper.reindex(
            collection,
            elasticsearchAccess,
            indexAliasGrp,
            indexSettings,
            ElasticsearchCollections.OBJECTGROUP,
            Arrays.asList(1, 2),
            "mygrp"
        );

        // Then
        ElasticsearchIndexAlias newIndex0 = ElasticsearchIndexAlias.ofFullIndexName(reindexTenant0.getIndexName());
        ElasticsearchIndexAlias newIndexGrp = ElasticsearchIndexAlias.ofFullIndexName(
            reindexTenantGroup.getIndexName()
        );

        assertThat(reindexTenant0.getAliasName()).isEqualTo(indexAlias0.getName());
        assertThat(indexAlias0.isValidAliasOfIndex(newIndex0)).isTrue();
        assertThat(reindexTenant0.getTenants()).containsExactlyInAnyOrder(0);
        assertThat(reindexTenant0.getTenantGroup()).isNull();

        assertThat(reindexTenantGroup.getAliasName()).isEqualTo(indexAliasGrp.getName());
        assertThat(indexAliasGrp.isValidAliasOfIndex(newIndexGrp)).isTrue();
        assertThat(reindexTenantGroup.getTenants()).containsExactlyInAnyOrder(1, 2);
        assertThat(reindexTenantGroup.getTenantGroup()).isEqualTo("mygrp");

        // Ensure new index exists
        assertThat(elasticsearchAccess.existsIndex(newIndex0)).isTrue();
        assertThat(elasticsearchAccess.existsIndex(newIndexGrp)).isTrue();

        // Ensure aliases still reference old indexes
        GetAliasesResponse alias0BeforeSwitch = elasticsearchAccess.getAlias(indexAlias0);
        assertThat(alias0BeforeSwitch.getAliases()).containsOnlyKeys(INDEX + "_0_initialindex");

        GetAliasesResponse aliasGrpBeforeSwitch = elasticsearchAccess.getAlias(indexAliasGrp);
        assertThat(aliasGrpBeforeSwitch.getAliases()).containsOnlyKeys(INDEX + "_mygrp_initialindex");

        // Switch indices
        SwitchIndexResult switchIndexResult0 = indexationHelper.switchIndex(
            indexAlias0,
            newIndex0,
            elasticsearchAccess
        );
        SwitchIndexResult switchIndexResultGrp = indexationHelper.switchIndex(
            indexAliasGrp,
            newIndexGrp,
            elasticsearchAccess
        );

        assertThat(switchIndexResult0.getStatusCode()).isEqualTo(StatusCode.OK);
        assertThat(switchIndexResultGrp.getStatusCode()).isEqualTo(StatusCode.OK);

        // Check alias references
        GetAliasesResponse alias0AfterSwitch = elasticsearchAccess.getAlias(indexAlias0);
        assertThat(alias0AfterSwitch.getAliases()).containsOnlyKeys(newIndex0.getName());

        GetAliasesResponse aliasGrpAfterSwitch = elasticsearchAccess.getAlias(indexAliasGrp);
        assertThat(aliasGrpAfterSwitch.getAliases()).containsOnlyKeys(newIndexGrp.getName());

        // Purge old indices
        elasticsearchRule.purgeIndex(elasticsearchRule.getClient(), INDEX);
        elasticsearchRule.purgeIndex(elasticsearchRule.getClient(), INDEX + "_mygrp_initialindex");

        // Check documents
        elasticsearchAccess.refreshIndex(indexAlias0);
        elasticsearchAccess.refreshIndex(indexAliasGrp);
        VitamElasticsearchRepository vitamElasticsearchRepository = new VitamElasticsearchRepository(
            elasticsearchAccess.getClient(),
            indexAliasResolver
        );

        for (String id : mapIdsTenants.keySet()) {
            Integer tenant = mapIdsTenants.get(id);
            Optional<Document> documentById = vitamElasticsearchRepository.getByID(id, tenant);
            assertThat(documentById).isPresent();
            Document document = documentById.get();
            assertThat(document.get("Name", String.class)).contains("Description_" + tenant);
        }

        assertThat(countDocumentsByQuery(indexAlias0, vitamElasticsearchRepository, matchAllQuery())).isEqualTo(10);
        assertThat(countDocumentsByQuery(indexAliasGrp, vitamElasticsearchRepository, matchAllQuery())).isEqualTo(20);

        assertThat(
            countDocumentsByQuery(
                indexAlias0,
                vitamElasticsearchRepository,
                QueryBuilders.termQuery("Identifier", "Identifier_0")
            )
        ).isEqualTo(10);
        assertThat(
            countDocumentsByQuery(
                indexAliasGrp,
                vitamElasticsearchRepository,
                QueryBuilders.termQuery("Identifier", "Identifier_1")
            )
        ).isEqualTo(10);
        assertThat(
            countDocumentsByQuery(
                indexAliasGrp,
                vitamElasticsearchRepository,
                QueryBuilders.termQuery("Identifier", "Identifier_2")
            )
        ).isEqualTo(10);

        elasticsearchAccess.deleteIndexForTesting(newIndex0);
        elasticsearchAccess.deleteIndexForTesting(newIndexGrp);
    }

    private long countDocumentsByQuery(
        ElasticsearchIndexAlias indexAlias0,
        VitamElasticsearchRepository vitamElasticsearchRepository,
        QueryBuilder query
    ) throws IOException {
        return vitamElasticsearchRepository.search(indexAlias0.getName(), query).getHits().getTotalHits().value;
    }

    @Test
    public void should_reindex_and_switch_index_for_multiple_documents_without_tenant()
        throws IOException, DatabaseException {
        // Given
        ElasticsearchIndexAliasResolver indexAliasResolver = tenantId ->
            ElasticsearchIndexAlias.ofCrossTenantCollection(ALIAS);
        String mapping = FileUtils.readFileToString(
            PropertiesUtils.findFile(TEST_ES_MAPPING_JSON),
            StandardCharsets.UTF_8
        );
        ElasticsearchIndexAlias indexAlias = indexAliasResolver.resolveIndexName(null);

        ElasticsearchIndexSettings indexSettings = new ElasticsearchIndexSettings(2, 1, () -> mapping);

        final MongoCollection<Document> collection = mongoRule.getMongoCollection(ALIAS);
        Map<String, Integer> mapIdsTenants = populating(collection, indexAliasResolver);

        // When
        ReindexationOK indexationResult = indexationHelper.reindex(
            collection,
            elasticsearchAccess,
            indexAlias,
            indexSettings,
            ElasticsearchCollections.OBJECTGROUP,
            null,
            null
        );

        // Then
        ElasticsearchIndexAlias newIndex = ElasticsearchIndexAlias.ofFullIndexName(indexationResult.getIndexName());

        assertThat(indexationResult.getAliasName()).isEqualTo(indexAlias.getName());
        assertThat(indexAlias.isValidAliasOfIndex(newIndex)).isTrue();
        assertThat(indexationResult.getTenants()).isNull();
        assertThat(indexationResult.getTenantGroup()).isNull();

        // Ensure new index exists
        assertThat(elasticsearchAccess.existsIndex(newIndex)).isTrue();

        // Ensure aliases still reference old indexes
        GetAliasesResponse aliasBeforeSwitch = elasticsearchAccess.getAlias(indexAlias);
        assertThat(aliasBeforeSwitch.getAliases()).containsOnlyKeys(INDEX);

        // Switch indices
        SwitchIndexResult switchIndexResult = indexationHelper.switchIndex(indexAlias, newIndex, elasticsearchAccess);

        assertThat(switchIndexResult.getStatusCode()).isEqualTo(StatusCode.OK);

        // Check alias references
        GetAliasesResponse aliasAfterSwitch = elasticsearchAccess.getAlias(indexAlias);
        assertThat(aliasAfterSwitch.getAliases()).containsOnlyKeys(newIndex.getName());

        // Purge old indices
        elasticsearchRule.purgeIndex(elasticsearchRule.getClient(), INDEX);

        // Check documents
        elasticsearchAccess.refreshIndex(indexAlias);
        VitamElasticsearchRepository vitamElasticsearchRepository = new VitamElasticsearchRepository(
            elasticsearchAccess.getClient(),
            indexAliasResolver
        );

        for (String id : mapIdsTenants.keySet()) {
            Optional<Document> documentById = vitamElasticsearchRepository.getByID(id, null);
            assertThat(documentById).isPresent();
            Document document = documentById.get();
            assertThat(document.get("Name", String.class)).contains("Identifier_No_Tenant");
        }

        assertThat(countDocumentsByQuery(indexAlias, vitamElasticsearchRepository, matchAllQuery())).isEqualTo(10);

        assertThat(
            countDocumentsByQuery(
                indexAlias,
                vitamElasticsearchRepository,
                QueryBuilders.termQuery("Identifier", "Identifier_No_Tenant")
            )
        ).isEqualTo(10);

        elasticsearchRule.purgeIndex(elasticsearchRule.getClient(), newIndex.getName());
    }

    private Map<String, Integer> populating(
        MongoCollection<Document> collection,
        ElasticsearchIndexAliasResolver indexAliasResolver
    ) throws IOException, DatabaseException {
        VitamMongoRepository vitamMongoRepository = new VitamMongoRepository(collection);
        VitamElasticsearchRepository vitamElasticsearchRepository;
        Map<String, Integer> ids = new HashMap<>();
        vitamElasticsearchRepository = new VitamElasticsearchRepository(
            elasticsearchRule.getClient(),
            indexAliasResolver
        );
        insertDocuments(vitamMongoRepository, vitamElasticsearchRepository, ids);
        return ids;
    }

    private Map<String, Integer> populating(
        MongoCollection<Document> collection,
        List<Integer> tenants,
        ElasticsearchIndexAliasResolver indexAliasResolver
    ) throws IOException, DatabaseException {
        VitamMongoRepository vitamMongoRepository = new VitamMongoRepository(collection);
        VitamElasticsearchRepository vitamElasticsearchRepository;

        Map<String, Integer> ids = new HashMap<>();
        for (Integer tenant : tenants) {
            vitamElasticsearchRepository = new VitamElasticsearchRepository(
                elasticsearchRule.getClient(),
                indexAliasResolver
            );
            insertDocuments(vitamMongoRepository, vitamElasticsearchRepository, ids, tenant);
        }
        return ids;
    }

    private void insertDocuments(
        VitamMongoRepository vitamMongoRepository,
        VitamElasticsearchRepository vitamElasticsearchRepository,
        Map<String, Integer> ids,
        Integer tenant
    ) throws IOException, DatabaseException {
        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String id = GUIDFactory.newGUID().toString();
            XContentBuilder builder = jsonBuilder()
                .startObject()
                .field("_id", id)
                .field("_tenant", tenant)
                .field("Identifier", "Identifier_" + tenant)
                .field("Name", "Description_" + tenant + "_" + i + " " + RandomUtils.nextDouble())
                .endObject();
            documents.add(Document.parse(Strings.toString(builder)));
            ids.put(id, tenant);
        }
        vitamMongoRepository.save(documents);
        vitamElasticsearchRepository.save(documents);
    }

    private void insertDocuments(
        VitamMongoRepository vitamMongoRepository,
        VitamElasticsearchRepository vitamElasticsearchRepository,
        Map<String, Integer> ids
    ) throws IOException, DatabaseException {
        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String id = GUIDFactory.newGUID().toString();
            String value = "Identifier_No_Tenant";
            XContentBuilder builder = jsonBuilder()
                .startObject()
                .field("_id", id)
                .field("Identifier", value)
                .field("Name", "Description_" + value + " " + RandomUtils.nextDouble())
                .endObject();
            documents.add(Document.parse(Strings.toString(builder)));
            ids.put(id, null);
        }
        vitamMongoRepository.save(documents);
        vitamElasticsearchRepository.save(documents);
    }
}
