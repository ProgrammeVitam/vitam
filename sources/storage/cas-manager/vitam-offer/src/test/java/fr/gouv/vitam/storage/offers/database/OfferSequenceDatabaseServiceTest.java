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
import com.mongodb.client.model.FindOneAndUpdateOptions;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.storage.engine.common.collection.OfferCollections;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageDatabaseException;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static fr.gouv.vitam.storage.engine.common.collection.OfferCollections.OFFER_SEQUENCE;
import static fr.gouv.vitam.storage.engine.common.model.OfferSequence.COUNTER_FIELD;
import static fr.gouv.vitam.storage.offers.database.OfferSequenceDatabaseService.BACKUP_LOG_SEQUENCE_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OfferSequenceDatabaseServiceTest {

    private static final String PREFIX = GUIDFactory.newGUID().getId();

    @ClassRule
    public static MongoRule mongoRule = new MongoRule(MongoDbAccess.getMongoClientSettingsBuilder());

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private OfferSequenceDatabaseService service;

    @AfterClass
    public static void after() {
        cleanDatabase();
        mongoRule.handleAfter();
    }

    @Before
    public void before() {
        cleanDatabase();
        service = new OfferSequenceDatabaseService(
            mongoRule.getMongoDatabase().getCollection(OFFER_SEQUENCE.getName())
        );
    }

    @Test
    public void should_get_next_sequence_with_increment_of_1() throws Exception {
        // Given / When
        service.getNextSequence(BACKUP_LOG_SEQUENCE_ID);

        // Then
        assertThat(getFirstSequenceInDb()).isEqualTo(1);
    }

    @Test
    public void should_get_next_sequence_with_increment_of_10() throws Exception {
        // Given / When
        service.getNextSequence(BACKUP_LOG_SEQUENCE_ID, 10);

        // Then
        assertThat(getFirstSequenceInDb()).isEqualTo(10);
    }

    @Test
    public void should_throw_error_when_no_sequence() throws Exception {
        // Given
        MongoCollection<Document> collection = mock(MongoCollection.class);
        when(
            collection.findOneAndUpdate(any(Bson.class), any(Bson.class), any(FindOneAndUpdateOptions.class))
        ).thenReturn(null);
        OfferSequenceDatabaseService service = new OfferSequenceDatabaseService(collection);

        // When
        ThrowingCallable getSequence = () -> service.getNextSequence(BACKUP_LOG_SEQUENCE_ID);

        // Then
        assertThatThrownBy(getSequence).isInstanceOf(ContentAddressableStorageDatabaseException.class);
    }

    @Test
    public void should_throw_error_when_mongo_exception() throws Exception {
        // Given
        MongoCollection<Document> collection = mock(MongoCollection.class);
        when(
            collection.findOneAndUpdate(any(Bson.class), any(Bson.class), any(FindOneAndUpdateOptions.class))
        ).thenThrow(MongoWriteException.class);
        OfferSequenceDatabaseService service = new OfferSequenceDatabaseService(collection);

        // When
        ThrowingCallable getSequence = () -> service.getNextSequence(BACKUP_LOG_SEQUENCE_ID);

        // Then
        assertThatThrownBy(getSequence).isInstanceOf(ContentAddressableStorageDatabaseException.class);
    }

    @Test
    public void should_increment_for_bulk() throws ContentAddressableStorageDatabaseException {
        assertThat(mongoRule.getMongoCollection(OfferCollections.OFFER_SEQUENCE.getName()).find()).hasSize(0);

        long nextSequence1 = service.getNextSequence(OfferSequenceDatabaseService.BACKUP_LOG_SEQUENCE_ID, 5L);
        assertThat(nextSequence1).isEqualTo(1L);
        assertThat(mongoRule.getMongoCollection(OfferCollections.OFFER_SEQUENCE.getName()).find())
            .hasSize(1)
            .extracting("Counter")
            .containsExactly(5L);

        long nextSequence2 = service.getNextSequence(OfferSequenceDatabaseService.BACKUP_LOG_SEQUENCE_ID, 3L);
        assertThat(nextSequence2).isEqualTo(6L);
        assertThat(mongoRule.getMongoCollection(OfferCollections.OFFER_SEQUENCE.getName()).find())
            .hasSize(1)
            .extracting("Counter")
            .containsExactly(8L);
    }

    @Test
    public void should_test_sequence() throws ContentAddressableStorageDatabaseException {
        assertThat(mongoRule.getMongoCollection(OfferCollections.OFFER_SEQUENCE.getName()).find()).hasSize(0);

        service.getNextSequence(OfferSequenceDatabaseService.BACKUP_LOG_SEQUENCE_ID);
        assertThat(mongoRule.getMongoCollection(OfferCollections.OFFER_SEQUENCE.getName()).find())
            .hasSize(1)
            .extracting("Counter")
            .containsExactly(1L);

        assertThat(mongoRule.getMongoCollection(OfferCollections.OFFER_SEQUENCE.getName()).find())
            .hasSize(1)
            .extracting("Counter")
            .containsExactly(1L);
    }

    public Long getFirstSequenceInDb() {
        return mongoRule
            .getMongoDatabase()
            .getCollection(OFFER_SEQUENCE.getName())
            .find()
            .map(d -> d.getLong(COUNTER_FIELD))
            .first();
    }

    private static void cleanDatabase() {
        mongoRule
            .getMongoDatabase()
            .getCollection(OfferCollections.OFFER_SEQUENCE.getName())
            .deleteMany(new Document());
        OfferCollections collectionPrefixed = OfferCollections.OFFER_SEQUENCE;
        collectionPrefixed.setPrefix(PREFIX);
        mongoRule.getMongoDatabase().getCollection(collectionPrefixed.getName()).deleteMany(new Document());
    }
}
