/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */

package fr.gouv.vitam.storage.offers.common.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.bson.Document;
import org.junit.Rule;
import org.junit.Test;

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageDatabaseException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

public class OfferSequenceServiceTest {

    private static final String DATABASE_NAME = "Vitam-test";

    @Rule
    public MongoRule mongoRule = new MongoRule(VitamCollection.getMongoClientOptions(), DATABASE_NAME,
        OfferSequenceDatabaseService.OFFER_SEQUENCE_COLLECTION);

    @Test
    public void testInitializeSequenceOnce() throws ContentAddressableStorageDatabaseException {
        OfferSequenceDatabaseService service = new OfferSequenceDatabaseService(mongoRule.getMongoDatabase());

        service.initSequences();
        assertThat(mongoRule.getMongoCollection(OfferSequenceDatabaseService.OFFER_SEQUENCE_COLLECTION).find()).hasSize(1)
            .extracting("Counter").containsExactly(0L);

        service.getNextSequence(OfferSequenceDatabaseService.BACKUP_LOG_SEQUENCE_ID);
        assertThat(mongoRule.getMongoCollection(OfferSequenceDatabaseService.OFFER_SEQUENCE_COLLECTION).find()).hasSize(1)
            .extracting("Counter").containsExactly(1L);

        service.initSequences();
        assertThat(mongoRule.getMongoCollection(OfferSequenceDatabaseService.OFFER_SEQUENCE_COLLECTION).find()).hasSize(1)
            .extracting("Counter").containsExactly(1L);
    }

    @Test
    public void testOfferSequenceService() throws ContentAddressableStorageDatabaseException {
        OfferSequenceDatabaseService service = new OfferSequenceDatabaseService(mongoRule.getMongoDatabase());

        service.initSequences();
        assertThat(mongoRule.getMongoCollection(OfferSequenceDatabaseService.OFFER_SEQUENCE_COLLECTION).find()).hasSize(1)
            .extracting("Counter").containsExactly(0L);

        Long count = service.getNextSequence(OfferSequenceDatabaseService.BACKUP_LOG_SEQUENCE_ID);
        assertThat(mongoRule.getMongoCollection(OfferSequenceDatabaseService.OFFER_SEQUENCE_COLLECTION).find()).hasSize(1)
            .extracting("Counter").containsExactly(1L);
        assertThat(count).isEqualTo(1L);

        count = service.getNextSequence(OfferSequenceDatabaseService.BACKUP_LOG_SEQUENCE_ID);
        assertThat(mongoRule.getMongoCollection(OfferSequenceDatabaseService.OFFER_SEQUENCE_COLLECTION).find()).hasSize(1)
            .extracting("Counter").containsExactly(2L);
        assertThat(count).isEqualTo(2L);
    }

    @Test
    public void testOfferSequenceServiceException() {
        OfferSequenceDatabaseService service = new OfferSequenceDatabaseService(mongoRule.getMongoDatabase());
        assertThatCode(() -> {
            service.getNextSequence(OfferSequenceDatabaseService.BACKUP_LOG_SEQUENCE_ID);
        }).isInstanceOf(ContentAddressableStorageDatabaseException.class);

        mongoRule.getMongoCollection(OfferSequenceDatabaseService.OFFER_SEQUENCE_COLLECTION).drop();
        assertThatCode(() -> {
            service.getNextSequence(OfferSequenceDatabaseService.BACKUP_LOG_SEQUENCE_ID);
        }).isInstanceOf(ContentAddressableStorageDatabaseException.class);
    }

    @Test
    public void should_throw_ContentAddressableStorageDatabaseException_on_getOfferLog_when_mongo_throws_MongoException()
        throws ContentAddressableStorageServerException, ContentAddressableStorageDatabaseException {
        // given
        MongoDatabase mongoDatabase = mock(MongoDatabase.class);
        MongoCollection<Document> mongoCollection = mock(MongoCollection.class);
        when(mongoDatabase.getCollection(any())).thenReturn(mongoCollection);
        when(mongoCollection.findOneAndUpdate(any(), any(), any())).thenThrow(new MongoException("mongo error"));
        OfferSequenceDatabaseService service = new OfferSequenceDatabaseService(mongoDatabase);

        // when + then
        assertThatCode(() -> {
            service.getNextSequence(OfferSequenceDatabaseService.BACKUP_LOG_SEQUENCE_ID);
        }).isInstanceOf(ContentAddressableStorageDatabaseException.class);
    }
}
