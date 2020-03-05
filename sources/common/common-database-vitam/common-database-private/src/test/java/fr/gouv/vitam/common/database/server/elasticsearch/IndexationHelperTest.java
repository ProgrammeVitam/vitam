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
package fr.gouv.vitam.common.database.server.elasticsearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.client.MongoCollection;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.api.impl.VitamElasticsearchRepository;
import fr.gouv.vitam.common.database.api.impl.VitamMongoRepository;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.database.index.model.IndexOK;
import fr.gouv.vitam.common.database.index.model.IndexationResult;
import fr.gouv.vitam.common.database.parameter.IndexParameters;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.mongo.MongoRule;
import org.apache.commons.lang3.RandomUtils;
import org.assertj.core.util.Lists;
import org.bson.Document;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.GetAliasesResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public class IndexationHelperTest {
    private static final String INDEX = "index" + GUIDFactory.newGUID().getId();
    private static final String ALIAS = "alias" + GUIDFactory.newGUID().getId();

    private static final String TEST_ES_MAPPING_JSON = "test-es-mapping.json";
    private static final String INDEXATION_RESULT_WITHOUT_TENANT_JSON = "indexation_result_without_tenant.json";
    private static final String INDEXATION_RESULT_WITH_TENANT_JSON = "indexation_result_with_tenant.json";
    private static final String messageCause = "failed";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @ClassRule
    public static MongoRule mongoRule =
        new MongoRule(VitamCollection.getMongoClientOptions(Lists.newArrayList(AgenciesTest.class)),
            ALIAS);
    @ClassRule
    public static ElasticsearchRule elasticsearchRule = new ElasticsearchRule(ALIAS + "_0", ALIAS + "_1", ALIAS + "_2");

    private static ElasticsearchAccess elasticsearchAccess;
    private IndexationHelper indexationHelper = IndexationHelper.getInstance();

    @BeforeClass
    public static void setUp() throws Exception {
        ArrayList<ElasticsearchNode> esNodes =
            Lists.newArrayList(new ElasticsearchNode(ElasticsearchRule.getHost(), elasticsearchRule.getPort()));
        elasticsearchAccess = new ElasticsearchAccess(ElasticsearchRule.VITAM_CLUSTER, esNodes);
        elasticsearchRule.createIndex(ALIAS, INDEX, "{}");
        elasticsearchRule.createIndex(ALIAS + "_0", INDEX + "_0", "{}");
        elasticsearchRule.createIndex(ALIAS + "_1", INDEX + "_1", "{}");
        elasticsearchRule.createIndex(ALIAS + "_2", INDEX + "_2", "{}");

        elasticsearchRule.addIndexToBePurged(INDEX);
        elasticsearchRule.addIndexToBePurged(INDEX + "_0");
        elasticsearchRule.addIndexToBePurged(INDEX + "_1");
        elasticsearchRule.addIndexToBePurged(INDEX + "_2");
    }

    @AfterClass
    public static void afterClass() {
        mongoRule.handleAfterClass();
        elasticsearchRule.deleteIndexes();
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
        Map<String, Integer> mapIdsTenants = populating(collection, tenants);


        InputStream resourceAsStream = new FileInputStream(PropertiesUtils.findFile(TEST_ES_MAPPING_JSON));
        // When
        IndexationResult indexationResult =
            indexationHelper.reindex(collection, INDEX, elasticsearchAccess, tenants, resourceAsStream);

        assertThat(indexationResult.getIndexKO()).isNull();
        assertThat(indexationResult.getIndexOK()).hasSize(3);

        for (IndexOK index : indexationResult.getIndexOK()) {
            String[] split = index.getIndexName().split("_");
            VitamElasticsearchRepository vitamElasticsearchRepository =
                new VitamElasticsearchRepository(elasticsearchAccess.getClient(), index.getIndexName(), false);
            for (String id : mapIdsTenants.keySet()) {
                Integer tenant = mapIdsTenants.get(id);
                // check indexName if is the same that tenant.
                if (split[1].equals(tenant.toString())) {
                    Optional<Document> documentById = vitamElasticsearchRepository.getByID(id, tenant);
                    Document document = documentById.get();
                    assertThat(document.get("Name", String.class)).contains("Description_" + tenant);
                }
            }
            // Purge old index
            elasticsearchRule.deleteIndex(elasticsearchRule.getClient(), INDEX + "_" + index.getTenant());
        }
        assertThat(indexationResult.getCollectionName()).isEqualTo(INDEX);
        assertThat(indexationResult.getIndexOK().size()).isEqualTo(3);

        for (IndexOK indexOK : indexationResult.getIndexOK()) {
            assertThat(indexOK.getIndexName()).isNotNull();
            assertThat(indexOK.getTenant()).isNotNull();
            String aliasName = ALIAS + "_" + indexOK.getTenant();
            indexationHelper.switchIndex(aliasName, indexOK.getIndexName(), elasticsearchAccess);

            GetAliasesResponse actualAliases = elasticsearchRule.getClient().indices()
                .getAlias(new GetAliasesRequest().indices(INDEX + "_" + indexOK.getTenant(), indexOK.getIndexName()),
                    RequestOptions.DEFAULT);
            Collection<Set<AliasMetaData>> values = actualAliases.getAliases().values();
            assertThat(values).hasSize(2);
            for (Map.Entry<String, Set<AliasMetaData>> aliasMetaDataEntry : actualAliases.getAliases().entrySet()) {
                String index = aliasMetaDataEntry.getKey();
                Set<AliasMetaData> aliasMetaDataSet = aliasMetaDataEntry.getValue();

                // Old index does not have alias
                if ((INDEX + "_" + indexOK.getTenant()).equals(index)) {
                    assertThat(aliasMetaDataSet).hasSize(0);
                }

                if (indexOK.getIndexName().equals(index)) {
                    assertThat(aliasMetaDataSet).hasSize(1);
                    assertThat(aliasMetaDataSet.iterator().next().alias()).isEqualTo(ALIAS + "_" + indexOK.getTenant());
                }
            }


            VitamElasticsearchRepository vitamElasticsearchRepository =
                new VitamElasticsearchRepository(elasticsearchAccess.getClient(), aliasName, false);

            SearchResponse searchResponse = vitamElasticsearchRepository
                .search(aliasName, QueryBuilders.termQuery("Identifier", "Identifier_" + indexOK.getTenant()));
            assertThat(searchResponse.getHits().getTotalHits().value).isEqualTo(10);
        }
    }


    @Test
    public void should_reindex_and_switch_index_for_multiple_documents_without_tenant()
        throws IOException, DatabaseException {
        // Given
        final MongoCollection<Document> collection = mongoRule.getMongoCollection(ALIAS);
        List<Integer> tenants = new ArrayList<>();
        Map<String, Integer> mapIdsTenants = populating(collection, tenants);


        InputStream resourceAsStream = new FileInputStream(PropertiesUtils.findFile(TEST_ES_MAPPING_JSON));
        // When
        IndexationResult indexationResult =
            indexationHelper.reindex(collection, INDEX, elasticsearchAccess, tenants, resourceAsStream);

        assertThat(indexationResult.getIndexKO()).isNull();
        assertThat(indexationResult.getIndexOK()).hasSize(1);

        for (IndexOK index : indexationResult.getIndexOK()) {
            String[] split = index.getIndexName().split("_");
            VitamElasticsearchRepository vitamElasticsearchRepository =
                new VitamElasticsearchRepository(elasticsearchAccess.getClient(), index.getIndexName(), false);
            for (String id : mapIdsTenants.keySet()) {
                Optional<Document> documentById = vitamElasticsearchRepository.getByID(id, null);
                Document document = documentById.get();
                assertThat(document.get("Name", String.class)).contains("Identifier_No_Tenant");
            }
            // Purge old index
            elasticsearchRule.deleteIndex(elasticsearchRule.getClient(), INDEX);
        }
        assertThat(indexationResult.getCollectionName()).isEqualTo(INDEX);

        for (IndexOK indexOK : indexationResult.getIndexOK()) {
            assertThat(indexOK.getIndexName()).isNotNull();
            assertThat(indexOK.getTenant()).isNull();
            String aliasName = ALIAS;
            indexationHelper.switchIndex(aliasName, indexOK.getIndexName(), elasticsearchAccess);

            GetAliasesResponse actualAliases = elasticsearchRule.getClient().indices()
                .getAlias(new GetAliasesRequest().indices(INDEX, indexOK.getIndexName()),
                    RequestOptions.DEFAULT);
            Collection<Set<AliasMetaData>> values = actualAliases.getAliases().values();
            assertThat(values).hasSize(2);
            for (Map.Entry<String, Set<AliasMetaData>> aliasMetaDataEntry : actualAliases.getAliases().entrySet()) {
                String index = aliasMetaDataEntry.getKey();
                Set<AliasMetaData> aliasMetaDataSet = aliasMetaDataEntry.getValue();

                // Old index does not have alias
                if (INDEX.equals(index)) {
                    assertThat(aliasMetaDataSet).hasSize(0);
                }

                if (indexOK.getIndexName().equals(index)) {
                    assertThat(aliasMetaDataSet).hasSize(1);
                    assertThat(aliasMetaDataSet.iterator().next().alias()).isEqualTo(ALIAS);
                }
            }


            VitamElasticsearchRepository vitamElasticsearchRepository =
                new VitamElasticsearchRepository(elasticsearchAccess.getClient(), aliasName, false);

            SearchResponse searchResponse = vitamElasticsearchRepository
                .search(aliasName, QueryBuilders.termQuery("Identifier", "Identifier_No_Tenant"));
            assertThat(searchResponse.getHits().getTotalHits().value).isEqualTo(10);
        }
    }

    @Test
    public void should_retrieve_indexation_result_without_tenant() throws Exception {
        // Given
        final FileInputStream fileInputStream =
            new FileInputStream(PropertiesUtils.getResourceFile(INDEXATION_RESULT_WITHOUT_TENANT_JSON));

        final JsonNode indexationResultNode = JsonHandler.getFromInputStream(fileInputStream);
        final String indexationResultTest = indexationResultNode.toString();
        IndexParameters indexParameters = new IndexParameters();
        indexParameters.setCollectionName("collection_name");
        // When
        IndexationResult koResult = indexationHelper.getFullKOResult(indexParameters, messageCause);
        final String koResultToAssert = JsonHandler.unprettyPrint(koResult);
        // Then
        assertThat(koResultToAssert).isEqualTo(indexationResultTest);
    }



    @Test
    public void should_retrieve_indexation_result_with_tenant() throws Exception {
        // Given
        final FileInputStream fileInputStream =
            new FileInputStream(PropertiesUtils.getResourceFile(INDEXATION_RESULT_WITH_TENANT_JSON));

        final JsonNode indexationResultNode = JsonHandler.getFromInputStream(fileInputStream);
        final String indexationResultTest = indexationResultNode.toString();
        IndexParameters indexParameters = new IndexParameters();
        indexParameters.setCollectionName("collection_name");
        indexParameters.setTenants(Arrays.asList(1, 2));
        // When
        IndexationResult koResult = indexationHelper.getFullKOResult(indexParameters, messageCause);
        final String koResultToAssert = JsonHandler.unprettyPrint(koResult);
        // Then
        assertThat(koResultToAssert).isEqualTo(indexationResultTest);
    }



    private Map<String, Integer> populating(MongoCollection<Document> collection, List<Integer> tenants)
        throws IOException, DatabaseException {
        VitamMongoRepository vitamMongoRepository = new VitamMongoRepository(collection);
        VitamElasticsearchRepository vitamElasticsearchRepository;

        Map<String, Integer> ids = new HashMap<>();
        if (!tenants.isEmpty()) {
            for (Integer tenant : tenants) {
                vitamElasticsearchRepository =
                    new VitamElasticsearchRepository(elasticsearchRule.getClient(),
                        collection.getNamespace().getCollectionName().toLowerCase(), true);
                insertDocuments(vitamMongoRepository, vitamElasticsearchRepository, ids, tenant);
            }
        } else {
            vitamElasticsearchRepository =
                new VitamElasticsearchRepository(elasticsearchRule.getClient(),
                    collection.getNamespace().getCollectionName().toLowerCase(), false);
            insertDocuments(vitamMongoRepository, vitamElasticsearchRepository, ids);

        }
        return ids;
    }

    private void insertDocuments(VitamMongoRepository vitamMongoRepository,
        VitamElasticsearchRepository vitamElasticsearchRepository, Map<String, Integer> ids, Integer tenant)
        throws IOException, DatabaseException {
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

    private void insertDocuments(VitamMongoRepository vitamMongoRepository,
        VitamElasticsearchRepository vitamElasticsearchRepository, Map<String, Integer> ids)
        throws IOException, DatabaseException {
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

