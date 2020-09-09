package fr.gouv.vitam.storage.offers.database;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.storage.engine.common.collection.OfferCollections;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.CompactedOfferLog;
import org.bson.Document;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.Collections;

import static fr.gouv.vitam.storage.engine.common.collection.OfferCollections.COMPACTED_OFFER_LOG;
import static org.assertj.core.api.Assertions.assertThat;

public class OfferLogCompactionDatabaseServiceTest {
    private static final String PREFIX = GUIDFactory.newGUID().getId();

    private OfferLogCompactionDatabaseService service;

    @ClassRule
    public static MongoRule mongoRule = new MongoRule(VitamCollection.getMongoClientOptions());

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Before
    public void before() {
        cleanDatabase();
        service = new OfferLogCompactionDatabaseService(mongoRule.getMongoDatabase().getCollection(COMPACTED_OFFER_LOG.getName()));
    }

    @AfterClass
    public static void after() {
        cleanDatabase();
        mongoRule.handleAfter();
    }

    @Test
    public void should_get_descending_offer_log_compaction_in_range() throws Exception {
        // Given
        CompactedOfferLog compactedOfferLog1 = new CompactedOfferLog(
            40,
            40,
            LocalDateUtil.now(),
            "containerName",
            Collections.singletonList(new OfferLog())
        );
        CompactedOfferLog compactedOfferLog2 = new CompactedOfferLog(
            41,
            42,
            LocalDateUtil.now(),
            "containerName",
            Arrays.asList(new OfferLog(), new OfferLog())
        );
        CompactedOfferLog compactedOfferLog3 = new CompactedOfferLog(
            1,
            5,
            LocalDateUtil.now(),
            "OTHER_containerName",
            Collections.singletonList(new OfferLog())
        );
        save(compactedOfferLog1);
        save(compactedOfferLog2);
        save(compactedOfferLog3);

        // When
        Iterable<CompactedOfferLog> compactionLogs = service.getDescendingOfferLogCompactionBy("containerName", 42L);

        // Then
        assertThat(compactionLogs).hasSize(2);
        assertThat(compactionLogs).extracting(CompactedOfferLog::getSequenceStart).containsExactly(41L, 40L);
        assertThat(compactionLogs).extracting(CompactedOfferLog::getSequenceEnd).containsExactly(42L, 40L);
        assertThat(compactionLogs).extracting(o -> o.getLogs().size()).containsExactly(2, 1);
    }

    @Test
    public void should_get_descending_offer_log_compaction_with_no_range() throws Exception {
        // Given
        CompactedOfferLog compactedOfferLog1 = new CompactedOfferLog(
            40,
            40,
            LocalDateUtil.now(),
            "containerName",
            Collections.singletonList(new OfferLog())
        );
        CompactedOfferLog compactedOfferLog2 = new CompactedOfferLog(
            41,
            42,
            LocalDateUtil.now(),
            "containerName",
            Arrays.asList(new OfferLog(), new OfferLog())
        );
        CompactedOfferLog compactedOfferLog3 = new CompactedOfferLog(
            43,
            45,
            LocalDateUtil.now(),
            "containerName",
            Arrays.asList(new OfferLog(), new OfferLog(), new OfferLog())
        );
        CompactedOfferLog compactedOfferLog4 = new CompactedOfferLog(
            1,
            5,
            LocalDateUtil.now(),
            "OTHER_containerName",
            Collections.singletonList(new OfferLog())
        );
        save(compactedOfferLog1);
        save(compactedOfferLog2);
        save(compactedOfferLog3);
        save(compactedOfferLog4);

        // When
        Iterable<CompactedOfferLog> compactionLogs = service.getDescendingOfferLogCompactionBy("containerName", null);

        // Then
        assertThat(compactionLogs).hasSize(3);
        assertThat(compactionLogs).extracting(CompactedOfferLog::getSequenceStart).containsExactly(43L, 41L, 40L);
        assertThat(compactionLogs).extracting(CompactedOfferLog::getSequenceEnd).containsExactly(45L, 42L, 40L);
        assertThat(compactionLogs).extracting(o -> o.getLogs().size()).containsExactly(3, 2, 1);
    }

