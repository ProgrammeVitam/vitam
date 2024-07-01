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
package fr.gouv.vitam.storage.offers.database;

import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoIterable;
import fr.gouv.vitam.common.database.server.mongodb.BsonHelper;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.time.LogicalClockRule;
import fr.gouv.vitam.storage.engine.common.collection.OfferCollections;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.offers.rest.OfferLogCompactionConfiguration;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageDatabaseException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.bson.Document;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;

import static fr.gouv.vitam.storage.engine.common.collection.OfferCollections.OFFER_LOG;
import static fr.gouv.vitam.storage.engine.common.model.OfferLogAction.DELETE;
import static fr.gouv.vitam.storage.engine.common.model.OfferLogAction.WRITE;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

public class OfferLogDatabaseServiceTest {

    private static final String PREFIX = GUIDFactory.newGUID().getId();

    @ClassRule
    public static MongoRule mongoRule = new MongoRule(MongoDbAccess.getMongoClientSettingsBuilder());

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Rule
    public LogicalClockRule logicalClock = new LogicalClockRule();

    private OfferLogDatabaseService service;

    @AfterClass
    public static void after() {
        cleanDatabase();
        mongoRule.handleAfter();
    }

    private static void cleanDatabase() {
        mongoRule.getMongoDatabase().getCollection(OFFER_LOG.getName()).deleteMany(new Document());
        OfferCollections collectionPrefixed = OFFER_LOG;
        collectionPrefixed.setPrefix(PREFIX);
        mongoRule.getMongoDatabase().getCollection(collectionPrefixed.getName()).deleteMany(new Document());
    }

    @Before
    public void before() {
        cleanDatabase();
        service = new OfferLogDatabaseService(mongoRule.getMongoDatabase().getCollection(OFFER_LOG.getName()));
    }

    @Test
    public void should_save_one_offer_log() throws Exception {
        // Given // When
        service.save("containerName", "fileName", DELETE, 42);

        // Then
        OfferLog offerLog = getFirstLogFromDb();
        assertThat(offerLog.getFileName()).isEqualTo("fileName");
        assertThat(offerLog.getContainer()).isEqualTo("containerName");
        assertThat(offerLog.getSequence()).isEqualTo(42);
        assertThat(offerLog.getAction()).isEqualTo(DELETE);
    }

    @Test
    public void should_throw_error_when_cannot_save_offer_log() {
        // Given
        MongoCollection<Document> collection = mock(MongoCollection.class);
        doThrow(MongoWriteException.class).when(collection).insertOne(any());
        OfferLogDatabaseService offerLogDatabaseService = new OfferLogDatabaseService(collection);

        // When
        ThrowingCallable save = () -> offerLogDatabaseService.save("containerName", "fileName", DELETE, 42);

        // Then
        assertThatThrownBy(save).isInstanceOf(ContentAddressableStorageDatabaseException.class);
    }

    @Test
    public void should_bulk_save_offer_log() throws Exception {
        // Given // When
        service.bulkSave("containerName", Collections.singletonList("batman"), DELETE, 15);

        // Then
        OfferLog offerLog = getFirstLogFromDb();
        assertThat(offerLog.getFileName()).isEqualTo("batman");
        assertThat(offerLog.getContainer()).isEqualTo("containerName");
        assertThat(offerLog.getSequence()).isEqualTo(15);
        assertThat(offerLog.getAction()).isEqualTo(DELETE);
    }

    @Test
    public void should_throw_error_when_cannot_bulk_save_offer_log() {
        // Given
        MongoCollection<Document> collection = mock(MongoCollection.class);
        doThrow(MongoWriteException.class).when(collection).insertMany(anyList(), any());
        OfferLogDatabaseService offerLogDatabaseService = new OfferLogDatabaseService(collection);

        // When
        ThrowingCallable save = () ->
            offerLogDatabaseService.bulkSave("containerName", Collections.singletonList("robin"), DELETE, 15);

        // Then
        assertThatThrownBy(save).isInstanceOf(ContentAddressableStorageDatabaseException.class);
    }

    @Test
    public void should_get_descending_offer_logs() throws Exception {
        // Given
        service.save("container", "fileName1", DELETE, 4);
        service.save("container", "fileName2", DELETE, 5);
        service.save("container", "fileName3", DELETE, 6);
        service.save("container", "fileName4", DELETE, 7);
        service.save("containerGotham", "batman", DELETE, 1);

        // When
        Iterable<OfferLog> descendingOfferLogs = service.getDescendingOfferLogsBy("container", 6L, 3);

        // Then
        assertThat(descendingOfferLogs).hasSize(3);
        assertThat(descendingOfferLogs).extracting(OfferLog::getSequence).containsExactly(6L, 5L, 4L);
        assertThat(descendingOfferLogs)
            .extracting(OfferLog::getFileName)
            .containsExactly("fileName3", "fileName2", "fileName1");
        assertThat(descendingOfferLogs).extracting(OfferLog::getAction).containsOnly(DELETE);
    }

    @Test
    public void should_get_descending_offer_logs_with_no_offset() throws Exception {
        // Given
        service.save("container", "fileName1", DELETE, 4);
        service.save("container", "fileName2", DELETE, 5);
        service.save("container", "fileName3", DELETE, 6);
        service.save("container", "fileName4", DELETE, 7);
        service.save("containerGotham", "batman", DELETE, 1);

        // When
        Iterable<OfferLog> descendingOfferLogs = service.getDescendingOfferLogsBy("container", null, 3);

        // Then
        assertThat(descendingOfferLogs).hasSize(3);
        assertThat(descendingOfferLogs).extracting(OfferLog::getSequence).containsExactly(7L, 6L, 5L);
        assertThat(descendingOfferLogs)
            .extracting(OfferLog::getFileName)
            .containsExactly("fileName4", "fileName3", "fileName2");
        assertThat(descendingOfferLogs).extracting(OfferLog::getAction).containsOnly(DELETE);
    }

