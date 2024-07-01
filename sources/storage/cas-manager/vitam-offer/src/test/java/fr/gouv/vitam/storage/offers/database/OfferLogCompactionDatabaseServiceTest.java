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

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
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
import java.util.Collections;
import java.util.List;

import static fr.gouv.vitam.storage.engine.common.collection.OfferCollections.COMPACTED_OFFER_LOG;
import static org.assertj.core.api.Assertions.assertThat;

public class OfferLogCompactionDatabaseServiceTest {

    private static final String PREFIX = GUIDFactory.newGUID().getId();

    private OfferLogCompactionDatabaseService service;

    @ClassRule
    public static MongoRule mongoRule = new MongoRule(MongoDbAccess.getMongoClientSettingsBuilder());

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Before
    public void before() throws InvalidParseOperationException {
        cleanDatabase();
        service = new OfferLogCompactionDatabaseService(
            mongoRule.getMongoDatabase().getCollection(COMPACTED_OFFER_LOG.getName())
        );
    }

    @AfterClass
    public static void after() {
        cleanDatabase();
        mongoRule.handleAfter();
    }

    @Test
    public void should_get_descending_offer_log_compaction_without_start_offset_with_high_limit() throws Exception {
        // Given
        importCompactedOfferLogDataSet();

        // When
        List<OfferLog> compactionLogs = service.getDescendingOfferLogCompactionBy("containerName", null, 1000);

        // Then
        assertThat(compactionLogs).extracting(OfferLog::getSequence).containsExactly(51L, 48L, 46L, 43L, 42L, 40L);
    }

    @Test
    public void should_get_descending_offer_log_compaction_without_start_offset_with_limit() throws Exception {
        // Given
        importCompactedOfferLogDataSet();

        // When
        List<OfferLog> compactionLogs = service.getDescendingOfferLogCompactionBy("containerName", null, 4);

        // Then
        assertThat(compactionLogs).extracting(OfferLog::getSequence).containsExactly(51L, 48L, 46L, 43L);
    }

    @Test
    public void should_get_descending_offer_log_compaction_with_high_start_offset_with_high_limit() throws Exception {
        // Given
        importCompactedOfferLogDataSet();

        // When
        List<OfferLog> compactionLogs = service.getDescendingOfferLogCompactionBy("containerName", 52L, 1000);

        // Then
        assertThat(compactionLogs).extracting(OfferLog::getSequence).containsExactly(51L, 48L, 46L, 43L, 42L, 40L);
    }

    @Test
    public void should_get_descending_offer_log_compaction_with_high_start_offset_with_limit() throws Exception {
        // Given
        importCompactedOfferLogDataSet();

        // When
        List<OfferLog> compactionLogs = service.getDescendingOfferLogCompactionBy("containerName", 52L, 4);

        // Then
        assertThat(compactionLogs).extracting(OfferLog::getSequence).containsExactly(51L, 48L, 46L, 43L);
    }

    @Test
    public void should_get_descending_offer_log_compaction_with_upper_range_start_offset_with_high_limit()
        throws Exception {
        // Given
        importCompactedOfferLogDataSet();

        // When
        List<OfferLog> compactionLogs = service.getDescendingOfferLogCompactionBy("containerName", 51L, 1000);

        // Then
        assertThat(compactionLogs).extracting(OfferLog::getSequence).containsExactly(51L, 48L, 46L, 43L, 42L, 40L);
    }

    @Test
    public void should_get_descending_offer_log_compaction_with_upper_range_start_offset_with_limit() throws Exception {
        // Given
        importCompactedOfferLogDataSet();

        // When
        List<OfferLog> compactionLogs = service.getDescendingOfferLogCompactionBy("containerName", 51L, 4);

        // Then
        assertThat(compactionLogs).extracting(OfferLog::getSequence).containsExactly(51L, 48L, 46L, 43L);
    }

    @Test
    public void should_get_descending_offer_log_compaction_with_middle_range_start_offset_with_high_limit()
        throws Exception {
        // Given
        importCompactedOfferLogDataSet();

        // When
        List<OfferLog> compactionLogs = service.getDescendingOfferLogCompactionBy("containerName", 49L, 1000);

        // Then
        assertThat(compactionLogs).extracting(OfferLog::getSequence).containsExactly(48L, 46L, 43L, 42L, 40L);
    }

