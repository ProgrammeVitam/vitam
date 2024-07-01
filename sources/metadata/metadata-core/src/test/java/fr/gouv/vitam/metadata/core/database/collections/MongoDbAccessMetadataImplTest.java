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

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.api.VitamRepositoryFactory;
import fr.gouv.vitam.common.database.api.impl.VitamElasticsearchRepository;
import fr.gouv.vitam.common.database.api.impl.VitamMongoRepository;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.FacetBucket;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.config.ElasticsearchFunctionalAdminIndexManager;
import fr.gouv.vitam.functional.administration.common.server.AccessionRegisterSymbolic;
import fr.gouv.vitam.functional.administration.common.server.ElasticsearchAccessFunctionalAdmin;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollectionsTestUtils;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.model.ObjectGroupPerOriginatingAgency;
import fr.gouv.vitam.metadata.core.MetaDataImpl;
import fr.gouv.vitam.metadata.core.config.ElasticsearchMetadataIndexManager;
import fr.gouv.vitam.metadata.core.utils.MappingLoaderTestUtils;
import org.assertj.core.util.Lists;
import org.bson.Document;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.aggregations.metrics.ParsedSum;
import org.elasticsearch.search.aggregations.metrics.ParsedValueCount;
import org.elasticsearch.search.aggregations.metrics.SumAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.ValueCountAggregationBuilder;
import org.elasticsearch.xcontent.ContextParser;
import org.elasticsearch.xcontent.DeprecationHandler;
import org.elasticsearch.xcontent.NamedXContentRegistry;
import org.elasticsearch.xcontent.ParseField;
import org.elasticsearch.xcontent.json.JsonXContent;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static fr.gouv.vitam.functional.administration.common.server.AccessionRegisterSymbolic.ARCHIVE_UNIT;
import static fr.gouv.vitam.functional.administration.common.server.AccessionRegisterSymbolic.BINARY_OBJECT;
import static fr.gouv.vitam.functional.administration.common.server.AccessionRegisterSymbolic.BINARY_OBJECTS_SIZE;
import static fr.gouv.vitam.functional.administration.common.server.AccessionRegisterSymbolic.CREATION_DATE;
import static fr.gouv.vitam.functional.administration.common.server.AccessionRegisterSymbolic.OBJECT_GROUP;
import static fr.gouv.vitam.functional.administration.common.server.AccessionRegisterSymbolic.ORIGINATING_AGENCY;
import static fr.gouv.vitam.functional.administration.common.server.AccessionRegisterSymbolic.TENANT;
import static fr.gouv.vitam.metadata.core.database.collections.MetadataCollections.OBJECTGROUP;
import static fr.gouv.vitam.metadata.core.database.collections.MetadataCollections.UNIT;
import static java.util.Locale.US;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyListOf;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWithCustomExecutor
public class MongoDbAccessMetadataImplTest {

    @ClassRule
    public static RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    public static final String PREFIX = GUIDFactory.newGUID().getId();

    private static final String DEFAULT_MONGO1 = PREFIX + "AccessionRegisterDetail";
    private static final String DEFAULT_MONGO2 = PREFIX + "AccessionRegisterSummary";
    private static final String DEFAULT_MONGO3 = PREFIX + "Unit";
    private static final String DEFAULT_MONGO4 = PREFIX + "ObjectGroup";
    private static final String DEFAULT_MONGO5 = PREFIX + "Unit Document{{v=2, key=Document{{_id=1}}, name=_id_";
    private static final String DEFAULT_MONGO6 =
        PREFIX + "Unit Document{{v=2, key=Document{{_id=hashed}}, name=_id_hashed";
    private static final String DEFAULT_MONGO7 = PREFIX + "ObjectGroup Document{{v=2, key=Document{{_id=1}}, name=_id_";
    private static final String DEFAULT_MONGO8 =
        PREFIX + "ObjectGroup Document{{v=2, key=Document{{_id=hashed}}, name=_id_hashed";

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    @ClassRule
    public static MongoRule mongoRule = new MongoRule(
        MongoDbAccess.getMongoClientSettingsBuilder(Unit.class, ObjectGroup.class)
    );

    @ClassRule
    public static ElasticsearchRule elasticsearchRule = new ElasticsearchRule();

    static final List<Integer> tenantList = Arrays.asList(0);
    private static ElasticsearchAccessMetadata esClient;

