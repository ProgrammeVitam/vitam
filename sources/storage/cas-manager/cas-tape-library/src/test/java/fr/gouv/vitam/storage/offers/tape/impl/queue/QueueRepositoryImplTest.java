package fr.gouv.vitam.storage.offers.tape.impl.queue;

import static fr.gouv.vitam.common.database.collections.VitamCollection.getMongoClientOptions;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.storage.engine.common.collection.OfferCollections;
import fr.gouv.vitam.storage.engine.common.model.QueueEntity;
import fr.gouv.vitam.storage.engine.common.model.QueueState;
import fr.gouv.vitam.storage.offers.tape.order.ReadOrder;
import fr.gouv.vitam.storage.offers.tape.order.WriteOrder;
import org.bson.conversions.Bson;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

public class QueueRepositoryImplTest {

    public static final String QUEUE = OfferCollections.OFFER_QUEUE.getName() + GUIDFactory.newGUID().getId();

    @ClassRule
    public static MongoRule mongoRule = new MongoRule(getMongoClientOptions(), QUEUE);

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

        WriteOrder entity = new WriteOrder().setBucket("mybucket").setFilePath("myFilePath");
        queueRepositoryImpl.add(entity, WriteOrder.class);

        Optional<WriteOrder> found = queueRepositoryImpl.peek(WriteOrder.class);
        assertThat(found).isPresent();
        assertThat(found.get().getState()).isEqualTo(QueueState.RUNNING);
        assertThat(found.get().getBucket()).isEqualTo("mybucket");
        assertThat(found.get().getFilePath()).isEqualTo("myFilePath");

