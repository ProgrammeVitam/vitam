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
package fr.gouv.vitam.functional.administration.migration.r7r8;

import static com.mongodb.client.model.Sorts.ascending;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Sorts;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.api.VitamRepositoryFactory;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.BsonHelper;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterDetail;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterSummary;
import fr.gouv.vitam.functional.administration.common.FunctionalBackupService;
import fr.gouv.vitam.functional.administration.common.server.ElasticsearchAccessFunctionalAdmin;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import net.javacrumbs.jsonunit.JsonAssert;
import net.javacrumbs.jsonunit.core.Option;
import org.apache.commons.lang3.time.StopWatch;
import org.bson.Document;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;


public class AccessionRegisterMigrationServiceTest {

    private static final String PREFIX = GUIDFactory.newGUID().getId();

    @ClassRule
    public static MongoRule mongoRule =
        new MongoRule(VitamCollection
            .getMongoClientOptions(Lists.newArrayList(AccessionRegisterDetail.class, AccessionRegisterSummary.class)));

    @ClassRule
    public static ElasticsearchRule elasticsearchRule = new ElasticsearchRule();

    private static AccessionRegisterMigrationRepository accessionRegisterMigrationRepository;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private FunctionalBackupService functionalBackupService;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        final List<ElasticsearchNode> esNodes =
            Lists.newArrayList(new ElasticsearchNode("localhost", ElasticsearchRule.TCP_PORT));