    static MongoDbAccessMetadataImpl mongoDbAccess;

    private static final ElasticsearchMetadataIndexManager metadataIndexManager =
        MetadataCollectionsTestUtils.createTestIndexManager(
            tenantList,
            Collections.emptyMap(),
            MappingLoaderTestUtils.getTestMappingLoader()
        );
    private static final ElasticsearchFunctionalAdminIndexManager functionalAdminIndexManager =
        FunctionalAdminCollectionsTestUtils.createTestIndexManager();

    @BeforeClass
    public static void setupOne() throws Exception {
        List<ElasticsearchNode> esNodes = Lists.newArrayList(
            new ElasticsearchNode(ElasticsearchRule.getHost(), ElasticsearchRule.getPort())
        );

        esClient = new ElasticsearchAccessMetadata(ElasticsearchRule.getClusterName(), esNodes, metadataIndexManager);
        MetadataCollectionsTestUtils.beforeTestClass(mongoRule.getMongoDatabase(), PREFIX, esClient);
        FunctionalAdminCollectionsTestUtils.beforeTestClass(
            mongoRule.getMongoDatabase(),
            PREFIX,
            new ElasticsearchAccessFunctionalAdmin(
                ElasticsearchRule.VITAM_CLUSTER,
                esNodes,
                functionalAdminIndexManager
            )
        );
    }

    @AfterClass
    public static void tearDownAfterClass() {
        MetadataCollectionsTestUtils.afterTestClass(metadataIndexManager, true);
        FunctionalAdminCollectionsTestUtils.afterTestClass(true);
    }

    @After
    public void after() {
        MetadataCollectionsTestUtils.afterTest(metadataIndexManager);
        FunctionalAdminCollectionsTestUtils.afterTest();
    }

    @Test
    public void givenMongoDbAccessConstructorWhenCreateWithRecreateThenAddDefaultCollections() {
        mongoDbAccess = new MongoDbAccessMetadataImpl(
            mongoRule.getMongoClient(),
            mongoRule.getMongoDatabase().getName(),
            true,
            esClient,
            UNIT,
            OBJECTGROUP
        );
        assertThat(mongoDbAccess.getInfo())
            .contains(DEFAULT_MONGO1)
            .contains(DEFAULT_MONGO2)
            .contains(DEFAULT_MONGO3)
            .contains(DEFAULT_MONGO4)
            .contains(DEFAULT_MONGO5)
            .contains(DEFAULT_MONGO6)
            .contains(DEFAULT_MONGO7)
            .contains(DEFAULT_MONGO8);
        assertThat(UNIT.getName()).isEqualTo(PREFIX + "Unit");
        assertThat(OBJECTGROUP.getName()).isEqualTo(PREFIX + "ObjectGroup");
        assertThat(MongoDbAccessMetadataImpl.getUnitSize()).isEqualTo(0);
        assertThat(MongoDbAccessMetadataImpl.getObjectGroupSize()).isEqualTo(0);
    }

    @Test
    public void givenMongoDbAccessConstructorWhenCreateWithoutRecreateThenAddNothing() {
        mongoDbAccess = new MongoDbAccessMetadataImpl(
            mongoRule.getMongoClient(),
            mongoRule.getMongoDatabase().getName(),
            false,
            esClient,
            UNIT,
            OBJECTGROUP
        );
        assertThat(mongoDbAccess.getInfo())
            .contains(DEFAULT_MONGO1)
            .contains(DEFAULT_MONGO2)
            .contains(DEFAULT_MONGO3)
            .contains(DEFAULT_MONGO4)
            .contains(DEFAULT_MONGO5)
            .contains(DEFAULT_MONGO6)
            .contains(DEFAULT_MONGO7)
            .contains(DEFAULT_MONGO8);
    }

