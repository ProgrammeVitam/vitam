package fr.gouv.vitam.storage.offers.common.migration;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.OfferLogFormatVersion;
import fr.gouv.vitam.storage.offers.common.core.DefaultOfferService;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import org.bson.Document;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.ArrayList;
import java.util.List;

import static fr.gouv.vitam.storage.offers.common.database.OfferLogDatabaseService.OFFER_LOG_COLLECTION_NAME;
import static fr.gouv.vitam.storage.offers.common.database.OfferSequenceDatabaseService.OFFER_SEQUENCE_COLLECTION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class OfferLogR7MigrationProcessTest {

    private static final String DATABASE_NAME = "Vitam";

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @Rule
    public MongoRule mongoRule =
        new MongoRule(VitamCollection.getMongoClientOptions(), DATABASE_NAME,
            OFFER_SEQUENCE_COLLECTION, OFFER_LOG_COLLECTION_NAME);

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private DefaultOfferService defaultOfferService;

    @BeforeClass
    public static void beforeClass() {
        VitamConfiguration.setBatchSize(10);
    }

    @AfterClass
    public static void afterClass() {
        VitamConfiguration.setBatchSize(10);
    }

    @Test
    @RunWithCustomExecutor
    public void testMigrationAllFilesExist() throws Exception {

        // Given
        MongoDatabase mongoDatabase = mongoRule.getMongoDatabase();
        OfferLogR7MigrationProcess instance = new OfferLogR7MigrationProcess(defaultOfferService, mongoDatabase);
        MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(OFFER_LOG_COLLECTION_NAME);

        List<Document> documentsToInsert = new ArrayList<>();
        for (int i = 0; i < 100; i++) {

            OfferLog offerLog = new OfferLog("0_unit", "file" + i, "write")
                .setFormatVersion(OfferLogFormatVersion.V1);
            offerLog.setSequence((long) i);
            String json;
            try {
                json = JsonHandler.writeAsString(offerLog);
            } catch (InvalidParseOperationException exc) {
                throw new ContentAddressableStorageServerException("Cannot parse storage log", exc);
            }
            documentsToInsert.add(Document.parse(json));
        }
        mongoCollection.insertMany(documentsToInsert);

        doReturn(true).when(defaultOfferService).isObjectExist(eq("0_unit"), anyString());

        // When
        instance.run(null);
        awaitTermination(instance);

        // Then
        assertThat(instance.isRunning()).isFalse();
        assertThat(instance.getMigrationStatus().getStatusCode()).isEqualTo(StatusCode.OK);
        assertThat(instance.getMigrationStatus().getCurrentOffset()).isEqualTo(99L);

        assertThat(mongoCollection.count()).isEqualTo(100);
        verify(defaultOfferService, times(100)).isObjectExist(eq("0_unit"), anyString());
        verifyNoMoreInteractions(defaultOfferService);
    }

    @Test
    @RunWithCustomExecutor
    public void testMigrationAllFilesExistWithStartOffset() throws Exception {

        // Given
        MongoDatabase mongoDatabase = mongoRule.getMongoDatabase();
        OfferLogR7MigrationProcess instance = new OfferLogR7MigrationProcess(defaultOfferService, mongoDatabase);
        MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(OFFER_LOG_COLLECTION_NAME);

        List<Document> documentsToInsert = new ArrayList<>();
        for (int i = 0; i < 100; i++) {

            OfferLog offerLog = new OfferLog("0_unit", "file" + i, "write")
                .setFormatVersion(OfferLogFormatVersion.V1);
            offerLog.setSequence((long) i);
            String json;
            try {
                json = JsonHandler.writeAsString(offerLog);
            } catch (InvalidParseOperationException exc) {
                throw new ContentAddressableStorageServerException("Cannot parse storage log", exc);
            }
            documentsToInsert.add(Document.parse(json));
        }
        mongoCollection.insertMany(documentsToInsert);

        doReturn(true).when(defaultOfferService).isObjectExist(eq("0_unit"), anyString());

        // When
        instance.run(12L);
        awaitTermination(instance);

        // Then
        assertThat(instance.isRunning()).isFalse();
        assertThat(instance.getMigrationStatus().getStatusCode()).isEqualTo(StatusCode.OK);
        assertThat(instance.getMigrationStatus().getCurrentOffset()).isEqualTo(99L);

        assertThat(mongoCollection.count()).isEqualTo(100);
        verify(defaultOfferService, times(88)).isObjectExist(eq("0_unit"), anyString());
        verifyNoMoreInteractions(defaultOfferService);
    }

    @Test
    @RunWithCustomExecutor
    public void testMigrationWithMissingFiles() throws Exception {

        // Given
        MongoDatabase mongoDatabase = mongoRule.getMongoDatabase();
        OfferLogR7MigrationProcess instance = new OfferLogR7MigrationProcess(defaultOfferService, mongoDatabase);
        MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(OFFER_LOG_COLLECTION_NAME);

        List<Document> documentsToInsert = new ArrayList<>();
        for (int i = 0; i < 100; i++) {

            OfferLog offerLog = new OfferLog("0_unit", "file" + i, "write")
                .setFormatVersion(OfferLogFormatVersion.V1);
            offerLog.setSequence((long) i);
            String json;
            try {
                json = JsonHandler.writeAsString(offerLog);
            } catch (InvalidParseOperationException exc) {
                throw new ContentAddressableStorageServerException("Cannot parse storage log", exc);
            }
            documentsToInsert.add(Document.parse(json));
        }
        mongoCollection.insertMany(documentsToInsert);

        doReturn(true).when(defaultOfferService).isObjectExist(eq("0_unit"), anyString());
        doReturn(false).when(defaultOfferService).isObjectExist("0_unit", "file1");
        doReturn(false).when(defaultOfferService).isObjectExist("0_unit", "file20");
        doReturn(false).when(defaultOfferService).isObjectExist("0_unit", "file90");

        // When
        instance.run(null);
        awaitTermination(instance);

        // Then
        assertThat(instance.isRunning()).isFalse();
        assertThat(instance.getMigrationStatus().getStatusCode()).isEqualTo(StatusCode.OK);
        assertThat(instance.getMigrationStatus().getCurrentOffset()).isEqualTo(99L);

        assertThat(mongoCollection.count()).isEqualTo(97);
        verify(defaultOfferService, times(100)).isObjectExist(eq("0_unit"), anyString());
        verifyNoMoreInteractions(defaultOfferService);
    }

    @Test
    @RunWithCustomExecutor
    public void testMigrationWithMissingFilesAndStartOffset() throws Exception {

        // Given
        MongoDatabase mongoDatabase = mongoRule.getMongoDatabase();
        OfferLogR7MigrationProcess instance = new OfferLogR7MigrationProcess(defaultOfferService, mongoDatabase);
        MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(OFFER_LOG_COLLECTION_NAME);

        List<Document> documentsToInsert = new ArrayList<>();
        for (int i = 0; i < 100; i++) {

            OfferLog offerLog = new OfferLog("0_unit", "file" + i, "write")
                .setFormatVersion(OfferLogFormatVersion.V1);
            offerLog.setSequence((long) i);
            String json;
            try {
                json = JsonHandler.writeAsString(offerLog);
            } catch (InvalidParseOperationException exc) {
                throw new ContentAddressableStorageServerException("Cannot parse storage log", exc);
            }
            documentsToInsert.add(Document.parse(json));
        }
        mongoCollection.insertMany(documentsToInsert);

        doReturn(true).when(defaultOfferService).isObjectExist(eq("0_unit"), anyString());
        doReturn(false).when(defaultOfferService).isObjectExist("0_unit", "file1");
        doReturn(false).when(defaultOfferService).isObjectExist("0_unit", "file20");
        doReturn(false).when(defaultOfferService).isObjectExist("0_unit", "file90");

        // When
        instance.run(12L);
        awaitTermination(instance);

        // Then
        assertThat(instance.isRunning()).isFalse();
        assertThat(instance.getMigrationStatus().getStatusCode()).isEqualTo(StatusCode.OK);
        assertThat(instance.getMigrationStatus().getCurrentOffset()).isEqualTo(99L);

        assertThat(mongoCollection.count()).isEqualTo(98);
        verify(defaultOfferService, times(88)).isObjectExist(eq("0_unit"), anyString());
        verifyNoMoreInteractions(defaultOfferService);
    }

    private void awaitTermination(OfferLogR7MigrationProcess instance) throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            if (!instance.isRunning()) {
                break;
            }
            Thread.sleep(100);
        }
        assertThat(instance.isRunning()).isFalse();
    }
}