    @Test
    public void should_get_descending_offer_log_compaction_with_middle_range_start_offset_with_limit()
        throws Exception {
        // Given
        importCompactedOfferLogDataSet();

        // When
        List<OfferLog> compactionLogs = service.getDescendingOfferLogCompactionBy("containerName", 49L, 4);

        // Then
        assertThat(compactionLogs).extracting(OfferLog::getSequence).containsExactly(48L, 46L, 43L, 42L);
    }

    @Test
    public void should_get_descending_offer_log_compaction_with_lower_range_start_offset_with_high_limit()
        throws Exception {
        // Given
        importCompactedOfferLogDataSet();

        // When
        List<OfferLog> compactionLogs = service.getDescendingOfferLogCompactionBy("containerName", 43L, 1000);

        // Then
        assertThat(compactionLogs).extracting(OfferLog::getSequence).containsExactly(43L, 42L, 40L);
    }

    @Test
    public void should_get_descending_offer_log_compaction_with_lower_range_start_offset_with_limit() throws Exception {
        // Given
        importCompactedOfferLogDataSet();

        // When
        List<OfferLog> compactionLogs = service.getDescendingOfferLogCompactionBy("containerName", 43L, 2);

        // Then
        assertThat(compactionLogs).extracting(OfferLog::getSequence).containsExactly(43L, 42L);
    }

    @Test
    public void should_get_descending_offer_log_compaction_with_too_low_start_offset() throws Exception {
        // Given
        importCompactedOfferLogDataSet();

        // When
        List<OfferLog> compactionLogs = service.getDescendingOfferLogCompactionBy("containerName", 39L, 1000);

        // Then
        assertThat(compactionLogs).isEmpty();
    }

    @Test
    public void should_get_ascending_offer_log_compaction_without_start_offset_with_high_limit() throws Exception {
        // Given
        importCompactedOfferLogDataSet();

        // When
        List<OfferLog> compactionLogs = service.getAscendingOfferLogCompactionBy("containerName", null, 1000);

        // Then
        assertThat(compactionLogs).extracting(OfferLog::getSequence).containsExactly(40L, 42L, 43L, 46L, 48L, 51L);
    }

    @Test
    public void should_get_ascending_offer_log_compaction_without_start_offset_with_limit() throws Exception {
        // Given
        importCompactedOfferLogDataSet();

        // When
        List<OfferLog> compactionLogs = service.getAscendingOfferLogCompactionBy("containerName", null, 4);

        // Then
        assertThat(compactionLogs).extracting(OfferLog::getSequence).containsExactly(40L, 42L, 43L, 46L);
    }

    @Test
    public void should_get_ascending_offer_log_compaction_with_low_start_offset_with_high_limit() throws Exception {
        // Given
        importCompactedOfferLogDataSet();

        // When
        List<OfferLog> compactionLogs = service.getAscendingOfferLogCompactionBy("containerName", 39L, 1000);

        // Then
        assertThat(compactionLogs).extracting(OfferLog::getSequence).containsExactly(40L, 42L, 43L, 46L, 48L, 51L);
    }

    @Test
    public void should_get_ascending_offer_log_compaction_with_low_start_offset_with_limit() throws Exception {
        // Given
        importCompactedOfferLogDataSet();

        // When
        List<OfferLog> compactionLogs = service.getAscendingOfferLogCompactionBy("containerName", 39L, 4);

        // Then
        assertThat(compactionLogs).extracting(OfferLog::getSequence).containsExactly(40L, 42L, 43L, 46L);
    }

    @Test
    public void should_get_ascending_offer_log_compaction_with_lower_range_start_offset_with_high_limit()
        throws Exception {
        // Given
        importCompactedOfferLogDataSet();

        // When
        List<OfferLog> compactionLogs = service.getAscendingOfferLogCompactionBy("containerName", 40L, 1000);

        // Then
        assertThat(compactionLogs).extracting(OfferLog::getSequence).containsExactly(40L, 42L, 43L, 46L, 48L, 51L);
    }

    @Test
    public void should_get_ascending_offer_log_compaction_with_lower_range_start_offset_with_limit() throws Exception {
        // Given
        importCompactedOfferLogDataSet();

        // When
        List<OfferLog> compactionLogs = service.getAscendingOfferLogCompactionBy("containerName", 40L, 4);

        // Then
        assertThat(compactionLogs).extracting(OfferLog::getSequence).containsExactly(40L, 42L, 43L, 46L);
    }