    @Test
    @RunWithCustomExecutor
    public void should_aggregate_unit_per_operation_id_and_originating_agency() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);

        mongoDbAccess = new MongoDbAccessMetadataImpl(
            mongoRule.getMongoClient(),
            mongoRule.getMongoDatabase().getName(),
            false,
            esClient,
            UNIT,
            OBJECTGROUP
        );

        // Given
        ElasticsearchMetadataIndexManager indexManager = mock(ElasticsearchMetadataIndexManager.class);
        final MetaDataImpl metaData = new MetaDataImpl(mongoDbAccess, 100, 300, 100, 300, 100, 300, indexManager);

        final String operationId = "1234";
        ArrayList<Document> units = Lists.newArrayList(
            new Document("_id", "1")
                .append("_tenant", 0)
                .append("_ops", Arrays.asList(operationId))
                .append("_opi", Arrays.asList(operationId))
                .append("_sp", "sp2")
                .append("_max", 1)
                .append("_sps", Arrays.asList("sp1", "sp2")),
            new Document("_id", "2")
                .append("_tenant", 0)
                .append("_ops", Arrays.asList(operationId))
                .append("_opi", Arrays.asList(operationId))
                .append("_sp", "sp1")
                .append("_max", 1)
                .append("_sps", Arrays.asList("sp1")),
            new Document("_id", "5")
                .append("_tenant", 0)
                .append("_ops", Arrays.asList(operationId))
                .append("_opi", Arrays.asList(operationId))
                .append("_sp", "sp1")
                .append("_max", 1)
                .append("_sps", Arrays.asList("sp1")),
            new Document("_id", "4")
                .append("_tenant", 0)
                .append("_ops", Arrays.asList(operationId))
                .append("_opi", Arrays.asList(operationId))
                .append("_sp", "sp1")
                .append("_max", 1)
                .append("_unitType", "HOLDING_UNIT")
                .append("_sps", Arrays.asList("sp1")),
            new Document("_id", "3")
                .append("_tenant", 0)
                .append("_ops", Arrays.asList("otherOperationId"))
                .append("_max", 1)
                .append("_sp", "sp1")
                .append("_opi", Arrays.asList("otherOperationId"))
                .append("_sps", Arrays.asList("sp2"))
        );

        VitamRepositoryFactory factory = VitamRepositoryFactory.get();
        VitamMongoRepository mongo = factory.getVitamMongoRepository(MetadataCollections.UNIT.getVitamCollection());
        mongo.save(units);

        VitamElasticsearchRepository es = VitamRepositoryFactory.get()
            .getVitamESRepository(
                MetadataCollections.UNIT.getVitamCollection(),
                metadataIndexManager.getElasticsearchIndexAliasResolver(UNIT)
            );
        es.save(units);

        // When
        List<FacetBucket> documents = metaData.selectOwnAccessionRegisterOnUnitByOperationId(operationId);

        // Then
        assertThat(documents).containsExactlyInAnyOrder(new FacetBucket("sp1", 2), new FacetBucket("sp2", 1));
    }

    @Test
    @RunWithCustomExecutor
    public void should_aggregate_object_group_per_operation_id_and_originating_agency_scenario() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);

        mongoDbAccess = new MongoDbAccessMetadataImpl(
            mongoRule.getMongoClient(),
            mongoRule.getMongoDatabase().getName(),
            false,
            esClient,
            UNIT,
            OBJECTGROUP
        );

        // Given
        ElasticsearchMetadataIndexManager indexManager = mock(ElasticsearchMetadataIndexManager.class);
        final MetaDataImpl metaData = new MetaDataImpl(mongoDbAccess, 100, 300, 100, 300, 100, 300, indexManager);
        initGotsForAccessionRegisterTest(
            "/got_1_sp1.json",
            "/got_2_sp1.json",
            "/got_3_sp2.json",
            "/got_4_sp1_sp2.json"
        );

        // When
        final String operationId1 = "opi1aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        List<ObjectGroupPerOriginatingAgency> documents1 =
            metaData.selectOwnAccessionRegisterOnObjectGroupByOperationId(0, operationId1);
        final String operationId4 = "opi4aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        List<ObjectGroupPerOriginatingAgency> documents4 =
            metaData.selectOwnAccessionRegisterOnObjectGroupByOperationId(0, operationId4);
        final String operationId5 = "opi5aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        List<ObjectGroupPerOriginatingAgency> documents5 =
            metaData.selectOwnAccessionRegisterOnObjectGroupByOperationId(0, operationId5);
        // Then
        assertThat(documents1)
            .extracting("operation", "agency", "numberOfObject", "numberOfGOT", "size")
            .contains(tuple("opi1aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "sp1", 3L, 2l, 200l));
        assertThat(documents4)
            .extracting("operation", "agency", "numberOfObject", "numberOfGOT", "size")
            .contains(
                tuple("opi4aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "sp1", 2L, 0l, 200l),
                tuple("opi4aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "sp2", 1L, 0l, 100l)
            );
        assertThat(documents5).isEmpty();
    }

    @Test
    @RunWithCustomExecutor
    public void should_aggregate_object_group_per_operation_id_and_originating_agency() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);

        mongoDbAccess = new MongoDbAccessMetadataImpl(
            mongoRule.getMongoClient(),
            mongoRule.getMongoDatabase().getName(),
            false,
            esClient,
            UNIT,
            OBJECTGROUP
        );

        // Given
        ElasticsearchMetadataIndexManager indexManager = mock(ElasticsearchMetadataIndexManager.class);
        final MetaDataImpl metaData = new MetaDataImpl(mongoDbAccess, 100, 300, 100, 300, 100, 300, indexManager);
        final String operationId = "aedqaaaaacgbcaacaar3kak4tr2o3wqaaaaq";
        initGotsForAccessionRegisterTest(
            "/object_sp1_1.json",
            "/object_sp1_sp2_2.json",
            "/object_sp2.json",
            "/object_sp2_4.json",
            "/object_other_operation_id.json"
        );

        // When
        List<ObjectGroupPerOriginatingAgency> documents = metaData.selectOwnAccessionRegisterOnObjectGroupByOperationId(
            0,
            operationId
        );

        // Then
        assertThat(documents)
            .extracting("operation", "agency", "numberOfObject", "numberOfGOT", "size")
            .contains(
                tuple("aedqaaaaacgbcaacaar3kak4tr2o3wqaaaaq", "sp1", 3L, 1l, 200l),
                tuple("aedqaaaaacgbcaacaar3kak4tr2o3wqaaaaq", "sp2", 7l, 3l, 480l)
            );
    }

    private void initGotsForAccessionRegisterTest(String... files) throws DatabaseException {
        List<Document> objectGroups = Arrays.asList(files)
            .stream()
            .map(file -> {
                try {
                    return (Document) (new ObjectGroup(
                            JsonHandler.getFromInputStream(getClass().getResourceAsStream(file))
                        ));
                } catch (InvalidParseOperationException e) {
                    throw new RuntimeException(e);
                }
            })
            .collect(Collectors.toList());

        VitamRepositoryFactory factory = VitamRepositoryFactory.get();
        VitamMongoRepository mongo = factory.getVitamMongoRepository(OBJECTGROUP.getVitamCollection());
        mongo.save(objectGroups);

        VitamElasticsearchRepository es = VitamRepositoryFactory.get()
            .getVitamESRepository(
                OBJECTGROUP.getVitamCollection(),
                metadataIndexManager.getElasticsearchIndexAliasResolver(OBJECTGROUP)
            );
        es.save(objectGroups);
    }

    @Test
    public void should_select_accession_register_symbolic() throws Exception {
        // Given
        ElasticsearchAccessMetadata client = mock(ElasticsearchAccessMetadata.class);
        when(client.getClient()).thenReturn(esClient.getClient());

        SearchResponse archiveUnitResponse = searchResult(
            PropertiesUtils.getResourceAsString("accession_register_symbolic_au_aggs_1.data")
        );
        SearchResponse objectGroupResponse = searchResult(
            PropertiesUtils.getResourceAsString("accession_register_symbolic_got_aggs_1.data")
        );

        given(
            client.basicAggregationSearch(eq(UNIT), eq(0), anyListOf(AggregationBuilder.class), any(QueryBuilder.class))
        ).willReturn(archiveUnitResponse.getAggregations());
        given(
            client.basicAggregationSearch(
                eq(OBJECTGROUP),
                eq(0),
                anyListOf(AggregationBuilder.class),
                any(QueryBuilder.class)
            )
        ).willReturn(objectGroupResponse.getAggregations());

        ElasticsearchMetadataIndexManager indexManager = mock(ElasticsearchMetadataIndexManager.class);
        final MetaDataImpl metaData = new MetaDataImpl(
            new MongoDbAccessMetadataImpl(
                mongoRule.getMongoClient(),
                mongoRule.getMongoDatabase().getName(),
                true,
                client,
                UNIT,
                OBJECTGROUP
            ),
            100,
            300,
            100,
            300,
            100,
            300,
            indexManager
        );

        // When
        List<Document> accessionRegisterSymbolics = metaData.createAccessionRegisterSymbolic(0);

        // Then
        assertThat(accessionRegisterSymbolics).hasSize(1);
    }

    @Test
    public void should_fill_all_accession_register_symbolic_information()
        throws IOException, MetaDataExecutionException {
        // Given
        ElasticsearchAccessMetadata client = mock(ElasticsearchAccessMetadata.class);
        when(client.getClient()).thenReturn(esClient.getClient());

        SearchResponse archiveUnitResponse = searchResult(
            PropertiesUtils.getResourceAsString("accession_register_symbolic_au_aggs_2.data")
        );
        SearchResponse objectGroupResponse = searchResult(
            PropertiesUtils.getResourceAsString("accession_register_symbolic_got_aggs_2.data")
        );

        given(
            client.basicAggregationSearch(eq(UNIT), eq(0), anyListOf(AggregationBuilder.class), any(QueryBuilder.class))
        ).willReturn(archiveUnitResponse.getAggregations());
        given(
            client.basicAggregationSearch(
                eq(OBJECTGROUP),
                eq(0),
                anyListOf(AggregationBuilder.class),
                any(QueryBuilder.class)
            )
        ).willReturn(objectGroupResponse.getAggregations());

        ElasticsearchMetadataIndexManager indexManager = mock(ElasticsearchMetadataIndexManager.class);
        final MetaDataImpl metaData = new MetaDataImpl(
            new MongoDbAccessMetadataImpl(
                mongoRule.getMongoClient(),
                mongoRule.getMongoDatabase().getName(),
                true,
                client,
                UNIT,
                OBJECTGROUP
            ),
            100,
            300,
            100,
            300,
            100,
            300,
            indexManager
        );

        // When
        Optional<AccessionRegisterSymbolic> first = metaData
            .createAccessionRegisterSymbolic(0)
            .stream()
            .map(a -> (AccessionRegisterSymbolic) a)
            .findFirst();

        // Then
        assertThat(first).map(a -> a.getInteger(TENANT)).hasValue(0);
        assertThat(first).map(a -> a.getString(ORIGINATING_AGENCY)).hasValue("Identifier0");
        assertThat(first).map(a -> a.getDouble(BINARY_OBJECTS_SIZE)).hasValue(88209.0);
        assertThat(first).map(a -> a.getLong(ARCHIVE_UNIT)).hasValue(2L);
        assertThat(first).map(a -> a.getLong(OBJECT_GROUP)).hasValue(3L);
        assertThat(first).map(a -> a.getLong(BINARY_OBJECT)).hasValue(2L);
        assertThat(first).map(a -> a.getString(CREATION_DATE)).isNotEmpty();
    }

    @Test
    public void should_subtracts_sp_count_to_sis_in_order_to_have_number_of_symbolic_link()
        throws IOException, InvalidParseOperationException, MetaDataExecutionException {
        // Given
        ElasticsearchAccessMetadata client = mock(ElasticsearchAccessMetadata.class);
        when(client.getClient()).thenReturn(esClient.getClient());

        long numberOfOriginatingAgencies = 12;
        long numberOfOriginatingAgency = 1;
        SearchResponse archiveUnitResponse = searchResult(
            String.format(
                PropertiesUtils.getResourceAsString("accession_register_symbolic_au_aggs_3.data"),
                numberOfOriginatingAgencies,
                numberOfOriginatingAgency
            )
        );
        SearchResponse objectGroupResponse = searchResult(
            PropertiesUtils.getResourceAsString("accession_register_symbolic_got_aggs_3.data")
        );

        given(
            client.basicAggregationSearch(eq(UNIT), eq(0), anyListOf(AggregationBuilder.class), any(QueryBuilder.class))
        ).willReturn(archiveUnitResponse.getAggregations());
        given(
            client.basicAggregationSearch(
                eq(OBJECTGROUP),
                eq(0),
                anyListOf(AggregationBuilder.class),
                any(QueryBuilder.class)
            )
        ).willReturn(objectGroupResponse.getAggregations());

        ElasticsearchMetadataIndexManager indexManager = mock(ElasticsearchMetadataIndexManager.class);
        final MetaDataImpl metaData = new MetaDataImpl(
            new MongoDbAccessMetadataImpl(
                mongoRule.getMongoClient(),
                mongoRule.getMongoDatabase().getName(),
                true,
                client,
                UNIT,
                OBJECTGROUP
            ),
            100,
            300,
            100,
            300,
            100,
            300,
            indexManager
        );

        // When
        Optional<AccessionRegisterSymbolic> first = metaData
            .createAccessionRegisterSymbolic(0)
            .stream()
            .map(a -> (AccessionRegisterSymbolic) a)
            .findFirst();

        // Then
        assertThat(first)
            .map(a -> a.getLong(ARCHIVE_UNIT))
            .hasValue(numberOfOriginatingAgencies - numberOfOriginatingAgency);
    }

    @Test
    public void should_add_number_of_binaries_and_binaries_total_size_to_related_accession_register_when_object_group_counted()
        throws IOException, MetaDataExecutionException {
        // Given
        ElasticsearchAccessMetadata client = mock(ElasticsearchAccessMetadata.class);
        when(client.getClient()).thenReturn(esClient.getClient());

        SearchResponse archiveUnitResponse = searchResult(
            String.format(PropertiesUtils.getResourceAsString("accession_register_symbolic_au_aggs_4.data"))
        );
        double binarySize = 88209;
        long binaryCount = 2;
        long objectGroupCountAll = 3;
        long objectGroupCountThis = 1;

        SearchResponse objectGroupResponse = searchResult(
            String.format(
                US,
                PropertiesUtils.getResourceAsString("accession_register_symbolic_got_aggs_4.data"),
                objectGroupCountAll,
                binaryCount,
                binarySize,
                objectGroupCountThis,
                binaryCount,
                binarySize
            )
        );

        given(
            client.basicAggregationSearch(eq(UNIT), eq(0), anyListOf(AggregationBuilder.class), any(QueryBuilder.class))
        ).willReturn(archiveUnitResponse.getAggregations());
        given(
            client.basicAggregationSearch(
                eq(OBJECTGROUP),
                eq(0),
                anyListOf(AggregationBuilder.class),
                any(QueryBuilder.class)
            )
        ).willReturn(objectGroupResponse.getAggregations());

        ElasticsearchMetadataIndexManager indexManager = mock(ElasticsearchMetadataIndexManager.class);
        final MetaDataImpl metaData = new MetaDataImpl(
            new MongoDbAccessMetadataImpl(
                mongoRule.getMongoClient(),
                mongoRule.getMongoDatabase().getName(),
                true,
                client,
                UNIT,
                OBJECTGROUP
            ),
            100,
            300,
            100,
            300,
            100,
            300,
            indexManager
        );

        // When
        Optional<AccessionRegisterSymbolic> first = metaData
            .createAccessionRegisterSymbolic(0)
            .stream()
            .map(a -> (AccessionRegisterSymbolic) a)
            .findFirst();

        // Then
        assertThat(first).map(a -> a.getDouble(BINARY_OBJECTS_SIZE)).hasValue(binarySize);
        assertThat(first).map(a -> a.getLong(OBJECT_GROUP)).hasValue(3L);
        assertThat(first).map(a -> a.getLong(BINARY_OBJECT)).hasValue(objectGroupCountAll - objectGroupCountThis);
    }

    @Test
    public void should_add_zero_binaries_and_zero_binaries_total_size_to_related_accession_register_when_object_group_NOT_counted()
        throws IOException, MetaDataExecutionException {
        // Given
        ElasticsearchAccessMetadata client = mock(ElasticsearchAccessMetadata.class);
        when(client.getClient()).thenReturn(esClient.getClient());

        SearchResponse archiveUnitResponse = searchResult(
            PropertiesUtils.getResourceAsString("accession_register_symbolic_au_aggs_5.data")
        );
        double binarySize = 0;
        long binaryCount = 0;
        long objectGroupCountAll = 1;
        long objectGroupCountThis = 1;

        SearchResponse objectGroupResponse = searchResult(
            String.format(
                US,
                PropertiesUtils.getResourceAsString("accession_register_symbolic_got_aggs_5.data"),
                objectGroupCountAll,
                binaryCount,
                binarySize,
                objectGroupCountThis,
                binaryCount,
                binarySize
            )
        );

        given(
            client.basicAggregationSearch(eq(UNIT), eq(0), anyListOf(AggregationBuilder.class), any(QueryBuilder.class))
        ).willReturn(archiveUnitResponse.getAggregations());
        given(
            client.basicAggregationSearch(
                eq(OBJECTGROUP),
                eq(0),
                anyListOf(AggregationBuilder.class),
                any(QueryBuilder.class)
            )
        ).willReturn(objectGroupResponse.getAggregations());

        ElasticsearchMetadataIndexManager indexManager = mock(ElasticsearchMetadataIndexManager.class);
        final MetaDataImpl metaData = new MetaDataImpl(
            new MongoDbAccessMetadataImpl(
                mongoRule.getMongoClient(),
                mongoRule.getMongoDatabase().getName(),
                true,
                client,
                UNIT,
                OBJECTGROUP
            ),
            100,
            300,
            100,
            300,
            100,
            300,
            indexManager
        );

        // When
        Optional<AccessionRegisterSymbolic> first = metaData
            .createAccessionRegisterSymbolic(0)
            .stream()
            .map(a -> (AccessionRegisterSymbolic) a)
            .findFirst();

        // Then
        assertThat(first).map(a -> a.getDouble(BINARY_OBJECTS_SIZE)).hasValue(0D);
        assertThat(first).map(a -> a.getLong(OBJECT_GROUP)).hasValue(0L);
        assertThat(first).map(a -> a.getLong(BINARY_OBJECT)).hasValue(0L);
    }

    @Test
    public void should_NOT_created_new_accession_register_with_object_group_information_when_no_related_accession_register()
        throws IOException, MetaDataExecutionException {
        // Given
        ElasticsearchAccessMetadata client = mock(ElasticsearchAccessMetadata.class);
        when(client.getClient()).thenReturn(esClient.getClient());

        SearchResponse archiveUnitResponse = searchResult(
            PropertiesUtils.getResourceAsString("accession_register_symbolic_au_aggs_6.data")
        );
        SearchResponse objectGroupResponse = searchResult(
            PropertiesUtils.getResourceAsString("accession_register_symbolic_got_aggs_6.data")
        );

        given(
            client.basicAggregationSearch(eq(UNIT), eq(0), anyListOf(AggregationBuilder.class), any(QueryBuilder.class))
        ).willReturn(archiveUnitResponse.getAggregations());
        given(
            client.basicAggregationSearch(
                eq(OBJECTGROUP),
                eq(0),
                anyListOf(AggregationBuilder.class),
                any(QueryBuilder.class)
            )
        ).willReturn(objectGroupResponse.getAggregations());

        ElasticsearchMetadataIndexManager indexManager = mock(ElasticsearchMetadataIndexManager.class);
        final MetaDataImpl metaData = new MetaDataImpl(
            new MongoDbAccessMetadataImpl(
                mongoRule.getMongoClient(),
                mongoRule.getMongoDatabase().getName(),
                true,
                client,
                UNIT,
                OBJECTGROUP
            ),
            100,
            300,
            100,
            300,
            100,
            300,
            indexManager
        );

        // When
        Optional<AccessionRegisterSymbolic> first = metaData
            .createAccessionRegisterSymbolic(0)
            .stream()
            .map(a -> (AccessionRegisterSymbolic) a)
            .findFirst();

        // Then
        assertThat(first).isEmpty();
    }

    private List<NamedXContentRegistry.Entry> getDefaultNamedXContents() {
        Map<String, ContextParser<Object, ? extends Aggregation>> map = new HashMap<>();

        map.put(StringTerms.NAME, (p, c) -> ParsedStringTerms.fromXContent(p, (String) c));
        map.put(SumAggregationBuilder.NAME, (p, c) -> ParsedSum.fromXContent(p, (String) c));
        map.put(ValueCountAggregationBuilder.NAME, (p, c) -> ParsedValueCount.fromXContent(p, (String) c));
        map.put(NestedAggregationBuilder.NAME, (p, c) -> ParsedNested.fromXContent(p, (String) c));

        return map
            .entrySet()
            .stream()
            .map(
                entry ->
                    new NamedXContentRegistry.Entry(Aggregation.class, new ParseField(entry.getKey()), entry.getValue())
            )
            .collect(Collectors.toList());
    }

    private SearchResponse searchResult(String content) throws IOException {
        NamedXContentRegistry registry = new NamedXContentRegistry(getDefaultNamedXContents());
        return SearchResponse.fromXContent(
            JsonXContent.jsonXContent.createParser(registry, DeprecationHandler.THROW_UNSUPPORTED_OPERATION, content)
        );
    }
}
