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
package fr.gouv.vitam.storage.offers.tape.impl.queue;

import com.mongodb.client.model.Filters;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.storage.engine.common.collection.OfferCollections;
import fr.gouv.vitam.storage.engine.common.model.QueueMessageType;
import fr.gouv.vitam.storage.engine.common.model.QueueState;
import fr.gouv.vitam.storage.engine.common.model.ReadOrder;
import fr.gouv.vitam.storage.engine.common.model.WriteOrder;
import fr.gouv.vitam.storage.offers.tape.exception.QueueException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.bson.conversions.Bson;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class QueueRepositoryImplTest {

    public static final String QUEUE = OfferCollections.TAPE_QUEUE_MESSAGE.getName() + GUIDFactory.newGUID().getId();

    @ClassRule
    public static MongoRule mongoRule = new MongoRule(MongoDbAccess.getMongoClientSettingsBuilder(), QUEUE);

    private static QueueRepositoryImpl queueRepositoryImpl;

    @BeforeClass
    public static void setUpBeforeClass() {
        queueRepositoryImpl = new QueueRepositoryImpl(mongoRule.getMongoDatabase().getCollection(QUEUE));
    }

    @AfterClass
    public static void setDownAfterClass() {
        mongoRule.handleAfterClass();
    }

    @After
    public void after() {
        mongoRule.handleAfter();
    }

    @Test
    public void testAddWriteOrder() throws QueueException {
        WriteOrder entity = new WriteOrder(
            "myBucket",
            "myFileBucketId",
            "myArchiveId",
            1_234_567_890_123L,
            "digest",
            "myArchiveId",
            QueueMessageType.WriteBackupOrder
        );
        queueRepositoryImpl.add(entity);

        Optional<WriteOrder> found = queueRepositoryImpl.receive(QueueMessageType.WriteBackupOrder);
        assertThat(found).isPresent();
        assertThat(found.get().getState()).isEqualTo(QueueState.RUNNING);
        assertThat(found.get().getBucket()).isEqualTo("myBucket");
        assertThat(found.get().getFileBucketId()).isEqualTo("myFileBucketId");
        assertThat(found.get().getFilePath()).isEqualTo("myArchiveId");
        assertThat(found.get().getDigest()).isEqualTo("digest");
        assertThat(found.get().getSize()).isEqualTo(1_234_567_890_123L);
        assertThat(found.get().getMessageType()).isEqualTo(QueueMessageType.WriteBackupOrder);

        found = queueRepositoryImpl.receive(QueueMessageType.WriteOrder);
        assertThat(found).isNotPresent();
    }

    @Test
    public void testAddReadOrder() throws QueueException {
        ReadOrder entity = new ReadOrder("VIT0001", 3, "tarId.tar", "myBucket", "myFileBucketId", 1_234_567_890_123L);

        queueRepositoryImpl.add(entity);

        Optional<ReadOrder> found = queueRepositoryImpl.receive(QueueMessageType.ReadOrder);
        assertThat(found).isPresent();
        assertThat(found.get().getState()).isEqualTo(QueueState.RUNNING);
        assertThat(found.get().getTapeCode()).isEqualTo("VIT0001");
        assertThat(found.get().getFileName()).isEqualTo("tarId.tar");
        assertThat(found.get().getBucket()).isEqualTo("myBucket");
        assertThat(found.get().getFileBucketId()).isEqualTo("myFileBucketId");
        assertThat(found.get().getSize()).isEqualTo(1_234_567_890_123L);
        assertThat(found.get().getFilePosition()).isEqualTo(3);

        found = queueRepositoryImpl.receive(QueueMessageType.ReadOrder);
        assertThat(found).isNotPresent();
    }

    @Test
    public void testRemoveOk() throws QueueException {
        WriteOrder entity = new WriteOrder(
            "myBucket",
            "myFileBucketId",
            "myArchiveId",
            1_234_567_890_123L,
            "digest",
            "myArchiveId",
            QueueMessageType.WriteOrder
        );
        queueRepositoryImpl.add(entity);

        queueRepositoryImpl.remove(entity.getId());

        Optional<WriteOrder> found = queueRepositoryImpl.receive(QueueMessageType.WriteOrder);
        assertThat(found).isNotPresent();
    }

    @Test
    public void testCompleteOk() throws QueueException {
        WriteOrder entity = new WriteOrder(
            "myBucket",
            "myFileBucketId",
            "myArchiveId",
            1_234_567_890_123L,
            "digest",
            "myArchiveId",
            QueueMessageType.WriteOrder
        );
        queueRepositoryImpl.add(entity);

        queueRepositoryImpl.complete(entity.getId());

        Optional<WriteOrder> found = queueRepositoryImpl.receive(QueueMessageType.WriteOrder);
        assertThat(found).isNotPresent();
    }

    @Test
    public void testPutOk() throws QueueException {
        WriteOrder entity = new WriteOrder(
            "myBucket",
            "myFileBucketId",
            "myArchiveId",
            1_234_567_890_123L,
            "digest",
            "myArchiveId",
            QueueMessageType.WriteOrder
        );
        queueRepositoryImpl.add(entity);

        queueRepositoryImpl.complete(entity.getId());

        Optional<WriteOrder> found = queueRepositoryImpl.receive(QueueMessageType.WriteOrder);
        assertThat(found).isNotPresent();

        queueRepositoryImpl.markReady(entity.getId());

        found = queueRepositoryImpl.receive(QueueMessageType.WriteOrder);
        assertThat(found).isPresent();
        assertThat(found.get().getState()).isEqualTo(QueueState.RUNNING);

        queueRepositoryImpl.complete(entity.getId());

        found = queueRepositoryImpl.receive(QueueMessageType.WriteOrder);
        assertThat(found).isNotPresent();
    }

    @Test
    public void testPeekWithPriority() throws QueueException {
        // Given document with priority 2
        WriteOrder entity = new WriteOrder(
            "myBucket1",
            "myFileBucketId1",
            "myArchiveId1",
            1_234_567_890_123L,
            "digest1",
            "myArchiveId1",
            QueueMessageType.WriteOrder
        );
        entity.setPriority(2);
        queueRepositoryImpl.add(entity);

        // Given document with priority 1
        entity = new WriteOrder(
            "myBucket2",
            "myFileBucketId2",
            "myArchiveId2",
            10L,
            "digest2",
            "myArchiveId2",
            QueueMessageType.WriteOrder
        );
        queueRepositoryImpl.add(entity);

        // Given document with priority 1
        Optional<WriteOrder> found = queueRepositoryImpl.receive(QueueMessageType.WriteOrder);
        assertThat(found).isPresent();
        assertThat(found.get().getState()).isEqualTo(QueueState.RUNNING);
        assertThat(found.get().getBucket()).isEqualTo("myBucket2");
        assertThat(found.get().getArchiveId()).isEqualTo("myArchiveId2");

        // Given document with priority 2
        found = queueRepositoryImpl.receive(QueueMessageType.WriteOrder);
        assertThat(found).isPresent();
        assertThat(found.get().getState()).isEqualTo(QueueState.RUNNING);
        assertThat(found.get().getBucket()).isEqualTo("myBucket1");
        assertThat(found.get().getArchiveId()).isEqualTo("myArchiveId1");
    }

    @Test
    public void testPeekWithoutPriority() throws QueueException {
        // Given document with priority 2
        WriteOrder entity = new WriteOrder(
            "myBucket1",
            "myFileBucketId1",
            "myArchiveId1",
            1_234_567_890_123L,
            "digest1",
            "myArchiveId1",
            QueueMessageType.WriteOrder
        );
        entity.setPriority(2);
        queueRepositoryImpl.add(entity);

        // Given document with priority 1
        entity = new WriteOrder(
            "myBucket2",
            "myFileBucketId2",
            "myArchiveId2",
            10L,
            "digest2",
            "myArchiveId2",
            QueueMessageType.WriteOrder
        );
        queueRepositoryImpl.add(entity);

        // Then get the first inserted document
        Optional<WriteOrder> found = queueRepositoryImpl.receive(QueueMessageType.WriteOrder, false);
        assertThat(found).isPresent();
        assertThat(found.get().getState()).isEqualTo(QueueState.RUNNING);
        assertThat(found.get().getBucket()).isEqualTo("myBucket1");
        assertThat(found.get().getArchiveId()).isEqualTo("myArchiveId1");

        // Then get the second inserted document
        found = queueRepositoryImpl.receive(QueueMessageType.WriteOrder, false);
        assertThat(found).isPresent();
        assertThat(found.get().getState()).isEqualTo(QueueState.RUNNING);
        assertThat(found.get().getBucket()).isEqualTo("myBucket2");
        assertThat(found.get().getArchiveId()).isEqualTo("myArchiveId2");
    }

    @Test
    public void testPeekWithQueryAndPriority() throws QueueException {
        // Given document with priority 2
        WriteOrder entity = new WriteOrder(
            "myBucket1",
            "myFileBucketId1",
            "myArchiveId1",
            1_234_567_890_123L,
            "digest1",
            "myArchiveId1",
            QueueMessageType.WriteBackupOrder
        );
        entity.setPriority(2);
        queueRepositoryImpl.add(entity);

        // Given document with priority 1
        entity = new WriteOrder(
            "myBucket2",
            "myFileBucketId2",
            "myArchiveId2",
            10L,
            "digest2",
            "myArchiveId2",
            QueueMessageType.WriteOrder
        );
        entity.setPriority(2);
        queueRepositoryImpl.add(entity);

        // Given document with priority 1
        entity = new WriteOrder(
            "myBucket2",
            "myFileBucketId3",
            "myArchiveId3",
            1_234L,
            "digest3",
            "myArchiveId3",
            QueueMessageType.WriteOrder
        );
        queueRepositoryImpl.add(entity);

        Bson query = Filters.eq(WriteOrder.BUCKET, "myBucket2");

        // Then get the first inserted document
        Optional<WriteOrder> found = queueRepositoryImpl.receive(query, QueueMessageType.WriteOrder);
        assertThat(found).isPresent();
        assertThat(found.get().getState()).isEqualTo(QueueState.RUNNING);
        assertThat(found.get().getBucket()).isEqualTo("myBucket2");
        assertThat(found.get().getArchiveId()).isEqualTo("myArchiveId3");
    }

    @Test
    public void testPeekWithQueryAndWithoutPriority() throws QueueException {
        // Given document with priority 2
        WriteOrder entity = new WriteOrder(
            "myBucket1",
            "myFileBucketId1",
            "myArchiveId1",
            1_234_567_890_123L,
            "digest1",
            "myArchiveId1",
            QueueMessageType.WriteOrder
        );
        entity.setPriority(2);
        queueRepositoryImpl.add(entity);

        // Given document with priority 1
        entity = new WriteOrder(
            "myBucket2",
            "myFileBucketId2",
            "myArchiveId2",
            10L,
            "digest2",
            "myArchiveId2",
            QueueMessageType.WriteOrder
        );
        entity.setPriority(2);
        queueRepositoryImpl.add(entity);

        // Given document with priority 1
        entity = new WriteOrder(
            "myBucket2",
            "myFileBucketId3",
            "myArchiveId3",
            1_234L,
            "digest3",
            "myArchiveId3",
            QueueMessageType.WriteOrder
        );
        queueRepositoryImpl.add(entity);

        Bson query = Filters.eq(WriteOrder.BUCKET, "myBucket2");

        // Then get the first inserted document
        Optional<WriteOrder> found = queueRepositoryImpl.receive(query, QueueMessageType.WriteOrder, false);
        assertThat(found).isPresent();
        assertThat(found.get().getState()).isEqualTo(QueueState.RUNNING);
        assertThat(found.get().getBucket()).isEqualTo("myBucket2");
        assertThat(found.get().getArchiveId()).isEqualTo("myArchiveId2");
    }

    @Test
    public void testCountByStateAndType() throws QueueException {
        // Given
        WriteOrder writeOrder1 = new WriteOrder(
            "myBucket",
            "myFileBucketId",
            "myArchiveId",
            1_234_567_890_123L,
            "digest",
            "myArchiveId",
            QueueMessageType.WriteOrder
        );
        writeOrder1.setState(QueueState.READY);
        queueRepositoryImpl.add(writeOrder1);

        WriteOrder writeOrder2 = new WriteOrder(
            "myBucket",
            "myFileBucketId",
            "myArchiveId",
            1_234_567_890_123L,
            "digest",
            "myArchiveId",
            QueueMessageType.WriteOrder
        );
        writeOrder2.setState(QueueState.READY);
        queueRepositoryImpl.add(writeOrder2);

        WriteOrder writeOrder3 = new WriteOrder(
            "myBucket",
            "myFileBucketId",
            "myArchiveId",
            1_234_567_890_123L,
            "digest",
            "myArchiveId",
            QueueMessageType.WriteOrder
        );
        writeOrder3.setState(QueueState.ERROR);
        queueRepositoryImpl.add(writeOrder3);

        WriteOrder backupWriteOrder1 = new WriteOrder(
            "backup",
            "myFileBucketId",
            "myArchiveId",
            1_234_567_890_123L,
            "digest",
            "myArchiveId",
            QueueMessageType.WriteBackupOrder
        );
        backupWriteOrder1.setState(QueueState.READY);
        queueRepositoryImpl.add(backupWriteOrder1);

        ReadOrder readOrder1 = new ReadOrder(
            "VIT0001",
            3,
            "tarId.tar",
            "myBucket",
            "myFileBucketId",
            1_234_567_890_123L
        );
        readOrder1.setState(QueueState.RUNNING);
        queueRepositoryImpl.add(readOrder1);

        // When
        Map<Pair<QueueState, QueueMessageType>, Integer> stats = queueRepositoryImpl.countByStateAndType();

        // Then
        assertThat(stats).isEqualTo(
            Map.of(
                new ImmutablePair<>(QueueState.READY, QueueMessageType.WriteOrder),
                2,
                new ImmutablePair<>(QueueState.ERROR, QueueMessageType.WriteOrder),
                1,
                new ImmutablePair<>(QueueState.READY, QueueMessageType.WriteBackupOrder),
                1,
                new ImmutablePair<>(QueueState.RUNNING, QueueMessageType.ReadOrder),
                1
            )
        );
    }
}