    @Test
    public void should_get_ascending_offer_log_compaction_in_range() throws Exception {
        // Given
        CompactedOfferLog compactedOfferLog1 = new CompactedOfferLog(
            40,
            40,
            LocalDateUtil.now(),
            "containerName",
            Collections.singletonList(new OfferLog())
        );
        CompactedOfferLog compactedOfferLog2 = new CompactedOfferLog(
            41,
            42,
            LocalDateUtil.now(),
            "containerName",
            Arrays.asList(new OfferLog(), new OfferLog())
        );
        CompactedOfferLog compactedOfferLog3 = new CompactedOfferLog(
            43,
            45,
            LocalDateUtil.now(),
            "containerName",
            Arrays.asList(new OfferLog(), new OfferLog(), new OfferLog())
        );
        CompactedOfferLog compactedOfferLog4 = new CompactedOfferLog(
            1,
            5,
            LocalDateUtil.now(),
            "OTHER_containerName",
            Collections.singletonList(new OfferLog())
        );
        save(compactedOfferLog1);
        save(compactedOfferLog2);
        save(compactedOfferLog3);
        save(compactedOfferLog4);

        // When
        Iterable<CompactedOfferLog> compactionLogs = service.getAscendingOfferLogCompactionBy("containerName", 42L);

        // Then
        assertThat(compactionLogs).hasSize(1);
        assertThat(compactionLogs).extracting(CompactedOfferLog::getSequenceStart).containsExactly(43L);
        assertThat(compactionLogs).extracting(CompactedOfferLog::getSequenceEnd).containsExactly(45L);
        assertThat(compactionLogs).extracting(o -> o.getLogs().size()).containsExactly(3);
    }

    @Test
    public void should_get_ascending_offer_log_compaction_with_no_range() throws Exception {
        // Given
        CompactedOfferLog compactedOfferLog1 = new CompactedOfferLog(
            40,
            40,
            LocalDateUtil.now(),
            "containerName",
            Collections.singletonList(new OfferLog())
        );
        CompactedOfferLog compactedOfferLog2 = new CompactedOfferLog(
            41,
            42,
            LocalDateUtil.now(),
            "containerName",
            Arrays.asList(new OfferLog(), new OfferLog())
        );
        CompactedOfferLog compactedOfferLog3 = new CompactedOfferLog(
            43,
            45,
            LocalDateUtil.now(),
            "containerName",
            Arrays.asList(new OfferLog(), new OfferLog(), new OfferLog())
        );
        CompactedOfferLog compactedOfferLog4 = new CompactedOfferLog(
            1,
            5,
            LocalDateUtil.now(),
            "OTHER_containerName",
            Collections.singletonList(new OfferLog())
        );
        save(compactedOfferLog1);
        save(compactedOfferLog2);
        save(compactedOfferLog3);
        save(compactedOfferLog4);

        // When
        Iterable<CompactedOfferLog> compactionLogs = service.getAscendingOfferLogCompactionBy("containerName", null);

        // Then
        assertThat(compactionLogs).hasSize(3);
        assertThat(compactionLogs).extracting(CompactedOfferLog::getSequenceStart).containsExactly(40L, 41L, 43L);
        assertThat(compactionLogs).extracting(CompactedOfferLog::getSequenceEnd).containsExactly(40L, 42L, 45L);
        assertThat(compactionLogs).extracting(o -> o.getLogs().size()).containsExactly(1 ,2, 3);
    }

    private static void cleanDatabase() {
        mongoRule.getMongoDatabase().getCollection(OfferCollections.COMPACTED_OFFER_LOG.getName()).deleteMany(new Document());
        OfferCollections collectionPrefixed = OfferCollections.COMPACTED_OFFER_LOG;
        collectionPrefixed.setPrefix(PREFIX);
        mongoRule.getMongoDatabase().getCollection(collectionPrefixed.getName()).deleteMany(new Document());
    }

    private void save(CompactedOfferLog compactedOfferLog) throws InvalidParseOperationException {
        mongoRule.getMongoDatabase()
            .getCollection(OfferCollections.COMPACTED_OFFER_LOG.getName())
            .insertOne(Document.parse(JsonHandler.writeAsString(compactedOfferLog)));
    }
}
