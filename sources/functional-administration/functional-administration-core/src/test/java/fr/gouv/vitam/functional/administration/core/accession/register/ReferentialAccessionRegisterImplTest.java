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
package fr.gouv.vitam.functional.administration.core.accession.register;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.client.MongoCollection;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.server.DbRequestResult;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.DocumentAlreadyExistsException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.AccessionRegisterDetailModel;
import fr.gouv.vitam.common.model.administration.AccessionRegisterStatus;
import fr.gouv.vitam.common.model.administration.RegisterValueDetailModel;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterDetail;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterSummary;
import fr.gouv.vitam.functional.administration.common.config.ElasticsearchFunctionalAdminIndexManager;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.AccessionRegisterSymbolic;
import fr.gouv.vitam.functional.administration.common.server.ElasticsearchAccessFunctionalAdmin;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollectionsTestUtils;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminFactory;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.core.backup.FunctionalBackupService;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import org.bson.Document;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ReferentialAccessionRegisterImplTest {
    public static final String PREFIX = GUIDFactory.newGUID().getId();
    private static final int ACCESSION_REGISTER_SYMBOLIC_THREAD_POOL_SIZE = 4;
    private static final String ACCESSION_REGISTER_DETAIL = "accession-register_detail.json";
    private static final String ACCESSION_REGISTER_DETAIL_ELIMINATION = "accession-register_detail_elimination.json";
    private static final String ACCESSION_REGISTER_DETAIL_ELIMINATION_2 =
        "accession-register_detail_elimination_2.json";
    private static final String FILE_TO_TEST_OK = "accession-register.json";
    private static final String FILE_TO_TEST_2_OK = "accession-register_2.json";
    private static final Integer TENANT_ID = 0;
    private static final ElasticsearchFunctionalAdminIndexManager indexManager =
        FunctionalAdminCollectionsTestUtils.createTestIndexManager();
    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();
    @ClassRule
    public static MongoRule mongoRule = new MongoRule(
        MongoDbAccess.getMongoClientSettingsBuilder(AccessionRegisterDetail.class, AccessionRegisterSummary.class));

    @ClassRule
    public static ElasticsearchRule elasticsearchRule = new ElasticsearchRule();
    static ReferentialAccessionRegisterImpl accessionRegisterImpl;
    static AccessionRegisterDetailModel accessionRegisterDetailModel;
    private static ElasticsearchAccessFunctionalAdmin esClient;
    private static MongoDbAccessAdminImpl mongoDbAccessAdmin;
    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private MetaDataClientFactory metaDataClientFactory;

    @Mock
    private MetaDataClient metaDataClient;

    @Mock
    private FunctionalBackupService functionalBackupService;

    @BeforeClass
    @RunWithCustomExecutor
    public static void setUpBeforeClass() throws Exception {

        final List<ElasticsearchNode> esNodes = new ArrayList<>();
        esNodes.add(new ElasticsearchNode(ElasticsearchRule.getHost(), ElasticsearchRule.getPort()));
        esClient = new ElasticsearchAccessFunctionalAdmin(ElasticsearchRule.VITAM_CLUSTER, esNodes, indexManager);

        final List<MongoDbNode> nodes = new ArrayList<>();
        nodes.add(new MongoDbNode("localhost", MongoRule.getDataBasePort()));
        mongoDbAccessAdmin =
            MongoDbAccessAdminFactory
                .create(new DbConfigurationImpl(nodes, mongoRule.getMongoDatabase().getName()), Collections::emptyList,
                    indexManager);

        FunctionalAdminCollectionsTestUtils.beforeTestClass(mongoRule.getMongoDatabase(), PREFIX,
            esClient,
            Arrays.asList(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL,
                FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY));
    }

    @AfterClass
    public static void tearDownAfterClass() {
        FunctionalAdminCollectionsTestUtils.afterTestClass(true);
    }

    @Before
    public void setup() {

        doReturn(metaDataClient).when(metaDataClientFactory).getClient();

        accessionRegisterImpl = new ReferentialAccessionRegisterImpl(mongoDbAccessAdmin,
            functionalBackupService, metaDataClientFactory,
            ACCESSION_REGISTER_SYMBOLIC_THREAD_POOL_SIZE);
    }

    @After
    public void afterTest() {
        FunctionalAdminCollectionsTestUtils.afterTest();
    }


    @Test
    @RunWithCustomExecutor
    public void createOrUpdateAccessionRegister_idempotence() throws Exception {

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        ElasticsearchAccessFunctionalAdmin.ensureIndex();

        AccessionRegisterDetailModel ardm =
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(ACCESSION_REGISTER_DETAIL),
                AccessionRegisterDetailModel.class);

        accessionRegisterImpl.createOrUpdateAccessionRegister(ardm);

        // Test idempotence of ingest
        ardm.setId(GUIDFactory.newGUID().getId());
        accessionRegisterImpl.createOrUpdateAccessionRegister(ardm);

        Select select = new Select();
        select.setQuery(QueryHelper.eq("OriginatingAgency", "OG_1"));
        RequestResponseOK<AccessionRegisterSummary> response =
            accessionRegisterImpl.findDocuments(select.getFinalSelect());
        assertThat(response.isOk()).isTrue();
        assertThat(response.getResults()).hasSize(1);
        AccessionRegisterSummary summary = response.getResults().iterator().next();
        assertThat(summary.getTotalUnits().getIngested()).isEqualTo(1000);
        assertThat(summary.getTotalUnits().getDeleted()).isEqualTo(0);
        assertThat(summary.getTotalUnits().getRemained()).isEqualTo(1000);

        assertThat(summary.getTotalObjectGroups().getIngested()).isEqualTo(1000);
        assertThat(summary.getTotalObjectGroups().getDeleted()).isEqualTo(0);
        assertThat(summary.getTotalObjectGroups().getRemained()).isEqualTo(1000);

        assertThat(summary.getTotalObjects().getIngested()).isEqualTo(1000);
        assertThat(summary.getTotalObjects().getDeleted()).isEqualTo(0);
        assertThat(summary.getTotalObjects().getRemained()).isEqualTo(1000);

        assertThat(summary.getTotalObjectSize().getIngested()).isEqualTo(9999);
        assertThat(summary.getTotalObjectSize().getDeleted()).isEqualTo(0);
        assertThat(summary.getTotalObjectSize().getRemained()).isEqualTo(9999);

        select = new Select();
        select.setQuery(
            QueryHelper.and().add(QueryHelper.eq("Opi", "Opi_1"), QueryHelper.eq("OriginatingAgency", "OG_1")));

        RequestResponseOK<AccessionRegisterDetail> detailResponse =
            accessionRegisterImpl.findDetail(select.getFinalSelect());

        assertThat(detailResponse.isOk()).isTrue();
        assertThat(detailResponse.getResults()).hasSize(1);
        AccessionRegisterDetail detail = detailResponse.getResults().iterator().next();
        assertThat(detail.getTotalUnits().getIngested()).isEqualTo(1000);
        assertThat(detail.getTotalUnits().getDeleted()).isEqualTo(0);
        assertThat(detail.getTotalUnits().getRemained()).isEqualTo(1000);

        assertThat(detail.getTotalObjectGroups().getIngested()).isEqualTo(1000);
        assertThat(detail.getTotalObjectGroups().getDeleted()).isEqualTo(0);
        assertThat(detail.getTotalObjectGroups().getRemained()).isEqualTo(1000);

        assertThat(detail.getTotalObjects().getIngested()).isEqualTo(1000);
        assertThat(detail.getTotalObjects().getDeleted()).isEqualTo(0);
        assertThat(detail.getTotalObjects().getRemained()).isEqualTo(1000);

        assertThat(detail.getTotalObjectSize().getIngested()).isEqualTo(9999);
        assertThat(detail.getTotalObjectSize().getDeleted()).isEqualTo(0);
        assertThat(detail.getTotalObjectSize().getRemained()).isEqualTo(9999);

        assertThat(detail.get(AccessionRegisterDetail.EVENTS, List.class)).hasSize(1);

        // Add elimination event 1
        ardm =
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(ACCESSION_REGISTER_DETAIL_ELIMINATION),
                AccessionRegisterDetailModel.class);

        accessionRegisterImpl.createOrUpdateAccessionRegister(ardm);

        select = new Select();
        select.setQuery(QueryHelper.eq("OriginatingAgency", "OG_1"));
        detailResponse = accessionRegisterImpl.findDetail(select.getFinalSelect());
        assertThat(detailResponse.isOk()).isTrue();
        assertThat(detailResponse.getResults()).hasSize(1);
        AccessionRegisterDetail accessionRegisterDetailBeforeUpdateResult = detailResponse.getResults().get(0);
        assertEquals(accessionRegisterDetailBeforeUpdateResult.getStatus(), AccessionRegisterStatus.STORED_AND_UPDATED);
        assertThat(accessionRegisterDetailBeforeUpdateResult.getEvents()).hasSize(2);
        assertEquals(accessionRegisterDetailBeforeUpdateResult.getStatus(), AccessionRegisterStatus.STORED_AND_UPDATED);

        // Test idempotence of ingest
        ardm.setId(GUIDFactory.newGUID().getId());
        accessionRegisterImpl.createOrUpdateAccessionRegister(ardm);

        select = new Select();
        select.setQuery(QueryHelper.eq("OriginatingAgency", "OG_1"));
        response =
            accessionRegisterImpl.findDocuments(select.getFinalSelect());
        assertThat(response.isOk()).isTrue();
        assertThat(response.getResults()).hasSize(1);
        summary = response.getResults().iterator().next();
        assertThat(summary.getTotalUnits().getIngested()).isEqualTo(1000);
        assertThat(summary.getTotalUnits().getDeleted()).isEqualTo(200);
        assertThat(summary.getTotalUnits().getRemained()).isEqualTo(800);

        assertThat(summary.getTotalObjectGroups().getIngested()).isEqualTo(1000);
        assertThat(summary.getTotalObjectGroups().getDeleted()).isEqualTo(200);
        assertThat(summary.getTotalObjectGroups().getRemained()).isEqualTo(800);

        assertThat(summary.getTotalObjects().getIngested()).isEqualTo(1000);
        assertThat(summary.getTotalObjects().getDeleted()).isEqualTo(200);
        assertThat(summary.getTotalObjects().getRemained()).isEqualTo(800);

        assertThat(summary.getTotalObjectSize().getIngested()).isEqualTo(9999);
        assertThat(summary.getTotalObjectSize().getDeleted()).isEqualTo(999);
        assertThat(summary.getTotalObjectSize().getRemained()).isEqualTo(9000);

        select = new Select();
        select.setQuery(
            QueryHelper.and().add(QueryHelper.eq("Opi", "Opi_1"), QueryHelper.eq("OriginatingAgency", "OG_1")));

        detailResponse =
            accessionRegisterImpl.findDetail(select.getFinalSelect());

        assertThat(detailResponse.isOk()).isTrue();
        assertThat(detailResponse.getResults()).hasSize(1);
        detail = detailResponse.getResults().iterator().next();
        assertThat(detail.getTotalUnits().getIngested()).isEqualTo(1000);
        assertThat(detail.getTotalUnits().getDeleted()).isEqualTo(200);
        assertThat(detail.getTotalUnits().getRemained()).isEqualTo(800);

        assertThat(detail.getTotalObjectGroups().getIngested()).isEqualTo(1000);
        assertThat(detail.getTotalObjectGroups().getDeleted()).isEqualTo(200);
        assertThat(detail.getTotalObjectGroups().getRemained()).isEqualTo(800);

        assertThat(detail.getTotalObjects().getIngested()).isEqualTo(1000);
        assertThat(detail.getTotalObjects().getDeleted()).isEqualTo(200);
        assertThat(detail.getTotalObjects().getRemained()).isEqualTo(800);

        assertThat(detail.getTotalObjectSize().getIngested()).isEqualTo(9999);
        assertThat(detail.getTotalObjectSize().getDeleted()).isEqualTo(999);
        assertThat(detail.getTotalObjectSize().getRemained()).isEqualTo(9000);
        assertEquals(detail.getStatus(), AccessionRegisterStatus.STORED_AND_UPDATED);

        assertThat(detail.get(AccessionRegisterDetail.EVENTS, List.class)).hasSize(2);


        // Add elimination event 1
        ardm =
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(ACCESSION_REGISTER_DETAIL_ELIMINATION_2),
                AccessionRegisterDetailModel.class);

        accessionRegisterImpl.createOrUpdateAccessionRegister(ardm);

        // Test idempotence of ingest
        ardm.setId(GUIDFactory.newGUID().getId());
        accessionRegisterImpl.createOrUpdateAccessionRegister(ardm);

        select = new Select();
        select.setQuery(QueryHelper.eq("OriginatingAgency", "OG_1"));
        response =
            accessionRegisterImpl.findDocuments(select.getFinalSelect());
        assertThat(response.isOk()).isTrue();
        assertThat(response.getResults()).hasSize(1);
        summary = response.getResults().iterator().next();
        assertThat(summary.getTotalUnits().getIngested()).isEqualTo(1000);
        assertThat(summary.getTotalUnits().getDeleted()).isEqualTo(1000);
        assertThat(summary.getTotalUnits().getRemained()).isEqualTo(0);

        assertThat(summary.getTotalObjectGroups().getIngested()).isEqualTo(1000);
        assertThat(summary.getTotalObjectGroups().getDeleted()).isEqualTo(1000);
        assertThat(summary.getTotalObjectGroups().getRemained()).isEqualTo(0);

        assertThat(summary.getTotalObjects().getIngested()).isEqualTo(1000);
        assertThat(summary.getTotalObjects().getDeleted()).isEqualTo(1000);
        assertThat(summary.getTotalObjects().getRemained()).isEqualTo(0);

        assertThat(summary.getTotalObjectSize().getIngested()).isEqualTo(9999);
        assertThat(summary.getTotalObjectSize().getDeleted()).isEqualTo(9999);
        assertThat(summary.getTotalObjectSize().getRemained()).isEqualTo(0);

        select = new Select();
        select.setQuery(
            QueryHelper.and().add(QueryHelper.eq("Opi", "Opi_1"), QueryHelper.eq("OriginatingAgency", "OG_1")));

        detailResponse =
            accessionRegisterImpl.findDetail(select.getFinalSelect());

        assertThat(detailResponse.isOk()).isTrue();
        assertThat(detailResponse.getResults()).hasSize(1);
        detail = detailResponse.getResults().iterator().next();
        assertThat(detail.getTotalUnits().getIngested()).isEqualTo(1000);
        assertThat(detail.getTotalUnits().getDeleted()).isEqualTo(1000);
        assertThat(detail.getTotalUnits().getRemained()).isEqualTo(0);

        assertThat(detail.getTotalObjectGroups().getIngested()).isEqualTo(1000);
        assertThat(detail.getTotalObjectGroups().getDeleted()).isEqualTo(1000);
        assertThat(detail.getTotalObjectGroups().getRemained()).isEqualTo(0);

        assertThat(detail.getTotalObjects().getIngested()).isEqualTo(1000);
        assertThat(detail.getTotalObjects().getDeleted()).isEqualTo(1000);
        assertThat(detail.getTotalObjects().getRemained()).isEqualTo(0);

        assertThat(detail.getTotalObjectSize().getIngested()).isEqualTo(9999);
        assertThat(detail.getTotalObjectSize().getDeleted()).isEqualTo(9999);
        assertThat(detail.getTotalObjectSize().getRemained()).isEqualTo(0);

        assertThat(detail.get(AccessionRegisterDetail.EVENTS, List.class)).hasSize(3);
    }

    @Test
    @RunWithCustomExecutor
    public void createOrUpdateAccessionRegister_and_findDetail_OK()
        throws Exception {

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        accessionRegisterDetailModel =
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(FILE_TO_TEST_OK),
                AccessionRegisterDetailModel.class);
        ElasticsearchAccessFunctionalAdmin.ensureIndex();

        accessionRegisterDetailModel.setOriginatingAgency("testFindAccessionRegisterDetailAgency");

        accessionRegisterImpl.createOrUpdateAccessionRegister(accessionRegisterDetailModel);
        final MongoCollection<Document> collection =
            mongoRule.getMongoCollection(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getName());
        assertEquals(1, collection.countDocuments());

        final Select select = new Select();
        select.setQuery(eq("OriginatingAgency", "testFindAccessionRegisterDetailAgency"));
        final RequestResponseOK<AccessionRegisterDetail> detail =
            accessionRegisterImpl.findDetail(select.getFinalSelect());
        assertEquals(1, detail.getResults().size());
        final AccessionRegisterDetail item = detail.getResults().get(0);
        assertEquals("testFindAccessionRegisterDetailAgency", item.getOriginatingAgency());
    }

    @Test
    @RunWithCustomExecutor
    public void createOrUpdateAccessionRegister_and_findDocuments_has_create_summary()
        throws Exception {

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        accessionRegisterDetailModel =
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(FILE_TO_TEST_2_OK),
                AccessionRegisterDetailModel.class);
        ElasticsearchAccessFunctionalAdmin.ensureIndex();

        accessionRegisterImpl.createOrUpdateAccessionRegister(accessionRegisterDetailModel);
        final MongoCollection<Document> collection =
            mongoRule.getMongoCollection(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getName());
        assertEquals(1, collection.countDocuments());
        final Select select = new Select();
        select.setQuery(eq("OriginatingAgency", "OG_1"));

        final RequestResponseOK<AccessionRegisterSummary> summary =
            accessionRegisterImpl.findDocuments(select.getFinalSelect());
        assertEquals(1, summary.getResults().size());
        final AccessionRegisterSummary item = summary.getResults().get(0);
        assertEquals("OG_1", item.getOriginatingAgency());
        assertEquals(1, item.getTotalObjects().getRemained());
        assertEquals(1, item.getTotalObjects().getIngested());
        assertEquals(0, item.getTotalObjects().getDeleted());
    }

    @Test
    @RunWithCustomExecutor
    public void createAccessionRegisterSymbolic_OK() throws Exception {

        // Given
        Set<Integer> tenants = ConcurrentHashMap.newKeySet();
        doAnswer((args) -> {
                Integer tenantId = VitamThreadUtils.getVitamSession().getTenantId();
                tenants.add(tenantId);
                return JsonHandler.toJsonNode(new RequestResponseOK<AccessionRegisterSymbolic>()
                    .addResult(new AccessionRegisterSymbolic()
                        .setTenant(tenantId)
                        .setId(GUIDFactory.newGUID().getId())
                        .setOriginatingAgency("sp" + tenantId)
                        .setArchiveUnit(10)
                    ));
            }
        ).when(metaDataClient).createAccessionRegisterSymbolic();

        // When
        List<Integer> tenantList = Arrays.asList(0, 1, 2, 3);
        accessionRegisterImpl.createAccessionRegisterSymbolic(tenantList);

        // Then
        verify(metaDataClient, times(tenantList.size())).createAccessionRegisterSymbolic();
        assertThat(tenants).containsAnyElementsOf(tenantList);

        for (Integer tenant : tenantList) {
            VitamThreadUtils.getVitamSession().setTenantId(tenant);
            final ObjectNode selectAll = new Select().getFinalSelect();
            DbRequestResult results =
                mongoDbAccessAdmin.findDocuments(selectAll, FunctionalAdminCollections.ACCESSION_REGISTER_SYMBOLIC);
            List<AccessionRegisterSymbolic> documents = results.getDocuments(AccessionRegisterSymbolic.class);
            assertThat(documents).hasSize(1);
            assertThat(documents.get(0).getOriginatingAgency()).isEqualTo("sp" + tenant);
        }

        verify(functionalBackupService, times(tenantList.size()))
            .saveDocument(Mockito.eq(FunctionalAdminCollections.ACCESSION_REGISTER_SYMBOLIC), any());
    }

    @Test
    @RunWithCustomExecutor
    public void createAccessionRegisterSymbolic_skip_empty_tenants() throws Exception {

        // Given
        Set<Integer> tenants = ConcurrentHashMap.newKeySet();
        doAnswer((args) -> {
                Integer tenantId = VitamThreadUtils.getVitamSession().getTenantId();
                tenants.add(tenantId);

                // Tenant 0 is empty
                if (tenantId == 0) {
                    return JsonHandler.toJsonNode(new RequestResponseOK<AccessionRegisterSymbolic>());
                }

                return JsonHandler.toJsonNode(new RequestResponseOK<AccessionRegisterSymbolic>()
                    .addResult(new AccessionRegisterSymbolic()
                        .setTenant(tenantId)
                        .setId(GUIDFactory.newGUID().getId())
                        .setOriginatingAgency("sp" + tenantId)
                        .setArchiveUnit(10)
                    ));
            }
        ).when(metaDataClient).createAccessionRegisterSymbolic();

        // When
        List<Integer> tenantList = Arrays.asList(0, 1, 2, 3);
        accessionRegisterImpl.createAccessionRegisterSymbolic(tenantList);

        // Then
        verify(metaDataClient, times(tenantList.size())).createAccessionRegisterSymbolic();
        assertThat(tenants).containsAnyElementsOf(tenantList);

        for (Integer tenant : tenantList) {
            VitamThreadUtils.getVitamSession().setTenantId(tenant);
            final ObjectNode selectAll = new Select().getFinalSelect();
            DbRequestResult results =
                mongoDbAccessAdmin.findDocuments(selectAll, FunctionalAdminCollections.ACCESSION_REGISTER_SYMBOLIC);
            List<AccessionRegisterSymbolic> documents = results.getDocuments(AccessionRegisterSymbolic.class);
            if (tenant == 0) {
                assertThat(documents).isEmpty();
            } else {
                assertThat(documents).hasSize(1);
                assertThat(documents.get(0).getOriginatingAgency()).isEqualTo("sp" + tenant);
            }
        }

        verify(functionalBackupService, times(3))
            .saveDocument(Mockito.eq(FunctionalAdminCollections.ACCESSION_REGISTER_SYMBOLIC), any());
    }

    @Test
    @RunWithCustomExecutor
    public void createAccessionRegisterSymbolic_with_one_tenant_KO_and_others_OK_return_global_response_KO()
        throws Exception {

        // Given
        Set<Integer> tenants = ConcurrentHashMap.newKeySet();
        doAnswer((args) -> {
                Integer tenantId = VitamThreadUtils.getVitamSession().getTenantId();
                tenants.add(tenantId);

                // Tenant 1 is KO
                if (tenantId == 1) {
                    throw new MetaDataClientServerException("");
                }

                return JsonHandler.toJsonNode(new RequestResponseOK<AccessionRegisterSymbolic>()
                    .addResult(new AccessionRegisterSymbolic()
                        .setTenant(tenantId)
                        .setId(GUIDFactory.newGUID().getId())
                        .setOriginatingAgency("sp" + tenantId)
                        .setArchiveUnit(10)
                    ));
            }
        ).when(metaDataClient).createAccessionRegisterSymbolic();

        // When / Then
        List<Integer> tenantList = Arrays.asList(0, 1, 2, 3);
        assertThatThrownBy(() -> accessionRegisterImpl.createAccessionRegisterSymbolic(tenantList))
            .isInstanceOf(ReferentialException.class);

        verify(metaDataClient, times(tenantList.size())).createAccessionRegisterSymbolic();
        assertThat(tenants).containsAnyElementsOf(tenantList);

        for (Integer tenant : tenantList) {
            VitamThreadUtils.getVitamSession().setTenantId(tenant);
            final ObjectNode selectAll = new Select().getFinalSelect();
            DbRequestResult results =
                mongoDbAccessAdmin.findDocuments(selectAll, FunctionalAdminCollections.ACCESSION_REGISTER_SYMBOLIC);
            List<AccessionRegisterSymbolic> documents = results.getDocuments(AccessionRegisterSymbolic.class);
            if (tenant == 1) {
                assertThat(documents).isEmpty();
            } else {
                assertThat(documents).hasSize(1);
                assertThat(documents.get(0).getOriginatingAgency()).isEqualTo("sp" + tenant);
            }
        }

        verify(functionalBackupService, times(3))
            .saveDocument(Mockito.eq(FunctionalAdminCollections.ACCESSION_REGISTER_SYMBOLIC), any());
    }

    @Test
    @RunWithCustomExecutor
    public void createOrUpdateAccessionRegister_with_full_elimination_should_change_status() throws Exception {

        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        ElasticsearchAccessFunctionalAdmin.ensureIndex();
        List<AccessionRegisterDetail> accessionRegisterDetails;
        AccessionRegisterDetail accessionRegisterDetail;
        AccessionRegisterDetailModel accessionRegisterDetailModel01 =
            resourceAccessionRegisterDetails("accession-registers/accession-register-03-01.json");
        AccessionRegisterDetailModel accessionRegisterDetailModel02 =
            resourceAccessionRegisterDetails("accession-registers/accession-register-03-02.json");
        AccessionRegisterDetailModel accessionRegisterDetailModel03 =
            resourceAccessionRegisterDetails("accession-registers/accession-register-03-03.json");

        // When / Then
        accessionRegisterImpl.createOrUpdateAccessionRegister(accessionRegisterDetailModel01);
        accessionRegisterDetails = mongoAccessionRegisterDetails();
        assertThat(accessionRegisterDetails).hasSize(1);
        accessionRegisterDetail = accessionRegisterDetails.get(0);
        assertThat(accessionRegisterDetail.getEvents().size()).isEqualTo(1);
        assertThat(accessionRegisterDetail.getTotalObjectGroups().getIngested()).isEqualTo(1000);
        assertThat(accessionRegisterDetail.getTotalObjectGroups().getDeleted()).isEqualTo(0);
        assertThat(accessionRegisterDetail.getTotalObjectGroups().getRemained()).isEqualTo(1000);
        assertThat(accessionRegisterDetail.getTotalUnits().getIngested()).isEqualTo(1000);
        assertThat(accessionRegisterDetail.getTotalUnits().getDeleted()).isEqualTo(0);
        assertThat(accessionRegisterDetail.getTotalUnits().getRemained()).isEqualTo(1000);
        assertThat(accessionRegisterDetail.getTotalObjects().getIngested()).isEqualTo(1000);
        assertThat(accessionRegisterDetail.getTotalObjects().getDeleted()).isEqualTo(0);
        assertThat(accessionRegisterDetail.getTotalObjects().getRemained()).isEqualTo(1000);
        assertThat(accessionRegisterDetail.getTotalObjectSize().getIngested()).isEqualTo(1000);
        assertThat(accessionRegisterDetail.getTotalObjectSize().getDeleted()).isEqualTo(0);
        assertThat(accessionRegisterDetail.getTotalObjectSize().getRemained()).isEqualTo(1000);
        assertThat(accessionRegisterDetail.getStatus()).isEqualTo(AccessionRegisterStatus.STORED_AND_COMPLETED);

        accessionRegisterImpl.createOrUpdateAccessionRegister(accessionRegisterDetailModel02);
        accessionRegisterDetails = mongoAccessionRegisterDetails();
        assertThat(accessionRegisterDetails).hasSize(1);
        accessionRegisterDetail = accessionRegisterDetails.get(0);
        assertThat(accessionRegisterDetail.getEvents()).hasSize(2);
        assertThat(accessionRegisterDetail.getTotalObjectGroups().getIngested()).isEqualTo(1000);
        assertThat(accessionRegisterDetail.getTotalObjectGroups().getDeleted()).isEqualTo(500);
        assertThat(accessionRegisterDetail.getTotalObjectGroups().getRemained()).isEqualTo(500);
        assertThat(accessionRegisterDetail.getTotalUnits().getIngested()).isEqualTo(1000);
        assertThat(accessionRegisterDetail.getTotalUnits().getDeleted()).isEqualTo(500);
        assertThat(accessionRegisterDetail.getTotalUnits().getRemained()).isEqualTo(500);
        assertThat(accessionRegisterDetail.getTotalObjects().getIngested()).isEqualTo(1000);
        assertThat(accessionRegisterDetail.getTotalObjects().getDeleted()).isEqualTo(500);
        assertThat(accessionRegisterDetail.getTotalObjects().getRemained()).isEqualTo(500);
        assertThat(accessionRegisterDetail.getTotalObjectSize().getIngested()).isEqualTo(1000);
        assertThat(accessionRegisterDetail.getTotalObjectSize().getDeleted()).isEqualTo(500);
        assertThat(accessionRegisterDetail.getTotalObjectSize().getRemained()).isEqualTo(500);
        assertThat(accessionRegisterDetail.getStatus()).isEqualTo(AccessionRegisterStatus.STORED_AND_UPDATED);

        accessionRegisterImpl.createOrUpdateAccessionRegister(accessionRegisterDetailModel03);
        accessionRegisterDetails = mongoAccessionRegisterDetails();
        assertThat(accessionRegisterDetails).hasSize(1);
        accessionRegisterDetail = accessionRegisterDetails.get(0);
        assertThat(accessionRegisterDetail.getEvents()).hasSize(3);
        assertThat(accessionRegisterDetail.getTotalObjectGroups().getIngested()).isEqualTo(1000);
        assertThat(accessionRegisterDetail.getTotalObjectGroups().getDeleted()).isEqualTo(1000);
        assertThat(accessionRegisterDetail.getTotalObjectGroups().getRemained()).isEqualTo(0);
        assertThat(accessionRegisterDetail.getTotalUnits().getIngested()).isEqualTo(1000);
        assertThat(accessionRegisterDetail.getTotalUnits().getDeleted()).isEqualTo(1000);
        assertThat(accessionRegisterDetail.getTotalUnits().getRemained()).isEqualTo(0);
        assertThat(accessionRegisterDetail.getTotalObjects().getIngested()).isEqualTo(1000);
        assertThat(accessionRegisterDetail.getTotalObjects().getDeleted()).isEqualTo(1000);
        assertThat(accessionRegisterDetail.getTotalObjects().getRemained()).isEqualTo(0);
        assertThat(accessionRegisterDetail.getTotalObjectSize().getIngested()).isEqualTo(1000);
        assertThat(accessionRegisterDetail.getTotalObjectSize().getDeleted()).isEqualTo(1000);
        assertThat(accessionRegisterDetail.getTotalObjectSize().getRemained()).isEqualTo(0);
        assertThat(accessionRegisterDetail.getStatus()).isEqualTo(AccessionRegisterStatus.UNSTORED);
    }

    @Test
    @RunWithCustomExecutor
    public void addEventToAccessionRegisterDetail_error_when_missing_fields_part_1() throws Exception {
        Exception ex;
        ex = assertThrows(IllegalArgumentException.class,
            () -> accessionRegisterImpl.addEventToAccessionRegisterDetail(null));
        assertThat(ex).hasMessage("Register detail mustn't be null");

        accessionRegisterDetailModel = new AccessionRegisterDetailModel();
        ex = assertThrows(IllegalArgumentException.class,
            () -> accessionRegisterImpl.addEventToAccessionRegisterDetail(accessionRegisterDetailModel));
        assertThat(ex).hasMessage("Register opi mustn't be null");

        accessionRegisterDetailModel.setOpi("Opi");
        ex = assertThrows(IllegalArgumentException.class,
            () -> accessionRegisterImpl.addEventToAccessionRegisterDetail(accessionRegisterDetailModel));
        assertThat(ex).hasMessage("Register opc mustn't be null");

        accessionRegisterDetailModel.setOpc("Opc");
        ex = assertThrows(IllegalArgumentException.class,
            () -> accessionRegisterImpl.addEventToAccessionRegisterDetail(accessionRegisterDetailModel));
        assertThat(ex).hasMessage("Register tenant mustn't be null");

        accessionRegisterDetailModel.setTenant(TENANT_ID);
        ex = assertThrows(IllegalArgumentException.class,
            () -> accessionRegisterImpl.addEventToAccessionRegisterDetail(accessionRegisterDetailModel));
        assertThat(ex).hasMessage("Register originatingAgency mustn't be null");

        accessionRegisterDetailModel.setOriginatingAgency("OriginatingAgency");
        ex = assertThrows(IllegalArgumentException.class,
            () -> accessionRegisterImpl.addEventToAccessionRegisterDetail(accessionRegisterDetailModel));
        assertThat(ex).hasMessage("Tenant id should be filled");

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        ex = assertThrows(ReferentialException.class,
            () -> accessionRegisterImpl.addEventToAccessionRegisterDetail(accessionRegisterDetailModel));
        assertThat(ex).hasMessage("Document not found");
    }

    @Test
    @RunWithCustomExecutor
    public void addEventToAccessionRegisterDetail_error_when_missing_fields_part_2() throws Exception {
        Exception ex;
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        AccessionRegisterDetailModel accessionRegisterDetailModel01 = resourceAccessionRegisterDetails(
            "accession-registers/accession-register-03-01.json");
        accessionRegisterImpl.createOrUpdateAccessionRegister(accessionRegisterDetailModel01);

        accessionRegisterDetailModel = new AccessionRegisterDetailModel()
            .setId("aehaaaaaaqhad455abryqalenegul3aaaaaq")
            .setTenant(TENANT_ID)
            .setOpi("Opi_1")
            .setOpc("Opc_2")
            .setOriginatingAgency("OG_1");

        ex = assertThrows(ReferentialException.class,
            () -> accessionRegisterImpl.addEventToAccessionRegisterDetail(accessionRegisterDetailModel));
        assertThat(ex).hasMessage("Create register detail error due to missing field");

        accessionRegisterDetailModel.setStatus(AccessionRegisterStatus.STORED_AND_UPDATED);
        ex = assertThrows(ReferentialException.class,
            () -> accessionRegisterImpl.addEventToAccessionRegisterDetail(accessionRegisterDetailModel));
        assertThat(ex).hasMessage("Create register detail error due to missing field");

        accessionRegisterDetailModel.setTotalObjectsGroups(new RegisterValueDetailModel());
        ex = assertThrows(ReferentialException.class,
            () -> accessionRegisterImpl.addEventToAccessionRegisterDetail(accessionRegisterDetailModel));
        assertThat(ex).hasMessage("Create register detail error due to missing field");

        accessionRegisterDetailModel.setTotalObjects(new RegisterValueDetailModel());
        ex = assertThrows(ReferentialException.class,
            () -> accessionRegisterImpl.addEventToAccessionRegisterDetail(accessionRegisterDetailModel));
        assertThat(ex).hasMessage("Create register detail error due to missing field");

        accessionRegisterDetailModel.setTotalUnits(new RegisterValueDetailModel());
        ex = assertThrows(ReferentialException.class,
            () -> accessionRegisterImpl.addEventToAccessionRegisterDetail(accessionRegisterDetailModel));
        assertThat(ex).hasMessage("Create register detail error due to missing field");

        accessionRegisterDetailModel.setObjectSize(new RegisterValueDetailModel());
    }

    @Test
    @RunWithCustomExecutor
    public void addEventToAccessionRegisterDetail_fail_if_event_exists() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        AccessionRegisterDetailModel accessionRegisterDetailModel01 =
            resourceAccessionRegisterDetails("accession-registers/accession-register-03-01.json");
        accessionRegisterImpl.createOrUpdateAccessionRegister(accessionRegisterDetailModel01);

        accessionRegisterDetailModel = new AccessionRegisterDetailModel()
            .setId("aehaaaaaaqhad455abryqalenegul3aaaaaq")
            .setTenant(TENANT_ID)
            .setOpi("Opi_1")
            .setOpc("Opc_2")
            .setOriginatingAgency("OG_1")
            .setTotalObjectsGroups(new RegisterValueDetailModel()
                .setIngested(11)
                .setRemained(12)
                .setDeleted(13))
            .setTotalObjects(new RegisterValueDetailModel()
                .setIngested(21)
                .setRemained(22)
                .setDeleted(23))
            .setTotalUnits(new RegisterValueDetailModel()
                .setIngested(31)
                .setRemained(32)
                .setDeleted(33))
            .setObjectSize(new RegisterValueDetailModel()
                .setIngested(41)
                .setRemained(42)
                .setDeleted(43))
            .setStatus(AccessionRegisterStatus.STORED_AND_UPDATED);
        accessionRegisterImpl.addEventToAccessionRegisterDetail(accessionRegisterDetailModel);

        Exception ex = assertThrows(DocumentAlreadyExistsException.class,
            () -> accessionRegisterImpl.addEventToAccessionRegisterDetail(accessionRegisterDetailModel));
        assertThat(ex).hasMessage(
            "Accession register detail for originating agency (OG_1) and opi (Opi_1) found and already contains the detail (Opc_2)");
    }

    private AccessionRegisterDetailModel resourceAccessionRegisterDetails(String path)
        throws Exception {
        return JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(path),
            AccessionRegisterDetailModel.class);
    }

    private List<AccessionRegisterDetail> mongoAccessionRegisterDetails() throws ReferentialException {
        return mongoDbAccessAdmin.findDocuments(new Select().getFinalSelect(),
            FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL).getDocuments(AccessionRegisterDetail.class);
    }
}