    @Test
    public void should_get_ascending_offer_logs() throws Exception {
        // Given
        service.save("container", "fileName1", DELETE, 4);
        service.save("container", "fileName2", DELETE, 5);
        service.save("container", "fileName3", DELETE, 6);
        service.save("container", "fileName4", DELETE, 7);
        service.save("containerGotham", "batman", DELETE, 1);

        // When
        Iterable<OfferLog> ascendingOfferLogs = service.getAscendingOfferLogsBy("container", 5L, 3);

        // Then
        assertThat(ascendingOfferLogs).hasSize(3);
        assertThat(ascendingOfferLogs).extracting(OfferLog::getSequence).containsExactly(5L, 6L, 7L);
        assertThat(ascendingOfferLogs)
            .extracting(OfferLog::getFileName)
            .containsExactly("fileName2", "fileName3", "fileName4");
        assertThat(ascendingOfferLogs).extracting(OfferLog::getAction).containsOnly(DELETE);
    }

    @Test
    public void should_get_ascending_offer_logs_with_no_offset() throws Exception {
        // Given
        service.save("container", "fileName1", DELETE, 4);
        service.save("container", "fileName2", DELETE, 5);
        service.save("container", "fileName3", DELETE, 6);
        service.save("container", "fileName4", DELETE, 7);
        service.save("containerGotham", "batman", DELETE, 1);

        // When
        Iterable<OfferLog> ascendingOfferLogs = service.getAscendingOfferLogsBy("container", null, 3);

        // Then
        assertThat(ascendingOfferLogs).hasSize(3);
        assertThat(ascendingOfferLogs).extracting(OfferLog::getSequence).containsExactly(4L, 5L, 6L);
        assertThat(ascendingOfferLogs)
            .extracting(OfferLog::getFileName)
            .containsExactly("fileName1", "fileName2", "fileName3");
        assertThat(ascendingOfferLogs).extracting(OfferLog::getAction).containsOnly(DELETE);
    }

    @Test
    public void should_get_expired_offer_logs()
        throws ContentAddressableStorageServerException, ContentAddressableStorageDatabaseException {
        // Given
        OfferLogCompactionConfiguration request = new OfferLogCompactionConfiguration(3, MINUTES, 10);

        String firstFileName = "MY_FIRST_FILE";
        service.save("Container1", firstFileName, WRITE, 1L);

        logicalClock.logicalSleep(3, ChronoUnit.MINUTES);

        service.bulkSave("Container1", Arrays.asList("file1", "file2", "file3"), WRITE, 2L);

        // When
        Iterable<OfferLog> logs = service.getExpiredOfferLogByContainer(
            request.getExpirationValue(),
            request.getExpirationUnit()
        );

        // Then
        assertThat(logs).extracting(OfferLog::getFileName).containsOnly(firstFileName);
    }

    @Test
    public void should_get_expired_offer_logs_return_empty_logs_when_saved_in_expiration_range()
        throws ContentAddressableStorageServerException, ContentAddressableStorageDatabaseException {
        // Given
        OfferLogCompactionConfiguration request = new OfferLogCompactionConfiguration(3, SECONDS, 10);

        service.bulkSave("Container1", Arrays.asList("file1", "file2", "file3"), WRITE, 2L);

        // When
        Iterable<OfferLog> logs = service.getExpiredOfferLogByContainer(
            request.getExpirationValue(),
            request.getExpirationUnit()
        );

        // Then
        assertThat(logs).isEmpty();
    }

    @Test
    public void should_get_offer_logs_by_container()
        throws ContentAddressableStorageServerException, ContentAddressableStorageDatabaseException {
        // Given
        OfferLogCompactionConfiguration request = new OfferLogCompactionConfiguration(0, MILLIS, 10);

        service.save("Container1", "MY_FIRST_FILE1", WRITE, 1L);
        service.save("Container2", "MY_FIRST_FILE2", WRITE, 2L);
        service.save("Container1", "MY_FIRST_FILE3", WRITE, 3L);
        service.save("Container3", "MY_FIRST_FILE4", WRITE, 4L);
        service.save("Container1", "MY_FIRST_FILE5", WRITE, 5L);

        // When
        Iterable<OfferLog> logs = service.getExpiredOfferLogByContainer(
            request.getExpirationValue(),
            request.getExpirationUnit()
        );

        // Then
        assertThat(logs)
            .extracting(OfferLog::getContainer)
            .containsExactly("Container1", "Container1", "Container1", "Container2", "Container3");
    }

    public OfferLog getFirstLogFromDb() {
        return getOfferLogs().first();
    }

    public MongoIterable<OfferLog> getOfferLogs() {
        return mongoRule.getMongoDatabase().getCollection(OFFER_LOG.getName()).find().map(this::getOfferLog);
    }

    public OfferLog getOfferLog(Document document) {
        try {
            return BsonHelper.fromDocumentToObject(document, OfferLog.class);
        } catch (InvalidParseOperationException e) {
            throw new VitamRuntimeException(e);
        }
    }
}
