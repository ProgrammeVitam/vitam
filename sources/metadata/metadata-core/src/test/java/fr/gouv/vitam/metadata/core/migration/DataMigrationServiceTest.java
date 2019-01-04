package fr.gouv.vitam.metadata.core.migration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.util.JSON;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.metadata.core.database.collections.MetadataDocument;
import fr.gouv.vitam.metadata.core.database.collections.MongoDbMetadataRepository;
import fr.gouv.vitam.metadata.core.database.collections.ObjectGroup;
import fr.gouv.vitam.metadata.core.database.collections.Unit;

import fr.gouv.vitam.metadata.core.graph.GraphService;
import net.javacrumbs.jsonunit.JsonAssert;
import net.javacrumbs.jsonunit.core.Option;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.mongodb.client.model.Indexes.ascending;
import static com.mongodb.client.model.Sorts.orderBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class DataMigrationServiceTest {

    private static final String UNIT_COLLECTION = "TestCollectionUnit";
    private static final String OBJECT_GROUP_COLLECTION = "TestCollectionGot";
    private static final String VITAM_TEST = "vitam-test";

    private static final int TEST_BULK_SIZE = 10;
    @Rule
    public RunWithCustomExecutorRule runInThread =
            new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());


    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    @ClassRule
    public static MongoRule mongoRule =
            new MongoRule(VitamCollection.getMongoClientOptions(Lists.newArrayList(Unit.class, ObjectGroup.class)),
                    VITAM_TEST,
                    UNIT_COLLECTION,
                    OBJECT_GROUP_COLLECTION);

    private MongoCollection<Unit> unitCollection = mongoRule.getMongoCollection(UNIT_COLLECTION, Unit.class);
    private MongoCollection<ObjectGroup> ogCollection =
            mongoRule.getMongoCollection(OBJECT_GROUP_COLLECTION, ObjectGroup.class);

    private DataMigrationRepository repository =
            new DataMigrationRepository(unitCollection, ogCollection, TEST_BULK_SIZE);

    private GraphService graphService;

    @Before
    public void setUpBeforeClass() throws Exception {
        graphService = new GraphService(new MongoDbMetadataRepository(unitCollection));
    }

    @After
    public void tearDown() {
        mongoRule.handleAfter();
    }

    @Test
    public void tryStartMongoDataUpdate_firstStart() throws Exception {

        // Given
        DataMigrationService instance = spy(new DataMigrationService(repository, graphService));
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
        DataMigrationService instance = spy(new DataMigrationService(repository, graphService));

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
        DataMigrationService instance = spy(new DataMigrationService(repository, graphService));

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
        DataMigrationService instance = new DataMigrationService(repository, graphService);

        // When
        instance.mongoDataUpdate();

        awaitTermination(instance);

        // Then
        assertThat(unitCollection.find().iterator().hasNext()).isFalse();
    }

    @RunWithCustomExecutor
    @Test
    public void mongoDataUpdate_checkGraphMigration() throws Exception {

        // Given : Complex data set with R6 model (no graph fields)
        String unitDataSetFile = "DataMigrationR6/30UnitDataSet/R6UnitDataSet.json";
        importUnitDataSetFile(unitDataSetFile);

        String ogDataSetFile = "DataMigrationR6/15ObjectGroupDataSet/R6ObjectGroupDataSet.json";
        importObjectGroupDataSetFile(ogDataSetFile);

        DataMigrationService instance = new DataMigrationService(repository, graphService);

        // When
        instance.mongoDataUpdate();

        awaitTermination(instance);

        // Then
        String expectedUnitDataSetFile = "DataMigrationR6/30UnitDataSet/ExpectedR7UnitDataSet.json";
        assertDataSetEqualsExpectedFile(unitCollection, expectedUnitDataSetFile);

        String expectedOGDataSetFile = "DataMigrationR6/15ObjectGroupDataSet/ExpectedR7ObjectGroupDataSet.json";
        assertDataSetEqualsExpectedFile(ogCollection, expectedOGDataSetFile);
    }

    private <T> void assertDataSetEqualsExpectedFile(MongoCollection<T> mongoCollection, String expectedDataSetFile)
            throws InvalidParseOperationException, FileNotFoundException {

        ArrayNode unitDataSet = dumpDataSet(mongoCollection);

        String updatedUnitDataSet = JsonHandler.unprettyPrint(unitDataSet);
        String expectedUnitDataSet =
                JsonHandler.unprettyPrint(JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(
                        expectedDataSetFile)));

        JsonAssert.assertJsonEquals(expectedUnitDataSet, updatedUnitDataSet,
                JsonAssert.when(Option.IGNORING_ARRAY_ORDER));
    }

    private <T> ArrayNode dumpDataSet(MongoCollection<T> mongoCollection) throws InvalidParseOperationException {

        ArrayNode dataSet = JsonHandler.createArrayNode();
        FindIterable<T> documents = mongoCollection.find()
                .sort(orderBy(ascending(MetadataDocument.ID)));

        for (T document : documents) {
            ObjectNode jsonUnit = (ObjectNode) JsonHandler.getFromString(JSON.serialize(document));

            // Replace _glpd with marker
            assertThat(jsonUnit.get(MetadataDocument.GRAPH_LAST_PERSISTED_DATE)).isNotNull();
            jsonUnit.put(MetadataDocument.GRAPH_LAST_PERSISTED_DATE, "#TIMESTAMP#");

            dataSet.add(jsonUnit);
        }

        return dataSet;
    }

    private void awaitTermination(DataMigrationService instance) throws InterruptedException {
        for (int i = 0; i < 30; i++) {
            Thread.sleep(1000);

            if (!instance.isMongoDataUpdateInProgress())
                return;
        }

        fail("Expected data migration termination");
    }

    private void importUnitDataSetFile(String dataSetFile)
            throws FileNotFoundException, InvalidParseOperationException {
        InputStream inputDataSet = PropertiesUtils.getResourceAsStream(dataSetFile);
        ArrayNode jsonDataSet = (ArrayNode) JsonHandler.getFromInputStream(inputDataSet);
        for (JsonNode jsonNode : jsonDataSet) {
            unitCollection.insertOne(new Unit(JsonHandler.unprettyPrint(jsonNode)));
        }
    }

    private void importObjectGroupDataSetFile(String dataSetFile)
            throws FileNotFoundException, InvalidParseOperationException {
        InputStream inputDataSet = PropertiesUtils.getResourceAsStream(dataSetFile);
        ArrayNode jsonDataSet = (ArrayNode) JsonHandler.getFromInputStream(inputDataSet);
        for (JsonNode jsonNode : jsonDataSet) {
            ogCollection.insertOne(new ObjectGroup(JsonHandler.unprettyPrint(jsonNode)));
        }
    }
}

