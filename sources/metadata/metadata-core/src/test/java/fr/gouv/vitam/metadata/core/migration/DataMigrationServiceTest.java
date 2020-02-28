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
package fr.gouv.vitam.metadata.core.migration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.BsonHelper;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.database.collections.MetadataDocument;
import fr.gouv.vitam.metadata.core.database.collections.MongoDbMetadataRepository;
import fr.gouv.vitam.metadata.core.database.collections.ObjectGroup;
import fr.gouv.vitam.metadata.core.database.collections.Unit;
import fr.gouv.vitam.metadata.core.graph.GraphLoader;
import net.javacrumbs.jsonunit.JsonAssert;
import net.javacrumbs.jsonunit.core.Option;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

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

public class DataMigrationServiceTest {

    private static final String UNIT_COLLECTION = "Unit" + GUIDFactory.newGUID().getId();
    private static final String OBJECT_GROUP_COLLECTION = "Got" + GUIDFactory.newGUID().getId();

    private static final int TEST_BULK_SIZE = 10;
    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    @ClassRule
    public static MongoRule mongoRule =
        new MongoRule(VitamCollection.getMongoClientOptions(Lists.newArrayList(Unit.class, ObjectGroup.class)),
            UNIT_COLLECTION,
            OBJECT_GROUP_COLLECTION);

    private static DataMigrationRepository dataMigrationRepository;

    private GraphLoader graphLoader;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        MetadataCollections.UNIT.getVitamCollection().setName(UNIT_COLLECTION);
        MetadataCollections.UNIT.getVitamCollection().initialize(mongoRule.getMongoDatabase(), false);
        MetadataCollections.OBJECTGROUP.getVitamCollection().setName(OBJECT_GROUP_COLLECTION);
        MetadataCollections.OBJECTGROUP.getVitamCollection().initialize(mongoRule.getMongoDatabase(), false);
        dataMigrationRepository = new DataMigrationRepository(TEST_BULK_SIZE);
    }

    @Before
    public void befor() {
        graphLoader = new GraphLoader(new MongoDbMetadataRepository(() -> MetadataCollections.UNIT.getCollection()));
    }

    @AfterClass
    public static void afterClass() {
        mongoRule.handleAfterClass();
    }

    @After
    public void tearDown() {
        mongoRule.handleAfter();
    }

    @Test
    public void tryStartMongoDataUpdate_firstStart() throws Exception {

        // Given
        DataMigrationService instance = spy(new DataMigrationService(dataMigrationRepository, graphLoader));
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
        DataMigrationService instance = spy(new DataMigrationService(dataMigrationRepository, graphLoader));

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
        DataMigrationService instance = spy(new DataMigrationService(dataMigrationRepository, graphLoader));

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
        DataMigrationService instance = new DataMigrationService(dataMigrationRepository, graphLoader);

        // When
        instance.mongoDataUpdate();

        awaitTermination(instance);

        // Then
        assertThat(MetadataCollections.UNIT.getCollection().find().iterator().hasNext()).isFalse();
    }

    @RunWithCustomExecutor
    @Test
    public void mongoDataUpdate_checkGraphMigration() throws Exception {

        // Given : Complex data set with R6 model (no graph fields)
        String unitDataSetFile = "DataMigrationR6/30UnitDataSet/R6UnitDataSet.json";
        importUnitDataSetFile(unitDataSetFile);

        String ogDataSetFile = "DataMigrationR6/15ObjectGroupDataSet/R6ObjectGroupDataSet.json";
        importObjectGroupDataSetFile(ogDataSetFile);

        DataMigrationService instance = new DataMigrationService(dataMigrationRepository, graphLoader);

        // When
        instance.mongoDataUpdate();

        awaitTermination(instance);

        // Then
        String expectedUnitDataSetFile = "DataMigrationR6/30UnitDataSet/ExpectedR7UnitDataSet.json";
        assertDataSetEqualsExpectedFile(MetadataCollections.UNIT.getCollection(), expectedUnitDataSetFile);

        String expectedOGDataSetFile = "DataMigrationR6/15ObjectGroupDataSet/ExpectedR7ObjectGroupDataSet.json";
        assertDataSetEqualsExpectedFile(MetadataCollections.OBJECTGROUP.getCollection(), expectedOGDataSetFile);
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
            ObjectNode jsonUnit = (ObjectNode) JsonHandler.getFromString(BsonHelper.stringify(document));

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
            MetadataCollections.UNIT.getCollection().insertOne(new Unit(JsonHandler.unprettyPrint(jsonNode)));
        }
    }

    private void importObjectGroupDataSetFile(String dataSetFile)
        throws FileNotFoundException, InvalidParseOperationException {
        InputStream inputDataSet = PropertiesUtils.getResourceAsStream(dataSetFile);
        ArrayNode jsonDataSet = (ArrayNode) JsonHandler.getFromInputStream(inputDataSet);
        for (JsonNode jsonNode : jsonDataSet) {
            MetadataCollections.OBJECTGROUP.getCollection()
                .insertOne(new ObjectGroup(JsonHandler.unprettyPrint(jsonNode)));
        }
    }
}