        found = queueRepositoryImpl.peek(WriteOrder.class);
        assertThat(found).isNotPresent();
    }

    @Test
    public void testAddReadOrder() throws QueueException {

        ReadOrder entity = new ReadOrder().setTapeCode("VIT0001").setFilePosition(3);
        queueRepositoryImpl.add(entity, ReadOrder.class);

        Optional<ReadOrder> found = queueRepositoryImpl.peek(ReadOrder.class);
        assertThat(found).isPresent();
        assertThat(found.get().getState()).isEqualTo(QueueState.RUNNING);
        assertThat(found.get().getTapeCode()).isEqualTo("VIT0001");
        assertThat(found.get().getFilePosition()).isEqualTo(3);

        found = queueRepositoryImpl.peek(ReadOrder.class);
        assertThat(found).isNotPresent();
    }

    @Test
    public void testRemoveOk() throws QueueException {
        WriteOrder entity = new WriteOrder().setBucket("mybucket").setFilePath("myFilePath");
        queueRepositoryImpl.add(entity, WriteOrder.class);

        queueRepositoryImpl.remove(entity.getId());

        Optional<WriteOrder> found = queueRepositoryImpl.peek(WriteOrder.class);
        assertThat(found).isNotPresent();

    }


    @Test
    public void testFinishOk() throws QueueException {
        WriteOrder entity = new WriteOrder().setBucket("mybucket").setFilePath("myFilePath");
        queueRepositoryImpl.add(entity, WriteOrder.class);

        queueRepositoryImpl.finish(entity.getId());

        Optional<WriteOrder> found = queueRepositoryImpl.peek(WriteOrder.class);
        assertThat(found).isNotPresent();

    }

    @Test
    public void testPeekWithPriority() throws QueueException {
        // Given document with priority 2
        WriteOrder entity = new WriteOrder().setBucket("mybucket_1").setFilePath("myFilePath_1");
        entity.setPriority(2);
        queueRepositoryImpl.add(entity, WriteOrder.class);

        // Given document with priority 1
        entity = new WriteOrder().setBucket("mybucket_2").setFilePath("myFilePath_2");
        queueRepositoryImpl.add(entity, WriteOrder.class);

        // Given document with priority 1
        Optional<WriteOrder> found = queueRepositoryImpl.peek(WriteOrder.class);
        assertThat(found).isPresent();
        assertThat(found.get().getState()).isEqualTo(QueueState.RUNNING);
        assertThat(found.get().getBucket()).isEqualTo("mybucket_2");
        assertThat(found.get().getFilePath()).isEqualTo("myFilePath_2");

        // Given document with priority 2
        found = queueRepositoryImpl.peek(WriteOrder.class);
        assertThat(found).isPresent();
        assertThat(found.get().getState()).isEqualTo(QueueState.RUNNING);
        assertThat(found.get().getBucket()).isEqualTo("mybucket_1");
        assertThat(found.get().getFilePath()).isEqualTo("myFilePath_1");
    }

    @Test
    public void testPeekWithoutPriority() throws QueueException {

        // Given document with priority 2
        WriteOrder entity = new WriteOrder().setBucket("mybucket_1").setFilePath("myFilePath_1");
        entity.setPriority(2);
        queueRepositoryImpl.add(entity, WriteOrder.class);

        // Given document with priority 1
        entity = new WriteOrder().setBucket("mybucket_2").setFilePath("myFilePath_2");
        queueRepositoryImpl.add(entity, WriteOrder.class);

        // Then get the first inserted document
        Optional<WriteOrder> found = queueRepositoryImpl.peek(WriteOrder.class, false);
        assertThat(found).isPresent();
        assertThat(found.get().getState()).isEqualTo(QueueState.RUNNING);
        assertThat(found.get().getBucket()).isEqualTo("mybucket_1");
        assertThat(found.get().getFilePath()).isEqualTo("myFilePath_1");

        // Then get the second inserted document
        found = queueRepositoryImpl.peek(WriteOrder.class, false);
        assertThat(found).isPresent();
        assertThat(found.get().getState()).isEqualTo(QueueState.RUNNING);
        assertThat(found.get().getBucket()).isEqualTo("mybucket_2");
        assertThat(found.get().getFilePath()).isEqualTo("myFilePath_2");
    }

    @Test
    public void testPeekWithQueryAndPriority() throws QueueException {

        // Given document with priority 2
        WriteOrder entity = new WriteOrder().setBucket("mybucket_1").setFilePath("myFilePath_1");
        entity.setPriority(2);
        queueRepositoryImpl.add(entity, WriteOrder.class);

        // Given document with priority 1
        entity = new WriteOrder().setBucket("mybucket_2").setFilePath("myFilePath_2");
        entity.setPriority(2);
        queueRepositoryImpl.add(entity, WriteOrder.class);

        // Given document with priority 1
        entity = new WriteOrder().setBucket("mybucket_2").setFilePath("myFilePath_3");
        queueRepositoryImpl.add(entity, WriteOrder.class);



        Bson query = Filters.eq(WriteOrder.BUCKET, "mybucket_2");
        Bson update = Updates.set(QueueEntity.PRIORITY, 5);

        // Then get the first inserted document
        Optional<WriteOrder> found = queueRepositoryImpl.peek(query, update, WriteOrder.class);
        assertThat(found).isPresent();
        assertThat(found.get().getState()).isEqualTo(QueueState.RUNNING);
        assertThat(found.get().getBucket()).isEqualTo("mybucket_2");
        assertThat(found.get().getFilePath()).isEqualTo("myFilePath_3");
        assertThat(found.get().getPriority()).isEqualTo(5);

    }

    @Test
    public void testPeekWithQueryAndWithoutPriority() throws QueueException {

        // Given document with priority 2
        WriteOrder entity = new WriteOrder().setBucket("mybucket_1").setFilePath("myFilePath_1");
        entity.setPriority(2);
        queueRepositoryImpl.add(entity, WriteOrder.class);

        // Given document with priority 1
        entity = new WriteOrder().setBucket("mybucket_2").setFilePath("myFilePath_2");
        entity.setPriority(2);
        queueRepositoryImpl.add(entity, WriteOrder.class);

        // Given document with priority 1
        entity = new WriteOrder().setBucket("mybucket_2").setFilePath("myFilePath_3");
        queueRepositoryImpl.add(entity, WriteOrder.class);



        Bson query = Filters.eq(WriteOrder.BUCKET, "mybucket_2");
        Bson update = Updates.set(QueueEntity.PRIORITY, 5);

        // Then get the first inserted document
        Optional<WriteOrder> found = queueRepositoryImpl.peek(query, update, WriteOrder.class, false);
        assertThat(found).isPresent();
        assertThat(found.get().getState()).isEqualTo(QueueState.RUNNING);
        assertThat(found.get().getBucket()).isEqualTo("mybucket_2");
        assertThat(found.get().getFilePath()).isEqualTo("myFilePath_2");
        assertThat(found.get().getPriority()).isEqualTo(5);

    }
}