    @Test
    public void should_get_ascending_offer_log_compaction_with_middle_range_start_offset_with_high_limit()
        throws Exception {
        // Given
        importCompactedOfferLogDataSet();

        // When
        List<OfferLog> compactionLogs = service.getAscendingOfferLogCompactionBy("containerName", 41L, 1000);

        // Then
        assertThat(compactionLogs).extracting(OfferLog::getSequence).containsExactly(42L, 43L, 46L, 48L, 51L);
    }

    @Test
    public void should_get_ascending_offer_log_compaction_with_middle_range_start_offset_with_limit() throws Exception {
        // Given
        importCompactedOfferLogDataSet();

        // When
        List<OfferLog> compactionLogs = service.getAscendingOfferLogCompactionBy("containerName", 41L, 4);

        // Then
        assertThat(compactionLogs).extracting(OfferLog::getSequence).containsExactly(42L, 43L, 46L, 48L);
    }

    @Test
    public void should_get_ascending_offer_log_compaction_with_upper_range_start_offset_with_high_limit()
        throws Exception {
        // Given
        importCompactedOfferLogDataSet();

        // When
        List<OfferLog> compactionLogs = service.getAscendingOfferLogCompactionBy("containerName", 43L, 1000);

        // Then
        assertThat(compactionLogs).extracting(OfferLog::getSequence).containsExactly(43L, 46L, 48L, 51L);
    }

    @Test
    public void should_get_ascending_offer_log_compaction_with_upper_range_start_offset_with_limit() throws Exception {
        // Given
        importCompactedOfferLogDataSet();

        // When
        List<OfferLog> compactionLogs = service.getAscendingOfferLogCompactionBy("containerName", 43L, 2);

        // Then
        assertThat(compactionLogs).extracting(OfferLog::getSequence).containsExactly(43L, 46L);
    }

    @Test
    public void should_get_ascending_offer_log_compaction_with_too_high_start_offset() throws Exception {
        // Given
        importCompactedOfferLogDataSet();

        // When
        List<OfferLog> compactionLogs = service.getAscendingOfferLogCompactionBy("containerName", 52L, 1000);

        // Then
        assertThat(compactionLogs).isEmpty();
    }

    private static void cleanDatabase() {
        mongoRule
            .getMongoDatabase()
            .getCollection(OfferCollections.COMPACTED_OFFER_LOG.getName())
            .deleteMany(new Document());
        OfferCollections collectionPrefixed = OfferCollections.COMPACTED_OFFER_LOG;
        collectionPrefixed.setPrefix(PREFIX);
        mongoRule.getMongoDatabase().getCollection(collectionPrefixed.getName()).deleteMany(new Document());
    }

    private void importCompactedOfferLogDataSet() throws InvalidParseOperationException {
        // Dataset
        CompactedOfferLog compactedOfferLog1 = new CompactedOfferLog(
            40,
            42,
            LocalDateUtil.now(),
            "containerName",
            Arrays.asList(new OfferLog().setSequence(40), new OfferLog().setSequence(42))
        );
        CompactedOfferLog compactedOfferLog2 = new CompactedOfferLog(
            43,
            43,
            LocalDateUtil.now(),
            "containerName",
            Collections.singletonList(new OfferLog().setSequence(43))
        );
        CompactedOfferLog compactedOfferLog3 = new CompactedOfferLog(
            1,
            50,
            LocalDateUtil.now(),
            "OTHER_containerName",
            Arrays.asList(new OfferLog().setSequence(1), new OfferLog().setSequence(30), new OfferLog().setSequence(50))
        );
        CompactedOfferLog compactedOfferLog4 = new CompactedOfferLog(
            46,
            51,
            LocalDateUtil.now(),
            "containerName",
            Arrays.asList(
                new OfferLog().setSequence(46),
                new OfferLog().setSequence(48),
                new OfferLog().setSequence(51)
            )
        );
        save(compactedOfferLog1);
        save(compactedOfferLog2);
        save(compactedOfferLog3);
        save(compactedOfferLog4);
    }

    private void save(CompactedOfferLog compactedOfferLog) throws InvalidParseOperationException {
        mongoRule
            .getMongoDatabase()
            .getCollection(OfferCollections.COMPACTED_OFFER_LOG.getName())
            .insertOne(Document.parse(JsonHandler.writeAsString(compactedOfferLog)));
    }
}
