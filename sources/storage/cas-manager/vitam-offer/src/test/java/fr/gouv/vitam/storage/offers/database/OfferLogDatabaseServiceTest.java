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
package fr.gouv.vitam.storage.offers.database;

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.storage.engine.common.collection.OfferCollections;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.OfferLogAction;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageDatabaseException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import org.apache.commons.collections4.IteratorUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OfferLogDatabaseServiceTest {

    private static final String CONTAINER_OBJECT_0 = "object_0";
    private static final String CONTAINER_OBJECT_1 = "object_1";
    private static final String PREFIX = GUIDFactory.newGUID().getId();

    @ClassRule
    public static MongoRule mongoRule = new MongoRule(VitamCollection.getMongoClientOptions());

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private OfferSequenceDatabaseService offerSequenceDatabaseService;

    private OfferLogDatabaseService offerLogDatabaseService;

    @BeforeClass
    public static void setUpBeforeClass() {
        for (OfferCollections o : OfferCollections.values()) {
            o.setPrefix(PREFIX);
            mongoRule.addCollectionToBePurged(o.getName());
        }
    }

    @AfterClass
    public static void afterClass() {
        mongoRule.handleAfterClass();
    }

    @After
    public void after() {
        mongoRule.handleAfter();
    }

    @Before
    public void setUp() {
        offerLogDatabaseService =
            new OfferLogDatabaseService(offerSequenceDatabaseService, mongoRule.getMongoDatabase());
    }

    @Test
    public void should_increment_sequence_when_save_twice_the_same_document()
        throws ContentAddressableStorageServerException, ContentAddressableStorageDatabaseException {
        // given
        when(offerSequenceDatabaseService.getNextSequence(OfferSequenceDatabaseService.BACKUP_LOG_SEQUENCE_ID))
            .thenReturn(1L)
            .thenReturn(2L);
        // when
        offerLogDatabaseService.save(CONTAINER_OBJECT_0, "object_name_0.json", OfferLogAction.WRITE);
        offerLogDatabaseService.save(CONTAINER_OBJECT_0, "object_name_0.json", OfferLogAction.WRITE);
        // then
        verify(offerSequenceDatabaseService, Mockito.times(2))
            .getNextSequence(OfferSequenceDatabaseService.BACKUP_LOG_SEQUENCE_ID);

        Document firstOfferLog = mongoRule.getMongoCollection(OfferCollections.OFFER_LOG.getName())
            .find(Filters.and(Filters.eq("FileName", "object_name_0.json"), Filters.eq("Sequence", 1L)))
            .first();
        assertThat(firstOfferLog.get("FileName")).isEqualTo("object_name_0.json");
        assertThat(firstOfferLog.get("Sequence")).isEqualTo(1);
        assertThat(firstOfferLog.get("Container")).isEqualTo(CONTAINER_OBJECT_0);
        Document secondOfferLog = mongoRule.getMongoCollection(OfferCollections.OFFER_LOG.getName())
            .find(Filters.and(Filters.eq("FileName", "object_name_0.json"), Filters.eq("Sequence", 2L)))
            .first();
        assertThat(secondOfferLog.get("FileName")).isEqualTo("object_name_0.json");
        assertThat(secondOfferLog.get("Sequence")).isEqualTo(2);
        assertThat(secondOfferLog.get("Container")).isEqualTo(CONTAINER_OBJECT_0);
    }

    @Test
    public void should_increment_sequence_when_save_two_different_container_documents()
        throws ContentAddressableStorageServerException, ContentAddressableStorageDatabaseException {
        // given
        when(offerSequenceDatabaseService.getNextSequence(OfferSequenceDatabaseService.BACKUP_LOG_SEQUENCE_ID))
            .thenReturn(1L)
            .thenReturn(2L);
        // when
        offerLogDatabaseService.save(CONTAINER_OBJECT_0, "object_name_0.json", OfferLogAction.WRITE);
        offerLogDatabaseService.save(CONTAINER_OBJECT_1, "object_name_1.json", OfferLogAction.WRITE);
        // then
        verify(offerSequenceDatabaseService, Mockito.times(2))
            .getNextSequence(OfferSequenceDatabaseService.BACKUP_LOG_SEQUENCE_ID);

        Document firstOfferLog = mongoRule.getMongoCollection(OfferCollections.OFFER_LOG.getName())
            .find(Filters.and(Filters.eq("FileName", "object_name_0.json"), Filters.eq("Sequence", 1L)))
            .first();
        assertThat(firstOfferLog.get("FileName")).isEqualTo("object_name_0.json");
        assertThat(firstOfferLog.get("Sequence")).isEqualTo(1);
        assertThat(firstOfferLog.get("Container")).isEqualTo(CONTAINER_OBJECT_0);

        Document secondOfferLog = mongoRule.getMongoCollection(OfferCollections.OFFER_LOG.getName())
            .find(Filters.and(Filters.eq("FileName", "object_name_1.json"), Filters.eq("Sequence", 2L)))
            .first();
        assertThat(secondOfferLog.get("FileName")).isEqualTo("object_name_1.json");
        assertThat(secondOfferLog.get("Sequence")).isEqualTo(2);
        assertThat(secondOfferLog.get("Container")).isEqualTo(CONTAINER_OBJECT_1);
    }

    @Test
    public void should_sequence_valid_when_save_with_long_sequence()
        throws ContentAddressableStorageServerException, ContentAddressableStorageDatabaseException {
        // given
        long longSequence = Integer.MAX_VALUE + 1L;
        when(offerSequenceDatabaseService.getNextSequence(OfferSequenceDatabaseService.BACKUP_LOG_SEQUENCE_ID))
            .thenReturn(longSequence);
        // when
        offerLogDatabaseService.save(CONTAINER_OBJECT_0, "object_name_0.json", OfferLogAction.WRITE);
        // then
        verify(offerSequenceDatabaseService, Mockito.times(1))
            .getNextSequence(OfferSequenceDatabaseService.BACKUP_LOG_SEQUENCE_ID);

        Document firstOfferLog = mongoRule.getMongoCollection(OfferCollections.OFFER_LOG.getName())
            .find(Filters.and(Filters.eq("FileName", "object_name_0.json"))).first();
        assertThat(firstOfferLog.get("FileName")).isEqualTo("object_name_0.json");
        assertThat(firstOfferLog.get("Sequence")).isEqualTo(longSequence);
        assertThat(firstOfferLog.get("Container")).isEqualTo(CONTAINER_OBJECT_0);
    }


    @Test
    public void should_throw_ContentAddressableStorageDatabaseException_on_save_when_mongo_throws_MongoException()
        throws ContentAddressableStorageServerException, ContentAddressableStorageDatabaseException {
        // given
        MongoDatabase mongoDatabase = Mockito.mock(MongoDatabase.class);
        MongoCollection<Document> mongoCollection = Mockito.mock(MongoCollection.class);
        Mockito.when(mongoDatabase.getCollection(Mockito.any())).thenReturn(mongoCollection);
        Mockito.doThrow(new MongoException("mongo error")).when(mongoCollection).insertOne(Mockito.any(Document.class));
        offerLogDatabaseService = new OfferLogDatabaseService(offerSequenceDatabaseService, mongoDatabase);

        // when + then
        assertThatCode(() -> {
            offerLogDatabaseService.save(CONTAINER_OBJECT_0, "object_name_0.json", OfferLogAction.WRITE);
        }).isInstanceOf(ContentAddressableStorageDatabaseException.class);
    }

    @Test
    public void should_get_two_document_when_get_offer_log_from_2_limit_2_ASC()
        throws ContentAddressableStorageServerException, ContentAddressableStorageDatabaseException {
        // given
        when(offerSequenceDatabaseService.getNextSequence(OfferSequenceDatabaseService.BACKUP_LOG_SEQUENCE_ID))
            .thenReturn(1L)
            .thenReturn(2L).thenReturn(3L).thenReturn(4L).thenReturn(5L);
        offerLogDatabaseService.save(CONTAINER_OBJECT_0, "object_name_0.json", OfferLogAction.WRITE);
        offerLogDatabaseService.save(CONTAINER_OBJECT_0, "object_name_1.json", OfferLogAction.WRITE);
        offerLogDatabaseService.save(CONTAINER_OBJECT_0, "object_name_2.json", OfferLogAction.WRITE);
        offerLogDatabaseService.save(CONTAINER_OBJECT_0, "object_name_3.json", OfferLogAction.WRITE);
        offerLogDatabaseService.save(CONTAINER_OBJECT_0, "object_name_4.json", OfferLogAction.WRITE);
        // when
        List<OfferLog> offerLogs = offerLogDatabaseService.searchOfferLog(CONTAINER_OBJECT_0, 1L, 2, Order.ASC);
        // then
        verify(offerSequenceDatabaseService, Mockito.times(5))
            .getNextSequence(OfferSequenceDatabaseService.BACKUP_LOG_SEQUENCE_ID);
        assertThat(offerLogs.size()).isEqualTo(2);
        assertThat(offerLogs.get(0)).isNotNull();
        assertThat(offerLogs.get(0).getSequence()).isEqualTo(1L);
        assertThat(offerLogs.get(1)).isNotNull();
        assertThat(offerLogs.get(1).getSequence()).isEqualTo(2L);
    }

    @Test
    public void should_get_two_document_when_get_offer_log_from_2_limit_2_DESC()
        throws ContentAddressableStorageServerException, ContentAddressableStorageDatabaseException {
        // given
        when(offerSequenceDatabaseService.getNextSequence(OfferSequenceDatabaseService.BACKUP_LOG_SEQUENCE_ID))
            .thenReturn(1L)
            .thenReturn(2L).thenReturn(3L).thenReturn(4L).thenReturn(5L);
        offerLogDatabaseService.save(CONTAINER_OBJECT_0, "object_name_0.json", OfferLogAction.WRITE);
        offerLogDatabaseService.save(CONTAINER_OBJECT_0, "object_name_1.json", OfferLogAction.WRITE);
        offerLogDatabaseService.save(CONTAINER_OBJECT_0, "object_name_2.json", OfferLogAction.WRITE);
        offerLogDatabaseService.save(CONTAINER_OBJECT_0, "object_name_3.json", OfferLogAction.WRITE);
        offerLogDatabaseService.save(CONTAINER_OBJECT_0, "object_name_4.json", OfferLogAction.WRITE);
        // when
        List<OfferLog> offerLogs = offerLogDatabaseService.searchOfferLog(CONTAINER_OBJECT_0, 2L, 2, Order.DESC);
        // then
        verify(offerSequenceDatabaseService, Mockito.times(5))
            .getNextSequence(OfferSequenceDatabaseService.BACKUP_LOG_SEQUENCE_ID);
        assertThat(offerLogs.size()).isEqualTo(2);
        assertThat(offerLogs.get(0)).isNotNull();
        assertThat(offerLogs.get(0).getSequence()).isEqualTo(2L);
        assertThat(offerLogs.get(1)).isNotNull();
        assertThat(offerLogs.get(1).getSequence()).isEqualTo(1L);
    }

    @Test
    public void get_documents_when_get_offer_log_from_0_limit_N_ASC_DESC()
        throws ContentAddressableStorageServerException, ContentAddressableStorageDatabaseException {
        // given
        when(offerSequenceDatabaseService.getNextSequence(OfferSequenceDatabaseService.BACKUP_LOG_SEQUENCE_ID))
            .thenReturn(1L)
            .thenReturn(2L)
            .thenReturn(3L)
            .thenReturn(4L)
            .thenReturn(5L);

        offerLogDatabaseService.save(CONTAINER_OBJECT_0, "object_name_0.json", OfferLogAction.WRITE);
        offerLogDatabaseService.save(CONTAINER_OBJECT_0, "object_name_1.json", OfferLogAction.WRITE);
        offerLogDatabaseService.save(CONTAINER_OBJECT_0, "object_name_2.json", OfferLogAction.WRITE);
        offerLogDatabaseService.save(CONTAINER_OBJECT_1, "object_name_1.json", OfferLogAction.WRITE);
        offerLogDatabaseService.save(CONTAINER_OBJECT_1, "object_name_2.json", OfferLogAction.WRITE);

        // then
        verify(offerSequenceDatabaseService, Mockito.times(5))
            .getNextSequence(OfferSequenceDatabaseService.BACKUP_LOG_SEQUENCE_ID);

        List<OfferLog> offerLogs = offerLogDatabaseService.searchOfferLog(CONTAINER_OBJECT_0, 0L, 1, Order.DESC);
        assertThat(offerLogs.size()).isEqualTo(0);

        offerLogs = offerLogDatabaseService.searchOfferLog(CONTAINER_OBJECT_1, 0L, 1, Order.ASC);
        assertThat(offerLogs.size()).isEqualTo(1);
        assertThat(offerLogs.get(0)).isNotNull();
        assertThat(offerLogs.get(0).getSequence()).isEqualTo(4L);
        assertThat(offerLogs.get(0).getFileName()).isEqualTo("object_name_1.json");
    }

    @Test
    public void should_get_document_with_the_last_sequence_by_container_when_get_offer_log_from_null_limit_1_DESC()
        throws ContentAddressableStorageServerException, ContentAddressableStorageDatabaseException {

        // given
        when(offerSequenceDatabaseService.getNextSequence(OfferSequenceDatabaseService.BACKUP_LOG_SEQUENCE_ID))
            .thenReturn(1L)
            .thenReturn(2L)
            .thenReturn(3L)
            .thenReturn(4L)
            .thenReturn(5L)
            .thenReturn(6L)
            .thenReturn(7L);

        // saving offerLog objects
        offerLogDatabaseService.save(CONTAINER_OBJECT_0, "object_name_0.json", OfferLogAction.WRITE);
        offerLogDatabaseService.save(CONTAINER_OBJECT_0, "object_name_1.json", OfferLogAction.WRITE);
        offerLogDatabaseService.save(CONTAINER_OBJECT_1, "object_name_0.json", OfferLogAction.WRITE);
        offerLogDatabaseService.save(CONTAINER_OBJECT_0, "object_name_2.json", OfferLogAction.WRITE);
        offerLogDatabaseService.save(CONTAINER_OBJECT_0, "object_name_3.json", OfferLogAction.WRITE);
        offerLogDatabaseService.save(CONTAINER_OBJECT_1, "object_name_1.json", OfferLogAction.WRITE);
        offerLogDatabaseService.save(CONTAINER_OBJECT_1, "object_name_2.json", OfferLogAction.WRITE);

        // when
        List<OfferLog> offerLogs = offerLogDatabaseService.searchOfferLog(CONTAINER_OBJECT_0, null, 1, Order.DESC);

        // then
        verify(offerSequenceDatabaseService, Mockito.times(7))
            .getNextSequence(OfferSequenceDatabaseService.BACKUP_LOG_SEQUENCE_ID);
        assertThat(offerLogs.size()).isEqualTo(1);
        assertThat(offerLogs.get(0)).isNotNull();
        assertThat(offerLogs.get(0).getSequence()).isEqualTo(5L);
        assertThat(offerLogs.get(0).getContainer()).isEqualTo(CONTAINER_OBJECT_0);
        assertThat(offerLogs.get(0).getFileName()).isEqualTo("object_name_3.json");

        // when
        offerLogs = offerLogDatabaseService.searchOfferLog(CONTAINER_OBJECT_1, null, 1, Order.DESC);

        // then
        verify(offerSequenceDatabaseService, Mockito.times(7))
            .getNextSequence(OfferSequenceDatabaseService.BACKUP_LOG_SEQUENCE_ID);
        assertThat(offerLogs.size()).isEqualTo(1);
        assertThat(offerLogs.get(0)).isNotNull();
        assertThat(offerLogs.get(0).getSequence()).isEqualTo(7L);
        assertThat(offerLogs.get(0).getContainer()).isEqualTo(CONTAINER_OBJECT_1);
        assertThat(offerLogs.get(0).getFileName()).isEqualTo("object_name_2.json");
    }

    @Test
    public void should_have_parse_error_when_document_invalid_time() {
        // given
        Document documentInvalid = new Document();
        documentInvalid.put("Sequence", 1L);
        documentInvalid.put("FileName", "object_name_0.json");
        documentInvalid.put("Container", CONTAINER_OBJECT_0);
        documentInvalid.put("Time", CONTAINER_OBJECT_0);
        mongoRule.getMongoCollection(OfferCollections.OFFER_LOG.getName()).insertOne(documentInvalid);

        // when + then
        assertThatCode(() -> {
            offerLogDatabaseService.searchOfferLog(CONTAINER_OBJECT_0, 0L, 1, Order.ASC);
        }).isInstanceOf(ContentAddressableStorageServerException.class);
    }

    @Test
    public void should_throw_ContentAddressableStorageDatabaseException_on_getOfferLog_when_mongo_throws_MongoException()
        throws ContentAddressableStorageServerException, ContentAddressableStorageDatabaseException {
        // given
        MongoDatabase mongoDatabase = Mockito.mock(MongoDatabase.class);
        MongoCollection<Document> mongoCollection = Mockito.mock(MongoCollection.class);
        Mockito.when(mongoDatabase.getCollection(Mockito.any())).thenReturn(mongoCollection);
        Mockito.when(mongoCollection.find(Mockito.any(Bson.class))).thenThrow(new MongoException("mongo error"));
        offerLogDatabaseService = new OfferLogDatabaseService(offerSequenceDatabaseService, mongoDatabase);

        // when + then
        assertThatCode(() -> {
            offerLogDatabaseService.searchOfferLog(CONTAINER_OBJECT_0, 0L, 1, Order.ASC);
        }).isInstanceOf(ContentAddressableStorageDatabaseException.class);
    }

    @Test
    public void should_append_sequence_when_bulk_save()
        throws ContentAddressableStorageServerException, ContentAddressableStorageDatabaseException {
        // given
        List<String> fileNames = Arrays.asList("object_name_1.json", "object_name_2.json", "object_name_3.json");
        long longSequence = Integer.MAX_VALUE + 10L;
        when(offerSequenceDatabaseService.getNextSequence(OfferSequenceDatabaseService.BACKUP_LOG_SEQUENCE_ID, (long)fileNames.size()))
            .thenReturn(longSequence);

        // when
        offerLogDatabaseService.bulkSave(CONTAINER_OBJECT_0, fileNames, OfferLogAction.WRITE);

        // then
        verify(offerSequenceDatabaseService, Mockito.times(1))
            .getNextSequence(OfferSequenceDatabaseService.BACKUP_LOG_SEQUENCE_ID, (long)fileNames.size());

        List<Document> documents =
            IteratorUtils.toList(mongoRule.getMongoCollection(OfferCollections.OFFER_LOG.getName())
                .find(Filters.and(Filters.in("FileName", fileNames))).sort(Sorts.ascending("FileName")).iterator());

        assertThat(documents).hasSize(fileNames.size());
        for (int i = 0; i < fileNames.size(); i++) {
            Document document = documents.get(i);
            assertThat(document.get("FileName")).isEqualTo(fileNames.get(i));
            assertThat(document.get("Sequence")).isEqualTo(longSequence + i);
            assertThat(document.get("Container")).isEqualTo(CONTAINER_OBJECT_0);
        }
    }
}
