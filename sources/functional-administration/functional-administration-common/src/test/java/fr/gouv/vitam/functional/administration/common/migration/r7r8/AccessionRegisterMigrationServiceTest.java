package fr.gouv.vitam.functional.administration.common.migration.r7r8;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Sorts;
import com.mongodb.util.JSON;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.database.offset.OffsetRepository;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterDetail;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterSummary;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import net.javacrumbs.jsonunit.JsonAssert;
import net.javacrumbs.jsonunit.core.Option;
import org.bson.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.mongodb.client.model.Sorts.ascending;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;


@RunWith(MockitoJUnitRunner.class)
public class AccessionRegisterMigrationServiceTest {

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    @ClassRule
    public static MongoRule mongoRule =
            new MongoRule(VitamCollection.getMongoClientOptions(Lists.newArrayList(AccessionRegisterDetail.class, AccessionRegisterSummary.class)), "Vitam-Test",
                    FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getName(),
                    FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getName());


    private AccessionRegisterMigrationRepository accessionRegisterMigrationRepository;

    @Before
    public void setUpBeforeClass() throws Exception {
        FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getVitamCollection().initialize(mongoRule.getMongoDatabase(), true);
        FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getVitamCollection().initialize(mongoRule.getMongoDatabase(), true);
        accessionRegisterMigrationRepository = new AccessionRegisterMigrationRepository();
    }

    @After
    public void tearDown() {
        mongoRule.handleAfter();
    }

    @Test
    public void tryStartMongoDataUpdate_firstStart() throws Exception {

        // Given
        AccessionRegisterMigrationService instance = spy(new AccessionRegisterMigrationService(accessionRegisterMigrationRepository));
        CountDownLatch awaitTermination = new CountDownLatch(1);
        Mockito.doAnswer(i -> {
            awaitTermination.countDown();
            return null;
        }).when(instance).mongoDataUpdate();

        // When
        boolean started = instance.tryStartMongoDataUpdate();
        awaitOrThrow(awaitTermination);

        // Than
        assertThat(started).isTrue();
        verify(instance, Mockito.times(1)).mongoDataUpdate();
    }

    @Test
    public void tryStartMongoDataUpdate_alreadyRunning() throws Exception {

        // Given
        AccessionRegisterMigrationService instance = spy(new AccessionRegisterMigrationService(accessionRegisterMigrationRepository));

        CountDownLatch longRunningTask = new CountDownLatch(1);

        Mockito.doAnswer(i -> {
            awaitOrThrow(longRunningTask);
            return null;
        }).when(instance).mongoDataUpdate();

        // When
        boolean firstStart = instance.tryStartMongoDataUpdate();
        boolean secondStart = instance.tryStartMongoDataUpdate();

        longRunningTask.countDown();
        awaitTermination(instance);

        // Then : Ensure process invoked once
        assertThat(firstStart).isTrue();
        assertThat(secondStart).isFalse();
        verify(instance, Mockito.times(1)).mongoDataUpdate();
    }

    @Test
    public void tryStartMongoDataUpdate_restart() throws Exception {

        // First invocation

        // Given
        AccessionRegisterMigrationService instance = spy(new AccessionRegisterMigrationService(accessionRegisterMigrationRepository));

        // When
        boolean firstStart = instance.tryStartMongoDataUpdate();
        awaitTermination(instance);

        boolean secondStart = instance.tryStartMongoDataUpdate();
        awaitTermination(instance);

        // Then : Ensure process invoked twice
        assertThat(firstStart).isTrue();
        assertThat(secondStart).isTrue();
        verify(instance, Mockito.times(2)).mongoDataUpdate();
    }

    private static void awaitOrThrow(CountDownLatch awaitTermination) throws InterruptedException {
        assertThat(awaitTermination.await(30, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void mongoDataUpdate_emptyDataSet() throws Exception {

        // Given
        AccessionRegisterMigrationService instance = spy(new AccessionRegisterMigrationService(accessionRegisterMigrationRepository));

        // When
        instance.mongoDataUpdate();

        awaitTermination(instance);

        // Then
        assertThat(accessionRegisterMigrationRepository.getAccessionRegisterDetailCollection().find().iterator().hasNext()).isFalse();
    }

    @Test
    public void mongoDataUpdate_checkAccessionRegister_Detail_and_Summary_Migration() throws Exception {

        // R7 AccessionRegister(Detail, Summary) model
        String accessionRegisterDetailDataSetFile = "migration_r7_r8/accession_register_detail.json";
        importDataSetFile(accessionRegisterMigrationRepository.getAccessionRegisterDetailCollection(), accessionRegisterDetailDataSetFile);

        String accessionRegisterSummaryDataSetFile = "migration_r7_r8/accession_register_summary.json";
        importDataSetFile(accessionRegisterMigrationRepository.getAccessionRegisterSummaryCollection(), accessionRegisterSummaryDataSetFile);

        AccessionRegisterMigrationService instance = spy(new AccessionRegisterMigrationService(accessionRegisterMigrationRepository));

        // When
        instance.mongoDataUpdate();

        awaitTermination(instance);

        // Then
        String expectedAccessionRegisterDetailDataSetFile = "migration_r7_r8/accession_register_detail_EXPECTED.json";
        assertDataSetEqualsExpectedFile(accessionRegisterMigrationRepository.getAccessionRegisterDetailCollection(), expectedAccessionRegisterDetailDataSetFile);

        String expectedAccessionRegisterSummaryDataSetFile = "migration_r7_r8/accession_register_summary_EXPECTED.json";
        assertDataSetEqualsExpectedFile(accessionRegisterMigrationRepository.getAccessionRegisterSummaryCollection(), expectedAccessionRegisterSummaryDataSetFile);
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
            ObjectNode json = (ObjectNode) JsonHandler.getFromString(JSON.serialize(document));
            dataSet.add(json);
        }

        return dataSet;
    }

    private void awaitTermination(AccessionRegisterMigrationService instance) throws InterruptedException {
        for (int i = 0; i < 30; i++) {
            Thread.sleep(1000);

            if (!instance.isMongoDataUpdateInProgress())
                return;
        }

        fail("Expected data migration termination");
    }
}