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

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.elasticsearch.indices.GetAliasResponse;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.client.MongoCollection;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.api.impl.VitamElasticsearchRepository;
import fr.gouv.vitam.common.database.api.impl.VitamMongoRepository;
import fr.gouv.vitam.common.database.index.model.ReindexationOK;
import fr.gouv.vitam.common.database.index.model.SwitchIndexResult;
import fr.gouv.vitam.common.database.parameter.IndexParameters;
import fr.gouv.vitam.common.database.server.elasticsearch.model.ElasticsearchCollections;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchTestHelper;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.time.LogicalClockRule;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomUtils;
import org.assertj.core.util.Lists;
import org.bson.Document;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchUtil.matchAll;
import static fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchUtil.termQuery;
import static fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchUtil.wildcard;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class IndexationHelperTest {

    private static final String INDEX = "index" + GUIDFactory.newGUID().getId();
    private static final String ALIAS = "alias" + GUIDFactory.newGUID().getId();

    private static final String TEST_ES_MAPPING_JSON = "test-es-mapping.json";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public LogicalClockRule logicalClock = new LogicalClockRule();

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
            null,
            ElasticsearchTestHelper.loadElasticSearchSettings()
        );
        ReindexationOK reindexTenantGroup = indexationHelper.reindex(
            collection,
            elasticsearchAccess,
            indexAliasGrp,
            indexSettings,
            ElasticsearchCollections.OBJECTGROUP,
            Arrays.asList(1, 2),
            "mygrp",
            ElasticsearchTestHelper.loadElasticSearchSettings()
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
        GetAliasResponse alias0BeforeSwitch = elasticsearchAccess.getAlias(indexAlias0);
        assertThat(alias0BeforeSwitch.aliases()).containsOnlyKeys(INDEX + "_0_initialindex");

        GetAliasResponse aliasGrpBeforeSwitch = elasticsearchAccess.getAlias(indexAliasGrp);
        assertThat(aliasGrpBeforeSwitch.aliases()).containsOnlyKeys(INDEX + "_mygrp_initialindex");

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
        GetAliasResponse alias0AfterSwitch = elasticsearchAccess.getAlias(indexAlias0);
        assertThat(alias0AfterSwitch.aliases()).containsOnlyKeys(newIndex0.getName());

        GetAliasResponse aliasGrpAfterSwitch = elasticsearchAccess.getAlias(indexAliasGrp);
        assertThat(aliasGrpAfterSwitch.aliases()).containsOnlyKeys(newIndexGrp.getName());

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

        assertThat(countDocumentsByQuery(indexAlias0, vitamElasticsearchRepository, matchAll())).isEqualTo(10);
        assertThat(countDocumentsByQuery(indexAliasGrp, vitamElasticsearchRepository, matchAll())).isEqualTo(20);

        assertThat(
            countDocumentsByQuery(indexAlias0, vitamElasticsearchRepository, termQuery("Identifier", "Identifier_0"))
        ).isEqualTo(10);
        assertThat(
            countDocumentsByQuery(indexAliasGrp, vitamElasticsearchRepository, termQuery("Identifier", "Identifier_1"))
        ).isEqualTo(10);
        assertThat(
            countDocumentsByQuery(indexAliasGrp, vitamElasticsearchRepository, termQuery("Identifier", "Identifier_2"))
        ).isEqualTo(10);

        elasticsearchAccess.deleteIndexForTesting(newIndex0);
        elasticsearchAccess.deleteIndexForTesting(newIndexGrp);
        elasticsearchAccess.deleteIndexForTesting(indexAlias0);
        elasticsearchAccess.deleteIndexForTesting(newIndex0);
    }

    private long countDocumentsByQuery(
        ElasticsearchIndexAlias indexAlias0,
        VitamElasticsearchRepository vitamElasticsearchRepository,
        Query query
    ) throws DatabaseException {
        TotalHits total = vitamElasticsearchRepository.search(indexAlias0.getName(), query).hits().total();
        assertThat(total).isNotNull();
        return total.value();
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
            null,
            ElasticsearchTestHelper.loadElasticSearchSettings()
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
        GetAliasResponse aliasBeforeSwitch = elasticsearchAccess.getAlias(indexAlias);
        assertThat(aliasBeforeSwitch.aliases()).containsOnlyKeys(INDEX);

        // Switch indices
        SwitchIndexResult switchIndexResult = indexationHelper.switchIndex(indexAlias, newIndex, elasticsearchAccess);

        assertThat(switchIndexResult.getStatusCode()).isEqualTo(StatusCode.OK);

        // Check alias references
        GetAliasResponse aliasAfterSwitch = elasticsearchAccess.getAlias(indexAlias);
        assertThat(aliasAfterSwitch.aliases()).containsOnlyKeys(newIndex.getName());

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

        assertThat(countDocumentsByQuery(indexAlias, vitamElasticsearchRepository, matchAll())).isEqualTo(10);

        assertThat(
            countDocumentsByQuery(
                indexAlias,
                vitamElasticsearchRepository,
                termQuery("Identifier", "Identifier_No_Tenant")
            )
        ).isEqualTo(10);

        elasticsearchRule.purgeIndex(elasticsearchRule.getClient(), newIndex.getName());
    }

    @Test
    public void should_apply_date_filter_for_unit_and_objectgroup_collections() throws IOException, DatabaseException {
        // Given
        String alias = ALIAS + GUIDFactory.newGUID().getId();
        final MongoCollection<Document> collection = mongoRule.getMongoCollection(alias);
        ElasticsearchIndexAliasResolver indexAliasResolver = tenantId ->
            ElasticsearchIndexAlias.ofMultiTenantCollection(alias, tenantId);

        VitamMongoRepository vitamMongoRepository = new VitamMongoRepository(collection);

        LocalDateTime baseDate = LocalDateTime.of(2023, 1, 1, 0, 0, 0);

        String mapping = FileUtils.readFileToString(
            PropertiesUtils.findFile(TEST_ES_MAPPING_JSON),
            StandardCharsets.UTF_8
        );
        ElasticsearchIndexAlias indexAlias = indexAliasResolver.resolveIndexName(0);
        ElasticsearchIndexSettings indexSettings = new ElasticsearchIndexSettings(2, 1, () -> mapping);

        // Step 1: Create initial index and alias setup
        // Create an initial index with alias to simulate existing setup
        elasticsearchAccess.createIndexAndAliasIfAliasNotExists(
            indexAlias,
            indexSettings,
            ElasticsearchTestHelper.loadElasticSearchSettings()
        );
        elasticsearchAccess.refreshIndex(indexAlias);

        // Add a small delay to ensure different timestamps for index creation
        logicalClock.logicalSleep(1, ChronoUnit.SECONDS);

        // Step 2: Insert 5 units in MongoDB
        List<Document> firstBatch = new ArrayList<>();
        String thirdUnitDate = "";
        for (int i = 0; i < 5; i++) {
            String id = GUIDFactory.newGUID().toString();

            String audDateStr = LocalDateUtil.getFormattedDateTimeForMongo(baseDate.plusDays(i));
            if (i == 2) { // Save the date of the 3rd unit (index 2)
                thirdUnitDate = audDateStr;
            }
            ObjectNode data = JsonHandler.createObjectNode()
                .put("_id", id)
                .put("_tenant", 0)
                .put("Identifier", "First_Batch_" + i)
                .put("Name", "Document from first batch " + i)
                .put("_aud", audDateStr);
            firstBatch.add(Document.parse(JsonHandler.unprettyPrint(data)));
        }
        vitamMongoRepository.save(firstBatch);

        // Step 3: Launch full reindexation (without date)
        ReindexationOK fullReindexResult = indexationHelper.reindex(
            collection,
            elasticsearchAccess,
            indexAlias,
            indexSettings,
            ElasticsearchCollections.UNIT,
            singletonList(0),
            null,
            ElasticsearchTestHelper.loadElasticSearchSettings()
        );
        assertThat(fullReindexResult.getAliasName()).isNotEqualTo(fullReindexResult.getIndexName());
        // Switch to new index after full reindexation
        ElasticsearchIndexAlias fullReindexIndex = ElasticsearchIndexAlias.ofFullIndexName(
            fullReindexResult.getIndexName()
        );

        indexationHelper.switchIndex(indexAlias, fullReindexIndex, elasticsearchAccess);

        // Step 4: Verify that the total count is exactly 5 units from first batch
        VitamElasticsearchRepository vitamElasticsearchRepository = new VitamElasticsearchRepository(
            elasticsearchAccess.getClient(),
            indexAliasResolver
        );

        long firstBatchDocs = countDocumentsByQuery(
            indexAlias,
            vitamElasticsearchRepository,
            wildcard("Identifier", "First_Batch*")
        );
        assertThat(firstBatchDocs).isEqualTo(5);

        // Step 5: Insert 5 more units in MongoDB
        List<Document> secondBatch = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            String id = GUIDFactory.newGUID().toString();
            String audDateStr = LocalDateUtil.getFormattedDateTimeForMongo(baseDate.plusDays(10 + i)); // Different dates to distinguish
            ObjectNode data = JsonHandler.createObjectNode()
                .put("_id", id)
                .put("_tenant", 0)
                .put("Identifier", "Second_Batch_" + i)
                .put("Name", "Document from second batch " + i)
                .put("_aud", audDateStr);
            secondBatch.add(Document.parse(JsonHandler.unprettyPrint(data)));
        }
        vitamMongoRepository.save(secondBatch);

        // Step 6: Launch reindexation with date filter from 3rd unit before full indexation
        LocalDate filterDate = LocalDateUtil.parseMongoFormattedDate(thirdUnitDate).toLocalDate();
        ReindexationOK filteredReindexResult = indexationHelper.reindex(
            collection,
            elasticsearchAccess,
            indexAlias,
            indexSettings,
            ElasticsearchCollections.UNIT,
            singletonList(0),
            null,
            ElasticsearchTestHelper.loadElasticSearchSettings(),
            filterDate
        );
        assertThat(filteredReindexResult.getIndexName()).isEqualTo(filteredReindexResult.getAliasName());

        // For incremental reindexation with date filter on UNIT collection,
        // the existing index is used (no new index created), so no need to switch
        elasticsearchAccess.refreshIndex(indexAlias);

        long totalDocs = countDocumentsByQuery(indexAlias, vitamElasticsearchRepository, matchAll());
        assertThat(totalDocs).isEqualTo(10);

        // Verify we have documents from both batches
        firstBatchDocs = countDocumentsByQuery(
            indexAlias,
            vitamElasticsearchRepository,
            termQuery("Identifier", "First_Batch_4")
        );
        assertThat(firstBatchDocs).isEqualTo(1);

        long secondBatchDocs = countDocumentsByQuery(
            indexAlias,
            vitamElasticsearchRepository,
            termQuery("Identifier", "Second_Batch_0")
        );
        assertThat(secondBatchDocs).isEqualTo(1);

        // Clean up
        elasticsearchRule.purgeIndex(elasticsearchRule.getClient(), indexAlias.getName());
        elasticsearchAccess.deleteIndexByAliasForTesting(indexAlias);
        elasticsearchAccess.deleteIndexForTesting(fullReindexIndex);
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
    ) throws DatabaseException {
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
    ) throws DatabaseException {
        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String id = GUIDFactory.newGUID().toString();
            ObjectNode data = JsonHandler.createObjectNode()
                .put("_id", id)
                .put("_tenant", tenant)
                .put("Identifier", "Identifier_" + tenant)
                .put("Name", "Description_" + tenant + "_" + i + " " + RandomUtils.nextDouble());
            documents.add(Document.parse(JsonHandler.unprettyPrint(data)));
            ids.put(id, tenant);
        }
        vitamMongoRepository.save(documents);
        vitamElasticsearchRepository.save(documents);
    }

    private void insertDocuments(
        VitamMongoRepository vitamMongoRepository,
        VitamElasticsearchRepository vitamElasticsearchRepository,
        Map<String, Integer> ids
    ) throws DatabaseException {
        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String id = GUIDFactory.newGUID().toString();
            String value = "Identifier_No_Tenant";
            ObjectNode data = JsonHandler.createObjectNode()
                .put("_id", id)
                .put("Identifier", value)
                .put("Name", "Description_" + value + " " + RandomUtils.nextDouble());
            documents.add(Document.parse(JsonHandler.unprettyPrint(data)));
            ids.put(id, null);
        }
        vitamMongoRepository.save(documents);
        vitamElasticsearchRepository.save(documents);
    }

    @Test
    public void should_validate_unit_collection_with_indexation_start_date() {
        // Given
        IndexParameters indexParameters = new IndexParameters();
        indexParameters.setCollectionName("UNIT");
        indexParameters.setTenants(Arrays.asList(0, 1, 2));
        indexParameters.setIndexationStartDate(LocalDate.of(2023, 1, 1));

        List<IndexParameters> parametersList = singletonList(indexParameters);

        // When & Then - should not throw any exception
        indexationHelper.validateIndexationParameters(parametersList);
    }

    @Test
    public void should_validate_objectgroup_collection_with_indexation_start_date() {
        // Given
        IndexParameters indexParameters = new IndexParameters();
        indexParameters.setCollectionName("OBJECTGROUP");
        indexParameters.setTenants(Arrays.asList(0, 1, 2));
        indexParameters.setIndexationStartDate(LocalDate.of(2023, 1, 1));

        List<IndexParameters> parametersList = singletonList(indexParameters);

        // When & Then - should not throw any exception
        indexationHelper.validateIndexationParameters(parametersList);
    }

    @Test
    public void should_validate_unit_collection_with_lowercase_name_and_indexation_start_date() {
        // Given
        IndexParameters indexParameters = new IndexParameters();
        indexParameters.setCollectionName("unit");
        indexParameters.setTenants(Arrays.asList(0, 1, 2));
        indexParameters.setIndexationStartDate(LocalDate.of(2023, 1, 1));

        List<IndexParameters> parametersList = singletonList(indexParameters);

        // When & Then - should not throw any exception
        indexationHelper.validateIndexationParameters(parametersList);
    }

    @Test
    public void should_validate_objectgroup_collection_with_lowercase_name_and_indexation_start_date() {
        // Given
        IndexParameters indexParameters = new IndexParameters();
        indexParameters.setCollectionName("objectgroup");
        indexParameters.setTenants(Arrays.asList(0, 1, 2));
        indexParameters.setIndexationStartDate(LocalDate.of(2023, 1, 1));

        List<IndexParameters> parametersList = singletonList(indexParameters);

        // When & Then - should not throw any exception
        indexationHelper.validateIndexationParameters(parametersList);
    }

    @Test
    public void should_validate_any_collection_without_indexation_start_date() {
        // Given
        IndexParameters indexParameters1 = new IndexParameters();
        indexParameters1.setCollectionName("FORMATS");
        indexParameters1.setTenants(Arrays.asList(0, 1, 2));
        indexParameters1.setIndexationStartDate(null);

        IndexParameters indexParameters2 = new IndexParameters();
        indexParameters2.setCollectionName("RULES");
        indexParameters2.setTenants(Arrays.asList(0, 1, 2));
        indexParameters2.setIndexationStartDate(null);

        IndexParameters indexParameters3 = new IndexParameters();
        indexParameters3.setCollectionName("UNIT");
        indexParameters3.setTenants(Arrays.asList(0, 1, 2));
        indexParameters3.setIndexationStartDate(null);

        List<IndexParameters> parametersList = Arrays.asList(indexParameters1, indexParameters2, indexParameters3);

        // When & Then - should not throw any exception
        indexationHelper.validateIndexationParameters(parametersList);
    }

    @Test
    public void should_do_nothing() {
        // Given
        List<IndexParameters> parametersList = new ArrayList<>();

        // When & Then - should not throw any exception
        indexationHelper.validateIndexationParameters(parametersList);
    }

    @Test
    public void should_throw_exception_with_correct_message_for_invalid_collection() {
        // Given
        IndexParameters indexParameters = new IndexParameters();
        indexParameters.setCollectionName("FORMATS");
        indexParameters.setTenants(Arrays.asList(0, 1, 2));
        indexParameters.setIndexationStartDate(LocalDate.of(2023, 1, 1));

        List<IndexParameters> parametersList = singletonList(indexParameters);

        // When & Then
        try {
            indexationHelper.validateIndexationParameters(parametersList);
            assertThat(false).as("Expected IllegalStateException to be thrown").isTrue();
        } catch (IllegalStateException e) {
            assertThat(e.getMessage()).contains(
                "Indexation with start date is only allowed for UNIT and OBJECTGROUP collections"
            );
            assertThat(e.getMessage()).contains("FORMATS");
        }
    }

    @Test
    public void should_validate_mixed_valid_parameters() {
        // Given
        IndexParameters validUnitParams = new IndexParameters();
        validUnitParams.setCollectionName("UNIT");
        validUnitParams.setTenants(Arrays.asList(0, 1));
        validUnitParams.setIndexationStartDate(LocalDate.of(2023, 1, 1));

        IndexParameters validObjectGroupParams = new IndexParameters();
        validObjectGroupParams.setCollectionName("OBJECTGROUP");
        validObjectGroupParams.setTenants(Arrays.asList(2, 3));
        validObjectGroupParams.setIndexationStartDate(LocalDate.of(2023, 2, 1));

        IndexParameters validFormatsParams = new IndexParameters();
        validFormatsParams.setCollectionName("FORMATS");
        validFormatsParams.setTenants(Arrays.asList(4, 5));
        validFormatsParams.setIndexationStartDate(null);

        List<IndexParameters> parametersList = Arrays.asList(
            validUnitParams,
            validObjectGroupParams,
            validFormatsParams
        );

        // When & Then - should not throw any exception
        indexationHelper.validateIndexationParameters(parametersList);
    }

    @Test(expected = IllegalStateException.class)
    public void should_throw_exception_when_one_parameter_is_invalid_in_mixed_list() {
        // Given
        IndexParameters validUnitParams = new IndexParameters();
        validUnitParams.setCollectionName("UNIT");
        validUnitParams.setTenants(Arrays.asList(0, 1));
        validUnitParams.setIndexationStartDate(LocalDate.of(2023, 1, 1));

        IndexParameters invalidFormatsParams = new IndexParameters();
        invalidFormatsParams.setCollectionName("FORMATS");
        invalidFormatsParams.setTenants(Arrays.asList(2, 3));
        invalidFormatsParams.setIndexationStartDate(LocalDate.of(2023, 2, 1));

        List<IndexParameters> parametersList = Arrays.asList(validUnitParams, invalidFormatsParams);

        // When & Then - should throw IllegalStateException
        indexationHelper.validateIndexationParameters(parametersList);
    }
}
