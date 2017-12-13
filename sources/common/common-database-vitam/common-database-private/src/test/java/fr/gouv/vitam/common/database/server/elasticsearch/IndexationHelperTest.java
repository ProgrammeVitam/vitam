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

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.client.MongoCollection;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.api.impl.VitamElasticsearchRepository;
import fr.gouv.vitam.common.database.api.impl.VitamMongoRepository;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.database.index.model.IndexOK;
import fr.gouv.vitam.common.database.index.model.IndexationResult;
import fr.gouv.vitam.common.database.parameter.IndexParameters;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.mongo.MongoRule;
import org.apache.commons.lang3.RandomUtils;
import org.assertj.core.util.Lists;
import org.bson.Document;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.elasticsearch.cluster.metadata.AliasOrIndex;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public class IndexationHelperTest {
    private static final String AGENCIES = "AgenciesTest";
    private static final String VITAM_TEST = "vitam-test";
    private static final String AGENCIES_TEST_ES_MAPPING_JSON = "agenciesTest-es-mapping.json";
    private static final String TYPEUNIQUE = VitamCollection.getTypeunique();
    private static final String INDEXATION_RESULT_WITHOUT_TENANT_JSON = "indexation_result_without_tenant.json";
    private static final String INDEXATION_RESULT_WITH_TENANT_JSON = "indexation_result_with_tenant.json";
    private static final String messageCause = "failed";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static TransportClient client;

    @Rule
    public MongoRule mongoRule =
        new MongoRule(VitamCollection.getMongoClientOptions(Lists.newArrayList(AgenciesTest.class)),
            VITAM_TEST,
            AGENCIES);

    public static final String CLUSTER_NAME = "elasticsearch-data";
    private List<ElasticsearchNode> nodes;
    private ElasticsearchAccess elasticsearchAccess;
    private static JunitHelper.ElasticsearchTestConfiguration config;

    @Before
    public void setUp() throws Exception {
        int tcpPort = JunitHelper.getInstance().findAvailablePort();
        int httPort = JunitHelper.getInstance().findAvailablePort();
        config = JunitHelper.startElasticsearchForTest(temporaryFolder, CLUSTER_NAME, tcpPort, httPort);
        Settings settings = ElasticsearchAccess.getSettings(CLUSTER_NAME);
        client = new PreBuiltTransportClient(settings);
        client.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("localhost"), tcpPort));
        nodes = new ArrayList<>();
        nodes.add(new ElasticsearchNode("localhost", tcpPort));
        elasticsearchAccess = new ElasticsearchAccess(CLUSTER_NAME, nodes);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (config != null) {
            JunitHelper.stopElasticsearchForTest(config);
        }
    }



    @Test
    public void should_index_in_elasticsearch_multiple_mongo_documents_with_tenant() throws Exception {
        // Given
        final MongoCollection<Document> collection =
            mongoRule.getMongoCollection(AgenciesTest.class.getSimpleName());
        List<Integer> tenants = Arrays.asList(0, 1, 2);
        Map<String, Integer> mapIdsTenants = populatingMongoDatabase(collection, tenants);

        final InputStream resourceAsStream =
            new FileInputStream(PropertiesUtils.findFile(AGENCIES_TEST_ES_MAPPING_JSON));
        // When
        final IndexationResult indexationResult =
            IndexationHelper.reindex(collection, elasticsearchAccess, tenants, resourceAsStream);
        // Then
        final SortedMap<String, AliasOrIndex> aliasAndIndexLookup =
            client.admin().cluster().prepareState().execute().actionGet().getState().getMetaData()
                .getAliasAndIndexLookup();
        for (String key : aliasAndIndexLookup.keySet()) {

            VitamElasticsearchRepository vitamElasticsearchRepository =
                new VitamElasticsearchRepository(elasticsearchAccess.getClient(), key,
                    false);
            for (String id : mapIdsTenants.keySet()) {
                Integer integer = mapIdsTenants.get(id);
                String[] split = key.split("_");
                // check indexName if is the same that tenant.
                if (split[1].equals(id)) {
                    Optional<Document> documentById = vitamElasticsearchRepository.getByID(id, integer);
                    Document document = documentById.get();
                    assertThat(document.get("Name")).isNotNull();
                }
            }
        }
        assertThat(indexationResult.getCollectionName()).isEqualTo(collection.getDocumentClass().getSimpleName());
        assertThat(indexationResult.getIndexOK().size()).isEqualTo(3);
        for (IndexOK indexOK : indexationResult.getIndexOK()) {
            assertThat(indexOK.getIndexName()).isNotNull();
            assertThat(indexOK.getTenant()).isNotNull();
        }
    }

    private Map<String, Integer> populatingMongoDatabase(MongoCollection<Document> collection,
        List<Integer> tenants) throws IOException, DatabaseException {
        VitamMongoRepository repository =
            new VitamMongoRepository(collection);
        Map<String, Integer> ids = new HashMap<>();
        for (Integer tenant : tenants) {

            List<Document> documents = new ArrayList<>();

            for (int i = 0; i < 10; i++) {
                String id = GUIDFactory.newGUID().toString();
                XContentBuilder builder = jsonBuilder()
                    .startObject()
                    .field("_id", id)
                    .field("_tenant", tenant)
                    .field("Name", "Description" + i + " " + RandomUtils.nextDouble())
                    .endObject();
                documents.add(Document.parse(builder.string()));
                ids.put(id, tenant);
            }
            repository.save(documents);
        }
        return ids;
    }

    @Test
    public void should_index_elasticsearch_multiple_mongo_documents_without_tenant() throws Exception {
        // Given
        final MongoCollection<Document> collection =
            mongoRule.getMongoCollection(AgenciesTest.class.getSimpleName());
        List<Integer> tenants = Arrays.asList();
        Map<String, Integer> mapIdsTenants = populatingMongoDatabase(collection, tenants);

        final InputStream resourceAsStream =
            new FileInputStream(PropertiesUtils.findFile(AGENCIES_TEST_ES_MAPPING_JSON));
        // When
        final IndexationResult indexationResult =
            IndexationHelper.reindex(collection, elasticsearchAccess, tenants, resourceAsStream);
        // Then
        final SortedMap<String, AliasOrIndex> aliasAndIndexLookup =
            client.admin().cluster().prepareState().execute().actionGet().getState().getMetaData()
                .getAliasAndIndexLookup();
        for (String key : aliasAndIndexLookup.keySet()) {

            VitamElasticsearchRepository vitamElasticsearchRepository =
                new VitamElasticsearchRepository(elasticsearchAccess.getClient(), key,
                    false);
            for (String id : mapIdsTenants.keySet()) {
                Integer integer = mapIdsTenants.get(id);
                String[] split = key.split("_");
                // check indexName is the same that tenant.
                if (split[1].equals(id)) {
                    Optional<Document> documentById = vitamElasticsearchRepository.getByID(id, integer);
                    Document document = documentById.get();
                    assertThat(document.get("Name")).isNotNull();
                }
            }
        }
        // Assert IndexationReport
        assertThat(indexationResult.getCollectionName()).isEqualTo(collection.getDocumentClass().getSimpleName());
        assertThat(indexationResult.getIndexOK().size()).isEqualTo(1);
        for (IndexOK indexOK : indexationResult.getIndexOK()) {
            assertThat(indexOK.getIndexName()).isNotNull();
            assertThat(indexOK.getTenant()).isNull();
        }
    }

    @Test
    public void should_switch_elasticsearch_index() throws Exception {
        // Given
        final MongoCollection<Document> collection =
            mongoRule.getMongoCollection(AgenciesTest.class.getSimpleName());
        List<Integer> tenants = Arrays.asList(0, 1, 2);
        Map<String, Integer> mapIdsTenants = populatingMongoDatabase(collection, tenants);

        final InputStream resourceAsStream =
            new FileInputStream(PropertiesUtils.findFile(AGENCIES_TEST_ES_MAPPING_JSON));
        String mapping = ElasticsearchUtil
            .transferJsonToMapping(new FileInputStream(PropertiesUtils.findFile(AGENCIES_TEST_ES_MAPPING_JSON)));
        String aliasName = collection.getDocumentClass().getSimpleName().toLowerCase();
        elasticsearchAccess
            .createIndexAndAliasIfAliasNotExists(aliasName, mapping, TYPEUNIQUE,
                null);

        final IndexationResult indexationResult =
            IndexationHelper.reindex(collection, elasticsearchAccess, tenants, resourceAsStream);
        // When
        for (IndexOK indexOK : indexationResult.getIndexOK()) {
            final String indexName = indexOK.getIndexName();
            IndexationHelper.switchIndex(aliasName, indexName,
                elasticsearchAccess);
            final SortedMap<String, AliasOrIndex> aliasAndIndexLookup =
                client.admin().cluster().prepareState().execute().actionGet().getState().getMetaData()
                    .getAliasAndIndexLookup();
            // Then
            assertThat(aliasAndIndexLookup.size()).isEqualTo(5);
            for (String key : aliasAndIndexLookup.keySet()) {
                GetAliasesResponse actualAliases =
                    client.admin().indices().getAliases(new GetAliasesRequest().indices(key))
                        .actionGet();
                for (Iterator<List<AliasMetaData>> it = actualAliases.getAliases().valuesIt(); it.hasNext(); ) {
                    if (it.hasNext()) {
                        final List<AliasMetaData> next = it.next();
                        assertThat(next.size()).isEqualTo(1);
                        final String alias = next.get(0).alias();
                        assertThat(alias).isEqualTo(aliasName);
                    }
                }
            }
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
        IndexationResult koResult = IndexationHelper.getFullKOResult(indexParameters, messageCause);
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
        indexParameters.setTenants(Arrays.asList(1,2));
        // When
        IndexationResult koResult = IndexationHelper.getFullKOResult(indexParameters, messageCause);
        final String koResultToAssert = JsonHandler.unprettyPrint(koResult);
        // Then
        assertThat(koResultToAssert).isEqualTo(indexationResultTest);
    }
}
