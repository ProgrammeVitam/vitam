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
package fr.gouv.vitam.functional.administration.accession.register.core;

import com.google.common.collect.Lists;
import com.mongodb.client.MongoCollection;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.AccessionRegisterDetailModel;
import fr.gouv.vitam.common.model.administration.AccessionRegisterStatus;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterDetail;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterSummary;
import fr.gouv.vitam.functional.administration.common.FunctionalBackupService;
import fr.gouv.vitam.functional.administration.common.server.ElasticsearchAccessFunctionalAdmin;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminFactory;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import org.bson.Document;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static fr.gouv.vitam.common.database.collections.VitamCollection.getMongoClientOptions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class ReferentialAccessionRegisterImplTest {
    static String ACCESSION_REGISTER_DETAIL = "accession-register_detail.json";
    static String ACCESSION_REGISTER_DETAIL_ELIMINATION = "accession-register_detail_elimination.json";
    static String ACCESSION_REGISTER_DETAIL_ELIMINATION_2 = "accession-register_detail_elimination_2.json";
    static String FILE_TO_TEST_OK = "accession-register.json";
    static String FILE_TO_TEST_2_OK = "accession-register_2.json";
    private static final Integer TENANT_ID = 0;

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());
    public static final String PREFIX = GUIDFactory.newGUID().getId();

    @ClassRule
    public static MongoRule mongoRule =
        new MongoRule(
            getMongoClientOptions(Lists.newArrayList(AccessionRegisterDetail.class, AccessionRegisterSummary.class)));

    @ClassRule
    public static ElasticsearchRule elasticsearchRule = new ElasticsearchRule();

    private static ElasticsearchAccessFunctionalAdmin esClient;

    static ReferentialAccessionRegisterImpl accessionRegisterImpl;
    static AccessionRegisterDetailModel register;

    @BeforeClass
    @RunWithCustomExecutor
    public static void setUpBeforeClass() throws Exception {

        final List<ElasticsearchNode> esNodes = new ArrayList<>();
        esNodes.add(new ElasticsearchNode("localhost", ElasticsearchRule.TCP_PORT));
        esClient = new ElasticsearchAccessFunctionalAdmin(ElasticsearchRule.VITAM_CLUSTER, esNodes);

        final List<MongoDbNode> nodes = new ArrayList<>();
        nodes.add(new MongoDbNode("localhost", mongoRule.getDataBasePort()));
        MongoDbAccessAdminImpl mongoDbAccessAdmin =
            MongoDbAccessAdminFactory
                .create(new DbConfigurationImpl(nodes, mongoRule.getMongoDatabase().getName()), Collections::emptyList);
        accessionRegisterImpl = new ReferentialAccessionRegisterImpl(mongoDbAccessAdmin,
            mock(FunctionalBackupService.class));

        FunctionalAdminCollections.beforeTestClass(mongoRule.getMongoDatabase(), PREFIX,
            esClient,
            Arrays.asList(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL,
                FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY));
    }

    @AfterClass
    public static void tearDownAfterClass() {
        FunctionalAdminCollections.afterTestClass(true);
    }

    @After
    public void afterTest() {
        FunctionalAdminCollections.afterTest();
    }


    @Test
    @RunWithCustomExecutor
    public void testCreateAndUpdateAccessionRegister() throws Exception {

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
        AccessionRegisterDetail accessionRegisterDetailBeforeUpdateResult =  detailResponse.getResults().get(0);
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
    public void testFindAccessionRegisterDetail()
        throws Exception {

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        register = JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(FILE_TO_TEST_OK),
            AccessionRegisterDetailModel.class);
        ElasticsearchAccessFunctionalAdmin.ensureIndex();

        register.setOriginatingAgency("testFindAccessionRegisterDetailAgency");

        accessionRegisterImpl.createOrUpdateAccessionRegister(register);
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
    public void testFindAccessionRegisterSummary()
        throws Exception {

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        register = JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(FILE_TO_TEST_2_OK),
            AccessionRegisterDetailModel.class);
        ElasticsearchAccessFunctionalAdmin.ensureIndex();

        accessionRegisterImpl.createOrUpdateAccessionRegister(register);
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
}
