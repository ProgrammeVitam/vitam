package fr.gouv.vitam.storage.offers.database;

import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.Sorts;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.database.server.mongodb.BsonHelper;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.storage.engine.common.collection.OfferCollections;
import fr.gouv.vitam.storage.engine.common.model.CompactedOfferLog;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import org.bson.Document;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.List;

import static fr.gouv.vitam.storage.engine.common.collection.OfferCollections.COMPACTED_OFFER_LOG;
import static fr.gouv.vitam.storage.engine.common.collection.OfferCollections.OFFER_LOG;
import static fr.gouv.vitam.storage.engine.common.model.OfferLogAction.WRITE;
import static org.assertj.core.api.Assertions.assertThat;

public class OfferLogAndCompactedOfferLogServiceTest {
    private static final String PREFIX = GUIDFactory.newGUID().getId();

    private OfferLogAndCompactedOfferLogService service;

    @ClassRule
    public static MongoRule mongoRule = new MongoRule(VitamCollection.getMongoClientOptions());

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Before
    public void before() {
        cleanDatabase();
        service = new OfferLogAndCompactedOfferLogService(
            mongoRule.getMongoDatabase().getCollection(OFFER_LOG.getName()),
            mongoRule.getMongoDatabase().getCollection(COMPACTED_OFFER_LOG.getName())
        );
    }

    @AfterClass
    public static void after() {
        cleanDatabase();
        mongoRule.handleAfter();
    }

    @Test
    public void should_save_and_delete_offer_log_and_compacted() throws Exception {
        // Given
        OfferLog offerLog1 = new OfferLog(1L, LocalDateUtil.now(), "Container0", "FILE1", WRITE);
        OfferLog offerLog2 = new OfferLog(2L, LocalDateUtil.now(), "Container1", "FILE2", WRITE);
        OfferLog offerLog3 = new OfferLog(3L, LocalDateUtil.now(), "Container1", "FILE3", WRITE);
        OfferLog offerLog4 = new OfferLog(4L, LocalDateUtil.now(), "Container0", "FILE4", WRITE);
        OfferLog offerLog5 = new OfferLog(5L, LocalDateUtil.now(), "Container1", "FILE5", WRITE);
        OfferLog offerLog6 = new OfferLog(6L, LocalDateUtil.now(), "Container1", "FILE6", WRITE);
        OfferLog offerLog7 = new OfferLog(7L, LocalDateUtil.now(), "Container0", "FILE7", WRITE);

        save(offerLog1);
        save(offerLog2);
        save(offerLog3);
        save(offerLog4);
        save(offerLog5);
        save(offerLog6);
        save(offerLog7);

        List<OfferLog> offerLogs = Arrays.asList(offerLog2, offerLog3, offerLog5);
        CompactedOfferLog compactedOfferLog = new CompactedOfferLog(2, 5, LocalDateUtil.now(), "Container1", offerLogs);

        // When
        service.almostTransactionalSaveAndDelete(compactedOfferLog, offerLogs);

        // Then
        assertThat(getCompactedOfferLogs()).containsExactly(compactedOfferLog);
        assertThat(getOfferLogs()).containsExactly(offerLog1, offerLog4, offerLog6, offerLog7);
    }

    private static void cleanDatabase() {
        mongoRule.getMongoDatabase().getCollection(OfferCollections.COMPACTED_OFFER_LOG.getName())
            .deleteMany(new Document());
        mongoRule.getMongoDatabase().getCollection(OfferCollections.OFFER_LOG.getName()).deleteMany(new Document());
        OfferCollections collectionPrefixed = OfferCollections.COMPACTED_OFFER_LOG;
        collectionPrefixed.setPrefix(PREFIX);
        mongoRule.getMongoDatabase().getCollection(collectionPrefixed.getName()).deleteMany(new Document());
        OfferCollections collectionLogPrefixed = OfferCollections.OFFER_LOG;
        collectionLogPrefixed.setPrefix(PREFIX);
        mongoRule.getMongoDatabase().getCollection(collectionLogPrefixed.getName()).deleteMany(new Document());
    }

    private void save(OfferLog offerLog) throws InvalidParseOperationException {
        mongoRule.getMongoDatabase()
            .getCollection(OFFER_LOG.getName())
            .insertOne(Document.parse(JsonHandler.writeAsString(offerLog)));
    }

    public MongoIterable<CompactedOfferLog> getCompactedOfferLogs() {
        return mongoRule.getMongoDatabase().getCollection(COMPACTED_OFFER_LOG.getName())
            .find()
            .map(this::getCompactedOfferLogs);
    }

    public CompactedOfferLog getCompactedOfferLogs(Document document) {
        try {
            return BsonHelper.fromDocumentToObject(document, CompactedOfferLog.class);
        } catch (InvalidParseOperationException e) {
            throw new VitamRuntimeException(e);
        }
    }

    public MongoIterable<OfferLog> getOfferLogs() {
        return mongoRule.getMongoDatabase().getCollection(OFFER_LOG.getName())
            .find()
            .sort(Sorts.ascending(OfferLog.SEQUENCE))
            .map(this::getOfferLog);
    }

    public OfferLog getOfferLog(Document document) {
        try {
            return BsonHelper.fromDocumentToObject(document, OfferLog.class);
        } catch (InvalidParseOperationException e) {
            throw new VitamRuntimeException(e);
        }
    }
}