        FunctionalAdminCollections.beforeTestClass(mongoRule.getMongoDatabase(), PREFIX,
            new ElasticsearchAccessFunctionalAdmin(ElasticsearchRule.VITAM_CLUSTER, esNodes),
            Arrays.asList(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL,
                FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY));
        accessionRegisterMigrationRepository = new AccessionRegisterMigrationRepository(VitamRepositoryFactory.get());

    }

    @After
    public void tearDown() {
        FunctionalAdminCollections.afterTest(Lists.newArrayList(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL,
            FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY));
    }

    @AfterClass
    public static void afterClass() {
        FunctionalAdminCollections
            .afterTestClass(Lists.newArrayList(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL,
                FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY), true);
    }

    @Test
    public void tryStartMigration_firstStart() throws Exception {
        // Given
        AccessionRegisterMigrationService instance =
            spy(new AccessionRegisterMigrationService(accessionRegisterMigrationRepository, functionalBackupService));
        doNothing().when(instance).mongoDataUpdate();

        // When
        boolean started = instance.tryStartMigration(MigrationAction.MIGRATE);

        StopWatch stopWatch = StopWatch.createStarted();

        while (instance.isMigrationInProgress()) {
            Thread.sleep(5);
            if (stopWatch.getTime(TimeUnit.MINUTES) >= 1) {
                fail("Timeout waiting tryStartMigration MIGRATE");
            }
        }

        // Than
        assertThat(started).isTrue();
        verify(instance, times(1)).mongoDataUpdate();


        doNothing().when(instance).purge();

        // When
        started = instance.tryStartMigration(MigrationAction.PURGE);

        stopWatch = StopWatch.createStarted();
        while (instance.isMigrationInProgress()) {
            Thread.sleep(5);
            if (stopWatch.getTime(TimeUnit.MINUTES) >= 1) {
                fail("Timeout waiting tryStartMigration PURGE");
            }
        }
        // Than
        assertThat(started).isTrue();
        verify(instance, times(1)).purge();
    }

    @Test
    public void tryStartMigration_alreadyRunning() throws Exception {

        // Given
        AccessionRegisterMigrationService instance =
            spy(new AccessionRegisterMigrationService(accessionRegisterMigrationRepository, functionalBackupService));

        CountDownLatch longRunningTask = new CountDownLatch(1);

        doAnswer(i -> {
            awaitOrThrow(longRunningTask);
            return null;
        }).when(instance).mongoDataUpdate();

        // When
        boolean firstStart = instance.tryStartMigration(MigrationAction.MIGRATE);
        boolean secondStart = instance.tryStartMigration(MigrationAction.MIGRATE);

        longRunningTask.countDown();
        awaitTermination(instance);

        // Then : Ensure process invoked once
        assertThat(firstStart).isTrue();
        assertThat(secondStart).isFalse();
        verify(instance, times(1)).mongoDataUpdate();
    }

    @Test
    public void tryStartMigration_restart() throws Exception {

        // First invocation

        // Given
        AccessionRegisterMigrationService instance =
            spy(new AccessionRegisterMigrationService(accessionRegisterMigrationRepository, functionalBackupService));

        // When
        boolean firstStart = instance.tryStartMigration(MigrationAction.MIGRATE);
        awaitTermination(instance);

        boolean secondStart = instance.tryStartMigration(MigrationAction.MIGRATE);
        awaitTermination(instance);

        // Then : Ensure process invoked twice
        assertThat(firstStart).isTrue();
        assertThat(secondStart).isTrue();
        verify(instance, times(2)).mongoDataUpdate();
    }

    private static void awaitOrThrow(CountDownLatch awaitTermination) throws InterruptedException {
        assertThat(awaitTermination.await(30, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void mongoDataUpdate_emptyDataSet() throws Exception {

        // Given
        AccessionRegisterMigrationService instance =
            spy(new AccessionRegisterMigrationService(accessionRegisterMigrationRepository, functionalBackupService));

        // When
        instance.tryStartMigration(MigrationAction.MIGRATE);

        awaitTermination(instance);

        // Then
        assertThat(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getCollection().find().iterator().hasNext())
            .isFalse();
    }

    @Test
    public void mongoDataUpdate_checkAccessionRegister_Detail_and_Summary_Migration() throws Exception {

        // R7 AccessionRegister(Detail, Summary) model
        String accessionRegisterDetailDataSetFile = "migration_r7_r8/accession_register_detail.json";
        importDataSetFile(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getCollection(),
            accessionRegisterDetailDataSetFile);

        String accessionRegisterSummaryDataSetFile = "migration_r7_r8/accession_register_summary.json";
        importDataSetFile(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getCollection(),
            accessionRegisterSummaryDataSetFile);

        AccessionRegisterMigrationService instance =
            spy(new AccessionRegisterMigrationService(accessionRegisterMigrationRepository, functionalBackupService));

        // When
        instance.tryStartMigration(MigrationAction.MIGRATE);

        awaitTermination(instance);

        // Then
        String expectedAccessionRegisterDetailDataSetFile = "migration_r7_r8/accession_register_detail_EXPECTED.json";
        assertDataSetEqualsExpectedFile(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getCollection(),
            expectedAccessionRegisterDetailDataSetFile);

        // Check persisted in ES
        SearchResponse search = FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getEsClient()
            .search(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getName().toLowerCase(), null,
                QueryBuilders.matchAllQuery());
        assertThat(search.getHits().getTotalHits().value).isEqualTo(2);

        String expectedAccessionRegisterSummaryDataSetFile = "migration_r7_r8/accession_register_summary_EXPECTED.json";
        assertDataSetEqualsExpectedFile(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getCollection(),
            expectedAccessionRegisterSummaryDataSetFile);

        // Check persisted in ES
        search = FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getEsClient()
            .search(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getName().toLowerCase(), null,
                QueryBuilders.matchAllQuery());
        assertThat(search.getHits().getTotalHits().value).isEqualTo(2);


        // When purge
        instance.tryStartMigration(MigrationAction.PURGE);

        awaitTermination(instance);

        // Then Mongo and Elasticsearch purged
        assertThat(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getCollection().countDocuments()).isEqualTo(0);
        assertThat(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getCollection().countDocuments()).isEqualTo(0);

        search = FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getEsClient()
            .search(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getName().toLowerCase(), null,
                QueryBuilders.matchAllQuery());
        assertThat(search.getHits().getTotalHits().value).isEqualTo(0);

        search = FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getEsClient()
            .search(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getName().toLowerCase(), null,
                QueryBuilders.matchAllQuery());
        assertThat(search.getHits().getTotalHits().value).isEqualTo(0);
    }

    private void importDataSetFile(MongoCollection<Document> mongoCollection, String dataSetFile)
        throws FileNotFoundException, InvalidParseOperationException {
        InputStream inputDataSet = PropertiesUtils.getResourceAsStream(dataSetFile);
        ArrayNode jsonDataSet = (ArrayNode) JsonHandler.getFromInputStream(inputDataSet);
        for (JsonNode jsonNode : jsonDataSet) {
            mongoCollection.insertOne(Document.parse(JsonHandler.unprettyPrint(jsonNode)));
        }
    }


    private <T> void assertDataSetEqualsExpectedFile(MongoCollection<T> mongoCollection, String expectedDataSetFile)
        throws InvalidParseOperationException, FileNotFoundException {

        ArrayNode unitDataSet = dumpDataSet(mongoCollection);

        String expectedUnitDataSet =
            JsonHandler.unprettyPrint(JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(
                expectedDataSetFile)));

        JsonAssert.assertJsonEquals(expectedUnitDataSet, unitDataSet,
            JsonAssert.when(Option.IGNORING_ARRAY_ORDER));
    }

    private <T> ArrayNode dumpDataSet(MongoCollection<T> mongoCollection) throws InvalidParseOperationException {

        ArrayNode dataSet = JsonHandler.createArrayNode();
        FindIterable<T> documents = mongoCollection.find()
            .sort(Sorts.orderBy(ascending("_id")));

        for (T document : documents) {
            ObjectNode json = (ObjectNode) JsonHandler.getFromString(BsonHelper.stringify(document));
            dataSet.add(json);
        }

        return dataSet;
    }

    private void awaitTermination(AccessionRegisterMigrationService instance) throws InterruptedException {
        for (int i = 0; i < 30; i++) {
            Thread.sleep(1000);

            if (!instance.isMigrationInProgress())
                return;
        }

        fail("Expected data migration termination");
    }
}
